package com.posbah.app.ui.screens.login

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.posbah.app.ui.components.PrimaryButton

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNeedTenantPick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(ui.signedInUser) { if (ui.signedInUser != null) onLoggedIn() }
    LaunchedEffect(ui.needsTenantPicker) { if (ui.needsTenantPicker != null) onNeedTenantPick() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Saffron header band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.40f)
                .background(MaterialTheme.colorScheme.primary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.padding(top = 24.dp)) {
                Text(
                    "POSBah",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "POS & Invoice native Android.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Aman, cepat, offline-first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().testTag("login-card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ModeSelector(current = ui.mode, onChange = viewModel::switchMode)
                    Spacer(Modifier.height(20.dp))

                    when (ui.mode) {
                        LoginMode.Google -> GoogleSection(
                            isLoading = ui.isLoading,
                            onClick = { activity?.let { viewModel.signInWithGoogle(it) } }
                        )
                        LoginMode.Pin -> PinSection(
                            tenantId = ui.pinTenantId,
                            email = ui.email,
                            pin = ui.pin,
                            isLoading = ui.isLoading,
                            onTenantChange = viewModel::updatePinTenantId,
                            onEmailChange = viewModel::updateEmail,
                            onPinChange = viewModel::updatePin,
                            onSubmit = viewModel::signInWithPin
                        )
                    }

                    ui.errorMessage?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Terenkripsi SQLCipher • Keystore hardware-backed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(current: LoginMode, onChange: (LoginMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeTab("Google SSO", current == LoginMode.Google) { onChange(LoginMode.Google) }
        ModeTab("PIN Karyawan", current == LoginMode.Pin) { onChange(LoginMode.Pin) }
    }
}

@Composable
private fun RowScope.ModeTab(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(3.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoogleSection(isLoading: Boolean, onClick: () -> Unit) {
    Column {
        Text(
            "Login dengan akun Google yang terhubung dengan tenant Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Menghubungi Google\u2026", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            PrimaryButton(
                label = "Masuk dengan Google",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().testTag("btn-google-login")
            )
        }
    }
}

@Composable
private fun PinSection(
    tenantId: String,
    email: String,
    pin: String,
    isLoading: Boolean,
    onTenantChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = tenantId,
            onValueChange = onTenantChange,
            label = { Text("Tenant ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("pin-tenant"),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Karyawan") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("pin-email"),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("PIN 6 digit") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("pin-input"),
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            label = if (isLoading) "Memverifikasi\u2026" else "Masuk",
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().testTag("btn-pin-login")
        )
    }
}

internal fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
