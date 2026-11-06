package com.example.ui.screens

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ReplayClip
import com.example.data.model.ReplaySettings
import com.example.engine.InstalledGame
import com.example.service.ReplayTileService
import com.example.ui.components.CyberCard
import com.example.ui.components.PulsingRadarDot
import com.example.ui.components.QuickFeatureCard
import com.example.ui.components.StatChip
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeBg
import com.example.ui.theme.SoftPurple
import com.example.ui.theme.SoftPurpleBg
import com.example.ui.theme.SubtleSurface
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
    onNavigateToSettings: () -> Unit = {},
    onUpdateSettings: (ReplaySettings) -> Unit = {},
    onPlayClip: (ReplayClip) -> Unit = {}
) {
    val context = LocalContext.current
    var showGamePicker by remember { mutableStateOf(false) }
    var gameSearchQuery by remember { mutableStateOf("") }

    val installedGameTitles = remember(state.detectedGames) {
        state.detectedGames.map { it.title }.distinct()
    }

    val filteredGames = remember(installedGameTitles, gameSearchQuery) {
        if (gameSearchQuery.isBlank()) installedGameTitles
        else installedGameTitles.filter { it.contains(gameSearchQuery, ignoreCase = true) }
    }

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

    fun requestStartScreenCapture() {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val config = MediaProjectionConfig.createConfigForDefaultDisplay()
            projectionManager.createScreenCaptureIntent(config)
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        projectionLauncher.launch(captureIntent)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Brand Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_replay_app_icon),
                    contentDescription = "GAME REPLAY PRO",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "GAME REPLAY PRO",
                        color = TextTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "智能游戏回录助手",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Search / Quick Game Selection Pill Bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showGamePicker = !showGamePicker }
                        .testTag("home_game_search_bar"),
                    shape = RoundedCornerShape(24.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, if (showGamePicker) PrimaryBlue else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索游戏",
                            tint = if (showGamePicker) PrimaryBlue else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "搜索游戏或快捷拉起对局（当前：${settings.currentGameTitle}）",
                            color = TextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showGamePicker,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SubtleSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search game",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (gameSearchQuery.isEmpty()) {
                                            Text("输入本机游戏名称快速筛选...", color = TextMuted, fontSize = 12.sp)
                                        }
                                        BasicTextField(
                                            value = gameSearchQuery,
                                            onValueChange = { gameSearchQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = TextTitle, fontSize = 13.sp),
                                            cursorBrush = SolidColor(PrimaryBlue),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (gameSearchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { gameSearchQuery = "" },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "清除", tint = TextMuted, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (filteredGames.isEmpty()) {
                                Text(
                                    text = if (installedGameTitles.isEmpty()) "暂未检测到本机已安装的手游" else "未找到匹配的本机游戏",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(filteredGames) { gameTitle ->
                                        val isSelected = settings.currentGameTitle == gameTitle
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) PrimaryBlueLight else SubtleSurface,
                                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onSelectGame(gameTitle)
                                                    showGamePicker = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = PrimaryBlue,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = gameTitle,
                                                    color = if (isSelected) PrimaryBlue else TextTitle,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. 2x2 Quick Feature Entry Grid (Original position above Replay Card with colored icon boxes)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                        isHighlighted = settings.isGameModeActive,
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

        // 4. Main Replay Status Card ("游戏回录")
        item {
            CyberCard(
                borderColor = Color(0xFFBFD7FF),
                glowEffect = state.isBuffering
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isBuffering) SoftGreen else TextMuted.copy(alpha = 0.45f))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "游戏回录",
                                    color = TextTitle,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsingRadarDot(
                                    isActive = state.isBuffering,
                                    activeColor = if (state.isBuffering) SoftGreen else TextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.isBuffering) "正在运行" else "未开启",
                                    color = if (state.isBuffering) SoftGreen else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Switch(
                            checked = state.isRunning,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    requestStartScreenCapture()
                                } else {
                                    onStopReplay()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = Color(0xFFD5E0EE)
                            ),
                            modifier = Modifier.testTag("main_replay_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Inner Buffer & Status Summary Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SubtleSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "已缓存:",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${if (state.isBuffering) state.bufferedSeconds else 0}秒",
                                    color = PrimaryBlue,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "状态:",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.isBuffering) "低功耗模式" else "待机中",
                                    color = if (state.isBuffering) SoftGreen else TextMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Save Action Button (Single white lightning icon, no duplicate yellow emoji)
                    Button(
                        onClick = {
                            if (state.isBuffering) {
                                onTriggerSave(null)
                                Toast.makeText(context, "正在合成回录视频，请稍候...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "请先打开右上角开关启动游戏回录", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !state.isSavingClip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_replay_big_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = PureWhite,
                            disabledContainerColor = Color(0xFFE8EEF5),
                            disabledContentColor = TextMuted
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "保存精彩瞬间",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isSavingClip) "正在合成精彩瞬间..." else "保存精彩瞬间",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Recording Specifications Card ("录制规格信息")
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
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onNavigateToSettings() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val resolutionDisplay = when {
                        settings.resolutionLabel.contains("原生") -> "原生直通"
                        settings.resolutionHeight >= 1440 || settings.resolutionLabel.contains("2K") -> "2K 超清"
                        settings.resolutionHeight >= 1080 || settings.resolutionLabel.contains("1080") -> "1080P"
                        else -> "720P"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip(
                            label = "画面分辨率",
                            value = resolutionDisplay,
                            subValue = if (settings.autoDetectOrientation) "自动识别横竖屏" else "固定比例",
                            highlightValue = true,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "帧率与码率",
                            value = "${settings.fps} FPS",
                            subValue = "${settings.bitrateMbps} Mbps",
                            highlightValue = true,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "循环时长",
                            value = "${settings.bufferDurationSeconds} 秒",
                            subValue = settings.videoCodec,
                            highlightValue = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 6. Floating Overlay & Quick Settings Tile Card
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "游戏内悬浮球快捷保存",
                                color = TextTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (settings.floatingButtonEnabled && !Settings.canDrawOverlays(context)) {
                                    "需授予系统悬浮窗权限以在游戏上层显示"
                                } else {
                                    "自动贴边半透明隐身，对局中点按即存"
                                },
                                color = if (settings.floatingButtonEnabled && !Settings.canDrawOverlays(context)) SoftOrange else TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        if (settings.floatingButtonEnabled && !Settings.canDrawOverlays(context)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftOrangeBg,
                                border = BorderStroke(1.dp, SoftOrange.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        overlayPermissionLauncher.launch(intent)
                                    }
                            ) {
                                Text(
                                    text = "去授权",
                                    color = SoftOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                                uncheckedTrackColor = Color(0xFFD5E0EE)
                            )
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlueLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                                        val component = ComponentName(context, ReplayTileService::class.java)
                                        statusBarManager?.requestAddTileService(
                                            component,
                                            context.getString(R.string.tile_replay_label),
                                            Icon.createWithResource(context, R.drawable.ic_qs_replay),
                                            context.mainExecutor
                                        ) { result ->
                                            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                                Toast.makeText(context, "已添加控制中心快捷开关", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "请在系统通知栏编辑中添加「游戏回录」", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "添加「游戏回录」快捷开关至系统状态栏",
                                    color = PrimaryBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
