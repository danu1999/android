package com.posbah.app.data.repository

import android.content.Context
import com.posbah.app.auth.GoogleSignInClient
import com.posbah.app.data.local.LocalDataSeeder
import com.posbah.app.data.local.dao.EmployeeDao
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.OutletDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.security.BackendHasher
import com.posbah.app.security.PinHasher
import com.posbah.app.security.SecurePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val securePrefs: SecurePreferences,
    private val localDataSeeder: LocalDataSeeder,
    @ApplicationContext private val context: Context
) {

    sealed class LoginOutcome {
        data class Success(val user: LocalUser, val tenant: Tenant) : LoginOutcome()
        data class NeedsTenantPick(val user: LocalUser, val tenants: List<Tenant>) : LoginOutcome()
        object Cancelled : LoginOutcome()
        data class Error(val message: String) : LoginOutcome()
        object Locked : LoginOutcome()
    }

    /**
     * Google SSO login - configured with simulated SSO to allow offline email testing.
     */
    suspend fun loginWithGoogle(
        activity: android.app.Activity,
        simulatedEmail: String? = null,
        simulatedName: String? = null
    ): LoginOutcome = withContext(Dispatchers.IO) {
        val cleanEmail = (simulatedEmail ?: "demo@posbah.app").lowercase().trim()
        val name = simulatedName?.ifBlank { null } ?: "User Demo"
        val sub = "google_sub_${cleanEmail.replace(".", "_").replace("@", "_")}"

        val isPremiumUser = cleanEmail == "muhammadmuizz8@gmail.com" ||
                            cleanEmail == "bahteramulyap@gmail.com" ||
                            cleanEmail == "hanafiariful@gmail.com" ||
                            cleanEmail == "fahrup22@gmail.com" ||
                            cleanEmail == "alfarisirosi40@gmail.com" ||
                            cleanEmail == "alfarisirosi04@gmail.com"
        val existing = userDao.getBySub(sub)

        // Rule: If they do not pay for 2 days since they created the demouser, they cannot login
        if (existing != null && !existing.isPremium && !isPremiumUser) {
            val elapsed = System.currentTimeMillis() - existing.registeredAt
            if (elapsed > 2 * 24 * 60 * 60 * 1000L) {
                return@withContext LoginOutcome.Error("Akun demo kedaluwarsa. Hubungi muhammadmuizz8@gmail.com untuk aktivasi premium.")
            }
        }

        // Rule: Prevent creating a new demouser with an expired email
        if (existing == null && !isPremiumUser) {
            val previousUser = userDao.getByEmail(cleanEmail)
            if (previousUser != null) {
                val elapsed = System.currentTimeMillis() - previousUser.registeredAt
                if (elapsed > 2 * 24 * 60 * 60 * 1000L) {
                    return@withContext LoginOutcome.Error("Email ini sudah kedaluwarsa untuk akun demo. Hubungi muhammadmuizz8@gmail.com untuk aktivasi.")
                }
            }
        }

        val user = (existing ?: LocalUser(
            googleSub = sub,
            email = cleanEmail,
            displayName = name,
            photoUrl = null,
            role = "OWNER",
            isPremium = isPremiumUser
        )).copy(
            email = cleanEmail,
            displayName = name,
            isPremium = isPremiumUser || (existing?.isPremium == true),
            lastLoginAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userDao.upsert(user)

        val isHanafiarifulGroup = cleanEmail == "hanafiariful@gmail.com" ||
                                  cleanEmail == "fahrup22@gmail.com" ||
                                  cleanEmail == "alfarisirosi40@gmail.com" ||
                                  cleanEmail == "alfarisirosi04@gmail.com"

        val tenantId = if (isHanafiarifulGroup) {
            "ten_premium_hanafiariful_gmail_com"
        } else if (user.isPremium) {
            "ten_premium_${cleanEmail.replace(".", "_").replace("@", "_")}"
        } else {
            "demo_tenant_${cleanEmail.replace(".", "_").replace("@", "_")}"
        }

        val existingTenant = tenantDao.getById(tenantId)
        val tenant = if (existingTenant == null) {
            val newTenant = Tenant(
                id = tenantId,
                name = if (isHanafiarifulGroup) "PISANG KEJU RAMAYANA"
                       else if (user.isPremium) "CV. $name (Premium)"
                       else "Demo - PISANG KEJU RAMAYANA",
                ownerEmail = if (isHanafiarifulGroup) "hanafiariful@gmail.com" else cleanEmail,
                businessMode = if (isHanafiarifulGroup) "FNB"
                               else if (user.isPremium) "BMP"
                               else "FNB"
            )
            tenantDao.upsert(newTenant)
            outletDao.insert(
                Outlet(
                    tenantId = newTenant.id,
                    name = "Outlet Utama",
                    isDefault = true
                )
            )
            newTenant
        } else {
            existingTenant
        }
        
        userDao.setTenant(sub, tenant.id)
        securePrefs.setActiveSession(sub, cleanEmail)
        securePrefs.currentTenantId = tenant.id
        
        // Seed mock/demo data if empty and not premium, or if it is the special hanafiariful tenant
        if (!user.isPremium || isHanafiarifulGroup) {
            localDataSeeder.seedFromSqlDump(context, tenant.id, null)
        }
        
        return@withContext LoginOutcome.Success(user.copy(tenantId = tenant.id), tenant)
    }

    /**
     * Local email and password authentication (no external network connections).
     */
    suspend fun loginWithEmailPassword(
        email: String,
        password: String
    ): LoginOutcome = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (securePrefs.lockoutUntil > now) return@withContext LoginOutcome.Locked

        val cleanEmail = email.lowercase().trim()
        val staticPremiumUsers = mapOf(
            "bahteramulyap@gmail.com" to Triple("8a0ff1f8926195dfde55af7e68c028591602dacc30dc3c7caef27a949ca45142b25514004cf4540c46eca830100d06517c6facc0faf77fc57140e9df5fe5ffc7", "CV Bahtera Mulya Plastik", "bahteramulyap@gmail.com"),
            "hanafiariful@gmail.com" to Triple("20710a82f8d6b458af10d49fbb1f985ac8aaf696e6b32e776d4f4ebbc30d08565e2bb5e1902ace18297d8db47ad35e49c086669125b1d6ac867c0d2d7e265e50", "PISANG KEJU RAMAYANA", "hanafiariful@gmail.com"),
            "fahrup22@gmail.com" to Triple("63e71711d1481b6da8b756e114aa2ac71a704929c0accf46f419706a5c1416ae1a312899ae84d3d8e33d255811e98fd4d17e59371a08e2f9c21c01d1b1c13a8d", "FahriP", "hanafiariful@gmail.com"),
            "alfarisirosi40@gmail.com" to Triple("a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c", "Mamet PKR", "hanafiariful@gmail.com"),
            "alfarisirosi04@gmail.com" to Triple("a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c", "Mamet PKR", "hanafiariful@gmail.com"),
            "syerlirahma7@gmail.com" to Triple("5819ef0d24208780b75c18009f0f69400eb933916f800ae980b778820cda595e3151de8600a0c325711f0e9641b5a72f393008868913578601ba0fa0d4c9ad93", "syerli", "bahteramulyap@gmail.com")
        )

        val successUser: LocalUser
        val successTenant: Tenant

        val staticInfo = staticPremiumUsers[cleanEmail]
        if (staticInfo != null) {
            val (hash, displayName, tenantId) = staticInfo
            if (!BackendHasher.verify(password, hash)) {
                return@withContext incrementFailedAttempts(now)
            }

            // Successfully authenticated premium/owner user
            securePrefs.failedPinAttempts = 0
            securePrefs.lockoutUntil = 0L

            // Ensure tenant exists in database
            val tenant = tenantDao.getById(tenantId) ?: Tenant(
                id = tenantId,
                name = if (tenantId == "bahteramulyap@gmail.com") "CV. BAHTERA MULYA PLASTIK" else "PISANG KEJU RAMAYANA",
                ownerEmail = tenantId,
                businessMode = if (tenantId == "bahteramulyap@gmail.com") "BMP" else "FNB"
            ).also { tenantDao.upsert(it) }

            // Ensure outlet exists
            val outlets = outletDao.listForTenant(tenantId)
            val outletId = if (outlets.isEmpty()) {
                outletDao.insert(
                    Outlet(
                        tenantId = tenantId,
                        name = "Outlet Utama",
                        isDefault = true
                    )
                )
            } else {
                outlets.first().id
            }

            // Seed tenant database from SQL assets if empty
            localDataSeeder.seedFromSqlDump(context, tenantId, outletId)

            val existing = userDao.getBySub(cleanEmail)
            val user = (existing ?: LocalUser(
                googleSub = cleanEmail,
                email = cleanEmail,
                displayName = displayName,
                photoUrl = null,
                role = if (cleanEmail == tenantId) "OWNER" else "ADMIN",
                isPremium = true
            )).copy(
                tenantId = tenantId,
                isPremium = true,
                lastLoginAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.upsert(user)

            successUser = user
            successTenant = tenant
        } else {
            // Check employees table in database
            val emp = employeeDao.findByEmail(cleanEmail)
                ?: return@withContext LoginOutcome.Error("Email atau password tidak ditemukan")

            val isPasswordValid = if (emp.pinHash.startsWith("v1$")) {
                PinHasher.verify(password, emp.pinHash)
            } else {
                BackendHasher.verify(password, emp.pinHash)
            }

            if (!isPasswordValid) {
                return@withContext incrementFailedAttempts(now)
            }

            // Successful PIN -> reset attempts
            securePrefs.failedPinAttempts = 0
            securePrefs.lockoutUntil = 0L
            val tenant = tenantDao.getById(emp.tenantId) ?: return@withContext LoginOutcome.Error("Tenant tidak ditemukan")

            // Map employee → ephemeral LocalUser for session
            val pseudoSub = "emp:${emp.id}"
            val user = LocalUser(
                googleSub = pseudoSub,
                email = emp.email ?: "${emp.name}@local",
                displayName = emp.name,
                photoUrl = null,
                role = emp.role,
                tenantId = emp.tenantId
            )
            userDao.upsert(user)

            successUser = user
            successTenant = tenant
        }

        securePrefs.setActiveSession(successUser.googleSub, successUser.email)
        securePrefs.currentTenantId = successUser.tenantId
        return@withContext LoginOutcome.Success(successUser, successTenant)
    }

    private fun incrementFailedAttempts(now: Long): LoginOutcome {
        val attempts = securePrefs.failedPinAttempts + 1
        securePrefs.failedPinAttempts = attempts
        if (attempts >= 5) {
            securePrefs.lockoutUntil = now + 5 * 60 * 1000L
            return LoginOutcome.Locked
        }
        return LoginOutcome.Error("Email atau password salah (percobaan $attempts/5)")
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

    suspend fun simulatePaymentAndUpgrade(
        email: String,
        password: String,
        businessName: String
    ): LoginOutcome = withContext(Dispatchers.IO) {
        val cleanEmail = email.lowercase().trim()
        
        // 1. Create Tenant
        val tenant = Tenant(
            id = cleanEmail,
            name = businessName.ifBlank { "CV. $cleanEmail" },
            ownerEmail = cleanEmail,
            businessMode = "BMP"
        )
        tenantDao.upsert(tenant)
        
        // 2. Create Outlet
        val outletId = outletDao.insert(
            Outlet(
                tenantId = cleanEmail,
                name = "Outlet Utama",
                isDefault = true
            )
        )
        
        // 3. Create Employee (Owner profile for login check fallback)
        val passwordHash = PinHasher.hash(password)
        val employee = Employee(
            tenantId = cleanEmail,
            outletId = outletId,
            name = "Premium Owner",
            email = cleanEmail,
            role = "OWNER",
            pinHash = passwordHash
        )
        employeeDao.insert(employee)
        
        // 4. Create LocalUser
        val user = LocalUser(
            googleSub = cleanEmail,
            email = cleanEmail,
            displayName = "Premium Owner",
            photoUrl = null,
            role = "OWNER",
            tenantId = cleanEmail
        )
        userDao.upsert(user)
        
        // 5. Seed default/clean database for this new tenant
        localDataSeeder.seedFromSqlDump(context, cleanEmail, outletId)
        
        // 6. Automatically set active session to this new tenant
        securePrefs.setActiveSession(cleanEmail, cleanEmail)
        securePrefs.currentTenantId = cleanEmail
        
        return@withContext LoginOutcome.Success(user, tenant)
    }

    suspend fun logout() {
        googleClient.signOut()
        securePrefs.wipe()
    }

    suspend fun confirmPaymentAndUpgrade(userEmail: String, confirmEmail: String): Boolean = withContext(Dispatchers.IO) {
        if (confirmEmail.lowercase().trim() != "muhammadmuizz8@gmail.com") return@withContext false

        val cleanUserEmail = userEmail.lowercase().trim()
        val user = userDao.getByEmail(cleanUserEmail) ?: return@withContext false

        // Upgrade status
        val upgradedUser = user.copy(isPremium = true)
        userDao.upsert(upgradedUser)

        // Create fresh/clean database (new tenant ID)
        val premiumTenantId = "ten_premium_${cleanUserEmail.replace(".", "_").replace("@", "_")}"
        val existingTenant = tenantDao.getById(premiumTenantId)
        if (existingTenant == null) {
            val demoTenantId = "demo_tenant_${cleanUserEmail.replace(".", "_").replace("@", "_")}"
            val demoTenant = tenantDao.getById(demoTenantId)
            val chosenMode = demoTenant?.businessMode ?: "BMP"

            val newTenant = Tenant(
                id = premiumTenantId,
                name = "CV. ${user.displayName ?: cleanUserEmail} (Premium)",
                ownerEmail = cleanUserEmail,
                businessMode = chosenMode
            )
            tenantDao.upsert(newTenant)
            outletDao.insert(
                Outlet(
                    tenantId = premiumTenantId,
                    name = "Outlet Utama",
                    isDefault = true
                )
            )
        }

        // Set active tenant to the clean database
        userDao.setTenant(user.googleSub, premiumTenantId)
        if (securePrefs.currentEmail == cleanUserEmail) {
            securePrefs.currentTenantId = premiumTenantId
        }

        return@withContext true
    }

    suspend fun fastForwardDemoTime(email: String): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.lowercase().trim()
        val user = userDao.getByEmail(cleanEmail) ?: return@withContext false
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L) - (5 * 60 * 1000L)
        userDao.upsert(user.copy(registeredAt = twoDaysAgo))
        return@withContext true
    }

    fun activeUserSub(): String? = securePrefs.currentGoogleSub
    fun activeTenantId(): String? = securePrefs.currentTenantId

    suspend fun getActiveUser(): LocalUser? = withContext(Dispatchers.IO) {
        val sub = activeUserSub() ?: return@withContext null
        userDao.getBySub(sub)
    }
}

