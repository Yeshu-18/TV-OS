package com.firelite.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class RemoteKey {
  UP, DOWN, LEFT, RIGHT, SELECT, BACK, HOME, PLAY_PAUSE, VOLUME_UP, VOLUME_DOWN, MUTE
}

@Serializable
sealed class RemoteCommand {
  @Serializable
  data class PressKey(val key: RemoteKey) : RemoteCommand()

  @Serializable
  data class SendText(val text: String) : RemoteCommand()

  @Serializable
  data class AddPin(val item: MediaItem) : RemoteCommand()

  @Serializable
  data class RemovePin(val itemId: String) : RemoteCommand()

  @Serializable
  data class MoveCategory(val itemId: String, val targetCategory: Category) : RemoteCommand()

  @Serializable
  data class ReorderPins(val category: Category, val fromIndex: Int, val toIndex: Int) : RemoteCommand()

  @Serializable
  data object RequestSync : RemoteCommand()

  @Serializable
  data class SyncState(
    val kids: List<MediaItem>,
    val series: List<MediaItem>,
    val whatsNew: List<WhatsNewItem>,
    val weather: WeatherAlertInfo
  ) : RemoteCommand()
}
