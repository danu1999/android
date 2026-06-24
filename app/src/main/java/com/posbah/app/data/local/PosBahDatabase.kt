package com.posbah.app.data.local

// ─────────────────────────────────────────────────────────────────────────────
// PosBahDatabase.kt — Full Online mode STUB
// Room RoomDatabase dihapus. Semua DAO method return singleton stub instances.
// File ini dipertahankan agar kompilasi tidak gagal untuk kode yang masih
// bergantung pada PosBahDatabase.
// ─────────────────────────────────────────────────────────────────────────────

import com.posbah.app.data.local.dao.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosBahDatabase @Inject constructor(
    private val _localUserDao: LocalUserDao,
    private val _tenantDao: TenantDao,
    private val _outletDao: OutletDao,
    private val _employeeDao: EmployeeDao,
    private val _productDao: ProductDao,
    private val _customerDao: CustomerDao,
    private val _transactionDao: TransactionDao,
    private val _transactionItemDao: TransactionItemDao,
    private val _activityLogDao: ActivityLogDao,
    private val _bmpClientDao: BmpClientDao,
    private val _bmpInvoiceDao: BmpInvoiceDao,
    private val _bmpProductDao: BmpProductDao,
    private val _bmpMasterProductDao: BmpMasterProductDao,
    private val _bmpPaymentDao: BmpPaymentDao,
    private val _bmpCashFlowDao: BmpCashFlowDao,
    private val _bmpSettingsDao: BmpSettingsDao,
    private val _printSettingsDao: PrintSettingsDao,
    private val _bmpEmployeeDao: BmpEmployeeDao,
    private val _bmpPayrollDao: BmpPayrollDao,
    private val _bmpBahanBakuDao: BmpBahanBakuDao,
    private val _bmpBahanBakuItemDao: BmpBahanBakuItemDao,
    private val _bmpProductStockDao: BmpProductStockDao,
    private val _bmpStockLedgerDao: BmpStockLedgerDao,
    private val _bmpProductionLogDao: BmpProductionLogDao
) {
    // ── DAO accessors (stub no-ops) ───────────────────────────────────────────
    fun localUserDao(): LocalUserDao = _localUserDao
    fun tenantDao(): TenantDao = _tenantDao
    fun outletDao(): OutletDao = _outletDao
    fun employeeDao(): EmployeeDao = _employeeDao
    fun productDao(): ProductDao = _productDao
    fun customerDao(): CustomerDao = _customerDao
    fun transactionDao(): TransactionDao = _transactionDao
    fun transactionItemDao(): TransactionItemDao = _transactionItemDao
    fun activityLogDao(): ActivityLogDao = _activityLogDao
    fun bmpClientDao(): BmpClientDao = _bmpClientDao
    fun bmpInvoiceDao(): BmpInvoiceDao = _bmpInvoiceDao
    fun bmpProductDao(): BmpProductDao = _bmpProductDao
    fun bmpMasterProductDao(): BmpMasterProductDao = _bmpMasterProductDao
    fun bmpPaymentDao(): BmpPaymentDao = _bmpPaymentDao
    fun bmpCashFlowDao(): BmpCashFlowDao = _bmpCashFlowDao
    fun bmpSettingsDao(): BmpSettingsDao = _bmpSettingsDao
    fun printSettingsDao(): PrintSettingsDao = _printSettingsDao
    fun bmpEmployeeDao(): BmpEmployeeDao = _bmpEmployeeDao
    fun bmpPayrollDao(): BmpPayrollDao = _bmpPayrollDao
    fun bmpBahanBakuDao(): BmpBahanBakuDao = _bmpBahanBakuDao
    fun bmpBahanBakuItemDao(): BmpBahanBakuItemDao = _bmpBahanBakuItemDao
    fun bmpProductStockDao(): BmpProductStockDao = _bmpProductStockDao
    fun bmpStockLedgerDao(): BmpStockLedgerDao = _bmpStockLedgerDao
    fun bmpProductionLogDao(): BmpProductionLogDao = _bmpProductionLogDao

    /** No-op: tidak ada Room database untuk di-clear */
    suspend fun clearAllTables() {
        android.util.Log.i("PosBahDatabase", "clearAllTables() called — no-op in full-online mode. All data is on VPS.")
    }

    /** No-op transaction wrapper: hanya eksekusi block langsung */
    suspend fun <T> withTransaction(block: suspend () -> T): T = block()

    fun close() {
        android.util.Log.i("PosBahDatabase", "Database close() stub called")
    }

    /**
     * Stub openHelper — MainActivity memanggil db.openHelper.writableDatabase
     * untuk preload SQLCipher. Dalam full-online mode ini no-op.
     */
    val openHelper: OpenHelperStub get() = OpenHelperStub

    object OpenHelperStub {
        val writableDatabase: Any get() {
            android.util.Log.i("PosBahDatabase", "openHelper.writableDatabase — no-op in full-online mode.")
            return Unit
        }
    }

    companion object {
        /** Stub factory — tidak membuat SQLite database */
        fun build(context: android.content.Context, passphrase: ByteArray? = null): PosBahDatabase {
            throw IllegalStateException(
                "PosBahDatabase.build() tidak boleh dipanggil dalam full-online mode. " +
                "Gunakan Hilt injection via @Inject constructor."
            )
        }
    }
}
