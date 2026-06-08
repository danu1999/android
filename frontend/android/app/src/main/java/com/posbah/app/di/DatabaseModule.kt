package com.posbah.app.di

import android.content.Context
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.dao.BmpCashFlowDao
import com.posbah.app.data.local.dao.BmpClientDao
import com.posbah.app.data.local.dao.BmpEmployeeDao
import com.posbah.app.data.local.dao.BmpInvoiceDao
import com.posbah.app.data.local.dao.BmpMasterProductDao
import com.posbah.app.data.local.dao.BmpPaymentDao
import com.posbah.app.data.local.dao.BmpPayrollDao
import com.posbah.app.data.local.dao.BmpProductDao
import com.posbah.app.data.local.dao.BmpSettingsDao
import com.posbah.app.data.local.dao.EmployeeDao
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.OutletDao
import com.posbah.app.data.local.dao.TenantDao
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
}
