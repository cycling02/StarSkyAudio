package com.cycling.starsky.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.cycling.starsky.cache.StarSkyCacheManager
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.toPlaybackState
import com.cycling.starsky.notification.StarSkyNotificationManager
import com.cycling.starsky.service.StarSkyMediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StarSkyPlayer(private val context: Context) {

    private val cacheManager = StarSkyCacheManager.getInstance(context)

    private var exoPlayer: ExoPlayer = createPlayer()

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

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentAudio = MutableStateFlow<AudioInfo?>(null)
    val currentAudio: StateFlow<AudioInfo?> = _currentAudio.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.LOOP)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val playlist = mutableListOf<AudioInfo>()
    private var currentIndex = -1

    private val playerScope = CoroutineScope(Dispatchers.Main + Job())
    private var positionUpdateJob: Job? = null

    private var notificationManager: StarSkyNotificationManager? = null
    private var mediaSession: MediaSession? = null

    private val listeners = mutableSetOf<OnPlayerEventListener>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? StarSkyMediaSessionService.LocalBinder
            binder?.getService()?.let { sessionService ->
                mediaSession = sessionService.getMediaSession()
                mediaSession?.let { session ->
                    sessionService.setPlayer(exoPlayer)
                    notificationManager = StarSkyNotificationManager(context, exoPlayer, session)
                    notificationManager?.startNotification()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaSession = null
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val state = playbackState.toPlayerState().toPlaybackState()
            _playbackState.value = state
            notifyPlaybackStateChanged(state)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
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
                _currentAudio.value = audioInfo
                notifyAudioChanged(audioInfo)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val state = PlaybackState.Error(error.message ?: "Unknown error", error)
            _playbackState.value = state
            notifyError(error.message ?: "Unknown error", error)
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        startPositionUpdate()
    }

    fun play(audioInfo: AudioInfo) {
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

        _currentAudio.value = audioInfo
    }

    fun playPlaylist(audioList: List<AudioInfo>, startIndex: Int = 0) {
        playlist.clear()
        playlist.addAll(audioList)
        currentIndex = startIndex

        val mediaItems = audioList.map { audio ->
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

        _currentAudio.value = audioList.getOrNull(startIndex)
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.play()
    }

    fun stop() {
        exoPlayer.stop()
        _playbackState.value = PlaybackState.Stopped
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun next() {
        if (playlist.isEmpty()) return

        when (_playMode.value) {
            PlayMode.SHUFFLE -> {
                currentIndex = playlist.indices.random()
            }
            else -> {
                currentIndex = (currentIndex + 1) % playlist.size
            }
        }

        exoPlayer.seekToNext()
        _currentAudio.value = playlist.getOrNull(currentIndex)
    }

    fun previous() {
        if (playlist.isEmpty()) return

        when (_playMode.value) {
            PlayMode.SHUFFLE -> {
                currentIndex = playlist.indices.random()
            }
            else -> {
                currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
            }
        }

        exoPlayer.seekToPrevious()
        _currentAudio.value = playlist.getOrNull(currentIndex)
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
        exoPlayer.repeatMode = when (mode) {
            PlayMode.LOOP -> Player.REPEAT_MODE_ALL
            PlayMode.SINGLE_LOOP -> Player.REPEAT_MODE_ONE
            PlayMode.SHUFFLE -> Player.REPEAT_MODE_ALL
        }
        notifyPlayModeChanged(mode)
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f,1f)
    }

    fun getVolume(): Float = exoPlayer.volume

    fun setSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
    }

    fun getSpeed(): Float = exoPlayer.playbackParameters.speed

    fun getExoPlayer(): ExoPlayer = exoPlayer

    fun getCurrentPlaylist(): List<AudioInfo> = playlist.toList()

    fun getCurrentIndex(): Int = currentIndex

    fun addListener(listener: OnPlayerEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnPlayerEventListener) {
        listeners.remove(listener)
    }

    private fun notifyPlaybackStateChanged(state: PlaybackState) {
        listeners.forEach { it.onPlaybackStateChanged(state) }
    }

    private fun notifyAudioChanged(audioInfo: AudioInfo?) {
        listeners.forEach { it.onAudioChanged(audioInfo) }
    }

    private fun notifyPlayProgress(position: Long, duration: Long) {
        listeners.forEach { it.onPlayProgress(position, duration) }
    }

    private fun notifyPlayModeChanged(mode: PlayMode) {
        listeners.forEach { it.onPlayModeChanged(mode) }
    }

    private fun notifyError(message: String, exception: Throwable?) {
        listeners.forEach { it.onError(message, exception) }
    }

    fun enableNotification() {
        val intent = Intent(context, StarSkyMediaSessionService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun disableNotification() {
        notificationManager?.release()
        notificationManager = null
        context.unbindService(serviceConnection)
    }

    private fun startPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = playerScope.launch {
            while (true) {
                delay(100)
                _playbackPosition.value = exoPlayer.currentPosition
                _playbackDuration.value = exoPlayer.duration
                notifyPlayProgress(exoPlayer.currentPosition, exoPlayer.duration)
            }
        }
    }

    fun release() {
        positionUpdateJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        disableNotification()
    }
}