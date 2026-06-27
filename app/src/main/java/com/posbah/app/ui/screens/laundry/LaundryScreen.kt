package com.posbah.app.ui.screens.laundry

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.History
import com.posbah.app.ui.navigation.Screen
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Image
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.posbah.app.util.CameraUtils
import com.posbah.app.ui.print.ReceiptPrinter

import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import com.posbah.app.util.OnlineStoreLinkGenerator

data class LaundryServiceItem(
    val id: String,
    val name: String,
    val category: String, // KILOAN | SATUAN
    val price: Double,
    val costPrice: Double = 0.0,
    val image: String? = null,
    val unit: String, // Kg | Pcs
    val monthlyMaintenance: Double = 0.0
)

data class CartItem(
    val service: LaundryServiceItem,
    var quantity: Double
)

data class LaundryOrder(
    val id: String,
    val customerName: String,
    val phone: String,
    val itemsSummary: String,
    val total: Double,
    var paymentStatus: String, // LUNAS | BELUM LUNAS
    var orderStatus: String, // BARU | PROSES | SELESAI | DIAMBIL
    val dateIn: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LaundryScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToPrintSettings: () -> Unit,
    viewModel: LaundryViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val services by viewModel.services.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val tenantName by viewModel.tenantName.collectAsState()
    val cart = viewModel.cart
    val customerList by viewModel.customers.collectAsState(emptyList())
    val transactionList by viewModel.transactions.collectAsState(emptyList())
    val printConfig by viewModel.printConfig.collectAsState()

    val isOwner by viewModel.isOwner.collectAsState()
    val canViewMargin by viewModel.canViewMargin.collectAsState()
    val canAddService by viewModel.canAddService.collectAsState()
    val activityLogsList by viewModel.activityLogs.collectAsState()
    var showLogsDialog by remember { mutableStateOf(false) }
    var activeStoreToken by remember { mutableStateOf<String?>(null) }
    var showStoreShareDialog by remember { mutableStateOf(false) }
    var showStoreSimulationDialog by remember { mutableStateOf(false) }

    val outletList by viewModel.availableOutlets.collectAsState()
    val activeOutletId by viewModel.activeOutletId.collectAsState()
    val activeOutlet = remember(outletList, activeOutletId) {
        outletList.find { it.id == activeOutletId }
    }
    val activeOutletName = activeOutlet?.name ?: "Outlet Utama"
    var showOutletDropdown by remember { mutableStateOf(false) }
    var showOwnerMenuDropdown by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") } // NEW filter
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showCustomerSelectionDialog by remember { mutableStateOf(false) }

    var showActiveOrders by remember { mutableStateOf(false) }
    var activeReceiptOrder by remember { mutableStateOf<LaundryOrder?>(null) }
    var showReceiptDetails by remember { mutableStateOf<List<CartItem>?>(null) }

    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceCategory by remember { mutableStateOf("KILOAN") }
    var newServicePrice by remember { mutableStateOf("") }
    var newServiceCost by remember { mutableStateOf("") }
    var newServiceMonthlyMaintenance by remember { mutableStateOf("") }
    var newServiceUnit by remember { mutableStateOf("Kg") }
    var rentDateMillis by remember { mutableStateOf<Long?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseName by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var tempPhotoFile by remember { mutableStateOf<java.io.File?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<java.io.File?>(null) }

    var showEditServiceDialog by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<LaundryServiceItem?>(null) }
    var editServiceName by remember { mutableStateOf("") }
    var editServiceCategory by remember { mutableStateOf("KILOAN") }
    var editServicePrice by remember { mutableStateOf("") }
    var editServiceCost by remember { mutableStateOf("") }
    var editServiceMonthlyMaintenance by remember { mutableStateOf("") }
    var editServiceUnit by remember { mutableStateOf("Kg") }

    val onLongClickService = { service: LaundryServiceItem ->
        serviceToEdit = service
        editServiceName = service.name
        editServiceCategory = service.category
        editServicePrice = service.price.toString()
        editServiceCost = service.costPrice.toString()
        editServiceMonthlyMaintenance = service.monthlyMaintenance.toString()
        editServiceUnit = service.unit
        capturedPhotoFile = null
        showEditServiceDialog = true
    }

    // Mobile tab state: 0 = Layanan, 1 = Keranjang
    var mobileTabIndex by remember { mutableStateOf(0) }

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

    val filteredServices = remember(services, searchQuery, selectedCategory) {
        services.filter {
            val matchesSearch = it.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "Kiloan" -> it.category.equals("KILOAN", ignoreCase = true)
                "Satuan" -> it.category.equals("SATUAN", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    val cartSubtotal = remember(cart.toList()) {
        cart.sumOf { it.service.price * it.quantity }
    }
    val cartCount = cart.size
    val activeOrderCount = orders.count { it.orderStatus != "DIAMBIL" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box {
                PosBahTopBar(
                    title = tenantName,
                    subtitle = "Outlet: $activeOutletName",
                    onBack = onBack,
                    onTitleClick = if (isOwner) { { showOutletDropdown = true } } else null,
                    actions = {
                        if (canViewMargin) {
                            IconButton(onClick = { onNavigate(Screen.MarginAnalysis.route) }) {
                                Icon(Icons.Outlined.History, contentDescription = "Analisis Margin & Riwayat")
                            }
                        }
                        IconButton(onClick = onNavigateToPrintSettings) {
                            Icon(Icons.Outlined.Print, contentDescription = "Pengaturan Struk")
                        }
                        IconButton(onClick = {
                            activeStoreToken = OnlineStoreLinkGenerator.generateShareLink(viewModel.activeTenantId)
                            showStoreShareDialog = true
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Toko Online")
                        }
                        IconButton(onClick = {
                            expenseName = ""
                            expenseAmount = ""
                            expenseDate = System.currentTimeMillis()
                            showAddExpenseDialog = true
                        }) {
                            Icon(Icons.Outlined.Receipt, contentDescription = "Biaya / Bahan Baku")
                        }
                        if (isOwner) {
                            Box {
                                IconButton(onClick = { showOwnerMenuDropdown = true }) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                                        contentDescription = "Menu Owner"
                                    )
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showOwnerMenuDropdown,
                                    onDismissRequest = { showOwnerMenuDropdown = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Kontrol Outlet") },
                                        onClick = {
                                            onNavigate("owner/outlet_control")
                                            showOwnerMenuDropdown = false
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Kelola Karyawan") },
                                        onClick = {
                                            onNavigate("owner/employees")
                                            showOwnerMenuDropdown = false
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Log Aktivitas") },
                                        onClick = {
                                            showLogsDialog = true
                                            showOwnerMenuDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        if (canAddService) {
                            IconButton(onClick = {
                                newServiceName = ""
                                newServiceCategory = "KILOAN"
                                newServicePrice = ""
                                newServiceCost = ""
                                newServiceMonthlyMaintenance = ""
                                newServiceUnit = "Kg"
                                capturedPhotoFile = null
                                showAddServiceDialog = true
                            }) {
                                Icon(Icons.Outlined.LocalLaundryService, contentDescription = "Tambah Layanan")
                            }
                        }
                        // Compact order count button
                        TextButton(
                            onClick = { showActiveOrders = true },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Order ($activeOrderCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val isCompact = maxWidth < 600.dp

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    if (isCompact) {
                // === MOBILE LAYOUT: Vertical with Tabs ===
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tab Switcher
                    Surface(
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TabPill(
                                label = "Layanan",
                                icon = Icons.Outlined.LocalLaundryService,
                                active = mobileTabIndex == 0,
                                modifier = Modifier.weight(1f),
                                onClick = { mobileTabIndex = 0 }
                            )
                            TabPill(
                                label = "Keranjang ($cartCount)",
                                icon = Icons.Outlined.ShoppingCart,
                                active = mobileTabIndex == 1,
                                modifier = Modifier.weight(1f),
                                onClick = { mobileTabIndex = 1 }
                            )
                        }
                    }

                    if (mobileTabIndex == 0) {
                        ServiceCatalogPane(
                            services = services,
                            filteredServices = filteredServices,
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            onServiceClick = { service ->
                                val existing = cart.firstOrNull { it.service.id == service.id }
                                if (existing != null) {
                                    val step = if (service.unit == "Kg") 0.5 else 1.0
                                    val idx = cart.indexOf(existing)
                                    cart[idx] = existing.copy(quantity = existing.quantity + step)
                                } else {
                                    cart.add(CartItem(service, 1.0))
                                }
                                Toast.makeText(context, "${service.name} ditambahkan ke keranjang", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteService = { id ->
                                viewModel.deleteService(id) {
                                    Toast.makeText(context, "Layanan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLongClickService = onLongClickService,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    } else {
                        CartOrderPane(
                            cart = cart,
                            cartSubtotal = cartSubtotal,
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            phone = phone,
                            onPhoneChange = { phone = it },
                            rentDateMillis = rentDateMillis,
                            onRentDateChange = { rentDateMillis = it },
                            onPickCustomer = { showCustomerSelectionDialog = true },
                            onCheckout = {
                                if (customerName.isBlank() || phone.isBlank()) {
                                    Toast.makeText(context, "Mohon lengkapi Nama & Phone Pelanggan!", Toast.LENGTH_SHORT).show()
                                    return@CartOrderPane
                                }
                                if (cart.isEmpty()) {
                                    Toast.makeText(context, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
                                    return@CartOrderPane
                                }
                                val detailsCopy = cart.toList()
                                viewModel.checkout(customerName, phone, rentDateMillis) { newOrder ->
                                    showReceiptDetails = detailsCopy
                                    activeReceiptOrder = newOrder
                                    customerName = ""
                                    phone = ""
                                    rentDateMillis = null
                                    mobileTabIndex = 0
                                    Toast.makeText(context, "Order laundry berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            } else {
                // === TABLET LAYOUT: Side-by-side ===
                Row(modifier = Modifier.fillMaxSize()) {
                    ServiceCatalogPane(
                        services = services,
                        filteredServices = filteredServices,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        onServiceClick = { service ->
                            val existing = cart.firstOrNull { it.service.id == service.id }
                            if (existing != null) {
                                val step = if (service.unit == "Kg") 0.5 else 1.0
                                val idx = cart.indexOf(existing)
                                cart[idx] = existing.copy(quantity = existing.quantity + step)
                            } else {
                                cart.add(CartItem(service, 1.0))
                            }
                        },
                        onDeleteService = { id ->
                            viewModel.deleteService(id) {
                                Toast.makeText(context, "Layanan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLongClickService = onLongClickService,
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                    Surface(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight(),
                        tonalElevation = 1.dp,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        CartOrderPane(
                            cart = cart,
                            cartSubtotal = cartSubtotal,
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            phone = phone,
                            onPhoneChange = { phone = it },
                            rentDateMillis = rentDateMillis,
                            onRentDateChange = { rentDateMillis = it },
                            onPickCustomer = { showCustomerSelectionDialog = true },
                            onCheckout = {
                                if (customerName.isBlank() || phone.isBlank()) {
                                    Toast.makeText(context, "Mohon lengkapi Nama & Phone Pelanggan!", Toast.LENGTH_SHORT).show()
                                    return@CartOrderPane
                                }
                                if (cart.isEmpty()) {
                                    Toast.makeText(context, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
                                    return@CartOrderPane
                                }
                                val detailsCopy = cart.toList()
                                viewModel.checkout(customerName, phone, rentDateMillis) { newOrder ->
                                    showReceiptDetails = detailsCopy
                                    activeReceiptOrder = newOrder
                                    customerName = ""
                                    phone = ""
                                    rentDateMillis = null
                                    Toast.makeText(context, "Order laundry berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Close Box
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
        }
    }

    // ============= DIALOGS (unchanged behavior) =============

    if (showActiveOrders) {
        AlertDialog(
            onDismissRequest = { showActiveOrders = false },
            title = { Text("Daftar Order Laundry") },
            text = {
                var viewHistoryOrders by remember { mutableStateOf(false) }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewHistoryOrders = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!viewHistoryOrders) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!viewHistoryOrders) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Order Aktif", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewHistoryOrders = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewHistoryOrders) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (viewHistoryOrders) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Riwayat Diambil", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    var dialogSearchQuery by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = dialogSearchQuery,
                        onValueChange = { dialogSearchQuery = it },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (dialogSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { dialogSearchQuery = "" }) {
                                    Icon(Icons.Outlined.Clear, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        placeholder = { Text("Cari pelanggan, order ID, item...", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    val filteredOrders = remember(orders, viewHistoryOrders, dialogSearchQuery) {
                        orders.filter {
                            val matchesStatus = if (viewHistoryOrders) it.orderStatus == "DIAMBIL" else it.orderStatus != "DIAMBIL"
                            val matchesSearch = it.customerName.contains(dialogSearchQuery, ignoreCase = true) ||
                                    it.id.contains(dialogSearchQuery, ignoreCase = true) ||
                                    it.itemsSummary.contains(dialogSearchQuery, ignoreCase = true)
                            matchesStatus && matchesSearch
                        }
                    }

                    if (filteredOrders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("Tidak ada order", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(320.dp)
                        ) {
                            items(filteredOrders) { o ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(o.id, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = {
                                                        viewModel.getReceiptDetails(o.id) { details ->
                                                            showReceiptDetails = details
                                                            activeReceiptOrder = o
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Receipt,
                                                        "Nota",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(Formatters.rupiah(o.total), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Pelanggan: ${o.customerName}", fontSize = 12.sp)
                                        Text("WhatsApp: ${o.phone}", fontSize = 11.sp)
                                        Text("Item: ${o.itemsSummary}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = {
                                                    val next = if (o.paymentStatus == "LUNAS") "BELUM LUNAS" else "LUNAS"
                                                    viewModel.updatePaymentStatus(o.id, next)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (o.paymentStatus == "LUNAS") Color(0xFF22C57E) else MaterialTheme.colorScheme.outline
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(o.paymentStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    val next = when (o.orderStatus) {
                                                        "BARU" -> "PROSES"
                                                        "PROSES" -> "SELESAI"
                                                        "SELESAI" -> "DIAMBIL"
                                                        else -> "BARU"
                                                    }
                                                    viewModel.updateOrderStatus(o.id, next)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Status: ${o.orderStatus}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (o.orderStatus == "SELESAI") {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val cleanPhone = o.phone.trim()
                                                            val formattedPhone = if (cleanPhone.startsWith("0")) {
                                                                "62" + cleanPhone.substring(1)
                                                            } else {
                                                                cleanPhone
                                                            }
                                                            val msg = "Halo, pesanan laundry Anda dengan ID ${o.id} telah SELESAI. Silakan mengambil pesanan Anda di outlet kami atau hubungi kami untuk pengantaran. Terima kasih!"
                                                            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${android.net.Uri.encode(msg)}"
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Gagal membuka WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF25D366),
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp).testTag("btn-notify-laundry-wa-${o.id}")
                                                ) {
                                                    Text("Kirim WA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
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
                TextButton(onClick = { showActiveOrders = false }) { Text("Tutup") }
            }
        )
    }

    if (activeReceiptOrder != null) {
        val r = activeReceiptOrder!!
        val details = showReceiptDetails ?: emptyList()
        AlertDialog(
            onDismissRequest = { activeReceiptOrder = null; showReceiptDetails = null },
            title = { Text("Nota Laundry Digital") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFFFF8),
                        border = BorderStroke(0.5.dp, Color.Gray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Text("POSBAH LAUNDRY & CLEANING", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Text("Nota Jasa Laundry", fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                                Text("Order ID: ${r.id}", fontSize = 10.sp)
                                Text("Pelanggan: ${r.customerName}", fontSize = 10.sp)
                                Text("WhatsApp: ${r.phone}", fontSize = 10.sp)
                                Text("Tanggal Masuk: ${Formatters.dateLong(r.dateIn)}", fontSize = 10.sp)
                                Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                            }
                            items(details) { item ->
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    val qtyStr = if (item.service.unit == "Kg") "%.1f Kg".format(item.quantity) else "${item.quantity.toInt()} Pcs"
                                    Text("${item.service.name}\nx$qtyStr", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text(Formatters.rupiah(item.service.price * item.quantity), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("TOTAL BIAYA", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text(Formatters.rupiah(r.total), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Status Bayar", fontSize = 11.sp)
                                    Text(r.paymentStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (r.paymentStatus == "LUNAS") Color(0xFF22C57E) else Color.Red)
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("Terima kasih atas order Anda! 🙏", fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeReceiptOrder = null; showReceiptDetails = null }) { Text("Tutup") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ReceiptPrinter.print(
                            context,
                            ReceiptPrinter.generateLaundryReceiptHtml(context, r, details, printConfig, tenantName)
                        )
                    }
                ) { Text("Cetak Nota") }
            }
        )
    }

    if (showAddServiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddServiceDialog = false },
            title = { Text("Tambah Layanan Laundry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newServiceName,
                        onValueChange = { nameVal -> newServiceName = nameVal },
                        label = { Text("Nama Layanan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add-service-name")
                    )
                    OutlinedTextField(
                        value = newServicePrice,
                        onValueChange = { priceVal -> newServicePrice = priceVal },
                        label = { Text("Harga Layanan / Tarif Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add-service-price")
                    )
                    OutlinedTextField(
                        value = newServiceCost,
                        onValueChange = { costVal -> newServiceCost = costVal },
                        label = { Text("Harga Modal / Beli (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newServiceMonthlyMaintenance,
                        onValueChange = { mmVal -> newServiceMonthlyMaintenance = mmVal },
                        label = { Text("Biaya Operasional Bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val jual = newServicePrice.toDoubleOrNull() ?: 0.0
                    val beli = newServiceCost.toDoubleOrNull() ?: 0.0
                    val margin = if (jual > 0) ((jual - beli) / jual) * 100 else 0.0
                    Text(
                        text = "Margin Keuntungan: ${String.format("%.1f", margin)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (margin >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Text("Kategori Layanan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                newServiceCategory = "KILOAN"
                                newServiceUnit = "Kg"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newServiceCategory == "KILOAN") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (newServiceCategory == "KILOAN") MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("KILOAN (Kg)") }
                        Button(
                            onClick = {
                                newServiceCategory = "SATUAN"
                                newServiceUnit = "Pcs"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newServiceCategory == "SATUAN") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (newServiceCategory == "SATUAN") MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("SATUAN (Pcs)") }
                    }

                    Spacer(Modifier.height(4.dp))

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
                                Icon(Icons.Outlined.Clear, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
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
                        val rate = newServicePrice.toDoubleOrNull() ?: 0.0
                        val cost = newServiceCost.toDoubleOrNull() ?: 0.0
                        val monthlyMaint = newServiceMonthlyMaintenance.toDoubleOrNull() ?: 0.0
                        if (newServiceName.isNotBlank() && rate > 0) {
                            viewModel.addService(newServiceName, rate, cost, monthlyMaint, newServiceCategory, newServiceUnit, capturedPhotoFile) {
                                showAddServiceDialog = false
                                Toast.makeText(context, "Layanan baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Semua kolom wajib diisi dengan benar!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn-save-new-service")
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddServiceDialog = false }) { Text("Batal") }
            }
        )
    }

    // Dialog: Edit Layanan
    if (showEditServiceDialog && serviceToEdit != null) {
        val originalService = serviceToEdit!!
        AlertDialog(
            onDismissRequest = { showEditServiceDialog = false },
            title = { Text("Edit Layanan Laundry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editServiceName,
                        onValueChange = { editServiceName = it },
                        label = { Text("Nama Layanan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editServicePrice,
                        onValueChange = { editServicePrice = it },
                        label = { Text("Harga Layanan / Tarif Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editServiceCost,
                        onValueChange = { editServiceCost = it },
                        label = { Text("Harga Modal / Beli (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editServiceMonthlyMaintenance,
                        onValueChange = { editServiceMonthlyMaintenance = it },
                        label = { Text("Biaya Operasional Bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val jual = editServicePrice.toDoubleOrNull() ?: 0.0
                    val beli = editServiceCost.toDoubleOrNull() ?: 0.0
                    val margin = if (jual > 0) ((jual - beli) / jual) * 100 else 0.0
                    Text(
                        text = "Margin Keuntungan: ${String.format("%.1f", margin)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (margin >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Text("Kategori Layanan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                editServiceCategory = "KILOAN"
                                editServiceUnit = "Kg"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editServiceCategory == "KILOAN") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (editServiceCategory == "KILOAN") MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("KILOAN (Kg)") }
                        Button(
                            onClick = {
                                editServiceCategory = "SATUAN"
                                editServiceUnit = "Pcs"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editServiceCategory == "SATUAN") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (editServiceCategory == "SATUAN") MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("SATUAN (Pcs)") }
                    }

                    Spacer(Modifier.height(4.dp))

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
                                Icon(Icons.Outlined.Clear, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else if (!originalService.image.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = decodeBase64Image(originalService.image),
                                contentDescription = "Foto Layanan Saat Ini",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
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
                        val rate = editServicePrice.toDoubleOrNull() ?: 0.0
                        val cost = editServiceCost.toDoubleOrNull() ?: 0.0
                        val monthlyMaint = editServiceMonthlyMaintenance.toDoubleOrNull() ?: 0.0
                        if (editServiceName.isNotBlank() && rate > 0) {
                            val keepExisting = capturedPhotoFile == null && !originalService.image.isNullOrBlank()
                            val prodEntity = viewModel.products.value.find { it.id.toString() == originalService.id }
                            if (prodEntity != null) {
                                viewModel.editService(
                                    product = prodEntity,
                                    name = editServiceName,
                                    price = rate,
                                    costPrice = cost,
                                    monthlyMaintenance = monthlyMaint,
                                    category = editServiceCategory,
                                    unit = editServiceUnit,
                                    imageFile = capturedPhotoFile,
                                    keepExistingImage = keepExisting
                                ) {
                                    showEditServiceDialog = false
                                    Toast.makeText(context, "Layanan laundry berhasil diubah!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Gagal menemukan data asli layanan!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Nama dan harga wajib diisi!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditServiceDialog = false }) { Text("Batal") }
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

    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text("Log Aktivitas Laundry (Owner Only)") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    if (activityLogsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Belum ada log aktivitas laundry.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            Text(log.action, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(dateStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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
                        Text("Berikut adalah daftar layanan dan status saat ini:", fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(services, key = { it.id }) { service ->
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
                                                if (!service.image.isNullOrBlank()) {
                                                    coil.compose.AsyncImage(
                                                        model = decodeBase64Image(service.image),
                                                        contentDescription = service.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = service.name.take(1).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(service.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Status: Aktif", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text("${Formatters.rupiah(service.price)} / ${service.unit}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
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

    if (showCustomerSelectionDialog) {
        var custNameInput by remember { mutableStateOf("") }
        var custPhoneInput by remember { mutableStateOf("") }
        var custAddressInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCustomerSelectionDialog = false },
            title = { Text("Pilih / Tambah Pelanggan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tambah Pelanggan Baru", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = custNameInput,
                        onValueChange = { custNameInput = it },
                        label = { Text("Nama Pelanggan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custPhoneInput,
                        onValueChange = { custPhoneInput = it },
                        label = { Text("No. WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custAddressInput,
                        onValueChange = { custAddressInput = it },
                        label = { Text("Alamat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (custNameInput.isNotBlank()) {
                                viewModel.addCustomer(custNameInput, custPhoneInput, custAddressInput) {
                                    Toast.makeText(context, "Pelanggan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                    custNameInput = ""
                                    custPhoneInput = ""
                                    custAddressInput = ""
                                }
                            } else {
                                Toast.makeText(context, "Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Pelanggan Baru")
                    }

                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Daftar Pelanggan Terdaftar", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
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
                                    val custTx = transactionList.filter {
                                        it.customerName.equals(cust.name, ignoreCase = true)
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                customerName = cust.name
                                                phone = cust.phone.orEmpty()
                                                showCustomerSelectionDialog = false
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text("Pilih", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
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
                TextButton(onClick = { showCustomerSelectionDialog = false }) { Text("Tutup") }
            }
        )
    }
}

// ====================== EXTRACTED PANES ======================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServiceCatalogPane(
    services: List<LaundryServiceItem>,
    filteredServices: List<LaundryServiceItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onServiceClick: (LaundryServiceItem) -> Unit,
    onDeleteService: (String) -> Unit,
    onLongClickService: (LaundryServiceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // === HEADER: title + count badge ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Pilih Layanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    "Tap untuk masukkan ke keranjang",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Text(
                    "${services.size} Layanan",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // === SEARCH + FILTER (horizontal compact) ===
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Outlined.Clear, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    placeholder = { Text("Cari layanan...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("laundry-search")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Semua", "Kiloan", "Satuan").forEach { cat ->
                        val active = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (active) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCategoryChange(cat) }
                        ) {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredServices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.LocalLaundryService,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Belum ada layanan",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredServices) { service ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onServiceClick(service) },
                                onLongClick = { onLongClickService(service) }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                if (!service.image.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = service.image,
                                        contentDescription = service.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.LocalLaundryService,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    service.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (service.category == "KILOAN") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = if (service.category == "KILOAN") "Kiloan" else "Satuan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (service.category == "KILOAN") MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 9.sp
                                        )
                                    }
                                    val marginPercent = if (service.price > 0) ((service.price - service.costPrice) / service.price) * 100 else 0.0
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (marginPercent >= 0) Color(0xFFE2FBE7) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "${String.format("%.0f", marginPercent)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (marginPercent >= 0) Color(0xFF1E824C) else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    Formatters.rupiah(service.price),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    "/ ${service.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { onDeleteService(service.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Clear,
                                        contentDescription = "Hapus Layanan",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartOrderPane(
    cart: SnapshotStateList<CartItem>,
    cartSubtotal: Double,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    rentDateMillis: Long?,
    onRentDateChange: (Long?) -> Unit,
    onPickCustomer: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Keranjang Order",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Text(
                    "${cart.size} Item",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (cart.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Keranjang kosong.\nPilih layanan di tab Layanan.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(cart) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.service.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${Formatters.rupiah(item.service.price)} / ${item.service.unit}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val step = if (item.service.unit == "Kg") 0.5 else 1.0
                                        if (item.quantity > step) {
                                            val idx = cart.indexOf(item)
                                            cart[idx] = item.copy(quantity = item.quantity - step)
                                        } else {
                                            cart.remove(item)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.RemoveCircleOutline,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (item.service.unit == "Kg") "%.1f".format(item.quantity) else item.quantity.toInt().toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    item.service.unit,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val step = if (item.service.unit == "Kg") 0.5 else 1.0
                                        val idx = cart.indexOf(item)
                                        cart[idx] = item.copy(quantity = item.quantity + step)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "+",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Customer Profile Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Profil Pelanggan",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isUmum = customerName == "Umum" && phone == ""
                    Button(
                        onClick = {
                            onCustomerNameChange("Umum")
                            onPhoneChange("")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUmum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isUmum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Umum", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onPickCustomer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isUmum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isUmum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Pilih Pelanggan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Nama Pelanggan", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Outlined.People, null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("laundry-cust-name")
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("WhatsApp", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("laundry-phone")
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Date + Subtotal Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                val calendar = java.util.Calendar.getInstance()
                if (rentDateMillis != null) calendar.timeInMillis = rentDateMillis
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = java.util.Calendar.getInstance()
                        selectedCal.set(year, month, dayOfMonth)
                        onRentDateChange(selectedCal.timeInMillis)
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )
                val dateText = if (rentDateMillis != null) {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(rentDateMillis))
                } else "Hari Ini"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tanggal Order:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(dateText, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(Formatters.rupiah(cartSubtotal), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onCheckout,
            enabled = cart.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn-confirm-laundry")
        ) {
            Text("Buat Order Laundry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

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
