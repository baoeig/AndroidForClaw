package com.xiaomo.androidforclaw.agent.memory

import android.util.Log
import com.xiaomo.androidforclaw.providers.LegacyMessage
import com.xiaomo.androidforclaw.providers.LegacyRepository

/**
 * 上下文压缩器
 * 对齐 OpenClaw src/agents/compaction.ts
 *
 * 功能：
 * - 检测是否需要压缩
 * - 生成历史消息摘要
 * - 标识符保留策略
 */
class ContextCompressor(
    private val legacyRepository: LegacyRepository,
    private val config: CompactionConfig = CompactionConfig()
) {
    companion object {
        private const val TAG = "ContextCompressor"

        // 安全边际：1.2x（补偿 token 估算不准确）
        private const val SAFETY_MARGIN = 1.2

        // 摘要提示开销
        private const val SUMMARIZATION_OVERHEAD_TOKENS = 4096
    }

    /**
     * 压缩配置
     */
    data class CompactionConfig(
        val mode: CompactionMode = CompactionMode.SAFEGUARD,
        val contextWindowTokens: Int = 200_000,        // Claude Opus 4.6 默认
        val reserveTokensFloor: Int = 20_000,          // 强制保留的最小 token
        val softThresholdTokens: Int = 4_000,          // 软阈值
        val keepRecentTokens: Int = 10_000,            // 保留最近内容的预算
        val maxHistoryShare: Double = 0.5,             // 历史最大占比
        val identifierPolicy: IdentifierPolicy = IdentifierPolicy.STRICT
    )

    /**
     * 压缩模式
     */
    enum class CompactionMode {
        SAFEGUARD,  // 保守模式
        DEFAULT     // 默认模式
    }

    /**
     * 标识符保留策略
     */
    enum class IdentifierPolicy {
        STRICT,     // 保留所有标识符
        OFF,        // 不保留
        CUSTOM      // 自定义（TODO）
    }

    /**
     * 压缩阈值
     */
    private val compactionThreshold: Int
        get() = config.contextWindowTokens - config.reserveTokensFloor - config.softThresholdTokens

    /**
     * 检查是否需要压缩
     *
     * @param messages 当前消息列表
     * @return 是否需要压缩
     */
    fun needsCompaction(messages: List<LegacyMessage>): Boolean {
        val totalTokens = TokenEstimator.estimateMessagesTokens(messages)
        val threshold = compactionThreshold

        Log.d(TAG, "Token check: $totalTokens / $threshold (threshold)")

        return totalTokens >= threshold
    }

    /**
     * 压缩消息历史
     *
     * @param messages 原始消息列表
     * @return 压缩后的消息列表
     */
    suspend fun compress(messages: List<LegacyMessage>): List<LegacyMessage> {
        if (messages.size <= 3) {
            Log.d(TAG, "Not enough messages to compress (${messages.size})")
            return messages
        }

        try {
            // 1. 分离系统消息和历史消息
            val systemMessages = messages.filter { it.role == "system" }
            val historyMessages = messages.filter { it.role != "system" }

            if (historyMessages.size <= 2) {
                return messages
            }

            // 2. 计算保留的最近消息数量
            val recentTokenBudget = config.keepRecentTokens
            var recentCount = 0
            var recentTokens = 0

            for (i in historyMessages.indices.reversed()) {
                val msgTokens = TokenEstimator.estimateMessageTokens(historyMessages[i])
                if (recentTokens + msgTokens > recentTokenBudget) {
                    break
                }
                recentTokens += msgTokens
                recentCount++
            }

            // 至少保留最后 2 条消息
            recentCount = recentCount.coerceAtLeast(2)

            // 3. 分割消息：需要压缩的 + 保留的最近消息
            val toCompress = historyMessages.dropLast(recentCount)
            val toKeep = historyMessages.takeLast(recentCount)

            if (toCompress.isEmpty()) {
                Log.d(TAG, "No messages to compress")
                return messages
            }

            Log.d(TAG, "Compressing ${toCompress.size} messages, keeping ${toKeep.size} recent")

            // 4. 生成摘要
            val summary = generateSummary(toCompress)

            // 5. 构建压缩后的消息列表
            val compressedMessages = mutableListOf<LegacyMessage>()

            // 添加系统消息
            compressedMessages.addAll(systemMessages)

            // 添加摘要消息
            compressedMessages.add(
                LegacyMessage(
                    role = "assistant",
                    content = """
                    [COMPACTED HISTORY]

                    This is a summary of ${toCompress.size} earlier messages in this conversation:

                    $summary

                    [END COMPACTED HISTORY]
                    """.trimIndent()
                )
            )

            // 添加保留的最近消息
            compressedMessages.addAll(toKeep)

            val originalTokens = TokenEstimator.estimateMessagesTokens(messages)
            val compressedTokens = TokenEstimator.estimateMessagesTokens(compressedMessages)
            val savedTokens = originalTokens - compressedTokens
            val compressionRatio = (savedTokens.toDouble() / originalTokens * 100).toInt()

            Log.d(TAG, "Compression complete: $originalTokens → $compressedTokens tokens (saved $savedTokens, ${compressionRatio}%)")

            return compressedMessages

        } catch (e: Exception) {
            Log.e(TAG, "Compression failed, returning original messages", e)
            return messages
        }
    }

    /**
     * 生成消息摘要
     */
    private suspend fun generateSummary(messages: List<LegacyMessage>): String {
        if (messages.isEmpty()) return ""

        // 构建摘要提示
        val conversationText = messages.joinToString("\n\n") { message ->
            val role = message.role.uppercase()
            val content = message.content?.toString() ?: ""
            "[$role]: $content"
        }

        val summaryPrompt = buildSummaryPrompt(conversationText)

        try {
            // 使用 LLM 生成摘要（使用 Extended Thinking）
            val summaryMessages = listOf(
                LegacyMessage(role = "user", content = summaryPrompt)
            )

            val response = legacyRepository.chatWithTools(
                messages = summaryMessages,
                tools = emptyList(),
                reasoningEnabled = true  // 使用 Extended Thinking 提高摘要质量
            )

            val message = response.choices.firstOrNull()?.message
            if (message == null) {
                return "[Failed to generate summary: no response]"
            }

            return message.content?.toString() ?: "Summary generation failed"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate summary", e)
            return "[Failed to generate summary: ${e.message}]"
        }
    }

    /**
     * 构建摘要提示
     */
    private fun buildSummaryPrompt(conversationText: String): String {
        val identifierGuidance = when (config.identifierPolicy) {
            IdentifierPolicy.STRICT -> """
            CRITICAL: Preserve ALL identifiers exactly as they appear:
            - UUIDs and hashes (e.g., a1b2c3d4-e5f6-7890)
            - API keys and tokens
            - File paths and URLs
            - Package names (e.g., com.example.app)
            - Hostnames and IP addresses
            - Port numbers
            - Database IDs
            - Any alphanumeric codes or identifiers

            Do NOT summarize, abbreviate, or replace identifiers with placeholders.
            """.trimIndent()

            IdentifierPolicy.OFF -> ""

            IdentifierPolicy.CUSTOM -> ""  // TODO: 自定义指导
        }

        return """
        Summarize the following conversation into a concise summary that captures the key information.

        $identifierGuidance

        Focus on preserving:
        - Active tasks and their current state
        - Bulk operation progress (e.g., "5/17 items completed")
        - User's last request
        - Decisions made and their rationales
        - TODOs, open questions, constraints
        - Any commitments or follow-up actions

        Prioritize recent context over older history.

        Conversation to summarize:

        $conversationText

        Provide a concise summary (aim for 30-50% of original length):
        """.trimIndent()
    }
}
