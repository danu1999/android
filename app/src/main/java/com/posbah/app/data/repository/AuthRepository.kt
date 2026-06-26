package com.posbah.app.data.repository

import android.content.Context
import com.posbah.app.auth.GoogleSignInClient
import com.posbah.app.BuildConfig
import com.posbah.app.security.BackendHasher
import com.posbah.app.security.PinHasher
import com.posbah.app.security.SecurePreferences
import com.posbah.app.data.repository.SessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// AuthRepository.kt — Full Online mode
// Menggantikan semua Room DAO dengan:
//   • VPS API calls (HttpURLConnection untuk auth endpoint)
//   • SecurePreferences untuk session management
// Tidak ada PosBahDatabase, tidak ada LocalDataSeeder, tidak ada SupabaseSyncManager.
// ─────────────────────────────────────────────────────────────────────────────

// Data class menggantikan Room entity untuk session
data class UserSession(
    val googleSub: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val role: String = "OWNER",
    val tenantId: String? = null,
    val businessMode: String? = null,
    val isPremium: Boolean = false,
    val businessModeLocked: Boolean = false,
    val apkVersion: String = "",
    val whatsapp: String? = null
)

data class TenantSession(
    val id: String,
    val name: String,
    val ownerEmail: String,
    val businessMode: String = "FNB"
)

@Singleton
class AuthRepository @Inject constructor(
    private val googleClient: GoogleSignInClient,
    private val securePrefs: SecurePreferences,
    private val sessionState: SessionState,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val BASE_URL = "https://www.zedmz.cloud"
        private const val CONNECT_TIMEOUT = 8000
        private const val READ_TIMEOUT = 8000
    }

    // Static premium user credentials (hash tetap sama, tidak berubah)
    private val staticPremiumUsers = mapOf(
        "bahteramulyap@gmail.com" to Triple("8a0ff1f8926195dfde55af7e68c028591602dacc30dc3c7caef27a949ca45142b25514004cf4540c46eca830100d06517c6facc0faf77fc57140e9df5fe5ffc7", "CV Bahtera Mulya Plastik", "ten_premium_bahteramulyap_gmail_com"),
        "hanafiariful@gmail.com" to Triple("20710a82f8d6b458af10d49fbb1f985ac8aaf696e6b32e776d4f4ebbc30d08565e2bb5e1902ace18297d8db47ad35e49c086669125b1d6ac867c0d2d7e265e50", "PISANG KEJU RAMAYANA", "ten_premium_hanafiariful_gmail_com"),
        "fahrup22@gmail.com" to Triple("63e71711d1481b6da8b756e114aa2ac71a704929c0accf46f419706a5c1416ae1a312899ae84d3d8e33d255811e98fd4d17e59371a08e2f9c21c01d1b1c13a8d", "FahriP", "ten_premium_hanafiariful_gmail_com"),
        "alfarisirosi40@gmail.com" to Triple("a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c", "Mamet PKR", "ten_premium_hanafiariful_gmail_com"),
        "mulyakus84@gmail.com" to Triple("d630f857374a68dff86f8bd605d9e1826c4e4b3267a13371d42c07516993337ea97d4d1fc4c1b68f611ab08ac77e9175cd74c03ee094e081dace5dbc097b2d44", "CV. Aku&dia Bersama (Laundry)", "ten_premium_mulyakus84_gmail_com_LAUNDRY"),
        "playstoretest@gmail.com" to Triple("f93226ab6fd88288603a9ea14137015f3667f84ea23e34c32fad092883b3994546a681e423e9c1d087a5ea6f7238dcf8d3b7a27b93d2315addfde043c01cbf1a", "PlayStore Test", "ten_premium_playstoretest_gmail_com")
    )

    private val staticDemoUsers = mapOf(
        "demo@posbah.com" to Triple("f93226ab6fd88288603a9ea14137015f3667f84ea23e34c32fad092883b3994546a681e423e9c1d087a5ea6f7238dcf8d3b7a27b93d2315addfde043c01cbf1a", "User Demo (Trial)", "demo_tenant_demo_posbah_com")
    )

    private val premiumEmailSet = setOf(
        "muhammadmuizz8@gmail.com", "bahteramulyap@gmail.com",
        "hanafiariful@gmail.com", "fahrup22@gmail.com",
        "alfarisirosi40@gmail.com", "mulyakus84@gmail.com"
    )

    // ── Login result ─────────────────────────────────────────────────────────

    sealed class LoginOutcome {
        data class Success(val user: UserSession, val tenant: TenantSession) : LoginOutcome()
        data class NeedsTenantPick(val user: UserSession, val tenants: List<TenantSession>) : LoginOutcome()
        object Cancelled : LoginOutcome()
        data class Error(val message: String, val email: String? = null) : LoginOutcome()
        object Locked : LoginOutcome()
    }

    class VersionOutdatedException(message: String) : Exception(message)

    // ── Helper: HTTP GET ──────────────────────────────────────────────────────

    private fun httpGet(url: String): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        var isOutdated = false
        val result = try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            val code = conn.responseCode
            if (code == 426) {
                isOutdated = true
            }
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
                       else conn.errorStream?.bufferedReader()?.use { it.readText() }
            Pair(code, body)
        } catch (e: Exception) { Pair(-1, null) }
        finally { conn?.disconnect() }

        if (isOutdated) {
            throw VersionOutdatedException("Pembaruan aplikasi wajib dilakukan. Silakan unduh versi terbaru untuk melanjutkan.")
        }
        return result
    }

    private fun httpPost(url: String, bearerToken: String? = null): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        var isOutdated = false
        val result = try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
            if (bearerToken != null) {
                conn.setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            val code = conn.responseCode
            if (code == 426) {
                isOutdated = true
            }
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
                       else conn.errorStream?.bufferedReader()?.use { it.readText() }
            Pair(code, body)
        } catch (e: Exception) { Pair(-1, null) }
        finally { conn?.disconnect() }

        if (isOutdated) {
            throw VersionOutdatedException("Pembaruan aplikasi wajib dilakukan. Silakan unduh versi terbaru untuk melanjutkan.")
        }
        return result
    }

    // ── Fetch user profile from VPS (new local_users endpoint) ───────────────

    private fun fetchUserFromVps(email: String): JSONObject? {
        val (code, body) = httpGet("$BASE_URL/api/sync/local_users?email=eq.${URLEncoder.encode(email, "UTF-8")}")
        if (code !in 200..299 || body.isNullOrBlank()) return null
        val arr = JSONArray(body)
        return if (arr.length() > 0) arr.getJSONObject(0) else null
    }

    private fun fetchTenantFromVps(tenantId: String): JSONObject? {
        val (code, body) = httpGet("$BASE_URL/api/sync/tenants?id=eq.$tenantId")
        if (code !in 200..299 || body.isNullOrBlank()) return null
        val arr = JSONArray(body)
        return if (arr.length() > 0) arr.getJSONObject(0) else null
    }

    private fun fetchEmployeeFromVps(email: String): JSONObject? {
        val (code, body) = httpGet("$BASE_URL/api/sync/employees?email=eq.${URLEncoder.encode(email, "UTF-8")}")
        if (code in 409..409) return null // conflict
        if (code !in 200..299 || body.isNullOrBlank()) return null
        val arr = JSONArray(body)
        return if (arr.length() > 0) arr.getJSONObject(0) else null
    }

    private fun fetchOutletsFromVps(tenantId: String): JSONArray {
        val (code, body) = httpGet("$BASE_URL/api/rt/outlets")
        // In full online mode, outlets are fetched via authenticated Retrofit — but for auth flow
        // we use legacy sync endpoint as fallback
        if (code !in 200..299 || body.isNullOrBlank()) return JSONArray()
        return try { JSONArray(body) } catch (_: Exception) { JSONArray() }
    }

    // ── Google SSO Login ─────────────────────────────────────────────────────

    suspend fun loginWithGoogle(activity: android.app.Activity): LoginOutcome = withContext(Dispatchers.IO) {
        val googleResult = try {
            googleClient.signIn(activity)
        } catch (e: Exception) {
            return@withContext LoginOutcome.Error("Gagal otentikasi Google: ${e.localizedMessage}")
        }

        val identity = when (googleResult) {
            is GoogleSignInClient.Result.Success -> googleResult.identity
            GoogleSignInClient.Result.Cancelled -> return@withContext LoginOutcome.Cancelled
            is GoogleSignInClient.Result.Error -> {
                val msg = when (googleResult) {
                    is GoogleSignInClient.Result.Error.NoCredentials -> googleResult.message ?: "No credentials found"
                    is GoogleSignInClient.Result.Error.InvalidToken -> googleResult.reason
                    is GoogleSignInClient.Result.Error.Unexpected -> googleResult.throwable.localizedMessage ?: "Unexpected error"
                }
                return@withContext LoginOutcome.Error("Gagal Google Sign-In: $msg")
            }
        }

        val cleanEmail = identity.email.lowercase().trim()
        val name = identity.displayName ?: cleanEmail.substringBefore("@")
        val sub = identity.sub

        try {
            // Check if deleted/blocked
            val (checkCode, checkBody) = httpGet("$BASE_URL/api/auth/check-deleted?email=${URLEncoder.encode(cleanEmail, "UTF-8")}")
            if (checkCode in 200..299 && !checkBody.isNullOrBlank()) {
                val checkObj = JSONObject(checkBody)
                if (checkObj.optBoolean("deleted", false)) {
                    if (checkObj.optString("status") == "REJOINED") {
                        securePrefs.wipe()
                        httpPost("$BASE_URL/api/auth/complete-rejoin?email=${URLEncoder.encode(cleanEmail, "UTF-8")}")
                    } else {
                        return@withContext LoginOutcome.Error("Gagal login karena database tidak ada. Silakan hubungi admin.", cleanEmail)
                    }
                }
            }

            val isPremiumUser = cleanEmail in premiumEmailSet

            // Fetch user from VPS
            val vpsUser = fetchUserFromVps(cleanEmail)
            val isPremiumFromServer = vpsUser?.optBoolean("isPremium", false) ?: false
            val isActiveFromServer = vpsUser?.optBoolean("isActive", true) ?: true
            val tenantIdFromServer = vpsUser?.optString("tenantId")?.takeIf { it.isNotBlank() }

            if (!isActiveFromServer) {
                return@withContext LoginOutcome.Error("Akun Anda diblokir secara permanen. Hubungi muhammadmuizz8@gmail.com.")
            }

            // Check if employee
            val vpsEmployee = fetchEmployeeFromVps(cleanEmail)
            val isEmployee = vpsEmployee != null
            val userRole = if (vpsEmployee != null) {
                vpsEmployee.optString("role", "KASIR")
            } else {
                "OWNER"
            }

            val isPremiumFinal = isPremiumUser || isEmployee || isPremiumFromServer
            if (isPremiumFinal) {
                return@withContext LoginOutcome.Error("Email Anda terdaftar sebagai akun Premium. Silakan masuk melalui tab Premium (Email) menggunakan Email dan Password/PIN.")
            }

            val targetTenantId = if (vpsEmployee != null) {
                vpsEmployee.optString("tenantId").takeIf { it.isNotBlank() }
            } else {
                tenantIdFromServer
            }

            val businessMode = if (targetTenantId != null) {
                val tenantObj = fetchTenantFromVps(targetTenantId)
                tenantObj?.optString("businessMode", "FNB") ?: "FNB"
            } else null

            val user = UserSession(
                googleSub = sub,
                email = cleanEmail,
                displayName = name,
                photoUrl = null,
                role = userRole,
                tenantId = targetTenantId,
                businessMode = businessMode,
                isPremium = isPremiumFinal,
                businessModeLocked = isPremiumFinal,
                apkVersion = BuildConfig.VERSION_NAME
            )

            if (targetTenantId != null) {
                val tenantObj = fetchTenantFromVps(targetTenantId)
                val tenant = TenantSession(
                    id = targetTenantId,
                    name = tenantObj?.optString("name", "Tenant") ?: "Tenant",
                    ownerEmail = cleanEmail,
                    businessMode = businessMode ?: "FNB"
                )
                securePrefs.setActiveSession(sub, cleanEmail)
                securePrefs.currentTenantId = targetTenantId
                securePrefs.currentBusinessMode = businessMode
                // Set default outlet from VPS
                val outletId = vpsUser?.optLong("outletId")?.takeIf { it > 0 }
                if (outletId != null) securePrefs.currentOutletId = outletId
                return@withContext LoginOutcome.Success(user, tenant)
            } else {
                val dummyTenant = TenantSession("dummy_tenant", "Needs Selection", cleanEmail, "FNB")
                securePrefs.setActiveSession(sub, cleanEmail)
                securePrefs.currentTenantId = null
                return@withContext LoginOutcome.Success(user, dummyTenant)
            }
        } catch (e: VersionOutdatedException) {
            return@withContext LoginOutcome.Error(e.message ?: "Pembaruan aplikasi wajib dilakukan.")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error in loginWithGoogle", e)
            return@withContext LoginOutcome.Error("Gagal otentikasi Google: ${e.localizedMessage}")
        }
    }

    // ── Email + Password / PIN Login ─────────────────────────────────────────

    suspend fun loginWithEmailPassword(email: String, password: String): LoginOutcome = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            if (securePrefs.lockoutUntil > now) return@withContext LoginOutcome.Locked

            val cleanEmail = email.lowercase().trim()
            val staticInfo = staticPremiumUsers[cleanEmail]
            val staticDemoInfo = staticDemoUsers[cleanEmail]
            val isStatic = staticInfo != null || staticDemoInfo != null

            // Try fetching employee from VPS
            val vpsEmployee = fetchEmployeeFromVps(cleanEmail)
            val networkReachable = vpsEmployee != null || isStatic

            if (!networkReachable && !isStatic) {
                return@withContext LoginOutcome.Error("Gagal masuk: Server tidak terjangkau atau Anda sedang offline.")
            }

            if (isStatic) {
                // ── Static / hardcoded premium or demo user ──────────────────
                val isPremiumUser = staticInfo != null
                val (hash, defaultDisplayName, defaultTenantId) = staticInfo ?: staticDemoInfo!!

                // Try verifying against fetched VPS employee first, then fallback to static hash
                val dbPinHash = vpsEmployee?.optString("pinHash")
                val isValid = when {
                    dbPinHash != null && dbPinHash.isNotBlank() -> {
                        if (dbPinHash.startsWith("v1$")) PinHasher.verify(password, dbPinHash)
                        else BackendHasher.verify(password, dbPinHash)
                    }
                    else -> BackendHasher.verify(password, hash)
                }

                if (!isValid) return@withContext incrementFailedAttempts(now)

                securePrefs.failedPinAttempts = 0
                securePrefs.lockoutUntil = 0L

                // Resolve tenantId & displayName dynamically from VPS employee if available
                val vpsTenantId = vpsEmployee?.optString("tenantId")
                val tenantId = if (!vpsTenantId.isNullOrBlank()) vpsTenantId else defaultTenantId
                val displayName = vpsEmployee?.optString("name") ?: defaultDisplayName

                val isBmpTenant = tenantId.lowercase().contains("bahteramulyap")
                val mode = when {
                    isBmpTenant -> "BMP"
                    tenantId.endsWith("_LAUNDRY") || tenantId.lowercase().contains("laundry") -> "LAUNDRY"
                    tenantId.endsWith("_RENTAL") || tenantId.lowercase().contains("rental") -> "RENTAL"
                    else -> "FNB"
                }

                val tenantObj = fetchTenantFromVps(tenantId)
                val tenantName = tenantObj?.optString("name") ?: when {
                    isBmpTenant -> "CV. BAHTERA MULYA PLASTIK"
                    tenantId.contains("demo") -> "User Demo (Trial)"
                    else -> displayName
                }
                val tenantMode = tenantObj?.optString("businessMode") ?: mode

                val userRole = when (cleanEmail) {
                    "hanafiariful@gmail.com", "bahteramulyap@gmail.com", "mulyakus84@gmail.com", "demo@posbah.com" -> "OWNER"
                    "alfarisirosi40@gmail.com" -> "KASIR"
                    else -> vpsEmployee?.optString("role") ?: "ADMIN"
                }

                val user = UserSession(
                    googleSub = cleanEmail,
                    email = cleanEmail,
                    displayName = displayName,
                    photoUrl = null,
                    role = userRole,
                    tenantId = tenantId,
                    businessMode = tenantMode,
                    isPremium = isPremiumUser,
                    businessModeLocked = true,
                    apkVersion = BuildConfig.VERSION_NAME
                )
                val tenant = TenantSession(tenantId, tenantName, cleanEmail, tenantMode)

                // Set outlet from VPS employee outletId
                val outletId = vpsEmployee?.optLong("outletId")?.takeIf { it > 0 }
                if (outletId != null) securePrefs.currentOutletId = outletId

                if (user.role == "OWNER") {
                    sessionState.clearEmployeeLock()
                } else {
                    sessionState.setEmployeeOutletLock(outletId)
                }

                securePrefs.setActiveSession(cleanEmail, cleanEmail)
                securePrefs.currentTenantId = tenantId
                securePrefs.currentBusinessMode = tenantMode
                securePrefs.currentRole = user.role
                if (isPremiumUser) securePrefs.tempPlainPassword = password

                return@withContext LoginOutcome.Success(user, tenant)
            } else {
                // ── VPS employee login ────────────────────────────────────────
                val emp = vpsEmployee ?: return@withContext LoginOutcome.Error("Email atau password tidak ditemukan")

                val pinHash = emp.optString("pinHash", "")
                val isValid = if (pinHash.startsWith("v1$")) PinHasher.verify(password, pinHash)
                              else BackendHasher.verify(password, pinHash)

                if (!isValid) return@withContext incrementFailedAttempts(now)

                securePrefs.failedPinAttempts = 0
                securePrefs.lockoutUntil = 0L

                val tenantId = emp.optString("tenantId")
                val role = emp.optString("role", "KASIR")

                val tenantObj = fetchTenantFromVps(tenantId)
                    ?: return@withContext LoginOutcome.Error("Tenant tidak ditemukan")

                val tenantMode = tenantObj.optString("businessMode", "FNB")
                val outletId = emp.optLong("outletId").takeIf { it > 0 }

                val user = UserSession(
                    googleSub = "emp:${emp.optLong("id")}",
                    email = cleanEmail,
                    displayName = emp.optString("name", cleanEmail),
                    photoUrl = null,
                    role = role,
                    tenantId = tenantId,
                    businessMode = tenantMode,
                    isPremium = tenantId.startsWith("ten_premium_"),
                    businessModeLocked = true,
                    apkVersion = BuildConfig.VERSION_NAME
                )
                val tenant = TenantSession(
                    id = tenantId,
                    name = tenantObj.optString("name", "Tenant"),
                    ownerEmail = tenantObj.optString("ownerEmail", cleanEmail),
                    businessMode = tenantMode
                )

                if (role == "OWNER") {
                    sessionState.clearEmployeeLock()
                    securePrefs.currentOutletId = outletId
                } else {
                    sessionState.setEmployeeOutletLock(outletId)
                    securePrefs.currentOutletId = outletId
                }

                securePrefs.setActiveSession(user.googleSub, cleanEmail)
                securePrefs.currentTenantId = tenantId
                securePrefs.currentBusinessMode = tenantMode
                securePrefs.currentRole = role

                return@withContext LoginOutcome.Success(user, tenant)
            }
        } catch (e: VersionOutdatedException) {
            return@withContext LoginOutcome.Error(e.message ?: "Pembaruan aplikasi wajib dilakukan.")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error in loginWithEmailPassword", e)
            return@withContext LoginOutcome.Error("Gagal masuk: ${e.localizedMessage}")
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    suspend fun logout() = withContext(Dispatchers.IO) {
        // Full online mode: tidak ada sync saat logout karena semua sudah langsung ke VPS.
        // Cukup sign out Google + wipe session.
        googleClient.signOut()
        securePrefs.wipe()
    }

    // ── Session helpers ───────────────────────────────────────────────────────

    fun activeUserSub(): String? = securePrefs.currentGoogleSub
    fun activeUserEmail(): String? = securePrefs.currentEmail
    fun activeTenantId(): String? = securePrefs.currentTenantId
    fun activeBusinessMode(): String? = securePrefs.currentBusinessMode

    /** Alias untuk getActiveSession() — dipakai oleh ViewModel lama */
    suspend fun getActiveUser(): UserSession? = getActiveSession()

    fun getActiveSession(): UserSession? {
        val sub = securePrefs.currentGoogleSub ?: return null
        val email = securePrefs.currentEmail ?: return null
        val tenantId = securePrefs.currentTenantId
        val cleanEmail = email.lowercase().trim()
        val isPremiumFinal = (tenantId?.startsWith("ten_premium_") == true) || (cleanEmail in premiumEmailSet)
        return UserSession(
            googleSub = sub,
            email = email,
            displayName = email.substringBefore("@"),
            photoUrl = null,
            role = securePrefs.currentRole ?: "OWNER",
            tenantId = tenantId,
            businessMode = securePrefs.currentBusinessMode,
            isPremium = isPremiumFinal,
            businessModeLocked = isPremiumFinal
        )
    }

    suspend fun fetchUserOnline(email: String): UserSession? = withContext(Dispatchers.IO) {
        val obj = fetchUserFromVps(email) ?: return@withContext null
        val sub = obj.optString("googleSub", "")
        UserSession(
            googleSub = sub,
            email = obj.optString("email", email),
            displayName = obj.optString("displayName", null).takeIf { !it.isNullOrBlank() },
            photoUrl = obj.optString("photoUrl", null).takeIf { !it.isNullOrBlank() },
            role = obj.optString("role", "OWNER"),
            tenantId = obj.optString("tenantId", null).takeIf { !it.isNullOrBlank() },
            businessMode = obj.optString("businessMode", null).takeIf { !it.isNullOrBlank() },
            isPremium = obj.optBoolean("isPremium", false),
            businessModeLocked = obj.optBoolean("businessModeLocked", false),
            apkVersion = obj.optString("apkVersion", ""),
            whatsapp = obj.optString("whatsapp", null).takeIf { !it.isNullOrBlank() }
        )
    }

    // ── Tenant selection (multi-tenant Google users) ──────────────────────────

    suspend fun selectTenant(googleSub: String, tenantId: String): Boolean = withContext(Dispatchers.IO) {
        val email = securePrefs.currentEmail ?: return@withContext false
        val cleanEmail = email.lowercase().trim()

        // Premium email validation
        val tenantObj = fetchTenantFromVps(tenantId) ?: return@withContext false
        val ownerEmail = tenantObj.optString("ownerEmail")
        if (ownerEmail != cleanEmail) return@withContext false

        securePrefs.currentTenantId = tenantId
        securePrefs.currentBusinessMode = tenantObj.optString("businessMode")
        true
    }

    suspend fun getTenantOnline(tenantId: String): com.posbah.app.data.local.entities.Tenant? = withContext(Dispatchers.IO) {
        val obj = fetchTenantFromVps(tenantId) ?: return@withContext null
        com.posbah.app.data.local.entities.Tenant(
            id = obj.getString("id"),
            name = obj.optString("name", ""),
            ownerEmail = obj.optString("ownerEmail", ""),
            businessMode = obj.optString("businessMode", "FNB")
        )
    }

    suspend fun updateTenantOnline(tenantId: String, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var conn: HttpURLConnection? = null
            try {
                conn = URL("$BASE_URL/api/sync/tenants?id=eq.${URLEncoder.encode(tenantId, "UTF-8")}").openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = READ_TIMEOUT
                val body = """{"name":"$name"}"""
                conn.outputStream.bufferedWriter().use { it.write(body) }
                conn.responseCode in 200..299
            } finally { conn?.disconnect() }
        } catch (_: Exception) { false }
    }

    suspend fun updateProfileOnline(email: String, tenantId: String, displayName: String, whatsapp: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var conn: HttpURLConnection? = null
            try {
                val cleanEmail = email.lowercase().trim()
                conn = URL("$BASE_URL/api/sync/local_users?email=eq.${URLEncoder.encode(cleanEmail, "UTF-8")}").openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-tenant-id", tenantId)
                conn.setRequestProperty("x-user-email", cleanEmail)
                conn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = READ_TIMEOUT
                val body = """{"displayName":"$displayName","whatsapp":"$whatsapp"}"""
                conn.outputStream.bufferedWriter().use { it.write(body) }
                conn.responseCode in 200..299
            } finally { conn?.disconnect() }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "updateProfileOnline error", e)
            false
        }
    }

    // ── Fetch owner tenants from VPS ──────────────────────────────────────────

    suspend fun fetchOwnerTenants(email: String): List<TenantSession> = withContext(Dispatchers.IO) {
        try {
            val (code, body) = httpGet("$BASE_URL/api/sync/tenants?ownerEmail=eq.${URLEncoder.encode(email, "UTF-8")}")
            if (code !in 200..299 || body.isNullOrBlank()) return@withContext emptyList()
            val arr = JSONArray(body)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TenantSession(
                    id = obj.getString("id"),
                    name = obj.optString("name", "Tenant"),
                    ownerEmail = email,
                    businessMode = obj.optString("businessMode", "FNB")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // ── Upgrade / simulate payment ────────────────────────────────────────────

    suspend fun simulatePaymentAndUpgrade(email: String, password: String, businessName: String): LoginOutcome = withContext(Dispatchers.IO) {
        val cleanEmail = email.lowercase().trim()
        val emailKey = cleanEmail.replace(".", "_").replace("@", "_")
        val targetTenantId = "ten_premium_${emailKey}_BMP"

        val passwordHash = PinHasher.hash(password)

        // Create tenant + employee on VPS via admin endpoint
        val (code, _) = httpPost("$BASE_URL/api/admin/bootstrap-tenant?tenantId=${URLEncoder.encode(targetTenantId, "UTF-8")}&email=${URLEncoder.encode(cleanEmail, "UTF-8")}&businessName=${URLEncoder.encode(businessName.ifBlank { businessName }, "UTF-8")}", BuildConfig.ADMIN_AUTH_TOKEN)

        val tenantName = "${businessName.ifBlank { "CV. $cleanEmail" }} (Invoice & Manufaktur)"
        val tenant = TenantSession(targetTenantId, tenantName, cleanEmail, "BMP")
        val user = UserSession(
            googleSub = cleanEmail, email = cleanEmail, displayName = "Premium Owner", photoUrl = null,
            role = "OWNER", tenantId = targetTenantId, businessMode = "BMP", isPremium = true, businessModeLocked = true,
            apkVersion = BuildConfig.VERSION_NAME
        )

        securePrefs.setActiveSession(cleanEmail, cleanEmail)
        securePrefs.currentTenantId = targetTenantId
        securePrefs.currentBusinessMode = "BMP"
        securePrefs.tempPlainPassword = password

        LoginOutcome.Success(user, tenant)
    }

    // ── Change password ───────────────────────────────────────────────────────

    sealed class ChangePasswordResult {
        object Success : ChangePasswordResult()
        data class Error(val message: String) : ChangePasswordResult()
    }

    suspend fun changePassword(oldPin: String, newPin: String): ChangePasswordResult = withContext(Dispatchers.IO) {
        val email = activeUserEmail() ?: return@withContext ChangePasswordResult.Error("Sesi tidak valid")
        val cleanEmail = email.lowercase().trim()

        // Verify against VPS employee
        val emp = fetchEmployeeFromVps(cleanEmail)
        val staticInfo = staticPremiumUsers[cleanEmail]

        val storedHash = emp?.optString("pinHash") ?: staticInfo?.first
        if (storedHash.isNullOrBlank()) {
            return@withContext ChangePasswordResult.Error("User tidak ditemukan")
        }

        val isOldValid = if (storedHash.startsWith("v1$")) PinHasher.verify(oldPin, storedHash)
                         else BackendHasher.verify(oldPin, storedHash)
        if (!isOldValid) {
            return@withContext ChangePasswordResult.Error("Password lama salah")
        }

        // Rate limit check via SecurePreferences
        val now = System.currentTimeMillis()
        val lastChangeDay = securePrefs.lastPasswordChangeDayMillis
        val changeCount = if (isSameDay(lastChangeDay, now)) securePrefs.passwordChangeTodayCount else 0
        if (changeCount >= 2) {
            return@withContext ChangePasswordResult.Error("Maksimal ganti password 2 kali per hari")
        }

        val newHash = PinHasher.hash(newPin)
        // Push new hash to VPS via employee endpoint if employee exists
        if (emp != null) {
            val empId = emp.optLong("id")
            val tenantId = emp.optString("tenantId")
            // POST to update employee hash on VPS
            var conn: HttpURLConnection? = null
            try {
                conn = URL("$BASE_URL/api/rt/employees/$empId").openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer ${securePrefs.currentGoogleSub}")
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = READ_TIMEOUT
                val bodyJson = """{"pinHash":"$newHash"}"""
                conn.outputStream.bufferedWriter().use { it.write(bodyJson) }
                conn.responseCode
            } catch (_: Exception) {} finally { conn?.disconnect() }
        }

        securePrefs.passwordChangeTodayCount = changeCount + 1
        securePrefs.lastPasswordChangeDayMillis = now

        ChangePasswordResult.Success
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun incrementFailedAttempts(now: Long): LoginOutcome {
        val lastAttempt = securePrefs.lastFailedAttemptTime
        val attempts = if (now - lastAttempt > 10 * 60 * 1000L) { // 10-minute window
            1
        } else {
            securePrefs.failedPinAttempts + 1
        }
        securePrefs.failedPinAttempts = attempts
        securePrefs.lastFailedAttemptTime = now
        if (attempts >= 5) {
            securePrefs.lockoutUntil = now + 5 * 60 * 1000L
            return LoginOutcome.Locked
        }
        return LoginOutcome.Error("Email atau password salah (percobaan $attempts/5)")
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t1 == 0L) return false
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    // ── Tenant management (Full Online) ───────────────────────────────────────

    /**
     * Ambil list tenant milik owner dari VPS.
     * Digunakan oleh TenantPickerViewModel.
     */
    /** Buat tenant baru di VPS */
    suspend fun createTenant(email: String, name: String, businessMode: String, tenantId: String? = null): String = withContext(Dispatchers.IO) {
        val cleanEmail = email.lowercase().trim()
        val emailKey = cleanEmail.replace(".", "_").replace("@", "_")
        val isPremium = cleanEmail in premiumEmailSet
        val prefix = if (isPremium) "ten_premium" else "demo_tenant"
        val now = System.currentTimeMillis()
        val finalTenantId = tenantId ?: "${prefix}_${emailKey}_${businessMode.lowercase()}_$now"

        try {
            val jsonObject = JSONObject().apply {
                put("id", finalTenantId)
                put("name", name)
                put("ownerEmail", cleanEmail)
                put("businessMode", businessMode)
                put("isActive", true)
                put("createdAt", now)
                put("updatedAt", now)
            }
            val body = JSONArray().put(jsonObject).toString()

            var conn: HttpURLConnection? = null
            try {
                conn = URL("$BASE_URL/api/sync/tenants").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-tenant-id", finalTenantId)
                conn.setRequestProperty("x-user-email", cleanEmail)
                conn.setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = READ_TIMEOUT
                conn.outputStream.bufferedWriter().use { it.write(body) }
                val code = conn.responseCode
                val responseMsg = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                }
                android.util.Log.d("AuthRepository", "createTenant response code: $code, response: $responseMsg")
            } finally {
                conn?.disconnect()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "createTenant error", e)
        }
        finalTenantId
    }

    /** Ambil list outlet untuk tenant dari VPS. */
    suspend fun fetchOutletsForTenant(tenantId: String): List<com.posbah.app.data.repository.OutletData> = withContext(Dispatchers.IO) {
        try {
            val (code, body) = httpGet("$BASE_URL/api/rt/outlets")
            if (code !in 200..299 || body.isNullOrBlank()) return@withContext emptyList()
            val arr = JSONArray(body)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                com.posbah.app.data.repository.OutletData(
                    id = obj.optLong("id"),
                    tenantId = obj.optString("tenantId"),
                    name = obj.optString("name"),
                    isDefault = obj.optBoolean("isDefault", false),
                    isOpen = obj.optBoolean("isOpen", true)
                )
            }
        } catch (e: Exception) { emptyList() }
    }
}
