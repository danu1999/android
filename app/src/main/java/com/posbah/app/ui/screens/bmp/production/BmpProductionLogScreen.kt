package com.posbah.app.ui.screens.bmp.production

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpMachineEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpProductionLogEntity
import com.posbah.app.data.local.entities.BmpProductStockEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpBahanBakuData
import com.posbah.app.data.repository.BmpBahanBakuRepository
import com.posbah.app.data.repository.BmpMachineRepository
import com.posbah.app.data.repository.BmpMasterProductRepository
import com.posbah.app.data.repository.BmpProductionLogRepository
import com.posbah.app.data.repository.BmpStockRepository
import com.posbah.app.data.repository.OnlineWriteResult
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Data Models ───────────────────────────────────────────────────────────────

/**
 * State per mesin untuk satu sesi shift produksi.
 * v2.19.17: setiap mesin punya form sendiri; cycle time & biaya listrik bisa di-override.
 */
data class MachineShiftEntry(
    val machine: BmpMachineEntity,
    val isRunningToday: Boolean = true,
    val isExpanded: Boolean = true,
    val selectedProduct: BmpMasterProductEntity? = null,
    val qtyProduced: String = "",
    val qtyRejected: String = "0",
    /** detik per siklus — pre-fill dari master produk, bisa di-override */
    val cycleTimeInput: String = "",
    /** Biaya listrik harian (Rp) — pre-fill dari mesin, bisa di-override */
    val electricityInput: String = "",
    val selectedRawMaterial: BmpBahanBakuData? = null
) {
    val missingFields: List<String>
        get() = if (!isRunningToday) emptyList() else buildList {
            if (selectedProduct == null) add("Produk")
            if (qtyProduced.toDoubleOrNull()?.let { it > 0 } != true) add("Jumlah Produksi")
        }
    val hasErrors: Boolean get() = missingFields.isNotEmpty()
    val estimatedMaterial: Double
        get() {
            val p = selectedProduct ?: return 0.0
            val qty = qtyProduced.toDoubleOrNull() ?: return 0.0
            return (qty * p.beratGram / 1000.0) * (1.0 + p.rejectRate / 100.0)
        }
}

data class ProductionLogItem(
    val log: BmpProductionLogEntity,
    val product: BmpMasterProductEntity?,
    val machineName: String?
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class BmpProductionLogViewModel @Inject constructor(
    private val logRepo: BmpProductionLogRepository,
    private val masterProductRepo: BmpMasterProductRepository,
    private val stockRepo: BmpStockRepository,
    private val bahanBakuRepo: BmpBahanBakuRepository,
    private val machineRepo: BmpMachineRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()

    val products = masterProductRepo.observe(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val productStocks = stockRepo.observeStocks(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rawMaterials = bahanBakuRepo.bahanBaku
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Entri shift per mesin — semua mesin (aktif & mati ditampilkan, default sesuai isActive) */
    private val _shiftEntries = MutableStateFlow<List<MachineShiftEntry>>(emptyList())
    val shiftEntries = _shiftEntries.asStateFlow()

    /** Log history: combine logs + products + machines */
    val logItems: StateFlow<List<ProductionLogItem>> = combine(
        logRepo.observeAll(tenantId),
        masterProductRepo.observe(tenantId),
        machineRepo.observe(tenantId)
    ) { logs, productsList, machines ->
        logs.filter { !it.isDeleted }
            .sortedByDescending { it.productionDate }
            .map { l ->
                val p = productsList.find { it.id == l.masterProductId }
                val m = machines.find { it.id == l.machineId }
                ProductionLogItem(l, p, m?.name)
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess = _saveSuccess.asStateFlow()
    fun clearSaveSuccess() { _saveSuccess.value = false }

    init {
        if (tenantId.isNotBlank()) {
            viewModelScope.launch {
                // Data refresh loop
                while (true) {
                    try {
                        logRepo.loadAll(tenantId)
                        bahanBakuRepo.refresh()
                    } catch (e: Exception) { e.printStackTrace() }
                    kotlinx.coroutines.delay(12_000)
                }
            }
            // Observe machines and build shift entries
            viewModelScope.launch {
                machineRepo.observe(tenantId).collect { machines ->
                    val filtered = machines.filter { !it.isDeleted }.sortedBy { it.id }
                    _shiftEntries.update { oldEntries ->
                        filtered.map { machine ->
                            val existing = oldEntries.find { it.machine.id == machine.id }
                            existing?.copy(machine = machine)
                                ?: MachineShiftEntry(
                                    machine = machine,
                                    isRunningToday = machine.isActive,
                                    electricityInput = if (machine.electricityCostDaily > 0)
                                        machine.electricityCostDaily.toLong().toString() else ""
                                )
                        }
                    }
                }
            }
        }
    }

    fun toggleMachineRunning(machineId: Long) {
        _shiftEntries.update { list ->
            list.map { if (it.machine.id == machineId) it.copy(isRunningToday = !it.isRunningToday) else it }
        }
    }

    fun toggleExpand(machineId: Long) {
        _shiftEntries.update { list ->
            list.map { if (it.machine.id == machineId) it.copy(isExpanded = !it.isExpanded) else it }
        }
    }

    fun selectProductForMachine(machineId: Long, product: BmpMasterProductEntity) {
        _shiftEntries.update { list ->
            list.map { entry ->
                if (entry.machine.id == machineId) {
                    entry.copy(
                        selectedProduct = product,
                        // Auto-fill cycle time from master product
                        cycleTimeInput = if (product.cycleTime > 0) product.cycleTime.toString() else ""
                    )
                } else entry
            }
        }
    }

    fun updateEntry(machineId: Long, transform: (MachineShiftEntry) -> MachineShiftEntry) {
        _shiftEntries.update { list ->
            list.map { if (it.machine.id == machineId) transform(it) else it }
        }
    }

    fun saveAllEntries() = viewModelScope.launch {
        val runningEntries = _shiftEntries.value.filter { it.isRunningToday }
        if (runningEntries.isEmpty()) {
            _error.value = "Tidak ada mesin yang sedang beroperasi hari ini."
            return@launch
        }

        // Validation check
        val entriesWithErrors = runningEntries.filter { it.hasErrors }
        if (entriesWithErrors.isNotEmpty()) {
            val names = entriesWithErrors.map { it.machine.name }.joinToString(", ")
            _error.value = "Data belum lengkap untuk: $names. Periksa field yang wajib diisi."
            return@launch
        }

        var savedCount = 0
        for (entry in runningEntries) {
            val product = entry.selectedProduct ?: continue
            val qtyProd = entry.qtyProduced.toDoubleOrNull() ?: continue
            val qtyRej = entry.qtyRejected.toDoubleOrNull() ?: 0.0
            val cycleTime = entry.cycleTimeInput.toDoubleOrNull() ?: product.cycleTime
            val electricityCost = entry.electricityInput.toDoubleOrNull()
                ?: entry.machine.electricityCostDaily

            val log = BmpProductionLogEntity(
                tenantId = tenantId,
                masterProductId = product.id,
                machineId = entry.machine.id,
                quantityProduced = qtyProd,
                quantityRejected = qtyRej,
                rawMaterialUsedKg = entry.estimatedMaterial,
                rawMaterialId = entry.selectedRawMaterial?.id ?: 0L,
                cycleTimeActual = cycleTime,
                electricityCostActual = electricityCost,
                operatorName = null,
                productionDate = System.currentTimeMillis()
            )

            val result = logRepo.addProductionLog(context, log)
            if (result is OnlineWriteResult.Success) {
                savedCount++
                stockRepo.adjustStock(productId = product.id, quantity = qtyProd, reason = "PRODUKSI")
                if (log.rawMaterialUsedKg > 0 && log.rawMaterialId > 0) {
                    bahanBakuRepo.addUsage(materialId = log.rawMaterialId, quantity = -log.rawMaterialUsedKg, reason = "PRODUKSI")
                }
            } else if (result is OnlineWriteResult.Error) {
                _error.value = "Gagal simpan [${entry.machine.name}]: ${result.message}"
            }
        }

        if (savedCount > 0) {
            // Reset running entries (keep machine state but clear form data)
            _shiftEntries.update { list ->
                list.map { entry ->
                    if (entry.isRunningToday) entry.copy(
                        selectedProduct = null,
                        qtyProduced = "",
                        qtyRejected = "0",
                        cycleTimeInput = "",
                        selectedRawMaterial = null
                    ) else entry
                }
            }
            _saveSuccess.value = true
        }
    }

    fun deleteLog(log: BmpProductionLogEntity) = viewModelScope.launch {
        val result = logRepo.deleteProductionLog(context, tenantId, log)
        if (result is OnlineWriteResult.Success) {
            stockRepo.adjustStock(productId = log.productId, quantity = -log.quantityProduced, reason = "PEMBATALAN PRODUKSI")
            if (log.rawMaterialUsedKg > 0) {
                bahanBakuRepo.addUsage(materialId = log.rawMaterialId, quantity = log.rawMaterialUsedKg, reason = "PEMBATALAN PRODUKSI")
            }
        } else if (result is OnlineWriteResult.Error) {
            _error.value = result.message
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun BmpProductionLogScreen(
    onBack: () -> Unit,
    viewModel: BmpProductionLogViewModel = hiltViewModel()
) {
    val shiftEntries by viewModel.shiftEntries.collectAsState()
    val logItems by viewModel.logItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val productStocks by viewModel.productStocks.collectAsState()
    val rawMaterials by viewModel.rawMaterials.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var logToDelete by remember { mutableStateOf<BmpProductionLogEntity?>(null) }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "✅ Hasil produksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Log Produksi Harian",
                subtitle = "Manufaktur — v2.19.17",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Tab Bar ───────────────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Shift Hari Ini") },
                    icon = { Icon(Icons.Outlined.PrecisionManufacturing, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat (${logItems.size})") },
                    icon = { Icon(Icons.Outlined.History, null, modifier = Modifier.size(18.dp)) }
                )
            }

            // ── Tab Content ───────────────────────────────────────────────────
            when (selectedTab) {
                0 -> ShiftTab(
                    shiftEntries = shiftEntries,
                    products = products,
                    productStocks = productStocks,
                    rawMaterials = rawMaterials,
                    onToggleMachine = { viewModel.toggleMachineRunning(it) },
                    onToggleExpand = { viewModel.toggleExpand(it) },
                    onSelectProduct = { machineId, product -> viewModel.selectProductForMachine(machineId, product) },
                    onUpdateEntry = { machineId, transform -> viewModel.updateEntry(machineId, transform) },
                    onSaveAll = { viewModel.saveAllEntries() }
                )
                1 -> HistoryTab(
                    logItems = logItems,
                    onDeleteRequest = { logToDelete = it }
                )
            }
        }
    }

    // Delete confirm dialog
    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Hapus Log Produksi") },
            text = { Text("Tindakan ini akan mengembalikan stok barang jadi yang telah ditambahkan. Yakin?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLog(log)
                    logToDelete = null
                    Toast.makeText(context, "Log produksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text("Batal") }
            }
        )
    }
}

// ── Shift Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun ShiftTab(
    shiftEntries: List<MachineShiftEntry>,
    products: List<BmpMasterProductEntity>,
    productStocks: List<BmpProductStockEntity>,
    rawMaterials: List<BmpBahanBakuData>,
    onToggleMachine: (Long) -> Unit,
    onToggleExpand: (Long) -> Unit,
    onSelectProduct: (Long, BmpMasterProductEntity) -> Unit,
    onUpdateEntry: (Long, (MachineShiftEntry) -> MachineShiftEntry) -> Unit,
    onSaveAll: () -> Unit
) {
    val runningCount = shiftEntries.count { it.isRunningToday }
    val errorCount = shiftEntries.count { it.hasErrors }

    Column(modifier = Modifier.fillMaxSize()) {
        // Global validation banner
        AnimatedVisibility(visible = errorCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Warning, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Text(
                        "$errorCount mesin memiliki field wajib yang belum diisi",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Machine cards
        if (shiftEntries.isEmpty()) {
            EmptyState(
                title = "Belum ada mesin terdaftar",
                description = "Tambahkan mesin di menu Pengaturan Mesin & Matras terlebih dahulu.",
                actionLabel = "Kembali",
                onAction = {}
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(shiftEntries, key = { it.machine.id }) { entry ->
                    MachineShiftCard(
                        entry = entry,
                        products = products,
                        productStocks = productStocks,
                        rawMaterials = rawMaterials,
                        onToggle = { onToggleMachine(entry.machine.id) },
                        onToggleExpand = { onToggleExpand(entry.machine.id) },
                        onSelectProduct = { p -> onSelectProduct(entry.machine.id, p) },
                        onUpdate = { transform -> onUpdateEntry(entry.machine.id, transform) }
                    )
                }
            }
        }

        // Save all button
        if (runningCount > 0) {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSaveAll,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = errorCount == 0
                ) {
                    Icon(Icons.Outlined.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Semua Hasil Produksi ($runningCount mesin)")
                }
            }
        }
    }
}

// ── Machine Shift Card ────────────────────────────────────────────────────────

@Composable
private fun MachineShiftCard(
    entry: MachineShiftEntry,
    products: List<BmpMasterProductEntity>,
    productStocks: List<BmpProductStockEntity>,
    rawMaterials: List<BmpBahanBakuData>,
    onToggle: () -> Unit,
    onToggleExpand: () -> Unit,
    onSelectProduct: (BmpMasterProductEntity) -> Unit,
    onUpdate: ((MachineShiftEntry) -> MachineShiftEntry) -> Unit
) {
    var productDropdown by remember { mutableStateOf(false) }
    var rawMatDropdown by remember { mutableStateOf(false) }
    val machine = entry.machine

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (entry.isRunningToday) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Machine Header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (entry.isRunningToday) onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (entry.isRunningToday) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Engineering,
                            null,
                            tint = if (entry.isRunningToday) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        machine.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        if (entry.isRunningToday) "🟢 Beroperasi Hari Ini"
                        else "⚫ Tidak Beroperasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entry.isRunningToday) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Expand/collapse icon (only when running)
                if (entry.isRunningToday) {
                    Icon(
                        if (entry.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                }
                // ON/OFF switch
                Switch(
                    checked = entry.isRunningToday,
                    onCheckedChange = { onToggle() }
                )
            }

            // ── Validation Errors Banner ─────────────────────────────────────
            AnimatedVisibility(visible = entry.hasErrors && entry.isExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Info, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            "Wajib diisi: ${entry.missingFields.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ── Form Fields (when machine is running) ────────────────────────
            AnimatedVisibility(visible = entry.isRunningToday && entry.isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Produk yang diproduksi
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val stockQty = productStocks.find { it.masterProductId == entry.selectedProduct?.id }?.quantity ?: 0.0
                        OutlinedTextField(
                            value = if (entry.selectedProduct != null)
                                "${entry.selectedProduct.title} (Stok: ${Formatters.number(stockQty)} ${entry.selectedProduct.unit})"
                            else "— Pilih Produk *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Produk yang Diproduksi *") },
                            isError = entry.isRunningToday && entry.selectedProduct == null,
                            trailingIcon = {
                                IconButton(onClick = { productDropdown = true }) {
                                    Icon(Icons.Outlined.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { productDropdown = true }
                        )
                        DropdownMenu(
                            expanded = productDropdown,
                            onDismissRequest = { productDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(p.title, fontWeight = FontWeight.Bold)
                                            Text(
                                                "HPP: ${Formatters.rupiah(p.hppTotalPcs)} | ${p.jenisBahanBaku.ifEmpty { "—" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectProduct(p)
                                        productDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Jumlah Produksi & Reject
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = entry.qtyProduced,
                            onValueChange = { v -> onUpdate { it.copy(qtyProduced = v) } },
                            label = { Text("Barang Jadi *") },
                            singleLine = true,
                            isError = entry.isRunningToday && entry.qtyProduced.toDoubleOrNull()?.let { it > 0 } != true,
                            suffix = { Text(entry.selectedProduct?.unit ?: "pcs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = entry.qtyRejected,
                            onValueChange = { v -> onUpdate { it.copy(qtyRejected = v) } },
                            label = { Text("Barang Reject") },
                            singleLine = true,
                            suffix = { Text(entry.selectedProduct?.unit ?: "pcs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. Cycle Time & Listrik
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = entry.cycleTimeInput,
                            onValueChange = { v -> onUpdate { it.copy(cycleTimeInput = v) } },
                            label = { Text("Cycle Time (detik)") },
                            singleLine = true,
                            placeholder = {
                                val hint = entry.selectedProduct?.cycleTime
                                if (hint != null && hint > 0) Text("$hint")
                            },
                            supportingText = { Text("→ dari master produk") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = entry.electricityInput,
                            onValueChange = { v -> onUpdate { it.copy(electricityInput = v) } },
                            label = { Text("Biaya Listrik/hari") },
                            singleLine = true,
                            placeholder = {
                                val hint = machine.electricityCostDaily
                                if (hint > 0) Text(hint.toLong().toString())
                            },
                            prefix = { Text("Rp") },
                            supportingText = { Text("→ dari mesin") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. Batch Bahan Baku (opsional)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val rawDisplayValue = entry.selectedRawMaterial?.let {
                            "Tagihan ${it.noTagihan} — ${it.supplier ?: "Tanpa Supplier"}"
                        } ?: "Pilih Batch Bahan Baku (Opsional)"
                        OutlinedTextField(
                            value = rawDisplayValue,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Batch Bahan Baku") },
                            trailingIcon = {
                                IconButton(onClick = { rawMatDropdown = true }) {
                                    Icon(Icons.Outlined.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { rawMatDropdown = true }
                        )
                        DropdownMenu(
                            expanded = rawMatDropdown,
                            onDismissRequest = { rawMatDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            rawMaterials.forEach { rm ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("No: ${rm.noTagihan}", fontWeight = FontWeight.Bold)
                                            Text("Supplier: ${rm.supplier ?: "—"}", style = MaterialTheme.typography.bodySmall)
                                            Text("Tgl: ${Formatters.dateLong(rm.tanggal)}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        onUpdate { it.copy(selectedRawMaterial = rm) }
                                        rawMatDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // 5. Estimasi Summary Card
                    if (entry.estimatedMaterial > 0 || entry.selectedProduct != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ESTIMASI SHIFT INI",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary)
                                val prod = entry.selectedProduct
                                if (prod != null) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Bahan Baku", style = MaterialTheme.typography.bodySmall)
                                        Text(prod.jenisBahanBaku.ifEmpty { "—" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Berat / unit", style = MaterialTheme.typography.bodySmall)
                                        Text("${Formatters.number(prod.beratGram)} gram",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("HPP/pcs (master)", style = MaterialTheme.typography.bodySmall)
                                        Text(Formatters.rupiah(prod.hppTotalPcs),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (entry.estimatedMaterial > 0) {
                                    Divider(modifier = Modifier.padding(vertical = 3.dp))
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Est. Bahan Terpakai",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${Formatters.number(entry.estimatedMaterial)} Kg",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── History Tab ───────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    logItems: List<ProductionLogItem>,
    onDeleteRequest: (BmpProductionLogEntity) -> Unit
) {
    if (logItems.isEmpty()) {
        EmptyState(
            title = "Belum ada riwayat produksi",
            description = "Catat hasil produksi harian di tab Shift Hari Ini.",
            actionLabel = "",
            onAction = {}
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(logItems, key = { it.log.id }) { item ->
                val log = item.log
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.PrecisionManufacturing, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.product?.title ?: "Produk #${log.masterProductId}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "${Formatters.dateLong(log.productionDate)} • ${item.machineName ?: "Tanpa Mesin"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (log.cycleTimeActual > 0 || log.electricityCostActual > 0) {
                                Text(
                                    buildString {
                                        if (log.cycleTimeActual > 0) append("CT: ${log.cycleTimeActual}s")
                                        if (log.electricityCostActual > 0) append("  Listrik: ${Formatters.rupiah(log.electricityCostActual)}")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SuggestionChip(onClick = {}, label = { Text("Reject: ${Formatters.number(log.quantityRejected)}") })
                                SuggestionChip(onClick = {}, label = { Text("BB: ${Formatters.number(log.rawMaterialUsedKg)} Kg") })
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "+${Formatters.number(log.quantityProduced)} ${item.product?.unit ?: "pcs"}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32)
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            IconButton(
                                onClick = { onDeleteRequest(log) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
