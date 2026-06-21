package com.posbah.app.data.remote

import android.content.Context
import android.util.Log
import com.posbah.app.data.local.PosBahDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object WebSocketSyncClient {
    private const val TAG = "WebSocketSyncClient"
    private const val WS_URL = "wss://www.zedmz.cloud/ws"

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var isManualDisconnect = false
    private var activeTenantId: String? = null
    private var activeContext: Context? = null
    private var activeDb: PosBahDatabase? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class LiveFeedEvent(
        val id: String = java.util.UUID.randomUUID().toString(),
        val type: String,
        val message: String,
        val timestamp: Long
    )

    private val _liveEvents = kotlinx.coroutines.flow.MutableStateFlow<List<LiveFeedEvent>>(emptyList())
    val liveEvents = _liveEvents.asStateFlow()

    private fun addLiveEvent(event: LiveFeedEvent) {
        val current = _liveEvents.value.toMutableList()
        current.add(0, event)
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        _liveEvents.value = current
    }

    @Synchronized
    fun connect(context: Context, tenantId: String, db: PosBahDatabase) {
        if (activeTenantId == tenantId && webSocket != null) {
            Log.d(TAG, "Already connected to WebSocket for tenant: $tenantId")
            return
        }

        disconnect()

        isManualDisconnect = false
        activeTenantId = tenantId
        activeContext = context.applicationContext
        activeDb = db

        if (client == null) {
            client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // Infinite timeout for websockets
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        val requestUrl = "$WS_URL?tenantId=${URLEncoder.encode(tenantId, "UTF-8")}"
        val request = Request.Builder()
            .url(requestUrl)
            .build()

        Log.i(TAG, "Connecting to WebSocket: $requestUrl")
        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connection opened successfully.")
                SupabaseSyncManager.onConnectionStateChanged?.invoke(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket connection closed: $code / $reason")
                if (!isManualDisconnect) {
                    SupabaseSyncManager.onConnectionStateChanged?.invoke(false)
                }
                triggerReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failed: ${t.message}", t)
                SupabaseSyncManager.onConnectionStateChanged?.invoke(false)
                triggerReconnect()
            }
        })
    }

    @Synchronized
    fun disconnect() {
        Log.i(TAG, "Disconnecting WebSocket manually.")
        isManualDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        activeTenantId = null
    }

    private fun handleMessage(text: String) {
        val context = activeContext ?: return
        val db = activeDb ?: return
        val tenantId = activeTenantId ?: return

        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            if (type == "sync_trigger") {
                Log.i(TAG, "Received real-time sync trigger event. Running pulling tasks...")
                scope.launch {
                    try {
                        SupabaseSyncManager.pullAll(context, db, tenantId)
                        SupabaseSyncManager.syncAll(context, db, tenantId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in running WebSocket triggered sync: ${e.message}", e)
                    }
                }
            } else if (type == "live_feed") {
                val event = json.optString("event")
                val msg = json.optString("message")
                val ts = json.optLong("timestamp", System.currentTimeMillis())
                addLiveEvent(LiveFeedEvent(type = event, message = msg, timestamp = ts))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WebSocket message: ${e.message}", e)
        }
    }

    @Synchronized
    private fun triggerReconnect() {
        if (isManualDisconnect) return

        val context = activeContext ?: return
        val tenantId = activeTenantId ?: return
        val db = activeDb ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            Log.i(TAG, "Scheduling auto-reconnect in 5 seconds...")
            delay(5000)
            if (!isManualDisconnect && activeTenantId == tenantId) {
                Log.i(TAG, "Attempting auto-reconnect...")
                connect(context, tenantId, db)
            }
        }
    }
}
