package com.xiaomo.androidforclaw.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.xiaomo.androidforclaw.agent.tools.AndroidToolRegistry
import com.xiaomo.androidforclaw.config.ConfigLoader
import com.xiaomo.androidforclaw.core.MyApplication
import com.xiaomo.androidforclaw.data.model.TaskDataManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Agent 集成测试
 * 在真实 Android 环境中测试 Agent 功能
 *
 * 运行:
 * ./gradlew connectedDebugAndroidTest --tests "AgentIntegrationTest"
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AgentIntegrationTest {

    private lateinit var context: Context
    private lateinit var configLoader: ConfigLoader
    private lateinit var toolRegistry: AndroidToolRegistry
    private lateinit var taskDataManager: TaskDataManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<MyApplication>()
        configLoader = ConfigLoader(context)
        taskDataManager = TaskDataManager.getInstance()
        toolRegistry = AndroidToolRegistry(context, taskDataManager)
    }

    // ========== 配置系统集成测试 ==========

    @Test
    fun testConfigLoader_loadsSuccessfully() {
        val config = configLoader.loadModelsConfig()

        assertNotNull("配置应该加载成功", config)
        assertTrue("应该有 providers", config.providers.isNotEmpty())
    }

    @Test
    fun testConfigLoader_findsProviders() {
        val provider = configLoader.getProviderConfig("anthropic")

        assertNotNull("应该找到 anthropic provider", provider)
        assertNotNull("BaseUrl 不应为空", provider?.baseUrl)
    }

    @Test
    fun testOpenClawConfig_loadsSuccessfully() {
        val config = configLoader.loadOpenClawConfig()

        assertNotNull("OpenClaw 配置应该加载成功", config)
        assertTrue("maxIterations 应该 > 0", config.agent.maxIterations > 0)
        assertNotNull("skills 配置应该存在", config.skills)
    }

    // ========== Tool Registry 集成测试 ==========

    @Test
    fun testToolRegistry_hasTools() {
        val toolCount = toolRegistry.getToolCount()

        assertTrue("应该有工具注册", toolCount > 0)
    }

    @Test
    fun testToolRegistry_hasWaitSkill() {
        val hasWait = toolRegistry.contains("wait")

        assertTrue("应该包含 wait skill", hasWait)
    }

    @Test
    fun testToolRegistry_hasStopSkill() {
        val hasStop = toolRegistry.contains("stop")

        assertTrue("应该包含 stop skill", hasStop)
    }

    @Test
    fun testToolRegistry_hasLogSkill() {
        val hasLog = toolRegistry.contains("log")

        assertTrue("应该包含 log skill", hasLog)
    }

    @Test
    fun testToolRegistry_getDefinitions() {
        val definitions = toolRegistry.getToolDefinitions()

        assertTrue("应该有工具定义", definitions.isNotEmpty())

        // 验证每个定义的结构
        definitions.forEach { def ->
            assertEquals("Type 应该是 function", "function", def.type)
            assertNotNull("Function 不应为空", def.function)
            assertTrue("Name 应该非空", def.function.name.isNotBlank())
            assertTrue("Description 应该非空", def.function.description.isNotBlank())
        }
    }

    // ========== Skill 执行集成测试 ==========

    @Test
    fun testWaitSkill_executesInAndroid() = runBlocking {
        val startTime = System.currentTimeMillis()

        val result = toolRegistry.execute("wait", mapOf("duration_ms" to 100))

        val elapsed = System.currentTimeMillis() - startTime

        assertTrue("Wait 应该成功", result.success)
        assertTrue("应该等待至少 100ms", elapsed >= 95)
        assertTrue("应该在 200ms 内完成", elapsed < 200)
    }

    @Test
    fun testLogSkill_executesInAndroid() = runBlocking {
        val result = toolRegistry.execute("log", mapOf(
            "message" to "集成测试日志",
            "level" to "INFO"
        ))

        assertTrue("Log 应该成功", result.success)
    }

    @Test
    fun testStopSkill_executesInAndroid() = runBlocking {
        val result = toolRegistry.execute("stop", mapOf(
            "reason" to "集成测试停止"
        ))

        assertTrue("Stop 应该成功", result.success)
        assertTrue("应该有 stopped 元数据", result.metadata.containsKey("stopped"))
        assertEquals("stopped 应该为 true", true, result.metadata["stopped"])
    }

    @Test
    fun testMultipleSkills_executeSequentially() = runBlocking {
        // 执行多个技能
        val result1 = toolRegistry.execute("log", mapOf("message" to "第一个"))
        val result2 = toolRegistry.execute("wait", mapOf("duration_ms" to 50))
        val result3 = toolRegistry.execute("log", mapOf("message" to "第二个"))

        assertTrue("所有技能应该成功", result1.success && result2.success && result3.success)
    }

    @Test
    fun testSkill_withInvalidArguments() = runBlocking {
        // 测试缺少必需参数
        val result = toolRegistry.execute("wait", emptyMap())

        assertFalse("缺少参数应该失败", result.success)
        assertTrue("应该包含错误信息", result.content.contains("duration_ms", ignoreCase = true))
    }

    // ========== 工作空间集成测试 ==========

    @Test
    fun testWorkspace_directoryExists() {
        val workspaceDir = java.io.File("/sdcard/androidforclaw-workspace")

        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }

        assertTrue("工作空间应该存在", workspaceDir.exists())
        assertTrue("应该可读", workspaceDir.canRead())
        assertTrue("应该可写", workspaceDir.canWrite())
    }

    @Test
    fun testWorkspace_skillsDirectoryExists() {
        val skillsDir = java.io.File("/sdcard/androidforclaw-workspace/skills")

        if (!skillsDir.exists()) {
            skillsDir.mkdirs()
        }

        assertTrue("Skills 目录应该存在", skillsDir.exists())
    }

    @Test
    fun testWorkspace_canCreateFile() {
        val testFile = java.io.File("/sdcard/androidforclaw-workspace/integration_test.txt")

        try {
            testFile.writeText("Integration test content")

            assertTrue("文件应该创建成功", testFile.exists())
            assertEquals("内容应该匹配", "Integration test content", testFile.readText())

        } finally {
            testFile.delete()
        }
    }

    // ========== Assets 资源集成测试 ==========

    @Test
    fun testAssets_skillsDirectoryExists() {
        try {
            val skillsList = context.assets.list("skills")

            assertNotNull("Skills 目录应该存在", skillsList)
            // 可能为空，取决于是否有内置 skills

        } catch (e: Exception) {
            fail("访问 assets skills 失败: ${e.message}")
        }
    }

    // ========== TaskDataManager 集成测试 ==========

    @Test
    fun testTaskDataManager_initialization() {
        assertNotNull("TaskDataManager 应该初始化", taskDataManager)
    }

    // ========== 性能集成测试 ==========

    @Test
    fun testPerformance_multipleToolCalls() = runBlocking {
        val iterations = 10
        val startTime = System.currentTimeMillis()

        repeat(iterations) {
            toolRegistry.execute("log", mapOf("message" to "性能测试 $it"))
        }

        val elapsed = System.currentTimeMillis() - startTime

        // 平均每次调用应该在合理时间内（< 100ms/次）
        val avgTime = elapsed / iterations
        assertTrue("平均执行时间应该合理 (< 100ms)", avgTime < 100)
    }

    @Test
    fun testPerformance_configReload() {
        val iterations = 5
        val startTime = System.currentTimeMillis()

        repeat(iterations) {
            configLoader.reloadConfig()
        }

        val elapsed = System.currentTimeMillis() - startTime

        // 配置重载应该快速（< 500ms/次）
        val avgTime = elapsed / iterations
        assertTrue("配置重载应该快速 (< 500ms)", avgTime < 500)
    }
}
