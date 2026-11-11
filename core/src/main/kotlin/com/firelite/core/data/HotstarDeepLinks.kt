package com.firelite.core.data

import com.firelite.core.model.MediaItem
import java.net.URI

/**
 * Canonical JioHotstar Fire TV deep-link definitions, legacy URL/ID migrations,
 * and streaming package resolution.
 */
object HotstarDeepLinks {
  const val PACKAGE_NAME = "in.startv.hotstar"

  const val IM_GAME_CONTENT_ID = "1271720887"
  const val IM_GAME_DETAILS_URI = "hotstar://in/movies/im-game/1271720887"
  const val IM_GAME_WATCH_URI = "hotstar://in/movies/im-game/1271720887/watch"

  const val BITTU_EK_KA_TEEN_CONTENT_ID = "1271708139"
  const val BITTU_EK_KA_TEEN_WATCH_URI =
    "hotstar://in/movies/bittu-bahanebaaz-ek-ka-teen/1271708139/watch"

  const val NINJA_HATTORI_CONTENT_ID = "1971003458"
  const val NINJA_HATTORI_DETAILS_URI =
    "hotstar://in/shows/ninja-hattori-classic/1971003458"

  const val DORAEMON_CONTENT_ID = "1260021004"
  const val SHINCHAN_CONTENT_ID = "1260020929"
  const val MOTU_PATLU_CONTENT_ID = "1971003164"
  const val HEART_BEAT_CONTENT_ID = "1260168231"
  const val THE_COURT_CONTENT_ID = "1271672560"
  const val SIRAGADIKKA_AASAI_CONTENT_ID = "1260129356"
  const val AYYANAR_THUNAI_CONTENT_ID = "1271388570"

  private const val LEGACY_IM_GAME_ID = "1271354531"
  private const val LEGACY_NINJA_HATTORI_ID = "1260000045"
  private const val LEGACY_BITTU_ID = "bittu_bahanebaaz"

  private val TITLE_OR_EPISODE_WATCH_PATH = Regex("""^/(movies|shows)/.+/(\d+)$""")
  private val SCHEME_PREFIX = Regex("""^([a-zA-Z][a-zA-Z0-9+.-]*):""")
  private val HOTSTAR_HTTP_PREFIX =
    Regex("""^https?://(?:www\.)?hotstar\.com/""", RegexOption.IGNORE_CASE)

  fun showDetailsUri(slug: String, contentId: String): String =
    "hotstar://in/shows/$slug/$contentId"

  fun showWatchUri(slug: String, contentId: String): String =
    "hotstar://in/shows/$slug/$contentId/watch"

  /**
   * Normalizes JioHotstar web/legacy URLs to Fire TV `hotstar://in/...` deep links
   * because JioHotstar's MainActivity prepends `hotstar://` onto non-matching schemes.
   */
  fun normalizeUrl(url: String?, preferWatch: Boolean = false): String? {
    val trimmed = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (trimmed.startsWith("intent:", ignoreCase = true)) return trimmed

    if (trimmed.contains(LEGACY_IM_GAME_ID)) {
      return if (preferWatch) IM_GAME_WATCH_URI else IM_GAME_DETAILS_URI
    }
    if (trimmed.contains(LEGACY_NINJA_HATTORI_ID)) {
      return if (preferWatch) "$NINJA_HATTORI_DETAILS_URI/watch" else NINJA_HATTORI_DETAILS_URI
    }
    if (trimmed.contains("search_query=Bittu+Bahanebaaz", ignoreCase = true) ||
      trimmed.contains("search_query=Bittu%20Bahanebaaz", ignoreCase = true)
    ) {
      return BITTU_EK_KA_TEEN_WATCH_URI
    }

    var normalized = if (HOTSTAR_HTTP_PREFIX.containsMatchIn(trimmed)) {
      "hotstar://" + HOTSTAR_HTTP_PREFIX.replaceFirst(trimmed, "")
    } else {
      trimmed
    }

    if (!normalized.startsWith("hotstar://", ignoreCase = true)) {
      return normalized
    }

    val afterScheme = normalized.substring("hotstar://".length)
    normalized = when {
      afterScheme.startsWith("in/", ignoreCase = true) -> "hotstar://$afterScheme"
      afterScheme.startsWith("movies/", ignoreCase = true) ||
        afterScheme.startsWith("shows/", ignoreCase = true) ||
        afterScheme.startsWith("explore", ignoreCase = true) -> "hotstar://in/$afterScheme"
      else -> "hotstar://$afterScheme"
    }

    if (!preferWatch) return normalized

    val parsed = runCatching { URI(normalized) }.getOrNull() ?: return normalized
    val host = parsed.host?.lowercase()
    val path = parsed.path?.trimEnd('/') ?: return normalized
    if (host == "in" && TITLE_OR_EPISODE_WATCH_PATH.matches(path)) {
      val query = parsed.rawQuery?.let { "?$it" } ?: ""
      val fragment = parsed.rawFragment?.let { "#$it" } ?: ""
      return "hotstar://in$path/watch$query$fragment"
    }
    return normalized
  }

  /**
   * Migrates persisted or in-flight [MediaItem] instances to verified content IDs and
   * Fire TV `hotstar://in/...` deep links.
   */
  fun normalizeMediaItem(item: MediaItem, preferWatch: Boolean = false): MediaItem {
    val isBittu = item.id == "cw_bittu_bahanebaaz" || item.contentId == LEGACY_BITTU_ID
    val normalizedId = when {
      item.contentId == LEGACY_IM_GAME_ID || item.id == "im_game" || item.id == "cw_im_game" ->
        IM_GAME_CONTENT_ID
      item.contentId == LEGACY_NINJA_HATTORI_ID || item.id == "k3" ->
        NINJA_HATTORI_CONTENT_ID
      isBittu ->
        BITTU_EK_KA_TEEN_CONTENT_ID
      else -> item.contentId
    }

    val normalizedUrl = when {
      isBittu && (item.contentUrl.isNullOrBlank() || item.contentUrl.contains("explore?", ignoreCase = true)) ->
        BITTU_EK_KA_TEEN_WATCH_URI
      item.contentId == LEGACY_IM_GAME_ID ->
        if (preferWatch) IM_GAME_WATCH_URI else IM_GAME_DETAILS_URI
      item.contentId == LEGACY_NINJA_HATTORI_ID ->
        if (preferWatch) "$NINJA_HATTORI_DETAILS_URI/watch" else NINJA_HATTORI_DETAILS_URI
      else -> normalizeUrl(item.contentUrl, preferWatch = preferWatch)
    }

    val isHotstar = item.platform.equals("JioHotstar", ignoreCase = true) ||
      (normalizedUrl?.startsWith("hotstar://", ignoreCase = true) == true)
    val resolvedPackage = item.deepLinkPackage ?: if (isHotstar) PACKAGE_NAME else null

    return item.copy(
      contentId = normalizedId,
      contentUrl = normalizedUrl,
      deepLinkPackage = resolvedPackage
    )
  }

  /**
   * Resolves target TV app package from either deep-link scheme (`hotstar://`, `netflix://`,
   * `amzn://`, `youtube://`, `sunnxt://`) or HTTPS host.
   */
  fun packageForUrl(url: String): String? {
    val trimmed = url.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("intent:", ignoreCase = true)) return null
    val parsed = runCatching { URI(trimmed) }.getOrNull()
    val scheme = parsed?.scheme?.lowercase()
      ?: SCHEME_PREFIX.find(trimmed)?.groupValues?.getOrNull(1)?.lowercase()
    val host = parsed?.host?.lowercase()?.removePrefix("www.")
    return when {
      scheme == "hotstar" || host?.contains("hotstar.com") == true -> PACKAGE_NAME
      scheme == "netflix" || host?.contains("netflix.com") == true -> "com.netflix.ninja"
      scheme == "amzn" || host?.contains("primevideo.com") == true -> "com.amazon.firebat"
      scheme == "youtube" || host?.contains("youtube.com") == true || host?.contains("youtu.be") == true ->
        "com.amazon.firetv.youtube"
      scheme == "sunnxt" || host?.contains("sunnxt.com") == true -> "com.suntv.sunnxt"
      host?.contains("zee5.com") == true -> "com.graymatrix.did"
      else -> null
    }
  }
}

