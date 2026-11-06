package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import java.util.Locale

private data class ResolutionSpec(
    val label: String,
    val subtitle: String,
    val width: Int,
    val height: Int
)

@Composable
fun SettingsScreen(
    settings: ReplaySettings,
    totalStorageUsed: Long?,
    onUpdateSettings: (ReplaySettings) -> Unit,
    onClearBufferCache: () -> Unit
) {
    val context = LocalContext.current

    val durationOptions = listOf(15, 30, 60, 120, 180)
    val fpsOptions = listOf(30, 60, 90, 120)
    val bitrateOptions = listOf(16, 24, 36, 50)
    val resolutionOptions = listOf(
        ResolutionSpec("原生分辨率直通", "1:1 物理屏幕像素", 2400, 1080),
        ResolutionSpec("1080P 超清", "1920 × 1080", 1920, 1080),
        ResolutionSpec("720P 流畅", "1280 × 720", 1280, 720),
        ResolutionSpec("2K 极致", "2560 × 1440", 2560, 1440)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Text(
                    text = "设置",
                    color = TextTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "配置高画质录制参数、缓存时长与音频偏好",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        // 2. Buffer Duration Card (15s / 30s / 60s / 120s / 180s)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "回录缓存时长",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "回录缓存时长",
                                    color = TextTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "点击保存时回溯过去多长时间的画面",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlueLight,
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "${settings.bufferDurationSeconds}秒",
                                color = PrimaryBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { sec ->
                            val isSelected = settings.bufferDurationSeconds == sec
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else SubtleSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateSettings(settings.copy(bufferDurationSeconds = sec))
                                    }
                                    .testTag("duration_btn_$sec")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}秒",
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Video Resolution Card (2x2 Grid)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "视频分辨率",
                                tint = SoftPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "视频分辨率",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "原生分辨率直通可避免缩放模糊，呈现肉眼级锐利画质",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val rows = resolutionOptions.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { option ->
                                    val isSelected = when {
                                        option.label.contains("原生") -> settings.resolutionLabel.contains("原生")
                                        option.label.contains("2K") -> settings.resolutionLabel.contains("2K") || settings.resolutionHeight == 1440
                                        option.label.contains("720") -> settings.resolutionLabel.contains("720") || settings.resolutionHeight == 720
                                        else -> !settings.resolutionLabel.contains("原生") &&
                                            !settings.resolutionLabel.contains("2K") &&
                                            !settings.resolutionLabel.contains("720") &&
                                            settings.resolutionHeight == 1080
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PrimaryBlueLight else SubtleSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryBlue else BorderSubtle
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onUpdateSettings(
                                                    settings.copy(
                                                        resolutionWidth = option.width,
                                                        resolutionHeight = option.height,
                                                        resolutionLabel = option.label
                                                    )
                                                )
                                            }
                                            .testTag("resolution_option_${option.height}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = option.label,
                                                color = if (isSelected) PrimaryBlue else TextTitle,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = option.subtitle,
                                                color = if (isSelected) PrimaryBlue else TextMuted,
                                                fontSize = 11.sp
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

        // 4. Frame Rate (FPS) & Bitrate Card
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftOrangeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "帧率与码率",
                                tint = SoftOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "帧率与高画质码率",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "硬件 CBR 恒定高码率消除高速团战马赛克与块状噪点",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // FPS Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "录制帧率 (FPS)",
                            color = TextTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${settings.fps} FPS",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fpsOptions.forEach { fps ->
                            val isSelected = settings.fps == fps
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else SubtleSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateSettings(settings.copy(fps = fps))
                                    }
                                    .testTag("fps_btn_$fps")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$fps FPS",
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = BorderSubtle
                    )

                    // Bitrate Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "视频码率 (Bitrate)",
                            color = TextTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${settings.bitrateMbps} Mbps",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bitrateOptions.forEach { mbps ->
                            val isSelected = settings.bitrateMbps == mbps
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else SubtleSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateSettings(
                                            settings.copy(
                                                bitrateMbps = mbps,
                                                batterySaverVbr = if (mbps >= 36) false else settings.batterySaverVbr
                                            )
                                        )
                                    }
                                    .testTag("bitrate_btn_$mbps")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$mbps Mbps",
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = BorderSubtle
                    )

                    // Video Encoder (HEVC / AVC)
                    Text(
                        text = "视频编码器",
                        color = TextTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val codecs = listOf(
                            "HEVC" to "H.265 高清高压缩",
                            "AVC" to "H.264 广泛兼容"
                        )
                        codecs.forEach { (codecKey, desc) ->
                            val isSelected = settings.videoCodec.equals(codecKey, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlueLight else SubtleSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateSettings(settings.copy(videoCodec = codecKey))
                                    }
                                    .testTag("codec_btn_${codecKey.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = codecKey,
                                        color = if (isSelected) PrimaryBlue else TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = desc,
                                        color = if (isSelected) PrimaryBlue else TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Audio Settings Card
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "音频与收音设置",
                                tint = SoftGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "音频与收音设置",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "控制游戏内部原声与麦克风开黑语音录制",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "录制游戏内部原声",
                        subtitle = "捕获游戏技能音效与背景音乐 (Android 10+)",
                        checked = settings.internalAudioEnabled,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(internalAudioEnabled = it))
                        },
                        testTag = "switch_internal_audio"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BorderSubtle
                    )

                    SettingSwitchRow(
                        title = "同时录制麦克风语音",
                        subtitle = "收录开黑队友沟通与解说人声",
                        checked = settings.micAudioEnabled,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(micAudioEnabled = it))
                        },
                        testTag = "switch_mic_audio"
                    )
                }
            }
        }

        // 6. Floating Ball & Performance Guard Card
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = "悬浮球与性能守护",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "悬浮球与性能守护",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "调节悬浮球透明度、尺寸与对局保帧策略",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Floating Ball Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "悬浮球不透明度",
                            color = TextTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(settings.floatingButtonOpacity * 100).toInt()}%",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = settings.floatingButtonOpacity.coerceIn(0.3f, 1.0f),
                        onValueChange = { newVal ->
                            onUpdateSettings(settings.copy(floatingButtonOpacity = newVal))
                        },
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Floating Ball Size
                    Text(
                        text = "悬浮球尺寸",
                        color = TextTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val sizeOptions = listOf(36 to "小巧 (36dp)", 48 to "标准 (48dp)", 60 to "醒目 (60dp)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizeOptions.forEach { (dpVal, label) ->
                            val isSelected = settings.floatingButtonSizeDp == dpVal
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PrimaryBlue else SubtleSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUpdateSettings(settings.copy(floatingButtonSizeDp = dpVal))
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) PureWhite else TextTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = BorderSubtle
                    )

                    SettingSwitchRow(
                        title = "对局保帧盾 (Zero-Lag Guard)",
                        subtitle = "将编码器置于后台低优先级线程，杜绝抢占游戏渲染算力",
                        checked = settings.gamePriorityShield,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(gamePriorityShield = it))
                        },
                        testTag = "switch_fps_guard"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BorderSubtle
                    )

                    SettingSwitchRow(
                        title = "智能动态码率 (VBR 省电模式)",
                        subtitle = "静止画面自动降低码率；关闭则使用恒定 CBR 最高画质",
                        checked = settings.batterySaverVbr,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(batterySaverVbr = it))
                        },
                        testTag = "switch_vbr_saver"
                    )
                }
            }
        }

        // 7. Storage & Cache Cleanup Card
        item {
            val usedBytes = totalStorageUsed ?: 0L
            val usedMb = String.format(Locale.US, "%.1f MB", usedBytes / (1024f * 1024f))

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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SdCard,
                                    contentDescription = "存储与缓存管理",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "存储与缓存管理",
                                    color = TextTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "已保存视频共占用：$usedMb • 应用版本 v1.5",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onClearBufferCache()
                            Toast.makeText(context, "已清理临时循环缓存分片", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SubtleSurface,
                            contentColor = TextBody
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "清理临时缓存",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "清理临时循环缓存分片",
                            color = TextTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = PureWhite,
                uncheckedTrackColor = Color(0xFFD5DCE6),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
