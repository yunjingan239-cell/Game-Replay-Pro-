package com.example.data.model

import android.content.Context
import com.example.util.ScreenDimensionHelper

data class ReplaySettings(
    val bufferDurationSeconds: Int = 60,
    val resolutionWidth: Int = 2400,
    val resolutionHeight: Int = 1080,
    val resolutionLabel: String = "原生屏幕对齐 (完整无裁切 - 推荐)",
    val fps: Int = 60,
    val bitrateMbps: Int = 12,
    val videoCodec: String = "HEVC", // "HEVC" (H.265 - 高达50%压缩省流) or "AVC" (H.264 - 极速兼容)
    val autoDetectOrientation: Boolean = true, // 自动识别横竖屏
    val preferLandscapeForGames: Boolean = true, // 游戏回录优先适配横屏
    val hdrEnabled: Boolean = false, // 10-bit HDR / 宽色域录制支持
    val gameLaunchDetectionEnabled: Boolean = true, // 游戏启动自动检测
    val autoStartOnGameLaunch: Boolean = false, // 检测到游戏启动时自动激活回录
    val internalAudioEnabled: Boolean = true,
    val micAudioEnabled: Boolean = false,
    val floatingButtonEnabled: Boolean = true,
    val floatingButtonOpacity: Float = 0.85f,
    val floatingButtonSizeDp: Int = 48, // 36dp (微型), 48dp (标准), 60dp (大号)
    val isFloatingButtonCollapsed: Boolean = false,
    val isGameModeActive: Boolean = false, // 沉浸游戏模式：隐藏非必要UI
    val volumeKeyShortcutEnabled: Boolean = true, // 音量键快捷组合键触发保存
    val currentGameTitle: String = "王者荣耀",
    val lowPowerOptimization: Boolean = true
) {
    val chunkDurationSeconds: Int = 10
    val maxChunksInRing: Int get() = (bufferDurationSeconds / chunkDurationSeconds).coerceAtLeast(3)

    /**
     * Calculates width and height adapting to the physical display's exact aspect ratio.
     * Prevents any black bars, letterboxing, or game UI clipping.
     */
    fun getEffectiveDimensions(context: Context? = null, isLandscape: Boolean): Pair<Int, Int> {
        if (context != null) {
            return ScreenDimensionHelper.calculateDimensions(context, this, isLandscape)
        }
        val maxDim = maxOf(resolutionWidth, resolutionHeight)
        val minDim = minOf(resolutionWidth, resolutionHeight)
        return if (isLandscape) {
            Pair(maxDim, minDim)
        } else {
            Pair(minDim, maxDim)
        }
    }
}
