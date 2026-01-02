package com.cycling.starsky.player

import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackStateManager(
    private val scope: CoroutineScope
) {

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

    private val _playHistory = MutableStateFlow<List<AudioInfo>>(emptyList())
    val playHistory: StateFlow<List<AudioInfo>> = _playHistory.asStateFlow()

    private val playHistoryList = mutableListOf<AudioInfo>()

    private val listeners = mutableSetOf<OnPlayerEventListener>()

    private var positionUpdateJob: Job? = null
    private var lastPositionSaveTime = 0L

    private var lastBufferedPosition = 0L
    private var networkError = false
    private var bufferingStartTime = 0L
    private var isCurrentlyBuffering = false

    fun setPlaybackState(state: PlaybackState) {
        _playbackState.value = state

        if (state is PlaybackState.Completed) {
            scope.launch {
                delay(500)
                notifyPlaybackCompleted()
            }
        }
    }

    fun setCurrentAudio(audioInfo: AudioInfo?) {
        _currentAudio.value = audioInfo
        audioInfo?.let { addToPlayHistory(it) }
        notifyAudioChanged(audioInfo)
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
        notifyPlayModeChanged(mode)
    }

    fun updatePlaybackPosition(position: Long, duration: Long) {
        _playbackPosition.value = position
        _playbackDuration.value = duration

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPositionSaveTime > 1000) {
            lastPositionSaveTime = currentTime
            notifyPlayProgress(position, duration)
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun setBufferedPosition(position: Long) {
        lastBufferedPosition = position
    }

    fun setNetworkError(hasError: Boolean) {
        networkError = hasError
    }

    fun setBuffering(isBuffering: Boolean) {
        isCurrentlyBuffering = isBuffering
        if (isBuffering) {
            bufferingStartTime = System.currentTimeMillis()
        }
    }

    fun getBufferedPosition(): Long {
        return lastBufferedPosition
    }

    fun isBuffering(): Boolean {
        return isCurrentlyBuffering
    }

    fun hasNetworkError(): Boolean {
        return networkError
    }

    fun getPlayHistory(): List<AudioInfo> {
        return playHistoryList.toList()
    }

    fun clearPlayHistory() {
        playHistoryList.clear()
        _playHistory.value = emptyList()
    }

    fun addToPlayHistory(audioInfo: AudioInfo) {
        playHistoryList.removeIf { it.songId == audioInfo.songId }
        playHistoryList.add(0, audioInfo)
        if (playHistoryList.size > 100) {
            playHistoryList.removeAt(playHistoryList.size - 1)
        }
        _playHistory.value = playHistoryList.toList()
    }

    fun loadPlayHistory(history: List<AudioInfo>) {
        playHistoryList.clear()
        playHistoryList.addAll(history)
        _playHistory.value = playHistoryList.toList()
    }

    fun addListener(listener: OnPlayerEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnPlayerEventListener) {
        listeners.remove(listener)
    }

    fun notifyPlaybackStateChanged(state: PlaybackState) {
        listeners.forEach { it.onPlaybackStateChanged(state) }
    }

    fun notifyAudioChanged(audioInfo: AudioInfo?) {
        listeners.forEach { it.onAudioChanged(audioInfo) }
    }

    fun notifyPlayProgress(position: Long, duration: Long) {
        listeners.forEach { it.onPlayProgress(position, duration) }
    }

    fun notifyPlayModeChanged(mode: PlayMode) {
        listeners.forEach { it.onPlayModeChanged(mode) }
    }

    fun notifyError(message: String, exception: Throwable?) {
        listeners.forEach { it.onError(message, exception) }
    }

    fun notifyPlaybackCompleted() {
        listeners.forEach { it.onPlaybackStateChanged(PlaybackState.Completed) }
    }

    fun startPositionUpdate(updateCallback: () -> Unit) {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                updateCallback()
                delay(100)
            }
        }
    }

    fun stopPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun release() {
        stopPositionUpdate()
        listeners.clear()
    }
}
