package com.firelite.core.data

import android.content.Context
import android.content.SharedPreferences
import com.firelite.core.model.AppDockItem
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.core.model.WeatherAlertInfo
import com.firelite.core.model.WhatsNewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FireLiteRepository(private val context: Context? = null) {

  private val json = Json { ignoreUnknownKeys = true }
  private val scope = CoroutineScope(Dispatchers.IO)
  private val prefs: SharedPreferences? = context?.getSharedPreferences("firelite_pins_store", Context.MODE_PRIVATE)

  private val _kidsItems = MutableStateFlow<List<MediaItem>>(loadSavedPins(Category.KIDS) ?: initialKidsItems())
  val kidsItems: StateFlow<List<MediaItem>> = _kidsItems.asStateFlow()

  private val _seriesItems = MutableStateFlow<List<MediaItem>>(loadSavedPins(Category.SERIES) ?: initialSeriesItems())
  val seriesItems: StateFlow<List<MediaItem>> = _seriesItems.asStateFlow()

  private val _whatsNew = MutableStateFlow<List<WhatsNewItem>>(initialWhatsNew())
  val whatsNew: StateFlow<List<WhatsNewItem>> = _whatsNew.asStateFlow()

  private val _weather = MutableStateFlow(WeatherAlertInfo())
  val weather: StateFlow<WeatherAlertInfo> = _weather.asStateFlow()

  private val _dockItems = MutableStateFlow<List<AppDockItem>>(initialDockItems())
  val dockItems: StateFlow<List<AppDockItem>> = _dockItems.asStateFlow()

  // Currently focused media item (for dynamic accent derivation on phone remote)
  private val _activeContent = MutableStateFlow<MediaItem?>(_kidsItems.value.firstOrNull())
  val activeContent: StateFlow<MediaItem?> = _activeContent.asStateFlow()

  fun setActiveContent(item: MediaItem) {
    _activeContent.value = item
  }

  fun addPinnedItem(item: MediaItem) {
    when (item.category) {
      Category.KIDS -> _kidsItems.update { current ->
        val updated = if (current.any { it.id == item.id }) current else current + item
        persistPins(Category.KIDS, updated)
        updated
      }
      Category.SERIES -> _seriesItems.update { current ->
        val updated = if (current.any { it.id == item.id }) current else current + item
        persistPins(Category.SERIES, updated)
        updated
      }
    }
  }

  fun removePinnedItem(itemId: String) {
    _kidsItems.update { current ->
      val updated = current.filterNot { item -> item.id == itemId }
      persistPins(Category.KIDS, updated)
      updated
    }
    _seriesItems.update { current ->
      val updated = current.filterNot { item -> item.id == itemId }
      persistPins(Category.SERIES, updated)
      updated
    }
  }

  fun moveCategory(itemId: String, targetCategory: Category) {
    val existingInKids = _kidsItems.value.find { it.id == itemId }
    val existingInSeries = _seriesItems.value.find { it.id == itemId }
    val itemToMove = existingInKids ?: existingInSeries ?: return

    val updatedItem = itemToMove.copy(category = targetCategory)
    removePinnedItem(itemId)
    addPinnedItem(updatedItem)
  }

  fun reorderPins(category: Category, fromIndex: Int, toIndex: Int) {
    when (category) {
      Category.KIDS -> _kidsItems.update { list ->
        val updated = reorderList(list, fromIndex, toIndex)
        persistPins(Category.KIDS, updated)
        updated
      }
      Category.SERIES -> _seriesItems.update { list ->
        val updated = reorderList(list, fromIndex, toIndex)
        persistPins(Category.SERIES, updated)
        updated
      }
    }
  }

  private fun reorderList(list: List<MediaItem>, fromIndex: Int, toIndex: Int): List<MediaItem> {
    if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
      return list
    }
    val mutable = list.toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
  }

  private fun loadSavedPins(category: Category): List<MediaItem>? {
    val p = prefs ?: return null
    val key = if (category == Category.KIDS) "pins_kids_v1" else "pins_series_v1"
    val rawJson = p.getString(key, null) ?: return null
    return try {
      val decoded = json.decodeFromString<List<MediaItem>>(rawJson)
      val migrated = decoded.map { HotstarDeepLinks.normalizeMediaItem(it, preferWatch = false) }
      if (migrated != decoded) {
        persistPins(category, migrated)
      }
      migrated
    } catch (_: Exception) {
      null
    }
  }

  private fun persistPins(category: Category, items: List<MediaItem>) {
    val p = prefs ?: return
    scope.launch {
      try {
        val key = if (category == Category.KIDS) "pins_kids_v1" else "pins_series_v1"
        val rawJson = json.encodeToString(items)
        p.edit().putString(key, rawJson).apply()
      } catch (_: Exception) {}
    }
  }

  companion object {
    fun initialKidsItems(): List<MediaItem> = listOf(
      MediaItem(
        id = "doraemon",
        title = "Doraemon",
        category = Category.KIDS,
        platform = "JioHotstar",
        subtitle = "Gadgets from the Future",
        durationOrEpisodes = "Season 12",
        badge = "POPULAR",
        rating = "TV-Y7",
        accentColorHex = 0xFF00A3FF,
        contentId = HotstarDeepLinks.DORAEMON_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("doraemon", HotstarDeepLinks.DORAEMON_CONTENT_ID),
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Doraemon",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_doraemon",
        characterResName = "art_doraemon_char"
      ),
      MediaItem(
        id = "shinchan",
        title = "Shinchan",
        category = Category.KIDS,
        platform = "JioHotstar",
        subtitle = "Nohara Family Adventures",
        durationOrEpisodes = "Season 8",
        badge = "FUN",
        rating = "TV-PG",
        accentColorHex = 0xFFEF4444,
        contentId = HotstarDeepLinks.SHINCHAN_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("shin-chan", HotstarDeepLinks.SHINCHAN_CONTENT_ID),
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Shinchan",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_shinchan"
      ),
      MediaItem(
        id = "k3",
        title = "Ninja Hattori",
        category = Category.KIDS,
        platform = "JioHotstar",
        subtitle = "Iga Ninja Skills",
        durationOrEpisodes = "Season 4",
        rating = "TV-Y7",
        accentColorHex = 0xFF6366F1,
        contentId = HotstarDeepLinks.NINJA_HATTORI_CONTENT_ID,
        contentUrl = HotstarDeepLinks.NINJA_HATTORI_DETAILS_URI,
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Ninja+Hattori",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_hattori"
      ),
      MediaItem(
        id = "k4",
        title = "Motu Patlu",
        category = Category.KIDS,
        platform = "JioHotstar",
        subtitle = "Furfurinagar Comedy",
        durationOrEpisodes = "Season 10",
        rating = "TV-Y",
        accentColorHex = 0xFFF59E0B,
        contentId = HotstarDeepLinks.MOTU_PATLU_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("motu-patlu", HotstarDeepLinks.MOTU_PATLU_CONTENT_ID),
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Motu+Patlu",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_motu_patlu"
      ),
      MediaItem(
        id = "k5",
        title = "Shintu aur Buntu",
        category = Category.KIDS,
        platform = "YouTube",
        subtitle = "Animation Series",
        durationOrEpisodes = "24 eps",
        rating = "TV-Y",
        accentColorHex = 0xFF14B8A6,
        contentUrl = "https://www.youtube.com/results?search_query=Shintu+aur+Buntu",
        fallbackUrl = "youtube://search?q=Shintu+aur+Buntu",
        deepLinkPackage = "com.amazon.firetv.youtube",
        artworkResName = "art_shintu_buntu"
      ),
      MediaItem(
        id = "k6",
        title = "Pokemon",
        category = Category.KIDS,
        platform = "YouTube",
        subtitle = "Gotta Catch 'Em All",
        durationOrEpisodes = "Season 1",
        badge = "ANIME",
        rating = "TV-Y7",
        accentColorHex = 0xFFEAB308,
        contentUrl = "https://www.youtube.com/results?search_query=Pokemon+IndiGo+League",
        fallbackUrl = "youtube://search?q=Pokemon",
        deepLinkPackage = "com.amazon.firetv.youtube",
        artworkResName = "art_pokemon"
      )
    )

    fun initialSeriesItems(): List<MediaItem> = listOf(
      MediaItem(
        id = "im_game",
        title = "I'm Game",
        category = Category.SERIES,
        platform = "JioHotstar",
        subtitle = "Action Thriller • Tamil",
        durationOrEpisodes = "2h 14m",
        badge = "TRENDING",
        rating = "UA 16+",
        accentColorHex = 0xFF00A3FF,
        contentId = HotstarDeepLinks.IM_GAME_CONTENT_ID,
        contentUrl = HotstarDeepLinks.IM_GAME_DETAILS_URI,
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=I%27m+Game",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_im_game"
      ),
      MediaItem(
        id = "heart_beat",
        title = "Heart Beat",
        category = Category.SERIES,
        platform = "JioHotstar",
        subtitle = "Medical Drama",
        durationOrEpisodes = "3 Seasons",
        badge = "NEW EPISODES",
        rating = "TV-14",
        accentColorHex = 0xFFF43F5E,
        contentId = HotstarDeepLinks.HEART_BEAT_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("heart-beat", HotstarDeepLinks.HEART_BEAT_CONTENT_ID),
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Heart+Beat",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_heart_beat"
      ),
      MediaItem(
        id = "the_court",
        title = "The Court",
        category = Category.SERIES,
        platform = "JioHotstar",
        subtitle = "Legal Drama",
        durationOrEpisodes = "Limited Series",
        badge = "TOP 10",
        rating = "TV-MA",
        accentColorHex = 0xFFEAB308,
        contentId = HotstarDeepLinks.THE_COURT_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("the-court", HotstarDeepLinks.THE_COURT_CONTENT_ID),
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=The+Court",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_the_court"
      ),
      MediaItem(
        id = "s3",
        title = "Siragadikka Aasai",
        category = Category.SERIES,
        platform = "JioHotstar",
        subtitle = "Family Drama",
        durationOrEpisodes = "Latest Episodes",
        badge = "PRIME TIME",
        rating = "TV-PG",
        accentColorHex = 0xFF06B6D4,
        contentId = HotstarDeepLinks.SIRAGADIKKA_AASAI_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("siragadikka-aasai", HotstarDeepLinks.SIRAGADIKKA_AASAI_CONTENT_ID),
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Siragadikka+Aasai",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_siragadikka"
      ),
      MediaItem(
        id = "ayyanar_thunai",
        title = "Ayyanar Thunai",
        category = Category.SERIES,
        platform = "JioHotstar",
        subtitle = "Village Chronicle",
        durationOrEpisodes = "Latest Episodes",
        badge = "TRENDING",
        rating = "TV-14",
        accentColorHex = 0xFF10B981,
        contentId = HotstarDeepLinks.AYYANAR_THUNAI_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("ayyanar-thunai", HotstarDeepLinks.AYYANAR_THUNAI_CONTENT_ID),
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Ayyanar+Thunai",
        deepLinkPackage = HotstarDeepLinks.PACKAGE_NAME,
        artworkResName = "art_ayyanar"
      ),
      MediaItem(
        id = "s5",
        title = "Brothers & Sisters",
        category = Category.SERIES,
        platform = "Prime Video",
        subtitle = "Drama Series",
        durationOrEpisodes = "Season 1",
        rating = "TV-14",
        accentColorHex = 0xFF8B5CF6,
        contentUrl = "amzn://avod/search?phrase=Brothers+and+Sisters",
        fallbackUrl = "https://app.primevideo.com/search?phrase=Brothers+and+Sisters",
        deepLinkPackage = "com.amazon.firebat",
        artworkResName = "art_brothers"
      ),
      MediaItem(
        id = "s6",
        title = "Panchayat",
        category = Category.SERIES,
        platform = "Prime Video",
        subtitle = "Comedy Drama",
        durationOrEpisodes = "Season 3",
        badge = "POPULAR",
        rating = "TV-14",
        accentColorHex = 0xFFF59E0B,
        contentUrl = "amzn://avod/search?phrase=Panchayat",
        fallbackUrl = "https://app.primevideo.com/search?phrase=Panchayat",
        deepLinkPackage = "com.amazon.firebat",
        artworkResName = "art_panchayat"
      )
    )

    fun initialWhatsNew(): List<WhatsNewItem> = listOf(
      WhatsNewItem(
        id = "wn1",
        title = "The Penguin",
        platform = "Netflix",
        contentType = "Series",
        posterResName = "art_wn_penguin",
        platformIconResName = "app_netflix",
        posterAccentHex = 0xFFE50914
      ),
      WhatsNewItem(
        id = "wn2",
        title = "Citadel: Honey Bunny",
        platform = "Prime Video",
        contentType = "Series",
        posterResName = "art_wn_citadel",
        platformIconResName = "app_primevideo",
        posterAccentHex = 0xFF00A8E1
      ),
      WhatsNewItem(
        id = "wn3",
        title = "CTRL",
        platform = "Netflix",
        contentType = "Movie",
        posterResName = "art_wn_ctrl",
        platformIconResName = "app_netflix",
        posterAccentHex = 0xFFE50914
      ),
      WhatsNewItem(
        id = "wn4",
        title = "Kishkindha Kaandam",
        platform = "JioHotstar",
        contentType = "Movie",
        posterResName = "art_wn_kishkindha",
        platformIconResName = "app_jiohotstar",
        posterAccentHex = 0xFF00A3FF
      )
    )

    fun initialDockItems(): List<AppDockItem> = listOf(
      AppDockItem("dock1", "JioHotstar", "in.startv.hotstar", "LAUNCH_APP", iconResName = "app_jiohotstar"),
      AppDockItem("dock2", "Netflix", "com.netflix.ninja", "LAUNCH_APP", iconResName = "app_netflix"),
      AppDockItem("dock3", "Prime Video", "com.amazon.firebat", "LAUNCH_APP", iconResName = "app_primevideo"),
      AppDockItem("dock4", "YouTube", "com.amazon.firetv.youtube", "LAUNCH_APP", iconResName = "app_youtube"),
      AppDockItem("dock5", "All Apps", "", "ALL_APPS", isSystemAction = true),
      AppDockItem("dock6", "Search", "", "SEARCH", isSystemAction = true),
      AppDockItem("dock7", "Settings", "", "SETTINGS", isSystemAction = true),
      AppDockItem("dock8", "Screen Mirror", "", "SCREEN_MIRROR", isSystemAction = true)
    )
  }
}
