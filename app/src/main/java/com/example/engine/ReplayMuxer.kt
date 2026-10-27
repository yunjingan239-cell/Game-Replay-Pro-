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
        val file: File,
        val videoWidth: Int,
        val videoHeight: Int,
        val videoMime: String
    )

    fun stitchChunks(chunks: List<VideoChunk>, gameTitle: String): File? {
        if (chunks.isEmpty()) {
            Log.w(tag, "No video chunks provided to stitch")
            return null
        }

        val candidateFiles = chunks.map { it.file }.filter { it.exists() && it.length() > 512L }
        if (candidateFiles.isEmpty()) {
            Log.w(tag, "All chunk files are missing or empty")
            return null
        }

        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val timePart = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(outputDirectory, "${datePart}_${timePart}_${System.currentTimeMillis() % 1000}.mp4")

        // Filter chunks so we only stitch chunks matching the NEWEST chunk's resolution & codec
        // (prevents mixing Portrait app lobby chunks with Landscape in-game chunks)
        val metas = mutableListOf<ChunkTrackMeta>()
        for (file in candidateFiles) {
            val ext = MediaExtractor()
            try {
                ext.setDataSource(file.absolutePath)
                var vWidth = 0
                var vHeight = 0
                var vMime = ""
                for (i in 0 until ext.trackCount) {
                    val fmt = ext.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/")) {
                        vMime = mime
                        vWidth = if (fmt.containsKey(MediaFormat.KEY_WIDTH)) fmt.getInteger(MediaFormat.KEY_WIDTH) else 0
                        vHeight = if (fmt.containsKey(MediaFormat.KEY_HEIGHT)) fmt.getInteger(MediaFormat.KEY_HEIGHT) else 0
                        break
                    }
                }
                if (vMime.isNotEmpty() && vWidth > 0 && vHeight > 0) {
                    metas.add(ChunkTrackMeta(file, vWidth, vHeight, vMime))
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
                candidateFiles.last().copyTo(outputFile, overwrite = true)
                outputFile
            } catch (_: Exception) {
                null
            }
        }

        val targetMeta = metas.last()
        val validFiles = metas.filter {
            it.videoWidth == targetMeta.videoWidth &&
                it.videoHeight == targetMeta.videoHeight &&
                it.videoMime == targetMeta.videoMime
        }.map { it.file }

        // Fast-path: If only 1 chunk matches, copy directly in < 5ms without re-muxing!
        if (validFiles.size == 1) {
            return try {
                validFiles.first().copyTo(outputFile, overwrite = true)
                Log.d(tag, "Single chunk fast-copy completed: ${outputFile.name}")
                outputFile
            } catch (e: Exception) {
                Log.e(tag, "Fast copy failed", e)
                null
            }
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

            val bufferSize = 1024 * 1024
            val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var globalVideoPtsOffsetUs = 0L
            var globalAudioPtsOffsetUs = 0L

            for (extractor in extractors) {
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

                var chunkFirstVideoPtsUs = -1L
                var chunkFirstAudioPtsUs = -1L
                var chunkMaxVideoPtsUs = 0L
                var chunkMaxAudioPtsUs = 0L

                if (chunkVideoTrack != -1) {
                    extractor.selectTrack(chunkVideoTrack)
                }
                if (chunkAudioTrack != -1 && muxerAudioTrack != -1) {
                    extractor.selectTrack(chunkAudioTrack)
                }

                while (true) {
                    val sampleTrackIndex = extractor.sampleTrackIndex
                    if (sampleTrackIndex < 0) break

                    val sampleSize = extractor.readSampleData(byteBuffer, 0)
                    if (sampleSize < 0) break

                    val currentPtsUs = extractor.sampleTime
                    val flags = extractor.sampleFlags

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.flags = flags

                    if (sampleTrackIndex == chunkVideoTrack && muxerVideoTrack != -1) {
                        if (chunkFirstVideoPtsUs == -1L) {
                            chunkFirstVideoPtsUs = currentPtsUs
                        }
                        val relativePtsUs = (currentPtsUs - chunkFirstVideoPtsUs).coerceAtLeast(0L)
                        if (relativePtsUs > chunkMaxVideoPtsUs) {
                            chunkMaxVideoPtsUs = relativePtsUs
                        }
                        bufferInfo.presentationTimeUs = globalVideoPtsOffsetUs + relativePtsUs
                        muxer.writeSampleData(muxerVideoTrack, byteBuffer, bufferInfo)
                    } else if (sampleTrackIndex == chunkAudioTrack && muxerAudioTrack != -1) {
                        if (chunkFirstAudioPtsUs == -1L) {
                            chunkFirstAudioPtsUs = currentPtsUs
                        }
                        val relativePtsUs = (currentPtsUs - chunkFirstAudioPtsUs).coerceAtLeast(0L)
                        if (relativePtsUs > chunkMaxAudioPtsUs) {
                            chunkMaxAudioPtsUs = relativePtsUs
                        }
                        bufferInfo.presentationTimeUs = globalAudioPtsOffsetUs + relativePtsUs
                        muxer.writeSampleData(muxerAudioTrack, byteBuffer, bufferInfo)
                    }

                    extractor.advance()
                }

                val chunkDurationUs = maxOf(chunkMaxVideoPtsUs, chunkMaxAudioPtsUs)
                globalVideoPtsOffsetUs += (chunkDurationUs + 16_666L)
                globalAudioPtsOffsetUs += (chunkDurationUs + 16_666L)
                extractor.release()
            }
            extractors.clear()

            muxer.stop()
            muxer.release()
            Log.d(tag, "Successfully stitched and synchronized ${validFiles.size} chunks into ${outputFile.name}")
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
