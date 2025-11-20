package com.example.task

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.rtspserver.RtspServerDisplay
//import com.pedro.rtspserver.RtspServerDisplay
import java.net.NetworkInterface

class ScreenStreamingService : Service(), ConnectChecker {

    companion object {
        private const val TAG = "ScreenStreamService"
        private const val CHANNEL_ID = "screen_streaming_channel"
        private const val NOTIFICATION_ID = 1
        private const val RTSP_PORT = 1935
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"

        var isStreaming = false
            private set

        var streamUrl: String? = null
            private set
    }

    private var rtspServerDisplay: RtspServerDisplay? = null
    private var resultCode: Int = 0
    private var data: Intent? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                data = intent.getParcelableExtra(EXTRA_DATA)
                startStreaming()
            }
            ACTION_STOP -> {
                stopStreaming()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startStreaming() {
        if (isStreaming) {
            Log.d(TAG, "Already streaming")
            return
        }

        startForeground(NOTIFICATION_ID, createNotification("Starting stream..."))

        try {
            // Initialize RTSP Server Display
            rtspServerDisplay = RtspServerDisplay(
                this,
                true,
                this,
                RTSP_PORT
            ).apply {
                setIntentResult(resultCode, data)
            }

            // Prepare video and audio
            val prepared = rtspServerDisplay?.prepareVideo(
                1280, // width
                720,  // height
                30,   // fps
                2500 * 1024, // bitrate (2.5 Mbps)
                0,    // rotation
                320   // dpi
            ) ?: false

            val audioPrepared = rtspServerDisplay?.prepareAudio(
                44100,
                128 * 1024,
                true,
            ) ?: false

            if (prepared && audioPrepared) {
                // Start the RTSP server
                rtspServerDisplay?.startStream()
                isStreaming = true

                // Get local IP address
                val ipAddress = getLocalIpAddress()
                streamUrl = "rtsp://$ipAddress:$RTSP_PORT/live"

                updateNotification("Streaming at: $streamUrl")
                Log.d(TAG, "Stream started: $streamUrl")
            } else {
                Log.e(TAG, "Failed to prepare stream")
                updateNotification("Failed to start stream")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting stream", e)
            updateNotification("Error: ${e.message}")
        }
    }

    private fun stopStreaming() {
        Log.d(TAG, "Stopping stream")
        rtspServerDisplay?.stopStream()
        isStreaming = false
        streamUrl = null
        updateNotification("Stream stopped")
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress
                        // Check for IPv4
                        if (hostAddress != null && hostAddress.indexOf(':') < 0) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
        }
        return "Unknown"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when screen is being streamed"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): android.app.Notification {
        val stopIntent = Intent(this, ScreenStreamingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mirroring")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(message))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
        Log.d(TAG, "Service destroyed")
    }

    // ConnectChecker callbacks
    override fun onConnectionStarted(url: String) {
        Log.d(TAG, "Connection started: $url")
    }

    override fun onConnectionSuccess() {
        Log.d(TAG, "Connection successful")
    }

    override fun onConnectionFailed(reason: String) {
        Log.e(TAG, "Connection failed: $reason")
        updateNotification("Connection failed: $reason")
    }

    override fun onNewBitrate(bitrate: Long) {
        Log.d(TAG, "New bitrate: $bitrate")
    }

    override fun onDisconnect() {
        Log.d(TAG, "Disconnected")
    }

    override fun onAuthError() {
        Log.e(TAG, "Auth error")
    }

    override fun onAuthSuccess() {
        Log.d(TAG, "Auth success")
    }
}