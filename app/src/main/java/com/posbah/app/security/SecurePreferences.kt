package com.posbah.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted shared preferences for session data:
 *   - current Google sub & email
 *   - active tenantId, outletId
 *   - login lockout counters
 *
 * Backed by AES256_GCM + Android Keystore master key. The XML file on disk is
 * encrypted at rest, so even with root + file extraction the contents are useless.
 */
@Singleton
class SecurePreferences @Inject constructor(
    context: Context
) {
    private val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setUserAuthenticationRequired(false)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var currentGoogleSub: String?
        get() = prefs.getString(KEY_GOOGLE_SUB, null)
        set(value) = prefs.edit().putString(KEY_GOOGLE_SUB, value).apply()

    var currentEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var currentTenantId: String?
        get() = prefs.getString(KEY_TENANT, null)
        set(value) = prefs.edit().putString(KEY_TENANT, value).apply()

    var currentOutletId: Long?
        get() = prefs.getLong(KEY_OUTLET, -1L).takeIf { it >= 0 }
        set(value) {
            if (value == null) prefs.edit().remove(KEY_OUTLET).apply()
            else prefs.edit().putLong(KEY_OUTLET, value).apply()
        }

    var lastSessionTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SESSION, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SESSION, value).apply()

    var failedPinAttempts: Int
        get() = prefs.getInt(KEY_FAILED_PIN, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_PIN, value).apply()

    var lockoutUntil: Long
        get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKOUT_UNTIL, value).apply()

    var isDemoCleanedV208: Boolean
        get() = prefs.getBoolean("demo_cleaned_v208", false)
        set(value) = prefs.edit().putBoolean("demo_cleaned_v208", value).apply()

    /** Wipe entire encrypted session. Called on logout / tamper detection. */
    fun wipe() {
        prefs.edit().clear().apply()
    }

    fun setActiveSession(googleSub: String?, email: String?) {
        prefs.edit()
            .putString(KEY_GOOGLE_SUB, googleSub)
            .putString(KEY_EMAIL, email)
            .putLong(KEY_LAST_SESSION, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val FILE_NAME = "posbah_session"
        private const val MASTER_KEY_ALIAS = "posbah_session_master"
        private const val KEY_GOOGLE_SUB = "google_sub"
        private const val KEY_EMAIL = "email"
        private const val KEY_TENANT = "tenant_id"
        private const val KEY_OUTLET = "outlet_id"
        private const val KEY_LAST_SESSION = "last_session_ts"
        private const val KEY_FAILED_PIN = "failed_pin"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
    }
}
