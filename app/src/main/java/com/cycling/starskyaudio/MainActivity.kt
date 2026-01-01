package com.cycling.starskyaudio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.cycling.starsky.StarSky
import com.cycling.starsky.listener.OnPlayerEventListener
import com.cycling.starsky.model.AudioInfo
import com.cycling.starsky.model.PlayMode
import com.cycling.starsky.model.PlaybackState
import com.cycling.starsky.service.StarSkyMediaSessionService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val audioList = listOf(
        AudioInfo(
            songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            songName = "Song 1",
            artist = "Artist 1"
        ),
        AudioInfo(
            songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            songName = "Song 2",
            artist = "Artist 2"
        ),
        AudioInfo(
            songUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            songName = "Song 3",
            artist = "Artist 3"
        )
    )

    private var isServiceBound = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        StarSky.init(this)
        
        setContent {
            MaterialTheme {
                PlayerScreen()
            }
        }

        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        StarSky.addListener(object : OnPlayerEventListener {
            override fun onPlaybackStateChanged(state: PlaybackState) {
            }

            override fun onAudioChanged(audioInfo: com.cycling.starsky.model.AudioInfo?) {
            }

            override fun onError(message: String, exception: Throwable?) {
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        StarSky.release()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PlayerScreen() {
        var playbackState by remember { mutableStateOf<PlaybackState>(PlaybackState.Idle) }
        var currentSong by remember { mutableStateOf("No song playing") }
        var playMode by remember { mutableStateOf(PlayMode.LOOP) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var isPlaying by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                StarSky.playbackState.collect { state ->
                    playbackState = state
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                StarSky.currentAudio.collect { audio ->
                    audio?.let {
                        currentSong = "${it.songName} - ${it.artist}"
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                StarSky.playbackPosition.collect { position ->
                    currentPosition = position
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                StarSky.playbackDuration.collect { dur ->
                    duration = dur
                }
            }
        }

        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                StarSky.isPlaying.collect { playing ->
                    isPlaying = playing
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "State: $playbackState",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = currentSong,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { value ->
                        if (duration > 0) {
                            StarSky.seekTo((value * duration).toLong())
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
                        onClick = { StarSky.previous() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Previous")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (isPlaying) {
                                StarSky.pause()
                            } else {
                                StarSky.playPlaylist(audioList, 0)
                                StarSky.enableNotification()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { StarSky.stop() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { StarSky.next() },
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
                            PlayMode.SHUFFLE -> PlayMode.LOOP
                        }
                        playMode = newMode
                        StarSky.setPlayMode(newMode)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Mode: $playMode")
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}