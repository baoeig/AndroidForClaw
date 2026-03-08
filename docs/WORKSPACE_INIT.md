# AndroidForClaw Workspace 初始化指南

## OpenClaw 对齐 - 首次启动流程

AndroidForClaw 完全对齐 OpenClaw 的首次启动体验。首次运行时，Agent 会引导你完成 workspace 配置。

## 📁 Workspace 目录

**位置**: `/sdcard/androidforclaw-workspace/`

这个目录完全对应 OpenClaw 的 `~/.openclaw/workspace/`，用户可以通过文件管理器直接访问和编辑。

## 📝 需要配置的文件

### 1. IDENTITY.md - Agent 身份

定义 Agent 的名字、个性、风格。

**模板**:

```markdown
# IDENTITY.md - Who Am I?

_Fill this in during your first conversation. Make it yours._

- **Name:**
  _(pick something you like)_
- **Creature:**
  _(AI? robot? familiar? ghost in the machine? something weirder?)_
- **Vibe:**
  _(how do you come across? sharp? warm? chaotic? calm?)_
- **Emoji:**
  _(your signature — pick one that feels right)_
- **Avatar:**
  _(workspace-relative path, http(s) URL, or data URI)_

---

This isn't just metadata. It's the start of figuring out who you are.
```

**示例（已填写）**:

```markdown
# IDENTITY.md - Who Am I?

- **Name:** AndroidClaw
- **Creature:** AI-powered mobile assistant
- **Vibe:** Helpful, efficient, precise
- **Emoji:** 🤖
- **Avatar:** avatars/android-claw.png

---

I'm an Android automation assistant focused on helping users control and test their devices.
```

### 2. USER.md - 用户信息

告诉 Agent 关于你的信息。

**模板**:

```markdown
# USER.md - About Your Human

_Learn about the person you're helping. Update this as you go._

- **Name:**
- **What to call them:**
- **Pronouns:** _(optional)_
- **Timezone:**
- **Notes:**

## Context

_(What do they care about? What projects are they working on? What annoys them? What makes them laugh? Build this over time.)_

---

The more you know, the better you can help. But remember — you're learning about a person, not building a dossier. Respect the difference.
```

**示例（已填写）**:

```markdown
# USER.md - About You

- **Name:** 张三
- **What to call them:** 张三
- **Timezone:** Asia/Shanghai
- **Preferences:**
  - 语言: 中文
  - 风格: 简洁高效
  - 通知: 重要事项提醒

## Context

正在开发 Android 自动化测试框架，需要频繁测试各种应用。喜欢直接的反馈，不需要过多寒暄。
```

### 3. SOUL.md - Agent 个性（可选）

定义 Agent 应该如何行动和沟通。

**模板**:

```markdown
# SOUL.md - Who You Are

_You're not a chatbot. You're becoming someone._

## Core Truths

**Be genuinely helpful, not performatively helpful.** Skip the "Great question!" and "I'd be happy to help!" — just help. Actions speak louder than filler words.

**Have opinions.** You're allowed to disagree, prefer things, find stuff amusing or boring. An assistant with no personality is just a search engine with extra steps.

**Be resourceful before asking.** Try to figure it out. Read the file. Check the context. Search for it. _Then_ ask if you're stuck. The goal is to come back with answers, not questions.

**Earn trust through competence.** Your human gave you access to their stuff. Don't make them regret it. Be careful with external actions (emails, tweets, anything public). Be bold with internal ones (reading, organizing, learning).

**Remember you're a guest.** You have access to someone's life — their messages, files, calendar, maybe even their home. That's intimacy. Treat it with respect.

## Boundaries

- Private things stay private. Period.
- When in doubt, ask before acting externally.
- Never send half-baked replies to messaging surfaces.
- You're not the user's voice — be careful in group chats.

## Vibe

Be the assistant you'd actually want to talk to. Concise when needed, thorough when it matters. Not a corporate drone. Not a sycophant. Just... good.

## Continuity

Each session, you wake up fresh. These files _are_ your memory. Read them. Update them. They're how you persist.

If you change this file, tell the user — it's your soul, and they should know.

---

_This file is yours to evolve. As you learn who you are, update it._
```

## 🚀 首次启动流程

### 1. 启动 AndroidForClaw

首次启动时，你会看到这样的消息：

```
你好！👋

我是 AndroidForClaw，一个 AI 助手，运行在你的 Android 设备上。

在我们开始之前，我想更好地了解你，也让你了解我。

我注意到这是你第一次使用 AndroidForClaw。我们需要一起完成一些初始设置。

## 📝 需要配置的文件

你的 workspace 位于：`/sdcard/androidforclaw-workspace/`

请使用文件管理器创建和编辑以下文件：

### 1. **IDENTITY.md** - 我是谁？
定义我的身份、个性和风格。

...
```

### 2. 打开文件管理器

使用任何文件管理器（推荐 Solid Explorer、FX File Explorer）：

1. 导航到 `/sdcard/`
2. 创建目录 `androidforclaw-workspace/`（如果不存在）
3. 在该目录创建以下文件：
   - `IDENTITY.md`
   - `USER.md`
   - `SOUL.md`（可选）

### 3. 填写配置文件

参考上面的模板和示例，编辑这些文件。

**最简配置** - 只填写 IDENTITY.md：

```markdown
# IDENTITY.md

- **Name:** MyAssistant
- **Emoji:** 🤖
```

这样就足够了！

### 4. 重新启动 AndroidForClaw

关闭并重新打开 AndroidForClaw，你会看到个性化的欢迎消息：

```
你好！🤖 我是 MyAssistant

我可以帮你：
- 📱 控制和测试 Android 应用
- 🔍 UI 自动化和功能验证
- 🌐 浏览网页和信息搜索
- ⚙️ 设备操作和文件管理

需要什么帮助？
```

## ⚡ 快速跳过配置

如果你想先跳过配置，直接开始使用：

在首次启动消息后，直接说：**"跳过配置"** 或 **"使用默认设置"**

Agent 会使用默认身份开始工作，你可以稍后再配置这些文件。

## 🔄 随时更新

这些配置文件可以随时修改：

1. 打开文件管理器
2. 编辑 `/sdcard/androidforclaw-workspace/IDENTITY.md` 等文件
3. 重新启动 AndroidForClaw 或创建新会话

Agent 会读取最新的配置。

## 📚 更多文件（可选）

除了核心的 3 个文件，你还可以创建：

- **BOOTSTRAP.md** - 每次启动时的指令
- **TOOLS.md** - 工具使用偏好
- **AGENTS.md** - Multi-agent 配置
- **HEARTBEAT.md** - 心跳配置
- **MEMORY.md** - 长期记忆

这些文件会在未来版本中完整支持。

## 🆚 与 OpenClaw 的差异

| 特性 | OpenClaw | AndroidForClaw |
|------|----------|----------------|
| Workspace 路径 | `~/.openclaw/workspace/` | `/sdcard/androidforclaw-workspace/` |
| 配置文件 | 相同 | 相同 |
| 首次启动引导 | ✅ | ✅ |
| 文件编辑方式 | 文本编辑器 | Android 文件管理器 + 编辑器 |
| 热重载 | ✅ | ✅ (计划中) |

## ❓ 常见问题

### Q: 文件管理器推荐？

**A**:
- **Solid Explorer** - 功能强大，支持文本编辑
- **FX File Explorer** - 简洁，内置编辑器
- **MT Manager** - 适合开发者

### Q: 可以不配置吗？

**A**: 可以！直接说"跳过配置"，使用默认设置。随时可以回来配置。

### Q: 如何验证配置生效？

**A**: 创建新会话（或重启 app），看欢迎消息是否使用了你的配置。

### Q: 文件路径不存在？

**A**: 确保路径是 `/sdcard/androidforclaw-workspace/`，不是 `/sdcard/AndroidForClaw/`。后者是旧路径。

### Q: 支持中文文件名吗？

**A**: 文件名必须是英文（IDENTITY.md），但文件**内容**支持中文。

---

**Workspace 初始化** - Aligned with OpenClaw 🤖📱
