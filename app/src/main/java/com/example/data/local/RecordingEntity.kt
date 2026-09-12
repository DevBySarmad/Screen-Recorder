package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.RecordingItem

@Entity(tableName = "recordings")
data class RecordingEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val filePath: String,
  val contentUriString: String,
  val durationMs: Long,
  val fileSizeBytes: Long,
  val dateAddedMs: Long,
  val resolution: String,
  val fps: Int,
  val isFavorite: Boolean = false
) {
  fun toDomain(): RecordingItem = RecordingItem(
    id = id,
    title = title,
    filePath = filePath,
    contentUriString = contentUriString,
    durationMs = durationMs,
    fileSizeBytes = fileSizeBytes,
    dateAddedMs = dateAddedMs,
    resolution = resolution,
    fps = fps,
    isFavorite = isFavorite
  )

  companion object {
    fun fromDomain(item: RecordingItem): RecordingEntity = RecordingEntity(
      id = item.id,
      title = item.title,
      filePath = item.filePath,
      contentUriString = item.contentUriString,
      durationMs = item.durationMs,
      fileSizeBytes = item.fileSizeBytes,
      dateAddedMs = item.dateAddedMs,
      resolution = item.resolution,
      fps = item.fps,
      isFavorite = item.isFavorite
    )
  }
}
