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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val sessionState: com.posbah.app.data.repository.SessionState
) : ViewModel() {

    private val _route = MutableStateFlow<SplashRoute>(SplashRoute.NotReady)
    val route = _route.asStateFlow()

    init { evaluate() }

    private fun evaluate() {
        viewModelScope.launch {
            // 1) Device integrity guard — local check, no network, must be synchronous
            val report = withContext(Dispatchers.IO) {
                DeviceIntegrityGuard.inspect(appContext)
            }
            if (report.rooted || report.hookFrameworkDetected) {
                _route.value = SplashRoute.Blocked(
                    "Perangkat Anda terdeteksi rooted / memuat hook framework. " +
                        "Demi keamanan data, POSBah tidak dapat dijalankan."
                )
                return@launch
            }

            // 2) Play Integrity check — non-blocking fire-and-forget
            launch(Dispatchers.IO) {
                integrityChecker.fetchIntegrityToken()
            }

            // 3) Session-based routing — purely local/preferences, no network needed
            val sub = authRepository.activeUserSub()
            val tenant = authRepository.activeTenantId()

            // 4) Server online check — fire-and-forget (does NOT block routing)
            //    Result is stored in SessionState and used by the rest of the app.
            //    A 3-second connect timeout was blocking the splash for up to 6 s.
            if (sub != null && tenant != null) {
                launch(Dispatchers.IO) {
                    var conn: java.net.HttpURLConnection? = null
                    try {
                        val url = java.net.URL(
                            "https://www.zedmz.cloud/api/sync/check-status?tenantId=${
                                java.net.URLEncoder.encode(tenant, "UTF-8")
                            }"
                        )
                        conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 5_000
                        conn.readTimeout = 5_000
                        val online = conn.responseCode in 200..299
                        sessionState.setOnline(online)
                    } catch (e: Exception) {
                        sessionState.setOnline(false)
                    } finally {
                        conn?.disconnect()
                    }
                }
            }

            // 5) Determine target route immediately (no network wait)
            var targetRoute: SplashRoute = when {
                sub == null  -> SplashRoute.GoToLogin
                tenant == null -> SplashRoute.GoToLogin
                else -> SplashRoute.GoToLock // require biometric/PIN to resume
            }

            // 6) Premium upgrade check for demo users — fire-and-forget.
            //    If the user is upgraded, they are logged out in the background;
            //    on the NEXT app launch the routing will correctly send them to Login.
            //    This avoids an extra 3-second network wait on every splash.
            if (targetRoute == SplashRoute.GoToLock && tenant != null &&
                (tenant.startsWith("demo_tenant_") || tenant == "demo_tenant")
            ) {
                val email = authRepository.activeUserEmail()
                if (!email.isNullOrBlank()) {
                    launch(Dispatchers.IO) {
                        var checkConn: java.net.HttpURLConnection? = null
                        try {
                            val url = java.net.URL(
                                "https://www.zedmz.cloud/api/sync/local_users?email=eq.${
                                    java.net.URLEncoder.encode(email.lowercase().trim(), "UTF-8")
                                }"
                            )
                            checkConn = url.openConnection() as java.net.HttpURLConnection
                            checkConn.requestMethod = "GET"
                            checkConn.setRequestProperty(
                                "x-client-version",
                                com.posbah.app.BuildConfig.VERSION_NAME
                            )
                            checkConn.connectTimeout = 5_000
                            checkConn.readTimeout = 5_000
                            if (checkConn.responseCode in 200..299) {
                                val response =
                                    checkConn.inputStream.bufferedReader().use { it.readText() }
                                val array = org.json.JSONArray(response)
                                if (array.length() > 0 &&
                                    array.getJSONObject(0).optBoolean("isPremium", false)
                                ) {
                                    // User upgraded — clear demo data and force re-login on next launch
                                    try { db.clearAllTables() } catch (e: Exception) { e.printStackTrace() }
                                    authRepository.logout()
                                    // Now update the route on the main thread so user sees Login immediately
                                    withContext(Dispatchers.Main) {
                                        _route.value = SplashRoute.GoToLogin
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            checkConn?.disconnect()
                        }
                    }
                }
            }

            // Emit routing decision immediately — no more waiting on network
            _route.value = targetRoute
        }
    }
}
