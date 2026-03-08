package com.xiaomo.quickjs

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * QuickJS Executor 测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QuickJSExecutorTest {

    private lateinit var context: Context
    private lateinit var executor: QuickJSExecutor

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        executor = QuickJSExecutor(context)
    }

    @Test
    fun `test basic arithmetic`() = runBlocking {
        val result = executor.execute("return 1 + 1;")

        assertTrue(result.success)
        assertEquals("2", result.result?.trim())
    }

    @Test
    fun `test array operations with lodash`() = runBlocking {
        val result = executor.execute("""
            const numbers = [1, 2, 3, 4, 5];
            const doubled = _.map(numbers, x => x * 2);
            return _.sum(doubled);
        """.trimIndent())

        assertTrue(result.success)
        assertEquals("30", result.result?.trim())
    }

    @Test
    fun `test group by operation`() = runBlocking {
        val result = executor.execute("""
            const users = [
                { name: 'John', role: 'admin' },
                { name: 'Jane', role: 'user' },
                { name: 'Bob', role: 'admin' }
            ];
            const grouped = _.groupBy(users, 'role');
            return JSON.stringify(grouped);
        """.trimIndent())

        assertTrue(result.success)
        assertTrue(result.result?.contains("admin") == true)
        assertTrue(result.result?.contains("user") == true)
    }

    @Test
    fun `test async await`() = runBlocking {
        val result = executor.execute("""
            async function test() {
                await System.sleep(100);
                return 'completed';
            }
            return await test();
        """.trimIndent())

        assertTrue(result.success)
        assertEquals("completed", result.result?.trim())
    }

    @Test
    fun `test CSV parsing`() = runBlocking {
        val result = executor.execute("""
            const csv = "name,age\nJohn,30\nJane,25";
            const data = parseCSV(csv, { hasHeader: true });
            return data.length;
        """.trimIndent())

        assertTrue(result.success)
        assertEquals("2", result.result?.trim())
    }

    @Test
    fun `test string operations`() = runBlocking {
        val result = executor.execute("""
            const camel = _.camelCase('hello-world-test');
            const snake = _.snakeCase('helloWorldTest');
            return JSON.stringify({ camel, snake });
        """.trimIndent())

        assertTrue(result.success)
        assertTrue(result.result?.contains("helloWorldTest") == true)
        assertTrue(result.result?.contains("hello_world_test") == true)
    }

    @Test
    fun `test statistics functions`() = runBlocking {
        val result = executor.execute("""
            const numbers = [1, 2, 3, 4, 5];
            return JSON.stringify({
                sum: _.sum(numbers),
                mean: _.mean(numbers),
                max: _.max(numbers),
                min: _.min(numbers)
            });
        """.trimIndent())

        assertTrue(result.success)
        assertTrue(result.result?.contains("\"sum\":15") == true)
        assertTrue(result.result?.contains("\"mean\":3") == true)
    }

    @Test
    fun `test error handling`() = runBlocking {
        val result = executor.execute("throw new Error('Test error');")

        assertFalse(result.success)
        assertTrue(result.error?.contains("Test error") == true)
    }

    @Test
    fun `test empty code`() = runBlocking {
        val result = executor.execute("")

        assertFalse(result.success)
        assertTrue(result.error?.contains("empty") == true)
    }

    @Test
    fun `test JSON parsing utility`() = runBlocking {
        val result = executor.execute("""
            const valid = parseJSON('{"key": "value"}');
            const invalid = parseJSON('invalid json', {});
            return JSON.stringify({ valid, invalid });
        """.trimIndent())

        assertTrue(result.success)
        assertTrue(result.result?.contains("key") == true)
    }

    @Test
    fun `test array utility functions`() = runBlocking {
        val result = executor.execute("""
            const arr = [1, 2, 2, 3, 3, 3];
            return JSON.stringify({
                unique: _.uniq(arr),
                chunks: _.chunk(arr, 2),
                compact: _.compact([1, 0, null, 2, false, 3])
            });
        """.trimIndent())

        assertTrue(result.success)
        assertTrue(result.result?.contains("unique") == true)
        assertTrue(result.result?.contains("chunks") == true)
    }
}
