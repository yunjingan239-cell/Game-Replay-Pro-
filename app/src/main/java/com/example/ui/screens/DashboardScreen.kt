package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ReplayClip
import com.example.data.model.ReplaySettings
import com.example.engine.InstalledGame
import com.example.ui.components.CyberCard
import com.example.ui.components.PulsingRadarDot
import com.example.ui.components.QuickActionIconItem
import com.example.ui.components.QuickFeatureCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppBackgroundGradientTop
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeBg
import com.example.ui.theme.SoftPurple
import com.example.ui.theme.SoftPurpleBg
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    settings: ReplaySettings,
    recentClips: List<ReplayClip> = emptyList(),
    onStartReplay: (Int, Intent?) -> Unit,
    onStopReplay: () -> Unit,
    onTriggerSave: (String?) -> Unit,
    onToggleFloatingOverlay: (Boolean) -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onSelectGame: (String) -> Unit,
    onLaunchGame: (InstalledGame) -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToGameMode: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            onStartReplay(result.resultCode, result.data)
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            onToggleFloatingOverlay(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppBackgroundGradientTop,
                        AppBackground,
                        AppBackground
                    ),
                    startY = 0f,
                    endY = 600f
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header Area (Inspired by reference screenshot)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Friendly brand emblem icon
                            Image(
                                painter = painterResource(id = com.example.R.drawable.img_replay_app_icon),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "GAME REPLAY PRO",
                                color = TextTitle,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "智能游戏回录助手",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Device / Engine Status Pill (like "2台设备在线" in reference)
                    StatusBadge(
                        text = if (state.isBuffering) "正在运行" else "引擎待机",
                        isActive = state.isBuffering
                    )
                }
            }

            // 2. Clean Search / Quick Game Launcher Pill (like search bar in reference)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToGameMode() },
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "搜索游戏或快捷拉起对局（当前：${settings.currentGameTitle}）",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 3. Quick Function Area (4 Feature Cards Grid, inspired by mobile cloud apps)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: 🎮 游戏模式 & 🎥 回录设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickFeatureCard(
                            title = "游戏模式",
                            description = "沉浸对局免打扰",
                            icon = Icons.Default.SportsEsports,
                            iconBgColor = PrimaryBlueLight,
                            iconTint = PrimaryBlue,
                            onClick = onNavigateToGameMode,
                            modifier = Modifier.weight(1f)
                        )
                        QuickFeatureCard(
                            title = "回录设置",
                            description = "画质与缓存时长",
                            icon = Icons.Default.Videocam,
                            iconBgColor = SoftPurpleBg,
                            iconTint = SoftPurple,
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: 📁 视频库 & ⚙ 设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickFeatureCard(
                            title = "视频库",
                            description = "精彩高光回放",
                            icon = Icons.Default.Folder,
                            iconBgColor = SoftOrangeBg,
                            iconTint = SoftOrange,
                            onClick = onNavigateToLibrary,
                            modifier = Modifier.weight(1f)
                        )
                        QuickFeatureCard(
                            title = "设置",
                            description = "悬浮球与存储管理",
                            icon = Icons.Default.Settings,
                            iconBgColor = SoftGreenBg,
                            iconTint = SoftGreen,
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Main Status Card (Clean, Minimal, Large White Floating Card)
            item {
                CyberCard(
                    borderColor = if (state.isBuffering) PrimaryBlue.copy(alpha = 0.35f) else BorderSubtle,
                    glowEffect = state.isBuffering
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Title row with circular indicator, status text, and clean toggle switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (state.isBuffering) SoftGreen else TextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "游戏回录",
                                        color = TextTitle,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (state.isBuffering) "● 正在运行" else "○ 引擎待机",
                                    color = if (state.isBuffering) SoftGreen else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Clean toggle switch
                            Switch(
                                checked = state.isRunning,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                        val captureIntent = projectionManager.createScreenCaptureIntent()
                                        projectionLauncher.launch(captureIntent)
                                    } else {
                                        onStopReplay()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = PrimaryBlue,
                                    uncheckedThumbColor = PureWhite,
                                    uncheckedTrackColor = Color(0xFFDCDFE6)
                                ),
                                modifier = Modifier.testTag("main_replay_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Info Block (已缓存: 60秒 / 状态: 低功耗模式)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF7F9FC))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "已缓存:",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (state.isBuffering) "${state.bufferedSeconds}秒" else "${settings.bufferDurationSeconds}秒 (待机)",
                                    color = if (state.isBuffering) PrimaryBlue else TextBody,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "状态:",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (state.isBuffering) "低功耗模式" else "未启动",
                                    color = if (state.isBuffering) SoftGreen else TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Large Primary Button: ⚡ 保存精彩瞬间
                        Button(
                            onClick = { onTriggerSave(null) },
                            enabled = state.isBuffering && !state.isSavingClip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("save_replay_big_btn"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = PureWhite,
                                disabledContainerColor = Color(0xFFE2E7F0),
                                disabledContentColor = TextMuted
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Save Replay",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isSavingClip) "正在拼接并保存..." else "⚡ 保存精彩瞬间",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Friendly Recording Information (Replacing technical parameter blocks)
            item {
                CyberCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "录制规格信息",
                                color = TextTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "轻触调整",
                                color = PrimaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { onNavigateToSettings() }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Current Quality (当前画质: 1080P 60FPS)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("当前画质", color = TextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (settings.resolutionHeight >= 1080) "1080P" else "720P"} ${settings.fps}FPS",
                                        color = PrimaryBlue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = settings.resolutionLabel,
                                        color = TextBody,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Buffer Duration (缓存时间: 60秒)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("缓存时间", color = TextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${settings.bufferDurationSeconds}秒",
                                        color = PrimaryBlue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "滑动窗口循环",
                                        color = TextBody,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Storage Space (存储空间: 12.5GB)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("存储空间", color = TextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "12.5GB",
                                        color = PrimaryBlue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "剩余可用",
                                        color = TextBody,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Recent Highlights Area (Inspired by "最近播放" in reference screenshot)
            item {
                CyberCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "最近回录",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigateToLibrary() }
                            ) {
                                Text(
                                    text = "查看全部",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = "More",
                                    tint = TextMuted,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (recentClips.isNotEmpty()) {
                            val latest = recentClips.first()
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToLibrary() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail container
                                    Box(
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimaryBlueLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = "Video",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = latest.title,
                                            color = TextTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${latest.gameName} • 长度 ${latest.durationSeconds}秒",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Friendly clean empty state (like media box in reference screenshot)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(PrimaryBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "No replays yet",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "暂无播放内容",
                                    color = TextBody,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "在游戏中随时点击悬浮球，即可极速保存精彩瞬间",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 7. Floating Replay Button Quick Switch
            item {
                CyberCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Floating Button",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "游戏半透明悬浮球",
                                        color = TextTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "退出应用后保留在屏幕上 • 一键存高光",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = settings.floatingButtonEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        overlayPermissionLauncher.launch(intent)
                                    } else {
                                        onToggleFloatingOverlay(checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = PrimaryBlue,
                                    uncheckedThumbColor = PureWhite,
                                    uncheckedTrackColor = Color(0xFFDCDFE6)
                                )
                            )
                        }

                        // Prominent permission prompt if overlay permission is not granted
                        if (settings.floatingButtonEnabled && !Settings.canDrawOverlays(context)) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFFF7E6),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFD591))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚠️ 未授权悬浮窗：退出应用后无法常驻屏幕",
                                        color = Color(0xFFD46B08),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            overlayPermissionLauncher.launch(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFA8C16),
                                            contentColor = PureWhite
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("去授权", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
