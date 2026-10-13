package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.GameReplayApplication
import com.example.MainActivity
import com.example.data.model.ReplaySettings

/**
 * Enhanced Gaming Floating Replay Overlay:
 * - Draggable with edge alignment.
 * - Dynamic customizable sizes (Small 36dp, Medium 48dp, Large 60dp).
 * - Semi-transparent with customizable opacity.
 * - Collapse/Hide mode into an ultra-slim edge indicator to prevent game blocking.
 * - Cyberpunk neon pulse animation during active buffering.
 * - Single tap: Instant clip save with haptic feedback.
 * - Long press: Quick HUD menu.
 */
class FloatingOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        if (Settings.canDrawOverlays(this)) {
            showFloatingOverlay()
        } else {
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 260
        }

        val app = application as? GameReplayApplication

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)

            setContent {
                val settings by (app?.repository?.settings?.collectAsState()
                    ?: remember { mutableStateOf(ReplaySettings()) })

                FloatingReplayOverlay(
                    settings = settings,
                    onUpdateSettings = { newSettings ->
                        app?.repository?.updateSettings(newSettings)
                    },
                    onDragDelta = { dx, dy ->
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        windowManager?.updateViewLayout(this, params)
                    },
                    onTriggerSave = {
                        ReplayRecorderService.triggerSaveClip(this@FloatingOverlayService)
                    },
                    onOpenApp = {
                        val appIntent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(appIntent)
                    },
                    onCloseOverlay = {
                        stopSelf()
                    }
                )
            }
        }

        overlayView = composeView
        windowManager?.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        overlayView?.let { view ->
            windowManager?.removeView(view)
        }
        overlayView = null
    }

    companion object {
        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, FloatingOverlayService::class.java)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }
}

@Composable
fun FloatingReplayOverlay(
    settings: ReplaySettings,
    onUpdateSettings: (ReplaySettings) -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onTriggerSave: () -> Unit,
    onOpenApp: () -> Unit,
    onCloseOverlay: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isHiddenCollapsed by remember { mutableStateOf(settings.isFloatingButtonCollapsed) }

    val serviceStatus by ReplayRecorderService.serviceStatus.collectAsState()
    val isSaving = serviceStatus.isSavingClip
    val isBuffering = serviceStatus.isBuffering
    val bufferedSec = serviceStatus.bufferedSeconds

    // Breathing neon pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Smooth animated size and alpha transitions
    val animatedButtonSizeDp by androidx.compose.animation.core.animateDpAsState(
        targetValue = settings.floatingButtonSizeDp.dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "button_size"
    )
    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = settings.floatingButtonOpacity.coerceIn(0.2f, 1.0f),
        animationSpec = tween(300),
        label = "button_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .animateContentSize()
    ) {
        if (isHiddenCollapsed) {
            // Ultra-compact edge strip handle when hidden
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 24.dp, height = 48.dp)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xCC00E5FF))
                    .border(1.dp, Color.White, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .clickable {
                        isHiddenCollapsed = false
                        onUpdateSettings(settings.copy(isFloatingButtonCollapsed = false))
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Expand Floating Button",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            // Main Floating Micro-Dot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(animatedButtonSizeDp)
                    .alpha(animatedAlpha)
                    .scale(if (isBuffering || isSaving) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isSaving) Color(0xFFFF1744) else Color(0xFF00E5FF),
                                Color(0xDD090C10)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSaving) Color(0xFFFF1744) else Color(0xFF00E5FF),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onTriggerSave()
                            },
                            onLongPress = {
                                isExpanded = !isExpanded
                            }
                        )
                    }
            ) {
                Icon(
                    imageVector = if (isSaving) Icons.Default.FiberManualRecord else Icons.Default.Bolt,
                    contentDescription = "Save Replay",
                    tint = Color.White,
                    modifier = Modifier.size((animatedButtonSizeDp.value * 0.55f).dp)
                )
            }

            // Quick HUD Menu when expanded
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + scaleIn(spring()),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF010141E))
                        .border(1.dp, Color(0xFF2B334D), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isBuffering) "BUFF: ${bufferedSec}s" else "待机",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "单击截取 • 音量键亦可",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Size toggle button (Cycles 36 -> 48 -> 60)
                            IconButton(
                                onClick = {
                                    val nextSize = when (settings.floatingButtonSizeDp) {
                                        36 -> 48
                                        48 -> 60
                                        else -> 36
                                    }
                                    onUpdateSettings(settings.copy(floatingButtonSizeDp = nextSize))
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoSizeSelectSmall,
                                    contentDescription = "Change Size",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Collapse / Hide to edge button
                            IconButton(
                                onClick = {
                                    isHiddenCollapsed = true
                                    isExpanded = false
                                    onUpdateSettings(settings.copy(isFloatingButtonCollapsed = true))
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Collapse to Edge",
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Open App
                            IconButton(
                                onClick = {
                                    onOpenApp()
                                    isExpanded = false
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open App",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Close Overlay
                            IconButton(
                                onClick = {
                                    onCloseOverlay()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
