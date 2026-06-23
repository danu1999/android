package com.posbah.app.data.remote

import android.content.Context
import com.posbah.app.data.local.entities.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * BmpOnlineWriter
 *
 * Helper untuk melakukan Write-Through ke VPS secara synchronous sebelum data disimpan di Room.
 * Menggunakan [SupabaseSyncManager.uploadRowWriteThrough] dan [SupabaseSyncManager.deleteRowWriteThrough].
 *
 * Semua metode suspend dan mengembalikan [SupabaseSyncManager.SyncResult].
 */
object BmpOnlineWriter {

    // ── Invoices ──────────────────────────────────────────────────────────────

    suspend fun upsertInvoice(
        context: Context,
        tenantId: String,
        invoice: BmpInvoiceEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
                put("id", invoice.id)
                put("tenantId", invoice.tenantId)
                put("outletId", invoice.outletId ?: JSONObject.NULL)
                put("clientId", invoice.clientId ?: JSONObject.NULL)
                put("title", invoice.title)
                put("number", invoice.number)
                put("dueDate", invoice.dueDate ?: JSONObject.NULL)
                put("paymentTerms", invoice.paymentTerms)
                put("status", invoice.status)
                put("notes", invoice.notes ?: JSONObject.NULL)
                put("totalAmount", invoice.totalAmount)
                put("paidAmount", invoice.paidAmount)
                put("uniqueID", invoice.uniqueID ?: JSONObject.NULL)
                put("slug", invoice.slug)
                put("isSynced", true)
                put("receiverSignaturePath", invoice.receiverSignaturePath ?: JSONObject.NULL)
                put("receiverSignatureUrl", invoice.receiverSignatureUrl ?: JSONObject.NULL)
                put("receiverNameActual", invoice.receiverNameActual ?: JSONObject.NULL)
                put("createdAt", invoice.createdAt)
                put("updatedAt", invoice.updatedAt)
            })
        }
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_invoices", array, tenantId)
    }

    suspend fun upsertProducts(
        context: Context,
        tenantId: String,
        products: List<BmpProductEntity>
    ): SupabaseSyncManager.SyncResult {
        if (products.isEmpty()) return SupabaseSyncManager.SyncResult.Success
        val array = JSONArray()
        products.forEach { p ->
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
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_products", array, tenantId)
    }

    suspend fun deleteInvoice(
        context: Context,
        tenantId: String,
        invoiceId: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_invoices", invoiceId, tenantId)

    suspend fun deleteProduct(
        context: Context,
        tenantId: String,
        productId: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_products", productId, tenantId)

    suspend fun deleteProducts(
        context: Context,
        tenantId: String,
        productIds: List<Long>
    ): SupabaseSyncManager.SyncResult {
        for (id in productIds) {
            val r = SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_products", id, tenantId)
            if (r is SupabaseSyncManager.SyncResult.Error) return r
            if (r is SupabaseSyncManager.SyncResult.NoConnection) return r
        }
        return SupabaseSyncManager.SyncResult.Success
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    suspend fun upsertPayment(
        context: Context,
        tenantId: String,
        payment: BmpInvoicePaymentEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
                put("id", payment.id)
                put("tenantId", payment.tenantId)
                put("invoiceId", payment.invoiceId)
                put("paymentDate", payment.paymentDate)
                put("paymentAmount", payment.paymentAmount)
                put("paymentMethod", payment.paymentMethod)
                put("notes", payment.notes ?: JSONObject.NULL)
                put("isSynced", true)
                put("createdAt", payment.createdAt)
            })
        }
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_invoice_payments", array, tenantId)
    }

    suspend fun deletePayment(
        context: Context,
        tenantId: String,
        paymentId: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_invoice_payments", paymentId, tenantId)

    // ── CashFlow ──────────────────────────────────────────────────────────────

    suspend fun upsertCashFlow(
        context: Context,
        tenantId: String,
        cf: BmpCashFlowEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
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
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_cashflow", array, tenantId)
    }

    suspend fun deleteCashFlow(
        context: Context,
        tenantId: String,
        cfId: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_cashflow", cfId, tenantId)

    // ── Clients ───────────────────────────────────────────────────────────────

    suspend fun upsertClient(
        context: Context,
        tenantId: String,
        client: BmpClientEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
                put("id", client.id)
                put("tenantId", client.tenantId)
                put("outletId", client.outletId ?: JSONObject.NULL)
                put("clientName", client.clientName)
                put("saldoTitipan", client.saldoTitipan)
                put("addressLine1", client.addressLine1 ?: JSONObject.NULL)
                put("clientLogo", client.clientLogo ?: JSONObject.NULL)
                put("province", client.province ?: JSONObject.NULL)
                put("postalCode", client.postalCode ?: JSONObject.NULL)
                put("phoneNumber", client.phoneNumber ?: JSONObject.NULL)
                put("emailAddress", client.emailAddress ?: JSONObject.NULL)
                put("taxNumber", client.taxNumber ?: JSONObject.NULL)
                put("uniqueID", client.uniqueID ?: JSONObject.NULL)
                put("slug", client.slug ?: JSONObject.NULL)
                put("receiverSignatureUrl", client.receiverSignatureUrl ?: JSONObject.NULL)
                put("receiverNameActual", client.receiverNameActual ?: JSONObject.NULL)
                put("isSynced", true)
                put("createdAt", client.createdAt)
                put("updatedAt", client.updatedAt)
            })
        }
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_clients", array, tenantId)
    }

    suspend fun deleteClient(
        context: Context,
        tenantId: String,
        clientId: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_clients", clientId, tenantId)

    // ── Master Products ───────────────────────────────────────────────────────

    suspend fun upsertMasterProduct(
        context: Context,
        tenantId: String,
        mp: BmpMasterProductEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
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
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_master_products", array, tenantId)
    }

    suspend fun deleteMasterProduct(
        context: Context,
        tenantId: String,
        id: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_master_products", id, tenantId)

    // ── Bahan Baku ────────────────────────────────────────────────────────────

    suspend fun upsertBahanBaku(
        context: Context,
        tenantId: String,
        bb: com.posbah.app.data.local.entities.BmpBahanBakuEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
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
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_bahan_baku", array, tenantId)
    }

    suspend fun upsertBahanBakuItems(
        context: Context,
        tenantId: String,
        items: List<com.posbah.app.data.local.entities.BmpBahanBakuItemEntity>
    ): SupabaseSyncManager.SyncResult {
        if (items.isEmpty()) return SupabaseSyncManager.SyncResult.Success
        val array = JSONArray()
        items.forEach { bbi ->
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
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_bahan_baku_item", array, tenantId)
    }

    suspend fun deleteBahanBaku(
        context: Context,
        tenantId: String,
        id: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_bahan_baku", id, tenantId)

    suspend fun deleteBahanBakuItem(
        context: Context,
        tenantId: String,
        id: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_bahan_baku_item", id, tenantId)

    // ── Production Logs ───────────────────────────────────────────────────────

    suspend fun upsertProductionLog(
        context: Context,
        tenantId: String,
        log: BmpProductionLogEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
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
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_production_logs", array, tenantId)
    }

    suspend fun deleteProductionLog(
        context: Context,
        tenantId: String,
        id: Long
    ): SupabaseSyncManager.SyncResult =
        SupabaseSyncManager.deleteRowWriteThrough(context, "bmp_production_logs", id, tenantId)

    // ── Product Stocks ────────────────────────────────────────────────────────

    suspend fun upsertProductStock(
        context: Context,
        tenantId: String,
        stock: BmpProductStockEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
                put("id", stock.id)
                put("tenantId", stock.tenantId)
                put("outletId", stock.outletId ?: JSONObject.NULL)
                put("masterProductId", stock.masterProductId)
                put("quantity", stock.quantity)
                put("minStockAlert", stock.minStockAlert)
                put("isSynced", true)
                put("updatedAt", stock.updatedAt)
            })
        }
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_product_stocks", array, tenantId)
    }

    suspend fun upsertStockLedger(
        context: Context,
        tenantId: String,
        ledger: BmpStockLedgerEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
                put("id", ledger.id)
                put("tenantId", ledger.tenantId)
                put("masterProductId", ledger.masterProductId)
                put("referenceId", ledger.referenceId)
                put("mutationType", ledger.mutationType)
                put("quantityChange", ledger.quantityChange)
                put("finalStock", ledger.finalStock)
                put("notes", ledger.notes ?: JSONObject.NULL)
                put("isSynced", true)
                put("createdAt", ledger.createdAt)
            })
        }
        return SupabaseSyncManager.uploadRowWriteThrough(context, "bmp_stock_ledger", array, tenantId)
    }

    // ── Print Settings ────────────────────────────────────────────────────────

    suspend fun upsertPrintSettings(
        context: Context,
        tenantId: String,
        settings: PrintSettingsEntity
    ): SupabaseSyncManager.SyncResult {
        val array = JSONArray().apply {
            put(JSONObject().apply {
                put("id", settings.id)
                put("tenantId", settings.tenantId)
                put("moduleKey", settings.moduleKey)
                put("jpgUseLogo", settings.jpgUseLogo)
                put("jpgHeaderAlign", settings.jpgHeaderAlign)
                put("jpgUseSignature", settings.jpgUseSignature)
                put("jpgSignatureSenderName", settings.jpgSignatureSenderName)
                put("jpgSignatureReceiverName", settings.jpgSignatureReceiverName)
                put("jpgSignatureDrawnBase64", settings.jpgSignatureDrawnBase64 ?: JSONObject.NULL)
                put("jpgIsColor", settings.jpgIsColor)
                put("sjUseLogo", settings.sjUseLogo)
                put("sjHeaderAlign", settings.sjHeaderAlign)
                put("sjUseSignature", settings.sjUseSignature)
                put("sjSignatureSenderName", settings.sjSignatureSenderName)
                put("sjSignatureReceiverName", settings.sjSignatureReceiverName)
                put("sjSignatureDrawnBase64", settings.sjSignatureDrawnBase64 ?: JSONObject.NULL)
                put("sjIsColor", settings.sjIsColor)
                put("invoiceUseLogo", settings.invoiceUseLogo)
                put("invoiceHeaderAlign", settings.invoiceHeaderAlign)
                put("invoiceUseSignature", settings.invoiceUseSignature)
                put("invoiceSignatureSenderName", settings.invoiceSignatureSenderName)
                put("invoiceSignatureReceiverName", settings.invoiceSignatureReceiverName)
                put("invoiceSignatureDrawnBase64", settings.invoiceSignatureDrawnBase64 ?: JSONObject.NULL)
                put("invoiceIsColor", settings.invoiceIsColor)
                put("receiptPaperWidth", settings.receiptPaperWidth)
                put("receiptUseLogo", settings.receiptUseLogo)
                put("receiptHeaderAlign", settings.receiptHeaderAlign)
                put("receiptIsColor", settings.receiptIsColor)
                put("receiptShowItemPrice", settings.receiptShowItemPrice)
                put("receiptFooterText", settings.receiptFooterText)
                put("jpgTemplateType", settings.jpgTemplateType)
                put("sjTemplateType", settings.sjTemplateType)
                put("invoiceTemplateType", settings.invoiceTemplateType)
                put("bankOwnerName", settings.bankOwnerName)
                put("bankName", settings.bankName)
                put("bankAccountNumber", settings.bankAccountNumber)
                put("logoPath", settings.logoPath ?: JSONObject.NULL)
                put("createdAt", settings.createdAt)
                put("updatedAt", settings.updatedAt)
            })
        }
        return SupabaseSyncManager.uploadRowWriteThrough(context, "print_settings", array, tenantId)
    }
}
