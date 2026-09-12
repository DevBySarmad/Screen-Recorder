package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AudioSourceOption
import com.example.service.RecordingState
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.PermissionsCard
import com.example.ui.components.RecordingConsole
import com.example.ui.components.RenameDialog
import com.example.ui.components.SettingsScreen
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GlassPrimaryPillBrush
import com.example.ui.theme.GlassRecordPillBg
import com.example.ui.theme.GlassRecordRed
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RecorderViewModel
import com.example.ui.viewmodel.RecorderViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: RecorderViewModel by viewModels {
    val app = application as ScreenRecorderApp
    RecorderViewModelFactory(app, app.repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        ScreenRecorderMainScreen(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.checkPermissions()
    viewModel.syncWithMediaStore()
  }
}

@Composable
fun ScreenRecorderMainScreen(viewModel: RecorderViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val recordingState by viewModel.recordingState.collectAsState()
  val config by viewModel.config.collectAsState()
  val countdown by viewModel.countdown.collectAsState()
  val recordings by viewModel.filteredRecordings.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val filterFavOnly by viewModel.filterFavoritesOnly.collectAsState()

  val hasMicPermission by viewModel.hasMicrophonePermission.collectAsState()
  val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
  val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()

  val selectedVideo by viewModel.selectedVideo.collectAsState()
  val videoToRename by viewModel.videoToRename.collectAsState()
  val videoToDelete by viewModel.videoToDelete.collectAsState()
  val userMessage by viewModel.userMessage.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  var isSettingsOpen by remember { mutableStateOf(false) }

  BackHandler(enabled = isSettingsOpen) {
    isSettingsOpen = false
  }

  // MediaProjection Launcher
  val mediaProjectionManager = remember {
    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
  }
  val mediaProjectionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
      viewModel.startRecordingWithCountdown(result.resultCode, result.data!!)
    }
  }

  // Permission Launchers
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) {
    viewModel.checkPermissions()
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) {
    viewModel.checkPermissions()
  }

  val overlaySettingsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {
    viewModel.checkPermissions()
  }

  // Handle user snackbar messages
  LaunchedEffect(userMessage) {
    userMessage?.let { msg ->
      coroutineScope.launch {
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissUserMessage()
      }
    }
  }

  val isSessionActive = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused

  // Ambient Glass Backdrop with iOS 27 refraction
  GlassBackground {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
      BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
      ) {
        val isWideScreen = maxWidth >= 720.dp
        val isTabletLandscape = maxWidth >= 960.dp
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val listBottomPadding = 68.dp + navBarBottomInset

        if (isSettingsOpen) {
          SettingsScreen(
            modifier = Modifier.fillMaxSize(),
            config = config,
            hasMicPermission = hasMicPermission,
            hasOverlayPermission = hasOverlayPermission,
            hasNotificationPermission = hasNotificationPermission,
            onClose = { isSettingsOpen = false },
            onResolutionChange = { viewModel.updateResolution(it) },
            onFpsChange = { viewModel.updateFps(it) },
            onAudioChange = { enabled ->
              viewModel.updateAudio(enabled)
              if (enabled && !hasMicPermission) {
                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
              }
            },
            onAudioSourceChange = { source ->
              viewModel.updateAudioSource(source)
              if ((source == AudioSourceOption.MIC || source == AudioSourceOption.SYSTEM_AND_MIC) && !hasMicPermission) {
                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
              }
            },
            onOverlayChange = { enabled ->
              viewModel.updateOverlay(enabled)
              if (enabled && !hasOverlayPermission) {
                val intent = Intent(
                  Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  Uri.parse("package:${context.packageName}")
                )
                overlaySettingsLauncher.launch(intent)
              }
            },
            onRequestMicPermission = {
              micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            },
            onRequestOverlayPermission = {
              val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
              )
              overlaySettingsLauncher.launch(intent)
            },
            onRequestNotificationPermission = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
              }
            },
            onSyncLibrary = {
              viewModel.syncWithMediaStore()
              viewModel.checkPermissions()
            }
          )
        } else {
          Column(modifier = Modifier.fillMaxSize()) {
            // Top Controls Bar (Clean minimal bar without Screen Recorder header)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isSessionActive) Arrangement.SpaceBetween else Arrangement.End
          ) {
            if (isSessionActive) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(14.dp))
                  .background(GlassRecordPillBg)
                  .border(1.dp, Color(0x66EF4444), RoundedCornerShape(14.dp))
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Rounded.FiberManualRecord,
                    contentDescription = "REC",
                    tint = GlassRecordRed,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = "REC",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFDC2626)
                  )
                }
              }
            }

            Box(
              modifier = Modifier
                .size(42.dp)
                .shadow(4.dp, CircleShape, ambientColor = Color(0x1A000000))
                .clip(CircleShape)
                .background(GlassSurfaceElevated)
                .border(1.dp, Color(0x66FFFFFF), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              IconButton(
                onClick = {
                  isSettingsOpen = true
                },
                modifier = Modifier
                  .size(42.dp)
                  .testTag("settings_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = "Settings",
                  tint = GlassTextPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          // Content Area: Adaptive Dual-Pane for Wide / Tabs for Compact
          if (isWideScreen) {
            // Tablet / Foldable Dual Pane Layout
            Row(
              modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = if (isTabletLandscape) 28.dp else 20.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
              // Left Pane: Centered Studio Console
              Box(
                modifier = Modifier
                  .width(if (isTabletLandscape) 420.dp else 370.dp)
                  .fillMaxHeight(),
                contentAlignment = Alignment.Center
              ) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                  RecordingConsole(
                    recordingState = recordingState,
                    config = config,
                    countdown = countdown,
                    onStartClick = {
                      val needsMic = config.audioSource == AudioSourceOption.MIC || config.audioSource == AudioSourceOption.SYSTEM_AND_MIC
                      if (needsMic && !hasMicPermission) {
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                      } else {
                        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                      }
                    },
                    onCancelCountdown = { viewModel.cancelCountdown() },
                    onPauseClick = { viewModel.pauseRecording() },
                    onResumeClick = { viewModel.resumeRecording() },
                    onStopClick = { viewModel.stopRecording() }
                  )
                }
              }

              // Right Pane: Recordings Library
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              ) {
                RecordingsLibraryView(
                  recordings = recordings,
                  searchQuery = searchQuery,
                  filterFavOnly = filterFavOnly,
                  isGrid = isTabletLandscape,
                  onSearchQueryChange = { viewModel.setSearchQuery(it) },
                  onToggleFavoritesOnly = { viewModel.setFilterFavoritesOnly(!filterFavOnly) },
                  onPlayClick = { viewModel.selectVideoForPlayback(it) },
                  onShareClick = { viewModel.shareRecording(context, it) },
                  onRenameClick = { viewModel.promptRename(it) },
                  onDeleteClick = { viewModel.promptDelete(it) },
                  onToggleFavorite = { viewModel.toggleFavorite(it) },
                  onLoadThumbnail = { viewModel.loadThumbnail(it) }
                )
              }
            }
          } else {
            // Compact Mobile: Screen Tabs + Floating Glass Dock Navigation
            Box(
              modifier = Modifier
                .fillMaxSize()
                .weight(1f)
            ) {
              if (selectedTab == 0) {
                // Studio Tab: Centered Recording Console ("Tap to start")
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = listBottomPadding),
                  contentAlignment = Alignment.Center
                ) {
                  Box(modifier = Modifier.widthIn(max = 520.dp)) {
                    RecordingConsole(
                      recordingState = recordingState,
                      config = config,
                      countdown = countdown,
                      onStartClick = {
                        val needsMic = config.audioSource == AudioSourceOption.MIC || config.audioSource == AudioSourceOption.SYSTEM_AND_MIC
                        if (needsMic && !hasMicPermission) {
                          micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        } else {
                          mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
                      },
                      onCancelCountdown = { viewModel.cancelCountdown() },
                      onPauseClick = { viewModel.pauseRecording() },
                      onResumeClick = { viewModel.resumeRecording() },
                      onStopClick = { viewModel.stopRecording() }
                    )
                  }
                }
              } else {
                // Recordings Tab
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                  RecordingsLibraryView(
                    recordings = recordings,
                    searchQuery = searchQuery,
                    filterFavOnly = filterFavOnly,
                    isGrid = false,
                    contentBottomPadding = listBottomPadding,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleFavoritesOnly = { viewModel.setFilterFavoritesOnly(!filterFavOnly) },
                    onPlayClick = { viewModel.selectVideoForPlayback(it) },
                    onShareClick = { viewModel.shareRecording(context, it) },
                    onRenameClick = { viewModel.promptRename(it) },
                    onDeleteClick = { viewModel.promptDelete(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onLoadThumbnail = { viewModel.loadThumbnail(it) }
                  )
                }
              }

              // Clean Floating Glass Dock Navigation (iPhone iOS 18 & Google Material 3)
              Box(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .navigationBarsPadding()
                  .padding(bottom = 8.dp)
              ) {
                GlassCard(
                  shape = RoundedCornerShape(32.dp),
                  backgroundColor = Color(0xF7FFFFFF),
                  elevation = 8.dp,
                  modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 380.dp)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Studio Dock Pill
                    val isStudio = selectedTab == 0
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .then(
                          if (isStudio) {
                            Modifier
                              .shadow(6.dp, RoundedCornerShape(23.dp), spotColor = Color(0x330284C7))
                              .background(GlassPrimaryPillBrush)
                              .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(23.dp))
                          } else {
                            Modifier.background(Color.Transparent)
                          }
                        )
                        .clickable { selectedTab = 0 }
                        .testTag("tab_studio"),
                      contentAlignment = Alignment.Center
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Videocam,
                          contentDescription = "Studio",
                          tint = if (isStudio) Color.White else GlassTextSecondary,
                          modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "Studio",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = if (isStudio) FontWeight.Bold else FontWeight.Medium,
                          color = if (isStudio) Color.White else GlassTextSecondary
                        )
                      }
                    }

                    // Recordings Dock Pill
                    val isRecordings = selectedTab == 1
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .then(
                          if (isRecordings) {
                            Modifier
                              .shadow(6.dp, RoundedCornerShape(23.dp), spotColor = Color(0x330284C7))
                              .background(GlassPrimaryPillBrush)
                              .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(23.dp))
                          } else {
                            Modifier.background(Color.Transparent)
                          }
                        )
                        .clickable { selectedTab = 1 }
                        .testTag("tab_recordings"),
                      contentAlignment = Alignment.Center
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.VideoLibrary,
                          contentDescription = "Recordings",
                          tint = if (isRecordings) Color.White else GlassTextSecondary,
                          modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "Recordings",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = if (isRecordings) FontWeight.Bold else FontWeight.Medium,
                          color = if (isRecordings) Color.White else GlassTextSecondary
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
        }
      }
    }
  }

  // Video Player Dialog (ExoPlayer with Glass Controls)
  selectedVideo?.let { video ->
    VideoPlayerDialog(
      item = video,
      onDismiss = { viewModel.selectVideoForPlayback(null) },
      onShare = { viewModel.shareRecording(context, video) },
      onDelete = { viewModel.promptDelete(video) }
    )
  }

  // Rename Dialog
  videoToRename?.let { video ->
    RenameDialog(
      item = video,
      onDismiss = { viewModel.promptRename(null) },
      onConfirm = { newTitle ->
        viewModel.renameRecording(video, newTitle)
      }
    )
  }

  // Delete Confirmation Dialog
  videoToDelete?.let { video ->
    DeleteConfirmDialog(
      item = video,
      onDismiss = { viewModel.promptDelete(null) },
      onConfirm = {
        viewModel.deleteRecording(video)
      }
    )
  }
}

/**
 * Responsive Recordings Library View with Glassmorphic Search & Filters.
 */
@Composable
fun RecordingsLibraryView(
  recordings: List<com.example.data.model.RecordingItem>,
  searchQuery: String,
  filterFavOnly: Boolean,
  isGrid: Boolean,
  contentBottomPadding: androidx.compose.ui.unit.Dp = 24.dp,
  onSearchQueryChange: (String) -> Unit,
  onToggleFavoritesOnly: () -> Unit,
  onPlayClick: (com.example.data.model.RecordingItem) -> Unit,
  onShareClick: (com.example.data.model.RecordingItem) -> Unit,
  onRenameClick: (com.example.data.model.RecordingItem) -> Unit,
  onDeleteClick: (com.example.data.model.RecordingItem) -> Unit,
  onToggleFavorite: (com.example.data.model.RecordingItem) -> Unit,
  onLoadThumbnail: suspend (com.example.data.model.RecordingItem) -> android.graphics.Bitmap?
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Glass Search & Filter Control
    GlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 14.dp),
      shape = RoundedCornerShape(22.dp),
      backgroundColor = GlassSurfaceElevated
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Search recorded videos…", color = GlassTextSecondary) },
          leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = GlassTextSecondary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = GlassTextSecondary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0x3DF1F5F9),
            focusedContainerColor = Color(0x80FFFFFF),
            unfocusedBorderColor = Color(0x33CBD5E1),
            focusedBorderColor = GlassAccentBlue
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_recordings_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Favorite Filter Chip
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .then(
                if (filterFavOnly) {
                  Modifier
                    .background(Color(0x26EF4444))
                    .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                } else {
                  Modifier
                    .background(Color(0x3DF1F5F9))
                    .border(1.dp, Color(0x20CBD5E1), RoundedCornerShape(12.dp))
                }
              )
              .clickable { onToggleFavoritesOnly() }
              .padding(horizontal = 14.dp, vertical = 7.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (filterFavOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (filterFavOnly) Color(0xFFEF4444) else GlassTextSecondary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Favorites Only",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (filterFavOnly) FontWeight.Bold else FontWeight.Medium,
                color = if (filterFavOnly) Color(0xFFDC2626) else GlassTextSecondary
              )
            }
          }

          Text(
            text = "${recordings.size} video${if (recordings.size == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = GlassTextSecondary
          )
        }
      }
    }

    // Recordings Content
    if (recordings.isEmpty()) {
      GlassCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 24.dp),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = GlassSurfaceElevated
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(Color(0x260284C7))
              .border(1.5.dp, GlassBorderBrush, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.VideoLibrary,
              contentDescription = null,
              tint = GlassAccentBlue,
              modifier = Modifier.size(38.dp)
            )
          }
          Spacer(modifier = Modifier.height(18.dp))
          Text(
            text = if (searchQuery.isNotEmpty() || filterFavOnly) "No matching recordings" else "No recordings yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (searchQuery.isNotEmpty() || filterFavOnly) "Try changing your search keywords or removing filters" else "Start capturing your mobile screen in 1080p Full HD to populate your library!",
            style = MaterialTheme.typography.bodyMedium,
            color = GlassTextSecondary,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      if (isGrid) {
        // Multi-column responsive grid on large screens/tablets
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 320.dp),
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = contentBottomPadding),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(recordings, key = { it.id }) { item ->
            VideoCard(
              item = item,
              onPlayClick = { onPlayClick(item) },
              onShareClick = { onShareClick(item) },
              onRenameClick = { onRenameClick(item) },
              onDeleteClick = { onDeleteClick(item) },
              onToggleFavorite = { onToggleFavorite(item) },
              onLoadThumbnail = { onLoadThumbnail(it) }
            )
          }
        }
      } else {
        // Single column list on compact devices
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = contentBottomPadding),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(recordings, key = { it.id }) { item ->
            VideoCard(
              item = item,
              onPlayClick = { onPlayClick(item) },
              onShareClick = { onShareClick(item) },
              onRenameClick = { onRenameClick(item) },
              onDeleteClick = { onDeleteClick(item) },
              onToggleFavorite = { onToggleFavorite(item) },
              onLoadThumbnail = { onLoadThumbnail(it) }
            )
          }
        }
      }
    }
  }
}
