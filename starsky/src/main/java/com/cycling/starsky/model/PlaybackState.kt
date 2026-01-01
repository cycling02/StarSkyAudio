package com.cycling.starsky.model

import androidx.media3.common.Player

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data object Buffering : PlaybackState()
    data object Playing : PlaybackState()
    data object Paused : PlaybackState()
    data object Stopped : PlaybackState()
    data object Completed : PlaybackState()
    data class Error(val message: String, val exception: Throwable? = null) : PlaybackState()
}

fun Int.toPlaybackState(isPlaying: Boolean): PlaybackState = when (this) {
    Player.STATE_IDLE -> PlaybackState.Idle
    Player.STATE_BUFFERING -> PlaybackState.Buffering
    Player.STATE_READY -> if (isPlaying) PlaybackState.Playing else PlaybackState.Paused
    Player.STATE_ENDED -> PlaybackState.Completed
    else -> PlaybackState.Idle
}