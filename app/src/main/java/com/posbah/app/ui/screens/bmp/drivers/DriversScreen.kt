package com.posbah.app.ui.screens.bmp.drivers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.posbah.app.data.repository.OnlineWriteResult
import com.posbah.app.data.remote.VpsImageUploader
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpDriverData
import com.posbah.app.data.repository.BmpDriverRepository
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.CameraUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DriversViewModel @Inject constructor(
    private val driverRepo: BmpDriverRepository,
    private val authRepo: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val tenantId = authRepo.activeTenantId().orEmpty()
    val drivers: StateFlow<List<BmpDriverData>> = driverRepo.drivers

    val isSubmitting = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            driverRepo.refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            driverRepo.refresh()
        }
    }

    suspend fun uploadDoc(file: File, docType: String): String? = withContext(Dispatchers.IO) {
        try {
            val tId = authRepo.activeTenantId()?.takeIf { it.isNotBlank() } ?: "ten_default"
            val token = authRepo.activeUserSub() ?: authRepo.activeUserEmail()
            val compressed = CameraUtils.compressToMaxSize(file, maxSizeKb = 250)
            val bytes = compressed.readBytes()
            VpsImageUploader.uploadDriverDocToVps(context, bytes, tId, docType, token)
        } catch (e: Exception) {
            android.util.Log.e("DriversViewModel", "Gagal upload dokumen sopir", e)
            null
        }
    }

    fun saveDriver(
        id: Long,
        name: String,
        phone: String,
        plateNumber: String,
        truckType: String,
        ktpUrl: String?,
        truckUrl: String?,
        stnkUrl: String?,
        notes: String?,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            isSubmitting.value = true
            errorMessage.value = null
            val res = if (id == 0L) {
                driverRepo.addDriver(
                    name = name,
                    phone = phone,
                    plateNumber = plateNumber,
                    truckType = truckType,
                    ktpImageUrl = ktpUrl,
                    truckImageUrl = truckUrl,
                    stnkImageUrl = stnkUrl,
                    notes = notes
                )
            } else {
                driverRepo.updateDriver(
                    id = id,
                    name = name,
                    phone = phone,
                    plateNumber = plateNumber,
                    truckType = truckType,
                    ktpImageUrl = ktpUrl,
                    truckImageUrl = truckUrl,
                    stnkImageUrl = stnkUrl,
                    notes = notes
                )
            }
            isSubmitting.value = false
            when (res) {
                is OnlineWriteResult.Success -> {
                    driverRepo.refresh()
                    onDone(true)
                }
                is OnlineWriteResult.Error -> {
                    errorMessage.value = res.message
                    onDone(false)
                }
                is OnlineWriteResult.NoConnection -> {
                    errorMessage.value = "Tidak ada koneksi internet."
                    onDone(false)
                }
            }
        }
    }

    fun deleteDriver(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            driverRepo.deleteDriver(id)
            onDone()
        }
    }
}

@Composable
fun DriversScreen(
    onBack: () -> Unit,
    viewModel: DriversViewModel = hiltViewModel()
) {
    val drivers by viewModel.drivers.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedDriverForEdit by remember { mutableStateOf<BmpDriverData?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var driverToDelete by remember { mutableStateOf<BmpDriverData?>(null) }
    var previewImageUrl by remember { mutableStateOf<Pair<String, String>?>(null) } // (Title, URL)

    val filteredDrivers = remember(drivers, searchQuery) {
        if (searchQuery.isBlank()) drivers
        else drivers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.plateNumber.contains(searchQuery, ignoreCase = true) ||
            it.truckType.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Master Sopir & Armada",
                subtitle = "${drivers.size} Sopir Terdaftar",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedDriverForEdit = null
                    showFormDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, contentDescription = "Tambah")
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Sopir", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama sopir, plat, atau jenis truk...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Hapus")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (filteredDrivers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = if (searchQuery.isBlank()) "Belum ada data sopir" else "Sopir tidak ditemukan",
                        description = if (searchQuery.isBlank()) "Klik tombol + Tambah Sopir untuk mencatat sopir dan dokumen armada" else "Coba cari dengan kata kunci lain"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = filteredDrivers, key = { it.id }) { driver: BmpDriverData ->
                        DriverCardItem(
                            driver = driver,
                            onEdit = {
                                selectedDriverForEdit = driver
                                showFormDialog = true
                            },
                            onDelete = {
                                driverToDelete = driver
                            },
                            onPreviewImage = { title, url ->
                                previewImageUrl = Pair(title, url)
                            },
                            onCallOrWa = { phone ->
                                val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
                                val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone"))
                                context.startActivity(intent)
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialog Form Tambah / Edit
    if (showFormDialog) {
        DriverFormDialog(
            driver = selectedDriverForEdit,
            isSubmitting = isSubmitting,
            onDismiss = { showFormDialog = false },
            onSave = { id, name, phone, plate, type, ktp, truck, stnk, notes ->
                viewModel.saveDriver(id, name, phone, plate, type, ktp, truck, stnk, notes) { success ->
                    if (success) {
                        showFormDialog = false
                    }
                }
            },
            onUploadDoc = { file, type ->
                viewModel.uploadDoc(file, type)
            }
        )
    }

    // Dialog Konfirmasi Hapus
    driverToDelete?.let { driver ->
        AlertDialog(
            onDismissRequest = { driverToDelete = null },
            title = { Text("Hapus Sopir?") },
            text = { Text("Apakah Anda yakin ingin menghapus data sopir \"${driver.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDriver(driver.id) {
                            driverToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { driverToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Preview Foto Dokumen
    previewImageUrl?.let { (title, url) ->
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { previewImageUrl = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}

@Composable
fun DriverCardItem(
    driver: BmpDriverData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPreviewImage: (String, String) -> Unit,
    onCallOrWa: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto Truk atau Icon Truk
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!driver.truckImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = driver.truckImageUrl,
                            contentDescription = "Foto Truk",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPreviewImage("Foto Truk - ${driver.name}", driver.truckImageUrl) },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = driver.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (driver.plateNumber.isNotBlank()) driver.plateNumber else "Plat Belum Diisi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E88E5)
                        )
                        if (driver.truckType.isNotBlank()) {
                            Text(" • ${driver.truckType}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // WhatsApp / Phone Shortcut Button
                if (driver.phone.isNotBlank()) {
                    IconButton(
                        onClick = { onCallOrWa(driver.phone) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            Icons.Outlined.Call,
                            contentDescription = "Hubungi",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Info No HP
            if (driver.phone.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("No. HP / WA:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(driver.phone, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Badges Dokumen (KTP, Truk, STNK)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentStatusChip(
                    title = "KTP",
                    imageUrl = driver.ktpImageUrl,
                    onClick = { url -> onPreviewImage("KTP - ${driver.name}", url) },
                    modifier = Modifier.weight(1f)
                )
                DocumentStatusChip(
                    title = "Foto Truk",
                    imageUrl = driver.truckImageUrl,
                    onClick = { url -> onPreviewImage("Foto Truk - ${driver.name}", url) },
                    modifier = Modifier.weight(1f)
                )
                DocumentStatusChip(
                    title = "STNK",
                    imageUrl = driver.stnkImageUrl,
                    onClick = { url -> onPreviewImage("STNK - ${driver.name}", url) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!driver.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Catatan: ${driver.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DocumentStatusChip(
    title: String,
    imageUrl: String?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDoc = !imageUrl.isNullOrBlank()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (hasDoc) Color(0xFFE8F5E9) else Color(0xFFEEEEEE),
        border = BorderStroke(1.dp, if (hasDoc) Color(0xFFA5D6A7) else Color(0xFFE0E0E0)),
        modifier = modifier.clickable(enabled = hasDoc) {
            imageUrl?.let { onClick(it) }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (hasDoc) Icons.Outlined.CheckCircle else Icons.Outlined.Clear,
                contentDescription = null,
                tint = if (hasDoc) Color(0xFF2E7D32) else Color(0xFF9E9E9E),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (hasDoc) FontWeight.Bold else FontWeight.Normal,
                color = if (hasDoc) Color(0xFF2E7D32) else Color(0xFF757575)
            )
        }
    }
}

@Composable
fun DriverFormDialog(
    driver: BmpDriverData?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, phone: String, plate: String, type: String, ktp: String?, truck: String?, stnk: String?, notes: String?) -> Unit,
    onUploadDoc: suspend (File, String) -> String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(driver?.name ?: "") }
    var phone by remember { mutableStateOf(driver?.phone ?: "") }
    var plateNumber by remember { mutableStateOf(driver?.plateNumber ?: "") }
    var truckType by remember { mutableStateOf(driver?.truckType ?: "") }
    var notes by remember { mutableStateOf(driver?.notes ?: "") }

    var ktpUrl by remember { mutableStateOf(driver?.ktpImageUrl) }
    var truckUrl by remember { mutableStateOf(driver?.truckImageUrl) }
    var stnkUrl by remember { mutableStateOf(driver?.stnkImageUrl) }

    var uploadingDocType by remember { mutableStateOf<String?>(null) } // "ktp", "truck", "stnk"
    var currentTargetDocType by remember { mutableStateOf("") }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Launcher Camera
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraFile != null && currentTargetDocType.isNotBlank()) {
            val type = currentTargetDocType
            val fileToUpload = tempCameraFile!!
            scope.launch {
                uploadingDocType = type
                val uploadedUrl = onUploadDoc(fileToUpload, type)
                if (uploadedUrl != null) {
                    when (type) {
                        "ktp" -> ktpUrl = uploadedUrl
                        "truck" -> truckUrl = uploadedUrl
                        "stnk" -> stnkUrl = uploadedUrl
                    }
                    android.widget.Toast.makeText(context, "Foto berhasil diunggah!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Gagal mengunggah foto. Silakan coba lagi.", android.widget.Toast.LENGTH_SHORT).show()
                }
                uploadingDocType = null
            }
        }
    }

    // Launcher Gallery
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && currentTargetDocType.isNotBlank()) {
            val type = currentTargetDocType
            scope.launch {
                uploadingDocType = type
                val file = CameraUtils.copyUriToTempFile(context, uri)
                if (file != null) {
                    val uploadedUrl = onUploadDoc(file, type)
                    if (uploadedUrl != null) {
                        when (type) {
                            "ktp" -> ktpUrl = uploadedUrl
                            "truck" -> truckUrl = uploadedUrl
                            "stnk" -> stnkUrl = uploadedUrl
                        }
                        android.widget.Toast.makeText(context, "Foto berhasil diunggah!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Gagal mengunggah foto. Silakan coba lagi.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Gagal memproses file foto galeri", android.widget.Toast.LENGTH_SHORT).show()
                }
                uploadingDocType = null
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && tempCameraFile != null) {
            val photoUri = CameraUtils.getFileProviderUri(context, tempCameraFile!!)
            photoUri.let { cameraLauncher.launch(it) }
        } else if (!granted) {
            android.widget.Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun takePhoto(type: String) {
        currentTargetDocType = type
        val file = CameraUtils.createTempCameraFile(context)
        tempCameraFile = file
        val photoUri = CameraUtils.getFileProviderUri(context, file)

        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            photoUri.let { cameraLauncher.launch(it) }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun pickFromGallery(type: String) {
        currentTargetDocType = type
        galleryLauncher.launch("image/*")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (driver == null) "Tambah Sopir & Armada" else "Edit Data Sopir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lengkapi data sopir dan foto dokumen kendaraan untuk arsip",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Sopir *") },
                    placeholder = { Text("Contoh: Pak Slamet") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. WhatsApp / HP *") },
                    placeholder = { Text("08123456789") },
                    leadingIcon = { Icon(Icons.Outlined.Call, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = plateNumber,
                        onValueChange = { plateNumber = it.uppercase() },
                        label = { Text("No. Plat Truk") },
                        placeholder = { Text("L 1234 AB") },
                        leadingIcon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = truckType,
                        onValueChange = { truckType = it },
                        label = { Text("Jenis Truk") },
                        placeholder = { Text("Engkel / CDD / L300") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text("Foto Dokumen Sopir & Kendaraan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Disimpan aman di internal sistem untuk verifikasi dan arsip", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(12.dp))

                // Upload Field 1: KTP
                DocUploadRow(
                    title = "Foto KTP Sopir",
                    imageUrl = ktpUrl,
                    isUploading = uploadingDocType == "ktp",
                    onCamera = { takePhoto("ktp") },
                    onGallery = { pickFromGallery("ktp") },
                    onRemove = { ktpUrl = null }
                )

                Spacer(Modifier.height(8.dp))

                // Upload Field 2: Truk
                DocUploadRow(
                    title = "Foto Truk / Kendaraan",
                    imageUrl = truckUrl,
                    isUploading = uploadingDocType == "truck",
                    onCamera = { takePhoto("truck") },
                    onGallery = { pickFromGallery("truck") },
                    onRemove = { truckUrl = null }
                )

                Spacer(Modifier.height(8.dp))

                // Upload Field 3: STNK
                DocUploadRow(
                    title = "Foto STNK Kendaraan",
                    imageUrl = stnkUrl,
                    isUploading = uploadingDocType == "stnk",
                    onCamera = { takePhoto("stnk") },
                    onGallery = { pickFromGallery("stnk") },
                    onRemove = { stnkUrl = null }
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    placeholder = { Text("Sopir langganan rute Jawa Timur, dll.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onSave(
                                    driver?.id ?: 0L,
                                    name.trim(),
                                    phone.trim(),
                                    plateNumber.trim(),
                                    truckType.trim(),
                                    ktpUrl,
                                    truckUrl,
                                    stnkUrl,
                                    notes.trim()
                                )
                            }
                        },
                        enabled = name.isNotBlank() && phone.isNotBlank() && !isSubmitting,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Simpan Data")
                    }
                }
            }
        }
    }
}

@Composable
fun DocUploadRow(
    title: String,
    imageUrl: String?,
    isUploading: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = if (isUploading) "Mengunggah..." else if (!imageUrl.isNullOrBlank()) "✓ Foto tersimpan" else "Belum ada foto",
                    fontSize = 11.sp,
                    color = if (!imageUrl.isNullOrBlank()) Color(0xFF2E7D32) else Color.Gray
                )
            }

            if (!imageUrl.isNullOrBlank()) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Hapus Foto", tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }

            IconButton(onClick = onCamera, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = "Kamera", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onGallery, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Image, contentDescription = "Galeri", modifier = Modifier.size(18.dp))
            }
        }
    }
}
