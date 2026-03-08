package com.xiaomo.androidforclaw.agent.tools.memory

import android.util.Log
import com.xiaomo.androidforclaw.agent.memory.MemoryManager
import com.xiaomo.androidforclaw.agent.tools.Skill
import com.xiaomo.androidforclaw.agent.tools.SkillResult
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition
import java.io.File

/**
 * memory_search 工具
 * 对齐 OpenClaw memory-tool.ts
 *
 * 搜索记忆文件中的相关内容
 * 当前版本：基于关键词的文本搜索
 * TODO: 未来添加向量嵌入和语义搜索
 */
class MemorySearchSkill(
    private val memoryManager: MemoryManager,
    private val workspacePath: String
) : Skill {
    companion object {
        private const val TAG = "MemorySearchSkill"
        private const val DEFAULT_MAX_RESULTS = 6
        private const val CONTEXT_LINES = 2  // 上下文行数
    }

    override val name = "memory_search"
    override val description = "Search through memory files for relevant information. Returns matching text snippets with context."

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "query" to PropertySchema(
                            type = "string",
                            description = "Search query (keywords or phrases)"
                        ),
                        "max_results" to PropertySchema(
                            type = "integer",
                            description = "Maximum number of results to return (default: 6)"
                        )
                    ),
                    required = listOf("query")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): SkillResult {
        val query = args["query"] as? String
            ?: return SkillResult.error("Missing required parameter: query")

        val maxResults = (args["max_results"] as? Number)?.toInt() ?: DEFAULT_MAX_RESULTS

        return try {
            val results = searchMemoryFiles(query, maxResults)

            if (results.isEmpty()) {
                return SkillResult.success(
                    content = "No matching memories found for query: \"$query\"",
                    metadata = mapOf(
                        "query" to query,
                        "results_count" to 0
                    )
                )
            }

            // 格式化结果
            val formatted = results.mapIndexed { index, result ->
                """
                ## Result ${index + 1} (${result.file}, lines ${result.startLine}-${result.endLine})
                ${result.snippet}
                """.trimIndent()
            }.joinToString("\n\n")

            SkillResult.success(
                content = formatted,
                metadata = mapOf(
                    "query" to query,
                    "results_count" to results.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Memory search failed", e)
            SkillResult.error("Failed to search memory: ${e.message}")
        }
    }

    /**
     * 搜索结果
     */
    private data class SearchResult(
        val file: String,
        val startLine: Int,
        val endLine: Int,
        val snippet: String,
        val score: Double
    )

    /**
     * 搜索记忆文件
     */
    private suspend fun searchMemoryFiles(query: String, maxResults: Int): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val queryLower = query.lowercase()
        val queryWords = queryLower.split(Regex("\\s+")).filter { it.length > 2 }

        // 获取所有记忆文件
        val memoryFiles = memoryManager.listMemoryFiles()

        // 添加今天和昨天的日志
        val workspaceDir = File(workspacePath)
        val todayLog = File(workspaceDir, "memory/${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}.md")
        if (todayLog.exists()) {
            memoryFiles.toMutableList().add(todayLog.absolutePath)
        }

        // 搜索每个文件
        for (filePath in memoryFiles) {
            try {
                val file = File(filePath)
                if (!file.exists()) continue

                val lines = file.readLines()
                val relativePath = file.relativeTo(File(workspacePath)).path

                // 搜索每一行及其上下文
                for (i in lines.indices) {
                    val line = lines[i]
                    val lineLower = line.lowercase()

                    // 计算匹配分数
                    var score = 0.0

                    // 完整查询匹配
                    if (lineLower.contains(queryLower)) {
                        score += 10.0
                    }

                    // 单词匹配
                    for (word in queryWords) {
                        if (lineLower.contains(word)) {
                            score += 1.0
                        }
                    }

                    // 如果有匹配，提取上下文
                    if (score > 0) {
                        val startLine = (i - CONTEXT_LINES).coerceAtLeast(0)
                        val endLine = (i + CONTEXT_LINES + 1).coerceAtMost(lines.size)
                        val snippet = lines.subList(startLine, endLine).joinToString("\n")

                        results.add(
                            SearchResult(
                                file = relativePath,
                                startLine = startLine + 1,
                                endLine = endLine,
                                snippet = snippet,
                                score = score
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to search file: $filePath", e)
            }
        }

        // 按分数排序并返回前 N 个结果
        return results.sortedByDescending { it.score }.take(maxResults)
    }
}
