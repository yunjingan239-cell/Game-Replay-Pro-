package com.example.data.model

import android.content.Context
import com.example.util.ScreenDimensionHelper
import kotlin.math.roundToInt

data class ReplaySettings(
    val bufferDurationSeconds: Int = 60,
    val resolutionWidth: Int = 2400,
    val resolutionHeight: Int = 1080,
    val resolutionLabel: String = "原生对齐 (无裁切)",
    val fps: Int = 60,
    val bitrateMbps: Int = 36, // Default 36 Mbps for ultra-sharp, macroblock-free visual quality
    val videoCodec: String = "HEVC", // "HEVC" (H.265) or "AVC" (H.264)
    val autoDetectOrientation: Boolean = true,
    val preferLandscapeForGames: Boolean = true,
    val hdrEnabled: Boolean = false,
    val gameLaunchDetectionEnabled: Boolean = true,
    val autoStartOnGameLaunch: Boolean = false,
    val internalAudioEnabled: Boolean = true,
    val micAudioEnabled: Boolean = false,
    val floatingButtonEnabled: Boolean = true,
    val floatingButtonOpacity: Float = 0.85f,
    val floatingButtonSizeDp: Int = 48,
    val isFloatingButtonCollapsed: Boolean = false,
    val isGameModeActive: Boolean = false,
    val volumeKeyShortcutEnabled: Boolean = true,
    val currentGameTitle: String = "王者荣耀",
    val lowPowerOptimization: Boolean = false,
    val gamePriorityShield: Boolean = true,
    val smartStorageGuard: Boolean = true,
    val autoDimFloatingButton: Boolean = true,
    val isProMember: Boolean = false,
    val proPlanName: String = "未激活",
    val autoThermalGuard: Boolean = true,
    val batterySaverVbr: Boolean = false // Default false so constant/high bitrate prevents macroblocking in fast motion
) {
    val chunkDurationSeconds: Int = 10
    val maxChunksInRing: Int get() = (bufferDurationSeconds / chunkDurationSeconds).coerceAtLeast(3)

    /**
     * Calculates width and height adapting to the physical display's exact aspect ratio.
     * Prevents any black bars, letterboxing, or game UI clipping.
     */
    fun getEffectiveDimensions(
        context: Context? = null,
        isLandscape: Boolean,
        alignStep: Int = 2
    ): Pair<Int, Int> {
        if (context != null) {
            return ScreenDimensionHelper.calculateDimensions(context, this, isLandscape, alignStep)
        }
        val maxDim = maxOf(resolutionWidth, resolutionHeight)
        val minDim = minOf(resolutionWidth, resolutionHeight)
        return if (isLandscape) {
            Pair(maxDim, minDim)
        } else {
            Pair(minDim, maxDim)
        }
    }

    /**
     * Computes the target encoder bitrate in bits per second (bps), scaling automatically
     * with resolution (>1080p) and high frame rates (90/120 FPS) so fast-motion game frames
     * never suffer from macroblocking or compression blur.
     */
    fun getEffectiveBitrateBps(width: Int, height: Int): Int {
        val baseMbps = bitrateMbps.coerceIn(12, 80)
        val pixelCount = (width.toLong() * height.toLong()).coerceAtLeast(1280L * 720L)
        val base1080pPixels = 1920f * 1080f
        // Scale bitrate up when recording high-res native screens (e.g., 2656x1200 or 2K)
        val resolutionFactor = (pixelCount.toFloat() / base1080pPixels).coerceIn(0.85f, 1.85f)
        // Scale bitrate up for 90 FPS / 120 FPS
        val fpsFactor = when {
            fps >= 120 -> 1.45f
            fps >= 90 -> 1.25f
            fps <= 30 -> 0.80f
            else -> 1.0f
        }
        // AVC (H.264) needs ~25% more bitrate than HEVC (H.265) for equivalent visual sharpness
        val codecFactor = if (videoCodec.contains("AVC", ignoreCase = true)) 1.25f else 1.0f

        val targetMbps = (baseMbps * resolutionFactor * fpsFactor * codecFactor)
            .roundToInt()
            .coerceIn(baseMbps, 80)
        return targetMbps * 1_000_000
    }

    companion object {
        private const val PREFS_NAME = "game_replay_settings_prefs"
        private const val KEY_BUFFER_DURATION = "key_buffer_duration"
        private const val KEY_RES_WIDTH = "key_res_width"
        private const val KEY_RES_HEIGHT = "key_res_height"
        private const val KEY_RES_LABEL = "key_res_label"
        private const val KEY_FPS = "key_fps"
        private const val KEY_BITRATE_V2 = "key_bitrate_v2_hq"
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
        private const val KEY_GAME_PRIORITY = "key_game_priority"
        private const val KEY_SMART_STORAGE = "key_smart_storage"
        private const val KEY_AUTO_DIM_BTN = "key_auto_dim_btn"
        private const val KEY_IS_PRO = "key_is_pro_member"
        private const val KEY_PRO_PLAN = "key_pro_plan_name"
        private const val KEY_THERMAL_GUARD = "key_thermal_guard"
        private const val KEY_BATTERY_VBR_V2 = "key_battery_vbr_v2_hq"

        fun load(context: Context): ReplaySettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val default = ReplaySettings()
            return ReplaySettings(
                bufferDurationSeconds = prefs.getInt(KEY_BUFFER_DURATION, default.bufferDurationSeconds),
                resolutionWidth = prefs.getInt(KEY_RES_WIDTH, default.resolutionWidth),
                resolutionHeight = prefs.getInt(KEY_RES_HEIGHT, default.resolutionHeight),
                resolutionLabel = prefs.getString(KEY_RES_LABEL, default.resolutionLabel) ?: default.resolutionLabel,
                fps = prefs.getInt(KEY_FPS, default.fps),
                bitrateMbps = prefs.getInt(KEY_BITRATE_V2, default.bitrateMbps),
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
                lowPowerOptimization = prefs.getBoolean(KEY_LOW_POWER, default.lowPowerOptimization),
                gamePriorityShield = prefs.getBoolean(KEY_GAME_PRIORITY, default.gamePriorityShield),
                smartStorageGuard = prefs.getBoolean(KEY_SMART_STORAGE, default.smartStorageGuard),
                autoDimFloatingButton = prefs.getBoolean(KEY_AUTO_DIM_BTN, default.autoDimFloatingButton),
                isProMember = prefs.getBoolean(KEY_IS_PRO, default.isProMember),
                proPlanName = prefs.getString(KEY_PRO_PLAN, default.proPlanName) ?: default.proPlanName,
                autoThermalGuard = prefs.getBoolean(KEY_THERMAL_GUARD, default.autoThermalGuard),
                batterySaverVbr = prefs.getBoolean(KEY_BATTERY_VBR_V2, default.batterySaverVbr)
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
                .putInt(KEY_BITRATE_V2, settings.bitrateMbps)
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
                .putBoolean(KEY_GAME_PRIORITY, settings.gamePriorityShield)
                .putBoolean(KEY_SMART_STORAGE, settings.smartStorageGuard)
                .putBoolean(KEY_AUTO_DIM_BTN, settings.autoDimFloatingButton)
                .putBoolean(KEY_IS_PRO, settings.isProMember)
                .putString(KEY_PRO_PLAN, settings.proPlanName)
                .putBoolean(KEY_THERMAL_GUARD, settings.autoThermalGuard)
                .putBoolean(KEY_BATTERY_VBR_V2, settings.batterySaverVbr)
                .commit()
        }
    }
}
