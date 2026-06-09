package com.posbah.app.security

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN / password hashing using PBKDF2-HMAC-SHA256 with a random per-record salt.
 * Output format: "v1$iterations$base64Salt$base64Hash"
 * Equivalent in security model to the original POSBah Pbkdf2 hashing used on the
 * web backend, so existing employee PIN hashes can be migrated if needed.
 */
object PinHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LEN = 256
    private const val SALT_LEN = 16

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_LEN).also { java.security.SecureRandom().nextBytes(it) }
        val hash = derive(pin.toCharArray(), salt, ITERATIONS, KEY_LEN)
        return "v1$" +
            ITERATIONS + "$" +
            android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP) + "$" +
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }

    fun verify(pin: String, stored: String): Boolean {
        return runCatching {
            val parts = stored.split("$")
            if (parts.size != 4 || parts[0] != "v1") return false
            val iters = parts[1].toInt()
            val salt = android.util.Base64.decode(parts[2], android.util.Base64.NO_WRAP)
            val expected = android.util.Base64.decode(parts[3], android.util.Base64.NO_WRAP)
            val actual = derive(pin.toCharArray(), salt, iters, expected.size * 8)
            constantTimeEquals(expected, actual)
        }.getOrDefault(false)
    }

    private fun derive(pin: CharArray, salt: ByteArray, iters: Int, keyBits: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iters, keyBits)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}

object BackendHasher {
    private const val HASH_SALT = "posbah_default_salt_secret"
    private const val ITERATIONS = 1000
    private const val KEY_LEN = 512

    fun hash(password: String): String {
        val spec = PBEKeySpec(password.toCharArray(), HASH_SALT.toByteArray(Charsets.UTF_8), ITERATIONS, KEY_LEN)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val hashBytes = skf.generateSecret(spec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, storedHash: String): Boolean {
        if (storedHash.length != 128) return false
        val computedHash = hash(password)
        return computedHash.equals(storedHash, ignoreCase = true)
    }
}

