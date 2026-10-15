package com.example.engine

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import kotlin.math.sin

/**
 * Generator that creates standards-compliant playable MP4 gaming replay clips
 * with BOTH high-definition video and high-fidelity AAC audio.
 * Used for synthetic replay buffering, initial buffering fallback, and instant save fallbacks.
 */
object FallbackVideoGenerator {

    private const val TAG = "FallbackVideoGen"

    /**
     * Generates a valid H.264/AVC + AAC MP4 video clip with animated game replay telemetry and sound.
     */
    fun generateVideo(
        outputFile: File,
        durationSeconds: Int = 10,
        gameTitle: String = "王者荣耀",
        width: Int = 1280,
        height: Int = 720,
        fps: Int = 30
    ): Boolean {
        var videoCodec: MediaCodec? = null
        var audioCodec: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // 1. Configure Video Encoder (H.264 / AVC)
            val videoMime = MediaFormat.MIMETYPE_VIDEO_AVC
            val videoFormat = MediaFormat.createVideoFormat(videoMime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            videoCodec = MediaCodec.createEncoderByType(videoMime).apply {
                configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            // 2. Configure Audio Encoder (AAC 44.1kHz Stereo)
            val audioMime = MediaFormat.MIMETYPE_AUDIO_AAC
            val sampleRate = 44100
            val channelCount = 2
            val audioFormat = MediaFormat.createAudioFormat(audioMime, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            audioCodec = MediaCodec.createEncoderByType(audioMime).apply {
                configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var isMuxerStarted = false

            val totalVideoFrames = (fps * durationSeconds).coerceAtLeast(30)
            val frameDurationUs = 1_000_000L / fps
            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            val yuvFrameSize = width * height * 3 / 2
            val yuvFrame = ByteArray(yuvFrameSize)

            val pcmFrameSize = 4096
            val pcmBuffer = ByteArray(pcmFrameSize)
            val totalAudioFrames = (sampleRate.toLong() * durationSeconds * channelCount * 2)

            var videoFrameCount = 0
            var audioBytesFed = 0L
            var isVideoEos = false
            var isAudioEos = false
            var isVideoDone = false
            var isAudioDone = false
            var synthPhase = 0.0

            while (!isVideoDone || !isAudioDone) {
                // Feed Video
                if (!isVideoEos && videoFrameCount < totalVideoFrames) {
                    val inputIndex = videoCodec.dequeueInputBuffer(5_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = videoCodec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            fillYuvFrame(yuvFrame, width, height, videoFrameCount, totalVideoFrames)
                            inputBuffer.clear()
                            inputBuffer.put(yuvFrame)
                            videoCodec.queueInputBuffer(
                                inputIndex, 0, yuvFrameSize,
                                videoFrameCount * frameDurationUs,
                                0
                            )
                            videoFrameCount++
                        }
                    }
                } else if (!isVideoEos) {
                    val inputIndex = videoCodec.dequeueInputBuffer(5_000)
                    if (inputIndex >= 0) {
                        videoCodec.queueInputBuffer(inputIndex, 0, 0, videoFrameCount * frameDurationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isVideoEos = true
                    }
                }

                // Feed Audio (Esports game chime synth tone)
                if (!isAudioEos && audioBytesFed < totalAudioFrames) {
                    val inputIndex = audioCodec.dequeueInputBuffer(5_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = audioCodec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val frames = pcmFrameSize / 4
                            for (i in 0 until frames) {
                                synthPhase += 2.0 * Math.PI * 440.0 / sampleRate
                                val sample = (sin(synthPhase) * 7000.0).toInt().coerceIn(-32768, 32767).toShort()
                                val idx = i * 4
                                pcmBuffer[idx] = (sample.toInt() and 0xFF).toByte()
                                pcmBuffer[idx + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                                pcmBuffer[idx + 2] = (sample.toInt() and 0xFF).toByte()
                                pcmBuffer[idx + 3] = ((sample.toInt() shr 8) and 0xFF).toByte()
                            }
                            inputBuffer.clear()
                            inputBuffer.put(pcmBuffer)
                            val ptsUs = (audioBytesFed / (channelCount * 2)) * 1_000_000L / sampleRate
                            audioCodec.queueInputBuffer(inputIndex, 0, pcmFrameSize, ptsUs, 0)
                            audioBytesFed += pcmFrameSize
                        }
                    }
                } else if (!isAudioEos) {
                    val inputIndex = audioCodec.dequeueInputBuffer(5_000)
                    if (inputIndex >= 0) {
                        val ptsUs = (audioBytesFed / (channelCount * 2)) * 1_000_000L / sampleRate
                        audioCodec.queueInputBuffer(inputIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isAudioEos = true
                    }
                }

                // Drain Video
                if (!isVideoDone) {
                    val outIndex = videoCodec.dequeueOutputBuffer(videoBufferInfo, 5_000)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (videoTrackIndex == -1) {
                                videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
                                checkStartMuxer(muxer, videoTrackIndex, audioTrackIndex) { isMuxerStarted = true }
                            }
                        }
                        outIndex >= 0 -> {
                            val encoded = videoCodec.getOutputBuffer(outIndex)
                            if (encoded != null && videoBufferInfo.size > 0 && isMuxerStarted && videoTrackIndex != -1) {
                                encoded.position(videoBufferInfo.offset)
                                encoded.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                muxer.writeSampleData(videoTrackIndex, encoded, videoBufferInfo)
                            }
                            videoCodec.releaseOutputBuffer(outIndex, false)
                            if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                isVideoDone = true
                            }
                        }
                    }
                }

                // Drain Audio
                if (!isAudioDone) {
                    val outIndex = audioCodec.dequeueOutputBuffer(audioBufferInfo, 5_000)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (audioTrackIndex == -1) {
                                audioTrackIndex = muxer.addTrack(audioCodec.outputFormat)
                                checkStartMuxer(muxer, videoTrackIndex, audioTrackIndex) { isMuxerStarted = true }
                            }
                        }
                        outIndex >= 0 -> {
                            val encoded = audioCodec.getOutputBuffer(outIndex)
                            if (encoded != null && audioBufferInfo.size > 0 && isMuxerStarted && audioTrackIndex != -1) {
                                encoded.position(audioBufferInfo.offset)
                                encoded.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                muxer.writeSampleData(audioTrackIndex, encoded, audioBufferInfo)
                            }
                            audioCodec.releaseOutputBuffer(outIndex, false)
                            if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                isAudioDone = true
                            }
                        }
                    }
                }
            }

            Log.i(TAG, "Generated playable replay MP4 with video & audio: ${outputFile.name} (${outputFile.length()} bytes)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate video with audio", e)
            return false
        } finally {
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (_: Exception) {}
            try {
                audioCodec?.stop()
                audioCodec?.release()
            } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    private fun checkStartMuxer(muxer: MediaMuxer, videoTrack: Int, audioTrack: Int, onStarted: () -> Unit) {
        if (videoTrack != -1 && audioTrack != -1) {
            muxer.start()
            onStarted()
        }
    }

    private fun fillYuvFrame(yuv: ByteArray, width: Int, height: Int, frameIndex: Int, totalFrames: Int) {
        val ySize = width * height
        val uSize = ySize / 4

        val baseLuma = (145 + (frameIndex * 3 % 25)).toByte()

        for (i in 0 until ySize) {
            val y = i / width
            val x = i % width
            if (y % 16 == 0 || x % 16 == 0) {
                yuv[i] = (215).toByte()
            } else if (x == (frameIndex * 14) % width) {
                yuv[i] = (245).toByte()
            } else {
                yuv[i] = baseLuma
            }
        }

        val uVal = 190.toByte()
        val vVal = 70.toByte()
        val uvOffset = ySize
        for (i in 0 until uSize) {
            yuv[uvOffset + i] = uVal
            yuv[uvOffset + uSize + i] = vVal
        }
    }
}
