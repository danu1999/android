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
import com.posbah.app.data.local.dao.BmpProductStockDao
import com.posbah.app.data.local.dao.BmpStockLedgerDao
import com.posbah.app.data.local.dao.BmpProductionLogDao
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
import com.posbah.app.data.local.entities.BmpProductStockEntity
import com.posbah.app.data.local.entities.BmpStockLedgerEntity
import com.posbah.app.data.local.entities.BmpProductionLogEntity
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
        ActivityLogEntity::class,
        BmpProductStockEntity::class,
        BmpStockLedgerEntity::class,
        BmpProductionLogEntity::class
    ],
    version = 28,
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
    abstract fun bmpProductStockDao(): BmpProductStockDao
    abstract fun bmpStockLedgerDao(): BmpStockLedgerDao
    abstract fun bmpProductionLogDao(): BmpProductionLogDao

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

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `outlets` ADD COLUMN `isOpen` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `outlets` ADD COLUMN `currentEmployee` TEXT")
                db.execSQL("ALTER TABLE `employees` ADD COLUMN `payPeriod` TEXT NOT NULL DEFAULT 'MONTHLY'")
                db.execSQL("ALTER TABLE `employees` ADD COLUMN `lastPaidAt` INTEGER")
                db.execSQL("ALTER TABLE `employees` ADD COLUMN `emailVerified` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `jpgTemplateType` TEXT NOT NULL DEFAULT 'MODERN'")
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `sjTemplateType` TEXT NOT NULL DEFAULT 'MODERN'")
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `invoiceTemplateType` TEXT NOT NULL DEFAULT 'MODERN'")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `bankOwnerName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `bankName` TEXT NOT NULL DEFAULT 'BCA'")
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `bankAccountNumber` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `bmp_clients` ADD COLUMN `receiverSignatureUrl` TEXT")
                db.execSQL("ALTER TABLE `bmp_clients` ADD COLUMN `receiverNameActual` TEXT")
            }
        }

        /**
         * Migration v14 → v15: Isolasi pengaturan cetak per modul bisnis.
         *
         * Sebelumnya hanya ada 1 record PrintSettings per tenant (unique pada tenantId).
         * Sekarang setiap modul (BMP, FNB, LAUNDRY, RENTAL) punya record sendiri,
         * sehingga unique index berubah menjadi (tenantId, moduleKey).
         *
         * Langkah migrasi:
         * 1. Tambah kolom `moduleKey` TEXT NOT NULL DEFAULT 'BMP'
         * 2. Recreate tabel dengan index baru (tenantId, moduleKey)
         *    SQLite tidak mendukung DROP INDEX + CREATE UNIQUE INDEX pada tabel yang sama,
         *    jadi kita gunakan strategi copy-alter-drop.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: Tambah kolom moduleKey dengan default 'BMP' (data lama tetap ada)
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `moduleKey` TEXT NOT NULL DEFAULT 'BMP'")

                // Step 2: Buat tabel baru dengan index unik (tenantId, moduleKey)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `print_settings_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tenantId` TEXT NOT NULL,
                        `moduleKey` TEXT NOT NULL DEFAULT 'BMP',
                        `jpgUseLogo` INTEGER NOT NULL DEFAULT 1,
                        `jpgHeaderAlign` TEXT NOT NULL DEFAULT 'LEFT',
                        `jpgUseSignature` INTEGER NOT NULL DEFAULT 1,
                        `jpgSignatureSenderName` TEXT NOT NULL DEFAULT 'Admin',
                        `jpgSignatureReceiverName` TEXT NOT NULL DEFAULT '',
                        `jpgSignatureDrawnBase64` TEXT,
                        `jpgIsColor` INTEGER NOT NULL DEFAULT 1,
                        `sjUseLogo` INTEGER NOT NULL DEFAULT 1,
                        `sjHeaderAlign` TEXT NOT NULL DEFAULT 'LEFT',
                        `sjUseSignature` INTEGER NOT NULL DEFAULT 1,
                        `sjSignatureSenderName` TEXT NOT NULL DEFAULT 'Admin',
                        `sjSignatureReceiverName` TEXT NOT NULL DEFAULT '',
                        `sjSignatureDrawnBase64` TEXT,
                        `sjIsColor` INTEGER NOT NULL DEFAULT 0,
                        `invoiceUseLogo` INTEGER NOT NULL DEFAULT 1,
                        `invoiceHeaderAlign` TEXT NOT NULL DEFAULT 'LEFT',
                        `invoiceUseSignature` INTEGER NOT NULL DEFAULT 1,
                        `invoiceSignatureSenderName` TEXT NOT NULL DEFAULT 'Admin',
                        `invoiceSignatureReceiverName` TEXT NOT NULL DEFAULT '',
                        `invoiceSignatureDrawnBase64` TEXT,
                        `invoiceIsColor` INTEGER NOT NULL DEFAULT 1,
                        `receiptPaperWidth` TEXT NOT NULL DEFAULT 'MM80',
                        `receiptUseLogo` INTEGER NOT NULL DEFAULT 1,
                        `receiptHeaderAlign` TEXT NOT NULL DEFAULT 'CENTER',
                        `receiptIsColor` INTEGER NOT NULL DEFAULT 0,
                        `receiptShowItemPrice` INTEGER NOT NULL DEFAULT 1,
                        `receiptFooterText` TEXT NOT NULL DEFAULT 'Terima kasih sudah berbelanja!',
                        `jpgTemplateType` TEXT NOT NULL DEFAULT 'MODERN',
                        `sjTemplateType` TEXT NOT NULL DEFAULT 'MODERN',
                        `invoiceTemplateType` TEXT NOT NULL DEFAULT 'MODERN',
                        `bankOwnerName` TEXT NOT NULL DEFAULT '',
                        `bankName` TEXT NOT NULL DEFAULT 'BCA',
                        `bankAccountNumber` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Step 3: Copy data lama ke tabel baru (semua jadi moduleKey='BMP')
                db.execSQL("""
                    INSERT INTO `print_settings_new` (
                        `id`, `tenantId`, `moduleKey`, 
                        `jpgUseLogo`, `jpgHeaderAlign`, `jpgUseSignature`, `jpgSignatureSenderName`, `jpgSignatureReceiverName`, `jpgSignatureDrawnBase64`, `jpgIsColor`, 
                        `sjUseLogo`, `sjHeaderAlign`, `sjUseSignature`, `sjSignatureSenderName`, `sjSignatureReceiverName`, `sjSignatureDrawnBase64`, `sjIsColor`, 
                        `invoiceUseLogo`, `invoiceHeaderAlign`, `invoiceUseSignature`, `invoiceSignatureSenderName`, `invoiceSignatureReceiverName`, `invoiceSignatureDrawnBase64`, `invoiceIsColor`, 
                        `receiptPaperWidth`, `receiptUseLogo`, `receiptHeaderAlign`, `receiptIsColor`, `receiptShowItemPrice`, `receiptFooterText`, 
                        `jpgTemplateType`, `sjTemplateType`, `invoiceTemplateType`, 
                        `bankOwnerName`, `bankName`, `bankAccountNumber`, 
                        `createdAt`, `updatedAt`
                    )
                    SELECT 
                        `id`, `tenantId`, `moduleKey`, 
                        `jpgUseLogo`, `jpgHeaderAlign`, `jpgUseSignature`, `jpgSignatureSenderName`, `jpgSignatureReceiverName`, `jpgSignatureDrawnBase64`, `jpgIsColor`, 
                        `sjUseLogo`, `sjHeaderAlign`, `sjUseSignature`, `sjSignatureSenderName`, `sjSignatureReceiverName`, `sjSignatureDrawnBase64`, `sjIsColor`, 
                        `invoiceUseLogo`, `invoiceHeaderAlign`, `invoiceUseSignature`, `invoiceSignatureSenderName`, `invoiceSignatureReceiverName`, `invoiceSignatureDrawnBase64`, `invoiceIsColor`, 
                        `receiptPaperWidth`, `receiptUseLogo`, `receiptHeaderAlign`, `receiptIsColor`, `receiptShowItemPrice`, `receiptFooterText`, 
                        `jpgTemplateType`, `sjTemplateType`, `invoiceTemplateType`, 
                        `bankOwnerName`, `bankName`, `bankAccountNumber`, 
                        `createdAt`, `updatedAt`
                    FROM `print_settings`
                """.trimIndent())

                // Step 4: Drop tabel lama dan rename tabel baru
                db.execSQL("DROP TABLE `print_settings`")
                db.execSQL("ALTER TABLE `print_settings_new` RENAME TO `print_settings`")

                // Step 5: Buat unique index baru
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_print_settings_tenantId_moduleKey` ON `print_settings` (`tenantId`, `moduleKey`)")
            }
        }

        /**
         * Migration v15 → v16: Isolasi pelanggan per outlet.
         *
         * Menambahkan kolom `outletId` ke tabel `customers` agar data pelanggan
         * dapat difilter per outlet. Kolom nullable (INTEGER, default NULL) sehingga
         * data pelanggan lama tetap terbaca di semua outlet (backward compat).
         *
         * Setelah migrasi ini, `CustomerEntity` memiliki outletId dan query
         * `observeForOutlet` / `listForOutlet` berfungsi dengan benar.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tambah kolom outletId nullable ke customers
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `outletId` INTEGER")
                // Recreate index — SQLite memerlukan drop index lama yang berdasarkan (name, tenantId)
                // dan membuat index baru (name, tenantId, outletId)
                db.execSQL("DROP INDEX IF EXISTS `index_customers_name_tenantId`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_name_tenantId_outletId` ON `customers` (`name`, `tenantId`, `outletId`)"
                )
                // Tambah index tambahan untuk outletId
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_customers_outletId` ON `customers` (`outletId`)"
                )
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `employees` ADD COLUMN `phone` TEXT")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `print_settings` ADD COLUMN `logoPath` TEXT")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `employees` ADD COLUMN `passwordChangeCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `employees` ADD COLUMN `lastPasswordChangeDate` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `local_users` ADD COLUMN `apkVersion` TEXT NOT NULL DEFAULT '2.0.3'")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Delete all transactions and product data for a clean slate
                db.execSQL("DELETE FROM `transaction_items`")
                db.execSQL("DELETE FROM `transactions`")
                db.execSQL("DELETE FROM `products`")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Delete all transactions and product data for a clean slate again
                db.execSQL("DELETE FROM `transaction_items`")
                db.execSQL("DELETE FROM `transactions`")
                db.execSQL("DELETE FROM `products`")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `bmp_employees` ADD COLUMN `outletId` INTEGER")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tambah kolom isSynced ke bmp_products agar item produk invoice
                // bisa ditrack sinkronisasinya secara efisien (hanya upload yang belum diupload)
                db.execSQL(
                    "ALTER TABLE `bmp_products` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migration v24 → v25: Soft-delete support untuk semua tabel operasional BMP.
         *
         * Menambah kolom `isDeleted` INTEGER NOT NULL DEFAULT 0 ke semua tabel yang
         * memerlukan mekanisme soft-delete agar data yang dihapus tidak muncul kembali
         * saat pullAll() dari server.
         *
         * Tabel yang mendapat kolom isDeleted:
         * - bmp_clients       (klien/customer BMP)
         * - bmp_invoices      (invoice/faktur)
         * - bmp_master_products (produk master)
         * - bmp_products      (item produk per invoice)
         * - bmp_invoice_payments (pembayaran invoice)
         * - bmp_cashflow      (arus kas)
         * - bmp_bahan_baku    (tagihan bahan baku)
         * - bmp_bahan_baku_item (item bahan baku)
         */
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf(
                    "bmp_clients",
                    "bmp_invoices",
                    "bmp_master_products",
                    "bmp_products",
                    "bmp_invoice_payments",
                    "bmp_cashflow",
                    "bmp_bahan_baku",
                    "bmp_bahan_baku_item",
                    "transactions",  // POS - FnB/Laundry/Rental
                    "products"       // POS catalog products (F&B, Laundry, Rental)
                )
                for (table in tables) {
                    db.execSQL(
                        "ALTER TABLE `$table` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Tambah kolom jenisBahanBaku ke bmp_master_products
                db.execSQL("ALTER TABLE `bmp_master_products` ADD COLUMN `jenisBahanBaku` TEXT NOT NULL DEFAULT ''")

                // 2. Buat tabel bmp_product_stocks
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bmp_product_stocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tenantId` TEXT NOT NULL,
                        `masterProductId` INTEGER NOT NULL,
                        `quantity` REAL NOT NULL DEFAULT 0.0,
                        `minStockAlert` REAL NOT NULL DEFAULT 0.0,
                        `isSynced` INTEGER NOT NULL DEFAULT 0,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bmp_product_stocks_tenantId` ON `bmp_product_stocks` (`tenantId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bmp_product_stocks_masterProductId_tenantId` ON `bmp_product_stocks` (`masterProductId`, `tenantId`)")

                // 3. Buat tabel bmp_stock_ledger
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bmp_stock_ledger` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tenantId` TEXT NOT NULL,
                        `masterProductId` INTEGER NOT NULL,
                        `referenceId` INTEGER NOT NULL,
                        `mutationType` TEXT NOT NULL,
                        `quantityChange` REAL NOT NULL,
                        `finalStock` REAL NOT NULL,
                        `notes` TEXT,
                        `isSynced` INTEGER NOT NULL DEFAULT 0,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bmp_stock_ledger_tenantId` ON `bmp_stock_ledger` (`tenantId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bmp_stock_ledger_masterProductId` ON `bmp_stock_ledger` (`masterProductId`)")

                // 4. Buat tabel bmp_production_logs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bmp_production_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tenantId` TEXT NOT NULL,
                        `masterProductId` INTEGER NOT NULL,
                        `quantityProduced` REAL NOT NULL,
                        `quantityRejected` REAL NOT NULL,
                        `rawMaterialUsedKg` REAL NOT NULL,
                        `operatorName` TEXT,
                        `productionDate` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL DEFAULT 0,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bmp_production_logs_tenantId` ON `bmp_production_logs` (`tenantId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bmp_production_logs_masterProductId` ON `bmp_production_logs` (`masterProductId`)")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `products` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `bmp_master_products` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `bmp_employees` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `outlets` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `employees` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `bmp_master_products` ADD COLUMN `image` TEXT")
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
                .addMigrations(
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                    MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22,
                    MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27,
                    MIGRATION_27_28
                ) // ← Data AMAN, tidak terhapus
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
