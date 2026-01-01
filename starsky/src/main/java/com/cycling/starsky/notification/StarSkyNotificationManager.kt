package com.cycling.starsky.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.cycling.starsky.R

@OptIn(UnstableApi::class)
class StarSkyNotificationManager(
    private val context: Context,
    private val player: Player,
    private val mediaSession: MediaSession
) {

    private var playerNotificationManager: PlayerNotificationManager? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "starrysky_playback_channel"
        private const val CHANNEL_NAME = "Music Playback"
    }

    fun startNotification() {
        createNotificationChannel()

        playerNotificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setMediaDescriptionAdapter(DescriptionAdapter(context))
            .setNotificationListener(NotificationListener())
            .build()

        playerNotificationManager?.setPlayer(player)
    }

    fun stopNotification() {
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
    }

    fun release() {
        stopNotification()
    }

    private fun createNotificationChannel() {
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

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private class DescriptionAdapter(private val context: Context) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence {
            return player.currentMediaItem?.mediaMetadata?.title ?: "Unknown"
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return player.currentMediaItem?.mediaMetadata?.artist
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): android.graphics.Bitmap? {
            return null
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private class NotificationListener : PlayerNotificationManager.NotificationListener {
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        }

        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
        }
    }
}