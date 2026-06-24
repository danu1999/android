package com.posbah.app.data.local.dao

// ─────────────────────────────────────────────────────────────────────────────
// FnbDaos.kt — Full Online mode STUB
// Room @Dao interfaces diganti dengan stub concrete classes.
// Semua method no-op / return empty. Tidak ada SQLite query.
// ─────────────────────────────────────────────────────────────────────────────

import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<ProductEntity>> = emptyFlow()
    suspend fun list(tenantId: String): List<ProductEntity> = emptyList()
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<ProductEntity>> = emptyFlow()
    suspend fun listForOutlet(tenantId: String, outletId: Long): List<ProductEntity> = emptyList()
    suspend fun getById(id: Long): ProductEntity? = null
    suspend fun getByBarcode(tenantId: String, barcode: String): ProductEntity? = null
    fun search(tenantId: String, query: String): Flow<List<ProductEntity>> = emptyFlow()
    fun searchForOutlet(tenantId: String, outletId: Long, query: String): Flow<List<ProductEntity>> = emptyFlow()
    suspend fun upsert(product: ProductEntity): Long = 0L
    suspend fun updateStock(id: Long, newStock: Int, updatedAt: Long = System.currentTimeMillis()) {}
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis()) {}
    suspend fun markSynced(id: Long) {}
}

@Singleton
class CustomerDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<CustomerEntity>> = emptyFlow()
    suspend fun list(tenantId: String): List<CustomerEntity> = emptyList()
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<CustomerEntity>> = emptyFlow()
    suspend fun listForOutlet(tenantId: String, outletId: Long): List<CustomerEntity> = emptyList()
    suspend fun getById(id: Long): CustomerEntity? = null
    suspend fun upsert(customer: CustomerEntity): Long = 0L
    suspend fun delete(id: Long) {}
    suspend fun markSynced(id: Long) {}
}

@Singleton
class TransactionDao @Inject constructor() {
    fun observe(tenantId: String): Flow<List<TransactionEntity>> = emptyFlow()
    fun observePendingQueues(tenantId: String): Flow<List<TransactionEntity>> = emptyFlow()
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<TransactionEntity>> = emptyFlow()
    fun observePendingQueuesForOutlet(tenantId: String, outletId: Long): Flow<List<TransactionEntity>> = emptyFlow()
    suspend fun getById(id: Long): TransactionEntity? = null
    suspend fun insert(transaction: TransactionEntity): Long = 0L
    suspend fun update(transaction: TransactionEntity) {}
    suspend fun getAll(): List<TransactionEntity> = emptyList()
    suspend fun getLastReceiptNumber(tenantId: String): String? = null
    suspend fun completePendingTransaction(id: Long, method: String, amtPaid: Double?, chg: Double?) {}
    suspend fun cancelTransaction(id: Long) {}
    suspend fun softDelete(id: Long) {}
    suspend fun markSynced(id: Long) {}
}

@Singleton
class TransactionItemDao @Inject constructor() {
    suspend fun listForTransaction(transactionId: Long): List<TransactionItemEntity> = emptyList()
    suspend fun insertAll(items: List<TransactionItemEntity>) {}
    suspend fun markSynced(transactionId: Long) {}
    suspend fun getAll(): List<TransactionItemEntity> = emptyList()
}

@Singleton
class ActivityLogDao @Inject constructor() {
    fun observeLogs(tenantId: String, appMode: String): Flow<List<com.posbah.app.data.local.entities.ActivityLogEntity>> = emptyFlow()
    fun observeAllLogs(tenantId: String): Flow<List<com.posbah.app.data.local.entities.ActivityLogEntity>> = emptyFlow()
    suspend fun insertLog(log: com.posbah.app.data.local.entities.ActivityLogEntity) {}
    suspend fun getAll(): List<com.posbah.app.data.local.entities.ActivityLogEntity> = emptyList()
}
