package com.firelite.tv.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.firelite.core.data.FireLiteRepository
import com.firelite.core.model.AppDockItem
import com.firelite.core.model.MediaItem
import com.firelite.core.theme.FireLiteColors
import com.firelite.core.theme.FireLiteSpacing

enum class TvOverlayState {
  NONE,
  ALL_APPS,
  SEARCH,
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
  val kidsItems by repository.kidsItems.collectAsState()
  val seriesItems by repository.seriesItems.collectAsState()
  val whatsNew by repository.whatsNew.collectAsState()
  val weather by repository.weather.collectAsState()
  val dockItems by repository.dockItems.collectAsState()

  var overlayState by remember { mutableStateOf(TvOverlayState.NONE) }

  // Deep linking launch flow (Section 22)
  val onSelectMedia: (MediaItem) -> Unit = { item ->
    repository.setActiveContent(item)
    val contentUrl = item.contentUrl
    val deepLinkPackage = item.deepLinkPackage
    var launched = false
    if (contentUrl != null) {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contentUrl))
        if (deepLinkPackage != null) {
          intent.setPackage(deepLinkPackage)
        }
        context.startActivity(intent)
        launched = true
      } catch (_: Exception) {}
    }

    if (!launched && deepLinkPackage != null) {
      try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(deepLinkPackage)
        if (launchIntent != null) {
          context.startActivity(launchIntent)
          launched = true
        }
      } catch (_: Exception) {}
    }

    if (!launched) {
      Toast.makeText(context, "Opening ${item.title} on ${item.platform}", Toast.LENGTH_SHORT).show()
    }
  }

  val onSelectDockItem: (AppDockItem) -> Unit = { dockItem ->
    when (dockItem.actionType) {
      "ALL_APPS" -> overlayState = TvOverlayState.ALL_APPS
      "SEARCH" -> overlayState = TvOverlayState.SEARCH
      "SETTINGS" -> overlayState = TvOverlayState.SETTINGS
      "SCREEN_MIRROR" -> overlayState = TvOverlayState.SCREEN_MIRROR
      else -> {
        var launched = false
        if (dockItem.packageName.isNotEmpty()) {
          try {
            val intent = context.packageManager.getLaunchIntentForPackage(dockItem.packageName)
            if (intent != null) {
              context.startActivity(intent)
              launched = true
            }
          } catch (_: Exception) {}
        }
        if (!launched) {
          Toast.makeText(context, "Launching ${dockItem.name}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(FireLiteColors.Background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 76.dp)
    ) {
      // Top Area: Small identity & status (Section 11)
      TvTopBar(
        serverIp = serverIp,
        connectedClients = connectedClients
      )

      // Main Grid: 75–80% Pinned Content | 20–25% Right Information Rail (Section 12)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        // Left Content: Pinned Rows (Kids & Series)
        Column(
          modifier = Modifier
            .weight(0.76f)
            .fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(FireLiteSpacing.xs)
        ) {
          // Kids Row (Section 13)
          PinnedContentRow(
            title = "Kids",
            items = kidsItems,
            onSelectItem = onSelectMedia,
            onFocusItem = { repository.setActiveContent(it) }
          )

          // Series Row (Section 14)
          PinnedContentRow(
            title = "Series",
            items = seriesItems,
            onSelectItem = onSelectMedia,
            onFocusItem = { repository.setActiveContent(it) }
          )
        }

        // Right Rail: What's New & Weather/Alert Panel
        Column(
          modifier = Modifier
            .weight(0.24f)
            .padding(end = FireLiteSpacing.screen, top = FireLiteSpacing.xs)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(FireLiteSpacing.md)
        ) {
          // What's New (Section 15)
          WhatsNewPanel(items = whatsNew)

          // Weather / Alert Panel (Section 16)
          WeatherAlertPanel(weather = weather)
        }
      }
    }

    // Floating App Dock at the bottom center (Section 17)
    FloatingAppDock(
      apps = dockItems,
      onSelectDockItem = onSelectDockItem,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = FireLiteSpacing.card)
    )

    // Overlays
    when (overlayState) {
      TvOverlayState.ALL_APPS -> {
        AllAppsOverlay(
          onDismiss = { overlayState = TvOverlayState.NONE },
          onLaunchApp = { pkg ->
            try {
              val intent = context.packageManager.getLaunchIntentForPackage(pkg)
              if (intent != null) context.startActivity(intent)
              else Toast.makeText(context, "Opening $pkg", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
          }
        )
      }
      TvOverlayState.SEARCH -> {
        SearchOverlay(
          pinnedItems = kidsItems + seriesItems,
          onDismiss = { overlayState = TvOverlayState.NONE },
          onSelectItem = { item ->
            overlayState = TvOverlayState.NONE
            onSelectMedia(item)
          }
        )
      }
      TvOverlayState.SCREEN_MIRROR -> {
        ScreenMirrorOverlay(
          connectedDevice = if (connectedClients > 0) "Yeshwanth's Phone" else null,
          onDismiss = { overlayState = TvOverlayState.NONE }
        )
      }
      TvOverlayState.SETTINGS -> {
        SettingsOverlay(
          onDismiss = { overlayState = TvOverlayState.NONE }
        )
      }
      TvOverlayState.NONE -> {}
    }
  }
}
