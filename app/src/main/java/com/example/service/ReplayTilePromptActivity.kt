package com.example.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * Transparent helper activity launched from the Quick Settings Tile (控制中心快捷开关).
 * Requests MediaProjection with [MediaProjectionConfig.createConfigForDefaultDisplay]
 * on Android 14+ (API 34+) to force full-screen capture directly, preventing the system
 * single-app capture dialog ("应用范围：共享一个应用") from disrupting gameplay.
 */
class ReplayTilePromptActivity : Activity() {

    private val tag = "ReplayTilePromptAct"
    private val requestCodeCapture = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity transition completely instantaneous and invisible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Key: Default to capturing the entire physical display directly (跳过单应用选择器，直奔全屏录制)
                val config = MediaProjectionConfig.createConfigForDefaultDisplay()
                projectionManager.createScreenCaptureIntent(config)
            } else {
                projectionManager.createScreenCaptureIntent()
            }
            startActivityForResult(captureIntent, requestCodeCapture)
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch screen capture intent from tile", e)
            Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeCapture) {
            if (resultCode == RESULT_OK && data != null) {
                // Start hardware replay recorder service immediately
                ReplayRecorderService.start(this, resultCode, data)
                val app = application as? com.example.GameReplayApplication
                val settings = app?.repository?.settings?.value
                if (settings?.floatingButtonEnabled == true && android.provider.Settings.canDrawOverlays(this)) {
                    FloatingOverlayService.start(this)
                }
                ReplayTileService.requestTileUpdate(this)
                Toast.makeText(this, "回录已开启", Toast.LENGTH_SHORT).show()
            } else {
                ReplayTileService.requestTileUpdate(this)
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show()
            }
            finish()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }
    }
}
