package com.posbah.app.ui.screens.bmp.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton

@Composable
fun ClientEditScreen(
    onDone: () -> Unit,
    viewModel: ClientEditViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) { if (saved) onDone() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = if (form.id == 0L) "Tambah Klien" else "Edit Klien",
                onBack = onDone
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.clientName,
                onValueChange = { v -> viewModel.update { it.copy(clientName = v) } },
                label = { Text("Nama Klien*") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input-client-name")
            )
            OutlinedTextField(
                value = form.phoneNumber.orEmpty(),
                onValueChange = { v -> viewModel.update { it.copy(phoneNumber = v.ifBlank { null }) } },
                label = { Text("Nomor Telepon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.emailAddress.orEmpty(),
                onValueChange = { v -> viewModel.update { it.copy(emailAddress = v.ifBlank { null }) } },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.addressLine1.orEmpty(),
                onValueChange = { v -> viewModel.update { it.copy(addressLine1 = v.ifBlank { null }) } },
                label = { Text("Alamat") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.province.orEmpty(),
                onValueChange = { v -> viewModel.update { it.copy(province = v.ifBlank { null }) } },
                label = { Text("Provinsi") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.postalCode.orEmpty(),
                onValueChange = { v -> viewModel.update { it.copy(postalCode = v.ifBlank { null }) } },
                label = { Text("Kode Pos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.taxNumber.orEmpty(),
                onValueChange = { v -> viewModel.update { it.copy(taxNumber = v.ifBlank { null }) } },
                label = { Text("NPWP") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = if (form.saldoTitipan == 0.0) "" else form.saldoTitipan.toString(),
                onValueChange = { v ->
                    val parsed = v.replace(",", ".").toDoubleOrNull() ?: 0.0
                    viewModel.update { it.copy(saldoTitipan = parsed) }
                },
                label = { Text("Saldo Titipan (Rp)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                label = "Simpan Klien",
                onClick = viewModel::save,
                enabled = form.clientName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("btn-save-client")
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
