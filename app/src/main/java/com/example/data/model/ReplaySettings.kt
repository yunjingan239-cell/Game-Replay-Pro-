package com.example.data.model

data class ReplaySettings(
    val bufferDurationSeconds: Int = 60,
    val resolutionWidth: Int = 1920,
    val resolutionHeight: Int = 1080,
    val resolutionLabel: String = "1080p (FHD)",
    val fps: Int = 60,
    val bitrateMbps: Int = 12,
    val videoCodec: String = "HEVC", // "HEVC" (H.265 - 高达50%压缩省流) or "AVC" (H.264 - 极速兼容)
    val autoDetectOrientation: Boolean = true, // 自动识别横竖屏
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
     * Calculates the width and height adapting to the actual display orientation (landscape vs portrait).
     */
    fun getEffectiveDimensions(isLandscape: Boolean): Pair<Int, Int> {
        val maxDim = maxOf(resolutionWidth, resolutionHeight)
        val minDim = minOf(resolutionWidth, resolutionHeight)
        return if (isLandscape) {
            Pair(maxDim, minDim)
        } else {
            Pair(minDim, maxDim)
        }
    }
}
