package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle

/**
 * Modern floating white card with soft corners, light border, and comfortable internal spacing.
 * Inspired by mobile cloud storage applications and modern system utilities.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderSubtle,
    glowEffect: Boolean = false,
    backgroundColor: Color = PureWhite,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (glowEffect) PrimaryBlue.copy(alpha = 0.35f) else borderColor)
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

/**
 * Status indicator dot with a gentle breathing pulse ring.
 */
@Composable
fun PulsingRadarDot(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = SoftGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(16.dp)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.25f))
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else TextMuted.copy(alpha = 0.4f))
        )
    }
}

/**
 * Status pill badge (e.g. "● 正在运行" / "● 2台设备在线" style in reference).
 */
@Composable
fun StatusBadge(
    text: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) SoftGreenBg else PrimaryBlueLight,
        border = BorderStroke(1.dp, if (isActive) SoftGreenBorder else BorderSubtle),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingRadarDot(isActive = isActive, activeColor = if (isActive) SoftGreen else PrimaryBlue)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isActive) Color(0xFF237804) else PrimaryBlueDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Clean user-friendly metric chip (e.g. "当前画质: 1080P 60FPS").
 */
@Composable
fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryBlue
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF7F9FC),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Clean section title with subtle accent bar.
 */
@Composable
fun CyberSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(16.dp)
                    .background(PrimaryBlue, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TextTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 11.5.dp, top = 3.dp)
            )
        }
    }
}

/**
 * Large primary action button (Large rounded rectangle, blue background, white text).
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
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) PrimaryBlue else PureWhite,
            contentColor = if (isPrimary) PureWhite else TextTitle,
            disabledContainerColor = Color(0xFFE4E7ED),
            disabledContentColor = TextMuted
        ),
        border = if (!isPrimary) BorderStroke(1.dp, BorderSubtle) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isPrimary) 2.dp else 0.dp),
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Quick action card with pastel rounded icon container (5-icon grid style from reference image).
 */
@Composable
fun QuickActionIconItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBgColor: Color = PrimaryBlueLight,
    iconTint: Color = PrimaryBlue
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = TextTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Quick Feature Card (White background, 20dp rounded corners, soft shadow/border, simple icon, and short description).
 * Matches the cloud app feature cards grid requested in prompt.
 */
@Composable
fun QuickFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBgColor: Color = PrimaryBlueLight,
    iconTint: Color = PrimaryBlue
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = TextTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
