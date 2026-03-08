package com.xiaomo.androidforclaw.config

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 配置测试 Activity
 * 用于测试和验证配置系统
 *
 * 使用 ADB 启动:
 * adb shell am start -n com.xiaomo.androidforclaw/.config.ConfigTestActivity
 */
class ConfigTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ConfigTestActivity"
    }

    private lateinit var resultTextView: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建简单的布局
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // 标题
        val titleTextView = TextView(this).apply {
            text = "配置系统测试"
            textSize = 20f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleTextView)

        // 运行测试按钮
        val runTestButton = Button(this).apply {
            text = "运行所有测试"
            setOnClickListener { runTests() }
        }
        layout.addView(runTestButton)

        // 打印配置按钮
        val printConfigButton = Button(this).apply {
            text = "打印配置摘要"
            setOnClickListener { printConfig() }
        }
        layout.addView(printConfigButton)

        // 清除结果按钮
        val clearButton = Button(this).apply {
            text = "清除结果"
            setOnClickListener {
                resultTextView.text = ""
            }
        }
        layout.addView(clearButton)

        // 结果显示区域
        scrollView = ScrollView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        resultTextView = TextView(this).apply {
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(16, 16, 16, 16)
        }

        scrollView.addView(resultTextView)
        layout.addView(scrollView)

        setContentView(layout)

        Log.i(TAG, "ConfigTestActivity 已启动")
        appendLog("配置测试工具已就绪\n")
        appendLog("点击按钮开始测试\n")
    }

    /**
     * 运行测试
     */
    private fun runTests() {
        appendLog("\n========================================\n")
        appendLog("开始运行测试...\n")
        appendLog("========================================\n\n")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    // 基础配置测试
                    testConfigLoading()
                }

                appendLog("\n✅ 配置测试完成\n")

            } catch (e: Exception) {
                appendLog("\n❌ 测试失败: ${e.message}\n")
                Log.e(TAG, "测试失败", e)
            }

            scrollToBottom()
        }
    }

    /**
     * 测试配置加载
     */
    private fun testConfigLoading() {
        val configLoader = ConfigLoader(this)

        appendLog("1. 测试模型配置加载...\n")
        try {
            val modelsConfig = configLoader.loadModelsConfig()
            appendLog("   ✓ 加载成功: ${modelsConfig.providers.size} 个 providers\n")
        } catch (e: Exception) {
            appendLog("   ✗ 加载失败: ${e.message}\n")
        }

        appendLog("2. 测试 OpenClaw 配置加载...\n")
        try {
            val openClawConfig = configLoader.loadOpenClawConfig()
            appendLog("   ✓ 加载成功: maxIterations=${openClawConfig.agent.maxIterations}\n")
        } catch (e: Exception) {
            appendLog("   ✗ 加载失败: ${e.message}\n")
        }

        appendLog("3. 测试 Provider 查找...\n")
        try {
            val provider = configLoader.getProviderConfig("anthropic")
            if (provider != null) {
                appendLog("   ✓ 找到 anthropic provider\n")
            } else {
                appendLog("   ✗ 未找到 anthropic provider\n")
            }
        } catch (e: Exception) {
            appendLog("   ✗ 查找失败: ${e.message}\n")
        }
    }

    /**
     * 打印配置摘要
     */
    private fun printConfig() {
        appendLog("\n========================================\n")
        appendLog("配置摘要\n")
        appendLog("========================================\n\n")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val configLoader = ConfigLoader(this@ConfigTestActivity)
                val modelsConfig = configLoader.loadModelsConfig()
                val openClawConfig = configLoader.loadOpenClawConfig()

                appendLog("📦 模型配置 (models.json)\n")
                appendLog("  Mode: ${modelsConfig.mode}\n")
                appendLog("  Providers: ${modelsConfig.providers.size}\n")
                modelsConfig.providers.forEach { (name, provider) ->
                    appendLog("    [$name]\n")
                    appendLog("      URL: ${provider.baseUrl}\n")
                    appendLog("      API: ${provider.api}\n")
                    appendLog("      Models: ${provider.models.size}\n")
                    provider.models.forEach { model ->
                        appendLog("        - ${model.name} (${model.id})\n")
                    }
                }

                appendLog("\n⚙️ OpenClaw 配置 (openclaw.json)\n")
                appendLog("  Agent:\n")
                appendLog("    Model: ${openClawConfig.agent.defaultModel}\n")
                appendLog("    Max Iterations: ${openClawConfig.agent.maxIterations}\n")
                appendLog("    Timeout: ${openClawConfig.agent.timeout}ms\n")
                appendLog("    Mode: ${openClawConfig.agent.mode}\n")
                appendLog("  Thinking:\n")
                appendLog("    Enabled: ${openClawConfig.thinking.enabled}\n")
                appendLog("    Budget: ${openClawConfig.thinking.budgetTokens}\n")
                appendLog("  Gateway:\n")
                appendLog("    Enabled: ${openClawConfig.gateway.enabled}\n")
                appendLog("    Port: ${openClawConfig.gateway.port}\n")
                appendLog("  Skills:\n")
                appendLog("    Bundled: ${openClawConfig.skills.bundledPath}\n")
                appendLog("    Workspace: ${openClawConfig.skills.workspacePath}\n")
                appendLog("    Auto Load: ${openClawConfig.skills.autoLoad.joinToString(", ")}\n")

                appendLog("\n========================================\n")

            } catch (e: Exception) {
                appendLog("\n❌ 打印配置失败: ${e.message}\n")
                Log.e(TAG, "打印配置失败", e)
            }

            scrollToBottom()
        }
    }

    /**
     * 添加日志到 TextView
     */
    private fun appendLog(text: String) {
        runOnUiThread {
            resultTextView.append(text)
        }
    }

    /**
     * 滚动到底部
     */
    private fun scrollToBottom() {
        runOnUiThread {
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}
