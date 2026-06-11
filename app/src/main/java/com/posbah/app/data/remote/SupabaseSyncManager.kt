package com.posbah.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.*
import kotlinx.coroutines.Dispatchers
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

    sealed class SyncResult {
        object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
        object NoConnection : SyncResult()
    }

    /** Check network connection status */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
                    })
                }
                uploadTable("local_users", array)
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
                uploadTable("tenants", array)
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
                uploadTable("outlets", array)
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
                        put("salary", e.salary)
                        put("isActive", e.isActive)
                        put("payPeriod", e.payPeriod)
                        put("lastPaidAt", e.lastPaidAt ?: JSONObject.NULL)
                        put("emailVerified", e.emailVerified)
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
                uploadTable("employees", array)
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
                if (uploadTable("bmp_clients", array)) {
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
                if (uploadTable("bmp_invoices", array)) {
                    unsyncedInvoices.forEach { db.bmpInvoiceDao().markSynced(it.id) }
                }
            }

            // 7. bmp_products (Metadata - upload all)
            val bmpProducts = db.bmpProductDao().getAll().filter { it.tenantId == activeTenantId }
            if (bmpProducts.isNotEmpty()) {
                val array = JSONArray()
                bmpProducts.forEach { p ->
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
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    })
                }
                uploadTable("bmp_products", array)
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
                uploadTable("bmp_master_products", array)
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
                if (uploadTable("bmp_invoice_payments", array)) {
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
                if (uploadTable("bmp_cashflow", array)) {
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
                uploadTable("bmp_settings", array)
            }

            // 12. bmp_employees (Metadata - upload all)
            val bmpEmployees = db.bmpEmployeeDao().getAll().filter { it.tenantId == activeTenantId }
            if (bmpEmployees.isNotEmpty()) {
                val array = JSONArray()
                bmpEmployees.forEach { e ->
                    array.put(JSONObject().apply {
                        put("id", e.id)
                        put("tenantId", e.tenantId)
                        put("name", e.name)
                        put("position", e.position ?: JSONObject.NULL)
                        put("salaryAmount", e.salaryAmount)
                        put("isActive", e.isActive)
                        put("fingerprintPIN", e.fingerprintPIN ?: JSONObject.NULL)
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
                uploadTable("bmp_employees", array)
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
                if (uploadTable("bmp_payrolls", array)) {
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
                if (uploadTable("bmp_bahan_baku", array)) {
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
                if (uploadTable("bmp_bahan_baku_item", array)) {
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
                        put("createdAt", ps.createdAt)
                        put("updatedAt", ps.updatedAt)
                    })
                }
                uploadTable("print_settings", array)
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
                uploadTable("products", array)
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
                uploadTable("customers", array)
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
                uploadTable("transactions", array)
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
                uploadTable("transaction_items", array)
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
                uploadTable("activity_logs", array)
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
    private fun uploadTable(tableName: String, jsonArray: JSONArray): Boolean {
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
}
