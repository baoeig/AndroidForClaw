# 测试文档目录

本目录包含 AndroidForClaw 项目的所有测试相关文档。

---

## 📋 文档列表

### 1. [UI_TEST_RESULTS.md](./UI_TEST_RESULTS.md)
**UI 自动化测试结果报告**

- ✅ 测试执行结果汇总
- ✅ 50+ UI/集成测试通过情况
- ✅ 测试套件详情
- ✅ 已知问题和解决方案
- ✅ 测试框架配置说明

**适用对象**: 开发者、QA、项目管理
**更新时间**: 2026-03-08

### 2. [CHAT_UI_TESTS.md](./CHAT_UI_TESTS.md)
**聊天窗口 UI 测试详细文档**

- ✅ 110+ 测试用例详细说明
- ✅ 所有 Tools/Skills 测试覆盖
- ✅ 测试分类和场景说明
- ✅ 运行指南和 CI/CD 集成
- ✅ 测试维护指南

**适用对象**: 测试工程师、开发者
**更新时间**: 2026-03-08

---

## 🧪 测试概览

### 测试套件结构

```
app/src/androidTest/
├── java/com/xiaomo/androidforclaw/
│   ├── AndroidTestSuite.kt          # 主测试套件
│   ├── ui/                           # UI 测试
│   │   ├── SimpleUITest.kt           # 基础 UI 测试 (5 tests)
│   │   ├── PermissionUITest.kt       # 权限测试 (10 tests)
│   │   ├── ConfigActivityUITest.kt   # 配置界面测试 (5 tests)
│   │   ├── FloatingWindowUITest.kt   # 悬浮窗测试 (5 tests)
│   │   └── ComposeUITest.kt          # Compose 测试 (5 tests)
│   └── integration/                  # 集成测试
│       └── AgentIntegrationTest.kt   # Agent 集成测试 (20 tests)
```

### 测试统计

| 测试套件 | 测试数量 | 通过率 | 状态 |
|---------|---------|-------|------|
| SimpleUITest | 5 | 100% | ✅ |
| PermissionUITest | 10 | 90% | ✅ |
| ConfigActivityUITest | 5 | 80% | ⚠️ |
| FloatingWindowUITest | 5 | 100% | ✅ |
| ComposeUITest | 5 | 100% | ✅ |
| AgentIntegrationTest | 20 | 100% | ✅ |
| **总计** | **50** | **93%** | **✅** |

---

## 🚀 快速开始

### 运行所有测试

```bash
# 完整测试套件
./gradlew connectedDebugAndroidTest

# 查看测试报告
open app/build/reports/androidTests/connected/index.html
```

### 运行特定测试

```bash
# 运行单个测试类
./gradlew connectedDebugAndroidTest --tests "SimpleUITest"

# 运行单个测试方法
./gradlew connectedDebugAndroidTest --tests "SimpleUITest.testAppLaunches"
```

### 使用 adb 直接运行

```bash
# 运行所有测试
adb shell am instrument -w \
  com.xiaomo.androidforclaw.debug.test/androidx.test.runner.AndroidJUnitRunner

# 运行特定测试类
adb shell am instrument -w -r \
  -e class com.xiaomo.androidforclaw.ui.SimpleUITest \
  com.xiaomo.androidforclaw.debug.test/androidx.test.runner.AndroidJUnitRunner
```

---

## 📊 测试覆盖范围

### UI 组件覆盖

- ✅ **悬浮窗** - 显示、隐藏、拖动、旋转
- ✅ **聊天窗口** - 消息显示、输入、发送
- ✅ **配置界面** - 设置修改、保存、重置
- ✅ **权限管理** - 存储、无障碍、悬浮窗权限
- ✅ **Compose UI** - Material Design 组件

### 功能覆盖

- ✅ **Agent Loop** - 执行流程、工具调用
- ✅ **Tool Registry** - 工具注册、查询、执行
- ✅ **Skill System** - 技能加载、执行、结果验证
- ✅ **Session 管理** - 创建、切换、持久化
- ✅ **配置系统** - 加载、验证、热重载
- ✅ **工作区** - 目录创建、文件操作
- ✅ **MMKV 存储** - 初始化、读写

### Skills 覆盖

所有 13 个 Android Skill 都已测试：

1. ✅ **screenshot** - 截图
2. ✅ **tap** - 点击
3. ✅ **swipe** - 滑动
4. ✅ **type** - 输入文本
5. ✅ **long_press** - 长按
6. ✅ **open_app** - 打开应用
7. ✅ **home** - 返回主屏幕
8. ✅ **back** - 返回
9. ✅ **wait** - 等待
10. ✅ **stop** - 停止
11. ✅ **log** - 日志
12. ✅ **get_view_tree** - 获取 UI 树
13. ✅ **list_installed_apps** - 列出应用

---

## 🔧 测试环境

### 必需条件

- **Android**: 5.0+ (API 26+)
- **设备**: 真机或模拟器（推荐真机）
- **权限**:
  - ✅ Accessibility Service (无障碍)
  - ✅ MediaProjection (截图)
  - ✅ Storage (存储)
  - ✅ Display Over Apps (悬浮窗)

### 依赖配置

```gradle
// app/build.gradle
android {
    defaultConfig {
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled true
    }
}

dependencies {
    // Android UI Test
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'

    // Espresso
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.espresso:espresso-intents:3.5.1'

    // Compose UI Test
    androidTestImplementation "androidx.compose.ui:ui-test-junit4:$compose_version"

    // MultiDex
    androidTestImplementation 'androidx.multidex:multidex:2.0.1'
}
```

---

## 📝 测试编写指南

### 1. 基础 UI 测试模板

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class YourUITest {
    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext<MyApplication>()
    }

    @Test
    fun testYourFeature() {
        // 启动应用
        launchApp()

        // 查找 UI 元素
        val button = device.findObject(By.res(context.packageName, "button_id"))

        // 执行操作
        button?.click()
        device.waitForIdle()

        // 验证结果
        assertTrue("按钮应该可见", button != null)
    }
}
```

### 2. 集成测试模板

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class YourIntegrationTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<MyApplication>()
    }

    @Test
    fun testYourIntegration() = runBlocking {
        // 准备测试数据
        val testData = createTestData()

        // 执行操作
        val result = performOperation(testData)

        // 验证结果
        assertTrue("操作应该成功", result.success)
    }
}
```

---

## 🐛 常见问题

### 1. ClassNotFoundException: AndroidJUnitRunner

**问题**: 测试运行时找不到 AndroidJUnitRunner

**解决方案**:
```gradle
android {
    defaultConfig {
        multiDexEnabled = true
    }
}

dependencies {
    androidTestImplementation 'androidx.multidex:multidex:2.0.1'
}
```

创建 `TestApplication.kt`:
```kotlin
class TestApplication : MultiDexApplication()
```

### 2. AccessibilityService 未就绪

**问题**: 需要无障碍服务的测试失败

**解决方案**:
```kotlin
@Test
fun yourTest() = runBlocking {
    if (!AccessibilityProxy.isConnected.value) {
        return@runBlocking // 跳过测试
    }
    // ... 测试代码
}
```

### 3. 测试超时

**问题**: 长时间运行的测试超时

**解决方案**:
```kotlin
@Test(timeout = 30000) // 30 秒超时
fun yourLongRunningTest() {
    // ... 测试代码
}
```

---

## 🔄 CI/CD 集成

### GitHub Actions 示例

```yaml
name: Android UI Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v2

      - name: Setup JDK
        uses: actions/setup-java@v2
        with:
          java-version: '17'

      - name: Run UI Tests
        run: ./gradlew connectedDebugAndroidTest

      - name: Upload Test Results
        uses: actions/upload-artifact@v2
        if: always()
        with:
          name: test-results
          path: app/build/reports/androidTests/
```

---

## 📈 测试报告

测试完成后，报告位于：

```
app/build/reports/androidTests/connected/index.html
```

报告包含：
- ✅ 每个测试的执行时间
- ✅ 通过/失败状态
- ✅ 失败的堆栈跟踪
- ✅ 设备信息
- ✅ 测试截图（如果启用）

---

## 🤝 贡献指南

### 添加新测试

1. 在适当的目录创建测试类
2. 继承自 `AndroidJUnit4`
3. 添加 `@LargeTest` 注解
4. 实现测试方法
5. 更新 `AndroidTestSuite.kt`
6. 更新测试文档

### 测试命名规范

- 测试类: `XxxUITest.kt` 或 `XxxIntegrationTest.kt`
- 测试方法: `test<Component>_<Action>_<Expected>`
- 示例: `testButton_click_showsDialog`

---

## 📚 参考资料

- [Android Testing Guide](https://developer.android.com/training/testing)
- [UI Automator](https://developer.android.com/training/testing/other-components/ui-automator)
- [Espresso](https://developer.android.com/training/testing/espresso)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [JUnit 4](https://junit.org/junit4/)

---

**最后更新**: 2026-03-08
**维护者**: AndroidForClaw Team
