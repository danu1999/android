package com.posbah.app.ui.screens.bmp.payments

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpInvoiceRepository
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.posbah.app.data.repository.BmpClientRepository
import com.posbah.app.data.repository.toBmpPaymentData

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class PaymentsListUiState(
    val editingPayment: BmpInvoicePaymentEntity? = null,
    val newPaymentAmount: String = "",
    val newPaymentMethod: String = "TRANSFER",
    val confirmDeletePaymentId: Long? = null
)

typealias BmpPaymentDto = com.posbah.app.data.repository.BmpPaymentData

data class PaymentWithContext(
    val payment: BmpPaymentDto,
    val clientName: String,
    val invoiceNumber: String
)

@HiltViewModel
class PaymentsListViewModel @Inject constructor(
    private val invoiceRepo: BmpInvoiceRepository,
    private val clientRepo: BmpClientRepository,
    private val api: com.posbah.app.data.remote.api.BmpApiService,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val tenantId = authRepository.activeTenantId().orEmpty()
    
    init {
        viewModelScope.launch {
            invoiceRepo.refreshPayments()
            clientRepo.refresh()
            invoiceRepo.refresh()
        }
    }

    val paymentsWithContext: StateFlow<UiState<List<PaymentWithContext>>> = combine(
        invoiceRepo.allPayments,
        invoiceRepo.allInvoices,
        clientRepo.allClients
    ) { rawPayments, invoices, clients ->
        val list = rawPayments.map { p ->
            val invoice = invoices.find { it.id == p.invoiceId }
            val client = clients.find { it.id == invoice?.clientId }
            PaymentWithContext(
                payment = p,
                clientName = client?.clientName ?: "Klien Tidak Dikenal",
                invoiceNumber = invoice?.number ?: "#${p.invoiceId}"
            )
        }
        UiState.Success(list)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _uiState = MutableStateFlow(PaymentsListUiState())
    val uiState = _uiState.asStateFlow()

    fun selectEditPayment(payment: BmpInvoicePaymentEntity) {
        _uiState.update {
            it.copy(
                editingPayment = payment,
                newPaymentAmount = payment.paymentAmount.toLong().toString(),
                newPaymentMethod = payment.paymentMethod
            )
        }
    }

    fun cancelEditPayment() {
        _uiState.update {
            it.copy(
                editingPayment = null,
                newPaymentAmount = "",
                newPaymentMethod = "TRANSFER"
            )
        }
    }

    fun updatePaymentAmount(amount: String) {
        _uiState.update { it.copy(newPaymentAmount = amount) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(newPaymentMethod = method) }
    }

    fun saveEditPayment() {
        val state = _uiState.value
        val editing = state.editingPayment ?: return
        val amt = state.newPaymentAmount.replace(",", ".").toDoubleOrNull() ?: return
        if (amt <= 0) return
        cancelEditPayment()
        viewModelScope.launch {
            invoiceRepo.editPayment(context, tenantId, editing.id, amt, state.newPaymentMethod, notes = editing.notes)
        }
    }

    fun selectDeletePayment(paymentId: Long) {
        _uiState.update { it.copy(confirmDeletePaymentId = paymentId) }
    }

    fun cancelDeletePayment() {
        _uiState.update { it.copy(confirmDeletePaymentId = null) }
    }

    fun confirmDeletePayment() {
        val paymentId = _uiState.value.confirmDeletePaymentId ?: return
        cancelDeletePayment()
        viewModelScope.launch {
            invoiceRepo.deletePayment(context, tenantId, paymentId)
        }
    }
}

@Composable
fun PaymentsListScreen(
    onBack: () -> Unit,
    viewModel: PaymentsListViewModel = hiltViewModel()
) {
    val state by viewModel.paymentsWithContext.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val count = (state as? UiState.Success)?.data?.size ?: 0
            PosBahTopBar(title = "Pembayaran", subtitle = "$count transaksi", onBack = onBack)
        }
    ) { padding ->
        when (val ui = state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                val list = ui.data
                if (list.isEmpty()) {
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        EmptyState("Belum ada pembayaran", "Catat pembayaran melalui detail invoice")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(list, key = { it.payment.id }) { item ->
                            val p = item.payment
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "${item.clientName} • ${item.invoiceNumber}",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                "${Formatters.dateTime(p.paymentDate)} • ${p.paymentMethod}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            Formatters.rupiah(p.paymentAmount),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                val entity = BmpInvoicePaymentEntity(
                                                    id = p.id,
                                                    tenantId = p.tenantId,
                                                    invoiceId = p.invoiceId,
                                                    paymentDate = p.paymentDate,
                                                    paymentAmount = p.paymentAmount,
                                                    paymentMethod = p.paymentMethod,
                                                    notes = p.notes
                                                )
                                                viewModel.selectEditPayment(entity)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Ubah", style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(
                                            onClick = { viewModel.selectDeletePayment(p.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Hapus", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ui.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Edit Payment Dialog
    if (uiState.editingPayment != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelEditPayment,
            title = { Text("Ubah Pembayaran") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.newPaymentAmount,
                        onValueChange = viewModel::updatePaymentAmount,
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("payment-amount")
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        listOf("CASH", "TRANSFER", "QRIS").forEach { m ->
                            val selected = uiState.newPaymentMethod == m
                            TextButton(onClick = { viewModel.setPaymentMethod(m) }) {
                                Text(
                                    m,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveEditPayment, modifier = Modifier.testTag("btn-save-payment")) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelEditPayment) { Text("Batal") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.confirmDeletePaymentId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeletePayment,
            title = { Text("Hapus Pembayaran") },
            text = { Text("Apakah Anda yakin ingin menghapus pembayaran ini? Saldo kas flow dan status invoice akan disesuaikan otomatis.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDeletePayment,
                    modifier = Modifier.testTag("btn-confirm-delete")
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeletePayment) {
                    Text("Batal")
                }
            }
        )
    }
}
