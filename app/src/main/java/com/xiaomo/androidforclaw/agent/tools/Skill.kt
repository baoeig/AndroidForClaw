package com.xiaomo.androidforclaw.agent.tools

import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * Skill 接口
 * 参考 nanobot 的 Skill 设计
 */
interface Skill {
    /**
     * Skill 名称（对应 function name）
     */
    val name: String

    /**
     * Skill 描述
     */
    val description: String

    /**
     * 获取 Tool Definition（用于 LLM function calling）
     */
    fun getToolDefinition(): ToolDefinition

    /**
     * 执行 skill
     * @param args 参数 Map
     * @return SkillResult 执行结果
     */
    suspend fun execute(args: Map<String, Any?>): SkillResult
}

/**
 * Skill 执行结果
 */
data class SkillResult(
    val success: Boolean,
    val content: String,
    val metadata: Map<String, Any?> = emptyMap()
) {
    companion object {
        fun success(content: String, metadata: Map<String, Any?> = emptyMap()) =
            SkillResult(true, content, metadata)

        fun error(message: String) =
            SkillResult(false, "Error: $message")
    }

    override fun toString(): String = content
}
