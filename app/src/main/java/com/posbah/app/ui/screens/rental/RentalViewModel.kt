package com.posbah.app.ui.screens.rental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.LocalDataSeeder
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import com.posbah.app.data.local.dao.ActivityLogDao
import com.posbah.app.data.local.entities.ActivityLogEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.ProductRepository
import com.posbah.app.data.repository.SessionState
import com.posbah.app.data.repository.TransactionRepository
import com.posbah.app.util.CameraUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class VehicleRentalInfo(
    val renterName: String,
    val expiry: Long,
    val days: Int,
    val receiptNumber: String
)

@Serializable
data class ProductWholesaleMetadata(
    val monthlyMaintenance: Double = 0.0
)

@HiltViewModel
class RentalViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionState: SessionState,
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val localDataSeeder: LocalDataSeeder,
    private val activityLogDao: ActivityLogDao,
    private val db: com.posbah.app.data.local.PosBahDatabase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val currentOutletId get() = sessionState.outletId.value

    val tenantName = flow {
        val t = db.tenantDao().getById(tenantId)
        emit(t?.name ?: "Rental POS")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Rental POS")

    init {
        viewModelScope.launch {
            if (sessionState.outletId.value == null) {
                val outlets = db.outletDao().listForTenant(tenantId)
                val defaultOutlet = outlets.firstOrNull { it.isDefault } ?: outlets.firstOrNull()
                if (defaultOutlet != null) {
                    sessionState.setOutlet(defaultOutlet.id)
                }
            }
        }
        // Trigger background pull sync
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.posbah.app.data.remote.SupabaseSyncManager.pullAll(appContext, db, tenantId)
        }
    }

    val availableOutlets = db.outletDao().observeForTenant(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeOutletId = sessionState.outletId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectOutlet(id: Long?) {
        val owner = isOwner.value
        if (!owner) {
            android.util.Log.w("RentalViewModel", "selectOutlet() ditolak: hanya OWNER yang bisa ganti outlet.")
            return
        }
        sessionState.setOutlet(id)
    }

    val products = kotlinx.coroutines.flow.combine(
        productRepository.observe(tenantId),
        sessionState.outletId,
        db.outletDao().observeForTenant(tenantId)
    ) { allProducts, activeOutletId, outlets ->
        val defaultOutletId = outlets.firstOrNull { it.isDefault }?.id
        allProducts.filter { p ->
            p.outletId == activeOutletId || (activeOutletId == defaultOutletId && p.outletId == null)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transactions = kotlinx.coroutines.flow.combine(
        transactionRepository.observe(tenantId),
        sessionState.outletId,
        db.outletDao().observeForTenant(tenantId)
    ) { allTransactions, activeOutletId, outlets ->
        val defaultOutletId = outlets.firstOrNull { it.isDefault }?.id
        allTransactions.filter { t ->
            t.outletId == activeOutletId || (activeOutletId == defaultOutletId && t.outletId == null)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customers = db.customerDao().observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isOwner = flow {
        val user = authRepository.getActiveUser()
        emit(user?.role == "OWNER")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canViewMargin = flow {
        val user = authRepository.getActiveUser()
        emit(user?.role == "OWNER")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activityLogs = activityLogDao.observeLogs(tenantId, "RENTAL")
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val vehicles: StateFlow<List<Vehicle>> = combine(products, transactions) { prodList, txList ->
        val filtered = prodList.filter { it.category == "MOBIL" || it.category == "MOTOR" }
        
        if (filtered.isEmpty() && prodList.isEmpty()) {
            viewModelScope.launch {
                localDataSeeder.seedDefaultVehicles(tenantId, currentOutletId)
            }
        }
        
        filtered.map { p ->
            var activeRenter: String? = null
            var expiry: Long? = null
            
            if (p.stock == 0 && !p.variants.isNullOrBlank()) {
                try {
                    val info = Json.decodeFromString(VehicleRentalInfo.serializer(), p.variants)
                    activeRenter = info.renterName
                    expiry = info.expiry
                } catch (e: Exception) {
                    // parse fallback
                }
            }

            val monthlyMaint = if (!p.wholesalePrices.isNullOrBlank()) {
                try {
                    val meta = Json.decodeFromString(ProductWholesaleMetadata.serializer(), p.wholesalePrices)
                    meta.monthlyMaintenance
                } catch (e: Exception) {
                    0.0
                }
            } else {
                0.0
            }
            
            Vehicle(
                id = p.id.toString(),
                name = p.name,
                plateNumber = p.barcode.orEmpty(),
                type = p.category,
                pricePerDay = p.price,
                costPrice = p.costPrice,
                image = p.image,
                isRented = p.stock == 0,
                activeRenterName = activeRenter,
                rentExpiry = expiry,
                monthlyMaintenance = monthlyMaint
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rentalOrders: StateFlow<List<RentalOrder>> = combine(transactions, products) { txList, prodList ->
        txList.filter { it.receiptNumber.startsWith("RN-") }.map { tx ->
            val vehicle = prodList.find { it.id == tx.customerId }
            RentalOrder(
                id = tx.receiptNumber,
                vehicleId = tx.customerId?.toString().orEmpty(),
                vehicleName = vehicle?.name ?: "Kendaraan Terhapus",
                customerName = tx.customerName.orEmpty(),
                whatsapp = tx.notes.orEmpty(),
                days = tx.queueNumber ?: 1,
                total = tx.total,
                paid = tx.amountPaid ?: tx.total,
                rentDate = tx.date,
                status = tx.orderStatus ?: "ACTIVE"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addVehicle(
        name: String,
        plateNumber: String,
        type: String,
        pricePerDay: Double,
        costPrice: Double,
        monthlyMaintenance: Double,
        imageFile: java.io.File?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            var compressedPath: String? = null
            if (imageFile != null && imageFile.exists()) {
                val compressed = CameraUtils.compressToMaxSize(imageFile, 100)
                compressedPath = compressed.absolutePath
            }
            val metadataStr = Json.encodeToString(
                ProductWholesaleMetadata.serializer(),
                ProductWholesaleMetadata(monthlyMaintenance)
            )
            val p = ProductEntity(
                tenantId = tenantId,
                outletId = currentOutletId,
                name = name,
                price = pricePerDay,
                costPrice = costPrice,
                stock = 1, // 1 = available
                unit = "hari",
                barcode = plateNumber,
                category = type,
                wholesalePrices = metadataStr,
                image = compressedPath
            )
            productRepository.upsert(p)
            logActivity("TAMBAH KENDARAAN", "Menambahkan armada baru: $name ($plateNumber) Jual: $pricePerDay Modal: $costPrice")
            onDone()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteVehicle(vehicleId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val idVal = vehicleId.toLongOrNull()
            if (idVal != null) {
                val p = productRepository.getById(idVal)
                productRepository.delete(idVal)
                logActivity("HAPUS KENDARAAN", "Menghapus armada: ${p?.name ?: vehicleId}")
            }
            onDone()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun rentVehicle(
        vehicle: Vehicle,
        customerName: String,
        whatsapp: String,
        days: Int,
        cashPaid: Double,
        rentDate: Long?,
        onDone: (RentalOrder) -> Unit
    ) {
        viewModelScope.launch {
            val c = com.posbah.app.data.local.entities.CustomerEntity(
                tenantId = tenantId,
                name = customerName,
                phone = whatsapp.takeIf { it.isNotBlank() },
                address = ""
            )
            db.customerDao().upsert(c)

            val total = vehicle.pricePerDay * days
            val receiptNum = transactionRepository.generateReceiptNumberForType(tenantId, "RN")
            val txDate = rentDate ?: System.currentTimeMillis()
            
            val tx = TransactionEntity(
                tenantId = tenantId,
                outletId = currentOutletId,
                employeeId = 1L,
                customerId = vehicle.id.toLongOrNull(), // Store vehicle ID in customerId
                customerName = customerName,
                receiptNumber = receiptNum,
                date = txDate,
                subtotal = total,
                total = total,
                paymentMethod = "CASH",
                amountPaid = cashPaid,
                change = cashPaid - total,
                status = "COMPLETED",
                orderStatus = "ACTIVE",
                queueNumber = days,
                notes = whatsapp,
                deliveryDate = txDate + days * 24 * 60 * 60 * 1000L
            )
            
            val line = TransactionItemEntity(
                transactionId = 0,
                productId = vehicle.id.toLong(),
                variantId = null,
                variantName = null,
                quantity = 1,
                price = vehicle.pricePerDay,
                costPrice = vehicle.costPrice
            )
            
            transactionRepository.checkout(tx, listOf(line))
            
            val p = productRepository.getById(vehicle.id.toLong())
            if (p != null) {
                val info = VehicleRentalInfo(
                    renterName = customerName,
                    expiry = tx.deliveryDate ?: System.currentTimeMillis(),
                    days = days,
                    receiptNumber = receiptNum
                )
                val infoJson = Json.encodeToString(VehicleRentalInfo.serializer(), info)
                productRepository.upsert(p.copy(stock = 0, variants = infoJson))
            }

            logActivity("SEWA KENDARAAN", "Sewa kendaraan ${vehicle.name} kepada $customerName selama $days hari (Struk: $receiptNum)")

            val mappedOrder = RentalOrder(
                id = tx.receiptNumber,
                vehicleId = vehicle.id,
                vehicleName = vehicle.name,
                customerName = customerName,
                whatsapp = whatsapp,
                days = days,
                total = total,
                paid = cashPaid,
                rentDate = tx.date,
                status = "ACTIVE"
            )
            onDone(mappedOrder)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
        }
    }

    fun returnVehicle(order: RentalOrder, lateDays: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            val tx = transactions.value.find { it.receiptNumber == order.id } ?: return@launch
            
            val pricePerDay = order.total / order.days
            val lateFee = lateDays * pricePerDay * 1.5
            
            val updatedTx = tx.copy(
                orderStatus = "RETURNED",
                total = tx.total + lateFee,
                notes = tx.notes + if (lateDays > 0) " (Denda keterlambatan: ${lateDays} hari, +${lateFee})" else ""
            )
            transactionRepository.update(updatedTx)
            
            val p = productRepository.getById(order.vehicleId.toLong())
            if (p != null) {
                productRepository.upsert(p.copy(stock = 1, variants = null))
            }
            logActivity("PENGEMBALIAN KENDARAAN", "Pengembalian kendaraan ${order.vehicleName} (Terlambat: $lateDays hari, Denda: $lateFee)")
            onDone()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
        }
    }

    fun logActivity(action: String, description: String) {
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            val employeeName = user?.displayName ?: "Owner"
            activityLogDao.insertLog(
                ActivityLogEntity(
                    tenantId = tenantId,
                    action = action,
                    description = description,
                    employeeName = employeeName,
                    appMode = "RENTAL"
                )
            )
        }
    }

    fun addCustomer(name: String, phone: String, address: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val c = com.posbah.app.data.local.entities.CustomerEntity(
                tenantId = tenantId,
                name = name,
                phone = phone.takeIf { it.isNotBlank() },
                address = address.takeIf { it.isNotBlank() }
            )
            db.customerDao().upsert(c)
            logActivity("TAMBAH PELANGGAN", "Menambahkan pelanggan baru: $name")
            onDone()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addExpense(description: String, amount: Double, dateMillis: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val todayStr = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US).format(java.util.Date(dateMillis))
            val prefix = "EXP-RN"
            val receiptNumber = "$prefix-$todayStr-${java.util.UUID.randomUUID().toString().take(6).uppercase()}"
            val expenseTx = TransactionEntity(
                tenantId = tenantId,
                outletId = currentOutletId,
                employeeId = 1L,
                customerName = "Pengeluaran",
                receiptNumber = receiptNumber,
                date = dateMillis,
                subtotal = -amount,
                total = -amount,
                paymentMethod = "CASH",
                status = "COMPLETED",
                type = "EXPENSE",
                notes = description
            )
            transactionRepository.checkout(expenseTx, emptyList())
            logActivity("CATAT PENGELUARAN", "Mencatat pengeluaran: $description senilai Rp $amount")
            onDone()
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
        }
    }
}
