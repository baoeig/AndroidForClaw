# BrowserForClaw 工具参考手册

**版本**: v0.4.0
**日期**: 2026-03-06
**工具总数**: 13 个

---

## 📋 工具列表

| # | 工具名称 | 功能 | 优先级 |
|---|---------|------|--------|
| 1 | browser_navigate | 导航到 URL | ⭐⭐⭐⭐⭐ |
| 2 | browser_click | 点击元素 | ⭐⭐⭐⭐⭐ |
| 3 | browser_type | 输入文本 | ⭐⭐⭐⭐⭐ |
| 4 | browser_scroll | 滚动页面 | ⭐⭐⭐⭐ |
| 5 | browser_get_content | 获取页面内容 | ⭐⭐⭐⭐⭐ |
| 6 | browser_wait | 等待条件 | ⭐⭐⭐⭐⭐ |
| 7 | browser_execute | 执行 JavaScript | ⭐⭐⭐⭐⭐ |
| 8 | browser_press | 按键 | ⭐⭐⭐⭐ |
| 9 | browser_hover | 悬停元素 | ⭐⭐⭐ |
| 10 | browser_select | 选择下拉框 | ⭐⭐⭐ |
| 11 | browser_screenshot | 截图 | ⭐⭐⭐⭐ |
| 12 | browser_get_cookies | 获取 Cookies | ⭐⭐⭐ |
| 13 | browser_set_cookies | 设置 Cookies | ⭐⭐⭐ |

---

## 🔧 工具详解

### 1. browser_navigate

**功能**: 导航到指定 URL

**参数**:
```json
{
  "url": "https://google.com",  // 必需: 目标 URL
  "waitMs": 500                 // 可选: 等待加载的毫秒数，默认 500
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "url": "https://google.com",
    "currentUrl": "https://google.com"
  }
}
```

**示例**:
```kotlin
browserNavigate("https://google.com")
browserNavigate("example.com", waitMs = 1000)  // 自动添加 https://
```

---

### 2. browser_click

**功能**: 点击页面元素

**参数**:
```json
{
  "selector": "#search-button"  // 必需: CSS 选择器
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "selector": "#search-button",
    "clicked": true
  }
}
```

**示例**:
```kotlin
browserClick("#login-button")
browserClick("button.primary")
browserClick("[data-testid='submit']")
```

---

### 3. browser_type

**功能**: 在输入框中输入文本

**参数**:
```json
{
  "selector": "input[name='q']",  // 必需: CSS 选择器
  "text": "Hello World",          // 必需: 要输入的文本
  "submit": false                 // 可选: 是否提交表单，默认 false
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "selector": "input[name='q']",
    "text": "Hello World",
    "submitted": false
  }
}
```

**示例**:
```kotlin
browserType("#email", "user@example.com")
browserType("input[name='password']", "secret123", submit = true)
```

---

### 4. browser_scroll

**功能**: 滚动页面

**参数**:
```json
{
  "direction": "down",  // 可选: "down", "up", "top", "bottom"，默认 "down"
  "amount": 500         // 可选: 滚动像素数（仅 down/up 有效）
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "direction": "down",
    "scrolled": true
  }
}
```

**示例**:
```kotlin
browserScroll("down")              // 向下滚动一屏
browserScroll("down", amount = 500) // 向下滚动 500px
browserScroll("top")               // 滚动到顶部
browserScroll("bottom")            // 滚动到底部
```

---

### 5. browser_get_content

**功能**: 获取页面内容

**参数**:
```json
{
  "format": "text"  // 可选: "text" 或 "html"，默认 "text"
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "content": "Page content here...",
    "length": 1234,
    "url": "https://example.com",
    "title": "Example Page",
    "format": "text"
  }
}
```

**示例**:
```kotlin
browserGetContent()                  // 获取纯文本
browserGetContent(format = "html")   // 获取 HTML
```

---

### 6. browser_wait ⭐

**功能**: 等待页面满足特定条件

**参数**:
```json
{
  // 选择一种等待方式:
  "timeMs": 1000,                     // 1. 简单等待时间
  "selector": "#content",             // 2. 等待元素出现
  "text": "Loading complete",         // 3. 等待文本出现
  "textGone": "Loading...",           // 4. 等待文本消失
  "url": "https://example.com/done",  // 5. 等待 URL 包含字符串
  "jsCondition": "document.readyState === 'complete'",  // 6. 自定义 JS 条件

  "timeout": 30000  // 可选: 超时时间（毫秒），默认 30000
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "waitType": "selector",
    "selector": "#content"
  }
}
```

**示例**:
```kotlin
// 等待固定时间
browserWait(timeMs = 2000)

// 等待元素出现
browserWait(selector = "#login-form")

// 等待文本出现
browserWait(text = "Welcome back")

// 等待文本消失
browserWait(textGone = "Loading...")

// 等待 URL 变化
browserWait(url = "dashboard")

// 自定义条件
browserWait(jsCondition = "document.querySelectorAll('.item').length > 5")
```

---

### 7. browser_execute ⭐

**功能**: 执行自定义 JavaScript 代码

**参数**:
```json
{
  "script": "return document.title",  // 必需: JavaScript 代码
  "selector": "#element"              // 可选: 在特定元素上执行
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "result": "Example Page",
    "script": "return document.title",
    "selector": null
  }
}
```

**示例**:
```kotlin
// 页面级别执行
browserExecute("return document.title")
browserExecute("return document.querySelectorAll('a').length")

// 元素级别执行
browserExecute(
  script = "return element.textContent",
  selector = "#content"
)

// 修改页面
browserExecute("document.body.style.backgroundColor = 'yellow'")
```

---

### 8. browser_press

**功能**: 模拟键盘按键

**参数**:
```json
{
  "key": "Enter",   // 必需: 按键名称
  "delayMs": 100    // 可选: 按键后延迟，默认 100
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "key": "Enter",
    "pressed": true
  }
}
```

**支持的按键**:
- `Enter`, `Tab`, `Escape`
- `ArrowDown`, `ArrowUp`, `ArrowLeft`, `ArrowRight`
- `Backspace`, `Delete`
- `Home`, `End`, `PageUp`, `PageDown`
- 任何字母/数字键

**示例**:
```kotlin
browserPress("Enter")
browserPress("Tab")
browserPress("ArrowDown", delayMs = 200)
```

---

### 9. browser_hover

**功能**: 触发鼠标悬停事件

**参数**:
```json
{
  "selector": "#menu-item"  // 必需: CSS 选择器
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "selector": "#menu-item",
    "hovered": true
  }
}
```

**示例**:
```kotlin
browserHover("#dropdown-menu")
browserHover(".tooltip-trigger")
```

---

### 10. browser_select

**功能**: 选择下拉框选项

**参数**:
```json
{
  "selector": "select[name='country']",  // 必需: CSS 选择器 (指向 <select>)
  "values": ["US", "CN"]                 // 必需: 要选择的值列表
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "selector": "select[name='country']",
    "values": ["US", "CN"],
    "selected": true
  }
}
```

**示例**:
```kotlin
// 单选
browserSelect("#country", values = listOf("US"))

// 多选 (如果 select 支持 multiple)
browserSelect("#languages", values = listOf("en", "zh", "ja"))
```

---

### 11. browser_screenshot

**功能**: 截取页面图片

**参数**:
```json
{
  "fullPage": false,  // 可选: 是否全页截图，默认 false
  "format": "png",    // 可选: "png" 或 "jpeg"，默认 "png"
  "quality": 80       // 可选: JPEG 质量 0-100，默认 80
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "screenshot": "iVBORw0KGgoAAAANS...",  // Base64 编码的图片
    "width": 1080,
    "height": 1920,
    "format": "png",
    "size": 123456
  }
}
```

**示例**:
```kotlin
// 当前可见区域
browserScreenshot()

// 全页截图
browserScreenshot(fullPage = true)

// JPEG 格式
browserScreenshot(format = "jpeg", quality = 90)
```

---

### 12. browser_get_cookies

**功能**: 获取当前页面的 Cookies

**参数**: 无

**返回**:
```json
{
  "success": true,
  "data": {
    "cookies": "session_id=abc123; user_id=456",
    "url": "https://example.com"
  }
}
```

**示例**:
```kotlin
browserGetCookies()
```

---

### 13. browser_set_cookies

**功能**: 设置 Cookies

**参数**:
```json
{
  "url": "https://example.com",  // 可选: 目标 URL，默认当前 URL
  "cookies": [                   // 必需: Cookie 列表
    "session_id=abc123; path=/; domain=.example.com",
    "user_id=456; path=/"
  ]
}
```

**返回**:
```json
{
  "success": true,
  "data": {
    "url": "https://example.com",
    "count": 2
  }
}
```

**示例**:
```kotlin
browserSetCookies(
  cookies = listOf(
    "session_id=abc123; path=/",
    "user_id=456; path=/"
  )
)

// 指定 URL
browserSetCookies(
  url = "https://example.com",
  cookies = listOf("token=xyz789")
)
```

---

## 🎯 常见使用场景

### 场景 1: 登录流程

```kotlin
// 1. 导航到登录页
browserNavigate("https://example.com/login")

// 2. 等待页面加载
browserWait(selector = "#login-form")

// 3. 输入用户名
browserType("#username", "user@example.com")

// 4. 输入密码
browserType("#password", "secret123")

// 5. 点击登录按钮
browserClick("#login-button")

// 6. 等待跳转到首页
browserWait(url = "dashboard")

// 7. 验证登录成功
browserGetContent()  // 检查是否包含用户名
```

---

### 场景 2: 搜索操作

```kotlin
// 1. 导航到搜索引擎
browserNavigate("https://google.com")

// 2. 等待搜索框
browserWait(selector = "input[name='q']")

// 3. 输入搜索关键词并提交
browserType("input[name='q']", "OpenClaw", submit = true)

// 4. 等待搜索结果
browserWait(selector = "#search")

// 5. 获取搜索结果
browserGetContent()
```

---

### 场景 3: 表单填写

```kotlin
// 1. 导航到表单页面
browserNavigate("https://example.com/form")

// 2. 等待表单加载
browserWait(selector = "#registration-form")

// 3. 填写文本字段
browserType("#name", "John Doe")
browserType("#email", "john@example.com")

// 4. 选择下拉框
browserSelect("#country", values = listOf("US"))

// 5. 点击复选框
browserClick("#agree-terms")

// 6. 提交表单
browserClick("#submit-button")

// 7. 等待成功消息
browserWait(text = "Registration successful")
```

---

### 场景 4: 动态内容等待

```kotlin
// 1. 导航到页面
browserNavigate("https://example.com/products")

// 2. 等待加载文本消失
browserWait(textGone = "Loading...")

// 3. 等待产品列表出现
browserWait(selector = ".product-item")

// 4. 自定义等待条件：至少加载 10 个产品
browserWait(
  jsCondition = "document.querySelectorAll('.product-item').length >= 10"
)

// 5. 获取内容
browserGetContent()
```

---

### 场景 5: 截图和调试

```kotlin
// 1. 导航到页面
browserNavigate("https://example.com")

// 2. 等待加载完成
browserWait(jsCondition = "document.readyState === 'complete'")

// 3. 截取全页图片
browserScreenshot(fullPage = true)

// 4. 执行自定义 JavaScript 调试
browserExecute("console.log('Debug info:', document.title)")

// 5. 获取页面性能数据
browserExecute(
  "return JSON.stringify(performance.timing)"
)
```

---

### 场景 6: Cookie 管理

```kotlin
// 1. 获取当前 Cookies
val cookies = browserGetCookies()

// 2. 设置新的 Cookies
browserSetCookies(
  cookies = listOf(
    "session_id=new_session; path=/",
    "user_pref=dark_mode; path=/"
  )
)

// 3. 刷新页面验证
browserNavigate(currentUrl)
```

---

## 📊 与 OpenClaw Browser 对齐

| OpenClaw 操作 | BrowserForClaw 工具 | 状态 |
|--------------|---------------------|------|
| navigate | browser_navigate | ✅ 100% |
| { kind: "click" } | browser_click | ✅ 100% |
| { kind: "type" } | browser_type | ✅ 100% |
| { kind: "scrollIntoView" } | browser_scroll | ✅ 90% |
| snapshot | browser_get_content | ✅ 100% |
| { kind: "wait" } | browser_wait | ✅ 100% |
| { kind: "evaluate" } | browser_execute | ✅ 100% |
| { kind: "press" } | browser_press | ✅ 100% |
| { kind: "hover" } | browser_hover | ✅ 100% |
| { kind: "select" } | browser_select | ✅ 100% |
| screenshot | browser_screenshot | ✅ 100% |
| cookies | browser_get_cookies / browser_set_cookies | ✅ 100% |

**总体对齐度**: **95%+** ✅

**缺失功能**:
- drag (拖拽) - 使用频率低
- fill (批量填充) - 可用多次 type 代替
- resize (调整大小) - 移动端不适用
- close (关闭标签) - 后续版本

---

## 🔄 工具演进

### v0.3.0 (初始版本)
- 5 个核心工具
- 42% OpenClaw 对齐

### v0.4.0 (当前版本) ✅
- 13 个工具
- 95% OpenClaw 对齐
- 完全可用

### v0.5.0 (未来)
- 增加 drag, fill
- 多标签支持
- 100% OpenClaw 对齐

---

**BrowserForClaw v0.4.0 Tools Reference**

*Complete Browser Control for phoneforclaw* 🚀
