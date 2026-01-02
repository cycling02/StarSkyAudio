# StarSky Audio

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
    implementation("com.github.cycling:StarSkyAudio:1.0.0")
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

### Service 配置

StarSky 提供了灵活的 Service 配置选项：

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

### 播放单首音频

创建 AudioInfo 对象，包含歌曲 ID、URL、名称、艺术家、专辑和封面等信息，然后调用播放方法。

### 播放播放列表

创建 AudioInfo 列表，指定起始索引，调用播放列表方法。

### 播放控制

提供播放、暂停、停止、跳转、上一首、下一首等控制方法。可以获取当前播放列表和播放索引，也可以获取底层 ExoPlayer 实例进行高级操作。

### 播放模式

支持三种播放模式：列表循环、单曲循环和随机播放，可通过相应方法切换。

### 音量和速度控制

支持音量调节（0.0 - 1.0）和播放速度调节（0.5x - 2.0x），提供获取和设置方法。

### 队列管理

提供完整的队列管理功能：
- 添加歌曲到列表末尾
- 在指定位置插入歌曲
- 删除指定索引的歌曲
- 清空整个播放列表
- 获取当前播放列表和播放索引

### 缓存状态监控

提供实时缓存状态监控功能：
- 获取当前已缓存到的位置（毫秒）
- 检查播放器是否正在缓冲
- 检查指定歌曲是否正在缓冲
- 检测网络错误（智能防抖，缓冲超过 3 秒且无进度变化时判定为网络错误）

### 状态监听

通过监听器接口监听各种状态变化，包括播放状态、当前音频、播放进度、播放模式和错误等。也支持使用 StateFlow 进行响应式状态监听，包括播放状态、当前音频、播放模式、播放进度、播放时长、是否正在播放、当前播放列表和当前播放索引等。

### 通知功能

提供启用和禁用通知的方法，可在系统通知栏显示播放信息并提供控制按钮。

### 缓存管理

提供获取缓存大小和清理缓存的方法，支持自动缓存已播放的音频，使用 LRU 策略管理缓存空间。

### 播放状态持久化

支持恢复和清除持久化的播放状态，包括音频信息、播放列表、播放位置、播放模式、音量和播放速度等。

### 释放资源

在不再需要使用播放器时，调用释放方法释放相关资源。

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

本项目通过 JitPack 发布，版本格式为：`com.github.cycling:StarSkyAudio:1.0.0`

### 发布新版本

1. 更新 `starsky/build.gradle.kts` 中的版本号
2. 创建并推送 Git tag：
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```
3. 在 [JitPack](https://jitpack.io/) 上查看构建状态
4. 构建成功后即可使用新版本

## 许可证

MIT License
