package com.aitts.engine.service

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 听书睡眠倒计时管理器 (Sleep Timer with Smooth Fade-out)：
 * 支持在睡前听书时设置定时关闭（15/30/45/60分钟等），
 * 结束前自动平滑淡出音量，并在归零时自动终止后台朗读并释放网络与音频资源。
 */
class SleepTimerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    // 剩余秒数
    private val _remainingSecondsFlow = MutableStateFlow(0)
    val remainingSecondsFlow: StateFlow<Int> = _remainingSecondsFlow.asStateFlow()

    // 定时器是否激活
    private val _isActiveFlow = MutableStateFlow(false)
    val isActiveFlow: StateFlow<Boolean> = _isActiveFlow.asStateFlow()

    /**
     * 启动睡眠定时器
     * @param minutes 定时时长（分钟）
     */
    fun startTimer(minutes: Int) {
        stopTimer()
        if (minutes <= 0) return

        val totalSeconds = minutes * 60
        _remainingSecondsFlow.value = totalSeconds
        _isActiveFlow.value = true

        timerJob = scope.launch {
            while (isActive && _remainingSecondsFlow.value > 0) {
                delay(1000L)
                val next = _remainingSecondsFlow.value - 1
                _remainingSecondsFlow.value = next
            }

            // 倒计时结束，触发停止广播
            _isActiveFlow.value = false
            _remainingSecondsFlow.value = 0
            val stopIntent = Intent(TtsNotificationManager.ACTION_STOP_TTS).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(stopIntent)
        }
    }

    /**
     * 取消/停止睡眠定时器
     */
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSecondsFlow.value = 0
        _isActiveFlow.value = false
    }

    /**
     * 计算当前音量淡出系数 (0.0f ~ 1.0f)
     * 在最后 15 秒内平滑淡出
     */
    fun getFadeVolumeFactor(): Float {
        if (!_isActiveFlow.value) return 1.0f
        val remaining = _remainingSecondsFlow.value
        return when {
            remaining <= 0 -> 0.0f
            remaining <= 15 -> (remaining / 15.0f).coerceIn(0.0f, 1.0f)
            else -> 1.0f
        }
    }

    companion object {
        @Volatile
        private var instance: SleepTimerManager? = null

        fun getInstance(context: Context): SleepTimerManager {
            return instance ?: synchronized(this) {
                instance ?: SleepTimerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
