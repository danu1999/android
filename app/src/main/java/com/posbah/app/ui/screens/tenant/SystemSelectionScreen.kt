package com.posbah.app.ui.screens.tenant

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.security.SecurePreferences
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.data.repository.SessionState
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.UserSession
import com.posbah.app.data.repository.OutletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.random.Random

data class SystemOption(
    val code: String, // BMP | FNB | RENTAL | LAUNDRY
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@HiltViewModel
class SystemSelectionViewModel @Inject constructor(
    private val securePrefs: SecurePreferences,
    private val sessionState: SessionState,
    private val authRepository: AuthRepository,
    private val outletRepo: OutletRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _user = MutableStateFlow<UserSession?>(null)
    val user = _user.asStateFlow()

    init {
        val email = securePrefs.currentEmail
        if (email != null) {
            viewModelScope.launch {
                try {
                    _user.value = authRepository.fetchUserOnline(email)
                } catch (e: Exception) {
                    _user.value = authRepository.getActiveSession()
                }
            }
        }
    }

    fun getTempPlainPassword(): String {
        return securePrefs.tempPlainPassword ?: ""
    }

    fun saveProfile(
        businessName: String,
        whatsapp: String,
        newPin: String?,
        onDone: (Boolean, String?) -> Unit
    ) {
        val email = securePrefs.currentEmail ?: return onDone(false, "Sesi tidak valid")
        val tempPassword = securePrefs.tempPlainPassword.orEmpty()
        viewModelScope.launch {
            try {
                val u = authRepository.fetchUserOnline(email) ?: return@launch onDone(false, "User tidak ditemukan")

                // If a new password is provided, change it
                if (!newPin.isNullOrBlank()) {
                    val res = authRepository.changePassword(tempPassword, newPin)
                    if (res is AuthRepository.ChangePasswordResult.Error) {
                        return@launch onDone(false, res.message)
                    }
                }

                val success = authRepository.updateProfileOnline(email, u.tenantId ?: "", businessName, whatsapp)
                if (!success) {
                    return@launch onDone(false, "Gagal memperbarui profil di server")
                }
                val updatedUser = u.copy(
                    displayName = businessName,
                    whatsapp = whatsapp
                )
                _user.value = updatedUser
                securePrefs.tempPlainPassword = null
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, e.localizedMessage)
            }
        }
    }

    fun lockInSystem(businessMode: String, onDone: () -> Unit) {
        val email = securePrefs.currentEmail ?: return

        viewModelScope.launch {
            val u = _user.value
            if (u != null) {
                val isPremiumUser = u.isPremium
                val emailKey = email.lowercase().trim().replace(".", "_").replace("@", "_")
                val chosenTenantId = when (email.lowercase().trim()) {
                    "hanafiariful@gmail.com" -> "ten_premium_hanafiariful_gmail_com"
                    "bahteramulyap@gmail.com" -> "ten_premium_bahteramulyap_gmail_com"
                    else -> if (isPremiumUser) {
                        "ten_premium_${emailKey}_$businessMode"
                    } else {
                        "demo_tenant_${emailKey}_$businessMode"
                    }
                }

                // Create the tenant if it doesn't exist
                val modeName = when (businessMode) {
                    "FNB" -> "FnB"
                    "RENTAL" -> "Rental"
                    "LAUNDRY" -> "Laundry"
                    else -> "Invoice & Manufaktur"
                }
                val tenantName = if (isPremiumUser) {
                    val customName = u.displayName
                    if (!customName.isNullOrBlank()) customName else "CV. $email ($modeName)"
                } else {
                    "Demo - ${u.displayName ?: email} ($modeName)"
                }

                authRepository.createTenant(email, tenantName, businessMode, chosenTenantId)

                // Lock system selection online
                authRepository.updateProfileOnline(email, chosenTenantId, u.displayName ?: "", u.whatsapp ?: "")
                // Also update the local session state
                securePrefs.currentTenantId = chosenTenantId
                securePrefs.currentBusinessMode = businessMode
                sessionState.setTenant(chosenTenantId)

                // Create and select default outlet for the chosen tenant (except BMP)
                if (businessMode != "BMP") {
                    val outlets = outletRepo.list()
                    val activeOutlet = outlets.firstOrNull { it.isDefault } ?: outlets.firstOrNull()
                    val activeOutletId = if (activeOutlet == null) {
                        outletRepo.create("Outlet Utama")
                    } else {
                        activeOutlet.id
                    }
                    sessionState.setOutlet(activeOutletId)
                } else {
                    sessionState.setOutlet(null)
                }
            }
            onDone()
        }
    }
}

@Composable
fun SystemSelectionScreen(
    onSelected: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SystemSelectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userState by viewModel.user.collectAsState()
    val user = userState

    // State for flow progression
    var currentFlowStep by remember { mutableStateOf(1) } // 1: Profile Setup, 2: Choose System

    // Setup form fields state
    var businessName by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // OTP states
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }
    var isOtpVerified by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var profileSaveError by remember { mutableStateOf<String?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }

    // Pre-fill business name from default username if empty
    LaunchedEffect(user) {
        if (user != null && businessName.isEmpty()) {
            businessName = if (user.displayName != "Owner Premium" && user.displayName != "Premium Owner") {
                user.displayName.orEmpty()
            } else {
                ""
            }
        }
    }

    // Determine if we need to show the Setup Form first
    val isPremium = user?.isPremium == true
    val needsSetup = isPremium && (user?.whatsapp.isNullOrBlank() || isOtpVerified.not())

    if (needsSetup && currentFlowStep == 1) {
        // Render Premium Profile Setup Form
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            PosBahTopBar(
                title = "Lengkapi Profil Premium",
                subtitle = "Langkah 1 dari 2 sebelum memilih sistem",
                actions = {
                    TextButton(onClick = onLogout) { Text("Keluar") }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Selamat! Akun Anda Telah Premium",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Silakan lengkapi informasi usaha Anda untuk melokalisasi faktur, struk, dan sistem pengelolaan.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Email Field (Read-Only)
                item {
                    OutlinedTextField(
                        value = user?.email.orEmpty(),
                        onValueChange = {},
                        label = { Text("Email Pengguna (Akun)") },
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup-email")
                    )
                }

                // Business Name Field
                item {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Nama Usaha / Perusahaan") },
                        placeholder = { Text("Contoh: Toko Roti Makmur") },
                        leadingIcon = { Icon(Icons.Outlined.Business, null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup-business-name")
                    )
                }

                // Generated Password (Read-Only but visible)
                item {
                    val tempPwd = viewModel.getTempPlainPassword()
                    OutlinedTextField(
                        value = tempPwd.ifEmpty { "******" },
                        onValueChange = {},
                        label = { Text("Password dari Gmail (Sistem)") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup-generated-password")
                    )
                }

                // Option small label
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Text(
                            "bebas ganti atau tidak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 4.dp, top = (-10).dp)
                        )
                    }
                }

                // New Password Field
                item {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password Baru (Opsional)") },
                        placeholder = { Text("Tulis password baru jika ingin ganti") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                Icon(
                                    if (isNewPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup-new-password")
                    )
                }

                // Confirm Password Field
                item {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Password Baru") },
                        placeholder = { Text("Tulis ulang password baru") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    if (isConfirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup-confirm-password")
                    )
                }

                // WhatsApp Field
                item {
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = {
                            if (!isOtpVerified) {
                                whatsapp = it
                            }
                        },
                        label = { Text("Nomor WhatsApp Usaha") },
                        placeholder = { Text("Contoh: 08123456789") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                        trailingIcon = {
                            if (isOtpVerified) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF22C57E),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        if (whatsapp.length >= 10) {
                                            // Generate simulated 6-digit code
                                            generatedOtp = (100000 + Random.nextInt(900000)).toString()
                                            enteredOtp = ""
                                            otpError = null
                                            showOtpDialog = true
                                            Toast.makeText(context, "[SIMULASI OTP] Kode dikirim: $generatedOtp", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Nomor WhatsApp tidak valid!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = whatsapp.length >= 10,
                                    modifier = Modifier.testTag("btn-send-otp")
                                ) {
                                    Text("Kirim OTP")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        readOnly = isOtpVerified,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup-whatsapp")
                    )
                }

                item {
                    profileSaveError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (businessName.trim().isBlank()) {
                                profileSaveError = "Nama usaha tidak boleh kosong"
                                return@Button
                            }
                            if (!isOtpVerified) {
                                profileSaveError = "Mohon lakukan konfirmasi verifikasi OTP WhatsApp"
                                return@Button
                            }
                            if (newPassword.isNotEmpty()) {
                                if (newPassword != confirmPassword) {
                                    profileSaveError = "Konfirmasi password baru tidak cocok"
                                    return@Button
                                }
                                if (newPassword.length < 4) {
                                    profileSaveError = "Password minimal 4 karakter"
                                    return@Button
                                }
                            }

                            isSavingProfile = true
                            profileSaveError = null
                            viewModel.saveProfile(
                                businessName = businessName.trim(),
                                whatsapp = whatsapp.trim(),
                                newPin = newPassword.takeIf { it.isNotEmpty() }
                            ) { success, errorMsg ->
                                isSavingProfile = false
                                if (success) {
                                    Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    currentFlowStep = 2 // Go to choose system step
                                } else {
                                    profileSaveError = errorMsg ?: "Gagal menyimpan profil"
                                }
                            }
                        },
                        enabled = !isSavingProfile && isOtpVerified && businessName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn-save-profile")
                    ) {
                        if (isSavingProfile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Simpan Profil & Lanjut", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Simulated OTP Verification Dialog
        if (showOtpDialog) {
            AlertDialog(
                onDismissRequest = { showOtpDialog = false },
                title = { Text("Konfirmasi Kode OTP") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Kode OTP simulasi telah dikirim ke nomor WhatsApp $whatsapp. Masukkan kode 6-digit di bawah ini:")
                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = { enteredOtp = it.take(6) },
                            placeholder = { Text("Masukkan 6 Digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp-input")
                        )
                        otpError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredOtp == generatedOtp) {
                                isOtpVerified = true
                                showOtpDialog = false
                                Toast.makeText(context, "WhatsApp Berhasil Diverifikasi!", Toast.LENGTH_SHORT).show()
                            } else {
                                otpError = "Kode OTP tidak cocok! Silakan cek kembali."
                            }
                        },
                        modifier = Modifier.testTag("btn-verify-otp")
                    ) {
                        Text("Verifikasi")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOtpDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    } else {
        // Render POS System selection cards catalog
        val options = remember {
            listOf(
                SystemOption(
                    "BMP",
                    "BMP (Bahan Baku & Manufaktur)",
                    "Cocok untuk manufaktur & grosir. Kelola bahan baku, invoice piutang, serta penggajian karyawan.",
                    Icons.Outlined.AccountBalance,
                    Color(0xFF3B82F6)
                ),
                SystemOption(
                    "FNB",
                    "FNB (Food & Beverage / Kasir)",
                    "Desain kasir pintar cepat saji untuk makanan & minuman. Input menu cepat & cetak struk thermal.",
                    Icons.Outlined.Storefront,
                    Color(0xFFF59E0B)
                ),
                SystemOption(
                    "RENTAL",
                    "RENTAL (Sewa Mobil & Motor)",
                    "Kelola ketersediaan armada kendaraan, pencatatan durasi sewa harian, denda keterlambatan, & struk sewa.",
                    Icons.Outlined.DirectionsCar,
                    Color(0xFF10B981)
                ),
                SystemOption(
                    "LAUNDRY",
                    "LAUNDRY (Service & Cuci Kiloan)",
                    "Kelola jasa cuci kiloan/satuan, pantau status pengerjaan (proses, selesai, diambil), & struk laundry.",
                    Icons.Outlined.LocalLaundryService,
                    Color(0xFF8B5CF6)
                )
            )
        }

        var selectedOption by remember { mutableStateOf<SystemOption?>(null) }
        var showConfirmDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            PosBahTopBar(
                title = "Pilih Sistem POS Anda",
                subtitle = "Mulai dengan 1 dari 4 sistem spesialis",
                actions = {
                    if (isPremium) {
                        // Allow premium user to go back to profile setup
                        TextButton(onClick = { currentFlowStep = 1 }) { Text("Profil") }
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = onLogout) { Text("Keluar") }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Silakan pilih sistem spesialis yang ingin Anda gunakan. Pilihan ini bersifat permanen dan tidak bisa diganti lagi.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(options) { option ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            border = BorderStroke(
                                1.5.dp,
                                if (selectedOption?.code == option.code) option.color
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = option }
                                .testTag("system-card-${option.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = option.color.copy(alpha = 0.15f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            option.icon,
                                            contentDescription = null,
                                            tint = option.color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        option.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        option.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedOption != null) {
                            showConfirmDialog = true
                        }
                    },
                    enabled = selectedOption != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn-confirm-system-choice")
                ) {
                    Text("Konfirmasi & Kunci Sistem", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showConfirmDialog && selectedOption != null) {
            val option = selectedOption!!
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Kunci Pilihan Sistem POS?") },
                text = {
                    Text(
                        "Anda memilih sistem: ${option.title}.\n\nSekali Anda memilih sistem ini, Anda tidak dapat mengganti atau memilih sistem lain lagi. Apakah Anda yakin?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.lockInSystem(option.code, onSelected)
                        },
                        modifier = Modifier.testTag("btn-confirm-lock-in")
                    ) { Text("Ya, Kunci Sekarang") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
