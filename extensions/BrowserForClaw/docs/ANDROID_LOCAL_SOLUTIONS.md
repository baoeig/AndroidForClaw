# Android 本地执行方案分析

> **场景**: 无法使用远程服务器，所有计算必须在 Android 设备本地完成

---

## 📋 目录

1. [可行方案对比](#可行方案对比)
2. [方案 1: Termux 深度集成](#方案-1-termux-深度集成)
3. [方案 2: 嵌入式 JavaScript 引擎](#方案-2-嵌入式-javascript-引擎)
4. [方案 3: Chaquopy Python](#方案-3-chaquopy-python)
5. [方案 4: Lua/LuaJ 脚本](#方案-4-lualuaj-脚本)
6. [方案 5: 混合方案](#方案-5-混合方案)
7. [推荐实施路径](#推荐实施路径)

---

## 可行方案对比

| 方案 | 生态 | APK体积 | 性能 | 实现难度 | 推荐度 |
|------|------|---------|------|----------|--------|
| **Termux 集成** | ⭐⭐⭐⭐⭐ | +0 MB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **JavaScript (J2V8/QuickJS)** | ⭐⭐⭐⭐ | +5-10 MB | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **Chaquopy Python** | ⭐⭐⭐⭐ | +50-80 MB | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Lua/LuaJ** | ⭐⭐ | +1 MB | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐ |
| **混合方案** | ⭐⭐⭐⭐⭐ | +10-15 MB | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 方案 1: Termux 深度集成 ⭐⭐⭐⭐⭐

### 核心思路

Termux 提供完整的 Linux 环境，PhoneForClaw 通过进程间通信调用。

### 架构设计

```
┌──────────────────────────────────────┐
│       PhoneForClaw App               │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  TermuxBridge Tool             │ │
│  │  - 检测 Termux 安装            │ │
│  │  - 通过 Intent/IPC 通信        │ │
│  │  - 文件共享                    │ │
│  └───────────┬────────────────────┘ │
└──────────────┼──────────────────────┘
               │ Unix Socket / File IPC
               ↓
┌──────────────────────────────────────┐
│       Termux Environment             │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  RPC Server (Python)           │ │
│  │  - 监听 Unix Socket            │ │
│  │  - 执行 Python/Node/Shell      │ │
│  │  - 返回结果                    │ │
│  └────────────────────────────────┘ │
│                                      │
│  Python 3.11 + pip                   │
│  Node.js 18+ + npm                   │
│  完整 Linux 工具链                   │
└──────────────────────────────────────┘
```

### 实现方案 A: Unix Socket 通信 (推荐)

**Termux 端: RPC 服务器**

```python
# ~/.termux/phoneforclaw_server.py
import socket
import json
import subprocess
import os
import sys

SOCKET_PATH = '/data/data/com.termux/files/home/.phoneforclaw.sock'

def execute_code(runtime, code, args=None):
    """执行代码并返回结果"""
    if runtime == 'python':
        result = subprocess.run(
            ['python3', '-c', code],
            capture_output=True,
            text=True,
            timeout=60,
            cwd=args.get('cwd', os.getcwd()) if args else os.getcwd()
        )
    elif runtime == 'nodejs':
        result = subprocess.run(
            ['node', '-e', code],
            capture_output=True,
            text=True,
            timeout=60
        )
    elif runtime == 'shell':
        result = subprocess.run(
            code,
            shell=True,
            capture_output=True,
            text=True,
            timeout=60
        )
    else:
        return {'success': False, 'error': f'Unknown runtime: {runtime}'}

    return {
        'success': result.returncode == 0,
        'stdout': result.stdout,
        'stderr': result.stderr,
        'returncode': result.returncode
    }

def handle_request(data):
    """处理请求"""
    try:
        request = json.loads(data)
        action = request.get('action')

        if action == 'exec':
            runtime = request.get('runtime')
            code = request.get('code')
            args = request.get('args', {})
            return execute_code(runtime, code, args)

        elif action == 'install':
            package_manager = request.get('pm')  # pip, npm
            package = request.get('package')
            cmd = f"{package_manager} install {package}"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            return {
                'success': result.returncode == 0,
                'output': result.stdout + result.stderr
            }

        elif action == 'ping':
            return {'success': True, 'message': 'pong'}

        else:
            return {'success': False, 'error': f'Unknown action: {action}'}

    except Exception as e:
        return {'success': False, 'error': str(e)}

def start_server():
    """启动 Unix Socket 服务器"""
    # 删除旧的 socket 文件
    if os.path.exists(SOCKET_PATH):
        os.unlink(SOCKET_PATH)

    # 创建 Unix socket
    server = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    server.bind(SOCKET_PATH)
    server.listen(5)

    # 设置权限，允许其他应用访问
    os.chmod(SOCKET_PATH, 0o777)

    print(f"[PhoneForClaw Bridge] Server started at {SOCKET_PATH}")

    while True:
        client, _ = server.accept()
        try:
            # 接收数据
            data = b''
            while True:
                chunk = client.recv(4096)
                if not chunk:
                    break
                data += chunk
                if b'\n' in chunk:  # 以换行符作为消息结束标志
                    break

            # 处理请求
            result = handle_request(data.decode('utf-8'))

            # 发送响应
            response = json.dumps(result) + '\n'
            client.sendall(response.encode('utf-8'))

        finally:
            client.close()

if __name__ == '__main__':
    start_server()
```

**启动脚本**

```bash
# ~/.termux/start_bridge.sh
#!/data/data/com.termux/files/usr/bin/bash

echo "Starting PhoneForClaw Bridge Server..."

# 检查依赖
command -v python3 >/dev/null || { echo "Python not found. Installing..."; pkg install python; }
command -v node >/dev/null || { echo "Node.js not found. Installing..."; pkg install nodejs; }

# 启动服务器
python3 ~/.termux/phoneforclaw_server.py
```

**PhoneForClaw 端: TermuxBridge Tool**

```kotlin
// app/src/main/java/com/xiaomo/androidforclaw/agent/tools/TermuxBridgeTool.kt
package com.xiaomo.androidforclaw.agent.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.xiaomo.androidforclaw.agent.model.ToolDefinition
import com.xiaomo.androidforclaw.agent.model.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.Socket

class TermuxBridgeTool(private val context: Context) : Tool {
    override val name = "termux_exec"
    override val description = "Execute code locally in Termux environment (Python/Node.js/Shell)"

    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val SOCKET_PATH = "/data/data/com.termux/files/home/.phoneforclaw.sock"
    }

    override fun getToolDefinition(): ToolDefinition {
        return ToolDefinition(
            name = name,
            description = description,
            parameters = mapOf(
                "runtime" to mapOf(
                    "type" to "string",
                    "enum" to listOf("python", "nodejs", "shell"),
                    "description" to "Execution runtime"
                ),
                "code" to mapOf(
                    "type" to "string",
                    "description" to "Code to execute"
                ),
                "cwd" to mapOf(
                    "type" to "string",
                    "description" to "Working directory (optional)"
                )
            ),
            required = listOf("runtime", "code")
        )
    }

    /**
     * 检查 Termux 是否已安装
     */
    private fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 通过 Unix Socket 与 Termux 通信
     */
    private suspend fun communicateWithTermux(request: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        try {
            // 使用 Unix Domain Socket
            val socket = Socket()
            val address = java.net.InetSocketAddress("localhost", 0)  // Unix socket workaround

            // Android 不直接支持 Unix socket，使用文件 IPC 替代
            // 实际实现需要使用 JNI 或者通过文件共享

            // 简化方案: 使用共享文件
            val requestFile = File("/sdcard/AndroidForClaw/.ipc/request.json")
            val responseFile = File("/sdcard/AndroidForClaw/.ipc/response.json")

            requestFile.parentFile?.mkdirs()
            requestFile.writeText(request.toString())

            // 通过 Intent 触发 Termux 执行
            val intent = Intent().apply {
                setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH",
                    "/data/data/com.termux/files/usr/bin/python3")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(
                    "/data/data/com.termux/files/home/.termux/process_request.py",
                    requestFile.absolutePath,
                    responseFile.absolutePath
                ))
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }

            context.startService(intent)

            // 等待响应文件生成
            var attempts = 0
            while (!responseFile.exists() && attempts < 60) {
                Thread.sleep(500)
                attempts++
            }

            if (!responseFile.exists()) {
                return@withContext JSONObject().apply {
                    put("success", false)
                    put("error", "Timeout waiting for Termux response")
                }
            }

            val response = JSONObject(responseFile.readText())

            // 清理
            requestFile.delete()
            responseFile.delete()

            response

        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("error", "Communication failed: ${e.message}")
            }
        }
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        // 检查 Termux 是否安装
        if (!isTermuxInstalled()) {
            return ToolResult(
                success = false,
                error = "Termux not installed. Please install Termux from F-Droid or GitHub."
            )
        }

        val runtime = args["runtime"] as? String ?: return ToolResult(
            success = false,
            error = "Missing runtime parameter"
        )

        val code = args["code"] as? String ?: return ToolResult(
            success = false,
            error = "Missing code parameter"
        )

        val cwd = args["cwd"] as? String

        // 构建请求
        val request = JSONObject().apply {
            put("action", "exec")
            put("runtime", runtime)
            put("code", code)
            if (cwd != null) {
                put("args", JSONObject().apply {
                    put("cwd", cwd)
                })
            }
        }

        // 与 Termux 通信
        val response = communicateWithTermux(request)

        return if (response.optBoolean("success", false)) {
            ToolResult(
                success = true,
                data = mapOf(
                    "stdout" to response.optString("stdout", ""),
                    "stderr" to response.optString("stderr", ""),
                    "returncode" to response.optInt("returncode", 0)
                )
            )
        } else {
            ToolResult(
                success = false,
                error = response.optString("error", "Unknown error")
            )
        }
    }
}
```

**Termux 端: 请求处理脚本**

```python
# ~/.termux/process_request.py
import sys
import json
import subprocess

def main():
    if len(sys.argv) < 3:
        print("Usage: process_request.py <request_file> <response_file>")
        sys.exit(1)

    request_file = sys.argv[1]
    response_file = sys.argv[2]

    # 读取请求
    with open(request_file, 'r') as f:
        request = json.load(f)

    # 执行代码
    runtime = request.get('runtime')
    code = request.get('code')

    if runtime == 'python':
        result = subprocess.run(
            ['python3', '-c', code],
            capture_output=True,
            text=True,
            timeout=60
        )
    elif runtime == 'nodejs':
        result = subprocess.run(
            ['node', '-e', code],
            capture_output=True,
            text=True,
            timeout=60
        )
    elif runtime == 'shell':
        result = subprocess.run(
            code,
            shell=True,
            capture_output=True,
            text=True,
            timeout=60
        )
    else:
        result = None

    # 写入响应
    response = {
        'success': result.returncode == 0 if result else False,
        'stdout': result.stdout if result else '',
        'stderr': result.stderr if result else '',
        'returncode': result.returncode if result else -1
    }

    with open(response_file, 'w') as f:
        json.dump(response, f)

if __name__ == '__main__':
    main()
```

### 实现方案 B: 共享存储 + 文件监听

如果 Unix Socket 太复杂，使用简化的文件 IPC:

```kotlin
class TermuxBridgeSimpleTool(private val context: Context) : Tool {
    private val ipcDir = File("/sdcard/AndroidForClaw/.termux_ipc")

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        ipcDir.mkdirs()

        val requestId = System.currentTimeMillis().toString()
        val requestFile = File(ipcDir, "request_$requestId.json")
        val responseFile = File(ipcDir, "response_$requestId.json")
        val lockFile = File(ipcDir, "lock_$requestId")

        // 写入请求
        requestFile.writeText(JSONObject(args).toString())
        lockFile.createNewFile()

        // 触发 Termux 处理
        val intent = Intent().apply {
            action = "com.termux.RUN_COMMAND"
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(
                "-c",
                "python3 ~/.termux/phoneforclaw_processor.py ${requestFile.absolutePath} ${responseFile.absolutePath} ${lockFile.absolutePath}"
            ))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }

        try {
            context.startService(intent)
        } catch (e: Exception) {
            return@withContext ToolResult(
                success = false,
                error = "Failed to start Termux: ${e.message}"
            )
        }

        // 等待响应 (最多30秒)
        var attempts = 0
        while (lockFile.exists() && attempts < 60) {
            delay(500)
            attempts++
        }

        if (!responseFile.exists()) {
            return@withContext ToolResult(
                success = false,
                error = "Timeout: Termux did not respond"
            )
        }

        // 读取响应
        val response = JSONObject(responseFile.readText())

        // 清理
        requestFile.delete()
        responseFile.delete()
        lockFile.delete()

        ToolResult(
            success = response.optBoolean("success", false),
            data = mapOf(
                "stdout" to response.optString("stdout", ""),
                "stderr" to response.optString("stderr", "")
            )
        )
    }
}
```

### Termux 处理器

```python
# ~/.termux/phoneforclaw_processor.py
import sys
import json
import subprocess
import os

request_file = sys.argv[1]
response_file = sys.argv[2]
lock_file = sys.argv[3]

try:
    # 读取请求
    with open(request_file, 'r') as f:
        request = json.load(f)

    runtime = request.get('runtime')
    code = request.get('code')

    # 执行
    if runtime == 'python':
        proc = subprocess.run(['python3', '-c', code],
                            capture_output=True, text=True, timeout=60)
    elif runtime == 'nodejs':
        proc = subprocess.run(['node', '-e', code],
                            capture_output=True, text=True, timeout=60)
    elif runtime == 'shell':
        proc = subprocess.run(code, shell=True,
                            capture_output=True, text=True, timeout=60)

    # 写入响应
    response = {
        'success': proc.returncode == 0,
        'stdout': proc.stdout,
        'stderr': proc.stderr
    }

    with open(response_file, 'w') as f:
        json.dump(response, f)

finally:
    # 删除锁文件，表示处理完成
    if os.path.exists(lock_file):
        os.remove(lock_file)
```

### 用户设置指南

**安装和配置**

```bash
# 1. 安装 Termux (F-Droid 版本)
# https://f-droid.org/en/packages/com.termux/

# 2. 在 Termux 中安装依赖
pkg update
pkg install python nodejs

# 3. 安装常用库
pip install requests pandas beautifulsoup4 numpy
npm install -g axios cheerio lodash

# 4. 下载 PhoneForClaw Bridge 脚本
curl -o ~/.termux/phoneforclaw_processor.py \
  https://raw.githubusercontent.com/your-repo/termux-bridge/main/processor.py

chmod +x ~/.termux/phoneforclaw_processor.py

# 5. 配置存储权限
termux-setup-storage

# 6. 测试
python3 -c "print('Termux is ready!')"
```

### 优势

✅ **完整生态**: Python + Node.js + 所有包管理器
✅ **零 APK 增量**: PhoneForClaw 不需要嵌入解释器
✅ **性能优秀**: 原生 Python/Node.js
✅ **用户可控**: 用户决定安装什么包
✅ **社区成熟**: Termux 生态很成熟

### 劣势

⚠️ 需要用户安装 Termux
⚠️ IPC 通信有延迟 (500ms-2s)
⚠️ 需要配置脚本

---

## 方案 2: 嵌入式 JavaScript 引擎 ⭐⭐⭐⭐

### 核心思路

嵌入 QuickJS 或 J2V8，提供完整的 JavaScript 运行时。

### 技术选型

#### 选项 A: QuickJS (推荐)

```gradle
// app/build.gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
        }
    }
}

dependencies {
    // QuickJS Android 绑定
    implementation 'app.cash.quickjs:quickjs-android:0.9.2'
}
```

**实现 JavaScriptExecutorTool**

```kotlin
// JavaScriptExecutorTool.kt
import app.cash.quickjs.QuickJs

class JavaScriptExecutorTool : Tool {
    override val name = "js_exec"
    override val description = "Execute JavaScript code with full Node.js-like capabilities"

    private val quickJs = QuickJs.create()

    init {
        // 注入常用库
        injectLibraries()
    }

    private fun injectLibraries() {
        // 注入 lodash-like 工具函数
        quickJs.evaluate("""
            const _ = {
                map: (arr, fn) => arr.map(fn),
                filter: (arr, fn) => arr.filter(fn),
                reduce: (arr, fn, init) => arr.reduce(fn, init),
                groupBy: (arr, key) => arr.reduce((acc, item) => {
                    (acc[item[key]] = acc[item[key]] || []).push(item);
                    return acc;
                }, {}),
                uniq: (arr) => [...new Set(arr)],
                flatten: (arr) => arr.flat(),
                chunk: (arr, size) => {
                    const result = [];
                    for (let i = 0; i < arr.length; i += size) {
                        result.push(arr.slice(i, i + size));
                    }
                    return result;
                }
            };

            // CSV 解析
            function parseCSV(text) {
                return text.trim().split('\\n').map(line => line.split(','));
            }

            // HTTP 请求 (通过 Android 桥接)
            async function fetch(url, options = {}) {
                return await Android.httpRequest(url, JSON.stringify(options));
            }

            // 文件操作 (通过 Android 桥接)
            const fs = {
                readFile: (path) => Android.readFile(path),
                writeFile: (path, content) => Android.writeFile(path, content),
                exists: (path) => Android.fileExists(path)
            };
        """.trimIndent())
    }

    override suspend fun execute(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val code = args["code"] as? String ?: return@withContext ToolResult(
            success = false,
            error = "Missing code parameter"
        )

        try {
            // 包装代码使其支持 async/await
            val wrappedCode = """
                (async function() {
                    try {
                        const result = await (async function() {
                            $code
                        })();
                        return { success: true, result: result };
                    } catch (error) {
                        return { success: false, error: error.message };
                    }
                })();
            """.trimIndent()

            val result = quickJs.evaluate(wrappedCode)

            ToolResult(
                success = true,
                data = mapOf("result" to result)
            )

        } catch (e: Exception) {
            ToolResult(
                success = false,
                error = "JavaScript execution failed: ${e.message}"
            )
        }
    }

    /**
     * 注入 Android 桥接接口
     */
    fun injectAndroidBridge(bridge: AndroidBridge) {
        quickJs.set("Android", AndroidBridge::class.java, bridge)
    }

    override fun close() {
        quickJs.close()
    }
}

/**
 * JavaScript 调用 Android 的桥接类
 */
class AndroidBridge(private val context: Context) {
    @JavascriptInterface
    fun httpRequest(url: String, optionsJson: String): String {
        // 实现 HTTP 请求
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    @JavascriptInterface
    fun readFile(path: String): String {
        return File(path).readText()
    }

    @JavascriptInterface
    fun writeFile(path: String, content: String): Boolean {
        return try {
            File(path).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
}
```

### 使用示例

```kotlin
// 数据分析
val result = jsExecutor.execute(mapOf(
    "code" to """
        const data = JSON.parse(fs.readFile('/sdcard/data.json'));

        const summary = {
            total: data.length,
            categories: _.uniq(data.map(d => d.category)),
            avgValue: _.reduce(data, (sum, d) => sum + d.value, 0) / data.length,
            grouped: _.groupBy(data, 'category')
        };

        return summary;
    """
))

// 网页爬取
val result = jsExecutor.execute(mapOf(
    "code" to """
        const html = await fetch('https://example.com').then(r => r.text());

        // 简单的 HTML 解析
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const titles = Array.from(doc.querySelectorAll('h2')).map(el => el.textContent);

        return titles;
    """
))
```

### 优势

✅ **体积小**: +5-10 MB
✅ **性能好**: QuickJS 很快
✅ **完全独立**: 不依赖外部应用
✅ **生态丰富**: 可以移植很多 npm 库

### 劣势

⚠️ 不是完整的 Node.js (没有 fs, http 等模块)
⚠️ 需要手动实现桥接
⚠️ 异步支持有限

---

## 方案 3: Chaquopy Python ⭐⭐⭐

### 实现

```gradle
// build.gradle (project)
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url "https://chaquo.com/maven" }
    }
    dependencies {
        classpath "com.chaquo.python:gradle:15.0.1"
    }
}

// app/build.gradle
plugins {
    id 'com.chaquo.python'
}

android {
    defaultConfig {
        python {
            pip {
                install "requests"
                install "beautifulsoup4"
                install "pandas"
                install "numpy"
            }
        }

        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
        }
    }

    splits {
        abi {
            enable true
            reset()
            include "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
            universalApk true
        }
    }
}
```

```kotlin
class PythonExecutorTool : Tool {
    private val python = Python.getInstance()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val code = args["code"] as String

        try {
            val module = python.getModule("__main__")
            module.callAttr("exec", code)

            // 获取结果 (如果有)
            val result = module.get("result")

            return ToolResult(
                success = true,
                data = mapOf("result" to result?.toString())
            )
        } catch (e: Exception) {
            return ToolResult(
                success = false,
                error = e.message
            )
        }
    }
}
```

### 优劣

✅ 真正的 Python 3.8+
✅ pip 包管理
❌ APK 巨大 (+50-80 MB)
❌ 商业许可 (免费版有限制)

---

## 方案 4: Lua/LuaJ 脚本 ⭐⭐

### 实现

```gradle
dependencies {
    implementation 'org.luaj:luaj-jse:3.0.1'
}
```

```kotlin
class LuaExecutorTool : Tool {
    private val globals = JsePlatform.standardGlobals()

    override suspend fun execute(args: Map<String, Any?>): ToolResult {
        val code = args["code"] as String

        try {
            val result = globals.load(code).call()
            return ToolResult(
                success = true,
                data = mapOf("result" to result.toString())
            )
        } catch (e: Exception) {
            return ToolResult(success = false, error = e.message)
        }
    }
}
```

### 优劣

✅ 体积极小 (+1 MB)
✅ 性能优秀
❌ 生态极小
❌ 不适合复杂任务

---

## 方案 5: 混合方案 (最佳实践) ⭐⭐⭐⭐⭐

### 核心思路

**根据任务类型选择执行环境**

```
┌────────────────────────────────────┐
│      PhoneForClaw Agent            │
│                                    │
│  Task Classification:              │
│  - 数据分析 → Termux Python        │
│  - 网页处理 → JavaScript (QuickJS) │
│  - 轻量计算 → Lua                  │
│  - 系统操作 → Shell                │
└────────────────────────────────────┘
```

### 实现: 智能执行调度器

```kotlin
class SmartExecutor(private val context: Context) {
    private val termuxBridge = TermuxBridgeTool(context)
    private val jsExecutor = JavaScriptExecutorTool()
    private val luaExecutor = LuaExecutorTool()

    /**
     * 智能选择执行环境
     */
    suspend fun execute(task: ExecutionTask): ToolResult {
        return when (classifyTask(task)) {
            TaskType.DATA_ANALYSIS -> {
                // 优先使用 Termux Python
                if (termuxBridge.isAvailable()) {
                    termuxBridge.execute(mapOf(
                        "runtime" to "python",
                        "code" to task.code
                    ))
                } else {
                    // 降级到 JavaScript
                    jsExecutor.execute(mapOf("code" to task.code))
                }
            }

            TaskType.WEB_SCRAPING -> {
                // JavaScript 最适合 DOM 操作
                jsExecutor.execute(mapOf("code" to task.code))
            }

            TaskType.LIGHT_COMPUTE -> {
                // Lua 最快
                luaExecutor.execute(mapOf("code" to task.code))
            }

            TaskType.SYSTEM_OPERATION -> {
                // 使用 Shell
                termuxBridge.execute(mapOf(
                    "runtime" to "shell",
                    "code" to task.code
                ))
            }

            else -> {
                ToolResult(success = false, error = "Unknown task type")
            }
        }
    }

    private fun classifyTask(task: ExecutionTask): TaskType {
        return when {
            task.code.contains("pandas") ||
            task.code.contains("numpy") -> TaskType.DATA_ANALYSIS

            task.code.contains("fetch") ||
            task.code.contains("querySelector") -> TaskType.WEB_SCRAPING

            task.code.contains("for") &&
            task.code.length < 500 -> TaskType.LIGHT_COMPUTE

            task.code.contains("ls") ||
            task.code.contains("grep") -> TaskType.SYSTEM_OPERATION

            else -> TaskType.GENERAL
        }
    }
}

enum class TaskType {
    DATA_ANALYSIS,
    WEB_SCRAPING,
    LIGHT_COMPUTE,
    SYSTEM_OPERATION,
    GENERAL
}
```

---

## 推荐实施路径

### 阶段 1: 基础支持 (Week 1-2)

1. **实现 Termux Bridge**
   - 文件 IPC 通信
   - Python/Node.js/Shell 执行
   - 错误处理

2. **用户文档**
   - Termux 安装指南
   - 依赖配置脚本
   - 测试用例

### 阶段 2: JavaScript 增强 (Week 3-4)

3. **集成 QuickJS**
   - 嵌入 QuickJS 引擎
   - 注入工具库
   - Android 桥接

4. **实现常用功能**
   - HTTP 客户端
   - 文件操作
   - 数据处理库

### 阶段 3: 智能调度 (Week 5-6)

5. **Smart Executor**
   - 任务分类逻辑
   - 自动降级策略
   - 性能优化

6. **Skills 扩展**
   - 创建针对性 Skills
   - 教 Agent 选择合适的执行环境

---

## 最终推荐

### 🏆 首选: Termux Bridge + QuickJS

**理由**:
- ✅ Termux 提供完整生态 (Python + Node.js + 所有包)
- ✅ QuickJS 提供嵌入式 JavaScript (轻量、快速)
- ✅ 组合覆盖 95% 使用场景
- ✅ APK 增量小 (+10 MB for QuickJS only)
- ✅ 用户可选 (有 Termux 更强,没有也能用)

**实施成本**: 2-3 周
**APK 增量**: +10 MB (仅 QuickJS)
**能力覆盖**: ⭐⭐⭐⭐⭐

---

## 总结对比

| 方案 | 代码能力 | APK大小 | 用户门槛 | 推荐度 |
|------|---------|---------|---------|--------|
| Termux 单独 | ⭐⭐⭐⭐⭐ | +0 MB | 中 | ⭐⭐⭐⭐ |
| QuickJS 单独 | ⭐⭐⭐ | +10 MB | 低 | ⭐⭐⭐ |
| **Termux + QuickJS** | **⭐⭐⭐⭐⭐** | **+10 MB** | **低** | **⭐⭐⭐⭐⭐** |
| Chaquopy | ⭐⭐⭐⭐ | +60 MB | 低 | ⭐⭐ |
| Lua | ⭐⭐ | +1 MB | 低 | ⭐⭐ |

**最佳实践**: Termux Bridge (可选) + QuickJS (必选) + 智能调度器
