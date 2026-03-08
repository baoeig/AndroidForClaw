# Feishu Channel 完整实现计划

## 当前状态 (Phase 1 - 基础完成)

### ✅ 已完成
1. **Module 结构**
   - build.gradle
   - AndroidManifest.xml
   - settings.gradle 注册

2. **核心配置**
   - FeishuConfig.kt - 完整配置结构（对齐 OpenClaw）
   - 所有配置项：连接模式、策略、工具开关等

3. **API 客户端**
   - FeishuClient.kt - HTTP API 封装
   - tenant_access_token 管理（带缓存）
   - 通用 API 请求方法（GET/POST/PUT/DELETE）

4. **Channel 核心**
   - FeishuChannel.kt - Channel 入口类
   - 事件流（eventFlow）
   - 基础消息发送（sendMessage, sendCard）
   - 用户/群组信息获取

5. **WebSocket Handler**
   - FeishuWebSocketHandler.kt - WebSocket 连接
   - 心跳处理
   - 事件回调处理
   - 消息接收基础逻辑

6. **Webhook Handler**
   - FeishuWebhookHandler.kt - Webhook 占位符（待 Gateway 集成）

---

## Phase 2: 消息功能完善 (当前任务)

### 需要实现的文件

#### 1. FeishuSender.kt - 消息发送器
```kotlin
package com.xiaomo.feishu.messaging

/**
 * 对齐 src/send.ts
 *
 * 功能：
 * - sendMessageFeishu: 发送文本/卡片消息
 * - sendCardFeishu: 发送交互式卡片
 * - updateCardFeishu: 更新卡片内容
 * - editMessageFeishu: 编辑已发送消息
 * - getMessageFeishu: 获取消息详情
 * - deleteMessageFeishu: 删除消息
 * - 消息分块（textChunkLimit）
 * - Markdown 链接规范化
 */
```

#### 2. FeishuReceiver.kt - 消息接收器
```kotlin
/**
 * 处理接收到的消息
 *
 * 功能：
 * - 解析不同类型消息（text, post, image, file, audio, video等）
 * - 提取纯文本内容
 * - 解析 @ 提及
 * - 合并转发消息处理
 * - 消息历史记录
 */
```

#### 3. FeishuMedia.kt - 媒体处理
```kotlin
/**
 * 对齐 src/media.ts
 *
 * 功能：
 * - uploadImageFeishu: 上传图片
 * - uploadFileFeishu: 上传文件
 * - downloadImageFeishu: 下载图片
 * - downloadFileFeishu: 下载文件
 * - sendImageFeishu: 发送图片消息
 * - sendFileFeishu: 发送文件消息
 * - sendMediaFeishu: 发送媒体消息（图片/视频/音频）
 * - getMediaDuration: 获取音视频时长
 * - 媒体大小限制检查
 */
```

#### 4. FeishuMention.kt - @ 提及处理
```kotlin
/**
 * 对齐 src/mention.ts
 *
 * 功能：
 * - extractMentionTargets: 提取提及目标
 * - extractMessageBody: 提取消息正文（移除提及）
 * - isMentionForwardRequest: 判断是否为转发请求
 * - formatMentionForText: 格式化文本消息中的 @
 * - formatMentionForCard: 格式化卡片中的 @
 * - buildMentionedMessage: 构建带提及的消息
 * - buildMentionedCardContent: 构建带提及的卡片
 */
```

#### 5. FeishuReactions.kt - 表情回复
```kotlin
/**
 * 对齐 src/reactions.ts
 *
 * 功能：
 * - addReactionFeishu: 添加表情回复
 * - removeReactionFeishu: 移除表情回复
 * - listReactionsFeishu: 列出消息的所有回复
 * - FeishuEmoji 枚举类（所有支持的表情）
 */
```

#### 6. FeishuCard.kt - 交互式卡片
```kotlin
/**
 * 卡片消息构建器
 *
 * 功能：
 * - CardBuilder DSL
 * - 流式更新卡片（StreamingCard）
 * - 卡片模板
 * - 回调处理
 */
```

---

## Phase 3: 策略和会话管理

### 需要实现的文件

#### 7. FeishuPolicy.kt - 策略管理
```kotlin
/**
 * 对齐 src/policy.ts
 *
 * 功能：
 * - resolveFeishuDmPolicy: DM 策略判断
 * - resolveFeishuGroupPolicy: 群组策略判断
 * - resolveFeishuAllowlistMatch: 白名单匹配
 * - resolveFeishuGroupToolPolicy: 群组工具策略
 * - resolveFeishuReplyPolicy: 回复策略
 * - isFeishuGroupAllowed: 群组是否允许
 * - resolveFeishuGroupCommandMentionBypass: 提及绕过规则
 */
```

#### 8. FeishuSessionManager.kt - 会话管理
```kotlin
/**
 * 会话生命周期管理
 *
 * 功能：
 * - Session 创建/销毁
 * - Topic Session 支持
 * - Session 上下文管理
 * - Session 超时处理
 */
```

#### 9. FeishuHistoryManager.kt - 历史记录管理
```kotlin
/**
 * 消息历史记录
 *
 * 功能：
 * - 历史记录存储
 * - historyLimit / dmHistoryLimit 限制
 * - 历史记录查询
 * - 历史记录清理
 */
```

#### 10. FeishuDedup.kt - 消息去重
```kotlin
/**
 * 对齐 src/dedup.ts
 *
 * 功能：
 * - tryRecordMessage: 记录消息ID
 * - 重复消息检测
 * - 去重缓存管理
 */
```

#### 11. FeishuDirectory.kt - 通讯录
```kotlin
/**
 * 对齐 src/directory.ts
 *
 * 功能：
 * - listFeishuDirectoryPeers: 列出单聊联系人
 * - listFeishuDirectoryGroups: 列出群组
 * - listFeishuDirectoryPeersLive: 实时联系人列表
 * - listFeishuDirectoryGroupsLive: 实时群组列表
 */
```

---

## Phase 4: 工具集成 (8个工具集)

### 需要实现的目录结构

```
feishu-channel/src/main/java/com/xiaomo/feishu/
├── tools/
│   ├── common/
│   │   ├── FeishuToolContext.kt
│   │   └── FeishuToolBase.kt
│   ├── doc/
│   │   ├── FeishuDocTools.kt
│   │   ├── DocCreateTool.kt
│   │   ├── DocReadTool.kt
│   │   ├── DocUpdateTool.kt
│   │   └── DocDeleteTool.kt
│   ├── wiki/
│   │   ├── FeishuWikiTools.kt
│   │   ├── WikiCreateTool.kt
│   │   ├── WikiReadTool.kt
│   │   └── WikiUpdateTool.kt
│   ├── drive/
│   │   ├── FeishuDriveTools.kt
│   │   ├── DriveUploadTool.kt
│   │   ├── DriveDownloadTool.kt
│   │   └── DriveListTool.kt
│   ├── bitable/
│   │   ├── FeishuBitableTools.kt
│   │   ├── BitableCreateTool.kt
│   │   ├── BitableReadTool.kt
│   │   └── BitableUpdateTool.kt
│   ├── task/
│   │   ├── FeishuTaskTools.kt
│   │   ├── TaskCreateTool.kt
│   │   ├── TaskUpdateTool.kt
│   │   └── TaskListTool.kt
│   ├── chat/
│   │   ├── FeishuChatTools.kt
│   │   ├── ChatCreateTool.kt
│   │   ├── ChatAddMemberTool.kt
│   │   └── ChatInfoTool.kt
│   ├── perm/
│   │   ├── FeishuPermTools.kt
│   │   ├── PermCheckTool.kt
│   │   └── PermGrantTool.kt
│   └── urgent/
│       ├── FeishuUrgentTools.kt
│       └── UrgentMessageTool.kt
```

每个工具集对齐 OpenClaw 对应目录的完整功能。

---

## Phase 5: 高级功能

#### 12. FeishuDynamicAgent.kt
```kotlin
/**
 * 对齐 src/dynamic-agent.ts
 * 动态 Agent 创建
 */
```

#### 13. FeishuTyping.kt
```kotlin
/**
 * 对齐 src/typing.ts
 * 输入指示器
 */
```

#### 14. FeishuProbe.kt
```kotlin
/**
 * 对齐 src/probe.ts
 * 连接测试
 */
```

#### 15. FeishuOnboarding.kt
```kotlin
/**
 * 对齐 src/onboarding.ts
 * 新用户引导
 */
```

#### 16. FeishuMonitor.kt
```kotlin
/**
 * 对齐 src/monitor.ts
 * 运行状态监控
 */
```

#### 17. FeishuStreamingCard.kt
```kotlin
/**
 * 对齐 src/streaming-card.ts
 * 流式更新卡片
 */
```

---

## 实现优先级

### P0 (必须 - Phase 2)
1. FeishuSender - 消息发送
2. FeishuReceiver - 消息接收
3. FeishuMention - @ 提及
4. FeishuMedia - 媒体上传下载

### P1 (重要 - Phase 3)
5. FeishuPolicy - 策略管理
6. FeishuSessionManager - 会话管理
7. FeishuHistoryManager - 历史记录
8. FeishuDedup - 去重

### P2 (增强 - Phase 4)
9. 8 个工具集（按需实现）

### P3 (可选 - Phase 5)
10. 高级功能

---

## 测试计划

### 单元测试
- 每个核心类编写单元测试
- Mock 飞书 API 响应
- 覆盖率 > 80%

### 集成测试
- 真实飞书环境测试
- WebSocket 连接测试
- 消息收发测试
- 工具调用测试

### E2E 测试
- 完整对话流程
- 多会话并发
- 错误恢复
- 重连机制

---

## 文档计划

### 用户文档
- 快速开始指南
- 配置说明
- API 参考
- 常见问题

### 开发文档
- 架构设计
- 类图
- 序列图
- 扩展指南

---

## 下一步行动

1. **立即开始** - 实现 Phase 2 的 4 个核心文件
2. **并行开发** - 可以分批实现不同功能模块
3. **持续集成** - 每完成一个模块就集成测试
4. **逐步对齐** - 对照 OpenClaw 源码确保功能完整

当前文件已创建：
- ✅ FeishuConfig.kt
- ✅ FeishuClient.kt
- ✅ FeishuChannel.kt
- ✅ FeishuWebSocketHandler.kt
- ✅ FeishuWebhookHandler.kt (占位符)

下一步：创建 Phase 2 的 4 个文件
