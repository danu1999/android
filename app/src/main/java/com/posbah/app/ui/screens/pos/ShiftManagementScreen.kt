package com.posbah.app.ui.screens.pos

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.CashierShiftData
import com.posbah.app.data.repository.CashierShiftRepository
import com.posbah.app.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val shiftRepo: CashierShiftRepository,
    private val securePrefs: SecurePreferences
) : ViewModel() {

    private val _activeShift = MutableStateFlow<CashierShiftData?>(null)
    val activeShift = _activeShift.asStateFlow()

    private val _shiftHistory = MutableStateFlow<List<CashierShiftData>>(emptyList())
    val shiftHistory = _shiftHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val empId = securePrefs.employeeId ?: 0L
            _activeShift.value = shiftRepo.checkActiveShift(empId)
            _shiftHistory.value = shiftRepo.listShifts(empId)
            _isLoading.value = false
        }
    }

    fun openShift(startCash: Double, outletId: Long?) {
        viewModelScope.launch {
            _isLoading.value = true
            val empId = securePrefs.employeeId ?: 0L
            val result = shiftRepo.openShift(empId, startCash, outletId)
            _message.value = if (result != null) {
                _activeShift.value = result
                "✅ Shift berhasil dibuka! Selamat bekerja."
            } else "❌ Gagal membuka shift. Coba lagi."
            _isLoading.value = false
            if (result != null) loadData()
        }
    }

    fun closeShift(shiftId: Long, actualEndCash: Double, expectedEndCash: Double, notes: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = shiftRepo.closeShift(shiftId, actualEndCash, expectedEndCash, notes)
            _message.value = if (success) {
                _activeShift.value = null
                "✅ Shift ditutup. Terima kasih!"
            } else "❌ Gagal menutup shift. Coba lagi."
            _isLoading.value = false
            if (success) loadData()
        }
    }

    fun clearMessage() { _message.value = null }
}

// ── UI Colors ────────────────────────────────────────────────────────────────

private val ShiftGreen = Color(0xFF1B5E20)
private val ShiftGreenLight = Color(0xFF2E7D32)
private val ShiftAmber = Color(0xFFF57F17)
private val ShiftRed = Color(0xFFB71C1C)
private val ShiftBgDark = Color(0xFF0D1B12)
private val ShiftCardDark = Color(0xFF1A2E1F)
private val ShiftCardMedium = Color(0xFF22392A)

private val shiftIdrFmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
private val shiftDtFmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id"))

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ShiftManagementScreen(
    onBack: () -> Unit,
    vm: ShiftViewModel = hiltViewModel()
) {
    val activeShift by vm.activeShift.collectAsState()
    val history by vm.shiftHistory.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()

    var showOpenDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) { delay(3000); vm.clearMessage() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(ShiftBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // AppBar
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(ShiftGreen, ShiftGreenLight)))
                    .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                    Column {
                        Text("Manajemen Shift", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Buka & Tutup Sesi Kasir", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ShiftGreenLight)
                        }
                    }
                }

                message?.let { msg ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (msg.startsWith("✅")) ShiftGreen.copy(alpha = 0.3f) else ShiftRed.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (msg.startsWith("✅")) ShiftGreenLight else ShiftRed)
                        ) {
                            Text(msg, modifier = Modifier.padding(16.dp), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                item {
                    ActiveShiftCard(
                        shift = activeShift,
                        onOpenShift = { showOpenDialog = true },
                        onCloseShift = { showCloseDialog = true }
                    )
                }

                if (history.isNotEmpty()) {
                    item {
                        Text(
                            "Riwayat Shift",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(history.take(20)) { shift -> ShiftHistoryCard(shift) }
                }
            }
        }

        if (showOpenDialog) {
            OpenShiftDialog(
                onDismiss = { showOpenDialog = false },
                onConfirm = { startCash -> vm.openShift(startCash, null); showOpenDialog = false }
            )
        }

        activeShift?.let { shift ->
            if (showCloseDialog) {
                CloseShiftDialog(
                    shift = shift,
                    onDismiss = { showCloseDialog = false },
                    onConfirm = { actualCash, notes ->
                        vm.closeShift(shift.id, actualCash, shift.expectedEndCash, notes)
                        showCloseDialog = false
                    }
                )
            }
        }
    }
}

// ── Active Shift Card ─────────────────────────────────────────────────────────

@Composable
private fun ActiveShiftCard(
    shift: CashierShiftData?,
    onOpenShift: () -> Unit,
    onCloseShift: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ShiftCardDark,
        border = BorderStroke(1.dp,
            if (shift != null) ShiftGreenLight.copy(alpha = 0.5f)
            else Color.White.copy(alpha = 0.1f))
    ) {
        if (shift == null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(ShiftCardMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LockOpen, null, tint = ShiftAmber, modifier = Modifier.size(28.dp))
                }
                Text("Shift Belum Dibuka", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                Text(
                    "Buka shift terlebih dahulu sebelum mulai melayani transaksi. Input kas awal untuk memulai.",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center
                )
                Button(
                    onClick = onOpenShift,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftGreenLight),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Shift Sekarang", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(ShiftGreen.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LockOpen, null, tint = ShiftGreenLight, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Shift Sedang Aktif", color = ShiftGreenLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Sejak: ${shiftDtFmt.format(Date(shift.openedAt))}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(20.dp), color = ShiftGreen.copy(alpha = 0.3f)) {
                        Text("OPEN", color = ShiftGreenLight, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ShiftStatItem("Kas Awal", shiftIdrFmt.format(shift.startCash))
                    ShiftStatItem("Ekspektasi Akhir", shiftIdrFmt.format(shift.expectedEndCash))
                }

                Button(
                    onClick = onCloseShift,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShiftRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tutup Shift", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShiftStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ── Shift History Card ────────────────────────────────────────────────────────

@Composable
private fun ShiftHistoryCard(shift: CashierShiftData) {
    val isClosed = shift.status == "CLOSED"
    val diff = shift.cashDifference
    val diffColor = when {
        diff > 500 -> Color(0xFF66BB6A)
        diff < -500 -> ShiftRed
        else -> Color.White.copy(alpha = 0.5f)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ShiftCardMedium,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(if (isClosed) ShiftCardDark else ShiftGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isClosed) Icons.Default.Lock else Icons.Default.LockOpen,
                    null,
                    tint = if (isClosed) Color.White.copy(alpha = 0.3f) else ShiftGreenLight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(shiftDtFmt.format(Date(shift.openedAt)), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                if (isClosed && shift.closedAt != null) {
                    Text("Tutup: ${shiftDtFmt.format(Date(shift.closedAt))}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Text("Kas Awal: ${shiftIdrFmt.format(shift.startCash)}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            if (isClosed) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Selisih", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    Text(
                        "${if (diff >= 0) "+" else ""}${shiftIdrFmt.format(diff)}",
                        color = diffColor, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            } else {
                Surface(shape = RoundedCornerShape(8.dp), color = ShiftGreen.copy(alpha = 0.25f)) {
                    Text("OPEN", color = ShiftGreenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun OpenShiftDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var kasAwal by remember { mutableStateOf("") }
    val parsed = kasAwal.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ShiftCardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, tint = ShiftGreenLight)
                Spacer(Modifier.width(8.dp))
                Text("Buka Shift Kasir", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Masukkan jumlah uang kas awal yang ada di laci kasir saat ini.",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp
                )
                OutlinedTextField(
                    value = kasAwal,
                    onValueChange = { kasAwal = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Kas Awal (Rp)", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ShiftGreenLight,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (kasAwal.isNotBlank()) {
                    Text("= ${shiftIdrFmt.format(parsed)}", color = ShiftGreenLight,
                        fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(parsed) },
                colors = ButtonDefaults.buttonColors(containerColor = ShiftGreenLight),
                enabled = kasAwal.isNotBlank()
            ) { Text("Buka Shift", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

@Composable
private fun CloseShiftDialog(
    shift: CashierShiftData,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var kasAkhir by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    val actualCash = kasAkhir.toDoubleOrNull() ?: 0.0
    val expectedCash = shift.expectedEndCash
    val diff = actualCash - expectedCash
    val diffColor = when {
        diff > 500 -> Color(0xFF66BB6A)
        diff < -500 -> ShiftRed
        else -> Color.White.copy(alpha = 0.5f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ShiftCardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stop, null, tint = ShiftRed)
                Spacer(Modifier.width(8.dp))
                Text("Tutup Shift Kasir", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = ShiftCardMedium) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Kas Awal", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            Text(shiftIdrFmt.format(shift.startCash), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Ekspektasi Akhir", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            Text(shiftIdrFmt.format(expectedCash), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = kasAkhir,
                    onValueChange = { kasAkhir = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Kas Akhir Aktual (Rp)", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ShiftGreenLight,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (kasAkhir.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (diff >= 0) ShiftGreen.copy(alpha = 0.15f) else ShiftRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, diffColor.copy(alpha = 0.5f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Selisih Kas", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            Text(
                                "${if (diff >= 0) "+" else ""}${shiftIdrFmt.format(diff)}",
                                color = diffColor, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan (opsional)", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ShiftGreenLight,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(actualCash, catatan.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = ShiftRed),
                enabled = kasAkhir.isNotBlank()
            ) { Text("Tutup Shift", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}
