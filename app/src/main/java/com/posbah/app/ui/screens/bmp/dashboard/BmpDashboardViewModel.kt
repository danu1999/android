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
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _ui = MutableStateFlow(BmpDashboardUiState())
    val ui = _ui.asStateFlow()

    init {
        val tenantId = authRepository.activeTenantId()
        if (tenantId != null) {
            _ui.value = _ui.value.copy(tenantId = tenantId, isLoading = false)
            viewModelScope.launch {
                try {
                    val list = bahanBakuRepo.observe(tenantId).first()
                    if (list.isEmpty() && (tenantId == "bahteramulyap@gmail.com" || tenantId == "demo_tenant")) {
                        localDataSeeder.seedFromSqlDump(context, tenantId, null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            viewModelScope.launch {
                combine(
                    clientRepo.count(tenantId),
                    invoiceRepo.count(tenantId),
                    invoiceRepo.totalAmount(tenantId),
                    invoiceRepo.totalOutstanding(tenantId),
                ) { cc, ic, total, out -> listOf(cc, ic, total, out) }
                    .collect { values ->
                        _ui.value = _ui.value.copy(
                            clientCount = values[0] as Int,
                            invoiceCount = values[1] as Int,
                            totalAmount = values[2] as Double,
                            totalOutstanding = values[3] as Double
                        )
                        recalcSaldo()
                    }
            }
            viewModelScope.launch {
                combine(
                    cashFlowRepo.totalIn(tenantId),
                    cashFlowRepo.totalOut(tenantId)
                ) { a, b -> a to b }.collect { (i, o) ->
                    _ui.value = _ui.value.copy(totalIn = i, totalOut = o)
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
