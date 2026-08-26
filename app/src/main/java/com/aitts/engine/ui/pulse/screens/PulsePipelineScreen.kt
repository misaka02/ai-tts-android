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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ReplacementRule
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.pulse.components.ActionHubItem
import com.aitts.engine.ui.pulse.components.UniversalActionHub
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import java.util.UUID

import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * ⚡ Pulse 正则发音替换流水线 (Pulse Rules Pipeline)
 * 1. 动态正则发音替换规则管理（分类筛选、一键批量开关、停启用、匹配模式、替换词）；
 * 2. 单手即时正则流水线测试台；
 * 3. 单规则口令复制与分享。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PulsePipelineScreen(
    configDataStore: ConfigDataStore,
    parentPagerState: androidx.compose.foundation.pager.PagerState? = null
) {
    val context = LocalContext.current
    val rules by configDataStore.rulesFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var testInputText by remember { mutableStateOf("例如输入：第123章，主角生命值-50%，获得了1000g黄金。") }
    var testResultText by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var currentEditingRule by remember { mutableStateOf<ReplacementRule?>(null) }
    var rulePattern by remember { mutableStateOf("") }
    var ruleReplacement by remember { mutableStateOf("") }
    var ruleDescription by remember { mutableStateOf("") }
    var ruleCategory by remember { mutableStateOf("COMMON") }
    var isRegex by remember { mutableStateOf(true) }
    var isCaseSensitive by remember { mutableStateOf(false) }
    var showCategorySelectorDialog by remember { mutableStateOf(false) }

    val categoryTabs = listOf(
        "ALL" to "全部 (${rules.size})",
        "POLYPHONE" to "🔤 多音字",
        "CLEANUP" to "🧹 符号净化",
        "WATERMARK" to "🛡️ 防盗水印",
        "SPECIAL" to "🧩 专有缩写",
        "COMMON" to "📝 通用规则"
    )

    fun runTest() {
        testResultText = TextPreprocessor.process(testInputText, rules)
    }

    fun batchToggleRules(enable: Boolean) {
        val updated = rules.map { rule ->
            if (selectedCategory == "ALL" || rule.category == selectedCategory) rule.copy(enabled = enable)
            else rule
        }
        configDataStore.saveRules(updated)
        val catName = categoryTabs.find { it.first == selectedCategory }?.second ?: "当前分类"
        Toast.makeText(context, if (enable) "已开启 ${catName} 的全部规则" else "已关闭 ${catName} 的全部规则", Toast.LENGTH_SHORT).show()
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
                        text = "共 ${rules.size} 条文本发音修正与正则规则 · 按分类一键启停",
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

            // 规则分类胶囊栏
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryTabs) { (catKey, catTitle) ->
                        val isSelected = selectedCategory == catKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = catKey },
                            label = { Text(catTitle, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PulseTokens.CyanElectric,
                                selectedLabelColor = Color.Black,
                                containerColor = PulseTokens.SurfaceElevated,
                                labelColor = PulseTokens.TextSecondary
                            ),
                            border = if (isSelected) null else PulseTokens.BorderSubtle
                        )
                    }
                }
            }

            // 分类快捷批量控制与搜索过滤栏
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentCatTitle = categoryTabs.find { it.first == selectedCategory }?.second ?: "全部"
                        Text(
                            text = "分类「$currentCatTitle」批量控制",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PulseTokens.TextSecondary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val updated = rules.map { r ->
                                        if (selectedCategory == "ALL" || r.category.equals(selectedCategory, ignoreCase = true)) {
                                            r.copy(enabled = true)
                                        } else r
                                    }
                                    configDataStore.saveRules(updated)
                                    Toast.makeText(context, "已全部开启「$currentCatTitle」规则", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("一键全开", fontSize = 11.5.sp, color = PulseTokens.CyanElectric, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val updated = rules.map { r ->
                                        if (selectedCategory == "ALL" || r.category.equals(selectedCategory, ignoreCase = true)) {
                                            r.copy(enabled = false)
                                        } else r
                                    }
                                    configDataStore.saveRules(updated)
                                    Toast.makeText(context, "已全部关闭「$currentCatTitle」规则", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, PulseTokens.MagentaLaser.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = PulseTokens.MagentaLaser, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("一键全关", fontSize = 11.5.sp, color = PulseTokens.MagentaLaser, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

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
            }

            val filteredRules = rules.filter {
                val matchesCategory = selectedCategory == "ALL" || it.category.equals(selectedCategory, ignoreCase = true)
                val matchesQuery = searchQuery.isBlank() ||
                    it.pattern.contains(searchQuery, ignoreCase = true) ||
                    it.replacement.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesQuery
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val catBadge = when (rule.category.uppercase()) {
                                        "POLYPHONE" -> "🔤 多音字"
                                        "CLEANUP" -> "🧹 净化"
                                        "WATERMARK" -> "🛡️ 防盗"
                                        "SPECIAL" -> "🧩 专有"
                                        else -> "📝 通用"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PulseTokens.SurfaceElevated,
                                        border = BorderStroke(0.8.dp, PulseTokens.CyanElectric.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = catBadge,
                                            fontSize = 9.5.sp,
                                            color = PulseTokens.CyanElectric,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = if (rule.description.isNotBlank()) rule.description else "规则: ${rule.pattern}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (rule.enabled) PulseTokens.TextPrimary else PulseTokens.TextTertiary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
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
                                        ruleCategory = rule.category
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

        // 右下角大拇指悬浮收纳岛 (规则流水线专属动作组 - 仅在当前页面活跃时渲染)
        if (parentPagerState == null || parentPagerState.currentPage == 2) {
            UniversalActionHub(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp),
                items = listOf(
                    ActionHubItem(
                        label = if (selectedCategory == "ALL") "一键开启全部规则" else "一键开启当前分类",
                        icon = Icons.Default.Check,
                        color = PulseTokens.CyanElectric,
                        onClick = { batchToggleRules(true) }
                    ),
                    ActionHubItem(
                        label = if (selectedCategory == "ALL") "一键关闭全部规则" else "一键关闭当前分类",
                        icon = Icons.Default.Block,
                        color = PulseTokens.MagentaLaser,
                        onClick = { batchToggleRules(false) }
                    ),
                    ActionHubItem(
                        label = "切换分类 (${categoryTabs.find { it.first == selectedCategory }?.second ?: "全部"})",
                        icon = Icons.Default.Category,
                        color = PulseTokens.SonicBlue,
                        onClick = { showCategorySelectorDialog = true }
                    ),
                    ActionHubItem(
                        label = "新建替换规则",
                        icon = Icons.Default.Add,
                        color = PulseTokens.AmberWarm,
                        onClick = {
                            currentEditingRule = null
                            rulePattern = ""
                            ruleReplacement = ""
                            ruleDescription = ""
                            ruleCategory = if (selectedCategory == "ALL") "COMMON" else selectedCategory
                            isRegex = true
                            isCaseSensitive = false
                            showEditDialog = true
                        }
                    ),
                    ActionHubItem(
                        label = "测试当前流水线",
                        icon = Icons.Default.Science,
                        color = PulseTokens.CyanElectric,
                        onClick = { runTest() }
                    )
                ),
                icon = Icons.Default.Tune
            )
        }

        // 弹窗: 大拇指快速切换分类轮盘
        if (showCategorySelectorDialog) {
            AlertDialog(
                onDismissRequest = { showCategorySelectorDialog = false },
                title = { Text("大拇指快速切换分类", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryTabs.forEach { (catKey, catLabel) ->
                            val isSelected = selectedCategory == catKey
                            PulseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = catKey
                                        showCategorySelectorDialog = false
                                    },
                                backgroundColor = if (isSelected) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                border = if (isSelected) BorderStroke(1.5.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(catLabel, fontSize = 13.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategorySelectorDialog = false }) { Text("关闭") }
                }
            )
        }

        if (showEditDialog) {
            val availableCategories = listOf(
                "POLYPHONE" to "🔤 多音字修复",
                "CLEANUP" to "🧹 符号标点净化",
                "WATERMARK" to "🛡️ 防盗去水印",
                "SPECIAL" to "🧩 专有缩写名词",
                "COMMON" to "📝 通用替换规则"
            )

            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text(if (currentEditingRule == null) "新建替换规则" else "编辑替换规则", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("所属分类", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableCategories) { (catKey, catLabel) ->
                                val isSelected = ruleCategory == catKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { ruleCategory = catKey },
                                    label = { Text(catLabel, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PulseTokens.CyanElectric,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

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
                                category = ruleCategory,
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
