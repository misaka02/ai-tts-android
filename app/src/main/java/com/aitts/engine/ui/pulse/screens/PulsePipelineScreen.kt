package com.aitts.engine.ui.pulse.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ReplacementRule
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import java.util.UUID

/**
 * ⚡ Pulse 正则发音替换流水线 (Pulse Rules Pipeline)
 * 1. 动态正则发音替换规则管理（停启用、匹配模式、替换词）；
 * 2. 单手即时正则流水线测试台；
 * 3. 单规则口令复制与分享。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsePipelineScreen(
    configDataStore: ConfigDataStore
) {
    val context = LocalContext.current
    val rules by configDataStore.rulesFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var testInputText by remember { mutableStateOf("例如输入：第123章，主角生命值-50%，获得了1000g黄金。") }
    var testResultText by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var currentEditingRule by remember { mutableStateOf<ReplacementRule?>(null) }
    var rulePattern by remember { mutableStateOf("") }
    var ruleReplacement by remember { mutableStateOf("") }
    var ruleDescription by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(true) }
    var isCaseSensitive by remember { mutableStateOf(false) }

    fun runTest() {
        testResultText = TextPreprocessor.process(testInputText, rules)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                    Text(
                        text = "发音正则替换流水线",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = PulseTokens.TextPrimary
                    )
                    Text(
                        text = "共 ${rules.size} 条文本发音修正与正则规则",
                        fontSize = 11.sp,
                        color = PulseTokens.CyanElectric,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 即时正则测试工作台
            item {
                PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🧪 单手即时替换流水线测试", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PulseTokens.CyanElectric)

                        OutlinedTextField(
                            value = testInputText,
                            onValueChange = {
                                testInputText = it
                                testResultText = TextPreprocessor.process(it, rules)
                            },
                            label = { Text("原始文本") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric),
                            maxLines = 3
                        )

                        if (testResultText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PulseTokens.SurfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("流水线输出结果:", fontSize = 11.sp, color = PulseTokens.TextTertiary)
                                    Text(testResultText, fontSize = 13.sp, color = PulseTokens.CyanElectric, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 搜索过滤栏
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索匹配模式或描述...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PulseTokens.TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric),
                    singleLine = true
                )
            }

            val filteredRules = rules.filter {
                it.pattern.contains(searchQuery, ignoreCase = true) ||
                it.replacement.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }

            itemsIndexed(filteredRules, key = { _, r -> r.id }) { _, rule ->
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = if (rule.enabled) PulseTokens.SurfaceCard else PulseTokens.SurfaceDark.copy(alpha = 0.6f),
                    border = if (rule.enabled) PulseTokens.BorderSubtle else null
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (rule.description.isNotBlank()) rule.description else "规则: ${rule.pattern}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (rule.enabled) PulseTokens.TextPrimary else PulseTokens.TextTertiary
                                )
                                Text(
                                    text = "「${rule.pattern}」 ➔ 「${rule.replacement}」",
                                    fontSize = 12.sp,
                                    color = if (rule.enabled) PulseTokens.CyanElectric else PulseTokens.TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = {
                                    configDataStore.updateRule(rule.copy(enabled = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (rule.isRegex) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = PulseTokens.CyanElectric.copy(alpha = 0.15f)) {
                                        Text("正则", fontSize = 9.5.sp, color = PulseTokens.CyanElectric, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                if (rule.isCaseSensitive) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = PulseTokens.AmberWarm.copy(alpha = 0.15f)) {
                                        Text("区分大小写", fontSize = 9.5.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = {
                                        val token = configDataStore.exportRuleToken(rule)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI-TTS-Rule-Token", token))
                                        Toast.makeText(context, "已复制规则口令", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "复制", tint = PulseTokens.TextTertiary, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        currentEditingRule = rule
                                        rulePattern = rule.pattern
                                        ruleReplacement = rule.replacement
                                        ruleDescription = rule.description
                                        isRegex = rule.isRegex
                                        isCaseSensitive = rule.isCaseSensitive
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = PulseTokens.CyanElectric, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        configDataStore.deleteRule(rule.id)
                                        Toast.makeText(context, "已删除规则", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = PulseTokens.MagentaLaser, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 新增规则 FAB
        FloatingActionButton(
            onClick = {
                currentEditingRule = null
                rulePattern = ""
                ruleReplacement = ""
                ruleDescription = ""
                isRegex = true
                isCaseSensitive = false
                showEditDialog = true
            },
            containerColor = PulseTokens.CyanElectric,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增规则", modifier = Modifier.size(24.dp))
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text(if (currentEditingRule == null) "新建替换规则" else "编辑替换规则", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = rulePattern,
                            onValueChange = { rulePattern = it },
                            label = { Text("匹配模式 (Pattern)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ruleReplacement,
                            onValueChange = { ruleReplacement = it },
                            label = { Text("替换为 (Replacement)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ruleDescription,
                            onValueChange = { ruleDescription = it },
                            label = { Text("规则说明/备注") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                            Text("启用正则表达式匹配", fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isCaseSensitive, onCheckedChange = { isCaseSensitive = it })
                            Text("区分英文大小写", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rulePattern.isBlank()) {
                                Toast.makeText(context, "匹配模式不能为空", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val rule = ReplacementRule(
                                id = currentEditingRule?.id ?: "rule_${UUID.randomUUID().toString().take(6)}",
                                pattern = rulePattern.trim(),
                                replacement = ruleReplacement,
                                description = ruleDescription.trim(),
                                isRegex = isRegex,
                                isCaseSensitive = isCaseSensitive,
                                enabled = currentEditingRule?.enabled ?: true
                            )
                            configDataStore.updateRule(rule)
                            showEditDialog = false
                            Toast.makeText(context, "规则已保存", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black)
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
