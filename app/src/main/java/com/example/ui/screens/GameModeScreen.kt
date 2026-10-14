package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeBg
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle

@Composable
fun GameModeScreen(
    settings: ReplaySettings,
    detectedGames: List<InstalledGame>,
    onToggleGameMode: (Boolean) -> Unit,
    onSelectGame: (String) -> Unit,
    onLaunchGame: (InstalledGame) -> Unit,
    onUpdateSettings: (ReplaySettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Text(
                    text = "游戏模式",
                    color = TextTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "沉浸对局体验与智能启动感知",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        // 1. Game Mode Main Switch Card
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (settings.isGameModeActive) SoftGreenBg else PrimaryBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "Game Mode",
                                tint = if (settings.isGameModeActive) SoftGreen else PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "沉浸游戏模式",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.isGameModeActive) "已激活：精简非必要界面，专注竞技" else "开启后自动精简界面，优先保障流畅度",
                                color = if (settings.isGameModeActive) SoftGreen else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.isGameModeActive,
                        onCheckedChange = { onToggleGameMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = SoftGreen,
                            uncheckedThumbColor = PureWhite,
                            uncheckedTrackColor = Color(0xFFDCDFE6)
                        )
                    )
                }
            }
        }

        // 2. Detected Games (Smart Launcher)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已检测手游列表",
                            color = TextTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "共检测到 ${detectedGames.size} 款",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        detectedGames.forEach { game ->
                            val isCurrentGame = settings.currentGameTitle == game.title
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCurrentGame) Color(0xFFF0F7FF) else Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, if (isCurrentGame) PrimaryBlue.copy(alpha = 0.4f) else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectGame(game.title) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
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
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCurrentGame) PrimaryBlue else Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = game.title.take(1),
                                                color = if (isCurrentGame) PureWhite else TextTitle,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = game.title,
                                                color = TextTitle,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isCurrentGame) "当前关联游戏" else "点击设为当前关联",
                                                color = if (isCurrentGame) PrimaryBlue else TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onLaunchGame(game) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCurrentGame) PrimaryBlue else PureWhite,
                                            contentColor = if (isCurrentGame) PureWhite else PrimaryBlue
                                        ),
                                        border = if (!isCurrentGame) BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)) else null,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Launch",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "启动对局", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Game Helper Quick Toggles
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "对局辅助特性",
                        color = TextTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto orientation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = "Orientation", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("自动识别横竖屏", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("智能适配横屏开黑与竖屏大厅", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = settings.autoDetectOrientation,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoDetectOrientation = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Volume Key shortcut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SoftOrangeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VolumeDown, contentDescription = "Volume Key", tint = SoftOrange, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("音量下键双击盲存", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("激烈团战时盲按双击即可瞬间保存", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = settings.volumeKeyShortcutEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(volumeKeyShortcutEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
