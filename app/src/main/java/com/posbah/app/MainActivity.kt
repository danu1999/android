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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Periodic foreground auto-sync: pulls remote updates & pushes local changes every 30 seconds
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    try {
                        val tenantId = authRepository.activeTenantId()
                        if (!tenantId.isNullOrBlank()) {
                            // Run synchronization in Dispatchers.IO
                            val context = applicationContext
                            val activeDb = db
                            // 1. Pull remote updates first
                            com.posbah.app.data.remote.SupabaseSyncManager.pullAll(context, activeDb, tenantId)
                            // 2. Push any unsynced local mutations
                            com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, activeDb, tenantId)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(30_000)
                }
            }
        }

        // Block screenshots / screen recording on sensitive screens (covers entire app)
        // This is part of anti-data-exfiltration hardening for POS data.
        if (Build.TYPE != "eng") {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

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
