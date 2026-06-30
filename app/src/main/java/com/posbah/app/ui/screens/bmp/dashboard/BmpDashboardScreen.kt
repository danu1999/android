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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.StatChip
import com.posbah.app.ui.navigation.Screen
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
    val kpiState by viewModel.kpiState.collectAsStateWithLifecycle()

    val navController = remember {
        object {
            fun navigate(route: String) {
                when (route) {
                    "create_invoice" -> onNavigate(Screen.BmpCreateInvoice.build(null))
                    "add_production" -> onNavigate(Screen.BmpProductionLog.route)
                    "buy_material" -> onNavigate(Screen.BmpBahanBakuForm.build(null))
                    else -> onNavigate(route)
                }
            }
        }
    }

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var upgradeEmail by remember { mutableStateOf("") }
    var upgradePassword by remember { mutableStateOf("") }
    var upgradeBusinessName by remember { mutableStateOf("") }
    var isUpgrading by remember { mutableStateOf(false) }

    val menuItems = remember(ui.role) {
        listOf(
            BmpMenuItem("Klien", "Daftar pelanggan & saldo", Icons.Outlined.Group, "bmp/clients", "menu-clients"),
            BmpMenuItem("Invoice", "Buat & kelola tagihan", Icons.Outlined.Description, "bmp/invoices", "menu-invoices"),
            BmpMenuItem("Bahan Baku", "Pembelian bahan & HPP", Icons.Outlined.Science, "bmp/bahanbaku", "menu-bahanbaku"),
            BmpMenuItem("Produk", "Master produk & harga", Icons.Outlined.Inventory2, "bmp/products", "menu-products"),
            BmpMenuItem("Stok", "Stok gudang & ledger", Icons.Outlined.Inventory, "bmp/stock", "menu-stock"),
            BmpMenuItem("Produksi", "Catatan produksi harian", Icons.Outlined.PrecisionManufacturing, "bmp/production", "menu-production"),
            BmpMenuItem("Pembayaran", "Catat & lihat penerimaan", Icons.Outlined.Payments, "bmp/payments", "menu-payments"),
            BmpMenuItem("Arus Kas", "Cashflow masuk/keluar", Icons.Outlined.AccountBalance, "bmp/cashflow", "menu-cashflow"),
            BmpMenuItem("Analisis Keuangan", "Laba rugi & ekspor Excel", Icons.Outlined.Analytics, "bmp/reports/financial", "menu-financial-report"),
            BmpMenuItem("Karyawan", "Data & kontrol staf", Icons.Outlined.Badge, "bmp/employees", "menu-employees"),
            BmpMenuItem("Penggajian", "Payroll & rekap", Icons.Outlined.PriceChange, "bmp/payroll", "menu-payroll"),
            BmpMenuItem("Pengaturan", "Profil perusahaan & sistem", Icons.Outlined.Settings, "bmp/settings", "menu-settings"),
        ).filter { item ->
            if (ui.role != "OWNER") {
                item.testTag != "menu-financial-report" &&
                item.testTag != "menu-employees" &&
                item.testTag != "menu-payroll"
            } else {
                true
            }
        }
    }

    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Box(contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(
                    onClick = { fabMenuExpanded = !fabMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab-quick-action")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Aksi Cepat"
                    )
                }
                
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Buat Invoice Baru") },
                        onClick = {
                            fabMenuExpanded = false
                            onNavigate(Screen.BmpCreateInvoice.build(null))
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Description, contentDescription = null)
                        },
                        modifier = Modifier.testTag("menu-item-create-invoice")
                    )
                    DropdownMenuItem(
                        text = { Text("Tambah Klien Baru") },
                        onClick = {
                            fabMenuExpanded = false
                            onNavigate(Screen.BmpClientEdit.build(null))
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Group, contentDescription = null)
                        },
                        modifier = Modifier.testTag("menu-item-add-client")
                    )
                    DropdownMenuItem(
                        text = { Text("Catat Bahan Baku Baru") },
                        onClick = {
                            fabMenuExpanded = false
                            onNavigate(Screen.BmpBahanBakuForm.build(null))
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Science, contentDescription = null)
                        },
                        modifier = Modifier.testTag("menu-item-add-bahanbaku")
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PosBahTopBar(
                title = ui.tenantName ?: "BMP",
                subtitle = ui.tenantId?.let { "Tenant: $it" } ?: "Tenant tidak aktif",
                actions = {
                    IconButton(onClick = { onNavigate(Screen.QrScanner.route) }, modifier = Modifier.testTag("btn-qr-scanner")) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR Web")
                    }
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
                    Spacer(Modifier.height(4.dp))
                    CashFlowTrendChart(history = ui.cashFlowHistory)
                }
                if (ui.tenantId == "demo_tenant") {
                    item {
                        Spacer(Modifier.height(4.dp))
                        UpgradeDemoCard(onUpgradeClick = {
                            showUpgradeDialog = true
                            upgradeEmail = ""
                            upgradePassword = ""
                            upgradeBusinessName = ""
                        })
                    }
                }
                if (ui.ownerEmail == "muhammadmuizz8@gmail.com") {
                    item {
                        Spacer(Modifier.height(4.dp))
                        AdminPanelCard(onAdminClick = { onNavigate(Screen.AdminPanel.route) })
                    }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.weight(1f)) {
                            StatChip(
                                label = "Saldo Kas Riil",
                                value = Formatters.rupiah(ui.saldoKasRiil),
                                accent = if (ui.saldoKasRiil >= 0)
                                    androidx.compose.ui.graphics.Color(0xFF3B82F6)
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            StatChip(
                                label = "Simulasi Saldo",
                                value = Formatters.rupiah(ui.simulasiSaldo),
                                accent = if (ui.simulasiSaldo >= 0)
                                    MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (kpiState is UiState.Success) {
                    val kpiData = (kpiState as UiState.Success).data
                    item {
                        Spacer(Modifier.height(4.dp))
                        OverdueInvoiceCard(
                            overdueCount = kpiData.overdueCount,
                            onClick = { onNavigate(Screen.BmpInvoices.build(null, filterOverdue = true)) }
                        )
                    }
                    
                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(Modifier.weight(1f)) {
                                StatChip(
                                    label = "Aset Stok",
                                    value = Formatters.rupiah(kpiData.totalStockValue),
                                    accent = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                StatChip(
                                    label = "Produksi Bulan Ini",
                                    value = Formatters.number(kpiData.productionThisMonth.toLong()),
                                    accent = MaterialTheme.colorScheme.primary
                                )
                            }
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

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = ui.tenantName ?: "POSBah Premium",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .testTag("dashboard-footer-name")
                    )
                }
            }
        }
    }

    if (showUpgradeDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { if (!isUpgrading) showUpgradeDialog = false },
            title = { Text("Aktivasi Akun Premium") },
            text = {
                Column {
                    Text(
                        "Masukkan email dan password akun premium Anda. Sistem akan membuat database mandiri khusus untuk akun premium Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = upgradeEmail,
                        onValueChange = { upgradeEmail = it },
                        label = { Text("Email Premium Baru") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().testTag("upgrade-email")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = upgradePassword,
                        onValueChange = { upgradePassword = it },
                        label = { Text("Password Baru") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("upgrade-password")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = upgradeBusinessName,
                        onValueChange = { upgradeBusinessName = it },
                        label = { Text("Nama Usaha / Perusahaan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("upgrade-business-name")
                    )
                    if (isUpgrading) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Sedang memproses pembayaran & membuat database...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isUpgrading && upgradeEmail.isNotBlank() && upgradePassword.isNotBlank(),
                    onClick = {
                        isUpgrading = true
                        viewModel.upgradeToPremium(upgradeEmail, upgradePassword, upgradeBusinessName) {
                            isUpgrading = false
                            showUpgradeDialog = false
                            android.widget.Toast.makeText(
                                context,
                                "Aktivasi Premium Berhasil! Silakan login kembali dengan akun premium Anda.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            onLogout()
                        }
                    },
                    modifier = Modifier.testTag("btn-confirm-upgrade")
                ) { Text("Aktifkan Premium") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isUpgrading,
                    onClick = { showUpgradeDialog = false }
                ) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun AdminPanelCard(onAdminClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("admin-panel-card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "SISTEM ADMINISTRATOR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Kelola pendaftaran demo user, konfirmasi pembayaran premium, dan hapus akun demo tidak aktif.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            com.posbah.app.ui.components.PrimaryButton(
                label = "Buka Panel Admin",
                onClick = onAdminClick,
                modifier = Modifier.fillMaxWidth().testTag("btn-open-admin")
            )
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
private fun UpgradeDemoCard(onUpgradeClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("upgrade-demo-card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "AKUN DEMO AKTIF",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Upgrade ke premium secara offline untuk membuat database mandiri Anda sendiri menggunakan email pribadi.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            com.posbah.app.ui.components.PrimaryButton(
                label = "Upgrade ke Premium",
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth().testTag("btn-upgrade-demo")
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

@Composable
fun CashFlowTrendChart(
    history: List<BmpCashFlowDataPoint>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return
    
    val maxAmount = remember(history) {
        val maxVal = history.flatMap { listOf(it.inAmount, it.outAmount) }.maxOrNull() ?: 0.0
        if (maxVal == 0.0) 10000.0 else maxVal * 1.15 // 15% padding at top
    }
    
    val primaryColor = androidx.compose.ui.graphics.Color(0xFF22C57E) // Masuk (Green)
    val errorColor = androidx.compose.ui.graphics.Color(0xFFEF4444)   // Keluar (Red)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tren Arus Kas (7 Hari Terakhir)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Perbandingan kas masuk vs kas keluar harian",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Canvas Chart
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height
                
                val paddingLeft = 50f
                val paddingRight = 50f
                val paddingTop = 20f
                val paddingBottom = 50f
                
                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom
                
                // Draw horizontal grid lines (3 lines)
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = paddingTop + (chartHeight / gridLines) * i
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                        end = androidx.compose.ui.geometry.Offset(width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                val stepX = if (history.size > 1) chartWidth / (history.size - 1) else chartWidth
                
                // Prepare paths for line and shadow
                val inPath = androidx.compose.ui.graphics.Path()
                val outPath = androidx.compose.ui.graphics.Path()
                val inShadowPath = androidx.compose.ui.graphics.Path()
                val outShadowPath = androidx.compose.ui.graphics.Path()
                
                val pointsIn = ArrayList<androidx.compose.ui.geometry.Offset>()
                val pointsOut = ArrayList<androidx.compose.ui.geometry.Offset>()
                
                history.forEachIndexed { index, dp ->
                    val x = paddingLeft + index * stepX
                    
                    // Inflow Y
                    val yIn = paddingTop + chartHeight - ((dp.inAmount / maxAmount) * chartHeight).toFloat()
                    // Outflow Y
                    val yOut = paddingTop + chartHeight - ((dp.outAmount / maxAmount) * chartHeight).toFloat()
                    
                    val pIn = androidx.compose.ui.geometry.Offset(x, yIn)
                    val pOut = androidx.compose.ui.geometry.Offset(x, yOut)
                    
                    pointsIn.add(pIn)
                    pointsOut.add(pOut)
                    
                    if (index == 0) {
                        inPath.moveTo(x, yIn)
                        outPath.moveTo(x, yOut)
                        inShadowPath.moveTo(x, paddingTop + chartHeight)
                        inShadowPath.lineTo(x, yIn)
                        outShadowPath.moveTo(x, paddingTop + chartHeight)
                        outShadowPath.lineTo(x, yOut)
                    } else {
                        inPath.lineTo(x, yIn)
                        outPath.lineTo(x, yOut)
                        inShadowPath.lineTo(x, yIn)
                        outShadowPath.lineTo(x, yOut)
                    }
                    
                    if (index == history.lastIndex) {
                        inShadowPath.lineTo(x, paddingTop + chartHeight)
                        inShadowPath.close()
                        outShadowPath.lineTo(x, paddingTop + chartHeight)
                        outShadowPath.close()
                    }
                    
                    // Draw date labels on bottom
                    drawContext.canvas.nativeCanvas.drawText(
                        dp.dateLabel,
                        x,
                        height - 10f,
                        textPaint
                    )
                }
                
                // Draw Inflow Shadow (Green gradient)
                drawPath(
                    path = inShadowPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.2f),
                            primaryColor.copy(alpha = 0.0f)
                        ),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )
                
                // Draw Outflow Shadow (Red gradient)
                drawPath(
                    path = outShadowPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            errorColor.copy(alpha = 0.15f),
                            errorColor.copy(alpha = 0.0f)
                        ),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )
                
                // Draw Lines
                drawPath(
                    path = inPath,
                    color = primaryColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                
                drawPath(
                    path = outPath,
                    color = errorColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                
                // Draw dot points
                pointsIn.forEach { pt ->
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
                
                pointsOut.forEach { pt ->
                    drawCircle(
                        color = errorColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(primaryColor, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kas Masuk",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(errorColor, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kas Keluar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverdueInvoiceCard(overdueCount: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("overdue-invoice-card")
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "INVOICE JATUH TEMPO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ada invoice unpaid yang melewati tenggat waktu pembayaran.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = overdueCount.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}
