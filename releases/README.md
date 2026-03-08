# AndroidForClaw Releases

Pre-built APK packages, ready to use.

## 📦 Latest Version: v2.4.3

### Download Files

| File | Size | Description |
|------|------|-------------|
| [androidforclaw-v2.4.3-debug.apk](androidforclaw-v2.4.3-debug.apk) | 39MB | Main application (Debug version) |
| [androidforclaw-accessibility-v2.4.3.apk](androidforclaw-accessibility-v2.4.3.apk) | 4.3MB | Accessibility Service APK |

### Installation Steps

1. **Download APK**
   - Download both APK files above

2. **Install Apps**
   ```bash
   # Install via ADB
   adb install androidforclaw-v2.4.3-debug.apk
   adb install androidforclaw-accessibility-v2.4.3.apk

   # Or install directly on phone
   ```

3. **Configure API**

   Create config file `/sdcard/AndroidForClaw/config/models.json`:

   ```json
   {
     "mode": "merge",
     "providers": {
       "openrouter": {
         "baseUrl": "https://openrouter.ai/api/v1",
         "apiKey": "YOUR_API_KEY_HERE",
         "api": "openai-completions",
         "models": [
           {
             "id": "anthropic/claude-opus-4",
             "name": "Claude Opus 4",
             "reasoning": true,
             "contextWindow": 200000
           }
         ]
       }
     }
   }
   ```

   Push config to device:
   ```bash
   adb push models.json /sdcard/AndroidForClaw/config/models.json
   ```

4. **Grant Permissions**

   Open the app and grant following permissions:
   - ✅ **Accessibility Service** - Required for device control
   - ✅ **Display Over Apps** - Required for UI overlay
   - ✅ **Media Projection** - Required for screenshots

5. **Configure Channels** (Optional)

   Configure Feishu or Discord: `/sdcard/AndroidForClaw/config/openclaw.json`

   ```json
   {
     "gateway": {
       "feishu": {
         "enabled": true,
         "appId": "YOUR_APP_ID",
         "appSecret": "YOUR_APP_SECRET"
       },
       "discord": {
         "enabled": true,
         "token": "YOUR_DISCORD_BOT_TOKEN"
       }
     }
   }
   ```

6. **Start Using**

   - Send messages in Feishu or Discord
   - Or use the in-app chat interface
   - AI will automatically control your phone!

## 📋 System Requirements

- **Android**: 5.0+ (API 21+)
- **Permissions**: Accessibility Service, Display Over Apps, Media Projection
- **Network**: Internet connection for LLM API (Claude Opus 4 or OpenAI-compatible)

## 🔧 Signing Info

### Main App (androidforclaw-v2.4.3-debug.apk)
- Signature: Android Debug Keystore
- Package: `com.xiaomo.androidforclaw.debug`

### Accessibility Service (androidforclaw-accessibility-v2.4.3.apk)
- Signature: Unsigned (manual signing required or allow unsigned installation)
- Package: `com.xiaomo.androidforclaw.accessibility`

## 📝 Release Notes

### v2.4.3 (2026-03-07)

**New Features:**
- Complete Skills System implementation
- Multi-channel Gateway (Feishu, Discord, HTTP)
- Session management and conversation history
- Extended Thinking with Claude Opus 4.6

**Improvements:**
- Cleaned up unused resources (-2369 lines)
- Removed all legacy project references
- Optimized APK size
- Improved compilation speed

**Architecture:**
- ~85% alignment with OpenClaw
- Agent Loop + Skills + Tools pattern
- Gateway-based multi-channel access

## 🐛 Troubleshooting

Having issues?

1. Check [Documentation](../docs/README.md)
2. Search [Known Issues](https://github.com/xiaomochn/AndroidForClaw/issues)
3. Submit [New Issue](https://github.com/xiaomochn/AndroidForClaw/issues/new)

## 📚 More Resources

- [Full Documentation](../docs/README.md)
- [Quick Start](../README.md#-quick-start)
- [Architecture](../CLAUDE.md)
- [Contributing](../CONTRIBUTING.md)

---

**AndroidForClaw** - Give AI an Android Phone 📱🤖

*Inspired by [OpenClaw](https://github.com/openclaw/openclaw)*
