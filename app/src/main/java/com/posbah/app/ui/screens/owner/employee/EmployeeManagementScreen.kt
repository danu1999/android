package com.posbah.app.ui.screens.owner.employee

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    onBack: () -> Unit,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("KASIR") }
    var salaryInput by remember { mutableStateOf("") }
    var payPeriodInput by remember { mutableStateOf("MONTHLY") }
    var selectedOutletId by remember { mutableStateOf<Long?>(null) }
    var selectedOutletName by remember { mutableStateOf("Seluruh Outlet") }

    var otpInput by remember { mutableStateOf("") }

    var showPinChangeDialog by remember { mutableStateOf(false) }
    var activeEmployeeForPinChange by remember { mutableStateOf<Employee?>(null) }
    var newPinInput by remember { mutableStateOf("") }

    var outletDropdownExpanded by remember { mutableStateOf(false) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var periodDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            PosBahTopBar(
                title = "Manajemen Karyawan",
                subtitle = "Kelola Data & Gaji Karyawan",
                onBack = onBack,
                actions = {
                    if (state.isOwner) {
                        IconButton(onClick = {
                            nameInput = ""
                            emailInput = ""
                            phoneInput = ""
                            pinInput = ""
                            roleInput = "KASIR"
                            salaryInput = ""
                            payPeriodInput = "MONTHLY"
                            selectedOutletId = null
                            selectedOutletName = "Seluruh Outlet"
                            showAddDialog = true
                        }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Tambah Karyawan")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (!state.isOwner) {
            // Unauthorised access protection screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Terkunci",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Akses Pemilik Sahaja",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hanya akun Owner yang diperbolehkan masuk ke halaman manajemen karyawan dan melakukan pembayaran gaji.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Kembali")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.employees.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Belum Ada Karyawan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Tambahkan karyawan baru dengan menekan tombol plus di kanan atas.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(state.employees) { emp ->
                        EmployeeCard(
                            employee = emp,
                            onPaySalary = { viewModel.paySalary(emp) },
                            onChangePin = {
                                activeEmployeeForPinChange = emp
                                newPinInput = ""
                                showPinChangeDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Employee dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Karyawan Baru") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Karyawan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email (G-Mail)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Nomor Telepon / WhatsApp") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN / Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (roleInput == "ADMIN") "Administrator" else "Kasir",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = { Text("▾", modifier = Modifier.clickable { roleDropdownExpanded = true }) },
                            modifier = Modifier.fillMaxWidth().clickable { roleDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Kasir") },
                                onClick = {
                                    roleInput = "KASIR"
                                    roleDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Administrator") },
                                onClick = {
                                    roleInput = "ADMIN"
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }

                    // Outlet selector dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedOutletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Penempatan Outlet") },
                            trailingIcon = { Text("▾", modifier = Modifier.clickable { outletDropdownExpanded = true }) },
                            modifier = Modifier.fillMaxWidth().clickable { outletDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = outletDropdownExpanded,
                            onDismissRequest = { outletDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Seluruh Outlet") },
                                onClick = {
                                    selectedOutletId = null
                                    selectedOutletName = "Seluruh Outlet"
                                    outletDropdownExpanded = false
                                }
                            )
                            state.outlets.forEach { outlet ->
                                DropdownMenuItem(
                                    text = { Text(outlet.name) },
                                    onClick = {
                                        selectedOutletId = outlet.id
                                        selectedOutletName = outlet.name
                                        outletDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Salary Input
                    OutlinedTextField(
                        value = salaryInput,
                        onValueChange = { salaryInput = it },
                        label = { Text("Gaji Pokok (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Pay period dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val periodLabel = when (payPeriodInput) {
                            "WEEKLY" -> "Per Minggu"
                            "BI_WEEKLY" -> "Per 2 Minggu"
                            else -> "Per Bulan"
                        }
                        OutlinedTextField(
                            value = periodLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Siklus Pembayaran") },
                            trailingIcon = { Text("▾", modifier = Modifier.clickable { periodDropdownExpanded = true }) },
                            modifier = Modifier.fillMaxWidth().clickable { periodDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = periodDropdownExpanded,
                            onDismissRequest = { periodDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Per Minggu") },
                                onClick = {
                                    payPeriodInput = "WEEKLY"
                                    periodDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Per 2 Minggu") },
                                onClick = {
                                    payPeriodInput = "BI_WEEKLY"
                                    periodDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Per Bulan") },
                                onClick = {
                                    payPeriodInput = "MONTHLY"
                                    periodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sal = salaryInput.toDoubleOrNull() ?: 0.0
                        viewModel.startAddEmployee(
                            name = nameInput,
                            email = emailInput,
                            phone = phoneInput,
                            pin = pinInput,
                            role = roleInput,
                            salary = sal,
                            payPeriod = payPeriodInput,
                            outletId = selectedOutletId
                        )
                        showAddDialog = false
                    }
                ) {
                    Text("Kirim OTP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // OTP Code Verification Dialog
    if (state.emailVerificationOtp != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelAddEmployee() },
            title = { Text("Simulasi Verifikasi Email") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Sistem mengirim simulasi tautan konfirmasi ke email: ${state.pendingEmployee?.email}",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Styling code mockup
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("KODE OTP SIMULASI GMAIL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                state.emailVerificationOtp ?: "",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text("Masukkan Kode OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyOtpAndInsert(otpInput)
                        otpInput = ""
                    }
                ) {
                    Text("Konfirmasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelAddEmployee() }) {
                    Text("Batal")
                }
            }
        )
    }

    // PIN change dialog
    if (showPinChangeDialog && activeEmployeeForPinChange != null) {
        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = { Text("Ganti PIN: ${activeEmployeeForPinChange?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Masukkan PIN numerik baru untuk karyawan ini.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("PIN Baru") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changeEmployeePin(activeEmployeeForPinChange!!.id, newPinInput)
                        showPinChangeDialog = false
                    }
                ) {
                    Text("Simpan PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun EmployeeCard(
    employee: Employee,
    onPaySalary: () -> Unit,
    onChangePin: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Karyawan
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar Placeholder
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                employee.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                employee.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (employee.role == "ADMIN") Color(0xFFFFF3E0) else Color(0xFFE3F2FD),
                                contentColor = if (employee.role == "ADMIN") Color(0xFFE65100) else Color(0xFF0D47A1)
                            ) {
                                Text(
                                    text = if (employee.role == "ADMIN") "ADMIN" else "KASIR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            employee.email ?: "-",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        if (!employee.phone.isNullOrBlank()) {
                            Text(
                                "No. Telp: ${employee.phone}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // PIN Change Icon
                IconButton(onClick = onChangePin) {
                    Icon(
                        Icons.Outlined.Key,
                        contentDescription = "Ganti PIN",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rincian Gaji & Pembayaran
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val periodLabel = when (employee.payPeriod) {
                            "WEEKLY" -> "Minggu"
                            "BI_WEEKLY" -> "2 Minggu"
                            else -> "Bulan"
                        }
                        Text("Gaji Pokok / $periodLabel", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            Formatters.rupiah(employee.salary),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Terakhir Dibayar", fontSize = 10.sp, color = Color.Gray)
                        if (employee.lastPaidAt != null) {
                            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            Text(
                                sdf.format(Date(employee.lastPaidAt)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            Text(
                                "Belum pernah dibayar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Bayar Gaji
            Button(
                onClick = onPaySalary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bayar Gaji Karyawan", fontWeight = FontWeight.Bold)
            }
        }
    }
}
