package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RecordingItem
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GlassRecordBrush
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary

@Composable
fun RenameDialog(
  item: RecordingItem,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var newTitle by remember { mutableStateOf(item.title) }
  var isError by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    GlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(28.dp),
      backgroundColor = GlassSurfaceElevated,
      elevation = 16.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0x260284C7)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.DriveFileRenameOutline,
              contentDescription = "Rename",
              tint = GlassAccentBlue,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Rename Video",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Enter a title for this screen recording:",
          style = MaterialTheme.typography.bodyMedium,
          color = GlassTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = newTitle,
          onValueChange = {
            newTitle = it
            isError = it.isBlank()
          },
          label = { Text("Video Title") },
          singleLine = true,
          isError = isError,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("rename_input_field")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("rename_cancel_button")
          ) {
            Text("Cancel", color = GlassTextSecondary, fontWeight = FontWeight.SemiBold)
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = {
              if (newTitle.isNotBlank()) {
                onConfirm(newTitle.trim())
              } else {
                isError = true
              }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = GlassAccentBlue,
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("rename_save_button")
          ) {
            Text("Save", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun DeleteConfirmDialog(
  item: RecordingItem,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    GlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(28.dp),
      backgroundColor = GlassSurfaceElevated,
      elevation = 16.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0x26EF4444)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = "Delete",
              tint = Color(0xFFEF4444),
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Delete Recording?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Are you sure you want to permanently delete \"${item.title}\"? This action cannot be undone.",
          style = MaterialTheme.typography.bodyMedium,
          color = GlassTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("cancel_delete_button")
          ) {
            Text("Cancel", color = GlassTextSecondary, fontWeight = FontWeight.SemiBold)
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFDC2626),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("confirm_delete_button")
          ) {
            Text("Delete", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
