package com.posbah.app.ui.screens.rental

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
import androidx.compose.material.icons.outlined.CarRental
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import com.posbah.app.util.OnlineStoreLinkGenerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.navigation.Screen
import com.posbah.app.util.Formatters
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.posbah.app.util.CameraUtils
import com.posbah.app.ui.print.ReceiptPrinter

data class Vehicle(
    val id: String,
    val name: String,
    val plateNumber: String,
    val type: String, // MOBIL | MOTOR
    val pricePerDay: Double,
    val costPrice: Double = 0.0,
    val image: String? = null,
    var isRented: Boolean = false,
    var activeRenterName: String? = null,
    var rentExpiry: Long? = null,
    val monthlyMaintenance: Double = 0.0
)

data class RentalOrder(
    val id: String,
    val vehicleId: String,
    val vehicleName: String,
    val customerName: String,
    val whatsapp: String,
    val days: Int,
    val total: Double,
    val paid: Double,
    val rentDate: Long,
    val status: String // ACTIVE | RETURNED
)

@Composable
fun RentalScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToPrintSettings: () -> Unit,
    viewModel: RentalViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val vehicles by viewModel.vehicles.collectAsState()
    val rentalOrders by viewModel.rentalOrders.collectAsState()
    val tenantName by viewModel.tenantName.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val canViewMargin by viewModel.canViewMargin.collectAsState()
    val activityLogsList by viewModel.activityLogs.collectAsState()
    val customerList by viewModel.customers.collectAsState(emptyList())
    val transactionList by viewModel.transactions.collectAsState(emptyList())
    val printConfig by viewModel.printConfig.collectAsState()

    val outletList by viewModel.availableOutlets.collectAsState()
    val activeOutletId by viewModel.activeOutletId.collectAsState()
    val activeOutlet = remember(outletList, activeOutletId) {
        outletList.find { it.id == activeOutletId }
    }
    val activeOutletName = activeOutlet?.name ?: "Outlet Utama"
    var showOutletDropdown by remember { mutableStateOf(false) }
    var showOwnerMenuDropdown by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedStatus by remember { mutableStateOf("Semua") }
    var selectedVehicleForRent by remember { mutableStateOf<Vehicle?>(null) }
    var customerName by remember { mutableStateOf("") }
    var rentDays by remember { mutableStateOf("1") }
    var whatsapp by remember { mutableStateOf("") }
    var cashPaid by remember { mutableStateOf("") }
    var showCustomerSelectionDialog by remember { mutableStateOf(false) }

    var activeReceiptOrder by remember { mutableStateOf<RentalOrder?>(null) }
    var selectedOrderForReturn by remember { mutableStateOf<RentalOrder?>(null) }
    var lateDaysInput by remember { mutableStateOf("0") }

    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var newVehicleName by remember { mutableStateOf("") }
    var newVehiclePlate by remember { mutableStateOf("") }
    var newVehicleType by remember { mutableStateOf("MOBIL") }
    var newVehiclePrice by remember { mutableStateOf("") }
    var newVehicleCost by remember { mutableStateOf("") }
    var newVehicleMonthlyMaintenance by remember { mutableStateOf("") }
    var rentDateMillis by remember { mutableStateOf<Long?>(null) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseName by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var activeStoreToken by remember { mutableStateOf<String?>(null) }
    var showStoreShareDialog by remember { mutableStateOf(false) }
    var showStoreSimulationDialog by remember { mutableStateOf(false) }

    var tempPhotoFile by remember { mutableStateOf<java.io.File?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<java.io.File?>(null) }

    // Mobile tab: 0 = Armada (catalog), 1 = Sewa (orders)
    var mobileTabIndex by remember { mutableStateOf(0) }
    // Right pane (tablet) / orders pane: history toggle
    var showHistory by remember { mutableStateOf(false) }

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

    val filteredVehicles = remember(vehicles, searchQuery, selectedCategory, selectedStatus) {
        vehicles.filter { vehicle ->
            val matchesSearch = vehicle.name.contains(searchQuery, ignoreCase = true) ||
                    vehicle.plateNumber.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "Mobil" -> vehicle.type.equals("MOBIL", ignoreCase = true)
                "Motor" -> vehicle.type.equals("MOTOR", ignoreCase = true)
                else -> true
            }

            val matchesStatus = when (selectedStatus) {
                "Tersedia" -> !vehicle.isRented
                "Disewa" -> vehicle.isRented
                else -> true
            }

            matchesSearch && matchesCategory && matchesStatus
        }
    }

    val activeOrderCount = rentalOrders.count { it.status == "ACTIVE" }

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
                                        imageVector = Icons.Default.MoreVert,
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
                        if (canViewMargin) {
                            IconButton(onClick = {
                                newVehicleName = ""
                                newVehiclePlate = ""
                                newVehicleType = "MOBIL"
                                newVehiclePrice = ""
                                newVehicleCost = ""
                                newVehicleMonthlyMaintenance = ""
                                capturedPhotoFile = null
                                showAddVehicleDialog = true
                            }) {
                                Icon(Icons.Outlined.DirectionsCar, contentDescription = "Tambah Armada")
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
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TabPill(
                                label = "Armada (${vehicles.size})",
                                icon = Icons.Outlined.DirectionsCar,
                                active = mobileTabIndex == 0,
                                modifier = Modifier.weight(1f),
                                onClick = { mobileTabIndex = 0 }
                            )
                            TabPill(
                                label = "Sewa ($activeOrderCount)",
                                icon = Icons.Outlined.CarRental,
                                active = mobileTabIndex == 1,
                                modifier = Modifier.weight(1f),
                                onClick = { mobileTabIndex = 1 }
                            )
                        }
                    }

                    if (mobileTabIndex == 0) {
                        VehicleCatalogPane(
                            vehicles = vehicles,
                            filteredVehicles = filteredVehicles,
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            selectedStatus = selectedStatus,
                            onStatusChange = { selectedStatus = it },
                            onVehicleClick = { vehicle ->
                                if (!vehicle.isRented) {
                                    selectedVehicleForRent = vehicle
                                    customerName = ""
                                    rentDays = "1"
                                    whatsapp = ""
                                    cashPaid = vehicle.pricePerDay.toLong().toString()
                                }
                            },
                            onDeleteVehicle = { id ->
                                viewModel.deleteVehicle(id) {
                                    Toast.makeText(context, "Armada berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    } else {
                        RentalOrdersPane(
                            rentalOrders = rentalOrders,
                            showHistory = showHistory,
                            onToggleHistory = { showHistory = it },
                            onViewReceipt = { activeReceiptOrder = it },
                            onReturn = { order ->
                                selectedOrderForReturn = order
                                lateDaysInput = "0"
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
                    VehicleCatalogPane(
                        vehicles = vehicles,
                        filteredVehicles = filteredVehicles,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        selectedStatus = selectedStatus,
                        onStatusChange = { selectedStatus = it },
                        onVehicleClick = { vehicle ->
                            if (!vehicle.isRented) {
                                selectedVehicleForRent = vehicle
                                customerName = ""
                                rentDays = "1"
                                whatsapp = ""
                                cashPaid = vehicle.pricePerDay.toLong().toString()
                            }
                        },
                        onDeleteVehicle = { id ->
                            viewModel.deleteVehicle(id) {
                                Toast.makeText(context, "Armada berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            }
                        },
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
                        RentalOrdersPane(
                            rentalOrders = rentalOrders,
                            showHistory = showHistory,
                            onToggleHistory = { showHistory = it },
                            onViewReceipt = { activeReceiptOrder = it },
                            onReturn = { order ->
                                selectedOrderForReturn = order
                                lateDaysInput = "0"
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

    // =================== DIALOGS ===================

    if (selectedVehicleForRent != null) {
        val vehicle = selectedVehicleForRent!!
        val daysInt = rentDays.toIntOrNull() ?: 1
        val computedTotal = vehicle.pricePerDay * daysInt
        LaunchedEffect(rentDays) {
            cashPaid = computedTotal.toLong().toString()
        }

        AlertDialog(
            onDismissRequest = {
                selectedVehicleForRent = null
                rentDateMillis = null
            },
            title = {
                Text(
                    "Formulir Sewa: ${vehicle.name}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Informasi Penyewa",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isUmum = customerName == "Umum" && whatsapp == ""
                                Button(
                                    onClick = {
                                        customerName = "Umum"
                                        whatsapp = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isUmum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isUmum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Umum", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { showCustomerSelectionDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isUmum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (!isUmum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Pilih Pelanggan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Nama Pelanggan") },
                                leadingIcon = { Icon(Icons.Outlined.People, null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("rental-cust-name")
                            )
                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = { Text("No. WhatsApp") },
                                leadingIcon = { Icon(Icons.Outlined.People, null, modifier = Modifier.size(18.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("rental-phone")
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Pengaturan Sewa",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = rentDays,
                                    onValueChange = { rentDays = it },
                                    label = { Text("Durasi (Hari)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("rental-days")
                                )
                                OutlinedTextField(
                                    value = cashPaid,
                                    onValueChange = { cashPaid = it },
                                    label = { Text("Uang Muka (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.5f).testTag("rental-cash")
                                )
                            }

                            val calendar = java.util.Calendar.getInstance()
                            if (rentDateMillis != null) {
                                calendar.timeInMillis = rentDateMillis!!
                            }
                            val datePickerDialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = java.util.Calendar.getInstance()
                                    selectedCal.set(year, month, dayOfMonth)
                                    rentDateMillis = selectedCal.timeInMillis
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            )

                            val dateText = if (rentDateMillis != null) {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(rentDateMillis!!))
                            } else "Hari Ini"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mulai Sewa:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                OutlinedButton(
                                    onClick = { datePickerDialog.show() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(dateText, fontSize = 11.sp)
                                }
                            }

                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Tarif per Hari", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(Formatters.rupiah(vehicle.pricePerDay), fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("TOTAL BIAYA", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(Formatters.rupiah(computedTotal), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerName.isBlank() || whatsapp.isBlank()) {
                            Toast.makeText(context, "Mohon lengkapi semua field!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val finalPaid = cashPaid.toDoubleOrNull() ?: computedTotal
                        viewModel.rentVehicle(vehicle, customerName, whatsapp, daysInt, finalPaid, rentDateMillis) { newOrder ->
                            selectedVehicleForRent = null
                            activeReceiptOrder = newOrder
                            rentDateMillis = null
                            // pindah ke tab Sewa di HP
                            mobileTabIndex = 1
                            Toast.makeText(context, "Sewa berhasil diproses!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn-confirm-rental")
                ) { Text("Proses Sewa") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedVehicleForRent = null
                        rentDateMillis = null
                    }
                ) { Text("Batal") }
            }
        )
    }

    if (selectedOrderForReturn != null) {
        val order = selectedOrderForReturn!!
        val price = order.total / order.days
        val lateDays = lateDaysInput.toIntOrNull() ?: 0
        val lateFee = lateDays * price * 1.5

        AlertDialog(
            onDismissRequest = { selectedOrderForReturn = null },
            title = { Text("Pengembalian Armada") },
            text = {
                Column {
                    Text(
                        "Konfirmasi pengembalian untuk armada ${order.vehicleName} yang disewa oleh ${order.customerName}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = lateDaysInput,
                        onValueChange = { lateDaysInput = it },
                        label = { Text("Keterlambatan (Hari)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rental-late-days")
                    )
                    if (lateDays > 0) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Denda Terlambat (150%):", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(Formatters.rupiah(lateFee), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.returnVehicle(order, lateDays) {
                            selectedOrderForReturn = null
                            Toast.makeText(context, "Armada berhasil dikembalikan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C57E))
                ) { Text("Konfirmasi Kembali") }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForReturn = null }) { Text("Batal") }
            }
        )
    }

    if (activeReceiptOrder != null) {
        val r = activeReceiptOrder!!
        AlertDialog(
            onDismissRequest = { activeReceiptOrder = null },
            title = { Text("Nota Sewa / Struk Transaksi") },
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
                                Text("POSBAH CAR & MOTOR RENTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Text("Struk Penyewaan Resmi", fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                                Text("No Transaksi: ${r.id}", fontSize = 10.sp)
                                Text("Penyewa: ${r.customerName}", fontSize = 10.sp)
                                Text("WhatsApp: ${r.whatsapp}", fontSize = 10.sp)
                                Text("Tanggal Sewa: ${Formatters.dateLong(r.rentDate)}", fontSize = 10.sp)
                                Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("${r.vehicleName}\n@ ${r.days} Hari", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text(Formatters.rupiah(r.total), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            item {
                                Text("-".repeat(34), fontSize = 10.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("TOTAL BAYAR", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text(Formatters.rupiah(r.total), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Uang Muka/Cash", fontSize = 11.sp)
                                    Text(Formatters.rupiah(r.paid), fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Sisa Pembayaran", fontSize = 11.sp)
                                    Text(Formatters.rupiah(if (r.total - r.paid > 0) r.total - r.paid else 0.0), fontSize = 11.sp)
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("Terima kasih atas kepercayaan Anda! 🙏", fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Text("Harap kembalikan armada tepat waktu.", fontSize = 9.sp, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeReceiptOrder = null }) { Text("Tutup") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ReceiptPrinter.print(
                            context,
                            ReceiptPrinter.generateRentalReceiptHtml(context, r, printConfig, tenantName)
                        )
                    }
                ) { Text("Cetak Struk") }
            }
        )
    }

    if (showAddVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Tambah Armada Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newVehicleName,
                        onValueChange = { newVehicleName = it },
                        label = { Text("Nama Kendaraan") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add-vehicle-name")
                    )
                    OutlinedTextField(
                        value = newVehiclePlate,
                        onValueChange = { newVehiclePlate = it },
                        label = { Text("Nomor Plat Kendaraan") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add-vehicle-plate")
                    )
                    OutlinedTextField(
                        value = newVehiclePrice,
                        onValueChange = { newVehiclePrice = it },
                        label = { Text("Tarif Jual per Hari (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add-vehicle-price")
                    )
                    OutlinedTextField(
                        value = newVehicleCost,
                        onValueChange = { newVehicleCost = it },
                        label = { Text("Biaya Modal/Beli (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVehicleMonthlyMaintenance,
                        onValueChange = { newVehicleMonthlyMaintenance = it },
                        label = { Text("Biaya Perawatan Bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val jual = newVehiclePrice.toDoubleOrNull() ?: 0.0
                    val beli = newVehicleCost.toDoubleOrNull() ?: 0.0
                    val margin = if (jual > 0) ((jual - beli) / jual) * 100 else 0.0
                    Text(
                        text = "Margin Keuntungan: ${String.format("%.1f", margin)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (margin >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Text("Tipe Kendaraan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { newVehicleType = "MOBIL" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newVehicleType == "MOBIL") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (newVehicleType == "MOBIL") MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("MOBIL") }
                        Button(
                            onClick = { newVehicleType = "MOTOR" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newVehicleType == "MOTOR") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (newVehicleType == "MOTOR") MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("MOTOR") }
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
                                Icon(Icons.Outlined.Close, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = launchCamera,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ambil Foto Kendaraan")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = newVehiclePrice.toDoubleOrNull() ?: 0.0
                        val cost = newVehicleCost.toDoubleOrNull() ?: 0.0
                        val monthlyMaint = newVehicleMonthlyMaintenance.toDoubleOrNull() ?: 0.0
                        if (newVehicleName.isNotBlank() && newVehiclePlate.isNotBlank() && rate > 0) {
                            viewModel.addVehicle(newVehicleName, newVehiclePlate, newVehicleType, rate, cost, monthlyMaint, capturedPhotoFile) {
                                showAddVehicleDialog = false
                                Toast.makeText(context, "Armada baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Semua kolom wajib diisi dengan benar!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn-save-new-vehicle")
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) { Text("Batal") }
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
                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expenseDate))

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
            title = { Text("Log Aktivitas Rental (Owner Only)") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    if (activityLogsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Belum ada log aktivitas rental.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activityLogsList, key = { it.id }) { log ->
                                val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date(log.date))
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
                        Text("Berikut adalah daftar produk dan status real-time saat ini:", fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(vehicles, key = { it.id }) { vehicle ->
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
                                                if (!vehicle.image.isNullOrBlank()) {
                                                    coil.compose.AsyncImage(
                                                        model = decodeBase64Image(vehicle.image),
                                                        contentDescription = vehicle.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = vehicle.name.take(1).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(vehicle.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Status: ${if (vehicle.isRented) "Disewa" else "Tersedia"}", fontSize = 11.sp, color = if (!vehicle.isRented) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        Text(Formatters.rupiah(vehicle.pricePerDay), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tambah Pelanggan Baru", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = custNameInput,
                        onValueChange = { custNameInput = it },
                        label = { Text("Nama Pelanggan") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custPhoneInput,
                        onValueChange = { custPhoneInput = it },
                        label = { Text("No. WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custAddressInput,
                        onValueChange = { custAddressInput = it },
                        label = { Text("Alamat") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
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
                                    } else "Belum ada transaksi"

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                customerName = cust.name
                                                whatsapp = cust.phone.orEmpty()
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

@Composable
private fun VehicleCatalogPane(
    vehicles: List<Vehicle>,
    filteredVehicles: List<Vehicle>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
    onVehicleClick: (Vehicle) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        // === HEADER: title + subtitle + compact badges (semua horizontal 1 baris) ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Katalog Armada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    "Kelola & sewa armada Anda",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactStatBadge(count = vehicles.size, color = MaterialTheme.colorScheme.primary)
                CompactStatBadge(count = vehicles.count { !it.isRented }, color = Color(0xFF10B981))
                CompactStatBadge(count = vehicles.count { it.isRented }, color = Color(0xFFEF4444))
            }
        }

        Spacer(Modifier.height(10.dp))

        // === SEARCH + FILTER (semua horizontal, di dalam 1 container) ===
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
                    placeholder = { Text("Cari armada / plat...", fontSize = 12.sp) },
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
                        .testTag("rental-search")
                )

                // Kategori chips - rata bagi 3 dengan weight
                Text("Tipe", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Semua", "Mobil", "Motor").forEach { cat ->
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

                // Status chips - rata bagi 3 dengan weight
                Text("Status", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Semua", "Tersedia", "Disewa").forEach { status ->
                        val active = selectedStatus == status
                        val badgeColor = when (status) {
                            "Tersedia" -> Color(0xFF10B981)
                            "Disewa" -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) badgeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            contentColor = if (active) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (active) badgeColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onStatusChange(status) }
                        ) {
                            Text(
                                text = status,
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

        if (filteredVehicles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tidak ada armada cocok",
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
                items(filteredVehicles) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onClick = { onVehicleClick(vehicle) },
                        onDelete = { onDeleteVehicle(vehicle.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !vehicle.isRented, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // === Top row: image + name + price ===
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (vehicle.isRented) MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.size(48.dp)
                ) {
                    if (!vehicle.image.isNullOrBlank()) {
                        AsyncImage(
                            model = vehicle.image,
                            contentDescription = vehicle.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = if (vehicle.isRented) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        vehicle.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        color = if (vehicle.isRented) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (vehicle.isRented) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFF1E1E24),
                            border = BorderStroke(0.5.dp, if (vehicle.isRented) Color.Gray.copy(alpha = 0.3f) else Color(0xFFCCCCCC))
                        ) {
                            Text(
                                vehicle.plateNumber,
                                color = if (vehicle.isRented) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 9.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (vehicle.type == "MOBIL") Color(0xFFE3F2FD) else Color(0xFFFFF3E0),
                            border = BorderStroke(0.5.dp, if (vehicle.type == "MOBIL") Color(0xFF90CAF9) else Color(0xFFFFCC80))
                        ) {
                            Text(
                                text = vehicle.type,
                                fontSize = 9.sp,
                                color = if (vehicle.type == "MOBIL") Color(0xFF0D47A1) else Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Formatters.rupiah(vehicle.pricePerDay),
                        fontWeight = FontWeight.Black,
                        color = if (vehicle.isRented) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        "/ hari",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // === Bottom row: margin + status + delete ===
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val profit = vehicle.pricePerDay - vehicle.costPrice
                val marginPercent = if (vehicle.pricePerDay > 0) (profit / vehicle.pricePerDay) * 100 else 0.0
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (marginPercent >= 0) Color(0xFFE2FBE7) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    border = BorderStroke(0.5.dp, if (marginPercent >= 0) Color(0xA01E824C) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Text(
                        "Margin ${String.format("%.0f", marginPercent)}%",
                        fontSize = 10.sp,
                        color = if (marginPercent >= 0) Color(0xFF1E824C) else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (vehicle.isRented) Color(0xFFFDE8E8) else Color(0xFFE2FBE7),
                    border = BorderStroke(0.5.dp, if (vehicle.isRented) Color(0xFFF8B4B4) else Color(0xA01E824C))
                ) {
                    Text(
                        text = if (vehicle.isRented) "Disewa" else "Tersedia",
                        color = if (vehicle.isRented) Color(0xFFC81E1E) else Color(0xFF1E824C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!vehicle.isRented) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Hapus Armada",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Renter info if rented
            if (vehicle.isRented) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Penyewa: ${vehicle.activeRenterName ?: "Umum"}" +
                                (vehicle.rentExpiry?.let { " (s.d. ${Formatters.dateLong(it)})" } ?: ""),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun RentalOrdersPane(
    rentalOrders: List<RentalOrder>,
    showHistory: Boolean,
    onToggleHistory: (Boolean) -> Unit,
    onViewReceipt: (RentalOrder) -> Unit,
    onReturn: (RentalOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
            "Manajemen Sewa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))

        // Segmented control: Aktif / Riwayat
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!showHistory) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onToggleHistory(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sewa Aktif",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!showHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showHistory) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onToggleHistory(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Riwayat Selesai",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Clear, null, modifier = Modifier.size(16.dp))
                    }
                }
            },
            placeholder = { Text("Cari penyewa, armada, struk...", fontSize = 11.sp) },
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
                .height(44.dp)
        )

        Spacer(Modifier.height(10.dp))

        val filteredOrders = remember(rentalOrders, showHistory, searchQuery) {
            rentalOrders.filter {
                val matchesStatus = if (showHistory) it.status == "RETURNED" else it.status == "ACTIVE"
                val matchesSearch = it.customerName.contains(searchQuery, ignoreCase = true) ||
                        it.vehicleName.contains(searchQuery, ignoreCase = true) ||
                        it.id.contains(searchQuery, ignoreCase = true)
                matchesStatus && matchesSearch
            }
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.CarRental,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (showHistory) "Belum ada riwayat sewa selesai."
                        else "Belum ada sewa aktif.\nPilih armada di tab Armada untuk disewakan.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders) { order ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = order.vehicleName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onViewReceipt(order) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Receipt,
                                            contentDescription = "Lihat Nota",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = order.customerName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "WhatsApp: ${order.whatsapp}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Mulai Sewa: ${Formatters.dateLong(order.rentDate)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(6.dp))
                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            Spacer(Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "Durasi",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${order.days} Hari",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.rupiah(order.total),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (order.status == "ACTIVE") {
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { onReturn(order) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text("Kembalikan Armada", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE2FBE7),
                                    border = BorderStroke(0.5.dp, Color(0xA01E824C)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Armada Sudah Dikembalikan",
                                        color = Color(0xFF1E824C),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
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
private fun TabPill(
    label: String,
    icon: ImageVector,
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

@Composable
private fun CompactStatBadge(count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Text(
                text = "$count",
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
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
