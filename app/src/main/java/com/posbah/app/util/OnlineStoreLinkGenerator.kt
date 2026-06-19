package com.posbah.app.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Utilitas untuk membuat dan memvalidasi link Toko Online dinamis berdurasi 5 menit.
 */
object OnlineStoreLinkGenerator {

    private const val SECRET_KEY = "PosBahStoreSecretKey123!"
    const val BASE_URL = "https://www.zedmz.cloud/store/"

    fun generateShareLink(tenantId: String): String {
        val signature = computeHmacSha256(tenantId, SECRET_KEY)
        val tokenRaw = "$tenantId:$signature"
        val tokenEncoded = Base64.encodeToString(
            tokenRaw.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
        
        return "$BASE_URL$tokenEncoded"
    }

    fun validateToken(tokenEncoded: String): String? {
        return try {
            val decodedBytes = Base64.decode(tokenEncoded, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
            val tokenRaw = String(decodedBytes, StandardCharsets.UTF_8)
            val parts = tokenRaw.split(":")
            if (parts.size == 2) {
                val tenantId = parts[0]
                val signature = parts[1]
                val expectedSignature = computeHmacSha256(tenantId, SECRET_KEY)
                if (expectedSignature == signature) tenantId else null
            } else if (parts.size == 3) {
                val tenantId = parts[0]
                val expiry = parts[1].toLongOrNull() ?: return null
                val signature = parts[2]
                val expectedSignature = computeHmacSha256("$tenantId:$expiry", SECRET_KEY)
                if (expectedSignature == signature) tenantId else null
            } else {
                null
            }
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
