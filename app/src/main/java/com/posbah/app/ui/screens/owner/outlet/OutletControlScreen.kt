package com.posbah.app.ui.screens.owner.outlet

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.Employee
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import com.posbah.app.data.local.entities.Outlet
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletControlScreen(
    onBack: () -> Unit,
    viewModel: OutletControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showOutletDialog by remember { mutableStateOf(false) }
    var editingOutlet by remember { mutableStateOf<Outlet?>(null) }
    var outletName by remember { mutableStateOf("") }
    var outletAddress by remember { mutableStateOf("") }
    var outletPhone by remember { mutableStateOf("") }
    var outletEmployee by remember { mutableStateOf("") }
    var showEmployeeListDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var outletToDelete by remember { mutableStateOf<Outlet?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            PosBahTopBar(
                title = "Kontrol Multi-Outlet",
                subtitle = "Analisis Margin & Status Toko",
                onBack = onBack,
                actions = {
                    if (state.outlets.size < 3) {
                        IconButton(onClick = {
                            editingOutlet = null
                            outletName = ""
                            outletAddress = ""
                            outletPhone = ""
                            outletEmployee = ""
                            showOutletDialog = true
                        }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Tambah Outlet")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Title
                item {
                    Text(
                        text = "Manajemen Outlet (Maks 3)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // List Outlets (Grid-like cards)
                items(state.outlets) { summary ->
                    OutletCard(
                        summary = summary,
                        allEmployees = state.employees,
                        onToggleStatus = { viewModel.toggleOutletStatus(summary.outlet.id) },
                        onAssignEmployee = { empName -> viewModel.assignEmployee(summary.outlet.id, empName) },
                        onEditClick = {
                            editingOutlet = summary.outlet
                            outletName = summary.outlet.name
                            outletAddress = summary.outlet.address.orEmpty()
                            outletPhone = summary.outlet.phone.orEmpty()
                            outletEmployee = summary.activeEmployeeName.takeIf { it != "-" }.orEmpty()
                            showOutletDialog = true
                        },
                        onDeleteClick = {
                            outletToDelete = summary.outlet
                            showDeleteConfirmDialog = true
                        }
                    )
                }

                // Empty / Placeholder Slot for creation
                if (state.outlets.size < 3) {
                    item {
                        Card(
                            onClick = {
                                editingOutlet = null
                                outletName = ""
                                outletAddress = ""
                                outletPhone = ""
                                outletEmployee = ""
                                showOutletDialog = true
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Tambah Outlet Baru (${state.outlets.size}/3)",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Margin Charts Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grafik Margin Keuntungan (7 Hari Terakhir)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                state.outlets.forEachIndexed { index, summary ->
                                    val color = getOutletColor(index)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(color, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(summary.outlet.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Line Chart
                            if (state.marginHistory.isNotEmpty()) {
                                MarginComparisonChart(
                                    history = state.marginHistory,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Belum ada data transaksi untuk dirender.", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Outlet Dialog
    if (showOutletDialog) {
        var employeeDropdownExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showOutletDialog = false },
            title = { Text(if (editingOutlet == null) "Tambah Outlet Baru" else "Edit Detail Outlet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Warning banner if no employees
                    if (state.employees.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tambahkan karyawan terlebih dahulu (bisa admin, bisa kasir)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = outletName,
                        onValueChange = { outletName = it },
                        label = { Text("Nama Outlet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = outletAddress,
                        onValueChange = { outletAddress = it },
                        label = { Text("Lokasi Outlet (Alamat)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = outletPhone,
                        onValueChange = { outletPhone = it },
                        label = { Text("Nomor Telepon (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Employee assignment selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (outletEmployee.isBlank()) "- Kosong -" else outletEmployee,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Karyawan Outlet") },
                            trailingIcon = {
                                IconButton(onClick = { employeeDropdownExpanded = true }) {
                                    Text("▾", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { employeeDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = employeeDropdownExpanded,
                            onDismissRequest = { employeeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("- Kosong -") },
                                onClick = {
                                    outletEmployee = ""
                                    employeeDropdownExpanded = false
                                }
                            )
                            state.employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text(emp.name) },
                                    onClick = {
                                        outletEmployee = emp.name
                                        employeeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Button to view registered employees list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showEmployeeListDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("List Karyawan")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (outletName.isBlank()) {
                            Toast.makeText(context, "Nama outlet tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editingOutlet == null && outletEmployee.isBlank()) {
                            Toast.makeText(context, "Silakan pilih karyawan untuk outlet baru.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val currentEditing = editingOutlet
                        if (currentEditing == null) {
                            viewModel.createOutlet(outletName, outletAddress, outletPhone, outletEmployee)
                        } else {
                            viewModel.updateOutlet(currentEditing.id, outletName, outletAddress, outletPhone, outletEmployee)
                        }
                        showOutletDialog = false
                    }
                ) {
                    Text(if (editingOutlet == null) "Tambah" else "Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOutletDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // List of Employees sub-dialog
    if (showEmployeeListDialog) {
        AlertDialog(
            onDismissRequest = { showEmployeeListDialog = false },
            title = { Text("Daftar Karyawan") },
            text = {
                if (state.employees.isEmpty()) {
                    Text("Belum ada karyawan terdaftar.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(state.employees) { emp ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text(emp.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (emp.role == "ADMIN") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (emp.role == "ADMIN") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Text(
                                            text = emp.role,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmployeeListDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showDeleteConfirmDialog && outletToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Outlet") },
            text = {
                Text("Apakah Anda yakin ingin menghapus outlet ${outletToDelete?.name}? Karyawan yang ditugaskan ke outlet ini akan dipindahkan ke penempatan 'Seluruh Outlet'.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        outletToDelete?.let { viewModel.deleteOutlet(it.id) }
                        showDeleteConfirmDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun OutletCard(
    summary: OutletSummary,
    allEmployees: List<Employee>,
    onToggleStatus: () -> Unit,
    onAssignEmployee: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var employeeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onEditClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name and status indicator badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.outlet.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (summary.outlet.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                "Utama",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Detail",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Hapus Outlet",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Buka/Tutup Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (summary.outlet.isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    contentColor = if (summary.outlet.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.clickable { onToggleStatus() }
                ) {
                    Text(
                        text = if (summary.outlet.isOpen) "BUKA" else "TUTUP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subdetails: Alamat & Phone
            if (!summary.outlet.address.isNullOrBlank()) {
                Text(
                    summary.outlet.address,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Interactive Row details (Stok, Karyawan, Margin)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stock Info
                Column {
                    Text("Total Stok", fontSize = 11.sp, color = Color.Gray)
                    Text("${summary.totalStock} pcs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Active Employee Selector
                Column {
                    Text("Karyawan Aktif", fontSize = 11.sp, color = Color.Gray)
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { employeeMenuExpanded = true }
                        ) {
                            Text(
                                summary.activeEmployeeName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("▾", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        DropdownMenu(
                            expanded = employeeMenuExpanded,
                            onDismissRequest = { employeeMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("- Kosong -") },
                                onClick = {
                                    onAssignEmployee("")
                                    employeeMenuExpanded = false
                                }
                            )
                            allEmployees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text(emp.name) },
                                    onClick = {
                                        onAssignEmployee(emp.name)
                                        employeeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Margin Info
                Column(horizontalAlignment = Alignment.End) {
                    Text("Margin Penjualan", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        Formatters.rupiah(summary.totalMargin),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Quick toggle button for opening/closing the outlet
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onToggleStatus,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (summary.outlet.isOpen) "Tutup Toko Sementara" else "Buka Toko Sekarang",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MarginComparisonChart(
    history: List<MarginDataPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(history) {
        val highest = history.maxOf { 
            maxOf(it.marginA, it.marginB, it.marginC, 1000.0) 
        }
        highest * 1.15 // pad 15% top
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 36f

        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        val stepX = chartWidth / (history.size - 1).coerceAtLeast(1)

        // Draw horizontal grid lines (Y-axis grid)
        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = padding + (chartHeight * (1 - ratio))
            
            // Draw grid line
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )

            // Draw Y-axis Label
            val gridVal = maxVal * ratio
            drawContext.canvas.nativeCanvas.drawText(
                Formatters.rupiah(gridVal).substringBefore(","),
                5f,
                y + 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.LEFT
                }
            )
        }

        // Helper paths for drawing
        val pathA = Path()
        val pathB = Path()
        val pathC = Path()

        history.forEachIndexed { index, dp ->
            val x = padding + (index * stepX)
            
            val yA = padding + (chartHeight * (1f - (dp.marginA / maxVal).toFloat()))
            val yB = padding + (chartHeight * (1f - (dp.marginB / maxVal).toFloat()))
            val yC = padding + (chartHeight * (1f - (dp.marginC / maxVal).toFloat()))

            if (index == 0) {
                pathA.moveTo(x, yA)
                pathB.moveTo(x, yB)
                pathC.moveTo(x, yC)
            } else {
                pathA.lineTo(x, yA)
                pathB.lineTo(x, yB)
                pathC.lineTo(x, yC)
            }

            // Draw X-axis label
            drawContext.canvas.nativeCanvas.drawText(
                dp.dateStr,
                x,
                height - 8f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        // Draw Lines
        drawPath(
            path = pathA,
            color = Color(0xFF2196F3), // Blue
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
        drawPath(
            path = pathB,
            color = Color(0xFF4CAF50), // Green
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
        drawPath(
            path = pathC,
            color = Color(0xFFFF9800), // Orange
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // Draw Data Points
        history.forEachIndexed { index, dp ->
            val x = padding + (index * stepX)
            
            val yA = padding + (chartHeight * (1f - (dp.marginA / maxVal).toFloat()))
            val yB = padding + (chartHeight * (1f - (dp.marginB / maxVal).toFloat()))
            val yC = padding + (chartHeight * (1f - (dp.marginC / maxVal).toFloat()))

            drawCircle(color = Color(0xFF2196F3), radius = 8f, center = Offset(x, yA))
            drawCircle(color = Color(0xFF4CAF50), radius = 8f, center = Offset(x, yB))
            drawCircle(color = Color(0xFFFF9800), radius = 8f, center = Offset(x, yC))
        }
    }
}

fun getOutletColor(index: Int): Color {
    return when (index) {
        0 -> Color(0xFF2196F3) // Blue
        1 -> Color(0xFF4CAF50) // Green
        2 -> Color(0xFFFF9800) // Orange
        else -> Color.Gray
    }
}
