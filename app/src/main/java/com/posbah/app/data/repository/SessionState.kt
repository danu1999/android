package com.posbah.app.data.repository

import com.posbah.app.security.SecurePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently active tenant + outlet for the duration of a session.
 * Persisted to encrypted prefs for restart resilience.
 *
 * v2.6.0: Added isSyncing indicator and employee outlet lock enforcement.
 */
@Singleton
class SessionState @Inject constructor(
    private val securePrefs: SecurePreferences
) {
    private val _tenantId = MutableStateFlow<String?>(securePrefs.currentTenantId)
    val tenantId: StateFlow<String?> = _tenantId.asStateFlow()

    private val _outletId = MutableStateFlow<Long?>(securePrefs.currentOutletId)
    val outletId: StateFlow<Long?> = _outletId.asStateFlow()

    /** True saat syncAll / pullAll sedang berjalan — dipakai UI untuk indikator sync. */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * ID outlet yang dikunci untuk karyawan non-OWNER.
     * Null = tidak dikunci (OWNER bebas ganti outlet).
     */
    private val _lockedEmployeeOutletId = MutableStateFlow<Long?>(securePrefs.employeeOutletIdLocked)
    val lockedEmployeeOutletId: StateFlow<Long?> = _lockedEmployeeOutletId.asStateFlow()

    fun setTenant(id: String?) {
        securePrefs.currentTenantId = id
        _tenantId.value = id
    }

    fun setOutlet(id: Long?) {
        securePrefs.currentOutletId = id
        _outletId.value = id
    }

    /**
     * Kunci outlet untuk karyawan non-OWNER.
     * Dipanggil saat login PIN/email karyawan berhasil,
     * dengan outletId dari Employee.outletId di database.
     */
    fun setEmployeeOutletLock(outletId: Long?) {
        securePrefs.employeeOutletIdLocked = outletId
        _lockedEmployeeOutletId.value = outletId
        if (outletId != null) {
            setOutlet(outletId)
        }
    }

    /** Hapus lock outlet — dipanggil saat logout atau saat OWNER login. */
    fun clearEmployeeLock() {
        securePrefs.employeeOutletIdLocked = null
        _lockedEmployeeOutletId.value = null
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

    fun requireTenantId(): String =
        _tenantId.value ?: error("No active tenant. User must complete login + tenant selection.")
}
