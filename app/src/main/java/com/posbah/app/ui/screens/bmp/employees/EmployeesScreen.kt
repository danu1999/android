package com.posbah.app.ui.screens.bmp.employees

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpEmployeeEntity
import com.posbah.app.data.local.entities.BmpPayrollEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpEmployeeRepository
import com.posbah.app.data.repository.EmployeeRepository
import com.posbah.app.data.repository.OutletRepository
import com.posbah.app.data.repository.OutletData
import com.posbah.app.data.repository.EmployeeData
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.posbah.app.data.local.PosBahDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class EmployeesViewModel @Inject constructor(
    private val repo: BmpEmployeeRepository,
    private val posEmployeeRepo: EmployeeRepository,
    private val outletRepo: OutletRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val tenantId = authRepository.activeTenantId().orEmpty()
    val employees = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpEmployeeEntity>())
    val payrolls = repo.observePayrolls(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpPayrollEntity>())
    val outlets = outletRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<OutletData>())
    val posEmployees = posEmployeeRepo.observeForTenant(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<EmployeeData>())

    init {
        viewModelScope.launch {
            try { repo.refresh() } catch (_: Exception) {}
            try { repo.refreshPayrolls() } catch (_: Exception) {}
            try { posEmployeeRepo.refresh() } catch (_: Exception) {}
        }
    }

    val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun dismissError() { error.value = null }

    fun upsert(
        e: BmpEmployeeEntity,
        addToPos: Boolean,
        posEmail: String,
        posPin: String,
        posRole: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val currentEmployees = employees.value.filter { it.isActive }
        
        if (e.outletId != null) {
            val currentCount = currentEmployees.count { it.outletId == e.outletId && it.id != e.id }
            if (currentCount >= 10) {
                error.value = "Gagal: Outlet tujuan sudah mencapai batas maksimal 10 karyawan."
                return@launch
            }
        }

        if (e.id != 0L) {
            val oldRecord = currentEmployees.firstOrNull { it.id == e.id }
            if (oldRecord != null && oldRecord.outletId != null && oldRecord.outletId != e.outletId) {
                val oldOutletCount = currentEmployees.count { it.outletId == oldRecord.outletId }
                if (oldOutletCount <= 1) {
                    val oldOutletName = outlets.value.find { it.id == oldRecord.outletId }?.name ?: "Outlet Lain"
                    error.value = "Gagal: ${e.name} adalah karyawan terakhir di $oldOutletName."
                    return@launch
                }
            }
        }

        var finalEmployeeId = e.employeeId
        val email = authRepository.activeUserEmail()

        if (addToPos) {
            if (posEmail.isBlank()) {
                error.value = "Gagal: Email POS tidak boleh kosong."
                return@launch
            }
            val hashedPassword = if (posPin.isNotBlank()) com.posbah.app.security.PinHasher.hash(posPin) else ""

            if (e.employeeId != null) {
                val existing = posEmployeeRepo.getById(e.employeeId)
                if (existing != null) {
                    val updatedPos = existing.copy(
                        name = e.name,
                        email = posEmail,
                        role = posRole,
                        pinHash = if (posPin.isNotBlank()) hashedPassword else existing.pinHash,
                        outletId = e.outletId,
                        salary = e.salaryAmount,
                        updatedAt = System.currentTimeMillis()
                    )
                    posEmployeeRepo.update(updatedPos)
                }
            } else {
                if (posPin.isBlank()) {
                    error.value = "Gagal: Password POS wajib diisi untuk karyawan baru."
                    return@launch
                }
                val alreadyUsed = posEmployeeRepo.list().find { it.email?.lowercase()?.trim() == posEmail.lowercase().trim() }
                if (alreadyUsed != null && alreadyUsed.tenantId == tenantId) {
                    error.value = "Gagal: Email POS sudah terdaftar."
                    return@launch
                }
                val newPos = com.posbah.app.data.repository.EmployeeData(
                    tenantId = tenantId,
                    outletId = e.outletId,
                    name = e.name,
                    email = posEmail,
                    role = posRole,
                    pinHash = hashedPassword,
                    salary = e.salaryAmount,
                    isActive = true
                )
                val newId = posEmployeeRepo.insert(newPos)
                finalEmployeeId = newId
            }
        } else {
            finalEmployeeId = null
        }

        val toSave = e.copy(employeeId = finalEmployeeId)
        val generatedId = repo.upsert(toSave)
        val targetId = if (e.id != 0L) e.id else generatedId
    }

    fun softDelete(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val currentEmployees = employees.value.filter { it.isActive }
        val emp = currentEmployees.firstOrNull { it.id == id }
        if (emp != null && emp.outletId != null) {
            val currentCount = currentEmployees.count { it.outletId == emp.outletId }
            if (currentCount <= 1) {
                val outletName = outlets.value.find { it.id == emp.outletId }?.name ?: "Outlet"
                error.value = "Gagal: $outletName harus memiliki minimal 1 karyawan."
                return@launch
            }
        }

        repo.softDelete(id)
    }

    fun payEmployee(emp: BmpEmployeeEntity, amount: Double, attendance: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (amount <= 0) return@launch
        val generatedPayrollId = repo.insertPayroll(
            BmpPayrollEntity(
                tenantId = tenantId,
                employeeId = emp.id,
                paymentDate = System.currentTimeMillis(),
                amount = amount,
                attendanceCount = attendance,
                dailyRate = if (attendance > 0) amount / attendance else 0.0
            )
        )
    }

    fun editPayroll(payroll: BmpPayrollEntity) = viewModelScope.launch(Dispatchers.IO) {
        repo.updatePayroll(payroll)
    }

    fun deletePayroll(id: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deletePayroll(id)
    }
}

@Composable
fun EmployeesScreen(
    onBack: () -> Unit,
    viewModel: EmployeesViewModel = hiltViewModel()
) {
    val list by viewModel.employees.collectAsState()
    val outlets by viewModel.outlets.collectAsState()
    var formEdit by remember { mutableStateOf<BmpEmployeeEntity?>(null) }
    var payTarget by remember { mutableStateOf<BmpEmployeeEntity?>(null) }

    val context = LocalContext.current
    val errorState by viewModel.error.collectAsState()
    LaunchedEffect(errorState) {
        errorState?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Karyawan", subtitle = "${list.size} aktif", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    formEdit = BmpEmployeeEntity(tenantId = viewModel.tenantId, name = "", salaryAmount = 0.0)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-add-employee")
            ) { Icon(Icons.Outlined.Add, contentDescription = null) }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (list.isEmpty()) {
                EmptyState(
                    "Belum ada karyawan",
                    "Tambah karyawan untuk mulai mengelola data gaji",
                    "+ Tambah Karyawan",
                    onAction = {
                        formEdit = BmpEmployeeEntity(tenantId = viewModel.tenantId, name = "", salaryAmount = 0.0)
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.id }) { e ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { formEdit = e }
                                .testTag("emp-${e.id}")
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Badge, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = MaterialTheme.typography.titleMedium)
                                    val pinText = if (!e.fingerprintPIN.isNullOrBlank()) "PIN: ${e.fingerprintPIN}" else "PIN Belum Set"
                                    val outletName = outlets.firstOrNull { it.id == e.outletId }?.name ?: "Seluruh Outlet"
                                    Text(
                                        "${e.position ?: "Karyawan"} • ${Formatters.rupiah(e.salaryAmount)} • $pinText • Outlet: $outletName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { payTarget = e }, modifier = Modifier.testTag("pay-${e.id}")) {
                                    Text("Bayar Gaji")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    formEdit?.let { editing ->
        var name by remember { mutableStateOf(editing.name) }
        var position by remember { mutableStateOf(editing.position.orEmpty()) }
        var pin by remember { mutableStateOf(editing.fingerprintPIN.orEmpty()) }
        var salary by remember { mutableStateOf(if (editing.salaryAmount == 0.0) "" else editing.salaryAmount.toLong().toString()) }
        var selectedEmployeeType by remember { mutableStateOf(editing.employeeType) }
        var selectedOutletId by remember { mutableStateOf(editing.outletId) }
        var selectedOutletName by remember {
            mutableStateOf(outlets.firstOrNull { it.id == editing.outletId }?.name ?: "Seluruh Outlet")
        }
        var outletDropdownExpanded by remember { mutableStateOf(false) }

        val posEmployees by viewModel.posEmployees.collectAsState()
        var addToPos by remember { mutableStateOf(editing.employeeId != null) }
        val linkedPosEmp = remember(editing.employeeId, posEmployees) {
            posEmployees.firstOrNull { it.id == editing.employeeId }
        }
        var posEmail by remember { mutableStateOf(linkedPosEmp?.email.orEmpty()) }
        var posPin by remember { mutableStateOf("") }
        var posRole by remember { mutableStateOf(linkedPosEmp?.role ?: "KASIR") }
        var posRoleDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { formEdit = null },
            title = { Text(if (editing.id == 0L) "Karyawan Baru" else "Edit Karyawan") },
            text = {
                val scrollState = androidx.compose.foundation.rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama / ID Karyawan") },
                        modifier = Modifier.fillMaxWidth().testTag("emp-name"))
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("Jabatan") },
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = pin, onValueChange = { pin = it.filter { c -> c.isDigit() } },
                        label = { Text("PIN Masuk (Numerik)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = salary, onValueChange = { salary = it },
                        label = { Text("Gaji Pokok (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("emp-salary")
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Kategori Karyawan:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.padding(top = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "OPERATING_EXPENSE" to "Non-Produksi",
                            "DIRECT_LABOR" to "Buruh Langsung",
                            "INDIRECT_LABOR" to "Overhead"
                        ).forEach { (valType, label) ->
                            val selected = selectedEmployeeType == valType
                            androidx.compose.material3.OutlinedButton(
                                onClick = { selectedEmployeeType = valType },
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedOutletName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Penugasan Outlet") },
                            trailingIcon = {
                                IconButton(onClick = { outletDropdownExpanded = true }) {
                                    Text("▾", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { outletDropdownExpanded = true }
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
                            outlets.forEach { outlet ->
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
                    Spacer(Modifier.size(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { addToPos = !addToPos }.padding(vertical = 4.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = addToPos,
                            onCheckedChange = { addToPos = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Daftarkan ke Karyawan Outlet (POS)", fontSize = 14.sp)
                    }
                    if (addToPos) {
                        Spacer(Modifier.size(8.dp))
                        OutlinedTextField(
                            value = posEmail,
                            onValueChange = { posEmail = it },
                            label = { Text("Email POS (G-Mail)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.size(8.dp))
                        OutlinedTextField(
                            value = posPin,
                            onValueChange = { posPin = it },
                            label = { Text(if (editing.employeeId != null) "Ganti Password POS (Alphanumeric, Opsional)" else "Password POS Baru (Alphanumeric)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.size(8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (posRole == "ADMIN") "Administrator" else "Kasir",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Role POS") },
                                trailingIcon = {
                                    IconButton(onClick = { posRoleDropdownExpanded = true }) {
                                        Text("▾", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { posRoleDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = posRoleDropdownExpanded,
                                onDismissRequest = { posRoleDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Kasir") },
                                    onClick = {
                                        posRole = "KASIR"
                                        posRoleDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Administrator") },
                                    onClick = {
                                        posRole = "ADMIN"
                                        posRoleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.upsert(
                                editing.copy(
                                    name = name,
                                    position = position.ifBlank { null },
                                    salaryAmount = salary.replace(",", "").toDoubleOrNull() ?: 0.0,
                                    employeeType = selectedEmployeeType,
                                    fingerprintPIN = pin.ifBlank { null },
                                    outletId = selectedOutletId
                                ),
                                addToPos = addToPos,
                                posEmail = posEmail,
                                posPin = posPin,
                                posRole = posRole
                            )
                            formEdit = null
                        }
                    },
                    modifier = Modifier.testTag("btn-save-emp")
                ) { Text("Simpan") }
            },
            dismissButton = {
                Row {
                    if (editing.id != 0L) {
                        TextButton(onClick = {
                            viewModel.softDelete(editing.id); formEdit = null
                        }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { formEdit = null }) { Text("Batal") }
                }
            }
        )
    }

    payTarget?.let { target ->
        var amt by remember { mutableStateOf(target.salaryAmount.toLong().toString()) }
        var att by remember { mutableStateOf("26") }
        AlertDialog(
            onDismissRequest = { payTarget = null },
            title = { Text("Bayar Gaji: ${target.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = amt, onValueChange = { amt = it },
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("pay-amount")
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = att, onValueChange = { att = it.filter { c -> c.isDigit() } },
                        label = { Text("Hari kehadiran") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("pay-att")
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.payEmployee(target, amt.replace(",", "").toDoubleOrNull() ?: 0.0, att.toIntOrNull() ?: 0)
                    payTarget = null
                }, modifier = Modifier.testTag("btn-confirm-pay")) { Text("Bayar") }
            },
            dismissButton = { TextButton(onClick = { payTarget = null }) { Text("Batal") } }
        )
    }
}

@Composable
fun PayrollScreen(
    onBack: () -> Unit,
    viewModel: EmployeesViewModel = hiltViewModel()
) {
    val payrolls by viewModel.payrolls.collectAsState()
    val emps by viewModel.employees.collectAsState()
    var editTarget by remember { mutableStateOf<BmpPayrollEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Penggajian", subtitle = "${payrolls.size} pencatatan", onBack = onBack) }
    ) { padding ->
        if (payrolls.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                EmptyState("Belum ada catatan gaji", "Bayar gaji dari menu Karyawan")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(payrolls, key = { it.id }) { p ->
                    val emp = emps.firstOrNull { it.id == p.employeeId }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payroll-${p.id}")
                            .clickable { editTarget = p }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(emp?.name ?: "Karyawan #${p.employeeId}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${Formatters.dateLong(p.paymentDate)} • ${p.attendanceCount} hari",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                Formatters.rupiah(p.amount),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (editTarget != null) {
        val p = editTarget!!
        val empName = emps.find { it.id == p.employeeId }?.name ?: "Karyawan"
        var daysText by remember { mutableStateOf(p.attendanceCount.toString()) }
        var amountText by remember { mutableStateOf(p.amount.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Koreksi Gaji: $empName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it },
                        label = { Text("Hari Kerja (Absensi)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Total Nominal Gaji (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val days = daysText.toIntOrNull() ?: 0
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            val rate = if (days > 0) amt / days else 0.0
                            viewModel.editPayroll(
                                p.copy(
                                    attendanceCount = days,
                                    amount = amt,
                                    dailyRate = rate
                                )
                            )
                        }
                        editTarget = null
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.deletePayroll(p.id)
                            editTarget = null
                        }
                    ) { Text("Hapus") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { editTarget = null }) { Text("Batal") }
                }
            }
        )
    }
}
