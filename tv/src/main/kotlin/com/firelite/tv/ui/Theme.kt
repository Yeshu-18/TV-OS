package com.firelite.tv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.firelite.core.theme.FireLiteColors

private val DarkColorScheme = darkColorScheme(
  primary = FireLiteColors.DefaultAccent,
  secondary = FireLiteColors.DefaultAccent,
  background = FireLiteColors.Background,
  surface = FireLiteColors.Surface,
  surfaceVariant = FireLiteColors.SurfaceElevated,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onBackground = FireLiteColors.PrimaryText,
  onSurface = FireLiteColors.PrimaryText,
  onSurfaceVariant = FireLiteColors.SecondaryText,
  outline = FireLiteColors.Border,
  outlineVariant = FireLiteColors.BorderStrong
)

@Composable
fun FireLiteTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    content = content
  )
}
