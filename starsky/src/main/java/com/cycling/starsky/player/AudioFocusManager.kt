package com.cycling.starsky.player

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.Player

class AudioFocusManager(
    private val audioManager: AudioManager,
    private val onAudioFocusLost: () -> Unit,
    private val onAudioFocusGained: () -> Unit
) {

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var exoPlayer: Player? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                handleAudioFocusGain()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                handleAudioFocusLoss()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                handleAudioFocusLossTransient()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                handleAudioFocusLossTransientCanDuck()
            }
        }
    }

    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(audioFocusChangeListener)
        .build()

    private var wasPlayingWhenLostFocus = false
    private var hasAudioFocus = false

    fun setPlayer(player: Player) {
        this.exoPlayer = player
    }

    fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val result = audioManager.requestAudioFocus(audioFocusRequest)

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    fun abandonAudioFocus() {
        if (!hasAudioFocus) return

        audioManager.abandonAudioFocusRequest(audioFocusRequest)

        hasAudioFocus = false
    }

    private fun handleAudioFocusGain() {
        val player = exoPlayer ?: return

        if (player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED) {
            return
        }

        player.volume = 1f
        if (wasPlayingWhenLostFocus) {
            player.play()
            wasPlayingWhenLostFocus = false
        }

        onAudioFocusGained()
    }

    private fun handleAudioFocusLoss() {
        val player = exoPlayer ?: return

        wasPlayingWhenLostFocus = player.playWhenReady
        player.pause()
        abandonAudioFocus()

        onAudioFocusLost()
    }

    private fun handleAudioFocusLossTransient() {
        val player = exoPlayer ?: return

        wasPlayingWhenLostFocus = player.playWhenReady
        player.pause()
    }

    private fun handleAudioFocusLossTransientCanDuck() {
        val player = exoPlayer ?: return
        player.volume = 0.3f
    }

    fun release() {
        abandonAudioFocus()
        exoPlayer = null
    }
}
