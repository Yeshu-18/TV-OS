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
      json.decodeFromString<List<MediaItem>>(rawJson)
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
        id = "k1",
        title = "Doraemon",
        category = Category.KIDS,
        platform = "JioHotstar",
        subtitle = "Gadgets from the Future",
        durationOrEpisodes = "Season 12",
        badge = "POPULAR",
        rating = "TV-Y7",
        accentColorHex = 0xFF00A3FF,
        contentUrl = "https://www.hotstar.com/in/shows/doraemon/17621",
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Doraemon",
        deepLinkPackage = "in.startv.hotstar",
        artworkResName = "art_doraemon",
        characterResName = "art_doraemon_char"
      ),
      MediaItem(
        id = "k2",
        title = "Shinchan",
        category = Category.KIDS,
        platform = "JioHotstar",
        subtitle = "Nohara Family Adventures",
        durationOrEpisodes = "Season 8",
        badge = "FUN",
        rating = "TV-PG",
        accentColorHex = 0xFFEF4444,
        contentUrl = "https://www.hotstar.com/in/shows/shinchan/1260000030",
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Shinchan",
        deepLinkPackage = "in.startv.hotstar",
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
        contentUrl = "https://www.hotstar.com/in/shows/ninja-hattori/1260000045",
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Ninja+Hattori",
        deepLinkPackage = "in.startv.hotstar",
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
        contentUrl = "https://www.hotstar.com/in/shows/motu-patlu/1260005510",
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Motu+Patlu",
        deepLinkPackage = "in.startv.hotstar",
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
        id = "s1",
        title = "Heart Beat",
        category = Category.SERIES,
        platform = "JioHotstar",
        subtitle = "Medical Drama",
        durationOrEpisodes = "Season 2",
        badge = "NEW EPISODES",
        rating = "TV-14",
        accentColorHex = 0xFFF43F5E,
        contentUrl = "https://www.hotstar.com/in/shows/heart-beat/1271302824",
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Heart+Beat",
        deepLinkPackage = "in.startv.hotstar",
        artworkResName = "art_heart_beat"
      ),
      MediaItem(
        id = "s2",
        title = "The Court",
        category = Category.SERIES,
        platform = "Netflix",
        subtitle = "Legal Drama",
        durationOrEpisodes = "Limited Series",
        badge = "TOP 10",
        rating = "TV-MA",
        accentColorHex = 0xFFEAB308,
        contentUrl = "https://www.netflix.com/title/81435689",
        fallbackUrl = "netflix://title/81435689",
        deepLinkPackage = "com.netflix.ninja",
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
        contentUrl = "https://www.hotstar.com/in/shows/siragadikka-aasai/1260127599",
        fallbackUrl = "https://www.hotstar.com/in/explore?search_query=Siragadikka+Aasai",
        deepLinkPackage = "in.startv.hotstar",
        artworkResName = "art_siragadikka"
      ),
      MediaItem(
        id = "s4",
        title = "Ayyanar Thunai",
        category = Category.SERIES,
        platform = "Sun NXT",
        subtitle = "Village Chronicle",
        durationOrEpisodes = "Ep 42",
        badge = "TRENDING",
        rating = "TV-14",
        accentColorHex = 0xFF10B981,
        contentUrl = "sunnxt://deeplink?content_id=ayyanar_thunai",
        fallbackUrl = "https://www.sunnxt.com",
        deepLinkPackage = "com.suntv.sunnxt",
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
