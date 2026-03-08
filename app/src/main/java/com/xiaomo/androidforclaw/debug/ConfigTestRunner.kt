package com.xiaomo.androidforclaw.config

import android.content.Context
import android.util.Log

/**
 * 简单的配置测试运行器
 * 可以在任何地方直接调用
 */
object ConfigTestRunner {
    private const val TAG = "ConfigTestRunner"

    /**
     * 运行基本配置测试
     */
    fun runBasicTests(context: Context) {
        Log.i(TAG, "========================================")
        Log.i(TAG, "开始配置系统快速测试")
        Log.i(TAG, "========================================")

        val configLoader = ConfigLoader(context)

        // 测试 1: 加载模型配置
        try {
            Log.i(TAG, "\n测试 1: 加载模型配置")
            val modelsConfig = configLoader.loadModelsConfig()
            Log.i(TAG, "✅ 加载成功: ${modelsConfig.providers.size} 个 providers")

            modelsConfig.providers.forEach { (name, provider) ->
                Log.i(TAG, "  Provider: $name")
                Log.i(TAG, "    Base URL: ${provider.baseUrl}")
                Log.i(TAG, "    API: ${provider.api}")
                Log.i(TAG, "    Models: ${provider.models.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试 1 失败: ${e.message}", e)
        }

        // 测试 2: 加载 OpenClaw 配置
        try {
            Log.i(TAG, "\n测试 2: 加载 OpenClaw 配置")
            val openClawConfig = configLoader.loadOpenClawConfig()
            Log.i(TAG, "✅ 加载成功")
            Log.i(TAG, "  Default Model: ${openClawConfig.agent.defaultModel}")
            Log.i(TAG, "  Max Iterations: ${openClawConfig.agent.maxIterations}")
            Log.i(TAG, "  Thinking Enabled: ${openClawConfig.thinking.enabled}")
            Log.i(TAG, "  Thinking Budget: ${openClawConfig.thinking.budgetTokens}")
            Log.i(TAG, "  Gateway Port: ${openClawConfig.gateway.port}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试 2 失败: ${e.message}", e)
        }

        // 测试 3: 获取特定 Provider
        try {
            Log.i(TAG, "\n测试 3: 获取 Provider 配置")
            val provider = configLoader.getProviderConfig("anthropic")
            if (provider != null) {
                Log.i(TAG, "✅ 获取成功")
                Log.i(TAG, "  Base URL: ${provider.baseUrl}")
                Log.i(TAG, "  Models: ${provider.models.size}")
            } else {
                Log.e(TAG, "❌ Provider 不存在")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试 3 失败: ${e.message}", e)
        }

        // 测试 4: 获取模型定义
        try {
            Log.i(TAG, "\n测试 4: 获取模型定义")
            val model = configLoader.getModelDefinition("anthropic", "ppio/pa/claude-opus-4-6")
            if (model != null) {
                Log.i(TAG, "✅ 获取成功")
                Log.i(TAG, "  Name: ${model.name}")
                Log.i(TAG, "  Context Window: ${model.contextWindow}")
                Log.i(TAG, "  Max Tokens: ${model.maxTokens}")
                Log.i(TAG, "  Reasoning: ${model.reasoning}")
            } else {
                Log.e(TAG, "❌ 模型不存在")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试 4 失败: ${e.message}", e)
        }

        // 测试 5: 列出所有模型
        try {
            Log.i(TAG, "\n测试 5: 列出所有模型")
            val models = configLoader.listAllModels()
            Log.i(TAG, "✅ 找到 ${models.size} 个模型")
            models.forEach { (providerName, model) ->
                Log.i(TAG, "  [$providerName] ${model.name} (${model.id})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 测试 5 失败: ${e.message}", e)
        }

        Log.i(TAG, "\n========================================")
        Log.i(TAG, "配置系统测试完成")
        Log.i(TAG, "========================================")
    }

    /**
     * 测试 LegacyRepository 配置集成
     */
    fun testLegacyRepository(context: Context) {
        Log.i(TAG, "\n========================================")
        Log.i(TAG, "测试 LegacyRepository 配置集成")
        Log.i(TAG, "========================================")

        try {
            val legacyRepository = com.xiaomo.androidforclaw.providers.LegacyRepository(context)
            Log.i(TAG, "\n✅ LegacyRepository 初始化成功")
            // Log.i(TAG, legacyRepository.getConfigInfo())
        } catch (e: Exception) {
            Log.e(TAG, "❌ LegacyRepository 测试失败: ${e.message}", e)
        }

        Log.i(TAG, "\n========================================")
    }
}
