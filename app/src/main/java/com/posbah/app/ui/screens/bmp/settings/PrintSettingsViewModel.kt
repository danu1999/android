package com.posbah.app.ui.screens.bmp.settings

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

import com.posbah.app.data.local.PosBahDatabase

@HiltViewModel
class PrintSettingsViewModel @Inject constructor(
    private val printSettingsRepo: PrintSettingsRepository,
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    private val _draft = MutableStateFlow<PrintSettingsEntity?>(null)
    val draft = _draft.asStateFlow()

    private var businessMode: String = "BMP"

    init {
        viewModelScope.launch {
            val existing = printSettingsRepo.get(tenantId)
            _draft.value = existing ?: PrintSettingsEntity(
                tenantId = tenantId
            )
            val tenant = db.tenantDao().getById(tenantId)
            businessMode = tenant?.businessMode ?: "BMP"
        }
    }

    fun update(transform: (PrintSettingsEntity) -> PrintSettingsEntity) {
        val cur = _draft.value ?: return
        _draft.update { transform(cur) }
    }

    fun save(onError: (String) -> Unit = {}, onDone: () -> Unit = {}) = viewModelScope.launch {
        val d = _draft.value ?: return@launch
        if (businessMode == "BMP") {
            if (d.bankOwnerName.isBlank()) {
                onError("Kolom Atas Nama info pembayaran wajib diisi untuk Invoice & Manufaktur!")
                return@launch
            }
            if (d.bankAccountNumber.isBlank()) {
                onError("Kolom Nomor Rekening / Dana / Shopee wajib diisi untuk Invoice & Manufaktur!")
                return@launch
            }
        }
        printSettingsRepo.upsert(d)
        onDone()
    }
}
