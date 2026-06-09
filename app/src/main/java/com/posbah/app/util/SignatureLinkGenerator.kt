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
    const val BASE_URL = "https://posbah.com/sign/"

    /**
     * Membuat URL share untuk tanda tangan penerima.
     *
     * @param invoiceId ID dari invoice terkait.
     * @param durationMinutes Durasi link aktif dalam menit (default 3 menit).
     * @return URL lengkap dengan token Base64 aman.
     */
    fun generateShareLink(invoiceId: Long, durationMinutes: Int = 3): String {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        val dataToSign = "$invoiceId:$expiry"
        val signature = computeHmacSha256(dataToSign, SECRET_KEY)
        
        // Token format: "invoiceId:expiry:signature"
        val tokenRaw = "$invoiceId:$expiry:$signature"
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
            if (parts.size != 3) return null

            val invoiceId = parts[0].toLongOrNull() ?: return null
            val expiry = parts[1].toLongOrNull() ?: return null
            val signature = parts[2]

            // Cek kadaluarsa
            if (System.currentTimeMillis() > expiry) return null

            // Cek kecocokan signature
            val dataToSign = "$invoiceId:$expiry"
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
