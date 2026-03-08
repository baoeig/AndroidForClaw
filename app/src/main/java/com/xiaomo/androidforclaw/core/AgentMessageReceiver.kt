package com.xiaomo.androidforclaw.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Agent 消息广播接收器
 * 用于接收来自 Gateway 或 ADB 的 Agent 执行请求
 */
class AgentMessageReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AgentMessageReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 使用 System.out 确保能看到日志
        System.out.println("========== AgentMessageReceiver.onReceive 被调用 ==========")
        Log.e(TAG, "========== onReceive 被调用 ==========")
        Log.e(TAG, "Action: ${intent.action}")
        Log.e(TAG, "Extras: ${intent.extras}")

        if (intent.action != "com.xiaomo.androidforclaw.ACTION_EXECUTE_AGENT") {
            Log.e(TAG, "⚠️ [Receiver] 未知的 action: ${intent.action}")
            return
        }

        val message = intent.getStringExtra("message")
        val sessionId = intent.getStringExtra("sessionId")

        Log.e(TAG, "📨 [Receiver] 收到 Agent 执行请求:")
        Log.e(TAG, "  💬 Message: $message")
        Log.e(TAG, "  🆔 Session ID: $sessionId")
        System.out.println("📨 Message: $message, SessionID: $sessionId")

        if (message.isNullOrEmpty()) {
            Log.e(TAG, "⚠️ [Receiver] 消息为空，忽略")
            return
        }

        // 确保 MainEntryNew 已初始化
        try {
            Log.e(TAG, "🔧 [Receiver] 确保 MainEntryNew 已初始化...")
            MainEntryNew.initialize(context.applicationContext as android.app.Application)
        } catch (e: Exception) {
            // Already initialized, ignore
            Log.e(TAG, "✓ [Receiver] MainEntryNew 已初始化")
        }

        // 执行 Agent
        Log.e(TAG, "🚀 [Receiver] 启动 Agent 执行...")
        MainEntryNew.runWithSession(
            userInput = message,
            sessionId = sessionId,
            application = context.applicationContext as android.app.Application
        )
        Log.e(TAG, "✅ [Receiver] Agent 执行已启动")
    }
}
