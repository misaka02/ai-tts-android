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

    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        android.util.Log.d("AiTtsEngine", message)
        val current = _logsFlow.value.toMutableList()
        if (current.size > 200) current.removeAt(0)
        current.add(logEntry)
        _logsFlow.value = current
    }

    fun clearLogs() {
        _logsFlow.value = emptyList()
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
        scope.launch {
            prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
        }
    }

    fun getActiveProvider(): TtsProviderConfig {
        val activeId = _settingsFlow.value.activeProviderId
        return _providersFlow.value.find { it.id == activeId }
            ?: _providersFlow.value.firstOrNull { it.enabled }
            ?: PresetConfigs.createDefaultProviders().first()
    }

    fun setActiveProviderId(id: String) {
        val current = _settingsFlow.value
        updateSettings(current.copy(activeProviderId = id))
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
            current.add(provider)
        }
        saveProviders(current)
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

    // --- Backup & Restore ---
    fun exportAllConfigJson(): String {
        val backupData = BackupPayload(
            settings = _settingsFlow.value,
            providers = _providersFlow.value,
            rules = _rulesFlow.value
        )
        return json.encodeToString(backupData)
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

    companion object {
        private const val KEY_SETTINGS = "global_settings"
        private const val KEY_PROVIDERS = "tts_providers"
        private const val KEY_RULES = "replacement_rules"

        @Volatile
        private var instance: ConfigDataStore? = null

        fun getInstance(context: Context): ConfigDataStore {
            return instance ?: synchronized(this) {
                instance ?: ConfigDataStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
