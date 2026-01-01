package com.cycling.starsky.model

import org.junit.Test
import org.junit.Assert.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AudioInfoTest {

    @Test
    fun testAudioInfoCreation() {
        val audioInfo = AudioInfo(
            songId = "123",
            songUrl = "https://example.com/audio.mp3",
            songName = "Test Song",
            artist = "Test Artist",
            albumName = "Test Album",
            coverUrl = "https://example.com/cover.jpg",
            duration = 180000L
        )

        assertEquals("123", audioInfo.songId)
        assertEquals("https://example.com/audio.mp3", audioInfo.songUrl)
        assertEquals("Test Song", audioInfo.songName)
        assertEquals("Test Artist", audioInfo.artist)
        assertEquals("Test Album", audioInfo.albumName)
        assertEquals("https://example.com/cover.jpg", audioInfo.coverUrl)
        assertEquals(180000L, audioInfo.duration)
    }

    @Test
    fun testAudioInfoWithDefaultValues() {
        val audioInfo = AudioInfo(
            songUrl = "https://example.com/audio.mp3"
        )

        assertEquals("", audioInfo.songId)
        assertEquals("https://example.com/audio.mp3", audioInfo.songUrl)
        assertEquals("", audioInfo.songName)
        assertEquals("", audioInfo.artist)
        assertEquals("", audioInfo.albumName)
        assertEquals("", audioInfo.coverUrl)
        assertEquals(0L, audioInfo.duration)
    }

    @Test
    fun testAudioInfoCreateFactory() {
        val url = "https://example.com/audio.mp3"
        val audioInfo = AudioInfo.create(url)

        assertEquals(url.hashCode().toString(), audioInfo.songId)
        assertEquals(url, audioInfo.songUrl)
    }

    @Test
    fun testAudioInfoSerialization() {
        val audioInfo = AudioInfo(
            songId = "123",
            songUrl = "https://example.com/audio.mp3",
            songName = "Test Song",
            artist = "Test Artist",
            albumName = "Test Album",
            coverUrl = "https://example.com/cover.jpg",
            duration = 180000L
        )

        val json = Json.encodeToString(audioInfo)
        assertNotNull(json)
        assertTrue(json.contains("123"))
        assertTrue(json.contains("Test Song"))
    }

    @Test
    fun testAudioInfoDeserialization() {
        val json = """{"songId":"123","songUrl":"https://example.com/audio.mp3","songName":"Test Song","artist":"Test Artist","albumName":"Test Album","coverUrl":"https://example.com/cover.jpg","duration":180000}"""
        
        val audioInfo = Json.decodeFromString<AudioInfo>(json)
        
        assertEquals("123", audioInfo.songId)
        assertEquals("https://example.com/audio.mp3", audioInfo.songUrl)
        assertEquals("Test Song", audioInfo.songName)
        assertEquals("Test Artist", audioInfo.artist)
        assertEquals("Test Album", audioInfo.albumName)
        assertEquals("https://example.com/cover.jpg", audioInfo.coverUrl)
        assertEquals(180000L, audioInfo.duration)
    }
}
