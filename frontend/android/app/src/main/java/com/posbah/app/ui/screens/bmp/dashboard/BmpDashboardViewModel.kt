package com.posbah.app.ui.screens.bmp.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpCashFlowRepository
import com.posbah.app.data.repository.BmpClientRepository
import com.posbah.app.data.repository.BmpInvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val isLoading: Boolean = true
)

@HiltViewModel
class BmpDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val clientRepo: BmpClientRepository,
    private val invoiceRepo: BmpInvoiceRepository,
    private val cashFlowRepo: BmpCashFlowRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(BmpDashboardUiState())
    val ui = _ui.asStateFlow()

    init {
        val tenantId = authRepository.activeTenantId()
        if (tenantId != null) {
            _ui.value = _ui.value.copy(tenantId = tenantId, isLoading = false)
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
                    }
            }
            viewModelScope.launch {
                combine(
                    cashFlowRepo.totalIn(tenantId),
                    cashFlowRepo.totalOut(tenantId)
                ) { a, b -> a to b }.collect { (i, o) ->
                    _ui.value = _ui.value.copy(totalIn = i, totalOut = o)
                }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
