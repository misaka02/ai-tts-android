package com.aitts.engine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ReplacementRule
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.theme.PrimaryBlue
import java.util.UUID

@Composable
fun RulesScreen(configDataStore: ConfigDataStore) {
    val rules by configDataStore.rulesFlow.collectAsState()

    var testInput by remember { mutableStateOf("他在重庆的一家银行工作，【突然】……") }
    var editingRule by remember { mutableStateOf<ReplacementRule?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val processedOutput = remember(testInput, rules) {
        TextPreprocessor.process(testInput, rules)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRule = ReplacementRule(
                        id = "rule_${UUID.randomUUID().toString().take(8)}",
                        pattern = "",
                        replacement = "",
                        isRegex = false,
                        description = ""
                    )
                    showDialog = true
                },
                containerColor = PrimaryBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增规则")
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
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "文本正则与发音纠正",
                    subtitle = "用于多音字读音修正、清理小说特殊排版符号等"
                )

                // 实时规则测试对比卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("规则实时预览器", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = testInput,
                            onValueChange = { testInput = it },
                            label = { Text("输入测试文本") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "清洗替换后: $processedOutput",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("规则清单 (${rules.size})", fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        configDataStore.saveRules(PresetConfigs.defaultRules)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("恢复预设")
                    }
                }
            }

            items(rules) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { enabled ->
                                configDataStore.updateRule(rule.copy(enabled = enabled))
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rule.pattern} ➔ ${rule.replacement.ifBlank { "（删除）" }}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (rule.description.isNotBlank()) {
                                Text(
                                    text = rule.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (rule.isRegex) "正则表达式" else "普通匹配",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        IconButton(onClick = {
                            editingRule = rule
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = PrimaryBlue)
                        }

                        IconButton(onClick = { configDataStore.deleteRule(rule.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showDialog && editingRule != null) {
        var pattern by remember { mutableStateOf(editingRule!!.pattern) }
        var replacement by remember { mutableStateOf(editingRule!!.replacement) }
        var isRegex by remember { mutableStateOf(editingRule!!.isRegex) }
        var description by remember { mutableStateOf(editingRule!!.description) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("编辑替换规则") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("匹配词 / 正则表达式") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = { replacement = it },
                        label = { Text("替换为 (可留空以删除)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("规则说明") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                        Text("启用正则表达式")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        configDataStore.updateRule(
                            editingRule!!.copy(
                                pattern = pattern,
                                replacement = replacement,
                                isRegex = isRegex,
                                description = description
                            )
                        )
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
