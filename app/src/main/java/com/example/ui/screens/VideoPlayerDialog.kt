package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.view.ViewGroup
import android.widget.Toast
import android.widget.VideoView
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
import kotlinx.coroutines.delay
import java.io.File
import java.lang.ref.WeakReference

/**
 * High-performance Esports Video Player Dialog.
 * Features:
 * - Native aspect-ratio playback (no distortion or awkward cropping).
 * - Draggable real-time progress slider with seek preview.
 * - Direct "Save to System Gallery" button (MediaStore integration).
 * - Fast-forward/rewind +/-5s, play/pause, and looping controls.
 */
@Composable
fun VideoPlayerDialog(
    clip: ReplayClip,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val videoFile = remember(clip.filePath) { File(clip.filePath) }

    var isPlaying by remember { mutableStateOf(true) }
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
                retriever.release()
                (w / h).coerceIn(0.5f, 2.5f)
            } else 16f / 9f
        } catch (_: Exception) {
            16f / 9f
        }
        mutableFloatStateOf(initialRatio)
    }

    var videoViewRef by remember { mutableStateOf<WeakReference<VideoView>?>(null) }

    // Periodic progress updater while playing
    LaunchedEffect(isPlaying, isDragging) {
        while (isPlaying && !isDragging) {
            val vView = videoViewRef?.get()
            if (vView != null && vView.isPlaying) {
                currentPosMs = vView.currentPosition
                if (vView.duration > 0) {
                    totalDurationMs = vView.duration
                }
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

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.get()?.stopPlayback()
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
                        Text(
                            text = "${clip.gameName} • 规格 ${clip.resolution} • ${clip.durationSeconds}秒",
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
                    if (videoFile.exists()) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setVideoPath(videoFile.absolutePath)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        totalDurationMs = mp.duration.coerceAtLeast(1000)
                                        if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                                            videoAspectRatio = (mp.videoWidth.toFloat() / mp.videoHeight.toFloat()).coerceIn(0.5f, 2.5f)
                                        }
                                        start()
                                        isPlaying = true
                                    }
                                    setOnCompletionListener {
                                        isPlaying = false
                                        currentPosMs = totalDurationMs
                                    }
                                    videoViewRef = WeakReference(this)
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
                                            videoViewRef?.get()?.seekTo(newPos)
                                            currentPosMs = newPos
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
                                            val vView = videoViewRef?.get()
                                            if (vView != null) {
                                                if (vView.isPlaying) {
                                                    vView.pause()
                                                    isPlaying = false
                                                } else {
                                                    vView.start()
                                                    isPlaying = true
                                                }
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
                                            videoViewRef?.get()?.seekTo(newPos)
                                            currentPosMs = newPos
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
                                .background(Color.Black.copy(alpha = 0.65f))
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
                                        videoViewRef?.get()?.seekTo(0)
                                        videoViewRef?.get()?.start()
                                        currentPosMs = 0
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

                                // Interactive Draggable Slider
                                Slider(
                                    value = if (isDragging) dragProgressMs else currentPosMs.toFloat(),
                                    onValueChange = {
                                        isDragging = true
                                        dragProgressMs = it
                                    },
                                    onValueChangeFinished = {
                                        isDragging = false
                                        val targetPos = dragProgressMs.toInt()
                                        videoViewRef?.get()?.seekTo(targetPos)
                                        currentPosMs = targetPos
                                    },
                                    valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = PureWhite,
                                        activeTrackColor = PrimaryBlue,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
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
                                    Toast.makeText(context, "✅ 已成功保存到系统相册 (Movies/GameReplay)", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "保存失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
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
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
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
