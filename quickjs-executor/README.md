# QuickJS Executor Module

> **独立的 JavaScript 执行器模块**，基于 QuickJS 引擎，为 Android 应用提供完整的 JavaScript 运行时环境。

---

## 📦 模块信息

- **Package**: `com.xiaomo.quickjs`
- **引擎**: QuickJS (轻量级 JavaScript 引擎)
- **语言**: Kotlin + JavaScript
- **APK 增量**: ~10 MB
- **最低 SDK**: 26 (Android 8.0)

---

## ✨ 功能特性

### JavaScript 引擎
- ✅ ES6+ 语法支持
- ✅ async/await 和 Promise
- ✅ 完整的 Array/Object 方法
- ✅ 正则表达式
- ✅ JSON 解析
- ✅ Math 运算

### 内置工具库
- **Lodash-like**: map, filter, reduce, groupBy, sum, mean, etc.
- **文件系统**: readFile, writeFile, exists, listDir, delete
- **HTTP 客户端**: fetch API (GET/POST/PUT/DELETE)
- **数据解析**: CSV, JSON
- **字符串工具**: camelCase, snakeCase, kebabCase
- **实用函数**: retry, timeout, sleep

### Android 桥接
- ✅ 文件读写 (/sdcard 访问)
- ✅ HTTP 请求 (OkHttp)
- ✅ 目录列表
- ✅ 日志输出
- ✅ 时间戳获取

---

## 🚀 快速开始

### 1. 添加依赖

在项目的 `settings.gradle` 中添加:

```gradle
include ':quickjs-executor'
```

在 app 的 `build.gradle` 中添加:

```gradle
dependencies {
    implementation project(':quickjs-executor')
}
```

### 2. 使用示例

#### 基础用法

```kotlin
import com.xiaomo.quickjs.QuickJSExecutor
import kotlinx.coroutines.runBlocking

val executor = QuickJSExecutor(context)

// 执行 JavaScript 代码
val result = runBlocking {
    executor.execute("return 1 + 1;")
}

if (result.success) {
    println("Result: ${result.result}") // Output: 2
} else {
    println("Error: ${result.error}")
}
```

#### 数据处理

```kotlin
val result = executor.execute("""
    const data = [
        { name: 'John', age: 30 },
        { name: 'Jane', age: 25 },
        { name: 'Bob', age: 30 }
    ];

    const grouped = _.groupBy(data, 'age');
    const avgAge = _.mean(data.map(d => d.age));

    return JSON.stringify({ grouped, avgAge });
""")
```

#### 文件操作

```kotlin
val result = executor.execute("""
    // 读取文件
    const content = fs.readFile('/sdcard/data.json');
    const data = JSON.parse(content);

    // 处理数据
    const processed = data.map(item => ({
        ...item,
        processed: true
    }));

    // 保存结果
    fs.writeFile('/sdcard/output.json', JSON.stringify(processed, null, 2));

    return { count: processed.length };
""")
```

#### HTTP 请求

```kotlin
val result = executor.execute("""
    const response = await fetch('https://api.example.com/users');
    const users = await response.json();

    const activeUsers = users.filter(u => u.status === 'active');

    return { total: users.length, active: activeUsers.length };
""")
```

---

## 📚 API 文档

### QuickJSExecutor

#### 构造函数

```kotlin
QuickJSExecutor(context: Context)
```

#### 方法

##### execute()

执行 JavaScript 代码。

```kotlin
suspend fun execute(
    code: String,
    timeout: Long = 30000
): ExecutionResult
```

**参数**:
- `code`: JavaScript 代码字符串
- `timeout`: 超时时间(毫秒)，默认 30000

**返回**: `ExecutionResult`

```kotlin
data class ExecutionResult(
    val success: Boolean,
    val result: String? = null,
    val error: String? = null,
    val metadata: Map<String, Any?> = emptyMap()
)
```

##### cleanup()

清理资源，释放 QuickJS 引擎。

```kotlin
fun cleanup()
```

---

## 🛠️ 内置 JavaScript API

### 工具函数 (_)

```javascript
// 数组操作
_.map([1,2,3], x => x * 2)          // [2, 4, 6]
_.filter([1,2,3,4], x => x > 2)     // [3, 4]
_.reduce([1,2,3], (a,b) => a+b, 0)  // 6
_.uniq([1,2,2,3])                   // [1, 2, 3]
_.flatten([[1,2],[3,4]])            // [1,2,3,4]
_.chunk([1,2,3,4], 2)               // [[1,2], [3,4]]

// 对象操作
_.groupBy(users, 'role')            // 按字段分组
_.countBy(items, 'category')        // 按字段计数
_.keyBy(users, 'id')                // 按字段索引

// 统计
_.sum([1,2,3,4])                    // 10
_.mean([1,2,3,4])                   // 2.5
_.max([1,5,3])                      // 5
_.min([1,5,3])                      // 1

// 字符串
_.capitalize('hello')               // 'Hello'
_.camelCase('hello-world')          // 'helloWorld'
_.snakeCase('helloWorld')           // 'hello_world'
_.kebabCase('helloWorld')           // 'hello-world'
```

### 文件系统 (fs)

```javascript
// 读文件
const content = fs.readFile('/sdcard/file.txt');

// 写文件
fs.writeFile('/sdcard/output.txt', 'Hello World');

// 检查存在
if (fs.exists('/sdcard/file.txt')) { ... }

// 列出目录
const files = fs.listDir('/sdcard/Documents');
// 返回: [{ name, path, isDirectory, size, lastModified }, ...]

// 删除文件
fs.delete('/sdcard/temp.txt');
```

### HTTP 客户端 (fetch)

```javascript
// GET 请求
const response = await fetch('https://api.example.com/data');
const data = await response.json();

// POST 请求
const response = await fetch('https://api.example.com/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: 'John' })
});

// 响应属性
response.ok           // boolean
response.status       // number
response.text()       // Promise<string>
response.json()       // Promise<object>
```

### 数据解析

```javascript
// CSV 解析
const data = parseCSV(csvText, { hasHeader: true });
// 返回: [{ col1: 'val1', col2: 'val2' }, ...]

// 安全 JSON 解析
const obj = parseJSON('{"key":"value"}', {});  // 失败返回默认值
```

### 系统工具 (System)

```javascript
// 日志
System.log('Processing...');

// 时间戳
const now = System.currentTimeMillis();

// 延迟
await System.sleep(1000);  // 1 秒
```

### 实用函数

```javascript
// 重试
await retry(async () => {
    return await fetch('https://api.example.com');
}, { maxAttempts: 3, delay: 1000 });

// 超时
await timeout(longTask(), 5000);  // 5 秒超时
```

---

## 🧪 测试

运行测试:

```bash
./gradlew :quickjs-executor:test
```

测试覆盖:
- ✅ 基础运算
- ✅ 数组操作
- ✅ 对象操作
- ✅ async/await
- ✅ CSV 解析
- ✅ 字符串处理
- ✅ 统计函数
- ✅ 错误处理

---

## 📊 性能指标

| 指标 | 数值 |
|------|------|
| 启动时间 | ~50ms |
| 简单计算 | <10ms |
| 文件读写 | ~100ms |
| HTTP 请求 | 网络延迟 |
| 内存占用 | ~5-10 MB |
| APK 增量 | ~10 MB |

---

## ⚠️ 限制

1. **无 npm 包**: 不支持 require() 或 import
2. **无 DOM API**: 不能操作浏览器 DOM
3. **无 Node.js 模块**: 不支持 fs, path, os 等模块
4. **文件访问限制**: 仅能访问 /sdcard 和应用目录

---

## 🔧 高级用法

### 自定义桥接

可以扩展 `QuickJSBridge` 添加更多 Android 功能:

```kotlin
class CustomBridge(context: Context) : QuickJSBridge(context) {

    @Suppress("unused")
    fun customFunction(arg: String): String {
        // 自定义实现
        return "Result: $arg"
    }
}
```

### 预加载脚本

```kotlin
val executor = QuickJSExecutor(context)

// 预加载常用库
executor.execute("""
    // 自定义工具函数
    function myHelper(data) {
        return data.map(x => x * 2);
    }
""")

// 后续代码可以使用 myHelper
val result = executor.execute("return myHelper([1,2,3]);")
```

---

## 📖 示例项目

参考 phoneforclaw 主应用的集成方式:

```
phoneforclaw/
├── app/
│   └── src/main/java/.../tools/
│       └── JavaScriptExecutorTool.kt    # Tool 包装
└── quickjs-executor/                     # 独立 module
    ├── src/main/java/com/xiaomo/quickjs/
    │   ├── QuickJSExecutor.kt
    │   └── QuickJSBridge.kt
    └── build.gradle
```

---

## 📄 License

MIT License

---

## 🙏 致谢

- [QuickJS](https://bellard.org/quickjs/) - 轻量级 JavaScript 引擎
- [quickjs-android](https://github.com/cashapp/quickjs-android) - Android QuickJS 绑定

---

**QuickJS Executor** - Powered by QuickJS ⚡
