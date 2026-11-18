package com.omnidroid.app.mobile.shared.compose.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnidroid.R
import com.omnidroid.app.mobile.feature.library.NotchOrientation
import com.omnidroid.app.mobile.feature.library.SyncProgressNotch
import com.omnidroid.lib.core.SyncProgress

/** Top bar width below which the status read-outs give way to the controls. */
private val StatusMinBarWidth = 700.dp

/** Shared app chrome. Screens can supply controls without changing its styling. */
@Composable
fun AppTopBar(
    gamepadConnected: Boolean,
    portrait: Boolean,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    syncProgress: SyncProgress? = null,
    onBackPressed: (() -> Unit)? = null,
    controlsVisible: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    showStatus: Boolean = true,
    /** Page title shown beside the Back button, e.g. "Settings". */
    title: String? = null,
) {
    val spec = LocalAdaptiveLayout.current
    val height by animateDpAsState(
        targetValue = if (controlsVisible && bottomContent != null) spec.topBarPortraitHeight else spec.topBarRowHeight,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "topBarHeight",
    )
    // Below a punch-hole at the top in portrait; beside one in landscape, with the same start and end
    // spacing as the content underneath so their edges line up.
    val topInset = cutoutTopInset()
    val edges = rememberContentEdges(contentEdge(portrait))
    val syncAlignment = if (portrait) Alignment.Top else Alignment.Bottom
    val syncOrigin = TransformOrigin(0.5f, if (portrait) 0f else 1f)

    CompositionLocalProvider(LocalContentColor provides chromeContentColor) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            // On narrow bars (small 4:3 handhelds, or a large UI scale) the read-outs would crowd
            // search and the buttons, so they make way while the controls are up.
            val statusFits = !controlsVisible || trailingContent == null || maxWidth >= StatusMinBarWidth * spec.uiScale
            val status = showStatus && statusFits
            // The fade is part of the chrome, independent of control visibility and clipping.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height + topInset)
                        .background(Brush.verticalGradient(listOf(homeChromeColor, Color.Transparent))),
            )
            Column(
                modifier =
                    Modifier
                        .padding(top = topInset)
                        .fillMaxWidth()
                        .height(height)
                        .clipToBounds(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(spec.topBarRowHeight).padding(start = edges.start, end = edges.end),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = controlsVisible && leadingContent != null,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                        ) { leadingContent?.invoke() }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = onBackPressed != null,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FloatingChromeButton(onClick = { onBackPressed?.invoke() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        modifier = Modifier.size(18.dp * spec.uiScale),
                                    )
                                }
                                if (title != null) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = chromeContentColor,
                                        maxLines = 1,
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (status) SystemStatusIndicators(gamepadConnected = gamepadConnected)
                    AnimatedVisibility(
                        visible = controlsVisible && trailingContent != null,
                        enter = fadeIn(tween(200)) + expandHorizontally(tween(300, easing = FastOutSlowInEasing), expandFrom = Alignment.End),
                        exit = fadeOut(tween(200)) + shrinkHorizontally(tween(300, easing = FastOutSlowInEasing), shrinkTowards = Alignment.End),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (status) {
                                // Separates the status read-outs from the buttons.
                                Spacer(modifier = Modifier.width(chromeItemGap()))
                                ChromeDivider(Modifier.height(chromeButtonSize() * 0.55f), vertical = true)
                                Spacer(modifier = Modifier.width(chromeItemGap()))
                            } else {
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            trailingContent?.invoke()
                        }
                    }
                }
                AnimatedVisibility(
                    visible = controlsVisible && bottomContent != null,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(spec.topBarRowHeight).padding(start = edges.start, end = edges.end),
                            contentAlignment = Alignment.Center,
                        ) { bottomContent?.invoke() }
                    }
                }
            }

            AnimatedVisibility(
                visible = syncProgress != null,
                enter =
                    fadeIn(tween(220)) +
                        expandVertically(tween(320, easing = FastOutSlowInEasing), expandFrom = syncAlignment) +
                        scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.85f, transformOrigin = syncOrigin),
                exit =
                    fadeOut(tween(200)) +
                        shrinkVertically(tween(280, easing = FastOutSlowInEasing), shrinkTowards = syncAlignment) +
                        scaleOut(tween(280, easing = FastOutSlowInEasing), targetScale = 0.85f, transformOrigin = syncOrigin),
                modifier =
                    Modifier
                        .align(if (portrait) Alignment.TopCenter else Alignment.BottomCenter)
                        .padding(top = if (portrait) height + topInset else 0.dp, bottom = if (portrait) 0.dp else bottomPadding)
                        .fillMaxWidth(0.5f),
            ) {
                SyncProgressNotch(
                    syncProgress = syncProgress,
                    orientation = if (portrait) NotchOrientation.TOP else NotchOrientation.BOTTOM,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
