package com.posbah.app.data.local.entities

// ─────────────────────────────────────────────────────────────────────────────
// CoreEntities.kt — Full Online mode
// Room @Entity annotations dihapus. Data classes sekarang plain Kotlin classes.
// Semua data disimpan di VPS, tidak ada SQLite lokal.
// File ini dipertahankan agar ViewModel yang belum direfactor tetap compile.
// ─────────────────────────────────────────────────────────────────────────────

data class LocalUser(
    val googleSub: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val role: String = "OWNER",
    val tenantId: String? = null,
    val whatsapp: String? = null,
    val isPremium: Boolean = false,
    val businessModeLocked: Boolean = false,
    val registeredAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val apkVersion: String = "3.0.0"
)

data class Tenant(
    val id: String,
    val name: String,
    val ownerEmail: String,
    val businessMode: String = "BMP",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Outlet(
    val id: Long = 0,
    val tenantId: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val isDefault: Boolean = false,
    val isOpen: Boolean = true,
    val currentEmployee: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class Employee(
    val id: Long = 0,
    val tenantId: String,
    val outletId: Long?,
    val name: String,
    val email: String? = null,
    val role: String = "KASIR",
    val pinHash: String,
    val phone: String? = null,
    val salary: Double = 0.0,
    val isActive: Boolean = true,
    val payPeriod: String = "MONTHLY",
    val lastPaidAt: Long? = null,
    val emailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val passwordChangeCount: Int = 0,
    val lastPasswordChangeDate: Long = 0L,
    val isSynced: Boolean = false
)
