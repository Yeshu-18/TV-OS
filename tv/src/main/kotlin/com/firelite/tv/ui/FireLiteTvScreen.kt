package com.firelite.tv.ui

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firelite.core.data.FireLiteRepository
import com.firelite.tv.util.TvAppItem
import com.firelite.tv.util.TvAppManager
import kotlinx.coroutines.delay

enum class TvOverlayState {
  NONE,
  SETTINGS,
  SCREEN_MIRROR
}

@Composable
fun FireLiteTvScreen(
  repository: FireLiteRepository,
  serverIp: String,
  connectedClients: Int,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var overlayState by remember { mutableStateOf(TvOverlayState.NONE) }
  var appList by remember { mutableStateOf(TvAppManager.getInstantFallbackApps()) }
  var focusedApp by remember { mutableStateOf<TvAppItem?>(appList.firstOrNull()) }

  val listState = rememberLazyListState()

  // Smooth automatic centering of the focused app during D-pad navigation
  LaunchedEffect(focusedApp) {
    val idx = appList.indexOf(focusedApp)
    if (idx >= 0) {
      listState.animateScrollToItem(
        index = (idx - 1).coerceAtLeast(0),
        scrollOffset = -60
      )
    }
  }

  val firstAppFocusRequester = remember { FocusRequester() }
  val settingsFocusRequester = remember { FocusRequester() }

  // 1. Asynchronously pre-warm & resolve all installed apps in parallel on background thread
  LaunchedEffect(Unit) {
    try {
      val resolved = TvAppManager.resolveInstalledAppsParallel(context)
      if (resolved.isNotEmpty()) {
        appList = resolved
        if (focusedApp == null) {
          focusedApp = resolved.firstOrNull()
        }
      }
    } catch (_: Exception) {}
  }

  // 2. Request initial D-pad focus on frame 0
  LaunchedEffect(Unit) {
    delay(50)
    try {
      firstAppFocusRequester.requestFocus()
    } catch (_: Exception) {}
  }

  // 3. Global BackHandler
  BackHandler(enabled = overlayState != TvOverlayState.NONE) {
    overlayState = TvOverlayState.NONE
  }

  // Fast application launcher using Couch-TV task reset flags
  fun launchApplication(packageName: String): Boolean {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
      ?: pm.getLaunchIntentForPackage(packageName)
      ?: return false
    return try {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }

  // Fire TV Settings launcher
  fun openTvSettings(): Boolean {
    val intents = listOf(
      Intent().setComponent(ComponentName("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.MainSettingsActivity")),
      Intent().setComponent(ComponentName("com.amazon.tv.settings.v2", "com.amazon.tv.settings.v2.tv.HomeActivity")),
      Intent(Settings.ACTION_SETTINGS)
    )
    for (i in intents) {
      try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
        return true
      } catch (_: Exception) {}
    }
    return false
  }

  // Fire TV Screen Mirroring launcher
  fun openScreenMirroring(): Boolean {
    val intents = listOf(
      Intent("com.amazon.device.settings.SYSTEM_MIRRORING"),
      Intent("android.settings.CAST_SETTINGS")
    )
    for (i in intents) {
      try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
        return true
      } catch (_: Exception) {}
    }
    return false
  }

  val onSelectApp: (TvAppItem) -> Unit = { item ->
    if (item.isSystemAction) {
      when (item.systemActionType) {
        "SETTINGS" -> {
          if (!openTvSettings()) overlayState = TvOverlayState.SETTINGS
        }
        "SCREEN_MIRROR" -> {
          if (!openScreenMirroring()) overlayState = TvOverlayState.SCREEN_MIRROR
        }
        else -> {}
      }
    } else if (!item.packageName.isNullOrEmpty()) {
      val ok = launchApplication(item.packageName)
      if (!ok) {
        Toast.makeText(context, "Could not open ${item.name}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Pure GPU RenderThread Animation (Zero Recomposition of Children)
  val activeColor = focusedApp?.brandColor ?: Color(0xFF0F1F38)
  val ambientAnim = remember { Animatable(activeColor) }
  LaunchedEffect(activeColor) {
    ambientAnim.animateTo(
      targetValue = activeColor,
      animationSpec = tween(durationMillis = 150) // Snappy 150ms crossfade
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF040608)) // Pure deep black
      .drawBehind {
        val c = ambientAnim.value
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(
              c.copy(alpha = 0.65f),
              c.copy(alpha = 0.20f),
              Color.Transparent
            ),
            center = Offset(size.width * 0.50f, size.height * 0.40f),
            radius = size.width * 0.70f
          )
        )
        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0x33000000),
              Color.Transparent,
              Color(0x77000000),
              Color(0xF0040608)
            )
          )
        )
      }
  ) {
    // Main UI Column
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 48.dp, vertical = 26.dp)
    ) {
      // Top Bar: Clean Brand on Left, Digital Clock, Date & Bare Settings Icon on Right
      TvTopBar(
        onSearchClick = {},
        onSettingsClick = {
          if (!openTvSettings()) overlayState = TvOverlayState.SETTINGS
        },
        settingsFocusRequester = settingsFocusRequester,
        downFocusRequester = firstAppFocusRequester
      )

      Spacer(modifier = Modifier.weight(0.20f))

      // Apple TV Style Active App Spotlight (Instantaneous text update, 0 animation overhead)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 6.dp, bottom = 18.dp)
      ) {
        Text(
          text = focusedApp?.name?.uppercase() ?: "APPLICATIONS",
          fontSize = 24.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = focusedApp?.tagline ?: "Use D-Pad to browse • Press OK to open",
          fontSize = 13.sp,
          fontWeight = FontWeight.Normal,
          color = Color(0xBBFFFFFF)
        )
      }

      // Horizontal Row of TV App Cards with Smooth Auto-Scroll
      LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        itemsIndexed(appList, key = { _, item -> item.id }) { index, app ->
          val tileModifier = Modifier
            .then(
              if (index == 0) Modifier.focusRequester(firstAppFocusRequester)
              else Modifier
            )
            .focusProperties {
              up = settingsFocusRequester
            }
            .onFocusChanged { focusState ->
              if (focusState.isFocused) {
                focusedApp = app
              }
            }

          val systemIcon = when (app.systemActionType) {
            "SETTINGS" -> Icons.Default.Settings
            "SCREEN_MIRROR" -> Icons.Default.Cast
            else -> null
          }

          TvAppTile(
            title = app.name,
            packageName = app.packageName,
            iconResName = app.iconResName,
            systemIcon = systemIcon,
            onSelect = { onSelectApp(app) },
            modifier = tileModifier
          )
        }
      }

      Spacer(modifier = Modifier.weight(0.35f))

      // Clean Minimalist Remote Hint at Bottom
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "FireLite • 60 FPS Fluid Engine • Zero Ads",
          fontSize = 11.sp,
          color = Color(0x33FFFFFF),
          fontWeight = FontWeight.Medium
        )
      }
    }

    // Overlays
    when (overlayState) {
      TvOverlayState.SCREEN_MIRROR -> {
        ScreenMirrorOverlay(
          connectedDevice = if (connectedClients > 0) "Remote connected: $serverIp" else null,
          onDismiss = { overlayState = TvOverlayState.NONE }
        )
      }
      TvOverlayState.SETTINGS -> {
        SettingsOverlay(
          onDismiss = { overlayState = TvOverlayState.NONE }
        )
      }
      else -> {}
    }
  }
}
