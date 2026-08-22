package com.aitts.engine.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.ui.components.ProviderCard
import com.aitts.engine.ui.components.SectionHeader
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProviderListScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit
) {
    val context = LocalContext.current
    val providers by configDataStore.providersFlow.collectAsState()
    val settings by configDataStore.settingsFlow.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newId = "custom_provider_${UUID.randomUUID().toString().take(8)}"
                    val newConfig = TtsProviderConfig(
                        id = newId,
                        type = ProviderType.MIMO,
                        name = "新建 AI 模型配置",
                        enabled = true,
                        baseUrl = "https://api.xiaomimimo.com/v1/chat/completions",
                        modelName = "mimo-v2.5-tts",
                        voiceId = "茉莉"
                    )
                    configDataStore.updateProvider(newConfig)
                    onNavigateToEditProvider(newId)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增模型")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(contentType = "header") {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "AI 语音模型服务管理",
                    subtitle = "支持上下拖动/移动顺序、一键置顶与克隆多音色副本"
                )
            }

            items(providers, key = { it.id }, contentType = { "provider_card" }) { provider ->
                ProviderCard(
                    provider = provider,
                    isActive = provider.id == settings.activeProviderId,
                    onSelect = {
                        configDataStore.setActiveProviderId(provider.id)
                    },
                    onEdit = {
                        onNavigateToEditProvider(provider.id)
                    },
                    onTest = {
                        onNavigateToEditProvider(provider.id)
                    },
                    onPinTop = {
                        configDataStore.pinProviderToTop(provider.id)
                        Toast.makeText(context, "已将 ${provider.name} 置顶", Toast.LENGTH_SHORT).show()
                    },
                    onMoveUp = {
                        configDataStore.moveProviderUp(provider.id)
                    },
                    onMoveDown = {
                        configDataStore.moveProviderDown(provider.id)
                    },
                    onDuplicate = {
                        configDataStore.duplicateProvider(provider.id)
                        Toast.makeText(context, "已复制配置副本", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        if (providers.size <= 1) {
                            Toast.makeText(context, "至少需要保留一个音色配置", Toast.LENGTH_SHORT).show()
                        } else {
                            configDataStore.deleteProvider(provider.id)
                            Toast.makeText(context, "已删除 ${provider.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
