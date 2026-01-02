# StarSky Audio

[![JitPack](https://jitpack.io/v/cycling02/StarSkyAudio.svg)](https://jitpack.io/#cycling02/StarSkyAudio)

基于 AndroidX Media3 的音频播放库，提供简单易用的音频播放功能。

## 安装

### 通过 JitPack 集成

#### Step 1. 添加 JitPack 仓库

在项目的 `settings.gradle.kts` 文件中添加 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### Step 2. 添加依赖

在模块的 `build.gradle.kts` 文件中添加依赖：

```kotlin
dependencies {
    implementation("com.github.cycling02:StarSkyAudio:1.1.0")
}
```

#### Step 3. 添加必要的权限

在 `AndroidManifest.xml` 中添加必要的权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 功能特性

### 核心播放功能
- 单首音频播放
- 播放列表播放
- 支持本地和网络音频资源
- 自动缓存管理

### 播放控制
- 播放/暂停
- 停止播放
- 跳转到指定位置
- 上一首/下一首
- 播放/暂停状态保持（不会重新开始播放）

### 播放模式
- 列表循环 (LOOP)
- 单曲循环 (SINGLE_LOOP)
- 随机播放 (SHUFFLE)

### 音量和速度控制
- 音量调节 (0.0 - 1.0)
- 播放速度调节 (0.5x - 2.0x)

### 队列管理
- 添加歌曲到播放列表末尾
- 在指定位置插入歌曲
- 删除指定位置的歌曲
- 清空播放列表
- 获取当前播放列表
- 获取当前播放索引

### 缓存状态监控
- 获取当前缓存位置（毫秒）
- 检查是否正在缓冲
- 检查指定歌曲是否正在缓冲
- 网络错误检测（智能防抖）

### 状态监听
- 播放状态监听 (Idle, Buffering, Playing, Paused, Completed, Stopped, Error)
- 当前音频信息监听
- 播放进度监听
- 播放模式监听
- 错误监听
- 播放列表变化监听
- 当前播放索引监听

### 通知功能
- 系统通知栏显示播放信息
- 通知栏控制按钮（播放/暂停、上一首/下一首）
- 媒体会话集成，支持系统媒体控制

### 缓存管理
- 自动缓存已播放的音频
- 缓存大小查询
- 缓存清理
- LRU 缓存策略（默认 512MB）

### 播放状态持久化
- 自动保存播放状态（音频信息、播放列表、播放位置）
- 自动保存播放设置（播放模式、音量、播放速度）
- 应用重启后恢复上次播放状态
- 支持清除持久化数据

## 快速开始

### 初始化

在 Application 类中初始化播放器：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        StarSky.init(this)
            .setOpenCache(true)              // 启用缓存
            .setNotificationEnabled(true)    // 启用通知
            .setAutoPlay(false)              // 不自动播放
            .setRestoreState(true)           // 恢复播放状态
            .apply()
    }
}
```

### 创建播放器实例

#### 方式一：使用默认配置

```kotlin
val playerControl = PlayerControlImpl(context)
```

#### 方式二：使用自定义配置

```kotlin
val config = StarSkyConfig.Builder()
    .setConnService(true)  // 是否需要后台服务（默认 true）
    .setStartService(true)  // 是否需要 startService（默认 false）
    .setOnlyStartService(false)  // 是否只是 startService 而不需要 startForegroundService（默认 true）
    .setConnServiceListener(object : ConnServiceListener {
        override fun onServiceConnected() {
            // Service 连接成功回调
        }
        override fun onServiceDisconnected() {
            // Service 断开连接回调
        }
    })
    .setStartForegroundByWorkManager(false)  // 是否使用 WorkManager 启动后台服务（默认 false）
    .build()

val playerControl = PlayerControlImpl(context, config)
```

**配置说明：**
- `connService`: 是否需要后台服务，默认 true。如果设置为 false，所有逻辑不经过 Service
- `isStartService`: 是否需要 startService，默认 false。false 的话只有 bindService
- `onlyStartService`: 是否只是 startService 而不需要 startForegroundService，默认 true
- `connServiceListener`: 连接服务回调，可通过这个监听查看 Service 是否连接成功
- `startForegroundByWorkManager`: 开关，可选择是否使用 WorkManager 来启动安卓 12 的后台服务，默认关闭

### 启用通知

如果需要显示通知栏控制，需要在播放前启用通知：

```kotlin
playerControl.enableNotification()
```

### 添加状态监听器

```kotlin
val listener = object : OnPlayerEventListener {
    override fun onPlaybackStateChanged(state: PlaybackState) {
        when (state) {
            is PlaybackState.Playing -> {
                // 正在播放
            }
            is PlaybackState.Paused -> {
                // 已暂停
            }
            is PlaybackState.Buffering -> {
                // 正在缓冲
            }
            is PlaybackState.Error -> {
                // 发生错误
            }
            // 其他状态...
        }
    }
    
    override fun onAudioChanged(audioInfo: AudioInfo?) {
        // 当前播放的音频发生变化
    }
    
    override fun onPlayProgress(position: Long, duration: Long) {
        // 播放进度更新
    }
    
    override fun onPlayModeChanged(mode: PlayMode) {
        // 播放模式变化
    }
    
    override fun onError(message: String, exception: Throwable?) {
        // 发生错误
    }
}

playerControl.addListener(listener)
```

### 使用 StateFlow 监听状态

```kotlin
lifecycleScope.launch {
    playerControl.playbackState.collect { state ->
        // 播放状态变化
    }
}

lifecycleScope.launch {
    playerControl.currentAudio.collect { audioInfo ->
        // 当前音频变化
    }
}

lifecycleScope.launch {
    playerControl.playbackPosition.collect { position ->
        // 播放位置变化
    }
}

lifecycleScope.launch {
    playerControl.isPlaying.collect { isPlaying ->
        // 播放状态变化
    }
}
```

## API 文档

### 核心类

#### PlayerControl

播放器控制接口，提供所有播放控制功能。

**实现类：** `PlayerControlImpl`

#### StarSkyConfig

播放器配置类，使用 Builder 模式构建。

**配置参数：**
- `openCache`: 是否启用缓存（默认 false）
- `notificationEnabled`: 是否启用通知（默认 true）
- `autoPlay`: 是否自动播放（默认 false）
- `restoreState`: 是否恢复播放状态（默认 true）
- `connService`: 是否需要后台服务（默认 true）
- `isStartService`: 是否需要 startService（默认 false）
- `onlyStartService`: 是否只是 startService 而不需要 startForegroundService（默认 true）
- `connServiceListener`: Service 连接监听器（默认 null）
- `startForegroundByWorkManager`: 是否使用 WorkManager 启动后台服务（默认 false）

#### AudioInfo

音频信息数据类。

**属性：**
- `songId`: 歌曲 ID
- `songUrl`: 歌曲 URL（必填）
- `songName`: 歌曲名称
- `artist`: 艺术家
- `albumName`: 专辑名称
- `coverUrl`: 封面 URL
- `duration`: 时长（毫秒）
- `mimeType`: MIME 类型

**创建方式：**
```kotlin
// 方式一：完整创建
val audioInfo = AudioInfo(
    songId = "123",
    songUrl = "https://example.com/song.mp3",
    songName = "歌曲名称",
    artist = "艺术家",
    albumName = "专辑名称",
    coverUrl = "https://example.com/cover.jpg"
)

// 方式二：快速创建（自动生成 ID）
val audioInfo = AudioInfo.create("https://example.com/song.mp3")
```

#### PlayMode

播放模式枚举。

**可选值：**
- `LOOP`: 列表循环
- `SINGLE_LOOP`: 单曲循环
- `SHUFFLE`: 随机播放
- `NO_LOOP`: 不循环

#### PlaybackState

播放状态密封类。

**可选状态：**
- `Idle`: 空闲状态
- `Buffering`: 正在缓冲
- `Playing`: 正在播放
- `Paused`: 已暂停
- `Stopped`: 已停止
- `Completed`: 播放完成
- `Error`: 发生错误（包含错误消息和异常）

### 播放控制 API

#### 播放单首音频

```kotlin
playerControl.play(audioInfo)
```

#### 播放播放列表

```kotlin
playerControl.playPlaylist(audioList, startIndex = 0)
```

#### 暂停播放

```kotlin
playerControl.pause()
```

#### 恢复播放

```kotlin
playerControl.resume()
```

#### 停止播放

```kotlin
playerControl.stop()
```

#### 跳转到指定位置

```kotlin
playerControl.seekTo(30000)  // 跳转到 30 秒位置
```

#### 下一首

```kotlin
playerControl.next()
```

#### 上一首

```kotlin
playerControl.previous()
```

### 队列管理 API

#### 添加歌曲到列表末尾

```kotlin
playerControl.addSongInfo(audioInfo)
```

#### 在指定位置插入歌曲

```kotlin
playerControl.addSongInfoAt(audioInfo, index = 2)
```

#### 删除指定位置的歌曲

```kotlin
playerControl.removeSongInfo(index = 1)
```

#### 清空播放列表

```kotlin
playerControl.clearPlaylist()
```

#### 获取当前播放列表

```kotlin
val playlist = playerControl.getCurrentPlaylist()
```

#### 获取当前播放索引

```kotlin
val index = playerControl.getCurrentIndex()
```

### 播放模式 API

#### 设置播放模式

```kotlin
playerControl.setPlayMode(PlayMode.LOOP)
playerControl.setPlayMode(PlayMode.SINGLE_LOOP)
playerControl.setPlayMode(PlayMode.SHUFFLE)
playerControl.setPlayMode(PlayMode.NO_LOOP)
```

### 音量和速度控制 API

#### 设置音量

```kotlin
playerControl.setVolume(0.8f)  // 范围：0.0 - 1.0
```

#### 获取音量

```kotlin
val volume = playerControl.getVolume()
```

#### 设置播放速度

```kotlin
playerControl.setSpeed(1.5f)  // 范围：0.5x - 2.0x
```

#### 获取播放速度

```kotlin
val speed = playerControl.getSpeed()
```

### 缓存状态监控 API

#### 获取当前缓存位置

```kotlin
val bufferedPosition = playerControl.getBufferedPosition()
```

#### 检查是否正在缓冲

```kotlin
val isBuffering = playerControl.isBuffering()
```

#### 检查指定歌曲是否正在缓冲

```kotlin
val isBuffering = playerControl.isCurrMusicIsBuffering(audioInfo)
```

#### 检测网络错误

```kotlin
val hasNetworkError = playerControl.hasNetworkError()
```

### 状态监听 API

#### 添加监听器

```kotlin
playerControl.addListener(listener)
```

#### 移除监听器

```kotlin
playerControl.removeListener(listener)
```

### StateFlow 状态监听

#### 播放状态

```kotlin
val playbackState: StateFlow<PlaybackState> = playerControl.playbackState
```

#### 当前音频信息

```kotlin
val currentAudio: StateFlow<AudioInfo?> = playerControl.currentAudio
```

#### 播放模式

```kotlin
val playMode: StateFlow<PlayMode> = playerControl.playMode
```

#### 播放位置

```kotlin
val playbackPosition: StateFlow<Long> = playerControl.playbackPosition
```

#### 播放时长

```kotlin
val playbackDuration: StateFlow<Long> = playerControl.playbackDuration
```

#### 是否正在播放

```kotlin
val isPlaying: StateFlow<Boolean> = playerControl.isPlaying
```

#### 当前播放列表

```kotlin
val currentPlaylist: StateFlow<List<AudioInfo>> = playerControl.currentPlaylist
```

#### 当前播放索引

```kotlin
val currentIndex: StateFlow<Int> = playerControl.currentIndex
```

#### 播放历史

```kotlin
val playHistory: StateFlow<List<AudioInfo>> = playerControl.playHistory
```

### 通知功能 API

#### 启用通知

```kotlin
playerControl.enableNotification()
```

#### 禁用通知

```kotlin
playerControl.disableNotification()
```

### 播放历史 API

#### 获取播放历史

```kotlin
val history = playerControl.getPlayHistory()
```

#### 清空播放历史

```kotlin
playerControl.clearPlayHistory()
```

### 高级功能 API

#### 获取底层 ExoPlayer 实例

```kotlin
val exoPlayer = playerControl.getExoPlayer()
```

### 资源释放

#### 释放播放器资源

```kotlin
playerControl.release()
```

**注意：** 在不再使用播放器时，必须调用此方法释放资源，避免内存泄漏。

## 使用示例

### 完整示例：播放列表播放

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var playerControl: PlayerControlImpl
    private val listener = object : OnPlayerEventListener {
        override fun onPlaybackStateChanged(state: PlaybackState) {
            when (state) {
                is PlaybackState.Playing -> {
                    // 更新 UI 为播放状态
                }
                is PlaybackState.Paused -> {
                    // 更新 UI 为暂停状态
                }
                is PlaybackState.Buffering -> {
                    // 显示缓冲进度
                }
                is PlaybackState.Error -> {
                    // 显示错误信息
                }
                else -> {}
            }
        }
        
        override fun onAudioChanged(audioInfo: AudioInfo?) {
            audioInfo?.let {
                // 更新当前播放歌曲信息
                tvSongName.text = it.songName
                tvArtist.text = it.artist
                Glide.with(this@MainActivity).load(it.coverUrl).into(ivCover)
            }
        }
        
        override fun onPlayProgress(position: Long, duration: Long) {
            // 更新进度条
            val progress = (position * 100 / duration).toInt()
            progressBar.progress = progress
            tvCurrentTime.text = formatTime(position)
            tvTotalTime.text = formatTime(duration)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 创建播放器
        val config = StarSkyConfig.Builder()
            .setConnService(true)
            .setConnServiceListener(object : ConnServiceListener {
                override fun onServiceConnected() {
                    // Service 连接成功
                }
                override fun onServiceDisconnected() {
                    // Service 断开连接
                }
            })
            .build()
        
        playerControl = PlayerControlImpl(this, config)
        playerControl.enableNotification()
        playerControl.addListener(listener)
        
        // 创建播放列表
        val audioList = listOf(
            AudioInfo(
                songId = "1",
                songUrl = "https://example.com/song1.mp3",
                songName = "歌曲1",
                artist = "艺术家1",
                albumName = "专辑1",
                coverUrl = "https://example.com/cover1.jpg"
            ),
            AudioInfo(
                songId = "2",
                songUrl = "https://example.com/song2.mp3",
                songName = "歌曲2",
                artist = "艺术家2",
                albumName = "专辑2",
                coverUrl = "https://example.com/cover2.jpg"
            )
        )
        
        // 开始播放
        playerControl.playPlaylist(audioList, startIndex = 0)
        
        // 监听播放状态
        lifecycleScope.launch {
            playerControl.isPlaying.collect { isPlaying ->
                // 更新播放/暂停按钮状态
                btnPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        playerControl.removeListener(listener)
        playerControl.release()
    }
    
    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
```

### 示例：使用 StateFlow 构建响应式 UI

```kotlin
@Composable
fun PlayerScreen(playerControl: PlayerControl) {
    val playbackState by playerControl.playbackState.collectAsState()
    val currentAudio by playerControl.currentAudio.collectAsState()
    val playbackPosition by playerControl.playbackPosition.collectAsState()
    val playbackDuration by playerControl.playbackDuration.collectAsState()
    val isPlaying by playerControl.isPlaying.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        currentAudio?.let { audio ->
            AsyncImage(
                model = audio.coverUrl,
                contentDescription = "Album Cover",
                modifier = Modifier.size(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = audio.songName, style = MaterialTheme.typography.headlineMedium)
            Text(text = audio.artist, style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Slider(
                value = playbackPosition.toFloat(),
                onValueChange = { position ->
                    playerControl.seekTo(position.toLong())
                },
                valueRange = 0f..playbackDuration.toFloat()
            )
            
            Text(
                text = "${formatTime(playbackPosition)} / ${formatTime(playbackDuration)}"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row {
                IconButton(onClick = { playerControl.previous() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            playerControl.pause()
                        } else {
                            playerControl.resume()
                        }
                    }
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                
                IconButton(onClick = { playerControl.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}
```

## 技术栈

- **AndroidX Media3 1.6.1** - 媒体播放核心
- **ExoPlayer** - 底层播放引擎
- **Kotlin Coroutines** - 异步处理
- **StateFlow** - 状态管理
- **DataStore** - 数据持久化
- **Kotlin Serialization** - 数据序列化

## 项目结构

```
starsky/
├── cache/              # 缓存管理
├── listener/           # 事件监听器
├── model/              # 数据模型
│   ├── AudioInfo       # 音频信息
│   ├── PlayMode        # 播放模式
│   └── PlaybackState  # 播放状态
├── notification/       # 通知管理
├── player/             # 播放器核心
├── preferences/        # 持久化管理
├── service/            # 媒体会话服务
└── StarSky.kt          # 主入口类
```

## 注意事项

- 使用前必须调用初始化方法进行初始化
- 通知功能需要 FOREGROUND_SERVICE 权限
- 建议在 Application 或 MainActivity 中初始化
- 不再使用时调用释放方法释放资源
- 网络错误检测采用智能防抖机制，缓冲超过 3 秒且无进度变化时才判定为网络错误
- 缓存使用 LRU 策略，默认最大缓存大小为 512MB

## 版本历史

### 1.1.0
- 添加完整的 API 文档
- 添加详细的使用示例（传统 View 和 Jetpack Compose）
- 优化 JitPack 发布配置，只发布库模块
- 改进 README 文档结构

### 1.0.0
- 基于 AndroidX Media3 的完整音频播放功能
- 支持单首音频和播放列表播放
- 完整的播放控制功能
- 多种播放模式支持
- 音量和速度控制
- 队列管理功能
- 缓存状态监控
- 状态监听和 StateFlow 支持
- 系统通知和媒体会话集成
- 缓存管理（LRU 策略）
- 播放状态持久化
- 灵活的 Service 配置
- Service 连接监听器

## 发布说明

本项目通过 JitPack 发布，版本格式为：`com.github.cycling02:StarSkyAudio:1.1.0`

### 发布新版本

1. 更新 `starsky/build.gradle.kts` 中的版本号
2. 创建并推送 Git tag：
   ```bash
   git tag -a v1.1.0 -m "Release version 1.1.0"
   git push origin v1.1.0
   ```
3. 在 [JitPack](https://jitpack.io/) 上查看构建状态
4. 构建成功后即可使用新版本

## 许可证

MIT License
