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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletControlScreen(
    onBack: () -> Unit,
    viewModel: OutletControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddOutletDialog by remember { mutableStateOf(false) }
    var newOutletName by remember { mutableStateOf("") }
    var newOutletAddress by remember { mutableStateOf("") }
    var newOutletPhone by remember { mutableStateOf("") }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
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
                            newOutletName = ""
                            newOutletAddress = ""
                            newOutletPhone = ""
                            showAddOutletDialog = true
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
                        onAssignEmployee = { empName -> viewModel.assignEmployee(summary.outlet.id, empName) }
                    )
                }

                // Empty / Placeholder Slot for creation
                if (state.outlets.size < 3) {
                    item {
                        Card(
                            onClick = {
                                newOutletName = ""
                                newOutletAddress = ""
                                newOutletPhone = ""
                                showAddOutletDialog = true
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

    // Add Outlet Dialog
    if (showAddOutletDialog) {
        AlertDialog(
            onDismissRequest = { showAddOutletDialog = false },
            title = { Text("Tambah Outlet Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newOutletName,
                        onValueChange = { newOutletName = it },
                        label = { Text("Nama Outlet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newOutletAddress,
                        onValueChange = { newOutletAddress = it },
                        label = { Text("Alamat (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newOutletPhone,
                        onValueChange = { newOutletPhone = it },
                        label = { Text("Nomor Telepon (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createOutlet(newOutletName, newOutletAddress, newOutletPhone)
                        showAddOutletDialog = false
                    }
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOutletDialog = false }) {
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
    onAssignEmployee: (String) -> Unit
) {
    var employeeMenuExpanded by remember { mutableStateOf(false) }

    Card(
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
