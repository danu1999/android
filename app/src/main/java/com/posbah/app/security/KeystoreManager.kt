package com.posbah.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all cryptographic material in the Android Keystore (hardware-backed
 * when available). Provides:
 *   - Master DB encryption key (used to derive SQLCipher passphrase)
 *   - AES-GCM encrypt/decrypt for arbitrary payloads (e.g. PINs, refresh tokens)
 *
 * Keys never leave the Keystore. On TEE/Strongbox-capable devices the private
 * key material is held in hardware and can NOT be extracted even by root.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /** Generate (or fetch) the master AES key. */
    private fun getOrCreateMasterKey(alias: String): SecretKey {
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    /** Encrypt arbitrary bytes. Returns IV || ciphertext (no separator). */
    fun encrypt(alias: String, plaintext: ByteArray): ByteArray {
        val key = getOrCreateMasterKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ct = cipher.doFinal(plaintext)
        return iv + ct
    }

    /** Decrypt bytes produced by [encrypt]. */
    fun decrypt(alias: String, payload: ByteArray): ByteArray {
        val key = getOrCreateMasterKey(alias)
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH)
        val ct = payload.copyOfRange(GCM_IV_LENGTH, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    /**
     * Derive a stable, device-bound passphrase for SQLCipher.
     * The seed is generated once and stored encrypted in keystore-protected prefs.
     * Result: Even if the DB file is copied off-device, it cannot be decrypted
     * without the per-device Keystore-backed key.
     */
    fun deriveDatabaseKey(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(SECURE_PREFS, Context.MODE_PRIVATE)
        val storedEnc = prefs.getString(DB_SEED_KEY, null)

        val seed: ByteArray = if (storedEnc == null) {
            val newSeed = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            val enc = encrypt(DB_KEY_ALIAS, newSeed)
            prefs.edit().putString(DB_SEED_KEY, android.util.Base64.encodeToString(enc, android.util.Base64.NO_WRAP)).apply()
            newSeed
        } else {
            decrypt(DB_KEY_ALIAS, android.util.Base64.decode(storedEnc, android.util.Base64.NO_WRAP))
        }
        return seed
    }

    fun deleteKey(alias: String) {
        runCatching { keyStore.deleteEntry(alias) }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128

        const val DB_KEY_ALIAS = "posbah_db_master"
        const val PIN_KEY_ALIAS = "posbah_pin_master"
        const val SESSION_KEY_ALIAS = "posbah_session_master"

        private const val SECURE_PREFS = "posbah_secure_prefs"
        private const val DB_SEED_KEY = "db_seed_enc"
    }
}
