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

    companion object {
        private const val PREFS_NAME = "game_replay_settings_prefs"
        private const val KEY_BUFFER_DURATION = "key_buffer_duration"
        private const val KEY_RES_WIDTH = "key_res_width"
        private const val KEY_RES_HEIGHT = "key_res_height"
        private const val KEY_RES_LABEL = "key_res_label"
        private const val KEY_FPS = "key_fps"
        private const val KEY_BITRATE = "key_bitrate"
        private const val KEY_CODEC = "key_codec"
        private const val KEY_AUTO_ORIENTATION = "key_auto_orientation"
        private const val KEY_PREFER_LANDSCAPE = "key_prefer_landscape"
        private const val KEY_HDR = "key_hdr"
        private const val KEY_GAME_DETECTION = "key_game_detection"
        private const val KEY_AUTO_START_GAME = "key_auto_start_game"
        private const val KEY_INTERNAL_AUDIO = "key_internal_audio"
        private const val KEY_MIC_AUDIO = "key_mic_audio"
        private const val KEY_FLOAT_BTN_ENABLED = "key_float_btn_enabled"
        private const val KEY_FLOAT_BTN_OPACITY = "key_float_btn_opacity"
        private const val KEY_FLOAT_BTN_SIZE = "key_float_btn_size"
        private const val KEY_FLOAT_BTN_COLLAPSED = "key_float_btn_collapsed"
        private const val KEY_GAME_MODE_ACTIVE = "key_game_mode_active"
        private const val KEY_VOLUME_SHORTCUT = "key_volume_shortcut"
        private const val KEY_CURRENT_GAME = "key_current_game"
        private const val KEY_LOW_POWER = "key_low_power"

        fun load(context: Context): ReplaySettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val default = ReplaySettings()
            return ReplaySettings(
                bufferDurationSeconds = prefs.getInt(KEY_BUFFER_DURATION, default.bufferDurationSeconds),
                resolutionWidth = prefs.getInt(KEY_RES_WIDTH, default.resolutionWidth),
                resolutionHeight = prefs.getInt(KEY_RES_HEIGHT, default.resolutionHeight),
                resolutionLabel = prefs.getString(KEY_RES_LABEL, default.resolutionLabel) ?: default.resolutionLabel,
                fps = prefs.getInt(KEY_FPS, default.fps),
                bitrateMbps = prefs.getInt(KEY_BITRATE, default.bitrateMbps),
                videoCodec = prefs.getString(KEY_CODEC, default.videoCodec) ?: default.videoCodec,
                autoDetectOrientation = prefs.getBoolean(KEY_AUTO_ORIENTATION, default.autoDetectOrientation),
                preferLandscapeForGames = prefs.getBoolean(KEY_PREFER_LANDSCAPE, default.preferLandscapeForGames),
                hdrEnabled = prefs.getBoolean(KEY_HDR, default.hdrEnabled),
                gameLaunchDetectionEnabled = prefs.getBoolean(KEY_GAME_DETECTION, default.gameLaunchDetectionEnabled),
                autoStartOnGameLaunch = prefs.getBoolean(KEY_AUTO_START_GAME, default.autoStartOnGameLaunch),
                internalAudioEnabled = prefs.getBoolean(KEY_INTERNAL_AUDIO, default.internalAudioEnabled),
                micAudioEnabled = prefs.getBoolean(KEY_MIC_AUDIO, default.micAudioEnabled),
                floatingButtonEnabled = prefs.getBoolean(KEY_FLOAT_BTN_ENABLED, default.floatingButtonEnabled),
                floatingButtonOpacity = prefs.getFloat(KEY_FLOAT_BTN_OPACITY, default.floatingButtonOpacity),
                floatingButtonSizeDp = prefs.getInt(KEY_FLOAT_BTN_SIZE, default.floatingButtonSizeDp),
                isFloatingButtonCollapsed = prefs.getBoolean(KEY_FLOAT_BTN_COLLAPSED, default.isFloatingButtonCollapsed),
                isGameModeActive = prefs.getBoolean(KEY_GAME_MODE_ACTIVE, default.isGameModeActive),
                volumeKeyShortcutEnabled = prefs.getBoolean(KEY_VOLUME_SHORTCUT, default.volumeKeyShortcutEnabled),
                currentGameTitle = prefs.getString(KEY_CURRENT_GAME, default.currentGameTitle) ?: default.currentGameTitle,
                lowPowerOptimization = prefs.getBoolean(KEY_LOW_POWER, default.lowPowerOptimization)
            )
        }

        fun save(context: Context, settings: ReplaySettings) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_BUFFER_DURATION, settings.bufferDurationSeconds)
                .putInt(KEY_RES_WIDTH, settings.resolutionWidth)
                .putInt(KEY_RES_HEIGHT, settings.resolutionHeight)
                .putString(KEY_RES_LABEL, settings.resolutionLabel)
                .putInt(KEY_FPS, settings.fps)
                .putInt(KEY_BITRATE, settings.bitrateMbps)
                .putString(KEY_CODEC, settings.videoCodec)
                .putBoolean(KEY_AUTO_ORIENTATION, settings.autoDetectOrientation)
                .putBoolean(KEY_PREFER_LANDSCAPE, settings.preferLandscapeForGames)
                .putBoolean(KEY_HDR, settings.hdrEnabled)
                .putBoolean(KEY_GAME_DETECTION, settings.gameLaunchDetectionEnabled)
                .putBoolean(KEY_AUTO_START_GAME, settings.autoStartOnGameLaunch)
                .putBoolean(KEY_INTERNAL_AUDIO, settings.internalAudioEnabled)
                .putBoolean(KEY_MIC_AUDIO, settings.micAudioEnabled)
                .putBoolean(KEY_FLOAT_BTN_ENABLED, settings.floatingButtonEnabled)
                .putFloat(KEY_FLOAT_BTN_OPACITY, settings.floatingButtonOpacity)
                .putInt(KEY_FLOAT_BTN_SIZE, settings.floatingButtonSizeDp)
                .putBoolean(KEY_FLOAT_BTN_COLLAPSED, settings.isFloatingButtonCollapsed)
                .putBoolean(KEY_GAME_MODE_ACTIVE, settings.isGameModeActive)
                .putBoolean(KEY_VOLUME_SHORTCUT, settings.volumeKeyShortcutEnabled)
                .putString(KEY_CURRENT_GAME, settings.currentGameTitle)
                .putBoolean(KEY_LOW_POWER, settings.lowPowerOptimization)
                .commit()
        }
    }
}
