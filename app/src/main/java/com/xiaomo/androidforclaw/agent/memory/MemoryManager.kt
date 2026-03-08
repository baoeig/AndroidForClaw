package com.xiaomo.androidforclaw.agent.memory

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 记忆管理器
 * 对齐 OpenClaw 记忆系统
 *
 * 功能：
 * - 长期记忆（MEMORY.md）读写
 * - 每日日志（memory/YYYY-MM-DD.md）追加
 * - 记忆文件路径管理
 */
class MemoryManager(private val workspacePath: String) {
    companion object {
        private const val TAG = "MemoryManager"
        private const val MEMORY_FILE = "MEMORY.md"
        private const val MEMORY_DIR = "memory"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val workspaceDir = File(workspacePath)
    private val memoryFile = File(workspaceDir, MEMORY_FILE)
    private val memoryDir = File(workspaceDir, MEMORY_DIR)

    init {
        // 确保目录存在
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }
        if (!memoryDir.exists()) {
            memoryDir.mkdirs()
        }
    }

    /**
     * 读取长期记忆（MEMORY.md）
     *
     * @return 记忆内容，如果文件不存在返回空字符串
     */
    suspend fun readMemory(): String = withContext(Dispatchers.IO) {
        try {
            if (memoryFile.exists()) {
                memoryFile.readText()
            } else {
                Log.d(TAG, "MEMORY.md does not exist, creating template")
                createMemoryTemplate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read MEMORY.md", e)
            ""
        }
    }

    /**
     * 写入长期记忆（MEMORY.md）
     *
     * @param content 要写入的内容
     */
    suspend fun writeMemory(content: String) = withContext(Dispatchers.IO) {
        try {
            memoryFile.writeText(content)
            Log.d(TAG, "MEMORY.md written successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write MEMORY.md", e)
        }
    }

    /**
     * 追加内容到长期记忆
     *
     * @param section 章节名称（例如 "## User Preferences"）
     * @param content 要追加的内容
     */
    suspend fun appendToMemory(section: String, content: String) = withContext(Dispatchers.IO) {
        try {
            val currentContent = readMemory()
            val newContent = if (currentContent.contains(section)) {
                // 在指定章节后追加
                currentContent.replace(section, "$section\n$content")
            } else {
                // 章节不存在，追加到末尾
                "$currentContent\n\n$section\n$content"
            }
            writeMemory(newContent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append to MEMORY.md", e)
        }
    }

    /**
     * 读取今天的日志
     *
     * @return 今天的日志内容
     */
    suspend fun getTodayLog(): String = withContext(Dispatchers.IO) {
        val today = DATE_FORMAT.format(Date())
        val logFile = File(memoryDir, "$today.md")
        try {
            if (logFile.exists()) {
                logFile.readText()
            } else {
                Log.d(TAG, "Today's log does not exist: $today.md")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read today's log", e)
            ""
        }
    }

    /**
     * 读取昨天的日志
     *
     * @return 昨天的日志内容
     */
    suspend fun getYesterdayLog(): String = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = DATE_FORMAT.format(calendar.time)
        val logFile = File(memoryDir, "$yesterday.md")
        try {
            if (logFile.exists()) {
                logFile.readText()
            } else {
                Log.d(TAG, "Yesterday's log does not exist: $yesterday.md")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read yesterday's log", e)
            ""
        }
    }

    /**
     * 追加内容到今天的日志
     *
     * @param content 要追加的内容
     */
    suspend fun appendToToday(content: String) = withContext(Dispatchers.IO) {
        val today = DATE_FORMAT.format(Date())
        val logFile = File(memoryDir, "$today.md")
        try {
            if (!logFile.exists()) {
                // 创建新日志文件
                val header = "# Daily Log - $today\n\n"
                logFile.writeText(header)
                Log.d(TAG, "Created new daily log: $today.md")
            }

            // 追加内容（带时间戳）
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val entry = "\n## [$timestamp]\n$content\n"
            logFile.appendText(entry)
            Log.d(TAG, "Appended to today's log: $today.md")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append to today's log", e)
        }
    }

    /**
     * 读取指定日期的日志
     *
     * @param date 日期字符串（yyyy-MM-dd）
     * @return 日志内容
     */
    suspend fun getLogByDate(date: String): String = withContext(Dispatchers.IO) {
        val logFile = File(memoryDir, "$date.md")
        try {
            if (logFile.exists()) {
                logFile.readText()
            } else {
                Log.d(TAG, "Log does not exist: $date.md")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log: $date.md", e)
            ""
        }
    }

    /**
     * 列出所有日志文件
     *
     * @return 日志文件列表（按日期倒序）
     */
    suspend fun listLogs(): List<String> = withContext(Dispatchers.IO) {
        try {
            memoryDir.listFiles { file ->
                file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md"))
            }?.map { it.nameWithoutExtension }?.sortedDescending() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list logs", e)
            emptyList()
        }
    }

    /**
     * 列出所有记忆文件（非日期文件）
     *
     * @return 记忆文件路径列表
     */
    suspend fun listMemoryFiles(): List<String> = withContext(Dispatchers.IO) {
        try {
            val files = mutableListOf<String>()

            // 添加根目录的 MEMORY.md
            if (memoryFile.exists()) {
                files.add(memoryFile.absolutePath)
            }

            // 添加 memory/ 目录下的非日期文件
            memoryDir.listFiles { file ->
                file.isFile &&
                file.name.endsWith(".md") &&
                !file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md"))
            }?.forEach { file ->
                files.add(file.absolutePath)
            }

            files
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list memory files", e)
            emptyList()
        }
    }

    /**
     * 创建 MEMORY.md 模板
     */
    private fun createMemoryTemplate(): String {
        val template = """
# Long-term Memory

This file stores long-term, curated memories that persist across sessions.

## User Preferences

<!-- User preferences, communication style, language preferences -->

## Application Knowledge

<!-- Common app package names, successful operation patterns -->

## Known Issues and Solutions

<!-- Problems encountered and their solutions -->

## Stable Coordinates

<!-- UI element coordinates if they are fixed -->

## Important Context

<!-- Other important context that should be remembered -->

---

Last updated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}
        """.trimIndent()

        try {
            memoryFile.writeText(template)
            Log.d(TAG, "Created MEMORY.md template")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create MEMORY.md template", e)
        }

        return template
    }

    /**
     * 清除超过指定天数的日志
     *
     * @param days 保留天数
     */
    suspend fun pruneOldLogs(days: Int) = withContext(Dispatchers.IO) {
        try {
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -days)
            }.time

            memoryDir.listFiles { file ->
                file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md"))
            }?.forEach { file ->
                try {
                    val fileDate = DATE_FORMAT.parse(file.nameWithoutExtension)
                    if (fileDate != null && fileDate.before(cutoffDate)) {
                        file.delete()
                        Log.d(TAG, "Pruned old log: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse date for: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune old logs", e)
        }
    }
}
