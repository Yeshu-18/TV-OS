package com.firelite.tv.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.firelite.core.data.HotstarDeepLinks
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.tv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Interactive stage of the "Don't have time to browse?" Mood Cards Deck.
 */
enum class MoodDeckStage {
  BROWSE,
  REEL_SPINNING,
  COUNTDOWN
}

/**
 * Definition of a curated mood category shown in the deck stack.
 */
@Immutable
data class MoodCategorySpec(
  val id: String,
  val name: String,
  val colorHex: Long,
  val isAllTitlesBucket: Boolean = false
) {
  val color: Color get() = Color(colorHex)
}

/**
 * Definition of a curated show/movie entry belonging to one or more moods.
 */
@Immutable
data class MoodTitleSpec(
  val id: String,
  val title: String,
  val appName: String,
  val packageName: String,
  val link: String,
  val moodIds: List<String>,
  val posterResName: String,
  val backdropResName: String
) {
  fun toMediaItem(accentColor: Long): MediaItem = MediaItem(
    id = "mood_$id",
    title = title,
    category = Category.SERIES,
    subtitle = appName,
    platform = appName,
    deepLinkPackage = packageName,
    contentId = id,
    contentUrl = link,
    accentColorHex = accentColor
  )
}

/**
 * Resolved mood card containing only titles whose target streaming app is installed.
 */
@Immutable
data class PlayableMoodCard(
  val category: MoodCategorySpec,
  val titles: List<MoodTitleSpec>
)

private val MOOD_POSTER_RES_MAP: Map<String, Int> = mapOf(
  "mood_poster_the_office" to R.drawable.mood_poster_the_office,
  "mood_poster_modern_family" to R.drawable.mood_poster_modern_family,
  "mood_poster_himym" to R.drawable.mood_poster_himym,
  "mood_poster_brooklyn_99" to R.drawable.mood_poster_brooklyn_99,
  "mood_poster_panchayat" to R.drawable.mood_poster_panchayat,
  "mood_poster_the_boys" to R.drawable.mood_poster_the_boys,
  "mood_poster_deadpool_wolverine" to R.drawable.mood_poster_deadpool_wolverine,
  "mood_poster_avengers_endgame" to R.drawable.mood_poster_avengers_endgame,
  "mood_poster_loki" to R.drawable.mood_poster_loki,
  "mood_poster_invincible" to R.drawable.mood_poster_invincible,
  "mood_poster_leo" to R.drawable.mood_poster_leo,
  "mood_poster_master" to R.drawable.mood_poster_master,
  "mood_poster_thuppakki" to R.drawable.mood_poster_thuppakki,
  "mood_poster_mersal" to R.drawable.mood_poster_mersal,
  "mood_poster_vikram" to R.drawable.mood_poster_vikram,
  "mood_poster_amaran" to R.drawable.mood_poster_amaran,
  "mood_poster_dark" to R.drawable.mood_poster_dark,
  "mood_poster_breaking_bad" to R.drawable.mood_poster_breaking_bad,
  "mood_poster_mindhunter" to R.drawable.mood_poster_mindhunter,
  "mood_poster_farzi" to R.drawable.mood_poster_farzi,
  "mood_poster_maharaja" to R.drawable.mood_poster_maharaja,
  "mood_poster_stranger_things" to R.drawable.mood_poster_stranger_things,
  "mood_poster_peaky_blinders" to R.drawable.mood_poster_peaky_blinders,
  "mood_poster_mirzapur" to R.drawable.mood_poster_mirzapur
)

private val MOOD_BACKDROP_RES_MAP: Map<String, Int> = mapOf(
  "mood_bg_the_office" to R.drawable.mood_bg_the_office,
  "mood_bg_modern_family" to R.drawable.mood_bg_modern_family,
  "mood_bg_himym" to R.drawable.mood_bg_himym,
  "mood_bg_brooklyn_99" to R.drawable.mood_bg_brooklyn_99,
  "mood_bg_panchayat" to R.drawable.mood_bg_panchayat,
  "mood_bg_the_boys" to R.drawable.mood_bg_the_boys,
  "mood_bg_deadpool_wolverine" to R.drawable.mood_bg_deadpool_wolverine,
  "mood_bg_avengers_endgame" to R.drawable.mood_bg_avengers_endgame,
  "mood_bg_loki" to R.drawable.mood_bg_loki,
  "mood_bg_invincible" to R.drawable.mood_bg_invincible,
  "mood_bg_leo" to R.drawable.mood_bg_leo,
  "mood_bg_master" to R.drawable.mood_bg_master,
  "mood_bg_thuppakki" to R.drawable.mood_bg_thuppakki,
  "mood_bg_mersal" to R.drawable.mood_bg_mersal,
  "mood_bg_vikram" to R.drawable.mood_bg_vikram,
  "mood_bg_amaran" to R.drawable.mood_bg_amaran,
  "mood_bg_dark" to R.drawable.mood_bg_dark,
  "mood_bg_breaking_bad" to R.drawable.mood_bg_breaking_bad,
  "mood_bg_mindhunter" to R.drawable.mood_bg_mindhunter,
  "mood_bg_farzi" to R.drawable.mood_bg_farzi,
  "mood_bg_maharaja" to R.drawable.mood_bg_maharaja,
  "mood_bg_stranger_things" to R.drawable.mood_bg_stranger_things,
  "mood_bg_peaky_blinders" to R.drawable.mood_bg_peaky_blinders,
  "mood_bg_mirzapur" to R.drawable.mood_bg_mirzapur
)

private const val PKG_HOTSTAR = HotstarDeepLinks.PACKAGE_NAME
private const val PKG_NETFLIX = "com.netflix.ninja"
private const val PKG_PRIME = "com.amazon.firebat"

/**
 * Returns the 6 mood deck categories in display order.
 * The final card "Anything at all" (#B8C0CC) aggregates every installed title automatically.
 */
fun buildDefaultMoodCategories(): List<MoodCategorySpec> = listOf(
  MoodCategorySpec("comfort_sitcom", "Comfort sitcom", 0xFFF2B84B),
  MoodCategorySpec("capes_chaos", "Capes and chaos", 0xFFE5484D),
  MoodCategorySpec("tamil_mass", "Thalapathy & Tamil mass", 0xFFFF6B2C),
  MoodCategorySpec("mind_games", "Mind games", 0xFF38BDF8),
  MoodCategorySpec("after_hours", "After hours drama", 0xFFA855F7),
  MoodCategorySpec("anything", "Anything at all", 0xFFB8C0CC, isAllTitlesBucket = true)
)

/**
 * Returns the 24 curated shows & movies across JioHotstar, Netflix, and Prime Video India.
 */
fun buildDefaultMoodTitles(): List<MoodTitleSpec> = listOf(
  MoodTitleSpec(
    id = "the_office",
    title = "The Office",
    appName = "Prime Video",
    packageName = PKG_PRIME,
    link = "https://www.primevideo.com/detail/0GWXUJDVFSE2NH8PSEC3MS6ADZ",
    moodIds = listOf("comfort_sitcom"),
    posterResName = "mood_poster_the_office",
    backdropResName = "mood_bg_the_office"
  ),
  MoodTitleSpec(
    id = "modern_family",
    title = "Modern Family",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/shows/modern-family/8549",
    moodIds = listOf("comfort_sitcom"),
    posterResName = "mood_poster_modern_family",
    backdropResName = "mood_bg_modern_family"
  ),
  MoodTitleSpec(
    id = "himym",
    title = "How I Met Your Mother",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/shows/how-i-met-your-mother/8323",
    moodIds = listOf("comfort_sitcom"),
    posterResName = "mood_poster_himym",
    backdropResName = "mood_bg_himym"
  ),
  MoodTitleSpec(
    id = "brooklyn_99",
    title = "Brooklyn Nine-Nine",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/70281562",
    moodIds = listOf("comfort_sitcom"),
    posterResName = "mood_poster_brooklyn_99",
    backdropResName = "mood_bg_brooklyn_99"
  ),
  MoodTitleSpec(
    id = "panchayat",
    title = "Panchayat",
    appName = "Prime Video",
    packageName = PKG_PRIME,
    link = "https://www.primevideo.com/detail/0KAFPP1MAADAFUOA6I2WR5D4TM",
    moodIds = listOf("comfort_sitcom"),
    posterResName = "mood_poster_panchayat",
    backdropResName = "mood_bg_panchayat"
  ),
  MoodTitleSpec(
    id = "the_boys",
    title = "The Boys",
    appName = "Prime Video",
    packageName = PKG_PRIME,
    link = "https://www.primevideo.com/detail/0S1FYJ3LY9KTL9C7WFFAGA9F6F",
    moodIds = listOf("capes_chaos"),
    posterResName = "mood_poster_the_boys",
    backdropResName = "mood_bg_the_boys"
  ),
  MoodTitleSpec(
    id = "deadpool_wolverine",
    title = "Deadpool & Wolverine",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/movies/deadpool-wolverine/1271305185",
    moodIds = listOf("capes_chaos"),
    posterResName = "mood_poster_deadpool_wolverine",
    backdropResName = "mood_bg_deadpool_wolverine"
  ),
  MoodTitleSpec(
    id = "avengers_endgame",
    title = "Avengers: Endgame",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/movies/avengers-endgame/1260013556",
    moodIds = listOf("capes_chaos"),
    posterResName = "mood_poster_avengers_endgame",
    backdropResName = "mood_bg_avengers_endgame"
  ),
  MoodTitleSpec(
    id = "loki",
    title = "Loki",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/shows/loki/1260063451",
    moodIds = listOf("capes_chaos", "mind_games"),
    posterResName = "mood_poster_loki",
    backdropResName = "mood_bg_loki"
  ),
  MoodTitleSpec(
    id = "invincible",
    title = "Invincible",
    appName = "Prime Video",
    packageName = PKG_PRIME,
    link = "https://www.primevideo.com/detail/0K677J96WQ96K6UY6BL15O70CO",
    moodIds = listOf("capes_chaos"),
    posterResName = "mood_poster_invincible",
    backdropResName = "mood_bg_invincible"
  ),
  MoodTitleSpec(
    id = "leo",
    title = "Leo",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/81639319",
    moodIds = listOf("tamil_mass"),
    posterResName = "mood_poster_leo",
    backdropResName = "mood_bg_leo"
  ),
  MoodTitleSpec(
    id = "master",
    title = "Master",
    appName = "Prime Video",
    packageName = PKG_PRIME,
    link = "https://www.primevideo.com/detail/0HSY45SA9NH2ABLBGAJEVV1HAL",
    moodIds = listOf("tamil_mass"),
    posterResName = "mood_poster_master",
    backdropResName = "mood_bg_master"
  ),
  MoodTitleSpec(
    id = "thuppakki",
    title = "Thuppakki",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/movies/thuppakki/1000035948",
    moodIds = listOf("tamil_mass"),
    posterResName = "mood_poster_thuppakki",
    backdropResName = "mood_bg_thuppakki"
  ),
  MoodTitleSpec(
    id = "mersal",
    title = "Mersal",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/movies/mersal/1000217938",
    moodIds = listOf("tamil_mass"),
    posterResName = "mood_poster_mersal",
    backdropResName = "mood_bg_mersal"
  ),
  MoodTitleSpec(
    id = "vikram",
    title = "Vikram",
    appName = "JioHotstar",
    packageName = PKG_HOTSTAR,
    link = "hotstar://in/movies/vikram/1260106598",
    moodIds = listOf("tamil_mass"),
    posterResName = "mood_poster_vikram",
    backdropResName = "mood_bg_vikram"
  ),
  MoodTitleSpec(
    id = "amaran",
    title = "Amaran",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/81728596",
    moodIds = listOf("tamil_mass"),
    posterResName = "mood_poster_amaran",
    backdropResName = "mood_bg_amaran"
  ),
  MoodTitleSpec(
    id = "dark",
    title = "Dark",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/80100172",
    moodIds = listOf("mind_games", "after_hours"),
    posterResName = "mood_poster_dark",
    backdropResName = "mood_bg_dark"
  ),
  MoodTitleSpec(
    id = "breaking_bad",
    title = "Breaking Bad",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/70143836",
    moodIds = listOf("mind_games", "after_hours"),
    posterResName = "mood_poster_breaking_bad",
    backdropResName = "mood_bg_breaking_bad"
  ),
  MoodTitleSpec(
    id = "mindhunter",
    title = "Mindhunter",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/80114855",
    moodIds = listOf("mind_games"),
    posterResName = "mood_poster_mindhunter",
    backdropResName = "mood_bg_mindhunter"
  ),
  MoodTitleSpec(
    id = "farzi",
    title = "Farzi",
    appName = "Prime Video",
    packageName = PKG_PRIME,
    link = "https://www.primevideo.com/detail/0HDHQAUF5LPWOJRCO025LFJSJI",
    moodIds = listOf("mind_games"),
    posterResName = "mood_poster_farzi",
    backdropResName = "mood_bg_farzi"
  ),
  MoodTitleSpec(
    id = "maharaja",
    title = "Maharaja",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/81690671",
    moodIds = listOf("mind_games", "tamil_mass"),
    posterResName = "mood_poster_maharaja",
    backdropResName = "mood_bg_maharaja"
  ),
  MoodTitleSpec(
    id = "stranger_things",
    title = "Stranger Things",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/80057281",
    moodIds = listOf("after_hours"),
    posterResName = "mood_poster_stranger_things",
    backdropResName = "mood_bg_stranger_things"
  ),
  MoodTitleSpec(
    id = "peaky_blinders",
    title = "Peaky Blinders",
    appName = "Netflix",
    packageName = PKG_NETFLIX,
    link = "https://www.netflix.com/in/title/80002479",
    moodIds = listOf("after_hours"),
    posterResName = "mood_poster_peaky_blinders",
    backdropResName = "mood_bg_peaky_blinders"
  ),
  MoodTitleSpec(
    id = "mirzapur",
    title = "Mirzapur",
    appName = "Prime Video",
    packageName = PKG_PRIME,
    link = "https://www.primevideo.com/detail/0PDOKMV9CRLOMO5EUKNCUJLG4Q",
    moodIds = listOf("after_hours"),
    posterResName = "mood_poster_mirzapur",
    backdropResName = "mood_bg_mirzapur"
  )
)

/**
 * Filters mood titles against [installedPackages], drops empty moods, and builds
 * "Anything at all" automatically from all installed titles.
 */
fun filterPlayableMoodDecks(
  installedPackages: Set<String>,
  categories: List<MoodCategorySpec> = buildDefaultMoodCategories(),
  allTitles: List<MoodTitleSpec> = buildDefaultMoodTitles()
): List<PlayableMoodCard> {
  val installedTitles = allTitles.filter { it.packageName in installedPackages }
  if (installedTitles.isEmpty()) return emptyList()

  return categories.mapNotNull { category ->
    val matching = if (category.isAllTitlesBucket) {
      installedTitles
    } else {
      installedTitles.filter { category.id in it.moodIds }
    }
    if (matching.isEmpty()) null else PlayableMoodCard(category = category, titles = matching)
  }
}

/**
 * Discovers installed streaming packages used by the mood deck via [PackageManager].
 */
fun detectInstalledStreamingPackages(context: Context): Set<String> {
  val pm = context.packageManager
  val candidatePkgs = buildDefaultMoodTitles().map { it.packageName }.toSet()
  return candidatePkgs.filterTo(mutableSetOf()) { pkg ->
    if (pm.getLeanbackLaunchIntentForPackage(pkg) != null || pm.getLaunchIntentForPackage(pkg) != null) {
      true
    } else {
      runCatching {
        pm.getPackageInfo(pkg, 0)
        true
      }.getOrDefault(false)
    }
  }
}

/**
 * Pure helper to wrap deck top index forward by 1 when swiping left.
 */
fun advanceDeckIndex(currentIndex: Int, deckSize: Int): Int {
  if (deckSize <= 0) return 0
  return ((currentIndex + 1) % deckSize + deckSize) % deckSize
}

/**
 * Picks a random title from [titles], avoiding [excludeTitleId] when alternatives exist.
 */
fun pickRandomSpinTitle(
  titles: List<MoodTitleSpec>,
  excludeTitleId: String? = null,
  random: Random = Random.Default
): MoodTitleSpec? {
  if (titles.isEmpty()) return null
  val pool = if (titles.size > 1 && excludeTitleId != null) {
    titles.filter { it.id != excludeTitleId }.ifEmpty { titles }
  } else {
    titles
  }
  return pool[random.nextInt(pool.size)]
}

/**
 * Builds the vertical poster sequence ending on [winner] for the slot-machine reel spin.
 */
fun buildReelStrip(
  titles: List<MoodTitleSpec>,
  winner: MoodTitleSpec,
  hopCount: Int = 20,
  random: Random = Random.Default
): List<MoodTitleSpec> {
  if (titles.isEmpty()) return listOf(winner)
  val strip = ArrayList<MoodTitleSpec>(hopCount + 2)
  for (i in 0 until hopCount) {
    val prev = strip.lastOrNull()
    val candidate = pickRandomSpinTitle(titles, excludeTitleId = prev?.id, random = random) ?: winner
    strip.add(candidate)
  }
  strip.add(winner)
  // Trailing wrap item so bottom edge of center reticle stays visually seamless
  val trailing = pickRandomSpinTitle(titles, excludeTitleId = winner.id, random = random) ?: winner
  strip.add(trailing)
  return strip
}

/**
 * Cache holding decoded `RGB_565` posters and lazily decoded `16:9` scene backdrops
 * so scrolling/swiping stays smooth within Fire TV memory limits.
 */
class PreloadedMoodAssets {
  val posters = mutableStateMapOf<String, ImageBitmap>()
  val backdrops = mutableStateMapOf<String, ImageBitmap>()
}

private fun decodeResRgb565(
  context: Context,
  resId: Int
): ImageBitmap? = runCatching {
  val opts = BitmapFactory.Options().apply {
    inPreferredConfig = Bitmap.Config.RGB_565
  }
  BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
}.getOrNull()

/**
 * Preloads fanned posters upfront and lazily caches neighboring/winner backdrops on [Dispatchers.IO].
 */
@Composable
fun rememberPreloadedMoodAssets(
  decks: List<PlayableMoodCard>,
  topIndex: Int,
  winnerTitle: MoodTitleSpec?
): PreloadedMoodAssets {
  val context = LocalContext.current
  val assets = remember { PreloadedMoodAssets() }

  // Prioritize first 3 posters per deck card, then decode remaining posters gently off main thread
  LaunchedEffect(decks) {
    val prioritizedIds = decks.flatMap { it.titles.take(3) }.plus(decks.flatMap { it.titles })
      .distinctBy { it.posterResName }
    withContext(Dispatchers.IO) {
      prioritizedIds.forEach { title ->
        if (!assets.posters.containsKey(title.posterResName)) {
          val resId = MOOD_POSTER_RES_MAP[title.posterResName]
          if (resId != null) {
            decodeResRgb565(context, resId)?.let { bmp ->
              withContext(Dispatchers.Main) { assets.posters[title.posterResName] = bmp }
            }
          }
        }
      }
    }
  }

  // Eagerly decode backdrop for top deck, next 2 peeking decks, and current/pending spin winner
  LaunchedEffect(decks, topIndex, winnerTitle?.id) {
    if (decks.isEmpty()) return@LaunchedEffect
    val targets = buildList {
      for (offset in 0..minOf(2, decks.lastIndex)) {
        val deckIdx = (topIndex + offset) % decks.size
        resolveDeckBackdropTitle(decks, deckIdx, winner = null)?.let { add(it) }
      }
      winnerTitle?.let { add(it) }
    }.distinctBy { it.backdropResName }

    withContext(Dispatchers.IO) {
      targets.forEach { title ->
        if (!assets.backdrops.containsKey(title.backdropResName)) {
          val resId = MOOD_BACKDROP_RES_MAP[title.backdropResName]
          if (resId != null) {
            decodeResRgb565(context, resId)?.let { bmp ->
              withContext(Dispatchers.Main) { assets.backdrops[title.backdropResName] = bmp }
            }
          }
        }
      }
    }
  }

  return assets
}

private val BW_COLOR_FILTER: ColorFilter = ColorFilter.colorMatrix(
  ColorMatrix().apply { setToSaturation(0f) }
)

/**
 * Full-screen heavily darkened, vignetted black-and-white scene backdrop behind the Mood Deck.
 * Changes as the user swipes cards and crossfades to the picked winner's scene when the reel lands.
 */
@Composable
fun MoodDeckSceneBackdrop(
  isVisible: Boolean,
  sceneTitle: MoodTitleSpec?,
  moodColor: Color,
  assets: PreloadedMoodAssets,
  modifier: Modifier = Modifier
) {
  val layerAlpha by animateFloatAsState(
    targetValue = if (isVisible && sceneTitle != null) 1f else 0f,
    animationSpec = tween(durationMillis = 360),
    label = "moodSceneLayerAlpha"
  )
  val animatedMoodGlow by animateColorAsState(
    targetValue = if (isVisible) moodColor else Color.Transparent,
    animationSpec = tween(durationMillis = 380),
    label = "moodBackdropGlow"
  )

  if (layerAlpha <= 0.005f) return

  Box(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer { alpha = layerAlpha }
  ) {
    AnimatedContent(
      targetState = sceneTitle?.backdropResName,
      transitionSpec = {
        fadeIn(tween(durationMillis = 480)) togetherWith fadeOut(tween(durationMillis = 480))
      },
      label = "moodBackdropCrossfade",
      modifier = Modifier.fillMaxSize()
    ) { resName ->
      val bmp = resName?.let { assets.backdrops[it] }
      if (bmp != null) {
        Image(
          bitmap = bmp,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          colorFilter = BW_COLOR_FILTER,
          modifier = Modifier
            .fillMaxSize()
            .alpha(0.34f)
        )
      }
    }

    // Heavy vignette + obsidian darkening wash so foreground cards read with crisp contrast
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0x52070A10),
              Color(0xBB070A10),
              Color(0xF506080E)
            ),
            radius = 920f
          )
        )
    )

    // Subtle vertical scrim grounding bottom deck area and top chrome
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            0.0f to Color(0x9906080D),
            0.42f to Color(0x2806080D),
            0.85f to Color(0xAA06080D),
            1.0f to Color(0xEA05070B)
          )
        )
    )

    // Very soft mood-coloured atmospheric bloom centered behind the deck
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width * 0.5f, size.height * 0.62f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            animatedMoodGlow.copy(alpha = 0.24f),
            animatedMoodGlow.copy(alpha = 0.08f),
            Color.Transparent
          ),
          center = center,
          radius = size.width * 0.42f
        ),
        center = center,
        radius = size.width * 0.42f,
        blendMode = BlendMode.Screen
      )
    }
  }
}

/**
 * Resolves the representative title whose scene should sit behind the deck:
 * either the picked winner once the reel lands, or a deterministic title from the top card.
 */
fun resolveDeckBackdropTitle(
  decks: List<PlayableMoodCard>,
  topIndex: Int,
  winner: MoodTitleSpec?
): MoodTitleSpec? {
  if (winner != null) return winner
  val topCard = decks.getOrNull(topIndex) ?: return null
  if (topCard.titles.isEmpty()) return null
  // Rotate index per deck position so adjacent moods sharing titles display varied scenes
  return topCard.titles[topIndex % topCard.titles.size]
}

/**
 * Interactive "Don't have time to browse?" Mood Cards Deck section under My Favourites.
 */
@Composable
fun MoodDeckSection(
  decks: List<PlayableMoodCard>,
  topIndex: Int,
  stage: MoodDeckStage,
  spinToken: Int,
  winner: MoodTitleSpec?,
  isSectionFocused: Boolean,
  assets: PreloadedMoodAssets,
  deckFocusRequester: FocusRequester,
  upFocusRequesterProvider: () -> FocusRequester,
  onFocusDeck: () -> Unit,
  onSwipeLeft: () -> Unit,
  onStartSpin: () -> Unit,
  onReelLanded: (MoodTitleSpec) -> Unit,
  onCancelToDeck: () -> Unit,
  onOpenTitle: (MoodTitleSpec) -> Unit,
  modifier: Modifier = Modifier
) {
  if (decks.isEmpty()) return

  val activeCard = decks[topIndex % decks.size]
  val moodColor = activeCard.category.color
  val animatedMoodColor by animateColorAsState(
    targetValue = moodColor,
    animationSpec = tween(durationMillis = 320),
    label = "moodDeckActiveColor"
  )

  val scope = rememberCoroutineScope()
  val swipeOffsetX = remember { Animatable(0f) }
  val swipeTiltDeg = remember { Animatable(0f) }
  val swipeAlpha = remember { Animatable(1f) }
  var exitingCard by remember { mutableStateOf<PlayableMoodCard?>(null) }

  // Cancel reel or countdown with Android Back button cleanly
  BackHandler(enabled = isSectionFocused && stage != MoodDeckStage.BROWSE) {
    onCancelToDeck()
  }

  fun triggerSwipeLeft() {
    if (stage != MoodDeckStage.BROWSE || decks.size <= 1) return
    val departing = activeCard
    exitingCard = departing
    onSwipeLeft()
    scope.launch {
      swipeOffsetX.snapTo(0f)
      swipeTiltDeg.snapTo(0f)
      swipeAlpha.snapTo(1f)
      coroutineScope {
        launch {
          swipeOffsetX.animateTo(-620f, animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing))
        }
        launch {
          swipeTiltDeg.animateTo(-15f, animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing))
        }
        launch {
          swipeAlpha.animateTo(0f, animationSpec = tween(durationMillis = 240))
        }
      }
      exitingCard = null
    }
  }

  // Scale-in cue when the whole Mood Deck receives focus
  val cardFocusScale by animateFloatAsState(
    targetValue = if (isSectionFocused) 1.015f else 1f,
    animationSpec = tween(durationMillis = 200),
    label = "moodDeckFocusScale"
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header row matching My Favourites / Applications typography
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "DON'T HAVE TIME TO BROWSE?",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(animatedMoodColor.copy(alpha = 0.20f))
            .border(1.dp, animatedMoodColor.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 1.dp)
        ) {
          Text(
            text = "${(topIndex % decks.size) + 1} / ${decks.size}",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = animatedMoodColor
          )
        }
      }

      Text(
        text = when (stage) {
          MoodDeckStage.BROWSE -> "← Skip mood  •  → / OK Spin"
          MoodDeckStage.REEL_SPINNING -> "← / Back Cancel"
          MoodDeckStage.COUNTDOWN -> "OK Open now  •  → Spin again  •  ← / Back Cancel"
        },
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xB8E2E8F0)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Focusable stage box (single focus node so D-pad Left/Right/OK/Up/Back never lose focus mid-spin)
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxWidth()
        .height(332.dp)
        .focusRequester(deckFocusRequester)
        .focusProperties {
          up = if (stage == MoodDeckStage.BROWSE) upFocusRequesterProvider() else FocusRequester.Cancel
          down = FocusRequester.Cancel
          left = FocusRequester.Cancel
          right = FocusRequester.Cancel
        }
        .onFocusChanged { focusState ->
          if (focusState.isFocused) {
            onFocusDeck()
          }
        }
        .onPreviewKeyEvent { keyEvent ->
          if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
          when (stage) {
            MoodDeckStage.BROWSE -> when (keyEvent.key) {
              Key.DirectionLeft -> {
                triggerSwipeLeft()
                true
              }
              Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                onStartSpin()
                true
              }
              Key.DirectionDown -> true
              else -> false
            }

            MoodDeckStage.REEL_SPINNING -> when (keyEvent.key) {
              Key.DirectionLeft, Key.Back, Key.Escape -> {
                onCancelToDeck()
                true
              }
              Key.DirectionUp, Key.DirectionDown, Key.DirectionRight,
              Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> true
              else -> false
            }

            MoodDeckStage.COUNTDOWN -> when (keyEvent.key) {
              Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                winner?.let { onOpenTitle(it) }
                true
              }
              Key.DirectionRight -> {
                onStartSpin()
                true
              }
              Key.DirectionLeft, Key.Back, Key.Escape -> {
                onCancelToDeck()
                true
              }
              Key.DirectionUp, Key.DirectionDown -> true
              else -> false
            }
          }
        }
        .focusable()
    ) {
      // Soft ambient glow directly behind cards
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.5f, size.height * 0.52f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              animatedMoodColor.copy(alpha = if (isSectionFocused) 0.30f else 0.14f),
              animatedMoodColor.copy(alpha = 0.06f),
              Color.Transparent
            ),
            center = center,
            radius = size.width * 0.36f
          ),
          center = center,
          radius = size.width * 0.36f,
          blendMode = BlendMode.Screen
        )
      }

      when (stage) {
        MoodDeckStage.BROWSE -> {
          MoodDeckStack(
            decks = decks,
            topIndex = topIndex,
            isFocused = isSectionFocused,
            focusScale = cardFocusScale,
            assets = assets,
            exitingCard = exitingCard,
            exitingOffsetX = swipeOffsetX.value,
            exitingTiltDeg = swipeTiltDeg.value,
            exitingAlpha = swipeAlpha.value
          )
        }

        MoodDeckStage.REEL_SPINNING, MoodDeckStage.COUNTDOWN -> {
          MoodReelAndCountdownStage(
            moodCard = activeCard,
            stage = stage,
            spinToken = spinToken,
            initialWinner = winner,
            assets = assets,
            onReelLanded = onReelLanded,
            onCountdownFinished = { finishedWinner ->
              onOpenTitle(finishedWinner)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun BoxScope.MoodDeckStack(
  decks: List<PlayableMoodCard>,
  topIndex: Int,
  isFocused: Boolean,
  focusScale: Float,
  assets: PreloadedMoodAssets,
  exitingCard: PlayableMoodCard?,
  exitingOffsetX: Float,
  exitingTiltDeg: Float,
  exitingAlpha: Float
) {
  val visiblePeekDepth = minOf(2, decks.lastIndex)

  // Render back peeking cards first (depth 2 -> depth 1)
  for (depth in visiblePeekDepth downTo 1) {
    val peekCard = decks[(topIndex + depth) % decks.size]
    val targetShiftX = if (depth == 1) 42f else 78f
    val targetShiftY = if (depth == 1) 5f else 10f
    val targetScale = if (depth == 1) 0.935f else 0.875f
    val targetRot = if (depth == 1) 2.5f else 5.0f
    val targetAlpha = if (depth == 1) 0.72f else 0.42f

    MoodFrontCard(
      card = peekCard,
      assets = assets,
      isTopCard = false,
      isFocused = false,
      modifier = Modifier
        .zIndex((visiblePeekDepth - depth).toFloat())
        .offset(x = (-22 + targetShiftX).dp, y = targetShiftY.dp)
        .graphicsLayer {
          scaleX = targetScale
          scaleY = targetScale
          rotationZ = targetRot
          alpha = targetAlpha
        }
    )
  }

  // Front top card
  val topCard = decks[topIndex % decks.size]
  MoodFrontCard(
    card = topCard,
    assets = assets,
    isTopCard = true,
    isFocused = isFocused,
    modifier = Modifier
      .zIndex(5f)
      .offset(x = (-22).dp)
      .graphicsLayer {
        scaleX = focusScale
        scaleY = focusScale
      }
  )

  // Exiting card flying left + tilting + fading on swipe
  if (exitingCard != null && exitingAlpha > 0.01f) {
    MoodFrontCard(
      card = exitingCard,
      assets = assets,
      isTopCard = true,
      isFocused = true,
      modifier = Modifier
        .zIndex(10f)
        .offset(x = (-22).dp)
        .graphicsLayer {
          translationX = exitingOffsetX
          rotationZ = exitingTiltDeg
          alpha = exitingAlpha
        }
    )
  }
}

@Composable
private fun MoodFrontCard(
  card: PlayableMoodCard,
  assets: PreloadedMoodAssets,
  isTopCard: Boolean,
  isFocused: Boolean,
  modifier: Modifier = Modifier
) {
  val moodColor = card.category.color
  val shape = RoundedCornerShape(24.dp)
  val borderColor = when {
    isTopCard && isFocused -> moodColor.copy(alpha = 0.95f)
    isTopCard -> Color.White.copy(alpha = 0.24f)
    else -> moodColor.copy(alpha = 0.28f)
  }

  Box(
    modifier = modifier
      .width(446.dp)
      .height(308.dp)
      .shadow(
        elevation = if (isTopCard) 24.dp else 10.dp,
        shape = shape,
        spotColor = moodColor.copy(alpha = if (isFocused) 0.50f else 0.25f)
      )
      .clip(shape)
      .background(
        Brush.verticalGradient(
          0.0f to lerpColor(Color(0xFF0E131D), moodColor, 0.34f),
          0.48f to lerpColor(Color(0xFF0A0D14), moodColor, 0.14f),
          1.0f to Color(0xFF06080D)
        )
      )
      .border(
        width = if (isTopCard && isFocused) 2.5.dp else 1.2.dp,
        color = borderColor,
        shape = shape
      )
  ) {
    // Subtle corner mood wash
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(moodColor.copy(alpha = 0.28f), Color.Transparent),
          center = Offset(size.width * 0.5f, size.height * 0.14f),
          radius = size.width * 0.72f
        ),
        center = Offset(size.width * 0.5f, size.height * 0.14f),
        radius = size.width * 0.72f
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // First three 2:3 posters fanned out neatly
      val fanTitles = card.titles.take(3)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .height(172.dp)
      ) {
        when (fanTitles.size) {
          1 -> {
            MoodPosterThumb(
              title = fanTitles[0],
              assets = assets,
              borderColor = moodColor.copy(alpha = 0.7f),
              modifier = Modifier
                .width(104.dp)
                .height(156.dp)
                .zIndex(3f)
            )
          }

          2 -> {
            MoodPosterThumb(
              title = fanTitles[1],
              assets = assets,
              borderColor = Color.White.copy(alpha = 0.22f),
              modifier = Modifier
                .width(98.dp)
                .height(147.dp)
                .offset(x = 46.dp, y = 6.dp)
                .graphicsLayer { rotationZ = 9f }
                .zIndex(1f)
            )
            MoodPosterThumb(
              title = fanTitles[0],
              assets = assets,
              borderColor = moodColor.copy(alpha = 0.7f),
              modifier = Modifier
                .width(104.dp)
                .height(156.dp)
                .offset(x = (-32).dp)
                .graphicsLayer { rotationZ = -6f }
                .zIndex(2f)
            )
          }

          else -> {
            // Index 1 left (-11 deg), index 2 right (+11 deg), index 0 crisp on top in center
            MoodPosterThumb(
              title = fanTitles[1],
              assets = assets,
              borderColor = Color.White.copy(alpha = 0.22f),
              modifier = Modifier
                .width(96.dp)
                .height(144.dp)
                .offset(x = (-74).dp, y = 10.dp)
                .graphicsLayer { rotationZ = -11f }
                .zIndex(1f)
            )
            MoodPosterThumb(
              title = fanTitles[2],
              assets = assets,
              borderColor = Color.White.copy(alpha = 0.22f),
              modifier = Modifier
                .width(96.dp)
                .height(144.dp)
                .offset(x = 74.dp, y = 10.dp)
                .graphicsLayer { rotationZ = 11f }
                .zIndex(2f)
            )
            MoodPosterThumb(
              title = fanTitles[0],
              assets = assets,
              borderColor = if (isFocused) moodColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.35f),
              modifier = Modifier
                .width(106.dp)
                .height(159.dp)
                .offset(y = (-2).dp)
                .zIndex(3f)
            )
          }
        }
      }

      // Mood name + N titles underneath
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = card.category.name,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(moodColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          val countLabel = if (card.titles.size == 1) "1 title" else "${card.titles.size} titles"
          Text(
            text = countLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = moodColor.copy(alpha = 0.95f)
          )
        }

        if (isTopCard) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            DeckHintChip(label = "← Nope", borderColor = Color.White.copy(alpha = 0.16f), textColor = Color(0xFFB8C0CC))
            DeckHintChip(
              label = "OK / → Pick for me",
              borderColor = moodColor.copy(alpha = 0.55f),
              textColor = Color.White
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DeckHintChip(
  label: String,
  borderColor: Color,
  textColor: Color
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(Color(0x5506080D))
      .border(1.dp, borderColor, RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 3.dp)
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = textColor
    )
  }
}

@Composable
private fun MoodPosterThumb(
  title: MoodTitleSpec,
  assets: PreloadedMoodAssets,
  borderColor: Color,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(10.dp)
  val bmp = assets.posters[title.posterResName]
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .shadow(12.dp, shape, spotColor = Color.Black.copy(alpha = 0.7f))
      .clip(shape)
      .background(Color(0xFF141923))
      .border(1.dp, borderColor, shape)
  ) {
    if (bmp != null) {
      Image(
        bitmap = bmp,
        contentDescription = title.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      Text(
        text = title.title,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xCCFFFFFF),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(6.dp)
      )
    }
  }
}

/**
 * Slot-machine poster reel + "Opening in 3…" countdown stage.
 */
@Composable
private fun MoodReelAndCountdownStage(
  moodCard: PlayableMoodCard,
  stage: MoodDeckStage,
  spinToken: Int,
  initialWinner: MoodTitleSpec?,
  assets: PreloadedMoodAssets,
  onReelLanded: (MoodTitleSpec) -> Unit,
  onCountdownFinished: (MoodTitleSpec) -> Unit
) {
  val moodColor = moodCard.category.color
  val density = LocalDensity.current

  var currentStrip by remember { mutableStateOf<List<MoodTitleSpec>>(emptyList()) }
  var chosenWinner by remember { mutableStateOf(initialWinner) }
  val scrollIndexAnim = remember { Animatable(0f) }
  val countdownProgressAnim = remember { Animatable(1f) }
  val landingPopScale = remember { Animatable(1f) }

  // Launch a fresh spin whenever stage enters REEL_SPINNING or spinToken increments
  LaunchedEffect(stage, moodCard.category.id, spinToken) {
    if (stage == MoodDeckStage.REEL_SPINNING) {
      val picked = initialWinner ?: pickRandomSpinTitle(moodCard.titles) ?: return@LaunchedEffect
      val strip = buildReelStrip(moodCard.titles, winner = picked, hopCount = 20)
      val targetIndex = (strip.size - 2).toFloat()
      currentStrip = strip
      chosenWinner = picked
      countdownProgressAnim.snapTo(1f)
      landingPopScale.snapTo(1f)
      scrollIndexAnim.snapTo(0f)

      // Fast burst initially, dramatic exponential deceleration onto winner
      scrollIndexAnim.animateTo(
        targetValue = targetIndex,
        animationSpec = tween(
          durationMillis = 2300,
          easing = CubicBezierEasing(0.08f, 0.92f, 0.22f, 1.0f)
        )
      )
      onReelLanded(picked)

      // Crisp micro-pop on lock-in
      landingPopScale.animateTo(1.06f, animationSpec = tween(110))
      landingPopScale.animateTo(1.0f, animationSpec = tween(160))
    }
  }

  // Run smooth 3-second countdown bar in COUNTDOWN stage
  LaunchedEffect(stage, chosenWinner?.id, spinToken) {
    if (stage == MoodDeckStage.COUNTDOWN && chosenWinner != null) {
      countdownProgressAnim.snapTo(1f)
      countdownProgressAnim.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 3000, easing = { it })
      )
      chosenWinner?.let { onCountdownFinished(it) }
    }
  }

  val cardShape = RoundedCornerShape(24.dp)
  val slotItemHeightDp = 216.dp
  val slotStrideDp = 228.dp
  val slotStridePx = with(density) { slotStrideDp.toPx() }
  val currentSlotIdx = if (currentStrip.isNotEmpty()) {
    floor(scrollIndexAnim.value + 0.5f).toInt().coerceIn(0, currentStrip.lastIndex)
  } else 0
  val rollingTitle = currentStrip.getOrNull(currentSlotIdx) ?: chosenWinner ?: moodCard.titles.first()
  val displayTitle = if (stage == MoodDeckStage.COUNTDOWN) (chosenWinner ?: rollingTitle) else rollingTitle

  val remainingSeconds = when {
    countdownProgressAnim.value > 0.666f -> 3
    countdownProgressAnim.value > 0.333f -> 2
    else -> 1
  }

  Row(
    modifier = Modifier
      .width(652.dp)
      .height(308.dp)
      .shadow(24.dp, cardShape, spotColor = moodColor.copy(alpha = 0.45f))
      .clip(cardShape)
      .background(
        Brush.horizontalGradient(
          0.0f to lerpColor(Color(0xFF0A0E17), moodColor, 0.26f),
          0.45f to Color(0xFF0A0E16),
          1.0f to Color(0xFF06080D)
        )
      )
      .border(2.dp, moodColor.copy(alpha = 0.85f), cardShape)
  ) {
    // LEFT COLUMN: Slot-machine vertical poster reel
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .width(226.dp)
        .fillMaxHeight()
        .background(Color(0x9904060A))
        .clipToBounds()
    ) {
      val visibleIndices = ((currentSlotIdx - 1).coerceAtLeast(0))..
        ((currentSlotIdx + 1).coerceAtMost(currentStrip.lastIndex.coerceAtLeast(0)))

      for (idx in visibleIndices) {
        val item = currentStrip.getOrNull(idx) ?: continue
        val deltaPx = ((idx.toFloat() - scrollIndexAnim.value) * slotStridePx).roundToInt()
        val distanceFromCenter = kotlin.math.abs(idx.toFloat() - scrollIndexAnim.value)
        val itemAlpha = (1f - distanceFromCenter * 0.48f).coerceIn(0.25f, 1f)
        val itemScale = if (distanceFromCenter < 0.05f && stage == MoodDeckStage.COUNTDOWN) {
          landingPopScale.value
        } else {
          (1f - distanceFromCenter * 0.10f).coerceIn(0.84f, 1f)
        }

        MoodPosterThumb(
          title = item,
          assets = assets,
          borderColor = if (distanceFromCenter < 0.3f) moodColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.2f),
          modifier = Modifier
            .offset { IntOffset(0, deltaPx) }
            .graphicsLayer {
              scaleX = itemScale
              scaleY = itemScale
              alpha = itemAlpha
            }
            .width(144.dp)
            .height(slotItemHeightDp)
        )
      }

      // Top & bottom drum shading masks so neighbors disappear naturally behind slot hood
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .align(Alignment.TopCenter)
          .background(
            Brush.verticalGradient(listOf(Color(0xEE05070C), Color.Transparent))
          )
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE05070C)))
          )
      )

      // Glowing mood-coloured selector reticle framing center landing slot
      Box(
        modifier = Modifier
          .width(152.dp)
          .height(224.dp)
          .border(2.5.dp, moodColor.copy(alpha = if (stage == MoodDeckStage.COUNTDOWN) 1f else 0.72f), RoundedCornerShape(13.dp))
      )
    }

    // Subtle divider glow
    Box(
      modifier = Modifier
        .width(1.dp)
        .fillMaxHeight()
        .background(moodColor.copy(alpha = 0.28f))
    )

    // RIGHT COLUMN: Spinning live readout -> Countdown + mood-tinted progress bar
    Column(
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .padding(horizontal = 26.dp, vertical = 22.dp)
    ) {
      Column {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(moodColor.copy(alpha = 0.20f))
              .border(1.dp, moodColor.copy(alpha = 0.60f), RoundedCornerShape(6.dp))
              .padding(horizontal = 9.dp, vertical = 3.dp)
          ) {
            Text(
              text = moodCard.category.name.uppercase(),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.9.sp,
              color = moodColor
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0x26FFFFFF))
              .padding(horizontal = 9.dp, vertical = 3.dp)
          ) {
            Text(
              text = displayTitle.appName,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFFE2E8F0)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = displayTitle.title,
          fontSize = 25.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 30.sp
        )
      }

      // Middle / Bottom countdown or spinning status
      if (stage == MoodDeckStage.REEL_SPINNING) {
        Column {
          Text(
            text = "Picking something from ${moodCard.category.name}…",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xCCFFFFFF)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(Color(0x22FFFFFF))
          ) {
            val spinFrac = if (currentStrip.size > 2) {
              (scrollIndexAnim.value / (currentStrip.size - 2)).coerceIn(0f, 1f)
            } else 0.5f
            Box(
              modifier = Modifier
                .fillMaxWidth(spinFrac)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(moodColor)
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          DeckHintChip(
            label = "← / Back  Cancel",
            borderColor = Color.White.copy(alpha = 0.20f),
            textColor = Color(0xFFCBD5E1)
          )
        }
      } else {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Text(
              text = "Opening in $remainingSeconds…",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = moodColor
            )
            Text(
              text = displayTitle.appName,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0x99FFFFFF)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Mood-coloured countdown progress bar
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(Color(0x28FFFFFF))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(countdownProgressAnim.value.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(
                  Brush.horizontalGradient(
                    listOf(moodColor.copy(alpha = 0.85f), moodColor)
                  )
                )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            DeckHintChip(label = "OK Open now", borderColor = moodColor.copy(alpha = 0.65f), textColor = Color.White)
            DeckHintChip(label = "→ Spin again", borderColor = Color.White.copy(alpha = 0.22f), textColor = Color(0xFFE2E8F0))
            DeckHintChip(label = "← Cancel", borderColor = Color.White.copy(alpha = 0.16f), textColor = Color(0xFFB8C0CC))
          }
        }
      }
    }
  }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
  val f = fraction.coerceIn(0f, 1f)
  return Color(
    red = start.red + (end.red - start.red) * f,
    green = start.green + (end.green - start.green) * f,
    blue = start.blue + (end.blue - start.blue) * f,
    alpha = start.alpha + (end.alpha - start.alpha) * f
  )
}
