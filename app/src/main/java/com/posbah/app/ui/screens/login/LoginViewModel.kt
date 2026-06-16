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
    val password: String = "",
    val passwordTenantId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorEmail: String? = null,
    val rejoinMessage: String? = null,
    val signedInUser: LocalUser? = null,
    val needsTenantPicker: Pair<LocalUser, List<Tenant>>? = null,
    val locked: Boolean = false
)

enum class LoginMode { Google, Password }

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
    fun updatePassword(p: String) = _uiState.update { it.copy(password = p.take(64)) }
    fun updatePasswordTenantId(t: String) = _uiState.update { it.copy(passwordTenantId = t) }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun signInWithGoogle(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val outcome = authRepository.loginWithGoogle(activity)) {
                is AuthRepository.LoginOutcome.Success ->
                    _uiState.update { it.copy(isLoading = false, signedInUser = outcome.user) }
                is AuthRepository.LoginOutcome.NeedsTenantPick ->
                    _uiState.update {
                        it.copy(isLoading = false, needsTenantPicker = outcome.user to outcome.tenants)
                    }
                AuthRepository.LoginOutcome.Cancelled ->
                    _uiState.update { it.copy(isLoading = false) }
                is AuthRepository.LoginOutcome.Error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = outcome.message, errorEmail = outcome.email) }
                AuthRepository.LoginOutcome.Locked ->
                    _uiState.update { it.copy(isLoading = false, locked = true) }
            }
        }
    }



    fun signInWithPassword() {
        val s = _uiState.value
        val cleanEmail = s.email.trim().lowercase()
        if (cleanEmail.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email dan password wajib diisi") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val outcome = authRepository.loginWithEmailPassword(cleanEmail, s.password)) {
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memproses masuk: ${e.localizedMessage}"
                    )
                }
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



    fun clearRejoinMessage() = _uiState.update { it.copy(rejoinMessage = null) }

    fun requestRejoin(email: String) {
        val cleanEmail = email.trim().lowercase()
        _uiState.update { it.copy(isLoading = true, errorMessage = null, rejoinMessage = null) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var conn: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://www.zedmz.cloud/api/auth/request-rejoin?email=${java.net.URLEncoder.encode(cleanEmail, "UTF-8")}")
                conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode in 200..299) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rejoinMessage = "Permintaan rejoin terkirim. Silakan periksa email Anda ($email) untuk mencoba menggunakan POSBah lagi."
                        )
                    }
                } else {
                    val msg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Server returned ${conn.responseCode}"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal meminta rejoin: $msg"
                        )
                    }
                }
            } catch (e: java.lang.Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal menghubungi server: ${e.localizedMessage}"
                    )
                }
            } finally {
                conn?.disconnect()
            }
        }
    }
}
