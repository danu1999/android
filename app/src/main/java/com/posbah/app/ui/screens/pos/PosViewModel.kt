package com.posbah.app.ui.screens.pos

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.LocalDataSeeder
import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.CustomerRepository
import com.posbah.app.data.repository.ProductRepository
import com.posbah.app.data.repository.SessionState
import com.posbah.app.data.repository.TransactionRepository
import com.posbah.app.data.repository.PrintSettingsRepository
import com.posbah.app.ui.print.PrintConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import com.posbah.app.data.local.dao.ActivityLogDao
import com.posbah.app.data.local.entities.ActivityLogEntity
import com.posbah.app.util.CameraUtils
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@Serializable
data class WholesaleTier(val minQty: Int, val price: Double)

@Serializable
data class ProductVariant(val id: Long, val name: String, val price: Double?, val costPrice: Double?, val stock: Int?)

data class CartItem(
    val cartKey: String, // product.id or product.id-v[variant.id]
    val product: ProductEntity,
    val variantId: Long? = null,
    val variantName: String? = null,
    val variantPrice: Double? = null,
    val quantity: Int = 1,
    val discount: Double = 0.0,
    val note: String? = null
)

data class PosUiState(
    val searchQuery: String = "",
    val activeCategory: String = "Semua",
    val customerId: Long? = null,
    val customerName: String? = null,
    val discountType: String = "percent", // "percent" | "nominal"
    val discountInput: String = "",
    val amountPaid: String = "",
    val queueNumber: String = "",
    val notes: String = "",
    val paymentMethod: String = "CASH", // CASH, QRIS, TRANSFER, HUTANG
    val debtDueDate: String = "",
    val showPayModal: Boolean = false,
    val showQueueModal: Boolean = false,
    val showReceiptDialog: Boolean = false,
    val activeReceipt: TransactionEntity? = null,
    val activeReceiptItems: List<TransactionItemEntity> = emptyList(),
    val printConfig: PrintConfig = PrintConfig(),
    val isSeeding: Boolean = false,
    val seedError: String? = null,
    val selectedTransactionDate: Long? = null,
    val isOwner: Boolean = false,
    val isPremium: Boolean = false,
    val isSeedTenant: Boolean = false,
    val canViewMargin: Boolean = false
)

@HiltViewModel
class PosViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val localDataSeeder: LocalDataSeeder,
    private val authRepository: AuthRepository,
    private val printSettingsRepository: PrintSettingsRepository,
    private val sessionState: SessionState,
    private val activityLogDao: ActivityLogDao,
    private val db: com.posbah.app.data.local.PosBahDatabase
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()
    private val currentOutletId get() = sessionState.outletId.value
    val activeTenantId get() = tenantId

    val tenantName = flow {
        val t = db.tenantDao().getById(tenantId)
        emit(t?.name ?: "Kasir F&B")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Kasir F&B")

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            printSettingsRepository.observe(tenantId, "FNB").collect { entity ->
                _uiState.update { it.copy(printConfig = PrintConfig.fromEntity(entity)) }
            }
        }
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            val seededTenants = setOf(
                "bahteramulyap@gmail.com",
                "ten_premium_bahteramulyap_gmail_com",
                "hanafiariful@gmail.com",
                "demo_tenant"
            )
            val isSeed = seededTenants.any { tenantId == it } ||
                tenantId.contains("hanafiariful_gmail_com") ||
                tenantId.startsWith("demo_tenant_")

            _uiState.update { 
                it.copy(
                    isOwner = user?.role == "OWNER",
                    isPremium = user?.isPremium == true,
                    isSeedTenant = isSeed,
                    canViewMargin = user?.role == "OWNER"
                )
            }
        }
        viewModelScope.launch {
            if (sessionState.outletId.value == null) {
                val outlets = db.outletDao().listForTenant(tenantId)
                val defaultOutlet = outlets.firstOrNull { it.isDefault } ?: outlets.firstOrNull()
                if (defaultOutlet != null) {
                    sessionState.setOutlet(defaultOutlet.id)
                }
            }
        }
        // Trigger background pull sync for outlets, employees, products
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.posbah.app.data.remote.SupabaseSyncManager.pullAll(appContext, db, tenantId)
        }

        // Auto-repair: silently re-seed transaction dates that were incorrectly set to
        // System.currentTimeMillis() during the first seeding due to a parsing bug.
        // Runs once automatically on first launch after this fix — no user action needed.
        autoRepairTransactionDates()
    }

    /**
     * Automatically corrects transaction dates for seeded tenants whose historical
     * transactions were saved with today's timestamp instead of the original SQL date.
     *
     * Uses a SharedPreferences version flag so the repair only runs once and never
     * again, without requiring the customer to do anything manually.
     */
    private fun autoRepairTransactionDates() {
        val seededTenants = setOf(
            "bahteramulyap@gmail.com",
            "ten_premium_bahteramulyap_gmail_com",
            "hanafiariful@gmail.com",
            "demo_tenant"
        )
        val isSeedTenant = seededTenants.any { tenantId == it } ||
            tenantId.contains("hanafiariful_gmail_com") ||
            tenantId.startsWith("demo_tenant_")
        if (!isSeedTenant) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prefs = appContext.getSharedPreferences("posbah_seeder_meta", android.content.Context.MODE_PRIVATE)
                val repairVersion = prefs.getInt("date_repair_v", 0)
                // Version 3 = fix for parseSqlTimestamp bug on transaction dates
                if (repairVersion < 3) {
                    _uiState.update { it.copy(isSeeding = true) }
                    localDataSeeder.seedFromSqlDump(appContext, tenantId, currentOutletId)
                    _uiState.update { it.copy(isSeeding = false) }
                    prefs.edit().putInt("date_repair_v", 3).apply()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSeeding = false) }
                android.util.Log.e("PosViewModel", "autoRepairTransactionDates failed", e)
            }
        }
    }

    // Reactive streams from database
    val availableOutlets = db.outletDao().observeForTenant(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeOutletId = sessionState.outletId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Ganti outlet aktif.
     * Hanya OWNER yang diizinkan. KASIR/ADMIN dikunci ke outlet yang ditetapkan saat login.
     */
    fun selectOutlet(id: Long?) {
        val isOwner = _uiState.value.isOwner
        if (!isOwner) {
            android.util.Log.w("PosViewModel", "selectOutlet() ditolak: hanya OWNER yang bisa ganti outlet.")
            return
        }
        sessionState.setOutlet(id)
    }

    /**
     * Produk untuk outlet yang aktif saat ini.
     * Menggunakan query DAO langsung (bukan filter in-memory) untuk isolasi ketat.
     * Produk dengan outletId=null juga disertakan (backward compat data lama).
     */
    val products = sessionState.outletId
        .flatMapLatest { outletId ->
            if (outletId != null) {
                productRepository.observeForOutlet(tenantId, outletId)
            } else {
                productRepository.observe(tenantId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Pelanggan untuk outlet yang aktif saat ini.
     */
    val customers = sessionState.outletId
        .flatMapLatest { outletId ->
            if (outletId != null) {
                customerRepository.observeForOutlet(tenantId, outletId)
            } else {
                customerRepository.observe(tenantId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Antrian pending untuk outlet yang aktif saat ini.
     */
    val pendingQueues = sessionState.outletId
        .flatMapLatest { outletId ->
            if (outletId != null) {
                transactionRepository.observePendingQueuesForOutlet(tenantId, outletId)
            } else {
                transactionRepository.observePendingQueues(tenantId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Transaksi untuk outlet yang aktif saat ini.
     */
    val transactions = sessionState.outletId
        .flatMapLatest { outletId ->
            if (outletId != null) {
                transactionRepository.observeForOutlet(tenantId, outletId)
            } else {
                transactionRepository.observe(tenantId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activityLogs = activityLogDao.observeLogs(tenantId, "FNB").stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Cart list managed locally
    val cart = mutableStateListOf<CartItem>()

    fun updateSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun updateCategory(cat: String) {
        _uiState.update { it.copy(activeCategory = cat) }
    }

    fun selectCustomer(id: Long?, name: String?) {
        _uiState.update { it.copy(customerId = id, customerName = name) }
    }

    fun updateDiscountType(type: String) {
        _uiState.update { it.copy(discountType = type) }
    }

    fun updateDiscountInput(input: String) {
        _uiState.update { it.copy(discountInput = input) }
    }

    fun updateAmountPaid(amt: String) {
        _uiState.update { it.copy(amountPaid = amt) }
    }

    fun updateQueueNumber(num: String) {
        _uiState.update { it.copy(queueNumber = num) }
    }

    fun updateNotes(n: String) {
        _uiState.update { it.copy(notes = n) }
    }

    fun updatePaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun updateDebtDueDate(date: String) {
        _uiState.update { it.copy(debtDueDate = date) }
    }

    fun togglePayModal(show: Boolean) {
        _uiState.update { it.copy(showPayModal = show) }
    }

    fun toggleQueueModal(show: Boolean) {
        _uiState.update { it.copy(showQueueModal = show) }
    }

    fun closeReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = false, activeReceipt = null, activeReceiptItems = emptyList()) }
    }

    // Cart Actions
    fun addToCart(p: ProductEntity, variant: ProductVariant? = null) {
        val vars = parseVariants(p)
        if (variant == null && vars.isNotEmpty()) {
            // Should select variant first (handled in UI via variant dialog state)
            return
        }

        val maxStock = variant?.stock ?: p.stock
        if (maxStock < 1) return

        val key = if (variant != null) "${p.id}-v${variant.id}" else p.id.toString()
        val existingIndex = cart.indexOfFirst { it.cartKey == key }

        if (existingIndex != -1) {
            val existing = cart[existingIndex]
            if (existing.quantity >= maxStock) return
            cart[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            cart.add(
                CartItem(
                    cartKey = key,
                    product = p,
                    variantId = variant?.id,
                    variantName = variant?.name,
                    variantPrice = variant?.price,
                    quantity = 1
                )
            )
        }
    }

    fun updateQty(cartKey: String, delta: Int) {
        val idx = cart.indexOfFirst { it.cartKey == cartKey }
        if (idx == -1) return
        val item = cart[idx]
        val nextQty = item.quantity + delta
        if (nextQty < 1) return

        val maxStock = item.variantId?.let { vId ->
            parseVariants(item.product).find { it.id == vId }?.stock ?: item.product.stock
        } ?: item.product.stock

        if (nextQty > maxStock) return
        cart[idx] = item.copy(quantity = nextQty)
    }

    fun updateItemNote(cartKey: String, note: String) {
        val idx = cart.indexOfFirst { it.cartKey == cartKey }
        if (idx == -1) return
        cart[idx] = cart[idx].copy(note = note)
    }

    fun removeFromCart(cartKey: String) {
        cart.removeAll { it.cartKey == cartKey }
    }

    fun clearCart() {
        cart.clear()
        _uiState.update {
            it.copy(
                customerId = null,
                customerName = null,
                discountInput = "",
                amountPaid = "",
                queueNumber = "",
                notes = "",
                selectedTransactionDate = null
            )
        }
    }

    // Cart calculations
    fun getEffectivePrice(p: ProductEntity, qty: Int): Double {
        if (!p.wholesaleEnabled || p.wholesalePrices.isNullOrBlank()) return p.price
        return try {
            val tiers = Json.decodeFromString<List<WholesaleTier>>(p.wholesalePrices)
            val applicable = tiers.filter { qty >= it.minQty }.maxByOrNull { it.minQty }
            applicable?.price ?: p.price
        } catch (e: Exception) {
            p.price
        }
    }

    fun getItemUnitPrice(item: CartItem): Double {
        return item.variantPrice ?: getEffectivePrice(item.product, item.quantity)
    }

    fun getSubtotal(): Double {
        return cart.sumOf { (getItemUnitPrice(it) - it.discount) * it.quantity }
    }

    fun getDiscountAmt(): Double {
        val sub = getSubtotal()
        val input = _uiState.value.discountInput.toDoubleOrNull() ?: 0.0
        if (input <= 0.0) return 0.0
        return if (_uiState.value.discountType == "percent") {
            Math.round(sub * input.coerceAtMost(100.0) / 100.0).toDouble()
        } else {
            input.coerceAtMost(sub)
        }
    }

    fun getTotal(): Double {
        return (getSubtotal() - getDiscountAmt()).coerceAtLeast(0.0)
    }

    // Checkout
    fun checkout(isQueue: Boolean = false) {
        if (cart.isEmpty()) return
        val currentUi = _uiState.value

        viewModelScope.launch {
            val txDate = currentUi.selectedTransactionDate ?: System.currentTimeMillis()
            val dueDateMs = if (currentUi.paymentMethod == "HUTANG" && currentUi.debtDueDate.isNotBlank()) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    sdf.parse(currentUi.debtDueDate.trim())?.time
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            val tx = TransactionEntity(
                tenantId = tenantId,
                outletId = currentOutletId,
                employeeId = 1L,
                customerId = currentUi.customerId,
                customerName = currentUi.customerName ?: "Umum",
                receiptNumber = "",
                date = txDate,
                subtotal = getSubtotal(),
                discountType = if (getDiscountAmt() > 0) currentUi.discountType else null,
                discountInput = currentUi.discountInput.toDoubleOrNull() ?: 0.0,
                discountAmt = getDiscountAmt(),
                total = getTotal(),
                discount = getDiscountAmt(),
                paymentMethod = if (isQueue) "PENDING" else currentUi.paymentMethod,
                amountPaid = if (isQueue) null else currentUi.amountPaid.toDoubleOrNull(),
                change = if (isQueue) null else (currentUi.amountPaid.toDoubleOrNull() ?: getTotal()) - getTotal(),
                status = if (isQueue) "PENDING" else "COMPLETED",
                notes = currentUi.notes.takeIf { it.isNotBlank() },
                queueNumber = currentUi.queueNumber.toIntOrNull(),
                deliveryDate = dueDateMs
            )

            val lines = cart.map {
                TransactionItemEntity(
                    transactionId = 0,
                    productId = it.product.id,
                    variantId = it.variantId,
                    variantName = it.variantName,
                    quantity = it.quantity,
                    price = getItemUnitPrice(it),
                    costPrice = it.product.costPrice,
                    discount = it.discount,
                    note = it.note
                )
            }

            val savedTx = transactionRepository.checkout(tx, lines)
            val savedItems = transactionRepository.listItemsForTransaction(savedTx.id)

            val logAction = if (isQueue) "BUAT ANTRIAN" else "CHECKOUT"
            val logDesc = if (isQueue) {
                "Membuat antrian #${tx.queueNumber} senilai Rp ${tx.total}"
            } else {
                "Transaksi ${savedTx.receiptNumber} (${tx.paymentMethod}) senilai Rp ${tx.total}"
            }
            logActivity(logAction, logDesc)

            clearCart()
            _uiState.update {
                it.copy(
                    showPayModal = false,
                    activeReceipt = savedTx,
                    activeReceiptItems = savedItems,
                    showReceiptDialog = !isQueue
                )
            }
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                sessionState.setSyncing(true)
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
                } finally {
                    sessionState.setSyncing(false)
                }
            }
        }
    }

    // Queue actions
    fun resumeQueue(tx: TransactionEntity) {
        viewModelScope.launch {
            val items = transactionRepository.listItemsForTransaction(tx.id)
            clearCart()
            _uiState.update {
                it.copy(
                    customerId = tx.customerId,
                    customerName = tx.customerName,
                    discountType = tx.discountType ?: "percent",
                    discountInput = if (tx.discountInput > 0) tx.discountInput.toString() else "",
                    queueNumber = tx.queueNumber?.toString().orEmpty(),
                    notes = tx.notes.orEmpty(),
                    showQueueModal = false
                )
            }

            // Load products back to cart
            for (line in items) {
                val prod = productRepository.getById(line.productId) ?: continue
                val vars = parseVariants(prod)
                val matchingVar = vars.find { it.id == line.variantId }

                val key = if (matchingVar != null) "${prod.id}-v${matchingVar.id}" else prod.id.toString()
                cart.add(
                    CartItem(
                        cartKey = key,
                        product = prod,
                        variantId = line.variantId,
                        variantName = line.variantName,
                        variantPrice = matchingVar?.price,
                        quantity = line.quantity,
                        discount = line.discount,
                        note = line.note
                    )
                )
            }
            // Delete this pending transaction now that it is loaded back into editing
            transactionRepository.cancelTransaction(tx.id)
        }
    }

    fun completeQueue(tx: TransactionEntity, method: String) {
        viewModelScope.launch {
            transactionRepository.completePendingTransaction(
                id = tx.id,
                method = method,
                amountPaid = tx.total,
                change = 0.0
            )
            val items = transactionRepository.listItemsForTransaction(tx.id)
            _uiState.update {
                it.copy(
                    showQueueModal = false,
                    activeReceipt = tx.copy(status = "COMPLETED", paymentMethod = method, amountPaid = tx.total, change = 0.0),
                    activeReceiptItems = items,
                    showReceiptDialog = true
                )
            }
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
        }
    }

    fun cancelQueue(txId: Long) {
        viewModelScope.launch {
            transactionRepository.cancelTransaction(txId)
        }
    }

    fun addProduct(
        name: String,
        price: Double,
        costPrice: Double,
        stock: Int,
        category: String,
        barcode: String?,
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
                outletId = currentOutletId,
                name = name,
                price = price,
                costPrice = costPrice,
                stock = stock,
                unit = "pcs",
                barcode = barcode?.takeIf { it.isNotBlank() },
                category = category.ifBlank { "Umum" },
                image = compressedPath
            )
            productRepository.upsert(p)
            logActivity("TAMBAH PRODUK", "Menambahkan produk baru: $name (Jual: Rp $price, Beli: Rp $costPrice)")
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

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            val p = productRepository.getById(productId) ?: return@launch
            productRepository.delete(productId)
            logActivity("HAPUS PRODUK", "Menghapus produk: ${p.name}")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addCustomer(name: String, phone: String, address: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val c = CustomerEntity(
                tenantId = tenantId,
                outletId = currentOutletId,
                name = name,
                phone = phone.takeIf { it.isNotBlank() },
                address = address.takeIf { it.isNotBlank() }
            )
            customerRepository.upsert(c)
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

    suspend fun getTransactionItems(transactionId: Long): List<TransactionItemEntity> {
        return transactionRepository.listItemsForTransaction(transactionId)
    }

    fun updateTransactionDate(dateMillis: Long?) {
        _uiState.update { it.copy(selectedTransactionDate = dateMillis) }
    }

    fun settlePiutang(transactionId: Long) {
        viewModelScope.launch {
            val tx = transactionRepository.getById(transactionId) ?: return@launch
            val updated = tx.copy(
                paymentMethod = "CASH",
                amountPaid = tx.total,
                change = 0.0,
                status = "COMPLETED",
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.update(updated)
            logActivity("LUNAS PIUTANG", "Melunasi piutang struk ${tx.receiptNumber} sebesar Rp ${tx.total}")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Hapus riwayat transaksi secara permanen.
     * Soft-delete lokal + push delete ke server saat syncAll berikutnya.
     * Jika transaksi COMPLETED, stok produk dikembalikan.
     */
    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            val tx = transactionRepository.getById(transactionId) ?: return@launch
            transactionRepository.deleteTransaction(transactionId)
            logActivity("HAPUS TRANSAKSI", "Menghapus transaksi ${tx.receiptNumber} (${tx.paymentMethod}) senilai Rp ${tx.total}")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                    appMode = "FNB"
                )
            )
        }
    }

    // DB Imports/Seeder
    fun importDemoData() {
        _uiState.update { it.copy(isSeeding = true, seedError = null) }
        viewModelScope.launch {
            try {
                localDataSeeder.seedFromSqlDump(appContext, tenantId, currentOutletId)
                _uiState.update { it.copy(isSeeding = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSeeding = false, seedError = e.localizedMessage) }
            }
        }
    }

    // Parsing helpers
    fun parseVariants(p: ProductEntity): List<ProductVariant> {
        if (p.variants.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString<List<ProductVariant>>(p.variants)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateReceipt(
        id: Long,
        customerName: String?,
        paymentMethod: String,
        date: Long,
        amountPaid: Double?,
        change: Double?,
        notes: String?,
        total: Double
    ) {
        viewModelScope.launch {
            val tx = transactionRepository.getById(id) ?: return@launch
            val updated = tx.copy(
                customerName = customerName,
                paymentMethod = paymentMethod,
                date = date,
                amountPaid = amountPaid,
                change = change,
                notes = notes,
                total = total,
                subtotal = total,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.update(updated)
            logActivity("EDIT STRUK", "Mengedit struk ${tx.receiptNumber} (${tx.paymentMethod} -> $paymentMethod, Total: Rp $total)")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(appContext, db, tenantId)
            }
        }
    }

    fun addExpense(description: String, amount: Double, dateMillis: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val todayStr = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US).format(java.util.Date(dateMillis))
            val prefix = "EXP-FNB"
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
