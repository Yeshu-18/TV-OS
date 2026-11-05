package com.firelite.tv

import com.firelite.core.data.FireLiteRepository
import com.firelite.core.model.Category
import com.firelite.core.model.MediaItem
import junit.framework.TestCase.assertEquals
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
    assertEquals(6, series.size)
    assertEquals("Doraemon", kids[0].title)
    assertEquals("Heart Beat", series[0].title)
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
}
