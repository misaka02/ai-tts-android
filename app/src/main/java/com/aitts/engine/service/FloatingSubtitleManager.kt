package com.aitts.engine.service

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.GlobalSettings
import com.aitts.engine.permission.PermissionManager

/**
 * 前台小说悬浮文本字幕管理器：
 * 1. 后台听书时在系统顶层悬浮展示当前正在朗读的小说文本；
 * 2. 采用 Gravity.TOP or Gravity.START 绝对坐标系与 FLAG_LAYOUT_IN_SCREEN，杜绝屏外渲染与各厂商 ROM 适配异常；
 * 3. 边界限定拖拽，支持位置锁定（防翻页误触）；
 * 4. 支持根据当前阅读背景智能自适应（系统深浅色感知、环境光流转、全场景万能高反差抗干扰轮廓、双击/点图标一键反转）；
 * 5. 支持个性化定制：背景不透明度 (0%~100%)、字体大小 (11~28sp)、配色方案、对齐方式、折叠行数、图标显隐等；
 * 6. 点击文本区域展开/折叠长文本 (默认行数 <-> 14行)，专属关闭按钮防冲突退出。
 */
class FloatingSubtitleManager private constructor(private val appContext: Context) {

    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootView: View? = null
    private var containerView: LinearLayout? = null
    private var iconView: ImageView? = null
    private var textView: TextView? = null
    private var closeBtn: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isViewAttached = false
    private var isExpanded = false
    private var lastSpokenText: String = ""
    private var isTemporaryInverted = false // 用户临时手势反转标记 (应对小说阅读器单独开夜间模式的场景)

    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {
            mainHandler.post {
                if (isViewAttached) {
                    val settings = ConfigDataStore.getInstance(appContext).settingsFlow.value
                    if (settings.floatingSubtitleBgStyle == "AUTO_ADAPTIVE" || settings.floatingSubtitleFollowSystemTheme) {
                        applySettings(settings)
                    }
                }
            }
        }
        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) {}
    }

    init {
        try {
            appContext.registerComponentCallbacks(componentCallbacks)
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        @Volatile
        private var instance: FloatingSubtitleManager? = null

        fun getInstance(context: Context): FloatingSubtitleManager {
            return instance ?: synchronized(this) {
                instance ?: FloatingSubtitleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 更新当前正在朗读的文本
     */
    fun updateText(text: String) {
        lastSpokenText = text
        mainHandler.post {
            val settings = ConfigDataStore.getInstance(appContext).settingsFlow.value
            if (rootView == null) {
                initView(settings)
            } else {
                applySettings(settings)
            }
            textView?.text = text.ifBlank { "正在准备音频流..." }

            if (!isViewAttached && text.isNotBlank()) {
                attachToWindow()
            }
        }
    }

    /**
     * 显式开启并挂载悬浮窗
     */
    fun show() {
        mainHandler.post {
            val settings = ConfigDataStore.getInstance(appContext).settingsFlow.value
            if (rootView == null) {
                initView(settings)
            } else {
                applySettings(settings)
            }
            if (lastSpokenText.isNotBlank()) {
                textView?.text = lastSpokenText
            }
            if (!isViewAttached) {
                attachToWindow()
            }
        }
    }

    /**
     * 隐藏并从窗口树中移除
     */
    fun hide() {
        mainHandler.post {
            detachFromWindow()
        }
    }

    /**
     * 判断当前悬浮窗是否正显示在屏幕上
     */
    fun isShowing(): Boolean = isViewAttached

    /**
     * 动态应用个性化定制配置（背景透明度、字体大小、文字颜色、对齐方式、折叠行数等）
     */
    fun applySettings(settings: GlobalSettings) {
        mainHandler.post {
            val dpToPx = { dp: Float ->
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, appContext.resources.displayMetrics).toInt()
            }

            // 0. 判断当前环境是否偏深色 (联动系统深浅色与临时手势翻转状态)
            val isSystemNight = (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val isEffectiveDark = when (settings.floatingSubtitleBgStyle) {
                "AUTO_ADAPTIVE" -> if (isTemporaryInverted) !isSystemNight else isSystemNight
                "LIGHT_FROST" -> isTemporaryInverted
                "DARK_FROST", "AMOLED_BLACK" -> !isTemporaryInverted
                "PARCHMENT" -> false
                else -> if (isTemporaryInverted) !isSystemNight else isSystemNight
            }

            // 1. 背景材质与透明度
            val alpha = (settings.floatingSubtitleOpacity.coerceIn(0f, 1f) * 255).toInt()
            if (settings.floatingSubtitleBgStyle == "PURE_TRANSPARENT" || alpha <= 5) {
                containerView?.background = null
                containerView?.elevation = 0f
            } else {
                val baseColor = when (settings.floatingSubtitleBgStyle) {
                    "AUTO_ADAPTIVE" -> if (isEffectiveDark) Color.parseColor("#181A20") else Color.parseColor("#FFFFFF")
                    "LIGHT_FROST" -> if (isEffectiveDark) Color.parseColor("#181A20") else Color.parseColor("#F8FAFC")
                    "PARCHMENT" -> Color.parseColor("#2B231D")
                    "AMOLED_BLACK" -> Color.parseColor("#000000")
                    else -> Color.parseColor("#181A20")
                }
                val bgColor = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                val strokeColor = when (settings.floatingSubtitleBgStyle) {
                    "AUTO_ADAPTIVE" -> if (isEffectiveDark) {
                        Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 255, 255, 255)
                    } else {
                        Color.argb((alpha * 0.25f).toInt().coerceIn(0, 255), 15, 23, 42)
                    }
                    "LIGHT_FROST" -> if (isEffectiveDark) {
                        Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 255, 255, 255)
                    } else {
                        Color.argb((alpha * 0.25f).toInt().coerceIn(0, 255), 15, 23, 42)
                    }
                    "PARCHMENT" -> Color.argb((alpha * 0.4f).toInt().coerceIn(0, 255), 255, 215, 120)
                    else -> Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 255, 255, 255)
                }
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(16f).toFloat()
                    setColor(bgColor)
                    setStroke(dpToPx(1f), strokeColor)
                }
                containerView?.background = bg
                containerView?.elevation = dpToPx(6f).toFloat()
            }

            // 2. 文字颜色自适应推导
            val textColor = if (settings.floatingSubtitleBgStyle == "AUTO_ADAPTIVE" || settings.floatingSubtitleBgStyle == "LIGHT_FROST") {
                if (!isEffectiveDark && settings.floatingSubtitleTextColor.equals("#F5F5F7", ignoreCase = true)) {
                    Color.parseColor("#0F172A") // 白昼浅色背景自动采用高对比度深曜石炭黑
                } else if (isEffectiveDark && settings.floatingSubtitleTextColor.equals("#0F172A", ignoreCase = true)) {
                    Color.parseColor("#F5F5F7") // 暗夜深色背景自动采用高对比度极光象牙白
                } else {
                    try {
                        Color.parseColor(settings.floatingSubtitleTextColor)
                    } catch (e: Exception) {
                        if (isEffectiveDark) Color.parseColor("#F5F5F7") else Color.parseColor("#0F172A")
                    }
                }
            } else {
                try {
                    Color.parseColor(settings.floatingSubtitleTextColor)
                } catch (e: Exception) {
                    Color.parseColor("#F5F5F7")
                }
            }
            textView?.setTextColor(textColor)

            // 3. 全场景万能高反差抗干扰轮廓引擎 (Universal Contrast Engine)
            val textLuminance = (0.299 * Color.red(textColor) + 0.587 * Color.green(textColor) + 0.114 * Color.blue(textColor)) / 255.0
            if (settings.floatingSubtitleBgStyle == "PURE_TRANSPARENT" || alpha <= 5) {
                if (textLuminance > 0.5) {
                    // 亮色文字：在纯白纸张或浅色底上，自动注入浓黑立体外发光阴影轮廓
                    textView?.setShadowLayer(dpToPx(4f).toFloat(), 0f, dpToPx(1.5f).toFloat(), Color.parseColor("#EE000000"))
                } else {
                    // 暗色文字：在深色底上，自动注入柔和高亮微光轮廓
                    textView?.setShadowLayer(dpToPx(4f).toFloat(), 0f, dpToPx(1.5f).toFloat(), Color.parseColor("#D9FFFFFF"))
                }
            } else if (settings.floatingSubtitleAdaptiveContrast) {
                if (textLuminance > 0.5) {
                    textView?.setShadowLayer(dpToPx(2.5f).toFloat(), 0f, dpToPx(1f).toFloat(), Color.parseColor("#B3000000"))
                } else {
                    textView?.setShadowLayer(dpToPx(2.5f).toFloat(), 0f, dpToPx(1f).toFloat(), Color.parseColor("#80FFFFFF"))
                }
            } else {
                textView?.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }

            // 4. 文字大小
            val sp = settings.floatingSubtitleFontSize.coerceIn(11, 28)
            textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp.toFloat())

            // 5. 文字对齐
            val align = if (settings.floatingSubtitleAlignment == "CENTER") {
                Gravity.CENTER
            } else {
                Gravity.START or Gravity.CENTER_VERTICAL
            }
            textView?.gravity = align

            // 6. 折叠行数
            textView?.maxLines = if (isExpanded) 14 else settings.floatingSubtitleMaxLines.coerceIn(1, 8)

            // 7. 图标显示与色调
            val showIcon = settings.floatingSubtitleShowIcon
            iconView?.visibility = if (showIcon) View.VISIBLE else View.GONE
            iconView?.setColorFilter(textColor)

            // 8. 关闭按钮风格
            val closeColor = if (!isEffectiveDark && (settings.floatingSubtitleBgStyle == "AUTO_ADAPTIVE" || settings.floatingSubtitleBgStyle == "LIGHT_FROST")) {
                Color.parseColor("#475569")
            } else if (settings.floatingSubtitleBgStyle == "PARCHMENT") {
                Color.parseColor("#D7CCC8")
            } else {
                Color.parseColor("#B0B5C5")
            }
            closeBtn?.setColorFilter(closeColor)
        }
    }

    /**
     * 初始化悬浮窗 View 与布局参数
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initView(settings: GlobalSettings) {
        val wm = windowManager ?: return

        val dpToPx = { dp: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, appContext.resources.displayMetrics).toInt()
        }

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = appContext.resources.displayMetrics
        val calculatedWidth = minOf(
            (displayMetrics.widthPixels * 0.92f).toInt(),
            displayMetrics.widthPixels - dpToPx(24f)
        )
        val defaultX = ((displayMetrics.widthPixels - calculatedWidth) / 2).coerceAtLeast(0)
        val defaultY = (displayMetrics.heightPixels - dpToPx(180f)).coerceAtLeast(dpToPx(60f))

        layoutParams = WindowManager.LayoutParams(
            calculatedWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultX
            y = defaultY
        }

        // 外层卡片容器
        val container = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12f), dpToPx(10f), dpToPx(8f), dpToPx(10f))
        }
        containerView = container

        // 左侧听书/字幕指示图标 (轻触可极速原地反转黑白深浅自适应模式)
        val icon = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            val iconSize = dpToPx(20f)
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = dpToPx(8f)
            }
            isClickable = true
            isFocusable = false
            setOnClickListener {
                isTemporaryInverted = !isTemporaryInverted
                try {
                    container.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (e: Exception) {
                    // ignore
                }
                val curSettings = ConfigDataStore.getInstance(appContext).settingsFlow.value
                applySettings(curSettings)
            }
        }
        iconView = icon

        // 核心文字显示区
        val tv = TextView(appContext).apply {
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dpToPx(2.5f).toFloat(), 1.2f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = lastSpokenText.ifBlank { "AI 听书字幕准备就绪" }
        }
        textView = tv

        // 右侧一键关闭按钮 (拥有充足的 38dp 点击热区)
        val close = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            val btnSize = dpToPx(38f)
            val iconPadding = dpToPx(8f)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            isClickable = true
            isFocusable = false
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = dpToPx(4f)
            }
            setOnClickListener {
                hide()
                // 发送关闭广播同步通知栏与配置
                val intent = Intent(TtsNotificationManager.ACTION_TOGGLE_FLOATING_SUBTITLE).apply {
                    setPackage(appContext.packageName)
                    putExtra("EXTRA_FORCE_DISABLE", true)
                }
                appContext.sendBroadcast(intent)
            }
        }
        closeBtn = close

        container.addView(icon)
        container.addView(tv)
        container.addView(close)

        // 应用个性化配置属性
        applySettings(settings)

        // 触摸拖动与点击展开监听
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false
        var lastTapTime = 0L
        val closeHitRect = Rect()
        val iconHitRect = Rect()

        container.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false

            // 若点击落在关闭按钮区域，将事件让渡给 closeBtn 处理
            close.getHitRect(closeHitRect)
            closeHitRect.inset(-dpToPx(6f), -dpToPx(6f))
            if (closeHitRect.contains(event.x.toInt(), event.y.toInt())) {
                return@setOnTouchListener false
            }

            // 若点击落在左侧指示图标区域，将事件让渡给 iconView 处理 (触发反转)
            if (icon.visibility == View.VISIBLE) {
                icon.getHitRect(iconHitRect)
                iconHitRect.inset(-dpToPx(6f), -dpToPx(6f))
                if (iconHitRect.contains(event.x.toInt(), event.y.toInt())) {
                    return@setOnTouchListener false
                }
            }

            val currentSettings = ConfigDataStore.getInstance(appContext).settingsFlow.value

            // 若开启了「锁定悬浮窗位置」，则禁止拖拽移位，避免听书翻页误触
            if (currentSettings.floatingSubtitleLockPosition) {
                if (event.action == MotionEvent.ACTION_UP) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 300) {
                        // 双击极速翻转黑白深浅自适应
                        isTemporaryInverted = !isTemporaryInverted
                        try {
                            container.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (e: Exception) {}
                        applySettings(currentSettings)
                        lastTapTime = 0L
                    } else {
                        lastTapTime = now
                        isExpanded = !isExpanded
                        tv.maxLines = if (isExpanded) 14 else currentSettings.floatingSubtitleMaxLines.coerceIn(1, 8)
                    }
                }
                return@setOnTouchListener true
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        isMoved = true
                        val screenW = appContext.resources.displayMetrics.widthPixels
                        val screenH = appContext.resources.displayMetrics.heightPixels
                        val currentW = container.width.takeIf { it > 0 } ?: lp.width
                        val currentH = container.height.takeIf { it > 0 } ?: dpToPx(60f)

                        lp.x = (initialX + dx).coerceIn(0, maxOf(0, screenW - currentW))
                        lp.y = (initialY + dy).coerceIn(dpToPx(20f), maxOf(dpToPx(20f), screenH - currentH))
                        try {
                            if (isViewAttached) {
                                wm.updateViewLayout(container, lp)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            // 双击极速翻转黑白深浅自适应
                            isTemporaryInverted = !isTemporaryInverted
                            try {
                                container.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            } catch (e: Exception) {}
                            applySettings(currentSettings)
                            lastTapTime = 0L
                        } else {
                            lastTapTime = now
                            // 单击切换展开 / 折叠全文
                            isExpanded = !isExpanded
                            tv.maxLines = if (isExpanded) 14 else currentSettings.floatingSubtitleMaxLines.coerceIn(1, 8)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        rootView = container
    }

    private fun attachToWindow() {
        val wm = windowManager ?: return
        val view = rootView ?: return
        val lp = layoutParams ?: return

        if (!PermissionManager.hasOverlayPermission(appContext)) {
            return
        }

        try {
            if (view.parent != null) {
                try {
                    wm.removeViewImmediate(view)
                } catch (e: Exception) {
                    // ignore
                }
            }
            wm.addView(view, lp)
            isViewAttached = true
        } catch (e: Exception) {
            isViewAttached = false
        }
    }

    private fun detachFromWindow() {
        val wm = windowManager ?: return
        val view = rootView ?: return

        try {
            if (isViewAttached || view.parent != null) {
                wm.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            isViewAttached = false
        }
    }

    /**
     * 彻底释放清理
     */
    fun destroy() {
        hide()
        try {
            appContext.unregisterComponentCallbacks(componentCallbacks)
        } catch (e: Exception) {
            // ignore
        }
        rootView = null
        containerView = null
        iconView = null
        textView = null
        closeBtn = null
        layoutParams = null
    }
}
