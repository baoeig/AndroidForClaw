# Discord Extension for AndroidForClaw

Discord Channel 扩展，参考 OpenClaw Discord 扩展实现。

## 功能

- ✅ Discord Bot 集成
- ✅ DM (私聊) 支持
- ✅ Guild Channel (服务器频道) 支持
- ✅ Thread (帖子) 支持
- ✅ Reactions (表情反应)
- ✅ Message Actions (消息按钮/组件)
- ✅ Pairing (配对验证)
- ✅ Tool Policy (工具权限)

## 架构

```
extensions/discord/
├── src/main/java/com/xiaomo/discord/
│   ├── DiscordChannel.kt          # Channel 主入口
│   ├── DiscordClient.kt           # Discord API 客户端
│   ├── DiscordConfig.kt           # 配置管理
│   ├── DiscordAccounts.kt         # 多账户支持
│   ├── DiscordDirectory.kt        # 目录服务 (peers/groups)
│   ├── DiscordProbe.kt            # 连接探测
│   ├── DiscordGateway.kt          # WebSocket Gateway
│   ├── messaging/
│   │   ├── DiscordSender.kt       # 消息发送
│   │   ├── DiscordMedia.kt        # 媒体处理
│   │   ├── DiscordReactions.kt    # 表情反应
│   │   ├── DiscordMention.kt      # @提及
│   │   └── DiscordTyping.kt       # 输入状态
│   ├── policy/
│   │   └── DiscordPolicy.kt       # DM/群组权限策略
│   ├── session/
│   │   ├── DiscordSessionManager.kt  # 会话管理
│   │   ├── DiscordHistoryManager.kt  # 历史消息
│   │   └── DiscordDedup.kt        # 去重
│   └── tools/
│       ├── DiscordToolRegistry.kt # 工具注册
│       └── ...                     # Discord 特定工具
└── build.gradle                    # 构建配置
```

## 配置

在 `/sdcard/AndroidForClaw/config/openclaw.json` 中配置：

```json
{
  "gateway": {
    "discord": {
      "enabled": true,
      "token": "${DISCORD_BOT_TOKEN}",
      "dm": {
        "policy": "pairing",
        "allowFrom": []
      },
      "guilds": {
        "123456789": {
          "channels": ["987654321"],
          "requireMention": true
        }
      },
      "groupPolicy": "allowlist"
    }
  }
}
```

## 参考

- OpenClaw Discord: `~/file/clawdbot/openclaw/extensions/discord`
- Discord.js: https://discord.js.org/
- Discord API: https://discord.com/developers/docs
