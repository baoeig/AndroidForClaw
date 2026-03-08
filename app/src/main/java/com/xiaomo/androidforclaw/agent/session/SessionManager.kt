package com.xiaomo.androidforclaw.agent.session

import android.util.Log
import com.xiaomo.androidforclaw.agent.memory.ContextCompressor
import com.xiaomo.androidforclaw.agent.memory.TokenEstimator
import com.xiaomo.androidforclaw.providers.LegacyMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Session Manager - 会话管理器
 * 对齐 OpenClaw 会话管理
 *
 * 职责:
 * 1. 管理对话历史记录
 * 2. 持久化会话数据
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
        private const val MAX_HISTORY_MESSAGES = 100  // 最大历史消息数（弃用，使用 token 限制）
        private const val AUTO_PRUNE_DAYS = 30        // 自动清理 30 天前的会话
    }

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val sessionsDir: File = File(workspace, SESSIONS_DIR).apply {
        if (!exists()) {
            mkdirs()
            Log.d(TAG, "Created sessions directory: $absolutePath")
        }
    }

    // 内存缓存
    private val sessions = mutableMapOf<String, Session>()

    /**
     * 获取或创建会话
     */
    fun getOrCreate(sessionKey: String): Session {
        return sessions.getOrPut(sessionKey) {
            Log.d(TAG, "Creating new session: $sessionKey")
            loadSession(sessionKey) ?: Session(
                key = sessionKey,
                messages = mutableListOf(),
                createdAt = currentTimestamp(),
                updatedAt = currentTimestamp()
            )
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
        session.updatedAt = currentTimestamp()
        sessions[session.key] = session

        // 持久化到文件
        try {
            val sessionFile = getSessionFile(session.key)
            val json = gson.toJson(session)
            sessionFile.writeText(json)
            Log.d(TAG, "Session saved: ${session.key}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session: ${session.key}", e)
        }
    }

    /**
     * 清除会话
     */
    fun clear(sessionKey: String) {
        sessions.remove(sessionKey)
        getSessionFile(sessionKey).delete()
        Log.d(TAG, "Session cleared: $sessionKey")
    }

    /**
     * 清除所有会话
     */
    fun clearAll() {
        sessions.clear()
        sessionsDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "All sessions cleared")
    }

    /**
     * 获取所有会话键
     */
    fun getAllKeys(): List<String> {
        return sessionsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
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

    private fun loadSession(sessionKey: String): Session? {
        val sessionFile = getSessionFile(sessionKey)
        if (!sessionFile.exists()) {
            return null
        }

        return try {
            val json = sessionFile.readText()
            val session = gson.fromJson(json, Session::class.java)
            Log.d(TAG, "Session loaded: $sessionKey (${session.messages.size} messages)")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session: $sessionKey", e)
            null
        }
    }

    private fun getSessionFile(sessionKey: String): File {
        val safeKey = sessionKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(sessionsDir, "$safeKey.json")
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
