package com.example.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Android Quick Settings Tile Service (下拉控制中心快捷控制开关).
 * Allows users to effortlessly toggle instant replay directly from the system control center shade.
 * Displays real-time recording status and enables 1-tap full screen instant capture.
 */
class ReplayTileService : TileService() {

    private val tag = "ReplayTileService"
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        // Immediately render latest state synchronously
        updateTileState(ReplayRecorderService.serviceStatus.value.isBuffering)
        serviceJob?.cancel()
        serviceJob = scope.launch {
            ReplayRecorderService.serviceStatus.collectLatest { status ->
                updateTileState(status.isBuffering)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        serviceJob?.cancel()
        serviceJob = null
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun {
                performClickAction()
            }
        } else {
            performClickAction()
        }
    }

    private fun performClickAction() {
        val isCurrentlyBuffering = ReplayRecorderService.serviceStatus.value.isBuffering

        if (isCurrentlyBuffering) {
            // 开启之后再次点击是关闭回录包括浮窗，显示简洁气泡
            ReplayRecorderService.stop(this)
            FloatingOverlayService.stop(this)
            updateTileState(false)
            Toast.makeText(this, "回录已关闭", Toast.LENGTH_SHORT).show()
        } else {
            // If not recording, launch transparent full-screen prompt activity
            val intent = Intent(this, ReplayTilePromptActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTileState(isBuffering: Boolean) {
        val tile = qsTile ?: return
        try {
            if (isBuffering) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_replay_label)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_replay)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.tile_subtitle_active)
                }
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_replay_label)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_replay)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.tile_subtitle_inactive)
                }
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.w(tag, "Failed to update QS tile: ${e.message}")
        }
    }

    companion object {
        fun requestTileUpdate(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, ReplayTileService::class.java)
                )
            } catch (e: Exception) {
                Log.w("ReplayTileService", "Failed to request tile listening state: ${e.message}")
            }
        }
    }
}
