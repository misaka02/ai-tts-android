package com.aitts.engine.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.aitts.engine.service.ScreenColorSampler

/**
 * 透明无感 Activity：用于唤起系统 MediaProjection 屏幕捕获授权对话框，
 * 授权成功后将 token 传递给 ScreenColorSampler 进行悬浮窗正下方像素色值采样。
 */
class ScreenCapturePermissionActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 10102

        fun start(context: Context) {
            val intent = Intent(context, ScreenCapturePermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager == null) {
            ScreenColorSampler.getInstance(this).onPermissionResult(RESULT_CANCELED, null)
            finish()
            return
        }

        try {
            @Suppress("DEPRECATION")
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
        } catch (e: Exception) {
            ScreenColorSampler.getInstance(this).onPermissionResult(RESULT_CANCELED, null)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            ScreenColorSampler.getInstance(this).onPermissionResult(resultCode, data)
            finish()
        }
    }
}
