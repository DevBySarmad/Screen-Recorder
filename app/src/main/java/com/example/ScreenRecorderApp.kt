package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.RecordingRepository

class ScreenRecorderApp : Application() {
  val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
  val repository: RecordingRepository by lazy {
    RecordingRepository(this, database.recordingDao())
  }

  override fun onCreate() {
    super.onCreate()
  }
}
