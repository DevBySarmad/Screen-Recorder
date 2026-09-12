package com.example.ui.components

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AudioSourceOption
import com.example.service.RecordingConfig
import com.example.service.RecordingResolution
import kotlin.math.roundToInt

// iOS & Google Hybrid Design Tokens
private val IosBackground = Color(0xFFF2F2F7)
private val IosCardBackground = Color(0xFFFFFFFF)
private val IosLabelPrimary = Color(0xFF1C1C1E)
private val IosLabelSecondary = Color(0xFF8E8E93)
private val IosSectionHeader = Color(0xFF6E6E73)
private val IosSeparator = Color(0x1F3C3C43)
private val IosSegmentedBg = Color(0xFFE5E5EA)

// Iconic System Accent Colors
private val IosBlue = Color(0xFF007AFF)
private val IosPurple = Color(0xFF5856D6)
private val IosOrange = Color(0xFFFF9500)
private val IosGreen = Color(0xFF34C759)
private val IosTeal = Color(0xFF30B0C7)
private val IosPink = Color(0xFFFF2D55)
private val IosAmber = Color(0xFFFF9F0A)
private val IosIndigo = Color(0xFF5E5CE6)

@Composable
fun SettingsScreen(
  config: RecordingConfig,
  hasMicPermission: Boolean,
  hasOverlayPermission: Boolean,
  hasNotificationPermission: Boolean,
  onClose: () -> Unit,
  onResolutionChange: (RecordingResolution) -> Unit,
  onFpsChange: (Int) -> Unit,
  onAudioChange: (Boolean) -> Unit = {},
  onAudioSourceChange: (AudioSourceOption) -> Unit = {},
  onOverlayChange: (Boolean) -> Unit,
  onRequestMicPermission: () -> Unit,
  onRequestOverlayPermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onSyncLibrary: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  var audioDropdownExpanded by remember { mutableStateOf(false) }
  val context = LocalContext.current

  // Detect device screen refresh rate capability safely
  val supportedFpsList = remember(config.fps) {
    val maxRefresh = try {
      val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
          (context as? android.app.Activity)?.display ?: context.display
        } catch (t: Throwable) {
          null
        }
      } else {
        try {
          @Suppress("DEPRECATION")
          (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        } catch (t: Throwable) {
          null
        }
      }
      val refresh = display?.refreshRate ?: 60f
      if (refresh > 0f) refresh.roundToInt() else 60
    } catch (t: Throwable) {
      60
    }

    val baseList = when {
      maxRefresh >= 140 -> listOf(144, 120, 60, 30)
      maxRefresh >= 115 -> listOf(120, 90, 60, 30)
      maxRefresh >= 85 -> listOf(90, 60, 30)
      else -> listOf(60, 30)
    }
    if (config.fps !in baseList) (baseList + config.fps).distinct().sortedDescending()
    else baseList
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(IosBackground)
      .statusBarsPadding()
  ) {
    // Navigation Top Bar (iOS & Google M3 Hybrid)
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, ambientColor = Color(0x10000000), spotColor = Color(0x10000000)),
      color = IosCardBackground
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // iOS Style Back Button
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFF2F2F7))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true, color = IosBlue)
            ) { onClose() }
            .testTag("settings_back_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = IosBlue,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
          text = "Settings",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = IosLabelPrimary
        )
      }
    }

    // Scrollable Settings Grouped Table
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(modifier = Modifier.widthIn(max = 600.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {

          // ==========================================
          // SECTION 1: VIDEO OUTPUT
          // ==========================================
          Column(modifier = Modifier.fillMaxWidth().testTag("settings_section_video")) {
            IosSectionHeader(title = "VIDEO OUTPUT")

            IosGroupCard {
              // Row: Video Resolution
              IosSettingRow(
                icon = Icons.Rounded.Videocam,
                iconBg = IosBlue,
                title = "Resolution",
                subtitle = "${config.resolution.label} • ${config.resolution.width}×${config.resolution.height}"
              )

              // Resolution Segmented Control (iOS Style with 4K, 2K, 1080p, 720p, 480p)
              Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                IosSegmentedControl(
                  options = RecordingResolution.entries,
                  selectedOption = config.resolution,
                  onOptionSelected = onResolutionChange,
                  labelMapper = { res ->
                    when (res) {
                      RecordingResolution.RES_4K -> "4K"
                      RecordingResolution.RES_2K -> "2K"
                      RecordingResolution.RES_1080P -> "1080p"
                      RecordingResolution.RES_720P -> "720p"
                      RecordingResolution.RES_480P -> "480p"
                    }
                  }
                )
              }

              IosInsetDivider()

              // Row: Frame Rate
              IosSettingRow(
                icon = Icons.Rounded.Speed,
                iconBg = IosPurple,
                title = "Frame Rate",
                subtitle = "${config.fps} FPS (Device capability: up to ${supportedFpsList.firstOrNull() ?: 60} FPS)"
              )

              // FPS Segmented Control (iOS Style based on device capability)
              Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                IosSegmentedControl(
                  options = supportedFpsList,
                  selectedOption = config.fps,
                  onOptionSelected = onFpsChange,
                  labelMapper = { "$it FPS" }
                )
              }
            }
          }

          // ==========================================
          // SECTION 2: AUDIO & RECORDING FEATURES
          // ==========================================
          Column(modifier = Modifier.fillMaxWidth().testTag("settings_section_features")) {
            IosSectionHeader(title = "RECORDING FEATURES")

            IosGroupCard {
              // Sound Source Drop Down Menu Row
              val (audioIcon, audioAccent) = when (config.audioSource) {
                AudioSourceOption.MUTE -> Icons.Rounded.MicOff to Color(0xFF64748B)
                AudioSourceOption.SYSTEM -> Icons.Rounded.Notifications to IosAmber
                AudioSourceOption.MIC -> Icons.Rounded.Mic to IosBlue
                AudioSourceOption.SYSTEM_AND_MIC -> Icons.Rounded.Layers to IosPurple
              }

              val rotationAngle by animateFloatAsState(
                targetValue = if (audioDropdownExpanded) 180f else 0f,
                label = "dropdown_arrow"
              )

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("settings_audio_source_dropdown_container")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                      interactionSource = remember { MutableInteractionSource() },
                      indication = ripple(bounded = true)
                    ) { audioDropdownExpanded = !audioDropdownExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("settings_audio_source_dropdown"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    IosIconBadge(icon = audioIcon, backgroundColor = audioAccent)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                      Text(
                        text = "Sound Source",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IosLabelPrimary
                      )
                      Text(
                        text = config.audioSource.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = IosLabelSecondary
                      )
                    }
                  }

                  // Active Selection Pill with Dropdown Chevron
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF2F2F7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
                    modifier = Modifier.padding(start = 8.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text(
                        text = config.audioSource.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = audioAccent
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Select sound source",
                        tint = audioAccent,
                        modifier = Modifier
                          .size(20.dp)
                          .rotate(rotationAngle)
                      )
                    }
                  }
                }

                // Drop Down Menu anchored to the selector
                DropdownMenu(
                  expanded = audioDropdownExpanded,
                  onDismissRequest = { audioDropdownExpanded = false },
                  modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x1A000000), RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp),
                  shape = RoundedCornerShape(16.dp),
                  containerColor = Color.White,
                  shadowElevation = 8.dp
                ) {
                  listOf(
                    Triple(AudioSourceOption.MUTE, Icons.Rounded.MicOff, Color(0xFF64748B)),
                    Triple(AudioSourceOption.SYSTEM, Icons.Rounded.Notifications, IosAmber),
                    Triple(AudioSourceOption.MIC, Icons.Rounded.Mic, IosBlue),
                    Triple(AudioSourceOption.SYSTEM_AND_MIC, Icons.Rounded.Layers, IosPurple)
                  ).forEach { (option, icon, accentColor) ->
                    val isSelected = config.audioSource == option

                    DropdownMenuItem(
                      text = {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                          Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) IosLabelPrimary else IosLabelSecondary
                          )
                          Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = IosLabelSecondary
                          )
                        }
                      },
                      leadingIcon = {
                        Surface(
                          color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0x14000000),
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) accentColor else IosLabelSecondary,
                            modifier = Modifier.padding(7.dp)
                          )
                        }
                      },
                      trailingIcon = {
                        if (isSelected) {
                          Surface(
                            color = accentColor,
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Rounded.Check,
                              contentDescription = "Selected",
                              tint = Color.White,
                              modifier = Modifier.padding(3.dp)
                            )
                          }
                        }
                      },
                      onClick = {
                        onAudioSourceChange(option)
                        audioDropdownExpanded = false
                      },
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                          if (isSelected) Modifier.background(accentColor.copy(alpha = 0.08f))
                          else Modifier
                        )
                        .testTag("settings_audio_${option.name.lowercase()}")
                    )
                  }
                }
              }

              IosInsetDivider()

              // Floating Controls Overlay Switch Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                  ) { onOverlayChange(!config.showFloatingOverlay) }
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  IosIconBadge(icon = Icons.Rounded.Layers, backgroundColor = IosTeal)
                  Spacer(modifier = Modifier.width(14.dp))
                  Column {
                    Text(
                      text = "Floating Controls Bubble",
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = IosLabelPrimary
                    )
                    Text(
                      text = if (config.showFloatingOverlay) "Overlay visible over other apps" else "Off (use notification bar)",
                      style = MaterialTheme.typography.bodySmall,
                      color = IosLabelSecondary
                    )
                  }
                }

                Switch(
                  checked = config.showFloatingOverlay,
                  onCheckedChange = onOverlayChange,
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = IosGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE5E5EA),
                    uncheckedBorderColor = Color.Transparent
                  ),
                  modifier = Modifier.testTag("settings_overlay_switch")
                )
              }
            }
          }

          // ==========================================
          // SECTION 3: SYSTEM PERMISSIONS
          // ==========================================
          Column(modifier = Modifier.fillMaxWidth().testTag("settings_section_permissions")) {
            IosSectionHeader(title = "PERMISSIONS & ACCESS")

            IosGroupCard {
              // Permission 1: Microphone
              IosPermissionRow(
                icon = Icons.Rounded.Mic,
                iconBg = IosPink,
                title = "Microphone",
                subtitle = "Voice commentary and external audio",
                isGranted = hasMicPermission,
                onGrantClick = onRequestMicPermission,
                testTag = "settings_grant_mic"
              )

              IosInsetDivider()

              // Permission 2: Floating Overlay
              IosPermissionRow(
                icon = Icons.Rounded.Layers,
                iconBg = IosIndigo,
                title = "Floating Bubble",
                subtitle = "Display controls over active apps",
                isGranted = hasOverlayPermission,
                onGrantClick = onRequestOverlayPermission,
                testTag = "settings_grant_overlay"
              )

              IosInsetDivider()

              // Permission 3: Notifications
              IosPermissionRow(
                icon = Icons.Rounded.Notifications,
                iconBg = IosAmber,
                title = "Notifications",
                subtitle = "Keeps capture active in the background",
                isGranted = hasNotificationPermission,
                onGrantClick = onRequestNotificationPermission,
                testTag = "settings_grant_notifications"
              )
            }
          }

          // ==========================================
          // SECTION 4: STORAGE & MEDIA LIBRARY
          // ==========================================
          Column(modifier = Modifier.fillMaxWidth().testTag("settings_section_storage")) {
            IosSectionHeader(title = "STORAGE & LIBRARY")

            IosGroupCard {
              // Storage Folder Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  IosIconBadge(icon = Icons.Rounded.Folder, backgroundColor = IosBlue)
                  Spacer(modifier = Modifier.width(14.dp))
                  Column {
                    Text(
                      text = "Storage Location",
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = IosLabelPrimary
                    )
                    Text(
                      text = "Movies/ScreenRecordings",
                      style = MaterialTheme.typography.bodySmall,
                      color = IosLabelSecondary
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFF2F2F7)
                ) {
                  Text(
                    text = "Public MediaStore",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = IosLabelSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }

              IosInsetDivider()

              // Sync & Refresh Button Row (iOS Style Action Item)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = IosBlue)
                  ) { onSyncLibrary() }
                  .padding(horizontal = 16.dp, vertical = 14.dp)
                  .testTag("sync_refresh_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IosIconBadge(icon = Icons.Rounded.Refresh, backgroundColor = IosGreen)
                  Spacer(modifier = Modifier.width(14.dp))
                  Text(
                    text = "Sync & Refresh Library",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = IosBlue
                  )
                }

                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = null,
                  tint = IosLabelSecondary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          // System Info Footer (Cupertino / Google clean branding)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Screen Recorder",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = IosLabelSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Crafted with Material 3 & iOS Design Principles",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFA1A1AA)
            )
          }

          Spacer(modifier = Modifier.height(32.dp))
        }
      }
    }
  }
}

// ==========================================
// iOS & GOOGLE M3 HYBRID HELPER COMPONENTS
// ==========================================

@Composable
private fun IosSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.SemiBold,
    color = IosSectionHeader,
    letterSpacing = 0.8.sp,
    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 4.dp)
  )
}

@Composable
private fun IosSectionFooter(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = IosLabelSecondary,
    lineHeight = 16.sp,
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
  )
}

@Composable
private fun IosGroupCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(1.dp, RoundedCornerShape(16.dp), spotColor = Color(0x14000000)),
    shape = RoundedCornerShape(16.dp),
    color = IosCardBackground
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      content()
    }
  }
}

@Composable
private fun IosIconBadge(
  icon: ImageVector,
  backgroundColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.size(32.dp),
    shape = RoundedCornerShape(8.dp),
    color = backgroundColor
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.padding(6.dp)
    )
  }
}

@Composable
private fun IosInsetDivider(modifier: Modifier = Modifier) {
  HorizontalDivider(
    modifier = modifier.fillMaxWidth(),
    color = IosSeparator,
    thickness = 0.6.dp
  )
}

@Composable
private fun IosSettingRow(
  icon: ImageVector,
  iconBg: Color,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IosIconBadge(icon = icon, backgroundColor = iconBg)
    Spacer(modifier = Modifier.width(14.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = IosLabelPrimary
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = IosLabelSecondary
      )
    }
  }
}

@Composable
private fun <T> IosSegmentedControl(
  options: List<T>,
  selectedOption: T,
  onOptionSelected: (T) -> Unit,
  labelMapper: (T) -> String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(IosSegmentedBg)
      .padding(2.5.dp),
    horizontalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    options.forEach { option ->
      val isSelected = option == selectedOption
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .then(
            if (isSelected) {
              Modifier
                .shadow(2.dp, RoundedCornerShape(8.dp), spotColor = Color(0x29000000))
                .background(Color.White)
            } else {
              Modifier.background(Color.Transparent)
            }
          )
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true)
          ) { onOptionSelected(option) }
          .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = labelMapper(option),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) IosLabelPrimary else IosLabelSecondary
        )
      }
    }
  }
}

@Composable
private fun IosPermissionRow(
  icon: ImageVector,
  iconBg: Color,
  title: String,
  subtitle: String,
  isGranted: Boolean,
  onGrantClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      IosIconBadge(icon = icon, backgroundColor = iconBg)
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = IosLabelPrimary
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = IosLabelSecondary
        )
      }
    }

    if (isGranted) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x1F34C759),
        modifier = Modifier.padding(start = 8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Granted",
            tint = IosGreen,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Active",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = IosGreen
          )
        }
      }
    } else {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = IosBlue,
        modifier = Modifier
          .padding(start = 8.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true)
          ) { onGrantClick() }
          .testTag(testTag)
      ) {
        Text(
          text = "Allow",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
      }
    }
  }
}
