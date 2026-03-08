package com.xiaomo.androidforclaw.util

/**
 * MMKV 配置键
 * 新架构聚焦: AgentLoop + Tools
 */
enum class MMKVKeys(val key: String) {
    BUG_SWITCH("bug_switch"),

    // ========== 保留功能 ==========
    // 悬浮窗显示开关 (EasyFloat)
    FLOAT_WINDOW_ENABLED("float_window_enabled"),

    // 探索模式开关（false: 规划模式, true: 探索模式）
    EXPLORATION_MODE("exploration_mode")
}
