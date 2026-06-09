package com.posbah.app.ui.screens.bmp.clients

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.BmpClientEntity
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters

@Composable
fun ClientsScreen(
    onBack: () -> Unit,
    onEdit: (Long?) -> Unit,
    viewModel: ClientsViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current

    // Tampilkan feedback Toast setelah bayar borongan
    LaunchedEffect(ui.payMassalFeedback) {
        ui.payMassalFeedback?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Klien",
                subtitle = "Daftar pelanggan BMP",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEdit(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-add-client")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Tambah Klien")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = "",
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Cari klien\u2026") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("client-search")
            )

            if (clients.isEmpty()) {
                EmptyState(
                    title = "Belum ada klien",
                    description = "Tambah klien pertama Anda untuk mulai membuat invoice",
                    actionLabel = "+ Tambah Klien",
                    onAction = { onEdit(null) }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
                        ClientRow(
                            client = client,
                            onClick = { onEdit(client.id) },
                            onDelete = { viewModel.delete(client.id) },
                            onPayMassal = { viewModel.openPayMassal(client.id) }
                        )
                    }
                }
            }
        }
    }

    // ── Dialog Bayar Borongan ──────────────────────────────────────────────────
    if (ui.payMassalClientId != null) {
        val clientId = ui.payMassalClientId!!
        val client = clients.find { it.id == clientId }

        var nominalText by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("TRANSFER") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = viewModel::closePayMassal,
            title = {
                Column {
                    Text(
                        "Bayar Borongan",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    client?.let {
                        Text(
                            it.clientName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Info saldo titipan jika ada
                    if ((client?.saldoTitipan ?: 0.0) > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "💰 Saldo Titipan: ${Formatters.rupiah(client!!.saldoTitipan)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Input nominal
                    OutlinedTextField(
                        value = nominalText,
                        onValueChange = { nominalText = it },
                        label = { Text("Total Pembayaran (Rp)") },
                        placeholder = { Text("Contoh: 5000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pay-massal-amount")
                    )

                    // Pilih metode pembayaran
                    Text(
                        "Metode Pembayaran",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf("CASH", "TRANSFER", "QRIS").forEach { m ->
                            RadioButton(
                                selected = method == m,
                                onClick = { method = m },
                                modifier = Modifier.testTag("pay-massal-method-$m")
                            )
                            Text(
                                m,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .clickable { method = m }
                                    .padding(end = 8.dp)
                            )
                        }
                    }

                    // Catatan opsional
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Keterangan logika distribusi
                    Text(
                        "💡 Pembayaran akan didistribusikan ke invoice tertua yang belum lunas. Sisa saldo masuk ke Saldo Titipan klien.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nominal = nominalText.replace(",", "").toDoubleOrNull() ?: 0.0
                        if (nominal > 0) {
                            viewModel.payMassal(
                                clientId = clientId,
                                nominal = nominal,
                                method = method,
                                notes = notes.ifBlank { null }
                            )
                        }
                    },
                    modifier = Modifier.testTag("btn-confirm-pay-massal")
                ) {
                    Text(
                        "Bayar Sekarang",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closePayMassal) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ClientRow(
    client: BmpClientEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPayMassal: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("client-${client.id}")
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    client.clientName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    client.phoneNumber ?: client.emailAddress ?: "Tanpa kontak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (client.saldoTitipan != 0.0) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        "Saldo titipan: ${Formatters.rupiah(client.saldoTitipan)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            // Tombol Bayar Borongan
            IconButton(
                onClick = onPayMassal,
                modifier = Modifier.testTag("pay-massal-${client.id}")
            ) {
                Icon(
                    Icons.Outlined.Payments,
                    contentDescription = "Bayar Borongan",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            // Tombol Hapus
            IconButton(onClick = onDelete, modifier = Modifier.testTag("del-client-${client.id}")) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
