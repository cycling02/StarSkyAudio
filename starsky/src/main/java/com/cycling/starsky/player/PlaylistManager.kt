package com.cycling.starsky.player

import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaylistManager {

    private val playlist = mutableListOf<AudioInfo>()
    private var currentSongIndex = -1
    private val playedIndices = mutableSetOf<Int>()

    private val _currentPlaylist = MutableStateFlow<List<AudioInfo>>(emptyList())
    val currentPlaylist: StateFlow<List<AudioInfo>> = _currentPlaylist.asStateFlow()

    private val _currentIndexFlow = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndexFlow.asStateFlow()

    fun setPlaylist(audioList: List<AudioInfo>, startIndex: Int = 0) {
        playlist.clear()
        val uniqueAudioList = audioList.distinctBy { it.songId }
        playlist.addAll(uniqueAudioList)
        currentSongIndex = startIndex.coerceAtMost(uniqueAudioList.size - 1)
        playedIndices.clear()

        _currentPlaylist.value = playlist.toList()
        _currentIndexFlow.value = currentSongIndex
    }

    fun addSong(audioInfo: AudioInfo) {
        if (playlist.any { it.songId == audioInfo.songId }) {
            return
        }

        playlist.add(audioInfo)
        _currentPlaylist.value = playlist.toList()
    }

    fun addSongAt(audioInfo: AudioInfo, index: Int) {
        if (index < 0 || index > playlist.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for playlist size ${playlist.size}")
        }

        if (playlist.any { it.songId == audioInfo.songId }) {
            return
        }

        playlist.add(index, audioInfo)

        if (index <= currentSongIndex && currentSongIndex != -1) {
            currentSongIndex++
        }

        _currentPlaylist.value = playlist.toList()
        _currentIndexFlow.value = currentSongIndex
    }

    fun removeSong(index: Int) {
        if (index < 0 || index >= playlist.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for playlist size ${playlist.size}")
        }

        playlist.removeAt(index)

        if (index == currentSongIndex) {
            currentSongIndex = if (playlist.isEmpty()) {
                -1
            } else {
                index.coerceAtMost(playlist.size - 1)
            }
        } else if (index < currentSongIndex) {
            currentSongIndex--
        }

        _currentPlaylist.value = playlist.toList()
        _currentIndexFlow.value = currentSongIndex
    }

    fun clearPlaylist() {
        playlist.clear()
        currentSongIndex = -1
        playedIndices.clear()

        _currentPlaylist.value = emptyList()
        _currentIndexFlow.value = -1
    }

    fun getCurrentSong(): AudioInfo? {
        return playlist.getOrNull(currentSongIndex)
    }

    fun getCurrentIndex(): Int {
        return currentSongIndex
    }

    fun getPlaylist(): List<AudioInfo> {
        return playlist.toList()
    }

    fun isEmpty(): Boolean {
        return playlist.isEmpty()
    }

    fun size(): Int {
        return playlist.size
    }

    fun getNextIndex(playMode: PlayMode): Int? {
        if (playlist.isEmpty()) return null

        return when (playMode) {
            PlayMode.SHUFFLE -> {
                val availableIndices = playlist.indices.filter { it !in playedIndices }
                if (availableIndices.isEmpty()) {
                    playedIndices.clear()
                    playlist.indices.random()
                } else {
                    availableIndices.random()
                }
            }
            PlayMode.NO_LOOP -> {
                if (currentSongIndex < playlist.size - 1) {
                    currentSongIndex + 1
                } else {
                    null
                }
            }
            else -> {
                (currentSongIndex + 1) % playlist.size
            }
        }
    }

    fun getPreviousIndex(playMode: PlayMode): Int? {
        if (playlist.isEmpty()) return null

        return when (playMode) {
            PlayMode.SHUFFLE -> {
                playlist.indices.random()
            }
            else -> {
                if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
            }
        }
    }

    fun moveToIndex(index: Int) {
        if (index < 0 || index >= playlist.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for playlist size ${playlist.size}")
        }

        currentSongIndex = index
        _currentIndexFlow.value = currentSongIndex
    }

    fun markAsPlayed(index: Int) {
        playedIndices.add(index)
    }

    fun clearPlayedIndices() {
        playedIndices.clear()
    }
}
