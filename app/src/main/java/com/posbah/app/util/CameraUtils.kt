package com.posbah.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitas untuk kamera dan kompresi foto nota bahan baku.
 *
 * Alur kerja:
 * 1. [createTempCameraFile] → buat file kosong di direktori Pictures/NotaBahanBaku
 * 2. [getFileProviderUri] → konversi file ke URI yang aman untuk intent kamera
 * 3. [compressToMaxSize] → kompres hasil foto ke ≤ [maxSizeKb] KB (default 100 KB)
 *
 * Catatan arsitektur:
 * - File foto disimpan di getExternalFilesDir(Pictures/NotaBahanBaku) — tidak butuh READ_EXTERNAL_STORAGE
 * - Kompresi menggunakan iterasi progressif (kualitas 90 → 80 → 70 → 50 → 30) sampai ≤ 100 KB
 * - File dirotasi berdasarkan EXIF orientation agar tampil benar di semua perangkat
 */
object CameraUtils {

    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val PHOTO_DIR = "Pictures/NotaBahanBaku"
    private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Buat file kosong untuk target foto kamera.
     * @return File di [getExternalFilesDir]/Pictures/NotaBahanBaku/NOTA_yyyyMMdd_HHmmss.jpg
     */
    fun createTempCameraFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), PHOTO_DIR).also { it.mkdirs() }
        val timestamp = DATE_FORMAT.format(Date())
        return File(dir, "NOTA_${timestamp}.jpg")
    }

    /**
     * Konversi File ke URI menggunakan FileProvider (aman untuk Android 7+).
     */
    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file
        )
    }

    /**
     * Kompres file foto hingga ukurannya ≤ [maxSizeKb] KB.
     *
     * Algoritma:
     * - Decode bitmap dari [sourceFile]
     * - Koreksi orientasi berdasarkan EXIF (penting untuk foto portrait/landscape)
     * - Iterasi kompresi JPEG dengan penurunan kualitas bertahap
     * - Jika kualitas minimum tidak cukup → scale down resolusi 50% dan ulangi
     * - Hasil ditulis kembali ke [sourceFile] (in-place, hemat storage)
     *
     * @return File yang sudah dikompresi (sama dengan [sourceFile])
     */
    fun compressToMaxSize(sourceFile: File, maxSizeKb: Int = 100): File {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return sourceFile

        // Decode bitmap
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = false }
        var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)
            ?: return sourceFile

        // Koreksi EXIF rotation
        bitmap = correctExifRotation(bitmap, sourceFile.absolutePath)

        val maxBytes = maxSizeKb * 1024L
        val qualities = intArrayOf(90, 80, 70, 50, 35, 20)

        // Iterasi kualitas
        for (quality in qualities) {
            val compressed = compressBitmapToBytes(bitmap, quality)
            if (compressed.size <= maxBytes) {
                FileOutputStream(sourceFile).use { it.write(compressed) }
                bitmap.recycle()
                return sourceFile
            }
        }

        // Masih terlalu besar → scale down 50% dan ulangi sekali
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmap.width / 2,
            bitmap.height / 2,
            true
        )
        bitmap.recycle()

        for (quality in qualities) {
            val compressed = compressBitmapToBytes(scaledBitmap, quality)
            if (compressed.size <= maxBytes) {
                FileOutputStream(sourceFile).use { it.write(compressed) }
                scaledBitmap.recycle()
                return sourceFile
            }
        }

        // Final fallback: tulis dengan kualitas 20 apapun hasilnya
        val finalBytes = compressBitmapToBytes(scaledBitmap, 20)
        FileOutputStream(sourceFile).use { it.write(finalBytes) }
        scaledBitmap.recycle()
        return sourceFile
    }

    private fun compressBitmapToBytes(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Koreksi rotasi berdasarkan EXIF metadata.
     * Banyak kamera Android menyimpan foto landscape meski orientasi portrait — EXIF yang menyimpan rotasinya.
     */
    private fun correctExifRotation(bitmap: Bitmap, filePath: String): Bitmap {
        return try {
            val exif = androidx.exifinterface.media.ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            val rotation = when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation == 0f) return bitmap

            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            bitmap // jika gagal baca EXIF, kembalikan bitmap asli
        }
    }

    /** Hapus file foto sementara dari storage (saat user batalkan atau error). */
    fun deleteSafely(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try { File(filePath).delete() } catch (_: Exception) {}
    }

    /** Ukuran file dalam KB. */
    fun fileSizeKb(filePath: String?): Long {
        if (filePath.isNullOrBlank()) return 0L
        return (File(filePath).length() / 1024).coerceAtLeast(0)
    }
}
