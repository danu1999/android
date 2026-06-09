package com.posbah.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE tenantId = :tenantId ORDER BY name ASC")
    fun observe(tenantId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE tenantId = :tenantId ORDER BY name ASC")
    suspend fun list(tenantId: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE tenantId = :tenantId AND barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(tenantId: String, barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE tenantId = :tenantId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(tenantId: String, query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET stock = :newStock, updatedAt = :ts WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM products WHERE tenantId = :tenantId")
    suspend fun clearTenantProducts(tenantId: String)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE tenantId = :tenantId ORDER BY name ASC")
    fun observe(tenantId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE tenantId = :tenantId ORDER BY name ASC")
    suspend fun list(tenantId: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE tenantId = :tenantId ORDER BY date DESC")
    fun observe(tenantId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE tenantId = :tenantId AND status = 'PENDING' ORDER BY date DESC")
    fun observePendingQueues(tenantId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET status = :status, paymentMethod = :method, amountPaid = :amtPaid, change = :chg, updatedAt = :ts WHERE id = :id")
    suspend fun completePendingTransaction(
        id: Long,
        status: String = "COMPLETED",
        method: String,
        amtPaid: Double?,
        chg: Double?,
        ts: Long = System.currentTimeMillis()
    )

    @Query("UPDATE transactions SET status = 'CANCELLED', updatedAt = :ts WHERE id = :id")
    suspend fun cancelTransaction(id: Long, ts: Long = System.currentTimeMillis())

    @Query("SELECT receiptNumber FROM transactions WHERE tenantId = :tenantId ORDER BY id DESC LIMIT 1")
    suspend fun getLastReceiptNumber(tenantId: String): String?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TransactionItemDao {
    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id ASC")
    fun observeForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun listForTransaction(transactionId: Long): List<TransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionItemEntity>)

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteForTransaction(transactionId: Long)
}
