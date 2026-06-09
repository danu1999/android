package com.posbah.app.ui.screens.bmp.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.local.entities.Outlet
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpSettingsRepository
import com.posbah.app.data.repository.OutletRepository
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.ui.components.ButtonVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: BmpSettingsRepository,
    private val outletRepo: OutletRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    private val _draft = MutableStateFlow<BmpSettingsEntity?>(null)
    val draft = _draft.asStateFlow()

    val outlets = outletRepo.observe(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val existing = settingsRepo.get(tenantId)
            _draft.value = existing ?: BmpSettingsEntity(
                tenantId = tenantId,
                clientName = "Perusahaan Saya"
            )
        }
    }

    fun update(transform: (BmpSettingsEntity) -> BmpSettingsEntity) {
        val cur = _draft.value ?: return
        _draft.update { transform(cur) }
    }

    fun save() = viewModelScope.launch {
        val d = _draft.value ?: return@launch
        settingsRepo.upsert(d)
    }

    fun addOutlet(name: String, address: String?, phone: String?) = viewModelScope.launch {
        outletRepo.create(tenantId, name, address, phone)
    }
    fun updateOutlet(o: Outlet) = viewModelScope.launch { outletRepo.update(o) }
    fun deleteOutlet(id: Long) = viewModelScope.launch { outletRepo.delete(id) }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrintSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val draft by viewModel.draft.collectAsState()
    val outlets by viewModel.outlets.collectAsState()
    var showOutletForm by remember { mutableStateOf(false) }
    var outletName by remember { mutableStateOf("") }
    var outletAddress by remember { mutableStateOf("") }
    var outletPhone by remember { mutableStateOf("") }

    val d = draft ?: return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Pengaturan", subtitle = "Profil & Outlet", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("PROFIL PERUSAHAAN", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(
                    value = d.clientName,
                    onValueChange = { v -> viewModel.update { it.copy(clientName = v) } },
                    label = { Text("Nama Perusahaan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-name")
                )
            }
            item {
                OutlinedTextField(
                    value = d.addressLine1.orEmpty(),
                    onValueChange = { v -> viewModel.update { it.copy(addressLine1 = v.ifBlank { null }) } },
                    label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = d.phoneNumber.orEmpty(),
                    onValueChange = { v -> viewModel.update { it.copy(phoneNumber = v.ifBlank { null }) } },
                    label = { Text("Telepon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = d.emailAddress.orEmpty(),
                    onValueChange = { v -> viewModel.update { it.copy(emailAddress = v.ifBlank { null }) } },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = d.taxNumber.orEmpty(),
                    onValueChange = { v -> viewModel.update { it.copy(taxNumber = v.ifBlank { null }) } },
                    label = { Text("NPWP") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("PENGATURAN HPP MANUFAKTUR", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(
                    value = if (d.listrikBulanan == 0.0) "" else d.listrikBulanan.toLong().toString(),
                    onValueChange = { v ->
                        val n = v.replace(",", "").toDoubleOrNull() ?: 0.0
                        viewModel.update { it.copy(listrikBulanan = n) }
                    },
                    label = { Text("Listrik Bulanan (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("settings-listrik")
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (d.jumlahMesin == 0) "" else d.jumlahMesin.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull() ?: 0
                            viewModel.update { it.copy(jumlahMesin = n) }
                        },
                        label = { Text("Jumlah Mesin (Unit)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("settings-mesin")
                    )
                    OutlinedTextField(
                        value = if (d.jumlahKaryawan == 0) "" else d.jumlahKaryawan.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull() ?: 0
                            viewModel.update { it.copy(jumlahKaryawan = n) }
                        },
                        label = { Text("Jumlah Karyawan") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("settings-karyawan")
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (d.gajiHarian == 0.0) "" else d.gajiHarian.toLong().toString(),
                        onValueChange = { v ->
                            val n = v.replace(",", "").toDoubleOrNull() ?: 0.0
                            viewModel.update { it.copy(gajiHarian = n) }
                        },
                        label = { Text("Gaji Harian (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("settings-gaji")
                    )
                    OutlinedTextField(
                        value = if (d.hariKerjaSebulan == 0) "" else d.hariKerjaSebulan.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull() ?: 0
                            viewModel.update { it.copy(hariKerjaSebulan = n) }
                        },
                        label = { Text("Hari Kerja / Bulan") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("settings-hari-kerja")
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (d.biayaKarungPer1000 == 0.0) "" else d.biayaKarungPer1000.toLong().toString(),
                        onValueChange = { v ->
                            val n = v.replace(",", "").toDoubleOrNull() ?: 0.0
                            viewModel.update { it.copy(biayaKarungPer1000 = n) }
                        },
                        label = { Text("Biaya Karung / 1000 Pcs (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f).testTag("settings-karung")
                    )
                    OutlinedTextField(
                        value = if (d.hoursPerDay == 0) "" else d.hoursPerDay.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull() ?: 0
                            viewModel.update { it.copy(hoursPerDay = n) }
                        },
                        label = { Text("Jam Kerja / Hari") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f).testTag("settings-jam-kerja")
                    )
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    label = "Simpan Profil & Pengaturan",
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth().testTag("btn-save-settings")
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    label = "Pengaturan Cetak Invoice & Struk",
                    variant = ButtonVariant.Outline,
                    onClick = onNavigateToPrintSettings,
                    modifier = Modifier.fillMaxWidth().testTag("btn-print-settings")
                )
            }
            item {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OUTLET", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { showOutletForm = true; outletName = ""; outletAddress = ""; outletPhone = "" },
                        modifier = Modifier.testTag("btn-add-outlet")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("Outlet Baru")
                    }
                }
            }
            items(outlets, key = { it.id }) { o ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().testTag("outlet-${o.id}")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Storefront, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                o.name + if (o.isDefault) " · Utama" else "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                o.address ?: o.phone ?: "Tanpa detail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!o.isDefault) {
                            TextButton(onClick = { viewModel.deleteOutlet(o.id) }) {
                                Text("Hapus", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOutletForm) {
        AlertDialog(
            onDismissRequest = { showOutletForm = false },
            title = { Text("Outlet Baru") },
            text = {
                Column {
                    OutlinedTextField(value = outletName, onValueChange = { outletName = it },
                        label = { Text("Nama outlet") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("outlet-name"))
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(value = outletAddress, onValueChange = { outletAddress = it },
                        label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(value = outletPhone, onValueChange = { outletPhone = it },
                        label = { Text("Telepon") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (outletName.isNotBlank()) {
                            viewModel.addOutlet(outletName, outletAddress.ifBlank { null }, outletPhone.ifBlank { null })
                            showOutletForm = false
                        }
                    }, modifier = Modifier.testTag("btn-save-outlet")
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showOutletForm = false }) { Text("Batal") } }
        )
    }
}
