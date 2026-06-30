package com.posbah.app.ui.screens.bmp.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpMasterProductEntity
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpMasterProductRepository
import com.posbah.app.data.repository.OnlineWriteResult
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.Formatters
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.width
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class MasterProductsViewModel @Inject constructor(
    private val repo: BmpMasterProductRepository,
    private val settingsRepo: com.posbah.app.data.repository.BmpSettingsRepository,
    private val authRepository: AuthRepository,
    private val bahanBakuRepo: com.posbah.app.data.repository.BmpBahanBakuRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    val products = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = settingsRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _latestRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val latestRates = _latestRates.asStateFlow()

    private val _distinctMaterials = MutableStateFlow<List<String>>(emptyList())
    val distinctMaterials = _distinctMaterials.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    repo.refresh()
                    val ratesMap = bahanBakuRepo.getLatestMaterialRates(tenantId)
                    _distinctMaterials.value = ratesMap.keys.toList()
                    _latestRates.value = ratesMap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(12_000)
            }
        }
    }

    data class FormState(
        val editing: BmpMasterProductEntity? = null,
        val show: Boolean = false
    )

    private val _form = MutableStateFlow(FormState())
    val form = _form.asStateFlow()

    fun openCreate() = _form.update {
        FormState(editing = BmpMasterProductEntity(tenantId = tenantId, title = ""), show = true)
    }
    fun openEdit(p: BmpMasterProductEntity) = _form.update { FormState(editing = p, show = true) }
    fun closeForm() = _form.update { FormState() }
    fun updateField(transform: (BmpMasterProductEntity) -> BmpMasterProductEntity) {
        val cur = _form.value.editing ?: return
        _form.update { it.copy(editing = transform(cur)) }
    }

    fun save(imageFile: java.io.File?, keepExistingImage: Boolean) = viewModelScope.launch {
        val e = _form.value.editing ?: return@launch
        if (e.title.isBlank()) return@launch
        val isNew = e.id == 0L
        var base64Url = if (keepExistingImage) e.image else null
        if (imageFile != null && imageFile.exists()) {
            try {
                val compressed = com.posbah.app.util.CameraUtils.compressToMaxSize(imageFile, 80)
                val bytes = compressed.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                base64Url = "data:image/jpeg;base64,$base64"
                try { compressed.delete() } catch(ex: Exception) {}
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        val s = settings.value
        val rates = latestRates.value
        val computed = if (s != null) {
            val totalGaji = s.jumlahKaryawan * s.gajiHarian * s.hariKerjaSebulan
            val overheadBulanan = s.listrikBulanan + totalGaji
            val totalDetikSebulan = s.jumlahMesin * s.hariKerjaSebulan * s.hoursPerDay * 3600.0
            val biayaPerDetik = if (totalDetikSebulan > 0) overheadBulanan / totalDetikSebulan else 0.0
            val biayaMesin = e.cycleTime * biayaPerDetik
            
            val bahanRate = if (e.jenisBahanBaku.isNotEmpty()) {
                rates[e.jenisBahanBaku] ?: e.price
            } else {
                e.price
            }

            val hppSatuan = (e.beratGram * (bahanRate / 1000.0) + biayaMesin) * (1.0 + (e.rejectRate / 100.0))
            val biayaKemasanPcs = s.biayaKarungPer1000 / 1000.0
            val hppTotalPcs = hppSatuan + biayaKemasanPcs
            val hppLusin = hppTotalPcs * 12.0
            Pair(hppTotalPcs, hppLusin)
        } else {
            Pair(0.0, 0.0)
        }
        val finalProduct = e.copy(
            image = base64Url,
            hppTotalPcs = computed.first,
            hppLusin = computed.second
        )
        val data = com.posbah.app.data.repository.BmpMasterProductData(
            id = finalProduct.id,
            tenantId = finalProduct.tenantId,
            title = finalProduct.title,
            description = finalProduct.description,
            unit = finalProduct.unit,
            price = finalProduct.price,
            beratGram = finalProduct.beratGram,
            cycleTime = finalProduct.cycleTime,
            cavity = finalProduct.cavity,
            rejectRate = finalProduct.rejectRate,
            uniqueID = finalProduct.uniqueID ?: java.util.UUID.randomUUID().toString(),
            slug = finalProduct.slug ?: finalProduct.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
            jenisBahanBaku = finalProduct.jenisBahanBaku,
            image = finalProduct.image,
            isDeleted = finalProduct.isDeleted,
            updatedAt = System.currentTimeMillis()
        )
        // Optimistic: tutup form langsung sebelum menunggu network
        _form.update { FormState() }
        val result = repo.upsert(data)
        when (result) {
            is OnlineWriteResult.Success -> {
                if (isNew) {
                    logActivity("TAMBAH PRODUK BMP", "Menambahkan master produk: ${e.title} (Harga: Rp ${e.price})")
                } else {
                    logActivity("EDIT PRODUK BMP", "Mengubah master produk: ${e.title} (Harga: Rp ${e.price})")
                }
                Toast.makeText(context, "Produk berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
            is OnlineWriteResult.Error -> {
                // Rollback: buka kembali form dengan data yang sama
                _form.update { FormState(editing = e, show = true) }
                _error.value = result.message
                Toast.makeText(context, "Gagal menyimpan: ${result.message}", Toast.LENGTH_LONG).show()
            }
            is OnlineWriteResult.NoConnection -> {
                // Rollback: buka kembali form dengan data yang sama
                _form.update { FormState(editing = e, show = true) }
                _error.value = "Tidak ada koneksi internet. Data tidak tersimpan."
                Toast.makeText(context, "Tidak ada koneksi internet. Data tidak tersimpan.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        // Ambil nama produk dari state lokal (tidak perlu network call)
        val productName = products.value.find { it.id == id }?.title ?: "#$id"
        _error.value = null
        val result = repo.delete(id)
        when (result) {
            is OnlineWriteResult.Success -> {
                logActivity("HAPUS PRODUK BMP", "Menghapus master produk: $productName")
                Toast.makeText(context, "Produk berhasil dihapus!", Toast.LENGTH_SHORT).show()
            }
            is OnlineWriteResult.Error -> {
                _error.value = result.message
                Toast.makeText(context, "Gagal menghapus: ${result.message}", Toast.LENGTH_LONG).show()
            }
            is OnlineWriteResult.NoConnection -> {
                _error.value = "Tidak ada koneksi internet. Hapus dibatalkan."
                Toast.makeText(context, "Tidak ada koneksi internet. Hapus dibatalkan.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun logActivity(action: String, description: String) {
        // No-op in full online mode
    }
}

data class HppResult(
    val overheadBulanan: Double,
    val outputJamPcs: Double,
    val outputJamLusin: Double,
    val hppTotalPcs: Double,
    val hppLusin: Double
)

@Composable
fun MasterProductsScreen(
    onBack: () -> Unit,
    viewModel: MasterProductsViewModel = hiltViewModel()
) {
    val list by viewModel.products.collectAsState()
    val form by viewModel.form.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    var tempPhotoFile by remember { mutableStateOf<java.io.File?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<java.io.File?>(null) }

    LaunchedEffect(form.show, form.editing?.id) {
        if (form.show) {
            capturedPhotoFile = null
            tempPhotoFile = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            capturedPhotoFile = tempPhotoFile
        }
    }

    val launchCamera = {
        try {
            val file = com.posbah.app.util.CameraUtils.createTempCameraFile(context)
            tempPhotoFile = file
            val uri = com.posbah.app.util.CameraUtils.getFileProviderUri(context, file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka kamera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = com.posbah.app.util.CameraUtils.copyUriToTempFile(context, it)
            if (file != null) {
                capturedPhotoFile = file
            } else {
                Toast.makeText(context, "Gagal memuat gambar dari galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchGallery = {
        galleryLauncher.launch("image/*")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Master Produk", subtitle = "${list.size} item", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-add-product")
            ) { Icon(Icons.Outlined.Add, contentDescription = "Tambah") }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (list.isEmpty()) {
                EmptyState(
                    "Belum ada master produk",
                    "Tambah produk pertama untuk mempercepat pembuatan invoice",
                    "+ Tambah Produk",
                    onAction = viewModel::openCreate
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.id }) { p ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.openEdit(p) }
                                .testTag("product-${p.id}")
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (!p.image.isNullOrBlank()) {
                                            AsyncImage(
                                                model = decodeBase64Image(p.image),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${Formatters.rupiah(p.price)} / ${p.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { viewModel.delete(p.id) }) {
                                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (form.show && form.editing != null) {
        val e = form.editing!!
        val settings by viewModel.settings.collectAsState()
        var simHargaJualPcs by remember { mutableStateOf("") }
        var simHargaJualLusin by remember { mutableStateOf("") }

        val latestRates by viewModel.latestRates.collectAsState()
        val hppRes = remember(e, settings, latestRates) {
            settings?.let { s ->
                val totalGaji = s.jumlahKaryawan * s.gajiHarian * s.hariKerjaSebulan
                val overheadBulanan = s.listrikBulanan + totalGaji
                val totalDetikSebulan = s.jumlahMesin * s.hariKerjaSebulan * s.hoursPerDay * 3600.0
                val biayaPerDetik = if (totalDetikSebulan > 0) overheadBulanan / totalDetikSebulan else 0.0
                val biayaMesin = e.cycleTime * biayaPerDetik
                
                val bahanRate = if (e.jenisBahanBaku.isNotEmpty()) {
                    latestRates[e.jenisBahanBaku] ?: e.price
                } else {
                    e.price
                }

                val hppSatuan = (e.beratGram * (bahanRate / 1000.0) + biayaMesin) * (1.0 + (e.rejectRate / 100.0))
                val biayaKemasanPcs = s.biayaKarungPer1000 / 1000.0
                val hppTotalPcs = hppSatuan + biayaKemasanPcs
                val hppLusin = hppTotalPcs * 12.0
                val outputJamPcs = if (e.cycleTime > 0) (3600.0 * e.cavity) / e.cycleTime else 0.0
                val outputJamLusin = outputJamPcs / 12.0

                HppResult(
                    overheadBulanan = overheadBulanan,
                    outputJamPcs = outputJamPcs,
                    outputJamLusin = outputJamLusin,
                    hppTotalPcs = hppTotalPcs,
                    hppLusin = hppLusin
                )
            }
        }

        val simPricePcsVal = simHargaJualPcs.toDoubleOrNull() ?: 0.0
        val simPriceLusinVal = simHargaJualLusin.toDoubleOrNull() ?: 0.0
        // Kalkulasi simulasi: profit dari input per pcs
        val simMarginPcs = if (hppRes != null && simPricePcsVal > 0) simPricePcsVal - hppRes.hppTotalPcs else 0.0
        val simMarginLusinFromPcs = simMarginPcs * 12.0
        // Kalkulasi simulasi: profit dari input per lusin (1 lusin = 12 pcs)
        val simMarginLusin = if (hppRes != null && simPriceLusinVal > 0) simPriceLusinVal - hppRes.hppLusin else 0.0
        val simMarginPcsFromLusin = if (simPriceLusinVal > 0) simMarginLusin / 12.0 else 0.0

        AlertDialog(
            onDismissRequest = viewModel::closeForm,
            title = { Text(if (e.id == 0L) "Produk Baru" else "Edit Produk") },
            text = {
                val materials by viewModel.distinctMaterials.collectAsState()
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = e.title,
                        onValueChange = { v -> viewModel.updateField { it.copy(title = v) } },
                        label = { Text("Nama produk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input-product-name")
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = e.jenisBahanBaku,
                        onValueChange = { v -> viewModel.updateField { it.copy(jenisBahanBaku = v) } },
                        label = { Text("Jenis Bahan Baku (contoh: PP, HDPE)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (materials.isNotEmpty()) {
                        Spacer(Modifier.size(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            materials.take(5).forEach { mat ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.clickable {
                                        viewModel.updateField { it.copy(jenisBahanBaku = mat) }
                                    }
                                ) {
                                    Text(
                                        mat,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = e.unit,
                            onValueChange = { v -> viewModel.updateField { it.copy(unit = v) } },
                            label = { Text("Unit") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (e.price == 0.0) "" else e.price.toLong().toString(),
                            onValueChange = { v ->
                                val n = v.replace(",", "").toDoubleOrNull() ?: 0.0
                                viewModel.updateField { it.copy(price = n) }
                            },
                            label = { Text("Harga Bahan / Kg (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                    val bahanRate = if (e.jenisBahanBaku.isNotEmpty()) {
                        latestRates[e.jenisBahanBaku] ?: e.price
                    } else {
                        e.price
                    }
                    val isDynamic = e.jenisBahanBaku.isNotEmpty() && latestRates.containsKey(e.jenisBahanBaku)
                    if (isDynamic) {
                        Text(
                            "Menggunakan harga beli terakhir untuk '${e.jenisBahanBaku}': Rp ${Formatters.number(bahanRate)}/Kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = if (e.beratGram == 0.0) "" else e.beratGram.toString(),
                            onValueChange = { v ->
                                val n = v.replace(",", ".").toDoubleOrNull() ?: 0.0
                                viewModel.updateField { it.copy(beratGram = n) }
                            },
                            label = { Text("Berat (Gram)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (e.cycleTime == 0.0) "" else e.cycleTime.toString(),
                            onValueChange = { v ->
                                val n = v.replace(",", ".").toDoubleOrNull() ?: 0.0
                                viewModel.updateField { it.copy(cycleTime = n) }
                            },
                            label = { Text("Cycle Time (Detik)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = if (e.cavity == 0) "" else e.cavity.toString(),
                            onValueChange = { v ->
                                val n = v.toIntOrNull() ?: 1
                                viewModel.updateField { it.copy(cavity = n) }
                            },
                            label = { Text("Cavity") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (e.rejectRate == 0.0) "" else e.rejectRate.toString(),
                            onValueChange = { v ->
                                val n = v.replace(",", ".").toDoubleOrNull() ?: 0.0
                                viewModel.updateField { it.copy(rejectRate = n) }
                            },
                            label = { Text("Reject Rate (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = e.description.orEmpty(),
                        onValueChange = { v -> viewModel.updateField { it.copy(description = v.ifBlank { null }) } },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Foto Produk (Opsional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(4.dp))
                    if (capturedPhotoFile != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = capturedPhotoFile,
                                contentDescription = "Preview Foto Baru",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { capturedPhotoFile = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else if (!e.image.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = decodeBase64Image(e.image),
                                contentDescription = "Foto Produk Saat Ini",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.updateField { it.copy(image = null) } },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = launchCamera,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Ganti Kamera", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = launchGallery,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Ganti Galeri", fontSize = 11.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = launchCamera,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Kamera", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = launchGallery,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Galeri", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))

                    if (hppRes != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "RINGKASAN HPP LIVE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Overhead Bulanan", style = MaterialTheme.typography.bodySmall)
                                    Text(Formatters.rupiah(hppRes.overheadBulanan), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Output / Jam", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${Formatters.number(hppRes.outputJamPcs)} pcs (${Formatters.number(hppRes.outputJamLusin)} lsn)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Divider(modifier = Modifier.padding(vertical = 6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("HPP per Pcs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(Formatters.rupiah(hppRes.hppTotalPcs), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("HPP per Lusin", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(Formatters.rupiah(hppRes.hppLusin), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        // ── Simulasi Harga Jual Per Pcs ──
                        OutlinedTextField(
                            value = simHargaJualPcs,
                            onValueChange = {
                                simHargaJualPcs = it
                                // Auto-sync ke input lusin
                                val pcsVal = it.toDoubleOrNull()
                                if (pcsVal != null) simHargaJualLusin = (pcsVal * 12.0).toLong().toString()
                            },
                            label = { Text("Simulasi Harga Jual / Pcs (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = simHargaJualLusin,
                            onValueChange = {
                                simHargaJualLusin = it
                                // Auto-sync ke input pcs
                                val lusinVal = it.toDoubleOrNull()
                                if (lusinVal != null) simHargaJualPcs = (lusinVal / 12.0).toLong().toString()
                            },
                            label = { Text("Simulasi Harga Jual / Lusin (Rp) — 1 Lusin = 12 Pcs") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // ── Hasil Simulasi ──
                        if (simPricePcsVal > 0.0 || simPriceLusinVal > 0.0) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (simMarginPcs >= 0 && simPricePcsVal > 0) Color(0xFF1F8B4C).copy(alpha = 0.1f)
                                        else if (simMarginLusin >= 0 && simPriceLusinVal > 0) Color(0xFF1F8B4C).copy(alpha = 0.1f)
                                        else Color(0xFFC5453B).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (simPricePcsVal > 0.0) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Profit / Pcs", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                Formatters.rupiah(simMarginPcs),
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (simMarginPcs >= 0) Color(0xFF1F8B4C) else Color(0xFFC5453B)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Profit / Lusin (×12)", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                Formatters.rupiah(simMarginLusinFromPcs),
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (simMarginLusinFromPcs >= 0) Color(0xFF1F8B4C) else Color(0xFFC5453B)
                                            )
                                        }
                                    }
                                    if (simPriceLusinVal > 0.0 && simPricePcsVal == 0.0) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Profit / Lusin", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                Formatters.rupiah(simMarginLusin),
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (simMarginLusin >= 0) Color(0xFF1F8B4C) else Color(0xFFC5453B)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Profit / Pcs (÷12)", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                Formatters.rupiah(simMarginPcsFromLusin),
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (simMarginPcsFromLusin >= 0) Color(0xFF1F8B4C) else Color(0xFFC5453B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val keepExisting = capturedPhotoFile == null && !(e.image.isNullOrBlank())
                        viewModel.save(capturedPhotoFile, keepExisting)
                    },
                    modifier = Modifier.testTag("btn-save-product")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::closeForm) { Text("Batal") } }
        )
    }
}

private fun decodeBase64Image(imageStr: String?): Any? {
    if (imageStr.isNullOrBlank()) return null
    if (imageStr.startsWith("data:image")) {
        val base64Data = imageStr.substringAfter("base64,")
        return try {
            android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            imageStr
        }
    }
    return imageStr
}
