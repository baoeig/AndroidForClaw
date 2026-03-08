package com.xiaomo.androidforclaw.agent.session

import android.util.Log
import com.xiaomo.androidforclaw.agent.memory.ContextCompressor
import com.xiaomo.androidforclaw.agent.memory.TokenEstimator
import com.xiaomo.androidforclaw.providers.LegacyMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Session Manager - 会话管理器
 * 对齐 OpenClaw 会话管理
 *
 * 存储格式 (OpenClaw Protocol):
 * - sessions.json: 元数据索引 {"agent:main:main": {"sessionId":"uuid", "updatedAt":1234567890, "sessionFile":"/path/to/uuid.jsonl", ...}}
 * - {sessionId}.jsonl: 消息历史 (JSONL, 每行一个事件)
 *
 * 职责:
 * 1. 管理对话历史记录
 * 2. 持久化会话数据 (JSONL 格式)
 * 3. 自动上下文压缩
 * 4. Token 预算管理
 * 5. 提供会话的创建、获取、保存、清除功能
 */
class SessionManager(
    private val workspace: File,
    private val contextCompressor: ContextCompressor? = null
) {
    companion object {
        private const val TAG = "SessionManager"
        private const val SESSIONS_DIR = "sessions"
        private const val SESSIONS_INDEX = "sessions.json"
        private const val AUTO_PRUNE_DAYS = 30        // 自动清理 30 天前的会话
    }

    private val gson: Gson = GsonBuilder().create()  // No pretty printing for JSONL
    private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()  // For sessions.json index

    private val sessionsDir: File = File(workspace, SESSIONS_DIR).apply {
        if (!exists()) {
            mkdirs()
            Log.d(TAG, "Created sessions directory: $absolutePath")
        }
    }

    private val indexFile: File = File(sessionsDir, SESSIONS_INDEX)

    // 内存缓存
    private val sessions = mutableMapOf<String, Session>()
    private val sessionIndex = mutableMapOf<String, SessionMetadata>()

    init {
        loadIndex()
    }

    /**
     * 获取或创建会话
     */
    fun getOrCreate(sessionKey: String): Session {
        return sessions.getOrPut(sessionKey) {
            Log.d(TAG, "Creating new session: $sessionKey")
            loadSession(sessionKey) ?: createNewSession(sessionKey)
        }
    }

    /**
     * 获取会话（如果不存在返回 null）
     */
    fun get(sessionKey: String): Session? {
        return sessions[sessionKey] ?: loadSession(sessionKey)
    }

    /**
     * 保存会话
     */
    fun save(session: Session) {
        val nowMs = System.currentTimeMillis()
        session.updatedAt = currentTimestamp()
        sessions[session.key] = session

        // 持久化到 JSONL 文件
        try {
            saveSessionMessages(session)

            // 更新索引
            val metadata = sessionIndex.getOrPut(session.key) {
                SessionMetadata(
                    sessionId = session.sessionId,
                    updatedAt = nowMs,
                    sessionFile = getSessionJSONLFile(session.sessionId).absolutePath,
                    compactionCount = session.compactionCount
                )
            }
            metadata.updatedAt = nowMs
            metadata.compactionCount = session.compactionCount
            saveIndex()

            Log.d(TAG, "Session saved: ${session.key}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session: ${session.key}", e)
        }
    }

    /**
     * 清除会话
     */
    fun clear(sessionKey: String) {
        val session = sessions.remove(sessionKey)
        if (session != null) {
            getSessionJSONLFile(session.sessionId).delete()
            sessionIndex.remove(sessionKey)
            saveIndex()
        }
        Log.d(TAG, "Session cleared: $sessionKey")
    }

    /**
     * 清除所有会话
     */
    fun clearAll() {
        sessions.clear()
        sessionIndex.clear()
        sessionsDir.listFiles()?.forEach {
            if (it.extension == "jsonl") {
                it.delete()
            }
        }
        indexFile.delete()
        Log.d(TAG, "All sessions cleared")
    }

    /**
     * 获取所有会话键 (仅返回新格式的 sessions)
     */
    fun getAllKeys(): List<String> {
        loadIndex()
        // 只返回索引中的 sessions (新格式),忽略旧的 .json 文件
        return sessionIndex.keys.toList()
    }

    /**
     * 检查并自动压缩会话
     *
     * @param session 会话
     * @return 是否执行了压缩
     */
    suspend fun compressIfNeeded(session: Session): Boolean = withContext(Dispatchers.IO) {
        if (contextCompressor == null) {
            return@withContext false
        }

        try {
            // 检查是否需要压缩
            if (!contextCompressor.needsCompaction(session.messages)) {
                return@withContext false
            }

            Log.d(TAG, "Auto-compressing session: ${session.key} (${session.messages.size} messages, ${session.getTokenCount()} tokens)")

            // 执行压缩
            val compressedMessages = contextCompressor.compress(session.messages)

            // 更新会话
            session.messages.clear()
            session.messages.addAll(compressedMessages)
            session.markCompacted()

            // 保存会话
            save(session)

            Log.d(TAG, "Session compressed: ${session.key} → ${session.messages.size} messages, ${session.getTokenCount()} tokens (compaction #${session.compactionCount})")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress session: ${session.key}", e)
            false
        }
    }

    /**
     * 自动清理旧会话
     *
     * @param days 清理多少天前的会话
     */
    suspend fun pruneOldSessions(days: Int = AUTO_PRUNE_DAYS): Unit = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

            var prunedCount = 0

            getAllKeys().forEach { key ->
                val session = loadSession(key)
                if (session != null) {
                    try {
                        val updatedDate = dateFormat.parse(session.updatedAt)
                        if (updatedDate != null && updatedDate.time < cutoffTime) {
                            clear(key)
                            prunedCount++
                            Log.d(TAG, "Pruned old session: $key (last updated: ${session.updatedAt})")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse date for session: $key", e)
                    }
                }
            }

            if (prunedCount > 0) {
                Log.d(TAG, "Pruned $prunedCount old sessions (older than $days days)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune old sessions", e)
        }
    }

    // ================ Private Helpers ================

    /**
     * 创建新会话
     */
    private fun createNewSession(sessionKey: String): Session {
        val sessionId = UUID.randomUUID().toString()
        val nowMs = System.currentTimeMillis()
        val timestamp = currentTimestamp()

        val session = Session(
            key = sessionKey,
            sessionId = sessionId,
            messages = mutableListOf(),
            createdAt = timestamp,
            updatedAt = timestamp
        )

        // 写入 JSONL header
        val jsonlFile = getSessionJSONLFile(sessionId)
        FileOutputStream(jsonlFile, false).use { out ->
            val header = mapOf(
                "type" to "session",
                "version" to 3,
                "id" to sessionId,
                "timestamp" to timestamp,
                "cwd" to workspace.absolutePath
            )
            out.write((gson.toJson(header) + "\n").toByteArray())
        }

        // 更新索引
        sessionIndex[sessionKey] = SessionMetadata(
            sessionId = sessionId,
            updatedAt = nowMs,
            sessionFile = jsonlFile.absolutePath,
            compactionCount = 0
        )
        saveIndex()

        return session
    }

    /**
     * 加载索引文件
     */
    private fun loadIndex() {
        if (!indexFile.exists()) {
            return
        }

        try {
            val json = indexFile.readText()
            val jsonObject = JsonParser.parseString(json).asJsonObject

            sessionIndex.clear()
            for ((key, value) in jsonObject.entrySet()) {
                val obj = value.asJsonObject
                sessionIndex[key] = SessionMetadata(
                    sessionId = obj.get("sessionId").asString,
                    updatedAt = obj.get("updatedAt").asLong,
                    sessionFile = obj.get("sessionFile").asString,
                    compactionCount = obj.get("compactionCount")?.asInt ?: 0
                )
            }

            Log.d(TAG, "Index loaded: ${sessionIndex.size} sessions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load index", e)
        }
    }

    /**
     * 保存索引文件
     */
    private fun saveIndex() {
        try {
            val jsonObject = JsonObject()
            for ((key, metadata) in sessionIndex) {
                val obj = JsonObject()
                obj.addProperty("sessionId", metadata.sessionId)
                obj.addProperty("updatedAt", metadata.updatedAt)
                obj.addProperty("sessionFile", metadata.sessionFile)
                obj.addProperty("compactionCount", metadata.compactionCount)
                jsonObject.add(key, obj)
            }

            Log.d(TAG, "💾 Saving index to: ${indexFile.absolutePath}")
            indexFile.writeText(gsonPretty.toJson(jsonObject))
            Log.d(TAG, "✅ Index saved: ${sessionIndex.size} sessions")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save index: ${e.message}", e)
        }
    }

    /**
     * 加载会话
     */
    private fun loadSession(sessionKey: String): Session? {
        val metadata = sessionIndex[sessionKey] ?: return null
        val jsonlFile = getSessionJSONLFile(metadata.sessionId)

        if (!jsonlFile.exists()) {
            Log.w(TAG, "Session JSONL file not found: ${metadata.sessionId}")
            return null
        }

        return try {
            val messages = mutableListOf<LegacyMessage>()
            var createdAt = currentTimestamp()
            var updatedAt = currentTimestamp()

            jsonlFile.forEachLine { line ->
                if (line.isBlank()) return@forEachLine

                val event = JsonParser.parseString(line).asJsonObject
                val type = event.get("type")?.asString ?: return@forEachLine

                when (type) {
                    "session" -> {
                        createdAt = event.get("timestamp")?.asString ?: createdAt
                    }
                    "message" -> {
                        val role = event.get("role")?.asString ?: return@forEachLine
                        val content = event.get("content")?.asString ?: ""

                        messages.add(LegacyMessage(
                            role = role,
                            content = content
                        ))
                    }
                }
            }

            val session = Session(
                key = sessionKey,
                sessionId = metadata.sessionId,
                messages = messages,
                createdAt = createdAt,
                updatedAt = updatedAt,
                compactionCount = metadata.compactionCount
            )

            Log.d(TAG, "Session loaded: $sessionKey (${messages.size} messages)")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session: $sessionKey", e)
            null
        }
    }

    /**
     * 保存会话消息到 JSONL
     */
    private fun saveSessionMessages(session: Session) {
        val jsonlFile = getSessionJSONLFile(session.sessionId)

        try {
            Log.d(TAG, "💾 Saving session messages to: ${jsonlFile.absolutePath}")

            // 重写整个文件
            FileOutputStream(jsonlFile, false).use { out ->
                // 1. Session header
                val header = mapOf(
                    "type" to "session",
                    "version" to 3,
                    "id" to session.sessionId,
                    "timestamp" to session.createdAt,
                    "cwd" to workspace.absolutePath
                )
                out.write((gson.toJson(header) + "\n").toByteArray())

                // 2. Messages
                for (msg in session.messages) {
                    val event = mapOf(
                        "type" to "message",
                        "id" to UUID.randomUUID().toString(),
                        "role" to msg.role,
                        "content" to msg.content,
                        "timestamp" to currentTimestamp()
                    )
                    out.write((gson.toJson(event) + "\n").toByteArray())
                }
            }

            Log.d(TAG, "✅ Session messages saved: ${session.messages.size} messages to ${jsonlFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save session messages: ${e.message}", e)
        }
    }

    private fun getSessionJSONLFile(sessionId: String): File {
        return File(sessionsDir, "$sessionId.jsonl")
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .format(Date())
    }
}

/**
 * Session - 会话数据
 */
data class Session(
    val key: String,
    val sessionId: String,                     // UUID (对齐 OpenClaw)
    var messages: MutableList<LegacyMessage>,
    var createdAt: String,
    var updatedAt: String,
    var metadata: MutableMap<String, Any?> = mutableMapOf(),
    var compactionCount: Int = 0,              // 压缩次数
    var totalTokens: Int = 0,                  // 总 token 数
    var totalTokensFresh: Boolean = false      // token 数据是否新鲜
) {
    /**
     * 添加消息
     */
    fun addMessage(message: LegacyMessage) {
        messages.add(message)
        totalTokensFresh = false  // 标记 token 计数为陈旧
    }

    /**
     * 获取最近的 N 条消息
     */
    fun getRecentMessages(count: Int): List<LegacyMessage> {
        return if (messages.size <= count) {
            messages.toList()
        } else {
            messages.takeLast(count)
        }
    }

    /**
     * 清除消息
     */
    fun clearMessages() {
        messages.clear()
        totalTokens = 0
        totalTokensFresh = true
    }

    /**
     * 获取消息数量
     */
    fun messageCount(): Int {
        return messages.size
    }

    /**
     * 更新 token 计数
     */
    fun updateTokenCount() {
        totalTokens = TokenEstimator.estimateMessagesTokens(messages)
        totalTokensFresh = true
    }

    /**
     * 获取 token 计数（如果不新鲜则重新计算）
     */
    fun getTokenCount(): Int {
        if (!totalTokensFresh) {
            updateTokenCount()
        }
        return totalTokens
    }

    /**
     * 标记已压缩
     */
    fun markCompacted() {
        compactionCount++
        totalTokensFresh = false
    }
}

/**
 * SessionMetadata - 会话元数据 (对齐 OpenClaw sessions.json)
 */
data class SessionMetadata(
    val sessionId: String,
    var updatedAt: Long,
    val sessionFile: String,
    var compactionCount: Int = 0
)
