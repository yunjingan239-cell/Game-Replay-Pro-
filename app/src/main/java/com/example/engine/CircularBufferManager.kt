package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.model.ReplaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque

data class VideoChunk(
    val file: File,
    val sequenceNumber: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 5_000L,
    val sizeBytes: Long = 0L
)

/**
 * High-performance sliding circular video buffer manager.
 * Retains recent video chunks to cover the full user-configured replay duration
 * (e.g. 15s, 30s, 60s, 120s, 180s) even at ultra-high bitrates (36-80 Mbps),
 * and asynchronously deletes evicted chunk files on a background I/O dispatcher.
 */
class CircularBufferManager(private val context: Context) {

    private val tag = "CircularBufferManager"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queueLock = Any()

    private val bufferDir: File = File(context.cacheDir, "replay_buffer").apply {
        if (!exists()) mkdirs()
    }

    private val ringQueue = ConcurrentLinkedDeque<VideoChunk>()
    private var chunkSequence = 0L

    @Volatile
    private var targetDurationSeconds: Int = try {
        ReplaySettings.load(context).bufferDurationSeconds.coerceIn(5, 600)
    } catch (_: Throwable) {
        60
    }

    // Generous 2.5 GB scratch ceiling so 180s at 50-80 Mbps is never prematurely truncated
    private val maxDiskScratchBudgetBytes = 2_500L * 1024L * 1024L
    private val minDeviceFreeSpaceBytes = 250L * 1024L * 1024L

    init {
        purgeAllOrphanChunks()
    }

    fun createNextChunkFile(): File {
        val seq = ++chunkSequence
        return File(bufferDir, "chunk_${seq}_${System.currentTimeMillis()}.mp4")
    }

    /**
     * Dynamically updates the target replay duration window when the user changes Settings.
     * Immediately prunes excess chunks if the user reduced the buffer duration.
     */
    fun updateTargetDuration(newDurationSeconds: Int, newMaxChunks: Int) {
        val sanitizedSec = newDurationSeconds.coerceIn(5, 600)
        targetDurationSeconds = sanitizedSec
        val filesToDelete = mutableListOf<File>()
        synchronized(queueLock) {
            pruneExcessChunksLocked(newMaxChunks, sanitizedSec, filesToDelete)
        }
        if (filesToDelete.isNotEmpty()) {
            ioScope.launch {
                filesToDelete.forEach { safelyDeleteChunkFile(it) }
            }
        }
    }

    /**
     * Called when an active video chunk has finished writing.
     * Appends to ring queue and purges expired chunks beyond the configured duration window.
     */
    fun onChunkFinished(
        file: File,
        durationMs: Long,
        maxChunks: Int,
        targetDurationSec: Int = targetDurationSeconds
    ) {
        val fileLength = file.length()
        if (!file.exists() || fileLength <= 0L) {
            Log.w(tag, "Chunk file empty or missing, ignoring: ${file.name}")
            return
        }

        val sanitizedTargetSec = targetDurationSec.coerceIn(5, 600)
        targetDurationSeconds = sanitizedTargetSec
        val safeDurationMs = durationMs.coerceAtLeast(500L)

        val chunk = VideoChunk(
            file = file,
            sequenceNumber = chunkSequence,
            timestamp = System.currentTimeMillis(),
            durationMs = safeDurationMs,
            sizeBytes = fileLength
        )

        val filesToDelete = mutableListOf<File>()
        synchronized(queueLock) {
            ringQueue.addLast(chunk)
            pruneExcessChunksLocked(maxChunks, sanitizedTargetSec, filesToDelete)
        }

        Log.d(
            tag,
            "Buffer chunk added: ${chunk.file.name} (${safeDurationMs}ms). " +
                "Active chunks=${ringQueue.size}, buffered=${getBufferedDurationSeconds()}s/${sanitizedTargetSec}s"
        )

        if (filesToDelete.isNotEmpty()) {
            ioScope.launch {
                filesToDelete.forEach { safelyDeleteChunkFile(it) }
            }
        }
    }

    private fun pruneExcessChunksLocked(
        maxChunks: Int,
        targetDurationSec: Int,
        filesToDelete: MutableList<File>
    ) {
        val targetDurationMs = targetDurationSec * 1000L
        // Keep enough chunks so that cumulative duration never drops below targetDurationMs
        // (with a 400ms jitter tolerance for chunk boundary timing)
        val minKeepDurationMs = (targetDurationMs - 400L).coerceAtLeast(1000L)
        val hardMaxChunks = (maxChunks + 8).coerceAtLeast(6)

        var totalDurationMs = ringQueue.sumOf { it.durationMs }

        while (ringQueue.size > 1) {
            val oldest = ringQueue.peekFirst() ?: break
            val durationAfterRemoval = totalDurationMs - oldest.durationMs
            val exceedsDurationWindow = durationAfterRemoval >= minKeepDurationMs
            val exceedsHardChunkLimit = ringQueue.size > hardMaxChunks

            if (exceedsDurationWindow || exceedsHardChunkLimit) {
                val removed = ringQueue.pollFirst()
                if (removed != null) {
                    totalDurationMs -= removed.durationMs
                    filesToDelete.add(removed.file)
                }
            } else {
                break
            }
        }

        // Disk storage safeguard: only prune if scratch exceeds 2.5 GB or device free space < 250 MB
        var currentBytes = ringQueue.sumOf { it.sizeBytes }
        val usableSpace = try {
            bufferDir.usableSpace
        } catch (_: Throwable) {
            Long.MAX_VALUE
        }
        val isLowDiskSpace = usableSpace in 1 until minDeviceFreeSpaceBytes

        while ((currentBytes > maxDiskScratchBudgetBytes || isLowDiskSpace) && ringQueue.size > 2) {
            val removed = ringQueue.pollFirst() ?: break
            currentBytes = (currentBytes - removed.sizeBytes).coerceAtLeast(0L)
            filesToDelete.add(removed.file)
        }
    }

    private fun safelyDeleteChunkFile(file: File) {
        try {
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(tag, "Sliding buffer auto-pruned chunk: ${file.name}, deleted=$deleted")
            }
        } catch (e: Exception) {
            Log.w(tag, "Error deleting chunk file: ${file.name}", e)
        }
    }

    fun getBufferedChunks(): List<VideoChunk> {
        synchronized(queueLock) {
            return ringQueue.toList()
        }
    }

    fun getRawBufferedDurationMs(): Long {
        synchronized(queueLock) {
            return ringQueue.sumOf { it.durationMs }
        }
    }

    fun getBufferedDurationSeconds(): Int {
        val totalMs = getRawBufferedDurationMs()
        val seconds = ((totalMs + 499L) / 1000L).toInt()
        return seconds.coerceAtMost(targetDurationSeconds)
    }

    fun getBufferedSizeBytes(): Long {
        synchronized(queueLock) {
            return ringQueue.sumOf { it.sizeBytes }
        }
    }

    fun getChunkCount(): Int = ringQueue.size

    fun purgeAllOrphanChunks() {
        synchronized(queueLock) {
            ringQueue.clear()
        }
        ioScope.launch {
            try {
                bufferDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".mp4")) {
                        file.delete()
                    }
                }
                Log.d(tag, "All orphan buffer scratch chunks purged successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error purging orphan buffer scratch files", e)
            }
        }
    }
}
