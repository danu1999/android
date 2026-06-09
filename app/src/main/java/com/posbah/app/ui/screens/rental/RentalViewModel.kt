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

@HiltViewModel
class RentalViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionState: SessionState,
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val localDataSeeder: LocalDataSeeder,
    private val activityLogDao: ActivityLogDao
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val outletId = sessionState.outletId.value

    val products = productRepository.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val transactions = transactionRepository.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isOwner = flow {
        val user = authRepository.getActiveUser()
        emit(user?.role == "OWNER")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activityLogs = activityLogDao.observeLogs(tenantId, "RENTAL")
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val vehicles: StateFlow<List<Vehicle>> = combine(products, transactions) { prodList, txList ->
        val filtered = prodList.filter { it.category == "MOBIL" || it.category == "MOTOR" }
        
        if (filtered.isEmpty() && prodList.isEmpty()) {
            viewModelScope.launch {
                localDataSeeder.seedDefaultVehicles(tenantId, outletId)
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
                rentExpiry = expiry
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
        imageFile: java.io.File?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            var compressedPath: String? = null
            if (imageFile != null && imageFile.exists()) {
                val compressed = CameraUtils.compressToMaxSize(imageFile, 100)
                compressedPath = compressed.absolutePath
            }
            val p = ProductEntity(
                tenantId = tenantId,
                outletId = outletId,
                name = name,
                price = pricePerDay,
                costPrice = costPrice,
                stock = 1, // 1 = available
                unit = "hari",
                barcode = plateNumber,
                category = type,
                image = compressedPath
            )
            productRepository.upsert(p)
            logActivity("TAMBAH KENDARAAN", "Menambahkan armada baru: $name ($plateNumber) Jual: $pricePerDay Modal: $costPrice")
            onDone()
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
            val total = vehicle.pricePerDay * days
            val receiptNum = transactionRepository.generateReceiptNumberForType(tenantId, "RN")
            val txDate = rentDate ?: System.currentTimeMillis()
            
            val tx = TransactionEntity(
                tenantId = tenantId,
                outletId = outletId,
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
}
