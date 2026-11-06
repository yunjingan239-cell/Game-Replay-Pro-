package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.ReplayClip
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import com.example.util.MediaExportHelper
import com.example.util.VideoThumbnailView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReplayLibraryScreen(
    clips: List<ReplayClip>,
    totalStorageUsed: Long?,
    onPlayClip: (ReplayClip) -> Unit,
    onDeleteClip: (ReplayClip) -> Unit,
    onUpdateTitle: (Long, String) -> Unit,
    onToggleFavorite: (ReplayClip) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var clipToRename by remember { mutableStateOf<ReplayClip?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var clipToDelete by remember { mutableStateOf<ReplayClip?>(null) }

    val filteredClips = clips.filter { clip ->
        val matchesTab = when (selectedTab) {
            1 -> clip.isFavorite
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
            clip.title.contains(searchQuery, ignoreCase = true) ||
            clip.gameName.contains(searchQuery, ignoreCase = true)

        matchesTab && matchesSearch
    }

    val totalStorageFormatted = formatFileSize(totalStorageUsed ?: 0L)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Header Title & Storage Summary
        Text(
            text = "视频库",
            color = TextTitle,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "已保存 ${clips.size} 个精彩瞬间 • 共占用 $totalStorageFormatted",
            color = TextMuted,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Rounded Search Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PureWhite)
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = if (searchQuery.isNotEmpty()) PrimaryBlue else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "搜索精彩瞬间或游戏名称...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除搜索",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Original Pill Filter Tabs ("全部回录" / "我的收藏")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "全部回录 (${clips.size})",
                "我的收藏 (${clips.count { it.isFavorite }})"
            )
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) PrimaryBlue else PureWhite,
                    border = if (isSelected) null else BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedTab = index }
                        .testTag("library_filter_tab_$index")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) PureWhite else TextBody,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Video List or Empty State
        if (filteredClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "暂无回录视频",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "未找到匹配的回录视频"
                        } else if (selectedTab == 1) {
                            "暂无收藏的精彩瞬间"
                        } else {
                            "暂无保存的回录视频"
                        },
                        color = TextTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "请尝试更换关键词或清除搜索条件"
                        } else {
                            "开启游戏回录后，点击悬浮球或保存按钮即可在此查看高光录像"
                        },
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredClips, key = { it.id }) { clip ->
                    ModernVideoCard(
                        clip = clip,
                        onPlay = { onPlayClip(clip) },
                        onSaveToGallery = {
                            val file = File(clip.filePath)
                            val res = MediaExportHelper.saveVideoToGallery(context, file, clip.title)
                            if (res.isSuccess) {
                                Toast.makeText(context, "已保存至手机系统相册", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = { shareClip(context, clip) },
                        onRename = {
                            clipToRename = clip
                            renameInput = clip.title
                        },
                        onDelete = { clipToDelete = clip },
                        onToggleFavorite = { onToggleFavorite(clip) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Rename Dialog
    clipToRename?.let { clip ->
        AlertDialog(
            onDismissRequest = { clipToRename = null },
            title = { Text("重命名视频", fontWeight = FontWeight.Bold, color = TextTitle, fontSize = 18.sp) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("视频名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onUpdateTitle(clip.id, renameInput.trim())
                        }
                        clipToRename = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("确认保存", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { clipToRename = null }) {
                    Text("取消", color = TextMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Delete Confirmation Dialog
    clipToDelete?.let { clip ->
        AlertDialog(
            onDismissRequest = { clipToDelete = null },
            title = { Text("确认删除视频", fontWeight = FontWeight.Bold, color = TextTitle, fontSize = 18.sp) },
            text = {
                Text(
                    text = "确定要删除「${clip.title}」吗？删除后将无法恢复。",
                    color = TextBody,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClip(clip)
                        clipToDelete = null
                        Toast.makeText(context, "视频已删除", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                    modifier = Modifier.testTag("confirm_delete_clip_btn")
                ) {
                    Text("确认删除", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { clipToDelete = null }) {
                    Text("取消", color = TextMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun ModernVideoCard(
    clip: ReplayClip,
    onPlay: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val durationFormatted = formatDuration(clip.durationSeconds)
    val sizeFormatted = formatFileSize(clip.fileSizeBytes)
    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(clip.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_card_${clip.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                VideoThumbnailView(
                    clip = clip,
                    modifier = Modifier.fillMaxSize()
                )

                // Center Blue Circle Play Button (Matches screenshot 338a58874e99e5b28bce2bc3827e7333_dc476c85.jpg)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放视频",
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bottom-Right Duration Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = durationFormatted,
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Video Metadata & 5 Action Icons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clip.title,
                        color = TextTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dateFormatted •",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$sizeFormatted • ${clip.resolution}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSaveToGallery, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "保存到相册",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (clip.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (clip.isFavorite) "取消收藏" else "收藏",
                            tint = if (clip.isFavorite) SoftRed else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "重命名",
                            tint = TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除",
                            tint = SoftRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val safeSeconds = if (seconds in 1..3600) seconds else 60
    val mins = safeSeconds / 60
    val secs = safeSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1024) {
        String.format(Locale.getDefault(), "%.1f GB", mb / 1024)
    } else {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}

private fun shareClip(context: Context, clip: ReplayClip) {
    try {
        val file = File(clip.filePath)
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享游戏精彩瞬间"))
    } catch (_: Exception) {}
}
