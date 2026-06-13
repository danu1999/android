package com.posbah.app.ui.print

import android.content.Context
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import com.posbah.app.data.local.entities.ProductEntity

object ReceiptPrinter {

    fun generateReceiptHtml(
        context: Context,
        t: TransactionEntity,
        items: List<TransactionItemEntity>,
        products: List<ProductEntity>,
        printConfig: PrintConfig
    ): String {
        val storeName = "PISANG KEJU RAMAYANA"
        val subheader = "Struk Pembayaran POS"

        val qLine = t.queueNumber?.let {
            "<div class=\"queue-box\">No. Antrian: #$it</div>"
        } ?: ""
        val cLine = t.customerName?.let { "<div>Pelanggan: $it</div>" } ?: ""
        val notesLine = t.notes?.let { "<div>Catatan: $it</div>" } ?: ""

        val itemsHtml = StringBuilder()
        for (item in items) {
            val prod = products.find { it.id == item.productId }
            val name = item.variantName?.let { "${prod?.name ?: "Item"} ($it)" } ?: (prod?.name ?: "Item")
            val noteLine = item.note?.let { "<tr><td colspan=\"3\" class=\"indent\">&#8627; $it</td></tr>" } ?: ""
            val lineTotal = item.quantity * item.price - item.discount

            itemsHtml.append("<tr><td colspan=\"3\" class=\"item-name\">$name</td></tr>")
            itemsHtml.append(noteLine)

            if (printConfig.receiptShowItemPrice) {
                itemsHtml.append("""
                    <tr>
                        <td class="item-qty">${item.quantity}x</td>
                        <td class="item-price">Rp${formatRupiahValue(item.price)}</td>
                        <td class="item-total r">Rp${formatRupiahValue(lineTotal)}</td>
                    </tr>
                """.trimIndent())
            } else {
                itemsHtml.append("""
                    <tr>
                        <td colspan="2">${item.quantity}x</td>
                        <td class="item-total r">Rp${formatRupiahValue(lineTotal)}</td>
                    </tr>
                """.trimIndent())
            }
        }

        val dAmt = t.discountAmt
        val dLabel = if (t.discountType == "percent") "Diskon (${t.discountInput}%)" else "Diskon"
        val discRow = if (dAmt > 0) "<tr><td colspan=\"2\">$dLabel</td><td class=\"r\">-Rp${formatRupiahValue(dAmt)}</td></tr>" else ""
        val subRow = if (dAmt > 0) "<tr><td colspan=\"2\">Subtotal</td><td class=\"r\">Rp${formatRupiahValue(t.subtotal)}</td></tr>" else ""
        val paidRow = t.amountPaid?.let {
            """
            <tr><td colspan="2">Bayar (Tunai)</td><td class="r">Rp${formatRupiahValue(it)}</td></tr>
            <tr><td colspan="2">Kembali</td><td class="r">Rp${formatRupiahValue((t.change ?: 0.0))}</td></tr>
            """.trimIndent()
        } ?: ""

        val is58 = printConfig.receiptPaperWidth == PaperWidth.MM58
        val paperWidthCss = if (is58) "58mm" else "80mm"
        val fontSizeCss = if (is58) "11px" else "13px"
        val paddingCss = if (is58) "0.3cm" else "0.6cm"

        val logoBase64 = if (printConfig.receiptUseLogo) getUriOrAssetBase64(context, printConfig.logoPath, "logo.jpg") else ""
        val themeColor = if (printConfig.receiptIsColor) "#1E3A8A" else "#000000"

        val headerHtml = if (logoBase64.isNotEmpty()) {
            """
            <div class="c" style="margin-bottom: 8px;">
                <img src="$logoBase64" style="width: 50px; height: auto;" /><br>
                <span class="store-title b" style="color: $themeColor;">$storeName</span><br>
                <span>$subheader</span>
            </div>
            """
        } else {
            val alignClass = if (printConfig.receiptHeaderAlign == HeaderAlign.LEFT) "l" else "c"
            """
            <div class="$alignClass" style="margin-bottom: 8px;">
                <span class="store-title b" style="color: $themeColor;">$storeName</span><br>
                <span>$subheader</span>
            </div>
            """
        }

        return """
            <html>
            <head>
                <style>
                    @page { margin: 0; }
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        width: $paperWidthCss;
                        margin: 0 auto;
                        padding: $paddingCss;
                        font-size: $fontSizeCss;
                        color: #000;
                        background-color: #fff;
                    }
                    .c { text-align: center; }
                    .l { text-align: left; }
                    .b { font-weight: bold; }
                    .r { text-align: right; }
                    hr { border-top: 1px dashed #000; border-bottom: none; border-left: none; border-right: none; margin: 8px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    td { vertical-align: top; padding: 2px 0; }
                    .store-title { font-size: 15px; }
                    .queue-box {
                        font-weight: bold;
                        font-size: 14px;
                        border: 1px dashed #000;
                        padding: 6px;
                        margin: 8px 0;
                        text-align: center;
                    }
                    .indent { padding-left: 8px; font-size: 10px; color: #444; }
                    .item-name { font-weight: bold; word-break: break-all; }
                    .item-qty, .item-price { font-size: 90%; color: #333; }
                </style>
            </head>
            <body>
                $headerHtml
                <hr>
                <div>No: ${t.receiptNumber}</div>
                <div>Tanggal: ${com.posbah.app.util.Formatters.dateLong(t.createdAt)}</div>
                <div>Metode: ${when (t.paymentMethod.uppercase()) {
                    "CASH" -> "TUNAI"
                    "TRANSFER" -> "TRANSFER"
                    "QRIS" -> "QRIS"
                    else -> t.paymentMethod
                }}</div>
                $cLine
                $notesLine
                <hr>
                $qLine
                <table>
                    $itemsHtml
                </table>
                <hr>
                <table>
                    $subRow
                    $discRow
                    <tr>
                        <td colspan="2" class="b">TOTAL</td>
                        <td class="r b" style="font-size: 110%;">Rp${formatRupiahValue(t.total)}</td>
                    </tr>
                    $paidRow
                </table>
                <hr>
                <div class="c b">${printConfig.receiptFooterText}</div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getAssetBase64(context: Context, fileName: String): String {
        return try {
            val inputStream = context.assets.open(fileName)
            val bytes = inputStream.readBytes()
            inputStream.close()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val extension = when {
                fileName.endsWith(".png", true) -> "png"
                fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "jpeg"
                else -> "png"
            }
            "data:image/$extension;base64,$base64"
        } catch (e: Exception) {
            ""
        }
    }

    private fun getUriOrAssetBase64(context: Context, pathOrUri: String?, defaultAsset: String): String {
        if (pathOrUri.isNullOrBlank()) {
            return getAssetBase64(context, defaultAsset)
        }
        return try {
            val uri = android.net.Uri.parse(pathOrUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val bytes = inputStream.readBytes()
                inputStream.close()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val mimeType = context.contentResolver.getType(uri) ?: "image/png"
                "data:$mimeType;base64,$base64"
            } else {
                getAssetBase64(context, defaultAsset)
            }
        } catch (e: Exception) {
            getAssetBase64(context, defaultAsset)
        }
    }

    private fun formatRupiahValue(number: Number): String {
        return String.format("%,d", number.toLong()).replace(",", ".")
    }
}
