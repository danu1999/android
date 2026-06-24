package com.posbah.app.ui.screens.bmp.clients

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpClientRepository
import com.posbah.app.data.repository.OnlineWriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow

import com.posbah.app.data.repository.BmpInvoiceRepository
import javax.inject.Inject

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

typealias ClientDto = BmpClientEntity
typealias BmpInvoiceDto = com.posbah.app.data.local.entities.BmpInvoiceEntity

data class ClientsUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val payMassalClientId: Long? = null,    // clientId yang sedang dibuka dialog bayar borongan
    val payMassalFeedback: String? = null   // pesan sukses/error setelah bayar borongan
)

/** Ringkasan piutang per klien — dipakai di kartu klien */
data class ClientInvoiceSummary(
    val totalPiutang: Double,
    val jumlahUnpaid: Int,
    val hasOverdue: Boolean
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val clientRepository: BmpClientRepository,
    private val invoiceRepository: BmpInvoiceRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val _query = MutableStateFlow("")
    private val _ui = MutableStateFlow(ClientsUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            clientRepository.refresh()
            invoiceRepository.refresh()
        }
    }

    private val repository = object {
        fun getClientsState(): kotlinx.coroutines.flow.Flow<UiState<List<ClientDto>>> =
            clientRepository.allClients.map { UiState.Success(it) }

        fun getInvoicesState(): kotlinx.coroutines.flow.Flow<UiState<List<BmpInvoiceDto>>> =
            invoiceRepository.allInvoices.map { UiState.Success(it) }
    }

    /** Map clientId → ringkasan piutang — diupdate setiap kali invoices/clients berubah */
    val clientInvoiceSummary: StateFlow<UiState<Map<Long, ClientInvoiceSummary>>> = combine(
        repository.getClientsState(), // memancarkan UiState<List<ClientDto>>
        repository.getInvoicesState() // memancarkan UiState<List<BmpInvoiceDto>>
    ) { clientsState, invoicesState ->
        if (clientsState is UiState.Success && invoicesState is UiState.Success) {
            val clients = clientsState.data
            val invoices = invoicesState.data
            val now = System.currentTimeMillis()
            val map = clients.associate { client ->
                val clientInvoices = invoices.filter { it.clientId == client.id && it.status != "PAID" && !it.isDeleted }
                val totalPiutang = clientInvoices.sumOf { it.totalAmount - it.paidAmount }
                val jumlahUnpaid = clientInvoices.size
                val hasOverdue = clientInvoices.any { inv ->
                    inv.dueDate != null && inv.dueDate < now && inv.status != "PAID"
                }
                client.id to ClientInvoiceSummary(
                    totalPiutang = totalPiutang,
                    jumlahUnpaid = jumlahUnpaid,
                    hasOverdue = hasOverdue
                )
            }
            UiState.Success(map)
        } else {
            UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val clients = _query
        .flatMapLatest { q ->
            if (q.isBlank()) clientRepository.observe(tenantId) else clientRepository.search(tenantId, q)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError = _deleteError.asStateFlow()
    fun clearDeleteError() { _deleteError.value = null }

    fun delete(id: Long) {
        viewModelScope.launch {
            _deleteError.value = null
            val result = clientRepository.delete(context, tenantId, id)
            if (result is OnlineWriteResult.Error) {
                _deleteError.value = result.message
            } else if (result is OnlineWriteResult.NoConnection) {
                _deleteError.value = "Tidak ada koneksi internet. Hapus dibatalkan."
            }
        }
    }

    /** Buka dialog Bayar Borongan untuk klien tertentu */
    fun openPayMassal(clientId: Long) {
        _ui.update { it.copy(payMassalClientId = clientId, payMassalFeedback = null) }
    }

    /** Tutup dialog Bayar Borongan */
    fun closePayMassal() {
        _ui.update { it.copy(payMassalClientId = null, payMassalFeedback = null) }
    }

    /**
     * Eksekusi pembayaran borongan:
     * - Alokasikan [nominal] ke invoice-invoice UNPAID/PARTIAL milik [clientId], tertua dulu.
     * - Sisa uang (jika ada) masuk ke [BmpClientEntity.saldoTitipan].
     * - Catat 1 entri kas masuk di CashFlow.
     */
    fun payMassal(clientId: Long, nominal: Double, method: String, notes: String?) {
        viewModelScope.launch {
            try {
                invoiceRepository.payMassal(
                    tenantId = tenantId,
                    clientId = clientId,
                    nominal = nominal,
                    paymentMethod = method,
                    notes = notes
                )
                _ui.update {
                    it.copy(
                        payMassalClientId = null,
                        payMassalFeedback = "Pembayaran borongan berhasil dicatat"
                    )
                }
                // Full online: tidak perlu sync manual, data sudah real-time di VPS
            } catch (e: Exception) {
                _ui.update {
                    it.copy(payMassalFeedback = "Gagal: ${e.message}")
                }
            }
        }
    }
}

@HiltViewModel
class ClientEditViewModel @Inject constructor(
    private val repo: BmpClientRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    savedState: SavedStateHandle
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val editingId: Long = savedState.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 } ?: -1L

    private val _form = MutableStateFlow(
        BmpClientEntity(
            tenantId = tenantId,
            clientName = ""
        )
    )
    val form = _form.asStateFlow()
    private val _saved = MutableStateFlow(false)
    val saved = _saved.asStateFlow()

    init {
        if (editingId > 0) viewModelScope.launch {
            repo.getById(editingId)?.let { _form.value = it }
        }
    }

    fun update(transform: (BmpClientEntity) -> BmpClientEntity) {
        _form.update(transform)
    }

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError = _saveError.asStateFlow()
    fun clearSaveError() { _saveError.value = null }

    fun save() {
        viewModelScope.launch {
            _saveError.value = null
            val result = repo.upsert(context, _form.value)
            when (result) {
                is OnlineWriteResult.Success -> {
                    _saved.value = true
                }
                is OnlineWriteResult.Error -> {
                    _saveError.value = result.message
                }
                is OnlineWriteResult.NoConnection -> {
                    _saveError.value = "Tidak ada koneksi internet. Data tidak tersimpan."
                }
            }
        }
    }
}
