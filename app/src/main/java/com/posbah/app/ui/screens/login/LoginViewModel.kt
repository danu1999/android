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
    val locked: Boolean = false,
    val updateVersion: String? = null,
    val updateDescription: String? = null,
    val isCheckingUpdate: Boolean = false,
    val showUpdateDialog: Boolean = false
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
        if (s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email dan password wajib diisi") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val outcome = authRepository.loginWithEmailPassword(s.email, s.password)) {
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

    fun checkForUpdates(isManual: Boolean = false) {
        if (isManual) {
            _uiState.update { it.copy(isCheckingUpdate = true, errorMessage = null) }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var conn: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://www.zedmz.cloud/api/apk-version")
                conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                val currentVersion = com.posbah.app.BuildConfig.VERSION_NAME
                if (conn.responseCode in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val obj = org.json.JSONObject(response)
                    val version = obj.optString("version", "")
                    val description = obj.optString("description", "")
                    
                    val hasUpdate = version.isNotEmpty() && run {
                        val parts1 = version.split(".")
                        val parts2 = currentVersion.split(".")
                        val length = maxOf(parts1.size, parts2.size)
                        var isNewer = false
                        for (i in 0 until length) {
                            val n1 = parts1.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                            val n2 = parts2.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                            if (n1 > n2) {
                                isNewer = true
                                break
                            } else if (n1 < n2) {
                                break
                            }
                        }
                        isNewer
                    }
                    
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateVersion = version,
                            updateDescription = description,
                            showUpdateDialog = hasUpdate || (isManual && version.isNotEmpty()),
                            errorMessage = if (isManual && !hasUpdate) "Aplikasi Anda sudah versi terbaru." else it.errorMessage
                        )
                    }
                } else {
                    val code = conn.responseCode
                    if (isManual) {
                        _uiState.update {
                            it.copy(
                                isCheckingUpdate = false,
                                errorMessage = "Gagal mengecek update: Server merespon $code"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (isManual) {
                    val msg = e.localizedMessage ?: "Koneksi gagal"
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            errorMessage = "Gagal mengecek update: $msg"
                        )
                    }
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun clearRejoinMessage() = _uiState.update { it.copy(rejoinMessage = null) }

    fun requestRejoin(email: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, rejoinMessage = null) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var conn: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://www.zedmz.cloud/api/auth/request-rejoin?email=${java.net.URLEncoder.encode(email, "UTF-8")}")
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
