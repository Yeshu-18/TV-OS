package com.firelite.tv.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.core.theme.FireLiteColors
import com.firelite.core.theme.FireLiteRadius
import com.firelite.core.theme.FireLiteSpacing
import com.firelite.core.theme.FireLiteTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
  val label: String,
  val packageName: String,
  val iconDrawable: android.graphics.drawable.Drawable? = null
)

suspend fun loadInstalledApplications(context: Context): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
  val pm = context.packageManager
  val myPackage = context.packageName

  // Query both Leanback (Android TV / Fire TV) and standard Launcher intents
  val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
  val standardIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

  val leanbackApps = try { pm.queryIntentActivities(leanbackIntent, 0) } catch (_: Exception) { emptyList() }
  val standardApps = try { pm.queryIntentActivities(standardIntent, 0) } catch (_: Exception) { emptyList() }

  val allResolveInfos = (leanbackApps + standardApps).distinctBy { it.activityInfo.packageName }

  allResolveInfos
    .filter { it.activityInfo.packageName != myPackage }
    .map { resolveInfo ->
      val label = resolveInfo.loadLabel(pm).toString()
      val icon = try { resolveInfo.loadIcon(pm) } catch (_: Exception) { null }
      InstalledAppInfo(
        label = label,
        packageName = resolveInfo.activityInfo.packageName,
        iconDrawable = icon
      )
    }
    .sortedBy { it.label.lowercase() }
}

/**
 * All Apps Screen:
 * Pinned apps row at top (with verified Fire TV package identifiers),
 * subtle divider, and asynchronously loaded installed apps grid.
 */
@Composable
fun AllAppsOverlay(
  onDismiss: () -> Unit,
  onLaunchApp: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onDismiss)

  val context = LocalContext.current
  var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    installedApps = loadInstalledApplications(context)
    isLoading = false
  }

  // Pinned OTT Apps with verified packages and icon resources
  val pinnedApps = remember {
    listOf(
      Triple("JioHotstar", "in.startv.hotstar", "app_jiohotstar"),
      Triple("Netflix", "com.netflix.ninja", "app_netflix"),
      Triple("Prime Video", "com.amazon.firebat", "app_primevideo"),
      Triple("YouTube", "com.amazon.firetv.youtube", "app_youtube")
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xF0080B12))
      .padding(horizontal = 32.dp, vertical = 20.dp)
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
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Apps",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Pinned Apps Row
      Text(
        text = "FAVORITES",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = Color(0x99D1D5DB)
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        pinnedApps.forEach { (name, pkg, iconRes) ->
          AppItemCard(
            label = name,
            packageName = pkg,
            iconResName = iconRes,
            isPinned = true,
            onClick = { onLaunchApp(pkg) }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = Color(0x26FFFFFF))
      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "ALL INSTALLED APPS",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = Color(0x99D1D5DB)
      )
      Spacer(modifier = Modifier.height(8.dp))

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(32.dp))
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 130.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          items(installedApps, key = { it.packageName }) { app ->
            AppItemCard(
              label = app.label,
              appDrawable = app.iconDrawable,
              isPinned = false,
              onClick = { onLaunchApp(app.packageName) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun AppItemCard(
  label: String,
  packageName: String? = null,
  iconResName: String? = null,
  appDrawable: android.graphics.drawable.Drawable? = null,
  isPinned: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val dynamicIcon = remember(packageName) {
    com.firelite.tv.util.AppIconLoader.getAppIcon(context, packageName)
  }
  val iconResId = getDrawableResId(iconResName)

  Box(
    modifier = modifier
      .width(130.dp)
      .height(82.dp)
      .scale(if (isFocused) 1.05f else 1.0f)
      .clip(RoundedCornerShape(12.dp))
      .background(if (isFocused) Color(0x4D38BDF8) else Color(0x331E293B))
      .border(
        if (isFocused) 1.5.dp else 1.dp,
        if (isFocused) Color(0xFF38BDF8) else Color(0x26FFFFFF),
        RoundedCornerShape(12.dp)
      )
      .clickable(interactionSource = interactionSource, indication = null) {
        onClick()
      }
      .padding(8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (dynamicIcon != null) {
        Image(
          bitmap = dynamicIcon,
          contentDescription = label,
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
        )
      } else if (iconResId != null) {
        Image(
          painter = painterResource(id = iconResId),
          contentDescription = label,
          modifier = Modifier.size(32.dp)
        )
      } else if (appDrawable != null) {
        val bitmap = remember(appDrawable) {
          try {
            appDrawable.toBitmap(width = 64, height = 64)
          } catch (_: Exception) {
            null
          }
        }
        if (bitmap != null) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            modifier = Modifier.size(32.dp)
          )
        } else {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label.take(1).uppercase(),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = label.take(2).uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Search Screen with Instant Filter
 */
@Composable
fun SearchOverlay(
  pinnedItems: List<MediaItem>,
  onDismiss: () -> Unit,
  onSelectItem: (MediaItem) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onDismiss)

  var searchQuery by remember { mutableStateOf("") }
  val filteredPins = remember(searchQuery, pinnedItems) {
    if (searchQuery.isEmpty()) pinnedItems
    else pinnedItems.filter { it.title.contains(searchQuery, ignoreCase = true) }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xF0080B12))
      .padding(horizontal = 32.dp, vertical = 20.dp)
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
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search your shows, apps and content...", color = Color(0x80D1D5DB)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(0.7f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0x33FFFFFF),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Results (${filteredPins.size})",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        filteredPins.take(6).forEach { item ->
          PinnedContentCard(
            item = item,
            category = item.category,
            onSelect = onSelectItem,
            onFocus = {}
          )
        }
      }
    }
  }
}

/**
 * Screen Mirror Overlay
 */
@Composable
fun ScreenMirrorOverlay(
  connectedDevice: String?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onDismiss)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xF0080B12))
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(Color(0x3338BDF8))
          .border(2.dp, Color(0xFF38BDF8), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Cast,
          contentDescription = "Screen Mirror",
          tint = Color(0xFF38BDF8),
          modifier = Modifier.size(32.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Screen Mirroring",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = if (connectedDevice != null) "$connectedDevice • Ready to cast" else "Connect via Fire TV Display Mirroring or FireLite Companion",
        fontSize = 12.sp,
        color = if (connectedDevice != null) Color(0xFF34D399) else Color(0x99D1D5DB)
      )

      Spacer(modifier = Modifier.height(20.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0x331E293B))
          .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
          .clickable(onClick = onDismiss)
          .padding(horizontal = 24.dp, vertical = 8.dp)
      ) {
        Text(
          text = "Close",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White
        )
      }
    }
  }
}

/**
 * Settings Screen
 */
@Composable
fun SettingsOverlay(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onDismiss)

  val categories = remember {
    listOf(
      "General",
      "Pinned Content",
      "Appearance",
      "Companion Remote",
      "Weather & Alerts",
      "What's New Feed",
      "Screen Mirror",
      "About FireLite"
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xF0080B12))
      .padding(horizontal = 32.dp, vertical = 20.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Settings",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      categories.forEach { cat ->
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0x3338BDF8) else Color.Transparent)
            .border(
              1.dp,
              if (isFocused) Color(0xFF38BDF8) else Color.Transparent,
              RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text(
            text = cat,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) Color(0xFF38BDF8) else Color.White
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
      }
    }
  }
}
