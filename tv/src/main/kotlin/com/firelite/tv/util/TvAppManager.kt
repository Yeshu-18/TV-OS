package com.firelite.tv.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class TvAppItem(
  val id: String,
  val name: String,
  val packageName: String?,
  val iconResName: String? = null,
  val brandColor: Color,
  val tagline: String,
  val isSystemAction: Boolean = false,
  val systemActionType: String? = null
)

object TvAppManager {

  private val BRAND_CONFIGS = mapOf(
    "in.startv.hotstar" to Pair(Color(0xFF0F2C6B), "Live sports, blockbuster movies & shows."),
    "com.netflix.ninja" to Pair(Color(0xFF6B0E17), "See what's next. Unlimited movies & TV."),
    "com.amazon.firebat" to Pair(Color(0xFF0B3F6E), "Welcome to Prime Video. Original series & films."),
    "com.amazon.firetv.youtube" to Pair(Color(0xFF6B1414), "Watch what the world is watching."),
    "com.zee5.amazon" to Pair(Color(0xFF421570), "Entertainment in your language."),
    "com.suntv.sunnxt" to Pair(Color(0xFF661020), "The heart of South Indian entertainment."),
    "com.jio.media.stb.ondemand" to Pair(Color(0xFF5E0B3E), "Spotlight cinema and on-demand entertainment."),
    "com.jio.media.jiotvplus" to Pair(Color(0xFF1E2B5E), "Live television and premium channels.")
  )

  private val BLOCKED_PACKAGES = setOf(
    "com.amazon.minitv.android.app",
    "com.amazon.ftvads.deeplinking",
    "com.amazon.adep",
    "com.amazon.sneakpeek",
    "com.amazon.cloud9",
    "com.amazon.cardinal",
    "com.amazon.tmm.tutorial",
    "com.amazon.storm.lightning.tutorial",
    "com.amazon.gamehub",
    "com.amazon.tv.livetv",
    "com.amazon.shoptv.client",
    "com.amazon.shoptv.firetv.client"
  )

  /** Default fast lineup so the screen renders instantaneously on frame 0 */
  fun getInstantFallbackApps(): List<TvAppItem> = listOf(
    TvAppItem("in.startv.hotstar", "JioHotstar", "in.startv.hotstar", "app_jiohotstar", BRAND_CONFIGS["in.startv.hotstar"]!!.first, BRAND_CONFIGS["in.startv.hotstar"]!!.second),
    TvAppItem("com.netflix.ninja", "Netflix", "com.netflix.ninja", "app_netflix", BRAND_CONFIGS["com.netflix.ninja"]!!.first, BRAND_CONFIGS["com.netflix.ninja"]!!.second),
    TvAppItem("com.amazon.firebat", "Prime Video", "com.amazon.firebat", "app_primevideo", BRAND_CONFIGS["com.amazon.firebat"]!!.first, BRAND_CONFIGS["com.amazon.firebat"]!!.second),
    TvAppItem("com.amazon.firetv.youtube", "YouTube", "com.amazon.firetv.youtube", "app_youtube", BRAND_CONFIGS["com.amazon.firetv.youtube"]!!.first, BRAND_CONFIGS["com.amazon.firetv.youtube"]!!.second),
    TvAppItem("com.zee5.amazon", "ZEE5", "com.zee5.amazon", null, BRAND_CONFIGS["com.zee5.amazon"]!!.first, BRAND_CONFIGS["com.zee5.amazon"]!!.second),
    TvAppItem("com.suntv.sunnxt", "Sun NXT", "com.suntv.sunnxt", null, BRAND_CONFIGS["com.suntv.sunnxt"]!!.first, BRAND_CONFIGS["com.suntv.sunnxt"]!!.second),
    TvAppItem("action_mirror", "Mirroring", null, null, Color(0xFF1C2240), "Cast phone, tablet or PC to TV.", true, "SCREEN_MIRROR"),
    TvAppItem("action_settings", "Settings", null, null, Color(0xFF10283B), "Configure network, display and Fire TV.", true, "SETTINGS")
  )

  /** High-speed parallel resolver running on background threads */
  suspend fun resolveInstalledAppsParallel(context: Context): List<TvAppItem> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val myPkg = context.packageName

    val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    val queryResults = try {
      pm.queryIntentActivities(leanbackIntent, 0)
    } catch (_: Exception) {
      emptyList()
    }

    val installedPackages = queryResults
      .map { it.activityInfo.packageName }
      .filter { it != myPkg && !BLOCKED_PACKAGES.contains(it) }
      .toSet()

    val priorityOrder = listOf(
      "in.startv.hotstar" to ("JioHotstar" to "app_jiohotstar"),
      "com.netflix.ninja" to ("Netflix" to "app_netflix"),
      "com.amazon.firebat" to ("Prime Video" to "app_primevideo"),
      "com.amazon.firetv.youtube" to ("YouTube" to "app_youtube"),
      "com.zee5.amazon" to ("ZEE5" to null),
      "com.suntv.sunnxt" to ("Sun NXT" to null),
      "com.jio.media.stb.ondemand" to ("JioCinema" to null),
      "com.jio.media.jiotvplus" to ("JioTV+" to null)
    )

    val resultList = mutableListOf<TvAppItem>()

    // 1. Add priority streaming apps
    for ((pkg, info) in priorityOrder) {
      if (installedPackages.contains(pkg) || pkg == "in.startv.hotstar" || pkg == "com.netflix.ninja") {
        val (color, tagline) = BRAND_CONFIGS[pkg] ?: Pair(Color(0xFF122238), "Stream your favorite titles.")
        resultList.add(
          TvAppItem(
            id = pkg,
            name = info.first,
            packageName = pkg,
            iconResName = info.second,
            brandColor = color,
            tagline = tagline
          )
        )
      }
    }

    // 2. Add other installed leanback apps
    val addedPackages = resultList.mapNotNull { it.packageName }.toSet()
    for (ri in queryResults) {
      val pkg = ri.activityInfo.packageName
      if (pkg != myPkg && !addedPackages.contains(pkg) && !BLOCKED_PACKAGES.contains(pkg)) {
        val label = ri.loadLabel(pm).toString()
        val (color, tagline) = BRAND_CONFIGS[pkg] ?: Pair(Color(0xFF122030), "Installed application.")
        resultList.add(
          TvAppItem(
            id = pkg,
            name = label,
            packageName = pkg,
            brandColor = color,
            tagline = tagline
          )
        )
      }
    }

    // 3. System actions
    resultList.add(
      TvAppItem(
        id = "action_mirror",
        name = "Mirroring",
        packageName = null,
        brandColor = Color(0xFF1C2240),
        tagline = "Cast phone, tablet or PC to TV.",
        isSystemAction = true,
        systemActionType = "SCREEN_MIRROR"
      )
    )
    resultList.add(
      TvAppItem(
        id = "action_settings",
        name = "Settings",
        packageName = null,
        brandColor = Color(0xFF10283B),
        tagline = "Configure network, display and Fire TV.",
        isSystemAction = true,
        systemActionType = "SETTINGS"
      )
    )

    // Pre-warm icons in parallel using coroutine async/awaitAll
    coroutineScope {
      resultList.map { item ->
        async {
          item.packageName?.let { pkg ->
            AppIconLoader.getAppIcon(context, pkg)
          }
        }
      }.awaitAll()
    }

    resultList
  }
}

