package com.posbah.app.ui.screens.bmp.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.IconButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.posbah.app.data.local.entities.BmpSettingsEntity
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpSettingsRepository
import com.posbah.app.data.repository.OutletRepository
import com.posbah.app.data.repository.EmployeeRepository
import com.posbah.app.data.repository.EmployeeData
import com.posbah.app.data.repository.BmpEmployeeRepository
import com.posbah.app.data.repository.OutletData
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

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: BmpSettingsRepository,
    private val outletRepo: OutletRepository,
    private val authRepository: AuthRepository,
    private val employeeRepo: EmployeeRepository,
    private val bmpEmployeeRepo: BmpEmployeeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    init {
        viewModelScope.launch {
            try { settingsRepo.refresh() } catch (_: Exception) {}
            try { employeeRepo.refresh() } catch (_: Exception) {}
            try { bmpEmployeeRepo.refresh() } catch (_: Exception) {}
        }
    }

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    private val _draft = MutableStateFlow<BmpSettingsEntity?>(null)
    val draft = _draft.asStateFlow()

    private val _draftTenant = MutableStateFlow<Tenant?>(null)
    val draftTenant = _draftTenant.asStateFlow()

    val outlets = outletRepo.observe(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<OutletData>())

    val posEmployees = employeeRepo.observeForTenant(tenantId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val data = settingsRepo.get()
            val existing = data?.let { d ->
                BmpSettingsEntity(
                    id = d.id,
                    tenantId = d.tenantId,
                    clientName = d.companyName,
                    clientLogo = d.logoUrl,
                    addressLine1 = d.address,
                    phoneNumber = d.phone,
                    emailAddress = d.email,
                    taxNumber = d.npwp,
                    updatedAt = d.updatedAt
                )
            }
            _draft.value = existing ?: BmpSettingsEntity(
                tenantId = tenantId,
                clientName = "Perusahaan Saya"
            )
            _draftTenant.value = authRepository.getTenantOnline(tenantId)
        }
        loadSyncSummary()
    }

    fun update(transform: (BmpSettingsEntity) -> BmpSettingsEntity) {
        val cur = _draft.value ?: return
        _draft.update { transform(cur) }
    }

    fun updateTenantName(name: String) {
        _draftTenant.update { it?.copy(name = name) }
        _draft.update { it?.copy(clientName = name) }
    }

    fun save() = viewModelScope.launch {
        val d = _draft.value ?: return@launch
        val t = _draftTenant.value
        val dataToSave = com.posbah.app.data.repository.BmpSettingsData(
            id = d.id,
            tenantId = d.tenantId,
            companyName = d.clientName,
            logoUrl = d.clientLogo,
            address = d.addressLine1,
            phone = d.phoneNumber,
            email = d.emailAddress,
            npwp = d.taxNumber,
            updatedAt = System.currentTimeMillis()
        )
        settingsRepo.save(dataToSave)
        if (t != null) {
            authRepository.updateTenantOnline(t.id, t.name)
        }
    }

    fun addOutlet(name: String, address: String?, phone: String?, employeeId: Long) = viewModelScope.launch {
        if (name.isBlank()) {
            _syncStatus.value = "Nama outlet tidak boleh kosong."
            return@launch
        }
        val activeEmployees = employeeRepo.list().filter { it.tenantId == tenantId && it.isActive }
        val emp = activeEmployees.firstOrNull { it.id == employeeId }
        if (emp == null) {
            _syncStatus.value = "Karyawan tidak ditemukan."
            return@launch
        }

        // Check if moving emp leaves old outlet with 0 staff
        if (emp.outletId != null) {
            val oldOutletCount = activeEmployees.count { it.outletId == emp.outletId }
            if (oldOutletCount <= 1) {
                val oldOutletName = outletRepo.getById(emp.outletId)?.name ?: "Outlet Lain"
                _syncStatus.value = "Gagal: ${emp.name} adalah karyawan terakhir di $oldOutletName."
                return@launch
            }
        }

        val newOutletId = outletRepo.create(name, address, phone)
        if (newOutletId > 0) {
            employeeRepo.update(emp.copy(outletId = newOutletId, updatedAt = System.currentTimeMillis()))
            _syncStatus.value = "Outlet berhasil ditambahkan."
        } else {
            _syncStatus.value = "Gagal membuat outlet."
        }
    }

    fun updateOutlet(o: OutletData) = viewModelScope.launch {
        outletRepo.update(o)
    }

    fun deleteOutlet(id: Long) = viewModelScope.launch {
        // Safe unlink for POS employees
        val posEmployees = employeeRepo.list().filter { it.outletId == id }
        posEmployees.forEach { emp ->
            employeeRepo.update(emp.copy(outletId = null, updatedAt = System.currentTimeMillis()))
        }

        outletRepo.delete(id)
        _syncStatus.value = "Outlet berhasil dihapus."
    }

    private val _syncSummary = MutableStateFlow<Map<String, Int>?>(null)
    val syncSummary = _syncSummary.asStateFlow()

    fun loadSyncSummary() = viewModelScope.launch(Dispatchers.IO) {
        val email = getActiveUserEmail()
        if (tenantId.isBlank() || email.isNullOrBlank()) return@launch
        try {
            val urlStr = "https://www.zedmz.cloud/api/sync-summary?tenantId=${java.net.URLEncoder.encode(tenantId, "UTF-8")}&email=${java.net.URLEncoder.encode(email, "UTF-8")}"
            val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = org.json.JSONObject(response)
                val countsObj = jsonObj.getJSONObject("counts")
                val map = mutableMapOf<String, Int>()
                val keys = countsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    map[key] = countsObj.getInt(key)
                }
                _syncSummary.value = map
            } else {
                android.util.Log.e("SettingsViewModel", "Failed to fetch sync summary: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun performSync(context: Context) = viewModelScope.launch {
        _syncStatus.value = "Menyegarkan data..."
        try {
            settingsRepo.refresh()
            employeeRepo.refresh()
            bmpEmployeeRepo.refresh()
            loadSyncSummary()
            _syncStatus.value = "Data berhasil diperbarui dari cloud!"
        } catch (e: Exception) {
            _syncStatus.value = "Gagal memperbarui data: ${e.localizedMessage}"
        }
    }

    fun getActiveUserEmail(): String? = authRepository.activeUserEmail()

    fun changePassword(oldPin: String, newPin: String, onResult: (AuthRepository.ChangePasswordResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.changePassword(oldPin, newPin)
            onResult(result)
        }
    }
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val posEmployees by viewModel.posEmployees.collectAsState()
    var selectedEmployeeId by remember { mutableStateOf<Long?>(null) }
    var selectedEmployeeName by remember { mutableStateOf("") }
    var employeeDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var outletToDelete by remember { mutableStateOf<OutletData?>(null) }


    var showChangePassword by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var isChangingPassword by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.update { entity -> entity.copy(clientLogo = it.toString()) }
        }
    }

    val d = draft
    if (d == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp, 60.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { logoPickerLauncher.launch("image/*") }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!d.clientLogo.isNullOrBlank()) {
                            AsyncImage(
                                model = d.clientLogo,
                                contentDescription = "Logo Perusahaan",
                                contentScale = ContentScale.Inside,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "Pilih Logo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column {
                        TextButton(
                            onClick = { logoPickerLauncher.launch("image/*") },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Unggah Logo")
                        }
                        Text(
                            text = "Rekomendasi: 240 x 120 px\nFormat PNG/JPG",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                val tenantDraft by viewModel.draftTenant.collectAsState()
                OutlinedTextField(
                    value = tenantDraft?.name ?: d.clientName,
                    onValueChange = { v -> viewModel.updateTenantName(v) },
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
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    label = "Ganti Password",
                    variant = ButtonVariant.Outline,
                    onClick = { showChangePassword = true },
                    modifier = Modifier.fillMaxWidth().testTag("btn-change-pwd")
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                val syncStatus by viewModel.syncStatus.collectAsState()
                val syncSummary by viewModel.syncSummary.collectAsState()

                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryButton(
                        label = "Sinkronisasi Cloud (VPS)",
                        onClick = { viewModel.performSync(context) },
                        modifier = Modifier.fillMaxWidth().testTag("btn-cloud-sync")
                    )
                    if (syncStatus != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = syncStatus!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncStatus!!.contains("berhasil")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("sync-status-text")
                        )
                    }
                    if (syncSummary != null) {
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Status Data Ter-upload di VPS:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                val displayMap = mapOf(
                                    "transactions" to "Transaksi POS",
                                    "bmp_invoices" to "Invoice B2B",
                                    "bmp_bahan_baku" to "Log Bahan Baku",
                                    "products" to "Produk POS",
                                    "customers" to "Pelanggan POS",
                                    "bmp_clients" to "Klien B2B",
                                    "bmp_employees" to "Karyawan BMP",
                                    "bmp_payrolls" to "Gaji / Payroll",
                                    "bmp_cashflow" to "Arus Kas (Bmp)",
                                    "bmp_settings" to "Profil Perusahaan",
                                    "outlets" to "Cabang / Outlet",
                                    "bmp_master_products" to "Master Produk (Bmp)"
                                )
                                displayMap.forEach { (table, displayName) ->
                                    val count = syncSummary!![table] ?: 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$count item", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OUTLET", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { showOutletForm = true; outletName = ""; outletAddress = ""; outletPhone = ""; selectedEmployeeId = null; selectedEmployeeName = "" },
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
                        TextButton(onClick = {
                            outletToDelete = o
                            showDeleteConfirmDialog = true
                        }) {
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
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
                    // Employee warning banner if empty
                    if (posEmployees.isEmpty()) {
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tambahkan karyawan outlet terlebih dahulu sebelum membuat outlet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

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
                    Spacer(Modifier.size(8.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedEmployeeName.isBlank()) "- Pilih Karyawan -" else selectedEmployeeName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Karyawan Utama Outlet") },
                            trailingIcon = {
                                IconButton(onClick = { employeeDropdownExpanded = true }) {
                                    Text("▾", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { employeeDropdownExpanded = true }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = employeeDropdownExpanded,
                            onDismissRequest = { employeeDropdownExpanded = false }
                        ) {
                            posEmployees.forEach { emp ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(emp.name) },
                                    onClick = {
                                        selectedEmployeeId = emp.id
                                        selectedEmployeeName = emp.name
                                        employeeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (outletName.isNotBlank() && selectedEmployeeId != null) {
                            viewModel.addOutlet(outletName, outletAddress.ifBlank { null }, outletPhone.ifBlank { null }, selectedEmployeeId!!)
                            showOutletForm = false
                        } else {
                            android.widget.Toast.makeText(context, "Nama outlet dan karyawan utama wajib diisi.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.testTag("btn-save-outlet")
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showOutletForm = false }) { Text("Batal") } }
        )
    }

    if (showChangePassword) {
        val userEmail = viewModel.getActiveUserEmail().orEmpty()
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { if (!isChangingPassword) { showChangePassword = false; oldPassword = ""; newPassword = ""; confirmPassword = ""; changePasswordError = null } },
            title = { Text("Ganti Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ubah password masuk Anda. Email tidak dapat diubah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Email Read-Only field
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = {},
                        label = { Text("Email (Gmail)") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Password Lama") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("change-pwd-old")
                    )
                    
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password Baru") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("change-pwd-new")
                    )
                    
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Password Baru") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("change-pwd-confirm")
                    )
                    
                    changePasswordError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isChangingPassword && oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                    onClick = {
                        if (newPassword != confirmPassword) {
                            changePasswordError = "Konfirmasi password baru tidak cocok"
                            return@TextButton
                        }
                        if (newPassword.length < 4) {
                            changePasswordError = "Password minimal 4 karakter"
                            return@TextButton
                        }
                        isChangingPassword = true
                        changePasswordError = null
                        viewModel.changePassword(oldPassword, newPassword) { result ->
                            isChangingPassword = false
                            when (result) {
                                is com.posbah.app.data.repository.AuthRepository.ChangePasswordResult.Success -> {
                                    showChangePassword = false
                                    oldPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                    android.widget.Toast.makeText(context, "Password berhasil diubah!", android.widget.Toast.LENGTH_LONG).show()
                                }
                                is com.posbah.app.data.repository.AuthRepository.ChangePasswordResult.Error -> {
                                    changePasswordError = result.message
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn-confirm-change-pwd")
                ) {
                    Text(if (isChangingPassword) "Memproses..." else "Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isChangingPassword,
                    onClick = {
                        showChangePassword = false
                        oldPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        changePasswordError = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteConfirmDialog && outletToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Outlet") },
            text = {
                Text("Apakah Anda yakin ingin menghapus outlet ${outletToDelete?.name}? Karyawan yang ditugaskan ke outlet ini akan dipindahkan ke penempatan 'Seluruh Outlet'.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        outletToDelete?.let { viewModel.deleteOutlet(it.id) }
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
