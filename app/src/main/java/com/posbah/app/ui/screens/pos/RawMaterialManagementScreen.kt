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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.RawMaterialData
import com.posbah.app.data.repository.RawMaterialRepository
import com.posbah.app.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class RawMaterialViewModel @Inject constructor(
    private val rawMaterialRepo: RawMaterialRepository,
    private val securePrefs: SecurePreferences
) : ViewModel() {

    private val _rawMaterials = MutableStateFlow<List<RawMaterialData>>(emptyList())
    val rawMaterials = _rawMaterials.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _rawMaterials.value = rawMaterialRepo.list()
            _isLoading.value = false
        }
    }

    fun create(name: String, purchaseUnit: String, recipeUnit: String,
               conversionRate: Double, initialStock: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val id = rawMaterialRepo.create(name, purchaseUnit, recipeUnit, conversionRate, initialStock)
            _message.value = if (id > 0) "✅ Bahan baku \"$name\" berhasil ditambahkan."
            else "❌ Gagal menambahkan bahan baku."
            loadData()
        }
    }

    fun addStock(rawMaterial: RawMaterialData, addedAmount: Double) {
        viewModelScope.launch {
            val newStock = rawMaterial.stock + addedAmount
            val ok = rawMaterialRepo.update(rawMaterial.id, stock = newStock)
            _message.value = if (ok) "✅ Stok \"${rawMaterial.name}\" berhasil ditambah."
            else "❌ Gagal update stok."
            loadData()
        }
    }

    fun delete(id: Long, name: String) {
        viewModelScope.launch {
            val ok = rawMaterialRepo.delete(id)
            _message.value = if (ok) "✅ \"$name\" dihapus." else "❌ Gagal menghapus bahan baku."
            loadData()
        }
    }

    fun clearMessage() { _message.value = null }
}

// ── UI Colors ─────────────────────────────────────────────────────────────────
private val RmGreen = Color(0xFF1B5E20)
private val RmGreenLight = Color(0xFF2E7D32)
private val RmAmber = Color(0xFFF57F17)
private val RmRed = Color(0xFFB71C1C)
private val RmBgDark = Color(0xFF0D1B12)
private val RmCardDark = Color(0xFF1A2E1F)
private val RmCardMedium = Color(0xFF22392A)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawMaterialManagementScreen(
    onBack: () -> Unit,
    vm: RawMaterialViewModel = hiltViewModel()
) {
    val rawMaterials by vm.rawMaterials.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val message by vm.message.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var addStockTarget by remember { mutableStateOf<RawMaterialData?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearMessage()
        }
    }

    val filtered = rawMaterials.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }

    val lowStockCount = rawMaterials.count { mat ->
        mat.minStock > 0 && mat.stock <= mat.minStock
    }

    Box(Modifier.fillMaxSize().background(RmBgDark)) {
        Column(Modifier.fillMaxSize()) {
            // AppBar
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(RmGreen, Color(0xFF388E3C))))
                    .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Stok Bahan Baku", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${rawMaterials.size} bahan baku terdaftar", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    if (lowStockCount > 0) {
                        Surface(shape = RoundedCornerShape(20.dp), color = RmAmber.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, RmAmber)) {
                            Text(
                                "⚠️ $lowStockCount stok rendah",
                                color = RmAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.size(40.dp),
                        containerColor = Color.White,
                        contentColor = RmGreen
                    ) { Icon(Icons.Default.Add, null) }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("Cari bahan baku...", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RmGreenLight,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = RmCardDark,
                    unfocusedContainerColor = RmCardDark
                )
            )

            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RmGreenLight)
                }
            }

            message?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (msg.startsWith("✅")) RmGreen.copy(alpha = 0.3f) else RmRed.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (msg.startsWith("✅")) RmGreenLight else RmRed)
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filtered.isEmpty() && !isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inventory2, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Belum ada bahan baku", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                                Text("Tap tombol + untuk menambahkan", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                items(filtered, key = { it.id }) { mat ->
                    RawMaterialCard(
                        rawMaterial = mat,
                        onAddStock = { addStockTarget = mat },
                        onDelete = { vm.delete(mat.id, mat.name) }
                    )
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            AddRawMaterialDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, pUnit, rUnit, conv, stock ->
                    vm.create(name, pUnit, rUnit, conv, stock)
                    showAddDialog = false
                }
            )
        }

        // Add Stock Dialog
        addStockTarget?.let { mat ->
            AddStockDialog(
                rawMaterial = mat,
                onDismiss = { addStockTarget = null },
                onConfirm = { amount ->
                    vm.addStock(mat, amount)
                    addStockTarget = null
                }
            )
        }
    }
}

// ── Raw Material Card ─────────────────────────────────────────────────────────

@Composable
private fun RawMaterialCard(
    rawMaterial: RawMaterialData,
    onAddStock: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = rawMaterial.minStock > 0 && rawMaterial.stock <= rawMaterial.minStock
    val stockColor = when {
        rawMaterial.stock <= 0 -> RmRed
        isLowStock -> RmAmber
        else -> Color(0xFF66BB6A)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RmCardDark,
        border = BorderStroke(1.dp, if (isLowStock) RmAmber.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(RmCardMedium),
                contentAlignment = Alignment.Center
            ) {
                Text(rawMaterial.name.take(1).uppercase(), color = RmGreenLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(rawMaterial.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Satuan konversi
                Text(
                    "1 ${rawMaterial.purchaseUnit} = ${formatQty(rawMaterial.conversionRate)} ${rawMaterial.recipeUnit}",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = stockColor.copy(alpha = 0.15f)) {
                        Text(
                            "${formatQty(rawMaterial.stock)} ${rawMaterial.recipeUnit}",
                            color = stockColor, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (isLowStock) {
                        Spacer(Modifier.width(6.dp))
                        Text("⚠️ Stok Rendah", color = RmAmber, fontSize = 10.sp)
                    }
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onAddStock, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.AddCircle, null, tint = RmGreenLight)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun formatQty(qty: Double): String {
    return if (qty == qty.toLong().toDouble()) qty.toLong().toString()
    else "%.2f".format(qty).trimEnd('0').trimEnd('.')
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun AddRawMaterialDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var purchaseUnit by remember { mutableStateOf("kg") }
    var recipeUnit by remember { mutableStateOf("gram") }
    var conversionRate by remember { mutableStateOf("1000") }
    var initialStock by remember { mutableStateOf("0") }

    // Common unit pairs suggestions
    val unitPairs = listOf(
        Pair("kg", "gram"), Pair("liter", "ml"), Pair("dus", "pcs"),
        Pair("karung", "gram"), Pair("bungkus", "gram"), Pair("buah", "buah")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RmCardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = RmGreenLight)
                Spacer(Modifier.width(8.dp))
                Text("Tambah Bahan Baku", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RmInputField("Nama Bahan Baku", name) { name = it }

                Text("Satuan Konversi:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)

                // Quick pair buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    unitPairs.take(3).forEach { (pu, ru) ->
                        FilterChip(
                            selected = purchaseUnit == pu && recipeUnit == ru,
                            onClick = {
                                purchaseUnit = pu; recipeUnit = ru
                                conversionRate = when(pu) { "kg" -> "1000"; "liter" -> "1000"; else -> "1" }
                            },
                            label = { Text("$pu/$ru", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RmGreenLight,
                                selectedLabelColor = Color.White,
                                containerColor = RmCardMedium,
                                labelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RmInputField("Satuan Beli", purchaseUnit, Modifier.weight(1f)) { purchaseUnit = it }
                    RmInputField("Satuan Pakai", recipeUnit, Modifier.weight(1f)) { recipeUnit = it }
                }

                RmInputField("Konversi (1 $purchaseUnit = ? $recipeUnit)", conversionRate, keyboardType = KeyboardType.Decimal) {
                    conversionRate = it.filter { c -> c.isDigit() || c == '.' }
                }
                Text("Contoh: 1 kg = 1000 gram → isi 1000", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)

                RmInputField("Stok Awal ($recipeUnit)", initialStock, keyboardType = KeyboardType.Decimal) {
                    initialStock = it.filter { c -> c.isDigit() || c == '.' }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.trim(),
                        purchaseUnit.trim(),
                        recipeUnit.trim(),
                        conversionRate.toDoubleOrNull() ?: 1.0,
                        initialStock.toDoubleOrNull() ?: 0.0
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RmGreenLight),
                enabled = name.isNotBlank()
            ) { Text("Simpan", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Color.White.copy(alpha = 0.5f)) }
        }
    )
}

@Composable
private fun AddStockDialog(
    rawMaterial: RawMaterialData,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var addAmount by remember { mutableStateOf("") }
    val addAmountDouble = addAmount.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RmCardDark,
        title = {
            Text("Tambah Stok", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Bahan: ${rawMaterial.name}", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                Text("Stok saat ini: ${formatQty(rawMaterial.stock)} ${rawMaterial.recipeUnit}", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                Text("Satuan beli: ${rawMaterial.purchaseUnit} | 1 ${rawMaterial.purchaseUnit} = ${formatQty(rawMaterial.conversionRate)} ${rawMaterial.recipeUnit}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)

                RmInputField(
                    "Jumlah Ditambah (${rawMaterial.recipeUnit})",
                    addAmount,
                    keyboardType = KeyboardType.Decimal
                ) { addAmount = it.filter { c -> c.isDigit() || c == '.' } }

                if (addAmountDouble > 0) {
                    val inPurchaseUnit = addAmountDouble / rawMaterial.conversionRate
                    Text(
                        "= ${formatQty(inPurchaseUnit)} ${rawMaterial.purchaseUnit}",
                        color = RmGreenLight, fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Stok baru: ${formatQty(rawMaterial.stock + addAmountDouble)} ${rawMaterial.recipeUnit}",
                        color = Color(0xFF66BB6A), fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(addAmountDouble) },
                colors = ButtonDefaults.buttonColors(containerColor = RmGreenLight),
                enabled = addAmountDouble > 0
            ) { Text("Tambah Stok", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Color.White.copy(alpha = 0.5f)) }
        }
    )
}

@Composable
private fun RmInputField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RmGreenLight,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
