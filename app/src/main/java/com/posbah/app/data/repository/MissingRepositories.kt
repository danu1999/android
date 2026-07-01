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
) {
    // Helper/alias properties for backward-compatibility with PrintConfig
    val paperWidth: String get() = receiptPaperWidth
    val useLogo: Boolean get() = receiptUseLogo
    val headerAlign: String get() = receiptHeaderAlign
    val isColor: Boolean get() = receiptIsColor
    val showItemPrice: Boolean get() = receiptShowItemPrice
    val footerText: String get() = receiptFooterText
    // BMP doc fields for backward-compatibility
    val paperSize: String get() = "A4"
    val showLogo: Boolean get() = true
    val showSignature: Boolean get() = false
    val headerText: String? get() = null
}

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
                        rawMaterialId = (m["rawMaterialId"] as? Number)?.toLong() ?: 0L,
                        machineId = (m["machine_id"] as? Number)?.toLong(),
                        isMachineActive = m["is_machine_active"] as? Boolean ?: true,
                        cycleTimeActual = (m["cycle_time_actual"] as? Number)?.toDouble() ?: 0.0,
                        electricityCostActual = (m["electricity_cost_actual"] as? Number)?.toDouble() ?: 0.0,
                        operatorName = m["operatorName"] as? String,
                        productionDate = (m["productionDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        isDeleted = m["isDeleted"] as? Boolean ?: false,
                        createdAt = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }.filter { !it.isDeleted }
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
        val snapshot = _logs.value
        val tempId = -System.currentTimeMillis()
        val tempLog = log.copy(id = tempId)
        _logs.value = snapshot + tempLog
        return try {
            val body = mutableMapOf<String, Any?>(
                "masterProductId" to log.masterProductId,
                "quantityProduced" to log.quantityProduced,
                "quantityRejected" to log.quantityRejected,
                "rawMaterialUsedKg" to log.rawMaterialUsedKg,
                "rawMaterialId" to log.rawMaterialId,
                "operatorName" to log.operatorName,
                "productionDate" to log.productionDate
            )
            if (log.machineId != null) body["machine_id"] = log.machineId
            if (log.cycleTimeActual > 0) body["cycle_time_actual"] = log.cycleTimeActual
            if (log.electricityCostActual > 0) body["electricity_cost_actual"] = log.electricityCostActual
            val resp = api.createProductionLog(body)
            if (resp.isSuccessful) {
                // Extract the real server-assigned ID and replace the temp entry
                val newId = (resp.body()?.get("id") as? Number)?.toLong() ?: 0L
                if (newId > 0) {
                    val savedLog = tempLog.copy(id = newId)
                    _logs.value = _logs.value.map { if (it.id == tempId) savedLog else it }
                } else {
                    // Fallback: reload from server if ID not returned
                    loadAll(log.tenantId)
                }
                OnlineWriteResult.Success
            } else {
                _logs.value = snapshot
                OnlineWriteResult.Error(resp.errorBody()?.string() ?: "Gagal simpan log produksi")
            }
        } catch (e: Exception) {
            _logs.value = snapshot
            OnlineWriteResult.Error(e.message ?: "Gagal simpan log produksi")
        }
    }

    suspend fun deleteProductionLog(
        context: Context,
        tenantId: String,
        log: BmpProductionLogEntity
    ): OnlineWriteResult = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        val snapshot = _logs.value
        _logs.value = snapshot.filter { it.id != log.id }
        try {
            val resp = api.deleteProductionLog(log.id)
            if (resp.isSuccessful) {
                OnlineWriteResult.Success
            } else {
                _logs.value = snapshot
                OnlineWriteResult.Error(resp.errorBody()?.string() ?: "Gagal hapus log produksi")
            }
        } catch (e: Exception) {
            _logs.value = snapshot
            OnlineWriteResult.Error(e.message ?: "Gagal hapus log produksi")
        }
    }
}
