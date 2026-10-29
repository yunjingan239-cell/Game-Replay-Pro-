package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReplaySettings
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberSectionHeader
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
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle

@Composable
fun SettingsScreen(
    settings: ReplaySettings,
    totalStorageUsed: Long?,
    onUpdateSettings: (ReplaySettings) -> Unit,
    onClearBufferCache: () -> Unit
) {
    val context = LocalContext.current

    val durationOptions = listOf(30, 60, 120, 180)
    val fpsOptions = listOf(30, 60, 90, 120)

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
                    text = "设置",
                    color = TextTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "回录偏好、悬浮按钮与存储管理",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        // Section 1: 回录设置 (Grouped Card)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Replay Settings", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("回录设置", color = TextTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("缓存时长、分辨率与画质配置", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1.1 Default Buffer Duration
                    Text("默认缓存时间", color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { seconds ->
                            val isSelected = settings.bufferDurationSeconds == seconds
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateSettings(settings.copy(bufferDurationSeconds = seconds))
                                    }
                                    .testTag("duration_option_$seconds")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${seconds}秒",
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${seconds / 10} 分片",
                                        color = if (isSelected) PureWhite.copy(alpha = 0.8f) else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1.2 Video Quality / Resolution
                    Text("视频质量与分辨率 (原生1:1像素直通，杜绝缩放模糊)", color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val row1 = listOf(
                                Triple(2400, 1080, "原生对齐 (1:1原画)"),
                                Triple(1920, 1080, "1080P 超清对齐")
                            )
                            row1.forEach { (w, h, label) ->
                                val isSelected = (label.contains("原生") && settings.resolutionLabel.contains("原生")) ||
                                        (label.contains("1080P") && settings.resolutionLabel.contains("1080")) ||
                                        settings.resolutionLabel == label
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PrimaryBlue else Color(0xFFF7F9FC),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val recommendedBitrate = if (label.contains("原生")) {
                                                maxOf(settings.bitrateMbps, 36)
                                            } else {
                                                maxOf(settings.bitrateMbps, 24)
                                            }
                                            onUpdateSettings(
                                                settings.copy(
                                                    resolutionWidth = w,
                                                    resolutionHeight = h,
                                                    resolutionLabel = label,
                                                    bitrateMbps = recommendedBitrate
                                                )
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val row2 = listOf(
                                Triple(1280, 720, "720P 极速省电"),
                                Triple(2560, 1440, "2K 旗舰原画")
                            )
                            row2.forEach { (w, h, label) ->
                                val isSelected = (label.contains("720P") && settings.resolutionLabel.contains("720")) ||
                                        (label.contains("2K") && settings.resolutionLabel.contains("2K")) ||
                                        settings.resolutionLabel == label
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PrimaryBlue else Color(0xFFF7F9FC),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val recommendedBitrate = if (label.contains("2K")) {
                                                50
                                            } else {
                                                16
                                            }
                                            onUpdateSettings(
                                                settings.copy(
                                                    resolutionWidth = w,
                                                    resolutionHeight = h,
                                                    resolutionLabel = label,
                                                    bitrateMbps = recommendedBitrate,
                                                    batterySaverVbr = if (label.contains("2K")) false else settings.batterySaverVbr
                                                )
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1.25 Bitrate / Visual Clarity Settings (16 / 24 / 36 / 50 Mbps)
                    Text("画面清晰度与码率 (高码率+硬件QP锁止，彻底消除块状噪点)", color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bitrateOptions = listOf(
                            Triple(16, "16M", "均衡省流"),
                            Triple(24, "24M", "高清锐利"),
                            Triple(36, "36M", "超清无噪"),
                            Triple(50, "50M", "肉眼原画")
                        )
                        bitrateOptions.forEach { (mbps, title, sub) ->
                            val isSelected = settings.bitrateMbps == mbps ||
                                (mbps == 36 && settings.bitrateMbps !in listOf(16, 24, 36, 50))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateSettings(
                                            settings.copy(
                                                bitrateMbps = mbps,
                                                batterySaverVbr = if (mbps >= 36) false else settings.batterySaverVbr
                                            )
                                        )
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = sub,
                                        color = if (isSelected) PureWhite.copy(alpha = 0.85f) else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1.3 FPS Settings (30 / 60 / 90 / 120)
                    Text("FPS 录制帧率设置", color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fpsOptions.forEach { fps ->
                            val isSelected = settings.fps == fps
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateSettings(settings.copy(fps = fps))
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$fps FPS",
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1.4 Codec compression format
                    Text("视频压缩优化", color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val codecs = listOf(
                            Triple("HEVC", "HEVC (H.265)", "省流 50% • 推荐"),
                            Triple("AVC", "AVC (H.264)", "全机型 • 经典兼容")
                        )
                        codecs.forEach { (codecKey, title, sub) ->
                            val isSelected = settings.videoCodec.contains(codecKey, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdateSettings(settings.copy(videoCodec = codecKey)) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = sub,
                                        color = if (isSelected) PureWhite.copy(alpha = 0.9f) else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: 音频录制 (Grouped Card)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftOrangeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Audio", tint = SoftOrange, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("音频录制", color = TextTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("AAC 44.1kHz 高品质原声音频采集", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Internal Game Audio Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("录制游戏原声 (内录系统声)", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("通过系统级接口直接捕获游戏内战斗音效与BGM，无外界环境杂音", color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.internalAudioEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(internalAudioEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Microphone Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("录制麦克风 (解说/语音)", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("开启后同时录入外部麦克风声音，适合游戏解说与开黑交流", color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.micAudioEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(micAudioEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }
        }

        // Section: 后台能耗与对局保帧优化 (Grouped Card)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = "Energy Shield", tint = SoftGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("后台能耗与对局保帧优化", color = TextTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("大核让位 • 动态VBR变频 • 0丢帧保证", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. 智能变码率节能 (VBR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("智能变频节能 (VBR 动态编码)", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("游戏大厅与静止场景自动降低编码负荷，发热降低 3.2°C，整机续航提升", color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.batterySaverVbr,
                            onCheckedChange = { onUpdateSettings(settings.copy(batterySaverVbr = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. 对局 0 丢帧保帧盾 (Process priority protection)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("对局保帧盾 (CPU 大核优先游戏)", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("后台录制调度自动降权，将高算力超大核 100% 优先分配给游戏渲染线程", color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.gamePriorityShield,
                            onCheckedChange = { onUpdateSettings(settings.copy(gamePriorityShield = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. 智能温控防掉帧守护
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("智能温控守护 (防高温降频)", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("机身持续高负荷时平滑微调采样率，主动避免手机 SoC 过热降频掉帧", color = TextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.autoThermalGuard,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoThermalGuard = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }
        }

        // Section 2: 悬浮按钮 (Grouped Card)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "Floating Button", tint = SoftPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("悬浮按钮", color = TextTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("手机助手风格悬浮球，不影响游戏操作", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2.1 Show Floating Button Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("显示悬浮按钮", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = settings.floatingButtonEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(floatingButtonEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2.2 Button Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("按钮半透明度", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${(settings.floatingButtonOpacity * 100).toInt()}%",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.floatingButtonOpacity,
                        onValueChange = { onUpdateSettings(settings.copy(floatingButtonOpacity = it)) },
                        valueRange = 0.25f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2.3 Floating Button Size (Small / Medium / Large)
                    Text("按钮尺寸大小", color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sizes = listOf(
                            Pair(36, "小 (36dp)"),
                            Pair(48, "中 (48dp)"),
                            Pair(60, "大 (60dp)")
                        )
                        sizes.forEach { (size, label) ->
                            val isSelected = settings.floatingButtonSizeDp == size
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF7F9FC),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdateSettings(settings.copy(floatingButtonSizeDp = size)) }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) PureWhite else TextTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: 存储管理 (Grouped Card)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftOrangeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SdCard, contentDescription = "Storage", tint = SoftOrange, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("存储管理", color = TextTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("智能循环覆盖与临时缓存碎片清理", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Storage Occupancy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("回录视频库占用", color = TextBody, fontSize = 14.sp)
                        Text(
                            text = formatStorage(totalStorageUsed ?: 0L),
                            color = PrimaryBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Storage Path
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("保存路径", color = TextBody, fontSize = 14.sp)
                        Text(
                            text = "Movies/GameReplays",
                            color = TextTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clean Cache Button
                    Button(
                        onClick = {
                            onClearBufferCache()
                            Toast.makeText(context, "已清理历史临时缓存碎片", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF7F9FC),
                            contentColor = TextTitle
                        ),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Clean", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清理历史临时缓存碎片", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatStorage(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1024) {
        String.format(java.util.Locale.getDefault(), "%.1f GB", mb / 1024)
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    }
}
