package com.posbah.app.data.remote

// ─────────────────────────────────────────────────────────────────────────────
// SupabaseSyncManager.kt — Full Online mode STUB
//
// Semua method ini sudah TIDAK DIGUNAKAN dalam arsitektur full-online.
// File ini hanya dipertahankan agar code yang belum direfactor tetap compile.
//
// Dalam full-online mode:
// - TIDAK ada Room database lokal
// - TIDAK ada sync background
// - Semua data langsung ke VPS via Retrofit
//
// TIDAK HAPUS FILE INI sampai semua ViewModel sudah direfactor.
// ─────────────────────────────────────────────────────────────────────────────

import android.content.Context
import android.util.Log
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity

object SupabaseSyncManager {

    private const val TAG = "SupabaseSyncManager"

    @Volatile
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    sealed class SyncResult {
        object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
        object NoConnection : SyncResult()
    }

    /** No-op — full online: tidak ada lokal data untuk di-pull */
    suspend fun pullAll(context: Context, db: PosBahDatabase, tenantId: String): SyncResult {
        Log.i(TAG, "pullAll() — no-op in full-online mode. tenantId=$tenantId")
        return SyncResult.Success
    }

    /** No-op — full online: tidak ada lokal data untuk di-sync */
    suspend fun syncAll(context: Context, db: PosBahDatabase, tenantId: String): SyncResult {
        Log.i(TAG, "syncAll() — no-op in full-online mode. tenantId=$tenantId")
        return SyncResult.Success
    }

    /** No-op — full online: checkout langsung via TransactionRepository */
    suspend fun checkoutWriteThrough(
        context: Context,
        tenantId: String,
        tx: TransactionEntity,
        lines: List<TransactionItemEntity>
    ): SyncResult {
        Log.w(TAG, "checkoutWriteThrough() — deprecated in full-online mode. Use TransactionRepository.checkout()")
        return SyncResult.Success
    }

    /** No-op */
    fun enqueueFullSync(context: Context, db: PosBahDatabase, tenantId: String, email: String?) {
        Log.i(TAG, "enqueueFullSync() — no-op in full-online mode.")
    }

    /** No-op */
    suspend fun patchRowDirectly(
        context: Context,
        table: String,
        id: Long,
        fields: Map<String, Any?>,
        tenantId: String
    ): SyncResult {
        Log.i(TAG, "patchRowDirectly($table, $id) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun deleteRow(
        context: Context,
        table: String,
        id: Long,
        tenantId: String
    ): SyncResult {
        Log.i(TAG, "deleteRow($table, $id) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun pushProductImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        productId: Long
    ): SyncResult {
        Log.i(TAG, "pushProductImmediate($productId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun deleteProductImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        productId: Long
    ): SyncResult {
        Log.i(TAG, "deleteProductImmediate($productId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun pushCustomerImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        customerId: Long
    ): SyncResult {
        Log.i(TAG, "pushCustomerImmediate($customerId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun fetchAndInsertOwnerTenants(
        context: Context,
        db: PosBahDatabase,
        email: String
    ): SyncResult {
        Log.i(TAG, "fetchAndInsertOwnerTenants($email) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun syncEmployeeWithRawPasswordImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        email: String,
        password: String
    ): SyncResult {
        Log.i(TAG, "syncEmployeeWithRawPasswordImmediate — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun syncEmployeePasswordChangeImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        employeeId: Long,
        newPassword: String
    ): SyncResult {
        Log.i(TAG, "syncEmployeePasswordChangeImmediate — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun pushEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        employeeId: Long,
        ownerEmail: String?
    ): SyncResult {
        Log.i(TAG, "pushEmployeeImmediate($employeeId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun deleteEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        employeeId: Long,
        ownerEmail: String?
    ): SyncResult {
        Log.i(TAG, "deleteEmployeeImmediate($employeeId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun pushBmpEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        employeeId: Long?,
        ownerEmail: String?
    ): SyncResult {
        Log.i(TAG, "pushBmpEmployeeImmediate($employeeId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun deleteBmpEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        employeeId: Long,
        ownerEmail: String?
    ): SyncResult {
        Log.i(TAG, "deleteBmpEmployeeImmediate($employeeId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun pushBmpPayrollImmediate(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        employeeId: Long,
        ownerEmail: String?
    ): SyncResult {
        Log.i(TAG, "pushBmpPayrollImmediate($employeeId) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /**
     * No-op — full online: tidak ada write-through lokal.
     * BmpOnlineWriter masih memanggil ini; dalam full-online mode operasi
     * sudah dilakukan langsung ke VPS via BmpApiService.
     */
    suspend fun uploadRowWriteThrough(
        context: Context,
        table: String,
        array: org.json.JSONArray,
        tenantId: String
    ): SyncResult {
        Log.i(TAG, "uploadRowWriteThrough($table) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /** No-op */
    suspend fun deleteRowWriteThrough(
        context: Context,
        table: String,
        id: Any,
        tenantId: String
    ): SyncResult {
        Log.i(TAG, "deleteRowWriteThrough($table, $id) — no-op in full-online mode.")
        return SyncResult.Success
    }

    /**
     * No-op — returns 0L (tidak ada server sync timestamp dalam full-online mode).
     * MainActivity masih memanggil ini untuk polling logic.
     */
    suspend fun checkServerSyncStatus(context: Context, tenantId: String): Long {
        Log.i(TAG, "checkServerSyncStatus() — no-op in full-online mode.")
        return 0L
    }

    suspend fun uploadLogoToVps(context: Context, bytes: ByteArray, tenantId: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var conn: java.net.HttpURLConnection? = null
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = java.net.URL("https://www.zedmz.cloud/api/upload/logo")
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("X-Tenant-Id", tenantId)

            conn.outputStream.use { os ->
                val writer = os.bufferedWriter(Charsets.UTF_8)
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"tenantId\"\r\n\r\n")
                writer.write("$tenantId\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"logo.png\"\r\n")
                writer.write("Content-Type: image/png\r\n\r\n")
                writer.flush()

                os.write(bytes)
                os.flush()

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(resp)
                if (json.optBoolean("success")) {
                    return@withContext json.optString("url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadLogoToVps error", e)
        } finally {
            conn?.disconnect()
        }
        null
    }

    suspend fun uploadTtdPengirimToVps(context: Context, bytes: ByteArray, tenantId: String, moduleKey: String, docType: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var conn: java.net.HttpURLConnection? = null
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = java.net.URL("https://www.zedmz.cloud/api/upload/ttd-pengirim")
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("X-Tenant-Id", tenantId)

            conn.outputStream.use { os ->
                val writer = os.bufferedWriter(Charsets.UTF_8)
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"tenantId\"\r\n\r\n")
                writer.write("$tenantId\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"moduleKey\"\r\n\r\n")
                writer.write("$moduleKey\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"docType\"\r\n\r\n")
                writer.write("$docType\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"signature.png\"\r\n")
                writer.write("Content-Type: image/png\r\n\r\n")
                writer.flush()

                os.write(bytes)
                os.flush()

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(resp)
                if (json.optBoolean("success")) {
                    return@withContext json.optString("url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadTtdPengirimToVps error", e)
        } finally {
            conn?.disconnect()
        }
        null
    }
}
