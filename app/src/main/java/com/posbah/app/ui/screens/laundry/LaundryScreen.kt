package com.posbah.app.ui.screens.laundry

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.draw.clip
import androidx.compose.material3.OutlinedButton

import androidx.compose.material.icons.outlined.PhotoCamera
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.posbah.app.util.CameraUtils

data class LaundryServiceItem(
    val id: String,
    val name: String,
    val category: String, // KILOAN | SATUAN
    val price: Double,
    val costPrice: Double = 0.0,
    val image: String? = null,
    val unit: String // Kg | Pcs
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaundryScreen(
    onBack: () -> Unit,
    viewModel: LaundryViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val services by viewModel.services.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val cart = viewModel.cart

    val isOwner by viewModel.isOwner.collectAsState()
    val activityLogsList by viewModel.activityLogs.collectAsState()
    var showLogsDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var showActiveOrders by remember { mutableStateOf(false) }
    var activeReceiptOrder by remember { mutableStateOf<LaundryOrder?>(null) }
    var showReceiptDetails by remember { mutableStateOf<List<CartItem>?>(null) }

    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceCategory by remember { mutableStateOf("KILOAN") }
    var newServicePrice by remember { mutableStateOf("") }
    var newServiceCost by remember { mutableStateOf("") }
    var newServiceUnit by remember { mutableStateOf("Kg") }
    var rentDateMillis by remember { mutableStateOf<Long?>(null) }

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

    val filteredServices = remember(services, searchQuery) {
        services.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val cartSubtotal = remember(cart.toList()) {
        cart.sumOf { it.service.price * it.quantity }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Laundry POS",
                subtitle = "Sistem Kasir Jasa Laundry",
                onBack = onBack,
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showLogsDialog = true }) {
                            Icon(Icons.Outlined.Notes, contentDescription = "Log Aktivitas")
                        }
                    }
                    IconButton(onClick = {
                        newServiceName = ""
                        newServiceCategory = "KILOAN"
                        newServicePrice = ""
                        newServiceCost = ""
                        newServiceUnit = "Kg"
                        capturedPhotoFile = null
                        showAddServiceDialog = true
                    }) {
                        Icon(Icons.Outlined.LocalLaundryService, contentDescription = "Tambah Layanan")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showActiveOrders = true }) {
                        Text("Daftar Order (${orders.filter { it.orderStatus != "DIAMBIL" }.size})")
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Left pane: Laundry Services Catalog
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Text(
                    "Layanan Laundry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Cari layanan...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("laundry-search")
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredServices) { service ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val existing = cart.firstOrNull { it.service.id == service.id }
                                    if (existing != null) {
                                        val step = if (service.unit == "Kg") 0.5 else 1.0
                                        val idx = cart.indexOf(existing)
                                        cart[idx] = existing.copy(quantity = existing.quantity + step)
                                    } else {
                                        cart.add(CartItem(service, 1.0))
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(42.dp)
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
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        service.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Kategori: ${service.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val marginPercent = if (service.price > 0) ((service.price - service.costPrice) / service.price) * 100 else 0.0
                                        Text(
                                            "• Margin: ${String.format("%.0f", marginPercent)}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (marginPercent >= 0) Color(0xFF1F8B4C) else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            Formatters.rupiah(service.price),
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "per ${service.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteService(service.id) {
                                                Toast.makeText(context, "Layanan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.RemoveCircleOutline,
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

            // Right pane: Laundry Shopping Cart Sidebar
            Surface(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight(),
                tonalElevation = 1.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Keranjang Order",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (cart.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Keranjang kosong.\nPilih layanan di kiri.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(cart) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.service.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                "${Formatters.rupiah(item.service.price)} / ${item.service.unit}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Outlined.RemoveCircleOutline, null, modifier = Modifier.size(16.dp))
                                            }
                                            Text(
                                                text = if (item.service.unit == "Kg") "%.1f".format(item.quantity) else item.quantity.toInt().toString(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(item.service.unit, fontSize = 10.sp)
                                            TextButton(
                                                onClick = {
                                                    val step = if (item.service.unit == "Kg") 0.5 else 1.0
                                                    val idx = cart.indexOf(item)
                                                    cart[idx] = item.copy(quantity = item.quantity + step)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nama Pelanggan") },
                        leadingIcon = { Icon(Icons.Outlined.People, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("laundry-cust-name")
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp Pelanggan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("laundry-phone")
                    )
                    Spacer(Modifier.height(10.dp))

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
                        Text("Tanggal Order:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { datePickerDialog.show() }) {
                            Text(dateText, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text(Formatters.rupiah(cartSubtotal), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (customerName.isBlank() || phone.isBlank()) {
                                Toast.makeText(context, "Mohon lengkapi Nama & Phone Pelanggan!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (cart.isEmpty()) {
                                Toast.makeText(context, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
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
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().testTag("btn-confirm-laundry")
                    ) {
                        Text("Buat Order Laundry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog 1: Active/History Laundry Orders Manager
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

                    val filteredOrders = remember(orders, viewHistoryOrders) {
                        orders.filter { if (viewHistoryOrders) it.orderStatus == "DIAMBIL" else it.orderStatus != "DIAMBIL" }
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
                                            // Payment status toggle
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

                                            // Order status cycler
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

    // Dialog 2: Laundry Receipt Mockup Dialog
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
                        Toast.makeText(context, "Mencetak ke printer Bluetooth thermal...", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Cetak Nota") }
            }
        )
    }

    // Dialog: Tambah Layanan Baru
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

                    // Real-time margin calculator
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
                                Icon(Icons.Outlined.Clear, contentDescription = "Hapus Foto", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = launchCamera,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ambil Foto Layanan")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = newServicePrice.toDoubleOrNull() ?: 0.0
                        val cost = newServiceCost.toDoubleOrNull() ?: 0.0
                        if (newServiceName.isNotBlank() && rate > 0) {
                            viewModel.addService(newServiceName, rate, cost, newServiceCategory, newServiceUnit, capturedPhotoFile) {
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

    // Dialog: Log Aktivitas Laundry (Owner Only)
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
}
