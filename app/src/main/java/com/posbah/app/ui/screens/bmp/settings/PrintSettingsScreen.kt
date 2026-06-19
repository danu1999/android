package com.posbah.app.ui.screens.bmp.settings

import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.PrintSettingsEntity
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.ui.components.SignatureCanvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Tab indices
private const val TAB_JPG = 0
private const val TAB_SJ = 1
private const val TAB_INVOICE = 2
private const val TAB_POS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintSettingsScreen(
    onBack: () -> Unit,
    viewModel: PrintSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val draft by viewModel.draft.collectAsState()
    val isBmp = viewModel.moduleKey == "BMP"
    var selectedTab by remember { mutableStateOf(if (isBmp) TAB_JPG else TAB_POS) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.processAndSetLogo(it) { err ->
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    val d = draft
    if (d == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    val titleText = when (viewModel.moduleKey) {
        "FNB" -> "Pengaturan Struk FnB"
        "LAUNDRY" -> "Pengaturan Struk Laundry"
        "RENTAL" -> "Pengaturan Struk Rental"
        else -> "Pengaturan Cetak"
    }
    val subtitleText = when (viewModel.moduleKey) {
        "BMP" -> "JPG • Surat Jalan • Invoice • POS"
        else -> "Ukuran Kertas • Logo • Harga • Warna • Footer"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = titleText,
                subtitle = subtitleText,
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isBmp) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == TAB_JPG,
                        onClick = { selectedTab = TAB_JPG },
                        text = { Text("Cetak JPG", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab-jpg")
                    )
                    Tab(
                        selected = selectedTab == TAB_SJ,
                        onClick = { selectedTab = TAB_SJ },
                        text = { Text("Surat Jalan", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab-sj")
                    )
                    Tab(
                        selected = selectedTab == TAB_INVOICE,
                        onClick = { selectedTab = TAB_INVOICE },
                        text = { Text("Cetak Invoice", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab-invoice")
                    )
                    Tab(
                        selected = selectedTab == TAB_POS,
                        onClick = { selectedTab = TAB_POS },
                        text = { Text("Struk POS", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab-pos")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    TAB_JPG -> {
                        item {
                            DocPrintSettingsCard(
                                title = "Cetak JPG",
                                subtitle = "Pengaturan ini hanya berlaku untuk ekspor JPG",
                                useLogo = d.jpgUseLogo,
                                onUseLogoChange = { v -> viewModel.update { e -> e.copy(jpgUseLogo = v) } },
                                headerAlign = d.jpgHeaderAlign,
                                onHeaderAlignChange = { viewModel.update { e -> e.copy(jpgHeaderAlign = it) } },
                                useSignature = d.jpgUseSignature,
                                onUseSignatureChange = { viewModel.update { e -> e.copy(jpgUseSignature = it) } },
                                senderName = d.jpgSignatureSenderName,
                                onSenderNameChange = { viewModel.update { e -> e.copy(jpgSignatureSenderName = it) } },
                                receiverName = d.jpgSignatureReceiverName,
                                onReceiverNameChange = { viewModel.update { e -> e.copy(jpgSignatureReceiverName = it) } },
                                drawnBase64 = d.jpgSignatureDrawnBase64,
                                onSignatureSaved = { viewModel.update { e -> e.copy(jpgSignatureDrawnBase64 = it) } },
                                isColor = d.jpgIsColor,
                                onIsColorChange = { viewModel.update { e -> e.copy(jpgIsColor = it) } },
                                templateType = d.jpgTemplateType,
                                onTemplateTypeChange = { viewModel.update { e -> e.copy(jpgTemplateType = it) } },
                                tagPrefix = "jpg",
                                context = context
                            )
                        }
                    }
                    TAB_SJ -> {
                        item {
                            DocPrintSettingsCard(
                                title = "Surat Jalan",
                                subtitle = "Pengaturan ini hanya berlaku untuk Surat Jalan",
                                useLogo = d.sjUseLogo,
                                onUseLogoChange = { viewModel.update { e -> e.copy(sjUseLogo = it) } },
                                headerAlign = d.sjHeaderAlign,
                                onHeaderAlignChange = { viewModel.update { e -> e.copy(sjHeaderAlign = it) } },
                                useSignature = d.sjUseSignature,
                                onUseSignatureChange = { viewModel.update { e -> e.copy(sjUseSignature = it) } },
                                senderName = d.sjSignatureSenderName,
                                onSenderNameChange = { viewModel.update { e -> e.copy(sjSignatureSenderName = it) } },
                                receiverName = d.sjSignatureReceiverName,
                                onReceiverNameChange = { viewModel.update { e -> e.copy(sjSignatureReceiverName = it) } },
                                drawnBase64 = d.sjSignatureDrawnBase64,
                                onSignatureSaved = { viewModel.update { e -> e.copy(sjSignatureDrawnBase64 = it) } },
                                isColor = d.sjIsColor,
                                onIsColorChange = { viewModel.update { e -> e.copy(sjIsColor = it) } },
                                templateType = d.sjTemplateType,
                                onTemplateTypeChange = { viewModel.update { e -> e.copy(sjTemplateType = it) } },
                                tagPrefix = "sj",
                                context = context
                            )
                        }
                    }
                    TAB_INVOICE -> {
                        item {
                            DocPrintSettingsCard(
                                title = "Cetak Invoice / Faktur",
                                subtitle = "Pengaturan ini hanya berlaku untuk Invoice PDF",
                                useLogo = d.invoiceUseLogo,
                                onUseLogoChange = { viewModel.update { e -> e.copy(invoiceUseLogo = it) } },
                                headerAlign = d.invoiceHeaderAlign,
                                onHeaderAlignChange = { viewModel.update { e -> e.copy(invoiceHeaderAlign = it) } },
                                useSignature = d.invoiceUseSignature,
                                onUseSignatureChange = { viewModel.update { e -> e.copy(invoiceUseSignature = it) } },
                                senderName = d.invoiceSignatureSenderName,
                                onSenderNameChange = { viewModel.update { e -> e.copy(invoiceSignatureSenderName = it) } },
                                receiverName = d.invoiceSignatureReceiverName,
                                onReceiverNameChange = { viewModel.update { e -> e.copy(invoiceSignatureReceiverName = it) } },
                                drawnBase64 = d.invoiceSignatureDrawnBase64,
                                onSignatureSaved = { viewModel.update { e -> e.copy(invoiceSignatureDrawnBase64 = it) } },
                                isColor = d.invoiceIsColor,
                                onIsColorChange = { viewModel.update { e -> e.copy(invoiceIsColor = it) } },
                                templateType = d.invoiceTemplateType,
                                onTemplateTypeChange = { viewModel.update { e -> e.copy(invoiceTemplateType = it) } },
                                tagPrefix = "inv",
                                context = context
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Informasi Pembayaran", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text(
                                        "Informasi bank/e-wallet untuk dicetak di invoice",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Bank / E-Wallet", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("BCA", "BRI", "MANDIRI", "DANA", "SHOPE").forEach { bank ->
                                            val selected = d.bankName == bank
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.update { it.copy(bankName = bank) } }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(bank, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = d.bankOwnerName,
                                        onValueChange = { v -> viewModel.update { it.copy(bankOwnerName = v) } },
                                        label = { Text("Atas Nama Pemilik") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("inv-bank-owner")
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    val inputLabel = if (d.bankName == "DANA" || d.bankName == "SHOPE") "Nomor Akun Dana/Shopee" else "Nomor Rekening"
                                    OutlinedTextField(
                                        value = d.bankAccountNumber,
                                        onValueChange = { v -> viewModel.update { it.copy(bankAccountNumber = v) } },
                                        label = { Text(inputLabel) },
                                        singleLine = true,
                                        keyboardOptions =   KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().testTag("inv-bank-number")
                                    )
                                }
                            }
                        }
                    }
                    TAB_POS -> {
                        // Struk POS Section
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Lebar Kertas Struk", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Pilih ukuran kertas printer thermal Anda",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = d.receiptPaperWidth == "MM80",
                                            onClick = { viewModel.update { it.copy(receiptPaperWidth = "MM80") } }
                                        )
                                        Text("80 mm", modifier = Modifier.clickable {
                                            viewModel.update { it.copy(receiptPaperWidth = "MM80") }
                                        })
                                        Spacer(modifier = Modifier.width(32.dp))
                                        RadioButton(
                                            selected = d.receiptPaperWidth == "MM58",
                                            onClick = { viewModel.update { it.copy(receiptPaperWidth = "MM58") } }
                                        )
                                        Text("58 mm", modifier = Modifier.clickable {
                                            viewModel.update { it.copy(receiptPaperWidth = "MM58") }
                                        })
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Gunakan Logo Perusahaan", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Tampilkan logo di bagian atas struk",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = d.receiptUseLogo,
                                            onCheckedChange = { viewModel.update { e -> e.copy(receiptUseLogo = it) } },
                                            modifier = Modifier.testTag("sw-receipt-logo")
                                        )
                                    }

                                    if (d.receiptUseLogo) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Unggah Logo Struk", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Pilih gambar logo untuk dicetak di struk POS Anda",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(100.dp, 60.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { logoPickerLauncher.launch("image/*") }
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!d.logoPath.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = d.logoPath,
                                                        contentDescription = "Logo Struk",
                                                        contentScale = ContentScale.Inside,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Pilih Logo",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (!d.logoPath.isNullOrBlank()) {
                                                PrimaryButton(
                                                    label = "Hapus Logo",
                                                    onClick = { viewModel.update { e -> e.copy(logoPath = null) } },
                                                    modifier = Modifier.height(36.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!d.receiptUseLogo) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Posisi Teks Header", fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = d.receiptHeaderAlign == "LEFT",
                                                onClick = { viewModel.update { e -> e.copy(receiptHeaderAlign = "LEFT") } }
                                            )
                                            Text("Mepet Kiri", modifier = Modifier.clickable {
                                                viewModel.update { e -> e.copy(receiptHeaderAlign = "LEFT") }
                                            })
                                            Spacer(modifier = Modifier.width(24.dp))
                                            RadioButton(
                                                selected = d.receiptHeaderAlign == "CENTER",
                                                onClick = { viewModel.update { e -> e.copy(receiptHeaderAlign = "CENTER") } }
                                            )
                                            Text("Tetap di Tengah", modifier = Modifier.clickable {
                                                viewModel.update { e -> e.copy(receiptHeaderAlign = "CENTER") }
                                            })
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Tampilkan Harga Per Item", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Tampilkan rincian harga satuan di struk",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = d.receiptShowItemPrice,
                                            onCheckedChange = { viewModel.update { e -> e.copy(receiptShowItemPrice = it) } },
                                            modifier = Modifier.testTag("sw-receipt-price")
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Warna Cetak Struk", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Struk thermal biasanya hitam putih",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = d.receiptIsColor,
                                            onCheckedChange = { viewModel.update { e -> e.copy(receiptIsColor = it) } },
                                            modifier = Modifier.testTag("sw-receipt-color")
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Pesan Kaki Struk (Footer)", fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = d.receiptFooterText,
                                        onValueChange = { viewModel.update { e -> e.copy(receiptFooterText = it) } },
                                        label = { Text("Teks Footer") },
                                        singleLine = false,
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    label = "Simpan Pengaturan Cetak",
                    onClick = {
                        viewModel.save(
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            },
                            onDone = {
                                Toast.makeText(context, "Pengaturan cetak berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn-save-print-settings")
                )
            }
        }
    }
}

/**
 * Reusable card widget untuk pengaturan dokumen cetak (logo, header align, TTD, warna).
 * Dipakai di tab JPG, Surat Jalan, dan Invoice PDF.
 */
@Composable
private fun DocPrintSettingsCard(
    title: String,
    subtitle: String,
    useLogo: Boolean,
    onUseLogoChange: (Boolean) -> Unit,
    headerAlign: String,
    onHeaderAlignChange: (String) -> Unit,
    useSignature: Boolean,
    onUseSignatureChange: (Boolean) -> Unit,
    senderName: String,
    onSenderNameChange: (String) -> Unit,
    receiverName: String,
    onReceiverNameChange: (String) -> Unit,
    drawnBase64: String?,
    onSignatureSaved: (String?) -> Unit,
    isColor: Boolean,
    onIsColorChange: (Boolean) -> Unit,
    templateType: String,
    onTemplateTypeChange: (String) -> Unit,
    tagPrefix: String,
    context: android.content.Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header label
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Desain Template card ──────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Desain Template", fontWeight = FontWeight.SemiBold)
                Text(
                    "Pilih ukuran dan desain template dokumen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = templateType == "MODERN",
                        onClick = { onTemplateTypeChange("MODERN") }
                    )
                    Text("Modern (A4 / Standar)", modifier = Modifier.clickable { onTemplateTypeChange("MODERN") })
                    Spacer(modifier = Modifier.width(24.dp))
                    RadioButton(
                        selected = templateType == "TRADITIONAL",
                        onClick = { onTemplateTypeChange("TRADITIONAL") }
                    )
                    Text("Tradisional (9.5x11 / Continuous Form)", modifier = Modifier.clickable { onTemplateTypeChange("TRADITIONAL") })
                }
            }
        }

        // ── Logo card ────────────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gunakan Logo Perusahaan", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tampilkan logo di bagian atas dokumen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useLogo,
                        onCheckedChange = onUseLogoChange,
                        modifier = Modifier.testTag("sw-$tagPrefix-logo")
                    )
                }

                if (!useLogo) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Posisi Teks Header", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pilih perataan nama & alamat perusahaan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = headerAlign == "LEFT",
                            onClick = { onHeaderAlignChange("LEFT") }
                        )
                        Text("Mepet Kiri", modifier = Modifier.clickable { onHeaderAlignChange("LEFT") })
                        Spacer(modifier = Modifier.width(24.dp))
                        RadioButton(
                            selected = headerAlign == "CENTER",
                            onClick = { onHeaderAlignChange("CENTER") }
                        )
                        Text("Tetap di Tengah", modifier = Modifier.clickable { onHeaderAlignChange("CENTER") })
                    }
                }
            }
        }

        // ── Warna card ───────────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Warna Cetak", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Pilih mode warna untuk dokumen ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isColor,
                        onCheckedChange = onIsColorChange,
                        modifier = Modifier.testTag("sw-$tagPrefix-color")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isColor) "Mode: Berwarna" else "Mode: Hitam Putih (Grayscale)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── TTD card ─────────────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gunakan Tanda Tangan (TTD)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tampilkan area tanda tangan penerima & hormat kami",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useSignature,
                        onCheckedChange = onUseSignatureChange,
                        modifier = Modifier.testTag("sw-$tagPrefix-sig")
                    )
                }

                if (useSignature) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = senderName,
                        onValueChange = onSenderNameChange,
                        label = { Text("Nama Terang Pengirim (Hormat Kami)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gambar Tanda Tangan Anda (Pengirim)", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Gambar tanda tangan digital Anda untuk kolom Hormat Kami",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SignatureCanvas(
                        initialSignatureBase64 = drawnBase64,
                        onSignatureSaved = { base64 ->
                            onSignatureSaved(base64)
                            Toast.makeText(context, "Tanda tangan disimpan sementara", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
