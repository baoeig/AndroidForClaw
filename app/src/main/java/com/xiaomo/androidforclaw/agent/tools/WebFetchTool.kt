package com.xiaomo.androidforclaw.agent.tools

import android.util.Log
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Web Fetch Tool - 获取网页内容
 * 参考 nanobot 的 WebFetchTool
 */
class WebFetchTool(
    private val maxChars: Int = 50000
) : Tool {
    companion object {
        private const val TAG = "WebFetchTool"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override val name = "web_fetch"
    override val description = "获取指定 URL 的网页内容。返回网页文本。"

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "url" to PropertySchema("string", "要获取的 URL"),
                        "max_chars" to PropertySchema("integer", "最大返回字符数，默认 50000")
                    ),
                    required = listOf("url")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val url = args["url"] as? String
        val maxCharsParam = (args["max_chars"] as? Number)?.toInt() ?: maxChars

        if (url == null) {
            return ToolResult.error("Missing required parameter: url")
        }

        // URL 验证
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ToolResult.error("URL must start with http:// or https://")
        }

        Log.d(TAG, "Fetching URL: $url")
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext ToolResult.error("HTTP ${response.code}: ${response.message}")
                }

                val contentType = response.header("Content-Type") ?: ""
                val body = response.body?.string() ?: ""

                // 简单的内容提取（移除 HTML 标签）
                val content = when {
                    contentType.contains("application/json", ignoreCase = true) -> {
                        // JSON 内容直接返回
                        body
                    }
                    contentType.contains("text/html", ignoreCase = true) -> {
                        // HTML 内容简单清理
                        stripHtmlTags(body)
                    }
                    else -> {
                        // 其他文本内容
                        body
                    }
                }

                // 截断过长内容
                val finalContent = if (content.length > maxCharsParam) {
                    content.take(maxCharsParam) + "\n... (truncated, ${content.length - maxCharsParam} more chars)"
                } else {
                    content
                }

                ToolResult.success(finalContent, mapOf("url" to url, "length" to content.length))
            } catch (e: Exception) {
                Log.e(TAG, "Web fetch failed", e)
                ToolResult.error("Web fetch failed: ${e.message}")
            }
        }
    }

    /**
     * 简单的 HTML 标签清理
     */
    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]+>"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
