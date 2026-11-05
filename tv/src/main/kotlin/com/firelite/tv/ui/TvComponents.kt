package com.firelite.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firelite.core.model.AppDockItem
import com.firelite.core.model.MediaItem
import com.firelite.core.model.WeatherAlertInfo
import com.firelite.core.model.WhatsNewItem
import com.firelite.core.theme.FireLiteColors
import com.firelite.core.theme.FireLiteElevation
import com.firelite.core.theme.FireLiteMotion
import com.firelite.core.theme.FireLiteRadius
import com.firelite.core.theme.FireLiteSpacing
import com.firelite.core.theme.FireLiteTypography
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvTopBar(
  serverIp: String,
  connectedClients: Int,
  modifier: Modifier = Modifier
) {
  var currentTime by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    while (true) {
      currentTime = formatter.format(Date())
      delay(1000)
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = FireLiteSpacing.screen,
        end = FireLiteSpacing.screen,
        top = FireLiteSpacing.card,
        bottom = FireLiteSpacing.sm
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Brand Identity
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "FIRELITE",
        fontSize = FireLiteTypography.CardTitle,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = FireLiteColors.PrimaryText
      )
      Spacer(modifier = Modifier.width(FireLiteSpacing.md))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(FireLiteRadius.sm))
          .background(FireLiteColors.SurfaceStrong)
          .padding(horizontal = FireLiteSpacing.sm, vertical = FireLiteSpacing.xs)
      ) {
        Text(
          text = "FIRE TV",
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          color = FireLiteColors.SecondaryText
        )
      }
    }

    // Status Area: Remote Connection & Time
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(FireLiteRadius.floating))
          .background(if (connectedClients > 0) Color(0xFF0F291E) else FireLiteColors.Surface)
          .border(
            1.dp,
            if (connectedClients > 0) Color(0xFF10B981) else FireLiteColors.Border,
            RoundedCornerShape(FireLiteRadius.floating)
          )
          .padding(horizontal = FireLiteSpacing.md, vertical = 6.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (connectedClients > 0) Icons.Default.CastConnected else Icons.Default.Wifi,
            contentDescription = "Companion Status",
            tint = if (connectedClients > 0) Color(0xFF34D399) else FireLiteColors.SecondaryText,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (connectedClients > 0) "Remote Connected" else "Remote IP: $serverIp",
            fontSize = FireLiteTypography.Metadata,
            fontWeight = FontWeight.Medium,
            color = if (connectedClients > 0) Color(0xFFA7F3D0) else FireLiteColors.SecondaryText
          )
        }
      }

      Spacer(modifier = Modifier.width(FireLiteSpacing.card))

      Text(
        text = currentTime,
        fontSize = FireLiteTypography.Body,
        fontWeight = FontWeight.SemiBold,
        color = FireLiteColors.PrimaryText
      )
    }
  }
}

/**
 * Pinned Content Card (Section 10 of Design System)
 * Structure: Backdrop + Content Artwork + Character Cutout layer + Subtle Shadow
 * Normal: scale 1.0
 * Focused: scale 1.05, translateY -4dp, subtle accent border & glow, character layer moves 3dp up
 * Pressed: scale 0.98
 */
@Composable
fun PinnedContentCard(
  item: MediaItem,
  onSelect: (MediaItem) -> Unit,
  onFocus: (MediaItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val isPressed by interactionSource.collectIsPressedAsState()

  LaunchedEffect(isFocused) {
    if (isFocused) {
      onFocus(item)
    }
  }

  // Animation values adhering to FireLiteMotion tokens
  val scale by animateFloatAsState(
    targetValue = when {
      isPressed -> 0.98f
      isFocused -> 1.05f
      else -> 1.0f
    },
    animationSpec = tween(durationMillis = FireLiteMotion.Focus),
    label = "cardScale"
  )

  val translateY by animateDpAsState(
    targetValue = if (isFocused) (-4).dp else 0.dp,
    animationSpec = tween(durationMillis = FireLiteMotion.Focus),
    label = "cardTranslateY"
  )

  val characterTranslateY by animateDpAsState(
    targetValue = if (isFocused) (-3).dp else 0.dp,
    animationSpec = tween(durationMillis = FireLiteMotion.Focus),
    label = "characterTranslateY"
  )

  val borderColor by animateColorAsState(
    targetValue = if (isFocused) FireLiteColors.DefaultAccent else FireLiteColors.Border,
    animationSpec = tween(durationMillis = FireLiteMotion.Focus),
    label = "cardBorderColor"
  )

  val elevation by animateDpAsState(
    targetValue = if (isFocused) FireLiteElevation.level3 else FireLiteElevation.level1,
    label = "cardElevation"
  )

  Box(
    modifier = modifier
      .width(220.dp)
      .height(124.dp)
      .offset(y = translateY)
      .scale(scale)
      .clip(RoundedCornerShape(FireLiteRadius.md))
      .background(FireLiteColors.Surface)
      .border(1.5.dp, borderColor, RoundedCornerShape(FireLiteRadius.md))
      .shadow(elevation, RoundedCornerShape(FireLiteRadius.md))
      .focusable(interactionSource = interactionSource)
      .clickable(interactionSource = interactionSource, indication = null) {
        onSelect(item)
      }
  ) {
    // 1. BACKDROP & ARTWORK GRADIENT
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(item.accentColorHex).copy(alpha = if (isFocused) 0.35f else 0.18f),
              FireLiteColors.SurfaceElevated,
              FireLiteColors.Background
            )
          )
        )
    )

    // 2. CHARACTER / VISUAL SILHOUETTE LAYER (2.5D visual lift)
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(end = FireLiteSpacing.sm, top = FireLiteSpacing.xs)
        .offset(y = characterTranslateY)
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Color(item.accentColorHex).copy(alpha = if (isFocused) 0.3f else 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = item.title.firstOrNull()?.toString() ?: "•",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = Color(item.accentColorHex)
        )
      }
    }

    // 3. CONTENT OVERLAY
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(FireLiteSpacing.md),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top row: Platform Tag & Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(FireLiteRadius.sm))
            .background(FireLiteColors.SurfaceStrong)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = item.platform,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = FireLiteColors.SecondaryText
          )
        }

        val badge = item.badge
        if (badge != null) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(FireLiteRadius.sm))
              .background(Color(item.accentColorHex).copy(alpha = 0.85f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = badge,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }

      // Bottom: Title & Subtitle
      Column {
        Text(
          text = item.title,
          fontSize = FireLiteTypography.CardTitle,
          fontWeight = FontWeight.Medium,
          color = FireLiteColors.PrimaryText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = item.subtitle,
          fontSize = FireLiteTypography.Metadata,
          fontWeight = FontWeight.Normal,
          color = FireLiteColors.SecondaryText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // Subtle Play indicator when focused
    if (isFocused) {
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(36.dp)
          .clip(CircleShape)
          .background(FireLiteColors.DefaultAccent.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Play",
          tint = Color.Black,
          modifier = Modifier.size(22.dp)
        )
      }
    }
  }
}

@Composable
fun PinnedContentRow(
  title: String,
  items: List<MediaItem>,
  onSelectItem: (MediaItem) -> Unit,
  onFocusItem: (MediaItem) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = FireLiteSpacing.screen, vertical = FireLiteSpacing.xs),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        fontSize = FireLiteTypography.SectionTitle,
        fontWeight = FontWeight.SemiBold,
        color = FireLiteColors.PrimaryText
      )
      Spacer(modifier = Modifier.width(FireLiteSpacing.sm))
      Text(
        text = "${items.size}",
        fontSize = FireLiteTypography.Metadata,
        color = FireLiteColors.TertiaryText
      )
    }

    LazyRow(
      contentPadding = PaddingValues(horizontal = FireLiteSpacing.screen, vertical = FireLiteSpacing.sm),
      horizontalArrangement = Arrangement.spacedBy(FireLiteSpacing.card)
    ) {
      items(items, key = { it.id }) { item ->
        PinnedContentCard(
          item = item,
          onSelect = onSelectItem,
          onFocus = onFocusItem
        )
      }
    }
  }
}

/**
 * What's New Panel (Section 15 of Design System)
 */
@Composable
fun WhatsNewPanel(
  items: List<WhatsNewItem>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(FireLiteRadius.lg))
      .background(FireLiteColors.Surface.copy(alpha = 0.95f))
      .border(1.dp, FireLiteColors.Border, RoundedCornerShape(FireLiteRadius.lg))
      .padding(FireLiteSpacing.card)
  ) {
    Text(
      text = "What's New",
      fontSize = 16.sp,
      fontWeight = FontWeight.SemiBold,
      color = FireLiteColors.PrimaryText
    )

    Spacer(modifier = Modifier.height(FireLiteSpacing.md))

    Column(verticalArrangement = Arrangement.spacedBy(FireLiteSpacing.md)) {
      items.forEach { item ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Poster thumbnail representation
          Box(
            modifier = Modifier
              .size(width = 34.dp, height = 44.dp)
              .clip(RoundedCornerShape(FireLiteRadius.sm))
              .background(Color(item.posterAccentHex).copy(alpha = 0.3f))
              .border(1.dp, Color(item.posterAccentHex).copy(alpha = 0.5f), RoundedCornerShape(FireLiteRadius.sm)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = item.title.firstOrNull()?.toString() ?: "",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color(item.posterAccentHex)
            )
          }

          Spacer(modifier = Modifier.width(FireLiteSpacing.md))

          Column {
            Text(
              text = item.title,
              fontSize = FireLiteTypography.Secondary,
              fontWeight = FontWeight.Medium,
              color = FireLiteColors.PrimaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.platform,
              fontSize = FireLiteTypography.Metadata,
              color = FireLiteColors.SecondaryText
            )
          }
        }
      }
    }
  }
}

/**
 * Weather & Alert Panel (Section 16 of Design System)
 */
@Composable
fun WeatherAlertPanel(
  weather: WeatherAlertInfo,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(FireLiteRadius.lg))
      .background(FireLiteColors.Surface.copy(alpha = 0.95f))
      .border(1.dp, FireLiteColors.Border, RoundedCornerShape(FireLiteRadius.lg))
      .padding(FireLiteSpacing.card)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Cloud,
          contentDescription = "Weather",
          tint = FireLiteColors.DefaultAccent,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(FireLiteSpacing.sm))
        Text(
          text = weather.location,
          fontSize = FireLiteTypography.Secondary,
          fontWeight = FontWeight.Medium,
          color = FireLiteColors.PrimaryText
        )
      }

      Text(
        text = "${weather.temperature} • ${weather.condition}",
        fontSize = FireLiteTypography.Metadata,
        color = FireLiteColors.SecondaryText
      )
    }

    val alertTitle = weather.alertTitle
    val alertTime = weather.alertTime
    if (alertTitle != null && alertTime != null) {
      Spacer(modifier = Modifier.height(FireLiteSpacing.sm))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(FireLiteRadius.sm))
          .background(Color(0xFF3B1E12))
          .border(1.dp, Color(0xFFD97706), RoundedCornerShape(FireLiteRadius.sm))
          .padding(horizontal = FireLiteSpacing.sm, vertical = FireLiteSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.WarningAmber,
          contentDescription = "Alert",
          tint = Color(0xFFFBBF24),
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(FireLiteSpacing.xs))
        Text(
          text = "$alertTitle • $alertTime",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFFDE68A),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

/**
 * Floating App Dock (Section 17 of Design System)
 * Apps: JioHotstar, Netflix, Prime Video, YouTube, All Apps, Search, Settings, Screen Mirror
 */
@Composable
fun FloatingAppDock(
  apps: List<AppDockItem>,
  onSelectDockItem: (AppDockItem) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(FireLiteRadius.dock))
      .background(FireLiteColors.SurfaceElevated.copy(alpha = 0.88f))
      .border(1.dp, FireLiteColors.Border, RoundedCornerShape(FireLiteRadius.dock))
      .padding(horizontal = FireLiteSpacing.card, vertical = FireLiteSpacing.sm)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(FireLiteSpacing.md),
      verticalAlignment = Alignment.CenterVertically
    ) {
      apps.forEach { app ->
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        val scale by animateFloatAsState(
          targetValue = if (isFocused) 1.1f else 1.0f,
          animationSpec = tween(durationMillis = FireLiteMotion.Focus),
          label = "dockScale"
        )
        val translateY by animateDpAsState(
          targetValue = if (isFocused) (-3).dp else 0.dp,
          label = "dockTranslate"
        )
        val borderColor by animateColorAsState(
          targetValue = if (isFocused) FireLiteColors.DefaultAccent else Color.Transparent,
          label = "dockBorder"
        )

        val iconVector: ImageVector? = when (app.actionType) {
          "ALL_APPS" -> Icons.Default.Apps
          "SEARCH" -> Icons.Default.Search
          "SETTINGS" -> Icons.Default.Settings
          "SCREEN_MIRROR" -> Icons.Default.Cast
          else -> null
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .offset(y = translateY)
            .scale(scale)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) {
              onSelectDockItem(app)
            }
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(FireLiteRadius.md))
              .background(
                if (isFocused) FireLiteColors.SurfaceStrong else FireLiteColors.Surface
              )
              .border(1.5.dp, borderColor, RoundedCornerShape(FireLiteRadius.md)),
            contentAlignment = Alignment.Center
          ) {
            if (iconVector != null) {
              Icon(
                imageVector = iconVector,
                contentDescription = app.name,
                tint = if (isFocused) FireLiteColors.DefaultAccent else FireLiteColors.PrimaryText,
                modifier = Modifier.size(20.dp)
              )
            } else {
              // Streaming app abbreviation
              Text(
                text = app.name.take(2).uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) FireLiteColors.DefaultAccent else FireLiteColors.PrimaryText
              )
            }
          }

          if (isFocused) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = app.name,
              fontSize = FireLiteTypography.DockLabel,
              fontWeight = FontWeight.Medium,
              color = FireLiteColors.DefaultAccent
            )
          }
        }
      }
    }
  }
}
