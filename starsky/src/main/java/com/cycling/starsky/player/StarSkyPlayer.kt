package com.cycling.starsky.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.cycling.starsky.cache.StarSkyCacheManager
import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import com.cycling.starsky.preferences.StarSkyPreferencesManager
import com.cycling.starsky.service.StarSkyMediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@androidx.media3.common.util.UnstableApi
class StarSkyPlayer(private val context: Context) {

    private val cacheManager = StarSkyCacheManager.getInstance(context)
    private val preferencesManager = StarSkyPreferencesManager(context)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioFocusManager = AudioFocusManager(
        audioManager = audioManager,
        onAudioFocusLost = { },
        onAudioFocusGained = { }
    )

    private val playlistManager = PlaylistManager()

    private val playerScope = CoroutineScope(Dispatchers.Main + Job())

    private val playbackStateManager = PlaybackStateManager(playerScope)

    private var exoPlayer: ExoPlayer = createPlayer()

    private var mediaSession: MediaSession? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? StarSkyMediaSessionService.LocalBinder
            binder?.getService()?.let { sessionService ->
                mediaSession = sessionService.getMediaSession()
                sessionService.setPlayer(exoPlayer)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaSession = null
        }
    }

    private val playerListener = object : Player.Listener {
        private var currentPlaybackState = Player.STATE_IDLE
        private var currentIsPlaying = false

        override fun onPlaybackStateChanged(playbackState: Int) {
            currentPlaybackState = playbackState
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            currentIsPlaying = isPlaying
            updatePlaybackState()
        }

        private fun updatePlaybackState() {
            val state = when (currentPlaybackState) {
                Player.STATE_IDLE -> PlaybackState.Idle
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> if (currentIsPlaying) PlaybackState.Playing else PlaybackState.Paused
                Player.STATE_ENDED -> PlaybackState.Completed
                else -> PlaybackState.Idle
            }

            playbackStateManager.setPlaybackState(state)
            playbackStateManager.setPlaying(currentIsPlaying)
            playbackStateManager.notifyPlaybackStateChanged(state)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                val audioInfo = AudioInfo(
                    songId = it.mediaId,
                    songUrl = it.localConfiguration?.uri.toString(),
                    songName = it.mediaMetadata.title?.toString() ?: "",
                    artist = it.mediaMetadata.artist?.toString() ?: "",
                    albumName = it.mediaMetadata.albumTitle?.toString() ?: "",
                    coverUrl = it.mediaMetadata.artworkUri?.toString() ?: ""
                )
                playbackStateManager.setCurrentAudio(audioInfo)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val state = PlaybackState.Error(error.message ?: "Unknown error", error)
            playbackStateManager.setPlaybackState(state)
            playbackStateManager.notifyError(error.message ?: "Unknown error", error)
        }
    }

    private fun createPlayer(): ExoPlayer {
        val cacheDataSourceFactory = cacheManager.createCacheDataSource(
            DefaultDataSource.Factory(context)
        )

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory)
            )
            .build()
    }

    init {
        audioFocusManager.setPlayer(exoPlayer)
        exoPlayer.addListener(playerListener)
        playbackStateManager.startPositionUpdate {
            updatePlaybackProgress()
        }
        loadPlayHistory()
    }

    private fun loadPlayHistory() {
        playerScope.launch {
            preferencesManager.getPlayHistory().collect { history ->
                playbackStateManager.loadPlayHistory(history)
            }
        }
    }

    private fun updatePlaybackProgress() {
        playbackStateManager.updatePlaybackPosition(
            exoPlayer.currentPosition,
            exoPlayer.duration
        )
        playbackStateManager.setBufferedPosition(exoPlayer.bufferedPosition)
    }

    val playbackState: StateFlow<PlaybackState>
        get() = playbackStateManager.playbackState

    val currentAudio: StateFlow<AudioInfo?>
        get() = playbackStateManager.currentAudio

    val playMode: StateFlow<PlayMode>
        get() = playbackStateManager.playMode

    val playbackPosition: StateFlow<Long>
        get() = playbackStateManager.playbackPosition

    val playbackDuration: StateFlow<Long>
        get() = playbackStateManager.playbackDuration

    val isPlaying: StateFlow<Boolean>
        get() = playbackStateManager.isPlaying

    val currentPlaylist: StateFlow<List<AudioInfo>>
        get() = playlistManager.currentPlaylist

    val currentIndex: StateFlow<Int>
        get() = playlistManager.currentIndex

    val playHistory: StateFlow<List<AudioInfo>>
        get() = playbackStateManager.playHistory

    fun play(audioInfo: AudioInfo) {
        audioFocusManager.requestAudioFocus()

        val mediaItem = MediaItem.Builder()
            .setUri(audioInfo.songUrl)
            .setMediaId(audioInfo.songId)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(audioInfo.songName)
                    .setArtist(audioInfo.artist)
                    .setAlbumTitle(audioInfo.albumName)
                    .setArtworkUri(audioInfo.coverUrl.takeIf { it.isNotEmpty() }?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        playbackStateManager.setCurrentAudio(audioInfo)

        playerScope.launch {
            preferencesManager.saveCurrentAudio(audioInfo)
        }
    }

    fun playPlaylist(audioList: List<AudioInfo>, startIndex: Int = 0) {
        audioFocusManager.requestAudioFocus()

        playlistManager.setPlaylist(audioList, startIndex)

        val mediaItems = playlistManager.getPlaylist().map { audio ->
            MediaItem.Builder()
                .setUri(audio.songUrl)
                .setMediaId(audio.songId)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(audio.songName)
                        .setArtist(audio.artist)
                        .setAlbumTitle(audio.albumName)
                        .setArtworkUri(audio.coverUrl.takeIf { it.isNotEmpty() }?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()
        }

        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()

        playbackStateManager.setCurrentAudio(playlistManager.getCurrentSong())

        playerScope.launch {
            preferencesManager.savePlaylist(playlistManager.getPlaylist())
            preferencesManager.saveCurrentIndex(playlistManager.getCurrentIndex())
            preferencesManager.saveCurrentAudio(playlistManager.getCurrentSong())
        }
    }

    fun addSongInfo(audioInfo: AudioInfo) {
        playlistManager.addSong(audioInfo)

        val mediaItem = MediaItem.Builder()
            .setUri(audioInfo.songUrl)
            .setMediaId(audioInfo.songId)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(audioInfo.songName)
                    .setArtist(audioInfo.artist)
                    .setAlbumTitle(audioInfo.albumName)
                    .setArtworkUri(audioInfo.coverUrl.takeIf { it.isNotEmpty() }?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()

        exoPlayer.addMediaItem(mediaItem)

        playerScope.launch {
            preferencesManager.savePlaylist(playlistManager.getPlaylist())
        }
    }

    fun addSongInfoAt(audioInfo: AudioInfo, index: Int) {
        playlistManager.addSongAt(audioInfo, index)

        val mediaItem = MediaItem.Builder()
            .setUri(audioInfo.songUrl)
            .setMediaId(audioInfo.songId)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(audioInfo.songName)
                    .setArtist(audioInfo.artist)
                    .setAlbumTitle(audioInfo.albumName)
                    .setArtworkUri(audioInfo.coverUrl.takeIf { it.isNotEmpty() }?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()

        exoPlayer.addMediaItem(index, mediaItem)

        playerScope.launch {
            preferencesManager.savePlaylist(playlistManager.getPlaylist())
        }
    }

    fun removeSongInfo(index: Int) {
        playlistManager.removeSong(index)
        exoPlayer.removeMediaItem(index)

        if (playlistManager.isEmpty()) {
            stop()
            playbackStateManager.setCurrentAudio(null)
        } else {
            playbackStateManager.setCurrentAudio(playlistManager.getCurrentSong())
        }

        playerScope.launch {
            preferencesManager.savePlaylist(playlistManager.getPlaylist())
            preferencesManager.saveCurrentIndex(playlistManager.getCurrentIndex())
            preferencesManager.saveCurrentAudio(playlistManager.getCurrentSong())
        }
    }

    fun clearPlaylist() {
        playlistManager.clearPlaylist()
        exoPlayer.clearMediaItems()
        stop()
        playbackStateManager.setCurrentAudio(null)

        playerScope.launch {
            preferencesManager.savePlaylist(emptyList())
            preferencesManager.saveCurrentIndex(-1)
            preferencesManager.saveCurrentAudio(null)
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.play()
    }

    fun stop() {
        exoPlayer.stop()
        playbackStateManager.setPlaybackState(PlaybackState.Stopped)
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun next() {
        if (playlistManager.isEmpty()) return

        val nextIndex = playlistManager.getNextIndex(playbackStateManager.playMode.value)
        if (nextIndex != null) {
            playlistManager.moveToIndex(nextIndex)
            if (playbackStateManager.playMode.value == PlayMode.SHUFFLE) {
                playlistManager.markAsPlayed(nextIndex)
            }
            exoPlayer.seekToNext()
            playbackStateManager.setCurrentAudio(playlistManager.getCurrentSong())
        } else {
            stop()
        }
    }

    fun previous() {
        if (playlistManager.isEmpty()) return

        val previousIndex = playlistManager.getPreviousIndex(playbackStateManager.playMode.value)
        if (previousIndex != null) {
            playlistManager.moveToIndex(previousIndex)
            exoPlayer.seekToPrevious()
            playbackStateManager.setCurrentAudio(playlistManager.getCurrentSong())
        }
    }

    fun setPlayMode(mode: PlayMode) {
        playbackStateManager.setPlayMode(mode)
        exoPlayer.repeatMode = when (mode) {
            PlayMode.LOOP -> Player.REPEAT_MODE_ALL
            PlayMode.SINGLE_LOOP -> Player.REPEAT_MODE_ONE
            PlayMode.SHUFFLE -> Player.REPEAT_MODE_ALL
            PlayMode.NO_LOOP -> Player.REPEAT_MODE_OFF
        }

        playerScope.launch {
            preferencesManager.savePlayMode(mode)
        }
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)

        playerScope.launch {
            preferencesManager.saveVolume(volume)
        }
    }

    fun getVolume(): Float = exoPlayer.volume

    fun setSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))

        playerScope.launch {
            preferencesManager.saveSpeed(speed)
        }
    }

    fun getSpeed(): Float = exoPlayer.playbackParameters.speed

    fun getExoPlayer(): ExoPlayer = exoPlayer

    fun getCurrentPlaylist(): List<AudioInfo> = playlistManager.getPlaylist()

    fun getCurrentIndex(): Int = playlistManager.getCurrentIndex()

    fun getBufferedPosition(): Long = playbackStateManager.getBufferedPosition()

    fun isBuffering(): Boolean = playbackStateManager.isBuffering()

    fun isCurrMusicIsBuffering(audioInfo: AudioInfo): Boolean {
        return isBuffering() && playbackStateManager.currentAudio.value?.songId == audioInfo.songId
    }

    fun hasNetworkError(): Boolean = playbackStateManager.hasNetworkError()

    fun getPlayHistory(): List<AudioInfo> = playbackStateManager.getPlayHistory()

    fun clearPlayHistory() {
        playbackStateManager.clearPlayHistory()

        playerScope.launch {
            preferencesManager.savePlayHistory(emptyList())
        }
    }

    fun addListener(listener: OnPlayerEventListener) {
        playbackStateManager.addListener(listener)
    }

    fun removeListener(listener: OnPlayerEventListener) {
        playbackStateManager.removeListener(listener)
    }

    fun enableNotification() {
        val intent = Intent(context, StarSkyMediaSessionService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun disableNotification() {
        context.unbindService(serviceConnection)
    }

    fun release() {
        playbackStateManager.release()
        audioFocusManager.release()
        exoPlayer.release()
        mediaSession?.release()
        mediaSession = null
    }
}
