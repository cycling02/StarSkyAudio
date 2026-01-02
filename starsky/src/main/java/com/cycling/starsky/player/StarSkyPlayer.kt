package com.cycling.starsky.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
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
import com.cycling.starsky.preferences.StarSkyPreferencesManager
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
    private val preferencesManager = StarSkyPreferencesManager(context)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (exoPlayer.playbackState == Player.STATE_IDLE || 
                    exoPlayer.playbackState == Player.STATE_ENDED) {
                    return@OnAudioFocusChangeListener
                }
                exoPlayer.volume = 1f
                if (wasPlayingWhenLostFocus) {
                    exoPlayer.play()
                    wasPlayingWhenLostFocus = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingWhenLostFocus = exoPlayer.playWhenReady
                exoPlayer.pause()
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingWhenLostFocus = exoPlayer.playWhenReady
                exoPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                exoPlayer.volume = 0.3f
            }
        }
    }

    private val audioFocusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    } else {
        null
    }

    private var wasPlayingWhenLostFocus = false
    private var hasAudioFocus = false

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

    private val _currentPlaylist = MutableStateFlow<List<AudioInfo>>(emptyList())
    val currentPlaylist: StateFlow<List<AudioInfo>> = _currentPlaylist.asStateFlow()

    private val _currentIndexFlow = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndexFlow.asStateFlow()

    private val _playHistory = MutableStateFlow<List<AudioInfo>>(emptyList())
    val playHistory: StateFlow<List<AudioInfo>> = _playHistory.asStateFlow()

    private val playlist = mutableListOf<AudioInfo>()
    private var currentSongIndex = -1
    private val playedIndices = mutableSetOf<Int>()
    private val playHistoryList = mutableListOf<AudioInfo>()

    private val playerScope = CoroutineScope(Dispatchers.Main + Job())
    private var positionUpdateJob: Job? = null
    private var lastPositionSaveTime = 0L

    private var lastBufferedPosition = 0L
    private var networkError = false
    private var bufferingStartTime = 0L
    private var isCurrentlyBuffering = false

    private var mediaSession: MediaSession? = null

    private val listeners = mutableSetOf<OnPlayerEventListener>()

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
            _playbackState.value = state
            _isPlaying.value = currentIsPlaying
            notifyPlaybackStateChanged(state)

            if (currentPlaybackState == Player.STATE_ENDED) {
                playerScope.launch {
                    delay(500)
                    next()
                }
            }
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
                addToPlayHistory(audioInfo)
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
        loadPlayHistory()
    }

    private fun loadPlayHistory() {
        playerScope.launch {
            preferencesManager.getPlayHistory().collect { history ->
                playHistoryList.clear()
                playHistoryList.addAll(history)
                _playHistory.value = playHistoryList.toList()
            }
        }
    }

    private fun addToPlayHistory(audioInfo: AudioInfo) {
        playHistoryList.removeIf { it.songId == audioInfo.songId }
        playHistoryList.add(0, audioInfo)
        if (playHistoryList.size > 100) {
            playHistoryList.removeAt(playHistoryList.size - 1)
        }
        _playHistory.value = playHistoryList.toList()

        playerScope.launch {
            preferencesManager.savePlayHistory(playHistoryList)
        }
    }

    fun play(audioInfo: AudioInfo) {
        requestAudioFocus()

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
        addToPlayHistory(audioInfo)

        playerScope.launch {
            preferencesManager.saveCurrentAudio(audioInfo)
        }
    }

    fun playPlaylist(audioList: List<AudioInfo>, startIndex: Int = 0) {
        requestAudioFocus()

        playlist.clear()
        val uniqueAudioList = audioList.distinctBy { it.songId }
        playlist.addAll(uniqueAudioList)
        currentSongIndex = startIndex.coerceAtMost(uniqueAudioList.size - 1)
        _currentPlaylist.value = playlist.toList()
        _currentIndexFlow.value = currentSongIndex

        val mediaItems = uniqueAudioList.map { audio ->
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

        exoPlayer.setMediaItems(mediaItems, currentSongIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()

        _currentAudio.value = uniqueAudioList.getOrNull(currentSongIndex)
        uniqueAudioList.getOrNull(currentSongIndex)?.let { addToPlayHistory(it) }

        playerScope.launch {
            preferencesManager.savePlaylist(playlist)
            preferencesManager.saveCurrentIndex(currentSongIndex)
            preferencesManager.saveCurrentAudio(uniqueAudioList.getOrNull(currentSongIndex))
        }
    }

    fun addSongInfo(audioInfo: AudioInfo) {
        if (playlist.any { it.songId == audioInfo.songId }) {
            return
        }

        playlist.add(audioInfo)

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

        _currentPlaylist.value = playlist.toList()

        playerScope.launch {
            preferencesManager.savePlaylist(playlist)
        }
    }

    fun addSongInfoAt(audioInfo: AudioInfo, index: Int) {
        if (index < 0 || index > playlist.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for playlist size ${playlist.size}")
        }

        if (playlist.any { it.songId == audioInfo.songId }) {
            return
        }

        playlist.add(index, audioInfo)

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

        if (index <= currentSongIndex && currentSongIndex != -1) {
            currentSongIndex++
        }

        _currentPlaylist.value = playlist.toList()
        _currentIndexFlow.value = currentSongIndex

        playerScope.launch {
            preferencesManager.savePlaylist(playlist)
        }
    }

    fun removeSongInfo(index: Int) {
        if (index < 0 || index >= playlist.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for playlist size ${playlist.size}")
        }

        playlist.removeAt(index)
        exoPlayer.removeMediaItem(index)

        if (index == currentSongIndex) {
            if (playlist.isEmpty()) {
                stop()
                currentSongIndex = -1
                _currentAudio.value = null
            } else {
                currentSongIndex = index.coerceAtMost(playlist.size - 1)
                _currentAudio.value = playlist.getOrNull(currentSongIndex)
            }
        } else if (index < currentSongIndex) {
            currentSongIndex--
        }

        _currentPlaylist.value = playlist.toList()
        _currentIndexFlow.value = currentSongIndex

        playerScope.launch {
            preferencesManager.savePlaylist(playlist)
            preferencesManager.saveCurrentIndex(currentSongIndex)
            preferencesManager.saveCurrentAudio(playlist.getOrNull(currentSongIndex))
        }
    }

    fun clearPlaylist() {
        playlist.clear()
        exoPlayer.clearMediaItems()
        stop()
        currentSongIndex = -1
        _currentAudio.value = null
        _currentPlaylist.value = emptyList()
        _currentIndexFlow.value = -1

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
        _playbackState.value = PlaybackState.Stopped
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun next() {
        if (playlist.isEmpty()) return

        when (_playMode.value) {
            PlayMode.SHUFFLE -> {
                val availableIndices = playlist.indices.filter { it !in playedIndices }
                if (availableIndices.isEmpty()) {
                    playedIndices.clear()
                    currentSongIndex = playlist.indices.random()
                } else {
                    currentSongIndex = availableIndices.random()
                }
                playedIndices.add(currentSongIndex)
            }
            PlayMode.NO_LOOP -> {
                if (currentSongIndex < playlist.size - 1) {
                    currentSongIndex++
                } else {
                    stop()
                    return
                }
            }
            else -> {
                currentSongIndex = (currentSongIndex + 1) % playlist.size
            }
        }

        exoPlayer.seekToNext()
        _currentAudio.value = playlist.getOrNull(currentSongIndex)
        _currentIndexFlow.value = currentSongIndex
    }

    fun previous() {
        if (playlist.isEmpty()) return

        when (_playMode.value) {
            PlayMode.SHUFFLE -> {
                currentSongIndex = playlist.indices.random()
            }
            else -> {
                currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
            }
        }

        exoPlayer.seekToPrevious()
        _currentAudio.value = playlist.getOrNull(currentSongIndex)
        _currentIndexFlow.value = currentSongIndex
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
        exoPlayer.repeatMode = when (mode) {
            PlayMode.LOOP -> Player.REPEAT_MODE_ALL
            PlayMode.SINGLE_LOOP -> Player.REPEAT_MODE_ONE
            PlayMode.SHUFFLE -> Player.REPEAT_MODE_ALL
            PlayMode.NO_LOOP -> Player.REPEAT_MODE_OFF
        }
        notifyPlayModeChanged(mode)

        playerScope.launch {
            preferencesManager.savePlayMode(mode)
        }
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f,1f)

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

    fun getCurrentPlaylist(): List<AudioInfo> = playlist.toList()

    fun getCurrentIndex(): Int = currentSongIndex

    fun getBufferedPosition(): Long = exoPlayer.bufferedPosition

    fun isBuffering(): Boolean = exoPlayer.playbackState == Player.STATE_BUFFERING

    fun isCurrMusicIsBuffering(audioInfo: AudioInfo): Boolean {
        return isBuffering() && currentAudio.value?.songId == audioInfo.songId
    }

    fun hasNetworkError(): Boolean = networkError

    fun getPlayHistory(): List<AudioInfo> = playHistoryList.toList()

    fun clearPlayHistory() {
        playHistoryList.clear()
        _playHistory.value = emptyList()

        playerScope.launch {
            preferencesManager.savePlayHistory(emptyList())
        }
    }

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
        context.unbindService(serviceConnection)
    }

    private fun startPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = playerScope.launch {
            while (true) {
                delay(100)
                val currentPosition = exoPlayer.currentPosition
                val currentTime = System.currentTimeMillis()
                val currentBufferedPosition = exoPlayer.bufferedPosition
                
                _playbackPosition.value = currentPosition
                _playbackDuration.value = exoPlayer.duration
                notifyPlayProgress(currentPosition, exoPlayer.duration)

                val isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
                
                if (isBuffering && !isCurrentlyBuffering) {
                    isCurrentlyBuffering = true
                    bufferingStartTime = currentTime
                    networkError = false
                }
                
                if (isBuffering) {
                    val bufferingDuration = currentTime - bufferingStartTime
                    if (bufferingDuration > 3000 && currentBufferedPosition == lastBufferedPosition) {
                        networkError = true
                    }
                } else {
                    isCurrentlyBuffering = false
                    if (currentBufferedPosition > 0) {
                        networkError = false
                    }
                }
                
                lastBufferedPosition = currentBufferedPosition

                if (currentTime - lastPositionSaveTime >= 1000) {
                    preferencesManager.savePlaybackPosition(currentPosition)
                    lastPositionSaveTime = currentTime
                }
            }
        }
    }

    fun release() {
        abandonAudioFocus()
        positionUpdateJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        disableNotification()
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) {
            return true
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }

        hasAudioFocus = false
    }
}