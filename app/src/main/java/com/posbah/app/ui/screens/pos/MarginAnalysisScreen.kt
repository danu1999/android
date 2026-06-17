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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.History
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
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MarginAnalysisViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val sessionState: com.posbah.app.data.repository.SessionState,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    val tenantId = authRepository.activeTenantId().orEmpty()

    /** Outlet yang tersedia untuk filter (Owner melihat semua). */
    val availableOutlets = db.outletDao().observeForTenant(tenantId)
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

    val userRole = flow {
        emit(authRepository.getActiveUser()?.role ?: "KASIR")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "KASIR")

    private val _transactionItems = MutableStateFlow<List<TransactionItemEntity>>(emptyList())
    val transactionItems = _transactionItems.asStateFlow()

    init {
        // For non-OWNER: lock filter to their outlet
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            if (user?.role != "OWNER") {
                val lockedOutlet = sessionState.lockedEmployeeOutletId.value
                _selectedOutletId.value = lockedOutlet
            }
        }
        refreshItems()
        // Auto-refresh when transactions update
        viewModelScope.launch {
            transactions.collect {
                refreshItems()
            }
        }
        // Trigger background pull sync
        viewModelScope.launch(Dispatchers.IO) {
            com.posbah.app.data.remote.SupabaseSyncManager.pullAll(context, db, tenantId)
        }
    }

    fun refreshItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _transactionItems.value = db.transactionItemDao().getAll()
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
            viewModelScope.launch(Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context.applicationContext, db, tenantId)
            }
        }
    }
}

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

    // Filters state
    var posMode by remember { mutableStateOf("SEMUA") } // SEMUA, FNB, RENTAL, LAUNDRY
    var datePreset by remember { mutableStateOf("HARI_INI") } // HARI_INI, 7_HARI, 30_HARI, KUSTOM
    var customerType by remember { mutableStateOf("SEMUA") } // SEMUA, PELANGGAN, UMUM
    var activeTab by remember { mutableStateOf("HISTORY") } // "HISTORY" or "AGING_AR"
    var txToSettle by remember { mutableStateOf<TransactionEntity?>(null) }

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
            when {
                tx.receiptNumber.startsWith("FNB-") -> {
                    item.quantity * item.costPrice
                }
                tx.receiptNumber.startsWith("RN-") -> {
                    val days = tx.queueNumber ?: 1
                    val prod = products.find { it.id == item.productId }
                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                    val dailyCogs = (if (item.costPrice > 1_000_000.0) item.costPrice / 1825.0 else item.costPrice) + (monthlyMaint / 30.0)
                    dailyCogs * days
                }
                tx.receiptNumber.startsWith("LD-") -> {
                    val prod = products.find { it.id == item.productId }
                    val isKg = prod?.unit == "Kg"
                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                    val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                    val baseCogs = qty * item.costPrice
                    val maintShare = qty * (monthlyMaint / 300.0)
                    baseCogs + maintShare
                }
                else -> item.quantity * item.costPrice
            }
        }
    }
    val totalExpenses = expenseTx.sumOf { Math.abs(it.total) }
    val grossProfit = totalRevenue - totalCogs
    val netProfit = grossProfit - totalExpenses
    val marginPercent = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100.0 else 0.0
    val netMarginPercent = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0

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
            // Row 1: Filters bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // POS Mode Tabs
                Card(
                    modifier = Modifier.weight(1.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Modul Kasir", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
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
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Date Presets
                Card(
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Rentang Waktu", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
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
                }
            }

            // Custom Date Range Pickers (only visible if preset is KUSTOM)
            if (datePreset == "KUSTOM") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pilih Tanggal:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        // Start Date Button
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
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mulai: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(startDateMillis))}", fontSize = 11.sp)
                        }

                        // End Date Button
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
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hingga: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(endDateMillis))}", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Customer Type Filter Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Filter Pelanggan:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val types = listOf("SEMUA" to "Semua Transaksi", "PELANGGAN" to "Pelanggan Terdaftar", "UMUM" to "Umum (Walk-in)")
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
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Outlet Filter Row (hanya tampil untuk OWNER dengan 2+ outlet, atau info outlet untuk karyawan)
            if (userRole == "OWNER" && availableOutlets.size > 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Filter Outlet:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // "Semua Outlet" chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedOutletId == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedOutletId == null) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { viewModel.selectOutletFilter(null) }
                            ) {
                                Text("Semua", fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                            }
                            availableOutlets.forEach { outlet ->
                                val isActive = selectedOutletId == outlet.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isActive) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.clickable { viewModel.selectOutletFilter(outlet.id) }
                                ) {
                                    Text(outlet.name, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                                }
                            }
                        }
                    }
                }
            } else if (userRole != "OWNER") {
                // Karyawan: tampilkan outlet mereka sebagai info (read-only)
                val myOutlet = availableOutlets.firstOrNull { it.id == selectedOutletId }
                if (myOutlet != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "📍 Outlet: ${myOutlet.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Premium Dashboard Summary Cards (Revenue, COGS, Profit, Expenses, Net Profit, Margins)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Revenue Card (Green)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.15f)),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFE8F5E9), Color.White)
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("Pendapatan Kotor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                Spacer(Modifier.height(4.dp))
                                Text(Formatters.rupiah(totalRevenue), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                            }
                        }
                    }

                    // COGS Card (Amber)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFD84315).copy(alpha = 0.15f)),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFBE9E7), Color.White)
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("Total HPP (COGS)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD84315))
                                Spacer(Modifier.height(4.dp))
                                Text(Formatters.rupiah(totalCogs), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFBF360C))
                            }
                        }
                    }

                    // Profit Card (Indigo)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF283593).copy(alpha = 0.15f)),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFE8EAF6), Color.White)
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("Laba Kotor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF283593))
                                Spacer(Modifier.height(4.dp))
                                Text(Formatters.rupiah(grossProfit), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A237E))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Expenses Card (Red)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.15f)),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFEBEE), Color.White)
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("Biaya Usaha", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                                Spacer(Modifier.height(4.dp))
                                Text(Formatters.rupiah(totalExpenses), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFB71C1C))
                            }
                        }
                    }

                    // Net Profit Card (Teal)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF00796B).copy(alpha = 0.15f)),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFE0F2F1), Color.White)
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("Laba Bersih", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00796B))
                                Spacer(Modifier.height(4.dp))
                                Text(Formatters.rupiah(netProfit), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF004D40))
                            }
                        }
                    }

                    // Margin Percentage Card (Purple)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF6A1B9A).copy(alpha = 0.15f)),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFF3E5F5), Color.White)
                                    )
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Margin Laba %", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6A1B9A))
                                Spacer(Modifier.height(2.dp))
                                Text("Kotor: ${String.format("%.1f%%", marginPercent)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                                Text("Bersih: ${String.format("%.1f%%", netMarginPercent)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                            }
                        }
                    }
                }
            }

            // Tab Toggle Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { activeTab = "HISTORY" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == "HISTORY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (activeTab == "HISTORY") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.History, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Riwayat Margin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { activeTab = "AGING_AR" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == "AGING_AR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (activeTab == "AGING_AR") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.People, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buku Piutang & Aging AR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeTab == "HISTORY") {
                // Transaction History Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Transaksi Terfilter (${filteredTx.size} Transaksi)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // LazyColumn of Transactions
                if (filteredTx.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada data transaksi untuk filter ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTx, key = { it.id }) { tx ->
                            val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(tx.date))
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

                            val isExpense = tx.type == "EXPENSE"

                            // Calculate HPP/COGS for this transaction
                            val txItems = transactionItems.filter { it.transactionId == tx.id }
                            val txCogs = if (isExpense) 0.0 else txItems.sumOf { item ->
                                when {
                                    tx.receiptNumber.startsWith("FNB-") -> {
                                        item.quantity * item.costPrice
                                    }
                                    tx.receiptNumber.startsWith("RN-") -> {
                                        val days = tx.queueNumber ?: 1
                                        val prod = products.find { it.id == item.productId }
                                        val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                        val dailyCogs = (if (item.costPrice > 1_000_000.0) item.costPrice / 1825.0 else item.costPrice) + (monthlyMaint / 30.0)
                                        dailyCogs * days
                                    }
                                    tx.receiptNumber.startsWith("LD-") -> {
                                        val prod = products.find { it.id == item.productId }
                                        val isKg = prod?.unit == "Kg"
                                        val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                        val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                                        val baseCogs = qty * item.costPrice
                                        val maintShare = qty * (monthlyMaint / 300.0)
                                        baseCogs + maintShare
                                    }
                                    else -> item.quantity * item.costPrice
                                }
                            }
                            val txMargin = if (isExpense) 0.0 else tx.total - txCogs
                            val txMarginPercent = if (!isExpense && tx.total > 0) (txMargin / tx.total) * 100.0 else 0.0

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTxDetails = tx
                                        selectedTxItems = txItems
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(tx.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Spacer(Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = posColor.first,
                                                    contentColor = posColor.second
                                                ) {
                                                    Text(
                                                        text = if (isExpense) "$posType (Biaya)" else posType,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(if (isExpense) "Keterangan: ${tx.notes ?: "-"}" else "Pelanggan: ${tx.customerName ?: "Umum"}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = Formatters.rupiah(tx.total),
                                                fontWeight = FontWeight.Black,
                                                color = if (isExpense) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                                fontSize = 13.sp
                                            )
                                            if (isExpense) {
                                                Text(
                                                    text = "Biaya Usaha",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828)
                                                )
                                            } else {
                                                Text(
                                                    text = "Profit: ${Formatters.rupiah(txMargin)} (${String.format("%.0f%%", txMarginPercent)})",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (txMargin >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
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

                            // Calculate aging category
                            val agingCat = if (tx.deliveryDate == null) {
                                "Belum Jatuh Tempo"
                            } else {
                                val diffMs = todayMs - tx.deliveryDate
                                val diffDays = diffMs / (24 * 60 * 60 * 1000)
                                when {
                                    diffDays <= 0 -> "Belum Jatuh Tempo"
                                    diffDays in 1..30 -> "Terlambat 1-30 Hari"
                                    diffDays in 31..60 -> "Terlambat 31-60 Hari"
                                    else -> "Terlambat > 60 Hari"
                                }
                            }

                            val agingColor = when (agingCat) {
                                "Belum Jatuh Tempo" -> Color(0xFF2E7D32)
                                "Terlambat 1-30 Hari" -> Color(0xFFE65100)
                                "Terlambat 31-60 Hari" -> Color(0xFFD84315)
                                else -> Color(0xFFC62828)
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
                                        Text("Pelanggan: ${tx.customerName ?: "Umum"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            val itemCogs = when (posType) {
                                "FnB" -> item.quantity * item.costPrice
                                "Rental" -> {
                                    val days = tx.queueNumber ?: 1
                                    val prod = products.find { it.id == item.productId }
                                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                    val dailyCogs = (if (item.costPrice > 1_000_000.0) item.costPrice / 1825.0 else item.costPrice) + (monthlyMaint / 30.0)
                                    dailyCogs * days
                                }
                                "Laundry" -> {
                                    val prod = products.find { it.id == item.productId }
                                    val monthlyMaint = getMonthlyMaintenance(prod?.wholesalePrices)
                                    val qty = if (isKg) item.quantity / 10.0 else item.quantity.toDouble()
                                    val baseCogs = qty * item.costPrice
                                    val maintShare = qty * (monthlyMaint / 300.0)
                                    baseCogs + maintShare
                                }
                                else -> item.quantity * item.costPrice
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
                                    Text(prod?.name ?: "Produk Terhapus", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
}
