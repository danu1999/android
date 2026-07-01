package com.posbah.app.data.local.entities

// ─────────────────────────────────────────────────────────────────────────────
// BmpEntities.kt — Full Online mode
// Room @Entity annotations dihapus. Data classes sekarang plain Kotlin classes.
// Semua data disimpan di VPS, tidak ada SQLite lokal.
// File ini dipertahankan agar UI/ViewModel yang belum direfactor tetap compile.
// ─────────────────────────────────────────────────────────────────────────────

data class BmpClientEntity(
    val id: Long = 0,
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
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val receiverSignatureUrl: String? = null,
    val receiverNameActual: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpInvoiceEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val clientId: Long? = null,
    val title: String,
    val number: String,
    val dueDate: Long? = null,
    val paymentTerms: String = "14 days",
    val status: String = "DRAFT",
    val notes: String? = null,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val uniqueID: String? = null,
    val slug: String,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val receiverSignaturePath: String? = null,
    val receiverSignatureUrl: String? = null,
    val receiverNameActual: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpMasterProductEntity(
    val id: Long = 0,
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
    val isDeleted: Boolean = false,
    val jenisBahanBaku: String = "",
    val image: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val hppTotalPcs: Double = 0.0,
    val hppLusin: Double = 0.0,
    val machineId: Int? = null,
    val moldId: Int? = null,
    val colorantRatio: Double = 0.0,
    val colorantMaterial: String? = null,
    val colorantType: String = "RATIO"
)

data class BmpProductEntity(
    val id: Long = 0,
    val tenantId: String,
    val invoiceId: Long? = null,
    val masterItemID: Long? = null,
    val title: String,
    val description: String? = null,
    val unit: String = "pcs",
    val price: Double = 0.0,
    val jumlahLusin: Double = 1.0,
    val quantity: Double = 0.0,
    val isKhusus: Boolean = false,
    val hargaBeli: Double = 0.0,
    val currency: String = "Rp",
    val uniqueID: String? = null,
    val slug: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpInvoicePaymentEntity(
    val id: Long = 0,
    val tenantId: String,
    val invoiceId: Long,
    val paymentDate: Long,
    val paymentAmount: Double,
    val paymentMethod: String = "TRANSFER",
    val notes: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BmpCashFlowEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val transactionDate: Long,
    val transactionType: String,
    val description: String,
    val amount: Double = 0.0,
    val costType: String = "OPERATING_EXPENSE",
    val paymentRefId: Long? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BmpSettingsEntity(
    val id: Long = 0,
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

data class BmpEmployeeEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val name: String,
    val position: String? = null,
    val salaryAmount: Double,
    val employeeType: String = "OPERATING_EXPENSE",
    val isActive: Boolean = true,
    val fingerprintPIN: String? = null,
    val employeeId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class BmpPayrollEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tenantId: String,
    val outletId: Long? = null,
    val employeeId: Long,
    val paymentDate: Long,
    val amount: Double,
    val attendanceCount: Int = 0,
    val dailyRate: Double,
    val description: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BmpBahanBakuEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val tanggal: Long = System.currentTimeMillis(),
    val noTagihan: String,
    val supplier: String? = null,
    val totalHarga: Double = 0.0,
    val nominal: Double = 0.0,
    val notes: String? = null,
    val notaFotoPath: String? = null,
    val notaFotoUrl: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpBahanBakuItemEntity(
    val id: Long = 0,
    val tenantId: String,
    val bahanBakuId: Long,
    val jenisBahan: String,
    val kuantitas: Double = 0.0,
    val unit: String = "Kg",
    val rate: Double = 0.0,
    /** JSON string: [{"color":"Merah","rasio":"1"},{"color":"PP Natural","rasio":"9"}] */
    val colorMixture: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PrintSettingsEntity(
    val id: Long = 0,
    val tenantId: String,
    val moduleKey: String = "BMP",
    val jpgUseLogo: Boolean = true,
    val jpgHeaderAlign: String = "LEFT",
    val jpgUseSignature: Boolean = true,
    val jpgSignatureSenderName: String = "Admin",
    val jpgSignatureReceiverName: String = "",
    val jpgSignatureDrawnBase64: String? = null,
    val jpgIsColor: Boolean = true,
    val sjUseLogo: Boolean = true,
    val sjHeaderAlign: String = "LEFT",
    val sjUseSignature: Boolean = true,
    val sjSignatureSenderName: String = "Admin",
    val sjSignatureReceiverName: String = "",
    val sjSignatureDrawnBase64: String? = null,
    val sjIsColor: Boolean = false,
    val invoiceUseLogo: Boolean = true,
    val invoiceHeaderAlign: String = "LEFT",
    val invoiceUseSignature: Boolean = true,
    val invoiceSignatureSenderName: String = "Admin",
    val invoiceSignatureReceiverName: String = "",
    val invoiceSignatureDrawnBase64: String? = null,
    val invoiceIsColor: Boolean = true,
    val receiptPaperWidth: String = "MM80",
    val receiptUseLogo: Boolean = true,
    val receiptHeaderAlign: String = "CENTER",
    val receiptIsColor: Boolean = false,
    val receiptShowItemPrice: Boolean = true,
    val receiptFooterText: String = "Terima kasih sudah berbelanja!",
    val jpgTemplateType: String = "MODERN",
    val sjTemplateType: String = "MODERN",
    val invoiceTemplateType: String = "MODERN",
    val bankOwnerName: String = "",
    val bankName: String = "BCA",
    val bankAccountNumber: String = "",
    val logoPath: String? = null,
    val logoUrl: String? = null,
    val jpgSignatureDrawnUrl: String? = null,
    val sjSignatureDrawnUrl: String? = null,
    val invoiceSignatureDrawnUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpProductStockEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val masterProductId: Long,
    val quantity: Double = 0.0,
    val minStockAlert: Double = 0.0,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpStockLedgerEntity(
    val id: Long = 0,
    val tenantId: String,
    val masterProductId: Long,
    val referenceId: Long,
    val mutationType: String,
    val quantityChange: Double,
    val finalStock: Double,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class BmpProductionLogEntity(
    val id: Long = 0,
    val tenantId: String,
    val masterProductId: Long,
    val quantityProduced: Double,
    val quantityRejected: Double,
    val rawMaterialUsedKg: Double,
    val rawMaterialId: Long = 0L,
    val machineId: Long? = null,
    val isMachineActive: Boolean = true,
    val cycleTimeActual: Double = 0.0,
    val electricityCostActual: Double = 0.0,
    /** JSON campuran warna per shift: [{\"color\":\"Merah\",\"rasio\":\"1\"},{\"color\":\"Natural\",\"rasio\":\"9\"}] */
    val colorMixture: String? = null,
    val operatorName: String? = null,
    val productionDate: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val productId: Long get() = masterProductId
}

data class BmpMachineEntity(
    val id: Long = 0,
    val tenantId: String,
    val name: String,
    val depreciationMonthly: Double = 0.0,
    val powerConsumptionKw: Double = 0.0,
    val electricityCostDaily: Double = 0.0,
    val operatorSalaryMonthly: Double = 0.0,
    val overheadAllocatedMonthly: Double = 0.0,
    val hoursCapacityMonthly: Double = 624.0,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpMoldEntity(
    val id: Long = 0,
    val tenantId: String,
    val name: String,
    val purchasePrice: Double = 0.0,
    val expectedShotsLifetime: Int = 100000,
    val masterProductId: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

