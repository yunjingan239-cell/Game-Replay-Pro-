package com.example.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.ReplaySettings
import java.nio.ByteBuffer
import kotlin.math.sin

/**
 * High-performance Game Audio Recording Engine.
 * Features:
 * - Internal Game Audio Playback Capture (Android 10+ API 29+) via MediaProjection:
 *   Captures in-game BGM, combat sound effects, and team voice chat directly with zero external ambient noise.
 * - Hardware AAC (Advanced Audio Coding) encoder for broadcast-quality esports audio (44.1kHz, Stereo, 128kbps).
 * - Graceful Microphone commentary capture fallback if requested or if internal audio is unavailable.
 * - Esports ambient synth generator fallback for synthetic buffer modes and device testing.
 */
class AudioRecordingEngine(
    private val context: Context,
    private val onAudioFormatAvailable: (MediaFormat) -> Unit,
    private val onAudioSampleAvailable: (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
    private val onError: (String) -> Unit
) {
    private val tag = "AudioRecordingEngine"

    private var audioRecord: AudioRecord? = null
    private var audioCodec: MediaCodec? = null

    @Volatile
    private var isRecording = false

    private var audioThread: Thread? = null
    private var cachedFormat: MediaFormat? = null

    val sampleRate = 44100
    val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    val channelCount = 2
    val bitrate = 128_000

    fun start(mediaProjection: MediaProjection?, settings: ReplaySettings) {
        if (isRecording) {
            Log.w(tag, "Audio engine already running")
            return
        }

        try {
            setupAudioCodec()
            setupAudioRecord(mediaProjection, settings)

            isRecording = true
            audioThread = Thread({
                processAudioCaptureLoop(settings)
            }, "ReplayAudioThread").apply {
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
            Log.i(tag, "Audio recording engine active (internal=${settings.internalAudioEnabled}, mic=${settings.micAudioEnabled})")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize audio engine", e)
            onError("音频录制引擎初始化失败: ${e.localizedMessage}")
        }
    }

    private fun setupAudioCodec() {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }
    }

    private fun setupAudioRecord(mediaProjection: MediaProjection?, settings: ReplaySettings) {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
            .coerceAtLeast(8192)

        var createdRecord: AudioRecord? = null

        // 1. Try Internal Game Audio Playback Capture (Android 10+ MediaProjection)
        if (settings.internalAudioEnabled && mediaProjection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasRecordAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasRecordAudioPermission) {
                try {
                    val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build()

                    val record = AudioRecord.Builder()
                        .setAudioPlaybackCaptureConfig(playbackConfig)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(audioEncoding)
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelConfig)
                                .build()
                        )
                        .setBufferSizeInBytes(minBufferSize * 2)
                        .build()

                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        createdRecord = record
                        Log.i(tag, "Using internal game audio playback capture (zero background mic noise)")
                    } else {
                        record.release()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Internal audio capture init failed, falling back: ${e.message}")
                }
            } else {
                Log.w(tag, "RECORD_AUDIO permission not granted yet for internal playback capture")
            }
        }

        // 2. Microphone fallback if internal capture unavailable or mic commentary enabled
        if (createdRecord == null) {
            val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMicPermission) {
                try {
                    val record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioEncoding,
                        minBufferSize * 2
                    )
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        createdRecord = record
                        Log.i(tag, "Using microphone audio capture")
                    } else {
                        record.release()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Mic AudioRecord init failed: ${e.message}")
                }
            }
        }

        audioRecord = createdRecord
        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.w(tag, "Failed to start AudioRecord recording: ${e.message}")
        }
    }

    private fun processAudioCaptureLoop(settings: ReplaySettings) {
        val codec = audioCodec ?: return
        val record = audioRecord

        val pcmBufferSize = 4096
        val pcmBuffer = ByteArray(pcmBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        var lastAudioPtsUs = 0L
        val bytesPerFrame = channelCount * 2 // 16-bit stereo = 4 bytes per sample frame
        val frameDurationUs = (pcmBufferSize / bytesPerFrame) * 1_000_000L / sampleRate

        // Synthetic gaming tone generator if hardware audio record is unavailable (e.g. preview mode)
        var synthPhase = 0.0

        while (isRecording) {
            var effectiveBytes = 0

            // Step 1: Read audio data from AudioRecord or generate game synth audio
            if (record != null && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = record.read(pcmBuffer, 0, pcmBufferSize)
                if (bytesRead > 0) {
                    effectiveBytes = bytesRead
                } else {
                    // Audio hardware returned no bytes or error; fill with silence so audio clock never stalls
                    pcmBuffer.fill(0)
                    effectiveBytes = pcmBufferSize
                    try {
                        Thread.sleep((frameDurationUs / 1000).coerceAtLeast(10))
                    } catch (_: Exception) {}
                }
            } else {
                // Generate clean esports chord audio tone (440Hz / 880Hz subtle game chime)
                val frames = pcmBufferSize / 4
                for (i in 0 until frames) {
                    synthPhase += 2.0 * Math.PI * 440.0 / sampleRate
                    val sample = (sin(synthPhase) * 6000.0).toInt().coerceIn(-32768, 32767).toShort()
                    val idx = i * 4
                    pcmBuffer[idx] = (sample.toInt() and 0xFF).toByte()
                    pcmBuffer[idx + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                    pcmBuffer[idx + 2] = (sample.toInt() and 0xFF).toByte()
                    pcmBuffer[idx + 3] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }
                try {
                    Thread.sleep((frameDurationUs / 1000).coerceAtLeast(10))
                } catch (_: Exception) {}
                effectiveBytes = pcmBufferSize
            }

            if (effectiveBytes > 0 && isRecording) {
                // Step 2: Feed PCM to AAC Encoder with monotonic system timestamp
                val nowUs = System.nanoTime() / 1000L
                val ptsUs = if (lastAudioPtsUs == 0L) nowUs else maxOf(lastAudioPtsUs + frameDurationUs, nowUs)
                lastAudioPtsUs = ptsUs

                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(pcmBuffer, 0, effectiveBytes)
                        codec.queueInputBuffer(inputIndex, 0, effectiveBytes, ptsUs, 0)
                    }
                }
            }

            // Step 3: Drain encoded AAC data from MediaCodec
            while (isRecording) {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 2_000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = codec.outputFormat
                        cachedFormat = newFormat
                        onAudioFormatAvailable(newFormat)
                        Log.i(tag, "AAC Audio output format configured: $newFormat")
                    }
                    outputIndex >= 0 -> {
                        val encodedBuffer = codec.getOutputBuffer(outputIndex)
                        if (encodedBuffer != null && bufferInfo.size > 0) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            onAudioSampleAvailable(encodedBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                    else -> break
                }
            }
        }
    }

    fun getAudioFormat(): MediaFormat? = cachedFormat

    fun stop() {
        isRecording = false
        try {
            audioThread?.interrupt()
            audioThread?.join(500)
        } catch (_: Exception) {}
        audioThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioCodec?.stop()
            audioCodec?.release()
        } catch (_: Exception) {}
        audioCodec = null

        Log.i(tag, "Audio recording engine stopped")
    }
}
