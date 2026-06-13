package com.posbah.app.ui.screens.bmp.clients

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.posbah.app.data.repository.BmpInvoiceRepository
import javax.inject.Inject

data class ClientsUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val payMassalClientId: Long? = null,    // clientId yang sedang dibuka dialog bayar borongan
    val payMassalFeedback: String? = null   // pesan sukses/error setelah bayar borongan
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val repo: BmpClientRepository,
    private val invoiceRepo: BmpInvoiceRepository,
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val _query = MutableStateFlow("")
    private val _ui = MutableStateFlow(ClientsUiState())
    val ui = _ui.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val clients = _query
        .flatMapLatest { q ->
            if (q.isBlank()) repo.observe(tenantId) else repo.search(tenantId, q)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repo.delete(id)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                invoiceRepo.payMassal(
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
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
    private val db: PosBahDatabase,
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

    fun save() {
        viewModelScope.launch {
            repo.upsert(_form.value)
            _saved.value = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
