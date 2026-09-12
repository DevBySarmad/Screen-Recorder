package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.RecordingItem
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(
  item: RecordingItem,
  onDismiss: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  val context = LocalContext.current

  val exoPlayer = remember(item.contentUriString, item.filePath) {
    ExoPlayer.Builder(context).build().apply {
      val uri = if (item.contentUriString.startsWith("content://")) {
        Uri.parse(item.contentUriString)
      } else {
        Uri.fromFile(File(item.filePath))
      }
      setMediaItem(MediaItem.fromUri(uri))
      prepare()
      playWhenReady = true
    }
  }

  DisposableEffect(exoPlayer) {
    onDispose {
      exoPlayer.stop()
      exoPlayer.release()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true
    )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black),
      color = Color.Black
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        // Player Surface
        AndroidView(
          factory = { ctx ->
            PlayerView(ctx).apply {
              player = exoPlayer
              useController = true
              setShowNextButton(false)
              setShowPreviousButton(false)
              setShowFastForwardButton(true)
              setShowRewindButton(true)
              controllerShowTimeoutMs = 3500
            }
          },
          modifier = Modifier
            .fillMaxSize()
            .testTag("exo_player_view")
        )

        // Glass Floating Header Bar (iOS 27 Dynamic Header)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .align(Alignment.TopCenter)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x99000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(Color(0x33FFFFFF))
                  .border(1.dp, Color(0x40FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                IconButton(
                  onClick = onDismiss,
                  modifier = Modifier
                    .size(38.dp)
                    .testTag("player_close_button")
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close player",
                    tint = Color.White
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column {
                Text(
                  text = item.title,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${item.formattedDuration} • ${item.formattedFileSize}",
                  color = Color.White.copy(alpha = 0.7f),
                  style = MaterialTheme.typography.bodySmall
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onShare,
                modifier = Modifier
                  .padding(end = 4.dp)
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(Color(0x22FFFFFF))
                  .testTag("player_share_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Share,
                  contentDescription = "Share",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }

              IconButton(
                onClick = onDelete,
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(Color(0x33EF4444))
                  .testTag("player_delete_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Delete,
                  contentDescription = "Delete",
                  tint = Color(0xFFFF6B6B),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
