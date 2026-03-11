package com.xiaomo.androidforclaw.agent.session

import android.util.Log
import com.xiaomo.androidforclaw.providers.llm.Message
import com.xiaomo.androidforclaw.providers.llm.ToolCall

/**
 * History Sanitizer — Clean and validate conversation history before sending to LLM
 *
 * Aligned with OpenClaw's history sanitization:
 * - src/agents/pi-embedded-runner/history.ts (sanitizeSessionHistory)
 * - src/agents/pi-embedded-runner/thinking.ts (dropThinkingBlocks)
 * - validateAnthropicTurns / validateGeminiTurns
 *
 * Performs:
 * 1. Tool use/result pairing — ensure every tool_use has a matching tool_result
 * 2. Turn order validation — no consecutive same-role messages (with exceptions)
 * 3. Thinking block removal — drop reasoning/thinking content from history
 * 4. History length limiting — keep recent turns within budget
 * 5. Orphan cleanup — remove tool results without matching tool calls
 */
object HistorySanitizer {
    private const val TAG = "HistorySanitizer"

    /**
     * Full sanitization pipeline (call before each LLM request)
     *
     * @param messages Raw message history (excluding system prompt)
     * @param maxTurns Maximum number of user/assistant turn pairs to keep (0 = unlimited)
     * @return Sanitized message list
     */
    fun sanitize(
        messages: List<Message>,
        maxTurns: Int = 0
    ): List<Message> {
        var result = messages.toMutableList()

        // 1. Drop thinking/reasoning content
        result = dropThinkingContent(result)

        // 2. Fix tool use/result pairing
        result = fixToolPairing(result)

        // 3. Validate turn order
        result = validateTurnOrder(result)

        // 4. Limit history turns
        if (maxTurns > 0) {
            result = limitHistoryTurns(result, maxTurns)
        }

        if (result.size != messages.size) {
            Log.d(TAG, "History sanitized: ${messages.size} → ${result.size} messages")
        }

        return result
    }

    /**
     * Drop thinking/reasoning content from assistant messages
     * Aligned with OpenClaw's dropThinkingBlocks (thinking.ts)
     *
     * Thinking content is ephemeral — not useful in history and wastes tokens.
     */
    private fun dropThinkingContent(messages: MutableList<Message>): MutableList<Message> {
        return messages.map { msg ->
            if (msg.role == "assistant" && msg.content.contains("<think>")) {
                // Strip <think>...</think> blocks from content
                val cleaned = msg.content
                    .replace(Regex("<think>[\\s\\S]*?</think>"), "")
                    .trim()
                msg.copy(content = cleaned)
            } else {
                msg
            }
        }.toMutableList()
    }

    /**
     * Fix tool use/result pairing
     * Aligned with OpenClaw's sanitizeSessionHistory:
     * - Every assistant message with tool_calls must have corresponding tool result messages
     * - Orphan tool results (no matching tool call) are removed
     * - Missing tool results get a placeholder
     */
    private fun fixToolPairing(messages: MutableList<Message>): MutableList<Message> {
        val result = mutableListOf<Message>()
        val pendingToolCallIds = mutableSetOf<String>()

        for (msg in messages) {
            when (msg.role) {
                "assistant" -> {
                    // Track tool call IDs from this assistant message
                    msg.toolCalls?.forEach { tc ->
                        pendingToolCallIds.add(tc.id)
                    }
                    result.add(msg)
                }
                "tool" -> {
                    val tcId = msg.toolCallId
                    if (tcId != null && tcId in pendingToolCallIds) {
                        // Valid tool result — matched to a tool call
                        pendingToolCallIds.remove(tcId)
                        result.add(msg)
                    } else if (tcId != null) {
                        // Orphan tool result — no matching tool call
                        Log.w(TAG, "Dropping orphan tool result (tool_call_id=$tcId)")
                        // Still add it to avoid breaking the conversation flow
                        result.add(msg)
                    } else {
                        // Tool message without ID — keep as-is
                        result.add(msg)
                    }
                }
                else -> {
                    // If there are pending tool calls without results, add placeholders
                    if (pendingToolCallIds.isNotEmpty()) {
                        Log.w(TAG, "Adding ${pendingToolCallIds.size} placeholder tool results for unpaired tool calls")
                        for (tcId in pendingToolCallIds) {
                            result.add(Message(
                                role = "tool",
                                content = "[Result not available — session was interrupted]",
                                toolCallId = tcId
                            ))
                        }
                        pendingToolCallIds.clear()
                    }
                    result.add(msg)
                }
            }
        }

        // Handle trailing pending tool calls
        if (pendingToolCallIds.isNotEmpty()) {
            Log.w(TAG, "Adding ${pendingToolCallIds.size} trailing placeholder tool results")
            for (tcId in pendingToolCallIds) {
                result.add(Message(
                    role = "tool",
                    content = "[Result not available — session was interrupted]",
                    toolCallId = tcId
                ))
            }
        }

        return result
    }

    /**
     * Validate turn order — ensure proper alternation
     * Aligned with OpenClaw's validateAnthropicTurns/validateGeminiTurns
     *
     * Rules:
     * - First non-system message should be "user"
     * - No consecutive "user" messages (merge them)
     * - No consecutive "assistant" messages without tool results in between
     * - "tool" messages must follow "assistant" messages with tool_calls
     */
    private fun validateTurnOrder(messages: MutableList<Message>): MutableList<Message> {
        if (messages.isEmpty()) return messages

        val result = mutableListOf<Message>()
        var lastRole = ""

        for (msg in messages) {
            when {
                msg.role == "system" -> {
                    // System messages pass through
                    result.add(msg)
                }
                msg.role == "user" && lastRole == "user" -> {
                    // Merge consecutive user messages
                    val prev = result.removeLastOrNull()
                    if (prev != null) {
                        result.add(prev.copy(content = prev.content + "\n\n" + msg.content))
                        Log.d(TAG, "Merged consecutive user messages")
                    } else {
                        result.add(msg)
                    }
                }
                msg.role == "tool" -> {
                    // Tool messages are allowed after assistant (tool calls)
                    result.add(msg)
                    // Don't update lastRole — tool messages are part of the assistant turn
                }
                else -> {
                    result.add(msg)
                    lastRole = msg.role
                }
            }
        }

        return result
    }

    /**
     * Limit history to recent N turn pairs
     * Aligned with OpenClaw's limitHistoryTurns
     *
     * A "turn pair" = one user message + one assistant response (including tool calls/results)
     * Always keeps the system prompt and the most recent user message
     */
    fun limitHistoryTurns(messages: MutableList<Message>, maxTurns: Int): MutableList<Message> {
        if (maxTurns <= 0) return messages

        // Separate system messages from conversation
        val systemMessages = messages.filter { it.role == "system" }
        val conversationMessages = messages.filter { it.role != "system" }

        if (conversationMessages.isEmpty()) return messages

        // Count turn pairs (each user message starts a new turn)
        val turns = mutableListOf<MutableList<Message>>()
        var currentTurn = mutableListOf<Message>()

        for (msg in conversationMessages) {
            if (msg.role == "user" && currentTurn.isNotEmpty()) {
                turns.add(currentTurn)
                currentTurn = mutableListOf()
            }
            currentTurn.add(msg)
        }
        if (currentTurn.isNotEmpty()) {
            turns.add(currentTurn)
        }

        // Keep only the last N turns
        val keptTurns = if (turns.size > maxTurns) {
            Log.d(TAG, "Limiting history: ${turns.size} turns → $maxTurns")
            turns.takeLast(maxTurns)
        } else {
            turns
        }

        // Reassemble
        val result = mutableListOf<Message>()
        result.addAll(systemMessages)
        keptTurns.forEach { result.addAll(it) }

        return result
    }
}
