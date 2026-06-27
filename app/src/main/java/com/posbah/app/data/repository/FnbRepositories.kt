package com.posbah.app.data.repository

import com.posbah.app.data.remote.api.PosApiService
import com.posbah.app.security.SecurePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// FnbRepositories.kt — Full Online mode
// Semua operasi langsung ke VPS via Retrofit.
// Tidak ada Room DAO, tidak ada local cache.
// ─────────────────────────────────────────────────────────────────────────────

// ── Data classes (menggantikan Room Entity) ───────────────────────────────────

data class ProductData(
    val id: Long = 0,
    val tenantId: String = "",
    val outletId: Long? = null,
    val name: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val category: String? = null,
    val barcode: String? = null,
    val imageUrl: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class CustomerData(
    val id: Long = 0,
    val tenantId: String = "",
    val outletId: Long? = null,
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val updatedAt: Long = 0
)

data class TransactionData(
    val id: Long = 0,
    val tenantId: String = "",
    val outletId: Long? = null,
    val receiptNumber: String = "",
    val type: String = "SALE",
    val status: String = "COMPLETED",
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val amountPaid: Double? = null,
    val change: Double? = null,
    val customerId: Long? = null,
    val notes: String? = null,
    val date: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class TransactionItemData(
    val id: Long = 0,
    val transactionId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val subtotal: Double = 0.0
)

// Helper: convert API Map response to data class
fun Map<String, Any?>.toProductData() = ProductData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    outletId = (get("outletId") as? Number)?.toLong(),
    name = get("name") as? String ?: "",
    price = (get("price") as? Number)?.toDouble() ?: 0.0,
    stock = (get("stock") as? Number)?.toInt() ?: 0,
    category = get("category") as? String,
    barcode = get("barcode") as? String,
    imageUrl = (get("image") ?: get("imageUrl")) as? String,
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toCustomerData() = CustomerData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    outletId = (get("outletId") as? Number)?.toLong(),
    name = get("name") as? String ?: "",
    phone = get("phone") as? String,
    address = get("address") as? String,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toTransactionData() = TransactionData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    outletId = (get("outletId") as? Number)?.toLong(),
    receiptNumber = get("receiptNumber") as? String ?: "",
    type = get("type") as? String ?: "SALE",
    status = get("status") as? String ?: "COMPLETED",
    totalAmount = (get("totalAmount") as? Number)?.toDouble() ?: 0.0,
    paymentMethod = get("paymentMethod") as? String ?: "CASH",
    amountPaid = (get("amountPaid") as? Number)?.toDouble(),
    change = (get("change") as? Number)?.toDouble(),
    customerId = (get("customerId") as? Number)?.toLong(),
    notes = get("notes") as? String,
    date = (get("date") as? Number)?.toLong() ?: System.currentTimeMillis(),
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toTransactionItemData() = TransactionItemData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    transactionId = (get("transactionId") as? Number)?.toLong() ?: 0,
    productId = (get("productId") as? Number)?.toLong() ?: 0,
    productName = get("productName") as? String ?: "",
    price = (get("price") as? Number)?.toDouble() ?: 0.0,
    quantity = (get("quantity") as? Number)?.toInt() ?: 1,
    subtotal = (get("subtotal") as? Number)?.toDouble() ?: 0.0
)

// ── ProductRepository ─────────────────────────────────────────────────────────

@Singleton
class ProductRepository @Inject constructor(
    private val api: PosApiService,
    private val securePrefs: SecurePreferences
) {
    private val tenantId get() = securePrefs.currentTenantId ?: ""

    // StateFlow-based observe pattern — refresh on demand from VPS
    private val _products = MutableStateFlow<List<ProductData>>(emptyList())
    val products = _products.asStateFlow()

    suspend fun refresh(outletId: Long? = null) {
        try {
            val resp = api.getProducts(outletId?.toString())
            if (resp.isSuccessful) {
                _products.value = resp.body()?.map { it.toProductData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(outletId: Long? = null): List<ProductData> {
        return try {
            val resp = api.getProducts(outletId?.toString())
            resp.body()?.map { it.toProductData() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun ProductData.toEntity() = com.posbah.app.data.local.entities.ProductEntity(
        id = id,
        tenantId = tenantId,
        outletId = outletId,
        name = name,
        price = price,
        stock = stock,
        category = category ?: "Umum",
        barcode = barcode,
        image = imageUrl,
        isDeleted = isDeleted,
        updatedAt = updatedAt
    )

    fun updateStockLocal(id: Long, newStock: Int) {
        _products.value = _products.value.map { if (it.id == id) it.copy(stock = newStock) else it }
    }

    fun rollbackStocks(snapshot: List<ProductData>) {
        _products.value = snapshot
    }

    suspend fun getById(id: Long): com.posbah.app.data.local.entities.ProductEntity? {
        val cached = _products.value.find { it.id == id }
        if (cached != null) return cached.toEntity()
        return list().find { it.id == id }?.toEntity()
    }

    suspend fun getByBarcode(barcode: String): com.posbah.app.data.local.entities.ProductEntity? {
        val cached = _products.value.find { it.barcode == barcode }
        if (cached != null) return cached.toEntity()
        return list().find { it.barcode == barcode }?.toEntity()
    }

    suspend fun upsert(product: ProductData): Long {
        val snapshot = _products.value
        val isNew = product.id == 0L
        val tempId = if (isNew) -System.currentTimeMillis() else product.id
        val tempProduct = product.copy(id = tempId, tenantId = tenantId)

        _products.value = if (isNew) {
            snapshot + tempProduct
        } else {
            snapshot.map { if (it.id == product.id) tempProduct else it }
        }

        return try {
            val body = mapOf<String, Any?>(
                "name" to product.name,
                "price" to product.price,
                "stock" to product.stock,
                "category" to product.category,
                "barcode" to product.barcode,
                "image" to product.imageUrl,
                "outletId" to product.outletId
            )
            val newId = if (product.id == 0L) {
                val resp = api.createProduct(body)
                if (resp.isSuccessful) {
                    (resp.body()?.get("id") as? Number)?.toLong() ?: 0L
                } else {
                    _products.value = snapshot
                    0L
                }
            } else {
                val resp = api.updateProduct(product.id, body)
                if (resp.isSuccessful) {
                    product.id
                } else {
                    _products.value = snapshot
                    0L
                }
            }

            if (newId > 0L && isNew) {
                _products.value = _products.value.map { if (it.id == tempId) it.copy(id = newId) else it }
            }
            newId
        } catch (_: Exception) {
            _products.value = snapshot
            0L
        }
    }

    suspend fun upsert(product: com.posbah.app.data.local.entities.ProductEntity): Long {
        return upsert(ProductData(
            id = product.id,
            tenantId = product.tenantId,
            outletId = product.outletId,
            name = product.name,
            price = product.price,
            stock = product.stock,
            category = product.category,
            barcode = product.barcode,
            imageUrl = product.image,
            isDeleted = product.isDeleted
        ))
    }

    suspend fun updateStock(id: Long, newStock: Int) {
        val snapshot = _products.value
        updateStockLocal(id, newStock)
        try {
            val resp = api.updateProduct(id, mapOf("stock" to newStock))
            if (!resp.isSuccessful) {
                _products.value = snapshot
            }
        } catch (_: Exception) {
            _products.value = snapshot
        }
    }

    suspend fun delete(id: Long) {
        val snapshot = _products.value
        _products.value = snapshot.filter { it.id != id }
        try {
            val resp = api.deleteProduct(id)
            if (!resp.isSuccessful) {
                _products.value = snapshot
            }
        } catch (_: Exception) {
            _products.value = snapshot
        }
    }

    /**
     * Observe products sebagai Flow — backward compat untuk ViewModel lama.
     * Full online: wrap StateFlow products dengan conversion Entity.
     */
    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.ProductEntity>> {
        return _products.map { list ->
            list.map { p ->
                com.posbah.app.data.local.entities.ProductEntity(
                    id = p.id,
                    tenantId = p.tenantId,
                    outletId = p.outletId,
                    name = p.name,
                    price = p.price,
                    stock = p.stock,
                    category = p.category ?: "Umum",
                    barcode = p.barcode,
                    image = p.imageUrl,
                    isDeleted = p.isDeleted
                )
            }
        }
    }

    fun observeForOutlet(tenantId: String, outletId: Long): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.ProductEntity>> {
        return _products.map { list ->
            list.filter { it.outletId == outletId || it.outletId == null }.map { p ->
                com.posbah.app.data.local.entities.ProductEntity(
                    id = p.id,
                    tenantId = p.tenantId,
                    outletId = p.outletId,
                    name = p.name,
                    price = p.price,
                    stock = p.stock,
                    category = p.category ?: "Umum",
                    barcode = p.barcode,
                    image = p.imageUrl,
                    isDeleted = p.isDeleted
                )
            }
        }
    }
}

// ── CustomerRepository ────────────────────────────────────────────────────────

@Singleton
class CustomerRepository @Inject constructor(
    private val api: PosApiService,
    private val securePrefs: SecurePreferences
) {
    private val tenantId get() = securePrefs.currentTenantId ?: ""

    private val _customers = MutableStateFlow<List<CustomerData>>(emptyList())
    val customers = _customers.asStateFlow()

    suspend fun refresh() {
        try {
            val resp = api.getCustomers()
            if (resp.isSuccessful) {
                _customers.value = resp.body()?.map { it.toCustomerData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(outletId: Long? = null): List<CustomerData> {
        return try {
            val resp = api.getCustomers()
            resp.body()?.map { it.toCustomerData() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getById(id: Long): CustomerData? {
        val cached = _customers.value.find { it.id == id }
        if (cached != null) return cached
        return list().find { it.id == id }
    }

    suspend fun upsert(customer: CustomerData): Long {
        val snapshot = _customers.value
        val isNew = customer.id == 0L
        val tempId = if (isNew) -System.currentTimeMillis() else customer.id
        val tempCustomer = customer.copy(id = tempId, tenantId = tenantId)

        _customers.value = if (isNew) {
            snapshot + tempCustomer
        } else {
            snapshot.map { if (it.id == customer.id) tempCustomer else it }
        }

        return try {
            val body = mapOf<String, Any?>(
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "outletId" to customer.outletId
            )
            val newId = if (customer.id == 0L) {
                val resp = api.createCustomer(body)
                if (resp.isSuccessful) {
                    (resp.body()?.get("id") as? Number)?.toLong() ?: 0L
                } else {
                    _customers.value = snapshot
                    0L
                }
            } else {
                customer.id
            }

            if (newId > 0L && isNew) {
                _customers.value = _customers.value.map { if (it.id == tempId) it.copy(id = newId) else it }
            }
            newId
        } catch (_: Exception) {
            _customers.value = snapshot
            0L
        }
    }

    suspend fun upsert(customer: com.posbah.app.data.local.entities.CustomerEntity): Long {
        return upsert(CustomerData(
            id = customer.id,
            tenantId = customer.tenantId,
            outletId = customer.outletId,
            name = customer.name,
            phone = customer.phone,
            address = customer.address
        ))
    }

    suspend fun delete(id: Long) {
        val snapshot = _customers.value
        _customers.value = snapshot.filter { it.id != id }
        try {
            val resp = api.deleteCustomer(id)
            if (!resp.isSuccessful) {
                _customers.value = snapshot
            }
        } catch (_: Exception) {
            _customers.value = snapshot
        }
    }

    private fun CustomerData.toEntity() = com.posbah.app.data.local.entities.CustomerEntity(
        id = id,
        tenantId = tenantId,
        outletId = outletId,
        name = name,
        phone = phone,
        address = address
    )

    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.CustomerEntity>> {
        return _customers.map { list -> list.map { it.toEntity() } }
    }

    fun observeForOutlet(tenantId: String, outletId: Long): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.CustomerEntity>> {
        return _customers.map { list ->
            list.filter { it.outletId == outletId || it.outletId == null }.map { it.toEntity() }
        }
    }
}

// ── TransactionRepository ─────────────────────────────────────────────────────

@Singleton
class TransactionRepository @Inject constructor(
    private val api: PosApiService,
    private val securePrefs: SecurePreferences,
    private val cashflowRepo: BmpCashFlowRepository
) {
    private val tenantId get() = securePrefs.currentTenantId ?: ""

    private val _transactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val transactions = _transactions.asStateFlow()

    suspend fun refresh(outletId: Long? = null) {
        try {
            val resp = api.getTransactions(outletId?.toString(), 500)
            if (resp.isSuccessful) {
                _transactions.value = resp.body()?.map { it.toTransactionData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    /**
     * Observe transactions sebagai Flow — backward compat untuk ViewModel lama.
     * Returns Flow<List<TransactionEntity>> yang di-wrap dari StateFlow _transactions.
     */
    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.TransactionEntity>> {
        return _transactions.map { list ->
            list.map { t ->
                com.posbah.app.data.local.entities.TransactionEntity(
                    id = t.id,
                    tenantId = t.tenantId,
                    outletId = t.outletId,
                    employeeId = 0L,
                    receiptNumber = t.receiptNumber,
                    date = t.date,
                    total = t.totalAmount,
                    paymentMethod = t.paymentMethod,
                    amountPaid = t.amountPaid,
                    change = t.change,
                    customerId = t.customerId,
                    notes = t.notes,
                    status = t.status,
                    type = t.type,
                    isDeleted = t.isDeleted
                )
            }
        }
    }

    fun observeForOutlet(tenantId: String, outletId: Long): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.TransactionEntity>> {
        return _transactions.map { list ->
            list.filter { it.outletId == outletId }.map { t ->
                com.posbah.app.data.local.entities.TransactionEntity(
                    id = t.id,
                    tenantId = t.tenantId,
                    outletId = t.outletId,
                    employeeId = 0L,
                    receiptNumber = t.receiptNumber,
                    date = t.date,
                    total = t.totalAmount,
                    paymentMethod = t.paymentMethod,
                    amountPaid = t.amountPaid,
                    change = t.change,
                    customerId = t.customerId,
                    notes = t.notes,
                    status = t.status,
                    type = t.type,
                    isDeleted = t.isDeleted
                )
            }
        }
    }

    fun observePendingQueues(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.TransactionEntity>> {
        return _transactions.map { list ->
            list.filter { it.status == "PENDING" }.map { t ->
                com.posbah.app.data.local.entities.TransactionEntity(
                    id = t.id,
                    tenantId = t.tenantId,
                    outletId = t.outletId,
                    employeeId = 0L,
                    receiptNumber = t.receiptNumber,
                    date = t.date,
                    total = t.totalAmount,
                    paymentMethod = t.paymentMethod,
                    amountPaid = t.amountPaid,
                    change = t.change,
                    customerId = t.customerId,
                    notes = t.notes,
                    status = t.status,
                    type = t.type,
                    isDeleted = t.isDeleted
                )
            }
        }
    }

    fun observePendingQueuesForOutlet(tenantId: String, outletId: Long): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.TransactionEntity>> {
        return _transactions.map { list ->
            list.filter { it.status == "PENDING" && it.outletId == outletId }.map { t ->
                com.posbah.app.data.local.entities.TransactionEntity(
                    id = t.id,
                    tenantId = t.tenantId,
                    outletId = t.outletId,
                    employeeId = 0L,
                    receiptNumber = t.receiptNumber,
                    date = t.date,
                    total = t.totalAmount,
                    paymentMethod = t.paymentMethod,
                    amountPaid = t.amountPaid,
                    change = t.change,
                    customerId = t.customerId,
                    notes = t.notes,
                    status = t.status,
                    type = t.type,
                    isDeleted = t.isDeleted
                )
            }
        }
    }

    suspend fun list(outletId: Long? = null, limit: Int = 500): List<TransactionData> {
        return try {
            val resp = api.getTransactions(outletId?.toString(), limit)
            resp.body()?.map { it.toTransactionData() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getById(id: Long): com.posbah.app.data.local.entities.TransactionEntity? {
        val data = _transactions.value.find { it.id == id } ?: list().find { it.id == id } ?: return null
        return com.posbah.app.data.local.entities.TransactionEntity(
            id = data.id,
            tenantId = data.tenantId,
            outletId = data.outletId,
            employeeId = 0L,
            receiptNumber = data.receiptNumber,
            date = data.date,
            total = data.totalAmount,
            paymentMethod = data.paymentMethod,
            amountPaid = data.amountPaid,
            change = data.change,
            customerId = data.customerId,
            notes = data.notes,
            status = data.status,
            type = data.type,
            isDeleted = data.isDeleted
        )
    }

    suspend fun listItemsForTransaction(transactionId: Long): List<com.posbah.app.data.local.entities.TransactionItemEntity> {
        return try {
            val resp = api.getTransactionItems(transactionId)
            resp.body()?.map {
                com.posbah.app.data.local.entities.TransactionItemEntity(
                    id = (it["id"] as? Number)?.toLong() ?: 0,
                    transactionId = transactionId,
                    productId = (it["productId"] as? Number)?.toLong() ?: 0,
                    variantName = it["productName"] as? String ?: "",
                    quantity = (it["quantity"] as? Number)?.toInt() ?: 1,
                    price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                    costPrice = 0.0,
                    discount = 0.0,
                    note = null
                )
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun generateReceiptNumberForType(prefixType: String): String {
        val todayStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val prefix = "$prefixType-$todayStr-"
        // Cari nomor urut berikutnya dari transaksi yang sudah ada
        val existing = list().filter { it.receiptNumber.startsWith(prefix) }
        val maxSeq = existing.maxOfOrNull {
            it.receiptNumber.removePrefix(prefix).toIntOrNull() ?: 0
        } ?: 0
        return String.format("$prefix%05d", maxSeq + 1)
    }

    /**
     * Checkout kasir — POST transaksi + items ke VPS secara real-time.
     * Business logic: stok dikurangi di VPS via updateProduct.
     */
    suspend fun checkout(
        transaction: TransactionData,
        items: List<TransactionItemData>,
        productRepo: ProductRepository
    ): TransactionData = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _transactions.value
        val prefix = when (transaction.type) {
            "RENTAL" -> "RN"
            "LAUNDRY" -> "LD"
            else -> "FNB"
        }
        val receiptNum = if (transaction.receiptNumber.isBlank() || transaction.receiptNumber.startsWith("TEMP-")) {
            generateReceiptNumberForType(prefix)
        } else {
            transaction.receiptNumber
        }

        val tempId = -System.currentTimeMillis()
        val tempTx = transaction.copy(id = tempId, receiptNumber = receiptNum, tenantId = tenantId)

        _transactions.value = snapshot + tempTx

        val productSnapshot = productRepo.products.value
        if (transaction.status == "COMPLETED") {
            items.forEach { item ->
                val prod = productSnapshot.find { it.id == item.productId }
                if (prod != null) {
                    val newStock = (prod.stock - item.quantity).coerceAtLeast(0)
                    productRepo.updateStockLocal(item.productId, newStock)
                }
            }
        }

        try {
            val txBody = mapOf<String, Any?>(
                "receiptNumber" to receiptNum,
                "type" to transaction.type,
                "status" to transaction.status,
                "totalAmount" to transaction.totalAmount,
                "paymentMethod" to transaction.paymentMethod,
                "amountPaid" to transaction.amountPaid,
                "change" to transaction.change,
                "customerId" to transaction.customerId,
                "notes" to transaction.notes,
                "date" to transaction.date,
                "outletId" to transaction.outletId
            )

            val txResp = api.createTransaction(txBody)
            if (!txResp.isSuccessful) {
                _transactions.value = snapshot
                productRepo.rollbackStocks(productSnapshot)
                return@withContext transaction
            }
            val newId = (txResp.body()?.get("id") as? Number)?.toLong() ?: 0L

            val itemBodies = items.map {
                mapOf<String, Any?>(
                    "transactionId" to newId,
                    "productId" to it.productId,
                    "productName" to it.productName,
                    "price" to it.price,
                    "quantity" to it.quantity,
                    "subtotal" to it.subtotal
                )
            }
            val itemsResp = api.createTransactionItems(itemBodies)
            if (!itemsResp.isSuccessful) {
                try { api.deleteTransaction(newId) } catch (_: Exception) {}
                _transactions.value = snapshot
                productRepo.rollbackStocks(productSnapshot)
                return@withContext transaction
            }

            if (transaction.status == "COMPLETED") {
                items.forEach { item ->
                    val prod = productSnapshot.find { it.id == item.productId }
                    if (prod != null) {
                        val newStock = (prod.stock - item.quantity).coerceAtLeast(0)
                        api.updateProduct(item.productId, mapOf("stock" to newStock))
                    }
                }

                // Sync to Cashflow
                val isExpense = transaction.type == "EXPENSE"
                val cfType = if (isExpense) "KELUAR" else "MASUK"
                val cfDesc = if (isExpense) "Pengeluaran: $receiptNum" else "Penjualan: $receiptNum"
                val cfAmount = if (isExpense) Math.abs(transaction.totalAmount) else transaction.totalAmount
                cashflowRepo.createEntry(
                    BmpCashflowData(
                        transactionType = cfType,
                        description = cfDesc,
                        amount = cfAmount,
                        transactionDate = transaction.date
                    )
                )
            }

            val savedTx = tempTx.copy(id = newId)
            _transactions.value = _transactions.value.map { if (it.id == tempId) savedTx else it }
            savedTx
        } catch (e: Exception) {
            _transactions.value = snapshot
            productRepo.rollbackStocks(productSnapshot)
            transaction
        }
    }

    suspend fun completePendingTransaction(
        id: Long,
        method: String,
        amountPaid: Double?,
        change: Double?
    ) = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _transactions.value
        val tx = snapshot.find { it.id == id } ?: return@withContext
        val updatedTx = tx.copy(status = "COMPLETED", paymentMethod = method, amountPaid = amountPaid, change = change)
        _transactions.value = snapshot.map {
            if (it.id == id) updatedTx else it
        }
        try {
            val resp = api.updateTransaction(id, mapOf(
                "status" to "COMPLETED",
                "paymentMethod" to method,
                "amountPaid" to amountPaid,
                "change" to change
            ))
            if (!resp.isSuccessful) {
                _transactions.value = snapshot
                return@withContext
            }

            // Sync to Cashflow
            val isExpense = updatedTx.type == "EXPENSE"
            val cfType = if (isExpense) "KELUAR" else "MASUK"
            val cfDesc = if (isExpense) "Pengeluaran: ${updatedTx.receiptNumber}" else "Penjualan: ${updatedTx.receiptNumber}"
            val cfAmount = if (isExpense) Math.abs(updatedTx.totalAmount) else updatedTx.totalAmount
            cashflowRepo.createEntry(
                BmpCashflowData(
                    transactionType = cfType,
                    description = cfDesc,
                    amount = cfAmount,
                    transactionDate = updatedTx.date
                )
            )
        } catch (_: Exception) {
            _transactions.value = snapshot
        }
    }

    suspend fun cancelTransaction(id: Long, productRepo: ProductRepository) = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _transactions.value
        val tx = snapshot.find { it.id == id } ?: return@withContext
        if (tx.status == "CANCELLED") return@withContext

        _transactions.value = snapshot.map { if (it.id == id) it.copy(status = "CANCELLED") else it }

        val productSnapshot = productRepo.products.value
        try {
            val resp = api.updateTransaction(id, mapOf("status" to "CANCELLED"))
            if (!resp.isSuccessful) {
                _transactions.value = snapshot
                return@withContext
            }

            // Sync to Cashflow: delete matching entry
            val cfList = cashflowRepo.list()
            val match = cfList.find { it.description.contains(tx.receiptNumber) }
            if (match != null) {
                cashflowRepo.delete(match.id)
            }

            if (tx.status == "COMPLETED") {
                val items = listItemsForTransaction(id)
                items.forEach { item ->
                    val prod = productSnapshot.find { it.id == item.productId }
                    if (prod != null) {
                        val newStock = prod.stock + item.quantity
                        productRepo.updateStock(item.productId, newStock)
                    }
                }
            }
        } catch (_: Exception) {
            _transactions.value = snapshot
            productRepo.rollbackStocks(productSnapshot)
        }
    }

    suspend fun deleteTransaction(id: Long, productRepo: ProductRepository) = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _transactions.value
        val tx = snapshot.find { it.id == id } ?: return@withContext

        _transactions.value = snapshot.filter { it.id != id }

        val productSnapshot = productRepo.products.value
        try {
            // Sync to Cashflow: delete matching entry
            val cfList = cashflowRepo.list()
            val match = cfList.find { it.description.contains(tx.receiptNumber) }
            if (match != null) {
                cashflowRepo.delete(match.id)
            }

            if (tx.status == "COMPLETED" && tx.type != "EXPENSE") {
                val items = listItemsForTransaction(id)
                items.forEach { item ->
                    val prod = productSnapshot.find { it.id == item.productId }
                    if (prod != null) {
                        val newStock = prod.stock + item.quantity
                        productRepo.updateStock(item.productId, newStock)
                    }
                }
            }
            val resp = api.deleteTransaction(id)
            if (!resp.isSuccessful) {
                _transactions.value = snapshot
                productRepo.rollbackStocks(productSnapshot)
            }
        } catch (_: Exception) {
            _transactions.value = snapshot
            productRepo.rollbackStocks(productSnapshot)
        }
    }

    suspend fun update(tx: TransactionData) = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _transactions.value
        _transactions.value = snapshot.map { if (it.id == tx.id) tx else it }
        try {
            val resp = api.updateTransaction(tx.id, mapOf(
                "status" to tx.status,
                "paymentMethod" to tx.paymentMethod,
                "amountPaid" to tx.amountPaid,
                "change" to tx.change,
                "notes" to tx.notes
            ))
            if (!resp.isSuccessful) {
                _transactions.value = snapshot
                return@withContext
            }

            // Sync to Cashflow: update or delete or create matching entry
            val cfList = cashflowRepo.list()
            val match = cfList.find { it.description.contains(tx.receiptNumber) }
            if (match != null) {
                if (tx.status == "COMPLETED") {
                    val isExpense = tx.type == "EXPENSE"
                    val cfAmount = if (isExpense) Math.abs(tx.totalAmount) else tx.totalAmount
                    cashflowRepo.update(match.copy(
                        amount = cfAmount,
                        transactionDate = tx.date
                    ))
                } else {
                    cashflowRepo.delete(match.id)
                }
            } else if (tx.status == "COMPLETED") {
                val isExpense = tx.type == "EXPENSE"
                val cfType = if (isExpense) "KELUAR" else "MASUK"
                val cfDesc = if (isExpense) "Pengeluaran: ${tx.receiptNumber}" else "Penjualan: ${tx.receiptNumber}"
                val cfAmount = if (isExpense) Math.abs(tx.totalAmount) else tx.totalAmount
                cashflowRepo.createEntry(
                    BmpCashflowData(
                        transactionType = cfType,
                        description = cfDesc,
                        amount = cfAmount,
                        transactionDate = tx.date
                    )
                )
            }
        } catch (_: Exception) {
            _transactions.value = snapshot
        }
    }

    suspend fun update(tx: com.posbah.app.data.local.entities.TransactionEntity) {
        update(TransactionData(
            id = tx.id,
            tenantId = tx.tenantId,
            outletId = tx.outletId,
            receiptNumber = tx.receiptNumber,
            type = tx.type,
            status = tx.status,
            totalAmount = tx.total,
            paymentMethod = tx.paymentMethod,
            amountPaid = tx.amountPaid,
            change = tx.change,
            customerId = tx.customerId,
            notes = tx.notes,
            date = tx.date
        ))
    }

    /**
     * Overload untuk backward compat: terima TransactionEntity + TransactionItemEntity.
     * RentalViewModel, LaundryViewModel masih pakai Entity — dikonversi ke Data class di sini.
     */
    suspend fun checkout(
        transaction: com.posbah.app.data.local.entities.TransactionEntity,
        items: List<com.posbah.app.data.local.entities.TransactionItemEntity>,
        productRepo: ProductRepository
    ): com.posbah.app.data.local.entities.TransactionEntity {
        val txData = TransactionData(
            id = transaction.id,
            tenantId = transaction.tenantId,
            outletId = transaction.outletId,
            receiptNumber = transaction.receiptNumber,
            type = transaction.type,
            status = transaction.status,
            totalAmount = transaction.total,
            paymentMethod = transaction.paymentMethod,
            amountPaid = transaction.amountPaid,
            change = transaction.change,
            customerId = transaction.customerId,
            notes = transaction.notes,
            date = transaction.date
        )
        val itemsData = items.map { it.toTransactionItemData() }
        val result = checkout(txData, itemsData, productRepo)
        return transaction.copy(id = result.id, receiptNumber = result.receiptNumber)
    }

    /** Generate receipt number — overload terima tenantId string prefix */
    suspend fun generateReceiptNumberForType(prefixType: String, outletId: Long?): String =
        generateReceiptNumberForType(prefixType)
}

// ── Extension converters ──────────────────────────────────────────────────────

fun com.posbah.app.data.local.entities.TransactionItemEntity.toTransactionItemData() = TransactionItemData(
    id = id,
    transactionId = transactionId,
    productId = productId,
    productName = variantName ?: "",
    price = price,
    quantity = quantity,
    subtotal = price * quantity
)

// ── EmployeeRepository ────────────────────────────────────────────────────────

data class EmployeeData(
    val id: Long = 0,
    val tenantId: String = "",
    val outletId: Long? = null,
    val name: String = "",
    val email: String? = null,
    val role: String = "KASIR",
    val pinHash: String = "",
    val phone: String? = null,
    val salary: Double = 0.0,
    val isActive: Boolean = true,
    val payPeriod: String = "MONTHLY",
    val lastPaidAt: Long? = null,
    val emailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val passwordChangeCount: Int = 0,
    val lastPasswordChangeDate: Long = 0L
)

fun Map<String, Any?>.toEmployeeData() = EmployeeData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    outletId = (get("outletId") as? Number)?.toLong(),
    name = get("name") as? String ?: "",
    email = get("email") as? String,
    role = get("role") as? String ?: "KASIR",
    pinHash = (get("pinHash") ?: get("password") ?: "") as? String ?: "",
    phone = get("phone") as? String,
    salary = (get("salary") as? Number)?.toDouble() ?: 0.0,
    isActive = get("isActive") as? Boolean ?: true,
    payPeriod = get("payPeriod") as? String ?: "MONTHLY",
    lastPaidAt = (get("lastPaidAt") as? Number)?.toLong(),
    emailVerified = get("emailVerified") as? Boolean ?: false,
    createdAt = (get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
    passwordChangeCount = (get("passwordChangeCount") as? Number)?.toInt() ?: 0,
    lastPasswordChangeDate = (get("lastPasswordChangeDate") as? Number)?.toLong() ?: 0L
)

@Singleton
class EmployeeRepository @Inject constructor(
    private val api: PosApiService,
    private val securePrefs: SecurePreferences
) {
    private val tenantId get() = securePrefs.currentTenantId ?: ""

    private val _employees = MutableStateFlow<List<EmployeeData>>(emptyList())
    val employees = _employees.asStateFlow()

    suspend fun refresh() {
        try {
            val resp = api.getEmployees()
            if (resp.isSuccessful) {
                _employees.value = resp.body()?.map { it.toEmployeeData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<EmployeeData> {
        val cached = _employees.value
        if (cached.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try { refresh() } catch (_: Exception) {}
            }
            return cached
        }
        refresh()
        return _employees.value
    }

    suspend fun getById(id: Long): EmployeeData? {
        val cached = _employees.value.find { it.id == id }
        if (cached != null) return cached
        return list().find { it.id == id }
    }

    suspend fun insert(emp: EmployeeData): Long {
        val snapshot = _employees.value
        val tempId = -System.currentTimeMillis()
        val tempEmp = emp.copy(id = tempId, tenantId = tenantId)
        _employees.value = snapshot + tempEmp
        return try {
            val body = mapOf(
                "name" to emp.name,
                "email" to emp.email,
                "role" to emp.role,
                "pinHash" to emp.pinHash,
                "phone" to emp.phone,
                "salary" to emp.salary,
                "isActive" to emp.isActive,
                "payPeriod" to emp.payPeriod,
                "lastPaidAt" to emp.lastPaidAt,
                "outletId" to emp.outletId
            )
            val resp = api.createEmployee(body)
            if (resp.isSuccessful) {
                val newId = (resp.body()?.get("id") as? Number)?.toLong() ?: 0L
                val savedEmp = tempEmp.copy(id = newId)
                _employees.value = _employees.value.map { if (it.id == tempId) savedEmp else it }
                newId
            } else {
                _employees.value = snapshot
                0L
            }
        } catch (e: Exception) {
            _employees.value = snapshot
            0L
        }
    }

    suspend fun update(emp: EmployeeData) {
        val snapshot = _employees.value
        _employees.value = snapshot.map { if (it.id == emp.id) emp else it }
        try {
            val body = mapOf(
                "name" to emp.name,
                "email" to emp.email,
                "role" to emp.role,
                "pinHash" to emp.pinHash,
                "phone" to emp.phone,
                "salary" to emp.salary,
                "isActive" to emp.isActive,
                "payPeriod" to emp.payPeriod,
                "lastPaidAt" to emp.lastPaidAt,
                "outletId" to emp.outletId
            )
            api.updateEmployee(emp.id, body)
        } catch (e: Exception) {
            _employees.value = snapshot
        }
    }

    suspend fun delete(id: Long) {
        val snapshot = _employees.value
        _employees.value = snapshot.filter { it.id != id }
        try {
            api.deleteEmployee(id)
        } catch (e: Exception) {
            _employees.value = snapshot
        }
    }

    fun observeForTenant(tenantId: String): Flow<List<EmployeeData>> {
        return _employees.map { list ->
            list.filter { it.tenantId == tenantId }
        }
    }
}

