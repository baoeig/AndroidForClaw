package com.xiaomo.androidforclaw.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xiaomo.androidforclaw.core.MainEntryNew
import com.xiaomo.androidforclaw.core.MyApplication
import com.draco.ladb.R
import com.draco.ladb.databinding.ActivityAgentTestBinding
import kotlinx.coroutines.launch

/**
 * Agent 测试页面
 * 快速测试 Agent 执行
 */
class AgentTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgentTestBinding
    private var isRunning = false

    companion object {
        private const val TAG = "AgentTestActivity"

        // 预设测试任务
        val PRESET_TASKS = listOf(
            "截取当前屏幕截图",
            "打开设置应用",
            "返回主屏幕",
            "获取当前时间"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgentTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Agent 测试"
        }

        setupViews()
    }

    private fun setupViews() {
        // 预设任务按钮
        binding.apply {
            btnTask1.text = PRESET_TASKS[0]
            btnTask2.text = PRESET_TASKS[1]
            btnTask3.text = PRESET_TASKS[2]
            btnTask4.text = PRESET_TASKS[3]

            btnTask1.setOnClickListener { runTask(PRESET_TASKS[0]) }
            btnTask2.setOnClickListener { runTask(PRESET_TASKS[1]) }
            btnTask3.setOnClickListener { runTask(PRESET_TASKS[2]) }
            btnTask4.setOnClickListener { runTask(PRESET_TASKS[3]) }

            // 自定义任务
            btnRun.setOnClickListener {
                val task = etCustomTask.text.toString().trim()
                if (task.isNotEmpty()) {
                    runTask(task)
                } else {
                    Toast.makeText(this@AgentTestActivity, "请输入任务", Toast.LENGTH_SHORT).show()
                }
            }

            // 停止按钮
            btnStop.setOnClickListener {
                stopTask()
            }
        }
    }

    private fun runTask(task: String) {
        if (isRunning) {
            Toast.makeText(this, "任务正在执行中", Toast.LENGTH_SHORT).show()
            return
        }

        isRunning = true
        binding.apply {
            progressBar.visibility = View.VISIBLE
            btnRun.isEnabled = false
            btnStop.isEnabled = true
            tvStatus.text = "执行中: $task"
            tvOutput.text = ""
        }

        lifecycleScope.launch {
            try {
                // 使用 MainEntryNew.run() 执行 Agent
                MainEntryNew.run(
                    userInput = task,
                    application = application,
                    existingRecordId = null,
                    existingPackageName = null,
                    onSummaryFinished = null
                )

                // 观察完成状态
                MainEntryNew.summaryFinished.collect { finished ->
                    if (finished) {
                        runOnUiThread {
                            binding.tvStatus.text = "完成"
                            binding.tvOutput.text = "任务执行完成\n请查看悬浮窗或日志获取详细结果"
                        }
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    binding.tvStatus.text = "失败"
                    binding.tvOutput.text = "错误: ${e.message}\n\n${e.stackTraceToString()}"
                }
            } finally {
                isRunning = false
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRun.isEnabled = true
                    binding.btnStop.isEnabled = false
                }
            }
        }
    }

    private fun updateProgress(progress: Map<String, Any>) {
        val type = progress["type"] as? String ?: "unknown"
        val message = when (type) {
            "iteration" -> "迭代 ${progress["number"]}"
            "tool_call" -> "调用工具: ${progress["name"]}"
            "tool_result" -> "工具结果: ${(progress["result"] as? String)?.take(50)}"
            "iteration_complete" -> "迭代完成"
            else -> type
        }

        binding.tvOutput.append("$message\n")
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun stopTask() {
        // TODO: 实现停止逻辑
        Toast.makeText(this, "停止功能开发中", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
