package com.posbah.app.data.repository

import android.content.Context
import com.posbah.app.auth.GoogleSignInClient
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.LocalDataSeeder
import com.posbah.app.data.local.dao.EmployeeDao
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.OutletDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.data.local.dao.BmpSettingsDao
import com.posbah.app.data.local.dao.PrintSettingsDao
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.security.BackendHasher
import com.posbah.app.security.PinHasher
import com.posbah.app.BuildConfig
import com.posbah.app.data.remote.SupabaseSyncManager
import com.posbah.app.security.SecurePreferences
import com.posbah.app.data.repository.SessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    private val sessionState: SessionState,
    private val localDataSeeder: LocalDataSeeder,
    private val bmpSettingsDao: BmpSettingsDao,
    private val printSettingsDao: PrintSettingsDao,
    private val db: PosBahDatabase,
    @param:ApplicationContext private val context: Context
) {

    private val staticPremiumUsers = mapOf(
        "bahteramulyap@gmail.com" to Triple("8a0ff1f8926195dfde55af7e68c028591602dacc30dc3c7caef27a949ca45142b25514004cf4540c46eca830100d06517c6facc0faf77fc57140e9df5fe5ffc7", "CV Bahtera Mulya Plastik", "ten_premium_bahteramulyap_gmail_com"),
        "hanafiariful@gmail.com" to Triple("20710a82f8d6b458af10d49fbb1f985ac8aaf696e6b32e776d4f4ebbc30d08565e2bb5e1902ace18297d8db47ad35e49c086669125b1d6ac867c0d2d7e265e50", "PISANG KEJU RAMAYANA", "ten_premium_hanafiariful_gmail_com"),
        "fahrup22@gmail.com" to Triple("63e71711d1481b6da8b756e114aa2ac71a704929c0accf46f419706a5c1416ae1a312899ae84d3d8e33d255811e98fd4d17e59371a08e2f9c21c01d1b1c13a8d", "FahriP", "ten_premium_hanafiariful_gmail_com"),
        "alfarisirosi40@gmail.com" to Triple("a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c", "Mamet PKR", "ten_premium_hanafiariful_gmail_com"),
        "playstoretest@gmail.com" to Triple("f93226ab6fd88288603a9ea14137015f3667f84ea23e34c32fad092883b3994546a681e423e9c1d087a5ea6f7238dcf8d3b7a27b93d2315addfde043c01cbf1a", "PlayStore Test", "ten_premium_playstoretest_gmail_com")
    )

    private val staticDemoUsers = mapOf(
        "demo@posbah.com" to Triple("f93226ab6fd88288603a9ea14137015f3667f84ea23e34c32fad092883b3994546a681e423e9c1d087a5ea6f7238dcf8d3b7a27b93d2315addfde043c01cbf1a", "User Demo (Trial)", "demo_tenant_demo_posbah_com")
    )

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Delete duplicate tenant created under email-key
                tenantDao.deleteById("hanafiariful@gmail.com")

                // Clean up local duplicate owner employee (id = 1) under hanafiariful@gmail.com's tenant
                try {
                    val dupEmp = employeeDao.getById(1L)
                    if (dupEmp != null && dupEmp.tenantId == "ten_premium_hanafiariful_gmail_com") {
                        employeeDao.deleteById(1L)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Clean up other duplicate/unused tenants for hanafiariful@gmail.com and bahteramulyap@gmail.com
                val hanafiarifulTenants = tenantDao.listForOwner("hanafiariful@gmail.com")
                for (t in hanafiarifulTenants) {
                    if (t.id != "ten_premium_hanafiariful_gmail_com") {
                        tenantDao.deleteById(t.id)
                    }
                }
                val bahteramulyapTenants = tenantDao.listForOwner("bahteramulyap@gmail.com")
                for (t in bahteramulyapTenants) {
                    if (t.id != "ten_premium_bahteramulyap_gmail_com") {
                        tenantDao.deleteById(t.id)
                    }
                }

                // ── Hapus data PrintSettings & BmpSettings orphan dengan tenantId email lama ──
                // Dulu, tenantId memakai email langsung (misal "hanafiariful@gmail.com").
                // Setelah migrasi ke format "ten_premium_...", data lama perlu dibersihkan
                // agar tidak terbaca oleh akun lain yang query-nya filter by tenantId.
                // Cleanup legacy tenantId entries that used email as tenantId (pre-v2.0)
                // Note: "alfarisirosi04@gmail.com" was a TYPO of "alfarisirosi40@gmail.com" — removed from this list.
                val legacyEmailTenantIds = listOf(
                    "hanafiariful@gmail.com",
                    "bahteramulyap@gmail.com",
                    "fahrup22@gmail.com",
                    "alfarisirosi40@gmail.com",
                    "syerlirahma7@gmail.com"
                )
                for (legacyId in legacyEmailTenantIds) {
                    bmpSettingsDao.deleteByTenantId(legacyId)
                    printSettingsDao.deleteByTenantId(legacyId)
                }

                // Delete duplicate employee entry with typo email "alfarisirosi04" if it exists
                employeeDao.deleteByEmail("alfarisirosi04@gmail.com")

                // Delete wrong employees seeded from CV Bahtera dump
                employeeDao.deleteIncorrectEmployees(
                    emails = listOf("bahteramulyap@gmail.com", "syerlirahma7@gmail.com"),
                    allowedTenants = listOf("bahteramulyap@gmail.com", "ten_premium_bahteramulyap_gmail_com")
                )

                // ── Reset/Re-seed demo database to purge production data leakage ──
                val currentTenant = securePrefs.currentTenantId
                val isDemoCleaned = securePrefs.isDemoCleanedV211
                if (currentTenant != null && 
                    (currentTenant.startsWith("demo_tenant_") || currentTenant == "demo_tenant") && 
                    !isDemoCleaned) {
                    
                    android.util.Log.i("AuthRepository", "Purging and resetting demo tenant database for $currentTenant to ensure clean simulated data.")
                    
                    // 1. Wipe all local SQLite tables (Room)
                    db.clearAllTables()
                    
                    // 2. Clear prefs (except current credentials so they don't get logged out!)
                    val savedSub = securePrefs.currentGoogleSub
                    val savedEmail = securePrefs.currentEmail
                    securePrefs.wipe()
                    
                    // Restore essential credentials & tenant
                    securePrefs.setActiveSession(savedSub, savedEmail)
                    securePrefs.currentTenantId = currentTenant
                    securePrefs.isDemoCleanedV211 = true
                    
                    // 3. Re-create the LocalUser, Tenant & default Outlet in Room
                    val user = LocalUser(
                        googleSub = savedSub ?: "",
                        email = savedEmail ?: "",
                        displayName = savedEmail?.substringBefore("@") ?: "Demo User",
                        photoUrl = null,
                        role = "OWNER",
                        tenantId = currentTenant,
                        isPremium = false,
                        businessModeLocked = true,
                        registeredAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis(),
                        isActive = true
                    )
                    userDao.upsert(user)
                    val displayName = user.displayName ?: "Demo User"
                    
                    // Upsert Tenant
                    val tenant = Tenant(
                        id = currentTenant,
                        name = "Demo - $displayName",
                        ownerEmail = savedEmail ?: "",
                        businessMode = if (currentTenant.endsWith("_BMP")) "BMP" else "FNB"
                    )
                    tenantDao.upsert(tenant)
                    
                    // Insert Outlet
                    val defaultOutletId = if (!currentTenant.endsWith("_BMP")) {
                        outletDao.insert(
                            Outlet(
                                tenantId = currentTenant,
                                name = "Outlet Utama",
                                isDefault = true
                            )
                        )
                    } else null
                    
                    // 4. Re-seed clean programmatic data
                    localDataSeeder.seedFromSqlDump(context, currentTenant, defaultOutletId)
                    
                    // 5. Trigger a full upload sync to VPS so the server database is overwritten with clean data
                    // We must first request the VPS to purge our tenant's online data to avoid merging with old production records!
                    var purgeConn: java.net.HttpURLConnection? = null
                    try {
                        val url = java.net.URL("https://www.zedmz.cloud/api/admin/inspect-tenant?tenantId=${java.net.URLEncoder.encode(currentTenant, "UTF-8")}&purge=true")
                        purgeConn = url.openConnection() as java.net.HttpURLConnection
                        purgeConn.requestMethod = "POST"
                        purgeConn.setRequestProperty("Authorization", BuildConfig.ADMIN_AUTH_TOKEN)
                        purgeConn.connectTimeout = 5000
                        purgeConn.readTimeout = 5000
                        purgeConn.responseCode // execute
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        purgeConn?.disconnect()
                    }
                    
                    // Trigger syncAll
                    try {
                        com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, currentTenant)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    sealed class LoginOutcome {
        data class Success(val user: LocalUser, val tenant: Tenant) : LoginOutcome()
        data class NeedsTenantPick(val user: LocalUser, val tenants: List<Tenant>) : LoginOutcome()
        object Cancelled : LoginOutcome()
        data class Error(val message: String, val email: String? = null) : LoginOutcome()
        object Locked : LoginOutcome()
    }

    /**
     * Google SSO login - configured with simulated SSO to allow offline email testing.
     */
    suspend fun loginWithGoogle(
        activity: android.app.Activity
    ): LoginOutcome = withContext(Dispatchers.IO) {
        val googleResult = try {
            googleClient.signIn(activity)
        } catch (e: Exception) {
            return@withContext LoginOutcome.Error("Gagal otentikasi Google: ${e.localizedMessage}")
        }

        val identity = when (googleResult) {
            is com.posbah.app.auth.GoogleSignInClient.Result.Success -> googleResult.identity
            com.posbah.app.auth.GoogleSignInClient.Result.Cancelled -> return@withContext LoginOutcome.Cancelled
            is com.posbah.app.auth.GoogleSignInClient.Result.Error -> {
                val errorMsg = when (googleResult) {
                    is com.posbah.app.auth.GoogleSignInClient.Result.Error.NoCredentials -> googleResult.message ?: "No credentials found"
                    is com.posbah.app.auth.GoogleSignInClient.Result.Error.InvalidToken -> googleResult.reason
                    is com.posbah.app.auth.GoogleSignInClient.Result.Error.Unexpected -> googleResult.throwable.localizedMessage ?: "Unexpected error"
                }
                return@withContext LoginOutcome.Error("Gagal Google Sign-In: $errorMsg")
            }
        }

        val cleanEmail = identity.email.lowercase().trim()
        val name = identity.displayName ?: cleanEmail.substringBefore("@")
        val sub = identity.sub

        // Check if email is deleted
        var isDeleted = false
        var deleteStatus = ""
        var checkConn: java.net.HttpURLConnection? = null
        try {
            val checkUrl = java.net.URL("https://www.zedmz.cloud/api/auth/check-deleted?email=${java.net.URLEncoder.encode(cleanEmail, "UTF-8")}")
            checkConn = checkUrl.openConnection() as java.net.HttpURLConnection
            checkConn.requestMethod = "GET"
            checkConn.connectTimeout = 5000
            checkConn.readTimeout = 5000
            if (checkConn.responseCode in 200..299) {
                val checkRes = checkConn.inputStream.bufferedReader().use { it.readText() }
                val checkObj = org.json.JSONObject(checkRes)
                isDeleted = checkObj.optBoolean("deleted", false)
                deleteStatus = checkObj.optString("status", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            checkConn?.disconnect()
        }

        if (isDeleted) {
            if (deleteStatus == "REJOINED") {
                // Approved to rejoin! Clear local databases and prefs to force "data 0"
                try {
                    db.clearAllTables()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                securePrefs.wipe()

                // Call complete-rejoin to mark user as active (not deleted anymore)
                var completeConn: java.net.HttpURLConnection? = null
                try {
                    val completeUrl = java.net.URL("https://www.zedmz.cloud/api/auth/complete-rejoin?email=${java.net.URLEncoder.encode(cleanEmail, "UTF-8")}")
                    completeConn = completeUrl.openConnection() as java.net.HttpURLConnection
                    completeConn.requestMethod = "POST"
                    completeConn.connectTimeout = 5000
                    completeConn.readTimeout = 5000
                    completeConn.responseCode // execute request
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    completeConn?.disconnect()
                }
            } else {
                // Still blocked (status is DELETED, PENDING_USER_CONFIRM, or PENDING_ADMIN_APPROVE)
                return@withContext LoginOutcome.Error("Gagal login karena database tidak ada. Silakan hubungi admin.", cleanEmail)
            }
        }

        val isPremiumUser = cleanEmail == "muhammadmuizz8@gmail.com" ||
                            cleanEmail == "bahteramulyap@gmail.com" ||
                            cleanEmail == "hanafiariful@gmail.com" ||
                            cleanEmail == "fahrup22@gmail.com" ||
                            cleanEmail == "alfarisirosi40@gmail.com" ||
                            cleanEmail == "mulyakus84@gmail.com"

        // Check if employee
        val dbEmployee = employeeDao.findByEmail(cleanEmail)
        val isStaticEmployee = cleanEmail == "fahrup22@gmail.com" ||
                               cleanEmail == "alfarisirosi40@gmail.com"

        val isEmployee = dbEmployee != null || isStaticEmployee

        val userRole = if (isEmployee) {
            if (cleanEmail == "fahrup22@gmail.com") "ADMIN"
            else if (cleanEmail == "alfarisirosi40@gmail.com") "KASIR"
            else dbEmployee?.role ?: "KASIR"
        } else {
            "OWNER"
        }

        // Fetch user status from VPS to support multi-device consistency
        var isPremiumFromServer = false
        var isActiveFromServer = true
        var tenantIdFromServer: String? = null
        var businessModeLockedFromServer = false
        var registeredAtFromServer = System.currentTimeMillis()

        var conn: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL("https://www.zedmz.cloud/api/sync/local_users?email=eq.${java.net.URLEncoder.encode(cleanEmail, "UTF-8")}")
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode in 200..299) {
                val res = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = org.json.JSONArray(res)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    isPremiumFromServer = obj.optBoolean("isPremium", false)
                    isActiveFromServer = obj.optBoolean("isActive", true)
                    if (obj.has("tenantId") && !obj.isNull("tenantId")) {
                        tenantIdFromServer = obj.getString("tenantId")
                    }
                    businessModeLockedFromServer = obj.optBoolean("businessModeLocked", false)
                    registeredAtFromServer = obj.optLong("registeredAt", System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }

        if (!isActiveFromServer) {
            return@withContext LoginOutcome.Error("Akun Anda diblokir secara permanen. Hubungi muhammadmuizz8@gmail.com.")
        }

        val existing = userDao.getBySub(sub)

        // Rule: If they are blocked/inactive locally, they cannot login
        if (existing != null && !existing.isActive) {
            return@withContext LoginOutcome.Error("Akun Anda diblokir secara permanen. Hubungi muhammadmuizz8@gmail.com.")
        }

        // Rule: If they do not pay for 2 days since they created the demouser, they cannot login
        if (existing != null && !existing.isPremium && !isPremiumUser && !isEmployee && !isPremiumFromServer) {
            val elapsed = System.currentTimeMillis() - existing.registeredAt
            if (elapsed > 2 * 24 * 60 * 60 * 1000L) {
                if (cleanEmail.endsWith("@posbah.com")) {
                    // Reset registration date for simulated demo user so it never expires!
                    userDao.upsert(existing.copy(registeredAt = System.currentTimeMillis()))
                } else {
                    return@withContext LoginOutcome.Error("Akun demo kedaluwarsa. Hubungi muhammadmuizz8@gmail.com untuk aktivasi premium.")
                }
            }
        }

        // Rule: Prevent creating a new demouser with an expired or blocked email
        if (existing == null && !isPremiumUser && !isEmployee && !isPremiumFromServer) {
            val previousUser = userDao.getByEmail(cleanEmail)
            if (previousUser != null) {
                if (!previousUser.isActive) {
                    return@withContext LoginOutcome.Error("Email ini sudah diblokir secara permanen. Hubungi muhammadmuizz8@gmail.com.")
                }
                val elapsed = System.currentTimeMillis() - previousUser.registeredAt
                if (elapsed > 2 * 24 * 60 * 60 * 1000L) {
                    if (cleanEmail.endsWith("@posbah.com")) {
                        // Reset registration date for simulated demo user so it never expires!
                        userDao.upsert(previousUser.copy(registeredAt = System.currentTimeMillis()))
                    } else {
                        return@withContext LoginOutcome.Error("Email ini sudah kedaluwarsa untuk akun demo. Hubungi muhammadmuizz8@gmail.com untuk aktivasi.")
                    }
                }
            }
        }

        // Determine tenantId
        val targetTenantId = if (isEmployee) {
            if (cleanEmail == "fahrup22@gmail.com" || cleanEmail == "alfarisirosi40@gmail.com") {
                "ten_premium_hanafiariful_gmail_com"
            } else {
                dbEmployee?.tenantId.orEmpty()
            }
        } else {
            // For owners
            when (cleanEmail) {
                "hanafiariful@gmail.com" -> "ten_premium_hanafiariful_gmail_com"
                "bahteramulyap@gmail.com" -> "ten_premium_bahteramulyap_gmail_com"
                else -> tenantIdFromServer ?: existing?.tenantId
            }
        }

        val lastTenant = securePrefs.lastActiveTenantId ?: securePrefs.currentTenantId
        if (targetTenantId != null && lastTenant != null && targetTenantId != lastTenant) {
            try {
                db.clearAllTables()
                android.util.Log.i("AuthRepository", "Cleared all tables in loginWithGoogle due to tenant switch: $lastTenant -> $targetTenantId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (targetTenantId != null) {
            securePrefs.lastActiveTenantId = targetTenantId
        }

        val targetMode = if (isEmployee) {
            if (targetTenantId?.contains("bahteramulyap") == true) "BMP" else "FNB"
        } else {
            when (cleanEmail) {
                "hanafiariful@gmail.com" -> "FNB"
                "bahteramulyap@gmail.com" -> "BMP"
                else -> targetTenantId?.let { tenantDao.getById(it)?.businessMode }
            }
        }

        val isPremiumFinal = isPremiumUser || isEmployee || isPremiumFromServer || (existing?.isPremium == true)

        if (isPremiumFinal) {
            return@withContext LoginOutcome.Error("Email Anda terdaftar sebagai akun Premium. Silakan masuk melalui tab Premium (Email) menggunakan Email dan Password/PIN.")
        }

        val businessModeLocked = if (isEmployee) {
            true
        } else {
            if (cleanEmail == "hanafiariful@gmail.com" || cleanEmail == "bahteramulyap@gmail.com") true
            else if (isPremiumFinal) false
            else (businessModeLockedFromServer || (existing?.businessModeLocked ?: false))
        }

        val user = (existing ?: LocalUser(
            googleSub = sub,
            email = cleanEmail,
            displayName = name,
            photoUrl = null,
            role = userRole,
            isPremium = isPremiumFinal,
            registeredAt = registeredAtFromServer
        )).copy(
            email = cleanEmail,
            displayName = name,
            role = userRole,
            tenantId = targetTenantId,
            businessModeLocked = businessModeLocked,
            isPremium = isPremiumFinal,
            lastLoginAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            apkVersion = BuildConfig.VERSION_NAME
        )
        userDao.upsert(user)

        // Initialize tenant if locked and targetTenantId is not null
        if (targetTenantId != null) {
            var tenant = tenantDao.getById(targetTenantId)
            val finalMode = targetMode ?: "FNB"
            if (tenant == null) {
                val modeName = when (finalMode) {
                    "FNB" -> "FnB"
                    "RENTAL" -> "Rental"
                    "LAUNDRY" -> "Laundry"
                    else -> "Invoice & Manufaktur"
                }
                tenant = Tenant(
                    id = targetTenantId,
                    name = when {
                        cleanEmail == "hanafiariful@gmail.com" || isStaticEmployee && targetTenantId == "ten_premium_hanafiariful_gmail_com" -> "PISANG KEJU RAMAYANA"
                        cleanEmail == "bahteramulyap@gmail.com" || isStaticEmployee && targetTenantId == "ten_premium_bahteramulyap_gmail_com" -> "CV. BAHTERA MULYA PLASTIK"
                        user.isPremium -> "CV. $name ($modeName)"
                        else -> "Demo - $name ($modeName)"
                    },
                    ownerEmail = if (isEmployee) {
                        if (cleanEmail.contains("fahrup") || cleanEmail.contains("alfarisi")) "hanafiariful@gmail.com"
                        else if (cleanEmail.contains("syerli")) "bahteramulyap@gmail.com"
                        else dbEmployee?.tenantId?.substringAfter("ten_premium_")?.substringBeforeLast("_")?.replace("_", ".") ?: "hanafiariful@gmail.com"
                    } else cleanEmail,
                    businessMode = finalMode
                )
                tenantDao.upsert(tenant)
                if (finalMode != "BMP") {
                    outletDao.insert(
                        Outlet(
                            tenantId = targetTenantId,
                            name = "Outlet Utama",
                            isDefault = true
                        )
                    )
                }
            } else if (user.isPremium && tenant.name.startsWith("Demo - ")) {
                val newName = tenant.name.removePrefix("Demo - ")
                tenant = tenant.copy(name = newName, updatedAt = System.currentTimeMillis())
                tenantDao.upsert(tenant)
            }

            // Seed mock data if FNB mode
            if (finalMode == "FNB") {
                try {
                    val defaultOutlet = outletDao.listForTenant(targetTenantId).firstOrNull { it.isDefault }
                    localDataSeeder.seedFromSqlDump(context, targetTenantId, defaultOutlet?.id)
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Error seeding google login data for mode FNB", e)
                }
            }
            securePrefs.setActiveSession(sub, cleanEmail)
            securePrefs.currentTenantId = targetTenantId
            val outlets = outletDao.listForTenant(targetTenantId)
            val activeOutlet = outlets.firstOrNull { it.isDefault } ?: outlets.firstOrNull()
            securePrefs.currentOutletId = activeOutlet?.id
            // Trigger background pull sync right after successful login
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val cleanEmail = user.email.lowercase().trim()
                    if (user.role == "OWNER") {
                        SupabaseSyncManager.fetchAndInsertOwnerTenants(context, db, cleanEmail)
                    }
                    SupabaseSyncManager.pullAll(context, db, targetTenantId, cleanEmail)
                    SupabaseSyncManager.syncAll(context, db, targetTenantId, cleanEmail)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@withContext LoginOutcome.Success(user, tenant)
        } else {
            // New user, not locked yet. Will select system mode in SystemSelectionScreen
            val dummyTenant = Tenant(
                id = "dummy_tenant",
                name = "Needs Selection",
                ownerEmail = cleanEmail,
                businessMode = "FNB"
            )
            securePrefs.setActiveSession(sub, cleanEmail)
            securePrefs.currentTenantId = null
            securePrefs.currentOutletId = null
            return@withContext LoginOutcome.Success(user, dummyTenant)
        }
    }

    /**
     * Local email and password authentication (no external network connections).
     */
    suspend fun loginWithEmailPassword(
        email: String,
        password: String
    ): LoginOutcome = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
        if (securePrefs.lockoutUntil > now) return@withContext LoginOutcome.Locked

        val cleanEmail = email.lowercase().trim()

        // Sync local user's premium status from the server if they are currently marked as demo locally
        var dbUser = userDao.getByEmail(cleanEmail)
        if (dbUser != null && !dbUser.isPremium) {
            var conn: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://www.zedmz.cloud/api/sync/local_users?email=eq.${java.net.URLEncoder.encode(cleanEmail, "UTF-8")}")
                conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = org.json.JSONArray(response)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val isPremiumOnServer = obj.optBoolean("isPremium", false)
                        if (isPremiumOnServer) {
                            val serverTenantId = obj.optString("tenantId", "")

                            // INTERCONNECT AUTOMATION: Wipe local Room database of all demo tables
                            try {
                                db.clearAllTables()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // Re-insert the updated user, tenant, and default outlet
                            val updatedUser = LocalUser(
                                googleSub = obj.optString("googleSub", cleanEmail),
                                email = cleanEmail,
                                displayName = obj.optString("displayName", "Owner Premium"),
                                photoUrl = if (obj.isNull("photoUrl")) null else obj.optString("photoUrl"),
                                role = obj.optString("role", "OWNER"),
                                tenantId = serverTenantId,
                                isPremium = true,
                                businessModeLocked = true,
                                registeredAt = obj.optLong("registeredAt", System.currentTimeMillis()),
                                updatedAt = System.currentTimeMillis(),
                                lastLoginAt = System.currentTimeMillis(),
                                apkVersion = obj.optString("apkVersion", BuildConfig.VERSION_NAME)
                            )
                            userDao.upsert(updatedUser)
                            dbUser = userDao.getByEmail(cleanEmail)

                            // Ensure tenant exists locally
                            if (serverTenantId.isNotEmpty()) {
                                var tenantName = "CV. Premium Owner (Premium)"
                                var tenantBusinessMode = "BMP"
                                var tenantConn: java.net.HttpURLConnection? = null
                                try {
                                    val tUrl = java.net.URL("https://www.zedmz.cloud/api/sync/tenants?id=eq.$serverTenantId")
                                    tenantConn = tUrl.openConnection() as java.net.HttpURLConnection
                                    tenantConn.requestMethod = "GET"
                                    tenantConn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                                    if (tenantConn.responseCode in 200..299) {
                                        val tResponse = tenantConn.inputStream.bufferedReader().use { it.readText() }
                                        val tArray = org.json.JSONArray(tResponse)
                                        if (tArray.length() > 0) {
                                            val tObj = tArray.getJSONObject(0)
                                            tenantName = tObj.getString("name")
                                            tenantBusinessMode = tObj.getString("businessMode")
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    tenantConn?.disconnect()
                                }
                                val tenant = Tenant(
                                    id = serverTenantId,
                                    name = tenantName,
                                    ownerEmail = cleanEmail,
                                    businessMode = tenantBusinessMode
                                )
                                tenantDao.upsert(tenant)

                                // Insert fresh default outlet
                                outletDao.insert(
                                    Outlet(
                                        tenantId = serverTenantId,
                                        name = "Outlet Utama",
                                        isDefault = true
                                    )
                                )

                                // Seed default configurations for premium user
                                localDataSeeder.seedDefaultSettings(serverTenantId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }
        }

        // Enforce: Demo users must use Google Login (except special static demo bypass users)
        if (dbUser != null && !dbUser.isPremium && cleanEmail != "demo@posbah.com") {
            return@withContext LoginOutcome.Error("Akun demo wajib menggunakan Google Login. Hubungi muhammadmuizz8@gmail.com jika sudah melakukan pembayaran premium.")
        }

        val successUser: LocalUser
        val successTenant: Tenant

        val staticInfo = staticPremiumUsers[cleanEmail]
        val staticDemoInfo = staticDemoUsers[cleanEmail]
        if (staticInfo != null || staticDemoInfo != null) {
            val isPremiumUser = staticInfo != null
            val (hash, displayName, tenantId) = staticInfo ?: staticDemoInfo!!

            val lastTenant = securePrefs.lastActiveTenantId ?: securePrefs.currentTenantId
            if (!tenantId.isNullOrBlank() && !lastTenant.isNullOrBlank() && tenantId != lastTenant) {
                try {
                    db.clearAllTables()
                    android.util.Log.i("AuthRepository", "Cleared all tables in loginWithEmailPassword for static user due to tenant switch: $lastTenant -> $tenantId")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            securePrefs.lastActiveTenantId = tenantId
            
            // Check if there is an employee record in DB (e.g. they changed password)
            val dbEmp = employeeDao.findByEmail(cleanEmail)
            val isPasswordValid = if (dbEmp != null) {
                if (dbEmp.pinHash.startsWith("v1$")) {
                    PinHasher.verify(password, dbEmp.pinHash)
                } else {
                    BackendHasher.verify(password, dbEmp.pinHash)
                }
            } else {
                BackendHasher.verify(password, hash)
            }
            
            if (!isPasswordValid) {
                return@withContext incrementFailedAttempts(now)
            }

            // Successfully authenticated static user
            securePrefs.failedPinAttempts = 0
            securePrefs.lockoutUntil = 0L
            
            // Ensure the tenant exists for this static user
            val isBmpTenant = tenantId.contains("bahteramulyap")
            val mode = if (isBmpTenant) "BMP" else "FNB"
            var t = tenantDao.getById(tenantId)
            if (t == null) {
                t = Tenant(
                    id = tenantId,
                    name = if (isBmpTenant) "CV. BAHTERA MULYA PLASTIK" else if (tenantId.contains("demo")) "User Demo (Trial)" else "PISANG KEJU RAMAYANA",
                    ownerEmail = if (isBmpTenant) "bahteramulyap@gmail.com" else if (tenantId.contains("demo")) "demo@posbah.com" else "hanafiariful@gmail.com",
                    businessMode = mode
                )
                tenantDao.upsert(t)
                if (mode != "BMP") {
                    outletDao.insert(
                        Outlet(
                            tenantId = tenantId,
                            name = "Outlet Utama",
                            isDefault = true
                        )
                    )
                }
            }

            // Seed tenant database from SQL assets if empty
            val outlets = outletDao.listForTenant(tenantId)
            val outletId = outlets.firstOrNull { it.isDefault }?.id ?: outlets.firstOrNull()?.id
            try {
                localDataSeeder.seedFromSqlDump(context, tenantId, outletId)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error seeding email/pin login data", e)
            }

            val userRole = if (cleanEmail == "hanafiariful@gmail.com" || cleanEmail == "bahteramulyap@gmail.com" || cleanEmail == "demo@posbah.com") {
                "OWNER"
            } else if (cleanEmail == "alfarisirosi40@gmail.com") {
                "KASIR"
            } else {
                "ADMIN"
            }

            val existing = userDao.getBySub(cleanEmail)
            val user = (existing ?: LocalUser(
                googleSub = cleanEmail,
                email = cleanEmail,
                displayName = displayName,
                photoUrl = null,
                role = userRole,
                isPremium = isPremiumUser
            )).copy(
                role = userRole,
                tenantId = tenantId,
                isPremium = isPremiumUser,
                businessModeLocked = true,
                lastLoginAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                apkVersion = com.posbah.app.BuildConfig.VERSION_NAME
            )
            userDao.upsert(user)

            successUser = user
            successTenant = tenantDao.getById(tenantId)!!

            // Set outlet session untuk static premium users
            val staticOutlets = outletDao.listForTenant(tenantId)
            val staticDefaultOutlet = staticOutlets.firstOrNull { it.isDefault } ?: staticOutlets.firstOrNull()
            if (userRole == "OWNER") {
                // Owner bebas ganti outlet, hapus lock karyawan
                sessionState.clearEmployeeLock()
                securePrefs.currentOutletId = staticDefaultOutlet?.id
            } else {
                // Karyawan: kunci ke outletId mereka
                val empRecord = employeeDao.findByEmail(cleanEmail)
                val lockedOutletId = empRecord?.outletId ?: staticDefaultOutlet?.id
                sessionState.setEmployeeOutletLock(lockedOutletId)
                securePrefs.currentOutletId = lockedOutletId
            }
        } else {
            // Check if we can fetch the latest employee details from the server (if online)
            var fetchedEmp: Employee? = null
            var conn: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://www.zedmz.cloud/api/sync/employees?email=eq.$cleanEmail")
                conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = org.json.JSONArray(response)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val tenantId = obj.getString("tenantId")

                        // Tenant switch detection
                        val lastTenant = securePrefs.lastActiveTenantId ?: securePrefs.currentTenantId
                        if (!tenantId.isNullOrBlank() && !lastTenant.isNullOrBlank() && tenantId != lastTenant) {
                            try {
                                db.clearAllTables()
                                android.util.Log.i("AuthRepository", "Cleared all tables in loginWithEmailPassword due to tenant switch: $lastTenant -> $tenantId")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        // Check if transitioned from demo to premium
                        val localEmp = employeeDao.findByEmail(cleanEmail)
                        val transitioned = localEmp != null && 
                                           localEmp.tenantId.startsWith("demo_tenant_") && 
                                           tenantId.startsWith("ten_premium_")
                                           
                        if (transitioned) {
                            // Clear all local SQLite tables (Room)
                            try {
                                db.clearAllTables()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        // Let's first make sure the Tenant exists locally! If not, create it or query from VPS.
                        var tenantName = "CV. Premium Owner (Premium)"
                        var tenantBusinessMode = "BMP"
                        var tenantConn: java.net.HttpURLConnection? = null
                        try {
                            val tUrl = java.net.URL("https://www.zedmz.cloud/api/sync/tenants?id=eq.$tenantId")
                            tenantConn = tUrl.openConnection() as java.net.HttpURLConnection
                            tenantConn.requestMethod = "GET"
                            tenantConn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                            if (tenantConn.responseCode in 200..299) {
                                val tResponse = tenantConn.inputStream.bufferedReader().use { it.readText() }
                                val tArray = org.json.JSONArray(tResponse)
                                if (tArray.length() > 0) {
                                    val tObj = tArray.getJSONObject(0)
                                    tenantName = tObj.getString("name")
                                    tenantBusinessMode = tObj.getString("businessMode")
                                }
                            }
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        } finally {
                            tenantConn?.disconnect()
                        }

                        // Upsert the tenant locally
                        val tenant = Tenant(
                            id = tenantId,
                            name = tenantName,
                            ownerEmail = cleanEmail,
                            businessMode = tenantBusinessMode
                        )
                        tenantDao.upsert(tenant)

                        // Ensure an outlet exists locally for this tenant
                        val outlets = outletDao.listForTenant(tenantId)
                        val serverOutletId = if (obj.isNull("outletId")) null else obj.optLong("outletId")
                        val outletId = if (outlets.isEmpty()) {
                            if (serverOutletId != null && serverOutletId > 0) {
                                outletDao.insert(
                                    Outlet(
                                        id = serverOutletId,
                                        tenantId = tenantId,
                                        name = "Outlet Utama",
                                        isDefault = true
                                    )
                                )
                                serverOutletId
                            } else {
                                outletDao.insert(
                                    Outlet(
                                        tenantId = tenantId,
                                        name = "Outlet Utama",
                                        isDefault = true
                                    )
                                )
                            }
                        } else {
                            if (serverOutletId != null && outlets.any { it.id == serverOutletId }) {
                                serverOutletId
                            } else {
                                outlets.first().id
                            }
                        }

                        val fetchedEmpEntity = Employee(
                            id = obj.getLong("id"),
                            tenantId = tenantId,
                            outletId = outletId,
                            name = obj.getString("name"),
                            email = cleanEmail,
                            role = obj.getString("role"),
                            pinHash = obj.getString("pinHash"),
                            salary = obj.optDouble("salary", 0.0),
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                        // Save to local database
                        employeeDao.insert(fetchedEmpEntity)
                        if (fetchedEmpEntity.isActive) {
                            fetchedEmp = fetchedEmpEntity

                            // Update/Upsert the local user record as premium
                            val existingUser = userDao.getByEmail(cleanEmail)
                            val updatedUser = (existingUser ?: LocalUser(
                                googleSub = obj.optString("googleSub", "emp:${fetchedEmpEntity.id}"),
                                email = cleanEmail,
                                displayName = fetchedEmpEntity.name,
                                photoUrl = null,
                                role = fetchedEmpEntity.role
                            )).copy(
                                isPremium = tenantId.startsWith("ten_premium_"),
                                businessModeLocked = true,
                                tenantId = tenantId
                            )
                            userDao.upsert(updatedUser)

                            // Seed default configurations/settings for this tenant
                            localDataSeeder.seedDefaultSettings(tenantId)
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }

            var emp = fetchedEmp
            if (emp == null) {
                // Offline fallback: check local employees table in database
                emp = employeeDao.findByEmail(cleanEmail)
                if (emp != null && !emp.isActive) {
                    emp = null
                }
            }

            if (emp == null) {
                // Query VPS local_users table to see if they are an unactivated demo user
                var isDemo = false
                var conn2: java.net.HttpURLConnection? = null
                try {
                    val url2 = java.net.URL("https://www.zedmz.cloud/api/sync/local_users?email=eq.$cleanEmail")
                    conn2 = url2.openConnection() as java.net.HttpURLConnection
                    conn2.requestMethod = "GET"
                    conn2.setRequestProperty("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                    if (conn2.responseCode in 200..299) {
                        val res = conn2.inputStream.bufferedReader().use { it.readText() }
                        val arr = org.json.JSONArray(res)
                        if (arr.length() > 0) {
                            val uObj = arr.getJSONObject(0)
                            if (!uObj.optBoolean("isPremium", false)) {
                                isDemo = true
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                } finally {
                    conn2?.disconnect()
                }

                if (isDemo) {
                    return@withContext LoginOutcome.Error("Akun demo wajib menggunakan Google Login. Hubungi muhammadmuizz8@gmail.com jika sudah melakukan pembayaran premium.")
                }
                return@withContext LoginOutcome.Error("Email atau password tidak ditemukan")
            }

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
                tenantId = emp.tenantId,
                isPremium = emp.tenantId.startsWith("ten_premium_"),
                businessModeLocked = true
            )
            userDao.upsert(user)

            successUser = user
            successTenant = tenant
        }

            // Enforce outlet lock untuk karyawan non-OWNER
            val tenantIdForOutlet = successUser.tenantId.orEmpty()
            val allOutlets = outletDao.listForTenant(tenantIdForOutlet)
            val defaultOutlet = allOutlets.firstOrNull { it.isDefault } ?: allOutlets.firstOrNull()
            if (successUser.role == "OWNER") {
                // Owner bebas ganti outlet
                sessionState.clearEmployeeLock()
                securePrefs.currentOutletId = defaultOutlet?.id
            } else {
                // Karyawan (ADMIN/KASIR): kunci ke outlet yang ditetapkan
                val emp = employeeDao.findByEmail(successUser.email.lowercase().trim())
                val lockedOutletId = emp?.outletId ?: defaultOutlet?.id
                sessionState.setEmployeeOutletLock(lockedOutletId)
                securePrefs.currentOutletId = lockedOutletId
            }

            if (successUser.isPremium) {
                securePrefs.tempPlainPassword = password
            }
            if (successUser.tenantId != null) {
                securePrefs.lastActiveTenantId = successUser.tenantId
            }
            securePrefs.setActiveSession(successUser.googleSub, successUser.email)
            securePrefs.currentTenantId = successUser.tenantId

                            // Seed default configurations if they do not exist
            localDataSeeder.seedDefaultSettings(successUser.tenantId.orEmpty())

            // Trigger background pull sync right after successful login
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val cleanEmail = successUser.email.lowercase().trim()
                    if (successUser.role == "OWNER") {
                        SupabaseSyncManager.fetchAndInsertOwnerTenants(context, db, cleanEmail)
                    }
                    SupabaseSyncManager.pullAll(context, db, successUser.tenantId.orEmpty(), cleanEmail)
                    SupabaseSyncManager.syncAll(context, db, successUser.tenantId.orEmpty(), cleanEmail)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@withContext LoginOutcome.Success(successUser, successTenant)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error in loginWithEmailPassword", e)
            return@withContext LoginOutcome.Error("Gagal masuk: ${e.localizedMessage}")
        }
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


    suspend fun selectTenant(googleSub: String, tenantId: String): Boolean {
        val user = userDao.getBySub(googleSub) ?: return false
        val email = user.email.lowercase().trim()
        if (email == "hanafiariful@gmail.com" && tenantId != "ten_premium_hanafiariful_gmail_com") {
            return false
        }
        if (email == "bahteramulyap@gmail.com" && tenantId != "ten_premium_bahteramulyap_gmail_com") {
            return false
        }
        val tenant = tenantDao.getById(tenantId) ?: return false
        if (user.role == "OWNER") {
            if (tenant.ownerEmail.lowercase().trim() != email) {
                return false
            }
        } else {
            if (tenant.id != user.tenantId) {
                return false
            }
        }
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
        val emailKey = cleanEmail.replace(".", "_").replace("@", "_")

        val targetTenantId = "ten_premium_${emailKey}_BMP"

        // Wipe local database of all demo tables before transition
        try {
            db.clearAllTables()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val t = Tenant(
            id = targetTenantId,
            name = "${businessName.ifBlank { "CV. $cleanEmail" }} (Invoice & Manufaktur)",
            ownerEmail = cleanEmail,
            businessMode = "BMP"
        )
        tenantDao.upsert(t)

        // Insert fresh default outlet
        val defaultOutletId = outletDao.insert(
            Outlet(
                tenantId = targetTenantId,
                name = "Outlet Utama",
                isDefault = true
            )
        )

        val passwordHash = PinHasher.hash(password)
        val employee = Employee(
            tenantId = targetTenantId,
            outletId = defaultOutletId,
            name = "Premium Owner",
            email = cleanEmail,
            role = "OWNER",
            pinHash = passwordHash
        )
        employeeDao.insert(employee)

        val user = LocalUser(
            googleSub = cleanEmail,
            email = cleanEmail,
            displayName = "Premium Owner",
            photoUrl = null,
            role = "OWNER",
            tenantId = targetTenantId,
            isPremium = true,
            businessModeLocked = true,
            apkVersion = com.posbah.app.BuildConfig.VERSION_NAME
        )
        userDao.upsert(user)

        // Seed default configurations
        localDataSeeder.seedDefaultSettings(targetTenantId)

        try {
            localDataSeeder.seedFromSqlDump(context, targetTenantId, defaultOutletId)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error seeding simulated payment data", e)
        }

        securePrefs.setActiveSession(cleanEmail, cleanEmail)
        securePrefs.currentTenantId = targetTenantId
        sessionState.setOutlet(defaultOutletId)

        return@withContext LoginOutcome.Success(user, t)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val tenantId = securePrefs.currentTenantId
        val email = securePrefs.currentEmail
        
        if (!tenantId.isNullOrBlank() && !email.isNullOrBlank()) {
            try {
                // Ensure the sync task runs completely under NonCancellable block
                withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.withTimeoutOrNull(10000) {
                        com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId, email)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Sync failed during logout", e)
            }
        }
        
        googleClient.signOut()
        securePrefs.wipe()
    }

    fun activeUserSub(): String? = securePrefs.currentGoogleSub
    fun activeUserEmail(): String? = securePrefs.currentEmail
    fun activeTenantId(): String? = securePrefs.currentTenantId

    suspend fun getActiveUser(): LocalUser? = withContext(Dispatchers.IO) {
        val sub = activeUserSub() ?: return@withContext null
        userDao.getBySub(sub)
    }

    sealed class ChangePasswordResult {
        object Success : ChangePasswordResult()
        data class Error(val message: String) : ChangePasswordResult()
    }

    suspend fun changePassword(oldPin: String, newPin: String): ChangePasswordResult = withContext(Dispatchers.IO) {
        val email = activeUserEmail() ?: return@withContext ChangePasswordResult.Error("Sesi tidak valid")
        val cleanEmail = email.lowercase().trim()
        val user = getActiveUser() ?: return@withContext ChangePasswordResult.Error("User tidak ditemukan")

        // 1. Find employee in database
        val emp = employeeDao.findByEmail(cleanEmail)
        
        // 2. Verify current password
        val isOldPasswordValid = if (emp != null) {
            if (emp.pinHash.startsWith("v1$")) {
                PinHasher.verify(oldPin, emp.pinHash)
            } else {
                BackendHasher.verify(oldPin, emp.pinHash)
            }
        } else {
            // Check static map
            val staticInfo = staticPremiumUsers[cleanEmail]
            if (staticInfo != null) {
                val (hash, _, _) = staticInfo
                BackendHasher.verify(oldPin, hash)
            } else {
                false
            }
        }

        if (!isOldPasswordValid) {
            return@withContext ChangePasswordResult.Error("Password lama salah")
        }

        val now = System.currentTimeMillis()
        
        // Helper to check if same calendar day
        fun isSameDay(t1: Long, t2: Long): Boolean {
            val cal1 = java.util.Calendar.getInstance()
            cal1.timeInMillis = t1
            val cal2 = java.util.Calendar.getInstance()
            cal2.timeInMillis = t2
            return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                   cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
        }

        // 3. Enforce rate limit (max 2 per day)
        if (emp != null) {
            val sameDay = isSameDay(emp.lastPasswordChangeDate, now)
            val currentCount = if (sameDay) emp.passwordChangeCount else 0
            if (currentCount >= 2) {
                return@withContext ChangePasswordResult.Error("Maksimal ganti password 2 kali per hari")
            }
        }

        // 4. Update password
        val hashedNewPin = PinHasher.hash(newPin)
        if (emp != null) {
            val sameDay = isSameDay(emp.lastPasswordChangeDate, now)
            val nextCount = if (sameDay) emp.passwordChangeCount + 1 else 1
            val updated = emp.copy(
                pinHash = hashedNewPin,
                passwordChangeCount = nextCount,
                lastPasswordChangeDate = now,
                updatedAt = now
            )
            employeeDao.update(updated)
        } else {
            // Static user / Owner does not have a DB record in employees, so we create one!
            val targetTenantId = activeTenantId() ?: "ten_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            
            // Check if tenant exists, if not create a default one
            var tenant = tenantDao.getById(targetTenantId)
            if (tenant == null) {
                tenant = Tenant(
                    id = targetTenantId,
                    name = user.displayName?.let { "$it - BMP" } ?: "Tenant Baru",
                    ownerEmail = cleanEmail
                )
                tenantDao.upsert(tenant)
                outletDao.insert(
                    Outlet(
                        tenantId = targetTenantId,
                        name = "Outlet Utama",
                        isDefault = true
                    )
                )
            }
            
            val outlets = outletDao.listForTenant(targetTenantId)
            val outletId = outlets.firstOrNull { it.isDefault }?.id ?: outlets.firstOrNull()?.id

            val newEmployee = Employee(
                tenantId = targetTenantId,
                outletId = outletId,
                name = user.displayName ?: "Owner",
                email = cleanEmail,
                role = user.role, // e.g. "OWNER"
                pinHash = hashedNewPin,
                passwordChangeCount = 1,
                lastPasswordChangeDate = now,
                createdAt = now,
                updatedAt = now,
                emailVerified = true
            )
            employeeDao.insert(newEmployee)
        }

        ChangePasswordResult.Success
    }
}

