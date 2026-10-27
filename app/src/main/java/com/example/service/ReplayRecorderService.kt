package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.GameReplayApplication
import com.example.MainActivity
import com.example.R
import com.example.engine.ScreenRecordingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class ServiceStatus(
    val isRunning: Boolean = false,
    val isBuffering: Boolean = false,
    val isSavingClip: Boolean = false,
    val bufferedChunkCount: Int = 0,
    val bufferedSeconds: Int = 0,
    val bufferedBytes: Long = 0L,
    val encoderName: String = "Hardware HEVC Encoder",
    val codecMime: String = "HEVC (H.265)",
    val isHardwareAccelerated: Boolean = true,
    val isHdrActive: Boolean = false,
    val currentOrientation: String = "横屏对局 (16:9)",
    val activeResolution: String = "1920x1080",
    val activeFps: Int = 60,
    val compressionRatio: String = "HEVC 50%高压缩"
)

/**
 * Foreground Service hosting the instant replay rolling circular buffer engine.
 * Includes battery optimization, partial wake lock management during gameplay,
 * and seamless background notification controls.
 */
class ReplayRecorderService : Service() {

    private val tag = "ReplayRecorderService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var recordingEngine: ScreenRecordingEngine? = null
    private var mediaProjection: MediaProjection? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(tag, "Screen off detected — background buffer idling")
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(tag, "Screen on detected — buffer active")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                acquireWakeLock()
                startForegroundServiceWithNotification()
                initializeAndStartEngine(resultCode, resultData)
            }
            ACTION_STOP -> {
                FloatingOverlayService.stop(this)
                stopReplayService()
            }
            ACTION_SAVE_CLIP -> {
                saveReplayClip(intent.getStringExtra(EXTRA_CUSTOM_TITLE))
            }
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GameReplayPro::RecordingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.w(tag, "WakeLock acquisition skipped: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (_: Exception) {}
    }

    private fun startForegroundServiceWithNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val saveClipIntent = Intent(this, ReplayRecorderService::class.java).apply {
            action = ACTION_SAVE_CLIP
        }
        val saveClipPendingIntent = PendingIntent.getService(
            this, 1, saveClipIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ReplayRecorderService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, GameReplayApplication.CHANNEL_ID_REPLAY)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_stat_replay)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, getString(R.string.notification_action_save), saveClipPendingIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(GameReplayApplication.NOTIFICATION_ID_RECORDER, notification, serviceType)
        } else {
            startForeground(GameReplayApplication.NOTIFICATION_ID_RECORDER, notification)
        }
        _serviceStatus.value = _serviceStatus.value.copy(isRunning = true)
    }

    private fun initializeAndStartEngine(resultCode: Int, resultData: Intent?) {
        val app = application as GameReplayApplication
        val repo = app.repository
        val settings = repo.settings.value

        if (resultCode != 0 && resultData != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
        }

        recordingEngine = ScreenRecordingEngine(
            context = this,
            bufferManager = repo.bufferManager,
            onChunkSaved = { _, _ ->
                _serviceStatus.value = _serviceStatus.value.copy(
                    bufferedChunkCount = repo.bufferManager.getChunkCount(),
                    bufferedSeconds = repo.bufferManager.getBufferedDurationSeconds(),
                    bufferedBytes = repo.bufferManager.getBufferedSizeBytes()
                )
            },
            onTelemetryUpdate = { telemetry ->
                _serviceStatus.value = _serviceStatus.value.copy(
                    encoderName = telemetry.encoderName,
                    codecMime = telemetry.codecMime,
                    isHardwareAccelerated = telemetry.isHardwareAccelerated,
                    isHdrActive = telemetry.isHdrActive,
                    currentOrientation = telemetry.currentOrientation,
                    activeResolution = telemetry.activeResolution,
                    activeFps = telemetry.activeFps,
                    compressionRatio = telemetry.compressionRatio
                )
            },
            onError = { err ->
                Log.e(tag, "Recording engine error: $err")
            }
        )

        recordingEngine?.startRecording(mediaProjection, settings)
        _serviceStatus.value = _serviceStatus.value.copy(isBuffering = true)
        ReplayTileService.requestTileUpdate(this)
    }

    private fun saveReplayClip(customTitle: String?) {
        // Prevent duplicate overlapping save requests if already saving
        if (_serviceStatus.value.isSavingClip) {
            return
        }

        vibratePhone()
        _serviceStatus.value = _serviceStatus.value.copy(isSavingClip = true)
        Toast.makeText(this, "正在保存精彩瞬间...", Toast.LENGTH_SHORT).show()

        serviceScope.launch {
            try {
                val app = application as GameReplayApplication
                val repo = app.repository
                val settings = repo.settings.value

                // Bounded timeout (1200ms max) ensures flushCurrentChunk can never block saving indefinitely
                withTimeoutOrNull(1200L) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        val engine = recordingEngine
                        if (engine != null) {
                            engine.flushCurrentChunk(settings) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                        } else {
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                }

                val savedClip = repo.saveCurrentBufferAsReplay(customTitle)
                _serviceStatus.value = _serviceStatus.value.copy(
                    isSavingClip = false,
                    bufferedChunkCount = repo.bufferManager.getChunkCount(),
                    bufferedSeconds = repo.bufferManager.getBufferedDurationSeconds(),
                    bufferedBytes = repo.bufferManager.getBufferedSizeBytes()
                )

                if (savedClip != null) {
                    Toast.makeText(this@ReplayRecorderService, "已保存精彩瞬间", Toast.LENGTH_SHORT).show()
                    Log.i(tag, "Replay successfully saved: ${savedClip.title}")
                } else {
                    Toast.makeText(this@ReplayRecorderService, "保存失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving replay clip", e)
                _serviceStatus.value = _serviceStatus.value.copy(isSavingClip = false)
                Toast.makeText(this@ReplayRecorderService, "保存异常", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Vibration skipped: ${e.message}")
        }
    }

    private fun stopReplayService() {
        recordingEngine?.stopRecording()
        recordingEngine = null

        mediaProjection?.stop()
        mediaProjection = null

        releaseWakeLock()

        _serviceStatus.value = ServiceStatus(
            isRunning = false,
            isBuffering = false,
            isSavingClip = false,
            bufferedChunkCount = 0,
            bufferedSeconds = 0,
            bufferedBytes = 0L
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        ReplayTileService.requestTileUpdate(this)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {}
        stopReplayService()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.example.action.START_REPLAY"
        const val ACTION_STOP = "com.example.action.STOP_REPLAY"
        const val ACTION_SAVE_CLIP = "com.example.action.SAVE_CLIP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_CUSTOM_TITLE = "extra_custom_title"

        private val _serviceStatus = MutableStateFlow(ServiceStatus())
        val serviceStatus = _serviceStatus.asStateFlow()

        fun start(context: Context, resultCode: Int, resultData: Intent?) {
            val intent = Intent(context, ReplayRecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReplayRecorderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerSaveClip(context: Context, customTitle: String? = null) {
            val intent = Intent(context, ReplayRecorderService::class.java).apply {
                action = ACTION_SAVE_CLIP
                if (customTitle != null) putExtra(EXTRA_CUSTOM_TITLE, customTitle)
            }
            context.startService(intent)
        }
    }
}
