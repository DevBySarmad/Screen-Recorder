package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GlassSuccess
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary

@Composable
fun PermissionItemRow(
  icon: ImageVector,
  title: String,
  description: String,
  isGranted: Boolean,
  onGrantClick: () -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(if (isGranted) Color(0x2610B981) else Color(0x260284C7))
          .border(
            1.dp,
            if (isGranted) Color(0x4D10B981) else Color(0x4D0284C7),
            RoundedCornerShape(12.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isGranted) GlassSuccess else GlassAccentBlue,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = GlassTextPrimary
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = GlassTextSecondary
        )
      }
    }

    if (isGranted) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = "Granted",
          tint = GlassSuccess,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Active",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = GlassSuccess
        )
      }
    } else {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xD8FFFFFF))
          .border(1.dp, GlassBorderBrush, RoundedCornerShape(12.dp))
          .clickable { onGrantClick() }
          .padding(horizontal = 14.dp, vertical = 7.dp)
          .testTag(testTag),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Allow",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = GlassAccentBlue
        )
      }
    }
  }
}

@Composable
fun PermissionsCard(
  hasMicPermission: Boolean,
  hasOverlayPermission: Boolean,
  hasNotificationPermission: Boolean,
  onRequestMicPermission: () -> Unit,
  onRequestOverlayPermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  modifier: Modifier = Modifier
) {
  val allGranted = hasMicPermission && hasOverlayPermission && hasNotificationPermission
  if (allGranted) return

  GlassCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("permissions_card"),
    shape = RoundedCornerShape(26.dp),
    backgroundColor = GlassSurfaceElevated
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Text(
        text = "Permissions",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = GlassTextPrimary
      )
      Text(
        text = "Enable features for seamless background and audio capture",
        style = MaterialTheme.typography.bodySmall,
        color = GlassTextSecondary
      )

      Spacer(modifier = Modifier.height(14.dp))

      PermissionItemRow(
        icon = Icons.Rounded.Mic,
        title = "Microphone Audio",
        description = "To capture your commentary or voice",
        isGranted = hasMicPermission,
        onGrantClick = onRequestMicPermission,
        testTag = "grant_mic_button"
      )

      PermissionItemRow(
        icon = Icons.Rounded.Layers,
        title = "Floating Controls Bubble",
        description = "Pause or stop recording over other apps",
        isGranted = hasOverlayPermission,
        onGrantClick = onRequestOverlayPermission,
        testTag = "grant_overlay_button"
      )

      PermissionItemRow(
        icon = Icons.Rounded.Notifications,
        title = "Notifications",
        description = "Keeps recording active in background",
        isGranted = hasNotificationPermission,
        onGrantClick = onRequestNotificationPermission,
        testTag = "grant_notification_button"
      )
    }
  }
}
