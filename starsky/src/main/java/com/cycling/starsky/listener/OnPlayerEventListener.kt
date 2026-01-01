package com.cycling.starsky.listener

import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlaybackState

interface OnPlayerEventListener {
    fun onPlaybackStateChanged(state: PlaybackState) {}
    fun onAudioChanged(audioInfo: AudioInfo?) {}
    fun onPlayProgress(position: Long, duration: Long) {}
    fun onPlayModeChanged(mode: com.cycling.starsky.model.PlayMode) {}
    fun onError(message: String, exception: Throwable?) {}
}