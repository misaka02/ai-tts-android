package com.aitts.engine.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.aitts.engine.permission.PermissionManager

/**
 * 前台小说悬浮文本字幕管理器：
 * 1. 后台听书时在系统顶层悬浮展示当前正在朗读的小说文本；
 * 2. 支持手指在屏幕任意位置随心拖动；
 * 3. 支持点击展开/折叠长文本，支持快捷关闭按钮；
 * 4. 采用原生极轻量组件构建，内存开销极低 (<50KB)，生命周期稳定安全。
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

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(100f) // 默认距离屏幕底部 100dp
            width = (appContext.resources.displayMetrics.widthPixels * 0.92).toInt()
        }

        // 外层卡片容器
        val container = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12f), dpToPx(10f), dpToPx(10f), dpToPx(10f))

            // 现代深色半透磨砂质感圆角背景
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16f).toFloat()
                setColor(Color.parseColor("#E6181A20")) // 90% 不透明度暗夜黑
                setStroke(dpToPx(1f), Color.parseColor("#33FFFFFF")) // 20% 高光细边框
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

        // 右侧一键关闭按钮
        val closeBtn = ImageView(appContext).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#A0A5B5"))
            val btnSize = dpToPx(24f)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = dpToPx(6f)
            }
            setOnClickListener {
                hide()
                // 发送关闭广播同步通知栏
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

        container.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
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
                    val dy = (initialTouchY - event.rawY).toInt() // Y 轴由下往上
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isMoved = true
                        lp.x = initialX + dx
                        lp.y = (initialY + dy).coerceAtLeast(dpToPx(20f))
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
            if (!isViewAttached) {
                wm.addView(view, lp)
                isViewAttached = true
            }
        } catch (e: Exception) {
            isViewAttached = false
        }
    }

    private fun detachFromWindow() {
        val wm = windowManager ?: return
        val view = rootView ?: return

        try {
            if (isViewAttached) {
                wm.removeViewImmediate(view)
                isViewAttached = false
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
