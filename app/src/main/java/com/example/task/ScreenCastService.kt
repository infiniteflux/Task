package com.example.task

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import com.pedro.common.ConnectChecker
import com.pedro.library.generic.GenericStream
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource

class ScreenCastService : Service(), ConnectChecker {

    companion object {
        private const val TAG = "ScreenCastService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ScreenCastChannel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        const val RTSP_PORT = 8554
    }

    private var genericStream: GenericStream? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode == -1 && data != null) {
                    startForeground(NOTIFICATION_ID, createNotification("Initializing..."))
                    startStreaming(resultCode, data)
                }
            }
            ACTION_STOP -> {
                stopStreaming()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startStreaming(resultCode: Int, data: Intent) {
        try {
            val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            val mediaProjection: MediaProjection? = mediaProjectionManager.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null, cannot start streaming.")
                sendBroadcast(Intent("STREAMING_FAILED").apply {
                    putExtra("reason", "MediaProjection could not be acquired.")
                })
                stopSelf()
                return
            }
            //
            val screenSource = ScreenSource(applicationContext, mediaProjection, null)
            val microphoneSource = MicrophoneSource()

            genericStream = GenericStream(applicationContext, this, screenSource, microphoneSource).apply {

                getGlInterface().setForceRender(true, 15)

                val videoPrepared = prepareVideo(
                    width = 1280,
                    height = 720,
                    bitrate = 2500 * 1024,
                    fps = 30,
                    iFrameInterval = 2,
                    rotation = 0
                )

                val audioPrepared = prepareAudio(
                    bitrate = 128 * 1024,
                    sampleRate = 44100,
                    isStereo = true,
                    echoCanceler = true,
                    noiseSuppressor = true
                )

                if (videoPrepared && audioPrepared) {
                    val rtspUrl = "rtsp://localhost:$RTSP_PORT"
                    startStream(rtspUrl)

                    sendBroadcast(Intent("STREAMING_STARTED"))
                    updateNotification("Streaming Active")
                    Log.i(TAG, "Streaming started successfully on: $rtspUrl")
                } else {
                    Log.e(TAG, "Failed to prepare video/audio. Video: $videoPrepared, Audio: $audioPrepared")
                    sendBroadcast(Intent("STREAMING_FAILED").apply {
                        putExtra("reason", "Failed to prepare encoder")
                    })
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting stream", e)
            sendBroadcast(Intent("STREAMING_FAILED").apply {
                putExtra("reason", e.message ?: "Unknown error")
            })
            stopSelf()
        }
    }

    private fun stopStreaming() {
        genericStream?.let {
            if (it.isStreaming) {
                it.stopStream()
            }
        }
        genericStream?.release()
        genericStream = null
        sendBroadcast(Intent("STREAMING_STOPPED"))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Cast Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen casting is active"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Cast")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
    }

    // ConnectChecker callbacks
    override fun onAuthError() {
        Log.e(TAG, "Auth error")
        sendBroadcast(Intent("STREAMING_FAILED").apply {
            putExtra("reason", "Authentication error")
        })
    }

    override fun onAuthSuccess() {
        Log.i(TAG, "Auth success")
    }

    override fun onConnectionFailed(reason: String) {
        Log.e(TAG, "Connection failed: $reason")
        sendBroadcast(Intent("STREAMING_FAILED").apply {
            putExtra("reason", reason)
        })
    }

    override fun onConnectionStarted(url: String) {
        Log.i(TAG, "Connection started: $url")
        updateNotification("Streaming to: $url")
    }

    override fun onConnectionSuccess() {
        Log.i(TAG, "Connection success")
        updateNotification("Connected successfully")
    }

    override fun onDisconnect() {
        Log.i(TAG, "Disconnected")
        sendBroadcast(Intent("STREAMING_STOPPED"))
    }

    override fun onNewBitrate(bitrate: Long) {
        Log.d(TAG, "New bitrate: $bitrate")
    }
}