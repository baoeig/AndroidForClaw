package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import android.util.Log
import com.xiaomo.androidforclaw.DeviceController
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * Type Skill
 * 在当前焦点输入框中输入文本
 */
class TypeSkill(private val context: Context) : Skill {
    companion object {
        private const val TAG = "TypeSkill"
    }

    override val name = "type"
    override val description: String
        get() {
            val isAccessibilityEnabled = com.xiaomo.androidforclaw.accessibility.AccessibilityProxy.isConnected.value == true &&
                                        com.xiaomo.androidforclaw.accessibility.AccessibilityProxy.isServiceReady()
            val statusNote = if (!isAccessibilityEnabled) " ⚠️ **不可用**-无障碍服务未连接" else " ✅"
            return "在当前焦点的输入框中输入文本。使用前请先点击输入框获得焦点。$statusNote"
        }

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "text" to PropertySchema("string", "要输入的文本内容")
                    ),
                    required = listOf("text")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        val text = args["text"] as? String

        if (text == null) {
            return SkillResult.error("Missing required parameter: text")
        }

        Log.d(TAG, "Typing text: $text")
        return try {
            // 输入文本
            DeviceController.inputText(text, context)

            // 等待输入完成 + 输入法响应（根据文本长度动态调整）
            val waitTime = 100L + (text.length * 5L).coerceAtMost(300L) // 最少 100ms，最多 400ms
            kotlinx.coroutines.delay(waitTime)

            SkillResult.success(
                "Typed: $text (${text.length} chars)",
                mapOf(
                    "text" to text,
                    "length" to text.length,
                    "wait_time_ms" to waitTime
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Type failed", e)
            SkillResult.error("Type failed: ${e.message}")
        }
    }
}
