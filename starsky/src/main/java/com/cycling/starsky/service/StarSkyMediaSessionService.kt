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
import androidx.media3.ui.PlayerNotificationManager
import com.cycling.starsky.R
import com.cycling.starsky.notification.StarSkyNotificationManager

class StarSkyMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: Player? = null
    private var notificationManager: StarSkyNotificationManager? = null
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
        startForegroundService()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StarSky Player")
            .setContentText("Playing music")
            .setSmallIcon(R.drawable.ic_music)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun setPlayer(player: Player) {
        this.player = player
        mediaSession?.release()
        
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaSession.Builder(this, player).apply {
            setId("starrysky_media_session")
            setSessionActivity(pendingIntent)
            setCallback(MediaSessionCallback())
        }.build()

        notificationManager?.release()
        notificationManager = StarSkyNotificationManager(this, player, mediaSession!!)
        notificationManager?.startNotification()
    }

    fun getMediaSession(): MediaSession? = mediaSession

    override fun onDestroy() {
        notificationManager?.release()
        notificationManager = null
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
