package com.posbah.app.ui.screens.bmp.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.PrintSettingsEntity
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.PrintSettingsRepository
import com.posbah.app.data.repository.OnlineWriteResult
import com.posbah.app.data.repository.PrintSettingsData
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
    private val securePrefs: com.posbah.app.security.SecurePreferences,
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
                val data = printSettingsRepo.get(moduleKey)
                val existing = data?.let { d ->
                    PrintSettingsEntity(
                        id = d.id,
                        tenantId = tenantId,
                        moduleKey = moduleKey,
                        jpgUseLogo = d.jpgUseLogo,
                        jpgHeaderAlign = d.jpgHeaderAlign,
                        jpgUseSignature = d.jpgUseSignature,
                        jpgSignatureSenderName = d.jpgSignatureSenderName,
                        jpgSignatureReceiverName = d.jpgSignatureReceiverName,
                        jpgSignatureDrawnBase64 = d.jpgSignatureDrawnBase64,
                        jpgIsColor = d.jpgIsColor,
                        sjUseLogo = d.sjUseLogo,
                        sjHeaderAlign = d.sjHeaderAlign,
                        sjUseSignature = d.sjUseSignature,
                        sjSignatureSenderName = d.sjSignatureSenderName,
                        sjSignatureReceiverName = d.sjSignatureReceiverName,
                        sjSignatureDrawnBase64 = d.sjSignatureDrawnBase64,
                        sjIsColor = d.sjIsColor,
                        invoiceUseLogo = d.invoiceUseLogo,
                        invoiceHeaderAlign = d.invoiceHeaderAlign,
                        invoiceUseSignature = d.invoiceUseSignature,
                        invoiceSignatureSenderName = d.invoiceSignatureSenderName,
                        invoiceSignatureReceiverName = d.invoiceSignatureReceiverName,
                        invoiceSignatureDrawnBase64 = d.invoiceSignatureDrawnBase64,
                        invoiceIsColor = d.invoiceIsColor,
                        receiptPaperWidth = d.receiptPaperWidth,
                        receiptUseLogo = d.receiptUseLogo,
                        receiptHeaderAlign = d.receiptHeaderAlign,
                        receiptIsColor = d.receiptIsColor,
                        receiptShowItemPrice = d.receiptShowItemPrice,
                        receiptFooterText = d.receiptFooterText,
                        jpgTemplateType = d.jpgTemplateType,
                        sjTemplateType = d.sjTemplateType,
                        invoiceTemplateType = d.invoiceTemplateType,
                        bankOwnerName = d.bankOwnerName,
                        bankName = d.bankName,
                        bankAccountNumber = d.bankAccountNumber,
                        logoPath = d.logoPath,
                        logoUrl = d.logoUrl,
                        jpgSignatureDrawnUrl = d.jpgSignatureDrawnUrl,
                        sjSignatureDrawnUrl = d.sjSignatureDrawnUrl,
                        invoiceSignatureDrawnUrl = d.invoiceSignatureDrawnUrl,
                        createdAt = d.createdAt,
                        updatedAt = d.updatedAt
                    )
                }
                _draft.value = existing ?: PrintSettingsEntity(
                    tenantId = tenantId,
                    moduleKey = moduleKey
                )
                // Ambil nama tenant dari SecurePreferences (tidak hit tenantDao stub)
                val tenantName = securePrefs.currentTenantName ?: authRepository.getActiveUser()?.displayName
                _draftTenant.value = com.posbah.app.data.local.entities.Tenant(
                    id = tenantId,
                    name = tenantName ?: "",
                    ownerEmail = authRepository.activeUserEmail() ?: "",
                    updatedAt = System.currentTimeMillis()
                )
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

                // Set logo lokal dulu agar UI responsif
                launch(Dispatchers.Main) {
                    update { e -> e.copy(logoPath = base64Url) }
                }

                // Upload ke VPS secara background — folder terisolasi per tenantId
                // Logo disimpan di: /logos/{tenantId}/logo.png di server
                if (tenantId.isNotBlank()) {
                    val logoUrl = com.posbah.app.data.remote.VpsImageUploader.uploadLogoToVps(
                        context,
                        bytes,
                        tenantId
                    )
                    if (logoUrl != null) {
                        // Simpan URL permanen ke draft agar tersinkronisasi ke VPS via save()
                        launch(Dispatchers.Main) {
                            update { e -> e.copy(logoUrl = logoUrl) }
                        }
                        android.util.Log.d("PrintSettingsVM", "[Logo] Upload berhasil: $logoUrl")
                    } else {
                        android.util.Log.w("PrintSettingsVM", "[Logo] Upload ke VPS gagal — logo lokal tetap tersimpan, akan coba saat sync berikutnya")
                    }
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

        // Upload TTD pengirim ke VPS jika ada base64 tapi belum ada URL
        // Folder terisolasi per tenant: /ttd-pengirim/{tenantId}/{moduleKey}_{docType}.png
        // Ini memastikan TTD pengirim tidak hilang saat ganti HP atau reinstall
        var entity = d
        if (tenantId.isNotBlank()) {
            // JPG / Surat Jalan / Invoice — upload masing-masing jika ada base64 tapi belum ada URL
            if (!d.jpgSignatureDrawnBase64.isNullOrBlank() && d.jpgSignatureDrawnUrl.isNullOrBlank()) {
                try {
                    val bytes = android.util.Base64.decode(
                        d.jpgSignatureDrawnBase64!!.removePrefix("data:image/png;base64,"),
                        android.util.Base64.NO_WRAP
                    )
                    val url = com.posbah.app.data.remote.VpsImageUploader.uploadTtdPengirimToVps(
                        context, bytes, tenantId, moduleKey, "jpg"
                    )
                    if (url != null) {
                        entity = entity.copy(jpgSignatureDrawnUrl = url)
                        android.util.Log.d("PrintSettingsVM", "[TTD] Upload JPG berhasil: $url")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PrintSettingsVM", "[TTD] Gagal upload JPG TTD: ${e.message}")
                }
            }
            if (!d.sjSignatureDrawnBase64.isNullOrBlank() && d.sjSignatureDrawnUrl.isNullOrBlank()) {
                try {
                    val bytes = android.util.Base64.decode(
                        d.sjSignatureDrawnBase64!!.removePrefix("data:image/png;base64,"),
                        android.util.Base64.NO_WRAP
                    )
                    val url = com.posbah.app.data.remote.VpsImageUploader.uploadTtdPengirimToVps(
                        context, bytes, tenantId, moduleKey, "sj"
                    )
                    if (url != null) {
                        entity = entity.copy(sjSignatureDrawnUrl = url)
                        android.util.Log.d("PrintSettingsVM", "[TTD] Upload SJ berhasil: $url")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PrintSettingsVM", "[TTD] Gagal upload SJ TTD: ${e.message}")
                }
            }
            if (!d.invoiceSignatureDrawnBase64.isNullOrBlank() && d.invoiceSignatureDrawnUrl.isNullOrBlank()) {
                try {
                    val bytes = android.util.Base64.decode(
                        d.invoiceSignatureDrawnBase64!!.removePrefix("data:image/png;base64,"),
                        android.util.Base64.NO_WRAP
                    )
                    val url = com.posbah.app.data.remote.VpsImageUploader.uploadTtdPengirimToVps(
                        context, bytes, tenantId, moduleKey, "invoice"
                    )
                    if (url != null) {
                        entity = entity.copy(invoiceSignatureDrawnUrl = url)
                        android.util.Log.d("PrintSettingsVM", "[TTD] Upload Invoice berhasil: $url")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PrintSettingsVM", "[TTD] Gagal upload Invoice TTD: ${e.message}")
                }
            }
            // Update draft agar URL tersimpan di UI state juga
            if (entity !== d) _draft.value = entity
        }

        val updateWithTime = entity.copy(updatedAt = System.currentTimeMillis())
        val dataToSave = PrintSettingsData(
            id = updateWithTime.id,
            tenantId = tenantId,
            moduleKey = updateWithTime.moduleKey,
            jpgUseLogo = updateWithTime.jpgUseLogo,
            jpgHeaderAlign = updateWithTime.jpgHeaderAlign,
            jpgUseSignature = updateWithTime.jpgUseSignature,
            jpgSignatureSenderName = updateWithTime.jpgSignatureSenderName,
            jpgSignatureReceiverName = updateWithTime.jpgSignatureReceiverName,
            jpgSignatureDrawnBase64 = updateWithTime.jpgSignatureDrawnBase64,
            jpgIsColor = updateWithTime.jpgIsColor,
            sjUseLogo = updateWithTime.sjUseLogo,
            sjHeaderAlign = updateWithTime.sjHeaderAlign,
            sjUseSignature = updateWithTime.sjUseSignature,
            sjSignatureSenderName = updateWithTime.sjSignatureSenderName,
            sjSignatureReceiverName = updateWithTime.sjSignatureReceiverName,
            sjSignatureDrawnBase64 = updateWithTime.sjSignatureDrawnBase64,
            sjIsColor = updateWithTime.sjIsColor,
            invoiceUseLogo = updateWithTime.invoiceUseLogo,
            invoiceHeaderAlign = updateWithTime.invoiceHeaderAlign,
            invoiceUseSignature = updateWithTime.invoiceUseSignature,
            invoiceSignatureSenderName = updateWithTime.invoiceSignatureSenderName,
            invoiceSignatureReceiverName = updateWithTime.invoiceSignatureReceiverName,
            invoiceSignatureDrawnBase64 = updateWithTime.invoiceSignatureDrawnBase64,
            invoiceIsColor = updateWithTime.invoiceIsColor,
            receiptPaperWidth = updateWithTime.receiptPaperWidth,
            receiptUseLogo = updateWithTime.receiptUseLogo,
            receiptHeaderAlign = updateWithTime.receiptHeaderAlign,
            receiptIsColor = updateWithTime.receiptIsColor,
            receiptShowItemPrice = updateWithTime.receiptShowItemPrice,
            receiptFooterText = updateWithTime.receiptFooterText,
            jpgTemplateType = updateWithTime.jpgTemplateType,
            sjTemplateType = updateWithTime.sjTemplateType,
            invoiceTemplateType = updateWithTime.invoiceTemplateType,
            bankOwnerName = updateWithTime.bankOwnerName,
            bankName = updateWithTime.bankName,
            bankAccountNumber = updateWithTime.bankAccountNumber,
            logoPath = updateWithTime.logoPath,
            logoUrl = updateWithTime.logoUrl ?: updateWithTime.logoPath,
            jpgSignatureDrawnUrl = updateWithTime.jpgSignatureDrawnUrl,
            sjSignatureDrawnUrl = updateWithTime.sjSignatureDrawnUrl,
            invoiceSignatureDrawnUrl = updateWithTime.invoiceSignatureDrawnUrl,
            createdAt = updateWithTime.createdAt,
            updatedAt = updateWithTime.updatedAt
        )

        viewModelScope.launch {
            val vpsResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                printSettingsRepo.save(dataToSave)
            }
            if (vpsResult is OnlineWriteResult.Success) {
                onDone()
            } else {
                val msg = if (vpsResult is OnlineWriteResult.Error) vpsResult.message else "Tidak ada koneksi internet. Pengaturan tidak tersimpan."
                onError(msg)
            }
        }
    }
}
