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
    private var player: Player? = null
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
        this.player = player
        mediaSession?.release()
        mediaSession = MediaSession.Builder(this, player).apply {
            setCallback(MediaSessionCallback())
        }.build()
    }

    fun getMediaSession(): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        instance = null
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

    private inner class MediaSessionCallback : MediaSession.Callback
}