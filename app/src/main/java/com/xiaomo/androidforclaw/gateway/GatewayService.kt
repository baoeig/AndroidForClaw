package com.xiaomo.androidforclaw.gateway

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import java.io.IOException

/**
 * Gateway Service - WebSocket RPC 服务
 * 
 * 功能：
 * - 提供 WebSocket 连接
 * - RPC 接口（agent, agent.wait, health）
 * - Session 管理
 * - 远程控制能力
 * 
 * 参考：OpenClaw Gateway 架构
 */
class GatewayService(port: Int = 8765) : NanoWSD(null, port) {  // null = 监听所有网络接口 (0.0.0.0)
    
    companion object {
        private const val TAG = "GatewayService"
    }

    private val gson = Gson()
    private val sessions = mutableMapOf<String, GatewaySession>()
    private var agentHandler: AgentHandler? = null

    /**
     * 设置 Agent 处理器
     */
    fun setAgentHandler(handler: AgentHandler) {
        this.agentHandler = handler
    }

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return GatewayWebSocket(handshake)
    }

    /**
     * WebSocket 连接处理
     */
    inner class GatewayWebSocket(handshake: IHTTPSession) : WebSocket(handshake) {
        
        private var sessionId: String? = null

        override fun onOpen() {
            sessionId = generateSessionId()
            val session = GatewaySession(sessionId!!, this)
            sessions[sessionId!!] = session
            
            Log.i(TAG, "✅ WebSocket 连接建立: session=$sessionId")
            
            // 发送欢迎消息
            sendMessage(JsonObject().apply {
                addProperty("type", "connected")
                addProperty("sessionId", sessionId)
                addProperty("message", "Welcome to AndroidForClaw Gateway")
            })
        }

        override fun onClose(
            code: WebSocketFrame.CloseCode,
            reason: String?,
            initiatedByRemote: Boolean
        ) {
            sessionId?.let { sessions.remove(it) }
            Log.i(TAG, "❌ WebSocket 连接关闭: session=$sessionId, reason=$reason")
        }

        override fun onMessage(message: WebSocketFrame) {
            try {
                val text = message.textPayload
                Log.d(TAG, "📥 收到消息: $text")

                val request: RpcRequest = gson.fromJson(text, RpcRequest::class.java)
                handleRpcRequest(request)
                
            } catch (e: Exception) {
                Log.e(TAG, "处理消息失败", e)
                sendError("Invalid request: ${e.message}")
            }
        }

        override fun onPong(pong: WebSocketFrame) {
            // 心跳响应
        }

        override fun onException(exception: IOException) {
            Log.e(TAG, "WebSocket 异常", exception)
        }

        /**
         * 处理 RPC 请求
         */
        private fun handleRpcRequest(request: RpcRequest) {
            when (request.method) {
                "agent" -> handleAgentRequest(request)
                "agent.wait" -> handleAgentWaitRequest(request)
                "health" -> handleHealthRequest(request)
                "session.list" -> handleSessionListRequest(request)
                "session.reset" -> handleSessionResetRequest(request)
                "session.listAll" -> handleSessionListAllRequest(request)
                else -> sendError("Unknown method: ${request.method}")
            }
        }

        /**
         * agent() - 执行 Agent 任务
         */
        private fun handleAgentRequest(request: RpcRequest) {
            val params = request.params ?: run {
                sendError("Missing params")
                return
            }

            val userMessage = params.message ?: run {
                sendError("Missing message")
                return
            }

            val systemPrompt = params.systemPrompt
            val tools = params.tools
            val maxIterations = params.maxIterations ?: 20

            // 🆔 支持指定 sessionId 来切换到其他 channel 的 session
            // 如果 params 中指定了 sessionId，使用指定的；否则使用当前 WebSocket 的 sessionId
            val targetSessionId = params.sessionId ?: sessionId!!

            Log.d(TAG, "🆔 [Agent Request] Target Session: $targetSessionId")
            if (params.sessionId != null) {
                Log.d(TAG, "   ↳ 切换到外部 session: ${params.sessionId}")
            } else {
                Log.d(TAG, "   ↳ 使用当前 WebSocket session")
            }

            // 异步执行 Agent
            Thread {
                try {
                    agentHandler?.executeAgent(
                        sessionId = targetSessionId,
                        userMessage = userMessage,
                        systemPrompt = systemPrompt,
                        tools = tools,
                        maxIterations = maxIterations,
                        progressCallback = { progress ->
                            // 发送进度更新（在新线程避免 NetworkOnMainThreadException）
                            Thread {
                                try {
                                    sendMessage(JsonObject().apply {
                                        addProperty("type", "progress")
                                        addProperty("requestId", request.id)
                                        add("data", gson.toJsonTree(progress))
                                    })
                                } catch (e: Exception) {
                                    Log.w(TAG, "发送进度失败: ${e.message}")
                                }
                            }.start()
                        },
                        completeCallback = { result ->
                            // 发送完成结果（在新线程避免 NetworkOnMainThreadException）
                            Thread {
                                try {
                                    sendResponse(request.id, result)
                                } catch (e: Exception) {
                                    Log.w(TAG, "发送结果失败: ${e.message}")
                                }
                            }.start()
                        }
                    )
                } catch (e: Exception) {
                    sendError("Agent execution failed: ${e.message}", request.id)
                }
            }.start()
        }

        /**
         * agent.wait() - 等待 Agent 完成
         */
        private fun handleAgentWaitRequest(request: RpcRequest) {
            val params = request.params ?: run {
                sendError("Missing params")
                return
            }

            val runId = params.runId ?: run {
                sendError("Missing runId")
                return
            }

            // TODO: 实现 agent.wait 逻辑
            sendResponse(request.id, mapOf(
                "status" to "completed",
                "runId" to runId
            ))
        }

        /**
         * health() - 健康检查
         */
        private fun handleHealthRequest(request: RpcRequest) {
            sendResponse(request.id, mapOf(
                "status" to "healthy",
                "timestamp" to System.currentTimeMillis(),
                "sessions" to sessions.size
            ))
        }

        /**
         * session.list() - 列出所有会话（包括 channel 创建的）
         */
        private fun handleSessionListRequest(request: RpcRequest) {
            try {
                val sessionManager = com.xiaomo.androidforclaw.core.MainEntryNew.getSessionManager()
                if (sessionManager == null) {
                    // 如果 SessionManager 未初始化，只返回 WebSocket sessions
                    val sessionList = sessions.keys.map { mapOf("id" to it) }
                    sendResponse(request.id, mapOf("sessions" to sessionList, "total" to sessionList.size))
                    return
                }

                // 获取所有 sessions（飞书、Discord、WebSocket）
                val allKeys = sessionManager.getAllKeys()
                val sessionList = allKeys.map { key ->
                    val session = sessionManager.get(key)
                    mapOf(
                        "id" to key,
                        "messageCount" to (session?.messageCount() ?: 0),
                        "createdAt" to (session?.createdAt ?: ""),
                        "updatedAt" to (session?.updatedAt ?: ""),
                        "type" to when {
                            key.startsWith("discord_") -> "discord"
                            key.contains("_p2p") || key.contains("_group") -> "feishu"
                            key.startsWith("session_") -> "websocket"
                            else -> "other"
                        }
                    )
                }

                sendResponse(request.id, mapOf(
                    "sessions" to sessionList,
                    "total" to sessionList.size
                ))

                Log.d(TAG, "📋 [Session List] 返回 ${sessionList.size} 个会话")

            } catch (e: Exception) {
                Log.e(TAG, "列出会话失败", e)
                sendError("Failed to list sessions: ${e.message}", request.id)
            }
        }

        /**
         * session.reset() - 重置会话
         */
        private fun handleSessionResetRequest(request: RpcRequest) {
            val params = request.params
            val targetSessionId = params?.sessionId ?: sessionId

            targetSessionId?.let {
                sessions[it]?.reset()
                sendResponse(request.id, mapOf("success" to true))
            } ?: sendError("Session not found")
        }

        /**
         * session.listAll() - 列出所有会话（包括 channel 创建的）
         */
        private fun handleSessionListAllRequest(request: RpcRequest) {
            try {
                val sessionManager = com.xiaomo.androidforclaw.core.MainEntryNew.getSessionManager()
                if (sessionManager == null) {
                    sendResponse(request.id, mapOf(
                        "sessions" to emptyList<Map<String, Any>>(),
                        "total" to 0
                    ))
                    return
                }

                val allKeys = sessionManager.getAllKeys()
                val sessionList = allKeys.map { key ->
                    val session = sessionManager.get(key)
                    mapOf(
                        "id" to key,
                        "messageCount" to (session?.messageCount() ?: 0),
                        "createdAt" to (session?.createdAt ?: ""),
                        "updatedAt" to (session?.updatedAt ?: ""),
                        "type" to when {
                            key.startsWith("discord_") -> "discord"
                            key.contains("_p2p") || key.contains("_group") -> "feishu"
                            else -> "other"
                        }
                    )
                }

                sendResponse(request.id, mapOf(
                    "sessions" to sessionList,
                    "total" to sessionList.size
                ))

                Log.d(TAG, "📋 [Session List] 返回 ${sessionList.size} 个会话")

            } catch (e: Exception) {
                Log.e(TAG, "列出会话失败", e)
                sendError("Failed to list sessions: ${e.message}", request.id)
            }
        }

        /**
         * 发送响应
         */
        private fun sendResponse(requestId: String?, data: Any) {
            sendMessage(JsonObject().apply {
                addProperty("type", "response")
                requestId?.let { addProperty("id", it) }
                add("data", gson.toJsonTree(data))
            })
        }

        /**
         * 发送错误
         */
        private fun sendError(message: String, requestId: String? = null) {
            sendMessage(JsonObject().apply {
                addProperty("type", "error")
                requestId?.let { addProperty("id", it) }
                addProperty("message", message)
            })
        }

        /**
         * 发送消息
         */
        private fun sendMessage(json: JsonObject) {
            try {
                send(gson.toJson(json))
            } catch (e: IOException) {
                Log.e(TAG, "发送消息失败", e)
            }
        }
    }

    /**
     * 生成会话 ID
     */
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * RPC 请求
 */
data class RpcRequest(
    val id: String?,
    val method: String,
    val params: RpcParams?
)

/**
 * RPC 参数
 */
data class RpcParams(
    val message: String?,
    val systemPrompt: String?,
    val tools: List<Any>?,
    val maxIterations: Int?,
    val runId: String?,
    val sessionId: String?
)

/**
 * Gateway Session
 */
data class GatewaySession(
    val id: String,
    val webSocket: NanoWSD.WebSocket,
    var lastActivity: Long = System.currentTimeMillis()
) {
    fun reset() {
        lastActivity = System.currentTimeMillis()
    }
}

/**
 * Agent 处理器接口
 */
interface AgentHandler {
    fun executeAgent(
        sessionId: String,
        userMessage: String,
        systemPrompt: String?,
        tools: List<Any>?,
        maxIterations: Int,
        progressCallback: (Map<String, Any>) -> Unit,
        completeCallback: (Map<String, Any>) -> Unit
    )
}
