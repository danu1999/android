package com.posbah.app.data.repository

import com.posbah.app.data.local.dao.CustomerDao
import com.posbah.app.data.local.dao.ProductDao
import com.posbah.app.data.local.dao.TransactionDao
import com.posbah.app.data.local.dao.TransactionItemDao
import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    fun observe(tenantId: String): Flow<List<ProductEntity>> = productDao.observe(tenantId)
    suspend fun list(tenantId: String): List<ProductEntity> = productDao.list(tenantId)
    suspend fun getById(id: Long): ProductEntity? = productDao.getById(id)
    suspend fun getByBarcode(tenantId: String, barcode: String): ProductEntity? = productDao.getByBarcode(tenantId, barcode)
    fun search(tenantId: String, query: String): Flow<List<ProductEntity>> = productDao.search(tenantId, query)
    suspend fun upsert(product: ProductEntity): Long = productDao.upsert(product)
    suspend fun updateStock(id: Long, newStock: Int) = productDao.updateStock(id, newStock)
    suspend fun delete(id: Long) = productDao.delete(id)
}

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao
) {
    fun observe(tenantId: String): Flow<List<CustomerEntity>> = customerDao.observe(tenantId)
    suspend fun list(tenantId: String): List<CustomerEntity> = customerDao.list(tenantId)
    suspend fun getById(id: Long): CustomerEntity? = customerDao.getById(id)
    suspend fun upsert(customer: CustomerEntity): Long = customerDao.upsert(customer)
    suspend fun delete(id: Long) = customerDao.delete(id)
}

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val productDao: ProductDao
) {
    fun observe(tenantId: String): Flow<List<TransactionEntity>> = transactionDao.observe(tenantId)
    fun observePendingQueues(tenantId: String): Flow<List<TransactionEntity>> = transactionDao.observePendingQueues(tenantId)
    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)
    suspend fun update(transaction: TransactionEntity) = transactionDao.update(transaction)
    suspend fun listItemsForTransaction(transactionId: Long): List<TransactionItemEntity> =
        transactionItemDao.listForTransaction(transactionId)

    suspend fun generateReceiptNumberForType(tenantId: String, prefixType: String): String {
        val todayStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val prefix = "$prefixType-$todayStr-"
        val lastReceipt = transactionDao.getLastReceiptNumber(tenantId)
        val seq = if (lastReceipt != null && lastReceipt.startsWith(prefix)) {
            val seqStr = lastReceipt.removePrefix(prefix)
            (seqStr.toIntOrNull() ?: 0) + 1
        } else {
            1
        }
        return String.format("$prefix%05d", seq)
    }

    /**
     * Executes local POS checkout. Saves the receipt record, maps lines, and deducts inventory quantities.
     */
    suspend fun checkout(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): TransactionEntity {
        val tenantId = transaction.tenantId
        val receiptNum = if (transaction.receiptNumber.isBlank() || transaction.receiptNumber.startsWith("TEMP-")) {
            generateReceiptNumber(tenantId)
        } else {
            transaction.receiptNumber
        }

        val finalTx = transaction.copy(receiptNumber = receiptNum)
        val transactionId = transactionDao.insert(finalTx)

        // Map line items to dynamic foreign key ID
        val itemsToInsert = items.map { it.copy(transactionId = transactionId) }
        transactionItemDao.insertAll(itemsToInsert)

        // Deduct inventory stock if transaction completed immediately
        if (finalTx.status == "COMPLETED") {
            deductInventory(itemsToInsert)
        }

        return finalTx.copy(id = transactionId)
    }

    /**
     * Completes a pending queue transaction and deducts matching stock lines.
     */
    suspend fun completePendingTransaction(
        id: Long,
        method: String,
        amountPaid: Double?,
        change: Double?
    ) {
        transactionDao.completePendingTransaction(id, method = method, amtPaid = amountPaid, chg = change)
        val items = transactionItemDao.listForTransaction(id)
        deductInventory(items)
    }

    suspend fun cancelTransaction(id: Long) {
        val tx = transactionDao.getById(id) ?: return
        if (tx.status == "CANCELLED") return
        
        transactionDao.cancelTransaction(id)
        if (tx.status == "COMPLETED") {
            val items = transactionItemDao.listForTransaction(id)
            for (item in items) {
                val product = productDao.getById(item.productId) ?: continue
                val newStock = product.stock + item.quantity
                productDao.updateStock(product.id, newStock)
            }
        }
    }

    private suspend fun deductInventory(items: List<TransactionItemEntity>) {
        for (item in items) {
            val product = productDao.getById(item.productId) ?: continue
            val newStock = (product.stock - item.quantity).coerceAtLeast(0)
            productDao.updateStock(product.id, newStock)
        }
    }

    private suspend fun generateReceiptNumber(tenantId: String): String {
        val todayStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val prefix = "FNB-$todayStr-"
        val lastReceipt = transactionDao.getLastReceiptNumber(tenantId)
        val seq = if (lastReceipt != null && lastReceipt.startsWith(prefix)) {
            val seqStr = lastReceipt.removePrefix(prefix)
            (seqStr.toIntOrNull() ?: 0) + 1
        } else {
            1
        }
        return String.format("$prefix%05d", seq)
    }
}
