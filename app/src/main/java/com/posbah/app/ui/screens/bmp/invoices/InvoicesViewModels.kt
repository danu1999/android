package com.posbah.app.ui.screens.bmp.invoices

import androidx.lifecycle.SavedStateHandle
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpClientRepository
import com.posbah.app.data.repository.BmpInvoiceRepository
import com.posbah.app.data.repository.BmpMasterProductRepository
import com.posbah.app.data.repository.BmpSettingsRepository
import com.posbah.app.data.repository.PrintSettingsRepository
import com.posbah.app.ui.print.PrintConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class InvoicesListViewModel @Inject constructor(
    private val invoiceRepo: BmpInvoiceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    val invoices = invoiceRepo.observe(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(id: Long) = viewModelScope.launch { invoiceRepo.deleteInvoice(id) }
}

data class InvoiceDetailUi(
    val invoice: BmpInvoiceEntity? = null,
    val products: List<BmpProductEntity> = emptyList(),
    val payments: List<BmpInvoicePaymentEntity> = emptyList(),
    val client: BmpClientEntity? = null,
    val settings: BmpSettingsEntity? = null,
    val printConfig: PrintConfig = PrintConfig(),
    val showAddPayment: Boolean = false,
    val newPaymentAmount: String = "",
    val newPaymentMethod: String = "TRANSFER",
    val editingPayment: BmpInvoicePaymentEntity? = null,
    val isPollingSignature: Boolean = false,
    val pollingCountdown: Int = 180,
    val pollingError: String? = null
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val invoiceRepo: BmpInvoiceRepository,
    private val clientRepo: BmpClientRepository,
    private val settingsRepo: BmpSettingsRepository,
    private val printSettingsRepo: PrintSettingsRepository,
    private val authRepository: AuthRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val invoiceId: Long = savedState.get<String>("id")?.toLongOrNull() ?: -1L

    private val _ui = MutableStateFlow(InvoiceDetailUi())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val inv = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = inv) }
            inv?.clientId?.let { _ui.update { st -> st.copy(client = clientRepo.getById(it)) } }
        }
        viewModelScope.launch {
            val settings = settingsRepo.get(tenantId)
            _ui.update { it.copy(settings = settings) }
        }
        viewModelScope.launch {
            printSettingsRepo.observe(tenantId).collect { printSettings ->
                _ui.update { it.copy(printConfig = PrintConfig.fromEntity(printSettings)) }
            }
        }
        viewModelScope.launch {
            invoiceRepo.observeProducts(invoiceId).collect { products ->
                _ui.update { it.copy(products = products) }
            }
        }
        viewModelScope.launch {
            invoiceRepo.observePayments(invoiceId).collect { payments ->
                _ui.update { it.copy(payments = payments) }
            }
        }
    }

    fun togglePayment(open: Boolean) =
        _ui.update { it.copy(showAddPayment = open, newPaymentAmount = "") }

    fun updatePayment(amount: String) =
        _ui.update { it.copy(newPaymentAmount = amount) }

    fun setMethod(method: String) =
        _ui.update { it.copy(newPaymentMethod = method) }

    fun savePayment() {
        val amt = _ui.value.newPaymentAmount.replace(",", ".").toDoubleOrNull() ?: return
        val method = _ui.value.newPaymentMethod
        if (amt <= 0) return
        viewModelScope.launch {
            invoiceRepo.recordPayment(tenantId, invoiceId, amt, method, notes = null)
            // refresh invoice header to show new status
            val inv = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = inv, showAddPayment = false, newPaymentAmount = "") }
        }
    }

    fun selectEditPayment(payment: BmpInvoicePaymentEntity) =
        _ui.update { it.copy(editingPayment = payment, newPaymentAmount = payment.paymentAmount.toString(), newPaymentMethod = payment.paymentMethod) }

    fun cancelEditPayment() =
        _ui.update { it.copy(editingPayment = null, newPaymentAmount = "", newPaymentMethod = "TRANSFER") }

    fun saveEditPayment() {
        val editing = _ui.value.editingPayment ?: return
        val amt = _ui.value.newPaymentAmount.replace(",", ".").toDoubleOrNull() ?: return
        val method = _ui.value.newPaymentMethod
        if (amt <= 0) return
        viewModelScope.launch {
            invoiceRepo.editPayment(tenantId, editing.id, amt, method, notes = editing.notes)
            val inv = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = inv, editingPayment = null, newPaymentAmount = "", newPaymentMethod = "TRANSFER") }
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            invoiceRepo.deletePayment(paymentId)
            val inv = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = inv) }
        }
    }

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun saveLocalSignature(context: Context, base64Image: String, receiverName: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isPollingSignature = true, pollingError = null) }
            val path = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val cleanBase64 = if (base64Image.contains(",")) base64Image.substringAfter(",") else base64Image
                    val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    val dir = File(context.filesDir, "signatures").apply { mkdirs() }
                    val file = File(dir, "sig_${invoiceId}.png")
                    FileOutputStream(file).use { it.write(bytes) }
                    file.absolutePath
                } catch (e: Exception) {
                    null
                }
            }
            if (path != null) {
                invoiceRepo.saveReceiverSignature(invoiceId, path, null, receiverName)
                val inv = invoiceRepo.getById(invoiceId)
                _ui.update { it.copy(invoice = inv, isPollingSignature = false) }
            } else {
                _ui.update { it.copy(isPollingSignature = false, pollingError = "Gagal menyimpan tanda tangan") }
            }
        }
    }

    fun deleteSignature() {
        viewModelScope.launch {
            val inv = _ui.value.invoice ?: return@launch
            inv.receiverSignaturePath?.let { path ->
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try { File(path).delete() } catch (_: Exception) {}
                }
            }
            invoiceRepo.saveReceiverSignature(invoiceId, null, null, "")
            val updated = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = updated) }
        }
    }

    fun startSignaturePolling() {
        stopSignaturePolling()
        _ui.update { it.copy(isPollingSignature = true, pollingCountdown = 180, pollingError = null) }
        pollingJob = viewModelScope.launch {
            var secondsLeft = 180
            while (secondsLeft > 0) {
                kotlinx.coroutines.delay(5000) // check every 5s
                secondsLeft -= 5
                _ui.update { it.copy(pollingCountdown = secondsLeft) }
                
                val result = invoiceRepo.checkReceiverSignatureRemote(invoiceId)
                if (result is BmpInvoiceRepository.RemoteSignatureResult.Success) {
                    invoiceRepo.saveReceiverSignature(invoiceId, null, result.url, result.name)
                    val updated = invoiceRepo.getById(invoiceId)
                    _ui.update { it.copy(invoice = updated, isPollingSignature = false) }
                    break
                } else if (result is BmpInvoiceRepository.RemoteSignatureResult.Error) {
                    _ui.update { it.copy(isPollingSignature = false, pollingError = result.message) }
                    break
                }
            }
            if (secondsLeft <= 0 && _ui.value.isPollingSignature) {
                _ui.update { it.copy(isPollingSignature = false, pollingError = "Waktu tunggu kadaluarsa") }
            }
        }
    }

    fun stopSignaturePolling() {
        pollingJob?.cancel()
        pollingJob = null
        _ui.update { it.copy(isPollingSignature = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopSignaturePolling()
    }
}

data class InvoiceFormUi(
    val invoice: BmpInvoiceEntity? = null,
    val productLines: List<BmpProductEntity> = emptyList(),
    val clients: List<BmpClientEntity> = emptyList(),
    val masterProducts: List<BmpMasterProductEntity> = emptyList(),
    val showClientSheet: Boolean = false,
    val showProductSheet: Boolean = false,
    val isLoading: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class InvoiceFormViewModel @Inject constructor(
    private val invoiceRepo: BmpInvoiceRepository,
    private val clientRepo: BmpClientRepository,
    private val masterRepo: BmpMasterProductRepository,
    private val authRepository: AuthRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val editingId: Long = savedState.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 } ?: -1L

    private val _ui = MutableStateFlow(
        InvoiceFormUi(
            invoice = BmpInvoiceEntity(
                tenantId = tenantId,
                title = "Invoice Baru",
                number = generateNumber(),
                slug = "",
                paymentTerms = "14 days",
                status = "DRAFT"
            )
        )
    )
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            clientRepo.observe(tenantId).collect { list ->
                _ui.update { it.copy(clients = list) }
            }
        }
        viewModelScope.launch {
            masterRepo.observe(tenantId).collect { list ->
                _ui.update { it.copy(masterProducts = list) }
            }
        }
        if (editingId > 0) viewModelScope.launch {
            val inv = invoiceRepo.getById(editingId) ?: return@launch
            _ui.update { it.copy(invoice = inv) }
            invoiceRepo.observeProducts(editingId).collect { products ->
                _ui.update { it.copy(productLines = products) }
            }
        }
    }

    fun updateInvoice(transform: (BmpInvoiceEntity) -> BmpInvoiceEntity) {
        val inv = _ui.value.invoice ?: return
        _ui.update { it.copy(invoice = transform(inv)) }
    }

    fun selectClient(client: BmpClientEntity?) {
        updateInvoice { it.copy(clientId = client?.id) }
    }

    fun addProductLine() {
        _ui.update {
            it.copy(productLines = it.productLines + BmpProductEntity(
                tenantId = tenantId,
                title = "Item baru",
                quantity = 1.0,
                price = 0.0
            ))
        }
    }

    fun updateProductLine(index: Int, transform: (BmpProductEntity) -> BmpProductEntity) {
        val lines = _ui.value.productLines.toMutableList()
        if (index in lines.indices) {
            lines[index] = transform(lines[index])
            _ui.update { it.copy(productLines = lines) }
        }
    }

    fun removeProductLine(index: Int) {
        val lines = _ui.value.productLines.toMutableList()
        if (index in lines.indices) {
            lines.removeAt(index)
            _ui.update { it.copy(productLines = lines) }
        }
    }

    fun save() {
        val inv = _ui.value.invoice ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true) }
            if (inv.id == 0L) {
                invoiceRepo.createInvoice(inv, _ui.value.productLines)
            } else {
                invoiceRepo.updateInvoice(inv, _ui.value.productLines)
            }
            _ui.update { it.copy(isLoading = false, saved = true) }
        }
    }

    private fun generateNumber(): String =
        "INV-" + System.currentTimeMillis().toString().takeLast(8)
}
