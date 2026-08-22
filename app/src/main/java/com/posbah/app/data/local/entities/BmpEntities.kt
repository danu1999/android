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
    val driverId: Long? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val plateNumber: String? = null,
    val ongkirSopir: Double = 0.0,
    val biayaKuli: Double = 0.0,
    val deliveryStatus: String = "PENDING",
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
    val biayaKarungPer1000: Double = 0.0,
    val hoursPerDay: Int = 24,
    val attendanceMode: String = "SUPERVISOR",
    val fingerprintIp: String = "",
    val fingerprintPort: String = "4370",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpEmployeeEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val name: String,
    val position: String? = null,
    val salaryAmount: Double = 0.0,
    val employeeType: String = "OPERATING_EXPENSE",
    val phone: String? = null,
    val email: String? = null,
    val isActive: Boolean = true,
    val fingerprintPIN: String? = null,
    val employeeId: Long? = null,
    val lastPaidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class BmpPayrollEntity(
    val id: Long = 0,
    val tenantId: String,
    val employeeId: Long,
    val employeeName: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val attendanceCount: Int = 0,
    val dailyRate: Double = 0.0,
    val description: String? = null,
    val paymentMethod: String = "TRANSFER",
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


data class BmpBahanBakuEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val tanggal: Long = System.currentTimeMillis(),
    val noTagihan: String,
    val supplier: String? = null,
    val category: String = "BAHAN_BAKU",
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
    /** JSON absensi operator per shift: [{\"employeeId\":1,\"checkIn\":\"07:00\",\"checkOut\":\"15:00\"}] */
    val workersAttendance: String? = null,
    /** Nama shift: PAGI | SORE | MALAM */
    val shiftName: String = "PAGI",
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
    val moldId: Long? = null,
    val maintenanceIntervalHours: Int = 500,
    val lastMaintenanceHours: Double = 0.0,
    val totalOperatingHours: Double = 0.0,
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
    val usageCount: Int = 0,
    val maintenanceIntervalShots: Int = 50000,
    val lastMaintenanceShots: Int = 0,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ─── Absensi Karyawan per Mesin per Shift ───────────────────────────────────

/**
 * Representasi 1 entri absensi karyawan di mesin, disimpan sebagai item dalam JSON array
 * di kolom workers_attendance pada bmp_production_logs.
 */
data class BmpMachineWorkerAttendance(
    val employeeId: Long,
    val employeeName: String = "",
    val checkIn: String = "07:00",   // format "HH:mm"
    val checkOut: String = "15:00"   // format "HH:mm"
)

/** Serialisasi list absensi ke JSON string untuk disimpan ke DB */
fun serializeWorkersAttendance(list: List<BmpMachineWorkerAttendance>): String? {
    if (list.isEmpty()) return null
    return try {
        com.google.gson.Gson().toJson(list)
    } catch (_: Exception) { null }
}

/** Deserialisasi JSON string dari DB ke list absensi */
fun parseWorkersAttendance(json: String?): List<BmpMachineWorkerAttendance> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<List<BmpMachineWorkerAttendance>>() {}.type
        com.google.gson.Gson().fromJson(json, type) ?: emptyList()
    } catch (_: Exception) { emptyList() }
}

// ─── Surat Perintah Kerja (SPK / Work Orders) (v2.19.58) ──────────────────────

data class BmpWorkOrderEntity(
    val id: Long = 0,
    val tenantId: String,
    val spkNumber: String,
    val invoiceId: Long? = null,
    val masterProductId: Long,
    val masterProductName: String? = null,
    val targetQuantity: Double,
    val completedQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val machineId: Long? = null,
    val moldId: Long? = null,
    val startDate: Long = System.currentTimeMillis(),
    val targetCompletionDate: Long? = null,
    val actualCompletionDate: Long? = null,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH, URGENT
    val notes: String? = null,
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ─── Preventive Maintenance Logs (v2.19.58) ──────────────────────────────────

data class BmpMaintenanceLogEntity(
    val id: Long = 0,
    val tenantId: String,
    val assetType: String, // 'MACHINE' atau 'MOLD'
    val assetId: Long,
    val assetName: String? = null,
    val maintenanceDate: Long = System.currentTimeMillis(),
    val serviceType: String = "RUTIN", // 'RUTIN', 'PERBAIKAN', 'PENGGANTIAN_SPAREPART', 'PELUMASAN'
    val cost: Double = 0.0,
    val technicianName: String? = null,
    val notes: String? = null,
    val recordedToCashflow: Boolean = true,
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ─── AR Aging (Laporan Umur Piutang Klien) (v2.19.58) ────────────────────────

data class InvoiceAgingItem(
    val invoiceId: Long = 0,
    val invoiceNumber: String = "",
    val title: String = "",
    val dueDate: Long = 0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remaining: Double = 0.0,
    val overdueDays: Int = 0,
    val bucket: String = "CURRENT", // "CURRENT", "DAYS_1_30", "DAYS_31_60", "DAYS_OVER_60"
    val status: String = "PENDING",
    val createdAt: Long = 0
)

data class ClientAgingGroup(
    val clientId: Long = 0,
    val clientName: String = "",
    val phoneNumber: String = "",
    val totalReceivable: Double = 0.0,
    val currentAmount: Double = 0.0,
    val days1To30: Double = 0.0,
    val days31To60: Double = 0.0,
    val daysOver60: Double = 0.0,
    val oldestOverdueDays: Int = 0,
    val invoices: List<InvoiceAgingItem> = emptyList()
)

data class ArAgingSummary(
    val totalReceivable: Double = 0.0,
    val currentAmount: Double = 0.0,
    val days1To30: Double = 0.0,
    val days31To60: Double = 0.0,
    val daysOver60: Double = 0.0,
    val clientCount: Int = 0,
    val clients: List<ClientAgingGroup> = emptyList()
)

data class BmpDriverEntity(
    val id: Long = 0,
    val tenantId: String = "",
    val name: String = "",
    val phone: String = "",
    val plateNumber: String = "",
    val truckType: String = "",
    val ktpImageUrl: String? = null,
    val truckImageUrl: String? = null,
    val stnkImageUrl: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BmpJobInvitationEntity(
    val id: Long = 0,
    val tenantId: String = "",
    val token: String = "",
    val candidateName: String = "",
    val candidatePhone: String = "",
    val positionTarget: String = "OPERATOR",
    val status: String = "ACTIVE",
    val usedAt: Long? = null,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class BmpJobApplicantEntity(
    val id: Long = 0,
    val tenantId: String = "",
    val invitationId: Long? = null,
    val token: String = "",
    val fullName: String = "",
    val nik: String = "",
    val phone: String = "",
    val email: String? = null,
    val gender: String = "LAKI_LAKI",
    val birthPlaceDate: String = "",
    val address: String = "",
    val positionApplied: String = "OPERATOR",
    val education: String = "SMA/SMK",
    val experience: String? = null,
    val ktpPhotoUrl: String? = null,
    val selfPhotoUrl: String? = null,
    val simPhotoUrl: String? = null,
    val cvPdfUrl: String? = null,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val acceptedEmployeeId: Long? = null,
    val acceptedDriverId: Long? = null,
    val salaryOffer: Double = 0.0,
    val notes: String? = null,
    val appliedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
