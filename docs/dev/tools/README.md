# PhoneForClaw Development Tools

开发工具和辅助脚本集合。

---

## 📁 工具列表

### 1. Self-Control Claude 文档

**文件**: `self-control-claude.md`

**用途**: 让 Claude Code 能够通过 ADB 远程控制 PhoneForClaw

**使用方式**:
1. Claude Code 读取这个文档
2. 根据用户指令生成 ADB 命令
3. 执行命令控制设备

**示例**:
```
User: "帮我查看 PhoneForClaw 的错误日志"

Claude: 我来帮你查询错误日志
[执行 ADB 命令]
./self-control-adb.sh query_logs level=E lines:i=50
```

**详细说明**: 查看 [self-control-claude.md](self-control-claude.md)

---

## 🚀 快速开始

### 设置 ADB

确保 ADB 已安装并在 PATH 中：

```bash
# macOS (Homebrew)
brew install android-platform-tools

# 或下载 Android SDK Platform Tools
# https://developer.android.com/studio/releases/platform-tools

# 验证
adb version
```

### 连接设备

```bash
# USB 连接
adb devices

# 网络 ADB（需要先 USB 连接一次）
adb tcpip 5555
adb connect DEVICE_IP:5555
```

### 使用辅助脚本

```bash
# 复制脚本到 tools 目录
cp ../self-control/self-control-adb.sh .
chmod +x self-control-adb.sh

# 查看帮助
./self-control-adb.sh help

# 测试连接
./self-control-adb.sh health
```

---

## 📚 相关文档

- [self-control-claude.md](self-control-claude.md) - Claude Code 使用指南
- [../self-control/ADB_USAGE.md](../self-control/ADB_USAGE.md) - 完整 ADB 使用指南
- [../self-control/DUAL_MODE.md](../self-control/DUAL_MODE.md) - 双模式说明

---

## 🔧 未来工具

计划添加的开发工具：

- [ ] **日志分析器** - 自动分析错误日志
- [ ] **性能监控** - 实时监控应用性能
- [ ] **配置生成器** - 生成测试配置
- [ ] **批量设备管理** - 同时控制多个设备

---

**PhoneForClaw Tools** - 开发者工具集 🛠️
