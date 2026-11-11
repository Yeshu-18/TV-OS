package com.firelite.tv

import com.firelite.core.data.FireLiteRepository
import com.firelite.core.data.HotstarDeepLinks
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import com.firelite.tv.ui.FavouriteRow
import com.firelite.tv.ui.buildDefaultFavouritesLineup
import com.firelite.tv.ui.computeFavouritesNeighbors
import com.firelite.tv.ui.resolveCharacterZIndex
import com.firelite.tv.util.RecentlyWatchedLruCache
import com.firelite.tv.util.TvWatchNextManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FireLiteRepositoryTest {
  @Test
  fun initialPins_areLoadedCorrectly() = runTest {
    val repo = FireLiteRepository()
    val kids = repo.kidsItems.value
    val series = repo.seriesItems.value

    assertEquals(6, kids.size)
    assertEquals(7, series.size)
    assertEquals("Doraemon", kids[0].title)
    assertEquals("I'm Game", series[0].title)
    assertEquals("Heart Beat", series[1].title)

    val allHotstarPins = (kids + series).filter { it.platform == "JioHotstar" }
    assertTrue(allHotstarPins.all { it.contentUrl?.startsWith("hotstar://in/") == true })
    assertTrue(allHotstarPins.all { it.deepLinkPackage == HotstarDeepLinks.PACKAGE_NAME })
    assertEquals("1271720887", series.first { it.id == "im_game" }.contentId)
    assertEquals("hotstar://in/movies/im-game/1271720887", series.first { it.id == "im_game" }.contentUrl)
    assertEquals("hotstar://in/shows/heart-beat/1260168231", series.first { it.id == "heart_beat" }.contentUrl)
    assertEquals("hotstar://in/shows/the-court/1271672560", series.first { it.id == "the_court" }.contentUrl)
    assertEquals("hotstar://in/shows/shin-chan/1260020929", kids.first { it.id == "shinchan" }.contentUrl)
    assertEquals("hotstar://in/shows/doraemon/1260021004", kids.first { it.id == "doraemon" }.contentUrl)
    assertEquals("hotstar://in/shows/ninja-hattori-classic/1971003458", kids.first { it.id == "k3" }.contentUrl)
  }

  @Test
  fun initialContinueWatching_useHotstarWatchDeepLinks() {
    val items = TvWatchNextManager.getInitialContinueWatchingItems()
    val byId = items.associateBy { it.id }

    assertEquals("1271720887", byId["cw_im_game"]?.contentId)
    assertEquals("hotstar://in/movies/im-game/1271720887/watch", byId["cw_im_game"]?.contentUrl)
    assertEquals("1271708139", byId["cw_bittu_bahanebaaz"]?.contentId)
    assertEquals(
      "hotstar://in/movies/bittu-bahanebaaz-ek-ka-teen/1271708139/watch",
      byId["cw_bittu_bahanebaaz"]?.contentUrl
    )
    assertEquals("hotstar://in/shows/heart-beat/1260168231/watch", byId["cw_heart_beat"]?.contentUrl)
    assertEquals("hotstar://in/shows/the-court/1271672560/watch", byId["cw_the_court"]?.contentUrl)
    assertEquals("hotstar://in/shows/doraemon/1260021004/watch", byId["cw_doraemon"]?.contentUrl)
  }

  @Test
  fun hotstarDeepLinks_normalizeAndResolvePackageCorrectly() {
    assertEquals(
      "hotstar://in/movies/im-game/1271720887/watch",
      HotstarDeepLinks.normalizeUrl("https://www.hotstar.com/in/movies/im-game/1271354531", preferWatch = true)
    )
    assertEquals(
      "hotstar://in/shows/shin-chan/1260020929/watch",
      HotstarDeepLinks.normalizeUrl("hotstar://shows/shin-chan/1260020929", preferWatch = true)
    )
    assertEquals(
      "hotstar://in/shows/the-court/1271672560/the-choice/1271687155/watch",
      HotstarDeepLinks.normalizeUrl(
        "https://www.hotstar.com/in/shows/the-court/1271672560/the-choice/1271687155",
        preferWatch = true
      )
    )
    assertEquals(
      "intent:#Intent;package=in.startv.hotstar;end",
      HotstarDeepLinks.normalizeUrl("intent:#Intent;package=in.startv.hotstar;end", preferWatch = true)
    )

    val migratedBittu = HotstarDeepLinks.normalizeMediaItem(
      MediaItem(
        id = "cw_bittu_bahanebaaz",
        title = "Bittu Bahanebaaz",
        category = Category.KIDS,
        platform = "JioHotstar",
        contentId = "bittu_bahanebaaz",
        contentUrl = "https://www.hotstar.com/in/explore?search_query=Bittu+Bahanebaaz"
      ),
      preferWatch = true
    )
    assertEquals("1271708139", migratedBittu.contentId)
    assertEquals(
      "hotstar://in/movies/bittu-bahanebaaz-ek-ka-teen/1271708139/watch",
      migratedBittu.contentUrl
    )
    assertEquals(HotstarDeepLinks.PACKAGE_NAME, migratedBittu.deepLinkPackage)

    assertEquals(
      "in.startv.hotstar",
      TvWatchNextManager.packageForUrl("hotstar://in/shows/doraemon/1260021004/watch")
    )
    assertEquals("com.netflix.ninja", TvWatchNextManager.packageForUrl("netflix://title/81435689"))
    assertEquals("com.amazon.firebat", TvWatchNextManager.packageForUrl("amzn://avod/search?phrase=Panchayat"))
    assertEquals(
      "com.amazon.firetv.youtube",
      TvWatchNextManager.packageForUrl("youtube://search?q=Pokemon")
    )
    assertNull(TvWatchNextManager.packageForUrl("intent:#Intent;package=in.startv.hotstar;end"))
  }

  @Test
  fun addAndRemovePin_worksAsExpected() = runTest {
    val repo = FireLiteRepository()
    val newItem = MediaItem(
      id = "test1",
      title = "Test Show",
      category = Category.KIDS,
      platform = "YouTube"
    )
    repo.addPinnedItem(newItem)
    assertTrue(repo.kidsItems.value.any { it.id == "test1" })

    repo.removePinnedItem("test1")
    assertTrue(repo.kidsItems.value.none { it.id == "test1" })
  }

  @Test
  fun recentlyWatchedLruCache_keepsMostRecentTenAndEvictsLeastRecentlyUsed() {
    assertEquals(10, TvWatchNextManager.MAX_RECENT_ITEMS)
    val lru = RecentlyWatchedLruCache(maxSize = TvWatchNextManager.MAX_RECENT_ITEMS)

    for (i in 1..10) {
      lru.put(
        MediaItem(
          id = "show_$i",
          title = "Show $i",
          category = Category.SERIES,
          platform = "JioHotstar",
          contentId = "9000$i",
          contentUrl = "hotstar://in/shows/show-$i/9000$i"
        ),
        lastWatchedAtMillis = i * 1000L
      )
    }
    assertEquals(10, lru.size())
    assertEquals((10 downTo 1).map { "Show $it" }, lru.snapshotRecentFirst().map { it.title })

    // Re-watch Show 1 (oldest) so Show 2 becomes least-recently-used before adding Show 11
    lru.put(
      MediaItem(
        id = "cw_show_1",
        title = "Show 1",
        category = Category.SERIES,
        platform = "JioHotstar",
        contentId = "90001",
        contentUrl = "hotstar://in/shows/show-1/90001/watch"
      ),
      lastWatchedAtMillis = 11_000L
    )
    assertEquals("Show 1", lru.snapshotRecentFirst().first().title)
    assertEquals(10, lru.size())

    // Watching Show 11 must evict Show 2 (LRU), while retaining Show 1 at slot 1
    lru.put(
      MediaItem(
        id = "show_11",
        title = "Show 11",
        category = Category.SERIES,
        platform = "JioHotstar",
        contentId = "90011",
        contentUrl = "hotstar://in/shows/show-11/90011/watch"
      ),
      lastWatchedAtMillis = 12_000L
    )
    val titlesAfterEviction = lru.snapshotRecentFirst().map { it.title }
    assertEquals(10, titlesAfterEviction.size)
    assertEquals(listOf("Show 11", "Show 1", "Show 10"), titlesAfterEviction.take(3))
    assertTrue("Expected Show 2 (LRU) to be evicted", "Show 2" !in titlesAfterEviction)
  }

  @Test
  fun recentlyWatchedLruCache_deduplicatesAndPreservesOrderAcrossRestoreWithoutPinning() {
    val lru = RecentlyWatchedLruCache(maxSize = TvWatchNextManager.MAX_RECENT_ITEMS)

    // Watch I'm Game first, then Heart Beat, then The Court (The Court must stay #1 across restore)
    lru.put(
      MediaItem(
        id = "im_game",
        title = "I'm Game",
        category = Category.SERIES,
        platform = "JioHotstar",
        contentId = HotstarDeepLinks.IM_GAME_CONTENT_ID,
        contentUrl = HotstarDeepLinks.IM_GAME_DETAILS_URI,
        artworkResName = "art_im_game"
      ),
      lastWatchedAtMillis = 1_000L
    )
    lru.put(
      MediaItem(
        id = "heart_beat",
        title = "Heart Beat",
        category = Category.SERIES,
        platform = "JioHotstar",
        contentId = HotstarDeepLinks.HEART_BEAT_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("heart-beat", HotstarDeepLinks.HEART_BEAT_CONTENT_ID),
        artworkResName = "art_heart_beat"
      ),
      lastWatchedAtMillis = 2_000L
    )
    lru.put(
      MediaItem(
        id = "the_court",
        title = "The Court",
        category = Category.SERIES,
        platform = "JioHotstar",
        contentId = HotstarDeepLinks.THE_COURT_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showDetailsUri("the-court", HotstarDeepLinks.THE_COURT_CONTENT_ID),
        artworkResName = "art_the_court"
      ),
      lastWatchedAtMillis = 3_000L
    )

    // Touch I'm Game using an OS Watch Next style entry (no artworkResName, different id) -> merges metadata & moves to front
    lru.put(
      MediaItem(
        id = "wn_im_game",
        title = "I'm Game",
        category = Category.SERIES,
        platform = "JioHotstar",
        contentUrl = HotstarDeepLinks.IM_GAME_WATCH_URI
      ),
      lastWatchedAtMillis = 4_000L
    )
    assertEquals(3, lru.size())
    assertEquals("art_im_game", lru.snapshotRecentFirst()[0].artworkResName)

    // Touch Heart Beat last, then simulate restart restore
    lru.put(
      MediaItem(
        id = "heart_beat",
        title = "Heart Beat",
        category = Category.SERIES,
        platform = "JioHotstar",
        contentId = HotstarDeepLinks.HEART_BEAT_CONTENT_ID,
        contentUrl = HotstarDeepLinks.showWatchUri("heart-beat", HotstarDeepLinks.HEART_BEAT_CONTENT_ID)
      ),
      lastWatchedAtMillis = 5_000L
    )

    val saved = lru.snapshotEntriesRecentFirst()
    val restored = RecentlyWatchedLruCache(maxSize = TvWatchNextManager.MAX_RECENT_ITEMS)
    restored.restoreEntriesRecentFirst(saved)

    assertEquals(
      listOf("Heart Beat", "I'm Game", "The Court"),
      restored.snapshotRecentFirst().map { it.title }
    )
    assertEquals(
      HotstarDeepLinks.showWatchUri("heart-beat", HotstarDeepLinks.HEART_BEAT_CONTENT_ID),
      restored.snapshotRecentFirst()[0].contentUrl
    )
  }

  @Test
  fun myFavourites_lineupRowsZOrderAndDeepLinksAreValid() {
    val lineup = buildDefaultFavouritesLineup()
    assertEquals(5, lineup.size)

    val back = lineup.filter { it.row == FavouriteRow.BACK }.sortedBy { it.rowOrder }
    val front = lineup.filter { it.row == FavouriteRow.FRONT }.sortedBy { it.rowOrder }
    assertEquals(listOf("heart_beat", "doraemon", "the_court"), back.map { it.id })
    assertEquals(listOf("siragadikka_aasai", "shinchan"), front.map { it.id })

    // Verify every favourite maps cleanly to a verified JioHotstar hotstar://in/... deep link
    assertTrue(lineup.all { it.streamingApp == "JioHotstar" })
    assertTrue(lineup.all { it.deepLinkPackage == HotstarDeepLinks.PACKAGE_NAME })
    assertTrue(lineup.all { it.contentUrl.startsWith("hotstar://in/shows/") })

    // Verify z-order invariant:
    // unselected back < selected back < unselected front < selected front
    val zBackUnselected = resolveCharacterZIndex(FavouriteRow.BACK, isSelected = false)
    val zBackSelected = resolveCharacterZIndex(FavouriteRow.BACK, isSelected = true)
    val zFrontUnselected = resolveCharacterZIndex(FavouriteRow.FRONT, isSelected = false)
    val zFrontSelected = resolveCharacterZIndex(FavouriteRow.FRONT, isSelected = true)
    assertTrue(zBackUnselected < zBackSelected)
    assertTrue(zBackSelected < zFrontUnselected)
    assertTrue(zFrontUnselected < zFrontSelected)

    // Verify row-locked Left/Right and cross-row Up/Down D-pad neighbor graph
    val doraemonNav = computeFavouritesNeighbors(back[1], back, front)
    assertEquals("heart_beat", doraemonNav.leftId)
    assertEquals("the_court", doraemonNav.rightId)
    assertNull(doraemonNav.upId)
    assertEquals("shinchan", doraemonNav.downId)

    val siragadikkaNav = computeFavouritesNeighbors(front[0], back, front)
    assertNull(siragadikkaNav.leftId)
    assertEquals("shinchan", siragadikkaNav.rightId)
    assertEquals("doraemon", siragadikkaNav.upId)
    assertNull(siragadikkaNav.downId)
  }
}
