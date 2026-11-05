package com.firelite.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object FireLiteColors {
  // Base Colors
  val Background = Color(0xFF08090B)
  val BackgroundElevated = Color(0xFF101216)
  val Surface = Color(0xFF15171C)
  val SurfaceElevated = Color(0xFF1B1E24)
  val SurfaceStrong = Color(0xFF22262D)
  val Border = Color(0xFF2A2E36)
  val BorderStrong = Color(0xFF3A3F48)

  // Typography
  val PrimaryText = Color(0xFFF5F5F2)
  val SecondaryText = Color(0xFFA7A9AE)
  val TertiaryText = Color(0xFF6F737B)
  val Disabled = Color(0xFF50545B)

  // Accent System (Default FireLite Accent)
  val DefaultAccent = Color(0xFF7C8CFF)
  val FocusGlow = Color(0x667C8CFF)
}

object FireLiteSpacing {
  val xs = 4.dp
  val sm = 8.dp
  val md = 12.dp
  val card = 16.dp
  val lg = 24.dp
  val section = 32.dp
  val xl = 40.dp
  val screen = 48.dp
  val xxl = 64.dp
}

object FireLiteRadius {
  val sm = 8.dp
  val md = 12.dp
  val lg = 16.dp
  val floating = 20.dp
  val dock = 24.dp
}

object FireLiteElevation {
  val level0 = 0.dp
  val level1 = 2.dp
  val level2 = 4.dp
  val level3 = 8.dp
}

object FireLiteMotion {
  const val Micro = 120
  const val Focus = 150
  const val Screen = 200
  const val DynamicAccent = 400
}

object FireLiteTypography {
  val Display = 32.sp
  val ScreenTitle = 28.sp
  val SectionTitle = 22.sp
  val CardTitle = 18.sp
  val Body = 16.sp
  val Secondary = 14.sp
  val Metadata = 12.sp
  val DockLabel = 12.sp
}
