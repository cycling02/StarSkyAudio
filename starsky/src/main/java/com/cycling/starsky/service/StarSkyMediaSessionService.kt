package com.cycling.starsky.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.cycling.starsky.R

class StarSkyMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val binder = LocalBinder()

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "starrysky_playback_channel"
        private const val CHANNEL_NAME = "Music Playback"
        var instance: StarSkyMediaSessionService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun setPlayer(player: Player) {
        mediaSession?.release()
        mediaSession = MediaSession.Builder(this, player).apply {
            setCallback(MediaSessionCallback())
        }.build()
        startForegroundNotification()
    }

    fun getMediaSession(): MediaSession? = mediaSession

    fun startForegroundNotification() {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    fun stopForegroundNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Playing")
            .setContentText("StarSky Audio Player")
            .setSmallIcon(R.drawable.ic_music)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        instance = null
        stopForegroundNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    inner class LocalBinder : Binder() {
        fun getService(): StarSkyMediaSessionService = this@StarSkyMediaSessionService
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onPlay() {
            mediaSession?.player?.play()
        }

        override fun onPause() {
            mediaSession?.player?.pause()
        }

        override fun onSeekTo(pos: Long) {
            mediaSession?.player?.seekTo(pos)
        }

        override fun onSkipToNext() {
            mediaSession?.player?.seekToNext()
        }

        override fun onSkipToPrevious() {
            mediaSession?.player?.seekToPrevious()
        }

        override fun onStop() {
            mediaSession?.player?.stop()
        }
    }
}