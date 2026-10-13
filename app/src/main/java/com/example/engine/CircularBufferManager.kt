package com.example.engine

import android.content.Context
import android.util.Log
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
    val durationMs: Long = 10_000L
)

/**
 * High-performance sliding circular video buffer manager.
 * Guarantees bounded memory and storage footprint by maintaining a strict ring queue of recent chunks
 * and asynchronously pruning obsolete chunks on a background I/O dispatcher without blocking encoding threads.
 */
class CircularBufferManager(private val context: Context) {

    private val tag = "CircularBufferManager"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bufferDir: File = File(context.cacheDir, "replay_buffer").apply {
        if (!exists()) mkdirs()
    }

    private val ringQueue = ConcurrentLinkedDeque<VideoChunk>()
    private var chunkSequence = 0L

    // Max disk scratch budget for circular buffer: 120 MB
    private val maxDiskScratchBudgetBytes = 120 * 1024 * 1024L

    init {
        purgeAllOrphanChunks()
    }

    fun createNextChunkFile(): File {
        val seq = ++chunkSequence
        return File(bufferDir, "chunk_${seq}_${System.currentTimeMillis()}.mp4")
    }

    /**
     * Called when an active video chunk has finished writing.
     * Appends to ring queue and purges expired chunks beyond the sliding window.
     */
    fun onChunkFinished(file: File, durationMs: Long, maxChunks: Int) {
        if (!file.exists() || file.length() == 0L) {
            Log.w(tag, "Chunk file empty or missing, ignoring: ${file.name}")
            return
        }

        val chunk = VideoChunk(
            file = file,
            sequenceNumber = chunkSequence,
            timestamp = System.currentTimeMillis(),
            durationMs = durationMs
        )
        ringQueue.addLast(chunk)
        Log.d(tag, "Buffer sliding queue added: ${chunk.file.name}. Size: ${ringQueue.size}/$maxChunks (${durationMs}ms)")

        // Asynchronously prune chunks that exceed the rolling time window or disk budget
        ioScope.launch {
            pruneExcessChunks(maxChunks)
        }
    }

    private fun pruneExcessChunks(maxChunks: Int) {
        // 1. Sliding window time limit
        while (ringQueue.size > maxChunks) {
            val oldest = ringQueue.pollFirst()
            if (oldest != null) {
                safelyDeleteChunkFile(oldest.file)
            }
        }

        // 2. Strict disk storage quota safeguard
        var currentBytes = getBufferedSizeBytes()
        while (currentBytes > maxDiskScratchBudgetBytes && ringQueue.size > 2) {
            val oldest = ringQueue.pollFirst()
            if (oldest != null) {
                safelyDeleteChunkFile(oldest.file)
                currentBytes = getBufferedSizeBytes()
            }
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
        return ringQueue.toList()
    }

    fun getBufferedDurationSeconds(): Int {
        val totalMs = ringQueue.sumOf { it.durationMs }
        return (totalMs / 1000).toInt()
    }

    fun getBufferedSizeBytes(): Long {
        return ringQueue.sumOf { if (it.file.exists()) it.file.length() else 0L }
    }

    fun getChunkCount(): Int = ringQueue.size

    fun purgeAllOrphanChunks() {
        ringQueue.clear()
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
