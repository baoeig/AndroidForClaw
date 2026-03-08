# AndroidForClaw Documentation

Welcome to AndroidForClaw! Give Claw a Phone. 📱

---

## 🚀 Quick Navigation

### Getting Started

- **[Getting Started](./start/getting-started.md)** - Set up in 5 minutes
- **[Installation](./start/installation.md)** - Build and install guide
- **[Configuration](./start/configuration.md)** - API keys and settings
- **[First Task](./start/first-task.md)** - Run your first agent task

### Core Concepts

- **[Architecture Overview](./concepts/architecture.md)** - System design
- **[Agent Loop](./concepts/agent-loop.md)** - Core execution mechanism
- **[Skills System](./concepts/skills.md)** - Knowledge and code separation
- **[Session Management](./concepts/sessions.md)** - Conversation persistence

### Tools & Capabilities

- **[Android Tools](./tools/android.md)** - screenshot, tap, swipe, type
- **[Tool Reference](./tools/reference.md)** - Complete tool list
- **[Custom Skills](./tools/custom-skills.md)** - Create your own skills

### Gateway & Channels

- **[Gateway Overview](./gateway/overview.md)** - Multi-channel architecture
- **[WebSocket API](./gateway/websocket.md)** - RPC protocol
- **[REST API](./gateway/rest.md)** - HTTP endpoints
- **[Channels](./channels/overview.md)** - App UI, WebUI, ADB

### Development

- **[Testing Guide](./debug/testing.md)** - Test methods and scripts
- **[Debugging](./debug/debugging.md)** - Logs and troubleshooting
- **[Contributing](./reference/contributing.md)** - Development guidelines

### Reference

- **[API Reference](./reference/api.md)** - Complete API documentation
- **[Configuration Reference](./reference/config.md)** - All config options
- **[OpenClaw Alignment](./reference/alignment.md)** - Alignment status

---

## 📖 Documentation Structure

```
docs/
├── index.md                    # This file
│
├── start/                      # Getting Started
│   ├── getting-started.md      # Quick setup guide
│   ├── installation.md         # Build and install
│   ├── configuration.md        # API configuration
│   └── first-task.md           # First agent task
│
├── concepts/                   # Core Concepts
│   ├── architecture.md         # System architecture
│   ├── agent-loop.md           # Agent loop mechanism
│   ├── skills.md               # Skills system
│   └── sessions.md             # Session management
│
├── tools/                      # Tools & Capabilities
│   ├── android.md              # Android-specific tools
│   ├── reference.md            # Complete tool list
│   └── custom-skills.md        # Creating custom skills
│
├── gateway/                    # Gateway & API
│   ├── overview.md             # Gateway architecture
│   ├── websocket.md            # WebSocket RPC
│   └── rest.md                 # REST API
│
├── channels/                   # Channels
│   ├── overview.md             # Channel concept
│   ├── app-ui.md               # Android app UI
│   ├── webui.md                # Web interface
│   └── adb.md                  # ADB commands
│
├── debug/                      # Debugging
│   ├── testing.md              # Testing guide
│   ├── debugging.md            # Debug techniques
│   └── logs.md                 # Log analysis
│
├── reference/                  # Reference
│   ├── api.md                  # API reference
│   ├── config.md               # Configuration
│   ├── alignment.md            # OpenClaw alignment
│   └── contributing.md         # Contributing guide
│
└── platforms/                  # Platform-specific
    └── android.md              # Android platform details
```

---

## 🎯 What is AndroidForClaw?

AndroidForClaw is an **AI-powered Android automation agent** that enables Claude to observe and control Android devices through natural language.

**Part of the forClaw family**:
- 🦞 **OpenClaw** - Personal AI assistant framework (Pi/Linux)
- 📱 **AndroidForClaw** - Android device control (this project)
- 🌐 **BrowserForClaw** - Web automation (planned)

**Key Features**:
- 📸 **Vision** - See what's on screen via screenshots
- 🤖 **Control** - Tap, swipe, type, navigate
- 🧠 **Intelligence** - Claude Opus 4.6 + Extended Thinking
- 🔧 **Skills** - Extensible knowledge system
- 🌐 **Gateway** - Multi-channel access (WebUI, ADB, etc.)

---

## 💡 Philosophy

AndroidForClaw follows OpenClaw's core principles:

### 1. Knowledge & Code Separation

```
Tools (Capabilities)          Skills (Knowledge)
─────────────────────────    ─────────────────────
Kotlin/Java code             Markdown documents
Provides functions           Teaches strategy
Compiled into app            Loaded on-demand
Executes operations          Provides guidance
```

### 2. Agent Loop Architecture

Simple and powerful:
```
LLM → Tool Calls → Observations → LLM → ...
```

No complex pipelines, no hardcoded workflows. The AI decides what to do next.

### 3. Platform-Specific Adaptation

OpenClaw's design adapted for Android:
- **Tools**: exec → accessibility actions
- **Skills**: File-based → Asset-bundled (future: dynamic loading)
- **Gateway**: Multi-process → Single app (future: remote gateway)

---

## 🌟 Why AndroidForClaw?

Compared to traditional Android automation:

| Traditional Automation | AndroidForClaw |
|-----------------------|--------------|
| Hardcoded scripts | Natural language |
| Brittle selectors | Vision-based (screenshot) |
| Manual maintenance | Self-adapting |
| Fixed workflows | Dynamic planning |
| No context | Conversation memory |

**Use Cases**:
- 🧪 App testing with natural language
- 🤖 Personal assistant on your phone
- 📱 Remote device control
- 🔄 Automated workflows
- 🎮 Game automation (with user guidance)

---

## 📚 Learning Path

### Path 1: Quick Start (30 minutes)

1. Read [Getting Started](./start/getting-started.md)
2. Follow [Installation](./start/installation.md)
3. Complete [First Task](./start/first-task.md)
4. Try [Testing Guide](./debug/testing.md)

### Path 2: Deep Dive (2 hours)

1. Complete Path 1
2. Read [Architecture](./concepts/architecture.md)
3. Read [Agent Loop](./concepts/agent-loop.md)
4. Read [Skills System](./concepts/skills.md)
5. Try [Custom Skills](./tools/custom-skills.md)

### Path 3: Contributing (4 hours)

1. Complete Path 2
2. Read [Contributing Guide](./reference/contributing.md)
3. Read [OpenClaw Alignment](./reference/alignment.md)
4. Read [API Reference](./reference/api.md)
5. Pick an area to contribute

---

## 🔗 External Resources

- **OpenClaw**: [github.com/openclaw/openclaw](https://github.com/openclaw/openclaw)
- **AgentSkills.io**: Coming soon - Skills marketplace
- **forClaw Project**: Parent project (multi-platform AI)

---

## 💬 Community & Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/androidforclaw/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/androidforclaw/discussions)
- **OpenClaw Discord**: Join the community

---

**Last Updated**: 2026-03-06
**Version**: v2.5.0
**Status**: Active Development
