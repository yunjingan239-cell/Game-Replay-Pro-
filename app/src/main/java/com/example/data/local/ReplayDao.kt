package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplayDao {
    @Query("SELECT * FROM replay_clips ORDER BY createdAt DESC")
    fun getAllClips(): Flow<List<ReplayClip>>

    @Query("SELECT * FROM replay_clips WHERE id = :id LIMIT 1")
    suspend fun getClipById(id: Long): ReplayClip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ReplayClip): Long

    @Update
    suspend fun updateClip(clip: ReplayClip)

    @Delete
    suspend fun deleteClip(clip: ReplayClip)

    @Query("DELETE FROM replay_clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)

    @Query("UPDATE replay_clips SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: Long, newTitle: String)

    @Query("UPDATE replay_clips SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT SUM(fileSizeBytes) FROM replay_clips")
    fun getTotalStorageUsed(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM replay_clips")
    fun getTotalClipsCount(): Flow<Int>
}
