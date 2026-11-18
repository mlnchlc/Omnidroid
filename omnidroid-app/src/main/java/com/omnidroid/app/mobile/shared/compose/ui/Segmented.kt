package com.omnidroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow

/**
 * A pill of mutually exclusive segments: settings choices, Home's Console / Grid / List switch.
 * The selected segment is filled with the accent ([selectionColors]); [content] draws each
 * segment's label or icon in the colours it is given.
 *
 * [segmentWidth] fixes every segment's width (icon segments); null sizes each to its content with
 * [segmentPadding] either side.
 */
@Composable
fun <T> SegmentedPills(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    segmentHeight: Dp = 32.dp,
    segmentWidth: Dp? = null,
    segmentPadding: Dp = 14.dp,
    inset: Dp = 3.dp,
    background: Color = Color.Transparent,
    enabled: Boolean = true,
    role: Role = Role.RadioButton,
    content: @Composable (option: T, colors: SelectionColors) -> Unit,
) {
    Row(
        modifier =
            modifier
                .background(background, CircleShape)
                .border(BorderStroke(1.dp, chromeColors.panelBorder), CircleShape)
                .padding(inset)
                .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val colors = selectionColors(isSelected)
            Box(
                modifier =
                    Modifier
                        .height(segmentHeight)
                        .then(if (segmentWidth != null) Modifier.width(segmentWidth) else Modifier)
                        .controllerFocusGlow(CircleShape, onFill = isSelected)
                        .background(colors.container, CircleShape)
                        .selectable(selected = isSelected, enabled = enabled, role = role) { onSelect(option) }
                        .then(if (segmentWidth == null) Modifier.padding(horizontal = segmentPadding) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                content(option, colors)
            }
        }
    }
}
