package com.posbah.app.ui.screens.bmp.bahanbaku

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpBahanBakuEntity
import com.posbah.app.data.local.entities.BmpBahanBakuItemEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpBahanBakuRepository
import com.posbah.app.data.repository.BmpCashFlowRepository
import com.posbah.app.data.repository.BmpCashflowData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// List ViewModel
// ─────────────────────────────────────────────

@HiltViewModel
class BahanBakuListViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val repo: BmpBahanBakuRepository,
    private val authRepo: AuthRepository,
    private val cashFlowRepo: BmpCashFlowRepository
) : ViewModel() {
    private val tenantId = authRepo.activeTenantId().orEmpty()

    init {
        viewModelScope.launch {
            try {
                repo.refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val list = repo.observe(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpBahanBakuEntity>())

    val suppliers = repo.suppliers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalHarga = repo.totalHarga(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalNominal = repo.totalNominal(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _filterStartDate = MutableStateFlow<Long?>(null)
    val filterStartDate = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<Long?>(null)
    val filterEndDate = _filterEndDate.asStateFlow()

    private val _filterHutang = MutableStateFlow(false)
    val filterHutang = _filterHutang.asStateFlow()

    private val _filterDibayar = MutableStateFlow(false)
    val filterDibayar = _filterDibayar.asStateFlow()

    val filteredList = kotlinx.coroutines.flow.combine(
        repo.observe(tenantId),
        _filterStartDate,
        _filterEndDate,
        _filterHutang,
        _filterDibayar
    ) { rawList, start, end, showHutang, showPaid ->
        var list = rawList
        if (start != null) {
            list = list.filter { it.tanggal >= start }
        }
        if (end != null) {
            list = list.filter { it.tanggal <= end + 24 * 60 * 60 * 1000L - 1 }
        }
        if (showHutang && !showPaid) {
            list = list.filter { it.totalHarga - it.nominal > 0 }
        }
        if (showPaid && !showHutang) {
            list = list.filter { it.totalHarga - it.nominal <= 0 }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<BmpBahanBakuEntity>())

    fun setDateRange(start: Long?, end: Long?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }

    fun toggleFilterHutang(enabled: Boolean) {
        _filterHutang.value = enabled
    }

    fun toggleFilterDibayar(enabled: Boolean) {
        _filterDibayar.value = enabled
    }

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }

    fun delete(id: Long) = viewModelScope.launch {
        android.widget.Toast.makeText(context, "Data berhasil dihapus!", android.widget.Toast.LENGTH_SHORT).show()
        val result = repo.delete(context, tenantId, id)
        if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            _error.value = result.message
            android.widget.Toast.makeText(context, "Gagal hapus: ${result.message}", android.widget.Toast.LENGTH_LONG).show()
        } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
            _error.value = "Tidak ada koneksi internet."
            android.widget.Toast.makeText(context, "Gagal hapus: Tidak ada internet", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun payDebt(id: Long, amount: Double) = viewModelScope.launch {
        val result = repo.payDebt(context, tenantId, id, amount)
        if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            _error.value = result.message
        } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
            _error.value = "Tidak ada koneksi internet."
        }
    }

    fun payGlobal(method: String, amount: Double, supplier: String, notes: String) = viewModelScope.launch {
        val noTagihan = if (method == "TRANSFER") "BAYAR-TF" else "BAYAR-CASH"
        val paymentBaku = com.posbah.app.data.repository.BmpBahanBakuData(
            id = 0,
            tenantId = tenantId,
            tanggal = System.currentTimeMillis(),
            noTagihan = noTagihan,
            supplier = supplier,
            totalHarga = 0.0,
            nominal = amount,
            notaFotoPath = null,
            notaFotoUrl = null,
            notes = notes
        )
        val result = repo.create(paymentBaku, emptyList())
        if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
            _error.value = result.message
        } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
            _error.value = "Tidak ada koneksi internet."
        } else {
            try {
                repo.refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun recordAfvalRecycling(jenisBahan: String, beratKg: Double, biayaGiling: Double) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        if (beratKg <= 0 || biayaGiling <= 0 || jenisBahan.isBlank()) return@launch
        try {
            val rate = biayaGiling / beratKg
            
            // 1. Catat kas keluar (FACTORY_OVERHEAD)
            cashFlowRepo.createEntry(
                BmpCashflowData(
                    tenantId = tenantId,
                    transactionDate = System.currentTimeMillis(),
                    transactionType = "KELUAR",
                    description = "Jasa Giling Afval ($jenisBahan gilingan, $beratKg Kg)",
                    amount = biayaGiling,
                    costType = "FACTORY_OVERHEAD"
                )
            )

            // 2. Tambah stok bahan baku masuk
            val bahanBaku = com.posbah.app.data.repository.BmpBahanBakuData(
                id = 0,
                tenantId = tenantId,
                tanggal = System.currentTimeMillis(),
                noTagihan = "AFVAL-GILING-${System.currentTimeMillis() / 1000}",
                supplier = "Daur Ulang Internal (Afval)",
                totalHarga = biayaGiling,
                nominal = biayaGiling,
                notes = "Hasil Daur Ulang Afval $jenisBahan"
            )
            val item = com.posbah.app.data.repository.BmpBahanBakuItemData(
                id = 0,
                bahanBakuId = 0,
                jenisBahan = jenisBahan,
                kuantitas = beratKg,
                rate = rate
            )
            repo.create(bahanBaku, listOf(item))
            repo.refresh()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ─────────────────────────────────────────────
// Form ViewModel
// ─────────────────────────────────────────────

data class BahanBakuItemDraft(
    val id: Long = 0,
    val jenisBahan: String = "",
    val kuantitas: String = "",
    val unit: String = "Kg",
    val rate: String = ""
) {
    val subtotal: Double get() {
        val q = kuantitas.replace(",", ".").toDoubleOrNull() ?: 0.0
        val r = rate.replace(",", ".").toDoubleOrNull() ?: 0.0
        return q * r
    }
}

/** Status upload foto nota ke Cloudinary. */
enum class FotoUploadStatus {
    IDLE,           // Belum ada foto
    LOCAL_SAVED,    // Foto sudah diambil & dikompresi lokal, belum upload
    UPLOADING,      // Sedang upload ke Cloudinary
    UPLOADED,       // Berhasil upload, URL tersedia
    ERROR           // Gagal upload
}

data class BahanBakuFormUiState(
    val header: BmpBahanBakuEntity? = null,
    val items: List<BahanBakuItemDraft> = listOf(BahanBakuItemDraft()),
    val nominalInput: String = "",
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val saveError: String? = null,
    // ── Foto Nota ──────────────────────────────────────────────
    val notaFotoPath: String? = null,           // path lokal JPEG ≤100 KB
    val notaFotoUrl: String? = null,            // URL Cloudinary (jika sudah upload)
    val fotoFileSizeKb: Long = 0L,              // ukuran file untuk ditampilkan di UI
    val fotoUploadStatus: FotoUploadStatus = FotoUploadStatus.IDLE,
    val fotoUploadError: String? = null         // pesan error upload
) {
    val totalHarga: Double get() = items.sumOf { it.subtotal }
    val hasLocalPhoto: Boolean get() = !notaFotoPath.isNullOrBlank()
    val hasCloudPhoto: Boolean get() = !notaFotoUrl.isNullOrBlank()
}

@HiltViewModel
class BahanBakuFormViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val repo: BmpBahanBakuRepository,
    private val authRepo: AuthRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    companion object {
        private val uploadScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        )
    }

    private val tenantId = authRepo.activeTenantId().orEmpty()
    private val editingId: Long =
        savedState.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 } ?: -1L

    private var originalNominal: Double = 0.0

    private val _ui = MutableStateFlow(
        BahanBakuFormUiState(
            header = BmpBahanBakuEntity(
                tenantId = tenantId,
                noTagihan = "BB-" + System.currentTimeMillis().toString().takeLast(8),
                tanggal = System.currentTimeMillis()
            )
        )
    )
    val ui = _ui.asStateFlow()

    val suppliers = repo.suppliers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (editingId > 0) viewModelScope.launch {
            val h = repo.getById(editingId) ?: return@launch
            originalNominal = h.nominal
            val existingItems = repo.getItems(editingId).map { e ->
                BahanBakuItemDraft(
                    id = e.id,
                    jenisBahan = e.jenisBahan,
                    kuantitas = e.kuantitas.toBigDecimal().stripTrailingZeros().toPlainString(),
                    unit = e.unit,
                    rate = e.rate.toBigDecimal().stripTrailingZeros().toPlainString()
                )
            }
            _ui.update {
                it.copy(
                    header = h,
                    items = existingItems.ifEmpty { listOf(BahanBakuItemDraft()) },
                    nominalInput = if (h.nominal > 0.0) h.nominal.toBigDecimal().stripTrailingZeros().toPlainString() else "",
                    notaFotoPath = h.notaFotoPath,
                    notaFotoUrl = h.notaFotoUrl,
                    fotoFileSizeKb = com.posbah.app.util.CameraUtils.fileSizeKb(context, h.notaFotoPath),
                    fotoUploadStatus = when {
                        !h.notaFotoUrl.isNullOrBlank() -> FotoUploadStatus.UPLOADED
                        !h.notaFotoPath.isNullOrBlank() -> FotoUploadStatus.LOCAL_SAVED
                        else -> FotoUploadStatus.IDLE
                    }
                )
            }
        }
    }

    fun updateHeader(transform: (BmpBahanBakuEntity) -> BmpBahanBakuEntity) {
        val h = _ui.value.header ?: return
        _ui.update { it.copy(header = transform(h)) }
    }

    fun updateNominalInput(input: String) {
        _ui.update { it.copy(nominalInput = input) }
    }

    fun addItem() {
        _ui.update { it.copy(items = it.items + BahanBakuItemDraft()) }
    }

    fun updateItem(index: Int, transform: (BahanBakuItemDraft) -> BahanBakuItemDraft) {
        val items = _ui.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = transform(items[index])
            _ui.update { it.copy(items = items) }
        }
    }

    fun removeItem(index: Int) {
        val items = _ui.value.items.toMutableList()
        if (items.size > 1 && index in items.indices) {
            items.removeAt(index)
            _ui.update { it.copy(items = items) }
        }
    }

    /**
     * Dipanggil setelah kamera berhasil mengambil foto.
     *
     * @param filePath Path absolut file foto dari [CameraUtils.createTempCameraFile]
     *
     * Alur:
     * 1. Kompres foto ke ≤100 KB menggunakan [CameraUtils.compressToMaxSize]
     * 2. Simpan path ke state (belum upload ke Cloudinary)
     * 3. Update header entity dengan notaFotoPath
     * 4. Coba upload ke Cloudinary (jika CLOUDINARY_ENABLED = true)
     */
    fun onPhotoCaptured(filePath: String) {
        viewModelScope.launch {
            _ui.update { it.copy(fotoUploadStatus = FotoUploadStatus.LOCAL_SAVED) }

            // Step 1: Kompres ke ≤100 KB di background thread
            val compressedFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.util.CameraUtils.compressToMaxSize(java.io.File(filePath), maxSizeKb = 100)
            }

            val sizeKb = com.posbah.app.util.CameraUtils.fileSizeKb(context, compressedFile.absolutePath)

            // Step 2: Pindahkan ke MediaStore (storage permanen — survive uninstall & clear storage)
            val persistentUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.posbah.app.util.CameraUtils.persistToMediaStore(context, compressedFile, tenantId)
            }

            if (persistentUri == null) {
                _ui.update { st ->
                    st.copy(
                        fotoUploadStatus = FotoUploadStatus.IDLE,
                        fotoUploadError = "Gagal menyimpan foto ke storage permanen"
                    )
                }
                return@launch
            }

            // Step 3: Update state lokal — simpan URI MediaStore (BUKAN file path lagi)
            _ui.update { st ->
                st.copy(
                    notaFotoPath = persistentUri.toString(),
                    fotoFileSizeKb = sizeKb,
                    fotoUploadStatus = FotoUploadStatus.LOCAL_SAVED,
                    fotoUploadError = null
                )
            }

            // Step 4: Cloudinary upload
            uploadToCloudinary(persistentUri)
        }
    }

    /**
     * Hapus foto nota yang sudah diambil.
     * Juga menghapus file dari storage lokal.
     */
    fun removePhoto() {
        val uri = _ui.value.notaFotoPath
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.posbah.app.util.CameraUtils.deleteMediaStoreUri(context, uri)
        }
        _ui.update {
            it.copy(
                notaFotoPath = null,
                notaFotoUrl = null,
                fotoFileSizeKb = 0L,
                fotoUploadStatus = FotoUploadStatus.IDLE,
                fotoUploadError = null
            )
        }
    }

    /**
     * Upload foto ke Cloudinary.
     * Saat ini disabled (CLOUDINARY_ENABLED = false di [CloudinaryUploader]).
     * Setelah user berikan credential, upload akan berjalan otomatis.
     */
    private fun uploadToCloudinary(uri: android.net.Uri) {
        uploadScope.launch {
            _ui.update { it.copy(fotoUploadStatus = FotoUploadStatus.UPLOADING) }

            val fileName = "NOTA_${_ui.value.header?.noTagihan ?: "UNKNOWN"}_${System.currentTimeMillis()}"
            val result = com.posbah.app.data.remote.CloudinaryUploader.upload(
                context = context,
                uri = uri,
                fileName = fileName,
                tenantId = tenantId,
                email = authRepo.activeUserEmail()
            )

            when (result) {
                is com.posbah.app.data.remote.CloudinaryUploader.UploadResult.Success -> {
                    _ui.update { st ->
                        st.copy(
                            notaFotoUrl = result.url,
                            fotoUploadStatus = FotoUploadStatus.UPLOADED,
                            fotoUploadError = null
                        )
                    }
                    // Simpan URL ke DB secara langsung jika data utama bahan baku sudah di-save sebelumnya
                    val noTagihan = _ui.value.header?.noTagihan
                    if (!noTagihan.isNullOrBlank()) {
                        val saved = repo.getByTagihan(tenantId, noTagihan)
                        if (saved != null) {
                            val updated = saved.copy(
                                notaFotoPath = _ui.value.notaFotoPath,
                                notaFotoUrl = result.url,
                                isSynced = false,
                                updatedAt = System.currentTimeMillis()
                            )
                            repo.updateHeaderOnly(updated)
                        }
                    }
                }
                is com.posbah.app.data.remote.CloudinaryUploader.UploadResult.Error -> {
                    _ui.update { st ->
                        st.copy(
                            fotoUploadStatus = FotoUploadStatus.LOCAL_SAVED, // kembali ke lokal
                            fotoUploadError = result.message
                        )
                    }
                }
                is com.posbah.app.data.remote.CloudinaryUploader.UploadResult.Disabled -> {
                    // Cloudinary belum dikonfigurasi — foto tetap tersimpan lokal
                    _ui.update { it.copy(fotoUploadStatus = FotoUploadStatus.LOCAL_SAVED) }
                }
            }
        }
    }

    fun save() {
        val h = _ui.value.header ?: return
        val draftItems = _ui.value.items
        if (draftItems.all { it.jenisBahan.isBlank() }) return

        val entities = draftItems
            .filter { it.jenisBahan.isNotBlank() }
            .map { d ->
                BmpBahanBakuItemEntity(
                    id = d.id,
                    tenantId = tenantId,
                    bahanBakuId = h.id,
                    jenisBahan = d.jenisBahan,
                    kuantitas = d.kuantitas.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    unit = d.unit,
                    rate = d.rate.replace(",", ".").toDoubleOrNull() ?: 0.0
                )
            }

        val total = entities.sumOf { it.kuantitas * it.rate }
        val enteredNominal = _ui.value.nominalInput.replace(",", ".").toDoubleOrNull()
        val finalNominal = if (enteredNominal != null) {
            enteredNominal.coerceAtLeast(0.0)
        } else {
            total
        }
        // Pastikan path & URL foto tersimpan ke entity
        val finalHeader = h.copy(
            totalHarga = total,
            nominal = finalNominal,
            notaFotoPath = _ui.value.notaFotoPath,
            notaFotoUrl = _ui.value.notaFotoUrl
        )

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, saveError = null) }
            // Optimistic: tandai saved dulu, lanjut ke background
            _ui.update { it.copy(isLoading = false, saved = true) }
            val result = if (finalHeader.id == 0L) {
                val (_, r) = repo.save(context, finalHeader, entities)
                r
            } else {
                repo.update(context, originalNominal, finalHeader, entities)
            }
            if (result is com.posbah.app.data.repository.OnlineWriteResult.Error) {
                // Rollback: kembali ke form dengan error
                _ui.update { it.copy(saved = false, saveError = result.message) }
            } else if (result is com.posbah.app.data.repository.OnlineWriteResult.NoConnection) {
                // Rollback: kembali ke form dengan error
                _ui.update { it.copy(saved = false, saveError = "Tidak ada koneksi internet. Data tidak tersimpan.") }
            }
        }
    }

    fun clearSaveError() { _ui.update { it.copy(saveError = null) } }
}
