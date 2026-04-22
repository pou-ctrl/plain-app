package com.ismartcoding.plain.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.MainActivity

class TunnelService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        LogCat.d("TunnelService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogCat.d("TunnelService started")

        // Start tunnel if not running
        if (!TunnelManager.isTunnelRunning) {
            if (!TunnelManager.startTunnel(this)) {
                LogCat.e("Failed to start tunnel in service")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        TunnelManager.stopTunnel()
        LogCat.d("TunnelService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Remote Access",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Cloudflare Tunnel Service"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Remote Access Active")
            .setContentText("https://app.shakti.buzz")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tunnel_service_channel"
    }
}