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
 */
@Singleton
class SessionState @Inject constructor(
    private val securePrefs: SecurePreferences
) {
    private val _tenantId = MutableStateFlow<String?>(securePrefs.currentTenantId)
    val tenantId: StateFlow<String?> = _tenantId.asStateFlow()

    private val _outletId = MutableStateFlow<Long?>(securePrefs.currentOutletId)
    val outletId: StateFlow<Long?> = _outletId.asStateFlow()

    fun setTenant(id: String?) {
        securePrefs.currentTenantId = id
        _tenantId.value = id
    }

    fun setOutlet(id: Long?) {
        securePrefs.currentOutletId = id
        _outletId.value = id
    }

    fun requireTenantId(): String =
        _tenantId.value ?: error("No active tenant. User must complete login + tenant selection.")
}
