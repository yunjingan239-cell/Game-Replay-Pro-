package com.example.util

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.example.data.model.ReplaySettings
import kotlin.math.roundToInt

/**
 * Screen dimension utility to ensure screen recordings match the device's physical aspect ratio
 * with 1:1 native pixel fidelity when native/ultra quality is selected, preventing bilinear scaling blur,
 * video clipping, letterboxing, or distortion in gaming scenarios.
 */
object ScreenDimensionHelper {

    /**
     * Retrieves the physical screen dimensions (real width and real height) in pixels.
     * Uses [DisplayManager] so it is 100% safe to call from non-UI contexts such as [android.app.Service]
     * on Android 11–16+ without triggering WindowManager UI-Context exceptions.
     */
    fun getRealDisplayMetrics(context: Context): Pair<Int, Int> {
        try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val defaultDisplay = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            if (defaultDisplay != null) {
                val realMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                defaultDisplay.getRealMetrics(realMetrics)
                if (realMetrics.widthPixels > 0 && realMetrics.heightPixels > 0) {
                    return Pair(realMetrics.widthPixels, realMetrics.heightPixels)
                }
            }
        } catch (_: Throwable) {}

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (wm != null) {
                val dm = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay?.getRealMetrics(dm)
                if (dm.widthPixels > 0 && dm.heightPixels > 0) {
                    return Pair(dm.widthPixels, dm.heightPixels)
                }
            }
        } catch (_: Throwable) {}

        val resMetrics = context.resources.displayMetrics
        val w = if (resMetrics.widthPixels > 0) resMetrics.widthPixels else 1080
        val h = if (resMetrics.heightPixels > 0) resMetrics.heightPixels else 2400
        return Pair(w, h)
    }

    /**
     * Calculates width and height that align with the physical screen's aspect ratio.
     * - For Native ("原生") or 2K ("2K") on <=1440p screens: uses exact 1:1 physical screen pixels
     *   (aligned only to 2-pixel even boundaries) to avoid any GPU bilinear resampling blur.
     * - Never upscales beyond the physical screen resolution.
     */
    fun calculateDimensions(
        context: Context,
        settings: ReplaySettings,
        isLandscape: Boolean,
        alignStep: Int = 2
    ): Pair<Int, Int> {
        val (realW, realH) = getRealDisplayMetrics(context)
        val maxScreen = maxOf(realW, realH).coerceAtLeast(1280)
        val minScreen = minOf(realW, realH).coerceAtLeast(720)
        val aspectRatio = maxScreen.toFloat() / minScreen.toFloat()

        val label = settings.resolutionLabel
        val targetMin = when {
            label.contains("720", ignoreCase = true) -> minOf(minScreen, 720)
            label.contains("1080", ignoreCase = true) -> minOf(minScreen, 1080)
            label.contains("2K", ignoreCase = true) -> minOf(minScreen, maxOf(minScreen, 1440).coerceAtMost(minScreen))
            label.contains("原生", ignoreCase = true) || label.contains("原画", ignoreCase = true) -> minScreen
            else -> minScreen
        }

        val step = alignStep.coerceAtLeast(2)
        val alignedMin: Int
        val alignedMax: Int

        if (targetMin >= minScreen && step <= 2) {
            // Exact 1:1 physical pixel mapping (even-aligned for YUV420) — zero scaling blur
            alignedMin = (minScreen / 2) * 2
            alignedMax = (maxScreen / 2) * 2
        } else {
            alignedMin = ((targetMin + step / 2) / step) * step
            val rawMax = (alignedMin * aspectRatio).roundToInt()
            alignedMax = ((rawMax + step / 2) / step) * step
        }

        return if (isLandscape) {
            Pair(alignedMax, alignedMin)
        } else {
            Pair(alignedMin, alignedMax)
        }
    }
}
