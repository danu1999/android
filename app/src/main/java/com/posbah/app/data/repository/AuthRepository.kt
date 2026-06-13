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
    @ApplicationContext private val context: Context
) {

    private val staticPremiumUsers = mapOf(
        "bahteramulyap@gmail.com" to Triple("8a0ff1f8926195dfde55af7e68c028591602dacc30dc3c7caef27a949ca45142b25514004cf4540c46eca830100d06517c6facc0faf77fc57140e9df5fe5ffc7", "CV Bahtera Mulya Plastik", "ten_premium_bahteramulyap_gmail_com"),
        "hanafiariful@gmail.com" to Triple("20710a82f8d6b458af10d49fbb1f985ac8aaf696e6b32e776d4f4ebbc30d08565e2bb5e1902ace18297d8db47ad35e49c086669125b1d6ac867c0d2d7e265e50", "PISANG KEJU RAMAYANA", "ten_premium_hanafiariful_gmail_com"),
        "fahrup22@gmail.com" to Triple("63e71711d1481b6da8b756e114aa2ac71a704929c0accf46f419706a5c1416ae1a312899ae84d3d8e33d255811e98fd4d17e59371a08e2f9c21c01d1b1c13a8d", "FahriP", "ten_premium_hanafiariful_gmail_com"),
        "alfarisirosi40@gmail.com" to Triple("a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c", "Mamet PKR", "ten_premium_hanafiariful_gmail_com"),
        "alfarisirosi04@gmail.com" to Triple("a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c", "Mamet PKR", "ten_premium_hanafiariful_gmail_com"),
        "syerlirahma7@gmail.com" to Triple("5819ef0d24208780b75c18009f0f69400eb933916f800ae980b778820cda595e3151de8600a0c325711f0e9641b5a72f393008868913578601ba0fa0d4c9ad93", "syerli", "ten_premium_bahteramulyap_gmail_com")
    )

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Delete duplicate tenant created under email-key
                tenantDao.deleteById("hanafiariful@gmail.com")

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
                val legacyEmailTenantIds = listOf(
                    "hanafiariful@gmail.com",
                    "bahteramulyap@gmail.com",
                    "fahrup22@gmail.com",
                    "alfarisirosi40@gmail.com",
                    "alfarisirosi04@gmail.com",
                    "syerlirahma7@gmail.com"
                )
                for (legacyId in legacyEmailTenantIds) {
                    bmpSettingsDao.deleteByTenantId(legacyId)
                    printSettingsDao.deleteByTenantId(legacyId)
                }
                
                // Delete wrong employees seeded from CV Bahtera dump
                employeeDao.deleteIncorrectEmployees(
                    emails = listOf("bahteramulyap@gmail.com", "syerlirahma7@gmail.com"),
                    allowedTenants = listOf("bahteramulyap@gmail.com", "ten_premium_bahteramulyap_gmail_com")
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


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
                val msg = when (googleResult) {
                    is com.posbah.app.auth.GoogleSignInClient.Result.Error.NoCredentials -> googleResult.message ?: "Tidak ada kredensial"
                    is com.posbah.app.auth.GoogleSignInClient.Result.Error.InvalidToken -> googleResult.reason
                    is com.posbah.app.auth.GoogleSignInClient.Result.Error.Unexpected -> googleResult.throwable.localizedMessage ?: "Kesalahan tidak terduga"
                }
                return@withContext LoginOutcome.Error("Gagal Google Sign-In: $msg")
            }
        }

        val cleanEmail = identity.email.lowercase().trim()
        val name = identity.displayName ?: cleanEmail.substringBefore("@")
        val sub = identity.sub

        val isPremiumUser = cleanEmail == "muhammadmuizz8@gmail.com" ||
                            cleanEmail == "bahteramulyap@gmail.com" ||
                            cleanEmail == "hanafiariful@gmail.com" ||
                            cleanEmail == "fahrup22@gmail.com" ||
                            cleanEmail == "alfarisirosi40@gmail.com" ||
                            cleanEmail == "alfarisirosi04@gmail.com"

        // Check if employee
        val dbEmployee = employeeDao.findByEmail(cleanEmail)
        val isStaticEmployee = cleanEmail == "fahrup22@gmail.com" ||
                               cleanEmail == "alfarisirosi40@gmail.com" ||
                               cleanEmail == "alfarisirosi04@gmail.com" ||
                               cleanEmail == "syerlirahma7@gmail.com"

        val isEmployee = dbEmployee != null || isStaticEmployee

        val userRole = if (isEmployee) {
            if (cleanEmail == "fahrup22@gmail.com" || cleanEmail == "syerlirahma7@gmail.com") "ADMIN"
            else if (cleanEmail == "alfarisirosi40@gmail.com" || cleanEmail == "alfarisirosi04@gmail.com") "KASIR"
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
            val url = java.net.URL("https://www.zedmz.cloud/api/sync/local_users?googleSub=eq.$sub")
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
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
                return@withContext LoginOutcome.Error("Akun demo kedaluwarsa. Hubungi muhammadmuizz8@gmail.com untuk aktivasi premium.")
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
                    return@withContext LoginOutcome.Error("Email ini sudah kedaluwarsa untuk akun demo. Hubungi muhammadmuizz8@gmail.com untuk aktivasi.")
                }
            }
        }

        // Determine tenantId
        val targetTenantId = if (isEmployee) {
            if (cleanEmail == "fahrup22@gmail.com" || cleanEmail == "alfarisirosi40@gmail.com" || cleanEmail == "alfarisirosi04@gmail.com") {
                "ten_premium_hanafiariful_gmail_com"
            } else if (cleanEmail == "syerlirahma7@gmail.com") {
                "ten_premium_bahteramulyap_gmail_com"
            } else {
                dbEmployee?.tenantId.orEmpty()
            }
        } else {
            // For owners
            if (cleanEmail == "hanafiariful@gmail.com") {
                "ten_premium_hanafiariful_gmail_com"
            } else if (cleanEmail == "bahteramulyap@gmail.com") {
                "ten_premium_bahteramulyap_gmail_com"
            } else {
                tenantIdFromServer ?: existing?.tenantId
            }
        }

        val targetMode = if (isEmployee) {
            if (targetTenantId?.contains("bahteramulyap") == true) "BMP" else "FNB"
        } else {
            if (cleanEmail == "hanafiariful@gmail.com") "FNB"
            else if (cleanEmail == "bahteramulyap@gmail.com") "BMP"
            else targetTenantId?.let { tenantDao.getById(it)?.businessMode }
        }

        val isPremiumFinal = isPremiumUser || isEmployee || isPremiumFromServer || (existing?.isPremium == true)

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
            updatedAt = System.currentTimeMillis()
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
                    name = if (cleanEmail == "hanafiariful@gmail.com" || isStaticEmployee && targetTenantId == "ten_premium_hanafiariful_gmail_com") "PISANG KEJU RAMAYANA"
                           else if (cleanEmail == "bahteramulyap@gmail.com" || isStaticEmployee && targetTenantId == "ten_premium_bahteramulyap_gmail_com") "CV. BAHTERA MULYA PLASTIK"
                           else if (user.isPremium) "CV. $name ($modeName)"
                           else "Demo - $name ($modeName)",
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
                    com.posbah.app.data.remote.SupabaseSyncManager.pullAll(context, db, targetTenantId)
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

        // Enforce: Demo users must use Google Login
        val dbUser = userDao.getByEmail(cleanEmail)
        if (dbUser != null && !dbUser.isPremium) {
            return@withContext LoginOutcome.Error("Akun demo wajib menggunakan Google Login. Hubungi muhammadmuizz8@gmail.com jika sudah melakukan pembayaran premium.")
        }

        val successUser: LocalUser
        val successTenant: Tenant

        val staticInfo = staticPremiumUsers[cleanEmail]
        if (staticInfo != null) {
            val (hash, displayName, tenantId) = staticInfo
            
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

            // Successfully authenticated premium/owner user
            securePrefs.failedPinAttempts = 0
            securePrefs.lockoutUntil = 0L
            
            // Ensure the premium tenant exists for this static owner/user
            val isBmpTenant = tenantId.contains("bahteramulyap")
            val mode = if (isBmpTenant) "BMP" else "FNB"
            var t = tenantDao.getById(tenantId)
            if (t == null) {
                t = Tenant(
                    id = tenantId,
                    name = if (isBmpTenant) "CV. BAHTERA MULYA PLASTIK" else "PISANG KEJU RAMAYANA",
                    ownerEmail = if (isBmpTenant) "bahteramulyap@gmail.com" else "hanafiariful@gmail.com",
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

            val userRole = if (cleanEmail == "hanafiariful@gmail.com" || cleanEmail == "bahteramulyap@gmail.com") {
                "OWNER"
            } else if (cleanEmail == "alfarisirosi40@gmail.com" || cleanEmail == "alfarisirosi04@gmail.com") {
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
                isPremium = true
            )).copy(
                role = userRole,
                tenantId = tenantId,
                isPremium = true,
                businessModeLocked = true,
                lastLoginAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.upsert(user)

            successUser = user
            successTenant = tenantDao.getById(tenantId)!!
        } else {
            // Check employees table in database
            var emp = employeeDao.findByEmail(cleanEmail)
            if (emp == null) {
                // Fetch from VPS remote database
                var conn: java.net.HttpURLConnection? = null
                try {
                    val url = java.net.URL("https://www.zedmz.cloud/api/sync/employees?email=eq.$cleanEmail")
                    conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val code = conn.responseCode
                    if (code in 200..299) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val array = org.json.JSONArray(response)
                        if (array.length() > 0) {
                            val obj = array.getJSONObject(0)
                            val tenantId = obj.getString("tenantId")
                            
                             // Let's first make sure the Tenant exists locally! If not, create it or query from VPS.
                            var tenantName = "CV. Premium Owner (Premium)"
                            var tenantBusinessMode = "BMP"
                            var tenantConn: java.net.HttpURLConnection? = null
                            try {
                                val tUrl = java.net.URL("https://www.zedmz.cloud/api/sync/tenants?id=eq.$tenantId")
                                tenantConn = tUrl.openConnection() as java.net.HttpURLConnection
                                tenantConn.requestMethod = "GET"
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

                            // Build the Employee entity
                            val fetchedEmp = Employee(
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
                            employeeDao.insert(fetchedEmp)
                            emp = fetchedEmp

                            // We should also check if the user is in local_users table locally, if not create them.
                            val existingUser = userDao.getByEmail(cleanEmail)
                            if (existingUser == null) {
                                val user = LocalUser(
                                    googleSub = obj.optString("googleSub", "emp:${fetchedEmp.id}"),
                                    email = cleanEmail,
                                    displayName = fetchedEmp.name,
                                    photoUrl = null,
                                    role = fetchedEmp.role,
                                    isPremium = true,
                                    tenantId = tenantId
                                )
                                userDao.upsert(user)
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                } finally {
                    conn?.disconnect()
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
                tenantId = emp.tenantId
            )
            userDao.upsert(user)

            successUser = user
            successTenant = tenant
        }

            securePrefs.setActiveSession(successUser.googleSub, successUser.email)
            securePrefs.currentTenantId = successUser.tenantId

            // Trigger background pull sync right after successful login
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    com.posbah.app.data.remote.SupabaseSyncManager.pullAll(context, db, successUser.tenantId.orEmpty())
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
        val t = Tenant(
            id = targetTenantId,
            name = "${businessName.ifBlank { "CV. $cleanEmail" }} (Invoice & Manufaktur)",
            ownerEmail = cleanEmail,
            businessMode = "BMP"
        )
        tenantDao.upsert(t)

        val outlets = outletDao.listForTenant(targetTenantId)
        val outletId = outlets.firstOrNull { it.isDefault }?.id ?: outlets.firstOrNull()?.id

        val passwordHash = PinHasher.hash(password)
        val employee = Employee(
            tenantId = targetTenantId,
            outletId = outletId,
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
            businessModeLocked = true
        )
        userDao.upsert(user)

        try {
            localDataSeeder.seedFromSqlDump(context, targetTenantId, outletId)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error seeding simulated payment data", e)
        }

        securePrefs.setActiveSession(cleanEmail, cleanEmail)
        securePrefs.currentTenantId = targetTenantId
        sessionState.setOutlet(outletId)

        return@withContext LoginOutcome.Success(user, t)
    }

    suspend fun logout() {
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

