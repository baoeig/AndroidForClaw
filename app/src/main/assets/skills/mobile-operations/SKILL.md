---
name: mobile-operations
description: Core mobile device operation skills for Android interaction and control
metadata: { "openclaw": { "always": true, "emoji": "📱" } }
---

# Mobile Operations Skill

Core skills for AI to observe and control Android devices through various tools and capabilities.

## 🎯 Core Loop

All mobile operations follow this pattern:

```
Observe → Think → Act → Verify
```

1. **Observe**: Use `screenshot` or `get_view_tree` to see current state
2. **Think**: Analyze what you see and plan next action
3. **Act**: Execute operation (tap, swipe, type, etc.)
4. **Verify**: Screenshot again to confirm action succeeded

## 🛠️ Available Tools

### Observation Tools

**screenshot()**
- Captures current screen + UI tree
- **Important**: Use BEFORE and AFTER every operation
- Returns: Image path, screen dimensions, UI elements with coordinates
- Example: `screenshot()` → See what's on screen

**get_view_tree()**
- Lightweight alternative to screenshot
- Gets UI hierarchy without image capture
- Faster, use when visual info not needed
- Returns: List of interactive elements with coordinates

### Action Tools

**tap(x, y)**
- Taps screen at coordinates
- Get coordinates from screenshot or get_view_tree
- Example: `tap(540, 1200)` → Tap button at center-bottom

**swipe(startX, startY, endX, endY, duration)**
- Swipes from start to end point
- Duration in milliseconds (default: 300ms)
- Example: `swipe(540, 1500, 540, 500, 300)` → Swipe up

**type(text)**
- Types text into focused input field
- Must tap input field first
- Example: `tap(540, 800)` then `type("Hello World")`

**long_press(x, y, duration)**
- Long press at coordinates
- Duration in milliseconds (default: 1000ms)
- Example: `long_press(540, 800, 1500)` → Long press 1.5s

### Navigation Tools

**home()**
- Presses Android home button
- Returns to launcher
- Example: `home()` → Go to home screen

**back()**
- Presses Android back button
- Goes back one screen
- Example: `back()` → Go back

**open_app(package_name)**
- Opens app by package name
- Waits for app to launch (1s default)
- Example: `open_app("com.android.settings")` → Open Settings

**list_installed_apps(include_system, filter)**
- Lists all installed applications
- `include_system`: Include system apps (default: false)
- `filter`: Filter by name or package (optional)
- Returns: List of apps with package names and labels
- Example: `list_installed_apps(false, "browser")` → Find browser apps

**start_activity(component, package, activity)**
- Starts specific Activity by component name
- Use `component` for full name: "package/.ActivityName"
- Or use `package` + `activity` separately
- Example: `start_activity(component="info.plateaukao.einkbro/.activity.BrowserActivity")`

### System Tools

**wait(seconds)**
- Waits for specified duration
- Use when UI is loading
- Example: `wait(2)` → Wait 2 seconds

**stop(reason)**
- Stops task execution
- Include reason/summary
- Example: `stop("Task completed successfully")`

**log(message, level)**
- Logs message for debugging
- Level: info, warn, error
- Example: `log("Starting test phase 2", "info")`

## 🎓 Key Principles

### 1. Never Assume - Always Observe

❌ **Wrong**:
```
tap(540, 800)  # Assuming button is there
type("username")
```

✅ **Correct**:
```
screenshot()  # See what's on screen first
# After seeing login button at (540, 800)
tap(540, 800)
wait(1)
screenshot()  # Verify button was tapped
type("username")
```

### 2. Verify Every Step

After each action, screenshot to confirm it worked:

```
# 1. See initial state
screenshot()

# 2. Take action
tap(540, 1200)

# 3. Verify result
wait(1)  # Give UI time to update
screenshot()  # Confirm action succeeded
```

### 3. Handle Timeouts

When loading, use wait() before checking:

```
tap(540, 800)  # Open something
wait(2)  # Wait for loading
screenshot()  # Now check result
```

### 4. Be Flexible

If an action fails, try alternatives:

```
# Try primary method
tap(540, 800)
wait(1)
screenshot()

# If failed, try alternative approach
# Maybe UI layout changed, look for element by text
get_view_tree()  # Find element by content
tap(new_x, new_y)
```

### 5. Coordinate Extraction

When you receive UI tree or screenshot result:

```
【屏幕 UI 元素】（共 25 个）

[0] "Settings" (120, 200) [可点击]
[1] "Search" (960, 200) [可点击]
[2] "Network & internet" (540, 400) [可点击]
...
```

Use these coordinates directly:
```
tap(540, 400)  # Tap "Network & internet"
```

## 📱 Common Workflows

### Opening and Testing an App

```
# 1. Go to home
home()
wait(1)

# 2. Open app
open_app("com.example.app")
wait(2)

# 3. Observe initial state
screenshot()

# 4. Interact based on what you see
tap(x, y)  # Based on screenshot
wait(1)
screenshot()  # Verify

# 5. Continue testing...
```

### Filling a Form

```
# 1. See the form
screenshot()

# 2. Fill first field
tap(540, 600)  # Username field
wait(0.5)
type("testuser")

# 3. Next field
tap(540, 800)  # Password field
wait(0.5)
type("password123")

# 4. Submit
screenshot()  # Locate submit button
tap(540, 1200)  # Submit button
wait(2)
screenshot()  # Verify submitted
```

## ⚠️ Important Notes

### Accessibility Service Required

Most operations require Android Accessibility Service:
- `tap`, `swipe`, `type`, `long_press`
- `home`, `back`
- `get_view_tree`, `screenshot` (UI tree part)

If service not enabled, these will fail with permission error.

### Coordinates are Absolute

All coordinates are screen pixels (absolute):
- Example: 1080x2400 screen
- X range: 0-1080
- Y range: 0-2400

### Screenshot vs get_view_tree

- Use `screenshot` when you need visual info (colors, images, OCR)
- Use `get_view_tree` for quick element lookup (faster)
- Both return UI element coordinates

## 🐛 Troubleshooting

### Action Not Working?

1. **Check coordinates**: `screenshot()` to see current state
2. **Check timing**: Add `wait()` before checking result
3. **Check accessibility**: Ensure service is enabled
4. **Try alternative**: Use different approach (swipe vs tap, etc.)

### Element Not Found?

1. **Scroll**: Element might be off-screen, try `swipe()` to scroll
2. **Wait**: UI might still be loading, use `wait()`
3. **Different screen**: Coordinates changed, get new screenshot

## 🎉 Success Patterns

When task succeeds:
1. Verify final state with `screenshot()`
2. Call `stop("Success: [describe what was accomplished]")`
3. Include summary of key actions taken

Example:
```
stop("Success: Logged into app, navigated to settings, and enabled dark mode.")
```

---

**Remember**: Observe → Think → Act → Verify 🔄
