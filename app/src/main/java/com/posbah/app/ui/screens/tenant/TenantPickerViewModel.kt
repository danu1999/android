package com.posbah.app.ui.screens.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.TenantRepository
import com.posbah.app.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val securePrefs: SecurePreferences
) : ViewModel() {

    private val _ui = MutableStateFlow(TenantPickerUiState())
    val ui = _ui.asStateFlow()

    init { load() }

    private fun load() {
        val email = securePrefs.currentEmail ?: return
        viewModelScope.launch {
            tenantRepository.observeForOwner(email).collect { list ->
                _ui.update { 
                    it.copy(
                        isLoading = false, 
                        tenants = list,
                        canAddTenant = email == "muhammadmuizz8@gmail.com"
                    ) 
                }
            }
        }
    }

    fun toggleCreate(open: Boolean) =
        _ui.update { it.copy(showCreateDialog = open, newTenantName = "") }
    fun updateNewName(name: String) = _ui.update { it.copy(newTenantName = name) }

    fun createTenant() {
        val email = securePrefs.currentEmail ?: return
        val name = _ui.value.newTenantName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            tenantRepository.create(email, name)
            _ui.update { it.copy(showCreateDialog = false, newTenantName = "") }
        }
    }

    fun selectTenant(tenantId: String, onDone: () -> Unit) {
        val sub = securePrefs.currentGoogleSub ?: return
        viewModelScope.launch {
            val ok = authRepository.selectTenant(sub, tenantId)
            if (ok) {
                _ui.update { it.copy(selectedId = tenantId) }
                onDone()
            } else {
                _ui.update { it.copy(error = "Gagal memilih tenant") }
            }
        }
    }
}
