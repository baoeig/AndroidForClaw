package com.xiaomo.androidforclaw.accessibility.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.xiaomo.androidforclaw.accessibility.MediaProjectionHelper
import java.io.File

/**
 * Separate service for AIDL binding
 * This allows external apps to bind and control the accessibility service
 */
class AccessibilityBinderService : Service() {
    companion object {
        private const val TAG = "AccessibilityBinderService"
        var serviceInstance: PhoneAccessibilityService? = null
    }

    private lateinit var binder: AccessibilityBinder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AccessibilityBinderService created")

        // 初始化 MediaProjectionHelper 截图目录
        val screenshotDir = File(getExternalFilesDir(null), "Screenshots")
        MediaProjectionHelper.setScreenshotDirectory(screenshotDir)
        Log.d(TAG, "MediaProjectionHelper screenshot directory: ${screenshotDir.absolutePath}")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Binding AIDL interface")

        // Wait for accessibility service to be ready
        val accessibilityService = serviceInstance
        if (accessibilityService == null) {
            Log.w(TAG, "Accessibility service not ready yet")
            return null
        }

        binder = AccessibilityBinder(accessibilityService)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AccessibilityBinderService destroyed")
    }
}
