#!/bin/bash

# AndroidForClaw 工具完整测试脚本
# 用法: ./test_skills_complete.sh

set -e

echo "======================================"
echo "AndroidForClaw 工具测试脚本"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 测试计数
TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0

# 测试结果记录
declare -a TEST_RESULTS

# 测试函数
run_test() {
    local test_name=$1
    local test_prompt=$2
    local expected_tool=$3

    TOTAL=$((TOTAL + 1))
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "测试 #$TOTAL: $test_name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "提示词: $test_prompt"
    echo "预期工具: $expected_tool"
    echo ""

    # 这里需要人工测试，自动化暂不实现
    echo "${YELLOW}⏸️  请在 AndroidForClaw 中输入测试提示词${NC}"
    echo ""
    echo "测试完成后，请输入结果:"
    echo "  1) ✅ 通过"
    echo "  2) ⚠️  部分通过"
    echo "  3) ❌ 失败"
    echo "  4) ⏸️  跳过"
    read -p "选择 (1-4): " result

    case $result in
        1)
            echo -e "${GREEN}✅ 通过${NC}"
            PASSED=$((PASSED + 1))
            TEST_RESULTS+=("✅ $test_name")
            ;;
        2)
            echo -e "${YELLOW}⚠️  部分通过${NC}"
            read -p "问题描述: " issue
            TEST_RESULTS+=("⚠️  $test_name - $issue")
            ;;
        3)
            echo -e "${RED}❌ 失败${NC}"
            FAILED=$((FAILED + 1))
            read -p "错误描述: " error
            TEST_RESULTS+=("❌ $test_name - $error")
            ;;
        4)
            echo -e "${YELLOW}⏸️  跳过${NC}"
            SKIPPED=$((SKIPPED + 1))
            TEST_RESULTS+=("⏸️  $test_name")
            ;;
        *)
            echo -e "${YELLOW}⏸️  跳过 (无效输入)${NC}"
            SKIPPED=$((SKIPPED + 1))
            TEST_RESULTS+=("⏸️  $test_name")
            ;;
    esac
}

# ============= 核心 Android 工具测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第一部分: 核心 Android 工具       │"
echo "└────────────────────────────────────┘"

run_test \
    "截图" \
    "帮我截个图看看当前屏幕" \
    "screenshot"

run_test \
    "点击" \
    "点击屏幕中心位置 (540, 1000)" \
    "tap"

run_test \
    "向上滑动" \
    "从屏幕底部 (540, 1500) 向上滑动到中间 (540, 800)" \
    "swipe"

run_test \
    "返回主屏幕" \
    "返回到主屏幕" \
    "home"

run_test \
    "返回键" \
    "按返回键" \
    "back"

run_test \
    "打开应用" \
    "打开设置应用" \
    "open_app"

run_test \
    "列出应用" \
    "列出所有用户安装的应用" \
    "list_installed_apps"

run_test \
    "获取UI树" \
    "获取当前屏幕的 UI 树结构" \
    "get_view_tree"

run_test \
    "等待" \
    "等待 2 秒" \
    "wait"

# ============= 文件系统工具测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第二部分: 文件系统工具            │"
echo "└────────────────────────────────────┘"

run_test \
    "列出目录" \
    "列出 /sdcard/Download 目录下的文件" \
    "list_dir"

run_test \
    "写入文件" \
    "写入 'Hello from AndroidForClaw!' 到 /sdcard/Download/test.txt" \
    "write_file"

run_test \
    "读取文件" \
    "读取 /sdcard/Download/test.txt 文件内容" \
    "read_file"

run_test \
    "编辑文件" \
    "在 /sdcard/Download/test.txt 中将 'Hello' 替换为 'Hi'" \
    "edit_file"

# ============= Shell 工具测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第三部分: Shell 工具              │"
echo "└────────────────────────────────────┘"

run_test \
    "执行命令-ls" \
    "执行命令: ls /sdcard" \
    "exec"

run_test \
    "执行命令-getprop" \
    "执行命令 getprop ro.build.version.release 获取 Android 版本" \
    "exec"

# ============= 浏览器工具测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第四部分: 浏览器工具 (需要BClaw)  │"
echo "└────────────────────────────────────┘"

echo "${YELLOW}注意: 请先启动 BClaw 浏览器${NC}"
read -p "BClaw 已启动? (y/n): " bclaw_ready

if [ "$bclaw_ready" = "y" ]; then
    run_test \
        "浏览器导航" \
        "使用浏览器打开 https://www.google.com" \
        "browser_navigate"

    run_test \
        "获取页面内容" \
        "获取浏览器当前页面的文本内容" \
        "browser_get_content"

    run_test \
        "浏览器等待" \
        "在浏览器中等待 2 秒" \
        "browser_wait"
else
    echo "${YELLOW}⏸️  跳过浏览器测试${NC}"
    SKIPPED=$((SKIPPED + 3))
fi

# ============= 综合流程测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第五部分: 综合流程测试            │"
echo "└────────────────────────────────────┘"

run_test \
    "截图-操作-验证" \
    "1. 先截图看当前屏幕 2. 点击屏幕中间 3. 再截图确认" \
    "screenshot, tap"

run_test \
    "打开应用-操作-返回" \
    "1. 打开设置 2. 等待2秒 3. 获取UI树 4. 返回主屏幕" \
    "open_app, wait, get_view_tree, home"

# ============= 边界条件测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第六部分: 边界条件测试            │"
echo "└────────────────────────────────────┘"

run_test \
    "坐标越界" \
    "点击超出屏幕的坐标 (10000, 10000)" \
    "tap"

run_test \
    "读取不存在的文件" \
    "读取不存在的文件 /sdcard/nonexist_file_12345.txt" \
    "read_file"

# ============= 错误处理测试 =============

echo ""
echo "┌────────────────────────────────────┐"
echo "│  第七部分: 错误处理测试            │"
echo "└────────────────────────────────────┘"

run_test \
    "危险命令拒绝" \
    "执行危险命令: rm -rf /sdcard/*" \
    "exec (应被拒绝)"

run_test \
    "无效包名" \
    "打开不存在的应用 com.nonexist.app.12345" \
    "open_app (应报错)"

# ============= 测试报告 =============

echo ""
echo "======================================"
echo "测试完成！"
echo "======================================"
echo ""
echo "测试统计:"
echo "  总计: $TOTAL"
echo -e "  ${GREEN}通过: $PASSED${NC}"
echo -e "  ${RED}失败: $FAILED${NC}"
echo -e "  ${YELLOW}跳过: $SKIPPED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ 所有测试通过！${NC}"
else
    echo -e "${RED}⚠️  有 $FAILED 个测试失败${NC}"
fi

echo ""
echo "详细测试结果:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
for result in "${TEST_RESULTS[@]}"; do
    echo "$result"
done
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 生成测试报告
REPORT_FILE="test_report_$(date +%Y%m%d_%H%M%S).txt"
echo "生成测试报告: $REPORT_FILE"
cat > "$REPORT_FILE" <<EOF
AndroidForClaw 工具测试报告
生成时间: $(date)

测试统计:
  总计: $TOTAL
  通过: $PASSED
  失败: $FAILED
  跳过: $SKIPPED

详细结果:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$(printf '%s\n' "${TEST_RESULTS[@]}")
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

EOF

echo ""
echo "报告已保存到: $REPORT_FILE"
echo ""
echo "======================================"
