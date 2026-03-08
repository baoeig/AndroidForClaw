package com.xiaomo.androidforclaw.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.draco.ladb.R
// import com.xiaomo.androidforclaw.ui.activity.QuickJSTestActivity

/**
 * 前台服务 - 用于保活进程
 *
 * 保活机制：
 * 1. 前台服务通知（降低被杀风险）
 * 2. START_STICKY 重启策略
 * 3. 通知点击跳转到应用
 * 4. onDestroy 自动重启
 */
class ForegroundService : Service() {
    companion object {
        private const val TAG = "ForegroundService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "androidforclaw_service"
        private const val CHANNEL_NAME = "AndroidForClaw 后台服务"
        const val ACTION_START_ACTIVITY = "START_ACTIVITY"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_ACTIVITY_NAME = "activity_name"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "ForegroundService onCreate")
        createNotificationChannel()

        // Android 14+ 需要显式指定 foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    /**
     * 当 Service 被启动时调用
     *
     * 返回 START_STICKY：服务被杀后，系统会尝试重新创建服务
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "ForegroundService onStartCommand")

        when (intent?.action) {
            ACTION_START_ACTIVITY -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val activityName = intent.getStringExtra(EXTRA_ACTIVITY_NAME)

                if (packageName != null && activityName != null) {
                    val activityIntent = Intent().also { intent ->
                        intent.component = ComponentName(packageName, activityName)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        startActivity(activityIntent)
                        Log.i(TAG, "启动 Activity: $packageName/$activityName")
                    } catch (e: Exception) {
                        Log.e(TAG, "启动 Activity 失败", e)
                    }
                }
            }
        }

        // START_STICKY: 服务被杀后系统会重启，intent 可能为 null
        return START_STICKY
    }

    /**
     * 如果该 Service 不提供绑定，则返回 null。
     */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "ForegroundService onDestroy - 服务被销毁")

        // 保活机制：服务被销毁时尝试重启
        try {
            val restartIntent = Intent(applicationContext, ForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
            Log.i(TAG, "已触发服务重启")
        } catch (e: Exception) {
            Log.e(TAG, "服务重启失败", e)
        }
    }

    /**
     * 创建通知渠道（Android 8.0+ 必需）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持 AndroidForClaw 后台运行"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "通知渠道已创建")
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        // 创建点击通知的 PendingIntent（跳转到应用）
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent()

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AndroidForClaw 正在运行")
            .setContentText("点击打开应用")
            .setSmallIcon(R.drawable.ic_baseline_adb_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(true)
            .setOngoing(true) // 设置为持续通知，用户无法滑动删除
            .setContentIntent(pendingIntent)
            .setAutoCancel(false) // 点击后不自动取消
            .build()
    }
}