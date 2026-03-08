package com.xiaomo.androidforclaw.agent.context

/**
 * 上下文错误检测工具
 * 对齐 OpenClaw 的 errors.ts 实现
 */
object ContextErrors {

    /**
     * 严格检测上下文溢出错误
     * 参考: OpenClaw/src/agents/pi-embedded-helpers/errors.ts isContextOverflowError()
     */
    fun isContextOverflowError(errorMessage: String?): Boolean {
        if (errorMessage.isNullOrBlank()) return false

        val msg = errorMessage.lowercase()

        // Anthropic API 特定错误
        if (msg.contains("request_too_large")) return true
        if (msg.contains("request size exceeds")) return true

        // 通用上下文窗口错误
        if (msg.contains("context window")) return true
        if (msg.contains("context length")) return true
        if (msg.contains("maximum context length")) return true

        // Prompt 长度错误
        if (msg.contains("prompt is too long")) return true
        if (msg.contains("exceeds model context window")) return true
        if (msg.contains("model token limit")) return true

        // 显式上下文溢出标记
        if (msg.contains("context overflow:")) return true
        if (msg.contains("exceed context limit")) return true

        // 中文错误消息
        if (msg.contains("上下文过长")) return true
        if (msg.contains("上下文超出")) return true
        if (msg.contains("请压缩上下文")) return true

        // Token 数量相关
        if (msg.contains("tokens") && (
            msg.contains("exceed") ||
            msg.contains("too many") ||
            msg.contains("limit")
        )) return true

        // HTTP 413 Payload Too Large
        if (msg.contains("413") || msg.contains("payload too large")) return true

        return false
    }

    /**
     * 启发式检测上下文溢出（更宽松）
     * 参考: OpenClaw/src/agents/pi-embedded-helpers/errors.ts isLikelyContextOverflowError()
     */
    fun isLikelyContextOverflowError(errorMessage: String?): Boolean {
        if (errorMessage.isNullOrBlank()) return false

        val msg = errorMessage.lowercase()

        // 先检查严格匹配
        if (isContextOverflowError(errorMessage)) return true

        // 排除其他错误类型
        if (isRateLimitError(msg)) return false
        if (isAuthError(msg)) return false
        if (isBillingError(msg)) return false

        // 正则匹配
        val contextPattern = Regex(
            "context.*(overflow|too|exceed|limit)|" +
            "prompt.*(too|exceed|limit)|" +
            "window.*(exceed|limit)",
            RegexOption.IGNORE_CASE
        )

        return contextPattern.containsMatchIn(msg)
    }

    /**
     * 检测 Compaction 失败错误
     */
    fun isCompactionFailureError(errorMessage: String?): Boolean {
        if (errorMessage.isNullOrBlank()) return false

        val msg = errorMessage.lowercase()

        return (msg.contains("summarization failed") ||
                msg.contains("auto-compaction") ||
                msg.contains("compaction failed")) &&
                isLikelyContextOverflowError(errorMessage)
    }

    /**
     * 检测速率限制错误
     */
    private fun isRateLimitError(msg: String): Boolean {
        return msg.contains("rate limit") ||
               msg.contains("too many requests") ||
               msg.contains("quota exceeded") ||
               msg.contains("tokens per minute")
    }

    /**
     * 检测认证错误
     */
    private fun isAuthError(msg: String): Boolean {
        return msg.contains("unauthorized") ||
               msg.contains("authentication") ||
               msg.contains("invalid api key") ||
               msg.contains("api key")
    }

    /**
     * 检测计费错误
     */
    private fun isBillingError(msg: String): Boolean {
        return msg.contains("insufficient") ||
               msg.contains("balance") ||
               msg.contains("billing") ||
               msg.contains("payment")
    }

    /**
     * 从异常中提取错误消息
     */
    fun extractErrorMessage(exception: Throwable): String {
        val message = exception.message ?: ""
        val cause = exception.cause?.message ?: ""
        return "$message $cause"
    }
}
