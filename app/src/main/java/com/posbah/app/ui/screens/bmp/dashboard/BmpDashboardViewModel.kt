package com.posbah.app.ui.screens.bmp.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpBahanBakuRepository
import com.posbah.app.data.repository.BmpCashFlowRepository
import com.posbah.app.data.repository.BmpClientRepository
import com.posbah.app.data.repository.BmpInvoiceRepository
import com.posbah.app.data.repository.BmpMasterProductRepository
import com.posbah.app.data.repository.BmpStockRepository
import com.posbah.app.data.repository.BmpProductionLogRepository
import com.posbah.app.data.repository.BmpSettingsRepository
import com.posbah.app.data.remote.api.BmpApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DashboardKpiData(
    val overdueCount: Int,
    val totalStockValue: Double,
    val productionThisMonth: Int
)

data class BmpCashFlowDataPoint(
    val dateLabel: String,
    val inAmount: Double,
    val outAmount: Double
)

data class BmpDashboardUiState(
    val tenantId: String? = null,
    val tenantName: String? = null,
    val ownerEmail: String? = null,
    val clientCount: Int = 0,
    val invoiceCount: Int = 0,
    val totalAmount: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
    val nonoTotalHarga: Double = 0.0,    // Total nilai bahan baku masuk (termasuk hutang)
    val nonoTotalNominal: Double = 0.0,  // Total kas yang dibayarkan ke supplier
    val saldoKasRiil: Double = 0.0,      // totalIn - totalOut - nonoTotalNominal
    val simulasiSaldo: Double = 0.0,     // totalAmount - nonoTotalHarga - totalOut
    val cashFlowHistory: List<BmpCashFlowDataPoint> = emptyList(),
    val kpiState: UiState<DashboardKpiData> = UiState.Loading,
    val isLoading: Boolean = true
)

@HiltViewModel
class BmpDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val clientRepo: BmpClientRepository,
    private val invoiceRepo: BmpInvoiceRepository,
    private val cashFlowRepo: BmpCashFlowRepository,
    private val bahanBakuRepo: BmpBahanBakuRepository,
    private val productRepo: BmpMasterProductRepository,
    private val stockRepo: BmpStockRepository,
    private val productionLogRepo: BmpProductionLogRepository,
    private val settingsRepo: BmpSettingsRepository,
    private val apiService: BmpApiService,
    private val localDataSeeder: com.posbah.app.data.local.LocalDataSeeder,
    private val db: com.posbah.app.data.local.PosBahDatabase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _ui = MutableStateFlow(BmpDashboardUiState())
    val ui = _ui.asStateFlow()

    private val _kpiState = MutableStateFlow<UiState<DashboardKpiData>>(UiState.Loading)
    val kpiState = _kpiState.asStateFlow()

    init {
        val tenantId = authRepository.activeTenantId()
        val userEmail = authRepository.activeUserEmail()
        if (tenantId != null) {
            _ui.value = _ui.value.copy(tenantId = tenantId, ownerEmail = userEmail, isLoading = false)
            
            viewModelScope.launch {
                try {
                    _kpiState.value = UiState.Loading
                    
                    // 1. Ambil seluruh invoice "UNPAID" via API, filter yang melewati dueDate, lalu lakukan hit update status
                    val now = System.currentTimeMillis()
                    val invoices = invoiceRepo.list()
                    val unpaidInvoices = invoices.filter {
                        (it.status == "UNPAID" || it.status == "PARTIAL") &&
                        it.dueDate != null && it.dueDate < now
                    }
                    for (inv in unpaidInvoices) {
                        try {
                            apiService.updateInvoice(inv.id, mapOf("status" to "OVERDUE"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // Refresh data terbaru dari VPS
                    invoiceRepo.refresh()
                    productRepo.refresh()
                    productionLogRepo.loadAll(tenantId)
                    
                    val updatedInvoices = invoiceRepo.list()
                    val overdueCount = updatedInvoices.count { it.status == "OVERDUE" }
                    
                    val products = productRepo.list()
                    val stocksList = try {
                        apiService.getProductStocks().body() ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                    val s = try {
                        settingsRepo.get()
                    } catch (_: Exception) {
                        null
                    }
                    val rates = try {
                        val materials = db.bmpBahanBakuItemDao().getDistinctBahanBaku(tenantId)
                        materials.associateWith { db.bmpBahanBakuItemDao().getLatestRate(tenantId, it) ?: 0.0 }
                    } catch (_: Exception) {
                        emptyMap<String, Double>()
                    }
                    var totalStockValue = 0.0
                    if (s != null) {
                        val totalGaji = s.jumlahKaryawan * s.gajiHarian * s.hariKerjaSebulan
                        val overheadBulanan = s.listrikBulanan + totalGaji
                        val totalDetikSebulan = s.jumlahMesin * s.hariKerjaSebulan * s.hoursPerDay * 3600.0
                        val biayaPerDetik = if (totalDetikSebulan > 0) overheadBulanan / totalDetikSebulan else 0.0
                        val biayaKemasanPcs = s.biayaKarungPer1000 / 1000.0
                        for (stockMap in stocksList) {
                            val masterProductId = (stockMap["masterItemId"] as? Number)?.toLong() ?: 0L
                            val quantity = (stockMap["currentStock"] as? Number)?.toDouble() ?: 0.0
                            val product = products.find { it.id == masterProductId }
                            if (product != null && quantity > 0) {
                                val biayaMesin = product.cycleTime * biayaPerDetik
                                val bahanRate = if (product.jenisBahanBaku.isNotEmpty()) {
                                    rates[product.jenisBahanBaku] ?: product.price
                                } else {
                                    product.price
                                }
                                val hppSatuan = (product.beratGram * (bahanRate / 1000.0) + biayaMesin) * (1.0 + (product.rejectRate / 100.0))
                                val hppTotalPcs = hppSatuan + biayaKemasanPcs
                                totalStockValue += quantity * hppTotalPcs
                            }
                        }
                    }
                    
                    // Hitung produksi bulan ini
                    val cal = java.util.Calendar.getInstance()
                    val currentYear = cal.get(java.util.Calendar.YEAR)
                    val currentMonth = cal.get(java.util.Calendar.MONTH)
                    val startOfMonthCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.YEAR, currentYear)
                        set(java.util.Calendar.MONTH, currentMonth)
                        set(java.util.Calendar.DAY_OF_MONTH, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val startOfMonth = startOfMonthCal.timeInMillis
                    
                    val logs = productionLogRepo.observeAll(tenantId).first()
                    val productionThisMonth = logs.filter { it.productionDate >= startOfMonth }
                        .sumOf { it.quantityProduced }.toInt()
                    
                    val kpiData = DashboardKpiData(
                        overdueCount = overdueCount,
                        totalStockValue = totalStockValue,
                        productionThisMonth = productionThisMonth
                    )
                    
                    _kpiState.value = UiState.Success(kpiData)
                    _ui.value = _ui.value.copy(kpiState = UiState.Success(kpiData))
                } catch (e: Exception) {
                    _kpiState.value = UiState.Error(e.message ?: "Gagal menghitung KPI")
                    _ui.value = _ui.value.copy(kpiState = UiState.Error(e.message ?: "Gagal menghitung KPI"))
                }
            }
            viewModelScope.launch {
                try {
                    // Ambil tenant name dari active user (tidak perlu hit tenantDao stub)
                    val user = authRepository.getActiveUser()
                    val name = user?.displayName ?: userEmail ?: tenantId
                    _ui.value = _ui.value.copy(tenantName = name)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            viewModelScope.launch {
                try {
                    val list = bahanBakuRepo.observe(tenantId).first()
                    if (list.isEmpty() && (tenantId == "bahteramulyap@gmail.com" || tenantId == "ten_premium_bahteramulyap_gmail_com" || tenantId == "demo_tenant")) {
                        localDataSeeder.seedFromSqlDump(context, tenantId, null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            viewModelScope.launch {
                try {
                    invoiceRepo.refresh()
                    clientRepo.refresh()
                    cashFlowRepo.refresh()
                    bahanBakuRepo.refresh()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            viewModelScope.launch {
                combine(
                    clientRepo.count(tenantId),
                    invoiceRepo.observe(tenantId)
                ) { cc, invoices ->
                    val nonDraftInvoices = invoices.filter { it.status != "DRAFT" }
                    val total = if (userEmail == "bahteramulyap@gmail.com") {
                        nonDraftInvoices.sumOf { it.totalAmount }
                    } else {
                        invoices.sumOf { it.totalAmount }
                    }
                    val out = if (userEmail == "bahteramulyap@gmail.com") {
                        nonDraftInvoices.filter { it.status != "PAID" }.sumOf { it.totalAmount - it.paidAmount }
                    } else {
                        invoices.filter { it.status != "PAID" }.sumOf { it.totalAmount - it.paidAmount }
                    }
                    val invCount = if (userEmail == "bahteramulyap@gmail.com") {
                        nonDraftInvoices.size
                    } else {
                        invoices.size
                    }
                    Triple(cc, invCount, total to out)
                }.collect { (cc, invCount, totals) ->
                    _ui.value = _ui.value.copy(
                        clientCount = cc,
                        invoiceCount = invCount,
                        totalAmount = totals.first,
                        totalOutstanding = totals.second
                    )
                    recalcSaldo()
                }
            }
            viewModelScope.launch {
                cashFlowRepo.observe(tenantId).collect { cashflows ->
                    val totalIn = cashflows.filter { it.transactionType == "MASUK" }.sumOf { it.amount }
                    val totalOut = if (userEmail == "bahteramulyap@gmail.com") {
                        cashflows.filter { 
                            it.transactionType == "KELUAR" && 
                            !it.description.contains("Nono", ignoreCase = true) 
                        }.sumOf { it.amount }
                    } else {
                        cashflows.filter { it.transactionType == "KELUAR" }.sumOf { it.amount }
                    }
                    _ui.value = _ui.value.copy(totalIn = totalIn, totalOut = totalOut)
                    recalcSaldo()
                }
            }
            viewModelScope.launch {
                combine(
                    bahanBakuRepo.totalHarga(tenantId),
                    bahanBakuRepo.totalNominal(tenantId)
                ) { h, n -> h to n }.collect { (h, n) ->
                    _ui.value = _ui.value.copy(nonoTotalHarga = h, nonoTotalNominal = n)
                    recalcSaldo()
                }
            }
            viewModelScope.launch {
                cashFlowRepo.observe(tenantId).collect { cashflows ->
                    val sdfKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val sdfLabel = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                    val last7Days = (0..6).map { offset ->
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
                        cal.time
                    }.reversed()
                    
                    val grouped = cashflows.groupBy { sdfKey.format(java.util.Date(it.transactionDate)) }
                    val points = last7Days.map { date ->
                        val key = sdfKey.format(date)
                        val label = sdfLabel.format(date)
                        val entries = grouped[key] ?: emptyList()
                        val inAmt = entries.filter { it.transactionType == "MASUK" }.sumOf { it.amount }
                        val outAmt = entries.filter { it.transactionType == "KELUAR" }.sumOf { it.amount }
                        BmpCashFlowDataPoint(
                            dateLabel = label,
                            inAmount = inAmt,
                            outAmount = outAmt
                        )
                    }
                    _ui.value = _ui.value.copy(cashFlowHistory = points)
                }
            }
        }
    }

    private fun recalcSaldo() {
        val s = _ui.value
        _ui.value = s.copy(
            saldoKasRiil = s.totalIn - s.totalOut - s.nonoTotalNominal,
            simulasiSaldo = s.totalAmount - s.nonoTotalHarga - s.totalOut
        )
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    fun upgradeToPremium(
        email: String,
        password: String,
        businessName: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            authRepository.simulatePaymentAndUpgrade(email, password, businessName)
            onDone()
        }
    }
}
