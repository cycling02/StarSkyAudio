package com.cycling.starsky.control

import android.content.Context
import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

interface PlayerControl {

    val playbackState: StateFlow<PlaybackState>

    val currentAudio: StateFlow<AudioInfo?>

    val playMode: StateFlow<PlayMode>

    val playbackPosition: StateFlow<Long>

    val playbackDuration: StateFlow<Long>

    val isPlaying: StateFlow<Boolean>

    val currentPlaylist: StateFlow<List<AudioInfo>>

    val currentIndex: StateFlow<Int>

    val playHistory: StateFlow<List<AudioInfo>>

    fun play(audioInfo: AudioInfo)

    fun playPlaylist(audioList: List<AudioInfo>, startIndex: Int = 0)

    fun addSongInfo(audioInfo: AudioInfo)

    fun addSongInfoAt(audioInfo: AudioInfo, index: Int)

    fun removeSongInfo(index: Int)

    fun clearPlaylist()

    fun pause()

    fun resume()

    fun stop()

    fun seekTo(position: Long)

    fun next()

    fun previous()

    fun setPlayMode(mode: PlayMode)

    fun setVolume(volume: Float)

    fun getVolume(): Float

    fun setSpeed(speed: Float)

    fun getSpeed(): Float

    fun getExoPlayer(): Player

    fun getCurrentPlaylist(): List<AudioInfo>

    fun getCurrentIndex(): Int

    fun getBufferedPosition(): Long

    fun isBuffering(): Boolean

    fun isCurrMusicIsBuffering(audioInfo: AudioInfo): Boolean

    fun hasNetworkError(): Boolean

    fun getPlayHistory(): List<AudioInfo>

    fun clearPlayHistory()

    fun addListener(listener: OnPlayerEventListener)

    fun removeListener(listener: OnPlayerEventListener)

    fun enableNotification()

    fun disableNotification()

    fun release()
}
