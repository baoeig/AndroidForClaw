package com.xiaomo.androidforclaw.agent.skills

import android.content.Context
import com.xiaomo.androidforclaw.agent.tools.Skill
import com.xiaomo.androidforclaw.agent.tools.SkillResult
import com.xiaomo.androidforclaw.browser.BrowserToolClient
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * BrowserForClaw - 浏览器控制 Skill
 *
 * 这是一个统一的浏览器控制入口，封装了所有浏览器操作能力。
 * 对应独立的 browserforclaw 项目，通过 HTTP API 进行通信。
 *
 * 支持的操作：
 * - navigate: 导航到 URL
 * - click: 点击元素
 * - type: 输入文本
 * - get_content: 获取页面内容
 * - wait: 等待条件
 * - scroll: 滚动页面
 * - execute: 执行 JavaScript
 * - press: 按键
 * - screenshot: 截图
 * - get_cookies/set_cookies: Cookie 操作
 * - hover: 悬停
 * - select: 下拉选择
 */
class BrowserForClawSkill(private val context: Context) : Skill {
    override val name = "browser"
    override val description = "Control browserforclaw to perform web automation tasks"

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = "Control browserforclaw browser for web automation. Unified interface supporting: navigate (to URL), click (element), type (text input), get_content (page content), wait (conditions), scroll (page), execute (JavaScript), press (keys), screenshot, get_cookies/set_cookies, hover, select (dropdown). Pass operation + relevant params (url, selector, text, etc) to browserforclaw.",
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "operation" to PropertySchema(
                            "string",
                            "Browser operation: navigate, click, type, get_content, wait, scroll, execute, press, screenshot, get_cookies, set_cookies, hover, select"
                        ),
                        "url" to PropertySchema("string", "URL for navigate operation"),
                        "selector" to PropertySchema("string", "CSS selector for element operations"),
                        "text" to PropertySchema("string", "Text for type operation"),
                        "format" to PropertySchema("string", "Content format for get_content: text, html, markdown"),
                        "direction" to PropertySchema("string", "Scroll direction: up, down, top, bottom"),
                        "script" to PropertySchema("string", "JavaScript code for execute operation"),
                        "key" to PropertySchema("string", "Key name for press operation"),
                        "timeMs" to PropertySchema("integer", "Wait time in milliseconds"),
                        "waitMs" to PropertySchema("integer", "Wait time after navigation"),
                        "timeout" to PropertySchema("integer", "Timeout for wait operations"),
                        "index" to PropertySchema("integer", "Element index when multiple match"),
                        "clear" to PropertySchema("boolean", "Clear field before typing"),
                        "submit" to PropertySchema("boolean", "Submit form after typing"),
                        "fullPage" to PropertySchema("boolean", "Capture full page screenshot"),
                        "cookies" to PropertySchema("array", "Cookie list for set_cookies"),
                        "values" to PropertySchema("array", "Values for select operation"),
                        "x" to PropertySchema("integer", "X coordinate for scroll"),
                        "y" to PropertySchema("integer", "Y coordinate for scroll")
                    ),
                    required = listOf("operation")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        val operation = args["operation"] as? String
            ?: return SkillResult.error("Missing required parameter: operation")

        return try {
            val browserClient = BrowserToolClient(context)

            // 将 operation 映射到 browserforclaw 的工具名称
            val toolName = "browser_$operation"

            // 移除 operation 参数，其余参数直接传递给 browserforclaw
            val toolArgs = args.filterKeys { it != "operation" }

            val result = browserClient.executeToolAsync(toolName, toolArgs)

            if (result.success) {
                // 根据操作类型格式化返回消息
                val message = when (operation) {
                    "navigate" -> "Successfully navigated to ${args["url"]}"
                    "click" -> "Successfully clicked element: ${args["selector"]}"
                    "type" -> "Successfully typed text into ${args["selector"]}"
                    "get_content" -> {
                        val content = result.data?.get("content") as? String ?: ""
                        val url = result.data?.get("url") as? String ?: ""
                        val title = result.data?.get("title") as? String ?: ""
                        "Page content retrieved:\nURL: $url\nTitle: $title\n\nContent:\n${content.take(1000)}${if (content.length > 1000) "\n...(truncated)" else ""}"
                    }
                    "wait" -> "Wait condition met"
                    "scroll" -> "Successfully scrolled"
                    "execute" -> "JavaScript executed: ${result.data?.get("result")}"
                    "press" -> "Pressed key: ${args["key"]}"
                    "screenshot" -> "Screenshot captured"
                    "get_cookies" -> "Cookies retrieved: ${result.data?.get("cookies")}"
                    "set_cookies" -> "Cookies set successfully"
                    "hover" -> "Hovered over element: ${args["selector"]}"
                    "select" -> "Selected options in ${args["selector"]}"
                    else -> "Operation completed"
                }

                SkillResult.success(message, result.data ?: emptyMap())
            } else {
                SkillResult.error(result.error ?: "Browser operation failed")
            }
        } catch (e: Exception) {
            SkillResult.error("Failed to execute browser operation: ${e.message}")
        }
    }
}
