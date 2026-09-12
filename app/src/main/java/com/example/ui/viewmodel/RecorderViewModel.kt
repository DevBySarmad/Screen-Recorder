package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ScreenRecorderApp
import com.example.data.model.RecordingItem
import com.example.data.repository.RecordingRepository
import com.example.service.AudioSourceOption
import com.example.service.RecordingConfig
import com.example.service.RecordingResolution
import com.example.service.RecordingState
import com.example.service.ScreenRecordService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class RecorderViewModel(
  application: Application,
  private val repository: RecordingRepository
) : AndroidViewModel(application) {

  val recordingState: StateFlow<RecordingState> = ScreenRecordService.recordingState

  private val _config = MutableStateFlow(RecordingConfig())
  val config: StateFlow<RecordingConfig> = _config.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _filterFavoritesOnly = MutableStateFlow(false)
  val filterFavoritesOnly: StateFlow<Boolean> = _filterFavoritesOnly.asStateFlow()

  private val _hasMicrophonePermission = MutableStateFlow(false)
  val hasMicrophonePermission: StateFlow<Boolean> = _hasMicrophonePermission.asStateFlow()

  private val _hasOverlayPermission = MutableStateFlow(false)
  val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

  private val _hasNotificationPermission = MutableStateFlow(false)
  val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

  private val _selectedVideo = MutableStateFlow<RecordingItem?>(null)
  val selectedVideo: StateFlow<RecordingItem?> = _selectedVideo.asStateFlow()

  private val _videoToRename = MutableStateFlow<RecordingItem?>(null)
  val videoToRename: StateFlow<RecordingItem?> = _videoToRename.asStateFlow()

  private val _videoToDelete = MutableStateFlow<RecordingItem?>(null)
  val videoToDelete: StateFlow<RecordingItem?> = _videoToDelete.asStateFlow()

  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  private val _countdown = MutableStateFlow<Int?>(null)
  val countdown: StateFlow<Int?> = _countdown.asStateFlow()

  private var countdownJob: Job? = null

  // Combined filtered recordings
  val filteredRecordings: StateFlow<List<RecordingItem>> = combine(
    repository.recordings,
    _searchQuery,
    _filterFavoritesOnly
  ) { list, query, favOnly ->
    list.filter { item ->
      val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true)
      val matchesFav = !favOnly || item.isFavorite
      matchesQuery && matchesFav
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    syncWithMediaStore()
    checkPermissions()
  }

  fun checkPermissions() {
    val context = getApplication<Application>()
    _hasMicrophonePermission.value = ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    _hasOverlayPermission.value = Settings.canDrawOverlays(context)

    _hasNotificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun syncWithMediaStore() {
    viewModelScope.launch {
      repository.syncWithMediaStore()
    }
  }

  fun updateResolution(resolution: RecordingResolution) {
    _config.value = _config.value.copy(resolution = resolution)
  }

  fun updateFps(fps: Int) {
    _config.value = _config.value.copy(fps = fps)
  }

  fun updateAudioSource(source: AudioSourceOption) {
    _config.value = _config.value.copy(audioSource = source)
  }

  fun updateAudio(enabled: Boolean) {
    _config.value = _config.value.copy(
      audioSource = if (enabled) AudioSourceOption.MIC else AudioSourceOption.MUTE
    )
  }

  fun updateOverlay(enabled: Boolean) {
    _config.value = _config.value.copy(showFloatingOverlay = enabled)
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setFilterFavoritesOnly(enabled: Boolean) {
    _filterFavoritesOnly.value = enabled
  }

  fun startRecordingWithCountdown(resultCode: Int, data: Intent) {
    countdownJob?.cancel()
    countdownJob = viewModelScope.launch {
      _countdown.value = 3
      delay(800)
      _countdown.value = 2
      delay(800)
      _countdown.value = 1
      delay(800)
      _countdown.value = null

      ScreenRecordService.start(
        context = getApplication(),
        resultCode = resultCode,
        data = data,
        config = _config.value
      )
    }
  }

  fun cancelCountdown() {
    countdownJob?.cancel()
    _countdown.value = null
  }

  fun pauseRecording() {
    ScreenRecordService.pause(getApplication())
  }

  fun resumeRecording() {
    ScreenRecordService.resume(getApplication())
  }

  fun stopRecording() {
    ScreenRecordService.stop(getApplication())
  }

  fun selectVideoForPlayback(item: RecordingItem?) {
    _selectedVideo.value = item
  }

  fun promptRename(item: RecordingItem?) {
    _videoToRename.value = item
  }

  fun promptDelete(item: RecordingItem?) {
    _videoToDelete.value = item
  }

  fun dismissUserMessage() {
    _userMessage.value = null
  }

  fun toggleFavorite(item: RecordingItem) {
    viewModelScope.launch {
      repository.updateFavorite(item.id, !item.isFavorite)
    }
  }

  fun renameRecording(item: RecordingItem, newTitle: String) {
    if (newTitle.isBlank()) return
    viewModelScope.launch {
      val success = repository.renameRecording(item, newTitle)
      if (success) {
        _userMessage.value = "Recording renamed to '$newTitle'"
      } else {
        _userMessage.value = "Failed to rename recording"
      }
      _videoToRename.value = null
    }
  }

  fun deleteRecording(item: RecordingItem) {
    viewModelScope.launch {
      val success = repository.deleteRecording(item)
      if (success) {
        _userMessage.value = "Recording deleted"
      } else {
        _userMessage.value = "Failed to delete recording"
      }
      _videoToDelete.value = null
      if (_selectedVideo.value?.id == item.id) {
        _selectedVideo.value = null
      }
    }
  }

  fun shareRecording(context: Context, item: RecordingItem) {
    try {
      val uri = if (item.contentUriString.startsWith("content://")) {
        Uri.parse(item.contentUriString)
      } else {
        val file = File(item.filePath)
        if (file.exists()) {
          FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } else {
          null
        }
      }

      if (uri != null) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
          type = "video/mp4"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_SUBJECT, item.title)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Screen Recording"))
      } else {
        _userMessage.value = "Cannot share video: File not accessible"
      }
    } catch (e: Exception) {
      e.printStackTrace()
      _userMessage.value = "Share failed: ${e.localizedMessage}"
    }
  }

  suspend fun loadThumbnail(item: RecordingItem) = repository.loadThumbnail(item.contentUriString, item.filePath)
}

class RecorderViewModelFactory(
  private val application: Application,
  private val repository: RecordingRepository
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(RecorderViewModel::class.java)) {
      return RecorderViewModel(application, repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
