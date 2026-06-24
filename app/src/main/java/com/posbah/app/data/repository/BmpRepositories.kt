package com.posbah.app.data.repository

import com.posbah.app.data.remote.api.BmpApiService
import com.posbah.app.data.remote.api.PosApiService
import com.posbah.app.security.SecurePreferences
import com.posbah.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// BmpRepositories.kt — Full Online mode
// Semua operasi langsung ke VPS via BmpApiService (Retrofit).
// Tidak ada Room DAO, tidak ada local cache.
// Business logic TETAP sama: invoice creates cashflow, bahan baku deducts cashflow.
// ─────────────────────────────────────────────────────────────────────────────

// ── Sealed result untuk UI feedback ──────────────────────────────────────────

sealed class OnlineWriteResult {
    object Success : OnlineWriteResult()
    data class Error(val message: String) : OnlineWriteResult()
    object NoConnection : OnlineWriteResult()
}

// ── Data classes (menggantikan Room Entity) ───────────────────────────────────

data class BmpClientData(
    val id: Long = 0,
    val tenantId: String = "",
    val clientName: String = "",
    val saldoTitipan: Double = 0.0,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val npwp: String? = null,
    val uniqueID: String? = null,
    val slug: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpInvoiceData(
    val id: Long = 0,
    val tenantId: String = "",
    val clientId: Long = 0,
    val number: String = "",
    val status: String = "UNPAID",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentTerms: String = "14 hari",
    val dueDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpProductItemData(
    val id: Long = 0,
    val tenantId: String = "",
    val invoiceId: Long = 0,
    val masterItemID: Long? = null,
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val jumlahLusin: Int = 1,
    val hargaBeli: Double = 0.0,
    val isKhusus: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpMasterProductData(
    val id: Long = 0,
    val tenantId: String = "",
    val title: String = "",
    val sku: String? = null,
    val hppTotalPcs: Double = 0.0,
    val pricePerPcs: Double = 0.0,
    val currentStock: Int = 0,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpCashflowData(
    val id: Long = 0,
    val tenantId: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val transactionType: String = "MASUK",
    val description: String = "",
    val amount: Double = 0.0,
    val paymentRefId: Long? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpPaymentData(
    val id: Long = 0,
    val tenantId: String = "",
    val invoiceId: Long = 0,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val notes: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpEmployeeData(
    val id: Long = 0,
    val tenantId: String = "",
    val name: String = "",
    val role: String = "KARYAWAN",
    val salary: Double = 0.0,
    val phone: String? = null,
    val email: String? = null,
    val isActive: Boolean = true,
    val updatedAt: Long = 0
)

data class BmpPayrollData(
    val id: Long = 0,
    val tenantId: String = "",
    val employeeId: Long = 0,
    val employeeName: String = "",
    val paymentDate: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val notes: String? = null,
    val updatedAt: Long = 0
)

data class BmpBahanBakuData(
    val id: Long = 0,
    val tenantId: String = "",
    val nomorNota: String = "",
    val tanggal: Long = System.currentTimeMillis(),
    val supplier: String? = null,
    val totalBiaya: Double = 0.0,
    val paidAmount: Double = 0.0,
    val notes: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpBahanBakuItemData(
    val id: Long = 0,
    val bahanBakuId: Long = 0,
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val pricePerUnit: Double = 0.0,
    val subtotal: Double = 0.0,
    val isDeleted: Boolean = false
)

data class BmpProductionLogData(
    val id: Long = 0,
    val tenantId: String = "",
    val masterItemId: Long = 0,
    val productionDate: Long = System.currentTimeMillis(),
    val quantityProduced: Int = 0,
    val notes: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpProductStockData(
    val id: Long = 0,
    val tenantId: String = "",
    val masterItemId: Long = 0,
    val currentStock: Int = 0,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpStockLedgerData(
    val id: Long = 0,
    val tenantId: String = "",
    val masterItemId: Long = 0,
    val mutationType: String = "",
    val change: Int = 0,
    val stockAfter: Int = 0,
    val referenceId: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class BmpSettingsData(
    val id: Long = 0,
    val tenantId: String = "",
    val companyName: String = "",
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val npwp: String? = null,
    val logoUrl: String? = null,
    val bankInfo: String? = null,
    val invoicePrefix: String = "INV",
    val listrikBulanan: Double = 30_000_000.0,
    val jumlahMesin: Int = 5,
    val jumlahKaryawan: Int = 19,
    val gajiHarian: Double = 80_000.0,
    val hariKerjaSebulan: Int = 26,
    val biayaKarungPer1000: Double = 2_100_000.0,
    val hoursPerDay: Int = 24,
    val updatedAt: Long = 0
)

// PrintSettingsData — defined in MissingRepositories.kt (same package)


// ── Converters from API Map to data classes ────────────────────────────────────

fun Map<String, Any?>.toBmpClientData() = BmpClientData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    clientName = get("clientName") as? String ?: "",
    saldoTitipan = (get("saldoTitipan") as? Number)?.toDouble() ?: 0.0,
    address = get("address") as? String,
    phone = get("phone") as? String,
    email = get("email") as? String,
    npwp = get("npwp") as? String,
    uniqueID = get("uniqueID") as? String,
    slug = get("slug") as? String,
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpInvoiceData() = BmpInvoiceData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    clientId = (get("clientId") as? Number)?.toLong() ?: 0,
    number = get("number") as? String ?: "",
    status = get("status") as? String ?: "UNPAID",
    totalAmount = (get("totalAmount") as? Number)?.toDouble() ?: 0.0,
    paidAmount = (get("paidAmount") as? Number)?.toDouble() ?: 0.0,
    paymentTerms = get("paymentTerms") as? String ?: "14 hari",
    dueDate = (get("dueDate") as? Number)?.toLong(),
    notes = get("notes") as? String,
    createdAt = (get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpProductItemData() = BmpProductItemData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    invoiceId = (get("invoiceId") as? Number)?.toLong() ?: 0,
    masterItemID = (get("masterItemID") as? Number)?.toLong(),
    name = get("name") as? String ?: "",
    price = (get("price") as? Number)?.toDouble() ?: 0.0,
    quantity = (get("quantity") as? Number)?.toInt() ?: 1,
    jumlahLusin = (get("jumlahLusin") as? Number)?.toInt() ?: 1,
    hargaBeli = (get("hargaBeli") as? Number)?.toDouble() ?: 0.0,
    isKhusus = get("isKhusus") as? Boolean ?: false,
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpMasterProductData() = BmpMasterProductData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    title = get("title") as? String ?: "",
    sku = get("sku") as? String,
    hppTotalPcs = (get("hppTotalPcs") as? Number)?.toDouble() ?: 0.0,
    pricePerPcs = (get("pricePerPcs") as? Number)?.toDouble() ?: 0.0,
    currentStock = (get("currentStock") as? Number)?.toInt() ?: 0,
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpCashflowData() = BmpCashflowData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    transactionDate = (get("transactionDate") as? Number)?.toLong() ?: System.currentTimeMillis(),
    transactionType = get("transactionType") as? String ?: "MASUK",
    description = get("description") as? String ?: "",
    amount = (get("amount") as? Number)?.toDouble() ?: 0.0,
    paymentRefId = (get("paymentRefId") as? Number)?.toLong(),
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpPaymentData() = BmpPaymentData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    invoiceId = (get("invoiceId") as? Number)?.toLong() ?: 0,
    paymentDate = (get("paymentDate") as? Number)?.toLong() ?: System.currentTimeMillis(),
    paymentAmount = (get("paymentAmount") as? Number)?.toDouble() ?: 0.0,
    paymentMethod = get("paymentMethod") as? String ?: "CASH",
    notes = get("notes") as? String,
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpBahanBakuData() = BmpBahanBakuData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    nomorNota = get("nomorNota") as? String ?: "",
    tanggal = (get("tanggal") as? Number)?.toLong() ?: System.currentTimeMillis(),
    supplier = get("supplier") as? String,
    totalBiaya = (get("totalBiaya") as? Number)?.toDouble() ?: 0.0,
    notes = get("notes") as? String,
    isDeleted = get("isDeleted") as? Boolean ?: false,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

// ── BmpClientRepository ───────────────────────────────────────────────────────

@Singleton
class BmpClientRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    private val _clients = MutableStateFlow<List<BmpClientData>>(emptyList())
    val clients = _clients.asStateFlow()

    private fun BmpClientData.toEntity() = BmpClientEntity(
        id = id,
        tenantId = tenantId,
        clientName = clientName,
        addressLine1 = address,
        phoneNumber = phone,
        emailAddress = email,
        taxNumber = npwp,
        uniqueID = uniqueID,
        slug = slug,
        isDeleted = isDeleted,
        isSynced = true,
        updatedAt = updatedAt
    )

    private fun BmpClientEntity.toData() = BmpClientData(
        id = id,
        tenantId = tenantId,
        clientName = clientName,
        address = addressLine1,
        phone = phoneNumber,
        email = emailAddress,
        npwp = taxNumber,
        uniqueID = uniqueID,
        slug = slug,
        isDeleted = isDeleted,
        updatedAt = updatedAt
    )

    suspend fun refresh() {
        try {
            val resp = api.getClients()
            if (resp.isSuccessful) {
                _clients.value = resp.body()?.map { it.toBmpClientData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpClientData> {
        return try {
            api.getClients().body()?.map { it.toBmpClientData() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<BmpClientEntity>> =
        kotlinx.coroutines.flow.flow {
            emit(list().map { it.toEntity() })
        }

    fun search(tenantId: String, query: String): kotlinx.coroutines.flow.Flow<List<BmpClientEntity>> =
        kotlinx.coroutines.flow.flow {
            val all = list().map { it.toEntity() }
            val filtered = all.filter { it.clientName.contains(query, ignoreCase = true) }
            emit(filtered)
        }

    fun count(tenantId: String): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flow {
            emit(list().size)
        }

    suspend fun getById(id: Long): BmpClientEntity? =
        list().find { it.id == id }?.toEntity()

    suspend fun upsert(client: BmpClientData): OnlineWriteResult {
        return try {
            val body = mapOf<String, Any?>(
                "clientName" to client.clientName,
                "address" to client.address,
                "phone" to client.phone,
                "email" to client.email,
                "npwp" to client.npwp,
                "uniqueID" to (client.uniqueID ?: java.util.UUID.randomUUID().toString()),
                "slug" to (client.slug ?: client.clientName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'))
            )
            if (client.id == 0L) {
                api.createClient(body)
            } else {
                api.updateClient(client.id, body)
            }
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan klien") }
    }

    suspend fun upsert(context: android.content.Context, entity: BmpClientEntity): OnlineWriteResult {
        return upsert(entity.toData())
    }

    suspend fun delete(id: Long, cashflowRepo: BmpCashFlowRepository): OnlineWriteResult {
        return try {
            api.deleteClient(id)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus klien") }
    }

    suspend fun delete(context: android.content.Context, tenantId: String, id: Long): OnlineWriteResult {
        return try {
            api.deleteClient(id)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus klien") }
    }
}

// ── BmpInvoiceRepository ──────────────────────────────────────────────────────

@Singleton
class BmpInvoiceRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences,
    private val cashflowRepo: BmpCashFlowRepository,
    private val stockRepo: BmpStockRepository
) {
    private val _invoices = MutableStateFlow<List<BmpInvoiceData>>(emptyList())
    val invoices = _invoices.asStateFlow()

    private fun BmpInvoiceData.toEntity() = com.posbah.app.data.local.entities.BmpInvoiceEntity(
        id = id,
        tenantId = tenantId,
        clientId = clientId,
        title = "",
        number = number,
        dueDate = dueDate,
        paymentTerms = paymentTerms,
        status = status,
        notes = notes,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        isSynced = true,
        createdAt = createdAt,
        updatedAt = updatedAt,
        slug = ""
    )

    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.BmpInvoiceEntity>> =
        kotlinx.coroutines.flow.flow {
            emit(list().map { it.toEntity() })
        }

    suspend fun payMassal(
        tenantId: String,
        clientId: Long,
        nominal: Double,
        paymentMethod: String,
        notes: String?
    ) {
        val allInvoices = list().filter { it.clientId == clientId && !it.isDeleted }
        val unpaidInvoices = allInvoices.filter { it.status == "UNPAID" || it.status == "PARTIAL" }
            .sortedBy { it.createdAt }

        var remaining = nominal
        for (inv in unpaidInvoices) {
            if (remaining <= 0.0) break
            val unpaidAmount = inv.totalAmount - inv.paidAmount
            if (unpaidAmount <= 0.0) continue

            val toPay = if (remaining >= unpaidAmount) unpaidAmount else remaining
            val newPaid = inv.paidAmount + toPay
            val newStatus = if (newPaid >= inv.totalAmount - 0.01) "PAID" else "PARTIAL"

            val payResp = api.createPayment(mapOf(
                "invoiceId" to inv.id,
                "paymentDate" to System.currentTimeMillis(),
                "paymentAmount" to toPay,
                "paymentMethod" to paymentMethod,
                "notes" to (notes ?: "Pembayaran borongan")
            ))
            val payId = (payResp.body()?.get("id") as? Number)?.toLong()

            api.updateInvoice(inv.id, mapOf(
                "status" to newStatus,
                "paidAmount" to newPaid
            ))

            remaining -= toPay
        }

        if (remaining > 0.0) {
            val clientResp = api.getClients()
            val clientData = clientResp.body()?.map { it.toBmpClientData() }?.find { it.id == clientId }
            if (clientData != null) {
                val newSaldo = clientData.saldoTitipan + remaining
                api.updateClient(clientId, mapOf("saldoTitipan" to newSaldo))
            }
        }

        // Catat cashflow masuk
        cashflowRepo.createEntry(BmpCashflowData(
            tenantId = tenantId,
            transactionType = "MASUK",
            description = notes ?: "Pembayaran borongan klien",
            amount = nominal,
            transactionDate = System.currentTimeMillis()
        ))
    }

    suspend fun refresh() {
        try {
            val resp = api.getInvoices()
            if (resp.isSuccessful) {
                _invoices.value = resp.body()?.map { it.toBmpInvoiceData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpInvoiceData> = try {
        api.getInvoices().body()?.map { it.toBmpInvoiceData() } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    suspend fun getById(id: Long): com.posbah.app.data.local.entities.BmpInvoiceEntity? =
        (_invoices.value.find { it.id == id } ?: list().find { it.id == id })?.toEntity()

    suspend fun getProductsByInvoice(invoiceId: Long): List<BmpProductItemData> = try {
        api.getBmpProducts(invoiceId).body()?.map { it.toBmpProductItemData() } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    suspend fun getPaymentsByInvoice(invoiceId: Long): List<BmpPaymentData> = try {
        api.getPayments().body()?.map { it.toBmpPaymentData() }
            ?.filter { it.invoiceId == invoiceId } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    /**
     * Buat invoice baru + products ke VPS secara real-time.
     * Business logic TETAP: jika ada uang muka (paidAmount > 0) → auto create cashflow MASUK.
     * Jika ada item isKhusus → create cashflow KELUAR.
     */
    suspend fun createInvoice(
        invoice: BmpInvoiceData,
        products: List<BmpProductItemData>,
        cashflowRepo: BmpCashFlowRepository,
        stockRepo: BmpStockRepository
    ): Pair<Long, OnlineWriteResult> {
        return try {
            val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
            val days = parsePaymentTermsDays(invoice.paymentTerms)
            val computedDueDate = invoice.dueDate ?: (invoice.createdAt + days * 86400_000L)
            val newStatus = computeInvoiceStatus(total, invoice.paidAmount)

            // 1. POST invoice header ke VPS
            val invoiceBody = mapOf<String, Any?>(
                "clientId" to invoice.clientId,
                "number" to invoice.number,
                "status" to newStatus,
                "totalAmount" to total,
                "paidAmount" to invoice.paidAmount,
                "paymentTerms" to invoice.paymentTerms,
                "dueDate" to computedDueDate,
                "notes" to invoice.notes,
                "createdAt" to invoice.createdAt
            )
            val invoiceResp = api.createInvoice(invoiceBody)
            if (!invoiceResp.isSuccessful) {
                return Pair(-1L, OnlineWriteResult.Error("Gagal simpan invoice ke server"))
            }
            val newId = (invoiceResp.body()?.get("id") as? Number)?.toLong() ?: -1L

            // 2. POST products (invoice line items)
            for (prod in products) {
                api.createBmpProduct(mapOf(
                    "invoiceId" to newId,
                    "masterItemID" to prod.masterItemID,
                    "name" to prod.name,
                    "price" to prod.price,
                    "quantity" to prod.quantity,
                    "jumlahLusin" to prod.jumlahLusin,
                    "hargaBeli" to prod.hargaBeli,
                    "isKhusus" to prod.isKhusus
                ))
            }

            // 3. ── Business logic: Deduct finished goods stock ────────────────
            for (prod in products) {
                if (prod.masterItemID != null) {
                    stockRepo.adjustStock(
                        masterItemId = prod.masterItemID,
                        change = -(prod.quantity * prod.jumlahLusin),
                        mutationType = "PENJUALAN",
                        referenceId = newId,
                        notes = "Penjualan Invoice #${invoice.number}"
                    )
                }
            }

            // 4. ── Business logic: Uang muka → cashflow MASUK ────────────────
            if (invoice.paidAmount > 0) {
                val payResp = api.createPayment(mapOf(
                    "invoiceId" to newId,
                    "paymentDate" to System.currentTimeMillis(),
                    "paymentAmount" to invoice.paidAmount,
                    "paymentMethod" to "CASH",
                    "notes" to "Uang muka saat pembuatan Invoice"
                ))
                val payId = (payResp.body()?.get("id") as? Number)?.toLong()
                cashflowRepo.createEntry(BmpCashflowData(
                    transactionType = "MASUK",
                    description = "Pembayaran Invoice ${invoice.number}",
                    amount = invoice.paidAmount,
                    paymentRefId = payId,
                    transactionDate = System.currentTimeMillis()
                ))
            }

            // 5. ── Business logic: isKhusus item → cashflow KELUAR ────────────
            for (prod in products) {
                if (prod.isKhusus && prod.hargaBeli > 0) {
                    cashflowRepo.createEntry(BmpCashflowData(
                        transactionType = "KELUAR",
                        description = "Pembelian barang khusus untuk Faktur ${invoice.number}",
                        amount = prod.hargaBeli,
                        transactionDate = System.currentTimeMillis()
                    ))
                }
            }

            Pair(newId, OnlineWriteResult.Success)
        } catch (e: Exception) {
            Pair(-1L, OnlineWriteResult.Error(e.message ?: "Gagal membuat invoice"))
        }
    }

    /**
     * Update invoice + products.
     * Business logic TETAP: restore stok lama, kurangi stok baru, update cashflow exit jika ada isKhusus.
     */
    suspend fun updateInvoice(
        invoice: BmpInvoiceData,
        products: List<BmpProductItemData>,
        cashflowRepo: BmpCashFlowRepository,
        stockRepo: BmpStockRepository
    ): OnlineWriteResult {
        return try {
            val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
            val days = parsePaymentTermsDays(invoice.paymentTerms)
            val computedDueDate = invoice.dueDate ?: (invoice.createdAt + days * 86400_000L)
            val allPayments = getPaymentsByInvoice(invoice.id)
            val totalPaidAmt = allPayments.sumOf { it.paymentAmount }
            val newStatus = computeInvoiceStatus(total, totalPaidAmt, computedDueDate)

            // 1. Hapus produk lama di VPS
            val oldProducts = getProductsByInvoice(invoice.id)
            for (op in oldProducts) {
                try { api.deleteBmpProduct(op.id) } catch (_: Exception) {}
                // Kembalikan stok produk lama
                if (op.masterItemID != null) {
                    stockRepo.adjustStock(
                        masterItemId = op.masterItemID,
                        change = op.quantity * op.jumlahLusin,
                        mutationType = "PENJUALAN",
                        referenceId = invoice.id,
                        notes = "Koreksi Invoice #${invoice.number} (Kembalikan)"
                    )
                }
            }

            // 2. Update invoice header
            api.updateInvoice(invoice.id, mapOf(
                "clientId" to invoice.clientId,
                "number" to invoice.number,
                "status" to newStatus,
                "totalAmount" to total,
                "paidAmount" to totalPaidAmt,
                "paymentTerms" to invoice.paymentTerms,
                "dueDate" to computedDueDate,
                "notes" to invoice.notes
            ))

            // 3. Insert produk baru
            for (prod in products) {
                api.createBmpProduct(mapOf(
                    "invoiceId" to invoice.id,
                    "masterItemID" to prod.masterItemID,
                    "name" to prod.name,
                    "price" to prod.price,
                    "quantity" to prod.quantity,
                    "jumlahLusin" to prod.jumlahLusin,
                    "hargaBeli" to prod.hargaBeli,
                    "isKhusus" to prod.isKhusus
                ))
                // Kurangi stok baru
                if (prod.masterItemID != null) {
                    stockRepo.adjustStock(
                        masterItemId = prod.masterItemID,
                        change = -(prod.quantity * prod.jumlahLusin),
                        mutationType = "PENJUALAN",
                        referenceId = invoice.id,
                        notes = "Koreksi Invoice #${invoice.number} (Kurangi)"
                    )
                }
            }

            // 4. Re-create cashflow KELUAR untuk isKhusus baru
            for (prod in products) {
                if (prod.isKhusus && prod.hargaBeli > 0) {
                    cashflowRepo.createEntry(BmpCashflowData(
                        transactionType = "KELUAR",
                        description = "Pembelian barang khusus untuk Faktur ${invoice.number}",
                        amount = prod.hargaBeli,
                        transactionDate = System.currentTimeMillis()
                    ))
                }
            }

            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal update invoice") }
    }

    suspend fun deleteInvoice(
        id: Long,
        cashflowRepo: BmpCashFlowRepository,
        stockRepo: BmpStockRepository
    ): OnlineWriteResult {
        return try {
            val inv = getById(id) ?: return OnlineWriteResult.Error("Invoice tidak ditemukan")
            // Kembalikan stok
            val products = getProductsByInvoice(id)
            for (prod in products) {
                if (prod.masterItemID != null) {
                    stockRepo.adjustStock(
                        masterItemId = prod.masterItemID,
                        change = prod.quantity * prod.jumlahLusin,
                        mutationType = "PENJUALAN",
                        referenceId = id,
                        notes = "Hapus Invoice #${inv.number}"
                    )
                }
                try { api.deleteBmpProduct(prod.id) } catch (_: Exception) {}
            }
            // Hapus payments
            val payments = getPaymentsByInvoice(id)
            for (pay in payments) {
                try { api.deletePayment(pay.id) } catch (_: Exception) {}
            }
            api.deleteInvoice(id)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus invoice") }
    }

    private fun parsePaymentTermsDays(terms: String): Long {
        val clean = terms.trim().lowercase()
        return if (clean.contains("cash") || clean.contains("tunai") || clean.contains("cod")) 0L
        else terms.split(" ").firstOrNull()?.toLongOrNull() ?: 14L
    }

    private fun computeInvoiceStatus(total: Double, paid: Double, dueDate: Long? = null): String {
        return when {
            paid >= total - 0.01 -> "PAID"
            paid > 0 -> if (dueDate != null && System.currentTimeMillis() > dueDate) "OVERDUE" else "PARTIAL"
            else -> if (dueDate != null && System.currentTimeMillis() > dueDate) "OVERDUE" else "UNPAID"
        }
    }

    /**
     * Tambah pembayaran: POST payment → update invoice status → create cashflow MASUK.
     * Business logic: setiap pembayaran → kas bertambah (cashflow MASUK).
     */
    suspend fun addPayment(
        invoiceId: Long,
        invoiceNumber: String,
        amount: Double,
        method: String,
        notes: String?,
        cashflowRepo: BmpCashFlowRepository
    ): OnlineWriteResult {
        return try {
            val payResp = api.createPayment(mapOf(
                "invoiceId" to invoiceId,
                "paymentDate" to System.currentTimeMillis(),
                "paymentAmount" to amount,
                "paymentMethod" to method,
                "notes" to notes
            ))
            val payId = (payResp.body()?.get("id") as? Number)?.toLong()

            // Recalculate invoice status
            val inv = getById(invoiceId)
            if (inv != null) {
                val allPayments = getPaymentsByInvoice(invoiceId)
                val totalPaid = allPayments.sumOf { it.paymentAmount } + amount
                val newStatus = computeInvoiceStatus(inv.totalAmount, totalPaid, inv.dueDate)
                api.updateInvoice(invoiceId, mapOf(
                    "status" to newStatus,
                    "paidAmount" to totalPaid
                ))
            }

            // Business logic: payment → cashflow MASUK
            cashflowRepo.createEntry(BmpCashflowData(
                transactionType = "MASUK",
                description = "Pembayaran Invoice $invoiceNumber",
                amount = amount,
                paymentRefId = payId,
                transactionDate = System.currentTimeMillis()
            ))

            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal catat pembayaran") }
    }

    suspend fun editPayment(
        context: android.content.Context,
        tenantId: String,
        paymentId: Long,
        amount: Double,
        method: String,
        notes: String?
    ): OnlineWriteResult {
        return try {
            api.updatePayment(paymentId, mapOf(
                "paymentAmount" to amount,
                "paymentMethod" to method,
                "notes" to notes
            ))
            val allPayments = api.getPayments().body()?.map { it.toBmpPaymentData() } ?: emptyList()
            val pay = allPayments.find { it.id == paymentId }
            if (pay != null) {
                val invId = pay.invoiceId
                val inv = getById(invId)
                if (inv != null) {
                    val invoicePayments = allPayments.filter { it.invoiceId == invId && it.id != paymentId }
                    val totalPaid = invoicePayments.sumOf { it.paymentAmount } + amount
                    val newStatus = computeInvoiceStatus(inv.totalAmount, totalPaid, inv.dueDate)
                    api.updateInvoice(invId, mapOf(
                        "status" to newStatus,
                        "paidAmount" to totalPaid
                    ))
                }
            }
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal ubah pembayaran") }
    }

    suspend fun deletePayment(
        context: android.content.Context,
        tenantId: String,
        paymentId: Long
    ): OnlineWriteResult {
        return try {
            val allPayments = api.getPayments().body()?.map { it.toBmpPaymentData() } ?: emptyList()
            val pay = allPayments.find { it.id == paymentId }
            if (pay != null) {
                val invId = pay.invoiceId
                val inv = getById(invId)
                if (inv != null) {
                    val invoicePayments = allPayments.filter { it.invoiceId == invId && it.id != paymentId }
                    val totalPaid = invoicePayments.sumOf { it.paymentAmount }
                    val newStatus = computeInvoiceStatus(inv.totalAmount, totalPaid, inv.dueDate)
                    api.updateInvoice(invId, mapOf(
                        "status" to newStatus,
                        "paidAmount" to totalPaid
                    ))
                }
            }
            api.deletePayment(paymentId)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus pembayaran") }
    }

    suspend fun deleteInvoice(
        context: android.content.Context,
        tenantId: String,
        id: Long
    ): OnlineWriteResult {
        return deleteInvoice(id, cashflowRepo, stockRepo)
    }

    fun observeProducts(invoiceId: Long): Flow<List<com.posbah.app.data.local.entities.BmpProductEntity>> =
        kotlinx.coroutines.flow.flow {
            emit(getProductsByInvoice(invoiceId).map {
                com.posbah.app.data.local.entities.BmpProductEntity(
                    id = it.id,
                    tenantId = it.tenantId,
                    invoiceId = it.invoiceId,
                    masterItemID = it.masterItemID,
                    title = it.name,
                    price = it.price,
                    jumlahLusin = it.jumlahLusin.toDouble(),
                    quantity = it.quantity.toDouble(),
                    isKhusus = it.isKhusus,
                    hargaBeli = it.hargaBeli,
                    currency = "IDR",
                    uniqueID = null,
                    isDeleted = false,
                    isSynced = true
                )
            })
        }

    suspend fun markAsUnsynced(invoiceId: Long) {}

    sealed class RemoteSignatureResult {
        data class Success(val url: String, val name: String) : RemoteSignatureResult()
        data class Error(val message: String) : RemoteSignatureResult()
        object Pending : RemoteSignatureResult()
    }

    suspend fun checkReceiverSignatureRemote(
        tenantId: String,
        invoiceId: Long
    ): RemoteSignatureResult {
        return try {
            val inv = getById(invoiceId)
            if (inv != null && !inv.receiverSignatureUrl.isNullOrEmpty()) {
                RemoteSignatureResult.Success(inv.receiverSignatureUrl, inv.receiverNameActual ?: "")
            } else {
                RemoteSignatureResult.Pending
            }
        } catch (e: Exception) {
            RemoteSignatureResult.Error(e.message ?: "Gagal memeriksa tanda tangan remote")
        }
    }

    suspend fun saveReceiverSignature(
        context: android.content.Context,
        tenantId: String,
        invoiceId: Long,
        localPath: String?,
        signatureUrl: String?,
        receiverName: String
    ): OnlineWriteResult {
        return try {
            api.updateInvoice(invoiceId, mapOf(
                "receiverSignaturePath" to localPath,
                "receiverSignatureUrl" to signatureUrl,
                "receiverNameActual" to receiverName
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan tanda tangan") }
    }

    suspend fun createInvoice(
        context: android.content.Context,
        invoice: com.posbah.app.data.local.entities.BmpInvoiceEntity,
        products: List<com.posbah.app.data.local.entities.BmpProductEntity>
    ): Pair<Long, OnlineWriteResult> {
        val invoiceData = BmpInvoiceData(
            id = invoice.id,
            tenantId = invoice.tenantId,
            clientId = invoice.clientId ?: 0L,
            number = invoice.number,
            status = invoice.status,
            totalAmount = invoice.totalAmount,
            paidAmount = invoice.paidAmount,
            paymentTerms = invoice.paymentTerms,
            dueDate = invoice.dueDate,
            notes = invoice.notes,
            createdAt = invoice.createdAt
        )
        val productDataList = products.map {
            BmpProductItemData(
                id = it.id,
                tenantId = it.tenantId,
                invoiceId = it.invoiceId ?: 0L,
                masterItemID = it.masterItemID,
                name = it.title,
                price = it.price,
                quantity = it.quantity.toInt(),
                jumlahLusin = it.jumlahLusin.toInt(),
                hargaBeli = it.hargaBeli,
                isKhusus = it.isKhusus
            )
        }
        return createInvoice(invoiceData, productDataList, cashflowRepo, stockRepo)
    }

    suspend fun updateInvoice(
        context: android.content.Context,
        invoice: com.posbah.app.data.local.entities.BmpInvoiceEntity,
        products: List<com.posbah.app.data.local.entities.BmpProductEntity>
    ): OnlineWriteResult {
        val invoiceData = BmpInvoiceData(
            id = invoice.id,
            tenantId = invoice.tenantId,
            clientId = invoice.clientId ?: 0L,
            number = invoice.number,
            status = invoice.status,
            totalAmount = invoice.totalAmount,
            paidAmount = invoice.paidAmount,
            paymentTerms = invoice.paymentTerms,
            dueDate = invoice.dueDate,
            notes = invoice.notes,
            createdAt = invoice.createdAt
        )
        val productDataList = products.map {
            BmpProductItemData(
                id = it.id,
                tenantId = it.tenantId,
                invoiceId = it.invoiceId ?: 0L,
                masterItemID = it.masterItemID,
                name = it.title,
                price = it.price,
                quantity = it.quantity.toInt(),
                jumlahLusin = it.jumlahLusin.toInt(),
                hargaBeli = it.hargaBeli,
                isKhusus = it.isKhusus
            )
        }
        return updateInvoice(invoiceData, productDataList, cashflowRepo, stockRepo)
    }

    fun observePayments(invoiceId: Long): kotlinx.coroutines.flow.Flow<List<BmpInvoicePaymentEntity>> =
        kotlinx.coroutines.flow.flow {
            emit(getPaymentsByInvoice(invoiceId).map {
                BmpInvoicePaymentEntity(
                    id = it.id,
                    tenantId = it.tenantId,
                    invoiceId = it.invoiceId,
                    paymentDate = it.paymentDate,
                    paymentAmount = it.paymentAmount,
                    paymentMethod = it.paymentMethod,
                    notes = it.notes,
                    isSynced = true,
                    isDeleted = it.isDeleted
                )
            })
        }

    suspend fun recordPayment(
        context: android.content.Context,
        tenantId: String,
        invoiceId: Long,
        amount: Double,
        method: String,
        notes: String?
    ): OnlineWriteResult {
        val inv = getById(invoiceId) ?: return OnlineWriteResult.Error("Invoice tidak ditemukan")
        return addPayment(
            invoiceId = invoiceId,
            invoiceNumber = inv.number,
            amount = amount,
            method = method,
            notes = notes,
            cashflowRepo = cashflowRepo
        )
    }
}


// ── BmpMasterProductRepository ────────────────────────────────────────────────

@Singleton
class BmpMasterProductRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    private val _items = MutableStateFlow<List<BmpMasterProductData>>(emptyList())
    val items = _items.asStateFlow()

    suspend fun refresh() {
        try {
            val resp = api.getMasterProducts()
            if (resp.isSuccessful) {
                _items.value = resp.body()?.map { it.toBmpMasterProductData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpMasterProductData> = try {
        api.getMasterProducts().body()?.map { it.toBmpMasterProductData() } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    suspend fun getById(id: Long): BmpMasterProductData? = list().find { it.id == id }

    suspend fun upsert(item: BmpMasterProductData): OnlineWriteResult {
        return try {
            val body = mapOf<String, Any?>(
                "title" to item.title,
                "sku" to item.sku,
                "hppTotalPcs" to item.hppTotalPcs,
                "pricePerPcs" to item.pricePerPcs,
                "currentStock" to item.currentStock
            )
            if (item.id == 0L) api.createMasterProduct(body)
            else api.updateMasterProduct(item.id, body)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan master produk") }
    }

    suspend fun delete(id: Long): OnlineWriteResult {
        return try {
            api.deleteMasterProduct(id)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus") }
    }

    fun observe(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpMasterProductEntity>> =
        kotlinx.coroutines.flow.flow {
            emit(list().map {
                com.posbah.app.data.local.entities.BmpMasterProductEntity(
                    id = it.id,
                    tenantId = it.tenantId,
                    title = it.title,
                    description = null,
                    unit = "Kg",
                    price = it.pricePerPcs,
                    beratGram = 0.0,
                    cycleTime = 0.0,
                    cavity = 1,
                    rejectRate = 0.0,
                    uniqueID = null,
                    slug = null,
                    isDeleted = it.isDeleted,
                    jenisBahanBaku = "",
                    image = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = it.updatedAt,
                    isSynced = true,
                    hppTotalPcs = it.hppTotalPcs,
                    hppLusin = 0.0
                )
            })
        }
}

// ── BmpCashFlowRepository ─────────────────────────────────────────────────────

@Singleton
class BmpCashFlowRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    private val _entries = MutableStateFlow<List<BmpCashflowData>>(emptyList())
    val entries = _entries.asStateFlow()

    suspend fun refresh() {
        try {
            val resp = api.getCashflow()
            if (resp.isSuccessful) {
                _entries.value = resp.body()?.map { it.toBmpCashflowData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpCashflowData> = try {
        api.getCashflow().body()?.map { it.toBmpCashflowData() } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    suspend fun createEntry(entry: BmpCashflowData): Long {
        return try {
            val resp = api.createCashflow(mapOf(
                "transactionDate" to entry.transactionDate,
                "transactionType" to entry.transactionType,
                "description" to entry.description,
                "amount" to entry.amount,
                "paymentRefId" to entry.paymentRefId
            ))
            (resp.body()?.get("id") as? Number)?.toLong() ?: 0L
        } catch (_: Exception) { 0L }
    }

    suspend fun update(entry: BmpCashflowData): OnlineWriteResult {
        return try {
            api.updateCashflow(entry.id, mapOf(
                "transactionDate" to entry.transactionDate,
                "transactionType" to entry.transactionType,
                "description" to entry.description,
                "amount" to entry.amount
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal update cashflow") }
    }

    suspend fun delete(id: Long): OnlineWriteResult {
        return try {
            api.deleteCashflow(id)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus cashflow") }
    }

    fun saldo(): Double = _entries.value.sumOf {
        if (it.transactionType == "MASUK") it.amount else -it.amount
    }

    /** Observe cashflow sebagai Flow<List<BmpCashFlowEntity>> — backward compat */
    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.BmpCashFlowEntity>> {
        return _entries.map { list ->
            list.map { d ->
                com.posbah.app.data.local.entities.BmpCashFlowEntity(
                    id = d.id,
                    tenantId = d.tenantId,
                    transactionType = d.transactionType,
                    description = d.description,
                    amount = d.amount,
                    transactionDate = d.transactionDate,
                    isSynced = true
                )
            }
        }
    }

    fun totalIn(tenantId: String): kotlinx.coroutines.flow.Flow<Double> =
        _entries.map { list -> list.filter { it.transactionType == "MASUK" }.sumOf { it.amount } }

    fun totalOut(tenantId: String): kotlinx.coroutines.flow.Flow<Double> =
        _entries.map { list -> list.filter { it.transactionType == "KELUAR" }.sumOf { it.amount } }

    /** Insert via entity — backward compat untuk CashFlowScreen */
    suspend fun insert(entity: com.posbah.app.data.local.entities.BmpCashFlowEntity) {
        createEntry(BmpCashflowData(
            tenantId = entity.tenantId,
            transactionType = entity.transactionType,
            description = entity.description,
            amount = entity.amount,
            transactionDate = entity.transactionDate
        ))
    }
}


// ── BmpEmployeeRepository ─────────────────────────────────────────────────────

@Singleton
class BmpEmployeeRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences,
    private val payrollRepo: BmpPayrollRepository
) {
    private val _employees = MutableStateFlow<List<BmpEmployeeData>>(emptyList())
    val employees = _employees.asStateFlow()

    suspend fun refresh() {
        try {
            val resp = api.getBmpEmployees()
            if (resp.isSuccessful) {
                _employees.value = resp.body()?.map {
                    BmpEmployeeData(
                        id = (it["id"] as? Number)?.toLong() ?: 0,
                        tenantId = it["tenantId"] as? String ?: "",
                        name = it["name"] as? String ?: "",
                        role = it["role"] as? String ?: "KARYAWAN",
                        salary = (it["salary"] as? Number)?.toDouble() ?: 0.0,
                        phone = it["phone"] as? String,
                        email = it["email"] as? String,
                        isActive = it["isActive"] as? Boolean ?: true,
                        updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: 0
                    )
                } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpEmployeeData> = _employees.value.ifEmpty {
        try { api.getBmpEmployees().body()?.map {
            BmpEmployeeData(
                id = (it["id"] as? Number)?.toLong() ?: 0,
                tenantId = it["tenantId"] as? String ?: "",
                name = it["name"] as? String ?: "",
                role = it["role"] as? String ?: "KARYAWAN",
                salary = (it["salary"] as? Number)?.toDouble() ?: 0.0,
                phone = it["phone"] as? String,
                email = it["email"] as? String,
                isActive = it["isActive"] as? Boolean ?: true,
                updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: 0
            )
        } ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    suspend fun upsert(emp: BmpEmployeeData): OnlineWriteResult {
        return try {
            val body = mapOf<String, Any?>(
                "name" to emp.name, "role" to emp.role, "salary" to emp.salary,
                "phone" to emp.phone, "email" to emp.email, "isActive" to emp.isActive
            )
            if (emp.id == 0L) api.createBmpEmployee(body) else api.updateBmpEmployee(emp.id, body)
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan karyawan") }
    }

    suspend fun delete(id: Long): OnlineWriteResult {
        return try { api.deleteBmpEmployee(id); OnlineWriteResult.Success }
        catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus karyawan") }
    }

    fun observe(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpEmployeeEntity>> =
        kotlinx.coroutines.flow.flow {
            emit(list().map {
                com.posbah.app.data.local.entities.BmpEmployeeEntity(
                    id = it.id,
                    tenantId = it.tenantId,
                    name = it.name,
                    position = it.role,
                    salaryAmount = it.salary,
                    isActive = it.isActive,
                    fingerprintPIN = null,
                    employeeId = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = it.updatedAt,
                    isSynced = true
                )
            })
        }

    fun observePayrolls(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpPayrollEntity>> =
        kotlinx.coroutines.flow.flow {
            emit(payrollRepo.list().map {
                com.posbah.app.data.local.entities.BmpPayrollEntity(
                    id = it.id.toString(),
                    tenantId = it.tenantId,
                    employeeId = it.employeeId,
                    paymentDate = it.paymentDate,
                    amount = it.amount,
                    attendanceCount = 0,
                    dailyRate = 0.0,
                    description = it.notes ?: ""
                )
            })
        }

    suspend fun upsert(e: com.posbah.app.data.local.entities.BmpEmployeeEntity): Long {
        val data = BmpEmployeeData(
            id = e.id,
            tenantId = e.tenantId,
            name = e.name,
            role = e.position ?: "KARYAWAN",
            salary = e.salaryAmount,
            phone = null,
            email = null,
            isActive = e.isActive,
            updatedAt = System.currentTimeMillis()
        )
        val res = upsert(data)
        return e.id
    }

    suspend fun softDelete(id: Long) {
        delete(id)
    }

    suspend fun insertPayroll(payroll: com.posbah.app.data.local.entities.BmpPayrollEntity): Long {
        val data = BmpPayrollData(
            id = 0,
            tenantId = payroll.tenantId,
            employeeId = payroll.employeeId,
            employeeName = "",
            paymentDate = payroll.paymentDate,
            amount = payroll.amount,
            notes = payroll.description,
            updatedAt = System.currentTimeMillis()
        )
        payrollRepo.createPayroll(data)
        return payroll.employeeId
    }
}

// ── BmpPayrollRepository ──────────────────────────────────────────────────────

@Singleton
class BmpPayrollRepository @Inject constructor(
    private val api: BmpApiService,
    private val cashflowRepo: BmpCashFlowRepository,
    private val securePrefs: SecurePreferences
) {
    suspend fun list(): List<BmpPayrollData> = try {
        api.getPayrolls().body()?.map {
            BmpPayrollData(
                id = (it["id"] as? Number)?.toLong() ?: 0,
                tenantId = it["tenantId"] as? String ?: "",
                employeeId = (it["employeeId"] as? Number)?.toLong() ?: 0,
                employeeName = it["employeeName"] as? String ?: "",
                paymentDate = (it["paymentDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                notes = it["notes"] as? String,
                updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: 0
            )
        } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    /**
     * Bayar gaji → POST payroll + business logic: cashflow KELUAR otomatis.
     */
    suspend fun createPayroll(payroll: BmpPayrollData): OnlineWriteResult {
        return try {
            api.createPayroll(mapOf(
                "employeeId" to payroll.employeeId,
                "employeeName" to payroll.employeeName,
                "paymentDate" to payroll.paymentDate,
                "amount" to payroll.amount,
                "notes" to payroll.notes
            ))
            // Business logic: gaji dibayar → kas berkurang
            cashflowRepo.createEntry(BmpCashflowData(
                transactionType = "KELUAR",
                description = "Gaji Karyawan: ${payroll.employeeName}",
                amount = payroll.amount,
                transactionDate = payroll.paymentDate
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal bayar gaji") }
    }
}

// ── BmpBahanBakuRepository ────────────────────────────────────────────────────

@Singleton
class BmpBahanBakuRepository @Inject constructor(
    private val api: BmpApiService,
    private val cashflowRepo: BmpCashFlowRepository,
    private val securePrefs: SecurePreferences
) {
    private val _bahanBaku = MutableStateFlow<List<BmpBahanBakuData>>(emptyList())
    val bahanBaku = _bahanBaku.asStateFlow()

    suspend fun refresh() {
        try {
            val resp = api.getBahanBaku()
            if (resp.isSuccessful) {
                _bahanBaku.value = resp.body()?.map { it.toBmpBahanBakuData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpBahanBakuData> = try {
        api.getBahanBaku().body()?.map { it.toBmpBahanBakuData() } ?: emptyList()
    } catch (_: Exception) { emptyList() }



    /**
     * Simpan bahan baku + items.
     * Business logic TETAP: input bahan baku → OTOMATIS potong cashflow (KELUAR).
     */
    suspend fun create(
        bahanBaku: BmpBahanBakuData,
        items: List<BmpBahanBakuItemData>
    ): OnlineWriteResult {
        return try {
            val resp = api.createBahanBaku(mapOf(
                "nomorNota" to bahanBaku.nomorNota,
                "tanggal" to bahanBaku.tanggal,
                "supplier" to bahanBaku.supplier,
                "totalBiaya" to bahanBaku.totalBiaya,
                "notes" to bahanBaku.notes
            ))
            val newId = (resp.body()?.get("id") as? Number)?.toLong() ?: 0L

            // POST items
            val itemBodies = items.map {
                mapOf<String, Any?>(
                    "bahanBakuId" to newId,
                    "name" to it.name,
                    "quantity" to it.quantity,
                    "unit" to it.unit,
                    "pricePerUnit" to it.pricePerUnit,
                    "subtotal" to it.subtotal
                )
            }
            api.createBahanBakuItems(itemBodies)

            // ── Business logic: input bahan baku → potong cashflow ──────────
            cashflowRepo.createEntry(BmpCashflowData(
                transactionType = "KELUAR",
                description = "Pembelian Bahan Baku: ${bahanBaku.nomorNota}",
                amount = bahanBaku.totalBiaya,
                transactionDate = bahanBaku.tanggal
            ))

            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan bahan baku") }
    }

    suspend fun update(bahanBaku: BmpBahanBakuData): OnlineWriteResult {
        return try {
            api.updateBahanBaku(bahanBaku.id, mapOf(
                "nomorNota" to bahanBaku.nomorNota,
                "tanggal" to bahanBaku.tanggal,
                "supplier" to bahanBaku.supplier,
                "totalBiaya" to bahanBaku.totalBiaya,
                "notes" to bahanBaku.notes
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal update bahan baku") }
    }

    suspend fun delete(id: Long): OnlineWriteResult {
        return try { api.deleteBahanBaku(id); OnlineWriteResult.Success }
        catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal hapus bahan baku") }
    }

    /** Context overload — backward compat untuk BahanBakuListViewModel */
    suspend fun delete(context: android.content.Context, tenantId: String, id: Long): OnlineWriteResult = delete(id)

    /** Bayar hutang bahan baku — update paidAmount di VPS */
    suspend fun payDebt(context: android.content.Context, tenantId: String, id: Long, amount: Double): OnlineWriteResult {
        return try {
            api.updateBahanBaku(id, mapOf("paidAmount" to amount))
            // Refresh setelah update
            refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal bayar hutang") }
    }

    /** Get by id — returns BmpBahanBakuEntity for backward compat */
    suspend fun getById(id: Long): com.posbah.app.data.local.entities.BmpBahanBakuEntity? {
        val data = try {
            api.getBahanBaku().body()?.map { it.toBmpBahanBakuData() }?.find { it.id == id }
        } catch (_: Exception) { null } ?: return null
        return com.posbah.app.data.local.entities.BmpBahanBakuEntity(
            id = data.id,
            tenantId = data.tenantId,
            noTagihan = data.nomorNota,
            tanggal = data.tanggal,
            totalHarga = data.totalBiaya,
            nominal = data.paidAmount,
            notes = data.notes
        )
    }

    /** Get by nomor tagihan */
    suspend fun getByTagihan(noTagihan: String): com.posbah.app.data.local.entities.BmpBahanBakuEntity? {
        val data = try {
            api.getBahanBaku().body()?.map { it.toBmpBahanBakuData() }?.find { it.nomorNota == noTagihan }
        } catch (_: Exception) { null } ?: return null
        return com.posbah.app.data.local.entities.BmpBahanBakuEntity(
            id = data.id, tenantId = data.tenantId, noTagihan = data.nomorNota,
            tanggal = data.tanggal, totalHarga = data.totalBiaya, nominal = data.paidAmount, notes = data.notes
        )
    }

    suspend fun getByTagihan(tenantId: String, noTagihan: String): com.posbah.app.data.local.entities.BmpBahanBakuEntity? =
        getByTagihan(noTagihan)

    /** Observe items for a specific bahan baku id — returns Flow<List<BmpBahanBakuItemEntity>> */
    fun observeItems(bahanBakuId: Long): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>> {
        return kotlinx.coroutines.flow.flow {
            val items = getItems(bahanBakuId)
            emit(items.map { d ->
                com.posbah.app.data.local.entities.BmpBahanBakuItemEntity(
                    id = d.id, tenantId = "", bahanBakuId = d.bahanBakuId,
                    jenisBahan = d.name, kuantitas = d.quantity, unit = d.unit, rate = d.pricePerUnit
                )
            })
        }
    }

    /** Update header only — alias for update() */
    suspend fun updateHeaderOnly(entity: com.posbah.app.data.local.entities.BmpBahanBakuEntity): OnlineWriteResult {
        return update(BmpBahanBakuData(
            id = entity.id, tenantId = entity.tenantId, nomorNota = entity.noTagihan,
            tanggal = entity.tanggal, totalBiaya = entity.totalHarga,
            paidAmount = entity.nominal, notes = entity.notes
        ))
    }

    /** Save (create or update) — alias based on entity id */
    suspend fun save(
        entity: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): OnlineWriteResult {
        val data = BmpBahanBakuData(
            id = entity.id, tenantId = entity.tenantId, nomorNota = entity.noTagihan,
            tanggal = entity.tanggal, totalBiaya = entity.totalHarga,
            paidAmount = entity.nominal, notes = entity.notes
        )
        val itemData = items.map {
            BmpBahanBakuItemData(
                id = it.id, bahanBakuId = it.bahanBakuId, name = it.jenisBahan,
                quantity = it.kuantitas, unit = it.unit, pricePerUnit = it.rate,
                subtotal = it.kuantitas * it.rate
            )
        }
        return if (entity.id == 0L) create(data, itemData) else update(data)
    }

    suspend fun save(
        context: android.content.Context,
        entity: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): Pair<Long, OnlineWriteResult> {
        val res = save(entity, items)
        return Pair(entity.id, res)
    }

    suspend fun update(
        context: android.content.Context,
        originalNominal: Double,
        entity: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): OnlineWriteResult {
        return save(entity, items)
    }

    suspend fun getItems(bahanBakuId: Long): List<BmpBahanBakuItemData> = try {
        api.getBahanBakuItems(bahanBakuId).body()?.map {
            BmpBahanBakuItemData(
                id = (it["id"] as? Number)?.toLong() ?: 0,
                bahanBakuId = bahanBakuId,
                name = it["name"] as? String ?: "",
                quantity = (it["quantity"] as? Number)?.toDouble() ?: 0.0,
                unit = it["unit"] as? String ?: "kg",
                pricePerUnit = (it["pricePerUnit"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (it["subtotal"] as? Number)?.toDouble() ?: 0.0
            )
        } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    /**
     * Observe bahan baku sebagai Flow<List<BmpBahanBakuEntity>> — backward compat untuk ViewModel.
     */
    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.BmpBahanBakuEntity>> {
        return _bahanBaku.map { list ->
            list.map { d ->
                com.posbah.app.data.local.entities.BmpBahanBakuEntity(
                    id = d.id,
                    tenantId = d.tenantId,
                    noTagihan = d.nomorNota,
                    totalHarga = d.totalBiaya,
                    nominal = d.paidAmount,
                    notes = d.notes
                )
            }
        }
    }

    fun totalHarga(tenantId: String): kotlinx.coroutines.flow.Flow<Double> =
        _bahanBaku.map { list -> list.sumOf { it.totalBiaya } }

    fun totalNominal(tenantId: String): kotlinx.coroutines.flow.Flow<Double> =
        _bahanBaku.map { list -> list.sumOf { it.paidAmount } }
}

// ── BmpStockRepository ────────────────────────────────────────────────────────

@Singleton
class BmpStockRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    suspend fun adjustStock(
        masterItemId: Long,
        change: Int,
        mutationType: String,
        referenceId: Long? = null,
        notes: String? = null
    ) {
        try {
            // 1. Get current stock
            val stocks = api.getProductStocks().body()
            val currentEntry = stocks?.find { (it["masterItemId"] as? Number)?.toLong() == masterItemId }
            val currentStock = (currentEntry?.get("currentStock") as? Number)?.toInt() ?: 0
            val stockEntryId = (currentEntry?.get("id") as? Number)?.toLong()

            val newStock = (currentStock + change).coerceAtLeast(0)

            // 2. Update stock
            if (stockEntryId != null) {
                api.createProductStock(mapOf(
                    "masterItemId" to masterItemId,
                    "currentStock" to newStock
                ))
            } else {
                api.createProductStock(mapOf(
                    "masterItemId" to masterItemId,
                    "currentStock" to newStock
                ))
            }

            // 3. Append ledger entry
            api.addStockLedgerEntry(mapOf(
                "masterItemId" to masterItemId,
                "mutationType" to mutationType,
                "change" to change,
                "stockAfter" to newStock,
                "referenceId" to referenceId,
                "notes" to notes
            ))
        } catch (_: Exception) {}
    }

    suspend fun getStocksForMaster(masterItemId: Long): BmpProductStockData? {
        return try {
            api.getProductStocks().body()
                ?.find { (it["masterItemId"] as? Number)?.toLong() == masterItemId }
                ?.let {
                    BmpProductStockData(
                        id = (it["id"] as? Number)?.toLong() ?: 0,
                        tenantId = it["tenantId"] as? String ?: "",
                        masterItemId = masterItemId,
                        currentStock = (it["currentStock"] as? Number)?.toInt() ?: 0
                    )
                }
        } catch (_: Exception) { null }
    }

    suspend fun getLedger(masterItemId: Long): List<BmpStockLedgerData> = try {
        api.getStockLedger().body()
            ?.filter { (it["masterItemId"] as? Number)?.toLong() == masterItemId }
            ?.map {
                BmpStockLedgerData(
                    id = (it["id"] as? Number)?.toLong() ?: 0,
                    tenantId = it["tenantId"] as? String ?: "",
                    masterItemId = masterItemId,
                    mutationType = it["mutationType"] as? String ?: "",
                    change = (it["change"] as? Number)?.toInt() ?: 0,
                    stockAfter = (it["stockAfter"] as? Number)?.toInt() ?: 0,
                    referenceId = (it["referenceId"] as? Number)?.toLong(),
                    notes = it["notes"] as? String,
                    createdAt = (it["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun observeStocks(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpProductStockEntity>> =
        kotlinx.coroutines.flow.flow {
            try {
                val stocks = api.getProductStocks().body()?.map {
                    com.posbah.app.data.local.entities.BmpProductStockEntity(
                        id = (it["id"] as? Number)?.toLong() ?: 0,
                        tenantId = it["tenantId"] as? String ?: tenantId,
                        outletId = (it["outletId"] as? Number)?.toLong(),
                        masterProductId = (it["masterItemId"] as? Number)?.toLong() ?: 0,
                        quantity = (it["currentStock"] as? Number)?.toDouble() ?: 0.0,
                        minStockAlert = 0.0,
                        isSynced = true,
                        isDeleted = false,
                        updatedAt = System.currentTimeMillis()
                    )
                } ?: emptyList()
                emit(stocks)
            } catch (_: Exception) { emit(emptyList()) }
        }

    fun observeAllLedger(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpStockLedgerEntity>> =
        kotlinx.coroutines.flow.flow {
            try {
                val ledgers = api.getStockLedger().body()?.map {
                    com.posbah.app.data.local.entities.BmpStockLedgerEntity(
                        id = (it["id"] as? Number)?.toLong() ?: 0,
                        tenantId = it["tenantId"] as? String ?: tenantId,
                        masterProductId = (it["masterItemId"] as? Number)?.toLong() ?: 0,
                        referenceId = (it["referenceId"] as? Number)?.toLong() ?: 0L,
                        mutationType = it["mutationType"] as? String ?: "",
                        quantityChange = (it["change"] as? Number)?.toDouble() ?: 0.0,
                        finalStock = (it["stockAfter"] as? Number)?.toDouble() ?: 0.0,
                        notes = it["notes"] as? String,
                        isSynced = true,
                        isDeleted = false,
                        createdAt = (it["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                emit(ledgers)
            } catch (_: Exception) { emit(emptyList()) }
        }
}

// ── BmpSettingsRepository ─────────────────────────────────────────────────────

@Singleton
class BmpSettingsRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    suspend fun get(): BmpSettingsData? = try {
        api.getBmpSettings().body()?.let {
            BmpSettingsData(
                id = (it["id"] as? Number)?.toLong() ?: 0,
                tenantId = it["tenantId"] as? String ?: "",
                companyName = it["companyName"] as? String ?: "",
                address = it["address"] as? String,
                phone = it["phone"] as? String,
                email = it["email"] as? String,
                npwp = it["npwp"] as? String,
                logoUrl = it["logoUrl"] as? String,
                bankInfo = it["bankInfo"] as? String,
                invoicePrefix = it["invoicePrefix"] as? String ?: "INV",
                listrikBulanan = (it["listrikBulanan"] as? Number)?.toDouble() ?: 30_000_000.0,
                jumlahMesin = (it["jumlahMesin"] as? Number)?.toInt() ?: 5,
                jumlahKaryawan = (it["jumlahKaryawan"] as? Number)?.toInt() ?: 19,
                gajiHarian = (it["gajiHarian"] as? Number)?.toDouble() ?: 80_000.0,
                hariKerjaSebulan = (it["hariKerjaSebulan"] as? Number)?.toInt() ?: 26,
                biayaKarungPer1000 = (it["biayaKarungPer1000"] as? Number)?.toDouble() ?: 2_100_000.0,
                hoursPerDay = (it["hoursPerDay"] as? Number)?.toInt() ?: 24,
                updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: 0
            )
        }
    } catch (_: Exception) { null }

    suspend fun save(settings: BmpSettingsData): OnlineWriteResult {
        return try {
            api.saveBmpSettings(mapOf(
                "companyName" to settings.companyName,
                "address" to settings.address,
                "phone" to settings.phone,
                "email" to settings.email,
                "npwp" to settings.npwp,
                "logoUrl" to settings.logoUrl,
                "bankInfo" to settings.bankInfo,
                "invoicePrefix" to settings.invoicePrefix,
                "listrikBulanan" to settings.listrikBulanan,
                "jumlahMesin" to settings.jumlahMesin,
                "jumlahKaryawan" to settings.jumlahKaryawan,
                "gajiHarian" to settings.gajiHarian,
                "hariKerjaSebulan" to settings.hariKerjaSebulan,
                "biayaKarungPer1000" to settings.biayaKarungPer1000,
                "hoursPerDay" to settings.hoursPerDay
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan settings") }
    }

    fun observe(tenantId: String): Flow<com.posbah.app.data.local.entities.BmpSettingsEntity?> =
        kotlinx.coroutines.flow.flow {
            val data = get()
            if (data != null) {
                emit(com.posbah.app.data.local.entities.BmpSettingsEntity(
                    id = data.id,
                    tenantId = data.tenantId,
                    clientName = data.companyName,
                    clientLogo = data.logoUrl,
                    addressLine1 = data.address,
                    phoneNumber = data.phone,
                    emailAddress = data.email,
                    taxNumber = data.npwp,
                    listrikBulanan = data.listrikBulanan,
                    jumlahMesin = data.jumlahMesin,
                    jumlahKaryawan = data.jumlahKaryawan,
                    gajiHarian = data.gajiHarian,
                    hariKerjaSebulan = data.hariKerjaSebulan,
                    biayaKarungPer1000 = data.biayaKarungPer1000,
                    hoursPerDay = data.hoursPerDay,
                    updatedAt = data.updatedAt
                ))
            } else {
                emit(null)
            }
        }

    suspend fun get(tenantId: String): com.posbah.app.data.local.entities.BmpSettingsEntity? {
        val data = get() ?: return null
        return com.posbah.app.data.local.entities.BmpSettingsEntity(
            id = data.id,
            tenantId = data.tenantId,
            clientName = data.companyName,
            clientLogo = data.logoUrl,
            addressLine1 = data.address,
            phoneNumber = data.phone,
            emailAddress = data.email,
            taxNumber = data.npwp,
            listrikBulanan = data.listrikBulanan,
            jumlahMesin = data.jumlahMesin,
            jumlahKaryawan = data.jumlahKaryawan,
            gajiHarian = data.gajiHarian,
            hariKerjaSebulan = data.hariKerjaSebulan,
            biayaKarungPer1000 = data.biayaKarungPer1000,
            hoursPerDay = data.hoursPerDay,
            updatedAt = data.updatedAt
        )
    }
}

// ── PrintSettingsRepository ───────────────────────────────────────────────────

@Singleton
class PrintSettingsRepository @Inject constructor(
    private val api: PosApiService,
    private val securePrefs: SecurePreferences
) {
    suspend fun get(moduleKey: String): PrintSettingsData? = try {
        api.getPrintSettings(moduleKey).body()?.firstOrNull()?.let {
            PrintSettingsData(
                id = (it["id"] as? Number)?.toLong() ?: 0,
                tenantId = it["tenantId"] as? String ?: "",
                moduleKey = moduleKey,
                paperSize = it["paperSize"] as? String ?: "A4",
                showLogo = it["showLogo"] as? Boolean ?: true,
                showSignature = it["showSignature"] as? Boolean ?: false,
                headerText = it["headerText"] as? String,
                footerText = it["footerText"] as? String ?: "Terima kasih!",
                updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: 0
            )
        }
    } catch (_: Exception) { null }

    suspend fun save(settings: PrintSettingsData): OnlineWriteResult {
        return try {
            api.savePrintSettings(mapOf(
                "moduleKey" to settings.moduleKey,
                "paperSize" to settings.paperSize,
                "showLogo" to settings.showLogo,
                "showSignature" to settings.showSignature,
                "headerText" to settings.headerText,
                "footerText" to settings.footerText
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan print settings") }
    }

    /**
     * Observe print settings sebagai Flow — backward compat untuk ViewModel lama.
     * Fetch sekali dari VPS dan emit hasilnya.
     */
    fun observe(tenantId: String, moduleKey: String): kotlinx.coroutines.flow.Flow<PrintSettingsData?> =
        kotlinx.coroutines.flow.flow {
            emit(get(moduleKey))
        }
}
