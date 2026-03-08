# Feishu Channel Module

飞书 Channel 模块，完整对齐 OpenClaw 的 [@m1heng-clawd/feishu](https://github.com/m1heng/clawdbot-feishu) 插件。

## 功能对齐

### ✅ 核心功能 (Phase 1)
- [x] FeishuConfig - 完整配置管理
- [x] FeishuClient - API 客户端（tenant_access_token, HTTP 请求）
- [x] FeishuChannel - Channel 核心
- [x] FeishuWebSocketHandler - WebSocket 连接
- [ ] FeishuWebhookHandler - Webhook 连接（需要 Gateway 集成）

### 🚧 消息功能 (Phase 2)
- [ ] 文本消息发送/接收
- [ ] 卡片消息（Interactive Card）
- [ ] 图片/文件上传下载
- [ ] @ 提及处理
- [ ] 消息编辑/撤回
- [ ] 表情回复（Reactions）

### 🚧 会话管理 (Phase 3)
- [ ] DM Policy（open/pairing/allowlist）
- [ ] Group Policy（open/allowlist/disabled）
- [ ] Session 管理（支持 topic session）
- [ ] 历史记录管理
- [ ] 消息去重

### ✅ 工具集成 (Phase 4) - 完成
- [x] Doc Tools - 文档操作 (4 tools)
- [x] Wiki Tools - 知识库操作 (4 tools)
- [x] Drive Tools - 云空间操作 (4 tools)
- [x] Bitable Tools - 多维表格操作 (5 tools)
- [x] Task Tools - 任务操作 (4 tools)
- [x] Chat Tools - 聊天操作 (4 tools)
- [x] Perm Tools - 权限管理 (3 tools)
- [x] Urgent Tools - 加急通知 (2 tools)
- [x] FeishuToolRegistry - 工具注册中心

### 🚧 高级功能 (Phase 5)
- [ ] Dynamic Agent Creation
- [ ] Typing Indicator
- [ ] Probe (测试连接)
- [ ] Onboarding (新用户引导)
- [ ] Monitor (运行状态监控)
- [ ] Streaming Card (流式更新卡片)

## 架构设计

```
FeishuChannel
├── Core
│   ├── FeishuConfig - 配置管理
│   ├── FeishuClient - API 客户端
│   ├── FeishuChannel - Channel 入口
│   └── Connection Handlers
│       ├── FeishuWebSocketHandler
│       └── FeishuWebhookHandler
├── Messaging
│   ├── FeishuSender - 消息发送
│   ├── FeishuReceiver - 消息接收
│   ├── FeishuMedia - 媒体处理
│   ├── FeishuMention - @ 提及
│   └── FeishuReactions - 表情回复
├── Policy
│   ├── FeishuDmPolicy - DM 策略
│   ├── FeishuGroupPolicy - 群组策略
│   └── FeishuAllowlist - 白名单管理
├── Session
│   ├── FeishuSessionManager - 会话管理
│   ├── FeishuHistoryManager - 历史记录
│   └── FeishuDedup - 消息去重
└── Tools (8 个工具集)
    ├── FeishuDocTools
    ├── FeishuWikiTools
    ├── FeishuDriveTools
    ├── FeishuBitableTools
    ├── FeishuTaskTools
    ├── FeishuChatTools
    ├── FeishuPermTools
    └── FeishuUrgentTools
```

## 使用方式

```kotlin
// 1. 创建配置
val config = FeishuConfig(
    enabled = true,
    appId = "cli_xxx",
    appSecret = "xxx",
    connectionMode = FeishuConfig.ConnectionMode.WEBSOCKET,
    dmPolicy = FeishuConfig.DmPolicy.PAIRING,
    groupPolicy = FeishuConfig.GroupPolicy.ALLOWLIST
)

// 2. 创建 Channel
val channel = FeishuChannel(config)

// 3. 监听事件
channel.eventFlow.collect { event ->
    when (event) {
        is FeishuEvent.Message -> {
            // 处理消息
            handleMessage(event)
        }
        is FeishuEvent.Connected -> {
            Log.i(TAG, "Connected")
        }
        is FeishuEvent.Error -> {
            Log.e(TAG, "Error", event.error)
        }
    }
}

// 4. 启动 Channel
channel.start()

// 5. 发送消息
channel.sendMessage(
    receiveId = "ou_xxx",
    content = "Hello from AndroidForClaw!"
)
```

## 集成到 AndroidForClaw

### Gateway 集成
```kotlin
// 在 GatewayService 中注册 feishu channel
val feishuChannel = FeishuChannel(feishuConfig)
feishuChannel.start()

// 将飞书事件转发到 AgentLoop
feishuChannel.eventFlow.collect { event ->
    when (event) {
        is FeishuEvent.Message -> {
            // 调用 MainEntryNew.run()
            MainEntryNew.run(
                userInput = event.content,
                application = application
            )
        }
    }
}
```

### 配置管理
配置文件位置：`/sdcard/AndroidForClaw/config/channels/feishu.json`

```json
{
  "enabled": true,
  "appId": "cli_xxx",
  "appSecret": "xxx",
  "connectionMode": "websocket",
  "dmPolicy": "pairing",
  "groupPolicy": "allowlist",
  "groupAllowFrom": ["oc_xxx"],
  "requireMention": true
}
```

## 开发计划

### Phase 1: 核心基础 ✅ (当前完成)
- [x] Module 结构
- [x] FeishuConfig
- [x] FeishuClient (API 客户端)
- [x] FeishuChannel (核心入口)
- [x] FeishuWebSocketHandler (基础实现)

### Phase 2: 消息功能 (下一步)
- [ ] 完善 WebSocket 消息处理
- [ ] 文本/卡片消息发送
- [ ] 图片/文件上传下载
- [ ] @ 提及解析和格式化
- [ ] 消息编辑/撤回
- [ ] 表情回复

### Phase 3: 策略和会话
- [ ] DM/Group Policy 完整实现
- [ ] Allowlist 管理
- [ ] Session Manager
- [ ] History Manager
- [ ] Dedup (消息去重)

### Phase 4: 工具集成 ✅ 完成
- [x] 8 个飞书工具集 (30+ 工具)
- [x] FeishuToolRegistry (统一管理)
- [ ] 与 AgentLoop 集成
- [ ] Tool Context 管理

### Phase 5: 高级功能
- [ ] Dynamic Agent
- [ ] Typing Indicator
- [ ] Streaming Card
- [ ] Onboarding
- [ ] Monitor

## 参考资料

- [OpenClaw Feishu Plugin](https://github.com/m1heng/clawdbot-feishu)
- [飞书开放平台文档](https://open.feishu.cn/document)
- [飞书 API 参考](https://open.feishu.cn/document/server-docs/api-call-guide/api-debugging-tool)
