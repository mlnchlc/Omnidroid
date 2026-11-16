package com.omnidroid.app.mobile.feature.library

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLibraryTest {
    private val step = 110f
    private val small = 96f
    private val selected = 130f
    private val gap = 14f
    private val viewport = 600f

    private fun target(index: Int, scroll: Float, count: Int = 20) =
        homeRowScrollTarget(index, count, scroll, step, small, selected, gap, viewport)

    @Test
    fun `a card already in view does not scroll`() {
        assertNull(target(index = 2, scroll = 0f))
    }

    @Test
    fun `moving past the right edge scrolls just enough to show the next card peeking`() {
        val result = target(index = 5, scroll = 0f)!!
        val end = 5 * step + selected
        assertEquals(end + gap + small * 0.4f - viewport, result, 0.01f)
    }

    @Test
    fun `moving back past the left edge keeps the previous card peeking`() {
        val result = target(index = 3, scroll = 400f)!!
        assertEquals(3 * step - (gap + small * 0.4f), result, 0.01f)
    }

    @Test
    fun `the first and last cards need no peek`() {
        assertEquals(0f, target(index = 0, scroll = 50f)!!, 0.01f)
        val last = target(index = 19, scroll = 0f)!!
        assertEquals(19 * step + selected - viewport, last, 0.01f)
    }

    @Test
    fun `an empty row never scrolls`() {
        assertNull(target(index = 0, scroll = 0f, count = 0))
    }

    @Test
    fun `landscape cards fill the height left over`() {
        val metrics = HomeRowMetrics.from(availableWidth = 800.dp, availableHeight = 300.dp, portrait = false)
        // 30 dp header and 6 dp of ring space above and below the row.
        assertEquals(258f, metrics.selectedHeight.value, 0.01f)
        assertEquals(172f, metrics.selectedWidth.value, 0.01f)
        assertEquals(258f * 0.86f, metrics.smallHeight.value, 0.01f)
    }

    @Test
    fun `portrait cards are capped by the screen width`() {
        val metrics = HomeRowMetrics.from(availableWidth = 360.dp, availableHeight = 700.dp, portrait = true)
        assertEquals(216f, metrics.selectedWidth.value, 0.01f)
        assertTrue(metrics.selectedHeight.value < 700f)
    }
}
