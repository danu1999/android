package com.posbah.app.ui.screens.owner.outlet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.remote.api.PosApiService
import com.posbah.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class OutletSummary(
    val outlet: Outlet,
    val totalStock: Int,
    val activeEmployeeName: String,
    val totalMargin: Double
)

data class MarginDataPoint(
    val dateStr: String,
    val marginA: Double,
    val marginB: Double,
    val marginC: Double
)

data class OutletControlUiState(
    val outlets: List<OutletSummary> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val marginHistory: List<MarginDataPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOwner: Boolean = false,
    val products: List<ProductEntity> = emptyList()
)

@HiltViewModel
class OutletControlViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
    private val api: PosApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()

    private val _uiState = MutableStateFlow(OutletControlUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val isOwner = authRepository.getActiveUser()?.role == "OWNER"
                val ownerEmail = authRepository.activeUserEmail()?.lowercase()?.trim()

                // ── Fetch outlets via API ──────────────────────────────────
                val outletResp = withContext(Dispatchers.IO) { api.getOutlets() }
                val rawOutlets = outletResp.body()?.map { m ->
                    Outlet(
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

                // ── Fetch products via API ─────────────────────────────────
                val productResp = withContext(Dispatchers.IO) { api.getProducts() }
                val allProducts = productResp.body()?.mapNotNull { m ->
                    val id = (m["id"] as? Number)?.toLong() ?: return@mapNotNull null
                    ProductEntity(
                        id = id,
                        tenantId = m["tenantId"] as? String ?: tenantId,
                        outletId = (m["outletId"] as? Number)?.toLong(),
                        name = m["name"] as? String ?: "",
                        price = (m["price"] as? Number)?.toDouble() ?: 0.0,
                        costPrice = (m["costPrice"] as? Number)?.toDouble() ?: 0.0,
                        stock = (m["stock"] as? Number)?.toInt() ?: 0,
                        unit = m["unit"] as? String ?: "pcs",
                        barcode = m["barcode"] as? String,
                        category = m["category"] as? String ?: "Umum",
                        image = (m["image"] ?: m["imageUrl"]) as? String,
                        isSynced = true
                    )
                } ?: emptyList()

                // ── Fetch employees via API ────────────────────────────────
                val empResp = withContext(Dispatchers.IO) { api.getEmployees() }
                val allEmployees = empResp.body()?.mapNotNull { m ->
                    val empId = (m["id"] as? Number)?.toLong() ?: return@mapNotNull null
                    val empRole = m["role"] as? String ?: "KASIR"
                    val empEmail = m["email"] as? String
                    if (empRole == "OWNER") return@mapNotNull null
                    if (!ownerEmail.isNullOrBlank() && empEmail?.lowercase() == ownerEmail) return@mapNotNull null
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

                // ── Fetch transactions via API untuk margin ────────────────
                val txResp = withContext(Dispatchers.IO) { api.getTransactions(limit = 1000) }
                val completedTransactions = txResp.body()?.filter {
                    it["status"] as? String == "COMPLETED"
                } ?: emptyList()

                // ── Build outlet summaries ─────────────────────────────────
                val summaries = rawOutlets.map { outlet ->
                    val isDefaultOutlet = outlet.isDefault
                    val outletProducts = allProducts.filter { p ->
                        p.outletId == outlet.id || (isDefaultOutlet && p.outletId == null)
                    }
                    val stockCount = outletProducts.sumOf { it.stock }

                    val assignedEmployees = allEmployees.filter { it.outletId == outlet.id && it.isActive }
                    val employeeName = if (assignedEmployees.isNotEmpty()) {
                        assignedEmployees.joinToString(", ") { it.name }
                    } else {
                        outlet.currentEmployee ?: "-"
                    }

                    // Margin: transactions tanpa items detail — pakai total saja (simplified)
                    val outletTxs = completedTransactions.filter { tx ->
                        val txOutletId = (tx["outletId"] as? Number)?.toLong()
                        txOutletId == outlet.id || (isDefaultOutlet && txOutletId == null)
                    }
                    val outletMargin = outletTxs.sumOf { (it["total"] as? Number)?.toDouble() ?: 0.0 }

                    OutletSummary(
                        outlet = outlet,
                        totalStock = stockCount,
                        activeEmployeeName = employeeName,
                        totalMargin = outletMargin
                    )
                }

                // ── Generate 7-day margin history ─────────────────────────
                val sdf = SimpleDateFormat("dd MMM", Locale.US)
                val days = (0..6).map { i ->
                    val dCal = Calendar.getInstance()
                    dCal.add(Calendar.DAY_OF_YEAR, -i)
                    dCal
                }.reversed()
                val outletAId = rawOutlets.getOrNull(0)?.id
                val outletBId = rawOutlets.getOrNull(1)?.id
                val outletCId = rawOutlets.getOrNull(2)?.id
                val defaultOutletId = rawOutlets.firstOrNull { it.isDefault }?.id

                val history = days.map { day ->
                    val dayStart = day.apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dayEnd = day.apply {
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val dateLabel = sdf.format(Date(dayStart))

                    val dayTxs = completedTransactions.filter { tx ->
                        val d = (tx["date"] as? Number)?.toLong() ?: 0
                        d in dayStart..dayEnd
                    }

                    fun calcForOutlet(oid: Long?): Double = dayTxs.filter { tx ->
                        val txOid = (tx["outletId"] as? Number)?.toLong()
                        txOid == oid || (oid == defaultOutletId && txOid == null)
                    }.sumOf { (it["total"] as? Number)?.toDouble() ?: 0.0 }

                    MarginDataPoint(
                        dateStr = dateLabel,
                        marginA = calcForOutlet(outletAId),
                        marginB = calcForOutlet(outletBId),
                        marginC = calcForOutlet(outletCId)
                    )
                }

                _uiState.update {
                    it.copy(
                        outlets = summaries,
                        employees = allEmployees,
                        marginHistory = history,
                        isLoading = false,
                        isOwner = isOwner,
                        products = allProducts
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("OutletControlVM", "loadData error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Gagal memuat data") }
            }
        }
    }

    fun toggleOutletStatus(outletId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val current = _uiState.value.outlets.find { it.outlet.id == outletId }?.outlet ?: return@launch
                withContext(Dispatchers.IO) {
                    api.updateOutlet(outletId, mapOf("isOpen" to !current.isOpen))
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gagal ubah status outlet: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun deleteOutlet(outletId: Long) {
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            if (user?.role != "OWNER") {
                _uiState.update { it.copy(error = "Akses ditolak: Hanya OWNER yang dapat menghapus outlet.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Unassign employees dari outlet ini
                val assignedEmps = _uiState.value.employees.filter { it.outletId == outletId }
                assignedEmps.forEach { emp ->
                    withContext(Dispatchers.IO) {
                        api.updateEmployee(emp.id, mapOf("outletId" to null))
                    }
                }
                withContext(Dispatchers.IO) { api.deleteOutlet(outletId) }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gagal hapus outlet: ${e.localizedMessage}") }
            }
        }
    }

    fun assignEmployeesToOutlet(outletId: Long, employeeIds: List<Long>) {
        viewModelScope.launch {
            if (employeeIds.size > 10) {
                _uiState.update { it.copy(error = "Maksimal 10 karyawan per outlet.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allEmps = _uiState.value.employees
                // Unassign employees yang sebelumnya di outlet ini tapi tidak dipilih lagi
                val currentAssigned = allEmps.filter { it.outletId == outletId }
                currentAssigned.forEach { emp ->
                    if (emp.id !in employeeIds) {
                        withContext(Dispatchers.IO) {
                            api.updateEmployee(emp.id, mapOf("outletId" to null))
                        }
                    }
                }
                // Assign newly selected
                employeeIds.forEach { empId ->
                    val emp = allEmps.firstOrNull { it.id == empId }
                    if (emp != null && emp.outletId != outletId) {
                        withContext(Dispatchers.IO) {
                            api.updateEmployee(emp.id, mapOf("outletId" to outletId))
                        }
                    }
                }
                // Update currentEmployee nama di outlet
                val selectedNames = allEmps.filter { it.id in employeeIds }.joinToString(", ") { it.name }
                withContext(Dispatchers.IO) {
                    api.updateOutlet(outletId, mapOf("currentEmployee" to selectedNames.takeIf { it.isNotBlank() }))
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gagal assign karyawan: ${e.localizedMessage}") }
            }
        }
    }

    fun assignEmployee(outletId: Long, employeeName: String) {
        viewModelScope.launch {
            val allEmps = _uiState.value.employees
            if (employeeName.isBlank()) {
                // Unassign semua dari outlet ini
                val currentAssigned = allEmps.filter { it.outletId == outletId }
                currentAssigned.forEach { emp ->
                    withContext(Dispatchers.IO) {
                        api.updateEmployee(emp.id, mapOf("outletId" to null))
                    }
                }
                withContext(Dispatchers.IO) {
                    api.updateOutlet(outletId, mapOf("currentEmployee" to null))
                }
                loadData()
            } else {
                val emp = allEmps.firstOrNull { it.name == employeeName } ?: return@launch
                val currentAssigned = allEmps.filter { it.outletId == outletId }.map { it.id }.toMutableList()
                if (emp.id !in currentAssigned) {
                    if (currentAssigned.size >= 10) return@launch
                    currentAssigned.add(emp.id)
                }
                assignEmployeesToOutlet(outletId, currentAssigned)
            }
        }
    }

    fun createOutlet(name: String, address: String?, phone: String?, employeeIds: List<Long>) {
        viewModelScope.launch {
            val existingCount = _uiState.value.outlets.size
            if (existingCount >= 3) {
                _uiState.update { it.copy(error = "Maksimum outlet dibatasi 3.") }
                return@launch
            }
            if (name.isBlank()) {
                _uiState.update { it.copy(error = "Nama outlet tidak boleh kosong.") }
                return@launch
            }
            if (employeeIds.isEmpty()) {
                _uiState.update { it.copy(error = "Gagal: Outlet baru harus memiliki minimal 1 karyawan.") }
                return@launch
            }
            if (employeeIds.size > 10) {
                _uiState.update { it.copy(error = "Gagal: Maksimal 10 karyawan per outlet.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                val isDefault = existingCount == 0
                val selectedNames = _uiState.value.employees
                    .filter { it.id in employeeIds }
                    .joinToString(", ") { it.name }

                val resp = withContext(Dispatchers.IO) {
                    api.createOutlet(mapOf(
                        "name" to name,
                        "address" to address,
                        "phone" to phone,
                        "isDefault" to isDefault,
                        "isOpen" to true,
                        "currentEmployee" to selectedNames.takeIf { it.isNotBlank() }
                    ))
                }
                val newOutletId = (resp.body()?.get("id") as? Number)?.toLong()
                if (newOutletId != null && employeeIds.isNotEmpty()) {
                    employeeIds.forEach { empId ->
                        withContext(Dispatchers.IO) {
                            api.updateEmployee(empId, mapOf("outletId" to newOutletId))
                        }
                    }
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gagal buat outlet: ${e.localizedMessage}") }
            }
        }
    }

    fun updateOutlet(outletId: Long, name: String, address: String?, phone: String?, employeeIds: List<Long>) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiState.update { it.copy(error = "Nama outlet tidak boleh kosong.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allEmps = _uiState.value.employees
                // Unassign yang tidak dipilih lagi
                allEmps.filter { it.outletId == outletId && it.id !in employeeIds }.forEach { emp ->
                    withContext(Dispatchers.IO) { api.updateEmployee(emp.id, mapOf("outletId" to null)) }
                }
                // Assign yang dipilih
                employeeIds.forEach { empId ->
                    val emp = allEmps.firstOrNull { it.id == empId }
                    if (emp != null && emp.outletId != outletId) {
                        withContext(Dispatchers.IO) { api.updateEmployee(emp.id, mapOf("outletId" to outletId)) }
                    }
                }
                val selectedNames = allEmps.filter { it.id in employeeIds }.joinToString(", ") { it.name }
                withContext(Dispatchers.IO) {
                    api.updateOutlet(outletId, mapOf(
                        "name" to name,
                        "address" to address,
                        "phone" to phone,
                        "currentEmployee" to selectedNames.takeIf { it.isNotBlank() }
                    ))
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Gagal update outlet: ${e.localizedMessage}") }
            }
        }
    }

    fun transferStock(
        sourceOutletId: Long,
        destOutletId: Long,
        productId: Long,
        qty: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (sourceOutletId == destOutletId) { onError("Outlet asal dan tujuan tidak boleh sama."); return@launch }
                if (qty <= 0) { onError("Jumlah transfer harus lebih dari 0."); return@launch }

                val products = _uiState.value.products
                val sourceProduct = products.firstOrNull {
                    it.id == productId && (it.outletId == sourceOutletId || (_uiState.value.outlets.find { o -> o.outlet.id == sourceOutletId }?.outlet?.isDefault == true && it.outletId == null))
                } ?: run { onError("Produk asal tidak ditemukan."); return@launch }

                if (sourceProduct.stock < qty) {
                    onError("Stok produk tidak mencukupi (Tersedia: ${sourceProduct.stock}).")
                    return@launch
                }

                // Kurangi stok source
                withContext(Dispatchers.IO) {
                    api.updateProduct(sourceProduct.id, mapOf("stock" to (sourceProduct.stock - qty)))
                }

                // Tambah stok di dest
                val destProduct = products.firstOrNull { p ->
                    p.name.equals(sourceProduct.name, ignoreCase = true) &&
                    (p.outletId == destOutletId || (_uiState.value.outlets.find { o -> o.outlet.id == destOutletId }?.outlet?.isDefault == true && p.outletId == null))
                }
                if (destProduct != null) {
                    withContext(Dispatchers.IO) {
                        api.updateProduct(destProduct.id, mapOf("stock" to (destProduct.stock + qty)))
                    }
                } else {
                    // Buat produk baru di outlet tujuan
                    withContext(Dispatchers.IO) {
                        api.createProduct(mapOf(
                            "name" to sourceProduct.name,
                            "price" to sourceProduct.price,
                            "costPrice" to sourceProduct.costPrice,
                            "stock" to qty,
                            "unit" to sourceProduct.unit,
                            "barcode" to sourceProduct.barcode,
                            "category" to sourceProduct.category,
                            "image" to sourceProduct.image,
                            "outletId" to destOutletId
                        ))
                    }
                }

                loadData()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal melakukan transfer stok.")
            }
        }
    }
}
