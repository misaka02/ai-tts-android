package com.aitts.engine.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.service.FloatingSubtitleManager
import com.aitts.engine.service.TtsNotificationManager

/**
 * 悬浮窗权限交互透明中转 Activity：
 * 1. 彻底规避 Android 10+ 后台 Service / Broadcast 启动 Activity 受限（BAL 拦截）的系统致命问题；
 * 2. 由用户在系统通知栏点击「开启字幕」动作触发（属于合规前台交互）；
 * 3. 引导用户开启悬浮窗权限后，自动激活前台小说悬浮字幕并刷新通知栏控制项，平滑返回原有听书应用。
 */
class OverlayPermissionActivity : Activity() {

    private var hasRequestedPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configDataStore = ConfigDataStore.getInstance(this)
        val currentSettings = configDataStore.settingsFlow.value
        configDataStore.updateSettings(currentSettings.copy(isFloatingSubtitleEnabled = true))

        if (PermissionManager.hasOverlayPermission(this)) {
            FloatingSubtitleManager.getInstance(this).show()
            TtsNotificationManager.refreshNotification(this)
            Toast.makeText(this, "前台小说字幕已开启", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                hasRequestedPermission = true
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(fallbackIntent)
                } catch (fe: Exception) {
                    Toast.makeText(this, "无法打开悬浮窗权限设置页，请前往系统设置手动开启", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasRequestedPermission) {
            val configDataStore = ConfigDataStore.getInstance(this)
            val currentSettings = configDataStore.settingsFlow.value
            if (PermissionManager.hasOverlayPermission(this)) {
                configDataStore.updateSettings(currentSettings.copy(isFloatingSubtitleEnabled = true))
                FloatingSubtitleManager.getInstance(this).show()
                TtsNotificationManager.refreshNotification(this)
                Toast.makeText(this, "悬浮窗权限已开启，前台小说字幕已就绪", Toast.LENGTH_SHORT).show()
            } else {
                configDataStore.updateSettings(currentSettings.copy(isFloatingSubtitleEnabled = false))
                Toast.makeText(this, "未授予悬浮窗权限，前台字幕无法显示", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
