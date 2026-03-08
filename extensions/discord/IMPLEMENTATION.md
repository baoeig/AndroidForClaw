# Discord Extension Implementation Guide

Discord 扩展实现进度和架构说明。

## 已完成

✅ **核心文件**
- `DiscordConfig.kt` - 配置数据类
- `DiscordClient.kt` - Discord REST API 客户端
- `build.gradle` - 构建配置
- `AndroidManifest.xml` - 清单文件
- `README.md` - 功能说明

✅ **核心模块**
- `DiscordChannel.kt` - 主入口、消息路由、生命周期管理
- `DiscordGateway.kt` - WebSocket Gateway、实时消息接收、心跳、重连
- `DiscordAccounts.kt` - 多账户管理、配置解析、默认账户
- `DiscordDirectory.kt` - 目录服务、listPeers、listGroups、自动发现
- `DiscordProbe.kt` - 连接探测、健康检查、状态监控

✅ **Messaging 模块**
- `messaging/DiscordSender.kt` - 消息发送封装、格式化
- `messaging/DiscordReactions.kt` - 表情反应管理
- `messaging/DiscordMention.kt` - @提及处理
- `messaging/DiscordTyping.kt` - 输入状态指示器

✅ **Policy 模块**
- `policy/DiscordPolicy.kt` - DM/群组权限策略

✅ **Session 模块**
- `session/DiscordSessionManager.kt` - 会话管理
- `session/DiscordHistoryManager.kt` - 历史消息管理
- `session/DiscordDedup.kt` - 消息去重

## 待实现

### Messaging 模块

- **DiscordMedia.kt**
  - 参考：`extensions/feishu/messaging/FeishuMedia.kt`
  - 职责：媒体文件上传/下载
  - 注：Discord 媒体上传需要额外实现

## 集成步骤

### 1. 更新 settings.gradle

```gradle
include ':extensions:discord'
```

### 2. 更新 app/build.gradle

```gradle
dependencies {
    implementation project(':extensions:discord')
}
```

### 3. 配置文件

在 `/sdcard/AndroidForClaw/config/openclaw.json` 添加：

```json
{
  "gateway": {
    "discord": {
      "enabled": true,
      "token": "${DISCORD_BOT_TOKEN}",
      "dm": {
        "policy": "pairing"
      },
      "guilds": {
        "YOUR_GUILD_ID": {
          "channels": ["CHANNEL_ID"],
          "requireMention": true
        }
      }
    }
  }
}
```

### 4. 主应用集成

在 `MyApplication.kt` 中：

```kotlin
import com.xiaomo.discord.DiscordChannel

// 启动 Discord Channel
private fun startDiscordChannelIfEnabled() {
    try {
        val config = configLoader.loadOpenClawConfig()
        val discordConfig = config.gateway?.get("discord")

        if (discordConfig?.enabled == true) {
            Log.i(TAG, "🤖 Starting Discord Channel...")
            DiscordChannel.start(this, discordConfig)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to start Discord Channel", e)
    }
}
```

## Discord API 参考

- REST API: https://discord.com/developers/docs/reference
- Gateway API: https://discord.com/developers/docs/topics/gateway
- Gateway Intents: 需要在 Discord Developer Portal 启用
  - GUILD_MESSAGES
  - DIRECT_MESSAGES
  - MESSAGE_CONTENT (特权 Intent)

## Bot 创建步骤

1. 访问 https://discord.com/developers/applications
2. 创建新 Application
3. 在 Bot 页面创建 Bot
4. 复制 Bot Token
5. 启用必要的 Intents (GUILD_MESSAGES, DIRECT_MESSAGES, MESSAGE_CONTENT)
6. 生成邀请链接 (Bot Permissions: Send Messages, Read Message History, Add Reactions)
7. 邀请 Bot 到服务器

## 下一步

1. 实现 `DiscordGateway.kt` - WebSocket 连接
2. 实现 `DiscordChannel.kt` - 主入口和消息路由
3. 实现 Messaging 模块 - 消息发送和格式化
4. 实现 Policy 模块 - 权限控制
5. 集成到主应用
6. 测试端到端流程

## 注意事项

- Discord Bot Token 必须保密，不要提交到代码仓库
- 使用环境变量 `${DISCORD_BOT_TOKEN}` 存储
- MESSAGE_CONTENT Intent 是特权 Intent，需要在 Discord Portal 申请
- 遵守 Discord Rate Limits (每秒 5 请求)
