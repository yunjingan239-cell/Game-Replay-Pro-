package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReplayMuxer(private val context: Context) {

    private val tag = "ReplayMuxer"

    val outputDirectory: File by lazy {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies")
        File(moviesDir, "GameReplays").apply {
            if (!exists()) mkdirs()
        }
    }

    private data class ChunkTrackMeta(
        val chunk: VideoChunk,
        val file: File,
        val videoWidth: Int,
        val videoHeight: Int,
        val videoMime: String,
        val durationMs: Long
    )

    fun stitchChunks(
        chunks: List<VideoChunk>,
        gameTitle: String,
        targetDurationSeconds: Int = 60
    ): File? {
        if (chunks.isEmpty()) {
            Log.w(tag, "No video chunks provided to stitch")
            return null
        }

        val validChunks = chunks.filter { it.file.exists() && it.file.length() > 512L }
        if (validChunks.isEmpty()) {
            Log.w(tag, "All chunk files are missing or empty")
            return null
        }

        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val timePart = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(outputDirectory, "${datePart}_${timePart}_${System.currentTimeMillis() % 1000}.mp4")

        val metas = mutableListOf<ChunkTrackMeta>()
        for (vc in validChunks) {
            val file = vc.file
            val ext = MediaExtractor()
            try {
                ext.setDataSource(file.absolutePath)
                var vWidth = 0
                var vHeight = 0
                var vMime = ""
                var trackDurationUs = 0L
                for (i in 0 until ext.trackCount) {
                    val fmt = ext.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/")) {
                        vMime = mime
                        vWidth = if (fmt.containsKey(MediaFormat.KEY_WIDTH)) fmt.getInteger(MediaFormat.KEY_WIDTH) else 0
                        vHeight = if (fmt.containsKey(MediaFormat.KEY_HEIGHT)) fmt.getInteger(MediaFormat.KEY_HEIGHT) else 0
                        if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
                            trackDurationUs = fmt.getLong(MediaFormat.KEY_DURATION)
                        }
                        break
                    }
                }
                if (vMime.isNotEmpty() && vWidth > 0 && vHeight > 0) {
                    val effectiveDurationMs = maxOf(vc.durationMs, trackDurationUs / 1000L).coerceAtLeast(500L)
                    metas.add(ChunkTrackMeta(vc, file, vWidth, vHeight, vMime, effectiveDurationMs))
                }
            } catch (e: Exception) {
                Log.w(tag, "Skipping unreadable chunk ${file.name}: ${e.message}")
            } finally {
                try {
                    ext.release()
                } catch (_: Exception) {}
            }
        }

        if (metas.isEmpty()) {
            return try {
                validChunks.last().file.copyTo(outputFile, overwrite = true)
                outputFile
            } catch (_: Exception) {
                null
            }
        }

        // Group chunks by (width, height, codec). If the user briefly switched back to the Portrait app
        // (< 6s) just to tap "Save", preserve the main gameplay orientation group rather than discarding it.
        val newestMeta = metas.last()
        val newestGroup = metas.filter {
            it.videoWidth == newestMeta.videoWidth &&
                it.videoHeight == newestMeta.videoHeight &&
                it.videoMime == newestMeta.videoMime
        }
        val newestGroupDurationMs = newestGroup.sumOf { it.durationMs }

        val chosenGroup = if (newestGroupDurationMs < 6_000L && metas.size > newestGroup.size) {
            val grouped = metas.groupBy { Triple(it.videoWidth, it.videoHeight, it.videoMime) }
            grouped.values.maxByOrNull { group -> group.sumOf { it.durationMs } } ?: newestGroup
        } else {
            newestGroup
        }

        // Select the most recent chunks needed to cover targetDurationSeconds
        val targetDurationMs = targetDurationSeconds.coerceIn(5, 600) * 1000L
        val selectedMetasReversed = mutableListOf<ChunkTrackMeta>()
        var accumulatedMs = 0L
        for (meta in chosenGroup.asReversed()) {
            selectedMetasReversed.add(meta)
            accumulatedMs += meta.durationMs
            if (accumulatedMs >= targetDurationMs) {
                break
            }
        }
        val selectedMetas = selectedMetasReversed.asReversed()
        val validFiles = selectedMetas.map { it.file }

        // Fast-path: If only 1 chunk and it doesn't exceed targetDurationMs by more than 2s, copy directly
        if (validFiles.size == 1 && accumulatedMs <= targetDurationMs + 2_000L) {
            return try {
                validFiles.first().copyTo(outputFile, overwrite = true)
                Log.d(tag, "Single chunk fast-copy completed: ${outputFile.name}")
                outputFile
            } catch (e: Exception) {
                Log.e(tag, "Fast copy failed", e)
                null
            }
        }

        // If total duration of selected chunks exceeds targetDurationMs, calculate lead trim on the first chunk
        val excessLeadUs = if (accumulatedMs > targetDurationMs + 1_200L) {
            ((accumulatedMs - targetDurationMs) * 1000L).coerceAtLeast(0L)
        } else {
            0L
        }

        var muxer: MediaMuxer? = null
        val extractors = mutableListOf<MediaExtractor>()
        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            for (file in validFiles) {
                extractors.add(MediaExtractor().apply { setDataSource(file.absolutePath) })
            }

            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            // Scan from newest chunk first for pristine video and audio formats
            for (ext in extractors.asReversed()) {
                for (i in 0 until ext.trackCount) {
                    val format = ext.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/") && videoFormat == null) {
                        videoFormat = format
                    }
                    if (mime.startsWith("audio/") && audioFormat == null) {
                        audioFormat = format
                    }
                }
                if (videoFormat != null && audioFormat != null) break
            }

            if (videoFormat == null) {
                extractors.forEach { try { it.release() } catch (_: Exception) {} }
                muxer.release()
                outputFile.delete()
                validFiles.last().copyTo(outputFile, overwrite = true)
                return outputFile
            }

            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = if (audioFormat != null) muxer.addTrack(audioFormat) else -1

            muxer.start()

            // 6 MB direct buffer to safely hold ultra-high-bitrate (36-80 Mbps) 2K/native keyframes
            val bufferSize = 6 * 1024 * 1024
            val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var globalVideoPtsOffsetUs = 0L
            var globalAudioPtsOffsetUs = 0L
            var lastWrittenVideoPtsUs = -1L
            var lastWrittenAudioPtsUs = -1L

            for ((chunkIdx, extractor) in extractors.withIndex()) {
                val chunkMeta = selectedMetas[chunkIdx]
                var chunkVideoTrack = -1
                var chunkAudioTrack = -1

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/") && chunkVideoTrack == -1) {
                        chunkVideoTrack = i
                    } else if (mime.startsWith("audio/") && chunkAudioTrack == -1) {
                        chunkAudioTrack = i
                    }
                }

                // For the first chunk, determine keyframe-aligned trim point if excessLeadUs > 0
                var startTrimPtsUs = 0L
                if (chunkIdx == 0 && excessLeadUs > 0L && chunkVideoTrack != -1) {
                    val maxTrimInFirstChunkUs = ((chunkMeta.durationMs * 1000L) - 1_000_000L).coerceAtLeast(0L)
                    val desiredTrimUs = excessLeadUs.coerceAtMost(maxTrimInFirstChunkUs)
                    if (desiredTrimUs > 500_000L) {
                        extractor.selectTrack(chunkVideoTrack)
                        var basePts = -1L
                        var bestSyncPts = -1L
                        while (true) {
                            val tIdx = extractor.sampleTrackIndex
                            if (tIdx < 0) break
                            val pts = extractor.sampleTime
                            if (basePts == -1L) basePts = pts
                            val rel = (pts - basePts).coerceAtLeast(0L)
                            val isSync = (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                            if (isSync && rel <= desiredTrimUs) {
                                bestSyncPts = pts
                            }
                            if (rel > desiredTrimUs) break
                            extractor.advance()
                        }
                        if (bestSyncPts > 0L) {
                            startTrimPtsUs = bestSyncPts
                        }
                        extractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                        extractor.unselectTrack(chunkVideoTrack)
                    }
                }

                if (chunkVideoTrack != -1) {
                    extractor.selectTrack(chunkVideoTrack)
                }
                if (chunkAudioTrack != -1 && muxerAudioTrack != -1) {
                    extractor.selectTrack(chunkAudioTrack)
                }
                if (startTrimPtsUs > 0L) {
                    extractor.seekTo(startTrimPtsUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                }

                var chunkFirstVideoPtsUs = -1L
                var chunkFirstAudioPtsUs = -1L
                var chunkMaxVideoPtsUs = 0L
                var chunkMaxAudioPtsUs = 0L
                var wroteFirstVideoKeyFrameInChunk = false

                while (true) {
                    val sampleTrackIndex = extractor.sampleTrackIndex
                    if (sampleTrackIndex < 0) break

                    byteBuffer.clear()
                    val sampleSize = extractor.readSampleData(byteBuffer, 0)
                    if (sampleSize < 0) break

                    val currentPtsUs = extractor.sampleTime
                    if (startTrimPtsUs > 0L && currentPtsUs < startTrimPtsUs) {
                        extractor.advance()
                        continue
                    }

                    val rawFlags = extractor.sampleFlags
                    val isSync = (rawFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.flags = if (isSync) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

                    if (sampleTrackIndex == chunkVideoTrack && muxerVideoTrack != -1) {
                        if (!wroteFirstVideoKeyFrameInChunk) {
                            if (!isSync) {
                                extractor.advance()
                                continue
                            }
                            wroteFirstVideoKeyFrameInChunk = true
                        }
                        if (chunkFirstVideoPtsUs == -1L) {
                            chunkFirstVideoPtsUs = currentPtsUs
                        }
                        val relativePtsUs = (currentPtsUs - chunkFirstVideoPtsUs).coerceAtLeast(0L)
                        if (relativePtsUs > chunkMaxVideoPtsUs) {
                            chunkMaxVideoPtsUs = relativePtsUs
                        }
                        val candidatePtsUs = globalVideoPtsOffsetUs + relativePtsUs
                        val safePtsUs = when {
                            lastWrittenVideoPtsUs < 0L -> candidatePtsUs.coerceAtLeast(0L)
                            candidatePtsUs == lastWrittenVideoPtsUs -> lastWrittenVideoPtsUs + 100L
                            candidatePtsUs < lastWrittenVideoPtsUs && isSync -> lastWrittenVideoPtsUs + 16_666L
                            else -> candidatePtsUs
                        }
                        if (safePtsUs > lastWrittenVideoPtsUs) {
                            lastWrittenVideoPtsUs = safePtsUs
                        }
                        bufferInfo.presentationTimeUs = safePtsUs
                        muxer.writeSampleData(muxerVideoTrack, byteBuffer, bufferInfo)
                    } else if (sampleTrackIndex == chunkAudioTrack && muxerAudioTrack != -1) {
                        if (!wroteFirstVideoKeyFrameInChunk) {
                            extractor.advance()
                            continue
                        }
                        if (chunkFirstAudioPtsUs == -1L) {
                            chunkFirstAudioPtsUs = currentPtsUs
                        }
                        val relativePtsUs = (currentPtsUs - chunkFirstAudioPtsUs).coerceAtLeast(0L)
                        if (relativePtsUs > chunkMaxAudioPtsUs) {
                            chunkMaxAudioPtsUs = relativePtsUs
                        }
                        val candidatePtsUs = globalAudioPtsOffsetUs + relativePtsUs
                        val safePtsUs = when {
                            lastWrittenAudioPtsUs < 0L -> candidatePtsUs.coerceAtLeast(0L)
                            candidatePtsUs <= lastWrittenAudioPtsUs -> lastWrittenAudioPtsUs + 100L
                            else -> candidatePtsUs
                        }
                        lastWrittenAudioPtsUs = safePtsUs
                        bufferInfo.presentationTimeUs = safePtsUs
                        muxer.writeSampleData(muxerAudioTrack, byteBuffer, bufferInfo)
                    }

                    extractor.advance()
                }

                val trimmedDurationMs = if (startTrimPtsUs > 0L && chunkFirstVideoPtsUs >= 0L) {
                    val remainingMetaMs = chunkMeta.durationMs - (startTrimPtsUs / 1000L)
                    maxOf(chunkMaxVideoPtsUs / 1000L, remainingMetaMs).coerceAtLeast(500L)
                } else {
                    chunkMeta.durationMs
                }
                val chunkDurationUs = maxOf(
                    chunkMaxVideoPtsUs + 16_666L,
                    chunkMaxAudioPtsUs + 16_666L,
                    trimmedDurationMs * 1000L
                )
                val nextChunkStartPtsUs = maxOf(
                    globalVideoPtsOffsetUs + chunkDurationUs,
                    lastWrittenVideoPtsUs + 16_666L,
                    lastWrittenAudioPtsUs + 16_666L
                )
                globalVideoPtsOffsetUs = nextChunkStartPtsUs
                globalAudioPtsOffsetUs = nextChunkStartPtsUs
                extractor.release()
            }
            extractors.clear()

            muxer.stop()
            muxer.release()
            val totalSavedSec = ((lastWrittenVideoPtsUs.coerceAtLeast(0L) + 500_000L) / 1_000_000L).toInt()
            Log.d(
                tag,
                "Successfully stitched ${validFiles.size} chunks into ${outputFile.name} " +
                    "(duration=${totalSavedSec}s, target=${targetDurationSeconds}s)"
            )
            return outputFile
        } catch (e: Exception) {
            Log.e(tag, "Error during MediaMuxer stitching, using fallback copy", e)
            extractors.forEach { try { it.release() } catch (_: Exception) {} }
            try {
                muxer?.release()
            } catch (_: Exception) {}
            return try {
                validFiles.last().copyTo(outputFile, overwrite = true)
                outputFile
            } catch (err: Exception) {
                Log.e(tag, "Fallback copy failed", err)
                null
            }
        }
    }
}
