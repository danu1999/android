package com.posbah.app.di

// ─────────────────────────────────────────────────────────────────────────────
// DatabaseModule.kt — Full Online mode
// Room dihapus. Module ini sekarang hanya menyediakan stub DAO instances
// via constructor injection. Tidak ada SQLite, tidak ada SQLCipher.
// ─────────────────────────────────────────────────────────────────────────────

import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // ── Core DAOs ──────────────────────────────────────────────────────────────

    @Provides @Singleton fun provideLocalUserDao(): LocalUserDao = LocalUserDao()
    @Provides @Singleton fun provideTenantDao(): TenantDao = TenantDao()
    @Provides @Singleton fun provideOutletDao(): OutletDao = OutletDao()
    @Provides @Singleton fun provideEmployeeDao(): EmployeeDao = EmployeeDao()

    // ── FnB DAOs ───────────────────────────────────────────────────────────────

    @Provides @Singleton fun provideProductDao(): ProductDao = ProductDao()
    @Provides @Singleton fun provideCustomerDao(): CustomerDao = CustomerDao()
    @Provides @Singleton fun provideTransactionDao(): TransactionDao = TransactionDao()
    @Provides @Singleton fun provideTransactionItemDao(): TransactionItemDao = TransactionItemDao()
    @Provides @Singleton fun provideActivityLogDao(): ActivityLogDao = ActivityLogDao()

    // ── BMP DAOs ───────────────────────────────────────────────────────────────

    @Provides @Singleton fun provideBmpClientDao(): BmpClientDao = BmpClientDao()
    @Provides @Singleton fun provideBmpInvoiceDao(): BmpInvoiceDao = BmpInvoiceDao()
    @Provides @Singleton fun provideBmpProductDao(): BmpProductDao = BmpProductDao()
    @Provides @Singleton fun provideBmpMasterProductDao(): BmpMasterProductDao = BmpMasterProductDao()
    @Provides @Singleton fun provideBmpPaymentDao(): BmpPaymentDao = BmpPaymentDao()
    @Provides @Singleton fun provideBmpCashFlowDao(): BmpCashFlowDao = BmpCashFlowDao()
    @Provides @Singleton fun provideBmpSettingsDao(): BmpSettingsDao = BmpSettingsDao()
    @Provides @Singleton fun providePrintSettingsDao(): PrintSettingsDao = PrintSettingsDao()
    @Provides @Singleton fun provideBmpEmployeeDao(): BmpEmployeeDao = BmpEmployeeDao()
    @Provides @Singleton fun provideBmpPayrollDao(): BmpPayrollDao = BmpPayrollDao()
    @Provides @Singleton fun provideBmpBahanBakuDao(): BmpBahanBakuDao = BmpBahanBakuDao()
    @Provides @Singleton fun provideBmpBahanBakuItemDao(): BmpBahanBakuItemDao = BmpBahanBakuItemDao()
    @Provides @Singleton fun provideBmpProductStockDao(): BmpProductStockDao = BmpProductStockDao()
    @Provides @Singleton fun provideBmpStockLedgerDao(): BmpStockLedgerDao = BmpStockLedgerDao()
    @Provides @Singleton fun provideBmpProductionLogDao(): BmpProductionLogDao = BmpProductionLogDao()

    // ── PosBahDatabase wrapper (stub) ──────────────────────────────────────────

    @Provides @Singleton
    fun provideDatabase(
        localUserDao: LocalUserDao,
        tenantDao: TenantDao,
        outletDao: OutletDao,
        employeeDao: EmployeeDao,
        productDao: ProductDao,
        customerDao: CustomerDao,
        transactionDao: TransactionDao,
        transactionItemDao: TransactionItemDao,
        activityLogDao: ActivityLogDao,
        bmpClientDao: BmpClientDao,
        bmpInvoiceDao: BmpInvoiceDao,
        bmpProductDao: BmpProductDao,
        bmpMasterProductDao: BmpMasterProductDao,
        bmpPaymentDao: BmpPaymentDao,
        bmpCashFlowDao: BmpCashFlowDao,
        bmpSettingsDao: BmpSettingsDao,
        printSettingsDao: PrintSettingsDao,
        bmpEmployeeDao: BmpEmployeeDao,
        bmpPayrollDao: BmpPayrollDao,
        bmpBahanBakuDao: BmpBahanBakuDao,
        bmpBahanBakuItemDao: BmpBahanBakuItemDao,
        bmpProductStockDao: BmpProductStockDao,
        bmpStockLedgerDao: BmpStockLedgerDao,
        bmpProductionLogDao: BmpProductionLogDao
    ): PosBahDatabase = PosBahDatabase(
        localUserDao, tenantDao, outletDao, employeeDao,
        productDao, customerDao, transactionDao, transactionItemDao, activityLogDao,
        bmpClientDao, bmpInvoiceDao, bmpProductDao, bmpMasterProductDao,
        bmpPaymentDao, bmpCashFlowDao, bmpSettingsDao, printSettingsDao,
        bmpEmployeeDao, bmpPayrollDao, bmpBahanBakuDao, bmpBahanBakuItemDao,
        bmpProductStockDao, bmpStockLedgerDao, bmpProductionLogDao
    )
}
