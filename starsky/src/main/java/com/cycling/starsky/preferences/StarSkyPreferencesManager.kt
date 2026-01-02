package com.cycling.starsky.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "starrysky_preferences")

object PreferencesKeys {
    val CURRENT_SONG_ID = stringPreferencesKey("current_song_id")
    val CURRENT_SONG_URL = stringPreferencesKey("current_song_url")
    val CURRENT_SONG_NAME = stringPreferencesKey("current_song_name")
    val CURRENT_ARTIST = stringPreferencesKey("current_artist")
    val CURRENT_ALBUM_NAME = stringPreferencesKey("current_album_name")
    val CURRENT_COVER_URL = stringPreferencesKey("current_cover_url")
    val PLAYBACK_POSITION = longPreferencesKey("playback_position")
    val PLAY_MODE = stringPreferencesKey("play_mode")
    val VOLUME = floatPreferencesKey("volume")
    val SPEED = floatPreferencesKey("speed")
    val PLAYLIST = stringPreferencesKey("playlist")
    val CURRENT_INDEX = intPreferencesKey("current_index")
    val PLAY_HISTORY = stringPreferencesKey("play_history")
}

class StarSkyPreferencesManager(private val context: Context) {

    private val dataStore = context.dataStore

    suspend fun saveCurrentAudio(audioInfo: AudioInfo?) {
        dataStore.edit { preferences ->
            if (audioInfo != null) {
                preferences[PreferencesKeys.CURRENT_SONG_ID] = audioInfo.songId
                preferences[PreferencesKeys.CURRENT_SONG_URL] = audioInfo.songUrl
                preferences[PreferencesKeys.CURRENT_SONG_NAME] = audioInfo.songName
                preferences[PreferencesKeys.CURRENT_ARTIST] = audioInfo.artist
                preferences[PreferencesKeys.CURRENT_ALBUM_NAME] = audioInfo.albumName
                preferences[PreferencesKeys.CURRENT_COVER_URL] = audioInfo.coverUrl
            } else {
                preferences.remove(PreferencesKeys.CURRENT_SONG_ID)
                preferences.remove(PreferencesKeys.CURRENT_SONG_URL)
                preferences.remove(PreferencesKeys.CURRENT_SONG_NAME)
                preferences.remove(PreferencesKeys.CURRENT_ARTIST)
                preferences.remove(PreferencesKeys.CURRENT_ALBUM_NAME)
                preferences.remove(PreferencesKeys.CURRENT_COVER_URL)
            }
        }
    }

    fun getCurrentAudio(): Flow<AudioInfo?> {
        return dataStore.data.map { preferences ->
            val songId = preferences[PreferencesKeys.CURRENT_SONG_ID]
            if (songId != null) {
                AudioInfo(
                    songId = songId,
                    songUrl = preferences[PreferencesKeys.CURRENT_SONG_URL] ?: "",
                    songName = preferences[PreferencesKeys.CURRENT_SONG_NAME] ?: "",
                    artist = preferences[PreferencesKeys.CURRENT_ARTIST] ?: "",
                    albumName = preferences[PreferencesKeys.CURRENT_ALBUM_NAME] ?: "",
                    coverUrl = preferences[PreferencesKeys.CURRENT_COVER_URL] ?: ""
                )
            } else {
                null
            }
        }
    }

    suspend fun savePlaybackPosition(position: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYBACK_POSITION] = position
        }
    }

    fun getPlaybackPosition(): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.PLAYBACK_POSITION] ?: 0L
        }
    }

    suspend fun savePlayMode(mode: PlayMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAY_MODE] = mode.name
        }
    }

    fun getPlayMode(): Flow<PlayMode> {
        return dataStore.data.map { preferences ->
            val modeName = preferences[PreferencesKeys.PLAY_MODE]
            try {
                PlayMode.valueOf(modeName ?: PlayMode.LOOP.name)
            } catch (e: IllegalArgumentException) {
                PlayMode.LOOP
            }
        }
    }

    suspend fun saveVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOLUME] = volume
        }
    }

    fun getVolume(): Flow<Float> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.VOLUME] ?: 1.0f
        }
    }

    suspend fun saveSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEED] = speed
        }
    }

    fun getSpeed(): Flow<Float> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SPEED] ?: 1.0f
        }
    }

    suspend fun savePlaylist(playlist: List<AudioInfo>) {
        val json = Json.encodeToString(playlist)
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYLIST] = json
        }
    }

    fun getPlaylist(): Flow<List<AudioInfo>> {
        return dataStore.data.map { preferences ->
            val json = preferences[PreferencesKeys.PLAYLIST]
            if (json != null) {
                try {
                    Json.decodeFromString<List<AudioInfo>>(json)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun saveCurrentIndex(index: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_INDEX] = index
        }
    }

    fun getCurrentIndex(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.CURRENT_INDEX] ?: 0
        }
    }

    suspend fun clearPlaybackState() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun savePlayHistory(history: List<AudioInfo>) {
        val json = Json.encodeToString(history)
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAY_HISTORY] = json
        }
    }

    fun getPlayHistory(): Flow<List<AudioInfo>> {
        return dataStore.data.map { preferences ->
            val json = preferences[PreferencesKeys.PLAY_HISTORY]
            if (json != null) {
                try {
                    Json.decodeFromString<List<AudioInfo>>(json)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
}
