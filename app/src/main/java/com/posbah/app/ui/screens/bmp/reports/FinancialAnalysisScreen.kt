package com.posbah.app.ui.screens.bmp.reports

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.remote.api.BmpApiService
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TopProductReport(
    val name: String,
    val qtySold: Double,
    val revenue: Double
)

data class FinancialReportUiState(
    val periodType: String = "MONTHLY", // MONTHLY, QUARTERLY, ANNUALLY
    val date: String = "",               // e.g. "2026-08"
    val periodLabel: String = "",
    val omzet: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalUnpaid: Double = 0.0,
    val cogs: Double = 0.0,
    val labaKotor: Double = 0.0,
    val gajiKaryawan: Double = 0.0,
    val biayaMaintenance: Double = 0.0,
    val biayaOperasionalLain: Double = 0.0,
    val totalBebanOperasional: Double = 0.0,
    val labaBersih: Double = 0.0,
    val cogsPercentage: Double = 0.0,
    val marginPercentage: Double = 0.0,
    val netMarginPercentage: Double = 0.0,
    val bepNominal: Double = 0.0,
    val topProducts: List<TopProductReport> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val warnings: List<String> = emptyList()
)

@HiltViewModel
class FinancialAnalysisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: BmpApiService,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    val businessMode: String get() = authRepository.activeBusinessMode() ?: "FNB"

    private val _uiState = MutableStateFlow(FinancialReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val defaultDate = sdf.format(Date())
        _uiState.update { it.copy(date = defaultDate, periodType = "MONTHLY") }
        fetchReport()
    }

    fun getCompanyName(): String {
        return authRepository.getActiveSession()?.displayName?.ifBlank { "POSBah Invoice & Manufaktur" } ?: "POSBah Invoice & Manufaktur"
    }

    fun setPeriodType(type: String) {
        val currentDate = Date()
        val date = when (type) {
            "MONTHLY" -> SimpleDateFormat("yyyy-MM", Locale.US).format(currentDate)
            "QUARTERLY" -> {
                val cal = Calendar.getInstance()
                val q = (cal.get(Calendar.MONTH) / 3) + 1
                "${cal.get(Calendar.YEAR)}-Q$q"
            }
            else -> SimpleDateFormat("yyyy", Locale.US).format(currentDate)
        }
        _uiState.update { it.copy(periodType = type, date = date) }
        fetchReport()
    }

    fun navigatePeriod(offset: Int) {
        val cal = Calendar.getInstance()
        val state = _uiState.value
        try {
            if (state.periodType == "MONTHLY") {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
                val d = sdf.parse(state.date) ?: return
                cal.time = d
                cal.add(Calendar.MONTH, offset)
                _uiState.update { it.copy(date = sdf.format(cal.time)) }
            } else if (state.periodType == "QUARTERLY") {
                val parts = state.date.split("-Q")
                val year = parts[0].toInt()
                val q = parts[1].toInt()
                var newQ = q + offset
                var newYear = year
                if (newQ > 4) {
                    newQ = 1
                    newYear++
                } else if (newQ < 1) {
                    newQ = 4
                    newYear--
                }
                _uiState.update { it.copy(date = "$newYear-Q$newQ") }
            } else {
                val year = state.date.toInt()
                _uiState.update { it.copy(date = (year + offset).toString()) }
            }
            fetchReport()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchReport() = viewModelScope.launch {
        val state = _uiState.value
        if (state.date.isBlank()) return@launch

        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val resp = api.getFinancialReport(state.periodType, state.date)
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!

                val topProductsList = (body["topProducts"] as? List<*>)?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    TopProductReport(
                        name = map["name"] as? String ?: "-",
                        qtySold = (map["qtySold"] as? Number)?.toDouble() ?: 0.0,
                        revenue = (map["revenue"] as? Number)?.toDouble() ?: 0.0
                    )
                } ?: emptyList()

                val warningsList = (body["warnings"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                _uiState.update {
                    it.copy(
                        periodLabel = body["period"] as? String ?: state.date,
                        omzet = (body["omzet"] as? Number)?.toDouble() ?: 0.0,
                        totalPaid = (body["totalPaid"] as? Number)?.toDouble() ?: 0.0,
                        totalUnpaid = (body["totalUnpaid"] as? Number)?.toDouble() ?: 0.0,
                        cogs = (body["cogs"] as? Number)?.toDouble() ?: 0.0,
                        labaKotor = (body["labaKotor"] as? Number)?.toDouble() ?: 0.0,
                        gajiKaryawan = (body["gajiKaryawan"] as? Number)?.toDouble() ?: 0.0,
                        biayaMaintenance = (body["biayaMaintenance"] as? Number)?.toDouble() ?: 0.0,
                        biayaOperasionalLain = (body["biayaOperasionalLain"] as? Number)?.toDouble() ?: 0.0,
                        totalBebanOperasional = (body["totalBebanOperasional"] as? Number)?.toDouble() ?: 0.0,
                        labaBersih = (body["labaBersih"] as? Number)?.toDouble() ?: 0.0,
                        cogsPercentage = (body["cogsPercentage"] as? Number)?.toDouble() ?: 0.0,
                        marginPercentage = (body["marginPercentage"] as? Number)?.toDouble() ?: 0.0,
                        netMarginPercentage = (body["netMarginPercentage"] as? Number)?.toDouble() ?: 0.0,
                        bepNominal = (body["bepNominal"] as? Number)?.toDouble() ?: 0.0,
                        topProducts = topProductsList,
                        isLoading = false,
                        warnings = warningsList
                    )
                }
            } else {
                val errBody = resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Gagal memuat data dari server (HTTP ${resp.code()})"
                _uiState.update { it.copy(isLoading = false, error = errBody) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Terjadi kesalahan jaringan.") }
        }
    }

    fun exportExcel(onFileReady: (File) -> Unit) = viewModelScope.launch {
        val state = _uiState.value
        Toast.makeText(context, "Mengekspor laporan ke Excel...", Toast.LENGTH_SHORT).show()
        try {
            val resp = api.downloadFinancialReportExcel(state.periodType, state.date)
            if (resp.isSuccessful && resp.body() != null) {
                val file = saveFileSafely(resp.body()!!, "Laporan_Keuangan_${getCompanyName().replace(" ", "_")}_${state.date}.csv")
                if (file != null) {
                    onFileReady(file)
                }
            } else {
                val errBody = resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "HTTP ${resp.code()}"
                Toast.makeText(context, "Gagal mengunduh file Excel: $errBody", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveFileSafely(body: ResponseBody, fileName: String): File? {
        return try {
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val file = File(targetDir, fileName)
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menyimpan file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }
}

@Composable
fun FinancialAnalysisScreen(
    onBack: () -> Unit,
    viewModel: FinancialAnalysisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val companyName = remember { viewModel.getCompanyName() }
    val businessMode = viewModel.businessMode

    var downloadedFile by remember { mutableStateOf<File?>(null) }

    if (businessMode != "BMP") {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📊 Analisis Keuangan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fitur ini hanya tersedia untuk mode Invoice & Manufaktur.\nAkun Anda saat ini menggunakan mode $businessMode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onBack) { Text("Kembali") }
            }
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Analisis Keuangan",
                onBack = onBack,
                actions = {
                    // WhatsApp Summary Share Button
                    IconButton(
                        onClick = {
                            shareFinancialSummaryWhatsApp(context, companyName, state)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Kirim Ringkasan WA",
                            tint = Color(0xFF2E7D32)
                        )
                    }

                    // Print / Save PDF Button
                    IconButton(
                        onClick = {
                            printFinancialReportPdf(context, companyName, state)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = "Cetak / Simpan PDF",
                            tint = Color(0xFFD32F2F)
                        )
                    }

                    // Export Excel/CSV Button
                    IconButton(
                        onClick = {
                            viewModel.exportExcel { file ->
                                downloadedFile = file
                            }
                        },
                        modifier = Modifier.testTag("btn-export-financial")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Ekspor Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period Selector
            item {
                PeriodTypeSelector(
                    selectedType = state.periodType,
                    onSelect = { viewModel.setPeriodType(it) }
                )
            }

            // Period Navigator
            item {
                PeriodNavigator(
                    label = state.periodLabel.ifBlank { state.date },
                    onPrev = { viewModel.navigatePeriod(-1) },
                    onNext = { viewModel.navigatePeriod(1) }
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.error != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.error!!,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.fetchReport() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else {
                if (state.warnings.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = Color(0xFFF59E0B),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.warnings.forEach { warningText ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = "Peringatan",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = warningText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── KPI Summary Cards ─────────────────────────────────────────
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Card 1: Omzet & Kas Masuk
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Omzet Penjualan (Kotor)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Formatters.rupiah(state.omzet), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                                        modifier = Modifier.size(44.dp),
                                        contentColor = Color(0xFF10B981)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kas Cair: ${Formatters.rupiah(state.totalPaid)}", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                                    Text("Piutang: ${Formatters.rupiah(state.totalUnpaid)}", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Card 2: Laba Kotor vs Beban Operasional
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FinancialStatCard(
                                title = "Laba Kotor",
                                value = Formatters.rupiah(state.labaKotor),
                                subValue = "Margin: ${String.format("%.1f", state.marginPercentage)}%",
                                icon = Icons.Outlined.AccountBalanceWallet,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.weight(1f)
                            )
                            FinancialStatCard(
                                title = "Beban Operasional",
                                value = Formatters.rupiah(state.totalBebanOperasional),
                                subValue = "Gaji + Servis + Kas",
                                icon = Icons.Outlined.ReceiptLong,
                                color = Color(0xFFE65100),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Card 3: LABA BERSIH (NET PROFIT)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (state.labaBersih >= 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.5.dp, if (state.labaBersih >= 0) Color(0xFF10B981) else Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "LABA BERSIH (NET PROFIT)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.labaBersih >= 0) Color(0xFF047857) else Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = Formatters.rupiah(state.labaBersih),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (state.labaBersih >= 0) Color(0xFF065F46) else Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "Net Margin: ${String.format("%.1f", state.netMarginPercentage)}% • BEP: ${Formatters.rupiah(state.bepNominal)}",
                                        fontSize = 11.sp,
                                        color = if (state.labaBersih >= 0) Color(0xFF047857) else Color(0xFFB91C1C)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (state.labaBersih >= 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                    modifier = Modifier.size(48.dp),
                                    contentColor = if (state.labaBersih >= 0) Color(0xFF047857) else Color(0xFFB91C1C)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (state.labaBersih >= 0) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Struktur Keuangan (Bar Chart 3 Segmen) ───────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Struktur & Komposisi Pendapatan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(14.dp))

                            val total = state.omzet
                            val cogsRatio = if (total > 0) (state.cogs / total).coerceIn(0.0, 1.0).toFloat() else 0f
                            val opexRatio = if (total > 0) (state.totalBebanOperasional / total).coerceIn(0.0, 1.0 - cogsRatio.toDouble()).toFloat() else 0f
                            val netRatio = if (total > 0 && state.labaBersih > 0) (state.labaBersih / total).coerceIn(0.0, 1.0).toFloat() else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (cogsRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(cogsRatio)
                                            .background(Color(0xFFEF4444), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                    )
                                }
                                if (opexRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(if (netRatio > 0) opexRatio / (1f - cogsRatio) else 1f)
                                            .background(Color(0xFFF59E0B))
                                    )
                                }
                                if (netRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f)
                                            .background(Color(0xFF10B981), RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LegendItem(color = Color(0xFFEF4444), label = "HPP Bahan Baku (COGS)", value = "${String.format("%.1f", state.cogsPercentage)}% (${Formatters.rupiah(state.cogs)})")
                                LegendItem(color = Color(0xFFF59E0B), label = "Beban Operasional (OPEX)", value = "${String.format("%.1f", if (state.omzet > 0) (state.totalBebanOperasional / state.omzet * 100.0) else 0.0)}% (${Formatters.rupiah(state.totalBebanOperasional)})")
                                LegendItem(color = Color(0xFF10B981), label = "Laba Bersih (Net Profit)", value = "${String.format("%.1f", state.netMarginPercentage)}% (${Formatters.rupiah(state.labaBersih)})")
                            }
                        }
                    }
                }

                // ── Laporan Laba Rugi Komprehensif ───────────────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Laporan Laba Rugi Komprehensif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))

                            ReportLine("OMZET PENJUALAN", Formatters.rupiah(state.omzet), isHeader = true)
                            ReportLine("  - Kas Riil Masuk", Formatters.rupiah(state.totalPaid), valueColor = Color(0xFF2E7D32))
                            ReportLine("  - Sisa Piutang Berjalan", Formatters.rupiah(state.totalUnpaid), valueColor = Color(0xFFE65100))
                            ReportLine("Harga Pokok Penjualan (HPP / COGS)", "- ${Formatters.rupiah(state.cogs)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ReportLine(
                                label = "LABA KOTOR (Gross Profit)",
                                value = Formatters.rupiah(state.labaKotor),
                                isHeader = true,
                                valueColor = if (state.labaKotor >= 0) Color(0xFF2563EB) else Color(0xFFEF4444)
                            )
                            Spacer(Modifier.height(8.dp))

                            Text("BEBAN OPERASIONAL (OPEX)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            ReportLine("  - Beban Gaji Karyawan", "- ${Formatters.rupiah(state.gajiKaryawan)}")
                            ReportLine("  - Beban Pemeliharaan Mesin & Matras", "- ${Formatters.rupiah(state.biayaMaintenance)}")
                            if (state.biayaOperasionalLain > 0) {
                                ReportLine("  - Beban Kas Operasional Lainnya", "- ${Formatters.rupiah(state.biayaOperasionalLain)}")
                            }
                            ReportLine("TOTAL BEBAN OPERASIONAL", "- ${Formatters.rupiah(state.totalBebanOperasional)}", isBold = true)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            ReportLine(
                                label = "LABA BERSIH (NET PROFIT)",
                                value = Formatters.rupiah(state.labaBersih),
                                isHeader = true,
                                valueColor = if (state.labaBersih >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // ── Produk Terlaris ──────────────────────────────────────────
                if (state.topProducts.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "5 Produk Terlaris",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))

                                state.topProducts.forEachIndexed { idx, prod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${idx + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.name, fontWeight = FontWeight.Medium)
                                            Text("${prod.qtySold.toInt()} unit terjual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(Formatters.rupiah(prod.revenue), fontWeight = FontWeight.SemiBold)
                                    }
                                    if (idx < state.topProducts.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet saat File Excel Berhasil Diunduh
    if (downloadedFile != null) {
        val file = downloadedFile!!
        AlertDialog(
            onDismissRequest = { downloadedFile = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(8.dp))
                    Text("Ekspor Excel Berhasil")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("File laporan keuangan telah disimpan:")
                    Text(file.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Lokasi: ${file.parent}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openExportedFile(context, file)
                        downloadedFile = null
                    }
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buka File")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            shareExportedFile(context, file)
                            downloadedFile = null
                        }
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bagikan")
                    }
                    TextButton(onClick = { downloadedFile = null }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}

@Composable
fun PeriodTypeSelector(
    selectedType: String,
    onSelect: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            listOf(
                "MONTHLY" to "Bulanan",
                "QUARTERLY" to "Kuartal",
                "ANNUALLY" to "Tahunan"
            ).forEach { (type, label) ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(type) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodNavigator(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Sebelumnya")
        }
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Selanjutnya")
        }
    }
}

@Composable
fun FinancialStatCard(
    title: String,
    value: String,
    subValue: String? = null,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp),
                contentColor = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (!subValue.isNullOrBlank()) {
                    Text(subValue, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ReportLine(
    label: String,
    value: String,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isHeader) 13.sp else 12.sp,
            color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isHeader) 13.sp else 12.sp,
            color = if (valueColor != Color.Unspecified) valueColor else (if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        )
    }
}

// ── Document Printing & Sharing Helpers ───────────────────────────────────────

fun printFinancialReportPdf(context: Context, companyName: String, state: FinancialReportUiState) {
    val html = buildFinancialReportHtml(companyName, state)
    val jobName = "Laporan_Keuangan_${companyName.replace(" ", "_")}_${state.date}"

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val printAttributes = PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        }
    }
    webView.loadDataWithBaseURL("https://www.zedmz.cloud", html, "text/html", "UTF-8", null)
}

fun buildFinancialReportHtml(companyName: String, state: FinancialReportUiState): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.forLanguageTag("id-ID"))
    val printDate = sdf.format(Date())

    val topProductsHtml = if (state.topProducts.isNotEmpty()) {
        val rows = state.topProducts.mapIndexed { idx, p ->
            "<tr><td>${idx + 1}</td><td>${p.name}</td><td style='text-align:right;'>${p.qtySold.toInt()} pcs</td><td style='text-align:right;'>${Formatters.rupiah(p.revenue)}</td></tr>"
        }.joinToString("\n")
        """
        <h3 style="margin-top:20px; color:#1e293b; border-bottom:1px solid #e2e8f0; padding-bottom:6px;">Produk Terlaris</h3>
        <table class="report-table">
            <thead>
                <tr><th>No</th><th>Nama Produk</th><th style="text-align:right;">Terjual</th><th style="text-align:right;">Pendapatan</th></tr>
            </thead>
            <tbody>$rows</tbody>
        </table>
        """
    } else ""

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>Laporan Keuangan - $companyName</title>
        <style>
            body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 25px; color: #1e293b; line-height: 1.5; }
            .header { text-align: center; border-bottom: 2px solid #0f766e; padding-bottom: 12px; margin-bottom: 20px; }
            .header h1 { margin: 0; font-size: 20px; color: #0f766e; }
            .header h2 { margin: 4px 0 0 0; font-size: 15px; color: #334155; font-weight: normal; }
            .meta-info { display: flex; justify-content: space-between; font-size: 12px; color: #64748b; margin-bottom: 16px; }
            .report-table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
            .report-table th, .report-table td { border: 1px solid #cbd5e1; padding: 8px 10px; }
            .report-table th { background-color: #f1f5f9; color: #334155; font-weight: bold; text-align: left; }
            .row-header { background-color: #f8fafc; font-weight: bold; }
            .total-row { background-color: #e6fffa; font-weight: bold; font-size: 13px; color: #047857; }
            .loss-row { background-color: #fef2f2; font-weight: bold; font-size: 13px; color: #b91c1c; }
            .signature { margin-top: 40px; display: flex; justify-content: flex-end; }
            .signature-box { text-align: center; width: 200px; font-size: 12px; }
        </style>
    </head>
    <body>
        <div class="header">
            <h1>$companyName</h1>
            <h2>LAPORAN KEUANGAN & LABA RUGI KOMPREHENSIF</h2>
        </div>
        <div class="meta-info">
            <div><strong>Periode:</strong> ${state.periodLabel.ifBlank { state.date }} (${state.periodType})</div>
            <div><strong>Dicetak:</strong> $printDate</div>
        </div>

        <table class="report-table">
            <thead>
                <tr>
                    <th>Pos Keuangan</th>
                    <th style="text-align: right;">Nominal (Rupiah)</th>
                    <th>Keterangan</th>
                </tr>
            </thead>
            <tbody>
                <tr class="row-header">
                    <td>OMZET PENJUALAN (Faktur Diterbitkan)</td>
                    <td style="text-align: right;">${Formatters.rupiah(state.omzet)}</td>
                    <td>Total seluruh tagihan penjualan</td>
                </tr>
                <tr>
                    <td style="padding-left: 20px;">- Kas Riil Masuk (Cash In)</td>
                    <td style="text-align: right; color:#047857;">${Formatters.rupiah(state.totalPaid)}</td>
                    <td>Telah diterima ke kas/rekening</td>
                </tr>
                <tr>
                    <td style="padding-left: 20px;">- Sisa Piutang Berjalan (AR)</td>
                    <td style="text-align: right; color:#d97706;">${Formatters.rupiah(state.totalUnpaid)}</td>
                    <td>Tagihan belum lunas</td>
                </tr>
                <tr>
                    <td>Harga Pokok Penjualan (HPP / COGS)</td>
                    <td style="text-align: right; color:#dc2626;">- ${Formatters.rupiah(state.cogs)}</td>
                    <td>Biaya bahan baku (${String.format("%.1f", state.cogsPercentage)}%)</td>
                </tr>
                <tr style="background-color: #eff6ff; font-weight: bold;">
                    <td>LABA KOTOR (Gross Profit)</td>
                    <td style="text-align: right; color:#1d4ed8;">${Formatters.rupiah(state.labaKotor)}</td>
                    <td>Gross Margin: ${String.format("%.1f", state.marginPercentage)}%</td>
                </tr>
                <tr class="row-header">
                    <td colspan="3">BEBAN OPERASIONAL (OPEX)</td>
                </tr>
                <tr>
                    <td style="padding-left: 20px;">- Beban Gaji Karyawan</td>
                    <td style="text-align: right;">- ${Formatters.rupiah(state.gajiKaryawan)}</td>
                    <td>Total payroll karyawan</td>
                </tr>
                <tr>
                    <td style="padding-left: 20px;">- Beban Pemeliharaan Mesin & Matras</td>
                    <td style="text-align: right;">- ${Formatters.rupiah(state.biayaMaintenance)}</td>
                    <td>Biaya servis preventif & perbaikan</td>
                </tr>
                ${if (state.biayaOperasionalLain > 0) "<tr><td style='padding-left: 20px;'>- Beban Operasional Lainnya</td><td style='text-align: right;'>- ${Formatters.rupiah(state.biayaOperasionalLain)}</td><td>Operasional pabrik</td></tr>" else ""}
                <tr style="font-weight: bold;">
                    <td>TOTAL BEBAN OPERASIONAL</td>
                    <td style="text-align: right; color:#ea580c;">- ${Formatters.rupiah(state.totalBebanOperasional)}</td>
                    <td>Total pengeluaran operasional</td>
                </tr>
                <tr class="${if (state.labaBersih >= 0) "total-row" else "loss-row"}">
                    <td>LABA BERSIH (NET PROFIT)</td>
                    <td style="text-align: right;">${Formatters.rupiah(state.labaBersih)}</td>
                    <td>Net Margin: ${String.format("%.1f", state.netMarginPercentage)}%</td>
                </tr>
            </tbody>
        </table>

        $topProductsHtml

        <div class="signature">
            <div class="signature-box">
                <p>Mengetahui,</p>
                <br><br><br>
                <p><strong>( Pimpinan / Manajemen )</strong></p>
            </div>
        </div>
    </body>
    </html>
    """.trimIndent()
}

fun shareFinancialSummaryWhatsApp(context: Context, companyName: String, state: FinancialReportUiState) {
    val sb = StringBuilder()
    sb.append("*LAPORAN KEUANGAN & LABA RUGI*\n")
    sb.append("*$companyName*\n\n")
    sb.append("📅 *Periode:* ${state.periodLabel.ifBlank { state.date }} (${state.periodType})\n\n")
    sb.append("📈 *Omzet Penjualan:* ${Formatters.rupiah(state.omzet)}\n")
    sb.append("  • Kas Masuk: ${Formatters.rupiah(state.totalPaid)}\n")
    sb.append("  • Sisa Piutang: ${Formatters.rupiah(state.totalUnpaid)}\n")
    sb.append("📦 *HPP Bahan Baku:* ${Formatters.rupiah(state.cogs)} (${String.format("%.1f", state.cogsPercentage)}%)\n")
    sb.append("💰 *Laba Kotor:* ${Formatters.rupiah(state.labaKotor)} (${String.format("%.1f", state.marginPercentage)}%)\n\n")
    sb.append("⚙️ *Beban Operasional:*\n")
    sb.append("  • Gaji Karyawan: ${Formatters.rupiah(state.gajiKaryawan)}\n")
    sb.append("  • Servis Mesin/Matras: ${Formatters.rupiah(state.biayaMaintenance)}\n")
    if (state.biayaOperasionalLain > 0) {
        sb.append("  • Biaya Kas Lain: ${Formatters.rupiah(state.biayaOperasionalLain)}\n")
    }
    sb.append("  • *Total Beban:* ${Formatters.rupiah(state.totalBebanOperasional)}\n\n")
    sb.append("🏆 *LABA BERSIH (Net Profit):* ${Formatters.rupiah(state.labaBersih)}\n")
    sb.append("📊 *Net Margin:* ${String.format("%.1f", state.netMarginPercentage)}%\n")
    sb.append("🎯 *Titik Impas (BEP):* ${Formatters.rupiah(state.bepNominal)}\n\n")
    sb.append("_Laporan resmi otomatis POSBah Invoice & Manufaktur._")

    val text = sb.toString()
    try {
        val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(text))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Ringkasan Keuangan via"))
    }
}

fun openExportedFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membuka file CSV/Excel: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun shareExportedFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan File Laporan Keuangan"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
