---
name: browser-automation
description: Web browser automation and control via HTTP API
metadata: { "openclaw": { "always": false, "emoji": "🌐" } }
---

# Browser Automation Skill

Control web browsers to automate web interactions, scraping, testing, and research tasks.

## 🎯 Core Pattern

All browser operations follow this pattern:

```
Navigate → Wait → Interact → Extract
```

1. **Navigate**: Open target URL
2. **Wait**: Ensure page/elements loaded
3. **Interact**: Click, type, scroll, execute JS
4. **Extract**: Get content, screenshot, or cookies

## 🛠️ Available Tools

### Navigation

**browser_navigate(url)**
- Navigate to a URL
- Automatically adds https:// if missing
- Parameters:
  - `url` (string, required): Target URL
  - `waitMs` (number, optional): Wait time after navigation (default: 500ms)
- Returns: `{url, status: "navigating"}`
- Example: `browser_navigate("https://google.com")`

### Content Extraction

**browser_get_content(format)**
- Get page content in different formats
- Parameters:
  - `format` (string): "text", "html", or "markdown"
- Returns: `{content, format}`
- Example: `browser_get_content("text")` → Extract visible text

**browser_screenshot(fullPage)**
- Capture page screenshot
- Parameters:
  - `fullPage` (boolean, optional): Capture entire page vs viewport (default: false)
- Returns: `{screenshot: "<base64>"}`
- Example: `browser_screenshot(false)` → Capture viewport

### Element Interaction

**browser_click(selector)**
- Click an element via CSS selector
- Parameters:
  - `selector` (string, required): CSS selector
- Returns: `{selector, clicked: true}`
- Examples:
  - `browser_click("#submit-button")`
  - `browser_click("button.primary")`
  - `browser_click("[data-testid='login']")`

**browser_type(selector, text, submit)**
- Type text into input field
- Parameters:
  - `selector` (string, required): CSS selector
  - `text` (string, required): Text to type
  - `submit` (boolean, optional): Press Enter after typing (default: false)
- Returns: `{selector, text, submitted}`
- Example: `browser_type("input[name='q']", "search term", true)`

**browser_hover(selector)**
- Hover over element (triggers hover states/tooltips)
- Parameters:
  - `selector` (string, required): CSS selector
- Returns: `{selector, hovered: true}`
- Example: `browser_hover(".dropdown-trigger")`

**browser_select(selector, values)**
- Select dropdown options
- Parameters:
  - `selector` (string, required): Select element CSS selector
  - `values` (array, required): List of values to select
- Returns: `{selector, values, count}`
- Example: `browser_select("#country", ["US"])`

### Keyboard & Scrolling

**browser_press(key)**
- Press keyboard key
- Parameters:
  - `key` (string, required): Key name - "Enter", "Escape", "Tab", "Backspace", "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"
- Returns: `{key, pressed: true}`
- Example: `browser_press("Enter")` → Submit form

**browser_scroll(direction)**
- Scroll page in direction
- Parameters:
  - `direction` (string, required): "up", "down", "top", or "bottom"
- Returns: `{direction, scrolled: true}`
- Examples:
  - `browser_scroll("down")` → Scroll down one viewport
  - `browser_scroll("bottom")` → Scroll to page bottom

### Waiting & Synchronization

**browser_wait(selector, text, url, timeMs)**
- Wait for conditions (all parameters optional, at least one required)
- Parameters:
  - `selector` (string): Wait for element to appear
  - `text` (string): Wait for text to appear on page
  - `url` (string): Wait for URL to contain string
  - `timeMs` (number): Simple delay in milliseconds
- Returns: `{waitType, timeMs}`
- Examples:
  - `browser_wait({"timeMs": 2000})` → Wait 2 seconds
  - `browser_wait({"selector": "#result"})` → Wait for element
  - `browser_wait({"text": "Success"})` → Wait for text
  - `browser_wait({"url": "checkout"})` → Wait for URL change

### JavaScript Execution

**browser_execute(script, selector)**
- Execute custom JavaScript in browser context
- Parameters:
  - `script` (string, required): JavaScript code (must return value)
  - `selector` (string, optional): Target specific element
- Returns: `{result, script}`
- Examples:
  - `browser_execute("return document.title")` → Get page title
  - `browser_execute("return window.location.href")` → Get current URL
  - `browser_execute("return document.querySelectorAll('a').length")` → Count links
  - `browser_execute("return document.querySelector('.price').innerText", ".price")` → Extract price

### Cookie Management

**browser_get_cookies()**
- Get all cookies for current domain
- No parameters
- Returns: `{cookies: "name1=value1; name2=value2"}`
- Example: `browser_get_cookies()` → Get session cookies

**browser_set_cookies(cookies, url)**
- Set cookies for domain
- Parameters:
  - `cookies` (array, required): List of cookie strings
  - `url` (string, optional): Target URL (default: current page)
- Cookie format: `"name=value; path=/; domain=.example.com"`
- Returns: `{url, count}`
- Example: `browser_set_cookies(["session=abc123; path=/", "user=john; path=/"])`

## 📋 Common Patterns

### Pattern 1: Google Search

```
1. browser_navigate("https://google.com")
2. browser_wait({"selector": "input[name='q']"})
3. browser_type("input[name='q']", "search query")
4. browser_press("Enter")
5. browser_wait({"url": "search"})
6. browser_get_content("text")
```

### Pattern 2: Form Filling

```
1. browser_navigate("https://example.com/form")
2. browser_type("#email", "user@example.com")
3. browser_type("#password", "password123")
4. browser_click("button[type='submit']")
5. browser_wait({"text": "Success"})
```

### Pattern 3: Data Extraction

```
1. browser_navigate("https://news.site.com")
2. browser_wait({"selector": ".article-title"})
3. browser_execute("return Array.from(document.querySelectorAll('.article-title')).map(el => el.innerText)")
4. browser_screenshot(false) → Capture for verification
```

### Pattern 4: Authentication with Cookies

```
1. browser_navigate("https://example.com/login")
2. browser_type("#username", "user")
3. browser_type("#password", "pass")
4. browser_click("#login-button")
5. browser_wait({"url": "dashboard"})
6. browser_get_cookies() → Save session
```

### Pattern 5: Pagination Scraping

```
Loop:
  1. browser_get_content("text") → Extract current page
  2. browser_scroll("bottom") → Load lazy content
  3. browser_wait({"timeMs": 1000})
  4. browser_click(".next-page")
  5. browser_wait({"selector": ".content-loaded"})
```

## ⚠️ Important Rules

### 1. Always Wait After Navigate
```javascript
// ❌ BAD
browser_navigate("https://example.com")
browser_click("#button")  // Element not loaded yet!

// ✅ GOOD
browser_navigate("https://example.com")
browser_wait({"selector": "#button"})  // Wait for element
browser_click("#button")
```

### 2. Use Correct Selectors
- **ID**: `#submit-button`
- **Class**: `.btn-primary`
- **Attribute**: `[data-testid='login']`
- **Name**: `input[name='email']`
- **Type**: `button[type='submit']`
- **Nested**: `form .submit-button`

### 3. Handle Dynamic Content
```javascript
// For single-page apps (SPA)
browser_wait({"selector": ".content"})  // Wait for AJAX load
browser_wait({"timeMs": 1000})          // Additional buffer

// For infinite scroll
browser_scroll("down")
browser_wait({"timeMs": 2000})  // Wait for new content
```

### 4. JavaScript Must Return Values
```javascript
// ❌ BAD
browser_execute("console.log(document.title)")  // No return!

// ✅ GOOD
browser_execute("return document.title")
```

### 5. Verify After Actions
```javascript
// Always check success
browser_click("#submit")
browser_wait({"text": "Success"})  // Verify action worked
```

## 🚀 Setup Requirements

### Prerequisites

1. **EinkBro APK installed** (includes BrowserForClaw HTTP server)
2. **Port forwarding configured** (if using adb):
   ```bash
   adb forward tcp:8765 tcp:8765
   ```
3. **Browser running** with HTTP server on port 8765

### Starting the Browser

If browser is not running, the host app (e.g., phoneforclaw) should launch it using this Intent:

```kotlin
// Launch BrowserForClaw app
val intent = Intent().apply {
    component = ComponentName(
        "info.plateaukao.einkbro",
        "info.plateaukao.einkbro.activity.BrowserActivity"
    )
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
context.startActivity(intent)

// Wait for HTTP server to start (1-2 seconds)
delay(2000)
```

Or via ADB:
```bash
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity
```

### HTTP Client Setup (phoneforclaw)

The BrowserHttpClient must be configured in phoneforclaw:

```kotlin
package com.example.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
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

    suspend fun executeToolAsync(
        tool: String,
        args: Map<String, Any?>
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            val requestJson = JSONObject().apply {
                put("tool", tool)
                put("args", JSONObject(args))
            }.toString()

            val socket = Socket(host, port)
            socket.soTimeout = 30000

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Send HTTP POST
            writer.println("POST /api/browser/execute HTTP/1.1")
            writer.println("Host: $host:$port")
            writer.println("Content-Type: application/json")
            writer.println("Content-Length: ${requestJson.length}")
            writer.println()
            writer.print(requestJson)
            writer.flush()

            // Read response headers
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read response body
            val responseBody = if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                reader.read(buffer, 0, contentLength)
                String(buffer)
            } else {
                StringBuilder().apply {
                    while (reader.readLine().also { line = it } != null) {
                        append(line)
                    }
                }.toString()
            }

            socket.close()

            // Parse JSON response
            val responseJson = JSONObject(responseBody)
            val success = responseJson.optBoolean("success", false)
            val dataJson = responseJson.optJSONObject("data")
            val error = responseJson.optString("error", null)

            val data = dataJson?.let { json ->
                mutableMapOf<String, Any?>().apply {
                    json.keys().forEach { key ->
                        put(key, json.opt(key))
                    }
                }
            }

            ToolResult(success, data, error)

        } catch (e: Exception) {
            ToolResult(false, error = "Request failed: ${e.message}")
        }
    }
}
```

Then register browser tools in ToolRegistry:

```kotlin
// In ToolRegistry.registerDefaultTools()
val browserClient = BrowserHttpClient()

register(BrowserNavigateTool(browserClient))
register(BrowserClickTool(browserClient))
register(BrowserTypeTool(browserClient))
register(BrowserScrollTool(browserClient))
register(BrowserGetContentTool(browserClient))
register(BrowserWaitTool(browserClient))
register(BrowserExecuteTool(browserClient))
register(BrowserPressTool(browserClient))
register(BrowserHoverTool(browserClient))
register(BrowserSelectTool(browserClient))
register(BrowserScreenshotTool(browserClient))
register(BrowserGetCookiesTool(browserClient))
register(BrowserSetCookiesTool(browserClient))
```

## 🎓 Best Practices

### 1. Progressive Enhancement
Start simple, add complexity only when needed:
```
1. Navigate → 2. Wait → 3. Extract
(Add interaction only if simple extraction fails)
```

### 2. Robust Selectors
Prefer stable selectors:
- ✅ `[data-testid='submit']` (most stable)
- ✅ `#unique-id` (stable if ID doesn't change)
- ⚠️  `.css-class` (may change with styling)
- ❌ `div > div > span:nth-child(3)` (fragile)

### 3. Error Recovery
```javascript
// Try multiple selectors
browser_wait({"selector": "#submit"})  // Try ID
// If fails:
browser_wait({"selector": "button[type='submit']"})  // Try type
// If fails:
browser_wait({"text": "Submit"})  // Try text
```

### 4. Performance
- Use `browser_wait({"timeMs": X})` instead of multiple short waits
- Use `browser_get_content("text")` for data extraction (faster than screenshot)
- Only use `browser_screenshot` when visual verification needed

### 5. Security
- Never hardcode credentials
- Clear cookies after sensitive operations: `browser_set_cookies([])`
- Use incognito mode for testing (manual setup)

## 📚 Additional Resources

- Full API Reference: [TOOLS_REFERENCE.md](./TOOLS_REFERENCE.md)
- HTTP Client Implementation: [PHONEFORCLAW_HTTP_CLIENT.md](./PHONEFORCLAW_HTTP_CLIENT.md)
- Quick Start Guide: [QUICK_START.md](./QUICK_START.md)
- Test Results: [LIVE_TEST_RESULTS.md](./LIVE_TEST_RESULTS.md)

## 🔧 Troubleshooting

**Connection refused**:
- Ensure EinkBro is running (launch via Intent if needed)
- Check port forwarding: `adb forward tcp:8765 tcp:8765`
- Verify HTTP server started: `adb logcat | grep BrowserHttpServer`

**Element not found**:
- Use `browser_execute("return document.body.innerHTML")` to inspect page
- Try alternative selectors
- Add wait before interaction

**Timeout errors**:
- Increase wait time: `browser_wait({"timeMs": 5000})`
- Check network connectivity
- Verify page actually loads in manual test

**JavaScript errors**:
- Ensure script returns a value
- Check for typos in JS syntax
- Test script in browser console first

---

**BrowserForClaw v0.5.0** - Browser Automation for AI Agents 🌐
