package com.cycling.starsky.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioInfo(
    val songId: String = "",
    val songUrl: String,
    val songName: String = "",
    val artist: String = "",
    val albumName: String = "",
    val coverUrl: String = "",
    val duration: Long = 0L,
    val mimeType: String? = null
) {
    companion object {
        fun create(url: String): AudioInfo = AudioInfo(
            songUrl = url,
            songId = url.hashCode().toString()
        )
    }
}