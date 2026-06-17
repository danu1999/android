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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpEmployeeEntity
import com.posbah.app.data.local.entities.BmpPayrollEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpEmployeeRepository
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
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val tenantId = authRepository.activeTenantId().orEmpty()
    val employees = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val payrolls = repo.observePayrolls(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val outlets = db.outletDao().observeForTenant(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun dismissError() { error.value = null }

    fun upsert(e: BmpEmployeeEntity) = viewModelScope.launch {
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
                    val oldOutletName = db.outletDao().getById(oldRecord.outletId)?.name ?: "Outlet Lain"
                    error.value = "Gagal: ${e.name} adalah karyawan terakhir di $oldOutletName."
                    return@launch
                }
            }
        }

        repo.upsert(e)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun softDelete(id: Long) = viewModelScope.launch {
        val currentEmployees = employees.value.filter { it.isActive }
        val emp = currentEmployees.firstOrNull { it.id == id }
        if (emp != null && emp.outletId != null) {
            val currentCount = currentEmployees.count { it.outletId == emp.outletId }
            if (currentCount <= 1) {
                val outletName = db.outletDao().getById(emp.outletId)?.name ?: "Outlet"
                error.value = "Gagal: $outletName harus memiliki minimal 1 karyawan."
                return@launch
            }
        }

        repo.softDelete(id)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun payEmployee(emp: BmpEmployeeEntity, amount: Double, attendance: Int) = viewModelScope.launch {
        if (amount <= 0) return@launch
        repo.insertPayroll(
            BmpPayrollEntity(
                tenantId = tenantId,
                employeeId = emp.id,
                paymentDate = System.currentTimeMillis(),
                amount = amount,
                attendanceCount = attendance,
                dailyRate = if (attendance > 0) amount / attendance else 0.0
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
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
        var selectedOutletId by remember { mutableStateOf(editing.outletId) }
        var selectedOutletName by remember {
            mutableStateOf(outlets.firstOrNull { it.id == editing.outletId }?.name ?: "Seluruh Outlet")
        }
        var outletDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { formEdit = null },
            title = { Text(if (editing.id == 0L) "Karyawan Baru" else "Edit Karyawan") },
            text = {
                Column {
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.upsert(editing.copy(
                                name = name,
                                position = position.ifBlank { null },
                                salaryAmount = salary.replace(",", "").toDoubleOrNull() ?: 0.0,
                                fingerprintPIN = pin.ifBlank { null },
                                outletId = selectedOutletId
                            ))
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
                        modifier = Modifier.fillMaxWidth().testTag("payroll-${p.id}")
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
}
