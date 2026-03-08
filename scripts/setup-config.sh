#!/bin/bash

# AndroidForClaw 配置文件设置脚本
# 用于将配置文件推送到 Android 设备并运行测试

set -e

echo "========================================="
echo "AndroidForClaw 配置文件设置"
echo "========================================="
echo ""

# 检查 ADB 连接
echo "检查 ADB 连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 错误: 没有检测到 ADB 设备"
    echo "请确保:"
    echo "  1. USB 调试已开启"
    echo "  2. 设备已连接"
    echo "  3. ADB 驱动已安装"
    exit 1
fi

echo "✅ ADB 设备已连接"
echo ""

# 创建配置目录
echo "创建配置目录..."
adb shell mkdir -p /sdcard/AndroidForClaw/config

# 推送配置文件
echo "推送 models.json..."
adb push config-templates/models.json /sdcard/AndroidForClaw/config/models.json

echo "推送 openclaw.json..."
adb push config-templates/openclaw.json /sdcard/AndroidForClaw/config/openclaw.json

echo "✅ 配置文件已推送"
echo ""

# 验证文件
echo "验证配置文件..."
adb shell ls -lh /sdcard/AndroidForClaw/config/

echo ""
echo "========================================="
echo "配置文件设置完成"
echo "========================================="
echo ""

# 询问是否运行测试
read -p "是否立即运行配置测试? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "启动配置测试 Activity..."
    adb shell am start -n com.xiaomo.androidforclaw/.config.ConfigTestActivity

    echo ""
    echo "查看日志 (Ctrl+C 退出):"
    echo ""
    adb logcat -s ConfigLoader ConfigLoaderTest ConfigTestActivity
fi
