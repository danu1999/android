package com.posbah.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.posbah.app.data.local.entities.BmpCashFlowEntity
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpEmployeeEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpPayrollEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        LocalUser::class,
        Tenant::class,
        Outlet::class,
        Employee::class,
        BmpClientEntity::class,
        BmpInvoiceEntity::class,
        BmpProductEntity::class,
        BmpMasterProductEntity::class,
        BmpInvoicePaymentEntity::class,
        BmpCashFlowEntity::class,
        BmpSettingsEntity::class,
        BmpEmployeeEntity::class,
        BmpPayrollEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PosBahDatabase : RoomDatabase() {

    abstract fun localUserDao(): LocalUserDao
    abstract fun tenantDao(): TenantDao
    abstract fun outletDao(): OutletDao
    abstract fun employeeDao(): EmployeeDao

    abstract fun bmpClientDao(): BmpClientDao
    abstract fun bmpInvoiceDao(): BmpInvoiceDao
    abstract fun bmpProductDao(): BmpProductDao
    abstract fun bmpMasterProductDao(): BmpMasterProductDao
    abstract fun bmpPaymentDao(): BmpPaymentDao
    abstract fun bmpCashFlowDao(): BmpCashFlowDao
    abstract fun bmpSettingsDao(): BmpSettingsDao
    abstract fun bmpEmployeeDao(): BmpEmployeeDao
    abstract fun bmpPayrollDao(): BmpPayrollDao

    companion object {
        const val DB_NAME = "posbah.db"

        fun build(context: Context, passphrase: ByteArray): PosBahDatabase {
            // Load SQLCipher native library
            System.loadLibrary("sqlcipher")

            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                PosBahDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // safe for v1; we'll add migrations later
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Run secure pragmas
                        db.execSQL("PRAGMA cipher_memory_security = ON")
                    }
                })
                .build()
        }
    }
}
