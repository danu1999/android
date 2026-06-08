package com.posbah.app.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.posbah.app.security.BiometricHelper
import com.posbah.app.ui.components.ButtonVariant
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.ui.screens.login.findActivity

/**
 * Biometric unlock screen shown when the app resumes from background after the
 * user has previously signed in. If biometric unavailable, falls back to logout.
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }
    var attempting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (activity == null) {
            error = "Activity tidak ditemukan"
            return@LaunchedEffect
        }
        val avail = BiometricHelper.availability(activity)
        if (avail != BiometricHelper.Availability.AVAILABLE) {
            // No biometric — let user proceed via re-login
            error = when (avail) {
                BiometricHelper.Availability.NONE_ENROLLED ->
                    "Biometrik belum didaftarkan di perangkat. Silakan login ulang."
                BiometricHelper.Availability.NO_HARDWARE,
                BiometricHelper.Availability.HW_UNAVAILABLE ->
                    "Biometrik tidak tersedia. Silakan login ulang."
                else -> "Verifikasi biometrik gagal. Silakan login ulang."
            }
            return@LaunchedEffect
        }
        attempting = true
        val r = BiometricHelper.authenticate(
            activity,
            title = "Verifikasi Identitas",
            subtitle = "Gunakan biometrik untuk membuka POSBah",
            negativeText = "Batal"
        )
        attempting = false
        when (r) {
            BiometricHelper.Result.Success -> onUnlocked()
            BiometricHelper.Result.Cancelled -> error = "Dibatalkan"
            else -> error = "Verifikasi gagal"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Aplikasi Terkunci",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Verifikasi identitas Anda untuk melanjutkan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            error?.let {
                Spacer(Modifier.height(20.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    label = "Login Ulang",
                    onClick = onLogout,
                    modifier = Modifier.testTag("btn-relogin")
                )
            }

            if (attempting && error == null) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Menunggu verifikasi\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
