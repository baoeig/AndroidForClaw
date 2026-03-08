# BrowserForClaw Complete Guide

> One-stop guide from installation to integration

---

## 📖 Table of Contents

1. [Understanding BrowserForClaw](#understanding-browserforclaw)
2. [Quick Start](#quick-start)
3. [AI Agent Integration](#ai-agent-integration)
4. [HTTP Client Implementation](#http-client-implementation)

---

## Understanding BrowserForClaw

### 🧩 Two-Part System

BrowserForClaw consists of **two complementary parts**:

#### 1️⃣ Android App (Capability Provider)

**What it is**:
- An Android application running on your device
- Provides real browser (WebView) with automation controls
- Exposes HTTP API server on port 8765

**What it does**:
- Receives HTTP requests from external apps/agents
- Executes browser actions (navigate, click, type, etc.)
- Returns results in JSON format

**Location**: `android-project/` - Complete einkbro browser project

#### 2️⃣ SKILL.md File (Knowledge Provider)

**What it is**:
- A Markdown file with structured instructions
- Teaches AI agents **HOW** to use browser automation tools
- Follows AgentSkills.io format

**What it contains**:
- Descriptions of all 13 browser tools
- When and how to use each tool
- Common patterns and workflows
- Best practices and error handling
- Example code and troubleshooting

**Location**: `skill/SKILL.md`

### 🔗 How They Work Together

```
┌─────────────────────────────────────┐
│        AI Agent System              │
│                                     │
│  ┌──────────┐      ┌──────────┐   │
│  │ SKILL.md │─load→│Tool      │   │
│  │(Knowledge)│     │Registry  │   │
│  └──────────┘      └────┬─────┘   │
│                          │         │
│  User: "Search Google"   │         │
│      ↓                   │         │
│  AI decides:             │         │
│  1. browser_navigate ────┼─→ HTTP │
│  2. browser_type     ────┼─→ HTTP │
│  3. browser_press    ────┼─→ HTTP │
└──────────────────────────┼─────────┘
                           ↓ :8765
              ┌────────────────────┐
              │ BrowserForClaw App │
              │ (Android Device)   │
              │  - HTTP Server     │
              │  - 13 Tools        │
              │  - WebView         │
              └────────────────────┘
```

**Key Understanding**:
- Android App = Provides **CAPABILITY** (HTTP API)
- SKILL.md = Provides **KNOWLEDGE** (how to use)
- AI Agent = **CAPABILITY** + **KNOWLEDGE** = **INTELLIGENT AUTOMATION**

---

## Quick Start

### Prerequisites

- Android 7.0+ device or emulator (API 24+)
- ADB tools (for development)
- BrowserForClaw APK

### Step 1: Install the App

**Option A: Direct Download**
```bash
# Download APK from releases/
adb install BrowserForClaw-v0.5.1.apk

# Or manually install on device
```

**Option B: Build from Source**
```bash
cd android-project
./gradlew assembleRelease

# APK output: app/build/outputs/apk/release/
```

### Step 2: Start and Test

**Start the App**
```bash
# Launch the browser (HTTP server auto-starts on port 8765)
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity

# Setup port forwarding (for development)
adb forward tcp:8765 tcp:8765
```

**Health Check**
```bash
curl http://localhost:8765/health
# Output: {"status":"ok"}
```

**Test Navigation**
```bash
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_navigate","args":{"url":"https://example.com"}}'

# Output: {"success":true,"data":{"url":"https://example.com","title":"Example Domain"}}
```

### Step 3: Verify Tools

**Test JavaScript Execution**
```bash
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_execute","args":{"script":"return document.title"}}'

# Output: {"success":true,"data":{"result":"Example Domain"}}
```

**Test Content Extraction**
```bash
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_get_content","args":{"format":"text"}}'

# Output: {"success":true,"data":{"content":"..."}}
```

---

## AI Agent Integration

### Method 1: Using SKILL.md (Recommended)

**For AgentSkills.io Compatible Systems** (OpenClaw, phoneforclaw, etc.)

```bash
# 1. Copy SKILL.md to your agent's skills directory
cp skill/SKILL.md /path/to/your-agent/app/src/main/assets/skills/browser-automation/

# 2. Restart the agent - it will automatically:
#    - Load the skill file
#    - Learn about all 13 browser tools
#    - Understand when and how to use them
#    - Apply best practices

# 3. The agent can now use browser automation intelligently!
```

**How It Works**:
```
Agent loads SKILL.md → Learns:
  • 13 tools exist (navigate, click, type, etc.)
  • When to use each tool
  • Common patterns (Google search, form filling, etc.)
  • Error handling strategies

User asks: "Find weather in Tokyo"
  ↓
Agent automatically:
  1. Decides to use browser automation
  2. Plans the sequence:
     - Navigate to Google
     - Wait for search box
     - Type "Tokyo weather"
     - Press Enter
     - Extract results
  3. Executes HTTP calls to localhost:8765
  4. Returns formatted answer to user
```

### Method 2: Manual HTTP Client Implementation

**For Custom AI Agents or Direct Integration**

If your agent doesn't support AgentSkills.io format, you can implement a direct HTTP client. See the [HTTP Client Implementation](#http-client-implementation) section below.

---

## HTTP Client Implementation

### Full Kotlin Implementation

Here's a complete HTTP client with all 13 tool wrappers:

```kotlin
package com.example.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.Socket

/**
 * HTTP client for BrowserForClaw automation
 * Communicates with the Android app's HTTP API on port 8765
 */
class BrowserHttpClient(
    private val host: String = "localhost",
    private val port: Int = 8765
) {
    data class ToolResult(
        val success: Boolean,
        val data: Map<String, Any?>? = null,
        val error: String? = null
    )

    /**
     * Execute a browser tool via HTTP POST
     * @param tool Tool name (e.g., "browser_navigate")
     * @param args Tool arguments as Map
     * @return ToolResult with success status and data/error
     */
    suspend fun executeToolAsync(
        tool: String,
        args: Map<String, Any?>
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            // Build JSON request
            val requestJson = JSONObject().apply {
                put("tool", tool)
                put("args", JSONObject(args))
            }.toString()

            // Open socket connection
            val socket = Socket(host, port)
            socket.soTimeout = 30000 // 30 second timeout

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Send HTTP POST request
            writer.println("POST /api/browser/execute HTTP/1.1")
            writer.println("Host: $host:$port")
            writer.println("Content-Type: application/json")
            writer.println("Content-Length: ${requestJson.length}")
            writer.println()
            writer.print(requestJson)
            writer.flush()

            // Read response headers
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break // Empty line = end of headers
            }

            // Read response body
            val responseBody = StringBuilder()
            while (reader.readLine().also { line = it } != null) {
                responseBody.append(line)
            }

            socket.close()

            // Parse JSON response
            val responseJson = JSONObject(responseBody.toString())
            val success = responseJson.optBoolean("success", false)
            val data = responseJson.optJSONObject("data")?.let { json ->
                json.keys().asSequence().associateWith { key -> json.get(key) }
            }
            val error = responseJson.optString("error", null)

            ToolResult(success, data, error)

        } catch (e: Exception) {
            ToolResult(false, error = "Request failed: ${e.message}")
        }
    }
}

// ============================================================================
// Convenience Extension Functions - One for Each of the 13 Tools
// ============================================================================

/**
 * Navigate to a URL
 * @param url Target URL (e.g., "https://google.com")
 */
suspend fun BrowserHttpClient.navigate(url: String) =
    executeToolAsync("browser_navigate", mapOf("url" to url))

/**
 * Click an element
 * @param selector CSS selector (e.g., "#submit-button")
 */
suspend fun BrowserHttpClient.click(selector: String) =
    executeToolAsync("browser_click", mapOf("selector" to selector))

/**
 * Type text into an element
 * @param selector CSS selector for the input field
 * @param text Text to type
 */
suspend fun BrowserHttpClient.type(selector: String, text: String) =
    executeToolAsync("browser_type", mapOf(
        "selector" to selector,
        "text" to text
    ))

/**
 * Scroll the page
 * @param direction "up" or "down"
 * @param amount Number of pixels (optional)
 */
suspend fun BrowserHttpClient.scroll(direction: String, amount: Int? = null) =
    executeToolAsync("browser_scroll", buildMap {
        put("direction", direction)
        amount?.let { put("amount", it) }
    })

/**
 * Get page content
 * @param format "text", "html", or "markdown"
 */
suspend fun BrowserHttpClient.getContent(format: String = "text") =
    executeToolAsync("browser_get_content", mapOf("format" to format))

/**
 * Wait for a condition
 * @param selector CSS selector to wait for (optional)
 * @param timeMs Timeout in milliseconds (default: 5000)
 */
suspend fun BrowserHttpClient.wait(selector: String? = null, timeMs: Int = 5000) =
    executeToolAsync("browser_wait", buildMap {
        selector?.let { put("selector", it) }
        put("timeMs", timeMs)
    })

/**
 * Execute JavaScript
 * @param script JavaScript code to execute
 */
suspend fun BrowserHttpClient.execute(script: String) =
    executeToolAsync("browser_execute", mapOf("script" to script))

/**
 * Press a key
 * @param key Key name (e.g., "Enter", "Tab", "Backspace")
 */
suspend fun BrowserHttpClient.press(key: String) =
    executeToolAsync("browser_press", mapOf("key" to key))

/**
 * Hover over an element
 * @param selector CSS selector
 */
suspend fun BrowserHttpClient.hover(selector: String) =
    executeToolAsync("browser_hover", mapOf("selector" to selector))

/**
 * Select option from dropdown
 * @param selector CSS selector for the select element
 * @param value Value to select
 */
suspend fun BrowserHttpClient.select(selector: String, value: String) =
    executeToolAsync("browser_select", mapOf(
        "selector" to selector,
        "value" to value
    ))

/**
 * Take a screenshot
 * @return Base64-encoded PNG image
 */
suspend fun BrowserHttpClient.screenshot() =
    executeToolAsync("browser_screenshot", emptyMap())

/**
 * Get cookies
 * @param url URL to get cookies for (optional, defaults to current page)
 */
suspend fun BrowserHttpClient.getCookies(url: String? = null) =
    executeToolAsync("browser_get_cookies", buildMap {
        url?.let { put("url", it) }
    })

/**
 * Set cookies
 * @param cookies List of cookie objects with name, value, domain, path
 */
suspend fun BrowserHttpClient.setCookies(cookies: List<Map<String, String>>) =
    executeToolAsync("browser_set_cookies", mapOf("cookies" to cookies))
```

### Usage Examples

**Example 1: Google Search**
```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val browser = BrowserHttpClient()

    // Navigate to Google
    browser.navigate("https://google.com")

    // Wait for search box to load
    browser.wait(selector = "input[name='q']", timeMs = 5000)

    // Type search query
    browser.type("input[name='q']", "BrowserForClaw")

    // Submit search
    browser.press("Enter")

    // Wait for results
    browser.wait(timeMs = 3000)

    // Get page content
    val result = browser.getContent(format = "text")
    if (result.success) {
        println("Search results: ${result.data?.get("content")}")
    }
}
```

**Example 2: Form Filling**
```kotlin
suspend fun fillLoginForm(browser: BrowserHttpClient) {
    // Navigate to login page
    browser.navigate("https://example.com/login")
    browser.wait(selector = "#username", timeMs = 5000)

    // Fill username
    browser.type("#username", "myuser@example.com")

    // Fill password
    browser.type("#password", "mypassword")

    // Click login button
    browser.click("#login-button")

    // Wait for redirect
    browser.wait(timeMs = 3000)

    // Verify login success
    val content = browser.getContent()
    println("Login result: ${content.data?.get("content")}")
}
```

**Example 3: Web Scraping**
```kotlin
suspend fun scrapeProductInfo(browser: BrowserHttpClient, productUrl: String) {
    // Navigate to product page
    browser.navigate(productUrl)
    browser.wait(selector = ".product-title", timeMs = 5000)

    // Extract data using JavaScript
    val titleResult = browser.execute(
        "return document.querySelector('.product-title').textContent"
    )
    val priceResult = browser.execute(
        "return document.querySelector('.product-price').textContent"
    )

    println("Product: ${titleResult.data?.get("result")}")
    println("Price: ${priceResult.data?.get("result")}")

    // Take screenshot
    val screenshot = browser.screenshot()
    println("Screenshot captured: ${screenshot.success}")
}
```

### Integration with phoneforclaw

**Step 1: Add HTTP Client to phoneforclaw**

```kotlin
// In phoneforclaw project: app/src/main/java/com/claw/phone/browser/
// Copy the BrowserHttpClient.kt file

package com.claw.phone.browser

// ... (paste the full BrowserHttpClient implementation above)
```

**Step 2: Register Tools in phoneforclaw**

```kotlin
// In phoneforclaw's tool registry
class BrowserToolRegistry {
    private val httpClient = BrowserHttpClient()

    fun registerBrowserTools(toolManager: ToolManager) {
        // Register browser_navigate
        toolManager.register(
            name = "browser_navigate",
            description = "Navigate to a URL",
            parameters = listOf(
                Parameter("url", "string", "Target URL", required = true)
            ),
            execute = { args ->
                httpClient.navigate(args["url"] as String)
            }
        )

        // Register browser_click
        toolManager.register(
            name = "browser_click",
            description = "Click an element",
            parameters = listOf(
                Parameter("selector", "string", "CSS selector", required = true)
            ),
            execute = { args ->
                httpClient.click(args["selector"] as String)
            }
        )

        // Register remaining 11 tools similarly...
    }
}
```

**Step 3: Copy SKILL.md**

```bash
# Copy skill file to phoneforclaw
cp skill/SKILL.md /path/to/phoneforclaw/app/src/main/assets/skills/browser-automation/

# The AI agent will now:
# 1. Load the skill on startup
# 2. Learn about all 13 browser tools
# 3. Know when and how to use them
# 4. Apply best practices automatically
```

---

## Common Patterns

### Pattern 1: Navigate → Wait → Interact → Extract

```kotlin
suspend fun searchAndExtract(browser: BrowserHttpClient, query: String) {
    // 1. Navigate
    browser.navigate("https://google.com")

    // 2. Wait for element
    browser.wait(selector = "input[name='q']", timeMs = 5000)

    // 3. Interact
    browser.type("input[name='q']", query)
    browser.press("Enter")
    browser.wait(timeMs = 3000)

    // 4. Extract
    val content = browser.getContent(format = "text")
    return content.data?.get("content")
}
```

### Pattern 2: Scroll to Load More Content

```kotlin
suspend fun loadAllItems(browser: BrowserHttpClient) {
    browser.navigate("https://example.com/infinite-scroll")
    browser.wait(timeMs = 2000)

    repeat(5) {
        browser.scroll("down", amount = 1000)
        browser.wait(timeMs = 1000) // Wait for new items to load
    }

    val allContent = browser.getContent()
    // Process content...
}
```

### Pattern 3: Cookie-Based Authentication

```kotlin
suspend fun loginWithCookies(browser: BrowserHttpClient) {
    // Set authentication cookies
    browser.setCookies(listOf(
        mapOf(
            "name" to "session_token",
            "value" to "abc123",
            "domain" to ".example.com",
            "path" to "/"
        )
    ))

    // Navigate to protected page
    browser.navigate("https://example.com/dashboard")

    // Verify cookies are set
    val cookies = browser.getCookies()
    println("Cookies: ${cookies.data}")
}
```

---

## Troubleshooting

### Connection Refused

**Problem**: `Connection refused when connecting to localhost:8765`

**Solutions**:
1. Verify the app is running: `adb shell dumpsys window | grep mCurrentFocus`
2. Check port forwarding: `adb forward tcp:8765 tcp:8765`
3. Restart the app: `adb shell am force-stop info.plateaukao.einkbro && adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity`

### Element Not Found

**Problem**: `browser_click` or `browser_type` fails with "element not found"

**Solutions**:
1. Use `browser_wait` before interacting: `browser.wait(selector = "#my-element", timeMs = 5000)`
2. Verify the selector is correct: Use browser DevTools to test selectors
3. Wait for page load: Add a delay after navigation

### JavaScript Execution Fails

**Problem**: `browser_execute` returns an error

**Solutions**:
1. Check JavaScript syntax: Test in browser console first
2. Use `return` statement: `browser.execute("return document.title")`
3. Handle async JavaScript: Use callbacks or Promises

### Timeout Errors

**Problem**: Operations timeout after 30 seconds

**Solutions**:
1. Increase socket timeout in `BrowserHttpClient`: `socket.soTimeout = 60000`
2. Break long operations into smaller steps
3. Use `browser_wait` with appropriate timeouts

---

## Reference: All 13 Tools

| Tool | Description | Required Args | Optional Args |
|------|-------------|---------------|---------------|
| `browser_navigate` | Navigate to URL | `url` | - |
| `browser_click` | Click element | `selector` | - |
| `browser_type` | Type text | `selector`, `text` | - |
| `browser_scroll` | Scroll page | `direction` | `amount` |
| `browser_get_content` | Get page content | - | `format` |
| `browser_wait` | Wait for condition | - | `selector`, `timeMs` |
| `browser_execute` | Execute JavaScript | `script` | - |
| `browser_press` | Press key | `key` | - |
| `browser_hover` | Hover element | `selector` | - |
| `browser_select` | Select dropdown | `selector`, `value` | - |
| `browser_screenshot` | Take screenshot | - | - |
| `browser_get_cookies` | Get cookies | - | `url` |
| `browser_set_cookies` | Set cookies | `cookies` | - |

---

## Next Steps

1. **Test the API**: Follow [Quick Start](#quick-start) to verify everything works
2. **Integrate with your agent**: Use [Method 1](#method-1-using-skillmd-recommended) or [Method 2](#method-2-manual-http-client-implementation)
3. **Read API Reference**: See [API.md](API.md) for detailed documentation
4. **Check FAQ**: See [FAQ.md](FAQ.md) for common questions

---

**BrowserForClaw** - Browser Automation for AI Agents 🌐🤖
