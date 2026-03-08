package com.xiaomo.androidforclaw.ui.activity

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.xiaomo.quickjs.QuickJSExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * QuickJS 测试 Activity
 * 用于直接测试 QuickJS JavaScript 执行器
 */
class QuickJSTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "QuickJSTestActivity"
    }

    private lateinit var resultTextView: TextView
    private lateinit var executor: QuickJSExecutor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建简单布局
        resultTextView = TextView(this).apply {
            text = "Testing QuickJS...\n\n"
            setPadding(32, 32, 32, 32)
            textSize = 14f
        }
        setContentView(resultTextView)

        executor = QuickJSExecutor(applicationContext)

        // 开始测试
        runTests()
    }

    private fun runTests() {
        CoroutineScope(Dispatchers.Main).launch {
            appendResult("=== QuickJS 测试开始 ===\n\n")

            // 测试 1: 基础计算
            test1BasicMath()

            // 测试 2: ES6 特性
            test2ES6Features()

            // 测试 3: async/await
            test3AsyncAwait()

            // 测试 4: 错误处理
            test4ErrorHandling()

            // 测试 5: 完整测试
            test5FullTest()

            appendResult("\n=== 所有测试完成 ===")
        }
    }

    private suspend fun test1BasicMath() {
        appendResult("【测试 1】基础计算\n")
        val code = "123 + 456"

        val result = withContext(Dispatchers.IO) {
            executor.execute(code)
        }

        if (result.success) {
            appendResult("✅ 结果: ${result.result}\n")
            Log.d(TAG, "Test 1 passed: ${result.result}")
        } else {
            appendResult("❌ 错误: ${result.error}\n")
            Log.e(TAG, "Test 1 failed: ${result.error}")
        }
        appendResult("\n")
    }

    private suspend fun test2ES6Features() {
        appendResult("【测试 2】ES6 特性\n")
        val code = """
            const arr = [1, 2, 3, 4, 5];
            const doubled = arr.map(x => x * 2);
            const sum = arr.reduce((a, b) => a + b, 0);
            JSON.stringify({ doubled, sum });
        """.trimIndent()

        val result = withContext(Dispatchers.IO) {
            executor.execute(code)
        }

        if (result.success) {
            appendResult("✅ 结果: ${result.result}\n")
            Log.d(TAG, "Test 2 passed: ${result.result}")
        } else {
            appendResult("❌ 错误: ${result.error}\n")
            Log.e(TAG, "Test 2 failed: ${result.error}")
        }
        appendResult("\n")
    }

    private suspend fun test3AsyncAwait() {
        appendResult("【测试 3】立即返回函数\n")
        val code = """
            const func = () => {
                return "Function works!";
            };
            func();
        """.trimIndent()

        val result = withContext(Dispatchers.IO) {
            executor.execute(code)
        }

        if (result.success) {
            appendResult("✅ 结果: ${result.result}\n")
            Log.d(TAG, "Test 3 passed: ${result.result}")
        } else {
            appendResult("❌ 错误: ${result.error}\n")
            Log.e(TAG, "Test 3 failed: ${result.error}")
        }
        appendResult("\n")
    }

    private suspend fun test4ErrorHandling() {
        appendResult("【测试 4】错误处理\n")
        val code = "throw new Error('Test error');"

        val result = withContext(Dispatchers.IO) {
            executor.execute(code)
        }

        if (!result.success && result.error?.contains("Test error") == true) {
            appendResult("✅ 错误正确捕获: ${result.error}\n")
            Log.d(TAG, "Test 4 passed: Error handling works")
        } else {
            appendResult("❌ 错误处理失败\n")
            Log.e(TAG, "Test 4 failed: Error not caught")
        }
        appendResult("\n")
    }

    private suspend fun test5FullTest() {
        appendResult("【测试 5】完整功能测试\n")
        val code = """
            const result1 = 123 + 456;
            const arr = [1, 2, 3, 4, 5];
            const doubled = arr.map(x => x * 2);
            const sum = arr.reduce((a, b) => a + b, 0);

            const obj = {
                name: "QuickJS Test",
                version: "1.0",
                features: ["ES6+", "Functions", "JSON"]
            };

            const str = "Hello, QuickJS!";
            const reversed = str.split('').reverse().join('');

            const funcTest = () => {
                return "Function works!";
            };

            JSON.stringify({
                basicMath: result1,
                arrayMap: doubled,
                arraySum: sum,
                object: obj,
                stringReverse: reversed,
                funcResult: funcTest()
            });
        """.trimIndent()

        val result = withContext(Dispatchers.IO) {
            executor.execute(code)
        }

        if (result.success) {
            appendResult("✅ 结果:\n${result.result}\n")
            Log.d(TAG, "Test 5 passed: ${result.result}")
        } else {
            appendResult("❌ 错误: ${result.error}\n")
            Log.e(TAG, "Test 5 failed: ${result.error}")
        }
        appendResult("\n")
    }

    private fun appendResult(text: String) {
        runOnUiThread {
            resultTextView.append(text)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.cleanup()
    }
}
