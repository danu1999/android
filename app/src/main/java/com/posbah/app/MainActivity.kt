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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Periodic foreground auto-sync: pulls remote updates & pushes local changes every 30 seconds
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    while (true) {
                        try {
                            val tenantId = authRepository.activeTenantId()
                            if (!tenantId.isNullOrBlank()) {
                                val context = applicationContext
                                val activeDb = db

                                // Connect WebSocket client for real-time push updates
                                com.posbah.app.data.remote.WebSocketSyncClient.connect(context, tenantId, activeDb)

                                // One-time local POS data wipe for POS redesign v2.17.29 (except bahteramulyap)
                                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                                if (!securePrefs.isPosRedesignWipedV1) {
                                    val email = authRepository.activeUserEmail()?.lowercase()?.trim().orEmpty()
                                    if (email != "bahteramulyap@gmail.com" && tenantId != "ten_premium_bahteramulyap_gmail_com") {
                                        try {
                                            val writeDb = activeDb.openHelper.writableDatabase
                                            writeDb.execSQL("DELETE FROM products WHERE tenantId = ?", arrayOf(tenantId))
                                            writeDb.execSQL("DELETE FROM transaction_items WHERE transactionId NOT IN (SELECT id FROM transactions WHERE tenantId = ?)", arrayOf(tenantId))
                                            writeDb.execSQL("DELETE FROM transactions WHERE tenantId = ?", arrayOf(tenantId))
                                            writeDb.execSQL("DELETE FROM customers WHERE tenantId = ?", arrayOf(tenantId))
                                            writeDb.execSQL("DELETE FROM activity_logs WHERE tenantId = ?", arrayOf(tenantId))
                                            android.util.Log.i("MainActivity", "POS redesign local data wipe completed successfully for tenant: $tenantId")
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "Failed to run POS redesign local data wipe", e)
                                        }
                                    }
                                    securePrefs.isPosRedesignWipedV1 = true
                                }

                                val email = authRepository.activeUserEmail()
                                if (!email.isNullOrBlank()) {
                                    val dbUser = activeDb.localUserDao().getByEmail(email)
                                    if (dbUser != null && dbUser.apkVersion != com.posbah.app.BuildConfig.VERSION_NAME) {
                                        try {
                                            com.posbah.app.data.remote.SupabaseSyncManager.pullAll(context, activeDb, tenantId)
                                            com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, activeDb, tenantId)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        activeDb.localUserDao().upsert(
                                            dbUser.copy(
                                                apkVersion = com.posbah.app.BuildConfig.VERSION_NAME,
                                                updatedAt = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                }

                                // If this is a demo session, perform an upgrade check against the server
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
                                                activeDb.clearAllTables()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            authRepository.logout()
                                            // Recreate activity to force redirect immediately back to splash/login and clear composable states
                                            this@MainActivity.recreate()
                                            break
                                        }
                                    }
                                }

                                // Optimized Realtime Polling: Check status first
                                val serverTs = com.posbah.app.data.remote.SupabaseSyncManager.checkServerSyncStatus(context, tenantId)
                                val localTs = securePrefs.lastSyncedTimestamp
                                if (serverTs > localTs || localTs == 0L) {
                                    // 1. Pull remote updates first
                                    val pullRes = com.posbah.app.data.remote.SupabaseSyncManager.pullAll(context, activeDb, tenantId)
                                    // 2. Push any unsynced local mutations
                                    val syncRes = com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, activeDb, tenantId)

                                    if (pullRes is com.posbah.app.data.remote.SupabaseSyncManager.SyncResult.Success &&
                                        syncRes is com.posbah.app.data.remote.SupabaseSyncManager.SyncResult.Success) {
                                        securePrefs.lastSyncedTimestamp = if (serverTs > 0L) serverTs else System.currentTimeMillis()

                                        // Reactively monitor and update employee outlet lock if owner changed it
                                        val activeEmail = authRepository.activeUserEmail()
                                        if (!activeEmail.isNullOrBlank()) {
                                            val dbUser = activeDb.localUserDao().getByEmail(activeEmail)
                                            if (dbUser != null && dbUser.role != "OWNER") {
                                                val empRecord = activeDb.employeeDao().findByEmail(activeEmail)
                                                if (empRecord != null) {
                                                    val currentLock = sessionState.lockedEmployeeOutletId.value
                                                    if (empRecord.outletId != currentLock) {
                                                        android.util.Log.i("MainActivity", "Dynamic outlet shift detected for employee $activeEmail: $currentLock -> ${empRecord.outletId}")
                                                        sessionState.setEmployeeOutletLock(empRecord.outletId)
                                                        securePrefs.currentOutletId = empRecord.outletId
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                com.posbah.app.data.remote.WebSocketSyncClient.disconnect()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        delay(30_000)
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
}
