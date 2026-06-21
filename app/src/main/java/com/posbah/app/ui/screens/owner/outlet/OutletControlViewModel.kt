package com.posbah.app.ui.screens.owner.outlet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()

    private val _uiState = MutableStateFlow(OutletControlUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Collect DB changes and refresh data reactively
        viewModelScope.launch {
            combine(
                db.outletDao().observeForTenant(tenantId),
                db.productDao().observe(tenantId),
                db.employeeDao().observeForTenant(tenantId),
                db.transactionDao().observe(tenantId)
            ) { _, _, _, _ ->
                // Trigger reload on database change
            }.collect {
                loadData()
            }
        }

        // Trigger background pull sync
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.posbah.app.data.remote.SupabaseSyncManager.pullAll(context, db, tenantId)
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch outlets
                val rawOutlets = db.outletDao().listForTenant(tenantId)
                
                // Fetch products to calculate stock (all products for this tenant)
                val allProducts = db.productDao().list(tenantId)
                
                // Fetch employees for assignment list — strictly scoped to this tenant only
                val ownerEmail = authRepository.activeUserEmail()?.lowercase()?.trim()
                val allEmployees = db.employeeDao().getAll()
                    .filter { emp ->
                        emp.tenantId == tenantId &&
                        emp.isActive &&
                        emp.role != "OWNER" &&
                        emp.email?.lowercase()?.trim() != ownerEmail
                    }


                // Fetch transactions and items for margin calculation
                val completedTransactions = db.transactionDao().getAll()
                    .filter { it.tenantId == tenantId && it.status == "COMPLETED" }
                val transactionItems = db.transactionItemDao().getAll()
                val itemsByTxId = transactionItems.groupBy { it.transactionId }

                // Map outlets to summary
                val summaries = rawOutlets.map { outlet ->
                    val isDefaultOutlet = outlet.isDefault
                    // Count stock. If product outletId is null, map it to the default outlet.
                    val outletProducts = allProducts.filter { p ->
                        p.outletId == outlet.id || (isDefaultOutlet && p.outletId == null)
                    }
                    val stockCount = outletProducts.sumOf { it.stock }

                    // Find active employee name
                    val employeeName = outlet.currentEmployee 
                        ?: allEmployees.firstOrNull { it.outletId == outlet.id }?.name 
                        ?: "-"

                    // Calculate total margin for this outlet
                    val outletTxs = completedTransactions.filter { tx ->
                        tx.outletId == outlet.id || (isDefaultOutlet && tx.outletId == null)
                    }
                    val outletMargin = outletTxs.sumOf { tx ->
                        val items = itemsByTxId[tx.id] ?: emptyList()
                        val cost = items.sumOf { it.costPrice * it.quantity }
                        tx.total - cost
                    }

                    OutletSummary(
                        outlet = outlet,
                        totalStock = stockCount,
                        activeEmployeeName = employeeName,
                        totalMargin = outletMargin
                    )
                }

                // Generate 7 Days Margin history comparison
                val sdf = SimpleDateFormat("dd MMM", Locale.US)
                val cal = Calendar.getInstance()
                val history = mutableListOf<MarginDataPoint>()

                // We want the last 7 days
                val days = (0..6).map { i ->
                    val dCal = Calendar.getInstance()
                    dCal.add(Calendar.DAY_OF_YEAR, -i)
                    dCal
                }.reversed()

                // Identify Outlet IDs corresponding to Outlet A, B, C (first 3 outlets)
                val outletAId = rawOutlets.getOrNull(0)?.id
                val outletBId = rawOutlets.getOrNull(1)?.id
                val outletCId = rawOutlets.getOrNull(2)?.id
                
                val defaultOutletId = rawOutlets.firstOrNull { it.isDefault }?.id

                days.forEach { day ->
                    val dayStart = day.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val dayEnd = day.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val dateLabel = sdf.format(Date(dayStart))

                    val dayTxs = completedTransactions.filter { it.date in dayStart..dayEnd }

                    // Function to calculate margin for a list of transactions
                    fun calcMargin(txs: List<com.posbah.app.data.local.entities.TransactionEntity>): Double {
                        return txs.sumOf { tx ->
                            val items = itemsByTxId[tx.id] ?: emptyList()
                            val cost = items.sumOf { it.costPrice * it.quantity }
                            tx.total - cost
                        }
                    }

                    val marginA = calcMargin(dayTxs.filter { tx ->
                        tx.outletId == outletAId || (outletAId == defaultOutletId && tx.outletId == null)
                    })

                    val marginB = calcMargin(dayTxs.filter { tx ->
                        tx.outletId == outletBId || (outletBId == defaultOutletId && tx.outletId == null)
                    })

                    val marginC = calcMargin(dayTxs.filter { tx ->
                        tx.outletId == outletCId || (outletCId == defaultOutletId && tx.outletId == null)
                    })

                    history.add(
                        MarginDataPoint(
                            dateStr = dateLabel,
                            marginA = marginA,
                            marginB = marginB,
                            marginC = marginC
                        )
                    )
                }

                val isOwner = authRepository.getActiveUser()?.role == "OWNER"
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
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load data") }
            }
        }
    }

    fun toggleOutletStatus(outletId: Long) {
        viewModelScope.launch {
            val outlet = db.outletDao().getById(outletId) ?: return@launch
            val updated = outlet.copy(isOpen = !outlet.isOpen, isSynced = false, updatedAt = System.currentTimeMillis())
            db.outletDao().update(updated)
            loadData()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
            val outlet = db.outletDao().getById(outletId) ?: return@launch

            // Safe sync unlink: set outletId to null for all employees in this outlet
            val employees = db.employeeDao().getAll().filter { it.outletId == outletId }
            employees.forEach { emp ->
                db.employeeDao().update(emp.copy(outletId = null, isSynced = false, updatedAt = System.currentTimeMillis()))
            }

            db.outletDao().delete(outletId)
            loadData()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.deleteRow(context, "outlets", outletId, tenantId)
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun assignEmployee(outletId: Long, employeeName: String) {
        viewModelScope.launch {
            val ownerEmail = authRepository.activeUserEmail()?.lowercase()?.trim()
            val activeEmployees = db.employeeDao().getAll().filter { emp ->
                emp.tenantId == tenantId &&
                emp.isActive &&
                emp.role != "OWNER" &&
                emp.email?.lowercase()?.trim() != ownerEmail
            }

            if (employeeName.isBlank()) {
                val employeesOfThisOutlet = activeEmployees.filter { it.outletId == outletId }
                if (employeesOfThisOutlet.size <= 1) {
                    _uiState.update { it.copy(error = "Gagal: Outlet harus memiliki minimal 1 karyawan.") }
                    return@launch
                }
                val outlet = db.outletDao().getById(outletId) ?: return@launch
                val updated = outlet.copy(currentEmployee = null, isSynced = false, updatedAt = System.currentTimeMillis())
                db.outletDao().update(updated)
            } else {
                val emp = activeEmployees.firstOrNull { it.name == employeeName }
                if (emp == null) {
                    _uiState.update { it.copy(error = "Karyawan tidak ditemukan.") }
                    return@launch
                }
                if (emp.outletId != outletId) {
                    if (emp.outletId != null) {
                        val oldOutletCount = activeEmployees.count { it.outletId == emp.outletId }
                        if (oldOutletCount <= 1) {
                            val oldOutletName = db.outletDao().getById(emp.outletId)?.name ?: "Outlet Lain"
                            _uiState.update { it.copy(error = "Gagal: ${emp.name} adalah karyawan terakhir di $oldOutletName.") }
                            return@launch
                        }
                    }
                    val targetCount = activeEmployees.count { it.outletId == outletId }
                    if (targetCount >= 10) {
                        _uiState.update { it.copy(error = "Gagal: Outlet sudah mencapai batas maksimal 10 karyawan.") }
                        return@launch
                    }
                    db.employeeDao().update(emp.copy(outletId = outletId, isSynced = false, updatedAt = System.currentTimeMillis()))
                }
                val outlet = db.outletDao().getById(outletId) ?: return@launch
                val updated = outlet.copy(currentEmployee = employeeName, isSynced = false, updatedAt = System.currentTimeMillis())
                db.outletDao().update(updated)
            }
            loadData()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun createOutlet(name: String, address: String?, phone: String?, currentEmployee: String?) {
        viewModelScope.launch {
            val existing = db.outletDao().listForTenant(tenantId)
            if (existing.size >= 3) {
                _uiState.update { it.copy(error = "Maksimum outlet dibatasi 3.") }
                return@launch
            }
            if (name.isBlank()) {
                _uiState.update { it.copy(error = "Nama outlet tidak boleh kosong.") }
                return@launch
            }
            if (currentEmployee.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Gagal: Outlet baru harus memiliki minimal 1 karyawan.") }
                return@launch
            }

            val ownerEmail = authRepository.activeUserEmail()?.lowercase()?.trim()
            val activeEmployees = db.employeeDao().getAll().filter { emp ->
                emp.tenantId == tenantId &&
                emp.isActive &&
                emp.role != "OWNER" &&
                emp.email?.lowercase()?.trim() != ownerEmail
            }

            val emp = activeEmployees.firstOrNull { it.name == currentEmployee }
            if (emp == null) {
                _uiState.update { it.copy(error = "Karyawan tidak ditemukan.") }
                return@launch
            }

            if (emp.outletId != null) {
                val oldOutletCount = activeEmployees.count { it.outletId == emp.outletId }
                if (oldOutletCount <= 1) {
                    val oldOutletName = db.outletDao().getById(emp.outletId)?.name ?: "Outlet Lain"
                    _uiState.update { it.copy(error = "Gagal: ${emp.name} adalah karyawan terakhir di $oldOutletName.") }
                    return@launch
                }
            }

            val isDefault = existing.isEmpty()
            val newOutlet = Outlet(
                tenantId = tenantId,
                name = name,
                address = address,
                phone = phone,
                currentEmployee = currentEmployee,
                isOpen = true,
                isDefault = isDefault
            )
            val newOutletId = db.outletDao().insert(newOutlet)
            db.employeeDao().update(emp.copy(outletId = newOutletId, isSynced = false, updatedAt = System.currentTimeMillis()))
            
            loadData()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateOutlet(outletId: Long, name: String, address: String?, phone: String?, currentEmployee: String?) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiState.update { it.copy(error = "Nama outlet tidak boleh kosong.") }
                return@launch
            }
            val existing = db.outletDao().getById(outletId) ?: return@launch

            val ownerEmail = authRepository.activeUserEmail()?.lowercase()?.trim()
            val activeEmployees = db.employeeDao().getAll().filter { emp ->
                emp.tenantId == tenantId &&
                emp.isActive &&
                emp.role != "OWNER" &&
                emp.email?.lowercase()?.trim() != ownerEmail
            }

            if (currentEmployee.isNullOrBlank()) {
                val targetCount = activeEmployees.count { it.outletId == outletId }
                if (targetCount <= 1) {
                    _uiState.update { it.copy(error = "Gagal: Outlet harus memiliki minimal 1 karyawan.") }
                    return@launch
                }
                val updated = existing.copy(
                    name = name,
                    address = address,
                    phone = phone,
                    currentEmployee = null,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                db.outletDao().update(updated)
            } else {
                val emp = activeEmployees.firstOrNull { it.name == currentEmployee }
                if (emp == null) {
                    _uiState.update { it.copy(error = "Karyawan tidak ditemukan.") }
                    return@launch
                }

                if (emp.outletId != outletId) {
                    if (emp.outletId != null) {
                        val oldOutletCount = activeEmployees.count { it.outletId == emp.outletId }
                        if (oldOutletCount <= 1) {
                            val oldOutletName = db.outletDao().getById(emp.outletId)?.name ?: "Outlet Lain"
                            _uiState.update { it.copy(error = "Gagal: ${emp.name} adalah karyawan terakhir di $oldOutletName.") }
                            return@launch
                        }
                    }
                    val targetCount = activeEmployees.count { it.outletId == outletId }
                    if (targetCount >= 10) {
                        _uiState.update { it.copy(error = "Gagal: Outlet sudah mencapai batas maksimal 10 karyawan.") }
                        return@launch
                    }
                    db.employeeDao().update(emp.copy(outletId = outletId, isSynced = false, updatedAt = System.currentTimeMillis()))
                }

                val updated = existing.copy(
                    name = name,
                    address = address,
                    phone = phone,
                    currentEmployee = currentEmployee,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                db.outletDao().update(updated)
            }

            loadData()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                if (sourceOutletId == destOutletId) {
                    onError("Outlet asal dan tujuan tidak boleh sama.")
                    return@launch
                }
                if (qty <= 0) {
                    onError("Jumlah transfer harus lebih dari 0.")
                    return@launch
                }
                val product = db.productDao().getById(productId)
                if (product == null) {
                    onError("Produk asal tidak ditemukan.")
                    return@launch
                }
                if (product.stock < qty) {
                    onError("Stok produk tidak mencukupi (Tersedia: ${product.stock}).")
                    return@launch
                }

                val sourceOutlet = db.outletDao().getById(sourceOutletId)
                val destOutlet = db.outletDao().getById(destOutletId)
                if (sourceOutlet == null || destOutlet == null) {
                    onError("Outlet tidak ditemukan.")
                    return@launch
                }

                // Deduct source stock
                val newSourceStock = product.stock - qty
                db.productDao().updateStock(product.id, newSourceStock)

                // Find or create destination product
                val allTenantProducts = db.productDao().list(tenantId)
                val isDestDefault = destOutlet.isDefault
                val destProduct = allTenantProducts.firstOrNull { p ->
                    p.name.equals(product.name, ignoreCase = true) &&
                    (p.outletId == destOutletId || (isDestDefault && p.outletId == null))
                }

                if (destProduct != null) {
                    db.productDao().updateStock(destProduct.id, destProduct.stock + qty)
                } else {
                    val newProduct = product.copy(
                        id = 0,
                        outletId = destOutletId,
                        stock = qty,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false
                    )
                    db.productDao().upsert(newProduct)
                }

                // Log activity
                db.activityLogDao().insertLog(
                    com.posbah.app.data.local.entities.ActivityLogEntity(
                        tenantId = tenantId,
                        action = "STOCK_TRANSFER",
                        description = "Owner mentransfer $qty ${product.unit} ${product.name} dari ${sourceOutlet.name} ke ${destOutlet.name}",
                        date = System.currentTimeMillis(),
                        employeeName = authRepository.getActiveUser()?.displayName ?: "Owner",
                        appMode = "FNB"
                    )
                )

                loadData()
                onSuccess()

                // Trigger background sync
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal melakukan transfer stok.")
            }
        }
    }
}
