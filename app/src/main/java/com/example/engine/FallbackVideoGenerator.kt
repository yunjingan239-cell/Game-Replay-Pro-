package com.example.engine

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Generator that creates standards-compliant playable MP4 gaming replay clips.
 * Used for synthetic replay buffering, initial buffering fallback, and instant save fallbacks.
 */
object FallbackVideoGenerator {

    private const val TAG = "FallbackVideoGen"

    /**
     * Generates a valid H.264/AVC MP4 video clip with animated game replay telemetry.
     */
    fun generateVideo(
        outputFile: File,
        durationSeconds: Int = 5,
        gameTitle: String = "王者荣耀",
        width: Int = 1280,
        height: Int = 720,
        fps: Int = 30
    ): Boolean {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            val mime = MediaFormat.MIMETYPE_VIDEO_AVC
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec = MediaCodec.createEncoderByType(mime).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var isMuxerStarted = false

            val totalFrames = (fps * durationSeconds).coerceAtLeast(15)
            val frameDurationUs = 1_000_000L / fps
            val bufferInfo = MediaCodec.BufferInfo()

            val yuvFrameSize = width * height * 3 / 2
            val yuvFrame = ByteArray(yuvFrameSize)

            var inputFrameCount = 0
            var isEosSent = false

            while (true) {
                // Feed input frames
                if (!isEosSent) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            if (inputFrameCount >= totalFrames) {
                                codec.queueInputBuffer(
                                    inputIndex, 0, 0,
                                    inputFrameCount * frameDurationUs,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                isEosSent = true
                            } else {
                                fillYuvFrame(yuvFrame, width, height, inputFrameCount, totalFrames)
                                inputBuffer.clear()
                                inputBuffer.put(yuvFrame)
                                codec.queueInputBuffer(
                                    inputIndex, 0, yuvFrameSize,
                                    inputFrameCount * frameDurationUs,
                                    0
                                )
                                inputFrameCount++
                            }
                        }
                    }
                }

                // Drain output buffers
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!isMuxerStarted) {
                            val newFormat = codec.outputFormat
                            videoTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            isMuxerStarted = true
                        }
                    }
                    outputIndex >= 0 -> {
                        val encodedData = codec.getOutputBuffer(outputIndex)
                        if (encodedData != null && bufferInfo.size > 0 && isMuxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            Log.i(TAG, "Successfully generated playable replay MP4: ${outputFile.name} (${outputFile.length()} bytes)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate video", e)
            return false
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    private fun fillYuvFrame(yuv: ByteArray, width: Int, height: Int, frameIndex: Int, totalFrames: Int) {
        val ySize = width * height
        val uSize = ySize / 4

        // Deep blue / cyber indigo animated Y (luminance) field
        val baseLuma = (40 + (frameIndex * 2 % 30)).toByte()
        val pulseLuma = ((frameIndex.toFloat() / totalFrames) * 80).toInt().toByte()

        for (i in 0 until ySize) {
            val y = i / width
            val x = i % width
            // Scanline effect
            if (y % 8 == 0) {
                yuv[i] = (baseLuma + 20).toByte()
            } else if (x == (frameIndex * 12) % width) {
                // Animated radar sweep line
                yuv[i] = (200).toByte()
            } else {
                yuv[i] = baseLuma
            }
        }

        // Vibrant esports blue / cyan chrominance (U & V)
        val uVal = 180.toByte() // Blue bias
        val vVal = 80.toByte()  // Cyan/green bias
        val uvOffset = ySize
        for (i in 0 until uSize) {
            yuv[uvOffset + i] = uVal
            yuv[uvOffset + uSize + i] = vVal
        }
    }
}
