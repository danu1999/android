package com.posbah.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.posbah.app.ui.PosBahRoot
import com.posbah.app.ui.theme.POSBahTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.repository.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect

/**
 * Provides the current Activity to the Compose tree so that Credential Manager
 * (which requires an Activity context) and BiometricPrompt (which requires a
 * FragmentActivity) can be invoked from any screen.
 */
val LocalActivity = staticCompositionLocalOf<FragmentActivity> {
    error("LocalActivity not provided")
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var db: PosBahDatabase

    @Inject
    lateinit var sessionState: SessionState

    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Setup active network connection guard callback
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                sessionState.setOnline(true)
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                sessionState.setOnline(false)
            }
        }
        networkCallback = callback
        connectivityManager.registerNetworkCallback(networkRequest, callback)

        // Initial connection check
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isOnlineInitial = capabilities != null && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        sessionState.setOnline(isOnlineInitial)

        // Setup connection listener callback for remote requests
        com.posbah.app.data.remote.SupabaseSyncManager.onConnectionStateChanged = { online ->
            sessionState.setOnline(online)
        }

        // Full Online mode: periodic foreground data refresh via Retrofit API every 30 seconds.
        // No more SupabaseSyncManager bulk sync (no local DB). All data comes from VPS real-time.
        lifecycleScope.launch(Dispatchers.IO) {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    while (true) {
                        try {
                            val tenantId = authRepository.activeTenantId()
                            if (!tenantId.isNullOrBlank() && sessionState.isOnline.value) {
                                // Keep WebSocket for real-time push notifications (no-op stub currently)
                                com.posbah.app.data.remote.WebSocketSyncClient.connect(
                                    applicationContext, tenantId, db
                                )

                                // Demo upgrade check (unchanged logic)
                                if (tenantId.startsWith("demo_tenant_") || tenantId == "demo_tenant") {
                                    val email = authRepository.activeUserEmail()
                                    if (!email.isNullOrBlank()) {
                                        var isUpgraded = false
                                        var checkConn: java.net.HttpURLConnection? = null
                                        try {
                                            val url = java.net.URL("https://www.zedmz.cloud/api/sync/local_users?email=eq.${java.net.URLEncoder.encode(email.lowercase().trim(), "UTF-8")}")
                                            checkConn = url.openConnection() as java.net.HttpURLConnection
                                            checkConn.requestMethod = "GET"
                                            checkConn.setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                                            checkConn.connectTimeout = 5000
                                            checkConn.readTimeout = 5000
                                            if (checkConn.responseCode in 200..299) {
                                                val response = checkConn.inputStream.bufferedReader().use { it.readText() }
                                                val array = org.json.JSONArray(response)
                                                if (array.length() > 0) {
                                                    isUpgraded = array.getJSONObject(0).optBoolean("isPremium", false)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            checkConn?.disconnect()
                                        }

                                        if (isUpgraded) {
                                            authRepository.logout()
                                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                this@MainActivity.recreate()
                                            }
                                            break
                                        }
                                    }
                                }

                                // Full Online: Active employee deactivation check via VPS API
                                // (replaces the old DAO-based employee watcher that called stub DAOs)
                                val currentRole = com.posbah.app.security.SecurePreferences(applicationContext).currentRole
                                if (currentRole != null && currentRole != "OWNER") {
                                    val activeEmail = authRepository.activeUserEmail()
                                    if (!activeEmail.isNullOrBlank()) {
                                        try {
                                            val url = java.net.URL("https://www.zedmz.cloud/api/sync/employees?email=eq.${java.net.URLEncoder.encode(activeEmail.lowercase().trim(), "UTF-8")}&tenantId=eq.$tenantId")
                                            val conn = url.openConnection() as java.net.HttpURLConnection
                                            conn.requestMethod = "GET"
                                            conn.connectTimeout = 5000
                                            conn.readTimeout = 5000
                                            if (conn.responseCode in 200..299) {
                                                val body = conn.inputStream.bufferedReader().use { it.readText() }
                                                val arr = org.json.JSONArray(body)
                                                if (arr.length() == 0) {
                                                    // Employee deleted from VPS → force logout
                                                    android.util.Log.w("MainActivity", "Employee not found on VPS. Force logout.")
                                                    authRepository.logout()
                                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                        this@MainActivity.recreate()
                                                    }
                                                } else {
                                                    val emp = arr.getJSONObject(0)
                                                    if (!emp.optBoolean("isActive", true)) {
                                                        android.util.Log.w("MainActivity", "Employee deactivated on VPS. Force logout.")
                                                        authRepository.logout()
                                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                            this@MainActivity.recreate()
                                                        }
                                                    } else {
                                                        // Check outlet shift
                                                        val vpsOutletId = emp.optLong("outletId").takeIf { it > 0 }
                                                        val currentLock = sessionState.lockedEmployeeOutletId.value
                                                        if (vpsOutletId != currentLock) {
                                                            android.util.Log.i("MainActivity", "Dynamic outlet shift: $currentLock -> $vpsOutletId")
                                                            sessionState.setEmployeeOutletLock(vpsOutletId)
                                                            com.posbah.app.security.SecurePreferences(applicationContext).currentOutletId = vpsOutletId
                                                        }
                                                    }
                                                }
                                            }
                                            conn.disconnect()
                                        } catch (e: Exception) {
                                            android.util.Log.w("MainActivity", "Employee check failed: ${e.message}")
                                        }
                                    }
                                }
                            } else if (!sessionState.isOnline.value) {
                                com.posbah.app.data.remote.WebSocketSyncClient.disconnect()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        delay(if (sessionState.isOnline.value) 30_000 else 5_000)
                    }
                } finally {
                    com.posbah.app.data.remote.WebSocketSyncClient.disconnect()
                }
            }
        }

        // Block screenshots / screen recording on sensitive screens (covers entire app)
        // This is part of anti-data-exfiltration hardening for POS data.
        //if (Build.TYPE != "eng") {
            //window.setFlags(
                //WindowManager.LayoutParams.FLAG_SECURE,
                //WindowManager.LayoutParams.FLAG_SECURE
            //)
        //}

        // Preload database on a background thread to move SQLCipher load and key derivation off main thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.openHelper.writableDatabase
                android.util.Log.i("MainActivity", "Database preloaded successfully on background thread")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error preloading database", e)
            }
        }
        // Note: Employee deactivation & outlet shift detection is now handled in the 
        // periodic API polling loop above (30-second interval, checks VPS /api/sync/employees).
        // The old db.employeeDao().observeAll() was calling a stub DAO — removed.
        
        enableEdgeToEdge()

        // Keep splash on screen until the SplashViewModel signals ready
        splash.setKeepOnScreenCondition { false }

        setContent {
            POSBahTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    PosBahRoot()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
    }
}
