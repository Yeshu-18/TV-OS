package com.firelite.tv.ui

import com.firelite.core.data.HotstarDeepLinks
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodDeckSectionTest {

  private val allPackages = setOf(
    HotstarDeepLinks.PACKAGE_NAME,
    "com.netflix.ninja",
    "com.amazon.firebat"
  )

  @Test
  fun defaultMoodCategories_hasSixMoodsEndingWithAnythingAtAll() {
    val categories = buildDefaultMoodCategories()
    assertEquals(6, categories.size)
    assertEquals("Comfort sitcom", categories[0].name)
    assertEquals(0xFFF2B84B, categories[0].colorHex)
    assertEquals("Capes and chaos", categories[1].name)
    assertEquals(0xFFE5484D, categories[1].colorHex)

    val last = categories.last()
    assertEquals("Anything at all", last.name)
    assertEquals(0xFFB8C0CC, last.colorHex)
    assertTrue(last.isAllTitlesBucket)
  }

  @Test
  fun defaultMoodTitles_haveValidLinksPackagesAndDrawables() {
    val titles = buildDefaultMoodTitles()
    assertEquals(24, titles.size)
    assertEquals(24, titles.map { it.id }.toSet().size)

    val categoryIds = buildDefaultMoodCategories()
      .filterNot { it.isAllTitlesBucket }
      .map { it.id }
      .toSet()

    titles.forEach { title ->
      assertTrue("Title ${title.id} should belong to at least one mood", title.moodIds.isNotEmpty())
      assertTrue("Unknown moodId on ${title.id}", title.moodIds.all { it in categoryIds })
      assertEquals(
        "Link package mismatch for ${title.title} (${title.link})",
        title.packageName,
        HotstarDeepLinks.packageForUrl(title.link)
      )
      assertEquals("mood_poster_${title.id}", title.posterResName)
      assertEquals("mood_bg_${title.id}", title.backdropResName)
    }
  }

  @Test
  fun filterPlayableMoodDecks_includesAllTitlesWhenAllAppsInstalled() {
    val decks = filterPlayableMoodDecks(installedPackages = allPackages)
    assertEquals(6, decks.size)

    val specificDecks = decks.dropLast(1)
    specificDecks.forEach { deck ->
      assertTrue("Mood ${deck.category.name} has too few titles: ${deck.titles.size}", deck.titles.size in 4..8)
    }

    val anything = decks.last()
    assertEquals("anything", anything.category.id)
    assertEquals(24, anything.titles.size)
  }

  @Test
  fun filterPlayableMoodDecks_skipsTitlesAndEmptyMoodsForUninstalledApps() {
    val hotstarOnly = filterPlayableMoodDecks(installedPackages = setOf(HotstarDeepLinks.PACKAGE_NAME))
    assertTrue(hotstarOnly.isNotEmpty())
    assertFalse(
      "After hours drama only has Netflix & Prime titles and should be dropped",
      hotstarOnly.any { it.category.id == "after_hours" }
    )
    hotstarOnly.forEach { deck ->
      assertTrue(deck.titles.all { it.packageName == HotstarDeepLinks.PACKAGE_NAME })
    }
    val expectedHotstarCount = buildDefaultMoodTitles().count { it.packageName == HotstarDeepLinks.PACKAGE_NAME }
    assertEquals(expectedHotstarCount, hotstarOnly.last().titles.size)

    val emptyDecks = filterPlayableMoodDecks(installedPackages = emptySet())
    assertTrue(emptyDecks.isEmpty())
  }

  @Test
  fun deckAdvanceAndReelHelpers_behaveDeterministically() {
    assertEquals(1, advanceDeckIndex(0, 6))
    assertEquals(0, advanceDeckIndex(5, 6))
    assertEquals(0, advanceDeckIndex(0, 0))

    val decks = filterPlayableMoodDecks(allPackages)
    val sitcoms = decks.first().titles
    val picked = pickRandomSpinTitle(sitcoms, excludeTitleId = sitcoms.first().id, random = Random(42))
    assertNotNull(picked)
    assertNotEquals(sitcoms.first().id, picked!!.id)

    val strip = buildReelStrip(sitcoms, winner = picked, hopCount = 18, random = Random(7))
    assertEquals(20, strip.size)
    assertEquals(picked, strip[strip.size - 2])

    assertEquals(picked, resolveDeckBackdropTitle(decks, topIndex = 0, winner = picked))
    assertNotNull(resolveDeckBackdropTitle(decks, topIndex = 0, winner = null))
  }
}

