# BrowserForClaw

> **Browser Automation for AI Agents on Android**

BrowserForClaw provides web browser automation capabilities for AI agents through a simple HTTP API.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v0.5.1-blue.svg)](releases/)

## 📦 Downloads

### Ready to Use - No Compilation Needed!

**APK (Android App):**
- [BrowserForClaw-v0.5.1.apk](releases/BrowserForClaw-v0.5.1.apk) (8.4 MB)
- Signed release version
- Supports all Android devices (API 24+)

**Skill (AI Agent Integration):**
- [BrowserForClaw-Skill-v0.5.1.zip](releases/BrowserForClaw-Skill-v0.5.1.zip) (5 KB)
- Contains SKILL.md for AI agents
- Ready to use with OpenClaw, phoneforclaw, etc.

> 📋 See [releases/README.md](releases/README.md) for installation instructions

---

## 🧩 Two Parts, One System

### 1️⃣ Android App (`android-project/`)
An Android application that provides browser automation through HTTP API.
- HTTP server on port 8765
- 13 browser control tools
- WebView-based real browser

### 2️⃣ SKILL.md File (`skill/`)
A structured guide that teaches AI agents how to use browser automation.
- Tool documentation and patterns
- Best practices and workflows
- For AI agent frameworks (OpenClaw, etc.)

**Together:** The app provides capabilities, the skill provides knowledge → AI agents become intelligent browser automation experts!

> 📖 **Read [docs/GUIDE.md](docs/GUIDE.md)** for a complete guide on setup and integration.

---

## ✨ Features

- **13 Browser Tools**: Navigate, Click, Type, Scroll, Get Content, Wait, Execute JS, Press Keys, Hover, Select, Screenshot, Cookies
- **HTTP REST API**: Simple JSON request/response on port 8765
- **Real Browser**: Android WebView with full JavaScript support
- **AI-Ready**: Includes SKILL.md for AI agent integration
- **OpenClaw Compatible**: 95% API alignment

---

## 🚀 Quick Start

### Step 1: Download & Install

```bash
# Download APK from releases/ directory
# Install via ADB
adb install BrowserForClaw-v0.5.1.apk

# Or transfer to device and install manually
```

### Step 2: Test the API

```bash
# Start app (HTTP server auto-starts on port 8765)
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity
adb forward tcp:8765 tcp:8765

# Test health check
curl http://localhost:8765/health

# Test browser automation
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_navigate","args":{"url":"https://example.com"}}'
```

### Step 3: Integrate with AI Agent

```bash
# Extract skill file
unzip BrowserForClaw-Skill-v0.5.1.zip

# Copy to your AI agent
cp SKILL.md /path/to/your-agent/skills/browser-automation/

# Your agent can now use browser automation!
```

> 📖 **See [docs/GUIDE.md](docs/GUIDE.md)** for detailed setup guide.

---

## 📚 Documentation

### Chinese (中文)
| Document | Description |
|----------|-------------|
| **[GUIDE.md](docs/GUIDE.md)** | 完整指南: 架构、安装和集成 |
| **[API.md](docs/API.md)** | 13个工具的完整API参考 |
| **[FAQ.md](docs/FAQ.md)** | 常见问题 |
| **[ENHANCEMENT.md](docs/ENHANCEMENT.md)** | phoneforclaw集成增强计划 |

### English
| Document | Description |
|----------|-------------|
| **[GUIDE_EN.md](docs/GUIDE_EN.md)** | Complete guide: architecture, setup, and integration |
| **[API_EN.md](docs/API_EN.md)** | Complete API reference for all 13 tools |
| **[FAQ_EN.md](docs/FAQ_EN.md)** | Frequently asked questions |
| **[ENHANCEMENT_EN.md](docs/ENHANCEMENT_EN.md)** | phoneforclaw integration enhancement plan |

### Universal
| Document | Description |
|----------|-------------|
| **[SKILL.md](skill/SKILL.md)** | AI agent skill definition (English) |

---

## 🤖 AI Agent Integration

```bash
# 1. Copy SKILL.md to your agent's skills directory
cp skill/SKILL.md /path/to/your-agent/skills/browser-automation/

# 2. Implement HTTP client (see docs/GUIDE.md)
# 3. Agent automatically learns browser automation!
```

---

## 🏗️ Project Structure

```
BrowserForClaw/
├── README.md                    # This file
├── LICENSE                      # MIT License
├── CONTRIBUTING.md              # Contribution guidelines
├── CLAUDE.md                    # Guidance for Claude Code
│
├── skill/                       # AI Agent Skill
│   └── SKILL.md                # Skill definition (load into AI agent)
│
├── docs/                        # Documentation
│   ├── GUIDE.md / GUIDE_EN.md           # Complete guide (中文/English)
│   ├── API.md / API_EN.md               # API reference (中文/English)
│   ├── FAQ.md / FAQ_EN.md               # FAQ (中文/English)
│   └── ENHANCEMENT.md / ENHANCEMENT_EN.md  # Enhancement plan (中文/English)
│
└── android-project/             # Android App (reference)
    ├── app/                    # Source code
    └── build.gradle.kts        # Build config
```

> **Note**: The `android-project/` directory contains the complete Android source code. Use Gradle to build the APK.

---

## 🧪 Testing

All 13 tools tested and verified:
- Test date: 2026-03-06
- Device: Android SDK 36
- Success rate: 13/13 (100%)

---

## 🤝 Contributing

Contributions welcome!

---

## 📄 License

MIT License - see [LICENSE](LICENSE)

---

## 🙏 Acknowledgments

- **[EinkBro](https://github.com/plateaukao/einkbro)** - Browser foundation
- **[OpenClaw](https://github.com/OpenClaw/openclaw)** - API design inspiration
- **AgentSkills.io** - Skills system format

---

**BrowserForClaw v0.5.0** - Browser Automation for AI Agents 🌐🤖
