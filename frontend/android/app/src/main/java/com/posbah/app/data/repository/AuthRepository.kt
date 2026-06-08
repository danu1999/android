package com.posbah.app.data.repository

import com.posbah.app.auth.GoogleSignInClient
import com.posbah.app.data.local.dao.EmployeeDao
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.OutletDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.security.PinHasher
import com.posbah.app.security.SecurePreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val googleClient: GoogleSignInClient,
    private val userDao: LocalUserDao,
    private val tenantDao: TenantDao,
    private val outletDao: OutletDao,
    private val employeeDao: EmployeeDao,
    private val securePrefs: SecurePreferences
) {

    sealed class LoginOutcome {
        data class Success(val user: LocalUser, val tenant: Tenant) : LoginOutcome()
        data class NeedsTenantPick(val user: LocalUser, val tenants: List<Tenant>) : LoginOutcome()
        object Cancelled : LoginOutcome()
        data class Error(val message: String) : LoginOutcome()
        object Locked : LoginOutcome()
    }

    /**
     * Google SSO login. On success:
     *  - upserts LocalUser
     *  - if no tenant exists for this owner, auto-creates default tenant + outlet
     *  - persists session in encrypted prefs
     */
    suspend fun loginWithGoogle(activity: android.app.Activity): LoginOutcome {
        return when (val r = googleClient.signIn(activity)) {
            is GoogleSignInClient.Result.Success -> {
                val identity = r.identity
                val existing = userDao.getBySub(identity.sub)
                val user = (existing ?: LocalUser(
                    googleSub = identity.sub,
                    email = identity.email,
                    displayName = identity.displayName,
                    photoUrl = identity.photoUrl
                )).copy(
                    email = identity.email,
                    displayName = identity.displayName ?: existing?.displayName,
                    photoUrl = identity.photoUrl ?: existing?.photoUrl,
                    lastLoginAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.upsert(user)

                // Tenant resolution
                val tenants = tenantDao.listForOwner(identity.email)
                when {
                    tenants.isEmpty() -> {
                        val newTenant = createDefaultTenant(identity.email, identity.displayName)
                        userDao.setTenant(user.googleSub, newTenant.id)
                        securePrefs.setActiveSession(identity.sub, identity.email)
                        securePrefs.currentTenantId = newTenant.id
                        LoginOutcome.Success(user.copy(tenantId = newTenant.id), newTenant)
                    }
                    tenants.size == 1 -> {
                        userDao.setTenant(user.googleSub, tenants.first().id)
                        securePrefs.setActiveSession(identity.sub, identity.email)
                        securePrefs.currentTenantId = tenants.first().id
                        LoginOutcome.Success(user.copy(tenantId = tenants.first().id), tenants.first())
                    }
                    else -> {
                        securePrefs.setActiveSession(identity.sub, identity.email)
                        LoginOutcome.NeedsTenantPick(user, tenants)
                    }
                }
            }
            GoogleSignInClient.Result.Cancelled -> LoginOutcome.Cancelled
            is GoogleSignInClient.Result.Error.NoCredentials ->
                LoginOutcome.Error("Tidak ada akun Google tersedia di perangkat")
            is GoogleSignInClient.Result.Error.InvalidToken ->
                LoginOutcome.Error("Token Google tidak valid: ${r.reason}")
            is GoogleSignInClient.Result.Error.Unexpected ->
                LoginOutcome.Error(r.throwable.localizedMessage ?: "Error tidak diketahui")
        }
    }

    /**
     * PIN-based employee login. Lockout after 5 failed attempts for 5 minutes.
     */
    suspend fun loginWithPin(
        tenantId: String,
        email: String,
        pin: String
    ): LoginOutcome {
        // Lockout check
        val now = System.currentTimeMillis()
        if (securePrefs.lockoutUntil > now) return LoginOutcome.Locked

        val emp = employeeDao.findForLogin(tenantId, email)
            ?: return LoginOutcome.Error("Karyawan tidak ditemukan")

        if (!PinHasher.verify(pin, emp.pinHash)) {
            val attempts = securePrefs.failedPinAttempts + 1
            securePrefs.failedPinAttempts = attempts
            if (attempts >= 5) {
                securePrefs.lockoutUntil = now + 5 * 60 * 1000L
                return LoginOutcome.Locked
            }
            return LoginOutcome.Error("PIN salah (percobaan $attempts/5)")
        }

        // Successful PIN -> reset attempts
        securePrefs.failedPinAttempts = 0
        securePrefs.lockoutUntil = 0L
        val tenant = tenantDao.getById(tenantId) ?: return LoginOutcome.Error("Tenant tidak ditemukan")

        // Map employee → ephemeral LocalUser for session
        val pseudoSub = "emp:${emp.id}"
        val user = LocalUser(
            googleSub = pseudoSub,
            email = emp.email ?: "${emp.name}@local",
            displayName = emp.name,
            photoUrl = null,
            role = emp.role,
            tenantId = tenantId
        )
        userDao.upsert(user)
        securePrefs.setActiveSession(pseudoSub, user.email)
        securePrefs.currentTenantId = tenantId
        return LoginOutcome.Success(user, tenant)
    }

    private suspend fun createDefaultTenant(ownerEmail: String, displayName: String?): Tenant {
        val tenant = Tenant(
            id = "ten_" + UUID.randomUUID().toString().replace("-", "").take(16),
            name = displayName?.let { "$it - BMP" } ?: "Tenant Baru",
            ownerEmail = ownerEmail,
            businessMode = "BMP"
        )
        tenantDao.upsert(tenant)
        outletDao.insert(
            Outlet(
                tenantId = tenant.id,
                name = "Outlet Utama",
                isDefault = true
            )
        )
        return tenant
    }

    suspend fun selectTenant(googleSub: String, tenantId: String): Boolean {
        val tenant = tenantDao.getById(tenantId) ?: return false
        userDao.setTenant(googleSub, tenantId)
        securePrefs.currentTenantId = tenantId
        return true
    }

    suspend fun logout() {
        googleClient.signOut()
        securePrefs.wipe()
    }

    fun activeUserSub(): String? = securePrefs.currentGoogleSub
    fun activeTenantId(): String? = securePrefs.currentTenantId
}
