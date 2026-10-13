package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GameReplayApplication
import com.example.data.local.ReplayClip
import com.example.data.model.ReplaySettings
import com.example.service.FloatingOverlayService
import com.example.service.ReplayRecorderService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isRunning: Boolean = false,
    val isBuffering: Boolean = false,
    val isSavingClip: Boolean = false,
    val bufferedSeconds: Int = 0,
    val bufferedChunkCount: Int = 0,
    val bufferedSizeBytes: Long = 0L,
    val targetBufferDuration: Int = 60,
    val targetGameTitle: String = "王者荣耀",
    val isOverlayEnabled: Boolean = true,
    val hasOverlayPermission: Boolean = false,
    val encoderName: String = "Hardware HEVC Encoder",
    val codecMime: String = "HEVC (H.265)",
    val isHardwareAccelerated: Boolean = true,
    val isHdrActive: Boolean = false,
    val currentOrientation: String = "横屏对局 (16:9)",
    val activeResolution: String = "1920x1080",
    val activeFps: Int = 60,
    val compressionRatio: String = "HEVC 50%高压缩",
    val detectedGames: List<com.example.engine.InstalledGame> = emptyList()
)

class ReplayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GameReplayApplication).repository
    private val gameDetectionManager = com.example.engine.GameDetectionManager(application)

    private val _detectedGames = kotlinx.coroutines.flow.MutableStateFlow(gameDetectionManager.getInstalledGames())
    val detectedGames = _detectedGames.asStateFlow()

    val clips: StateFlow<List<ReplayClip>> = repository.allClips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<ReplaySettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReplaySettings()
        )

    val totalStorageUsed: StateFlow<Long?> = repository.totalStorageUsed
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val dashboardState: StateFlow<DashboardUiState> = combine(
        ReplayRecorderService.serviceStatus,
        repository.settings,
        _detectedGames
    ) { status, currentSettings, games ->
        val context = getApplication<Application>()
        DashboardUiState(
            isRunning = status.isRunning,
            isBuffering = status.isBuffering,
            isSavingClip = status.isSavingClip,
            bufferedSeconds = status.bufferedSeconds,
            bufferedChunkCount = status.bufferedChunkCount,
            bufferedSizeBytes = status.bufferedBytes,
            targetBufferDuration = currentSettings.bufferDurationSeconds,
            targetGameTitle = currentSettings.currentGameTitle,
            isOverlayEnabled = currentSettings.floatingButtonEnabled,
            hasOverlayPermission = Settings.canDrawOverlays(context),
            encoderName = status.encoderName,
            codecMime = status.codecMime,
            isHardwareAccelerated = status.isHardwareAccelerated,
            isHdrActive = status.isHdrActive,
            currentOrientation = status.currentOrientation,
            activeResolution = status.activeResolution,
            activeFps = status.activeFps,
            compressionRatio = status.compressionRatio,
            detectedGames = games
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun refreshInstalledGames() {
        _detectedGames.value = gameDetectionManager.getInstalledGames()
    }

    fun launchGameAndSelect(game: com.example.engine.InstalledGame) {
        val current = settings.value
        updateSettings(current.copy(currentGameTitle = game.title))
        gameDetectionManager.launchGame(game.packageName)
    }

    fun startReplayEngine(resultCode: Int, resultData: Intent?) {
        val context = getApplication<Application>()
        ReplayRecorderService.start(context, resultCode, resultData)

        if (settings.value.floatingButtonEnabled && Settings.canDrawOverlays(context)) {
            FloatingOverlayService.start(context)
        }
    }

    fun stopReplayEngine() {
        val context = getApplication<Application>()
        ReplayRecorderService.stop(context)
        FloatingOverlayService.stop(context)
    }

    fun triggerSaveReplay(customTitle: String? = null) {
        val context = getApplication<Application>()
        ReplayRecorderService.triggerSaveClip(context, customTitle)
    }

    fun deleteClip(clip: ReplayClip) {
        viewModelScope.launch {
            repository.deleteClip(clip)
        }
    }

    fun updateClipTitle(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateClipTitle(id, newTitle)
        }
    }

    fun toggleFavorite(clip: ReplayClip) {
        viewModelScope.launch {
            repository.toggleFavorite(clip)
        }
    }

    fun updateSettings(newSettings: ReplaySettings) {
        repository.updateSettings(newSettings)
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        val context = getApplication<Application>()
        val newSettings = settings.value.copy(floatingButtonEnabled = enabled)
        repository.updateSettings(newSettings)

        if (enabled && Settings.canDrawOverlays(context)) {
            FloatingOverlayService.start(context)
        } else {
            FloatingOverlayService.stop(context)
        }
    }

    fun clearBufferCache() {
        viewModelScope.launch {
            repository.clearCacheBuffer()
        }
    }
}
