# ADB 日志抓取命令

用于分析AndroidForClaw说明书更新功能的常用ADB命令。

## 基础命令

### 清空日志缓冲区
```bash
adb logcat -c
```

### 查看设备连接状态
```bash
adb devices
```

## 日志过滤命令

### 按标签过滤（推荐）
```bash
# 抓取说明书更新相关日志
adb logcat ManualUpdater:D AppManualAgent:D PromptBuilder:D DifyRepository:D "*:S"

# 只看ManualUpdater的日志
adb logcat ManualUpdater:D "*:S"

# 只看AppManualAgent的日志
adb logcat AppManualAgent:D "*:S"

# 只看PromptBuilder的日志
adb logcat PromptBuilder:D "*:S"
```

### 按关键词过滤
```bash
# 过滤包含"提示词"的日志
adb logcat | grep "提示词"

# 过滤包含"AI响应"的日志
adb logcat | grep "AI响应"

# 过滤包含"解析结果"的日志
adb logcat | grep "解析结果"

# 过滤包含"更新"的日志
adb logcat | grep "更新"

# 过滤错误相关日志
adb logcat | grep -E "(错误|异常|失败)"
```

### 组合过滤
```bash
# 抓取说明书更新相关日志并过滤重要信息
adb logcat ManualUpdater:D AppManualAgent:D PromptBuilder:D "*:S" | grep -E "(===|START|END|错误|异常|失败|构造的提示词|AI响应|解析结果)"

# 只看局部更新相关的重要日志
adb logcat ManualUpdater:D AppManualAgent:D "*:S" | grep -E "(局部更新|页面更新|总览更新)"
```

## 实时日志查看

### 实时跟踪说明书更新日志
```bash
adb logcat ManualUpdater:D AppManualAgent:D PromptBuilder:D "*:S"
```

### 实时跟踪并高亮重要信息
```bash
adb logcat ManualUpdater:D AppManualAgent:D "*:S" | grep --color=always -E "(===|START|END|错误|异常|失败)"
```

## 历史日志查看

### 查看最近的日志
```bash
# 查看最近100行相关日志
adb logcat -d ManualUpdater:D AppManualAgent:D PromptBuilder:D "*:S" | tail -n 100

# 查看最近50行ManualUpdater日志
adb logcat -d ManualUpdater:D "*:S" | tail -n 50

# 查看最近30行错误日志
adb logcat -d | grep -E "(错误|异常|失败)" | tail -n 30
```

### 查看指定时间范围的日志
```bash
# 查看最近5分钟的日志
adb logcat -d -t "5 minutes ago" ManualUpdater:D AppManualAgent:D "*:S"

# 查看最近1小时的日志
adb logcat -d -t "1 hour ago" ManualUpdater:D AppManualAgent:D "*:S"
```

## 日志保存到文件

### 保存实时日志
```bash
# 保存说明书更新日志到文件
adb logcat ManualUpdater:D AppManualAgent:D PromptBuilder:D "*:S" > manual_update_logs.txt

# 保存日志并同时在终端显示
adb logcat ManualUpdater:D AppManualAgent:D "*:S" | tee manual_update_logs.txt

# 保存带时间戳的日志文件
adb logcat ManualUpdater:D AppManualAgent:D "*:S" > "manual_logs_$(date +%Y%m%d_%H%M%S).txt"
```

### 保存历史日志
```bash
# 保存现有缓冲区中的相关日志
adb logcat -d ManualUpdater:D AppManualAgent:D PromptBuilder:D "*:S" > existing_logs.txt

# 保存最近100行相关日志
adb logcat -d ManualUpdater:D AppManualAgent:D "*:S" | tail -n 100 > recent_logs.txt
```

## 特定场景的调试命令

### 调试提示词构造问题
```bash
# 查看提示词相关日志
adb logcat PromptBuilder:D ManualUpdater:D "*:S" | grep -E "(构造的提示词|System Prompt|User Prompt)"
```

### 调试AI响应解析问题
```bash
# 查看AI响应和解析相关日志
adb logcat ManualUpdater:D "*:S" | grep -E "(AI响应|解析结果|页面更新|总览更新)"
```

### 调试说明书结构解析问题
```bash
# 查看说明书解析相关日志
adb logcat AppManualAgent:D "*:S" | grep -E "(解析说明书|START_|页面数量|总览长度)"
```

### 调试更新保存问题
```bash
# 查看保存相关日志
adb logcat AppManualAgent:D "*:S" | grep -E "(保存|更新|版本号|缓存)"
```

## 日志级别说明

- `D` : Debug级别，显示调试信息
- `I` : Info级别，显示一般信息
- `W` : Warning级别，显示警告信息
- `E` : Error级别，显示错误信息
- `V` : Verbose级别，显示详细信息
- `S` : Silent级别，不显示任何信息

## 常用快捷命令

```bash
# 快速查看说明书更新相关的所有日志
alias manual_logs="adb logcat ManualUpdater:D AppManualAgent:D PromptBuilder:D '*:S'"

# 快速查看最近的说明书更新日志
alias manual_recent="adb logcat -d ManualUpdater:D AppManualAgent:D '*:S' | tail -n 50"

# 快速查看错误日志
alias manual_errors="adb logcat ManualUpdater:D AppManualAgent:D '*:S' | grep -E '(错误|异常|失败)'"

# 快速清空日志
alias clear_logs="adb logcat -c"
```

## 使用建议

1. **调试前先清空缓冲区**: `adb logcat -c`
2. **使用标签过滤减少噪音**: 只看相关的日志标签
3. **保存重要日志**: 遇到问题时及时保存日志到文件
4. **组合使用grep**: 进一步过滤出关键信息
5. **实时监控**: 在执行操作时同时监控日志输出

## 故障排查流程

1. 清空日志: `adb logcat -c`
2. 开始监控: `adb logcat ManualUpdater:D AppManualAgent:D "*:S"`
3. 执行测试操作
4. 分析日志输出
5. 保存关键日志: `adb logcat -d ManualUpdater:D AppManualAgent:D "*:S" > debug_logs.txt`
