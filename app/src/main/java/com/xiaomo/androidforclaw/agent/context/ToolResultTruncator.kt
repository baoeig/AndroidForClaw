package com.xiaomo.androidforclaw.agent.context

import android.util.Log
import com.xiaomo.androidforclaw.providers.LegacyMessage

/**
 * 工具结果截断器
 * 对齐 OpenClaw 的 tool-result-truncation.ts 实现
 */
object ToolResultTruncator {
    private const val TAG = "ToolResultTruncator"

    // 配置参数
    private const val MAX_TOOL_RESULT_CHARS = 4000  // 单个工具结果最大字符数
    private const val HEAD_CHARS = 1500  // 保留开头字符数
    private const val TAIL_CHARS = 1500  // 保留结尾字符数
    private const val PLACEHOLDER = "\n\n... [truncated] ...\n\n"

    /**
     * 截断消息列表中的超大工具结果
     */
    fun truncateToolResults(messages: List<LegacyMessage>): List<LegacyMessage> {
        var truncatedCount = 0

        val result = messages.map { msg ->
            if (msg.role == "tool" && msg.content != null) {
                val content = msg.content.toString()
                if (content.length > MAX_TOOL_RESULT_CHARS) {
                    truncatedCount++
                    val truncated = truncateContent(content)
                    msg.copy(content = truncated)
                } else {
                    msg
                }
            } else {
                msg
            }
        }

        if (truncatedCount > 0) {
            Log.d(TAG, "截断了 $truncatedCount 个超大工具结果")
        }

        return result
    }

    /**
     * 截断单个内容
     * 保留头部和尾部，中间用占位符替代
     */
    private fun truncateContent(content: String): String {
        if (content.length <= MAX_TOOL_RESULT_CHARS) {
            return content
        }

        val head = content.take(HEAD_CHARS)
        val tail = content.takeLast(TAIL_CHARS)

        val originalSize = content.length
        val truncatedSize = head.length + PLACEHOLDER.length + tail.length

        return buildString {
            append(head)
            append(PLACEHOLDER)
            append("[Original: $originalSize chars, Truncated to: $truncatedSize chars]")
            append(PLACEHOLDER)
            append(tail)
        }
    }

    /**
     * 检测会话中是否有超大工具结果
     */
    fun hasOversizedToolResults(messages: List<LegacyMessage>): Boolean {
        return messages.any { msg ->
            msg.role == "tool" &&
            msg.content != null &&
            msg.content.toString().length > MAX_TOOL_RESULT_CHARS
        }
    }

    /**
     * 统计工具结果的总大小
     */
    fun calculateToolResultSize(messages: List<LegacyMessage>): Int {
        return messages
            .filter { it.role == "tool" }
            .sumOf { it.content?.toString()?.length ?: 0 }
    }
}
