package com.posbah.app.data.repository

// ─────────────────────────────────────────────────────────────────────────────
// MissingRepositories.kt — Full Online mode
// Berisi repository dan data class yang belum ada di BmpRepositories.kt
// ─────────────────────────────────────────────────────────────────────────────

import android.content.Context
import android.util.Log
import com.posbah.app.data.local.entities.BmpProductionLogEntity
import com.posbah.app.data.remote.api.BmpApiService
import com.posbah.app.security.SecurePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ── PrintSettingsData ─────────────────────────────────────────────────────────

data class PrintSettingsData(
    val id: Long = 0,
    val tenantId: String = "",
    val moduleKey: String = "BMP",
    // ── Struk fields (digunakan PrintConfig.fromEntity) ───────────────────────
    val paperWidth: String = "MM80",
    val useLogo: Boolean = true,
    val headerAlign: String = "CENTER",
    val isColor: Boolean = false,
    val showItemPrice: Boolean = true,
    val footerText: String = "Terima kasih!",
    val bankOwnerName: String = "",
    val bankName: String = "BCA",
    val bankAccountNumber: String = "",
    val logoUrl: String? = null,
    // ── BMP doc fields (opsional) ─────────────────────────────────────────────
    val paperSize: String = "A4",
    val showLogo: Boolean = true,
    val showSignature: Boolean = false,
    val headerText: String? = null,
    val updatedAt: Long = 0
)

// ── BmpProductionLogRepository ────────────────────────────────────────────────

@Singleton
class BmpProductionLogRepository @Inject constructor(
    private val api: BmpApiService,
    private val securePrefs: SecurePreferences
) {
    private val _logs = MutableStateFlow<List<BmpProductionLogEntity>>(emptyList())

    fun observeAll(tenantId: String): Flow<List<BmpProductionLogEntity>> = _logs.asStateFlow()

    suspend fun loadAll(tenantId: String) {
        try {
            val resp = api.getProductionLogs()
            if (resp.isSuccessful) {
                val list = resp.body().orEmpty().map { m ->
                    BmpProductionLogEntity(
                        id = (m["id"] as? Number)?.toLong() ?: 0,
                        tenantId = m["tenantId"] as? String ?: tenantId,
                        masterProductId = (m["masterProductId"] as? Number)?.toLong() ?: 0,
                        quantityProduced = (m["quantityProduced"] as? Number)?.toDouble() ?: 0.0,
                        quantityRejected = (m["quantityRejected"] as? Number)?.toDouble() ?: 0.0,
                        rawMaterialUsedKg = (m["rawMaterialUsedKg"] as? Number)?.toDouble() ?: 0.0,
                        operatorName = m["operatorName"] as? String,
                        productionDate = (m["productionDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                _logs.value = list
            }
        } catch (e: Exception) {
            Log.e("BmpProductionLogRepo", "loadAll error", e)
        }
    }

    suspend fun addProductionLog(
        context: Context,
        log: BmpProductionLogEntity
    ): OnlineWriteResult {
        return try {
            val resp = api.createProductionLog(mapOf(
                "masterProductId" to log.masterProductId,
                "quantityProduced" to log.quantityProduced,
                "quantityRejected" to log.quantityRejected,
                "rawMaterialUsedKg" to log.rawMaterialUsedKg,
                "operatorName" to log.operatorName,
                "productionDate" to log.productionDate
            ))
            if (resp.isSuccessful) {
                loadAll(log.tenantId)
                OnlineWriteResult.Success
            } else {
                OnlineWriteResult.Error(resp.errorBody()?.string() ?: "Gagal simpan log produksi")
            }
        } catch (e: Exception) {
            OnlineWriteResult.Error(e.message ?: "Gagal simpan log produksi")
        }
    }

    suspend fun deleteProductionLog(
        context: Context,
        tenantId: String,
        log: BmpProductionLogEntity
    ): OnlineWriteResult {
        return try {
            val resp = api.deleteProductionLog(log.id)
            if (resp.isSuccessful) {
                loadAll(tenantId)
                OnlineWriteResult.Success
            } else {
                OnlineWriteResult.Error(resp.errorBody()?.string() ?: "Gagal hapus log produksi")
            }
        } catch (e: Exception) {
            OnlineWriteResult.Error(e.message ?: "Gagal hapus log produksi")
        }
    }
}
