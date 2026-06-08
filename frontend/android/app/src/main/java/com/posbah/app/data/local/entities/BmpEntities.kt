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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
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
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_cashflow",
    indices = [Index(value = ["tenantId"]), Index(value = ["paymentRefId"])]
)
data class BmpCashFlowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val transactionDate: Long,
    val transactionType: String, // MASUK | KELUAR
    val description: String,
    val amount: Double = 0.0,
    val paymentRefId: Long? = null,
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
    val name: String,
    val position: String? = null,
    val salaryAmount: Double,
    val isActive: Boolean = true,
    val fingerprintPIN: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bmp_payrolls",
    indices = [Index(value = ["tenantId"]), Index(value = ["employeeId"])]
)
data class BmpPayrollEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val employeeId: Long,
    val paymentDate: Long,
    val amount: Double,
    val attendanceCount: Int = 0,
    val dailyRate: Double,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
