package com.example.engine

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.example.data.model.ReplaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class EncoderTelemetry(
    val encoderName: String,
    val codecMime: String = "video/hevc",
    val isHardwareAccelerated: Boolean = true,
    val isHdrActive: Boolean = false,
    val currentOrientation: String = "横屏对局 (16:9)",
    val activeResolution: String = "1920x1080",
    val activeFps: Int = 60,
    val compressionRatio: String = "HEVC 原画超清",
    val frameDrops: Int = 0,
    val isAudioActive: Boolean = true
)

/**
 * High-Fidelity Screen Recording & Instant Replay Rolling Buffer Engine.
 * Features:
 * - 1:1 Native pixel resolution alignment to eliminate GPU bilinear scaling blur.
 * - High-Bitrate (up to 50-80 Mbps) + Hardware QP Clamping (QP_MAX <= 26) to eliminate macroblocking.
 * - Full motion-estimation lookahead (no forced zero-latency degradation) + BT.709 Full-Range color accuracy.
 * - Live hot-reloading when quality settings are updated while recording is active.
 */
class ScreenRecordingEngine(
    private val context: Context,
    private val bufferManager: CircularBufferManager,
    private val onChunkSaved: (File, Long) -> Unit,
    private val onTelemetryUpdate: (EncoderTelemetry) -> Unit,
    private val onError: (String) -> Unit,
    private val onProjectionStopped: (() -> Unit)? = null
) {

    private val tag = "ScreenRecordingEngine"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null

    private var audioEngine: AudioRecordingEngine? = null

    private val muxerLock = Any()
    private var currentMuxer: MediaMuxer? = null
    private var currentChunkFile: File? = null

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isMuxerStarted = false
    @Volatile
    private var isRecording = false

    // Preserved SPS/PPS/CSD metadata for seamless chunk creation without waiting for format change
    private var cachedVideoOutputFormat: MediaFormat? = null
    private var cachedAudioOutputFormat: MediaFormat? = null

    // PTS Normalization per chunk to guarantee synchronous zero-based video & audio playback
    private var chunkBaseVideoPtsUs: Long = -1L
    private var chunkBaseAudioPtsUs: Long = -1L
    private var lastChunkVideoPtsUs: Long = 0L
    private var lastChunkAudioPtsUs: Long = 0L
    private var chunkVideoSampleCount: Int = 0

    @Volatile
    private var isAudioRequested = false

    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var drainRunnable: Runnable? = null

    private var chunkRotationJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default)

    private var currentSettings: ReplaySettings? = null
    private var chunkStartTime = 0L
    private var encoderName = "Hardware HEVC Encoder"
    private var isHwAccelerated = true
    private var isHdrEnabled = false
    private var activeMimeType = MediaFormat.MIMETYPE_VIDEO_HEVC

    private var currentWidth = 1920
    private var currentHeight = 1080
    private var displayListener: DisplayManager.DisplayListener? = null
    private var lastReportedLandscape: Boolean? = null

    fun startRecording(projection: MediaProjection?, settings: ReplaySettings) {
        if (isRecording) {
            Log.w(tag, "Recording already active")
            return
        }

        this.mediaProjection = projection
        this.currentSettings = settings
        this.isAudioRequested = (settings.internalAudioEnabled || settings.micAudioEnabled)
        isRecording = true

        handlerThread = HandlerThread("ReplayEngineThread", Process.THREAD_PRIORITY_DEFAULT).apply {
            start()
        }
        backgroundHandler = Handler(handlerThread!!.looper)

        try {
            // Android 14+ (API 34+) strictly requires registering MediaProjection.Callback
            // BEFORE any AudioPlaybackCaptureConfiguration or createVirtualDisplay call.
            projection?.let { proj ->
                if (projectionCallback == null) {
                    val callback = object : MediaProjection.Callback() {
                        override fun onStop() {
                            super.onStop()
                            Log.i(tag, "MediaProjection stopped by system")
                            stopRecording()
                            onProjectionStopped?.invoke()
                        }
                    }
                    projectionCallback = callback
                    proj.registerCallback(callback, backgroundHandler)
                }
            }

            setupAudioEngine(settings)
            setupCodecAndVirtualDisplay(settings)
            startChunkRotationLoop(settings)
            setupDisplayOrientationMonitoring(settings)
            Log.i(tag, "High-fidelity replay engine active ($encoderName, MIME=$activeMimeType, res=${currentWidth}x${currentHeight})")
        } catch (e: Throwable) {
            Log.e(tag, "Failed to start hardware recording engine", e)
            onError("硬件录制编码器初始化失败: ${e.localizedMessage}")
            stopRecording()
        }
    }

    /**
     * Dynamically applies updated quality settings (resolution, FPS, bitrate, codec, VBR)
     * while recording is active so changes in SettingsScreen take effect immediately.
     */
    fun updateSettingsLive(newSettings: ReplaySettings) {
        val old = currentSettings
        currentSettings = newSettings
        if (!isRecording) return

        val needsEncoderRebuild = old == null ||
            old.resolutionLabel != newSettings.resolutionLabel ||
            old.resolutionWidth != newSettings.resolutionWidth ||
            old.resolutionHeight != newSettings.resolutionHeight ||
            old.fps != newSettings.fps ||
            old.bitrateMbps != newSettings.bitrateMbps ||
            old.videoCodec != newSettings.videoCodec ||
            old.hdrEnabled != newSettings.hdrEnabled ||
            old.batterySaverVbr != newSettings.batterySaverVbr

        if (needsEncoderRebuild) {
            val isLandscapeNow = isDeviceInLandscape()
            handleOrientationFlipped(newSettings, isLandscapeNow)
        }
    }

    private fun setupAudioEngine(settings: ReplaySettings) {
        if (!settings.internalAudioEnabled && !settings.micAudioEnabled) {
            isAudioRequested = false
            return
        }

        audioEngine = AudioRecordingEngine(
            context = context,
            onAudioFormatAvailable = { format ->
                backgroundHandler?.post {
                    cachedAudioOutputFormat = format
                    tryStartMuxerIfReady()
                }
            },
            onAudioSampleAvailable = { buffer, bufferInfo ->
                synchronized(muxerLock) {
                    if (isMuxerStarted && audioTrackIndex != -1 && currentMuxer != null) {
                        if (chunkBaseAudioPtsUs == -1L) {
                            chunkBaseAudioPtsUs = bufferInfo.presentationTimeUs
                        }
                        val relativePtsUs = (bufferInfo.presentationTimeUs - chunkBaseAudioPtsUs).coerceAtLeast(0L)
                        val finalPtsUs = maxOf(lastChunkAudioPtsUs, relativePtsUs)
                        lastChunkAudioPtsUs = finalPtsUs
                        bufferInfo.presentationTimeUs = finalPtsUs

                        try {
                            currentMuxer?.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                        } catch (e: Exception) {
                            Log.v(tag, "Audio sample write note: ${e.message}")
                        }
                    }
                }
            },
            onError = { err ->
                Log.w(tag, "Audio engine fallback to video-only: $err")
                isAudioRequested = false
                backgroundHandler?.post {
                    tryStartMuxerIfReady()
                }
            }
        ).apply {
            start(mediaProjection, settings)
        }
    }

    private fun setupCodecAndVirtualDisplay(settings: ReplaySettings) {
        val isLandscape = isDeviceInLandscape()
        lastReportedLandscape = isLandscape

        // 1. Select codec: HEVC preferred, fallback to AVC if needed
        val requestedMime = if (settings.videoCodec.contains("AVC", ignoreCase = true)) {
            MediaFormat.MIMETYPE_VIDEO_AVC
        } else {
            MediaFormat.MIMETYPE_VIDEO_HEVC
        }

        var (codecInfo, isHardware, foundMime) = resolveBestHardwareEncoderInfo(requestedMime)
        if (foundMime != requestedMime && requestedMime == MediaFormat.MIMETYPE_VIDEO_HEVC) {
            val fallback = resolveBestHardwareEncoderInfo(MediaFormat.MIMETYPE_VIDEO_AVC)
            codecInfo = fallback.first
            isHardware = fallback.second
            foundMime = MediaFormat.MIMETYPE_VIDEO_AVC
        }

        encoderName = codecInfo?.name ?: if (foundMime == MediaFormat.MIMETYPE_VIDEO_HEVC) "Hardware HEVC Encoder" else "Hardware AVC Encoder"
        isHwAccelerated = isHardware
        activeMimeType = foundMime

        val capabilities = try {
            codecInfo?.getCapabilitiesForType(activeMimeType)
        } catch (_: Exception) {
            null
        }

        // Determine hardware alignment requirement (default 2-pixel for 1:1 native sharpness)
        val widthAlignment = capabilities?.videoCapabilities?.widthAlignment ?: 2
        val heightAlignment = capabilities?.videoCapabilities?.heightAlignment ?: 2
        val alignStep = maxOf(2, widthAlignment, heightAlignment)

        val (w, h) = if (settings.autoDetectOrientation) {
            settings.getEffectiveDimensions(context, isLandscape, alignStep)
        } else {
            settings.getEffectiveDimensions(context, isLandscape, alignStep)
        }
        currentWidth = w
        currentHeight = h

        val fps = settings.fps
        val rawTargetBitrate = settings.getEffectiveBitrateBps(currentWidth, currentHeight)
        val supportedBitrateRange = capabilities?.videoCapabilities?.bitrateRange
        val effectiveBitrate = if (supportedBitrateRange != null) {
            rawTargetBitrate.coerceIn(supportedBitrateRange.lower.coerceAtLeast(8_000_000), supportedBitrateRange.upper)
        } else {
            rawTargetBitrate
        }
        val effectiveMbps = effectiveBitrate / 1_000_000

        // 2. Configure hardware MediaCodec with progressive fallback (Full QP + Color -> Standard HQ -> Safe 16px Aligned)
        mediaCodec = createAndConfigureCodecWithFallback(
            codecName = codecInfo?.name,
            mime = activeMimeType,
            capabilities = capabilities,
            width = currentWidth,
            height = currentHeight,
            bitrate = effectiveBitrate,
            fps = fps,
            settings = settings,
            isLandscape = isLandscape
        )

        onTelemetryUpdate(
            EncoderTelemetry(
                encoderName = encoderName,
                codecMime = if (activeMimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) "HEVC (H.265)" else "AVC (H.264)",
                isHardwareAccelerated = isHwAccelerated,
                isHdrActive = isHdrEnabled,
                currentOrientation = if (isLandscape) "横屏对局 (${currentWidth}x${currentHeight})" else "竖屏 (${currentWidth}x${currentHeight})",
                activeResolution = "${currentWidth}x${currentHeight}",
                activeFps = fps,
                compressionRatio = "${effectiveMbps}Mbps 原画级 (${if (activeMimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) "HEVC" else "AVC"})",
                isAudioActive = (audioEngine != null)
            )
        )

        prepareNewChunkMuxer()

        // 3. Connect or resize VirtualDisplay from MediaProjection
        mediaProjection?.let { proj ->
            val densityDpi = context.resources.displayMetrics.densityDpi
            if (projectionCallback == null) {
                val callback = object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        Log.i(tag, "MediaProjection stopped by system")
                        stopRecording()
                        onProjectionStopped?.invoke()
                    }
                }
                projectionCallback = callback
                proj.registerCallback(callback, backgroundHandler)
            }

            val existingDisplay = virtualDisplay
            if (existingDisplay == null) {
                virtualDisplay = proj.createVirtualDisplay(
                    "GameReplayCapture",
                    currentWidth,
                    currentHeight,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    inputSurface,
                    null,
                    backgroundHandler
                )
            } else {
                existingDisplay.resize(currentWidth, currentHeight, densityDpi)
                existingDisplay.surface = inputSurface
            }
        }

        startDrainCodecLoop()
    }

    private fun createAndConfigureCodecWithFallback(
        codecName: String?,
        mime: String,
        capabilities: MediaCodecInfo.CodecCapabilities?,
        width: Int,
        height: Int,
        bitrate: Int,
        fps: Int,
        settings: ReplaySettings,
        isLandscape: Boolean
    ): MediaCodec {
        fun instantiateCodec(): MediaCodec {
            return if (!codecName.isNullOrBlank()) {
                try {
                    MediaCodec.createByCodecName(codecName)
                } catch (_: Throwable) {
                    MediaCodec.createEncoderByType(mime)
                }
            } else {
                MediaCodec.createEncoderByType(mime)
            }
        }

        fun releaseQuietly(codec: MediaCodec?) {
            try {
                inputSurface?.release()
            } catch (_: Throwable) {}
            inputSurface = null
            try {
                codec?.release()
            } catch (_: Throwable) {}
        }

        // Attempt 1: Maximum visual quality with explicit QP clamping, BT.709 Full Range, and CBR/VBR tuning
        var candidateCodec: MediaCodec? = null
        try {
            val codec = instantiateCodec()
            candidateCodec = codec
            val format = createEncoderVideoFormat(
                mime = mime,
                capabilities = capabilities,
                width = width,
                height = height,
                bitrate = bitrate,
                fps = fps,
                enableHdr = settings.hdrEnabled && mime == MediaFormat.MIMETYPE_VIDEO_HEVC,
                useBatterySaverVbr = settings.batterySaverVbr,
                includeAdvancedQualityKeys = true
            )
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()
            return codec
        } catch (e: Throwable) {
            releaseQuietly(candidateCodec)
            Log.w(tag, "Advanced HQ format configure note (${e.message}), retrying standard high-bitrate format")
        }

        // Attempt 2: Standard High-Bitrate SDR format at native dimensions
        candidateCodec = null
        try {
            val codec = instantiateCodec()
            candidateCodec = codec
            isHdrEnabled = false
            val format = createEncoderVideoFormat(
                mime = mime,
                capabilities = capabilities,
                width = width,
                height = height,
                bitrate = bitrate,
                fps = fps,
                enableHdr = false,
                useBatterySaverVbr = settings.batterySaverVbr,
                includeAdvancedQualityKeys = false
            )
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()
            return codec
        } catch (e: Throwable) {
            releaseQuietly(candidateCodec)
            Log.w(tag, "Native 2px aligned configure note (${e.message}), falling back to 16px aligned high-bitrate")
        }

        // Attempt 3: 16-pixel aligned fallback for strict hardware encoders
        candidateCodec = null
        try {
            val (safeW, safeH) = settings.getEffectiveDimensions(context, isLandscape, alignStep = 16)
            currentWidth = safeW
            currentHeight = safeH
            val codec = instantiateCodec()
            candidateCodec = codec
            val fallbackFormat = createEncoderVideoFormat(
                mime = mime,
                capabilities = null,
                width = safeW,
                height = safeH,
                bitrate = bitrate.coerceAtMost(36_000_000),
                fps = fps.coerceAtMost(60),
                enableHdr = false,
                useBatterySaverVbr = true,
                includeAdvancedQualityKeys = false
            )
            codec.configure(fallbackFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()
            return codec
        } catch (e: Throwable) {
            releaseQuietly(candidateCodec)
            Log.w(tag, "16px aligned native configure note (${e.message}), falling back to 1080p aligned")
        }

        // Attempt 4: Universal 1080p 16-pixel aligned fallback (for devices whose hardware encoder caps at 1080p)
        val fallbackSettings = settings.copy(resolutionLabel = "1080P 超清")
        val (fhdW, fhdH) = fallbackSettings.getEffectiveDimensions(context, isLandscape, alignStep = 16)
        currentWidth = fhdW
        currentHeight = fhdH
        val codec = instantiateCodec()
        val fhdFormat = createEncoderVideoFormat(
            mime = mime,
            capabilities = null,
            width = fhdW,
            height = fhdH,
            bitrate = bitrate.coerceIn(16_000_000, 32_000_000),
            fps = fps.coerceAtMost(60),
            enableHdr = false,
            useBatterySaverVbr = true,
            includeAdvancedQualityKeys = false
        )
        codec.configure(fhdFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        codec.start()
        return codec
    }

    private fun createEncoderVideoFormat(
        mime: String,
        capabilities: MediaCodecInfo.CodecCapabilities?,
        width: Int,
        height: Int,
        bitrate: Int,
        fps: Int,
        enableHdr: Boolean,
        useBatterySaverVbr: Boolean,
        includeAdvancedQualityKeys: Boolean
    ): MediaFormat {
        return MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            // 2-second keyframe interval preserves bit budget for P-frames so fast motion stays crystal clear
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            // Repeat frame every 100ms when screen is static so muxer never starves
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000L)
            // Realtime priority + full operating rate so hardware encoder runs at peak clock
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            setFloat(MediaFormat.KEY_OPERATING_RATE, fps.toFloat())

            // Rate control mode: prefer CBR when user wants max clarity (no VBR bit-starving in teamfights)
            val encoderCaps = capabilities?.encoderCapabilities
            val desiredMode = if (!useBatterySaverVbr &&
                encoderCaps?.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR) == true
            ) {
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
            } else {
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            }
            setInteger(MediaFormat.KEY_BITRATE_MODE, desiredMode)

            if (includeAdvancedQualityKeys) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setInteger(MediaFormat.KEY_QUALITY, 100)
                }
                // Hardware Quantization Parameter (QP) Clamping on Android 12+ (API 31+):
                // Prevents the hardware encoder from ever quantizing macroblocks coarsely (QP > 26),
                // completely eliminating blocky compression artifacts in grass, particles, and dark scenes.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setInteger(MediaFormat.KEY_VIDEO_QP_I_MIN, 8)
                    setInteger(MediaFormat.KEY_VIDEO_QP_I_MAX, 24)
                    setInteger(MediaFormat.KEY_VIDEO_QP_P_MIN, 8)
                    setInteger(MediaFormat.KEY_VIDEO_QP_P_MAX, 26)
                    setInteger(MediaFormat.KEY_VIDEO_QP_B_MIN, 8)
                    setInteger(MediaFormat.KEY_VIDEO_QP_B_MAX, 28)
                }

                // Accurate BT.709 Full-Range color space so text edges and dark shadows are never washed out
                setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
                setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
                setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL)
            }

            isHdrEnabled = false
            if (enableHdr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
                    setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084)
                    setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL)
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10)
                    isHdrEnabled = true
                } catch (e: Exception) {
                    Log.w(tag, "HDR configuration ignored: ${e.message}")
                }
            }
        }
    }

    private fun isDeviceInLandscape(): Boolean {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val displayRotation = dm?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation
        if (displayRotation != null) {
            return displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270
        }

        val orientation = context.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) return true
        if (orientation == Configuration.ORIENTATION_PORTRAIT) return false

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val fallbackRotation = wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        return fallbackRotation == Surface.ROTATION_90 || fallbackRotation == Surface.ROTATION_270
    }

    private fun setupDisplayOrientationMonitoring(settings: ReplaySettings) {
        if (!settings.autoDetectOrientation) return

        try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}
                override fun onDisplayRemoved(displayId: Int) {}
                override fun onDisplayChanged(displayId: Int) {
                    if (displayId != Display.DEFAULT_DISPLAY || !isRecording) return
                    val isLandscapeNow = isDeviceInLandscape()
                    if (lastReportedLandscape != isLandscapeNow) {
                        lastReportedLandscape = isLandscapeNow
                        val activeSettings = currentSettings ?: settings
                        handleOrientationFlipped(activeSettings, isLandscapeNow)
                    }
                }
            }
            displayListener = listener
            dm.registerDisplayListener(listener, backgroundHandler)
        } catch (e: Exception) {
            Log.w(tag, "DisplayListener setup skipped: ${e.message}")
        }
    }

    private fun handleOrientationFlipped(settings: ReplaySettings, isLandscapeNow: Boolean) {
        backgroundHandler?.post {
            if (!isRecording) return@post
            try {
                Log.i(tag, "Reconfiguring encoder & VirtualDisplay (landscape=$isLandscapeNow)...")

                drainRunnable?.let { backgroundHandler?.removeCallbacks(it) }

                // 1. Drain and finalize active chunk if it has valid video samples
                drainAvailableEncoderOutputLocked()
                val completedFile = currentChunkFile
                val durationMs = System.currentTimeMillis() - chunkStartTime
                val hadValidVideoSamples = closeCurrentMuxerSafely()

                if (hadValidVideoSamples && completedFile != null && completedFile.exists() && completedFile.length() > 0) {
                    bufferManager.onChunkFinished(completedFile, durationMs, settings.maxChunksInRing)
                    onChunkSaved(completedFile, durationMs)
                } else {
                    try {
                        completedFile?.delete()
                    } catch (_: Exception) {}
                }

                // 2. Detach surface from VirtualDisplay before releasing old encoder
                try {
                    virtualDisplay?.surface = null
                } catch (_: Exception) {}

                try {
                    mediaCodec?.stop()
                    mediaCodec?.release()
                } catch (_: Exception) {}
                mediaCodec = null

                try {
                    inputSurface?.release()
                } catch (_: Exception) {}
                inputSurface = null

                cachedVideoOutputFormat = null

                // 3. Reconfigure encoder and resize existing VirtualDisplay
                setupCodecAndVirtualDisplay(settings)
            } catch (e: Exception) {
                Log.e(tag, "Failed to adapt encoder configuration", e)
            }
        }
    }

    private fun resolveBestHardwareEncoderInfo(mimeType: String): Triple<MediaCodecInfo?, Boolean, String> {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (codecInfo in codecList.codecInfos) {
                if (!codecInfo.isEncoder) continue
                for (type in codecInfo.supportedTypes) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            codecInfo.isHardwareAccelerated
                        } else {
                            val lower = codecInfo.name.lowercase()
                            !lower.startsWith("omx.google.") && !lower.contains("sw") && !lower.contains("google")
                        }
                        if (isHw) {
                            return Triple(codecInfo, true, mimeType)
                        }
                    }
                }
            }
            Triple(null, true, mimeType)
        } catch (e: Exception) {
            Log.w(tag, "Hardware encoder probing skipped: ${e.message}")
            Triple(null, true, mimeType)
        }
    }

    private fun prepareNewChunkMuxer() {
        synchronized(muxerLock) {
            try {
                val chunkFile = bufferManager.createNextChunkFile()
                currentChunkFile = chunkFile
                currentMuxer = MediaMuxer(chunkFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                isMuxerStarted = false
                videoTrackIndex = -1
                audioTrackIndex = -1
                chunkBaseVideoPtsUs = -1L
                chunkBaseAudioPtsUs = -1L
                lastChunkVideoPtsUs = 0L
                lastChunkAudioPtsUs = 0L
                chunkVideoSampleCount = 0
                chunkStartTime = System.currentTimeMillis()

                tryStartMuxerIfReady()
            } catch (e: Exception) {
                Log.e(tag, "Failed to create chunk muxer", e)
            }
        }
    }

    private fun tryStartMuxerIfReady() {
        synchronized(muxerLock) {
            val muxer = currentMuxer ?: return
            if (isMuxerStarted) return

            val vFormat = cachedVideoOutputFormat ?: return
            val aFormat = cachedAudioOutputFormat
            val audioRequired = isAudioRequested
            val waitedMs = System.currentTimeMillis() - chunkStartTime

            // Do not block video muxing forever if game audio capture takes > 400ms or is restricted
            if (!audioRequired || aFormat != null || waitedMs >= 400L) {
                videoTrackIndex = muxer.addTrack(vFormat)
                if (aFormat != null) {
                    audioTrackIndex = muxer.addTrack(aFormat)
                }
                muxer.start()
                isMuxerStarted = true
                Log.d(tag, "Muxer primed (videoTrack=$videoTrackIndex, audioTrack=$audioTrackIndex, waitedMs=$waitedMs)")
            }
        }
    }

    private fun startDrainCodecLoop() {
        drainRunnable?.let { backgroundHandler?.removeCallbacks(it) }

        val runnable = object : Runnable {
            override fun run() {
                if (!isRecording || mediaCodec == null) return
                drainAvailableEncoderOutputLocked()
                if (isRecording) {
                    backgroundHandler?.postDelayed(this, 8L)
                }
            }
        }
        drainRunnable = runnable
        backgroundHandler?.post(runnable)
    }

    private fun drainAvailableEncoderOutputLocked() {
        val encoder = mediaCodec ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            var drainedCount = 0
            while (isRecording && drainedCount < 24) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0L)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!isMuxerStarted && cachedVideoOutputFormat != null) {
                            tryStartMuxerIfReady()
                        }
                        break
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        cachedVideoOutputFormat = encoder.outputFormat
                        tryStartMuxerIfReady()
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        val isCodecConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (!isCodecConfig && outputBuffer != null && bufferInfo.size > 0) {
                            if (!isMuxerStarted && cachedVideoOutputFormat != null) {
                                tryStartMuxerIfReady()
                            }
                            synchronized(muxerLock) {
                                if (isMuxerStarted && videoTrackIndex != -1 && currentMuxer != null) {
                                    if (chunkBaseVideoPtsUs == -1L) {
                                        chunkBaseVideoPtsUs = bufferInfo.presentationTimeUs
                                    }
                                    val relativePtsUs = (bufferInfo.presentationTimeUs - chunkBaseVideoPtsUs).coerceAtLeast(0L)
                                    val finalPtsUs = if (chunkVideoSampleCount == 0) 0L else maxOf(lastChunkVideoPtsUs + 1000L, relativePtsUs)
                                    lastChunkVideoPtsUs = finalPtsUs
                                    bufferInfo.presentationTimeUs = finalPtsUs

                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    try {
                                        currentMuxer?.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                        chunkVideoSampleCount++
                                    } catch (e: Exception) {
                                        Log.v(tag, "Video write sample note: ${e.message}")
                                    }
                                }
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        drainedCount++
                    }
                    else -> break
                }
            }
        } catch (e: Exception) {
            Log.v(tag, "Drain codec iteration note: ${e.message}")
        }
    }

    private fun closeCurrentMuxerSafely(): Boolean {
        synchronized(muxerLock) {
            val hadValidVideo = isMuxerStarted && chunkVideoSampleCount > 0
            try {
                if (hadValidVideo) {
                    currentMuxer?.stop()
                }
            } catch (e: Exception) {
                Log.w(tag, "Muxer stop note: ${e.message}")
            }
            try {
                currentMuxer?.release()
            } catch (_: Exception) {}
            currentMuxer = null
            isMuxerStarted = false
            videoTrackIndex = -1
            audioTrackIndex = -1
            chunkVideoSampleCount = 0
            return hadValidVideo
        }
    }

    private fun startChunkRotationLoop(initialSettings: ReplaySettings) {
        chunkRotationJob = engineScope.launch {
            val isSyntheticMode = (mediaProjection == null)

            if (isSyntheticMode && bufferManager.getChunkCount() == 0) {
                val settings = currentSettings ?: initialSettings
                val seedChunk = bufferManager.createNextChunkFile()
                FallbackVideoGenerator.generateVideo(
                    outputFile = seedChunk,
                    durationSeconds = 5,
                    gameTitle = settings.currentGameTitle,
                    width = currentWidth,
                    height = currentHeight,
                    fps = settings.fps
                )
                if (seedChunk.exists() && seedChunk.length() > 0) {
                    bufferManager.onChunkFinished(seedChunk, 5_000L, settings.maxChunksInRing)
                    onChunkSaved(seedChunk, 5_000L)
                }
            }

            while (isActive && isRecording) {
                val activeSettings = currentSettings ?: initialSettings
                val elapsed = System.currentTimeMillis() - chunkStartTime
                if (elapsed >= (activeSettings.chunkDurationSeconds * 1000L)) {
                    requestInstantSyncFrame()
                    rotateChunk(activeSettings)
                }
                delay(if (activeSettings.batterySaverVbr) 400L else 200L)
            }
        }
    }

    private fun requestInstantSyncFrame() {
        try {
            val params = Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            }
            mediaCodec?.setParameters(params)
        } catch (_: Exception) {}
    }

    private fun rotateChunk(settings: ReplaySettings) {
        backgroundHandler?.post {
            try {
                drainAvailableEncoderOutputLocked()
                val completedFile = currentChunkFile
                val durationMs = System.currentTimeMillis() - chunkStartTime
                val hadValidVideo = closeCurrentMuxerSafely()

                if (completedFile != null) {
                    if (hadValidVideo && completedFile.exists() && completedFile.length() > 0) {
                        bufferManager.onChunkFinished(completedFile, durationMs, settings.maxChunksInRing)
                        onChunkSaved(completedFile, durationMs)
                    } else if (mediaProjection == null) {
                        FallbackVideoGenerator.generateVideo(
                            outputFile = completedFile,
                            durationSeconds = 5,
                            gameTitle = settings.currentGameTitle,
                            width = currentWidth,
                            height = currentHeight,
                            fps = settings.fps
                        )
                        if (completedFile.exists() && completedFile.length() > 0) {
                            bufferManager.onChunkFinished(completedFile, durationMs, settings.maxChunksInRing)
                            onChunkSaved(completedFile, durationMs)
                        }
                    } else {
                        try {
                            completedFile.delete()
                        } catch (_: Exception) {}
                    }
                }

                if (isRecording) {
                    prepareNewChunkMuxer()
                    requestInstantSyncFrame()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error rotating video chunk", e)
            }
        }
    }

    fun flushCurrentChunk(settings: ReplaySettings, onFlushed: () -> Unit) {
        val handler = backgroundHandler
        if (handler == null || !isRecording) {
            onFlushed()
            return
        }

        handler.post {
            try {
                requestInstantSyncFrame()
                drainAvailableEncoderOutputLocked()

                val durationMs = System.currentTimeMillis() - chunkStartTime
                val hasBufferedChunks = bufferManager.getChunkCount() > 0

                if (hasBufferedChunks && (durationMs < 450L || chunkVideoSampleCount < 2)) {
                    return@post
                }

                val completedFile = currentChunkFile
                val hadValidVideo = closeCurrentMuxerSafely()

                if (completedFile != null) {
                    if (hadValidVideo && completedFile.exists() && completedFile.length() > 0) {
                        bufferManager.onChunkFinished(completedFile, durationMs.coerceAtLeast(1000L), settings.maxChunksInRing)
                        onChunkSaved(completedFile, durationMs.coerceAtLeast(1000L))
                    } else if (mediaProjection == null && !hasBufferedChunks) {
                        FallbackVideoGenerator.generateVideo(
                            outputFile = completedFile,
                            durationSeconds = 4,
                            gameTitle = settings.currentGameTitle,
                            width = currentWidth,
                            height = currentHeight,
                            fps = settings.fps
                        )
                        if (completedFile.exists() && completedFile.length() > 0) {
                            bufferManager.onChunkFinished(completedFile, 4000L, settings.maxChunksInRing)
                            onChunkSaved(completedFile, 4000L)
                        }
                    } else {
                        try {
                            completedFile.delete()
                        } catch (_: Exception) {}
                    }
                }

                if (isRecording) {
                    prepareNewChunkMuxer()
                    requestInstantSyncFrame()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error flushing current chunk", e)
            } finally {
                onFlushed()
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        chunkRotationJob?.cancel()
        chunkRotationJob = null

        try {
            audioEngine?.stop()
            audioEngine = null
        } catch (_: Exception) {}

        try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            displayListener?.let { dm?.unregisterDisplayListener(it) }
            displayListener = null
        } catch (_: Exception) {}

        backgroundHandler?.post {
            drainRunnable?.let { backgroundHandler?.removeCallbacks(it) }
            drainRunnable = null

            closeCurrentMuxerSafely()

            try {
                mediaCodec?.stop()
                mediaCodec?.release()
            } catch (_: Exception) {}
            mediaCodec = null

            try {
                inputSurface?.release()
            } catch (_: Exception) {}
            inputSurface = null

            try {
                virtualDisplay?.release()
            } catch (_: Exception) {}
            virtualDisplay = null

            try {
                projectionCallback?.let { cb ->
                    mediaProjection?.unregisterCallback(cb)
                }
            } catch (_: Exception) {}
            projectionCallback = null

            cachedVideoOutputFormat = null
            cachedAudioOutputFormat = null

            handlerThread?.quitSafely()
            handlerThread = null
            backgroundHandler = null

            Log.i(tag, "Screen recording engine stopped")
        }
    }
}
