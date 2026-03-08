package com.xiaomo.androidforclaw.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * Chat Broadcast Receiver - ADB 测试接口
 *
 * 用途：方便通过 ADB 直接发送消息到聊天界面进行测试
 *
 * 使用方法:
 * adb shell am broadcast -a PHONE_FORCLAW_SEND_MESSAGE --es message "你的消息内容"
 *
 * 示例:
 * adb shell am broadcast -a PHONE_FORCLAW_SEND_MESSAGE --es message "使用browser搜索openclaw"
 */
class ChatBroadcastReceiver(
    private val onMessageReceived: (String) -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "ChatBroadcastReceiver"
        const val ACTION_SEND_MESSAGE = "PHONE_FORCLAW_SEND_MESSAGE"
        const val EXTRA_MESSAGE = "message"

        /**
         * 创建 IntentFilter
         */
        fun createIntentFilter(): IntentFilter {
            return IntentFilter(ACTION_SEND_MESSAGE)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "📨 onReceive 被调用 - action: ${intent?.action}")
        if (intent?.action == ACTION_SEND_MESSAGE) {
            val message = intent.getStringExtra(EXTRA_MESSAGE)
            Log.d(TAG, "📨 消息内容: $message")
            if (message != null && message.isNotBlank()) {
                Log.d(TAG, "✅ 收到 ADB 消息: $message")
                onMessageReceived(message)
            } else {
                Log.w(TAG, "⚠️ 收到空消息")
            }
        } else {
            Log.w(TAG, "⚠️ 未知 action: ${intent?.action}")
        }
    }
}
