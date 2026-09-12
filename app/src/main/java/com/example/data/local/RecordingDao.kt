package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
  @Query("SELECT * FROM recordings ORDER BY dateAddedMs DESC")
  fun getAllRecordings(): Flow<List<RecordingEntity>>

  @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
  suspend fun getRecordingById(id: Long): RecordingEntity?

  @Query("SELECT * FROM recordings WHERE filePath = :path LIMIT 1")
  suspend fun getRecordingByPath(path: String): RecordingEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecording(entity: RecordingEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAll(entities: List<RecordingEntity>)

  @Update
  suspend fun updateRecording(entity: RecordingEntity)

  @Query("UPDATE recordings SET title = :newTitle WHERE id = :id")
  suspend fun updateTitle(id: Long, newTitle: String)

  @Query("UPDATE recordings SET isFavorite = :isFavorite WHERE id = :id")
  suspend fun updateFavorite(id: Long, isFavorite: Boolean)

  @Delete
  suspend fun deleteRecording(entity: RecordingEntity)

  @Query("DELETE FROM recordings WHERE id = :id")
  suspend fun deleteById(id: Long)
}
