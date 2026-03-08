package com.xiaomo.androidforclaw.channel

/**
 * Channel Definition - 按照 OpenClaw 架构定义 Android App Channel
 *
 * OpenClaw Channel 核心概念:
 * - Channel: 通信渠道（WhatsApp, Telegram, Discord, etc.）
 * - Account: 渠道内的账号（多账号支持）
 * - Session: 会话实例（与用户/设备的对话）
 * - Capabilities: 渠道能力（polls, threads, media, etc.）
 *
 * Android App Channel 特点:
 * - 设备控制型渠道（非社交消息渠道）
 * - 单设备直接执行模式（无群组、无线程）
 * - 工具密集型（tap, swipe, screenshot, etc.）
 * - 认证方式: ADB/Accessibility 配对（非 token）
 */

/**
 * Channel ID - 渠道唯一标识
 */
const val CHANNEL_ID = "android-app"

/**
 * Channel Meta - 渠道元数据
 */
data class ChannelMeta(
    val label: String,               // 显示名称
    val emoji: String,               // 图标 emoji
    val description: String,         // 描述
    val systemImage: String? = null  // 系统图标路径
)

val CHANNEL_META = ChannelMeta(
    label = "Android App",
    emoji = "📱",
    description = "AndroidForClaw Android device control channel"
)

/**
 * Channel Capabilities - 渠道能力定义（参考 OpenClaw）
 */
data class ChannelCapabilities(
    val chatTypes: List<ChatType>,    // 支持的聊天类型
    val polls: Boolean = false,       // 投票
    val reactions: Boolean = false,   // 反应/表情
    val edit: Boolean = false,        // 编辑消息
    val unsend: Boolean = false,      // 撤回消息
    val reply: Boolean = false,       // 回复消息
    val effects: Boolean = false,     // 视觉效果
    val groupManagement: Boolean = false,  // 群组管理
    val threads: Boolean = false,     // 线程/嵌套对话
    val media: Boolean = false,       // 媒体（图片/文件）
    val nativeCommands: Boolean = false,   // 原生命令
    val blockStreaming: Boolean = false    // 阻塞流式响应
) {
    enum class ChatType {
        DIRECT,      // 直接对话
        GROUP,       // 群组
        CHANNEL,     // 频道
        THREAD       // 线程
    }
}

/**
 * Android App Channel 能力配置
 *
 * 对比 OpenClaw 其他渠道:
 * - WhatsApp: direct, group, polls, reactions, media
 * - Telegram: direct, group, channel, thread, polls, reactions, media, nativeCommands, blockStreaming
 * - Discord: direct, channel, thread, polls, reactions, media, nativeCommands, blockStreaming
 * - Slack: direct, channel, thread, reactions, media, nativeCommands, blockStreaming
 * - Signal: direct, group, reactions, media
 *
 * Android App: 最小化能力（设备控制专用）
 */
val ANDROID_CHANNEL_CAPABILITIES = ChannelCapabilities(
    chatTypes = listOf(ChannelCapabilities.ChatType.DIRECT),  // 仅直接执行
    polls = false,
    reactions = false,
    edit = false,
    unsend = false,
    reply = false,
    effects = false,
    groupManagement = false,
    threads = false,
    media = true,                    // ✓ 截图/录屏
    nativeCommands = true,           // ✓ 设备操作命令
    blockStreaming = true            // ✓ 阻塞流式响应（等待完整结果）
)

/**
 * Channel Account - 账号配置（对应 OpenClaw 的 ChannelAccountSnapshot）
 */
data class ChannelAccount(
    val accountId: String,                     // 账号 ID（Android: device-{uuid}）
    val name: String? = null,                  // 账号名称（设备名称）
    val enabled: Boolean = true,               // 是否启用
    val configured: Boolean = false,           // 是否已配置
    val linked: Boolean = false,               // 是否已连接
    val running: Boolean = false,              // 是否运行中
    val connected: Boolean = false,            // 是否已连接
    val reconnectAttempts: Int = 0,            // 重连尝试次数
    val lastConnectedAt: Long? = null,         // 最后连接时间
    val lastError: String? = null,             // 最后错误
    val lastStartAt: Long? = null,             // 最后启动时间
    val lastStopAt: Long? = null,              // 最后停止时间
    val lastInboundAt: Long? = null,           // 最后接收消息时间
    val lastOutboundAt: Long? = null,          // 最后发送消息时间
    val lastProbeAt: Long? = null,             // 最后探测时间

    // Android 特有字段
    val deviceId: String? = null,              // 设备 ID
    val deviceModel: String? = null,           // 设备型号
    val androidVersion: String? = null,        // Android 版本
    val apiLevel: Int? = null,                 // API Level
    val architecture: String? = null,          // CPU 架构
    val accessibilityEnabled: Boolean = false, // 无障碍服务状态
    val overlayPermission: Boolean = false,    // 悬浮窗权限
    val mediaProjection: Boolean = false       // 录屏权限
)

/**
 * Channel Status - 渠道状态快照（对应 OpenClaw 的 ChannelsStatusSnapshot）
 */
data class ChannelStatus(
    val timestamp: Long = System.currentTimeMillis(),
    val channelId: String = CHANNEL_ID,
    val meta: ChannelMeta = CHANNEL_META,
    val capabilities: ChannelCapabilities = ANDROID_CHANNEL_CAPABILITIES,
    val accounts: List<ChannelAccount> = emptyList(),
    val defaultAccountId: String? = null
)

/**
 * Agent Prompt Hints - 系统提示词提示（对应 OpenClaw 的 agentPrompt.messageToolHints）
 */
object AndroidChannelPromptHints {

    /**
     * 生成渠道特定的系统提示词提示
     */
    fun getMessageToolHints(account: ChannelAccount? = null): List<String> {
        val hints = mutableListOf<String>()

        // 基础提示
        hints.add("You are running on an Android device with direct access to device controls")
        hints.add("Use tools to observe and control the device:")
        hints.add("  - Observation: screenshot, get_ui_tree")
        hints.add("  - Actions: tap, swipe, type, long_press")
        hints.add("  - Navigation: home, back, open_app")
        hints.add("  - System: wait, stop, notification")

        // 设备特定提示
        if (account != null) {
            hints.add("")
            hints.add("Device Information:")
            hints.add("  - Model: ${account.deviceModel ?: "Unknown"}")
            hints.add("  - Android: ${account.androidVersion ?: "Unknown"} (API ${account.apiLevel ?: "Unknown"})")
            hints.add("  - Architecture: ${account.architecture ?: "Unknown"}")

            // 权限状态提示
            hints.add("")
            hints.add("Permissions Status:")
            hints.add("  - Accessibility: ${if (account.accessibilityEnabled) "✓ Enabled" else "✗ Disabled"}")
            hints.add("  - Overlay: ${if (account.overlayPermission) "✓ Granted" else "✗ Not granted"}")
            hints.add("  - Screen Capture: ${if (account.mediaProjection) "✓ Granted" else "✗ Not granted"}")
        }

        // 最佳实践提示
        hints.add("")
        hints.add("Best Practices:")
        hints.add("  - Always screenshot before and after actions")
        hints.add("  - Verify state changes after operations")
        hints.add("  - Use wait() for loading states")
        hints.add("  - Try alternative approaches when blocked")

        return hints
    }

    /**
     * 生成 Runtime Section 的 Channel 信息
     */
    fun getRuntimeChannelInfo(account: ChannelAccount? = null): String {
        return buildString {
            appendLine("channel: $CHANNEL_ID")
            appendLine("channel_label: ${CHANNEL_META.label}")
            if (account != null) {
                appendLine("account_id: ${account.accountId}")
                appendLine("device_id: ${account.deviceId ?: "unknown"}")
                appendLine("device_model: ${account.deviceModel ?: "unknown"}")
            }
        }.trim()
    }
}

/**
 * Channel Config - 渠道配置
 */
data class ChannelConfig(
    val enabled: Boolean = true,
    val defaultAccount: String? = null,
    val accounts: Map<String, ChannelAccount> = emptyMap()
)
