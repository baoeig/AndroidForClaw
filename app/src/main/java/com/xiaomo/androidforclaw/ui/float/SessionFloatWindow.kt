package com.xiaomo.androidforclaw.ui.float

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.draco.ladb.R
import com.xiaomo.androidforclaw.util.MMKVKeys
import com.tencent.mmkv.MMKV

/**
 * 会话信息悬浮窗管理器
 *
 * 功能：
 * - 仅在主页面不可见时显示
 * - 显示当前会话状态和最新消息
 * - 不支持滚动，仅显示固定内容
 * - 默认关闭，通过 App 内开关控制
 */
object SessionFloatWindow {
    private const val TAG = "SessionFloatWindow"
    private const val FLOAT_TAG = "session_float"

    private var isEnabled = false
    private var isMainActivityVisible = true
    private var sessionInfoTextView: TextView? = null

    /**
     * 初始化悬浮窗配置
     */
    fun init(context: Context) {
        // 从 MMKV 读取开关状态
        val mmkv = MMKV.defaultMMKV()
        isEnabled = mmkv.decodeBool(MMKVKeys.FLOAT_WINDOW_ENABLED.key, false)

        Log.d(TAG, "SessionFloatWindow initialized, enabled=$isEnabled")
    }

    /**
     * 设置悬浮窗开关状态
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        isEnabled = enabled

        // 保存到 MMKV
        val mmkv = MMKV.defaultMMKV()
        mmkv.encode(MMKVKeys.FLOAT_WINDOW_ENABLED.key, enabled)

        Log.d(TAG, "Float window enabled=$enabled")

        if (enabled) {
            // 如果主页面不可见，创建并显示悬浮窗
            if (!isMainActivityVisible) {
                createFloatWindow(context)
            }
        } else {
            // 关闭时销毁悬浮窗
            dismissFloatWindow()
        }
    }

    /**
     * 获取悬浮窗开关状态
     */
    fun isEnabled(): Boolean {
        return isEnabled
    }

    /**
     * 设置主页面可见性
     */
    fun setMainActivityVisible(visible: Boolean, context: Context) {
        isMainActivityVisible = visible

        Log.d(TAG, "Main activity visible=$visible, enabled=$isEnabled")

        if (!isEnabled) {
            return
        }

        if (visible) {
            // 主页面可见，隐藏悬浮窗
            dismissFloatWindow()
        } else {
            // 主页面不可见，显示悬浮窗
            createFloatWindow(context)
        }
    }

    /**
     * 更新会话信息
     */
    @SuppressLint("SetTextI18n")
    fun updateSessionInfo(title: String, content: String) {
        sessionInfoTextView?.text = "$title\n$content"
        Log.d(TAG, "Updated session info: $title")
    }

    /**
     * 创建悬浮窗
     */
    @SuppressLint("InflateParams")
    private fun createFloatWindow(context: Context) {
        // 检查是否已存在
        if (EasyFloat.isShow(FLOAT_TAG)) {
            Log.d(TAG, "Float window already exists")
            return
        }

        try {
            EasyFloat.with(context)
                .setTag(FLOAT_TAG)
                .setLayout(R.layout.layout_session_float) { view ->
                    // 初始化视图
                    sessionInfoTextView = view.findViewById(R.id.tv_session_info)
                    sessionInfoTextView?.text = "等待会话信息..."
                }
                .setGravity(Gravity.END or Gravity.CENTER_VERTICAL, 0, 0)
                .setShowPattern(ShowPattern.CURRENT_ACTIVITY)
                .setSidePattern(SidePattern.RESULT_SIDE)
                .setDragEnable(true)
                .show()

            Log.d(TAG, "Float window created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create float window", e)
        }
    }

    /**
     * 销毁悬浮窗
     */
    private fun dismissFloatWindow() {
        try {
            if (EasyFloat.isShow(FLOAT_TAG)) {
                EasyFloat.dismiss(FLOAT_TAG)
                sessionInfoTextView = null
                Log.d(TAG, "Float window dismissed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss float window", e)
        }
    }
}
