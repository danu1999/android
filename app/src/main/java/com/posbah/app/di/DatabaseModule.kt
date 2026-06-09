package com.posbah.app.di

import android.content.Context
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.dao.BmpBahanBakuDao
import com.posbah.app.data.local.dao.BmpBahanBakuItemDao
import com.posbah.app.data.local.dao.BmpCashFlowDao
import com.posbah.app.data.local.dao.BmpClientDao
import com.posbah.app.data.local.dao.BmpEmployeeDao
import com.posbah.app.data.local.dao.BmpInvoiceDao
import com.posbah.app.data.local.dao.BmpMasterProductDao
import com.posbah.app.data.local.dao.BmpPaymentDao
import com.posbah.app.data.local.dao.BmpPayrollDao
import com.posbah.app.data.local.dao.BmpProductDao
import com.posbah.app.data.local.dao.BmpSettingsDao
import com.posbah.app.data.local.dao.PrintSettingsDao
import com.posbah.app.data.local.dao.EmployeeDao
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.OutletDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.data.local.dao.ProductDao
import com.posbah.app.data.local.dao.CustomerDao
import com.posbah.app.data.local.dao.TransactionDao
import com.posbah.app.data.local.dao.TransactionItemDao
import com.posbah.app.data.local.dao.ActivityLogDao
import com.posbah.app.security.KeystoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keystore: KeystoreManager
    ): PosBahDatabase {
        val passphrase = keystore.deriveDatabaseKey(context)
        return PosBahDatabase.build(context, passphrase)
    }

    @Provides fun localUserDao(db: PosBahDatabase): LocalUserDao = db.localUserDao()
    @Provides fun tenantDao(db: PosBahDatabase): TenantDao = db.tenantDao()
    @Provides fun outletDao(db: PosBahDatabase): OutletDao = db.outletDao()
    @Provides fun employeeDao(db: PosBahDatabase): EmployeeDao = db.employeeDao()

    @Provides fun bmpClientDao(db: PosBahDatabase): BmpClientDao = db.bmpClientDao()
    @Provides fun bmpInvoiceDao(db: PosBahDatabase): BmpInvoiceDao = db.bmpInvoiceDao()
    @Provides fun bmpProductDao(db: PosBahDatabase): BmpProductDao = db.bmpProductDao()
    @Provides fun bmpMasterProductDao(db: PosBahDatabase): BmpMasterProductDao = db.bmpMasterProductDao()
    @Provides fun bmpPaymentDao(db: PosBahDatabase): BmpPaymentDao = db.bmpPaymentDao()
    @Provides fun bmpCashFlowDao(db: PosBahDatabase): BmpCashFlowDao = db.bmpCashFlowDao()
    @Provides fun bmpSettingsDao(db: PosBahDatabase): BmpSettingsDao = db.bmpSettingsDao()
    @Provides fun bmpEmployeeDao(db: PosBahDatabase): BmpEmployeeDao = db.bmpEmployeeDao()
    @Provides fun bmpPayrollDao(db: PosBahDatabase): BmpPayrollDao = db.bmpPayrollDao()
    @Provides fun bmpBahanBakuDao(db: PosBahDatabase): BmpBahanBakuDao = db.bmpBahanBakuDao()
    @Provides fun bmpBahanBakuItemDao(db: PosBahDatabase): BmpBahanBakuItemDao = db.bmpBahanBakuItemDao()
    @Provides fun printSettingsDao(db: PosBahDatabase): PrintSettingsDao = db.printSettingsDao()

    @Provides fun productDao(db: PosBahDatabase): ProductDao = db.productDao()
    @Provides fun customerDao(db: PosBahDatabase): CustomerDao = db.customerDao()
    @Provides fun transactionDao(db: PosBahDatabase): TransactionDao = db.transactionDao()
    @Provides fun transactionItemDao(db: PosBahDatabase): TransactionItemDao = db.transactionItemDao()
    @Provides fun activityLogDao(db: PosBahDatabase): ActivityLogDao = db.activityLogDao()
}
