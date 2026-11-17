package com.omnidroid.app.mobile.shared.compose.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * How far into the reported cutout safe area content sits. That area spans the whole status-bar
 * depth, well past the camera hole itself, so content only needs [fraction] of it.
 */
enum class CutoutClearance(val fraction: Float) {
    /** Library, settings and other app chrome. */
    Chrome(0.5f),

    /** On-screen pads and the game view during play. */
    InGame(0.7f),
}

/**
 * Display-cutout (camera hole) insets for the given [sides], reduced to the [clearance]
 * fraction of the reported depth.
 *
 * Omnidroid draws edge to edge, so anything pinned to a screen edge - on-screen pads, side
 * rails, full-bleed bars - can end up underneath the hole. The cutout follows the rotation, so
 * the sides a caller needs depend on the current orientation.
 *
 * Devices without a cutout report zero insets, so this is a no-op there.
 */
@Composable
fun rememberHuggingCutoutInsets(
    sides: WindowInsetsSides,
    clearance: CutoutClearance = CutoutClearance.Chrome,
): WindowInsets {
    val cutout = WindowInsets.displayCutout
    return remember(cutout, sides, clearance) { ScaledWindowInsets(cutout.only(sides), clearance.fraction) }
}

/** Keeps this content clear of the display cutout on [sides]. See [rememberHuggingCutoutInsets]. */
@Composable
fun Modifier.hugDisplayCutout(
    sides: WindowInsetsSides,
    clearance: CutoutClearance = CutoutClearance.Chrome,
): Modifier = windowInsetsPadding(rememberHuggingCutoutInsets(sides, clearance))

@Stable
private class ScaledWindowInsets(
    private val insets: WindowInsets,
    private val fraction: Float,
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int =
        (insets.getLeft(density, layoutDirection) * fraction).roundToInt()

    override fun getTop(density: Density): Int = (insets.getTop(density) * fraction).roundToInt()

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int =
        (insets.getRight(density, layoutDirection) * fraction).roundToInt()

    override fun getBottom(density: Density): Int = (insets.getBottom(density) * fraction).roundToInt()

    override fun equals(other: Any?): Boolean =
        other is ScaledWindowInsets && other.insets == insets && other.fraction == fraction

    override fun hashCode(): Int = 31 * insets.hashCode() + fraction.hashCode()
}

/** Gap kept between a display cutout and content beside it. */
val CutoutGap = 12.dp

/** Start and end spacing for content; see [rememberContentEdges]. */
@Immutable
data class ContentEdges(val start: Dp, val end: Dp)

/**
 * Start and end spacing shared by the top bar, Home and other full-width content, so their edges
 * line up. Normally [edge]; on the side of a camera cutout, the cutout's depth plus [CutoutGap].
 * Phones without a cutout report no inset. On a TV, at least [TvSafeMargin.horizontal], since
 * many sets crop the picture edges.
 */
@Composable
fun rememberContentEdges(edge: Dp): ContentEdges {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout
    val left = with(density) { cutout.getLeft(density, direction).toDp() }
    val right = with(density) { cutout.getRight(density, direction).toDp() }
    val base = if (isTelevision()) maxOf(edge, TvSafeMargin.horizontal) else edge
    fun clear(inset: Dp) = if (inset > 0.dp) maxOf(base, inset + CutoutGap) else base
    val (start, end) = if (direction == LayoutDirection.Ltr) left to right else right to left
    return ContentEdges(start = clear(start), end = clear(end))
}

/**
 * Depth of a cutout at the top of the screen (portrait punch-hole phones), or 0. On a TV, the
 * vertical safe margin instead.
 */
@Composable
fun cutoutTopInset(): Dp {
    val density = LocalDensity.current
    val cutout = with(density) { WindowInsets.displayCutout.getTop(density).toDp() }
    return if (isTelevision()) maxOf(cutout, TvSafeMargin.vertical) else cutout
}

/**
 * Margins that keep content clear of the edges a TV may crop (overscan): Android TV's guideline of
 * 48 dp at the sides and 27 dp at the top and bottom.
 */
object TvSafeMargin {
    val horizontal = 48.dp
    val vertical = 27.dp
}

/** True on Android TV / Google TV, where the app is driven from the sofa with a remote. */
@Composable
fun isTelevision(): Boolean =
    LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

/** Edge spacing for app chrome and content: 16 dp in portrait, 18 dp in landscape (wireframes). */
fun contentEdge(portrait: Boolean): Dp = if (portrait) 16.dp else 18.dp
