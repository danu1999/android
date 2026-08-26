package com.posbah.app.ui.screens.bmp.employees

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.posbah.app.data.local.entities.BmpEmployeeEntity
import com.posbah.app.data.local.entities.BmpJobApplicantEntity
import com.posbah.app.data.local.entities.BmpJobInvitationEntity
import com.posbah.app.data.local.entities.BmpWarningLetterEntity
import com.posbah.app.data.local.entities.parseWorkersAttendance
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpEmployeeRepository
import com.posbah.app.data.repository.BmpProductionLogRepository
import com.posbah.app.data.repository.BmpRecruitmentRepository
import com.posbah.app.data.repository.BmpSettingsRepository
import com.posbah.app.data.repository.BmpWarningLetterData
import com.posbah.app.data.repository.BmpWarningLetterRepository
import com.posbah.app.data.repository.EmployeeData
import com.posbah.app.data.repository.EmployeeRepository
import com.posbah.app.data.repository.OutletData
import com.posbah.app.data.repository.OutletRepository
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class EmployeesViewModel @Inject constructor(
    private val repo: BmpEmployeeRepository,
    private val payrollRepo: com.posbah.app.data.repository.BmpPayrollRepository,
    private val posEmployeeRepo: EmployeeRepository,
    private val outletRepo: OutletRepository,
    private val authRepository: AuthRepository,
    private val productionLogRepo: BmpProductionLogRepository,
    private val settingsRepo: BmpSettingsRepository,
    private val recruitmentRepo: BmpRecruitmentRepository,
    private val warningLetterRepo: BmpWarningLetterRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val tenantId = authRepository.activeTenantId().orEmpty()
    val employees = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpEmployeeEntity>())
    val outlets = outletRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<OutletData>())
    val posEmployees = posEmployeeRepo.observeForTenant(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<EmployeeData>())
    val settings = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val invitations = recruitmentRepo.observeInvitations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpJobInvitationEntity>())
    val applicants = recruitmentRepo.observeApplicants().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpJobApplicantEntity>())
    val warningLetters = warningLetterRepo.observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpWarningLetterEntity>())

    init {
        viewModelScope.launch {
            try { repo.refresh() } catch (_: Exception) {}
            try { payrollRepo.refresh() } catch (_: Exception) {}
            try { posEmployeeRepo.refresh() } catch (_: Exception) {}
            try { settingsRepo.refresh() } catch (_: Exception) {}
            try { recruitmentRepo.refresh() } catch (_: Exception) {}
            try { warningLetterRepo.refresh() } catch (_: Exception) {}
        }
    }

    val error = MutableStateFlow<String?>(null)
    fun dismissError() { error.value = null }

    fun saveAttendanceSettings(mode: String, ip: String, port: String) = viewModelScope.launch(Dispatchers.IO) {
        val current = settings.value ?: com.posbah.app.data.local.entities.BmpSettingsEntity(tenantId = tenantId, clientName = "CV. BAHTERA MULYA PLASTIK")
        val dataToSave = com.posbah.app.data.repository.BmpSettingsData(
            id = current.id,
            tenantId = current.tenantId,
            companyName = current.clientName,
            address = current.addressLine1,
            phone = current.phoneNumber,
            email = current.emailAddress,
            npwp = current.taxNumber,
            logoUrl = current.clientLogo,
            listrikBulanan = current.listrikBulanan,
            jumlahMesin = current.jumlahMesin,
            jumlahKaryawan = current.jumlahKaryawan,
            gajiHarian = current.gajiHarian,
            hariKerjaSebulan = current.hariKerjaSebulan,
            biayaKarungPer1000 = current.biayaKarungPer1000,
            hoursPerDay = current.hoursPerDay,
            attendanceMode = mode,
            fingerprintIp = ip,
            fingerprintPort = port,
            updatedAt = System.currentTimeMillis()
        )
        settingsRepo.save(dataToSave)
    }

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
        val res = repo.upsert(toSave)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
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

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) {
        repo.clearAllBmpEmployees()
        try { repo.refresh() } catch (_: Exception) {}
    }

    fun countAttendanceFromProduction(bmpEmployeeId: Long): Int {
        val cal = Calendar.getInstance()
        val startOfMonth = cal.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var count = 0
        viewModelScope.launch(Dispatchers.IO) {
            val logs = productionLogRepo.getCachedLogs(tenantId)
            logs.filter { it.productionDate >= startOfMonth }.forEach { log ->
                val attendees = parseWorkersAttendance(log.workersAttendance)
                if (attendees.any { it.employeeId == bmpEmployeeId }) count++
            }
        }
        return count
    }

    fun paySalary(
        target: BmpEmployeeEntity,
        daysCount: Int,
        totalAmount: Double,
        note: String,
        paymentMethod: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val res = payrollRepo.paySalary(
            employeeId = target.id,
            employeeName = target.name,
            amount = totalAmount,
            attendanceCount = daysCount,
            dailyRate = target.salaryAmount,
            description = note,
            paymentMethod = paymentMethod
        )
        withContext(Dispatchers.Main) {
            if (res is com.posbah.app.data.repository.OnlineWriteResult.Success) {
                onSuccess()
            } else if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                onError(res.message)
            }
        }
    }

    // ── Recruitment Handlers ──────────────────────────────────────────────────

    fun createInvitation(
        candidateName: String,
        candidatePhone: String,
        positionTarget: String,
        onResult: (token: String, formUrl: String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val res = recruitmentRepo.createInvitation(candidateName, candidatePhone, positionTarget)
        res.onSuccess { (token, url) ->
            withContext(Dispatchers.Main) {
                onResult(token, url)
            }
        }.onFailure { ex ->
            error.value = ex.message ?: "Gagal membuat link undangan"
        }
    }

    fun deleteInvitation(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val res = recruitmentRepo.deleteInvitation(id)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
    }

    fun deleteApplicant(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        val res = recruitmentRepo.deleteApplicant(id)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
    }

    fun acceptApplicant(
        applicantId: Long,
        salaryOffer: Double,
        position: String,
        role: String,
        onComplete: () -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val res = recruitmentRepo.acceptApplicant(applicantId, salaryOffer, position, role)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Success) {
            try { repo.refresh() } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                onComplete()
            }
        } else if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
    }

    fun rejectApplicant(applicantId: Long, reason: String) = viewModelScope.launch(Dispatchers.IO) {
        val res = recruitmentRepo.rejectApplicant(applicantId, reason)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
    }

    fun incrementTrainingDay(e: BmpEmployeeEntity) = viewModelScope.launch(Dispatchers.IO) {
        val newDays = e.trainingDaysCompleted + 1
        val updated = e.copy(trainingDaysCompleted = newDays)
        val res = repo.upsert(updated)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
    }

    fun updateTrainingDays(e: BmpEmployeeEntity, completed: Int, target: Int) = viewModelScope.launch(Dispatchers.IO) {
        val updated = e.copy(trainingDaysCompleted = completed, trainingTargetDays = target)
        val res = repo.upsert(updated)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
    }

    fun graduateTraining(e: BmpEmployeeEntity, regularSalary: Double = 70000.0) = viewModelScope.launch(Dispatchers.IO) {
        val updated = e.copy(isTraining = false, salaryAmount = regularSalary)
        val res = repo.upsert(updated)
        if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            error.value = res.message
        }
    }

    fun refreshRecruitment() = viewModelScope.launch(Dispatchers.IO) {
        try { recruitmentRepo.refresh() } catch (_: Exception) {}
    }

    // ── Warning Letters Handlers ──────────────────────────────────────────────

    fun createWarningLetter(
        letter: BmpWarningLetterData,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val res = warningLetterRepo.create(letter)
        withContext(Dispatchers.Main) {
            if (res is com.posbah.app.data.repository.OnlineWriteResult.Success) {
                try { repo.refresh() } catch (_: Exception) {}
                onSuccess()
            } else if (res is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                onError(res.message)
            }
        }
    }

    fun deleteWarningLetter(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        warningLetterRepo.delete(id)
    }
}

@Composable
fun EmployeesScreen(
    onBack: () -> Unit,
    viewModel: EmployeesViewModel = hiltViewModel()
) {
    val list by viewModel.employees.collectAsState()
    val outlets by viewModel.outlets.collectAsState()
    val invitations by viewModel.invitations.collectAsState()
    val applicants by viewModel.applicants.collectAsState()
    val warningLetters by viewModel.warningLetters.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val companyName = settings?.clientName?.takeIf { it.isNotBlank() } ?: "CV. Bahtera Plastik"
    val companyAddress = settings?.addressLine1?.takeIf { it.isNotBlank() } ?: "Sidoarjo, Jawa Timur"

    val pendingApplicants = remember(applicants) { applicants.filter { it.status == "PENDING" && !it.isDeleted } }

    var selectedTab by remember { mutableStateOf(0) }
    var formEdit by remember { mutableStateOf<BmpEmployeeEntity?>(null) }
    var paySalaryTarget by remember { mutableStateOf<BmpEmployeeEntity?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    var showCreateInviteDialog by remember { mutableStateOf(false) }
    var acceptApplicantTarget by remember { mutableStateOf<BmpJobApplicantEntity?>(null) }
    var rejectApplicantTarget by remember { mutableStateOf<BmpJobApplicantEntity?>(null) }
    var previewImageTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editTrainingTarget by remember { mutableStateOf<BmpEmployeeEntity?>(null) }
    var graduateConfirmTarget by remember { mutableStateOf<BmpEmployeeEntity?>(null) }

    // Warning letters state
    var createWarningLetterTarget by remember { mutableStateOf<BmpEmployeeEntity?>(null) }
    var showCreateWarningLetterDialog by remember { mutableStateOf(false) }
    var selectedWarningLetterDetail by remember { mutableStateOf<BmpWarningLetterEntity?>(null) }

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
        topBar = {
            PosBahTopBar(
                title = "Karyawan & Kedisiplinan",
                subtitle = when (selectedTab) {
                    0 -> "${list.size} karyawan aktif"
                    1 -> "${warningLetters.size} surat peringatan & PHK"
                    else -> "${pendingApplicants.size} pelamar baru"
                },
                onBack = onBack,
                actions = {
                    if (selectedTab == 0 && list.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirmDialog = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Bersihkan Semua Karyawan", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    FloatingActionButton(
                        onClick = {
                            formEdit = BmpEmployeeEntity(tenantId = viewModel.tenantId, name = "", salaryAmount = 0.0)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab-add-employee")
                    ) { Icon(Icons.Outlined.Add, contentDescription = null) }
                }
                1 -> {
                    FloatingActionButton(
                        onClick = {
                            createWarningLetterTarget = null
                            showCreateWarningLetterDialog = true
                        },
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Terbitkan Surat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = { showCreateInviteDialog = true },
                        containerColor = Color(0xFF0D47A1),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Link, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Buat Link Undangan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Karyawan", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "${list.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SP & PHK", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            if (warningLetters.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFD97706).copy(alpha = 0.15f),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "${warningLetters.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rekrutmen", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                            if (pendingApplicants.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "${pendingApplicants.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // TAB 0: KARYAWAN AKTIF
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        val activeSettings by viewModel.settings.collectAsState()
                        val currentMode = activeSettings?.attendanceMode ?: "SUPERVISOR"
                        val ip = activeSettings?.fingerprintIp.orEmpty()
                        val port = activeSettings?.fingerprintPort ?: "4370"

                        var showSettingsDialog by remember { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Metode Absensi Staf", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (currentMode == "SUPERVISOR") "Status: Supervisor (Manual via Log Produksi)"
                                    else "Status: Mesin Fingerprint Terkoneksi ($ip:$port)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.saveAttendanceSettings("SUPERVISOR", ip, port) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (currentMode == "SUPERVISOR") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                            contentColor = if (currentMode == "SUPERVISOR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (currentMode == "SUPERVISOR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Supervisor", fontSize = 11.sp, fontWeight = if (currentMode == "SUPERVISOR") FontWeight.Bold else FontWeight.Normal)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.saveAttendanceSettings("FINGERPRINT", ip, port)
                                            showSettingsDialog = true
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (currentMode == "FINGERPRINT") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                            contentColor = if (currentMode == "FINGERPRINT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (currentMode == "FINGERPRINT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Fingerprint", fontSize = 11.sp, fontWeight = if (currentMode == "FINGERPRINT") FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        if (showSettingsDialog) {
                            var ipInput by remember { mutableStateOf(ip) }
                            var portInput by remember { mutableStateOf(port) }
                            AlertDialog(
                                onDismissRequest = { showSettingsDialog = false },
                                title = { Text("Konfigurasi Mesin Fingerprint") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = ipInput,
                                            onValueChange = { ipInput = it },
                                            label = { Text("IP Address Mesin (misal 192.168.1.201)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = portInput,
                                            onValueChange = { portInput = it },
                                            label = { Text("Port (default 4370)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        viewModel.saveAttendanceSettings("FINGERPRINT", ipInput.trim(), portInput.trim())
                                        showSettingsDialog = false
                                    }) { Text("Simpan") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSettingsDialog = false }) { Text("Batal") }
                                }
                            )
                        }
                    }

                    if (list.isEmpty()) {
                        item {
                            EmptyState(
                                "Belum Ada Karyawan Manufaktur",
                                "Tambah karyawan untuk mulai mengelola data gaji atau bagikan Link Undangan Rekrutmen.",
                                "+ Tambah Karyawan",
                                onAction = {
                                    formEdit = BmpEmployeeEntity(tenantId = viewModel.tenantId, name = "", salaryAmount = 0.0)
                                }
                            )
                        }
                    } else {
                        items(list, key = { it.id }) { e ->
                            val attendanceCount = remember(e.id) { viewModel.countAttendanceFromProduction(e.id) }
                            val estimatedTotalGaji = attendanceCount * e.salaryAmount
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth().testTag("emp-${e.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { formEdit = e },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (e.isTraining) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            border = if (e.isTraining) BorderStroke(1.dp, Color(0xFFFDE68A)) else null,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (e.isTraining) Icons.Outlined.School else Icons.Outlined.Badge,
                                                    null,
                                                    tint = if (e.isTraining) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.size(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            val latestLetter = remember(warningLetters, e.id) {
                                                warningLetters.filter { it.employeeId == e.id }.maxByOrNull { it.issueDate }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(6.dp))
                                                if (e.isTraining) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFFEF3C7)
                                                    ) {
                                                        Text(
                                                            "TRAINING",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF92400E),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFECFDF5)
                                                    ) {
                                                        Text(
                                                            "REGULER",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF065F46),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                if (latestLetter != null) {
                                                    Spacer(Modifier.width(4.dp))
                                                    when (latestLetter.letterType) {
                                                        "TERMINATION" -> {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFFEE2E2),
                                                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                                            ) {
                                                                Text(
                                                                    "🛑 PHK",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF991B1B),
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        "SP_2" -> {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFFFE4E6),
                                                                border = BorderStroke(1.dp, Color(0xFFFDA4AF))
                                                            ) {
                                                                Text(
                                                                    "🚨 SP 2",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFFBE123C),
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        "SP_1" -> {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFFEF3C7),
                                                                border = BorderStroke(1.dp, Color(0xFFFCD34D))
                                                            ) {
                                                                Text(
                                                                    "⚠️ SP 1",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFFB45309),
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            val pinText = if (!e.fingerprintPIN.isNullOrBlank()) "PIN: ${e.fingerprintPIN}" else "PIN Belum Set"
                                            val outletName = outlets.firstOrNull { it.id == e.outletId }?.name
                                                ?: outlets.firstOrNull()?.name
                                                ?: "Outlet Utama"
                                            Text(
                                                "${e.position ?: "Karyawan"} • $pinText • Outlet: $outletName",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Training Progress Box (if in training period)
                                    if (e.isTraining) {
                                        Spacer(Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFFFBEB),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "⏳ Masa Training (Upah 50K/hr)",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309)
                                                    )
                                                    Text(
                                                        "${e.trainingDaysCompleted} / ${e.trainingTargetDays} Hari Masuk",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E)
                                                    )
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                val progress = if (e.trainingTargetDays > 0) (e.trainingDaysCompleted.toFloat() / e.trainingTargetDays).coerceIn(0f, 1f) else 0f
                                                LinearProgressIndicator(
                                                    progress = { progress },
                                                    color = Color(0xFFF59E0B),
                                                    trackColor = Color(0xFFFEF3C7),
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Button(
                                                        onClick = { viewModel.incrementTrainingDay(e) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(Modifier.width(2.dp))
                                                        Text("+1 Hari", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { editTrainingTarget = e },
                                                        border = BorderStroke(1.dp, Color(0xFFD97706)),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1.1f)
                                                    ) {
                                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(Modifier.width(2.dp))
                                                        Text("Edit Hari", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Button(
                                                        onClick = { graduateConfirmTarget = e },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1.4f)
                                                    ) {
                                                        Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(Modifier.width(2.dp))
                                                        Text("Lulus (70K)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    Spacer(Modifier.height(10.dp))

                                    // Rincian Gaji & Kehadiran (Full Width Info Box)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "Upah Harian: ",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        Formatters.rupiah(e.salaryAmount),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    "Hadir Bulan Ini: $attendanceCount hari • Est. ${Formatters.rupiah(estimatedTotalGaji)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (e.lastPaidAt != null && e.lastPaidAt > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFECFDF5)
                                                ) {
                                                    Text(
                                                        "Dibayar: ${Formatters.dateShort(e.lastPaidAt)}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF065F46),
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Action Buttons Row: Proporsional, Luas, & Nyaman Ditekan
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val latestLetter = remember(warningLetters, e.id) {
                                            warningLetters.filter { it.employeeId == e.id }.maxByOrNull { it.issueDate }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                createWarningLetterTarget = e
                                                showCreateWarningLetterDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (latestLetter != null) Color(0xFFDC2626) else Color(0xFFD97706)
                                            ),
                                            border = BorderStroke(1.dp, if (latestLetter != null) Color(0xFFFCA5A5) else Color(0xFFFDE68A)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                        ) {
                                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text("SP / PHK", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { paySalaryTarget = e },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .height(46.dp)
                                                .testTag("btn-pay-salary-${e.id}")
                                        ) {
                                            Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(19.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Bayar Gaji", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // TAB 1: SURAT PERINGATAN & PHK
                WarningLettersTabContent(
                    letters = warningLetters,
                    employees = list,
                    companyName = companyName,
                    companyAddress = companyAddress,
                    onCreateClick = {
                        createWarningLetterTarget = null
                        showCreateWarningLetterDialog = true
                    },
                    onSelectDetail = { selectedWarningLetterDetail = it },
                    onDelete = { viewModel.deleteWarningLetter(it) }
                )
            } else {
                // TAB 2: CALON KARYAWAN & UNDANGAN
                var selectedPositionFilter by remember { mutableStateOf("ALL") }

                val filteredApplicants = remember(applicants, selectedPositionFilter) {
                    val base = applicants.filter { !it.isDeleted }
                    if (selectedPositionFilter == "ALL") base
                    else base.filter { it.positionApplied == selectedPositionFilter }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Info Banner: Mode Opsi 1
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mode Undangan Sekali Pakai (Anti-Spam)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D47A1))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Kirim link unik ke calon kandidat via WhatsApp. Link hanya berlaku untuk 1x pengisian form web. Begitu diterima, data otomatis masuk ke Master Karyawan & Master Sopir.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E3A8A),
                                    lineHeight = 16.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { showCreateInviteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Buat Link Formulir Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Section: Filter Posisi
                    item {
                        Text("Daftar Berkas Pelamar Masuk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val filters = listOf(
                                "ALL" to "Semua (${applicants.count { !it.isDeleted }})",
                                "OPERATOR" to "Operator",
                                "DRIVER" to "Sopir Truk",
                                "KULI" to "Kuli / Bongkar",
                                "GUDANG" to "Gudang",
                                "TEKNISI" to "Teknisi",
                                "ADMIN" to "Admin"
                            )
                            items(filters) { (key, label) ->
                                FilterChip(
                                    selected = selectedPositionFilter == key,
                                    onClick = { selectedPositionFilter = key },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // Section: List Pelamar Masuk
                    if (filteredApplicants.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Belum Ada Pelamar Masuk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Buat link formulir dan kirimkan ke kandidat Anda.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        items(filteredApplicants, key = { it.id }) { app ->
                            ApplicantCard(
                                applicant = app,
                                onPreviewKtp = { url -> previewImageTarget = Pair("Foto KTP: ${app.fullName}", url) },
                                onPreviewSelf = { url -> previewImageTarget = Pair("Pas Foto: ${app.fullName}", url) },
                                onPreviewSim = { url -> previewImageTarget = Pair("Foto SIM: ${app.fullName}", url) },
                                onContactWa = {
                                    val cleanPhone = if (app.phone.startsWith("0")) "62" + app.phone.substring(1) else app.phone.replace("+", "")
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=Halo%20${Uri.encode(app.fullName)},%20kami%20dari%20CV.%20Bahtera%20Mulya%20Plastik%20telah%20menerima%20lamaran%20Anda.%20")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                },
                                onAccept = { acceptApplicantTarget = app },
                                onReject = { rejectApplicantTarget = app },
                                onDelete = { viewModel.deleteApplicant(app.id) }
                            )
                        }
                    }

                    // Section: Riwayat Link Undangan
                    if (invitations.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("Riwayat Link Undangan (${invitations.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        items(invitations, key = { it.id }) { inv ->
                            InvitationCard(
                                invitation = inv,
                                onCopyLink = {
                                    val url = "https://www.zedmz.cloud/karir/form?token=${inv.token}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Link Undangan Form", url)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Link undangan berhasil disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onShareWa = {
                                    val url = "https://www.zedmz.cloud/karir/form?token=${inv.token}"
                                    val msg = "Halo ${if (inv.candidateName.isNotBlank()) inv.candidateName else "Kandidat"}, silakan lengkapi formulir data identitas calon karyawan di CV. Bahtera Mulya Plastik melalui link berikut:\n\n$url\n\nLink ini berlaku 1x pengisian formulir. Terima kasih."
                                    val cleanPhone = if (inv.candidatePhone.startsWith("0")) "62" + inv.candidatePhone.substring(1) else inv.candidatePhone.replace("+", "")
                                    val uri = if (cleanPhone.isNotBlank()) {
                                        Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
                                    } else {
                                        Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                },
                                onDelete = { viewModel.deleteInvitation(inv.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Buat Surat Peringatan / PHK Baru ────────────────────────────
    if (showCreateWarningLetterDialog) {
        CreateWarningLetterDialog(
            targetEmployee = createWarningLetterTarget,
            allEmployees = list,
            companyName = companyName,
            onDismiss = {
                showCreateWarningLetterDialog = false
                createWarningLetterTarget = null
            },
            onConfirm = { letterData ->
                viewModel.createWarningLetter(
                    letter = letterData,
                    onSuccess = {
                        Toast.makeText(context, "Surat resmi berhasil diterbitkan!", Toast.LENGTH_SHORT).show()
                        showCreateWarningLetterDialog = false
                        createWarningLetterTarget = null
                        selectedTab = 1
                    },
                    onError = { err ->
                        Toast.makeText(context, "Gagal: $err", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // ── Dialog: Detail Surat Peringatan / PHK ──────────────────────────────
    selectedWarningLetterDetail?.let { letter ->
        WarningLetterDetailDialog(
            letter = letter,
            companyName = companyName,
            companyAddress = companyAddress,
            employeePhone = list.firstOrNull { it.id == letter.employeeId }?.phone,
            onDismiss = { selectedWarningLetterDetail = null },
            onPrint = {
                val html = generateWarningLetterHtml(
                    letter = letter,
                    companyName = companyName,
                    companyAddress = companyAddress
                )
                printWarningLetter(context, html, "Surat_${letter.letterNumber}")
            },
            onShareWa = {
                val phone = list.firstOrNull { it.id == letter.employeeId }?.phone
                shareWarningLetterWhatsApp(context, letter, phone, companyName)
            },
            onDelete = {
                viewModel.deleteWarningLetter(letter.id)
                selectedWarningLetterDetail = null
            }
        )
    }

    // ── Dialog: Buat Link Undangan Baru ─────────────────────────────────────────
    if (showCreateInviteDialog) {
        CreateInvitationDialog(
            onDismiss = { showCreateInviteDialog = false },
            onCreate = { name, phone, pos, onDone ->
                viewModel.createInvitation(name, phone, pos) { token, url ->
                    onDone(token, url)
                }
            }
        )
    }

    // ── Dialog: Terima Karyawan ─────────────────────────────────────────────────
    acceptApplicantTarget?.let { app ->
        AcceptApplicantDialog(
            applicant = app,
            onDismiss = { acceptApplicantTarget = null },
            onConfirm = { salary, pos, role ->
                viewModel.acceptApplicant(app.id, salary, pos, role) {
                    android.widget.Toast.makeText(context, "Selamat! ${app.fullName} resmi diterima menjadi Karyawan Aktif.", android.widget.Toast.LENGTH_LONG).show()
                    acceptApplicantTarget = null
                    selectedTab = 0
                }
            }
        )
    }

    // ── Dialog: Tolak Pelamar ───────────────────────────────────────────────────
    rejectApplicantTarget?.let { app ->
        RejectApplicantDialog(
            applicant = app,
            onDismiss = { rejectApplicantTarget = null },
            onConfirm = { reason ->
                viewModel.rejectApplicant(app.id, reason)
                android.widget.Toast.makeText(context, "Lamaran ${app.fullName} ditolak.", android.widget.Toast.LENGTH_SHORT).show()
                rejectApplicantTarget = null
            }
        )
    }

    // ── Dialog: Preview Foto Zoom ───────────────────────────────────────────────
    previewImageTarget?.let { (title, url) ->
        ImagePreviewDialog(
            title = title,
            imageUrl = url,
            onDismiss = { previewImageTarget = null }
        )
    }

    // ── Dialog: Form Edit Karyawan Manual ───────────────────────────────────────
    formEdit?.let { editing ->
        var name by remember { mutableStateOf(editing.name) }
        var position by remember { mutableStateOf(editing.position.orEmpty()) }
        var pin by remember { mutableStateOf(editing.fingerprintPIN.orEmpty()) }
        var salary by remember { mutableStateOf(if (editing.salaryAmount == 0.0) "" else editing.salaryAmount.toLong().toString()) }
        var isTraining by remember { mutableStateOf(editing.isTraining) }
        var trainingCompletedText by remember { mutableStateOf(editing.trainingDaysCompleted.toString()) }
        var trainingTargetText by remember { mutableStateOf(editing.trainingTargetDays.toString()) }
        var selectedEmployeeType by remember { mutableStateOf(editing.employeeType) }
        val defaultOutlet = remember(outlets) { outlets.firstOrNull { it.id == editing.outletId } ?: outlets.firstOrNull() }
        var selectedOutletId by remember { mutableStateOf(editing.outletId ?: defaultOutlet?.id) }
        var selectedOutletName by remember {
            mutableStateOf(outlets.firstOrNull { it.id == editing.outletId }?.name ?: defaultOutlet?.name ?: "Outlet Utama")
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
                        label = { Text("Gaji Harian (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("emp-salary")
                    )
                    Spacer(Modifier.size(8.dp))

                    // Masa Training Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { isTraining = !isTraining }.padding(vertical = 4.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = isTraining,
                            onCheckedChange = { isTraining = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Karyawan dalam Masa Training", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (isTraining) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = trainingCompletedText,
                                onValueChange = { trainingCompletedText = it.filter { c -> c.isDigit() } },
                                label = { Text("Hari Selesai") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = trainingTargetText,
                                onValueChange = { trainingTargetText = it.filter { c -> c.isDigit() } },
                                label = { Text("Target Hari") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

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
                            OutlinedButton(
                                onClick = { selectedEmployeeType = valType },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
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
                            if (outlets.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Seluruh Outlet") },
                                    onClick = {
                                        selectedOutletId = null
                                        selectedOutletName = "Seluruh Outlet"
                                        outletDropdownExpanded = false
                                    }
                                )
                            }
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
                                    isTraining = isTraining,
                                    trainingDaysCompleted = trainingCompletedText.toIntOrNull() ?: 0,
                                    trainingTargetDays = trainingTargetText.toIntOrNull() ?: 14,
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

    // Dialog Konfirmasi Bersihkan Semua Data Karyawan Manufaktur
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Bersihkan Semua Karyawan Manufaktur?") },
            text = { Text("Seluruh data karyawan manufaktur akan dihapus bersih. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showClearAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    paySalaryTarget?.let { target ->
        val defaultAttCount = remember(target.id) { viewModel.countAttendanceFromProduction(target.id) }
        var inputDaysText by remember { mutableStateOf(if (defaultAttCount > 0) defaultAttCount.toString() else "25") }
        var payMethod by remember { mutableStateOf("TRANSFER") }
        var payNote by remember { mutableStateOf("Gaji Bulan Ini") }
        val daysInt = inputDaysText.toIntOrNull() ?: 0
        val totalPay = daysInt * target.salaryAmount
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) paySalaryTarget = null },
            icon = { Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color(0xFF10B981)) },
            title = { Text("Bayar Gaji: ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Jabatan: ${target.position ?: "Karyawan"} • Gaji Harian: ${Formatters.rupiah(target.salaryAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputDaysText,
                        onValueChange = { inputDaysText = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah Hari Hadir (Qty Hadir)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input-pay-days")
                    )
                    OutlinedTextField(
                        value = payNote,
                        onValueChange = { payNote = it },
                        label = { Text("Catatan Pembayaran") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Rincian Pembayaran Kas Keluar:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("$daysInt hari × ${Formatters.rupiah(target.salaryAmount)}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Total Dibayar: ${Formatters.rupiah(totalPay)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        viewModel.paySalary(
                            target = target,
                            daysCount = daysInt,
                            totalAmount = totalPay,
                            note = payNote,
                            paymentMethod = payMethod,
                            onSuccess = {
                                isSubmitting = false
                                android.widget.Toast.makeText(
                                    context,
                                    "Gaji ${Formatters.rupiah(totalPay)} untuk ${target.name} ($daysInt hari) berhasil dibayarkan dan dicatat ke Arus Kas!",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                paySalaryTarget = null
                            },
                            onError = { errMsg ->
                                isSubmitting = false
                                android.widget.Toast.makeText(
                                    context,
                                    "Gagal membayar gaji: $errMsg",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.height(44.dp).testTag("btn-confirm-pay-salary")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                    }
                    Text("Konfirmasi Bayar Gaji", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSubmitting) paySalaryTarget = null }) {
                    Text("Batal")
                }
            }
        )
    }

    editTrainingTarget?.let { target ->
        EditTrainingDaysDialog(
            employee = target,
            onDismiss = { editTrainingTarget = null },
            onSave = { completed, targetDays ->
                viewModel.updateTrainingDays(target, completed, targetDays)
                android.widget.Toast.makeText(context, "Hari training ${target.name} diperbarui menjadi $completed / $targetDays hari masuk.", android.widget.Toast.LENGTH_SHORT).show()
                editTrainingTarget = null
            }
        )
    }

    graduateConfirmTarget?.let { target ->
        GraduateTrainingDialog(
            employee = target,
            onDismiss = { graduateConfirmTarget = null },
            onConfirm = { regularSalary ->
                viewModel.graduateTraining(target, regularSalary)
                android.widget.Toast.makeText(context, "${target.name} resmi lulus training menjadi Karyawan Reguler (Upah: ${Formatters.rupiah(regularSalary)}/hari)!", android.widget.Toast.LENGTH_LONG).show()
                graduateConfirmTarget = null
            }
        )
    }
}

// ── COMPOSABLES: RECRUITMENT COMPONENTS ──────────────────────────────────────

@Composable
fun ApplicantCard(
    applicant: BmpJobApplicantEntity,
    onPreviewKtp: (String) -> Unit,
    onPreviewSelf: (String) -> Unit,
    onPreviewSim: (String) -> Unit,
    onContactWa: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (applicant.status == "PENDING") Color(0xFF1565C0).copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1565C0).copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = Color(0xFF1565C0))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(applicant.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Melamar: ${applicant.positionApplied} • ${if (applicant.nik.isNotBlank()) "NIK: " + applicant.nik else applicant.phone}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                val (badgeColor, textColor, statusText) = when (applicant.status) {
                    "ACCEPTED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "DITERIMA")
                    "REJECTED" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "DITOLAK")
                    else -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "MENUNGGU REVIEW")
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(8.dp))

            // Details
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (applicant.phone.isNotBlank()) {
                    Text("📱 WhatsApp / HP: ${applicant.phone}", fontSize = 12.sp)
                }
                if (applicant.birthPlaceDate.isNotBlank()) {
                    Text("🎂 TTL: ${applicant.birthPlaceDate}", fontSize = 12.sp)
                }
                if (applicant.address.isNotBlank()) {
                    Text("📍 Alamat: ${applicant.address}", fontSize = 12.sp)
                }
                Text("🎓 Pendidikan: ${applicant.education}", fontSize = 12.sp)
                if (!applicant.experience.isNullOrBlank()) {
                    Text("💼 Pengalaman: ${applicant.experience}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text("🕒 Masuk: ${Formatters.dateShort(applicant.appliedAt)}", fontSize = 11.sp, color = Color.Gray)

                // Test Score Badge & Wage Agreement
                if (applicant.testScore > 0) {
                    Spacer(Modifier.height(3.dp))
                    val isPass = applicant.testScore >= 70
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPass) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        border = BorderStroke(1.dp, if (isPass) Color(0xFF81C784) else Color(0xFFFFB74D))
                    ) {
                        Text(
                            "🎯 Skor Tes: ${applicant.testScore}/100 ${if (isPass) "(Sangat Baik)" else "(Perlu Ditinjau)"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPass) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (applicant.wageAgreed) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F8E9)
                    ) {
                        Text(
                            "💰 Setuju Upah: 50K (Training) & 70K (Reguler)",
                            fontSize = 10.5.sp,
                            color = Color(0xFF33691E),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Photo Thumbnails
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!applicant.ktpPhotoUrl.isNullOrBlank()) {
                    DocumentThumbnail(label = "Foto KTP", url = applicant.ktpPhotoUrl) {
                        onPreviewKtp(applicant.ktpPhotoUrl)
                    }
                }
                if (!applicant.selfPhotoUrl.isNullOrBlank()) {
                    DocumentThumbnail(label = "Pas Foto", url = applicant.selfPhotoUrl) {
                        onPreviewSelf(applicant.selfPhotoUrl)
                    }
                }
                if (!applicant.simPhotoUrl.isNullOrBlank()) {
                    DocumentThumbnail(label = "Foto SIM", url = applicant.simPhotoUrl) {
                        onPreviewSim(applicant.simPhotoUrl)
                    }
                }
            }

            if (!applicant.cvPdfUrl.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(applicant.cvPdfUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuka file PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0)),
                    border = BorderStroke(1.dp, Color(0xFF1565C0)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp)
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("📄 Lihat Lampiran CV (PDF)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onContactWa,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hubungi WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (applicant.status == "PENDING") {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = BorderStroke(1.dp, Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text("Tolak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Terima Karyawan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Hapus Berkas", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentThumbnail(
    label: String,
    url: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier
            .width(84.dp)
            .height(58.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = url,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InvitationCard(
    invitation: BmpJobInvitationEntity,
    onCopyLink: () -> Unit,
    onShareWa: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(invitation.token, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0D47A1))
                    Spacer(Modifier.width(6.dp))
                    val (badgeBg, badgeText, statusLabel) = when (invitation.status) {
                        "USED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "SUDAH DIISI")
                        "CANCELLED" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "BATAL")
                        else -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "AKTIF")
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = badgeBg) {
                        Text(statusLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeText, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Target: ${if (invitation.candidateName.isNotBlank()) invitation.candidateName else "Kandidat"} • Posisi: ${invitation.positionTarget}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (invitation.status == "ACTIVE") {
                    IconButton(onClick = onCopyLink, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin Link", tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShareWa, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Share, contentDescription = "Kirim WA", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Batalkan", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── DIALOGS ──────────────────────────────────────────────────────────────────

@Composable
fun CreateInvitationDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, phone: String, pos: String, onDone: (token: String, url: String) -> Unit) -> Unit
) {
    var candidateName by remember { mutableStateOf("") }
    var candidatePhone by remember { mutableStateOf("") }
    var positionTarget by remember { mutableStateOf("OPERATOR") }
    var generatedToken by remember { mutableStateOf<String?>(null) }
    var generatedUrl by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(if (generatedUrl == null) "Buat Link Undangan Form" else "Link Undangan Berhasil Dibuat!") },
        text = {
            if (generatedUrl == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kandidat akan mengisi data identitas & foto KTP secara mandiri melalui link ini.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = candidateName,
                        onValueChange = { candidateName = it },
                        label = { Text("Nama Calon Kandidat (Opsional)") },
                        placeholder = { Text("Contoh: Slamet Riyadi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = candidatePhone,
                        onValueChange = { candidatePhone = it },
                        label = { Text("No. WhatsApp Calon (Opsional)") },
                        placeholder = { Text("Contoh: 08123456789") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Pilihan Posisi Ditawarkan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val positions = listOf(
                        "OPERATOR" to "Operator Mesin",
                        "DRIVER" to "Sopir Truk",
                        "KULI" to "Kuli / Bongkar",
                        "GUDANG" to "Staff Gudang",
                        "TEKNISI" to "Teknisi",
                        "ADMIN" to "Admin"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        positions.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (key, label) ->
                                    val selected = positionTarget == key
                                    OutlinedButton(
                                        onClick = { positionTarget = key },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected) Color(0xFF1565C0).copy(alpha = 0.15f) else Color.Transparent,
                                            contentColor = if (selected) Color(0xFF1565C0) else Color.DarkGray
                                        ),
                                        border = BorderStroke(1.dp, if (selected) Color(0xFF1565C0) else Color.LightGray),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Kode Token: $generatedToken", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                            Spacer(Modifier.height(4.dp))
                            Text(generatedUrl!!, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                    Text("Link ini hanya bisa diisi 1 kali saja oleh calon kandidat.", fontSize = 11.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Link Undangan Form", generatedUrl)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Link berhasil disalin!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Salin Link", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val msg = "Halo ${if (candidateName.isNotBlank()) candidateName else "Kandidat"}, silakan lengkapi formulir identitas calon karyawan CV. Bahtera Mulya Plastik melalui link berikut:\n\n$generatedUrl\n\nLink ini hanya berlaku 1x pengisian. Terima kasih."
                                val cleanPhone = if (candidatePhone.startsWith("0")) "62" + candidatePhone.substring(1) else candidatePhone.replace("+", "")
                                val uri = if (cleanPhone.isNotBlank()) {
                                    Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
                                } else {
                                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                                }
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kirim WA", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (generatedUrl == null) {
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        onCreate(candidateName.trim(), candidatePhone.trim(), positionTarget) { token, url ->
                            isSubmitting = false
                            generatedToken = token
                            generatedUrl = url
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Generate Link")
                }
            } else {
                Button(onClick = onDismiss) { Text("Selesai") }
            }
        },
        dismissButton = {
            if (generatedUrl == null) {
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}

@Composable
fun AcceptApplicantDialog(
    applicant: BmpJobApplicantEntity,
    onDismiss: () -> Unit,
    onConfirm: (salary: Double, pos: String, role: String) -> Unit
) {
    var salaryText by remember { mutableStateOf("50000") }
    var position by remember { mutableStateOf(applicant.positionApplied) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF1565C0)) },
        title = { Text("Terima Karyawan: ${applicant.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kandidat akan otomatis dimasukkan ke Master Karyawan Aktif (dan Master Sopir jika posisi Sopir).", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Jabatan / Posisi Kerja") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it.filter { c -> c.isDigit() } },
                    label = { Text("Gaji Pokok / Upah Harian (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    val salaryAmt = salaryText.toDoubleOrNull() ?: 0.0
                    onConfirm(salaryAmt, position.trim(), position.trim())
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text("Konfirmasi Terima")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }) { Text("Batal") }
        }
    )
}

@Composable
fun RejectApplicantDialog(
    applicant: BmpJobApplicantEntity,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("Kualifikasi belum sesuai kebutuhan") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tolak Lamaran: ${applicant.fullName}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status pelamar akan diubah menjadi DITOLAK.", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan Penolakan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) { Text("Tolak Pelamar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun ImagePreviewDialog(
    title: String,
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup")
                    }
                }
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup Preview")
                }
            }
        }
    }
}

@Composable
fun EditTrainingDaysDialog(
    employee: BmpEmployeeEntity,
    onDismiss: () -> Unit,
    onSave: (completed: Int, target: Int) -> Unit
) {
    var completedText by remember { mutableStateOf(employee.trainingDaysCompleted.toString()) }
    var targetText by remember { mutableStateOf(employee.trainingTargetDays.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color(0xFFD97706)) },
        title = { Text("Koreksi Hari Training: ${employee.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ubah jumlah hari kerja masuk yang telah dijalani atau ubah total target hari masa training.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = completedText,
                    onValueChange = { completedText = it.filter { c -> c.isDigit() } },
                    label = { Text("Hari Masuk Selesai (Saat Ini)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                    label = { Text("Target Total Hari Training") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val comp = completedText.toIntOrNull() ?: employee.trainingDaysCompleted
                    val targ = targetText.toIntOrNull() ?: employee.trainingTargetDays
                    onSave(comp, targ)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun GraduateTrainingDialog(
    employee: BmpEmployeeEntity,
    onDismiss: () -> Unit,
    onConfirm: (regularSalary: Double) -> Unit
) {
    var salaryText by remember { mutableStateOf("70000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.School, contentDescription = null, tint = Color(0xFF1565C0)) },
        title = { Text("Luluskan Training: ${employee.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Karyawan ini akan diubah statusnya menjadi Karyawan Reguler dan upah hariannya akan dinaikkan.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it.filter { c -> c.isDigit() } },
                    label = { Text("Upah Reguler Baru (Rp / hari)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sal = salaryText.toDoubleOrNull() ?: 70000.0
                    onConfirm(sal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("Luluskan & Naikkan Gaji")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SURAT PERINGATAN (SP 1, SP 2) & SURAT DIKELUARKAN (PHK)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WarningLettersTabContent(
    letters: List<BmpWarningLetterEntity>,
    employees: List<BmpEmployeeEntity>,
    companyName: String,
    companyAddress: String,
    onCreateClick: () -> Unit,
    onSelectDetail: (BmpWarningLetterEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val context = LocalContext.current

    val filteredLetters = remember(letters, selectedFilter) {
        when (selectedFilter) {
            "SP_1" -> letters.filter { it.letterType == "SP_1" }
            "SP_2" -> letters.filter { it.letterType == "SP_2" }
            "TERMINATION" -> letters.filter { it.letterType == "TERMINATION" }
            else -> letters
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Banner / Information
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Manajemen Disiplin & Surat Peringatan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Kelola Surat Peringatan I (SP 1), Peringatan II (SP 2), dan Surat Pemutusan Hubungan Kerja (PHK). Dilengkapi cetak PDF resmi ber-Kop Surat Sidoarjo dan kirim via WhatsApp.",
                        fontSize = 12.sp,
                        color = Color(0xFF78350F),
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onCreateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Terbitkan Surat Peringatan / PHK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar Surat Resmi (${filteredLetters.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val filters = listOf(
                    "ALL" to "Semua (${letters.size})",
                    "SP_1" to "SP 1 (${letters.count { it.letterType == "SP_1" }})",
                    "SP_2" to "SP 2 (${letters.count { it.letterType == "SP_2" }})",
                    "TERMINATION" to "PHK (${letters.count { it.letterType == "TERMINATION" }})"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (filteredLetters.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tidak Ada Catatan Surat Peringatan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Seluruh staf dan operator bekerja dengan tertib dan mematuhi SOP.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(filteredLetters, key = { it.id }) { letter ->
                val emp = employees.firstOrNull { it.id == letter.employeeId }
                WarningLetterCard(
                    letter = letter,
                    employeePhone = emp?.phone,
                    companyName = companyName,
                    companyAddress = companyAddress,
                    onDetailClick = { onSelectDetail(letter) },
                    onPrint = {
                        val html = generateWarningLetterHtml(letter, companyName, companyAddress)
                        printWarningLetter(context, html, "Surat_${letter.letterNumber}")
                    },
                    onShareWa = {
                        shareWarningLetterWhatsApp(context, letter, emp?.phone, companyName)
                    },
                    onDelete = { onDelete(letter.id) }
                )
            }
        }
    }
}

@Composable
fun WarningLetterCard(
    letter: BmpWarningLetterEntity,
    employeePhone: String?,
    companyName: String,
    companyAddress: String,
    onDetailClick: () -> Unit,
    onPrint: () -> Unit,
    onShareWa: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (letter.letterType) {
                        "TERMINATION" -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    "🛑 SURAT PHK / KELUAR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        "SP_2" -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFE4E6),
                                border = BorderStroke(1.dp, Color(0xFFFDA4AF))
                            ) {
                                Text(
                                    "🚨 SURAT PERINGATAN II (SP-2)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBE123C),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFCD34D))
                            ) {
                                Text(
                                    "⚠️ SURAT PERINGATAN I (SP-1)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        letter.letterNumber,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Nama & Posisi
            Text(
                letter.employeeName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${if (letter.employeeRole.isNotBlank()) letter.employeeRole else "Karyawan"} • Diterbitkan: ${Formatters.dateLong(letter.issueDate)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (letter.letterType != "TERMINATION" && letter.validUntil > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "⏱️ Berlaku s/d: ${Formatters.dateLong(letter.validUntil)}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD97706)
                )
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "Pelanggaran: ${letter.reasonCategory}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        letter.reasonDetail,
                        fontSize = 12.sp,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Detail", fontSize = 11.5.sp)
                }

                Button(
                    onClick = onPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onShareWa,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kirim WA", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Surat Ini?") },
            text = { Text("Surat ${letter.letterNumber} untuk ${letter.employeeName} akan dihapus dari arsip.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun CreateWarningLetterDialog(
    targetEmployee: BmpEmployeeEntity?,
    allEmployees: List<BmpEmployeeEntity>,
    companyName: String,
    onDismiss: () -> Unit,
    onConfirm: (BmpWarningLetterData) -> Unit
) {
    var selectedEmployee by remember { mutableStateOf(targetEmployee ?: allEmployees.firstOrNull()) }
    var letterType by remember { mutableStateOf("SP_1") }
    var letterNumber by remember { mutableStateOf("") }
    var reasonCategory by remember { mutableStateOf("Mangkir / Tidak Hadir Tanpa Izin") }
    var reasonDetail by remember { mutableStateOf("") }
    var correctiveAction by remember { mutableStateOf("") }
    var validityMonths by remember { mutableStateOf(3) }
    var city by remember { mutableStateOf("Sidoarjo") }
    var issuedBy by remember { mutableStateOf("Manajemen Perusahaan") }

    // Auto-generate letter number on type or emp change
    LaunchedEffect(letterType, selectedEmployee) {
        val typeCode = when (letterType) {
            "SP_1" -> "SP1"
            "SP_2" -> "SP2"
            else -> "SK-PHK"
        }
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val randCode = String.format("%03d", (System.currentTimeMillis() % 900 + 100))
        letterNumber = "$typeCode/BP/$year/$month/$randCode"
    }

    val reasonTemplates = listOf(
        "Mangkir / Tidak Hadir Tanpa Izin",
        "Terlambat Datang & Indisipliner Jam Kerja",
        "Kelalaian Operasional Mesin / Reject Tinggi",
        "Pelanggaran SOP & Tata Tertib Pabrik",
        "Tindakan Merugikan Kinerja Tim / Perusahaan",
        "Pemutusan Hubungan Kerja (Masa Evaluasi / Pelanggaran Berat)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color(0xFFD97706))
                Spacer(Modifier.width(8.dp))
                Text("Terbitkan Surat Resmi", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pilih Karyawan
                Text("Pilih Karyawan Target", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allEmployees) { emp ->
                        FilterChip(
                            selected = selectedEmployee?.id == emp.id,
                            onClick = { selectedEmployee = emp },
                            label = { Text(emp.name, fontSize = 11.5.sp) }
                        )
                    }
                }

                // Pilih Jenis Surat
                Text("Jenis Surat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf(
                        "SP_1" to "⚠️ SP 1",
                        "SP_2" to "🚨 SP 2",
                        "TERMINATION" to "🛑 PHK"
                    )
                    types.forEach { (tKey, tLabel) ->
                        FilterChip(
                            selected = letterType == tKey,
                            onClick = { letterType = tKey },
                            label = { Text(tLabel, fontSize = 11.5.sp, fontWeight = if (letterType == tKey) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                // Nomor Surat
                OutlinedTextField(
                    value = letterNumber,
                    onValueChange = { letterNumber = it },
                    label = { Text("Nomor Surat Resmi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Masa Berlaku (jika bukan PHK)
                if (letterType != "TERMINATION") {
                    Text("Masa Berlaku Surat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val validityOptions = listOf(1 to "1 Bulan", 3 to "3 Bulan", 6 to "6 Bulan", 0 to "Selamanya")
                        validityOptions.forEach { (m, label) ->
                            FilterChip(
                                selected = validityMonths == m,
                                onClick = { validityMonths = m },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Kategori Alasan Template
                Text("Kategori Pelanggaran / Alasan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reasonTemplates) { tmpl ->
                        FilterChip(
                            selected = reasonCategory == tmpl,
                            onClick = { reasonCategory = tmpl },
                            label = { Text(tmpl, fontSize = 11.sp) }
                        )
                    }
                }

                // Rincian Kronologi / Detail Pelanggaran
                OutlinedTextField(
                    value = reasonDetail,
                    onValueChange = { reasonDetail = it },
                    label = { Text("Rincian Kronologi & Poin Pelanggaran") },
                    placeholder = { Text("Contoh: Karyawan tidak masuk kerja tanpa izin selama 3 hari berturut-turut...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tindakan Korektif (Opsional)
                OutlinedTextField(
                    value = correctiveAction,
                    onValueChange = { correctiveAction = it },
                    label = { Text("Tindakan Korektif / Catatan Khusus") },
                    placeholder = { Text("Contoh: Wajib hadir tepat waktu dan melapor setiap pagi ke supervisor...") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Kota Penerbitan (Default: Sidoarjo)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Kota Terbit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = issuedBy,
                        onValueChange = { issuedBy = it },
                        label = { Text("Penandatangan") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val emp = selectedEmployee ?: return@Button
                    val now = System.currentTimeMillis()
                    val validUntil = if (letterType != "TERMINATION" && validityMonths > 0) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.MONTH, validityMonths)
                        cal.timeInMillis
                    } else 0L

                    val data = BmpWarningLetterData(
                        employeeId = emp.id,
                        employeeName = emp.name,
                        employeeNik = emp.nik.orEmpty(),
                        employeeRole = emp.position ?: "Karyawan",
                        letterType = letterType,
                        letterNumber = letterNumber.trim(),
                        issueDate = now,
                        validUntil = validUntil,
                        reasonCategory = reasonCategory.trim(),
                        reasonDetail = reasonDetail.ifBlank { "Tindakan ketidakdisiplinan tata tertib kerja perusahaan." }.trim(),
                        correctiveAction = correctiveAction.trim(),
                        issuedBy = issuedBy.trim().ifBlank { "Manajemen Perusahaan" },
                        city = city.trim().ifBlank { "Sidoarjo" },
                        companyName = companyName
                    )
                    onConfirm(data)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("Terbitkan Surat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun WarningLetterDetailDialog(
    letter: BmpWarningLetterEntity,
    companyName: String,
    companyAddress: String,
    employeePhone: String?,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onShareWa: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (letter.letterType) {
                    "TERMINATION" -> Icon(Icons.Outlined.Warning, null, tint = Color(0xFFDC2626))
                    "SP_2" -> Icon(Icons.Outlined.WarningAmber, null, tint = Color(0xFFE11D48))
                    else -> Icon(Icons.Outlined.WarningAmber, null, tint = Color(0xFFD97706))
                }
                Spacer(Modifier.width(8.dp))
                Text("Detail Surat Resmi", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            when (letter.letterType) {
                                "TERMINATION" -> "SURAT PEMUTUSAN HUBUNGAN KERJA (PHK)"
                                "SP_2" -> "SURAT PERINGATAN KEDUA (SP-2)"
                                else -> "SURAT PERINGATAN PERTAMA (SP-1)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Nomor: ${letter.letterNumber}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Perusahaan: $companyName", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("Identitas Karyawan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("• Nama: ${letter.employeeName}", fontSize = 12.sp)
                if (letter.employeeNik.isNotBlank()) Text("• NIK / KTP: ${letter.employeeNik}", fontSize = 12.sp)
                Text("• Jabatan: ${letter.employeeRole}", fontSize = 12.sp)
                Text("• Tanggal Terbit: ${Formatters.dateLong(letter.issueDate)}", fontSize = 12.sp)
                if (letter.letterType != "TERMINATION" && letter.validUntil > 0) {
                    Text("• Masa Berlaku: s/d ${Formatters.dateLong(letter.validUntil)}", fontSize = 12.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Poin & Rincian Pelanggaran:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Kategori: ${letter.reasonCategory}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF92400E))
                        Spacer(Modifier.height(4.dp))
                        Text(letter.reasonDetail, fontSize = 12.sp, color = Color(0xFF78350F))
                    }
                }

                if (letter.correctiveAction.isNotBlank()) {
                    Text("Tindakan Korektif Manajemen:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(letter.correctiveAction, fontSize = 12.sp)
                }

                Text("Kota & Pengesahan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("• Kota: ${letter.city}", fontSize = 12.sp)
                Text("• Penandatangan: ${letter.issuedBy} (Tanpa Tanda Tangan Karyawan)", fontSize = 12.sp)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(Icons.Outlined.Print, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak PDF")
                }
                Button(
                    onClick = onShareWa,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Outlined.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kirim WA")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Surat Resmi?") },
            text = { Text("Surat nomor ${letter.letterNumber} akan dihapus permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HTML & PRINT & WHATSAPP GENERATORS
// ─────────────────────────────────────────────────────────────────────────────

fun generateWarningLetterHtml(
    letter: BmpWarningLetterEntity,
    companyName: String = "CV. Bahtera Plastik",
    companyAddress: String = "Sidoarjo, Jawa Timur"
): String {
    val title = when (letter.letterType) {
        "SP_1" -> "SURAT PERINGATAN PERTAMA (SP-1)"
        "SP_2" -> "SURAT PERINGATAN KEDUA (SP-2)"
        "TERMINATION" -> "SURAT PEMUTUSAN HUBUNGAN KERJA (PHK)"
        else -> "SURAT PERINGATAN RESMI"
    }

    val introText = when (letter.letterType) {
        "SP_1" -> "Sehubungan dengan hasil evaluasi kedisiplinan dan tata tertib kerja, dengan ini Manajemen perusahaan menerbitkan <b>Surat Peringatan Pertama (SP-1)</b> kepada karyawan yang bersangkutan sehubungan dengan adanya tindakan pelanggaran berupa:"
        "SP_2" -> "Sehubungan dengan adanya pengulangan pelanggaran dan/atau tindakan indisipliner setelah diterbitkannya Surat Peringatan Pertama, dengan ini Manajemen perusahaan menerbitkan <b>Surat Peringatan Kedua (SP-2 / Peringatan Terakhir)</b> sehubungan dengan tindakan pelanggaran berupa:"
        "TERMINATION" -> "Berdasarkan evaluasi menyeluruh atas catatan kedisiplinan kerja dan ketentuan peraturan perusahaan yang berlaku, dengan ini Manajemen perusahaan memberitahukan bahwa terhitung sejak tanggal surat ini diterbitkan, hubungan kerja antara perusahaan dengan yang bersangkutan dinyatakan <b>BERAKHIR (DIBERHENTIKAN / PHK)</b> atas dasar pertimbangan:"
        else -> "Sehubungan dengan evaluasi kerja, dengan ini Manajemen perusahaan menerbitkan surat ini atas dasar pertimbangan:"
    }

    val consequenceText = when (letter.letterType) {
        "SP_1" -> "<div class='content'>Diharapkan yang bersangkutan dapat segera memperbaiki sikap, disiplin, dan etos kerja. Apabila dalam masa berlaku Surat Peringatan Pertama ini karyawan yang bersangkutan kembali melakukan pelanggaran serupa atau pelanggaran tata tertib lainnya, maka Manajemen akan menerbitkan <b>Surat Peringatan Kedua (SP-2)</b>.</div>"
        "SP_2" -> "<div class='content'>Surat Peringatan Kedua ini merupakan <b>peringatan terakhir</b>. Apabila dalam masa berlaku surat ini yang bersangkutan kembali melakukan pelanggaran disiplin kerja, maka Manajemen akan mengambil tindakan tegas berupa <b>Pemutusan Hubungan Kerja (PHK / Diberhentikan)</b> tanpa peringatan lanjutan.</div>"
        "TERMINATION" -> "<div class='content'>Perusahaan mengucapkan terima kasih atas kontribusi yang telah diberikan selama masa kerja. Segala hak dan kewajiban administrasi terkait penyelesaian masa kerja akan diselesaikan sesuai dengan ketentuan manajemen yang berlaku.</div>"
        else -> ""
    }

    val validUntilRow = if (letter.letterType != "TERMINATION" && letter.validUntil > 0) {
        "<tr><td>Masa Berlaku</td><td>:</td><td>${Formatters.dateLong(letter.validUntil)}</td></tr>"
    } else ""

    val nikRow = if (letter.employeeNik.isNotBlank()) {
        "<tr><td>NIK / No. KTP</td><td>:</td><td>${letter.employeeNik}</td></tr>"
    } else ""

    val correctiveBox = if (letter.correctiveAction.isNotBlank()) {
        """
        <div class="reason-box" style="margin-top: 10px; border-left: 4px solid #0284c7;">
            <b>Tindakan Korektif / Catatan Khusus Manajemen:</b>
            <div style="margin-top: 6px; white-space: pre-wrap;">${letter.correctiveAction}</div>
        </div>
        """.trimIndent()
    } else ""

    val issueDateStr = Formatters.dateLong(letter.issueDate)
    val city = if (letter.city.isNotBlank()) letter.city else "Sidoarjo"

    return """
    <!DOCTYPE html>
    <html>
    <head>
    <meta charset="utf-8">
    <title>$title</title>
    <style>
      @page { size: A4; margin: 15mm 20mm; }
      body { font-family: 'Times New Roman', Times, serif; color: #111; margin: 0; padding: 20px 25px; line-height: 1.6; font-size: 12.5pt; }
      .kop { text-align: center; margin-bottom: 5px; }
      .company-title { font-size: 18pt; font-weight: bold; text-transform: uppercase; letter-spacing: 1px; color: #111; }
      .company-sub { font-size: 10.5pt; color: #333; margin-top: 2px; }
      .company-addr { font-size: 9.5pt; color: #555; margin-top: 2px; }
      .divider { border: 0; border-top: 2.5px solid #000; border-bottom: 1px solid #000; height: 3px; margin: 12px 0 22px 0; }
      .letter-title { text-align: center; font-size: 14pt; font-weight: bold; text-decoration: underline; text-transform: uppercase; margin-bottom: 3px; }
      .letter-no { text-align: center; font-size: 11pt; margin-bottom: 25px; }
      .content { text-align: justify; margin-bottom: 14px; }
      table.emp-info { width: 100%; border-collapse: collapse; margin: 12px 0 18px 0; font-size: 12pt; }
      table.emp-info td { padding: 4px 6px; vertical-align: top; }
      .reason-box { background: #fdfdfd; border: 1px solid #ccc; border-left: 4px solid #d97706; padding: 12px 16px; margin: 14px 0; border-radius: 4px; }
      .sig-container { width: 100%; margin-top: 40px; display: table; }
      .sig-right { display: table-cell; width: 45%; text-align: center; float: right; }
      .sig-space { height: 70px; }
      .sig-name { font-weight: bold; text-decoration: underline; }
    </style>
    </head>
    <body>
      <div class="kop">
        <div class="company-title">$companyName</div>
        <div class="company-sub">Manufaktur Produk Plastik & Injection Moulding</div>
        <div class="company-addr">$companyAddress</div>
      </div>
      <div class="divider"></div>

      <div class="letter-title">$title</div>
      <div class="letter-no">Nomor: ${letter.letterNumber}</div>

      <div class="content">
        Surat ini diterbitkan dan diberikan secara resmi kepada karyawan:
      </div>

      <table class="emp-info">
        <tr><td style="width: 140px;">Nama Karyawan</td><td style="width: 15px;">:</td><td><b>${letter.employeeName}</b></td></tr>
        $nikRow
        <tr><td>Jabatan / Posisi</td><td>:</td><td>${if (letter.employeeRole.isNotBlank()) letter.employeeRole else "Operator / Staf"}</td></tr>
        <tr><td>Tanggal Terbit</td><td>:</td><td>$issueDateStr</td></tr>
        $validUntilRow
      </table>

      <div class="content">
        $introText
      </div>

      <div class="reason-box">
        <b>Kategori & Rincian Pelanggaran / Alasan:</b>
        <div style="margin-top: 6px; font-weight: 500; color: #b45309;">${letter.reasonCategory}</div>
        <div style="margin-top: 4px; white-space: pre-wrap;">${letter.reasonDetail}</div>
      </div>

      $correctiveBox

      $consequenceText

      <div class="content" style="margin-top: 14px;">
        Demikian Surat ini dibuat untuk menjadi perhatian sungguh-sungguh dan dipatuhi dengan penuh tanggung jawab demi kelancaran dan ketertiban operasional kerja.
      </div>

      <div class="sig-container">
        <div class="sig-right">
          <div>$city, $issueDateStr</div>
          <div style="font-weight: bold; margin-top: 4px;">$companyName</div>
          <div class="sig-space"></div>
          <div class="sig-name">( ${letter.issuedBy.ifBlank { "Manajemen Perusahaan" }} )</div>
          <div style="font-size: 10pt; color: #444;">Pimpinan / HRD Manajemen</div>
        </div>
      </div>
    </body>
    </html>
    """.trimIndent()
}

private fun printWarningLetter(context: Context, html: String, jobName: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val printAttributes = PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        }
    }
    webView.loadDataWithBaseURL("https://www.zedmz.cloud", html, "text/html", "UTF-8", null)
}

private fun shareWarningLetterWhatsApp(
    context: Context,
    letter: BmpWarningLetterEntity,
    employeePhone: String?,
    companyName: String = "CV. Bahtera Plastik"
) {
    val title = when (letter.letterType) {
        "SP_1" -> "SURAT PERINGATAN PERTAMA (SP-1)"
        "SP_2" -> "SURAT PERINGATAN KEDUA (SP-2)"
        "TERMINATION" -> "SURAT PEMUTUSAN HUBUNGAN KERJA (PHK)"
        else -> "SURAT PERINGATAN RESMI"
    }
    val city = if (letter.city.isNotBlank()) letter.city else "Sidoarjo"
    val issueDateStr = Formatters.dateLong(letter.issueDate)
    val validUntilStr = if (letter.letterType != "TERMINATION" && letter.validUntil > 0) "\n⏱️ *Masa Berlaku s/d:* ${Formatters.dateLong(letter.validUntil)}" else ""
    val correctiveStr = if (letter.correctiveAction.isNotBlank()) "\n💡 *Tindakan Korektif:* ${letter.correctiveAction}" else ""

    val text = """
    *${title}*
    *$companyName*
    ----------------------------------------
    📄 *No. Surat:* ${letter.letterNumber}
    📅 *Tanggal Terbit:* $issueDateStr$validUntilStr
    
    Kepada Yth.
    *${letter.employeeName}*
    Jabatan: ${if (letter.employeeRole.isNotBlank()) letter.employeeRole else "Karyawan / Operator"}
    
    Dengan ini Manajemen menyampaikan surat resmi terkait kedisiplinan dan tata tertib kerja:
    
    ⚠️ *Kategori:* ${letter.reasonCategory}
    📌 *Poin Pelanggaran / Alasan:*
    ${letter.reasonDetail}$correctiveStr
    
    ----------------------------------------
    Diterbitkan di $city, $issueDateStr
    *Manajemen $companyName*
    _(Dokumen resmi fisik / cetak PDF ber-kop surat telah diarsipkan oleh HRD)_
    """.trimIndent()

    val cleanPhone = employeePhone?.replace(Regex("[^0-9]"), "")?.let {
        if (it.startsWith("0")) "62" + it.substring(1) else it
    }

    val intent = if (!cleanPhone.isNullOrBlank() && cleanPhone.length >= 8) {
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}")
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

