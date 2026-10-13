package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "replay_clips")
data class ReplayClip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val resolution: String,
    val fps: Int,
    val gameName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
