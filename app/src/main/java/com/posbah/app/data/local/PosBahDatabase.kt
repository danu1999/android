package com.posbah.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.posbah.app.data.local.entities.BmpBahanBakuEntity
import com.posbah.app.data.local.entities.ActivityLogEntity
import com.posbah.app.data.local.entities.BmpBahanBakuItemEntity
import com.posbah.app.data.local.entities.BmpCashFlowEntity
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpEmployeeEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpPayrollEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.local.entities.PrintSettingsEntity
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
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
        BmpPayrollEntity::class,
        BmpBahanBakuEntity::class,
        BmpBahanBakuItemEntity::class,
        PrintSettingsEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        ActivityLogEntity::class
    ],
    version = 10,
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
    abstract fun bmpBahanBakuDao(): BmpBahanBakuDao
    abstract fun bmpBahanBakuItemDao(): BmpBahanBakuItemDao
    abstract fun printSettingsDao(): PrintSettingsDao

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        const val DB_NAME = "posbah.db"

        /**
         * Migration v5 → v6: Menambah kolom `isSynced` ke semua tabel operasional.
         *
         * Strategi ALTER TABLE ADD COLUMN yang aman — tidak menghapus data yang sudah ada.
         * SQLite tidak mendukung ADD COLUMN dengan constraint NOT NULL tanpa default value,
         * sehingga kita pakai DEFAULT 0 (false) yang setara dengan Boolean = false di Kotlin/Room.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabel yang mendapat kolom isSynced
                val tablesWithIsSynced = listOf(
                    "bmp_clients",
                    "bmp_invoices",
                    "bmp_invoice_payments",
                    "bmp_cashflow",
                    "bmp_payrolls",
                    "bmp_bahan_baku",
                    "bmp_bahan_baku_item"
                )
                for (table in tablesWithIsSynced) {
                    // ALTER TABLE aman: jika kolom sudah ada (unlikely), SQLite akan melempar
                    // exception — ini sengaja agar kita tahu ada state corruption.
                    db.execSQL(
                        "ALTER TABLE `$table` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }

        /**
         * Migration v6 → v7: Menambah kolom foto nota bahan baku.
         *
         * - notaFotoPath: path file JPEG lokal (≤100 KB) hasil kamera, null jika belum foto
         * - notaFotoUrl: URL Cloudinary setelah upload berhasil, null jika belum upload
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `bmp_bahan_baku` ADD COLUMN `notaFotoPath` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `bmp_bahan_baku` ADD COLUMN `notaFotoUrl` TEXT"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `bmp_invoices` ADD COLUMN `receiverSignaturePath` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `bmp_invoices` ADD COLUMN `receiverSignatureUrl` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `bmp_invoices` ADD COLUMN `receiverNameActual` TEXT"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `local_users` ADD COLUMN `isPremium` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `local_users` ADD COLUMN `businessModeLocked` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `activity_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tenantId` TEXT NOT NULL, `action` TEXT NOT NULL, `description` TEXT NOT NULL, `date` INTEGER NOT NULL, `employeeName` TEXT NOT NULL, `appMode` TEXT NOT NULL)"
                )
            }
        }

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
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10) // ← Data AMAN, tidak terhapus
                .fallbackToDestructiveMigration()      // ← Fallback jika dari versi < 5 (install baru)
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
