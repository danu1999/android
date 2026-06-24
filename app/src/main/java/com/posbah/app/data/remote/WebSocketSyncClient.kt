package com.posbah.app.data.remote

import android.util.Log

/**
 * WebSocketSyncClient — Full Online mode STUB
 *
 * Dalam full-online mode, WebSocket tidak digunakan.
 * MainActivity masih memanggil disconnect() dan connect() dari kode lama.
 * Semua method adalah no-op.
 *
 * TIDAK HAPUS sampai MainActivity selesai direfactor.
 */
object WebSocketSyncClient {

    private const val TAG = "WebSocketSyncClient"

    fun connect(tenantId: String, onEvent: (String) -> Unit = {}) {
        Log.i(TAG, "connect() — no-op in full-online mode.")
    }

    fun connect(context: android.content.Context, tenantId: String, db: Any?) {
        Log.i(TAG, "connect(context, tenantId, db) — no-op in full-online mode.")
    }

    fun disconnect() {
        Log.i(TAG, "disconnect() — no-op in full-online mode.")
    }

    fun isConnected(): Boolean = false
}
