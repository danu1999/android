package com.posbah.app.ui.screens.bmp.bahanbaku

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.BmpBahanBakuEntity
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.StatChip
import com.posbah.app.util.Formatters
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun BahanBakuListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: BahanBakuListViewModel = hiltViewModel()
) {
    val list by viewModel.list.collectAsState()
    val totalHarga by viewModel.totalHarga.collectAsState()
    val totalNominal by viewModel.totalNominal.collectAsState()

    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }
    var payDebtTarget by remember { mutableStateOf<BmpBahanBakuEntity?>(null) }
    var payAmount by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Bahan Baku",
                subtitle = "${list.size} transaksi",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-add-bahanbaku")
            ) { Icon(Icons.Outlined.Add, contentDescription = "Tambah") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary stats
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Total Nilai Bahan",
                            value = Formatters.rupiah(totalHarga),
                            accent = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Total Dibayar",
                            value = Formatters.rupiah(totalNominal),
                            accent = Color(0xFFEF4444)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                StatChip(
                    label = "Sisa Hutang Supplier",
                    value = Formatters.rupiah(totalHarga - totalNominal),
                    accent = if (totalHarga - totalNominal > 0)
                        MaterialTheme.colorScheme.error
                    else Color(0xFF22C57E)
                )
            }

            if (list.isEmpty()) {
                item {
                    Spacer(Modifier.height(40.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Belum ada transaksi bahan baku",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tekan + untuk mencatat pembelian bahan baru",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "TRANSAKSI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(list, key = { it.id }) { entry ->
                    BahanBakuCard(
                        entry = entry,
                        onEdit = { onEdit(entry.id) },
                        onDelete = { deleteTarget = entry.id },
                        onPreviewPhoto = { url -> previewPhotoUrl = url },
                        onPayDebt = {
                            payDebtTarget = entry
                            payAmount = ""
                        }
                    )
                }
            }
        }
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus Transaksi") },
            text = { Text("Yakin ingin menghapus transaksi ini? Entri kas terkait juga akan dihapus.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(id)
                        deleteTarget = null
                    },
                    modifier = Modifier.testTag("btn-confirm-delete-bb")
                ) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            }
        )
    }

    previewPhotoUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { previewPhotoUrl = null },
            title = { Text("Bukti Nota / Surat Jalan") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Preview Nota",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewPhotoUrl = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    payDebtTarget?.let { entry ->
        val sisaHutang = entry.totalHarga - entry.nominal
        AlertDialog(
            onDismissRequest = { payDebtTarget = null },
            title = { Text("Bayar Hutang Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hutang tersisa: ${Formatters.rupiah(sisaHutang)}")
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Jumlah Bayar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pay-debt-amount")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = payAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            if (amt > sisaHutang) {
                                android.widget.Toast.makeText(context, "Jumlah bayar melebihi sisa hutang!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.payDebt(entry.id, amt)
                                payDebtTarget = null
                                payAmount = ""
                                android.widget.Toast.makeText(context, "Berhasil membayar hutang supplier!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Masukkan jumlah bayar yang valid!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn-confirm-pay-debt")
                ) { Text("Bayar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { payDebtTarget = null; payAmount = "" }
                ) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun BahanBakuCard(
    entry: BmpBahanBakuEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPreviewPhoto: (String) -> Unit,
    onPayDebt: () -> Unit
) {
    val sisaHutang = entry.totalHarga - entry.nominal
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().testTag("bb-card-${entry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.noTagihan,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        Formatters.dateLong(entry.tanggal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.testTag("btn-edit-bb-${entry.id}")) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("btn-delete-bb-${entry.id}")) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountChip(
                            label = "Total Bahan",
                            value = Formatters.rupiah(entry.totalHarga),
                            color = MaterialTheme.colorScheme.primary
                        )
                        AmountChip(
                            label = "Dibayar",
                            value = Formatters.rupiah(entry.nominal),
                            color = Color(0xFF22C57E)
                        )
                        if (sisaHutang > 0) {
                            AmountChip(
                                label = "Hutang",
                                value = Formatters.rupiah(sisaHutang),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    entry.notes?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (sisaHutang > 0) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onPayDebt,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("btn-pay-debt-${entry.id}")
                        ) {
                            Text("Bayar Hutang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val photo = entry.notaFotoUrl ?: entry.notaFotoPath
                photo?.takeIf { it.isNotBlank() }?.let { nonNullPhoto ->
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = nonNullPhoto,
                        contentDescription = "Nota",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onPreviewPhoto(nonNullPhoto) }
                            .testTag("bb-photo-thumbnail-${entry.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}
