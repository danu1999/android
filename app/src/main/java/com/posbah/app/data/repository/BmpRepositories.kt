package com.posbah.app.data.repository

import com.posbah.app.data.remote.api.BmpApiService
import com.posbah.app.data.remote.api.PosApiService
import com.posbah.app.security.SecurePreferences
import com.posbah.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
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
    val addressLine1: String? = null,
    val phoneNumber: String? = null,
    val emailAddress: String? = null,
    val taxNumber: String? = null,
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
    val description: String? = null,
    val unit: String = "pcs",
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
    val description: String? = null,
    val unit: String = "Kg",
    val price: Double = 0.0,
    val beratGram: Double = 0.0,
    val cycleTime: Double = 0.0,
    val cavity: Int = 1,
    val rejectRate: Double = 0.0,
    val uniqueID: String? = null,
    val slug: String? = null,
    val jenisBahanBaku: String = "",
    val image: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0,
    // v2.19.1: HPP fields untuk kalkulasi COGS di laporan keuangan
    val hppTotalPcs: Double = 0.0,
    val hppLusin: Double = 0.0
)

data class BmpCashflowData(
    val id: Long = 0,
    val tenantId: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val transactionType: String = "MASUK",
    val description: String = "",
    val amount: Double = 0.0,
    val costType: String = "OPERATING_EXPENSE",
    val paymentRefId: Long? = null,
    val payrollRefId: String? = null,
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
    val employeeType: String = "OPERATING_EXPENSE",
    val phone: String? = null,
    val email: String? = null,
    val isActive: Boolean = true,
    val updatedAt: Long = 0
)

data class BmpPayrollData(
    val id: String = "",
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
    val noTagihan: String = "",
    val tanggal: Long = System.currentTimeMillis(),
    val supplier: String? = null,
    val totalHarga: Double = 0.0,
    val nominal: Double = 0.0,
    val notes: String? = null,
    val notaFotoPath: String? = null,
    val notaFotoUrl: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0
)

data class BmpBahanBakuItemData(
    val id: Long = 0,
    val bahanBakuId: Long = 0,
    val jenisBahan: String = "",
    val kuantitas: Double = 0.0,
    val unit: String = "kg",
    val rate: Double = 0.0,
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
    id = (getCaseInsensitive("id") as? Number)?.toLong() ?: 0,
    tenantId = getCaseInsensitive("tenantId") as? String ?: "",
    clientName = getCaseInsensitive("clientName") as? String ?: "",
    saldoTitipan = (getCaseInsensitive("saldoTitipan") as? Number)?.toDouble() ?: 0.0,
    addressLine1 = getCaseInsensitive("addressLine1") as? String,
    phoneNumber = getCaseInsensitive("phoneNumber") as? String,
    emailAddress = getCaseInsensitive("emailAddress") as? String,
    taxNumber = getCaseInsensitive("taxNumber") as? String,
    uniqueID = getCaseInsensitive("uniqueID") as? String,
    slug = getCaseInsensitive("slug") as? String,
    isDeleted = getCaseInsensitive("isDeleted") as? Boolean ?: false,
    updatedAt = (getCaseInsensitive("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpInvoiceData() = BmpInvoiceData(
    id = (getCaseInsensitive("id") as? Number)?.toLong() ?: 0,
    tenantId = getCaseInsensitive("tenantId") as? String ?: "",
    clientId = (getCaseInsensitive("clientId") as? Number)?.toLong() ?: 0,
    number = getCaseInsensitive("number") as? String ?: "",
    status = getCaseInsensitive("status") as? String ?: "UNPAID",
    totalAmount = (getCaseInsensitive("totalAmount") as? Number)?.toDouble() ?: 0.0,
    paidAmount = (getCaseInsensitive("paidAmount") as? Number)?.toDouble() ?: 0.0,
    paymentTerms = getCaseInsensitive("paymentTerms") as? String ?: "14 hari",
    dueDate = (getCaseInsensitive("dueDate") as? Number)?.toLong(),
    notes = getCaseInsensitive("notes") as? String,
    createdAt = (getCaseInsensitive("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
    isDeleted = getCaseInsensitive("isDeleted") as? Boolean ?: false,
    updatedAt = (getCaseInsensitive("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpProductItemData() = BmpProductItemData(
    id = (getCaseInsensitive("id") as? Number)?.toLong() ?: 0,
    tenantId = getCaseInsensitive("tenantId") as? String ?: "",
    invoiceId = (getCaseInsensitive("invoiceId") as? Number)?.toLong() ?: 0,
    masterItemID = (getCaseInsensitive("masterItemID") as? Number)?.toLong(),
    name = getCaseInsensitive("title") as? String ?: getCaseInsensitive("name") as? String ?: "",
    description = getCaseInsensitive("description") as? String,
    unit = getCaseInsensitive("unit") as? String ?: "pcs",
    price = (getCaseInsensitive("price") as? Number)?.toDouble() ?: 0.0,
    quantity = (getCaseInsensitive("quantity") as? Number)?.toInt() ?: 1,
    jumlahLusin = (getCaseInsensitive("jumlahLusin") as? Number)?.toInt() ?: 1,
    hargaBeli = (getCaseInsensitive("hargaBeli") as? Number)?.toDouble() ?: 0.0,
    isKhusus = getCaseInsensitive("isKhusus") as? Boolean ?: false,
    isDeleted = getCaseInsensitive("isDeleted") as? Boolean ?: false,
    updatedAt = (getCaseInsensitive("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpMasterProductData() = BmpMasterProductData(
    id = (getCaseInsensitive("id") as? Number)?.toLong() ?: 0,
    tenantId = getCaseInsensitive("tenantId") as? String ?: "",
    title = getCaseInsensitive("title") as? String ?: "",
    description = getCaseInsensitive("description") as? String,
    unit = getCaseInsensitive("unit") as? String ?: "Kg",
    price = (getCaseInsensitive("price") as? Number)?.toDouble() ?: 0.0,
    beratGram = (getCaseInsensitive("beratGram") as? Number)?.toDouble() ?: 0.0,
    cycleTime = (getCaseInsensitive("cycleTime") as? Number)?.toDouble() ?: 0.0,
    cavity = (getCaseInsensitive("cavity") as? Number)?.toInt() ?: 1,
    rejectRate = (getCaseInsensitive("rejectRate") as? Number)?.toDouble() ?: 0.0,
    uniqueID = getCaseInsensitive("uniqueID") as? String,
    slug = getCaseInsensitive("slug") as? String,
    jenisBahanBaku = getCaseInsensitive("jenisBahanBaku") as? String ?: "",
    image = getCaseInsensitive("image") as? String,
    isDeleted = getCaseInsensitive("isDeleted") as? Boolean ?: false,
    updatedAt = (getCaseInsensitive("updatedAt") as? Number)?.toLong() ?: 0,
    hppTotalPcs = (getCaseInsensitive("hppTotalPcs") as? Number)?.toDouble() ?: 0.0,
    hppLusin = (getCaseInsensitive("hppLusin") as? Number)?.toDouble() ?: 0.0
)

fun Map<String, Any?>.toBmpCashflowData() = BmpCashflowData(
    id = (getCaseInsensitive("id") as? Number)?.toLong() ?: 0,
    tenantId = getCaseInsensitive("tenantId") as? String ?: "",
    transactionDate = (getCaseInsensitive("transactionDate") as? Number)?.toLong() ?: System.currentTimeMillis(),
    transactionType = getCaseInsensitive("transactionType") as? String ?: "MASUK",
    description = getCaseInsensitive("description") as? String ?: "",
    amount = (getCaseInsensitive("amount") as? Number)?.toDouble() ?: 0.0,
    costType = getCaseInsensitive("costType") as? String ?: "OPERATING_EXPENSE",
    paymentRefId = (getCaseInsensitive("paymentRefId") as? Number)?.toLong(),
    payrollRefId = getCaseInsensitive("payrollRefId") as? String,
    isDeleted = getCaseInsensitive("isDeleted") as? Boolean ?: false,
    updatedAt = (getCaseInsensitive("updatedAt") as? Number)?.toLong() ?: 0
)

fun Map<String, Any?>.toBmpPaymentData() = BmpPaymentData(
    id = (getCaseInsensitive("id") as? Number)?.toLong() ?: 0,
    tenantId = getCaseInsensitive("tenantId") as? String ?: "",
    invoiceId = (getCaseInsensitive("invoiceId") as? Number)?.toLong() ?: 0,
    paymentDate = (getCaseInsensitive("paymentDate") as? Number)?.toLong() ?: System.currentTimeMillis(),
    paymentAmount = (getCaseInsensitive("paymentAmount") as? Number)?.toDouble() ?: 0.0,
    paymentMethod = getCaseInsensitive("paymentMethod") as? String ?: "CASH",
    notes = getCaseInsensitive("notes") as? String,
    isDeleted = getCaseInsensitive("isDeleted") as? Boolean ?: false,
    updatedAt = (getCaseInsensitive("updatedAt") as? Number)?.toLong() ?: 0
)

private fun Map<String, Any?>.getCaseInsensitive(key: String): Any? {
    val exact = get(key)
    if (exact != null) return exact
    val lowercaseKey = key.lowercase()
    for ((k, v) in this) {
        if (k.lowercase() == lowercaseKey) return v
    }
    return null
}

fun Map<String, Any?>.toBmpBahanBakuData() = BmpBahanBakuData(
    id = (getCaseInsensitive("id") as? Number)?.toLong() ?: 0,
    tenantId = getCaseInsensitive("tenantId") as? String ?: "",
    noTagihan = getCaseInsensitive("noTagihan") as? String ?: "",
    tanggal = (getCaseInsensitive("tanggal") as? Number)?.toLong() ?: System.currentTimeMillis(),
    supplier = getCaseInsensitive("supplier") as? String,
    totalHarga = (getCaseInsensitive("totalHarga") as? Number)?.toDouble() ?: 0.0,
    nominal = (getCaseInsensitive("nominal") as? Number)?.toDouble() ?: 0.0,
    notes = getCaseInsensitive("notes") as? String,
    notaFotoPath = getCaseInsensitive("notaFotoPath") as? String,
    notaFotoUrl = getCaseInsensitive("notaFotoUrl") as? String,
    isDeleted = getCaseInsensitive("isDeleted") as? Boolean ?: false,
    updatedAt = (getCaseInsensitive("updatedAt") as? Number)?.toLong() ?: 0
)

// ── BmpClientRepository ───────────────────────────────────────────────────────

@Singleton
class BmpClientRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    private val _clients = MutableStateFlow<List<BmpClientData>>(emptyList())
    val clients = _clients.asStateFlow()

    val allClients: kotlinx.coroutines.flow.Flow<List<BmpClientEntity>>
        get() = observe("")

    private fun BmpClientData.toEntity() = BmpClientEntity(
        id = id,
        tenantId = tenantId,
        clientName = clientName,
        addressLine1 = addressLine1,
        phoneNumber = phoneNumber,
        emailAddress = emailAddress,
        taxNumber = taxNumber,
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
        addressLine1 = addressLine1,
        phoneNumber = phoneNumber,
        emailAddress = emailAddress,
        taxNumber = taxNumber,
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
        _clients.map { list -> list.map { it.toEntity() } }

    fun search(tenantId: String, query: String): kotlinx.coroutines.flow.Flow<List<BmpClientEntity>> =
        _clients.map { list ->
            list.map { it.toEntity() }.filter { it.clientName.contains(query, ignoreCase = true) }
        }

    fun count(tenantId: String): kotlinx.coroutines.flow.Flow<Int> =
        _clients.map { it.size }

    suspend fun getById(id: Long): BmpClientEntity? =
        _clients.value.find { it.id == id }?.toEntity() ?: list().find { it.id == id }?.toEntity()

    suspend fun upsert(client: BmpClientData): OnlineWriteResult {
        val snapshot = _clients.value
        // Optimistic update: perbarui state lokal sebelum request jaringan
        if (client.id == 0L) {
            _clients.value = snapshot + client.copy(id = -System.currentTimeMillis())
        } else {
            _clients.value = snapshot.map { if (it.id == client.id) client else it }
        }
        return try {
            val uniqueId = client.uniqueID ?: java.util.UUID.randomUUID().toString()
            val body = mapOf<String, Any?>(
                "clientName" to client.clientName,
                "addressLine1" to client.addressLine1,
                "phoneNumber" to client.phoneNumber,
                "emailAddress" to client.emailAddress,
                "taxNumber" to client.taxNumber,
                "uniqueID" to uniqueId,
                "slug" to (client.slug ?: client.clientName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'))
            )
            val resp = if (client.id == 0L) {
                api.createClient(body)
            } else {
                api.updateClient(client.id, body)
            }
            // Refresh di background setelah berhasil untuk sinkronisasi ID sebenarnya
            refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _clients.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal simpan klien")
        }
    }

    suspend fun upsert(context: android.content.Context, entity: BmpClientEntity): OnlineWriteResult {
        return upsert(entity.toData())
    }

    suspend fun delete(id: Long, cashflowRepo: BmpCashFlowRepository): OnlineWriteResult {
        val snapshot = _clients.value
        _clients.value = snapshot.filter { it.id != id }  // optimistic: hapus dulu
        return try {
            api.deleteClient(id)
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _clients.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal hapus klien")
        }
    }

    suspend fun delete(context: android.content.Context, tenantId: String, id: Long): OnlineWriteResult {
        val snapshot = _clients.value
        _clients.value = snapshot.filter { it.id != id }  // optimistic: hapus dulu
        return try {
            api.deleteClient(id)
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _clients.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal hapus klien")
        }
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

    private val _payments = MutableStateFlow<List<BmpPaymentData>>(emptyList())
    val allPayments = _payments.asStateFlow()

    suspend fun refreshPayments() {
        try {
            val resp = api.getPayments()
            if (resp.isSuccessful) {
                _payments.value = resp.body()?.map { it.toBmpPaymentData() } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    val allInvoices: kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.BmpInvoiceEntity>>
        get() = observe("")

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
        _invoices.map { list -> list.map { it.toEntity() } }

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
        val snapshot = _invoices.value
        val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
        val days = parsePaymentTermsDays(invoice.paymentTerms)
        val computedDueDate = invoice.dueDate ?: (invoice.createdAt + days * 86400_000L)
        val newStatus = computeInvoiceStatus(total, invoice.paidAmount)

        // Optimistic update
        val tempId = -System.currentTimeMillis()
        val tempInvoice = invoice.copy(
            id = tempId,
            totalAmount = total,
            status = newStatus,
            dueDate = computedDueDate
        )
        _invoices.value = snapshot + tempInvoice

        return try {
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
                _invoices.value = snapshot // rollback
                return Pair(-1L, OnlineWriteResult.Error("Gagal simpan invoice ke server"))
            }
            val newId = (invoiceResp.body()?.get("id") as? Number)?.toLong() ?: -1L

            // 2. POST products (invoice line items)
            for (prod in products) {
                api.createBmpProduct(mapOf(
                    "invoiceId" to newId,
                    "masterItemID" to prod.masterItemID,
                    "title" to prod.name,   // kolom DB adalah 'title', bukan 'name'
                    "description" to prod.description,
                    "unit" to prod.unit,
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
                        change = -(prod.quantity * prod.jumlahLusin).toDouble(),
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

            // Replace temp invoice dengan saved invoice
            val savedInvoice = tempInvoice.copy(id = newId)
            _invoices.value = snapshot + savedInvoice

            Pair(newId, OnlineWriteResult.Success)
        } catch (e: Exception) {
            _invoices.value = snapshot // rollback
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
        val snapshot = _invoices.value
        val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
        val days = parsePaymentTermsDays(invoice.paymentTerms)
        val computedDueDate = invoice.dueDate ?: (invoice.createdAt + days * 86400_000L)
        val allPayments = getPaymentsByInvoice(invoice.id)
        val totalPaidAmt = allPayments.sumOf { it.paymentAmount }
        val newStatus = computeInvoiceStatus(total, totalPaidAmt, computedDueDate)

        // Optimistic update
        val updatedInvoice = invoice.copy(
            totalAmount = total,
            status = newStatus,
            dueDate = computedDueDate,
            paidAmount = totalPaidAmt
        )
        _invoices.value = snapshot.map { if (it.id == invoice.id) updatedInvoice else it }

        return try {
            // 1. Hapus produk lama di VPS
            val oldProducts = getProductsByInvoice(invoice.id)
            for (op in oldProducts) {
                try { api.deleteBmpProduct(op.id) } catch (_: Exception) {}
                // Kembalikan stok produk lama
                if (op.masterItemID != null) {
                    stockRepo.adjustStock(
                        masterItemId = op.masterItemID,
                        change = (op.quantity * op.jumlahLusin).toDouble(),
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
                    "title" to prod.name,   // kolom DB adalah 'title', bukan 'name'
                    "description" to prod.description,
                    "unit" to prod.unit,
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
                        change = -(prod.quantity * prod.jumlahLusin).toDouble(),
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
        } catch (e: Exception) {
            _invoices.value = snapshot // rollback
            OnlineWriteResult.Error(e.message ?: "Gagal update invoice")
        }
    }

    suspend fun deleteInvoice(
        id: Long,
        cashflowRepo: BmpCashFlowRepository,
        stockRepo: BmpStockRepository
    ): OnlineWriteResult {
        val snapshot = _invoices.value
        _invoices.value = snapshot.filter { it.id != id }  // optimistic: hapus dari list dulu
        return try {
            val inv = snapshot.find { it.id == id }
            // Kembalikan stok (di background, tidak memblokir UI)
            val products = getProductsByInvoice(id)
            for (prod in products) {
                if (prod.masterItemID != null) {
                    stockRepo.adjustStock(
                        masterItemId = prod.masterItemID,
                        change = (prod.quantity * prod.jumlahLusin).toDouble(),
                        mutationType = "PENJUALAN",
                        referenceId = id,
                        notes = "Hapus Invoice #${inv?.number ?: id}"
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
        } catch (e: Exception) {
            _invoices.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal hapus invoice")
        }
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
        val snapshot = _payments.value
        val invSnapshot = _invoices.value

        val tempPayment = BmpPaymentData(
            id = -System.currentTimeMillis(),
            tenantId = "",
            invoiceId = invoiceId,
            paymentDate = System.currentTimeMillis(),
            paymentAmount = amount,
            paymentMethod = method,
            notes = notes
        )
        _payments.value = snapshot + tempPayment

        val targetInv = invSnapshot.find { it.id == invoiceId }
        if (targetInv != null) {
            val newPaid = targetInv.paidAmount + amount
            val newStatus = computeInvoiceStatus(targetInv.totalAmount, newPaid, targetInv.dueDate)
            _invoices.value = invSnapshot.map {
                if (it.id == invoiceId) it.copy(paidAmount = newPaid, status = newStatus) else it
            }
        }

        return try {
            val payResp = api.createPayment(mapOf(
                "invoiceId" to invoiceId,
                "paymentDate" to tempPayment.paymentDate,
                "paymentAmount" to amount,
                "paymentMethod" to method,
                "notes" to notes
            ))
            val payId = (payResp.body()?.get("id") as? Number)?.toLong()

            if (targetInv != null) {
                val invoicePayments = getPaymentsByInvoice(invoiceId)
                val totalPaid = invoicePayments.sumOf { it.paymentAmount } + amount
                val newStatus = computeInvoiceStatus(targetInv.totalAmount, totalPaid, targetInv.dueDate)
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
            cashflowRepo.refreshEntries()

            refreshPayments()
            refresh()

            OnlineWriteResult.Success
        } catch (e: Exception) {
            _payments.value = snapshot
            _invoices.value = invSnapshot
            OnlineWriteResult.Error(e.message ?: "Gagal catat pembayaran")
        }
    }

    suspend fun editPayment(
        context: android.content.Context,
        tenantId: String,
        paymentId: Long,
        amount: Double,
        method: String,
        notes: String?
    ): OnlineWriteResult {
        val snapshot = _payments.value
        val invSnapshot = _invoices.value

        val pay = snapshot.find { it.id == paymentId }
        if (pay != null) {
            _payments.value = snapshot.map {
                if (it.id == paymentId) it.copy(paymentAmount = amount, paymentMethod = method, notes = notes) else it
            }
            val invId = pay.invoiceId
            val inv = invSnapshot.find { it.id == invId }
            if (inv != null) {
                val invoicePayments = snapshot.filter { it.invoiceId == invId && it.id != paymentId }
                val totalPaid = invoicePayments.sumOf { it.paymentAmount } + amount
                val newStatus = computeInvoiceStatus(inv.totalAmount, totalPaid, inv.dueDate)
                _invoices.value = invSnapshot.map {
                    if (it.id == invId) it.copy(paidAmount = totalPaid, status = newStatus) else it
                }
            }
        }

        return try {
            api.updatePayment(paymentId, mapOf(
                "paymentAmount" to amount,
                "paymentMethod" to method,
                "notes" to notes
            ))
            if (pay != null) {
                val invId = pay.invoiceId
                val inv = getById(invId)
                if (inv != null) {
                    val allPayments = api.getPayments().body()?.map { it.toBmpPaymentData() } ?: emptyList()
                    val invoicePayments = allPayments.filter { it.invoiceId == invId && it.id != paymentId }
                    val totalPaid = invoicePayments.sumOf { it.paymentAmount } + amount
                    val newStatus = computeInvoiceStatus(inv.totalAmount, totalPaid, inv.dueDate)
                    api.updateInvoice(invId, mapOf(
                        "status" to newStatus,
                        "paidAmount" to totalPaid
                    ))
                }
            }
            refreshPayments()
            refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _payments.value = snapshot
            _invoices.value = invSnapshot
            OnlineWriteResult.Error(e.message ?: "Gagal ubah pembayaran")
        }
    }

    suspend fun deletePayment(
        context: android.content.Context,
        tenantId: String,
        paymentId: Long
    ): OnlineWriteResult {
        val snapshot = _payments.value
        val invSnapshot = _invoices.value

        val pay = snapshot.find { it.id == paymentId }
        if (pay != null) {
            _payments.value = snapshot.filter { it.id != paymentId }
            val invId = pay.invoiceId
            val inv = invSnapshot.find { it.id == invId }
            if (inv != null) {
                val invoicePayments = snapshot.filter { it.invoiceId == invId && it.id != paymentId }
                val totalPaid = invoicePayments.sumOf { it.paymentAmount }
                val newStatus = computeInvoiceStatus(inv.totalAmount, totalPaid, inv.dueDate)
                _invoices.value = invSnapshot.map {
                    if (it.id == invId) it.copy(paidAmount = totalPaid, status = newStatus) else it
                }
            }
        }

        return try {
            api.deletePayment(paymentId)
            if (pay != null) {
                val invId = pay.invoiceId
                val inv = getById(invId)
                if (inv != null) {
                    val allPayments = api.getPayments().body()?.map { it.toBmpPaymentData() } ?: emptyList()
                    val invoicePayments = allPayments.filter { it.invoiceId == invId && it.id != paymentId }
                    val totalPaid = invoicePayments.sumOf { it.paymentAmount }
                    val newStatus = computeInvoiceStatus(inv.totalAmount, totalPaid, inv.dueDate)
                    api.updateInvoice(invId, mapOf(
                        "status" to newStatus,
                        "paidAmount" to totalPaid
                    ))
                }
            }
            refreshPayments()
            refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _payments.value = snapshot
            _invoices.value = invSnapshot
            OnlineWriteResult.Error(e.message ?: "Gagal hapus pembayaran")
        }
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
                    description = it.description,
                    unit = it.unit,
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
                description = it.description,
                unit = it.unit,
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
                description = it.description,
                unit = it.unit,
                price = it.price,
                quantity = it.quantity.toInt(),
                jumlahLusin = it.jumlahLusin.toInt(),
                hargaBeli = it.hargaBeli,
                isKhusus = it.isKhusus
            )
        }
        return updateInvoice(invoiceData, productDataList, cashflowRepo, stockRepo)
    }

    private fun BmpPaymentData.toEntity() = BmpInvoicePaymentEntity(
        id = id,
        tenantId = tenantId,
        invoiceId = invoiceId,
        paymentDate = paymentDate,
        paymentAmount = paymentAmount,
        paymentMethod = paymentMethod,
        notes = notes,
        isSynced = true,
        isDeleted = isDeleted
    )

    fun observePayments(invoiceId: Long): kotlinx.coroutines.flow.Flow<List<BmpInvoicePaymentEntity>> =
        _payments.map { list ->
            list.filter { it.invoiceId == invoiceId }.map { it.toEntity() }
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

    suspend fun list(): List<BmpMasterProductData> {
        val cached = _items.value
        if (cached.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try { refresh() } catch (_: Exception) {}
            }
            return cached
        }
        refresh()
        return _items.value
    }

    suspend fun getById(id: Long): BmpMasterProductData? = list().find { it.id == id }

    suspend fun upsert(item: BmpMasterProductData): OnlineWriteResult {
        val snapshot = _items.value
        // Optimistic update: perbarui state lokal sebelum request jaringan
        if (item.id == 0L) {
            _items.value = snapshot + item.copy(id = -System.currentTimeMillis())
        } else {
            _items.value = snapshot.map { if (it.id == item.id) item else it }
        }
        return try {
            val body = mutableMapOf<String, Any>().apply {
                put("title", item.title)
                put("unit", item.unit)
                put("price", item.price)
                put("beratGram", item.beratGram)
                put("cycleTime", item.cycleTime)
                put("cavity", item.cavity)
                put("rejectRate", item.rejectRate)
                put("jenisBahanBaku", item.jenisBahanBaku)
                // v2.19.1: Kirim hppTotalPcs dan hppLusin agar COGS di laporan keuangan terisi.
                // Nilai ini dihitung di MasterProductsScreen saat user klik Simpan.
                // Tanpa ini, kolom DB tetap 0.0 meski sudah ada migrasi.
                put("hppTotalPcs", item.hppTotalPcs)
                put("hppLusin", item.hppLusin)
                if (item.description != null) put("description", item.description)
                if (item.uniqueID != null) put("uniqueID", item.uniqueID)
                if (item.slug != null) put("slug", item.slug)
                if (item.image != null) put("image", item.image)
            }
            if (item.id == 0L) api.createMasterProduct(body)
            else api.updateMasterProduct(item.id, body)
            // Refresh di background untuk sinkronisasi ID asli dari server
            refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _items.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal simpan master produk")
        }
    }

    suspend fun delete(id: Long): OnlineWriteResult {
        val snapshot = _items.value
        _items.value = snapshot.filter { it.id != id }  // optimistic: hapus dulu
        return try {
            api.deleteMasterProduct(id)
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _items.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal hapus")
        }
    }

    fun observe(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpMasterProductEntity>> =
        _items.map { list ->
            list.map {
                com.posbah.app.data.local.entities.BmpMasterProductEntity(
                    id = it.id,
                    tenantId = it.tenantId,
                    title = it.title,
                    description = it.description,
                    unit = it.unit,
                    price = it.price,
                    beratGram = it.beratGram,
                    cycleTime = it.cycleTime,
                    cavity = it.cavity,
                    rejectRate = it.rejectRate,
                    uniqueID = it.uniqueID,
                    slug = it.slug,
                    isDeleted = it.isDeleted,
                    jenisBahanBaku = it.jenisBahanBaku,
                    image = it.image,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = it.updatedAt,
                    isSynced = true,
                    hppTotalPcs = it.hppTotalPcs,
                    hppLusin = it.hppLusin
                )
            }
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

    suspend fun refreshEntries() = refresh()

    suspend fun fetchCashFlowFromNetwork() = refresh()

    suspend fun list(): List<BmpCashflowData> = try {
        api.getCashflow().body()?.map { it.toBmpCashflowData() } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    suspend fun createEntry(entry: BmpCashflowData): Long {
        val snapshot = _entries.value
        val tempId = -System.currentTimeMillis()
        val tempEntry = entry.copy(id = tempId, tenantId = securePrefs.currentTenantId ?: "")
        _entries.value = snapshot + tempEntry
        return try {
            val resp = api.createCashflow(mapOf(
                "transactionDate" to entry.transactionDate,
                "transactionType" to entry.transactionType,
                "description" to entry.description,
                "amount" to entry.amount,
                "costType" to entry.costType,
                "paymentRefId" to entry.paymentRefId,
                "payrollRefId" to entry.payrollRefId
            ))
            val newId = (resp.body()?.get("id") as? Number)?.toLong() ?: 0L
            if (newId > 0) {
                _entries.value = _entries.value.map { if (it.id == tempId) it.copy(id = newId) else it }
                newId
            } else {
                _entries.value = snapshot
                0L
            }
        } catch (_: Exception) {
            _entries.value = snapshot
            0L
        }
    }

    suspend fun update(entry: BmpCashflowData): OnlineWriteResult {
        val snapshot = _entries.value
        _entries.value = snapshot.map { if (it.id == entry.id) entry else it }  // optimistic update
        return try {
            api.updateCashflow(entry.id, mapOf(
                "transactionDate" to entry.transactionDate,
                "transactionType" to entry.transactionType,
                "description" to entry.description,
                "amount" to entry.amount,
                "costType" to entry.costType
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _entries.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal update cashflow")
        }
    }

    suspend fun delete(id: Long): OnlineWriteResult {
        val snapshot = _entries.value
        _entries.value = snapshot.filter { it.id != id }  // optimistic: hapus dulu
        return try {
            api.deleteCashflow(id)
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _entries.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal hapus cashflow")
        }
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
                    costType = d.costType,
                    transactionDate = d.transactionDate,
                    paymentRefId = d.paymentRefId,
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
            costType = entity.costType,
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
                        employeeType = it["employeeType"] as? String ?: "OPERATING_EXPENSE",
                        phone = it["phone"] as? String,
                        email = it["email"] as? String,
                        isActive = it["isActive"] as? Boolean ?: true,
                        updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: 0
                    )
                } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpEmployeeData> {
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

    suspend fun upsert(emp: BmpEmployeeData): OnlineWriteResult {
        val snapshot = _employees.value
        // Optimistic update: perbarui state lokal sebelum request jaringan
        if (emp.id == 0L) {
            _employees.value = snapshot + emp.copy(id = -System.currentTimeMillis())
        } else {
            _employees.value = snapshot.map { if (it.id == emp.id) emp else it }
        }
        return try {
            val body = mapOf<String, Any?>(
                "name" to emp.name, "role" to emp.role, "salary" to emp.salary,
                "employeeType" to emp.employeeType,
                "phone" to emp.phone, "email" to emp.email, "isActive" to emp.isActive
            )
            if (emp.id == 0L) api.createBmpEmployee(body) else api.updateBmpEmployee(emp.id, body)
            refresh()  // sinkronisasi ID asli dari server
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _employees.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal simpan karyawan")
        }
    }

    suspend fun delete(id: Long): OnlineWriteResult {
        val snapshot = _employees.value
        _employees.value = snapshot.filter { it.id != id }  // optimistic: hapus dulu
        return try {
            api.deleteBmpEmployee(id)
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _employees.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal hapus karyawan")
        }
    }

    fun observe(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpEmployeeEntity>> =
        _employees.map { list ->
            list.map {
                com.posbah.app.data.local.entities.BmpEmployeeEntity(
                    id = it.id,
                    tenantId = it.tenantId,
                    name = it.name,
                    position = it.role,
                    salaryAmount = it.salary,
                    employeeType = it.employeeType,
                    isActive = it.isActive,
                    fingerprintPIN = null,
                    employeeId = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = it.updatedAt,
                    isSynced = true
                )
            }
        }

    fun observePayrolls(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpPayrollEntity>> =
        payrollRepo.payrolls.map { list ->
            list.map {
                com.posbah.app.data.local.entities.BmpPayrollEntity(
                    id = it.id,
                    tenantId = it.tenantId,
                    employeeId = it.employeeId,
                    paymentDate = it.paymentDate,
                    amount = it.amount,
                    attendanceCount = 0,
                    dailyRate = 0.0,
                    description = it.notes ?: ""
                )
            }
        }

    suspend fun upsert(e: com.posbah.app.data.local.entities.BmpEmployeeEntity): Long {
        val data = BmpEmployeeData(
            id = e.id,
            tenantId = e.tenantId,
            name = e.name,
            role = e.position ?: "KARYAWAN",
            salary = e.salaryAmount,
            employeeType = e.employeeType,
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
        val empName = _employees.value.find { it.id == payroll.employeeId }?.name.orEmpty()
        val data = BmpPayrollData(
            id = payroll.id,
            tenantId = payroll.tenantId,
            employeeId = payroll.employeeId,
            employeeName = empName,
            paymentDate = payroll.paymentDate,
            amount = payroll.amount,
            notes = payroll.description,
            updatedAt = System.currentTimeMillis()
        )
        payrollRepo.createPayroll(data)
        return payroll.employeeId
    }

    suspend fun refreshPayrolls() {
        payrollRepo.refresh()
    }

    suspend fun updatePayroll(payroll: com.posbah.app.data.local.entities.BmpPayrollEntity): OnlineWriteResult {
        val empName = _employees.value.find { it.id == payroll.employeeId }?.name.orEmpty()
        val data = BmpPayrollData(
            id = payroll.id,
            tenantId = payroll.tenantId,
            employeeId = payroll.employeeId,
            employeeName = empName,
            paymentDate = payroll.paymentDate,
            amount = payroll.amount,
            notes = payroll.description,
            updatedAt = System.currentTimeMillis()
        )
        return payrollRepo.updatePayroll(data)
    }

    suspend fun deletePayroll(id: String): OnlineWriteResult {
        return payrollRepo.deletePayroll(id)
    }
}

// ── BmpPayrollRepository ──────────────────────────────────────────────────────

@Singleton
class BmpPayrollRepository @Inject constructor(
    private val api: BmpApiService,
    private val cashflowRepo: BmpCashFlowRepository,
    private val securePrefs: SecurePreferences
) {
    private val _payrolls = MutableStateFlow<List<BmpPayrollData>>(emptyList())
    val payrolls = _payrolls.asStateFlow()

    suspend fun refresh() {
        try {
            val resp = api.getPayrolls()
            if (resp.isSuccessful) {
                _payrolls.value = resp.body()?.map {
                    BmpPayrollData(
                        id = it["id"] as? String ?: "",
                        tenantId = it["tenantId"] as? String ?: "",
                        employeeId = (it["employeeId"] as? Number)?.toLong() ?: 0,
                        employeeName = it["employeeName"] as? String ?: "",
                        paymentDate = (it["paymentDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                        notes = it["notes"] as? String,
                        updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: 0
                    )
                } ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    suspend fun list(): List<BmpPayrollData> = _payrolls.value.ifEmpty {
        try {
            api.getPayrolls().body()?.map {
                BmpPayrollData(
                    id = it["id"] as? String ?: "",
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
    }

    /**
     * Bayar gaji → POST payroll + business logic: cashflow KELUAR otomatis.
     */
    suspend fun createPayroll(payroll: BmpPayrollData): OnlineWriteResult {
        val snapshot = _payrolls.value
        _payrolls.value = snapshot + payroll.copy(id = "temp-${System.currentTimeMillis()}") // optimistic
        return try {
            api.createPayroll(mapOf(
                "id" to payroll.id,
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
                transactionDate = payroll.paymentDate,
                payrollRefId = payroll.id, // Link to the payroll record UUID string
                costType = "DIRECT_LABOR"  // Set cost type to DIRECT_LABOR
            ))
            refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _payrolls.value = snapshot // rollback
            OnlineWriteResult.Error(e.message ?: "Gagal bayar gaji")
        }
    }

    suspend fun updatePayroll(payroll: BmpPayrollData): OnlineWriteResult {
        val snapshot = _payrolls.value
        _payrolls.value = snapshot.map { if (it.id == payroll.id) payroll else it }
        return try {
            api.updatePayroll(payroll.id, mapOf(
                "employeeId" to payroll.employeeId,
                "employeeName" to payroll.employeeName,
                "paymentDate" to payroll.paymentDate,
                "amount" to payroll.amount,
                "notes" to payroll.notes
            ))
            refresh()
            cashflowRepo.refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _payrolls.value = snapshot // rollback
            OnlineWriteResult.Error(e.message ?: "Gagal ubah gaji")
        }
    }

    suspend fun deletePayroll(id: String): OnlineWriteResult {
        val snapshot = _payrolls.value
        _payrolls.value = snapshot.filter { it.id != id }
        return try {
            api.deletePayroll(id)
            refresh()
            cashflowRepo.refresh()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _payrolls.value = snapshot // rollback
            OnlineWriteResult.Error(e.message ?: "Gagal hapus gaji")
        }
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

    private val _suppliers = MutableStateFlow<List<String>>(emptyList())
    val suppliers = _suppliers.asStateFlow()

    suspend fun addUsage(materialId: Long, quantity: Double, reason: String) {
        addUsage(materialId = materialId, quantity = quantity, reason = reason, refId = 0L)
    }

    suspend fun addUsage(
        materialId: Long,
        quantity: Double,
        reason: String,
        refId: Long
    ) {
        try {
            android.util.Log.d("BmpBahanBakuRepo", "addUsage: materialId=$materialId, quantity=$quantity, reason=$reason, refId=$refId")
        } catch (_: Exception) {}
    }

    suspend fun refreshSuppliers() {
        try {
            val resp = api.getSuppliers()
            if (resp.isSuccessful) {
                _suppliers.value = resp.body() ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refresh() {
        try {
            val resp = api.getBahanBaku()
            if (resp.isSuccessful) {
                _bahanBaku.value = resp.body()?.map { it.toBmpBahanBakuData() } ?: emptyList()
                refreshSuppliers()
            } else {
                android.util.Log.e("BmpBahanBakuRepo", "refresh failed: ${resp.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("BmpBahanBakuRepo", "refresh error", e)
        }
    }

    suspend fun list(): List<BmpBahanBakuData> = try {
        api.getBahanBaku().body()?.map { it.toBmpBahanBakuData() } ?: emptyList()
    } catch (e: Exception) {
        android.util.Log.e("BmpBahanBakuRepo", "list error", e)
        emptyList()
    }

    /**
     * Simpan bahan baku + items.
     * Business logic TETAP: input bahan baku → OTOMATIS potong cashflow (KELUAR).
     */
     suspend fun create(
        bahanBaku: BmpBahanBakuData,
        items: List<BmpBahanBakuItemData>
     ): OnlineWriteResult = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
          val snapshot = _bahanBaku.value
          val tempId = -System.currentTimeMillis()
          val tempBahanBaku = bahanBaku.copy(id = tempId)
          _bahanBaku.value = snapshot + tempBahanBaku

          try {
              val resp = api.createBahanBaku(mapOf(
                  "noTagihan" to bahanBaku.noTagihan,
                  "tanggal" to bahanBaku.tanggal,
                  "supplier" to bahanBaku.supplier,
                  "totalHarga" to bahanBaku.totalHarga,
                  "nominal" to bahanBaku.nominal,
                  "notaFotoPath" to bahanBaku.notaFotoPath,
                  "notaFotoUrl" to bahanBaku.notaFotoUrl,
                  "notes" to bahanBaku.notes
              ))
              val newId = (resp.body()?.get("id") as? Number)?.toLong() ?: 0L

              // POST items
              val itemBodies = items.map {
                  mapOf<String, Any?>(
                      "bahanBakuId" to newId,
                      "jenisBahan" to it.jenisBahan,
                      "kuantitas" to it.kuantitas,
                      "unit" to it.unit,
                      "rate" to it.rate
                  )
              }
              api.createBahanBakuItems(itemBodies)

              // ── Business logic: input bahan baku → potong cashflow ──────────
              cashflowRepo.createEntry(BmpCashflowData(
                  transactionType = "KELUAR",
                  description = "Pembelian Bahan Baku: ${bahanBaku.noTagihan}",
                  amount = bahanBaku.nominal,
                  transactionDate = bahanBaku.tanggal
              ))

              val savedBahanBaku = tempBahanBaku.copy(id = newId)
              _bahanBaku.value = snapshot + savedBahanBaku

              OnlineWriteResult.Success
          } catch (e: Exception) {
              _bahanBaku.value = snapshot // rollback
              OnlineWriteResult.Error(e.message ?: "Gagal simpan bahan baku")
          }
     }

     suspend fun update(bahanBaku: BmpBahanBakuData): OnlineWriteResult = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
          val snapshot = _bahanBaku.value
          _bahanBaku.value = snapshot.map { if (it.id == bahanBaku.id) bahanBaku else it }

          try {
              val oldBahanBaku = snapshot.find { it.id == bahanBaku.id }
              val oldNoTagihan = oldBahanBaku?.noTagihan ?: bahanBaku.noTagihan

              api.updateBahanBaku(bahanBaku.id, mapOf(
                  "noTagihan" to bahanBaku.noTagihan,
                  "tanggal" to bahanBaku.tanggal,
                  "supplier" to bahanBaku.supplier,
                  "totalHarga" to bahanBaku.totalHarga,
                  "nominal" to bahanBaku.nominal,
                  "notaFotoPath" to bahanBaku.notaFotoPath,
                  "notaFotoUrl" to bahanBaku.notaFotoUrl,
                  "notes" to bahanBaku.notes
              ))

              // ── Business logic: update cashflow entry ──────────────────────
              val cfList = cashflowRepo.list()
              val match = cfList.find { it.description == "Pembelian Bahan Baku: $oldNoTagihan" }
              if (match != null) {
                  cashflowRepo.update(match.copy(
                      description = "Pembelian Bahan Baku: ${bahanBaku.noTagihan}",
                      amount = bahanBaku.nominal,
                      transactionDate = bahanBaku.tanggal
                  ))
              } else {
                  cashflowRepo.createEntry(BmpCashflowData(
                      transactionType = "KELUAR",
                      description = "Pembelian Bahan Baku: ${bahanBaku.noTagihan}",
                      amount = bahanBaku.nominal,
                      transactionDate = bahanBaku.tanggal
                  ))
              }

              // Also check and update any "Pembayaran Hutang Bahan Baku: $oldNoTagihan" cashflows!
              if (oldNoTagihan != bahanBaku.noTagihan) {
                  val debtMatches = cfList.filter { it.description == "Pembayaran Hutang Bahan Baku: $oldNoTagihan" }
                  debtMatches.forEach { debtCf ->
                      cashflowRepo.update(debtCf.copy(
                          description = "Pembayaran Hutang Bahan Baku: ${bahanBaku.noTagihan}"
                      ))
                  }
              }

              cashflowRepo.refreshEntries()

              OnlineWriteResult.Success
          } catch (e: Exception) {
              _bahanBaku.value = snapshot // rollback
              OnlineWriteResult.Error(e.message ?: "Gagal update bahan baku")
          }
     }

     suspend fun update(
         bahanBaku: BmpBahanBakuData,
         items: List<BmpBahanBakuItemData>
     ): OnlineWriteResult = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
          val snapshot = _bahanBaku.value
          _bahanBaku.value = snapshot.map { if (it.id == bahanBaku.id) bahanBaku else it }

          try {
              val oldBahanBaku = snapshot.find { it.id == bahanBaku.id }
              val oldNoTagihan = oldBahanBaku?.noTagihan ?: bahanBaku.noTagihan

              // 1. Update header
              api.updateBahanBaku(bahanBaku.id, mapOf(
                  "noTagihan" to bahanBaku.noTagihan,
                  "tanggal" to bahanBaku.tanggal,
                  "supplier" to bahanBaku.supplier,
                  "totalHarga" to bahanBaku.totalHarga,
                  "nominal" to bahanBaku.nominal,
                  "notaFotoPath" to bahanBaku.notaFotoPath,
                  "notaFotoUrl" to bahanBaku.notaFotoUrl,
                  "notes" to bahanBaku.notes
              ))

              // 2. Delete old items
              api.deleteBahanBakuItems(bahanBaku.id)

              // 3. Post new items
              if (items.isNotEmpty()) {
                  val itemBodies = items.map {
                      mapOf<String, Any?>(
                          "bahanBakuId" to bahanBaku.id,
                          "jenisBahan" to it.jenisBahan,
                          "kuantitas" to it.kuantitas,
                          "unit" to it.unit,
                          "rate" to it.rate
                      )
                  }
                  api.createBahanBakuItems(itemBodies)
              }

              // 4. Update cashflow entry
              val cfList = cashflowRepo.list()
              val match = cfList.find { it.description == "Pembelian Bahan Baku: $oldNoTagihan" }
              if (match != null) {
                  cashflowRepo.update(match.copy(
                      description = "Pembelian Bahan Baku: ${bahanBaku.noTagihan}",
                      amount = bahanBaku.nominal,
                      transactionDate = bahanBaku.tanggal
                  ))
              } else {
                  cashflowRepo.createEntry(BmpCashflowData(
                      transactionType = "KELUAR",
                      description = "Pembelian Bahan Baku: ${bahanBaku.noTagihan}",
                      amount = bahanBaku.nominal,
                      transactionDate = bahanBaku.tanggal
                  ))
              }

              // Also check and update any "Pembayaran Hutang Bahan Baku: $oldNoTagihan" cashflows!
              if (oldNoTagihan != bahanBaku.noTagihan) {
                  val debtMatches = cfList.filter { it.description == "Pembayaran Hutang Bahan Baku: $oldNoTagihan" }
                  debtMatches.forEach { debtCf ->
                      cashflowRepo.update(debtCf.copy(
                          description = "Pembayaran Hutang Bahan Baku: ${bahanBaku.noTagihan}"
                      ))
                  }
              }

              cashflowRepo.refreshEntries()

              OnlineWriteResult.Success
          } catch (e: Exception) {
              _bahanBaku.value = snapshot // rollback
              OnlineWriteResult.Error(e.message ?: "Gagal update bahan baku")
          }
     }

    suspend fun delete(id: Long): OnlineWriteResult = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _bahanBaku.value
        val target = snapshot.find { it.id == id }
        _bahanBaku.value = snapshot.filter { it.id != id }  // optimistic: hapus dulu
        try {
            api.deleteBahanBaku(id)

            // ── Business logic: delete cashflow entry ──────────────────────
            if (target != null) {
                val cfList = cashflowRepo.list()
                val match = cfList.find { it.description == "Pembelian Bahan Baku: ${target.noTagihan}" }
                if (match != null) {
                    cashflowRepo.delete(match.id)
                }

                // Also delete payments of debt for this tagihan!
                val debtMatches = cfList.filter { it.description == "Pembayaran Hutang Bahan Baku: ${target.noTagihan}" }
                debtMatches.forEach { debtCf ->
                    cashflowRepo.delete(debtCf.id)
                }
            }

            cashflowRepo.refreshEntries()

            OnlineWriteResult.Success
        } catch (e: Exception) {
            _bahanBaku.value = snapshot  // rollback jika gagal
            OnlineWriteResult.Error(e.message ?: "Gagal hapus bahan baku")
        }
    }

    /** Context overload — backward compat untuk BahanBakuListViewModel */
    suspend fun delete(context: android.content.Context, tenantId: String, id: Long): OnlineWriteResult = delete(id)

    /** Bayar hutang bahan baku — update nominal di VPS */
    suspend fun payDebt(context: android.content.Context, tenantId: String, id: Long, amount: Double): OnlineWriteResult = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _bahanBaku.value
        val target = snapshot.find { it.id == id }
        val originalNominal = target?.nominal ?: 0.0
        val paymentAmount = amount - originalNominal
        _bahanBaku.value = snapshot.map { if (it.id == id) it.copy(nominal = amount) else it }
        try {
            api.updateBahanBaku(id, mapOf("nominal" to amount))
            if (paymentAmount > 0.0 && target != null) {
                cashflowRepo.createEntry(BmpCashflowData(
                    transactionType = "KELUAR",
                    description = "Pembayaran Hutang Bahan Baku: ${target.noTagihan}",
                    amount = paymentAmount,
                    transactionDate = System.currentTimeMillis()
                ))
                cashflowRepo.refreshEntries()
            }
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _bahanBaku.value = snapshot
            OnlineWriteResult.Error(e.message ?: "Gagal bayar hutang")
        }
    }

    /** Get by id — returns BmpBahanBakuEntity for backward compat */
    suspend fun getById(id: Long): com.posbah.app.data.local.entities.BmpBahanBakuEntity? {
        val data = try {
            api.getBahanBaku().body()?.map { it.toBmpBahanBakuData() }?.find { it.id == id }
        } catch (_: Exception) { null } ?: return null
        return com.posbah.app.data.local.entities.BmpBahanBakuEntity(
            id = data.id,
            tenantId = data.tenantId,
            noTagihan = data.noTagihan,
            tanggal = data.tanggal,
            supplier = data.supplier,
            totalHarga = data.totalHarga,
            nominal = data.nominal,
            notes = data.notes,
            notaFotoPath = data.notaFotoPath,
            notaFotoUrl = data.notaFotoUrl
        )
    }

    /** Get by nomor tagihan */
    suspend fun getByTagihan(noTagihan: String): com.posbah.app.data.local.entities.BmpBahanBakuEntity? {
        val data = try {
            api.getBahanBaku().body()?.map { it.toBmpBahanBakuData() }?.find { it.noTagihan == noTagihan }
        } catch (_: Exception) { null } ?: return null
        return com.posbah.app.data.local.entities.BmpBahanBakuEntity(
            id = data.id, tenantId = data.tenantId, noTagihan = data.noTagihan,
            tanggal = data.tanggal, supplier = data.supplier, totalHarga = data.totalHarga, nominal = data.nominal, notes = data.notes,
            notaFotoPath = data.notaFotoPath,
            notaFotoUrl = data.notaFotoUrl
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
                    jenisBahan = d.jenisBahan, kuantitas = d.kuantitas, unit = d.unit, rate = d.rate
                )
            })
        }
    }

    /** Update header only — alias for update() */
    suspend fun updateHeaderOnly(entity: com.posbah.app.data.local.entities.BmpBahanBakuEntity): OnlineWriteResult {
        return update(BmpBahanBakuData(
            id = entity.id, tenantId = entity.tenantId, noTagihan = entity.noTagihan,
            tanggal = entity.tanggal, supplier = entity.supplier, totalHarga = entity.totalHarga,
            nominal = entity.nominal, notes = entity.notes,
            notaFotoPath = entity.notaFotoPath, notaFotoUrl = entity.notaFotoUrl
        ))
    }

    /** Save (create or update) — alias based on entity id */
    suspend fun save(
        entity: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): OnlineWriteResult {
        val data = BmpBahanBakuData(
            id = entity.id, tenantId = entity.tenantId, noTagihan = entity.noTagihan,
            tanggal = entity.tanggal, supplier = entity.supplier, totalHarga = entity.totalHarga,
            nominal = entity.nominal, notes = entity.notes,
            notaFotoPath = entity.notaFotoPath, notaFotoUrl = entity.notaFotoUrl
        )
        val itemData = items.map {
            BmpBahanBakuItemData(
                id = it.id, bahanBakuId = it.bahanBakuId, jenisBahan = it.jenisBahan,
                kuantitas = it.kuantitas, unit = it.unit, rate = it.rate,
                subtotal = it.kuantitas * it.rate
            )
        }
        return if (entity.id == 0L) create(data, itemData) else update(data, itemData)
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
    ): OnlineWriteResult = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _bahanBaku.value
        val data = BmpBahanBakuData(
            id = entity.id,
            tenantId = entity.tenantId,
            noTagihan = entity.noTagihan,
            tanggal = entity.tanggal,
            supplier = entity.supplier,
            totalHarga = entity.totalHarga,
            nominal = entity.nominal,
            notes = entity.notes,
            notaFotoPath = entity.notaFotoPath,
            notaFotoUrl = entity.notaFotoUrl
        )
        val itemData = items.map {
            BmpBahanBakuItemData(
                id = it.id,
                bahanBakuId = it.bahanBakuId,
                jenisBahan = it.jenisBahan,
                kuantitas = it.kuantitas,
                unit = it.unit,
                rate = it.rate,
                subtotal = it.kuantitas * it.rate
            )
        }

        _bahanBaku.value = snapshot.map { if (it.id == entity.id) data else it }

        try {
            val oldBahanBaku = snapshot.find { it.id == entity.id }
            val oldNoTagihan = oldBahanBaku?.noTagihan ?: entity.noTagihan

            // 1. Update header
            api.updateBahanBaku(entity.id, mapOf(
                "noTagihan" to entity.noTagihan,
                "tanggal" to entity.tanggal,
                "supplier" to entity.supplier,
                "totalHarga" to entity.totalHarga,
                "nominal" to entity.nominal,
                "notaFotoPath" to entity.notaFotoPath,
                "notaFotoUrl" to entity.notaFotoUrl,
                "notes" to entity.notes
            ))

            // 2. Delete old items
            api.deleteBahanBakuItems(entity.id)

            // 3. Post new items
            if (itemData.isNotEmpty()) {
                val itemBodies = itemData.map {
                    mapOf<String, Any?>(
                        "bahanBakuId" to entity.id,
                        "jenisBahan" to it.jenisBahan,
                        "kuantitas" to it.kuantitas,
                        "unit" to it.unit,
                        "rate" to it.rate
                    )
                }
                api.createBahanBakuItems(itemBodies)
            }

            // 4. Update cashflow entry
            val cfList = cashflowRepo.list()
            val match = cfList.find { it.description == "Pembelian Bahan Baku: $oldNoTagihan" }
            
            // Perhitungan nominal cashflow pembelian yang benar:
            // nominal pembelian = (nominal baru) - (semua pembayaran hutang yang tercatat di cashflow)
            val debtMatches = cfList.filter { it.description == "Pembayaran Hutang Bahan Baku: $oldNoTagihan" }
            val totalPaidDebt = debtMatches.sumOf { it.amount }
            
            // Nominal awal pembelian yang baru = nominal baru di form - total cicilan hutang yang sudah dibayar
            // Ini mencegah double counting karena cicilan hutang tetap tercatat di cashflow tersendiri.
            val newInitialNominal = (entity.nominal - totalPaidDebt).coerceAtLeast(0.0)

            if (match != null) {
                cashflowRepo.update(match.copy(
                    description = "Pembelian Bahan Baku: ${entity.noTagihan}",
                    amount = newInitialNominal,
                    transactionDate = entity.tanggal
                ))
            } else {
                cashflowRepo.createEntry(BmpCashflowData(
                    transactionType = "KELUAR",
                    description = "Pembelian Bahan Baku: ${entity.noTagihan}",
                    amount = newInitialNominal,
                    transactionDate = entity.tanggal
                ))
            }

            // Update nama tagihan di pembayaran hutang jika berubah
            if (oldNoTagihan != entity.noTagihan) {
                debtMatches.forEach { debtCf ->
                    cashflowRepo.update(debtCf.copy(
                        description = "Pembayaran Hutang Bahan Baku: ${entity.noTagihan}"
                    ))
                }
            }

            cashflowRepo.refreshEntries()
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _bahanBaku.value = snapshot // rollback
            OnlineWriteResult.Error(e.message ?: "Gagal update bahan baku")
        }
    }

    suspend fun getItems(bahanBakuId: Long): List<BmpBahanBakuItemData> = try {
        api.getBahanBakuItems(bahanBakuId).body()?.map {
            val q = (it["kuantitas"] as? Number)?.toDouble() ?: 0.0
            val r = (it["rate"] as? Number)?.toDouble() ?: 0.0
            BmpBahanBakuItemData(
                id = (it["id"] as? Number)?.toLong() ?: 0,
                bahanBakuId = bahanBakuId,
                jenisBahan = it["jenisBahan"] as? String ?: "",
                kuantitas = q,
                unit = it["unit"] as? String ?: "kg",
                rate = r,
                subtotal = (it["subtotal"] as? Number)?.toDouble() ?: (q * r)
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
                    noTagihan = d.noTagihan,
                    tanggal = d.tanggal,
                    supplier = d.supplier,
                    totalHarga = d.totalHarga,
                    nominal = d.nominal,
                    notes = d.notes,
                    notaFotoPath = d.notaFotoPath,
                    notaFotoUrl = d.notaFotoUrl
                )
            }
        }
    }

    fun totalHarga(tenantId: String): kotlinx.coroutines.flow.Flow<Double> =
        _bahanBaku.map { list -> list.sumOf { it.totalHarga } }

    fun totalNominal(tenantId: String): kotlinx.coroutines.flow.Flow<Double> =
        _bahanBaku.map { list -> list.sumOf { it.nominal } }

    suspend fun getLatestMaterialRates(tenantId: String): Map<String, Double> {
        val bbList = list()
        val rates = mutableMapOf<String, Double>()
        val sortedBb = bbList.sortedByDescending { it.tanggal }
        val targetBb = sortedBb.take(20)
        
        coroutineScope {
            val deferreds = targetBb.map { bb ->
                async {
                    try {
                        val resp = api.getBahanBakuItems(bb.id)
                        if (resp.isSuccessful) {
                            resp.body()?.map {
                                val name = it["jenisBahan"] as? String ?: ""
                                val rate = (it["rate"] as? Number)?.toDouble() ?: 0.0
                                Pair(name, rate)
                            } ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            awaitAll(*deferreds.toTypedArray()).flatten().forEach { (name, rate) ->
                if (name.isNotEmpty() && !rates.containsKey(name)) {
                    rates[name] = rate
                }
            }
        }
        return rates
    }
}

// ── BmpStockRepository ────────────────────────────────────────────────────────

@Singleton
class BmpStockRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    private val _stocks = MutableStateFlow<List<com.posbah.app.data.local.entities.BmpProductStockEntity>>(emptyList())
    val stocks = _stocks.asStateFlow()

    private val _ledgers = MutableStateFlow<List<com.posbah.app.data.local.entities.BmpStockLedgerEntity>>(emptyList())
    val ledgers = _ledgers.asStateFlow()

    suspend fun refresh() {
        try {
            val stocksResp = api.getProductStocks()
            if (stocksResp.isSuccessful) {
                _stocks.value = stocksResp.body()?.map {
                    com.posbah.app.data.local.entities.BmpProductStockEntity(
                        id = (it["id"] as? Number)?.toLong() ?: 0,
                        tenantId = it["tenantId"] as? String ?: "",
                        outletId = (it["outletId"] as? Number)?.toLong(),
                        masterProductId = (it["masterItemId"] as? Number)?.toLong() ?: 0,
                        quantity = (it["currentStock"] as? Number)?.toDouble() ?: 0.0,
                        minStockAlert = 0.0,
                        isSynced = true,
                        isDeleted = false,
                        updatedAt = System.currentTimeMillis()
                    )
                } ?: emptyList()
            }
            val ledgerResp = api.getStockLedger()
            if (ledgerResp.isSuccessful) {
                _ledgers.value = ledgerResp.body()?.map {
                    com.posbah.app.data.local.entities.BmpStockLedgerEntity(
                        id = (it["id"] as? Number)?.toLong() ?: 0,
                        tenantId = it["tenantId"] as? String ?: "",
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
            }
        } catch (_: Exception) {}
    }
    suspend fun adjustStock(productId: Long, quantity: Double, reason: String) {
        adjustStock(productId = productId, quantity = quantity, reason = reason, refId = 0L)
    }

    suspend fun adjustStock(productId: Long, quantity: Double, reason: String, refId: Long) {
        adjustStock(
            masterItemId = productId,
            change = quantity,
            mutationType = reason,
            referenceId = refId
        )
    }

    suspend fun adjustStock(
        masterItemId: Long,
        change: Double,
        mutationType: String,
        referenceId: Long? = null,
        notes: String? = null
    ) {
        val stocksSnapshot = _stocks.value
        val ledgersSnapshot = _ledgers.value

        // 1. Optimistic update stocks
        val currentEntry = stocksSnapshot.find { it.masterProductId == masterItemId }
        val currentStock = currentEntry?.quantity ?: 0.0
        val newStock = (currentStock + change).coerceAtLeast(0.0)

        val updatedStocks = if (currentEntry != null) {
            stocksSnapshot.map {
                if (it.masterProductId == masterItemId) it.copy(quantity = newStock) else it
            }
        } else {
            stocksSnapshot + com.posbah.app.data.local.entities.BmpProductStockEntity(
                id = -System.currentTimeMillis(),
                tenantId = "",
                masterProductId = masterItemId,
                quantity = newStock,
                minStockAlert = 0.0,
                isSynced = true,
                isDeleted = false,
                updatedAt = System.currentTimeMillis()
            )
        }
        _stocks.value = updatedStocks

        // 2. Optimistic append ledger entry
        val newLedger = com.posbah.app.data.local.entities.BmpStockLedgerEntity(
            id = -System.currentTimeMillis(),
            tenantId = "",
            masterProductId = masterItemId,
            referenceId = referenceId ?: 0L,
            mutationType = mutationType,
            quantityChange = change,
            finalStock = newStock,
            notes = notes,
            isSynced = true,
            isDeleted = false,
            createdAt = System.currentTimeMillis()
        )
        _ledgers.value = listOf(newLedger) + ledgersSnapshot

        try {
            api.createProductStock(mapOf(
                "masterItemId" to masterItemId,
                "currentStock" to newStock
            ))

            api.addStockLedgerEntry(mapOf(
                "masterItemId" to masterItemId,
                "mutationType" to mutationType,
                "change" to change,
                "stockAfter" to newStock,
                "referenceId" to referenceId,
                "notes" to notes
            ))

            // Background refresh to get the actual database IDs and values
            refresh()
        } catch (e: Exception) {
            // Rollback on failure
            _stocks.value = stocksSnapshot
            _ledgers.value = ledgersSnapshot
        }
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

    fun observeStocks(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpProductStockEntity>> = stocks

    fun observeAllLedger(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpStockLedgerEntity>> = ledgers
}

// ── BmpSettingsRepository ─────────────────────────────────────────────────────

@Singleton
class BmpSettingsRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    private val _settings = MutableStateFlow<com.posbah.app.data.local.entities.BmpSettingsEntity?>(null)
    val settings = _settings.asStateFlow()

    suspend fun refresh() {
        val data = get()
        if (data != null) {
            _settings.value = com.posbah.app.data.local.entities.BmpSettingsEntity(
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
    suspend fun get(): BmpSettingsData? {
        val current = _settings.value
        if (current != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try { refresh() } catch (_: Exception) {}
            }
            return BmpSettingsData(
                id = current.id,
                tenantId = current.tenantId,
                companyName = current.clientName,
                address = current.addressLine1,
                phone = current.phoneNumber,
                email = current.emailAddress,
                npwp = current.taxNumber,
                logoUrl = current.clientLogo,
                bankInfo = "",
                invoicePrefix = "INV",
                listrikBulanan = current.listrikBulanan,
                jumlahMesin = current.jumlahMesin,
                jumlahKaryawan = current.jumlahKaryawan,
                gajiHarian = current.gajiHarian,
                hariKerjaSebulan = current.hariKerjaSebulan,
                biayaKarungPer1000 = current.biayaKarungPer1000,
                hoursPerDay = current.hoursPerDay,
                updatedAt = current.updatedAt
            )
        }
        val data = try {
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
        if (data != null) {
            _settings.value = com.posbah.app.data.local.entities.BmpSettingsEntity(
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
        return data
    }

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
            
            // Update the cached flow immediately on success
            _settings.value = com.posbah.app.data.local.entities.BmpSettingsEntity(
                id = settings.id,
                tenantId = settings.tenantId,
                clientName = settings.companyName,
                clientLogo = settings.logoUrl,
                addressLine1 = settings.address,
                phoneNumber = settings.phone,
                emailAddress = settings.email,
                taxNumber = settings.npwp,
                listrikBulanan = settings.listrikBulanan,
                jumlahMesin = settings.jumlahMesin,
                jumlahKaryawan = settings.jumlahKaryawan,
                gajiHarian = settings.gajiHarian,
                hariKerjaSebulan = settings.hariKerjaSebulan,
                biayaKarungPer1000 = settings.biayaKarungPer1000,
                hoursPerDay = settings.hoursPerDay,
                updatedAt = settings.updatedAt
            )
            
            OnlineWriteResult.Success
        } catch (e: Exception) { OnlineWriteResult.Error(e.message ?: "Gagal simpan settings") }
    }


    fun observe(tenantId: String): Flow<com.posbah.app.data.local.entities.BmpSettingsEntity?> = settings

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
    private val _settingsList = kotlinx.coroutines.flow.MutableStateFlow<List<PrintSettingsData>>(emptyList())
    val settingsList = _settingsList.asStateFlow()

    suspend fun refresh(moduleKey: String) {
        try {
            val data = get(moduleKey)
            if (data != null) {
                val snapshot = _settingsList.value
                _settingsList.value = snapshot.filter { it.moduleKey != moduleKey } + data
            }
        } catch (_: Exception) {}
    }

    suspend fun get(moduleKey: String): PrintSettingsData? {
        val cached = _settingsList.value.find { it.moduleKey == moduleKey }
        if (cached != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val fresh = api.getPrintSettings(moduleKey).body()?.firstOrNull()?.let { it ->
                        PrintSettingsData(
                            id = (it["id"] as? Number)?.toLong() ?: 0,
                            tenantId = it["tenantId"] as? String ?: "",
                            moduleKey = it["moduleKey"] as? String ?: moduleKey,
                            jpgUseLogo = it["jpgUseLogo"] as? Boolean ?: true,
                            jpgHeaderAlign = it["jpgHeaderAlign"] as? String ?: "LEFT",
                            jpgUseSignature = it["jpgUseSignature"] as? Boolean ?: true,
                            jpgSignatureSenderName = it["jpgSignatureSenderName"] as? String ?: "Admin",
                            jpgSignatureReceiverName = it["jpgSignatureReceiverName"] as? String ?: "",
                            jpgSignatureDrawnBase64 = it["jpgSignatureDrawnBase64"] as? String,
                            jpgIsColor = it["jpgIsColor"] as? Boolean ?: true,
                            sjUseLogo = it["sjUseLogo"] as? Boolean ?: true,
                            sjHeaderAlign = it["sjHeaderAlign"] as? String ?: "LEFT",
                            sjUseSignature = it["sjUseSignature"] as? Boolean ?: true,
                            sjSignatureSenderName = it["sjSignatureSenderName"] as? String ?: "Admin",
                            sjSignatureReceiverName = it["sjSignatureReceiverName"] as? String ?: "",
                            sjSignatureDrawnBase64 = it["sjSignatureDrawnBase64"] as? String,
                            sjIsColor = it["sjIsColor"] as? Boolean ?: false,
                            invoiceUseLogo = it["invoiceUseLogo"] as? Boolean ?: true,
                            invoiceHeaderAlign = it["invoiceHeaderAlign"] as? String ?: "LEFT",
                            invoiceUseSignature = it["invoiceUseSignature"] as? Boolean ?: true,
                            invoiceSignatureSenderName = it["invoiceSignatureSenderName"] as? String ?: "Admin",
                            invoiceSignatureReceiverName = it["invoiceSignatureReceiverName"] as? String ?: "",
                            invoiceSignatureDrawnBase64 = it["invoiceSignatureDrawnBase64"] as? String,
                            invoiceIsColor = it["invoiceIsColor"] as? Boolean ?: true,
                            receiptPaperWidth = it["receiptPaperWidth"] as? String ?: "MM80",
                            receiptUseLogo = it["receiptUseLogo"] as? Boolean ?: true,
                            receiptHeaderAlign = it["receiptHeaderAlign"] as? String ?: "CENTER",
                            receiptIsColor = it["receiptIsColor"] as? Boolean ?: false,
                            receiptShowItemPrice = it["receiptShowItemPrice"] as? Boolean ?: true,
                            receiptFooterText = it["receiptFooterText"] as? String ?: "Terima kasih sudah berbelanja!",
                            jpgTemplateType = it["jpgTemplateType"] as? String ?: "MODERN",
                            sjTemplateType = it["sjTemplateType"] as? String ?: "MODERN",
                            invoiceTemplateType = it["invoiceTemplateType"] as? String ?: "MODERN",
                            bankOwnerName = it["bankOwnerName"] as? String ?: "",
                            bankName = it["bankName"] as? String ?: "BCA",
                            bankAccountNumber = it["bankAccountNumber"] as? String ?: "",
                            logoPath = it["logoPath"] as? String,
                            logoUrl = it["logoUrl"] as? String ?: it["logoPath"] as? String,
                            jpgSignatureDrawnUrl = it["jpgSignatureDrawnUrl"] as? String,
                            sjSignatureDrawnUrl = it["sjSignatureDrawnUrl"] as? String,
                            invoiceSignatureDrawnUrl = it["invoiceSignatureDrawnUrl"] as? String,
                            createdAt = (it["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    }
                    if (fresh != null) {
                        _settingsList.value = _settingsList.value.filter { it.moduleKey != moduleKey } + fresh
                    }
                } catch (_: Exception) {}
            }
            return cached
        }
        val fresh = try {
            api.getPrintSettings(moduleKey).body()?.firstOrNull()?.let { it ->
                PrintSettingsData(
                    id = (it["id"] as? Number)?.toLong() ?: 0,
                    tenantId = it["tenantId"] as? String ?: "",
                    moduleKey = it["moduleKey"] as? String ?: moduleKey,
                    jpgUseLogo = it["jpgUseLogo"] as? Boolean ?: true,
                    jpgHeaderAlign = it["jpgHeaderAlign"] as? String ?: "LEFT",
                    jpgUseSignature = it["jpgUseSignature"] as? Boolean ?: true,
                    jpgSignatureSenderName = it["jpgSignatureSenderName"] as? String ?: "Admin",
                    jpgSignatureReceiverName = it["jpgSignatureReceiverName"] as? String ?: "",
                    jpgSignatureDrawnBase64 = it["jpgSignatureDrawnBase64"] as? String,
                    jpgIsColor = it["jpgIsColor"] as? Boolean ?: true,
                    sjUseLogo = it["sjUseLogo"] as? Boolean ?: true,
                    sjHeaderAlign = it["sjHeaderAlign"] as? String ?: "LEFT",
                    sjUseSignature = it["sjUseSignature"] as? Boolean ?: true,
                    sjSignatureSenderName = it["sjSignatureSenderName"] as? String ?: "Admin",
                    sjSignatureReceiverName = it["sjSignatureReceiverName"] as? String ?: "",
                    sjSignatureDrawnBase64 = it["sjSignatureDrawnBase64"] as? String,
                    sjIsColor = it["sjIsColor"] as? Boolean ?: false,
                    invoiceUseLogo = it["invoiceUseLogo"] as? Boolean ?: true,
                    invoiceHeaderAlign = it["invoiceHeaderAlign"] as? String ?: "LEFT",
                    invoiceUseSignature = it["invoiceUseSignature"] as? Boolean ?: true,
                    invoiceSignatureSenderName = it["invoiceSignatureSenderName"] as? String ?: "Admin",
                    invoiceSignatureReceiverName = it["invoiceSignatureReceiverName"] as? String ?: "",
                    invoiceSignatureDrawnBase64 = it["invoiceSignatureDrawnBase64"] as? String,
                    invoiceIsColor = it["invoiceIsColor"] as? Boolean ?: true,
                    receiptPaperWidth = it["receiptPaperWidth"] as? String ?: "MM80",
                    receiptUseLogo = it["receiptUseLogo"] as? Boolean ?: true,
                    receiptHeaderAlign = it["receiptHeaderAlign"] as? String ?: "CENTER",
                    receiptIsColor = it["receiptIsColor"] as? Boolean ?: false,
                    receiptShowItemPrice = it["receiptShowItemPrice"] as? Boolean ?: true,
                    receiptFooterText = it["receiptFooterText"] as? String ?: "Terima kasih sudah berbelanja!",
                    jpgTemplateType = it["jpgTemplateType"] as? String ?: "MODERN",
                    sjTemplateType = it["sjTemplateType"] as? String ?: "MODERN",
                    invoiceTemplateType = it["invoiceTemplateType"] as? String ?: "MODERN",
                    bankOwnerName = it["bankOwnerName"] as? String ?: "",
                    bankName = it["bankName"] as? String ?: "BCA",
                    bankAccountNumber = it["bankAccountNumber"] as? String ?: "",
                    logoPath = it["logoPath"] as? String,
                    logoUrl = it["logoUrl"] as? String ?: it["logoPath"] as? String,
                    jpgSignatureDrawnUrl = it["jpgSignatureDrawnUrl"] as? String,
                    sjSignatureDrawnUrl = it["sjSignatureDrawnUrl"] as? String,
                    invoiceSignatureDrawnUrl = it["invoiceSignatureDrawnUrl"] as? String,
                    createdAt = (it["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (it["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
        } catch (_: Exception) { null }
        if (fresh != null) {
            _settingsList.value = _settingsList.value.filter { it.moduleKey != moduleKey } + fresh
        }
        return fresh
    }

    suspend fun save(settings: PrintSettingsData): OnlineWriteResult {
        val snapshot = _settingsList.value
        _settingsList.value = snapshot.filter { it.moduleKey != settings.moduleKey } + settings
        return try {
            api.savePrintSettings(mapOf(
                "id" to settings.id,
                "tenantId" to settings.tenantId,
                "moduleKey" to settings.moduleKey,
                "jpgUseLogo" to settings.jpgUseLogo,
                "jpgHeaderAlign" to settings.jpgHeaderAlign,
                "jpgUseSignature" to settings.jpgUseSignature,
                "jpgSignatureSenderName" to settings.jpgSignatureSenderName,
                "jpgSignatureReceiverName" to settings.jpgSignatureReceiverName,
                "jpgSignatureDrawnBase64" to settings.jpgSignatureDrawnBase64,
                "jpgIsColor" to settings.jpgIsColor,
                "sjUseLogo" to settings.sjUseLogo,
                "sjHeaderAlign" to settings.sjHeaderAlign,
                "sjUseSignature" to settings.sjUseSignature,
                "sjSignatureSenderName" to settings.sjSignatureSenderName,
                "sjSignatureReceiverName" to settings.sjSignatureReceiverName,
                "sjSignatureDrawnBase64" to settings.sjSignatureDrawnBase64,
                "sjIsColor" to settings.sjIsColor,
                "invoiceUseLogo" to settings.invoiceUseLogo,
                "invoiceHeaderAlign" to settings.invoiceHeaderAlign,
                "invoiceUseSignature" to settings.invoiceUseSignature,
                "invoiceSignatureSenderName" to settings.invoiceSignatureSenderName,
                "invoiceSignatureReceiverName" to settings.invoiceSignatureReceiverName,
                "invoiceSignatureDrawnBase64" to settings.invoiceSignatureDrawnBase64,
                "invoiceIsColor" to settings.invoiceIsColor,
                "receiptPaperWidth" to settings.receiptPaperWidth,
                "receiptUseLogo" to settings.receiptUseLogo,
                "receiptHeaderAlign" to settings.receiptHeaderAlign,
                "receiptIsColor" to settings.receiptIsColor,
                "receiptShowItemPrice" to settings.receiptShowItemPrice,
                "receiptFooterText" to settings.receiptFooterText,
                "jpgTemplateType" to settings.jpgTemplateType,
                "sjTemplateType" to settings.sjTemplateType,
                "invoiceTemplateType" to settings.invoiceTemplateType,
                "bankOwnerName" to settings.bankOwnerName,
                "bankName" to settings.bankName,
                "bankAccountNumber" to settings.bankAccountNumber,
                "logoPath" to settings.logoPath,
                "logoUrl" to settings.logoUrl,
                "jpgSignatureDrawnUrl" to settings.jpgSignatureDrawnUrl,
                "sjSignatureDrawnUrl" to settings.sjSignatureDrawnUrl,
                "invoiceSignatureDrawnUrl" to settings.invoiceSignatureDrawnUrl,
                "createdAt" to settings.createdAt,
                "updatedAt" to settings.updatedAt
            ))
            OnlineWriteResult.Success
        } catch (e: Exception) {
            _settingsList.value = snapshot
            OnlineWriteResult.Error(e.message ?: "Gagal simpan print settings")
        }
    }

    /**
     * Observe print settings sebagai Flow — backward compat untuk ViewModel lama.
     */
    fun observe(tenantId: String, moduleKey: String): kotlinx.coroutines.flow.Flow<PrintSettingsData?> =
        _settingsList.map { list -> list.find { it.moduleKey == moduleKey } }
}
