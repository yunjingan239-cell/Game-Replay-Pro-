package com.example.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.data.model.ReplaySettings

/**
 * Screen dimension utility to ensure screen recordings match the device's physical aspect ratio,
 * preventing any video clipping, letterboxing, or distortion in gaming scenarios.
 */
object ScreenDimensionHelper {

    /**
     * Retrieves the physical screen dimensions (real width and real height) in pixels.
     */
    fun getRealDisplayMetrics(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return Pair(1080, 2400)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay?.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    /**
     * Calculates width and height that align with the physical screen's aspect ratio.
     * Dimensions are aligned to multiples of 16 for optimal hardware video encoder (MediaCodec) compatibility.
     */
    fun calculateDimensions(
        context: Context,
        settings: ReplaySettings,
        isLandscape: Boolean
    ): Pair<Int, Int> {
        val (realW, realH) = getRealDisplayMetrics(context)
        val maxScreen = maxOf(realW, realH).coerceAtLeast(1280)
        val minScreen = minOf(realW, realH).coerceAtLeast(720)
        val aspectRatio = maxScreen.toFloat() / minScreen.toFloat()

        val targetMin = when {
            settings.resolutionLabel.contains("720p") -> 720
            settings.resolutionLabel.contains("2K") -> 1440
            settings.resolutionLabel.contains("原生") -> minScreen
            else -> 1080 // Default to 1080p height/width
        }

        // Align dimensions to 16-pixel boundaries for hardware encoders
        val alignedMin = (targetMin / 16) * 16
        val rawMax = (alignedMin * aspectRatio).toInt()
        val alignedMax = (rawMax / 16) * 16

        return if (isLandscape) {
            Pair(alignedMax, alignedMin)
        } else {
            Pair(alignedMin, alignedMax)
        }
    }
}
