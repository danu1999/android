package com.posbah.app.ui

import androidx.lifecycle.ViewModel
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.security.SecurePreferences
import com.posbah.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PosBahRootViewModel @Inject constructor(
    private val securePrefs: SecurePreferences,
    private val tenantDao: TenantDao,
    private val userDao: LocalUserDao
) : ViewModel() {

    /**
     * Inspects the active tenant's businessMode to return the correct dashboard route path.
     * Redirects to SystemSelection if a demo user hasn't locked their POS system choice.
     */
    suspend fun getDashboardRoute(): String {
        val sub = securePrefs.currentGoogleSub
        if (sub != null) {
            val user = userDao.getBySub(sub)
            if (user != null && !user.isPremium && !user.businessModeLocked) {
                return Screen.SystemSelection.route
            }
        }
        val tenantId = securePrefs.currentTenantId ?: return Screen.Login.route
        val tenant = tenantDao.getById(tenantId) ?: return Screen.Login.route
        return when (tenant.businessMode) {
            "FNB" -> Screen.PosDashboard.route
            "RENTAL" -> Screen.RentalDashboard.route
            "LAUNDRY" -> Screen.LaundryDashboard.route
            else -> Screen.BmpDashboard.route
        }
    }
}
