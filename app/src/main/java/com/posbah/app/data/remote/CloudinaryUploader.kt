package com.posbah.app.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cloudinary Upload Service — 100% tanpa library tambahan (menggunakan HttpURLConnection standar).
 *
 * ⚠️ STATUS: BELUM AKTIF — Diset disabled sampai user mengisi credential Cloudinary.
 *
 * CARA AKTIFKAN (setelah dapat credential dari user):
 * 1. Isi [CLOUD_NAME] dengan Cloud Name dari Cloudinary Dashboard
 * 2. Isi [UPLOAD_PRESET] dengan nama unsigned upload preset
 * 3. Ubah [CLOUDINARY_ENABLED] = true
 *
 * CREDENTIAL YANG DIBUTUHKAN:
 * - Cloud Name : dari Cloudinary Dashboard → atas kiri (contoh: "dxyz123abc")
 * - Upload Preset: Settings → Upload → Upload Presets → "Add upload preset" → Signing Mode: Unsigned
 *
 * CARA BUAT UPLOAD PRESET DI CLOUDINARY:
 * 1. Login ke cloudinary.com
 * 2. Settings (⚙️) → Upload
 * 3. Scroll ke "Upload presets" → klik "Add upload preset"
 * 4. Preset name: "nota_bahan_baku" (bebas, tapi catat)
 * 5. Signing Mode: UNSIGNED (penting! agar bisa upload dari mobile tanpa expose secret)
 * 6. Folder: "posbah/nota_bahan_baku" (opsional, untuk organisasi)
 * 7. Save
 *
 * Alur kerja saat diaktifkan:
 * [BahanBakuFormViewModel.uploadNota()] → [CloudinaryUploader.upload()] → URL Cloudinary
 * → simpan ke [BmpBahanBakuEntity.notaFotoUrl]
 */
object CloudinaryUploader {

    // ── CONFIG — ISI SAAT DAPAT CREDENTIAL ─────────────────────────────────
    private const val CLOUDINARY_ENABLED = false          // ← Ubah ke true setelah isi credential

    private const val CLOUD_NAME = "ISI_CLOUD_NAME_DI_SINI"          // contoh: "dxyz123abc"
    private const val UPLOAD_PRESET = "ISI_UPLOAD_PRESET_DI_SINI"    // contoh: "nota_bahan_baku"
    private const val FOLDER = "posbah/nota_bahan_baku"               // folder di Cloudinary
    // ────────────────────────────────────────────────────────────────────────

    private val UPLOAD_URL get() = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    sealed class UploadResult {
        data class Success(
            val url: String,
            val publicId: String,
            val bytes: Int
        ) : UploadResult()
        data class Error(val message: String) : UploadResult()
        object Disabled : UploadResult()
    }

    /**
     * Upload foto nota ke Cloudinary menggunakan multipart/form-data.
     *
     * @param file File JPEG yang sudah dikompresi (≤100 KB dari [CameraUtils.compressToMaxSize])
     * @param fileName Nama file untuk identifikasi di Cloudinary (contoh: "NOTA_20260609_123456")
     * @return [UploadResult.Success] dengan URL, [UploadResult.Error] jika gagal,
     *         [UploadResult.Disabled] jika belum dikonfigurasi
     */
    suspend fun upload(file: File, fileName: String): UploadResult = withContext(Dispatchers.IO) {
        if (!CLOUDINARY_ENABLED) {
            Log.d("CloudinaryUploader", "Upload dinonaktifkan. Set CLOUDINARY_ENABLED = true setelah isi credential.")
            return@withContext UploadResult.Disabled
        }

        if (CLOUD_NAME == "ISI_CLOUD_NAME_DI_SINI" || UPLOAD_PRESET == "ISI_UPLOAD_PRESET_DI_SINI") {
            return@withContext UploadResult.Error("Cloudinary belum dikonfigurasi. Isi CLOUD_NAME dan UPLOAD_PRESET.")
        }

        if (!file.exists() || file.length() == 0L) {
            return@withContext UploadResult.Error("File tidak ditemukan atau kosong: ${file.path}")
        }

        try {
            val boundary = "------PosBahBoundary${System.currentTimeMillis()}"
            val conn = (URL(UPLOAD_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 60_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            conn.outputStream.use { out ->
                val writer = out.bufferedWriter()

                // Field: upload_preset
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                writer.write("$UPLOAD_PRESET\r\n")

                // Field: folder
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"folder\"\r\n\r\n")
                writer.write("$FOLDER\r\n")

                // Field: public_id (nama file di Cloudinary)
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"public_id\"\r\n\r\n")
                writer.write("$fileName\r\n")

                // Field: file (binary)
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n")
                writer.write("Content-Type: image/jpeg\r\n\r\n")
                writer.flush()

                file.inputStream().use { input -> input.copyTo(out) }

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            }

            if (responseCode !in 200..299) {
                Log.e("CloudinaryUploader", "Upload gagal [$responseCode]: $responseBody")
                return@withContext UploadResult.Error("Upload gagal: HTTP $responseCode")
            }

            // Parse JSON response sederhana (tanpa library tambahan)
            val url = extractJsonString(responseBody, "secure_url")
                ?: return@withContext UploadResult.Error("Gagal parse URL dari response Cloudinary")
            val publicId = extractJsonString(responseBody, "public_id") ?: fileName
            val bytes = extractJsonInt(responseBody, "bytes") ?: file.length().toInt()

            Log.d("CloudinaryUploader", "Upload berhasil: $url ($bytes bytes)")
            UploadResult.Success(url = url, publicId = publicId, bytes = bytes)

        } catch (e: IOException) {
            Log.e("CloudinaryUploader", "IO Error: ${e.message}")
            UploadResult.Error("Koneksi gagal: ${e.message}")
        } catch (e: Exception) {
            Log.e("CloudinaryUploader", "Unexpected error: ${e.message}")
            UploadResult.Error("Error tidak terduga: ${e.message}")
        }
    }

    /** Parse nilai string dari JSON response tanpa library (ringan). */
    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }

    /** Parse nilai int dari JSON response tanpa library (ringan). */
    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\\d+)")
        return pattern.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
}
