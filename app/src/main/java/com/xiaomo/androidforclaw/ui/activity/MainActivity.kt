package com.xiaomo.androidforclaw.ui.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xiaomo.androidforclaw.core.MyApplication
import com.xiaomo.androidforclaw.accessibility.AccessibilityProxy
import com.xiaomo.androidforclaw.util.MMKVKeys
import com.draco.ladb.R
import com.draco.ladb.databinding.ActivityMainBinding
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.launch

/**
 * AndroidForClaw 主页
 *
 * 映射 OpenClaw CLI 命令到可视化界面:
 * - openclaw status → 状态卡片
 * - openclaw config → 配置页面
 * - openclaw skills → Skills 管理
 * - openclaw gateway → Gateway 控制
 * - openclaw sessions → Session 列表
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mmkv by lazy { MMKV.defaultMMKV() }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ACCESSIBILITY = 1001
        private const val REQUEST_OVERLAY = 1002
        private const val REQUEST_SCREEN_CAPTURE = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        updateStatusCards()
    }

    override fun onResume() {
        super.onResume()
        updateStatusCards()
    }

    private fun setupViews() {
        // 状态卡片点击事件
        binding.apply {
            // Gateway 卡片
            cardGateway.setOnClickListener {
                if (isGatewayRunning()) {
                    showGatewayInfo()
                } else {
                    Toast.makeText(this@MainActivity, "Gateway 未运行", Toast.LENGTH_SHORT).show()
                }
            }

            // 权限卡片
            cardPermissions.setOnClickListener {
                startActivity(Intent(this@MainActivity, PermissionsActivity::class.java))
            }

            // Skills 卡片
            cardSkills.setOnClickListener {
                // TODO: 打开 Skills 管理页面
                Toast.makeText(this@MainActivity, "Skills 管理 (开发中)", Toast.LENGTH_SHORT).show()
            }

            // Sessions 卡片
            cardSessions.setOnClickListener {
                // TODO: 打开 Sessions 列表
                Toast.makeText(this@MainActivity, "Session 列表 (开发中)", Toast.LENGTH_SHORT).show()
            }

            // 底部导航按钮
            btnConfig.setOnClickListener {
                startActivity(Intent(this@MainActivity, ConfigActivity::class.java))
            }

            btnTest.setOnClickListener {
                // AgentTestActivity已移除
                Toast.makeText(this@MainActivity, "Agent测试功能已废弃", Toast.LENGTH_SHORT).show()
            }

            btnLogs.setOnClickListener {
                // TODO: 查看日志
                Toast.makeText(this@MainActivity, "日志查看 (开发中)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 更新状态卡片
     * 映射 OpenClaw CLI: openclaw status
     */
    private fun updateStatusCards() {
        lifecycleScope.launch {
            updateGatewayCard()
            updatePermissionsCard()
            updateSkillsCard()
            updateSessionsCard()
        }
    }

    /**
     * 更新 Gateway 状态卡片
     */
    private fun updateGatewayCard() {
        val isRunning = isGatewayRunning()
        binding.apply {
            tvGatewayStatus.text = if (isRunning) "运行中" else "未运行"
            tvGatewayStatus.setTextColor(
                if (isRunning) getColor(R.color.status_ok)
                else getColor(R.color.status_error)
            )

            if (isRunning) {
                tvGatewayDetails.text = "WebSocket: ws://0.0.0.0:8765\n" +
                        "Sessions: ${getSessionCount()}"
            } else {
                tvGatewayDetails.text = "Gateway 服务未启动"
            }
        }
    }

    /**
     * 更新权限状态卡片
     */
    private fun updatePermissionsCard() {
        val accessibility = AccessibilityProxy.isConnected.value == true && AccessibilityProxy.isServiceReady()
        val overlay = Settings.canDrawOverlays(this)
        val screenCapture = AccessibilityProxy.isMediaProjectionGranted()

        val allGranted = accessibility && overlay && screenCapture

        binding.apply {
            tvPermissionsStatus.text = if (allGranted) "已授权" else "需要授权"
            tvPermissionsStatus.setTextColor(
                if (allGranted) getColor(R.color.status_ok)
                else getColor(R.color.status_warning)
            )

            tvPermissionsDetails.text = buildString {
                append("无障碍: ${if (accessibility) "✓" else "✗"}\n")
                append("悬浮窗: ${if (overlay) "✓" else "✗"}\n")
                append("录屏: ${if (screenCapture) "✓" else "✗"} (${AccessibilityProxy.getMediaProjectionStatus()})")
            }
        }
    }

    /**
     * 更新 Skills 状态卡片
     */
    private fun updateSkillsCard() {
        // TODO: 从 SkillsLoader 获取实际数据
        val totalSkills = 8  // 临时数据
        val alwaysSkills = 3

        binding.apply {
            tvSkillsStatus.text = "$totalSkills 个 Skills"
            tvSkillsStatus.setTextColor(getColor(R.color.status_ok))

            tvSkillsDetails.text = buildString {
                append("Always: $alwaysSkills\n")
                append("On-Demand: ${totalSkills - alwaysSkills}\n")
                append("Bundled: 8")
            }
        }
    }

    /**
     * 更新 Sessions 状态卡片
     */
    private fun updateSessionsCard() {
        val sessionCount = getSessionCount()

        binding.apply {
            tvSessionsStatus.text = if (sessionCount > 0) {
                "$sessionCount 个活跃会话"
            } else {
                "无活跃会话"
            }
            tvSessionsStatus.setTextColor(
                if (sessionCount > 0) getColor(R.color.status_ok)
                else getColor(R.color.text_secondary)
            )

            tvSessionsDetails.text = if (sessionCount > 0) {
                "点击查看详情"
            } else {
                "暂无活跃的 Agent 会话"
            }
        }
    }

    /**
     * 显示 Gateway 详细信息
     * 映射 OpenClaw CLI: openclaw gateway status
     */
    private fun showGatewayInfo() {
        val info = buildString {
            append("Gateway 状态\n\n")
            append("WebSocket 端口: 8765\n")
            append("连接地址: ws://0.0.0.0:8765\n")
            append("活跃 Sessions: ${getSessionCount()}\n\n")
            append("RPC 方法:\n")
            append("  • agent - 执行 Agent 任务\n")
            append("  • agent.wait - 等待任务完成\n")
            append("  • health - 健康检查\n")
            append("  • session.list - 列出会话\n")
            append("  • session.reset - 重置会话\n")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Gateway 信息")
            .setMessage(info)
            .setPositiveButton("关闭", null)
            .setNeutralButton("测试连接") { _, _ ->
                // TODO: 测试 WebSocket 连接
                Toast.makeText(this, "测试功能开发中", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 显示权限设置对话框
     */
    private fun showPermissionsDialog() {
        val accessibility = AccessibilityProxy.isConnected.value == true && AccessibilityProxy.isServiceReady()
        val overlay = Settings.canDrawOverlays(this)
        val screenCapture = AccessibilityProxy.isMediaProjectionGranted()

        val message = buildString {
            append("权限状态:\n\n")
            append("${if (accessibility) "✓" else "✗"} 无障碍服务\n")
            if (!accessibility) {
                append("  用于: 点击、滑动、输入\n\n")
            }
            append("${if (overlay) "✓" else "✗"} 悬浮窗权限\n")
            if (!overlay) {
                append("  用于: 显示 Agent 状态\n\n")
            }
            append("${if (screenCapture) "✓" else "✗"} 录屏权限\n")
            if (!screenCapture) {
                append("  用于: 截图观察界面\n")
                append("  状态: ${AccessibilityProxy.getMediaProjectionStatus()}\n")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("权限管理")
            .setMessage(message)
            .setPositiveButton("前往设置") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 请求权限
     */
    private fun requestPermissions() {
        val accessibility = AccessibilityProxy.isConnected.value == true && AccessibilityProxy.isServiceReady()
        val overlay = Settings.canDrawOverlays(this)
        val screenCapture = AccessibilityProxy.isMediaProjectionGranted()

        when {
            !accessibility -> {
                // 打开无障碍设置
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivityForResult(intent, REQUEST_ACCESSIBILITY)
            }
            !overlay -> {
                // 请求悬浮窗权限
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY)
            }
            !screenCapture -> {
                // 录屏权限需要在无障碍服务 APK 中授予
                Toast.makeText(
                    this,
                    "录屏权限由无障碍服务 APK 管理\n请在系统设置中授予",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 检查 Gateway 是否运行
     */
    private fun isGatewayRunning(): Boolean {
        // TODO: 实际检查 GatewayService 状态
        // 临时通过 Application 判断
        return true  // Gateway 在 Application.onCreate 中启动
    }

    /**
     * 获取活跃 Session 数量
     */
    private fun getSessionCount(): Int {
        // TODO: 从 GatewayService 获取实际数据
        return 0  // 临时返回 0
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ACCESSIBILITY, REQUEST_OVERLAY -> {
                // 权限设置返回，刷新状态
                updateStatusCards()
            }
        }
    }
}
