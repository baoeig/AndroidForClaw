package com.xiaomo.androidforclaw.providers

import com.xiaomo.androidforclaw.config.ModelApi
import com.xiaomo.androidforclaw.config.ModelDefinition
import com.xiaomo.androidforclaw.config.ProviderConfig
import com.xiaomo.androidforclaw.providers.llm.Message
import com.xiaomo.androidforclaw.providers.llm.ToolDefinition as NewToolDefinition
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject

/**
 * API 适配器
 * 负责将通用的请求格式转换为不同 API 提供商的特定格式
 *
 * 参考：OpenClaw src/agents/llm-adapters/
 */
object ApiAdapter {

    /**
     * 构建请求体
     */
    fun buildRequestBody(
        provider: ProviderConfig,
        model: ModelDefinition,
        messages: List<Message>,
        tools: List<NewToolDefinition>?,
        temperature: Double,
        maxTokens: Int?,
        reasoningEnabled: Boolean
    ): JSONObject {
        val api = model.api ?: provider.api

        return when (api) {
            ModelApi.ANTHROPIC_MESSAGES -> buildAnthropicRequest(
                model, messages, tools, temperature, maxTokens, reasoningEnabled
            )
            ModelApi.OPENAI_COMPLETIONS,
            ModelApi.OPENAI_RESPONSES,
            ModelApi.OPENAI_CODEX_RESPONSES -> buildOpenAIRequest(
                model, messages, tools, temperature, maxTokens, reasoningEnabled
            )
            ModelApi.GOOGLE_GENERATIVE_AI -> buildGeminiRequest(
                model, messages, tools, temperature, maxTokens
            )
            ModelApi.OLLAMA -> buildOllamaRequest(
                provider, model, messages, tools, temperature, maxTokens
            )
            ModelApi.GITHUB_COPILOT -> buildCopilotRequest(
                model, messages, tools, temperature, maxTokens
            )
            else -> {
                // 默认使用 OpenAI 兼容格式
                buildOpenAIRequest(model, messages, tools, temperature, maxTokens, reasoningEnabled)
            }
        }
    }

    /**
     * 构建请求头
     */
    fun buildHeaders(
        provider: ProviderConfig,
        model: ModelDefinition
    ): Headers {
        val builder = Headers.Builder()

        // Provider 级别的自定义头
        provider.headers?.forEach { (key, value) ->
            builder.add(key, value)
        }

        // Model 级别的自定义头（优先级更高）
        model.headers?.forEach { (key, value) ->
            builder.add(key, value)
        }

        // 添加 API Key（如果配置了 authHeader）
        if (provider.authHeader && provider.apiKey != null) {
            val api = model.api ?: provider.api
            when (api) {
                ModelApi.ANTHROPIC_MESSAGES -> {
                    builder.add("x-api-key", provider.apiKey)
                    builder.add("anthropic-version", "2023-06-01")
                }
                else -> {
                    // OpenAI 风格的 Authorization 头
                    builder.add("Authorization", "Bearer ${provider.apiKey}")
                }
            }
        }

        // 设置 Content-Type
        builder.add("Content-Type", "application/json")

        return builder.build()
    }

    /**
     * 解析响应
     */
    fun parseResponse(
        api: String,
        responseBody: String
    ): ParsedResponse {
        return when (api) {
            ModelApi.ANTHROPIC_MESSAGES -> parseAnthropicResponse(responseBody)
            ModelApi.OPENAI_COMPLETIONS,
            ModelApi.OPENAI_RESPONSES,
            ModelApi.OPENAI_CODEX_RESPONSES,
            ModelApi.OLLAMA,
            ModelApi.GITHUB_COPILOT -> parseOpenAIResponse(responseBody)
            ModelApi.GOOGLE_GENERATIVE_AI -> parseGeminiResponse(responseBody)
            else -> parseOpenAIResponse(responseBody)  // 默认按 OpenAI 格式解析
        }
    }

    // ============ Anthropic Messages API ============

    private fun buildAnthropicRequest(
        model: ModelDefinition,
        messages: List<Message>,
        tools: List<NewToolDefinition>?,
        temperature: Double,
        maxTokens: Int?,
        reasoningEnabled: Boolean
    ): JSONObject {
        val json = JSONObject()

        json.put("model", model.id)
        json.put("max_tokens", maxTokens ?: model.maxTokens)
        json.put("temperature", temperature)

        // 转换消息格式
        val anthropicMessages = JSONArray()
        var systemMessage: String? = null

        messages.forEach { message ->
            when (message.role) {
                "system" -> {
                    systemMessage = message.content
                }
                "user", "assistant" -> {
                    val msg = JSONObject()
                    msg.put("role", message.role)
                    msg.put("content", message.content)
                    anthropicMessages.put(msg)
                }
                "tool" -> {
                    // Anthropic 使用 tool_result 格式
                    val msg = JSONObject()
                    msg.put("role", "user")
                    msg.put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "tool_result")
                            put("tool_use_id", message.toolCallId ?: "")
                            put("content", message.content)
                        })
                    })
                    anthropicMessages.put(msg)
                }
            }
        }

        json.put("messages", anthropicMessages)

        // 添加 system 消息
        if (systemMessage != null) {
            json.put("system", systemMessage)
        }

        // 添加 tools
        if (!tools.isNullOrEmpty()) {
            val anthropicTools = JSONArray()
            tools.forEach { tool ->
                val toolJson = JSONObject()
                toolJson.put("name", tool.function.name)
                toolJson.put("description", tool.function.description)
                toolJson.put("input_schema", JSONObject(tool.function.parameters.toString()))
                anthropicTools.put(toolJson)
            }
            json.put("tools", anthropicTools)
        }

        // Extended Thinking 支持
        if (reasoningEnabled && model.reasoning) {
            json.put("thinking", JSONObject().apply {
                put("type", "enabled")
                put("budget_tokens", 10000)
            })
        }

        return json
    }

    private fun parseAnthropicResponse(responseBody: String): ParsedResponse {
        val json = JSONObject(responseBody)

        var content: String? = null
        val toolCalls = mutableListOf<ToolCall>()
        var thinkingContent: String? = null

        // 解析 content 数组
        val contentArray = json.optJSONArray("content")
        if (contentArray != null) {
            for (i in 0 until contentArray.length()) {
                val block = contentArray.getJSONObject(i)
                when (block.getString("type")) {
                    "text" -> {
                        content = block.getString("text")
                    }
                    "thinking" -> {
                        thinkingContent = block.getString("thinking")
                    }
                    "tool_use" -> {
                        toolCalls.add(
                            ToolCall(
                                id = block.getString("id"),
                                name = block.getString("name"),
                                arguments = block.getJSONObject("input").toString()
                            )
                        )
                    }
                }
            }
        }

        // 解析 usage
        val usage = json.optJSONObject("usage")?.let {
            Usage(
                promptTokens = it.optInt("input_tokens", 0),
                completionTokens = it.optInt("output_tokens", 0)
            )
        }

        return ParsedResponse(
            content = content,
            toolCalls = toolCalls.ifEmpty { null },
            thinkingContent = thinkingContent,
            usage = usage,
            finishReason = json.optString("stop_reason")
        )
    }

    // ============ OpenAI Chat Completions API ============

    private fun buildOpenAIRequest(
        model: ModelDefinition,
        messages: List<Message>,
        tools: List<NewToolDefinition>?,
        temperature: Double,
        maxTokens: Int?,
        reasoningEnabled: Boolean
    ): JSONObject {
        val json = JSONObject()

        json.put("model", model.id)
        json.put("temperature", temperature)

        // maxTokens 字段名称（根据兼容性配置）
        val maxTokensField = model.compat?.maxTokensField ?: "max_tokens"
        json.put(maxTokensField, maxTokens ?: model.maxTokens)

        // 转换消息格式
        val openaiMessages = JSONArray()
        messages.forEach { message ->
            val msg = JSONObject()
            msg.put("role", message.role)
            msg.put("content", message.content)

            if (message.toolCalls != null) {
                val toolCallsArray = JSONArray()
                message.toolCalls.forEach { toolCall ->
                    toolCallsArray.put(JSONObject().apply {
                        put("id", toolCall.id)
                        put("type", "function")
                        put("function", JSONObject().apply {
                            put("name", toolCall.name)
                            put("arguments", toolCall.arguments)
                        })
                    })
                }
                msg.put("tool_calls", toolCallsArray)
            }

            if (message.toolCallId != null) {
                msg.put("tool_call_id", message.toolCallId)
            }

            openaiMessages.put(msg)
        }

        json.put("messages", openaiMessages)

        // 添加 tools
        if (!tools.isNullOrEmpty()) {
            val openaiTools = JSONArray()
            tools.forEach { tool ->
                openaiTools.put(JSONObject(tool.toString()))
            }
            json.put("tools", openaiTools)
        }

        // 推理支持（OpenAI o1/o3 模型）
        if (reasoningEnabled && model.reasoning) {
            if (model.compat?.supportsReasoningEffort == true) {
                json.put("reasoning_effort", "medium")
            }
        }

        return json
    }

    private fun parseOpenAIResponse(responseBody: String): ParsedResponse {
        val json = JSONObject(responseBody)

        val choices = json.getJSONArray("choices")
        if (choices.length() == 0) {
            return ParsedResponse(content = null)
        }

        val choice = choices.getJSONObject(0)
        val message = choice.getJSONObject("message")

        val content = message.optString("content", null)
        val toolCallsArray = message.optJSONArray("tool_calls")
        val toolCalls = if (toolCallsArray != null) {
            mutableListOf<ToolCall>().apply {
                for (i in 0 until toolCallsArray.length()) {
                    val tc = toolCallsArray.getJSONObject(i)
                    val function = tc.getJSONObject("function")
                    add(
                        ToolCall(
                            id = tc.getString("id"),
                            name = function.getString("name"),
                            arguments = function.getString("arguments")
                        )
                    )
                }
            }
        } else null

        // 解析 usage
        val usage = json.optJSONObject("usage")?.let {
            Usage(
                promptTokens = it.optInt("prompt_tokens", 0),
                completionTokens = it.optInt("completion_tokens", 0)
            )
        }

        return ParsedResponse(
            content = content,
            toolCalls = toolCalls,
            usage = usage,
            finishReason = choice.optString("finish_reason")
        )
    }

    // ============ Google Gemini API ============

    private fun buildGeminiRequest(
        model: ModelDefinition,
        messages: List<Message>,
        tools: List<NewToolDefinition>?,
        temperature: Double,
        maxTokens: Int?
    ): JSONObject {
        val json = JSONObject()

        // Gemini 使用 contents 数组
        val contents = JSONArray()
        messages.filter { it.role != "system" }.forEach { message ->
            val content = JSONObject()
            content.put("role", when (message.role) {
                "assistant" -> "model"
                else -> "user"
            })
            content.put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", message.content)
                })
            })
            contents.put(content)
        }

        json.put("contents", contents)

        // Generation config
        json.put("generationConfig", JSONObject().apply {
            put("temperature", temperature)
            put("maxOutputTokens", maxTokens ?: model.maxTokens)
        })

        return json
    }

    private fun parseGeminiResponse(responseBody: String): ParsedResponse {
        val json = JSONObject(responseBody)

        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return ParsedResponse(content = null)
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        return ParsedResponse(
            content = text,
            finishReason = candidate.optString("finishReason")
        )
    }

    // ============ Ollama API ============

    private fun buildOllamaRequest(
        provider: ProviderConfig,
        model: ModelDefinition,
        messages: List<Message>,
        tools: List<NewToolDefinition>?,
        temperature: Double,
        maxTokens: Int?
    ): JSONObject {
        val json = buildOpenAIRequest(model, messages, tools, temperature, maxTokens, false)

        // Ollama 特殊处理：可能需要注入 num_ctx
        if (provider.injectNumCtxForOpenAICompat == true) {
            json.put("options", JSONObject().apply {
                put("num_ctx", model.contextWindow)
            })
        }

        return json
    }

    // ============ GitHub Copilot API ============

    private fun buildCopilotRequest(
        model: ModelDefinition,
        messages: List<Message>,
        tools: List<NewToolDefinition>?,
        temperature: Double,
        maxTokens: Int?
    ): JSONObject {
        // GitHub Copilot 使用 OpenAI 兼容格式
        return buildOpenAIRequest(model, messages, tools, temperature, maxTokens, false)
    }
}

/**
 * 解析后的响应
 */
data class ParsedResponse(
    val content: String?,
    val toolCalls: List<ToolCall>? = null,
    val thinkingContent: String? = null,
    val usage: Usage? = null,
    val finishReason: String? = null
)

/**
 * Tool Call
 */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

/**
 * Token 使用统计
 */
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int
) {
    val totalTokens: Int get() = promptTokens + completionTokens
}
