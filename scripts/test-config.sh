#!/bin/bash

# 快速配置测试脚本
# 运行配置测试并查看日志

echo "========================================="
echo "配置系统快速测试"
echo "========================================="
echo ""

# 检查 ADB
if ! adb devices | grep -q "device$"; then
    echo "❌ 没有检测到 ADB 设备"
    exit 1
fi

echo "✅ ADB 已连接"
echo ""

# 启动测试 Activity
echo "启动配置测试 Activity..."
adb shell am start -n com.xiaomo.androidforclaw/.config.ConfigTestActivity

sleep 2

echo ""
echo "========================================="
echo "实时日志 (Ctrl+C 退出)"
echo "========================================="
echo ""

# 查看日志
adb logcat -c  # 清除旧日志
adb logcat -s ConfigLoader:* ConfigLoaderTest:* ConfigTestActivity:* | grep -E "(✅|❌|⚠️|测试|配置)"
