package com.example.data.model

data class RecordingItem(
  val id: Long = 0,
  val title: String,
  val filePath: String,
  val contentUriString: String,
  val durationMs: Long,
  val fileSizeBytes: Long,
  val dateAddedMs: Long,
  val resolution: String = "1080p",
  val fps: Int = 60,
  val isFavorite: Boolean = false
) {
  val formattedDuration: String
    get() {
      val totalSeconds = durationMs / 1000
      val minutes = totalSeconds / 60
      val seconds = totalSeconds % 60
      val hours = minutes / 60
      return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
      } else {
        String.format("%02d:%02d", minutes, seconds)
      }
    }

  val formattedFileSize: String
    get() {
      val kb = fileSizeBytes / 1024.0
      val mb = kb / 1024.0
      val gb = mb / 1024.0
      return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.0f KB", kb)
      }
    }

  val formattedDate: String
    get() {
      val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
      return sdf.format(java.util.Date(dateAddedMs))
    }
}
