package com.aitts.engine.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ReplacementRule
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.theme.SuccessGreen
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(configDataStore: ConfigDataStore) {
    val context = LocalContext.current
    val rules by configDataStore.rulesFlow.collectAsState()

    var testInput by remember { mutableStateOf("他在重庆的一家银行工作，参差的关卡前他便宜行事。") }
    var editingRule by remember { mutableStateOf<ReplacementRule?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }

    val processedOutput = remember(testInput, rules) {
        TextPreprocessor.process(testInput, rules)
    }

    val filteredRules = rules.filter { rule ->
        val matchesSearch = rule.pattern.contains(searchQuery, ignoreCase = true) ||
                rule.replacement.contains(searchQuery, ignoreCase = true) ||
                rule.description.contains(searchQuery, ignoreCase = true)

        val matchesCat = when (selectedCategory) {
            "多音字纠错" -> rule.description.contains("多音字") || !rule.isRegex
            "排版与水印" -> rule.description.contains("排版") || rule.description.contains("水印") || rule.description.contains("省略号")
            "已启用" -> rule.enabled
            else -> true
        }

        matchesSearch && matchesCat
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
                containerColor = MaterialTheme.colorScheme.primary,
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
                    subtitle = "多音字纠音、排版符号过滤、兼容「阅读」App 替换规则无缝导入"
                )

                // 实时规则测试对比卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
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
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 快捷工具栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showImportDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("导入规则", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val json = configDataStore.exportRulesJson()
                                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                if (!downloadDir.exists()) downloadDir.mkdirs()
                                val file = File(downloadDir, "AI_TTS_Rules_${System.currentTimeMillis()}.json")
                                file.writeBytes(json.toByteArray(Charsets.UTF_8))
                                Toast.makeText(context, "规则已成功导出至 Downloads/${file.name}", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("导出文件", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            val merged = (rules + PresetConfigs.defaultRules).distinctBy { it.pattern }
                            configDataStore.saveRules(merged)
                            Toast.makeText(context, "已合并官方精品发音词库 (${merged.size}条)", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("合并词库", fontSize = 11.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索替换词 / 拼音 / 规则说明...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 分类 Filter Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("全部", "多音字纠错", "排版与水印", "已启用").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }

            item(contentType = "list_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("规则清单 (${filteredRules.size}/${rules.size})", fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        configDataStore.saveRules(PresetConfigs.defaultRules)
                        Toast.makeText(context, "已重置为默认预设", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("恢复预设")
                    }
                }
            }

            if (filteredRules.isEmpty()) {
                item(contentType = "empty_placeholder") {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("未找到符合条件的替换规则", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(filteredRules, key = { it.id }, contentType = { "rule_card" }) { rule ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${rule.pattern} ➔ ${rule.replacement.ifBlank { "（删除）" }}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (rule.isRegex) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (rule.isRegex) "正则" else "纯文本",
                                            color = if (rule.isRegex) MaterialTheme.colorScheme.primary else SuccessGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (rule.description.isNotBlank()) {
                                    Text(
                                        text = rule.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = {
                                val cloned = rule.copy(id = "rule_${UUID.randomUUID().toString().take(8)}")
                                configDataStore.updateRule(cloned)
                                Toast.makeText(context, "已复制规则", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制", tint = MaterialTheme.colorScheme.outline)
                            }

                            IconButton(onClick = {
                                editingRule = rule
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = { configDataStore.deleteRule(rule.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 导入阅读 (Legado) 规则弹窗
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入「阅读」或 JSON 替换规则") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "支持直接粘贴「阅读 3.0」导出的替换规则 JSON 数组或对象：",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("粘贴规则 JSON") },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val parsedRules = mutableListOf<ReplacementRule>()
                            val trimmed = importJsonText.trim()
                            if (trimmed.startsWith("[")) {
                                val array = JSONArray(trimmed)
                                for (i in 0 until array.length()) {
                                    val obj = array.optJSONObject(i) ?: continue
                                    val pattern = obj.optString("pattern", obj.optString("regex", ""))
                                    val replacement = obj.optString("replacement", obj.optString("replacement", ""))
                                    val name = obj.optString("name", obj.optString("description", ""))
                                    val isRegex = obj.optBoolean("isRegex", obj.optInt("isRegex", 1) == 1)
                                    if (pattern.isNotBlank()) {
                                        parsedRules.add(
                                            ReplacementRule(
                                                id = "rule_${UUID.randomUUID().toString().take(8)}",
                                                pattern = pattern,
                                                replacement = replacement,
                                                isRegex = isRegex,
                                                description = name
                                            )
                                        )
                                    }
                                }
                            } else if (trimmed.startsWith("{")) {
                                val obj = JSONObject(trimmed)
                                val pattern = obj.optString("pattern", obj.optString("regex", ""))
                                val replacement = obj.optString("replacement", "")
                                val name = obj.optString("name", "")
                                val isRegex = obj.optBoolean("isRegex", true)
                                if (pattern.isNotBlank()) {
                                    parsedRules.add(
                                        ReplacementRule(
                                            id = "rule_${UUID.randomUUID().toString().take(8)}",
                                            pattern = pattern,
                                            replacement = replacement,
                                            isRegex = isRegex,
                                            description = name
                                        )
                                    )
                                }
                            }

                            if (parsedRules.isNotEmpty()) {
                                val merged = (rules + parsedRules).distinctBy { it.pattern }
                                configDataStore.saveRules(merged)
                                Toast.makeText(context, "成功导入 ${parsedRules.size} 条规则！", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                                importJsonText = ""
                            } else {
                                Toast.makeText(context, "未解析到有效的替换规则", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "JSON 解析失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("开始导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
