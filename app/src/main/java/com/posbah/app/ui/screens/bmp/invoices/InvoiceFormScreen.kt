package com.posbah.app.ui.screens.bmp.invoices

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
    onDone: () -> Unit,
    viewModel: InvoiceFormViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val invoice = ui.invoice ?: return

    LaunchedEffect(ui.saved) { if (ui.saved) onDone() }

    var showClientPicker by remember { mutableStateOf(false) }
    var showNewProductPicker by remember { mutableStateOf(false) }
    val selectedClient = ui.clients.firstOrNull { it.id == invoice.clientId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = if (invoice.id == 0L) "Invoice Baru" else "Edit Invoice",
                onBack = onDone
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = invoice.number,
                    onValueChange = { v -> viewModel.updateInvoice { it.copy(number = v) } },
                    label = { Text("Nomor Invoice") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("invoice-number")
                )
            }
            item {
                OutlinedTextField(
                    value = invoice.title,
                    onValueChange = { v -> viewModel.updateInvoice { it.copy(title = v) } },
                    label = { Text("Judul / Keperluan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClientPicker = true }
                        .testTag("client-picker")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Klien", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                selectedClient?.clientName ?: "Pilih klien",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selectedClient != null) MaterialTheme.colorScheme.onBackground
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("\u203A", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val formattedDate = remember(invoice.createdAt) {
                    Formatters.dateLong(invoice.createdAt)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = invoice.createdAt }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = java.util.Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 12, 0, 0)
                                    }
                                    viewModel.updateInvoice { it.copy(createdAt = selectedCal.timeInMillis) }
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .testTag("invoice-date-picker")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tanggal Faktur", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = "Edit Tanggal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = invoice.paymentTerms,
                    onValueChange = { v -> viewModel.updateInvoice { it.copy(paymentTerms = v) } },
                    label = { Text("Term Pembayaran") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = invoice.notes.orEmpty(),
                    onValueChange = { v -> viewModel.updateInvoice { it.copy(notes = v.ifBlank { null }) } },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ITEM (${ui.productLines.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showNewProductPicker = true }, modifier = Modifier.testTag("btn-add-line")) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("Tambah baris")
                    }
                }
            }

            items(ui.productLines.size) { idx ->
                val line = ui.productLines[idx]
                ProductLineEditor(
                    line = line,
                    masterProducts = ui.masterProducts,
                    machines = ui.machines,
                    onChange = { trans -> viewModel.updateProductLine(idx) { trans(it) } },
                    onRemove = { viewModel.removeProductLine(idx) },
                    testTagPrefix = "line-$idx"
                )
            }

            item {
                val total = ui.productLines.sumOf { it.price * it.quantity * it.jumlahLusin }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("TOTAL", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                Formatters.rupiah(total),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                if (invoice.id == 0L) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = if (invoice.paidAmount == 0.0) "" else invoice.paidAmount.toLong().toString(),
                            onValueChange = { v ->
                                val cleaned = v.replace(Regex("[^0-9]"), "")
                                val n = cleaned.toDoubleOrNull() ?: 0.0
                                viewModel.updateInvoice { it.copy(paidAmount = n.coerceAtMost(total)) }
                            },
                            label = { Text("Jumlah Lunas (DP)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("invoice-paid-input")
                        )
                        val belumLunas = maxOf(0.0, total - invoice.paidAmount)
                        OutlinedTextField(
                            value = Formatters.rupiah(belumLunas),
                            onValueChange = {},
                            label = { Text("Belum Lunas (Sisa)") },
                            singleLine = true,
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    label = if (ui.isLoading) "Menyimpan\u2026" else "Simpan Invoice",
                    onClick = viewModel::save,
                    enabled = !ui.isLoading && ui.productLines.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("btn-save-invoice")
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showClientPicker) {
        ModalBottomSheet(
            onDismissRequest = { showClientPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Pelanggan",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { showClientPicker = false }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup")
                    }
                }

                // Add search bar
                var searchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama pelanggan...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = "Cari")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Customers list
                val filteredClients = remember(ui.clients, searchQuery) {
                    if (searchQuery.isBlank()) {
                        ui.clients
                    } else {
                        ui.clients.filter { it.clientName.contains(searchQuery, ignoreCase = true) }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val isSelected = invoice.clientId == client.id
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectClient(client)
                                    showClientPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = client.clientName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = client.clientName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!client.phoneNumber.isNullOrBlank() || !client.emailAddress.isNullOrBlank()) {
                                        Text(
                                            text = client.phoneNumber ?: client.emailAddress ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewProductPicker) {
        AlertDialog(
            onDismissRequest = { showNewProductPicker = false },
            title = { Text("Pilih Produk") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addBlankProductLine()
                                        showNewProductPicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                                    .testTag("picker-product-custom")
                            ) {
                                Column {
                                    Text(
                                        "Item Kosong / Kustom",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        "Tambah baris kosong baru",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        items(ui.masterProducts, key = { it.id }) { mp ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addProductLine(mp)
                                        showNewProductPicker = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                                    .testTag("picker-product-${mp.id}")
                            ) {
                                Column {
                                    Text(mp.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${Formatters.rupiah(mp.price)} / ${mp.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewProductPicker = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun ClientPickerRow(c: BmpClientEntity, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("picker-client-${c.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(c.clientName, style = MaterialTheme.typography.titleMedium)
            Text(
                c.phoneNumber ?: c.emailAddress ?: "Tanpa kontak",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductLineEditor(
    line: BmpProductEntity,
    masterProducts: List<com.posbah.app.data.local.entities.BmpMasterProductEntity>,
    machines: List<com.posbah.app.data.local.entities.BmpMachineEntity> = emptyList(),
    onChange: ((BmpProductEntity) -> BmpProductEntity) -> Unit,
    onRemove: () -> Unit,
    testTagPrefix: String
) {
    var showProductPicker by remember { mutableStateOf(false) }
    val matchedMaster = masterProducts.find { it.id == line.masterItemID }
    val matchedMachine = if (matchedMaster?.machineId != null)
        machines.find { it.id == matchedMaster.machineId.toLong() } else null
    val saranModal = if (matchedMaster != null) {
        matchedMaster.hppTotalPcs * line.quantity * line.jumlahLusin
    } else 0.0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.5f)) {
                    OutlinedTextField(
                        value = line.title,
                        onValueChange = { v -> onChange { it.copy(title = v) } },
                        label = { Text("Nama item") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("$testTagPrefix-title")
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = line.description ?: "",
                        onValueChange = { v -> onChange { it.copy(description = if (v.isBlank()) null else v) } },
                        label = { Text("Deskripsi (Opsional)") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("$testTagPrefix-description")
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { showProductPicker = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            if (matchedMaster != null) "Ganti Master: ${matchedMaster.title}" else "Pilih dari Master Produk",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    // Info chip: bahan baku + status mesin
                    if (matchedMaster != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (matchedMaster.jenisBahanBaku.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        "📦 ${matchedMaster.jenisBahanBaku}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (matchedMachine != null && !matchedMachine.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        "⚠️ Mesin Mati",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.testTag("$testTagPrefix-remove")) {
                    Icon(Icons.Outlined.Close, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = if (line.quantity == 0.0) "" else Formatters.plainDecimal(line.quantity),
                    onValueChange = { v ->
                        val n = v.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onChange { it.copy(quantity = n) }
                    },
                    label = { Text("Qty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = if (line.jumlahLusin == 0.0) "" else Formatters.plainDecimal(line.jumlahLusin),
                    onValueChange = { v ->
                        val n = v.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onChange { it.copy(jumlahLusin = n) }
                    },
                    label = { Text("Lsn") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = line.unit,
                    onValueChange = { v -> onChange { it.copy(unit = v) } },
                    label = { Text("Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
                OutlinedTextField(
                    value = if (line.price == 0.0) "" else line.price.toLong().toString(),
                    onValueChange = { v ->
                        val n = v.replace(",", "").toDoubleOrNull() ?: 0.0
                        onChange { it.copy(price = n) }
                    },
                    label = { Text("Harga") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    checked = line.isKhusus,
                    onCheckedChange = { checked ->
                        onChange {
                            it.copy(
                                isKhusus = checked,
                                hargaBeli = if (checked) saranModal else 0.0
                            )
                        }
                    }
                )
                Text("Barang Khusus", style = MaterialTheme.typography.bodyMedium)
            }
            if (line.isKhusus) {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = if (line.hargaBeli == 0.0) "" else line.hargaBeli.toLong().toString(),
                    onValueChange = { v ->
                        val n = v.replace(",", "").toDoubleOrNull() ?: 0.0
                        onChange { it.copy(hargaBeli = n) }
                    },
                    label = { Text("Harga Beli (Modal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (saranModal > 0.0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Saran Modal: ${Formatters.rupiah(saranModal)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF1F8B4C)
                    )
                }
            }
        }
    }

    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Pilih Master Produk") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(masterProducts, key = { it.id }) { mp ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange {
                                            it.copy(
                                                masterItemID = mp.id,
                                                title = mp.title,
                                                description = mp.description,
                                                unit = mp.unit,
                                                price = mp.price
                                            )
                                        }
                                        showProductPicker = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Column {
                                    Text(mp.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${Formatters.rupiah(mp.price)} / ${mp.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductPicker = false }) { Text("Tutup") }
            }
        )
    }
}
