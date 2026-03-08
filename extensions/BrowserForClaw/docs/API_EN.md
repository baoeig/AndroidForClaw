# BrowserForClaw Tools Reference Manual

**Version**: v0.4.0
**Date**: 2026-03-06
**Total Tools**: 13

---

## 📋 Tool List

| # | Tool Name | Function | Priority |
|---|---------|------|--------|
| 1 | browser_navigate | Navigate to URL | ⭐⭐⭐⭐⭐ |
| 2 | browser_click | Click element | ⭐⭐⭐⭐⭐ |
| 3 | browser_type | Type text | ⭐⭐⭐⭐⭐ |
| 4 | browser_scroll | Scroll page | ⭐⭐⭐⭐ |
| 5 | browser_get_content | Get page content | ⭐⭐⭐⭐⭐ |
| 6 | browser_wait | Wait for condition | ⭐⭐⭐⭐⭐ |
| 7 | browser_execute | Execute JavaScript | ⭐⭐⭐⭐⭐ |
| 8 | browser_press | Press key | ⭐⭐⭐⭐ |
| 9 | browser_hover | Hover element | ⭐⭐⭐ |
| 10 | browser_select | Select dropdown | ⭐⭐⭐ |
| 11 | browser_screenshot | Take screenshot | ⭐⭐⭐⭐ |
| 12 | browser_get_cookies | Get Cookies | ⭐⭐⭐ |
| 13 | browser_set_cookies | Set Cookies | ⭐⭐⭐ |

---

## 🔧 Tool Details

### 1. browser_navigate

**Function**: Navigate to specified URL

**Parameters**:
```json
{
  "url": "https://google.com",  // Required: Target URL
  "waitMs": 500                 // Optional: Milliseconds to wait for loading, default 500
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "url": "https://google.com",
    "currentUrl": "https://google.com"
  }
}
```

**Examples**:
```kotlin
browserNavigate("https://google.com")
browserNavigate("example.com", waitMs = 1000)  // Automatically adds https://
```

---

### 2. browser_click

**Function**: Click page element

**Parameters**:
```json
{
  "selector": "#search-button"  // Required: CSS selector
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "selector": "#search-button",
    "clicked": true
  }
}
```

**Examples**:
```kotlin
browserClick("#login-button")
browserClick("button.primary")
browserClick("[data-testid='submit']")
```

---

### 3. browser_type

**Function**: Type text in input field

**Parameters**:
```json
{
  "selector": "input[name='q']",  // Required: CSS selector
  "text": "Hello World",          // Required: Text to type
  "submit": false                 // Optional: Whether to submit form, default false
}
```

**Returns**:
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

**Examples**:
```kotlin
browserType("#email", "user@example.com")
browserType("input[name='password']", "secret123", submit = true)
```

---

### 4. browser_scroll

**Function**: Scroll page

**Parameters**:
```json
{
  "direction": "down",  // Optional: "down", "up", "top", "bottom", default "down"
  "amount": 500         // Optional: Pixels to scroll (only effective for down/up)
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "direction": "down",
    "scrolled": true
  }
}
```

**Examples**:
```kotlin
browserScroll("down")              // Scroll down one screen
browserScroll("down", amount = 500) // Scroll down 500px
browserScroll("top")               // Scroll to top
browserScroll("bottom")            // Scroll to bottom
```

---

### 5. browser_get_content

**Function**: Get page content

**Parameters**:
```json
{
  "format": "text"  // Optional: "text" or "html", default "text"
}
```

**Returns**:
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

**Examples**:
```kotlin
browserGetContent()                  // Get plain text
browserGetContent(format = "html")   // Get HTML
```

---

### 6. browser_wait ⭐

**Function**: Wait for page to meet specific condition

**Parameters**:
```json
{
  // Choose one wait method:
  "timeMs": 1000,                     // 1. Simple wait time
  "selector": "#content",             // 2. Wait for element to appear
  "text": "Loading complete",         // 3. Wait for text to appear
  "textGone": "Loading...",           // 4. Wait for text to disappear
  "url": "https://example.com/done",  // 5. Wait for URL to contain string
  "jsCondition": "document.readyState === 'complete'",  // 6. Custom JS condition

  "timeout": 30000  // Optional: Timeout in milliseconds, default 30000
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "waitType": "selector",
    "selector": "#content"
  }
}
```

**Examples**:
```kotlin
// Wait for fixed time
browserWait(timeMs = 2000)

// Wait for element to appear
browserWait(selector = "#login-form")

// Wait for text to appear
browserWait(text = "Welcome back")

// Wait for text to disappear
browserWait(textGone = "Loading...")

// Wait for URL change
browserWait(url = "dashboard")

// Custom condition
browserWait(jsCondition = "document.querySelectorAll('.item').length > 5")
```

---

### 7. browser_execute ⭐

**Function**: Execute custom JavaScript code

**Parameters**:
```json
{
  "script": "return document.title",  // Required: JavaScript code
  "selector": "#element"              // Optional: Execute on specific element
}
```

**Returns**:
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

**Examples**:
```kotlin
// Page-level execution
browserExecute("return document.title")
browserExecute("return document.querySelectorAll('a').length")

// Element-level execution
browserExecute(
  script = "return element.textContent",
  selector = "#content"
)

// Modify page
browserExecute("document.body.style.backgroundColor = 'yellow'")
```

---

### 8. browser_press

**Function**: Simulate keyboard press

**Parameters**:
```json
{
  "key": "Enter",   // Required: Key name
  "delayMs": 100    // Optional: Delay after press, default 100
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "key": "Enter",
    "pressed": true
  }
}
```

**Supported Keys**:
- `Enter`, `Tab`, `Escape`
- `ArrowDown`, `ArrowUp`, `ArrowLeft`, `ArrowRight`
- `Backspace`, `Delete`
- `Home`, `End`, `PageUp`, `PageDown`
- Any letter/number keys

**Examples**:
```kotlin
browserPress("Enter")
browserPress("Tab")
browserPress("ArrowDown", delayMs = 200)
```

---

### 9. browser_hover

**Function**: Trigger mouse hover event

**Parameters**:
```json
{
  "selector": "#menu-item"  // Required: CSS selector
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "selector": "#menu-item",
    "hovered": true
  }
}
```

**Examples**:
```kotlin
browserHover("#dropdown-menu")
browserHover(".tooltip-trigger")
```

---

### 10. browser_select

**Function**: Select dropdown option

**Parameters**:
```json
{
  "selector": "select[name='country']",  // Required: CSS selector (pointing to <select>)
  "values": ["US", "CN"]                 // Required: List of values to select
}
```

**Returns**:
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

**Examples**:
```kotlin
// Single selection
browserSelect("#country", values = listOf("US"))

// Multiple selection (if select supports multiple)
browserSelect("#languages", values = listOf("en", "zh", "ja"))
```

---

### 11. browser_screenshot

**Function**: Capture page screenshot

**Parameters**:
```json
{
  "fullPage": false,  // Optional: Whether to capture full page, default false
  "format": "png",    // Optional: "png" or "jpeg", default "png"
  "quality": 80       // Optional: JPEG quality 0-100, default 80
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "screenshot": "iVBORw0KGgoAAAANS...",  // Base64 encoded image
    "width": 1080,
    "height": 1920,
    "format": "png",
    "size": 123456
  }
}
```

**Examples**:
```kotlin
// Current visible area
browserScreenshot()

// Full page screenshot
browserScreenshot(fullPage = true)

// JPEG format
browserScreenshot(format = "jpeg", quality = 90)
```

---

### 12. browser_get_cookies

**Function**: Get cookies from current page

**Parameters**: None

**Returns**:
```json
{
  "success": true,
  "data": {
    "cookies": "session_id=abc123; user_id=456",
    "url": "https://example.com"
  }
}
```

**Examples**:
```kotlin
browserGetCookies()
```

---

### 13. browser_set_cookies

**Function**: Set cookies

**Parameters**:
```json
{
  "url": "https://example.com",  // Optional: Target URL, default current URL
  "cookies": [                   // Required: Cookie list
    "session_id=abc123; path=/; domain=.example.com",
    "user_id=456; path=/"
  ]
}
```

**Returns**:
```json
{
  "success": true,
  "data": {
    "url": "https://example.com",
    "count": 2
  }
}
```

**Examples**:
```kotlin
browserSetCookies(
  cookies = listOf(
    "session_id=abc123; path=/",
    "user_id=456; path=/"
  )
)

// Specify URL
browserSetCookies(
  url = "https://example.com",
  cookies = listOf("token=xyz789")
)
```

---

## 🎯 Common Usage Scenarios

### Scenario 1: Login Flow

```kotlin
// 1. Navigate to login page
browserNavigate("https://example.com/login")

// 2. Wait for page to load
browserWait(selector = "#login-form")

// 3. Enter username
browserType("#username", "user@example.com")

// 4. Enter password
browserType("#password", "secret123")

// 5. Click login button
browserClick("#login-button")

// 6. Wait for redirect to homepage
browserWait(url = "dashboard")

// 7. Verify login success
browserGetContent()  // Check if contains username
```

---

### Scenario 2: Search Operation

```kotlin
// 1. Navigate to search engine
browserNavigate("https://google.com")

// 2. Wait for search box
browserWait(selector = "input[name='q']")

// 3. Enter search keyword and submit
browserType("input[name='q']", "OpenClaw", submit = true)

// 4. Wait for search results
browserWait(selector = "#search")

// 5. Get search results
browserGetContent()
```

---

### Scenario 3: Form Filling

```kotlin
// 1. Navigate to form page
browserNavigate("https://example.com/form")

// 2. Wait for form to load
browserWait(selector = "#registration-form")

// 3. Fill text fields
browserType("#name", "John Doe")
browserType("#email", "john@example.com")

// 4. Select dropdown
browserSelect("#country", values = listOf("US"))

// 5. Click checkbox
browserClick("#agree-terms")

// 6. Submit form
browserClick("#submit-button")

// 7. Wait for success message
browserWait(text = "Registration successful")
```

---

### Scenario 4: Dynamic Content Waiting

```kotlin
// 1. Navigate to page
browserNavigate("https://example.com/products")

// 2. Wait for loading text to disappear
browserWait(textGone = "Loading...")

// 3. Wait for product list to appear
browserWait(selector = ".product-item")

// 4. Custom wait condition: at least 10 products loaded
browserWait(
  jsCondition = "document.querySelectorAll('.product-item').length >= 10"
)

// 5. Get content
browserGetContent()
```

---

### Scenario 5: Screenshot and Debugging

```kotlin
// 1. Navigate to page
browserNavigate("https://example.com")

// 2. Wait for loading to complete
browserWait(jsCondition = "document.readyState === 'complete'")

// 3. Capture full page screenshot
browserScreenshot(fullPage = true)

// 4. Execute custom JavaScript for debugging
browserExecute("console.log('Debug info:', document.title)")

// 5. Get page performance data
browserExecute(
  "return JSON.stringify(performance.timing)"
)
```

---

### Scenario 6: Cookie Management

```kotlin
// 1. Get current cookies
val cookies = browserGetCookies()

// 2. Set new cookies
browserSetCookies(
  cookies = listOf(
    "session_id=new_session; path=/",
    "user_pref=dark_mode; path=/"
  )
)

// 3. Refresh page to verify
browserNavigate(currentUrl)
```

---

## 📊 Alignment with OpenClaw Browser

| OpenClaw Operation | BrowserForClaw Tool | Status |
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

**Overall Alignment**: **95%+** ✅

**Missing Features**:
- drag (dragging) - Low usage frequency
- fill (batch filling) - Can be replaced with multiple type calls
- resize (resize) - Not applicable for mobile
- close (close tab) - Future version

---

## 🔄 Tool Evolution

### v0.3.0 (Initial Version)
- 5 core tools
- 42% OpenClaw alignment

### v0.4.0 (Current Version) ✅
- 13 tools
- 95% OpenClaw alignment
- Fully functional

### v0.5.0 (Future)
- Add drag, fill
- Multi-tab support
- 100% OpenClaw alignment

---

**BrowserForClaw v0.4.0 Tools Reference**

*Complete Browser Control for phoneforclaw* 🚀
