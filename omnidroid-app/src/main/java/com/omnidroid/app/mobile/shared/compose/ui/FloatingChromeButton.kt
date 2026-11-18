package com.omnidroid.app.mobile.shared.compose.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
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

/** One text style for the chrome (top bar, Home, menus): 13 sp, scaled with the UI scale setting. */
@Composable
internal fun chromeTextStyle(weight: FontWeight = FontWeight.Medium): TextStyle =
    MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.sp * LocalAdaptiveLayout.current.uiScale,
        fontWeight = weight,
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
    RoundIconButton(onClick = onClick, content = content)
}

/**
 * The app's round icon button: top bar, Home's actions, the game menu. [fill] is the resting colour
 * (transparent for an outline-only button); it darkens while pressed. [bordered] adds the 1 dp panel
 * border.
 */
@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = chromeButtonSize(),
    fill: Color = chromeColors.panel,
    bordered: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) chromeColors.pressed else fill,
        animationSpec = tween(120),
        label = "roundIconButtonPressed",
    )
    val border = chromeColors.panelBorder
    Box(
        modifier =
            modifier
                .size(size)
                .controllerFocusGlow(CircleShape)
                .clip(CircleShape)
                .background(background)
                .then(if (bordered) Modifier.border(1.dp, border, CircleShape) else Modifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides chromeContentColor) { content() }
    }
}
