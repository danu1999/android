package com.posbah.app.ui.screens.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.TenantRepository
import com.posbah.app.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.posbah.app.data.repository.SessionState

data class TenantPickerUiState(
    val isLoading: Boolean = true,
    val tenants: List<Tenant> = emptyList(),
    val selectedId: String? = null,
    val showCreateDialog: Boolean = false,
    val newTenantName: String = "",
    val canAddTenant: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TenantPickerViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val authRepository: AuthRepository,
    private val securePrefs: SecurePreferences,
    private val sessionState: SessionState,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _ui = MutableStateFlow(TenantPickerUiState())
    val ui = _ui.asStateFlow()

    init { load() }

    private fun load() {
        val email = securePrefs.currentEmail ?: return
        val role = securePrefs.currentRole ?: "OWNER"
        val tenantId = securePrefs.currentTenantId
        val lowerEmail = email.lowercase().trim()

        viewModelScope.launch {
            if (role == "OWNER") {
                // Fetch tenant list dari VPS — convert TenantSession → Tenant
                val sessions = authRepository.fetchOwnerTenants(email)
                val list = sessions.map { ts ->
                    Tenant(
                        id = ts.id,
                        name = ts.name,
                        ownerEmail = ts.ownerEmail,
                        businessMode = ts.businessMode
                    )
                }
                _ui.update {
                    it.copy(
                        isLoading = false,
                        tenants = list,
                        canAddTenant = (lowerEmail == "muhammadmuizz8@gmail.com" || lowerEmail == "mulyakus84@gmail.com")
                    )
                }
            } else {
                // Karyawan — lock ke tenant aktif saja
                val tenant = if (!tenantId.isNullOrBlank()) {
                    Tenant(
                        id = tenantId,
                        name = securePrefs.currentTenantName ?: tenantId,
                        ownerEmail = email,
                        businessMode = securePrefs.currentBusinessMode ?: "FNB"
                    )
                } else null
                val tenantList = if (tenant != null) listOf(tenant) else emptyList()
                _ui.update {
                    it.copy(
                        isLoading = false,
                        tenants = tenantList,
                        canAddTenant = false
                    )
                }
            }
        }
    }

    fun toggleCreate(open: Boolean) =
        _ui.update { it.copy(showCreateDialog = open, newTenantName = "") }
    fun updateNewName(name: String) = _ui.update { it.copy(newTenantName = name) }

    fun createTenant(businessMode: String) {
        val email = securePrefs.currentEmail ?: return
        val name = _ui.value.newTenantName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            authRepository.createTenant(email, name, businessMode)
            _ui.update { it.copy(showCreateDialog = false, newTenantName = "") }
            load() // refresh list
        }
    }

    fun selectTenant(tenantId: String, onDone: () -> Unit) {
        val sub = securePrefs.currentGoogleSub ?: return
        viewModelScope.launch {
            val ok = authRepository.selectTenant(sub, tenantId)
            if (ok) {
                // Load outlet untuk tenant ini
                val outlets = authRepository.fetchOutletsForTenant(tenantId)
                val activeOutlet = outlets.firstOrNull { it.isDefault } ?: outlets.firstOrNull()
                sessionState.setOutlet(activeOutlet?.id)
                _ui.update { it.copy(selectedId = tenantId) }
                onDone()
            } else {
                _ui.update { it.copy(error = "Gagal memilih tenant") }
            }
        }
    }

    fun getActiveUserEmail(): String? = securePrefs.currentEmail

    fun changePassword(oldPin: String, newPin: String, onResult: (AuthRepository.ChangePasswordResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.changePassword(oldPin, newPin)
            onResult(result)
        }
    }
}
