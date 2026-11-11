package com.firelite.tv.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.firelite.core.data.FireLiteRepository
import com.firelite.core.data.HotstarDeepLinks
import com.firelite.core.model.MediaItem
import com.firelite.tv.util.TvAppItem
import com.firelite.tv.util.TvAppManager
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

enum class TvOverlayState {
  NONE,
  SETTINGS,
  SCREEN_MIRROR
}

private enum class TvFocusRow {
  TOP_BAR,
  APPS,
  MY_FAVOURITES,
  MOOD_DECK
}

@OptIn(ExperimentalFoundationApi::class)
private val ZeroVerticalBringIntoViewSpec = object : BringIntoViewSpec {
  override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

@OptIn(ExperimentalFoundationApi::class)
private val TvHorizontalPivotBringIntoViewSpec = object : BringIntoViewSpec {
  private val parentFraction = 0.22f

  override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
    val targetForLeadingEdge = (parentFraction * containerSize).let { preferred ->
      val spaceAvailable = containerSize - preferred
      if (size <= containerSize && spaceAvailable < size) {
        containerSize - size
      } else {
        preferred
      }
    }
    return offset - targetForLeadingEdge
  }
}

@Composable
private fun TvSpotlightHeader(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 6.dp, bottom = 6.dp)
  ) {
    Text(
      text = title,
      fontSize = 20.sp,
      fontWeight = FontWeight.Black,
      color = Color.White,
      letterSpacing = 1.6.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = subtitle,
      fontSize = 12.sp,
      fontWeight = FontWeight.Normal,
      color = Color(0xBBFFFFFF),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FireLiteTvScreen(
  repository: FireLiteRepository,
  serverIp: String,
  connectedClients: Int,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  val favouriteSpecs = remember { buildDefaultFavouritesLineup() }
  val preloadedFavouritesAssets = rememberPreloadedFavouritesAssets(favouriteSpecs)

  var overlayState by remember { mutableStateOf(TvOverlayState.NONE) }
  var appList by remember { mutableStateOf(TvAppManager.getInstantFallbackApps()) }
  var moodDecks by remember {
    mutableStateOf(filterPlayableMoodDecks(installedPackages = detectInstalledStreamingPackages(context)))
  }

  var focusedRow by remember { mutableStateOf(TvFocusRow.APPS) }
  var focusedApp by remember { mutableStateOf<TvAppItem?>(appList.firstOrNull()) }
  var focusedFavourite by remember { mutableStateOf<FavouriteCharacterSpec?>(null) }

  var moodTopIndex by remember { mutableIntStateOf(0) }
  var moodStage by remember { mutableStateOf(MoodDeckStage.BROWSE) }
  var moodSpinToken by remember { mutableIntStateOf(0) }
  var moodPendingWinner by remember { mutableStateOf<MoodTitleSpec?>(null) }
  var moodLandedWinner by remember { mutableStateOf<MoodTitleSpec?>(null) }

  val safeMoodIndex = if (moodDecks.isEmpty()) 0 else moodTopIndex % moodDecks.size
  val activeMoodDeck = moodDecks.getOrNull(safeMoodIndex)
  val preloadedMoodAssets = rememberPreloadedMoodAssets(
    decks = moodDecks,
    topIndex = safeMoodIndex,
    winnerTitle = moodPendingWinner ?: moodLandedWinner
  )

  val appsScrollState = rememberScrollState()
  val verticalScrollState = rememberScrollState()
  var myFavouritesTopYPx by remember { mutableIntStateOf(0) }
  var moodDeckTopYPx by remember { mutableIntStateOf(0) }

  // Scroll vertically ONLY when changing rows (never on horizontal item movement).
  LaunchedEffect(focusedRow, myFavouritesTopYPx, moodDeckTopYPx, verticalScrollState.maxValue) {
    val maxScroll = verticalScrollState.maxValue
    if (maxScroll <= 0) return@LaunchedEffect
    val targetScroll = when (focusedRow) {
      TvFocusRow.TOP_BAR, TvFocusRow.APPS -> 0
      TvFocusRow.MY_FAVOURITES -> myFavouritesTopYPx.coerceIn(0, maxScroll)
      TvFocusRow.MOOD_DECK -> moodDeckTopYPx.coerceIn(0, maxScroll)
    }
    if (verticalScrollState.value != targetScroll) {
      verticalScrollState.animateScrollTo(targetScroll)
    }
  }

  // Per-item persistent FocusRequesters inside non-lazy Rows so horizontal scrolling
  // NEVER detaches or loses UP/DOWN D-pad focus targets across rows.
  val appRequesters = remember { HashMap<String, FocusRequester>() }
  fun requesterForApp(id: String): FocusRequester = appRequesters.getOrPut(id) { FocusRequester() }

  var lastFocusedAppId by remember { mutableStateOf<String?>(null) }
  var lastFocusedFrontFavId by remember {
    mutableStateOf(favouriteSpecs.firstOrNull { it.row == FavouriteRow.FRONT }?.id)
  }

  fun activeAppRequester(): FocusRequester {
    val targetId = lastFocusedAppId?.takeIf { id -> appList.any { it.id == id } }
      ?: appList.firstOrNull()?.id
    return targetId?.let { requesterForApp(it) } ?: FocusRequester.Default
  }

  val firstFavouriteFocusRequester = remember { FocusRequester() }
  val firstBackId = remember(favouriteSpecs) {
    favouriteSpecs.filter { it.row == FavouriteRow.BACK }.minByOrNull { it.rowOrder }?.id
  }
  val favouriteFocusRequesters = remember(favouriteSpecs, firstBackId) {
    favouriteSpecs.associate { item ->
      item.id to if (item.id == firstBackId) firstFavouriteFocusRequester else FocusRequester()
    }
  }
  val moodDeckFocusRequester = remember { FocusRequester() }
  val searchFocusRequester = remember { FocusRequester() }
  val settingsFocusRequester = remember { FocusRequester() }

  val lifecycleOwner = LocalLifecycleOwner.current

  // Refresh installed app filter whenever the launcher returns to foreground
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        moodDecks = filterPlayableMoodDecks(installedPackages = detectInstalledStreamingPackages(context))
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // 2. Asynchronously pre-warm & resolve all installed apps in parallel on background thread
  LaunchedEffect(Unit) {
    try {
      val resolved = TvAppManager.resolveInstalledAppsParallel(context)
      if (resolved.isNotEmpty()) {
        appList = resolved
        if (focusedApp == null && focusedRow == TvFocusRow.APPS) {
          focusedApp = resolved.firstOrNull()
        }
      }
    } catch (_: Exception) {}
  }

  // 3. Request initial D-pad focus on frame 0
  LaunchedEffect(Unit) {
    delay(50)
    try {
      activeAppRequester().requestFocus()
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

  // Couch-TV direct content deep link launcher with task-clearing flags
  fun launchContent(item: MediaItem) {
    val contentUrl = HotstarDeepLinks.normalizeUrl(item.contentUrl, preferWatch = false) ?: item.contentUrl
    val pkg = item.deepLinkPackage ?: (if (!contentUrl.isNullOrEmpty()) HotstarDeepLinks.packageForUrl(contentUrl) else null)
    var started = false

    if (!contentUrl.isNullOrEmpty()) {
      try {
        val intent = if (contentUrl.startsWith("intent:")) {
          Intent.parseUri(contentUrl, Intent.URI_INTENT_SCHEME)
        } else {
          Intent(Intent.ACTION_VIEW, Uri.parse(contentUrl))
        }
        if (!pkg.isNullOrEmpty()) {
          intent.setPackage(pkg)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        started = true
      } catch (_: Exception) {}
    }

    if (!started && !pkg.isNullOrEmpty()) {
      val pm = context.packageManager
      val fallback = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
      if (fallback != null) {
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(fallback)
        started = true
      }
    }

    if (!started) {
      Toast.makeText(context, "Opening ${item.title} on ${item.platform}", Toast.LENGTH_SHORT).show()
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

  val activeFavourite = if (focusedRow == TvFocusRow.MY_FAVOURITES) focusedFavourite else null
  val activeMood = if (focusedRow == TvFocusRow.MOOD_DECK) activeMoodDeck else null
  val activeMoodSceneTitle = resolveDeckBackdropTitle(
    decks = moodDecks,
    topIndex = safeMoodIndex,
    winner = moodLandedWinner
  )

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    // Full-screen wallpaper + darkened B&W scene crossfade while a favourite is selected
    FavouritesSceneBackdrop(
      activeFavourite = activeFavourite,
      preloadedAssets = preloadedFavouritesAssets,
      ambientColorProvider = {
        val fav = activeFavourite
        val app = focusedApp
        when {
          fav != null -> fav.glowColor
          activeMood != null -> activeMood.category.color
          app != null -> app.brandColor
          else -> Color(0xFF0F1F38)
        }
      }
    )

    // Heavily darkened, vignetted B&W 16:9 scene + soft mood glow behind the Mood Cards Deck
    MoodDeckSceneBackdrop(
      isVisible = focusedRow == TvFocusRow.MOOD_DECK,
      sceneTitle = activeMoodSceneTitle,
      moodColor = activeMoodDeck?.category?.color ?: Color(0xFF38BDF8),
      assets = preloadedMoodAssets
    )

    // Fixed Top Chrome + Stable Shelves Container
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 48.dp, end = 48.dp, top = 6.dp)
    ) {
      // Top Bar stays pinned at the top so vertical shelf navigation never scrolls headers off screen
      TvTopBar(
        onSearchClick = {},
        onSettingsClick = {
          if (!openTvSettings()) overlayState = TvOverlayState.SETTINGS
        },
        searchFocusRequester = searchFocusRequester,
        settingsFocusRequester = settingsFocusRequester,
        downFocusRequester = activeAppRequester(),
        rightRailFocusRequester = activeAppRequester(),
        modifier = Modifier.onFocusChanged { focusState ->
          if (focusState.hasFocus) {
            focusedRow = TvFocusRow.TOP_BAR
            focusedFavourite = null
          }
        }
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Single-line fixed-height spotlight header so text length changes never shift layout vertically
      val spotlightTitle = when {
        activeFavourite != null -> activeFavourite.showTitle.uppercase()
        activeMood != null ->
          moodLandedWinner?.title?.uppercase() ?: activeMood.category.name.uppercase()
        focusedApp != null -> focusedApp?.name?.uppercase() ?: "APPLICATIONS"
        else -> "APPLICATIONS"
      }
      val spotlightSubtitle = when {
        activeFavourite != null ->
          "${activeFavourite.characterName} • ${activeFavourite.subtitle} • Left/Right along row • Up/Down switch rows"
        activeMood != null -> {
          val landed = moodLandedWinner
          val deckSize = activeMood.titles.size
          val countText = if (deckSize == 1) "1 title" else "$deckSize titles"
          landed?.let { "${it.appName} • Picked from ${activeMood.category.name}" }
            ?: "$countText • Left: Skip mood • Right / OK: Spin reel"
        }
        focusedApp != null -> focusedApp?.tagline ?: ""
        else -> "Use D-Pad to browse • Press OK to open"
      }
      TvSpotlightHeader(
        title = spotlightTitle,
        subtitle = spotlightSubtitle
      )

      // Outer vertical scroll ignores child horizontal focus BringIntoView requests so rows never
      // bob up/down on Left/Right D-pad movement; nested LazyRows use smooth TV horizontal pivoting.
      CompositionLocalProvider(LocalBringIntoViewSpec provides ZeroVerticalBringIntoViewSpec) {
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(verticalScrollState)
        ) {
          CompositionLocalProvider(LocalBringIntoViewSpec provides TvHorizontalPivotBringIntoViewSpec) {
            // SECTION 1: APPLICATIONS
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
            ) {
              Text(
                text = "APPLICATIONS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.2.sp
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(appsScrollState)
                .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
              appList.forEach { app ->
                key(app.id) {
                  val tileModifier = Modifier
                    .focusRequester(requesterForApp(app.id))
                    .focusProperties {
                      up = settingsFocusRequester
                      down = firstFavouriteFocusRequester
                    }
                    .onFocusChanged { focusState ->
                      if (focusState.isFocused) {
                        focusedRow = TvFocusRow.APPS
                        focusedApp = app
                        lastFocusedAppId = app.id
                        focusedFavourite = null
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
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SECTION 2: MY FAVOURITES (Two-row interactive character lineup)
            MyFavouritesSection(
              specs = favouriteSpecs,
              selectedSpec = activeFavourite,
              preloadedAssets = preloadedFavouritesAssets,
              onFocusCharacter = { spec ->
                focusedRow = TvFocusRow.MY_FAVOURITES
                focusedFavourite = spec
                if (spec.row == FavouriteRow.FRONT) {
                  lastFocusedFrontFavId = spec.id
                }
                focusedApp = null
                repository.setActiveContent(spec.toMediaItem())
              },
              onSelectCharacter = { spec ->
                launchContent(spec.toMediaItem())
              },
              firstFocusRequester = firstFavouriteFocusRequester,
              upFocusRequesterProvider = { activeAppRequester() },
              downFocusRequesterProvider = {
                if (moodDecks.isNotEmpty()) moodDeckFocusRequester else FocusRequester.Cancel
              },
              externalFocusRequesters = favouriteFocusRequesters,
              modifier = Modifier.onGloballyPositioned { coordinates ->
                myFavouritesTopYPx = coordinates.positionInParent().y.roundToInt()
              }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 3: DON'T HAVE TIME TO BROWSE? (Mood Cards Deck + Slot Reel)
            MoodDeckSection(
              decks = moodDecks,
              topIndex = safeMoodIndex,
              stage = moodStage,
              spinToken = moodSpinToken,
              winner = if (moodStage == MoodDeckStage.REEL_SPINNING) moodPendingWinner else moodLandedWinner,
              isSectionFocused = focusedRow == TvFocusRow.MOOD_DECK,
              assets = preloadedMoodAssets,
              deckFocusRequester = moodDeckFocusRequester,
              upFocusRequesterProvider = {
                lastFocusedFrontFavId?.let { favouriteFocusRequesters[it] } ?: firstFavouriteFocusRequester
              },
              onFocusDeck = {
                focusedRow = TvFocusRow.MOOD_DECK
                focusedFavourite = null
                focusedApp = null
              },
              onSwipeLeft = {
                moodStage = MoodDeckStage.BROWSE
                moodPendingWinner = null
                moodLandedWinner = null
                moodTopIndex = advanceDeckIndex(safeMoodIndex, moodDecks.size)
              },
              onStartSpin = {
                val deck = activeMoodDeck ?: return@MoodDeckSection
                val picked = pickRandomSpinTitle(
                  titles = deck.titles,
                  excludeTitleId = moodLandedWinner?.id ?: moodPendingWinner?.id
                ) ?: return@MoodDeckSection
                moodPendingWinner = picked
                moodLandedWinner = null
                moodSpinToken += 1
                moodStage = MoodDeckStage.REEL_SPINNING
              },
              onReelLanded = { landed ->
                moodLandedWinner = landed
                moodStage = MoodDeckStage.COUNTDOWN
              },
              onCancelToDeck = {
                moodStage = MoodDeckStage.BROWSE
                moodPendingWinner = null
                moodLandedWinner = null
              },
              onOpenTitle = { titleSpec ->
                val accent = activeMoodDeck?.category?.colorHex ?: 0xFF38BDF8
                moodStage = MoodDeckStage.BROWSE
                moodPendingWinner = null
                moodLandedWinner = null
                launchContent(titleSpec.toMediaItem(accent))
              },
              modifier = Modifier.onGloballyPositioned { coordinates ->
                moodDeckTopYPx = coordinates.positionInParent().y.roundToInt()
              }
            )

            // Bottom scroll cushion so focusing My Favourites or Mood Deck scrolls cleanly into view
            Spacer(modifier = Modifier.height(160.dp))
          }
        }
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
