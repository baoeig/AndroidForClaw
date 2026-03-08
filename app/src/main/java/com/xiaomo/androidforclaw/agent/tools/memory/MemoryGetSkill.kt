package com.xiaomo.androidforclaw.agent.tools.memory

import com.xiaomo.androidforclaw.agent.memory.MemoryManager
import com.xiaomo.androidforclaw.agent.tools.Skill
import com.xiaomo.androidforclaw.agent.tools.SkillResult
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition
import java.io.File

/**
 * memory_get 工具
 * 对齐 OpenClaw memory-tool.ts
 *
 * 读取指定的记忆文件或日志
 */
class MemoryGetSkill(
    private val memoryManager: MemoryManager,
    private val workspacePath: String
) : Skill {
    override val name = "memory_get"
    override val description = "Read a specific memory file or daily log. Use this to retrieve stored memories, user preferences, or past session notes."

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "path" to PropertySchema(
                            type = "string",
                            description = "Path to the memory file, relative to workspace. Examples: 'MEMORY.md', 'memory/2024-03-07.md', 'memory/projects.md'"
                        ),
                        "start_line" to PropertySchema(
                            type = "integer",
                            description = "Starting line number (1-indexed, optional)"
                        ),
                        "line_count" to PropertySchema(
                            type = "integer",
                            description = "Number of lines to read (optional, default: all)"
                        )
                    ),
                    required = listOf("path")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        val path = args["path"] as? String
            ?: return SkillResult.error("Missing required parameter: path")

        val startLine = (args["start_line"] as? Number)?.toInt()
        val lineCount = (args["line_count"] as? Number)?.toInt()

        return try {
            // 验证路径安全性（防止目录遍历攻击）
            if (path.contains("..") || path.startsWith("/")) {
                return SkillResult.error("Invalid path: path must be relative and cannot contain '..'")
            }

            // 构建完整路径
            val file = File(workspacePath, path)

            // 验证文件在 workspace 内
            if (!file.canonicalPath.startsWith(File(workspacePath).canonicalPath)) {
                return SkillResult.error("Invalid path: file must be within workspace")
            }

            // 验证文件存在
            if (!file.exists()) {
                return SkillResult.error("File not found: $path")
            }

            // 验证是 Markdown 文件
            if (!file.name.endsWith(".md")) {
                return SkillResult.error("Invalid file type: only .md files are allowed")
            }

            // 读取文件内容
            val content = file.readText()

            // 如果指定了行范围，提取对应行
            val result = if (startLine != null) {
                val lines = content.lines()
                val start = (startLine - 1).coerceIn(0, lines.size)
                val count = lineCount ?: (lines.size - start)
                val end = (start + count).coerceIn(start, lines.size)

                lines.subList(start, end).joinToString("\n")
            } else {
                content
            }

            SkillResult.success(
                content = result,
                metadata = mapOf(
                    "path" to path,
                    "size" to result.length,
                    "lines" to result.lines().size
                )
            )
        } catch (e: Exception) {
            SkillResult.error("Failed to read memory file: ${e.message}")
        }
    }
}
