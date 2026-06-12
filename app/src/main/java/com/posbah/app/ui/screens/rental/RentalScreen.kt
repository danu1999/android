package com.posbah.app.ui.screens.rental

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.History
import com.posbah.app.ui.navigation.Screen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.PrimaryButton
import com.posbah.app.util.Formatters
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.outlined.Close

import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.PhotoCamera
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.posbah.app.util.CameraUtils

import androidx.compose.material.icons.outlined.Print

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
    val isOwner by viewModel.isOwner.collectAsState()
    val activityLogsList by viewModel.activityLogs.collectAsState()
    val customerList by viewModel.customers.collectAsState(emptyList())
    val transactionList by viewModel.transactions.collectAsState(emptyList())

    val outletList by viewModel.availableOutlets.collectAsState()
    val activeOutletId by viewModel.activeOutletId.collectAsState()
    val activeOutlet = remember(outletList, activeOutletId) {
        outletList.find { it.id == activeOutletId }
    }
    val activeOutletName = activeOutlet?.name ?: "Outlet Utama"
    var showOutletDropdown by remember { mutableStateOf(false) }
    var showOwnerMenuDropdown by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
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

    val filteredVehicles = remember(vehicles, searchQuery) {
        vehicles.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.plateNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box {
                PosBahTopBar(
                    title = "Rental POS",
                    subtitle = "Outlet: $activeOutletName",
                    onBack = onBack,
                    onTitleClick = { showOutletDropdown = true },
                    actions = {
                        IconButton(onClick = { onNavigate(Screen.MarginAnalysis.route) }) {
                            Icon(Icons.Outlined.History, contentDescription = "Analisis Margin & Riwayat")
                        }
                        IconButton(onClick = onNavigateToPrintSettings) {
                            Icon(Icons.Outlined.Print, contentDescription = "Pengaturan Struk")
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
        Row(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Left pane: Vehicles Catalog
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Text(
                    "Katalog Armada Kendaraan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, null)
                            }
                        }
                    },
                    placeholder = { Text("Cari armada atau plat nomor...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("rental-search")
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredVehicles) { vehicle ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            border = BorderStroke(1.dp, if (vehicle.isRented) MaterialTheme.colorScheme.outline.copy(alpha = 0.04f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !vehicle.isRented) {
                                    selectedVehicleForRent = vehicle
                                    customerName = ""
                                    rentDays = "1"
                                    whatsapp = ""
                                    cashPaid = vehicle.pricePerDay.toLong().toString()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (vehicle.isRented) MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(52.dp)
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
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        vehicle.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (vehicle.isRented) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Plate Number Tag
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (vehicle.isRented) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFF1E1E24),
                                            border = BorderStroke(0.5.dp, if (vehicle.isRented) Color.Gray.copy(alpha = 0.3f) else Color(0xFFCCCCCC))
                                        ) {
                                            Text(
                                                vehicle.plateNumber,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (vehicle.isRented) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        
                                        // Vehicle Type Tag
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (vehicle.type == "MOBIL") Color(0xFFE3F2FD) else Color(0xFFFFF3E0),
                                            border = BorderStroke(0.5.dp, if (vehicle.type == "MOBIL") Color(0xFF90CAF9) else Color(0xFFFFCC80))
                                        ) {
                                            Text(
                                                text = if (vehicle.type == "MOBIL") "MOBIL 🚗" else "MOTOR 🏍️",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (vehicle.type == "MOBIL") Color(0xFF0D47A1) else Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
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
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        Formatters.rupiah(vehicle.pricePerDay),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (vehicle.isRented) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                                    )
                                    
                                    val profit = vehicle.pricePerDay - vehicle.costPrice
                                    val marginPercent = if (vehicle.pricePerDay > 0) (profit / vehicle.pricePerDay) * 100 else 0.0
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        // Margin Pill
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (marginPercent >= 0) Color(0xFFE2FBE7) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                            border = BorderStroke(0.5.dp, if (marginPercent >= 0) Color(0xA01E824C) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                        ) {
                                            Text(
                                                "Margin: ${String.format("%.0f", marginPercent)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (marginPercent >= 0) Color(0xFF1E824C) else MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        // Status Pill
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (vehicle.isRented) Color(0xFFFDE8E8) else Color(0xFFE2FBE7),
                                            border = BorderStroke(0.5.dp, if (vehicle.isRented) Color(0xFFF8B4B4) else Color(0xA01E824C))
                                        ) {
                                            Text(
                                                text = if (vehicle.isRented) "Disewa" else "Tersedia",
                                                color = if (vehicle.isRented) Color(0xFFC81E1E) else Color(0xFF1E824C),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        
                                        if (!vehicle.isRented) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteVehicle(vehicle.id) {
                                                        Toast.makeText(context, "Armada berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(50)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Hapus Armada",
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
            }

            // Right pane: Rental Active / Completed Orders
            Surface(
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight(),
                tonalElevation = 1.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var showHistory by remember { mutableStateOf(false) }

                    // Premium Sliding-Tab (Segmented Control) Design
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
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
                                    .clickable { showHistory = false },
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
                                    .clickable { showHistory = true },
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

                    Spacer(Modifier.height(12.dp))

                    val filteredOrders = remember(rentalOrders, showHistory) {
                        rentalOrders.filter { if (showHistory) it.status == "RETURNED" else it.status == "ACTIVE" }
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
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    if (showHistory) "Belum ada riwayat sewa selesai."
                                    else "Belum ada sewa aktif.\nPilih kendaraan untuk disewakan.",
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
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = order.vehicleName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(
                                                onClick = { activeReceiptOrder = order },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(50)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Receipt,
                                                        contentDescription = "Lihat Nota",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.People,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = order.customerName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "WhatsApp: ${order.whatsapp}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Mulai Sewa: ${Formatters.dateLong(order.rentDate)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Spacer(Modifier.height(8.dp))
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                        Spacer(Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Durasi Sewa",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${order.days} Hari",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Total Biaya",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = Formatters.rupiah(order.total),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        if (order.status == "ACTIVE") {
                                            Spacer(Modifier.height(12.dp))
                                            Button(
                                                onClick = { selectedOrderForReturn = order; lateDaysInput = "0" },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF10B981),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(vertical = 8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(36.dp)
                                            ) {
                                                Text("Kembalikan Armada", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Spacer(Modifier.height(12.dp))
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
        }
    }

    // Dialog 1: Lease Confirmation Dialog
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
                    // Segment 1: Customer Profile Container
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
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text("Umum (Walk-in)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        showCustomerSelectionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isUmum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (!isUmum) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
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

                    // Segment 2: Rental Settings Container
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
                                    label = { Text("Uang Muka / Cash (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.5f).testTag("rental-cash")
                                )
                            }

                            // Date selection (Backdate)
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
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                sdf.format(Date(rentDateMillis!!))
                            } else {
                                "Hari Ini (Real-time)"
                            }

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

    // Dialog 2: Return Vehicle Dialog
    if (selectedOrderForReturn != null) {
        val order = selectedOrderForReturn!!
        val price = order.total / order.days
        val lateDays = lateDaysInput.toIntOrNull() ?: 0
        val lateFee = lateDays * price * 1.5 // Denda 150% tarif per hari

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

    // Dialog 3: Rental Receipt Dialog
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
                        Toast.makeText(context, "Mencetak ke printer Bluetooth thermal...", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Cetak Struk") }
            }
        )
    }

    // Dialog: Tambah Armada Baru
    if (showAddVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Tambah Armada Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newVehicleName,
                        onValueChange = { nameVal -> newVehicleName = nameVal },
                        label = { Text("Nama Kendaraan") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add-vehicle-name")
                    )
                    OutlinedTextField(
                        value = newVehiclePlate,
                        onValueChange = { plateVal -> newVehiclePlate = plateVal },
                        label = { Text("Nomor Plat Kendaraan") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add-vehicle-plate")
                    )
                    OutlinedTextField(
                        value = newVehiclePrice,
                        onValueChange = { priceVal -> newVehiclePrice = priceVal },
                        label = { Text("Tarif Jual per Hari (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add-vehicle-price")
                    )
                    OutlinedTextField(
                        value = newVehicleCost,
                        onValueChange = { costVal -> newVehicleCost = costVal },
                        label = { Text("Biaya Modal/Beli Kendaraan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVehicleMonthlyMaintenance,
                        onValueChange = { mmVal -> newVehicleMonthlyMaintenance = mmVal },
                        label = { Text("Biaya Perawatan Bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Real-time margin calculator
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
                    
                    // Simple MOBIL/MOTOR toggle selector
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

                    // Camera section
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

    // Dialog: Log Aktivitas Rental (Owner Only)
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

    // Dialog: Pilih / Tambah Pelanggan
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
