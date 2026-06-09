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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.sp
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

    var showGoogleSimDialog by remember { mutableStateOf(false) }
    var simEmail by remember { mutableStateOf("demo@posbah.app") }
    var simName by remember { mutableStateOf("User Demo") }

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var upgradeTargetEmail by remember { mutableStateOf("") }
    var upgradeConfirmEmail by remember { mutableStateOf("") }

    var showFastForwardDialog by remember { mutableStateOf(false) }
    var fastForwardEmail by remember { mutableStateOf("") }

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
                            onClick = { showGoogleSimDialog = true }
                        )
                        LoginMode.Pin -> PremiumSection(
                            email = ui.email,
                            pin = ui.pin,
                            isLoading = ui.isLoading,
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
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("login-error-msg")
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "ALAT SIMULASI & TESTING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                showUpgradeDialog = true
                                upgradeTargetEmail = ""
                                upgradeConfirmEmail = "muhammadmuizz8@gmail.com"
                            },
                            modifier = Modifier.weight(1f).testTag("btn-open-upgrade")
                        ) {
                            Text("Upgrade Premium", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                showFastForwardDialog = true
                                fastForwardEmail = ""
                            },
                            modifier = Modifier.weight(1f).testTag("btn-open-fastforward")
                        ) {
                            Text("Percepat 2 Hari", fontSize = 11.sp)
                        }
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

    // Dialog: Google Sign-In Simulation
    if (showGoogleSimDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSimDialog = false },
            title = { Text("Simulasi Google Sign-In") },
            text = {
                Column {
                    Text(
                        "Pilih preset atau masukkan email untuk menyimulasikan login Google SSO.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = simEmail,
                        onValueChange = { emailVal -> simEmail = emailVal },
                        label = { Text("Email Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sim-google-email")
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = simName,
                        onValueChange = { nameVal -> simName = nameVal },
                        label = { Text("Nama Tampilan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sim-google-name")
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Presets:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { simEmail = "demo@posbah.app"; simName = "Demo User" }) {
                                Text("demo@posbah.app", fontSize = 10.sp)
                            }
                            TextButton(onClick = { simEmail = "muhammadmuizz8@gmail.com"; simName = "Muhammad Muizz" }) {
                                Text("muhammadmuizz8@gmail.com", fontSize = 10.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { simEmail = "bahteramulyap@gmail.com"; simName = "CV Bahtera Mulya Plastik" }) {
                                Text("bahteramulyap@gmail.com", fontSize = 10.sp)
                            }
                            TextButton(onClick = { simEmail = "hanafiariful@gmail.com"; simName = "PISANG KEJU RAMAYANA" }) {
                                Text("hanafiariful@gmail.com", fontSize = 10.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { simEmail = "fahrup22@gmail.com"; simName = "FahriP (Karyawan)" }) {
                                Text("fahrup22@gmail.com", fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleSimDialog = false
                        activity?.let {
                            viewModel.signInWithGoogle(it, simEmail, simName)
                        }
                    },
                    modifier = Modifier.testTag("btn-sim-login")
                ) { Text("Masuk") }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleSimDialog = false }) { Text("Batal") }
            }
        )
    }

    // Dialog: Confirm Payment Upgrade
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Konfirmasi Pembayaran Premium") },
            text = {
                Column {
                    Text(
                        "Hubungkan akun demo Anda ke Premium. Harus dikonfirmasi oleh email administrator muhammadmuizz8@gmail.com.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = upgradeTargetEmail,
                        onValueChange = { target -> upgradeTargetEmail = target },
                        label = { Text("Email Akun Demo Anda") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("upgrade-target-email")
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = upgradeConfirmEmail,
                        onValueChange = { confirm -> upgradeConfirmEmail = confirm },
                        label = { Text("Email Konfirmator") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("upgrade-confirm-email")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmPayment(upgradeTargetEmail, upgradeConfirmEmail) { success ->
                            if (success) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Akun berhasil di-upgrade ke Premium! Silakan login ulang.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                showUpgradeDialog = false
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Gagal! Konfirmasi email salah atau akun tidak ditemukan.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn-submit-upgrade")
                ) { Text("Upgrade Sekarang") }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) { Text("Batal") }
            }
        )
    }

    // Dialog: Fast Forward Expired
    if (showFastForwardDialog) {
        AlertDialog(
            onDismissRequest = { showFastForwardDialog = false },
            title = { Text("Simulasikan Batas Uji Coba (2 Hari)") },
            text = {
                Column {
                    Text(
                        "Ubah waktu registrasi akun demo terpilih menjadi 2 hari yang lalu untuk mensimulasikan lockout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fastForwardEmail,
                        onValueChange = { ff -> fastForwardEmail = ff },
                        label = { Text("Email Akun Demo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("fastforward-email")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.fastForwardDemo(fastForwardEmail) {
                            android.widget.Toast.makeText(
                                context,
                                "Waktu registrasi berhasil dimanipulasi! Silakan login untuk tes lockout.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            showFastForwardDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn-submit-fastforward")
                ) { Text("Percepat Waktu") }
            },
            dismissButton = {
                TextButton(onClick = { showFastForwardDialog = false }) { Text("Batal") }
            }
        )
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
        ModeTab("User Demo (Google)", current == LoginMode.Google) { onChange(LoginMode.Google) }
        ModeTab("Premium (Email)", current == LoginMode.Pin) { onChange(LoginMode.Pin) }
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
            "Masuk secara offline dengan Akun Demo Google POSBah.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Memproses masuk demo\u2026", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            PrimaryButton(
                label = "Masuk Demo User",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().testTag("btn-google-login")
            )
        }
    }
}

@Composable
private fun PremiumSection(
    email: String,
    pin: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("pin-email"),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("Password") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
