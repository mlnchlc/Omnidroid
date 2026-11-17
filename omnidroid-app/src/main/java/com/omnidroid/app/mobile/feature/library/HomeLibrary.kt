package com.omnidroid.app.mobile.feature.library

import android.text.format.DateUtils
import com.omnidroid.app.mobile.shared.compose.ui.FloatingChromeButton
import com.omnidroid.app.mobile.shared.compose.ui.WireframeIcons
import com.omnidroid.app.mobile.shared.compose.ui.chromeButtonSize
import com.omnidroid.app.mobile.shared.compose.ui.chromeIconSize
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.omnidroid.app.OmnidroidApplication
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow
import com.omnidroid.app.mobile.feature.gamedetails.PlayTimeStore
import com.omnidroid.app.utils.games.GameUtils
import com.omnidroid.lib.library.db.dao.GamePlayStats
import kotlinx.coroutines.flow.flowOf
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
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.res.stringResource
import com.omnidroid.R
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
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private val HomeHeaderHeight = 30.dp

/** Portrait puts a larger title above the row and the game's actions below it. */
private val HomePortraitHeaderHeight = 88.dp
private val HomePortraitActionsGap = 20.dp
private val HomePortraitActionsHeight = 40.dp + HomePortraitActionsGap
private val HomeCardGap = 14.dp
private val HomeCardCorner = 6.dp
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
        fun from(
            availableWidth: Dp,
            availableHeight: Dp,
            portrait: Boolean,
            reservedHeight: Dp = HomeHeaderHeight,
        ): HomeRowMetrics {
            val maxWidth = availableWidth * if (portrait) 0.6f else 0.4f
            val rowHeight = availableHeight - reservedHeight - HomeRingSpace * 2
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
    onFavoriteToggle: (Game, Boolean) -> Unit,
    onBackdropGameChange: (Game?) -> Unit,
    showControllerGlyph: Boolean = false,
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
        val reserved = if (portrait) HomePortraitHeaderHeight + HomePortraitActionsHeight else HomeHeaderHeight
        val metrics = HomeRowMetrics.from(maxWidth - startPadding - endPadding, maxHeight, portrait, reserved)
        val listState = rememberLazyListState()
        val density = LocalDensity.current
        val viewportWidth = maxWidth - startPadding - endPadding
        // Portrait centres the selected card, with its neighbours peeking in at both edges.
        val centredPadding =
            ((maxWidth - metrics.selectedWidth - HomeRingSpace * 2) / 2).coerceAtLeast(startPadding)
        val rowStart = if (portrait) centredPadding else startPadding
        val rowEnd = if (portrait) centredPadding else endPadding

        /** Scrolls so the selected card sits on its spot: centred in portrait, in view otherwise. */
        suspend fun alignSelection(animate: Boolean) {
            suspend fun scrollTo(index: Int, offset: Int = 0) {
                runCatching {
                    if (animate) listState.animateScrollToItem(index, offset) else listState.scrollToItem(index, offset)
                }
            }
            // Every card before the selected one is small, so it starts at its own index.
            if (portrait) {
                if (!animate && selectedIsCentred(listState, selectedIndex)) return
                return scrollTo(selectedIndex)
            }
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
                    ) ?: return
                val targetIndex = floor(target / step).toInt().coerceIn(0, lastIndex)
                scrollTo(targetIndex, (target - targetIndex * step).roundToInt())
            }
        }

        LaunchedEffect(selectedIndex, metrics, viewportWidth, portrait) { alignSelection(animate = true) }

        if (portrait) {
            val currentSelect by rememberUpdatedState(select)
            val align: suspend (Boolean) -> Unit = { alignSelection(it) }
            val currentAlign by rememberUpdatedState(align)
            LaunchedEffect(listState) {
                var wasScrolling = false
                snapshotFlow {
                    Triple(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                }.collect { (scrolling, _, _) ->
                    val settled = wasScrolling && !scrolling
                    wasScrolling = scrolling
                    if (scrolling) return@collect
                    if (settled) {
                        // A swipe settles on the card nearest the centre, which becomes the selection.
                        val layout = listState.layoutInfo
                        val nearest =
                            layout.visibleItemsInfo.minByOrNull { abs(it.centre() - layout.centre()) }
                                ?: return@collect
                        if (nearest.index != selection.intValue) {
                            currentSelect(nearest.index)
                            return@collect
                        }
                    }
                    // The row moved without a scroll (paging keeps its place by item key while
                    // games load), or settled slightly off: put the selected card back in the centre.
                    currentAlign(false)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (portrait) Arrangement.Center else Arrangement.Top,
        ) {
            if (portrait) {
                HomePortraitHeader(
                    game = selectedGame,
                    continueGame = items.isFeatured(selectedIndex),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(HomePortraitHeaderHeight)
                            .padding(horizontal = startPadding),
                )
            } else {
                HomeSelectionHeader(
                    game = selectedGame,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(HomeHeaderHeight)
                            .padding(start = startPadding + HomeRingSpace, end = endPadding),
                )
            }
            LazyRow(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(metrics.selectedHeight + HomeRingSpace * 2),
                flingBehavior =
                    if (portrait) {
                        rememberSnapFlingBehavior(listState, SnapPosition.Center)
                    } else {
                        ScrollableDefaults.flingBehavior()
                    },
                contentPadding = PaddingValues(start = rowStart, end = rowEnd),
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
            if (portrait) {
                HomePageDots(
                    index = selectedIndex,
                    count = count,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                )
                Spacer(modifier = Modifier.height(HomePortraitActionsGap))
                HomePortraitActions(
                    game = selectedGame,
                    continueGame = items.isFeatured(selectedIndex),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = startPadding),
                    onPlay = { items.peek(selectedIndex)?.let(onPlayGame) },
                    onFavoriteToggle = onFavoriteToggle,
                    onMore = { items.peek(selectedIndex)?.let(onGameLongClick) },
                )
            }
        }
    }
}

private fun LazyListItemInfo.centre(): Int = offset + size / 2

private fun LazyListLayoutInfo.centre(): Int = (viewportStartOffset + viewportEndOffset) / 2

private fun selectedIsCentred(state: LazyListState, index: Int): Boolean {
    val layout = state.layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    return abs(item.centre() - layout.centre()) <= 2
}

@Composable
private fun HomePortraitHeader(
    game: Game?,
    continueGame: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (game == null) return@Column
        if (continueGame) {
            Text(
                text = stringResource(R.string.continue_playing).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
                color = chromeColors.muted,
                maxLines = 1,
            )
        }
        Text(
            text = game.displayName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = LibraryNeonGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = homeGameMeta(game),
            style = MaterialTheme.typography.bodyMedium,
            color = chromeColors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
    }
}

/** Title of the selected game, then "NES · played 12 min. ago · 3h 40m" beside it. */
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = LibraryNeonGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = homeGameMeta(game),
            style = MaterialTheme.typography.bodySmall,
            color = chromeColors.muted,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** "NES · played 12 min. ago · 3h 40m": console, when it was last played and the total play time. */
@Composable
private fun homeGameMeta(game: Game): String {
    val context = LocalContext.current
    val system = remember(game.id, game.systemId) { GameUtils.getSystemShortName(context, game) }
    val playTime = rememberPlayTimeMs(game)
    val played =
        game.lastPlayedAt?.let {
            val ago = DateUtils.getRelativeTimeSpanString(
                it,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            )
            stringResource(R.string.home_played_ago, ago)
        }
    return listOfNotNull(
        system.takeIf { it.isNotBlank() },
        played,
        playTime.takeIf { it > 0L }?.let(::formatPlayTime),
    ).joinToString(" · ")
}

@Composable
private fun rememberPlayTimeMs(game: Game): Long {
    val context = LocalContext.current
    val db = remember(context) { (context.applicationContext as? OmnidroidApplication)?.retrogradeDb }
    val stored = remember(context, game.id) { PlayTimeStore(context).get(game.id) }
    val stats by remember(game.id, db) {
        db?.gameSessionDao()?.observeStatsForGame(game.id) ?: flowOf(GamePlayStats(totalDurationMs = stored))
    }.collectAsState(initial = GamePlayStats(totalDurationMs = stored))
    return maxOf(stats.totalDurationMs, stored)
}

/** Up to five dots showing roughly where the selection sits in the list. */
@Composable
private fun HomePageDots(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count < 2) return
    val dots = count.coerceAtMost(5)
    val active = ((index.toFloat() / (count - 1)) * (dots - 1)).roundToInt()
    Row(modifier = modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(dots) { dot ->
            val selected = dot == active
            Box(
                Modifier
                    .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                    .background(if (selected) LibraryNeonGreen else chromeColors.muted.copy(alpha = 0.45f), CircleShape),
            )
        }
    }
}

/** Wide Continue / Start pill, then round favourite and more-options buttons. */
@Composable
private fun HomePortraitActions(
    game: Game?,
    continueGame: Boolean,
    onPlay: () -> Unit,
    onFavoriteToggle: (Game, Boolean) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Paging only re-emits the game after the database write lands, so flip the heart immediately.
    var favorite by remember(game?.id, game?.isFavorite) { mutableStateOf(game?.isFavorite == true) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onPlay,
            modifier = Modifier.weight(1f).widthIn(max = 240.dp).height(52.dp).controllerFocusGlow(CircleShape),
            shape = CircleShape,
            color = LibraryNeonGreen,
            contentColor = Color(0xFF0A0A0A),
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(WireframeIcons.Play, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (continueGame) R.string.continue_playing else R.string.library_console_start),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        HomeRoundButton(
            onClick = {
                game?.let {
                    favorite = !favorite
                    onFavoriteToggle(it, favorite)
                }
            },
        ) {
            Icon(
                imageVector = if (favorite) WireframeIcons.HeartFilled else WireframeIcons.Heart,
                contentDescription =
                    stringResource(
                        if (favorite) R.string.game_context_menu_remove_from_favorites else R.string.game_context_menu_add_to_favorites,
                    ),
                tint = if (favorite) LibraryNeonGreen else chromeColors.content,
            )
        }
        HomeRoundButton(onClick = onMore) {
            Icon(WireframeIcons.More, contentDescription = stringResource(R.string.game_settings), tint = chromeColors.content)
        }
    }
}

@Composable
private fun HomeRoundButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp).controllerFocusGlow(CircleShape),
        shape = CircleShape,
        color = chromeColors.panel,
        border = BorderStroke(1.dp, chromeColors.panelBorder),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
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
    val corner = HomeCardCorner
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
        restingCorner = HomeCardCorner,
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

/** Home's Row / Grid / List switch, sized to sit in the top bar beside search. */
@Composable
internal fun HomeViewSwitch(
    view: HomeView,
    onViewChange: (HomeView) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Same height as the other top bar controls; segments in the wireframe's 44 : 36 proportion.
    val height = chromeButtonSize()
    val inset = height * 0.1f
    Row(
        modifier =
            modifier
                .height(height)
                .background(chromeColors.panel, CircleShape)
                .border(BorderStroke(1.dp, chromeColors.panelBorder), CircleShape)
                .padding(inset)
                .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HomeView.entries.forEach { option ->
            val selected = option == view
            Box(
                modifier =
                    Modifier
                        .size(width = height, height = height - inset * 2)
                        .controllerFocusGlow(CircleShape)
                        .background(if (selected) chromeColors.panelSelected else Color.Transparent, CircleShape)
                        .selectable(selected = selected, role = Role.Tab) { onViewChange(option) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = option.icon(),
                    contentDescription = stringResource(option.labelRes()),
                    modifier = Modifier.size(chromeIconSize()),
                    tint = if (selected) LibraryNeonGreen else chromeColors.muted,
                )
            }
        }
    }
}

/**
 * One round button showing the current Home view; each press moves to the next. Used in the
 * portrait top bar, where the three-way switch does not fit beside search and the other buttons.
 */
@Composable
internal fun HomeViewCycleButton(
    view: HomeView,
    onViewChange: (HomeView) -> Unit,
) {
    val next = HomeView.entries[(view.ordinal + 1) % HomeView.entries.size]
    FloatingChromeButton(onClick = { onViewChange(next) }) {
        Icon(
            imageVector = view.icon(),
            contentDescription = stringResource(R.string.home_view_switch, stringResource(view.labelRes())),
            modifier = Modifier.size(chromeIconSize()),
            tint = LibraryNeonGreen,
        )
    }
}

private fun HomeView.icon() =
    when (this) {
        HomeView.ROW -> WireframeIcons.ViewRow
        HomeView.GRID -> WireframeIcons.ViewGrid
        HomeView.LIST -> WireframeIcons.ViewList
    }

private fun HomeView.labelRes() =
    when (this) {
        HomeView.ROW -> R.string.library_view_row
        HomeView.GRID -> R.string.library_view_grid
        HomeView.LIST -> R.string.library_view_list
    }
