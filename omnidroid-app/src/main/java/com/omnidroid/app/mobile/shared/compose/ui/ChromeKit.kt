package com.omnidroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Fill and content colours of a selectable item (segment, chip, section pill). */
@Immutable
data class SelectionColors(
    val container: Color,
    val content: Color,
)

/**
 * The selected look used everywhere: filled with the accent, with [onAccentColor] text and icons.
 * Unselected items sit on [unselected] with the chrome content colour.
 */
@Composable
fun selectionColors(
    selected: Boolean,
    unselected: Color = Color.Transparent,
): SelectionColors =
    if (selected) {
        SelectionColors(accentColor, onAccentColor)
    } else {
        SelectionColors(unselected, chromeColors.content)
    }

/** Corner radii shared by the chrome: group cards, tiles, list rows and covers. */
object ChromeShapes {
    val CardCorner = 12.dp
    val TileCorner = 10.dp
    val RowCorner = 8.dp
    val CoverCorner = 6.dp

    val Card = RoundedCornerShape(CardCorner)
    val Tile = RoundedCornerShape(TileCorner)
    val Row = RoundedCornerShape(RowCorner)
    val Cover = RoundedCornerShape(CoverCorner)
}

/** A 1 dp chrome divider: a full-height line when [vertical], else a full-width one. */
@Composable
fun ChromeDivider(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val size = if (vertical) Modifier.width(1.dp).fillMaxHeight() else Modifier.height(1.dp).fillMaxWidth()
    Box(modifier.then(size).background(chromeColors.divider))
}
