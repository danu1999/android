package com.posbah.app.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Utilitas untuk membuat dan memvalidasi link tanda tangan penerima dinamis.
 *
 * Menggunakan HMAC-SHA256 untuk memastikan token link aman dan tidak dimanipulasi.
 * Link yang dihasilkan berdurasi kadaluarsa jangka pendek (misal 3 menit).
 */
object SignatureLinkGenerator {

    private const val SECRET_KEY = "PosBahSignatureSecretKey123!" // Secret shared with server
    const val BASE_URL = "https://www.zedmz.cloud/api/sign/"

    /**
     * Membuat URL share untuk tanda tangan penerima.
     *
     * @param invoiceId ID dari invoice terkait.
     * @param durationMinutes Durasi link aktif dalam menit (default 60 menit).
     * @return URL lengkap dengan token Base64 aman.
     */
    fun generateShareLink(tenantId: String, invoiceId: Long, durationMinutes: Int = 60): String {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        val dataToSign = "$tenantId:$invoiceId:$expiry"
        val signature = computeHmacSha256(dataToSign, SECRET_KEY)
        
        // Token format: "tenantId:invoiceId:expiry:signature"
        val tokenRaw = "$tenantId:$invoiceId:$expiry:$signature"
        val tokenEncoded = Base64.encodeToString(
            tokenRaw.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
        
        return "$BASE_URL$tokenEncoded"
    }

    /**
     * Memvalidasi token link tanda tangan (dipakai untuk testing/validasi).
     *
     * @param tokenEncoded Token terenkripsi dari URL.
     * @return ID invoice jika token valid & belum kadaluarsa, null jika gagal.
     */
    fun validateToken(tokenEncoded: String): Long? {
        return try {
            val decodedBytes = Base64.decode(tokenEncoded, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
            val tokenRaw = String(decodedBytes, StandardCharsets.UTF_8)
            val parts = tokenRaw.split(":")
            
            val tenantId: String
            val invoiceId: Long
            val expiry: Long
            val signature: String
            
            if (parts.size == 4) {
                tenantId = parts[0]
                invoiceId = parts[1].toLongOrNull() ?: return null
                expiry = parts[2].toLongOrNull() ?: return null
                signature = parts[3]
            } else if (parts.size == 3) {
                tenantId = ""
                invoiceId = parts[0].toLongOrNull() ?: return null
                expiry = parts[1].toLongOrNull() ?: return null
                signature = parts[2]
            } else {
                return null
            }

            // Cek kadaluarsa
            if (System.currentTimeMillis() > expiry) return null

            // Cek kecocokan signature
            val dataToSign = if (tenantId.isNotEmpty()) "$tenantId:$invoiceId:$expiry" else "$invoiceId:$expiry"
            val expectedSignature = computeHmacSha256(dataToSign, SECRET_KEY)
            
            if (expectedSignature == signature) invoiceId else null
        } catch (e: Exception) {
            null
        }
    }

    private fun computeHmacSha256(data: String, key: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), algorithm)
        mac.init(secretKey)
        val hash = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }
}
