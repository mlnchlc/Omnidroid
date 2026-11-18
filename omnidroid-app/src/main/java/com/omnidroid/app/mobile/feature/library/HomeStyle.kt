package com.omnidroid.app.mobile.feature.library

import com.omnidroid.app.mobile.shared.compose.ui.RingPlacement
import com.omnidroid.app.mobile.shared.compose.ui.accentRing
import com.omnidroid.app.mobile.shared.compose.ui.SegmentedPills
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import com.omnidroid.app.mobile.shared.compose.ui.LocalAdaptiveLayout
import com.omnidroid.app.mobile.shared.compose.ui.chromeTextStyle
import com.omnidroid.app.mobile.shared.compose.ui.onAccentColor
import com.omnidroid.app.mobile.shared.compose.ui.RoundIconButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import com.omnidroid.app.mobile.shared.compose.ui.accentColor
import com.omnidroid.app.mobile.shared.compose.ui.gameMetaLine
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush

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
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import com.omnidroid.app.mobile.shared.controller.LocalControllerNavigation
import com.omnidroid.lib.library.db.entity.Game
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Home's type scale: one 13 sp body size (the top bar's) for meta lines, labels, captions and
 * buttons, and [HomeTitleSize] for the selected game's title.
 */
@Composable
internal fun homeTextStyle(
    weight: FontWeight = FontWeight.Normal,
    size: TextUnit = 13.sp,
): TextStyle = chromeTextStyle(weight).copy(fontSize = size * LocalAdaptiveLayout.current.uiScale)

internal val HomeTitleSize = 20.sp

/**
 * The accent pill with a play icon: Home's Start / Continue button, and the Continue marker on a
 * List row (no [onClick]). Filled with the accent, label in the on-accent colour.
 */
@Composable
internal fun AccentPill(
    label: String,
    height: Dp,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val content = onAccentColor
    Row(
        modifier =
            modifier
                .height(height)
                .then(if (onClick != null) Modifier.controllerFocusGlow(CircleShape, onFill = true) else Modifier)
                .clip(CircleShape)
                .background(accentColor)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = if (large) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(WireframeIcons.Play, contentDescription = null, tint = content, modifier = Modifier.size(if (large) 20.dp else 16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = if (large) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else homeTextStyle(FontWeight.Bold),
            color = content,
            maxLines = 1,
        )
    }
}

/** Card corner shared by every Home view. */
internal val HomeCardCorner = 6.dp
private val HomeRingWidth = 3.dp
private val HomeRingOutset = 3.dp

/** Room around a card for its selection ring. */
internal val HomeRingSpace = HomeRingWidth + HomeRingOutset

/** Height the landscape carousel keeps beside the card for title, meta line and buttons. */
internal val HomeInfoMinHeight = 96.dp

/**
 * Lets a controller reach the carousel's buttons: the carousel is one focusable that keeps
 * left/right for moving between games, so Down hands focus to [actions] (the Start pill), and Up
 * from the buttons returns to [carousel].
 */
internal class CarouselActionFocus(val actions: FocusRequester, val carousel: FocusRequester) {
    /** The carousel itself has controller focus. */
    val carouselFocused = mutableStateOf(false)

    /** A controller is in use: the selected card's ring dims while focus is elsewhere. */
    val controllerActive = mutableStateOf(false)
}

internal val LocalCarouselActionFocus = staticCompositionLocalOf<CarouselActionFocus?> { null }

/** The selected card's ring dims while a controller's focus is off the carousel (buttons, top bar). */
internal fun CarouselActionFocus?.ringDimmed(): Boolean = this != null && controllerActive.value && !carouselFocused.value

/** Wide Continue / Start pill, then round favourite and more-options buttons. */
@Composable
internal fun HomeActions(
    game: Game?,
    continueGame: Boolean,
    onPlay: () -> Unit,
    onFavoriteToggle: (Game, Boolean) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    /** Smaller buttons for the landscape carousel, where they share the space with the title. */
    compact: Boolean = false,
) {
    val height = (if (compact) 38.dp else 52.dp) * LocalAdaptiveLayout.current.uiScale
    // Paging only re-emits the game after the database write lands, so flip the heart immediately.
    var favorite by remember(game?.id, game?.isFavorite) { mutableStateOf(game?.isFavorite == true) }
    val carouselFocus = LocalCarouselActionFocus.current
    // 0 = Start, 1 = favourite, 2 = more: which button a controller is on.
    var focusedButton by remember { mutableIntStateOf(-1) }
    Row(
        modifier =
            modifier.then(
                if (carouselFocus != null) {
                    // A controller stays in this row: Up, and Left from Start, go back to the
                    // carousel; Right past the last button and Down go nowhere (not the top bar).
                    Modifier.onKeyEvent { event ->
                        val down = event.type == KeyEventType.KeyDown
                        fun toCarousel(): Boolean {
                            if (down) runCatching { carouselFocus.carousel.requestFocus() }
                            return true
                        }
                        when (event.key) {
                            Key.DirectionUp -> toCarousel()
                            Key.DirectionLeft -> if (focusedButton == 0) toCarousel() else false
                            Key.DirectionRight -> focusedButton == 2
                            Key.DirectionDown -> true
                            else -> false
                        }
                    }
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp, if (compact) Alignment.Start else Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentPill(
            label = stringResource(if (continueGame) R.string.continue_playing else R.string.library_console_start),
            height = height,
            large = !compact,
            onClick = onPlay,
            modifier =
                (if (compact) Modifier.widthIn(min = 132.dp) else Modifier.weight(1f).widthIn(max = 240.dp))
                    .then(if (carouselFocus != null) Modifier.focusRequester(carouselFocus.actions) else Modifier)
                    .onFocusChanged { if (it.isFocused) focusedButton = 0 },
        )
        RoundIconButton(
            size = height,
            bordered = true,
            modifier = Modifier.onFocusChanged { if (it.isFocused) focusedButton = 1 },
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
                tint = if (favorite) accentColor else chromeColors.content,
            )
        }
        RoundIconButton(size = height, bordered = true, modifier = Modifier.onFocusChanged { if (it.isFocused) focusedButton = 2 }, onClick = onMore) {
            Icon(WireframeIcons.More, contentDescription = stringResource(R.string.game_settings), tint = chromeColors.content)
        }
    }
}

/**
 * Accent ring drawn just outside a card, fading in with [amount] (0 to 1) as the card becomes the
 * selection, and dimmed while a controller's focus is elsewhere (buttons, top bar), so full
 * strength shows the carousel itself has focus. Drawn outside the bounds, so leave [HomeRingSpace]
 * around the card.
 */
internal fun Modifier.homeSelectionRing(
    corner: Dp,
    color: Color,
    dimmed: () -> Boolean = { false },
    amount: () -> Float = { 1f },
): Modifier =
    accentRing(
        shape = RoundedCornerShape(corner + HomeRingOutset + HomeRingWidth / 2),
        color = color,
        width = HomeRingWidth,
        gap = HomeRingOutset,
        placement = RingPlacement.Outside,
        alpha = { amount() * (if (dimmed()) 0.4f else 1f) },
    )

/**
 * The layout control for the top bar: the three-way switch when it fits beside search in [room],
 * else one button that steps through the layouts (portrait, narrow 4:3 screens).
 */
@Composable
internal fun LibraryLayoutControl(
    layout: LibraryLayout,
    onLayoutChange: (LibraryLayout) -> Unit,
    portrait: Boolean,
    room: Dp,
) {
    if (portrait || room < libraryLayoutSwitchWidth()) {
        LibraryLayoutCycleButton(layout = layout, onLayoutChange = onLayoutChange)
    } else {
        LibraryLayoutSwitch(layout = layout, onLayoutChange = onLayoutChange)
    }
}

/** Width of [LibraryLayoutSwitch]: three segments plus its inset and gaps. */
@Composable
internal fun libraryLayoutSwitchWidth(): Dp =
    // Three square segments, the inset either side, and the 2 dp gaps between segments.
    chromeButtonSize() * (LibraryLayout.entries.size + 2 * LayoutSwitchInset) + 2.dp * (LibraryLayout.entries.size - 1)

/** [LibraryLayoutSwitch]'s inset around its segments, as a fraction of its height. */
private const val LayoutSwitchInset = 0.1f

/** The Console / Grid / List switch, sized to sit in the top bar beside search. */
@Composable
internal fun LibraryLayoutSwitch(
    layout: LibraryLayout,
    onLayoutChange: (LibraryLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Same height as the other top bar controls; segments in the wireframe's 44 : 36 proportion.
    val height = chromeButtonSize()
    val inset = height * LayoutSwitchInset
    SegmentedPills(
        options = LibraryLayout.entries,
        selected = layout,
        onSelect = onLayoutChange,
        modifier = modifier.height(height),
        segmentHeight = height - inset * 2,
        segmentWidth = height,
        inset = inset,
        background = chromeColors.panel,
        role = Role.Tab,
    ) { option, colors ->
        Icon(
            imageVector = option.icon(),
            contentDescription = stringResource(option.labelRes()),
            modifier = Modifier.size(chromeIconSize()),
            tint = if (option == layout) colors.content else chromeColors.muted,
        )
    }
}

/**
 * One round button showing the current layout; each press moves to the next. Used in the
 * portrait top bar, where the three-way switch does not fit beside search and the other buttons.
 */
@Composable
internal fun LibraryLayoutCycleButton(
    layout: LibraryLayout,
    onLayoutChange: (LibraryLayout) -> Unit,
) {
    val next = LibraryLayout.entries[(layout.ordinal + 1) % LibraryLayout.entries.size]
    FloatingChromeButton(onClick = { onLayoutChange(next) }) {
        Icon(
            imageVector = layout.icon(),
            contentDescription = stringResource(R.string.home_view_switch, stringResource(layout.labelRes())),
            modifier = Modifier.size(chromeIconSize()),
            tint = accentColor,
        )
    }
}

private fun LibraryLayout.icon() =
    when (this) {
        LibraryLayout.CONSOLE -> WireframeIcons.ViewConsole
        LibraryLayout.GRID -> WireframeIcons.ViewGrid
        LibraryLayout.LIST -> WireframeIcons.ViewList
    }

private fun LibraryLayout.labelRes() =
    when (this) {
        LibraryLayout.CONSOLE -> R.string.library_view_console
        LibraryLayout.GRID -> R.string.library_view_grid
        LibraryLayout.LIST -> R.string.library_view_list
    }

/** Console and title over the bottom of a side card, on a dark gradient (light text in both themes). */
@Composable
internal fun HomeCardCaption(
    game: Game,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val system = remember(game.id, game.systemId) { GameUtils.getSystemShortName(context, game) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .padding(start = 10.dp, end = 10.dp, top = 28.dp, bottom = 10.dp),
    ) {
        Text(system, style = homeTextStyle(size = 11.sp), color = Color.White.copy(alpha = 0.8f), maxLines = 1)
        Text(
            game.displayName,
            style = homeTextStyle(FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
