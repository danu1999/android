@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.posbah.app.ui.screens.bmp.invoices

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.local.entities.ArAgingSummary
import com.posbah.app.data.local.entities.ClientAgingGroup
import com.posbah.app.data.local.entities.InvoiceAgingItem
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.data.repository.BmpArAgingRepository
import com.posbah.app.data.repository.PrintSettingsRepository
import com.posbah.app.ui.components.EmptyState
import com.posbah.app.ui.components.PosBahTopBar
import com.posbah.app.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ArAgingViewModel @Inject constructor(
    private val repo: BmpArAgingRepository,
    private val printSettingsRepo: PrintSettingsRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    val tenantId = authRepo.activeTenantId().orEmpty()

    val summary = repo.summary.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ArAgingSummary()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedBucket = MutableStateFlow("ALL")
    val selectedBucket = _selectedBucket.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun onSearchQueryChange(q: String) {
        _searchQuery.value = q
    }

    fun onBucketChange(bucket: String) {
        _selectedBucket.value = bucket
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchArAging()
            _isLoading.value = false
        }
    }

    suspend fun getBankDetails(): Pair<String, String> {
        val settings = printSettingsRepo.get("invoice")
        val bankName = settings?.bankName ?: ""
        val bankAcc = "${settings?.bankAccountNumber ?: ""} a.n ${settings?.bankOwnerName ?: ""}"
        return Pair(bankName, bankAcc)
    }
}

@Composable
fun ArAgingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInvoiceDetail: (Long) -> Unit = {},
    viewModel: ArAgingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val summary by viewModel.summary.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBucket by viewModel.selectedBucket.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val filteredClients = remember(summary.clients, searchQuery, selectedBucket) {
        summary.clients.filter { client ->
            val matchQuery = client.clientName.contains(searchQuery, ignoreCase = true) ||
                    client.phoneNumber.contains(searchQuery, ignoreCase = true)
            val matchBucket = when (selectedBucket) {
                "CURRENT" -> client.currentAmount > 0
                "DAYS_1_30" -> client.days1To30 > 0
                "DAYS_31_60" -> client.days31To60 > 0
                "DAYS_OVER_60" -> client.daysOver60 > 0
                else -> true
            }
            matchQuery && matchBucket
        }
    }

    Scaffold(
        topBar = {
            PosBahTopBar(
                title = "Umur Piutang Klien (AR Aging)",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // KPI Summary Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Card Grand Total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Piutang Tertagih", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                Formatters.rupiah(summary.totalReceivable),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${summary.clientCount} Klien",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Breakdown Buckets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AgingBucketCard(
                        modifier = Modifier.weight(1f),
                        title = "Lancar",
                        amount = summary.currentAmount,
                        color = Color(0xFF2E7D32),
                        isSelected = selectedBucket == "CURRENT",
                        onClick = { viewModel.onBucketChange(if (selectedBucket == "CURRENT") "ALL" else "CURRENT") }
                    )
                    AgingBucketCard(
                        modifier = Modifier.weight(1f),
                        title = "1-30 Hari",
                        amount = summary.days1To30,
                        color = Color(0xFFF57C00),
                        isSelected = selectedBucket == "DAYS_1_30",
                        onClick = { viewModel.onBucketChange(if (selectedBucket == "DAYS_1_30") "ALL" else "DAYS_1_30") }
                    )
                    AgingBucketCard(
                        modifier = Modifier.weight(1f),
                        title = "31-60 Hari",
                        amount = summary.days31To60,
                        color = Color(0xFFE65100),
                        isSelected = selectedBucket == "DAYS_31_60",
                        onClick = { viewModel.onBucketChange(if (selectedBucket == "DAYS_31_60") "ALL" else "DAYS_31_60") }
                    )
                    AgingBucketCard(
                        modifier = Modifier.weight(1f),
                        title = "> 60 Hari",
                        amount = summary.daysOver60,
                        color = Color(0xFFC62828),
                        isSelected = selectedBucket == "DAYS_OVER_60",
                        onClick = { viewModel.onBucketChange(if (selectedBucket == "DAYS_OVER_60") "ALL" else "DAYS_OVER_60") }
                    )
                }
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Cari nama klien atau nomor telepon...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            if (filteredClients.isEmpty()) {
                EmptyState(
                    title = "Tidak Ada Piutang",
                    description = "Semua invoice lunas atau tidak ada data yang cocok dengan filter."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients, key = { it.clientId }) { client ->
                        ClientAgingCard(
                            client = client,
                            onSendWhatsApp = {
                                viewModel.viewModelScope.launch {
                                    val (bankName, bankAcc) = viewModel.getBankDetails()
                                    sendWhatsAppReminder(context, client, bankName, bankAcc)
                                }
                            },
                            onCopyText = {
                                viewModel.viewModelScope.launch {
                                    val (bankName, bankAcc) = viewModel.getBankDetails()
                                    val text = buildReminderText(client, bankName, bankAcc)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Tagihan", text))
                                    Toast.makeText(context, "Pesan tagihan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onInvoiceClick = { invId -> onNavigateToInvoiceDetail(invId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgingBucketCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.2f) else color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                Formatters.number(amount.toLong()),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ClientAgingCard(
    client: ClientAgingGroup,
    onSendWhatsApp: () -> Unit,
    onCopyText: () -> Unit,
    onInvoiceClick: (Long) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val overdueColor = when {
        client.daysOver60 > 0 -> Color(0xFFC62828)
        client.days31To60 > 0 -> Color(0xFFE65100)
        client.days1To30 > 0 -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }

    val overdueLabel = when {
        client.oldestOverdueDays > 60 -> "Telat ${client.oldestOverdueDays} Hari (>60)"
        client.oldestOverdueDays > 30 -> "Telat ${client.oldestOverdueDays} Hari (31-60)"
        client.oldestOverdueDays > 0 -> "Telat ${client.oldestOverdueDays} Hari (1-30)"
        else -> "Lancar"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        client.clientName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (client.phoneNumber.isNotBlank()) {
                        Text(
                            client.phoneNumber,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = overdueColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        overdueLabel,
                        color = overdueColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Piutang Belum Lunas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.rupiah(client.totalReceivable),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    "${client.invoices.size} Faktur",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSendWhatsApp,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kirim WA", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCopyText,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin Teks", fontSize = 13.sp)
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Detail"
                    )
                }
            }

            // Expandable List of Invoices
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Rincian Faktur Belum Lunas:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    client.invoices.forEach { inv ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInvoiceClick(inv.invoiceId) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "Jatuh Tempo: ${if (inv.dueDate > 0) sdf.format(Date(inv.dueDate)) else "-"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        Formatters.rupiah(inv.remaining),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    if (inv.overdueDays > 0) {
                                        Text(
                                            "Telat ${inv.overdueDays} hari",
                                            fontSize = 10.sp,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold
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

fun buildReminderText(client: ClientAgingGroup, bankName: String, bankAcc: String): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("Yth. Bapak/Ibu ${client.clientName},\n\n")
    sb.append("Kami dari CV. BAHTERA MULYA PLASTIK ingin menginformasikan rincian tagihan faktur yang belum terselesaikan:\n\n")

    client.invoices.forEach { inv ->
        sb.append("• No. Faktur: ${inv.invoiceNumber}\n")
        sb.append("  Total: ${Formatters.rupiah(inv.totalAmount)}\n")
        sb.append("  Sisa Belum Lunas: ${Formatters.rupiah(inv.remaining)}\n")
        if (inv.dueDate > 0) {
            sb.append("  Jatuh Tempo: ${sdf.format(Date(inv.dueDate))}")
            if (inv.overdueDays > 0) {
                sb.append(" (${inv.overdueDays} hari yang lalu)")
            }
            sb.append("\n")
        }
        sb.append("\n")
    }

    sb.append("Total Keseluruhan: ${Formatters.rupiah(client.totalReceivable)}\n\n")

    if (bankAcc.isNotBlank()) {
        sb.append("Pembayaran dapat ditransfer melalui:\n")
        if (bankName.isNotBlank()) sb.append("Bank: $bankName\n")
        sb.append("Rekening: $bankAcc\n\n")
    }

    sb.append("Mohon konfirmasi jika pembayaran telah dilakukan. Terima kasih atas kerja samanya.")
    return sb.toString()
}

fun sendWhatsAppReminder(context: Context, client: ClientAgingGroup, bankName: String, bankAcc: String) {
    try {
        val text = buildReminderText(client, bankName, bankAcc)
        val cleanPhone = client.phoneNumber.replace("[^0-9]".toRegex(), "")
        val formattedPhone = if (cleanPhone.startsWith("0")) {
            "62" + cleanPhone.substring(1)
        } else cleanPhone

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=" + Uri.encode(text))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak dapat membuka WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
