package com.firelite.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Category(val displayName: String) {
  KIDS("Kids"),
  SERIES("Series")
}

@Serializable
data class MediaItem(
  val id: String,
  val title: String,
  val category: Category,
  val platform: String = "JioHotstar",
  val subtitle: String = "",
  val durationOrEpisodes: String = "",
  val badge: String? = null,
  val rating: String = "PG",
  val accentColorHex: Long = 0xFF7C8CFF,
  val contentUrl: String? = null,
  val deepLinkPackage: String? = null,
  val fallbackUrl: String? = null,
  val artworkResName: String? = null,
  val bannerResName: String? = null,
  val characterResName: String? = null,
  val contentId: String? = null,
  val posterUrl: String? = null,
  val playbackProgress: Float? = null,
  val episodeTitle: String? = null
)

@Serializable
data class WhatsNewItem(
  val id: String,
  val title: String,
  val platform: String,
  val contentType: String = "Series",
  val posterResName: String? = null,
  val platformIconResName: String? = null,
  val posterAccentHex: Long = 0xFF7C8CFF
)

@Serializable
data class WeatherAlertInfo(
  val location: String = "Bengaluru",
  val temperature: String = "24°C",
  val condition: String = "Light Rain",
  val alertTitle: String? = "Heavy rain expected",
  val alertTime: String? = "6:00 PM – 8:00 PM"
)

enum class DockActionType {
  LAUNCH_APP,
  ALL_APPS,
  SEARCH,
  SETTINGS,
  SCREEN_MIRROR
}

@Serializable
data class AppDockItem(
  val id: String,
  val name: String,
  val packageName: String,
  val actionType: String = "LAUNCH_APP",
  val isSystemAction: Boolean = false,
  val iconResName: String? = null
)
