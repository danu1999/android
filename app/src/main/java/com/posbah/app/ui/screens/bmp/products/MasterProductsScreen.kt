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
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.Formatters
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
import com.posbah.app.data.local.PosBahDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class MasterProductsViewModel @Inject constructor(
    private val repo: BmpMasterProductRepository,
    private val settingsRepo: com.posbah.app.data.repository.BmpSettingsRepository,
    private val authRepository: AuthRepository,
    private val db: PosBahDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    val products = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = settingsRepo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _latestRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val latestRates = _latestRates.asStateFlow()

    private val _distinctMaterials = MutableStateFlow<List<String>>(emptyList())
    val distinctMaterials = _distinctMaterials.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val materials = db.bmpBahanBakuItemDao().getDistinctBahanBaku(tenantId)
            val ratesMap = materials.associateWith { material ->
                db.bmpBahanBakuItemDao().getLatestRate(tenantId, material) ?: 0.0
            }
            _distinctMaterials.value = materials
            _latestRates.value = ratesMap
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

    fun save() = viewModelScope.launch {
        val e = _form.value.editing ?: return@launch
        if (e.title.isBlank()) return@launch
        repo.upsert(e)
        _form.update { FormState() }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.delete(id)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, tenantId)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
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
                                        Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
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
        var simHargaJual by remember { mutableStateOf("") }

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

        val simPriceVal = simHargaJual.toDoubleOrNull() ?: 0.0
        val simMarginPcs = if (hppRes != null) simPriceVal - hppRes.hppTotalPcs else 0.0
        val simMarginLusin = simMarginPcs * 12.0

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
                        OutlinedTextField(
                            value = simHargaJual,
                            onValueChange = { simHargaJual = it },
                            label = { Text("Simulasi Harga Jual / Pcs (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (simPriceVal > 0.0) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Simulasi Profit / Pcs", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Formatters.rupiah(simMarginPcs),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (simMarginPcs >= 0) Color(0xFF1F8B4C) else Color(0xFFC5453B)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Simulasi Profit / Lusin", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Formatters.rupiah(simMarginLusin),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (simMarginLusin >= 0) Color(0xFF1F8B4C) else Color(0xFFC5453B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::save, modifier = Modifier.testTag("btn-save-product")) {
                    Text("Simpan")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::closeForm) { Text("Batal") } }
        )
    }
}
