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
 *   - session timestamps
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

    var lastFailedAttemptTime: Long
        get() = prefs.getLong("last_failed_attempt_time", 0L)
        set(value) = prefs.edit().putLong("last_failed_attempt_time", value).apply()

    var lockoutUntil: Long
        get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKOUT_UNTIL, value).apply()

    var isDemoCleanedV211: Boolean
        get() = prefs.getBoolean("demo_cleaned_v211", false)
        set(value) = prefs.edit().putBoolean("demo_cleaned_v211", value).apply()

    var tempPlainPassword: String?
        get() = prefs.getString("temp_plain_password", null)
        set(value) = prefs.edit().putString("temp_plain_password", value).apply()

    var simulatedUserUuid: String?
        get() = prefs.getString("simulated_user_uuid", null)
        set(value) = prefs.edit().putString("simulated_user_uuid", value).apply()

    var lastSyncedTimestamp: Long
        get() = prefs.getLong("last_synced_ts_" + (currentTenantId ?: ""), 0L)
        set(value) = prefs.edit().putLong("last_synced_ts_" + (currentTenantId ?: ""), value).apply()

    var isPosRedesignWipedV1: Boolean
        get() = prefs.getBoolean("pos_redesign_wipe_v1", false)
        set(value) = prefs.edit().putBoolean("pos_redesign_wipe_v1", value).apply()

    /**
     * ID outlet yang dikunci untuk karyawan non-OWNER.
     * Diset otomatis saat login berhasil, berdasarkan employee.outletId.
     * Hanya OWNER yang bisa null (bebas ganti outlet).
     */
    var employeeOutletIdLocked: Long?
        get() = prefs.getLong(KEY_EMPLOYEE_OUTLET, -1L).takeIf { it >= 0 }
        set(value) {
            if (value == null) prefs.edit().remove(KEY_EMPLOYEE_OUTLET).apply()
            else prefs.edit().putLong(KEY_EMPLOYEE_OUTLET, value).apply()
        }

    var lastActiveTenantId: String?
        get() = prefs.getString("last_active_tenant_id", null)
        set(value) = prefs.edit().putString("last_active_tenant_id", value).apply()

    // ── Full Online mode additions ─────────────────────────────────────────────

    /** Business mode aktif: "FNB", "BMP", "RENTAL", "LAUNDRY" */
    var currentBusinessMode: String?
        get() = prefs.getString("current_business_mode", null)
        set(value) = prefs.edit().putString("current_business_mode", value).apply()

    /** Jumlah ganti password hari ini (reset tiap hari). */
    var passwordChangeTodayCount: Int
        get() = prefs.getInt("pwd_change_count_today", 0)
        set(value) = prefs.edit().putInt("pwd_change_count_today", value).apply()

    /** Timestamp terakhir ganti password (untuk rate-limit per hari). */
    var lastPasswordChangeDayMillis: Long
        get() = prefs.getLong("pwd_change_last_day", 0L)
        set(value) = prefs.edit().putLong("pwd_change_last_day", value).apply()

    /** Flag migrasi full-online selesai. Cukup 1 kali per akun. */
    var migrationCompleted: Boolean
        get() = prefs.getBoolean("migration_completed_v3", false)
        set(value) = prefs.edit().putBoolean("migration_completed_v3", value).apply()

    /** Role user aktif: "OWNER" | "KASIR" | "OUTLETKARYAWAN" */
    var currentRole: String?
        get() = prefs.getString("current_role", null)
        set(value) = prefs.edit().putString("current_role", value).apply()

    /** Nama tenant aktif — disimpan saat login/pilih tenant. */
    var currentTenantName: String?
        get() = prefs.getString("current_tenant_name", null)
        set(value) = prefs.edit().putString("current_tenant_name", value).apply()


    /** Wipe entire encrypted session. Called on logout / tamper detection. */
    fun wipe() {
        val lastTenant = lastActiveTenantId ?: currentTenantId
        prefs.edit().clear().apply()
        if (lastTenant != null) {
            prefs.edit().putString("last_active_tenant_id", lastTenant).apply()
        }
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
        private const val KEY_EMPLOYEE_OUTLET = "employee_outlet_locked"
    }
}
