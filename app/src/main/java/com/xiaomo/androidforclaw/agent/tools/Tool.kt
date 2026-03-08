package com.xiaomo.androidforclaw.agent.tools

import com.xiaomo.androidforclaw.providers.ToolDefinition

/**
 * Tool 接口 - 底层工具（参考 nanobot 的 Tool 基类）
 *
 * Tool 是底层的、通用的能力，例如：
 * - exec: 执行 shell 命令
 * - read_file: 读取文件
 * - write_file: 写入文件
 *
 * 与 Skill 的区别：
 * - Tool: 代码级实现，低层操作（文件、网络、shell）
 * - Skill: Android 特定能力，业务层操作（tap、screenshot）
 */
interface Tool {
    /**
     * Tool 名称（对应 function name）
     */
    val name: String

    /**
     * Tool 描述
     */
    val description: String

    /**
     * 获取 Tool Definition（用于 LLM function calling）
     */
    fun getToolDefinition(): ToolDefinition

    /**
     * 执行 tool
     * @param args 参数 Map
     * @return ToolResult 执行结果
     */
    suspend fun execute(args: Map<String, Any?>): ToolResult
}

// Tool 和 Skill 共享相同的 Result 类型
typealias ToolResult = SkillResult
