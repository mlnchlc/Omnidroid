package com.omnidroid.app.mobile.shared.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.omnidroid.app.mobile.shared.compose.ui.chromeContentColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnidroid.lib.library.db.entity.Game

/**
 * Controller / D-pad focus ring: a crisp ring in the content colour (white in dark mode) drawn just
 * inside the element. Inside, so lists, cards and docks that clip their children never cut it; and
 * not green, so it stays visible on green buttons and is never mistaken for a "selected" border.
 */
fun Modifier.controllerFocusGlow(
    shape: Shape = RoundedCornerShape(12.dp),
    strokeWidth: Dp = 2.dp,
    inset: Dp = 2.dp,
    intensity: Float = 1f,
): Modifier =
    composed {
        var focused by remember { mutableStateOf(false) }
        val progress by animateFloatAsState(
            targetValue = if (focused) 1f else 0f,
            animationSpec = tween(120),
            label = "controllerFocus",
        )
        val color = chromeContentColor
        this
            .onFocusChanged { focused = it.isFocused }
            .drawWithContent {
                drawContent()
                val alpha = progress * intensity
                if (alpha <= 0f) return@drawWithContent
                val stroke = strokeWidth.toPx()
                val offset = inset.toPx() + stroke / 2f
                val ringSize = Size(size.width - offset * 2f, size.height - offset * 2f)
                if (ringSize.minDimension <= 0f) return@drawWithContent
                val outline = shape.createOutline(ringSize, layoutDirection, this)
                translate(offset, offset) {
                    drawOutline(outline, color.copy(alpha = alpha), style = Stroke(width = stroke))
                }
            }
    }

fun Modifier.reportControllerGame(game: Game): Modifier =
    composed {
        val navigation = LocalControllerNavigation.current
        onFocusChanged { focusState ->
            if (navigation == null) return@onFocusChanged
            if (focusState.isFocused) {
                navigation.setFocusedGame(game)
            } else if (navigation.focusedGame.value?.id == game.id) {
                navigation.setFocusedGame(null)
            }
        }
    }
