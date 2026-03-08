package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import com.xiaomo.androidforclaw.providers.ToolDefinition
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.quickjs.QuickJSExecutor

/**
 * JavaScript 执行器 Tool - 基于 QuickJS Module
 *
 * 提供完整的 JavaScript 运行时环境，支持:
 * - ES6+ 语法 (const, let, arrow functions, etc.)
 * - async/await 异步编程
 * - Promise
 * - 内置工具库 (lodash-like)
 * - Android 桥接 (文件、HTTP、系统调用)
 *
 * 使用场景:
 * - 数据处理和分析 (JSON, CSV, etc.)
 * - 字符串操作和文本处理
 * - 数组和对象操作
 * - 简单的网络请求
 * - 文件读写
 */
class JavaScriptExecutorTool(private val context: Context) : Tool {
    override val name = "javascript_exec"
    override val description = "Execute JavaScript code with QuickJS engine (ES6+, async/await, Node.js-like APIs)"

    private val executor = QuickJSExecutor(context)

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "code" to PropertySchema("string", "JavaScript code to execute (ES6+, async/await supported)"),
                        "timeout" to PropertySchema("number", "Execution timeout in milliseconds (default: 30000)")
                    ),
                    required = listOf("code")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val code = args["code"] as? String
        if (code.isNullOrBlank()) {
            return ToolResult.error("Missing or empty 'code' parameter")
        }

        val timeout = (args["timeout"] as? Number)?.toLong() ?: 30000L

        val result = executor.execute(code, timeout)

        return if (result.success) {
            ToolResult.success(
                content = result.result ?: "undefined",
                metadata = result.metadata
            )
        } else {
            ToolResult.error(result.error ?: "Unknown error")
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        executor.cleanup()
    }
}
