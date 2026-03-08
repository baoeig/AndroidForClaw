---
name: create-skill
description: Create new skills for androidforclaw. Use this when the user asks to create a skill, write a skill, add capabilities, or teach the agent new workflows. Trigger on phrases like "create a skill for", "make a skill that", "add a skill to", "teach the agent how to".
metadata: { "openclaw": { "always": false, "emoji": "🛠️" }, "version": "1.0.0", "category": "meta" }
---

# Create Skill

Create new skills for androidforclaw to teach the agent new capabilities and workflows.

## 🎯 When to Use

Use this skill when the user wants to:

- "Create a skill for testing login flows"
- "Make a skill that monitors app performance"
- "Add a skill to handle user registration"
- "Teach the agent how to debug network issues"
- "Write a skill for data extraction from apps"

## 📋 Skill Structure

### Basic Format

Every skill is a directory containing a `SKILL.md` file with YAML frontmatter + Markdown body.

```
skills/
└── my-skill/
    └── SKILL.md
```

### YAML Frontmatter

```yaml
---
name: skill-name
description: Clear description of what this skill does and when to use it. Include trigger phrases.
metadata:
  always: false        # Load always (true) or on-demand (false)
  emoji: "🎯"         # Display emoji
  version: "1.0.0"    # Semantic version
  category: "testing" # Category (testing, automation, data, etc.)
---
```

**Important**:
- `name`: Lowercase, hyphenated (e.g., `login-testing`, `data-extraction`)
- `description`: Include both what it does AND when to use it (triggers)
- `always: false`: Most skills should be on-demand (not loaded by default)

### Markdown Body

Structure your skill with these sections:

1. **Title and Overview** (required)
2. **When to Use** (required) - Specific triggers and use cases
3. **Available Tools** (required) - List tools this skill uses
4. **Patterns/Examples** (required) - Show concrete examples
5. **Best Practices** (optional) - Tips and guidelines
6. **Troubleshooting** (optional) - Common issues

## 🛠️ Available Tools in androidforclaw

### Android Automation Tools (SkillRegistry)

```
screenshot        - Capture screen + UI tree
get_view_tree     - Get UI hierarchy (lightweight)
tap               - Tap coordinates
swipe             - Swipe gesture
type              - Input text
long_press        - Long press
home              - Press home button
back              - Press back button
open_app          - Launch app by package name
wait              - Sleep/delay
stop              - Stop execution
log               - Log message
notification      - Send notification
```

### General Tools (ToolRegistry)

```
read_file         - Read file from workspace
write_file        - Write file to workspace
edit_file         - Edit file in workspace
list_dir          - List directory contents
exec              - Execute shell command
web_fetch         - Fetch web content
javascript        - Execute JavaScript (QuickJS)
start_activity    - Launch Android Activity
```

### Browser Tools (via BrowserForClaw integration)

```
browser_navigate     - Navigate to URL
browser_click        - Click element
browser_type         - Type in input field
browser_get_content  - Extract page content
browser_wait         - Wait for element/condition
browser_screenshot   - Capture browser screenshot
```

## 📝 Skill Creation Process

### Step 1: Understand Requirements

Ask clarifying questions:
- What is the main goal?
- What Android apps/screens are involved?
- What are the success criteria?
- What edge cases need handling?

### Step 2: Design Workflow

Break down into steps using available tools:

```
1. Observe initial state (screenshot)
2. Navigate to target screen (tap/swipe)
3. Perform actions (tap/type)
4. Verify results (screenshot)
5. Handle errors (conditional logic)
```

### Step 3: Write Skill Content

Use this template structure:

```markdown
---
name: my-skill
description: What it does and when to use it (include trigger phrases)
metadata:
  always: false
  emoji: "🎯"
  version: "1.0.0"
  category: "automation"
---

# Skill Name

Brief overview of what this skill teaches.

## 🎯 When to Use

- Specific use case 1
- Specific use case 2
- Trigger phrase examples

## 🛠️ Available Tools

List and explain the tools this skill uses:

### tool_name
- Purpose and usage
- Example invocation

## 💡 Workflow Pattern

```
Step 1: Observe (screenshot)
Step 2: Navigate (tap/swipe)
Step 3: Action (type/tap)
Step 4: Verify (screenshot)
```

## 📋 Examples

### Example 1: Specific Task

```javascript
// Show concrete code/pseudocode
screenshot()
tap(x, y)
wait(1)
screenshot()
```

Explanation of what happens.

### Example 2: Another Use Case

More examples...

## ⚠️ Important Notes

- Edge cases
- Limitations
- Prerequisites

## 🎓 Best Practices

1. Always verify
2. Handle timeouts
3. Graceful failures

## 🐛 Troubleshooting

Common issues and solutions.
```

### Step 4: Create Skill File

Use tools to create the skill:

```javascript
// Option 1: Use write_file tool
{
  "tool": "write_file",
  "args": {
    "path": "skills/my-skill/SKILL.md",
    "content": "<skill content here>"
  }
}

// Option 2: Use javascript tool for complex generation
{
  "tool": "javascript",
  "args": {
    "code": `
      const skillContent = \`---
name: my-skill
...
\`;
      fs.writeFile('/sdcard/AndroidForClaw/workspace/skills/my-skill/SKILL.md', skillContent);
      return { success: true, path: 'skills/my-skill/SKILL.md' };
    `
  }
}
```

### Step 5: Test and Refine

After creating:
1. Test the skill with actual use cases
2. Refine based on results
3. Update documentation

## 🎨 Skill Categories

Organize skills by category:

- **automation**: UI automation, app testing
- **testing**: Test execution, validation
- **data**: Data processing, extraction
- **network**: API testing, web scraping
- **debugging**: Error analysis, performance
- **integration**: Cross-app workflows
- **utility**: General helper skills

## 📚 Example Skills

### Example 1: Login Testing Skill

```markdown
---
name: login-testing
description: Test login flows in Android apps. Use when user asks to test login, verify authentication, or validate user credentials flow.
metadata:
  always: false
  emoji: "🔐"
  version: "1.0.0"
  category: "testing"
---

# Login Testing Skill

Comprehensive login flow testing for Android apps.

## 🎯 When to Use

- Testing login functionality
- Verifying authentication flows
- Validating credential handling
- Checking error messages

## 🛠️ Available Tools

### screenshot
Capture current screen state before/after actions.

### tap
Tap login fields and buttons.

### type
Input username and password.

### wait
Wait for login processing/navigation.

## 💡 Workflow Pattern

```
1. Open app (open_app)
2. Observe login screen (screenshot)
3. Tap username field (tap)
4. Type username (type)
5. Tap password field (tap)
6. Type password (type)
7. Tap login button (tap)
8. Wait for processing (wait)
9. Verify success (screenshot)
```

## 📋 Examples

### Example 1: Valid Login

```javascript
// 1. Open app
open_app('com.example.app')
wait(2)

// 2. See login screen
screenshot()
// Locate username field at (540, 800)

// 3. Enter credentials
tap(540, 800)  // Username
wait(0.5)
type('testuser@example.com')

tap(540, 1000)  // Password
wait(0.5)
type('password123')

// 4. Submit
tap(540, 1400)  // Login button
wait(3)

// 5. Verify success
screenshot()
// Should see home screen or dashboard
```

### Example 2: Invalid Credentials

```javascript
// Same steps but with invalid credentials
tap(540, 800)
type('wrong@example.com')

tap(540, 1000)
type('wrongpass')

tap(540, 1400)
wait(2)

screenshot()
// Should see error message: "Invalid credentials"
```

## ⚠️ Important Notes

- Take screenshots BEFORE and AFTER each step
- Wait after tapping buttons (UI needs time to respond)
- Verify error messages appear for invalid inputs
- Check navigation to home screen on success

## 🎓 Best Practices

1. **Test multiple scenarios**: valid, invalid, empty fields
2. **Verify UI feedback**: error messages, loading states
3. **Check navigation**: ensure proper screen transitions
4. **Handle edge cases**: network errors, timeout

## 🐛 Troubleshooting

### Login Button Not Working?
- Verify button coordinates with screenshot
- Ensure keyboard is dismissed (tap elsewhere first)
- Check if button is enabled (not grayed out)

### Credentials Not Typing?
- Ensure field is focused (tap it first)
- Add wait() after tap to let keyboard appear
- Check if auto-fill is interfering
```

### Example 2: Data Extraction Skill

```markdown
---
name: data-extraction
description: Extract structured data from Android app UIs. Use when user needs to scrape data, collect information, or export app content.
metadata:
  always: false
  emoji: "📊"
  version: "1.0.0"
  category: "data"
---

# Data Extraction Skill

Extract and process data from Android app interfaces.

## 🎯 When to Use

- Scraping data from app screens
- Collecting information from lists/tables
- Exporting app content
- Monitoring data changes

## 🛠️ Available Tools

### get_view_tree
Get UI hierarchy with text and coordinates (faster than screenshot).

### screenshot
Visual capture for OCR and complex layouts.

### javascript
Process extracted data, transform formats, save to files.

## 💡 Workflow Pattern

```
1. Navigate to data screen
2. Extract UI tree (get_view_tree)
3. Parse elements and text
4. Process with javascript
5. Save to file (write_file)
```

## 📋 Examples

### Example 1: Extract Contact List

```javascript
// 1. Open contacts app
open_app('com.android.contacts')
wait(2)

// 2. Get UI tree
get_view_tree()
// Returns: [
//   { text: "John Doe", bounds: [100, 200, 500, 280] },
//   { text: "555-1234", bounds: [100, 285, 500, 320] },
//   ...
// ]

// 3. Process data with javascript
javascript({
  code: `
    const contacts = [
      { name: "John Doe", phone: "555-1234" },
      { name: "Jane Smith", phone: "555-5678" }
    ];

    // Save to JSON
    fs.writeFile('/sdcard/contacts.json', JSON.stringify(contacts, null, 2));

    return { extracted: contacts.length };
  `
})
```

### Example 2: Extract Product Prices

```javascript
// Navigate to product list
tap(540, 300)  // Categories
wait(1)
tap(540, 500)  // Electronics
wait(2)

// Extract data
get_view_tree()

// Process with javascript
javascript({
  code: `
    const products = [
      { name: "Phone X", price: "$599" },
      { name: "Tablet Y", price: "$399" }
    ];

    // Parse prices and calculate
    const parsed = products.map(p => ({
      name: p.name,
      price: parseFloat(p.price.replace('$', ''))
    }));

    const summary = {
      products: parsed,
      avgPrice: _.mean(parsed.map(p => p.price)),
      totalValue: _.sum(parsed.map(p => p.price))
    };

    fs.writeFile('/sdcard/products.json', JSON.stringify(summary, null, 2));
    return summary;
  `
})
```

## ⚠️ Important Notes

- Use `get_view_tree()` for text extraction (faster)
- Use `screenshot()` for visual elements (OCR needed)
- Process data with `javascript` tool (QuickJS)
- Save results to `/sdcard/AndroidForClaw/workspace/`

## 🎓 Best Practices

1. **Scroll for pagination**: Use swipe() to load more items
2. **Parse incrementally**: Process data in chunks for large datasets
3. **Validate extracted data**: Check for missing/malformed entries
4. **Export in standard formats**: JSON, CSV for compatibility
```

## 🚫 What NOT to Do

### ❌ Don't Hardcode Assumptions

```markdown
❌ "Tap the login button at (540, 1200)"

✅ "Take screenshot, locate login button, then tap its coordinates"
```

### ❌ Don't Skip Verification

```markdown
❌
tap(540, 800)
type("username")
tap(540, 1200)  // Submit immediately

✅
tap(540, 800)
wait(0.5)
screenshot()  // Verify keyboard appeared
type("username")
wait(0.5)
screenshot()  // Verify text entered
tap(540, 1200)
wait(2)
screenshot()  // Verify submission
```

### ❌ Don't Use Unavailable Features

```markdown
❌ "Use npm to install lodash"  # npm not available

✅ "Use javascript tool with built-in _ (lodash-like) functions"

❌ "Run Python script"  # Python not available

✅ "Use javascript tool or exec with shell commands"

❌ "Import React components"  # No frontend framework

✅ "Use screenshot + tap/swipe for UI interactions"
```

## 🎓 androidforclaw vs OpenClaw Differences

| Feature | OpenClaw | androidforclaw |
|---------|----------|----------------|
| **Runtime** | Node.js | QuickJS (embedded) |
| **npm packages** | ✅ Available | ❌ Not available (use built-ins) |
| **async/await** | ✅ Full support | ❌ Limited (use sync in QuickJS) |
| **File system** | Node.js fs | Tool-based (write_file, read_file) |
| **Browser automation** | Playwright | BrowserForClaw (separate app) |
| **Mobile UI** | ❌ Not available | ✅ tap/swipe/screenshot |
| **Accessibility** | ❌ Desktop only | ✅ Android Accessibility Service |

### Adaptation Guidelines

When adapting OpenClaw skills:

1. **Replace Node.js APIs**:
   - `fs.readFile()` → `read_file` tool or `javascript: fs.readFile()`
   - `require()` → Use built-in functions in javascript tool
   - `process.env` → Use Android system properties

2. **Replace async/await (in QuickJS)**:
   - Use synchronous alternatives
   - For actual async needs, structure as multiple tool calls

3. **Replace npm packages**:
   - lodash → Use built-in `_` functions in javascript tool
   - axios → Use built-in `fetch` in javascript tool
   - commander → Use Android broadcast intents

4. **Add Mobile-Specific Steps**:
   - Add screenshot verification steps
   - Add tap/swipe for navigation
   - Add wait() for UI transitions

## 📖 Complete Skill Creation Example

Let's create a complete "app-update-checker" skill:

```markdown
---
name: app-update-checker
description: Check if apps have available updates in Play Store. Use when user asks to check for updates, monitor app versions, or verify latest versions are installed.
metadata:
  always: false
  emoji: "🔄"
  version: "1.0.0"
  category: "utility"
---

# App Update Checker

Monitor and verify app update availability in Google Play Store.

## 🎯 When to Use

- Check if specific apps have updates
- Verify latest versions are installed
- Monitor update availability
- Automate update checks

## 🛠️ Available Tools

### open_app
Launch Play Store: `open_app('com.android.vending')`

### tap / swipe
Navigate Play Store UI.

### get_view_tree / screenshot
Extract version info and update status.

### javascript
Compare versions, generate reports.

## 💡 Workflow Pattern

```
1. Open Play Store
2. Navigate to "My apps & games"
3. Check "Updates" tab
4. Extract app list with update status
5. Generate report
```

## 📋 Example

```javascript
// 1. Open Play Store
open_app('com.android.vending')
wait(3)
screenshot()

// 2. Tap menu
tap(100, 100)  // Menu icon (top-left)
wait(1)

// 3. Navigate to My apps
tap(300, 500)  // "My apps & games"
wait(2)

// 4. Check Updates tab
tap(400, 300)  // "Updates" tab
wait(2)
screenshot()

// 5. Extract update info
get_view_tree()
// Parse results to find apps with "Update" button

// 6. Generate report
javascript({
  code: `
    const apps = [
      { name: "Chrome", hasUpdate: true, version: "120.0" },
      { name: "Gmail", hasUpdate: false, version: "2024.01" }
    ];

    const summary = {
      timestamp: new Date().toISOString(),
      totalApps: apps.length,
      updatesAvailable: apps.filter(a => a.hasUpdate).length,
      apps: apps
    };

    fs.writeFile('/sdcard/update_report.json', JSON.stringify(summary, null, 2));
    return summary;
  `
})
```

## ⚠️ Important Notes

- Play Store UI may vary by device/version
- Requires Google account logged in
- Some enterprise apps may not show in Play Store

## 🎓 Best Practices

1. Take screenshots at each navigation step
2. Handle "No updates available" gracefully
3. Save report with timestamp
4. Handle Play Store authentication prompts
```

## 🚀 Quick Start Checklist

When creating a skill, ensure:

- [ ] YAML frontmatter is valid
- [ ] `description` includes trigger phrases
- [ ] "When to Use" section is clear
- [ ] Available tools are listed and explained
- [ ] At least 2 concrete examples provided
- [ ] Mobile-specific steps included (screenshot, tap, wait)
- [ ] No unavailable features used (npm, Python, etc.)
- [ ] File paths use Android paths (`/sdcard/`, workspace)
- [ ] Error handling and edge cases mentioned
- [ ] Skill is saved to correct location

## 📁 Skill File Locations

### Bundled Skills (shipped with app)
```
app/src/main/assets/skills/
└── my-skill/
    └── SKILL.md
```

### Workspace Skills (user-defined, highest priority)
```
/sdcard/AndroidForClaw/workspace/skills/
└── my-skill/
    └── SKILL.md
```

### Managed Skills (system-managed)
```
/sdcard/AndroidForClaw/.skills/
└── my-skill/
    └── SKILL.md
```

Priority: Workspace > Managed > Bundled

## 🔗 Resources

- [Mobile Operations Skill](./mobile-operations/SKILL.md) - Android automation basics
- [JavaScript Executor Skill](./javascript-executor/SKILL.md) - Data processing
- [Data Processing Skill](./data-processing/SKILL.md) - Array/object manipulation
- CLAUDE.md - Project architecture and guidelines

---

**create-skill** - Meta-skill for teaching the agent new capabilities 🛠️

Sources:
- [Skills - OpenClaw](https://docs.openclaw.ai/tools/skills)
- [Creating Skills | OK OpenClaw](https://openclaw.dog/docs/tools/creating-skills/)
- [skill-creator by openclaw](https://playbooks.com/skills/openclaw/openclaw/skill-creator)
