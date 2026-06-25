package com.posbah.app.ui.screens.bmp.production

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ArrowDropDown
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
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpProductionLogEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpMasterProductRepository
import com.posbah.app.data.repository.BmpProductionLogRepository
import com.posbah.app.data.repository.BmpStockRepository
import com.posbah.app.data.repository.BmpBahanBakuRepository
import com.posbah.app.data.repository.OnlineWriteResult
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductionLogItem(
    val log: BmpProductionLogEntity,
    val product: BmpMasterProductEntity?
)

@HiltViewModel
class BmpProductionLogViewModel @Inject constructor(
    private val logRepo: BmpProductionLogRepository,
    private val masterProductRepo: BmpMasterProductRepository,
    private val stockRepo: BmpStockRepository,
    private val bahanBakuRepo: BmpBahanBakuRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val stockApi = stockRepo
    private val bahanBakuApi = bahanBakuRepo

    val products = masterProductRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val productStocks = stockRepo.observeStocks(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (tenantId.isNotBlank()) {
            viewModelScope.launch {
                logRepo.loadAll(tenantId)
            }
        }
    }

    val logItems: StateFlow<List<ProductionLogItem>> = combine(
        logRepo.observeAll(tenantId),
        masterProductRepo.observe(tenantId)
    ) { logs, products ->
        logs.filter { !it.isDeleted }.map { l ->
            val p = products.find { it.id == l.masterProductId }
            ProductionLogItem(l, p)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }

    fun addLog(
        productId: Long,
        quantityProduced: Double,
        quantityRejected: Double,
        rawMaterialUsedKg: Double,
        operatorName: String
    ) = viewModelScope.launch {
        val log = BmpProductionLogEntity(
            tenantId = tenantId,
            masterProductId = productId,
            quantityProduced = quantityProduced,
            quantityRejected = quantityRejected,
            rawMaterialUsedKg = rawMaterialUsedKg,
            operatorName = operatorName.ifBlank { null },
            productionDate = System.currentTimeMillis()
        )
        val result = logRepo.addProductionLog(context, log)
        if (result is OnlineWriteResult.Success) {
            stockApi.adjustStock(productId = log.productId, quantity = log.quantityProduced, reason = "PRODUKSI")
            if (log.rawMaterialUsedKg > 0) {
                bahanBakuApi.addUsage(materialId = log.rawMaterialId, quantity = -log.rawMaterialUsedKg, reason = "PRODUKSI")
            }
        } else if (result is OnlineWriteResult.Error) {
            _error.value = result.message
        } else if (result is OnlineWriteResult.NoConnection) {
            _error.value = "Tidak ada koneksi internet. Data tidak tersimpan."
        }
        // Full online: data langsung tersimpan di VPS, tidak perlu sync manual
    }

    fun deleteLog(log: BmpProductionLogEntity) = viewModelScope.launch {
        val result = logRepo.deleteProductionLog(context, tenantId, log)
        if (result is OnlineWriteResult.Success) {
            stockApi.adjustStock(productId = log.productId, quantity = -log.quantityProduced, reason = "PEMBATALAN PRODUKSI")
            if (log.rawMaterialUsedKg > 0) {
                bahanBakuApi.addUsage(materialId = log.rawMaterialId, quantity = log.rawMaterialUsedKg, reason = "PEMBATALAN PRODUKSI")
            }
        } else if (result is OnlineWriteResult.Error) {
            _error.value = result.message
        } else if (result is OnlineWriteResult.NoConnection) {
            _error.value = "Tidak ada koneksi internet. Hapus dibatalkan."
        }
        // Full online: data langsung tersimpan di VPS, tidak perlu sync manual
    }
}

@Composable
fun BmpProductionLogScreen(
    onBack: () -> Unit,
    viewModel: BmpProductionLogViewModel = hiltViewModel()
) {
    val logs by viewModel.logItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val productStocks by viewModel.productStocks.collectAsState()
    val context = LocalContext.current
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<BmpMasterProductEntity?>(null) }
    var quantityProducedInput by remember { mutableStateOf("") }
    var quantityRejectedInput by remember { mutableStateOf("") }
    var operatorNameInput by remember { mutableStateOf("") }
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<BmpProductionLogEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Log Produksi Harian", subtitle = "Manufaktur & Cetak", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedProduct = products.firstOrNull()
                    quantityProducedInput = ""
                    quantityRejectedInput = "0"
                    operatorNameInput = ""
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Tambah Log Produksi")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (logs.isEmpty()) {
                EmptyState(
                    title = "Belum ada log produksi",
                    description = "Catat hasil produksi harian cetak plastik untuk menambah stok barang jadi secara otomatis.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs) { item ->
                        val log = item.log
                        val p = item.product
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.PrecisionManufacturing,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        p?.title ?: "Produk #${log.masterProductId}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Tanggal: ${Formatters.dateLong(log.productionDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!log.operatorName.isNullOrBlank()) {
                                        Text(
                                            "Operator: ${log.operatorName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Reject: ${Formatters.number(log.quantityRejected)}") }
                                        )
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Bahan Baku: ${Formatters.number(log.rawMaterialUsedKg)} Kg") }
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        "+${Formatters.number(log.quantityProduced)} ${p?.unit ?: "pcs"}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    IconButton(
                                        onClick = {
                                            logToDelete = log
                                            showDeleteConfirmDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Hapus Log",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
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

    if (showAddDialog && selectedProduct != null) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        val prod = selectedProduct!!
        
        val qtyProduced = quantityProducedInput.toDoubleOrNull() ?: 0.0
        val rejectRateVal = prod.rejectRate
        val estimatedMaterialUsed = (qtyProduced * prod.beratGram / 1000.0) * (1.0 + (rejectRateVal / 100.0))

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Catat Produksi Harian") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pilih master produk dan masukkan hasil produksi cetak mesin hari ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Product Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentStockQty = productStocks.find { it.masterProductId == prod.id }?.quantity ?: 0.0
                        OutlinedTextField(
                            value = "${prod.title} (Stok: ${Formatters.number(currentStockQty)} ${prod.unit})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Produk Jadi") },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = true }) {
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            products.forEach { p ->
                                val pStockQty = productStocks.find { it.masterProductId == p.id }?.quantity ?: 0.0
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(p.title)
                                            Text(
                                                text = "Stok: ${Formatters.number(pStockQty)} ${p.unit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = p
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityProducedInput,
                        onValueChange = { quantityProducedInput = it },
                        label = { Text("Jumlah Produksi Sukses (${prod.unit})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = quantityRejectedInput,
                        onValueChange = { quantityRejectedInput = it },
                        label = { Text("Jumlah Reject / Rusak (${prod.unit})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = operatorNameInput,
                        onValueChange = { operatorNameInput = it },
                        label = { Text("Nama Operator (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Calculation Summary Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "ESTIMASI KONSUMSI BAHAN BAKU",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Bahan Baku", style = MaterialTheme.typography.bodySmall)
                                Text(prod.jenisBahanBaku.ifEmpty { "(Belum diatur)" }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Berat / unit", style = MaterialTheme.typography.bodySmall)
                                Text("${Formatters.number(prod.beratGram)} gram", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Reject Rate (Master)", style = MaterialTheme.typography.bodySmall)
                                Text("${Formatters.number(prod.rejectRate)} %", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Total Bahan Terpakai", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text("${Formatters.number(estimatedMaterialUsed)} Kg", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val qtyProd = quantityProducedInput.toDoubleOrNull()
                        val qtyRej = quantityRejectedInput.toDoubleOrNull() ?: 0.0
                        if (qtyProd == null || qtyProd <= 0.0) {
                            Toast.makeText(context, "Jumlah produksi tidak valid", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        
                        viewModel.addLog(
                            productId = prod.id,
                            quantityProduced = qtyProd,
                            quantityRejected = qtyRej,
                            rawMaterialUsedKg = estimatedMaterialUsed,
                            operatorName = operatorNameInput
                        )
                        showAddDialog = false
                        Toast.makeText(context, "Log produksi berhasil disimpan", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showDeleteConfirmDialog && logToDelete != null) {
        val log = logToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Log Produksi") },
            text = {
                Text("Apakah Anda yakin ingin menghapus log produksi ini? Tindakan ini akan mengembalikan stok barang jadi yang telah ditambahkan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLog(log)
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "Log produksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Batal") }
            }
        )
    }
}
