package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftGreenBorder
import com.example.ui.theme.SubtleSurface
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Minimalist card container following the 70% professional utility + 20% native OS + 10% light gaming spec:
 * - 18.dp rounded corners
 * - 1.dp #E8EDF3 fine border
 * - Zero heavy shadow clutter
 * - 18.dp internal padding
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderSubtle,
    glowEffect: Boolean = false,
    backgroundColor: Color = PureWhite,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (glowEffect) PrimaryBlue.copy(alpha = 0.32f) else borderColor
        )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * Status indicator dot with subtle breathing pulse when active.
 */
@Composable
fun PulsingRadarDot(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = SoftGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(14.dp)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.22f))
            )
        }
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else TextMuted.copy(alpha = 0.45f))
        )
    }
}

/**
 * Single clean status badge (green only for active running state, neutral/blue for standby).
 */
@Composable
fun StatusBadge(
    text: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) SoftGreenBg else SubtleSurface,
        border = BorderStroke(1.dp, if (isActive) SoftGreenBorder else BorderSubtle),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingRadarDot(isActive = isActive, activeColor = if (isActive) SoftGreen else TextMuted)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isActive) SoftGreen else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Compact Segmented Control for switching views or options (e.g., All Clips vs Favorites, Duration, Presets).
 */
@Composable
fun CompactSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "segment_opt"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SubtleSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) PureWhite else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    border = if (isSelected) BorderStroke(0.5.dp, BorderSubtle) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOptionSelected(index) }
                        .testTag("${testTagPrefix}_$index")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) PrimaryBlue else TextBody,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean user-friendly metric summary item.
 */
@Composable
fun StatChip(
    label: String,
    value: String,
    subValue: String? = null,
    modifier: Modifier = Modifier,
    highlightValue: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SubtleSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = if (highlightValue) PrimaryBlue else TextTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Section header with clean typography hierarchy.
 */
@Composable
fun CyberSectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = PrimaryBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Primary / Secondary standardized action button (54.dp height, 16.dp corner radius).
 */
@Composable
fun CyberActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "cyber_action_btn",
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .testTag(testTag)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) PrimaryBlue else PureWhite,
            contentColor = if (isPrimary) PureWhite else TextTitle,
            disabledContainerColor = Color(0xFFE8EDF3),
            disabledContentColor = TextMuted
        ),
        border = if (!isPrimary) BorderStroke(1.dp, BorderSubtle) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Quick Feature Card with unified #EAF3FF icon container to avoid multi-color visual clutter.
 */
@Composable
fun QuickFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    iconBgColor: Color = PrimaryBlueLight,
    iconTint: Color = PrimaryBlue
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, if (isHighlighted) PrimaryBlue.copy(alpha = 0.4f) else BorderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Interactive Floating Ball Simulator Overlay (Page 5 / Floating Ball Interactive Preview).
 * Supports:
 * 1. Semi-transparent edge-docked collapsed state (low interference during gaming)
 * 2. Expanded compact capsule with live buffer indicator, quick save action, and collapse button
 * 3. Vertical dragging along screen edge
 * 4. Saving state feedback ("已触发回录保存")
 */
@Composable
fun InteractiveFloatingBallOverlay(
    visible: Boolean,
    isBuffering: Boolean,
    bufferDurationSeconds: Int,
    bufferedSeconds: Int,
    opacity: Float,
    sizeDp: Int,
    onSaveClick: () -> Unit,
    onClosePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var isExpanded by remember { mutableStateOf(false) }
    var isDockedMinimum by remember { mutableStateOf(false) }
    var isJustSaved by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isJustSaved) {
        if (isJustSaved) {
            delay(1800)
            isJustSaved = false
            isExpanded = false
        }
    }

    val effectiveAlpha by animateFloatAsState(
        targetValue = when {
            isExpanded -> 0.98f
            isDockedMinimum -> (opacity * 0.65f).coerceIn(0.3f, 0.75f)
            else -> opacity.coerceIn(0.35f, 1.0f)
        },
        animationSpec = tween(220),
        label = "floating_ball_alpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .padding(end = if (isDockedMinimum && !isExpanded) 0.dp else 12.dp)
                .alpha(effectiveAlpha)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetY = (offsetY + dragAmount.y).coerceIn(-450f, 450f)
                    }
                }
        ) {
            // Expanded control capsule
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(200)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(180))
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF181F29).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Buffer time status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isBuffering) SoftGreen else PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isBuffering) "已缓存 ${bufferedSeconds}s" else "循环设定 ${bufferDurationSeconds}s",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBuffering) "实时循环中" else "悬浮窗预览模式",
                                    color = PureWhite.copy(alpha = 0.65f),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Save action pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isJustSaved) SoftGreen else PrimaryBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    isJustSaved = true
                                    onSaveClick()
                                }
                                .testTag("floating_preview_save_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isJustSaved) Icons.Default.Check else Icons.Default.Bolt,
                                    contentDescription = "Save",
                                    tint = PureWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isJustSaved) "已存高光" else "保存回录",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Dock to edge button
                        IconButton(
                            onClick = {
                                isExpanded = false
                                isDockedMinimum = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "贴边隐身",
                                tint = PureWhite.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Close preview button
                        IconButton(
                            onClick = onClosePreview,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭预览",
                                tint = PureWhite.copy(alpha = 0.55f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Main Floating Ball Orb or Edge Tab
            if (isDockedMinimum && !isExpanded) {
                // Edge-docked low-interference tab
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
                    color = Color(0xFF181F29).copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .height(sizeDp.dp)
                        .width(26.dp)
                        .clickable {
                            isDockedMinimum = false
                            isExpanded = true
                        }
                        .testTag("floating_preview_edge_tab")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "展开悬浮球",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Compact floating ball
                Surface(
                    shape = CircleShape,
                    color = if (isJustSaved) SoftGreen else Color(0xFF181F29).copy(alpha = 0.90f),
                    border = BorderStroke(1.5.dp, if (isJustSaved) SoftGreen else PrimaryBlue),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(sizeDp.dp.coerceIn(38.dp, 60.dp))
                        .clickable {
                            if (!isExpanded) {
                                isExpanded = true
                            } else {
                                isJustSaved = true
                                onSaveClick()
                            }
                        }
                        .testTag("floating_preview_orb")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isJustSaved) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Saved",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Floating Ball",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${if (isBuffering && bufferedSeconds > 0) bufferedSeconds else bufferDurationSeconds}s",
                                    color = PureWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
