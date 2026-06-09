package com.posbah.app.ui.screens.bmp.cashflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.BmpCashFlowEntity
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpCashFlowRepository
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.StatChip
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CashFlowViewModel @Inject constructor(
    private val repo: BmpCashFlowRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()
    val flows = repo.observe(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val totalIn = repo.totalIn(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)
    val totalOut = repo.totalOut(tenantId).stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun insert(type: String, desc: String, amount: Double) = viewModelScope.launch {
        if (desc.isBlank() || amount <= 0) return@launch
        repo.insert(
            BmpCashFlowEntity(
                tenantId = tenantId,
                transactionDate = System.currentTimeMillis(),
                transactionType = type,
                description = desc,
                amount = amount
            )
        )
    }
}

@Composable
fun CashFlowScreen(
    onBack: () -> Unit,
    viewModel: CashFlowViewModel = hiltViewModel()
) {
    val flows by viewModel.flows.collectAsState()
    val totalIn by viewModel.totalIn.collectAsState()
    val totalOut by viewModel.totalOut.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var formType by remember { mutableStateOf("MASUK") }
    var formDesc by remember { mutableStateOf("") }
    var formAmt by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Arus Kas", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showForm = true; formType = "MASUK"; formDesc = ""; formAmt = ""
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-add-cashflow")
            ) { Icon(Icons.Outlined.Add, contentDescription = "Tambah") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        StatChip("Masuk", Formatters.rupiah(totalIn), Color(0xFF22C57E))
                    }
                    Box(Modifier.weight(1f)) {
                        StatChip("Keluar", Formatters.rupiah(totalOut), MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.padding(top = 4.dp))
                StatChip(
                    "Saldo Bersih",
                    Formatters.rupiah(totalIn - totalOut),
                    if (totalIn - totalOut >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            items(flows, key = { it.id }) { f ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().testTag("cashflow-${f.id}")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.description, style = MaterialTheme.typography.titleSmall)
                            Text(
                                Formatters.dateLong(f.transactionDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            (if (f.transactionType == "MASUK") "+ " else "- ") + Formatters.rupiah(f.amount),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (f.transactionType == "MASUK") Color(0xFF22C57E)
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("Catat Arus Kas") },
            text = {
                Column {
                    Row {
                        listOf("MASUK", "KELUAR").forEach { t ->
                            val selected = formType == t
                            TextButton(onClick = { formType = t }, modifier = Modifier.testTag("cf-type-$t")) {
                                Text(
                                    t,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = if (selected) MaterialTheme.typography.labelLarge
                                            else MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = formDesc,
                        onValueChange = { formDesc = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth().testTag("cf-desc")
                    )
                    Spacer(Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = formAmt,
                        onValueChange = { formAmt = it },
                        label = { Text("Jumlah (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("cf-amount")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.insert(formType, formDesc, formAmt.replace(",", ".").toDoubleOrNull() ?: 0.0)
                        showForm = false
                    },
                    modifier = Modifier.testTag("btn-save-cf")
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Batal") } }
        )
    }
}
