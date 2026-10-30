package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReplaySettings
import com.example.engine.InstalledGame
import com.example.ui.components.CyberCard
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SubtleSurface
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import kotlinx.coroutines.delay

@Composable
fun GameModeScreen(
    settings: ReplaySettings,
    detectedGames: List<InstalledGame>,
    onToggleGameMode: (Boolean) -> Unit,
    onSelectGame: (String) -> Unit,
    onLaunchGame: (InstalledGame) -> Unit,
    onUpdateSettings: (ReplaySettings) -> Unit
) {
    var statusFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LaunchedEffect(statusFeedback) {
        if (statusFeedback != null) {
            delay(2500)
            statusFeedback = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Compact Header with Current Linked Game Tag
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "游戏模式",
                        color = TextTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "沉浸对局体验与智能启动感知",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                if (detectedGames.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryBlueLight,
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "关联：${settings.currentGameTitle}",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Toast / Status Feedback Banner
        if (statusFeedback != null) {
            item {
                AnimatedVisibility(visible = statusFeedback != null) {
                    statusFeedback?.let { (isSuccess, msg) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSuccess) SoftGreenBg else PrimaryBlueLight,
                            border = BorderStroke(
                                1.dp,
                                if (isSuccess) SoftGreen.copy(alpha = 0.35f) else PrimaryBlue.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isSuccess) SoftGreen else PrimaryBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = if (isSuccess) SoftGreen else PrimaryBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Immersive Game Mode Main Switch Card
        item {
            CyberCard(
                borderColor = if (settings.isGameModeActive) PrimaryBlue.copy(alpha = 0.35f) else BorderSubtle,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (settings.isGameModeActive) SoftGreenBg else PrimaryBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "沉浸游戏模式",
                                tint = if (settings.isGameModeActive) SoftGreen else PrimaryBlue,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "沉浸游戏模式",
                                    color = TextTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (settings.isGameModeActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SoftGreenBg
                                    ) {
                                        Text(
                                            text = "已开启",
                                            color = SoftGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.isGameModeActive) {
                                    "已激活对局专注视图，优先保障游戏与编码帧率稳定"
                                } else {
                                    "开启后精简非必要界面干扰，优先保障对局与编码帧率稳定"
                                },
                                color = if (settings.isGameModeActive) SoftGreen else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Switch(
                        checked = settings.isGameModeActive,
                        onCheckedChange = { enabled ->
                            onToggleGameMode(enabled)
                            statusFeedback = Pair(
                                enabled,
                                if (enabled) "已开启沉浸游戏模式" else "已关闭沉浸游戏模式"
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = PureWhite,
                            uncheckedTrackColor = Color(0xFFD8E0EA)
                        ),
                        modifier = Modifier.testTag("game_mode_main_switch")
                    )
                }
            }
        }

        // 3. Installed Games List Card (Strictly ONLY shows games actually installed on the device)
        item {
            CyberCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "本机已安装游戏",
                                color = TextTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "点击列表项切换关联，点击右侧按钮直接启动游戏",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SubtleSurface,
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = "共 ${detectedGames.size} 款",
                                color = TextBody,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (detectedGames.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "暂未检测到本机已安装的手游",
                                color = TextTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "安装游戏后将自动在此处显示真实图标与快捷启动入口",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detectedGames.forEach { game ->
                                val isSelected = settings.currentGameTitle == game.title
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) PrimaryBlueLight else SubtleSurface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) PrimaryBlue else BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSelectGame(game.title)
                                            statusFeedback = Pair(false, "已将「${game.title}」设为当前关联游戏")
                                        }
                                        .testTag("game_list_item_${game.title}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left: Authentic App Icon + Full Game Name + Association Status
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            GameAppIcon(
                                                game = game,
                                                isCurrentGame = isSelected
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = game.title,
                                                        color = TextTitle,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isSelected) PrimaryBlue else PureWhite,
                                                        border = if (!isSelected) BorderStroke(1.dp, BorderSubtle) else null
                                                    ) {
                                                        Text(
                                                            text = if (isSelected) "已关联" else "点击关联",
                                                            color = if (isSelected) PureWhite else TextMuted,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = if (isSelected) "当前回录关联游戏" else "本机已安装",
                                                    color = if (isSelected) PrimaryBlue else TextMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Right: Launch Game Action Button
                                        Button(
                                            onClick = {
                                                onLaunchGame(game)
                                                statusFeedback = Pair(true, "正在启动：${game.title}")
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) PrimaryBlue else PureWhite,
                                                contentColor = if (isSelected) PureWhite else PrimaryBlue
                                            ),
                                            border = if (!isSelected) BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.45f)) else null,
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("launch_game_btn_${game.title}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "启动游戏",
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "启动游戏",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
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

        // 4. Gaming Assist Toggles Card
        item {
            CyberCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "对局辅助设置",
                        color = TextTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "画幅方向自适应与实体按键快捷保存",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 1: Auto Orientation Detection
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "自动识别横竖屏",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "自动识别横竖屏",
                                    color = TextTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "自动适配横屏对局与竖屏游戏，保持原生画面比例",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = settings.autoDetectOrientation,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(autoDetectOrientation = it))
                                statusFeedback = Pair(
                                    false,
                                    if (it) "已开启自动识别横竖屏" else "已关闭自动识别横竖屏"
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = Color(0xFFD8E0EA)
                            ),
                            modifier = Modifier.testTag("auto_orientation_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = BorderSubtle
                    )

                    // Toggle 2: Volume Key Quick Save
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeDown,
                                    contentDescription = "音量键快捷保存",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "音量键快捷保存",
                                    color = TextTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "对局中快速双击「音量下键」即可触发保存精彩瞬间",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = settings.volumeKeyShortcutEnabled,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(volumeKeyShortcutEnabled = it))
                                statusFeedback = Pair(
                                    false,
                                    if (it) "已开启音量键快捷保存" else "已关闭音量键快捷保存"
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = Color(0xFFD8E0EA)
                            ),
                            modifier = Modifier.testTag("volume_shortcut_switch")
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GameAppIcon(
    game: InstalledGame,
    isCurrentGame: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIcon = remember(game.packageName) {
        loadAppIconBitmap(context, game.packageName)
    }

    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = game.title,
            modifier = modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCurrentGame) PureWhite else SubtleSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = game.title,
                tint = if (isCurrentGame) PrimaryBlue else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun loadAppIconBitmap(context: Context, packageName: String): ImageBitmap? {
    return try {
        val pm = context.packageManager
        val drawable = pm.getApplicationIcon(packageName)
        val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
