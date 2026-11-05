package com.firelite.companion.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FigmaTheme {
  val Background = Color(0xFF050608)
  val SurfaceElevated = Color(0xFF0B1017)
  val SurfaceContent = Color(0xFF0D141D)
  val SurfacePrimary = Color(0xFF0D141D)
  val SurfaceFocused = Color(0xFF141D29)
  val CardBg = Color(0xFF0D141D)
  val CardBorder = Color(0xFF161E2B)
  val CardBorderHighlight = Color(0xFF222D3E)
  val BorderSubtle = Color(0xFF161E2B)

  val AccentBlue = Color(0xFF00A3FF)
  val AccentCyan = Color(0xFF38BDF8)
  val ArtworkOuter = Color(0xFF0D2239)
  val ArtworkInner = Color(0xFF1E75D8)

  val TextPrimary = Color(0xFFF4F5F7)
  val TextSecondary = Color(0xFF858B96)
  val TextMuted = Color(0xFF555B66)

  val GreenDot = Color(0xFF10B981)
  val GreenBg = Color(0xFF0A1F17)
  val GreenBorder = Color(0xFF164E3A)
  val GreenText = Color(0xFF86EFAC)

  val NavBg = Color(0xEE0B1017)
  val NavBorder = Color(0xFF17202E)
  val NavActiveTile = Color(0xFF141C28)
  val NavInactive = Color(0xFF858B96)

  val ButtonPrimaryBg = Color(0xFF0E1A29)
  val ButtonPrimaryBorder = Color(0xFF00A3FF)
  val ButtonSecondaryBg = Color(0xFF0F1520)
  val ButtonSecondaryBorder = Color(0xFF1A2230)
}

fun getDynamicAccentColor(content: com.firelite.core.model.MediaItem?): Color {
  if (content == null) return FigmaTheme.AccentBlue
  val title = content.title.lowercase()
  return when {
    "doraemon" in title -> Color(0xFF00A3FF) // Blue
    "shinchan" in title -> Color(0xFFFF4757) // Warm Red
    "nature" in title || "earth" in title || "planet" in title || "ayyanar" in title -> Color(0xFF2ED573) // Green
    "barbie" in title || "heart" in title || "pink" in title -> Color(0xFFFF69B4) // Pink
    "lego" in title || "motu" in title || "court" in title || "yellow" in title -> Color(0xFFFFA502) // Yellow / Orange
    "harry" in title || "potter" in title || "magic" in title || "wizard" in title -> Color(0xFF8B5CF6) // Deep Blue/Purple
    else -> Color(content.accentColorHex)
  }
}

@Composable
fun CompanionTheme(
  activeAccent: Color = FigmaTheme.AccentBlue,
  content: @Composable () -> Unit
) {
  val darkColorScheme = darkColorScheme(
    primary = activeAccent,
    secondary = activeAccent,
    background = FigmaTheme.Background,
    surface = FigmaTheme.SurfacePrimary,
    surfaceVariant = FigmaTheme.NavActiveTile,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = FigmaTheme.TextPrimary,
    onSurface = FigmaTheme.TextPrimary,
    onSurfaceVariant = FigmaTheme.TextSecondary,
    outline = FigmaTheme.CardBorder,
    outlineVariant = FigmaTheme.NavBorder
  )

  MaterialTheme(
    colorScheme = darkColorScheme,
    content = content
  )
}


