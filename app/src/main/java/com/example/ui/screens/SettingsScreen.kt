package com.example.ui.screens

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReplaySettings
import com.example.ui.components.CompactSegmentedControl
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
import com.example.ui.theme.SubtleSurface
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import kotlinx.coroutines.delay
import java.util.Locale

private data class DeviceEncoderCapability(
    val supports2K: Boolean,
    val supports90Fps: Boolean,
    val supports120Fps: Boolean,
    val maxDisplayRefreshRate: Int
)

private enum class QualityPreset(val label: String, val desc: String) {
    SAVER("省空间", "720p · 16 Mbps · 30 FPS"),
    HD("高清", "1080p · 24 Mbps · 60 FPS"),
    ULTRA("极致画质", "原生分辨率直通 · 36 Mbps · 60 FPS"),
    CUSTOM("自定义", "手动组合分辨率、码率与帧率")
}

private data class ResolutionOption(
    val key: String,
    val label: String,
    val width: Int,
    val height: Int,
    val description: String,
    val requires2KSupport: Boolean = false
)

@Composable
fun SettingsScreen(
    settings: ReplaySettings,
    totalStorageUsed: Long?,
    onUpdateSettings: (ReplaySettings) -> Unit,
    onClearBufferCache: () -> Unit,
    onToggleFloatingBallPreview: () -> Unit = {},
    isFloatingBallPreviewVisible: Boolean = false
) {
    val context = LocalContext.current

    // Hardware capability inspection + user toggle to preview unsupported-device disabled states
    val hardwareCapability = remember { inspectDeviceEncoderCapability(context) }
    var simulateStandardDeviceLimit by remember { mutableStateOf(false) }

    val supports2K = !simulateStandardDeviceLimit && hardwareCapability.supports2K
    val supports120Fps = !simulateStandardDeviceLimit && hardwareCapability.supports120Fps

    var expandedParameterRow by remember { mutableStateOf<String?>(null) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var feedbackBannerText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedbackBannerText) {
        if (feedbackBannerText != null) {
            delay(2400)
            feedbackBannerText = null
        }
    }

    fun applySettingsWithFeedback(newSettings: ReplaySettings, message: String) {
        onUpdateSettings(newSettings)
        feedbackBannerText = message
    }

    // Normalize current resolution key
    val currentResolutionKey = remember(settings.resolutionLabel, settings.resolutionHeight) {
        when {
            settings.resolutionLabel.contains("原生") -> "NATIVE"
            settings.resolutionLabel.contains("2K") || settings.resolutionHeight >= 1440 -> "2K"
            settings.resolutionLabel.contains("720") || settings.resolutionHeight <= 720 -> "720P"
            else -> "1080P"
        }
    }

    // Determine active preset automatically based on current parameters
    val currentPreset = remember(currentResolutionKey, settings.bitrateMbps, settings.fps) {
        when {
            currentResolutionKey == "720P" && settings.bitrateMbps == 16 && settings.fps == 30 -> QualityPreset.SAVER
            currentResolutionKey == "1080P" && settings.bitrateMbps == 24 && settings.fps == 60 -> QualityPreset.HD
            currentResolutionKey == "NATIVE" && settings.bitrateMbps == 36 && settings.fps == 60 -> QualityPreset.ULTRA
            else -> QualityPreset.CUSTOM
        }
    }

    val durationOptions = listOf(30, 60, 120, 180)
    val bitrateOptions = listOf(16, 24, 36, 50)
    val fpsOptions = listOf(30, 60, 90, 120)
    val resolutionOptions = remember {
        listOf(
            ResolutionOption(
                key = "NATIVE",
                label = "原生分辨率直通",
                width = 2400,
                height = 1080,
                description = "1:1 匹配手机物理屏幕像素，无缩放模糊，画质最锐利"
            ),
            ResolutionOption(
                key = "1080P",
                label = "1080p",
                width = 1920,
                height = 1080,
                description = "主流高清标准，兼顾清晰度与文件体积"
            ),
            ResolutionOption(
                key = "720P",
                label = "720p",
                width = 1280,
                height = 720,
                description = "轻量省空间，适合长时间挂机与低存储设备"
            ),
            ResolutionOption(
                key = "2K",
                label = "2K",
                width = 2560,
                height = 1440,
                description = "超高清画幅，仅限支持 1440P 硬件编码的设备",
                requires2KSupport = true
            )
        )
    }

    val currentResolutionOption = resolutionOptions.find { it.key == currentResolutionKey } ?: resolutionOptions[0]

    // Estimate single clip file size in MB based on bitrate & buffer duration
    val estimatedClipSizeMb = remember(settings.bitrateMbps, settings.bufferDurationSeconds) {
        ((settings.bitrateMbps * settings.bufferDurationSeconds) / 8f).toInt().coerceAtLeast(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Page Header + Restore Defaults Action
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "设置",
                        color = TextTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "回录参数、画质预设与悬浮球配置",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showResetConfirmDialog = true }
                        .testTag("restore_defaults_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "恢复默认设置",
                            tint = TextBody,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "恢复默认",
                            color = TextBody,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Save Feedback Banner
        item {
            AnimatedVisibility(visible = feedbackBannerText != null) {
                feedbackBannerText?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftGreenBg,
                        border = BorderStroke(1.dp, SoftGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SoftGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = SoftGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Recording Presets Card (省空间 / 高清 / 极致画质 + 自定义联动状态)
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
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "录制预设",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "录制画质预设",
                                    color = TextTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentPreset.desc,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Current Preset Badge (Highlights "自定义" when user tweaks individual parameters)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentPreset == QualityPreset.CUSTOM) SoftOrangeBg else PrimaryBlueLight
                        ) {
                            Text(
                                text = currentPreset.label,
                                color = if (currentPreset == QualityPreset.CUSTOM) SoftOrange else PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val presetItems = listOf(QualityPreset.SAVER, QualityPreset.HD, QualityPreset.ULTRA)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetItems.forEach { preset ->
                            val isSelected = currentPreset == preset
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlueLight else SubtleSurface,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (preset) {
                                            QualityPreset.SAVER -> {
                                                applySettingsWithFeedback(
                                                    settings.copy(
                                                        resolutionWidth = 1280,
                                                        resolutionHeight = 720,
                                                        resolutionLabel = "720p",
                                                        bitrateMbps = 16,
                                                        fps = 30
                                                    ),
                                                    "已应用「省空间」预设：720p · 16 Mbps · 30 FPS"
                                                )
                                            }
                                            QualityPreset.HD -> {
                                                applySettingsWithFeedback(
                                                    settings.copy(
                                                        resolutionWidth = 1920,
                                                        resolutionHeight = 1080,
                                                        resolutionLabel = "1080p",
                                                        bitrateMbps = 24,
                                                        fps = 60
                                                    ),
                                                    "已应用「高清」预设：1080p · 24 Mbps · 60 FPS"
                                                )
                                            }
                                            QualityPreset.ULTRA -> {
                                                applySettingsWithFeedback(
                                                    settings.copy(
                                                        resolutionWidth = 2400,
                                                        resolutionHeight = 1080,
                                                        resolutionLabel = "原生分辨率直通",
                                                        bitrateMbps = 36,
                                                        fps = 60,
                                                        batterySaverVbr = false
                                                    ),
                                                    "已应用「极致画质」预设：原生分辨率直通 · 36 Mbps · 60 FPS"
                                                )
                                            }
                                            else -> {}
                                        }
                                    }
                                    .testTag("preset_option_${preset.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = preset.label,
                                        color = if (isSelected) PrimaryBlue else TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (preset) {
                                            QualityPreset.SAVER -> "720p · 30帧"
                                            QualityPreset.HD -> "1080p · 60帧"
                                            QualityPreset.ULTRA -> "原生 · 36M"
                                            else -> ""
                                        },
                                        color = if (isSelected) PrimaryBlue else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Core Recording Parameters Card (No repetitive rectangle button stacks)
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
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "核心回录参数",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "回录核心参数",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "预计单次保存耗时 <0.5 秒 • 约占 ${estimatedClipSizeMb} MB/段",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // A. Buffer Duration (Compact Segmented Selector: 30s / 60s / 120s / 180s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "缓存时长",
                            color = TextTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "当前：${settings.bufferDurationSeconds} 秒",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "后台自动循环覆盖旧画面，仅在点击保存时写入视频文件",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val selectedDurationIndex = durationOptions.indexOf(settings.bufferDurationSeconds).coerceAtLeast(0)
                    CompactSegmentedControl(
                        options = durationOptions.map { "${it} 秒" },
                        selectedIndex = selectedDurationIndex,
                        onOptionSelected = { idx ->
                            val newDuration = durationOptions[idx]
                            applySettingsWithFeedback(
                                settings.copy(bufferDurationSeconds = newDuration),
                                "缓存时长已更新为 ${newDuration} 秒"
                            )
                        },
                        testTagPrefix = "duration_segment"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = BorderSubtle
                    )

                    // B. Video Resolution (Current Value Row + Expandable Compact Selector)
                    val isResExpanded = expandedParameterRow == "RESOLUTION"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    expandedParameterRow = if (isResExpanded) null else "RESOLUTION"
                                }
                                .padding(vertical = 4.dp)
                                .testTag("setting_row_resolution"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "视频分辨率",
                                    color = TextTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentResolutionOption.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PrimaryBlueLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentResolutionOption.label,
                                        color = PrimaryBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isResExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "展开选择",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isResExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                resolutionOptions.forEach { option ->
                                    val isOptionSupported = !option.requires2KSupport || supports2K
                                    val isSelected = currentResolutionOption.key == option.key
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            !isOptionSupported -> SubtleSurface.copy(alpha = 0.6f)
                                            isSelected -> PrimaryBlueLight
                                            else -> SubtleSurface
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected && isOptionSupported) PrimaryBlue else BorderSubtle
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = isOptionSupported) {
                                                expandedParameterRow = null
                                                applySettingsWithFeedback(
                                                    settings.copy(
                                                        resolutionWidth = option.width,
                                                        resolutionHeight = option.height,
                                                        resolutionLabel = option.label
                                                    ),
                                                    "视频分辨率已切换为：${option.label}"
                                                )
                                            }
                                            .testTag("res_option_${option.key.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = option.label,
                                                        color = when {
                                                            !isOptionSupported -> TextLight
                                                            isSelected -> PrimaryBlue
                                                            else -> TextTitle
                                                        },
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (!isOptionSupported) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = BorderSubtle
                                                        ) {
                                                            Text(
                                                                text = "设备不支持",
                                                                color = TextMuted,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = option.description,
                                                    color = if (isOptionSupported) TextMuted else TextLight,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (isSelected && isOptionSupported) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "已选中",
                                                    tint = PrimaryBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = BorderSubtle
                    )

                    // C. Video Bitrate (16 Mbps / 24 Mbps / 36 Mbps / 50 Mbps)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "视频码率",
                                    color = TextTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "码率越高，高速团战画面细节越清晰、无块状噪点",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "${settings.bitrateMbps} Mbps",
                                color = PrimaryBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val selectedBitrateIdx = bitrateOptions.indexOf(settings.bitrateMbps).let {
                            if (it >= 0) it else 2
                        }
                        CompactSegmentedControl(
                            options = bitrateOptions.map { "${it} Mbps" },
                            selectedIndex = selectedBitrateIdx,
                            onOptionSelected = { idx ->
                                val mbps = bitrateOptions[idx]
                                applySettingsWithFeedback(
                                    settings.copy(
                                        bitrateMbps = mbps,
                                        batterySaverVbr = if (mbps >= 36) false else settings.batterySaverVbr
                                    ),
                                    "视频码率已更新为 $mbps Mbps"
                                )
                            },
                            testTagPrefix = "bitrate_segment"
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = BorderSubtle
                    )

                    // D. Recording Frame Rate (30 FPS / 60 FPS / 90 FPS / 120 FPS with Hardware Capability Check)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "录制帧率",
                                    color = TextTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "高帧率使镜头平移更丝滑，需设备屏幕与编码器支持",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fpsOptions.forEach { fpsVal ->
                                val isSupported = fpsVal < 120 || supports120Fps
                                val isSelected = settings.fps == fpsVal
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        !isSupported -> SubtleSurface.copy(alpha = 0.6f)
                                        isSelected -> PrimaryBlue
                                        else -> SubtleSurface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected && isSupported) PrimaryBlue else BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = isSupported) {
                                            applySettingsWithFeedback(
                                                settings.copy(fps = fpsVal),
                                                "录制帧率已更新为 $fpsVal FPS"
                                            )
                                        }
                                        .testTag("fps_option_$fpsVal")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$fpsVal FPS",
                                            color = when {
                                                !isSupported -> TextLight
                                                isSelected -> PureWhite
                                                else -> TextTitle
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!isSupported) {
                                            Text(
                                                text = "不支持",
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Performance & Storage Notice for high-bitrate / high-FPS / 2K
                        if (settings.fps >= 90 || settings.bitrateMbps >= 36 || currentResolutionKey == "2K") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftOrangeBg,
                                border = BorderStroke(1.dp, SoftOrange.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "性能与存储提示",
                                        tint = SoftOrange,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "性能与存储提示：当前启用高规格录制（${currentResolutionOption.label} · ${settings.bitrateMbps}Mbps · ${settings.fps}FPS），单段视频约占 ${estimatedClipSizeMb}MB，长时间对局机身功耗会小幅上升。",
                                        color = TextBody,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Floating Ball & Storage Management Card
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
                                    contentDescription = "悬浮球与存储",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "悬浮球与存储",
                                    color = TextTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "自动贴边半透明 • 已用空间 ${formatStorage(totalStorageUsed ?: 0L)}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFloatingBallPreviewVisible) PrimaryBlue else PrimaryBlueLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleFloatingBallPreview() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonChecked,
                                    contentDescription = null,
                                    tint = if (isFloatingBallPreviewVisible) PureWhite else PrimaryBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFloatingBallPreviewVisible) "关闭预览" else "预览悬浮球",
                                    color = if (isFloatingBallPreviewVisible) PureWhite else PrimaryBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Show Floating Button Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("显示游戏悬浮球", color = TextTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("支持自由拖动、贴边隐身与展开保存", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.floatingButtonEnabled,
                            onCheckedChange = {
                                applySettingsWithFeedback(
                                    settings.copy(floatingButtonEnabled = it),
                                    if (it) "已开启游戏悬浮球" else "已关闭游戏悬浮球"
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = PureWhite,
                                uncheckedTrackColor = Color(0xFFD8E0EA)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("悬浮球不透明度", color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Size Selection using CompactSegmentedControl
                    val sizeValues = listOf(36, 48, 60)
                    val selectedSizeIdx = sizeValues.indexOf(settings.floatingButtonSizeDp).let { if (it >= 0) it else 1 }
                    CompactSegmentedControl(
                        options = listOf("小尺寸 (36dp)", "标准 (48dp)", "大尺寸 (60dp)"),
                        selectedIndex = selectedSizeIdx,
                        onOptionSelected = { idx ->
                            val newSize = sizeValues[idx]
                            applySettingsWithFeedback(
                                settings.copy(floatingButtonSizeDp = newSize),
                                "悬浮球尺寸已调整为 ${newSize}dp"
                            )
                        },
                        testTagPrefix = "float_size_segment"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = BorderSubtle
                    )

                    // Storage Info & Cache Clean Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "本地保存路径：Movies/GameReplays",
                                color = TextTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "已保存视频占用 ${formatStorage(totalStorageUsed ?: 0L)}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SubtleSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onClearBufferCache()
                                    feedbackBannerText = "已清理历史临时缓存分片"
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "清理临时缓存",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "清理缓存碎片",
                                    color = TextTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Collapsible Advanced Settings Section (Keeps secondary options quiet)
        item {
            CyberCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                            .padding(vertical = 2.dp)
                            .testTag("toggle_advanced_settings_btn"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "高级设置",
                                color = TextTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "音频音源、编码格式 (${settings.videoCodec})、设备兼容检查与温控策略",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "展开或收起高级设置",
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isAdvancedExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Video Codec Selector
                            Text(
                                text = "视频编码格式",
                                color = TextTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val isHevc = settings.videoCodec.contains("HEVC", ignoreCase = true)
                            CompactSegmentedControl(
                                options = listOf("HEVC (H.265 高效推荐)", "AVC (H.264 广泛兼容)"),
                                selectedIndex = if (isHevc) 0 else 1,
                                onOptionSelected = { idx ->
                                    val codec = if (idx == 0) "HEVC" else "AVC"
                                    applySettingsWithFeedback(
                                        settings.copy(videoCodec = codec),
                                        "视频编码格式已切换为 $codec"
                                    )
                                },
                                testTagPrefix = "codec_segment"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Audio Switches
                            SettingToggleRow(
                                title = "录制游戏内部原声",
                                subtitle = "直接捕获系统内部游戏音效，隔绝外部环境杂音",
                                checked = settings.internalAudioEnabled,
                                onCheckedChange = {
                                    applySettingsWithFeedback(
                                        settings.copy(internalAudioEnabled = it),
                                        if (it) "已开启游戏原声内录" else "已关闭游戏原声内录"
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SettingToggleRow(
                                title = "同时录制麦克风语音",
                                subtitle = "录入玩家实时解说或开黑语音",
                                checked = settings.micAudioEnabled,
                                onCheckedChange = {
                                    applySettingsWithFeedback(
                                        settings.copy(micAudioEnabled = it),
                                        if (it) "已开启麦克风同步录制" else "已关闭麦克风录制"
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SettingToggleRow(
                                title = "对局保帧盾 (CPU 大核优先游戏)",
                                subtitle = "后台录制自动让渡高算力核心，保障团战不掉帧",
                                checked = settings.gamePriorityShield,
                                onCheckedChange = {
                                    applySettingsWithFeedback(
                                        settings.copy(gamePriorityShield = it),
                                        if (it) "已开启对局保帧盾" else "已关闭对局保帧盾"
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SettingToggleRow(
                                title = "智能温控防降频守护",
                                subtitle = "机身持续发热时平滑调节编码负载",
                                checked = settings.autoThermalGuard,
                                onCheckedChange = {
                                    applySettingsWithFeedback(
                                        settings.copy(autoThermalGuard = it),
                                        if (it) "已开启智能温控守护" else "已关闭智能温控守护"
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Device Capability Check & Simulation Toggle (As required by spec: reserve device capability check & disabled state for 2K/120FPS)
                            SettingToggleRow(
                                title = "模拟中端机型硬件能力限制",
                                subtitle = "开启后模拟不支持 2K 与 120 FPS 的机型禁用状态（当前真机刷新率上限：${hardwareCapability.maxDisplayRefreshRate}Hz）",
                                checked = simulateStandardDeviceLimit,
                                onCheckedChange = { limitEnabled ->
                                    simulateStandardDeviceLimit = limitEnabled
                                    var updated = settings
                                    if (limitEnabled) {
                                        if (currentResolutionKey == "2K") {
                                            updated = updated.copy(
                                                resolutionWidth = 1920,
                                                resolutionHeight = 1080,
                                                resolutionLabel = "1080p"
                                            )
                                        }
                                        if (updated.fps >= 120) {
                                            updated = updated.copy(fps = 60)
                                        }
                                        applySettingsWithFeedback(
                                            updated,
                                            "已开启机型能力受限模拟：2K 与 120 FPS 已置为不支持状态"
                                        )
                                    } else {
                                        feedbackBannerText = "已恢复本机真实硬件能力检测"
                                    }
                                }
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

    // Restore Defaults Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "恢复默认设置",
                    color = TextTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要将缓存时长、分辨率、码率、帧率及悬浮球参数全部恢复为推荐默认值吗？",
                    color = TextBody,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        simulateStandardDeviceLimit = false
                        val defaultSettings = ReplaySettings(currentGameTitle = settings.currentGameTitle)
                        applySettingsWithFeedback(
                            defaultSettings,
                            "已恢复全部默认回录参数（原生分辨率直通 · 36 Mbps · 60 FPS · 60 秒）"
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("confirm_restore_defaults_btn")
                ) {
                    Text("确认恢复", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("取消", color = TextMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                fontWeight = FontWeight.Medium
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
                uncheckedTrackColor = Color(0xFFD8E0EA)
            )
        )
    }
}

private fun inspectDeviceEncoderCapability(context: Context): DeviceEncoderCapability {
    var supports2K = true
    var supports120Fps = true
    var maxRefreshRate = 60

    try {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { context.display } catch (_: Exception) { wm?.defaultDisplay }
        } else {
            wm?.defaultDisplay
        }
        if (display != null) {
            maxRefreshRate = display.supportedModes.maxOfOrNull { it.refreshRate.toInt() }
                ?: display.refreshRate.toInt()
        }
    } catch (_: Exception) {}

    try {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val videoCap = codecList.codecInfos
            .filter { it.isEncoder }
            .firstOrNull { info ->
                info.supportedTypes.any {
                    it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) ||
                        it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)
                }
            }
            ?.let { info ->
                val mime = info.supportedTypes.first {
                    it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) ||
                        it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)
                }
                info.getCapabilitiesForType(mime)?.videoCapabilities
            }

        if (videoCap != null) {
            supports2K = videoCap.isSizeSupported(2560, 1440) || videoCap.isSizeSupported(1440, 2560)
            supports120Fps = videoCap.supportedFrameRates.upper >= 90
        }
    } catch (_: Exception) {}

    return DeviceEncoderCapability(
        supports2K = supports2K,
        supports90Fps = true,
        supports120Fps = supports120Fps,
        maxDisplayRefreshRate = maxRefreshRate.coerceAtLeast(60)
    )
}

private fun formatStorage(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1024) {
        String.format(Locale.getDefault(), "%.1f GB", mb / 1024)
    } else {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}
