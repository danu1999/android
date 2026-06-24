package com.posbah.app.ui.print

import com.posbah.app.data.local.entities.PrintSettingsEntity

enum class HeaderAlign { LEFT, CENTER }
enum class PaperWidth { MM80, MM58 }

/** Konfigurasi cetak untuk satu jenis dokumen (JPG / Surat Jalan / Invoice PDF). */
data class DocPrintConfig(
    val useLogo: Boolean = true,
    val headerAlign: HeaderAlign = HeaderAlign.LEFT,
    val useSignature: Boolean = true,
    val signatureSenderName: String = "Admin",
    val signatureReceiverName: String = "",
    val signatureDrawnBase64: String? = null,
    val isColor: Boolean = true,
    val templateType: String = "MODERN"
)

data class PrintConfig(
    /** Pengaturan khusus untuk Cetak JPG */
    val jpg: DocPrintConfig = DocPrintConfig(isColor = true),
    /** Pengaturan khusus untuk Surat Jalan */
    val sj: DocPrintConfig = DocPrintConfig(isColor = false),
    /** Pengaturan khusus untuk Cetak Invoice / Faktur PDF */
    val invoice: DocPrintConfig = DocPrintConfig(isColor = true),

    // ─── Struk POS (tetap independen) ─────────────────────────────────────────
    val receiptPaperWidth: PaperWidth = PaperWidth.MM80,
    val receiptUseLogo: Boolean = true,
    val receiptHeaderAlign: HeaderAlign = HeaderAlign.CENTER,
    val receiptIsColor: Boolean = false,
    val receiptShowItemPrice: Boolean = true,
    val receiptFooterText: String = "Terima kasih sudah berbelanja!",

    // ─── Info Pembayaran Bank/Wallet (Dinamis) ──────────────────────────────────
    val bankOwnerName: String = "",
    val bankName: String = "BCA",
    val bankAccountNumber: String = "",
    val logoPath: String? = null,
    /** URL permanen logo dari VPS, digunakan sebagai fallback jika logoPath lokal kosong */
    val logoUrl: String? = null
) {
    companion object {
        fun fromEntity(entity: PrintSettingsEntity?): PrintConfig {
            if (entity == null) return PrintConfig()
            return PrintConfig(
                jpg = DocPrintConfig(
                    useLogo = entity.jpgUseLogo,
                    headerAlign = if (entity.jpgHeaderAlign == "CENTER") HeaderAlign.CENTER else HeaderAlign.LEFT,
                    useSignature = entity.jpgUseSignature,
                    signatureSenderName = entity.jpgSignatureSenderName,
                    signatureReceiverName = entity.jpgSignatureReceiverName,
                    signatureDrawnBase64 = entity.jpgSignatureDrawnBase64,
                    isColor = entity.jpgIsColor,
                    templateType = entity.jpgTemplateType
                ),
                sj = DocPrintConfig(
                    useLogo = entity.sjUseLogo,
                    headerAlign = if (entity.sjHeaderAlign == "CENTER") HeaderAlign.CENTER else HeaderAlign.LEFT,
                    useSignature = entity.sjUseSignature,
                    signatureSenderName = entity.sjSignatureSenderName,
                    signatureReceiverName = entity.sjSignatureReceiverName,
                    signatureDrawnBase64 = entity.sjSignatureDrawnBase64,
                    isColor = entity.sjIsColor,
                    templateType = entity.sjTemplateType
                ),
                invoice = DocPrintConfig(
                    useLogo = entity.invoiceUseLogo,
                    headerAlign = if (entity.invoiceHeaderAlign == "CENTER") HeaderAlign.CENTER else HeaderAlign.LEFT,
                    useSignature = entity.invoiceUseSignature,
                    signatureSenderName = entity.invoiceSignatureSenderName,
                    signatureReceiverName = entity.invoiceSignatureReceiverName,
                    signatureDrawnBase64 = entity.invoiceSignatureDrawnBase64,
                    isColor = entity.invoiceIsColor,
                    templateType = entity.invoiceTemplateType
                ),
                receiptPaperWidth = if (entity.receiptPaperWidth == "MM58") PaperWidth.MM58 else PaperWidth.MM80,
                receiptUseLogo = entity.receiptUseLogo,
                receiptHeaderAlign = if (entity.receiptHeaderAlign == "LEFT") HeaderAlign.LEFT else HeaderAlign.CENTER,
                receiptIsColor = entity.receiptIsColor,
                receiptShowItemPrice = entity.receiptShowItemPrice,
                receiptFooterText = entity.receiptFooterText,
                bankOwnerName = entity.bankOwnerName,
                bankName = entity.bankName,
                bankAccountNumber = entity.bankAccountNumber,
                logoPath = entity.logoPath,
                logoUrl = entity.logoUrl
            )
        }

        /**
         * Overload untuk PrintSettingsData dari online mode.
         * Digunakan oleh RentalViewModel, LaundryViewModel, PosViewModel.
         */
        fun fromEntity(data: com.posbah.app.data.repository.PrintSettingsData?): PrintConfig {
            if (data == null) return PrintConfig()
            return PrintConfig(
                receiptPaperWidth = if (data.paperWidth == "MM58") PaperWidth.MM58 else PaperWidth.MM80,
                receiptUseLogo = data.useLogo,
                receiptHeaderAlign = if (data.headerAlign == "LEFT") HeaderAlign.LEFT else HeaderAlign.CENTER,
                receiptIsColor = data.isColor,
                receiptShowItemPrice = data.showItemPrice,
                receiptFooterText = data.footerText,
                bankOwnerName = data.bankOwnerName,
                bankName = data.bankName,
                bankAccountNumber = data.bankAccountNumber,
                logoUrl = data.logoUrl
            )
        }
    }
}
