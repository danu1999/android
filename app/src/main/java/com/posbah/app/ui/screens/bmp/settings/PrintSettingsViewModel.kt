package com.posbah.app.ui.screens.bmp.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.local.entities.PrintSettingsEntity
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.PrintSettingsRepository
import com.posbah.app.data.repository.toOnlineWriteResult
import com.posbah.app.data.repository.OnlineWriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrintSettingsViewModel @Inject constructor(
    private val printSettingsRepo: PrintSettingsRepository,
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tenantId = authRepository.activeTenantId().orEmpty()

    /**
     * Key modul yang pengaturannya sedang diedit.
     * Diterima dari argumen navigasi. Nilai valid: "BMP" | "FNB" | "LAUNDRY" | "RENTAL"
     * Default "BMP" untuk backward-compatibility (jalur dari Settings BMP).
     */
    val moduleKey: String = savedStateHandle.get<String>("moduleKey") ?: "BMP"

    private val _draft = MutableStateFlow<PrintSettingsEntity?>(null)
    val draft = _draft.asStateFlow()

    private val _draftTenant = MutableStateFlow<Tenant?>(null)
    val draftTenant = _draftTenant.asStateFlow()

    init {
        // Guard: jangan query database jika tenantId kosong (race condition saat login)
        if (tenantId.isNotBlank()) {
            viewModelScope.launch {
                val existing = printSettingsRepo.get(tenantId, moduleKey)
                _draft.value = existing ?: PrintSettingsEntity(
                    tenantId = tenantId,
                    moduleKey = moduleKey
                )
                _draftTenant.value = db.tenantDao().getById(tenantId)
            }
        }
    }

    fun updateTenantName(name: String) {
        _draftTenant.update { it?.copy(name = name) }
    }

    fun update(transform: (PrintSettingsEntity) -> PrintSettingsEntity) {
        val cur = _draft.value ?: return
        _draft.update { transform(cur) }
    }

    fun processAndSetLogo(uri: android.net.Uri, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    launch(Dispatchers.Main) { onError("Gagal membuka gambar") }
                    return@launch
                }
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (originalBitmap == null) {
                    launch(Dispatchers.Main) { onError("Gambar tidak valid") }
                    return@launch
                }

                // Resize bitmap so that max dimension is 512px
                val maxDim = 512
                val width = originalBitmap.width
                val height = originalBitmap.height
                val (newWidth, newHeight) = if (width > height) {
                    if (width > maxDim) {
                        maxDim to (height * maxDim / width)
                    } else {
                        width to height
                    }
                } else {
                    if (height > maxDim) {
                        (width * maxDim / height) to maxDim
                    } else {
                        width to height
                    }
                }
                
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap,
                    newWidth,
                    newHeight,
                    true
                )
                if (resizedBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }

                val outputStream = java.io.ByteArrayOutputStream()
                resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 85, outputStream)
                val bytes = outputStream.toByteArray()
                resizedBitmap.recycle()

                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val base64Url = "data:image/png;base64,$base64"

                launch(Dispatchers.Main) {
                    update { e -> e.copy(logoPath = base64Url) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    onError("Terjadi kesalahan: ${e.localizedMessage}")
                }
            }
        }
    }

    fun save(onError: (String) -> Unit = {}, onDone: () -> Unit = {}) = viewModelScope.launch {
        // Guard: pastikan tenantId tidak kosong
        if (tenantId.isBlank()) {
            onError("Sesi tidak valid. Silakan logout dan login kembali.")
            return@launch
        }
        val d = _draft.value ?: return@launch
        val t = _draftTenant.value

        // Validasi khusus modul BMP (Invoice & Manufaktur membutuhkan info bank)
        if (moduleKey == "BMP") {
            if (d.bankOwnerName.isBlank()) {
                onError("Kolom Atas Nama info pembayaran wajib diisi untuk Invoice & Manufaktur!")
                return@launch
            }
            if (d.bankAccountNumber.isBlank()) {
                onError("Kolom Nomor Rekening / Dana / Shopee wajib diisi untuk Invoice & Manufaktur!")
                return@launch
            }
        }

        val updateWithTime = d.copy(updatedAt = System.currentTimeMillis())
        printSettingsRepo.upsert(updateWithTime)

        // Write-through ke VPS
        val vpsResult = com.posbah.app.data.remote.BmpOnlineWriter.upsertPrintSettings(context, tenantId, updateWithTime).toOnlineWriteResult()
        if (vpsResult !is OnlineWriteResult.Success) {
            onError(if (vpsResult is OnlineWriteResult.Error) vpsResult.message else "Tidak ada koneksi internet. Pengaturan tidak tersimpan.")
            return@launch
        }

        if (t != null) {
            db.tenantDao().upsert(t.copy(updatedAt = System.currentTimeMillis()))
            // Sync BmpSettingsEntity if exists, to keep it consistent
            try {
                val existingBmpSettings = db.bmpSettingsDao().get(t.id)
                if (existingBmpSettings != null) {
                    db.bmpSettingsDao().upsert(existingBmpSettings.copy(clientName = t.name, updatedAt = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDone()
    }
}
