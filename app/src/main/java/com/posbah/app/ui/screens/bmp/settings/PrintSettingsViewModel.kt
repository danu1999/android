package com.posbah.app.ui.screens.bmp.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.PrintSettingsEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.PrintSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PrintSettingsViewModel @Inject constructor(
    private val printSettingsRepo: PrintSettingsRepository,
    private val authRepository: AuthRepository,
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

    init {
        // Guard: jangan query database jika tenantId kosong (race condition saat login)
        if (tenantId.isNotBlank()) {
            viewModelScope.launch {
                val existing = printSettingsRepo.get(tenantId, moduleKey)
                _draft.value = existing ?: PrintSettingsEntity(
                    tenantId = tenantId,
                    moduleKey = moduleKey
                )
            }
        }
    }

    fun update(transform: (PrintSettingsEntity) -> PrintSettingsEntity) {
        val cur = _draft.value ?: return
        _draft.update { transform(cur) }
    }

    fun save(onError: (String) -> Unit = {}, onDone: () -> Unit = {}) = viewModelScope.launch {
        // Guard: pastikan tenantId tidak kosong
        if (tenantId.isBlank()) {
            onError("Sesi tidak valid. Silakan logout dan login kembali.")
            return@launch
        }
        val d = _draft.value ?: return@launch

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

        printSettingsRepo.upsert(d.copy(updatedAt = System.currentTimeMillis()))
        onDone()
    }
}
