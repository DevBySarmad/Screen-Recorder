package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
  primary = GlassAccentBlue,
  onPrimary = Color.White,
  primaryContainer = GlassAccentBlueLight,
  onPrimaryContainer = Color(0xFF0369A1),
  secondary = GlassAccentIndigo,
  onSecondary = Color.White,
  secondaryContainer = GlassAccentIndigoLight,
  onSecondaryContainer = Color(0xFF3730A3),
  tertiary = GlassSuccess,
  onTertiary = Color.White,
  tertiaryContainer = GlassSuccessBg,
  onTertiaryContainer = Color(0xFF065F46),
  background = GlassCanvasStart,
  onBackground = GlassTextPrimary,
  surface = GlassSurface,
  onSurface = GlassTextPrimary,
  surfaceVariant = Color(0xD8F1F5F9),
  onSurfaceVariant = GlassTextSecondary,
  outline = GlassBorderSubtle,
  outlineVariant = Color(0x66CBD5E1),
  error = GlassRecordRed,
  onError = Color.White,
  errorContainer = GlassRecordPillBg,
  onErrorContainer = GlassRecordRedDark
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF38BDF8),
  onPrimary = Color(0xFF0C4A6E),
  primaryContainer = Color(0xFF0369A1),
  onPrimaryContainer = Color(0xFFE0F2FE),
  secondary = Color(0xFF818CF8),
  onSecondary = Color(0xFF1E1B4B),
  background = Color(0xFF0B0F19),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xCC1E293B),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0x99334155),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0x3394A3B8),
  error = GlassRecordRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      window?.let {
        WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
