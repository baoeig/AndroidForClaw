package com.xiaomo.androidforclaw.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.xiaomo.androidforclaw.data.model.TaskDataManager
import com.xiaomo.androidforclaw.ext.setMarkdownText
import com.draco.ladb.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

/**
 * TodosView
 * 专门展示 MobileOperationAgent 中的 instructionProgress 内容
 * 使用TaskDataManager和TaskData的Flow进行响应式更新
 */
class TodosView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "TodosView"
    }

    private lateinit var progressContent: TextView
    private lateinit var expectedContent: TextView
    private val taskDataManager: TaskDataManager = TaskDataManager.getInstance()
    private var progressCollectJob: Job? = null
    private var taskDataCollectJob: Job? = null
    private var expectedSet: Boolean = false

    init {
        orientation = VERTICAL
        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        // 加载布局
        inflate(context, R.layout.view_todo_list_simplified, this)
        progressContent = findViewById(R.id.progressContent)

        // 设置初始状态
        updateProgressDisplay("等待任务开始...")

        // 在进度内容后面追加一个“操作后的预期”卡片（静态，不连续更新）
        val parentContainer = progressContent.parent as? LinearLayout ?: this
        expectedContent = TextView(context).apply {
            textSize = 14f
            setTextColor(context.getColor(android.R.color.black))
            setPadding(8, 16, 8, 8)
            text = "操作后的预期：\n(等待任务开始)"
        }
        parentContainer.addView(expectedContent)
    }

    private fun setupObservers() {
        Log.d(TAG, "设置TodosView观察者...")

        // ✅ 使用GlobalScope，避免悬浮窗缺少LifecycleOwner的问题
        taskDataCollectJob = kotlinx.coroutines.GlobalScope.launch {
            taskDataManager.currentTaskData.collect { taskData ->
                Log.d(TAG, "TaskData变化: ${taskData?.taskId}")

                // 取消旧的订阅
                progressCollectJob?.cancel()

                if (taskData != null) {
                    // ✅ 修复：每次新任务开始时重置expectedSet标志
                    expectedSet = false
                    Log.d(TAG, "新任务开始，重置expectedSet标志")

                    // TODO: conversationFlow 已删除（旧架构）
                    // 新架构使用 SessionManager 管理对话历史，不再在 TaskData 中存储
                    // 进度信息通过 ProgressUpdate → SessionFloatWindow 显示
                    updateProgressDisplay("等待任务执行...")

                    // TODO: currentTestCaseFlow 已删除（旧架构概念）
                    // 新架构中没有预生成的测试用例，不需要显示"操作后的预期"
                    if (!expectedSet) {
                        post {
                            expectedContent.setMarkdownText("### 操作后的预期\n\n（新架构暂无）")
                            expectedSet = true
                        }
                    }
                } else {
                    // TaskData为null时显示默认状态
                    Log.d(TAG, "TaskData为null，显示默认状态")
                    updateProgressDisplay("等待任务开始...")
                    expectedContent.text = "操作后的预期：\n(等待任务开始)"
                    expectedSet = false
                }
            }
        }

        Log.d(TAG, "已设置TaskData观察者")
    }

    private fun updateProgressDisplay(progress: String) {
        Log.d(TAG, "更新进度显示: ${progress.take(100)}...")
        post {
            if (progress.isNullOrEmpty()) {
                progressContent.text = "等待任务开始..."
            } else {
                progressContent.setMarkdownText(progress)
            }
        }

    }

    fun cleanup() {
        Log.d(TAG, "清理TodosView资源")
        progressCollectJob?.cancel()
        progressCollectJob = null
        taskDataCollectJob?.cancel()
        taskDataCollectJob = null
    }
} 