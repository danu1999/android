package com.posbah.app.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VpsImageUploader {
    private const val TAG = "VpsImageUploader"

    suspend fun uploadLogoToVps(context: Context, bytes: ByteArray, tenantId: String): String? = withContext(Dispatchers.IO) {
        var conn: java.net.HttpURLConnection? = null
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = java.net.URL("https://www.zedmz.cloud/api/upload/logo")
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("X-Tenant-Id", tenantId)

            conn.outputStream.use { os ->
                val writer = os.bufferedWriter(Charsets.UTF_8)
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"tenantId\"\r\n\r\n")
                writer.write("$tenantId\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"logo.png\"\r\n")
                writer.write("Content-Type: image/png\r\n\r\n")
                writer.flush()

                os.write(bytes)
                os.flush()

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(resp)
                if (json.optBoolean("success")) {
                    return@withContext json.optString("url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadLogoToVps error", e)
        } finally {
            conn?.disconnect()
        }
        null
    }

    suspend fun uploadTtdPengirimToVps(context: Context, bytes: ByteArray, tenantId: String, moduleKey: String, docType: String): String? = withContext(Dispatchers.IO) {
        var conn: java.net.HttpURLConnection? = null
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = java.net.URL("https://www.zedmz.cloud/api/upload/ttd-pengirim")
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("X-Tenant-Id", tenantId)

            conn.outputStream.use { os ->
                val writer = os.bufferedWriter(Charsets.UTF_8)
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"tenantId\"\r\n\r\n")
                writer.write("$tenantId\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"moduleKey\"\r\n\r\n")
                writer.write("$moduleKey\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"docType\"\r\n\r\n")
                writer.write("$docType\r\n")

                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"signature.png\"\r\n")
                writer.write("Content-Type: image/png\r\n\r\n")
                writer.flush()

                os.write(bytes)
                os.flush()

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(resp)
                if (json.optBoolean("success")) {
                    return@withContext json.optString("url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadTtdPengirimToVps error", e)
        } finally {
            conn?.disconnect()
        }
        null
    }
}
