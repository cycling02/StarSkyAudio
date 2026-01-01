# StarSky 音乐播放库复刻计划（现代架构）

## 核心功能实现

### 1. 依赖配置

* 添加 media3 ExoPlayer、UI、Session 等核心依赖

* 添加 Kotlin Flow、Coroutines 相关依赖

### 2. 核心数据模型

* `AudioInfo` - 音频信息数据类

* `PlaybackState` - 播放状态 sealed class

* `PlayMode` - 播放模式枚举

### 3. 播放器核心（现代架构）

* `StarSky` - 主入口类（单例，使用 Kotlin 对象）

* `StarSkyPlayer` - 基于 media3 ExoPlayer 的播放器封装

* 使用 `StateFlow` 和 `SharedFlow` 管理播放状态和事件

* 播放控制 API（play、pause、stop、seek、next、previous）

* 播放列表管理（使用 `MutableStateFlow`）

### 4. 通知栏管理（media3 风格）

* 使用 `PlayerNotificationManager` 实现通知栏

* MediaSession 集成

* 支持自定义通知栏样式

### 5. 缓存管理

* 使用 media3 的 CacheDataSource

* 边播边存功能

### 6. 状态管理（Flow-based）

* `StarSkyPlayerState` - 播放器状态类（使用 StateFlow）

* 事件流（使用 SharedFlow）

* 进度流（使用 Flow）

### 8. 工具类和扩展函数

* `AudioUtils` - 音频工具类

* `PlayerExtensions` - ExoPlayer 扩展函数

## 实现步骤

1. 更新 starsky 模块的依赖配置（media3 + Flow + Coroutines）
2. 创建核心数据模型（AudioInfo、PlaybackState 等）
3. 实现播放器核心类（使用 StateFlow 管理状态）
4. 实现通知栏管理（使用 PlayerNotificationManager）
5. 实现缓存管理（使用 CacheDataSource）
6. 创建 Flow-based 的状态管理和事件系统
7. 更新 AndroidManifest.xml 添加必要权限和组件
8. 在 app 模块创建示例代码

