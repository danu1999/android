package com.posbah.app.ui.screens.bmp.invoices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpClientRepository
import com.posbah.app.data.repository.BmpInvoiceRepository
import com.posbah.app.data.repository.BmpMasterProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val showAddPayment: Boolean = false,
    val newPaymentAmount: String = "",
    val newPaymentMethod: String = "TRANSFER"
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val invoiceRepo: BmpInvoiceRepository,
    private val clientRepo: BmpClientRepository,
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
}

data class InvoiceFormUi(
    val invoice: BmpInvoiceEntity? = null,
    val productLines: List<BmpProductEntity> = emptyList(),
    val clients: List<BmpClientEntity> = emptyList(),
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
