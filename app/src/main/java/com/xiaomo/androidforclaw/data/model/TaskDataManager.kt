package com.xiaomo.androidforclaw.data.model

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 任务数据管理器
 * 负责管理TaskData的创建、替换和访问
 */
class TaskDataManager {
    companion object {
        private const val TAG = "TaskDataManager"

        @Volatile
        private var INSTANCE: TaskDataManager? = null

        fun getInstance(): TaskDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TaskDataManager().also { INSTANCE = it }
            }
        }
    }

    private val _currentTaskData = MutableStateFlow<TaskData?>(null)
    val currentTaskData: StateFlow<TaskData?> = _currentTaskData.asStateFlow()

    /**
     * 启动新任务，创建新的TaskData
     */
    fun startNewTask(taskId: String,packageName: String) {
        Log.d(TAG, "启动新任务: $taskId")
        val newTaskData = TaskData(taskId,packageName)
        _currentTaskData.value = newTaskData
    }

    /**
     * 获取当前任务数据
     */
    fun getCurrentTaskData(): TaskData? = _currentTaskData.value

    /**
     * 清理当前任务数据
     */
    fun clearCurrentTask() {
        Log.d(TAG, "清理当前任务数据")
        _currentTaskData.value = null
    }

    /**
     * 检查是否有当前任务
     */
    fun hasCurrentTask(): Boolean = _currentTaskData.value != null
}
