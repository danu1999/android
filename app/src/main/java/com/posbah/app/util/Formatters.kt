package com.posbah.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val idLocale = Locale("in", "ID")
    private val currencyFormat = NumberFormat.getCurrencyInstance(idLocale).apply {
        maximumFractionDigits = 0
    }
    private val numberFormat = NumberFormat.getNumberInstance(idLocale)
    private val dateLong = SimpleDateFormat("dd MMM yyyy", idLocale)
    private val dateShort = SimpleDateFormat("dd/MM/yy", idLocale)
    private val dateTime = SimpleDateFormat("dd MMM yyyy, HH:mm", idLocale)
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun rupiah(value: Double): String = currencyFormat.format(value)
    fun rupiah(value: Long): String = currencyFormat.format(value)
    fun number(value: Double): String = numberFormat.format(value)
    fun number(value: Long): String = numberFormat.format(value)

    /** Tampilkan Double sebagai bilangan bulat tanpa notasi saintifik. Contoh: 14080000.0 → "14080000" */
    fun plainInt(value: Double): String = value.toLong().toString()

    /** Tampilkan Double tanpa notasi saintifik, trailing zeros dihapus. Contoh: 1.5 → "1.5", 2.0 → "2" */
    fun plainDecimal(value: Double): String =
        value.toBigDecimal().stripTrailingZeros().toPlainString()

    fun dateLong(epochMs: Long): String = dateLong.format(Date(epochMs))
    fun dateShort(epochMs: Long): String = dateShort.format(Date(epochMs))
    fun dateTime(epochMs: Long): String = dateTime.format(Date(epochMs))
    fun iso(epochMs: Long): String = iso.format(Date(epochMs))

    fun parseIso(s: String): Long? = runCatching { iso.parse(s)?.time }.getOrNull()

    fun invoiceStatus(status: String): String = when (status.uppercase()) {
        "PAID" -> "Lunas"
        "UNPAID" -> "Belum Bayar"
        "PARTIAL" -> "Cicil"
        "OVERDUE" -> "Jatuh Tempo"
        "DRAFT" -> "Draft"
        else -> status
    }
}
