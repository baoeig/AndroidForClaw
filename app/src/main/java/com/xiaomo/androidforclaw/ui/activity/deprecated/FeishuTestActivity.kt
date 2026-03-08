package com.xiaomo.androidforclaw.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.xiaomo.androidforclaw.test.FeishuConnectionTest
import com.xiaomo.androidforclaw.test.FeishuWebSocketDirectTest
import kotlinx.coroutines.launch

/**
 * 飞书连接测试页面
 */
class FeishuTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FeishuTestScreen(
                    onBack = { finish() },
                    context = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeishuTestScreen(
    onBack: () -> Unit,
    context: android.content.Context
) {
    val scope = rememberCoroutineScope()
    val tester = remember { FeishuConnectionTest(context) }

    var testOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var testType by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("飞书连接测试") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            testOutput = ""
                            testType = null
                        },
                        enabled = !isRunning && testOutput.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Refresh, "清空")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 测试按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "选择测试类型",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // 完整测试
                    Button(
                        onClick = {
                            scope.launch {
                                isRunning = true
                                testType = "full"
                                testOutput = ""

                                tester.runFullTest { message ->
                                    testOutput += message
                                }

                                isRunning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("完整测试 (5 步)")
                    }

                    // 快速检查
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isRunning = true
                                testType = "quick"
                                testOutput = "🚀 运行快速健康检查...\n\n"

                                val result = tester.quickHealthCheck()
                                testOutput += if (result.success) {
                                    "✅ ${result.message}\n"
                                } else {
                                    "❌ ${result.message}\n"
                                }

                                isRunning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRunning
                    ) {
                        Text("快速检查 (仅 API)")
                    }

                    // WebSocket 直接测试
                    Button(
                        onClick = {
                            scope.launch {
                                isRunning = true
                                testType = "websocket"
                                testOutput = ""

                                val result = FeishuWebSocketDirectTest.runDirectTest(context)
                                testOutput = result

                                isRunning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("WebSocket 直接测试")
                    }

                    if (isRunning) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "测试进行中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 测试信息
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "测试说明",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = """
                        完整测试包含：
                        1. 配置加载测试
                        2. 配置验证测试
                        3. FeishuClient 初始化
                        4. Access Token 获取
                        5. WebSocket 连接测试

                        快速检查仅测试配置和 API 连接。
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 输出区域
            if (testOutput.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "测试输出",
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (testType != null) {
                                Text(
                                    text = when (testType) {
                                        "full" -> "完整测试"
                                        "quick" -> "快速检查"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = testOutput,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
