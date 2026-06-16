package com.posbah.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps a verified Google account to a local user record. Acts as the root of
 * the tenant tree: each LocalUser may own one Tenant.
 */
@Entity(
    tableName = "local_users",
    indices = [Index(value = ["email"], unique = true)]
)
data class LocalUser(
    @PrimaryKey val googleSub: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val role: String = "OWNER", // OWNER | ADMIN | KASIR
    val tenantId: String? = null,
    val whatsapp: String? = null,
    val isPremium: Boolean = false,
    val businessModeLocked: Boolean = false,
    val registeredAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val apkVersion: String = "2.3.0"
)

/**
 * A Tenant is the multi-tenant boundary. Every data row carries tenantId.
 * Each Google user/owner gets a default tenant on first login.
 */
@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey val id: String,
    val name: String,
    val ownerEmail: String,
    val businessMode: String = "BMP", // BMP | FNB | RENTAL | LAUNDRY
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Multi-outlet: a single Tenant can run multiple physical outlets.
 */
@Entity(
    tableName = "outlets",
    indices = [Index(value = ["tenantId"]), Index(value = ["name", "tenantId"], unique = true)]
)
data class Outlet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val isDefault: Boolean = false,
    val isOpen: Boolean = true,
    val currentEmployee: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Cross-tenant employee directory (PIN-based fallback login).
 * Stored PIN is PBKDF2-hashed via PinHasher.
 */
@Entity(
    tableName = "employees",
    indices = [Index(value = ["tenantId"]), Index(value = ["email", "tenantId"], unique = true)]
)
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val lastPasswordChangeDate: Long = 0L
)
