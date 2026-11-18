package com.omnidroid.app.mobile.shared.compose.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow

@Composable
internal fun chromeButtonSize(): Dp {
    val spec = LocalAdaptiveLayout.current
    // Sized from the wireframes (44 px on a 1040 x 480 landscape frame, 390 px wide portrait):
    // 34 dp on a landscape phone, 40 dp on a portrait phone, 44 dp on larger screens.
    val base =
        when {
            spec.isUltraCompact -> 40.dp
            spec.isCompact -> 34.dp
            else -> 44.dp
        }
    return base * spec.uiScale
}

/** One gap for everything on the right of the top bar: status items, the divider and buttons. */
@Composable
internal fun chromeItemGap(): Dp = chromeButtonSize() * 0.27f

/** One text style for the top bar: search, status read-outs and labels. */
@Composable
internal fun chromeTextStyle(): TextStyle =
    MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.sp * LocalAdaptiveLayout.current.uiScale,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    )

/** Icon size inside a [chromeButtonSize] button, keeping the wireframe's 20 / 44 ratio. */
@Composable
internal fun chromeIconSize(): Dp = chromeButtonSize() * 0.45f

/** Round top bar icon button on the chrome panel colour, darker while pressed. */
@Composable
fun FloatingChromeButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) chromeColors.pressed else chromeColors.panel,
        animationSpec = tween(120),
        label = "chromeButtonPressed",
    )
    Box(
        modifier =
            Modifier
                .size(chromeButtonSize())
                .controllerFocusGlow(CircleShape)
                .clip(CircleShape)
                .background(background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
