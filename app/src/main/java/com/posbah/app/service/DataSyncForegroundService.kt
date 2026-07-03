package com.posbah.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.posbah.app.MainActivity
import com.posbah.app.R

class DataSyncForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "posbah_data_sync_channel"
        const val CHANNEL_NAME = "Sinkronisasi Data POSBah"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SYNC = "ACTION_START_SYNC"
        const val ACTION_UPDATE_PROGRESS = "ACTION_UPDATE_PROGRESS"
        const val ACTION_STOP_SYNC = "ACTION_STOP_SYNC"

        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        const val EXTRA_MAX_PROGRESS = "EXTRA_MAX_PROGRESS"
        const val EXTRA_INDETERMINATE = "EXTRA_INDETERMINATE"

        fun startSync(context: Context, initialTitle: String = "Menyinkronkan Data", initialMessage: String = "Menghubungkan ke server...") {
            val intent = Intent(context, DataSyncForegroundService::class.java).apply {
                action = ACTION_START_SYNC
                putExtra(EXTRA_TITLE, initialTitle)
                putExtra(EXTRA_MESSAGE, initialMessage)
                putExtra(EXTRA_INDETERMINATE, true)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateProgress(
            context: Context,
            title: String,
            message: String,
            progress: Int,
            maxProgress: Int,
            indeterminate: Boolean = false
        ) {
            val intent = Intent(context, DataSyncForegroundService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_MAX_PROGRESS, maxProgress)
                putExtra(EXTRA_INDETERMINATE, indeterminate)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopSync(context: Context, successMessage: String? = "Sinkronisasi Selesai") {
            val intent = Intent(context, DataSyncForegroundService::class.java).apply {
                action = ACTION_STOP_SYNC
                putExtra(EXTRA_MESSAGE, successMessage)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_SYNC -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Menyinkronkan Data"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Memulai pengunduhan..."
                val notification = buildNotification(title, message, 0, 100, true)
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to normal notification if foreground start is restricted
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE_PROGRESS -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Menyinkronkan Data"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Proses unduh..."
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                val maxProgress = intent.getIntExtra(EXTRA_MAX_PROGRESS, 100)
                val indeterminate = intent.getBooleanExtra(EXTRA_INDETERMINATE, false)

                val notification = buildNotification(title, message, progress, maxProgress, indeterminate)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP_SYNC -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE)
                if (!message.isNullOrBlank()) {
                    val doneNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("POSBah — Sinkronisasi Selesai")
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(NOTIFICATION_ID + 1, doneNotification)
                }
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (e: Exception) {
                    notificationManager.cancel(NOTIFICATION_ID)
                }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi status progress sinkronisasi data POSBah"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        message: String,
        progress: Int,
        maxProgress: Int,
        indeterminate: Boolean
    ): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(maxProgress, progress, indeterminate)
            .setContentIntent(pendingIntent)
            .build()
    }
}

