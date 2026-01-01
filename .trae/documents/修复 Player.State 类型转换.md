# 修复 Player.State 类型转换问题

## 问题分析
`onPlaybackStateChanged(playbackState: Int)` 接收的是整数，需要先转换为 `Player.State` 枚举，再转换为 `PlaybackState`。

## 修复方案
在 `StarSkyPlayer.kt` 的 `onPlaybackStateChanged` 方法中：
1. 先将整数 `playbackState` 转换为 `Player.State` 枚举
2. 再将 `Player.State` 转换为 `PlaybackState`
3. 添加完整的 `else` 分支处理未知状态