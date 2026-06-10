package com.posbah.app.ui.screens.bmp.qr

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.posbah.app.data.local.dao.TenantDao
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.ui.components.PosBahTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject

data class QrScannerUiState(
    val scannedSessionId: String? = null,
    val isConfirming: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tenantDao: TenantDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState = _uiState.asStateFlow()

    fun onQrScanned(sessionId: String) {
        if (_uiState.value.scannedSessionId == null && !_uiState.value.isConfirming && !_uiState.value.success) {
            _uiState.update { it.copy(scannedSessionId = sessionId) }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(scannedSessionId = null) }
    }

    fun confirmLogin(onComplete: (Boolean, String?) -> Unit) {
        val sessionId = _uiState.value.scannedSessionId ?: return
        _uiState.update { it.copy(isConfirming = true, errorMessage = null) }
        
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            val tenantId = authRepository.activeTenantId()
            if (user == null || tenantId == null) {
                val err = "Sesi aktif tidak ditemukan di perangkat."
                _uiState.update { it.copy(isConfirming = false, errorMessage = err) }
                onComplete(false, err)
                return@launch
            }

            val tenant = tenantDao.getById(tenantId)
            val businessMode = tenant?.businessMode ?: "BMP"

            val success = confirmQrLoginOnServer(sessionId, user, businessMode)
            if (success) {
                _uiState.update { it.copy(isConfirming = false, success = true, scannedSessionId = null) }
                onComplete(true, null)
            } else {
                val err = "Gagal memproses login QR ke server. Periksa koneksi internet Anda."
                _uiState.update { it.copy(isConfirming = false, errorMessage = err) }
                onComplete(false, err)
            }
        }
    }

    private suspend fun confirmQrLoginOnServer(
        sessionId: String,
        user: com.posbah.app.data.local.entities.LocalUser,
        businessMode: String
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://www.zedmz.cloud/api/auth/qr-confirm")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json")
            }

            val userObj = JSONObject().apply {
                put("id", user.googleSub)
                put("name", user.displayName ?: user.email.split("@")[0])
                put("email", user.email)
                put("role", user.role)
                put("isDemo", !user.isPremium)
                put("businessMode", businessMode)
                put("tenantId", user.tenantId ?: "")
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                put("registeredAt", sdf.format(Date(user.registeredAt)))
            }

            val body = JSONObject().apply {
                put("sessionId", sessionId)
                put("user", userObj)
            }

            conn.outputStream.use { out ->
                out.bufferedWriter().use { it.write(body.toString()) }
            }

            val code = conn.responseCode
            Log.d("QrScannerViewModel", "Server response code: $code")
            code in 200..299
        } catch (e: Exception) {
            Log.e("QrScannerViewModel", "Error posting QR confirmation", e)
            false
        } finally {
            conn?.disconnect()
        }
    }
}

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { qrValue ->
                            onQrCodeScanned(qrValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Fail silently
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ✅ FIX 1: PreviewView di-remember di luar AndroidView agar referensinya
    // stabil dan langsung tersedia di LaunchedEffect tanpa race condition
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // ✅ FIX 2: Shutdown executor saat composable dispose
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    // ✅ FIX 3: Gunakan addListener callback (bukan blocking .get())
    // agar tidak terjadi deadlock di main thread
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = future.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrContent ->
                viewModel.onQrScanned(qrContent)
            })

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("QrScannerScreen", "Gagal bind kamera", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Scaffold(
        topBar = {
            PosBahTopBar(
                title = "Pindai QR Code Web",
                subtitle = "Login instan ke browser laptop",
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Akses kamera diperlukan untuk memindai QR Code.",
                    color = Color.White,
                    modifier = Modifier.padding(24.dp)
                )
            }

            // Viewfinder overlay — sudut-sudut kotak bidik
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.Center)
            ) {
                val stroke = 4.dp.toPx()
                val corner = 36.dp.toPx()
                val w = size.width
                val h = size.height
                val c = androidx.compose.ui.graphics.Color.White
                // Kiri atas
                drawLine(c, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(corner, 0f), stroke)
                drawLine(c, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, corner), stroke)
                // Kanan atas
                drawLine(c, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w - corner, 0f), stroke)
                drawLine(c, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w, corner), stroke)
                // Kiri bawah
                drawLine(c, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(corner, h), stroke)
                drawLine(c, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(0f, h - corner), stroke)
                // Kanan bawah
                drawLine(c, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w - corner, h), stroke)
                drawLine(c, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w, h - corner), stroke)
            }

            Text(
                text = "Arahkan kamera ke QR Code di layar laptop Anda",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isConfirming) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (uiState.scannedSessionId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text("Konfirmasi Login") },
            text = { Text("Apakah Anda ingin masuk ke POSBah Web di laptop?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmLogin { success, errorMsg ->
                            if (success) {
                                Toast.makeText(context, "Login berhasil dikonfirmasi!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, errorMsg ?: "Gagal mengonfirmasi login.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Ya, Masuk")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Batal")
                }
            }
        )
    }
}
