package com.xiaomo.androidforclaw.core

import android.content.Context
import android.util.Log
import java.io.File
import java.util.UUID

/**
 * AndroidForClaw 初始化
 * 完全对齐 OpenClaw 的目录结构和文件
 *
 * OpenClaw 目录结构:
 * ~/.openclaw/
 * ├── .builtin-mimo-provider
 * ├── .device-id
 * ├── agents/
 * │   └── main/
 * │       └── sessions/
 * │           └── sessions.json
 * ├── app.log
 * ├── canvas/
 * │   └── index.html
 * ├── config-backups/
 * ├── cron/
 * │   └── jobs.json
 * ├── devices/
 * │   ├── paired.json
 * │   └── pending.json
 * ├── gateway.log
 * ├── openclaw.json
 * ├── openclaw.last-known-good.json
 * ├── skills/
 * ├── update-check.json
 * └── workspace/
 */
object AndroidForClawInit {

    private const val TAG = "AndroidForClawInit"
    private const val BASE_DIR = "/sdcard/.androidforclaw"

    /**
     * 初始化所有目录和文件
     */
    fun initialize(context: Context) {
        try {
            Log.i(TAG, "========================================")
            Log.i(TAG, "初始化 AndroidForClaw 目录结构")
            Log.i(TAG, "========================================")

            // 1. 创建根目录
            val baseDir = File(BASE_DIR)
            if (!baseDir.exists()) {
                baseDir.mkdirs()
                Log.i(TAG, "✅ 创建根目录: $BASE_DIR")
            }

            // 2. 创建 .device-id
            initDeviceId()

            // 3. 创建 agents/main/sessions/
            initAgentsDirectory()

            // 4. 创建 canvas/
            initCanvasDirectory()

            // 5. 创建 config-backups/
            File(BASE_DIR, "config-backups").mkdirs()
            Log.d(TAG, "✅ 创建 config-backups/")

            // 6. 创建 cron/
            initCronDirectory()

            // 7. 创建 devices/
            initDevicesDirectory()

            // 8. 创建 skills/
            File(BASE_DIR, "skills").mkdirs()
            Log.d(TAG, "✅ 创建 skills/")

            // 9. 创建 workspace/
            File(BASE_DIR, "workspace").apply {
                mkdirs()
                // workspace 子目录
                File(this, "skills").mkdirs()
            }
            Log.d(TAG, "✅ 创建 workspace/")

            // 10. 创建 update-check.json
            initUpdateCheck()

            // 11. 创建日志文件占位符
            initLogFiles()

            // 12. openclaw.json 和 openclaw.last-known-good.json 由 ConfigLoader 处理

            Log.i(TAG, "========================================")
            Log.i(TAG, "✅ AndroidForClaw 初始化完成")
            Log.i(TAG, "========================================")
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
        }
    }

    /**
     * 初始化 .device-id
     */
    private fun initDeviceId() {
        val deviceIdFile = File(BASE_DIR, ".device-id")
        if (!deviceIdFile.exists()) {
            val deviceId = UUID.randomUUID().toString()
            deviceIdFile.writeText(deviceId)
            Log.i(TAG, "✅ 创建 .device-id: $deviceId")
        } else {
            val deviceId = deviceIdFile.readText().trim()
            Log.d(TAG, "Device ID: $deviceId")
        }
    }

    /**
     * 初始化 agents/main/sessions/
     */
    private fun initAgentsDirectory() {
        val sessionsDir = File(BASE_DIR, "agents/main/sessions")
        sessionsDir.mkdirs()
        Log.d(TAG, "✅ 创建 agents/main/sessions/")

        // 创建 sessions.json 索引文件
        val sessionsIndex = File(sessionsDir, "sessions.json")
        if (!sessionsIndex.exists()) {
            sessionsIndex.writeText("[]")
            Log.d(TAG, "✅ 创建 sessions.json")
        }
    }

    /**
     * 初始化 canvas/
     */
    private fun initCanvasDirectory() {
        val canvasDir = File(BASE_DIR, "canvas")
        canvasDir.mkdirs()
        Log.d(TAG, "✅ 创建 canvas/")

        // 创建 index.html 占位符
        val indexHtml = File(canvasDir, "index.html")
        if (!indexHtml.exists()) {
            indexHtml.writeText("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>AndroidForClaw Canvas</title>
                </head>
                <body>
                    <h1>AndroidForClaw Canvas</h1>
                    <p>Agent workspace canvas placeholder</p>
                </body>
                </html>
            """.trimIndent())
            Log.d(TAG, "✅ 创建 canvas/index.html")
        }
    }

    /**
     * 初始化 cron/
     */
    private fun initCronDirectory() {
        val cronDir = File(BASE_DIR, "cron")
        cronDir.mkdirs()
        Log.d(TAG, "✅ 创建 cron/")

        // 创建 jobs.json
        val jobsFile = File(cronDir, "jobs.json")
        if (!jobsFile.exists()) {
            jobsFile.writeText("[]")
            Log.d(TAG, "✅ 创建 cron/jobs.json")
        }
    }

    /**
     * 初始化 devices/
     */
    private fun initDevicesDirectory() {
        val devicesDir = File(BASE_DIR, "devices")
        devicesDir.mkdirs()
        Log.d(TAG, "✅ 创建 devices/")

        // 创建 paired.json
        val pairedFile = File(devicesDir, "paired.json")
        if (!pairedFile.exists()) {
            pairedFile.writeText("{}")
            Log.d(TAG, "✅ 创建 devices/paired.json")
        }

        // 创建 pending.json
        val pendingFile = File(devicesDir, "pending.json")
        if (!pendingFile.exists()) {
            pendingFile.writeText("[]")
            Log.d(TAG, "✅ 创建 devices/pending.json")
        }
    }

    /**
     * 初始化 update-check.json
     */
    private fun initUpdateCheck() {
        val updateCheckFile = File(BASE_DIR, "update-check.json")
        if (!updateCheckFile.exists()) {
            updateCheckFile.writeText("""{"lastCheck":"never","version":"0.0.0"}""")
            Log.d(TAG, "✅ 创建 update-check.json")
        }
    }

    /**
     * 初始化日志文件占位符
     */
    private fun initLogFiles() {
        val appLog = File(BASE_DIR, "app.log")
        if (!appLog.exists()) {
            appLog.createNewFile()
            Log.d(TAG, "✅ 创建 app.log")
        }

        val gatewayLog = File(BASE_DIR, "gateway.log")
        if (!gatewayLog.exists()) {
            gatewayLog.createNewFile()
            Log.d(TAG, "✅ 创建 gateway.log")
        }
    }

    /**
     * 获取 Device ID
     */
    fun getDeviceId(): String {
        val deviceIdFile = File(BASE_DIR, ".device-id")
        return if (deviceIdFile.exists()) {
            deviceIdFile.readText().trim()
        } else {
            "unknown"
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        val baseDir = File(BASE_DIR)
        val deviceIdFile = File(BASE_DIR, ".device-id")
        return baseDir.exists() && deviceIdFile.exists()
    }
}
