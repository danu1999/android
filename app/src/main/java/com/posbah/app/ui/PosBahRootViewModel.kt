package com.posbah.app.ui

import androidx.lifecycle.ViewModel
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.security.SecurePreferences
import com.posbah.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateRequired(val version: String, val description: String) : UpdateState()
    object UpToDate : UpdateState()
}

@HiltViewModel
class PosBahRootViewModel @Inject constructor(
    private val securePrefs: SecurePreferences,
    private val tenantDao: TenantDao,
    private val userDao: LocalUserDao,
    private val sessionState: com.posbah.app.data.repository.SessionState
) : ViewModel() {

    val isOnline = sessionState.isOnline

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    init {
        checkForForcedUpdate()
    }

    fun checkForForcedUpdate() {
        _updateState.value = UpdateState.Checking
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
                    
                    if (hasUpdate) {
                        _updateState.value = UpdateState.UpdateRequired(version, description)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.UpToDate
            } finally {
                conn?.disconnect()
            }
        }
    }

    /**
     * Inspects the active tenant's businessMode to return the correct dashboard route path.
     * Redirects to SystemSelection if a demo user hasn't locked their POS system choice.
     */
    suspend fun getDashboardRoute(): String {
        val sub = securePrefs.currentGoogleSub
        if (sub != null) {
            val user = userDao.getBySub(sub)
            if (user != null && !user.businessModeLocked) {
                return Screen.SystemSelection.route
            }
        }
        val tenantId = securePrefs.currentTenantId ?: return Screen.Login.route
        val tenant = tenantDao.getById(tenantId) ?: return Screen.Login.route
        return when (tenant.businessMode) {
            "FNB" -> Screen.PosDashboard.route
            "RENTAL" -> Screen.RentalDashboard.route
            "LAUNDRY" -> Screen.LaundryDashboard.route
            else -> Screen.BmpDashboard.route
        }
    }
}
