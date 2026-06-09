package com.posbah.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    initialSignatureBase64: String? = null,
    onSignatureSaved: (String?) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val currentStroke = remember { mutableStateListOf<Offset>() }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // If there is an initial signature, we indicate it so they know it is saved.
    var hasInitial by remember { mutableStateOf(initialSignatureBase64 != null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .onGloballyPositioned { size = it.size }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            hasInitial = false
                            currentStroke.clear()
                            currentStroke.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke.add(change.position)
                        },
                        onDragEnd = {
                            strokes.add(currentStroke.toList())
                            currentStroke.clear()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (hasInitial && initialSignatureBase64 != null) {
                Text(
                    text = "Tanda Tangan Tersimpan\n(Sentuh/gambar untuk mengganti)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            } else {
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    for (stroke in strokes) {
                        for (i in 0 until stroke.size - 1) {
                            drawLine(
                                color = Color.Black,
                                start = stroke[i],
                                end = stroke[i + 1],
                                strokeWidth = 6f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    for (i in 0 until currentStroke.size - 1) {
                        drawLine(
                            color = Color.Black,
                            start = currentStroke[i],
                            end = currentStroke[i + 1],
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PrimaryButton(
                label = "Hapus",
                variant = ButtonVariant.Outline,
                onClick = {
                    strokes.clear()
                    currentStroke.clear()
                    hasInitial = false
                    onSignatureSaved(null)
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            PrimaryButton(
                label = "Simpan TTD",
                variant = ButtonVariant.Filled,
                onClick = {
                    if (strokes.isEmpty() && !hasInitial) {
                        onSignatureSaved(null)
                    } else if (hasInitial) {
                        onSignatureSaved(initialSignatureBase64)
                    } else {
                        // Export to Base64 PNG
                        val width = if (size.width > 0) size.width else 400
                        val height = if (size.height > 0) size.height else 180
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(AndroidColor.WHITE)

                        val paint = Paint().apply {
                            color = AndroidColor.BLACK
                            strokeWidth = 6f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            isAntiAlias = true
                        }

                        for (stroke in strokes) {
                            for (i in 0 until stroke.size - 1) {
                                canvas.drawLine(
                                    stroke[i].x, stroke[i].y,
                                    stroke[i + 1].x, stroke[i + 1].y,
                                    paint
                                )
                            }
                        }

                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        val byteArray = outputStream.toByteArray()
                        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        val base64DataUri = "data:image/png;base64,$base64"
                        
                        hasInitial = true
                        onSignatureSaved(base64DataUri)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
