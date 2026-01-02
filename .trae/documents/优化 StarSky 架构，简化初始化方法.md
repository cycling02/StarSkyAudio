## StarSky 架构优化计划

### 1. 简化 StarSky 初始化方法
- 移除 `init()` 方法，只保留 `with(context: Context): PlayerControl`
- `with()` 方法内部处理初始化逻辑，如果未初始化则创建 PlayerControlImpl
- 这样更简洁，符合"通过 StarSky.with() 即可获取 PlayerControl 对象"的设计

### 2. 更新 MainActivity.kt 初始化调用
- 将 `StarSky.with(this).init(this)` 改为 `StarSky.with(this)` 或 `StarSky.init(this)`
- 简化初始化代码

### 3. 保持现有 API 设计
- StarSky 保留所有播放控制方法的暴露（play, pause, resume, stop, seekTo, next, previous, setPlayMode, setVolume, setSpeed）
- StarSky 保留所有 StateFlow 属性的暴露（playbackState, currentAudio, playMode, playbackPosition, playbackDuration, isPlaying）
- StarSky 保留缓存管理和持久化方法（clearCache, getCacheSize, restorePlaybackState, clearPlaybackState）
- 所有这些方法都通过 `playerControl?.xxx()` 或 `playerControl?.xxx` 委托

### 4. 验证编译和测试
- 运行编译测试，确保没有错误
- 运行单元测试，确保测试通过

### 架构优势
- 符合"直接调用 PlayerControl 的暴露的 API"设计
- 符合"通过 StarSky.with() 即可获取 PlayerControl 对象"设计
- 简化初始化，只保留一个 `with()` 方法
- 保持向后兼容，所有现有 API 继续可用
- 更清晰的职责分离：StarSky 作为单例入口，PlayerControl 作为播放控制接口