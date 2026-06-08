package com.posbah.app.data.repository

import com.posbah.app.data.local.dao.BmpAggregateDao
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

@Singleton
class BmpClientRepository @Inject constructor(
    private val dao: BmpClientDao
) {
    fun observe(tenantId: String): Flow<List<BmpClientEntity>> = dao.observe(tenantId)
    fun search(tenantId: String, q: String): Flow<List<BmpClientEntity>> = dao.search(tenantId, q)
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun upsert(client: BmpClientEntity) = dao.upsert(
        client.copy(
            updatedAt = System.currentTimeMillis(),
            slug = client.slug ?: client.clientName.toSlug()
        )
    )
    suspend fun delete(id: Long) = dao.delete(id)
    fun count(tenantId: String) = dao.count(tenantId)

    private fun String.toSlug(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifEmpty { UUID.randomUUID().toString().take(8) }
}

@Singleton
class BmpInvoiceRepository @Inject constructor(
    private val invoiceDao: BmpInvoiceDao,
    private val productDao: BmpProductDao,
    private val paymentDao: BmpPaymentDao,
    private val cashFlowDao: BmpCashFlowDao,
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
        val final = invoice.copy(totalAmount = total, slug = invoice.slug.ifBlank { autoSlug(invoice.number) })
        return aggregate.createInvoiceWithProducts(invoiceDao, productDao, final, products)
    }

    suspend fun updateInvoice(invoice: BmpInvoiceEntity, products: List<BmpProductEntity>) {
        productDao.deleteByInvoice(invoice.id)
        val total = products.sumOf { it.price * it.quantity * it.jumlahLusin }
        invoiceDao.update(invoice.copy(totalAmount = total, updatedAt = System.currentTimeMillis()))
        productDao.insertAll(products.map { it.copy(invoiceId = invoice.id) })
    }

    suspend fun deleteInvoice(id: Long) {
        productDao.deleteByInvoice(id)
        invoiceDao.delete(id)
    }

    /**
     * Record a payment. Updates running paid amount, derives new status,
     * and emits a cash-flow entry.
     */
    suspend fun recordPayment(
        tenantId: String,
        invoiceId: Long,
        paymentAmount: Double,
        paymentMethod: String,
        notes: String?,
        paymentDate: Long = System.currentTimeMillis()
    ) {
        val invoice = invoiceDao.getById(invoiceId) ?: return
        paymentDao.insert(
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
            totalPaid > 0 -> "PARTIAL"
            else -> invoice.status
        }
        invoiceDao.updatePaid(invoiceId, totalPaid, newStatus)

        cashFlowDao.insert(
            BmpCashFlowEntity(
                tenantId = tenantId,
                transactionDate = paymentDate,
                transactionType = "MASUK",
                description = "Pembayaran Invoice ${invoice.number}",
                amount = paymentAmount
            )
        )
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
    suspend fun delete(id: Long) = dao.delete(id)
}

@Singleton
class BmpCashFlowRepository @Inject constructor(
    private val dao: BmpCashFlowDao
) {
    fun observe(tenantId: String) = dao.observe(tenantId)
    fun totalIn(tenantId: String) = dao.totalIn(tenantId)
    fun totalOut(tenantId: String) = dao.totalOut(tenantId)
    suspend fun insert(e: BmpCashFlowEntity) = dao.insert(e)
    suspend fun delete(id: Long) = dao.delete(id)
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
    private val empDao: BmpEmployeeDao,
    private val payrollDao: BmpPayrollDao
) {
    fun observe(tenantId: String) = empDao.observe(tenantId)
    suspend fun upsert(e: BmpEmployeeEntity) = empDao.upsert(e.copy(updatedAt = System.currentTimeMillis()))
    suspend fun softDelete(id: Long) = empDao.softDelete(id)

    fun observePayrolls(tenantId: String) = payrollDao.observe(tenantId)
    fun observePayrollsForEmployee(empId: Long) = payrollDao.observeForEmployee(empId)
    suspend fun insertPayroll(p: BmpPayrollEntity) = payrollDao.insert(p)
    suspend fun deletePayroll(id: Long) = payrollDao.delete(id)
}
