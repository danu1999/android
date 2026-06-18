package com.posbah.app.data.repository

import com.posbah.app.data.local.dao.BmpCashFlowDao
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
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction
import com.posbah.app.data.local.dao.PrintSettingsDao
import com.posbah.app.data.local.entities.PrintSettingsEntity

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
    suspend fun upsert(client: BmpClientEntity) = dao.upsert(
        client.copy(
            uniqueID = client.uniqueID ?: UUID.randomUUID().toString(),
            slug = client.slug ?: client.clientName.toSlug(),
            updatedAt = System.currentTimeMillis()
        )
    )

    /**
     * Cascade-delete klien beserta SEMUA invoice, produk, pembayaran, dan cashflow terkait.
     * Soft-delete di lokal (isDeleted=1), hard-delete di server dilakukan oleh SyncManager.
     */
    suspend fun delete(id: Long) {
        db.withTransaction {
            // 1. Dapatkan semua invoice milik klien ini
            val invoices = invoiceDao.getByClientId(id)
            for (invoice in invoices) {
                // Soft-delete semua pembayaran invoice
                val payments = paymentDao.listAllForInvoice(invoice.id)
                for (payment in payments) {
                    cashFlowDao.softDeleteByPaymentRefId(payment.id)
                }
                paymentDao.softDeleteByInvoice(invoice.id)
                // Soft-delete produk invoice
                productDao.softDeleteByInvoice(invoice.id)
                // Soft-delete cashflow keluar (barang khusus invoice)
                cashFlowDao.deleteExitsForInvoice(invoice.number)
            }
            // 2. Soft-delete semua invoice klien
            invoiceDao.softDeleteByClientId(id)
            // 3. Soft-delete klien
            dao.softDelete(id)
        }
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
    private val aggregate: BmpAggregateDaoImpl
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

    suspend fun createInvoice(
        invoice: BmpInvoiceEntity,
        products: List<BmpProductEntity>
    ): Long {
        val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
        val totalPaid = invoice.paidAmount
        val newStatus = when {
            totalPaid >= total - 0.01 -> "PAID"
            totalPaid > 0 -> "PARTIAL"
            else -> "UNPAID"
        }
        val final = invoice.copy(
            totalAmount = total,
            status = newStatus,
            slug = invoice.slug.ifBlank { autoSlug(invoice.number) }
        )
        return db.withTransaction {
            val id = invoiceDao.insert(final)
            val mappedProducts = products.map { it.copy(invoiceId = id) }
            productDao.insertAll(mappedProducts)

            if (totalPaid > 0) {
                val paymentId = paymentDao.insert(
                    BmpInvoicePaymentEntity(
                        tenantId = invoice.tenantId,
                        invoiceId = id,
                        paymentDate = System.currentTimeMillis(),
                        paymentAmount = totalPaid,
                        paymentMethod = "CASH",
                        notes = "Uang muka saat pembuatan Invoice"
                    )
                )
                cashFlowDao.insert(
                    BmpCashFlowEntity(
                        tenantId = invoice.tenantId,
                        transactionDate = System.currentTimeMillis(),
                        transactionType = "MASUK",
                        description = "Pembayaran Invoice ${final.number}",
                        amount = totalPaid,
                        paymentRefId = paymentId
                    )
                )
            }
            
            // Check for isKhusus items with hargaBeli > 0
            for (prod in mappedProducts) {
                if (prod.isKhusus && prod.hargaBeli > 0) {
                    cashFlowDao.insert(
                        BmpCashFlowEntity(
                            tenantId = prod.tenantId,
                            transactionDate = System.currentTimeMillis(),
                            transactionType = "KELUAR",
                            description = "Pembelian barang khusus untuk Faktur ${final.number}",
                            amount = prod.hargaBeli
                        )
                    )
                }
            }
            id
        }
    }

    suspend fun updateInvoice(invoice: BmpInvoiceEntity, products: List<BmpProductEntity>) {
        db.withTransaction {
            productDao.deleteByInvoice(invoice.id)
            val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
            
            val totalPaid = paymentDao.sumForInvoice(invoice.id)
            val newStatus = when {
                totalPaid >= total - 0.01 -> "PAID"
                totalPaid > 0 -> {
                    if (invoice.dueDate != null && System.currentTimeMillis() > invoice.dueDate) "OVERDUE" else "PARTIAL"
                }
                else -> {
                    if (invoice.dueDate != null && System.currentTimeMillis() > invoice.dueDate) "OVERDUE" else "UNPAID"
                }
            }
            
            invoiceDao.update(
                invoice.copy(
                    totalAmount = total,
                    paidAmount = totalPaid,
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
            
            val mappedProducts = products.map { it.copy(invoiceId = invoice.id) }
            productDao.insertAll(mappedProducts)
            
            // Re-sync cash flow exits for this invoice
            cashFlowDao.deleteExitsForInvoice(invoice.number)
            for (prod in mappedProducts) {
                if (prod.isKhusus && prod.hargaBeli > 0) {
                    cashFlowDao.insert(
                        BmpCashFlowEntity(
                            tenantId = prod.tenantId,
                            transactionDate = System.currentTimeMillis(),
                            transactionType = "KELUAR",
                            description = "Pembelian barang khusus untuk Faktur ${invoice.number}",
                            amount = prod.hargaBeli
                        )
                    )
                }
            }
        }
    }

    /**
     * Cascade-delete invoice beserta semua produk, pembayaran, dan cashflow terkait.
     * Operasi ini menggunakan soft-delete (isDeleted=1) sehingga SyncManager dapat
     * mengirim DELETE ke server sebelum hard-delete lokal.
     */
    suspend fun deleteInvoice(id: Long) {
        db.withTransaction {
            val invoice = invoiceDao.getById(id)
                ?: invoiceDao.getAllForTenant("").find { it.id == id } // cek termasuk yg deleted
            if (invoice != null) {
                // 1. Soft-delete cashflow keluar untuk barang khusus
                cashFlowDao.deleteExitsForInvoice(invoice.number)
            }

            // 2. Ambil semua pembayaran (termasuk yg sudah deleted)
            val payments = paymentDao.listAllForInvoice(id)
            for (payment in payments) {
                // Soft-delete cashflow terkait pembayaran ini
                cashFlowDao.softDeleteByPaymentRefId(payment.id)
            }
            // 3. Soft-delete semua pembayaran
            paymentDao.softDeleteByInvoice(id)

            // 4. Soft-delete semua produk invoice
            productDao.softDeleteByInvoice(id)

            // 5. Soft-delete invoice
            invoiceDao.softDelete(id)
        }
    }

    suspend fun markAsUnsynced(id: Long) {
        invoiceDao.markUnsynced(id)
    }

    suspend fun saveReceiverSignature(
        invoiceId: Long,
        signaturePath: String?,
        signatureUrl: String?,
        receiverName: String
    ) {
        val invoice = invoiceDao.getById(invoiceId) ?: return
        invoiceDao.update(
            invoice.copy(
                receiverSignaturePath = signaturePath,
                receiverSignatureUrl = signatureUrl,
                receiverNameActual = receiverName,
                updatedAt = System.currentTimeMillis()
            )
        )
        invoice.clientId?.let { cId ->
            val client = clientDao.getById(cId)
            if (client != null) {
                clientDao.update(
                    client.copy(
                        receiverSignatureUrl = signatureUrl ?: client.receiverSignatureUrl,
                        receiverNameActual = if (receiverName.isNotEmpty()) receiverName else client.receiverNameActual,
                        isSynced = false,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
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
        tenantId: String,
        invoiceId: Long,
        paymentAmount: Double,
        paymentMethod: String,
        notes: String?,
        paymentDate: Long = System.currentTimeMillis()
    ) {
        db.withTransaction {
            val invoice = invoiceDao.getById(invoiceId) ?: return@withTransaction
            val paymentId = paymentDao.insert(
                BmpInvoicePaymentEntity(
                    tenantId = tenantId,
                    invoiceId = invoiceId,
                    paymentDate = paymentDate,
                    paymentAmount = paymentAmount,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
            )
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
            invoiceDao.updatePaid(invoiceId, totalPaid, newStatus)

            cashFlowDao.insert(
                BmpCashFlowEntity(
                    tenantId = tenantId,
                    transactionDate = paymentDate,
                    transactionType = "MASUK",
                    description = "Pembayaran Invoice ${invoice.number}",
                    amount = paymentAmount,
                    paymentRefId = paymentId
                )
            )
        }
    }

    suspend fun editPayment(
        tenantId: String,
        paymentId: Long,
        newAmount: Double,
        paymentMethod: String,
        notes: String?,
        paymentDate: Long = System.currentTimeMillis()
    ) {
        db.withTransaction {
            val payment = paymentDao.getById(paymentId) ?: return@withTransaction
            val invoiceId = payment.invoiceId
            val invoice = invoiceDao.getById(invoiceId) ?: return@withTransaction
            
            paymentDao.update(
                payment.copy(
                    paymentAmount = newAmount,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    paymentDate = paymentDate
                )
            )
            
            val existingCf = cashFlowDao.getByPaymentRefId(paymentId)
            if (existingCf != null) {
                cashFlowDao.update(
                    existingCf.copy(
                        amount = newAmount,
                        transactionDate = paymentDate,
                        description = "Pembayaran Invoice ${invoice.number}"
                    )
                )
            } else {
                cashFlowDao.insert(
                    BmpCashFlowEntity(
                        tenantId = tenantId,
                        transactionDate = paymentDate,
                        transactionType = "MASUK",
                        description = "Pembayaran Invoice ${invoice.number}",
                        amount = newAmount,
                        paymentRefId = paymentId
                    )
                )
            }
            
            recalculateInvoiceStatus(invoiceId)
        }
    }

    suspend fun deletePayment(paymentId: Long) {
        db.withTransaction {
            val payment = paymentDao.getById(paymentId) ?: return@withTransaction
            val invoiceId = payment.invoiceId

            // Soft-delete cashflow terkait
            cashFlowDao.softDeleteByPaymentRefId(paymentId)
            // Soft-delete payment
            paymentDao.softDelete(paymentId)

            recalculateInvoiceStatus(invoiceId)
        }
    }

    private suspend fun recalculateInvoiceStatus(invoiceId: Long) {
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
        invoiceDao.updatePaid(invoiceId, totalPaid, newStatus)
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
    private val dao: BmpMasterProductDao
) {
    fun observe(tenantId: String) = dao.observe(tenantId)
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun upsert(p: BmpMasterProductEntity) = dao.upsert(p.copy(updatedAt = System.currentTimeMillis()))
    suspend fun delete(id: Long) = dao.softDelete(id)
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
    suspend fun upsert(e: BmpEmployeeEntity) = empDao.upsert(e.copy(updatedAt = System.currentTimeMillis()))
    suspend fun softDelete(id: Long) = empDao.softDelete(id)

    fun observePayrolls(tenantId: String) = payrollDao.observe(tenantId)
    fun observePayrollsForEmployee(empId: Long) = payrollDao.observeForEmployee(empId)

    /**
     * Simpan catatan penggajian. Secara otomatis membuat entri CashFlow KELUAR
     * agar saldo kas real mencerminkan pengeluaran gaji karyawan.
     */
    suspend fun insertPayroll(p: BmpPayrollEntity): Long {
        return db.withTransaction {
            val id = payrollDao.insert(p)
            if (p.amount > 0) {
                cashFlowDao.insert(
                    BmpCashFlowEntity(
                        tenantId = p.tenantId,
                        transactionDate = p.paymentDate,
                        transactionType = "KELUAR",
                        description = "Penggajian Karyawan ID ${p.employeeId}",
                        amount = p.amount,
                        paymentRefId = id
                    )
                )
            }
            id
        }
    }

    /**
     * Hapus catatan penggajian beserta entri CashFlow terkait.
     */
    suspend fun deletePayroll(id: Long) {
        db.withTransaction {
            cashFlowDao.deleteByPaymentRefId(id)
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
    suspend fun getLatestRate(tenantId: String, jenisBahan: String) =
        itemDao.getLatestRate(tenantId, jenisBahan)

    /** Simpan transaksi baru (header + items). Jika nominal > 0, buat kas KELUAR otomatis. */
    suspend fun save(
        header: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): Long {
        val total = items.sumOf { it.kuantitas * it.rate }
        val finalHeader = header.copy(
            totalHarga = total,
            updatedAt = System.currentTimeMillis()
        )
        return db.withTransaction {
            val id = bahanBakuDao.insert(finalHeader)
            itemDao.insertAll(items.map { it.copy(bahanBakuId = id) })

            if (finalHeader.nominal > 0) {
                cashFlowDao.insert(
                    BmpCashFlowEntity(
                        tenantId = finalHeader.tenantId,
                        transactionDate = finalHeader.tanggal,
                        transactionType = "KELUAR",
                        description = "Pembayaran Bahan Baku - Tagihan: ${finalHeader.noTagihan}",
                        amount = finalHeader.nominal,
                        paymentRefId = id
                    )
                )
            }
            id
        }
    }

    /**
     * Update transaksi. Hitung selisih nominal terhadap nilai lama.
     * Jika ada selisih positif → buat entri kas KELUAR tambahan.
     */
    suspend fun update(
        oldNominal: Double,
        header: com.posbah.app.data.local.entities.BmpBahanBakuEntity,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ) {
        val total = items.sumOf { it.kuantitas * it.rate }
        val finalHeader = header.copy(
            totalHarga = total,
            updatedAt = System.currentTimeMillis()
        )
        db.withTransaction {
            bahanBakuDao.update(finalHeader)
            itemDao.deleteByBahanBaku(finalHeader.id)
            itemDao.insertAll(items.map { it.copy(bahanBakuId = finalHeader.id) })

            val diff = finalHeader.nominal - oldNominal
            if (diff > 0) {
                cashFlowDao.insert(
                    BmpCashFlowEntity(
                        tenantId = finalHeader.tenantId,
                        transactionDate = System.currentTimeMillis(),
                        transactionType = "KELUAR",
                        description = "Penyesuaian Bahan Baku - Tagihan: ${finalHeader.noTagihan}",
                        amount = diff,
                        paymentRefId = finalHeader.id
                    )
                )
            }
        }
    }

    suspend fun payDebt(id: Long, amountPaidNow: Double) {
        db.withTransaction {
            val header = getById(id) ?: return@withTransaction
            val newNominal = header.nominal + amountPaidNow
            bahanBakuDao.update(header.copy(nominal = newNominal))
            
            cashFlowDao.insert(
                BmpCashFlowEntity(
                    tenantId = header.tenantId,
                    transactionDate = System.currentTimeMillis(),
                    transactionType = "KELUAR",
                    description = "Pembayaran Hutang Supplier - Tagihan: ${header.noTagihan}",
                    amount = amountPaidNow,
                    paymentRefId = id
                )
            )
        }
    }

    /** Soft-delete transaksi beserta semua item dan entri kas terkait. */
    suspend fun delete(id: Long) {
        db.withTransaction {
            // 1. Soft-delete cashflow terkait (via paymentRefId = bahanBakuId)
            cashFlowDao.softDeleteByPaymentRefId(id)
            // 2. Soft-delete semua item bahan baku
            itemDao.softDeleteByBahanBaku(id)
            // 3. Soft-delete header bahan baku
            bahanBakuDao.softDelete(id)
        }
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
