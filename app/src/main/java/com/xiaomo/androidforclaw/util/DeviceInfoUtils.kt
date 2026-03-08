/*
 * AndroidDeviceInfoUtils.kt
 * 封装常用的设备与应用信息获取工具方法（Kotlin）
 * 使用说明：把此文件放入你的 Android 项目的合适包下（例如: com.example.utils），直接调用 DeviceInfoUtils 中的方法。
 * 兼容处理：对不同 Android API 做了兼容判断；对可能不存在的项做了多重回退。
 */

package com.xiaomo.androidforclaw.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.xiaomo.androidforclaw.util.LayoutExceptionLogger

/**
 * 封装返回的应用版本信息对象
 */
data class AppVersion(
    val versionName: String,
    val versionCode: Long
)

/**
 * 设备与应用信息工具类
 */
object DeviceInfoUtils {

    /**
     * 获取当前应用（调用方包名）的版本信息
     * @return AppVersion（不会为 null，若无法获取则返回 versionName="unknown", versionCode=-1）
     */
    fun getCurrentAppVersion(context: Context): AppVersion {
        return getAppVersionInfo(context, context.packageName)
            ?: AppVersion("unknown", -1L)
    }

    /**
     * 获取指定包名的版本信息
     * @param context Context
     * @param targetPackage 要查询的包名
     * @return AppVersion? 若未安装或异常返回 null
     */
    fun getAppVersionInfo(context: Context, targetPackage: String): AppVersion? {
        return try {
            val pm = context.packageManager
            val packageInfo: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // 从 Android 13 (TIRAMISU) 推荐使用带 flags 的 API
                    pm.getPackageInfo(targetPackage, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(targetPackage, 0)
                }

            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            AppVersion(versionName, versionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            LayoutExceptionLogger.log("DeviceInfoUtils#getAppVersion", e)
            null
        }
    }

    /**
     * 判断某个包名是否已安装在设备上
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            LayoutExceptionLogger.log("DeviceInfoUtils#isAppInstalled", e)
            false
        }
    }

    /**
     * 获取设备的厂商 + 型号，例如："Google Pixel 7" 或 "Vendor 12"
     */
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER?.trim()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            ?: ""
        val model = Build.MODEL?.trim() ?: ""
        return when {
            manufacturer.isEmpty() && model.isEmpty() -> "Unknown"
            manufacturer.isEmpty() -> model
            model.isEmpty() -> manufacturer
            model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }
    }

    /**
     * 获取设备名（优先取系统设置里的设备名，若不可用则回退到蓝牙名，再回退到厂商+型号）
     * 注意：不同厂商/系统对设备名的存储位置不同，因此做多重尝试
     */
    fun getDeviceName(context: Context): String {
        // 1) 尝试从 Settings.Global（部分 ROM 在此存储）
        try {
            val nameGlobal =
                Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            if (!nameGlobal.isNullOrBlank()) return nameGlobal
        } catch (e: Exception) {
            LayoutExceptionLogger.log("DeviceInfoUtils#getDeviceName#Global", e)
            // ignore
        }

        // 2) 尝试从 Settings.Secure（部分设备/厂商可能在此存储）
        try {
            val nameSecure = Settings.Secure.getString(context.contentResolver, "device_name")
            if (!nameSecure.isNullOrBlank()) return nameSecure
        } catch (e: Exception) {
            LayoutExceptionLogger.log("DeviceInfoUtils#getDeviceName#Secure", e)
            // ignore
        }

        return getDeviceModel()
    }

    /**
     * 获取已安装应用的可读名称（应用标签）
     * @return 应用名（如无法获取返回 null）
     */
    fun getAppLabel(context: Context, packageName: String): String? {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            LayoutExceptionLogger.log("DeviceInfoUtils#getAppName", e)
            null
        }
    }

    /**
     * 将 PackageInfo 的版本信息格式化为字符串（便于日志）
     */
    fun formatAppVersionForLog(appVersion: AppVersion?): String {
        return if (appVersion == null) {
            "not installed"
        } else {
            "versionName=${appVersion.versionName}, versionCode=${appVersion.versionCode}"
        }
    }

}
