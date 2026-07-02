package com.posbah.app.ui.screens.bmp.invoices

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.posbah.app.ui.print.PrintConfig
import com.posbah.app.ui.print.DocPrintConfig
import com.posbah.app.ui.print.HeaderAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.Formatters
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.posbah.app.ui.components.SignatureCanvas

@Composable
fun InvoiceDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val inv = ui.invoice
    val context = LocalContext.current

    var showLocalSignDialog by remember { mutableStateOf(false) }
    var showShareLinkDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Auto-dismiss dialog dan tampil toast saat tanda tangan berhasil diterima via link
    LaunchedEffect(ui.signatureReceivedRemotely) {
        if (ui.signatureReceivedRemotely) {
            viewModel.stopSignaturePolling()
            showShareLinkDialog = false
            Toast.makeText(
                context,
                "✅ Tanda tangan berhasil diterima!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = inv?.number ?: "Invoice",
                subtitle = ui.client?.clientName,
                onBack = onBack,
                actions = {
                    if (inv != null) {
                        IconButton(
                            onClick = {
                                val html = generateInvoiceHtml(context, inv, ui.products, ui.client, ui.settings, ui.printConfig.invoice, ui.printConfig, defaultCompanyName = ui.defaultCompanyName)
                                printHtml(context, html, "Invoice_${inv.number}", ui.printConfig.invoice.isColor, ui.printConfig.invoice.templateType == "TRADITIONAL")
                            },
                            modifier = Modifier.testTag("btn-print-top")
                        ) {
                            Icon(Icons.Outlined.Print, contentDescription = "Cetak")
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("btn-delete-invoice")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
                        InfoRow("Status", Formatters.invoiceStatus(inv.status))
                        InfoRow("Klien", ui.client?.clientName ?: "—")
                        InfoRow("Term Pembayaran", inv.paymentTerms)
                        inv.dueDate?.let { InfoRow("Jatuh Tempo", Formatters.dateLong(it)) }
                        inv.notes?.let { InfoRow("Catatan", it) }
                    }
                }
            }

            // Panel Aksi Cetak & Ekspor
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EKSPOR & CETAK DOKUMEN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ListItem(
                            headlineContent = { Text("Cetak JPG", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Ekspor invoice dan bagikan ke WhatsApp/Email", style = MaterialTheme.typography.bodySmall) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    val html = generateInvoiceHtml(context, inv, ui.products, ui.client, ui.settings, ui.printConfig.jpg, ui.printConfig, defaultCompanyName = ui.defaultCompanyName)
                                    printColoredJpg(context, html, "Invoice_${inv.number}")
                                }
                                .background(Color.Transparent)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                        ListItem(
                            headlineContent = { Text("Cetak Invoice", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Cetak fisik invoice resmi luring (Printer)", style = MaterialTheme.typography.bodySmall) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.Print,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    val html = generateInvoiceHtml(context, inv, ui.products, ui.client, ui.settings, ui.printConfig.invoice, ui.printConfig, defaultCompanyName = ui.defaultCompanyName)
                                    printHtml(context, html, "Invoice_${inv.number}", ui.printConfig.invoice.isColor, ui.printConfig.invoice.templateType == "TRADITIONAL")
                                }
                                .background(Color.Transparent)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                        ListItem(
                            headlineContent = { Text("Cetak Surat Jalan", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Cetak surat pengantar logistik tanpa nominal harga", style = MaterialTheme.typography.bodySmall) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    val html = generateSuratJalanHtml(context, inv, ui.products, ui.client, ui.settings, ui.printConfig.sj, ui.printConfig, defaultCompanyName = ui.defaultCompanyName)
                                    printHtml(context, html, "SuratJalan_${inv.number}", ui.printConfig.sj.isColor, ui.printConfig.sj.templateType == "TRADITIONAL")
                                }
                                .background(Color.Transparent)
                        )
                    }
                }
            }

            item {
                if (inv != null) {
                    ReceiverSignatureSection(
                        invoice = inv,
                        uiState = ui,
                        onSignOnDevice = { showLocalSignDialog = true },
                        onShareLink = { showShareLinkDialog = true },
                        onDeleteSignature = { viewModel.deleteSignature() }
                    )
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
                items(ui.payments, key = { it.id }) { pay ->
                    PaymentRow(
                        pay = pay,
                        onEdit = { viewModel.selectEditPayment(pay) },
                        onDelete = { viewModel.deletePayment(pay.id) }
                    )
                }
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

    if (ui.editingPayment != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelEditPayment,
            title = { Text("Ubah Pembayaran") },
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
                TextButton(onClick = viewModel::saveEditPayment, modifier = Modifier.testTag("btn-save-payment")) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelEditPayment) { Text("Batal") }
            }
        )
    }

    if (showLocalSignDialog && inv != null) {
        var tempBase64 by remember { mutableStateOf<String?>(null) }
        var receiverName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showLocalSignDialog = false },
            title = { Text("Tanda Tangan di HP Ini") },
            text = {
                Column {
                    OutlinedTextField(
                        value = receiverName,
                        onValueChange = { receiverName = it },
                        label = { Text("Nama Penerima") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("receiver-name-field")
                    )
                    Spacer(Modifier.height(12.dp))
                    SignatureCanvas(
                        initialSignatureBase64 = null,
                        onSignatureSaved = { base64 ->
                            tempBase64 = base64
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = receiverName.isNotBlank() && tempBase64 != null,
                    onClick = {
                        tempBase64?.let { base64 ->
                            viewModel.saveLocalSignature(context, base64, receiverName)
                        }
                        showLocalSignDialog = false
                    },
                    modifier = Modifier.testTag("btn-confirm-local-sig")
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showLocalSignDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showShareLinkDialog && inv != null) {
        val shareUrl = remember(inv.id) {
            com.posbah.app.util.SignatureLinkGenerator.generateShareLink(viewModel.tenantId, inv.id, durationMinutes = 10)
        }
        
        LaunchedEffect(Unit) {
            viewModel.startSignaturePolling()
        }
        
        AlertDialog(
            onDismissRequest = {
                viewModel.stopSignaturePolling()
                showShareLinkDialog = false
            },
            title = { Text("Bagikan Link Tanda Tangan") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bagikan link di bawah ke WhatsApp penerima. Link hanya berlaku selama 10 menit dan akan kadaluarsa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = shareUrl,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("URL Link TTD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    if (ui.isPollingSignature) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Menunggu tanda tangan... (${ui.pollingCountdown} detik tersisa)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ui.pollingError?.let { err ->
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Silakan tanda tangani tanda terima faktur ${inv.number} Anda di sini: $shareUrl")
                        }
                        context.startActivity(Intent.createChooser(intent, "Bagikan Link Tanda Tangan"))
                    },
                    modifier = Modifier.testTag("btn-send-whatsapp")
                ) { Text("Kirim Link") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.stopSignaturePolling()
                        showShareLinkDialog = false
                    }
                ) { Text("Batal") }
            }
        )
    }

    if (showDeleteConfirm && inv != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Invoice") },
            text = { Text("Apakah Anda yakin ingin menghapus invoice ini? Tindakan ini juga akan menghapus data pembayaran dan menyesuaikan kas terkait secara otomatis.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteInvoice {
                            showDeleteConfirm = false
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("btn-confirm-delete")
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
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
                if (!p.description.isNullOrBlank()) {
                    Text(
                        p.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                }
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
private fun PaymentRow(
    pay: BmpInvoicePaymentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Ubah", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Hapus", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun generateInvoiceHtml(
    context: Context,
    invoice: com.posbah.app.data.local.entities.BmpInvoiceEntity,
    products: List<com.posbah.app.data.local.entities.BmpProductEntity>,
    client: com.posbah.app.data.local.entities.BmpClientEntity?,
    settings: com.posbah.app.data.local.entities.BmpSettingsEntity?,
    printConfig: DocPrintConfig,
    globalConfig: PrintConfig,
    defaultCompanyName: String = "CV. Bahtera Mulya Plastik"
): String {
    val logoBase64 = if (printConfig.useLogo) com.posbah.app.ui.print.ReceiptPrinter.getUriOrAssetBase64(context, globalConfig.logoPath, "logo.jpg") else ""

    val companyName = if (!settings?.clientName.isNullOrBlank()) settings!!.clientName else defaultCompanyName
    val companyAddress = settings?.addressLine1 ?: "Sidoarjo, Jawa Timur"
    val companyPhone = settings?.phoneNumber ?: "082652626237"
    val companyEmail = settings?.emailAddress ?: "bahteramulyap@gmail.com"

    val clientName = client?.clientName ?: "-"
    val clientAddress = client?.addressLine1 ?: "-"
    val clientPhone = client?.phoneNumber ?: "-"

    val itemsHtml = StringBuilder()
    products.forEachIndexed { index, p ->
        val subtotal = p.price * p.quantity * p.jumlahLusin
        val satuanVal = "${Formatters.number(p.jumlahLusin)} ${if (p.unit.lowercase() == "lusin" || p.unit == "-") "Lusin" else p.unit}"
        val descHtml = if (!p.description.isNullOrBlank()) "<br/><span style=\"font-size: 8px; color: #555; font-weight: normal;\">${p.description}</span>" else ""
        itemsHtml.append("""
            <tr class="item-row">
                <td style="text-align: center;">${index + 1}</td>
                <td><strong>${p.title}</strong>$descHtml</td>
                <td style="text-align: center;">$satuanVal</td>
                <td style="text-align: center;">${Formatters.number(p.quantity)}</td>
                <td style="text-align: right;"><strong>${Formatters.rupiah(p.price)}</strong></td>
                <td style="text-align: right;"><strong>${Formatters.rupiah(subtotal)}</strong></td>
            </tr>
        """.trimIndent())
    }

    val isColor = printConfig.isColor
    val themeColor = if (isColor) "#1E3A8A" else "#000000"
    val accentBg = if (isColor) "#EFF6FF" else "#ffffff"

    val isTraditional = printConfig.templateType == "TRADITIONAL"
    val logoMaxHeight = if (isTraditional) "32px" else "58px"
    val logoCellWidth = if (isTraditional) "50px" else "75px"
    val logoPaddingRight = if (isTraditional) "8px" else "12px"

    val pageSizeCss = if (isTraditional) "size: 240mm 279mm; margin: 0.5cm;" else "size: 210mm 297mm; margin: 1cm;"
    val printContainerCss = if (isTraditional) """
        .print-container {
            width: 215mm;
            height: auto;
            box-sizing: border-box;
            position: relative;
        }
    """.trimIndent() else """
        .print-container {
            width: 100%;
            height: auto;
            box-sizing: border-box;
            position: relative;
        }
    """.trimIndent()

    val headerHtml = """
        <table style="width: 100%; border-bottom: 1.5px solid #000000; margin-bottom: 8px; padding-bottom: 5px;">
            <tr>
                <td style="width: 60%; vertical-align: top; text-align: left; padding-left: 0;">
                    <table style="width: 100%; border: none; border-collapse: collapse;">
                        <tr>
                            ${if (logoBase64.isNotEmpty()) """
                            <td style="width: $logoCellWidth; vertical-align: top; padding-right: $logoPaddingRight;">
                                <img id="logo-img" src="$logoBase64" alt="Logo" style="max-height: $logoMaxHeight; width: auto; display: block;" onerror="this.style.display='none';">
                            </td>
                            """ else ""}
                            <td style="vertical-align: top;">
                                <h1 style="margin: 0; color: #000000; font-size: 14px; line-height: 1.1; font-weight: bold;">
                                    $companyName
                                </h1>
                                <p style="margin: 3px 0 1px 0; font-size: 9px; color: #333333;">
                                    $companyAddress
                                </p>
                                <p style="margin: 1px 0; font-size: 9px; color: #333333;">
                                    Telp: $companyPhone<br>Email: $companyEmail
                                </p>
                            </td>
                        </tr>
                    </table>
                </td>

                <td style="width: 40%; vertical-align: top; text-align: right; padding-right: 0;">
                    <div class="nota-title">
                        <h1 style="margin: 0; font-size: 16px; letter-spacing: 1px; color: #000000; line-height: 1; font-weight: bold;">FAKTUR</h1>
                        <p style="margin: 4px 0 1px 0; font-size: 9px; line-height: 1.3;">
                            No: #<span id="invoice-number-val">${invoice.number}</span><br>
                            Tgl: <span id="created-date-val">${Formatters.dateLong(invoice.createdAt)}</span><br>
                            Jatuh Tempo: <span id="due-date-val">${invoice.dueDate?.let { Formatters.dateLong(it) } ?: "-"}</span>
                        </p>
                    </div>
                </td>
            </tr>
        </table>
    """.trimIndent()

    val signatureHtml = if (printConfig.useSignature) {
        val localSigBase64 = getFileBase64(invoice.receiverSignaturePath)
        val receiverSig = if (localSigBase64.isNotEmpty()) {
            localSigBase64
        } else if (!invoice.receiverSignatureUrl.isNullOrBlank()) {
            invoice.receiverSignatureUrl
        } else {
            client?.receiverSignatureUrl ?: ""
        }
        val receiverName = if (!invoice.receiverNameActual.isNullOrBlank()) {
            invoice.receiverNameActual
        } else if (!client?.receiverNameActual.isNullOrBlank()) {
            client.receiverNameActual
        } else {
            printConfig.signatureReceiverName
        }
        """
        <table class="signature-section" style="width: 100%; margin-top: 10px; page-break-inside: avoid; border-collapse: collapse; border: none;">
            <tr>
                <td style="width: 40%; text-align: center; vertical-align: top; font-size: 9px; border: none;">
                    Penerima / Pembeli,<br>
                    ${if (!receiverSig.isNullOrBlank()) """
                    <img src="$receiverSig" style="height: 40px; width: auto; margin: 3px auto; display: block;" />
                    """ else """
                    <div style="height: 40px;"></div>
                    """}
                    ( <strong>${receiverName.ifBlank { " _____________________ " }}</strong> )
                </td>
                <td style="width: 20%; border: none;"></td>
                <td style="width: 40%; text-align: center; vertical-align: top; font-size: 9px; border: none;">
                    Hormat Kami,<br>
                    ${if (printConfig.signatureDrawnBase64?.isNotEmpty() == true) """
                    <img id="ttd-img" src="${printConfig.signatureDrawnBase64}" style="height: 40px; width: auto; margin: 3px auto; display: block;" onerror="handleTtdError(this)" />
                    """ else """
                    <div id="ttd-container" style="height: 40px;"></div>
                    """}
                    ( <strong>${printConfig.signatureSenderName.ifBlank { " _____________________ " }}</strong> )
                </td>
            </tr>
        </table>
        """
    } else ""

    val dynamicUnitHeader = when {
        products.any { it.unit.equals("Lusin", ignoreCase = true) } -> "Lusin"
        products.any { it.unit.equals("Kg", ignoreCase = true) } -> "Kg"
        else -> "Satuan"
    }

    val tableHeaderHtml = """
        <table class="table-items">
            <thead>
                <tr>
                    <th style="width: 5%; text-align: center;">No</th>
                    <th style="width: 40%;">Deskripsi Barang</th>
                    <th style="width: 15%; text-align: center;">$dynamicUnitHeader</th>
                    <th style="width: 10%; text-align: center;">Jumlah</th>
                    <th style="width: 15%; text-align: right;">Harga</th>
                    <th style="width: 15%; text-align: right;">Total</th>
                </tr>
            </thead>
            <tbody>
                $itemsHtml
            </tbody>
        </table>
    """.trimIndent()

    return """
        <!DOCTYPE html>
        <html lang="id">
        <head>
            <meta charset="utf-8">
            <title>Faktur - $companyName</title>
            <style>
                @page {
                    $pageSizeCss
                }
                $printContainerCss
                body {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    color: #000000;
                    margin: 0;
                    padding: 0;
                    font-size: 10px;
                    line-height: 1.2;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .nota-title h1 {
                    margin: 0;
                    font-size: 16px;
                    letter-spacing: 1px;
                    color: #000000;
                    font-weight: bold;
                }
                .client-info {
                    margin-bottom: 5px;
                    font-weight: bold;
                    text-transform: uppercase;
                    font-size: 9px;
                    color: #000000;
                }
                .table-items {
                    margin-bottom: 8px;
                    border-collapse: collapse;
                    width: 100%;
                }
                .table-items th {
                    background-color: ${if (isColor) themeColor else "#ffffff"};
                    color: ${if (isColor) "white" else "#000000"};
                    padding: 4px 5px;
                    text-align: left;
                    text-transform: uppercase;
                    font-size: 11px;
                    border-top: 1.5px solid #000000;
                    border-bottom: 1.5px solid #000000;
                }
                .table-items td {
                    padding: 3px 5px;
                    border-bottom: 1px solid #dddddd;
                    font-size: 10px;
                    color: #000000;
                }
                .stempel-lunas {
                    color: #000000;
                    border: 2px solid #000000;
                    padding: 3px 6px;
                    font-size: 12px;
                    font-weight: bold;
                    text-align: center;
                    display: inline-block;
                    letter-spacing: 1px;
                }
                
                /* Overrides Khusus Traditional (Continuous 240x279mm) */
                .print-container.traditional {
                    font-size: 12px !important;
                    line-height: 1.3 !important;
                }
                .print-container.traditional h1 {
                    font-size: 16px !important;
                }
                .print-container.traditional .company-name, 
                .print-container.traditional .nota-title h1 {
                    font-size: 16px !important;
                }
                .print-container.traditional .client-info {
                    font-size: 12px !important;
                    margin-bottom: 12px !important;
                    padding-bottom: 6px !important;
                }
                .print-container.traditional .client-info span {
                    font-size: 12px !important;
                }
                .print-container.traditional .table-items {
                    margin-bottom: 12px !important;
                }
                .print-container.traditional .table-items th {
                    font-size: 12px !important;
                    padding: 6px 8px !important;
                }
                .print-container.traditional .table-items td {
                    font-size: 12px !important;
                    padding: 6px 8px !important;
                }
                .print-container.traditional .totals-table td {
                    font-size: 12px !important;
                    padding: 4px 6px !important;
                }
                .print-container.traditional .signature-section {
                    margin-top: 15px !important;
                }
                .print-container.traditional .signature-section td {
                    font-size: 12px !important;
                }
                .print-container.traditional .signature-section img {
                    height: 50px !important;
                }
                .print-container.traditional .sig-placeholder {
                    height: 50px !important;
                }
                .print-container.traditional .bank-info-box {
                    font-size: 12px !important;
                    margin-bottom: 8px !important;
                    padding-top: 4px !important;
                }
                .print-container.traditional .stempel-lunas {
                    font-size: 12px !important;
                    padding: 2px 4px !important;
                }
            </style>
        </head>
        <body>
            <div class="print-container ${if (isTraditional) "traditional" else ""}">
            $headerHtml

            <div class="client-info" style="border-bottom: 1px solid #eeeeee; padding-bottom: 4px; margin-bottom: 8px;">
                KEPADA YTH: 
                <span style="font-size: 10px; font-weight: bold; margin-left: 5px;">$clientName</span>
                <span style="font-weight: normal; font-size: 9px; text-transform: capitalize; color: #444444; margin-left: 10px;">
                    $clientAddress
                </span>
            </div>

            $tableHeaderHtml

            <table style="margin-top: 5px; width: 100%; page-break-inside: avoid; border-collapse: collapse; border: none;">
                <tr>
                    <td style="width: 50%; vertical-align: top; padding-right: 15px; border: none;">
                        ${if (globalConfig.bankOwnerName.isNotBlank() && globalConfig.bankAccountNumber.isNotBlank()) """
                        <div style="font-size: 9px; border-top: 1px solid #000000; padding-top: 4px; margin-bottom: 5px; line-height: 1.3;">
                            <strong>INFO PEMBAYARAN:</strong> ${globalConfig.bankName}: ${globalConfig.bankAccountNumber} a/n ${globalConfig.bankOwnerName}
                        </div>
                        """ else ""}

                        ${if (!invoice.notes.isNullOrBlank()) """
                        <div style="font-size: 8px; border-top: 1px solid #eeeeee; padding-top: 4px;">
                            <strong>CATATAN:</strong> ${invoice.notes}
                        </div>
                        """ else ""}
                    </td>
                    
                    <td style="width: 50%; vertical-align: top; border: none;">
                        <table style="width: 100%; font-size: 10px; line-height: 1.4; border-collapse: collapse;">
                            <tr style="border-bottom: 1px solid #000000;">
                                <td style="text-align: left; font-weight: bold; padding: 2px 0;">SUBTOTAL</td>
                                <td style="text-align: right; padding: 2px 0; font-weight: bold;">${Formatters.rupiah(invoice.totalAmount)}</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #000000;">
                                <td style="text-align: left; font-weight: bold; padding: 2px 0;">Telah Dibayar</td>
                                <td style="text-align: right; padding: 2px 0; font-weight: bold; color: ${if (isColor) "#10B981" else "#000000"};">${Formatters.rupiah(invoice.paidAmount)}</td>
                            </tr>
                            <tr style="border-top: 1px solid #000000;">
                                <td style="text-align: left; font-weight: bold; color: #000000; padding: 2px 0;">SISA TAGIHAN</td>
                                <td style="text-align: right; font-weight: bold; color: ${if (isColor) "#EF4444" else "#000000"}; padding: 2px 0;">
                                    ${Formatters.rupiah(invoice.totalAmount - invoice.paidAmount)}
                                </td>
                            </tr>

                            <tr>
                                <td style="text-align: left; font-weight: bold; color: #000000; padding: 4px 0; vertical-align: middle;">STATUS</td>
                                <td style="text-align: right; padding: 4px 0; vertical-align: middle;">
                                    <div id="status-stamp"></div>
                                    <div id="status-raw" style="display: none;">${invoice.status}</div>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>

            $signatureHtml
            </div>

            <script>
            window.onload = function() {
                checkTtdAndLogo();
                resolveStatusStamp();

                var rows = document.querySelectorAll('.table-items tbody tr');
                var rowCount = rows.length;

                var body = document.body;
                var logoImg = document.getElementById('logo-img');
                var ttdContainer = document.getElementById('ttd-container');
                var tableCells = document.querySelectorAll('.table-items td');
                var tableHeaders = document.querySelectorAll('.table-items th');

                var isTrad = document.querySelector('.print-container').classList.contains('traditional');
                var baseFontSize = isTrad ? 12 : 10;
                var paddingSize = isTrad ? 6 : 3;
                var logoHeight = isTrad ? 45 : 35;
                var ttdHeight = isTrad ? 50 : 40;

                var scale = 1.0;
                if (isTrad) {
                    if (rowCount > 8) {
                        scale = Math.max(0.7, 1 - (rowCount - 8) * 0.05);
                    }
                } else {
                    if (rowCount > 3) {
                        scale = Math.max(0.65, 1 - (rowCount - 3) * 0.08);
                    }
                }
                
                body.style.fontSize = (baseFontSize * scale) + 'px';
                if (logoImg) {
                    logoImg.style.maxHeight = Math.round(logoHeight * scale) + 'px';
                }
                if (ttdContainer) {
                    ttdContainer.style.height = Math.round(ttdHeight * scale) + 'px';
                }
                
                tableHeaders.forEach(function(th) {
                    th.style.fontSize = ((isTrad ? 12 : 11) * scale) + 'px';
                    th.style.padding = (paddingSize * scale) + 'px 4px';
                });
                tableCells.forEach(function(td) {
                    td.style.fontSize = ((isTrad ? 11 : 10) * scale) + 'px';
                    td.style.padding = (paddingSize * scale) + 'px 4px';
                });
            };

            function handleTtdError(img) {
                img.style.display = 'none';
            }

            function checkTtdAndLogo() {
                var ttdImg = document.getElementById('ttd-img');
                if (ttdImg) {
                    var src = ttdImg.getAttribute('src');
                    if (!src || src.indexOf('ttd_path') !== -1 || src.trim() === '') {
                        ttdImg.style.display = 'none';
                    }
                }
                var logoImg = document.getElementById('logo-img');
                if (logoImg) {
                    var src = logoImg.getAttribute('src');
                    if (!src || src.indexOf('logo_path') !== -1 || src.trim() === '') {
                        logoImg.style.display = 'none';
                    }
                }
            }

            function resolveStatusStamp() {
                var rawStatusEl = document.getElementById('status-raw');
                var stampEl = document.getElementById('status-stamp');
                if (!rawStatusEl || !stampEl) return;
                
                var status = rawStatusEl.textContent.trim().toUpperCase();
                var html = '';
                
                if (status === 'PAID' || status === 'LUNAS') {
                    html = '<div class="stempel-lunas">LUNAS</div>';
                    var dueDateVal = document.getElementById('due-date-val');
                    if (dueDateVal && dueDateVal.textContent.trim()) {
                        html += '<div style="font-size: 8px; color: #000000; margin-top: 1px; font-weight: bold;">Lunas Pada: ' + dueDateVal.textContent.trim() + '</div>';
                    }
                } else if (status === 'PARTIAL' || status === 'CICIL' || status === 'BELUM LUNAS (CICIL)') {
                    html = '<div style="color: #000000; font-weight: bold; font-size: 8px; text-transform: uppercase; border: 1px solid #000000; padding: 2px 4px; display: inline-block; border-radius: 2px;">CICIL</div>';
                } else {
                    html = '<div style="color: #000000; font-weight: bold; text-transform: uppercase; border: 1px solid #000000; padding: 2px 4px; display: inline-block; border-radius: 2px;">BELUM BAYAR</div>';
                }
                stampEl.innerHTML = html;
            }
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun generateSuratJalanHtml(
    context: Context,
    invoice: com.posbah.app.data.local.entities.BmpInvoiceEntity,
    products: List<com.posbah.app.data.local.entities.BmpProductEntity>,
    client: com.posbah.app.data.local.entities.BmpClientEntity?,
    settings: com.posbah.app.data.local.entities.BmpSettingsEntity?,
    printConfig: DocPrintConfig,
    globalConfig: PrintConfig,
    defaultCompanyName: String = "CV. Bahtera Mulya Plastik"
): String {
    val logoBase64 = if (printConfig.useLogo) com.posbah.app.ui.print.ReceiptPrinter.getUriOrAssetBase64(context, globalConfig.logoPath, "logo.jpg") else ""
    val isColor = printConfig.isColor

    val isTraditional = printConfig.templateType == "TRADITIONAL"
    val logoMaxHeight = if (isTraditional) "32px" else "58px"
    val logoCellWidth = if (isTraditional) "50px" else "75px"
    val logoPaddingRight = if (isTraditional) "8px" else "12px"

    val pageSizeCss = if (isTraditional) "size: 240mm 279mm; margin: 0.5cm;" else "size: 210mm 297mm; margin: 1cm;"
    val printContainerCss = if (isTraditional) """
        .print-container {
            width: 215mm;
            height: auto;
            box-sizing: border-box;
            position: relative;
        }
    """.trimIndent() else """
        .print-container {
            width: 100%;
            height: auto;
            box-sizing: border-box;
            position: relative;
        }
    """.trimIndent()

    val companyName = if (!settings?.clientName.isNullOrBlank()) settings!!.clientName else defaultCompanyName
    val companyAddress = settings?.addressLine1 ?: "Sidoarjo, Jawa Timur"
    val companyPhone = settings?.phoneNumber ?: "082652626237"
    val companyEmail = settings?.emailAddress ?: "bahteramulyap@gmail.com"

    val clientName = client?.clientName ?: "-"
    val clientAddress = client?.addressLine1 ?: "-"
    val clientPhone = client?.phoneNumber ?: "-"

    val itemsHtml = StringBuilder()
    products.forEachIndexed { index, p ->
        val satuanVal = "${Formatters.number(p.jumlahLusin)} ${if (p.unit.lowercase() == "lusin" || p.unit == "-") "Lusin" else p.unit}"
        val descHtml = if (!p.description.isNullOrBlank()) "<br/><span style=\"font-size: 9px; color: #555; font-weight: normal;\">${p.description}</span>" else ""
        itemsHtml.append("""
            <tr>
                <td style="text-align: center;">${index + 1}</td>
                <td><strong>${p.title}</strong>$descHtml</td>
                <td style="text-align: center;">$satuanVal</td>
                <td style="text-align: center;">${Formatters.number(p.quantity)}</td>
            </tr>
        """.trimIndent())
    }

    val headerHtml = """
        <table style="width: 100%; border-bottom: 1.5px solid #000000; margin-bottom: 8px; padding-bottom: 5px;">
            <tr>
                <td style="width: 60%; vertical-align: top; text-align: left; padding-left: 0;">
                    <table style="width: 100%; border: none; border-collapse: collapse;">
                        <tr>
                            ${if (logoBase64.isNotEmpty()) """
                            <td style="width: $logoCellWidth; vertical-align: top; padding-right: $logoPaddingRight;">
                                <img id="logo-img" src="$logoBase64" alt="Logo" style="max-height: $logoMaxHeight; width: auto; display: block;" onerror="this.style.display='none';">
                            </td>
                            """ else ""}
                            <td style="vertical-align: top;">
                                <h1 style="margin: 0; color: #000000; font-size: 14px; line-height: 1.1; font-weight: bold;">
                                    $companyName
                                </h1>
                                <p style="margin: 3px 0 1px 0; font-size: 9px; color: #333333;">
                                    $companyAddress
                                </p>
                                <p style="margin: 1px 0; font-size: 9px; color: #333333;">
                                    Telp: $companyPhone | Email: $companyEmail
                                </p>
                            </td>
                        </tr>
                    </table>
                </td>

                <td style="width: 40%; vertical-align: top; text-align: right; padding-right: 0;">
                    <div class="nota-title">
                        <h1 style="margin: 0; font-size: 16px; letter-spacing: 1px; color: #000000; line-height: 1; font-weight: bold;">SURAT JALAN</h1>
                        <p style="margin: 4px 0 1px 0; font-size: 9px; line-height: 1.3;">
                            No. SJ: <strong>SJ-${invoice.number}</strong><br>
                            Tanggal: ${Formatters.dateLong(System.currentTimeMillis())}
                        </p>
                    </div>
                </td>
            </tr>
        </table>
    """.trimIndent()

    val signatureHtml = if (printConfig.useSignature) {
        val localSigBase64 = getFileBase64(invoice.receiverSignaturePath)
        val receiverSig = if (localSigBase64.isNotEmpty()) {
            localSigBase64
        } else if (!invoice.receiverSignatureUrl.isNullOrBlank()) {
            invoice.receiverSignatureUrl
        } else {
            client?.receiverSignatureUrl ?: ""
        }
        val receiverName = if (!invoice.receiverNameActual.isNullOrBlank()) {
            invoice.receiverNameActual
        } else if (!client?.receiverNameActual.isNullOrBlank()) {
            client.receiverNameActual
        } else {
            printConfig.signatureReceiverName
        }
        """
        <table class="signature-section" style="width: 100%; margin-top: 15px; text-align: center; font-size: 10px; page-break-inside: avoid; border-collapse: collapse; border: none;">
            <tr>
                <td style="width: 33%; vertical-align: top; border: none; text-align: center;">
                    <strong>Penerima / Pembeli</strong>
                    <br>
                    ${if (!receiverSig.isNullOrBlank()) """
                    <img src="$receiverSig" style="height: 40px; width: auto; margin: 3px auto; display: block;" />
                    """ else """
                    <div style="height: 40px;"></div>
                    """}
                    ( <strong>${receiverName.ifBlank { " _____________________ " }}</strong> )
                </td>
                <td style="width: 33%; vertical-align: top; border: none; text-align: center;">
                    <strong>Sopir / Kurir</strong>
                    <br><br>
                    <div style="height: 40px;"></div>
                    ( <strong> _____________________ </strong> )
                </td>
                <td style="width: 34%; vertical-align: top; border: none; text-align: center;">
                    <strong>Hormat Kami,</strong>
                    <br>
                    ${if (printConfig.signatureDrawnBase64?.isNotEmpty() == true) """
                    <img src="${printConfig.signatureDrawnBase64}" style="height: 40px; width: auto; margin: 3px auto; display: block;" />
                    """ else """
                    <div style="height: 40px;"></div>
                    """}
                    ( <strong>Bag. Gudang</strong> )
                </td>
            </tr>
        </table>
        """
    } else {
        """
        <table style="margin-top: 15px; text-align: center; width: 100%; font-size: 10px; page-break-inside: avoid; border-collapse: collapse; border: none;">
            <tr>
                <td style="width: 33%; vertical-align: top; border: none; text-align: center;">
                    <strong>Penerima / Pembeli</strong>
                    <br><br><br>
                    ( .......................... )
                </td>
                <td style="width: 33%; vertical-align: top; border: none; text-align: center;">
                    <strong>Sopir / Kurir</strong>
                    <br><br><br>
                    ( .......................... )
                </td>
                <td style="width: 34%; vertical-align: top; border: none; text-align: center;">
                    <strong>Hormat Kami,</strong>
                    <div style="height: 30px; margin: 3px 0;"></div>
                    <strong>Bag. Gudang</strong>
                </td>
            </tr>
        </table>
        """
    }

    return """
        <!DOCTYPE html>
        <html lang="id">
        <head>
            <meta charset="utf-8">
            <title>Surat Jalan - $companyName</title>
            <style>
                @page {
                    $pageSizeCss
                }
                $printContainerCss
                body {
                    font-family: Helvetica, Arial, sans-serif;
                    color: #333333;
                    margin: 0;
                    padding: 0;
                    font-size: 10px;
                    line-height: 1.2;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .nota-title h1 {
                    margin: 0;
                    font-size: 16px;
                    letter-spacing: 1px;
                    color: #000000;
                    font-weight: bold;
                }
                .client-info {
                    margin-bottom: 5px;
                    font-weight: bold;
                    text-transform: uppercase;
                    font-size: 9px;
                    color: #000000;
                }
                .table-items {
                    margin-bottom: 8px;
                    border-collapse: collapse;
                    width: 100%;
                }
                .table-items th {
                    background-color: #ffffff;
                    color: #000000;
                    padding: 4px 5px;
                    text-align: left;
                    text-transform: uppercase;
                    font-size: 11px;
                    border-top: 1.5px solid #000000;
                    border-bottom: 1.5px solid #000000;
                }
                .table-items td {
                    padding: 3px 5px;
                    border-bottom: 1px solid #000000;
                    font-size: 10px;
                    color: #000000;
                }
                
                /* Overrides Khusus Traditional (Continuous 240x279mm) */
                .print-container.traditional {
                    font-size: 12px !important;
                    line-height: 1.3 !important;
                }
                .print-container.traditional h1 {
                    font-size: 16px !important;
                }
                .print-container.traditional .company-name, 
                .print-container.traditional .nota-title h1 {
                    font-size: 16px !important;
                }
                .print-container.traditional .client-info {
                    font-size: 12px !important;
                    margin-bottom: 12px !important;
                    padding-bottom: 6px !important;
                }
                .print-container.traditional .client-info span {
                    font-size: 12px !important;
                }
                .print-container.traditional .table-items {
                    margin-bottom: 12px !important;
                }
                .print-container.traditional .table-items th {
                    font-size: 12px !important;
                    padding: 6px 8px !important;
                }
                .print-container.traditional .table-items td {
                    font-size: 12px !important;
                    padding: 6px 8px !important;
                }
                .print-container.traditional .signature-section {
                    margin-top: 15px !important;
                }
                .print-container.traditional .signature-section td {
                    font-size: 12px !important;
                }
                .print-container.traditional .signature-section img {
                    height: 50px !important;
                }
            </style>
        </head>
        <body>
            <div class="print-container ${if (isTraditional) "traditional" else ""}">
            $headerHtml

            <div class="client-info" style="border-bottom: 1px solid #eeeeee; padding-bottom: 4px; margin-bottom: 8px;">
                ALAMAT PENGIRIMAN:
                <span style="font-size: 10px; font-weight: bold; margin-left: 5px;">$clientName</span>
                <span style="font-weight: normal; font-size: 9px; text-transform: capitalize; color: #444444; margin-left: 10px;">
                    $clientAddress
                </span>
            </div>

            <table class="table-items">
                <thead>
                    <tr>
                        <th style="width: 5%; text-align: center;">#</th>
                        <th style="width: 55%;">ITEM</th>
                        <th style="width: 20%; text-align: center;">LUSIN</th>
                        <th style="width: 20%; text-align: center;">QTY</th>
                    </tr>
                </thead>
                <tbody>
                    $itemsHtml
                </tbody>
            </table>

            $signatureHtml
            </div>
        </body>
        </html>
    """.trimIndent()
}
private fun getAssetBase64(context: Context, fileName: String): String {
    return try {
        val inputStream = context.assets.open(fileName)
        val bytes = inputStream.readBytes()
        inputStream.close()
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val extension = when {
            fileName.endsWith(".png", true) -> "png"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "jpeg"
            else -> "png"
        }
        "data:image/$extension;base64,$base64"
    } catch (e: Exception) {
        ""
    }
}

private fun printHtml(context: Context, html: String, jobName: String, isColor: Boolean, isTraditional: Boolean = false) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            
            val printAttributesBuilder = PrintAttributes.Builder()
                .setColorMode(if (isColor) PrintAttributes.COLOR_MODE_COLOR else PrintAttributes.COLOR_MODE_MONOCHROME)
            
            val customMediaSize = if (isTraditional) {
                PrintAttributes.MediaSize(
                    "continuous_9_5_11",
                    "Continuous 9.5x11 (240x279 mm)",
                    9449,
                    11000
                )
            } else {
                PrintAttributes.MediaSize.ISO_A4
            }
            printAttributesBuilder.setMediaSize(customMediaSize)
            
            val printAttributes = printAttributesBuilder.build()
            printManager.print(jobName, printAdapter, printAttributes)
        }
    }
    // Gunakan base URL agar gambar dari https://www.zedmz.cloud bisa dimuat
    // sebagai fallback jika file lokal tanda tangan tidak tersedia
    webView.loadDataWithBaseURL("https://www.zedmz.cloud", html, "text/html", "UTF-8", null)
}

private fun printColoredJpg(context: Context, html: String, fileName: String) {
    Toast.makeText(context, "Sedang memproses JPG…", Toast.LENGTH_SHORT).show()

    // ⚠️ WAJIB dipanggil SEBELUM WebView dibuat — aktifkan render seluruh dokumen
    // (default WebView hanya render viewport yang terlihat)
    WebView.enableSlowWholeDocumentDraw()

    val density = context.resources.displayMetrics.density
    val renderWidthPx = (794 * density).toInt() // v2.19.2: Mengubah lebar render JPG ke mode Portrait (794dp setara lebar A4 standar)

    val webView = WebView(context)
    // ✅ KUNCI: matikan hardware acceleration agar Canvas.draw() bisa capture bitmap
    // WebView hardware-accelerated render ke GPU layer yang tidak bisa di-read dari CPU
    webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

    webView.settings.apply {
        javaScriptEnabled = false
        useWideViewPort = true
        loadWithOverviewMode = true
        // Jangan blokir network: tanda tangan penerima mungkin perlu load dari URL
        // jika file lokal tidak tersedia (fallback). Base64 tetap prioritas utama.
        blockNetworkLoads = false
    }

    // Set ukuran awal sebelum load agar WebView tahu dimensinya saat render
    webView.measure(
        android.view.View.MeasureSpec.makeMeasureSpec(renderWidthPx, android.view.View.MeasureSpec.EXACTLY),
        android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
    )
    webView.layout(0, 0, renderWidthPx, 2000) // tinggi awal, akan di-update setelah render

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            // Tunggu 600ms agar rendering CSS + Base64 images selesai sepenuhnya
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    // Re-measure setelah halaman selesai render untuk dapat tinggi aktual
                    webView.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            renderWidthPx,
                            android.view.View.MeasureSpec.EXACTLY
                        ),
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            0,
                            android.view.View.MeasureSpec.UNSPECIFIED
                        )
                    )
                    val finalW = webView.measuredWidth.takeIf { it > 0 } ?: renderWidthPx
                    val finalH = webView.measuredHeight.takeIf { it > 0 }
                        ?: (webView.contentHeight * density).toInt().takeIf { it > 0 }
                        ?: 2000
                    webView.layout(0, 0, finalW, finalH)

                    val bitmap = android.graphics.Bitmap.createBitmap(
                        finalW, finalH, android.graphics.Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE) // background putih untuk JPEG
                    val canvas = android.graphics.Canvas(bitmap)
                    webView.draw(canvas)

                    // v2.19.25: Tambah margin putih 24px di setiap sisi agar hasil JPG lebih rapi
                    val marginPx = (24 * density).toInt()
                    val paddedBitmap = android.graphics.Bitmap.createBitmap(
                        finalW + marginPx * 2,
                        finalH + marginPx * 2,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    paddedBitmap.eraseColor(android.graphics.Color.WHITE)
                    val paddedCanvas = android.graphics.Canvas(paddedBitmap)
                    paddedCanvas.drawBitmap(bitmap, marginPx.toFloat(), marginPx.toFloat(), null)
                    bitmap.recycle()

                    val cachePath = File(context.cacheDir, "images")
                    cachePath.mkdirs()
                    val file = File(cachePath, "$fileName.jpg")
                    FileOutputStream(file).use { stream ->
                        paddedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
                        stream.flush()
                    }
                    paddedBitmap.recycle()

                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Bagikan JPG Invoice").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        context,
                        "Gagal membuat JPG: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }, 600)
        }
    }
    webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
}

@Composable
private fun ReceiverSignatureSection(
    invoice: com.posbah.app.data.local.entities.BmpInvoiceEntity,
    uiState: InvoiceDetailUi,
    onSignOnDevice: () -> Unit,
    onShareLink: () -> Unit,
    onDeleteSignature: () -> Unit
) {
    val isInvoiceSigned = !invoice.receiverSignaturePath.isNullOrBlank() || !invoice.receiverSignatureUrl.isNullOrBlank()
    val isClientSigned = !uiState.client?.receiverSignatureUrl.isNullOrBlank()
    val isSigned = isInvoiceSigned || isClientSigned

    val receiverName = if (!invoice.receiverNameActual.isNullOrBlank()) {
        invoice.receiverNameActual
    } else if (isClientSigned) {
        uiState.client?.receiverNameActual ?: ""
    } else {
        ""
    }

    val badgeText = if (isInvoiceSigned) "SUDAH DITANDATANGANI" else if (isClientSigned) "SUDAH DITANDATANGANI (KLIEN)" else "BELUM DITANDATANGANI"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("card-receiver-signature")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TANDA TANGAN PENERIMA",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSigned) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                    modifier = Modifier.testTag("sig-status-badge")
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSigned) Color(0xFF065F46) else Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isSigned) {
                val signatureModel = if (!invoice.receiverSignatureUrl.isNullOrBlank()) {
                    invoice.receiverSignatureUrl
                } else if (!invoice.receiverSignaturePath.isNullOrBlank()) {
                    invoice.receiverSignaturePath
                } else {
                    uiState.client?.receiverSignatureUrl
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = signatureModel,
                        contentDescription = "Tanda Tangan Penerima",
                        modifier = Modifier
                            .size(width = 100.dp, height = 60.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Penerima:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = receiverName.ifBlank { "-" },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDeleteSignature,
                        modifier = Modifier.testTag("btn-delete-signature")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Hapus Tanda Tangan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                PrimaryButton(
                    label = "Konfirmasi TTD",
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Tanda tangan penerima (${receiverName.ifBlank { "-" }}) telah dikonfirmasi secara sah.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn-confirm-signature")
                )
            } else {
                Text(
                    text = "Minta penerima menandatangani bukti penyerahan barang secara digital luring di HP ini atau share link unik berdurasi 10 menit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onSignOnDevice,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .testTag("btn-sign-device")
                    ) {
                        Icon(Icons.Outlined.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  Sign di HP Ini")
                    }
                    PrimaryButton(
                        label = "Bagikan Link TTD",
                        onClick = onShareLink,
                        modifier = Modifier.weight(1f).testTag("btn-share-sig-link")
                    )
                }
            }
        }
    }
}

private fun getFileBase64(filePath: String?): String {
    if (filePath.isNullOrBlank()) return ""
    return try {
        val file = File(filePath)
        if (!file.exists()) return ""
        val bytes = file.readBytes()
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val extension = when {
            filePath.endsWith(".png", true) -> "png"
            filePath.endsWith(".jpg", true) || filePath.endsWith(".jpeg", true) -> "jpeg"
            else -> "png"
        }
        "data:image/$extension;base64,$base64"
    } catch (e: Exception) {
        ""
    }
}

