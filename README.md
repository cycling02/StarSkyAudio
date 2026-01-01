# StarSky Audio

基于 AndroidX Media3 的音频播放库，提供简单易用的音频播放功能。

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

### 状态监听
- 播放状态监听 (Idle, Buffering, Playing, Paused, Completed, Stopped, Error)
- 当前音频信息监听
- 播放进度监听
- 播放模式监听
- 错误监听

### 通知功能
- 系统通知栏显示播放信息
- 通知栏控制按钮（播放/暂停、上一首/下一首）
- 媒体会话集成，支持系统媒体控制

### 缓存管理
- 自动缓存已播放的音频
- 缓存大小查询
- 缓存清理

### 播放状态持久化
- 自动保存播放状态（音频信息、播放列表、播放位置）
- 自动保存播放设置（播放模式、音量、播放速度）
- 应用重启后恢复上次播放状态
- 支持清除持久化数据

## 快速开始

### 初始化

```kotlin
// 在 Application 或 MainActivity 中初始化
StarSky.init(context)
```

### 播放单首音频

```kotlin
val audioInfo = AudioInfo(
    songId = "1",
    songUrl = "https://example.com/audio.mp3",
    songName = "歌曲名称",
    artist = "艺术家",
    albumName = "专辑名称",
    coverUrl = "https://example.com/cover.jpg"
)

StarSky.play(audioInfo)
```

### 播放播放列表

```kotlin
val audioList = listOf(
    AudioInfo(...),
    AudioInfo(...),
    AudioInfo(...)
)

StarSky.playPlaylist(audioList, startIndex = 0)
```

### 播放控制

```kotlin
// 播放/暂停
StarSky.pause()
StarSky.resume()

// 停止
StarSky.stop()

// 跳转
StarSky.seekTo(30000) // 跳转到 30 秒

// 上一首/下一首
StarSky.next()
StarSky.previous()

// 获取当前播放列表
val playlist = StarSky.getCurrentPlaylist()

// 获取当前播放索引
val currentIndex = StarSky.getCurrentIndex()

// 获取底层 ExoPlayer 实例（高级用法）
val exoPlayer = StarSky.getExoPlayer()
```

### 播放模式

```kotlin
// 列表循环
StarSky.setPlayMode(PlayMode.LOOP)

// 单曲循环
StarSky.setPlayMode(PlayMode.SINGLE_LOOP)

// 随机播放
StarSky.setPlayMode(PlayMode.SHUFFLE)
```

### 音量和速度控制

```kotlin
// 音量 (0.0 - 1.0)
StarSky.setVolume(0.8f)
val volume = StarSky.getVolume()

// 播放速度 (0.5x - 2.0x)
StarSky.setSpeed(1.5f)
val speed = StarSky.getSpeed()
```

### 状态监听

```kotlin
val listener = object : OnPlayerEventListener {
    override fun onPlaybackStateChanged(state: PlaybackState) {
        when (state) {
            PlaybackState.Idle -> TODO("空闲状态")
            PlaybackState.Buffering -> TODO("缓冲中")
            PlaybackState.Playing -> TODO("播放中")
            PlaybackState.Paused -> TODO("已暂停")
            PlaybackState.Completed -> TODO("播放完成")
            PlaybackState.Stopped -> TODO("已停止")
            is PlaybackState.Error -> TODO("错误: ${state.message}")
        }
    }

    override fun onAudioChanged(audioInfo: AudioInfo?) {
        TODO("当前音频: $audioInfo")
    }

    override fun onPlayProgress(position: Long, duration: Long) {
        TODO("播放进度: $position / $duration")
    }

    override fun onPlayModeChanged(mode: PlayMode) {
        TODO("播放模式: $mode")
    }

    override fun onError(message: String, exception: Throwable?) {
        TODO("错误: $message")
    }
}

StarSky.addListener(listener)
StarSky.removeListener(listener)
```

### 使用 StateFlow 监听状态

```kotlin
// 播放状态
StarSky.playbackState.collect { state ->
    // 处理播放状态变化
}

// 当前音频
StarSky.currentAudio.collect { audioInfo ->
    // 处理当前音频变化
}

// 播放模式
StarSky.playMode.collect { mode ->
    // 处理播放模式变化
}

// 播放进度
StarSky.playbackPosition.collect { position ->
    // 处理播放进度变化
}

// 播放时长
StarSky.playbackDuration.collect { duration ->
    // 处理播放时长变化
}

// 是否正在播放
StarSky.isPlaying.collect { isPlaying ->
    // 处理播放状态变化
}
```

### 通知功能

```kotlin
// 启用通知
StarSky.enableNotification()

// 禁用通知
StarSky.disableNotification()
```

### 缓存管理

```kotlin
// 获取缓存大小
val cacheSize = StarSky.getCacheSize()

// 清理缓存
StarSky.clearCache()
```

### 播放状态持久化

```kotlin
// 恢复上次播放状态（在 Application 或 MainActivity 中调用）
lifecycleScope.launch {
    StarSky.restorePlaybackState()
}

// 清除持久化的播放状态
lifecycleScope.launch {
    StarSky.clearPlaybackState()
}
```

### 释放资源

```kotlin
// 在不需要时释放资源
StarSky.release()
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

- 使用前必须调用 `StarSky.init(context)` 进行初始化
- 通知功能需要 `FOREGROUND_SERVICE` 权限
- 建议在 Application 或 MainActivity 中初始化
- 不再使用时调用 `StarSky.release()` 释放资源

## 许可证

MIT License
