package com.firelite.tv.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.firelite.core.data.HotstarDeepLinks
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Thread-safe LRU cache for up to [maxSize] recently watched shows/movies.
 * Backed by an access-ordered [LinkedHashMap] so most-recently-watched items stay at the
 * front when exported via [snapshotRecentFirst] and the least-recently-watched item is
 * evicted once more than [maxSize] distinct items are recorded.
 */
class RecentlyWatchedLruCache(
  val maxSize: Int = TvWatchNextManager.MAX_RECENT_ITEMS
) {
  data class Entry(
    val item: MediaItem,
    val lastWatchedAtMillis: Long = 0L
  )

  private val cache = object : LinkedHashMap<String, Entry>(maxSize + 1, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean {
      return size > maxSize
    }
  }

  @Synchronized
  fun put(item: MediaItem, lastWatchedAtMillis: Long = System.currentTimeMillis()): MediaItem {
    val normalized = HotstarDeepLinks.normalizeMediaItem(item, preferWatch = true)
    val key = canonicalKey(normalized)
    val matchingKey = findMatchingKeyLocked(normalized, key)
    val previous = matchingKey?.let { cache.remove(it) }
    val merged = mergeItem(incoming = normalized, previous = previous?.item)
    val resolvedTimestamp = maxOf(lastWatchedAtMillis, previous?.lastWatchedAtMillis ?: 0L)
    cache[key] = Entry(item = merged, lastWatchedAtMillis = resolvedTimestamp)
    return merged
  }

  @Synchronized
  fun restoreEntriesRecentFirst(entries: List<Entry>) {
    cache.clear()
    // Oldest/LRU is inserted first so newest/MRU lands at the LinkedHashMap tail.
    for (entry in entries.take(maxSize).asReversed()) {
      putInternalLocked(entry.item, entry.lastWatchedAtMillis)
    }
  }

  @Synchronized
  fun snapshotEntriesRecentFirst(): List<Entry> = cache.values.toList().asReversed()

  @Synchronized
  fun snapshotRecentFirst(): List<MediaItem> = cache.values.map { it.item }.asReversed()

  @Synchronized
  fun size(): Int = cache.size

  @Synchronized
  fun clear() {
    cache.clear()
  }

  private fun putInternalLocked(item: MediaItem, lastWatchedAtMillis: Long) {
    val normalized = HotstarDeepLinks.normalizeMediaItem(item, preferWatch = true)
    val key = canonicalKey(normalized)
    val matchingKey = findMatchingKeyLocked(normalized, key)
    val previous = matchingKey?.let { cache.remove(it) }
    val merged = mergeItem(incoming = normalized, previous = previous?.item)
    cache[key] = Entry(item = merged, lastWatchedAtMillis = lastWatchedAtMillis)
  }

  private fun findMatchingKeyLocked(candidate: MediaItem, preferredKey: String): String? {
    if (cache.containsKey(preferredKey)) return preferredKey
    return cache.entries.firstOrNull { (_, entry) ->
      val existing = entry.item
      (!candidate.contentId.isNullOrBlank() && candidate.contentId == existing.contentId &&
        candidate.platform.equals(existing.platform, ignoreCase = true)) ||
        (!candidate.contentUrl.isNullOrBlank() &&
          candidate.contentUrl.equals(existing.contentUrl, ignoreCase = true)) ||
        (candidate.title.equals(existing.title, ignoreCase = true) &&
          candidate.platform.equals(existing.platform, ignoreCase = true))
    }?.key
  }

  private fun mergeItem(incoming: MediaItem, previous: MediaItem?): MediaItem {
    if (previous == null) return incoming
    return incoming.copy(
      contentId = incoming.contentId ?: previous.contentId,
      contentUrl = incoming.contentUrl ?: previous.contentUrl,
      fallbackUrl = incoming.fallbackUrl ?: previous.fallbackUrl,
      deepLinkPackage = incoming.deepLinkPackage ?: previous.deepLinkPackage,
      artworkResName = incoming.artworkResName ?: previous.artworkResName,
      bannerResName = incoming.bannerResName ?: previous.bannerResName,
      characterResName = incoming.characterResName ?: previous.characterResName,
      posterUrl = incoming.posterUrl ?: previous.posterUrl,
      playbackProgress = incoming.playbackProgress ?: previous.playbackProgress,
      episodeTitle = incoming.episodeTitle ?: previous.episodeTitle
    )
  }

  internal fun canonicalKey(normalized: MediaItem): String {
    val cid = normalized.contentId?.trim()?.takeIf { it.isNotEmpty() }
    if (cid != null) return "cid:${normalized.platform.trim().lowercase()}:$cid"
    val url = normalized.contentUrl?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    if (url != null) return "url:$url"
    return "title:${normalized.platform.trim().lowercase()}:${normalized.title.trim().lowercase()}"
  }
}

/**
 * Reads Android TV / Fire OS native Watch Next rows and manages FireLite's
 * persistent 10-item LRU recently watched history without synthetic/pinned overrides.
 */
object TvWatchNextManager {
  const val MAX_RECENT_ITEMS = 10
  private const val TAG = "TvWatchNextManager"
  private const val PREFS_NAME = "firelite_continue_watching"
  private const val KEY_RECENT_LRU_ITEMS = "recently_watched_lru_v1"
  private val lruCache = RecentlyWatchedLruCache(maxSize = MAX_RECENT_ITEMS)

  private val WATCH_NEXT_URI: Uri by lazy {
    Uri.parse("content://android.media.tv/watch_next_program")
  }

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

  /**
   * Loads genuine recently watched items (up to [MAX_RECENT_ITEMS]) combining:
   * 1. FireLite's persisted LRU recently-watched cache
   * 2. Any system-level Watch Next rows from content://android.media.tv/watch_next_program
   */
  suspend fun loadWatchNextItems(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
    val localEntries = loadLocalHistoryEntries(context)
    val osEntries = queryOsWatchNextEntries(context)

    val mergedEntries: List<RecentlyWatchedLruCache.Entry> = if (osEntries.isEmpty()) {
      localEntries
    } else {
      val combined = RecentlyWatchedLruCache(MAX_RECENT_ITEMS)
      // Insert older items first so newer timestamps end up at the MRU tail; for ties, preserve original MRU order with local items taking priority.
      val allChronological = (osEntries.asReversed() + localEntries.asReversed()).sortedWith(
        compareBy<RecentlyWatchedLruCache.Entry> { it.lastWatchedAtMillis }
      )
      for (entry in allChronological) {
        combined.put(entry.item, entry.lastWatchedAtMillis)
      }
      combined.snapshotEntriesRecentFirst()
    }

    lruCache.restoreEntriesRecentFirst(mergedEntries)
    val snapshot = lruCache.snapshotRecentFirst()
    if (mergedEntries.map { it.item } != localEntries.map { it.item }) {
      saveLocalHistoryEntries(context, lruCache.snapshotEntriesRecentFirst())
    }
    snapshot
  }

  fun getRecentlyWatchedSnapshot(): List<MediaItem> = lruCache.snapshotRecentFirst()

  private fun queryOsWatchNextEntries(context: Context): List<RecentlyWatchedLruCache.Entry> {
    val osEntries = mutableListOf<RecentlyWatchedLruCache.Entry>()
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
        val engagementIdx = cursor.getColumnIndex("last_engagement_time_utc_millis")

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
          val engagementMillis = if (engagementIdx >= 0) cursor.getLong(engagementIdx) else 0L

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

          val itemId = "wn_${rowId.ifEmpty { osEntries.size.toString() }}"
          osEntries.add(
            RecentlyWatchedLruCache.Entry(
              item = MediaItem(
                id = itemId,
                title = title,
                category = Category.SERIES,
                platform = platformName,
                subtitle = episodeInfo.ifEmpty { "Recently Watched" },
                durationOrEpisodes = if (!season.isNullOrBlank() && !episode.isNullOrBlank()) "S$season E$episode" else (epTitle ?: "Resume"),
                badge = "RESUME",
                rating = "TV-14",
                accentColorHex = accentColor,
                contentUrl = if (pkg == HotstarDeepLinks.PACKAGE_NAME) {
                  HotstarDeepLinks.normalizeUrl(intentUri, preferWatch = true)
                } else {
                  intentUri
                },
                deepLinkPackage = pkg.ifEmpty { null },
                posterUrl = poster,
                playbackProgress = progress,
                episodeTitle = epTitle
              ),
              lastWatchedAtMillis = engagementMillis.coerceAtLeast(0L)
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.d(TAG, "Watch Next programs not accessible: ${e.message}")
    }
    return osEntries
  }

  /**
   * Records playback when user launches any title from FireLite using the 10-item LRU cache.
   * Promotes the item to the front (Most Recently Watched) with `/watch` resume URI and persists to disk.
   */
  fun recordWatch(context: Context, item: MediaItem, progress: Float? = null): MediaItem {
    if (lruCache.size() == 0) {
      val persisted = loadLocalHistoryEntries(context)
      if (persisted.isNotEmpty()) {
        lruCache.restoreEntriesRecentFirst(persisted)
      }
    }
    val normalized = HotstarDeepLinks.normalizeMediaItem(item, preferWatch = true)
    val stableId = if (normalized.id.startsWith("cw_")) normalized.id else "cw_${normalized.id}"
    val resolvedProgress = (progress ?: normalized.playbackProgress ?: 0.20f).coerceIn(0.05f, 0.95f)
    val prepared = normalized.copy(
      id = stableId,
      playbackProgress = resolvedProgress,
      badge = "RESUME"
    )
    val stored = lruCache.put(prepared, lastWatchedAtMillis = System.currentTimeMillis())
    saveLocalHistoryEntries(context, lruCache.snapshotEntriesRecentFirst())
    return stored
  }

  private fun loadLocalHistoryEntries(context: Context): List<RecentlyWatchedLruCache.Entry> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_RECENT_LRU_ITEMS, null) ?: return emptyList()
    val list = mutableListOf<RecentlyWatchedLruCache.Entry>()
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val item = MediaItem(
          id = obj.getString("id"),
          title = obj.getString("title"),
          category = Category.valueOf(obj.optString("category", "SERIES")),
          platform = obj.optString("platform", "JioHotstar"),
          subtitle = obj.optString("subtitle", "Recently Watched"),
          durationOrEpisodes = obj.optString("durationOrEpisodes", "Resume"),
          badge = obj.optString("badge", "RESUME"),
          rating = obj.optString("rating", "TV-14"),
          accentColorHex = obj.optLong("accentColorHex", 0xFF00A3FF),
          contentId = if (obj.has("contentId")) obj.getString("contentId") else null,
          contentUrl = if (obj.has("contentUrl")) obj.getString("contentUrl") else null,
          fallbackUrl = if (obj.has("fallbackUrl")) obj.getString("fallbackUrl") else null,
          deepLinkPackage = if (obj.has("deepLinkPackage")) obj.getString("deepLinkPackage") else null,
          artworkResName = if (obj.has("artworkResName")) obj.getString("artworkResName") else null,
          characterResName = if (obj.has("characterResName")) obj.getString("characterResName") else null,
          posterUrl = if (obj.has("posterUrl")) obj.getString("posterUrl") else null,
          playbackProgress = if (obj.has("playbackProgress")) obj.getDouble("playbackProgress").toFloat() else null,
          episodeTitle = if (obj.has("episodeTitle")) obj.getString("episodeTitle") else null
        )
        val ts = obj.optLong("lastWatchedAtMillis", 0L)
        list.add(
          RecentlyWatchedLruCache.Entry(
            item = HotstarDeepLinks.normalizeMediaItem(item, preferWatch = true),
            lastWatchedAtMillis = ts
          )
        )
      }
    } catch (_: Exception) {}
    val capped = list.take(MAX_RECENT_ITEMS)
    if (capped != list) {
      saveLocalHistoryEntries(context, capped)
    }
    return capped
  }

  private fun saveLocalHistoryEntries(context: Context, entries: List<RecentlyWatchedLruCache.Entry>) {
    try {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val arr = JSONArray()
      for (entry in entries.take(MAX_RECENT_ITEMS)) {
        val item = entry.item
        val obj = JSONObject().apply {
          put("id", item.id)
          put("title", item.title)
          put("category", item.category.name)
          put("platform", item.platform)
          put("subtitle", item.subtitle)
          put("durationOrEpisodes", item.durationOrEpisodes)
          put("badge", item.badge)
          put("rating", item.rating)
          put("accentColorHex", item.accentColorHex)
          put("lastWatchedAtMillis", entry.lastWatchedAtMillis)
          item.contentId?.let { put("contentId", it) }
          item.contentUrl?.let { put("contentUrl", it) }
          item.fallbackUrl?.let { put("fallbackUrl", it) }
          item.deepLinkPackage?.let { put("deepLinkPackage", it) }
          item.artworkResName?.let { put("artworkResName", it) }
          item.characterResName?.let { put("characterResName", it) }
          item.posterUrl?.let { put("posterUrl", it) }
          item.playbackProgress?.let { put("playbackProgress", it.toDouble()) }
          item.episodeTitle?.let { put("episodeTitle", it) }
        }
        arr.put(obj)
      }
      prefs.edit().putString(KEY_RECENT_LRU_ITEMS, arr.toString()).apply()
    } catch (_: Exception) {}
  }

  /**
   * Initial high-quality verified JioHotstar & streaming titles matching user's actual watched content.
   */
  fun getInitialContinueWatchingItems(): List<MediaItem> = listOf(
    MediaItem(
      id = "cw_im_game",
      title = "I'm Game",
      category = Category.SERIES,
      platform = "JioHotstar",
      subtitle = "Action Thriller • Tamil • 35m left",
      durationOrEpisodes = "35m left",
      badge = "CONTINUE",
      rating = "UA 16+",
      accentColorHex = 0xFF00A3FF,
      contentId = HotstarDeepLinks.IM_GAME_CONTENT_ID,
      contentUrl = HotstarDeepLinks.IM_GAME_WATCH_URI,
      fallbackUrl = "https://www.hotstar.com/in/explore?search_query=I%27m+Game",
      deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
      artworkResName = "art_im_game",
      playbackProgress = 0.74f,
      episodeTitle = "35m left"
    ),
    MediaItem(
      id = "cw_bittu_bahanebaaz",
      title = "Bittu Bahanebaaz",
      category = Category.KIDS,
      platform = "JioHotstar",
      subtitle = "Ek Ka Teen • Tamil • 35m left",
      durationOrEpisodes = "35m left",
      badge = "CONTINUE",
      rating = "TV-Y7",
      accentColorHex = 0xFF00A3FF,
      contentId = HotstarDeepLinks.BITTU_EK_KA_TEEN_CONTENT_ID,
      contentUrl = HotstarDeepLinks.BITTU_EK_KA_TEEN_WATCH_URI,
      fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Bittu+Bahanebaaz",
      deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
      artworkResName = "art_bittu_bahanebaaz",
      playbackProgress = 0.60f,
      episodeTitle = "Ek Ka Teen"
    ),
    MediaItem(
      id = "cw_heart_beat",
      title = "Heart Beat",
      category = Category.SERIES,
      platform = "JioHotstar",
      subtitle = "S2 E1 • The Emergency Ward",
      durationOrEpisodes = "S2 E1",
      badge = "RESUME",
      rating = "TV-14",
      accentColorHex = 0xFF00A3FF,
      contentId = HotstarDeepLinks.HEART_BEAT_CONTENT_ID,
      contentUrl = HotstarDeepLinks.showWatchUri("heart-beat", HotstarDeepLinks.HEART_BEAT_CONTENT_ID),
      fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Heart+Beat",
      deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
      artworkResName = "art_heart_beat",
      playbackProgress = 0.65f,
      episodeTitle = "The Emergency Ward"
    ),
    MediaItem(
      id = "cw_the_court",
      title = "The Court",
      category = Category.SERIES,
      platform = "JioHotstar",
      subtitle = "S1 E3 • Cross Examination",
      durationOrEpisodes = "S1 E3",
      badge = "RESUME",
      rating = "TV-MA",
      accentColorHex = 0xFF00A3FF,
      contentId = HotstarDeepLinks.THE_COURT_CONTENT_ID,
      contentUrl = HotstarDeepLinks.showWatchUri("the-court", HotstarDeepLinks.THE_COURT_CONTENT_ID),
      fallbackUrl = "https://www.hotstar.com/in/explore?search_query=The+Court",
      deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
      artworkResName = "art_the_court",
      playbackProgress = 0.42f,
      episodeTitle = "Cross Examination"
    ),
    MediaItem(
      id = "cw_doraemon",
      title = "Doraemon",
      category = Category.KIDS,
      platform = "JioHotstar",
      subtitle = "S12 E8 • Anywhere Door Chaos",
      durationOrEpisodes = "S12 E8",
      badge = "RESUME",
      rating = "TV-Y7",
      accentColorHex = 0xFF00A3FF,
      contentId = HotstarDeepLinks.DORAEMON_CONTENT_ID,
      contentUrl = HotstarDeepLinks.showWatchUri("doraemon", HotstarDeepLinks.DORAEMON_CONTENT_ID),
      fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Doraemon",
      deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
      artworkResName = "art_doraemon",
      characterResName = "art_doraemon_char",
      playbackProgress = 0.50f,
      episodeTitle = "Anywhere Door Chaos"
    )
  )

  /**
   * Couch-TV inspired scheme and domain-to-package resolver for streaming platforms.
   */
  fun packageForUrl(url: String): String? = HotstarDeepLinks.packageForUrl(url)

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

