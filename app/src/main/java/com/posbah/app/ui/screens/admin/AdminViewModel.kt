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
        private const val BASE_URL = "https://www.zedmz.cloud"
        private val AUTH_TOKEN = com.posbah.app.BuildConfig.ADMIN_AUTH_TOKEN
    }

    init {
        loadDemoUsers()
    }

    fun loadDemoUsers() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, generatedPasswordInfo = null) }
        viewModelScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("$BASE_URL/api/admin/demo-users")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Authorization", AUTH_TOKEN)

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
                                displayName = if (obj.isNull("displayName")) null else obj.getString("displayName"),
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

            var conn: HttpURLConnection? = null
            try {
                val url = URL("$BASE_URL/api/admin/approve-user")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Authorization", AUTH_TOKEN)
                conn.setRequestProperty("Content-Type", "application/json")

                val body = JSONObject().apply {
                    put("googleSub", item.googleSub)
                    put("email", item.email)
                    put("displayName", item.displayName ?: "Owner Premium")
                    put("pinHash", hashedPin)
                }

                conn.outputStream.use { out ->
                    out.bufferedWriter().use { it.write(body.toString()) }
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
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Persetujuan Gagal: $err") }
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
                val url = URL("$BASE_URL/api/admin/reject-user")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Authorization", AUTH_TOKEN)
                conn.setRequestProperty("Content-Type", "application/json")

                val body = JSONObject().apply {
                    put("googleSub", item.googleSub)
                }

                conn.outputStream.use { out ->
                    out.bufferedWriter().use { it.write(body.toString()) }
                }

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
