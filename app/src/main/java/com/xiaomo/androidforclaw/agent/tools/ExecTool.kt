package com.xiaomo.androidforclaw.agent.tools

import android.util.Log
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Exec Tool - 执行 shell 命令
 * 参考 nanobot 的 ExecTool
 */
class ExecTool(
    private val timeout: Long = 60000L, // 60 seconds
    private val workingDir: String? = null
) : Tool {
    companion object {
        private const val TAG = "ExecTool"

        // 危险命令黑名单
        private val DENY_PATTERNS = listOf(
            Regex("""\brm\s+-[rf]{1,2}\b"""),           // rm -r, rm -rf
            Regex("""\bformat\b"""),                    // format
            Regex("""\b(shutdown|reboot|poweroff)\b"""), // 系统电源
            Regex("""\bdd\s+if="""),                    // dd 命令
        )
    }

    override val name = "exec"
    override val description = "Execute shell commands on Android. Only Android built-in commands available (ls, cat, echo, grep, find, etc.). No npm, python, git, curl. No root access. Supported: file ops (ls, cat, cp, mv, mkdir, rm), text processing (grep, sed, awk), system info (getprop, ps, df). For app control use tap/swipe/type/open_app."

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "command" to PropertySchema("string", "要执行的 shell 命令"),
                        "working_dir" to PropertySchema("string", "可选的工作目录")
                    ),
                    required = listOf("command")
                )
            )
        )
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val command = args["command"] as? String
        val workDir = args["working_dir"] as? String ?: workingDir

        if (command == null) {
            return ToolResult.error("Missing required parameter: command")
        }

        // 安全检查
        val guardError = guardCommand(command)
        if (guardError != null) {
            return ToolResult.error(guardError)
        }

        Log.d(TAG, "Executing command: $command")
        return withContext(Dispatchers.IO) {
            try {
                val processBuilder = ProcessBuilder()
                if (workDir != null) {
                    processBuilder.directory(java.io.File(workDir))
                }

                // 分割命令（简单实现，不处理复杂的引号）
                val cmdArray = if (command.contains(" ")) {
                    listOf("sh", "-c", command)
                } else {
                    command.split(" ")
                }

                processBuilder.command(cmdArray)
                processBuilder.redirectErrorStream(false)

                val process = processBuilder.start()

                // 带超时的等待
                val result = withTimeout(timeout) {
                    val stdout = BufferedReader(InputStreamReader(process.inputStream))
                        .readText()
                    val stderr = BufferedReader(InputStreamReader(process.errorStream))
                        .readText()

                    process.waitFor()

                    val exitCode = process.exitValue()

                    buildString {
                        if (stdout.isNotEmpty()) {
                            append(stdout)
                        }
                        if (stderr.isNotEmpty()) {
                            if (isNotEmpty()) append("\n")
                            append("STDERR:\n$stderr")
                        }
                        if (exitCode != 0) {
                            if (isNotEmpty()) append("\n")
                            append("Exit code: $exitCode")
                        }
                    }.ifEmpty { "(no output)" }
                }

                // 截断过长输出
                val maxLen = 10000
                val finalResult = if (result.length > maxLen) {
                    result.take(maxLen) + "\n... (truncated, ${result.length - maxLen} more chars)"
                } else {
                    result
                }

                ToolResult.success(finalResult)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                ToolResult.error("Command timed out after ${timeout}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed", e)
                ToolResult.error("Command execution failed: ${e.message}")
            }
        }
    }

    /**
     * 安全检查：阻止危险命令
     */
    private fun guardCommand(command: String): String? {
        val lower = command.lowercase()

        for (pattern in DENY_PATTERNS) {
            if (pattern.containsMatchIn(lower)) {
                return "Command blocked by safety guard (dangerous pattern detected)"
            }
        }

        return null
    }
}
