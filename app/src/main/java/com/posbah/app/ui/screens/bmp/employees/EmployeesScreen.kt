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

@HiltViewModel
class EmployeesViewModel @Inject constructor(
    private val repo: BmpEmployeeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val tenantId = authRepository.activeTenantId().orEmpty()
    val employees = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val payrolls = repo.observePayrolls(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun upsert(e: BmpEmployeeEntity) = viewModelScope.launch { repo.upsert(e) }
    fun softDelete(id: Long) = viewModelScope.launch { repo.softDelete(id) }

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
    }
}

@Composable
fun EmployeesScreen(
    onBack: () -> Unit,
    viewModel: EmployeesViewModel = hiltViewModel()
) {
    val list by viewModel.employees.collectAsState()
    var formEdit by remember { mutableStateOf<BmpEmployeeEntity?>(null) }
    var payTarget by remember { mutableStateOf<BmpEmployeeEntity?>(null) }

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
                                    Text(
                                        "${e.position ?: "Karyawan"} • ${Formatters.rupiah(e.salaryAmount)} • $pinText",
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
                                fingerprintPIN = pin.ifBlank { null }
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
