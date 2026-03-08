# AndroidForClaw 测试指南

本文档提供 AndroidForClaw 的测试方法和调试工具。

---

## 🎯 测试目标

- ✅ 验证 Agent 执行流程
- ✅ 检查消息同步（Android ↔ WebUI）
- ✅ 验证会话持久化
- ✅ 调试工具调用
- ✅ 检查 Gateway 连接

---

## 🛠️ 测试工具

### 1. ADB 消息测试

**脚本**: `test_chat.sh`

**功能**: 通过 ADB 发送消息给 Agent，查看执行日志

**用法**:
```bash
./test_chat.sh "打开微信"
```

**输出**:
- 清空 logcat
- 发送 Intent 广播
- 等待 5 秒
- 显示关键日志（MainEntryNew, AgentLoop, SkillRegistry）

**适用场景**:
- Quickly test Agent functionality
- 调试工具调用
- 验证 Session 保存

---

### 2. WebUI 同步测试

**脚本**: `test_webui_chat.sh`

**功能**: 测试消息在 WebUI 和 Android 之间的同步

**用法**:
```bash
./test_webui_chat.sh "同步测试消息"
```

**输出**:
- 清空 logcat
- 发送消息
- 等待 5 秒（LLM 响应时间）
- 显示广播日志
- 显示 Gateway 连接状态

**验证点**:
- ✅ 消息被广播 (`📤 [Broadcast]`)
- ✅ WebSocket 连接正常
- ✅ WebUI 收到 `chat.message` 事件

**适用场景**:
- 测试多端同步
- 验证 Gateway 广播
- 调试 WebSocket 连接

---

### 3. 实时日志查看

**脚本**: `read_logs.sh`

**功能**: 实时查看所有关键日志

**用法**:
```bash
./read_logs.sh
```

**输出**: 实时流式日志，包含：
- 🚀 MainEntryNew
- 🔄 AgentLoop
- 🔧 SkillRegistry
- 🌐 GatewayServer
- 💬 ChatViewModel
- 📤 Broadcast

**适用场景**:
- 实时监控执行过程
- 调试工具调用
- 观察消息流向

**提示**: 在一个终端运行 `./read_logs.sh`，在另一个终端发送测试消息。

---

## 📱 手动测试流程

### 测试 1: 基本对话

**步骤**:
1. 打开 Android App 的 MainActivity (聊天界面)
2. 输入消息: "你好"
3. 观察 UI 显示

**预期结果**:
- ✅ 用户消息立即显示
- ✅ 出现 "正在思考..." 提示
- ✅ 3 秒内显示 AI 回复
- ✅ 消息保存到 Session

**验证命令**:
```bash
# 查看 Session 文件
adb shell cat /data/data/com.openclaw.phone.debug/files/sessions/default.json | head -50
```

---

### 测试 2: 上下文记忆

**步骤**:
1. 发送消息: "我叫张三"
2. 等待回复
3. 发送消息: "我叫什么名字？"

**预期结果**:
- ✅ AI 回复 "你叫张三" 或类似内容
- ✅ 证明 Agent 记住了前面的对话

**验证点**:
- Session 包含两轮对话
- AgentLoop 加载了上下文（最近 20 条消息）

**验证命令**:
```bash
adb logcat -d | grep "📚.*加载上下文"
```

---

### 测试 3: WebUI 同步

**前置条件**:
```bash
# 端口转发
adb forward tcp:8080 tcp:8080
adb forward tcp:5174 tcp:5174

# 启动 WebUI (dev mode)
cd ui && npm run dev
```

**步骤**:
1. 打开浏览器: `http://localhost:5174/`
2. 在 Android App 发送消息: "Android 测试"
3. 观察 WebUI 是否收到消息
4. 在 WebUI 发送消息: "WebUI 测试"
5. 观察 Android App 是否显示（需等待 3 秒刷新）

**预期结果**:
- ✅ WebUI 实时收到 Android 消息（<100ms）
- ✅ Android 收到 WebUI 消息（<3s 轮询延迟）
- ✅ 两端消息一致

**验证命令**:
```bash
# 查看广播日志
./test_webui_chat.sh "同步验证"

# 查看 WebSocket 连接
adb logcat -d | grep "WebSocket.*opened\|Active connections"
```

---

### 测试 4: 工具调用

**步骤**:
1. 发送消息: "截个屏"
2. 观察 Agent 行为

**预期结果**:
- ✅ Agent 调用 `screenshot` 工具
- ✅ 返回截图路径
- ✅ Session 包含 tool_result

**验证命令**:
```bash
./test_chat.sh "截个屏"
```

**日志输出**:
```
🔧 [SkillRegistry] 执行 Skill: screenshot
✅ [SkillRegistry] 截图成功: /sdcard/AndroidForClaw/screenshots/...
```

---

## 🐛 常见测试问题

### Q: Android UI 不显示消息

**症状**: 发送消息后，聊天窗口没有更新

**排查**:
```bash
# 1. 检查 SessionManager 是否正常
adb logcat -d | grep "ChatViewModel.*加载"

# 2. 检查 Session 文件
adb shell ls -lh /data/data/com.openclaw.phone.debug/files/sessions/

# 3. 检查消息数量
adb logcat -d | grep "找到历史消息"
```

**可能原因**:
- MainEntryNew 未初始化
- Session 文件损坏
- 自动刷新循环未运行

---

### Q: WebUI 收不到消息

**症状**: WebUI 连接成功，但看不到新消息

**排查**:
```bash
# 1. 检查 Gateway 是否启动
adb logcat -d | grep "GatewayServer.*initialized"

# 2. 检查 WebSocket 连接
adb logcat -d | grep "WebSocket opened"

# 3. 检查广播调用
adb logcat -d | grep "📤.*Broadcast.*chat.message"

# 4. 检查前端日志
# 打开浏览器 F12 Console，查看是否收到事件
```

**可能原因**:
- Gateway 未启动（Service 未运行）
- WebSocket 连接超时
- 广播未调用
- 前端未监听事件

---

### Q: 上下文不工作

**症状**: Agent 不记得之前的对话

**排查**:
```bash
# 1. 检查 Session 是否保存
adb shell cat /data/data/com.openclaw.phone.debug/files/sessions/default.json | grep -c '"role"'

# 2. 检查是否加载上下文
adb logcat -d | grep "📚.*上下文"

# 3. 查看传递给 LLM 的消息数量
adb logcat -d | grep "传递.*条消息"
```

**可能原因**:
- Session 未保存
- runWithSession() 未传递 contextHistory
- contextHistory 参数为空

---

### Q: 工具调用失败

**症状**: Agent 调用工具后返回错误

**排查**:
```bash
# 查看工具执行日志
adb logcat -d | grep "SkillRegistry.*执行\|SkillRegistry.*失败"

# 查看具体错误
./test_chat.sh "截个屏"
```

**可能原因**:
- 权限不足（截图需要 MediaProjection）
- 参数错误
- Accessibility Service 未启用

---

## 📊 性能基准

### 消息延迟

| 场景 | 预期延迟 | 说明 |
|------|----------|------|
| **Android → Session** | <10ms | 同步写入文件 |
| **Android → WebUI** | <100ms | WebSocket 实时推送 |
| **WebUI → Android** | <3s | 轮询刷新延迟 |
| **LLM 响应** | 2-10s | 取决于任务复杂度 |

### 资源使用

| 资源 | 典型值 | 说明 |
|------|--------|------|
| **Session 文件** | 10KB-1MB | 取决于对话长度 |
| **内存占用** | ~50MB | MainEntryNew + SessionManager |
| **WebSocket 连接** | 1 个/客户端 | 每个 WebUI 标签页 1 个 |

---

## 🔧 调试技巧

### 1. 实时监控

在一个终端运行:
```bash
./read_logs.sh
```

在另一个终端发送测试:
```bash
./test_chat.sh "测试消息"
```

### 2. 精准日志过滤

```bash
# 只看 Agent 执行
adb logcat -d | grep "MainEntryNew\|AgentLoop"

# 只看消息广播
adb logcat -d | grep "Broadcast.*chat.message"

# 只看 Session 操作
adb logcat -d | grep "SessionManager"

# 只看 UI 同步
adb logcat -d | grep "ChatViewModel"
```

### 3. 清空 Session

```bash
# 删除所有会话
adb shell rm -rf /data/data/com.openclaw.phone.debug/files/sessions/

# 重启 App
adb shell am force-stop com.openclaw.phone.debug
adb shell am start -n com.openclaw.phone.debug/.ui.activity.MainActivity
```

### 4. WebSocket 调试

打开浏览器 DevTools (F12):
- **Console**: 查看 Gateway 日志
- **Network**: 查看 WebSocket 连接状态
- **WS**: 查看 WebSocket 消息帧

---

## 📝 测试清单

### 基础功能

- [ ] App 启动成功
- [ ] MainActivity 聊天界面正常
- [ ] 输入框可以输入
- [ ] 发送按钮可点击

### Agent 功能

- [ ] 发送消息后有回复
- [ ] 工具调用成功（如 screenshot）
- [ ] 上下文记忆正常
- [ ] Session 持久化正常

### 同步功能

- [ ] Android → WebUI 实时同步
- [ ] WebUI → Android 同步（3秒内）
- [ ] 重启 App 后历史加载正常
- [ ] 多端消息一致

### Gateway 功能

- [ ] WebSocket 连接成功
- [ ] 心跳保持连接
- [ ] 断线自动重连
- [ ] RPC 请求正常响应

---

## 🚀 自动化测试 (未来)

目前测试主要是手动的。未来计划：

- [ ] 单元测试 - SkillRegistry, SessionManager
- [ ] 集成测试 - AgentLoop end-to-end
- [ ] UI 测试 - Espresso/Compose Testing
- [ ] E2E 测试 - WebUI ↔ Android 完整流程

---

**更新日期**: 2026-03-06
**版本**: v2.5.0
