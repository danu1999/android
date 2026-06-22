package com.posbah.app.ui.screens.pos

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close

import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.CustomerEntity
import com.posbah.app.data.local.entities.ProductEntity
import com.posbah.app.data.local.entities.TransactionEntity
import com.posbah.app.data.local.entities.TransactionItemEntity
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.LoadingBlock
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.ui.navigation.Screen
import com.posbah.app.util.Formatters
import com.posbah.app.util.CameraUtils
import com.posbah.app.util.OnlineStoreLinkGenerator
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun decodeBase64Image(imageStr: String?): Any? {
    if (imageStr.isNullOrBlank()) return null
    if (imageStr.startsWith("data:image")) {
        val base64Data = imageStr.substringAfter("base64,")
        return try {
            android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            imageStr
        }
    }
    return imageStr
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToPrintSettings: () -> Unit,
    viewModel: PosViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val ui by viewModel.uiState.collectAsState()
    val productList by viewModel.products.collectAsState()
    val customerList by viewModel.customers.collectAsState()
    val queueList by viewModel.pendingQueues.collectAsState()
    val tenantName by viewModel.tenantName.collectAsState()

    var activeProductForVariant by remember { mutableStateOf<ProductEntity?>(null) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var bluetoothProgress by remember { mutableStateOf(0f) }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var newProdName by remember { mutableStateOf("") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdCostPrice by remember { mutableStateOf("") }
    var newProdStock by remember { mutableStateOf("999") }
    var newProdCategory by remember { mutableStateOf("Umum") }
    var newProdBarcode by remember { mutableStateOf("") }
    var newProdMinStockAlert by remember { mutableStateOf("0") }
    var newCustName by remember { mutableStateOf("") }
    var newCustPhone by remember { mutableStateOf("") }
    var newCustAddress by remember { mutableStateOf("") }

    var showEditProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var editProdName by remember { mutableStateOf("") }
    var editProdPrice by remember { mutableStateOf("") }
    var editProdCostPrice by remember { mutableStateOf("") }
    var editProdStock by remember { mutableStateOf("") }
    var editProdCategory by remember { mutableStateOf("") }
    var editProdBarcode by remember { mutableStateOf("") }
    var editProdMinStockAlert by remember { mutableStateOf("") }

    var showTransactionsHistoryDialog by remember { mutableStateOf(false) }
    var showEditReceiptDialog by remember { mutableStateOf(false) }
    var selectedTxForEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var editCustName by remember { mutableStateOf("") }
    var editPaymentMethod by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf(0L) }
    var editTotalAmount by remember { mutableStateOf("") }
    var editAmountPaid by remember { mutableStateOf("") }
    var editChange by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseName by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showStoreShareDialog by remember { mutableStateOf(false) }
    var showStoreSimulationDialog by remember { mutableStateOf(false) }
    var activeStoreToken by remember { mutableStateOf<String?>(null) }
    var txSearchQuery by remember { mutableStateOf("") }
    var txFilterMethod by remember { mutableStateOf("SEMUA") }

    var tempPhotoFile by remember { mutableStateOf<java.io.File?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<java.io.File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            capturedPhotoFile = tempPhotoFile
        }
    }

    val launchCamera = {
        try {
            val file = CameraUtils.createTempCameraFile(context)
            tempPhotoFile = file
            val uri = CameraUtils.getFileProviderUri(context, file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka kamera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = CameraUtils.copyUriToTempFile(context, it)
            if (file != null) {
                capturedPhotoFile = file
            } else {
                Toast.makeText(context, "Gagal memuat gambar dari galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchGallery = {
        galleryLauncher.launch("image/*")
    }

    val transactionHistoryList by viewModel.transactions.collectAsState()
    val activityLogsList by viewModel.activityLogs.collectAsState()
    val tenantId = viewModel.activeTenantId

    val outletList by viewModel.availableOutlets.collectAsState()
    val activeOutletId by viewModel.activeOutletId.collectAsState()
    val activeOutlet = remember(outletList, activeOutletId) {
        outletList.find { it.id == activeOutletId }
    }
    val activeOutletName = activeOutlet?.name ?: "Outlet Utama"
    var showOutletDropdown by remember { mutableStateOf(false) }

    val categories = remember(productList) {
        listOf("Semua") + productList.map { it.category }.distinct().sorted()
    }

    val filteredProducts = remember(productList, ui.searchQuery, ui.activeCategory) {
        productList.filter {
            val matchesSearch = it.name.contains(ui.searchQuery, ignoreCase = true) || 
                                (it.barcode?.contains(ui.searchQuery, ignoreCase = true) == true)
            val matchesCategory = ui.activeCategory == "Semua" || it.category == ui.activeCategory
            matchesSearch && matchesCategory
        }
    }

    val subtotal = viewModel.getSubtotal()
    val discountAmt = viewModel.getDiscountAmt()
    val total = viewModel.getTotal()
    val cartSize = viewModel.cart.sumOf { it.quantity }

    val showBluetoothPrint = {
        scope.launch {
            showBluetoothDialog = true
            bluetoothProgress = 0f
            while (bluetoothProgress < 1.0f) {
                delay(150)
                bluetoothProgress += 0.1f
            }
            delay(300)
            showBluetoothDialog = false
            Toast.makeText(context, "Berhasil mencetak ke printer Bluetooth thermal!", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerNativePrint = { tx: TransactionEntity, lines: List<TransactionItemEntity> ->
        val html = com.posbah.app.ui.print.ReceiptPrinter.generateReceiptHtml(
            context, tx, lines, viewModel.products.value, ui.printConfig, tenantName
        )

        printHtmlReceipt(context, html)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Box {
                    PosBahTopBar(
                        title = tenantName,
                        subtitle = "Outlet: $activeOutletName",
                        onBack = onBack,
                        onTitleClick = if (ui.isOwner) { { showOutletDropdown = true } } else null,
                        actions = {
                            if (ui.canViewMargin) {
                                IconButton(onClick = { onNavigate(Screen.MarginAnalysis.route) }) {
                                    Icon(Icons.Outlined.History, contentDescription = "Analisis Margin & Riwayat")
                                }
                            }
                        }
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = showOutletDropdown,
                        onDismissRequest = { showOutletDropdown = false }
                    ) {
                        outletList.forEach { outlet ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(outlet.name) },
                                onClick = {
                                    viewModel.selectOutlet(outlet.id)
                                    showOutletDropdown = false
                                }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FooterButton(
                                icon = Icons.Outlined.Queue,
                                label = "Antrian",
                                onClick = {
                                    scope.launch {
                                        viewModel.toggleQueueModal(true)
                                    }
                                }
                            )
                        }
                        item {
                            FooterButton(
                                icon = Icons.Outlined.Share,
                                label = "Toko Online",
                                onClick = {
                                    activeStoreToken = OnlineStoreLinkGenerator.generateShareLink(tenantId)
                                    showStoreShareDialog = true
                                }
                            )
                        }
                        item {
                            FooterButton(
                                icon = Icons.Outlined.History,
                                label = "Riwayat",
                                onClick = { showTransactionsHistoryDialog = true }
                            )
                        }
                        if (ui.isOwner) {
                            item {
                                FooterButton(
                                    icon = Icons.Outlined.Notes,
                                    label = "Log Aktivitas",
                                    onClick = { showLogsDialog = true }
                                )
                            }
                        }
                        if (true) {
                            item {
                                FooterButton(
                                    icon = Icons.Outlined.Storefront,
                                    label = "Tambah Produk",
                                    onClick = {
                                        newProdName = ""
                                        newProdPrice = ""
                                        newProdCostPrice = ""
                                        newProdStock = "999"
                                        newProdCategory = "Umum"
                                        newProdBarcode = ""
                                        newProdMinStockAlert = "0"
                                        capturedPhotoFile = null
                                        showAddProductDialog = true
                                    }
                                )
                            }
                        }
                        item {
                            FooterButton(
                                icon = Icons.Outlined.People,
                                label = "Tambah Pelanggan",
                                onClick = {
                                    newCustName = ""
                                    newCustPhone = ""
                                    newCustAddress = ""
                                    showAddCustomerDialog = true
                                }
                            )
                        }

                        item {
                            FooterButton(
                                icon = Icons.Outlined.Print,
                                label = "Pengaturan Struk",
                                onClick = onNavigateToPrintSettings
                            )
                        }
                        item {
                            FooterButton(
                                icon = Icons.Outlined.Receipt,
                                label = "Biaya / Bahan Baku",
                                onClick = {
                                    expenseName = ""
                                    expenseAmount = ""
                                    expenseDate = System.currentTimeMillis()
                                    showAddExpenseDialog = true
                                }
                            )
                        }
                        if (ui.isOwner) {
                            item {
                                FooterButton(
                                    icon = Icons.Outlined.Storefront,
                                    label = "Kontrol Outlet",
                                    onClick = { onNavigate("owner/outlet_control") }
                                )
                            }
                            item {
                                FooterButton(
                                    icon = Icons.Outlined.People,
                                    label = "Kelola Karyawan",
                                    onClick = { onNavigate("owner/employees") }
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                // Left pane: Search, categories, and products grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = ui.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (ui.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Outlined.Clear, null)
                                }
                            }
                        },
                        placeholder = { Text("Cari produk atau scan barcode...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("pos-search")
                    )

                    // Categories Horizontal Row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val active = ui.activeCategory == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable { viewModel.updateCategory(cat) }
                                    .border(
                                        width = 1.dp,
                                        color = if (active) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Text(
                                    text = cat,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Product Grid
                    if (filteredProducts.isEmpty()) {
                        if (productList.isEmpty()) {
                            if (ui.isPremium) {
                                if (ui.isSeedTenant) {
                                    EmptyState(
                                        title = "Katalog Produk Kosong",
                                        description = "Database Anda belum terisi data. Klik tombol di bawah untuk memulihkan data transaksi dan produk Anda.",
                                        actionLabel = "Pulihkan Data Toko Sekarang",
                                        onAction = viewModel::importDemoData
                                    )
                                } else {
                                    EmptyState(
                                        title = "Katalog Produk Kosong",
                                        description = "Katalog produk Anda masih kosong. Silakan tambahkan produk baru untuk mulai bertransaksi.",
                                        actionLabel = "Tambah Produk Baru",
                                        onAction = {
                                            newProdName = ""
                                            newProdPrice = ""
                                            newProdCostPrice = ""
                                            newProdStock = "999"
                                            newProdCategory = "Umum"
                                            newProdBarcode = ""
                                            newProdMinStockAlert = "0"
                                            capturedPhotoFile = null
                                            showAddProductDialog = true
                                        }
                                    )
                                }
                            } else {
                                EmptyState(
                                    title = "Katalog Produk Kosong",
                                    description = "Database Anda belum terisi data F&B. Klik tombol di bawah untuk memuat data demo.",
                                    actionLabel = "Muat Data Demo Sekarang",
                                    onAction = viewModel::importDemoData
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Produk tidak ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(filteredProducts, key = { it.id }) { p ->
                                val vars = viewModel.parseVariants(p)
                                ProductCard(
                                    product = p,
                                    hasVariants = vars.isNotEmpty(),
                                    onClick = {
                                        if (vars.isNotEmpty()) {
                                            activeProductForVariant = p
                                        } else {
                                            viewModel.addToCart(p)
                                        }
                                    },
                                    onLongClick = {
                                        productToEdit = p
                                        editProdName = p.name
                                        editProdPrice = p.price.toString()
                                        editProdCostPrice = p.costPrice.toString()
                                        editProdStock = p.stock.toString()
                                        editProdCategory = p.category
                                        editProdBarcode = p.barcode.orEmpty()
                                        editProdMinStockAlert = p.minStockAlert.toString()
                                        capturedPhotoFile = null
                                        showEditProductDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Mobile view bottom navigation bar showing cart items count and total
                    if (!isTablet && viewModel.cart.isNotEmpty()) {
                        Surface(
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BadgedBox(badge = { Badge { Text(cartSize.toString()) } }) {
                                    Icon(
                                        Icons.Outlined.ShoppingCart,
                                        contentDescription = "Keranjang",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.rupiah(total), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                }
                                Button(
                                    onClick = { viewModel.togglePayModal(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Bayar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Right pane: Cart sidebar (for Tablet layouts)
                if (isTablet) {
                    Surface(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight(),
                        tonalElevation = 1.dp,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        CartSidebarContent(
                            viewModel = viewModel,
                            ui = ui,
                            customerList = customerList,
                            subtotal = subtotal,
                            discountAmt = discountAmt,
                            total = total,
                            onPayClick = { viewModel.togglePayModal(true) }
                        )
                    }
                }
                
                // Close Row
                }
                
                // Footer showing the business name
                Text(
                    text = tenantName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("dashboard-footer-name")
                )
                
                // Close Column
                }
            }

        // Mobile cart sheet modal
        if (!isTablet && ui.showPayModal && viewModel.cart.isNotEmpty()) {
            var isCheckingOut by remember { mutableStateOf(false) }
            ModalBottomSheet(
                onDismissRequest = { 
                    viewModel.togglePayModal(false)
                    isCheckingOut = false
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                ) {
                    if (!isCheckingOut) {
                        CartSidebarContent(
                            viewModel = viewModel,
                            ui = ui,
                            customerList = customerList,
                            subtotal = subtotal,
                            discountAmt = discountAmt,
                            total = total,
                            onPayClick = { isCheckingOut = true }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { isCheckingOut = false }) {
                                        Text("← Kembali")
                                    }
                                    Text("Pembayaran POS", fontWeight = FontWeight.Bold)
                                }
                                PaymentDialogContent(
                                    viewModel = viewModel,
                                    ui = ui,
                                    total = total
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.checkout(isQueue = true) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Antrian")
                                }
                                Button(
                                    onClick = { viewModel.checkout(isQueue = false) },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Bayar Sekarang")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active queues modal sheet
        if (ui.showQueueModal) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleQueueModal(false) },
                title = { Text("Daftar Antrian Aktif") },
                text = {
                    if (queueList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("Tidak ada antrian tertunda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(queueList, key = { it.id }) { q ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("No. Antrian: #${q.queueNumber ?: q.id}", fontWeight = FontWeight.Bold)
                                            Text("Pelanggan: ${q.customerName ?: "-"}", fontSize = 12.sp)
                                            Text("Total: ${Formatters.rupiah(q.total)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(onClick = { viewModel.resumeQueue(q) }) {
                                                Text("Panggil")
                                            }
                                            IconButton(onClick = { viewModel.cancelQueue(q.id) }) {
                                                Icon(Icons.Outlined.Delete, "Batal", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleQueueModal(false) }) { Text("Tutup") }
                }
            )
        }

        // Product Variant dialog picker
        if (activeProductForVariant != null) {
            val p = activeProductForVariant!!
            val vars = viewModel.parseVariants(p)
            AlertDialog(
                onDismissRequest = { activeProductForVariant = null },
                title = { Text("Pilih Varian: ${p.name}") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vars) { v ->
                            val vStock = v.stock ?: p.stock
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = vStock > 0) {
                                        viewModel.addToCart(p, v)
                                        activeProductForVariant = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(v.name, fontWeight = FontWeight.Bold)
                                        Text("Stok: ${if (vStock > 0) vStock else "Habis"}", fontSize = 12.sp, color = if (vStock > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                                    }
                                    Text(Formatters.rupiah(v.price ?: p.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeProductForVariant = null }) { Text("Batal") }
                }
            )
        }

        // Payment modal dialog
        if (ui.showPayModal && !isTablet) {
            // Already handled via ModalBottomSheet in mobile view
        } else if (ui.showPayModal && isTablet) {
            AlertDialog(
                onDismissRequest = { viewModel.togglePayModal(false) },
                title = { Text("Pembayaran POS") },
                text = {
                    PaymentDialogContent(
                        viewModel = viewModel,
                        ui = ui,
                        total = total
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.checkout(isQueue = false) }) {
                        Text("Selesaikan Bayar")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.checkout(isQueue = true) }) {
                        Text("Simpan Antrian")
                    }
                }
            )
        }

        // Checkout Error dialog
        if (ui.checkoutError != null) {
            AlertDialog(
                onDismissRequest = viewModel::clearCheckoutError,
                title = { Text("Transaksi Gagal") },
                text = { Text(ui.checkoutError ?: "Terjadi kesalahan tidak terduga saat checkout.") },
                confirmButton = {
                    TextButton(onClick = viewModel::clearCheckoutError) {
                        Text("Mengerti")
                    }
                }
            )
        }

        // Receipt dialog modal showing thermal receipt print mockup
        if (ui.showReceiptDialog && ui.activeReceipt != null) {
            val r = ui.activeReceipt!!
            val rItems = ui.activeReceiptItems
            AlertDialog(
                onDismissRequest = viewModel::closeReceiptDialog,
                title = { Text("Pembayaran Berhasil") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Transaksi berhasil disimpan secara lokal offline.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
                        
                        // Scrollable receipt mock
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFFFF8),
                            border = BorderStroke(0.5.dp, Color.Gray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .padding(horizontal = 4.dp)
                        ) {
                            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    Text("PISANG KEJU RAMAYANA", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    Text("Struk Pembayaran Offline", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(4.dp))
                                    Text("No: ${r.receiptNumber}", fontSize = 10.sp)
                                    Text("Metode: ${r.paymentMethod}", fontSize = 10.sp)
                                    r.customerName?.let { Text("Pelanggan: $it", fontSize = 10.sp) }
                                    Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                                }
                                items(rItems) { item ->
                                    val name = item.variantName?.let { "${item.productId} ($it)" } ?: "${item.productId}"
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("$name x${item.quantity}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(Formatters.rupiah(item.price * item.quantity), fontSize = 11.sp)
                                    }
                                }
                                item {
                                    Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Subtotal", fontSize = 11.sp)
                                        Text(Formatters.rupiah(r.subtotal), fontSize = 11.sp)
                                    }
                                    if (r.discountAmt > 0) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Diskon", fontSize = 11.sp)
                                            Text("-${Formatters.rupiah(r.discountAmt)}", fontSize = 11.sp)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(Formatters.rupiah(r.total), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    r.amountPaid?.let {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Tunai", fontSize = 11.sp)
                                            Text(Formatters.rupiah(it), fontSize = 11.sp)
                                        }
                                    }
                                    r.change?.let {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Kembali", fontSize = 11.sp)
                                            Text(Formatters.rupiah(it), fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text("Terima Kasih! 🙏", fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::closeReceiptDialog) { Text("Transaksi Baru") }
                },
                dismissButton = {
                    Row {
                        IconButton(onClick = { triggerNativePrint(r, rItems) }) {
                            Icon(Icons.Outlined.Print, "Print System")
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val html = com.posbah.app.ui.print.ReceiptPrinter.generateReceiptHtml(
                                    context, r, rItems, viewModel.products.value, ui.printConfig, tenantName
                                )
                                com.posbah.app.ui.print.ReceiptPrinter.print(context, html)
                            }
                        ) {
                            Icon(Icons.Outlined.Storefront, "Print Bluetooth")
                        }
                    }
                }
            )
        }

        // Simulated Bluetooth printing dialog spinner
        if (showBluetoothDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Mencetak Struk Bluetooth") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Mengirimkan file ESC/POS via Bluetooth...", modifier = Modifier.padding(bottom = 12.dp))
                        LinearProgressIndicator(progress = bluetoothProgress, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {}
            )
        }

        // Database seeding status banner overlay
        if (ui.isSeeding) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(if (ui.isPremium && ui.isSeedTenant) "Memulihkan Data Toko" else "Memuat Data Demo") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(if (ui.isPremium && ui.isSeedTenant) "Harap tunggu, memproses dan memulihkan data toko..." else "Harap tunggu, memproses dan memuat file data demo...")
                    }
                },
                confirmButton = {}
            )
        }

        // Dialog: Tambah Produk Baru
        if (showAddProductDialog) {
            AlertDialog(
                onDismissRequest = { showAddProductDialog = false },
                title = { Text("Tambah Produk Baru") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newProdName,
                            onValueChange = { nameVal -> newProdName = nameVal },
                            label = { Text("Nama Produk") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-product-name")
                        )
                        OutlinedTextField(
                            value = newProdPrice,
                            onValueChange = { priceVal -> newProdPrice = priceVal },
                            label = { Text("Harga Jual (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-product-price")
                        )
                        OutlinedTextField(
                            value = newProdCostPrice,
                            onValueChange = { costVal -> newProdCostPrice = costVal },
                            label = { Text("Harga Beli (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Real-time margin calculator
                        val jual = newProdPrice.toDoubleOrNull() ?: 0.0
                        val beli = newProdCostPrice.toDoubleOrNull() ?: 0.0
                        val margin = if (jual > 0) ((jual - beli) / jual) * 100 else 0.0
                        Text(
                            text = "Margin Keuntungan: ${String.format("%.1f", margin)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (margin >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        OutlinedTextField(
                            value = newProdStock,
                            onValueChange = { stockVal -> newProdStock = stockVal },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-product-stock")
                        )
                        OutlinedTextField(
                            value = newProdMinStockAlert,
                            onValueChange = { valStr -> newProdMinStockAlert = valStr },
                            label = { Text("Batas Minimum Stok (Peringatan)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-product-min-stock")
                        )
                        OutlinedTextField(
                            value = newProdCategory,
                            onValueChange = { catVal -> newProdCategory = catVal },
                            label = { Text("Kategori") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-product-category")
                        )
                        OutlinedTextField(
                            value = newProdBarcode,
                            onValueChange = { barcodeVal -> newProdBarcode = barcodeVal },
                            label = { Text("Barcode (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-product-barcode")
                        )

                        // Camera & Image Section
                        if (capturedPhotoFile != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = capturedPhotoFile,
                                    contentDescription = "Preview Foto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { capturedPhotoFile = null },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = launchCamera,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Kamera", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = launchGallery,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Image, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Galeri", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = newProdPrice.toDoubleOrNull() ?: 0.0
                            val costPrice = newProdCostPrice.toDoubleOrNull() ?: 0.0
                            val stock = newProdStock.toIntOrNull() ?: 0
                            val minStock = newProdMinStockAlert.toIntOrNull() ?: 0
                            if (newProdName.isNotBlank() && price > 0) {
                                viewModel.addProduct(newProdName, price, costPrice, stock, newProdCategory, newProdBarcode, capturedPhotoFile, minStock) {
                                    showAddProductDialog = false
                                    Toast.makeText(context, "Produk berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nama produk dan harga wajib diisi dengan benar!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("btn-save-new-product")
                    ) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProductDialog = false }) { Text("Batal") }
                }
            )
        }

        // Dialog: Edit Produk
        if (showEditProductDialog && productToEdit != null) {
            val originalProduct = productToEdit!!
            AlertDialog(
                onDismissRequest = { showEditProductDialog = false },
                title = { Text("Edit Produk") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = editProdName,
                            onValueChange = { editProdName = it },
                            label = { Text("Nama Produk") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editProdPrice,
                            onValueChange = { editProdPrice = it },
                            label = { Text("Harga Jual (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editProdCostPrice,
                            onValueChange = { editProdCostPrice = it },
                            label = { Text("Harga Beli (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Real-time margin calculator
                        val jual = editProdPrice.toDoubleOrNull() ?: 0.0
                        val beli = editProdCostPrice.toDoubleOrNull() ?: 0.0
                        val margin = if (jual > 0) ((jual - beli) / jual) * 100 else 0.0
                        Text(
                            text = "Margin Keuntungan: ${String.format("%.1f", margin)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (margin >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        OutlinedTextField(
                            value = editProdStock,
                            onValueChange = { editProdStock = it },
                            label = { Text("Stok") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editProdMinStockAlert,
                            onValueChange = { editProdMinStockAlert = it },
                            label = { Text("Batas Minimum Stok (Peringatan)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editProdCategory,
                            onValueChange = { editProdCategory = it },
                            label = { Text("Kategori") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editProdBarcode,
                            onValueChange = { editProdBarcode = it },
                            label = { Text("Barcode (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Camera & Image Section
                        if (capturedPhotoFile != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = capturedPhotoFile,
                                    contentDescription = "Preview Foto Baru",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { capturedPhotoFile = null },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else if (!originalProduct.image.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = decodeBase64Image(originalProduct.image),
                                    contentDescription = "Foto Produk Saat Ini",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("Foto saat ini tersimpan", color = Color.White, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = launchCamera,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ganti Kamera", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = launchGallery,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Image, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ganti Galeri", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = launchCamera,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Kamera", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = launchGallery,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Image, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Galeri", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = editProdPrice.toDoubleOrNull() ?: 0.0
                            val costPrice = editProdCostPrice.toDoubleOrNull() ?: 0.0
                            val stock = editProdStock.toIntOrNull() ?: 0
                            val minStock = editProdMinStockAlert.toIntOrNull() ?: 0
                            if (editProdName.isNotBlank() && price > 0) {
                                val keepExisting = capturedPhotoFile == null && !originalProduct.image.isNullOrBlank()
                                viewModel.editProduct(
                                    product = originalProduct,
                                    name = editProdName,
                                    price = price,
                                    costPrice = costPrice,
                                    stock = stock,
                                    category = editProdCategory,
                                    barcode = editProdBarcode,
                                    imageFile = capturedPhotoFile,
                                    keepExistingImage = keepExisting,
                                    minStockAlert = minStock
                                ) {
                                    showEditProductDialog = false
                                    Toast.makeText(context, "Produk berhasil diubah!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nama produk dan harga wajib diisi!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text("Simpan Perubahan") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProductDialog = false }) { Text("Batal") }
                }
            )
        }

        if (showAddExpenseDialog) {
            AlertDialog(
                onDismissRequest = { showAddExpenseDialog = false },
                title = { Text("Catat Biaya / Bahan Baku") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expenseName,
                            onValueChange = { expenseName = it },
                            label = { Text("Keterangan / Nama Bahan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = expenseAmount,
                            onValueChange = { expenseAmount = it },
                            label = { Text("Nominal Pengeluaran (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Date Picker Button
                        val calendar = java.util.Calendar.getInstance()
                        calendar.timeInMillis = expenseDate
                        val datePickerDialog = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = java.util.Calendar.getInstance()
                                selectedCal.timeInMillis = expenseDate
                                selectedCal.set(java.util.Calendar.YEAR, year)
                                selectedCal.set(java.util.Calendar.MONTH, month)
                                selectedCal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                expenseDate = selectedCal.timeInMillis
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val dateStr = sdf.format(Date(expenseDate))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tanggal: $dateStr", fontSize = 14.sp)
                            Button(onClick = { datePickerDialog.show() }) {
                                Text("Pilih Tanggal")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = expenseAmount.toDoubleOrNull() ?: 0.0
                            if (expenseName.isNotBlank() && amt > 0) {
                                viewModel.addExpense(expenseName, amt, expenseDate) {
                                    showAddExpenseDialog = false
                                    Toast.makeText(context, "Pengeluaran berhasil dicatat!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Keterangan dan nominal wajib diisi!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddExpenseDialog = false }) { Text("Batal") }
                }
            )
        }

        // Dialog: Tambah Pelanggan Baru
        if (showAddCustomerDialog) {
            AlertDialog(
                onDismissRequest = { showAddCustomerDialog = false },
                title = { Text("Tambah Pelanggan Baru") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCustName,
                            onValueChange = { nameVal -> newCustName = nameVal },
                            label = { Text("Nama Pelanggan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-customer-name")
                        )
                        OutlinedTextField(
                            value = newCustPhone,
                            onValueChange = { phoneVal -> newCustPhone = phoneVal },
                            label = { Text("No. WhatsApp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-customer-phone")
                        )
                        OutlinedTextField(
                            value = newCustAddress,
                            onValueChange = { addrVal -> newCustAddress = addrVal },
                            label = { Text("Alamat") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add-customer-address")
                        )

                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Daftar Pelanggan & Riwayat Belanja",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            if (customerList.isEmpty()) {
                                Text(
                                    "Belum ada pelanggan terdaftar",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(customerList) { cust ->
                                        val custTx = transactionHistoryList.filter {
                                            it.customerId == cust.id || it.customerName.equals(cust.name, ignoreCase = true)
                                        }
                                        val totalSpent = custTx.sumOf { it.total }
                                        val lastTx = custTx.maxByOrNull { it.date }
                                        val lastTxDateStr = if (lastTx != null) {
                                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(lastTx.date))
                                        } else {
                                            "Belum ada transaksi"
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text("WhatsApp: ${cust.phone ?: "-"}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Alamat: ${cust.address ?: "-"}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Total: ${Formatters.rupiah(totalSpent)}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text("Terakhir: $lastTxDateStr", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCustName.isNotBlank()) {
                                viewModel.addCustomer(newCustName, newCustPhone, newCustAddress) {
                                    showAddCustomerDialog = false
                                    Toast.makeText(context, "Pelanggan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nama pelanggan wajib diisi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("btn-save-new-customer")
                    ) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomerDialog = false }) { Text("Batal") }
                }
            )
        }

        // Dialog: Riwayat Transaksi & Pelunasan Piutang
        if (showTransactionsHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showTransactionsHistoryDialog = false },
                title = { Text("Riwayat Transaksi POS") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(450.dp)) {
                        OutlinedTextField(
                            value = txSearchQuery,
                            onValueChange = { txSearchQuery = it },
                            placeholder = { Text("Cari No. Struk atau Pelanggan...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf("SEMUA", "CASH", "QRIS", "HUTANG")
                            filters.forEach { filter ->
                                val active = txFilterMethod == filter
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { txFilterMethod = filter }
                                ) {
                                    Text(
                                        text = filter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        val filteredList = transactionHistoryList.filter {
                            val matchesSearch = it.receiptNumber.contains(txSearchQuery, ignoreCase = true) ||
                                                (it.customerName?.contains(txSearchQuery, ignoreCase = true) == true)
                            val matchesMethod = txFilterMethod == "SEMUA" || it.paymentMethod == txFilterMethod
                            matchesSearch && matchesMethod
                        }

                        if (filteredList.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Tidak ada transaksi ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredList, key = { it.id }) { tx ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                                    val dateStr = sdf.format(Date(tx.date))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(tx.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("Pelanggan: ${tx.customerName ?: "Umum"}", fontSize = 11.sp)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(Formatters.rupiah(tx.total), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (tx.paymentMethod == "HUTANG") MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    ) {
                                                        Text(
                                                            text = tx.paymentMethod,
                                                            color = if (tx.paymentMethod == "HUTANG") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (tx.paymentMethod == "HUTANG" && tx.status != "CANCELLED") {
                                                    Button(
                                                        onClick = { viewModel.settlePiutang(tx.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("Pelunasan Piutang", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(Modifier.width(8.dp))
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val items = viewModel.getTransactionItems(tx.id)
                                                            triggerNativePrint(tx, items)
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Print, null, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Cetak Nota", fontSize = 10.sp)
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedTxForEdit = tx
                                                        editCustName = tx.customerName.orEmpty()
                                                        editPaymentMethod = tx.paymentMethod
                                                        editDate = tx.date
                                                        editTotalAmount = tx.total.toString()
                                                        editAmountPaid = tx.amountPaid?.toString().orEmpty()
                                                        editChange = tx.change?.toString().orEmpty()
                                                        editNotes = tx.notes.orEmpty()
                                                        showEditReceiptDialog = true
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Edit", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTransactionsHistoryDialog = false }) { Text("Tutup") }
                }
            )
        }

        // Dialog: Edit Struk / Transaksi
        if (showEditReceiptDialog && selectedTxForEdit != null) {
            val tx = selectedTxForEdit!!
            AlertDialog(
                onDismissRequest = { showEditReceiptDialog = false },
                title = { Text("Edit Struk #${tx.receiptNumber}") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = editCustName,
                            onValueChange = { editCustName = it },
                            label = { Text("Nama Pelanggan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Date Picker Button
                        val calendar = java.util.Calendar.getInstance()
                        calendar.timeInMillis = editDate
                        val datePickerDialog = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = java.util.Calendar.getInstance()
                                selectedCal.timeInMillis = editDate
                                selectedCal.set(java.util.Calendar.YEAR, year)
                                selectedCal.set(java.util.Calendar.MONTH, month)
                                selectedCal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                editDate = selectedCal.timeInMillis
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val dateStr = sdf.format(Date(editDate))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tanggal:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            OutlinedButton(onClick = { datePickerDialog.show() }) {
                                Text(dateStr, fontSize = 12.sp)
                            }
                        }

                        // Payment Method
                        Text("Metode Pembayaran:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val methods = listOf("CASH", "QRIS", "TRANSFER", "HUTANG")
                            methods.forEach { m ->
                                val active = editPaymentMethod == m
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editPaymentMethod = m }
                                ) {
                                    Text(
                                        text = m,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editTotalAmount,
                            onValueChange = { editTotalAmount = it },
                            label = { Text("Total Belanja (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editAmountPaid,
                            onValueChange = { editAmountPaid = it },
                            label = { Text("Jumlah Bayar (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editChange,
                            onValueChange = { editChange = it },
                            label = { Text("Uang Kembali (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Catatan / Notes") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val totalVal = editTotalAmount.toDoubleOrNull() ?: tx.total
                            val paidVal = editAmountPaid.toDoubleOrNull()
                            val changeVal = editChange.toDoubleOrNull()
                            viewModel.updateReceipt(
                                id = tx.id,
                                customerName = editCustName.takeIf { it.isNotBlank() },
                                paymentMethod = editPaymentMethod,
                                date = editDate,
                                amountPaid = paidVal,
                                change = changeVal,
                                notes = editNotes.takeIf { it.isNotBlank() },
                                total = totalVal
                            )
                            showEditReceiptDialog = false
                            Toast.makeText(context, "Struk berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditReceiptDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog: Log Aktivitas (Owner Only)
        if (showLogsDialog) {
            AlertDialog(
                onDismissRequest = { showLogsDialog = false },
                title = { Text("Log Aktivitas Kasir (Owner Only)") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                        if (activityLogsList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Belum ada log aktivitas tercatat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(activityLogsList, key = { it.id }) { log ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                                    val dateStr = sdf.format(Date(log.date))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = log.action,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = dateStr,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(log.description, fontSize = 12.sp)
                                            Spacer(Modifier.height(4.dp))
                                            Text("Oleh: ${log.employeeName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLogsDialog = false }) { Text("Tutup") }
                }
            )
        }



        // Dialog: Bagikan Toko Online
        if (showStoreShareDialog && activeStoreToken != null) {
            AlertDialog(
                onDismissRequest = { showStoreShareDialog = false },
                title = { Text("Bagikan Toko Online") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Gunakan link di bawah ini untuk membagikan toko online Anda ke pelanggan agar pelanggan dapat melihat katalog produk terupdate.", fontSize = 12.sp)
                        OutlinedTextField(
                            value = activeStoreToken!!,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Link Toko Online") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Link Toko Online", activeStoreToken)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Salin Link Toko")
                        }
                        Spacer(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                        Text("Uji coba alur toko online:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                showStoreShareDialog = false
                                showStoreSimulationDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Simulasi Toko Online")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStoreShareDialog = false }) { Text("Batal") }
                }
            )
        }

        // Dialog: Simulasi Toko Online (Real-time Stock)
        if (showStoreSimulationDialog && activeStoreToken != null) {
            val tokenStr = activeStoreToken!!.removePrefix(OnlineStoreLinkGenerator.BASE_URL)
            val validatedTenant = OnlineStoreLinkGenerator.validateToken(tokenStr)

            AlertDialog(
                onDismissRequest = { showStoreSimulationDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Browser: Toko Online")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (validatedTenant != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (validatedTenant != null) "Status: Aktif" else "Invalid",
                                color = if (validatedTenant != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(400.dp)
                    ) {
                        if (validatedTenant == null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🚫 Link Toko Online Tidak Valid", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Link toko online ini tidak valid atau salah format. Silakan hubungi kasir.", textAlign = TextAlign.Center, fontSize = 12.sp)
                            }
                        } else {
                            Text("Selamat Datang di Katalog Online Kami!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Berikut adalah daftar produk dan stok real-time saat ini:", fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                items(productList, key = { it.id }) { product ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!product.image.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = decodeBase64Image(product.image),
                                                            contentDescription = product.name,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    } else {
                                                        Text(
                                                            text = product.name.take(1).uppercase(),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Stok Real-time: ${product.stock} pcs", fontSize = 11.sp, color = if (product.stock > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            Text(Formatters.rupiah(product.price), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showStoreSimulationDialog = false }) { Text("Tutup Simulasi") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartSidebarContent(
    viewModel: PosViewModel,
    ui: PosUiState,
    customerList: List<CustomerEntity>,
    subtotal: Double,
    discountAmt: Double,
    total: Double,
    onPayClick: () -> Unit
) {
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keranjang Belanja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            TextButton(onClick = viewModel::clearCart) {
                Text("Bersihkan", color = MaterialTheme.colorScheme.error)
            }
        }

        // Cart items list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (viewModel.cart.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Keranjang kosong. Pilih produk di sebelah kiri.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(viewModel.cart, key = { it.cartKey }) { item ->
                    val unitPrice = viewModel.getItemUnitPrice(item)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    item.variantName?.let {
                                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromCart(item.cartKey) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Formatters.rupiah(unitPrice * item.quantity), fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateQty(item.cartKey, -1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(6.dp))
                                    ) {
                                        Text(
                                            item.quantity.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateQty(item.cartKey, 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Linking customer & adding invoice notes
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isUmum = ui.customerId == null
            Button(
                onClick = { viewModel.selectCustomer(null, null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUmum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isUmum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Umum (Walk-in)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { isCustomerDropdownExpanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isUmum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!isUmum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Pilih Pelanggan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = isCustomerDropdownExpanded,
            onExpandedChange = { isCustomerDropdownExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                readOnly = true,
                value = ui.customerName ?: "Pilih Pelanggan (Umum)",
                onValueChange = {},
                label = { Text("Pelanggan") },
                leadingIcon = { Icon(Icons.Outlined.People, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = isCustomerDropdownExpanded,
                onDismissRequest = { isCustomerDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Pelanggan Umum") },
                    onClick = {
                        viewModel.selectCustomer(null, null)
                        isCustomerDropdownExpanded = false
                    }
                )
                customerList.forEach { cust ->
                    DropdownMenuItem(
                        text = { Text(cust.name) },
                        onClick = {
                            viewModel.selectCustomer(cust.id, cust.name)
                            isCustomerDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ui.queueNumber,
                onValueChange = viewModel::updateQueueNumber,
                label = { Text("No Antrian") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ui.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Catatan Struk") },
                leadingIcon = { Icon(Icons.Outlined.Notes, null) },
                singleLine = true,
                modifier = Modifier.weight(1.8f)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Smart Discount Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalOffer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Diskon Pintar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (ui.discountType == "percent") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (ui.discountType == "percent") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { viewModel.updateDiscountType("percent") }
                ) {
                    Text("%", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (ui.discountType == "nominal") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (ui.discountType == "nominal") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { viewModel.updateDiscountType("nominal") }
                ) {
                    Text("Rp", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }

        OutlinedTextField(
            value = ui.discountInput,
            onValueChange = viewModel::updateDiscountInput,
            placeholder = { Text(if (ui.discountType == "percent") "Contoh: 10 untuk 10%" else "Contoh: 5000 untuk Rp 5.000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Cost calculation details
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Formatters.rupiah(subtotal), fontSize = 13.sp)
            }
            if (discountAmt > 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (ui.discountType == "percent") "Diskon (${ui.discountInput}%)" else "Diskon", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    Text("-${Formatters.rupiah(discountAmt)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("TOTAL", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(Formatters.rupiah(total), fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(14.dp))

        PrimaryButton(
            label = "Bayar sekarang",
            onClick = onPayClick,
            enabled = viewModel.cart.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn-checkout")
        )
    }
}

@Composable
private fun PaymentDialogContent(
    viewModel: PosViewModel,
    ui: PosUiState,
    total: Double
) {
    val context = LocalContext.current
    val quickCashOptions = remember(total) {
        val list = mutableListOf(total)
        val roundedOptions = listOf(5000.0, 10000.0, 20000.0, 50000.0, 100000.0)
        for (opt in roundedOptions) {
            if (opt > total) {
                list.add(opt)
            }
            val modulo = total % opt
            if (modulo > 0) {
                val roundUp = total + (opt - modulo)
                if (!list.contains(roundUp)) {
                    list.add(roundUp)
                }
            }
        }
        list.distinct().sorted().take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Payment methods row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val methods = listOf("CASH", "QRIS", "TRANSFER", "HUTANG")
            methods.forEach { m ->
                val active = ui.paymentMethod == m
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.updatePaymentMethod(m) }
                ) {
                    Text(
                        text = m,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        // Date selection (Backdate)
        val calendar = java.util.Calendar.getInstance()
        if (ui.selectedTransactionDate != null) {
            calendar.timeInMillis = ui.selectedTransactionDate
        }
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = java.util.Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                viewModel.updateTransactionDate(selectedCal.timeInMillis)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )

        val dateText = if (ui.selectedTransactionDate != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(ui.selectedTransactionDate))
        } else {
            "Hari Ini (Real-time)"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tanggal Transaksi:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedButton(onClick = { datePickerDialog.show() }) {
                Text(dateText, fontSize = 12.sp)
            }
        }

        if (ui.paymentMethod == "HUTANG") {
            OutlinedTextField(
                value = ui.debtDueDate,
                onValueChange = viewModel::updateDebtDueDate,
                label = { Text("Jatuh Tempo (yyyy-mm-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Amount Paid input
            OutlinedTextField(
                value = ui.amountPaid,
                onValueChange = viewModel::updateAmountPaid,
                label = { Text("Nominal Tunai yang Dibayar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick cash buttons
            Text("Uang Pas & Saran Pecahan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickCashOptions.forEach { opt ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateAmountPaid(opt.toInt().toString()) }
                    ) {
                        Text(
                            text = Formatters.rupiah(opt).replace("Rp", "").trim(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Calculation result
            val amtPaid = ui.amountPaid.toDoubleOrNull() ?: 0.0
            val change = (amtPaid - total).coerceAtLeast(0.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Uang Kembali:", fontWeight = FontWeight.Bold)
                Text(
                    Formatters.rupiah(change),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductCard(
    product: ProductEntity,
    hasVariants: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
            .testTag("prod-card-${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!product.image.isNullOrBlank()) {
                    AsyncImage(
                        model = decodeBase64Image(product.image),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = product.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
                if (hasVariants) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            "Varian",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                    modifier = Modifier.height(30.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.rupiah(product.price),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stok: ${product.stock}",
                        fontSize = 10.sp,
                        color = if (product.stock > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    val profit = product.price - product.costPrice
                    val marginPercent = if (product.price > 0) (profit / product.price) * 100 else 0.0
                    Text(
                        text = "${String.format("%.0f", marginPercent)}% mg",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Receipt helper HTML builder
private fun buildReceiptHtml(
    context: Context,
    t: TransactionEntity,
    items: List<TransactionItemEntity>,
    products: List<ProductEntity>
): String {
    val storeName = "PISANG KEJU RAMAYANA"
    val subheader = "Struk Pembayaran POS"
    val footer = "Terima Kasih! 🙏"

    val qLine = t.queueNumber?.let { "<div class=\"b c\" style=\"font-size:16px;border:2px solid #000;padding:4px;margin:4px 0\">No. Antrian: #$it</div>" } ?: ""
    val cLine = t.customerName?.let { "<div>Pelanggan: $it</div>" } ?: ""
    val notesLine = t.notes?.let { "<div>Catatan: $it</div>" } ?: ""

    val itemsHtml = items.map { item ->
        val prod = products.find { it.id == item.productId }
        val name = item.variantName?.let { "${prod?.name ?: "Item"} ($it)" } ?: (prod?.name ?: "Item")
        val note = item.note?.let { "<tr><td colspan=\"3\" class=\"indent\">&#8627; $it</td></tr>" } ?: ""
        val lineTotal = item.quantity * item.price - item.discount
        "<tr><td colspan=\"3\">$name</td></tr>$note<tr><td>${item.quantity}x</td><td>Rp${item.price.toLocaleString()}</td><td class=\"r\">Rp${lineTotal.toLocaleString()}</td></tr>"
    }.joinToString("")

    val dAmt = t.discountAmt
    val dLabel = if (t.discountType == "percent") "Diskon (${t.discountInput}%)" else "Diskon"
    val discRow = if (dAmt > 0) "<tr><td colspan=\"2\">$dLabel</td><td class=\"r\">-Rp${dAmt.toLocaleString()}</td></tr>" else ""
    val subRow = if (dAmt > 0) "<tr><td colspan=\"2\">Subtotal</td><td class=\"r\">Rp${t.subtotal.toLocaleString()}</td></tr>" else ""
    val paidRow = t.amountPaid?.let {
        "<tr><td colspan=\"2\">Tunai</td><td class=\"r\">Rp${it.toLocaleString()}</td></tr><tr><td colspan=\"2\">Kembali</td><td class=\"r\">Rp${(t.change ?: 0.0).toLocaleString()}</td></tr>"
    } ?: ""

    return """
        <html><head><style>
            @page { margin: 0; }
            body { font-family: monospace; width: 58mm; margin: 0 auto; padding: 0.6cm; font-size: 12px; }
            .c { text-align: center; }
            .b { font-weight: bold; }
            .r { text-align: right; }
            hr { border-top: 1px dashed #000; }
            td { vertical-align: top; padding: 1px 2px; }
            .indent { padding-left: 8px; color: #555; font-size: 11px; }
        </style></head><body>
            <div class="c b" style="font-size:16px">$storeName</div>
            <div class="c">$subheader</div><hr>
            <div>No: ${t.receiptNumber}</div><div>Metode: ${t.paymentMethod}</div>$cLine$notesLine<hr>
            $qLine
            <table width="100%">$itemsHtml</table><hr>
            <table width="100%">$subRow$discRow<tr><td colspan="2" class="b">TOTAL</td><td class="r b">Rp${t.total.toLocaleString()}</td></tr>$paidRow</table><hr>
            <div class="c">$footer</div>
            <script>window.print();window.onafterprint=()=>window.close()</script>
        </body></html>
    """.trimIndent()
}

private fun Number.toLocaleString(): String {
    return String.format("%,d", this.toLong()).replace(",", ".")
}

private fun printHtmlReceipt(context: Context, html: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter("POS_Receipt_${System.currentTimeMillis()}")
            printManager.print("POS Receipt", printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

@Composable
fun FooterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
