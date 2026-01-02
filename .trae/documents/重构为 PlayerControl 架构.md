## PlayerControl 架构重构计划

### 1. 创建 PlayerControl 接口
- 定义播放控制接口，包含所有播放方法
- 包含状态监听方法
- 包含通知控制方法

### 2. 创建 PlayerControlImpl 实现类
- 实现 PlayerControl 接口
- 内部持有 StarSkyPlayer 实例
- 委托所有调用给 StarSkyPlayer

### 3. 修改 StarSky 单例
- 移除直接播放控制方法
- 添加 `with(context: Context)` 方法，返回 PlayerControl 实例
- 保留缓存管理和持久化方法
- 保持 StateFlow 暴露

### 4. 更新 MainActivity.kt
- 使用 `StarSky.with(context)` 获取 PlayerControl
- 更新所有播放控制调用

### 5. 更新单元测试
- 创建 PlayerControlTest 测试
- 更新 StarSkyTest 测试

### 6. 更新 README.md
- 更新 API 使用示例
- 说明新的架构设计

### 架构优势
- 解耦播放控制和单例访问
- 支持依赖注入
- 更好的测试性
- 更清晰的职责分离