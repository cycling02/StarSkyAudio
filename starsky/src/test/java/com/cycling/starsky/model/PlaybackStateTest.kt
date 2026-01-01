package com.cycling.starsky.model

import androidx.media3.common.Player
import org.junit.Test
import org.junit.Assert.*

class PlaybackStateTest {

    @Test
    fun testPlaybackStateIdle() {
        val state = PlaybackState.Idle
        assertTrue(state is PlaybackState.Idle)
    }

    @Test
    fun testPlaybackStateBuffering() {
        val state = PlaybackState.Buffering
        assertTrue(state is PlaybackState.Buffering)
    }

    @Test
    fun testPlaybackStatePlaying() {
        val state = PlaybackState.Playing
        assertTrue(state is PlaybackState.Playing)
    }

    @Test
    fun testPlaybackStatePaused() {
        val state = PlaybackState.Paused
        assertTrue(state is PlaybackState.Paused)
    }

    @Test
    fun testPlaybackStateStopped() {
        val state = PlaybackState.Stopped
        assertTrue(state is PlaybackState.Stopped)
    }

    @Test
    fun testPlaybackStateCompleted() {
        val state = PlaybackState.Completed
        assertTrue(state is PlaybackState.Completed)
    }

    @Test
    fun testPlaybackStateError() {
        val state = PlaybackState.Error("Test error", Exception("Test exception"))
        assertTrue(state is PlaybackState.Error)
        val errorState = state as PlaybackState.Error
        assertEquals("Test error", errorState.message)
        assertNotNull(errorState.exception)
    }

    @Test
    fun testToPlaybackStateIdle() {
        val state = Player.STATE_IDLE.toPlaybackState(false)
        assertTrue(state is PlaybackState.Idle)
    }

    @Test
    fun testToPlaybackStateBuffering() {
        val state = Player.STATE_BUFFERING.toPlaybackState(false)
        assertTrue(state is PlaybackState.Buffering)
    }

    @Test
    fun testToPlaybackStateReadyPlaying() {
        val state = Player.STATE_READY.toPlaybackState(true)
        assertTrue(state is PlaybackState.Playing)
    }

    @Test
    fun testToPlaybackStateReadyPaused() {
        val state = Player.STATE_READY.toPlaybackState(false)
        assertTrue(state is PlaybackState.Paused)
    }

    @Test
    fun testToPlaybackStateEnded() {
        val state = Player.STATE_ENDED.toPlaybackState(false)
        assertTrue(state is PlaybackState.Completed)
    }

    @Test
    fun testToPlaybackStateUnknown() {
        val state = 999.toPlaybackState(false)
        assertTrue(state is PlaybackState.Idle)
    }
}
