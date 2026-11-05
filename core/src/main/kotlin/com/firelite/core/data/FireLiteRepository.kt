package com.firelite.core.data

import com.firelite.core.model.AppDockItem
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.core.model.WeatherAlertInfo
import com.firelite.core.model.WhatsNewItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FireLiteRepository {

  private val _kidsItems = MutableStateFlow<List<MediaItem>>(initialKidsItems())
  val kidsItems: StateFlow<List<MediaItem>> = _kidsItems.asStateFlow()

  private val _seriesItems = MutableStateFlow<List<MediaItem>>(initialSeriesItems())
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
        if (current.any { it.id == item.id }) current else current + item
      }
      Category.SERIES -> _seriesItems.update { current ->
        if (current.any { it.id == item.id }) current else current + item
      }
    }
  }

  fun removePinnedItem(itemId: String) {
    _kidsItems.update { it.filterNot { item -> item.id == itemId } }
    _seriesItems.update { it.filterNot { item -> item.id == itemId } }
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
        reorderList(list, fromIndex, toIndex)
      }
      Category.SERIES -> _seriesItems.update { list ->
        reorderList(list, fromIndex, toIndex)
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
        accentColorHex = 0xFF38BDF8, // Dynamic Accent: Blue
        contentUrl = "hotstar://play/doraemon",
        deepLinkPackage = "in.startv.hotstar"
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
        accentColorHex = 0xFFEF4444, // Dynamic Accent: Warm Red
        contentUrl = "hotstar://play/shinchan",
        deepLinkPackage = "in.startv.hotstar"
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
        contentUrl = "hotstar://play/ninja-hattori",
        deepLinkPackage = "in.startv.hotstar"
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
        contentUrl = "hotstar://play/motu-patlu",
        deepLinkPackage = "in.startv.hotstar"
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
        contentUrl = "vnd.youtube://shintu-buntu",
        deepLinkPackage = "com.google.android.youtube.tv"
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
        contentUrl = "hotstar://play/heart-beat",
        deepLinkPackage = "in.startv.hotstar"
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
        contentUrl = "netflix://title/the-court",
        deepLinkPackage = "com.netflix.ninja"
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
        contentUrl = "hotstar://play/siragadikka-aasai",
        deepLinkPackage = "in.startv.hotstar"
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
        contentUrl = "sunnxt://content/ayyanar-thunai",
        deepLinkPackage = "com.suntv.sunnxt"
      ),
      MediaItem(
        id = "s5",
        title = "Our Planet",
        category = Category.SERIES,
        platform = "Netflix",
        subtitle = "Nature & Wildlife Documentary",
        durationOrEpisodes = "Season 2",
        badge = "4K HDR",
        rating = "TV-G",
        accentColorHex = 0xFF10B981, // Dynamic Accent: Nature Green
        contentUrl = "netflix://title/our-planet",
        deepLinkPackage = "com.netflix.ninja"
      ),
      MediaItem(
        id = "s6",
        title = "Brothers and Sisters",
        category = Category.SERIES,
        platform = "Prime Video",
        subtitle = "Drama Series",
        durationOrEpisodes = "Season 1",
        rating = "TV-14",
        accentColorHex = 0xFF8B5CF6,
        contentUrl = "primevideo://content/brothers-sisters",
        deepLinkPackage = "com.amazon.amazonvideo.livingroom"
      )
    )

    fun initialWhatsNew(): List<WhatsNewItem> = listOf(
      WhatsNewItem(
        id = "wn1",
        title = "Ayyanar Thunai Ep 42",
        platform = "Sun NXT",
        posterAccentHex = 0xFF10B981
      ),
      WhatsNewItem(
        id = "wn2",
        title = "The Court Finale",
        platform = "Netflix",
        posterAccentHex = 0xFFEAB308
      ),
      WhatsNewItem(
        id = "wn3",
        title = "Doraemon New Season",
        platform = "JioHotstar",
        posterAccentHex = 0xFF38BDF8
      )
    )

    fun initialDockItems(): List<AppDockItem> = listOf(
      AppDockItem("dock1", "JioHotstar", "in.startv.hotstar", "LAUNCH_APP"),
      AppDockItem("dock2", "Netflix", "com.netflix.ninja", "LAUNCH_APP"),
      AppDockItem("dock3", "Prime Video", "com.amazon.amazonvideo.livingroom", "LAUNCH_APP"),
      AppDockItem("dock4", "YouTube", "com.google.android.youtube.tv", "LAUNCH_APP"),
      AppDockItem("dock5", "All Apps", "", "ALL_APPS", isSystemAction = true),
      AppDockItem("dock6", "Search", "", "SEARCH", isSystemAction = true),
      AppDockItem("dock7", "Settings", "", "SETTINGS", isSystemAction = true),
      AppDockItem("dock8", "Screen Mirror", "", "SCREEN_MIRROR", isSystemAction = true)
    )
  }
}
