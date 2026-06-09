package com.posbah.app.ui.screens.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val mode: LoginMode = LoginMode.Google,
    val email: String = "",
    val pin: String = "",
    val pinTenantId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedInUser: LocalUser? = null,
    val needsTenantPicker: Pair<LocalUser, List<Tenant>>? = null,
    val locked: Boolean = false
)

enum class LoginMode { Google, Pin }

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun switchMode(mode: LoginMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun updateEmail(e: String) = _uiState.update { it.copy(email = e) }
    fun updatePin(p: String) = _uiState.update { it.copy(pin = p.take(64)) }
    fun updatePinTenantId(t: String) = _uiState.update { it.copy(pinTenantId = t) }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun signInWithGoogle(activity: Activity, simulatedEmail: String? = null, simulatedName: String? = null) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val outcome = authRepository.loginWithGoogle(activity, simulatedEmail, simulatedName)) {
                is AuthRepository.LoginOutcome.Success ->
                    _uiState.update { it.copy(isLoading = false, signedInUser = outcome.user) }
                is AuthRepository.LoginOutcome.NeedsTenantPick ->
                    _uiState.update {
                        it.copy(isLoading = false, needsTenantPicker = outcome.user to outcome.tenants)
                    }
                AuthRepository.LoginOutcome.Cancelled ->
                    _uiState.update { it.copy(isLoading = false) }
                is AuthRepository.LoginOutcome.Error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = outcome.message) }
                AuthRepository.LoginOutcome.Locked ->
                    _uiState.update { it.copy(isLoading = false, locked = true) }
            }
        }
    }

    fun confirmPayment(userEmail: String, confirmEmail: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authRepository.confirmPaymentAndUpgrade(userEmail, confirmEmail)
            onDone(success)
        }
    }

    fun fastForwardDemo(email: String, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.fastForwardDemoTime(email)
            onDone()
        }
    }

    fun signInWithPin() {
        val s = _uiState.value
        if (s.email.isBlank() || s.pin.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email dan password wajib diisi") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val outcome = authRepository.loginWithEmailPassword(s.email, s.pin)) {
                is AuthRepository.LoginOutcome.Success ->
                    _uiState.update { it.copy(isLoading = false, signedInUser = outcome.user) }
                is AuthRepository.LoginOutcome.Error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = outcome.message) }
                AuthRepository.LoginOutcome.Locked ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            locked = true,
                            errorMessage = "Terkunci 5 menit karena 5 kali gagal."
                        )
                    }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun pickTenant(googleSub: String, tenantId: String) {
        viewModelScope.launch {
            val ok = authRepository.selectTenant(googleSub, tenantId)
            if (ok) {
                val u = _uiState.value.needsTenantPicker?.first
                    ?.copy(tenantId = tenantId)
                _uiState.update { it.copy(signedInUser = u, needsTenantPicker = null) }
            }
        }
    }
}
