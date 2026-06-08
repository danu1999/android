package com.posbah.app.ui.screens.bmp.dashboard

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.StatChip
import com.posbah.app.util.Formatters

data class BmpMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val testTag: String
)

@Composable
fun BmpDashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: BmpDashboardViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    val menuItems = remember {
        listOf(
            BmpMenuItem("Klien", "Daftar pelanggan & saldo", Icons.Outlined.Group, "bmp/clients", "menu-clients"),
            BmpMenuItem("Invoice", "Buat & kelola tagihan", Icons.Outlined.Description, "bmp/invoices", "menu-invoices"),
            BmpMenuItem("Produk", "Master produk & harga", Icons.Outlined.Inventory2, "bmp/products", "menu-products"),
            BmpMenuItem("Pembayaran", "Catat & lihat penerimaan", Icons.Outlined.Payments, "bmp/payments", "menu-payments"),
            BmpMenuItem("Arus Kas", "Cashflow masuk/keluar", Icons.Outlined.AccountBalance, "bmp/cashflow", "menu-cashflow"),
            BmpMenuItem("Karyawan", "Data & kontrol staf", Icons.Outlined.Badge, "bmp/employees", "menu-employees"),
            BmpMenuItem("Penggajian", "Payroll & rekap", Icons.Outlined.PriceChange, "bmp/payroll", "menu-payroll"),
            BmpMenuItem("Pengaturan", "Profil perusahaan & sistem", Icons.Outlined.Settings, "bmp/settings", "menu-settings"),
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PosBahTopBar(
            title = "BMP",
            subtitle = ui.tenantId?.let { "Tenant: $it" } ?: "Tenant tidak aktif",
            actions = {
                IconButton(onClick = { viewModel.logout(onLogout) }, modifier = Modifier.testTag("btn-logout")) {
                    Icon(Icons.Outlined.Logout, contentDescription = "Logout")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeroCard(ui = ui)
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Invoice",
                            value = Formatters.number(ui.invoiceCount.toLong()),
                            accent = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Klien",
                            value = Formatters.number(ui.clientCount.toLong()),
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Cashflow Masuk",
                            value = Formatters.rupiah(ui.totalIn),
                            accent = androidx.compose.ui.graphics.Color(0xFF22C57E)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Cashflow Keluar",
                            value = Formatters.rupiah(ui.totalOut),
                            accent = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "MODUL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            // 2-column grid as items
            items(menuItems.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { mi ->
                        Box(modifier = Modifier.weight(1f)) {
                            MenuCard(item = mi, onClick = { onNavigate(mi.route) })
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroCard(ui: BmpDashboardUiState) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().testTag("hero-card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "TOTAL PIUTANG",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                Formatters.rupiah(ui.totalOutstanding),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "dari nilai total ${Formatters.rupiah(ui.totalAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun MenuCard(item: BmpMenuItem, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(item.testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
