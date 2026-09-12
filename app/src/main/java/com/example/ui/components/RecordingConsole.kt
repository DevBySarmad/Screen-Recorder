package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.RecordingConfig
import com.example.service.RecordingResolution
import com.example.service.RecordingState
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GlassPrimaryPillBrush
import com.example.ui.theme.GlassRecordBrush
import com.example.ui.theme.GlassRecordPillBg
import com.example.ui.theme.GlassRecordRed
import com.example.ui.theme.GlassRecordRedGlow
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary

@Composable
fun RecordingConsole(
  recordingState: RecordingState,
  config: RecordingConfig,
  countdown: Int?,
  onStartClick: () -> Unit,
  onCancelCountdown: () -> Unit,
  onPauseClick: () -> Unit,
  onResumeClick: () -> Unit,
  onStopClick: () -> Unit,
  onResolutionChange: (RecordingResolution) -> Unit = {},
  onFpsChange: (Int) -> Unit = {},
  onAudioChange: (Boolean) -> Unit = {},
  onOverlayChange: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isRecording = recordingState is RecordingState.Recording
  val isPaused = recordingState is RecordingState.Paused
  val isSessionActive = isRecording || isPaused

  val currentDuration = when (recordingState) {
    is RecordingState.Recording -> recordingState.durationSec
    is RecordingState.Paused -> recordingState.durationSec
    else -> 0L
  }

  // Pulsing animation for recording trigger & live indicators
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.14f,
    animationSpec = infiniteRepeatable(
      animation = tween(900),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  // Breathing animation and glowing ripple halo for the idle Tap to start button
  val idleTransition = rememberInfiniteTransition(label = "idle_tap_animation")
  val idleHaloScale by idleTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800),
      repeatMode = RepeatMode.Reverse
    ),
    label = "idle_halo_scale"
  )
  val idleHaloAlpha by idleTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800),
      repeatMode = RepeatMode.Reverse
    ),
    label = "idle_halo_alpha"
  )
  val idleButtonScale by idleTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "idle_button_scale"
  )

  GlassCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("recording_console_card"),
    shape = RoundedCornerShape(32.dp),
    backgroundColor = GlassSurfaceElevated
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (countdown != null) {
        // Countdown Mode (iOS 27 Glass Aperture)
        Column(
          modifier = Modifier.padding(vertical = 20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "CAPTURING SCREEN IN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GlassTextSecondary,
            letterSpacing = 1.5.sp
          )
          Spacer(modifier = Modifier.height(20.dp))
          Box(
            modifier = Modifier
              .size(110.dp)
              .shadow(16.dp, CircleShape, spotColor = GlassAccentBlue.copy(alpha = 0.4f))
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors = listOf(
                    Color(0xFFE0F2FE),
                    Color(0x99BAE6FD),
                    Color(0x407DD3FC)
                  )
                )
              )
              .border(1.5.dp, GlassBorderBrush, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "$countdown",
              style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 58.sp,
                fontWeight = FontWeight.Black
              ),
              color = GlassAccentBlue
            )
          }
          Spacer(modifier = Modifier.height(24.dp))
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .clickable { onCancelCountdown() }
              .border(1.dp, Color(0x33CBD5E1), RoundedCornerShape(16.dp))
              .testTag("cancel_countdown_button"),
            color = Color(0x66FFFFFF),
            shape = RoundedCornerShape(16.dp)
          ) {
            Text(
              text = "Cancel Countdown",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = GlassTextSecondary,
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
          }
        }
      } else if (isSessionActive) {
        // Active Recording Screen
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Glass Status Pill
          Surface(
            color = if (isPaused) Color(0x33F59E0B) else GlassRecordPillBg,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
              .border(
                1.dp,
                if (isPaused) Color(0x66F59E0B) else Color(0x66EF4444),
                RoundedCornerShape(20.dp)
              )
              .padding(1.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .scale(if (isPaused) 1f else pulseScale)
                  .clip(CircleShape)
                  .background(if (isPaused) Color(0xFFD97706) else Color(0xFFEF4444))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isPaused) "RECORDING PAUSED" else "LIVE CAPTURE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = if (isPaused) Color(0xFFB45309) else Color(0xFFDC2626)
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Monospace Glass Digital Timer
          val mins = currentDuration / 60
          val secs = currentDuration % 60
          val formattedTime = String.format("%02d:%02d", mins, secs)

          Text(
            text = formattedTime,
            style = MaterialTheme.typography.displayLarge.copy(
              fontSize = 68.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 2.sp
            ),
            color = GlassTextPrimary,
            modifier = Modifier.testTag("active_timer_display")
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "${config.resolution.label} • ${config.fps} FPS • ${if (config.recordAudio) "Mic ON" else "Mic Muted"}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = GlassTextSecondary
          )

          Spacer(modifier = Modifier.height(30.dp))

          // Dual Glass Action Buttons: Pause/Resume + Stop
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Glass Pause / Resume Pill
            Box(
              modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Color(0x1A0F172A))
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xD8FFFFFF))
                .border(1.2.dp, GlassBorderBrush, RoundedCornerShape(18.dp))
                .clickable {
                  if (isPaused) onResumeClick() else onPauseClick()
                }
                .testTag("pause_resume_button"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                  contentDescription = if (isPaused) "Resume" else "Pause",
                  tint = GlassTextPrimary,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (isPaused) "Resume" else "Pause",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.bodyLarge,
                  color = GlassTextPrimary
                )
              }
            }

            // Glass Crimson Stop Button
            Box(
              modifier = Modifier
                .weight(1.2f)
                .height(54.dp)
                .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = GlassRecordRedGlow)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassRecordBrush)
                .border(1.2.dp, Color(0x66FFFFFF), RoundedCornerShape(18.dp))
                .clickable { onStopClick() }
                .testTag("stop_recording_button"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.Stop,
                  contentDescription = "Stop recording",
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Stop & Save",
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.bodyLarge,
                  color = Color.White
                )
              }
            }
          }
        }
      } else {
        // Clean Centered Idle Console (iPhone & Google Design: Centered 'Tap to start' button)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          // Centered Clean Recording Button with subtle breathing aura and single clean border
          Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            // Soft borderless radiant pulse halo
            Box(
              modifier = Modifier
                .size(152.dp)
                .scale(idleHaloScale)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = idleHaloAlpha * 0.5f))
            )

            // Centered Primary Recording Button (Single crisp border, no inner ring)
            Box(
              modifier = Modifier
                .size(144.dp)
                .scale(idleButtonScale)
                .shadow(20.dp, CircleShape, ambientColor = Color(0x33EF4444), spotColor = Color(0x66EF4444))
                .clip(CircleShape)
                .background(GlassRecordBrush)
                .border(2.dp, Color(0xCCFFFFFF), CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = ripple(bounded = true, color = Color.White)
                ) { onStartClick() }
                .testTag("start_recording_button"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Tap to start",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Subtle Status & Quality Indicator
          Surface(
            color = Color(0x1A0284C7),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .border(1.dp, Color(0x330284C7), RoundedCornerShape(16.dp))
          ) {
            Text(
              text = "${config.resolution.label} • ${config.fps} FPS • ${config.audioSource.label}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = GlassAccentBlue,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }
        }
      }
    }
  }
}
