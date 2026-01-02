package com.cycling.starsky.control

import android.content.Context
import com.cycling.starsky.config.StarSkyConfig
import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import com.cycling.starsky.player.StarSkyPlayer
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

@androidx.media3.common.util.UnstableApi
class PlayerControlImpl(private val context: Context, private val config: StarSkyConfig = StarSkyConfig()) : PlayerControl {

    private val player: StarSkyPlayer by lazy { StarSkyPlayer(context) }

    override fun play(audioInfo: AudioInfo) {
        player.play(audioInfo)
    }

    override fun playPlaylist(audioList: List<AudioInfo>, startIndex: Int) {
        player.playPlaylist(audioList, startIndex)
    }

    override fun addSongInfo(audioInfo: AudioInfo) {
        player.addSongInfo(audioInfo)
    }

    override fun addSongInfoAt(audioInfo: AudioInfo, index: Int) {
        player.addSongInfoAt(audioInfo, index)
    }

    override fun removeSongInfo(index: Int) {
        player.removeSongInfo(index)
    }

    override fun clearPlaylist() {
        player.clearPlaylist()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.resume()
    }

    override fun stop() {
        player.stop()
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
    }

    override fun next() {
        player.next()
    }

    override fun previous() {
        player.previous()
    }

    override fun setPlayMode(mode: PlayMode) {
        player.setPlayMode(mode)
    }

    override fun setVolume(volume: Float) {
        player.setVolume(volume)
    }

    override fun getVolume(): Float {
        return player.getVolume()
    }

    override fun setSpeed(speed: Float) {
        player.setSpeed(speed)
    }

    override fun getSpeed(): Float {
        return player.getSpeed()
    }

    override fun getExoPlayer(): Player {
        return player.getExoPlayer()
    }

    override fun getCurrentPlaylist(): List<AudioInfo> {
        return player.getCurrentPlaylist()
    }

    override fun getCurrentIndex(): Int {
        return player.getCurrentIndex()
    }

    override fun getBufferedPosition(): Long {
        return player.getBufferedPosition()
    }

    override fun isBuffering(): Boolean {
        return player.isBuffering()
    }

    override fun isCurrMusicIsBuffering(audioInfo: AudioInfo): Boolean {
        return player.isCurrMusicIsBuffering(audioInfo)
    }

    override fun hasNetworkError(): Boolean {
        return player.hasNetworkError()
    }

    override fun getPlayHistory(): List<AudioInfo> {
        return player.getPlayHistory()
    }

    override fun clearPlayHistory() {
        player.clearPlayHistory()
    }

    override val playbackState: StateFlow<PlaybackState>
        get() = player.playbackState

    override val currentAudio: StateFlow<AudioInfo?>
        get() = player.currentAudio

    override val playMode: StateFlow<PlayMode>
        get() = player.playMode

    override val playbackPosition: StateFlow<Long>
        get() = player.playbackPosition

    override val playbackDuration: StateFlow<Long>
        get() = player.playbackDuration

    override val isPlaying: StateFlow<Boolean>
        get() = player.isPlaying

    override val currentPlaylist: StateFlow<List<AudioInfo>>
        get() = player.currentPlaylist

    override val currentIndex: StateFlow<Int>
        get() = player.currentIndex

    override val playHistory: StateFlow<List<AudioInfo>>
        get() = player.playHistory

    override fun addListener(listener: OnPlayerEventListener) {
        player.addListener(listener)
    }

    override fun removeListener(listener: OnPlayerEventListener) {
        player.removeListener(listener)
    }

    override fun enableNotification() {
        player.enableNotification()
    }

    override fun disableNotification() {
        player.disableNotification()
    }

    override fun release() {
        player.release()
    }
}
