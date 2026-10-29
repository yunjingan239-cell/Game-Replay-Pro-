package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.repository.ReplayRepository
import com.example.engine.CircularBufferManager
import com.example.engine.ReplayMuxer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameReplayApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ReplayRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        val bufferManager = CircularBufferManager(this)
        val replayMuxer = ReplayMuxer(this)

        repository = ReplayRepository(
            context = this,
            replayDao = database.replayDao(),
            bufferManager = bufferManager,
            replayMuxer = replayMuxer
        )

        createNotificationChannels()

        // Ensure Quick Settings Tile and service status are strictly synchronized on process startup
        com.example.service.ReplayRecorderService.syncAndVerifyState(this)
        com.example.service.ReplayTileService.requestTileUpdate(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            Log.w("GameReplayApp", "System memory pressure detected (level=$level). Purging non-essential scratch cache.")
            CoroutineScope(Dispatchers.IO).launch {
                repository.clearCacheBuffer()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w("GameReplayApp", "Critical low memory event! Purging buffer scratch cache immediately.")
        CoroutineScope(Dispatchers.IO).launch {
            repository.clearCacheBuffer()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_REPLAY,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_REPLAY = "channel_game_replay_pro"
        const val NOTIFICATION_ID_RECORDER = 9001
        const val NOTIFICATION_ID_OVERLAY = 9002

        lateinit var instance: GameReplayApplication
            private set
    }
}
