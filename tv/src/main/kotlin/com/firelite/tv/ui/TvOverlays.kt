package com.firelite.tv.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firelite.core.model.MediaItem
import com.firelite.core.theme.FireLiteColors
import com.firelite.core.theme.FireLiteRadius
import com.firelite.core.theme.FireLiteSpacing
import com.firelite.core.theme.FireLiteTypography

data class InstalledAppInfo(
  val label: String,
  val packageName: String
)

fun getInstalledApplications(context: Context): List<InstalledAppInfo> {
  val pm = context.packageManager
  val intent = Intent(Intent.ACTION_MAIN, null).apply {
    addCategory(Intent.CATEGORY_LAUNCHER)
  }
  return try {
    pm.queryIntentActivities(intent, 0).map { resolveInfo ->
      InstalledAppInfo(
        label = resolveInfo.loadLabel(pm).toString(),
        packageName = resolveInfo.activityInfo.packageName
      )
    }.distinctBy { it.packageName }
  } catch (_: Exception) {
    emptyList()
  }
}

/**
 * All Apps Screen (Section 18, 19 of Design System)
 * Pinned apps first, subtle divider, then dynamically discovered installed apps.
 */
@Composable
fun AllAppsOverlay(
  onDismiss: () -> Unit,
  onLaunchApp: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val installedApps = remember { getInstalledApplications(context) }
  val pinnedApps = listOf("JioHotstar", "Netflix", "Prime Video", "YouTube")

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FireLiteColors.Background.copy(alpha = 0.96f))
      .padding(FireLiteSpacing.screen)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = FireLiteColors.PrimaryText
          )
        }
        Spacer(modifier = Modifier.width(FireLiteSpacing.sm))
        Text(
          text = "Apps",
          fontSize = FireLiteTypography.ScreenTitle,
          fontWeight = FontWeight.SemiBold,
          color = FireLiteColors.PrimaryText
        )
      }

      Spacer(modifier = Modifier.height(FireLiteSpacing.lg))

      // Pinned Apps Row
      Row(
        horizontalArrangement = Arrangement.spacedBy(FireLiteSpacing.card),
        modifier = Modifier.fillMaxWidth()
      ) {
        pinnedApps.forEach { name ->
          AppItemCard(
            label = name,
            isPinned = true,
            onClick = { onLaunchApp(name) }
          )
        }
      }

      Spacer(modifier = Modifier.height(FireLiteSpacing.lg))
      HorizontalDivider(color = FireLiteColors.Border)
      Spacer(modifier = Modifier.height(FireLiteSpacing.lg))

      // Installed Apps Grid
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(FireLiteSpacing.card),
        verticalArrangement = Arrangement.spacedBy(FireLiteSpacing.card),
        modifier = Modifier.weight(1f)
      ) {
        items(installedApps, key = { it.packageName }) { app ->
          AppItemCard(
            label = app.label,
            isPinned = false,
            onClick = { onLaunchApp(app.packageName) }
          )
        }
      }
    }
  }
}

@Composable
fun AppItemCard(
  label: String,
  isPinned: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  Box(
    modifier = modifier
      .width(140.dp)
      .height(100.dp)
      .scale(if (isFocused) 1.06f else 1.0f)
      .clip(RoundedCornerShape(FireLiteRadius.lg))
      .background(if (isFocused) FireLiteColors.SurfaceElevated else FireLiteColors.Surface)
      .border(
        1.5.dp,
        if (isFocused) FireLiteColors.DefaultAccent else FireLiteColors.Border,
        RoundedCornerShape(FireLiteRadius.lg)
      )
      .focusable(interactionSource = interactionSource)
      .clickable(interactionSource = interactionSource, indication = null) {
        onClick()
      }
      .padding(FireLiteSpacing.md),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(FireLiteColors.SurfaceStrong),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = label.take(2).uppercase(),
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = FireLiteColors.DefaultAccent
        )
      }
      Spacer(modifier = Modifier.height(FireLiteSpacing.xs))
      Text(
        text = label,
        fontSize = FireLiteTypography.Metadata,
        fontWeight = FontWeight.Medium,
        color = FireLiteColors.PrimaryText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Search Screen (Section 23 of Design System)
 * Filtered by: Pinned, Apps, Content
 */
@Composable
fun SearchOverlay(
  pinnedItems: List<MediaItem>,
  onDismiss: () -> Unit,
  onSelectItem: (MediaItem) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredPins = remember(searchQuery) {
    if (searchQuery.isEmpty()) pinnedItems
    else pinnedItems.filter { it.title.contains(searchQuery, ignoreCase = true) }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FireLiteColors.Background.copy(alpha = 0.96f))
      .padding(FireLiteSpacing.screen)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = FireLiteColors.PrimaryText
          )
        }
        Spacer(modifier = Modifier.width(FireLiteSpacing.sm))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search your shows, apps, and TV...", color = FireLiteColors.TertiaryText) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(0.7f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FireLiteColors.DefaultAccent,
            unfocusedBorderColor = FireLiteColors.Border,
            focusedTextColor = FireLiteColors.PrimaryText,
            unfocusedTextColor = FireLiteColors.PrimaryText
          )
        )
      }

      Spacer(modifier = Modifier.height(FireLiteSpacing.lg))

      Text(
        text = "Pinned Results (${filteredPins.size})",
        fontSize = FireLiteTypography.SectionTitle,
        fontWeight = FontWeight.SemiBold,
        color = FireLiteColors.PrimaryText
      )

      Spacer(modifier = Modifier.height(FireLiteSpacing.md))

      Row(
        horizontalArrangement = Arrangement.spacedBy(FireLiteSpacing.card),
        modifier = Modifier.fillMaxWidth()
      ) {
        filteredPins.take(5).forEach { item ->
          PinnedContentCard(
            item = item,
            onSelect = onSelectItem,
            onFocus = {}
          )
        }
      }
    }
  }
}

/**
 * Screen Mirror Screen (Section 24 of Design System)
 */
@Composable
fun ScreenMirrorOverlay(
  connectedDevice: String?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FireLiteColors.Background.copy(alpha = 0.96f))
      .padding(FireLiteSpacing.screen),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(FireLiteColors.SurfaceElevated)
          .border(2.dp, FireLiteColors.DefaultAccent, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Cast,
          contentDescription = "Screen Mirror",
          tint = FireLiteColors.DefaultAccent,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(FireLiteSpacing.lg))

      Text(
        text = "Screen Mirror",
        fontSize = FireLiteTypography.ScreenTitle,
        fontWeight = FontWeight.SemiBold,
        color = FireLiteColors.PrimaryText
      )

      Spacer(modifier = Modifier.height(FireLiteSpacing.sm))

      Text(
        text = if (connectedDevice != null) "$connectedDevice • Connected" else "Waiting for device...",
        fontSize = FireLiteTypography.Body,
        color = if (connectedDevice != null) Color(0xFF34D399) else FireLiteColors.SecondaryText
      )

      Spacer(modifier = Modifier.height(FireLiteSpacing.xl))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(FireLiteRadius.md))
          .background(FireLiteColors.Surface)
          .border(1.dp, FireLiteColors.Border, RoundedCornerShape(FireLiteRadius.md))
          .clickable(onClick = onDismiss)
          .padding(horizontal = FireLiteSpacing.lg, vertical = FireLiteSpacing.md)
      ) {
        Text(
          text = "Close",
          fontSize = FireLiteTypography.Secondary,
          fontWeight = FontWeight.SemiBold,
          color = FireLiteColors.PrimaryText
        )
      }
    }
  }
}

/**
 * Settings Screen (Section 20 of Design System)
 * Categories: General, Pinned Content, Appearance, Remote, Weather, What's New, Screen Mirror, About
 */
@Composable
fun SettingsOverlay(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val categories = listOf(
    "General",
    "Pinned Content",
    "Appearance",
    "Remote",
    "Weather",
    "What's New",
    "Screen Mirror",
    "About"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FireLiteColors.Background.copy(alpha = 0.96f))
      .padding(FireLiteSpacing.screen)
  ) {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = FireLiteColors.PrimaryText
          )
        }
        Spacer(modifier = Modifier.width(FireLiteSpacing.sm))
        Text(
          text = "Settings",
          fontSize = FireLiteTypography.ScreenTitle,
          fontWeight = FontWeight.SemiBold,
          color = FireLiteColors.PrimaryText
        )
      }

      Spacer(modifier = Modifier.height(FireLiteSpacing.lg))

      categories.forEach { cat ->
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FireLiteRadius.md))
            .background(if (isFocused) FireLiteColors.SurfaceElevated else Color.Transparent)
            .border(
              1.dp,
              if (isFocused) FireLiteColors.DefaultAccent else Color.Transparent,
              RoundedCornerShape(FireLiteRadius.md)
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = FireLiteSpacing.md, vertical = FireLiteSpacing.sm)
        ) {
          Text(
            text = cat,
            fontSize = FireLiteTypography.Body,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) FireLiteColors.DefaultAccent else FireLiteColors.PrimaryText
          )
        }
        Spacer(modifier = Modifier.height(FireLiteSpacing.xs))
      }
    }
  }
}
