package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.xiaomo.androidforclaw.providers.FunctionDefinition
import com.xiaomo.androidforclaw.providers.ParametersSchema
import com.xiaomo.androidforclaw.providers.PropertySchema
import com.xiaomo.androidforclaw.providers.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * TermuxBridge Tool - Execute commands in Termux via SSH
 *
 * Communication: SSH to Termux's sshd on localhost:8022
 *
 * Requirements:
 * - Termux installed (F-Droid or GitHub)
 * - openssh installed in Termux: pkg install openssh
 * - sshd running in Termux: sshd
 * - Password set in Termux: passwd
 *
 * Optional:
 * - Termux:API for extra capabilities
 */
class TermuxBridgeTool(private val context: Context) : Tool {
    companion object {
        private const val TAG = "TermuxBridgeTool"
        private const val TERMUX_PACKAGE = "com.termux"
        private const val SSH_HOST = "127.0.0.1"
        private const val SSH_PORT = 8022
        private const val SSH_USER = "u0_a" // Termux default user prefix; will try common patterns
        private const val DEFAULT_TIMEOUT_S = 60

        // Config file for SSH credentials
        private const val CONFIG_DIR = "/sdcard/.androidforclaw"
        private const val SSH_CONFIG_FILE = "$CONFIG_DIR/termux_ssh.json"
    }

    override val name = "exec"
    override val description = "Run shell commands via Termux SSH bridge"

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = FunctionDefinition(
                name = name,
                description = description,
                parameters = ParametersSchema(
                    type = "object",
                    properties = mapOf(
                        "command" to PropertySchema(
                            type = "string",
                            description = "Shell command to execute in Termux"
                        ),
                        "working_dir" to PropertySchema(
                            type = "string",
                            description = "Working directory (optional, default: Termux home)"
                        ),
                        "timeout" to PropertySchema(
                            type = "number",
                            description = "Execution timeout in seconds (optional, default: 60)"
                        ),
                        "action" to PropertySchema(
                            type = "string",
                            description = "Action: exec (default) or setup",
                            enum = listOf("exec", "setup")
                        ),
                        "runtime" to PropertySchema(
                            type = "string",
                            description = "Backward-compatible runtime",
                            enum = listOf("python", "nodejs", "shell")
                        ),
                        "code" to PropertySchema(
                            type = "string",
                            description = "Backward-compatible code string"
                        ),
                        "cwd" to PropertySchema(
                            type = "string",
                            description = "Backward-compatible working directory alias"
                        )
                    ),
                    required = listOf("command")
                )
            )
        )
    }

    fun isAvailable(): Boolean = isTermuxInstalled() && isSSHReachable()

    private fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Quick check if SSH port is reachable
     */
    private fun isSSHReachable(): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(SSH_HOST, SSH_PORT), 1000)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load SSH credentials from config file
     */
    private fun loadSSHConfig(): SSHConfig {
        try {
            val file = java.io.File(SSH_CONFIG_FILE)
            if (file.exists()) {
                val json = org.json.JSONObject(file.readText())
                return SSHConfig(
                    host = json.optString("host", SSH_HOST),
                    port = json.optInt("port", SSH_PORT),
                    user = json.optString("user", ""),
                    password = json.optString("password", ""),
                    keyFile = json.optString("key_file", "")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load SSH config: ${e.message}")
        }
        return SSHConfig()
    }

    /**
     * Create and connect SSH client
     */
    private fun createSSHClient(config: SSHConfig): SSHClient {
        val client = SSHClient()
        client.addHostKeyVerifier(PromiscuousVerifier()) // localhost, safe to skip verification
        client.connectTimeout = 10_000
        client.connect(config.host, config.port)

        when {
            config.keyFile.isNotEmpty() -> {
                val keyProvider = client.loadKeys(config.keyFile)
                client.authPublickey(config.user, keyProvider)
            }
            config.password.isNotEmpty() -> {
                client.authPassword(config.user, config.password)
            }
            else -> {
                // Try key-based auth with default key locations
                val homeDir = "/sdcard/.androidforclaw"
                val keyPaths = listOf(
                    "$homeDir/termux_id_rsa",
                    "$homeDir/id_rsa",
                    "/data/data/${context.packageName}/files/termux_id_rsa"
                )
                var authenticated = false
                for (keyPath in keyPaths) {
                    try {
                        val keyFile = java.io.File(keyPath)
                        if (keyFile.exists()) {
                            val keyProvider = client.loadKeys(keyPath)
                            client.authPublickey(config.user.ifEmpty { "shell" }, keyProvider)
                            authenticated = true
                            break
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Key auth failed with $keyPath: ${e.message}")
                    }
                }
                if (!authenticated) {
                    throw IOException(
                        "SSH authentication failed. Please configure credentials:\n" +
                        "1. Set password in Termux: passwd\n" +
                        "2. Create config: echo '{\"user\":\"u0_aXXX\",\"password\":\"YOUR_PASS\"}' > $SSH_CONFIG_FILE\n" +
                        "   (Find your user with: whoami in Termux)"
                    )
                }
            }
        }
        return client
    }

    /**
     * Execute a command via SSH and return stdout/stderr/exitCode
     */
    private fun sshExec(command: String, cwd: String?, timeoutS: Int): SSHResult {
        val config = loadSSHConfig()
        val client = createSSHClient(config)

        try {
            val session = client.startSession()
            try {
                val fullCommand = if (cwd != null) {
                    "cd ${shellEscape(cwd)} && $command"
                } else {
                    command
                }

                val cmd = session.exec(fullCommand)
                cmd.join(timeoutS.toLong(), TimeUnit.SECONDS)

                val stdout = cmd.inputStream.bufferedReader().readText()
                val stderr = cmd.errorStream.bufferedReader().readText()
                val exitCode = cmd.exitStatus ?: -1

                return SSHResult(
                    success = exitCode == 0,
                    stdout = stdout,
                    stderr = stderr,
                    exitCode = exitCode
                )
            } finally {
                session.close()
            }
        } finally {
            client.disconnect()
        }
    }

    private fun shellEscape(s: String): String {
        return "'" + s.replace("'", "'\\''") + "'"
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: "exec"

        // 1. Check Termux installed
        if (!isTermuxInstalled()) {
            return ToolResult(
                success = false,
                content = buildString {
                    appendLine("❌ Termux not installed")
                    appendLine()
                    appendLine("Please install Termux:")
                    appendLine("• F-Droid: https://f-droid.org/packages/com.termux/")
                    appendLine("• GitHub: https://github.com/termux/termux-app/releases")
                }
            )
        }

        // 2. Handle setup action
        if (action == "setup") {
            return handleSetup()
        }

        // 3. Check SSH reachable
        if (!isSSHReachable()) {
            return ToolResult(
                success = false,
                content = buildString {
                    appendLine("❌ Termux SSH server not reachable (localhost:$SSH_PORT)")
                    appendLine()
                    appendLine("Please setup SSH in Termux:")
                    appendLine("  1. Open Termux")
                    appendLine("  2. Run: pkg install openssh")
                    appendLine("  3. Run: passwd  (set a password)")
                    appendLine("  4. Run: sshd")
                    appendLine("  5. Run: whoami  (note your username)")
                    appendLine()
                    appendLine("Then configure credentials:")
                    appendLine("  echo '{\"user\":\"YOUR_USER\",\"password\":\"YOUR_PASS\"}' > $SSH_CONFIG_FILE")
                }
            )
        }

        // 4. Resolve command
        val command = args["command"] as? String
        val runtime = args["runtime"] as? String
        val code = args["code"] as? String
        val cwd = (args["working_dir"] as? String) ?: (args["cwd"] as? String)
        val timeout = (args["timeout"] as? Number)?.toInt() ?: DEFAULT_TIMEOUT_S

        val resolvedCommand = when {
            !command.isNullOrBlank() -> command
            !runtime.isNullOrBlank() && !code.isNullOrBlank() -> {
                when (runtime) {
                    "python" -> "python3 -c ${shellEscape(code)}"
                    "nodejs" -> "node -e ${shellEscape(code)}"
                    "shell" -> code
                    else -> return ToolResult.error("Invalid runtime: $runtime (use python/nodejs/shell)")
                }
            }
            else -> return ToolResult.error("Missing required parameter: command")
        }

        // 5. Execute via SSH
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(timeout * 1000L + 5000L) {
                    val result = sshExec(resolvedCommand, cwd, timeout)
                    Log.d(TAG, "SSH exec completed: exitCode=${result.exitCode}, stdout=${result.stdout.length} chars")

                    ToolResult(
                        success = result.success,
                        content = buildString {
                            if (result.stdout.isNotEmpty()) {
                                appendLine(result.stdout.trim())
                            }
                            if (result.stderr.isNotEmpty()) {
                                if (isNotEmpty()) appendLine()
                                appendLine("STDERR:")
                                appendLine(result.stderr.trim())
                            }
                            if (result.exitCode != 0) {
                                if (isNotEmpty()) appendLine()
                                appendLine("Exit code: ${result.exitCode}")
                            }
                        }.ifEmpty { "(no output)" },
                        metadata = mapOf(
                            "backend" to "termux",
                            "transport" to "ssh",
                            "stdout" to result.stdout,
                            "stderr" to result.stderr,
                            "exitCode" to result.exitCode,
                            "command" to resolvedCommand,
                            "working_dir" to (cwd ?: "")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SSH exec failed", e)
                ToolResult(
                    success = false,
                    content = "SSH exec failed: ${e.message}",
                    metadata = mapOf(
                        "backend" to "termux",
                        "transport" to "ssh",
                        "error" to (e.message ?: "unknown"),
                        "command" to resolvedCommand,
                        "working_dir" to (cwd ?: "")
                    )
                )
            }
        }
    }

    private fun handleSetup(): ToolResult {
        val reachable = isSSHReachable()
        return ToolResult(
            success = true,
            content = buildString {
                appendLine("🔧 Termux SSH Bridge Setup Status")
                appendLine()
                appendLine("Termux installed: ✅")
                appendLine("SSH reachable (localhost:$SSH_PORT): ${if (reachable) "✅" else "❌"}")
                appendLine()
                if (!reachable) {
                    appendLine("Setup steps:")
                    appendLine("  1. Open Termux")
                    appendLine("  2. pkg install openssh")
                    appendLine("  3. passwd  (set a password)")
                    appendLine("  4. sshd")
                    appendLine("  5. whoami  (note your username, e.g. u0_a123)")
                    appendLine()
                    appendLine("Configure credentials:")
                    appendLine("  echo '{\"user\":\"u0_aXXX\",\"password\":\"YOUR_PASS\"}' > $SSH_CONFIG_FILE")
                } else {
                    val config = loadSSHConfig()
                    appendLine("SSH config: ${if (config.user.isNotEmpty()) "configured ✅" else "not configured ❌"}")
                    if (config.user.isEmpty()) {
                        appendLine()
                        appendLine("Please configure credentials:")
                        appendLine("  echo '{\"user\":\"YOUR_USER\",\"password\":\"YOUR_PASS\"}' > $SSH_CONFIG_FILE")
                    }
                }
            }
        )
    }

    data class SSHConfig(
        val host: String = SSH_HOST,
        val port: Int = SSH_PORT,
        val user: String = "",
        val password: String = "",
        val keyFile: String = ""
    )

    data class SSHResult(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
        val exitCode: Int
    )
}
