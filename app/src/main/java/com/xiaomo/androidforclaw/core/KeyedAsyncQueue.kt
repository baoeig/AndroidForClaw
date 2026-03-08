package com.xiaomo.androidforclaw.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 按 key 串行执行异步任务的队列
 *
 * 对齐 OpenClaw 的 KeyedAsyncQueue 实现：
 * - 同一个 key 的任务串行执行
 * - 不同 key 的任务可以并发执行
 * - 保证消息顺序（同一个 session 的消息按接收顺序处理）
 *
 * 参考: openclaw/src/plugin-sdk/keyed-async-queue.ts
 */
class KeyedAsyncQueue {

    // 存储每个 key 的最后一个任务（tail）
    private val tails = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /**
     * 将任务加入队列
     *
     * @param key 队列键（通常是 sessionId 或 channelId）
     * @param task 要执行的任务
     * @return 任务执行结果
     */
    suspend fun <T> enqueue(key: String, task: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            // 获取当前队列的尾部任务
            val previousTail = tails[key]

            // 创建新的 tail
            val newTail = CompletableDeferred<Unit>()

            // 更新 tail（在执行前更新，避免竞态条件）
            tails[key] = newTail

            try {
                // 等待上一个任务完成（忽略错误）
                try {
                    previousTail?.await()
                } catch (e: Exception) {
                    // 忽略前一个任务的错误
                }

                // 执行当前任务
                val result = task()

                // 标记当前任务完成
                newTail.complete(Unit)

                // 清理已完成的 tail
                if (tails[key] == newTail) {
                    tails.remove(key)
                }

                result
            } catch (e: Exception) {
                // 标记当前任务完成（即使失败）
                newTail.complete(Unit)

                // 清理已完成的 tail
                if (tails[key] == newTail) {
                    tails.remove(key)
                }

                throw e
            }
        }
    }

    /**
     * 获取队列中待处理的任务数量
     */
    fun getPendingCount(): Int = tails.size

    /**
     * 检查指定 key 是否有正在处理的任务
     */
    fun isProcessing(key: String): Boolean = tails.containsKey(key)

    /**
     * 清空所有队列（用于测试或重置）
     */
    fun clear() {
        tails.clear()
    }
}
