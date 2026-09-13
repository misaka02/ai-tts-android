package com.aitts.engine.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.aitts.engine.permission.PermissionManager

/**
 * 前台小说悬浮文本字幕管理器：
 * 1. 后台听书时在系统顶层悬浮展示当前正在朗读的小说文本；
 * 2. 采用 Gravity.TOP or Gravity.START 绝对坐标系与 FLAG_LAYOUT_IN_SCREEN，杜绝屏外渲染与各厂商 ROM 适配异常；
 * 3. 边界限定拖拽，手指随心拖动不飞出屏幕；
 * 4. 支持点击文本区域展开/折叠长文本 (3行 <-> 12行)，专属关闭按钮防冲突退出；
 * 5. 极轻量 (<50KB)，生命周期稳定安全。
 */
class FloatingSubtitleManager private constructor(private val appContext: Context) {

    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootView: View? = null
    private var textView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isViewAttached = false
    private var isExpanded = false
    private var lastSpokenText: String = ""

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
            if (rootView == null) {
                initView()
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
            if (rootView == null) {
                initView()
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
     * 初始化悬浮窗 View 与布局参数
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
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

            // 现代深色磨砂质感圆角高亮背景
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16f).toFloat()
                setColor(Color.parseColor("#EE181A20")) // 93% 不透明度暗夜极光灰
                setStroke(dpToPx(1f), Color.parseColor("#4DFFFFFF")) // 30% 白银微光边框
            }
            background = bg
            elevation = dpToPx(8f).toFloat()
        }

        // 左侧听书/字幕指示图标
        val iconView = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setColorFilter(Color.parseColor("#80DEEA")) // 青色微光点缀
            val iconSize = dpToPx(20f)
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = dpToPx(8f)
            }
        }

        // 核心文字显示区
        textView = TextView(appContext).apply {
            setTextColor(Color.parseColor("#F5F5F7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dpToPx(2f).toFloat(), 1.15f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = lastSpokenText.ifBlank { "AI 听书字幕准备就绪" }
        }

        // 右侧一键关闭按钮 (拥有充足的 38dp 点击热区)
        val closeBtn = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#B0B5C5"))
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

        container.addView(iconView)
        container.addView(textView)
        container.addView(closeBtn)

        // 触摸拖动与点击展开监听
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false
        val closeHitRect = Rect()

        container.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false

            // 若点击落在关闭按钮区域，将事件让渡给 closeBtn 处理
            closeBtn.getHitRect(closeHitRect)
            closeHitRect.inset(-dpToPx(6f), -dpToPx(6f))
            if (closeHitRect.contains(event.x.toInt(), event.y.toInt())) {
                return@setOnTouchListener false
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
                        // 点击切换展开 / 折叠全文
                        isExpanded = !isExpanded
                        textView?.maxLines = if (isExpanded) 12 else 3
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
        rootView = null
        textView = null
        layoutParams = null
    }
}
