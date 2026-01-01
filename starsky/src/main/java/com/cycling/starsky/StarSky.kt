package com.cycling.starsky

import android.content.Context
import com.cycling.starsky.cache.StarSkyCacheManager
import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import com.cycling.starsky.player.StarSkyPlayer
import com.cycling.starsky.preferences.StarSkyPreferencesManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

object StarSky {

    private var player: StarSkyPlayer? = null
    private lateinit var context: Context
    private val preferencesManager by lazy { StarSkyPreferencesManager(context) }

    fun init(context: Context) {
        this.context = context.applicationContext
        if (player == null) {
            player = StarSkyPlayer(this.context)
        }
    }

    fun getPlayer(): StarSkyPlayer {
        return player ?: throw IllegalStateException("StarSky not initialized. Call init() first.")
    }

    fun play(audioInfo: AudioInfo) {
        getPlayer().play(audioInfo)
    }

    fun playPlaylist(audioList: List<AudioInfo>, startIndex: Int = 0) {
        getPlayer().playPlaylist(audioList, startIndex)
    }

    fun pause() {
        getPlayer().pause()
    }

    fun resume() {
        getPlayer().resume()
    }

    fun stop() {
        getPlayer().stop()
    }

    fun seekTo(position: Long) {
        getPlayer().seekTo(position)
    }

    fun next() {
        getPlayer().next()
    }

    fun previous() {
        getPlayer().previous()
    }

    fun setPlayMode(mode: PlayMode) {
        getPlayer().setPlayMode(mode)
    }

    fun setVolume(volume: Float) {
        getPlayer().setVolume(volume)
    }

    fun getVolume(): Float {
        return getPlayer().getVolume()
    }

    fun setSpeed(speed: Float) {
        getPlayer().setSpeed(speed)
    }

    fun getSpeed(): Float {
        return getPlayer().getSpeed()
    }

    val playbackState: StateFlow<PlaybackState>
        get() = getPlayer().playbackState

    val currentAudio: StateFlow<AudioInfo?>
        get() = getPlayer().currentAudio

    val playMode: StateFlow<PlayMode>
        get() = getPlayer().playMode

    val playbackPosition: StateFlow<Long>
        get() = getPlayer().playbackPosition

    val playbackDuration: StateFlow<Long>
        get() = getPlayer().playbackDuration

    val isPlaying: StateFlow<Boolean>
        get() = getPlayer().isPlaying

    fun getExoPlayer() = getPlayer().getExoPlayer()

    fun getCurrentPlaylist() = getPlayer().getCurrentPlaylist()

    fun getCurrentIndex() = getPlayer().getCurrentIndex()

    fun addListener(listener: OnPlayerEventListener) {
        getPlayer().addListener(listener)
    }

    fun removeListener(listener: OnPlayerEventListener) {
        getPlayer().removeListener(listener)
    }

    fun enableNotification() {
        getPlayer().enableNotification()
    }

    fun disableNotification() {
        getPlayer().disableNotification()
    }

    fun clearCache() {
        StarSkyCacheManager.getInstance(context).clearCache()
    }

    fun getCacheSize(): Long {
        return StarSkyCacheManager.getInstance(context).getCacheSize()
    }

    suspend fun restorePlaybackState() {
        val currentAudio = preferencesManager.getCurrentAudio().first()
        val playlist = preferencesManager.getPlaylist().first()
        val currentIndex = preferencesManager.getCurrentIndex().first()
        val playMode = preferencesManager.getPlayMode().first()
        val volume = preferencesManager.getVolume().first()
        val speed = preferencesManager.getSpeed().first()
        val playbackPosition = preferencesManager.getPlaybackPosition().first()

        if (playlist.isNotEmpty()) {
            getPlayer().playPlaylist(playlist, currentIndex)
            getPlayer().setPlayMode(playMode)
            getPlayer().setVolume(volume)
            getPlayer().setSpeed(speed)
            getPlayer().seekTo(playbackPosition)
        } else if (currentAudio != null) {
            getPlayer().play(currentAudio)
            getPlayer().setPlayMode(playMode)
            getPlayer().setVolume(volume)
            getPlayer().setSpeed(speed)
            getPlayer().seekTo(playbackPosition)
        }
    }

    suspend fun clearPlaybackState() {
        preferencesManager.clearPlaybackState()
    }

    fun release() {
        player?.release()
        player = null
    }
}