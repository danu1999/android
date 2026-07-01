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
import com.posbah.app.data.repository.BmpCashFlowRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class InvoicesListViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val invoiceRepo: BmpInvoiceRepository,
    private val clientRepo: BmpClientRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    init {
        viewModelScope.launch {
            invoiceRepo.refresh()
            clientRepo.refresh()
        }
    }

    val initialClientId: Long? = savedStateHandle.get<String>("clientId")?.toLongOrNull()
    val initialFilterOverdue = savedStateHandle.get<String>("filterOverdue")?.toBoolean() ?: false

    val invoices = invoiceRepo.observe(tenantId)
        .map { list ->
            if (initialClientId != null) {
                list.filter { it.clientId == initialClientId }
            } else {
                list
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val clients = clientRepo.observe(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _filterClientId = MutableStateFlow<Long?>(initialClientId)
    val filterClientId = _filterClientId.asStateFlow()

    private val _filterStartDate = MutableStateFlow<Long?>(null)
    val filterStartDate = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<Long?>(null)
    val filterEndDate = _filterEndDate.asStateFlow()

    private val _filterPaid = MutableStateFlow(false)
    val filterPaid = _filterPaid.asStateFlow()

    private val _filterBelumBayar = MutableStateFlow(false)
    val filterBelumBayar = _filterBelumBayar.asStateFlow()

    private val _filterPartial = MutableStateFlow(false)
    val filterPartial = _filterPartial.asStateFlow()

    private val _filterOverdue = MutableStateFlow(initialFilterOverdue)
    val filterOverdue = _filterOverdue.asStateFlow()

    val filteredInvoices = combine(
        invoiceRepo.observe(tenantId),
        _filterClientId,
        _filterStartDate,
        _filterEndDate,
        _filterOverdue
    ) { rawList, clientId, start, end, overdueOnly ->
        var list = rawList
        if (clientId != null) {
            list = list.filter { it.clientId == clientId }
        }
        if (start != null) {
            list = list.filter { it.createdAt >= start }
        }
        if (end != null) {
            list = list.filter { it.createdAt <= end + 24 * 60 * 60 * 1000L - 1 }
        }
        if (overdueOnly) {
            list = list.filter { it.status == "OVERDUE" }
        }
        list
    }.combine(
        combine(
            _filterPaid,
            _filterBelumBayar,
            _filterPartial
        ) { paid, belumBayar, partial -> Triple(paid, belumBayar, partial) }
    ) { list, (showPaid, showBelumBayar, showPartial) ->
        var res = list
        if (showPaid || showBelumBayar || showPartial) {
            res = res.filter { inv ->
                (showPaid && inv.status == "PAID") ||
                (showBelumBayar && (inv.status == "UNPAID" || inv.status == "OVERDUE" || inv.status == "DRAFT")) ||
                (showPartial && inv.status == "PARTIAL")
            }
        }
        res
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleFilterOverdue(enabled: Boolean) {
        _filterOverdue.value = enabled
    }

    fun setClientFilter(clientId: Long?) {
        _filterClientId.value = clientId
    }

    fun setDateRange(start: Long?, end: Long?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }

    fun toggleFilterPaid(enabled: Boolean) {
        _filterPaid.value = enabled
    }

    fun toggleFilterBelumBayar(enabled: Boolean) {
        _filterBelumBayar.value = enabled
    }

    fun toggleFilterPartial(enabled: Boolean) {
        _filterPartial.value = enabled
    }

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError = _deleteError.asStateFlow()

    fun clearDeleteError() { _deleteError.value = null }

    fun delete(id: Long) = viewModelScope.launch {
        val result = invoiceRepo.deleteInvoice(context, tenantId, id)
        if (result is com.posbah.app.data.repository.OnlineWriteResult.Success) {
            android.widget.Toast.makeText(context, "Invoice berhasil dihapus!", android.widget.Toast.LENGTH_SHORT).show()
        } else if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            _deleteError.value = result.message
            android.widget.Toast.makeText(context, "Gagal hapus invoice: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
        } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
            _deleteError.value = "Tidak ada koneksi internet."
            android.widget.Toast.makeText(context, "Gagal hapus invoice: Tidak ada internet", android.widget.Toast.LENGTH_LONG).show()
        }
    }
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
    val pollingError: String? = null,
    // true sesaat setelah tanda tangan berhasil diterima via link — UI harus dismiss dialog
    val signatureReceivedRemotely: Boolean = false,
    val defaultCompanyName: String = "CV. Bahtera Mulya Plastik"
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val invoiceRepo: BmpInvoiceRepository,
    private val clientRepo: BmpClientRepository,
    private val settingsRepo: BmpSettingsRepository,
    private val printSettingsRepo: PrintSettingsRepository,
    private val authRepository: AuthRepository,
    private val cashflowRepo: BmpCashFlowRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    val tenantId = authRepository.activeTenantId().orEmpty()
    private val invoiceId: Long = savedState.get<String>("id")?.toLongOrNull() ?: -1L

    private val _ui = MutableStateFlow(InvoiceDetailUi())
    val ui = _ui.asStateFlow()

    init {
        val sessionName = authRepository.getActiveSession()?.displayName
        val computedDefault = if (!sessionName.isNullOrBlank()) {
            sessionName.trim()
        } else {
            "CV. Bahtera Mulya Plastik"
        }
        _ui.update { it.copy(defaultCompanyName = computedDefault) }

        viewModelScope.launch {
            val inv = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = inv) }
            inv?.clientId?.let { _ui.update { st -> st.copy(client = clientRepo.getById(it)) } }

            if (inv != null) {
                val hasUrl = !inv.receiverSignatureUrl.isNullOrBlank()
                val fileExists = inv.receiverSignaturePath?.let { java.io.File(it).exists() } == true

                when {
                    // Skenario A: Ada URL di Room tapi file lokal tidak ada → re-download
                    hasUrl && !fileExists -> {
                        val localPath = downloadSignatureToLocal(context, inv.receiverSignatureUrl!!, invoiceId)
                        if (localPath != null) {
                            invoiceRepo.saveReceiverSignature(context, tenantId, invoiceId, localPath, inv.receiverSignatureUrl, inv.receiverNameActual ?: "")
                            val updated = invoiceRepo.getById(invoiceId)
                            _ui.update { it.copy(invoice = updated) }
                        }
                    }
                    // Skenario B: Tidak ada URL di Room DB sama sekali
                    !hasUrl -> {
                        val remoteResult = invoiceRepo.checkReceiverSignatureRemote(tenantId, invoiceId)
                        if (remoteResult is BmpInvoiceRepository.RemoteSignatureResult.Success) {
                            val localPath = downloadSignatureToLocal(context, remoteResult.url, invoiceId)
                            invoiceRepo.saveReceiverSignature(context, tenantId, invoiceId, localPath, remoteResult.url, remoteResult.name)
                            val updated = invoiceRepo.getById(invoiceId)
                            _ui.update { it.copy(invoice = updated) }
                        }
                    }
                    // Skenario C: URL ada dan file lokal ada → sudah siap cetak
                    else -> { /* tidak perlu tindakan */ }
                }
            }
        }
        viewModelScope.launch {
            settingsRepo.observe(tenantId).collect { settings ->
                _ui.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            printSettingsRepo.observe(tenantId, "BMP").collect { printSettings ->
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
            val result = invoiceRepo.recordPayment(context, tenantId, invoiceId, amt, method, notes = null)
            if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                _ui.update { it.copy(pollingError = result.message) }
                return@launch
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
                _ui.update { it.copy(pollingError = "Tidak ada koneksi internet.") }
                return@launch
            }
            // Eksekusi pemanggilan fungsi fetch ulang untuk CashFlow secara paralel
            launch {
                try {
                    cashflowRepo.fetchCashFlowFromNetwork()
                } catch (_: Exception) {}
            }
            val inv = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = inv, showAddPayment = false, newPaymentAmount = "") }
        }
    }

    fun selectEditPayment(payment: BmpInvoicePaymentEntity) =
        _ui.update { it.copy(editingPayment = payment, newPaymentAmount = payment.paymentAmount.toLong().toString(), newPaymentMethod = payment.paymentMethod) }

    fun cancelEditPayment() =
        _ui.update { it.copy(editingPayment = null, newPaymentAmount = "", newPaymentMethod = "TRANSFER") }

    fun saveEditPayment() {
        val editing = _ui.value.editingPayment ?: return
        val amt = _ui.value.newPaymentAmount.replace(",", ".").toDoubleOrNull() ?: return
        val method = _ui.value.newPaymentMethod
        if (amt <= 0) return
        viewModelScope.launch {
            val result = invoiceRepo.editPayment(context, tenantId, editing.id, amt, method, notes = editing.notes)
            if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                _ui.update { it.copy(pollingError = result.message) }
                return@launch
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
                _ui.update { it.copy(pollingError = "Tidak ada koneksi internet.") }
                return@launch
            }
            val inv = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = inv, editingPayment = null, newPaymentAmount = "", newPaymentMethod = "TRANSFER") }
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            val result = invoiceRepo.deletePayment(context, tenantId, paymentId)
            if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                _ui.update { it.copy(pollingError = result.message) }
                return@launch
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
                _ui.update { it.copy(pollingError = "Tidak ada koneksi internet.") }
                return@launch
            }
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
                val result = invoiceRepo.saveReceiverSignature(context, tenantId, invoiceId, path, null, receiverName)
                if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                    _ui.update { it.copy(isPollingSignature = false, pollingError = result.message) }
                    return@launch
                } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
                    _ui.update { it.copy(isPollingSignature = false, pollingError = "Tidak ada koneksi internet.") }
                    return@launch
                }
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
            val result = invoiceRepo.saveReceiverSignature(context, tenantId, invoiceId, null, null, "")
            if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                _ui.update { it.copy(pollingError = result.message) }
                return@launch
            }
            val updated = invoiceRepo.getById(invoiceId)
            _ui.update { it.copy(invoice = updated) }
        }
    }

    fun startSignaturePolling() {
        stopSignaturePolling()
        _ui.update { it.copy(isPollingSignature = true, pollingCountdown = 600, pollingError = null) }
        pollingJob = viewModelScope.launch {
            invoiceRepo.markAsUnsynced(invoiceId)
            var secondsLeft = 600
            while (secondsLeft > 0) {
                kotlinx.coroutines.delay(5000)
                secondsLeft -= 5
                _ui.update { it.copy(pollingCountdown = secondsLeft) }

                val result = invoiceRepo.checkReceiverSignatureRemote(tenantId, invoiceId)
                if (result is BmpInvoiceRepository.RemoteSignatureResult.Success) {
                    val localPath = downloadSignatureToLocal(context, result.url, invoiceId)
                    invoiceRepo.saveReceiverSignature(context, tenantId, invoiceId, localPath, result.url, result.name)
                    val updated = invoiceRepo.getById(invoiceId)
                    _ui.update { it.copy(invoice = updated, isPollingSignature = false, signatureReceivedRemotely = true) }
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

    private suspend fun downloadSignatureToLocal(context: Context, urlStr: String, invoiceId: Long): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var conn: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL(urlStr)
                conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                if (code in 200..299) {
                    val dir = File(context.filesDir, "signatures").apply { mkdirs() }
                    val file = File(dir, "sig_${invoiceId}.png")
                    conn.inputStream.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.absolutePath
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun stopSignaturePolling() {
        pollingJob?.cancel()
        pollingJob = null
        _ui.update { it.copy(isPollingSignature = false) }
    }

    fun deleteInvoice(onDone: () -> Unit) {
        viewModelScope.launch {
            val result = invoiceRepo.deleteInvoice(context, tenantId, invoiceId)
            if (result is com.posbah.app.data.repository.OnlineWriteResult.Success) {
                android.widget.Toast.makeText(context, "Invoice berhasil dihapus!", android.widget.Toast.LENGTH_SHORT).show()
                onDone()
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                android.widget.Toast.makeText(context, "Gagal hapus invoice: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
                android.widget.Toast.makeText(context, "Gagal hapus invoice: Tidak ada internet", android.widget.Toast.LENGTH_LONG).show()
            }
        }
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
    val saved: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class InvoiceFormViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
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

    fun addProductLine(masterProduct: BmpMasterProductEntity) {
        _ui.update {
            it.copy(productLines = it.productLines + BmpProductEntity(
                tenantId = tenantId,
                masterItemID = masterProduct.id,
                title = masterProduct.title,
                description = masterProduct.description,
                quantity = 1.0,
                jumlahLusin = 1.0,
                unit = masterProduct.unit,
                price = masterProduct.price
            ))
        }
    }

    fun addBlankProductLine() {
        _ui.update {
            it.copy(productLines = it.productLines + BmpProductEntity(
                tenantId = tenantId,
                title = "Item baru",
                quantity = 1.0,
                jumlahLusin = 1.0,
                unit = "pcs",
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
            val result = if (inv.id == 0L) {
                val (_, r) = invoiceRepo.createInvoice(context, inv, _ui.value.productLines)
                r
            } else {
                invoiceRepo.updateInvoice(context, inv, _ui.value.productLines)
            }
            if (result is com.posbah.app.data.repository.OnlineWriteResult.Success) {
                android.widget.Toast.makeText(context, "Invoice berhasil disimpan!", android.widget.Toast.LENGTH_SHORT).show()
                _ui.update { it.copy(saved = true) }
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                android.widget.Toast.makeText(context, "Gagal simpan invoice: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
                android.widget.Toast.makeText(context, "Gagal simpan invoice: Tidak ada internet", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun clearSaveError() { _ui.update { it.copy(saveError = null) } }

    private fun generateNumber(): String =
        "INV-" + System.currentTimeMillis().toString().takeLast(8)
}
