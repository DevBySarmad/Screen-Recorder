package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GlassCanvasEnd
import com.example.ui.theme.GlassCanvasStart
import com.example.ui.theme.GlassOrbCyan
import com.example.ui.theme.GlassOrbPurple
import com.example.ui.theme.GlassOrbRose
import com.example.ui.theme.GlassSurface

/**
 * Renders an ambient luminous backdrop with soft iridescent color blooms,
 * simulating iOS 27 frosted glass refraction.
 */
@Composable
fun GlassBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(GlassCanvasStart, GlassCanvasEnd)
        )
      )
  ) {
    // Ambient Iridescent Light Blooms
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Top-left Sky Cyan Orb
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(GlassOrbCyan, Color.Transparent),
          center = Offset(w * 0.15f, h * 0.12f),
          radius = w * 0.6f
        ),
        radius = w * 0.6f,
        center = Offset(w * 0.15f, h * 0.12f)
      )

      // Center-right Electric Indigo Orb
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(GlassOrbPurple, Color.Transparent),
          center = Offset(w * 0.85f, h * 0.45f),
          radius = w * 0.65f
        ),
        radius = w * 0.65f,
        center = Offset(w * 0.85f, h * 0.45f)
      )

      // Bottom-center Rose Glow Orb
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(GlassOrbRose, Color.Transparent),
          center = Offset(w * 0.4f, h * 0.88f),
          radius = w * 0.55f
        ),
        radius = w * 0.55f,
        center = Offset(w * 0.4f, h * 0.88f)
      )
    }

    content()
  }
}

/**
 * Standard Glassmorphic Surface with specular highlight border and soft drop shadow.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(26.dp),
  backgroundColor: Color = GlassSurface,
  borderBrush: Brush = GlassBorderBrush,
  borderWidth: Dp = 1.dp,
  elevation: Dp = 6.dp,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x1A0F172A),
        spotColor = Color(0x1F0F172A)
      )
      .clip(shape)
      .background(backgroundColor)
      .border(borderWidth, borderBrush, shape)
  ) {
    content()
  }
}
