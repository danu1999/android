package com.posbah.app.ui.screens.bmp.cashflow

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.remote.api.BmpApiService
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.ui.components.StatChip
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Context

// ── Data class CashFlowEntry (BMP Arus Kas Terintegrasi) ──────────────────────
data class CashFlowEntry(
    val id: Long = 0L,
    val tenantId: String = "",
    val transactionType: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val costType: String = "",
    val transactionDate: Long = 0L,
    val paymentRefId: Long? = null,
    val sourceType: String = "MANUAL" // "INVOICE", "BAHAN_BAKU", "PAYROLL", "MANUAL"
)

@HiltViewModel
class CashFlowViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: BmpApiService,
    private val settingsRepo: com.posbah.app.data.repository.BmpSettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
    val selectedMonth = MutableStateFlow(currentMonth)

    private val _flows = MutableStateFlow<List<CashFlowEntry>>(emptyList())
    val flows = _flows.asStateFlow()

    private val _totalIn = MutableStateFlow(0.0)
    val totalIn = _totalIn.asStateFlow()

    private val _totalOut = MutableStateFlow(0.0)
    val totalOut = _totalOut.asStateFlow()

    private val _paymentToInvoice = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val paymentToInvoice = _paymentToInvoice.asStateFlow()

    private val _settings = MutableStateFlow<com.posbah.app.data.local.entities.BmpSettingsEntity?>(null)
    val settings = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _settings.value = settingsRepo.get(tenantId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            selectedMonth.collect { month ->
                refreshCashFlow(month)
            }
        }
    }

    fun refreshCashFlow(month: String) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val entries = mutableListOf<CashFlowEntry>()

        try {
            // 1. Kas Masuk: Pembayaran Invoice Klien
            val paymentsResp = api.getPayments()
            if (paymentsResp.isSuccessful) {
                val paymentList = paymentsResp.body() ?: emptyList()
                val invoiceMap = paymentList.mapNotNull { p ->
                    val paymentId = (p["id"] as? Number)?.toLong() ?: return@mapNotNull null
                    val invoiceId = (p["invoiceId"] as? Number)?.toLong() ?: return@mapNotNull null
                    paymentId to invoiceId
                }.toMap()
                _paymentToInvoice.value = invoiceMap

                paymentList.forEach { p ->
                    val pDate = (p["paymentDate"] as? Number)?.toLong() ?: return@forEach
                    val pAmt = (p["paymentAmount"] as? Number)?.toDouble() ?: 0.0
                    if (pAmt > 0 && sdf.format(java.util.Date(pDate)) == month) {
                        val pId = (p["id"] as? Number)?.toLong() ?: 0L
                        val invId = invoiceMap[pId]
                        val notes = p["notes"] as? String
                        val desc = buildString {
                            append("Pembayaran Invoice")
                            if (invId != null) append(" #$invId")
                            if (!notes.isNullOrBlank()) append(" ($notes)")
                        }
                        entries.add(
                            CashFlowEntry(
                                id = 1_000_000L + pId,
                                tenantId = tenantId,
                                transactionType = "MASUK",
                                description = desc,
                                amount = pAmt,
                                costType = "OPERATING_EXPENSE",
                                transactionDate = pDate,
                                paymentRefId = pId,
                                sourceType = "INVOICE"
                            )
                        )
                    }
                }
            }

            // 2. Kas Keluar: Pembelian Bahan Baku & Biaya Operasional
            val bbResp = api.getBahanBaku()
            if (bbResp.isSuccessful) {
                val bbList = bbResp.body() ?: emptyList()
                bbList.forEach { bb ->
                    val tgl = (bb["tanggal"] as? Number)?.toLong() ?: return@forEach
                    val nominal = (bb["nominal"] as? Number)?.toDouble() ?: (bb["totalHarga"] as? Number)?.toDouble() ?: 0.0
                    if (nominal > 0 && sdf.format(java.util.Date(tgl)) == month) {
                        val id = (bb["id"] as? Number)?.toLong() ?: 0L
                        val category = bb["category"] as? String ?: "BAHAN_BAKU"
                        val supplier = bb["supplier"] as? String
                        val noTagihan = bb["noTagihan"] as? String
                        val notes = bb["notes"] as? String
                        val title = supplier ?: if (category == "BAHAN_BAKU") "Bahan Baku" else "Biaya Pabrik"
                        val desc = "$title: ${notes ?: noTagihan ?: "Pembelian"}"
                        val costType = if (category == "BAHAN_BAKU") "DIRECT_LABOR" else "FACTORY_OVERHEAD"
                        entries.add(
                            CashFlowEntry(
                                id = 2_000_000L + id,
                                tenantId = tenantId,
                                transactionType = "KELUAR",
                                description = desc,
                                amount = nominal,
                                costType = costType,
                                transactionDate = tgl,
                                sourceType = "BAHAN_BAKU"
                            )
                        )
                    }
                }
            }

            // 3. Kas Keluar: Penggajian Karyawan (Payrolls)
            val payrollsResp = api.getPayrolls()
            if (payrollsResp.isSuccessful) {
                val payrollList = payrollsResp.body() ?: emptyList()
                payrollList.forEach { pr ->
                    val payDate = (pr["paymentDate"] as? Number)?.toLong() ?: return@forEach
                    val amount = (pr["amount"] as? Number)?.toDouble() ?: 0.0
                    if (amount > 0 && sdf.format(java.util.Date(payDate)) == month) {
                        val id = (pr["id"] as? Number)?.toLong() ?: 0L
                        val empName = pr["employeeName"] as? String ?: "Karyawan"
                        val days = (pr["attendanceCount"] as? Number)?.toInt() ?: 0
                        val descNote = pr["description"] as? String
                        val desc = "Gaji: $empName ($days hari)" + if (!descNote.isNullOrBlank()) " - $descNote" else ""
                        entries.add(
                            CashFlowEntry(
                                id = 3_000_000L + id,
                                tenantId = tenantId,
                                transactionType = "KELUAR",
                                description = desc,
                                amount = amount,
                                costType = "DIRECT_LABOR",
                                transactionDate = payDate,
                                sourceType = "PAYROLL"
                            )
                        )
                    }
                }
            }

            entries.sortByDescending { it.transactionDate }
            _flows.value = entries
            _totalIn.value = entries.filter { it.transactionType == "MASUK" }.sumOf { it.amount }
            _totalOut.value = entries.filter { it.transactionType == "KELUAR" }.sumOf { it.amount }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insert(
        type: String,
        desc: String,
        amount: Double,
        costType: String = "OPERATING_EXPENSE",
        transactionDate: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        if (desc.isBlank() || amount <= 0) return@launch
        val entry = CashFlowEntry(
            id = -System.currentTimeMillis(),
            tenantId = tenantId,
            transactionDate = transactionDate,
            transactionType = type,
            description = desc,
            amount = amount,
            costType = if (type == "MASUK") "OPERATING_EXPENSE" else costType,
            sourceType = "MANUAL"
        )
        _flows.value = (_flows.value + entry).sortedByDescending { it.transactionDate }
        _totalIn.value = _flows.value.filter { it.transactionType == "MASUK" }.sumOf { it.amount }
        _totalOut.value = _flows.value.filter { it.transactionType == "KELUAR" }.sumOf { it.amount }
    }

    fun postMonthlyOverhead() = viewModelScope.launch {
        val s = _settings.value ?: return@launch
        val totalGaji = s.jumlahKaryawan * s.gajiHarian * s.hariKerjaSebulan

        if (totalGaji > 0) insert("KELUAR", "Gaji Karyawan Bulanan (Estimasi Rutin)", totalGaji, "DIRECT_LABOR")
        if (s.listrikBulanan > 0) insert("KELUAR", "Listrik Bulanan (Estimasi Rutin)", s.listrikBulanan, "FACTORY_OVERHEAD")
    }
}

@Composable
fun CashFlowScreen(
    onBack: () -> Unit,
    onNavigateToInvoiceDetail: (Long) -> Unit = {},
    viewModel: CashFlowViewModel = hiltViewModel()
) {
    val flows by viewModel.flows.collectAsState()
    val totalIn by viewModel.totalIn.collectAsState()
    val totalOut by viewModel.totalOut.collectAsState()
    val selectedMonthState by viewModel.selectedMonth.collectAsState()
    val paymentToInvoice by viewModel.paymentToInvoice.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var formType by remember { mutableStateOf("MASUK") }
    var formDesc by remember { mutableStateOf("") }
    var formAmt by remember { mutableStateOf("") }
    var formCostType by remember { mutableStateOf("OPERATING_EXPENSE") }
    var formDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showPostOverheadDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val monthsList = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        (0..11).map { offset ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, -offset)
            sdf.format(cal.time)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PosBahTopBar(title = "Arus Kas", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showForm = true
                    formType = "MASUK"; formDesc = ""; formAmt = ""; formCostType = "OPERATING_EXPENSE"
                    formDate = System.currentTimeMillis()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab-add-cashflow")
            ) { Icon(Icons.Outlined.Add, contentDescription = "Tambah") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn-select-month")
                        ) {
                            Text("Periode: $selectedMonthState")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            monthsList.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        viewModel.selectedMonth.value = m
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showPostOverheadDialog = true },
                        modifier = Modifier.weight(1f).testTag("btn-post-overhead"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Posting Biaya Rutin", maxLines = 1)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        StatChip("Masuk", Formatters.rupiah(totalIn), Color(0xFF22C57E))
                    }
                    Box(Modifier.weight(1f)) {
                        StatChip("Keluar", Formatters.rupiah(totalOut), MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.padding(top = 4.dp))
                StatChip(
                    "Saldo Bersih",
                    Formatters.rupiah(totalIn - totalOut),
                    if (totalIn - totalOut >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            items(flows, key = { it.id }) { item ->
                val invoiceId = item.paymentRefId?.let { paymentToInvoice[it] }
                val clickableModifier = if (invoiceId != null) {
                    Modifier.clickable { onNavigateToInvoiceDetail(invoiceId) }
                } else {
                    Modifier
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(clickableModifier)
                        .testTag("cashflow-${item.id}")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.description, style = MaterialTheme.typography.titleSmall)
                                if (item.sourceType == "INVOICE" || item.paymentRefId != null) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Text(
                                            "Dari Invoice",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (item.sourceType == "PAYROLL") {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF047857)
                                    ) {
                                        Text(
                                            "Gaji Karyawan",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (item.sourceType == "BAHAN_BAKU") {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ) {
                                        Text(
                                            "Bahan Baku / Operasional",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                Formatters.dateLong(item.transactionDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            (if (item.transactionType == "MASUK") "+ " else "- ") + Formatters.rupiah(item.amount),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (item.transactionType == "MASUK") Color(0xFF22C57E)
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("Catat Arus Kas") },
            text = {
                Column {
                    Row {
                        listOf("MASUK", "KELUAR").forEach { t ->
                            val selected = formType == t
                            TextButton(onClick = { formType = t }, modifier = Modifier.testTag("cf-type-$t")) {
                                Text(
                                    t,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = if (selected) MaterialTheme.typography.labelLarge
                                            else MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    // ── Tanggal Transaksi ────────────────────────────────────────
                    val dateDisplayStr = remember(formDate) {
                        java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.forLanguageTag("id-ID"))
                            .format(java.util.Date(formDate))
                    }
                    Box {
                        OutlinedTextField(
                            value = dateDisplayStr,
                            onValueChange = {},
                            label = { Text("Tanggal Transaksi") },
                            readOnly = true,
                            trailingIcon = {
                                TextButton(onClick = { showDatePicker = true }) {
                                    Text("Ubah", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("cf-date")
                        )
                        // Overlay transparan untuk menangkap klik & mencegah keyboard muncul
                        Box(Modifier.matchParentSize().clickable { showDatePicker = true })
                    }
                    Spacer(Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = formDesc,
                        onValueChange = { formDesc = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth().testTag("cf-desc")
                    )
                    Spacer(Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = formAmt,
                        onValueChange = { formAmt = it },
                        label = { Text("Jumlah (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("cf-amount")
                    )
                    if (formType == "KELUAR") {
                        Spacer(Modifier.padding(top = 10.dp))
                        Text("Kategori Pengeluaran:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.padding(top = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "OPERATING_EXPENSE" to "OPEX",
                                "FACTORY_OVERHEAD" to "Overhead",
                                "DIRECT_LABOR" to "Tenaga Kerja"
                            ).forEach { (valType, label) ->
                                val selected = formCostType == valType
                                OutlinedButton(
                                    onClick = { formCostType = valType },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.insert(
                            formType,
                            formDesc,
                            formAmt.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            formCostType,
                            formDate
                        )
                        showForm = false
                    },
                    modifier = Modifier.testTag("btn-save-cf")
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Batal") } }
        )
    }

    // ── Date Picker Dialog (android.app.DatePickerDialog via DisposableEffect) ──
    if (showDatePicker) {
        val pickerCalendar = remember(formDate) {
            java.util.Calendar.getInstance().also { it.timeInMillis = formDate }
        }
        DisposableEffect(Unit) {
            val dialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val cal = java.util.Calendar.getInstance()
                    cal.set(year, month, dayOfMonth, 0, 0, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    formDate = cal.timeInMillis
                    showDatePicker = false
                },
                pickerCalendar.get(java.util.Calendar.YEAR),
                pickerCalendar.get(java.util.Calendar.MONTH),
                pickerCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            dialog.datePicker.maxDate = System.currentTimeMillis()  // tidak bisa pilih tanggal masa depan
            dialog.setOnDismissListener { showDatePicker = false }
            dialog.show()
            onDispose { if (dialog.isShowing) dialog.dismiss() }
        }
    }

    val settingsState by viewModel.settings.collectAsState()

    if (showPostOverheadDialog) {
        val s = settingsState
        if (s == null) {
            AlertDialog(
                onDismissRequest = { showPostOverheadDialog = false },
                title = { Text("Posting Biaya Rutin") },
                text = { Text("Memuat data pengaturan toko...") },
                confirmButton = { TextButton(onClick = { showPostOverheadDialog = false }) { Text("OK") } }
            )
        } else {
            val totalGaji = s.jumlahKaryawan * s.gajiHarian * s.hariKerjaSebulan
            val totalOverhead = s.listrikBulanan + totalGaji

            AlertDialog(
                onDismissRequest = { showPostOverheadDialog = false },
                title = { Text("Posting Biaya Rutin") },
                text = {
                    Column {
                        Text("Apakah Anda yakin ingin memposting biaya operasional bulanan rutin ke kas keluar?", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.padding(top = 12.dp))
                        Text("Rincian Biaya (Dari Pengaturan):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.padding(top = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1. Gaji Karyawan (${s.jumlahKaryawan} org x ${s.hariKerjaSebulan} hari):", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.rupiah(totalGaji), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("2. Listrik Bulanan:", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.rupiah(s.listrikBulanan), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Kas Keluar:", fontWeight = FontWeight.Bold)
                            Text(Formatters.rupiah(totalOverhead), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.postMonthlyOverhead()
                            showPostOverheadDialog = false
                            Toast.makeText(context, "Biaya rutin berhasil diposting ke kas keluar!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn-confirm-post-overhead")
                    ) { Text("Posting Sekarang") }
                },
                dismissButton = {
                    TextButton(onClick = { showPostOverheadDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
