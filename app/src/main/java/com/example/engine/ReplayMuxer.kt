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

    fun stitchChunks(chunks: List<VideoChunk>, gameTitle: String): File? {
        if (chunks.isEmpty()) {
            Log.w(tag, "No video chunks provided to stitch")
            return null
        }

        val validFiles = chunks.map { it.file }.filter { it.exists() && it.length() > 0 }
        if (validFiles.isEmpty()) {
            Log.w(tag, "All chunk files are missing or empty")
            return null
        }

        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val timePart = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(outputDirectory, "${datePart}_${timePart}.mp4")

        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val extractors = validFiles.map { file ->
                MediaExtractor().apply { setDataSource(file.absolutePath) }
            }

            val firstExtractor = extractors.first()
            var videoTrackSourceIndex = -1
            var audioTrackSourceIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            for (i in 0 until firstExtractor.trackCount) {
                val format = firstExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") && videoTrackSourceIndex == -1) {
                    videoTrackSourceIndex = i
                    videoFormat = format
                } else if (mime.startsWith("audio/") && audioTrackSourceIndex == -1) {
                    audioTrackSourceIndex = i
                    audioFormat = format
                }
            }

            if (videoTrackSourceIndex == -1 || videoFormat == null) {
                Log.e(tag, "No video track found in first chunk, falling back to file copy")
                extractors.forEach { it.release() }
                muxer.release()
                outputFile.delete()
                val newest = validFiles.last()
                newest.copyTo(outputFile, overwrite = true)
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
                var chunkFirstVideoPtsUs = -1L
                var chunkFirstAudioPtsUs = -1L
                var chunkMaxVideoPtsUs = 0L
                var chunkMaxAudioPtsUs = 0L

                if (videoTrackSourceIndex != -1) {
                    extractor.selectTrack(videoTrackSourceIndex)
                }
                if (audioTrackSourceIndex != -1 && muxerAudioTrack != -1) {
                    extractor.selectTrack(audioTrackSourceIndex)
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

                    if (sampleTrackIndex == videoTrackSourceIndex) {
                        if (chunkFirstVideoPtsUs == -1L) {
                            chunkFirstVideoPtsUs = currentPtsUs
                        }
                        val relativePtsUs = (currentPtsUs - chunkFirstVideoPtsUs).coerceAtLeast(0L)
                        if (relativePtsUs > chunkMaxVideoPtsUs) {
                            chunkMaxVideoPtsUs = relativePtsUs
                        }
                        bufferInfo.presentationTimeUs = globalVideoPtsOffsetUs + relativePtsUs
                        muxer.writeSampleData(muxerVideoTrack, byteBuffer, bufferInfo)
                    } else if (sampleTrackIndex == audioTrackSourceIndex && muxerAudioTrack != -1) {
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

                // Both tracks advance by the same chunk duration so audio and video never drift
                val chunkDurationUs = maxOf(chunkMaxVideoPtsUs, chunkMaxAudioPtsUs)
                globalVideoPtsOffsetUs += (chunkDurationUs + 16_666L)
                globalAudioPtsOffsetUs += (chunkDurationUs + 16_666L)
                extractor.release()
            }

            muxer.stop()
            muxer.release()
            Log.d(tag, "Successfully stitched and synchronized ${validFiles.size} chunks into ${outputFile.name}")
            return outputFile
        } catch (e: Exception) {
            Log.e(tag, "Error during MediaMuxer stitching, using fallback", e)
            try {
                muxer?.release()
            } catch (_: Exception) {}
            try {
                validFiles.last().copyTo(outputFile, overwrite = true)
                return outputFile
            } catch (err: Exception) {
                Log.e(tag, "Fallback copy failed", err)
                return null
            }
        }
    }
}
