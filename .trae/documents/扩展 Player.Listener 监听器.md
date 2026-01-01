# 扩展 Player.Listener 监听器

## 当前实现
- ✅ onPlaybackStateChanged - 播放状态变化
- ✅ onIsPlayingChanged - 播放/暂停状态
- ✅ onMediaItemTransition - 媒体切换
- ✅ onPlayerError - 播放错误

## 建议添加的监听器

### 高优先级
1. **onMediaMetadataChanged** - 监听元数据变化（标题、艺术家、专辑等）
2. **onPlayWhenReadyChanged** - 监听 playWhenReady 状态变化
3. **onPlaybackParametersChanged** - 监听播放参数变化（速度、音量等）
4. **onRepeatModeChanged** - 监听重复模式变化
5. **onShuffleModeChanged** - 监听随机模式变化

### 中优先级
6. **onPositionDiscontinuity** - 监听位置不连续变化
7. **onMaxSeekToPreviousPositionChanged** - 监听最大可跳转位置变化
8. **onPlaylistMetadataChanged** - 监听播放列表元数据变化

### 低优先级
9. **onAudioAttributesChanged** - 监听音频属性变化
10. **onAvailableCommandsChanged** - 监听可用命令变化
11. **onIsLoadingChanged** - 监听加载状态变化
12. **onDeviceVolumeChanged** - 监听设备音量变化

## 实现方案
1. 在 StarSkyPlayer 中添加这些监听器的实现
2. 在 OnPlayerEventListener 接口中添加对应的回调方法
3. 在 notifyXxx 方法中调用对应的监听器方法
4. 更新 PlaybackState 添加更多状态（如 Loading）
5. 添加播放参数变化的 StateFlow