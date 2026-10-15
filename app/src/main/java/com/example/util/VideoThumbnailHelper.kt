package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.ReplayClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * High-performance video thumbnail loader and generator with 2-tier caching (Memory + Disk).
 * Guarantees that video cards in ReplayLibrary always render vibrant, authentic game thumbnails
 * rather than black or blank screens.
 */
object VideoThumbnailHelper {
    private const val TAG = "VideoThumbnailHelper"

    // 1st Level: In-memory LRU Cache (Max 30 decoded bitmaps, ~15-20MB)
    private val memoryCache = object : LruCache<String, Bitmap>(30) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    /**
     * Loads a video thumbnail asynchronously.
     * 1. Checks memory cache.
     * 2. Checks disk thumbnail cache.
     * 3. Uses MediaMetadataRetriever to extract authentic frame.
     * 4. If frame is unavailable or all-black, synthesizes a vibrant esports gaming poster thumbnail.
     */
    suspend fun getThumbnail(context: Context, clip: ReplayClip): Bitmap = withContext(Dispatchers.IO) {
        val cacheKey = "${clip.id}_${clip.filePath.hashCode()}"
        
        // 1. Memory Cache
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // 2. Disk Cache
        val diskCacheDir = File(context.cacheDir, "replay_thumbnails").apply { mkdirs() }
        val diskCacheFile = File(diskCacheDir, "thumb_${clip.id}.jpg")
        if (diskCacheFile.exists() && diskCacheFile.length() > 0) {
            try {
                val diskBitmap = BitmapFactory.decodeFile(diskCacheFile.absolutePath)
                if (diskBitmap != null && !isBitmapCompletelyBlack(diskBitmap)) {
                    memoryCache.put(cacheKey, diskBitmap)
                    return@withContext diskBitmap
                }
            } catch (e: Exception) {
                Log.w(TAG, "Disk thumbnail decode failed: ${e.message}")
            }
        }

        // 3. Extract actual video frame using MediaMetadataRetriever
        var extractedBitmap: Bitmap? = null
        val videoFile = File(clip.filePath)
        if (videoFile.exists() && videoFile.length() > 0) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(clip.filePath)
                // Try 1s, then 500ms, then first sync frame
                extractedBitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime

                // Verify the extracted frame isn't pure black
                if (extractedBitmap != null && isBitmapCompletelyBlack(extractedBitmap)) {
                    // Try another position later in the video
                    val laterFrame = retriever.getFrameAtTime(2_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (laterFrame != null && !isBitmapCompletelyBlack(laterFrame)) {
                        extractedBitmap = laterFrame
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaMetadataRetriever failed for ${clip.filePath}: ${e.message}")
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }

        // 4. Fallback: generate vibrant esports game graphic thumbnail if extractedBitmap is null or too dark
        val finalBitmap = if (extractedBitmap != null && !isBitmapCompletelyBlack(extractedBitmap)) {
            // Scale down to crisp display size for low memory usage
            scaleBitmap(extractedBitmap, 640, 360)
        } else {
            generateStylizedGamePoster(clip)
        }

        // Save to disk cache & memory cache
        try {
            FileOutputStream(diskCacheFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write disk cache: ${e.message}")
        }
        memoryCache.put(cacheKey, finalBitmap)

        return@withContext finalBitmap
    }

    /**
     * Checks if a decoded bitmap is pitch black or near-black.
     */
    private fun isBitmapCompletelyBlack(bitmap: Bitmap): Boolean {
        try {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return true

            // Sample 16 points across the frame
            var maxLuma = 0
            for (stepX in 1..4) {
                for (stepY in 1..4) {
                    val pixel = bitmap.getPixel((w * stepX) / 5, (h * stepY) / 5)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val luma = (r * 299 + g * 587 + b * 114) / 1000
                    if (luma > maxLuma) maxLuma = luma
                }
            }
            return maxLuma < 12 // Less than 12/255 is virtually invisible black
        } catch (_: Exception) {
            return false
        }
    }

    private fun scaleBitmap(src: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxWidth && height <= maxHeight) return src

        val ratio = (maxWidth.toFloat() / width).coerceAtMost(maxHeight.toFloat() / height)
        val dstWidth = (width * ratio).toInt().coerceAtLeast(1)
        val dstHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true)
    }

    /**
     * Generates a high-resolution, vivid esports replay thumbnail artwork with cyberpunk gradients,
     * HUD telemetry grid, and the game name.
     */
    private fun generateStylizedGamePoster(clip: ReplayClip): Bitmap {
        val width = 640
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Palette selector based on game title
        val (startColor, midColor, endColor) = when {
            clip.gameName.contains("王者") -> Triple(0xFF1A1F3D.toInt(), 0xFF1E3A8A.toInt(), 0xFFD97706.toInt()) // Deep Blue & Gold
            clip.gameName.contains("和平") || clip.gameName.contains("吃鸡") -> Triple(0xFF14241B.toInt(), 0xFF065F46.toInt(), 0xFF10B981.toInt()) // Military Tactical Emerald
            clip.gameName.contains("原神") -> Triple(0xFF1E1B4B.toInt(), 0xFF4338CA.toInt(), 0xFF06B6D4.toInt()) // Celestia Cyan & Purple
            clip.gameName.contains("星穹") || clip.gameName.contains("崩坏") -> Triple(0xFF2E1065.toInt(), 0xFF6D28D9.toInt(), 0xFFEC4899.toInt()) // Astral Magenta & Violet
            clip.gameName.contains("LOL") || clip.gameName.contains("英雄联盟") -> Triple(0xFF0F172A.toInt(), 0xFF0284C7.toInt(), 0xFFF59E0B.toInt()) // Hextech Cyan & Gold
            else -> Triple(0xFF0A0F1D.toInt(), 0xFF1E40AF.toInt(), 0xFF7C3AED.toInt()) // Electric Blue & Cyber Violet
        }

        // 1. Diagonal Gradient Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(startColor, midColor, endColor),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. High-Tech Grid & Scanline Pattern
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22FFFFFF
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val gridSpacing = 40f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSpacing
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSpacing
        }

        // 3. Futuristic HUD Waveform / Spectrum Line in center
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x6638BDF8
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val wavePath = Path()
        wavePath.moveTo(40f, height * 0.72f)
        for (i in 0..20) {
            val px = 40f + (width - 80f) * (i / 20f)
            val py = (height * 0.72f) + (if (i % 2 == 0) -1 else 1) * ((abs(clip.id * 17 + i * 31) % 24).toFloat())
            wavePath.lineTo(px, py)
        }
        canvas.drawPath(wavePath, wavePaint)

        // 4. Central Circular Reticle Target / Glow
        val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(width / 2f, height / 2f, 70f, reticlePaint)
        canvas.drawCircle(width / 2f, height / 2f, 35f, reticlePaint)

        // 5. High-contrast Esports Badge Text (Game Title)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 34f
            isFakeBoldText = true
            setShadowLayer(10f, 0f, 4f, 0xAA000000.toInt())
        }
        canvas.drawText(clip.gameName, 44f, 75f, titlePaint)

        // Subtitle & Telemetry
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCBAE6FD.toInt()
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText("⚡ GAME REPLAY PRO • ${clip.resolution} • ${clip.fps} FPS", 44f, 110f, subPaint)

        return bitmap
    }
}

/**
 * Modern Jetpack Compose thumbnail view that displays video frame imagery
 * with smooth crossfade and readable dark scrim overlay.
 */
@Composable
fun VideoThumbnailView(
    clip: ReplayClip,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(clip.id, clip.filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(clip.id, clip.filePath) {
        val bitmap = VideoThumbnailHelper.getThumbnail(context, clip)
        thumbnailBitmap = bitmap
    }

    Box(modifier = modifier) {
        Crossfade(
            targetState = thumbnailBitmap,
            animationSpec = tween(300),
            label = "thumb_crossfade"
        ) { bmp ->
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = clip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Sleek placeholder gradient while loading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                )
            }
        }

        // Dark gradient scrim overlay so white text & play icon remain sharp and legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
    }
}
