package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import com.example.data.local.RecordingDao
import com.example.data.local.RecordingEntity
import com.example.data.model.RecordingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(
  private val context: Context,
  private val dao: RecordingDao
) {
  val recordings: Flow<List<RecordingItem>> = dao.getAllRecordings().map { list ->
    list.map { it.toDomain() }
  }

  suspend fun insertRecording(item: RecordingItem): Long = withContext(Dispatchers.IO) {
    dao.insertRecording(RecordingEntity.fromDomain(item))
  }

  suspend fun updateFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
    dao.updateFavorite(id, isFavorite)
  }

  suspend fun renameRecording(item: RecordingItem, newTitle: String): Boolean = withContext(Dispatchers.IO) {
    try {
      val sanitizedTitle = newTitle.trim().replace(Regex("[^a-zA-Z0-9._ -]"), "_")
      val newDisplayName = if (sanitizedTitle.endsWith(".mp4", ignoreCase = true)) sanitizedTitle else "$sanitizedTitle.mp4"

      // 1. Update MediaStore if URI is valid
      val uri = Uri.parse(item.contentUriString)
      if (item.contentUriString.startsWith("content://")) {
        val values = ContentValues().apply {
          put(MediaStore.Video.Media.TITLE, sanitizedTitle.removeSuffix(".mp4"))
          put(MediaStore.Video.Media.DISPLAY_NAME, newDisplayName)
        }
        try {
          context.contentResolver.update(uri, values, null, null)
        } catch (_: Exception) {
          // May throw RecoverableSecurityException on some Android versions if not owner, ignore
        }
      }

      // 2. Rename physical file if it exists
      val file = File(item.filePath)
      var finalPath = item.filePath
      if (file.exists()) {
        val parent = file.parentFile
        if (parent != null) {
          val destFile = File(parent, newDisplayName)
          if (file.renameTo(destFile)) {
            finalPath = destFile.absolutePath
          }
        }
      }

      // 3. Update in Room DB
      dao.updateRecording(
        RecordingEntity(
          id = item.id,
          title = sanitizedTitle.removeSuffix(".mp4"),
          filePath = finalPath,
          contentUriString = item.contentUriString,
          durationMs = item.durationMs,
          fileSizeBytes = item.fileSizeBytes,
          dateAddedMs = item.dateAddedMs,
          resolution = item.resolution,
          fps = item.fps,
          isFavorite = item.isFavorite
        )
      )
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  suspend fun deleteRecording(item: RecordingItem): Boolean = withContext(Dispatchers.IO) {
    try {
      // 1. Delete from MediaStore
      if (item.contentUriString.startsWith("content://")) {
        val uri = Uri.parse(item.contentUriString)
        try {
          context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
          // Handled gracefully
        }
      }

      // 2. Delete local file if still present
      val file = File(item.filePath)
      if (file.exists()) {
        file.delete()
      }

      // 3. Delete from Room DB
      dao.deleteById(item.id)
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  suspend fun syncWithMediaStore() = withContext(Dispatchers.IO) {
    try {
      val resolver: ContentResolver = context.contentResolver
      val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
      } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
      }

      val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_ADDED
      )

      // Look for videos in ScreenRecordings or containing "ScreenRecord"
      val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Video.Media.DATA} LIKE ?"
      val selectionArgs = arrayOf("%ScreenRecord%", "%ScreenRecordings%")

      resolver.query(collection, projection, selection, selectionArgs, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
        val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
        val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
          val mediaId = cursor.getLong(idCol)
          val uri = ContentUris.withAppendedId(collection, mediaId)
          val name = cursor.getString(nameCol) ?: "ScreenRecord_$mediaId.mp4"
          val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
          val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
          val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
          val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()

          val existing = dao.getRecordingByPath(path)
          if (existing == null) {
            dao.insertRecording(
              RecordingEntity(
                title = name.removeSuffix(".mp4"),
                filePath = path,
                contentUriString = uri.toString(),
                durationMs = duration,
                fileSizeBytes = size,
                dateAddedMs = dateAdded,
                resolution = "1080p",
                fps = 60
              )
            )
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  suspend fun loadThumbnail(contentUriString: String, filePath: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && contentUriString.startsWith("content://")) {
        val uri = Uri.parse(contentUriString)
        try {
          return@withContext context.contentResolver.loadThumbnail(uri, Size(320, 180), CancellationSignal())
        } catch (_: Exception) {
          // Fallback to retriever
        }
      }

      val retriever = MediaMetadataRetriever()
      try {
        if (contentUriString.startsWith("content://")) {
          retriever.setDataSource(context, Uri.parse(contentUriString))
        } else if (File(filePath).exists()) {
          retriever.setDataSource(filePath)
        }
        val bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          ?: retriever.getFrameAtTime(0)
        retriever.release()
        return@withContext bitmap
      } catch (e: Exception) {
        retriever.release()
      }
    } catch (_: Exception) {
    }
    null
  }
}
