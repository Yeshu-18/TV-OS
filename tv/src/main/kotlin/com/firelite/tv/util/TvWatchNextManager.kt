package com.firelite.tv.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads Android TV & Fire OS native Watch Next rows ("Continue Watching").
 * Apps such as Netflix, Prime Video, JioHotstar, and YouTube publish resume points,
 * episode details, poster art, and direct playback URIs to content://android.media.tv/watch_next_program.
 */
object TvWatchNextManager {
  private const val TAG = "TvWatchNextManager"
  private val WATCH_NEXT_URI = Uri.parse("content://android.media.tv/watch_next_program")

  private val PROJECTION = arrayOf(
    "_id",
    "package_name",
    "title",
    "episode_title",
    "season_display_number",
    "episode_display_number",
    "poster_art_uri",
    "intent_uri",
    "last_playback_position_millis",
    "duration_millis",
    "last_engagement_time_utc_millis"
  )

  suspend fun loadWatchNextItems(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaItem>()
    try {
      val cr = context.contentResolver
      cr.query(WATCH_NEXT_URI, PROJECTION, null, null, "last_engagement_time_utc_millis DESC")?.use { cursor ->
        val idIdx = cursor.getColumnIndex("_id")
        val pkgIdx = cursor.getColumnIndex("package_name")
        val titleIdx = cursor.getColumnIndex("title")
        val epTitleIdx = cursor.getColumnIndex("episode_title")
        val seasonIdx = cursor.getColumnIndex("season_display_number")
        val epIdx = cursor.getColumnIndex("episode_display_number")
        val posterIdx = cursor.getColumnIndex("poster_art_uri")
        val intentIdx = cursor.getColumnIndex("intent_uri")
        val posIdx = cursor.getColumnIndex("last_playback_position_millis")
        val durIdx = cursor.getColumnIndex("duration_millis")

        while (cursor.moveToNext()) {
          val rowId = if (idIdx >= 0) cursor.getString(idIdx) else ""
          val pkg = if (pkgIdx >= 0) cursor.getString(pkgIdx) else ""
          val title = if (titleIdx >= 0) cursor.getString(titleIdx) else ""
          if (title.isNullOrBlank()) continue

          val epTitle = if (epTitleIdx >= 0) cursor.getString(epTitleIdx) else null
          val season = if (seasonIdx >= 0) cursor.getString(seasonIdx) else null
          val episode = if (epIdx >= 0) cursor.getString(epIdx) else null
          val poster = if (posterIdx >= 0) cursor.getString(posterIdx) else null
          val intentUri = if (intentIdx >= 0) cursor.getString(intentIdx) else null
          val pos = if (posIdx >= 0) cursor.getLong(posIdx) else -1L
          val dur = if (durIdx >= 0) cursor.getLong(durIdx) else -1L

          val progress = if (dur > 0 && pos in 0..dur) {
            (pos.toFloat() / dur.toFloat()).coerceIn(0.05f, 0.95f)
          } else null

          val (platformName, accentColor) = resolvePlatformAndColor(pkg)

          val episodeInfo = buildString {
            if (!season.isNullOrBlank()) append("S$season ")
            if (!episode.isNullOrBlank()) append("E$episode ")
            if (!epTitle.isNullOrBlank()) {
              if (isNotEmpty()) append("• ")
              append(epTitle)
            }
          }.trim()

          items.add(
            MediaItem(
              id = "wn_${rowId.ifEmpty { items.size.toString() }}",
              title = title,
              category = Category.SERIES,
              platform = platformName,
              subtitle = episodeInfo.ifEmpty { "Continue Watching" },
              durationOrEpisodes = if (!season.isNullOrBlank() && !episode.isNullOrBlank()) "S$season E$episode" else (epTitle ?: "Resume"),
              badge = "RESUME",
              rating = "TV-14",
              accentColorHex = accentColor,
              contentUrl = intentUri,
              deepLinkPackage = pkg.ifEmpty { null },
              posterUrl = poster,
              playbackProgress = progress,
              episodeTitle = epTitle
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.d(TAG, "Watch Next programs not accessible: ${e.message}")
    }
    items
  }

  /**
   * Couch-TV inspired domain-to-package resolver for streaming platforms.
   */
  fun packageForUrl(url: String): String? {
    val host = runCatching { Uri.parse(url).host }.getOrNull()?.lowercase()?.removePrefix("www.") ?: return null
    return when {
      host.contains("hotstar.com") -> "in.startv.hotstar"
      host.contains("netflix.com") -> "com.netflix.ninja"
      host.contains("primevideo.com") -> "com.amazon.firebat"
      host.contains("youtube.com") || host.contains("youtu.be") -> "com.amazon.firetv.youtube"
      host.contains("sunnxt.com") -> "com.suntv.sunnxt"
      host.contains("zee5.com") -> "com.graymatrix.did"
      else -> null
    }
  }

  private fun resolvePlatformAndColor(pkg: String): Pair<String, Long> {
    return when {
      pkg.contains("hotstar") -> "JioHotstar" to 0xFF00A3FF
      pkg.contains("netflix") -> "Netflix" to 0xFFE50914
      pkg.contains("firebat") || pkg.contains("amazonvideo") -> "Prime Video" to 0xFF00A8E1
      pkg.contains("youtube") -> "YouTube" to 0xFFFF0000
      pkg.contains("sunnxt") -> "Sun NXT" to 0xFFFF6F00
      pkg.contains("jiotv") -> "JioTV+" to 0xFF0055FF
      pkg.contains("zee5") -> "ZEE5" to 0xFF8B5CF6
      else -> "Streaming" to 0xFF38BDF8
    }
  }
}
