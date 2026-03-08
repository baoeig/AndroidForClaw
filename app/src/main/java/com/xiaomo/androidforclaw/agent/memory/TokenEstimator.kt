package com.xiaomo.androidforclaw.agent.memory

import com.xiaomo.androidforclaw.providers.LegacyMessage

/**
 * Token 估算工具
 * 对齐 OpenClaw src/agents/compaction.ts
 *
 * 使用 chars/4 启发法 + 多字节字符修正
 */
object TokenEstimator {
    private const val TAG = "TokenEstimator"

    // 基础估算：4 个字符约等于 1 个 token
    private const val CHARS_PER_TOKEN = 4.0

    // 多字节字符的额外权重（中文、日文等）
    private const val MULTIBYTE_WEIGHT = 1.2

    /**
     * 估算文本的 token 数量
     *
     * @param text 要估算的文本
     * @return 估算的 token 数量
     */
    fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0

        val charCount = text.length
        var multibyteCount = 0

        // 统计多字节字符（非 ASCII）
        for (char in text) {
            if (char.code > 127) {
                multibyteCount++
            }
        }

        // 计算调整后的字符数
        val adjustedChars = charCount + (multibyteCount * (MULTIBYTE_WEIGHT - 1.0))

        // 转换为 token 数量
        return (adjustedChars / CHARS_PER_TOKEN).toInt()
    }

    /**
     * 估算消息的 token 数量
     *
     * @param message 要估算的消息
     * @return 估算的 token 数量
     */
    fun estimateMessageTokens(message: LegacyMessage): Int {
        var total = 0

        // 估算 role 字段 (约 5 tokens)
        total += 5

        // 估算 content
        when (val content = message.content) {
            is String -> {
                total += estimateTokens(content)
            }
            is List<*> -> {
                for (block in content) {
                    when (block) {
                        is Map<*, *> -> {
                            val type = block["type"] as? String
                            val text = block["text"] as? String
                            val imageUrl = block["image_url"] as? Map<*, *>

                            when (type) {
                                "text" -> {
                                    if (text != null) {
                                        total += estimateTokens(text)
                                    }
                                }
                                "image_url" -> {
                                    // 图片约 85-170 tokens，取中间值
                                    total += 127
                                }
                            }
                        }
                    }
                }
            }
        }

        // 估算 tool_calls
        if (message.toolCalls != null) {
            for (toolCall in message.toolCalls) {
                // tool call 结构开销
                total += 10

                // function name
                total += estimateTokens(toolCall.function.name)

                // arguments
                total += estimateTokens(toolCall.function.arguments)
            }
        }

        // 估算 tool_call_id
        if (message.toolCallId != null) {
            total += estimateTokens(message.toolCallId)
        }

        return total
    }

    /**
     * 估算消息列表的总 token 数量
     *
     * @param messages 消息列表
     * @return 估算的总 token 数量
     */
    fun estimateMessagesTokens(messages: List<LegacyMessage>): Int {
        return messages.sumOf { estimateMessageTokens(it) }
    }

    /**
     * 检查消息列表是否超过 token 限制
     *
     * @param messages 消息列表
     * @param maxTokens 最大 token 数量
     * @return 是否超过限制
     */
    fun exceedsTokenLimit(messages: List<LegacyMessage>, maxTokens: Int): Boolean {
        return estimateMessagesTokens(messages) > maxTokens
    }

    /**
     * 计算到达限制还可以添加多少 tokens
     *
     * @param messages 当前消息列表
     * @param maxTokens 最大 token 数量
     * @return 剩余可用 tokens
     */
    fun remainingTokens(messages: List<LegacyMessage>, maxTokens: Int): Int {
        val current = estimateMessagesTokens(messages)
        return maxTokens - current
    }
}
