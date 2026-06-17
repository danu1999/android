package com.posbah.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    private var currentTenantId: String = ""

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
            return false
        }
        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            Log.w(TAG, "[Network] Tidak ada jaringan aktif terdeteksi")
            return false
        }
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: run {
            Log.w(TAG, "[Network] Gagal mengambil NetworkCapabilities")
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
    suspend fun syncAll(context: Context, db: PosBahDatabase, activeTenantId: String): SyncResult = withContext(Dispatchers.IO) {
        currentTenantId = activeTenantId
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "Sinkronisasi dibatalkan: tidak ada koneksi internet.")
            return@withContext SyncResult.NoConnection
        }

        try {
            Log.d(TAG, "Memulai sinkronisasi ke VPS...")

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

            // 3. outlets (Metadata - upload all)
            val outlets = db.outletDao().getAll().filter { it.tenantId == activeTenantId }
            if (outlets.isNotEmpty()) {
                val array = JSONArray()
                outlets.forEach { o ->
                    array.put(JSONObject().apply {
                        put("id", o.id)
                        put("tenantId", o.tenantId)
                        put("name", o.name)
                        put("address", o.address ?: JSONObject.NULL)
                        put("phone", o.phone ?: JSONObject.NULL)
                        put("isDefault", o.isDefault)
                        put("isOpen", o.isOpen)
                        put("currentEmployee", o.currentEmployee ?: JSONObject.NULL)
                        put("createdAt", o.createdAt)
                        put("updatedAt", o.updatedAt)
                    })
                }
                uploadTable(context, "outlets", array)
            }

            // 4. employees (Metadata - upload all)
            val employees = db.employeeDao().getAll().filter { it.tenantId == activeTenantId }
            if (employees.isNotEmpty()) {
                val array = JSONArray()
                employees.forEach { e ->
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
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
                uploadTable(context, "employees", array)
            }

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
 
            // 8. bmp_master_products (Metadata - upload all)
            val bmpMasterProducts = db.bmpMasterProductDao().getAll().filter { it.tenantId == activeTenantId }
            if (bmpMasterProducts.isNotEmpty()) {
                val array = JSONArray()
                bmpMasterProducts.forEach { mp ->
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
                        put("createdAt", mp.createdAt)
                        put("updatedAt", mp.updatedAt)
                    })
                }
                uploadTable(context, "bmp_master_products", array)
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
 
            // 12. bmp_employees (Metadata - upload all)
            val bmpEmployees = db.bmpEmployeeDao().getAll().filter { it.tenantId == activeTenantId }
            if (bmpEmployees.isNotEmpty()) {
                val array = JSONArray()
                bmpEmployees.forEach { e ->
                    array.put(JSONObject().apply {
                        put("id", e.id)
                        put("tenantId", e.tenantId)
                        put("outletId", e.outletId ?: JSONObject.NULL)
                        put("name", e.name)
                        put("position", e.position ?: JSONObject.NULL)
                        put("salaryAmount", e.salaryAmount)
                        put("isActive", e.isActive)
                        put("fingerprintPIN", e.fingerprintPIN ?: JSONObject.NULL)
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
                uploadTable(context, "bmp_employees", array)
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
 
            // 17. products (Metadata - upload all)
            val productsList = db.productDao().getAll().filter { it.tenantId == activeTenantId }
            if (productsList.isNotEmpty()) {
                val array = JSONArray()
                productsList.forEach { p ->
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
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    })
                }
                uploadTable(context, "products", array)
            }
 
            // 18. customers (Metadata - upload all)
            val customersList = db.customerDao().getAll().filter { it.tenantId == activeTenantId }
            if (customersList.isNotEmpty()) {
                val array = JSONArray()
                customersList.forEach { c ->
                    array.put(JSONObject().apply {
                        put("id", c.id)
                        put("tenantId", c.tenantId)
                        put("name", c.name)
                        put("phone", c.phone ?: JSONObject.NULL)
                        put("address", c.address ?: JSONObject.NULL)
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
                uploadTable(context, "customers", array)
            }
 
            // 19. transactions (Operational / POS Transactions - upload all)
            val transactionsList = db.transactionDao().getAll().filter { it.tenantId == activeTenantId }
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
                        put("action", l.action)
                        put("description", l.description)
                        put("date", l.date)
                        put("employeeName", l.employeeName)
                        put("appMode", l.appMode)
                    })
                }
                uploadTable(context, "activity_logs", array)
            }
 
            Log.d(TAG, "Sinkronisasi selesai dengan sukses.")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sinkronisasi gagal: ${e.message}", e)
            SyncResult.Error(e.message ?: "Error tidak terduga saat sinkronisasi.")
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
                setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = securePrefs.currentEmail
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
                true
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal mengunggah tabel: $tableName ($responseCode): $errorStream")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "IO Exception saat mengunggah tabel $tableName: ${e.message}")
            false
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
                setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.d(TAG, "Berhasil menghapus baris dari tabel $tableName di server: ID $id")
                true
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal menghapus baris dari tabel $tableName di server: ID $id ($responseCode): $errorStream")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat menghapus baris dari tabel $tableName: ${e.message}", e)
            false
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
                setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                val securePrefs = com.posbah.app.security.SecurePreferences(context)
                val email = securePrefs.currentEmail
                if (!email.isNullOrBlank()) {
                    setRequestProperty("x-user-email", email)
                }
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                JSONArray(responseText)
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Gagal mengunduh tabel: $tableName ($responseCode): $errorStream")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat mengunduh tabel $tableName: ${e.message}", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Jalankan sinkronisasi unduh penuh untuk data Master: outlets, employees, dan products.
     */
    suspend fun pullAll(context: Context, db: PosBahDatabase, activeTenantId: String): SyncResult = withContext(Dispatchers.IO) {
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
            Log.d(TAG, "Memulai pull data master dari VPS...")

            // 1. Pull outlets
            val outletsArray = pullTable(context, "outlets", activeTenantId)
            if (outletsArray != null) {
                val list = mutableListOf<Outlet>()
                for (i in 0 until outletsArray.length()) {
                    val obj = outletsArray.getJSONObject(i)
                    list.add(Outlet(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
                        name = obj.optString("name"),
                        address = if (obj.isNull("address")) null else obj.optString("address"),
                        phone = if (obj.isNull("phone")) null else obj.optString("phone"),
                        isDefault = obj.optBoolean("isDefault", false),
                        isOpen = obj.optBoolean("isOpen", true),
                        currentEmployee = if (obj.isNull("currentEmployee")) null else obj.optString("currentEmployee"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.outletDao().insert(it) }
                }
            }

            // 2. Pull employees
            val employeesArray = pullTable(context, "employees", activeTenantId)
            if (employeesArray != null) {
                val list = mutableListOf<Employee>()
                for (i in 0 until employeesArray.length()) {
                    val obj = employeesArray.getJSONObject(i)
                    val outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId")
                    list.add(Employee(
                        id = obj.optLong("id", 0L),
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
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.employeeDao().insert(it) }
                }
            }

            // 3. Pull products
            val productsArray = pullTable(context, "products", activeTenantId)
            if (productsArray != null) {
                val list = mutableListOf<ProductEntity>()
                for (i in 0 until productsArray.length()) {
                    val obj = productsArray.getJSONObject(i)
                    val outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId")
                    list.add(ProductEntity(
                        id = obj.optLong("id", 0L),
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
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    db.productDao().insertAll(list)
                }
            }

            // 4. Pull customers
            val customersArray = pullTable(context, "customers", activeTenantId)
            if (customersArray != null) {
                val list = mutableListOf<CustomerEntity>()
                for (i in 0 until customersArray.length()) {
                    val obj = customersArray.getJSONObject(i)
                    list.add(CustomerEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
                        name = obj.optString("name"),
                        phone = if (obj.isNull("phone")) null else obj.optString("phone"),
                        address = if (obj.isNull("address")) null else obj.optString("address"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    db.customerDao().insertAll(list)
                }
            }

            // 5. Pull transactions
            val transactionsArray = pullTable(context, "transactions", activeTenantId)
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
                    
                    // Identify active or unpaid (BELUM LUNAS) transactions to pull their items
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
                        paymentMethod = obj.optString("paymentMethod", "CASH"),
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
                if (transactionEntities.isNotEmpty()) {
                    transactionEntities.forEach { db.transactionDao().insert(it) }
                }
            }

            // 6. Pull transaction_items
            // For active Rental/Laundry: always pull items so status is current on any device
            // For FNB/BMP: pull items for all transactions from last 30 days (supports HP-ganti use case)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val recentFnbTxIds = transactionEntities.filter { tx ->
                val isRental = tx.receiptNumber.startsWith("RN-")
                val isLaundry = tx.receiptNumber.startsWith("LD-")
                !isRental && !isLaundry && tx.createdAt >= thirtyDaysAgo
            }.map { it.id }.toSet()

            val allTargetTxIds = (activeTxIds + recentFnbTxIds).toSet()
            for (txId in allTargetTxIds) {
                val itemsArray = pullTable(context, "transaction_items", activeTenantId, "transactionId=eq.$txId")
                if (itemsArray != null) {
                    val itemsList = mutableListOf<TransactionItemEntity>()
                    for (i in 0 until itemsArray.length()) {
                        val obj = itemsArray.getJSONObject(i)
                        val variantId = if (obj.isNull("variantId")) null else obj.optLong("variantId")
                        val variantName = if (obj.isNull("variantName")) null else obj.optString("variantName")
                        itemsList.add(TransactionItemEntity(
                            id = obj.optLong("id", 0L),
                            transactionId = obj.optLong("transactionId", txId),
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
                    if (itemsList.isNotEmpty()) {
                        db.transactionItemDao().insertAll(itemsList)
                    }
                }
            }

            // 7. Pull bmp_clients
            val bmpClientsArray = pullTable(context, "bmp_clients", activeTenantId)
            if (bmpClientsArray != null) {
                val list = mutableListOf<BmpClientEntity>()
                for (i in 0 until bmpClientsArray.length()) {
                    val obj = bmpClientsArray.getJSONObject(i)
                    list.add(BmpClientEntity(
                        id = obj.optLong("id", 0L),
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
                    list.forEach { db.bmpClientDao().upsert(it) }
                }
            }

            // 8. Pull bmp_invoices
            val bmpInvoicesArray = pullTable(context, "bmp_invoices", activeTenantId)
            if (bmpInvoicesArray != null) {
                val list = mutableListOf<BmpInvoiceEntity>()
                for (i in 0 until bmpInvoicesArray.length()) {
                    val obj = bmpInvoicesArray.getJSONObject(i)
                    list.add(BmpInvoiceEntity(
                        id = obj.optLong("id", 0L),
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
                    list.forEach { db.bmpInvoiceDao().upsert(it) }
                }
            }

            // 9. Pull bmp_products
            val bmpProductsArray = pullTable(context, "bmp_products", activeTenantId)
            if (bmpProductsArray != null) {
                val list = mutableListOf<BmpProductEntity>()
                for (i in 0 until bmpProductsArray.length()) {
                    val obj = bmpProductsArray.getJSONObject(i)
                    list.add(BmpProductEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
                        invoiceId = if (obj.isNull("invoiceId")) null else obj.optLong("invoiceId"),
                        masterItemID = if (obj.isNull("masterItemID")) null else obj.optLong("masterItemID"),
                        title = obj.optString("title"),
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
                    db.bmpProductDao().insertAll(list)
                }
            }

            // 10. Pull bmp_master_products
            val bmpMasterProductsArray = pullTable(context, "bmp_master_products", activeTenantId)
            if (bmpMasterProductsArray != null) {
                val list = mutableListOf<BmpMasterProductEntity>()
                for (i in 0 until bmpMasterProductsArray.length()) {
                    val obj = bmpMasterProductsArray.getJSONObject(i)
                    list.add(BmpMasterProductEntity(
                        id = obj.optLong("id", 0L),
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
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.bmpMasterProductDao().upsert(it) }
                }
            }

            // 11. Pull bmp_invoice_payments
            val bmpPaymentsArray = pullTable(context, "bmp_invoice_payments", activeTenantId)
            if (bmpPaymentsArray != null) {
                val list = mutableListOf<BmpInvoicePaymentEntity>()
                for (i in 0 until bmpPaymentsArray.length()) {
                    val obj = bmpPaymentsArray.getJSONObject(i)
                    list.add(BmpInvoicePaymentEntity(
                        id = obj.optLong("id", 0L),
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
            }

            // 12. Pull bmp_cashflow
            val bmpCashFlowArray = pullTable(context, "bmp_cashflow", activeTenantId)
            if (bmpCashFlowArray != null) {
                val list = mutableListOf<BmpCashFlowEntity>()
                for (i in 0 until bmpCashFlowArray.length()) {
                    val obj = bmpCashFlowArray.getJSONObject(i)
                    list.add(BmpCashFlowEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
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
            }

            // 13. Pull bmp_settings
            val bmpSettingsArray = pullTable(context, "bmp_settings", activeTenantId)
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
                    list.forEach { db.bmpSettingsDao().upsert(it) }
                }
            }

            // 14. Pull bmp_employees
            val bmpEmployeesArray = pullTable(context, "bmp_employees", activeTenantId)
            if (bmpEmployeesArray != null) {
                val list = mutableListOf<BmpEmployeeEntity>()
                for (i in 0 until bmpEmployeesArray.length()) {
                    val obj = bmpEmployeesArray.getJSONObject(i)
                    list.add(BmpEmployeeEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
                        outletId = if (obj.isNull("outletId")) null else obj.optLong("outletId"),
                        name = obj.optString("name"),
                        position = if (obj.isNull("position")) null else obj.optString("position"),
                        salaryAmount = obj.optDouble("salaryAmount", 0.0),
                        isActive = obj.optBoolean("isActive", true),
                        fingerprintPIN = if (obj.isNull("fingerprintPIN")) null else obj.optString("fingerprintPIN"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    ))
                }
                if (list.isNotEmpty()) {
                    list.forEach { db.bmpEmployeeDao().upsert(it) }
                }
            }

            // 15. Pull bmp_payrolls
            val bmpPayrollsArray = pullTable(context, "bmp_payrolls", activeTenantId)
            if (bmpPayrollsArray != null) {
                val list = mutableListOf<BmpPayrollEntity>()
                for (i in 0 until bmpPayrollsArray.length()) {
                    val obj = bmpPayrollsArray.getJSONObject(i)
                    list.add(BmpPayrollEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
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
            }

            // 16. Pull bmp_bahan_baku
            val bmpBahanBakuArray = pullTable(context, "bmp_bahan_baku", activeTenantId)
            if (bmpBahanBakuArray != null) {
                val list = mutableListOf<BmpBahanBakuEntity>()
                for (i in 0 until bmpBahanBakuArray.length()) {
                    val obj = bmpBahanBakuArray.getJSONObject(i)
                    list.add(BmpBahanBakuEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
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
                    list.forEach { db.bmpBahanBakuDao().upsert(it) }
                }
            }

            // 17. Pull bmp_bahan_baku_item
            val bmpBahanBakuItemsArray = pullTable(context, "bmp_bahan_baku_item", activeTenantId)
            if (bmpBahanBakuItemsArray != null) {
                val list = mutableListOf<BmpBahanBakuItemEntity>()
                for (i in 0 until bmpBahanBakuItemsArray.length()) {
                    val obj = bmpBahanBakuItemsArray.getJSONObject(i)
                    list.add(BmpBahanBakuItemEntity(
                        id = obj.optLong("id", 0L),
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
                    db.bmpBahanBakuItemDao().insertAll(list)
                }
            }

            // 18. Pull print_settings
            val printSettingsArray = pullTable(context, "print_settings", activeTenantId)
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
                    list.forEach { db.printSettingsDao().upsert(it) }
                }
            }

            // 19. Pull activity_logs
            val activityLogsArray = pullTable(context, "activity_logs", activeTenantId)
            if (activityLogsArray != null) {
                val list = mutableListOf<com.posbah.app.data.local.entities.ActivityLogEntity>()
                for (i in 0 until activityLogsArray.length()) {
                    val obj = activityLogsArray.getJSONObject(i)
                    list.add(com.posbah.app.data.local.entities.ActivityLogEntity(
                        id = obj.optLong("id", 0L),
                        tenantId = obj.optString("tenantId", activeTenantId),
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

            Log.d(TAG, "Pull sinkronisasi selesai dengan sukses.")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Pull sinkronisasi gagal: ${e.message}", e)
            SyncResult.Error(e.message ?: "Error tidak terduga saat pull sinkronisasi.")
        } finally {
            if (pullMutex.isLocked) pullMutex.unlock()
        }
    }
}
