package com.omnidroid.app.mobile.feature.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.paging.compose.LazyPagingItems
import com.omnidroid.app.mobile.shared.compose.ui.GameCardInfoStyle
import com.omnidroid.app.mobile.shared.compose.ui.LibraryGameCardAspectRatio
import com.omnidroid.app.mobile.shared.compose.ui.LibraryNeonGreen
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import com.omnidroid.app.mobile.shared.controller.LocalControllerNavigation
import com.omnidroid.lib.library.db.entity.Game
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.roundToInt

private val HomeHeaderHeight = 30.dp
private val HomeCardGap = 14.dp
private val HomeRingWidth = 3.dp
private val HomeRingOutset = 3.dp

/** Room around every card for the selection ring, so the ring is never clipped by the row. */
private val HomeRingSpace = HomeRingWidth + HomeRingOutset
private const val HomeSmallCardScale = 0.86f

/** Edge spacing for Home content when nothing (such as a camera cutout) needs more room. */
internal val HomeEdgePadding = 24.dp
internal val HomePortraitEdgePadding = 16.dp

/** Gap kept between a display cutout and Home content. */
internal val HomeCutoutGap = 12.dp

/**
 * Card sizes for the Home row, taken from the height left over after the top bar, console row and
 * hint bar so no band of the screen stays empty. In portrait the width caps the selected card.
 */
internal data class HomeRowMetrics(
    val selectedWidth: Dp,
    val selectedHeight: Dp,
    val smallWidth: Dp,
    val smallHeight: Dp,
) {
    companion object {
        fun from(availableWidth: Dp, availableHeight: Dp, portrait: Boolean): HomeRowMetrics {
            val maxWidth = availableWidth * if (portrait) 0.6f else 0.4f
            val rowHeight = availableHeight - HomeHeaderHeight - HomeRingSpace * 2
            val height = min(rowHeight, maxWidth / LibraryGameCardAspectRatio).coerceAtLeast(96.dp)
            val width = height * LibraryGameCardAspectRatio
            return HomeRowMetrics(
                selectedWidth = width,
                selectedHeight = height,
                smallWidth = width * HomeSmallCardScale,
                smallHeight = height * HomeSmallCardScale,
            )
        }
    }
}

/**
 * Scroll position, in pixels from the first card, that keeps the card at [index] fully in view
 * with part of its neighbours showing, or null when it is already in view.
 *
 * Every card before the selected one is small, so the selected card starts at `index * step`.
 */
internal fun homeRowScrollTarget(
    index: Int,
    count: Int,
    currentScroll: Float,
    step: Float,
    smallWidth: Float,
    selectedWidth: Float,
    gap: Float,
    viewport: Float,
): Float? {
    if (count <= 0 || viewport <= 0f) return null
    val start = index * step
    val end = start + selectedWidth
    val peek = gap + smallWidth * 0.4f
    val before = if (index > 0) peek else 0f
    val after = if (index < count - 1) peek else 0f
    return when {
        start - before < currentScroll -> (start - before).coerceAtLeast(0f)
        end + after > currentScroll + viewport -> end + after - viewport
        else -> null
    }
}

/**
 * Console-style Home row. The selected card grows, with its title and play stats above the row.
 * Like the console carousel, the row takes focus as one item and moves the selection with the
 * D-pad, so focus never lands on a card that has scrolled away.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun HomeLibrary(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    games: LazyPagingItems<Game>,
    featured: Game?,
    selection: MutableIntState,
    portrait: Boolean,
    startPadding: Dp,
    endPadding: Dp,
    focusRequester: FocusRequester,
    onOpenGame: (Game) -> Unit,
    onOpenContinueGame: (Game) -> Unit,
    onPlayGame: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onBackdropGameChange: (Game?) -> Unit,
) {
    val items = remember(games, featured) { LibraryPagedItems(games, featured) }
    val count = items.count
    val lastIndex = (count - 1).coerceAtLeast(0)
    val selectedIndex = selection.intValue.coerceIn(0, lastIndex)
    LaunchedEffect(count) {
        if (count > 0 && selection.intValue > lastIndex) selection.intValue = lastIndex
    }
    val selectedGame = items.peek(selectedIndex)

    val navigation = LocalControllerNavigation.current
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(hasFocus, selectedGame?.id) {
        if (hasFocus) navigation?.setFocusedGame(selectedGame)
    }
    val currentOnBackdropGameChange by rememberUpdatedState(onBackdropGameChange)
    LaunchedEffect(selectedGame?.id) {
        delay(160)
        currentOnBackdropGameChange(selectedGame)
    }
    DisposableEffect(Unit) {
        onDispose { currentOnBackdropGameChange(null) }
    }

    val select: (Int) -> Unit = { index -> selection.intValue = index.coerceIn(0, lastIndex) }
    val onCardClick: (Int, Game) -> Unit = { index, game ->
        when {
            index != selectedIndex -> select(index)
            items.isFeatured(index) -> onOpenContinueGame(game)
            else -> onOpenGame(game)
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    hasFocus = focusState.isFocused
                    if (!focusState.isFocused) navigation?.setFocusedGame(null)
                }
                .onKeyEvent { event ->
                    when (event.key) {
                        // At either end the key is left unconsumed so focus can move on.
                        Key.DirectionLeft -> {
                            if (event.type != KeyEventType.KeyDown || selectedIndex <= 0) return@onKeyEvent false
                            select(selectedIndex - 1)
                            true
                        }
                        Key.DirectionRight -> {
                            if (event.type != KeyEventType.KeyDown || selectedIndex >= lastIndex) return@onKeyEvent false
                            select(selectedIndex + 1)
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.ButtonA -> {
                            if (event.type == KeyEventType.KeyUp) items.peek(selectedIndex)?.let(onPlayGame)
                            true
                        }
                        else -> false
                    }
                }
                .focusable(),
    ) {
        val metrics = HomeRowMetrics.from(maxWidth - startPadding - endPadding, maxHeight, portrait)
        val listState = rememberLazyListState()
        val density = LocalDensity.current
        val viewportWidth = maxWidth - startPadding - endPadding

        LaunchedEffect(selectedIndex, metrics, viewportWidth) {
            with(density) {
                val ring = HomeRingSpace.toPx() * 2
                val gap = HomeCardGap.toPx()
                val small = metrics.smallWidth.toPx() + ring
                val step = small + gap
                val current = listState.firstVisibleItemIndex * step + listState.firstVisibleItemScrollOffset
                val target =
                    homeRowScrollTarget(
                        index = selectedIndex,
                        count = count,
                        currentScroll = current,
                        step = step,
                        smallWidth = small,
                        selectedWidth = metrics.selectedWidth.toPx() + ring,
                        gap = gap,
                        viewport = viewportWidth.toPx(),
                    ) ?: return@LaunchedEffect
                val targetIndex = floor(target / step).toInt().coerceIn(0, lastIndex)
                val offset = (target - targetIndex * step).roundToInt()
                runCatching { listState.animateScrollToItem(targetIndex, offset) }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (portrait) Arrangement.Center else Arrangement.Top,
        ) {
            HomeSelectionHeader(
                game = selectedGame,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(HomeHeaderHeight)
                        .padding(start = startPadding + HomeRingSpace, end = endPadding),
            )
            LazyRow(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(metrics.selectedHeight + HomeRingSpace * 2),
                contentPadding = PaddingValues(start = startPadding, end = endPadding),
                horizontalArrangement = Arrangement.spacedBy(HomeCardGap),
                verticalAlignment = Alignment.Top,
            ) {
                items(
                    count = count,
                    // Paging can shrink the list before this row recomposes with the new count.
                    key = { index -> if (index < items.count) items.key(index) else "stale-$index" },
                    contentType = { index -> if (index < items.count) items.contentType(index) else null },
                ) { index ->
                    HomeRowCard(
                        game = items[index],
                        selected = index == selectedIndex,
                        featured = items.isFeatured(index),
                        metrics = metrics,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { game -> onCardClick(index, game) },
                        onLongClick = onGameLongClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSelectionHeader(
    game: Game?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (game == null) return@Row
        Text(
            text = game.displayName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LibraryNeonGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(14.dp))
        ConsoleGameStats(game = game, tint = chromeColors.muted)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeRowCard(
    game: Game?,
    selected: Boolean,
    featured: Boolean,
    metrics: HomeRowMetrics,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (Game) -> Unit,
    onLongClick: (Game) -> Unit,
) {
    val width by animateDpAsState(
        targetValue = if (selected) metrics.selectedWidth else metrics.smallWidth,
        animationSpec = tween(180),
        label = "homeCardWidth",
    )
    val height by animateDpAsState(
        targetValue = if (selected) metrics.selectedHeight else metrics.smallHeight,
        animationSpec = tween(180),
        label = "homeCardHeight",
    )
    // Matches the resting corner of the library cards (LibraryGameCardItem).
    val corner = 4.dp
    val cardModifier =
        Modifier
            .padding(HomeRingSpace)
            .size(width, height)
            .then(if (selected) Modifier.homeSelectionRing(corner) else Modifier)
            // The row holds focus for the whole carousel; cards are for touch only.
            .focusProperties { canFocus = false }
    if (game == null) {
        Box(
            cardModifier
                .background(chromeColors.listRow, RoundedCornerShape(corner))
                .clearAndSetSemantics {},
        )
        return
    }
    LibraryGameCardItem(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        game = game,
        modifier = cardModifier,
        continueAction = featured,
        infoStyle = GameCardInfoStyle.OVERLAY,
        fillCard = true,
        decodeWidth = metrics.selectedWidth,
        onClick = { onClick(game) },
        onLongClick = { onLongClick(game) },
    )
}

/** Accent ring drawn just outside the card, over the gap reserved by [HomeRingSpace]. */
private fun Modifier.homeSelectionRing(corner: Dp): Modifier =
    drawWithContent {
        drawContent()
        val outset = HomeRingOutset.toPx() + HomeRingWidth.toPx() / 2
        drawRoundRect(
            color = LibraryNeonGreen,
            topLeft = Offset(-outset, -outset),
            size = Size(size.width + outset * 2, size.height + outset * 2),
            cornerRadius = CornerRadius(corner.toPx() + outset),
            style = Stroke(width = HomeRingWidth.toPx()),
        )
    }
