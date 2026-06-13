package com.posbah.app.ui.screens.tenant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.dao.LocalUserDao
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.security.SecurePreferences
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.data.repository.SessionState
import com.posbah.app.data.local.entities.Tenant
import com.posbah.app.data.local.entities.Outlet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class SystemOption(
    val code: String, // BMP | FNB | RENTAL | LAUNDRY
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)


@HiltViewModel
class SystemSelectionViewModel @Inject constructor(
    private val securePrefs: SecurePreferences,
    private val userDao: LocalUserDao,
    private val tenantDao: TenantDao,
    private val sessionState: SessionState,
    private val db: com.posbah.app.data.local.PosBahDatabase,
    private val localDataSeeder: com.posbah.app.data.local.LocalDataSeeder,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    fun lockInSystem(businessMode: String, onDone: () -> Unit) {
        val sub = securePrefs.currentGoogleSub ?: return
        val email = securePrefs.currentEmail ?: return
        val emailKey = email.lowercase().trim().replace(".", "_").replace("@", "_")

        viewModelScope.launch {
            val user = userDao.getBySub(sub)
            if (user != null) {
                val isPremiumUser = user.isPremium
                val chosenTenantId = when (email.lowercase().trim()) {
                    "hanafiariful@gmail.com" -> "ten_premium_hanafiariful_gmail_com"
                    "bahteramulyap@gmail.com" -> "ten_premium_bahteramulyap_gmail_com"
                    else -> if (isPremiumUser) {
                        "ten_premium_${emailKey}_$businessMode"
                    } else {
                        "demo_tenant_${emailKey}_$businessMode"
                    }
                }

                // Create the tenant if it doesn't exist
                var tenant = tenantDao.getById(chosenTenantId)
                if (tenant == null) {
                    val modeName = when (businessMode) {
                        "FNB" -> "FnB"
                        "RENTAL" -> "Rental"
                        "LAUNDRY" -> "Laundry"
                        else -> "Invoice & Manufaktur"
                    }
                    tenant = Tenant(
                        id = chosenTenantId,
                        name = if (isPremiumUser) "CV. ${user.displayName ?: email} ($modeName)"
                               else "Demo - ${user.displayName ?: email} ($modeName)",
                        ownerEmail = email,
                        businessMode = businessMode
                    )
                    tenantDao.upsert(tenant)
                    if (businessMode != "BMP") {
                        db.outletDao().insert(
                            Outlet(
                                tenantId = chosenTenantId,
                                name = "Outlet Utama",
                                isDefault = true
                            )
                        )
                    }
                }

                userDao.upsert(user.copy(businessModeLocked = true, tenantId = chosenTenantId))
                securePrefs.currentTenantId = chosenTenantId
                sessionState.setTenant(chosenTenantId)

                // Select default outlet for the chosen tenant
                val outlets = db.outletDao().listForTenant(chosenTenantId)
                val activeOutlet = outlets.firstOrNull { it.isDefault } ?: outlets.firstOrNull()
                sessionState.setOutlet(activeOutlet?.id)

                // Seed simulated data instantly
                try {
                    localDataSeeder.seedFromSqlDump(context, chosenTenantId, activeOutlet?.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Sync the newly created/seeded tenant data up to VPS immediately
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        com.posbah.app.data.remote.SupabaseSyncManager.syncAll(context, db, chosenTenantId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            onDone()
        }
    }
}

@Composable
fun SystemSelectionScreen(
    onSelected: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SystemSelectionViewModel = hiltViewModel()
) {
    val options = remember {
        listOf(
            SystemOption(
                "BMP",
                "BMP (Bahan Baku & Manufaktur)",
                "Cocok untuk manufaktur & grosir. Kelola bahan baku, invoice piutang, serta penggajian karyawan.",
                Icons.Outlined.AccountBalance,
                Color(0xFF3B82F6)
            ),
            SystemOption(
                "FNB",
                "FNB (Food & Beverage / Kasir)",
                "Desain kasir pintar cepat saji untuk makanan & minuman. Input menu cepat & cetak struk thermal.",
                Icons.Outlined.Storefront,
                Color(0xFFF59E0B)
            ),
            SystemOption(
                "RENTAL",
                "RENTAL (Sewa Mobil & Motor)",
                "Kelola ketersediaan armada kendaraan, pencatatan durasi sewa harian, denda keterlambatan, & struk sewa.",
                Icons.Outlined.DirectionsCar,
                Color(0xFF10B981)
            ),
            SystemOption(
                "LAUNDRY",
                "LAUNDRY (Service & Cuci Kiloan)",
                "Kelola jasa cuci kiloan/satuan, pantau status pengerjaan (proses, selesai, diambil), & struk laundry.",
                Icons.Outlined.LocalLaundryService,
                Color(0xFF8B5CF6)
            )
        )
    }

    var selectedOption by remember { mutableStateOf<SystemOption?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PosBahTopBar(
            title = "Pilih Sistem POS Anda",
            subtitle = "Mulai dengan 1 dari 4 sistem spesialis",
            actions = {
                TextButton(onClick = onLogout) { Text("Keluar") }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Silakan pilih sistem spesialis yang ingin Anda gunakan. Sebagai demo user, pilihan ini bersifat permanen dan tidak bisa diganti lagi.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(options) { option ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = BorderStroke(
                            1.5.dp,
                            if (selectedOption?.code == option.code) option.color
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option }
                            .testTag("system-card-${option.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = option.color.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        option.icon,
                                        contentDescription = null,
                                        tint = option.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    option.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    option.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedOption != null) {
                        showConfirmDialog = true
                    }
                },
                enabled = selectedOption != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn-confirm-system-choice")
            ) {
                Text("Konfirmasi & Kunci Sistem", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirmDialog && selectedOption != null) {
        val option = selectedOption!!
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Kunci Pilihan Sistem POS?") },
            text = {
                Text(
                    "Anda memilih sistem: ${option.title}.\n\nSesuai aturan demo, sekali Anda memilih sistem ini, Anda tidak dapat mengganti atau memilih sistem lain lagi. Apakah Anda yakin?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.lockInSystem(option.code, onSelected)
                    },
                    modifier = Modifier.testTag("btn-confirm-lock-in")
                ) { Text("Ya, Kunci Sekarang") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Batal") }
            }
        )
    }
}
