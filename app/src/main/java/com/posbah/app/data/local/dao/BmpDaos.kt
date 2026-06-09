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
    @Query("SELECT * FROM bmp_clients WHERE tenantId = :tenantId ORDER BY clientName ASC")
    fun observe(tenantId: String): Flow<List<BmpClientEntity>>

    @Query("SELECT * FROM bmp_clients WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BmpClientEntity?

    @Query("SELECT * FROM bmp_clients WHERE tenantId = :tenantId AND clientName LIKE '%' || :query || '%' ORDER BY clientName ASC")
    fun search(tenantId: String, query: String): Flow<List<BmpClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: BmpClientEntity): Long

    @Update suspend fun update(client: BmpClientEntity)

    @Query("DELETE FROM bmp_clients WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM bmp_clients WHERE tenantId = :tenantId")
    fun count(tenantId: String): Flow<Int>
}

@Dao
interface BmpInvoiceDao {
    @Query("""
        SELECT * FROM bmp_invoices
        WHERE tenantId = :tenantId
        ORDER BY createdAt DESC
    """)
    fun observe(tenantId: String): Flow<List<BmpInvoiceEntity>>

    @Query("SELECT * FROM bmp_invoices WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BmpInvoiceEntity?

    @Query("SELECT * FROM bmp_invoices WHERE slug = :slug LIMIT 1")
    suspend fun getBySlug(slug: String): BmpInvoiceEntity?

    @Query("SELECT * FROM bmp_invoices WHERE tenantId = :tenantId AND status = :status ORDER BY createdAt DESC")
    fun observeByStatus(tenantId: String, status: String): Flow<List<BmpInvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: BmpInvoiceEntity): Long

    @Update suspend fun update(invoice: BmpInvoiceEntity)

    @Query("UPDATE bmp_invoices SET status = :status, updatedAt = :ts WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE bmp_invoices SET paidAmount = :paid, status = :status, updatedAt = :ts WHERE id = :id")
    suspend fun updatePaid(id: Long, paid: Double, status: String, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM bmp_invoices WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM bmp_invoices WHERE tenantId = :tenantId")
    fun count(tenantId: String): Flow<Int>

    @Query("SELECT IFNULL(SUM(totalAmount), 0) FROM bmp_invoices WHERE tenantId = :tenantId")
    fun totalAmount(tenantId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(paidAmount), 0) FROM bmp_invoices WHERE tenantId = :tenantId")
    fun totalPaid(tenantId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(totalAmount - paidAmount), 0) FROM bmp_invoices WHERE tenantId = :tenantId AND status != 'PAID'")
    fun totalOutstanding(tenantId: String): Flow<Double>

    @Query("SELECT * FROM bmp_invoices WHERE tenantId = :tenantId AND clientId = :clientId AND status != 'PAID' ORDER BY createdAt ASC")
    suspend fun getUnpaidInvoicesForClient(tenantId: String, clientId: Long): List<BmpInvoiceEntity>
}

@Dao
interface BmpProductDao {
    @Query("SELECT * FROM bmp_products WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun observeByInvoice(invoiceId: Long): Flow<List<BmpProductEntity>>

    @Query("SELECT * FROM bmp_products WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun listByInvoice(invoiceId: Long): List<BmpProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: BmpProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<BmpProductEntity>)

    @Update suspend fun update(product: BmpProductEntity)

    @Query("DELETE FROM bmp_products WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bmp_products WHERE invoiceId = :invoiceId")
    suspend fun deleteByInvoice(invoiceId: Long)
}

@Dao
interface BmpMasterProductDao {
    @Query("SELECT * FROM bmp_master_products WHERE tenantId = :tenantId ORDER BY title ASC")
    fun observe(tenantId: String): Flow<List<BmpMasterProductEntity>>

    @Query("SELECT * FROM bmp_master_products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BmpMasterProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: BmpMasterProductEntity): Long

    @Update suspend fun update(product: BmpMasterProductEntity)

    @Query("DELETE FROM bmp_master_products WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BmpPaymentDao {
    @Query("SELECT * FROM bmp_invoice_payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC")
    fun observeForInvoice(invoiceId: Long): Flow<List<BmpInvoicePaymentEntity>>

    @Query("SELECT * FROM bmp_invoice_payments WHERE tenantId = :tenantId ORDER BY paymentDate DESC")
    fun observe(tenantId: String): Flow<List<BmpInvoicePaymentEntity>>

    @Query("SELECT IFNULL(SUM(paymentAmount), 0) FROM bmp_invoice_payments WHERE invoiceId = :invoiceId")
    suspend fun sumForInvoice(invoiceId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: BmpInvoicePaymentEntity): Long

    @Query("SELECT * FROM bmp_invoice_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BmpInvoicePaymentEntity?

    @Update
    suspend fun update(payment: BmpInvoicePaymentEntity)

    @Query("DELETE FROM bmp_invoice_payments WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BmpCashFlowDao {
    @Query("SELECT * FROM bmp_cashflow WHERE tenantId = :tenantId ORDER BY transactionDate DESC")
    fun observe(tenantId: String): Flow<List<BmpCashFlowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BmpCashFlowEntity): Long

    @Query("DELETE FROM bmp_cashflow WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bmp_cashflow WHERE paymentRefId = :paymentRefId")
    suspend fun deleteByPaymentRefId(paymentRefId: Long)

    @Query("DELETE FROM bmp_cashflow WHERE transactionType = 'KELUAR' AND description = 'Pembelian barang khusus untuk Faktur ' || :invoiceNumber")
    suspend fun deleteExitsForInvoice(invoiceNumber: String)

    @Query("SELECT * FROM bmp_cashflow WHERE paymentRefId = :paymentRefId LIMIT 1")
    suspend fun getByPaymentRefId(paymentRefId: Long): BmpCashFlowEntity?

    @Update
    suspend fun update(entry: BmpCashFlowEntity)

    @Query("SELECT IFNULL(SUM(amount), 0) FROM bmp_cashflow WHERE tenantId = :tenantId AND transactionType = 'MASUK'")
    fun totalIn(tenantId: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM bmp_cashflow WHERE tenantId = :tenantId AND transactionType = 'KELUAR'")
    fun totalOut(tenantId: String): Flow<Double>
}

@Dao
interface BmpSettingsDao {
    @Query("SELECT * FROM bmp_settings WHERE tenantId = :tenantId LIMIT 1")
    fun observe(tenantId: String): Flow<BmpSettingsEntity?>

    @Query("SELECT * FROM bmp_settings WHERE tenantId = :tenantId LIMIT 1")
    suspend fun get(tenantId: String): BmpSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: BmpSettingsEntity)
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

    @Query("UPDATE bmp_employees SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

@Dao
interface BmpPayrollDao {
    @Query("SELECT * FROM bmp_payrolls WHERE tenantId = :tenantId ORDER BY paymentDate DESC")
    fun observe(tenantId: String): Flow<List<BmpPayrollEntity>>

    @Query("SELECT * FROM bmp_payrolls WHERE employeeId = :employeeId ORDER BY paymentDate DESC")
    fun observeForEmployee(employeeId: Long): Flow<List<BmpPayrollEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payroll: BmpPayrollEntity): Long

    @Query("DELETE FROM bmp_payrolls WHERE id = :id")
    suspend fun delete(id: Long)
}

/**
 * Aggregate operations that span multiple tables.
 * Note: implementation lives in BmpAggregateDaoImpl in the repository layer to
 * avoid Room compile-time restrictions on @Dao default methods that accept
 * other DAOs as parameters.
 */

@Dao
interface BmpBahanBakuDao {
    @Query("SELECT * FROM bmp_bahan_baku WHERE tenantId = :tenantId ORDER BY tanggal DESC")
    fun observe(tenantId: String): Flow<List<com.posbah.app.data.local.entities.BmpBahanBakuEntity>>

    @Query("SELECT * FROM bmp_bahan_baku WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): com.posbah.app.data.local.entities.BmpBahanBakuEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: com.posbah.app.data.local.entities.BmpBahanBakuEntity): Long

    @Update
    suspend fun update(entry: com.posbah.app.data.local.entities.BmpBahanBakuEntity)

    @Query("DELETE FROM bmp_bahan_baku WHERE id = :id")
    suspend fun delete(id: Long)

    /** Untuk saldo simulasi: total semua nilai bahan baku yang pernah masuk */
    @Query("SELECT IFNULL(SUM(totalHarga), 0) FROM bmp_bahan_baku WHERE tenantId = :tenantId")
    fun totalHarga(tenantId: String): Flow<Double>

    /** Untuk saldo kas riil: total kas yang sudah dibayarkan ke supplier */
    @Query("SELECT IFNULL(SUM(nominal), 0) FROM bmp_bahan_baku WHERE tenantId = :tenantId")
    fun totalNominal(tenantId: String): Flow<Double>
}

@Dao
interface BmpBahanBakuItemDao {
    @Query("SELECT * FROM bmp_bahan_baku_item WHERE bahanBakuId = :bahanBakuId ORDER BY id ASC")
    fun observeByBahanBaku(bahanBakuId: Long): Flow<List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>>

    @Query("SELECT * FROM bmp_bahan_baku_item WHERE bahanBakuId = :bahanBakuId ORDER BY id ASC")
    suspend fun listByBahanBaku(bahanBakuId: Long): List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>)

    @Query("DELETE FROM bmp_bahan_baku_item WHERE bahanBakuId = :bahanBakuId")
    suspend fun deleteByBahanBaku(bahanBakuId: Long)

    /** Ambil rate terbaru untuk jenis bahan tertentu — dipakai kalkulator HPP */
    @Query("SELECT rate FROM bmp_bahan_baku_item WHERE tenantId = :tenantId AND jenisBahan = :jenisBahan ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestRate(tenantId: String, jenisBahan: String): Double?
}

@Dao
interface PrintSettingsDao {
    @Query("SELECT * FROM print_settings WHERE tenantId = :tenantId LIMIT 1")
    fun observe(tenantId: String): Flow<com.posbah.app.data.local.entities.PrintSettingsEntity?>

    @Query("SELECT * FROM print_settings WHERE tenantId = :tenantId LIMIT 1")
    suspend fun get(tenantId: String): com.posbah.app.data.local.entities.PrintSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: com.posbah.app.data.local.entities.PrintSettingsEntity)
}
