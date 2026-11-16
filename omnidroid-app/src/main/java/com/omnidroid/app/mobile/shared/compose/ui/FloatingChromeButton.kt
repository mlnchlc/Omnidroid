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
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow

@Composable
internal fun chromeButtonSize(): Dp {
    val spec = LocalAdaptiveLayout.current
    // 44 dp round buttons, close to Android's 48 dp touch-target guidance and easy to hit on a TV.
    val base = if (spec.isUltraCompact) 40.dp else 44.dp
    return base * spec.uiScale
}

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
