package com.aitts.engine.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 全局配置持久化管理中心
 */
class ConfigDataStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_tts_config_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<GlobalSettings> = _settingsFlow.asStateFlow()

    private val _providersFlow = MutableStateFlow(loadProviders())
    val providersFlow: StateFlow<List<TtsProviderConfig>> = _providersFlow.asStateFlow()

    private val _rulesFlow = MutableStateFlow(loadRules())
    val rulesFlow: StateFlow<List<ReplacementRule>> = _rulesFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow(loadHistory())
    val historyFlow: StateFlow<List<SpeechHistoryItem>> = _historyFlow.asStateFlow()

    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    private val _structuredLogsFlow = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val structuredLogsFlow: StateFlow<List<AppLogEntry>> = _structuredLogsFlow.asStateFlow()

    @Volatile
    var activeSessionId: String? = null

    fun log(message: String, level: LogLevel = LogLevel.INFO, tag: String = "TTS", sessionId: String? = null) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val resolvedSessionId = sessionId ?: activeSessionId
        val entry = AppLogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            title = message,
            sessionId = resolvedSessionId
        )
        val formatted = entry.formatToString()
        android.util.Log.d("AiTtsEngine", formatted)
        val currentStr = _logsFlow.value.toMutableList()
        if (currentStr.size > 200) currentStr.removeAt(0)
        currentStr.add(formatted)
        _logsFlow.value = currentStr

        val currentStruct = _structuredLogsFlow.value.toMutableList()
        if (currentStruct.size > 200) currentStruct.removeAt(0)
        currentStruct.add(entry)
        _structuredLogsFlow.value = currentStruct
    }

    fun logStructured(
        level: LogLevel,
        tag: String,
        title: String,
        details: String? = null,
        sessionId: String? = null
    ) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val resolvedSessionId = sessionId ?: activeSessionId
        val entry = AppLogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            title = title,
            details = details,
            sessionId = resolvedSessionId
        )
        val formatted = entry.formatToString()
        android.util.Log.d("AiTtsEngine", formatted)
        val currentStr = _logsFlow.value.toMutableList()
        if (currentStr.size > 200) currentStr.removeAt(0)
        currentStr.add(formatted)
        _logsFlow.value = currentStr

        val currentStruct = _structuredLogsFlow.value.toMutableList()
        if (currentStruct.size > 200) currentStruct.removeAt(0)
        currentStruct.add(entry)
        _structuredLogsFlow.value = currentStruct
    }

    fun clearLogs() {
        _logsFlow.value = emptyList()
        _structuredLogsFlow.value = emptyList()
    }

    // --- History ---
    private fun loadHistory(): List<SpeechHistoryItem> {
        val str = prefs.getString(KEY_HISTORY, null)
        return if (!str.isNullOrBlank()) {
            try {
                json.decodeFromString<List<SpeechHistoryItem>>(str)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun recordSpeechHistory(item: SpeechHistoryItem) {
        val current = _historyFlow.value.toMutableList()
        current.add(0, item) // 最新在前
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        _historyFlow.value = current
        scope.launch {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(current)).apply()
        }
    }

    fun clearHistory() {
        _historyFlow.value = emptyList()
        scope.launch {
            prefs.edit().remove(KEY_HISTORY).apply()
        }
    }

    // --- Settings ---
    private fun loadSettings(): GlobalSettings {
        val str = prefs.getString(KEY_SETTINGS, null)
        return if (!str.isNullOrBlank()) {
            try {
                json.decodeFromString<GlobalSettings>(str)
            } catch (e: Exception) {
                GlobalSettings()
            }
        } else {
            GlobalSettings()
        }
    }

    fun updateSettings(settings: GlobalSettings) {
        _settingsFlow.value = settings
        com.aitts.engine.network.SharedHttpClient.updateConfiguration(settings)
        scope.launch {
            prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
        }
    }

    fun getActiveProvider(): TtsProviderConfig {
        val activeId = _settingsFlow.value.activeProviderId
        return _providersFlow.value.find { it.id == activeId }
            ?: _providersFlow.value.firstOrNull { it.enabled }
            ?: _providersFlow.value.firstOrNull()
            ?: PresetConfigs.createDefaultProviders().first()
    }

    fun setActiveProviderId(id: String) {
        val current = _settingsFlow.value
        updateSettings(current.copy(activeProviderId = id))
        // 自动激活该模型
        val providers = _providersFlow.value.toMutableList()
        val index = providers.indexOfFirst { it.id == id }
        if (index >= 0 && !providers[index].enabled) {
            providers[index] = providers[index].copy(enabled = true)
            saveProviders(providers)
        }
    }

    // --- Providers ---
    private fun loadProviders(): List<TtsProviderConfig> {
        val str = prefs.getString(KEY_PROVIDERS, null)
        return if (!str.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<TtsProviderConfig>>(str)
                if (list.isNotEmpty()) list else PresetConfigs.createDefaultProviders()
            } catch (e: Exception) {
                PresetConfigs.createDefaultProviders()
            }
        } else {
            PresetConfigs.createDefaultProviders()
        }
    }

    fun saveProviders(providers: List<TtsProviderConfig>) {
        _providersFlow.value = providers
        scope.launch {
            prefs.edit().putString(KEY_PROVIDERS, json.encodeToString(providers)).apply()
        }
    }

    fun updateProvider(provider: TtsProviderConfig) {
        val current = _providersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == provider.id }
        if (index >= 0) {
            current[index] = provider
        } else {
            // 新增引擎直接添加到列表顶部，方便用户立即配置
            current.add(0, provider)
        }
        saveProviders(current)
    }

    fun reorderProviders(fromIndex: Int, toIndex: Int) {
        val current = _providersFlow.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            saveProviders(current)
        }
    }

    fun moveProviderUp(id: String) {
        val current = _providersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            saveProviders(current)
        }
    }

    fun moveProviderDown(id: String) {
        val current = _providersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0 && index < current.size - 1) {
            val item = current.removeAt(index)
            current.add(index + 1, item)
            saveProviders(current)
        }
    }

    fun pinProviderToTop(id: String) {
        val current = _providersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = current.removeAt(index)
            current.add(0, item)
            saveProviders(current)
        }
    }

    fun duplicateProvider(id: String) {
        val current = _providersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val source = current[index]
            val newId = "${source.type.name.lowercase()}_${java.util.UUID.randomUUID().toString().take(6)}"
            val cloned = source.copy(
                id = newId,
                name = "${source.name} (副本)"
            )
            current.add(index + 1, cloned)
            saveProviders(current)
        }
    }

    fun deleteProvider(id: String) {
        val current = _providersFlow.value.filter { it.id != id }
        saveProviders(current)
    }

    // --- Rules ---
    private fun loadRules(): List<ReplacementRule> {
        val str = prefs.getString(KEY_RULES, null)
        return if (!str.isNullOrBlank()) {
            try {
                json.decodeFromString<List<ReplacementRule>>(str)
            } catch (e: Exception) {
                PresetConfigs.defaultRules
            }
        } else {
            PresetConfigs.defaultRules
        }
    }

    fun saveRules(rules: List<ReplacementRule>) {
        _rulesFlow.value = rules
        scope.launch {
            prefs.edit().putString(KEY_RULES, json.encodeToString(rules)).apply()
        }
    }

    fun updateRule(rule: ReplacementRule) {
        val current = _rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            current[index] = rule
        } else {
            current.add(rule)
        }
        saveRules(current)
    }

    fun deleteRule(id: String) {
        val current = _rulesFlow.value.filter { it.id != id }
        saveRules(current)
    }

    fun sortProvidersByLatency(latencyMap: Map<String, Long>) {
        val current = _providersFlow.value.toMutableList()
        current.sortBy { provider ->
            latencyMap[provider.id] ?: 999999L
        }
        saveProviders(current)
    }

    // --- Backup & Restore ---
    fun exportRulesJson(): String {
        return json.encodeToString(_rulesFlow.value)
    }

    fun exportAllConfigJson(desensitize: Boolean = false): String {
        val providersToExport = if (desensitize) {
            _providersFlow.value.map { it.copy(apiKey = "") }
        } else {
            _providersFlow.value
        }
        val backupData = BackupPayload(
            settings = _settingsFlow.value,
            providers = providersToExport,
            rules = _rulesFlow.value
        )
        return json.encodeToString(backupData)
    }

    /**
     * 生成单个 Provider 的 Base64 分享口令
     */
    fun exportProviderToken(provider: TtsProviderConfig): String {
        val jsonStr = json.encodeToString(provider)
        val b64 = android.util.Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return "aitts://provider?data=$b64"
    }

    /**
     * 从 Base64 分享口令或 JSON 解析并导入单个 Provider
     */
    fun importProviderFromToken(token: String): TtsProviderConfig? {
        return try {
            val raw = token.trim()
            val jsonStr = if (raw.startsWith("aitts://provider?data=")) {
                val b64 = raw.substringAfter("aitts://provider?data=")
                String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            } else if (raw.startsWith("eyJ")) { // Direct Base64
                String(android.util.Base64.decode(raw, android.util.Base64.DEFAULT), Charsets.UTF_8)
            } else {
                raw
            }
            val parsed = json.decodeFromString<TtsProviderConfig>(jsonStr)
            val updated = parsed.copy(id = "imported_${java.util.UUID.randomUUID().toString().take(6)}")
            updateProvider(updated)
            updated
        } catch (e: Exception) {
            log("导入分享口令失败: ${e.message}")
            null
        }
    }

    /**
     * 生成单条替换规则的 Base64 分享口令
     */
    fun exportRuleToken(rule: ReplacementRule): String {
        val jsonStr = json.encodeToString(rule)
        val b64 = android.util.Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return "aitts://rule?data=$b64"
    }

    fun importConfigJson(jsonStr: String): Boolean {
        return try {
            val backupData = json.decodeFromString<BackupPayload>(jsonStr)
            updateSettings(backupData.settings)
            saveProviders(backupData.providers)
            saveRules(backupData.rules)
            true
        } catch (e: Exception) {
            log("导入配置失败: ${e.message}")
            false
        }
    }

    fun resetToDefaults() {
        updateSettings(GlobalSettings())
        saveProviders(PresetConfigs.createDefaultProviders())
        saveRules(PresetConfigs.defaultRules)
        log("已重置所有配置为官方默认值")
    }

    companion object {
        private const val KEY_SETTINGS = "global_settings"
        private const val KEY_PROVIDERS = "tts_providers"
        private const val KEY_RULES = "replacement_rules"
        private const val KEY_HISTORY = "speech_history"

        @Volatile
        private var instance: ConfigDataStore? = null

        fun getInstance(context: Context): ConfigDataStore {
            return instance ?: synchronized(this) {
                instance ?: ConfigDataStore(context.applicationContext).also { instance = it }
            }
        }
    }
}

enum class LogLevel(val label: String) {
    INFO("INFO"),
    SUCCESS("SUCCESS"),
    WARN("WARN"),
    ERROR("ERROR"),
    METRIC("METRIC")
}

data class AppLogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val title: String,
    val details: String? = null,
    val sessionId: String? = null
) {
    fun formatToString(): String {
        val levelTag = when (level) {
            LogLevel.INFO -> "[INFO]"
            LogLevel.SUCCESS -> "[OK]"
            LogLevel.WARN -> "[WARN]"
            LogLevel.ERROR -> "[ERR]"
            LogLevel.METRIC -> "[PERF]"
        }
        val sessionTag = if (!sessionId.isNullOrBlank()) " [$sessionId]" else ""
        return if (details.isNullOrBlank()) {
            "[$timestamp] $levelTag [$tag]$sessionTag $title"
        } else {
            "[$timestamp] $levelTag [$tag]$sessionTag $title | $details"
        }
    }
}
