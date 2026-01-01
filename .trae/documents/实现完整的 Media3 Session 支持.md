# Media3 Session 完整实现计划

## 1. 创建 MediaSessionService
- 创建 `StarSkyMediaSessionService` 继承 `MediaSessionService`
- 在 Service 中管理 MediaSession 生命周期
- 实现前台服务以支持后台播放

## 2. 重构 StarSkyNotificationManager
- 移除 MediaSession 创建逻辑（由 Service 管理）
- 只负责通知栏的显示和更新
- 添加通知图标和停止按钮配置

## 3. 更新 StarSkyPlayer
- 添加与 MediaSessionService 的通信
- 确保 Player 状态正确同步到 MediaSession
- 添加 `setCallback` 来接收外部控制

## 4. 更新 AndroidManifest
- 注册 MediaSessionService
- 添加前台服务权限
- 配置通知渠道

## 5. 更新 MainActivity
- 启动 MediaSessionService
- 正确处理服务绑定和生命周期