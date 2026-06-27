package com.posbah.app.data.local.entities

// ─────────────────────────────────────────────────────────────────────────────
// FnbEntities.kt — Full Online mode
// Room @Entity annotations dihapus. Data classes sekarang plain Kotlin classes.
// ─────────────────────────────────────────────────────────────────────────────

data class ProductEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val name: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val stock: Int = 0,
    val unit: String = "pcs",
    val barcode: String? = null,
    val category: String = "Umum",
    val wholesaleEnabled: Boolean = false,
    val wholesalePrices: String? = null,
    val variants: String? = null,
    val image: String? = null,
    val minStockAlert: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val wholesalePrice: Double = 0.0,
    val minWholesaleQty: Int = 0,
    val costPriceBreakdown: String? = null,
    val defaultDailyTarget: Int = 0
)

data class CustomerEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class TransactionEntity(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val employeeId: Long,
    val customerId: Long? = null,
    val customerName: String? = null,
    val receiptNumber: String,
    val date: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discountType: String? = null,
    val discountInput: Double = 0.0,
    val discountAmt: Double = 0.0,
    val total: Double = 0.0,
    val discount: Double = 0.0,
    val paymentMethod: String,
    val amountPaid: Double? = null,
    val change: Double? = null,
    val status: String = "COMPLETED",
    val type: String = "SALES",
    val orderStatus: String? = null,
    val dpAmount: Double = 0.0,
    val deliveryDate: Long? = null,
    val queueNumber: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class TransactionItemEntity(
    val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val variantId: Long? = null,
    val variantName: String? = null,
    val quantity: Int,
    val price: Double,
    val costPrice: Double = 0.0,
    val discount: Double = 0.0,
    val note: String? = null
)
