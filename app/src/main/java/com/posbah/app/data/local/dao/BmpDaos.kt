package com.posbah.app.data.local.dao

// ─────────────────────────────────────────────────────────────────────────────
// BmpDaos.kt — Full Online mode STUB
// Semua Room @Dao interface diganti concrete no-op stub class.
// Tidak ada SQLite query. Semua method return empty/0/null.
// ─────────────────────────────────────────────────────────────────────────────

import com.posbah.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BmpClientDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpClientEntity>> = emptyFlow()
    suspend fun getById(id: Long): BmpClientEntity? = null
    fun search(tenantId: String, query: String): Flow<List<BmpClientEntity>> = emptyFlow()
    suspend fun upsert(client: BmpClientEntity): Long = 0L
    suspend fun update(client: BmpClientEntity) {}
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis()) {}
    suspend fun getAll(tenantId: String): List<BmpClientEntity> = emptyList()
    suspend fun getAll(): List<BmpClientEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
    fun count(tenantId: String): Flow<Int> = MutableStateFlow(0)
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
}

@Singleton
class BmpInvoiceDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpInvoiceEntity>> = emptyFlow()
    suspend fun getById(id: Long): BmpInvoiceEntity? = null
    suspend fun getBySlug(slug: String): BmpInvoiceEntity? = null
    fun observeByStatus(tenantId: String, status: String): Flow<List<BmpInvoiceEntity>> = emptyFlow()
    suspend fun insert(invoice: BmpInvoiceEntity): Long = 0L
    suspend fun upsert(invoice: BmpInvoiceEntity): Long = 0L
    suspend fun update(invoice: BmpInvoiceEntity) {}
    suspend fun setStatus(id: Long, status: String, ts: Long = System.currentTimeMillis()) {}
    suspend fun updatePaid(id: Long, paid: Double, status: String, ts: Long = System.currentTimeMillis()) {}
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis()) {}
    suspend fun softDeleteByClient(clientId: Long, ts: Long = System.currentTimeMillis()) {}
    suspend fun getAll(tenantId: String): List<BmpInvoiceEntity> = emptyList()
    suspend fun getAll(): List<BmpInvoiceEntity> = emptyList()
    suspend fun getByClientId(clientId: Long): List<BmpInvoiceEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
    suspend fun hardDeleteByClientId(clientId: Long) {}
    fun count(tenantId: String): Flow<Int> = MutableStateFlow(0)
    fun totalAmount(tenantId: String): Flow<Double?> = MutableStateFlow(null)
    fun totalPaid(tenantId: String): Flow<Double?> = MutableStateFlow(null)
    fun totalOutstanding(tenantId: String): Flow<Double?> = MutableStateFlow(null)
    suspend fun sumForInvoice(invoiceId: Long): Double = 0.0
}

@Singleton
class BmpProductDao @Inject constructor() {
    fun observeByInvoice(invoiceId: Long): Flow<List<BmpProductEntity>> = emptyFlow()
    suspend fun listByInvoice(invoiceId: Long): List<BmpProductEntity> = emptyList()
    suspend fun getById(id: Long): BmpProductEntity? = null
    suspend fun insertAll(products: List<BmpProductEntity>) {}
    suspend fun upsert(product: BmpProductEntity): Long = 0L
    suspend fun deleteByInvoice(invoiceId: Long) {}
    suspend fun getAll(tenantId: String): List<BmpProductEntity> = emptyList()
    suspend fun getAll(): List<BmpProductEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
}

@Singleton
class BmpMasterProductDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpMasterProductEntity>> = emptyFlow()
    suspend fun getById(id: Long): BmpMasterProductEntity? = null
    suspend fun upsert(product: BmpMasterProductEntity): Long = 0L
    suspend fun update(product: BmpMasterProductEntity) {}
    suspend fun softDelete(id: Long) {}
    suspend fun getAll(tenantId: String): List<BmpMasterProductEntity> = emptyList()
    suspend fun getAll(): List<BmpMasterProductEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
}

@Singleton
class BmpPaymentDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpInvoicePaymentEntity>> = emptyFlow()
    fun observeForInvoice(invoiceId: Long): Flow<List<BmpInvoicePaymentEntity>> = emptyFlow()
    suspend fun listAllForInvoice(invoiceId: Long): List<BmpInvoicePaymentEntity> = emptyList()
    suspend fun getById(id: Long): BmpInvoicePaymentEntity? = null
    suspend fun insert(payment: BmpInvoicePaymentEntity): Long = 0L
    suspend fun upsert(payment: BmpInvoicePaymentEntity): Long = 0L
    suspend fun getAll(tenantId: String): List<BmpInvoicePaymentEntity> = emptyList()
    suspend fun getAll(): List<BmpInvoicePaymentEntity> = emptyList()
    suspend fun sumForInvoice(invoiceId: Long): Double = 0.0
    suspend fun markSynced(id: Long) {}
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
    suspend fun deleteByInvoice(invoiceId: Long) {}
}

@Singleton
class BmpCashFlowDao @Inject constructor() {
    fun observeAll(tenantId: String): Flow<List<BmpCashFlowEntity>> = emptyFlow()
    suspend fun getAll(): List<BmpCashFlowEntity> = emptyList()
    suspend fun getAll(tenantId: String): List<BmpCashFlowEntity> = emptyList()
    suspend fun getById(id: Long): BmpCashFlowEntity? = null
    suspend fun insert(cf: BmpCashFlowEntity): Long = 0L
    suspend fun upsert(cf: BmpCashFlowEntity): Long = 0L
    suspend fun update(cf: BmpCashFlowEntity) {}
    suspend fun softDelete(id: Long) {}
    suspend fun markSynced(id: Long) {}
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
    suspend fun hardDeleteByPaymentRefId(paymentRefId: Long) {}
    suspend fun deleteExitsForInvoice(invoiceNumber: String) {}
    suspend fun hardDeleteExitsForInvoice(invoiceNumber: String) {}
    fun sumMasuk(tenantId: String): Flow<Double?> = MutableStateFlow(null)
    fun sumKeluar(tenantId: String): Flow<Double?> = MutableStateFlow(null)
}

@Singleton
class BmpSettingsDao @Inject constructor() {
    suspend fun getByTenantId(tenantId: String): BmpSettingsEntity? = null
    fun observeByTenantId(tenantId: String): Flow<BmpSettingsEntity?> = emptyFlow()
    suspend fun upsert(settings: BmpSettingsEntity): Long = 0L
    suspend fun deleteByTenantId(tenantId: String) {}
    suspend fun getAll(): List<BmpSettingsEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
}

@Singleton
class PrintSettingsDao @Inject constructor() {
    suspend fun getByTenantAndModule(tenantId: String, moduleKey: String): PrintSettingsEntity? = null
    fun observeByTenantAndModule(tenantId: String, moduleKey: String): Flow<PrintSettingsEntity?> = emptyFlow()
    suspend fun upsert(settings: PrintSettingsEntity): Long = 0L
    suspend fun deleteByTenantId(tenantId: String) {}
    suspend fun getAll(): List<PrintSettingsEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
}

@Singleton
class BmpEmployeeDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpEmployeeEntity>> = emptyFlow()
    suspend fun getById(id: Long): BmpEmployeeEntity? = null
    suspend fun upsert(employee: BmpEmployeeEntity): Long = 0L
    suspend fun update(employee: BmpEmployeeEntity) {}
    suspend fun softDelete(id: Long) {}
    suspend fun getAll(tenantId: String): List<BmpEmployeeEntity> = emptyList()
    suspend fun getAll(): List<BmpEmployeeEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
}

@Singleton
class BmpPayrollDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpPayrollEntity>> = emptyFlow()
    fun observeForEmployee(employeeId: Long): Flow<List<BmpPayrollEntity>> = emptyFlow()
    suspend fun getById(id: String): BmpPayrollEntity? = null
    suspend fun insert(payroll: BmpPayrollEntity) {}
    suspend fun getAll(tenantId: String): List<BmpPayrollEntity> = emptyList()
    suspend fun getAll(): List<BmpPayrollEntity> = emptyList()
    suspend fun markSynced(id: String) {}
    suspend fun getDeletedIds(tenantId: String): List<String> = emptyList()
    suspend fun hardDelete(id: String) {}
}

@Singleton
class BmpBahanBakuDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpBahanBakuEntity>> = emptyFlow()
    suspend fun getById(id: Long): BmpBahanBakuEntity? = null
    suspend fun insert(entity: BmpBahanBakuEntity): Long = 0L
    suspend fun upsert(entity: BmpBahanBakuEntity): Long = 0L
    suspend fun update(entity: BmpBahanBakuEntity) {}
    suspend fun softDelete(id: Long) {}
    suspend fun getAll(tenantId: String): List<BmpBahanBakuEntity> = emptyList()
    suspend fun getAll(): List<BmpBahanBakuEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
    suspend fun getDeletedIds(tenantId: String): List<Long> = emptyList()
    suspend fun hardDelete(id: Long) {}
}

@Singleton
class BmpBahanBakuItemDao @Inject constructor() {
    suspend fun listForBahanBaku(bahanBakuId: Long): List<BmpBahanBakuItemEntity> = emptyList()
    suspend fun insertAll(items: List<BmpBahanBakuItemEntity>) {}
    suspend fun deleteByBahanBaku(bahanBakuId: Long) {}
    suspend fun getAll(): List<BmpBahanBakuItemEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
    suspend fun getDistinctBahanBaku(tenantId: String): List<String> = emptyList()
    suspend fun getLatestRate(tenantId: String, name: String): Double? = null
}

@Singleton
class BmpProductStockDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpProductStockEntity>> = emptyFlow()
    suspend fun getByMasterProduct(masterProductId: Long, tenantId: String): BmpProductStockEntity? = null
    suspend fun upsert(stock: BmpProductStockEntity): Long = 0L
    suspend fun getAll(tenantId: String): List<BmpProductStockEntity> = emptyList()
    suspend fun getAll(): List<BmpProductStockEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
}

@Singleton
class BmpStockLedgerDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpStockLedgerEntity>> = emptyFlow()
    suspend fun insert(entry: BmpStockLedgerEntity): Long = 0L
    suspend fun getAll(tenantId: String): List<BmpStockLedgerEntity> = emptyList()
    suspend fun getAll(): List<BmpStockLedgerEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
}

@Singleton
class BmpProductionLogDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<BmpProductionLogEntity>> = emptyFlow()
    suspend fun insert(log: BmpProductionLogEntity): Long = 0L
    suspend fun getAll(tenantId: String): List<BmpProductionLogEntity> = emptyList()
    suspend fun getAll(): List<BmpProductionLogEntity> = emptyList()
    suspend fun markSynced(id: Long) {}
}

// ── BmpAggregateDaoImpl stub ──────────────────────────────────────────────────
class BmpAggregateDaoImpl @Inject constructor()
