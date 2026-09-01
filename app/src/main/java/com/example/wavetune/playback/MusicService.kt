package com.example.wavetune.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.wavetune.WaveTuneApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MusicService : Service() {
    private val binder = MusicServiceBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateJob: Job? = null

    inner class MusicServiceBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observePlaybackState()
    }

    private fun observePlaybackState() {
        val app = application as? WaveTuneApp ?: return
        stateJob = serviceScope.launch {
            app.playerEngine.playbackState.collect { state ->
                if (state.currentSong != null) {
                    val notification = buildNotification(state)
                    startForeground(NOTIFICATION_ID, notification)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WaveTune Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WaveTune background audio playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: PlaybackState): Notification {
        val song = state.currentSong

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val prevIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val closeIntent = Intent(this, MusicService::class.java).apply { action = ACTION_CLOSE }
        val closePendingIntent = PendingIntent.getService(
            this,
            4,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val playPauseIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "WaveTune")
            .setContentText(song?.artist ?: "Playing audio")
            .setSubText(song?.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_wavetune_logo))
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, if (state.isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", closePendingIntent)

        // Using standard MediaStyle notification presentation
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
        )

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? WaveTuneApp
        val engine = app?.playerEngine

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> engine?.togglePlayPause()
            ACTION_NEXT -> engine?.playNext()
            ACTION_PREV -> engine?.playPrevious()
            ACTION_CLOSE -> {
                engine?.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stateJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "wavetune_playback"
        const val NOTIFICATION_ID = 101

        const val ACTION_PLAY_PAUSE = "com.example.wavetune.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.wavetune.ACTION_NEXT"
        const val ACTION_PREV = "com.example.wavetune.ACTION_PREV"
        const val ACTION_CLOSE = "com.example.wavetune.ACTION_CLOSE"

        fun start(context: Context) {
            val intent = Intent(context, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
