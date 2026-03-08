package com.xiaomo.androidforclaw.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import com.draco.ladb.R

/**
 * 悬浮窗根布局容器
 * 使用自定义Button Tab切换View页面（避免Material Design组件在Service中的主题问题）
 */
class FloatingRootContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var btnTabChatHistory: Button
    private lateinit var btnTabTodos: Button
    private lateinit var btnTabBugList: Button
    private lateinit var contentContainer: LinearLayout

    // 页面View (ChatHistoryView 和 ChatLogView 已删除 - 旧架构)
    private var todoListView: TodosView? = null

    // 当前显示的页面索引
    private var currentPageIndex = 0

    init {
        orientation = VERTICAL
        setupViews()
        setupTabListener()
        showPage(0) // 默认显示第一个页面
    }

    private fun setupViews() {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.layout_floating_root, this, true)

        btnTabChatHistory = findViewById(R.id.btnTabChatHistory)
        btnTabTodos = findViewById(R.id.btnTabTodos)
        btnTabBugList = findViewById(R.id.btnTabBugList)
        contentContainer = findViewById(R.id.contentContainer)

        // 创建页面View
        createViews()
    }

    private fun createViews() {
        // 创建TodosView
        todoListView = TodosView(context)
    }

    private fun setupTabListener() {
        btnTabChatHistory.setOnClickListener {
            showPage(0)
        }

        btnTabTodos.setOnClickListener {
            showPage(1)
        }

        btnTabBugList.setOnClickListener {
            showPage(2)
        }
    }

    private fun showPage(index: Int) {
        currentPageIndex = index
        contentContainer.removeAllViews()

        // 更新Tab按钮样式
        updateTabAppearance(index)

        // 显示对应的页面
        when (index) {
            0 -> {
                // ChatHistoryView 和 ChatLogView 已删除（旧架构），暂不显示
            }

            1 -> {
                todoListView?.let { contentContainer.addView(it) }
            }

            2 -> {
                // BugListView 已移除
            }
        }
    }

    private fun updateTabAppearance(selectedIndex: Int) {
        // 重置所有按钮为未选中状态
        btnTabChatHistory.setBackgroundResource(R.drawable.tab_unselected_background)
        btnTabChatHistory.setTextColor(context.getColor(android.R.color.white))
        btnTabTodos.setBackgroundResource(R.drawable.tab_unselected_background)
        btnTabTodos.setTextColor(context.getColor(android.R.color.white))
        btnTabBugList.setBackgroundResource(R.drawable.tab_unselected_background)
        btnTabBugList.setTextColor(context.getColor(android.R.color.white))

        // 设置选中的按钮样式
        when (selectedIndex) {
            0 -> {
                btnTabChatHistory.setBackgroundResource(R.drawable.tab_selected_background)
                btnTabChatHistory.setTextColor(context.getColor(R.color.primary_color))
            }

            1 -> {
                btnTabTodos.setBackgroundResource(R.drawable.tab_selected_background)
                btnTabTodos.setTextColor(context.getColor(R.color.primary_color))
            }

            2 -> {
                btnTabBugList.setBackgroundResource(R.drawable.tab_selected_background)
                btnTabBugList.setTextColor(context.getColor(R.color.primary_color))
            }
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        todoListView?.cleanup()
    }
} 