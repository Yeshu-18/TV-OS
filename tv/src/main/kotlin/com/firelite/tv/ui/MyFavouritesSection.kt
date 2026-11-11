package com.firelite.tv.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.firelite.core.data.HotstarDeepLinks
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.tv.R
import com.firelite.tv.util.AppIconLoader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

enum class FavouriteRow {
  BACK,
  FRONT
}

enum class CharacterTouchType {
  HEART_BEAT_PULSE,
  DORAEMON_TAKECOPTER,
  COURT_GAVEL_SLAM,
  SIRAGADIKKA_SHIMMER,
  SHINCHAN_COMIC_WIGGLE
}

data class FavouriteCharacterSpec(
  val id: String,
  val characterName: String,
  val showTitle: String,
  val streamingApp: String,
  val streamingAppResName: String,
  val subtitle: String,
  val category: Category,
  val contentId: String,
  val contentUrl: String,
  val deepLinkPackage: String,
  val cutoutResName: String,
  val sceneResName: String,
  val artworkResName: String,
  val glowColor: Color,
  val glowColorHex: Long,
  val row: FavouriteRow,
  val rowOrder: Int,
  val xCenterFraction: Float,
  val heightDp: Float,
  val aspectRatio: Float,
  val bottomOffsetDp: Float,
  val touchType: CharacterTouchType
) {
  fun toMediaItem(): MediaItem = MediaItem(
    id = id,
    title = showTitle,
    category = category,
    platform = streamingApp,
    subtitle = subtitle,
    accentColorHex = glowColorHex,
    contentId = contentId,
    contentUrl = contentUrl,
    deepLinkPackage = deepLinkPackage,
    artworkResName = artworkResName,
    characterResName = cutoutResName
  )
}

/**
 * Canonical 2-row lineup built from the user's 5 uploaded show assets.
 * Back row: Heart Beat (Dr. Reena), Doraemon, The Court (Advocate).
 * Front row: Siragadikka Aasai (Muthu & Meena), Shinchan & Shiro.
 */
fun buildDefaultFavouritesLineup(): List<FavouriteCharacterSpec> = listOf(
  FavouriteCharacterSpec(
    id = "heart_beat",
    characterName = "Dr. Reena",
    showTitle = "Heart Beat",
    streamingApp = "JioHotstar",
    streamingAppResName = "app_jiohotstar",
    subtitle = "Medical Drama • RK Hospitals",
    category = Category.SERIES,
    contentId = HotstarDeepLinks.HEART_BEAT_CONTENT_ID,
    contentUrl = HotstarDeepLinks.showDetailsUri("heart-beat", HotstarDeepLinks.HEART_BEAT_CONTENT_ID),
    deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
    cutoutResName = "fav_heart_beat",
    sceneResName = "scene_fav_heart_beat",
    artworkResName = "art_heart_beat",
    glowColor = Color(0xFFF43F5E),
    glowColorHex = 0xFFF43F5E,
    row = FavouriteRow.BACK,
    rowOrder = 0,
    xCenterFraction = 0.22f,
    heightDp = 224f,
    aspectRatio = 477f / 480f,
    bottomOffsetDp = 48f,
    touchType = CharacterTouchType.HEART_BEAT_PULSE
  ),
  FavouriteCharacterSpec(
    id = "doraemon",
    characterName = "Doraemon",
    showTitle = "Doraemon",
    streamingApp = "JioHotstar",
    streamingAppResName = "app_jiohotstar",
    subtitle = "Gadgets from the Future",
    category = Category.KIDS,
    contentId = HotstarDeepLinks.DORAEMON_CONTENT_ID,
    contentUrl = HotstarDeepLinks.showDetailsUri("doraemon", HotstarDeepLinks.DORAEMON_CONTENT_ID),
    deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
    cutoutResName = "fav_doraemon",
    sceneResName = "scene_fav_doraemon",
    artworkResName = "art_doraemon",
    glowColor = Color(0xFF00A3FF),
    glowColorHex = 0xFF00A3FF,
    row = FavouriteRow.BACK,
    rowOrder = 1,
    xCenterFraction = 0.51f,
    heightDp = 186f,
    aspectRatio = 473f / 532f,
    bottomOffsetDp = 98f,
    touchType = CharacterTouchType.DORAEMON_TAKECOPTER
  ),
  FavouriteCharacterSpec(
    id = "the_court",
    characterName = "Advocate",
    showTitle = "The Court",
    streamingApp = "JioHotstar",
    streamingAppResName = "app_jiohotstar",
    subtitle = "Legal Drama • JioHotstar Specials",
    category = Category.SERIES,
    contentId = HotstarDeepLinks.THE_COURT_CONTENT_ID,
    contentUrl = HotstarDeepLinks.showDetailsUri("the-court", HotstarDeepLinks.THE_COURT_CONTENT_ID),
    deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
    cutoutResName = "fav_the_court",
    sceneResName = "scene_fav_the_court",
    artworkResName = "art_the_court",
    glowColor = Color(0xFFEAB308),
    glowColorHex = 0xFFEAB308,
    row = FavouriteRow.BACK,
    rowOrder = 2,
    xCenterFraction = 0.77f,
    heightDp = 286f,
    aspectRatio = 267f / 550f,
    bottomOffsetDp = 12f,
    touchType = CharacterTouchType.COURT_GAVEL_SLAM
  ),
  FavouriteCharacterSpec(
    id = "siragadikka_aasai",
    characterName = "Muthu & Meena",
    showTitle = "Siragadikka Aasai",
    streamingApp = "JioHotstar",
    streamingAppResName = "app_jiohotstar",
    subtitle = "Family Drama",
    category = Category.SERIES,
    contentId = HotstarDeepLinks.SIRAGADIKKA_AASAI_CONTENT_ID,
    contentUrl = HotstarDeepLinks.showDetailsUri("siragadikka-aasai", HotstarDeepLinks.SIRAGADIKKA_AASAI_CONTENT_ID),
    deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
    cutoutResName = "fav_siragadikka",
    sceneResName = "scene_fav_siragadikka",
    artworkResName = "art_siragadikka",
    glowColor = Color(0xFFF59E0B),
    glowColorHex = 0xFFF59E0B,
    row = FavouriteRow.FRONT,
    rowOrder = 0,
    xCenterFraction = 0.375f,
    heightDp = 176f,
    aspectRatio = 718f / 609f,
    bottomOffsetDp = 12f,
    touchType = CharacterTouchType.SIRAGADIKKA_SHIMMER
  ),
  FavouriteCharacterSpec(
    id = "shinchan",
    characterName = "Shinchan & Shiro",
    showTitle = "Shinchan",
    streamingApp = "JioHotstar",
    streamingAppResName = "app_jiohotstar",
    subtitle = "Nohara Family Adventures",
    category = Category.KIDS,
    contentId = HotstarDeepLinks.SHINCHAN_CONTENT_ID,
    contentUrl = HotstarDeepLinks.showDetailsUri("shin-chan", HotstarDeepLinks.SHINCHAN_CONTENT_ID),
    deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
    cutoutResName = "fav_shinchan",
    sceneResName = "scene_fav_shinchan",
    artworkResName = "art_shinchan",
    glowColor = Color(0xFFEF4444),
    glowColorHex = 0xFFEF4444,
    row = FavouriteRow.FRONT,
    rowOrder = 1,
    xCenterFraction = 0.635f,
    heightDp = 182f,
    aspectRatio = 380f / 552f,
    bottomOffsetDp = 12f,
    touchType = CharacterTouchType.SHINCHAN_COMIC_WIGGLE
  )
)

/**
 * Pure z-index invariant for the two-row lineup:
 * - Back-row characters grow above unselected back peers (`2f > 1f`) while staying behind front row (`< 10f`).
 * - Front-row characters step in front of everyone when selected (`20f > 10f > 2f`).
 */
internal fun resolveCharacterZIndex(row: FavouriteRow, isSelected: Boolean): Float = when (row) {
  FavouriteRow.BACK -> if (isSelected) 2f else 1f
  FavouriteRow.FRONT -> if (isSelected) 20f else 10f
}

/**
 * Resolves explicit D-pad navigation neighbors so Left/Right stays strictly within a row,
 * and Up/Down transitions reliably between Back Row and Front Row.
 */
internal data class FavouritesNavNeighbors(
  val leftId: String?,
  val rightId: String?,
  val upId: String?,
  val downId: String?
)

internal fun computeFavouritesNeighbors(
  item: FavouriteCharacterSpec,
  backRow: List<FavouriteCharacterSpec>,
  frontRow: List<FavouriteCharacterSpec>
): FavouritesNavNeighbors {
  val sameRow = if (item.row == FavouriteRow.BACK) backRow else frontRow
  val idx = sameRow.indexOfFirst { it.id == item.id }
  val leftId = if (idx > 0) sameRow[idx - 1].id else null
  val rightId = if (idx in 0 until sameRow.lastIndex) sameRow[idx + 1].id else null

  val nearestInOpposite: (List<FavouriteCharacterSpec>) -> String? = { opp ->
    opp.minByOrNull { kotlin.math.abs(it.xCenterFraction - item.xCenterFraction) }?.id
  }

  return when (item.row) {
    FavouriteRow.BACK -> FavouritesNavNeighbors(
      leftId = leftId,
      rightId = rightId,
      upId = null, // Caller wires Up to Recently Watched / Apps
      downId = nearestInOpposite(frontRow)
    )
    FavouriteRow.FRONT -> FavouritesNavNeighbors(
      leftId = leftId,
      rightId = rightId,
      upId = nearestInOpposite(backRow),
      downId = null // Bottom of screen
    )
  }
}

private val BW_DARK_SCENE_FILTER = ColorFilter.colorMatrix(
  ColorMatrix().apply {
    setToSaturation(0f)
  }
)

private val FAV_RES_MAP: Map<String, Int> = mapOf(
  "fav_heart_beat" to R.drawable.fav_heart_beat,
  "fav_doraemon" to R.drawable.fav_doraemon,
  "fav_the_court" to R.drawable.fav_the_court,
  "fav_siragadikka" to R.drawable.fav_siragadikka,
  "fav_shinchan" to R.drawable.fav_shinchan,
  "scene_fav_heart_beat" to R.drawable.scene_fav_heart_beat,
  "scene_fav_doraemon" to R.drawable.scene_fav_doraemon,
  "scene_fav_the_court" to R.drawable.scene_fav_the_court,
  "scene_fav_siragadikka" to R.drawable.scene_fav_siragadikka,
  "scene_fav_shinchan" to R.drawable.scene_fav_shinchan,
  "bg_cinematic_mountain" to R.drawable.bg_cinematic_mountain,
  "app_jiohotstar" to R.drawable.app_jiohotstar
)

data class PreloadedFavouritesAssets(
  val scenes: Map<String, ImageBitmap> = emptyMap(),
  val cutouts: Map<String, ImageBitmap> = emptyMap(),
  val defaultWallpaper: ImageBitmap? = null
)

/**
 * Pre-decodes wallpaper, character cutouts, and 16:9 scene backdrops off the UI thread in stages
 * so cold start renders frame 0 without main-thread or memory stalls, while moving across characters
 * triggers instant 0ms-stall scene crossfades.
 */
@Composable
fun rememberPreloadedFavouritesAssets(
  specs: List<FavouriteCharacterSpec>
): PreloadedFavouritesAssets {
  val context = LocalContext.current
  var assets by remember { mutableStateOf(PreloadedFavouritesAssets()) }

  LaunchedEffect(specs) {
    // Stage 1: Wallpaper + character cutouts on IO so initial home stage appears cleanly.
    val initialAssets = withContext(Dispatchers.IO) {
      val wallpaper = FAV_RES_MAP["bg_cinematic_mountain"]?.let {
        decodeScaledResource(context, it, config = android.graphics.Bitmap.Config.RGB_565)
      }
      val cutouts = mutableMapOf<String, ImageBitmap>()
      for (spec in specs) {
        FAV_RES_MAP[spec.cutoutResName]?.let { resId ->
          decodeScaledResource(context, resId, config = android.graphics.Bitmap.Config.ARGB_8888)?.let {
            cutouts[spec.cutoutResName] = it
          }
        }
        yield()
      }
      PreloadedFavouritesAssets(
        cutouts = cutouts,
        defaultWallpaper = wallpaper
      )
    }
    assets = initialAssets

    // Let initial Activity launch & focus settle before decoding opaque 16:9 scene backdrops.
    delay(120)

    // Stage 2: Preload all darkened scene backdrops using RGB_565 (~50% lower bitmap heap).
    val scenes = withContext(Dispatchers.IO) {
      val decoded = mutableMapOf<String, ImageBitmap>()
      for (spec in specs) {
        FAV_RES_MAP[spec.sceneResName]?.let { resId ->
          decodeScaledResource(context, resId, config = android.graphics.Bitmap.Config.RGB_565)?.let {
            decoded[spec.sceneResName] = it
          }
        }
        yield()
      }
      decoded
    }
    assets = initialAssets.copy(scenes = scenes)
  }
  return assets
}

private fun decodeScaledResource(
  context: Context,
  resId: Int,
  sampleSize: Int = 1,
  config: android.graphics.Bitmap.Config = android.graphics.Bitmap.Config.ARGB_8888
): ImageBitmap? {
  return try {
    val opts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = config
    }
    BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
  } catch (_: Throwable) {
    null
  }
}

/**
 * Full-screen wallpaper + instant darkened B&W scene crossfade layer.
 * When `activeFavourite` is non-null, crossfades smoothly into that show's preloaded darkened
 * monochrome 16:9 scene; moving away from My Favourites restores the default TV wallpaper.
 */
@Composable
fun FavouritesSceneBackdrop(
  activeFavourite: FavouriteCharacterSpec?,
  preloadedAssets: PreloadedFavouritesAssets,
  ambientColorProvider: () -> Color,
  modifier: Modifier = Modifier
) {
  val targetColor = ambientColorProvider()
  val ambientAnim = remember { Animatable(targetColor) }
  LaunchedEffect(targetColor) {
    ambientAnim.animateTo(targetColor, animationSpec = tween(durationMillis = 180))
  }

  val glowMask = remember { createLineupRadialGlowBitmap() }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF040608))
  ) {
    // 1. Crossfade between default mountain wallpaper (null) and active darkened B&W show scene
    AnimatedContent(
      targetState = activeFavourite?.sceneResName,
      transitionSpec = {
        fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
          fadeOut(animationSpec = tween(durationMillis = 220))
      },
      label = "sceneWallpaperCrossfade",
      modifier = Modifier.fillMaxSize()
    ) { sceneName ->
      val sceneBitmap = if (sceneName != null) {
        preloadedAssets.scenes[sceneName]
      } else {
        preloadedAssets.defaultWallpaper
      }

      if (sceneBitmap != null) {
        Image(
          bitmap = sceneBitmap,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          colorFilter = if (sceneName != null) BW_DARK_SCENE_FILTER else null,
          alpha = if (sceneName != null) 0.42f else 0.24f,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    // 2. Ambient color wash + vertical dark scrim for TV UI readability and edge grounding
    Spacer(
      modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
          val edgeScrim = Brush.verticalGradient(
            0.0f to Color(0x88040608),
            0.35f to Color(0x38040608),
            0.72f to Color(0x99040608),
            1.0f to Color(0xFA040608)
          )
          val radialDst = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
          onDrawBehind {
            drawImage(
              image = glowMask,
              dstSize = radialDst,
              colorFilter = ColorFilter.tint(ambientAnim.value.copy(alpha = 0.48f))
            )
            drawRect(brush = edgeScrim)
          }
        }
    )
  }
}

private fun createLineupRadialGlowBitmap(): ImageBitmap {
  val width = 128
  val height = 72
  val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
  val canvas = android.graphics.Canvas(bitmap)
  val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    shader = android.graphics.RadialGradient(
      width * 0.50f,
      height * 0.45f,
      width * 0.68f,
      intArrayOf(0xB8FFFFFF.toInt(), 0x44FFFFFF, 0x00FFFFFF),
      floatArrayOf(0f, 0.45f, 1f),
      android.graphics.Shader.TileMode.CLAMP
    )
  }
  canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
  return bitmap.asImageBitmap()
}

/**
 * Two-row interactive "My Favourites" character lineup stage:
 * - Characters render in black-and-white until selected, turning to full colour + growing slightly.
 * - Soft glowing colour halo shines directly behind the group/selected character.
 * - Title + streaming app logo render directly above the ensemble when selected.
 * - Characters' bottom legs/torsos dissolve seamlessly into the black bottom scrim.
 * - Plays each character's bespoke one-shot extra layer & signature touch once per selection.
 */
@Composable
fun MyFavouritesSection(
  specs: List<FavouriteCharacterSpec>,
  selectedSpec: FavouriteCharacterSpec?,
  preloadedAssets: PreloadedFavouritesAssets,
  onFocusCharacter: (FavouriteCharacterSpec) -> Unit,
  onSelectCharacter: (FavouriteCharacterSpec) -> Unit,
  firstFocusRequester: FocusRequester,
  upFocusRequesterProvider: () -> FocusRequester,
  downFocusRequesterProvider: () -> FocusRequester = { FocusRequester.Cancel },
  externalFocusRequesters: Map<String, FocusRequester> = emptyMap(),
  modifier: Modifier = Modifier
) {
  val backRow = remember(specs) {
    specs.filter { it.row == FavouriteRow.BACK }.sortedBy { it.rowOrder }
  }
  val frontRow = remember(specs) {
    specs.filter { it.row == FavouriteRow.FRONT }.sortedBy { it.rowOrder }
  }
  val orderedSpecs = remember(specs) { backRow + frontRow }
  val firstBackId = backRow.firstOrNull()?.id

  val focusRequesters = remember(orderedSpecs, externalFocusRequesters) {
    orderedSpecs.associate { item ->
      item.id to (externalFocusRequesters[item.id] ?: if (item.id == firstBackId) firstFocusRequester else FocusRequester())
    }
  }

  // Track last focused back-row character so pressing Up from Front Row restores cleanly
  var lastBackFocusedId by remember { mutableStateOf(firstBackId) }
  LaunchedEffect(selectedSpec?.id) {
    if (selectedSpec?.row == FavouriteRow.BACK) {
      lastBackFocusedId = selectedSpec.id
    }
  }

  // Animated soft glow color & horizontal spotlight center behind the group
  val targetGlowColor = selectedSpec?.glowColor ?: Color.Transparent
  val glowColorAnim = remember { Animatable(targetGlowColor) }
  LaunchedEffect(targetGlowColor) {
    glowColorAnim.animateTo(targetGlowColor, animationSpec = tween(durationMillis = 220))
  }
  val glowCenterFraction by animateFloatAsState(
    targetValue = selectedSpec?.xCenterFraction ?: 0.50f,
    animationSpec = spring(dampingRatio = 0.85f, stiffness = 360f),
    label = "favGlowCenter"
  )
  val glowAlpha by animateFloatAsState(
    targetValue = if (selectedSpec != null) 0.94f else 0f,
    animationSpec = tween(durationMillis = 200),
    label = "favGlowAlpha"
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Header above the group: Streaming App Logo + Show Title + My Favourites indicator
    FavouritesGroupHeader(
      selectedSpec = selectedSpec,
      totalCount = specs.size,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp)
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 2. Centered Two-Row Overlapping Character Stage
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .height(326.dp),
      contentAlignment = Alignment.BottomCenter
    ) {
      val stageWidth = if (maxWidth < 740.dp) maxWidth else 740.dp
      val density = LocalDensity.current

      Box(
        modifier = Modifier
          .width(stageWidth)
          .height(326.dp)
      ) {
        // Behind the group (zIndex 0): Soft radial light in the selected character's own main colour
        val glowColor = glowColorAnim.value
        val activeAlpha = glowAlpha
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .zIndex(0f)
        ) {
          if (activeAlpha > 0.01f && glowColor.alpha > 0.01f) {
            val centerX = size.width * glowCenterFraction
            val centerY = size.height * 0.48f
            val radius = size.width * 0.48f
            drawCircle(
              brush = Brush.radialGradient(
                0.0f to glowColor.copy(alpha = 0.70f * activeAlpha),
                0.38f to glowColor.copy(alpha = 0.34f * activeAlpha),
                0.72f to glowColor.copy(alpha = 0.10f * activeAlpha),
                1.0f to Color.Transparent,
                center = Offset(centerX, centerY),
                radius = radius
              ),
              center = Offset(centerX, centerY),
              radius = radius
            )
          }
        }

        // Character layers rendered with true DstIn vertical alpha fade so lower legs melt
        // transparently into the scene/wallpaper with zero hard black rectangular edge.
        Box(
          modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
              drawContent()
              drawRect(
                brush = Brush.verticalGradient(
                  0.0f to Color.Black,
                  0.70f to Color.Black,
                  0.86f to Color(0x66000000),
                  0.94f to Color.Transparent,
                  1.0f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
              )
            }
        ) {
          orderedSpecs.forEach { spec ->
            val isSelected = selectedSpec?.id == spec.id
            val neighbors = remember(spec, backRow, frontRow) {
              computeFavouritesNeighbors(spec, backRow, frontRow)
            }

            val charWidthDp = (spec.heightDp * spec.aspectRatio).dp
            val charHeightDp = spec.heightDp.dp
            val centerXPx = with(density) { (stageWidth * spec.xCenterFraction - charWidthDp / 2f).roundToPx() }
            val yPosOffsetDp = (-spec.bottomOffsetDp).dp

            val focusModifier = Modifier
              .focusRequester(focusRequesters[spec.id] ?: FocusRequester())
              .focusProperties {
                left = neighbors.leftId?.let { focusRequesters[it] } ?: FocusRequester.Cancel
                right = neighbors.rightId?.let { focusRequesters[it] } ?: FocusRequester.Cancel
                up = when (spec.row) {
                  FavouriteRow.BACK -> upFocusRequesterProvider()
                  FavouriteRow.FRONT -> {
                    // Prefer nearest back-row character, or restore last visited back character if adjacent
                    val preferredId = if (lastBackFocusedId == "doraemon") "doraemon" else neighbors.upId
                    preferredId?.let { focusRequesters[it] } ?: FocusRequester.Default
                  }
                }
                down = neighbors.downId?.let { focusRequesters[it] } ?: downFocusRequesterProvider()
              }

            FavouriteCharacterLayer(
              spec = spec,
              bitmap = preloadedAssets.cutouts[spec.cutoutResName],
              isSelected = isSelected,
              onFocused = { onFocusCharacter(spec) },
              onActivated = { onSelectCharacter(spec) },
              focusModifier = focusModifier,
              modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(x = centerXPx, y = yPosOffsetDp.roundToPx()) }
                .size(width = charWidthDp, height = charHeightDp)
                .zIndex(resolveCharacterZIndex(spec.row, isSelected))
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FavouritesGroupHeader(
  selectedSpec: FavouriteCharacterSpec?,
  totalCount: Int,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val appPkg = selectedSpec?.deepLinkPackage
  val dynamicAppIcon = remember(appPkg) {
    appPkg?.let { AppIconLoader.getAppIcon(context, it) }
  }
  val fallbackAppIconRes = selectedSpec?.streamingAppResName?.let { FAV_RES_MAP[it] }

  Row(
    modifier = modifier.height(64.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "MY FAVOURITES",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = 1.4.sp
      )
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0x2EFFFFFF))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = "$totalCount CHARACTERS",
          fontSize = 9.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xD9E2E8F0)
        )
      }
    }

    // Above the group: Streaming App Logo (un-cropped full banner/icon) + Show Title
    AnimatedContent(
      targetState = selectedSpec,
      transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
      label = "favTitleAppHeader"
    ) { active ->
      if (active != null) {
        val badgeWidthDp = if (dynamicAppIcon != null && dynamicAppIcon.height > 0) {
          val ratio = dynamicAppIcon.width.toFloat() / dynamicAppIcon.height.toFloat()
          (30f * ratio).coerceIn(30f, 64f).dp
        } else {
          30.dp
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x880B1120))
            .border(1.dp, active.glowColor.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          // Streaming App Logo rendered cleanly at its natural aspect ratio (never cropped)
          Box(
            modifier = Modifier
              .size(width = badgeWidthDp, height = 30.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0B1120)),
            contentAlignment = Alignment.Center
          ) {
            if (dynamicAppIcon != null) {
              Image(
                bitmap = dynamicAppIcon,
                contentDescription = active.streamingApp,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
              )
            } else if (fallbackAppIconRes != null) {
              Image(
                painter = painterResource(id = fallbackAppIconRes),
                contentDescription = active.streamingApp,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(24.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = active.showTitle.uppercase(),
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.1.sp,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(active.glowColor)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "${active.characterName} • ${active.streamingApp} • OK to watch",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xE6FFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      } else {
        Text(
          text = "Move down to explore your cast",
          fontSize = 11.sp,
          color = Color(0x99CBD5E1),
          modifier = Modifier.padding(end = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun FavouriteCharacterLayer(
  spec: FavouriteCharacterSpec,
  bitmap: ImageBitmap?,
  isSelected: Boolean,
  onFocused: () -> Unit,
  onActivated: () -> Unit,
  focusModifier: Modifier,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  LaunchedEffect(isFocused) {
    if (isFocused) {
      onFocused()
    }
  }

  // Smooth B&W (0f) <-> Full Colour (1f) transition
  val saturation by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = tween(durationMillis = 200),
    label = "charSaturation_${spec.id}"
  )

  // Slight scale step forward on selection
  val focusScale by animateFloatAsState(
    targetValue = if (isSelected) 1.09f else 1.0f,
    animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
    label = "charScale_${spec.id}"
  )

  // Per-selection one-shot signature animation state (NEVER loops!)
  var selectionTriggerCount by remember { mutableIntStateOf(0) }
  var comicSeed by remember { mutableIntStateOf(17) }
  val touchTranslateY = remember { Animatable(0f) }
  val touchRotation = remember { Animatable(0f) }
  val touchScaleBoost = remember { Animatable(0f) }
  val extraLayerProgress = remember { Animatable(0f) }

  LaunchedEffect(isSelected) {
    if (isSelected) {
      selectionTriggerCount += 1
      comicSeed = Random.nextInt(1, 100_000)
      touchTranslateY.snapTo(0f)
      touchRotation.snapTo(0f)
      touchScaleBoost.snapTo(0f)
      extraLayerProgress.snapTo(0f)

      when (spec.touchType) {
        CharacterTouchType.HEART_BEAT_PULSE -> {
          // One-shot two-beat cardiac "lub-dub" scale pulse + 900ms ECG sweep
          coroutineScope {
            launch {
              extraLayerProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
            }
            launch {
              touchScaleBoost.animateTo(0.055f, animationSpec = tween(130))
              touchScaleBoost.animateTo(0.005f, animationSpec = tween(120))
              touchScaleBoost.animateTo(0.072f, animationSpec = tween(140))
              touchScaleBoost.animateTo(0f, animationSpec = tween(320))
            }
          }
        }
        CharacterTouchType.DORAEMON_TAKECOPTER -> {
          // One-shot Take-copter upward levitation + rotor blur & 4D pocket sparkles (900ms)
          coroutineScope {
            launch {
              extraLayerProgress.animateTo(1f, animationSpec = tween(900, easing = LinearEasing))
            }
            launch {
              touchTranslateY.animateTo(-28f, animationSpec = tween(380, easing = FastOutSlowInEasing))
              touchTranslateY.animateTo(-8f, animationSpec = tween(420, easing = FastOutSlowInEasing))
            }
          }
        }
        CharacterTouchType.COURT_GAVEL_SLAM -> {
          // One-shot decisive vertical gavel thud + single courtroom shockwave ring (480ms)
          coroutineScope {
            launch {
              extraLayerProgress.animateTo(1f, animationSpec = tween(480, easing = FastOutSlowInEasing))
            }
            launch {
              touchTranslateY.animateTo(18f, animationSpec = tween(110))
              touchTranslateY.animateTo(
                0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f)
              )
            }
          }
        }
        CharacterTouchType.SIRAGADIKKA_SHIMMER -> {
          // One-shot joyful snap tilt + golden marigold shimmer rays (700ms)
          coroutineScope {
            launch {
              extraLayerProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
            launch {
              touchRotation.animateTo(-3.0f, animationSpec = tween(130))
              touchRotation.animateTo(2.4f, animationSpec = tween(160))
              touchRotation.animateTo(0f, animationSpec = tween(240))
            }
          }
        }
        CharacterTouchType.SHINCHAN_COMIC_WIGGLE -> {
          // One-shot cheeky hop-wiggle + randomized Kasukabe comic burst lines (720ms)
          coroutineScope {
            launch {
              extraLayerProgress.animateTo(1f, animationSpec = tween(720, easing = FastOutSlowInEasing))
            }
            launch {
              touchTranslateY.animateTo(-20f, animationSpec = tween(160))
              touchTranslateY.animateTo(0f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 480f))
            }
            launch {
              touchRotation.animateTo(-5.5f, animationSpec = tween(110))
              touchRotation.animateTo(5.5f, animationSpec = tween(130))
              touchRotation.animateTo(-3.2f, animationSpec = tween(120))
              touchRotation.animateTo(0f, animationSpec = tween(160))
            }
          }
        }
      }
    } else {
      touchTranslateY.snapTo(0f)
      touchRotation.snapTo(0f)
      touchScaleBoost.snapTo(0f)
      extraLayerProgress.snapTo(0f)
    }
  }

  val colorFilter = remember(saturation) {
    if (saturation >= 0.995f) null
    else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
  }

  val combinedScale = focusScale + touchScaleBoost.value

  Box(
    modifier = modifier
      .graphicsLayer {
        scaleX = combinedScale
        scaleY = combinedScale
        translationY = touchTranslateY.value
        rotationZ = touchRotation.value
        compositingStrategy = CompositingStrategy.Offscreen
      }
      .then(focusModifier)
      .clickable(interactionSource = interactionSource, indication = null) {
        onActivated()
      },
    contentAlignment = Alignment.BottomCenter
  ) {
    // Extra blend-mode layer drawn ONLY while this owner character is selected
    if (isSelected) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .alpha(if (saturation > 0.05f) 1f else 0f)
      ) {
        drawCharacterExtraLayer(
          spec = spec,
          progress = extraLayerProgress.value,
          seed = comicSeed,
          triggerCount = selectionTriggerCount
        )
      }
    }

    // Character Cutout Layer (decoded async in Dispatchers.IO so cold start stays stutter-free)
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = spec.showTitle,
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter,
        modifier = Modifier.fillMaxSize()
      )
    }

    // Foreground accents drawn OVER owner character ONLY while selected
    if (isSelected && (spec.touchType == CharacterTouchType.DORAEMON_TAKECOPTER ||
        spec.touchType == CharacterTouchType.SHINCHAN_COMIC_WIGGLE ||
        spec.touchType == CharacterTouchType.COURT_GAVEL_SLAM)
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCharacterForegroundAccent(
          touchType = spec.touchType,
          progress = extraLayerProgress.value,
          seed = comicSeed
        )
      }
    }
  }
}

private fun DrawScope.drawCharacterExtraLayer(
  spec: FavouriteCharacterSpec,
  progress: Float,
  seed: Int,
  triggerCount: Int
) {
  when (spec.touchType) {
    CharacterTouchType.HEART_BEAT_PULSE -> {
      // Glowing crimson ECG wave behind Dr. Reena's shoulders with BlendMode.Screen
      val midY = size.height * 0.62f
      val path = Path().apply {
        moveTo(0f, midY)
        lineTo(size.width * 0.16f, midY)
        lineTo(size.width * 0.22f, midY - size.height * 0.05f)
        lineTo(size.width * 0.27f, midY + size.height * 0.04f)
        lineTo(size.width * 0.34f, midY - size.height * 0.22f)
        lineTo(size.width * 0.40f, midY + size.height * 0.12f)
        lineTo(size.width * 0.47f, midY)
        lineTo(size.width * 0.64f, midY)
        lineTo(size.width * 0.70f, midY - size.height * 0.14f)
        lineTo(size.width * 0.75f, midY + size.height * 0.08f)
        lineTo(size.width * 0.81f, midY)
        lineTo(size.width * progress.coerceIn(0.05f, 1f), midY)
      }
      val alphaGlow = if (progress < 0.85f) 0.92f else 0.55f
      drawPath(
        path = path,
        color = Color(0xFFF43F5E).copy(alpha = alphaGlow),
        style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = BlendMode.Screen
      )
      drawPath(
        path = path,
        color = Color(0xFFFDA4AF).copy(alpha = alphaGlow * 0.75f),
        style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = BlendMode.Screen
      )
    }

    CharacterTouchType.COURT_GAVEL_SLAM -> {
      // Verdict seal + etched justice scales crest with BlendMode.Darken + radial gavel shockwave
      val center = Offset(size.width * 0.48f, size.height * 0.34f)
      val shockRadius = size.width * (0.28f + 0.55f * progress)
      val ringAlpha = (1f - progress).coerceIn(0f, 1f) * 0.85f
      if (ringAlpha > 0.02f) {
        drawCircle(
          color = Color(0xFFEAB308).copy(alpha = ringAlpha),
          radius = shockRadius,
          center = center,
          style = Stroke(width = 4f),
          blendMode = BlendMode.Screen
        )
      }
      // Persistent owner-only Scales of Justice balance mark behind shoulders (Darken shadow + gold trim)
      val barHalf = size.width * 0.36f
      val barY = size.height * 0.14f
      val scaleAlpha = (progress * 3.5f).coerceIn(0f, 1f)
      drawLine(
        color = Color(0xFF451A03).copy(alpha = 0.55f * scaleAlpha),
        start = Offset(center.x - barHalf, barY + 2f),
        end = Offset(center.x + barHalf, barY + 2f),
        strokeWidth = 6f,
        cap = StrokeCap.Round,
        blendMode = BlendMode.Darken
      )
      drawLine(
        color = Color(0xFFFDE047).copy(alpha = 0.72f * scaleAlpha),
        start = Offset(center.x - barHalf, barY),
        end = Offset(center.x + barHalf, barY),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
        blendMode = BlendMode.Screen
      )
      drawCircle(
        color = Color(0xFFFDE047).copy(alpha = 0.65f * scaleAlpha),
        radius = 6f,
        center = Offset(center.x, barY),
        blendMode = BlendMode.Screen
      )
    }

    CharacterTouchType.SIRAGADIKKA_SHIMMER -> {
      // Warm marigold sunbeam rays fanning upward behind Muthu & Meena (BlendMode.Screen)
      val origin = Offset(size.width * 0.48f, size.height * 0.52f)
      val sweep = (0.35f + 0.65f * progress).coerceIn(0f, 1f)
      val steadyAlpha = 0.48f - progress * 0.18f
      val rayAngles = listOf(-48f, -26f, -6f, 14f, 34f, 52f)
      rayAngles.forEachIndexed { idx, deg ->
        val rad = deg * (PI / 180.0)
        val rayLen = size.height * (0.48f + (idx % 2) * 0.14f) * sweep
        val end = Offset(
          origin.x + (sin(rad) * rayLen).toFloat(),
          origin.y - (cos(rad) * rayLen).toFloat()
        )
        drawLine(
          brush = Brush.linearGradient(
            colors = listOf(
              Color(0xFFF59E0B).copy(alpha = steadyAlpha.coerceIn(0.22f, 0.55f)),
              Color.Transparent
            ),
            start = origin,
            end = end
          ),
          start = origin,
          end = end,
          strokeWidth = 14f,
          cap = StrokeCap.Round,
          blendMode = BlendMode.Screen
        )
      }
    }

    CharacterTouchType.DORAEMON_TAKECOPTER -> {
      // Glowing 4D Pocket aura behind Doraemon
      val center = Offset(size.width * 0.5f, size.height * 0.55f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF38BDF8).copy(alpha = 0.52f),
            Color.Transparent
          ),
          center = center,
          radius = size.width * 0.58f
        ),
        center = center,
        radius = size.width * 0.58f,
        blendMode = BlendMode.Screen
      )
    }

    CharacterTouchType.SHINCHAN_COMIC_WIGGLE -> {
      // Pop background glow circle
      val rng = Random(seed + triggerCount)
      val bgAlpha = (0.42f - progress * 0.14f).coerceIn(0.20f, 0.45f)
      drawCircle(
        color = Color(0xFFEF4444).copy(alpha = bgAlpha),
        radius = size.width * (0.36f + rng.nextFloat() * 0.08f),
        center = Offset(size.width * 0.56f, size.height * 0.34f),
        blendMode = BlendMode.Screen
      )
    }
  }
}

private fun DrawScope.drawCharacterForegroundAccent(
  touchType: CharacterTouchType,
  progress: Float,
  seed: Int
) {
  when (touchType) {
    CharacterTouchType.DORAEMON_TAKECOPTER -> {
      // Spinning Take-copter blur arc during the 1-shot spin + crisp resting glint
      val propCenter = Offset(size.width * 0.52f, size.height * 0.08f)
      val arcAlpha = if (progress < 0.85f) 0.92f else 0.25f
      rotate(degrees = progress * 540f, pivot = propCenter) {
        drawOval(
          color = Color(0xFFE0F2FE).copy(alpha = arcAlpha),
          topLeft = Offset(propCenter.x - size.width * 0.16f, propCenter.y - 6f),
          size = androidx.compose.ui.geometry.Size(size.width * 0.32f, 12f),
          style = Stroke(width = 3f)
        )
      }
    }

    CharacterTouchType.SHINCHAN_COMIC_WIGGLE -> {
      // Randomized comic-style burst lines around Shinchan's head (keeps subtle glint after 1-shot pop)
      val rng = Random(seed)
      val headCenter = Offset(size.width * 0.56f, size.height * 0.24f)
      val burstAlpha = (1f - progress * 0.72f).coerceIn(0.24f, 1f)
      repeat(7) { idx ->
        val baseDeg = -135f + idx * 42f + (rng.nextFloat() - 0.5f) * 18f
        val rad = baseDeg * (PI / 180.0)
        val innerR = size.width * (0.28f + 0.10f * progress)
        val outerR = innerR + size.width * (0.11f + rng.nextFloat() * 0.07f)
        val start = Offset(
          headCenter.x + (cos(rad) * innerR).toFloat(),
          headCenter.y + (sin(rad) * innerR).toFloat()
        )
        val end = Offset(
          headCenter.x + (cos(rad) * outerR).toFloat(),
          headCenter.y + (sin(rad) * outerR).toFloat()
        )
        drawLine(
          color = if (idx % 2 == 0) Color(0xFFFDE047).copy(alpha = burstAlpha) else Color.White.copy(alpha = burstAlpha),
          start = start,
          end = end,
          strokeWidth = 4f,
          cap = StrokeCap.Round
        )
      }
    }

    CharacterTouchType.COURT_GAVEL_SLAM -> {
      // Subtle gavel seal medallion near shoulder (Darken ink ring + gold accent)
      val sealCenter = Offset(size.width * 0.80f, size.height * 0.16f)
      val fadeIn = (progress * 3f).coerceIn(0f, 1f)
      drawCircle(
        color = Color(0xFF78350F).copy(alpha = 0.50f * fadeIn),
        radius = 12f,
        center = sealCenter,
        blendMode = BlendMode.Darken
      )
      drawCircle(
        color = Color(0xFFFACC15).copy(alpha = 0.85f * fadeIn),
        radius = 9f,
        center = sealCenter,
        style = Stroke(width = 2f)
      )
    }

    else -> Unit
  }
}
