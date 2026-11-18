package com.omnidroid.app.mobile.feature.gamemenu.states

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.omnidroid.app.mobile.shared.compose.ui.accentColor

/** The save slot preview to show large beside the menu, set while a slot row has focus. */
@Stable
class SlotPreviewState {
    var bitmap by mutableStateOf<Bitmap?>(null)
}

val LocalSlotPreview = staticCompositionLocalOf<SlotPreviewState?> { null }

/**
 * Large preview of the focused save slot, drawn over the game beside the side menu, so a
 * controller user can see which save they are about to load or overwrite.
 */
@Composable
fun BoxScope.SlotPreviewBeside(state: SlotPreviewState) {
    val current = state.bitmap
    // Keep the last image while the preview fades out (a plain holder: no state write while
    // composing).
    val last = remember { LastBitmap() }
    if (current != null) last.bitmap = current
    val shown = current ?: last.bitmap
    AnimatedVisibility(
        visible = current != null,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.96f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.96f),
    ) {
        shown?.let { bitmap ->
            val shape = RoundedCornerShape(12.dp)
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        // Fits inside the space beside the menu whatever the shape: a save made
                        // in portrait is tall, one made in landscape is wide.
                        .padding(28.dp)
                        .aspectRatio(bitmap.width.coerceAtLeast(1) / bitmap.height.coerceAtLeast(1).toFloat())
                        .clip(shape)
                        .border(2.dp, accentColor, shape),
            )
        }
    }
}

private class LastBitmap {
    var bitmap: Bitmap? = null
}
