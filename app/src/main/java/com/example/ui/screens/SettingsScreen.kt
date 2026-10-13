package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReplaySettings
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberSectionHeader
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RadiantCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
    val bitrateOptions = listOf(8, 12, 16, 24)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ENGINE SETTINGS",
                color = ElectricCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "ShadowPlay 商业级硬件编码与游戏感知配置",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "瞬间回录缓存时长",
                        subtitle = "设置点击保存时，向前提取多少秒的游戏高光"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { seconds ->
                            val isSelected = settings.bufferDurationSeconds == seconds
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricCyan else CyberSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
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
                                        color = if (isSelected) CyberBlack else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${seconds / 10} 分片",
                                        color = if (isSelected) CyberBlack.copy(alpha = 0.8f) else TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Commercial Grade Video Codec & Compression
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "视频压缩优化 (硬件编码芯片)",
                        subtitle = "使用底层硬件 MediaCodec，HEVC 节省高达 50% 内存与磁盘开销"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("编码压缩格式", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val codecs = listOf(
                            Pair("HEVC", "HEVC (H.265 省流50%)"),
                            Pair("AVC", "AVC (H.264 经典兼容)")
                        )
                        codecs.forEach { (codecKey, label) ->
                            val isSelected = settings.videoCodec.contains(codecKey, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricCyan else CyberSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateSettings(settings.copy(videoCodec = codecKey))
                                    }
                            ) {
                                Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) CyberBlack else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("多分辨率规格", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val row1 = listOf(
                                Triple(1280, 720, "720P 极速省电"),
                                Triple(1920, 1080, "1080P 电竞主流")
                            )
                            row1.forEach { (w, h, label) ->
                                val isSelected = settings.resolutionWidth == w
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ElectricCyan else CyberSurfaceElevated,
                                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onUpdateSettings(
                                                settings.copy(
                                                    resolutionWidth = w,
                                                    resolutionHeight = h,
                                                    resolutionLabel = label
                                                )
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) CyberBlack else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
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
                                Triple(2560, 1440, "2K 旗舰超清"),
                                Triple(2400, 1080, "原生屏幕对齐")
                            )
                            row2.forEach { (w, h, label) ->
                                val isSelected = settings.resolutionWidth == w
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ElectricCyan else CyberSurfaceElevated,
                                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onUpdateSettings(
                                                settings.copy(
                                                    resolutionWidth = w,
                                                    resolutionHeight = h,
                                                    resolutionLabel = label
                                                )
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) CyberBlack else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("录制帧率 (FPS) - 支持高刷电竞", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        fpsOptions.forEach { fps ->
                            val isSelected = settings.fps == fps
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricCyan else CyberSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateSettings(settings.copy(fps = fps))
                                    }
                            ) {
                                Text(
                                    text = "$fps FPS",
                                    color = if (isSelected) CyberBlack else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("码率动态分配 (Mbps)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bitrateOptions.forEach { mbps ->
                            val isSelected = settings.bitrateMbps == mbps
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricCyan else CyberSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateSettings(settings.copy(bitrateMbps = mbps))
                                    }
                            ) {
                                Text(
                                    text = "$mbps M",
                                    color = if (isSelected) CyberBlack else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto Orientation Detection Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("自动识别横竖屏", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("根据游戏横屏/竖屏自动动态切换宽高，杜绝画面拉伸黑边", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.autoDetectOrientation,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoDetectOrientation = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // HDR 10-bit Color Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HDR10 10-bit 广色域支持", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("启用 BT.2020 广色域硬件录制，如设备不支持将自动回退 SDR", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.hdrEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(hdrEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Game Launch Detection Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("游戏启动自动检测感知", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("类似手机游戏助手，扫描设备安装游戏并提供对局一键就绪", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.gameLaunchDetectionEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(gameLaunchDetectionEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }
                }
            }
        }

        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "音频采集通道",
                        subtitle = "支持游戏内声音内录与麦克风解说录音"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Game Audio", tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("游戏内原生音频 (系统内录)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("清晰录制技能音效与对局战况", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = settings.internalAudioEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(internalAudioEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = "Mic", tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("玩家麦克风解说录制", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("同步录入玩家语音与开黑聊天", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = settings.micAudioEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(micAudioEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }
                }
            }
        }

        // Floating Button Tweaks
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "悬浮球动画与外观调节",
                        subtitle = "半透明电竞呼吸灯设计，支持自由调节尺寸与收起隐藏"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("显示悬浮控制球", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = settings.floatingButtonEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(floatingButtonEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Size Selection: 36dp (小) / 48dp (中) / 60dp (大)
                    Text("悬浮球尺寸大小", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
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
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricCyan else CyberSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) ElectricCyan else CyberBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateSettings(settings.copy(floatingButtonSizeDp = size))
                                    }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyberBlack else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("半透明度设置", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "${(settings.floatingButtonOpacity * 100).toInt()}%",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.floatingButtonOpacity,
                        onValueChange = { onUpdateSettings(settings.copy(floatingButtonOpacity = it)) },
                        valueRange = 0.25f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = CyberBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Collapsed mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("贴边自动收起隐藏", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("隐藏为屏幕边缘细小指示条，防误触防遮挡", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.isFloatingButtonCollapsed,
                            onCheckedChange = { onUpdateSettings(settings.copy(isFloatingButtonCollapsed = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }
                }
            }
        }

        // Shortcut Triggers Card
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "快捷保存触发方式",
                        subtitle = "除点击悬浮球外，支持物理音量组合键极速截取"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("音量键快捷触发保存", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("对局中双击【音量下键】无需看屏幕，盲按即可截取高光", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.volumeKeyShortcutEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(volumeKeyShortcutEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = ElectricCyan)
                        )
                    }
                }
            }
        }

        // Auto Naming Rule Card
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "视频自动命名规范",
                        subtitle = "保存时自动生成规范化高光视频文件名"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurfaceDark,
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("命名规范模板：", color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "游戏名称_YYYYMMDD_HHMMSS.mp4",
                                color = ElectricCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "例如：${settings.currentGameTitle}_20261012_201530.mp4",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CyberSectionHeader(
                        title = "存储占用与缓存管理",
                        subtitle = "环形缓存定期自动回收，保证不耗尽手机存储"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("回录视频库占用", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = formatFileSize(totalStorageUsed ?: 0L),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("缓存存储路径", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = "Movies/GameReplays",
                                color = ElectricCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onClearBufferCache()
                            Toast.makeText(context, "已清理环形缓存临时碎片", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Clean Cache", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清理历史临时缓存碎片")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
