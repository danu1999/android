package com.posbah.app.ui.screens.owner.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.security.PinHasher
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val pendingEmployee: Employee? = null
)

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
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
            _uiState.update { it.copy(isLoading = true) }
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

            val outletsList = db.outletDao().listForTenant(tenantId)

            db.employeeDao().observeForTenant(tenantId).collect { list ->
                val filteredList = list.filter { emp ->
                    emp.role != "OWNER" && emp.email?.lowercase()?.trim() != user?.email?.lowercase()?.trim()
                }
                _uiState.update { 
                    it.copy(
                        employees = filteredList,
                        outlets = outletsList,
                        isOwner = true,
                        isLoading = false
                    ) 
                }
            }
        }
    }

    /**
     * Pendaftaran karyawan baru dengan verifikasi email via server.
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
            val cleanEmail = email.lowercase().trim()
            // Check if email already registered as employee
            val existing = db.employeeDao().findByEmail(cleanEmail)
            if (existing != null) {
                _uiState.update { it.copy(error = "Email sudah digunakan oleh karyawan lain (FnB/Rental/Laundry).") }
                return@launch
            }

            // Check if email registered as owner
            val existingOwner = db.localUserDao().getByEmail(cleanEmail)
            if (existingOwner != null && existingOwner.role == "OWNER") {
                _uiState.update { it.copy(error = "Email ini terdaftar sebagai Owner.") }
                return@launch
            }

            val hashedPassword = PinHasher.hash(password)
            val newEmp = Employee(
                tenantId = tenantId,
                outletId = outletId,
                name = name,
                email = cleanEmail,
                phone = phone.trim().takeIf { it.isNotBlank() },
                role = role,
                pinHash = hashedPassword,
                salary = salary,
                payPeriod = payPeriod,
                emailVerified = false
            )

            db.employeeDao().insert(newEmp)
            
            // Clear status error
            _uiState.update { it.copy(error = null) }
            
            // Refresh list
            checkPermissionAndLoad()

            // Trigger auto-sync to VPS with raw password
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncEmployeeWithRawPassword(context, db, tenantId, cleanEmail, password)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
     * Ganti password karyawan oleh Owner.
     */
    fun changeEmployeePassword(employeeId: Long, newPassword: String) {
        if (newPassword.isBlank()) return
        viewModelScope.launch {
            val emp = db.employeeDao().getById(employeeId) ?: return@launch
            val hashed = PinHasher.hash(newPassword)
            val updated = emp.copy(pinHash = hashed, updatedAt = System.currentTimeMillis())
            db.employeeDao().update(updated)
            checkPermissionAndLoad()

            val ownerEmail = authRepository.activeUserEmail().orEmpty()

            // Trigger auto-sync to VPS
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncEmployeePasswordChange(
                        context,
                        db,
                        tenantId,
                        emp.email.orEmpty(),
                        newPassword,
                        ownerEmail
                    )
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Bayar Gaji Karyawan: mengurangi kas owner
     * (Menyisipkan entri pengeluaran negatif ke tabel transaksi).
     */
    fun paySalary(employee: Employee) {
        if (employee.role == "OWNER") {
            _uiState.update { it.copy(error = "Owner tidak dapat menerima pembayaran gaji.") }
            return
        }
        viewModelScope.launch {
            try {
                // Generate expense transaction
                val todayStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
                val receiptNumber = "EXP-PAY-$todayStr-${UUID.randomUUID().toString().take(6).uppercase()}"
                
                val currentOwner = authRepository.getActiveUser() ?: return@launch

                // Gaji karyawan dicatat sebagai transaksi EXPENSE dengan outletId karyawan
                val expenseTx = TransactionEntity(
                    tenantId = tenantId,
                    outletId = employee.outletId, // <- isolasi: catat ke outlet karyawan tersebut
                    employeeId = currentOwner.googleSub.hashCode().toLong(),
                    customerName = "Payroll: ${employee.name}",
                    receiptNumber = receiptNumber,
                    subtotal = -employee.salary,
                    total = -employee.salary,
                    paymentMethod = "CASH",
                    status = "COMPLETED",
                    type = "EXPENSE",
                    notes = "Pembayaran gaji karyawan ${employee.name} (${employee.payPeriod})"
                )

                db.transactionDao().insert(expenseTx)

                // Update employee payroll status
                val updatedEmp = employee.copy(
                    lastPaidAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                db.employeeDao().update(updatedEmp)

                // Log payroll activity dengan info outlet
                val outletName = employee.outletId?.let { oid ->
                    db.outletDao().getById(oid)?.name
                } ?: "Outlet Utama"
                db.activityLogDao().insertLog(
                    com.posbah.app.data.local.entities.ActivityLogEntity(
                        tenantId = tenantId,
                        action = "PAY_ROLL",
                        description = "Bayar gaji ${employee.name} (Outlet: $outletName) sebesar Rp ${employee.salary}",
                        date = System.currentTimeMillis(),
                        employeeName = currentOwner.displayName ?: "Owner",
                        appMode = "FNB"
                    )
                )

                // Reload list
                checkPermissionAndLoad()

                // Trigger auto-sync to VPS
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Gagal memproses pembayaran gaji.") }
            }
        }
    }

    /**
     * Hapus karyawan (soft delete) oleh Owner.
     */
    fun deleteEmployee(employeeId: Long) {
        viewModelScope.launch {
            try {
                db.employeeDao().softDelete(employeeId)
                checkPermissionAndLoad()

                // Trigger auto-sync to VPS
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Gagal menghapus karyawan.") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
