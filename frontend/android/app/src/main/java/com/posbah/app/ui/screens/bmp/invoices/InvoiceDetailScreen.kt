package com.posbah.app.ui.screens.bmp.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.Formatters

@Composable
fun InvoiceDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val inv = ui.invoice

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = inv?.number ?: "Invoice",
                subtitle = ui.client?.clientName,
                onBack = onBack,
                actions = {
                    if (inv != null) {
                        TextButton(onClick = { onEdit(inv.id) }, modifier = Modifier.testTag("btn-edit-invoice")) {
                            Text("Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (inv == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Invoice tidak ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "GRAND TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            Formatters.rupiah(inv.totalAmount),
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Dibayar: ${Formatters.rupiah(inv.paidAmount)} • Sisa: ${Formatters.rupiah(inv.totalAmount - inv.paidAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "DETAIL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Status", inv.status)
                        InfoRow("Klien", ui.client?.clientName ?: "—")
                        InfoRow("Term Pembayaran", inv.paymentTerms)
                        inv.dueDate?.let { InfoRow("Jatuh Tempo", Formatters.dateLong(it)) }
                        inv.notes?.let { InfoRow("Catatan", it) }
                    }
                }
            }
            item {
                Text(
                    "ITEM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(ui.products, key = { it.id }) { p -> ProductLineRow(p) }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "PEMBAYARAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (ui.payments.isEmpty()) {
                item {
                    Text(
                        "Belum ada pembayaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(ui.payments, key = { it.id }) { pay -> PaymentRow(pay) }
            }
            item {
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    label = "+ Catat Pembayaran",
                    onClick = { viewModel.togglePayment(true) },
                    modifier = Modifier.fillMaxWidth().testTag("btn-add-payment")
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (ui.showAddPayment) {
        AlertDialog(
            onDismissRequest = { viewModel.togglePayment(false) },
            title = { Text("Catat Pembayaran") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ui.newPaymentAmount,
                        onValueChange = viewModel::updatePayment,
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("payment-amount")
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        listOf("CASH", "TRANSFER", "QRIS").forEach { m ->
                            val selected = ui.newPaymentMethod == m
                            TextButton(onClick = { viewModel.setMethod(m) }) {
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
                TextButton(onClick = viewModel::savePayment, modifier = Modifier.testTag("btn-save-payment")) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.togglePayment(false) }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ProductLineRow(p: BmpProductEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(p.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${Formatters.number(p.quantity)} ${p.unit} × ${Formatters.rupiah(p.price)}" +
                        if (p.jumlahLusin > 1.0) " × ${Formatters.number(p.jumlahLusin)} lusin" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Formatters.rupiah(p.price * p.quantity * p.jumlahLusin),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun PaymentRow(pay: BmpInvoicePaymentEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    Formatters.dateLong(pay.paymentDate),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    pay.paymentMethod,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Formatters.rupiah(pay.paymentAmount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
