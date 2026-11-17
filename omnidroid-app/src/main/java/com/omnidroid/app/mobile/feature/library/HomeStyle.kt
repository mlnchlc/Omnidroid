package com.omnidroid.app.mobile.feature.library

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
import com.omnidroid.app.mobile.shared.compose.ui.LibraryNeonGreen
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
): TextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = size, fontWeight = weight, letterSpacing = 0.sp)

internal val HomeTitleSize = 20.sp

/** Label colour on Home's green buttons (dark, for contrast on the accent). */
internal val HomeOnAccent = Color(0xFF0A0A0A)

/** Card corner shared by every Home view. */
internal val HomeCardCorner = 6.dp
private val HomeRingWidth = 3.dp
private val HomeRingOutset = 3.dp

/** Room around a card for its selection ring. */
internal val HomeRingSpace = HomeRingWidth + HomeRingOutset

/** Height the landscape carousel keeps beside the card for title, meta line and buttons. */
internal val HomeInfoMinHeight = 96.dp

/**
 * Set by the Home layout around the console carousel, which then takes Home's look: 6 dp cards
 * with an accent ring, titles on the side cards, and Home's title, meta line and buttons.
 */
internal val LocalHomeCarouselStyle = staticCompositionLocalOf { false }

/** "NES · played 12 min. ago · 3h 40m": console, when it was last played and the total play time. */
@Composable
internal fun homeGameMeta(game: Game): String {
    val context = LocalContext.current
    val system = remember(game.id, game.systemId) { GameUtils.getSystemShortName(context, game) }
    val playTime = rememberPlayTimeMs(game)
    val played =
        game.lastPlayedAt?.let {
            if (System.currentTimeMillis() - it < DateUtils.MINUTE_IN_MILLIS) {
                return@let stringResource(R.string.home_played_just_now)
            }
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

/**
 * Lets a controller reach the carousel's buttons: the carousel is one focusable that keeps
 * left/right for moving between games, so Down hands focus to [actions] (the Start pill), and Up
 * from the buttons returns to [carousel].
 */
internal class CarouselActionFocus(val actions: FocusRequester, val carousel: FocusRequester)

internal val LocalCarouselActionFocus = staticCompositionLocalOf<CarouselActionFocus?> { null }

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
    val height = if (compact) 38.dp else 52.dp
    // Paging only re-emits the game after the database write lands, so flip the heart immediately.
    var favorite by remember(game?.id, game?.isFavorite) { mutableStateOf(game?.isFavorite == true) }
    val carouselFocus = LocalCarouselActionFocus.current
    Row(
        modifier = modifier.then(if (carouselFocus != null) Modifier.focusProperties { up = carouselFocus.carousel } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp, if (compact) Alignment.Start else Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onPlay,
            modifier =
                (if (compact) Modifier.widthIn(min = 132.dp) else Modifier.weight(1f).widthIn(max = 240.dp))
                    .then(if (carouselFocus != null) Modifier.focusRequester(carouselFocus.actions) else Modifier)
                    .height(height)
                    .controllerFocusGlow(CircleShape),
            shape = CircleShape,
            color = LibraryNeonGreen,
            contentColor = HomeOnAccent,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 18.dp else 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(WireframeIcons.Play, contentDescription = null, modifier = Modifier.size(if (compact) 16.dp else 20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (continueGame) R.string.continue_playing else R.string.library_console_start),
                    style =
                        if (compact) homeTextStyle(FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        HomeRoundButton(
            size = height,
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
        HomeRoundButton(size = height, onClick = onMore) {
            Icon(WireframeIcons.More, contentDescription = stringResource(R.string.game_settings), tint = chromeColors.content)
        }
    }
}

@Composable
private fun HomeRoundButton(
    size: Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size).controllerFocusGlow(CircleShape),
        shape = CircleShape,
        color = chromeColors.panel,
        border = BorderStroke(1.dp, chromeColors.panelBorder),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Accent ring drawn just outside a card, fading in with [amount] (0 to 1) as the card becomes the
 * selection. Drawn outside the bounds, so leave [HomeRingSpace] around the card.
 */
internal fun Modifier.homeSelectionRing(corner: Dp, amount: () -> Float = { 1f }): Modifier =
    drawWithContent {
        drawContent()
        val alpha = amount().coerceIn(0f, 1f)
        if (alpha <= 0f) return@drawWithContent
        val outset = HomeRingOutset.toPx() + HomeRingWidth.toPx() / 2
        drawRoundRect(
            color = LibraryNeonGreen.copy(alpha = alpha),
            topLeft = Offset(-outset, -outset),
            size = Size(size.width + outset * 2, size.height + outset * 2),
            cornerRadius = CornerRadius(corner.toPx() + outset),
            style = Stroke(width = HomeRingWidth.toPx()),
        )
    }

/** Home's Row / Grid / List switch, sized to sit in the top bar beside search. */
/** Width of [HomeViewSwitch]: three segments plus its inset and gaps. */
@Composable
internal fun homeViewSwitchWidth(): Dp = chromeButtonSize() * 3.2f + 4.dp

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
