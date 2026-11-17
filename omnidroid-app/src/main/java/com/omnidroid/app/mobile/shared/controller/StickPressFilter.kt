package com.omnidroid.app.mobile.shared.controller

import kotlin.math.abs
import kotlin.math.sign

/**
 * Turns left-stick movement into single D-pad presses for UI navigation.
 *
 * - One tilt is one press: it presses at [pressThreshold] and only lets go below [releaseThreshold],
 *   so a stick hovering near the edge doesn't press again and again.
 * - The spring back past the centre after letting go reads as the opposite direction. A rebound is
 *   quick and shallow, so an opposite tilt within [reboundMs] that stays under [reboundMaxLean] is
 *   ignored (a flick forth and back counts once); a deliberate flick back goes further and counts.
 * - Holding the stick repeats, but only after [holdDelayMs], every [holdRepeatMs].
 *
 * Pure (time is passed in) so it can be unit tested.
 */
class StickPressFilter(
    private val pressThreshold: Float = 0.6f,
    private val releaseThreshold: Float = 0.3f,
    private val reboundMs: Long = 150L,
    private val reboundMaxLean: Float = 0.85f,
    private val holdDelayMs: Long = 550L,
    private val holdRepeatMs: Long = 180L,
) {
    /** A press in one direction: x or y is -1 or 1, the other 0. */
    data class Press(val x: Int, val y: Int)

    private var heldX = 0
    private var heldY = 0
    private var pressedAt = 0L
    private var lastRepeatAt = 0L
    private var releasedX = 0
    private var releasedY = 0
    private var releasedAt = Long.MIN_VALUE / 2

    /** The stick is held in a direction (past the press threshold, not yet released). */
    val engaged: Boolean get() = heldX != 0 || heldY != 0

    fun reset() {
        heldX = 0
        heldY = 0
    }

    /** Feeds the stick position; returns a press to dispatch, or null. */
    fun update(
        x: Float,
        y: Float,
        now: Long,
    ): Press? {
        if (engaged) {
            // How far the stick still leans the way it was pressed.
            val lean = if (heldX != 0) x * heldX else y * heldY
            if (lean < releaseThreshold) {
                releasedX = heldX
                releasedY = heldY
                releasedAt = now
                reset()
                return null
            }
            if (now - pressedAt >= holdDelayMs && now - lastRepeatAt >= holdRepeatMs) {
                lastRepeatAt = now
                return Press(heldX, heldY)
            }
            return null
        }
        val ax = abs(x)
        val ay = abs(y)
        if (maxOf(ax, ay) < pressThreshold) return null
        // One direction only: the stronger axis, so a diagonal doesn't press twice.
        val dx = if (ax >= ay) sign(x).toInt() else 0
        val dy = if (ax >= ay) 0 else sign(y).toInt()
        val sinceRelease = now - releasedAt
        val opposite = dx == -releasedX && dy == -releasedY
        if (opposite && sinceRelease < reboundMs && maxOf(ax, ay) < reboundMaxLean) return null
        heldX = dx
        heldY = dy
        pressedAt = now
        lastRepeatAt = now
        return Press(dx, dy)
    }
}
