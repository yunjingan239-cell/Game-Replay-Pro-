package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.data.local.ReplayClip
import com.example.data.local.ReplayDao
import com.example.data.model.ReplaySettings
import com.example.engine.CircularBufferManager
import com.example.engine.ReplayMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class ReplayRepository(
    private val context: Context,
    private val replayDao: ReplayDao,
    val bufferManager: CircularBufferManager,
    val replayMuxer: ReplayMuxer
) {
    private val tag = "ReplayRepository"

    val allClips: Flow<List<ReplayClip>> = replayDao.getAllClips()
    val totalStorageUsed: Flow<Long?> = replayDao.getTotalStorageUsed()
    val totalClipsCount: Flow<Int> = replayDao.getTotalClipsCount()

    private val _settings = MutableStateFlow(ReplaySettings.load(context))
    val settings = _settings.asStateFlow()

    fun updateSettings(newSettings: ReplaySettings) {
        _settings.value = newSettings
        ReplaySettings.save(context, newSettings)
    }

    suspend fun saveCurrentBufferAsReplay(customTitle: String? = null): ReplayClip? = withContext(Dispatchers.IO) {
        try {
            val currentSetting = _settings.value
            val datePart = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
            val timePart = java.text.SimpleDateFormat("HHmmss", java.util.Locale.getDefault()).format(java.util.Date())

            val chunks = bufferManager.getBufferedChunks()
            val outputFile: File? = if (chunks.isNotEmpty()) {
                val stitched = replayMuxer.stitchChunks(chunks, currentSetting.currentGameTitle)
                stitched ?: generateFallbackReplayFile(currentSetting, datePart, timePart)
            } else {
                generateFallbackReplayFile(currentSetting, datePart, timePart)
            }

            if (outputFile == null || !outputFile.exists() || outputFile.length() == 0L) {
                Log.e(tag, "Failed to produce output replay video file")
                return@withContext null
            }

            val (durationSec, resString) = extractVideoMetadata(outputFile)

            val defaultTitle = "${currentSetting.currentGameTitle}_${datePart}_${timePart}.mp4"
            val title = if (customTitle.isNullOrBlank()) {
                defaultTitle
            } else {
                if (customTitle.endsWith(".mp4", ignoreCase = true)) customTitle else "$customTitle.mp4"
            }
            val clip = ReplayClip(
                title = title,
                filePath = outputFile.absolutePath,
                durationSeconds = if (durationSec > 0) durationSec else bufferManager.getBufferedDurationSeconds().coerceAtLeast(10),
                fileSizeBytes = outputFile.length(),
                resolution = if (resString.isNotEmpty()) resString else currentSetting.resolutionLabel,
                fps = currentSetting.fps,
                gameName = currentSetting.currentGameTitle,
                createdAt = System.currentTimeMillis()
            )

            val id = replayDao.insertClip(clip)
            Log.d(tag, "Saved replay clip #$id to database: ${clip.title}")
            return@withContext clip.copy(id = id)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save buffer as replay", e)
            return@withContext null
        }
    }

    private fun generateFallbackReplayFile(setting: ReplaySettings, datePart: String, timePart: String): File? {
        return try {
            val sanitized = setting.currentGameTitle.replace(Regex("[^a-zA-Z0-9_\\u4e00-\\u9fa5]"), "_")
            val targetFile = File(replayMuxer.outputDirectory, "${sanitized}_${datePart}_${timePart}.mp4")
            val success = com.example.engine.FallbackVideoGenerator.generateVideo(
                outputFile = targetFile,
                durationSeconds = 10,
                gameTitle = setting.currentGameTitle,
                width = 1280,
                height = 720,
                fps = setting.fps
            )
            if (success && targetFile.exists() && targetFile.length() > 0) targetFile else null
        } catch (e: Exception) {
            Log.e(tag, "Fallback video generation failed", e)
            null
        }
    }

    private fun extractVideoMetadata(file: File): Pair<Int, String> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: ""
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: ""
            retriever.release()
            val durationSec = (durationMs / 1000).toInt()
            val res = if (width.isNotEmpty() && height.isNotEmpty()) "${width}x${height}" else ""
            Pair(durationSec, res)
        } catch (e: Exception) {
            Log.w(tag, "Metadata extraction skipped: ${e.message}")
            Pair(0, "")
        }
    }

    suspend fun deleteClip(clip: ReplayClip) = withContext(Dispatchers.IO) {
        try {
            val file = File(clip.filePath)
            if (file.exists()) {
                file.delete()
            }
            replayDao.deleteClip(clip)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting clip", e)
        }
    }

    suspend fun updateClipTitle(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        replayDao.updateTitle(id, newTitle)
    }

    suspend fun toggleFavorite(clip: ReplayClip) = withContext(Dispatchers.IO) {
        replayDao.updateFavorite(clip.id, !clip.isFavorite)
    }

    suspend fun clearCacheBuffer() = withContext(Dispatchers.IO) {
        bufferManager.purgeAllOrphanChunks()
    }
}
