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

@Serializable
data class ProductWholesaleMetadata(
    val monthlyMaintenance: Double = 0.0
)

@HiltViewModel
class LaundryViewModel @Inject constructor(
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
    }

    val availableOutlets = db.outletDao().observeForTenant(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeOutletId = sessionState.outletId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectOutlet(id: Long?) {
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
        emit(user?.role == "OWNER" || user?.role == "ADMIN")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activityLogs = activityLogDao.observeLogs(tenantId, "LAUNDRY")
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val services: StateFlow<List<LaundryServiceItem>> = combine(products, transactions) { prodList, _ ->
        val filtered = prodList.filter { it.category == "KILOAN" || it.category == "SATUAN" }
        if (filtered.isEmpty() && prodList.isEmpty()) {
            viewModelScope.launch {
                localDataSeeder.seedDefaultLaundryServices(tenantId, currentOutletId)
            }
        }
        filtered.map { p ->
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
            LaundryServiceItem(
                id = p.id.toString(),
                name = p.name,
                category = p.category,
                price = p.price,
                costPrice = p.costPrice,
                image = p.image,
                unit = p.unit,
                monthlyMaintenance = monthlyMaint
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
        monthlyMaintenance: Double,
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
            val metadataStr = Json.encodeToString(
                ProductWholesaleMetadata.serializer(),
                ProductWholesaleMetadata(monthlyMaintenance)
            )
            val p = ProductEntity(
                tenantId = tenantId,
                outletId = currentOutletId,
                name = name,
                price = price,
                costPrice = costPrice,
                stock = 9999,
                unit = unit,
                barcode = "LD-SRV-${System.currentTimeMillis()}",
                category = category,
                wholesalePrices = metadataStr,
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
            val c = com.posbah.app.data.local.entities.CustomerEntity(
                tenantId = tenantId,
                name = customerName,
                phone = phone.takeIf { it.isNotBlank() },
                address = ""
            )
            db.customerDao().upsert(c)

            val subtotal = cart.sumOf { it.service.price * it.quantity }
            val receiptNum = transactionRepository.generateReceiptNumberForType(tenantId, "LD")
            val txDate = rentDate ?: System.currentTimeMillis()
            
            val summary = cart.joinToString(", ") { "${it.service.name} x${if (it.service.unit == "Kg") "%.1f Kg".format(it.quantity) else "${it.quantity.toInt()} Pcs"}" }
            val meta = LaundryTransactionMetadata(phone = phone, summary = summary)
            val metaJson = Json.encodeToString(LaundryTransactionMetadata.serializer(), meta)

            val tx = TransactionEntity(
                tenantId = tenantId,
                outletId = currentOutletId,
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
                notes = metaJson,
                deliveryDate = txDate + 3 * 24 * 60 * 60 * 1000L
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
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val tx = transactions.value.find { it.receiptNumber == orderId } ?: return@launch
            transactionRepository.update(tx.copy(orderStatus = newStatus))
            logActivity("UPDATE STATUS LAUNDRY", "Update status transaksi $orderId menjadi $newStatus")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
        }
    }

    fun updatePaymentStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val tx = transactions.value.find { it.receiptNumber == orderId } ?: return@launch
            val method = if (newStatus == "LUNAS") "CASH" else "HUTANG"
            transactionRepository.update(tx.copy(paymentMethod = method))
            logActivity("UPDATE PEMBAYARAN LAUNDRY", "Update pembayaran transaksi $orderId menjadi $newStatus")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
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
        }
    }

    fun addExpense(description: String, amount: Double, dateMillis: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val todayStr = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US).format(java.util.Date(dateMillis))
            val prefix = "EXP-LD"
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
