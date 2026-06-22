package com.posbah.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

@Dao
interface BmpClientDao {
    @Query("SELECT * FROM bmp_clients WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY clientName ASC")
    fun observe(tenantId: String): Flow<List<BmpClientEntity>>

    @Query("SELECT * FROM bmp_clients WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: Long): BmpClientEntity?

    @Query("SELECT * FROM bmp_clients WHERE tenantId = :tenantId AND clientName LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY clientName ASC")
    fun search(tenantId: String, query: String): Flow<List<BmpClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: BmpClientEntity): Long

    @Update suspend fun update(client: BmpClientEntity)

    /** Soft-delete: tandai isDeleted=1, data tidak dihapus dari DB lokal */
    @Query("UPDATE bmp_clients SET isDeleted = 1, updatedAt = :ts, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    /** Untuk sync: ambil semua data (termasuk yang sudah dihapus) */
    @Query("SELECT * FROM bmp_clients WHERE tenantId = :tenantId")
    suspend fun getAll(tenantId: String): List<BmpClientEntity>

    /** Untuk sync: ambil semua tanpa filter tenant (dipakai syncAll upload) */
    @Query("SELECT * FROM bmp_clients")
    suspend fun getAll(): List<BmpClientEntity>

    @Query("UPDATE bmp_clients SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT COUNT(*) FROM bmp_clients WHERE tenantId = :tenantId AND isDeleted = 0")
    fun count(tenantId: String): Flow<Int>

    /** Ambil semua id yang isDeleted=1 untuk dikirim DELETE ke server */
    @Query("SELECT id FROM bmp_clients WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    /** Hard-delete lokal setelah berhasil delete di server */
    @Query("DELETE FROM bmp_clients WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpInvoiceDao {
    @Query("""
        SELECT * FROM bmp_invoices
        WHERE tenantId = :tenantId AND isDeleted = 0
        ORDER BY createdAt DESC
    """)
    fun observe(tenantId: String): Flow<List<BmpInvoiceEntity>>

    @Query("SELECT * FROM bmp_invoices WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: Long): BmpInvoiceEntity?

    @Query("SELECT * FROM bmp_invoices WHERE slug = :slug AND isDeleted = 0 LIMIT 1")
    suspend fun getBySlug(slug: String): BmpInvoiceEntity?

    @Query("SELECT * FROM bmp_invoices WHERE tenantId = :tenantId AND status = :status AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeByStatus(tenantId: String, status: String): Flow<List<BmpInvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: BmpInvoiceEntity): Long

    /** Upsert — digunakan oleh pullAll() agar tidak terjadi duplikasi saat re-pull. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(invoice: BmpInvoiceEntity): Long

    @Update suspend fun update(invoice: BmpInvoiceEntity)

    @Query("UPDATE bmp_invoices SET status = :status, updatedAt = :ts, isSynced = 0 WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE bmp_invoices SET paidAmount = :paid, status = :status, updatedAt = :ts, isSynced = 0 WHERE id = :id")
    suspend fun updatePaid(id: Long, paid: Double, status: String, ts: Long = System.currentTimeMillis())

    /** Soft-delete invoice: set isDeleted=1 */
    @Query("UPDATE bmp_invoices SET isDeleted = 1, updatedAt = :ts, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    /** Soft-delete semua invoice milik sebuah klien */
    @Query("UPDATE bmp_invoices SET isDeleted = 1, updatedAt = :ts, isSynced = 0 WHERE clientId = :clientId")
    suspend fun softDeleteByClientId(clientId: Long, ts: Long = System.currentTimeMillis())

    /** Untuk sync: ambil semua (termasuk deleted) */
    @Query("SELECT * FROM bmp_invoices WHERE tenantId = :tenantId")
    suspend fun getAllForTenant(tenantId: String): List<BmpInvoiceEntity>

    @Query("SELECT * FROM bmp_invoices")
    suspend fun getAll(): List<BmpInvoiceEntity>

    @Query("UPDATE bmp_invoices SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE bmp_invoices SET isSynced = 0 WHERE id = :id")
    suspend fun markUnsynced(id: Long)

    @Query("SELECT COUNT(*) FROM bmp_invoices WHERE tenantId = :tenantId AND isDeleted = 0")
    fun count(tenantId: String): Flow<Int>

    @Query("SELECT IFNULL(SUM(totalAmount), 0) FROM bmp_invoices WHERE tenantId = :tenantId AND isDeleted = 0")
    fun totalAmount(tenantId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(paidAmount), 0) FROM bmp_invoices WHERE tenantId = :tenantId AND isDeleted = 0")
    fun totalPaid(tenantId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(totalAmount - paidAmount), 0) FROM bmp_invoices WHERE tenantId = :tenantId AND status != 'PAID' AND isDeleted = 0")
    fun totalOutstanding(tenantId: String): Flow<Double>

    @Query("SELECT * FROM bmp_invoices WHERE tenantId = :tenantId AND clientId = :clientId AND status != 'PAID' AND isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getUnpaidInvoicesForClient(tenantId: String, clientId: Long): List<BmpInvoiceEntity>

    /** Ambil ID yang sudah soft-deleted untuk dikirim ke server */
    @Query("SELECT id FROM bmp_invoices WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    /** Hard-delete setelah server berhasil delete */
    @Query("DELETE FROM bmp_invoices WHERE id = :id")
    suspend fun hardDelete(id: Long)

    /** Ambil semua invoice milik klien tertentu (termasuk yang belum deleted) */
    @Query("SELECT * FROM bmp_invoices WHERE clientId = :clientId")
    suspend fun getByClientId(clientId: Long): List<BmpInvoiceEntity>
}

@Dao
interface BmpProductDao {
    @Query("SELECT * FROM bmp_products WHERE invoiceId = :invoiceId AND isDeleted = 0 ORDER BY id ASC")
    fun observeByInvoice(invoiceId: Long): Flow<List<BmpProductEntity>>

    @Query("SELECT * FROM bmp_products WHERE invoiceId = :invoiceId AND isDeleted = 0 ORDER BY id ASC")
    suspend fun listByInvoice(invoiceId: Long): List<BmpProductEntity>

    @Query("SELECT * FROM bmp_products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BmpProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: BmpProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: BmpProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<BmpProductEntity>)

    @Update suspend fun update(product: BmpProductEntity)

    /** Soft-delete item produk */
    @Query("UPDATE bmp_products SET isDeleted = 1, isSynced = 0, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    /** Soft-delete semua produk milik sebuah invoice */
    @Query("UPDATE bmp_products SET isDeleted = 1, isSynced = 0, updatedAt = :ts WHERE invoiceId = :invoiceId")
    suspend fun softDeleteByInvoice(invoiceId: Long, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM bmp_products")
    suspend fun getAll(): List<BmpProductEntity>

    @Query("DELETE FROM bmp_products WHERE invoiceId = :invoiceId")
    suspend fun deleteByInvoice(invoiceId: Long)

    @Query("UPDATE bmp_products SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    /** Ambil ID yang sudah soft-deleted */
    @Query("SELECT id FROM bmp_products WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_products WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpMasterProductDao {
    @Query("SELECT * FROM bmp_master_products WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY title ASC")
    fun observe(tenantId: String): Flow<List<BmpMasterProductEntity>>

    @Query("SELECT * FROM bmp_master_products WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: Long): BmpMasterProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: BmpMasterProductEntity): Long

    @Update suspend fun update(product: BmpMasterProductEntity)

    /** Soft-delete produk master */
    @Query("UPDATE bmp_master_products SET isDeleted = 1, updatedAt = :ts, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM bmp_master_products")
    suspend fun getAll(): List<BmpMasterProductEntity>

    @Query("SELECT id FROM bmp_master_products WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_master_products WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("UPDATE bmp_master_products SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}

@Dao
interface BmpPaymentDao {
    @Query("SELECT * FROM bmp_invoice_payments WHERE invoiceId = :invoiceId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun observeForInvoice(invoiceId: Long): Flow<List<BmpInvoicePaymentEntity>>

    @Query("SELECT * FROM bmp_invoice_payments WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun observe(tenantId: String): Flow<List<BmpInvoicePaymentEntity>>

    @Query("SELECT IFNULL(SUM(paymentAmount), 0) FROM bmp_invoice_payments WHERE invoiceId = :invoiceId AND isDeleted = 0")
    suspend fun sumForInvoice(invoiceId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: BmpInvoicePaymentEntity): Long

    /** Upsert — digunakan oleh pullAll() agar tidak terjadi duplikasi saat re-pull. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: BmpInvoicePaymentEntity): Long

    @Query("SELECT * FROM bmp_invoice_payments WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: Long): BmpInvoicePaymentEntity?

    @Update
    suspend fun update(payment: BmpInvoicePaymentEntity)

    /** Soft-delete pembayaran */
    @Query("UPDATE bmp_invoice_payments SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** Soft-delete semua pembayaran milik sebuah invoice */
    @Query("UPDATE bmp_invoice_payments SET isDeleted = 1, isSynced = 0 WHERE invoiceId = :invoiceId")
    suspend fun softDeleteByInvoice(invoiceId: Long)

    @Query("SELECT * FROM bmp_invoice_payments WHERE invoiceId = :invoiceId AND isDeleted = 0")
    suspend fun listForInvoice(invoiceId: Long): List<BmpInvoicePaymentEntity>

    /** Untuk sync — termasuk yang deleted */
    @Query("SELECT * FROM bmp_invoice_payments WHERE invoiceId = :invoiceId")
    suspend fun listAllForInvoice(invoiceId: Long): List<BmpInvoicePaymentEntity>

    @Query("DELETE FROM bmp_invoice_payments WHERE invoiceId = :invoiceId")
    suspend fun deleteByInvoice(invoiceId: Long)

    @Query("SELECT * FROM bmp_invoice_payments")
    suspend fun getAll(): List<BmpInvoicePaymentEntity>

    @Query("UPDATE bmp_invoice_payments SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT id FROM bmp_invoice_payments WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_invoice_payments WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpCashFlowDao {
    @Query("SELECT * FROM bmp_cashflow WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY transactionDate DESC")
    fun observe(tenantId: String): Flow<List<BmpCashFlowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BmpCashFlowEntity): Long

    /** Upsert — digunakan oleh pullAll() agar tidak terjadi duplikasi saat re-pull. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BmpCashFlowEntity): Long

    /** Soft-delete entri cashflow */
    @Query("UPDATE bmp_cashflow SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** Soft-delete semua cashflow yang terkait dengan pembayaran tertentu */
    @Query("UPDATE bmp_cashflow SET isDeleted = 1, isSynced = 0 WHERE paymentRefId = :paymentRefId")
    suspend fun softDeleteByPaymentRefId(paymentRefId: Long)

    @Query("SELECT * FROM bmp_cashflow")
    suspend fun getAll(): List<BmpCashFlowEntity>

    @Query("UPDATE bmp_cashflow SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE bmp_cashflow SET isDeleted = 1, isSynced = 0 WHERE paymentRefId = :paymentRefId")
    suspend fun deleteByPaymentRefId(paymentRefId: Long)

    @Query("UPDATE bmp_cashflow SET isDeleted = 1, isSynced = 0 WHERE transactionType = 'KELUAR' AND description = 'Pembelian barang khusus untuk Faktur ' || :invoiceNumber AND isDeleted = 0")
    suspend fun deleteExitsForInvoice(invoiceNumber: String)

    @Query("SELECT * FROM bmp_cashflow WHERE paymentRefId = :paymentRefId AND isDeleted = 0 LIMIT 1")
    suspend fun getByPaymentRefId(paymentRefId: Long): BmpCashFlowEntity?

    @Update
    suspend fun update(entry: BmpCashFlowEntity)

    @Query("SELECT IFNULL(SUM(amount), 0) FROM bmp_cashflow WHERE tenantId = :tenantId AND transactionType = 'MASUK' AND isDeleted = 0")
    fun totalIn(tenantId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM bmp_cashflow WHERE tenantId = :tenantId AND transactionType = 'KELUAR' AND isDeleted = 0")
    fun totalOut(tenantId: String): Flow<Double>

    @Query("SELECT id FROM bmp_cashflow WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_cashflow WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpSettingsDao {
    @Query("SELECT * FROM bmp_settings WHERE tenantId = :tenantId LIMIT 1")
    fun observe(tenantId: String): Flow<BmpSettingsEntity?>

    @Query("SELECT * FROM bmp_settings WHERE tenantId = :tenantId LIMIT 1")
    suspend fun get(tenantId: String): BmpSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: BmpSettingsEntity)

    @Query("SELECT * FROM bmp_settings")
    suspend fun getAll(): List<BmpSettingsEntity>

    @Query("DELETE FROM bmp_settings WHERE tenantId = :tenantId")
    suspend fun deleteByTenantId(tenantId: String)
}

@Dao
interface BmpEmployeeDao {
    @Query("SELECT * FROM bmp_employees WHERE tenantId = :tenantId AND isActive = 1 ORDER BY name ASC")
    fun observe(tenantId: String): Flow<List<BmpEmployeeEntity>>

    @Query("SELECT * FROM bmp_employees WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BmpEmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(employee: BmpEmployeeEntity): Long

    @Update suspend fun update(employee: BmpEmployeeEntity)

    @Query("UPDATE bmp_employees SET isActive = 0, isSynced = 0, updatedAt = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM bmp_employees")
    suspend fun getAll(): List<BmpEmployeeEntity>

    @Query("UPDATE bmp_employees SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM bmp_employees WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpPayrollDao {
    @Query("SELECT * FROM bmp_payrolls WHERE tenantId = :tenantId ORDER BY paymentDate DESC")
    fun observe(tenantId: String): Flow<List<BmpPayrollEntity>>

    @Query("SELECT * FROM bmp_payrolls WHERE employeeId = :employeeId ORDER BY paymentDate DESC")
    fun observeForEmployee(employeeId: Long): Flow<List<BmpPayrollEntity>>

    @Query("SELECT * FROM bmp_payrolls WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BmpPayrollEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payroll: BmpPayrollEntity): Long

    /** Upsert — digunakan oleh pullAll() agar tidak terjadi duplikasi saat re-pull. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payroll: BmpPayrollEntity): Long

    @Query("DELETE FROM bmp_payrolls WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM bmp_payrolls")
    suspend fun getAll(): List<BmpPayrollEntity>

    @Query("UPDATE bmp_payrolls SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}

/**
 * Aggregate operations that span multiple tables.
 * Note: implementation lives in BmpAggregateDaoImpl in the repository layer to
 * avoid Room compile-time restrictions on @Dao default methods that accept
 * other DAOs as parameters.
 */

@Dao
interface BmpBahanBakuDao {
    @Query("SELECT * FROM bmp_bahan_baku WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY tanggal DESC")
    fun observe(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpBahanBakuEntity>>

    @Query("SELECT * FROM bmp_bahan_baku WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: Long): com.posbah.app.data.local.entities.BmpBahanBakuEntity?

    @Query("SELECT * FROM bmp_bahan_baku WHERE tenantId = :tenantId AND noTagihan = :noTagihan AND isDeleted = 0 LIMIT 1")
    suspend fun getByTagihan(tenantId: String, noTagihan: String): com.posbah.app.data.local.entities.BmpBahanBakuEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: com.posbah.app.data.local.entities.BmpBahanBakuEntity): Long

    /** Upsert — digunakan oleh pullAll() agar tidak terjadi duplikasi saat re-pull. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: com.posbah.app.data.local.entities.BmpBahanBakuEntity): Long

    @Update
    suspend fun update(entry: com.posbah.app.data.local.entities.BmpBahanBakuEntity)

    /** Soft-delete bahan baku header */
    @Query("UPDATE bmp_bahan_baku SET isDeleted = 1, updatedAt = :ts, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM bmp_bahan_baku")
    suspend fun getAll(): List<com.posbah.app.data.local.entities.BmpBahanBakuEntity>

    @Query("UPDATE bmp_bahan_baku SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    /** Untuk saldo simulasi: total semua nilai bahan baku yang pernah masuk (tidak termasuk deleted) */
    @Query("SELECT IFNULL(SUM(totalHarga), 0) FROM bmp_bahan_baku WHERE tenantId = :tenantId AND isDeleted = 0")
    fun totalHarga(tenantId: String): Flow<Double>

    /** Untuk saldo kas riil: total kas yang sudah dibayarkan ke supplier (tidak termasuk deleted) */
    @Query("SELECT IFNULL(SUM(nominal), 0) FROM bmp_bahan_baku WHERE tenantId = :tenantId AND isDeleted = 0")
    fun totalNominal(tenantId: String): Flow<Double>

    @Query("SELECT id FROM bmp_bahan_baku WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_bahan_baku WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpBahanBakuItemDao {
    @Query("SELECT * FROM bmp_bahan_baku_item WHERE bahanBakuId = :bahanBakuId AND isDeleted = 0 ORDER BY id ASC")
    fun observeByBahanBaku(bahanBakuId: Long): Flow<List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>>

    @Query("SELECT * FROM bmp_bahan_baku_item WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): com.posbah.app.data.local.entities.BmpBahanBakuItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: com.posbah.app.data.local.entities.BmpBahanBakuItemEntity): Long

    /** Soft-delete semua item milik bahan baku tertentu */
    @Query("UPDATE bmp_bahan_baku_item SET isDeleted = 1, isSynced = 0 WHERE bahanBakuId = :bahanBakuId")
    suspend fun softDeleteByBahanBaku(bahanBakuId: Long)

    @Query("DELETE FROM bmp_bahan_baku_item WHERE bahanBakuId = :bahanBakuId")
    suspend fun deleteByBahanBaku(bahanBakuId: Long)

    @Query("SELECT * FROM bmp_bahan_baku_item")
    suspend fun getAll(): List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>

    @Query("UPDATE bmp_bahan_baku_item SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    /** Ambil rate terbaru untuk jenis bahan tertentu — dipakai kalkulator HPP */
    @Query("SELECT rate FROM bmp_bahan_baku_item WHERE tenantId = :tenantId AND jenisBahan = :jenisBahan AND isDeleted = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestRate(tenantId: String, jenisBahan: String): Double?

    @Query("SELECT DISTINCT jenisBahan FROM bmp_bahan_baku_item WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY jenisBahan ASC")
    suspend fun getDistinctBahanBaku(tenantId: String): List<String>

    @Query("SELECT IFNULL(SUM(kuantitas), 0.0) FROM bmp_bahan_baku_item WHERE tenantId = :tenantId AND jenisBahan = :jenisBahan AND isDeleted = 0")
    suspend fun sumPurchasedBahanBaku(tenantId: String, jenisBahan: String): Double

    @Query("SELECT id FROM bmp_bahan_baku_item WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_bahan_baku_item WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface PrintSettingsDao {
    @Query("SELECT * FROM print_settings WHERE tenantId = :tenantId AND moduleKey = :moduleKey LIMIT 1")
    fun observe(tenantId: String, moduleKey: String): Flow<com.posbah.app.data.local.entities.PrintSettingsEntity?>

    @Query("SELECT * FROM print_settings WHERE tenantId = :tenantId AND moduleKey = :moduleKey LIMIT 1")
    suspend fun get(tenantId: String, moduleKey: String): com.posbah.app.data.local.entities.PrintSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: com.posbah.app.data.local.entities.PrintSettingsEntity)

    @Query("SELECT * FROM print_settings")
    suspend fun getAll(): List<com.posbah.app.data.local.entities.PrintSettingsEntity>

    @Query("DELETE FROM print_settings WHERE tenantId = :tenantId")
    suspend fun deleteByTenantId(tenantId: String)

    @Query("DELETE FROM print_settings WHERE tenantId = :tenantId AND moduleKey = :moduleKey")
    suspend fun deleteByTenantIdAndModule(tenantId: String, moduleKey: String)
}

@Dao
interface BmpProductStockDao {
    @Query("SELECT * FROM bmp_product_stocks WHERE tenantId = :tenantId AND isDeleted = 0")
    fun observeAll(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpProductStockEntity>>

    @Query("SELECT * FROM bmp_product_stocks WHERE tenantId = :tenantId AND masterProductId = :productId AND isDeleted = 0 LIMIT 1")
    suspend fun getByProductId(tenantId: String, productId: Long): com.posbah.app.data.local.entities.BmpProductStockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stock: com.posbah.app.data.local.entities.BmpProductStockEntity): Long

    @Query("UPDATE bmp_product_stocks SET quantity = :quantity, updatedAt = :ts, isSynced = 0 WHERE masterProductId = :productId AND tenantId = :tenantId")
    suspend fun updateQuantity(tenantId: String, productId: Long, quantity: Double, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM bmp_product_stocks")
    suspend fun getAll(): List<com.posbah.app.data.local.entities.BmpProductStockEntity>

    @Query("UPDATE bmp_product_stocks SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT id FROM bmp_product_stocks WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_product_stocks WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpStockLedgerDao {
    @Query("SELECT * FROM bmp_stock_ledger WHERE tenantId = :tenantId AND masterProductId = :productId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeByProduct(tenantId: String, productId: Long): Flow<List<com.posbah.app.data.local.entities.BmpStockLedgerEntity>>

    @Query("SELECT * FROM bmp_stock_ledger WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpStockLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: com.posbah.app.data.local.entities.BmpStockLedgerEntity): Long

    @Query("SELECT * FROM bmp_stock_ledger")
    suspend fun getAll(): List<com.posbah.app.data.local.entities.BmpStockLedgerEntity>

    @Query("UPDATE bmp_stock_ledger SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT id FROM bmp_stock_ledger WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_stock_ledger WHERE id = :id")
    suspend fun hardDelete(id: Long)
}

@Dao
interface BmpProductionLogDao {
    @Query("SELECT * FROM bmp_production_logs WHERE tenantId = :tenantId AND isDeleted = 0 ORDER BY productionDate DESC")
    fun observeAll(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpProductionLogEntity>>

    @Query("SELECT * FROM bmp_production_logs WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: Long): com.posbah.app.data.local.entities.BmpProductionLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: com.posbah.app.data.local.entities.BmpProductionLogEntity): Long

    @Query("UPDATE bmp_production_logs SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("""
        SELECT IFNULL(SUM(rawMaterialUsedKg), 0.0) FROM bmp_production_logs 
        WHERE tenantId = :tenantId AND isDeleted = 0 AND masterProductId IN (
            SELECT id FROM bmp_master_products WHERE jenisBahanBaku = :jenisBahan
        )
    """)
    suspend fun sumUsedBahanBaku(tenantId: String, jenisBahan: String): Double

    @Query("SELECT * FROM bmp_production_logs")
    suspend fun getAll(): List<com.posbah.app.data.local.entities.BmpProductionLogEntity>

    @Query("UPDATE bmp_production_logs SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT id FROM bmp_production_logs WHERE tenantId = :tenantId AND isDeleted = 1")
    suspend fun getDeletedIds(tenantId: String): List<Long>

    @Query("DELETE FROM bmp_production_logs WHERE id = :id")
    suspend fun hardDelete(id: Long)
}
