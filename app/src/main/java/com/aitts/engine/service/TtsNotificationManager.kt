package com.aitts.engine.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aitts.engine.ui.MainActivity

/**
 * TTS 实时播放状态通知栏管理器：
 * 1. 在后台听书与第三方应用调用 TTS 时，实时在通知栏展示当前朗读的音色与当前句子内容；
 * 2. 提供一键停止朗读 (Stop) 与快捷返回控制台 (Open App) 的交互操作。
 */
object TtsNotificationManager {

    const val CHANNEL_ID = "tts_playback_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP_TTS = "com.aitts.engine.ACTION_STOP_TTS"

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI TTS 朗读与播放状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "展示当前 AI 语音引擎的实时朗读内容与停止控制"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showPlaybackNotification(
        context: Context,
        providerName: String,
        voiceId: String,
        currentSentence: String
    ) {
        try {
            createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(ACTION_STOP_TTS).apply {
                setPackage(context.packageName)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("$providerName ($voiceId) · AI 朗读中")
                .setContentText(currentSentence.ifBlank { "正在准备音频流..." })
                .setStyle(NotificationCompat.BigTextStyle().bigText(currentSentence))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "⏹️ 停止朗读",
                    stopPendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_manage,
                    "⚙️ 控制台",
                    openPendingIntent
                )
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // 忽略权限缺失或通知发送异常
        }
    }

    fun cancelPlaybackNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // ignore
        }
    }
}