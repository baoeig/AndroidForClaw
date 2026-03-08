package com.xiaomo.androidforclaw.config

import android.content.Context
import android.util.Log

/**
 * ConfigLoader 测试运行器
 *
 * 测试配置加载、环境变量替换、验证等功能
 */
class ConfigLoaderTestRunner(private val context: Context) {

    companion object {
        private const val TAG = "ConfigLoaderTest"
    }

    private val loader = ConfigLoader(context)

    /**
     * 运行所有测试
     */
    fun runAllTests() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "开始配置系统测试")
        Log.i(TAG, "========================================")

        val tests = listOf(
            ::testLoadConfig,
            ::testDefaultConfig,
            ::testGetProviderConfig,
            ::testGetModelDefinition,
            ::testListAllModels,
            ::testConfigValidation,
            ::testEnvVarReplacement
        )

        var passed = 0
        var failed = 0

        tests.forEach { test ->
            try {
                test()
                passed++
                Log.i(TAG, "✅ ${test.name} PASSED")
            } catch (e: Exception) {
                failed++
                Log.e(TAG, "❌ ${test.name} FAILED: ${e.message}", e)
            }
        }

        Log.i(TAG, "========================================")
        Log.i(TAG, "测试完成: $passed passed, $failed failed")
        Log.i(TAG, "========================================")

        if (failed == 0) {
            Log.i(TAG, "🎉 所有测试通过！")
        }
    }

    /**
     * 测试加载配置
     */
    private fun testLoadConfig() {
        Log.i(TAG, "\n[Test] 加载配置")

        val config = loader.loadModelsConfig()

        // 验证基本结构
        require(config.mode in listOf("merge", "replace")) {
            "mode 应该是 'merge' 或 'replace'"
        }

        require(config.providers.isNotEmpty()) {
            "至少应该有一个 provider"
        }

        Log.i(TAG, "  - Mode: ${config.mode}")
        Log.i(TAG, "  - Providers: ${config.providers.keys.joinToString(", ")}")
        Log.i(TAG, "  - Total models: ${config.providers.values.sumOf { it.models.size }}")
    }

    /**
     * 测试默认配置
     */
    private fun testDefaultConfig() {
        Log.i(TAG, "\n[Test] 默认配置")

        val config = loader.loadModelsConfig()

        // 检查 anthropic provider
        val anthropicProvider = config.providers["anthropic"]
        requireNotNull(anthropicProvider) { "应该包含 anthropic provider" }

        require(anthropicProvider.baseUrl.isNotBlank()) {
            "anthropic baseUrl 不能为空"
        }

        require(anthropicProvider.models.isNotEmpty()) {
            "anthropic 应该至少有一个模型"
        }

        // 检查 Claude Opus 4.6 (默认模型)
        val opusModel = anthropicProvider.models.find { it.id == "ppio/pa/claude-opus-4-6" }
        requireNotNull(opusModel) { "应该包含 ppio/pa/claude-opus-4-6 模型" }

        require(opusModel.reasoning) {
            "Claude Opus 4.6 支持 reasoning"
        }

        require(opusModel.input.contains("text") && opusModel.input.contains("image")) {
            "Claude Opus 4.6 应该支持 text 和 image 输入"
        }

        Log.i(TAG, "  - Anthropic BaseURL: ${anthropicProvider.baseUrl}")
        Log.i(TAG, "  - API Type: ${anthropicProvider.api}")
        Log.i(TAG, "  - Opus Context Window: ${opusModel.contextWindow}")
        Log.i(TAG, "  - Opus Max Tokens: ${opusModel.maxTokens}")
    }

    /**
     * 测试获取 Provider 配置
     */
    private fun testGetProviderConfig() {
        Log.i(TAG, "\n[Test] 获取 Provider 配置")

        val anthropicConfig = loader.getProviderConfig("anthropic")
        requireNotNull(anthropicConfig) { "应该能获取 anthropic 配置" }

        require(anthropicConfig.api == "anthropic-messages") {
            "anthropic API 类型应该是 anthropic-messages"
        }

        Log.i(TAG, "  - Provider: anthropic")
        Log.i(TAG, "  - API Type: ${anthropicConfig.api}")
        Log.i(TAG, "  - Models Count: ${anthropicConfig.models.size}")
    }

    /**
     * 测试获取模型定义
     */
    private fun testGetModelDefinition() {
        Log.i(TAG, "\n[Test] 获取模型定义")

        val opusModel = loader.getModelDefinition("anthropic", "ppio/pa/claude-opus-4-6")
        requireNotNull(opusModel) { "应该能获取 ppio/pa/claude-opus-4-6 定义" }

        require(opusModel.name == "Claude Opus 4.6") {
            "模型名称应该是 'Claude Opus 4.6'"
        }

        require(opusModel.contextWindow == 200000) {
            "Context window 应该是 200000"
        }

        Log.i(TAG, "  - Model ID: ${opusModel.id}")
        Log.i(TAG, "  - Model Name: ${opusModel.name}")
        Log.i(TAG, "  - Reasoning: ${opusModel.reasoning}")
        Log.i(TAG, "  - Context Window: ${opusModel.contextWindow}")
    }

    /**
     * 测试列出所有模型
     */
    private fun testListAllModels() {
        Log.i(TAG, "\n[Test] 列出所有模型")

        val allModels = loader.listAllModels()

        require(allModels.isNotEmpty()) {
            "应该至少有一个模型"
        }

        Log.i(TAG, "  - Total models: ${allModels.size}")

        allModels.forEach { (provider, model) ->
            Log.i(TAG, "    [$provider] ${model.id} - ${model.name}")
        }
    }

    /**
     * 测试配置验证
     */
    private fun testConfigValidation() {
        Log.i(TAG, "\n[Test] 配置验证")

        // 验证会在 loadModelsConfig() 中自动执行
        val config = loader.loadModelsConfig()

        // 额外验证
        config.providers.forEach { (providerName, provider) ->
            require(provider.baseUrl.startsWith("http://") || provider.baseUrl.startsWith("https://")) {
                "Provider '$providerName' 的 baseUrl 应该以 http:// 或 https:// 开头"
            }

            provider.models.forEach { model ->
                require(model.id.isNotBlank()) {
                    "模型 ID 不能为空"
                }

                require(model.contextWindow > 0) {
                    "Context window 必须大于 0"
                }

                require(model.maxTokens > 0 && model.maxTokens <= model.contextWindow) {
                    "Max tokens 必须在 0 到 context window 之间"
                }
            }
        }

        Log.i(TAG, "  ✅ 所有配置验证通过")
    }

    /**
     * 测试环境变量替换
     */
    private fun testEnvVarReplacement() {
        Log.i(TAG, "\n[Test] 环境变量替换")

        val config = loader.loadModelsConfig()
        val anthropicProvider = config.providers["anthropic"]
        requireNotNull(anthropicProvider) { "应该包含 anthropic provider" }

        // 检查 apiKey 是否被替换（不应该包含 ${} 格式）
        if (anthropicProvider.apiKey != null) {
            require(!anthropicProvider.apiKey.contains("\${")) {
                "API key 不应该包含未替换的环境变量"
            }
            Log.i(TAG, "  - API Key 已替换: ${anthropicProvider.apiKey.take(10)}***")
        } else {
            Log.w(TAG, "  - API Key 为空（可能环境变量未设置）")
        }
    }

    /**
     * 打印完整配置信息
     */
    fun printConfigInfo() {
        Log.i(TAG, "\n========================================")
        Log.i(TAG, "配置信息")
        Log.i(TAG, "========================================")

        val config = loader.loadModelsConfig()

        Log.i(TAG, "Mode: ${config.mode}")
        Log.i(TAG, "\nProviders (${config.providers.size}):")

        config.providers.forEach { (providerName, provider) ->
            Log.i(TAG, "\n  [$providerName]")
            Log.i(TAG, "    Base URL: ${provider.baseUrl}")
            Log.i(TAG, "    API Type: ${provider.api}")
            Log.i(TAG, "    Auth Header: ${provider.authHeader}")
            Log.i(TAG, "    API Key: ${if (provider.apiKey != null) "***" else "null"}")
            Log.i(TAG, "    Models (${provider.models.size}):")

            provider.models.forEach { model ->
                Log.i(TAG, "      - ${model.id}")
                Log.i(TAG, "        Name: ${model.name}")
                Log.i(TAG, "        Reasoning: ${model.reasoning}")
                Log.i(TAG, "        Input: ${model.input.joinToString(", ")}")
                Log.i(TAG, "        Context: ${model.contextWindow} tokens")
                Log.i(TAG, "        Max Tokens: ${model.maxTokens}")
                Log.i(TAG, "        Cost: \$${model.cost.input}/\$${model.cost.output} per 1M tokens")
            }
        }

        Log.i(TAG, "========================================")
    }
}
