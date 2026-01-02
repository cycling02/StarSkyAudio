package com.cycling.starsky

import android.content.Context
import com.cycling.starsky.cache.StarSkyCacheManager
import com.cycling.starsky.config.StarSkyConfig
import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import com.cycling.starsky.control.PlayerControl
import com.cycling.starsky.control.PlayerControlImpl
import com.cycling.starsky.player.StarSkyPlayer
import com.cycling.starsky.preferences.StarSkyPreferencesManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object StarSky {

    private var playerControl: PlayerControl? = null
    private lateinit var context: Context
    private val preferencesManager by lazy { StarSkyPreferencesManager(context) }
    private var initialized = false
    private val configBuilder = StarSkyConfig.Builder()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun init(application: android.app.Application): StarSky {
        if (initialized) {
            throw IllegalStateException("StarSky already initialized")
        }
        this.context = application.applicationContext
        initialized = true
        return this
    }

    fun setOpenCache(open: Boolean): StarSky {
        configBuilder.setOpenCache(open)
        return this
    }

    fun setNotificationEnabled(enabled: Boolean): StarSky {
        configBuilder.setNotificationEnabled(enabled)
        return this
    }

    fun setAutoPlay(autoPlay: Boolean): StarSky {
        configBuilder.setAutoPlay(autoPlay)
        return this
    }

    fun setRestoreState(restore: Boolean): StarSky {
        configBuilder.setRestoreState(restore)
        return this
    }

    fun apply(): StarSky {
        if (!initialized) {
            throw IllegalStateException("StarSky not initialized. Call init() first.")
        }
        
        if (playerControl == null) {
            val config = configBuilder.build()
            playerControl = PlayerControlImpl(this.context, config)
        }
        
        if (configBuilder.build().notificationEnabled) {
            playerControl?.enableNotification()
        } else {
            playerControl?.disableNotification()
        }
        
        if (configBuilder.build().restoreState) {
            playerControl?.let {
                scope.launch {
                    restorePlaybackState()
                }
            }
        }
        
        return this
    }

    fun with(context: Context): PlayerControl {
        if (!initialized) {
            throw IllegalStateException("StarSky not initialized. Call init() in Application class first.")
        }
        return playerControl ?: throw IllegalStateException("PlayerControl not initialized")
    }

    fun play(audioInfo: AudioInfo) {
        playerControl?.play(audioInfo)
    }

    fun playPlaylist(audioList: List<AudioInfo>, startIndex: Int = 0) {
        playerControl?.playPlaylist(audioList, startIndex)
    }

    fun addSongInfo(audioInfo: AudioInfo) {
        playerControl?.addSongInfo(audioInfo)
    }

    fun addSongInfoAt(audioInfo: AudioInfo, index: Int) {
        playerControl?.addSongInfoAt(audioInfo, index)
    }

    fun removeSongInfo(index: Int) {
        playerControl?.removeSongInfo(index)
    }

    fun clearPlaylist() {
        playerControl?.clearPlaylist()
    }

    fun pause() {
        playerControl?.pause()
    }

    fun resume() {
        playerControl?.resume()
    }

    fun stop() {
        playerControl?.stop()
    }

    fun seekTo(position: Long) {
        playerControl?.seekTo(position)
    }

    fun next() {
        playerControl?.next()
    }

    fun previous() {
        playerControl?.previous()
    }

    fun setPlayMode(mode: PlayMode) {
        playerControl?.setPlayMode(mode)
    }

    fun setVolume(volume: Float) {
        playerControl?.setVolume(volume)
    }

    fun getVolume(): Float {
        return playerControl?.getVolume() ?: 1.0f
    }

    fun setSpeed(speed: Float) {
        playerControl?.setSpeed(speed)
    }

    fun getSpeed(): Float {
        return playerControl?.getSpeed() ?: 1.0f
    }

    val playbackState: StateFlow<PlaybackState>
        get() = playerControl?.playbackState ?: throw IllegalStateException("StarSky not initialized")

    val currentAudio: StateFlow<AudioInfo?>
        get() = playerControl?.currentAudio ?: throw IllegalStateException("StarSky not initialized")

    val playMode: StateFlow<PlayMode>
        get() = playerControl?.playMode ?: throw IllegalStateException("StarSky not initialized")

    val playbackPosition: StateFlow<Long>
        get() = playerControl?.playbackPosition ?: throw IllegalStateException("StarSky not initialized")

    val playbackDuration: StateFlow<Long>
        get() = playerControl?.playbackDuration ?: throw IllegalStateException("StarSky not initialized")

    val isPlaying: StateFlow<Boolean>
        get() = playerControl?.isPlaying ?: throw IllegalStateException("StarSky not initialized")

    val currentPlaylist: StateFlow<List<AudioInfo>>
        get() = playerControl?.currentPlaylist ?: throw IllegalStateException("StarSky not initialized")

    val currentIndex: StateFlow<Int>
        get() = playerControl?.currentIndex ?: throw IllegalStateException("StarSky not initialized")

    val playHistory: StateFlow<List<AudioInfo>>
        get() = playerControl?.playHistory ?: throw IllegalStateException("StarSky not initialized")

    fun getExoPlayer() = playerControl?.getExoPlayer() ?: throw IllegalStateException("StarSky not initialized")

    fun getCurrentPlaylist() = playerControl?.getCurrentPlaylist() ?: emptyList()

    fun getCurrentIndex() = playerControl?.getCurrentIndex() ?: -1

    fun getPlayHistory() = playerControl?.getPlayHistory() ?: emptyList()

    fun clearPlayHistory() {
        playerControl?.clearPlayHistory()
    }

    fun getBufferedPosition() = playerControl?.getBufferedPosition() ?: 0L

    fun isBuffering() = playerControl?.isBuffering() ?: false

    fun isCurrMusicIsBuffering(audioInfo: AudioInfo) = playerControl?.isCurrMusicIsBuffering(audioInfo) ?: false

    fun hasNetworkError() = playerControl?.hasNetworkError() ?: false

    fun addListener(listener: OnPlayerEventListener) {
        playerControl?.addListener(listener)
    }

    fun removeListener(listener: OnPlayerEventListener) {
        playerControl?.removeListener(listener)
    }

    fun enableNotification() {
        playerControl?.enableNotification()
    }

    fun disableNotification() {
        playerControl?.disableNotification()
    }

    fun clearCache() {
        StarSkyCacheManager.getInstance(context).clearCache()
    }

    fun getCacheSize(): Long {
        return StarSkyCacheManager.getInstance(context).getCacheSize()
    }

    suspend fun restorePlaybackState() {
        val config = configBuilder.build()
        val currentAudio = preferencesManager.getCurrentAudio().first()
        val playlist = preferencesManager.getPlaylist().first()
        val currentIndex = preferencesManager.getCurrentIndex().first()
        val playMode = preferencesManager.getPlayMode().first()
        val volume = preferencesManager.getVolume().first()
        val speed = preferencesManager.getSpeed().first()
        val playbackPosition = preferencesManager.getPlaybackPosition().first()

        if (playlist.isNotEmpty()) {
            playerControl?.playPlaylist(playlist, currentIndex)
            playerControl?.setPlayMode(playMode)
            playerControl?.setVolume(volume)
            playerControl?.setSpeed(speed)
            playerControl?.seekTo(playbackPosition)
            if (!config.autoPlay) {
                playerControl?.pause()
            }
        } else if (currentAudio != null) {
            playerControl?.play(currentAudio)
            playerControl?.setPlayMode(playMode)
            playerControl?.setVolume(volume)
            playerControl?.setSpeed(speed)
            playerControl?.seekTo(playbackPosition)
            if (!config.autoPlay) {
                playerControl?.pause()
            }
        }
    }

    suspend fun clearPlaybackState() {
        preferencesManager.clearPlaybackState()
    }

    fun release() {
        playerControl?.release()
        playerControl = null
    }
}