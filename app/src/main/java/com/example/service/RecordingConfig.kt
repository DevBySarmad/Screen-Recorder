package com.example.service

enum class RecordingResolution(
  val width: Int,
  val height: Int,
  val defaultBitrate: Int,
  val label: String
) {
  RES_4K(2160, 3840, 28_000_000, "4K UHD"),
  RES_2K(1440, 2560, 16_000_000, "2K QHD"),
  RES_1080P(1080, 1920, 10_000_000, "1080p FHD"),
  RES_720P(720, 1280, 5_000_000, "720p HD"),
  RES_480P(480, 854, 2_500_000, "480p SD");

  fun getDimensionForOrientation(isLandscape: Boolean): Pair<Int, Int> {
    return if (isLandscape) {
      Pair(maxOf(width, height), minOf(width, height))
    } else {
      Pair(minOf(width, height), maxOf(width, height))
    }
  }
}

enum class AudioSourceOption(val label: String, val description: String) {
  MUTE("Mute", "Video only (no audio)"),
  SYSTEM("System Sounds", "Device internal audio"),
  MIC("Mic", "Microphone voice commentary"),
  SYSTEM_AND_MIC("System + Mic", "Internal audio & microphone")
}

data class RecordingConfig(
  val resolution: RecordingResolution = RecordingResolution.RES_1080P,
  val fps: Int = 60,
  val audioSource: AudioSourceOption = AudioSourceOption.MIC,
  val showFloatingOverlay: Boolean = true
) {
  // Backward compatibility property for existing callers
  val recordAudio: Boolean get() = audioSource != AudioSourceOption.MUTE
}
