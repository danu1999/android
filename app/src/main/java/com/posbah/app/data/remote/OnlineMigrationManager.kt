package com.posbah.app.data.remote

import android.content.Context
import android.util.Log
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.remote.api.MigrationApiService
import com.posbah.app.data.remote.api.VerifyTableRequest
import com.posbah.app.security.SecurePreferences
import kotlinx.coroutines.delay
import org.json.JSONArray
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
    private val securePrefs: SecurePreferences
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
        val migrationDone = securePrefs.migrationCompleted
        return localDbFile.exists() && !migrationDone
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
        val tableStates = buildTableStates(db, tenantId)
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
                    uploadSingleTable(context, tenantId, state.tableName, db)
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

        // Hapus file DB lokal
        try {
            db.close()
        } catch (_: Exception) {}
        listOf("posbah.db", "posbah.db-shm", "posbah.db-wal")
            .forEach { context.getDatabasePath(it).delete() }

        // Tandai migrasi selesai & bersihkan progress tabel
        securePrefs.migrationCompleted = true
        context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE).edit().clear().apply()

        return MigrationResult.Success
    }

    private suspend fun buildTableStates(db: PosBahDatabase, tenantId: String): List<TableMigrationState> {
        return listOf(
            TableMigrationState("local_users",          db.localUserDao().getAll().size),
            TableMigrationState("tenants",              db.tenantDao().getAll().size),
            TableMigrationState("outlets",              db.outletDao().listForTenant(tenantId).size),
            TableMigrationState("employees",            db.employeeDao().getAll().size),
            TableMigrationState("products",             db.productDao().list(tenantId).size),
            TableMigrationState("customers",            db.customerDao().list(tenantId).size),
            TableMigrationState("bmp_clients",          db.bmpClientDao().getAll(tenantId).size),
            TableMigrationState("bmp_master_products",  db.bmpMasterProductDao().getAll(tenantId).size),
            TableMigrationState("bmp_settings",         db.bmpSettingsDao().getAll().size),
            TableMigrationState("bmp_employees",        db.bmpEmployeeDao().getAll(tenantId).size),
            TableMigrationState("print_settings",       db.printSettingsDao().getAll().size),
            TableMigrationState("transactions",         db.transactionDao().getAll().filter { it.tenantId == tenantId }.size),
            TableMigrationState("bmp_invoices",         db.bmpInvoiceDao().getAll(tenantId).size),
            TableMigrationState("bmp_bahan_baku",       db.bmpBahanBakuDao().getAll(tenantId).size),
            TableMigrationState("bmp_cashflow",         db.bmpCashFlowDao().getAll(tenantId).size),
            TableMigrationState("bmp_payrolls",         db.bmpPayrollDao().getAll(tenantId).size),
            TableMigrationState("transaction_items",    db.transactionItemDao().getAll().size),
            TableMigrationState("bmp_products",         db.bmpProductDao().getAll(tenantId).size),
            TableMigrationState("bmp_invoice_payments", db.bmpPaymentDao().getAll(tenantId).size),
            TableMigrationState("bmp_bahan_baku_item",  db.bmpBahanBakuItemDao().getAll().size),
            TableMigrationState("bmp_product_stocks",   db.bmpProductStockDao().getAll(tenantId).size),
            TableMigrationState("bmp_stock_ledger",     db.bmpStockLedgerDao().getAll(tenantId).size),
            TableMigrationState("bmp_production_logs",  db.bmpProductionLogDao().getAll(tenantId).size),
            TableMigrationState("activity_logs",        db.activityLogDao().getAll().size)
        )
    }

    private fun uploadSingleTable(context: Context, tenantId: String, tableName: String, db: PosBahDatabase): Boolean {
        val jsonArray = JSONArray()
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://www.zedmz.cloud/api/sync/$tableName")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                val email = securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            conn.outputStream.use { out ->
                out.bufferedWriter().use { writer ->
                    writer.write(jsonArray.toString())
                }
            }

            val responseCode = conn.responseCode
            responseCode in 200..299
        } catch (e: IOException) {
            Log.e(TAG, "Gagal upload table $tableName: ${e.localizedMessage}")
            false
        } finally {
            conn?.disconnect()
        }
    }
}
