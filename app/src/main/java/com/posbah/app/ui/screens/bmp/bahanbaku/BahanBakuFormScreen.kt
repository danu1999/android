package com.posbah.app.ui.screens.bmp.bahanbaku

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.CameraUtils
import com.posbah.app.util.Formatters
import java.io.File

private val JENIS_BAHAN_OPTIONS = listOf("PP", "PE", "HDPE", "LDPE", "PVC", "ABS", "PS", "Lainnya")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BahanBakuFormScreen(
    onDone: () -> Unit,
    viewModel: BahanBakuFormViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(ui.saved) {
        if (ui.saved) onDone()
    }

    val header = ui.header ?: return

    // ── State untuk file kamera ──────────────────────────────────────────────
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDenied by remember { mutableStateOf(false) }

    // Launcher kamera — ambil foto ke URI sementara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.absolutePath?.let { path ->
                viewModel.onPhotoCaptured(path)
            }
        }
        // Jika false (batal), biarkan state tidak berubah
    }

    // Launcher permisi kamera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permisi diberikan — buka kamera
            val file = CameraUtils.createTempCameraFile(context)
            tempCameraFile = file
            photoUri = CameraUtils.getFileProviderUri(context, file)
            photoUri?.let { cameraLauncher.launch(it) }
        } else {
            showPermissionDenied = true
        }
    }

    // Fungsi buka kamera
    fun openCamera() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val file = CameraUtils.createTempCameraFile(context)
            tempCameraFile = file
            photoUri = CameraUtils.getFileProviderUri(context, file)
            photoUri?.let { cameraLauncher.launch(it) }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = if (header.id == 0L) "Catat Bahan Baku" else "Edit Bahan Baku",
                subtitle = header.noTagihan,
                onBack = onDone
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── Header Section ───
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "INFORMASI TAGIHAN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = header.noTagihan,
                            onValueChange = { viewModel.updateHeader { h -> h.copy(noTagihan = it) } },
                            label = { Text("No. Tagihan / Surat Jalan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("bb-no-tagihan")
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = header.supplier ?: "",
                            onValueChange = { viewModel.updateHeader { h -> h.copy(supplier = it.ifBlank { null }) } },
                            label = { Text("Pemasok / Supplier") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("bb-supplier")
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = ui.nominalInput,
                            onValueChange = { viewModel.updateNominalInput(it) },
                            label = { Text("Nominal Bayar Kas (Rp)") },
                            supportingText = { Text("Sisa akan dicatat sebagai hutang supplier") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("bb-nominal")
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = header.notes ?: "",
                            onValueChange = { viewModel.updateHeader { h -> h.copy(notes = it.ifBlank { null }) } },
                            label = { Text("Catatan (Opsional)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("bb-notes")
                        )
                    }
                }
            }

            // ─── Seksi Foto Nota ───────────────────────────────────────────────────
            item {
                FotoNotaSection(
                    uiState = ui,
                    onOpenCamera = ::openCamera,
                    onRemovePhoto = viewModel::removePhoto
                )
            }

            // ─── Items Section Header ───
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "DETAIL BAHAN BAKU",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Total: ${Formatters.rupiah(ui.totalHarga)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = { viewModel.addItem() },
                        modifier = Modifier.testTag("btn-add-bb-item")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Tambah Bahan")
                    }
                }
            }

            // ─── Item Lines ───
            itemsIndexed(ui.items) { index, item ->
                BahanBakuItemRow(
                    item = item,
                    index = index,
                    canDelete = ui.items.size > 1,
                    onUpdate = { viewModel.updateItem(index, it) },
                    onRemove = { viewModel.removeItem(index) }
                )
            }

            // ─── Save Button ───
            item {
                Spacer(Modifier.height(8.dp))
                if (ui.isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    PrimaryButton(
                        label = if (header.id == 0L) "Simpan Transaksi" else "Perbarui Transaksi",
                        onClick = { viewModel.save() },
                        modifier = Modifier.fillMaxWidth().testTag("btn-save-bb")
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Pesan permisi ditolak
    if (showPermissionDenied) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            title = { Text("Izin Kamera Diperlukan") },
            text = { Text("Aplikasi memerlukan izin kamera untuk mengambil foto nota bahan baku. Buka Pengaturan → Aplikasi → POSBah → Izin → Kamera.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDenied = false }) { Text("Mengerti") }
            }
        )
    }
}

// ── Composable Foto Nota ─────────────────────────────────────────────────────

@Composable
private fun FotoNotaSection(
    uiState: BahanBakuFormUiState,
    onOpenCamera: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "FOTO NOTA BAHAN BAKU",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                // Status badge
                when (uiState.fotoUploadStatus) {
                    FotoUploadStatus.UPLOADING -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(4.dp))
                        Text("Mengupload…", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    FotoUploadStatus.UPLOADED -> {
                        Icon(Icons.Outlined.CheckCircle, null,
                            tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Cloud", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF22C55E))
                    }
                    FotoUploadStatus.LOCAL_SAVED -> {
                        Icon(Icons.Outlined.CloudUpload, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Lokal • ${uiState.fotoFileSizeKb} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {}
                }
            }
            Spacer(Modifier.height(12.dp))

            if (uiState.hasLocalPhoto) {
                // Preview foto
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = uiState.notaFotoUrl.takeIf { !it.isNullOrBlank() }
                            ?: uiState.notaFotoPath?.takeIf { path ->
                                if (path.startsWith("content://")) true else File(path).exists()
                            },
                        contentDescription = "Foto nota bahan baku",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .aspectRatio(4f / 3f)
                    )
                    // Tombol hapus foto
                    IconButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                RoundedCornerShape(50)
                            )
                            .size(32.dp)
                            .testTag("btn-remove-photo")
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Hapus foto",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Info ukuran & URL
                Spacer(Modifier.height(8.dp))
                if (uiState.hasCloudPhoto) {
                    Text(
                        "✅ Tersimpan di cloud",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF22C55E)
                    )
                } else {
                    Text(
                        "📱 Tersimpan lokal (${uiState.fotoFileSizeKb} KB) — akan diupload saat online",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tombol ganti foto
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onOpenCamera,
                    modifier = Modifier.testTag("btn-retake-photo")
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Ambil Ulang Foto")
                }

                // Pesan error upload (jika ada)
                uiState.fotoUploadError?.let { err ->
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }

            } else {
                // Placeholder — belum ada foto
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onOpenCamera() }
                        .testTag("btn-open-camera")
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = "Ambil foto nota",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Foto Nota / Surat Jalan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Ketuk untuk ambil foto\nFoto akan dikompres otomatis ≤ 100 KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Composable Item Row ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BahanBakuItemRow(
    item: BahanBakuItemDraft,
    index: Int,
    canDelete: Boolean,
    onUpdate: ((BahanBakuItemDraft) -> BahanBakuItemDraft) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("bb-item-row-$index")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Item ${index + 1}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (item.subtotal > 0) {
                    Text(
                        Formatters.rupiah(item.subtotal),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (canDelete) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp).testTag("btn-remove-bb-item-$index")
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Hapus item",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Jenis Bahan dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = item.jenisBahan,
                    onValueChange = { onUpdate { d -> d.copy(jenisBahan = it) } },
                    label = { Text("Jenis Bahan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor().testTag("bb-jenis-$index")
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    JENIS_BAHAN_OPTIONS.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                onUpdate { d -> d.copy(jenisBahan = opt) }
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.kuantitas,
                    onValueChange = { onUpdate { d -> d.copy(kuantitas = it) } },
                    label = { Text("Kuantitas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.2f).testTag("bb-kuantitas-$index")
                )
                OutlinedTextField(
                    value = item.unit,
                    onValueChange = { onUpdate { d -> d.copy(unit = it) } },
                    label = { Text("Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f).testTag("bb-unit-$index")
                )
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = item.rate,
                onValueChange = { onUpdate { d -> d.copy(rate = it) } },
                label = { Text("Rate (Rp/Kg)") },
                supportingText = if (item.subtotal > 0) {
                    { Text("Subtotal: ${Formatters.rupiah(item.subtotal)}") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("bb-rate-$index")
            )
        }
    }
}
