package com.example.service

sealed interface RecordingState {
  data object Idle : RecordingState

  data class Countdown(val secondsRemaining: Int) : RecordingState

  data class Recording(
    val durationSec: Long = 0,
    val isAudioEnabled: Boolean = true
  ) : RecordingState

  data class Paused(
    val durationSec: Long = 0,
    val isAudioEnabled: Boolean = true
  ) : RecordingState

  data class Completed(
    val uriString: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long
  ) : RecordingState

  data class Error(val message: String) : RecordingState
}
