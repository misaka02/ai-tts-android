package com.aitts.engine.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.aitts.engine.ui.MainActivity

/**
 * TTS 实时播放状态通知栏管理器：
 * 1. 在后台听书与第三方应用调用 TTS 时，实时在通知栏展示当前朗读的音色与当前播报的文本；
 * 2. 保证通知条为同一个（固定 NOTIFICATION_ID + setOnlyAlertOnce），杜绝逐句删除/重建闪烁；
 * 3. 引入防抖平滑过渡机制，单句推流结束时不立即注销通知，无缝承接后续句子；
 * 4. 提供通知栏一键开关前台小说悬浮字幕 (Floating Subtitle) 交互。
 */
object TtsNotificationManager {

    const val CHANNEL_ID = "tts_playback_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP_TTS = "com.aitts.engine.ACTION_STOP_TTS"
    const val ACTION_TOGGLE_FLOATING_SUBTITLE = "com.aitts.engine.ACTION_TOGGLE_FLOATING_SUBTITLE"

    private val handler = Handler(Looper.getMainLooper())
    private var pendingDismissRunnable: Runnable? = null

    private var lastProviderName: String = "AI 语音引擎"
    private var lastVoiceId: String = "默认"
    private var lastSentence: String = ""

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

    /**
     * 更新或展示后台朗读状态通知（保持同一个通知条，平滑更新）
     */
    fun showPlaybackNotification(
        context: Context,
        providerName: String,
        voiceId: String,
        currentSentence: String
    ) {
        try {
            cancelDelayedDismiss()
            lastProviderName = providerName
            lastVoiceId = voiceId
            lastSentence = currentSentence

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

            val isFloatingActive = FloatingSubtitleManager.getInstance(context).isShowing()
            val toggleSubtitleIntent = Intent(ACTION_TOGGLE_FLOATING_SUBTITLE).apply {
                setPackage(context.packageName)
            }
            val toggleSubtitlePendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                toggleSubtitleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val subtitleActionTitle = if (isFloatingActive) "🪟 隐藏字幕" else "🪟 开启字幕"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("$providerName ($voiceId) · AI 朗读中")
                .setContentText(currentSentence.ifBlank { "正在准备音频流..." })
                .setStyle(NotificationCompat.BigTextStyle().bigText(currentSentence.ifBlank { "正在准备音频流..." }))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true) // 核心：保持静默连续更新，避免每句刷提示震动或闪烁
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(
                    android.R.drawable.ic_menu_view,
                    subtitleActionTitle,
                    toggleSubtitlePendingIntent
                )
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

    /**
     * 刷新当前通知状态（如悬浮窗开启状态切换后，同步刷新按钮文案）
     */
    fun refreshNotification(context: Context) {
        if (lastSentence.isNotBlank()) {
            showPlaybackNotification(context, lastProviderName, lastVoiceId, lastSentence)
        }
    }

    /**
     * 防抖平滑注销机制：
     * 单句音频推流结束时，不立即强行 cancel 通知，而是保留 4 秒窗口。
     * 若后续句子在短时间内到达，则直接复用同一通知条刷新内容；
     * 若超过 4 秒无新请求（真正暂停/退出），才平滑收起通知与悬浮窗。
     */
    fun scheduleDelayedDismiss(context: Context, delayMs: Long = 4000L) {
        cancelDelayedDismiss()
        val appContext = context.applicationContext
        val runnable = Runnable {
            cancelPlaybackNotification(appContext)
            FloatingSubtitleManager.getInstance(appContext).hide()
        }
        pendingDismissRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    /**
     * 取消待执行的平滑注销计时器
     */
    fun cancelDelayedDismiss() {
        pendingDismissRunnable?.let {
            handler.removeCallbacks(it)
            pendingDismissRunnable = null
        }
    }

    /**
     * 立即彻底注销通知并收起悬浮窗（用于明确停止朗读或服务销毁）
     */
    fun cancelPlaybackNotificationImmediately(context: Context) {
        cancelDelayedDismiss()
        cancelPlaybackNotification(context)
        FloatingSubtitleManager.getInstance(context).hide()
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