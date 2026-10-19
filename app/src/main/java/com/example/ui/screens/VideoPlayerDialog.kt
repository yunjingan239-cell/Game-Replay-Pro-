package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.local.ReplayClip
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import com.example.util.MediaExportHelper
import com.example.util.VideoThumbnailView
import kotlinx.coroutines.delay
import java.io.File

/**
 * High-performance Esports Video Player Dialog.
 *
 * Architecture Improvements:
 * - Uses TextureView + MediaPlayer instead of SurfaceView/VideoView to eliminate
 *   the Compose Dialog black-screen bug (where only audio plays without video).
 * - Smooth frame-accurate seeking using MediaPlayer.SEEK_CLOSEST.
 * - Progress bar synchronization with drag state locks, preventing snapback glitches.
 * - Real-time scrubber preview with instant UI feedback.
 */
@Composable
fun VideoPlayerDialog(
    clip: ReplayClip,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val videoFile = remember(clip.filePath) { File(clip.filePath) }

    var isPlaying by remember { mutableStateOf(true) }
    var isPrepared by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf((clip.durationSeconds * 1000).coerceAtLeast(1000)) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableFloatStateOf(0f) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isSavedToGallery by remember { mutableStateOf(false) }

    // Dynamically retrieve real aspect ratio from video file
    var videoAspectRatio by remember {
        val initialRatio = try {
            if (videoFile.exists()) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoFile.absolutePath)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 16f
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 9f
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                retriever.release()
                if (rotation == 90 || rotation == 270) {
                    (h / w).coerceIn(0.5f, 2.5f)
                } else {
                    (w / h).coerceIn(0.5f, 2.5f)
                }
            } else 16f / 9f
        } catch (_: Exception) {
            16f / 9f
        }
        mutableFloatStateOf(initialRatio)
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }

    fun performSeek(targetMs: Int) {
        val mp = mediaPlayer ?: return
        val safeTarget = targetMs.coerceIn(0, totalDurationMs)
        currentPosMs = safeTarget
        isSeeking = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mp.seekTo(safeTarget.toLong(), MediaPlayer.SEEK_CLOSEST)
            } else {
                mp.seekTo(safeTarget)
            }
        } catch (_: Exception) {
            isSeeking = false
        }
    }

    // Initialize MediaPlayer lifecycle
    DisposableEffect(videoFile.absolutePath) {
        if (!videoFile.exists()) {
            onDispose {}
        } else {
            val mp = MediaPlayer().apply {
                try {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setVolume(1.0f, 1.0f)
                    setDataSource(videoFile.absolutePath)
                    isLooping = true
                    setOnPreparedListener { player ->
                        isPrepared = true
                        val duration = player.duration
                        if (duration in 1..1_800_000) {
                            totalDurationMs = duration
                        } else if (clip.durationSeconds in 1..1800) {
                            totalDurationMs = clip.durationSeconds * 1000
                        }
                        if (player.videoWidth > 0 && player.videoHeight > 0) {
                            videoAspectRatio = (player.videoWidth.toFloat() / player.videoHeight.toFloat()).coerceIn(0.5f, 2.5f)
                        }
                        surfaceRef?.let {
                            if (it.isValid) {
                                player.setSurface(it)
                            }
                        }
                        player.start()
                        isPlaying = true
                    }
                    setOnVideoSizeChangedListener { _, w, h ->
                        if (w > 0 && h > 0) {
                            videoAspectRatio = (w.toFloat() / h.toFloat()).coerceIn(0.5f, 2.5f)
                        }
                    }
                    setOnSeekCompleteListener {
                        isSeeking = false
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosMs = totalDurationMs
                    }
                    prepareAsync()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mediaPlayer = mp

            onDispose {
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.reset()
                    mp.release()
                } catch (_: Exception) {}
                surfaceRef?.release()
                surfaceRef = null
                mediaPlayer = null
            }
        }
    }

    // Periodic progress updater while playing
    LaunchedEffect(isPrepared, isPlaying, isDragging, isSeeking) {
        while (isPrepared && isPlaying && !isDragging && !isSeeking) {
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    if (mp.isPlaying) {
                        currentPosMs = mp.currentPosition
                        val dur = mp.duration
                        if (dur > 0) {
                            totalDurationMs = dur
                        }
                    }
                } catch (_: Exception) {}
            }
            delay(150)
        }
    }

    // Auto-hide controls after 4 seconds of uninterrupted playback
    LaunchedEffect(isControlsVisible, isPlaying, isDragging) {
        if (isControlsVisible && isPlaying && !isDragging) {
            delay(4000)
            isControlsVisible = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            color = PureWhite,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // 1. Header with Title and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clip.title,
                            color = TextTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val displayDurationSec = if (clip.durationSeconds in 1..1800) {
                            clip.durationSeconds
                        } else {
                            (totalDurationMs / 1000).coerceIn(1, 300)
                        }
                        Text(
                            text = "规格 ${clip.resolution} • ${displayDurationSec}秒",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Video Player Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(videoAspectRatio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF070B14))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isControlsVisible = !isControlsVisible
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder Backdrop before video frames arrive
                    if (!isPrepared) {
                        VideoThumbnailView(
                            clip = clip,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (videoFile.exists()) {
                        // High-performance hardware TextureView rendering
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                            val newSurface = Surface(st)
                                            surfaceRef = newSurface
                                            mediaPlayer?.setSurface(newSurface)
                                        }

                                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                            mediaPlayer?.setSurface(null)
                                            surfaceRef?.release()
                                            surfaceRef = null
                                            return true
                                        }

                                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                    }

                                    // If surface texture is already available on re-bind
                                    if (isAvailable && surfaceTexture != null) {
                                        val newSurface = Surface(surfaceTexture)
                                        surfaceRef = newSurface
                                        mediaPlayer?.setSurface(newSurface)
                                    }
                                }
                            },
                            modifier = Modifier.matchParentSize()
                        )

                        // Center Play/Pause Overlay Indicator
                        if (isControlsVisible) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // -5s Rewind
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .clickable {
                                            val newPos = (currentPosMs - 5000).coerceAtLeast(0)
                                            performSeek(newPos)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Rewind 5s",
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Play / Pause Button
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.9f))
                                        .clickable {
                                            val mp = mediaPlayer
                                            if (mp != null) {
                                                try {
                                                    if (mp.isPlaying) {
                                                        mp.pause()
                                                        isPlaying = false
                                                    } else {
                                                        mp.start()
                                                        isPlaying = true
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = PureWhite,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // +5s Fast Forward
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .clickable {
                                            val newPos = (currentPosMs + 5000).coerceAtMost(totalDurationMs)
                                            performSeek(newPos)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Forward 5s",
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Player HUD: Draggable Progress Bar & Timestamps
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.70f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Replay from start
                                IconButton(
                                    onClick = {
                                        performSeek(0)
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "Replay",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Interactive Draggable Slider with safe bounds
                                val currentVal = if (isDragging) dragProgressMs else currentPosMs.toFloat()
                                val safeVal = currentVal.coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f))

                                Slider(
                                    value = safeVal,
                                    onValueChange = { newValue ->
                                        isDragging = true
                                        dragProgressMs = newValue
                                        performSeek(newValue.toInt())
                                    },
                                    onValueChangeFinished = {
                                        val target = dragProgressMs.toInt()
                                        performSeek(target)
                                        isDragging = false
                                    },
                                    valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = PureWhite,
                                        activeTrackColor = PrimaryBlue,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                // Real-time timestamp: "00:15 / 00:30"
                                val displayPos = if (isDragging) dragProgressMs.toInt() else currentPosMs
                                Text(
                                    text = "${formatDurationMs(displayPos)} / ${formatDurationMs(totalDurationMs)}",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "视频文件未找到",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Action Buttons Row: Save to Gallery & Share Video
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action: Save to System Gallery (保存到系统相册)
                    Button(
                        onClick = {
                            if (videoFile.exists()) {
                                val result = MediaExportHelper.saveVideoToGallery(
                                    context = context,
                                    videoFile = videoFile,
                                    title = clip.title
                                )
                                if (result.isSuccess) {
                                    isSavedToGallery = true
                                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSavedToGallery) Color(0xFF10B981) else PrimaryBlue,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(
                            imageVector = if (isSavedToGallery) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = "Save to Gallery",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSavedToGallery) "已保存至相册" else "保存到相册",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Secondary Action: Share Video
                    OutlinedButton(
                        onClick = {
                            shareVideo(context, videoFile)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp), tint = TextTitle)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("分享回录", color = TextTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatDurationMs(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun shareVideo(context: Context, file: File) {
    if (!file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享游戏高光瞬间"))
    } catch (_: Exception) {}
}
