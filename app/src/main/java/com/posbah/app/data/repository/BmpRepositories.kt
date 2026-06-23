package com.posbah.app.data.repository

import android.content.Context
import com.posbah.app.data.local.dao.BmpCashFlowDao
import com.posbah.app.data.local.dao.BmpProductStockDao
import com.posbah.app.data.local.dao.BmpStockLedgerDao
import com.posbah.app.data.local.dao.BmpProductionLogDao
import com.posbah.app.data.local.entities.BmpProductStockEntity
import com.posbah.app.data.local.entities.BmpStockLedgerEntity
import com.posbah.app.data.local.entities.BmpProductionLogEntity
import com.posbah.app.data.local.dao.BmpClientDao
import com.posbah.app.data.local.dao.BmpEmployeeDao
import com.posbah.app.data.local.dao.BmpInvoiceDao
import com.posbah.app.data.local.dao.BmpMasterProductDao
import com.posbah.app.data.local.dao.BmpPaymentDao
import com.posbah.app.data.local.dao.BmpPayrollDao
import com.posbah.app.data.local.dao.BmpProductDao
import com.posbah.app.data.local.dao.BmpSettingsDao
import com.posbah.app.data.local.entities.BmpCashFlowEntity
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpEmployeeEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpPayrollEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.remote.BmpOnlineWriter
import com.posbah.app.data.remote.SupabaseSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.room.withTransaction
import com.posbah.app.data.local.dao.PrintSettingsDao
import com.posbah.app.data.local.entities.PrintSettingsEntity

sealed class OnlineWriteResult {
    object Success : OnlineWriteResult()
    data class Error(val message: String) : OnlineWriteResult()
    object NoConnection : OnlineWriteResult()
}

fun com.posbah.app.data.remote.SupabaseSyncManager.SyncResult.toOnlineWriteResult(): OnlineWriteResult = when (this) {
    is com.posbah.app.data.remote.SupabaseSyncManager.SyncResult.Success -> OnlineWriteResult.Success
    is com.posbah.app.data.remote.SupabaseSyncManager.SyncResult.Error -> OnlineWriteResult.Error(this.message)
    is com.posbah.app.data.remote.SupabaseSyncManager.SyncResult.NoConnection -> OnlineWriteResult.NoConnection
}

@Singleton
class BmpClientRepository @Inject constructor(
    private val dao: BmpClientDao,
    private val invoiceDao: BmpInvoiceDao,
    private val paymentDao: BmpPaymentDao,
    private val cashFlowDao: BmpCashFlowDao,
    private val productDao: BmpProductDao,
    private val db: com.posbah.app.data.local.PosBahDatabase
) {
    fun observe(tenantId: String): Flow<List<BmpClientEntity>> = dao.observe(tenantId)
    fun search(tenantId: String, q: String): Flow<List<BmpClientEntity>> = dao.search(tenantId, q)
    suspend fun getById(id: Long) = dao.getById(id)

    /**
     * Upsert klien ke VPS dulu, jika berhasil baru simpan ke Room.
     * Mengembalikan [OnlineWriteResult] agar caller dapat menampilkan error ke UI.
     */
    suspend fun upsert(context: Context, client: BmpClientEntity): OnlineWriteResult {
        val isNew = client.id == 0L
        val clientDraft = client.copy(
            uniqueID = client.uniqueID ?: UUID.randomUUID().toString(),
            slug = client.slug ?: client.clientName.toSlug(),
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        val localId = dao.upsert(clientDraft)
        val final = clientDraft.copy(id = if (isNew) localId else clientDraft.id, isSynced = true)

        val result = BmpOnlineWriter.upsertClient(context, final.tenantId, final).toOnlineWriteResult()
        if (result is OnlineWriteResult.Success) {
            dao.upsert(final)
        } else {
            if (isNew) {
                dao.hardDelete(localId)
            }
        }
        return result
    }

    /**
     * Cascade-delete klien beserta SEMUA invoice, produk, pembayaran, dan cashflow terkait.
     * Operasi VPS dulu, lalu soft-delete lokal.
     */
    suspend fun delete(context: Context, tenantId: String, id: Long): OnlineWriteResult {
        val result = BmpOnlineWriter.deleteClient(context, tenantId, id).toOnlineWriteResult()
        if (result is OnlineWriteResult.Success) {
            db.withTransaction {
                val invoices = invoiceDao.getByClientId(id)
                for (invoice in invoices) {
                    val payments = paymentDao.listAllForInvoice(invoice.id)
                    for (payment in payments) {
                        cashFlowDao.softDeleteByPaymentRefId(payment.id)
                    }
                    paymentDao.softDeleteByInvoice(invoice.id)
                    productDao.softDeleteByInvoice(invoice.id)
                    cashFlowDao.deleteExitsForInvoice(invoice.number)
                }
                invoiceDao.softDeleteByClientId(id)
                dao.softDelete(id)
            }
        }
        return result
    }

    fun count(tenantId: String) = dao.count(tenantId)

    private fun String.toSlug(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifEmpty { UUID.randomUUID().toString().take(8) }
}

@Singleton
class BmpInvoiceRepository @Inject constructor(
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val invoiceDao: BmpInvoiceDao,
    private val productDao: BmpProductDao,
    private val paymentDao: BmpPaymentDao,
    private val cashFlowDao: BmpCashFlowDao,
    private val clientDao: BmpClientDao,
    private val aggregate: BmpAggregateDaoImpl,
    private val stockRepo: BmpStockRepository
) {
    fun observe(tenantId: String): Flow<List<BmpInvoiceEntity>> = invoiceDao.observe(tenantId)
    fun observeByStatus(tenantId: String, status: String) = invoiceDao.observeByStatus(tenantId, status)
    suspend fun getById(id: Long) = invoiceDao.getById(id)
    fun observeProducts(invoiceId: Long): Flow<List<BmpProductEntity>> =
        productDao.observeByInvoice(invoiceId)
    fun observePayments(invoiceId: Long): Flow<List<BmpInvoicePaymentEntity>> =
        paymentDao.observeForInvoice(invoiceId)

    fun count(tenantId: String) = invoiceDao.count(tenantId)
    fun totalAmount(tenantId: String) = invoiceDao.totalAmount(tenantId)
    fun totalPaid(tenantId: String) = invoiceDao.totalPaid(tenantId)
    fun totalOutstanding(tenantId: String) = invoiceDao.totalOutstanding(tenantId)

    /**
     * Buat invoice baru: VPS dulu → Room jika berhasil.
     * Mengembalikan Pair(id, OnlineWriteResult) — id = -1 jika gagal.
     */
    suspend fun createInvoice(
        context: Context,
        invoice: BmpInvoiceEntity,
        products: List<BmpProductEntity>
    ): Pair<Long, OnlineWriteResult> {
        val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
        val totalPaid = invoice.paidAmount
        val newStatus = when {
            totalPaid >= total - 0.01 -> "PAID"
            totalPaid > 0 -> "PARTIAL"
            else -> "UNPAID"
        }
        val days = try {
            val clean = invoice.paymentTerms.trim().lowercase()
            if (clean.contains("cash") || clean.contains("tunai") || clean.contains("cod")) {
                0L
            } else {
                invoice.paymentTerms.split(" ").firstOrNull()?.toLongOrNull() ?: 14L
            }
        } catch (e: Exception) {
            14L
        }
        val computedDueDate = invoice.dueDate ?: (invoice.createdAt + days * 24 * 60 * 60 * 1000L)
        val final = invoice.copy(
            totalAmount = total,
            status = newStatus,
            dueDate = computedDueDate,
            slug = invoice.slug.ifBlank { autoSlug(invoice.number) },
            isSynced = false
        )

        // ── 1. Save to Room first to generate true IDs ───────────────────────
        val (id, paymentIds, cashflowIds) = db.withTransaction {
            val newId = invoiceDao.insert(final)
            val mappedProducts = products.map { prod ->
                var finalHargaBeli = prod.hargaBeli
                if (!prod.isKhusus && prod.masterItemID != null) {
                    val mp = db.bmpMasterProductDao().getById(prod.masterItemID)
                    if (mp != null) {
                        finalHargaBeli = mp.hppTotalPcs * prod.quantity * prod.jumlahLusin
                    }
                }
                prod.copy(invoiceId = newId, hargaBeli = finalHargaBeli, isSynced = false)
            }
            productDao.insertAll(mappedProducts)

            // Deduct finished goods stock
            for (prod in mappedProducts) {
                if (prod.masterItemID != null) {
                    stockRepo.adjustStock(
                        context = context,
                        tenantId = invoice.tenantId,
                        productId = prod.masterItemID,
                        change = -(prod.quantity * prod.jumlahLusin),
                        mutationType = "PENJUALAN",
                        referenceId = newId,
                        notes = "Penjualan Invoice #${final.number}"
                    )
                }
            }

            val payIds = mutableListOf<Long>()
            val cfIds = mutableListOf<Long>()

            if (totalPaid > 0) {
                val payment = BmpInvoicePaymentEntity(
                    tenantId = invoice.tenantId,
                    invoiceId = newId,
                    paymentDate = System.currentTimeMillis(),
                    paymentAmount = totalPaid,
                    paymentMethod = "CASH",
                    notes = "Uang muka saat pembuatan Invoice",
                    isSynced = false
                )
                val paymentId = paymentDao.insert(payment)
                payIds.add(paymentId)

                val cf = BmpCashFlowEntity(
                    tenantId = invoice.tenantId,
                    transactionDate = System.currentTimeMillis(),
                    transactionType = "MASUK",
                    description = "Pembayaran Invoice ${final.number}",
                    amount = totalPaid,
                    paymentRefId = paymentId,
                    isSynced = false
                )
                val cfId = cashFlowDao.insert(cf)
                cfIds.add(cfId)
            }

            // Check for isKhusus items with hargaBeli > 0
            for (prod in mappedProducts) {
                if (prod.isKhusus && prod.hargaBeli > 0) {
                    val cf = BmpCashFlowEntity(
                        tenantId = prod.tenantId,
                        transactionDate = System.currentTimeMillis(),
                        transactionType = "KELUAR",
                        description = "Pembelian barang khusus untuk Faktur ${final.number}",
                        amount = prod.hargaBeli,
                        isSynced = false
                    )
                    val cfId = cashFlowDao.insert(cf)
                    cfIds.add(cfId)
                }
            }
            Triple(newId, payIds, cfIds)
        }

        // ── 2. Get saved invoice with Room-generated ID and push header to VPS ──
        val savedInvoice = invoiceDao.getById(id) ?: return Pair(-1L, OnlineWriteResult.Error("Gagal simpan invoice lokal"))
        val finalInvoiceWithId = savedInvoice.copy(isSynced = true)

        val vpsResult = BmpOnlineWriter.upsertInvoice(context, final.tenantId, finalInvoiceWithId).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) {
            // Rollback/delete the inserted local data
            db.withTransaction {
                val insertedProducts = productDao.listByInvoice(id)
                for (prod in insertedProducts) {
                    if (prod.masterItemID != null) {
                        stockRepo.adjustStock(
                            context = context,
                            tenantId = invoice.tenantId,
                            productId = prod.masterItemID,
                            change = (prod.quantity * prod.jumlahLusin), // add back the stock
                            mutationType = "PENJUALAN",
                            referenceId = id,
                            notes = "Batal Penjualan Invoice #${final.number}"
                        )
                    }
                }
                productDao.deleteByInvoice(id)
                for (payId in paymentIds) {
                    paymentDao.hardDelete(payId)
                }
                for (cfId in cashflowIds) {
                    cashFlowDao.hardDelete(cfId)
                }
                invoiceDao.hardDelete(id)
            }
            return Pair(-1L, vpsResult)
        }

        // Mark invoice header as synced in Room
        invoiceDao.markSynced(id)

        // ── 3. VPS header successful → upload products, payments, cashflows ──
        val productsForVps = productDao.listByInvoice(id)
        if (productsForVps.isNotEmpty()) {
            val prodRes = BmpOnlineWriter.upsertProducts(context, final.tenantId, productsForVps)
            if (prodRes is SupabaseSyncManager.SyncResult.Success) {
                productsForVps.forEach { productDao.markSynced(it.id) }
            }
        }

        if (paymentIds.isNotEmpty()) {
            val paymentsForVps = paymentIds.mapNotNull { paymentDao.getById(it) }
            if (paymentsForVps.isNotEmpty()) {
                paymentsForVps.forEach { pay ->
                    val payRes = BmpOnlineWriter.upsertPayment(context, final.tenantId, pay)
                    if (payRes is SupabaseSyncManager.SyncResult.Success) {
                        paymentDao.markSynced(pay.id)
                    }
                }
            }
        }

        if (cashflowIds.isNotEmpty()) {
            cashflowIds.forEach { cfId ->
                val savedCf = cashFlowDao.getAll().find { it.id == cfId }
                if (savedCf != null) {
                    val cfRes = BmpOnlineWriter.upsertCashFlow(context, final.tenantId, savedCf)
                    if (cfRes is SupabaseSyncManager.SyncResult.Success) {
                        cashFlowDao.markSynced(cfId)
                    }
                }
            }
        }

        return Pair(id, OnlineWriteResult.Success)
    }

    /**
     * Update invoice: VPS dulu → Room jika berhasil.
     */
    suspend fun updateInvoice(
        context: Context,
        invoice: BmpInvoiceEntity,
        products: List<BmpProductEntity>
    ): OnlineWriteResult {
        // Build final invoice state first
        val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
        val totalPaidAmt = paymentDao.sumForInvoice(invoice.id)
        val days = try {
            val clean = invoice.paymentTerms.trim().lowercase()
            if (clean.contains("cash") || clean.contains("tunai") || clean.contains("cod")) 0L
            else invoice.paymentTerms.split(" ").firstOrNull()?.toLongOrNull() ?: 14L
        } catch (_: Exception) { 14L }
        val computedDueDate = invoice.dueDate ?: (invoice.createdAt + days * 24 * 60 * 60 * 1000L)
        val newStatus = when {
            totalPaidAmt >= total - 0.01 -> "PAID"
            totalPaidAmt > 0 -> if (computedDueDate != null && System.currentTimeMillis() > computedDueDate) "OVERDUE" else "PARTIAL"
            else -> if (computedDueDate != null && System.currentTimeMillis() > computedDueDate) "OVERDUE" else "UNPAID"
        }
        val finalInvoice = invoice.copy(
            totalAmount = total,
            paidAmount = totalPaidAmt,
            status = newStatus,
            dueDate = computedDueDate,
            isSynced = true,
            updatedAt = System.currentTimeMillis()
        )

        // ── 1. VPS dulu ───────────────────────────────────────────────────────
        val oldProducts = productDao.listByInvoice(invoice.id)
        if (oldProducts.isNotEmpty()) {
            BmpOnlineWriter.deleteProducts(context, invoice.tenantId, oldProducts.map { it.id })
        }
        val vpsResult = BmpOnlineWriter.upsertInvoice(context, finalInvoice.tenantId, finalInvoice).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        // ── 2. VPS berhasil → update Room ─────────────────────────────────────
        val cashflowIds = db.withTransaction {
            // Restore stock of old products before deleting them
            val oldProducts = productDao.listByInvoice(invoice.id)
            for (prod in oldProducts) {
                if (prod.masterItemID != null) {
                    stockRepo.adjustStock(
                        context = context,
                        tenantId = invoice.tenantId,
                        productId = prod.masterItemID,
                        change = prod.quantity * prod.jumlahLusin,
                        mutationType = "PENJUALAN",
                        referenceId = invoice.id,
                        notes = "Koreksi Invoice #${invoice.number} (Kembalikan)"
                    )
                }
            }

            productDao.deleteByInvoice(invoice.id)
            invoiceDao.update(finalInvoice)

            val mappedProducts = products.map { prod ->
                var finalHargaBeli = prod.hargaBeli
                if (!prod.isKhusus && prod.masterItemID != null) {
                    val mp = db.bmpMasterProductDao().getById(prod.masterItemID)
                    if (mp != null) {
                        finalHargaBeli = mp.hppTotalPcs * prod.quantity * prod.jumlahLusin
                    }
                }
                prod.copy(invoiceId = invoice.id, hargaBeli = finalHargaBeli, isSynced = false)
            }
            productDao.insertAll(mappedProducts)

            // Deduct stock of new products
            for (prod in mappedProducts) {
                if (prod.masterItemID != null) {
                    stockRepo.adjustStock(
                        context = context,
                        tenantId = invoice.tenantId,
                        productId = prod.masterItemID,
                        change = -(prod.quantity * prod.jumlahLusin),
                        mutationType = "PENJUALAN",
                        referenceId = invoice.id,
                        notes = "Koreksi Invoice #${invoice.number} (Kurangi)"
                    )
                }
            }

            // Re-sync cash flow exits for this invoice
            cashFlowDao.deleteExitsForInvoice(invoice.number)
            val cfIds = mutableListOf<Long>()
            for (prod in mappedProducts) {
                if (prod.isKhusus && prod.hargaBeli > 0) {
                    val cf = BmpCashFlowEntity(
                        tenantId = prod.tenantId,
                        transactionDate = System.currentTimeMillis(),
                        transactionType = "KELUAR",
                        description = "Pembelian barang khusus untuk Faktur ${invoice.number}",
                        amount = prod.hargaBeli,
                        isSynced = false
                    )
                    val cfId = cashFlowDao.insert(cf)
                    cfIds.add(cfId)
                }
            }
            cfIds
        }

        // Upload updated products to VPS
        val updatedProducts = productDao.listByInvoice(invoice.id)
        if (updatedProducts.isNotEmpty()) {
            val prodRes = BmpOnlineWriter.upsertProducts(context, finalInvoice.tenantId, updatedProducts)
            if (prodRes is SupabaseSyncManager.SyncResult.Success) {
                updatedProducts.forEach { productDao.markSynced(it.id) }
            }
        }

        // Upload new cashflows if any
        if (cashflowIds.isNotEmpty()) {
            cashflowIds.forEach { cfId ->
                val savedCf = cashFlowDao.getAll().find { it.id == cfId }
                if (savedCf != null) {
                    val cfRes = BmpOnlineWriter.upsertCashFlow(context, finalInvoice.tenantId, savedCf)
                    if (cfRes is SupabaseSyncManager.SyncResult.Success) {
                        cashFlowDao.markSynced(cfId)
                    }
                }
            }
        }

        return OnlineWriteResult.Success
    }

    /**
     * Hapus invoice: VPS dulu → soft-delete Room jika berhasil.
     */
    suspend fun deleteInvoice(context: Context, tenantId: String, id: Long): OnlineWriteResult {
        val products = productDao.listByInvoice(id)
        if (products.isNotEmpty()) {
            BmpOnlineWriter.deleteProducts(context, tenantId, products.map { it.id })
        }
        val vpsResult = BmpOnlineWriter.deleteInvoice(context, tenantId, id).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        db.withTransaction {
            val invoice = invoiceDao.getById(id)
                ?: invoiceDao.getAllForTenant("").find { it.id == id }
            if (invoice != null) {
                cashFlowDao.deleteExitsForInvoice(invoice.number)
                val products = productDao.listByInvoice(id)
                for (prod in products) {
                    if (prod.masterItemID != null) {
                        stockRepo.adjustStock(
                            context = context,
                            tenantId = invoice.tenantId,
                            productId = prod.masterItemID,
                            change = prod.quantity * prod.jumlahLusin,
                            mutationType = "PENJUALAN",
                            referenceId = id,
                            notes = "Pembatalan Invoice #${invoice.number} (Kembalikan)"
                        )
                    }
                }
            }
            val payments = paymentDao.listAllForInvoice(id)
            for (payment in payments) {
                cashFlowDao.softDeleteByPaymentRefId(payment.id)
            }
            paymentDao.softDeleteByInvoice(id)
            productDao.softDeleteByInvoice(id)
            invoiceDao.softDelete(id)
        }
        return OnlineWriteResult.Success
    }

    suspend fun markAsUnsynced(id: Long) {
        invoiceDao.markUnsynced(id)
    }

    suspend fun saveReceiverSignature(
        context: Context,
        tenantId: String,
        invoiceId: Long,
        signaturePath: String?,
        signatureUrl: String?,
        receiverName: String
    ): OnlineWriteResult {
        val invoice = invoiceDao.getById(invoiceId) ?: return OnlineWriteResult.Error("Invoice tidak ditemukan")
        val updatedInvoice = invoice.copy(
            receiverSignaturePath = signaturePath,
            receiverSignatureUrl = signatureUrl,
            receiverNameActual = receiverName,
            isSynced = true,
            updatedAt = System.currentTimeMillis()
        )
        val vpsResult = BmpOnlineWriter.upsertInvoice(context, tenantId, updatedInvoice).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        invoiceDao.update(updatedInvoice)
        invoice.clientId?.let { cId ->
            val client = clientDao.getById(cId)
            if (client != null) {
                val updatedClient = client.copy(
                    receiverSignatureUrl = signatureUrl ?: client.receiverSignatureUrl,
                    receiverNameActual = if (receiverName.isNotEmpty()) receiverName else client.receiverNameActual,
                    isSynced = true,
                    updatedAt = System.currentTimeMillis()
                )
                clientDao.update(updatedClient)
            }
        }
        return OnlineWriteResult.Success
    }

    sealed class RemoteSignatureResult {
        data class Success(val url: String, val name: String) : RemoteSignatureResult()
        object Pending : RemoteSignatureResult()
        data class Error(val message: String) : RemoteSignatureResult()
    }

    suspend fun checkReceiverSignatureRemote(tenantId: String, invoiceId: Long): RemoteSignatureResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var conn: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL("https://www.zedmz.cloud/api/invoice/signature-status?id=$invoiceId")
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val code = conn.responseCode
            if (code in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val array = org.json.JSONArray(response)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val urlStr = obj.optString("receiverSignatureUrl", "")
                    val nameStr = obj.optString("receiverNameActual", "")
                    if (!urlStr.isNullOrBlank() && urlStr != "null") {
                        return@withContext RemoteSignatureResult.Success(urlStr, nameStr)
                    }
                }
            }
            RemoteSignatureResult.Pending
        } catch (e: Exception) {
            e.printStackTrace()
            RemoteSignatureResult.Error(e.message ?: "Koneksi gagal")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Record a payment. Updates running paid amount, derives new status,
     * and emits a cash-flow entry with paymentRefId.
     */
    suspend fun recordPayment(
        context: Context,
        tenantId: String,
        invoiceId: Long,
        paymentAmount: Double,
        paymentMethod: String,
        notes: String?,
        paymentDate: Long = System.currentTimeMillis()
    ): OnlineWriteResult {
        val invoice = invoiceDao.getById(invoiceId) ?: return OnlineWriteResult.Error("Invoice tidak ditemukan")

        // Build payment entity first (id=0 → Room assigns real id after insert)
        val paymentDraft = BmpInvoicePaymentEntity(
            tenantId = tenantId,
            invoiceId = invoiceId,
            paymentDate = paymentDate,
            paymentAmount = paymentAmount,
            paymentMethod = paymentMethod,
            notes = notes,
            isSynced = false
        )

        // Insert to Room first to get the real id, then upsert to VPS
        val paymentId = paymentDao.insert(paymentDraft)
        val savedPayment = paymentDao.getById(paymentId) ?: return OnlineWriteResult.Error("Gagal simpan pembayaran")

        val vpsPayment = BmpOnlineWriter.upsertPayment(context, tenantId, savedPayment).toOnlineWriteResult()
        if (vpsPayment !is OnlineWriteResult.Success) {
            paymentDao.softDelete(paymentId)
            return vpsPayment
        }
        paymentDao.markSynced(paymentId)

        val totalPaid = paymentDao.sumForInvoice(invoiceId)
        val newStatus = when {
            totalPaid >= invoice.totalAmount - 0.01 -> "PAID"
            totalPaid > 0 -> if (invoice.dueDate != null && System.currentTimeMillis() > invoice.dueDate) "OVERDUE" else "PARTIAL"
            else -> if (invoice.dueDate != null && System.currentTimeMillis() > invoice.dueDate) "OVERDUE" else "UNPAID"
        }
        val updatedInvoice = invoice.copy(
            paidAmount = totalPaid,
            status = newStatus,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        invoiceDao.update(updatedInvoice)
        val vpsInv = BmpOnlineWriter.upsertInvoice(context, tenantId, updatedInvoice).toOnlineWriteResult()
        if (vpsInv is OnlineWriteResult.Success) {
            invoiceDao.markSynced(invoiceId)
        }

        val cf = BmpCashFlowEntity(
            tenantId = tenantId,
            transactionDate = paymentDate,
            transactionType = "MASUK",
            description = "Pembayaran Invoice ${invoice.number}",
            amount = paymentAmount,
            paymentRefId = paymentId,
            isSynced = false
        )
        val cfId = cashFlowDao.insert(cf)
        val finalCf = cf.copy(id = cfId)
        val cfRes = BmpOnlineWriter.upsertCashFlow(context, tenantId, finalCf)
        if (cfRes is SupabaseSyncManager.SyncResult.Success) {
            cashFlowDao.markSynced(cfId)
        }

        return OnlineWriteResult.Success
    }

    suspend fun editPayment(
        context: Context,
        tenantId: String,
        paymentId: Long,
        newAmount: Double,
        paymentMethod: String,
        notes: String?,
        paymentDate: Long = System.currentTimeMillis()
    ): OnlineWriteResult {
        val payment = paymentDao.getById(paymentId) ?: return OnlineWriteResult.Error("Pembayaran tidak ditemukan")
        val invoiceId = payment.invoiceId
        val invoice = invoiceDao.getById(invoiceId) ?: return OnlineWriteResult.Error("Invoice tidak ditemukan")

        val updatedPayment = payment.copy(
            paymentAmount = newAmount,
            paymentMethod = paymentMethod,
            notes = notes,
            paymentDate = paymentDate,
            isSynced = false
        )
        val vpsResult = BmpOnlineWriter.upsertPayment(context, tenantId, updatedPayment).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        paymentDao.update(updatedPayment)
        paymentDao.markSynced(paymentId)

        val existingCf = cashFlowDao.getByPaymentRefId(paymentId)
        if (existingCf != null) {
            val updatedCf = existingCf.copy(
                amount = newAmount,
                transactionDate = paymentDate,
                description = "Pembayaran Invoice ${invoice.number}",
                isSynced = false
            )
            cashFlowDao.update(updatedCf)
            val cfRes = BmpOnlineWriter.upsertCashFlow(context, tenantId, updatedCf)
            if (cfRes is SupabaseSyncManager.SyncResult.Success) {
                cashFlowDao.markSynced(updatedCf.id)
            }
        } else {
            val newCf = BmpCashFlowEntity(
                tenantId = tenantId,
                transactionDate = paymentDate,
                transactionType = "MASUK",
                description = "Pembayaran Invoice ${invoice.number}",
                amount = newAmount,
                paymentRefId = paymentId,
                isSynced = false
            )
            val newCfId = cashFlowDao.insert(newCf)
            val finalCf = newCf.copy(id = newCfId)
            val cfRes = BmpOnlineWriter.upsertCashFlow(context, tenantId, finalCf)
            if (cfRes is SupabaseSyncManager.SyncResult.Success) {
                cashFlowDao.markSynced(newCfId)
            }
        }

        recalculateInvoiceStatus(context, invoiceId)
        return OnlineWriteResult.Success
    }

    suspend fun deletePayment(context: Context, tenantId: String, paymentId: Long): OnlineWriteResult {
        val payment = paymentDao.getById(paymentId) ?: return OnlineWriteResult.Error("Pembayaran tidak ditemukan")
        val invoiceId = payment.invoiceId

        val vpsResult = BmpOnlineWriter.deletePayment(context, tenantId, paymentId).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        cashFlowDao.softDeleteByPaymentRefId(paymentId)
        paymentDao.softDelete(paymentId)
        recalculateInvoiceStatus(context, invoiceId)
        return OnlineWriteResult.Success
    }

    private suspend fun recalculateInvoiceStatus(context: Context, invoiceId: Long) {
        val invoice = invoiceDao.getById(invoiceId) ?: return
        val totalPaid = paymentDao.sumForInvoice(invoiceId)
        val newStatus = when {
            totalPaid >= invoice.totalAmount - 0.01 -> "PAID"
            totalPaid > 0 -> {
                if (invoice.dueDate != null && System.currentTimeMillis() > invoice.dueDate) "OVERDUE" else "PARTIAL"
            }
            else -> {
                if (invoice.dueDate != null && System.currentTimeMillis() > invoice.dueDate) "OVERDUE" else "UNPAID"
            }
        }
        val updatedInvoice = invoice.copy(
            paidAmount = totalPaid,
            status = newStatus,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        invoiceDao.update(updatedInvoice)
        val vpsInv = BmpOnlineWriter.upsertInvoice(context, invoice.tenantId, updatedInvoice).toOnlineWriteResult()
        if (vpsInv is OnlineWriteResult.Success) {
            invoiceDao.markSynced(invoiceId)
        }
    }

    suspend fun payMassal(
        tenantId: String,
        clientId: Long,
        nominal: Double,
        paymentMethod: String,
        notes: String?,
        paymentDate: Long = System.currentTimeMillis()
    ) {
        db.withTransaction {
            val client = clientDao.getById(clientId) ?: return@withTransaction
            val unpaidInvoices = invoiceDao.getUnpaidInvoicesForClient(tenantId, clientId)
            var remainingAmount = nominal
            
            for (invoice in unpaidInvoices) {
                if (remainingAmount <= 0.0) break
                val totalPaid = paymentDao.sumForInvoice(invoice.id)
                val sisaTagihan = (invoice.totalAmount - totalPaid).coerceAtLeast(0.0)
                if (sisaTagihan <= 0.0) continue
                
                val alloc = if (remainingAmount >= sisaTagihan) sisaTagihan else remainingAmount
                remainingAmount -= alloc
                
                val payment = BmpInvoicePaymentEntity(
                    tenantId = tenantId,
                    invoiceId = invoice.id,
                    paymentDate = paymentDate,
                    paymentAmount = alloc,
                    paymentMethod = paymentMethod,
                    notes = notes ?: "Pembayaran Massal"
                )
                val paymentId = paymentDao.insert(payment)
                
                val newPaidAmount = totalPaid + alloc
                val isFullyPaid = newPaidAmount >= invoice.totalAmount - 0.01
                val newStatus = when {
                    isFullyPaid -> "PAID"
                    invoice.dueDate != null && System.currentTimeMillis() > invoice.dueDate -> "OVERDUE"
                    else -> "PARTIAL"
                }
                invoiceDao.updatePaid(invoice.id, newPaidAmount, newStatus)
            }
            
            if (remainingAmount > 0.0) {
                val newSaldo = client.saldoTitipan + remainingAmount
                clientDao.update(client.copy(saldoTitipan = newSaldo))
            }
            
            cashFlowDao.insert(
                BmpCashFlowEntity(
                    tenantId = tenantId,
                    transactionDate = paymentDate,
                    transactionType = "MASUK",
                    description = "Pembayaran Borongan dari Klien ${client.clientName}",
                    amount = nominal
                )
            )
        }
    }

    private fun autoSlug(number: String): String =
        number.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') +
            "-" + System.currentTimeMillis()
}

/**
 * Concrete fake of [BmpAggregateDao] usable via DI (Hilt can't inject abstract
 * Room DAO methods with default implementations cleanly in v2.6).
 */
@Singleton
class BmpAggregateDaoImpl @Inject constructor() {
    suspend fun createInvoiceWithProducts(
        invoiceDao: BmpInvoiceDao,
        productDao: BmpProductDao,
        invoice: BmpInvoiceEntity,
        products: List<BmpProductEntity>
    ): Long {
        val id = invoiceDao.insert(invoice)
        productDao.insertAll(products.map { it.copy(invoiceId = id) })
        return id
    }
}

@Singleton
class BmpMasterProductRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: BmpMasterProductDao
) {
    fun observe(tenantId: String) = dao.observe(tenantId)
    suspend fun getById(id: Long) = dao.getById(id)

    suspend fun upsert(ctx: Context, p: BmpMasterProductEntity): OnlineWriteResult {
        val isNew = p.id == 0L
        val draft = p.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        val localId = dao.upsert(draft)
        val final = draft.copy(id = if (isNew) localId else draft.id, isSynced = true)

        val result = BmpOnlineWriter.upsertMasterProduct(ctx, final.tenantId, final).toOnlineWriteResult()
        if (result is OnlineWriteResult.Success) {
            dao.upsert(final)
        } else {
            if (isNew) {
                dao.hardDelete(localId)
            }
        }
        return result
    }

    suspend fun delete(ctx: Context, tenantId: String, id: Long): OnlineWriteResult {
        val result = BmpOnlineWriter.deleteMasterProduct(ctx, tenantId, id).toOnlineWriteResult()
        if (result is OnlineWriteResult.Success) dao.softDelete(id)
        return result
    }
}

@Singleton
class BmpCashFlowRepository @Inject constructor(
    private val dao: BmpCashFlowDao
) {
    fun observe(tenantId: String) = dao.observe(tenantId)
    fun totalIn(tenantId: String) = dao.totalIn(tenantId)
    fun totalOut(tenantId: String) = dao.totalOut(tenantId)
    suspend fun insert(e: BmpCashFlowEntity) = dao.insert(e)
    suspend fun delete(id: Long) = dao.softDelete(id)
}

@Singleton
class BmpSettingsRepository @Inject constructor(
    private val dao: BmpSettingsDao
) {
    fun observe(tenantId: String) = dao.observe(tenantId)
    suspend fun get(tenantId: String) = dao.get(tenantId)
    suspend fun upsert(s: BmpSettingsEntity) = dao.upsert(s.copy(updatedAt = System.currentTimeMillis()))
}

@Singleton
class BmpEmployeeRepository @Inject constructor(
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val empDao: BmpEmployeeDao,
    private val payrollDao: BmpPayrollDao,
    private val cashFlowDao: BmpCashFlowDao
) {
    fun observe(tenantId: String) = empDao.observe(tenantId)
    suspend fun upsert(e: BmpEmployeeEntity) = empDao.upsert(e.copy(isSynced = false, updatedAt = System.currentTimeMillis()))
    suspend fun softDelete(id: Long) = empDao.softDelete(id)

    fun observePayrolls(tenantId: String) = payrollDao.observe(tenantId)
    fun observePayrollsForEmployee(empId: Long) = payrollDao.observeForEmployee(empId)

    /**
     * Simpan catatan penggajian. Secara otomatis membuat entri CashFlow KELUAR
     * agar saldo kas real mencerminkan pengeluaran gaji karyawan.
     */
    suspend fun insertPayroll(p: BmpPayrollEntity): String {
        return db.withTransaction {
            payrollDao.insert(p)
            if (p.amount > 0) {
                cashFlowDao.insert(
                    BmpCashFlowEntity(
                        tenantId = p.tenantId,
                        transactionDate = p.paymentDate,
                        transactionType = "KELUAR",
                        description = "Penggajian Karyawan ID ${p.employeeId}",
                        amount = p.amount,
                        paymentRefId = null
                    )
                )
            }
            p.id
        }
    }

    /**
     * Hapus catatan penggajian beserta entri CashFlow terkait.
     */
    suspend fun deletePayroll(id: String) {
        db.withTransaction {
            payrollDao.delete(id)
        }
    }
}

/**
 * Repository untuk Modul Bahan Baku.
 *
 * Interkoneksi:
 * - Save dengan nominal > 0 → otomatis buat BmpCashFlowEntity(KELUAR)
 * - Update → hitung selisih nominal, buat entri penyesuaian kas
 * - Delete → hapus header, items, dan cashflow terkait
 * - totalHarga() → dipakai kalkulasi Simulasi Saldo di Dashboard
 * - totalNominal() → dipakai kalkulasi Saldo Kas Riil di Dashboard
 */
@Singleton
class BmpBahanBakuRepository @Inject constructor(
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val bahanBakuDao: com.posbah.app.data.local.dao.BmpBahanBakuDao,
    private val itemDao: com.posbah.app.data.local.dao.BmpBahanBakuItemDao,
    private val cashFlowDao: BmpCashFlowDao
) {
    fun observe(tenantId: String) = bahanBakuDao.observe(tenantId)
    fun observeItems(bahanBakuId: Long) = itemDao.observeByBahanBaku(bahanBakuId)
    fun totalHarga(tenantId: String) = bahanBakuDao.totalHarga(tenantId)
    fun totalNominal(tenantId: String) = bahanBakuDao.totalNominal(tenantId)
    suspend fun getById(id: Long) = bahanBakuDao.getById(id)
    suspend fun getByTagihan(tenantId: String, noTagihan: String) = bahanBakuDao.getByTagihan(tenantId, noTagihan)
    suspend fun updateHeaderOnly(entry: com.posbah.app.data.local.entities.BmpBahanBakuEntity) = bahanBakuDao.update(entry)
    suspend fun getLatestRate(tenantId: String, jenisBahan: String) =
        itemDao.getLatestRate(tenantId, jenisBahan)

    /** Simpan transaksi baru: VPS dulu → Room jika berhasil. */
    suspend fun save(
        context: Context,
        header: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): Pair<Long, OnlineWriteResult> {
        val total = items.sumOf { it.kuantitas * it.rate }
        val finalHeader = header.copy(
            totalHarga = total,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )

        // Save to Room first
        val (id, cfId) = db.withTransaction {
            val newId = bahanBakuDao.insert(finalHeader)
            val mappedItems = items.map { it.copy(bahanBakuId = newId, isSynced = false) }
            itemDao.insertAll(mappedItems)

            var newCfId: Long? = null
            if (finalHeader.nominal > 0) {
                val cf = BmpCashFlowEntity(
                    tenantId = finalHeader.tenantId,
                    transactionDate = finalHeader.tanggal,
                    transactionType = "KELUAR",
                    description = "Pembayaran Bahan Baku - Tagihan: ${finalHeader.noTagihan}",
                    amount = finalHeader.nominal,
                    paymentRefId = newId,
                    isSynced = false
                )
                newCfId = cashFlowDao.insert(cf)
            }
            Pair(newId, newCfId)
        }

        // Get saved header with Room-generated ID
        val savedHeader = bahanBakuDao.getById(id) ?: return Pair(-1L, OnlineWriteResult.Error("Gagal simpan bahan baku lokal"))
        val finalHeaderWithId = savedHeader.copy(isSynced = true)

        // Push header to VPS
        val vpsHeader = BmpOnlineWriter.upsertBahanBaku(context, finalHeaderWithId.tenantId, finalHeaderWithId).toOnlineWriteResult()
        if (vpsHeader !is OnlineWriteResult.Success) {
            // Rollback/delete
            db.withTransaction {
                itemDao.deleteByBahanBaku(id)
                if (cfId != null) {
                    cashFlowDao.hardDelete(cfId)
                }
                bahanBakuDao.hardDelete(id)
            }
            return Pair(-1L, vpsHeader)
        }

        // Mark header synced
        bahanBakuDao.markSynced(id)

        // Push items to VPS with correct bahanBakuId
        val savedItems = itemDao.listByBahanBaku(id)
        val itemsRes = BmpOnlineWriter.upsertBahanBakuItems(context, finalHeader.tenantId, savedItems)
        if (itemsRes is SupabaseSyncManager.SyncResult.Success) {
            savedItems.forEach { itemDao.markSynced(it.id) }
        }

        // Push cashflow if any
        if (cfId != null) {
            val savedCf = cashFlowDao.getByPaymentRefId(id)
            if (savedCf != null) {
                val cfRes = BmpOnlineWriter.upsertCashFlow(context, finalHeader.tenantId, savedCf)
                if (cfRes is SupabaseSyncManager.SyncResult.Success) {
                    cashFlowDao.markSynced(cfId)
                }
            }
        }

        return Pair(id, OnlineWriteResult.Success)
    }

    /** Update transaksi: VPS dulu → Room jika berhasil. */
    suspend fun update(
        context: Context,
        oldNominal: Double,
        header: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): OnlineWriteResult {
        val total = items.sumOf { it.kuantitas * it.rate }
        val finalHeader = header.copy(
            totalHarga = total,
            isSynced = true,
            updatedAt = System.currentTimeMillis()
        )
        val vpsResult = BmpOnlineWriter.upsertBahanBaku(context, finalHeader.tenantId, finalHeader).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        val cfId = db.withTransaction {
            bahanBakuDao.update(finalHeader)
            itemDao.deleteByBahanBaku(finalHeader.id)
            val mappedItems = items.map { it.copy(bahanBakuId = finalHeader.id, isSynced = false) }
            itemDao.insertAll(mappedItems)

            var insertedCfId: Long? = null
            val diff = finalHeader.nominal - oldNominal
            if (diff > 0) {
                val cf = BmpCashFlowEntity(
                    tenantId = finalHeader.tenantId,
                    transactionDate = System.currentTimeMillis(),
                    transactionType = "KELUAR",
                    description = "Penyesuaian Bahan Baku - Tagihan: ${finalHeader.noTagihan}",
                    amount = diff,
                    paymentRefId = finalHeader.id,
                    isSynced = false
                )
                insertedCfId = cashFlowDao.insert(cf)
            }
            insertedCfId
        }

        val savedItems = itemDao.listByBahanBaku(finalHeader.id)
        val itemsRes = BmpOnlineWriter.upsertBahanBakuItems(context, finalHeader.tenantId, savedItems)
        if (itemsRes is SupabaseSyncManager.SyncResult.Success) {
            savedItems.forEach { itemDao.markSynced(it.id) }
        }

        if (cfId != null) {
            val savedCf = cashFlowDao.getAll().find { it.id == cfId }
            if (savedCf != null) {
                val cfRes = BmpOnlineWriter.upsertCashFlow(context, finalHeader.tenantId, savedCf)
                if (cfRes is SupabaseSyncManager.SyncResult.Success) {
                    cashFlowDao.markSynced(cfId)
                }
            }
        }

        return OnlineWriteResult.Success
    }

    suspend fun payDebt(context: Context, tenantId: String, id: Long, amountPaidNow: Double): OnlineWriteResult {
        val header = getById(id) ?: return OnlineWriteResult.Error("Data tidak ditemukan")
        val newNominal = header.nominal + amountPaidNow
        val updatedHeader = header.copy(nominal = newNominal, isSynced = true)
        val vpsResult = BmpOnlineWriter.upsertBahanBaku(context, tenantId, updatedHeader).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        bahanBakuDao.update(updatedHeader)
        val cf = BmpCashFlowEntity(
            tenantId = header.tenantId,
            transactionDate = System.currentTimeMillis(),
            transactionType = "KELUAR",
            description = "Pembayaran Hutang Supplier - Tagihan: ${header.noTagihan}",
            amount = amountPaidNow,
            paymentRefId = id,
            isSynced = false
        )
        val cfId = cashFlowDao.insert(cf)
        val finalCf = cf.copy(id = cfId)
        val cfRes = BmpOnlineWriter.upsertCashFlow(context, tenantId, finalCf)
        if (cfRes is SupabaseSyncManager.SyncResult.Success) {
            cashFlowDao.markSynced(cfId)
        }
        return OnlineWriteResult.Success
    }

    /** Soft-delete transaksi beserta semua item dan entri kas terkait: VPS dulu. */
    suspend fun delete(context: Context, tenantId: String, id: Long): OnlineWriteResult {
        val result = BmpOnlineWriter.deleteBahanBaku(context, tenantId, id).toOnlineWriteResult()
        if (result !is OnlineWriteResult.Success) return result
        db.withTransaction {
            cashFlowDao.softDeleteByPaymentRefId(id)
            itemDao.softDeleteByBahanBaku(id)
            bahanBakuDao.softDelete(id)
        }
        return OnlineWriteResult.Success
    }
}

@Singleton
class PrintSettingsRepository @Inject constructor(
    private val dao: PrintSettingsDao
) {
    fun observe(tenantId: String, moduleKey: String): Flow<PrintSettingsEntity?> =
        dao.observe(tenantId, moduleKey)

    suspend fun get(tenantId: String, moduleKey: String): PrintSettingsEntity? =
        dao.get(tenantId, moduleKey)

    suspend fun upsert(settings: PrintSettingsEntity) = dao.upsert(settings)
}

@Singleton
class BmpStockRepository @Inject constructor(
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val stockDao: BmpProductStockDao,
    private val ledgerDao: BmpStockLedgerDao,
    private val itemDao: com.posbah.app.data.local.dao.BmpBahanBakuItemDao,
    private val productionLogDao: BmpProductionLogDao
) {
    fun observeStocks(tenantId: String): Flow<List<BmpProductStockEntity>> = stockDao.observeAll(tenantId)
    
    fun observeLedger(tenantId: String, productId: Long): Flow<List<BmpStockLedgerEntity>> =
        ledgerDao.observeByProduct(tenantId, productId)

    fun observeAllLedger(tenantId: String): Flow<List<BmpStockLedgerEntity>> =
        ledgerDao.observeAll(tenantId)

    suspend fun getStockByProductId(tenantId: String, productId: Long): BmpProductStockEntity? =
        stockDao.getByProductId(tenantId, productId)

    suspend fun getRawMaterialStock(tenantId: String, jenisBahan: String): Double {
        val purchased = itemDao.sumPurchasedBahanBaku(tenantId, jenisBahan)
        val used = productionLogDao.sumUsedBahanBaku(tenantId, jenisBahan)
        return (purchased - used).coerceAtLeast(0.0)
    }

    suspend fun adjustStock(
        context: Context,
        tenantId: String,
        productId: Long,
        change: Double,
        mutationType: String,
        referenceId: Long,
        notes: String? = null
    ) {
        val (stockEntity, ledger) = db.withTransaction {
            val existing = stockDao.getByProductId(tenantId, productId)
            val newQty = (existing?.quantity ?: 0.0) + change
            val stockEntity: BmpProductStockEntity = if (existing == null) {
                val newStock = BmpProductStockEntity(
                    tenantId = tenantId,
                    masterProductId = productId,
                    quantity = newQty,
                    minStockAlert = 0.0,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                val idVal = stockDao.upsert(newStock)
                newStock.copy(id = idVal)
            } else {
                stockDao.updateQuantity(tenantId, productId, newQty)
                existing.copy(quantity = newQty, isSynced = false, updatedAt = System.currentTimeMillis())
            }

            val ledger = BmpStockLedgerEntity(
                tenantId = tenantId,
                masterProductId = productId,
                referenceId = referenceId,
                mutationType = mutationType,
                quantityChange = change,
                finalStock = newQty,
                notes = notes,
                isSynced = false,
                createdAt = System.currentTimeMillis()
            )
            val ledgerId = ledgerDao.insert(ledger)
            val finalLedger = ledger.copy(id = ledgerId)
            Pair(stockEntity, finalLedger)
        }

        // Push to VPS in background (non-blocking, best-effort after room commit)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val stockRes = BmpOnlineWriter.upsertProductStock(context, tenantId, stockEntity)
                if (stockRes is SupabaseSyncManager.SyncResult.Success) {
                    stockDao.markSynced(stockEntity.id)
                }
                val ledgerRes = BmpOnlineWriter.upsertStockLedger(context, tenantId, ledger)
                if (ledgerRes is SupabaseSyncManager.SyncResult.Success) {
                    ledgerDao.markSynced(ledger.id)
                }
            } catch (_: Exception) {}
        }
    }
}

@Singleton
class BmpProductionLogRepository @Inject constructor(
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val logDao: BmpProductionLogDao,
    private val stockRepo: BmpStockRepository
) {
    fun observeAll(tenantId: String): Flow<List<BmpProductionLogEntity>> = logDao.observeAll(tenantId)

    suspend fun getById(id: Long) = logDao.getById(id)

    suspend fun addProductionLog(context: Context, log: BmpProductionLogEntity): OnlineWriteResult {
        val logDraft = log.copy(isSynced = false, createdAt = System.currentTimeMillis())
        val insertedId = db.withTransaction {
            val idVal = logDao.upsert(logDraft)
            stockRepo.adjustStock(
                context = context,
                tenantId = log.tenantId,
                productId = log.masterProductId,
                change = log.quantityProduced,
                mutationType = "PRODUKSI",
                referenceId = idVal,
                notes = "Hasil Produksi Harian"
            )
            idVal
        }

        val savedLog = logDao.getById(insertedId) ?: return OnlineWriteResult.Error("Gagal menyimpan log produksi lokal")
        val finalLogWithId = savedLog.copy(isSynced = true)

        val vpsResult = BmpOnlineWriter.upsertProductionLog(context, finalLogWithId.tenantId, finalLogWithId).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) {
            db.withTransaction {
                stockRepo.adjustStock(
                    context = context,
                    tenantId = log.tenantId,
                    productId = log.masterProductId,
                    change = -log.quantityProduced,
                    mutationType = "PRODUKSI",
                    referenceId = insertedId,
                    notes = "Batal Produksi (VPS Gagal)"
                )
                logDao.hardDelete(insertedId)
            }
            return vpsResult
        }

        logDao.markSynced(insertedId)
        return OnlineWriteResult.Success
    }

    suspend fun deleteProductionLog(context: Context, tenantId: String, log: BmpProductionLogEntity): OnlineWriteResult {
        val vpsResult = BmpOnlineWriter.deleteProductionLog(context, tenantId, log.id).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) return vpsResult

        db.withTransaction {
            logDao.softDelete(log.id)
            stockRepo.adjustStock(
                context = context,
                tenantId = log.tenantId,
                productId = log.masterProductId,
                change = -log.quantityProduced,
                mutationType = "PRODUKSI",
                referenceId = log.id,
                notes = "Pembatalan/Penghapusan Produksi"
            )
        }
        return OnlineWriteResult.Success
    }
}
