#!/bin/bash

# AndroidForClaw 工具加载测试脚本
# 用于快速检查所有工具是否正确加载到 ToolRegistry

set -e

echo "======================================"
echo "AndroidForClaw 工具加载检查"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 检查 adb 连接
echo "检查 ADB 连接..."
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ 未检测到 Android 设备${NC}"
    echo "请确保:"
    echo "  1. 设备已通过 USB 连接"
    echo "  2. 已启用开发者选项和 USB 调试"
    echo "  3. 已授权此电脑的 ADB 调试"
    exit 1
fi
echo -e "${GREEN}✅ ADB 连接正常${NC}"
echo ""

# 检查应用是否安装
echo "检查 AndroidForClaw 应用..."
if ! adb shell pm list packages | grep -q "com.xiaomo.androidforclaw"; then
    echo -e "${RED}❌ AndroidForClaw 未安装${NC}"
    echo "请先安装应用: ./gradlew installDebug"
    exit 1
fi
echo -e "${GREEN}✅ 应用已安装${NC}"
echo ""

# 启动应用并获取工具列表
echo "启动应用并获取工具列表..."
echo "请在应用中输入: \"列出所有可用工具\""
echo ""
echo "然后检查以下工具是否都已加载:"
echo ""

# 核心 Android 工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "核心 Android 工具 (必须加载):"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
  1. screenshot          - 截图
  2. tap                 - 点击
  3. swipe               - 滑动
  4. type                - 输入文本
  5. long_press          - 长按
  6. home                - 返回主屏幕
  7. back                - 返回键
  8. open_app            - 打开应用
  9. start_activity      - 启动 Activity
 10. list_installed_apps - 列出应用
 11. get_view_tree       - 获取 UI 树
 12. wait                - 等待
 13. stop                - 停止执行
EOF
echo ""

# 文件系统工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "文件系统工具:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
 14. read_file          - 读取文件
 15. write_file         - 写入文件
 16. edit_file          - 编辑文件
 17. list_dir           - 列出目录
EOF
echo ""

# Shell 工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Shell 工具:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
 18. exec               - 执行命令
EOF
echo ""

# JavaScript 工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "JavaScript 工具:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
 19. javascript         - 执行 JavaScript (QuickJS)
 20. javascript_exec    - JavaScript 执行器
EOF
echo ""

# 网络工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "网络工具:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
 21. web_fetch          - 获取网页内容
EOF
echo ""

# 记忆工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "记忆工具:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
 22. memory_search      - 搜索记忆
 23. memory_get         - 获取记忆
EOF
echo ""

# 日志工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "日志工具:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
 24. log                - 记录日志
EOF
echo ""

# 浏览器工具
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "浏览器工具 (需要 BClaw):"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cat <<EOF
 25. browser            - 统一浏览器入口
 26. browser_navigate   - 浏览器导航
 27. browser_click      - 浏览器点击
 28. browser_type       - 浏览器输入
 29. browser_get_content- 获取页面内容
 30. browser_wait       - 浏览器等待
 + 更多浏览器工具 (scroll, execute, press, screenshot, cookies, hover, select)
EOF
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "总计: 约 30+ 个工具"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 检查 logcat 输出
echo "检查 logcat 输出..."
echo "使用以下命令监控工具注册过程:"
echo ""
echo "  adb logcat -v time | grep -E \"(SkillRegistry|ToolRegistry|Skill.*registered)\""
echo ""

# 提供检查脚本
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "快速检查脚本:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "1. 清除 logcat 并监控工具注册:"
echo "   adb logcat -c && adb logcat -v time | grep -E \"SkillRegistry|registered\""
echo ""
echo "2. 重启应用触发工具加载:"
echo "   adb shell am force-stop com.xiaomo.androidforclaw"
echo "   adb shell am start -n com.xiaomo.androidforclaw/.ui.activity.MainActivity"
echo ""
echo "3. 检查是否有错误:"
echo "   adb logcat -v time | grep -E \"ERROR|Exception\" | grep -i skill"
echo ""

echo "======================================"
echo "工具加载检查完成"
echo "======================================"
