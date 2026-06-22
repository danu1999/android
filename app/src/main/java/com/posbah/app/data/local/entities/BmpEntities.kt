package com.posbah.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BMP Module entities (Bah Manufaktur & Pabrik / Invoice & Manufacturing).
 * All entities carry tenantId for multi-tenant isolation and optional outletId.
 */

@Entity(
    tableName = "bmp_clients",
    indices = [Index(value = ["tenantId"]), Index(value = ["slug", "tenantId"], unique = true)]
)
data class BmpClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val clientName: String,
    val saldoTitipan: Double = 0.0,
    val addressLine1: String? = null,
    val clientLogo: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val phoneNumber: String? = null,
    val emailAddress: String? = null,
    val taxNumber: String? = null,
    val uniqueID: String? = null,
    val slug: String? = null,
    val isSynced: Boolean = false,    // false = belum disinkronkan ke cloud
    val isDeleted: Boolean = false,   // soft-delete flag
    val receiverSignatureUrl: String? = null,
    val receiverNameActual: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_invoices",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["clientId"]),
        Index(value = ["slug"], unique = true)
    ]
)
data class BmpInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val clientId: Long? = null,
    val title: String,
    val number: String,
    val dueDate: Long? = null,
    val paymentTerms: String = "14 days",
    val status: String = "DRAFT", // DRAFT, PAID, UNPAID, PARTIAL, OVERDUE
    val notes: String? = null,
    val totalAmount: Double = 0.0,    // sum of products at invoice creation
    val paidAmount: Double = 0.0,     // running tally of payments
    val uniqueID: String? = null,
    val slug: String,
    val isSynced: Boolean = false,    // false = belum disinkronkan ke cloud
    val isDeleted: Boolean = false,   // soft-delete flag
    val receiverSignaturePath: String? = null,
    val receiverSignatureUrl: String? = null,
    val receiverNameActual: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_master_products",
    indices = [Index(value = ["tenantId"]), Index(value = ["title", "tenantId"], unique = true)]
)
data class BmpMasterProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val title: String,
    val description: String? = null,
    val unit: String = "Kg",
    val price: Double = 0.0,
    val beratGram: Double = 0.0,
    val cycleTime: Double = 0.0,
    val cavity: Int = 1,
    val rejectRate: Double = 0.0,
    val uniqueID: String? = null,
    val slug: String? = null,
    val isDeleted: Boolean = false,   // soft-delete flag
    val jenisBahanBaku: String = "",   // Pemetaan ke jenis bijih plastik (misal: PP, HDPE)
    val image: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(
    tableName = "bmp_products",
    indices = [Index(value = ["invoiceId"]), Index(value = ["masterItemID"])]
)
data class BmpProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val invoiceId: Long? = null,
    val masterItemID: Long? = null,
    val title: String,
    val unit: String = "pcs",
    val price: Double = 0.0,
    val jumlahLusin: Double = 1.0,
    val quantity: Double = 0.0,
    val isKhusus: Boolean = false,
    val hargaBeli: Double = 0.0,
    val currency: String = "Rp",
    val uniqueID: String? = null,
    val slug: String? = null,
    val isSynced: Boolean = false,    // false = belum disinkronkan ke cloud
    val isDeleted: Boolean = false,   // soft-delete flag
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_invoice_payments",
    indices = [Index(value = ["invoiceId"])]
)
data class BmpInvoicePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val invoiceId: Long,
    val paymentDate: Long,
    val paymentAmount: Double,
    val paymentMethod: String = "TRANSFER", // CASH, TRANSFER, QRIS
    val notes: String? = null,
    val isSynced: Boolean = false,    // false = belum disinkronkan ke cloud
    val isDeleted: Boolean = false,   // soft-delete flag
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_cashflow",
    indices = [Index(value = ["tenantId"]), Index(value = ["paymentRefId"])]
)
data class BmpCashFlowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,             // Per-outlet cashflow isolation
    val transactionDate: Long,
    val transactionType: String, // MASUK | KELUAR
    val description: String,
    val amount: Double = 0.0,
    val paymentRefId: Long? = null,
    val isSynced: Boolean = false,    // false = belum disinkronkan ke cloud
    val isDeleted: Boolean = false,   // soft-delete flag
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_settings",
    indices = [Index(value = ["tenantId"], unique = true)]
)
data class BmpSettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val clientName: String,
    val clientLogo: String? = null,
    val addressLine1: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val phoneNumber: String? = null,
    val emailAddress: String? = null,
    val taxNumber: String? = null,
    val listrikBulanan: Double = 30_000_000.0,
    val jumlahMesin: Int = 5,
    val jumlahKaryawan: Int = 19,
    val gajiHarian: Double = 80_000.0,
    val hariKerjaSebulan: Int = 26,
    val biayaKarungPer1000: Double = 2_100_000.0,
    val hoursPerDay: Int = 24,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_employees",
    indices = [Index(value = ["tenantId"])]
)
data class BmpEmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val name: String,
    val position: String? = null,
    val salaryAmount: Double,
    val isActive: Boolean = true,
    val fingerprintPIN: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(
    tableName = "bmp_payrolls",
    indices = [Index(value = ["tenantId"]), Index(value = ["employeeId"])]
)
data class BmpPayrollEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val tenantId: String,
    val outletId: Long? = null,             // Per-outlet payroll isolation
    val employeeId: Long,
    val paymentDate: Long,
    val amount: Double,
    val attendanceCount: Int = 0,
    val dailyRate: Double,
    val description: String? = null,
    val isSynced: Boolean = false,    // false = belum disinkronkan ke cloud
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Header transaksi pembelian bahan baku (bijih plastik) dari supplier.
 * Setiap record mewakili satu kali kedatangan bahan dari supplier.
 */
@Entity(
    tableName = "bmp_bahan_baku",
    indices = [Index(value = ["tenantId"]), Index(value = ["noTagihan", "tenantId"], unique = true)]
)
data class BmpBahanBakuEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,             // Lokasi gudang/outlet pembelian bahan
    val tanggal: Long = System.currentTimeMillis(),
    val noTagihan: String,                      // Nomor surat jalan/faktur dari supplier
    val totalHarga: Double = 0.0,               // Total nilai bahan baku masuk (Σ qty × rate)
    val nominal: Double = 0.0,                  // Kas yang langsung dibayar saat bahan datang
    val notes: String? = null,                  // Catatan kualitas/kondisi
    val notaFotoPath: String? = null,           // Path lokal foto nota (JPEG ≤100 KB, sebelum upload)
    val notaFotoUrl: String? = null,            // URL Cloudinary setelah berhasil diupload
    val isSynced: Boolean = false,    // false = belum disinkronkan ke cloud
    val isDeleted: Boolean = false,   // soft-delete flag
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Item detail per jenis bahan baku dalam satu tagihan (One-to-Many → BmpBahanBakuEntity).
 * Rate (Rp/Kg) digunakan sebagai acuan HPP produk plastik.
 */
@Entity(
    tableName = "bmp_bahan_baku_item",
    indices = [Index(value = ["bahanBakuId"])]
)
data class BmpBahanBakuItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val bahanBakuId: Long,                      // FK → BmpBahanBakuEntity.id
    val jenisBahan: String,                     // Contoh: PP, PE, HDPE, LDPE
    val kuantitas: Double = 0.0,
    val unit: String = "Kg",
    val rate: Double = 0.0,                     // Harga beli per Kg (acuan HPP)
    val isSynced: Boolean = false,              // false = belum disinkronkan ke cloud
    val isDeleted: Boolean = false,             // soft-delete flag
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Pengaturan cetak fleksibel per-tenant, per-modul.
 * Masing-masing dokumen (Cetak JPG, Surat Jalan, Invoice PDF, Struk POS)
 * memiliki pengaturan logo, TTD, dan warna yang INDEPENDEN.
 *
 * `moduleKey` memisahkan pengaturan per modul bisnis:
 *   - "BMP"     → Invoice & Manufaktur (4 tab: JPG, Surat Jalan, Invoice, Struk)
 *   - "FNB"     → POS FnB/Restoran (hanya tab Struk POS)
 *   - "LAUNDRY" → POS Laundry (hanya tab Struk POS)
 *   - "RENTAL"  → POS Rental (hanya tab Struk POS)
 *
 * Disimpan satu record per (tenant, modul), bisa diubah kapan saja dari layar
 * Pengaturan Cetak masing-masing modul.
 */
@Entity(
    tableName = "print_settings",
    indices = [Index(value = ["tenantId", "moduleKey"], unique = true)]
)
data class PrintSettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    /**
     * Identifier modul pemilik pengaturan ini.
     * Nilai valid: "BMP" | "FNB" | "LAUNDRY" | "RENTAL"
     */
    val moduleKey: String = "BMP",

    // ─── Cetak JPG ────────────────────────────────────────────────────────────
    /** Tampilkan logo di header JPG */
    val jpgUseLogo: Boolean = true,
    /** Alignment header JPG: "LEFT" | "CENTER" */
    val jpgHeaderAlign: String = "LEFT",
    /** Tampilkan kolom tanda tangan di JPG */
    val jpgUseSignature: Boolean = true,
    /** Nama terang Pengirim untuk JPG */
    val jpgSignatureSenderName: String = "Admin",
    /** Nama terang Penerima untuk JPG */
    val jpgSignatureReceiverName: String = "",
    /** Gambar TTD JPG (data URI PNG Base64) */
    val jpgSignatureDrawnBase64: String? = null,
    /** true = berwarna, false = hitam-putih */
    val jpgIsColor: Boolean = true,

    // ─── Surat Jalan ──────────────────────────────────────────────────────────
    /** Tampilkan logo di header Surat Jalan */
    val sjUseLogo: Boolean = true,
    /** Alignment header Surat Jalan: "LEFT" | "CENTER" */
    val sjHeaderAlign: String = "LEFT",
    /** Tampilkan kolom tanda tangan di Surat Jalan */
    val sjUseSignature: Boolean = true,
    /** Nama terang Pengirim untuk Surat Jalan */
    val sjSignatureSenderName: String = "Admin",
    /** Nama terang Penerima untuk Surat Jalan */
    val sjSignatureReceiverName: String = "",
    /** Gambar TTD Surat Jalan (data URI PNG Base64) */
    val sjSignatureDrawnBase64: String? = null,
    /** true = berwarna, false = hitam-putih */
    val sjIsColor: Boolean = false,

    // ─── Cetak Invoice / Faktur PDF ───────────────────────────────────────────
    /** Tampilkan logo di header Invoice PDF */
    val invoiceUseLogo: Boolean = true,
    /** Alignment header Invoice PDF: "LEFT" | "CENTER" */
    val invoiceHeaderAlign: String = "LEFT",
    /** Tampilkan kolom tanda tangan di Invoice PDF */
    val invoiceUseSignature: Boolean = true,
    /** Nama terang Pengirim untuk Invoice PDF */
    val invoiceSignatureSenderName: String = "Admin",
    /** Nama terang Penerima untuk Invoice PDF */
    val invoiceSignatureReceiverName: String = "",
    /** Gambar TTD Invoice PDF (data URI PNG Base64) */
    val invoiceSignatureDrawnBase64: String? = null,
    /** true = berwarna, false = hitam-putih */
    val invoiceIsColor: Boolean = true,

    // ─── Struk / Receipt POS ──────────────────────────────────────────────────
    /** Lebar kertas struk: "MM80" | "MM58" */
    val receiptPaperWidth: String = "MM80",
    /** Tampilkan logo di struk */
    val receiptUseLogo: Boolean = true,
    /** Alignment header struk: "LEFT" | "CENTER" */
    val receiptHeaderAlign: String = "CENTER",
    /** true = berwarna, false = hitam-putih */
    val receiptIsColor: Boolean = false,
    /** Tampilkan harga satuan per item di struk */
    val receiptShowItemPrice: Boolean = true,
    /** Teks footer struk (misal: terima kasih, wifi password, dsb.) */
    val receiptFooterText: String = "Terima kasih sudah berbelanja!",

    // ─── Desain Template Cetak ────────────────────────────────────────────────
    val jpgTemplateType: String = "MODERN",
    val sjTemplateType: String = "MODERN",
    val invoiceTemplateType: String = "MODERN",

    val bankOwnerName: String = "",
    val bankName: String = "BCA",
    val bankAccountNumber: String = "",

    val logoPath: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_product_stocks",
    indices = [Index(value = ["tenantId"]), Index(value = ["masterProductId", "tenantId"], unique = true)]
)
data class BmpProductStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,             // Stok per lokasi/outlet
    val masterProductId: Long,
    val quantity: Double = 0.0,
    val minStockAlert: Double = 0.0,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_stock_ledger",
    indices = [Index(value = ["tenantId"]), Index(value = ["masterProductId"])]
)
data class BmpStockLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val masterProductId: Long,
    val referenceId: Long,
    val mutationType: String, // "PRODUKSI" | "PENJUALAN" | "PENYESUAIAN"
    val quantityChange: Double,
    val finalStock: Double,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_production_logs",
    indices = [Index(value = ["tenantId"]), Index(value = ["masterProductId"])]
)
data class BmpProductionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val masterProductId: Long,
    val quantityProduced: Double,
    val quantityRejected: Double,
    val rawMaterialUsedKg: Double,
    val operatorName: String? = null,
    val productionDate: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

