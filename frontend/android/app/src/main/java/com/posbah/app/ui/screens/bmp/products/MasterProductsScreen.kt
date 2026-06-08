package com.posbah.app.ui.screens.bmp.products

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
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpMasterProductRepository
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MasterProductsViewModel @Inject constructor(
    private val repo: BmpMasterProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    val products = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
    }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}

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
        AlertDialog(
            onDismissRequest = viewModel::closeForm,
            title = { Text(if (e.id == 0L) "Produk Baru" else "Edit Produk") },
            text = {
                Column {
                    OutlinedTextField(
                        value = e.title,
                        onValueChange = { v -> viewModel.updateField { it.copy(title = v) } },
                        label = { Text("Nama produk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input-product-name")
                    )
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
                            label = { Text("Harga (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = e.description.orEmpty(),
                        onValueChange = { v -> viewModel.updateField { it.copy(description = v.ifBlank { null }) } },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
