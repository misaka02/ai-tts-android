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
    private var paletteBtn: ImageView? = null
    private var paletteBar: LinearLayout? = null
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
        ScreenColorSampler.getInstance(appContext).setOnColorSampledListener { _, _ ->
            mainHandler.post {
                if (isViewAttached) {
                    val settings = ConfigDataStore.getInstance(appContext).settingsFlow.value
                    if (settings.isRealScreenSamplingEnabled || settings.floatingSubtitleNovelPreset == "AUTO") {
                        applySettings(settings)
                    }
                }
            }
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

            // 0. 判断当前背景预设与真实屏幕像素采样
            val alpha = (settings.floatingSubtitleOpacity.coerceIn(0f, 1f) * 255).toInt()
            var isEffectiveDark = false
            var baseColor: Int
            var textColor: Int
            var strokeColor: Int

            when (settings.floatingSubtitleNovelPreset) {
                "WHITE" -> {
                    // 经典纯白书页预设 (高透纯白底 + 曜石深黑高对比字)
                    baseColor = Color.parseColor("#FFFFFF")
                    textColor = Color.parseColor("#0F172A")
                    strokeColor = Color.argb((alpha * 0.22f).toInt().coerceIn(0, 255), 15, 23, 42)
                    isEffectiveDark = false
                }
                "PARCHMENT" -> {
                    // 经典暖阳羊皮纸/米黄底色预设 (暖纸褐字)
                    baseColor = Color.parseColor("#F6EED8")
                    textColor = Color.parseColor("#382314")
                    strokeColor = Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 180, 140, 90)
                    isEffectiveDark = false
                }
                "GREEN" -> {
                    // 经典护眼豆沙绿预设 (柔和淡绿底 + 墨绿字)
                    baseColor = Color.parseColor("#D3E7D5")
                    textColor = Color.parseColor("#123524")
                    strokeColor = Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 90, 160, 110)
                    isEffectiveDark = false
                }
                "INK_GREY" -> {
                    // 水墨浅灰预设 (墨水屏素灰底 + 纯黑字)
                    baseColor = Color.parseColor("#E2E8F0")
                    textColor = Color.parseColor("#000000")
                    strokeColor = Color.argb((alpha * 0.25f).toInt().coerceIn(0, 255), 100, 116, 139)
                    isEffectiveDark = false
                }
                "NIGHT" -> {
                    // 暗夜极黑预设 (纯黑夜间底 + 极光象牙白字)
                    baseColor = Color.parseColor("#121316")
                    textColor = Color.parseColor("#F5F5F7")
                    strokeColor = Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 255, 255, 255)
                    isEffectiveDark = true
                }
                else -> {
                    // AUTO 模式：优先使用 MediaProjection 真实屏幕背后像素采样
                    val sampler = ScreenColorSampler.getInstance(appContext)
                    if (sampler.isSamplingActive()) {
                        val sampled = sampler.lastSampledColor
                        val lum = sampler.lastSampledLuminance
                        if (lum > 140) {
                            // 背后是浅色小说纸张/纯白/米黄等明亮背景
                            baseColor = Color.argb(255, Color.red(sampled), Color.green(sampled), Color.blue(sampled))
                            textColor = Color.parseColor("#0F172A")
                            strokeColor = Color.argb((alpha * 0.25f).toInt().coerceIn(0, 255), 15, 23, 42)
                            isEffectiveDark = false
                        } else {
                            // 背后是暗色小说夜间模式或深色屏幕
                            baseColor = Color.parseColor("#181A20")
                            textColor = Color.parseColor("#F5F5F7")
                            strokeColor = Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 255, 255, 255)
                            isEffectiveDark = true
                        }
                    } else {
                        // 降级回系统昼夜感知与风格预设
                        val isSystemNight = (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        isEffectiveDark = when (settings.floatingSubtitleBgStyle) {
                            "AUTO_ADAPTIVE" -> if (isTemporaryInverted) !isSystemNight else isSystemNight
                            "LIGHT_FROST" -> isTemporaryInverted
                            "DARK_FROST", "AMOLED_BLACK" -> !isTemporaryInverted
                            "PARCHMENT" -> false
                            else -> if (isTemporaryInverted) !isSystemNight else isSystemNight
                        }
                        baseColor = when (settings.floatingSubtitleBgStyle) {
                            "AUTO_ADAPTIVE" -> if (isEffectiveDark) Color.parseColor("#181A20") else Color.parseColor("#FFFFFF")
                            "LIGHT_FROST" -> if (isEffectiveDark) Color.parseColor("#181A20") else Color.parseColor("#F8FAFC")
                            "PARCHMENT" -> Color.parseColor("#2B231D")
                            "AMOLED_BLACK" -> Color.parseColor("#000000")
                            else -> Color.parseColor("#181A20")
                        }
                        textColor = if (!isEffectiveDark && settings.floatingSubtitleTextColor.equals("#F5F5F7", ignoreCase = true)) {
                            Color.parseColor("#0F172A")
                        } else if (isEffectiveDark && settings.floatingSubtitleTextColor.equals("#0F172A", ignoreCase = true)) {
                            Color.parseColor("#F5F5F7")
                        } else {
                            try {
                                Color.parseColor(settings.floatingSubtitleTextColor)
                            } catch (e: Exception) {
                                if (isEffectiveDark) Color.parseColor("#F5F5F7") else Color.parseColor("#0F172A")
                            }
                        }
                        strokeColor = if (isEffectiveDark) {
                            Color.argb((alpha * 0.35f).toInt().coerceIn(0, 255), 255, 255, 255)
                        } else {
                            Color.argb((alpha * 0.25f).toInt().coerceIn(0, 255), 15, 23, 42)
                        }
                    }
                }
            }

            // 1. 背景材质与不透明度合成
            if (settings.floatingSubtitleBgStyle == "PURE_TRANSPARENT" || alpha <= 5) {
                containerView?.background = null
                containerView?.elevation = 0f
            } else {
                val bgColor = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(16f).toFloat()
                    setColor(bgColor)
                    setStroke(dpToPx(1f), strokeColor)
                }
                containerView?.background = bg
                containerView?.elevation = dpToPx(6f).toFloat()
            }

            // 2. 文字颜色
            textView?.setTextColor(textColor)

            // 3. 全场景万能高反差抗干扰轮廓引擎 (Universal Contrast Engine)
            val textLuminance = (0.299 * Color.red(textColor) + 0.587 * Color.green(textColor) + 0.114 * Color.blue(textColor)) / 255.0
            if (settings.floatingSubtitleBgStyle == "PURE_TRANSPARENT" || alpha <= 5) {
                if (textLuminance > 0.5) {
                    textView?.setShadowLayer(dpToPx(4.5f).toFloat(), 0f, dpToPx(1.5f).toFloat(), Color.parseColor("#F2000000"))
                } else {
                    textView?.setShadowLayer(dpToPx(4.5f).toFloat(), 0f, dpToPx(1.5f).toFloat(), Color.parseColor("#E6FFFFFF"))
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
            paletteBtn?.setColorFilter(textColor)

            // 8. 关闭按钮风格
            val closeColor = if (!isEffectiveDark) Color.parseColor("#475569") else Color.parseColor("#B0B5C5")
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
            (displayMetrics.widthPixels * 0.94f).toInt(),
            displayMetrics.widthPixels - dpToPx(20f)
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

        // 外层卡片容器 (Vertical 垂直排列主内容与小说底色速配条)
        val container = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10f), dpToPx(8f), dpToPx(8f), dpToPx(8f))
        }
        containerView = container

        // 主行容器 (Horizontal)
        val mainRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 1. 左侧听书指示图标 (轻触可极速原地反转深浅自适应模式)
        val icon = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            val iconSize = dpToPx(20f)
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = dpToPx(6f)
            }
            isClickable = true
            isFocusable = false
            setOnClickListener {
                isTemporaryInverted = !isTemporaryInverted
                try {
                    container.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (e: Exception) {}
                val curSettings = ConfigDataStore.getInstance(appContext).settingsFlow.value
                applySettings(curSettings)
            }
        }
        iconView = icon

        // 2. 🎨 小说底色速配盘触发按钮
        val palette = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_menu_set_as)
            val btnSize = dpToPx(22f)
            val p = dpToPx(2f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginEnd = dpToPx(6f)
            }
            isClickable = true
            isFocusable = false
        }
        paletteBtn = palette

        // 3. 核心小说文本显示区
        val tv = TextView(appContext).apply {
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dpToPx(2.5f).toFloat(), 1.2f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = lastSpokenText.ifBlank { "AI 听书字幕准备就绪" }
        }
        textView = tv

        // 4. 右侧一键关闭按钮
        val close = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            val btnSize = dpToPx(36f)
            val iconPadding = dpToPx(8f)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            isClickable = true
            isFocusable = false
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = dpToPx(4f)
            }
            setOnClickListener {
                hide()
                val intent = Intent(TtsNotificationManager.ACTION_TOGGLE_FLOATING_SUBTITLE).apply {
                    setPackage(appContext.packageName)
                    putExtra("EXTRA_FORCE_DISABLE", true)
                }
                appContext.sendBroadcast(intent)
            }
        }
        closeBtn = close

        mainRow.addView(icon)
        mainRow.addView(palette)
        mainRow.addView(tv)
        mainRow.addView(close)

        // 5. 展开式小说底色速配工具条 (5 大经典小说纸张 + 屏幕真实取色)
        val paletteRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(0, dpToPx(6f), 0, dpToPx(2f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        paletteBar = paletteRow

        // 辅助生成底色 Chip
        fun createNovelChip(label: String, bgHex: String, textHex: String, presetKey: String): TextView {
            return TextView(appContext).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTextColor(Color.parseColor(textHex))
                gravity = Gravity.CENTER
                setPadding(dpToPx(7f), dpToPx(3f), dpToPx(7f), dpToPx(3f))
                val chipBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(8f).toFloat()
                    setColor(Color.parseColor(bgHex))
                    setStroke(dpToPx(1f), Color.parseColor("#66808080"))
                }
                background = chipBg
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(5f)
                }
                layoutParams = lp
                isClickable = true
                isFocusable = false
                setOnClickListener {
                    try {
                        container.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } catch (e: Exception) {}
                    if (presetKey == "AUTO") {
                        val sampler = ScreenColorSampler.getInstance(appContext)
                        if (!sampler.isSamplingActive()) {
                            sampler.requestPermission(appContext)
                        }
                        val updated = ConfigDataStore.getInstance(appContext).settingsFlow.value.copy(
                            floatingSubtitleNovelPreset = "AUTO",
                            isRealScreenSamplingEnabled = true
                        )
                        ConfigDataStore.getInstance(appContext).updateSettings(updated)
                        applySettings(updated)
                    } else {
                        val updated = ConfigDataStore.getInstance(appContext).settingsFlow.value.copy(
                            floatingSubtitleNovelPreset = presetKey
                        )
                        ConfigDataStore.getInstance(appContext).updateSettings(updated)
                        applySettings(updated)
                    }
                    paletteRow.visibility = View.GONE
                }
            }
        }

        paletteRow.addView(createNovelChip("📄白纸", "#FFFFFF", "#0F172A", "WHITE"))
        paletteRow.addView(createNovelChip("📜米黄", "#F6EED8", "#382314", "PARCHMENT"))
        paletteRow.addView(createNovelChip("🍃豆沙", "#D3E7D5", "#123524", "GREEN"))
        paletteRow.addView(createNovelChip("🌫️水墨", "#E2E8F0", "#000000", "INK_GREY"))
        paletteRow.addView(createNovelChip("🌙暗夜", "#121316", "#F5F5F7", "NIGHT"))
        paletteRow.addView(createNovelChip("🔍取色", "#2563EB", "#FFFFFF", "AUTO"))

        // 点击 🎨 按钮展开/折叠底色盘
        palette.setOnClickListener {
            try {
                container.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (e: Exception) {}
            paletteRow.visibility = if (paletteRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        container.addView(mainRow)
        container.addView(paletteRow)

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
        val paletteHitRect = Rect()
        val paletteBarHitRect = Rect()

        container.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false

            // 若点击落在关闭按钮区域，将事件让渡给 closeBtn 处理
            close.getHitRect(closeHitRect)
            closeHitRect.inset(-dpToPx(6f), -dpToPx(6f))
            if (closeHitRect.contains(event.x.toInt(), event.y.toInt())) {
                return@setOnTouchListener false
            }

            // 若点击落在左侧指示图标区域，让渡给 iconView 处理 (触发反转)
            if (icon.visibility == View.VISIBLE) {
                icon.getHitRect(iconHitRect)
                iconHitRect.inset(-dpToPx(6f), -dpToPx(6f))
                if (iconHitRect.contains(event.x.toInt(), event.y.toInt())) {
                    return@setOnTouchListener false
                }
            }

            // 若点击落在 🎨 调色盘按钮区域，让渡给 palette 处理 (展开/收起底色条)
            palette.getHitRect(paletteHitRect)
            paletteHitRect.inset(-dpToPx(6f), -dpToPx(6f))
            if (paletteHitRect.contains(event.x.toInt(), event.y.toInt())) {
                return@setOnTouchListener false
            }

            // 若底色条展开且点击在底色条区域，让渡给底色条子项处理
            if (paletteRow.visibility == View.VISIBLE) {
                paletteRow.getHitRect(paletteBarHitRect)
                if (paletteBarHitRect.contains(event.x.toInt(), event.y.toInt())) {
                    return@setOnTouchListener false
                }
            }

            val currentSettings = ConfigDataStore.getInstance(appContext).settingsFlow.value

            // 若开启了「锁定悬浮窗位置」，则禁止拖拽移位，避免听书翻页误触
            if (currentSettings.floatingSubtitleLockPosition) {
                if (event.action == MotionEvent.ACTION_UP) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 300) {
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
        try {
            ScreenColorSampler.getInstance(appContext).stopSampling()
        } catch (e: Exception) {
            // ignore
        }
        rootView = null
        containerView = null
        iconView = null
        paletteBtn = null
        paletteBar = null
        textView = null
        closeBtn = null
        layoutParams = null
    }
}
