package com.posbah.app.ui.screens.bmp.reports

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    val date: String = "",               // e.g. "2026-06"
    val periodLabel: String = "",
    val omzet: Double = 0.0,
    val cogs: Double = 0.0,
    val labaKotor: Double = 0.0,
    val opex: Double = 0.0,
    val labaBersih: Double = 0.0,
    val bep: Double = 0.0,
    val cogsPercentage: Double = 0.0,
    val marginPercentage: Double = 0.0,
    val topProducts: List<TopProductReport> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FinancialAnalysisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: BmpApiService,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tenantId = authRepository.activeTenantId().orEmpty()

    private val _uiState = MutableStateFlow(FinancialReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val defaultDate = sdf.format(Date())
        _uiState.update { it.copy(date = defaultDate, periodType = "MONTHLY") }
        fetchReport()
    }

    fun setPeriodType(type: String) {
        val currentDate = Date()
        val date = when (type) {
            "MONTHLY" -> SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate)
            "QUARTERLY" -> {
                val cal = Calendar.getInstance()
                val q = (cal.get(Calendar.MONTH) / 3) + 1
                "${cal.get(Calendar.YEAR)}-Q$q"
            }
            else -> SimpleDateFormat("yyyy", Locale.getDefault()).format(currentDate)
        }
        _uiState.update { it.copy(periodType = type, date = date) }
        fetchReport()
    }

    fun navigatePeriod(offset: Int) {
        val cal = Calendar.getInstance()
        val state = _uiState.value
        try {
            if (state.periodType == "MONTHLY") {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
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

                _uiState.update {
                    it.copy(
                        periodLabel = body["period"] as? String ?: state.date,
                        omzet = (body["omzet"] as? Number)?.toDouble() ?: 0.0,
                        cogs = (body["cogs"] as? Number)?.toDouble() ?: 0.0,
                        labaKotor = (body["labaKotor"] as? Number)?.toDouble() ?: 0.0,
                        opex = (body["opex"] as? Number)?.toDouble() ?: 0.0,
                        labaBersih = (body["labaBersih"] as? Number)?.toDouble() ?: 0.0,
                        bep = (body["bep"] as? Number)?.toDouble() ?: 0.0,
                        cogsPercentage = (body["cogsPercentage"] as? Number)?.toDouble() ?: 0.0,
                        marginPercentage = (body["marginPercentage"] as? Number)?.toDouble() ?: 0.0,
                        topProducts = topProductsList,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Gagal memuat data dari server.") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Terjadi kesalahan jaringan.") }
        }
    }

    fun exportExcel() = viewModelScope.launch {
        val state = _uiState.value
        Toast.makeText(context, "Mengekspor laporan ke Excel...", Toast.LENGTH_SHORT).show()
        try {
            val resp = api.downloadFinancialReportExcel(state.periodType, state.date)
            if (resp.isSuccessful && resp.body() != null) {
                saveFileToDownloads(resp.body()!!, "Laporan_Keuangan_POSBah_${state.date}.csv")
            } else {
                Toast.makeText(context, "Gagal mengunduh file Excel.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveFileToDownloads(body: ResponseBody, fileName: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
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
            
            Toast.makeText(context, "Laporan disimpan di folder Download:\n$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menyimpan file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun FinancialAnalysisScreen(
    onBack: () -> Unit,
    viewModel: FinancialAnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PosBahTopBar(
                title = "Analisis Keuangan",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.exportExcel() },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                // Summary Metric Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                FinancialStatCard(
                                    title = "Omzet (Kotor)",
                                    value = Formatters.rupiah(state.omzet),
                                    icon = Icons.Outlined.TrendingUp,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FinancialStatCard(
                                    title = "Laba Bersih",
                                    value = Formatters.rupiah(state.labaBersih),
                                    icon = if (state.labaBersih >= 0) Icons.Outlined.MonetizationOn else Icons.Outlined.TrendingDown,
                                    color = if (state.labaBersih >= 0) Color(0xFF3B82F6) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        FinancialStatCard(
                            title = "Break-Even Point (BEP)",
                            value = Formatters.rupiah(state.bep),
                            icon = Icons.Outlined.Shield,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Cost Breakdown Visual representation
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Struktur Keuangan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            // Visual horizontal progress representation
                            val total = state.omzet
                            val cogsRatio = if (total > 0) (state.cogs / total).toFloat() else 0f
                            val opexRatio = if (total > 0) (state.opex / total).toFloat() else 0f
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (cogsRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(cogsRatio)
                                            .background(
                                                color = Color(0xFFEF4444),
                                                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                            )
                                    )
                                }
                                if (opexRatio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(opexRatio)
                                            .background(color = Color(0xFFF59E0B))
                                    )
                                }
                                if (total > 0 && state.labaBersih > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f)
                                            .background(
                                                color = Color(0xFF10B981),
                                                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                            )
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            // Legend
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LegendItem(color = Color(0xFFEF4444), label = "HPP / COGS", value = "${String.format("%.1f", state.cogsPercentage)}%")
                                LegendItem(color = Color(0xFFF59E0B), label = "Beban Usaha / OPEX", value = "${String.format("%.1f", if (total > 0) (state.opex/total)*100 else 0.0)}%")
                                LegendItem(color = Color(0xFF10B981), label = "Margin Bersih", value = "${String.format("%.1f", if (total > 0) (state.labaBersih/total)*100 else 0.0)}%")
                            }
                        }
                    }
                }

                // Financial statement P&L table
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Laporan Laba Rugi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            
                            ReportLine("OMZET PENJUALAN", Formatters.rupiah(state.omzet), isHeader = true)
                            ReportLine("Harga Pokok Penjualan (COGS)", "- ${Formatters.rupiah(state.cogs)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ReportLine("LABA KOTOR (Gross Profit)", Formatters.rupiah(state.labaKotor), isBold = true)
                            ReportLine("Beban Operasional (OPEX)", "- ${Formatters.rupiah(state.opex)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ReportLine(
                                label = "LABA BERSIH (Net Profit)", 
                                value = Formatters.rupiah(state.labaBersih), 
                                isHeader = true,
                                valueColor = if (state.labaBersih >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Top Selling products rank
                if (state.topProducts.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Produk Terlaris",
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp),
                contentColor = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
