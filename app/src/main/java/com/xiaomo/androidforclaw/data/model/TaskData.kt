package com.xiaomo.androidforclaw.data.model

/**
 * 任务数据类 - 简化版
 *
 * 仅保留运行状态和包名信息。
 * 对话历史使用 Session 管理（agent/session/SessionManager.kt）
 */
class TaskData(
    val taskId: String,
    var packageName: String
) {
    // ===== 运行状态 =====
    private var _isRunning: Boolean = false
    private var _conversationId: String = ""

    // ===== 状态管理 =====
    fun setIsRunning(isRunning: Boolean) {
        _isRunning = isRunning
    }

    fun stopRunning(reason: String) {
        _isRunning = false
    }

    fun getIsRunning(): Boolean = _isRunning

    fun updateConversationId(conversationId: String) {
        _conversationId = conversationId
    }

    fun getConversationId(): String = _conversationId
}
