package com.xiaomo.androidforclaw.agent.loop

import android.util.Log
import com.xiaomo.androidforclaw.agent.context.ContextErrors
import com.xiaomo.androidforclaw.agent.context.ContextManager
import com.xiaomo.androidforclaw.agent.context.ContextRecoveryResult
import com.xiaomo.androidforclaw.agent.tools.AndroidToolRegistry
import com.xiaomo.androidforclaw.agent.tools.SkillResult
import com.xiaomo.androidforclaw.agent.tools.ToolRegistry
import com.xiaomo.androidforclaw.providers.UnifiedLLMProvider
import com.xiaomo.androidforclaw.providers.llm.Message
import com.xiaomo.androidforclaw.providers.llm.ToolCall
import com.xiaomo.androidforclaw.providers.llm.systemMessage
import com.xiaomo.androidforclaw.providers.llm.userMessage
import com.xiaomo.androidforclaw.providers.llm.assistantMessage
import com.xiaomo.androidforclaw.providers.llm.toolMessage
import com.xiaomo.androidforclaw.util.LayoutExceptionLogger
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Agent Loop - 核心循环引擎
 * 参考 OpenClaw 的 Agent Loop 实现
 *
 * 执行流程:
 * 1. 接收用户消息 + 系统提示词
 * 2. 调用 LLM (支持 reasoning)
 * 3. LLM 通过 function calling 选择工具
 * 4. 直接执行 LLM 选择的工具
 * 5. 重复步骤 2-4，直到 LLM 返回最终结果或达到最大迭代次数
 *
 * 架构（参考 OpenClaw pi-tools）:
 * - ToolRegistry: 通用工具（read, write, exec, web_fetch）
 * - AndroidToolRegistry: Android 平台工具（tap, screenshot, open_app）
 * - SkillsLoader: Skills 文档（mobile-operations.md）
 */
class AgentLoop(
    private val llmProvider: UnifiedLLMProvider,
    private val toolRegistry: ToolRegistry,
    private val androidToolRegistry: AndroidToolRegistry,
    private val contextManager: ContextManager? = null,  // 可选的上下文管理器
    private val maxIterations: Int = 40,
    private val modelRef: String? = null
) {
    companion object {
        private const val TAG = "AgentLoop"
        private const val MAX_OVERFLOW_RECOVERY_ATTEMPTS = 3  // 对齐 OpenClaw
    }

    private val gson = Gson()

    // ✅ 缓存 Tool Definitions，避免每次迭代重复构建
    // 合并通用工具和 Android 平台工具
    private val allToolDefinitions by lazy {
        toolRegistry.getToolDefinitions() + androidToolRegistry.getToolDefinitions()
    }

    // 进度更新流
    private val _progressFlow = MutableSharedFlow<ProgressUpdate>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val progressFlow: SharedFlow<ProgressUpdate> = _progressFlow.asSharedFlow()

    // 停止标志
    @Volatile
    private var shouldStop = false

    /**
     * 运行 Agent Loop
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param contextHistory 历史对话记录
     * @param reasoningEnabled 是否启用 reasoning
     * @return AgentResult 包含最终内容、使用的工具、所有消息
     */
    suspend fun run(
        systemPrompt: String,
        userMessage: String,
        contextHistory: List<Message> = emptyList(),
        reasoningEnabled: Boolean = true
    ): AgentResult {
        shouldStop = false
        val messages = mutableListOf<Message>()

        // 重置上下文管理器
        contextManager?.reset()

        Log.d(TAG, "========== Agent Loop 开始 ==========")
        Log.d(TAG, "Max iterations: $maxIterations")
        Log.d(TAG, "Model: ${modelRef ?: "default"}")
        Log.d(TAG, "Reasoning: ${if (reasoningEnabled) "enabled" else "disabled"}")
        Log.d(TAG, "🔧 Universal tools: ${toolRegistry.getToolCount()}")
        Log.d(TAG, "📱 Android tools: ${androidToolRegistry.getToolCount()}")
        Log.d(TAG, "🔄 Context manager: ${if (contextManager != null) "enabled" else "disabled"}")

        // 1. 添加系统提示词
        messages.add(systemMessage(systemPrompt))
        Log.d(TAG, "✅ System prompt added (${systemPrompt.length} chars)")

        // 2. 添加历史对话
        messages.addAll(contextHistory)
        if (contextHistory.isNotEmpty()) {
            Log.d(TAG, "✅ Context history added: ${contextHistory.size} messages")
        }

        // 3. 添加用户消息
        messages.add(userMessage(userMessage))
        Log.d(TAG, "✅ User message: $userMessage")
        Log.d(TAG, "📤 准备发送第一次 LLM 请求...")

        var iteration = 0
        var finalContent: String? = null
        val toolsUsed = mutableListOf<String>()

        // 4. 主循环
        while (iteration < maxIterations && !shouldStop) {
            iteration++
            val iterationStartTime = System.currentTimeMillis()
            Log.d(TAG, "========== Iteration $iteration ==========")

            try {
                // 4.1 调用 LLM
                Log.d(TAG, "📢 发送迭代进度更新...")
                _progressFlow.emit(ProgressUpdate.Iteration(iteration))
                Log.d(TAG, "✅ 迭代进度已发送")

                Log.d(TAG, "📤 调用 UnifiedLLMProvider.chatWithTools...")
                Log.d(TAG, "   Messages: ${messages.size}, Tools+Skills: ${allToolDefinitions.size}")

                val llmStartTime = System.currentTimeMillis()
                val response = llmProvider.chatWithTools(
                    messages = messages,
                    tools = allToolDefinitions,
                    modelRef = modelRef,
                    reasoningEnabled = reasoningEnabled
                )
                val llmDuration = System.currentTimeMillis() - llmStartTime

                Log.d(TAG, "✅ LLM 响应已收到 [耗时: ${llmDuration}ms]")

                // 4.2 显示 reasoning 思考过程
                response.thinkingContent?.let { reasoning ->
                    Log.d(TAG, "🧠 Reasoning (${reasoning.length} chars)")
                    _progressFlow.emit(ProgressUpdate.Reasoning(reasoning, llmDuration))
                }

                // 4.3 检查是否有 function calls
                if (response.toolCalls != null && response.toolCalls.isNotEmpty()) {
                    Log.d(TAG, "Function calls: ${response.toolCalls.size}")

                    // 添加 assistant 消息（包含 function calls）
                    messages.add(
                        assistantMessage(
                            content = response.content,
                            toolCalls = response.toolCalls.map {
                                com.xiaomo.androidforclaw.providers.llm.ToolCall(
                                    id = it.id,
                                    name = it.name,
                                    arguments = it.arguments
                                )
                            }
                        )
                    )

                    // 执行每个 tool/skill (直接执行 LLM 选择的能力)
                    var totalExecDuration = 0L
                    for (toolCall in response.toolCalls) {
                        val functionName = toolCall.name
                        val argsJson = toolCall.arguments

                        Log.d(TAG, "🔧 Function: $functionName")
                        Log.d(TAG, "   Args: $argsJson")

                        // 解析参数
                        val args = try {
                            @Suppress("UNCHECKED_CAST")
                            gson.fromJson(argsJson, Map::class.java) as Map<String, Any?>
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse arguments", e)
                            mapOf<String, Any?>()
                        }

                        toolsUsed.add(functionName)

                        // 发送调用进度更新
                        _progressFlow.emit(ProgressUpdate.ToolCall(functionName, args))

                        // ✅ 先从通用工具查找，再从 Android 工具查找
                        val execStartTime = System.currentTimeMillis()
                        val result = if (toolRegistry.contains(functionName)) {
                            Log.d(TAG, "   → Universal tool")
                            toolRegistry.execute(functionName, args)
                        } else if (androidToolRegistry.contains(functionName)) {
                            Log.d(TAG, "   → Android tool")
                            androidToolRegistry.execute(functionName, args)
                        } else {
                            Log.e(TAG, "   ❌ Unknown function: $functionName")
                            SkillResult.error("Unknown function: $functionName")
                        }
                        val execDuration = System.currentTimeMillis() - execStartTime
                        totalExecDuration += execDuration

                        Log.d(TAG, "   Result: ${result.success}, ${result.content.take(200)}")
                        Log.d(TAG, "   ⏱️ 执行耗时: ${execDuration}ms")

                        // 添加结果到消息列表
                        messages.add(
                            toolMessage(
                                toolCallId = toolCall.id,
                                content = result.toString(),
                                name = functionName
                            )
                        )

                        // 发送结果更新
                        _progressFlow.emit(ProgressUpdate.ToolResult(functionName, result.toString(), execDuration))

                        // 检查是否是 stop skill
                        if (functionName == "stop") {
                            val metadata = result.metadata
                            val stopped = metadata["stopped"] as? Boolean ?: false
                            if (stopped) {
                                shouldStop = true
                                finalContent = result.content
                                Log.d(TAG, "Stop function called, ending loop")
                                break
                            }
                        }
                    }

                    // 继续循环，让 LLM 看到 function 结果后决定下一步
                    if (shouldStop) break

                    val iterationDuration = System.currentTimeMillis() - iterationStartTime
                    Log.d(TAG, "⏱️ 本轮迭代总耗时: ${iterationDuration}ms (LLM: ${llmDuration}ms, 执行: ${totalExecDuration}ms)")

                    // 发送迭代完成事件（带时间统计）
                    _progressFlow.emit(ProgressUpdate.IterationComplete(iteration, iterationDuration, llmDuration, totalExecDuration))
                    continue
                }

                // 4.4 没有工具调用，说明 LLM 给出了最终答案
                finalContent = response.content
                messages.add(assistantMessage(content = finalContent))

                Log.d(TAG, "Final content received (finish_reason: ${response.finishReason})")
                break

            } catch (e: Exception) {
                Log.e(TAG, "Iteration $iteration error", e)
                LayoutExceptionLogger.log("AgentLoop#run#iteration$iteration", e)

                // 检查是否是上下文超限错误
                val errorMessage = ContextErrors.extractErrorMessage(e)
                val isContextOverflow = ContextErrors.isLikelyContextOverflowError(errorMessage)

                if (isContextOverflow && contextManager != null) {
                    Log.w(TAG, "🔄 检测到上下文超限，尝试恢复...")
                    _progressFlow.emit(ProgressUpdate.ContextOverflow("Context overflow detected, attempting recovery..."))

                    // 尝试恢复
                    val recoveryResult = contextManager.handleContextOverflow(
                        error = e,
                        messages = messages
                    )

                    when (recoveryResult) {
                        is ContextRecoveryResult.Recovered -> {
                            Log.d(TAG, "✅ 上下文恢复成功: ${recoveryResult.strategy} (attempt ${recoveryResult.attempt})")
                            _progressFlow.emit(ProgressUpdate.ContextRecovered(
                                strategy = recoveryResult.strategy,
                                attempt = recoveryResult.attempt
                            ))

                            // 替换消息列表
                            messages.clear()
                            messages.addAll(recoveryResult.messages)

                            // 重试当前迭代
                            continue
                        }
                        is ContextRecoveryResult.CannotRecover -> {
                            Log.e(TAG, "❌ 上下文恢复失败: ${recoveryResult.reason}")
                            _progressFlow.emit(ProgressUpdate.Error("Context overflow: ${recoveryResult.reason}"))

                            finalContent = "执行异常: ${recoveryResult.reason}"
                            break
                        }
                    }
                } else {
                    // 非上下文超限错误
                    _progressFlow.emit(ProgressUpdate.Error(e.message ?: "Unknown error"))

                    // 尝试继续或停止
                    if (e.message?.contains("timeout", ignoreCase = true) == true) {
                        // 超时错误，可以重试
                        continue
                    } else {
                        // 其他错误，停止循环
                        finalContent = "执行异常: ${e.message}"
                        break
                    }
                }
            }
        }

        // 5. 处理循环结束
        if (finalContent == null && iteration >= maxIterations) {
            Log.w(TAG, "Max iterations ($maxIterations) reached")
            finalContent = "达到最大迭代次数 ($maxIterations)，任务未完成。" +
                    "建议将任务拆分为更小的步骤。"
        }

        Log.d(TAG, "========== Agent Loop 结束 ==========")
        Log.d(TAG, "Iterations: $iteration")
        Log.d(TAG, "Tools used: ${toolsUsed.joinToString(", ")}")

        return AgentResult(
            finalContent = finalContent ?: "无响应",
            toolsUsed = toolsUsed,
            messages = messages,
            iterations = iteration
        )
    }

    /**
     * 停止 Agent Loop
     */
    fun stop() {
        shouldStop = true
        Log.d(TAG, "Stop signal received")
    }
}

/**
 * Agent 执行结果
 */
data class AgentResult(
    val finalContent: String,
    val toolsUsed: List<String>,
    val messages: List<Message>,
    val iterations: Int
)

/**
 * 进度更新
 */
sealed class ProgressUpdate {
    /** 开始新迭代 */
    data class Iteration(val number: Int) : ProgressUpdate()

    /** Reasoning 思考过程 */
    data class Reasoning(val content: String, val llmDuration: Long) : ProgressUpdate()

    /** 工具调用 */
    data class ToolCall(val name: String, val arguments: Map<String, Any?>) : ProgressUpdate()

    /** 工具结果 */
    data class ToolResult(val name: String, val result: String, val execDuration: Long) : ProgressUpdate()

    /** 迭代完成 */
    data class IterationComplete(val number: Int, val iterationDuration: Long, val llmDuration: Long, val execDuration: Long) : ProgressUpdate()

    /** 上下文超限 */
    data class ContextOverflow(val message: String) : ProgressUpdate()

    /** 上下文恢复成功 */
    data class ContextRecovered(val strategy: String, val attempt: Int) : ProgressUpdate()

    /** 错误 */
    data class Error(val message: String) : ProgressUpdate()
}
