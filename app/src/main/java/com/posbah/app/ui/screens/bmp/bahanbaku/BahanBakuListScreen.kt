package com.posbah.app.ui.screens.bmp.bahanbaku

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.BmpBahanBakuEntity
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.StatChip
import com.posbah.app.util.Formatters
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun BahanBakuListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: BahanBakuListViewModel = hiltViewModel()
) {
    val list by viewModel.filteredList.collectAsState()
    val totalHarga by viewModel.totalHarga.collectAsState()
    val totalNominal by viewModel.totalNominal.collectAsState()

    val filteredTotalHarga = remember(list) { list.sumOf { it.totalHarga } }
    val filteredTotalNominal = remember(list) { list.sumOf { it.nominal } }

    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }
    var payDebtTarget by remember { mutableStateOf<BmpBahanBakuEntity?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var showCalendarDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Bahan Baku",
                subtitle = "${list.size} transaksi",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showCalendarDialog = true },
                        modifier = Modifier.testTag("btn-calendar-trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = "Kalender"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-add-bahanbaku")
            ) { Icon(Icons.Outlined.Add, contentDescription = "Tambah") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary stats
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Total Nilai Bahan",
                            value = Formatters.rupiah(filteredTotalHarga),
                            accent = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        StatChip(
                            label = "Total Dibayar",
                            value = Formatters.rupiah(filteredTotalNominal),
                            accent = Color(0xFFEF4444)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                StatChip(
                    label = "Sisa Hutang Supplier",
                    value = Formatters.rupiah(filteredTotalHarga - filteredTotalNominal),
                    accent = if (filteredTotalHarga - filteredTotalNominal > 0)
                        MaterialTheme.colorScheme.error
                    else Color(0xFF22C57E)
                )
            }

            if (list.isEmpty()) {
                item {
                    Spacer(Modifier.height(40.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Belum ada transaksi bahan baku",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tekan + untuk mencatat pembelian bahan baru",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "TRANSAKSI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(list, key = { it.id }) { entry ->
                    BahanBakuCard(
                        entry = entry,
                        onEdit = { onEdit(entry.id) },
                        onDelete = { deleteTarget = entry.id },
                        onPreviewPhoto = { url -> previewPhotoUrl = url },
                        onPayDebt = {
                            payDebtTarget = entry
                            payAmount = ""
                        }
                    )
                }
            }
        }
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus Transaksi") },
            text = { Text("Yakin ingin menghapus transaksi ini? Entri kas terkait juga akan dihapus.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(id)
                        deleteTarget = null
                    },
                    modifier = Modifier.testTag("btn-confirm-delete-bb")
                ) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            }
        )
    }

    previewPhotoUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { previewPhotoUrl = null },
            title = { Text("Bukti Nota / Surat Jalan") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Preview Nota",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewPhotoUrl = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    payDebtTarget?.let { entry ->
        val sisaHutang = entry.totalHarga - entry.nominal
        AlertDialog(
            onDismissRequest = { payDebtTarget = null },
            title = { Text("Bayar Hutang Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hutang tersisa: ${Formatters.rupiah(sisaHutang)}")
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Jumlah Bayar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pay-debt-amount")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = payAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            if (amt > sisaHutang) {
                                android.widget.Toast.makeText(context, "Jumlah bayar melebihi sisa hutang!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.payDebt(entry.id, amt)
                                payDebtTarget = null
                                payAmount = ""
                                android.widget.Toast.makeText(context, "Berhasil membayar hutang supplier!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Masukkan jumlah bayar yang valid!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn-confirm-pay-debt")
                ) { Text("Bayar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { payDebtTarget = null; payAmount = "" }
                ) { Text("Batal") }
            }
        )
    }

    if (showCalendarDialog) {
        BahanBakuCalendarDialog(
            viewModel = viewModel,
            onDismiss = { showCalendarDialog = false }
        )
    }
}

@Composable
private fun BahanBakuCard(
    entry: BmpBahanBakuEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPreviewPhoto: (String) -> Unit,
    onPayDebt: () -> Unit
) {
    val sisaHutang = entry.totalHarga - entry.nominal
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().testTag("bb-card-${entry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.noTagihan,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        Formatters.dateLong(entry.tanggal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.testTag("btn-edit-bb-${entry.id}")) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("btn-delete-bb-${entry.id}")) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountChip(
                            label = "Total Bahan",
                            value = Formatters.rupiah(entry.totalHarga),
                            color = MaterialTheme.colorScheme.primary
                        )
                        AmountChip(
                            label = "Dibayar",
                            value = Formatters.rupiah(entry.nominal),
                            color = Color(0xFF22C57E)
                        )
                        if (sisaHutang > 0) {
                            AmountChip(
                                label = "Hutang",
                                value = Formatters.rupiah(sisaHutang),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    entry.notes?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (sisaHutang > 0) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onPayDebt,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("btn-pay-debt-${entry.id}")
                        ) {
                            Text("Bayar Hutang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val photo = entry.notaFotoPath?.takeIf { path ->
                    if (path.startsWith("content://")) true else java.io.File(path).exists()
                } ?: entry.notaFotoUrl
                photo?.takeIf { it.isNotBlank() }?.let { nonNullPhoto ->
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = nonNullPhoto,
                        contentDescription = "Nota",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onPreviewPhoto(nonNullPhoto) }
                            .testTag("bb-photo-thumbnail-${entry.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}

@Composable
fun BahanBakuCalendarDialog(
    viewModel: BahanBakuListViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val rawList by viewModel.list.collectAsState()
    val filterStartDate by viewModel.filterStartDate.collectAsState()
    val filterEndDate by viewModel.filterEndDate.collectAsState()
    val filterHutang by viewModel.filterHutang.collectAsState()
    val filterDibayar by viewModel.filterDibayar.collectAsState()

    var displayMonth by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) }
    var displayYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val calendar = remember(displayMonth, displayYear) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, displayYear)
            set(java.util.Calendar.MONTH, displayMonth)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
    }

    val firstDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val maxDays = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val emptySlotsBefore = firstDayOfWeek - 1

    val calendarList = remember(rawList, filterHutang, filterDibayar) {
        var result = rawList
        if (filterHutang && !filterDibayar) {
            result = result.filter { it.totalHarga - it.nominal > 0 }
        }
        if (filterDibayar && !filterHutang) {
            result = result.filter { it.totalHarga - it.nominal <= 0 }
        }
        result
    }

    val transactionsByDay = remember(calendarList, displayMonth, displayYear) {
        val map = mutableMapOf<Int, List<BmpBahanBakuEntity>>()
        val cal = java.util.Calendar.getInstance()
        for (t in calendarList) {
            cal.timeInMillis = t.tanggal
            if (cal.get(java.util.Calendar.MONTH) == displayMonth &&
                cal.get(java.util.Calendar.YEAR) == displayYear
            ) {
                val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                map[day] = (map[day] ?: emptyList()) + t
            }
        }
        map
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kalender Transaksi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (displayMonth == 0) {
                            displayMonth = 11
                            displayYear -= 1
                        } else {
                            displayMonth -= 1
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Bulan Sebelumnya"
                        )
                    }

                    Text(
                        text = "${monthNames[displayMonth]} $displayYear",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = {
                        if (displayMonth == 11) {
                            displayMonth = 0
                            displayYear += 1
                        } else {
                            displayMonth += 1
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Bulan Selanjutnya"
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    val weekdays = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
                    weekdays.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                val daysList = mutableListOf<Int?>()
                for (i in 0 until emptySlotsBefore) {
                    daysList.add(null)
                }
                for (d in 1..maxDays) {
                    daysList.add(d)
                }
                val chunkedWeeks = daysList.chunked(7)

                Column(modifier = Modifier.fillMaxWidth()) {
                    chunkedWeeks.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            week.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day != null) {
                                        val calCell = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.YEAR, displayYear)
                                            set(java.util.Calendar.MONTH, displayMonth)
                                            set(java.util.Calendar.DAY_OF_MONTH, day)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        val dayStartVal = calCell.timeInMillis
                                        val isInRange = when {
                                            filterStartDate != null && filterEndDate != null -> {
                                                dayStartVal in filterStartDate!!..filterEndDate!!
                                            }
                                            filterStartDate != null -> {
                                                dayStartVal == filterStartDate!!
                                            }
                                            filterEndDate != null -> {
                                                dayStartVal == filterEndDate!!
                                            }
                                            else -> false
                                        }
                                        val dayTx = transactionsByDay[day] ?: emptyList()
                                        CalendarDayCell(
                                            day = day,
                                            dayStart = dayStartVal,
                                            isInRange = isInRange,
                                            transactions = dayTx,
                                            onClick = {
                                                val start = filterStartDate
                                                val end = filterEndDate
                                                when {
                                                    start == null -> {
                                                        viewModel.setDateRange(dayStartVal, null)
                                                    }
                                                    end == null -> {
                                                        if (dayStartVal >= start) {
                                                            viewModel.setDateRange(start, dayStartVal)
                                                        } else {
                                                            viewModel.setDateRange(dayStartVal, start)
                                                        }
                                                    }
                                                    else -> {
                                                        viewModel.setDateRange(dayStartVal, null)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            if (week.size < 7) {
                                repeat(7 - week.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FILTER TRANSAKSI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val startText = if (filterStartDate != null) {
                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(filterStartDate!!))
                        } else {
                            "Mulai"
                        }
                        val endText = if (filterEndDate != null) {
                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(filterEndDate!!))
                        } else {
                            "Selesai"
                        }

                        Surface(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                if (filterStartDate != null) {
                                    cal.timeInMillis = filterStartDate!!
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = java.util.Calendar.getInstance()
                                        selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                        selectedCal.set(java.util.Calendar.MILLISECOND, 0)
                                        viewModel.setDateRange(selectedCal.timeInMillis, filterEndDate)
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(startText, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                            }
                        }

                        Text("s/d", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Surface(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                if (filterEndDate != null) {
                                    cal.timeInMillis = filterEndDate!!
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = java.util.Calendar.getInstance()
                                        selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                        selectedCal.set(java.util.Calendar.MILLISECOND, 0)
                                        viewModel.setDateRange(filterStartDate, selectedCal.timeInMillis)
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(endText, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (filterStartDate != null || filterEndDate != null) {
                            IconButton(
                                onClick = { viewModel.setDateRange(null, null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear Range",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomFilterChip(
                                label = "Hutang",
                                selected = filterHutang,
                                selectedColor = Color(0xFFEF4444),
                                onClick = { viewModel.toggleFilterHutang(!filterHutang) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomFilterChip(
                                label = "Dibayar",
                                selected = filterDibayar,
                                selectedColor = Color(0xFF22C57E),
                                onClick = { viewModel.toggleFilterDibayar(!filterDibayar) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CalendarDayCell(
    day: Int,
    dayStart: Long,
    isInRange: Boolean,
    transactions: List<BmpBahanBakuEntity>,
    onClick: () -> Unit
) {
    val hasTx = transactions.isNotEmpty()
    val sisaHutang = transactions.sumOf { it.totalHarga - it.nominal }
    val totalHarga = transactions.sumOf { it.totalHarga }
    
    val statusColor = if (sisaHutang > 0.0) Color(0xFFEF4444) else Color(0xFF22C57E)
    
    val backgroundColor = when {
        isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        hasTx -> statusColor.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    
    val borderColor = when {
        isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        hasTx -> statusColor.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = day.toString(),
            fontSize = 11.sp,
            fontWeight = if (hasTx || isInRange) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isInRange -> MaterialTheme.colorScheme.primary
                hasTx -> statusColor
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        if (hasTx) {
            Spacer(Modifier.height(1.dp))
            Icon(
                imageVector = Icons.Outlined.Science,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.height(1.dp))
            val shortText = formatShortRupiah(totalHarga)
            Text(
                text = shortText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CustomFilterChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) selectedColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (selected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        ),
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (selected) selectedColor else Color.Gray, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatShortRupiah(value: Double): String {
    return when {
        value >= 1_000_000_000 -> {
            val bill = value / 1_000_000_000.0
            if (bill % 1.0 == 0.0) "${bill.toInt()}B" else String.format(java.util.Locale.US, "%.1fB", bill)
        }
        value >= 1_000_000 -> {
            val mill = value / 1_000_000.0
            if (mill % 1.0 == 0.0) "${mill.toInt()}M" else String.format(java.util.Locale.US, "%.1fM", mill)
        }
        value >= 1_000 -> {
            val k = value / 1_000.0
            if (k % 1.0 == 0.0) "${k.toInt()}k" else String.format(java.util.Locale.US, "%.1fk", k)
        }
        else -> value.toInt().toString()
    }
}
