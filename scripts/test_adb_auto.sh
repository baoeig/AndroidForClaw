#!/bin/bash

# AndroidForClaw 自动化测试脚本 (通过 ADB 广播)
# 用法: ./test_adb_auto.sh

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
PACKAGE="com.xiaomo.androidforclaw.debug"
COMPONENT="$PACKAGE/com.xiaomo.androidforclaw.core.AgentMessageReceiver"
ACTION="com.xiaomo.androidforclaw.ACTION_EXECUTE_AGENT"
LOGCAT_TAG="AndroidForClaw"

echo "======================================"
echo "AndroidForClaw 自动化测试"
echo "======================================"
echo ""

# 检查 ADB 连接
echo "检查 ADB 连接..."
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ 未检测到 Android 设备${NC}"
    exit 1
fi
echo -e "${GREEN}✅ ADB 连接正常${NC}"
echo ""

# 检查应用是否安装
echo "检查应用..."
if ! adb shell pm list packages | grep -q "$PACKAGE"; then
    echo -e "${RED}❌ 应用未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 应用已安装${NC}"
echo ""

# 发送消息函数
send_message() {
    local message=$1
    local test_name=$2

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}测试: $test_name${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "发送消息: $message"
    echo ""

    # 清除 logcat
    adb logcat -c

    # 发送广播 (必须指定组件名才能绕过后台执行限制)
    echo "发送广播..."
    adb shell am broadcast \
        -n "$COMPONENT" \
        -a "$ACTION" \
        --es message "$message" \
        --es sessionId "test_$(date +%s)" \
        > /dev/null 2>&1

    echo -e "${GREEN}✅ 广播已发送${NC}"
    echo ""

    # 监听日志 (20秒，等待 Agent 执行完成)
    echo "监听日志 (20秒)..."
    echo "────────────────────────────────────"

    timeout 20 adb logcat -v time | grep -E "(AgentLoop|SkillRegistry|Tool.*execute|screenshot|tap|swipe|Error|Exception|达到最大)" | grep -v "vendor\|MiuiNearby\|Java_Reflection" || true

    echo "────────────────────────────────────"
    echo ""

    # 等待一下再继续
    sleep 2
}

# ============= 基础工具测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第一部分: 基础工具测试            │"
echo "└────────────────────────────────────┘"
echo ""

send_message "帮我截个图" "1. 截图测试"

send_message "点击屏幕中心位置 (540, 1000)" "2. 点击测试"

send_message "返回主屏幕" "3. Home键测试"

send_message "等待 2 秒" "4. 等待测试"

# ============= 文件系统测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第二部分: 文件系统测试            │"
echo "└────────────────────────────────────┘"
echo ""

send_message "列出 /sdcard/Download 目录的文件" "5. 列出目录测试"

send_message "写入 'Test from ADB' 到 /sdcard/Download/adb_test.txt" "6. 写入文件测试"

send_message "读取 /sdcard/Download/adb_test.txt 文件内容" "7. 读取文件测试"

# ============= Shell 测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第三部分: Shell 工具测试          │"
echo "└────────────────────────────────────┘"
echo ""

send_message "执行命令 ls /sdcard" "8. Shell命令测试"

send_message "执行命令 getprop ro.build.version.release" "9. 获取系统版本"

# ============= 应用操作测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第四部分: 应用操作测试            │"
echo "└────────────────────────────────────┘"
echo ""

send_message "列出所有用户安装的应用" "10. 列出应用测试"

send_message "打开设置应用" "11. 打开应用测试"

send_message "等待 2 秒后按返回键" "12. 返回键测试"

# ============= 综合测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第五部分: 综合流程测试            │"
echo "└────────────────────────────────────┘"
echo ""

send_message "先截图，然后点击屏幕中间，再截图验证" "13. 综合流程测试"

# ============= 边界条件测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第六部分: 边界条件测试            │"
echo "└────────────────────────────────────┘"
echo ""

send_message "点击超出屏幕的坐标 (10000, 10000)" "14. 坐标越界测试"

send_message "读取不存在的文件 /sdcard/nonexist_12345.txt" "15. 文件不存在测试"

# ============= 错误处理测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第七部分: 错误处理测试            │"
echo "└────────────────────────────────────┘"
echo ""

send_message "执行危险命令 rm -rf /sdcard/test" "16. 危险命令测试 (应被拒绝)"

send_message "打开不存在的应用 com.nonexist.app.12345" "17. 无效应用测试"

# ============= 完成 =============

echo ""
echo "======================================"
echo "自动化测试完成！"
echo "======================================"
echo ""
echo "测试完成，请检查以上日志输出:"
echo ""
echo "✅ 检查项："
echo "  1. AgentMessageReceiver 是否收到消息"
echo "  2. AgentLoop 是否正常启动"
echo "  3. 工具是否正确执行"
echo "  4. 是否有错误或异常"
echo "  5. JSON 序列化是否正常"
echo "  6. 迭代次数是否正常"
echo ""
echo "⚠️  如果发现问题："
echo "  - 查看完整 logcat: adb logcat -v time"
echo "  - 过滤错误: adb logcat -v time | grep -E 'ERROR|Exception'"
echo "  - 查看 JSON 错误: adb logcat -v time | grep 'Unterminated'"
echo ""
echo "======================================"
