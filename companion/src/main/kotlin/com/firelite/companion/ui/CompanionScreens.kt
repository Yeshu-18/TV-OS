package com.firelite.companion.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firelite.companion.R
import com.firelite.core.data.FireLiteRepository
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.core.model.RemoteCommand
import com.firelite.core.network.CompanionRemoteClient
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * REUSABLE FIRELITE TOP HEADER
 */
@Composable
fun FireLiteTopHeader(
  activeAccent: Color,
  onOpenMirror: () -> Unit = {},
  onPowerClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 4.dp, bottom = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "HOME TV",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 2.5.sp,
      color = activeAccent
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Screen Mirror icon-only control
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(FigmaTheme.SurfaceElevated)
          .border(1.dp, FigmaTheme.BorderSubtle, CircleShape)
          .clickable(onClick = onOpenMirror),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Tv,
          contentDescription = "Screen Mirror",
          tint = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.size(18.dp)
        )
      }

      // Power icon-only control (Soft gray/white, NOT bright red)
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(FigmaTheme.SurfaceElevated)
          .border(1.dp, FigmaTheme.BorderSubtle, CircleShape)
          .clickable(onClick = onPowerClick),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PowerSettingsNew,
          contentDescription = "Power",
          tint = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

private fun Drawable.toComposeBitmap(size: Int = 256): ImageBitmap? {
  return try {
    if (this is BitmapDrawable && bitmap != null) {
      bitmap.asImageBitmap()
    } else {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      setBounds(0, 0, size, size)
      draw(canvas)
      bmp.asImageBitmap()
    }
  } catch (_: Exception) {
    null
  }
}

/**
 * SCREEN 2: APPS SCREEN (APPS ONLY)
 * Layout: Two landscape rectangular (~16:9) app tiles per row.
 * Artwork-led: app artwork occupies 80-90% of tile area with subtle glass frame and bottom overlaid label.
 * Pinned Apps: JioHotstar, Netflix, Prime Video, YouTube, Amazon Music.
 * All Apps: Dynamically populated from PackageManager + streaming favorites.
 */
data class AppEntry(
  val name: String,
  val iconRes: Int = 0,
  val iconBitmap: ImageBitmap? = null,
  val packageName: String? = null
)

@Composable
fun PhoneAppsScreen(
  activeAccent: Color,
  onLaunch: (String) -> Unit,
  onOpenMirror: () -> Unit = {},
  onPowerClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val packageManager = remember { context.packageManager }

  // Pinned Apps: JioHotstar, Netflix, Prime Video, YouTube, Amazon Music
  val pinnedApps = remember {
    val candidates = listOf(
      Triple("JioHotstar", R.drawable.app_jiohotstar, listOf("in.startv.hotstar", "in.startv.hotstar.dplus")),
      Triple("Netflix", R.drawable.app_netflix, listOf("com.netflix.mediaclient", "com.netflix.ninja")),
      Triple("Prime Video", R.drawable.app_primevideo, listOf("com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom")),
      Triple("YouTube", R.drawable.app_youtube, listOf("com.google.android.youtube", "com.google.android.youtube.tv")),
      Triple("Amazon Music", R.drawable.app_amazonmusic, listOf("com.amazon.mp3"))
    )

    candidates.map { (name, fallbackRes, pkgs) ->
      var resolvedBitmap: ImageBitmap? = null
      var matchedPkg: String? = null
      for (pkg in pkgs) {
        try {
          val drawable = packageManager.getApplicationIcon(pkg)
          resolvedBitmap = drawable.toComposeBitmap(256)
          matchedPkg = pkg
          break
        } catch (_: Exception) {}
      }
      AppEntry(
        name = name,
        iconRes = fallbackRes,
        iconBitmap = resolvedBitmap,
        packageName = matchedPkg
      )
    }
  }

  // All Apps: dynamically populated from PackageManager, merged with streaming apps catalog
  val allApps = remember {
    val list = mutableListOf<AppEntry>()
    val seenPackages = mutableSetOf<String>()
    pinnedApps.mapNotNull { it.packageName }.forEach { seenPackages.add(it) }
    seenPackages.add(context.packageName)

    val catalog = listOf(
      Triple("Disney+ Hotstar", R.drawable.app_disney, listOf("in.startv.hotstar", "com.disney.disneyplus")),
      Triple("Apple TV", R.drawable.app_appletv, listOf("com.apple.atve.androidtv.appletv", "com.apple.atv")),
      Triple("MX Player", R.drawable.app_mxplayer, listOf("com.mxtech.videoplayer.ad", "com.mxtech.videoplayer.television")),
      Triple("SonyLIV", R.drawable.app_sonyliv, listOf("com.sonyliv")),
      Triple("ZEE5", R.drawable.app_zee5, listOf("com.graymatrix.did", "com.zee5.tv")),
      Triple("JioCinema", R.drawable.app_jiocinema, listOf("com.jio.media.ondemand")),
      Triple("Voot", R.drawable.app_voot, listOf("com.tv.v18.viola")),
      Triple("Hulu", R.drawable.app_hulu, listOf("com.hulu.plus"))
    )

    catalog.forEach { (name, fallbackRes, pkgs) ->
      var resolvedBitmap: ImageBitmap? = null
      var matchedPkg: String? = null
      for (pkg in pkgs) {
        try {
          val drawable = packageManager.getApplicationIcon(pkg)
          resolvedBitmap = drawable.toComposeBitmap(256)
          matchedPkg = pkg
          break
        } catch (_: Exception) {}
      }
      if (matchedPkg != null) seenPackages.add(matchedPkg)
      list.add(
        AppEntry(
          name = name,
          iconRes = fallbackRes,
          iconBitmap = resolvedBitmap,
          packageName = matchedPkg
        )
      )
    }

    try {
      val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val activities = packageManager.queryIntentActivities(mainIntent, 0)
      for (info in activities) {
        val pkg = info.activityInfo.packageName
        if (!seenPackages.contains(pkg)) {
          seenPackages.add(pkg)
          val label = info.loadLabel(packageManager).toString()
          val bmp = try {
            info.loadIcon(packageManager).toComposeBitmap(256)
          } catch (_: Exception) { null }
          list.add(
            AppEntry(
              name = label,
              iconRes = 0,
              iconBitmap = bmp,
              packageName = pkg
            )
          )
        }
      }
    } catch (_: Exception) {}

    list
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(FigmaTheme.Background)
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    // 1. Top Header
    FireLiteTopHeader(
      activeAccent = activeAccent,
      onOpenMirror = onOpenMirror,
      onPowerClick = onPowerClick
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      item {
        // Title & Subtitle
        Text(
          text = "Apps",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = FigmaTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Your TV Apps",
          fontSize = 13.sp,
          color = FigmaTheme.TextSecondary
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Pinned Apps Section Header
        Text(
          text = "Pinned Apps",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = FigmaTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Pinned Apps: Two wide landscape rectangular app tiles per row
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          pinnedApps.chunked(2).forEach { rowApps ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              rowApps.forEach { app ->
                Glass25DAppTile(
                  app = app,
                  activeAccent = activeAccent,
                  onClick = { onLaunch(app.name) },
                  modifier = Modifier.weight(1f)
                )
              }
              if (rowApps.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subtle gradient divider
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
              Brush.horizontalGradient(
                listOf(
                  Color.Transparent,
                  FigmaTheme.BorderSubtle.copy(alpha = 0.8f),
                  Color.Transparent
                )
              )
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // All Apps Section Header
        Text(
          text = "All Apps",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = FigmaTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // All Apps: Two wide landscape rectangular app tiles per row
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          allApps.chunked(2).forEach { rowApps ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              rowApps.forEach { app ->
                Glass25DAppTile(
                  app = app,
                  activeAccent = activeAccent,
                  onClick = { onLaunch(app.name) },
                  modifier = Modifier.weight(1f)
                )
              }
              if (rowApps.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

/**
 * GLASS + LANDSCAPE ARTWORK-LED APP TILE
 * Wide ~16:9 landscape card where the app artwork/icon occupies 80-90% of the tile area.
 * Features subtle translucent glass frame, tactile press animation (lifts slightly, artwork scales 1.02-1.04,
 * subtle dynamic accent glow behind artwork, soft shadow), and overlaid bottom app label with dark gradient scrim.
 */
@Composable
fun Glass25DAppTile(
  app: AppEntry,
  activeAccent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val tileScale by animateFloatAsState(
    targetValue = if (isPressed) 1.02f else 1f,
    animationSpec = tween(durationMillis = 140),
    label = "tileScale"
  )
  val tileLift by animateFloatAsState(
    targetValue = if (isPressed) -3f else 0f,
    animationSpec = tween(durationMillis = 140),
    label = "tileLift"
  )
  val artworkScale by animateFloatAsState(
    targetValue = if (isPressed) 1.04f else 1f,
    animationSpec = tween(durationMillis = 140),
    label = "artworkScale"
  )

  // 16:9-ish landscape card (aspect ratio 1.68)
  Box(
    modifier = modifier
      .aspectRatio(1.68f)
      .graphicsLayer {
        scaleX = tileScale
        scaleY = tileScale
        translationY = tileLift.dp.toPx()
      }
      .shadow(
        elevation = if (isPressed) 10.dp else 5.dp,
        shape = RoundedCornerShape(14.dp),
        spotColor = if (isPressed) activeAccent.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.55f)
      )
      .clip(RoundedCornerShape(14.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xE6101624),
            Color(0xD9090D15)
          )
        )
      )
      .border(
        width = 1.dp,
        color = if (isPressed) activeAccent.copy(alpha = 0.55f) else Color(0x1FFFFFFF),
        shape = RoundedCornerShape(14.dp)
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
  ) {
    // Subtle dynamic accent glow appears around/behind the artwork on press/focus
    if (isPressed) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(
                activeAccent.copy(alpha = 0.18f),
                Color.Transparent
              )
            )
          )
      )
    }

    // App artwork/icon occupying 80-90% of available tile height and central visual area
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp),
      contentAlignment = Alignment.Center
    ) {
      val artModifier = Modifier
        .fillMaxHeight(0.88f)
        .fillMaxWidth(0.88f)
        .graphicsLayer {
          scaleX = artworkScale
          scaleY = artworkScale
        }

      if (app.iconBitmap != null) {
        Image(
          bitmap = app.iconBitmap,
          contentDescription = app.name,
          modifier = artModifier.clip(RoundedCornerShape(12.dp)),
          contentScale = ContentScale.Fit
        )
      } else if (app.iconRes != 0) {
        Image(
          painter = painterResource(id = app.iconRes),
          contentDescription = app.name,
          modifier = artModifier,
          contentScale = ContentScale.Fit
        )
      }
    }

    // Subtle dark gradient scrim at the bottom for clean text readability
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(30.dp)
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              Color(0xBB060910),
              Color(0xEE060910)
            )
          )
        )
    )

    // Overlaid app name near the bottom of the tile
    Text(
      text = app.name,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.SemiBold,
      color = if (isPressed) activeAccent else FigmaTheme.TextPrimary.copy(alpha = 0.95f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 10.dp, bottom = 5.dp, end = 10.dp)
    )
  }
}

/**
 * SCREEN 3: DEDICATED PINS SCREEN
 * Contains all user explicitly pinned content (Kids, Series, Movies).
 * Artwork-led, subtle 2.5D depth language, and full pin management (add, remove, category filter).
 */
data class PinItemUi(
  val id: String,
  val title: String,
  val platform: String,
  val category: String,
  val imageRes: Int,
  val isCustom: Boolean = false
)

@Composable
fun PhonePinsScreen(
  repository: FireLiteRepository,
  client: CompanionRemoteClient,
  activeAccent: Color,
  onOpenMirror: () -> Unit = {},
  onPowerClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var selectedCategory by remember { mutableStateOf("All") }
  var showAddDialog by remember { mutableStateOf(false) }

  val kidsItems by repository.kidsItems.collectAsState()
  val seriesItems by repository.seriesItems.collectAsState()

  // Base curated pinned content with real artwork
  val defaultPins = listOf(
    PinItemUi("k1", "Doraemon", "JioHotstar", "Kids", R.drawable.pin_doraemon),
    PinItemUi("k2", "Shinchan", "JioHotstar", "Kids", R.drawable.pin_shinchan),
    PinItemUi("k3", "Peppa Pig", "Netflix", "Kids", R.drawable.pin_peppa),
    PinItemUi("k4", "Ninja Hattori", "JioHotstar", "Kids", R.drawable.pin_doraemon),
    PinItemUi("k5", "Motu Patlu", "JioCinema", "Kids", R.drawable.pin_shinchan),
    PinItemUi("s1", "Planet Earth", "Netflix", "Series", R.drawable.pin_earth),
    PinItemUi("s2", "LEGO City", "Prime Video", "Series", R.drawable.pin_lego),
    PinItemUi("s3", "Heart Beat", "JioHotstar", "Series", R.drawable.pin_barbie),
    PinItemUi("s4", "The Court", "Netflix", "Series", R.drawable.pin_potter),
    PinItemUi("s5", "Siragadikka Aasai", "JioHotstar", "Series", R.drawable.pin_earth),
    PinItemUi("m1", "Barbie", "Netflix", "Movies", R.drawable.pin_barbie),
    PinItemUi("m2", "Harry Potter", "JioHotstar", "Movies", R.drawable.pin_potter),
    PinItemUi("m3", "Spider-Man", "SonyLIV", "Movies", R.drawable.pin_spiderman)
  )

  // Merge repository custom items
  val customPins = remember(kidsItems, seriesItems) {
    val items = mutableListOf<PinItemUi>()
    kidsItems.filter { it.id !in listOf("k1", "k2", "k3", "k4", "k5") }.forEach {
      items.add(PinItemUi(it.id, it.title, it.platform, "Kids", R.drawable.pin_doraemon, isCustom = true))
    }
    seriesItems.filter { it.id !in listOf("s1", "s2", "s3", "s4", "s5") }.forEach {
      items.add(PinItemUi(it.id, it.title, it.platform, "Series", R.drawable.pin_potter, isCustom = true))
    }
    items
  }

  val allPins = remember(customPins) { defaultPins + customPins }

  val filteredPins = remember(selectedCategory, allPins) {
    if (selectedCategory == "All") allPins else allPins.filter { it.category == selectedCategory }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(FigmaTheme.Background)
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    // 1. Top Header
    FireLiteTopHeader(
      activeAccent = activeAccent,
      onOpenMirror = onOpenMirror,
      onPowerClick = onPowerClick
    )

    // 2. Title & Subtitle + Add Pin Action
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Pins",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = FigmaTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Your pinned shows & movies",
          fontSize = 13.sp,
          color = FigmaTheme.TextSecondary
        )
      }

      // + Add Pin Button
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(activeAccent.copy(alpha = 0.15f))
          .border(1.dp, activeAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
          .clickable { showAddDialog = true }
          .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Pin",
            tint = activeAccent,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Add Pin",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = activeAccent
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Category Selector Pills
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      listOf("All", "Kids", "Series", "Movies").forEach { cat ->
        val isSelected = selectedCategory == cat
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeAccent.copy(alpha = 0.18f) else FigmaTheme.SurfaceElevated)
            .border(
              width = 1.dp,
              color = if (isSelected) activeAccent.copy(alpha = 0.8f) else FigmaTheme.BorderSubtle,
              shape = RoundedCornerShape(20.dp)
            )
            .clickable { selectedCategory = cat }
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Text(
            text = cat,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) activeAccent else FigmaTheme.TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Two-Column Artwork Cards Grid
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      items(filteredPins.chunked(2)) { rowPins ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          rowPins.forEach { pin ->
            PinArtworkCard(
              pin = pin,
              activeAccent = activeAccent,
              onClick = {
                scope.launch {
                  client.sendCommand(RemoteCommand.SendText(pin.title))
                }
              },
              onDelete = if (pin.isCustom) {
                {
                  repository.removePinnedItem(pin.id)
                  scope.launch {
                    client.sendCommand(RemoteCommand.RemovePin(pin.id))
                  }
                }
              } else null,
              modifier = Modifier.weight(1f)
            )
          }
          if (rowPins.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddPinDialog(
      currentCategory = if (selectedCategory == "Series") Category.SERIES else Category.KIDS,
      activeAccent = activeAccent,
      onDismiss = { showAddDialog = false },
      onAdd = { newItem ->
        repository.addPinnedItem(newItem)
        scope.launch {
          client.sendCommand(RemoteCommand.AddPin(newItem))
        }
        showAddDialog = false
      }
    )
  }
}

@Composable
fun PinArtworkCard(
  pin: PinItemUi,
  activeAccent: Color,
  onClick: () -> Unit,
  onDelete: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "pinCardScale"
  )

  Column(
    modifier = modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(148.dp)
        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.6f))
        .clip(RoundedCornerShape(16.dp))
        .background(FigmaTheme.SurfaceContent)
        .border(
          width = 1.dp,
          color = if (isPressed) activeAccent.copy(alpha = 0.6f) else FigmaTheme.BorderSubtle,
          shape = RoundedCornerShape(16.dp)
        )
    ) {
      Image(
        painter = painterResource(id = pin.imageRes),
        contentDescription = pin.title,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )

      // Bottom subtle gradient overlay
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
            )
          )
      )

      // Delete icon for custom pins
      if (onDelete != null) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDelete),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Remove",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = pin.title,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = FigmaTheme.TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Text(
      text = "${pin.platform} · ${pin.category}",
      fontSize = 11.sp,
      color = FigmaTheme.TextSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * SCREEN 4: SEARCH SCREEN
 */
data class PosterItem(
  val title: String,
  val provider: String,
  val genre: String,
  val posterRes: Int
)

@Composable
fun PhoneSearchScreen(
  activeAccent: Color,
  currentApp: String?,
  onSendToTv: (String) -> Unit,
  onOpenMirror: () -> Unit = {},
  onPowerClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedProvider by remember { mutableStateOf("All Providers") }

  val providers = listOf(
    "All Providers",
    "JioHotstar",
    "Netflix",
    "Prime Video",
    "YouTube"
  )

  val trendingList = listOf(
    PosterItem("Doraemon", "JioHotstar", "Animation · Kids", R.drawable.poster_doraemon),
    PosterItem("Shinchan", "JioHotstar", "Comedy · Kids", R.drawable.poster_shinchan),
    PosterItem("Planet Earth", "Netflix", "Documentary · 4K", R.drawable.poster_earth),
    PosterItem("Barbie", "Netflix", "Animation", R.drawable.poster_barbie)
  )

  val popularList = listOf(
    PosterItem("LEGO City Adventures", "Prime Video", "Action · Kids", R.drawable.poster_lego),
    PosterItem("Miraculous", "Netflix", "Animation · Kids", R.drawable.poster_miraculous),
    PosterItem("Harry Potter", "JioHotstar", "Fantasy · Adventure", R.drawable.poster_potter),
    PosterItem("Spider-Man", "SonyLIV", "Action", R.drawable.poster_spiderman)
  )

  val allItems = trendingList + popularList
  val filteredItems = remember(searchQuery, selectedProvider) {
    allItems.filter { item ->
      val matchesQuery = searchQuery.isBlank() ||
        item.title.contains(searchQuery, ignoreCase = true) ||
        item.genre.contains(searchQuery, ignoreCase = true)
      val matchesProvider = selectedProvider == "All Providers" ||
        item.provider.equals(selectedProvider, ignoreCase = true)
      matchesQuery && matchesProvider
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(FigmaTheme.Background)
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    // 1. Top Header
    FireLiteTopHeader(
      activeAccent = activeAccent,
      onOpenMirror = onOpenMirror,
      onPowerClick = onPowerClick
    )

    // 2. Search Title & Subtitle
    Text(
      text = "Search",
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = FigmaTheme.TextPrimary
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "What do you want to watch?",
      fontSize = 13.sp,
      color = FigmaTheme.TextSecondary
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Search Bar with Mic icon
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.5f))
        .clip(RoundedCornerShape(14.dp))
        .background(FigmaTheme.SurfaceContent)
        .border(
          width = 1.dp,
          color = if (searchQuery.isNotEmpty()) activeAccent.copy(alpha = 0.65f) else FigmaTheme.BorderSubtle,
          shape = RoundedCornerShape(14.dp)
        )
        .padding(horizontal = 14.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = if (searchQuery.isNotEmpty()) activeAccent else FigmaTheme.TextSecondary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
          if (searchQuery.isEmpty()) {
            Text(
              text = "Search movies, shows & more",
              fontSize = 13.sp,
              color = FigmaTheme.TextSecondary
            )
          }
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(
              fontSize = 14.sp,
              color = FigmaTheme.TextPrimary,
              fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(activeAccent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
              if (searchQuery.isNotBlank()) onSendToTv(searchQuery)
            }),
            modifier = Modifier.fillMaxWidth()
          )
        }
        if (searchQuery.isNotEmpty()) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear",
            tint = FigmaTheme.TextSecondary,
            modifier = Modifier
              .size(18.dp)
              .clickable { searchQuery = "" }
          )
        } else {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Search",
            tint = FigmaTheme.TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. Provider Filter Pills
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(providers) { provider ->
        val isSelected = selectedProvider == provider
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeAccent.copy(alpha = 0.18f) else FigmaTheme.SurfaceElevated)
            .border(
              width = 1.dp,
              color = if (isSelected) activeAccent.copy(alpha = 0.8f) else FigmaTheme.BorderSubtle,
              shape = RoundedCornerShape(20.dp)
            )
            .clickable { selectedProvider = provider }
            .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
          Text(
            text = provider,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) activeAccent else FigmaTheme.TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (searchQuery.isBlank()) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        item {
          // Section 1: Trending for you
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Trending for you",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = FigmaTheme.TextPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              tint = FigmaTheme.TextSecondary,
              modifier = Modifier.size(12.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(trendingList) { item ->
              SearchPosterCard(
                item = item,
                onClick = { onSendToTv(item.title) }
              )
            }
          }
        }

        item {
          // Section 2: Popular on OTT
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Popular on OTT",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = FigmaTheme.TextPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              tint = FigmaTheme.TextSecondary,
              modifier = Modifier.size(12.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(popularList) { item ->
              SearchPosterCard(
                item = item,
                onClick = { onSendToTv(item.title) }
              )
            }
          }
        }
      }
    } else {
      Text(
        text = "RESULTS (${filteredItems.size})",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = FigmaTheme.TextSecondary
      )

      Spacer(modifier = Modifier.height(10.dp))

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(filteredItems) { item ->
          SearchFilteredResultCard(
            item = item,
            activeAccent = activeAccent,
            onPlayOnTv = { onSendToTv(item.title) }
          )
        }
      }
    }
  }
}

@Composable
fun SearchPosterCard(
  item: PosterItem,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1f,
    animationSpec = tween(durationMillis = 80),
    label = "posterScale"
  )

  Column(
    modifier = Modifier
      .width(112.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
  ) {
    Box(
      modifier = Modifier
        .size(width = 112.dp, height = 152.dp)
        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.6f))
        .clip(RoundedCornerShape(16.dp))
        .background(FigmaTheme.SurfaceContent)
        .border(1.dp, FigmaTheme.BorderSubtle, RoundedCornerShape(16.dp))
    ) {
      Image(
        painter = painterResource(id = item.posterRes),
        contentDescription = item.title,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = item.title,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = FigmaTheme.TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Text(
      text = item.provider,
      fontSize = 10.sp,
      color = FigmaTheme.TextSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Text(
      text = item.genre,
      fontSize = 9.sp,
      color = FigmaTheme.TextSecondary.copy(alpha = 0.75f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun SearchFilteredResultCard(
  item: PosterItem,
  activeAccent: Color,
  onPlayOnTv: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(72.dp)
      .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.4f))
      .clip(RoundedCornerShape(16.dp))
      .background(FigmaTheme.SurfaceContent)
      .border(1.dp, FigmaTheme.BorderSubtle, RoundedCornerShape(16.dp))
      .clickable(onClick = onPlayOnTv)
      .padding(horizontal = 12.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(width = 42.dp, height = 54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FigmaTheme.SurfaceElevated)
        ) {
          Image(
            painter = painterResource(id = item.posterRes),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FigmaTheme.TextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${item.provider} · ${item.genre}",
            fontSize = 11.sp,
            color = FigmaTheme.TextSecondary
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(activeAccent.copy(alpha = 0.12f))
          .border(1.dp, activeAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = activeAccent,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "Play",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = activeAccent
          )
        }
      }
    }
  }
}

/**
 * SCREEN 5: MORE SCREEN
 */
data class MoreSettingRow(
  val title: String,
  val subtitle: String,
  val icon: ImageVector
)

@Composable
fun PhoneMoreScreen(
  isConnected: Boolean,
  isPaired: Boolean,
  activeAccent: Color,
  onOpenPairDialog: () -> Unit,
  onOpenMirrorScreen: () -> Unit,
  onPowerClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val rows = listOf(
    MoreSettingRow(
      title = "TV Connection",
      subtitle = if (isConnected) "Living Room TV · Connected" else "Living Room TV · Disconnected",
      icon = Icons.Default.Tv
    ),
    MoreSettingRow(
      title = "Settings",
      subtitle = "App preferences",
      icon = Icons.Default.Settings
    ),
    MoreSettingRow(
      title = "Help & Support",
      subtitle = "Get help and tips",
      icon = Icons.Outlined.HelpOutline
    ),
    MoreSettingRow(
      title = "About Home Tv",
      subtitle = "Version 1.0.0",
      icon = Icons.Default.Info
    ),
    MoreSettingRow(
      title = "Send Feedback",
      subtitle = "Help us improve",
      icon = Icons.Default.Feedback
    )
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(FigmaTheme.Background)
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    // 1. Top Header
    FireLiteTopHeader(
      activeAccent = activeAccent,
      onOpenMirror = onOpenMirrorScreen,
      onPowerClick = onPowerClick
    )

    // 2. Title & Subtitle
    Text(
      text = "More",
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = FigmaTheme.TextPrimary
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "Settings & Information",
      fontSize = 13.sp,
      color = FigmaTheme.TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Settings Cards List (Glassy cards)
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(rows) { row ->
        MoreSettingCard(
          row = row,
          activeAccent = activeAccent,
          onClick = {
            when (row.title) {
              "TV Connection" -> onOpenPairDialog()
              else -> {}
            }
          }
        )
      }
    }
  }
}

@Composable
fun MoreSettingCard(
  row: MoreSettingRow,
  activeAccent: Color,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = tween(durationMillis = 80),
    label = "settingPressScale"
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(68.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.4f))
      .clip(RoundedCornerShape(16.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xEE101624),
            Color(0xCC090D15)
          )
        )
      )
      .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FigmaTheme.SurfaceElevated)
            .border(1.dp, FigmaTheme.BorderSubtle, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = row.icon,
            contentDescription = null,
            tint = if (row.title == "TV Connection") FigmaTheme.GreenDot else FigmaTheme.TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text(
            text = row.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = FigmaTheme.TextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = row.subtitle,
            fontSize = 12.sp,
            color = FigmaTheme.TextSecondary
          )
        }
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = "Open",
        tint = FigmaTheme.TextSecondary.copy(alpha = 0.6f),
        modifier = Modifier.size(13.dp)
      )
    }
  }
}

/**
 * SCREEN 6: SCREEN MIRROR
 */
@Composable
fun PhoneScreenMirrorScreen(
  isMirroring: Boolean,
  onToggleMirror: () -> Unit,
  activeAccent: Color = FigmaTheme.AccentBlue,
  onPowerClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(FigmaTheme.Background)
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    // 1. Top Header
    FireLiteTopHeader(
      activeAccent = activeAccent,
      onOpenMirror = {},
      onPowerClick = onPowerClick
    )

    // 2. Title & Subtitle
    Text(
      text = "Screen Mirror",
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = FigmaTheme.TextPrimary
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "Send your phone screen to the paired Home Tv",
      fontSize = 13.sp,
      color = FigmaTheme.TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Living Room TV Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(72.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xEE101624), Color(0xCC090D15))
          )
        )
        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FigmaTheme.SurfaceElevated)
            .border(1.dp, FigmaTheme.BorderSubtle, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = activeAccent,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text(
            text = "Living Room TV",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = FigmaTheme.TextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Ready for mirroring",
            fontSize = 12.sp,
            color = FigmaTheme.TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. Paired and Ready Status Pill
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(FigmaTheme.GreenBg)
        .border(1.dp, FigmaTheme.GreenBorder, RoundedCornerShape(14.dp))
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(FigmaTheme.GreenDot)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Paired and ready",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = FigmaTheme.GreenText
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. Start / Stop Screen Mirroring Button
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(if (isMirroring) FigmaTheme.SurfaceElevated else activeAccent.copy(alpha = 0.16f))
        .border(
          width = 1.dp,
          color = if (isMirroring) FigmaTheme.BorderSubtle else activeAccent.copy(alpha = 0.8f),
          shape = RoundedCornerShape(14.dp)
        )
        .clickable(onClick = onToggleMirror),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (isMirroring) "Stop Mirroring" else "Start Screen Mirroring",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (isMirroring) FigmaTheme.TextSecondary else activeAccent
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 6. Privacy Note Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xEE101624), Color(0xCC090D15))
          )
        )
        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
        .padding(18.dp)
    ) {
      Column {
        Text(
          text = "Private by default",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = FigmaTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Only paired phones can start or stop mirroring.\nThe TV displays a pairing code before first use.",
          fontSize = 12.sp,
          color = FigmaTheme.TextSecondary,
          lineHeight = 17.sp
        )
      }
    }
  }
}

/**
 * 4-Digit TV Pairing Dialog
 */
@Composable
fun PairingDialog(
  onDismiss: () -> Unit,
  onPairSuccess: () -> Unit,
  activeAccent: Color
) {
  var code by remember { mutableStateOf("8294") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Pairing",
          tint = activeAccent,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Pair with Home Tv", color = FigmaTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Enter the 4-digit code shown on your TV screen to pair this remote.",
          fontSize = 13.sp,
          color = FigmaTheme.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = code,
          onValueChange = { if (it.length <= 4) code = it },
          singleLine = true,
          textStyle = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center,
            color = FigmaTheme.TextPrimary
          ),
          modifier = Modifier.fillMaxWidth(0.6f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeAccent,
            unfocusedBorderColor = FigmaTheme.BorderSubtle,
            focusedTextColor = FigmaTheme.TextPrimary,
            unfocusedTextColor = FigmaTheme.TextPrimary
          )
        )

        if (error != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = error!!, color = Color(0xFFEF4444), fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (code.length == 4) {
            onPairSuccess()
          } else {
            error = "Please enter all 4 digits"
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = activeAccent)
      ) {
        Text("Pair Remote", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = FigmaTheme.TextSecondary)
      }
    },
    containerColor = FigmaTheme.SurfaceContent
  )
}

/**
 * Live Keyboard Input Dialog
 */
@Composable
fun LiveKeyboardDialog(
  activeAccent: Color,
  onDismiss: () -> Unit,
  onSend: (String) -> Unit
) {
  var text by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("TV Keyboard Input", color = FigmaTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Type text to beam to TV...", color = FigmaTheme.TextSecondary) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = activeAccent,
          unfocusedBorderColor = FigmaTheme.BorderSubtle,
          focusedTextColor = FigmaTheme.TextPrimary,
          unfocusedTextColor = FigmaTheme.TextPrimary
        )
      )
    },
    confirmButton = {
      Button(
        onClick = {
          if (text.isNotEmpty()) {
            onSend(text)
            onDismiss()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = activeAccent)
      ) {
        Text("Send", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = FigmaTheme.TextSecondary)
      }
    },
    containerColor = FigmaTheme.SurfaceContent
  )
}

/**
 * Add Pin Dialog
 */
@Composable
fun AddPinDialog(
  currentCategory: Category,
  activeAccent: Color,
  onDismiss: () -> Unit,
  onAdd: (MediaItem) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var platform by remember { mutableStateOf("JioHotstar") }
  var subtitle by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add New Pin", color = FigmaTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeAccent,
            unfocusedBorderColor = FigmaTheme.BorderSubtle,
            focusedTextColor = FigmaTheme.TextPrimary,
            unfocusedTextColor = FigmaTheme.TextPrimary
          )
        )
        OutlinedTextField(
          value = platform,
          onValueChange = { platform = it },
          label = { Text("Platform (JioHotstar, Netflix, etc.)") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeAccent,
            unfocusedBorderColor = FigmaTheme.BorderSubtle,
            focusedTextColor = FigmaTheme.TextPrimary,
            unfocusedTextColor = FigmaTheme.TextPrimary
          )
        )
        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          label = { Text("Genre / Description") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeAccent,
            unfocusedBorderColor = FigmaTheme.BorderSubtle,
            focusedTextColor = FigmaTheme.TextPrimary,
            unfocusedTextColor = FigmaTheme.TextPrimary
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotEmpty()) {
            val item = MediaItem(
              id = UUID.randomUUID().toString(),
              title = title.trim(),
              category = currentCategory,
              platform = platform.trim(),
              subtitle = subtitle.ifEmpty { "Pinned Content" },
              durationOrEpisodes = "New",
              contentUrl = null,
              accentColorHex = 0xFF00A3FF
            )
            onAdd(item)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = activeAccent)
      ) {
        Text("Save Pin", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = FigmaTheme.TextSecondary)
      }
    },
    containerColor = FigmaTheme.SurfaceContent
  )
}
