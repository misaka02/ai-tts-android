package com.aitts.engine.ui.material.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ReplacementRule
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.material.GoogleColors
import java.util.UUID

/**
 * 📝 Google 官方应用风格 - 发音规则流水线 (Google Rules Pipeline)
 * 1. Google Tasks / Keep 风格列表与圆角容器；
 * 2. 实时发音替换比对测试卡片；
 * 3. 搜索过滤与分类药丸 (全部 / 多音字 / 净化 / 专有名词)；
 * 4. M3 Switch 极简启闭开关与新增规则弹窗。
 */
@Composable
fun GoogleRulesScreen(
    configDataStore: ConfigDataStore,
    colors: GoogleColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rules by configDataStore.rulesFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var testInputText by remember { mutableStateOf("重庆的重阳节到了，银行行长去考察工作。") }
    var editingRule by remember { mutableStateOf<ReplacementRule?>(null) }
    var showRuleDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "ALL" to "全部规则",
        "POLYPHONE" to "多音字",
        "CLEANUP" to "符号净化",
        "SPECIAL" to "专有名词",
        "COMMON" to "常用规则"
    )

    val filteredRules = rules.filter { rule ->
        val matchesCategory = if (selectedCategory == "ALL") true else rule.category == selectedCategory
        val matchesSearch = if (searchQuery.isBlank()) true else {
            rule.pattern.contains(searchQuery, ignoreCase = true) ||
            rule.replacement.contains(searchQuery, ignoreCase = true) ||
            rule.description.contains(searchQuery, ignoreCase = true)
        }
        matchesCategory && matchesSearch
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶栏标题
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "发音修正与流水线",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "实时纠正多音字读音、净化特殊符号与专有名词规范化",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // Google 风格搜索药丸输入框
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索匹配规则或替换词...", fontSize = 14.sp, color = colors.textTertiary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                cursorColor = colors.primary,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            singleLine = true
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "清除", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 规则分类药丸滑轨
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (key, label) ->
                        val isSelected = selectedCategory == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = key },
                            label = { Text(label, fontSize = 12.5.sp) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.surfaceContainer,
                                labelColor = colors.textSecondary,
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer
                            ),
                            border = null
                        )
                    }
                }
            }

            // 实时发音替换比对测试卡片
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚡ 规则实时效果演练",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )

                        OutlinedTextField(
                            value = testInputText,
                            onValueChange = { testInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("输入测试原句", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outlineSubtle,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val processed: String = remember(testInputText, rules) {
                            TextPreprocessor.process(testInputText, rules)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceContainer
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("替换后发音文本:", fontSize = 11.sp, color = colors.textTertiary)
                                Text(
                                    text = processed as String,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (processed != testInputText) colors.googleGreen else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 规则卡片列表
            if (filteredRules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("未找到符合条件的发音规则", color = colors.textTertiary, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredRules, key = { it.id }) { rule ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (rule.category) {
                                            "POLYPHONE" -> colors.googleBlue.copy(alpha = 0.15f)
                                            "CLEANUP" -> colors.googleYellow.copy(alpha = 0.2f)
                                            "SPECIAL" -> colors.googleGreen.copy(alpha = 0.15f)
                                            else -> colors.surfaceContainerHigh
                                        }
                                    ) {
                                        Text(
                                            text = when (rule.category) {
                                                "POLYPHONE" -> "多音字"
                                                "CLEANUP" -> "净化"
                                                "SPECIAL" -> "专有名词"
                                                else -> "通用"
                                            },
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (rule.isRegex) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.surfaceContainerHigh
                                        ) {
                                            Text(
                                                text = "正则",
                                                fontSize = 10.sp,
                                                color = colors.textSecondary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (rule.description.isNotBlank()) {
                                        Text(
                                            text = rule.description,
                                            fontSize = 11.5.sp,
                                            color = colors.textTertiary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = rule.pattern,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = rule.replacement.ifBlank { "(清除)" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rule.replacement.isBlank()) colors.textTertiary else colors.primary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { isChecked ->
                                        configDataStore.updateRule(rule.copy(enabled = isChecked))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.onPrimary,
                                        checkedTrackColor = colors.primary,
                                        uncheckedThumbColor = colors.textTertiary,
                                        uncheckedTrackColor = colors.surfaceContainerHigh
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        editingRule = rule
                                        showRuleDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        configDataStore.deleteRule(rule.id)
                                        Toast.makeText(context, "已删除规则", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.googleRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Google Extended FAB
        ExtendedFloatingActionButton(
            onClick = {
                editingRule = null
                showRuleDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp),
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加规则", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }

    // 新增/编辑规则弹窗
    if (showRuleDialog) {
        var patternText by remember { mutableStateOf(editingRule?.pattern ?: "") }
        var replacementText by remember { mutableStateOf(editingRule?.replacement ?: "") }
        var isRegexChecked by remember { mutableStateOf(editingRule?.isRegex ?: false) }
        var ruleCategory by remember { mutableStateOf(editingRule?.category ?: "POLYPHONE") }
        var descText by remember { mutableStateOf(editingRule?.description ?: "") }

        AlertDialog(
            onDismissRequest = { showRuleDialog = false },
            title = { Text(if (editingRule != null) "编辑发音规则" else "新增发音规则", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = patternText,
                        onValueChange = { patternText = it },
                        label = { Text("匹配词语或正则表达式") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                    )

                    OutlinedTextField(
                        value = replacementText,
                        onValueChange = { replacementText = it },
                        label = { Text("替换发音文本 (可留空以清除)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                    )

                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("规则说明 (选填)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用正则表达式", fontSize = 13.sp, color = colors.textPrimary)
                        Switch(
                            checked = isRegexChecked,
                            onCheckedChange = { isRegexChecked = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (patternText.isBlank()) {
                            Toast.makeText(context, "匹配文本不能为空", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val rule = ReplacementRule(
                            id = editingRule?.id ?: UUID.randomUUID().toString(),
                            pattern = patternText.trim(),
                            replacement = replacementText.trim(),
                            isRegex = isRegexChecked,
                            category = ruleCategory,
                            description = descText.trim(),
                            enabled = editingRule?.enabled ?: true
                        )
                        configDataStore.updateRule(rule)
                        showRuleDialog = false
                        Toast.makeText(context, "规则已保存", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRuleDialog = false }) {
                    Text("取消", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
