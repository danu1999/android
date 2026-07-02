@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.posbah.app.ui.screens.bmp.products

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpMachineEntity
import com.posbah.app.data.local.entities.BmpMoldEntity
import com.posbah.app.data.repository.*
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MachineMoldViewModel @Inject constructor(
    private val machineRepo: BmpMachineRepository,
    private val moldRepo: BmpMoldRepository,
    private val productRepo: BmpMasterProductRepository,
    private val authRepo: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val tenantId = authRepo.activeTenantId().orEmpty()

    val machines = machineRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val molds = moldRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val products = productRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                machineRepo.refresh()
                moldRepo.refresh()
                productRepo.refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Machine Form State
    data class MachineFormState(
        val editing: BmpMachineEntity? = null,
        val show: Boolean = false
    )
    private val _machineForm = MutableStateFlow(MachineFormState())
    val machineForm = _machineForm.asStateFlow()

    fun openMachineCreate() {
        _machineForm.update { MachineFormState(editing = BmpMachineEntity(tenantId = tenantId, name = ""), show = true) }
    }
    fun openMachineEdit(m: BmpMachineEntity) {
        _machineForm.update { MachineFormState(editing = m, show = true) }
    }
    fun closeMachineForm() { _machineForm.update { MachineFormState() } }
    fun updateMachineField(transform: (BmpMachineEntity) -> BmpMachineEntity) {
        val cur = _machineForm.value.editing ?: return
        _machineForm.update { it.copy(editing = transform(cur)) }
    }

    fun saveMachine() = viewModelScope.launch {
        val e = _machineForm.value.editing ?: return@launch
        if (e.name.isBlank()) return@launch
        _machineForm.update { MachineFormState() }
        val data = BmpMachineData(
            id = e.id,
            tenantId = e.tenantId,
            name = e.name,
            depreciationMonthly = e.depreciationMonthly,
            powerConsumptionKw = e.powerConsumptionKw,
            electricityCostDaily = e.electricityCostDaily,
            operatorSalaryMonthly = e.operatorSalaryMonthly,
            overheadAllocatedMonthly = e.overheadAllocatedMonthly,
            hoursCapacityMonthly = e.hoursCapacityMonthly,
            moldId = e.moldId
        )
        val res = machineRepo.upsert(data)
        if (res is OnlineWriteResult.Error) {
            _machineForm.update { MachineFormState(editing = e, show = true) }
            _error.value = res.message
        } else {
            Toast.makeText(context, "Mesin berhasil disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleMachineActive(m: BmpMachineEntity) = viewModelScope.launch {
        val data = BmpMachineData(
            id = m.id,
            tenantId = m.tenantId,
            name = m.name,
            depreciationMonthly = m.depreciationMonthly,
            powerConsumptionKw = m.powerConsumptionKw,
            electricityCostDaily = m.electricityCostDaily,
            operatorSalaryMonthly = m.operatorSalaryMonthly,
            overheadAllocatedMonthly = m.overheadAllocatedMonthly,
            hoursCapacityMonthly = m.hoursCapacityMonthly,
            isActive = !m.isActive,
            moldId = m.moldId
        )
        val res = machineRepo.upsert(data)
        if (res is OnlineWriteResult.Error) {
            _error.value = res.message
        } else {
            Toast.makeText(context, "Status mesin ${m.name} diubah menjadi ${if (!m.isActive) "Aktif" else "Mati"}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteMachine(id: Long) = viewModelScope.launch {
        val res = machineRepo.delete(id)
        if (res is OnlineWriteResult.Error) {
            _error.value = res.message
        } else {
            Toast.makeText(context, "Mesin berhasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    // Mold Form State
    data class MoldFormState(
        val editing: BmpMoldEntity? = null,
        val show: Boolean = false
    )
    private val _moldForm = MutableStateFlow(MoldFormState())
    val moldForm = _moldForm.asStateFlow()

    fun openMoldCreate() {
        _moldForm.update { MoldFormState(editing = BmpMoldEntity(tenantId = tenantId, name = ""), show = true) }
    }
    fun openMoldEdit(m: BmpMoldEntity) {
        _moldForm.update { MoldFormState(editing = m, show = true) }
    }
    fun closeMoldForm() { _moldForm.update { MoldFormState() } }
    fun updateMoldField(transform: (BmpMoldEntity) -> BmpMoldEntity) {
        val cur = _moldForm.value.editing ?: return
        _moldForm.update { it.copy(editing = transform(cur)) }
    }

    fun saveMold() = viewModelScope.launch {
        val e = _moldForm.value.editing ?: return@launch
        if (e.name.isBlank()) return@launch
        _moldForm.update { MoldFormState() }
        val data = BmpMoldData(
            id = e.id,
            tenantId = e.tenantId,
            name = e.name,
            purchasePrice = e.purchasePrice,
            expectedShotsLifetime = e.expectedShotsLifetime,
            masterProductId = e.masterProductId
        )
        val res = moldRepo.upsert(data)
        if (res is OnlineWriteResult.Error) {
            _moldForm.update { MoldFormState(editing = e, show = true) }
            _error.value = res.message
        } else {
            Toast.makeText(context, "Cetakan/Matras berhasil disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteMold(id: Long) = viewModelScope.launch {
        val res = moldRepo.delete(id)
        if (res is OnlineWriteResult.Error) {
            _error.value = res.message
        } else {
            Toast.makeText(context, "Cetakan/Matras berhasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    /** v2.19.25: Reset usage_count setelah matras diservis */
    fun resetMoldUsage(id: Long) = viewModelScope.launch {
        val res = moldRepo.resetUsageCount(id)
        if (res is OnlineWriteResult.Error) {
            _error.value = res.message
        } else {
            Toast.makeText(context, "✅ Pemakaian matras berhasil direset ke 0", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MachineMoldManagementScreen(
    onBack: () -> Unit,
    viewModel: MachineMoldViewModel = hiltViewModel()
) {
    val machines by viewModel.machines.collectAsState()
    val molds by viewModel.molds.collectAsState()
    val products by viewModel.products.collectAsState()

    val machineForm by viewModel.machineForm.collectAsState()
    val moldForm by viewModel.moldForm.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Daftar Mesin", "Daftar Matras/Cetakan")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                PosBahTopBar(
                    title = "Mesin & Matras",
                    subtitle = "Manajemen aset cetakan & overhead mesin",
                    onBack = onBack
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) viewModel.openMachineCreate()
                    else viewModel.openMoldCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (selectedTab == 0) {
                // Mesin Tab
                if (machines.isEmpty()) {
                    EmptyState(
                        title = "Belum ada mesin cetak",
                        description = "Tambahkan mesin injection moulding Anda untuk mengalokasikan biaya listrik & depresiasi.",
                        actionLabel = "+ Tambah Mesin",
                        onAction = viewModel::openMachineCreate
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(machines, key = { it.id }) { machine ->
                            MachineCard(
                                machine = machine,
                                moldName = molds.find { it.id == machine.moldId }?.name,
                                onEdit = { viewModel.openMachineEdit(machine) },
                                onDelete = { viewModel.deleteMachine(machine.id) },
                                onToggleActive = { viewModel.toggleMachineActive(machine) }
                            )
                        }
                    }
                }
            } else {
                // Molds Tab
                if (molds.isEmpty()) {
                    EmptyState(
                        title = "Belum ada matras cetakan",
                        description = "Daftarkan cetakan matras Anda untuk mengalokasikan biaya penyusutan cetakan per pcs produk.",
                        actionLabel = "+ Tambah Matras",
                        onAction = viewModel::openMoldCreate
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(molds, key = { it.id }) { mold ->
                            val matchedProd = products.find { it.id == mold.masterProductId }
                            MoldCard(
                                mold = mold,
                                matchedProductTitle = matchedProd?.title ?: "Belum Terhubung ke Produk",
                                onEdit = { viewModel.openMoldEdit(mold) },
                                onDelete = { viewModel.deleteMold(mold.id) },
                                onResetUsage = { viewModel.resetMoldUsage(mold.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Machine Dialog Form
    if (machineForm.show && machineForm.editing != null) {
        val edit = machineForm.editing!!
        AlertDialog(
            onDismissRequest = viewModel::closeMachineForm,
            title = { Text(if (edit.id == 0L) "Tambah Mesin" else "Edit Mesin") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = edit.name,
                        onValueChange = { v -> viewModel.updateMachineField { it.copy(name = v) } },
                        label = { Text("Nama Mesin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = if (edit.depreciationMonthly == 0.0) "" else edit.depreciationMonthly.toLong().toString(),
                        onValueChange = { v ->
                            val n = v.replace(",", "").replace(".", "").toDoubleOrNull() ?: 0.0
                            viewModel.updateMachineField { it.copy(depreciationMonthly = n) }
                        },
                        label = { Text("Penyusutan Mesin / Bulan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // v2.19.24: Biaya listrik harian dihapus dari form mesin.
                    // Diisi langsung di halaman Log Produksi (electricityCostActual per shift).
                    // Field electricityCostDaily di entity tetap ada sebagai pre-fill default.
                    // v2.19.24: Gaji operator dihapus dari form mesin.
                    // Upah riil kini berasal dari absensi operator di Log Produksi per shift.
                    OutlinedTextField(
                        value = edit.overheadAllocatedMonthly.let { if (it == 0.0) "" else it.toLong().toString() },
                        onValueChange = { v ->
                            val n = v.replace(",", "").replace(".", "").toDoubleOrNull() ?: 0.0
                            viewModel.updateMachineField { it.copy(overheadAllocatedMonthly = n) }
                        },
                        label = { Text("Alokasi BOP Bulanan Lainnya (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    // Dropdown Pilih Matras
                    var expandMoldDropdown by remember { mutableStateOf(false) }
                    val selectedMoldName = molds.find { it.id == edit.moldId }?.name
                    ExposedDropdownMenuBox(
                        expanded = expandMoldDropdown,
                        onExpandedChange = { expandMoldDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedMoldName ?: "— Tidak ada matras —",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Matras/Cetakan Terpasang") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandMoldDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandMoldDropdown,
                            onDismissRequest = { expandMoldDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("— Tidak ada matras —", color = Color.Gray) },
                                onClick = {
                                    viewModel.updateMachineField { it.copy(moldId = null) }
                                    expandMoldDropdown = false
                                }
                            )
                            molds.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name) },
                                    onClick = {
                                        viewModel.updateMachineField { it.copy(moldId = m.id) }
                                        expandMoldDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Mesin Aktif (Hidup)", fontWeight = FontWeight.Bold)
                        Switch(
                            checked = edit.isActive,
                            onCheckedChange = { checked ->
                                viewModel.updateMachineField { it.copy(isActive = checked) }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::saveMachine) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeMachineForm) {
                    Text("Batal")
                }
            }
        )
    }

    // Mold Dialog Form
    if (moldForm.show && moldForm.editing != null) {
        val edit = moldForm.editing!!
        var showProductSelector by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = viewModel::closeMoldForm,
            title = { Text(if (edit.id == 0L) "Tambah Matras" else "Edit Matras") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = edit.name,
                        onValueChange = { v -> viewModel.updateMoldField { it.copy(name = v) } },
                        label = { Text("Nama Matras / Cetakan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = if (edit.purchasePrice == 0.0) "" else edit.purchasePrice.toLong().toString(),
                        onValueChange = { v ->
                            val n = v.replace(",", "").replace(".", "").toDoubleOrNull() ?: 0.0
                            viewModel.updateMoldField { it.copy(purchasePrice = n) }
                        },
                        label = { Text("Harga Beli Matras (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = if (edit.expectedShotsLifetime == 0) "" else edit.expectedShotsLifetime.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull() ?: 100000
                            viewModel.updateMoldField { it.copy(expectedShotsLifetime = n) }
                        },
                        label = { Text("Expected Shots Lifetime (Kali Pakai)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val matchedProd = products.find { it.id == edit.masterProductId }
                    OutlinedTextField(
                        value = matchedProd?.title ?: "Pilih Produk...",
                        onValueChange = {},
                        label = { Text("Dihubungkan ke Produk") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProductSelector = true }
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::saveMold) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeMoldForm) {
                    Text("Batal")
                }
            }
        )

        if (showProductSelector) {
            AlertDialog(
                onDismissRequest = { showProductSelector = false },
                title = { Text("Pilih Produk") },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateMoldField { it.copy(masterProductId = null) }
                                        showProductSelector = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("(Tidak terhubung ke produk manapun)", color = Color.Gray)
                            }
                        }
                        items(products) { p ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateMoldField { it.copy(masterProductId = p.id) }
                                        showProductSelector = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(p.title, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showProductSelector = false }) { Text("Tutup") }
                }
            )
        }
    }
}

@Composable
fun MachineCard(
    machine: BmpMachineEntity,
    moldName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = machine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = machine.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Penyusutan Bulanan:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(Formatters.rupiah(machine.depreciationMonthly), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            // v2.19.24: Biaya listrik harian dihapus dari tampilan kartu mesin
            // (diisi langsung di halaman Log Produksi per shift)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("BOP Bulanan Alokasi:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(Formatters.rupiah(machine.overheadAllocatedMonthly), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Status Mesin:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(
                    text = if (machine.isActive) "AKTIF / HIDUP" else "MATI / OFF",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (machine.isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            // Matras terpasang
            Spacer(Modifier.height(6.dp))
            if (moldName != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎯 Matras: $moldName",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Belum ada matras terpasang — edit mesin untuk memilih matras",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFF8D6E00),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MoldCard(
    mold: BmpMoldEntity,
    matchedProductTitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetUsage: () -> Unit = {}
) {
    val usagePct = if (mold.expectedShotsLifetime > 0)
        (mold.usageCount.toFloat() / mold.expectedShotsLifetime.toFloat()).coerceIn(0f, 1f)
    else 0f
    val needsService = usagePct >= 0.9f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (needsService) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mold.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (needsService) Color(0xFF8D6E00) else MaterialTheme.colorScheme.onSurface
                    )
                    if (needsService) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFF8F00),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "⚠️ Perlu Servis",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Harga Cetakan:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(Formatters.rupiah(mold.purchasePrice), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Expected Shots Lifetime:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("${Formatters.number(mold.expectedShotsLifetime.toDouble())} kali", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Pemakaian Aktual:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("${Formatters.number(mold.usageCount.toDouble())} kali (${(usagePct * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (needsService) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(6.dp))
            // Usage progress bar
            LinearProgressIndicator(
                progress = usagePct,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (needsService) Color(0xFFFF8F00) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Amortisasi Per Shot:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                val amortPerShot = if (mold.expectedShotsLifetime > 0) mold.purchasePrice / mold.expectedShotsLifetime else 0.0
                Text(Formatters.rupiah(amortPerShot), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Dihubungkan ke: $matchedProductTitle",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            // v2.19.25: Tombol reset pemakaian setelah servis (muncul jika usage >= 70%)
            if (usagePct >= 0.7f) {
                Spacer(Modifier.height(8.dp))
                var showResetConfirm by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE65100)
                    )
                ) {
                    Text("🔧 Reset Pemakaian Setelah Servis")
                }
                if (showResetConfirm) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirm = false },
                        title = { Text("Konfirmasi Reset") },
                        text = { Text("Yakin ingin mereset pemakaian matras \"${mold.name}\" ke 0? Lakukan ini hanya setelah matras sudah diservis atau diganti.") },
                        confirmButton = {
                            Button(
                                onClick = { onResetUsage(); showResetConfirm = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                            ) { Text("Ya, Reset") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetConfirm = false }) { Text("Batal") }
                        }
                    )
                }
            }
        }
    }
}
