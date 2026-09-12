package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// iOS 27 Luminous Glass Theme Palette
val GlassCanvasStart = Color(0xFFF1F5F9) // Frosted Slate 100
val GlassCanvasEnd = Color(0xFFE2E8F0) // Frosted Slate 200

val GlassOrbCyan = Color(0x3338BDF8) // Soft sky cyan ambient orb
val GlassOrbPurple = Color(0x2E818CF8) // Soft indigo violet ambient orb
val GlassOrbRose = Color(0x28FB7185) // Soft rose ambient orb

// Glass Materials
val GlassSurface = Color(0xB8FFFFFF) // 72% opacity crystalline white
val GlassSurfaceElevated = Color(0xD8FFFFFF) // 85% opacity elevated glass
val GlassSurfaceSubtle = Color(0x66F8FAFC) // 40% translucent surface
val GlassBorder = Color(0x99FFFFFF) // Crisp specular edge highlight
val GlassBorderSubtle = Color(0x33CBD5E1) // Soft secondary outline

// Text & Accents
val GlassTextPrimary = Color(0xFF0F172A) // Deep Slate 900
val GlassTextSecondary = Color(0xFF475569) // Slate 600
val GlassTextTertiary = Color(0xFF94A3B8) // Slate 400

val GlassAccentBlue = Color(0xFF0284C7) // Luminous Cyan-Blue
val GlassAccentBlueLight = Color(0xFFE0F2FE)
val GlassAccentIndigo = Color(0xFF4F46E5) // Electric Indigo
val GlassAccentIndigoLight = Color(0xFFEEF2FF)

// Recording Crimson & Coral (Glass liquid ruby)
val GlassRecordRed = Color(0xFFEF4444)
val GlassRecordRedDark = Color(0xFFDC2626)
val GlassRecordRedGlow = Color(0x40EF4444)
val GlassRecordPillBg = Color(0x33EF4444)

// Success Emerald Glass
val GlassSuccess = Color(0xFF10B981)
val GlassSuccessBg = Color(0x2610B981)

// Gradient Brushes
val GlassBorderBrush = Brush.linearGradient(
  colors = listOf(
    Color(0xB3FFFFFF),
    Color(0x40CBD5E1),
    Color(0x1AFFFFFF)
  )
)

val GlassRecordBrush = Brush.radialGradient(
  colors = listOf(
    Color(0xFFFF5252),
    Color(0xFFE11D48),
    Color(0xFFBE123C)
  )
)

val GlassPrimaryPillBrush = Brush.horizontalGradient(
  colors = listOf(
    Color(0xFF0284C7),
    Color(0xFF3B82F6)
  )
)
