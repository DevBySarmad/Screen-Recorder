package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ScreenRecorderApp
import com.example.data.model.RecordingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

  companion object {
    const val CHANNEL_ID = "screen_record_channel"
    const val NOTIFICATION_ID = 1001

    const val ACTION_START = "com.example.action.START_RECORDING"
    const val ACTION_PAUSE = "com.example.action.PAUSE_RECORDING"
    const val ACTION_RESUME = "com.example.action.RESUME_RECORDING"
    const val ACTION_STOP = "com.example.action.STOP_RECORDING"

    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_PROJECTION_DATA = "extra_projection_data"
    const val EXTRA_RESOLUTION = "extra_resolution"
    const val EXTRA_FPS = "extra_fps"
    const val EXTRA_RECORD_AUDIO = "extra_record_audio"
    const val EXTRA_AUDIO_SOURCE = "extra_audio_source"
    const val EXTRA_SHOW_OVERLAY = "extra_show_overlay"

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    fun start(
      context: Context,
      resultCode: Int,
      data: Intent,
      config: RecordingConfig
    ) {
      val intent = Intent(context, ScreenRecordService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_RESULT_CODE, resultCode)
        putExtra(EXTRA_PROJECTION_DATA, data)
        putExtra(EXTRA_RESOLUTION, config.resolution.name)
        putExtra(EXTRA_FPS, config.fps)
        putExtra(EXTRA_RECORD_AUDIO, config.recordAudio)
        putExtra(EXTRA_AUDIO_SOURCE, config.audioSource.name)
        putExtra(EXTRA_SHOW_OVERLAY, config.showFloatingOverlay)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun pause(context: Context) {
      val intent = Intent(context, ScreenRecordService::class.java).apply {
        action = ACTION_PAUSE
      }
      context.startService(intent)
    }

    fun resume(context: Context) {
      val intent = Intent(context, ScreenRecordService::class.java).apply {
        action = ACTION_RESUME
      }
      context.startService(intent)
    }

    fun stop(context: Context) {
      val intent = Intent(context, ScreenRecordService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }
  }

  private var mediaProjection: MediaProjection? = null
  private var mediaRecorder: MediaRecorder? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var outputPfd: ParcelFileDescriptor? = null
  private var outputUri: Uri? = null
  private var outputFilePath: String = ""
  private var recordingTitle: String = ""

  private var floatingOverlayController: FloatingOverlayController? = null
  private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
  private var timerJob: Job? = null
  private var elapsedSeconds: Long = 0
  private var isPaused = false
  private var startTimeMs: Long = 0
  private var currentConfig = RecordingConfig()

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> handleStart(intent)
      ACTION_PAUSE -> handlePause()
      ACTION_RESUME -> handleResume()
      ACTION_STOP -> handleStop()
    }
    return START_NOT_STICKY
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Screen Recording Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Ongoing notification and controls during screen capture"
        setShowBadge(false)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(text: String, isRecording: Boolean, isPaused: Boolean): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      this, 0, openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP }
    val stopPendingIntent = PendingIntent.getService(
      this, 1, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_menu_camera)
      .setContentTitle("Screen Recorder")
      .setContentText(text)
      .setContentIntent(contentPendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)

    if (isRecording) {
      if (isPaused) {
        val resumeIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_RESUME }
        val resumePendingIntent = PendingIntent.getService(
          this, 2, resumeIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
      } else {
        val pauseIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
          this, 3, pauseIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
      }
      builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
    }

    return builder.build()
  }

  private fun handleStart(intent: Intent) {
    val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
    val projectionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
    }

    if (resultCode == 0 || projectionData == null) {
      _recordingState.value = RecordingState.Error("Media projection permission not provided.")
      stopSelf()
      return
    }

    val resName = intent.getStringExtra(EXTRA_RESOLUTION) ?: RecordingResolution.RES_1080P.name
    val resolution = try {
      RecordingResolution.valueOf(resName)
    } catch (_: Exception) {
      RecordingResolution.RES_1080P
    }
    val fps = intent.getIntExtra(EXTRA_FPS, 60)
    val audioSourceName = intent.getStringExtra(EXTRA_AUDIO_SOURCE)
    val audioSource = if (audioSourceName != null) {
      try {
        AudioSourceOption.valueOf(audioSourceName)
      } catch (_: Exception) {
        AudioSourceOption.MIC
      }
    } else {
      if (intent.getBooleanExtra(EXTRA_RECORD_AUDIO, true)) AudioSourceOption.MIC else AudioSourceOption.MUTE
    }
    val showOverlay = intent.getBooleanExtra(EXTRA_SHOW_OVERLAY, true)

    currentConfig = RecordingConfig(
      resolution = resolution,
      fps = fps,
      audioSource = audioSource,
      showFloatingOverlay = showOverlay
    )

    // Android 14+ requirement: startForeground with FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    // MUST occur before getMediaProjection is invoked!
    val initialNotification = buildNotification("Initializing recorder…", isRecording = true, isPaused = false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    } else {
      startForeground(NOTIFICATION_ID, initialNotification)
    }

    val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    try {
      mediaProjection = projectionManager.getMediaProjection(resultCode, projectionData)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
          override fun onStop() {
            handleStop()
          }
        }, Handler(Looper.getMainLooper()))
      }
    } catch (e: Exception) {
      e.printStackTrace()
      _recordingState.value = RecordingState.Error("Failed to acquire MediaProjection: ${e.localizedMessage}")
      stopSelf()
      return
    }

    startRecordingSession()
  }

  private fun startRecordingSession() {
    try {
      val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      windowManager.defaultDisplay.getRealMetrics(metrics)

      val isLandscape = metrics.widthPixels > metrics.heightPixels
      val dims = currentConfig.resolution.getDimensionForOrientation(isLandscape)
      val width = dims.first
      val height = dims.second
      val densityDpi = metrics.densityDpi

      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      recordingTitle = "ScreenRecord_$timeStamp"
      val fileName = "$recordingTitle.mp4"

      // Initialize MediaRecorder
      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(this)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }

      var audioConfigured = false
      when (currentConfig.audioSource) {
        AudioSourceOption.MUTE -> {
          // Mute: No audio track configured
          audioConfigured = false
        }
        AudioSourceOption.SYSTEM -> {
          try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            } else {
              recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            }
            audioConfigured = true
          } catch (e: Exception) {
            e.printStackTrace()
            try {
              recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
              audioConfigured = true
            } catch (_: Exception) {}
          }
        }
        AudioSourceOption.MIC -> {
          try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            audioConfigured = true
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
        AudioSourceOption.SYSTEM_AND_MIC -> {
          try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            audioConfigured = true
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }

      recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
      recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

      if (audioConfigured) {
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioEncodingBitRate(128_000)
        recorder.setAudioSamplingRate(44_100)
      }

      recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
      recorder.setVideoSize(width, height)
      recorder.setVideoFrameRate(currentConfig.fps)
      recorder.setVideoEncodingBitRate(currentConfig.resolution.defaultBitrate)

      // MediaStore Output
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
          put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ScreenRecordings")
          put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
          outputUri = uri
          outputPfd = contentResolver.openFileDescriptor(uri, "rw")
          outputPfd?.let {
            recorder.setOutputFile(it.fileDescriptor)
          }
        } else {
          fallbackToFile(recorder, fileName)
        }
      } else {
        fallbackToFile(recorder, fileName)
      }

      recorder.prepare()
      mediaRecorder = recorder

      // Virtual Display
      virtualDisplay = mediaProjection?.createVirtualDisplay(
        "ScreenRecorderDisplay",
        width,
        height,
        densityDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        recorder.surface,
        null,
        null
      )

      recorder.start()
      startTimeMs = System.currentTimeMillis()
      elapsedSeconds = 0
      isPaused = false

      _recordingState.value = RecordingState.Recording(
        durationSec = 0,
        isAudioEnabled = audioConfigured
      )

      // Floating Overlay
      if (currentConfig.showFloatingOverlay) {
        floatingOverlayController = FloatingOverlayController(
          context = this,
          onPauseResumeClicked = {
            if (isPaused) handleResume() else handlePause()
          },
          onStopClicked = {
            handleStop()
          }
        )
        floatingOverlayController?.show()
      }

      startTimer()
      updateNotification("Recording active • 00:00")
    } catch (e: Exception) {
      e.printStackTrace()
      _recordingState.value = RecordingState.Error("Error starting recording: ${e.localizedMessage}")
      cleanup()
      stopSelf()
    }
  }

  private fun fallbackToFile(recorder: MediaRecorder, fileName: String) {
    val moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
    val file = File(moviesDir, fileName)
    outputFilePath = file.absolutePath
    recorder.setOutputFile(outputFilePath)
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob = serviceScope.launch {
      while (isActive) {
        delay(1000)
        if (!isPaused) {
          elapsedSeconds++
          _recordingState.value = RecordingState.Recording(
            durationSec = elapsedSeconds,
            isAudioEnabled = currentConfig.recordAudio
          )
          floatingOverlayController?.updateTimer(elapsedSeconds)

          val mins = elapsedSeconds / 60
          val secs = elapsedSeconds % 60
          val timeStr = String.format("%02d:%02d", mins, secs)
          updateNotification("Recording in progress • $timeStr")
        }
      }
    }
  }

  private fun handlePause() {
    if (!isPaused && mediaRecorder != null) {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          mediaRecorder?.pause()
          isPaused = true
          _recordingState.value = RecordingState.Paused(
            durationSec = elapsedSeconds,
            isAudioEnabled = currentConfig.recordAudio
          )
          floatingOverlayController?.updatePauseState(true)
          updateNotification("Recording paused (${formatTime(elapsedSeconds)})")
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun handleResume() {
    if (isPaused && mediaRecorder != null) {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          mediaRecorder?.resume()
          isPaused = false
          _recordingState.value = RecordingState.Recording(
            durationSec = elapsedSeconds,
            isAudioEnabled = currentConfig.recordAudio
          )
          floatingOverlayController?.updatePauseState(false)
          updateNotification("Recording in progress • ${formatTime(elapsedSeconds)}")
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun handleStop() {
    timerJob?.cancel()
    floatingOverlayController?.hide()
    floatingOverlayController = null

    var finalDurationMs = elapsedSeconds * 1000L
    if (finalDurationMs == 0L) {
      finalDurationMs = System.currentTimeMillis() - startTimeMs
    }

    try {
      mediaRecorder?.stop()
      mediaRecorder?.reset()
    } catch (e: Exception) {
      e.printStackTrace()
    }

    try {
      outputPfd?.close()
    } catch (_: Exception) {}

    // Finalize MediaStore pending status
    val resolvedUri = outputUri
    if (resolvedUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.Video.Media.IS_PENDING, 0)
      }
      try {
        contentResolver.update(resolvedUri, values, null, null)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    // Determine final file path / uri
    var finalPath = outputFilePath
    var fileSize = 0L

    if (resolvedUri != null) {
      val projection = arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE)
      contentResolver.query(resolvedUri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
          val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
          if (dataCol != -1) finalPath = cursor.getString(dataCol) ?: ""
          if (sizeCol != -1) fileSize = cursor.getLong(sizeCol)
        }
      }
    }

    if (fileSize == 0L && finalPath.isNotEmpty()) {
      val f = File(finalPath)
      if (f.exists()) fileSize = f.length()
    }

    val itemUriString = resolvedUri?.toString() ?: Uri.fromFile(File(finalPath)).toString()

    // Save to App Database
    serviceScope.launch(Dispatchers.IO) {
      try {
        val app = applicationContext as? ScreenRecorderApp
        val repo = app?.repository
        if (repo != null) {
          repo.insertRecording(
            RecordingItem(
              title = recordingTitle,
              filePath = finalPath,
              contentUriString = itemUriString,
              durationMs = finalDurationMs,
              fileSizeBytes = fileSize,
              dateAddedMs = System.currentTimeMillis(),
              resolution = currentConfig.resolution.label,
              fps = currentConfig.fps
            )
          )
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    _recordingState.value = RecordingState.Completed(
      uriString = itemUriString,
      filePath = finalPath,
      durationMs = finalDurationMs,
      sizeBytes = fileSize
    )

    cleanup()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun updateNotification(text: String) {
    val notification = buildNotification(text, isRecording = true, isPaused = isPaused)
    val manager = getSystemService(NotificationManager::class.java)
    manager?.notify(NOTIFICATION_ID, notification)
  }

  private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
  }

  private fun cleanup() {
    try {
      virtualDisplay?.release()
      virtualDisplay = null
      mediaProjection?.stop()
      mediaProjection = null
      mediaRecorder?.release()
      mediaRecorder = null
    } catch (_: Exception) {}
  }

  override fun onDestroy() {
    cleanup()
    floatingOverlayController?.hide()
    timerJob?.cancel()
    super.onDestroy()
  }
}
