package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.RecordingItem
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoCard(
  item: RecordingItem,
  onPlayClick: () -> Unit,
  onShareClick: () -> Unit,
  onRenameClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onLoadThumbnail: suspend (RecordingItem) -> Bitmap?,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }

  val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.id, key2 = item.filePath) {
    value = withContext(Dispatchers.IO) {
      onLoadThumbnail(item)
    }
  }

  GlassCard(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onPlayClick() }
      .testTag("video_card_${item.id}"),
    shape = RoundedCornerShape(26.dp),
    backgroundColor = GlassSurfaceElevated,
    elevation = 5.dp
  ) {
    Column {
      // 16:9 Thumbnail Surface
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .background(Color(0xFF0F172A))
      ) {
        if (thumbnailBitmap != null) {
          Image(
            bitmap = thumbnailBitmap!!.asImageBitmap(),
            contentDescription = "Thumbnail for ${item.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        } else {
          // Glassy Dark Aperture Placeholder
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Videocam,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.25f),
              modifier = Modifier.size(46.dp)
            )
          }
        }

        // Vignette Gradient
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .align(Alignment.BottomCenter)
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xCC000000))
              )
            )
        )

        // Glass Duration Pill (Bottom Right)
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(10.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xB3000000))
            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = item.formattedDuration,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
          )
        }

        // Glass Resolution Badge (Top Left)
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xCC0284C7))
            .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = item.resolution,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black
          )
        }

        // Glass Center Play Lens
        Box(
          modifier = Modifier
            .size(52.dp)
            .align(Alignment.Center)
            .shadow(8.dp, CircleShape, spotColor = Color(0x66000000))
            .clip(CircleShape)
            .background(Color(0x990F172A))
            .border(1.5.dp, Color(0x99FFFFFF), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Play video",
            tint = Color.White,
            modifier = Modifier.size(30.dp)
          )
        }
      }

      // Metadata Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(3.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = item.formattedDate,
              style = MaterialTheme.typography.bodySmall,
              color = GlassTextSecondary
            )
            Text(
              text = "  •  ",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8)
            )
            Text(
              text = item.formattedFileSize,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = GlassAccentBlue
            )
          }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(38.dp)
          ) {
            Icon(
              imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = if (item.isFavorite) "Unfavorite" else "Favorite",
              tint = if (item.isFavorite) Color(0xFFEF4444) else GlassTextSecondary
            )
          }

          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.size(38.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options",
                tint = GlassTextSecondary
              )
            }

            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false },
              modifier = Modifier
                .background(Color(0xF0FFFFFF))
                .border(1.dp, GlassBorderBrush, RoundedCornerShape(16.dp)),
              shape = RoundedCornerShape(16.dp)
            ) {
              DropdownMenuItem(
                text = { Text("Play Video", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                  Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = GlassAccentBlue)
                },
                onClick = {
                  menuExpanded = false
                  onPlayClick()
                }
              )
              DropdownMenuItem(
                text = { Text("Share", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                  Icon(Icons.Rounded.Share, contentDescription = null, tint = GlassTextSecondary)
                },
                onClick = {
                  menuExpanded = false
                  onShareClick()
                }
              )
              DropdownMenuItem(
                text = { Text("Rename", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                  Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null, tint = GlassTextSecondary)
                },
                onClick = {
                  menuExpanded = false
                  onRenameClick()
                }
              )
              DropdownMenuItem(
                text = { Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                  Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626)
                  )
                },
                onClick = {
                  menuExpanded = false
                  onDeleteClick()
                }
              )
            }
          }
        }
      }
    }
  }
}
