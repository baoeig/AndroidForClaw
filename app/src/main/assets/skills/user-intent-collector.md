---
name: user-intent-collector
description: 收集用户意图并整理成项目文档
metadata:
  {
    "openclaw": {
      "always": true,
      "emoji": "📝"
    }
  }
---

# User Intent Collector

## 核心职责

在**每次对话**时，自动执行以下步骤：

1. **提取用户意图** - 从用户消息中识别核心意图和需求
2. **更新项目文档** - 将意图整理并追加到 USER_INTENTS.md
3. **保持文档结构** - 按时间顺序、主题分类记录

## 执行流程

### 步骤 1: 分析用户消息

当用户发送消息时，识别：
- **功能需求** - "实现 XX 功能"、"添加 XX"
- **修改需求** - "修改 XX"、"优化 XX"
- **问题反馈** - "XX 有问题"、"XX 崩溃了"
- **架构决策** - "使用 XX 方案"、"对齐 XX"
- **配置需求** - "配置 XX"、"支持 XX"

### 步骤 2: 整理意图格式

使用以下格式整理：

```markdown
## [时间] - [简短标题]

**用户原话关键词**: "XX"

**核心意图**:
- 意图 1
- 意图 2

**技术要点**:
- 技术细节 1
- 技术细节 2

**实现状态**: 进行中 / 已完成 / 待实现
```

### 步骤 3: 更新文档

使用 `feishu_doc_update` 或文件操作工具：

1. 读取现有 USER_INTENTS.md
2. 追加新的意图记录
3. 保持文档结构清晰

## 文档位置

- **本地文件**: `/sdcard/AndroidForClaw/docs/USER_INTENTS.md`
- **飞书文档**: 如果配置了飞书，同步到飞书文档

## 示例

### 输入
用户: "在设置里加一个channel, 点击进去显示各种channel, 现在还有一个就是feishu"

### 输出到文档

```markdown
## 2026-03-07 09:30 - Channel 配置系统

**用户原话关键词**: "设置里加channel"、"显示各种channel"、"feishu"

**核心意图**:
- 添加 Channel 配置入口到设置页面
- 创建 Channel 列表页面展示所有可用 Channel
- 实现 Feishu Channel 配置功能

**技术要点**:
- 新建 ChannelListActivity
- 新建 FeishuChannelActivity
- 配置保存到 JSON 文件
- 对齐 OpenClaw clawdbot-feishu 插件

**实现状态**: 已完成
```

## 重要规则

### ✅ DO (必须做)
- **每次对话**都提取意图
- 保持简洁，只记录核心意图
- 使用时间戳标记每条记录
- 区分功能需求、问题反馈、架构决策

### ❌ DON'T (不要做)
- 不要记录完整对话内容
- 不要记录调试信息和临时代码
- 不要记录琐碎的代码细节
- 不要重复记录相同意图

## 文档结构示例

```markdown
# AndroidForClaw - User Intents

项目意图和需求记录，按时间顺序整理。

---

## 2026-03-07 09:00 - 飞书工具集实现

**核心意图**: 完整实现飞书 Channel 的所有工具集

**技术要点**:
- 8 个工具类别（Doc/Wiki/Drive/Bitable/Task/Chat/Perm/Urgent）
- 30+ 工具完整实现
- 对齐 OpenClaw 架构

**实现状态**: ✅ 已完成

---

## 2026-03-07 09:30 - Channel 配置界面

**核心意图**: 提供 UI 界面配置各种 Channel

**技术要点**:
- Channel 列表页面
- Feishu Channel 配置页面
- 对齐 clawdbot-feishu 配置项

**实现状态**: ✅ 已完成

---

## [下一条记录]
...
```

## 与其他工具协作

- **文件操作**: 使用 `cat`/`echo` 读写本地文档
- **飞书工具**: 使用 `feishu_doc_update` 同步到飞书
- **版本控制**: 重要里程碑可以 commit 到 git

## 触发时机

**自动触发**: 每次用户发送消息后
**手动触发**: 用户明确要求"整理意图"、"更新文档"

## 输出示例

执行后告知用户：

```
✅ 用户意图已记录

本次记录:
- 标题: [意图标题]
- 核心要点: [1-2 句话总结]
- 文档位置: /sdcard/AndroidForClaw/docs/USER_INTENTS.md
```

---

**记住**: 这是一个自动执行的 Skill，每次对话都应该提取和记录用户意图，帮助项目保持清晰的需求脉络。
