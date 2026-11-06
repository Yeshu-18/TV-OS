package com.firelite.tv.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.firelite.core.data.FireLiteRepository
import com.firelite.core.model.AppDockItem
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.core.model.WhatsNewItem
import com.firelite.tv.R
import kotlinx.coroutines.delay

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

  // Target FocusRequesters bound directly to focusable items (Search, Cards, Dock, Rail)
  val searchFocusRequester = remember { FocusRequester() }
  val settingsFocusRequester = remember { FocusRequester() }
  val kidsCardFocusRequester = remember { FocusRequester() }
  val seriesCardFocusRequester = remember { FocusRequester() }
  val whatsNewFocusRequester = remember { FocusRequester() }
  val weatherFocusRequester = remember { FocusRequester() }
  val dockFocusRequester = remember { FocusRequester() }

  // Request initial focus on first Kids card (Doraemon) matching reference design
  LaunchedEffect(Unit) {
    delay(150)
    try {
      kidsCardFocusRequester.requestFocus()
    } catch (_: Exception) {}
  }

  // Global BackHandler for overlay dismissal
  BackHandler(enabled = overlayState != TvOverlayState.NONE) {
    overlayState = TvOverlayState.NONE
  }

  // Safe launch helper with Fire TV fallbacks
  fun launchPackageWithFallback(primaryPkg: String, fallbackPkg: String? = null): Boolean {
    val pm = context.packageManager
    var intent = pm.getLaunchIntentForPackage(primaryPkg)
      ?: pm.getLeanbackLaunchIntentForPackage(primaryPkg)
    if (intent == null && fallbackPkg != null) {
      intent = pm.getLaunchIntentForPackage(fallbackPkg)
        ?: pm.getLeanbackLaunchIntentForPackage(fallbackPkg)
    }
    return if (intent != null) {
      try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
      } catch (_: Exception) {
        false
      }
    } else {
      false
    }
  }

  // Robust content launcher verifying intents, search fallbacks, and provider apps
  val onSelectMedia: (MediaItem) -> Unit = { item ->
    repository.setActiveContent(item)
    val contentUrl = item.contentUrl
    val fallbackUrl = item.fallbackUrl
    val deepLinkPackage = item.deepLinkPackage
    var launched = false

    // 1. Try primary content deep link (with FLAG_ACTIVITY_NEW_TASK and explicit package)
    if (!contentUrl.isNullOrEmpty()) {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contentUrl)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          addCategory(Intent.CATEGORY_BROWSABLE)
          if (!deepLinkPackage.isNullOrEmpty()) {
            setPackage(deepLinkPackage)
          }
        }
        if (intent.resolveActivity(context.packageManager) != null) {
          context.startActivity(intent)
          launched = true
        }
      } catch (_: Exception) {}
    }

    // 2. Try fallback search / title URL if primary deep link was unhandled
    if (!launched && !fallbackUrl.isNullOrEmpty()) {
      try {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          addCategory(Intent.CATEGORY_BROWSABLE)
          if (!deepLinkPackage.isNullOrEmpty()) {
            setPackage(deepLinkPackage)
          }
        }
        if (fallbackIntent.resolveActivity(context.packageManager) != null) {
          context.startActivity(fallbackIntent)
          launched = true
        }
      } catch (_: Exception) {}
    }

    // 3. Fall back to launching the streaming provider application
    if (!launched && !deepLinkPackage.isNullOrEmpty()) {
      val fallbackPkg = when (deepLinkPackage) {
        "com.amazon.firebat" -> "com.amazon.amazonvideo.livingroom"
        "com.amazon.firetv.youtube" -> "com.google.android.youtube.tv"
        else -> null
      }
      launched = launchPackageWithFallback(deepLinkPackage, fallbackPkg)
    }

    if (!launched) {
      Toast.makeText(context, "Opening ${item.title} on ${item.platform}", Toast.LENGTH_SHORT).show()
    }
  }

  val onSelectWhatsNewItem: (WhatsNewItem) -> Unit = { wn ->
    val (pkg, url) = when (wn.platform) {
      "Netflix" -> "com.netflix.ninja" to "https://www.netflix.com/title/${if (wn.title.contains("CTRL")) "81616434" else "81435689"}"
      "Prime Video" -> "com.amazon.firebat" to "amzn://avod/search?phrase=${Uri.encode(wn.title)}"
      "JioHotstar" -> "in.startv.hotstar" to "https://www.hotstar.com/in/explore?search_query=${Uri.encode(wn.title)}"
      else -> null to null
    }

    var launched = false
    if (!url.isNullOrEmpty() && !pkg.isNullOrEmpty()) {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          addCategory(Intent.CATEGORY_BROWSABLE)
          setPackage(pkg)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
          context.startActivity(intent)
          launched = true
        }
      } catch (_: Exception) {}
    }
    if (!launched && !pkg.isNullOrEmpty()) {
      launched = launchPackageWithFallback(pkg)
    }
    if (!launched) {
      Toast.makeText(context, "Opening ${wn.title} on ${wn.platform}", Toast.LENGTH_SHORT).show()
    }
  }

  val onSelectDockItem: (AppDockItem) -> Unit = { dockItem ->
    when (dockItem.actionType) {
      "ALL_APPS" -> overlayState = TvOverlayState.ALL_APPS
      "SEARCH" -> overlayState = TvOverlayState.SEARCH
      "SETTINGS" -> overlayState = TvOverlayState.SETTINGS
      "SCREEN_MIRROR" -> {
        var mirrorLaunched = false
        val mirrorIntents = listOf(
          Intent("com.amazon.device.settings.SYSTEM_MIRRORING"),
          Intent("android.settings.CAST_SETTINGS")
        )
        for (mIntent in mirrorIntents) {
          if (mIntent.resolveActivity(context.packageManager) != null) {
            try {
              mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              context.startActivity(mIntent)
              mirrorLaunched = true
              break
            } catch (_: Exception) {}
          }
        }
        if (!mirrorLaunched) {
          overlayState = TvOverlayState.SCREEN_MIRROR
        }
      }
      else -> {
        val fallbackPkg = when (dockItem.packageName) {
          "com.amazon.firebat" -> "com.amazon.amazonvideo.livingroom"
          "com.amazon.firetv.youtube" -> "com.google.android.youtube.tv"
          else -> null
        }
        val launched = launchPackageWithFallback(dockItem.packageName, fallbackPkg)
        if (!launched) {
          Toast.makeText(context, "Launching ${dockItem.name}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    // 1. Cinematic Alpine Mountain Background Layer
    Image(
      painter = painterResource(id = R.drawable.bg_cinematic_mountain),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    // 2. Atmospheric Scrim / Vignette Gradients for UI Contrast
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xCC050810),
              Color(0x66080C16),
              Color(0xAA060912),
              Color(0xF204060A)
            )
          )
        )
    )

    // 3. Main UI Layout
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 68.dp) // Leave clean clearance for bottom floating dock
    ) {
      // Top Bar: Brand left, Time/Date/Search/Settings right
      TvTopBar(
        onSearchClick = { overlayState = TvOverlayState.SEARCH },
        onSettingsClick = { overlayState = TvOverlayState.SETTINGS },
        searchFocusRequester = searchFocusRequester,
        settingsFocusRequester = settingsFocusRequester,
        downFocusRequester = kidsCardFocusRequester,
        rightRailFocusRequester = whatsNewFocusRequester
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Main Content Grid: 74% Left Pinned Rows | 26% Right Info Rail
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        // Left Column: Kids & Series rows
        Column(
          modifier = Modifier
            .weight(0.74f)
            .fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PinnedContentRow(
            category = Category.KIDS,
            items = kidsItems,
            onSelectItem = onSelectMedia,
            onFocusItem = { repository.setActiveContent(it) },
            onScrollMore = { overlayState = TvOverlayState.ALL_APPS },
            firstItemFocusRequester = kidsCardFocusRequester,
            upFocusRequester = searchFocusRequester,
            downFocusRequester = seriesCardFocusRequester,
            rightEdgeFocusRequester = whatsNewFocusRequester
          )

          PinnedContentRow(
            category = Category.SERIES,
            items = seriesItems,
            onSelectItem = onSelectMedia,
            onFocusItem = { repository.setActiveContent(it) },
            onScrollMore = { overlayState = TvOverlayState.ALL_APPS },
            firstItemFocusRequester = seriesCardFocusRequester,
            upFocusRequester = kidsCardFocusRequester,
            downFocusRequester = dockFocusRequester,
            rightEdgeFocusRequester = weatherFocusRequester
          )
        }

        // Right Column: What's New & Weather/Alert
        Column(
          modifier = Modifier
            .weight(0.26f)
            .padding(end = 28.dp, top = 6.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          WhatsNewPanel(
            items = whatsNew,
            onSelectItem = onSelectWhatsNewItem,
            firstItemFocusRequester = whatsNewFocusRequester,
            upFocusRequester = settingsFocusRequester,
            downFocusRequester = weatherFocusRequester,
            leftFocusRequester = kidsCardFocusRequester
          )
          WeatherAlertPanel(
            weather = weather,
            onWeatherClick = {
              Toast.makeText(context, "${weather.location}: ${weather.temperature}, ${weather.condition}", Toast.LENGTH_SHORT).show()
            },
            onAlertClick = {
              Toast.makeText(context, "${weather.alertTitle}: ${weather.alertTime}", Toast.LENGTH_SHORT).show()
            },
            firstItemFocusRequester = weatherFocusRequester,
            upFocusRequester = whatsNewFocusRequester,
            downFocusRequester = dockFocusRequester,
            leftFocusRequester = seriesCardFocusRequester
          )
        }
      }
    }

    // 4. Floating App Dock at bottom center (8 items exactly as reference, NO Power)
    FloatingAppDock(
      apps = dockItems,
      onSelectDockItem = onSelectDockItem,
      firstItemFocusRequester = dockFocusRequester,
      upFocusRequester = seriesCardFocusRequester,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 12.dp)
    )

    // 5. Overlays (Dismissible with Back button)
    when (overlayState) {
      TvOverlayState.ALL_APPS -> {
        AllAppsOverlay(
          onDismiss = { overlayState = TvOverlayState.NONE },
          onLaunchApp = { pkg ->
            val launched = launchPackageWithFallback(pkg)
            if (!launched) {
              Toast.makeText(context, "Opening $pkg", Toast.LENGTH_SHORT).show()
            }
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
          connectedDevice = if (connectedClients > 0) "FireLite Remote Connected" else null,
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
