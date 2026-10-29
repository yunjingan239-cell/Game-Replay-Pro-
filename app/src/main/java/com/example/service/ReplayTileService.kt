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
 * - Single tap: Toggles instant replay recording on/off.
 * - Long press: Handled via ACTION_QS_TILE_PREFERENCES in MainActivity to directly open the app.
 * - Strict state sync: Always verifies live ReplayRecorderService status in onStartListening()
 *   so background force-kill never leaves the tile in a stale active state.
 */
class ReplayTileService : TileService() {

    private val tag = "ReplayTileService"
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onTileAdded() {
        super.onTileAdded()
        val isBuffering = ReplayRecorderService.syncAndVerifyState(this)
        updateTileState(isBuffering)
    }

    override fun onStartListening() {
        super.onStartListening()
        // Verify live OS service status so if app was force-closed in background, tile immediately resets to INACTIVE
        val initialBuffering = ReplayRecorderService.syncAndVerifyState(this)
        updateTileState(initialBuffering)

        serviceJob?.cancel()
        serviceJob = scope.launch {
            ReplayRecorderService.serviceStatus.collectLatest { status ->
                val isTrulyBuffering = status.isBuffering && ReplayRecorderService.syncAndVerifyState(this@ReplayTileService)
                updateTileState(isTrulyBuffering)
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
        val isCurrentlyBuffering = ReplayRecorderService.syncAndVerifyState(this)

        if (isCurrentlyBuffering) {
            ReplayRecorderService.stop(this)
            FloatingOverlayService.stop(this)
            updateTileState(false)
            Toast.makeText(this, "回录已关闭", Toast.LENGTH_SHORT).show()
        } else {
            // Ensure tile visual state reflects inactive until user confirms MediaProjection
            updateTileState(false)
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
                    context.applicationContext,
                    ComponentName(context.applicationContext, ReplayTileService::class.java)
                )
            } catch (e: Exception) {
                Log.w("ReplayTileService", "Failed to request tile listening state: ${e.message}")
            }
        }
    }
}
