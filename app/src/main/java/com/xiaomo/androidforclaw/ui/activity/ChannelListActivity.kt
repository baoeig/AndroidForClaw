package com.xiaomo.androidforclaw.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tencent.mmkv.MMKV

/**
 * Channel 列表页面
 */
class ChannelListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChannelListScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val mmkv = remember { MMKV.defaultMMKV() }

    // 读取 Feishu Channel 启用状态
    var feishuEnabled by remember {
        mutableStateOf(mmkv.decodeBool("channel_feishu_enabled", false))
    }

    // 读取 Discord Channel 启用状态
    var discordEnabled by remember {
        mutableStateOf(mmkv.decodeBool("channel_discord_enabled", false))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channels") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "返回")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "配置多渠道接入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Feishu Channel 卡片
            ChannelCard(
                name = "Feishu (飞书)",
                description = "飞书群聊和私聊接入",
                enabled = feishuEnabled,
                onClick = {
                    // 跳转到飞书配置页面
                    val intent = Intent(context, FeishuChannelActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // Discord Channel 卡片
            ChannelCard(
                name = "Discord",
                description = "Discord 服务器和私聊接入",
                enabled = discordEnabled,
                onClick = {
                    // 跳转到 Discord 配置页面
                    val intent = Intent(context, DiscordChannelActivity::class.java)
                    context.startActivity(intent)
                }
            )

            // 未来可以添加更多 Channel
            // - WhatsApp Channel
            // - Telegram Channel
            // - Web UI Channel
            // - HTTP API Channel
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelCard(
    name: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (enabled) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已启用",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
