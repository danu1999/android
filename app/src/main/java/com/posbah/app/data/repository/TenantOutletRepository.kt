package com.posbah.app.data.repository

import com.posbah.app.data.remote.api.PosApiService
import com.posbah.app.security.SecurePreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// TenantOutletRepository.kt — Full Online mode
// ─────────────────────────────────────────────────────────────────────────────

data class TenantData(
    val id: String = "",
    val name: String = "",
    val ownerEmail: String = "",
    val businessMode: String = "FNB",
    val isActive: Boolean = true,
    val updatedAt: Long = 0
)

data class OutletData(
    val id: Long = 0,
    val tenantId: String = "",
    val name: String = "",
    val address: String? = null,
    val phone: String? = null,
    val isDefault: Boolean = false,
    val isOpen: Boolean = true,
    val updatedAt: Long = 0
)

fun Map<String, Any?>.toOutletData() = OutletData(
    id = (get("id") as? Number)?.toLong() ?: 0,
    tenantId = get("tenantId") as? String ?: "",
    name = get("name") as? String ?: "",
    address = get("address") as? String,
    phone = get("phone") as? String,
    isDefault = get("isDefault") as? Boolean ?: false,
    isOpen = get("isOpen") as? Boolean ?: true,
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0
)

@Singleton
class TenantRepository @Inject constructor(
    private val securePrefs: SecurePreferences
) {
    // Dalam full online mode, data tenant disimpan di SecurePreferences setelah login.
    // TenantRepository hanya membaca dari session yang aktif.

    fun getCurrentTenantId(): String? = securePrefs.currentTenantId

    fun getCurrentBusinessMode(): String? = securePrefs.currentBusinessMode

    // Digunakan oleh TenantPickerViewModel untuk list tenant dari VPS
    // Data tenant list diambil langsung di ViewModel via AuthRepository.fetchOwnerTenants()
}

@Singleton
class OutletRepository @Inject constructor(
    private val api: PosApiService,
    private val securePrefs: SecurePreferences
) {
    suspend fun list(): List<OutletData> {
        return try {
            val resp = api.getOutlets()
            resp.body()?.map { it.toOutletData() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getById(id: Long): OutletData? = list().find { it.id == id }

    suspend fun create(name: String, address: String? = null, phone: String? = null): Long {
        return try {
            val existing = list()
            val isDefault = existing.isEmpty()
            val resp = api.createOutlet(mapOf(
                "name" to name,
                "address" to address,
                "phone" to phone,
                "isDefault" to isDefault
            ))
            (resp.body()?.get("id") as? Number)?.toLong() ?: 0L
        } catch (_: Exception) { 0L }
    }

    suspend fun update(outlet: OutletData) {
        try {
            api.updateOutlet(outlet.id, mapOf(
                "name" to outlet.name,
                "address" to outlet.address,
                "phone" to outlet.phone,
                "isDefault" to outlet.isDefault,
                "isOpen" to outlet.isOpen
            ))
        } catch (_: Exception) {}
    }

    suspend fun delete(id: Long) {
        try { api.deleteOutlet(id) } catch (_: Exception) {}
    }

    suspend fun setDefault(outletId: Long) {
        try {
            // Reset semua outlet jadi non-default
            val all = list()
            all.forEach { outlet ->
                if (outlet.isDefault && outlet.id != outletId) {
                    api.updateOutlet(outlet.id, mapOf("isDefault" to false))
                }
            }
            api.updateOutlet(outletId, mapOf("isDefault" to true))
            securePrefs.currentOutletId = outletId
        } catch (_: Exception) {}
    }

    fun observe(tenantId: String): kotlinx.coroutines.flow.Flow<List<com.posbah.app.data.local.entities.Outlet>> =
        kotlinx.coroutines.flow.flow {
            emit(list().map {
                com.posbah.app.data.local.entities.Outlet(
                    id = it.id,
                    tenantId = it.tenantId,
                    name = it.name,
                    address = it.address,
                    phone = it.phone,
                    isDefault = it.isDefault,
                    isOpen = it.isOpen,
                    currentEmployee = null,
                    createdAt = it.updatedAt,
                    updatedAt = it.updatedAt,
                    isSynced = true
                )
            })
        }
}
