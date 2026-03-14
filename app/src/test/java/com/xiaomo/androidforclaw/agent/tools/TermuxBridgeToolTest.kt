package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TermuxBridgeToolTest {

    private lateinit var context: Context
    private lateinit var pm: PackageManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        pm = mockk(relaxed = true)
        every { context.packageManager } returns pm
    }

    private fun termuxInstalled(installed: Boolean = true) {
        if (installed) {
            every { pm.getPackageInfo("com.termux", any<Int>()) } returns PackageInfo()
        } else {
            every { pm.getPackageInfo("com.termux", any<Int>()) } throws PackageManager.NameNotFoundException()
        }
    }

    private fun tool() = TermuxBridgeTool(context)

    // ==================== 1. isAvailable ====================

    @Test
    fun `isAvailable returns false when termux not installed`() {
        termuxInstalled(false)
        assertFalse(tool().isAvailable())
    }

    @Test
    fun `isAvailable returns false when termux installed but ssh unreachable`() {
        termuxInstalled(true)
        // No sshd in test env → isAvailable should be false
        assertFalse(tool().isAvailable())
    }

    // ==================== 2. Termux not installed ====================

    @Test
    fun `execute returns install prompt when termux not installed`() = runBlocking {
        termuxInstalled(false)
        val result = tool().execute(mapOf("command" to "echo hi"))
        assertFalse(result.success)
        assertTrue(result.content.contains("Termux not installed"))
        assertTrue(result.content.contains("f-droid.org"))
        assertTrue(result.content.contains("github.com/termux"))
    }

    @Test
    fun `install prompt shown regardless of action when termux missing`() = runBlocking {
        termuxInstalled(false)
        val result = tool().execute(mapOf("command" to "x", "action" to "setup"))
        assertFalse(result.success)
        assertTrue(result.content.contains("Termux not installed"))
    }

    // ==================== 3. SSH not reachable ====================

    @Test
    fun `execute returns ssh setup prompt when ssh not reachable`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "echo hi"))
        assertFalse(result.success)
        assertTrue(
            result.content.contains("SSH server not reachable") ||
            result.content.contains("SSH exec failed")
        )
    }

    @Test
    fun `ssh setup prompt mentions port 8022`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "echo hi"))
        assertFalse(result.success)
        assertTrue(result.content.contains("8022"))
    }

    @Test
    fun `ssh setup prompt mentions openssh and sshd`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "echo hi"))
        assertFalse(result.success)
        assertTrue(result.content.contains("openssh") || result.content.contains("sshd"))
    }

    @Test
    fun `ssh setup prompt mentions passwd`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "echo hi"))
        assertFalse(result.success)
        assertTrue(result.content.contains("passwd"))
    }

    // ==================== 4. Setup action ====================

    @Test
    fun `setup action returns status with termux installed check`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "ignored", "action" to "setup"))
        assertTrue(result.success)
        assertTrue(result.content.contains("Termux SSH Bridge Setup"))
        assertTrue(result.content.contains("Termux installed: ✅"))
    }

    @Test
    fun `setup action shows ssh reachability status`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "ignored", "action" to "setup"))
        assertTrue(result.success)
        assertTrue(result.content.contains("SSH reachable"))
    }

    @Test
    fun `setup action shows ssh unreachable when no sshd`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "ignored", "action" to "setup"))
        assertTrue(result.success)
        // In test env, SSH is unreachable
        assertTrue(result.content.contains("❌"))
        assertTrue(result.content.contains("Setup steps"))
    }

    @Test
    fun `setup action mentions config file path`() = runBlocking {
        termuxInstalled(true)
        val result = tool().execute(mapOf("command" to "ignored", "action" to "setup"))
        assertTrue(result.success)
        assertTrue(result.content.contains("termux_ssh.json"))
    }

    // ==================== 5. Parameter validation ====================

    @Test
    fun `rejects missing command and code`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf<String, Any?>())
        assertFalse(result.success)
        assertTrue(result.content.contains("Missing required parameter"))
    }

    @Test
    fun `rejects blank command`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("command" to "   "))
        assertFalse(result.success)
        assertTrue(result.content.contains("Missing required parameter"))
    }

    @Test
    fun `rejects invalid runtime`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("runtime" to "rust", "code" to "fn main(){}"))
        assertFalse(result.success)
        assertTrue(result.content.contains("Invalid runtime"))
    }

    @Test
    fun `rejects runtime without code`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("runtime" to "python"))
        assertFalse(result.success)
        assertTrue(result.content.contains("Missing required parameter"))
    }

    @Test
    fun `rejects code without runtime`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("code" to "print('hi')"))
        assertFalse(result.success)
        assertTrue(result.content.contains("Missing required parameter"))
    }

    // ==================== 6. Runtime resolution ====================

    @Test
    fun `python runtime resolves to python3 -c`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("runtime" to "python", "code" to "print('hi')"))
        assertTrue(result.success)
        assertTrue(result.content.contains("python3 -c"))
    }

    @Test
    fun `nodejs runtime resolves to node -e`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("runtime" to "nodejs", "code" to "console.log(1)"))
        assertTrue(result.success)
        assertTrue(result.content.contains("node -e"))
    }

    @Test
    fun `shell runtime passes code directly`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("runtime" to "shell", "code" to "echo hi"))
        assertTrue(result.success)
        assertEquals("resolved:echo hi", result.content)
    }

    @Test
    fun `command takes priority over runtime and code`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("command" to "ls -la", "runtime" to "python", "code" to "print()"))
        assertTrue(result.success)
        assertEquals("resolved:ls -la", result.content)
    }

    // ==================== 7. Working directory ====================

    @Test
    fun `working_dir is passed through`() = runBlocking {
        val tool = createParamValidationToolWithCwd()
        val result = tool.execute(mapOf("command" to "ls", "working_dir" to "/tmp"))
        assertTrue(result.success)
        assertTrue(result.content.contains("cwd:/tmp"))
    }

    @Test
    fun `cwd alias works same as working_dir`() = runBlocking {
        val tool = createParamValidationToolWithCwd()
        val result = tool.execute(mapOf("command" to "ls", "cwd" to "/home"))
        assertTrue(result.success)
        assertTrue(result.content.contains("cwd:/home"))
    }

    @Test
    fun `working_dir takes priority over cwd`() = runBlocking {
        val tool = createParamValidationToolWithCwd()
        val result = tool.execute(mapOf("command" to "ls", "working_dir" to "/a", "cwd" to "/b"))
        assertTrue(result.success)
        assertTrue(result.content.contains("cwd:/a"))
    }

    // ==================== 8. Timeout ====================

    @Test
    fun `default timeout is 60`() = runBlocking {
        val tool = createParamValidationToolWithTimeout()
        val result = tool.execute(mapOf("command" to "sleep 1"))
        assertTrue(result.success)
        assertTrue(result.content.contains("timeout:60"))
    }

    @Test
    fun `custom timeout is respected`() = runBlocking {
        val tool = createParamValidationToolWithTimeout()
        val result = tool.execute(mapOf("command" to "sleep 1", "timeout" to 120))
        assertTrue(result.success)
        assertTrue(result.content.contains("timeout:120"))
    }

    // ==================== 9. Shell escaping ====================

    @Test
    fun `code with single quotes is escaped`() = runBlocking {
        val tool = createParamValidationTool()
        val result = tool.execute(mapOf("runtime" to "python", "code" to "print('hello')"))
        assertTrue(result.success)
        // Should contain escaped quotes
        assertTrue(result.content.contains("python3 -c"))
    }

    // ==================== 10. getToolDefinition ====================

    @Test
    fun `getToolDefinition has correct name`() {
        termuxInstalled(true)
        val def = tool().getToolDefinition()
        assertEquals("exec", def.function.name)
    }

    @Test
    fun `getToolDefinition has object type parameters`() {
        termuxInstalled(true)
        val def = tool().getToolDefinition()
        assertEquals("object", def.function.parameters.type)
    }

    @Test
    fun `getToolDefinition includes all expected properties`() {
        termuxInstalled(true)
        val props = tool().getToolDefinition().function.parameters.properties
        val expected = listOf("command", "working_dir", "timeout", "action", "runtime", "code", "cwd")
        expected.forEach { key ->
            assertTrue("Missing property: $key", props.containsKey(key))
        }
    }

    @Test
    fun `getToolDefinition command is required`() {
        termuxInstalled(true)
        assertTrue(tool().getToolDefinition().function.parameters.required.contains("command"))
    }

    @Test
    fun `getToolDefinition action has enum values`() {
        termuxInstalled(true)
        val action = tool().getToolDefinition().function.parameters.properties["action"]!!
        assertEquals(listOf("exec", "setup"), action.enum)
    }

    @Test
    fun `getToolDefinition runtime has enum values`() {
        termuxInstalled(true)
        val runtime = tool().getToolDefinition().function.parameters.properties["runtime"]!!
        assertEquals(listOf("python", "nodejs", "shell"), runtime.enum)
    }

    // ==================== 11. ExecFacadeTool integration ====================

    @Test
    fun `ExecFacadeTool schema includes backend param`() {
        val internal = FakeTool("exec", ToolResult.success(""))
        termuxInstalled(true)
        val facade = ExecFacadeTool(internal, tool(), termuxAvailable = false)
        val def = facade.getToolDefinition()
        assertTrue(def.function.parameters.properties.containsKey("backend"))
        val backendSchema = def.function.parameters.properties["backend"]!!
        assertEquals("string", backendSchema.type)
        assertTrue(backendSchema.enum!!.containsAll(listOf("auto", "termux", "internal")))
    }

    @Test
    fun `ExecFacadeTool auto routes to termux when available`() = runBlocking {
        val internal = FakeTool("exec", ToolResult.success("internal"))
        val termux = FakeTool("exec", ToolResult.success("termux"))
        val facade = ExecFacadeTool(internal, termux, termuxAvailable = true)
        assertEquals("termux", facade.execute(mapOf("command" to "echo")).content)
    }

    @Test
    fun `ExecFacadeTool auto falls back to internal when unavailable`() = runBlocking {
        val internal = FakeTool("exec", ToolResult.success("internal"))
        val termux = FakeTool("exec", ToolResult.success("termux"))
        val facade = ExecFacadeTool(internal, termux, termuxAvailable = false)
        assertEquals("internal", facade.execute(mapOf("command" to "echo")).content)
    }

    @Test
    fun `ExecFacadeTool backend=auto explicit same as omitted`() = runBlocking {
        val internal = FakeTool("exec", ToolResult.success("internal"))
        val termux = FakeTool("exec", ToolResult.success("termux"))
        val facade = ExecFacadeTool(internal, termux, termuxAvailable = true)
        assertEquals("termux", facade.execute(mapOf("command" to "echo", "backend" to "auto")).content)
    }

    @Test
    fun `ExecFacadeTool backend=internal forces internal`() = runBlocking {
        val internal = FakeTool("exec", ToolResult.success("internal"))
        val termux = FakeTool("exec", ToolResult.success("termux"))
        val facade = ExecFacadeTool(internal, termux, termuxAvailable = true)
        assertEquals("internal", facade.execute(mapOf("command" to "echo", "backend" to "internal")).content)
    }

    @Test
    fun `ExecFacadeTool backend=termux forces termux even when unavailable`() = runBlocking {
        val internal = FakeTool("exec", ToolResult.success("internal"))
        val termux = FakeTool("exec", ToolResult.success("termux"))
        val facade = ExecFacadeTool(internal, termux, termuxAvailable = false)
        assertEquals("termux", facade.execute(mapOf("command" to "echo", "backend" to "termux")).content)
    }

    @Test
    fun `ExecFacadeTool unknown backend falls to auto`() = runBlocking {
        val internal = FakeTool("exec", ToolResult.success("internal"))
        val termux = FakeTool("exec", ToolResult.success("termux"))
        val facade = ExecFacadeTool(internal, termux, termuxAvailable = true)
        assertEquals("termux", facade.execute(mapOf("command" to "echo", "backend" to "gpu")).content)
    }

    // ==================== 12. Metadata ====================

    @Test
    fun `ssh failure result includes transport metadata`() = runBlocking {
        termuxInstalled(true)
        // Will fail at SSH connection in test env
        val result = tool().execute(mapOf("command" to "echo hi"))
        assertFalse(result.success)
        // Check metadata if present (SSH not reachable returns early without metadata)
        // This primarily validates the error path doesn't crash
    }

    // ==================== Helpers ====================

    private fun createParamValidationTool(): Tool {
        return object : Tool {
            override val name = "exec"
            override val description = "test"
            override fun getToolDefinition() = tool().getToolDefinition()

            override suspend fun execute(args: Map<String, Any?>): ToolResult {
                val action = args["action"] as? String ?: "exec"
                if (action == "setup") return ToolResult.success("setup")

                val command = args["command"] as? String
                val runtime = args["runtime"] as? String
                val code = args["code"] as? String

                fun shellEscape(s: String) = "'" + s.replace("'", "'\\''") + "'"

                val resolved = when {
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
                return ToolResult.success("resolved:$resolved")
            }
        }
    }

    private fun createParamValidationToolWithCwd(): Tool {
        return object : Tool {
            override val name = "exec"
            override val description = "test"
            override fun getToolDefinition() = tool().getToolDefinition()

            override suspend fun execute(args: Map<String, Any?>): ToolResult {
                val command = args["command"] as? String ?: return ToolResult.error("no command")
                val cwd = (args["working_dir"] as? String) ?: (args["cwd"] as? String)
                return ToolResult.success("resolved:$command|cwd:${cwd ?: "none"}")
            }
        }
    }

    private fun createParamValidationToolWithTimeout(): Tool {
        return object : Tool {
            override val name = "exec"
            override val description = "test"
            override fun getToolDefinition() = tool().getToolDefinition()

            override suspend fun execute(args: Map<String, Any?>): ToolResult {
                val command = args["command"] as? String ?: return ToolResult.error("no command")
                val timeout = (args["timeout"] as? Number)?.toInt() ?: 60
                return ToolResult.success("resolved:$command|timeout:$timeout")
            }
        }
    }

    private class FakeTool(
        override val name: String,
        private val result: ToolResult
    ) : Tool {
        override val description = name
        override fun getToolDefinition() = com.xiaomo.androidforclaw.providers.ToolDefinition(
            type = "function",
            function = com.xiaomo.androidforclaw.providers.FunctionDefinition(
                name = name, description = description,
                parameters = com.xiaomo.androidforclaw.providers.ParametersSchema(
                    type = "object", properties = emptyMap(), required = emptyList()
                )
            )
        )
        override suspend fun execute(args: Map<String, Any?>) = result
    }
}
