package com.posbah.app.ui.screens.bmp.invoices

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posbah.app.data.local.entities.BmpInvoiceEntity
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters

@Composable
fun InvoicesListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: InvoicesListViewModel = hiltViewModel()
) {
    val invoices by viewModel.filteredInvoices.collectAsState()
    val rawList by viewModel.invoices.collectAsState()
    val clients by viewModel.clients.collectAsState()

    val selectedClientId by viewModel.filterClientId.collectAsState()
    val filterStartDate by viewModel.filterStartDate.collectAsState()
    val filterEndDate by viewModel.filterEndDate.collectAsState()
    val filterPaid by viewModel.filterPaid.collectAsState()
    val filterBelumBayar by viewModel.filterBelumBayar.collectAsState()
    val filterPartial by viewModel.filterPartial.collectAsState()

    val selectedClient = remember(clients, selectedClientId) {
        clients.find { it.id == selectedClientId }
    }

    val context = LocalContext.current
    var showCalendarDialog by remember { mutableStateOf(false) }
    var isCustomerMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Invoice",
                subtitle = "${invoices.size} tagihan",
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { isCustomerMenuExpanded = true }, modifier = Modifier.testTag("btn-client-filter")) {
                            Icon(
                                imageVector = if (selectedClientId != null) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "Filter Pelanggan",
                                tint = if (selectedClientId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = isCustomerMenuExpanded,
                            onDismissRequest = { isCustomerMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Semua Pelanggan", fontWeight = if (selectedClientId == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    viewModel.setClientFilter(null)
                                    isCustomerMenuExpanded = false
                                }
                            )
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.clientName, fontWeight = if (selectedClientId == client.id) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        viewModel.setClientFilter(client.id)
                                        isCustomerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showCalendarDialog = true }, modifier = Modifier.testTag("btn-calendar-trigger")) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = "Kalender",
                            tint = if (filterStartDate != null || filterEndDate != null || filterPaid || filterBelumBayar || filterPartial)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-create-invoice")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Buat Invoice")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Active Filter Chips Row
            val activeFiltersExist = selectedClientId != null || filterStartDate != null || filterEndDate != null || filterPaid || filterBelumBayar || filterPartial
            if (activeFiltersExist) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter aktif:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (selectedClient != null) {
                            item {
                                ActiveFilterChip(
                                    label = selectedClient.clientName,
                                    onClear = { viewModel.setClientFilter(null) }
                                )
                            }
                        }
                        if (filterStartDate != null || filterEndDate != null) {
                            item {
                                val dateStr = when {
                                    filterStartDate != null && filterEndDate != null -> {
                                        val sdf = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
                                        "${sdf.format(java.util.Date(filterStartDate!!))} - ${sdf.format(java.util.Date(filterEndDate!!))}"
                                    }
                                    filterStartDate != null -> {
                                        val sdf = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
                                        "Sejak ${sdf.format(java.util.Date(filterStartDate!!))}"
                                    }
                                    else -> {
                                        val sdf = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
                                        "Hingga ${sdf.format(java.util.Date(filterEndDate!!))}"
                                    }
                                }
                                ActiveFilterChip(
                                    label = dateStr,
                                    onClear = { viewModel.setDateRange(null, null) }
                                )
                            }
                        }
                        if (filterPaid) {
                            item {
                                ActiveFilterChip(
                                    label = "Lunas",
                                    onClear = { viewModel.toggleFilterPaid(false) }
                                )
                            }
                        }
                        if (filterBelumBayar) {
                            item {
                                ActiveFilterChip(
                                    label = "Belum Bayar",
                                    onClear = { viewModel.toggleFilterBelumBayar(false) }
                                )
                            }
                        }
                        if (filterPartial) {
                            item {
                                ActiveFilterChip(
                                    label = "Cicil",
                                    onClear = { viewModel.toggleFilterPartial(false) }
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (invoices.isEmpty()) {
                    EmptyState(
                        title = "Tidak ada invoice",
                        description = if (activeFiltersExist) "Coba sesuaikan filter pencarian Anda" else "Buat invoice pertama Anda untuk mulai mencatat penagihan",
                        actionLabel = if (activeFiltersExist) "Reset Semua Filter" else "+ Buat Invoice",
                        onAction = {
                            if (activeFiltersExist) {
                                viewModel.setClientFilter(null)
                                viewModel.setDateRange(null, null)
                                viewModel.toggleFilterPaid(false)
                                viewModel.toggleFilterBelumBayar(false)
                                viewModel.toggleFilterPartial(false)
                            } else {
                                onCreate()
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(invoices, key = { it.id }) { inv ->
                            InvoiceRow(invoice = inv, onClick = { onOpen(inv.id) })
                        }
                    }
                }
            }
        }
    }

    if (showCalendarDialog) {
        BmpInvoiceCalendarDialog(
            viewModel = viewModel,
            onDismiss = { showCalendarDialog = false }
        )
    }
}

@Composable
private fun InvoiceRow(invoice: BmpInvoiceEntity, onClick: () -> Unit) {
    val (statusColor, statusBg) = when (invoice.status) {
        "PAID" -> Color(0xFF1F8B4C) to Color(0xFF1F8B4C).copy(alpha = 0.15f)
        "PARTIAL" -> Color(0xFFE08A1B) to Color(0xFFE08A1B).copy(alpha = 0.15f)
        "OVERDUE" -> Color(0xFFC5453B) to Color(0xFFC5453B).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("invoice-${invoice.id}")
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    invoice.number,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    invoice.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    Formatters.rupiah(invoice.totalAmount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = statusBg,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    Formatters.invoiceStatus(invoice.status),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveFilterChip(label: String, onClear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Clear",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClear() }
            )
        }
    }
}

@Composable
fun BmpInvoiceCalendarDialog(
    viewModel: InvoicesListViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val rawList by viewModel.invoices.collectAsState()
    val filterStartDate by viewModel.filterStartDate.collectAsState()
    val filterEndDate by viewModel.filterEndDate.collectAsState()
    val filterPaid by viewModel.filterPaid.collectAsState()
    val filterBelumBayar by viewModel.filterBelumBayar.collectAsState()
    val filterPartial by viewModel.filterPartial.collectAsState()

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

    val calendarList = remember(rawList, filterPaid, filterBelumBayar, filterPartial) {
        var result = rawList
        if (filterPaid || filterBelumBayar || filterPartial) {
            result = result.filter { inv ->
                (filterPaid && inv.status == "PAID") ||
                (filterBelumBayar && (inv.status == "UNPAID" || inv.status == "OVERDUE" || inv.status == "DRAFT")) ||
                (filterPartial && inv.status == "PARTIAL")
            }
        }
        result
    }

    val transactionsByDay = remember(calendarList, displayMonth, displayYear) {
        val map = mutableMapOf<Int, List<BmpInvoiceEntity>>()
        val cal = java.util.Calendar.getInstance()
        for (t in calendarList) {
            cal.timeInMillis = t.createdAt
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
                                        InvoiceCalendarDayCell(
                                            day = day,
                                            dayStart = dayStartVal,
                                            isInRange = isInRange,
                                            invoices = dayTx,
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomFilterChip(
                                label = "Lunas",
                                selected = filterPaid,
                                selectedColor = Color(0xFF1F8B4C),
                                onClick = { viewModel.toggleFilterPaid(!filterPaid) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomFilterChip(
                                label = "Belum Bayar",
                                selected = filterBelumBayar,
                                selectedColor = Color(0xFFC5453B),
                                onClick = { viewModel.toggleFilterBelumBayar(!filterBelumBayar) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomFilterChip(
                                label = "Cicil",
                                selected = filterPartial,
                                selectedColor = Color(0xFFE08A1B),
                                onClick = { viewModel.toggleFilterPartial(!filterPartial) }
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
fun InvoiceCalendarDayCell(
    day: Int,
    dayStart: Long,
    isInRange: Boolean,
    invoices: List<BmpInvoiceEntity>,
    onClick: () -> Unit
) {
    val hasTx = invoices.isNotEmpty()
    val totalAmount = invoices.sumOf { it.totalAmount }
    
    val statusColor = when {
        invoices.any { it.status == "UNPAID" || it.status == "OVERDUE" || it.status == "DRAFT" } -> Color(0xFFC5453B)
        invoices.any { it.status == "PARTIAL" } -> Color(0xFFE08A1B)
        else -> Color(0xFF1F8B4C)
    }
    
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
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.height(1.dp))
            val shortText = formatShortRupiah(totalAmount)
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
