package com.omnidroid.app.mobile.feature.library

import com.omnidroid.app.mobile.shared.compose.ui.ChromeDivider
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import com.omnidroid.app.mobile.shared.compose.ui.rememberMinuteClock
import com.omnidroid.app.mobile.shared.compose.ui.LocalMinuteClock
import com.omnidroid.app.mobile.shared.compose.ui.accentColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import android.content.Context
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.omnidroid.app.utils.android.settings.booleanPreferenceState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.omnidroid.BuildConfig
import com.omnidroid.R
import com.omnidroid.app.shared.settings.StorageFrameworkPickerLauncher
import com.omnidroid.app.utils.android.benchmarkTag
import com.omnidroid.app.mobile.shared.compose.ui.homeChromeBackground
import com.omnidroid.app.mobile.shared.compose.ui.isTelevision
import com.omnidroid.app.mobile.shared.compose.ui.TvSafeMargin
import com.omnidroid.app.mobile.shared.compose.ui.OmnidroidEmptyView
import com.omnidroid.app.mobile.shared.compose.ui.LocalAdaptiveLayout
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import com.omnidroid.app.mobile.shared.compose.ui.hugDisplayCutout
import com.omnidroid.app.mobile.shared.compose.ui.WireframeIcons
import com.omnidroid.app.mobile.shared.compose.ui.contentEdge
import com.omnidroid.app.mobile.shared.compose.ui.rememberContentEdges
import com.omnidroid.app.mobile.shared.compose.ui.ContentEdges
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.omnidroid.app.mobile.shared.controller.LocalControllerNavigation
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow
import com.omnidroid.lib.library.GameSystem
import com.omnidroid.lib.library.MetaSystemID
import com.omnidroid.lib.preferences.SharedPreferencesHelper
import com.omnidroid.lib.library.db.entity.Game
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


fun libraryGridRows(zoomDensity: Int, availableHeight: Dp, isSquare: Boolean = false): Int {
    if (isSquare) {
        return when (zoomDensity) {
            0 -> 2
            1 -> 3
            else -> 4
        }
    }
    val h = availableHeight.value
    return when {
        h < 440f -> {
            when (zoomDensity) {
                0 -> 1
                1 -> 2
                else -> 3
            }
        }
        h < 640f -> {
            when (zoomDensity) {
                0 -> 2
                1 -> 3
                else -> 5
            }
        }
        h < 880f -> {
            when (zoomDensity) {
                0 -> 3
                1 -> 5
                else -> 7
            }
        }
        else -> {
            when (zoomDensity) {
                0 -> 3
                1 -> (h / 140f).toInt().coerceIn(5, 6)
                else -> (h / 90f).toInt().coerceIn(7, 9)
            }
        }
    }
}

fun libraryPortraitColumns(zoomDensity: Int, availableWidth: Dp, isSquare: Boolean = false): Int {
    if (isSquare) {
        return when (zoomDensity) {
            0 -> 3
            1 -> 4
            else -> 5
        }
    }
    val w = availableWidth.value
    val cardWidth =
        when (zoomDensity) {
            0 -> if (w < 400f) 110f else 150f
            1 -> if (w < 400f) 85f else 112f
            else -> if (w < 400f) 65f else 84f
        }
    return (w / cardWidth).toInt().coerceIn(2, 6)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: LibraryViewModel,
    onGameClick: (Game) -> Unit,
    onContinueClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onFavoriteToggle: (Game, Boolean) -> Unit,
    controllerConnected: Boolean = false,
    portraitColumnWidth: Dp = Dp.Unspecified,
    backdropTopBleed: Dp = 0.dp,
    onOpenContinueGame: (Game) -> Unit = onGameClick,
) {
    val state = viewModel.state.collectAsState().value
    val games = viewModel.games.collectAsLazyPagingItems()
    if (BuildConfig.BUILD_TYPE == "benchmarkRelease" || BuildConfig.BUILD_TYPE == "nonMinifiedRelease") {
        ReportDrawnWhen { games.loadState.refresh is LoadState.NotLoading }
    }
    var backdropGame by remember { mutableStateOf<Game?>(null) }
    val consoleSelection =
        rememberConsoleSelection(listOf(state.filter, state.searchQuery, state.continueGame?.id, state.sort))
    val pagedScrollState = key(state.filter, state.searchQuery, state.sort) {
        rememberLibraryPagedScrollState()
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .homeChromeBackground(),
    ) {
        val portrait = maxHeight > maxWidth
        // Follows the Dynamic background setting, which also colours the game screen and menu.
        val dynamicBackground = booleanPreferenceState(R.string.pref_key_enable_immersive_mode, false).value
        if (dynamicBackground) {
            ConsoleBackdrop(
                game = backdropGame,
                modifier =
                    Modifier
                        .matchParentSize()
                        .bleedTop(backdropTopBleed),
            )
        }
        // Each orientation shows the view chosen for it.
        LaunchedEffect(portrait) { viewModel.setPortrait(portrait) }
        // Upside-down portrait puts a punch-hole at the bottom, under the console row.
        val cutoutBottom =
            with(LocalDensity.current) { WindowInsets.displayCutout.getBottom(this).toDp() }
                .let { if (isTelevision()) maxOf(it, TvSafeMargin.vertical) else it }
        val edges = rememberContentEdges(contentEdge(portrait))
        // The carousel's Start button, which Down from the carousel goes to.
        val homeActionsRequester = remember { FocusRequester() }
        val minuteClock = rememberMinuteClock()
        CompositionLocalProvider(LocalMinuteClock provides minuteClock) {
        Column(
            modifier =
                Modifier
                    .padding(bottom = cutoutBottom)
                    .then(
                        if (portrait && portraitColumnWidth != Dp.Unspecified) {
                            Modifier.width(portraitColumnWidth)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    )
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
        ) {
            LibraryGames(
                modifier = Modifier.weight(1f),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                state = state,
                games = games,
                onGameClick = onGameClick,
                onOpenContinueGame = onOpenContinueGame,
                onContinueClick = onContinueClick,
                onGameLongClick = onGameLongClick,
                onFavoriteToggle = onFavoriteToggle,
                onSync = { viewModel.syncLibrary(it) },
                onClearSearch = { viewModel.clearSearchQuery() },
                onZoomDensityChange = { viewModel.setZoomDensity(it) },
                onBackdropGameChange = { backdropGame = it },
                consoleSelection = consoleSelection,
                pagedScrollState = pagedScrollState,
                requestInitialFocus = controllerConnected,
                portrait = portrait,
                edges = edges,
                homeActionsRequester = homeActionsRequester,
            )
            HomeConsoleRow(
                filter = state.filter,
                systems = state.sidebarSystems,
                selectedGameCount = state.selectedSystemGameCount,
                portrait = portrait,
                edges = edges,
                onFilterSelected = { viewModel.selectFilter(it) },
            )
        }
        }
    }
}

private val DockFadeLength = 14.dp

/** Fades scrolling dock content into its edges instead of clipping it against an invisible line. */
private fun Modifier.dockEdgeFade(
    horizontal: Boolean,
    fadeStart: () -> Boolean,
    fadeEnd: () -> Boolean,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val length = DockFadeLength.toPx()
            val extent = if (horizontal) size.width else size.height
            fun mask(fromTransparent: Boolean, start: Float) {
                val colors = if (fromTransparent) listOf(Color.Transparent, Color.Black) else listOf(Color.Black, Color.Transparent)
                val brush =
                    if (horizontal) {
                        Brush.horizontalGradient(colors, startX = start, endX = start + length)
                    } else {
                        Brush.verticalGradient(colors, startY = start, endY = start + length)
                    }
                drawRect(
                    brush = brush,
                    topLeft = if (horizontal) Offset(start, 0f) else Offset(0f, start),
                    size = if (horizontal) Size(length, size.height) else Size(size.width, length),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (fadeStart()) mask(fromTransparent = true, start = 0f)
            if (fadeEnd()) mask(fromTransparent = false, start = extent - length)
        }

@Composable
internal fun SidebarIconButton(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val adaptiveSpec = LocalAdaptiveLayout.current
    Surface(
        // The selectable Surface tells screen readers which console or filter is showing.
        selected = selected,
        onClick = onClick,
        modifier = Modifier.size(adaptiveSpec.sidebarButtonSize).controllerFocusGlow(CircleShape),
        shape = CircleShape,
        color = chromeColors.panel,
        tonalElevation = 0.dp,
        border =
            if (selected) {
                BorderStroke(2.dp, accentColor)
            } else {
                BorderStroke(1.dp, chromeColors.panelBorder)
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(adaptiveSpec.sidebarIconSize),
                tint = if (selected) accentColor else chromeColors.muted,
            )
        }
    }
}

/**
 * Consoles sit on the same charcoal as the other dock buttons and only take their brand colour
 * when selected. With [label] set, the selected button widens into a pill showing it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SidebarSystemLogoButton(
    meta: MetaSystemID,
    selected: Boolean,
    onClick: () -> Unit,
    label: String? = null,
) {
    val adaptiveSpec = LocalAdaptiveLayout.current
    val buttonSize = adaptiveSpec.sidebarButtonSize
    val dockIdle = chromeColors.panel
    val background by animateColorAsState(
        targetValue = if (selected) Color(meta.color()) else dockIdle,
        animationSpec = tween(220),
        label = "dockConsoleColor",
    )
    Surface(
        modifier =
            Modifier
                .height(buttonSize)
                .widthIn(min = buttonSize)
                .controllerFocusGlow(CircleShape)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = background,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            Row(
                modifier = Modifier.animateContentSize(tween(220, easing = FastOutSlowInEasing)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = meta.imageResId),
                    contentDescription = stringResource(id = meta.titleResId),
                    modifier =
                        Modifier
                            .size(buttonSize)
                            .padding(buttonSize * 0.08f),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (selected) null else ColorFilter.tint(chromeColors.muted),
                )
                if (selected && label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Home's console row: Favorites, All and each console as round buttons with the selected
 * filter's name beside them, centred in landscape. In portrait the name sits above the row.
 */
@Composable
internal fun HomeConsoleRow(
    filter: LibraryFilter,
    systems: List<MetaSystemID>,
    selectedGameCount: Int?,
    portrait: Boolean,
    edges: ContentEdges,
    onFilterSelected: (LibraryFilter) -> Unit,
) {
    // Console buttons as in the wireframes: 36 dp in landscape, 44 dp in portrait.
    val baseSpec = LocalAdaptiveLayout.current
    val buttonSize = (if (portrait) 44.dp else 36.dp) * baseSpec.uiScale
    val adaptiveSpec =
        baseSpec.copy(sidebarButtonSize = buttonSize, sidebarIconSize = buttonSize * 0.45f)
    val listState = rememberLazyListState()
    val label =
        when (filter) {
            is LibraryFilter.Favorites -> stringResource(R.string.favorites)
            is LibraryFilter.System -> {
                val title = stringResource(GameSystem.findById(filter.metaSystemID.systemIDs.first().dbname).shortTitleResId)
                if (selectedGameCount != null) "$title · $selectedGameCount" else title
            }
            else -> stringResource(R.string.home_all_games)
        }
    val selectedSystemIndex = (filter as? LibraryFilter.System)?.let { systems.indexOf(it.metaSystemID) } ?: -1
    // The list starts with Favorites, All and the divider.
    val selectedItem =
        when (filter) {
            is LibraryFilter.Favorites -> 0
            is LibraryFilter.System -> if (selectedSystemIndex >= 0) selectedSystemIndex + 3 else -1
            else -> 1
        }
    val itemPx = with(LocalDensity.current) { buttonSize.roundToPx() }
    var dockPlaced by remember { mutableStateOf(false) }
    LaunchedEffect(selectedItem, systems.size) {
        // Keep the console that is showing in the middle of the dock (Favorites and All too). Wait
        // for the first layout so the viewport is known; jump there on first show, then animate.
        if (selectedItem < 0) return@LaunchedEffect
        val viewport = snapshotFlow { listState.layoutInfo.viewportSize.width }.first { it > 0 }
        val offset = -((viewport - itemPx) / 2).coerceAtLeast(0)
        runCatching {
            if (dockPlaced) listState.animateScrollToItem(selectedItem, offset) else listState.scrollToItem(selectedItem, offset)
        }
        dockPlaced = true
    }
    val labelText: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = label,
            style = homeTextStyle(FontWeight.SemiBold),
            color = chromeColors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (portrait) TextAlign.Start else TextAlign.End,
            modifier = modifier,
        )
    }
    val row: @Composable (Modifier) -> Unit = { modifier ->
        LazyRow(
            state = listState,
            modifier =
                modifier
                    // No controller focus here: L1/R1 switch console (one highlight, the selected
                    // one). Touch still works.
                    .focusProperties { onEnter = { cancelFocusChange() } }
                    .dockEdgeFade(
                        horizontal = true,
                        fadeStart = { listState.canScrollBackward },
                        fadeEnd = { listState.canScrollForward },
                    ),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item(key = "favorites") {
                Box {
                SidebarIconButton(
                    selected = filter is LibraryFilter.Favorites,
                    icon = if (filter is LibraryFilter.Favorites) WireframeIcons.HeartFilled else WireframeIcons.Heart,
                    contentDescription = stringResource(R.string.favorites),
                    onClick = { onFilterSelected(LibraryFilter.Favorites) },
                )
                }
            }
            item(key = "all") {
                Box {
                SidebarIconButton(
                    selected = filter is LibraryFilter.All,
                    icon = WireframeIcons.AllGames,
                    contentDescription = stringResource(R.string.show_all),
                    onClick = { onFilterSelected(LibraryFilter.All) },
                )
                }
            }
            item(key = "divider") {
                ChromeDivider(Modifier.height(28.dp * adaptiveSpec.uiScale), vertical = true)
            }
            items(systems, key = { it.name }) { system ->
                Box {
                SidebarSystemLogoButton(
                    meta = system,
                    selected = (filter as? LibraryFilter.System)?.metaSystemID == system,
                    onClick = { onFilterSelected(LibraryFilter.System(system)) },
                )
                }
            }
        }
    }
    CompositionLocalProvider(LocalAdaptiveLayout provides adaptiveSpec) {
    if (portrait) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = edges.start, end = edges.end, bottom = 12.dp).focusGroup(),
        ) {
            labelText(Modifier.padding(bottom = 8.dp))
            row(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(adaptiveSpec.consoleBarHeight)
                    .padding(start = edges.start, end = edges.end)
                    .focusGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The label gets a fixed slot, so the console buttons don't shift as it changes
            // ("All games", "N64 · 12", ...).
            row(Modifier.weight(1f))
            labelText(Modifier.padding(start = 16.dp).width(DockLabelWidth * adaptiveSpec.uiScale))
        }
    }
    }
}

/** Landscape Home dock: room for the longest usual label, e.g. "Favourites" or "PSP · 1234". */
private val DockLabelWidth = 112.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LibraryGames(
    modifier: Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: LibraryViewModel.UiState,
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onOpenContinueGame: (Game) -> Unit,
    onContinueClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onFavoriteToggle: (Game, Boolean) -> Unit,
    onSync: (Context) -> Unit,
    onClearSearch: () -> Unit,
    onZoomDensityChange: (Int) -> Unit,
    onBackdropGameChange: (Game?) -> Unit,
    consoleSelection: MutableIntState,
    pagedScrollState: LibraryPagedScrollState,
    edges: ContentEdges,
    homeActionsRequester: FocusRequester,
    requestInitialFocus: Boolean = false,
    portrait: Boolean = false,
) {
    val context = LocalContext.current
    val firstItemRequester = remember { FocusRequester() }
    val navigation = LocalControllerNavigation.current
    navigation?.contentFocusRequester = firstItemRequester
    val continueGame = state.continueGame
    val showContinue =
        state.filter is LibraryFilter.All &&
            state.searchQuery.isBlank() &&
            continueGame != null
    // Re-focus the content when it is rebuilt: a new view, or another console (L1/R1 rebuilds Grid
    // and List, which drops the focused cell). The dock never holds focus, so this can't pull a
    // controller out of it. If the first cell isn't laid out yet, try again a frame later.
    val hasContent = games.itemCount > 0 || showContinue
    LaunchedEffect(requestInitialFocus, hasContent, state.layout, state.filter) {
        if (requestInitialFocus && hasContent) {
            if (!runCatching { firstItemRequester.requestFocus() }.getOrDefault(false)) {
                withFrameNanos { }
                runCatching { firstItemRequester.requestFocus() }
            }
        }
    }
    val isRefreshing = games.loadState.refresh is LoadState.Loading
    val isEmpty = games.itemCount == 0 && !showContinue && !isRefreshing
    // Re-read after scans so "pick a folder" turns into "no games found" once one is chosen.
    val hasGamesFolder =
        remember(state.operationInProgress, isEmpty) {
            !SharedPreferencesHelper.getLegacySharedPreferences(context)
                .getString(context.getString(com.omnidroid.lib.R.string.pref_key_extenral_folder), null)
                .isNullOrBlank()
        }
    val pinchScale = remember { mutableFloatStateOf(1f) }
    val currentZoomDensity by rememberUpdatedState(state.zoomDensity)
    val currentOnZoomDensityChange by rememberUpdatedState(onZoomDensityChange)

    Column(modifier = modifier.fillMaxSize().benchmarkTag(if (isRefreshing) "library-loading" else "library-ready")) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .libraryPinchZoom(
                        enabled = state.layout == LibraryLayout.GRID,
                        density = { currentZoomDensity },
                        onDensityChange = { currentOnZoomDensityChange(it) },
                        liveScale = pinchScale,
                    ),
        ) {
            when {
                isRefreshing && games.itemCount == 0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                isEmpty -> {
                    LibraryEmptyState(
                        kind = libraryEmptyKind(hasGamesFolder, state.filter, state.searchQuery),
                        filter = state.filter,
                        searchQuery = state.searchQuery,
                        scanning = state.operationInProgress,
                        onPickFolder = { StorageFrameworkPickerLauncher.pickFolder(context) },
                        onRescan = { onSync(context) },
                        onClearSearch = onClearSearch,
                    )
                }
                else -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pinchScale.floatValue
                                    scaleY = pinchScale.floatValue
                                },
                    ) {
                        val featured = continueGame.takeIf { showContinue }
                        if (state.layout == LibraryLayout.CONSOLE) {
                            // The carousel sits between the top bar and the console dock, clear of a cutout.
                            val carouselActionFocus = remember(firstItemRequester, homeActionsRequester) {
                                CarouselActionFocus(homeActionsRequester, firstItemRequester)
                            }
                            CompositionLocalProvider(LocalCarouselActionFocus provides carouselActionFocus) {
                                ConsoleLibrary(
                                    modifier = Modifier.fillMaxSize().padding(start = edges.start, end = edges.end),
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    games = games,
                                    featured = featured,
                                    selection = consoleSelection,
                                    portrait = portrait,
                                    showControllerGlyph = requestInitialFocus,
                                    focusRequester = firstItemRequester,
                                    onOpenGame = onGameClick,
                                    onOpenContinueGame = onOpenContinueGame,
                                    onPlayGame = onContinueClick,
                                    onGameLongClick = onGameLongClick,
                                    onFavoriteToggle = onFavoriteToggle,
                                    onBackdropGameChange = onBackdropGameChange,
                                )
                            }
                        } else {
                            key(state.filter, state.searchQuery, state.sort) {
                                LibraryPagedContent(
                                    games = games,
                                    featured = featured,
                                    layout = state.layout,
                                    zoomDensity = state.zoomDensity,
                                    portrait = portrait,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    firstItemRequester = firstItemRequester,
                                    onGameClick = onGameClick,
                                    onContinueClick = onContinueClick,
                                    onGameLongClick = onGameLongClick,
                                    scrollState = pagedScrollState,
                                    startPadding = edges.start,
                                    endPadding = edges.end,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Two-finger pinch steps through the grid densities. [liveScale] tracks the pinch between steps
 * so the grid can follow the fingers before the next density lands.
 */
private fun Modifier.libraryPinchZoom(
    enabled: Boolean,
    density: () -> Int,
    onDensityChange: (Int) -> Unit,
    liveScale: MutableFloatState,
): Modifier {
    if (!enabled) return this
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var scale = 1f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    scale *= event.calculateZoom()
                    val current = density()
                    when {
                        scale > PinchStepScale && current > 0 -> {
                            onDensityChange(current - 1)
                            scale = 1f
                        }
                        scale < 1f / PinchStepScale && current < LibraryViewModel.MAX_ZOOM_DENSITY -> {
                            onDensityChange(current + 1)
                            scale = 1f
                        }
                    }
                    liveScale.floatValue = scale.coerceIn(1f / PinchStepScale, PinchStepScale)
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            liveScale.floatValue = 1f
        }
    }
}

private const val PinchStepScale = 1.25f

@Composable
internal fun LibrarySyncEmpty(
    modifier: Modifier,
    scanning: Boolean,
    onSync: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        FilledTonalButton(
            onClick = { if (!scanning) onSync() },
            enabled = !scanning,
            modifier = Modifier.controllerFocusGlow(),
        ) {
            RescanIcon(
                spinning = scanning,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (scanning) R.string.library_scanning else R.string.library_pick_games_folder,
                ),
            )
        }
    }
}
