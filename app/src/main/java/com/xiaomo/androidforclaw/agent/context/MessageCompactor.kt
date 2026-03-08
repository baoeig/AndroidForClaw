package com.xiaomo.androidforclaw.agent.context

import android.util.Log
import com.xiaomo.androidforclaw.providers.LegacyMessage
import com.xiaomo.androidforclaw.providers.UnifiedLLMProvider
import com.xiaomo.androidforclaw.providers.llm.Message
import com.xiaomo.androidforclaw.providers.llm.systemMessage
import com.xiaomo.androidforclaw.providers.llm.userMessage

/**
 * 消息压缩器 - 总结旧消息以减少上下文
 * 对齐 OpenClaw 的 compaction.ts 实现
 */
class MessageCompactor(
    private val llmProvider: UnifiedLLMProvider
) {
    companion object {
        private const val TAG = "MessageCompactor"

        // 配置参数（对齐 OpenClaw 默认值）
        private const val KEEP_RECENT_TOKENS = 20000  // 保留最近的消息
        private const val MIN_MESSAGES_TO_COMPACT = 5  // 最少需要这么多消息才压缩
        private const val CHARS_PER_TOKEN = 4  // 粗略估算：4 字符 ≈ 1 token
    }

    /**
     * 压缩消息历史
     * @param messages 原始消息列表
     * @param keepLastN 保留最后 N 条消息不压缩
     * @return 压缩后的消息列表
     */
    suspend fun compactMessages(
        messages: List<LegacyMessage>,
        keepLastN: Int = 3
    ): Result<List<LegacyMessage>> {
        return try {
            Log.d(TAG, "开始压缩消息: 总数=${messages.size}, 保留最后${keepLastN}条")

            // 检查是否需要压缩
            if (messages.size < MIN_MESSAGES_TO_COMPACT) {
                Log.d(TAG, "消息数量太少，无需压缩")
                return Result.success(messages)
            }

            // 分离 system prompt、待压缩消息、最近消息
            val systemMessages = messages.filter { it.role == "system" }
            val nonSystemMessages = messages.filter { it.role != "system" }

            if (nonSystemMessages.size <= keepLastN) {
                Log.d(TAG, "非系统消息太少，无需压缩")
                return Result.success(messages)
            }

            val toCompact = nonSystemMessages.dropLast(keepLastN)
            val recentMessages = nonSystemMessages.takeLast(keepLastN)

            Log.d(TAG, "待压缩消息: ${toCompact.size}条")
            Log.d(TAG, "保留消息: ${recentMessages.size}条")

            // 总结旧消息
            val summary = summarizeMessages(toCompact)

            // 构建压缩后的消息列表
            val compacted = buildList {
                // 1. 保留 system prompt
                addAll(systemMessages)

                // 2. 添加总结消息
                add(LegacyMessage(
                    role = "user",
                    content = "[Earlier conversation summary]\n$summary"
                ))
                add(LegacyMessage(
                    role = "assistant",
                    content = "I understand. I'll continue from where we left off."
                ))

                // 3. 保留最近的消息
                addAll(recentMessages)
            }

            Log.d(TAG, "压缩完成: ${messages.size} -> ${compacted.size}条消息")

            Result.success(compacted)

        } catch (e: Exception) {
            Log.e(TAG, "压缩失败", e)
            Result.failure(e)
        }
    }

    /**
     * 总结消息列表
     */
    private suspend fun summarizeMessages(messages: List<LegacyMessage>): String {
        if (messages.isEmpty()) return ""

        // 构建待总结的文本
        val textToSummarize = buildString {
            messages.forEach { msg ->
                appendLine("Role: ${msg.role}")
                appendLine("Content: ${msg.content}")
                if (msg.toolCalls != null) {
                    appendLine("Tool calls: ${msg.toolCalls.size}")
                }
                appendLine()
            }
        }

        Log.d(TAG, "总结文本长度: ${textToSummarize.length} 字符")

        // 使用 LLM 总结
        val systemPrompt = """
            You are summarizing a conversation history to save context space.

            Requirements:
            1. Preserve all important facts, decisions, and outcomes
            2. Keep identifiers (UUIDs, IPs, file paths, package names, etc.)
            3. Note any errors or important observations
            4. Keep the summary concise but complete
            5. Use bullet points for clarity

            Format:
            - Task: [what was being done]
            - Key actions: [list of important actions]
            - Outcomes: [results or observations]
            - Important details: [any critical information to remember]
        """.trimIndent()

        val userPrompt = """
            Summarize this conversation history:

            $textToSummarize
        """.trimIndent()

        return try {
            val messagesToSend = listOf(
                systemMessage(systemPrompt),
                userMessage(userPrompt)
            )

            val response = llmProvider.chatWithTools(
                messages = messagesToSend,
                tools = null,  // 不需要工具
                reasoningEnabled = true  // 使用扩展思考
            )

            response.content ?: "Summary generation failed"

        } catch (e: Exception) {
            Log.e(TAG, "LLM 总结失败", e)
            // 降级：生成简单摘要
            generateSimpleSummary(messages)
        }
    }

    /**
     * 降级策略：生成简单摘要（不调用 LLM）
     */
    private fun generateSimpleSummary(messages: List<LegacyMessage>): String {
        return buildString {
            appendLine("Earlier conversation (${messages.size} messages):")

            // 统计消息类型
            val userMsgCount = messages.count { it.role == "user" }
            val assistantMsgCount = messages.count { it.role == "assistant" }
            val toolCallCount = messages.sumOf { it.toolCalls?.size ?: 0 }

            appendLine("- User messages: $userMsgCount")
            appendLine("- Assistant messages: $assistantMsgCount")
            appendLine("- Tool calls: $toolCallCount")

            // 提取工具名称
            val toolNames = messages
                .flatMap { it.toolCalls ?: emptyList() }
                .map { it.function.name }
                .distinct()

            if (toolNames.isNotEmpty()) {
                appendLine("- Tools used: ${toolNames.joinToString(", ")}")
            }
        }
    }

    /**
     * 估算消息列表的 token 数量
     */
    fun estimateTokens(messages: List<LegacyMessage>): Int {
        val totalChars = messages.sumOf { msg ->
            (msg.content?.toString()?.length ?: 0) +
            (msg.toolCalls?.sumOf { it.function.arguments.length } ?: 0)
        }
        return totalChars / CHARS_PER_TOKEN
    }

    /**
     * 检查是否需要压缩
     */
    fun shouldCompact(
        messages: List<LegacyMessage>,
        contextWindow: Int = 200000
    ): Boolean {
        val estimatedTokens = estimateTokens(messages)
        val threshold = contextWindow * 0.7  // 70% 阈值

        Log.d(TAG, "Token 估算: $estimatedTokens / $contextWindow (阈值: $threshold)")

        return estimatedTokens > threshold
    }
}
