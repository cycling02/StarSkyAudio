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

fun Player.State.toPlaybackState(): PlaybackState = when (this) {
    Player.State.IDLE -> PlaybackState.Idle
    Player.State.BUFFERING -> PlaybackState.Buffering
    Player.State.READY -> PlaybackState.Playing
    Player.State.ENDED -> PlaybackState.Completed
    else -> PlaybackState.Idle
}