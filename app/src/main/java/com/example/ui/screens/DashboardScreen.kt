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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.QuickFeatureCard
import com.example.ui.components.StatChip
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeBg
import com.example.ui.theme.SubtleSurface
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import com.example.ui.viewmodel.DashboardUiState
import kotlinx.coroutines.delay

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
    onPlayClip: (ReplayClip) -> Unit = {},
    onToggleFloatingBallPreview: () -> Unit = {},
    isFloatingBallPreviewVisible: Boolean = false
) {
    val context = LocalContext.current
    var showGamePicker by remember { mutableStateOf(false) }
    var gameSearchQuery by remember { mutableStateOf("") }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val installedGameTitles = remember(state.detectedGames) {
        state.detectedGames.map { it.title }.distinct()
    }

    val filteredGames = remember(installedGameTitles, gameSearchQuery) {
        if (gameSearchQuery.isBlank()) installedGameTitles
        else installedGameTitles.filter { it.contains(gameSearchQuery, ignoreCase = true) }
    }

    LaunchedEffect(saveFeedbackMessage) {
        if (saveFeedbackMessage != null) {
            delay(2800)
            saveFeedbackMessage = null
        }
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
        // 1. Compact Brand Header (Reduced vertical height to prioritize core content)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_replay_app_icon),
                        contentDescription = "GAME REPLAY PRO",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GAME REPLAY PRO",
                            color = TextTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "智能游戏回录助手",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Floating Ball Interactive Preview Toggle Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isFloatingBallPreviewVisible) PrimaryBlue else PrimaryBlueLight,
                    border = BorderStroke(1.dp, if (isFloatingBallPreviewVisible) PrimaryBlue else BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onToggleFloatingBallPreview() }
                        .testTag("toggle_floating_ball_preview_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = "悬浮球预览",
                            tint = if (isFloatingBallPreviewVisible) PureWhite else PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isFloatingBallPreviewVisible) "收起悬浮球" else "悬浮球预览",
                            color = if (isFloatingBallPreviewVisible) PureWhite else PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2. Game Search / Quick Selection Bar (48dp height)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showGamePicker = !showGamePicker }
                        .testTag("home_game_search_bar"),
                    shape = RoundedCornerShape(14.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, if (showGamePicker) PrimaryBlue else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索或切换关联游戏",
                                tint = if (showGamePicker) PrimaryBlue else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "当前关联：${settings.currentGameTitle}",
                                color = TextTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showGamePicker) "收起" else "切换游戏",
                                color = PrimaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "展开游戏选择",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Expandable Inline Game Search & Selection Panel
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
                            // Search input inside picker
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
                                            Text("输入游戏名称搜索或直接关联...", color = TextMuted, fontSize = 12.sp)
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
                                                    saveFeedbackMessage = "已将当前关联游戏切换为：$gameTitle"
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

        // 3. Primary Hero Card: Replay Status & Main Action (4-Level Visual Hierarchy)
        item {
            CyberCard(
                borderColor = if (state.isBuffering) PrimaryBlue.copy(alpha = 0.35f) else BorderSubtle,
                glowEffect = state.isBuffering
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Level 1: Current Replay Status & Main Switch (No duplicate dots)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "游戏回录",
                                    color = TextTitle,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                StatusBadge(
                                    text = if (state.isBuffering) "正在缓存" else "未开启",
                                    isActive = state.isBuffering
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (state.isBuffering) {
                                    "后台内存循环覆写中，不占用永久存储空间"
                                } else {
                                    "开启后自动在后台循环记住最近 ${settings.bufferDurationSeconds} 秒游戏画面"
                                },
                                color = TextMuted,
                                fontSize = 12.sp
                            )
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
                                uncheckedTrackColor = Color(0xFFD8E0EA)
                            ),
                            modifier = Modifier.testTag("main_replay_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Level 2: Buffer Duration & Current Linked Game (Clear two-column summary)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SubtleSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Buffer Duration Status
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (state.isBuffering) "实时循环缓存" else "设定缓存时长",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (state.isBuffering) "${state.bufferedSeconds}" else "${settings.bufferDurationSeconds}",
                                            color = if (state.isBuffering) PrimaryBlue else TextTitle,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (state.isBuffering) "/ ${settings.bufferDurationSeconds} 秒" else "秒",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(BorderSubtle)
                                )

                                // Right: Current Linked Game
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showGamePicker = !showGamePicker }
                                        .padding(start = 16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "当前关联游戏",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = settings.currentGameTitle,
                                        color = TextTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (state.isBuffering) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val progress = (state.bufferedSeconds.toFloat() / settings.bufferDurationSeconds.toFloat().coerceAtLeast(1f))
                                    .coerceIn(0.05f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = PrimaryBlue,
                                    trackColor = BorderSubtle
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Level 3: Primary Action Button — "保存精彩瞬间" (54dp height, single clean icon)
                    Button(
                        onClick = {
                            if (state.isBuffering) {
                                onTriggerSave(null)
                                saveFeedbackMessage = "已触发回录合成，过去 ${state.bufferedSeconds.coerceAtLeast(1)} 秒画面正在保存至视频库"
                            } else {
                                saveFeedbackMessage = "原型交互提示：当前未开启实时录屏，点击右侧开关可启动真实回录"
                            }
                        },
                        enabled = !state.isSavingClip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_replay_big_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isBuffering) PrimaryBlue else PrimaryBlue.copy(alpha = 0.92f),
                            contentColor = PureWhite,
                            disabledContainerColor = Color(0xFFE8EDF3),
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
                            text = when {
                                state.isSavingClip -> "正在合成高光视频..."
                                state.isBuffering -> "保存精彩瞬间（过去 ${settings.bufferDurationSeconds} 秒）"
                                else -> "保存精彩瞬间"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Feedback banner when user clicks save or switches game
                    AnimatedVisibility(visible = saveFeedbackMessage != null) {
                        saveFeedbackMessage?.let { msg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (state.isBuffering) SoftGreenBg else PrimaryBlueLight,
                                border = BorderStroke(1.dp, if (state.isBuffering) SoftGreen.copy(alpha = 0.3f) else PrimaryBlue.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        color = if (state.isBuffering) SoftGreen else PrimaryBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!state.isBuffering) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "立即开启",
                                            color = PrimaryBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { requestStartScreenCapture() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Level 4: Recording Specifications Summary & Quick Entry Grid
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "当前录制规格",
                            color = TextTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToSettings() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "调整参数",
                                color = PrimaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "调整参数",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val resolutionDisplay = when {
                        settings.resolutionLabel.contains("原生") -> "原生直通"
                        settings.resolutionHeight >= 1440 || settings.resolutionLabel.contains("2K") -> "2K"
                        settings.resolutionHeight >= 1080 || settings.resolutionLabel.contains("1080") -> "1080p"
                        else -> "720p"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip(
                            label = "视频分辨率",
                            value = resolutionDisplay,
                            subValue = if (settings.autoDetectOrientation) "自动横竖屏" else "固定比例",
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
                            label = "缓存时长",
                            value = "${settings.bufferDurationSeconds} 秒",
                            subValue = settings.videoCodec,
                            highlightValue = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Four Quick Navigation Cards (Unified #EAF3FF Blue Icon Containers)
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
                        description = if (settings.isGameModeActive) "沉浸模式已开启" else "关联游戏与快捷启动",
                        icon = Icons.Default.SportsEsports,
                        isHighlighted = settings.isGameModeActive,
                        onClick = onNavigateToGameMode,
                        modifier = Modifier.weight(1f)
                    )
                    QuickFeatureCard(
                        title = "回录设置",
                        description = "画质、码率与帧率",
                        icon = Icons.Default.Tune,
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
                        description = if (recentClips.isNotEmpty()) "已存 ${recentClips.size} 个高光片段" else "管理与预览高光录像",
                        icon = Icons.Default.Folder,
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.weight(1f)
                    )
                    QuickFeatureCard(
                        title = "设置",
                        description = "悬浮球与存储空间",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Saved Videos Section (Clearly distinguished from "Active Memory Buffering")
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "已保存视频",
                                color = TextTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryBlueLight
                            ) {
                                Text(
                                    text = "${recentClips.size} 个文件",
                                    color = PrimaryBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToLibrary() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "进入视频库",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "进入视频库",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentClips.isNotEmpty()) {
                        val latest = recentClips.first()
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SubtleSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onPlayClip(latest) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 68.dp, height = 46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "播放预览",
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val safeDuration = if (latest.durationSeconds in 1..1800) latest.durationSeconds else 60
                                    Text(
                                        text = "${latest.gameName} • ${latest.resolution} • ${safeDuration}秒",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "预览",
                                    color = PrimaryBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // Compact empty state with quick demo preview trigger
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SubtleSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimaryBlueLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "暂无已保存的高光视频",
                                            color = TextTitle,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "点击「保存精彩瞬间」后才会生成视频文件",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Quick entry to preview the player UI with a demo highlight clip
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PureWhite,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onPlayClip(
                                                ReplayClip(
                                                    id = -1L,
                                                    title = "${settings.currentGameTitle}_五杀高光时刻_演示",
                                                    filePath = "DEMO_PREVIEW",
                                                    durationSeconds = settings.bufferDurationSeconds,
                                                    fileSizeBytes = 42_500_000L,
                                                    createdAt = System.currentTimeMillis(),
                                                    resolution = "1920x1080",
                                                    fps = settings.fps,
                                                    gameName = settings.currentGameTitle,
                                                    isFavorite = false
                                                )
                                            )
                                        }
                                        .testTag("home_demo_player_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = "播放器预览",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "预览播放器",
                                            color = PrimaryBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = BorderSubtle
                    )

                    // Compact Floating Ball & Quick Settings Tile Row inside the same bottom card (reducing card proliferation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "游戏内悬浮球快捷保存",
                                color = TextTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.floatingButtonEnabled && !Settings.canDrawOverlays(context)) {
                                    "需授予系统悬浮窗权限以在游戏上层显示"
                                } else {
                                    "自动贴边半透明隐身，对局中点按即存"
                                },
                                color = if (settings.floatingButtonEnabled && !Settings.canDrawOverlays(context)) SoftOrange else TextMuted,
                                fontSize = 11.sp
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
                                uncheckedTrackColor = Color(0xFFD8E0EA)
                            )
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(modifier = Modifier.height(10.dp))
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
