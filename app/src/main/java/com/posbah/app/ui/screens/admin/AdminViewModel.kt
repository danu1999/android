package com.posbah.app.ui.screens.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.LocalUser
import com.posbah.app.security.BackendHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random

data class DemoUserItem(
    val googleSub: String,
    val email: String,
    val displayName: String?,
    val registeredAt: Long,
    val isActive: Boolean
)

data class AdminUiState(
    val isLoading: Boolean = false,
    val demoUsers: List<DemoUserItem> = emptyList(),
    val errorMessage: String? = null,
    val generatedPasswordInfo: String? = null // Format: "Email: X, Password: Y"
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    companion object {
        private const val SUPABASE_URL = "https://etustetneufkfilndimy.supabase.co"
        private const val API_KEY = "sb_publishable_X_BhY3R3kKLp4wEpNX4giQ_U9xKDg2R"
    }

    init {
        loadDemoUsers()
    }

    fun loadDemoUsers() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, generatedPasswordInfo = null) }
        viewModelScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$SUPABASE_URL/rest/v1/local_users?isPremium=eq.false&order=registeredAt.desc")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $API_KEY")

                val code = conn.responseCode
                if (code in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(response)
                    val list = mutableListOf<DemoUserItem>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            DemoUserItem(
                                googleSub = obj.getString("googleSub"),
                                email = obj.getString("email"),
                                displayName = obj.optString("displayName", null),
                                registeredAt = obj.getLong("registeredAt"),
                                isActive = obj.optBoolean("isActive", true)
                            )
                        )
                    }
                    _uiState.update { it.copy(isLoading = false, demoUsers = list) }
                } else {
                    val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Error $code: $errorText") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Koneksi gagal") }
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun approveUser(item: DemoUserItem) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            // Generate a 6-digit random password PIN
            val generatedPin = String.format("%06d", Random.nextInt(100000, 999999))
            val hashedPin = BackendHasher.hash(generatedPin)

            // Step 1: Query the user's business mode from the demo tenant if it exists, default to "FNB"
            var businessMode = "FNB"
            val demoTenantId = "demo_tenant_${item.email.replace(".", "_").replace("@", "_")}"
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$SUPABASE_URL/rest/v1/tenants?id=eq.$demoTenantId")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $API_KEY")
                if (conn.responseCode in 200..299) {
                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(res)
                    if (array.length() > 0) {
                        businessMode = array.getJSONObject(0).optString("businessMode", "FNB")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }

            // Step 2: Create a Premium Tenant on Supabase
            val premiumTenantId = "ten_premium_${item.email.replace(".", "_").replace("@", "_")}"
            val tenantJson = JSONObject().apply {
                put("id", premiumTenantId)
                put("name", "CV. ${item.displayName ?: "Premium"} (Premium)")
                put("ownerEmail", item.email)
                put("businessMode", businessMode)
                put("isActive", true)
                put("createdAt", System.currentTimeMillis())
                put("updatedAt", System.currentTimeMillis())
            }
            conn = null
            try {
                val url = URL("$SUPABASE_URL/rest/v1/tenants")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $API_KEY")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                conn.outputStream.use { out ->
                    out.bufferedWriter().use { it.write(tenantJson.toString()) }
                }
                conn.responseCode // execute
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }

            // Step 3: Create an Owner Employee record on Supabase with the generated pinHash
            val employeeJson = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("tenantId", premiumTenantId)
                put("outletId", JSONObject.NULL)
                put("name", item.displayName ?: "Owner Premium")
                put("email", item.email)
                put("role", "OWNER")
                put("pinHash", hashedPin)
                put("salary", 0.0)
                put("isActive", true)
                put("createdAt", System.currentTimeMillis())
                put("updatedAt", System.currentTimeMillis())
            }
            conn = null
            try {
                val url = URL("$SUPABASE_URL/rest/v1/employees")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $API_KEY")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                conn.outputStream.use { out ->
                    out.bufferedWriter().use { it.write(employeeJson.toString()) }
                }
                conn.responseCode // execute
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }

            // Step 4: Update the user to Premium and set the premium tenantId
            conn = null
            try {
                val url = URL("$SUPABASE_URL/rest/v1/local_users?googleSub=eq.${item.googleSub}")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.doOutput = true
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $API_KEY")
                conn.setRequestProperty("Content-Type", "application/json")
                
                val patchBody = JSONObject().apply {
                    put("isPremium", true)
                    put("tenantId", premiumTenantId)
                    put("updatedAt", System.currentTimeMillis())
                }
                conn.outputStream.use { out ->
                    out.bufferedWriter().use { it.write(patchBody.toString()) }
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generatedPasswordInfo = "Email: ${item.email}\nPassword: $generatedPin"
                        )
                    }
                    loadDemoUsers()
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Update User Gagal: $err") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun rejectUser(item: DemoUserItem) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                // Delete user from local_users table on Supabase
                val url = URL("$SUPABASE_URL/rest/v1/local_users?googleSub=eq.${item.googleSub}")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $API_KEY")

                val code = conn.responseCode
                if (code in 200..299) {
                    _uiState.update { it.copy(isLoading = false) }
                    loadDemoUsers()
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Hapus Gagal: $err") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun clearPasswordInfo() {
        _uiState.update { it.copy(generatedPasswordInfo = null) }
    }
}
