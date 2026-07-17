package com.posbah.app.ui.screens.pos

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.ProductRepository
import com.posbah.app.data.repository.TransactionRepository
import com.posbah.app.data.repository.OutletRepository
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

import com.posbah.app.data.repository.CustomerRepository

@HiltViewModel
class MarginAnalysisViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val outletRepository: OutletRepository,
    private val customerRepository: CustomerRepository,
    private val sessionState: com.posbah.app.data.repository.SessionState,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    val customers = customerRepository.customers

    val tenantId = authRepository.activeTenantId().orEmpty()

    /** Outlet yang tersedia untuk filter (Owner melihat semua). */
    val availableOutlets = outletRepository.observe(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Outlet yang dipilih sebagai filter.
     * null = semua outlet (hanya OWNER yang bisa null).
     */
    private val _selectedOutletId = MutableStateFlow<Long?>(sessionState.lockedEmployeeOutletId.value)
    val selectedOutletId = _selectedOutletId.asStateFlow()

    /** Ganti filter outlet — hanya OWNER yang bisa memilih null atau outlet lain. */
    fun selectOutletFilter(outletId: Long?) {
        viewModelScope.launch {
            val role = authRepository.getActiveUser()?.role ?: "KASIR"
            if (role == "OWNER") {
                _selectedOutletId.value = outletId
            }
            // Non-OWNER: diabaikan, locked ke outlet mereka
        }
    }

    val transactions = _selectedOutletId
        .flatMapLatest { outletId ->
            if (outletId != null) {
                transactionRepository.observeForOutlet(tenantId, outletId)
            } else {
                transactionRepository.observe(tenantId) // owner sees all
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val products = _selectedOutletId
        .flatMapLatest { outletId ->
            if (outletId != null) {
                productRepository.observeForOutlet(tenantId, outletId)
            } else {
                productRepository.observe(tenantId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _userRole = MutableStateFlow("KASIR")
    val userRole = _userRole.asStateFlow()

    private val _transactionItems = MutableStateFlow<List<TransactionItemEntity>>(emptyList())
    val transactionItems = _transactionItems.asStateFlow()

    init {
        // Load active user role and enforce outlet lock for non-OWNER
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            val role = user?.role ?: "KASIR"
            _userRole.value = role
            if (role != "OWNER") {
                val lockedOutlet = sessionState.lockedEmployeeOutletId.value
                _selectedOutletId.value = lockedOutlet
            }
        }
        viewModelScope.launch {
            _selectedOutletId.collect { outletId ->
                try {
                    transactionRepository.refresh(outletId)
                } catch (_: Exception) {}
            }
        }
        viewModelScope.launch {
            try {
                customerRepository.refresh()
            } catch (_: Exception) {}
        }
        refreshItems()
        // Auto-refresh when transactions update
        viewModelScope.launch {
            transactions.collect {
                refreshItems()
            }
        }
    }

    fun refreshItems() {
        val txs = transactions.value
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<TransactionItemEntity>()
            coroutineScope {
                val deferreds = txs.map { tx ->
                    async {
                        try {
                            transactionRepository.listItemsForTransaction(tx.id)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }
                list.addAll(awaitAll(*deferreds.toTypedArray()).flatten())
            }
            _transactionItems.value = list
        }
    }

    fun settleTransaction(context: android.content.Context, tx: TransactionEntity, paymentMethod: String) {
        viewModelScope.launch {
            val updated = tx.copy(
                paymentMethod = paymentMethod,
                amountPaid = tx.total,
                change = 0.0,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.update(updated)
            refreshItems()
        }
    }

    fun addWastage(
        context: android.content.Context,
        product: ProductEntity,
        quantity: Int,
        reason: String,
        outletId: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val costPrice = product.costPrice
            val totalLoss = quantity * costPrice
            val todayStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
            val prefix = when {
                product.category.contains("rental", ignoreCase = true) -> "RN"
                product.category.contains("laundry", ignoreCase = true) -> "LD"
                else -> "FNB"
            }
            val receiptNumber = "EXP-$prefix-WASTAGE-$todayStr-${java.util.UUID.randomUUID().toString().take(6).uppercase()}"
            val txId = System.currentTimeMillis()
            val tx = TransactionEntity(
                id = txId,
                tenantId = tenantId,
                outletId = outletId,
                employeeId = 1L,
                customerName = "Wastage / Spoilage",
                receiptNumber = receiptNumber,
                date = System.currentTimeMillis(),
                subtotal = -totalLoss,
                total = -totalLoss,
                paymentMethod = "CASH",
                status = "COMPLETED",
                type = "EXPENSE",
                notes = "Wastage: ${product.name} (Qty: $quantity ${product.unit}) - Alasan: $reason"
            )

            val newStock = (product.stock - quantity).coerceAtLeast(0)
            val txData = com.posbah.app.data.repository.TransactionData(
                id = tx.id,
                tenantId = tx.tenantId,
                outletId = tx.outletId,
                employeeId = tx.employeeId,           // ✅ tambah employeeId
                receiptNumber = tx.receiptNumber,
                type = tx.type,
                status = tx.status,
                totalAmount = tx.total,
                subtotal = tx.subtotal,               // ✅ tambah subtotal
                paymentMethod = tx.paymentMethod,
                amountPaid = tx.amountPaid,
                change = tx.change,
                customerId = tx.customerId,
                notes = tx.notes,
                date = tx.date,
                isDeleted = tx.isDeleted,
                updatedAt = tx.updatedAt
            )
            transactionRepository.checkout(txData, emptyList<com.posbah.app.data.repository.TransactionItemData>(), productRepository)
            productRepository.updateStock(product.id, newStock)

            refreshItems()
            onSuccess()
        }
    }
}

data class ProductAnalysisItem(
    val product: ProductEntity,
    val unitsSold: Double,
    val revenue: Double,
    val cogs: Double,
    val grossProfit: Double,
    val marginPercent: Double
)

fun getMonthlyMaintenance(wholesalePrices: String?): Double {
    if (wholesalePrices.isNullOrBlank()) return 0.0
    return try {
        val regex = Regex("""\"monthlyMaintenance\"\s*:\s*([\d\.]+)""")
        val match = regex.find(wholesalePrices)
        match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    } catch (e: Exception) {
        0.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarginAnalysisScreen(
    onBack: () -> Unit,
    viewModel: MarginAnalysisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    var outletDropdownExpanded by remember { mutableStateOf(false) }

    if (userRole != "OWNER") {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Akses Ditolak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Hanya Owner yang dapat melihat analisis margin & keuntungan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Kembali")
                }
            }
        }
        return
    }

    val transactions by viewModel.transactions.collectAsState()
    val products by viewModel.products.collectAsState()
    val transactionItems by viewModel.transactionItems.collectAsState()
    val availableOutlets by viewModel.availableOutlets.collectAsState()
    val selectedOutletId by viewModel.selectedOutletId.collectAsState()
    val customers by viewModel.customers.collectAsState()

    // Filters state
    var posMode by remember { mutableStateOf("SEMUA") } // SEMUA, FNB, RENTAL, LAUNDRY
    var datePreset by remember { mutableStateOf("HARI_INI") } // HARI_INI, 7_HARI, 30_HARI, KUSTOM
    var customerType by remember { mutableStateOf("SEMUA") } // SEMUA, PELANGGAN, UMUM
    var activeTab by remember { mutableStateOf("HISTORY") } // "HISTORY", "MENU_ENGINEERING", "AGING_AR"
    var txToSettle by remember { mutableStateOf<TransactionEntity?>(null) }
    var showWastageDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Date pickers state
    val calendar = Calendar.getInstance()
    var startDateMillis by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var endDateMillis by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        )
    }

    // Apply presets
    LaunchedEffect(datePreset) {
        val now = Calendar.getInstance()
        when (datePreset) {
            "HARI_INI" -> {
                startDateMillis = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                endDateMillis = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
            "7_HARI" -> {
                endDateMillis = now.timeInMillis
                startDateMillis = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            "30_HARI" -> {
                endDateMillis = now.timeInMillis
                startDateMillis = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
    }

    // Dynamic calculations
    val filteredTx = remember(transactions, posMode, customerType, startDateMillis, endDateMillis) {
        transactions.filter { tx ->
            // Date Filter
            val matchesDate = tx.date in startDateMillis..endDateMillis

            // POS Mode Filter
            val matchesPos = when (posMode) {
                "FNB" -> tx.receiptNumber.startsWith("FNB-") || tx.receiptNumber.startsWith("EXP-FNB-")
                "RENTAL" -> tx.receiptNumber.startsWith("RN-") || tx.receiptNumber.startsWith("EXP-RN-")
                "LAUNDRY" -> tx.receiptNumber.startsWith("LD-") || tx.receiptNumber.startsWith("EXP-LD-")
                else -> true
            }

            // Customer Type Filter
            val isRegCustomer = !tx.customerName.isNullOrBlank() && 
                                tx.customerName != "Umum" && 
                                tx.customerName != "Pelanggan Umum"
            val matchesCustomer = when (customerType) {
                "PELANGGAN" -> isRegCustomer
                "UMUM" -> !isRegCustomer
                else -> true
            }

            matchesDate && matchesPos && matchesCustomer
        }
    }

    // Detail modal state
    var selectedTxDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedTxItems by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }


    // Aggregate margin calculations
    val salesTx = filteredTx.filter { it.type != "EXPENSE" }
    val expenseTx = filteredTx.filter { it.type == "EXPENSE" }

    val totalRevenue = salesTx.sumOf { it.total }
    val totalCogs = salesTx.sumOf { tx ->
        val txItems = transactionItems.filter { it.transactionId == tx.id }
        txItems.sumOf { item ->
            // Fallback: jika costPrice di transaksi = 0 (produk belum punya HPP saat dijual),
            // gunakan costPrice produk saat ini agar margin tetap bisa terhitung.
            val prod = products.find { it.id == item.productId }
            val effectiveCostPrice = if (item.costPrice > 0.0) item.costPrice else (prod?.costPrice ?: 0.0)
            when {
                tx.receiptNumber.startsWith("FNB-") ->
                    item.quantity * effectiveCostPrice
                tx.receiptNumber.startsWith("RN-") -> {
                    val days = tx.queueNumber ?: 1
                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                    val dailyCogs = (if (effectiveCostPrice > 1_000_000.0) effectiveCostPrice / 1825.0 else effectiveCostPrice) + (monthlyMaint / 30.0)
                    dailyCogs * days
                }
                tx.receiptNumber.startsWith("LD-") -> {
                    val isKg = prod?.unit == "Kg"
                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                    val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                    val baseCogs = qty * effectiveCostPrice
                    val maintShare = qty * (monthlyMaint / 300.0)
                    baseCogs + maintShare
                }
                else -> item.quantity * effectiveCostPrice
            }
        }
    }
    val totalExpenses = expenseTx.sumOf { Math.abs(it.total) }
    val grossProfit = totalRevenue - totalCogs
    val netProfit = grossProfit - totalExpenses
    val marginPercent = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100.0 else 0.0
    val netMarginPercent = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0
    val markupPercent = if (totalCogs > 0) (grossProfit / totalCogs) * 100.0 else 0.0

    // Menu Engineering & Product margin analysis calculations
    val productAnalysisItems = remember(products, filteredTx, transactionItems) {
        val salesTxOnly = filteredTx.filter { it.type != "EXPENSE" }
        products.map { prod ->
            val salesForProd = transactionItems.filter { item ->
                item.productId == prod.id && salesTxOnly.any { it.id == item.transactionId }
            }
            val unitsSold = salesForProd.sumOf { item ->
                val tx = salesTxOnly.find { it.id == item.transactionId }
                val isKg = tx?.receiptNumber?.startsWith("LD-") == true && prod.unit == "Kg"
                if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
            }
            val revenue = salesForProd.sumOf { item ->
                val tx = salesTxOnly.find { it.id == item.transactionId }
                val isKg = tx?.receiptNumber?.startsWith("LD-") == true && prod.unit == "Kg"
                val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                qty * item.price
            }
            val cogs = salesForProd.sumOf { item ->
                val tx = salesTxOnly.find { it.id == item.transactionId }
                val isKg = tx?.receiptNumber?.startsWith("LD-") == true && prod.unit == "Kg"
                val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                // Fallback: jika costPrice di transaksi = 0 (produk belum punya HPP saat dijual),
                // gunakan costPrice produk saat ini agar margin tetap bisa terhitung.
                val effectiveCostPrice = if (item.costPrice > 0.0) item.costPrice else prod.costPrice
                when {
                    tx?.receiptNumber?.startsWith("FNB-") == true -> item.quantity * effectiveCostPrice
                    tx?.receiptNumber?.startsWith("RN-") == true -> {
                        val days = tx.queueNumber ?: 1
                        val monthlyMaint = getMonthlyMaintenance(prod.wholesalePrices)
                        val dailyCogs = (if (effectiveCostPrice > 1_000_000.0) effectiveCostPrice / 1825.0 else effectiveCostPrice) + (monthlyMaint / 30.0)
                        dailyCogs * days
                    }
                    tx?.receiptNumber?.startsWith("LD-") == true -> {
                        val monthlyMaint = getMonthlyMaintenance(prod.wholesalePrices)
                        val baseCogs = qty * effectiveCostPrice
                        val maintShare = qty * (monthlyMaint / 300.0)
                        baseCogs + maintShare
                    }
                    else -> item.quantity * effectiveCostPrice
                }
            }
            val grossProfit = revenue - cogs
            val marginPercent = if (revenue > 0) (grossProfit / revenue) * 100.0 else {
                if (prod.price > 0) ((prod.price - prod.costPrice) / prod.price) * 100.0 else 0.0
            }

            ProductAnalysisItem(
                product = prod,
                unitsSold = unitsSold,
                revenue = revenue,
                cogs = cogs,
                grossProfit = grossProfit,
                marginPercent = marginPercent
            )
        }
    }

    val soldProducts = remember(productAnalysisItems) {
        productAnalysisItems.filter { it.unitsSold > 0 }
    }
    val avgPopularity = remember(soldProducts) {
        if (soldProducts.isNotEmpty()) soldProducts.sumOf { it.unitsSold } / soldProducts.size else 0.0
    }
    val avgMarginPercent = remember(soldProducts) {
        if (soldProducts.isNotEmpty()) soldProducts.sumOf { it.marginPercent } / soldProducts.size else 0.0
    }

    fun getMenuCategory(item: ProductAnalysisItem): String {
        return when {
            item.unitsSold >= avgPopularity && item.marginPercent >= avgMarginPercent -> "STAR"
            item.unitsSold >= avgPopularity && item.marginPercent < avgMarginPercent -> "PLOWHORSE"
            item.unitsSold < avgPopularity && item.marginPercent >= avgMarginPercent -> "PUZZLE"
            else -> "DOG"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Analisis Margin & Keuntungan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compact Collapsible Filter Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFilterDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚙️ Filter: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        val outletLabel = if (selectedOutletId == null) "Semua Outlet" else (availableOutlets.find { it.id == selectedOutletId }?.name ?: "Semua Outlet")
                        val dateLabel = when (datePreset) {
                            "HARI_INI" -> "Hari Ini"
                            "7_HARI" -> "7 Hari"
                            "30_HARI" -> "30 Hari"
                            else -> "${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(startDateMillis))} - ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(endDateMillis))}"
                        }
                        val customerLabel = when (customerType) {
                            "PELANGGAN" -> "Pelanggan"
                            "UMUM" -> "Umum"
                            else -> "Semua"
                        }
                        Text(
                            text = "$posMode • $dateLabel • $customerLabel • $outletLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Ubah ⚙️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (showFilterDialog) {
                AlertDialog(
                    onDismissRequest = { showFilterDialog = false },
                    title = { Text("Konfigurasi Filter Laporan", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        ) {
                            // POS Mode
                            Column {
                                Text("Modul Kasir", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val modes = listOf("SEMUA", "FNB", "RENTAL", "LAUNDRY")
                                    modes.forEach { m ->
                                        val active = posMode == m
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { posMode = m }
                                        ) {
                                            Text(
                                                text = m,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Date Preset
                            Column {
                                Text("Rentang Waktu", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val presets = listOf("HARI_INI" to "Hari Ini", "7_HARI" to "7 Hari", "30_HARI" to "30 Hari", "KUSTOM" to "Kustom")
                                    presets.forEach { (p, label) ->
                                        val active = datePreset == p
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { datePreset = p }
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Custom Date Picker (if KUSTOM is active)
                            if (datePreset == "KUSTOM") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Kustom Tanggal", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        // Start date
                                        val sCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                                        val sDialog = DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val cal = Calendar.getInstance().apply {
                                                    set(y, m, d, 0, 0, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                startDateMillis = cal.timeInMillis
                                            },
                                            sCal.get(Calendar.YEAR),
                                            sCal.get(Calendar.MONTH),
                                            sCal.get(Calendar.DAY_OF_MONTH)
                                        )
                                        OutlinedButton(
                                            onClick = { sDialog.show() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text("Mulai: ${SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(startDateMillis))}", fontSize = 10.sp)
                                        }

                                        // End date
                                        val eCal = Calendar.getInstance().apply { timeInMillis = endDateMillis }
                                        val eDialog = DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val cal = Calendar.getInstance().apply {
                                                    set(y, m, d, 23, 59, 59)
                                                    set(Calendar.MILLISECOND, 999)
                                                }
                                                endDateMillis = cal.timeInMillis
                                            },
                                            eCal.get(Calendar.YEAR),
                                            eCal.get(Calendar.MONTH),
                                            eCal.get(Calendar.DAY_OF_MONTH)
                                        )
                                        OutlinedButton(
                                            onClick = { eDialog.show() },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text("Hingga: ${SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(endDateMillis))}", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            // Customer Type
                            Column {
                                Text("Filter Pelanggan", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val types = listOf("SEMUA" to "Semua", "PELANGGAN" to "Terdaftar", "UMUM" to "Walk-in")
                                    types.forEach { (t, label) ->
                                        val active = customerType == t
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { customerType = t }
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Outlet selection
                            if (userRole == "OWNER" && availableOutlets.size > 1) {
                                Column {
                                    Text("Filter Outlet", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.height(4.dp))
                                    val selectedOutletName = if (selectedOutletId == null) "Semua Outlet" else (availableOutlets.find { it.id == selectedOutletId }?.name ?: "Semua Outlet")
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { outletDropdownExpanded = !outletDropdownExpanded },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = selectedOutletName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Icon(
                                                    imageVector = if (outletDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = outletDropdownExpanded,
                                            onDismissRequest = { outletDropdownExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Semua Outlet", fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    viewModel.selectOutletFilter(null)
                                                    outletDropdownExpanded = false
                                                }
                                            )
                                            availableOutlets.forEach { outlet ->
                                                DropdownMenuItem(
                                                    text = { Text(outlet.name) },
                                                    onClick = {
                                                        viewModel.selectOutletFilter(outlet.id)
                                                        outletDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (userRole != "OWNER") {
                                val myOutlet = availableOutlets.firstOrNull { it.id == selectedOutletId }
                                if (myOutlet != null) {
                                    Text(
                                        text = "📍 Outlet: ${myOutlet.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showFilterDialog = false }) {
                            Text("Terapkan Filter")
                        }
                    }
                )
            }

            // Redesigned Unified Dashboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF004D40).copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE0F2F1), Color.White)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Laba Bersih Bisnis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = Formatters.rupiah(netProfit),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF004D40)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFF00796B),
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text(
                                    text = "${String.format("%.1f%%", netMarginPercent)} M. Bersih",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Horizontally Scrollable Secondary Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Omzet Card
                Surface(
                    modifier = Modifier.width(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Pendapatan Kotor", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(Modifier.height(2.dp))
                        Text(Formatters.rupiah(totalRevenue), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                    }
                }

                // COGS/HPP Card
                Surface(
                    modifier = Modifier.width(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFBE9E7),
                    border = BorderStroke(1.dp, Color(0xFFD84315).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total HPP (COGS)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
                        Spacer(Modifier.height(2.dp))
                        Text(Formatters.rupiah(totalCogs), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFBF360C))
                    }
                }

                // Laba Kotor Card
                Surface(
                    modifier = Modifier.width(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8EAF6),
                    border = BorderStroke(1.dp, Color(0xFF283593).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Laba Kotor", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF283593))
                        Spacer(Modifier.height(2.dp))
                        Text(Formatters.rupiah(grossProfit), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A237E))
                    }
                }

                // Biaya Card
                Surface(
                    modifier = Modifier.width(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE),
                    border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Biaya Usaha", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        Spacer(Modifier.height(2.dp))
                        Text(Formatters.rupiah(totalExpenses), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFB71C1C))
                    }
                }

                // Margins & Markup Card
                Surface(
                    modifier = Modifier.width(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3E5F5),
                    border = BorderStroke(1.dp, Color(0xFF6A1B9A).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("M. Kotor & Markup", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                        Spacer(Modifier.height(2.dp))
                        Text("Kotor: ${String.format("%.1f%%", marginPercent)} | Mkup: ${String.format("%.1f%%", markupPercent)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF4A148C))
                    }
                }
            }

            // Tab Toggle Selector
            if (isSmallScreen) {
                var activeTabDropdownExpanded by remember { mutableStateOf(false) }
                val selectedTabLabel = when (activeTab) {
                    "HISTORY" -> "Riwayat Transaksi"
                    "MENU_ENGINEERING" -> "📊 Analisis Menu"
                    else -> "👥 Buku Piutang"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    OutlinedButton(
                        onClick = { activeTabDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (activeTab) {
                                    "HISTORY" -> Icon(Icons.Outlined.History, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    "MENU_ENGINEERING" -> Text("📊", fontSize = 14.sp)
                                    else -> Icon(Icons.Outlined.People, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = selectedTabLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (activeTabDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = activeTabDropdownExpanded,
                        onDismissRequest = { activeTabDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.History, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Riwayat Transaksi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                activeTab = "HISTORY"
                                activeTabDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📊 Analisis Menu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                activeTab = "MENU_ENGINEERING"
                                activeTabDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.People, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Buku Piutang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                activeTab = "AGING_AR"
                                activeTabDropdownExpanded = false
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { activeTab = "HISTORY" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "HISTORY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeTab == "HISTORY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Outlined.History, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Riwayat", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { activeTab = "MENU_ENGINEERING" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "MENU_ENGINEERING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeTab == "MENU_ENGINEERING") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("📊 Analisis Menu", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { activeTab = "AGING_AR" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "AGING_AR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeTab == "AGING_AR") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Outlined.People, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Buku Piutang", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!outletDropdownExpanded) {
                when (activeTab) {
                "HISTORY" -> {
                    // ── Quick Filter Pills ──────────────────────────────────
                    var ledgerFilter by remember { mutableStateOf("SEMUA") }

                    // ── Daily Summary + Filtered List ───────────────────────
                    // Pre-compute COGS for all transactions
                    data class TxWithMetrics(
                        val tx: TransactionEntity,
                        val isExpense: Boolean,
                        val cogs: Double,
                        val margin: Double,
                        val marginPct: Double,
                        val dateStr: String,
                        val dayKey: String,
                        val posType: String,
                        val posColor: Pair<Color, Color>,
                        val txItems: List<TransactionItemEntity>
                    )

                    val txWithMetricsList = remember(filteredTx, transactionItems, products) {
                        filteredTx.map { tx ->
                            val isExpense = tx.type == "EXPENSE"
                            val txItems = transactionItems.filter { it.transactionId == tx.id }
                            val cogs = if (isExpense) 0.0 else txItems.sumOf { item ->
                                val prod = products.find { it.id == item.productId }
                                val effectiveCostPrice = if (item.costPrice > 0.0) item.costPrice else (prod?.costPrice ?: 0.0)
                                when {
                                    tx.receiptNumber.startsWith("FNB-") -> item.quantity * effectiveCostPrice
                                    tx.receiptNumber.startsWith("RN-") -> {
                                        val days = tx.queueNumber ?: 1
                                        val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                        val dailyCogs = (if (effectiveCostPrice > 1_000_000.0) effectiveCostPrice / 1825.0 else effectiveCostPrice) + (monthlyMaint / 30.0)
                                        dailyCogs * days
                                    }
                                    tx.receiptNumber.startsWith("LD-") -> {
                                        val isKg = prod?.unit == "Kg"
                                        val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                        val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                                        qty * effectiveCostPrice + qty * (monthlyMaint / 300.0)
                                    }
                                    else -> item.quantity * effectiveCostPrice
                                }
                            }
                            val margin = if (isExpense) 0.0 else tx.total - cogs
                            val marginPct = if (!isExpense && tx.total > 0) (margin / tx.total) * 100.0 else 0.0
                            val posType = when {
                                tx.receiptNumber.startsWith("FNB-") || tx.receiptNumber.startsWith("EXP-FNB-") -> "FnB"
                                tx.receiptNumber.startsWith("RN-") || tx.receiptNumber.startsWith("EXP-RN-") -> "Rental"
                                tx.receiptNumber.startsWith("LD-") || tx.receiptNumber.startsWith("EXP-LD-") -> "Laundry"
                                else -> "POS"
                            }
                            val posColor = when (posType) {
                                "FnB" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                "Rental" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                                "Laundry" -> Color(0xFFE1F5FE) to Color(0xFF0288D1)
                                else -> Color(0xFFECEFF1) to Color(0xFF37474F)
                            }
                            TxWithMetrics(
                                tx = tx,
                                isExpense = isExpense,
                                cogs = cogs,
                                margin = margin,
                                marginPct = marginPct,
                                dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(tx.date)),
                                dayKey = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id")).format(Date(tx.date)),
                                posType = posType,
                                posColor = posColor,
                                txItems = txItems
                            )
                        }
                    }

                    val ledgerFiltered = remember(txWithMetricsList, ledgerFilter) {
                        when (ledgerFilter) {
                            "PEMASUKAN" -> txWithMetricsList.filter { !it.isExpense }
                            "PENGELUARAN" -> txWithMetricsList.filter { it.isExpense }
                            else -> txWithMetricsList
                        }
                    }

                    // Group by day for daily banners
                    val groupedByDay = remember(ledgerFiltered) {
                        ledgerFiltered.groupBy { it.dayKey }
                    }

                    // ── Summary Card ──────────────────────────────────────────
                    val totalIncome = remember(txWithMetricsList) { txWithMetricsList.filter { !it.isExpense }.sumOf { it.tx.total } }
                    val totalExpense = remember(txWithMetricsList) { txWithMetricsList.filter { it.isExpense }.sumOf { it.tx.total } }
                    val netProfit = remember(txWithMetricsList) { txWithMetricsList.filter { !it.isExpense }.sumOf { it.margin } }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Pemasukan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text(Formatters.rupiah(totalIncome), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                                Spacer(Modifier.height(2.dp))
                                Text("Pengeluaran", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Text(Formatters.rupiah(totalExpense), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Laba Bersih", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Formatters.rupiah(netProfit),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (netProfit >= 0) Color(0xFF1B5E20) else Color(0xFFC62828)
                                )
                                Text("${filteredTx.size} Transaksi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // ── Quick Filter Pills ────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SEMUA", "PEMASUKAN", "PENGELUARAN").forEach { filter ->
                            val isSelected = ledgerFilter == filter
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = when {
                                    isSelected && filter == "PEMASUKAN" -> Color(0xFF2E7D32)
                                    isSelected && filter == "PENGELUARAN" -> Color(0xFFC62828)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.clickable { ledgerFilter = filter }
                            ) {
                                Text(
                                    text = when(filter) {
                                        "PEMASUKAN" -> "↗ Pemasukan"
                                        "PENGELUARAN" -> "↙ Pengeluaran"
                                        else -> "⇅ Semua"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // ── Timeline Ledger List ──────────────────────────────────
                    if (ledgerFiltered.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = if (filteredTx.isEmpty()) "📋 Belum Ada Transaksi" else "Tidak ada data untuk filter ini",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (filteredTx.isEmpty())
                                        "Lakukan transaksi pertama Anda di layar POS untuk melihat analisis riwayat di sini."
                                    else
                                        "Coba ubah rentang waktu atau filter yang digunakan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            groupedByDay.forEach { (dayLabel, dayItems) ->
                                // ── Daily Summary Banner ─────────────────────
                                val dayIncome = dayItems.filter { !it.isExpense }.sumOf { it.tx.total }
                                val dayExpense = dayItems.filter { it.isExpense }.sumOf { it.tx.total }
                                val dayNet = dayItems.filter { !it.isExpense }.sumOf { it.margin } - dayExpense

                                item(key = "header_$dayLabel") {
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (dayNet >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                dayLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Net: ${if (dayNet >= 0) "+" else ""}${Formatters.rupiah(dayNet)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (dayNet >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }

                                // ── Transaction Cards with Timeline ──────────
                                items(dayItems, key = { it.tx.id }) { item ->
                                    val isExpense = item.isExpense
                                    val cardAlignment = if (isExpense) Alignment.End else Alignment.Start
                                    val anchorColor = if (isExpense) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    val bgColor = if (isExpense) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                                    val borderColor = if (isExpense) Color(0xFFC62828).copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        // ── Centre timeline line ──────────────
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(100.dp)
                                                .align(Alignment.Center)
                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        )
                                        // ── Anchor dot on timeline ────────────
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .align(Alignment.Center)
                                                .background(anchorColor, shape = RoundedCornerShape(50))
                                        )

                                        // ── Transaction Card (left or right) ─
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = if (isExpense) Alignment.CenterEnd else Alignment.CenterStart
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(
                                                    topStart = if (isExpense) 12.dp else 2.dp,
                                                    topEnd = if (isExpense) 2.dp else 12.dp,
                                                    bottomStart = 12.dp,
                                                    bottomEnd = 12.dp
                                                ),
                                                color = bgColor,
                                                border = BorderStroke(1.dp, borderColor),
                                                modifier = Modifier
                                                    .fillMaxWidth(0.82f)
                                                    .clickable {
                                                        selectedTxDetails = item.tx
                                                        selectedTxItems = item.txItems
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    // Header row: receipt number + type badge
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            if (isExpense) "↙ ${item.tx.receiptNumber}" else "↗ ${item.tx.receiptNumber}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = anchorColor,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = item.posColor.first,
                                                            contentColor = item.posColor.second
                                                        ) {
                                                            Text(
                                                                text = if (isExpense) "${item.posType} (Biaya)" else item.posType,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(item.dateStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        if (isExpense) "Keterangan: ${item.tx.notes ?: "-"}" else "Pelanggan: ${item.tx.customerName ?: "Umum"}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(Modifier.height(6.dp))
                                                    // Amount + margin/label
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Bottom
                                                    ) {
                                                        if (isExpense) {
                                                            Text(
                                                                "Biaya Usaha",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFFC62828)
                                                            )
                                                        } else {
                                                            Column {
                                                                Text(
                                                                    "Laba: ${Formatters.rupiah(item.margin)} (${String.format("%.1f%%", item.marginPct)})",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = if (item.margin >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                                                )
                                                                Spacer(Modifier.height(2.dp))
                                                                LinearProgressIndicator(
                                                                    progress = (item.marginPct.coerceIn(0.0, 100.0) / 100.0).toFloat(),
                                                                    modifier = Modifier.fillMaxWidth(0.7f).height(3.dp),
                                                                    color = if (item.margin >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = Formatters.rupiah(item.tx.total),
                                                            fontWeight = FontWeight.Black,
                                                            color = if (isExpense) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Spacer after each day
                                item(key = "footer_$dayLabel") {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                "MENU_ENGINEERING" -> {
                    var menuSearchQuery by remember { mutableStateOf("") }
                    val filteredProductsForAnalysis = remember(productAnalysisItems, menuSearchQuery) {
                        productAnalysisItems.filter {
                            it.product.name.contains(menuSearchQuery, ignoreCase = true)
                        }
                    }

                    val starCount = productAnalysisItems.count { getMenuCategory(it) == "STAR" }
                    val plowhorseCount = productAnalysisItems.count { getMenuCategory(it) == "PLOWHORSE" }
                    val puzzleCount = productAnalysisItems.count { getMenuCategory(it) == "PUZZLE" }
                    val dogCount = productAnalysisItems.count { getMenuCategory(it) == "DOG" }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (isSmallScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.width(135.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("⭐ Stars", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1B5E20))
                                    Text("$starCount Menu", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1B5E20))
                                    Text("Laris & Margin Tinggi", fontSize = 8.sp, color = Color(0xFF2E7D32))
                                }
                            }
                            Surface(
                                modifier = Modifier.width(135.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF3E0),
                                border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("↗️ Plowhorses", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFE65100))
                                    Text("$plowhorseCount Menu", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFE65100))
                                    Text("Laris tapi Margin Rendah", fontSize = 8.sp, color = Color(0xFFE65100))
                                }
                            }
                            Surface(
                                modifier = Modifier.width(135.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8EAF6),
                                border = BorderStroke(1.dp, Color(0xFF1A237E).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("➡️ Puzzles", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1A237E))
                                    Text("$puzzleCount Menu", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1A237E))
                                    Text("Sepi tapi Margin Tinggi", fontSize = 8.sp, color = Color(0xFF1A237E))
                                }
                            }
                            Surface(
                                modifier = Modifier.width(135.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("⬇️ Dogs", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFB71C1C))
                                    Text("$dogCount Menu", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFB71C1C))
                                    Text("Sepi & Margin Rendah", fontSize = 8.sp, color = Color(0xFFC62828))
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("⭐ Stars", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                                    Text("$starCount Menu", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1B5E20))
                                    Text("Laris & Margin Tinggi", fontSize = 9.sp, color = Color(0xFF2E7D32))
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF3E0),
                                border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("↗️ Plowhorses", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE65100))
                                    Text("$plowhorseCount Menu", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFE65100))
                                    Text("Laris tapi Margin Rendah", fontSize = 9.sp, color = Color(0xFFE65100))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8EAF6),
                                border = BorderStroke(1.dp, Color(0xFF1A237E).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("➡️ Puzzles", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A237E))
                                    Text("$puzzleCount Menu", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1A237E))
                                    Text("Sepi tapi Margin Tinggi", fontSize = 9.sp, color = Color(0xFF1A237E))
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("⬇️ Dogs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB71C1C))
                                    Text("$dogCount Menu", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFB71C1C))
                                    Text("Sepi & Margin Rendah", fontSize = 9.sp, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = menuSearchQuery,
                                onValueChange = { menuSearchQuery = it },
                                placeholder = { Text("Cari menu...", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showWastageDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Wastage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (filteredProductsForAnalysis.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tidak ada produk untuk filter ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            var expandedProductIds by remember { mutableStateOf(emptySet<Long>()) }

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredProductsForAnalysis, key = { it.product.id }) { item ->
                                    val cat = getMenuCategory(item)
                                    val catLabel = when (cat) {
                                        "STAR" -> "⭐ Star"
                                        "PLOWHORSE" -> "↗️ Plowhorse"
                                        "PUZZLE" -> "➡️ Puzzle"
                                        else -> "⬇️ Dog"
                                    }
                                    val catColor = when (cat) {
                                        "STAR" -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
                                        "PLOWHORSE" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                                        "PUZZLE" -> Color(0xFFE8EAF6) to Color(0xFF1A237E)
                                        else -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
                                    }

                                    val isKg = item.product.unit == "Kg"
                                    val unitsSoldStr = if (isKg) {
                                        "${String.format("%.1f", item.unitsSold)} Kg"
                                    } else {
                                        "${item.unitsSold.toInt()} ${item.product.unit}"
                                    }

                                    val components = remember(item.product.costPriceBreakdown) {
                                        parseHppBreakdown(item.product.costPriceBreakdown)
                                    }
                                    val isOlahan = components.isNotEmpty()
                                    val isExpanded = expandedProductIds.contains(item.product.id)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = isOlahan) {
                                                expandedProductIds = if (isExpanded) {
                                                    expandedProductIds - item.product.id
                                                } else {
                                                    expandedProductIds + item.product.id
                                                }
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = catColor.first,
                                                    contentColor = catColor.second
                                                ) {
                                                    Text(
                                                        text = catLabel,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = if (isSmallScreen) Modifier.weight(1f) else Modifier) {
                                                    Text("Terjual: $unitsSoldStr", fontSize = if (isSmallScreen) 10.sp else 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("Harga Jual: ${Formatters.rupiah(item.product.price)}", fontSize = if (isSmallScreen) 9.sp else 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    
                                                    val suffix = if (isOlahan) {
                                                        if (isExpanded) " (Sembunyikan Resep ▲)" else " (Lihat Resep ▼)"
                                                    } else ""
                                                    Text(
                                                        text = "HPP: ${Formatters.rupiah(item.product.costPrice)}$suffix",
                                                        fontSize = if (isSmallScreen) 9.sp else 10.sp,
                                                        fontWeight = if (isOlahan) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isOlahan) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (isSmallScreen) {
                                                    Spacer(Modifier.width(8.dp))
                                                }
                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    modifier = if (isSmallScreen) Modifier.wrapContentWidth() else Modifier
                                                ) {
                                                    Text("Omzet: ${Formatters.rupiah(item.revenue)}", fontSize = if (isSmallScreen) 10.sp else 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Profit: ${Formatters.rupiah(item.grossProfit)}", fontSize = if (isSmallScreen) 10.sp else 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                    Text("Margin %: ${String.format("%.1f%%", item.marginPercent)}", fontSize = if (isSmallScreen) 10.sp else 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF6A1B9A))
                                                }
                                            }

                                            if (isExpanded && isOlahan) {
                                                Spacer(Modifier.height(8.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text("Struktur Kontribusi HPP:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                        Spacer(Modifier.height(6.dp))
                                                        components.forEachIndexed { idx, comp ->
                                                            val costPerUnit = if (comp.yield > 0) comp.cost / comp.yield else comp.cost
                                                            val totalCostPrice = item.product.costPrice
                                                            val contributionPercent = if (totalCostPrice > 0) (costPerUnit / totalCostPrice) * 100.0 else 0.0

                                                            val catLabel = when (comp.category) {
                                                                "OVERHEAD" -> "Overhead"
                                                                "TENAGA_KERJA" -> "Jasa"
                                                                else -> "Bahan"
                                                            }
                                                            val barColor = when (comp.category) {
                                                                "OVERHEAD" -> Color(0xFFE65100)
                                                                "TENAGA_KERJA" -> Color(0xFF1A237E)
                                                                else -> Color(0xFF2E7D32)
                                                            }
                                                            val matchedProd = products.find { it.name.equals(comp.name, ignoreCase = true) }
                                                            val stockText = if (matchedProd != null) {
                                                                " | Stok aktif: ${matchedProd.stock} ${matchedProd.unit}"
                                                            } else ""

                                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = "${idx + 1}. ${comp.name} ($catLabel)$stockText",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                        modifier = Modifier.weight(1.8f)
                                                                    )
                                                                    Text(
                                                                        text = "Rp ${costPerUnit.toInt()} (${String.format("%.0f%%", contributionPercent)})",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.weight(1.2f),
                                                                        textAlign = TextAlign.End
                                                                    )
                                                                }
                                                                Spacer(Modifier.height(3.dp))
                                                                LinearProgressIndicator(
                                                                    progress = (contributionPercent.coerceIn(0.0, 100.0) / 100.0).toFloat(),
                                                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                                                    color = barColor,
                                                                    trackColor = Color.LightGray.copy(alpha = 0.3f)
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
                        }
                    }
                }
                "AGING_AR" -> {
                    // Tab Buku Piutang & Aging AR
                    val unpaidTx = remember(transactions) {
                        transactions.filter { it.paymentMethod == "HUTANG" && it.type != "EXPENSE" }
                    }
                    val unpaidTxFiltered = remember(unpaidTx, posMode) {
                        unpaidTx.filter { tx ->
                            when (posMode) {
                                "FNB" -> tx.receiptNumber.startsWith("FNB-")
                                "RENTAL" -> tx.receiptNumber.startsWith("RN-")
                                "LAUNDRY" -> tx.receiptNumber.startsWith("LD-")
                                else -> true
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buku Piutang (${unpaidTxFiltered.size} Piutang Outstanding)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (unpaidTxFiltered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada piutang outstanding.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val todayMs = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(unpaidTxFiltered, key = { it.id }) { tx ->
                                val txDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.date))
                                val dueDateStr = if (tx.deliveryDate != null) {
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.deliveryDate))
                                } else {
                                    "Tidak Diatur"
                                }

                                // Calculate aging category & colors (Yellow, Orange, Red)
                                val agingCat = if (tx.deliveryDate == null) {
                                    "Belum Jatuh Tempo"
                                } else {
                                    val diffMs = todayMs - tx.deliveryDate
                                    val diffDays = diffMs / (24 * 60 * 60 * 1000)
                                    when {
                                        diffDays <= 0 -> "Belum Jatuh Tempo"
                                        diffDays in 1..7 -> "Terlambat 1-7 Hari (Peringatan)"
                                        diffDays in 8..30 -> "Terlambat 8-30 Hari (Jatuh Tempo)"
                                        else -> "Terlambat > 30 Hari (Macet/Kritis)"
                                    }
                                }

                                val agingColor = when {
                                    agingCat == "Belum Jatuh Tempo" -> Color(0xFF2E7D32)
                                    agingCat.contains("1-7") -> Color(0xFFFBC02D) // Kuning
                                    agingCat.contains("8-30") -> Color(0xFFE65100) // Oranye
                                    else -> Color(0xFFC62828) // Merah
                                }

                                val matchedCustomer = customers.find { it.id == tx.customerId }
                                val phoneNum = matchedCustomer?.phone
                                val intentContext = LocalContext.current

                                val triggerWhatsApp: () -> Unit = {
                                    if (!phoneNum.isNullOrBlank()) {
                                        var formattedPhone = phoneNum.replace("+", "").replace("-", "").replace(" ", "")
                                        if (formattedPhone.startsWith("0")) {
                                            formattedPhone = "62" + formattedPhone.substring(1)
                                        }
                                        try {
                                            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=Halo%20Kak%20${tx.customerName ?: ""},%20mengingatkan%20terkait%20tagihan%20piutang%20struk%20${tx.receiptNumber}%20sebesar%20${Formatters.rupiah(tx.total)}.%20Terima%20kasih!"
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                            intentContext.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(intentContext, "Tidak dapat membuka WhatsApp!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Pelanggan: ${tx.customerName ?: "Umum"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                if (!phoneNum.isNullOrBlank()) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(50.dp),
                                                        color = Color(0xFFE8F5E9),
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clickable { triggerWhatsApp() }
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text("💬", fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text("Tgl Transaksi: $txDateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Tgl Jatuh Tempo: $dueDateStr", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = agingColor)
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = agingColor.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = agingCat,
                                                    color = agingColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = Formatters.rupiah(tx.total),
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Button(
                                                onClick = { txToSettle = tx },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Settle / Lunas", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        }
    }

    // Dialog: Transaction detail items
    if (selectedTxDetails != null) {
        val tx = selectedTxDetails!!
        val posType = when {
            tx.receiptNumber.startsWith("FNB-") -> "FnB"
            tx.receiptNumber.startsWith("RN-") -> "Rental"
            tx.receiptNumber.startsWith("LD-") -> "Laundry"
            else -> "POS"
        }
        AlertDialog(
            onDismissRequest = { selectedTxDetails = null },
            title = { Text("Detail Transaksi - ${tx.receiptNumber}") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    Text("Pelanggan: ${tx.customerName ?: "Umum"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Metode Pembayaran: ${tx.paymentMethod}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (posType == "Rental" && tx.queueNumber != null) {
                        Text("Durasi Sewa: ${tx.queueNumber} Hari", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selectedTxItems) { item ->
                            val prod = products.find { it.id == item.productId }
                            val isKg = posType == "Laundry" && prod?.unit == "Kg"
                            val quantityStr = if (isKg) {
                                "${item.quantity / 10.0} Kg"
                            } else {
                                "${item.quantity} ${prod?.unit ?: "pcs"}"
                            }

                            // Item COGS calculation
                            val effectiveCostPrice = if (item.costPrice > 0.0) item.costPrice else (prod?.costPrice ?: 0.0)
                            val itemCogs = when (posType) {
                                "FnB" -> item.quantity * effectiveCostPrice
                                "Rental" -> {
                                    val days = tx.queueNumber ?: 1
                                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                    val dailyCogs = (if (effectiveCostPrice > 1_000_000.0) effectiveCostPrice / 1825.0 else effectiveCostPrice) + (monthlyMaint / 30.0)
                                    dailyCogs * days
                                }
                                "Laundry" -> {
                                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                    val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                                    val baseCogs = qty * effectiveCostPrice
                                    val maintShare = qty * (monthlyMaint / 300.0)
                                    baseCogs + maintShare
                                }
                                else -> item.quantity * effectiveCostPrice
                            }
                            val priceTimesQty = if (isKg) {
                                (item.quantity / 10.0) * item.price
                            } else {
                                item.quantity * item.price
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    val displayName = item.productName.takeIf { it.isNotBlank() } ?: prod?.name ?: "Produk Terhapus"
                                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("$quantityStr x ${Formatters.rupiah(item.price)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                    Text(Formatters.rupiah(priceTimesQty), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("COGS: ${Formatters.rupiah(itemCogs)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 12.sp)
                        Text(Formatters.rupiah(tx.subtotal), fontSize = 12.sp)
                    }
                    if (tx.discountAmt > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Diskon", fontSize = 12.sp)
                            Text("-${Formatters.rupiah(tx.discountAmt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pembayaran", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(Formatters.rupiah(tx.total), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTxDetails = null }) { Text("Tutup") }
            }
        )
    }

    // Dialog Pelunasan Piutang
    if (txToSettle != null) {
        val tx = txToSettle!!
        AlertDialog(
            onDismissRequest = { txToSettle = null },
            title = { Text("Pelunasan Piutang - ${tx.receiptNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pilih metode pembayaran untuk pelunasan piutang senilai ${Formatters.rupiah(tx.total)} oleh ${tx.customerName ?: "Umum"}:", fontSize = 13.sp)
                    val methods = listOf("CASH" to "Tunai (Cash)", "QRIS" to "QRIS", "TRANSFER" to "Transfer Bank")
                    methods.forEach { (m, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.settleTransaction(context, tx, m)
                                    txToSettle = null
                                    Toast.makeText(context, "Piutang berhasil dilunasi via $m!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { txToSettle = null }) { Text("Batal") }
            }
        )
    }

    // Dialog: Catat Wastage
    if (showWastageDialog) {
        var wastageOutletIdState by remember { mutableStateOf<Long?>(selectedOutletId ?: availableOutlets.firstOrNull()?.id) }
        var wastageProductState by remember { mutableStateOf<ProductEntity?>(null) }
        var wastageQtyState by remember { mutableStateOf("") }
        var wastageReasonState by remember { mutableStateOf("") }
        var productSearchQuery by remember { mutableStateOf("") }
        var showProductDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showWastageDialog = false },
            title = { Text("Catat Wastage / Bahan Terbuang", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Outlet Selection
                    if (selectedOutletId == null && availableOutlets.size > 1) {
                        Text("Pilih Outlet:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        var outletExpanded by remember { mutableStateOf(false) }
                        val activeOutlet = availableOutlets.find { it.id == wastageOutletIdState }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { outletExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(activeOutlet?.name ?: "Pilih Outlet")
                            }
                            DropdownMenu(
                                expanded = outletExpanded,
                                onDismissRequest = { outletExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                availableOutlets.forEach { ot ->
                                    DropdownMenuItem(
                                        text = { Text(ot.name) },
                                        onClick = {
                                            wastageOutletIdState = ot.id
                                            wastageProductState = null
                                            productSearchQuery = ""
                                            outletExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Product Selection with Search
                    Text("Pilih Produk:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    val productsForWastage = remember(products, wastageOutletIdState) {
                        if (wastageOutletIdState != null) {
                            products.filter { it.outletId == wastageOutletIdState && !it.isDeleted }
                        } else {
                            products.filter { !it.isDeleted }
                        }
                    }
                    val filteredProductsForDropdown = remember(productsForWastage, productSearchQuery) {
                        productsForWastage.filter { it.name.contains(productSearchQuery, ignoreCase = true) }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (wastageProductState != null && !showProductDropdown) wastageProductState!!.name else productSearchQuery,
                            onValueChange = {
                                productSearchQuery = it
                                showProductDropdown = true
                                if (wastageProductState?.name != it) {
                                    wastageProductState = null
                                }
                            },
                            placeholder = { Text("Ketik nama produk...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                IconButton(onClick = { showProductDropdown = !showProductDropdown }) {
                                    Text("▼", fontSize = 10.sp)
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        if (showProductDropdown && filteredProductsForDropdown.isNotEmpty()) {
                            DropdownMenu(
                                expanded = showProductDropdown,
                                onDismissRequest = { showProductDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 200.dp)
                            ) {
                                filteredProductsForDropdown.forEach { prod ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Stok: ${prod.stock} ${prod.unit} | HPP: ${Formatters.rupiah(prod.costPrice)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            wastageProductState = prod
                                            productSearchQuery = prod.name
                                            showProductDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Selected product info
                    if (wastageProductState != null) {
                        val prod = wastageProductState!!
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Unit: ${prod.unit}", fontSize = 11.sp)
                                Text("Stok Sekarang: ${prod.stock} ${prod.unit}", fontSize = 11.sp)
                                Text("HPP (Cost Price): ${Formatters.rupiah(prod.costPrice)}", fontSize = 11.sp)
                            }
                        }
                    }

                    // 3. Qty Input
                    OutlinedTextField(
                        value = wastageQtyState,
                        onValueChange = { wastageQtyState = it.filter { char -> char.isDigit() } },
                        label = { Text("Jumlah Terbuang (Qty)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    // 4. Reason Input
                    OutlinedTextField(
                        value = wastageReasonState,
                        onValueChange = { wastageReasonState = it },
                        label = { Text("Alasan / Catatan", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    // 5. Loss Calculation
                    val qty = wastageQtyState.toIntOrNull() ?: 0
                    val costPrice = wastageProductState?.costPrice ?: 0.0
                    val estimatedLoss = qty * costPrice
                    if (estimatedLoss > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimasi Kerugian:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(Formatters.rupiah(estimatedLoss), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                val qty = wastageQtyState.toIntOrNull() ?: 0
                val prod = wastageProductState
                val outletId = wastageOutletIdState
                Button(
                    onClick = {
                        if (prod == null) {
                            Toast.makeText(context, "Silakan pilih produk!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (qty <= 0) {
                            Toast.makeText(context, "Jumlah harus lebih besar dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (outletId == null) {
                            Toast.makeText(context, "Outlet tidak valid!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addWastage(
                            context = context,
                            product = prod,
                            quantity = qty,
                            reason = wastageReasonState.ifBlank { "Wastage / Spoilage tanpa alasan spesifik" },
                            outletId = outletId
                        ) {
                            showWastageDialog = false
                            wastageQtyState = ""
                            wastageReasonState = ""
                            wastageProductState = null
                            productSearchQuery = ""
                            Toast.makeText(context, "Wastage berhasil dicatat & disinkronkan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Catat & Potong Stok")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWastageDialog = false }) { Text("Batal") }
            }
        )
    }
}

private data class MarginHppComponent(
    val name: String,
    val cost: Double,
    val yield: Double,
    val category: String
)

private fun parseHppBreakdown(json: String?): List<MarginHppComponent> {
    if (json.isNullOrBlank()) return emptyList()
    val list = mutableListOf<MarginHppComponent>()
    try {
        val arr = org.json.JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.getString("name")
            val cost = obj.optDouble("cost", 0.0)
            val yield = obj.optDouble("yield", 1.0)
            val category = obj.optString("category", "BAHAN_BAKU")
            list.add(MarginHppComponent(name, cost, yield, category))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
