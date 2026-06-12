package com.posbah.app.ui.screens.owner.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.security.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val db: PosBahDatabase
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
     * Pendaftaran karyawan baru dengan verifikasi email simulasi.
     * Memicu pengiriman kode OTP 6 digit.
     */
    fun startAddEmployee(
        name: String,
        email: String,
        phone: String,
        pin: String,
        role: String,
        salary: Double,
        payPeriod: String,
        outletId: Long?
    ) {
        if (name.isBlank() || email.isBlank() || pin.isBlank()) {
            _uiState.update { it.copy(error = "Nama, Email, dan PIN wajib diisi.") }
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

            val otp = String.format("%06d", Random.nextInt(100000, 999999))
            val hashedPin = PinHasher.hash(pin)
            val newEmp = Employee(
                tenantId = tenantId,
                outletId = outletId,
                name = name,
                email = cleanEmail,
                phone = phone.trim().takeIf { it.isNotBlank() },
                role = role,
                pinHash = hashedPin,
                salary = salary,
                payPeriod = payPeriod,
                emailVerified = false
            )

            _uiState.update { 
                it.copy(
                    emailVerificationOtp = otp,
                    pendingEmployee = newEmp,
                    error = null
                ) 
            }
        }
    }

    /**
     * Konfirmasi verifikasi kode OTP email karyawan.
     */
    fun verifyOtpAndInsert(otpInput: String) {
        val currentState = _uiState.value
        val correctOtp = currentState.emailVerificationOtp
        val emp = currentState.pendingEmployee

        if (correctOtp == null || emp == null) {
            _uiState.update { it.copy(error = "Tidak ada proses registrasi berjalan.") }
            return
        }

        if (otpInput != correctOtp) {
            _uiState.update { it.copy(error = "Kode verifikasi salah.") }
            return
        }

        viewModelScope.launch {
            try {
                val verifiedEmp = emp.copy(
                    emailVerified = true,
                    updatedAt = System.currentTimeMillis()
                )
                db.employeeDao().insert(verifiedEmp)
                
                // Clear state
                _uiState.update { 
                    it.copy(
                        emailVerificationOtp = null,
                        pendingEmployee = null,
                        error = null
                    ) 
                }
                
                // Refresh list
                checkPermissionAndLoad()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Gagal mendaftarkan karyawan.") }
            }
        }
    }

    fun cancelAddEmployee() {
        _uiState.update { 
            it.copy(
                emailVerificationOtp = null,
                pendingEmployee = null
            ) 
        }
    }

    /**
     * Ganti PIN/password karyawan oleh Owner.
     */
    fun changeEmployeePin(employeeId: Long, newPin: String) {
        if (newPin.isBlank()) return
        viewModelScope.launch {
            val emp = db.employeeDao().getById(employeeId) ?: return@launch
            val hashed = PinHasher.hash(newPin)
            val updated = emp.copy(pinHash = hashed, updatedAt = System.currentTimeMillis())
            db.employeeDao().update(updated)
            checkPermissionAndLoad()
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
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Gagal memproses pembayaran gaji.") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
