package com.xiaomo.androidforclaw.agent.context

import android.util.Log
import com.xiaomo.androidforclaw.providers.UnifiedLLMProvider
import com.xiaomo.androidforclaw.providers.LegacyMessage
import com.xiaomo.androidforclaw.providers.llm.Message

/**
 * 上下文管理器 - 处理上下文超限
 * 对齐 OpenClaw 的多层恢复策略
 *
 * 策略顺序（参考 OpenClaw run.ts 行 687-1055）：
 * 1. 检测上下文超限错误
 * 2. 尝试 Compaction（最多 3 次）
 * 3. 尝试截断超大工具结果
 * 4. 放弃并返回错误
 */
class ContextManager(
    private val llmProvider: UnifiedLLMProvider
) {
    companion object {
        private const val TAG = "ContextManager"
        private const val MAX_COMPACTION_ATTEMPTS = 3
        private const val DEFAULT_CONTEXT_WINDOW = 200000
    }

    private val compactor = MessageCompactor(llmProvider)
    private var compactionAttempts = 0
    private var toolResultTruncationAttempted = false

    /**
     * 重置计数器（新的 run 开始时调用）
     */
    fun reset() {
        compactionAttempts = 0
        toolResultTruncationAttempted = false
        Log.d(TAG, "Context manager reset")
    }

    /**
     * 处理上下文超限错误
     * @return 如果可以恢复返回修复后的消息列表，否则返回 null
     */
    suspend fun handleContextOverflow(
        error: Throwable,
        messages: List<Message>,
        contextWindow: Int = DEFAULT_CONTEXT_WINDOW
    ): ContextRecoveryResult {
        val errorMessage = ContextErrors.extractErrorMessage(error)

        Log.e(TAG, "检测到上下文超限: $errorMessage")
        Log.d(TAG, "当前消息数: ${messages.size}")
        Log.d(TAG, "已尝试 compaction: $compactionAttempts 次")
        Log.d(TAG, "已尝试截断: $toolResultTruncationAttempted")

        // 1. 确认是上下文超限错误
        if (!ContextErrors.isLikelyContextOverflowError(errorMessage)) {
            Log.w(TAG, "不是上下文超限错误，无法处理")
            return ContextRecoveryResult.CannotRecover(errorMessage)
        }

        // 2. 尝试 Compaction
        if (compactionAttempts < MAX_COMPACTION_ATTEMPTS) {
            Log.d(TAG, "尝试 Compaction (第 ${compactionAttempts + 1} 次)...")

            // 转换 Message 到 LegacyMessage
            val legacyMessages = convertToLegacyMessages(messages)

            val compactionResult = compactor.compactMessages(
                messages = legacyMessages,
                keepLastN = 3
            )

            if (compactionResult.isSuccess) {
                compactionAttempts++
                val compacted = compactionResult.getOrNull()!!

                Log.d(TAG, "Compaction 成功: ${legacyMessages.size} -> ${compacted.size} 条消息")

                // 转换回 Message
                val newMessages = convertFromLegacyMessages(compacted)

                return ContextRecoveryResult.Recovered(
                    messages = newMessages,
                    strategy = "compaction",
                    attempt = compactionAttempts
                )
            } else {
                Log.e(TAG, "Compaction 失败: ${compactionResult.exceptionOrNull()?.message}")

                // Compaction 失败，检查是否是因为 compaction 本身导致的超限
                if (ContextErrors.isCompactionFailureError(errorMessage)) {
                    Log.e(TAG, "Compaction 本身失败，无法恢复")
                    return ContextRecoveryResult.CannotRecover(
                        "Compaction failed: context overflow during summarization. " +
                        "Try /reset or use a larger-context model."
                    )
                }
            }
        }

        // 3. 尝试截断超大工具结果
        if (!toolResultTruncationAttempted) {
            Log.d(TAG, "尝试截断超大工具结果...")

            val legacyMessages = convertToLegacyMessages(messages)

            if (ToolResultTruncator.hasOversizedToolResults(legacyMessages)) {
                toolResultTruncationAttempted = true

                val truncated = ToolResultTruncator.truncateToolResults(legacyMessages)
                val newMessages = convertFromLegacyMessages(truncated)

                Log.d(TAG, "工具结果截断完成")

                return ContextRecoveryResult.Recovered(
                    messages = newMessages,
                    strategy = "truncation",
                    attempt = 1
                )
            } else {
                Log.d(TAG, "没有检测到超大工具结果")
            }
        }

        // 4. 放弃
        Log.e(TAG, "所有恢复策略均失败")
        return ContextRecoveryResult.CannotRecover(
            "Context overflow: prompt too large for the model. " +
            "Compaction attempts: $compactionAttempts. " +
            "Try clearing session history or use a larger-context model."
        )
    }

    /**
     * 检查是否应该预防性压缩
     */
    fun shouldPreemptivelyCompact(
        messages: List<Message>,
        contextWindow: Int = DEFAULT_CONTEXT_WINDOW
    ): Boolean {
        val legacyMessages = convertToLegacyMessages(messages)
        return compactor.shouldCompact(legacyMessages, contextWindow)
    }

    /**
     * 预防性压缩（在发送请求前）
     */
    suspend fun preemptivelyCompact(
        messages: List<Message>
    ): List<Message> {
        Log.d(TAG, "预防性压缩...")

        val legacyMessages = convertToLegacyMessages(messages)
        val result = compactor.compactMessages(legacyMessages, keepLastN = 5)

        return if (result.isSuccess) {
            Log.d(TAG, "预防性压缩成功")
            convertFromLegacyMessages(result.getOrNull()!!)
        } else {
            Log.w(TAG, "预防性压缩失败，使用原消息")
            messages
        }
    }

    /**
     * 转换新格式到旧格式（用于 compactor）
     */
    private fun convertToLegacyMessages(messages: List<Message>): List<LegacyMessage> {
        return messages.map { msg ->
            when (msg.role) {
                "system", "user" -> LegacyMessage(msg.role, msg.content)
                "assistant" -> {
                    if (msg.toolCalls != null) {
                        LegacyMessage(
                            role = "assistant",
                            content = msg.content,
                            toolCalls = msg.toolCalls.map { tc ->
                                com.xiaomo.androidforclaw.providers.LegacyToolCall(
                                    id = tc.id,
                                    type = "function",
                                    function = com.xiaomo.androidforclaw.providers.LegacyFunction(
                                        name = tc.name,
                                        arguments = tc.arguments
                                    )
                                )
                            }
                        )
                    } else {
                        LegacyMessage("assistant", msg.content)
                    }
                }
                "tool" -> LegacyMessage(
                    role = "tool",
                    content = msg.content,
                    toolCallId = msg.toolCallId,
                    name = msg.name
                )
                else -> LegacyMessage(msg.role, msg.content)
            }
        }
    }

    /**
     * 转换旧格式到新格式
     */
    private fun convertFromLegacyMessages(legacyMessages: List<LegacyMessage>): List<Message> {
        return legacyMessages.map { msg ->
            when (msg.role) {
                "system" -> Message(
                    role = "system",
                    content = msg.content?.toString() ?: ""
                )
                "user" -> Message(
                    role = "user",
                    content = msg.content?.toString() ?: ""
                )
                "assistant" -> {
                    if (msg.toolCalls != null) {
                        Message(
                            role = "assistant",
                            content = msg.content?.toString() ?: "",
                            toolCalls = msg.toolCalls.map { tc ->
                                com.xiaomo.androidforclaw.providers.llm.ToolCall(
                                    id = tc.id,
                                    name = tc.function.name,
                                    arguments = tc.function.arguments
                                )
                            }
                        )
                    } else {
                        Message(
                            role = "assistant",
                            content = msg.content?.toString() ?: ""
                        )
                    }
                }
                "tool" -> Message(
                    role = "tool",
                    content = msg.content?.toString() ?: "",
                    toolCallId = msg.toolCallId,
                    name = msg.name
                )
                else -> Message(
                    role = msg.role,
                    content = msg.content?.toString() ?: ""
                )
            }
        }
    }
}

/**
 * 上下文恢复结果
 */
sealed class ContextRecoveryResult {
    /**
     * 成功恢复
     */
    data class Recovered(
        val messages: List<Message>,
        val strategy: String,  // "compaction" 或 "truncation"
        val attempt: Int
    ) : ContextRecoveryResult()

    /**
     * 无法恢复
     */
    data class CannotRecover(
        val reason: String
    ) : ContextRecoveryResult()
}
