package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.local.ReplayClip
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.util.MediaExportHelper
import com.example.util.VideoThumbnailView
import kotlinx.coroutines.delay
import java.io.File

private data class VideoDisplayMeta(
    val displayWidth: Int,
    val displayHeight: Int,
    val rotation: Int,
    val aspectRatio: Float
)

/**
 * Full-Window Cinema Video Player Overlay.
 *
 * Designed to maximize video display area while keeping all playback and export controls
 * minimalist and semi-transparent:
 * 1. Hosted directly in MainActivity's root window (instead of a separate Dialog PhoneWindow)
 *    so switching between portrait preview and fullscreen landscape updates the window bounds
 *    natively and seamlessly without any narrow-dialog clipping or aspect-ratio distortion.
 * 2. Initial Preview Mode: Uses 100% of available screen width for the video canvas with sleek,
 *    minimalist semi-transparent dark glass controls (Top bar with Title, Save, Share, Close;
 *    Bottom capsule with Play/Pause, Time, Progress Slider, Replay, Mute, Fullscreen).
 * 3. Fullscreen Mode: Rotates the Activity window to landscape for horizontal game videos,
 *    hides system bars, and fits the video strictly to its native aspect ratio.
 */
@Composable
fun VideoPlayerDialog(
    clip: ReplayClip,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
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
    var isFullscreen by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Extract initial video dimensions & rotation metadata
    val initialMeta = remember(videoFile.absolutePath, clip.resolution) {
        extractVideoDisplayMetadata(videoFile, clip.resolution)
    }
    val videoRotation by remember { mutableIntStateOf(initialMeta.rotation) }
    var videoWidthPx by remember { mutableIntStateOf(initialMeta.displayWidth) }
    var videoHeightPx by remember { mutableIntStateOf(initialMeta.displayHeight) }
    var videoAspectRatio by remember { mutableFloatStateOf(initialMeta.aspectRatio) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }

    // Handle Back button: exit fullscreen first, or dismiss overlay when in preview mode
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onDismiss()
        }
    }

    // Manage Activity screen orientation and system bars for Fullscreen vs Preview mode
    DisposableEffect(isFullscreen, videoAspectRatio) {
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else {
            null
        }

        if (isFullscreen) {
            isControlsVisible = true
            if (videoAspectRatio > 1.0f) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            insetsController?.let { controller ->
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {}
    }

    // Restore default orientation & system bars when player overlay is completely closed
    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
                isControlsVisible = true
            } else {
                mp.start()
                isPlaying = true
            }
        } catch (_: Exception) {}
    }

    fun toggleMute() {
        val mp = mediaPlayer ?: return
        isMuted = !isMuted
        try {
            if (isMuted) {
                mp.setVolume(0f, 0f)
            } else {
                mp.setVolume(1.0f, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun saveClipToGallery() {
        if (videoFile.exists()) {
            val result = MediaExportHelper.saveVideoToGallery(
                context = context,
                videoFile = videoFile,
                title = clip.title
            )
            if (result.isSuccess) {
                isSavedToGallery = true
                Toast.makeText(context, "已保存至系统相册", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateVideoDimensions(rawW: Int, rawH: Int) {
        if (rawW <= 0 || rawH <= 0) return
        val dispW = if (videoRotation == 90 || videoRotation == 270) rawH else rawW
        val dispH = if (videoRotation == 90 || videoRotation == 270) rawW else rawH
        videoWidthPx = dispW
        videoHeightPx = dispH
        videoAspectRatio = (dispW.toFloat() / dispH.toFloat()).coerceIn(0.35f, 3.0f)
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
                            updateVideoDimensions(player.videoWidth, player.videoHeight)
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
                            updateVideoDimensions(w, h)
                        }
                    }
                    setOnSeekCompleteListener {
                        isSeeking = false
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosMs = totalDurationMs
                        isControlsVisible = true
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
            delay(120)
        }
    }

    // Auto-hide minimalist semi-transparent controls after 3.2 seconds during active playback
    LaunchedEffect(isControlsVisible, isPlaying, isDragging, isFullscreen) {
        if (isControlsVisible && isPlaying && !isDragging) {
            delay(3200)
            isControlsVisible = false
        }
    }

    // Reset seek badge feedback after 600ms
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(600)
            seekFeedback = null
        }
    }

    val handleSurfaceReady: (Surface) -> Unit = { newSurface ->
        val oldSurface = surfaceRef
        surfaceRef = newSurface
        try {
            mediaPlayer?.setSurface(newSurface)
            if (isPrepared && !isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer?.seekTo(currentPosMs.toLong(), MediaPlayer.SEEK_CLOSEST)
                } else {
                    mediaPlayer?.seekTo(currentPosMs)
                }
            }
        } catch (_: Exception) {}
        if (oldSurface != null && oldSurface !== newSurface) {
            try {
                oldSurface.release()
            } catch (_: Exception) {}
        }
    }

    val displayDurationSec = if (clip.durationSeconds in 1..1800) {
        clip.durationSeconds
    } else {
        (totalDurationMs / 1000).coerceIn(1, 300)
    }

    // Root Full-Window Cinema Stage (no separate Dialog PhoneWindow)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else Color.Black.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isFullscreen) {
                    isControlsVisible = !isControlsVisible
                } else {
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Maximized Video Canvas (100% width, strict aspect-ratio fit without any distortion)
        val stageModifier = if (isFullscreen) {
            Modifier.fillMaxSize()
        } else if (videoAspectRatio < 1.0f) {
            // Portrait video in preview mode: fill maximum screen area
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        } else {
            // Landscape video in preview mode: span 100% full screen width with zero side padding
            Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio.coerceIn(1.0f, 2.5f))
        }

        Box(
            modifier = stageModifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isControlsVisible = !isControlsVisible
                },
            contentAlignment = Alignment.Center
        ) {
            VideoPlayerSurface(
                clip = clip,
                videoFile = videoFile,
                videoAspectRatio = videoAspectRatio,
                videoWidthPx = videoWidthPx,
                videoHeightPx = videoHeightPx,
                isPrepared = isPrepared,
                isPlaying = isPlaying,
                isControlsVisible = isControlsVisible,
                currentPosMs = currentPosMs,
                totalDurationMs = totalDurationMs,
                seekFeedback = seekFeedback,
                onSurfaceReady = handleSurfaceReady,
                onToggleControls = { isControlsVisible = !isControlsVisible },
                onTogglePlayPause = { togglePlayPause() },
                onSeek = { newPos -> performSeek(newPos) },
                onSeekFeedback = { isLeft, label -> seekFeedback = Pair(isLeft, label) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Minimalist Semi-Transparent Top HUD Overlay
        AnimatedVisibility(
            visible = isControlsVisible || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.58f),
                                Color.Black.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
                    .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back/Close + Title & Specs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isFullscreen) {
                        MinimalistCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "退出全屏",
                            onClick = { isFullscreen = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = clip.title,
                            color = PureWhite.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${clip.resolution} • ${displayDurationSec}秒",
                            color = PureWhite.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Minimalist Semi-Transparent Action Pills (Save, Share, Fullscreen, Close)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MinimalistGlassPillButton(
                        icon = if (isSavedToGallery) Icons.Default.Check else Icons.Default.Download,
                        label = if (isSavedToGallery) "已保存" else "保存",
                        accentColor = if (isSavedToGallery) Color(0xFF10B981).copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.38f),
                        onClick = { saveClipToGallery() }
                    )

                    MinimalistGlassPillButton(
                        icon = Icons.Default.Share,
                        label = "分享",
                        accentColor = Color.Black.copy(alpha = 0.38f),
                        onClick = { shareVideo(context, videoFile) }
                    )

                    MinimalistCircleIconButton(
                        icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "退出全屏" else "全屏播放",
                        onClick = { isFullscreen = !isFullscreen }
                    )

                    MinimalistCircleIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "关闭",
                        onClick = {
                            if (isFullscreen) {
                                isFullscreen = false
                            } else {
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }

        // 3. Minimalist Semi-Transparent Bottom Playback Capsule Overlay
        AnimatedVisibility(
            visible = isControlsVisible || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val displayPos = if (isDragging) dragProgressMs.toInt() else currentPosMs
            val currentSliderVal = if (isDragging) dragProgressMs else currentPosMs.toFloat()
            val maxSliderVal = totalDurationMs.toFloat().coerceAtLeast(1f)
            val safeSliderVal = currentSliderVal.coerceIn(0f, maxSliderVal)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play / Pause
                    IconButton(
                        onClick = { togglePlayPause() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = PureWhite,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Current / Total Time
                    Text(
                        text = "${formatDurationMs(displayPos)}/${formatDurationMs(totalDurationMs)}",
                        color = PureWhite.copy(alpha = 0.88f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )

                    // Slim Progress Slider
                    Slider(
                        value = safeSliderVal,
                        onValueChange = { newValue ->
                            isDragging = true
                            dragProgressMs = newValue
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            performSeek(dragProgressMs.toInt())
                        },
                        valueRange = 0f..maxSliderVal,
                        colors = SliderDefaults.colors(
                            thumbColor = PureWhite,
                            activeTrackColor = PrimaryBlue.copy(alpha = 0.9f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    )

                    // Replay from 00:00
                    IconButton(
                        onClick = { performSeek(0) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "重播",
                            tint = PureWhite.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Mute / Unmute
                    IconButton(
                        onClick = { toggleMute() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "静音切换",
                            tint = if (isMuted) Color(0xFFFF6B6B) else PureWhite.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Fullscreen / Expand Toggle
                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "退出全屏" else "全屏放大",
                            tint = PureWhite,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalistGlassPillButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PureWhite.copy(alpha = 0.92f),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            color = PureWhite.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MinimalistCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PureWhite.copy(alpha = 0.92f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Reusable, High-Fidelity Video Surface with Strict Aspect-Ratio Fitting and Post-Layout Matrix Sync.
 */
@Composable
private fun VideoPlayerSurface(
    clip: ReplayClip,
    videoFile: File,
    videoAspectRatio: Float,
    videoWidthPx: Int,
    videoHeightPx: Int,
    isPrepared: Boolean,
    isPlaying: Boolean,
    isControlsVisible: Boolean,
    currentPosMs: Int,
    totalDurationMs: Int,
    seekFeedback: Pair<Boolean, String>?,
    onSurfaceReady: (Surface) -> Unit,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSeekFeedback: (Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeAspectRatio = videoAspectRatio.coerceIn(0.35f, 3.0f)

    Box(
        modifier = modifier
            .pointerInput(totalDurationMs) {
                detectTapGestures(
                    onTap = {
                        onToggleControls()
                    },
                    onDoubleTap = { offset ->
                        val isLeft = offset.x < size.width / 2
                        val delta = if (isLeft) -5000 else 5000
                        val target = (currentPosMs + delta).coerceIn(0, totalDurationMs)
                        onSeek(target)
                        onSeekFeedback(isLeft, if (isLeft) "-5s" else "+5s")
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (videoFile.exists()) {
            // Fit the video canvas strictly to videoAspectRatio inside the available container
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val containerRatio = if (maxHeight > 0.dp) maxWidth / maxHeight else safeAspectRatio
                val fittedVideoModifier = if (containerRatio > safeAspectRatio) {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(safeAspectRatio)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(safeAspectRatio)
                }

                Box(
                    modifier = fittedVideoModifier,
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Placeholder before first frame renders
                    if (!isPrepared) {
                        VideoThumbnailView(
                            clip = clip,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 2. Hardware TextureView Renderer with Post-Layout Center-Fit Matrix Protection
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                tag = Pair(videoWidthPx, videoHeightPx)
                                addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
                                    val w = right - left
                                    val h = bottom - top
                                    val dims = (view.tag as? Pair<*, *>)
                                    val vw = (dims?.first as? Int) ?: videoWidthPx
                                    val vh = (dims?.second as? Int) ?: videoHeightPx
                                    applyCenterFitMatrix(this, w, h, vw, vh)
                                }
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                        val dims = (tag as? Pair<*, *>)
                                        val vw = (dims?.first as? Int) ?: videoWidthPx
                                        val vh = (dims?.second as? Int) ?: videoHeightPx
                                        applyCenterFitMatrix(this@apply, width, height, vw, vh)
                                        onSurfaceReady(Surface(st))
                                    }

                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                                        val dims = (tag as? Pair<*, *>)
                                        val vw = (dims?.first as? Int) ?: videoWidthPx
                                        val vh = (dims?.second as? Int) ?: videoHeightPx
                                        applyCenterFitMatrix(this@apply, width, height, vw, vh)
                                    }

                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true

                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                                if (isAvailable && surfaceTexture != null) {
                                    applyCenterFitMatrix(this, width, height, videoWidthPx, videoHeightPx)
                                    onSurfaceReady(Surface(surfaceTexture!!))
                                }
                            }
                        },
                        update = { textureView ->
                            textureView.tag = Pair(videoWidthPx, videoHeightPx)
                            textureView.post {
                                applyCenterFitMatrix(
                                    textureView = textureView,
                                    viewWidth = textureView.width,
                                    viewHeight = textureView.height,
                                    videoWidth = videoWidthPx,
                                    videoHeight = videoHeightPx
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Minimalist Semi-Transparent Center Play/Pause Button
            AnimatedVisibility(
                visible = isControlsVisible || !isPlaying,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.36f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = PureWhite.copy(alpha = 0.92f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 4. Ultra-thin 2dp bottom progress line when controls auto-hide
            if (!isControlsVisible && isPlaying) {
                val progressFraction = (currentPosMs.toFloat() / totalDurationMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progressFraction)
                        .height(2.dp)
                        .background(PrimaryBlue.copy(alpha = 0.75f))
                )
            }

            // 5. Double-tap seek feedback badge (-5s / +5s)
            seekFeedback?.let { (isLeft, label) ->
                Box(
                    modifier = Modifier
                        .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLeft) Icons.Default.FastRewind else Icons.Default.FastForward,
                            contentDescription = label,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
}

/**
 * Applies a hardware center-fit Matrix transform to TextureView using post-layout bounds
 * so video frames never stretch or distort during orientation transitions.
 */
private fun applyCenterFitMatrix(
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
) {
    if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

    val scaleX: Float
    val scaleY: Float
    if (videoRatio > viewRatio) {
        scaleX = 1f
        scaleY = viewRatio / videoRatio
    } else {
        scaleX = videoRatio / viewRatio
        scaleY = 1f
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

private fun extractVideoDisplayMetadata(videoFile: File, fallbackResolution: String): VideoDisplayMeta {
    val fallback = parseResolutionDimensions(fallbackResolution)
    if (!videoFile.exists()) {
        val ratio = (fallback.first.toFloat() / fallback.second.toFloat()).coerceIn(0.35f, 3.0f)
        return VideoDisplayMeta(fallback.first, fallback.second, 0, ratio)
    }

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(videoFile.absolutePath)
        val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            ?: fallback.first
        val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            ?: fallback.second
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
            ?: 0

        val dispW = if (rotation == 90 || rotation == 270) rawH else rawW
        val dispH = if (rotation == 90 || rotation == 270) rawW else rawH
        val ratio = (dispW.toFloat() / dispH.toFloat().coerceAtLeast(1f)).coerceIn(0.35f, 3.0f)
        VideoDisplayMeta(dispW, dispH, rotation, ratio)
    } catch (_: Exception) {
        val ratio = (fallback.first.toFloat() / fallback.second.toFloat()).coerceIn(0.35f, 3.0f)
        VideoDisplayMeta(fallback.first, fallback.second, 0, ratio)
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {}
    }
}

private fun parseResolutionDimensions(resolution: String): Pair<Int, Int> {
    val parts = resolution.lowercase().split("x")
    if (parts.size == 2) {
        val w = parts[0].trim().toIntOrNull()
        val h = parts[1].trim().toIntOrNull()
        if (w != null && h != null && w > 0 && h > 0) {
            return Pair(w, h)
        }
    }
    return Pair(1920, 1080)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
