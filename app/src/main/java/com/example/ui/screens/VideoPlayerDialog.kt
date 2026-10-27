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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

private data class VideoDisplayMeta(
    val displayWidth: Int,
    val displayHeight: Int,
    val rotation: Int,
    val aspectRatio: Float
)

/**
 * High-performance Esports Video Player Dialog.
 *
 * UX/UI Optimization:
 * - Solves the critical issue where playback controls heavily obscure gameplay details.
 * - Auto-hides all controls (center buttons, bottom HUD, gradient scrims) after 2.5s of playback.
 * - While playing without controls, presents a 100% unobstructed view with only an ultra-thin 2dp
 *   progress line hugging the bottom edge.
 * - Single-tap toggles controls visibility.
 * - Double-tap left/right seeks -5s/+5s with subtle, non-intrusive gesture feedback badges,
 *   completely replacing bulky permanent rewind/forward buttons in the center.
 * - Center play/pause button is refined and only displayed when paused or on-demand.
 * - Supports seamless fullscreen immersion mode with automatic landscape switching and strict
 *   aspect-ratio preservation (zero stretching or distortion).
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
    var isControlsVisible by remember { mutableStateOf(false) }
    var isSavedToGallery by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // Pair(isLeft, label)

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

    // Automatically switch Activity orientation to landscape when entering fullscreen for landscape videos
    DisposableEffect(isFullscreen, videoAspectRatio) {
        if (isFullscreen) {
            if (videoAspectRatio > 1.0f) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            if (!isFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Ensure orientation is always restored when dialog is completely dismissed
    DisposableEffect(Unit) {
        onDispose {
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
                isControlsVisible = true // Always show controls when paused
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
            delay(150)
        }
    }

    // Auto-hide controls after 2.5 seconds of playing with zero interaction
    LaunchedEffect(isControlsVisible, isPlaying, isDragging) {
        if (isControlsVisible && isPlaying && !isDragging) {
            delay(2500)
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

    if (isFullscreen) {
        BackHandler { isFullscreen = false }
    }

    Dialog(
        onDismissRequest = {
            if (isFullscreen) {
                isFullscreen = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = !isFullscreen,
            dismissOnBackPress = true,
            dismissOnClickOutside = !isFullscreen
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow, isFullscreen) {
            dialogWindow?.let { window ->
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (isFullscreen) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
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

        if (isFullscreen) {
            // Fullscreen Immersion Mode: Deep Cinema View with automatic landscape adaptation
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // If the video is landscape (> 1.0f) but the host window is still in portrait (maxWidth < maxHeight),
                // rotate the fullscreen canvas 90 degrees and swap layout width/height constraints so it always plays in landscape.
                val shouldRotateCanvasToLandscape = videoAspectRatio > 1.0f && maxWidth < maxHeight

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotateFullscreenLandscapeIfNeeded(shouldRotateCanvasToLandscape)
                        .background(Color.Black),
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
                        isDragging = isDragging,
                        isControlsVisible = isControlsVisible,
                        currentPosMs = currentPosMs,
                        totalDurationMs = totalDurationMs,
                        dragProgressMs = dragProgressMs,
                        seekFeedback = seekFeedback,
                        isMuted = isMuted,
                        isFullscreen = true,
                        onSurfaceReady = handleSurfaceReady,
                        onToggleControls = { isControlsVisible = !isControlsVisible },
                        onTogglePlayPause = { togglePlayPause() },
                        onToggleMute = { toggleMute() },
                        onToggleFullscreen = { isFullscreen = false },
                        onSeek = { newPos -> performSeek(newPos) },
                        onSeekFeedback = { isLeft, label -> seekFeedback = Pair(isLeft, label) },
                        onDragChange = { isDrag, pos ->
                            isDragging = isDrag
                            dragProgressMs = pos
                            if (!isDrag) performSeek(pos.toInt())
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Bar in Fullscreen
                    AnimatedVisibility(
                        visible = isControlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isFullscreen = false }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Exit Fullscreen",
                                        tint = PureWhite
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = clip.title,
                                        color = PureWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${clip.resolution} • ${(totalDurationMs / 1000).coerceAtLeast(1)}秒",
                                        color = PureWhite.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(onClick = { toggleMute() }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Dialog Card Mode (optimized for non-obstruction)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
                color = PureWhite,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 1. Header with Title and Control Buttons
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Fullscreen Expansion Button
                            IconButton(onClick = { isFullscreen = true }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Fullscreen",
                                    tint = PrimaryBlue
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
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Video Player Frame (Unobstructed View)
                    val cardVideoModifier = if (videoAspectRatio >= 1.0f) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoAspectRatio.coerceIn(1.0f, 3.0f))
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .aspectRatio(videoAspectRatio.coerceIn(0.35f, 1.0f))
                    }

                    Box(
                        modifier = cardVideoModifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF070B14)),
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
                            isDragging = isDragging,
                            isControlsVisible = isControlsVisible,
                            currentPosMs = currentPosMs,
                            totalDurationMs = totalDurationMs,
                            dragProgressMs = dragProgressMs,
                            seekFeedback = seekFeedback,
                            isMuted = isMuted,
                            isFullscreen = false,
                            onSurfaceReady = handleSurfaceReady,
                            onToggleControls = { isControlsVisible = !isControlsVisible },
                            onTogglePlayPause = { togglePlayPause() },
                            onToggleMute = { toggleMute() },
                            onToggleFullscreen = { isFullscreen = true },
                            onSeek = { newPos -> performSeek(newPos) },
                            onSeekFeedback = { isLeft, label -> seekFeedback = Pair(isLeft, label) },
                            onDragChange = { isDrag, pos ->
                                isDragging = isDrag
                                dragProgressMs = pos
                                if (!isDrag) performSeek(pos.toInt())
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Action Buttons: Save to Gallery & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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

                        OutlinedButton(
                            onClick = { shareVideo(context, videoFile) },
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
}

/**
 * Reusable, High-Fidelity Video Surface with Unobtrusive Modern Overlay and Strict Aspect-Ratio Fitting.
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
    isDragging: Boolean,
    isControlsVisible: Boolean,
    currentPosMs: Int,
    totalDurationMs: Int,
    dragProgressMs: Float,
    seekFeedback: Pair<Boolean, String>?,
    isMuted: Boolean,
    isFullscreen: Boolean,
    onSurfaceReady: (Surface) -> Unit,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSeek: (Int) -> Unit,
    onSeekFeedback: (Boolean, String) -> Unit,
    onDragChange: (Boolean, Float) -> Unit,
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
                        // Double-tap left half seeks -5s, right half seeks +5s
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

                    // 2. Hardware TextureView Renderer with Center-Fit Matrix Protection
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                        applyCenterFitMatrix(this@apply, width, height, videoWidthPx, videoHeightPx)
                                        onSurfaceReady(Surface(st))
                                    }

                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                                        applyCenterFitMatrix(this@apply, width, height, videoWidthPx, videoHeightPx)
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
                            applyCenterFitMatrix(
                                textureView = textureView,
                                viewWidth = textureView.width,
                                viewHeight = textureView.height,
                                videoWidth = videoWidthPx,
                                videoHeight = videoHeightPx
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Center Play/Pause indicator - ONLY shown when paused or when controls are active
            AnimatedVisibility(
                visible = isControlsVisible || !isPlaying,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = PureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // 4. Subtle Top Overlay in non-fullscreen (Mute quick toggle)
            if (!isFullscreen) {
                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onToggleMute() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 5. Sleek Bottom Control Bar (Semi-transparent gradient scrim, auto-fades out)
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.70f)
                                )
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Replay from beginning
                        IconButton(
                            onClick = { onSeek(0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Replay",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Compact Timestamp (00:01 / 00:25)
                        val displayPos = if (isDragging) dragProgressMs.toInt() else currentPosMs
                        Text(
                            text = "${formatDurationMs(displayPos)} / ${formatDurationMs(totalDurationMs)}",
                            color = PureWhite.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )

                        // Ultra-slim interactive timeline slider
                        val currentVal = if (isDragging) dragProgressMs else currentPosMs.toFloat()
                        val safeVal = currentVal.coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f))

                        Slider(
                            value = safeVal,
                            onValueChange = { newValue ->
                                onDragChange(true, newValue)
                            },
                            onValueChangeFinished = {
                                onDragChange(false, dragProgressMs)
                            },
                            valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = PureWhite,
                                activeTrackColor = PrimaryBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Fullscreen toggle button
                        IconButton(
                            onClick = { onToggleFullscreen() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 6. Ultra-thin 2dp bottom edge line when controls are hidden (Zero screen blockage)
            if (!isControlsVisible) {
                val progressFraction = (currentPosMs.toFloat() / totalDurationMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progressFraction)
                        .height(2.dp)
                        .background(PrimaryBlue.copy(alpha = 0.85f))
                )
            }

            // 7. Double-tap seek feedback badge (-5s / +5s)
            seekFeedback?.let { (isLeft, label) ->
                Box(
                    modifier = Modifier
                        .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
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
 * Rotates a fullscreen container 90 degrees clockwise and swaps width/height layout constraints
 * when playing a landscape video inside a portrait window (e.g. when device auto-rotate is locked
 * or the Dialog window remains in portrait orientation).
 */
private fun Modifier.rotateFullscreenLandscapeIfNeeded(rotate: Boolean): Modifier =
    if (!rotate) {
        this
    } else {
        this
            .graphicsLayer {
                rotationZ = 90f
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        x = (constraints.maxWidth - placeable.width) / 2,
                        y = (constraints.maxHeight - placeable.height) / 2
                    )
                }
            }
    }

/**
 * Applies a hardware center-fit Matrix transform to TextureView so video frames never stretch or distort
 * even during intermediate layout/orientation transitions.
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
