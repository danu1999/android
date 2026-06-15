package com.posbah.app.ui.screens.bmp.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpBahanBakuRepository
import com.posbah.app.data.repository.BmpCashFlowRepository
import com.posbah.app.data.repository.BmpClientRepository
import com.posbah.app.data.repository.BmpInvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    val isLoading: Boolean = true
)

@HiltViewModel
class BmpDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val clientRepo: BmpClientRepository,
    private val invoiceRepo: BmpInvoiceRepository,
    private val cashFlowRepo: BmpCashFlowRepository,
    private val bahanBakuRepo: BmpBahanBakuRepository,
    private val localDataSeeder: com.posbah.app.data.local.LocalDataSeeder,
    private val db: com.posbah.app.data.local.PosBahDatabase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _ui = MutableStateFlow(BmpDashboardUiState())
    val ui = _ui.asStateFlow()

    init {
        val tenantId = authRepository.activeTenantId()
        val userEmail = authRepository.activeUserEmail()
        if (tenantId != null) {
            _ui.value = _ui.value.copy(tenantId = tenantId, ownerEmail = userEmail, isLoading = false)
            viewModelScope.launch {
                try {
                    val t = db.tenantDao().getById(tenantId)
                    if (t != null) {
                        _ui.value = _ui.value.copy(tenantName = t.name)
                    }
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
