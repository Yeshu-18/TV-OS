package com.firelite.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firelite.core.model.AppDockItem
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.core.model.WeatherAlertInfo
import com.firelite.core.model.WhatsNewItem
import com.firelite.core.theme.FireLiteMotion
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun getDrawableResId(name: String?): Int? {
  if (name.isNullOrEmpty()) return null
  val context = LocalContext.current
  val id = remember(name) {
    context.resources.getIdentifier(name, "drawable", context.packageName)
  }
  return if (id != 0) id else null
}

/**
 * Top Bar:
 * Left: "FIRELITE" uppercase brand + "YOUR SHOWS • YOUR WAY"
 * Right: Clock time (10:42 PM) + Date (Thu, 23 Oct) + Glass Search & Settings circular buttons.
 * Optimized with minute-aligned timer (no per-second recompositions).
 */
@Composable
fun TvTopBar(
  onSearchClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentTime by remember { mutableStateOf("") }
  var currentDate by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    while (true) {
      val now = Date()
      currentTime = timeFormat.format(now)
      currentDate = dateFormat.format(now)

      val calendar = Calendar.getInstance()
      val second = calendar.get(Calendar.SECOND)
      val millis = calendar.get(Calendar.MILLISECOND)
      val delayMs = ((60 - second) * 1000L - millis).coerceAtLeast(1000L)
      delay(delayMs)
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Brand identity
    Column {
      Text(
        text = "FIRELITE",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = Color.White
      )
      Text(
        text = "YOUR SHOWS • YOUR WAY",
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.4.sp,
        color = Color(0xB3D1D5DB)
      )
    }

    // Top Right: Time, Date, Search, Settings
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = if (currentTime.isNotEmpty()) currentTime else "10:42 PM",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White
        )
        Text(
          text = if (currentDate.isNotEmpty()) currentDate else "Thu, 23 Oct",
          fontSize = 10.sp,
          fontWeight = FontWeight.Normal,
          color = Color(0x99D1D5DB)
        )
      }

      TopBarGlassIconButton(
        icon = Icons.Default.Search,
        contentDescription = "Search",
        onClick = onSearchClick
      )

      TopBarGlassIconButton(
        icon = Icons.Default.Settings,
        contentDescription = "Settings",
        onClick = onSettingsClick
      )
    }
  }
}

@Composable
fun TopBarGlassIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.15f else 1.0f,
    animationSpec = tween(FireLiteMotion.Focus),
    label = "topIconScale"
  )
  val borderColor by animateColorAsState(
    targetValue = if (isFocused) Color(0xFF38BDF8) else Color(0x33FFFFFF),
    label = "topIconBorder"
  )

  Box(
    modifier = modifier
      .size(34.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(CircleShape)
      .background(if (isFocused) Color(0x661E293B) else Color(0x330F172A))
      .border(1.2.dp, borderColor, CircleShape)
      .clickable(interactionSource = interactionSource, indication = null) {
        onClick()
      },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (isFocused) Color(0xFF38BDF8) else Color(0xEEFFFFFF),
      modifier = Modifier.size(16.dp)
    )
  }
}

/**
 * 2.5D Pinned Content Card:
 * Hardware-accelerated with graphicsLayer (scale and translationY).
 * Zero layout recalculation during focus animation ensures 60 FPS on Fire TV.
 */
@Composable
fun PinnedContentCard(
  item: MediaItem,
  category: Category,
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

  val scale by animateFloatAsState(
    targetValue = when {
      isPressed -> 0.96f
      isFocused -> 1.08f
      else -> 1.0f
    },
    animationSpec = tween(durationMillis = FireLiteMotion.Focus),
    label = "cardScale"
  )

  val density = LocalDensity.current
  val translateYPx = remember(density) { with(density) { (-4).dp.toPx() } }
  val currentTranslateY = if (isFocused) translateYPx else 0f

  val borderColor by animateColorAsState(
    targetValue = if (isFocused) Color(0xFF38BDF8) else Color(0x2EFFFFFF),
    animationSpec = tween(durationMillis = FireLiteMotion.Focus),
    label = "cardBorderColor"
  )

  val isKids = category == Category.KIDS
  val cardWidth = if (isKids) 94.dp else 104.dp
  val cardHeight = if (isKids) 82.dp else 72.dp
  val cornerRadius = if (isKids) 14.dp else 12.dp

  val artworkResId = getDrawableResId(item.artworkResName)
  val charResId = getDrawableResId(item.characterResName)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.width(cardWidth)
  ) {
    Box(
      modifier = Modifier
        .size(width = cardWidth, height = cardHeight)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
          translationY = currentTranslateY
        }
        .clip(RoundedCornerShape(cornerRadius))
        .background(Color(0xFF131722))
        .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(cornerRadius))
        .clickable(interactionSource = interactionSource, indication = null) {
          onSelect(item)
        }
    ) {
      // 1. Content Artwork Background
      if (artworkResId != null) {
        Image(
          painter = painterResource(id = artworkResId),
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color(item.accentColorHex).copy(alpha = 0.45f),
                  Color(0xFF0F172A)
                )
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = item.title.firstOrNull()?.toString() ?: "",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(item.accentColorHex)
          )
        }
      }

      // 2. Character pop-out layer for Kids (if available)
      if (isKids && charResId != null && isFocused) {
        Image(
          painter = painterResource(id = charResId),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxHeight(0.9f)
            .align(Alignment.BottomCenter)
            .offset(y = (-2).dp)
        )
      }

      // 3. Floating label pill at the bottom of the card on focus (Reference design)
      if (isFocused) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xD9080B12))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = item.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    // 4. Stable height title label underneath card (alpha transition avoids layout remeasurement)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = item.title,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      color = Color(0xCCFFFFFF),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .alpha(if (isFocused) 0f else 1f)
    )
  }
}

/**
 * Pinned Content Row with Category Header, focusGroup for memory restoration, and Scroll Chevron
 */
@Composable
fun PinnedContentRow(
  category: Category,
  items: List<MediaItem>,
  onSelectItem: (MediaItem) -> Unit,
  onFocusItem: (MediaItem) -> Unit,
  onScrollMore: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isKids = category == Category.KIDS

  Column(modifier = modifier.fillMaxWidth()) {
    // Row Header (Section Title)
    if (isKids) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
      ) {
        Text(text = "📌", fontSize = 11.sp)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = "PINNED",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
          color = Color(0xCCE2E8F0)
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 28.dp, bottom = 6.dp)
      ) {
        Text(text = "🧸", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "KIDS",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp,
          color = Color.White
        )
      }
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 28.dp, top = 2.dp, bottom = 6.dp)
      ) {
        Text(text = "🎬", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "SERIES",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp,
          color = Color.White
        )
      }
    }

    // Horizontal Card Row with focusGroup for automatic focus position restoration
    LazyRow(
      contentPadding = PaddingValues(start = 28.dp, end = 20.dp, top = 2.dp, bottom = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .focusGroup()
    ) {
      items(items, key = { it.id }) { item ->
        PinnedContentCard(
          item = item,
          category = category,
          onSelect = onSelectItem,
          onFocus = onFocusItem
        )
      }

      // Scroll Chevron at the end of the row
      item {
        ChevronScrollButton(onClick = onScrollMore)
      }
    }
  }
}

@Composable
fun ChevronScrollButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  Box(
    modifier = modifier
      .size(28.dp)
      .clip(CircleShape)
      .background(if (isFocused) Color(0x6638BDF8) else Color(0x33000000))
      .border(1.dp, if (isFocused) Color(0xFF38BDF8) else Color(0x33FFFFFF), CircleShape)
      .clickable(interactionSource = interactionSource, indication = null) {
        onClick()
      },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = "More",
      tint = if (isFocused) Color(0xFF38BDF8) else Color(0xCCFFFFFF),
      modifier = Modifier.size(16.dp)
    )
  }
}

/**
 * What's New Panel (Right Rail):
 * Frosted translucent glass container, Star header, 4 rich media rows with thumbnails and OTT logos.
 */
@Composable
fun WhatsNewPanel(
  items: List<WhatsNewItem>,
  onSelectItem: (WhatsNewItem) -> Unit = {},
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0x38121826))
      .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(16.dp))
      .focusGroup()
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Column {
      // Header: Star + "What's New" + Chevron
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "What's New",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
          contentDescription = null,
          tint = Color(0x80FFFFFF),
          modifier = Modifier.size(10.dp)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Media items
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.take(4).forEach { item ->
          WhatsNewRowItem(
            item = item,
            onClick = { onSelectItem(item) }
          )
        }
      }
    }
  }
}

@Composable
fun WhatsNewRowItem(
  item: WhatsNewItem,
  onClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val posterResId = getDrawableResId(item.posterResName)
  val platformIconId = getDrawableResId(item.platformIconResName)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(if (isFocused) Color(0x3338BDF8) else Color.Transparent)
      .border(
        1.dp,
        if (isFocused) Color(0xFF38BDF8) else Color.Transparent,
        RoundedCornerShape(8.dp)
      )
      .clickable(interactionSource = interactionSource, indication = null) {
        onClick()
      }
      .padding(horizontal = 4.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Thumbnail (40x30)
    Box(
      modifier = Modifier
        .size(width = 38.dp, height = 28.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF1E293B)),
      contentAlignment = Alignment.Center
    ) {
      if (posterResId != null) {
        Image(
          painter = painterResource(id = posterResId),
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Text(
          text = item.title.firstOrNull()?.toString() ?: "",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(item.posterAccentHex)
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Title & Platform info
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (platformIconId != null) {
          Image(
            painter = painterResource(id = platformIconId),
            contentDescription = null,
            modifier = Modifier.size(10.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
          text = "${item.platform} • ${item.contentType}",
          fontSize = 9.sp,
          fontWeight = FontWeight.Normal,
          color = Color(0x99D1D5DB)
        )
      }
    }
  }
}

/**
 * Weather & Alert Panels (Side-by-side translucent glass cards in Right Rail)
 */
@Composable
fun WeatherAlertPanel(
  weather: WeatherAlertInfo,
  onWeatherClick: () -> Unit = {},
  onAlertClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .focusGroup(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Left: Weather Card
    val weatherInteraction = remember { MutableInteractionSource() }
    val isWeatherFocused by weatherInteraction.collectIsFocusedAsState()

    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(14.dp))
        .background(if (isWeatherFocused) Color(0x551E293B) else Color(0x38121826))
        .border(
          1.dp,
          if (isWeatherFocused) Color(0xFF38BDF8) else Color(0x29FFFFFF),
          RoundedCornerShape(14.dp)
        )
        .clickable(interactionSource = weatherInteraction, indication = null) {
          onWeatherClick()
        }
        .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Cloud,
          contentDescription = "Weather",
          tint = Color(0xFF67E8F9),
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = weather.location,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0x99D1D5DB)
          )
          Text(
            text = weather.temperature,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = weather.condition,
            fontSize = 8.sp,
            color = Color(0x80D1D5DB)
          )
        }
      }
    }

    // Right: Amber Alert Card
    val alertInteraction = remember { MutableInteractionSource() }
    val isAlertFocused by alertInteraction.collectIsFocusedAsState()

    Box(
      modifier = Modifier
        .weight(1.3f)
        .clip(RoundedCornerShape(14.dp))
        .background(if (isAlertFocused) Color(0x554B1D1D) else Color(0x38351515))
        .border(
          1.dp,
          if (isAlertFocused) Color(0xFFEF4444) else Color(0x40EF4444),
          RoundedCornerShape(14.dp)
        )
        .clickable(interactionSource = alertInteraction, indication = null) {
          onAlertClick()
        }
        .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Alert",
            tint = Color(0xFFF87171),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = weather.alertTitle ?: "Heavy rain expected",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFCA5A5),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = weather.alertTime ?: "6:00 PM – 8:00 PM",
              fontSize = 8.sp,
              color = Color(0x99FECACA)
            )
          }
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
          contentDescription = null,
          tint = Color(0x80EF4444),
          modifier = Modifier.size(9.dp)
        )
      }
    }
  }
}

/**
 * Floating App Dock:
 * Exactly 8 items: JioHotstar, Netflix, Prime Video, YouTube, All Apps, Search, Settings, Screen Mirror.
 * (NO Power on TV dock)
 * Hardware-accelerated with graphicsLayer.
 */
@Composable
fun FloatingAppDock(
  apps: List<AppDockItem>,
  onSelectDockItem: (AppDockItem) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(26.dp))
      .background(Color(0x4D0C101A))
      .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(26.dp))
      .focusGroup()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      apps.forEach { app ->
        AppDockButton(app = app, onSelect = { onSelectDockItem(app) })
      }
    }
  }
}

@Composable
fun AppDockButton(
  app: AppDockItem,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.12f else 1.0f,
    animationSpec = tween(durationMillis = FireLiteMotion.Focus),
    label = "dockScale"
  )
  val density = LocalDensity.current
  val translateYPx = remember(density) { with(density) { (-3).dp.toPx() } }
  val currentTranslateY = if (isFocused) translateYPx else 0f

  val borderColor by animateColorAsState(
    targetValue = if (isFocused) Color(0xFF38BDF8) else Color(0x1AFFFFFF),
    label = "dockBorder"
  )

  val iconResId = getDrawableResId(app.iconResName)
  val systemIcon: ImageVector? = when (app.actionType) {
    "ALL_APPS" -> Icons.Default.Apps
    "SEARCH" -> Icons.Default.Search
    "SETTINGS" -> Icons.Default.Settings
    "SCREEN_MIRROR" -> Icons.Default.Cast
    else -> null
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = currentTranslateY
      }
      .clickable(interactionSource = interactionSource, indication = null) {
        onSelect()
      }
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(
          if (isFocused) Color(0x661E293B) else Color(0x330F172A)
        )
        .border(1.2.dp, borderColor, RoundedCornerShape(10.dp)),
      contentAlignment = Alignment.Center
    ) {
      if (iconResId != null) {
        Image(
          painter = painterResource(id = iconResId),
          contentDescription = app.name,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(24.dp)
        )
      } else if (systemIcon != null) {
        Icon(
          imageVector = systemIcon,
          contentDescription = app.name,
          tint = if (isFocused) Color(0xFF38BDF8) else Color(0xEEFFFFFF),
          modifier = Modifier.size(18.dp)
        )
      } else {
        Text(
          text = app.name.take(2).uppercase(),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = if (isFocused) Color(0xFF38BDF8) else Color.White
        )
      }
    }

    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = app.name,
      fontSize = 9.sp,
      fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
      color = if (isFocused) Color(0xFF38BDF8) else Color(0xB3FFFFFF),
      maxLines = 1
    )
  }
}
