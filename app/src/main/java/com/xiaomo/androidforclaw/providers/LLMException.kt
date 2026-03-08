package com.xiaomo.androidforclaw.providers

/**
 * Legacy LLM API Exception
 */
class LLMException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
