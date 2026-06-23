package com.posbah.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import com.posbah.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * VpsSyncManager
 *
 * Mengelola sinkronisasi database Room lokal ke VPS PostgreSQL REST API.
 * Implementasi murni menggunakan HttpURLConnection standar tanpa dependency eksternal berat.
 */
object SupabaseSyncManager {

    private const val TAG = "VpsSyncManager"
    private const val VPS_URL = "https://www.zedmz.cloud"

    @Volatile
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    private fun notifyConnectionState(online: Boolean) {
        onConnectionStateChanged?.invoke(online)
    }

    @Volatile
    private var currentTenantId: String = ""

    private val syncScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    @Volatile
    private var currentUserEmail: String = ""

    /**
     * Mutex untuk mencegah dua coroutine menjalankan pullAll secara bersamaan
     * (race condition dari multi-ViewModel init).
     */
    private val pullMutex = Mutex()

    sealed class SyncResult {
        object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
        object NoConnection : SyncResult()
    }

    /** Check network connection status */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: run {
            Log.e(TAG, "[Network] ConnectivityManager tidak tersedia")
            notifyConnectionState(false)
            return false
        }
        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            Log.w(TAG, "[Network] Tidak ada jaringan aktif terdeteksi")
            notifyConnectionState(false)
            return false
        }
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: run {
            Log.w(TAG, "[Network] Gagal mengambil NetworkCapabilities")
            notifyConnectionState(false)
            return false
        }
        val isInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (isInternet) {
            val transport = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OTHER"
            }
            Log.i(TAG, "[Network] Internet tersedia via $transport")
        } else {
            Log.w(TAG, "[Network] Koneksi ada tetapi tidak memiliki kemampuan NET_CAPABILITY_INTERNET")
            notifyConnectionState(false)
        }
        return isInternet
    }

    /**
     * Sinkronisasikan satu karyawan baru beserta password mentah untuk dikirimkan via email konfirmasi.
     */
    suspend fun syncEmployeeWithRawPassword(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeEmail: String,
        rawPass: String
    ): Boolean = withContext(Dispatchers.IO) {
        currentTenantId = activeTenantId
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "Sinkronisasi dibatalkan: tidak ada koneksi internet.")
            return@withContext false
        }
        try {
            val emp = db.employeeDao().findByEmail(employeeEmail)
            if (emp == null) {
                Log.e(TAG, "Employee tidak ditemukan di local DB untuk email: $employeeEmail")
                return@withContext false
            }
            val array = JSONArray()
            array.put(JSONObject().apply {
                put("id", emp.id)
                put("tenantId", emp.tenantId)
                put("outletId", emp.outletId ?: JSONObject.NULL)
                put("name", emp.name)
                put("email", emp.email ?: JSONObject.NULL)
                put("role", emp.role)
                put("pinHash", emp.pinHash)
                put("phone", emp.phone ?: JSONObject.NULL)
                put("salary", emp.salary)
                put("isActive", emp.isActive)
                put("payPeriod", emp.payPeriod)
                put("lastPaidAt", emp.lastPaidAt ?: JSONObject.NULL)
                put("emailVerified", emp.emailVerified)
                put("createdAt", emp.createdAt)
                put("updatedAt", emp.updatedAt)
                put("rawPassword", rawPass)
            })
            uploadTable(context, "employees", array)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mensinkronisasikan karyawan baru dengan password mentah: ${e.message}", e)
            false
        }
    }

    /**
     * Sinkronisasikan perubahan password karyawan beserta password mentah dan email owner.
     */
    suspend fun syncEmployeePasswordChange(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeEmail: String,
        rawPass: String,
        ownerEmail: String
    ): Boolean = withContext(Dispatchers.IO) {
        currentTenantId = activeTenantId
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "Sinkronisasi dibatalkan: tidak ada koneksi internet.")
            return@withContext false
        }
        try {
            val emp = db.employeeDao().findByEmail(employeeEmail)
            if (emp == null) {
                Log.e(TAG, "Employee tidak ditemukan di local DB untuk email: $employeeEmail")
                return@withContext false
            }
            val array = JSONArray()
            array.put(JSONObject().apply {
                put("id", emp.id)
                put("tenantId", emp.tenantId)
                put("outletId", emp.outletId ?: JSONObject.NULL)
                put("name", emp.name)
                put("email", emp.email ?: JSONObject.NULL)
                put("role", emp.role)
                put("pinHash", emp.pinHash)
                put("phone", emp.phone ?: JSONObject.NULL)
                put("salary", emp.salary)
                put("isActive", emp.isActive)
                put("payPeriod", emp.payPeriod)
                put("lastPaidAt", emp.lastPaidAt ?: JSONObject.NULL)
                put("emailVerified", emp.emailVerified)
                put("createdAt", emp.createdAt)
                put("updatedAt", emp.updatedAt)
                put("rawPassword", rawPass)
                put("isPasswordChange", true)
                put("ownerEmail", ownerEmail)
            })
            uploadTable(context, "employees", array)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mensinkronisasikan perubahan password: ${e.message}", e)
            false
        }
    }

    /**
     * Jalankan sinkronisasi penuh untuk seluruh data di database lokal.
     */
    suspend fun syncAll(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        userEmail: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        val email = userEmail ?: com.posbah.app.security.SecurePreferences(context).currentEmail.orEmpty()
        val deferred = syncScope.async {
            performSyncAll(context, db, activeTenantId, email)
        }
        try {
            deferred.await()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        }
    }

    private suspend fun performSyncAll(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        email: String
    ): SyncResult {
        currentTenantId = activeTenantId
        currentUserEmail = email
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "Sinkronisasi dibatalkan: tidak ada koneksi internet.")
            return SyncResult.NoConnection
        }

        return try {
            Log.d(TAG, "Memulai sinkronisasi ke VPS...")

            val tenant = db.tenantDao().getById(activeTenantId)
            val isBmp = tenant?.businessMode == "BMP"

            // PHASE 0: Push deletes ke server SEBELUM upload data baru
            // Penting: data yang dihapus harus dikonfirmasi server terlebih dahulu
            // agar pullAll tidak me-restore kembali data yang sudah dihapus.
            pushDeletedRecordsToServer(context, db, activeTenantId)

            // 1. local_users (Metadata - upload all)
            val users = db.localUserDao().getAll().filter { it.tenantId == activeTenantId }
            if (users.isNotEmpty()) {
                val array = JSONArray()
                users.forEach { u ->
                    array.put(JSONObject().apply {
                        put("googleSub", u.googleSub)
                        put("email", u.email)
                        put("displayName", u.displayName ?: JSONObject.NULL)
                        put("photoUrl", u.photoUrl ?: JSONObject.NULL)
                        put("role", u.role)
                        put("tenantId", u.tenantId ?: JSONObject.NULL)
                        put("whatsapp", u.whatsapp ?: JSONObject.NULL)
                        put("isPremium", u.isPremium)
                        put("businessModeLocked", u.businessModeLocked)
                        put("registeredAt", u.registeredAt)
                        put("updatedAt", u.updatedAt)
                        put("lastLoginAt", u.lastLoginAt)
                        put("isActive", u.isActive)
                        put("apkVersion", u.apkVersion)
                    })
                }
                uploadTable(context, "local_users", array)
            }

            // 2. tenants (Metadata - upload all)
            val tenants = db.tenantDao().getAll().filter { it.id == activeTenantId }
            if (tenants.isNotEmpty()) {
                val array = JSONArray()
                tenants.forEach { t ->
                    array.put(JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                        put("ownerEmail", t.ownerEmail)
                        put("businessMode", t.businessMode)
                        put("isActive", t.isActive)
                        put("createdAt", t.createdAt)
                        put("updatedAt", t.updatedAt)
                    })
                }
                uploadTable(context, "tenants", array)
            }

            // 3. outlets (Metadata - filter unsynced)
            val outlets = db.outletDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedOutlets = outlets.filter { !it.isSynced }
            if (unsyncedOutlets.isNotEmpty()) {
                val array = JSONArray()
                unsyncedOutlets.forEach { o ->
                    array.put(JSONObject().apply {
                        put("id", o.id)
                        put("tenantId", o.tenantId)
                        put("name", o.name)
                        put("address", o.address ?: JSONObject.NULL)
                        put("phone", o.phone ?: JSONObject.NULL)
                        put("isDefault", o.isDefault)
                        put("isOpen", o.isOpen)
                        put("currentEmployee", o.currentEmployee ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", o.createdAt)
                        put("updatedAt", o.updatedAt)
                    })
                }
                if (uploadTable(context, "outlets", array)) {
                    unsyncedOutlets.forEach { db.outletDao().markSynced(it.id) }
                }
            }
 
            // 4. employees (Metadata - filter unsynced)
            val employees = db.employeeDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedEmployees = employees.filter { !it.isSynced }
            if (unsyncedEmployees.isNotEmpty()) {
                val array = JSONArray()
                unsyncedEmployees.forEach { e ->
                    array.put(JSONObject().apply {
                        put("id", e.id)
                        put("tenantId", e.tenantId)
                        put("outletId", e.outletId ?: JSONObject.NULL)
                        put("name", e.name)
                        put("email", e.email ?: JSONObject.NULL)
                        put("role", e.role)
                        put("pinHash", e.pinHash)
                        put("phone", e.phone ?: JSONObject.NULL)
                        put("salary", e.salary)
                        put("isActive", e.isActive)
                        put("payPeriod", e.payPeriod)
                        put("lastPaidAt", e.lastPaidAt ?: JSONObject.NULL)
                        put("emailVerified", e.emailVerified)
                        put("isSynced", true)
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
                if (uploadTable(context, "employees", array)) {
                    unsyncedEmployees.forEach { db.employeeDao().markSynced(it.id) }
                }
            }

            if (isBmp) {
            // 5. bmp_clients (Operational - filter unsynced)
            val clients = db.bmpClientDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedClients = clients.filter { !it.isSynced }
            if (unsyncedClients.isNotEmpty()) {
                val array = JSONArray()
                unsyncedClients.forEach { c ->
                    array.put(JSONObject().apply {
                        put("id", c.id)
                        put("tenantId", c.tenantId)
                        put("outletId", c.outletId ?: JSONObject.NULL)
                        put("clientName", c.clientName)
                        put("saldoTitipan", c.saldoTitipan)
                        put("addressLine1", c.addressLine1 ?: JSONObject.NULL)
                        put("clientLogo", c.clientLogo ?: JSONObject.NULL)
                        put("province", c.province ?: JSONObject.NULL)
                        put("postalCode", c.postalCode ?: JSONObject.NULL)
                        put("phoneNumber", c.phoneNumber ?: JSONObject.NULL)
                        put("emailAddress", c.emailAddress ?: JSONObject.NULL)
                        put("taxNumber", c.taxNumber ?: JSONObject.NULL)
                        put("uniqueID", c.uniqueID ?: JSONObject.NULL)
                        put("slug", c.slug ?: JSONObject.NULL)
                        put("receiverSignatureUrl", c.receiverSignatureUrl ?: JSONObject.NULL)
                        put("receiverNameActual", c.receiverNameActual ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
                if (uploadTable(context, "bmp_clients", array)) {
                    unsyncedClients.forEach { db.bmpClientDao().markSynced(it.id) }
                }
            }
 
            // 6. bmp_invoices (Operational - filter unsynced)
            val invoices = db.bmpInvoiceDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedInvoices = invoices.filter { !it.isSynced }
            if (unsyncedInvoices.isNotEmpty()) {
                val array = JSONArray()
                unsyncedInvoices.forEach { i ->
                    array.put(JSONObject().apply {
                        put("id", i.id)
                        put("tenantId", i.tenantId)
                        put("outletId", i.outletId ?: JSONObject.NULL)
                        put("clientId", i.clientId ?: JSONObject.NULL)
                        put("title", i.title)
                        put("number", i.number)
                        put("dueDate", i.dueDate ?: JSONObject.NULL)
                        put("paymentTerms", i.paymentTerms)
                        put("status", i.status)
                        put("notes", i.notes ?: JSONObject.NULL)
                        put("totalAmount", i.totalAmount)
                        put("paidAmount", i.paidAmount)
                        put("uniqueID", i.uniqueID ?: JSONObject.NULL)
                        put("slug", i.slug)
                        put("isSynced", true)
                        put("receiverSignaturePath", i.receiverSignaturePath ?: JSONObject.NULL)
                        put("receiverSignatureUrl", i.receiverSignatureUrl ?: JSONObject.NULL)
                        put("receiverNameActual", i.receiverNameActual ?: JSONObject.NULL)
                        put("createdAt", i.createdAt)
                        put("updatedAt", i.updatedAt)
                    })
                }
                if (uploadTable(context, "bmp_invoices", array)) {
                    unsyncedInvoices.forEach { db.bmpInvoiceDao().markSynced(it.id) }
                }
            }
 
            // 7. bmp_products (Operational - filter unsynced)
            val bmpProducts = db.bmpProductDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBmpProducts = bmpProducts.filter { !it.isSynced }
            if (unsyncedBmpProducts.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBmpProducts.forEach { p ->
                    array.put(JSONObject().apply {
                        put("id", p.id)
                        put("tenantId", p.tenantId)
                        put("invoiceId", p.invoiceId ?: JSONObject.NULL)
                        put("masterItemID", p.masterItemID ?: JSONObject.NULL)
                        put("title", p.title)
                        put("description", p.description ?: JSONObject.NULL)
                        put("unit", p.unit)
                        put("price", p.price)
                        put("jumlahLusin", p.jumlahLusin)
                        put("quantity", p.quantity)
                        put("isKhusus", p.isKhusus)
                        put("hargaBeli", p.hargaBeli)
                        put("currency", p.currency)
                        put("uniqueID", p.uniqueID ?: JSONObject.NULL)
                        put("slug", p.slug ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    })
                }
                if (uploadTable(context, "bmp_products", array)) {
                    unsyncedBmpProducts.forEach { db.bmpProductDao().markSynced(it.id) }
                }
            }
 
            // 8. bmp_master_products (Metadata - filter unsynced)
            val bmpMasterProducts = db.bmpMasterProductDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBmpMasterProducts = bmpMasterProducts.filter { !it.isSynced }
            if (unsyncedBmpMasterProducts.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBmpMasterProducts.forEach { mp ->
                    array.put(JSONObject().apply {
                        put("id", mp.id)
                        put("tenantId", mp.tenantId)
                        put("title", mp.title)
                        put("description", mp.description ?: JSONObject.NULL)
                        put("unit", mp.unit)
                        put("price", mp.price)
                        put("beratGram", mp.beratGram)
                        put("cycleTime", mp.cycleTime)
                        put("cavity", mp.cavity)
                        put("rejectRate", mp.rejectRate)
                        put("uniqueID", mp.uniqueID ?: JSONObject.NULL)
                        put("slug", mp.slug ?: JSONObject.NULL)
                        put("jenisBahanBaku", mp.jenisBahanBaku)
                        put("image", mp.image ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", mp.createdAt)
                        put("updatedAt", mp.updatedAt)
                        put("hppTotalPcs", mp.hppTotalPcs)
                        put("hppLusin", mp.hppLusin)
                    })
                }
                if (uploadTable(context, "bmp_master_products", array)) {
                    unsyncedBmpMasterProducts.forEach { db.bmpMasterProductDao().markSynced(it.id) }
                }
            }
 
            // 9. bmp_invoice_payments (Operational - filter unsynced)
            val bmpPayments = db.bmpPaymentDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedPayments = bmpPayments.filter { !it.isSynced }
            if (unsyncedPayments.isNotEmpty()) {
                val array = JSONArray()
                unsyncedPayments.forEach { p ->
                    array.put(JSONObject().apply {
                        put("id", p.id)
                        put("tenantId", p.tenantId)
                        put("invoiceId", p.invoiceId)
                        put("paymentDate", p.paymentDate)
                        put("paymentAmount", p.paymentAmount)
                        put("paymentMethod", p.paymentMethod)
                        put("notes", p.notes ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", p.createdAt)
                    })
                }
                if (uploadTable(context, "bmp_invoice_payments", array)) {
                    unsyncedPayments.forEach { db.bmpPaymentDao().markSynced(it.id) }
                }
            }
 
            // 10. bmp_cashflow (Operational - filter unsynced)
            val cashflow = db.bmpCashFlowDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedCashflow = cashflow.filter { !it.isSynced }
            if (unsyncedCashflow.isNotEmpty()) {
                val array = JSONArray()
                unsyncedCashflow.forEach { cf ->
                    array.put(JSONObject().apply {
                        put("id", cf.id)
                        put("tenantId", cf.tenantId)
                        put("outletId", cf.outletId ?: JSONObject.NULL)
                        put("transactionDate", cf.transactionDate)
                        put("transactionType", cf.transactionType)
                        put("description", cf.description)
                        put("amount", cf.amount)
                        put("paymentRefId", cf.paymentRefId ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", cf.createdAt)
                    })
                }
                if (uploadTable(context, "bmp_cashflow", array)) {
                    unsyncedCashflow.forEach { db.bmpCashFlowDao().markSynced(it.id) }
                }
            }
 
            // 11. bmp_settings (Metadata - upload all)
            val bmpSettings = db.bmpSettingsDao().getAll().filter { it.tenantId == activeTenantId }
            if (bmpSettings.isNotEmpty()) {
                val array = JSONArray()
                bmpSettings.forEach { s ->
                    array.put(JSONObject().apply {
                        put("id", s.id)
                        put("tenantId", s.tenantId)
                        put("clientName", s.clientName)
                        put("clientLogo", s.clientLogo ?: JSONObject.NULL)
                        put("addressLine1", s.addressLine1 ?: JSONObject.NULL)
                        put("province", s.province ?: JSONObject.NULL)
                        put("postalCode", s.postalCode ?: JSONObject.NULL)
                        put("phoneNumber", s.phoneNumber ?: JSONObject.NULL)
                        put("emailAddress", s.emailAddress ?: JSONObject.NULL)
                        put("taxNumber", s.taxNumber ?: JSONObject.NULL)
                        put("listrikBulanan", s.listrikBulanan)
                        put("jumlahMesin", s.jumlahMesin)
                        put("jumlahKaryawan", s.jumlahKaryawan)
                        put("gajiHarian", s.gajiHarian)
                        put("hariKerjaSebulan", s.hariKerjaSebulan)
                        put("biayaKarungPer1000", s.biayaKarungPer1000)
                        put("hoursPerDay", s.hoursPerDay)
                        put("createdAt", s.createdAt)
                        put("updatedAt", s.updatedAt)
                    })
                }
                uploadTable(context, "bmp_settings", array)
            }
 
            // 12. bmp_employees (Metadata - filter unsynced)
            val bmpEmployees = db.bmpEmployeeDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBmpEmployees = bmpEmployees.filter { !it.isSynced }
            if (unsyncedBmpEmployees.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBmpEmployees.forEach { e ->
                    array.put(JSONObject().apply {
                        put("id", e.id)
                        put("tenantId", e.tenantId)
                        put("outletId", e.outletId ?: JSONObject.NULL)
                        put("name", e.name)
                        put("position", e.position ?: JSONObject.NULL)
                        put("salaryAmount", e.salaryAmount)
                        put("isActive", e.isActive)
                        put("fingerprintPIN", e.fingerprintPIN ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
                if (uploadTable(context, "bmp_employees", array)) {
                    unsyncedBmpEmployees.forEach { db.bmpEmployeeDao().markSynced(it.id) }
                }
            }
 
            // 13. bmp_payrolls (Operational - filter unsynced)
            val payrolls = db.bmpPayrollDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedPayrolls = payrolls.filter { !it.isSynced }
            if (unsyncedPayrolls.isNotEmpty()) {
                val array = JSONArray()
                unsyncedPayrolls.forEach { p ->
                    array.put(JSONObject().apply {
                        put("id", p.id)
                        put("tenantId", p.tenantId)
                        put("outletId", p.outletId ?: JSONObject.NULL)
                        put("employeeId", p.employeeId)
                        put("paymentDate", p.paymentDate)
                        put("amount", p.amount)
                        put("attendanceCount", p.attendanceCount)
                        put("dailyRate", p.dailyRate)
                        put("description", p.description ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", p.createdAt)
                    })
                }
                if (uploadTable(context, "bmp_payrolls", array)) {
                    unsyncedPayrolls.forEach { db.bmpPayrollDao().markSynced(it.id) }
                }
            }
 
            // 14. bmp_bahan_baku (Operational - filter unsynced)
            val bahanBaku = db.bmpBahanBakuDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBahanBaku = bahanBaku.filter { !it.isSynced }
            if (unsyncedBahanBaku.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBahanBaku.forEach { bb ->
                    array.put(JSONObject().apply {
                        put("id", bb.id)
                        put("tenantId", bb.tenantId)
                        put("outletId", bb.outletId ?: JSONObject.NULL)
                        put("tanggal", bb.tanggal)
                        put("noTagihan", bb.noTagihan)
                        put("totalHarga", bb.totalHarga)
                        put("nominal", bb.nominal)
                        put("notes", bb.notes ?: JSONObject.NULL)
                        put("notaFotoPath", bb.notaFotoPath ?: JSONObject.NULL)
                        put("notaFotoUrl", bb.notaFotoUrl ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", bb.createdAt)
                        put("updatedAt", bb.updatedAt)
                    })
                }
                if (uploadTable(context, "bmp_bahan_baku", array)) {
                    unsyncedBahanBaku.forEach { db.bmpBahanBakuDao().markSynced(it.id) }
                }
            }
 
            // 15. bmp_bahan_baku_item (Operational - filter unsynced)
            val bahanBakuItems = db.bmpBahanBakuItemDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBahanBakuItems = bahanBakuItems.filter { !it.isSynced }
            if (unsyncedBahanBakuItems.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBahanBakuItems.forEach { bbi ->
                    array.put(JSONObject().apply {
                        put("id", bbi.id)
                        put("tenantId", bbi.tenantId)
                        put("bahanBakuId", bbi.bahanBakuId)
                        put("jenisBahan", bbi.jenisBahan)
                        put("kuantitas", bbi.kuantitas)
                        put("unit", bbi.unit)
                        put("rate", bbi.rate)
                        put("isSynced", true)
                        put("createdAt", bbi.createdAt)
                    })
                }
                if (uploadTable(context, "bmp_bahan_baku_item", array)) {
                    unsyncedBahanBakuItems.forEach { db.bmpBahanBakuItemDao().markSynced(it.id) }
                }
            }
            }
 
            // 16. print_settings (Metadata - upload all)
            val printSettings = db.printSettingsDao().getAll().filter { it.tenantId == activeTenantId }
            if (printSettings.isNotEmpty()) {
                val array = JSONArray()
                printSettings.forEach { ps ->
                    array.put(JSONObject().apply {
                        put("id", ps.id)
                        put("tenantId", ps.tenantId)
                        put("moduleKey", ps.moduleKey)
                        put("jpgUseLogo", ps.jpgUseLogo)
                        put("jpgHeaderAlign", ps.jpgHeaderAlign)
                        put("jpgUseSignature", ps.jpgUseSignature)
                        put("jpgSignatureSenderName", ps.jpgSignatureSenderName)
                        put("jpgSignatureReceiverName", ps.jpgSignatureReceiverName)
                        put("jpgSignatureDrawnBase64", ps.jpgSignatureDrawnBase64 ?: JSONObject.NULL)
                        put("jpgIsColor", ps.jpgIsColor)
                        put("sjUseLogo", ps.sjUseLogo)
                        put("sjHeaderAlign", ps.sjHeaderAlign)
                        put("sjUseSignature", ps.sjUseSignature)
                        put("sjSignatureSenderName", ps.sjSignatureSenderName)
                        put("sjSignatureReceiverName", ps.sjSignatureReceiverName)
                        put("sjSignatureDrawnBase64", ps.sjSignatureDrawnBase64 ?: JSONObject.NULL)
                        put("sjIsColor", ps.sjIsColor)
                        put("invoiceUseLogo", ps.invoiceUseLogo)
                        put("invoiceHeaderAlign", ps.invoiceHeaderAlign)
                        put("invoiceUseSignature", ps.invoiceUseSignature)
                        put("invoiceSignatureSenderName", ps.invoiceSignatureSenderName)
                        put("invoiceSignatureReceiverName", ps.invoiceSignatureReceiverName)
                        put("invoiceSignatureDrawnBase64", ps.invoiceSignatureDrawnBase64 ?: JSONObject.NULL)
                        put("invoiceIsColor", ps.invoiceIsColor)
                        put("receiptPaperWidth", ps.receiptPaperWidth)
                        put("receiptUseLogo", ps.receiptUseLogo)
                        put("receiptHeaderAlign", ps.receiptHeaderAlign)
                        put("receiptIsColor", ps.receiptIsColor)
                        put("receiptShowItemPrice", ps.receiptShowItemPrice)
                        put("receiptFooterText", ps.receiptFooterText)
                        put("bankOwnerName", ps.bankOwnerName)
                        put("bankName", ps.bankName)
                        put("bankAccountNumber", ps.bankAccountNumber)
                        put("logoPath", ps.logoPath ?: JSONObject.NULL)
                        put("createdAt", ps.createdAt)
                        put("updatedAt", ps.updatedAt)
                    })
                }
                uploadTable(context, "print_settings", array)
            }
 
            if (!isBmp) {
            // 17. products (Metadata - filter unsynced)
            val productsList = db.productDao().getAll().filter { it.tenantId == activeTenantId && !it.isDeleted }
            val unsyncedProducts = productsList.filter { !it.isSynced }
            if (unsyncedProducts.isNotEmpty()) {
                val array = JSONArray()
                unsyncedProducts.forEach { p ->
                    array.put(JSONObject().apply {
                        put("id", p.id)
                        put("tenantId", p.tenantId)
                        put("outletId", p.outletId ?: JSONObject.NULL)
                        put("name", p.name)
                        put("price", p.price)
                        put("costPrice", p.costPrice)
                        put("stock", p.stock)
                        put("unit", p.unit)
                        put("barcode", p.barcode ?: JSONObject.NULL)
                        put("category", p.category)
                        put("wholesaleEnabled", p.wholesaleEnabled)
                        put("wholesalePrices", p.wholesalePrices ?: JSONObject.NULL)
                        put("variants", p.variants ?: JSONObject.NULL)
                        put("image", p.image ?: JSONObject.NULL)
                        put("minStockAlert", p.minStockAlert)
                        put("isSynced", true)
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    })
                }
                if (uploadTable(context, "products", array)) {
                    unsyncedProducts.forEach { db.productDao().markSynced(it.id) }
                }
            }
 
            // 18. customers (Metadata - filter unsynced)
            val customersList = db.customerDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedCustomers = customersList.filter { !it.isSynced }
            if (unsyncedCustomers.isNotEmpty()) {
                val array = JSONArray()
                unsyncedCustomers.forEach { c ->
                    array.put(JSONObject().apply {
                        put("id", c.id)
                        put("tenantId", c.tenantId)
                        put("name", c.name)
                        put("phone", c.phone ?: JSONObject.NULL)
                        put("address", c.address ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
                if (uploadTable(context, "customers", array)) {
                    unsyncedCustomers.forEach { db.customerDao().markSynced(it.id) }
                }
            }
 
            // 19. transactions (Operational / POS Transactions - upload all NON-DELETED)
            val transactionsList = db.transactionDao().getAll().filter { it.tenantId == activeTenantId && !it.isDeleted }
            if (transactionsList.isNotEmpty()) {
                val array = JSONArray()
                transactionsList.forEach { t ->
                    array.put(JSONObject().apply {
                        put("id", t.id)
                        put("tenantId", t.tenantId)
                        put("outletId", t.outletId ?: JSONObject.NULL)
                        put("employeeId", t.employeeId)
                        put("customerId", t.customerId ?: JSONObject.NULL)
                        put("customerName", t.customerName ?: JSONObject.NULL)
                        put("receiptNumber", t.receiptNumber)
                        put("date", t.date)
                        put("subtotal", t.subtotal)
                        put("discountType", t.discountType ?: JSONObject.NULL)
                        put("discountInput", t.discountInput)
                        put("discountAmt", t.discountAmt)
                        put("total", t.total)
                        put("discount", t.discount)
                        put("paymentMethod", t.paymentMethod)
                        put("amountPaid", t.amountPaid ?: JSONObject.NULL)
                        put("change", t.change ?: JSONObject.NULL)
                        put("status", t.status)
                        put("type", t.type)
                        put("orderStatus", t.orderStatus ?: JSONObject.NULL)
                        put("dpAmount", t.dpAmount)
                        put("deliveryDate", t.deliveryDate ?: JSONObject.NULL)
                        put("queueNumber", t.queueNumber ?: JSONObject.NULL)
                        put("notes", t.notes ?: JSONObject.NULL)
                        put("createdAt", t.createdAt)
                        put("updatedAt", t.updatedAt)
                    })
                }
                uploadTable(context, "transactions", array)
            }
 
            // 20. transaction_items (Operational - upload all)
            val allowedTxIds = transactionsList.map { it.id }.toSet()
            val transactionItemsList = db.transactionItemDao().getAll().filter { it.transactionId in allowedTxIds }
            if (transactionItemsList.isNotEmpty()) {
                val array = JSONArray()
                transactionItemsList.forEach { ti ->
                    array.put(JSONObject().apply {
                        put("id", ti.id)
                        put("transactionId", ti.transactionId)
                        put("productId", ti.productId)
                        put("variantId", ti.variantId ?: JSONObject.NULL)
                        put("variantName", ti.variantName ?: JSONObject.NULL)
                        put("quantity", ti.quantity)
                        put("price", ti.price)
                        put("costPrice", ti.costPrice)
                        put("discount", ti.discount)
                        put("note", ti.note ?: JSONObject.NULL)
                    })
                }
                uploadTable(context, "transaction_items", array)
            }
 
            // 21. activity_logs (Logs - upload all)
            val activityLogsList = db.activityLogDao().getAll().filter { it.tenantId == activeTenantId }
            if (activityLogsList.isNotEmpty()) {
                val array = JSONArray()
                activityLogsList.forEach { l ->
                    array.put(JSONObject().apply {
                        put("id", l.id)
                        put("tenantId", l.tenantId)
                        put("outletId", l.outletId ?: JSONObject.NULL)
                        put("action", l.action)
                        put("description", l.description)
                        put("date", l.date)
                        put("employeeName", l.employeeName)
                        put("appMode", l.appMode)
                    })
                }
                uploadTable(context, "activity_logs", array)
            }
            }

            if (isBmp) {
            // 22. bmp_product_stocks (Operational - filter unsynced)
            val bmpProductStocks = db.bmpProductStockDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBmpProductStocks = bmpProductStocks.filter { !it.isSynced }
            if (unsyncedBmpProductStocks.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBmpProductStocks.forEach { s ->
                    array.put(JSONObject().apply {
                        put("id", s.id)
                        put("tenantId", s.tenantId)
                        put("outletId", s.outletId ?: JSONObject.NULL)
                        put("masterProductId", s.masterProductId)
                        put("quantity", s.quantity)
                        put("minStockAlert", s.minStockAlert)
                        put("isSynced", true)
                        put("updatedAt", s.updatedAt)
                    })
                }
                if (uploadTable(context, "bmp_product_stocks", array)) {
                    unsyncedBmpProductStocks.forEach { db.bmpProductStockDao().markSynced(it.id) }
                }
            }

            // 23. bmp_stock_ledger (Operational - filter unsynced)
            val bmpStockLedger = db.bmpStockLedgerDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBmpStockLedger = bmpStockLedger.filter { !it.isSynced }
            if (unsyncedBmpStockLedger.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBmpStockLedger.forEach { l ->
                    array.put(JSONObject().apply {
                        put("id", l.id)
                        put("tenantId", l.tenantId)
                        put("masterProductId", l.masterProductId)
                        put("referenceId", l.referenceId)
                        put("mutationType", l.mutationType)
                        put("quantityChange", l.quantityChange)
                        put("finalStock", l.finalStock)
                        put("notes", l.notes ?: JSONObject.NULL)
                        put("isSynced", true)
                        put("createdAt", l.createdAt)
                    })
                }
                if (uploadTable(context, "bmp_stock_ledger", array)) {
                    unsyncedBmpStockLedger.forEach { db.bmpStockLedgerDao().markSynced(it.id) }
                }
            }

            // 24. bmp_production_logs (Operational - filter unsynced)
            val bmpProductionLogs = db.bmpProductionLogDao().getAll().filter { it.tenantId == activeTenantId }
            val unsyncedBmpProductionLogs = bmpProductionLogs.filter { !it.isSynced }
            if (unsyncedBmpProductionLogs.isNotEmpty()) {
                val array = JSONArray()
                unsyncedBmpProductionLogs.forEach { log ->
                    array.put(JSONObject().apply {
                        put("id", log.id)
                        put("tenantId", log.tenantId)
                        put("masterProductId", log.masterProductId)
                        put("quantityProduced", log.quantityProduced)
                        put("quantityRejected", log.quantityRejected)
                        put("rawMaterialUsedKg", log.rawMaterialUsedKg)
                        put("operatorName", log.operatorName ?: JSONObject.NULL)
                        put("productionDate", log.productionDate)
                        put("isSynced", true)
                        put("createdAt", log.createdAt)
                    })
                }
                if (uploadTable(context, "bmp_production_logs", array)) {
                    unsyncedBmpProductionLogs.forEach { db.bmpProductionLogDao().markSynced(it.id) }
                }
            }
            }
 
            downloadMissingBahanBakuPhotos(context, db, activeTenantId, email)

            Log.d(TAG, "Sinkronisasi selesai dengan sukses.")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sinkronisasi gagal: ${e.message}", e)
            SyncResult.Error(e.message ?: "Error tidak terduga saat sinkronisasi.")
        }
    }

    /**
     * Kirim semua record yang sudah soft-deleted ke server untuk hard-delete.
     * Setelah server mengkonfirmasi delete (HTTP 200), hard-delete dari Room lokal.
     *
     * Urutan hapus sangat penting (child dulu, baru parent) untuk menghindari
     * foreign key constraint error di server.
     */
    private suspend fun pushDeletedRecordsToServer(
        context: Context,
        db: PosBahDatabase,
        tenantId: String
    ) {
        if (!isNetworkAvailable(context)) return

        try {
            val tenant = db.tenantDao().getById(tenantId)
            val isBmp = tenant?.businessMode == "BMP"

            if (isBmp) {
                // ── BMP: urutan child → parent ──────────────────────────────────

                // 1. bmp_products (child invoice)
                val deletedProductIds = db.bmpProductDao().getDeletedIds(tenantId)
            for (id in deletedProductIds) {
                if (deleteRow(context, "bmp_products", id, tenantId)) {
                    db.bmpProductDao().hardDelete(id)
                } else {
                    Log.w(TAG, "[DeletePush] Gagal delete bmp_products id=$id dari server, akan dicoba lagi nanti")
                }
            }

            // 2. bmp_cashflow (child payment)
            val deletedCashFlowIds = db.bmpCashFlowDao().getDeletedIds(tenantId)
            for (id in deletedCashFlowIds) {
                if (deleteRow(context, "bmp_cashflow", id, tenantId)) {
                    db.bmpCashFlowDao().hardDelete(id)
                }
            }

            // 3. bmp_invoice_payments (child invoice)
            val deletedPaymentIds = db.bmpPaymentDao().getDeletedIds(tenantId)
            for (id in deletedPaymentIds) {
                if (deleteRow(context, "bmp_invoice_payments", id, tenantId)) {
                    db.bmpPaymentDao().hardDelete(id)
                }
            }

            // 4. bmp_invoices
            val deletedInvoiceIds = db.bmpInvoiceDao().getDeletedIds(tenantId)
            for (id in deletedInvoiceIds) {
                if (deleteRow(context, "bmp_invoices", id, tenantId)) {
                    db.bmpInvoiceDao().hardDelete(id)
                }
            }

            // 5. bmp_clients
            val deletedClientIds = db.bmpClientDao().getDeletedIds(tenantId)
            for (id in deletedClientIds) {
                if (deleteRow(context, "bmp_clients", id, tenantId)) {
                    db.bmpClientDao().hardDelete(id)
                }
            }

            // 6. bmp_bahan_baku_item (child header)
            val deletedBahanBakuItemIds = db.bmpBahanBakuItemDao().getDeletedIds(tenantId)
            for (id in deletedBahanBakuItemIds) {
                if (deleteRow(context, "bmp_bahan_baku_item", id, tenantId)) {
                    db.bmpBahanBakuItemDao().hardDelete(id)
                }
            }

            // 7. bmp_bahan_baku
            val deletedBahanBakuIds = db.bmpBahanBakuDao().getDeletedIds(tenantId)
            for (id in deletedBahanBakuIds) {
                if (deleteRow(context, "bmp_bahan_baku", id, tenantId)) {
                    db.bmpBahanBakuDao().hardDelete(id)
                }
            }

            // 8. bmp_master_products
            val deletedMasterProductIds = db.bmpMasterProductDao().getDeletedIds(tenantId)
            for (id in deletedMasterProductIds) {
                if (deleteRow(context, "bmp_master_products", id, tenantId)) {
                    db.bmpMasterProductDao().hardDelete(id)
                }
            }

            // 8a. bmp_product_stocks
            val deletedProductStockIds = db.bmpProductStockDao().getDeletedIds(tenantId)
            for (id in deletedProductStockIds) {
                if (deleteRow(context, "bmp_product_stocks", id, tenantId)) {
                    db.bmpProductStockDao().hardDelete(id)
                }
            }

            // 8b. bmp_stock_ledger
            val deletedStockLedgerIds = db.bmpStockLedgerDao().getDeletedIds(tenantId)
            for (id in deletedStockLedgerIds) {
                if (deleteRow(context, "bmp_stock_ledger", id, tenantId)) {
                    db.bmpStockLedgerDao().hardDelete(id)
                }
            }

            // 8c. bmp_production_logs
            val deletedProductionLogIds = db.bmpProductionLogDao().getDeletedIds(tenantId)
            for (id in deletedProductionLogIds) {
                if (deleteRow(context, "bmp_production_logs", id, tenantId)) {
                    db.bmpProductionLogDao().hardDelete(id)
                }
            }
            }

            if (!isBmp) {
                // ── POS: transactions ──────────────────────────────────────────

                // 9. POS products (catalog) yang di-soft-delete
                val deletedPosProductIds = db.productDao().getDeletedIds(tenantId)
            for (id in deletedPosProductIds) {
                if (deleteRow(context, "products", id, tenantId)) {
                    db.productDao().hardDelete(id)
                }
            }

            // 10. transaction_items (child transactions — hapus items dulu)
            val deletedTxIds = db.transactionDao().getDeletedIds(tenantId)
            for (txId in deletedTxIds) {
                // Hapus semua items di server untuk transaksi ini
                val items = db.transactionItemDao().listForTransaction(txId)
                for (item in items) {
                    deleteRow(context, "transaction_items", item.id, tenantId)
                }
                // Hapus items lokal
                db.transactionItemDao().deleteForTransaction(txId)
            }

            // 10. transactions
            for (txId in deletedTxIds) {
                if (deleteRow(context, "transactions", txId, tenantId)) {
                    db.transactionDao().delete(txId)
                }
            }
            }

            Log.d(TAG, "[DeletePush] Selesai push deletes ke server.")
        } catch (e: Exception) {
            Log.e(TAG, "[DeletePush] Error saat push deletes: ${e.message}", e)
            // Non-fatal: lanjutkan syncAll meski delete gagal
        }
    }
 
    /**
     * Upload data JSON array ke endpoint REST VPS tertentu dengan model UPSERT.
     */
    private fun uploadTable(context: Context, tableName: String, jsonArray: JSONArray): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val endpointUrl = "$VPS_URL/api/sync/$tableName"
            val url = URL(endpointUrl)
 
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-tenant-id", currentTenantId)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = currentUserEmail.takeIf { it.isNotBlank() } ?: securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }
 
            conn.outputStream.use { out ->
                val body = jsonArray.toString()
                out.bufferedWriter().use { writer ->
                    writer.write(body)
                }
            }
 
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.d(TAG, "Berhasil mengunggah tabel: $tableName ($responseCode)")
                notifyConnectionState(true)
                true
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal mengunggah tabel: $tableName ($responseCode): $errorStream")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "IO Exception saat mengunggah tabel $tableName: ${e.message}")
            notifyConnectionState(false)
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Upload data ke server VPS secara sinkron (Write-Through) untuk memastikan
     * validasi stok di server berhasil sebelum disimpan secara lokal.
     */
    suspend fun uploadRowWriteThrough(context: Context, tableName: String, jsonArray: JSONArray, tenantId: String): SyncResult = withContext(Dispatchers.IO) {
        currentTenantId = tenantId
        if (!isNetworkAvailable(context)) {
            return@withContext SyncResult.NoConnection
        }
        var conn: HttpURLConnection? = null
        try {
            val endpointUrl = "$VPS_URL/api/sync/$tableName"
            val url = URL(endpointUrl)

            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = currentUserEmail.takeIf { it.isNotBlank() } ?: securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            conn.outputStream.use { out ->
                val body = jsonArray.toString()
                out.bufferedWriter().use { writer ->
                    writer.write(body)
                }
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                notifyConnectionState(true)
                SyncResult.Success
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal mengunggah table write-through: $tableName ($responseCode): $errorStream")

                var errorMsg = "Upload failed ($responseCode)"
                try {
                    val errorObj = JSONObject(errorStream)
                    val detail = errorObj.optString("message", "")
                    if (detail.isNotEmpty()) {
                        errorMsg = detail
                    } else {
                        val err = errorObj.optString("error", "")
                        if (err.isNotEmpty()) errorMsg = err
                    }
                } catch (e: Exception) {
                    if (errorStream.isNotEmpty()) {
                        errorMsg = errorStream
                    }
                }
                SyncResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat mengunggah table write-through $tableName: ${e.message}")
            notifyConnectionState(false)
            SyncResult.Error(e.message ?: "Connection error")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Mengirim data transaksi & item secara sinkron (Write-Through) untuk validasi stok di server.
     */
    suspend fun checkoutWriteThrough(
        context: Context,
        tenantId: String,
        tx: TransactionEntity,
        items: List<TransactionItemEntity>
    ): SyncResult {
        val txJson = JSONObject().apply {
            put("id", tx.id)
            put("tenantId", tenantId)
            put("outletId", tx.outletId ?: JSONObject.NULL)
            put("employeeId", tx.employeeId)
            put("customerId", tx.customerId ?: JSONObject.NULL)
            put("customerName", tx.customerName ?: JSONObject.NULL)
            put("receiptNumber", tx.receiptNumber)
            put("date", tx.date)
            put("subtotal", tx.subtotal)
            put("discountType", tx.discountType ?: JSONObject.NULL)
            put("discountInput", tx.discountInput)
            put("discountAmt", tx.discountAmt)
            put("total", tx.total)
            put("discount", tx.discount)
            put("paymentMethod", tx.paymentMethod)
            put("amountPaid", tx.amountPaid ?: JSONObject.NULL)
            put("change", tx.change ?: JSONObject.NULL)
            put("status", tx.status)
            put("type", tx.type)
            put("orderStatus", tx.orderStatus ?: JSONObject.NULL)
            put("dpAmount", tx.dpAmount)
            put("deliveryDate", tx.deliveryDate ?: JSONObject.NULL)
            put("queueNumber", tx.queueNumber ?: JSONObject.NULL)
            put("notes", tx.notes ?: JSONObject.NULL)
            put("createdAt", tx.createdAt)
            put("updatedAt", tx.updatedAt)
        }

        val itemsArray = JSONArray()
        items.forEach { ti ->
            itemsArray.put(JSONObject().apply {
                put("id", ti.id)
                put("transactionId", ti.transactionId)
                put("productId", ti.productId)
                put("variantId", ti.variantId ?: JSONObject.NULL)
                put("variantName", ti.variantName ?: JSONObject.NULL)
                put("quantity", ti.quantity)
                put("price", ti.price)
                put("costPrice", ti.costPrice)
                put("discount", ti.discount)
                put("note", ti.note ?: JSONObject.NULL)
            })
        }

        val payload = JSONObject().apply {
            put("transaction", txJson)
            put("items", itemsArray)
        }

        val array = JSONArray().apply { put(payload) }
        return uploadRowWriteThrough(context, "checkout", array, tenantId)
    }

    /**
     * Mengirim satu baris data secara sinkron (Write-Through POST) ke server VPS.
     */
    suspend fun postRowDirectly(context: Context, tableName: String, row: JSONObject, tenantId: String): SyncResult {
        val array = JSONArray().apply { put(row) }
        return uploadRowWriteThrough(context, tableName, array, tenantId)
    }

    /**
     * Memperbarui satu baris data secara sinkron (Write-Through PATCH) ke server VPS.
     */
    suspend fun patchRowDirectly(context: Context, tableName: String, id: Long, row: JSONObject, tenantId: String): SyncResult = withContext(Dispatchers.IO) {
        currentTenantId = tenantId
        if (!isNetworkAvailable(context)) {
            return@withContext SyncResult.NoConnection
        }
        var conn: HttpURLConnection? = null
        try {
            val endpointUrl = "$VPS_URL/api/sync/$tableName?id=eq.$id"
            val url = URL(endpointUrl)

            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = currentUserEmail.takeIf { it.isNotBlank() } ?: securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            conn.outputStream.use { out ->
                val body = row.toString()
                out.bufferedWriter().use { writer ->
                    writer.write(body)
                }
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                notifyConnectionState(true)
                SyncResult.Success
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal meng-patch table write-through: $tableName ($responseCode): $errorStream")
                var errorMsg = "Update failed ($responseCode)"
                try {
                    val errorObj = JSONObject(errorStream)
                    val detail = errorObj.optString("message", "")
                    if (detail.isNotEmpty()) {
                        errorMsg = detail
                    } else {
                        val err = errorObj.optString("error", "")
                        if (err.isNotEmpty()) errorMsg = err
                    }
                } catch (e: Exception) {
                    if (errorStream.isNotEmpty()) {
                        errorMsg = errorStream
                    }
                }
                SyncResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat meng-patch table write-through $tableName: ${e.message}")
            notifyConnectionState(false)
            SyncResult.Error(e.message ?: "Connection error")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Hapus satu baris data dari VPS REST API.
     */
    suspend fun deleteRow(context: Context, tableName: String, id: Long, tenantId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "Penghapusan dibatalkan: tidak ada koneksi internet.")
            return@withContext false
        }
        var conn: HttpURLConnection? = null
        try {
            val endpointUrl = "$VPS_URL/api/sync/$tableName?id=eq.$id"
            val url = URL(endpointUrl)

            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = currentUserEmail.takeIf { it.isNotBlank() } ?: securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.d(TAG, "Berhasil menghapus baris dari tabel $tableName di server: ID $id")
                notifyConnectionState(true)
                true
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal menghapus baris dari tabel $tableName di server: ID $id ($responseCode): $errorStream")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat menghapus baris dari tabel $tableName: ${e.message}", e)
            notifyConnectionState(false)
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Hapus satu baris dari VPS secara synchronous dan mengembalikan [SyncResult].
     * Digunakan oleh write-through operations (berbeda dari [deleteRow] yang mengembalikan Boolean).
     */
    suspend fun deleteRowWriteThrough(context: Context, tableName: String, id: Long, tenantId: String): SyncResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            return@withContext SyncResult.NoConnection
        }
        var conn: HttpURLConnection? = null
        try {
            val endpointUrl = "$VPS_URL/api/sync/$tableName?id=eq.$id"
            val url = URL(endpointUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = currentUserEmail.takeIf { it.isNotBlank() } ?: securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                notifyConnectionState(true)
                SyncResult.Success
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "[deleteRowWriteThrough] Gagal hapus $tableName id=$id ($responseCode): $errorStream")
                var errorMsg = "Hapus gagal ($responseCode)"
                try {
                    val obj = org.json.JSONObject(errorStream)
                    val detail = obj.optString("message", "").ifEmpty { obj.optString("error", "") }
                    if (detail.isNotEmpty()) errorMsg = detail
                } catch (_: Exception) {
                    if (errorStream.isNotEmpty()) errorMsg = errorStream
                }
                SyncResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[deleteRowWriteThrough] Exception $tableName id=$id: ${e.message}")
            notifyConnectionState(false)
            SyncResult.Error(e.message ?: "Connection error")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Download data dari VPS REST API.
     */
    private fun pullTable(context: Context, tableName: String, tenantId: String, queryParams: String = ""): JSONArray? {
        var conn: HttpURLConnection? = null
        return try {
            val separator = if (queryParams.isNotEmpty()) "?" else ""
            val endpointUrl = "$VPS_URL/api/sync/$tableName$separator$queryParams"
            val url = URL(endpointUrl)

            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = currentUserEmail.takeIf { it.isNotBlank() } ?: securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                notifyConnectionState(true)
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                JSONArray(responseText)
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal mengunduh tabel: $tableName ($responseCode): $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat mengunduh tabel $tableName: ${e.message}", e)
            notifyConnectionState(false)
            null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun checkServerSyncStatus(context: Context, tenantId: String): Long = withContext(Dispatchers.IO) {
        if (tenantId.isBlank()) return@withContext 0L
        if (!isNetworkAvailable(context)) return@withContext 0L

        var conn: HttpURLConnection? = null
        try {
            val endpointUrl = "$VPS_URL/api/sync/check-status?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}"
            val url = URL(endpointUrl)

            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-tenant-id", tenantId)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = currentUserEmail.takeIf { it.isNotBlank() } ?: securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                notifyConnectionState(true)
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                json.optLong("lastUpdated", 0L)
            } else {
                Log.w(TAG, "Gagal mengecek status sinkronisasi server ($responseCode)")
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat mengecek status sinkronisasi server: ${e.message}")
            notifyConnectionState(false)
            0L
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Jalankan sinkronisasi unduh penuh untuk data Master: outlets, employees, dan products.
     */
    suspend fun pullAll(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        userEmail: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        val email = userEmail ?: com.posbah.app.security.SecurePreferences(context).currentEmail.orEmpty()
        val deferred = syncScope.async {
            pullAllInternal(context, db, activeTenantId, email)
        }
        try {
            deferred.await()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        }
    }

    private suspend fun pullAllInternal(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        email: String
    ): SyncResult = withContext(Dispatchers.IO) {
        currentTenantId = activeTenantId
        currentUserEmail = email
        // Anti race-condition: jika pullAll sudah berjalan, skip tanpa error
        if (!pullMutex.tryLock()) {
            Log.w(TAG, "[pullAll] Sudah berjalan di thread lain, skip untuk menghindari race condition.")
            return@withContext SyncResult.Success
        }
        try {
            if (activeTenantId.isBlank()) {
                Log.w(TAG, "Pull sinkronisasi dibatalkan: tenantId kosong.")
                return@withContext SyncResult.Error("tenantId kosong")
            }
            if (!isNetworkAvailable(context)) {
                Log.w(TAG, "Pull sinkronisasi dibatalkan: tidak ada koneksi internet.")
                return@withContext SyncResult.NoConnection
            }
            Log.d(TAG, "Memulai pull data master dari VPS (paralel)...")

            // Guard: push deletes dulu sebelum pull agar data yang dihapus tidak di-restore
            pushDeletedRecordsToServer(context, db, activeTenantId)

            // ── PHASE 1: Fetch semua tabel secara PARALEL ────────────────────────────
            // Semua HTTP request dijalankan bersamaan, lalu hasil di-await sekaligus.
            // Ini mengurangi waktu pull dari N×RTT menjadi ~1×RTT.
            val tenantsArray: JSONArray?
            val outletsArray: JSONArray?
            val employeesArray: JSONArray?
            val productsArray: JSONArray?
            val customersArray: JSONArray?
            val transactionsArray: JSONArray?
            val bmpClientsArray: JSONArray?
            val bmpInvoicesArray: JSONArray?
            val bmpProductsArray: JSONArray?
            val bmpMasterProductsArray: JSONArray?
            val bmpPaymentsArray: JSONArray?
            val bmpCashFlowArray: JSONArray?
            val bmpSettingsArray: JSONArray?
            val bmpEmployeesArray: JSONArray?
            val bmpPayrollsArray: JSONArray?
            val bmpBahanBakuArray: JSONArray?
            val bmpBahanBakuItemsArray: JSONArray?
            val bmpProductStocksArray: JSONArray?
            val bmpStockLedgerArray: JSONArray?
            val bmpProductionLogsArray: JSONArray?
            val printSettingsArray: JSONArray?
            val activityLogsArray: JSONArray?
            val transactionItemsArray: JSONArray?

            coroutineScope {
                val t1  = async { pullTable(context, "tenants",              activeTenantId) }
                val t2  = async { pullTable(context, "outlets",              activeTenantId) }
                val t3  = async { pullTable(context, "employees",            activeTenantId) }
                val t4  = async { pullTable(context, "products",             activeTenantId) }
                val t5  = async { pullTable(context, "customers",            activeTenantId) }
                val t6  = async { pullTable(context, "transactions",         activeTenantId) }
                val t7  = async { pullTable(context, "bmp_clients",          activeTenantId) }
                val t8  = async { pullTable(context, "bmp_invoices",         activeTenantId) }
                val t9  = async { pullTable(context, "bmp_products",         activeTenantId) }
                val t10 = async { pullTable(context, "bmp_master_products",  activeTenantId) }
                val t11 = async { pullTable(context, "bmp_invoice_payments", activeTenantId) }
                val t12 = async { pullTable(context, "bmp_cashflow",         activeTenantId) }
                val t13 = async { pullTable(context, "bmp_settings",         activeTenantId) }
                val t14 = async { pullTable(context, "bmp_employees",        activeTenantId) }
                val t15 = async { pullTable(context, "bmp_payrolls",         activeTenantId) }
                val t16 = async { pullTable(context, "bmp_bahan_baku",       activeTenantId) }
                val t17 = async { pullTable(context, "bmp_bahan_baku_item",  activeTenantId) }
                val t18 = async { pullTable(context, "bmp_product_stocks",   activeTenantId) }
                val t19 = async { pullTable(context, "bmp_stock_ledger",     activeTenantId) }
                val t20 = async { pullTable(context, "bmp_production_logs",  activeTenantId) }
                val t21 = async { pullTable(context, "print_settings",       activeTenantId) }
                val t22 = async { pullTable(context, "activity_logs",        activeTenantId) }
                val t23 = async { pullTable(context, "transaction_items",    activeTenantId) }

                tenantsArray           = t1.await()
                outletsArray           = t2.await()
                employeesArray         = t3.await()
                productsArray          = t4.await()
                customersArray         = t5.await()
                transactionsArray      = t6.await()
                bmpClientsArray        = t7.await()
                bmpInvoicesArray       = t8.await()
                bmpProductsArray       = t9.await()
                bmpMasterProductsArray = t10.await()
                bmpPaymentsArray       = t11.await()
                bmpCashFlowArray       = t12.await()
                bmpSettingsArray       = t13.await()
                bmpEmployeesArray      = t14.await()
                bmpPayrollsArray       = t15.await()
                bmpBahanBakuArray      = t16.await()
                bmpBahanBakuItemsArray = t17.await()
                bmpProductStocksArray  = t18.await()
                bmpStockLedgerArray    = t19.await()
                bmpProductionLogsArray = t20.await()
                printSettingsArray     = t21.await()
                activityLogsArray      = t22.await()
                transactionItemsArray  = t23.await()
            }

            // Parse transactionsArray and fetch transaction items in parallel before entering the DB transaction
            val activeTxIds = mutableListOf<Long>()
            val transactionEntities = mutableListOf<TransactionEntity>()
            if (transactionsArray != null) {
                for (i in 0 until transactionsArray.length()) {
                    val obj = transactionsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    val outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId")
                    val customerId = if (obj.isNull("customerId")) null else obj.optLong("customerId")
                    val amountPaid = if (obj.isNull("amountPaid")) null else obj.optDouble("amountPaid")
                    val change = if (obj.isNull("change")) null else obj.optDouble("change")
                    
                    val status = obj.optString("status", "COMPLETED")
                    val orderStatus = if (obj.isNull("orderStatus")) null else obj.optString("orderStatus")
                    val receiptNumber = obj.optString("receiptNumber")
                    val paymentMethod = obj.optString("paymentMethod", "CASH")
                    
                    val isRentalActive = receiptNumber.startsWith("RN-") && (orderStatus == "ACTIVE" || paymentMethod != "CASH" || status == "PENDING")
                    val isLaundryActive = receiptNumber.startsWith("LD-") && (orderStatus != "DIAMBIL" || paymentMethod != "CASH" || status == "PENDING")
                    if (isRentalActive || isLaundryActive) {
                        activeTxIds.add(idVal)
                    }

                    transactionEntities.add(TransactionEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = outletId,
                        employeeId = obj.optLong("employeeId", 1L),
                        customerId = customerId,
                        customerName = if (obj.isNull("customerName")) null else obj.optString("customerName"),
                        receiptNumber = receiptNumber,
                        date = obj.optLong("date", System.currentTimeMillis()),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        discountType = if (obj.isNull("discountType")) null else obj.optString("discountType"),
                        discountInput = obj.optDouble("discountInput", 0.0),
                        discountAmt = obj.optDouble("discountAmt", 0.0),
                        total = obj.optDouble("total", 0.0),
                        discount = obj.optDouble("discount", 0.0),
                        paymentMethod = paymentMethod,
                        amountPaid = amountPaid,
                        change = change,
                        status = status,
                        type = obj.optString("type", "SALES"),
                        orderStatus = orderStatus,
                        dpAmount = obj.optDouble("dpAmount", 0.0),
                        deliveryDate = if (obj.isNull("deliveryDate")) null else obj.optLong("deliveryDate"),
                        queueNumber = if (obj.isNull("queueNumber")) null else obj.optInt("queueNumber"),
                        notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
            }

            Log.d(TAG, "[pullAll] Semua fetch paralel selesai, mulai menulis ke DB...")

            // ── PHASE 2: Tulis hasil ke DB secara SEQUENTIAL ────────────────────────
            db.withTransaction {

            // 0. Tenants
            if (tenantsArray != null && tenantsArray.length() > 0) {
                for (i in 0 until tenantsArray.length()) {
                    val obj = tenantsArray.getJSONObject(i)
                    val idVal = obj.getString("id")
                    if (idVal == activeTenantId) {
                        val serverTenant = Tenant(
                            id = idVal,
                            name = obj.getString("name"),
                            ownerEmail = obj.getString("ownerEmail"),
                            businessMode = obj.getString("businessMode"),
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                        val localTenant = db.tenantDao().getById(activeTenantId)
                        if (localTenant == null || serverTenant.updatedAt > localTenant.updatedAt) {
                            db.tenantDao().upsert(serverTenant)
                        }
                    }
                }
            }

            // 1. Outlets
            if (outletsArray != null) {
                val list = mutableListOf<Outlet>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until outletsArray.length()) {
                    val obj = outletsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(Outlet(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        name = obj.optString("name"),
                        address = if (obj.isNull("address")) null else obj.optString("address"),
                        phone = if (obj.isNull("phone")) null else obj.optString("phone"),
                        isDefault = obj.optBoolean("isDefault", false),
                        isOpen = obj.optBoolean("isOpen", true),
                        currentEmployee = if (obj.isNull("currentEmployee")) null else obj.optString("currentEmployee"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isSynced = true
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverOutlet ->
                        val localOutlet = db.outletDao().getById(serverOutlet.id)
                        if (localOutlet == null) {
                            db.outletDao().insert(serverOutlet)
                        } else if (localOutlet.isSynced) {
                            if (serverOutlet.updatedAt >= localOutlet.updatedAt) {
                                db.outletDao().insert(serverOutlet)
                            }
                        }
                    }
                }
                // Local pruning — NEVER prune the default outlet to prevent data lockouts
                val localOutlets = db.outletDao().getAll().filter { it.tenantId == activeTenantId }
                localOutlets.forEach { local ->
                    if (local.isSynced && !local.isDefault && local.id !in serverIds) {
                        db.outletDao().delete(local.id)
                    }
                }
            }

            // 2. Employees
            if (employeesArray != null) {
                val list = mutableListOf<Employee>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until employeesArray.length()) {
                    val obj = employeesArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    val outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId")
                    list.add(Employee(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = outletId,
                        name = obj.optString("name"),
                        email = if (obj.isNull("email")) null else obj.optString("email"),
                        role = obj.optString("role", "KASIR"),
                        pinHash = obj.optString("pinHash"),
                        phone = if (obj.isNull("phone")) null else obj.optString("phone"),
                        salary = obj.optDouble("salary", 0.0),
                        isActive = obj.optBoolean("isActive", true),
                        payPeriod = obj.optString("payPeriod", "MONTHLY"),
                        lastPaidAt = if (obj.isNull("lastPaidAt")) null else obj.optLong("lastPaidAt"),
                        emailVerified = obj.optBoolean("emailVerified", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isSynced = true
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverEmployee ->
                        val localEmployee = db.employeeDao().getById(serverEmployee.id)
                        if (localEmployee == null) {
                            db.employeeDao().insert(serverEmployee)
                        } else if (localEmployee.isSynced) {
                            if (serverEmployee.updatedAt >= localEmployee.updatedAt) {
                                db.employeeDao().insert(serverEmployee)
                            }
                        }
                    }
                }
                // Local pruning — NEVER prune the OWNER account to prevent lockouts
                val localEmployees = db.employeeDao().getAll().filter { it.tenantId == activeTenantId }
                localEmployees.forEach { local ->
                    if (local.isSynced && local.role != "OWNER" && local.id !in serverIds) {
                        db.employeeDao().deleteById(local.id)
                    }
                }
            }

            // 3. Products
            if (productsArray != null) {
                val list = mutableListOf<ProductEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until productsArray.length()) {
                    val obj = productsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    val outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId")
                    list.add(ProductEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = outletId,
                        name = obj.optString("name"),
                        price = obj.optDouble("price", 0.0),
                        costPrice = obj.optDouble("costPrice", 0.0),
                        stock = obj.optInt("stock", 0),
                        unit = obj.optString("unit", "pcs"),
                        barcode = if (obj.isNull("barcode")) null else obj.optString("barcode"),
                        category = obj.optString("category", "Umum"),
                        wholesaleEnabled = obj.optBoolean("wholesaleEnabled", false),
                        wholesalePrices = if (obj.isNull("wholesalePrices")) null else obj.opt("wholesalePrices")?.toString(),
                        variants = if (obj.isNull("variants")) null else obj.opt("variants")?.toString(),
                        image = if (obj.isNull("image")) null else obj.optString("image"),
                        minStockAlert = obj.optInt("minStockAlert", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isSynced = true
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverProd ->
                        val localProd = db.productDao().getById(serverProd.id)
                        if (localProd == null) {
                            db.productDao().upsert(serverProd)
                        } else if (localProd.isSynced) {
                            if (serverProd.updatedAt >= localProd.updatedAt) {
                                db.productDao().upsert(serverProd)
                            }
                        }
                    }
                }
                // Local pruning
                val localProducts = db.productDao().getAll().filter { it.tenantId == activeTenantId }
                localProducts.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.productDao().delete(local.id)
                    }
                }
            }

            // 4. Customers
            if (customersArray != null) {
                val list = mutableListOf<CustomerEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until customersArray.length()) {
                    val obj = customersArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(CustomerEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        name = obj.optString("name"),
                        phone = if (obj.isNull("phone")) null else obj.optString("phone"),
                        address = if (obj.isNull("address")) null else obj.optString("address"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isSynced = true
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverCustomer ->
                        val localCustomer = db.customerDao().getById(serverCustomer.id)
                        if (localCustomer == null) {
                            db.customerDao().upsert(serverCustomer)
                        } else if (localCustomer.isSynced) {
                            if (serverCustomer.updatedAt >= localCustomer.updatedAt) {
                                db.customerDao().upsert(serverCustomer)
                            }
                        }
                    }
                }
                // Local pruning
                val localCustomers = db.customerDao().getAll().filter { it.tenantId == activeTenantId }
                localCustomers.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.customerDao().delete(local.id)
                    }
                }
            }

            // 5. Transactions
            if (transactionEntities.isNotEmpty()) {
                transactionEntities.forEach { db.transactionDao().insert(it) }
            }

            // 6. Pull transaction_items
            if (transactionItemsArray != null && transactionItemsArray.length() > 0) {
                val tenantTxIds = transactionEntities.map { it.id }.toSet()
                if (tenantTxIds.isNotEmpty()) {
                    tenantTxIds.forEach { txId ->
                        db.transactionItemDao().deleteForTransaction(txId)
                    }
                }

                val itemsList = mutableListOf<TransactionItemEntity>()
                for (i in 0 until transactionItemsArray.length()) {
                    val obj = transactionItemsArray.getJSONObject(i)
                    val txId = obj.optLong("transactionId", 0L)
                    if (txId in tenantTxIds) {
                        val variantId = if (obj.isNull("variantId")) null else obj.optLong("variantId")
                        val variantName = if (obj.isNull("variantName")) null else obj.optString("variantName")
                        itemsList.add(TransactionItemEntity(
                            id = obj.optLong("id", 0L),
                            transactionId = txId,
                            productId = obj.optLong("productId", 0L),
                            variantId = variantId,
                            variantName = variantName,
                            quantity = obj.optInt("quantity", 1),
                            price = obj.optDouble("price", 0.0),
                            costPrice = obj.optDouble("costPrice", 0.0),
                            discount = obj.optDouble("discount", 0.0),
                            note = if (obj.isNull("note")) null else obj.optString("note")
                        ))
                    }
                }
                if (itemsList.isNotEmpty()) {
                    db.transactionItemDao().insertAll(itemsList)
                }
            }

            // 7. bmp_clients
            if (bmpClientsArray != null) {
                val list = mutableListOf<BmpClientEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpClientsArray.length()) {
                    val obj = bmpClientsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpClientEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        clientName = obj.optString("clientName"),
                        saldoTitipan = obj.optDouble("saldoTitipan", 0.0),
                        addressLine1 = if (obj.isNull("addressLine1")) null else obj.optString("addressLine1"),
                        clientLogo = if (obj.isNull("clientLogo")) null else obj.optString("clientLogo"),
                        province = if (obj.isNull("province")) null else obj.optString("province"),
                        postalCode = if (obj.isNull("postalCode")) null else obj.optString("postalCode"),
                        phoneNumber = if (obj.isNull("phoneNumber")) null else obj.optString("phoneNumber"),
                        emailAddress = if (obj.isNull("emailAddress")) null else obj.optString("emailAddress"),
                        taxNumber = if (obj.isNull("taxNumber")) null else obj.optString("taxNumber"),
                        uniqueID = if (obj.isNull("uniqueID")) null else obj.optString("uniqueID"),
                        slug = if (obj.isNull("slug")) null else obj.optString("slug"),
                        isSynced = true,
                        receiverSignatureUrl = if (obj.isNull("receiverSignatureUrl")) null else obj.optString("receiverSignatureUrl"),
                        receiverNameActual = if (obj.isNull("receiverNameActual")) null else obj.optString("receiverNameActual"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    // Guard: jangan restore klien yang sudah di-soft-delete lokal
                    val localDeletedIds = db.bmpClientDao().getDeletedIds(activeTenantId).toSet()
                    list.filter { it.id !in localDeletedIds }.forEach { serverClient ->
                        val localClient = db.bmpClientDao().getById(serverClient.id)
                        if (localClient == null) {
                            db.bmpClientDao().upsert(serverClient)
                        } else if (localClient.isSynced) {
                            if (serverClient.updatedAt >= localClient.updatedAt) {
                                db.bmpClientDao().upsert(serverClient)
                            }
                        }
                    }
                }
                // Local pruning
                val localClients = db.bmpClientDao().getAll().filter { it.tenantId == activeTenantId }
                localClients.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpClientDao().hardDelete(local.id)
                    }
                }
            }

            // 8. bmp_invoices
            if (bmpInvoicesArray != null) {
                val list = mutableListOf<BmpInvoiceEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpInvoicesArray.length()) {
                    val obj = bmpInvoicesArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpInvoiceEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        clientId = if (obj.isNull("clientId")) null else obj.optLong("clientId"),
                        title = obj.optString("title"),
                        number = obj.optString("number"),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        paymentTerms = obj.optString("paymentTerms", "14 days"),
                        status = obj.optString("status", "DRAFT"),
                        notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        paidAmount = obj.optDouble("paidAmount", 0.0),
                        uniqueID = if (obj.isNull("uniqueID")) null else obj.optString("uniqueID"),
                        slug = obj.optString("slug"),
                        isSynced = true,
                        receiverSignatureUrl = if (obj.isNull("receiverSignatureUrl")) null else obj.optString("receiverSignatureUrl"),
                        receiverNameActual = if (obj.isNull("receiverNameActual")) null else obj.optString("receiverNameActual"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverInvoice ->
                        val localInvoice = db.bmpInvoiceDao().getById(serverInvoice.id)
                        if (localInvoice == null) {
                            db.bmpInvoiceDao().upsert(serverInvoice)
                        } else if (localInvoice.isSynced) {
                            if (serverInvoice.updatedAt >= localInvoice.updatedAt) {
                                // Preserve local signature file path to keep the printed invoice signature working offline
                                val merged = serverInvoice.copy(receiverSignaturePath = localInvoice.receiverSignaturePath)
                                db.bmpInvoiceDao().upsert(merged)
                            }
                        }
                    }
                }
                // Local pruning
                val localInvoices = db.bmpInvoiceDao().getAll().filter { it.tenantId == activeTenantId }
                localInvoices.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpInvoiceDao().hardDelete(local.id)
                    }
                }
            }

            // 9. bmp_products
            if (bmpProductsArray != null) {
                val list = mutableListOf<BmpProductEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpProductsArray.length()) {
                    val obj = bmpProductsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpProductEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        invoiceId = if (obj.isNull("invoiceId")) null else obj.optLong("invoiceId"),
                        masterItemID = if (obj.isNull("masterItemID")) null else obj.optLong("masterItemID"),
                        title = obj.optString("title"),
                        description = if (obj.isNull("description")) null else obj.optString("description"),
                        unit = obj.optString("unit", "pcs"),
                        price = obj.optDouble("price", 0.0),
                        jumlahLusin = obj.optDouble("jumlahLusin", 1.0),
                        quantity = obj.optDouble("quantity", 0.0),
                        isKhusus = obj.optBoolean("isKhusus", false),
                        hargaBeli = obj.optDouble("hargaBeli", 0.0),
                        currency = obj.optString("currency", "Rp"),
                        uniqueID = if (obj.isNull("uniqueID")) null else obj.optString("uniqueID"),
                        slug = if (obj.isNull("slug")) null else obj.optString("slug"),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverProd ->
                        val localProd = db.bmpProductDao().getById(serverProd.id)
                        if (localProd == null) {
                            db.bmpProductDao().upsert(serverProd)
                        } else if (localProd.isSynced) {
                            if (serverProd.updatedAt >= localProd.updatedAt) {
                                db.bmpProductDao().upsert(serverProd)
                            }
                        }
                    }
                }
                // Local pruning
                val localProducts = db.bmpProductDao().getAll().filter { it.tenantId == activeTenantId }
                localProducts.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpProductDao().hardDelete(local.id)
                    }
                }
            }

            // 10. bmp_master_products
            if (bmpMasterProductsArray != null) {
                val list = mutableListOf<BmpMasterProductEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpMasterProductsArray.length()) {
                    val obj = bmpMasterProductsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpMasterProductEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        title = obj.optString("title"),
                        description = if (obj.isNull("description")) null else obj.optString("description"),
                        unit = obj.optString("unit", "Kg"),
                        price = obj.optDouble("price", 0.0),
                        beratGram = obj.optDouble("beratGram", 0.0),
                        cycleTime = obj.optDouble("cycleTime", 0.0),
                        cavity = obj.optInt("cavity", 1),
                        rejectRate = obj.optDouble("rejectRate", 0.0),
                        uniqueID = if (obj.isNull("uniqueID")) null else obj.optString("uniqueID"),
                        slug = if (obj.isNull("slug")) null else obj.optString("slug"),
                        jenisBahanBaku = obj.optString("jenisBahanBaku", ""),
                        image = if (obj.isNull("image")) null else obj.optString("image"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isSynced = true,
                        hppTotalPcs = obj.optDouble("hppTotalPcs", 0.0),
                        hppLusin = obj.optDouble("hppLusin", 0.0)
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverProd ->
                        val localProd = db.bmpMasterProductDao().getById(serverProd.id)
                        if (localProd == null) {
                            db.bmpMasterProductDao().upsert(serverProd)
                        } else if (localProd.isSynced) {
                            if (serverProd.updatedAt >= localProd.updatedAt) {
                                db.bmpMasterProductDao().upsert(serverProd)
                            }
                        }
                    }
                }
                // Local pruning
                val localMasterProducts = db.bmpMasterProductDao().getAll().filter { it.tenantId == activeTenantId }
                localMasterProducts.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpMasterProductDao().hardDelete(local.id)
                    }
                }
            }

            // 11. bmp_invoice_payments
            if (bmpPaymentsArray != null) {
                val list = mutableListOf<BmpInvoicePaymentEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpPaymentsArray.length()) {
                    val obj = bmpPaymentsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpInvoicePaymentEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        invoiceId = obj.optLong("invoiceId"),
                        paymentDate = obj.optLong("paymentDate"),
                        paymentAmount = obj.optDouble("paymentAmount", 0.0),
                        paymentMethod = obj.optString("paymentMethod", "TRANSFER"),
                        notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.bmpPaymentDao().upsert(it) }
                }
                // Local pruning
                val localPayments = db.bmpPaymentDao().getAll().filter { it.tenantId == activeTenantId }
                localPayments.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpPaymentDao().hardDelete(local.id)
                    }
                }
            }

            // 12. bmp_cashflow
            if (bmpCashFlowArray != null) {
                val list = mutableListOf<BmpCashFlowEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpCashFlowArray.length()) {
                    val obj = bmpCashFlowArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpCashFlowEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        transactionDate = obj.optLong("transactionDate"),
                        transactionType = obj.optString("transactionType"),
                        description = obj.optString("description"),
                        amount = obj.optDouble("amount", 0.0),
                        paymentRefId = if (obj.isNull("paymentRefId")) null else obj.optLong("paymentRefId"),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.bmpCashFlowDao().upsert(it) }
                }
                // Local pruning
                val localCashFlow = db.bmpCashFlowDao().getAll().filter { it.tenantId == activeTenantId }
                localCashFlow.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpCashFlowDao().hardDelete(local.id)
                    }
                }
            }

            // 13. bmp_settings
            if (bmpSettingsArray != null) {
                val list = mutableListOf<BmpSettingsEntity>()
                for (i in 0 until bmpSettingsArray.length()) {
                    val obj = bmpSettingsArray.getJSONObject(i)
                    list.add(BmpSettingsEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
                        clientName = obj.optString("clientName"),
                        clientLogo = if (obj.isNull("clientLogo")) null else obj.optString("clientLogo"),
                        addressLine1 = if (obj.isNull("addressLine1")) null else obj.optString("addressLine1"),
                        province = if (obj.isNull("province")) null else obj.optString("province"),
                        postalCode = if (obj.isNull("postalCode")) null else obj.optString("postalCode"),
                        phoneNumber = if (obj.isNull("phoneNumber")) null else obj.optString("phoneNumber"),
                        emailAddress = if (obj.isNull("emailAddress")) null else obj.optString("emailAddress"),
                        taxNumber = if (obj.isNull("taxNumber")) null else obj.optString("taxNumber"),
                        listrikBulanan = obj.optDouble("listrikBulanan", 30000000.0),
                        jumlahMesin = obj.optInt("jumlahMesin", 5),
                        jumlahKaryawan = obj.optInt("jumlahKaryawan", 19),
                        gajiHarian = obj.optDouble("gajiHarian", 80000.0),
                        hariKerjaSebulan = obj.optInt("hariKerjaSebulan", 26),
                        biayaKarungPer1000 = obj.optDouble("biayaKarungPer1000", 2100000.0),
                        hoursPerDay = obj.optInt("hoursPerDay", 24),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverSettings ->
                        val localSettings = db.bmpSettingsDao().get(serverSettings.tenantId)
                        if (localSettings == null || serverSettings.updatedAt > localSettings.updatedAt) {
                            db.bmpSettingsDao().upsert(serverSettings)
                        }
                    }
                }
            }

            // 14. bmp_employees
            if (bmpEmployeesArray != null) {
                val list = mutableListOf<BmpEmployeeEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpEmployeesArray.length()) {
                    val obj = bmpEmployeesArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpEmployeeEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        name = obj.optString("name"),
                        position = if (obj.isNull("position")) null else obj.optString("position"),
                        salaryAmount = obj.optDouble("salaryAmount", 0.0),
                        isActive = obj.optBoolean("isActive", true),
                        fingerprintPIN = if (obj.isNull("fingerprintPIN")) null else obj.optString("fingerprintPIN"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isSynced = true
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.bmpEmployeeDao().upsert(it) }
                }
                // Local pruning
                val localBmpEmployees = db.bmpEmployeeDao().getAll().filter { it.tenantId == activeTenantId }
                localBmpEmployees.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpEmployeeDao().hardDelete(local.id)
                    }
                }
            }

            // 15. bmp_payrolls
            if (bmpPayrollsArray != null) {
                val list = mutableListOf<BmpPayrollEntity>()
                val serverIds = mutableSetOf<String>()
                for (i in 0 until bmpPayrollsArray.length()) {
                    val obj = bmpPayrollsArray.getJSONObject(i)
                    val idVal = obj.optString("id", "")
                    serverIds.add(idVal)
                    list.add(BmpPayrollEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        employeeId = obj.optLong("employeeId"),
                        paymentDate = obj.optLong("paymentDate"),
                        amount = obj.optDouble("amount", 0.0),
                        attendanceCount = obj.optInt("attendanceCount", 0),
                        dailyRate = obj.optDouble("dailyRate", 0.0),
                        description = if (obj.isNull("description")) null else obj.optString("description"),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.bmpPayrollDao().upsert(it) }
                }
                // Local pruning
                val localPayrolls = db.bmpPayrollDao().getAll().filter { it.tenantId == activeTenantId }
                localPayrolls.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpPayrollDao().delete(local.id)
                    }
                }
            }

            // 16. bmp_bahan_baku
            if (bmpBahanBakuArray != null) {
                val list = mutableListOf<BmpBahanBakuEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpBahanBakuArray.length()) {
                    val obj = bmpBahanBakuArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpBahanBakuEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        tanggal = obj.optLong("tanggal", System.currentTimeMillis()),
                        noTagihan = obj.optString("noTagihan"),
                        totalHarga = obj.optDouble("totalHarga", 0.0),
                        nominal = obj.optDouble("nominal", 0.0),
                        notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                        notaFotoUrl = if (obj.isNull("notaFotoUrl")) null else obj.optString("notaFotoUrl"),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverBahan ->
                        val localBahan = db.bmpBahanBakuDao().getById(serverBahan.id)
                        if (localBahan == null) {
                            db.bmpBahanBakuDao().upsert(serverBahan)
                        } else if (localBahan.isSynced) {
                            if (serverBahan.updatedAt >= localBahan.updatedAt) {
                                // Preserve local photo path to prevent the local image from disappearing
                                val merged = serverBahan.copy(notaFotoPath = localBahan.notaFotoPath)
                                db.bmpBahanBakuDao().upsert(merged)
                            }
                        }
                    }
                }
                // Local pruning
                val localBahanBaku = db.bmpBahanBakuDao().getAll().filter { it.tenantId == activeTenantId }
                localBahanBaku.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpBahanBakuDao().hardDelete(local.id)
                    }
                }
            }

            // 17. bmp_bahan_baku_item
            if (bmpBahanBakuItemsArray != null) {
                val list = mutableListOf<BmpBahanBakuItemEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpBahanBakuItemsArray.length()) {
                    val obj = bmpBahanBakuItemsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpBahanBakuItemEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        bahanBakuId = obj.optLong("bahanBakuId"),
                        jenisBahan = obj.optString("jenisBahan"),
                        kuantitas = obj.optDouble("kuantitas", 0.0),
                        unit = obj.optString("unit", "Kg"),
                        rate = obj.optDouble("rate", 0.0),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverItem ->
                        val localItem = db.bmpBahanBakuItemDao().getById(serverItem.id)
                        if (localItem == null) {
                            db.bmpBahanBakuItemDao().upsert(serverItem)
                        } else if (localItem.isSynced) {
                            db.bmpBahanBakuItemDao().upsert(serverItem)
                        }
                    }
                }
                // Local pruning
                val localBahanBakuItems = db.bmpBahanBakuItemDao().getAll().filter { it.tenantId == activeTenantId }
                localBahanBakuItems.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpBahanBakuItemDao().hardDelete(local.id)
                    }
                }
            }

            // 17a. bmp_product_stocks
            if (bmpProductStocksArray != null) {
                val list = mutableListOf<BmpProductStockEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpProductStocksArray.length()) {
                    val obj = bmpProductStocksArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpProductStockEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        masterProductId = obj.optLong("masterProductId"),
                        quantity = obj.optDouble("quantity", 0.0),
                        minStockAlert = obj.optDouble("minStockAlert", 0.0),
                        isSynced = true,
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    val localDeletedIds = db.bmpProductStockDao().getDeletedIds(activeTenantId).toSet()
                    list.filter { it.id !in localDeletedIds }.forEach { db.bmpProductStockDao().upsert(it) }
                }
                // Local pruning
                val localProductStocks = db.bmpProductStockDao().getAll().filter { it.tenantId == activeTenantId }
                localProductStocks.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpProductStockDao().hardDelete(local.id)
                    }
                }
            }

            // 17b. bmp_stock_ledger
            if (bmpStockLedgerArray != null) {
                val list = mutableListOf<BmpStockLedgerEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpStockLedgerArray.length()) {
                    val obj = bmpStockLedgerArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpStockLedgerEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        masterProductId = obj.optLong("masterProductId"),
                        referenceId = obj.optLong("referenceId"),
                        mutationType = obj.optString("mutationType"),
                        quantityChange = obj.optDouble("quantityChange", 0.0),
                        finalStock = obj.optDouble("finalStock", 0.0),
                        notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    val localDeletedIds = db.bmpStockLedgerDao().getDeletedIds(activeTenantId).toSet()
                    list.filter { it.id !in localDeletedIds }.forEach { db.bmpStockLedgerDao().insert(it) }
                }
                // Local pruning
                val localStockLedgers = db.bmpStockLedgerDao().getAll().filter { it.tenantId == activeTenantId }
                localStockLedgers.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpStockLedgerDao().hardDelete(local.id)
                    }
                }
            }

            // 17c. bmp_production_logs
            if (bmpProductionLogsArray != null) {
                val list = mutableListOf<BmpProductionLogEntity>()
                val serverIds = mutableSetOf<Long>()
                for (i in 0 until bmpProductionLogsArray.length()) {
                    val obj = bmpProductionLogsArray.getJSONObject(i)
                    val idVal = obj.optLong("id", 0L)
                    serverIds.add(idVal)
                    list.add(BmpProductionLogEntity(
                        id = idVal,
                        tenantId = obj.optString("tenantId", activeTenantId),
                        masterProductId = obj.optLong("masterProductId"),
                        quantityProduced = obj.optDouble("quantityProduced", 0.0),
                        quantityRejected = obj.optDouble("quantityRejected", 0.0),
                        rawMaterialUsedKg = obj.optDouble("rawMaterialUsedKg", 0.0),
                        operatorName = if (obj.isNull("operatorName")) null else obj.optString("operatorName"),
                        productionDate = obj.optLong("productionDate", System.currentTimeMillis()),
                        isSynced = true,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    val localDeletedIds = db.bmpProductionLogDao().getDeletedIds(activeTenantId).toSet()
                    list.filter { it.id !in localDeletedIds }.forEach { db.bmpProductionLogDao().upsert(it) }
                }
                // Local pruning
                val localProductionLogs = db.bmpProductionLogDao().getAll().filter { it.tenantId == activeTenantId }
                localProductionLogs.forEach { local ->
                    if (local.isSynced && local.id !in serverIds) {
                        db.bmpProductionLogDao().hardDelete(local.id)
                    }
                }
            }

            // 18. print_settings
            if (printSettingsArray != null) {
                val list = mutableListOf<PrintSettingsEntity>()
                for (i in 0 until printSettingsArray.length()) {
                    val obj = printSettingsArray.getJSONObject(i)
                    list.add(PrintSettingsEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
                        moduleKey = obj.optString("moduleKey", "BMP"),
                        jpgUseLogo = obj.optBoolean("jpgUseLogo", true),
                        jpgHeaderAlign = obj.optString("jpgHeaderAlign", "LEFT"),
                        jpgUseSignature = obj.optBoolean("jpgUseSignature", true),
                        jpgSignatureSenderName = obj.optString("jpgSignatureSenderName", "Admin"),
                        jpgSignatureReceiverName = obj.optString("jpgSignatureReceiverName", ""),
                        jpgSignatureDrawnBase64 = if (obj.isNull("jpgSignatureDrawnBase64")) null else obj.optString("jpgSignatureDrawnBase64"),
                        jpgIsColor = obj.optBoolean("jpgIsColor", true),
                        sjUseLogo = obj.optBoolean("sjUseLogo", true),
                        sjHeaderAlign = obj.optString("sjHeaderAlign", "LEFT"),
                        sjUseSignature = obj.optBoolean("sjUseSignature", true),
                        sjSignatureSenderName = obj.optString("sjSignatureSenderName", "Admin"),
                        sjSignatureReceiverName = obj.optString("sjSignatureReceiverName", ""),
                        sjSignatureDrawnBase64 = if (obj.isNull("sjSignatureDrawnBase64")) null else obj.optString("sjSignatureDrawnBase64"),
                        sjIsColor = obj.optBoolean("sjIsColor", false),
                        invoiceUseLogo = obj.optBoolean("invoiceUseLogo", true),
                        invoiceHeaderAlign = obj.optString("invoiceHeaderAlign", "LEFT"),
                        invoiceUseSignature = obj.optBoolean("invoiceUseSignature", true),
                        invoiceSignatureSenderName = obj.optString("invoiceSignatureSenderName", "Admin"),
                        invoiceSignatureReceiverName = obj.optString("invoiceSignatureReceiverName", ""),
                        invoiceSignatureDrawnBase64 = if (obj.isNull("invoiceSignatureDrawnBase64")) null else obj.optString("invoiceSignatureDrawnBase64"),
                        invoiceIsColor = obj.optBoolean("invoiceIsColor", true),
                        receiptPaperWidth = obj.optString("receiptPaperWidth", "MM80"),
                        receiptUseLogo = obj.optBoolean("receiptUseLogo", true),
                        receiptHeaderAlign = obj.optString("receiptHeaderAlign", "CENTER"),
                        receiptIsColor = obj.optBoolean("receiptIsColor", false),
                        receiptShowItemPrice = obj.optBoolean("receiptShowItemPrice", true),
                        receiptFooterText = obj.optString("receiptFooterText", "Terima kasih sudah berbelanja!"),
                        jpgTemplateType = obj.optString("jpgTemplateType", "MODERN"),
                        sjTemplateType = obj.optString("sjTemplateType", "MODERN"),
                        invoiceTemplateType = obj.optString("invoiceTemplateType", "MODERN"),
                        bankOwnerName = obj.optString("bankOwnerName", ""),
                        bankName = obj.optString("bankName", "BCA"),
                        bankAccountNumber = obj.optString("bankAccountNumber", ""),
                        logoPath = if (obj.isNull("logoPath")) null else obj.optString("logoPath"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { serverSettings ->
                        val localSettings = db.printSettingsDao().get(serverSettings.tenantId, serverSettings.moduleKey)
                        if (localSettings == null || serverSettings.updatedAt > localSettings.updatedAt) {
                            db.printSettingsDao().upsert(serverSettings)
                        }
                    }
                }
            }

            // 19. activity_logs
            if (activityLogsArray != null) {
                val list = mutableListOf<com.posbah.app.data.local.entities.ActivityLogEntity>()
                for (i in 0 until activityLogsArray.length()) {
                    val obj = activityLogsArray.getJSONObject(i)
                    list.add(com.posbah.app.data.local.entities.ActivityLogEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        action = obj.optString("action"),
                        description = obj.optString("description"),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        employeeName = obj.optString("employeeName"),
                        appMode = obj.optString("appMode", "FNB")
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.activityLogDao().insertLog(it) }
                }
            }
            } // end of db.withTransaction

            downloadMissingBahanBakuPhotos(context, db, activeTenantId, email)

            Log.d(TAG, "Pull sinkronisasi selesai dengan sukses.")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Pull sinkronisasi gagal: ${e.message}", e)
            SyncResult.Error(e.message ?: "Error tidak terduga saat pull sinkronisasi.")
        } finally {
            if (pullMutex.isLocked) pullMutex.unlock()
        }
    }

    /**
     * Unduh semua tenant yang dimiliki oleh ownerEmail dari VPS dan simpan ke Room local DB.
     * Ini juga mengunduh outlet default untuk tiap tenant agar proses pemilihan tenant lancar.
     */
    suspend fun fetchAndInsertOwnerTenants(context: Context, db: PosBahDatabase, ownerEmail: String) = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "[fetchAndInsertOwnerTenants] Jaringan tidak tersedia, lewati.")
            return@withContext
        }
        try {
            val emailEncoded = java.net.URLEncoder.encode(ownerEmail.lowercase().trim(), "UTF-8")
            val url = URL("$VPS_URL/api/sync/tenants?ownerEmail=eq.$emailEncoded")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val activeTenantId = securePrefs.currentTenantId
                if (!activeTenantId.isNullOrBlank()) {
                    setRequestProperty("x-tenant-id", activeTenantId)
                }
                setRequestProperty("x-user-email", ownerEmail)
                setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
            }
            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(responseText)
                Log.d(TAG, "[fetchAndInsertOwnerTenants] Ditemukan ${array.length()} tenant di server.")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tenant = Tenant(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        ownerEmail = obj.getString("ownerEmail"),
                        businessMode = obj.getString("businessMode"),
                        isActive = obj.optBoolean("isActive", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    db.tenantDao().upsert(tenant)

                    // Unduh data outlet untuk tenant ini agar tidak kosong saat dipilih
                    try {
                        val outletsArray = pullTable(context, "outlets", tenant.id)
                        if (outletsArray != null && outletsArray.length() > 0) {
                            for (j in 0 until outletsArray.length()) {
                                val oObj = outletsArray.getJSONObject(j)
                                db.outletDao().insert(
                                    Outlet(
                                        id = oObj.optLong("id", 0L),
                                        tenantId = oObj.optString("tenantId", tenant.id),
                                        name = oObj.optString("name"),
                                        address = if (oObj.isNull("address")) null else oObj.optString("address"),
                                        phone = if (oObj.isNull("phone")) null else oObj.optString("phone"),
                                        isDefault = oObj.optBoolean("isDefault", false),
                                        isOpen = oObj.optBoolean("isOpen", true),
                                        currentEmployee = if (oObj.isNull("currentEmployee")) null else oObj.optString("currentEmployee"),
                                        createdAt = oObj.optLong("createdAt", System.currentTimeMillis()),
                                        updatedAt = oObj.optLong("updatedAt", System.currentTimeMillis())
                                    )
                                )
                            }
                        } else {
                            // Seed local default outlet if none exists on server
                            val localOutlets = db.outletDao().listForTenant(tenant.id)
                            if (localOutlets.isEmpty()) {
                                db.outletDao().insert(
                                    Outlet(
                                        tenantId = tenant.id,
                                        name = "Outlet Utama",
                                        isDefault = true
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gagal mengunduh outlets untuk tenant ${tenant.id}: ${e.message}")
                    }
                }
            } else {
                Log.e(TAG, "Gagal mengunduh tenants untuk owner $ownerEmail: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat mengunduh tenants untuk owner $ownerEmail: ${e.message}", e)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // IMMEDIATE WRITE-THROUGH PUSH HELPERS
    //
    // Tujuan: Memastikan data master (produk, customer, master_product BMP)
    // langsung ter-upload ke VPS PostgreSQL SEGERA setelah disimpan lokal,
    // tanpa menunggu syncAll lengkap (yang berat karena meng-upload 24 tabel).
    //
    // Menggunakan syncScope (SupervisorJob+IO global) sehingga tidak ikut
    // dibatalkan ketika ViewModel di-clear (mis. saat user logout/navigate).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Push satu produk POS ke VPS secara non-blocking di global scope.
     * Aman dipanggil dari ViewModel: tidak akan ikut dibatalkan saat ViewModel di-clear.
     * Setelah berhasil, kolom isSynced di Room akan di-set true.
     */
    fun pushProductImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        productId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[pushProductImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                val p = db.productDao().getById(productId)
                if (p == null) {
                    Log.w(TAG, "[pushProductImmediate] Produk id=$productId tidak ditemukan")
                    return@launch
                }
                if (p.tenantId != activeTenantId) {
                    Log.w(TAG, "[pushProductImmediate] tenantId mismatch (entity=${p.tenantId}, active=$activeTenantId)")
                    return@launch
                }
                val array = JSONArray()
                array.put(JSONObject().apply {
                    put("id", p.id)
                    put("tenantId", p.tenantId)
                    put("outletId", p.outletId ?: JSONObject.NULL)
                    put("name", p.name)
                    put("price", p.price)
                    put("costPrice", p.costPrice)
                    put("stock", p.stock)
                    put("unit", p.unit)
                    put("barcode", p.barcode ?: JSONObject.NULL)
                    put("category", p.category)
                    put("wholesaleEnabled", p.wholesaleEnabled)
                    put("wholesalePrices", p.wholesalePrices ?: JSONObject.NULL)
                    put("variants", p.variants ?: JSONObject.NULL)
                    put("image", p.image ?: JSONObject.NULL)
                    put("minStockAlert", p.minStockAlert)
                    put("isSynced", true)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)
                })
                if (uploadTable(context, "products", array)) {
                    db.productDao().markSynced(productId)
                    Log.i(TAG, "[pushProductImmediate] OK push produk id=$productId ke VPS")
                } else {
                    Log.w(TAG, "[pushProductImmediate] Gagal push produk id=$productId, akan diretry pada syncAll berikutnya")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[pushProductImmediate] error: ${e.message}", e)
            }
        }
    }

    /**
     * Hapus produk POS dari VPS secara non-blocking di global scope.
     * Setelah berhasil hapus di server, baris di Room juga di-hard-delete.
     */
    fun deleteProductImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        productId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[deleteProductImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                if (deleteRow(context, "products", productId, activeTenantId)) {
                    db.productDao().hardDelete(productId)
                    Log.i(TAG, "[deleteProductImmediate] OK delete produk id=$productId di VPS")
                } else {
                    Log.w(TAG, "[deleteProductImmediate] Gagal delete produk id=$productId di server, retry pada syncAll berikutnya")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[deleteProductImmediate] error: ${e.message}", e)
            }
        }
    }

    /**
     * Push satu customer POS ke VPS secara non-blocking di global scope.
     */
    fun pushCustomerImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        customerId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[pushCustomerImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                val c = db.customerDao().getById(customerId) ?: return@launch
                if (c.tenantId != activeTenantId) return@launch
                val array = JSONArray()
                array.put(JSONObject().apply {
                    put("id", c.id)
                    put("tenantId", c.tenantId)
                    put("name", c.name)
                    put("phone", c.phone ?: JSONObject.NULL)
                    put("address", c.address ?: JSONObject.NULL)
                    put("isSynced", true)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                })
                if (uploadTable(context, "customers", array)) {
                    db.customerDao().markSynced(customerId)
                    Log.i(TAG, "[pushCustomerImmediate] OK push customer id=$customerId ke VPS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[pushCustomerImmediate] error: ${e.message}", e)
            }
        }
    }

    /**
     * Push satu master produk BMP ke VPS secara non-blocking di global scope.
     */
    fun pushBmpMasterProductImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        masterProductId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[pushBmpMasterProductImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                val mp = db.bmpMasterProductDao().getById(masterProductId) ?: return@launch
                if (mp.tenantId != activeTenantId) return@launch
                val array = JSONArray()
                array.put(JSONObject().apply {
                    put("id", mp.id)
                    put("tenantId", mp.tenantId)
                    put("title", mp.title)
                    put("description", mp.description ?: JSONObject.NULL)
                    put("unit", mp.unit)
                    put("price", mp.price)
                    put("beratGram", mp.beratGram)
                    put("cycleTime", mp.cycleTime)
                    put("cavity", mp.cavity)
                    put("rejectRate", mp.rejectRate)
                    put("uniqueID", mp.uniqueID ?: JSONObject.NULL)
                    put("slug", mp.slug ?: JSONObject.NULL)
                    put("jenisBahanBaku", mp.jenisBahanBaku)
                    put("image", mp.image ?: JSONObject.NULL)
                    put("isSynced", true)
                    put("createdAt", mp.createdAt)
                    put("updatedAt", mp.updatedAt)
                    put("hppTotalPcs", mp.hppTotalPcs)
                    put("hppLusin", mp.hppLusin)
                })
                if (uploadTable(context, "bmp_master_products", array)) {
                    db.bmpMasterProductDao().markSynced(masterProductId)
                    Log.i(TAG, "[pushBmpMasterProductImmediate] OK push master produk BMP id=$masterProductId ke VPS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[pushBmpMasterProductImmediate] error: ${e.message}", e)
            }
        }
    }

    /**
     * Hapus master produk BMP dari VPS secara non-blocking di global scope.
     */
    fun deleteBmpMasterProductImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        masterProductId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) return@launch
                if (deleteRow(context, "bmp_master_products", masterProductId, activeTenantId)) {
                    db.bmpMasterProductDao().hardDelete(masterProductId)
                    Log.i(TAG, "[deleteBmpMasterProductImmediate] OK delete master produk BMP id=$masterProductId di VPS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[deleteBmpMasterProductImmediate] error: ${e.message}", e)
            }
        }
    }

    fun pushEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[pushEmployeeImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                val e = db.employeeDao().getById(employeeId)
                if (e == null) {
                    Log.w(TAG, "[pushEmployeeImmediate] Employee id=$employeeId tidak ditemukan")
                    return@launch
                }
                if (e.tenantId != activeTenantId) {
                    Log.w(TAG, "[pushEmployeeImmediate] tenantId mismatch (entity=${e.tenantId}, active=$activeTenantId)")
                    return@launch
                }
                val array = JSONArray()
                array.put(JSONObject().apply {
                    put("id", e.id)
                    put("tenantId", e.tenantId)
                    put("outletId", e.outletId ?: JSONObject.NULL)
                    put("name", e.name)
                    put("email", e.email ?: JSONObject.NULL)
                    put("role", e.role)
                    put("pinHash", e.pinHash)
                    put("phone", e.phone ?: JSONObject.NULL)
                    put("salary", e.salary)
                    put("isActive", e.isActive)
                    put("payPeriod", e.payPeriod)
                    put("lastPaidAt", e.lastPaidAt ?: JSONObject.NULL)
                    put("emailVerified", e.emailVerified)
                    put("isSynced", true)
                    put("createdAt", e.createdAt)
                    put("updatedAt", e.updatedAt)
                })
                if (uploadTable(context, "employees", array)) {
                    db.employeeDao().markSynced(employeeId)
                    Log.i(TAG, "[pushEmployeeImmediate] OK push employee id=$employeeId ke VPS")
                } else {
                    Log.w(TAG, "[pushEmployeeImmediate] Gagal push employee id=$employeeId, akan diretry pada syncAll berikutnya")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "[pushEmployeeImmediate] error: ${ex.message}", ex)
            }
        }
    }

    fun deleteEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[deleteEmployeeImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                if (deleteRow(context, "employees", employeeId, activeTenantId)) {
                    db.employeeDao().deleteById(employeeId)
                    Log.i(TAG, "[deleteEmployeeImmediate] OK delete employee id=$employeeId di VPS")
                } else {
                    Log.w(TAG, "[deleteEmployeeImmediate] Gagal delete employee id=$employeeId di server, retry pada syncAll berikutnya")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "[deleteEmployeeImmediate] error: ${ex.message}", ex)
            }
        }
    }

    fun pushBmpEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[pushBmpEmployeeImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                val e = db.bmpEmployeeDao().getById(employeeId)
                if (e == null) {
                    Log.w(TAG, "[pushBmpEmployeeImmediate] BMP Employee id=$employeeId tidak ditemukan")
                    return@launch
                }
                if (e.tenantId != activeTenantId) {
                    Log.w(TAG, "[pushBmpEmployeeImmediate] tenantId mismatch (entity=${e.tenantId}, active=$activeTenantId)")
                    return@launch
                }
                val array = JSONArray()
                array.put(JSONObject().apply {
                    put("id", e.id)
                    put("tenantId", e.tenantId)
                    put("outletId", e.outletId ?: JSONObject.NULL)
                    put("name", e.name)
                    put("position", e.position ?: JSONObject.NULL)
                    put("salaryAmount", e.salaryAmount)
                    put("isActive", e.isActive)
                    put("fingerprintPIN", e.fingerprintPIN ?: JSONObject.NULL)
                    put("isSynced", true)
                    put("createdAt", e.createdAt)
                    put("updatedAt", e.updatedAt)
                })
                if (uploadTable(context, "bmp_employees", array)) {
                    db.bmpEmployeeDao().markSynced(employeeId)
                    Log.i(TAG, "[pushBmpEmployeeImmediate] OK push BMP employee id=$employeeId ke VPS")
                } else {
                    Log.w(TAG, "[pushBmpEmployeeImmediate] Gagal push BMP employee id=$employeeId, akan diretry pada syncAll berikutnya")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "[pushBmpEmployeeImmediate] error: ${ex.message}", ex)
            }
        }
    }

    fun deleteBmpEmployeeImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeId: Long,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[deleteBmpEmployeeImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                if (deleteRow(context, "bmp_employees", employeeId, activeTenantId)) {
                    db.bmpEmployeeDao().hardDelete(employeeId)
                    Log.i(TAG, "[deleteBmpEmployeeImmediate] OK delete BMP employee id=$employeeId di VPS")
                } else {
                    Log.w(TAG, "[deleteBmpEmployeeImmediate] Gagal delete BMP employee id=$employeeId di server, retry pada syncAll berikutnya")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "[deleteBmpEmployeeImmediate] error: ${ex.message}", ex)
            }
        }
    }

    fun pushBmpPayrollImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        payrollId: String,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                currentTenantId = activeTenantId
                if (!userEmail.isNullOrBlank()) currentUserEmail = userEmail
                if (!isNetworkAvailable(context)) {
                    Log.w(TAG, "[pushBmpPayrollImmediate] Offline, akan diretry pada syncAll berikutnya")
                    return@launch
                }
                val p = db.bmpPayrollDao().getById(payrollId)
                if (p == null) {
                    Log.w(TAG, "[pushBmpPayrollImmediate] BMP Payroll id=$payrollId tidak ditemukan")
                    return@launch
                }
                if (p.tenantId != activeTenantId) {
                    Log.w(TAG, "[pushBmpPayrollImmediate] tenantId mismatch (entity=${p.tenantId}, active=$activeTenantId)")
                    return@launch
                }
                val array = JSONArray()
                array.put(JSONObject().apply {
                    put("id", p.id)
                    put("tenantId", p.tenantId)
                    put("outletId", p.outletId ?: JSONObject.NULL)
                    put("employeeId", p.employeeId)
                    put("paymentDate", p.paymentDate)
                    put("amount", p.amount)
                    put("attendanceCount", p.attendanceCount)
                    put("dailyRate", p.dailyRate)
                    put("description", p.description ?: JSONObject.NULL)
                    put("isSynced", true)
                    put("createdAt", p.createdAt)
                })
                if (uploadTable(context, "bmp_payrolls", array)) {
                    db.bmpPayrollDao().markSynced(payrollId)
                    Log.i(TAG, "[pushBmpPayrollImmediate] OK push BMP payroll id=$payrollId ke VPS")
                } else {
                    Log.w(TAG, "[pushBmpPayrollImmediate] Gagal push BMP payroll id=$payrollId, akan diretry pada syncAll berikutnya")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "[pushBmpPayrollImmediate] error: ${ex.message}", ex)
            }
        }
    }

    fun syncEmployeeWithRawPasswordImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeEmail: String,
        rawPass: String
    ) {
        syncScope.launch {
            try {
                syncEmployeeWithRawPassword(context, db, activeTenantId, employeeEmail, rawPass)
            } catch (e: Exception) {
                Log.e(TAG, "[syncEmployeeWithRawPasswordImmediate] error", e)
            }
        }
    }

    fun syncEmployeePasswordChangeImmediate(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        employeeEmail: String,
        rawPass: String,
        ownerEmail: String
    ) {
        syncScope.launch {
            try {
                syncEmployeePasswordChange(context, db, activeTenantId, employeeEmail, rawPass, ownerEmail)
            } catch (e: Exception) {
                Log.e(TAG, "[syncEmployeePasswordChangeImmediate] error", e)
            }
        }
    }

    /**
     * Jalankan syncAll penuh di latar belakang menggunakan global scope.
     * Cocok dipanggil dari ViewModel sebagai fallback (mis. setelah edit transaksi)
     * tanpa khawatir dibatalkan ketika ViewModel di-clear.
     */
    fun enqueueFullSync(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        userEmail: String? = null
    ) {
        syncScope.launch {
            try {
                syncAll(context, db, activeTenantId, userEmail)
            } catch (e: Exception) {
                Log.e(TAG, "[enqueueFullSync] error: ${e.message}", e)
            }
        }
    }

    suspend fun hasUnsyncedOrDeletedChanges(db: PosBahDatabase, tenantId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tenant = db.tenantDao().getById(tenantId)
            val isBmp = tenant?.businessMode == "BMP"

            // Check common tables first
            val hasCommonUnsynced = db.outletDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                    db.employeeDao().getAll().any { it.tenantId == tenantId && !it.isSynced }
            if (hasCommonUnsynced) return@withContext true

            if (isBmp) {
                // Check BMP deleted records
                val hasBmpDeleted = db.bmpProductDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpCashFlowDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpPaymentDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpInvoiceDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpClientDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpBahanBakuItemDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpBahanBakuDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpMasterProductDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpProductStockDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpStockLedgerDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.bmpProductionLogDao().getDeletedIds(tenantId).isNotEmpty()
                if (hasBmpDeleted) return@withContext true

                // Check BMP unsynced records
                val hasBmpUnsynced = db.bmpClientDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpInvoiceDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpProductDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpMasterProductDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpPaymentDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpCashFlowDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpEmployeeDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpPayrollDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpBahanBakuDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpBahanBakuItemDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpProductStockDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpStockLedgerDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.bmpProductionLogDao().getAll().any { it.tenantId == tenantId && !it.isSynced }
                if (hasBmpUnsynced) return@withContext true
            } else {
                // FNB mode
                val hasFnbDeleted = db.productDao().getDeletedIds(tenantId).isNotEmpty() ||
                        db.transactionDao().getDeletedIds(tenantId).isNotEmpty()
                if (hasFnbDeleted) return@withContext true

                val hasFnbUnsynced = db.productDao().getAll().any { it.tenantId == tenantId && !it.isSynced } ||
                        db.customerDao().getAll().any { it.tenantId == tenantId && !it.isSynced }
                if (hasFnbUnsynced) return@withContext true
            }

            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "[hasUnsyncedOrDeletedChanges] error checking changes, fallback to true", e)
            return@withContext true
        }
    }

    private fun downloadMissingBahanBakuPhotos(
        context: Context,
        db: PosBahDatabase,
        activeTenantId: String,
        email: String
    ) {
        val sanitizedEmail = email.trim().lowercase()
        if (sanitizedEmail.isEmpty()) return

        syncScope.launch(Dispatchers.IO) {
            try {
                val folderName = "bahanbaku_$sanitizedEmail"
                val accountDir = File(context.filesDir, folderName)
                if (!accountDir.exists()) {
                    accountDir.mkdirs()
                }

                val list = db.bmpBahanBakuDao().getAll()
                    .filter { it.tenantId == activeTenantId && !it.notaFotoUrl.isNullOrBlank() }

                for (item in list) {
                    val urlStr = item.notaFotoUrl!!
                    val ext = when {
                        urlStr.endsWith(".png", ignoreCase = true) -> "png"
                        urlStr.endsWith(".webp", ignoreCase = true) -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "nota_${item.id}.$ext"
                    val targetFile = File(accountDir, fileName)

                    var downloadSuccess = false
                    if (targetFile.exists()) {
                        downloadSuccess = true
                    } else {
                        try {
                            val tempFile = File(accountDir, "$fileName.tmp")
                            val url = URL(urlStr)
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 10000
                            conn.readTimeout = 10000
                            conn.requestMethod = "GET"

                            val responseCode = conn.responseCode
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                conn.inputStream.use { input ->
                                    tempFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (tempFile.renameTo(targetFile)) {
                                    downloadSuccess = true
                                    Log.d(TAG, "Successfully downloaded: ${targetFile.absolutePath}")
                                } else {
                                    tempFile.delete()
                                    Log.e(TAG, "Failed to rename temp file to ${targetFile.absolutePath}")
                                }
                            } else {
                                Log.w(TAG, "Failed to download image from $urlStr, response code: $responseCode")
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error downloading photo for bahanbaku ${item.id}: ${e.message}")
                        }
                    }

                    if (downloadSuccess) {
                        val localPath = targetFile.absolutePath
                        if (item.notaFotoPath != localPath) {
                            val updated = item.copy(notaFotoPath = localPath)
                            db.bmpBahanBakuDao().update(updated)
                            Log.d(TAG, "Updated database notaFotoPath for ID ${item.id} to: $localPath")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadMissingBahanBakuPhotos: ${e.message}", e)
            }
        }
    }
}
