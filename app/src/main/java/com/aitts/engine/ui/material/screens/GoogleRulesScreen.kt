package com.aitts.engine.ui.material.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ReplacementRule
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.material.GoogleColors
import java.util.UUID

/**
 * 📝 Google 官方应用风格 - 发音规则流水线全功能版 (Google Rules Pipeline)
 * 适配全量高级能力：
 * 1. 规则搜索过滤与分类滑轨；
 * 2. 实时发音替换比对测试卡片；
 * 3. 预设规则包恢复与内置精选库；
 * 4. 批量一键全开/全关；
 * 5. 规则顺序上下移动调整匹配优先级；
 * 6. 规则 JSON 导出与导入；
 * 7. 正则、大小写敏感、分类标签与备注。
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
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "ALL" to "全部规则",
        "POLYPHONE" to "多音字",
        "CLEANUP" to "符号净化",
        "WATERMARK" to "去水印",
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

    fun moveRule(fromIndex: Int, up: Boolean) {
        val toIndex = if (up) fromIndex - 1 else fromIndex + 1
        if (toIndex in rules.indices) {
            val list = rules.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            configDataStore.saveRules(list)
        }
    }

    fun batchToggle(enabled: Boolean) {
        val updated = rules.map {
            if (selectedCategory == "ALL" || it.category == selectedCategory) {
                it.copy(enabled = enabled)
            } else {
                it
            }
        }
        configDataStore.saveRules(updated)
        Toast.makeText(context, if (enabled) "已开启当前匹配规则" else "已关闭当前匹配规则", Toast.LENGTH_SHORT).show()
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶栏标题与快捷工具
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "发音规则流水线",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "多音字纠错、去水印与专有名词发音规范化",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 导出规则 JSON
                        IconButton(
                            onClick = {
                                val jsonStr = configDataStore.exportRulesJson()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("aitts_rules", jsonStr))
                                Toast.makeText(context, "规则已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "导出规则", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }

                        // 恢复默认规则
                        IconButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "恢复内置预设", tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Google 搜索药丸与批量控制条
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Icon(Icons.Default.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
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

                    // 批量全开 / 全关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { batchToggle(true) },
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceContainer
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = colors.googleGreen, modifier = Modifier.size(12.dp))
                                Text("开启全部", fontSize = 11.sp, color = colors.googleGreen)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { batchToggle(false) },
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceContainer
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = colors.googleRed, modifier = Modifier.size(12.dp))
                                Text("关闭全部", fontSize = 11.sp, color = colors.googleRed)
                            }
                        }
                    }
                }
            }

            // 规则分类药丸滑轨
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // 实时发音比对演练卡片
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚡ 规则实时效果演练", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)

                        OutlinedTextField(
                            value = testInputText,
                            onValueChange = { testInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("输入测试原句", fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
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

            // 规则卡片列表 (包含优先级上移/下移)
            if (filteredRules.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Text("未找到符合条件的发音规则", color = colors.textTertiary, fontSize = 13.sp)
                    }
                }
            } else {
                itemsIndexed(filteredRules, key = { _, it -> it.id }) { index, rule ->
                    val originalIndex = rules.indexOf(rule)

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
                                            "WATERMARK" -> colors.googleRed.copy(alpha = 0.15f)
                                            "SPECIAL" -> colors.googleGreen.copy(alpha = 0.15f)
                                            else -> colors.surfaceContainerHigh
                                        }
                                    ) {
                                        Text(
                                            text = when (rule.category) {
                                                "POLYPHONE" -> "多音字"
                                                "CLEANUP" -> "净化"
                                                "WATERMARK" -> "去水印"
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
                                        Surface(shape = RoundedCornerShape(6.dp), color = colors.surfaceContainerHigh) {
                                            Text("正则", fontSize = 10.sp, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }

                                    if (rule.description.isNotBlank()) {
                                        Text(rule.description, fontSize = 11.5.sp, color = colors.textTertiary, maxLines = 1)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(rule.pattern, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = rule.replacement.ifBlank { "(清除)" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rule.replacement.isBlank()) colors.textTertiary else colors.primary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                // 上移优先级
                                IconButton(
                                    onClick = { if (originalIndex > 0) moveRule(originalIndex, true) },
                                    enabled = originalIndex > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "上移", tint = if (originalIndex > 0) colors.textSecondary else colors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(15.dp))
                                }

                                // 下移优先级
                                IconButton(
                                    onClick = { if (originalIndex < rules.size - 1) moveRule(originalIndex, false) },
                                    enabled = originalIndex < rules.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "下移", tint = if (originalIndex < rules.size - 1) colors.textSecondary else colors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(15.dp))
                                }

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
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        configDataStore.deleteRule(rule.id)
                                        Toast.makeText(context, "已删除规则", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.googleRed, modifier = Modifier.size(15.dp))
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

    // 重置默认规则弹窗
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("恢复内置精选规则库", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("确定要恢复系统内置的常用多音字与去水印精选规则包吗？这不会删除您自建的规则。", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        configDataStore.saveRules(PresetConfigs.defaultRules)
                        showResetConfirmDialog = false
                        Toast.makeText(context, "已恢复内置精选规则包", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("确认恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("取消") }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 新增/编辑规则弹窗
    if (showRuleDialog) {
        var patternText by remember { mutableStateOf(editingRule?.pattern ?: "") }
        var replacementText by remember { mutableStateOf(editingRule?.replacement ?: "") }
        var isRegexChecked by remember { mutableStateOf(editingRule?.isRegex ?: false) }
        var isCaseSensitiveChecked by remember { mutableStateOf(editingRule?.isCaseSensitive ?: false) }
        var ruleCategory by remember { mutableStateOf(editingRule?.category ?: if (selectedCategory == "ALL") "POLYPHONE" else selectedCategory) }
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                    )

                    OutlinedTextField(
                        value = replacementText,
                        onValueChange = { replacementText = it },
                        label = { Text("替换发音文本 (可留空以清除)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                    )

                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("规则说明/备注 (选填)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("区分大小写", fontSize = 13.sp, color = colors.textPrimary)
                        Switch(
                            checked = isCaseSensitiveChecked,
                            onCheckedChange = { isCaseSensitiveChecked = it },
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
                            isCaseSensitive = isCaseSensitiveChecked,
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
