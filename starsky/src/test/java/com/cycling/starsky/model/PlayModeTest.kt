package com.cycling.starsky.model

import org.junit.Test
import org.junit.Assert.*

class PlayModeTest {

    @Test
    fun testPlayModeLoop() {
        assertEquals("LOOP", PlayMode.LOOP.name)
    }

    @Test
    fun testPlayModeSingleLoop() {
        assertEquals("SINGLE_LOOP", PlayMode.SINGLE_LOOP.name)
    }

    @Test
    fun testPlayModeShuffle() {
        assertEquals("SHUFFLE", PlayMode.SHUFFLE.name)
    }

    @Test
    fun testPlayModeValues() {
        val modes = PlayMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(PlayMode.LOOP))
        assertTrue(modes.contains(PlayMode.SINGLE_LOOP))
        assertTrue(modes.contains(PlayMode.SHUFFLE))
    }
}
