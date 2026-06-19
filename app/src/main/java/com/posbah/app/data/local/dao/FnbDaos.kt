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
    // ── Tenant-level queries (used by seeder, owner dashboard) ──────────────
    @Query("SELECT * FROM products WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY name ASC")
    fun observe(tenantId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun list(tenantId: String): List<ProductEntity>

    // ── Outlet-level queries (strict isolation per outlet) ───────────────────
    /**
     * Returns products belonging to a specific outlet OR products with no outletId
     * if the requested outlet is the default outlet (for backward compat with legacy data).
     */
    @Query("""
        SELECT * FROM products
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
          AND isDeleted = 0
        ORDER BY name ASC
    """)
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE tenantId = :tenantId
          AND outletId = :outletId
          AND isDeleted = 0
        ORDER BY name ASC
    """)
    suspend fun listForOutlet(tenantId: String, outletId: Long): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE tenantId = :tenantId AND barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(tenantId: String, barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE tenantId = :tenantId AND name LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY name ASC")
    fun search(tenantId: String, query: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
          AND name LIKE '%' || :query || '%'
          AND isDeleted = 0
        ORDER BY name ASC
    """)
    fun searchForOutlet(tenantId: String, outletId: Long, query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET stock = :newStock, updatedAt = :ts WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)

    /** Soft-delete produk POS */
    @Query("UPDATE products SET isDeleted = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    /** Ambil ID yang sudah deleted untuk dikirim DELETE ke server */
    @Query("SELECT id FROM products WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Query("DELETE FROM products WHERE tenantId = :tenantId")
    suspend fun clearTenantProducts(tenantId: String)

    @Query("UPDATE products SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}

@Dao
interface CustomerDao {
    // ── Tenant-level (owner sees all) ────────────────────────────────────────
    @Query("SELECT * FROM customers WHERE tenantId = :tenantId ORDER BY name ASC")
    fun observe(tenantId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE tenantId = :tenantId ORDER BY name ASC")
    suspend fun list(tenantId: String): List<CustomerEntity>

    // ── Outlet-level (strict isolation) ──────────────────────────────────────
    @Query("""
        SELECT * FROM customers
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
        ORDER BY name ASC
    """)
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<CustomerEntity>>

    @Query("""
        SELECT * FROM customers
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
        ORDER BY name ASC
    """)
    suspend fun listForOutlet(tenantId: String, outletId: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customers")
    suspend fun getAll(): List<CustomerEntity>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE customers SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}

@Dao
interface TransactionDao {
    // ── Tenant-level (owner sees all) ────────────────────────────────────────
    @Query("SELECT * FROM transactions WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY date DESC")
    fun observe(tenantId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE tenantId = :tenantId AND status = 'PENDING' AND isDeleted = 0 ORDER BY date DESC")
    fun observePendingQueues(tenantId: String): Flow<List<TransactionEntity>>

    // ── Outlet-level (strict isolation) ──────────────────────────────────────
    @Query("""
        SELECT * FROM transactions
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
          AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun observeForOutlet(tenantId: String, outletId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE tenantId = :tenantId
          AND (outletId = :outletId OR outletId IS NULL)
          AND status = 'PENDING'
          AND isDeleted = 0
        ORDER BY date DESC
    """)
    fun observePendingQueuesForOutlet(tenantId: String, outletId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0 LIMIT 1")
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

    @Query("SELECT receiptNumber FROM transactions WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getLastReceiptNumber(tenantId: String): String?

    /** Forcefully corrects a stale date for an existing transaction identified by receipt number. */
    @Query("UPDATE transactions SET date = :date, createdAt = :date, updatedAt = :date WHERE receiptNumber = :receiptNumber")
    suspend fun updateDateByReceiptNumber(receiptNumber: String, date: Long)

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    /** Soft-delete: tandai isDeleted=1, cascade ke server via SyncManager */
    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    /** Hard-delete setelah server berhasil delete */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    /** Ambil ID yang sudah soft-deleted untuk dikirim DELETE ke server */
    @Query("SELECT id FROM transactions WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>
}

@Dao
interface TransactionItemDao {
    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id ASC")
    fun observeForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun listForTransaction(transactionId: Long): List<TransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transaction_items")
    suspend fun getAll(): List<TransactionItemEntity>

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteForTransaction(transactionId: Long)
}
