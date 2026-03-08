# BrowserForClaw 完整指南

> 从安装到集成的一站式指南

---

## 📖 目录

1. [理解 BrowserForClaw](#理解-browserforclaw)
2. [快速开始](#快速开始)
3. [AI Agent 集成](#ai-agent-集成)
4. [HTTP 客户端实现](#http-客户端实现)

---

## 理解 BrowserForClaw

### 🧩 两部分系统

BrowserForClaw 由**两个互补部分**组成：

#### 1️⃣ Android App (能力提供者)

**是什么**:
- 运行在 Android 设备上的应用
- 提供真实浏览器 (WebView) 和自动化控制
- 暴露 HTTP API 服务器 (端口 8765)

**做什么**:
- 接收来自外部应用/Agent 的 HTTP 请求
- 执行浏览器操作 (导航、点击、输入等)
- 返回 JSON 格式结果

**位置**: `android-project/` - 完整 einkbro 浏览器项目

#### 2️⃣ SKILL.md 文件 (知识提供者)

**是什么**:
- Markdown 文件，包含结构化指令
- 教 AI Agent **如何**使用浏览器自动化工具
- 遵循 AgentSkills.io 格式

**包含什么**:
- 13 个浏览器工具的描述
- 何时以及如何使用每个工具
- 常见模式和工作流
- 最佳实践和错误处理
- 示例代码和故障排除

**位置**: `skill/SKILL.md`

### 🔗 协同工作方式

```
┌─────────────────────────────────────┐
│        AI Agent System              │
│                                     │
│  ┌──────────┐      ┌──────────┐   │
│  │ SKILL.md │─加载→│Tool      │   │
│  │ (知识)   │      │Registry  │   │
│  └──────────┘      └────┬─────┘   │
│                          │         │
│  用户: "搜索 Google"      │         │
│      ↓                   │         │
│  AI 决策:                │         │
│  1. browser_navigate ────┼─→ HTTP │
│  2. browser_type     ────┼─→ HTTP │
│  3. browser_press    ────┼─→ HTTP │
└──────────────────────────┼─────────┘
                           ↓ :8765
              ┌────────────────────┐
              │ BrowserForClaw App │
              │ (Android 设备)      │
              │  - HTTP Server     │
              │  - 13 Tools        │
              │  - WebView         │
              └────────────────────┘
```

**关键理解**:
- Android App = 提供**能力** (HTTP API)
- SKILL.md = 提供**知识** (如何使用)
- AI Agent = **能力** + **知识** = **智能自动化**

---

## 快速开始

### 前置要求

- Android 7.0+ 设备或模拟器 (API 24+)
- ADB 工具 (开发用)
- BrowserForClaw APK

### 步骤 1: 安装应用

**选项 A: 直接下载**
```bash
# 从 releases/ 下载 APK
adb install BrowserForClaw-v0.5.1.apk
```

**选项 B: 从源码编译**
```bash
cd android-project
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-universal-release.apk
```

### 步骤 2: 启动和测试

```bash
# 启动应用 (HTTP 服务器自动在 8765 端口启动)
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity

# 设置端口转发 (开发环境)
adb forward tcp:8765 tcp:8765

# 健康检查
curl http://localhost:8765/health
# 输出: {"status":"ok"}

# 测试导航
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_navigate","args":{"url":"https://example.com"}}'

# 执行 JavaScript
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_execute","args":{"script":"return document.title"}}'
```

### 步骤 3: 验证工具

测试 13 个浏览器工具:

```bash
# 导航
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_navigate","args":{"url":"https://google.com"}}'

# 等待元素
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_wait","args":{"selector":"input[name=q]","timeMs":5000}}'

# 输入文本
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_type","args":{"selector":"input[name=q]","text":"test"}}'

# 按键
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_press","args":{"key":"Enter"}}'

# 获取内容
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_get_content","args":{"format":"text"}}'
```

完整工具列表参见 [API.md](./API.md)

---

## AI Agent 集成

### 方式 1: 使用 SKILL.md (推荐)

适用于支持 AgentSkills.io 格式的 AI Agent 框架 (如 phoneforclaw)。

**步骤**:

1. **复制 Skill 文件**
```bash
# 方式 A: 使用预打包的 zip
unzip BrowserForClaw-Skill-v0.5.1.zip
cp SKILL.md /path/to/your-agent/skills/browser-automation/

# 方式 B: 直接复制
cp skill/SKILL.md /path/to/your-agent/skills/browser-automation/
```

2. **Agent 自动加载**
   - Agent 启动时会扫描 skills/ 目录
   - 自动加载 SKILL.md
   - AI 学会所有浏览器工具的使用方法

3. **开始使用**
```kotlin
// AI Agent 现在可以理解这样的指令
agent.ask("帮我在百度搜索 OpenClaw 的最新消息")

// Agent 内部自动:
// 1. 检查 browser 是否运行，如果没有则通过 Intent 启动
// 2. browser_navigate("https://baidu.com")
// 3. browser_wait(selector: "#kw")
// 4. browser_type("#kw", "OpenClaw 最新消息")
// 5. browser_press("Enter")
// 6. browser_wait(timeMs: 2000)
// 7. browser_get_content(format: "text")
// 8. 分析结果并返回给用户
```

### 方式 2: 手动实现 HTTP 客户端

如果你的 Agent 框架不支持 Skills 系统，可以手动实现。

---

## HTTP 客户端实现

### 核心客户端类

```kotlin
package com.example.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.Socket

class BrowserHttpClient(
    private val host: String = "localhost",
    private val port: Int = 8765
) {
    data class ToolResult(
        val success: Boolean,
        val data: Map<String, Any?>? = null,
        val error: String? = null
    )

    suspend fun execute(
        tool: String,
        args: Map<String, Any?>
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            // 构建 JSON 请求
            val requestJson = JSONObject().apply {
                put("tool", tool)
                put("args", JSONObject(args))
            }.toString()

            // 建立 Socket 连接
            val socket = Socket(host, port)
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // 发送 HTTP POST 请求
            writer.println("POST /api/browser/execute HTTP/1.1")
            writer.println("Host: $host:$port")
            writer.println("Content-Type: application/json")
            writer.println("Content-Length: ${requestJson.length}")
            writer.println()
            writer.print(requestJson)
            writer.flush()

            // 读取响应头
            var line = reader.readLine()
            while (line != null && line.isNotEmpty()) {
                line = reader.readLine()
            }

            // 读取响应体
            val responseBody = StringBuilder()
            while (reader.ready()) {
                responseBody.append(reader.readLine())
            }

            // 解析 JSON 响应
            val json = JSONObject(responseBody.toString())
            val success = json.optBoolean("success", false)
            val error = json.optString("error", null)
            val dataJson = json.optJSONObject("data")

            val data = dataJson?.let { jsonToMap(it) }

            socket.close()

            ToolResult(success, data, error)
        } catch (e: Exception) {
            ToolResult(false, error = "Request failed: ${e.message}")
        }
    }

    private fun jsonToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        json.keys().forEach { key ->
            map[key] = json.opt(key)
        }
        return map
    }
}
```

### 便捷扩展函数

```kotlin
// 导航
suspend fun BrowserHttpClient.navigate(url: String) =
    execute("browser_navigate", mapOf("url" to url))

// 点击
suspend fun BrowserHttpClient.click(selector: String) =
    execute("browser_click", mapOf("selector" to selector))

// 输入文本
suspend fun BrowserHttpClient.type(selector: String, text: String) =
    execute("browser_type", mapOf("selector" to selector, "text" to text))

// 等待元素
suspend fun BrowserHttpClient.waitForSelector(selector: String, timeMs: Long = 5000) =
    execute("browser_wait", mapOf("selector" to selector, "timeMs" to timeMs))

// 按键
suspend fun BrowserHttpClient.press(key: String) =
    execute("browser_press", mapOf("key" to key))

// 获取内容
suspend fun BrowserHttpClient.getContent(format: String = "text") =
    execute("browser_get_content", mapOf("format" to format))

// 执行 JavaScript
suspend fun BrowserHttpClient.executeJS(script: String) =
    execute("browser_execute", mapOf("script" to script))

// 截图
suspend fun BrowserHttpClient.screenshot(fullPage: Boolean = false) =
    execute("browser_screenshot", mapOf("fullPage" to fullPage))
```

### 使用示例

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val browser = BrowserHttpClient()

    // Google 搜索
    browser.navigate("https://google.com")
    browser.waitForSelector("input[name='q']")
    browser.type("input[name='q']", "BrowserForClaw")
    browser.press("Enter")

    // 等待结果加载
    browser.waitForSelector("#search", 3000)

    // 获取结果
    val result = browser.getContent("text")
    println("搜索结果: ${result.data?.get("content")}")

    // 执行自定义 JavaScript
    val titles = browser.executeJS("""
        return Array.from(document.querySelectorAll('h3'))
            .map(el => el.innerText)
            .slice(0, 5)
    """)
    println("标题: ${titles.data?.get("result")}")
}
```

### 集成到 phoneforclaw

**步骤 1: 添加 HTTP 客户端**

将上面的 `BrowserHttpClient.kt` 添加到:
```
phoneforclaw/app/src/main/java/com/xiaomo/androidforclaw/browser/BrowserHttpClient.kt
```

**步骤 2: 创建 BrowserForClawSkill**

```kotlin
// phoneforclaw: agent/tools/BrowserForClawSkill.kt
class BrowserForClawSkill(
    private val context: Context
) : Skill {
    private val client = BrowserHttpClient()

    override val name = "browser_navigate" // 示例工具
    override val description = "Navigate to URL"

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        // 检查浏览器是否运行
        if (!isBrowserRunning()) {
            launchBrowser()
            delay(2000) // 等待 HTTP 服务器启动
        }

        // 调用 HTTP API
        val result = client.navigate(args["url"] as String)

        return SkillResult(
            success = result.success,
            content = result.data.toString(),
            metadata = result.data ?: emptyMap()
        )
    }

    private fun launchBrowser() {
        val intent = Intent().apply {
            component = ComponentName(
                "info.plateaukao.einkbro",
                "info.plateaukao.einkbro.activity.BrowserActivity"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun isBrowserRunning(): Boolean {
        return try {
            val socket = Socket("localhost", 8765)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

**步骤 3: 注册所有浏览器工具**

```kotlin
// SkillRegistry.kt
fun registerBrowserTools(context: Context) {
    // 13 个工具
    register(BrowserNavigateTool(context))
    register(BrowserClickTool(context))
    register(BrowserTypeTool(context))
    register(BrowserScrollTool(context))
    register(BrowserGetContentTool(context))
    register(BrowserWaitTool(context))
    register(BrowserExecuteTool(context))
    register(BrowserPressTool(context))
    register(BrowserHoverTool(context))
    register(BrowserSelectTool(context))
    register(BrowserScreenshotTool(context))
    register(BrowserGetCookiesTool(context))
    register(BrowserSetCookiesTool(context))
}
```

**步骤 4: 复制 SKILL.md**

```bash
cp skill/SKILL.md /path/to/phoneforclaw/app/src/main/assets/skills/browser-automation/SKILL.md
```

Agent 会自动加载这个 Skill 文件，学习如何使用浏览器。

---

## 架构图

### 完整数据流

```
用户
  ↓ "在百度搜索 AI 新闻"
┌─────────────────────────────────┐
│     phoneforclaw Agent          │
│                                 │
│  1. 加载 browser-automation     │
│     Skill 知识                  │
│                                 │
│  2. Claude 分析任务:            │
│     需要: 浏览器搜索             │
│                                 │
│  3. 生成工具调用序列:           │
│     - browser_navigate          │
│     - browser_wait              │
│     - browser_type              │
│     - browser_press             │
│     - browser_get_content       │
│                                 │
│  4. 执行工具:                   │
│     BrowserHttpClient.execute() │
└──────────────┬──────────────────┘
               │ HTTP POST :8765
               ↓
┌─────────────────────────────────┐
│  BrowserForClaw (Android)       │
│                                 │
│  SimpleBrowserHttpServer        │
│    ↓ 解析请求                   │
│  BrowserToolsExecutor           │
│    ↓ 路由到工具                 │
│  BrowserNavigateTool.execute()  │
│    ↓ 控制 WebView               │
│  BrowserManager                 │
│    ↓ JavaScript 执行            │
│  Android WebView                │
│    ↓ 渲染页面                   │
│  [真实浏览器]                   │
│    ↓ 返回结果                   │
│  JSON Response                  │
└──────────────┬──────────────────┘
               │ HTTP Response
               ↓
         phoneforclaw Agent
               ↓ 分析结果
         返回给用户
```

---

## 常见问题

### Q: HTTP 服务器未启动？

```bash
# 查看日志
adb logcat | grep BrowserHttpServer
# 应该看到: ✅ HTTP Server started on port 8765

# 重启应用
adb shell am force-stop info.plateaukao.einkbro
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity
```

### Q: 连接被拒绝？

```bash
# 检查端口转发
adb forward --list
# 应该看到: tcp:8765 tcp:8765

# 重新设置
adb forward --remove tcp:8765
adb forward tcp:8765 tcp:8765

# 测试连接
curl http://localhost:8765/health
```

### Q: 工具执行失败？

- 确保页面已加载完成 (使用 `browser_wait`)
- 验证 CSS 选择器正确
- 检查日志: `adb logcat | grep BrowserTools`
- JavaScript 密集型网站需要更长等待时间

### Q: AI Agent 如何自动启动浏览器？

使用 Intent:

```kotlin
val intent = Intent().apply {
    component = ComponentName(
        "info.plateaukao.einkbro",
        "info.plateaukao.einkbro.activity.BrowserActivity"
    )
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
context.startActivity(intent)
delay(2000) // 等待 HTTP 服务器启动
```

### Q: 可以远程控制吗？

可以！通过网络访问：

```bash
# 在设备上 (获取 IP 地址)
adb shell ip addr show wlan0

# 在其他设备上 (使用设备 IP)
curl http://192.168.1.100:8765/health
```

注意: 默认无认证，仅在可信网络使用。

---

## 13 个浏览器工具

| 类别 | 工具 | 说明 |
|------|------|------|
| **导航** | `browser_navigate` | 导航到 URL |
| **交互** | `browser_click` | 点击元素 |
|  | `browser_type` | 输入文本 |
|  | `browser_hover` | 鼠标悬停 |
|  | `browser_select` | 选择下拉选项 |
| **内容** | `browser_get_content` | 获取页面内容 (text/html/markdown) |
|  | `browser_screenshot` | 页面截图 (base64) |
| **键盘** | `browser_press` | 按键 (Enter/Escape/Tab...) |
| **滚动** | `browser_scroll` | 滚动页面 (up/down/top/bottom) |
| **等待** | `browser_wait` | 等待元素/文本/URL/时间 |
| **JavaScript** | `browser_execute` | 执行自定义 JavaScript |
| **Cookie** | `browser_get_cookies` | 获取 Cookie |
|  | `browser_set_cookies` | 设置 Cookie |

详细 API 参考: [API.md](./API.md)

---

## 核心模式

所有浏览器操作遵循此模式:

```
Navigate → Wait → Interact → Extract
```

### 示例 1: Google 搜索

```javascript
1. browser_navigate("https://google.com")
2. browser_wait({"selector": "input[name='q']", "timeMs": 5000})
3. browser_type("input[name='q']", "search query")
4. browser_press("Enter")
5. browser_wait({"timeMs": 2000})
6. browser_get_content({"format": "text"})
```

### 示例 2: 表单填写

```javascript
1. browser_navigate("https://example.com/form")
2. browser_wait({"selector": "#name"})
3. browser_type("#name", "John Doe")
4. browser_type("#email", "john@example.com")
5. browser_select("#country", ["US"])
6. browser_click("#submit")
7. browser_wait({"text": "Success"})
```

### 示例 3: 数据提取

```javascript
1. browser_navigate("https://news.site.com")
2. browser_wait({"selector": ".article"})
3. browser_execute("""
    return Array.from(document.querySelectorAll('.article'))
        .map(el => ({
            title: el.querySelector('.title').innerText,
            link: el.querySelector('a').href
        }))
""")
```

---

## 重要规则

### 1. 始终在导航后等待
```javascript
// ❌ 错误
browser_navigate("https://example.com")
browser_click("#button")  // 元素可能还没加载！

// ✅ 正确
browser_navigate("https://example.com")
browser_wait({"selector": "#button"})
browser_click("#button")
```

### 2. 使用正确的 CSS 选择器
- ID: `#submit-button`
- Class: `.btn-primary`
- Attribute: `[data-testid='login']`
- Name: `input[name='email']`

### 3. JavaScript 必须返回值
```javascript
// ❌ 错误
browser_execute("console.log(document.title)")

// ✅ 正确
browser_execute("return document.title")
```

---

## 故障排除

### 连接问题
1. 确保 BrowserForClaw 应用正在运行
2. 检查端口转发: `adb forward tcp:8765 tcp:8765`
3. 验证服务器启动: `adb logcat | grep BrowserHttpServer`

### 元素未找到
1. 使用 `browser_execute("return document.body.innerHTML")` 检查页面
2. 尝试其他选择器
3. 在交互前添加等待

### 超时错误
1. 增加等待时间: `browser_wait({"timeMs": 10000})`
2. 检查网络连接
3. 验证页面在手动测试中能加载

### JavaScript 错误
1. 确保脚本返回值
2. 检查 JS 语法
3. 先在浏览器控制台测试脚本

---

## 下一步

- **API 详细文档**: [API.md](./API.md)
- **常见问题**: [FAQ.md](./FAQ.md)
- **能力增强**: [ENHANCEMENT.md](./ENHANCEMENT.md)

---

**BrowserForClaw v0.5.1** - Browser Automation for AI Agents 🌐🤖
