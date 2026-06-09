package com.posbah.app.data.local

import android.content.Context
import com.posbah.app.data.local.dao.CustomerDao
import com.posbah.app.data.local.dao.EmployeeDao
import com.posbah.app.data.local.dao.ProductDao
import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.data.local.entities.BmpProductEntity
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpInvoicePaymentEntity
import com.posbah.app.data.local.entities.BmpCashFlowEntity
import com.posbah.app.data.local.entities.BmpSettingsEntity
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
        // Only seed SQL dump data for bahteramulyap@gmail.com and demo_tenant.
        // Other new premium accounts will start with a fresh/empty database space.
        if (tenantId != "bahteramulyap@gmail.com" && 
            tenantId != "demo_tenant" && 
            !tenantId.contains("hanafiariful_gmail_com") && 
            tenantId != "hanafiariful@gmail.com") {
            return@withContext
        }

        val assetManager = context.assets
        val inputStream = assetManager.open("posbah_tenant_bahteramulyap_gmail_com.sql")
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

        var currentTable: String? = null
        var columns = listOf<String>()
        val productList = mutableListOf<ProductEntity>()
        val customerList = mutableListOf<CustomerEntity>()
        val employeeList = mutableListOf<Employee>()

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

                        when (currentTable) {
                            "Product" -> {
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
                                val name = rowMap["name"]
                                if (name.isNullOrBlank() || name == "\\N") continue
                                val phone = rowMap["phone"]?.takeIf { it != "\\N" }
                                val address = rowMap["address"]?.takeIf { it != "\\N" }

                                customerList.add(
                                    CustomerEntity(
                                        tenantId = tenantId,
                                        name = name,
                                        phone = phone,
                                        address = address
                                    )
                                )
                            }
                            "Employee" -> {
                                val name = rowMap["name"]
                                if (name.isNullOrBlank() || name == "\\N") continue
                                val role = rowMap["role"]?.takeIf { it != "\\N" } ?: "KASIR"
                                val pinHash = rowMap["pin"]?.takeIf { it != "\\N" && it.isNotBlank() } ?: ""
                                val salary = rowMap["salary"]?.toDoubleOrNull() ?: 0.0
                                val email = rowMap["email"]?.takeIf { it != "\\N" }

                                employeeList.add(
                                    Employee(
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
                                val clientLogo = rowMap["clientLogo"]?.takeIf { it != "\\N" }
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
}
