package com.posbah.app.ui.screens.migration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.PosBahDatabase
import com.posbah.app.data.remote.MigrationResult
import com.posbah.app.data.remote.OnlineMigrationManager
import com.posbah.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val migrationManager: OnlineMigrationManager,
    private val db: PosBahDatabase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _states = MutableStateFlow<List<OnlineMigrationManager.TableMigrationState>>(emptyList())
    val states = _states.asStateFlow()

    private val _migrationResult = MutableStateFlow<MigrationResult?>(null)
    val migrationResult = _migrationResult.asStateFlow()

    private val _isMigrating = MutableStateFlow(false)
    val isMigrating = _isMigrating.asStateFlow()

    init {
        startMigration()
    }

    fun startMigration() {
        if (_isMigrating.value) return
        _isMigrating.value = true
        _migrationResult.value = null
        viewModelScope.launch {
            val tenantId = authRepository.activeTenantId().orEmpty()
            val result = migrationManager.runMigration(context, db, tenantId) { newStates ->
                _states.value = newStates.toList()
            }
            _migrationResult.value = result
            _isMigrating.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationScreen(
    onMigrationDone: () -> Unit,
    viewModel: MigrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val states by viewModel.states.collectAsState()
    val migrationResult by viewModel.migrationResult.collectAsState()
    val isMigrating by viewModel.isMigrating.collectAsState()

    LaunchedEffect(migrationResult) {
        if (migrationResult is MigrationResult.Success) {
            onMigrationDone()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sinkronisasi Cloud POSBah", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Memperbarui Database Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sedang memindahkan data transaksi lokal Anda ke server cloud real-time. Mohon jangan menutup aplikasi ini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            val verifiedCount = states.count { it.status == OnlineMigrationManager.TableStatus.VERIFIED }
            val totalCount = states.size.coerceAtLeast(1)
            val progress = verifiedCount.toFloat() / totalCount.toFloat()

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progres: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$verifiedCount dari $totalCount bagian selesai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(states) { state ->
                    TableStateRow(state)
                }
            }

            AnimatedVisibility(visible = migrationResult is MigrationResult.TableFailed) {
                val errorMsg = (migrationResult as? MigrationResult.TableFailed)?.error ?: "Terjadi kesalahan"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Migrasi Terhenti: $errorMsg",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.startMigration() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lanjutkan Migrasi")
                    }
                }
            }
        }
    }
}

@Composable
fun TableStateRow(state: OnlineMigrationManager.TableMigrationState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state.status) {
                OnlineMigrationManager.TableStatus.VERIFIED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                OnlineMigrationManager.TableStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getFriendlyTableName(state.tableName),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${state.tableName} • ${state.localCount} data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                when (state.status) {
                    OnlineMigrationManager.TableStatus.VERIFIED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    OnlineMigrationManager.TableStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    OnlineMigrationManager.TableStatus.UPLOADING,
                    OnlineMigrationManager.TableStatus.UPLOADED,
                    OnlineMigrationManager.TableStatus.VERIFYING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Pending,
                            contentDescription = "Pending",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getFriendlyTableName(name: String): String {
    return when (name) {
        "local_users" -> "Akun Pengguna"
        "tenants" -> "Profil Bisnis (Tenant)"
        "outlets" -> "Outlet / Cabang"
        "employees" -> "Karyawan POS"
        "products" -> "Produk POS"
        "customers" -> "Pelanggan"
        "bmp_clients" -> "Klien B2B"
        "bmp_master_products" -> "Master Produk Manufaktur"
        "bmp_settings" -> "Pengaturan Pabrik"
        "bmp_employees" -> "Karyawan Pabrik"
        "print_settings" -> "Pengaturan Cetak Struk"
        "transactions" -> "Transaksi Kasir"
        "bmp_invoices" -> "Invoice B2B"
        "bmp_bahan_baku" -> "Pembelian Bahan Baku"
        "bmp_cashflow" -> "Arus Kas Pabrik"
        "bmp_payrolls" -> "Penggajian Pabrik"
        "transaction_items" -> "Detail Item Transaksi"
        "bmp_products" -> "Detail Produk Invoice"
        "bmp_invoice_payments" -> "Pembayaran Invoice"
        "bmp_bahan_baku_item" -> "Item Bahan Baku"
        "bmp_product_stocks" -> "Saldo Stok Produk"
        "bmp_stock_ledger" -> "Log Mutasi Stok"
        "bmp_production_logs" -> "Laporan Produksi"
        "activity_logs" -> "Log Aktivitas"
        else -> name
    }
}
