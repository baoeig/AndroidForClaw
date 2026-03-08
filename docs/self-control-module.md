# Self-Control Module - 完整文档

## 📚 概述

**Self-Control Module** 是 PhoneForClaw 的一个独立 Android Library Module，提供了让 AI Agent 控制和管理应用自身的能力。

**核心理念**: 让 AI Agent 具备自我认知、自我调优、自我诊断的能力，实现 "AI 开发 AI" 的目标。

---

## 🎯 设计目标

### 为什么需要 Self-Control？

在 AgentLoop 架构中，AI Agent 通过 Skills 执行各种操作。但传统 Skills 都是面向外部系统的（点击屏幕、打开应用等），缺乏对自身的控制能力。

Self-Control Module 填补了这个空白：

```
传统 Skills:           Self-Control Skills:
┌─────────────┐       ┌──────────────────┐
│ 操作手机系统  │       │ 操作 PhoneForClaw │
│ 控制其他应用  │       │ 控制自己          │
│ 面向外部     │       │ 面向内部          │
└─────────────┘       └──────────────────┘
```

### 使用场景

1. **自我开发迭代**
   - Agent 发现功能不足 → 查看配置 → 修改参数 → 测试验证

2. **自动化调优**
   - Agent 检测性能问题 → 查询日志 → 分析瓶颈 → 调整参数

3. **远程管理**
   - 用户通过聊天指令 → Agent 打开配置页面 → 修改设置

4. **自我诊断**
   - 操作失败 → Agent 查询日志 → 分析原因 → 提供建议

---

## 🏗️ 模块结构

### 文件组织

```
self-control/
├── build.gradle                    # Module 配置
├── proguard-rules.pro              # ProGuard 规则
├── consumer-rules.pro              # Consumer ProGuard 规则
├── .gitignore                      # Git 忽略规则
├── README.md                       # 模块说明文档
├── INTEGRATION.md                  # 集成指南
├── self-control-skill.md           # Skill 使用文档（供 LLM 参考）
└── src/main/
    ├── AndroidManifest.xml         # Manifest
    └── java/com/xiaomo/androidforclaw/selfcontrol/
        ├── NavigationSkill.kt      # 页面导航 Skill
        ├── ConfigSkill.kt          # 配置管理 Skill
        ├── ServiceControlSkill.kt  # 服务控制 Skill
        ├── LogQuerySkill.kt        # 日志查询 Skill
        ├── SelfControlRegistry.kt  # Skill 注册器
        └── SelfControlDemoActivity.kt  # 演示 Activity
```

### 核心类说明

| 类名 | 职责 | 公开 API |
|------|------|---------|
| `NavigationSkill` | 页面跳转 | `execute("navigate_app", args)` |
| `ConfigSkill` | 配置读写 | `execute("manage_config", args)` |
| `ServiceControlSkill` | 服务管理 | `execute("control_service", args)` |
| `LogQuerySkill` | 日志查询 | `execute("query_logs", args)` |
| `SelfControlRegistry` | 统一管理 | `getAllToolDefinitions()`, `execute()` |

---

## 📦 集成步骤

### 快速集成（3 步）

#### 1. 添加 Module 依赖

**settings.gradle**:
```gradle
include ':self-control'
```

**app/build.gradle**:
```gradle
dependencies {
    implementation project(':self-control')
}
```

#### 2. 集成到 SkillRegistry

```kotlin
class SkillRegistry(private val context: Context) {
    private val skills = mutableMapOf<String, Skill>()
    private val selfControlRegistry = SelfControlRegistry(context)  // ✅

    fun getAllToolDefinitions(): List<ToolDefinition> {
        return skills.values.map { it.getToolDefinition() } +
               selfControlRegistry.getAllToolDefinitions()  // ✅
    }

    suspend fun execute(name: String, args: Map<String, Any?>): SkillResult {
        // 优先检查 Self-Control Skills
        return selfControlRegistry.execute(name, args)  // ✅
            ?: skills[name]?.execute(args)
            ?: SkillResult.error("Unknown skill: $name")
    }
}
```

#### 3. 构建和测试

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew installDebug
```

**验证日志**:
```bash
adb logcat | grep SelfControl
```

预期输出：
```
SkillRegistry: Self-Control skills: [navigate_app, manage_config, control_service, query_logs]
```

---

## 🎯 功能详解

### 1. NavigationSkill - 页面导航

**功能**: 跳转到应用内各个页面

**可用页面**:
| 页面 ID | Activity | 用途 |
|---------|----------|------|
| `main` | MainActivity | 主界面 |
| `config` | ConfigActivity | 配置页面 |
| `permissions` | PermissionsActivity | 权限管理 |
| `chat_history` | ChatHistoryActivity | 对话历史 |
| `chat_log` | ChatLogActivity | 详细日志 |
| `feishu` | FeishuChannelActivity | 飞书通道 |
| `channels` | ChannelListActivity | 通道列表 |
| `result` | ResultActivity | 结果展示 |

**示例**:
```kotlin
// 打开配置页面
val result = registry.execute("navigate_app", mapOf("page" to "config"))

// 带额外参数跳转
val result = registry.execute("navigate_app", mapOf(
    "page" to "chat_log",
    "extras" to mapOf("session_id" to "123")
))
```

### 2. ConfigSkill - 配置管理

**功能**: 读取和修改 MMKV 配置

**操作类型**:
- `get`: 读取单个配置
- `set`: 设置单个配置
- `list`: 列出分类配置
- `delete`: 删除配置

**配置分类**:
- `agent`: Agent 运行参数
- `api`: API 设置
- `ui`: UI 偏好
- `feature`: 功能开关
- `perf`: 性能参数

**示例**:
```kotlin
// 读取
registry.execute("manage_config", mapOf(
    "operation" to "get",
    "key" to "exploration_mode"
))

// 设置
registry.execute("manage_config", mapOf(
    "operation" to "set",
    "key" to "exploration_mode",
    "value" to "true"
))

// 列出功能开关
registry.execute("manage_config", mapOf(
    "operation" to "list",
    "category" to "feature"
))
```

### 3. ServiceControlSkill - 服务控制

**功能**: 控制悬浮窗和后台服务

**操作类型**:
- `show_float`: 显示悬浮窗
- `hide_float`: 隐藏悬浮窗
- `start_float`: 启动服务
- `stop_float`: 停止服务
- `check_status`: 检查状态

**典型使用**: 截图前隐藏悬浮窗

```kotlin
// 隐藏悬浮窗
registry.execute("control_service", mapOf("operation" to "hide_float"))

// 延迟确保 UI 更新
delay(100)

// 执行截图
DeviceController.getScreenshot(context)

// 延迟显示悬浮窗
registry.execute("control_service", mapOf(
    "operation" to "show_float",
    "delay_ms" to 500
))
```

### 4. LogQuerySkill - 日志查询

**功能**: 查询应用运行日志

**日志级别**: V, D, I, W, E, F

**参数**:
- `level`: 日志级别（默认 I）
- `filter`: 过滤关键字
- `lines`: 返回行数（1-200，默认 100）
- `source`: 日志来源（logcat/file）

**示例**:
```kotlin
// 查询错误日志
registry.execute("query_logs", mapOf(
    "level" to "E",
    "lines" to 50
))

// 搜索特定 TAG
registry.execute("query_logs", mapOf(
    "level" to "D",
    "filter" to "AgentLoop",
    "lines" to 100
))
```

---

## 🔄 典型工作流

### 工作流 1: 截图操作（最佳实践）

```kotlin
suspend fun screenshotWithFloatingWindowControl() {
    // 1. 隐藏悬浮窗
    registry.execute("control_service", mapOf("operation" to "hide_float"))

    // 2. 等待 UI 更新
    delay(100)

    // 3. 执行截图
    val screenshot = DeviceController.getScreenshot(context)

    // 4. 延迟显示悬浮窗（避免干扰）
    registry.execute("control_service", mapOf(
        "operation" to "show_float",
        "delay_ms" to 500
    ))

    return screenshot
}
```

### 工作流 2: 自我诊断

```kotlin
suspend fun selfDiagnosis(): String {
    // 1. 检查服务状态
    val status = registry.execute("control_service",
        mapOf("operation" to "check_status"))

    // 2. 查询错误日志
    val errors = registry.execute("query_logs",
        mapOf("level" to "E", "lines" to 50))

    // 3. 分析问题
    val issues = analyzeErrors(errors.content)

    // 4. 如果发现配置问题，尝试修复
    if (issues.contains("screenshot_delay")) {
        registry.execute("manage_config", mapOf(
            "operation" to "set",
            "key" to "screenshot_delay",
            "value" to "200"
        ))
    }

    return "诊断完成: $issues"
}
```

### 工作流 3: 用户配置修改

```kotlin
suspend fun handleUserConfigRequest(request: String) {
    // 1. 打开配置页面
    registry.execute("navigate_app", mapOf("page" to "config"))

    // 2. 列出当前配置
    val configs = registry.execute("manage_config",
        mapOf("operation" to "list", "category" to "feature"))

    // 3. 根据用户请求修改
    when (request) {
        "启用探索模式" -> {
            registry.execute("manage_config", mapOf(
                "operation" to "set",
                "key" to "exploration_mode",
                "value" to "true"
            ))
        }
        // ... 其他配置
    }

    // 4. 验证修改
    val verify = registry.execute("manage_config",
        mapOf("operation" to "get", "key" to "exploration_mode"))

    return "配置已更新: ${verify.content}"
}
```

---

## 🎓 最佳实践

### 1. 配置修改原则

✅ **应该做的**:
- 修改前读取原值
- 修改后验证结果
- 记录修改操作
- 提供回滚机制

❌ **不应该做的**:
- 猜测性修改配置
- 修改敏感配置（api_key）
- 频繁修改配置
- 不验证直接修改

### 2. 日志查询优化

✅ **应该做的**:
- 使用 filter 精确查找
- 根据问题选择日志级别
- 限制合理的行数
- 仅在诊断时查询

❌ **不应该做的**:
- 每个步骤都查日志
- 查询过多行数（>200）
- 不加 filter 全量查询
- 在正常流程中查日志

### 3. 服务控制规范

✅ **应该做的**:
- hide_float 后必须 show_float
- 使用 delay_ms 延迟显示
- 检查操作结果
- 优雅处理失败

❌ **不应该做的**:
- 只隐藏不显示
- 立即显示悬浮窗
- 忽略失败结果
- 频繁切换状态

### 4. 页面导航建议

✅ **应该做的**:
- 跳转前说明原因
- 跳转后等待用户操作
- 传递必要的 extras
- 处理跳转失败

❌ **不应该做的**:
- 频繁跳转页面
- 跳转后立即返回
- 传递无效参数
- 不处理异常

---

## 🔒 安全考虑

### 权限要求

| 操作 | 需要权限 | 说明 |
|------|---------|------|
| `navigate_app` | 无 | Activity 跳转 |
| `manage_config` | 无 | MMKV 读写 |
| `control_service` | SYSTEM_ALERT_WINDOW | 悬浮窗权限 |
| `query_logs` (logcat) | System UID | 读取 logcat |
| `query_logs` (file) | READ_EXTERNAL_STORAGE | 读取日志文件 |

### 配置白名单建议

建议限制可修改的配置键：

```kotlin
val ALLOWED_CONFIG_KEYS = setOf(
    // 安全的配置
    "exploration_mode",
    "screenshot_delay",
    "ui_tree_enabled",
    "reasoning_enabled",
    "max_iterations",

    // 禁止修改
    // "api_key",         // 敏感
    // "api_base_url",    // 安全
    // "system_config"    // 系统级
)
```

### 操作审计

所有 Self-Control 操作都记录到日志：

```kotlin
Log.d("SelfControl", "Operation: $name, Args: $args, Result: ${result.success}")
```

---

## 📊 性能分析

### 操作开销

| Skill | 平均耗时 | 内存占用 | 频率建议 |
|-------|---------|---------|---------|
| `navigate_app` | 50-100ms | 低 | 按需 |
| `manage_config` (get) | <10ms | 极低 | 随意 |
| `manage_config` (set) | <20ms | 低 | 谨慎 |
| `control_service` | 20-50ms | 低 | 按需 |
| `query_logs` (logcat) | 100-500ms | 中 | 诊断时 |
| `query_logs` (file) | 50-200ms | 中 | 诊断时 |

### 优化建议

1. **批量操作** - 一次性完成多个配置修改
2. **缓存结果** - 避免重复查询相同配置
3. **延迟日志查询** - 只在必要时查日志
4. **异步执行** - 不阻塞主流程

---

## 🧪 测试

### 单元测试示例

```kotlin
class SelfControlSkillsTest {
    private lateinit var context: Context
    private lateinit var registry: SelfControlRegistry

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        registry = SelfControlRegistry(context)
    }

    @Test
    fun testNavigationSkill() = runBlocking {
        val result = registry.execute("navigate_app", mapOf("page" to "config"))
        assertTrue(result?.success == true)
    }

    @Test
    fun testConfigSkill() = runBlocking {
        // Set
        val setResult = registry.execute("manage_config", mapOf(
            "operation" to "set",
            "key" to "test_key",
            "value" to "test_value"
        ))
        assertTrue(setResult?.success == true)

        // Get
        val getResult = registry.execute("manage_config", mapOf(
            "operation" to "get",
            "key" to "test_key"
        ))
        assertTrue(getResult?.content?.contains("test_value") == true)

        // Delete
        registry.execute("manage_config", mapOf(
            "operation" to "delete",
            "key" to "test_key"
        ))
    }

    @Test
    fun testServiceControlSkill() = runBlocking {
        val result = registry.execute("control_service",
            mapOf("operation" to "check_status"))
        assertTrue(result?.success == true)
    }

    @Test
    fun testLogQuerySkill() = runBlocking {
        val result = registry.execute("query_logs", mapOf(
            "level" to "I",
            "lines" to 10
        ))
        assertTrue(result != null)
    }
}
```

### 集成测试

```bash
# 测试页面跳转
adb shell am start -n com.xiaomo.androidforclaw/.ui.activity.ConfigActivity

# 测试日志查询
adb logcat -d -t 50 | grep AndroidForClaw
```

---

## 🚀 未来扩展

### 计划新增 Skills

1. **PermissionSkill** - 权限管理
   ```kotlin
   execute("manage_permission", mapOf(
       "operation" to "check",
       "permission" to "SYSTEM_ALERT_WINDOW"
   ))
   ```

2. **PackageManageSkill** - 应用管理
   ```kotlin
   execute("manage_package", mapOf(
       "operation" to "install",
       "apk_path" to "/sdcard/app.apk"
   ))
   ```

3. **NetworkSkill** - 网络诊断
   ```kotlin
   execute("diagnose_network", mapOf(
       "target" to "api.openai.com"
   ))
   ```

4. **StorageSkill** - 存储管理
   ```kotlin
   execute("manage_storage", mapOf(
       "operation" to "clear_cache"
   ))
   ```

5. **TaskSkill** - 定时任务
   ```kotlin
   execute("schedule_task", mapOf(
       "action" to "screenshot",
       "cron" to "0 */10 * * * *"
   ))
   ```

### 架构演进

```
当前:                      未来:
SelfControlRegistry       SelfControlRegistry
├── NavigationSkill            ├── Core/
├── ConfigSkill                │   ├── NavigationSkill
├── ServiceControlSkill        │   ├── ConfigSkill
└── LogQuerySkill              │   └── ServiceControlSkill
                               ├── Diagnostic/
                               │   ├── LogQuerySkill
                               │   └── NetworkSkill
                               ├── Management/
                               │   ├── PermissionSkill
                               │   └── PackageSkill
                               └── Automation/
                                   └── TaskSkill
```

---

## 📝 参考文档

- **README.md** - 模块说明
- **INTEGRATION.md** - 集成指南
- **self-control-skill.md** - Skill 使用文档（供 LLM）
- **CLAUDE.md** - 项目架构文档

---

## 🎯 总结

Self-Control Module 让 PhoneForClaw 的 AI Agent 具备了：

1. **自我认知** - 通过日志查询了解运行状态
2. **自我调优** - 通过配置管理调整参数
3. **自我开发** - 通过页面导航进入配置
4. **自我诊断** - 通过错误分析定位问题

这使得 AI Agent 能够：
- 在执行任务时动态调整策略
- 遇到问题时自动诊断和修复
- 根据用户反馈自我优化
- 实现真正的自主闭环

**最终目标**: 让 AI Agent 能够通过 Self-Control Skills 实现完全自主的开发和迭代，达到 "AI 开发 AI" 的愿景。

---

**PhoneForClaw Self-Control Module** 🧠🔧

_Inspired by OpenClaw Skills System - Tools provide capabilities, Skills teach how to use them._
