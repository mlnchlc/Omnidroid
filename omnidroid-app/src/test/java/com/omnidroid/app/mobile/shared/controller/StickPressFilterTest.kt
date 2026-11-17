package com.omnidroid.app.mobile.shared.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StickPressFilterTest {
    private fun presses(
        filter: StickPressFilter,
        samples: List<Triple<Float, Float, Long>>,
    ): List<StickPressFilter.Press> = samples.mapNotNull { (x, y, t) -> filter.update(x, y, t) }

    @Test
    fun `a flick right and back is one press`() {
        val filter = StickPressFilter()
        val result =
            presses(
                filter,
                listOf(
                    Triple(0.3f, 0f, 0L),
                    Triple(0.8f, 0f, 16L),
                    Triple(1f, 0f, 32L),
                    Triple(0.4f, 0f, 48L),
                    Triple(0.1f, 0f, 64L),
                    // Spring back overshoots to the left.
                    Triple(-0.7f, 0f, 80L),
                    Triple(-0.2f, 0f, 96L),
                    Triple(0f, 0f, 112L),
                ),
            )
        assertEquals(listOf(StickPressFilter.Press(1, 0)), result)
    }

    @Test
    fun `jitter around the threshold does not press again`() {
        val filter = StickPressFilter()
        val result =
            presses(
                filter,
                listOf(0.62f, 0.58f, 0.61f, 0.55f, 0.63f).mapIndexed { i, x -> Triple(x, 0f, i * 16L) },
            )
        assertEquals(1, result.size)
    }

    @Test
    fun `two separate flicks the same way are two presses`() {
        val filter = StickPressFilter()
        val result =
            presses(
                filter,
                listOf(
                    Triple(0.9f, 0f, 0L),
                    Triple(0f, 0f, 100L),
                    Triple(0.9f, 0f, 250L),
                    Triple(0f, 0f, 350L),
                ),
            )
        assertEquals(2, result.size)
    }

    @Test
    fun `left after the rebound window is a real press`() {
        val filter = StickPressFilter()
        val result =
            presses(
                filter,
                listOf(
                    Triple(0.9f, 0f, 0L),
                    Triple(0f, 0f, 100L),
                    Triple(-0.9f, 0f, 600L),
                ),
            )
        assertEquals(listOf(StickPressFilter.Press(1, 0), StickPressFilter.Press(-1, 0)), result)
    }

    @Test
    fun `a quick deliberate flick back counts`() {
        val filter = StickPressFilter()
        val result =
            presses(
                filter,
                listOf(
                    Triple(1f, 0f, 0L),
                    Triple(0.1f, 0f, 40L),
                    // Pushed hard the other way right after: a real press, not a rebound.
                    Triple(-1f, 0f, 70L),
                    Triple(0f, 0f, 110L),
                ),
            )
        assertEquals(listOf(StickPressFilter.Press(1, 0), StickPressFilter.Press(-1, 0)), result)
    }

    @Test
    fun `a diagonal presses only the stronger direction`() {
        val filter = StickPressFilter()
        assertEquals(StickPressFilter.Press(0, 1), filter.update(0.65f, 0.8f, 0L))
        assertNull(filter.update(0.7f, 0.8f, 16L))
    }

    @Test
    fun `holding repeats only after the hold delay`() {
        val filter = StickPressFilter()
        val result = presses(filter, (0..50).map { Triple(1f, 0f, it * 20L) })
        // First press at 0 ms, then repeats from 560 ms every >= 180 ms up to 1000 ms: 560, 740, 920.
        assertEquals(4, result.size)
    }
}
