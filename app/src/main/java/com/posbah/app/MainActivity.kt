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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

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
