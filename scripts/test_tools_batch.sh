#!/bin/bash

# AndroidForClaw 批量工具测试
# 自动测试所有核心工具

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

COMPONENT="com.xiaomo.androidforclaw.debug/com.xiaomo.androidforclaw.core.AgentMessageReceiver"
ACTION="com.xiaomo.androidforclaw.ACTION_EXECUTE_AGENT"

echo "======================================"
echo "AndroidForClaw 批量工具测试"
echo "======================================"
echo ""

# 测试计数
TOTAL=0
PASSED=0
FAILED=0

# 测试函数
test_tool() {
    local tool_name=$1
    local message=$2
    local expected_log=$3
    local timeout=${4:-10}

    TOTAL=$((TOTAL + 1))
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "[$TOTAL] 测试: $tool_name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "消息: $message"

    adb logcat -c > /dev/null 2>&1
    adb shell "am broadcast -n $COMPONENT -a $ACTION --es message '$message' --es sessionId test_$TOTAL" > /dev/null 2>&1

    sleep "$timeout"

    local result=$(adb logcat -d | grep -E "$expected_log" | head -5)

    if [ -n "$result" ]; then
        echo -e "${GREEN}✅ 通过${NC}"
        echo "日志: $result"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}❌ 失败${NC}"
        echo "未找到预期日志: $expected_log"
        FAILED=$((FAILED + 1))
    fi
}

# ============= 核心工具测试 =============

echo "开始测试核心Android工具..."

test_tool "tap" "点击屏幕 540 1000" "Function: tap|TapSkill" 8

test_tool "wait" "等待2秒" "Function: wait|WaitSkill" 8

test_tool "home" "返回主屏幕" "Function: home|HomeSkill" 6

test_tool "back" "按返回键" "Function: back|BackSkill" 6

# ============= 文件系统工具测试 =============

echo ""
echo "开始测试文件系统工具..."

test_tool "list_dir" "列出 /sdcard/Download 目录" "Function: list_dir|ListDirTool" 8

test_tool "write_file" "写入 Test 到 /sdcard/test.txt" "Function: write_file|WriteFileTool" 8

test_tool "read_file" "读取 /sdcard/test.txt" "Function: read_file|ReadFileTool" 8

# ============= Shell工具测试 =============

echo ""
echo "开始测试Shell工具..."

test_tool "exec" "执行命令 ls /sdcard" "Function: exec|ExecTool" 8

# ============= 应用工具测试 =============

echo ""
echo "开始测试应用工具..."

test_tool "list_apps" "列出所有用户应用" "Function: list_installed_apps|ListInstalledAppsSkill" 10

test_tool "open_app" "打开设置" "Function: open_app|OpenAppSkill" 8

# ============= 结果汇总 =============

echo ""
echo "======================================"
echo "测试完成！"
echo "======================================"
echo ""
echo "测试统计:"
echo "  总计: $TOTAL"
echo -e "  ${GREEN}通过: $PASSED${NC}"
echo -e "  ${RED}失败: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}⚠️  有 $FAILED 个测试失败${NC}"
    echo "请检查失败的工具实现"
    exit 1
fi
