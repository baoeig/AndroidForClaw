package com.xiaomo.androidforclaw.core

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 消息队列管理器
 *
 * 完全对齐 OpenClaw 的队列机制：
 * - interrupt: 新消息立即中断当前运行，清空队列
 * - steer: 新消息传递给正在运行的 Agent
 * - followup: 新消息加入队列，按顺序处理
 * - collect: 收集多条消息，一次性处理
 * - queue: 简单队列，FIFO
 *
 * 参考:
 * - openclaw/src/auto-reply/reply/get-reply-run.ts
 * - openclaw/src/auto-reply/reply/queue/types.ts
 * - openclaw/src/utils/queue-helpers.ts
 */
class MessageQueueManager {

    companion object {
        private const val TAG = "MessageQueueManager"
    }

    /**
     * 队列模式（对齐 OpenClaw）
     */
    enum class QueueMode {
        INTERRUPT,   // 中断当前运行
        STEER,       // 引导当前运行
        FOLLOWUP,    // 跟进队列
        COLLECT,     // 收集模式
        QUEUE        // 简单队列
    }

    /**
     * Drop 策略（对齐 OpenClaw）
     */
    enum class DropPolicy {
        OLD,         // 丢弃最旧的消息
        NEW,         // 拒绝新消息
        SUMMARIZE    // 丢弃但保留摘要
    }

    /**
     * 消息队列状态
     */
    private data class QueueState(
        val key: String,
        val mode: QueueMode,
        val messages: MutableList<QueuedMessage> = mutableListOf(),
        val isProcessing: AtomicBoolean = AtomicBoolean(false),
        val currentJob: Job? = null,
        val droppedCount: Int = 0,
        val summaryLines: MutableList<String> = mutableListOf(),
        var cap: Int = 10,
        var dropPolicy: DropPolicy = DropPolicy.OLD
    )

    /**
     * 队列中的消息
     */
    data class QueuedMessage(
        val messageId: String,
        val content: String,
        val senderId: String,
        val chatId: String,
        val chatType: String,
        val timestamp: Long = System.currentTimeMillis(),
        val metadata: Map<String, Any?> = emptyMap()
    )

    // 每个队列 key 的状态
    private val queues = ConcurrentHashMap<String, QueueState>()

    // 基础队列（用于 followup 和 queue 模式）
    private val baseQueue = KeyedAsyncQueue()

    /**
     * 入队消息
     *
     * @param key 队列键（通常是 chatId）
     * @param message 消息
     * @param mode 队列模式
     * @param processor 消息处理器
     */
    suspend fun enqueue(
        key: String,
        message: QueuedMessage,
        mode: QueueMode = QueueMode.FOLLOWUP,
        processor: suspend (QueuedMessage) -> Unit
    ) {
        when (mode) {
            QueueMode.INTERRUPT -> handleInterrupt(key, message, processor)
            QueueMode.STEER -> handleSteer(key, message, processor)
            QueueMode.FOLLOWUP -> handleFollowup(key, message, processor)
            QueueMode.COLLECT -> handleCollect(key, message, processor)
            QueueMode.QUEUE -> handleQueue(key, message, processor)
        }
    }

    /**
     * INTERRUPT 模式：立即中断当前运行，清空队列
     *
     * 对齐 OpenClaw 逻辑：
     * ```typescript
     * if (resolvedQueue.mode === "interrupt" && laneSize > 0) {
     *   const cleared = clearCommandLane(sessionLaneKey);
     *   const aborted = abortEmbeddedPiRun(sessionIdFinal);
     * }
     * ```
     */
    private suspend fun handleInterrupt(
        key: String,
        message: QueuedMessage,
        processor: suspend (QueuedMessage) -> Unit
    ) {
        val state = queues.getOrPut(key) {
            QueueState(key = key, mode = QueueMode.INTERRUPT)
        }

        // 1. 取消当前正在运行的任务
        if (state.isProcessing.get()) {
            Log.d(TAG, "🛑 [INTERRUPT] Aborting current run for $key")
            state.currentJob?.cancel()
        }

        // 2. 清空队列
        val cleared = state.messages.size
        if (cleared > 0) {
            Log.d(TAG, "🗑️  [INTERRUPT] Clearing $cleared queued messages for $key")
            state.messages.clear()
        }

        // 3. 立即处理新消息
        Log.d(TAG, "⚡ [INTERRUPT] Processing new message immediately for $key")
        state.isProcessing.set(true)
        try {
            processor(message)
        } finally {
            state.isProcessing.set(false)
        }
    }

    /**
     * STEER 模式：新消息传递给正在运行的 Agent
     *
     * 对齐 OpenClaw 逻辑：
     * - 如果 Agent 正在运行，将新消息注入到 Agent 的消息流
     * - 如果 Agent 未运行，正常处理
     */
    private suspend fun handleSteer(
        key: String,
        message: QueuedMessage,
        processor: suspend (QueuedMessage) -> Unit
    ) {
        val state = queues.getOrPut(key) {
            QueueState(key = key, mode = QueueMode.STEER)
        }

        if (state.isProcessing.get()) {
            // Agent 正在运行，将消息添加到 steer 队列
            Log.d(TAG, "🎯 [STEER] Injecting message into running Agent for $key")
            state.messages.add(message)
            // TODO: 通知 AgentLoop 有新消息（需要 AgentLoop 支持）
            notifyAgentLoop(key, message)
        } else {
            // Agent 未运行，正常处理
            Log.d(TAG, "▶️  [STEER] Agent not running, processing normally for $key")
            baseQueue.enqueue(key) {
                state.isProcessing.set(true)
                try {
                    processor(message)
                } finally {
                    state.isProcessing.set(false)
                }
            }
        }
    }

    /**
     * FOLLOWUP 模式：加入队列，按顺序处理
     *
     * 这是当前已实现的基本行为
     */
    private suspend fun handleFollowup(
        key: String,
        message: QueuedMessage,
        processor: suspend (QueuedMessage) -> Unit
    ) {
        val state = queues.getOrPut(key) {
            QueueState(key = key, mode = QueueMode.FOLLOWUP)
        }

        Log.d(TAG, "📝 [FOLLOWUP] Enqueueing message for $key (queue size: ${state.messages.size})")

        baseQueue.enqueue(key) {
            state.isProcessing.set(true)
            try {
                processor(message)
            } finally {
                state.isProcessing.set(false)
            }
        }
    }

    /**
     * COLLECT 模式：收集多条消息，一次性处理
     *
     * 对齐 OpenClaw 逻辑：
     * - 消息加入队列
     * - 当前消息处理完成后，一次性处理所有排队消息
     */
    private suspend fun handleCollect(
        key: String,
        message: QueuedMessage,
        processor: suspend (QueuedMessage) -> Unit
    ) {
        val state = queues.getOrPut(key) {
            QueueState(key = key, mode = QueueMode.COLLECT)
        }

        // 应用 drop policy
        if (!applyDropPolicy(state, message)) {
            Log.w(TAG, "🚫 [COLLECT] Message dropped due to drop policy for $key")
            return
        }

        state.messages.add(message)
        Log.d(TAG, "📦 [COLLECT] Collected message for $key (${state.messages.size} total)")

        // 如果没有正在处理，触发批量处理
        if (!state.isProcessing.get()) {
            baseQueue.enqueue(key) {
                state.isProcessing.set(true)
                try {
                    processBatch(state, processor)
                } finally {
                    state.isProcessing.set(false)
                }
            }
        }
    }

    /**
     * QUEUE 模式：简单 FIFO 队列
     */
    private suspend fun handleQueue(
        key: String,
        message: QueuedMessage,
        processor: suspend (QueuedMessage) -> Unit
    ) {
        // 与 FOLLOWUP 相同，简单排队
        handleFollowup(key, message, processor)
    }

    /**
     * 应用 drop policy
     */
    private fun applyDropPolicy(state: QueueState, newMessage: QueuedMessage): Boolean {
        if (state.cap <= 0 || state.messages.size < state.cap) {
            return true
        }

        return when (state.dropPolicy) {
            DropPolicy.NEW -> {
                // 拒绝新消息
                Log.w(TAG, "🚫 Drop policy: NEW - rejecting new message")
                false
            }
            DropPolicy.OLD -> {
                // 丢弃最旧的消息
                val dropped = state.messages.removeAt(0)
                Log.d(TAG, "🗑️  Drop policy: OLD - dropped message: ${dropped.messageId}")
                true
            }
            DropPolicy.SUMMARIZE -> {
                // 丢弃最旧的消息，但保留摘要
                val dropped = state.messages.removeAt(0)
                val summary = summarizeMessage(dropped)
                state.summaryLines.add(summary)
                Log.d(TAG, "📝 Drop policy: SUMMARIZE - dropped and summarized: ${dropped.messageId}")

                // 限制摘要数量
                if (state.summaryLines.size > state.cap) {
                    state.summaryLines.removeAt(0)
                }
                true
            }
        }
    }

    /**
     * 汇总消息（用于 SUMMARIZE drop policy）
     */
    private fun summarizeMessage(message: QueuedMessage): String {
        val content = message.content.take(100)
        return "[${message.timestamp}] ${message.senderId}: $content${if (message.content.length > 100) "..." else ""}"
    }

    /**
     * 批量处理消息（COLLECT 模式）
     */
    private suspend fun processBatch(
        state: QueueState,
        processor: suspend (QueuedMessage) -> Unit
    ) {
        if (state.messages.isEmpty()) return

        Log.d(TAG, "📦 [COLLECT] Processing batch of ${state.messages.size} messages")

        // 取出所有消息
        val batch = state.messages.toList()
        state.messages.clear()

        // 构建批量消息提示词
        val batchMessage = buildBatchMessage(batch, state)

        // 处理批量消息
        processor(batchMessage)
    }

    /**
     * 构建批量消息（COLLECT 模式）
     *
     * 对齐 OpenClaw 的 buildCollectPrompt
     */
    private fun buildBatchMessage(
        messages: List<QueuedMessage>,
        state: QueueState
    ): QueuedMessage {
        val content = buildString {
            appendLine("[Batch] Collected ${messages.size} message(s):")
            appendLine()

            // 如果有被丢弃的消息摘要
            if (state.droppedCount > 0 && state.summaryLines.isNotEmpty()) {
                appendLine("[Queue overflow] Dropped ${state.droppedCount} message(s) due to cap.")
                appendLine("Summary:")
                state.summaryLines.forEach { line ->
                    appendLine("- $line")
                }
                appendLine()
                state.summaryLines.clear()
            }

            // 列出所有消息
            messages.forEachIndexed { index, msg ->
                appendLine("Message ${index + 1}:")
                appendLine("From: ${msg.senderId}")
                appendLine("Content: ${msg.content}")
                appendLine()
            }
        }

        // 使用最后一条消息的元数据
        val lastMessage = messages.last()
        return QueuedMessage(
            messageId = "batch_${System.currentTimeMillis()}",
            content = content,
            senderId = lastMessage.senderId,
            chatId = lastMessage.chatId,
            chatType = lastMessage.chatType,
            metadata = mapOf(
                "isBatch" to true,
                "batchSize" to messages.size,
                "messageIds" to messages.map { it.messageId }
            )
        )
    }

    /**
     * 通知 AgentLoop 有新消息（STEER 模式）
     *
     * TODO: 需要 AgentLoop 支持消息注入
     */
    private fun notifyAgentLoop(key: String, message: QueuedMessage) {
        // 这里需要实现与 AgentLoop 的通信机制
        // 可能的方案：
        // 1. 使用 SharedFlow 广播新消息
        // 2. 在 AgentLoop 的每次迭代前检查队列
        // 3. 使用 Channel 传递消息
        Log.d(TAG, "⚠️  [STEER] notifyAgentLoop not implemented yet")
    }

    /**
     * 设置队列配置
     */
    fun setQueueSettings(
        key: String,
        cap: Int? = null,
        dropPolicy: DropPolicy? = null
    ) {
        val state = queues.getOrPut(key) {
            QueueState(key = key, mode = QueueMode.FOLLOWUP)
        }

        if (cap != null) {
            state.cap = cap
        }
        if (dropPolicy != null) {
            state.dropPolicy = dropPolicy
        }
    }

    /**
     * 获取队列状态（用于调试）
     */
    fun getQueueState(key: String): Map<String, Any> {
        val state = queues[key] ?: return mapOf(
            "exists" to false
        )

        return mapOf(
            "exists" to true,
            "mode" to state.mode.name,
            "isProcessing" to state.isProcessing.get(),
            "queueSize" to state.messages.size,
            "droppedCount" to state.droppedCount,
            "cap" to state.cap,
            "dropPolicy" to state.dropPolicy.name
        )
    }

    /**
     * 清空指定队列
     */
    fun clearQueue(key: String) {
        val state = queues[key] ?: return
        state.messages.clear()
        state.summaryLines.clear()
        Log.d(TAG, "🗑️  Cleared queue for $key")
    }

    /**
     * 清空所有队列
     */
    fun clearAllQueues() {
        queues.values.forEach { state ->
            state.messages.clear()
            state.summaryLines.clear()
        }
        queues.clear()
        Log.d(TAG, "🗑️  Cleared all queues")
    }
}
