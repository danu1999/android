package com.posbah.app.ui.screens.laundry

import androidx.compose.runtime.mutableStateListOf
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
data class LaundryTransactionMetadata(
    val phone: String,
    val summary: String
)

@HiltViewModel
class LaundryViewModel @Inject constructor(
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

    val activityLogs = activityLogDao.observeLogs(tenantId, "LAUNDRY")
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val services: StateFlow<List<LaundryServiceItem>> = combine(products, transactions) { prodList, _ ->
        val filtered = prodList.filter { it.category == "KILOAN" || it.category == "SATUAN" }
        if (filtered.isEmpty() && prodList.isEmpty()) {
            viewModelScope.launch {
                localDataSeeder.seedDefaultLaundryServices(tenantId, outletId)
            }
        }
        filtered.map { p ->
            LaundryServiceItem(
                id = p.id.toString(),
                name = p.name,
                category = p.category,
                price = p.price,
                costPrice = p.costPrice,
                image = p.image,
                unit = p.unit
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cart = mutableStateListOf<CartItem>()

    val orders: StateFlow<List<LaundryOrder>> = combine(transactions, products) { txList, prodList ->
        txList.filter { it.receiptNumber.startsWith("LD-") }.map { tx ->
            var phone = ""
            var summary = ""
            if (!tx.notes.isNullOrBlank()) {
                try {
                    val meta = Json.decodeFromString(LaundryTransactionMetadata.serializer(), tx.notes)
                    phone = meta.phone
                    summary = meta.summary
                } catch (e: Exception) {
                    summary = tx.notes.orEmpty()
                }
            }
            LaundryOrder(
                id = tx.receiptNumber,
                customerName = tx.customerName.orEmpty(),
                phone = phone,
                itemsSummary = summary,
                total = tx.total,
                paymentStatus = if (tx.paymentMethod == "CASH") "LUNAS" else "BELUM LUNAS",
                orderStatus = tx.orderStatus ?: "BARU",
                dateIn = tx.date
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addService(
        name: String,
        price: Double,
        costPrice: Double,
        category: String,
        unit: String,
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
                price = price,
                costPrice = costPrice,
                stock = 9999,
                unit = unit,
                barcode = "LD-SRV-${System.currentTimeMillis()}",
                category = category,
                image = compressedPath
            )
            productRepository.upsert(p)
            logActivity("TAMBAH LAYANAN", "Menambahkan layanan laundry baru: $name ($category) Jual: $price Modal: $costPrice")
            onDone()
        }
    }

    fun deleteService(serviceId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val idVal = serviceId.toLongOrNull()
            if (idVal != null) {
                val p = productRepository.getById(idVal)
                productRepository.delete(idVal)
                logActivity("HAPUS LAYANAN", "Menghapus layanan laundry: ${p?.name ?: serviceId}")
            }
            onDone()
        }
    }

    fun checkout(customerName: String, phone: String, rentDate: Long?, onDone: (LaundryOrder) -> Unit) {
        viewModelScope.launch {
            val subtotal = cart.sumOf { it.service.price * it.quantity }
            val receiptNum = transactionRepository.generateReceiptNumberForType(tenantId, "LD")
            val txDate = rentDate ?: System.currentTimeMillis()
            
            val summary = cart.joinToString(", ") { "${it.service.name} x${if (it.service.unit == "Kg") "%.1f Kg".format(it.quantity) else "${it.quantity.toInt()} Pcs"}" }
            val meta = LaundryTransactionMetadata(phone = phone, summary = summary)
            val metaJson = Json.encodeToString(LaundryTransactionMetadata.serializer(), meta)

            val tx = TransactionEntity(
                tenantId = tenantId,
                outletId = outletId,
                employeeId = 1L,
                customerId = null,
                customerName = customerName,
                receiptNumber = receiptNum,
                date = txDate,
                subtotal = subtotal,
                total = subtotal,
                paymentMethod = "HUTANG",
                status = "COMPLETED",
                orderStatus = "BARU",
                notes = metaJson
            )

            val lines = cart.map { item ->
                val qty = if (item.service.unit == "Kg") (item.quantity * 10).toInt() else item.quantity.toInt()
                val price = if (item.service.unit == "Kg") item.service.price / 10.0 else item.service.price
                
                TransactionItemEntity(
                    transactionId = 0,
                    productId = item.service.id.toLong(),
                    quantity = qty,
                    price = price,
                    costPrice = item.service.costPrice
                )
            }

            transactionRepository.checkout(tx, lines)
            
            logActivity("CHECKOUT LAUNDRY", "Checkout laundry pelanggan $customerName senilai Rp $subtotal (Struk: $receiptNum)")

            val order = LaundryOrder(
                id = receiptNum,
                customerName = customerName,
                phone = phone,
                itemsSummary = summary,
                total = subtotal,
                paymentStatus = "BELUM LUNAS",
                orderStatus = "BARU",
                dateIn = tx.date
            )
            cart.clear()
            onDone(order)
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val tx = transactions.value.find { it.receiptNumber == orderId } ?: return@launch
            transactionRepository.update(tx.copy(orderStatus = newStatus))
            logActivity("UPDATE STATUS LAUNDRY", "Update status transaksi $orderId menjadi $newStatus")
        }
    }

    fun updatePaymentStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val tx = transactions.value.find { it.receiptNumber == orderId } ?: return@launch
            val method = if (newStatus == "LUNAS") "CASH" else "HUTANG"
            transactionRepository.update(tx.copy(paymentMethod = method))
            logActivity("UPDATE PEMBAYARAN LAUNDRY", "Update pembayaran transaksi $orderId menjadi $newStatus")
        }
    }

    fun getReceiptDetails(orderId: String, onLoaded: (List<CartItem>) -> Unit) {
        viewModelScope.launch {
            val tx = transactions.value.find { it.receiptNumber == orderId } ?: return@launch
            val lines = transactionRepository.listItemsForTransaction(tx.id)
            val mapped = lines.mapNotNull { line ->
                val prod = products.value.find { it.id == line.productId } ?: return@mapNotNull null
                val qty = if (prod.unit == "Kg") line.quantity.toDouble() / 10.0 else line.quantity.toDouble()
                val price = prod.price
                
                CartItem(
                    service = LaundryServiceItem(
                        id = prod.id.toString(),
                        name = prod.name,
                        category = prod.category,
                        price = price,
                        costPrice = prod.costPrice,
                        image = prod.image,
                        unit = prod.unit
                    ),
                    quantity = qty
                )
            }
            onLoaded(mapped)
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
                    appMode = "LAUNDRY"
                )
            )
        }
    }
}
