package com.posbah.app.data.local

import android.content.Context
import com.posbah.app.data.local.dao.CustomerDao
import com.posbah.app.data.local.dao.EmployeeDao
import com.posbah.app.data.local.dao.ProductDao
import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpCashFlowEntity
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.local.entities.PrintSettingsEntity
import com.posbah.app.data.local.entities.BmpEmployeeEntity
import com.posbah.app.data.local.entities.BmpPayrollEntity
import com.posbah.app.data.local.entities.BmpBahanBakuEntity
import com.posbah.app.data.local.entities.BmpBahanBakuItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSeeder @Inject constructor(
    private val db: PosBahDatabase,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val employeeDao: EmployeeDao
) {
    /**
     * Parse timestamp string from PostgreSQL COPY statement (UTC) to epoch millisecond Long.
     */
    private fun parseSqlTimestamp(str: String?): Long {
        if (str.isNullOrBlank() || str == "\\N") return System.currentTimeMillis()
        val clean = str.trim()
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss.SS",
            "yyyy-MM-dd HH:mm:ss.S",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsedDate = sdf.parse(clean)
                if (parsedDate != null) {
                    return parsedDate.time
                }
            } catch (e: Exception) {
                // try next format
            }
        }
        return clean.toLongOrNull() ?: System.currentTimeMillis()
    }

    /**
     * Seeds local products, customers, and employees from the local assets SQL dump.
     * Overwrites/replaces duplicate records dynamically.
     * Specific database dumps are only seeded for "bahteramulyap@gmail.com" and "demo_tenant".
     */
    suspend fun seedFromSqlDump(context: Context, tenantId: String, outletId: Long?) = withContext(Dispatchers.IO) {
        // Only seed SQL dump data for bahteramulyap@gmail.com, hanafiariful@gmail.com, and demo_tenant accounts.
        // Other new premium accounts will start with a fresh/empty database space.
        if (tenantId != "bahteramulyap@gmail.com" && 
            tenantId != "ten_premium_bahteramulyap_gmail_com" && 
            tenantId != "demo_tenant" && 
            !tenantId.contains("hanafiariful_gmail_com") && 
            tenantId != "hanafiariful@gmail.com" &&
            !tenantId.startsWith("demo_tenant_")) {
            return@withContext
        }

        try {
            val tenant = db.tenantDao().getById(tenantId)
            val isBmpMode = tenant?.businessMode == "BMP"
            val isDemo = tenantId == "demo_tenant" || tenantId.startsWith("demo_tenant_")

            if (!isDemo) {
                val assetManager = context.assets
        // Select the right seed SQL file based on tenantId
        val sqlFileName = when {
            tenantId == "hanafiariful@gmail.com" || tenantId.contains("hanafiariful_gmail_com") ->
                "posbah_tenant_hanafiariful_gmail_com.sql"
            else -> "posbah_tenant_bahteramulyap_gmail_com.sql"
        }
        val inputStream = assetManager.open(sqlFileName)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

        var currentTable: String? = null
        var columns = listOf<String>()
        val productList = mutableListOf<ProductEntity>()
        val customerList = mutableListOf<CustomerEntity>()
        val employeeList = mutableListOf<Employee>()
        val transactionList = mutableListOf<TransactionEntity>()
        val transactionItemList = mutableListOf<TransactionItemEntity>()

        // BMP lists
        val bmpClientList = mutableListOf<BmpClientEntity>()
        val bmpInvoiceList = mutableListOf<BmpInvoiceEntity>()
        val bmpProductList = mutableListOf<BmpProductEntity>()
        val bmpMasterProductList = mutableListOf<BmpMasterProductEntity>()
        val bmpInvoicePaymentList = mutableListOf<BmpInvoicePaymentEntity>()
        val bmpCashFlowList = mutableListOf<BmpCashFlowEntity>()
        val bmpSettingsList = mutableListOf<BmpSettingsEntity>()
        val bmpEmployeeList = mutableListOf<BmpEmployeeEntity>()
        val bmpPayrollList = mutableListOf<BmpPayrollEntity>()
        val bmpBahanBakuList = mutableListOf<BmpBahanBakuEntity>()
        val bmpBahanBakuItemList = mutableListOf<BmpBahanBakuItemEntity>()

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("COPY public.")) {
                    // Match COPY public."TableName" (col1, col2) FROM stdin;
                    val matchResult = Regex("""COPY public\."(\w+)" \((.+)\) FROM stdin;""").find(trimmed)
                    if (matchResult != null) {
                        currentTable = matchResult.groupValues[1]
                        columns = matchResult.groupValues[2].split(",").map { it.replace("\"", "").trim() }
                    }
                } else if (trimmed == "\\.") {
                    currentTable = null
                    columns = emptyList()
                } else if (currentTable != null) {
                    // SQL COPY dump data rows are tab-separated
                    val parts = line.split("\t")
                    if (parts.size >= columns.size) {
                        val rowMap = columns.zip(parts).toMap()

                        val isPosTable = currentTable in listOf("Product", "Customer", "Employee", "Transaction", "TransactionItem")
                        val isBmpTable = currentTable in listOf(
                            "BmpClient", "BmpInvoice", "BmpProduct", "BmpMasterProduct",
                            "BmpInvoicePayment", "BmpCashFlow", "BmpSettings", "BmpEmployee",
                            "BmpPayroll", "BmpBahanNono", "BmpBahanNonoItem"
                        )
                        if (isPosTable && isBmpMode) continue
                        if (isBmpTable && !isBmpMode) continue

                        when (currentTable) {
                            "Product" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: 0L
                                val name = rowMap["name"]
                                if (name.isNullOrBlank() || name == "\\N") continue
                                val price = rowMap["price"]?.toDoubleOrNull() ?: 0.0
                                val costPrice = rowMap["costPrice"]?.toDoubleOrNull() ?: 0.0
                                val stock = rowMap["stock"]?.toIntOrNull() ?: 0
                                val unit = rowMap["unit"]?.takeIf { it != "\\N" } ?: "pcs"
                                val barcode = rowMap["barcode"]?.takeIf { it != "\\N" }
                                val category = rowMap["category"]?.takeIf { it != "\\N" } ?: "Umum"
                                val wholesaleEnabled = rowMap["wholesaleEnabled"] == "t"
                                val wholesalePrices = rowMap["wholesalePrices"]?.takeIf { it != "\\N" }
                                val variants = rowMap["variants"]?.takeIf { it != "\\N" }
                                val image = rowMap["image"]?.takeIf { it != "\\N" }

                                productList.add(
                                    ProductEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        outletId = outletId,
                                        name = name,
                                        price = price,
                                        costPrice = costPrice,
                                        stock = stock,
                                        unit = unit,
                                        barcode = barcode,
                                        category = category,
                                        wholesaleEnabled = wholesaleEnabled,
                                        wholesalePrices = wholesalePrices,
                                        variants = variants,
                                        image = image
                                    )
                                )
                            }
                            "Customer" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: 0L
                                val name = rowMap["name"]
                                if (name.isNullOrBlank() || name == "\\N") continue
                                val phone = rowMap["phone"]?.takeIf { it != "\\N" }
                                val address = rowMap["address"]?.takeIf { it != "\\N" }

                                customerList.add(
                                    CustomerEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        name = name,
                                        phone = phone,
                                        address = address
                                    )
                                )
                            }
                            "Employee" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: 0L
                                val name = rowMap["name"]
                                if (name.isNullOrBlank() || name == "\\N") continue
                                val role = rowMap["role"]?.takeIf { it != "\\N" } ?: "KASIR"
                                val pinHash = rowMap["pin"]?.takeIf { it != "\\N" && it.isNotBlank() } ?: ""
                                val salary = rowMap["salary"]?.toDoubleOrNull() ?: 0.0
                                val email = rowMap["email"]?.takeIf { it != "\\N" }

                                // Skip CV Bahtera Mulya Plastik employees if seeding into another tenant (e.g. FNB or demo)
                                val isBmpEmployee = email == "bahteramulyap@gmail.com" || email == "syerlirahma7@gmail.com"
                                val isBmpTenant = tenantId == "bahteramulyap@gmail.com" || tenantId == "ten_premium_bahteramulyap_gmail_com"
                                if (isBmpEmployee && !isBmpTenant) {
                                    continue
                                }

                                employeeList.add(
                                    Employee(
                                        id = id,
                                        tenantId = tenantId,
                                        outletId = outletId,
                                        name = name,
                                        email = email,
                                        role = role,
                                        pinHash = pinHash,
                                        salary = salary
                                    )
                                )
                            }
                            "Transaction" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: 0L
                                val receiptNumber = rowMap["receiptNumber"] ?: continue
                                if (receiptNumber.isBlank() || receiptNumber == "\\N") continue
                                val dateMs = parseSqlTimestamp(rowMap["date"])
                                val subtotal = rowMap["subtotal"]?.toDoubleOrNull() ?: 0.0
                                val discountAmt = rowMap["discountAmt"]?.toDoubleOrNull() ?: 0.0
                                val total = rowMap["total"]?.toDoubleOrNull() ?: subtotal
                                val discount = rowMap["discount"]?.toDoubleOrNull() ?: 0.0
                                val paymentMethod = rowMap["paymentMethod"]?.takeIf { it != "\\N" } ?: "CASH"
                                val type = rowMap["type"]?.takeIf { it != "\\N" } ?: "SALES"
                                val status = rowMap["status"]?.takeIf { it != "\\N" } ?: "COMPLETED"
                                val employeeId = rowMap["employeeId"]?.toLongOrNull() ?: 1L

                                transactionList.add(
                                    TransactionEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        outletId = outletId,
                                        employeeId = employeeId,
                                        receiptNumber = receiptNumber,
                                        date = dateMs,
                                        subtotal = subtotal,
                                        discountAmt = discountAmt,
                                        total = total,
                                        discount = discount,
                                        paymentMethod = paymentMethod,
                                        type = type,
                                        status = status,
                                        createdAt = dateMs,
                                        updatedAt = dateMs
                                    )
                                )
                            }
                            "TransactionItem" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val transactionId = rowMap["transactionId"]?.toLongOrNull() ?: continue
                                val productId = rowMap["productId"]?.toLongOrNull() ?: continue
                                val variantId = rowMap["variantId"]?.toLongOrNull()
                                val variantName = rowMap["variantName"]?.takeIf { it != "\\N" }
                                val quantity = rowMap["quantity"]?.toIntOrNull() ?: 1
                                val price = rowMap["price"]?.toDoubleOrNull() ?: 0.0
                                val costPrice = rowMap["costPrice"]?.toDoubleOrNull() ?: 0.0
                                val discount = rowMap["discount"]?.toDoubleOrNull() ?: 0.0
                                val note = rowMap["note"]?.takeIf { it != "\\N" }

                                transactionItemList.add(
                                    TransactionItemEntity(
                                        id = id,
                                        transactionId = transactionId,
                                        productId = productId,
                                        variantId = variantId,
                                        variantName = variantName,
                                        quantity = quantity,
                                        price = price,
                                        costPrice = costPrice,
                                        discount = discount,
                                        note = note
                                    )
                                )
                            }
                            "BmpClient" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val clientName = rowMap["clientName"] ?: ""
                                if (clientName.isBlank() || clientName == "\\N") continue
                                val saldoTitipan = rowMap["saldoTitipan"]?.toDoubleOrNull() ?: 0.0
                                val addressLine1 = rowMap["addressLine1"]?.takeIf { it != "\\N" }
                                val clientLogo = rowMap["clientLogo"]?.takeIf { it != "\\N" }
                                val province = rowMap["province"]?.takeIf { it != "\\N" }
                                val postalCode = rowMap["postalCode"]?.takeIf { it != "\\N" }
                                val phoneNumber = rowMap["phoneNumber"]?.takeIf { it != "\\N" }
                                val emailAddress = rowMap["emailAddress"]?.takeIf { it != "\\N" }
                                val taxNumber = rowMap["taxNumber"]?.takeIf { it != "\\N" }
                                val uniqueID = rowMap["uniqueID"]?.takeIf { it != "\\N" }
                                val slug = rowMap["slug"]?.takeIf { it != "\\N" }
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])
                                val updatedAt = parseSqlTimestamp(rowMap["updatedAt"])

                                bmpClientList.add(
                                    BmpClientEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        outletId = outletId,
                                        clientName = clientName,
                                        saldoTitipan = saldoTitipan,
                                        addressLine1 = addressLine1,
                                        clientLogo = clientLogo,
                                        province = province,
                                        postalCode = postalCode,
                                        phoneNumber = phoneNumber,
                                        emailAddress = emailAddress,
                                        taxNumber = taxNumber,
                                        uniqueID = uniqueID,
                                        slug = slug,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                            "BmpInvoice" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val title = rowMap["title"] ?: ""
                                val number = rowMap["number"] ?: ""
                                val dueDate = rowMap["dueDate"]?.let { parseSqlTimestamp(it) }
                                val paymentTerms = rowMap["paymentTerms"] ?: "14 days"
                                val status = rowMap["status"] ?: "DRAFT"
                                val notes = rowMap["notes"]?.takeIf { it != "\\N" }
                                val clientId = rowMap["clientId"]?.toLongOrNull()
                                val uniqueID = rowMap["uniqueID"]?.takeIf { it != "\\N" }
                                val slug = rowMap["slug"] ?: ""
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])
                                val updatedAt = parseSqlTimestamp(rowMap["updatedAt"])

                                bmpInvoiceList.add(
                                    BmpInvoiceEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        outletId = outletId,
                                        clientId = clientId,
                                        title = title,
                                        number = number,
                                        dueDate = dueDate,
                                        paymentTerms = paymentTerms,
                                        status = status,
                                        notes = notes,
                                        totalAmount = 0.0,
                                        paidAmount = 0.0,
                                        uniqueID = uniqueID,
                                        slug = slug,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                            "BmpProduct" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val title = rowMap["title"] ?: ""
                                if (title.isBlank() || title == "\\N") continue
                                val masterItemID = rowMap["masterItemID"]?.toLongOrNull()
                                val unit = rowMap["unit"] ?: "pcs"
                                val price = rowMap["price"]?.toDoubleOrNull() ?: 0.0
                                val jumlahLusin = rowMap["jumlahLusin"]?.toDoubleOrNull() ?: 1.0
                                val quantity = rowMap["quantity"]?.toDoubleOrNull() ?: 0.0
                                val isKhusus = rowMap["isKhusus"] == "t"
                                val hargaBeli = rowMap["hargaBeli"]?.toDoubleOrNull() ?: 0.0
                                val currency = rowMap["currency"] ?: "Rp"
                                val invoiceId = rowMap["invoiceId"]?.toLongOrNull()
                                val uniqueID = rowMap["uniqueID"]?.takeIf { it != "\\N" }
                                val slug = rowMap["slug"]?.takeIf { it != "\\N" }
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])
                                val updatedAt = parseSqlTimestamp(rowMap["updatedAt"])

                                bmpProductList.add(
                                    BmpProductEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        invoiceId = invoiceId,
                                        masterItemID = masterItemID,
                                        title = title,
                                        unit = unit,
                                        price = price,
                                        jumlahLusin = jumlahLusin,
                                        quantity = quantity,
                                        isKhusus = isKhusus,
                                        hargaBeli = hargaBeli,
                                        currency = currency,
                                        uniqueID = uniqueID,
                                        slug = slug,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                            "BmpMasterProduct" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val title = rowMap["title"] ?: ""
                                if (title.isBlank() || title == "\\N") continue
                                val description = rowMap["description"]?.takeIf { it != "\\N" }
                                val unit = rowMap["unit"] ?: "Kg"
                                val price = rowMap["price"]?.toDoubleOrNull() ?: 0.0
                                val beratGram = rowMap["beratGram"]?.toDoubleOrNull() ?: 0.0
                                val cycleTime = rowMap["cycleTime"]?.toDoubleOrNull() ?: 0.0
                                val cavity = rowMap["cavity"]?.toIntOrNull() ?: 1
                                val rejectRate = rowMap["rejectRate"]?.toDoubleOrNull() ?: 0.0
                                val uniqueID = rowMap["uniqueID"]?.takeIf { it != "\\N" }
                                val slug = rowMap["slug"]?.takeIf { it != "\\N" }
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])
                                val updatedAt = parseSqlTimestamp(rowMap["updatedAt"])

                                bmpMasterProductList.add(
                                    BmpMasterProductEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        title = title,
                                        description = description,
                                        unit = unit,
                                        price = price,
                                        beratGram = beratGram,
                                        cycleTime = cycleTime,
                                        cavity = cavity,
                                        rejectRate = rejectRate,
                                        uniqueID = uniqueID,
                                        slug = slug,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                            "BmpInvoicePayment" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val invoiceId = rowMap["invoiceId"]?.toLongOrNull() ?: continue
                                val paymentDate = parseSqlTimestamp(rowMap["paymentDate"])
                                val paymentAmount = rowMap["paymentAmount"]?.toDoubleOrNull() ?: 0.0
                                val paymentMethod = rowMap["paymentMethod"] ?: "TRANSFER"
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])

                                bmpInvoicePaymentList.add(
                                    BmpInvoicePaymentEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        invoiceId = invoiceId,
                                        paymentDate = paymentDate,
                                        paymentAmount = paymentAmount,
                                        paymentMethod = paymentMethod,
                                        notes = null,
                                        createdAt = createdAt
                                    )
                                )
                            }
                            "BmpCashFlow" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val transactionDate = parseSqlTimestamp(rowMap["transactionDate"])
                                val transactionType = rowMap["transactionType"] ?: "MASUK"
                                val description = rowMap["description"] ?: ""
                                val amount = rowMap["amount"]?.toDoubleOrNull() ?: 0.0
                                val paymentRefId = rowMap["paymentRefId"]?.toLongOrNull()
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])

                                bmpCashFlowList.add(
                                    BmpCashFlowEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        transactionDate = transactionDate,
                                        transactionType = transactionType,
                                        description = description,
                                        amount = amount,
                                        paymentRefId = paymentRefId,
                                        createdAt = createdAt
                                    )
                                )
                            }
                            "BmpSettings" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val clientName = rowMap["clientName"] ?: ""
                                // KEAMANAN: clientLogo sengaja tidak di-seed karena berupa
                                // path file lokal perangkat yang tidak valid di perangkat lain
                                // dan bisa menyebabkan kebocoran data antar-tenant.
                                // User harus upload ulang logo mereka sendiri.
                                val clientLogo: String? = null
                                val addressLine1 = rowMap["addressLine1"]?.takeIf { it != "\\N" }
                                val province = rowMap["province"]?.takeIf { it != "\\N" }
                                val postalCode = rowMap["postalCode"]?.takeIf { it != "\\N" }
                                val phoneNumber = rowMap["phoneNumber"]?.takeIf { it != "\\N" }
                                val emailAddress = rowMap["emailAddress"]?.takeIf { it != "\\N" }
                                val taxNumber = rowMap["taxNumber"]?.takeIf { it != "\\N" }
                                val listrikBulanan = rowMap["listrikBulanan"]?.toDoubleOrNull() ?: 30_000_000.0
                                val jumlahMesin = rowMap["jumlahMesin"]?.toIntOrNull() ?: 5
                                val jumlahKaryawan = rowMap["jumlahKaryawan"]?.toIntOrNull() ?: 19
                                val gajiHarian = rowMap["gajiHarian"]?.toDoubleOrNull() ?: 80_000.0
                                val hariKerjaSebulan = rowMap["hariKerjaSebulan"]?.toIntOrNull() ?: 26
                                val biayaKarungPer1000 = rowMap["biayaKarungPer1000"]?.toDoubleOrNull() ?: 2_100_000.0
                                val hoursPerDay = rowMap["hoursPerDay"]?.toIntOrNull() ?: 24
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])
                                val updatedAt = parseSqlTimestamp(rowMap["updatedAt"])

                                bmpSettingsList.add(
                                    BmpSettingsEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        clientName = clientName,
                                        clientLogo = clientLogo,
                                        addressLine1 = addressLine1,
                                        province = province,
                                        postalCode = postalCode,
                                        phoneNumber = phoneNumber,
                                        emailAddress = emailAddress,
                                        taxNumber = taxNumber,
                                        listrikBulanan = listrikBulanan,
                                        jumlahMesin = jumlahMesin,
                                        jumlahKaryawan = jumlahKaryawan,
                                        gajiHarian = gajiHarian,
                                        hariKerjaSebulan = hariKerjaSebulan,
                                        biayaKarungPer1000 = biayaKarungPer1000,
                                        hoursPerDay = hoursPerDay,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                            "BmpEmployee" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val name = rowMap["name"] ?: ""
                                if (name.isBlank() || name == "\\N") continue
                                val position = rowMap["position"]?.takeIf { it != "\\N" }
                                val salaryAmount = rowMap["salaryAmount"]?.toDoubleOrNull() ?: 0.0
                                val isActive = rowMap["isActive"] == "t"
                                val fingerprintPIN = rowMap["fingerprintPIN"]?.takeIf { it != "\\N" }
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])
                                val updatedAt = parseSqlTimestamp(rowMap["updatedAt"])

                                bmpEmployeeList.add(
                                    BmpEmployeeEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        name = name,
                                        position = position,
                                        salaryAmount = salaryAmount,
                                        isActive = isActive,
                                        fingerprintPIN = fingerprintPIN,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                            "BmpPayroll" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val employeeId = rowMap["employeeId"]?.toLongOrNull() ?: continue
                                val paymentDate = parseSqlTimestamp(rowMap["paymentDate"])
                                val amount = rowMap["amount"]?.toDoubleOrNull() ?: 0.0
                                val attendanceCount = rowMap["attendanceCount"]?.toIntOrNull() ?: 0
                                val dailyRate = rowMap["dailyRate"]?.toDoubleOrNull() ?: 0.0
                                val description = rowMap["description"]?.takeIf { it != "\\N" }
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])

                                bmpPayrollList.add(
                                    BmpPayrollEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        employeeId = employeeId,
                                        paymentDate = paymentDate,
                                        amount = amount,
                                        attendanceCount = attendanceCount,
                                        dailyRate = dailyRate,
                                        description = description,
                                        createdAt = createdAt
                                    )
                                )
                            }
                            "BmpBahanNono" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val tanggal = parseSqlTimestamp(rowMap["tanggal"])
                                val nominal = rowMap["nominal"]?.toDoubleOrNull() ?: 0.0
                                val notes = rowMap["notes"]?.takeIf { it != "\\N" }
                                val tagihan = rowMap["tagihan"] ?: ""
                                val totalHarga = rowMap["totalHarga"]?.toDoubleOrNull() ?: 0.0
                                val createdAt = parseSqlTimestamp(rowMap["createdAt"])

                                bmpBahanBakuList.add(
                                    BmpBahanBakuEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        tanggal = tanggal,
                                        noTagihan = tagihan,
                                        totalHarga = totalHarga,
                                        nominal = nominal,
                                        notes = notes,
                                        notaFotoPath = null,
                                        notaFotoUrl = null,
                                        isSynced = false,
                                        createdAt = createdAt,
                                        updatedAt = createdAt
                                    )
                                )
                            }
                            "BmpBahanNonoItem" -> {
                                val id = rowMap["id"]?.toLongOrNull() ?: continue
                                val bahanNonoId = rowMap["bahanNonoId"]?.toLongOrNull() ?: continue
                                val jenisBahan = rowMap["jenisBahan"] ?: ""
                                val kuantitas = rowMap["kuantitas"]?.toDoubleOrNull() ?: 0.0
                                val unit = rowMap["unit"] ?: "Kg"
                                val rate = rowMap["rate"]?.toDoubleOrNull() ?: 0.0

                                bmpBahanBakuItemList.add(
                                    BmpBahanBakuItemEntity(
                                        id = id,
                                        tenantId = tenantId,
                                        bahanBakuId = bahanNonoId,
                                        jenisBahan = jenisBahan,
                                        kuantitas = kuantitas,
                                        unit = unit,
                                        rate = rate,
                                        isSynced = false,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Commit seeded data to Room DB using block insert/upsert
        if (productList.isNotEmpty()) {
            productDao.clearTenantProducts(tenantId)
            productDao.insertAll(productList)
        }
        if (customerList.isNotEmpty()) {
            customerDao.insertAll(customerList)
        }
        if (employeeList.isNotEmpty()) {
            for (emp in employeeList) {
                employeeDao.insert(emp)
            }
        }
        if (transactionList.isNotEmpty()) {
            for (tx in transactionList) {
                try {
                    db.transactionDao().insert(tx)
                } catch (e: Exception) {
                    // REPLACE strategy handles duplicates; exception means a different error
                }
                // Always forcefully correct the date — guarantees stale System.currentTimeMillis()
                // dates from the original seeding bug are overwritten with the correct SQL dates.
                try {
                    db.transactionDao().updateDateByReceiptNumber(tx.receiptNumber, tx.date)
                } catch (e: Exception) {
                    // Ignore — best-effort correction
                }
            }
        }
        if (transactionItemList.isNotEmpty()) {
            db.transactionItemDao().insertAll(transactionItemList)
        }

        // Commit BMP seeded data
        if (bmpClientList.isNotEmpty()) {
            for (item in bmpClientList) {
                db.bmpClientDao().upsert(item)
            }
        }
        if (bmpInvoiceList.isNotEmpty()) {
            // Compute totalAmount and paidAmount dynamically from products and payments
            val updatedInvoiceList = bmpInvoiceList.map { invoice ->
                val total = bmpProductList.filter { it.invoiceId == invoice.id }.sumOf { it.price * it.quantity * it.jumlahLusin }
                val paid = bmpInvoicePaymentList.filter { it.invoiceId == invoice.id }.sumOf { it.paymentAmount }
                invoice.copy(totalAmount = total, paidAmount = paid)
            }
            for (item in updatedInvoiceList) {
                db.bmpInvoiceDao().insert(item)
            }
        }
        if (bmpProductList.isNotEmpty()) {
            db.bmpProductDao().insertAll(bmpProductList)
        }
        if (bmpMasterProductList.isNotEmpty()) {
            for (item in bmpMasterProductList) {
                db.bmpMasterProductDao().upsert(item)
            }
        }
        if (bmpInvoicePaymentList.isNotEmpty()) {
            for (item in bmpInvoicePaymentList) {
                db.bmpPaymentDao().insert(item)
            }
        }
        if (bmpCashFlowList.isNotEmpty()) {
            for (item in bmpCashFlowList) {
                db.bmpCashFlowDao().insert(item)
            }
        }
        if (bmpSettingsList.isNotEmpty()) {
            for (item in bmpSettingsList) {
                db.bmpSettingsDao().upsert(item)
            }
        }
        if (bmpEmployeeList.isNotEmpty()) {
            for (item in bmpEmployeeList) {
                db.bmpEmployeeDao().upsert(item)
            }
        }
        if (bmpPayrollList.isNotEmpty()) {
            for (item in bmpPayrollList) {
                db.bmpPayrollDao().insert(item)
            }
        }
        if (bmpBahanBakuList.isNotEmpty()) {
            for (item in bmpBahanBakuList) {
                db.bmpBahanBakuDao().insert(item)
            }
        }
        if (bmpBahanBakuItemList.isNotEmpty()) {
            db.bmpBahanBakuItemDao().insertAll(bmpBahanBakuItemList)
        }
        }

        if (tenantId.startsWith("demo_tenant_") || tenantId == "demo_tenant") {
            val isBmp = tenantId.endsWith("_BMP") || tenant?.businessMode == "BMP"
            if (isBmp) {
                // 1. Generate PT. Globalindo as client
                val clientId = 888888L
                val bmpClient = BmpClientEntity(
                    id = clientId,
                    tenantId = tenantId,
                    outletId = outletId,
                    clientName = "PT. Globalindo Manufaktur (Simulasi Demo)",
                    saldoTitipan = 0.0,
                    addressLine1 = "Kawasan Industri MM2100, Blok C-10, Cikarang",
                    province = "Jawa Barat",
                    postalCode = "17530",
                    phoneNumber = "628119876543",
                    emailAddress = "info@globalindo.co.id",
                    taxNumber = "01.234.567.8-012.000",
                    uniqueID = "client-globalindo",
                    slug = "pt-globalindo-manufaktur-simulasi-demo",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                db.bmpClientDao().upsert(bmpClient)

                // 2. Generate BMP Employees (Karyawan Kerja)
                val emp1 = BmpEmployeeEntity(
                    id = 888881L,
                    tenantId = tenantId,
                    name = "Budi Santoso (Operator)",
                    position = "OPERATOR MESIN 1",
                    salaryAmount = 4500000.0,
                    isActive = true,
                    fingerprintPIN = "1122",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val emp2 = BmpEmployeeEntity(
                    id = 888882L,
                    tenantId = tenantId,
                    name = "Siti Aminah (QC)",
                    position = "QUALITY CONTROL",
                    salaryAmount = 4800000.0,
                    isActive = true,
                    fingerprintPIN = "3344",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                db.bmpEmployeeDao().upsert(emp1)
                db.bmpEmployeeDao().upsert(emp2)

                // 3. Generate Invoice of 1 Billion Rupiah (1.000.000.000)
                val invoiceId = 888888L
                val bmpInvoice = BmpInvoiceEntity(
                    id = invoiceId,
                    tenantId = tenantId,
                    outletId = outletId,
                    clientId = clientId,
                    title = "Faktur Penjualan Botol Plastik PET Premium",
                    number = "INV/2026/06/001",
                    dueDate = System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000L,
                    paymentTerms = "14 days",
                    status = "PAID",
                    notes = "Pesanan masal botol plastik untuk kebutuhan produksi teh kemasan.",
                    totalAmount = 1000000000.0,
                    paidAmount = 1000000000.0,
                    uniqueID = "invoice-globalindo-1b",
                    slug = "inv-2026-06-001",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                db.bmpInvoiceDao().insert(bmpInvoice)

                // 4. Products under the 1 Billion invoice
                val prod1 = BmpProductEntity(
                    id = 888881L,
                    tenantId = tenantId,
                    invoiceId = invoiceId,
                    masterItemID = 1L,
                    title = "Botol Plastik PET 500ml (Premium)",
                    unit = "pcs",
                    price = 1000.0,
                    jumlahLusin = 1.0,
                    quantity = 500000.0,
                    isKhusus = false,
                    hargaBeli = 600.0,
                    currency = "Rp",
                    uniqueID = "inv-prod-500ml",
                    slug = "botol-plastik-pet-500ml-premium",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val prod2 = BmpProductEntity(
                    id = 888882L,
                    tenantId = tenantId,
                    invoiceId = invoiceId,
                    masterItemID = 2L,
                    title = "Botol Plastik PET 250ml (Premium)",
                    unit = "pcs",
                    price = 1000.0,
                    jumlahLusin = 1.0,
                    quantity = 500000.0,
                    isKhusus = false,
                    hargaBeli = 650.0,
                    currency = "Rp",
                    uniqueID = "inv-prod-250ml",
                    slug = "botol-plastik-pet-250ml-premium",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                db.bmpProductDao().insertAll(listOf(prod1, prod2))

                // 5. Payment record of 1 Billion Rupiah
                val paymentId = 888888L
                val bmpPayment = BmpInvoicePaymentEntity(
                    id = paymentId,
                    tenantId = tenantId,
                    invoiceId = invoiceId,
                    paymentDate = System.currentTimeMillis(),
                    paymentAmount = 1000000000.0,
                    paymentMethod = "TRANSFER",
                    notes = "Pelunasan via transfer Bank Mandiri",
                    createdAt = System.currentTimeMillis()
                )
                db.bmpPaymentDao().insert(bmpPayment)

                // 6. Cash flow entry of 1 Billion Rupiah
                val cashflowId = 888888L
                val bmpCashflow = BmpCashFlowEntity(
                    id = cashflowId,
                    tenantId = tenantId,
                    transactionDate = System.currentTimeMillis(),
                    transactionType = "MASUK",
                    description = "Pelunasan Invoice INV/2026/06/001 - PT. Globalindo",
                    amount = 1000000000.0,
                    paymentRefId = paymentId,
                    createdAt = System.currentTimeMillis()
                )
                db.bmpCashFlowDao().insert(bmpCashflow)

                // 7. Generate default BMP Settings
                val bmpSettings = BmpSettingsEntity(
                    id = 888888L,
                    tenantId = tenantId,
                    clientName = "Pabrik Plastik Mandiri Jaya (Demo)",
                    clientLogo = null,
                    addressLine1 = "Kawasan Industri Driyorejo, Gresik",
                    province = "Jawa Timur",
                    postalCode = "61177",
                    phoneNumber = "6281234567890",
                    emailAddress = "contact@mandirijaya.co.id",
                    taxNumber = "01.999.888.7-011.000",
                    listrikBulanan = 35000000.0,
                    jumlahMesin = 6,
                    jumlahKaryawan = 25,
                    gajiHarian = 90000.0,
                    hariKerjaSebulan = 26,
                    biayaKarungPer1000 = 2200000.0,
                    hoursPerDay = 24,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                db.bmpSettingsDao().upsert(bmpSettings)

                // Generate default Print Settings for BMP
                val printSettings = PrintSettingsEntity(
                    tenantId = tenantId,
                    moduleKey = "BMP",
                    logoPath = null
                )
                db.printSettingsDao().upsert(printSettings)

                // 8. Generate default BMP Master Products
                val masterProds = listOf(
                    BmpMasterProductEntity(
                        id = 1L,
                        tenantId = tenantId,
                        title = "Botol Plastik PET 500ml (Premium)",
                        description = "Botol plastik tebal untuk minuman teh/kopi premium",
                        unit = "pcs",
                        price = 1000.0,
                        beratGram = 24.5,
                        cycleTime = 12.0,
                        cavity = 4,
                        rejectRate = 0.02,
                        uniqueID = "master-pet-500",
                        slug = "botol-plastik-pet-500ml-premium",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    BmpMasterProductEntity(
                        id = 2L,
                        tenantId = tenantId,
                        title = "Botol Plastik PET 250ml (Premium)",
                        description = "Botol plastik kecil untuk susu/jus premium",
                        unit = "pcs",
                        price = 1000.0,
                        beratGram = 18.0,
                        cycleTime = 10.0,
                        cavity = 4,
                        rejectRate = 0.015,
                        uniqueID = "master-pet-250",
                        slug = "botol-plastik-pet-250ml-premium",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                for (item in masterProds) {
                    db.bmpMasterProductDao().upsert(item)
                }

                // 9. Generate default BmpBahanBaku and BmpBahanBakuItem
                val bahanBakuId = 888888L
                val bahanBaku = BmpBahanBakuEntity(
                    id = bahanBakuId,
                    tenantId = tenantId,
                    tanggal = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L,
                    noTagihan = "BILL-SUPPLIER-001",
                    totalHarga = 45000000.0,
                    nominal = 45000000.0,
                    notes = "Pengiriman bijih plastik PET premium dari PT. Petrochem",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                db.bmpBahanBakuDao().insert(bahanBaku)

                val bbItems = listOf(
                    BmpBahanBakuItemEntity(
                        id = 888881L,
                        tenantId = tenantId,
                        bahanBakuId = bahanBakuId,
                        jenisBahan = "PET",
                        kuantitas = 3000.0,
                        unit = "Kg",
                        rate = 15000.0,
                        createdAt = System.currentTimeMillis()
                    )
                )
                db.bmpBahanBakuItemDao().insertAll(bbItems)

            } else {
                // POS Mode (FNB / RENTAL / LAUNDRY) -> Generate transactions > 30M
                val mode = tenant?.businessMode ?: when {
                    tenantId.endsWith("_BMP") -> "BMP"
                    tenantId.endsWith("_RENTAL") -> "RENTAL"
                    tenantId.endsWith("_LAUNDRY") -> "LAUNDRY"
                    else -> "FNB"
                }

                if (mode == "FNB") {
                    // Seed PrintSettings for FNB
                    val printSettings = PrintSettingsEntity(
                        tenantId = tenantId,
                        moduleKey = "FNB",
                        logoPath = null
                    )
                    db.printSettingsDao().upsert(printSettings)

                    // 1. Outlet Employees (Kasir)
                    val cashier1 = Employee(
                        id = 888881L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Lani Kasir (FNB)",
                        email = "lani@kasir.com",
                        role = "KASIR",
                        pinHash = "123456",
                        salary = 3000000.0
                    )
                    val waiter = Employee(
                        id = 888882L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Budi Resto (Waiter)",
                        email = "budi@resto.com",
                        role = "KASIR",
                        pinHash = "123456",
                        salary = 2500000.0
                    )
                    db.employeeDao().insert(cashier1)
                    db.employeeDao().insert(waiter)

                    // 2. Customers
                    val cust1 = CustomerEntity(
                        id = 888881L,
                        tenantId = tenantId,
                        name = "Catering Bu Endang (Demo)",
                        phone = "08776543210",
                        address = "Kec. Waru, Sidoarjo"
                    )
                    val cust2 = CustomerEntity(
                        id = 888882L,
                        tenantId = tenantId,
                        name = "Pelanggan Setia (Demo)",
                        phone = "08122334455",
                        address = "Kec. Gedangan, Sidoarjo"
                    )
                    db.customerDao().insertAll(listOf(cust1, cust2))

                    // 3. Products
                    val p1 = ProductEntity(
                        id = 888881L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Paket Catering Bento Box Premium",
                        price = 150000.0,
                        costPrice = 80000.0,
                        stock = 500,
                        unit = "pax",
                        barcode = "FNB-001",
                        category = "CATERING"
                    )
                    val p2 = ProductEntity(
                        id = 888884L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Nasi Goreng Spesial Posbah",
                        price = 25000.0,
                        costPrice = 12000.0,
                        stock = 1000,
                        unit = "porsi",
                        barcode = "FNB-002",
                        category = "MAKANAN"
                    )
                    val p3 = ProductEntity(
                        id = 888885L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Es Teh Manis Jumbo",
                        price = 5000.0,
                        costPrice = 1500.0,
                        stock = 2000,
                        unit = "gelas",
                        barcode = "FNB-003",
                        category = "MINUMAN"
                    )
                    val p4 = ProductEntity(
                        id = 888886L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Ayam Bakar Madu",
                        price = 35000.0,
                        costPrice = 18000.0,
                        stock = 300,
                        unit = "porsi",
                        barcode = "FNB-004",
                        category = "MAKANAN"
                    )
                    db.productDao().insertAll(listOf(p1, p2, p3, p4))

                    // 4. Transactions (Total > 30M)
                    val startMs = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L
                    val newTxs = mutableListOf<TransactionEntity>()
                    val newTxItems = mutableListOf<TransactionItemEntity>()
                    var txIndex = 888881L
                    var txItemIndex = 888881L

                    // 15 Catering sales (15 * 1.5M = 22.5M)
                    for (i in 0 until 15) {
                        val date = startMs + i * 6 * 60 * 60 * 1000L
                        val subtotal = 150000.0 * 10
                        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(java.util.Date(date))
                        val receiptNum = "FNB-$dateStr-${String.format("%05d", i + 1)}"

                        newTxs.add(
                            TransactionEntity(
                                id = txIndex,
                                tenantId = tenantId,
                                outletId = outletId,
                                employeeId = 888881L,
                                customerId = 888881L,
                                customerName = "Catering Bu Endang (Demo)",
                                receiptNumber = receiptNum,
                                date = date,
                                subtotal = subtotal,
                                total = subtotal,
                                paymentMethod = "TRANSFER",
                                type = "SALES",
                                status = "COMPLETED",
                                createdAt = date,
                                updatedAt = date
                            )
                        )
                        newTxItems.add(
                            TransactionItemEntity(
                                id = txItemIndex++,
                                transactionId = txIndex,
                                productId = 888881L,
                                quantity = 10,
                                price = 150000.0,
                                costPrice = 80000.0
                            )
                        )
                        txIndex++
                    }

                    // 40 Misc Restaurant sales (40 * 300k = 12.0M)
                    for (i in 0 until 40) {
                        val date = startMs + i * 2 * 60 * 60 * 1000L + 30 * 60 * 1000L
                        val subtotal = 300000.0
                        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(java.util.Date(date))
                        val receiptNum = "FNB-$dateStr-${String.format("%05d", i + 16)}"

                        newTxs.add(
                            TransactionEntity(
                                id = txIndex,
                                tenantId = tenantId,
                                outletId = outletId,
                                employeeId = 888881L,
                                customerId = 888882L,
                                customerName = "Pelanggan Setia (Demo)",
                                receiptNumber = receiptNum,
                                date = date,
                                subtotal = subtotal,
                                total = subtotal,
                                paymentMethod = "CASH",
                                type = "SALES",
                                status = "COMPLETED",
                                createdAt = date,
                                updatedAt = date
                            )
                        )
                        newTxItems.add(
                            TransactionItemEntity(
                                id = txItemIndex++,
                                transactionId = txIndex,
                                productId = 888884L,
                                quantity = 5,
                                price = 25000.0,
                                costPrice = 12000.0
                            )
                        )
                        newTxItems.add(
                            TransactionItemEntity(
                                id = txItemIndex++,
                                transactionId = txIndex,
                                productId = 888886L,
                                quantity = 5,
                                price = 35000.0,
                                costPrice = 18000.0
                            )
                        )
                        txIndex++
                    }

                    for (tx in newTxs) {
                        try {
                            val txId = db.transactionDao().insert(tx)
                            val itemsForTx = newTxItems.filter { it.transactionId == tx.id }
                            db.transactionItemDao().insertAll(itemsForTx.map { it.copy(transactionId = txId) })
                        } catch (e: Exception) {}
                    }
                } else if (mode == "RENTAL") {
                    // Seed PrintSettings for RENTAL
                    val printSettings = PrintSettingsEntity(
                        tenantId = tenantId,
                        moduleKey = "RENTAL",
                        logoPath = null
                    )
                    db.printSettingsDao().upsert(printSettings)

                    // 1. Outlet Employees
                    val cashier = Employee(
                        id = 888881L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Rian Kasir (ARMADA)",
                        email = "rian@kasir.com",
                        role = "KASIR",
                        pinHash = "123456",
                        salary = 3200000.0
                    )
                    val driver = Employee(
                        id = 888882L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Dedi Supir (Driver)",
                        email = "dedi@driver.com",
                        role = "KASIR",
                        pinHash = "123456",
                        salary = 2800000.0
                    )
                    db.employeeDao().insert(cashier)
                    db.employeeDao().insert(driver)

                    // 2. Customers
                    val cust1 = CustomerEntity(
                        id = 888881L,
                        tenantId = tenantId,
                        name = "Hotel Santika (Demo)",
                        phone = "08123456789",
                        address = "Jl. Raya Sidoarjo No. 10"
                    )
                    val cust2 = CustomerEntity(
                        id = 888882L,
                        tenantId = tenantId,
                        name = "Pribadi Eka (Demo)",
                        phone = "08779988776",
                        address = "Kec. Buduran, Sidoarjo"
                    )
                    db.customerDao().insertAll(listOf(cust1, cust2))

                    // 3. Products (Vehicles) - Some available, some rented
                    val v1 = ProductEntity(
                        id = 888882L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Sewa Toyota Alphard + Sopir (24 Jam)",
                        price = 2500000.0,
                        costPrice = 1200000.0,
                        stock = 0, // 0 = Rented out
                        unit = "hari",
                        barcode = "RNT-001",
                        category = "MOBIL",
                        variants = """{"renterName":"Hotel Santika (Demo)","expiry":${System.currentTimeMillis() + 12 * 60 * 60 * 1000L},"days":2,"receiptNumber":"RN-260615-00001"}"""
                    )
                    val v2 = ProductEntity(
                        id = 888887L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Toyota Avanza Veloz",
                        price = 350000.0,
                        costPrice = 150000.0,
                        stock = 1, // 1 = Available
                        unit = "hari",
                        barcode = "B 2038 UFX",
                        category = "MOBIL"
                    )
                    val v3 = ProductEntity(
                        id = 888888L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Honda Beat Street",
                        price = 80000.0,
                        costPrice = 30000.0,
                        stock = 1, // 1 = Available
                        unit = "hari",
                        barcode = "F 6432 AA",
                        category = "MOTOR"
                    )
                    val v4 = ProductEntity(
                        id = 888889L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Mitsubishi Xpander Ultimate",
                        price = 400000.0,
                        costPrice = 180000.0,
                        stock = 0, // 0 = Rented out
                        unit = "hari",
                        barcode = "B 1093 KLS",
                        category = "MOBIL",
                        variants = """{"renterName":"Pribadi Eka (Demo)","expiry":${System.currentTimeMillis() + 36 * 60 * 60 * 1000L},"days":3,"receiptNumber":"RN-260615-00002"}"""
                    )
                    db.productDao().insertAll(listOf(v1, v2, v3, v4))

                    // 4. Transactions (Total > 30M)
                    val startMs = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L
                    val newTxs = mutableListOf<TransactionEntity>()
                    val newTxItems = mutableListOf<TransactionItemEntity>()
                    var txIndex = 888881L
                    var txItemIndex = 888881L

                    // 10 Sewa Alphard transactions (10 * 2.5M = 25.0M)
                    for (i in 0 until 10) {
                        val date = startMs + i * 10 * 60 * 60 * 1000L
                        val subtotal = 2500000.0
                        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(java.util.Date(date))
                        val receiptNum = "RN-$dateStr-${String.format("%05d", i + 1)}"

                        newTxs.add(
                            TransactionEntity(
                                id = txIndex,
                                tenantId = tenantId,
                                outletId = outletId,
                                employeeId = 888881L,
                                customerId = 888882L, // vehicle product ID
                                customerName = "Hotel Santika (Demo)",
                                receiptNumber = receiptNum,
                                date = date,
                                subtotal = subtotal,
                                total = subtotal,
                                paymentMethod = "CASH",
                                status = "COMPLETED",
                                orderStatus = "RETURNED",
                                queueNumber = 1,
                                notes = "08123456789",
                                createdAt = date,
                                updatedAt = date
                            )
                        )
                        newTxItems.add(
                            TransactionItemEntity(
                                id = txItemIndex++,
                                transactionId = txIndex,
                                productId = 888882L,
                                quantity = 1,
                                price = 2500000.0,
                                costPrice = 1200000.0
                            )
                        )
                        txIndex++
                    }

                    // 20 Sewa Avanza transactions (20 * 2 days * 350k = 14.0M)
                    for (i in 0 until 20) {
                        val date = startMs + i * 5 * 60 * 60 * 1000L + 2 * 60 * 60 * 1000L
                        val subtotal = 700000.0
                        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(java.util.Date(date))
                        val receiptNum = "RN-$dateStr-${String.format("%05d", i + 11)}"

                        newTxs.add(
                            TransactionEntity(
                                id = txIndex,
                                tenantId = tenantId,
                                outletId = outletId,
                                employeeId = 888881L,
                                customerId = 888887L,
                                customerName = "Pribadi Eka (Demo)",
                                receiptNumber = receiptNum,
                                date = date,
                                subtotal = subtotal,
                                total = subtotal,
                                paymentMethod = "CASH",
                                status = "COMPLETED",
                                orderStatus = "RETURNED",
                                queueNumber = 2,
                                notes = "08779988776",
                                createdAt = date,
                                updatedAt = date
                            )
                        )
                        newTxItems.add(
                            TransactionItemEntity(
                                id = txItemIndex++,
                                transactionId = txIndex,
                                productId = 888887L,
                                quantity = 1,
                                price = 350000.0,
                                costPrice = 150000.0
                            )
                        )
                        txIndex++
                    }

                    for (tx in newTxs) {
                        try {
                            val txId = db.transactionDao().insert(tx)
                            val itemsForTx = newTxItems.filter { it.transactionId == tx.id }
                            db.transactionItemDao().insertAll(itemsForTx.map { it.copy(transactionId = txId) })
                        } catch (e: Exception) {}
                    }
                } else if (mode == "LAUNDRY") {
                    // Seed PrintSettings for LAUNDRY
                    val printSettings = PrintSettingsEntity(
                        tenantId = tenantId,
                        moduleKey = "LAUNDRY",
                        logoPath = null
                    )
                    db.printSettingsDao().upsert(printSettings)

                    // 1. Outlet Employees
                    val cashier = Employee(
                        id = 888881L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Lina Kasir (LAUNDRY)",
                        email = "lina@kasir.com",
                        role = "KASIR",
                        pinHash = "123456",
                        salary = 3000000.0
                    )
                    val washer = Employee(
                        id = 888882L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Agus Cuci (Operator)",
                        email = "agus@laundry.com",
                        role = "KASIR",
                        pinHash = "123456",
                        salary = 2500000.0
                    )
                    db.employeeDao().insert(cashier)
                    db.employeeDao().insert(washer)

                    // 2. Customers
                    val cust1 = CustomerEntity(
                        id = 888881L,
                        tenantId = tenantId,
                        name = "Hotel Santika (Demo)",
                        phone = "08123456789",
                        address = "Jl. Raya Sidoarjo No. 10"
                    )
                    val cust2 = CustomerEntity(
                        id = 888882L,
                        tenantId = tenantId,
                        name = "Kost Asri (Demo)",
                        phone = "08199887766",
                        address = "Kec. Gedangan, Sidoarjo"
                    )
                    db.customerDao().insertAll(listOf(cust1, cust2))

                    // 3. Products
                    val s1 = ProductEntity(
                        id = 888883L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Paket Cuci Setrika Bedcover Hotel",
                        price = 50000.0,
                        costPrice = 20000.0,
                        stock = 1000,
                        unit = "pcs",
                        barcode = "LND-001",
                        category = "SATUAN"
                    )
                    val s2 = ProductEntity(
                        id = 888890L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Cuci Kiloan Reguler (Cuci + Gosok)",
                        price = 6000.0,
                        costPrice = 2000.0,
                        stock = 9999,
                        unit = "Kg",
                        barcode = "LND-002",
                        category = "KILOAN"
                    )
                    val s3 = ProductEntity(
                        id = 888891L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Cuci Kiloan Kilat (6 Jam Selesai)",
                        price = 12000.0,
                        costPrice = 4000.0,
                        stock = 9999,
                        unit = "Kg",
                        barcode = "LND-003",
                        category = "KILOAN"
                    )
                    val s4 = ProductEntity(
                        id = 888892L,
                        tenantId = tenantId,
                        outletId = outletId,
                        name = "Gosok Setrika Saja",
                        price = 4500.0,
                        costPrice = 1500.0,
                        stock = 9999,
                        unit = "Kg",
                        barcode = "LND-004",
                        category = "KILOAN"
                    )
                    db.productDao().insertAll(listOf(s1, s2, s3, s4))

                    // 4. Transactions (Total > 30M)
                    val startMs = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L
                    val newTxs = mutableListOf<TransactionEntity>()
                    val newTxItems = mutableListOf<TransactionItemEntity>()
                    var txIndex = 888881L
                    var txItemIndex = 888881L

                    // 12 Bulk bedcover washes (12 * 50 pcs * 50k = 30.0M)
                    for (i in 0 until 12) {
                        val date = startMs + i * 8 * 60 * 60 * 1000L
                        val subtotal = 2500000.0
                        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(java.util.Date(date))
                        val receiptNum = "LD-$dateStr-${String.format("%05d", i + 1)}"

                        val meta = """{"phone":"08123456789","summary":"Paket Cuci Setrika Bedcover Hotel x50 Pcs"}"""
                        newTxs.add(
                            TransactionEntity(
                                id = txIndex,
                                tenantId = tenantId,
                                outletId = outletId,
                                employeeId = 888881L,
                                customerName = "Hotel Santika (Demo)",
                                receiptNumber = receiptNum,
                                date = date,
                                subtotal = subtotal,
                                total = subtotal,
                                paymentMethod = "CASH",
                                status = "COMPLETED",
                                orderStatus = "SELESAI",
                                notes = meta,
                                createdAt = date,
                                updatedAt = date
                            )
                        )
                        newTxItems.add(
                            TransactionItemEntity(
                                id = txItemIndex++,
                                transactionId = txIndex,
                                productId = 888883L,
                                quantity = 50,
                                price = 50000.0,
                                costPrice = 20000.0
                            )
                        )
                        txIndex++
                    }

                    // 30 Kilat laundry orders (30 * 15 Kg * 12k = 5.4M)
                    for (i in 0 until 30) {
                        val date = startMs + i * 3 * 60 * 60 * 1000L + i * 15 * 60 * 1000L
                        val subtotal = 180000.0
                        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(java.util.Date(date))
                        val receiptNum = "LD-$dateStr-${String.format("%05d", i + 13)}"

                        val meta = """{"phone":"08199887766","summary":"Cuci Kiloan Kilat (6 Jam Selesai) x15.0 Kg"}"""
                        newTxs.add(
                            TransactionEntity(
                                id = txIndex,
                                tenantId = tenantId,
                                outletId = outletId,
                                employeeId = 888881L,
                                customerName = "Kost Asri (Demo)",
                                receiptNumber = receiptNum,
                                date = date,
                                subtotal = subtotal,
                                total = subtotal,
                                paymentMethod = "CASH",
                                status = "COMPLETED",
                                orderStatus = "SELESAI",
                                notes = meta,
                                createdAt = date,
                                updatedAt = date
                            )
                        )
                        // Kiloan quantity is stored multiplied by 10 (15.0 Kg -> 150)
                        // Price is divided by 10 (12000 -> 1200)
                        newTxItems.add(
                            TransactionItemEntity(
                                id = txItemIndex++,
                                transactionId = txIndex,
                                productId = 888891L,
                                quantity = 150,
                                price = 1200.0,
                                costPrice = 4000.0
                            )
                        )
                        txIndex++
                    }

                    for (tx in newTxs) {
                        try {
                            val txId = db.transactionDao().insert(tx)
                            val itemsForTx = newTxItems.filter { it.transactionId == tx.id }
                            db.transactionItemDao().insertAll(itemsForTx.map { it.copy(transactionId = txId) })
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        // Auto-seed static premium employees for hanafiariful's tenant
        if (tenantId == "ten_premium_hanafiariful_gmail_com" || tenantId == "hanafiariful@gmail.com") {
            val staticEmployees = listOf(
                Employee(
                    id = 10002L,
                    tenantId = "ten_premium_hanafiariful_gmail_com",
                    outletId = outletId,
                    name = "FahriP",
                    email = "fahrup22@gmail.com",
                    role = "ADMIN",
                    pinHash = "63e71711d1481b6da8b756e114aa2ac71a704929c0accf46f419706a5c1416ae1a312899ae84d3d8e33d255811e98fd4d17e59371a08e2f9c21c01d1b1c13a8d",
                    salary = 3000000.0,
                    isActive = true,
                    payPeriod = "MONTHLY",
                    emailVerified = true
                ),
                Employee(
                    id = 10003L,
                    tenantId = "ten_premium_hanafiariful_gmail_com",
                    outletId = outletId,
                    name = "Mamet PKR",
                    email = "alfarisirosi40@gmail.com",
                    role = "KASIR",
                    pinHash = "a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c",
                    salary = 2500000.0,
                    isActive = true,
                    payPeriod = "MONTHLY",
                    emailVerified = true
                )
            )
            for (emp in staticEmployees) {
                try {
                    db.employeeDao().insert(emp)
                } catch (e: java.lang.Exception) {
                    android.util.Log.e("LocalDataSeeder", "Error inserting static employee ${emp.email}", e)
                }
            }
        }

        } catch (e: Exception) {
            android.util.Log.e("LocalDataSeeder", "Error during seedFromSqlDump execution", e)
        }
    }

    suspend fun seedDefaultVehicles(tenantId: String, outletId: Long?) = withContext(Dispatchers.IO) {
        val list = listOf(
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Toyota Avanza Veloz", price = 350000.0, stock = 1, unit = "hari", barcode = "B 2038 UFX", category = "MOBIL"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Honda Beat Street", price = 80000.0, stock = 1, unit = "hari", barcode = "F 6432 AA", category = "MOTOR"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Mitsubishi Xpander Ultimate", price = 400000.0, stock = 1, unit = "hari", barcode = "B 1093 KLS", category = "MOBIL"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Yamaha NMAX 155", price = 120000.0, stock = 1, unit = "hari", barcode = "B 4912 CDE", category = "MOTOR"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Toyota Innova Zenix", price = 650000.0, stock = 1, unit = "hari", barcode = "D 1289 VCB", category = "MOBIL")
        )
        productDao.insertAll(list)
    }

    suspend fun seedDefaultLaundryServices(tenantId: String, outletId: Long?) = withContext(Dispatchers.IO) {
        val list = listOf(
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Cuci Kiloan Reguler (Cuci + Gosok)", price = 6000.0, stock = 9999, unit = "Kg", barcode = "LD-SRV-1", category = "KILOAN"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Cuci Kiloan Kilat (6 Jam Selesai)", price = 12000.0, stock = 9999, unit = "Kg", barcode = "LD-SRV-2", category = "KILOAN"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Cuci Kering Saja", price = 4000.0, stock = 9999, unit = "Kg", barcode = "LD-SRV-3", category = "KILOAN"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Gosok Setrika Saja", price = 4500.0, stock = 9999, unit = "Kg", barcode = "LD-SRV-4", category = "KILOAN"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Cuci Sprei / Bed Cover Besar", price = 20000.0, stock = 9999, unit = "Pcs", barcode = "LD-SRV-5", category = "SATUAN"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Cuci Selimut / Bed Cover Kecil", price = 15000.0, stock = 9999, unit = "Pcs", barcode = "LD-SRV-6", category = "SATUAN"),
            ProductEntity(tenantId = tenantId, outletId = outletId, name = "Cuci Jaket Tebal / Leather", price = 25000.0, stock = 9999, unit = "Pcs", barcode = "LD-SRV-7", category = "SATUAN")
        )
        productDao.insertAll(list)
    }

    suspend fun seedDefaultSettings(tenantId: String) = withContext(Dispatchers.IO) {
        if (tenantId.isBlank()) return@withContext
        
        // Seed BmpSettings if not exists
        val existingBmpSettings = db.bmpSettingsDao().get(tenantId)
        if (existingBmpSettings == null) {
            db.bmpSettingsDao().upsert(
                BmpSettingsEntity(
                    tenantId = tenantId,
                    clientName = "Perusahaan Saya",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Seed PrintSettings for all modules if they do not exist
        val modules = listOf("BMP", "FNB", "LAUNDRY", "RENTAL")
        for (m in modules) {
            val existingPrint = db.printSettingsDao().get(tenantId, m)
            if (existingPrint == null) {
                db.printSettingsDao().upsert(
                    PrintSettingsEntity(
                        tenantId = tenantId,
                        moduleKey = m,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
