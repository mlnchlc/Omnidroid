package com.omnidroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A landscape rail beside a content pane, split by a [ChromeDivider] (Settings sections, the Profile
 * summary). The rail takes about a quarter of the width, between [minRail] and [maxRail], so small
 * 4:3 screens keep room for the content. Each slot gets the modifier that sizes it.
 */
@Composable
fun RailScaffold(
    modifier: Modifier = Modifier,
    minRail: Dp = 168.dp,
    maxRail: Dp = 240.dp,
    rail: @Composable (Modifier) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val railWidth = (maxWidth * 0.26f).coerceIn(minRail, maxRail)
        Row {
            rail(Modifier.width(railWidth).fillMaxHeight())
            ChromeDivider(vertical = true)
            content(Modifier.weight(1f).fillMaxHeight())
        }
    }
}
