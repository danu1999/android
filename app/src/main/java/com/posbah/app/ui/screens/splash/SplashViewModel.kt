package com.posbah.app.ui.screens.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.security.DeviceIntegrityGuard
import com.posbah.app.security.IntegrityChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashRoute {
    object NotReady : SplashRoute()
    object GoToLogin : SplashRoute()
    object GoToLock : SplashRoute()
    object GoToBmpDashboard : SplashRoute()
    data class Blocked(val reason: String) : SplashRoute()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val integrityChecker: IntegrityChecker,
    private val db: com.posbah.app.data.local.PosBahDatabase
) : ViewModel() {

    private val _route = MutableStateFlow<SplashRoute>(SplashRoute.NotReady)
    val route = _route.asStateFlow()

    init { evaluate() }

    private fun evaluate() {
        viewModelScope.launch {
            // 1) Device integrity guard
            val report = DeviceIntegrityGuard.inspect(appContext)
            if (report.rooted || report.hookFrameworkDetected) {
                _route.value = SplashRoute.Blocked(
                    "Perangkat Anda terdeteksi rooted / memuat hook framework. " +
                        "Demi keamanan data, POSBah tidak dapat dijalankan."
                )
                return@launch
            }

            // 2) Best-effort Play Integrity check (non-blocking)
            //    Run async, do not gate UI on it. Just log signature SHA-256.
            launch { integrityChecker.fetchIntegrityToken() }

            // 3) Session-based routing
            val sub = authRepository.activeUserSub()
            val tenant = authRepository.activeTenantId()
            var targetRoute = when {
                sub == null -> SplashRoute.GoToLogin
                tenant == null -> SplashRoute.GoToLogin
                else -> SplashRoute.GoToLock // require biometric/PIN to resume
            }

            // If the user has an active session but is a demo user, check if they've been upgraded to premium on the server
            if (targetRoute == SplashRoute.GoToLock && tenant != null && (tenant.startsWith("demo_tenant_") || tenant == "demo_tenant")) {
                val email = authRepository.activeUserEmail()
                if (!email.isNullOrBlank()) {
                    var isUpgraded = false
                    var checkConn: java.net.HttpURLConnection? = null
                    try {
                        val url = java.net.URL("https://www.zedmz.cloud/api/sync/local_users?email=eq.${java.net.URLEncoder.encode(email.lowercase().trim(), "UTF-8")}")
                        checkConn = url.openConnection() as java.net.HttpURLConnection
                        checkConn.requestMethod = "GET"
                        checkConn.connectTimeout = 3000
                        checkConn.readTimeout = 3000
                        if (checkConn.responseCode in 200..299) {
                            val response = checkConn.inputStream.bufferedReader().use { it.readText() }
                            val array = org.json.JSONArray(response)
                            if (array.length() > 0) {
                                val obj = array.getJSONObject(0)
                                isUpgraded = obj.optBoolean("isPremium", false)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        checkConn?.disconnect()
                    }

                    if (isUpgraded) {
                        // AUTOMATION: Upgraded to premium! Clear demo data to 0 and logout
                        try {
                            db.clearAllTables()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        authRepository.logout()
                        targetRoute = SplashRoute.GoToLogin
                    }
                }
            }

            _route.value = targetRoute
        }
    }
}
