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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.ui.components.LoadingBlock
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton

@Composable
fun TenantPickerScreen(
    onSelected: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TenantPickerViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PosBahTopBar(
            title = "Pilih Tenant",
            subtitle = "Bisnis aktif Anda",
            actions = {
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
                Column {
                    Text("Beri nama untuk tenant baru Anda.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ui.newTenantName,
                        onValueChange = viewModel::updateNewName,
                        label = { Text("Nama tenant") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new-tenant-name")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::createTenant,
                    modifier = Modifier.testTag("btn-confirm-tenant")
                ) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleCreate(false) }) { Text("Batal") }
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
