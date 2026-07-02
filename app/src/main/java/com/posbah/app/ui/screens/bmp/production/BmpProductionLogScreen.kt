@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import com.posbah.app.ui.screens.bmp.bahanbaku.ColorMixEntry
import com.posbah.app.ui.screens.bmp.bahanbaku.WARNA_OPTIONS
import com.posbah.app.data.local.entities.BmpMachineEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpProductionLogEntity
import com.posbah.app.data.local.entities.BmpProductStockEntity
import com.posbah.app.data.local.entities.serializeWorkersAttendance
import com.posbah.app.data.local.entities.BmpMachineWorkerAttendance
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpBahanBakuData
import com.posbah.app.data.repository.BmpBahanBakuRepository
import com.posbah.app.data.repository.BmpEmployeeData
import com.posbah.app.data.repository.BmpEmployeeRepository
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
import java.util.Calendar
import javax.inject.Inject

// ── Data Models ───────────────────────────────────────────────────────────────

/** Tiga shift produksi harian */
enum class ShiftType(val label: String, val startHour: Int, val defaultCheckIn: String, val defaultCheckOut: String) {
    PAGI("Shift Pagi (07.00)", 7, "07:00", "15:00"),
    SORE("Shift Sore (15.00)", 15, "15:00", "23:00"),
    MALAM("Shift Malam (23.00)", 23, "23:00", "07:00")
}

/** Deteksi shift aktif berdasarkan jam lokal */
fun detectCurrentShift(): ShiftType {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 7..14  -> ShiftType.PAGI
        hour in 15..22 -> ShiftType.SORE
        else           -> ShiftType.MALAM
    }
}

/**
 * State per mesin untuk satu sesi shift produksi.
 * v2.19.21: tambah shiftName, operatorEmployeeId, checkIn/Out per mesin.
 */
data class OperatorAttendanceEntry(
    val employeeId: Long? = null,
    val checkIn: String = "07:00",
    val checkOut: String = "15:00"
)

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
    val selectedRawMaterial: BmpBahanBakuData? = null,
    /** v2.19.18: Apakah mesin ini menggunakan campuran warna pada shift ini */
    val isCampuranBahan: Boolean = false,
    /** Campuran warna: [{warna:"Merah",rasio:"1"},{warna:"Natural",rasio:"9"}] */
    val campuranBahan: List<ColorMixEntry> = listOf(ColorMixEntry()),
    /** v2.19.22: Daftar operator yang bertugas di mesin ini pada shift ini */
    val operators: List<OperatorAttendanceEntry> = listOf(OperatorAttendanceEntry())
) {
    val missingFields: List<String>
        get() = if (!isRunningToday) emptyList() else buildList {
            if (selectedProduct == null) add("Produk")
            if (qtyProduced.toDoubleOrNull()?.let { it > 0 } != true) add("Jumlah Produksi")
            if (operators.any { it.employeeId == null }) add("Operator")
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
    private val employeeRepo: BmpEmployeeRepository,
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

    val employees = employeeRepo.employees
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Shift aktif saat ini — user dapat mengubahnya */
    private val _activeShift = MutableStateFlow(detectCurrentShift())
    val activeShift = _activeShift.asStateFlow()

    fun setShift(shift: ShiftType) {
        _activeShift.value = shift
        // Update default check-in/out di semua entri
        _shiftEntries.update { list ->
            list.map { entry ->
                entry.copy(
                    operators = entry.operators.map { op ->
                        op.copy(checkIn = shift.defaultCheckIn, checkOut = shift.defaultCheckOut)
                    }
                )
            }
        }
    }

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
                        employeeRepo.refresh()
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

            val colorMixtureJson = if (entry.isCampuranBahan &&
                entry.campuranBahan.any { it.warna.isNotBlank() }) {
                entry.campuranBahan
                    .filter { it.warna.isNotBlank() }
                    .joinToString(",", "[", "]") { ce ->
                        val rawIdStr = ce.selectedRawMaterial?.id?.toString() ?: "null"
                        "{\"color\":\"${ce.warna}\",\"rasio\":\"${ce.rasio.ifBlank { "1" }}\",\"raw_material_id\":$rawIdStr}"
                    }
            } else null

            // Serialize workers attendance (can have multiple operators, min 1)
            val attendanceItems = entry.operators.mapNotNull { op ->
                if (op.employeeId != null) {
                    val empName = employees.value.find { it.id == op.employeeId }?.name ?: ""
                    BmpMachineWorkerAttendance(
                        employeeId = op.employeeId,
                        employeeName = empName,
                        checkIn = op.checkIn,
                        checkOut = op.checkOut
                    )
                } else null
            }
            val attendanceJson = serializeWorkersAttendance(attendanceItems)

            val log = BmpProductionLogEntity(
                tenantId = tenantId,
                masterProductId = product.id,
                machineId = entry.machine.id,
                quantityProduced = qtyProd,
                quantityRejected = qtyRej,
                rawMaterialUsedKg = entry.estimatedMaterial,
                rawMaterialId = if (entry.isCampuranBahan) 0L else (entry.selectedRawMaterial?.id ?: 0L),
                cycleTimeActual = cycleTime,
                electricityCostActual = electricityCost,
                colorMixture = colorMixtureJson,
                operatorName = attendanceItems.joinToString(", ") { it.employeeName }.ifBlank { null },
                workersAttendance = attendanceJson,
                shiftName = _activeShift.value.name,
                productionDate = System.currentTimeMillis()
            )

            val result = logRepo.addProductionLog(context, log)
            if (result is OnlineWriteResult.Success) {
                savedCount++
                stockRepo.adjustStock(productId = product.id, quantity = qtyProd, reason = "PRODUKSI")
                if (entry.isCampuranBahan) {
                    val totalRasio = entry.campuranBahan.sumOf { it.rasio.toDoubleOrNull() ?: 0.0 }
                    if (totalRasio > 0) {
                        entry.campuranBahan.forEach { ce ->
                            val rmId = ce.selectedRawMaterial?.id ?: 0L
                            val ratio = ce.rasio.toDoubleOrNull() ?: 0.0
                            val partQty = log.rawMaterialUsedKg * (ratio / totalRasio)
                            if (rmId > 0 && partQty > 0) {
                                bahanBakuRepo.addUsage(materialId = rmId, quantity = -partQty, reason = "PRODUKSI")
                            }
                        }
                    }
                } else {
                    if (log.rawMaterialUsedKg > 0 && log.rawMaterialId > 0) {
                        bahanBakuRepo.addUsage(materialId = log.rawMaterialId, quantity = -log.rawMaterialUsedKg, reason = "PRODUKSI")
                    }
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
                        selectedRawMaterial = null,
                        operators = listOf(OperatorAttendanceEntry())
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
                if (!log.colorMixture.isNullOrBlank()) {
                    try {
                        val entries = parseColorMixtureEntries(log.colorMixture)
                        val totalRasio = entries.sumOf { it.rasio }
                        if (totalRasio > 0) {
                            entries.forEach { ce ->
                                if (ce.rawMaterialId > 0) {
                                    val partQty = log.rawMaterialUsedKg * (ce.rasio / totalRasio)
                                    bahanBakuRepo.addUsage(materialId = ce.rawMaterialId, quantity = partQty, reason = "PEMBATALAN PRODUKSI")
                                }
                            }
                        }
                    } catch (_: Exception) {
                        if (log.rawMaterialId > 0) {
                            bahanBakuRepo.addUsage(materialId = log.rawMaterialId, quantity = log.rawMaterialUsedKg, reason = "PEMBATALAN PRODUKSI")
                        }
                    }
                } else {
                    if (log.rawMaterialId > 0) {
                        bahanBakuRepo.addUsage(materialId = log.rawMaterialId, quantity = log.rawMaterialUsedKg, reason = "PEMBATALAN PRODUKSI")
                    }
                }
            }
        } else if (result is OnlineWriteResult.Error) {
            _error.value = result.message
        }
    }
}

data class ColorMixtureJsonEntry(
    val color: String,
    val rasio: Double,
    val rawMaterialId: Long
)

fun parseColorMixtureEntries(json: String): List<ColorMixtureJsonEntry> {
    if (json.isBlank()) return emptyList()
    return try {
        json.trimStart('[').trimEnd(']')
            .split("},")
            .filter { it.isNotBlank() }
            .map { chunk ->
                val clean = chunk.replace("{", "").replace("}", "").replace("\"", "")
                val map = clean.split(",").associate {
                    val kv = it.split(":")
                    kv.getOrElse(0) { "" }.trim() to kv.getOrElse(1) { "" }.trim()
                }
                ColorMixtureJsonEntry(
                    color = map["color"] ?: "",
                    rasio = map["rasio"]?.toDoubleOrNull() ?: 1.0,
                    rawMaterialId = map["raw_material_id"]?.toLongOrNull() ?: 0L
                )
            }
    } catch (_: Exception) {
        emptyList()
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
    val employees by viewModel.employees.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
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
                subtitle = "Manufaktur — v2.19.21",
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
                    activeShift = activeShift,
                    employees = employees,
                    products = products,
                    productStocks = productStocks,
                    rawMaterials = rawMaterials,
                    onShiftChange = { viewModel.setShift(it) },
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
    activeShift: ShiftType,
    employees: List<BmpEmployeeData>,
    products: List<BmpMasterProductEntity>,
    productStocks: List<BmpProductStockEntity>,
    rawMaterials: List<BmpBahanBakuData>,
    onShiftChange: (ShiftType) -> Unit,
    onToggleMachine: (Long) -> Unit,
    onToggleExpand: (Long) -> Unit,
    onSelectProduct: (Long, BmpMasterProductEntity) -> Unit,
    onUpdateEntry: (Long, (MachineShiftEntry) -> MachineShiftEntry) -> Unit,
    onSaveAll: () -> Unit
) {
    val runningCount = shiftEntries.count { it.isRunningToday }
    val errorCount = shiftEntries.count { it.hasErrors }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Shift Selector ────────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🕔 Shift:", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                ShiftType.values().forEach { shift ->
                    val isSelected = shift == activeShift
                    FilterChip(
                        selected = isSelected,
                        onClick = { onShiftChange(shift) },
                        label = { Text(shift.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

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
                        employees = employees,
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
    employees: List<BmpEmployeeData>,
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

                    if (!entry.isCampuranBahan) {
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
                    }

                    // 5a. Absensi Operator (min 1 orang per mesin)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "👷 Operator Shift Ini",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    entry.operators.forEachIndexed { opIdx, op ->
                        var opDropdownOpen by remember { mutableStateOf(false) }
                        val selectedEmpName = employees.find { it.id == op.employeeId }?.name

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1.8f)) {
                                    ExposedDropdownMenuBox(
                                        expanded = opDropdownOpen,
                                        onExpandedChange = { opDropdownOpen = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedEmpName ?: "— Pilih Operator ${opIdx + 1} —",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Operator ${opIdx + 1}") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = opDropdownOpen) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = opDropdownOpen,
                                            onDismissRequest = { opDropdownOpen = false }
                                        ) {
                                            employees.filter { it.isActive }.forEach { emp ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(emp.name, fontWeight = FontWeight.Medium)
                                                            Text(emp.role, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                        }
                                                    },
                                                    onClick = {
                                                        onUpdate { d ->
                                                            val nl = d.operators.toMutableList()
                                                            if (opIdx in nl.indices) nl[opIdx] = nl[opIdx].copy(employeeId = emp.id)
                                                            d.copy(operators = nl)
                                                        }
                                                        opDropdownOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (entry.operators.size > 1) {
                                    IconButton(
                                        onClick = {
                                            onUpdate { d ->
                                                val nl = d.operators.toMutableList().also { it.removeAt(opIdx) }
                                                d.copy(operators = nl)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = op.checkIn,
                                    onValueChange = { v ->
                                        onUpdate { d ->
                                            val nl = d.operators.toMutableList()
                                            if (opIdx in nl.indices) nl[opIdx] = nl[opIdx].copy(checkIn = v)
                                            d.copy(operators = nl)
                                        }
                                    },
                                    label = { Text("Jam Masuk") },
                                    singleLine = true,
                                    placeholder = { Text("07:00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = op.checkOut,
                                    onValueChange = { v ->
                                        onUpdate { d ->
                                            val nl = d.operators.toMutableList()
                                            if (opIdx in nl.indices) nl[opIdx] = nl[opIdx].copy(checkOut = v)
                                            d.copy(operators = nl)
                                        }
                                    },
                                    label = { Text("Jam Keluar") },
                                    singleLine = true,
                                    placeholder = { Text("15:00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { onUpdate { d -> d.copy(operators = d.operators + OperatorAttendanceEntry()) } },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah Operator", style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onUpdate { it.copy(isCampuranBahan = !it.isCampuranBahan) }
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = entry.isCampuranBahan,
                            onCheckedChange = { onUpdate { e -> e.copy(isCampuranBahan = it) } }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Campuran Pewarna",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                if (entry.isCampuranBahan) "Isi pewarna & rasio campuran"
                                else "Mesin ini menggunakan campuran pewarna?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AnimatedVisibility(visible = entry.isCampuranBahan) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Summary chip
                            val totalRasio = entry.campuranBahan.sumOf { it.rasio.toDoubleOrNull() ?: 0.0 }
                            if (totalRasio > 0) {
                                val summary = entry.campuranBahan.filter { it.warna.isNotBlank() }.joinToString(" + ") { ce ->
                                    val pct = if (totalRasio > 0) ((ce.rasio.toDoubleOrNull() ?: 0.0) / totalRasio * 100).toInt() else 0
                                    "${ce.warna} ${pct}%"
                                }
                                if (summary.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Campuran: $summary",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            modifier = Modifier.padding(8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // 1. Bahan Utama (Warna 1)
                            val mainMaterial = entry.campuranBahan.firstOrNull() ?: ColorMixEntry()
                            var mainWarnaDropOpen by remember { mutableStateOf(false) }
                            var mainBatchDropOpen by remember { mutableStateOf(false) }
                            val mainBatchDisplay = mainMaterial.selectedRawMaterial?.let {
                                "Batch: ${it.noTagihan} (${it.supplier ?: "Tanpa Supplier"})"
                            } ?: "Pilih Batch Bahan Utama"

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Bahan Utama (Warna 1)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1.8f)) {
                                        ExposedDropdownMenuBox(expanded = mainWarnaDropOpen, onExpandedChange = { mainWarnaDropOpen = !mainWarnaDropOpen }) {
                                            OutlinedTextField(
                                                value = mainMaterial.warna,
                                                onValueChange = { v ->
                                                    onUpdate { d ->
                                                        val nl = d.campuranBahan.toMutableList()
                                                        if (nl.isNotEmpty()) nl[0] = nl[0].copy(warna = v)
                                                        d.copy(campuranBahan = nl)
                                                    }
                                                },
                                                label = { Text("Warna Utama") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mainWarnaDropOpen) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().menuAnchor()
                                            )
                                            ExposedDropdownMenu(expanded = mainWarnaDropOpen, onDismissRequest = { mainWarnaDropOpen = false }) {
                                                WARNA_OPTIONS.forEach { wOpt ->
                                                    DropdownMenuItem(
                                                        text = { Text(wOpt) },
                                                        onClick = {
                                                            onUpdate { d ->
                                                                val nl = d.campuranBahan.toMutableList()
                                                                if (nl.isNotEmpty()) nl[0] = nl[0].copy(warna = wOpt)
                                                                d.copy(campuranBahan = nl)
                                                            }
                                                            mainWarnaDropOpen = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = mainMaterial.rasio,
                                        onValueChange = { v ->
                                            onUpdate { d ->
                                                val nl = d.campuranBahan.toMutableList()
                                                if (nl.isNotEmpty()) nl[0] = nl[0].copy(rasio = v)
                                                d.copy(campuranBahan = nl)
                                            }
                                        },
                                        label = { Text("Bagian") },
                                        singleLine = true,
                                        suffix = { Text("×") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }

                                // Batch Dropdown untuk Bahan Utama
                                Box(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
                                    OutlinedTextField(
                                        value = mainBatchDisplay,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Batch Bahan Utama") },
                                        trailingIcon = {
                                            IconButton(onClick = { mainBatchDropOpen = true }) {
                                                Icon(Icons.Outlined.ArrowDropDown, null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { mainBatchDropOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = mainBatchDropOpen,
                                        onDismissRequest = { mainBatchDropOpen = false },
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
                                                    onUpdate { d ->
                                                        val nl = d.campuranBahan.toMutableList()
                                                        if (nl.isNotEmpty()) nl[0] = nl[0].copy(selectedRawMaterial = rm)
                                                        d.copy(campuranBahan = nl)
                                                    }
                                                    mainBatchDropOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Checkbox: Bahan Pewarna (Campuran)
                            val isGunakanPewarna = entry.campuranBahan.size > 1
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onUpdate { d ->
                                        if (isGunakanPewarna) {
                                            d.copy(campuranBahan = d.campuranBahan.take(1))
                                        } else {
                                            d.copy(campuranBahan = d.campuranBahan + ColorMixEntry(rasio = "1"))
                                        }
                                    }
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isGunakanPewarna,
                                    onCheckedChange = { checked ->
                                        onUpdate { d ->
                                            if (!checked) {
                                                d.copy(campuranBahan = d.campuranBahan.take(1))
                                            } else {
                                                d.copy(campuranBahan = d.campuranBahan + ColorMixEntry(rasio = "1"))
                                            }
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Gunakan Bahan Pewarna Tambahan",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        "Centang untuk membandingkan takaran pewarna dengan bahan utama",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 3. Pewarna rows (hanya jika isGunakanPewarna)
                            if (isGunakanPewarna) {
                                entry.campuranBahan.drop(1).forEachIndexed { index, colorEntry ->
                                    val ci = index + 1 // Pewarna 1, Pewarna 2, etc.
                                    val fullIndex = index + 1
                                    var warnaDropOpen by remember { mutableStateOf(false) }
                                    var batchDropOpen by remember { mutableStateOf(false) }
                                    val batchDisplay = colorEntry.selectedRawMaterial?.let {
                                        "Batch: ${it.noTagihan} (${it.supplier ?: "Tanpa Supplier"})"
                                    } ?: "Pilih Batch Pewarna"

                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1.8f)) {
                                                ExposedDropdownMenuBox(expanded = warnaDropOpen, onExpandedChange = { warnaDropOpen = !warnaDropOpen }) {
                                                    OutlinedTextField(
                                                        value = colorEntry.warna,
                                                        onValueChange = { v ->
                                                            onUpdate { d ->
                                                                val nl = d.campuranBahan.toMutableList()
                                                                if (fullIndex in nl.indices) nl[fullIndex] = nl[fullIndex].copy(warna = v)
                                                                d.copy(campuranBahan = nl)
                                                            }
                                                        },
                                                        label = { Text("Pewarna $ci") },
                                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warnaDropOpen) },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                                    )
                                                    ExposedDropdownMenu(expanded = warnaDropOpen, onDismissRequest = { warnaDropOpen = false }) {
                                                        WARNA_OPTIONS.forEach { wOpt ->
                                                            DropdownMenuItem(
                                                                text = { Text(wOpt) },
                                                                onClick = {
                                                                    onUpdate { d ->
                                                                        val nl = d.campuranBahan.toMutableList()
                                                                        if (fullIndex in nl.indices) nl[fullIndex] = nl[fullIndex].copy(warna = wOpt)
                                                                        d.copy(campuranBahan = nl)
                                                                    }
                                                                    warnaDropOpen = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            OutlinedTextField(
                                                value = colorEntry.rasio,
                                                onValueChange = { v ->
                                                    onUpdate { d ->
                                                        val nl = d.campuranBahan.toMutableList()
                                                        if (fullIndex in nl.indices) nl[fullIndex] = nl[fullIndex].copy(rasio = v)
                                                        d.copy(campuranBahan = nl)
                                                    }
                                                },
                                                label = { Text("Bagian") },
                                                singleLine = true,
                                                suffix = { Text("×") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(0.9f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    onUpdate { d ->
                                                        val nl = d.campuranBahan.toMutableList().also { it.removeAt(fullIndex) }
                                                        d.copy(campuranBahan = nl)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Batch Dropdown untuk Pewarna
                                        Box(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
                                            OutlinedTextField(
                                                value = batchDisplay,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Batch Pewarna $ci") },
                                                trailingIcon = {
                                                    IconButton(onClick = { batchDropOpen = true }) {
                                                        Icon(Icons.Outlined.ArrowDropDown, null)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().clickable { batchDropOpen = true }
                                            )
                                            DropdownMenu(
                                                expanded = batchDropOpen,
                                                onDismissRequest = { batchDropOpen = false },
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
                                                            onUpdate { d ->
                                                                val nl = d.campuranBahan.toMutableList()
                                                                if (fullIndex in nl.indices) nl[fullIndex] = nl[fullIndex].copy(selectedRawMaterial = rm)
                                                                d.copy(campuranBahan = nl)
                                                            }
                                                            batchDropOpen = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { onUpdate { d -> d.copy(campuranBahan = d.campuranBahan + ColorMixEntry()) } },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tambah Pewarna Lain", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // 6. Estimasi Summary Card
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "${Formatters.dateLong(log.productionDate)} • ${item.machineName ?: "Tanpa Mesin"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Badge shift
                                if (!log.shiftName.isNullOrBlank() && log.shiftName != "PAGI" || log.shiftName == "PAGI") {
                                    val shiftColor = when(log.shiftName) {
                                        "SORE" -> Color(0xFFFF6F00)
                                        "MALAM" -> Color(0xFF303F9F)
                                        else -> Color(0xFF388E3C)
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = shiftColor.copy(alpha = 0.15f)) {
                                        Text(
                                            log.shiftName ?: "PAGI",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = shiftColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
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
                            // Tampilkan campuran warna jika ada
                            val mixSummary = parseColorMixtureSummary(log.colorMixture)
                            if (mixSummary.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        "🎨 $mixSummary",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
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

/**
 * Parse JSON color_mixture string menjadi human-readable summary persentase.
 * Input:  [{"color":"Natural","rasio":"9"},{"color":"Merah","rasio":"1"}]
 * Output: "Natural 90% + Merah 10%"
 */
fun parseColorMixtureSummary(colorMixture: String?): String {
    if (colorMixture.isNullOrBlank()) return ""
    return try {
        val entries = colorMixture
            .trimStart('[').trimEnd(']')
            .split("},")
            .filter { it.isNotBlank() }
            .map { chunk ->
                val clean = chunk.replace("{", "").replace("}", "").replace("\"", "")
                val map = clean.split(",").associate {
                    val kv = it.split(":")
                    kv.getOrElse(0) { "" }.trim() to kv.getOrElse(1) { "" }.trim()
                }
                val color = map["color"] ?: ""
                val rasio = map["rasio"]?.toDoubleOrNull() ?: 1.0
                color to rasio
            }.filter { it.first.isNotBlank() }

        if (entries.isEmpty()) return ""
        val total = entries.sumOf { it.second }
        if (total <= 0) return ""
        entries.joinToString(" + ") { (color, rasio) ->
            val pct = (rasio / total * 100).toInt()
            "$color $pct%"
        }
    } catch (_: Exception) { "" }
}
