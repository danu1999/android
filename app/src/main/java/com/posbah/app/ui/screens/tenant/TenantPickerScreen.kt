package com.posbah.app.ui.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.ui.components.LoadingBlock
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun TenantPickerScreen(
    onSelected: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TenantPickerViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var selectedMode by remember { mutableStateOf("FNB") }
    val context = LocalContext.current

    var showChangePassword by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var isChangingPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PosBahTopBar(
            title = "Pilih Tenant",
            subtitle = "Bisnis aktif Anda",
            actions = {
                TextButton(onClick = { showChangePassword = true }) { Text("Ganti Password") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onLogout) { Text("Keluar") }
            }
        )

        if (ui.isLoading) {
            LoadingBlock()
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            items(ui.tenants, key = { it.id }) { tenant ->
                TenantCard(tenant = tenant, onClick = {
                    viewModel.selectTenant(tenant.id, onSelected)
                })
            }
            if (ui.canAddTenant) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PrimaryButton(
                            label = "+ Tambah Tenant Baru",
                            onClick = { viewModel.toggleCreate(true) },
                            modifier = Modifier.testTag("btn-add-tenant")
                        )
                    }
                }
            }
        }
    }

    if (ui.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleCreate(false) },
            title = { Text("Tenant Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Beri nama untuk tenant baru Anda.")
                    OutlinedTextField(
                        value = ui.newTenantName,
                        onValueChange = viewModel::updateNewName,
                        label = { Text("Nama tenant") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new-tenant-name")
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Pilih Sistem / Mode Bisnis:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val modes = listOf(
                        "FNB" to "FnB / Kasir",
                        "LAUNDRY" to "Laundry",
                        "RENTAL" to "Rental Mobil/Motor",
                        "BMP" to "Bahan Baku & Manufaktur"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { (code, label) ->
                            val selected = selectedMode == code
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMode = code }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.RadioButton(
                                        selected = selected,
                                        onClick = { selectedMode = code },
                                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.createTenant(selectedMode) },
                    modifier = Modifier.testTag("btn-confirm-tenant")
                ) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleCreate(false) }) { Text("Batal") }
            }
        )
    }

    if (showChangePassword) {
        val userEmail = viewModel.getActiveUserEmail().orEmpty()
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
}

@Composable
private fun TenantCard(tenant: Tenant, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tenant-${tenant.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Apartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tenant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Mode: ${tenant.businessMode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
