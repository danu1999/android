@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.posbah.app.ui.screens.bmp.production

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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpMachineEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpMoldEntity
import com.posbah.app.data.local.entities.BmpWorkOrderEntity
import com.posbah.app.data.repository.*
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WorkOrderViewModel @Inject constructor(
    private val repo: BmpWorkOrderRepository,
    private val masterProductRepo: BmpMasterProductRepository,
    private val machineRepo: BmpMachineRepository,
    private val moldRepo: BmpMoldRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    val tenantId = authRepo.activeTenantId().orEmpty()

    val workOrders = repo.observe(tenantId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val masterProducts = masterProductRepo.observe(tenantId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val machines = machineRepo.observe(tenantId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val molds = moldRepo.observe(tenantId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _selectedStatusFilter = MutableStateFlow("ALL")
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repo.refresh()
            masterProductRepo.refresh()
            machineRepo.refresh()
            moldRepo.refresh()
            _isRefreshing.value = false
        }
    }

    fun upsertWorkOrder(data: BmpWorkOrderData, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repo.upsert(data)) {
                is OnlineWriteResult.Success -> {
                    repo.refresh()
                    onResult(true, null)
                }
                is OnlineWriteResult.Error -> onResult(false, res.message)
                else -> onResult(false, "Koneksi offline atau tidak terhubung")
            }
        }
    }

    fun updateStatus(id: Long, newStatus: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val existing = repo.list().find { it.id == id }
            if (existing != null) {
                val updated = existing.copy(
                    status = newStatus,
                    actualCompletionDate = if (newStatus == "COMPLETED") System.currentTimeMillis() else existing.actualCompletionDate
                )
                when (val res = repo.upsert(updated)) {
                    is OnlineWriteResult.Success -> {
                        repo.refresh()
                        onResult(true, null)
                    }
                    is OnlineWriteResult.Error -> onResult(false, res.message)
                    else -> onResult(false, "Koneksi offline atau tidak terhubung")
                }
            }
        }
    }

    fun deleteWorkOrder(id: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repo.delete(id)) {
                is OnlineWriteResult.Success -> {
                    repo.refresh()
                    onResult(true, null)
                }
                is OnlineWriteResult.Error -> onResult(false, res.message)
                else -> onResult(false, "Koneksi offline atau tidak terhubung")
            }
        }
    }
}

@Composable
fun WorkOrderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductionLog: (Long) -> Unit = {},
    viewModel: WorkOrderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val workOrders by viewModel.workOrders.collectAsState()
    val masterProducts by viewModel.masterProducts.collectAsState()
    val machines by viewModel.machines.collectAsState()
    val molds by viewModel.molds.collectAsState()
    val currentFilter by viewModel.selectedStatusFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingOrder by remember { mutableStateOf<BmpWorkOrderEntity?>(null) }
    var deleteConfirmOrder by remember { mutableStateOf<BmpWorkOrderEntity?>(null) }

    val filteredList = remember(workOrders, currentFilter) {
        if (currentFilter == "ALL") workOrders
        else workOrders.filter { it.status == currentFilter }
    }

    // Aggregates
    val totalSPK = workOrders.size
    val pendingCount = workOrders.count { it.status == "PENDING" }
    val inProgressCount = workOrders.count { it.status == "IN_PROGRESS" }
    val completedCount = workOrders.count { it.status == "COMPLETED" }

    Scaffold(
        topBar = {
            PosBahTopBar(
                title = "Surat Perintah Kerja (SPK)",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingOrder = null
                    showFormDialog = true
                },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Buat SPK") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // KPI Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Total SPK",
                    value = "$totalSPK",
                    color = MaterialTheme.colorScheme.primary
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Antrean",
                    value = "$pendingCount",
                    color = Color(0xFFE65100)
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Berjalan",
                    value = "$inProgressCount",
                    color = Color(0xFF1565C0)
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Selesai",
                    value = "$completedCount",
                    color = Color(0xFF2E7D32)
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = currentFilter == "ALL",
                    onClick = { viewModel.setStatusFilter("ALL") },
                    label = { Text("Semua ($totalSPK)") }
                )
                FilterChip(
                    selected = currentFilter == "PENDING",
                    onClick = { viewModel.setStatusFilter("PENDING") },
                    label = { Text("Antrean") }
                )
                FilterChip(
                    selected = currentFilter == "IN_PROGRESS",
                    onClick = { viewModel.setStatusFilter("IN_PROGRESS") },
                    label = { Text("Berjalan") }
                )
                FilterChip(
                    selected = currentFilter == "COMPLETED",
                    onClick = { viewModel.setStatusFilter("COMPLETED") },
                    label = { Text("Selesai") }
                )
            }

            if (filteredList.isEmpty()) {
                EmptyState(
                    title = "Belum Ada Surat Perintah Kerja",
                    description = "Tekan tombol + Buat SPK untuk menerbitkan instruksi produksi pabrik baru."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val product = masterProducts.find { it.id == item.masterProductId }
                        val machine = machines.find { it.id == item.machineId }
                        val mold = molds.find { it.id == item.moldId }

                        WorkOrderCard(
                            item = item,
                            productName = product?.title ?: item.masterProductName ?: "Produk #${item.masterProductId}",
                            machineName = machine?.name,
                            moldName = mold?.name,
                            onStart = {
                                viewModel.updateStatus(item.id, "IN_PROGRESS") { success, err ->
                                    if (success) Toast.makeText(context, "SPK dimulai!", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, err ?: "Gagal", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onComplete = {
                                viewModel.updateStatus(item.id, "COMPLETED") { success, err ->
                                    if (success) Toast.makeText(context, "SPK diselesaikan!", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, err ?: "Gagal", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEdit = {
                                editingOrder = item
                                showFormDialog = true
                            },
                            onDelete = {
                                deleteConfirmOrder = item
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        WorkOrderFormDialog(
            existing = editingOrder,
            masterProducts = masterProducts,
            machines = machines,
            molds = molds,
            onDismiss = { showFormDialog = false },
            onSave = { data ->
                viewModel.upsertWorkOrder(data) { success, err ->
                    if (success) {
                        Toast.makeText(context, "SPK berhasil disimpan", Toast.LENGTH_SHORT).show()
                        showFormDialog = false
                    } else {
                        Toast.makeText(context, err ?: "Gagal menyimpan SPK", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (deleteConfirmOrder != null) {
        val order = deleteConfirmOrder!!
        AlertDialog(
            onDismissRequest = { deleteConfirmOrder = null },
            title = { Text("Hapus SPK") },
            text = { Text("Apakah Anda yakin ingin menghapus SPK ${order.spkNumber}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkOrder(order.id) { success, err ->
                            if (success) {
                                Toast.makeText(context, "SPK dihapus", Toast.LENGTH_SHORT).show()
                                deleteConfirmOrder = null
                            } else {
                                Toast.makeText(context, err ?: "Gagal menghapus", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmOrder = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 16.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WorkOrderCard(
    item: BmpWorkOrderEntity,
    productName: String,
    machineName: String?,
    moldName: String?,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (item.targetQuantity > 0) {
        (item.completedQuantity / item.targetQuantity).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    val percent = (progress * 100).toInt()

    val statusColor = when (item.status) {
        "COMPLETED" -> Color(0xFF2E7D32)
        "IN_PROGRESS" -> Color(0xFF1565C0)
        "CANCELLED" -> Color(0xFFC62828)
        else -> Color(0xFFE65100)
    }

    val statusLabel = when (item.status) {
        "COMPLETED" -> "Selesai"
        "IN_PROGRESS" -> "Sedang Berjalan"
        "CANCELLED" -> "Dibatalkan"
        else -> "Antrean"
    }

    val priorityColor = when (item.priority) {
        "URGENT" -> Color(0xFFD32F2F)
        "HIGH" -> Color(0xFFF57C00)
        else -> Color(0xFF616161)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: SPK Number & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        item.spkNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Dibuat: ${Formatters.dateShort(item.createdAt)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.priority != "NORMAL") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                item.priority,
                                color = priorityColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            statusLabel,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Product & Equipment Info
            Text(
                productName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!machineName.isNullOrBlank()) {
                    Text(
                        "Mesin: $machineName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!moldName.isNullOrBlank()) {
                    Text(
                        "Matras: $moldName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Progres: ${Formatters.number(item.completedQuantity.toLong())} / ${Formatters.number(item.targetQuantity.toLong())} pcs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$percent%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percent >= 100) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (percent >= 100) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (item.rejectedQuantity > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Total Reject: ${Formatters.number(item.rejectedQuantity.toLong())} pcs",
                    fontSize = 11.sp,
                    color = Color(0xFFC62828)
                )
            }

            if (!item.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Catatan: ${item.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.status == "PENDING") {
                    FilledTonalButton(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mulai Produksi")
                    }
                } else if (item.status == "IN_PROGRESS") {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tandai Selesai")
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun WorkOrderFormDialog(
    existing: BmpWorkOrderEntity?,
    masterProducts: List<BmpMasterProductEntity>,
    machines: List<BmpMachineEntity>,
    molds: List<BmpMoldEntity>,
    onDismiss: () -> Unit,
    onSave: (BmpWorkOrderData) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val defaultSpkNum = remember { "SPK-${sdf.format(Date())}-${(100..999).random()}" }

    var spkNumber by remember { mutableStateOf(existing?.spkNumber ?: defaultSpkNum) }
    var selectedProductId by remember { mutableStateOf(existing?.masterProductId ?: masterProducts.firstOrNull()?.id ?: 0L) }
    var targetQuantityStr by remember { mutableStateOf(existing?.targetQuantity?.toLong()?.toString() ?: "1000") }
    var selectedMachineId by remember { mutableStateOf<Long?>(existing?.machineId ?: machines.firstOrNull()?.id) }
    var selectedMoldId by remember { mutableStateOf<Long?>(existing?.moldId ?: molds.firstOrNull()?.id) }
    var priority by remember { mutableStateOf(existing?.priority ?: "NORMAL") }
    var status by remember { mutableStateOf(existing?.status ?: "PENDING") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    var productExpanded by remember { mutableStateOf(false) }
    var machineExpanded by remember { mutableStateOf(false) }
    var moldExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Buat SPK Baru" else "Edit SPK") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = spkNumber,
                    onValueChange = { spkNumber = it },
                    label = { Text("Nomor SPK") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Product Dropdown
                val selectedProduct = masterProducts.find { it.id == selectedProductId }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedProduct?.title ?: "Pilih Produk...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produk Target") },
                        trailingIcon = {
                            IconButton(onClick = { productExpanded = true }) {
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { productExpanded = true }
                    )
                    DropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        masterProducts.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text(prod.title) },
                                onClick = {
                                    selectedProductId = prod.id
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetQuantityStr,
                    onValueChange = { targetQuantityStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target Kuantitas (pcs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Machine Dropdown
                val selectedMachine = machines.find { it.id == selectedMachineId }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedMachine?.name ?: "Pilih Mesin (Opsional)...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mesin Produksi") },
                        trailingIcon = {
                            IconButton(onClick = { machineExpanded = true }) {
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { machineExpanded = true }
                    )
                    DropdownMenu(
                        expanded = machineExpanded,
                        onDismissRequest = { machineExpanded = false }
                    ) {
                        machines.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.name) },
                                onClick = {
                                    selectedMachineId = m.id
                                    machineExpanded = false
                                }
                            )
                        }
                    }
                }

                // Mold Dropdown
                val selectedMold = molds.find { it.id == selectedMoldId }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedMold?.name ?: "Pilih Matras/Cetakan (Opsional)...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Matras / Cetakan") },
                        trailingIcon = {
                            IconButton(onClick = { moldExpanded = true }) {
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { moldExpanded = true }
                    )
                    DropdownMenu(
                        expanded = moldExpanded,
                        onDismissRequest = { moldExpanded = false }
                    ) {
                        molds.forEach { mld ->
                            DropdownMenuItem(
                                text = { Text(mld.name) },
                                onClick = {
                                    selectedMoldId = mld.id
                                    moldExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority Selection
                Text("Prioritas:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NORMAL", "HIGH", "URGENT").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan SPK") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = targetQuantityStr.toDoubleOrNull() ?: 1000.0
                    val prodTitle = masterProducts.find { it.id == selectedProductId }?.title
                    val data = BmpWorkOrderData(
                        id = existing?.id ?: 0L,
                        spkNumber = spkNumber,
                        masterProductId = selectedProductId,
                        masterProductName = prodTitle,
                        targetQuantity = qty,
                        completedQuantity = existing?.completedQuantity ?: 0.0,
                        rejectedQuantity = existing?.rejectedQuantity ?: 0.0,
                        machineId = selectedMachineId,
                        moldId = selectedMoldId,
                        startDate = existing?.startDate ?: System.currentTimeMillis(),
                        status = status,
                        priority = priority,
                        notes = notes.ifBlank { null }
                    )
                    onSave(data)
                }
            ) {
                Text("Simpan SPK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
