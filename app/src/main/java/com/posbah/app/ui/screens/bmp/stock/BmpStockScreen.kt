package com.posbah.app.ui.screens.bmp.stock

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
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
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.BmpProductStockEntity
import com.posbah.app.data.local.entities.BmpStockLedgerEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.remote.SupabaseSyncManager
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpStockRepository
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class StockProductItem(
    val stock: BmpProductStockEntity?,
    val product: BmpMasterProductEntity
) {
    val quantity: Double get() = stock?.quantity ?: 0.0
    val hppTotalPcs: Double get() = product.hppTotalPcs
}

data class LedgerItem(
    val ledger: BmpStockLedgerEntity,
    val productTitle: String
)

@HiltViewModel
class BmpStockViewModel @Inject constructor(
    private val stockRepo: BmpStockRepository,
    private val masterProductRepo: com.posbah.app.data.repository.BmpMasterProductRepository,
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    val stockItems: StateFlow<List<StockProductItem>> = combine(
        stockRepo.observeStocks(tenantId),
        masterProductRepo.observe(tenantId)
    ) { stocks: List<BmpProductStockEntity>, products: List<BmpMasterProductEntity> ->
        products.map { p: BmpMasterProductEntity ->
            val s = stocks.find { it.masterProductId == p.id }
            StockProductItem(s, p)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stockItemsState: StateFlow<UiState<List<StockProductItem>>> = combine(
        stockRepo.observeStocks(tenantId),
        masterProductRepo.observe(tenantId)
    ) { stocks: List<BmpProductStockEntity>, products: List<BmpMasterProductEntity> ->
        val list = products.map { p ->
            val s = stocks.find { it.masterProductId == p.id }
            StockProductItem(s, p)
        }
        UiState.Success(list)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val ledgerItems: StateFlow<List<LedgerItem>> = combine(
        stockRepo.observeAllLedger(tenantId),
        masterProductRepo.observe(tenantId)
    ) { ledgers: List<BmpStockLedgerEntity>, products: List<BmpMasterProductEntity> ->
        ledgers.map { l: BmpStockLedgerEntity ->
            val p = products.find { it.id == l.masterProductId }
            LedgerItem(l, p?.title ?: "Produk #${l.masterProductId}")
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun adjustStockManual(productId: Long, change: Double, notes: String) = viewModelScope.launch {
        stockRepo.adjustStock(
            masterItemId = productId,
            change = change.toInt(),
            mutationType = "PENYESUAIAN",
            referenceId = 0L,
            notes = notes
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SupabaseSyncManager.syncAll(context, db, tenantId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun BmpStockScreen(
    onBack: () -> Unit,
    viewModel: BmpStockViewModel = hiltViewModel()
) {
    val stockState by viewModel.stockItemsState.collectAsState()
    val ledgers by viewModel.ledgerItems.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustProduct by remember { mutableStateOf<BmpMasterProductEntity?>(null) }
    var adjustChangeInput by remember { mutableStateOf("") }
    var adjustNotes by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Stok Barang Jadi", subtitle = "Manajemen Gudang", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stok Gudang") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat Mutasi") }
                )
            }

            if (selectedTab == 0) {
                when (val ui = stockState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Success -> {
                        val stockList = ui.data
                        if (stockList.isEmpty()) {
                            EmptyState(
                                title = "Belum ada produk master",
                                description = "Tambah produk master terlebih dahulu di menu Produk untuk melacak stok gudang.",
                                actionLabel = "Kembali",
                                onAction = onBack
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(stockList) { item ->
                                        val currentQty = item.quantity
                                        val isLowStock = item.stock != null && currentQty <= item.stock.minStockAlert && item.stock.minStockAlert > 0.0
                                        
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
                                                    color = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(42.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Outlined.Inventory,
                                                            null,
                                                            tint = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        item.product.title,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    if (item.product.jenisBahanBaku.isNotEmpty()) {
                                                        Text(
                                                            "Bahan: ${item.product.jenisBahanBaku}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        "HPP: ${Formatters.rupiah(item.hppTotalPcs)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (isLowStock) {
                                                        Text(
                                                            "Stok Minim! (Batas: ${Formatters.number(item.stock?.minStockAlert ?: 0.0)} ${item.product.unit})",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                                Column(
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text(
                                                        "${Formatters.number(currentQty)} ${item.product.unit}",
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            adjustProduct = item.product
                                                            adjustChangeInput = ""
                                                            adjustNotes = ""
                                                            showAdjustDialog = true
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Outlined.Edit,
                                                            contentDescription = "Edit Stok",
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Footer: total nilai aset stok
                                val totalValue = stockList.sumOf { it.quantity * it.hppTotalPcs }
                                Surface(
                                    tonalElevation = 4.dp,
                                    shadowElevation = 8.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Total Nilai Aset Stok:",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = Formatters.rupiah(totalValue),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(ui.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                if (ledgers.isEmpty()) {
                    EmptyState(
                        title = "Belum ada riwayat mutasi",
                        description = "Setiap aktivitas produksi atau penjualan invoice akan dicatat di kartu stok ini secara otomatis.",
                        actionLabel = "Kembali",
                        onAction = onBack
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ledgers) { item ->
                            val l = item.ledger
                            val isPositive = l.quantityChange >= 0
                            
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Outlined.History,
                                                null,
                                                tint = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.productTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            "Tipe: ${l.mutationType} • Sisa: ${Formatters.number(l.finalStock)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!l.notes.isNullOrBlank()) {
                                            Text(
                                                l.notes,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = (if (isPositive) "+" else "") + Formatters.number(l.quantityChange),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdjustDialog && adjustProduct != null) {
        val prod = adjustProduct!!
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Penyesuaian Stok Manual") },
            text = {
                Column {
                    Text(
                        "Sesuaikan kuantitas stok untuk '${prod.title}'. Masukkan angka positif untuk menambah stok, atau negatif untuk mengurangi stok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adjustChangeInput,
                        onValueChange = { adjustChangeInput = it },
                        label = { Text("Kuantitas Perubahan (+ / -)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustNotes,
                        onValueChange = { adjustNotes = it },
                        label = { Text("Alasan / Catatan Penyesuaian") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val change = adjustChangeInput.toDoubleOrNull()
                        if (change == null || change == 0.0) {
                            Toast.makeText(context, "Kuantitas tidak valid", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (adjustNotes.isBlank()) {
                            Toast.makeText(context, "Catatan/alasan wajib diisi", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        viewModel.adjustStockManual(prod.id, change, adjustNotes)
                        showAdjustDialog = false
                        Toast.makeText(context, "Stok berhasil disesuaikan", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) { Text("Batal") }
            }
        )
    }
}
