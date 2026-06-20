package com.posbah.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing products in the F&B / POS catalog.
 * Supports standard price, cost price, stock, category, barcode, wholesale tiers, and custom variants.
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["outletId"]),
        Index(value = ["barcode"], unique = true)
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val wholesalePrices: String? = null, // JSON String representation of wholesale tiers
    val variants: String? = null, // JSON String representation of variants
    val image: String? = null,
    val minStockAlert: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,  // soft-delete flag
    val isSynced: Boolean = false
)

/**
 * Entity representing local customers for POS tracking.
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["outletId"]),
        Index(value = ["name", "tenantId", "outletId"], unique = true)
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

/**
 * Entity representing local transaction checkout receipts.
 * Uses status PENDING for active order queues.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["outletId"]),
        Index(value = ["employeeId"]),
        Index(value = ["customerId"]),
        Index(value = ["receiptNumber"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val outletId: Long? = null,
    val employeeId: Long,
    val customerId: Long? = null,
    val customerName: String? = null,
    val receiptNumber: String,
    val date: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discountType: String? = null, // "percent" | "nominal"
    val discountInput: Double = 0.0,
    val discountAmt: Double = 0.0,
    val total: Double = 0.0,
    val discount: Double = 0.0, // compatibility mapping
    val paymentMethod: String, // "CASH" | "QRIS" | "TRANSFER" | "HUTANG" | "PENDING"
    val amountPaid: Double? = null,
    val change: Double? = null,
    val status: String = "COMPLETED", // "COMPLETED" | "PENDING" | "CANCELLED"
    val type: String = "SALES", // "SALES" | "BACKDATE"
    val orderStatus: String? = null,
    val dpAmount: Double = 0.0,
    val deliveryDate: Long? = null,
    val queueNumber: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false  // soft-delete flag
)

/**
 * Entity mapping transaction items to checkout receipts.
 */
@Entity(
    tableName = "transaction_items",
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["productId"])
    ]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
