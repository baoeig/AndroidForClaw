# Feishu Skills

AndroidForClaw 飞书技能集 - 完全对齐 OpenClaw 飞书插件。

## 📦 技能清单

| 技能 | 描述 | 工具数 | 状态 |
|------|------|--------|------|
| [feishu-doc](./feishu-doc/) | 文档读写操作 | 4 | ✅ |
| [feishu-drive](./feishu-drive/) | 云空间文件管理 | 4 | ✅ |
| [feishu-wiki](./feishu-wiki/) | 知识库导航 | 4 | ✅ |
| [feishu-perm](./feishu-perm/) | 权限管理 | 3 | ✅ |
| [feishu-bitable](./feishu-bitable/) | 多维表格操作 | 5 | ✅ |
| [feishu-task](./feishu-task/) | 任务管理 | 4 | ✅ |
| [feishu-chat](./feishu-chat/) | 聊天管理 | 4 | ✅ |
| [feishu-urgent](./feishu-urgent/) | 加急通知 | 2 | ✅ |

**总计**: 8 个技能集，30+ 工具

## 🎯 与 OpenClaw 对齐

AndroidForClaw 的飞书技能完全对齐 OpenClaw 的 `@m1heng/clawdbot-feishu` 插件：

| OpenClaw Skill | AndroidForClaw Skill | 对齐状态 |
|----------------|----------------------|----------|
| feishu-doc | [feishu-doc](./feishu-doc/) | ✅ 完全对齐 |
| feishu-drive | [feishu-drive](./feishu-drive/) | ✅ 完全对齐 |
| feishu-wiki | [feishu-wiki](./feishu-wiki/) | ✅ 完全对齐 |
| feishu-perm | [feishu-perm](./feishu-perm/) | ✅ 完全对齐 |
| - | [feishu-bitable](./feishu-bitable/) | ✨ AndroidForClaw 扩展 |
| - | [feishu-task](./feishu-task/) | ✨ AndroidForClaw 扩展 |
| - | [feishu-chat](./feishu-chat/) | ✨ AndroidForClaw 扩展 |
| - | [feishu-urgent](./feishu-urgent/) | ✨ AndroidForClaw 扩展 |

## 📐 技能结构

每个技能包含：

```
feishu-xxx/
├── SKILL.md           # 技能文档（对齐 AgentSkills.io 格式）
└── references/        # 参考文档（可选）
```

### SKILL.md 格式

```markdown
---
name: skill-name
description: |
  Skill description
---

# Skill Title

## Actions

### Action Name
\```json
{ "action": "xxx", "param": "value" }
\```

## AndroidForClaw Implementation

**Tool Class**: `FeishuXxxTools.kt`
**Available Tools**: ...
**Example Usage**: ...
```

## 🔧 工具实现

所有飞书技能的工具实现位于：

```
extensions/feishu/src/main/java/com/xiaomo/feishu/tools/
├── doc/               # FeishuDocTools.kt
├── drive/             # FeishuDriveTools.kt
├── wiki/              # FeishuWikiTools.kt
├── perm/              # FeishuPermTools.kt
├── bitable/           # FeishuBitableTools.kt
├── task/              # FeishuTaskTools.kt
├── chat/              # FeishuChatTools.kt
├── urgent/            # FeishuUrgentTools.kt
└── FeishuToolRegistry.kt  # 工具注册中心
```

## 🚀 使用方式

### 1. 在 AgentLoop 中加载技能

```kotlin
// 加载技能到 SkillRegistry
val skillRegistry = SkillRegistry()
skillRegistry.loadFeishuSkills()

// AgentLoop 会自动使用这些技能
val agentLoop = AgentLoop(
    toolRegistry = toolRegistry,
    skillRegistry = skillRegistry
)
```

### 2. 直接调用工具

```kotlin
// 获取工具注册中心
val toolRegistry = FeishuToolRegistry(config, client)

// 调用文档工具
val docTools = toolRegistry.getDocTools()
val result = docTools.readDoc(docToken = "ABC123def")

// 调用任务工具
val taskTools = toolRegistry.getTaskTools()
val result = taskTools.createTask(
    title = "New Task",
    description = "Description"
)
```

## 📚 文档结构

每个技能文档包含：

1. **Frontmatter** - 技能名称和描述
2. **Actions** - 所有可用操作和 JSON 参数
3. **AndroidForClaw Implementation** - Kotlin 实现说明
4. **Permissions** - 所需飞书权限
5. **Use Cases** - 使用场景
6. **Best Practices** - 最佳实践

## 🔑 权限配置

飞书技能需要以下权限：

| 技能 | 所需权限 |
|------|---------|
| feishu-doc | `docx:document`, `docx:document:readonly` |
| feishu-drive | `drive:drive`, `drive:drive:readonly` |
| feishu-wiki | `wiki:wiki`, `wiki:wiki:readonly` |
| feishu-perm | `drive:permission` |
| feishu-bitable | `bitable:app`, `bitable:app:readonly` |
| feishu-task | `task:task`, `task:task:readonly` |
| feishu-chat | `im:chat`, `im:chat:readonly` |
| feishu-urgent | `im:message:send_as_bot`, `im:message` |

在飞书开放平台配置这些权限：https://open.feishu.cn/app

## 🎓 学习资源

- [OpenClaw Feishu Plugin](https://github.com/m1heng/clawdbot-feishu)
- [飞书开放平台文档](https://open.feishu.cn/document)
- [AgentSkills.io 格式规范](https://agentskills.io)

## 🤝 贡献

欢迎贡献新的飞书技能！请参考现有技能的格式和结构。

---

**AndroidForClaw Feishu Skills** - 让 AI 控制飞书 🤖📱
