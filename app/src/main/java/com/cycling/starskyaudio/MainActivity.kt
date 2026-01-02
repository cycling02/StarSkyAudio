package com.cycling.starskyaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.cycling.starsky.StarSky
import com.cycling.starsky.control.PlayerControl
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val audioList = listOf(
        AudioInfo(
            songId = "song_1",
            songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            songName = "Song 1",
            artist = "Artist 1"
        ),
        AudioInfo(
            songId = "song_2",
            songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            songName = "Song 2",
            artist = "Artist 2"
        ),
        AudioInfo(
            songId = "song_3",
            songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            songName = "Song 3",
            artist = "Artist 3"
        )
    )

    private var isServiceBound = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                PlayerScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        StarSky.release()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PlayerScreen() {
        val context = LocalContext.current
        val playerControl = remember { StarSky.with(context) }
        
        var playbackState by remember { mutableStateOf<PlaybackState>(PlaybackState.Idle) }
        var currentSong by remember { mutableStateOf("No song playing") }
        var playMode by remember { mutableStateOf(PlayMode.LOOP) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentPlaylist by remember { mutableStateOf<List<AudioInfo>>(emptyList()) }
        var currentIndex by remember { mutableStateOf(-1) }
        var bufferedPosition by remember { mutableLongStateOf(0L) }
        var isBuffering by remember { mutableStateOf(false) }
        var networkError by remember { mutableStateOf(false) }
        var playHistory by remember { mutableStateOf<List<AudioInfo>>(emptyList()) }
        var showHistory by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                playerControl.playbackState.collect { state ->
                    playbackState = state
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                playerControl.currentAudio.collect { audio ->
                    audio?.let {
                        currentSong = "${it.songName} - ${it.artist}"
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                playerControl.playbackPosition.collect { position ->
                    currentPosition = position
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                playerControl.playbackDuration.collect { dur ->
                    duration = dur
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                playerControl.isPlaying.collect { playing ->
                    isPlaying = playing
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                playerControl.currentPlaylist.collect { playlist ->
                    currentPlaylist = playlist
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                playerControl.currentIndex.collect { index ->
                    currentIndex = index
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(500)
                    bufferedPosition = playerControl.getBufferedPosition()
                    isBuffering = playerControl.isBuffering()
                    networkError = playerControl.hasNetworkError()
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                StarSky.playHistory.collect { history ->
                    playHistory = history
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("StarSky Player") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "State: $playbackState",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (networkError) "Network Error" else if (isBuffering) "Buffering..." else "Ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (networkError) MaterialTheme.colorScheme.error else if (isBuffering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Buffered: ${formatTime(bufferedPosition)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = currentSong,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        playerControl.playPlaylist(audioList, 0)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Play Playlist")
                }

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { value ->
                        if (duration > 0) {
                            playerControl.seekTo((value * duration).toLong())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { playerControl.previous() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Previous")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isPlaying) {
                                playerControl.pause()
                            } else {
                                playerControl.resume()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { playerControl.stop() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { playerControl.next() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                    }
                }
                Button(
                    onClick = {
                        val newMode = when (playMode) {
                            PlayMode.LOOP -> PlayMode.SINGLE_LOOP
                            PlayMode.SINGLE_LOOP -> PlayMode.SHUFFLE
                            PlayMode.SHUFFLE -> PlayMode.NO_LOOP
                            PlayMode.NO_LOOP -> PlayMode.LOOP
                        }
                        playMode = newMode
                        playerControl.setPlayMode(newMode)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Mode: $playMode")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            val newSong = AudioInfo(
                                songId = "song_4",
                                songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                                songName = "Song 4",
                                artist = "Artist 4"
                            )
                            playerControl.addSongInfo(newSong)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Song")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newSong = AudioInfo(
                                songId = "song_5",
                                songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                                songName = "Song 5",
                                artist = "Artist 5"
                            )
                            playerControl.addSongInfoAt(newSong, 0)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add at 0")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            val playlist = playerControl.getCurrentPlaylist()
                            if (playlist.isNotEmpty()) {
                                playerControl.removeSongInfo(playlist.size - 1)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove Last")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            playerControl.clearPlaylist()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            val playlist = playerControl.getCurrentPlaylist()
                            if (currentIndex >= 0 && currentIndex < playlist.size) {
                                val currentSong = playlist[currentIndex]
                                val isBuffering = playerControl.isCurrMusicIsBuffering(currentSong)
                                android.util.Log.d("MainActivity", "Current song buffering: $isBuffering")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Check Buffer")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showHistory = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("History (${playHistory.size})")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Playlist (${currentPlaylist.size} songs)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(currentPlaylist) { index, song ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == currentIndex) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = song.songName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (index == currentIndex) {
                                    Text(
                                        text = "♪",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showHistory) {
            AlertDialog(
                onDismissRequest = { showHistory = false },
                title = { Text("Play History (${playHistory.size})") },
                text = {
                    LazyColumn(
                        modifier = Modifier.height(400.dp)
                    ) {
                        itemsIndexed(playHistory) { index, song ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = song.songName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (playHistory.isEmpty()) {
                            item {
                                Text(
                                    text = "No play history yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showHistory = false }) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    if (playHistory.isNotEmpty()) {
                        Button(onClick = {
                            StarSky.clearPlayHistory()
                        }) {
                            Text("Clear")
                        }
                    }
                }
            )
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}