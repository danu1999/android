package com.posbah.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraUtils {

    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val PHOTO_DIR = "Pictures/NotaBahanBaku"
    private const val MEDIASTORE_RELATIVE_PATH = "Pictures/PosBah/NotaBahanBaku"
    private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Buat file temporary untuk kamera (tetap di externalFilesDir untuk staging).
     * Setelah dikompres, panggil persistToMediaStore untuk pindahkan ke storage permanen.
     */
    fun createTempCameraFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), PHOTO_DIR).also { it.mkdirs() }
        val timestamp = DATE_FORMAT.format(Date())
        return File(dir, "NOTA_${timestamp}.jpg")
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file
        )
    }

    /**
     * Pindah file dari externalFilesDir ke MediaStore (storage permanen).
     * Foto akan: survive app uninstall, survive Clear Storage, visible di Gallery.
     */
    fun persistToMediaStore(
        context: Context,
        sourceFile: File,
        tenantId: String
    ): Uri? {
        if (!sourceFile.exists()) return null

        val fileName = "NOTA_${tenantId}_${DATE_FORMAT.format(Date())}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "$MEDIASTORE_RELATIVE_PATH/$tenantId")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            sourceFile.delete()
            uri
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            null
        }
    }

    /** Cek apakah URI MediaStore masih valid (file belum dihapus user dari Gallery). */
    fun isMediaStoreUriValid(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun compressToMaxSize(sourceFile: File, maxSizeKb: Int = 100): File {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return sourceFile

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = false }
        var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options) ?: return sourceFile
        bitmap = correctExifRotation(bitmap, sourceFile.absolutePath)

        val maxBytes = maxSizeKb * 1024L
        val qualities = intArrayOf(90, 80, 70, 50, 35, 20)

        for (quality in qualities) {
            val compressed = compressBitmapToBytes(bitmap, quality)
            if (compressed.size <= maxBytes) {
                FileOutputStream(sourceFile).use { it.write(compressed) }
                bitmap.recycle()
                return sourceFile
            }
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
        bitmap.recycle()

        for (quality in qualities) {
            val compressed = compressBitmapToBytes(scaledBitmap, quality)
            if (compressed.size <= maxBytes) {
                FileOutputStream(sourceFile).use { it.write(compressed) }
                scaledBitmap.recycle()
                return sourceFile
            }
        }

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
            bitmap
        }
    }

    fun deleteSafely(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try { File(filePath).delete() } catch (_: Exception) {}
    }

    /** Hapus dari MediaStore via ContentResolver (untuk removePhoto). */
    fun deleteMediaStoreUri(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) return
        try {
            context.contentResolver.delete(Uri.parse(uriString), null, null)
        } catch (_: Exception) {}
    }

    fun fileSizeKb(context: Context, filePath: String?): Long {
        if (filePath.isNullOrBlank()) return 0L
        if (filePath.startsWith("content://")) {
            return try {
                context.contentResolver.openFileDescriptor(Uri.parse(filePath), "r")?.use { pfd ->
                    (pfd.statSize / 1024).coerceAtLeast(0)
                } ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        return (File(filePath).length() / 1024).coerceAtLeast(0)
    }

    fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = createTempCameraFile(context)
            tempFile.outputStream().use { output ->
                inputStream.use { it.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
