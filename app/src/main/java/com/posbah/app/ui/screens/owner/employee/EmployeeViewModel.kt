package com.posbah.app.ui.screens.owner.employee

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.ActivityLogEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.remote.api.PosApiService
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.security.PinHasher
import android.content.Context as AndroidContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

data class EmployeeManagementUiState(
    val employees: List<Employee> = emptyList(),
    val outlets: List<com.posbah.app.data.local.entities.Outlet> = emptyList(),
    val isOwner: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailVerificationOtp: String? = null,
    val pendingEmployee: Employee? = null,
    val businessMode: String = "FNB",
    // ── Ganti Gaji ──────────────────────────────────────────────────────────
    val showSalaryChangeDialog: Boolean = false,
    val activeEmployeeForSalaryChange: Employee? = null,
    val activityLogs: List<ActivityLogEntity> = emptyList()
)

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
    private val api: PosApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()

    private val _uiState = MutableStateFlow(EmployeeManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val user = authRepository.getActiveUser()
            val isOwner = user?.role == "OWNER"

            if (!isOwner) {
                _uiState.update {
                    it.copy(
                        isOwner = false,
                        isLoading = false,
                        error = "Akses ditolak: Hanya Owner yang dapat mengelola karyawan dan gaji."
                    )
                }
                return@launch
            }

            loadFromApi(user)
        }
    }

    private suspend fun loadFromApi(user: com.posbah.app.data.repository.UserSession?) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val ownerEmail = user?.email?.lowercase()?.trim()

            // ── Fetch outlets via API ──────────────────────────────────────
            val outletsResp = withContext(Dispatchers.IO) { api.getOutlets() }
            val outletsList = outletsResp.body()?.map { m ->
                com.posbah.app.data.local.entities.Outlet(
                    id = (m["id"] as? Number)?.toLong() ?: 0,
                    tenantId = m["tenantId"] as? String ?: tenantId,
                    name = m["name"] as? String ?: "",
                    address = m["address"] as? String,
                    phone = m["phone"] as? String,
                    isDefault = m["isDefault"] as? Boolean ?: false,
                    isOpen = m["isOpen"] as? Boolean ?: true,
                    currentEmployee = m["currentEmployee"] as? String,
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0,
                    isSynced = true
                )
            } ?: emptyList()

            // ── Fetch employees via API ────────────────────────────────────
            val empResp = withContext(Dispatchers.IO) { api.getEmployees() }
            val allEmployees = empResp.body()?.mapNotNull { m ->
                val empId = (m["id"] as? Number)?.toLong() ?: return@mapNotNull null
                val empRole = m["role"] as? String ?: "KASIR"
                val empEmail = m["email"] as? String
                // Skip OWNER and the logged-in owner themselves
                if (empRole == "OWNER") return@mapNotNull null
                if (!ownerEmail.isNullOrBlank() && empEmail?.lowercase()?.trim() == ownerEmail) return@mapNotNull null
                Employee(
                    id = empId,
                    tenantId = m["tenantId"] as? String ?: tenantId,
                    outletId = (m["outletId"] as? Number)?.toLong(),
                    name = m["name"] as? String ?: "",
                    email = empEmail,
                    phone = m["phone"] as? String,
                    role = empRole,
                    pinHash = m["pinHash"] as? String ?: "",
                    salary = (m["salary"] as? Number)?.toDouble() ?: 0.0,
                    payPeriod = m["payPeriod"] as? String ?: "MONTHLY",
                    isActive = m["isActive"] as? Boolean ?: true,
                    lastPaidAt = (m["lastPaidAt"] as? Number)?.toLong(),
                    emailVerified = m["emailVerified"] as? Boolean ?: false,
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0,
                    updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0,
                    isSynced = true
                )
            } ?: emptyList()

            _uiState.update {
                it.copy(
                    employees = allEmployees,
                    outlets = outletsList,
                    activityLogs = emptyList(), // Activity logs loaded from API if needed
                    isOwner = true,
                    isLoading = false,
                    businessMode = authRepository.activeBusinessMode() ?: "FNB",
                    error = null
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("EmployeeVM", "loadFromApi error: ${e.message}", e)
            _uiState.update { it.copy(isLoading = false, error = "Gagal memuat data: ${e.localizedMessage}") }
        }
    }

    /**
     * Tambah karyawan baru langsung ke VPS via API.
     */
    fun startAddEmployee(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String,
        salary: Double,
        payPeriod: String,
        outletId: Long?
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Nama, Email, dan Password wajib diisi.") }
            return
        }
        if (role == "OWNER") {
            _uiState.update { it.copy(error = "Owner tidak dapat menambahkan karyawan sebagai Owner.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val cleanEmail = email.lowercase().trim()

            try {
                // Check max 10 employees untuk outlet ini via API
                if (outletId != null) {
                    val currentEmployees = _uiState.value.employees.count { it.outletId == outletId }
                    if (currentEmployees >= 10) {
                        _uiState.update { it.copy(isLoading = false, error = "Gagal: Outlet tujuan sudah mencapai batas maksimal 10 karyawan.") }
                        return@launch
                    }
                }

                // Check duplikat email via current state
                val existingInList = _uiState.value.employees.any { it.email?.lowercase() == cleanEmail }
                if (existingInList) {
                    _uiState.update { it.copy(isLoading = false, error = "Email sudah digunakan oleh karyawan lain.") }
                    return@launch
                }

                val hashedPassword = PinHasher.hash(password)
                val body = mutableMapOf<String, Any?>(
                    "name" to name,
                    "email" to cleanEmail,
                    "phone" to phone.trim().takeIf { it.isNotBlank() },
                    "role" to role,
                    "pinHash" to hashedPassword,
                    "salary" to salary,
                    "payPeriod" to payPeriod,
                    "outletId" to outletId,
                    "isActive" to true,
                    "emailVerified" to false
                )

                val resp = withContext(Dispatchers.IO) { api.createEmployee(body) }
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(error = null) }
                    // Reload dari API
                    val user = authRepository.getActiveUser()
                    loadFromApi(user)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Gagal menambah karyawan: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.localizedMessage}") }
            }
        }
    }

    fun verifyOtpAndInsert(otpInput: String) {
        // Unused stub in new confirmation model
    }

    fun cancelAddEmployee() {
        // Unused stub in new confirmation model
    }

    /**
     * Ganti password karyawan oleh Owner — langsung ke VPS via API.
     */
    fun changeEmployeePassword(employeeId: Long, newPassword: String) {
        if (newPassword.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val hashed = PinHasher.hash(newPassword)
                val resp = withContext(Dispatchers.IO) {
                    api.updateEmployee(employeeId, mapOf(
                        "pinHash" to hashed,
                        "passwordChangeCount" to 1
                    ))
                }
                val user = authRepository.getActiveUser()
                loadFromApi(user)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gagal ganti password: ${e.localizedMessage}") }
            }
        }
    }

    /**
     * Buka dialog ubah gaji.
     */
    fun openSalaryChangeDialog(employee: Employee) {
        if (employee.role == "OWNER") {
            _uiState.update { it.copy(error = "Owner tidak memiliki gaji dan tidak bisa diubah.") }
            return
        }
        _uiState.update {
            it.copy(
                showSalaryChangeDialog = true,
                activeEmployeeForSalaryChange = employee
            )
        }
    }

    fun dismissSalaryChangeDialog() {
        _uiState.update {
            it.copy(
                showSalaryChangeDialog = false,
                activeEmployeeForSalaryChange = null
            )
        }
    }

    /**
     * Ubah gaji dan siklus pembayaran karyawan — langsung ke VPS via API.
     */
    fun changeSalary(employeeId: Long, newSalary: Double, newPayPeriod: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resp = withContext(Dispatchers.IO) {
                    api.updateEmployee(employeeId, mapOf(
                        "salary" to newSalary,
                        "payPeriod" to newPayPeriod
                    ))
                }
                dismissSalaryChangeDialog()
                val user = authRepository.getActiveUser()
                loadFromApi(user)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gagal ubah gaji: ${e.localizedMessage}") }
            }
        }
    }

    /**
     * Bayar Gaji Karyawan — catat transaksi EXPENSE ke VPS via API.
     */
    fun paySalary(employee: Employee) {
        if (employee.role == "OWNER") {
            _uiState.update { it.copy(error = "Owner tidak dapat menerima pembayaran gaji.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val todayStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
                val receiptNumber = "EXP-PAY-$todayStr-${UUID.randomUUID().toString().take(6).uppercase()}"
                val currentOwner = authRepository.getActiveUser() ?: return@launch

                val txBody = mapOf<String, Any?>(
                    "outletId" to employee.outletId,
                    "employeeId" to currentOwner.googleSub.hashCode().toLong(),
                    "customerName" to "Payroll: ${employee.name}",
                    "receiptNumber" to receiptNumber,
                    "subtotal" to -employee.salary,
                    "total" to -employee.salary,
                    "paymentMethod" to "CASH",
                    "status" to "COMPLETED",
                    "type" to "EXPENSE",
                    "notes" to "Pembayaran gaji karyawan ${employee.name} (${employee.payPeriod})",
                    "date" to System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) { api.createTransaction(txBody) }

                // Update lastPaidAt karyawan
                withContext(Dispatchers.IO) {
                    api.updateEmployee(employee.id, mapOf("lastPaidAt" to System.currentTimeMillis()))
                }

                val user = authRepository.getActiveUser()
                loadFromApi(user)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memproses pembayaran gaji.") }
            }
        }
    }

    /**
     * Hapus karyawan (soft delete via API).
     */
    fun deleteEmployee(employeeId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO) { api.deleteEmployee(employeeId) }
                val user = authRepository.getActiveUser()
                loadFromApi(user)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Gagal menghapus karyawan.") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
