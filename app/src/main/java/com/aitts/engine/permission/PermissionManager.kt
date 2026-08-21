package com.aitts.engine.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Android 权限与系统常驻一键检测与申请管理
 */
object PermissionManager {

    data class PermissionState(
        val hasStoragePermission: Boolean,
        val hasAllFilesAccess: Boolean,
        val isIgnoringBatteryOptimizations: Boolean,
        val hasNotificationPermission: Boolean
    ) {
        val isAllGranted: Boolean
            get() = (hasStoragePermission || hasAllFilesAccess) && isIgnoringBatteryOptimizations
    }

    /**
     * 检查当前各项权限状态
     */
    fun checkPermissions(context: Context): PermissionState {
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // Android 13+ 使用媒体读取或全文件管理
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        val hasAllFiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return PermissionState(
            hasStoragePermission = hasStorage,
            hasAllFilesAccess = hasAllFiles,
            isIgnoringBatteryOptimizations = isIgnoringBattery,
            hasNotificationPermission = hasNotification
        )
    }

    /**
     * 请求所有必要的基础运行时权限
     */
    fun requestBasicPermissions(activity: Activity, requestCode: Int = 1001) {
        val list = mutableListOf<String>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (list.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, list.toTypedArray(), requestCode)
        }
    }

    /**
     * 跳转至 Android 11+ 全文件访问权限设置页
     */
    fun requestAllFilesAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * 请求忽略电池优化（防系统杀后台听书）
     */
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }

    /**
     * 一键跳转系统 TTS 默认引擎选择页
     */
    fun openSystemTtsSettings(activity: Activity) {
        val actions = listOf(
            "com.android.settings.TTS_SETTINGS",
            Settings.ACTION_VOICE_INPUT_SETTINGS,
            Settings.ACTION_SETTINGS
        )
        for (action in actions) {
            try {
                val intent = Intent(action)
                activity.startActivity(intent)
                return
            } catch (e: Exception) {
                // try next
            }
        }
    }
}
