package com.posbah.app.data.remote

import android.content.Context
import android.util.Log
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.remote.api.MigrationApiService
import com.posbah.app.data.remote.api.VerifyTableRequest
import com.posbah.app.security.SecurePreferences
import com.posbah.app.security.KeystoreManager
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import net.sqlcipher.database.SQLiteDatabase
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed class MigrationResult {
    object Success : MigrationResult()
    data class TableFailed(val tableName: String, val error: String) : MigrationResult()
    object Incomplete : MigrationResult()
}

@Singleton
class OnlineMigrationManager @Inject constructor(
    private val apiService: MigrationApiService,
    private val securePrefs: SecurePreferences,
    private val keystoreManager: KeystoreManager
) {

    companion object {
        private const val TAG = "OnlineMigrationManager"
        private const val PREF_VERIFIED_TABLES = "migration_verified_tables_v1"
    }

    enum class TableStatus { PENDING, UPLOADING, UPLOADED, VERIFYING, VERIFIED, FAILED }

    data class TableMigrationState(
        val tableName: String,
        val localCount: Int,
        var status: TableStatus = TableStatus.PENDING,
        var serverCount: Int = 0,
        var error: String? = null
    )

    fun isMigrationNeeded(context: Context): Boolean {
        val localDbFile = context.getDatabasePath("posbah.db")
        val backupFile = java.io.File(localDbFile.path + ".bak")
        val migrationDone = securePrefs.migrationCompleted
        return (localDbFile.exists() || backupFile.exists()) && !migrationDone
    }

    private fun getAlreadyVerifiedTables(context: Context): Set<String> {
        val sharedPrefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getStringSet(PREF_VERIFIED_TABLES, emptySet()) ?: emptySet()
    }

    private fun markTableVerified(context: Context, tableName: String) {
        val sharedPrefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
        val current = getAlreadyVerifiedTables(context).toMutableSet()
        current.add(tableName)
        sharedPrefs.edit().putStringSet(PREF_VERIFIED_TABLES, current).apply()
    }

    suspend fun runMigration(
        context: Context,
        db: PosBahDatabase,
        tenantId: String,
        onTableStateChanged: (List<TableMigrationState>) -> Unit
    ): MigrationResult {
        // Restore posbah.db from backup if main file is missing but backup exists
        val dbFile = context.getDatabasePath("posbah.db")
        val backupFile = java.io.File(dbFile.path + ".bak")
        if (backupFile.exists() && !dbFile.exists()) {
            try {
                dbFile.parentFile?.mkdirs()
                backupFile.copyTo(dbFile, overwrite = true)
                val walBak = java.io.File(dbFile.path + "-wal.bak")
                val walDb = java.io.File(dbFile.path + "-wal")
                if (walBak.exists()) walBak.copyTo(walDb, overwrite = true)
                
                val shmBak = java.io.File(dbFile.path + "-shm.bak")
                val shmDb = java.io.File(dbFile.path + "-shm")
                if (shmBak.exists()) shmBak.copyTo(shmDb, overwrite = true)
                
                Log.i(TAG, "Restored posbah.db from backup for migration processing")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore backup db", e)
            }
        }

        // Create backup of current files before starting migration to guarantee data safety
        if (dbFile.exists()) {
            try {
                val dbWal = java.io.File(dbFile.path + "-wal")
                val dbShm = java.io.File(dbFile.path + "-shm")
                
                dbFile.copyTo(backupFile, overwrite = true)
                if (dbWal.exists()) dbWal.copyTo(java.io.File(dbFile.path + "-wal.bak"), overwrite = true)
                if (dbShm.exists()) dbShm.copyTo(java.io.File(dbFile.path + "-shm.bak"), overwrite = true)
                Log.i(TAG, "Created a backup copy of posbah.db before migration starting")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create database backup", e)
            }
        }

        val userEmail = securePrefs.currentEmail.orEmpty()
        val tableStates = buildTableStates(context, tenantId, userEmail)
        val alreadyVerified = getAlreadyVerifiedTables(context)

        tableStates.forEach { state ->
            if (state.tableName in alreadyVerified) {
                state.status = TableStatus.VERIFIED
            }
        }
        onTableStateChanged(tableStates)

        for (state in tableStates) {
            if (state.status == TableStatus.VERIFIED) continue

            var retryCount = 0
            val maxRetry = 3

            while (retryCount < maxRetry) {
                state.status = TableStatus.UPLOADING
                onTableStateChanged(tableStates)

                val uploadSuccess = if (state.localCount > 0) {
                    uploadSingleTable(context, tenantId, state.tableName, userEmail)
                } else {
                    true
                }

                if (!uploadSuccess) {
                    retryCount++
                    if (retryCount >= maxRetry) {
                        state.status = TableStatus.FAILED
                        state.error = "Upload gagal setelah $maxRetry percobaan"
                        onTableStateChanged(tableStates)
                        return MigrationResult.TableFailed(state.tableName, state.error!!)
                    }
                    delay(2000)
                    continue
                }
                state.status = TableStatus.UPLOADED

                state.status = TableStatus.VERIFYING
                onTableStateChanged(tableStates)

                try {
                    val verifyResp = apiService.verifyTable(VerifyTableRequest(state.tableName, state.localCount))
                    if (verifyResp.isSuccessful && verifyResp.body() != null) {
                        val verifyResult = verifyResp.body()!!
                        if (verifyResult.match || verifyResult.serverCount >= state.localCount) {
                            state.status = TableStatus.VERIFIED
                            state.serverCount = verifyResult.serverCount
                            markTableVerified(context, state.tableName)
                            onTableStateChanged(tableStates)
                            break
                        } else {
                            retryCount++
                            if (retryCount >= maxRetry) {
                                state.status = TableStatus.FAILED
                                state.error = "Verifikasi gagal: lokal=${state.localCount}, server=${verifyResult.serverCount}"
                                onTableStateChanged(tableStates)
                                return MigrationResult.TableFailed(state.tableName, state.error!!)
                            }
                            delay(1000)
                        }
                    } else {
                        retryCount++
                        if (retryCount >= maxRetry) {
                            state.status = TableStatus.FAILED
                            state.error = "Verifikasi gagal dengan HTTP code ${verifyResp.code()}"
                            onTableStateChanged(tableStates)
                            return MigrationResult.TableFailed(state.tableName, state.error!!)
                        }
                        delay(1000)
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount >= maxRetry) {
                        state.status = TableStatus.FAILED
                        state.error = "Koneksi gagal: ${e.localizedMessage}"
                        onTableStateChanged(tableStates)
                        return MigrationResult.TableFailed(state.tableName, state.error!!)
                    }
                    delay(2000)
                }
            }
        }

        val allVerified = tableStates.all { it.status == TableStatus.VERIFIED }
        if (!allVerified) return MigrationResult.Incomplete

        // Hapus file DB lokal dan backup
        try {
            db.close()
        } catch (_: Exception) {}
        listOf(
            "posbah.db", "posbah.db-shm", "posbah.db-wal",
            "posbah.db.bak", "posbah.db-wal.bak", "posbah.db-shm.bak"
        ).forEach {
            try {
                context.getDatabasePath(it).delete()
            } catch (_: Exception) {}
        }

        // Tandai migrasi selesai & bersihkan progress tabel
        securePrefs.migrationCompleted = true
        context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE).edit().clear().apply()

        return MigrationResult.Success
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val chars = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = chars[v ushr 4]
            hexChars[i * 2 + 1] = chars[v and 0x0F]
        }
        return "x'" + String(hexChars) + "'"
    }

    private fun getTableQuery(tableName: String, tenantId: String, userEmail: String): Pair<String, Array<String>> {
        return when (tableName) {
            "local_users" -> {
                Pair("SELECT * FROM `$tableName` WHERE TRIM(LOWER(email)) = TRIM(LOWER(?))", arrayOf(userEmail))
            }
            "tenants" -> {
                Pair("SELECT * FROM `$tableName` WHERE id = ?", arrayOf(tenantId))
            }
            "transaction_items" -> {
                Pair("SELECT * FROM `$tableName` WHERE transactionId IN (SELECT id FROM transactions WHERE tenantId = ?)", arrayOf(tenantId))
            }
            "bmp_bahan_baku_item" -> {
                Pair("SELECT * FROM `$tableName` WHERE bahanBakuId IN (SELECT id FROM bmp_bahan_baku WHERE tenantId = ?)", arrayOf(tenantId))
            }
            "activity_logs" -> {
                Pair("SELECT * FROM `$tableName` WHERE tenantId = ?", arrayOf(tenantId))
            }
            else -> {
                Pair("SELECT * FROM `$tableName` WHERE tenantId = ?", arrayOf(tenantId))
            }
        }
    }

    private fun getLocalRowCount(context: Context, tableName: String, tenantId: String, userEmail: String): Int {
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
            val dbFile = context.getDatabasePath("posbah.db")
            if (!dbFile.exists()) return 0
            val passphrase = keystoreManager.deriveDatabaseKey(context)
            val passphraseStr = bytesToHex(passphrase)
            val openedDb = SQLiteDatabase.openDatabase(dbFile.path, passphraseStr, null, SQLiteDatabase.OPEN_READONLY)
            db = openedDb

            val checkCursor = openedDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
            val tableExists = checkCursor.use { it.count > 0 }
            if (!tableExists) return 0

            val (sql, args) = getTableQuery(tableName, tenantId, userEmail)
            val countSql = "SELECT COUNT(*) FROM ($sql)"
            val c = openedDb.rawQuery(countSql, args)
            cursor = c
            if (c.moveToFirst()) {
                return c.getInt(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting rows for table $tableName: ${e.message}", e)
        } finally {
            cursor?.close()
            db?.close()
        }
        return 0
    }

    private fun readTableFromSqlite(context: Context, tableName: String, tenantId: String, userEmail: String): JSONArray {
        val jsonArray = JSONArray()
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
            val dbFile = context.getDatabasePath("posbah.db")
            if (!dbFile.exists()) return jsonArray

            val passphrase = keystoreManager.deriveDatabaseKey(context)
            val passphraseStr = bytesToHex(passphrase)
            val openedDb = SQLiteDatabase.openDatabase(dbFile.path, passphraseStr, null, SQLiteDatabase.OPEN_READONLY)
            db = openedDb

            val checkCursor = openedDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
            val tableExists = checkCursor.use { it.count > 0 }
            if (!tableExists) return jsonArray

            val (sql, args) = getTableQuery(tableName, tenantId, userEmail)
            val c = openedDb.rawQuery(sql, args)
            cursor = c
            val columnNames = c.columnNames
            while (c.moveToNext()) {
                val row = JSONObject()
                for (i in columnNames.indices) {
                    val colName = columnNames[i]
                    if (c.isNull(i)) {
                        row.put(colName, JSONObject.NULL)
                        continue
                    }
                    when (c.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> {
                            row.put(colName, c.getLong(i))
                        }
                        android.database.Cursor.FIELD_TYPE_FLOAT -> {
                            row.put(colName, c.getDouble(i))
                        }
                        android.database.Cursor.FIELD_TYPE_STRING -> {
                            row.put(colName, c.getString(i))
                        }
                        android.database.Cursor.FIELD_TYPE_BLOB -> {
                            val blob = c.getBlob(i)
                            val base64 = android.util.Base64.encodeToString(blob, android.util.Base64.NO_WRAP)
                            row.put(colName, base64)
                        }
                        else -> {
                            row.put(colName, c.getString(i))
                        }
                    }
                }
                jsonArray.put(row)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading table $tableName from local db: ${e.message}", e)
        } finally {
            cursor?.close()
            db?.close()
        }
        return jsonArray
    }

    private fun buildTableStates(context: Context, tenantId: String, userEmail: String): List<TableMigrationState> {
        return listOf(
            TableMigrationState("local_users",          getLocalRowCount(context, "local_users", tenantId, userEmail)),
            TableMigrationState("tenants",              getLocalRowCount(context, "tenants", tenantId, userEmail)),
            TableMigrationState("outlets",              getLocalRowCount(context, "outlets", tenantId, userEmail)),
            TableMigrationState("employees",            getLocalRowCount(context, "employees", tenantId, userEmail)),
            TableMigrationState("products",             getLocalRowCount(context, "products", tenantId, userEmail)),
            TableMigrationState("customers",            getLocalRowCount(context, "customers", tenantId, userEmail)),
            TableMigrationState("bmp_clients",          getLocalRowCount(context, "bmp_clients", tenantId, userEmail)),
            TableMigrationState("bmp_master_products",  getLocalRowCount(context, "bmp_master_products", tenantId, userEmail)),
            TableMigrationState("bmp_settings",         getLocalRowCount(context, "bmp_settings", tenantId, userEmail)),
            TableMigrationState("bmp_employees",        getLocalRowCount(context, "bmp_employees", tenantId, userEmail)),
            TableMigrationState("print_settings",       getLocalRowCount(context, "print_settings", tenantId, userEmail)),
            TableMigrationState("transactions",         getLocalRowCount(context, "transactions", tenantId, userEmail)),
            TableMigrationState("bmp_invoices",         getLocalRowCount(context, "bmp_invoices", tenantId, userEmail)),
            TableMigrationState("bmp_bahan_baku",       getLocalRowCount(context, "bmp_bahan_baku", tenantId, userEmail)),
            TableMigrationState("bmp_cashflow",         getLocalRowCount(context, "bmp_cashflow", tenantId, userEmail)),
            TableMigrationState("bmp_payrolls",         getLocalRowCount(context, "bmp_payrolls", tenantId, userEmail)),
            TableMigrationState("transaction_items",    getLocalRowCount(context, "transaction_items", tenantId, userEmail)),
            TableMigrationState("bmp_products",         getLocalRowCount(context, "bmp_products", tenantId, userEmail)),
            TableMigrationState("bmp_invoice_payments", getLocalRowCount(context, "bmp_invoice_payments", tenantId, userEmail)),
            TableMigrationState("bmp_bahan_baku_item",  getLocalRowCount(context, "bmp_bahan_baku_item", tenantId, userEmail)),
            TableMigrationState("bmp_product_stocks",   getLocalRowCount(context, "bmp_product_stocks", tenantId, userEmail)),
            TableMigrationState("bmp_stock_ledger",     getLocalRowCount(context, "bmp_stock_ledger", tenantId, userEmail)),
            TableMigrationState("bmp_production_logs",  getLocalRowCount(context, "bmp_production_logs", tenantId, userEmail)),
            TableMigrationState("activity_logs",        getLocalRowCount(context, "activity_logs", tenantId, userEmail))
        )
    }

    private fun uploadSingleTable(context: Context, tenantId: String, tableName: String, userEmail: String): Boolean {
        val jsonArray = readTableFromSqlite(context, tableName, tenantId, userEmail)
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://www.zedmz.cloud/api/sync/$tableName")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                if (userEmail.isNotEmpty()) {
                    setRequestProperty("x-user-email", userEmail)
                }
            }

            conn.outputStream.use { out ->
                out.bufferedWriter().use { writer ->
                    writer.write(jsonArray.toString())
                }
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Server error syncing table $tableName: $errorStream")
            }
            responseCode in 200..299
        } catch (e: IOException) {
            Log.e(TAG, "Gagal upload table $tableName: ${e.localizedMessage}")
            false
        } finally {
            conn?.disconnect()
        }
    }
}
