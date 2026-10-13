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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReplaySettings
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberSectionHeader
import com.example.ui.components.PulsingRadarDot
import com.example.ui.components.StatChip
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RadiantCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    settings: ReplaySettings,
    onStartReplay: (Int, Intent?) -> Unit,
    onStopReplay: () -> Unit,
    onTriggerSave: (String?) -> Unit,
    onToggleFloatingOverlay: (Boolean) -> Unit,
    onToggleGameMode: (Boolean) -> Unit,
    onSelectGame: (String) -> Unit,
    onLaunchGame: (com.example.engine.InstalledGame) -> Unit
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

    val popularGames = listOf(
        "王者荣耀", "和平精英", "Valorant Mobile", "原神 (Genshin)", "英雄联盟手游", "通用游戏"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GAME REPLAY PRO",
                        color = ElectricCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (settings.isGameModeActive) "游戏模式已激活 • 沉浸HUD" else "ShadowPlay 极速瞬间回录助手",
                        color = if (settings.isGameModeActive) NeonGreen else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurfaceElevated,
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingRadarDot(isActive = state.isBuffering)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isBuffering) "正在循环缓存" else "引擎待机",
                            color = if (state.isBuffering) NeonGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Game Mode Toggle Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onToggleGameMode(!settings.isGameModeActive)
                    },
                color = if (settings.isGameModeActive) Color(0xFF132F2B) else CyberSurfaceCard,
                border = BorderStroke(
                    1.dp,
                    if (settings.isGameModeActive) NeonGreen else CyberBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (settings.isGameModeActive) NeonGreen.copy(alpha = 0.2f) else CyberSurfaceElevated)
                                .border(1.dp, if (settings.isGameModeActive) NeonGreen else CyberBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.isGameModeActive) Icons.Filled.Gamepad else Icons.Outlined.Gamepad,
                                contentDescription = "Game Mode",
                                tint = if (settings.isGameModeActive) NeonGreen else ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "沉浸游戏模式",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (settings.isGameModeActive) "已自动精简非必要UI，全神贯注电竞对局" else "开启后自动隐藏非必要UI，仅保留核心按键",
                                color = if (settings.isGameModeActive) NeonGreen else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.isGameModeActive,
                        onCheckedChange = { onToggleGameMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier.testTag("game_mode_switch")
                    )
                }
            }
        }

        // Main Replay Status Card
        item {
            val statusBorderColor by animateColorAsState(
                targetValue = if (state.isBuffering) ElectricCyan else CyberBorder,
                label = "border_color"
            )

            CyberCard(
                borderColor = statusBorderColor,
                glowEffect = state.isBuffering
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "回录循环缓存状态",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (state.isBuffering) "● 正在持续缓存最近 ${state.targetBufferDuration}秒" else "○ 回录引擎未启动",
                                color = if (state.isBuffering) TextPrimary else TextMuted,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberSurfaceDark,
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Text(
                                text = "硬件GPU零拷贝",
                                color = ElectricCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxSegments = settings.maxChunksInRing
                    val activeSegments = state.bufferedChunkCount.coerceAtMost(maxSegments)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "滑动窗口队列 (${state.bufferedSeconds}s / ${state.targetBufferDuration}s)",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${activeSegments}/$maxSegments 分片",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0 until maxSegments) {
                                val isChunkReady = i < activeSegments
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isChunkReady) ElectricCyan else CyberBorder.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatChip(
                                label = "编码压缩",
                                value = "${state.codecMime} (${state.compressionRatio})",
                                modifier = Modifier.weight(1.3f)
                            )
                            StatChip(
                                label = "画质规格",
                                value = "${state.activeFps}FPS • ${state.activeResolution}",
                                modifier = Modifier.weight(1.1f)
                            )
                            StatChip(
                                label = "色彩",
                                value = if (state.isHdrActive) "10-bit HDR" else "8-bit SDR",
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatChip(
                                label = "动态屏幕方向",
                                value = state.currentOrientation,
                                modifier = Modifier.weight(1.3f)
                            )
                            StatChip(
                                label = "RAM缓存配额",
                                value = "~${(state.bufferedSizeBytes / (1024 * 1024)).coerceAtLeast(14)} MB",
                                modifier = Modifier.weight(0.9f)
                            )
                            StatChip(
                                label = "硬件加速",
                                value = if (state.isHardwareAccelerated) "GPU零拷贝" else "CPU兼容",
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Big Instant Replay Save Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                if (state.isBuffering) Color(0xFF00B4D8) else Color(0xFF1E2438),
                                if (state.isBuffering) Color(0xFF7209B7) else Color(0xFF151A28)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (state.isBuffering) ElectricCyan else CyberBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        enabled = state.isBuffering && !state.isSavingClip
                    ) {
                        onTriggerSave(null)
                    }
                    .padding(vertical = 20.dp, horizontal = 16.dp)
                    .testTag("save_replay_big_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Instant Replay",
                        tint = if (state.isBuffering) Color.White else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = if (state.isSavingClip) "正在拼接并保存回录..." else "保存精彩瞬间 (过去${state.targetBufferDuration}秒)",
                            color = if (state.isBuffering) Color.White else TextMuted,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (state.isBuffering) "自动命名: ${settings.currentGameTitle}_YYYYMMDD_HHMMSS.mp4" else "请先点击下方开启回录",
                            color = if (state.isBuffering) Color.White.copy(alpha = 0.85f) else TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Shortcut Triggers Notification Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CyberSurfaceElevated,
                border = BorderStroke(1.dp, CyberBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = "Volume Shortcut",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "快捷抓取组合键：音量下键",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "双击音量下键 或 点击屏幕悬浮球均可瞬间保存",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onTriggerSave(null) },
                        enabled = state.isBuffering,
                        modifier = Modifier.height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, ElectricCyan)
                    ) {
                        Text("测试按键", fontSize = 11.sp, color = ElectricCyan)
                    }
                }
            }
        }

        // Primary Engine Controls
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "回录主控开关",
                        subtitle = "开启后自动在后台硬件循环缓存最近片段"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!state.isRunning) {
                            CyberActionButton(
                                text = "开启瞬间回录",
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                modifier = Modifier.weight(1f),
                                testTag = "start_replay_btn",
                                onClick = {
                                    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                    val captureIntent = projectionManager.createScreenCaptureIntent()
                                    projectionLauncher.launch(captureIntent)
                                }
                            )
                        } else {
                            CyberActionButton(
                                text = "停止回录引擎",
                                icon = Icons.Default.Stop,
                                isPrimary = false,
                                modifier = Modifier.weight(1f),
                                testTag = "stop_replay_btn",
                                onClick = {
                                    onStopReplay()
                                }
                            )
                        }
                    }
                }
            }
        }

        // NON-ESSENTIAL UI (Automatically hidden when isGameModeActive == true)
        if (!settings.isGameModeActive) {
            // Game Selection & Game Assistant Hub
            item {
                CyberCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CyberSectionHeader(
                            title = "手机游戏助手 • 智能对局启动检测",
                            subtitle = "自动扫描手机已安装手游，一键拉起游戏并自动关联回录"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val gamesList = if (state.detectedGames.isNotEmpty()) state.detectedGames else popularGames.map { 
                            com.example.engine.InstalledGame(title = it, packageName = "com.example.game") 
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(gamesList) { game ->
                                val isSelected = settings.currentGameTitle == game.title
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) CyberSurfaceElevated else CyberSurfaceDark,
                                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
                                    modifier = Modifier
                                        .clickable { onSelectGame(game.title) }
                                        .testTag("game_item_${game.title}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = game.title,
                                            color = if (isSelected) ElectricCyan else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { onLaunchGame(game) },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            border = BorderStroke(1.dp, if (isSelected) NeonGreen else CyberBorder)
                                        ) {
                                            Text(
                                                text = if (isSelected) "启动对局 ▶" else "选中并启动",
                                                fontSize = 10.sp,
                                                color = if (isSelected) NeonGreen else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Micro-Dot Controller Card
            item {
                CyberCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceElevated)
                                    .border(1.dp, ElectricCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Floating Button",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "游戏半透明悬浮按钮",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "支持透明度/大小调节/边缘收起隐藏",
                                    color = TextSecondary,
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
                                checkedThumbColor = CyberBlack,
                                checkedTrackColor = ElectricCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CyberSurfaceElevated
                            ),
                            modifier = Modifier.testTag("floating_dot_switch")
                        )
                    }
                }
            }

            // Zero Game Lag Assurance Card
            item {
                CyberCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Zero Lag",
                            tint = NeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "手游性能保障机制",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "仅占用环形缓存配额，无磁盘反复全写，硬件H.264实时编码，保证游戏 60/120 帧丝滑运行。",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
