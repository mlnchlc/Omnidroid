package com.omnidroid.app.mobile.shared.compose.sidemenu

import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow
import com.omnidroid.app.mobile.shared.compose.ui.chromeContentColor
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnidroid.app.mobile.shared.controller.LocalControllerNavigation
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsGroup
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsList
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsMenuLink
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsPage
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsSwitch
import com.omnidroid.app.utils.android.settings.SettingsHorizontalDivider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.yield

/** Sections and rows compose existing settings renderers; they do not own storage or effects. */
class SideMenuPageScope internal constructor(private val focused: MutableState<String?>) {
    internal val targets = linkedMapOf<String, FocusRequester>()
    internal var sectionCount = 0

    @Composable
    internal fun itemModifier(id: String, enabled: Boolean, modifier: Modifier): Modifier {
        val requester = remember(id) { FocusRequester() }
        if (enabled) targets[id] = requester
        return modifier.testTag(id).focusRequester(requester).onFocusChanged { if (it.isFocused) focused.value = id }
    }
}

@Composable
fun SideMenuPage(id: String, content: @Composable SideMenuPageScope.() -> Unit) {
    val focused = rememberSaveable(id) { mutableStateOf<String?>(null) }
    val scope = SideMenuPageScope(focused)
    val controller = LocalControllerNavigation.current
    val ownedTargets = remember { mutableSetOf<FocusRequester>() }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    OmnidroidSettingsPage(verticalArrangement = Arrangement.spacedBy(0.dp)) { scope.content() }
    val target = scope.targets[focused.value] ?: scope.targets.values.firstOrNull()
    SideEffect {
        ownedTargets.addAll(scope.targets.values)
        if (lifecycle.isAtLeast(Lifecycle.State.RESUMED)) controller?.contentFocusRequester = target
    }
    DisposableEffect(controller) {
        onDispose { if (controller?.contentFocusRequester in ownedTargets) controller?.contentFocusRequester = null }
    }
    LaunchedEffect(id, scope.targets.keys.toList(), lifecycle) {
        if (!lifecycle.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
        yield()
        runCatching { (scope.targets[focused.value] ?: scope.targets.values.firstOrNull())?.requestFocus() }
    }
}

@Composable
fun SideMenuPageScope.MenuSection(
    id: String,
    visible: Boolean = true,
    @StringRes titleRes: Int? = null,
    title: (@Composable () -> Unit)? = titleRes?.let { { Text(stringResource(it)) } },
    content: @Composable SideMenuPageScope.() -> Unit,
) {
    if (!visible) return
    key(id) {
        if (sectionCount++ > 0) SettingsHorizontalDivider()
        OmnidroidSettingsGroup(title = title) { content() }
    }
}

@Composable
fun SideMenuPageScope.MenuAction(
    id: String,
    @StringRes titleRes: Int? = null,
    title: @Composable () -> Unit = { Text(stringResource(requireNotNull(titleRes))) },
    subtitle: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = OmnidroidSettingsMenuLink(
    modifier = itemModifier(id, enabled, modifier), title = title, subtitle = subtitle, icon = icon,
    enabled = enabled, onClick = onClick,
)

data class MenuOption<T>(val value: T, val label: String)

/**
 * Index of [value] among [this] options; String values match ignoring case, because cores and
 * stored preferences don't always agree on capitalisation ("enabled" vs "Enabled"). -1 if absent.
 */
fun <T> List<MenuOption<T>>.indexOfMenuValue(value: T): Int {
    val exact = indexOfFirst { it.value == value }
    if (exact >= 0 || value !is String) return exact
    return indexOfFirst { (it.value as? String)?.equals(value, ignoreCase = true) == true }
}

@Composable
fun <T> SideMenuPageScope.MenuChoice(
    id: String,
    @StringRes titleRes: Int? = null,
    title: @Composable () -> Unit = { Text(stringResource(requireNotNull(titleRes))) },
    options: List<MenuOption<T>>,
    value: T,
    enabled: Boolean = true,
    subtitle: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    useSelectedValueAsSubtitle: Boolean = true,
    onSelected: (T) -> Unit,
) = OmnidroidSettingsList(
    modifier = itemModifier(id, enabled && options.isNotEmpty(), Modifier),
    title = title, items = options.map { it.label }, selectedIndex = options.indexOfMenuValue(value),
    enabled = enabled, subtitle = subtitle, icon = icon, useSelectedValueAsSubtitle = useSelectedValueAsSubtitle,
    onItemSelected = { index, _ -> onSelected(options[index].value) },
)

@Composable
fun SideMenuPageScope.MenuToggle(
    id: String,
    @StringRes titleRes: Int? = null,
    title: @Composable () -> Unit = { Text(stringResource(requireNotNull(titleRes))) },
    value: Boolean,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onChanged: (Boolean) -> Unit,
) = OmnidroidSettingsSwitch(
    modifier = itemModifier(id, enabled, Modifier), title = title, value = value,
    enabled = enabled, icon = icon, onCheckedChange = onChanged,
)

@Composable
fun SideMenuPageScope.MenuLink(id: String, destination: SideMenuDestination, onNavigate: (SideMenuDestination) -> Unit) =
    MenuAction(id = id, titleRes = destination.titleRes) { onNavigate(destination) }

/** A large square action in a [MenuTiles] row. */
data class MenuTile(
    val id: String,
    val label: String,
    val icon: @Composable () -> Unit,
    val highlighted: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * The most-used actions as big tiles, easier to hit than list rows, in both the portrait sheet and
 * the side panel. One row when each tile gets at least [MinTileWidth], else two per row (a narrow
 * panel, or a large font). Each tile is a controller focus target like any other row.
 *
 * A plain [Layout], not BoxWithConstraints: the tiles must be composed with the rest of the page,
 * or [SideMenuPage] picks its focus target before they are registered.
 */
@Composable
fun SideMenuPageScope.MenuTiles(id: String, tiles: List<MenuTile>) {
    key(id) {
        Layout(
            content = { tiles.forEach { tile -> MenuTileCell(tile, Modifier) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        ) { measurables, constraints ->
            val gap = TileGap.roundToPx()
            val width = constraints.maxWidth
            val count = measurables.size
            val perRow =
                if (count > 2 && (width - gap * (count - 1)) / count < MinTileWidth.roundToPx()) 2 else count.coerceAtLeast(1)
            val cellWidth = ((width - gap * (perRow - 1)) / perRow).coerceAtLeast(0)
            // Tiles in a row share the tallest one's height, so a two-line label keeps them even.
            val rows =
                measurables.chunked(perRow).map { row ->
                    val height = row.maxOf { it.maxIntrinsicHeight(cellWidth) }
                    row.map { it.measure(Constraints.fixed(cellWidth, height)) }
                }
            val totalHeight = rows.sumOf { row -> row.maxOf { it.height } } + gap * (rows.size - 1).coerceAtLeast(0)
            layout(width, totalHeight) {
                var y = 0
                rows.forEach { row ->
                    row.forEachIndexed { i, placeable -> placeable.placeRelative(i * (cellWidth + gap), y) }
                    y += row.maxOf { it.height } + gap
                }
            }
        }
    }
}

private val TileGap = 8.dp

private val MinTileWidth = 80.dp

@Composable
private fun SideMenuPageScope.MenuTileCell(
    tile: MenuTile,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = tile.onClick,
        // The focus requester must come before the clickable for focus restore to find it.
        modifier = itemModifier(tile.id, true, modifier.heightIn(min = 76.dp).controllerFocusGlow(shape)),
        shape = shape,
        color = if (tile.highlighted) chromeColors.gameMenuAccent.copy(alpha = 0.18f) else chromeColors.panelSelected,
        contentColor = chromeContentColor,
        border = BorderStroke(1.dp, if (tile.highlighted) chromeColors.gameMenuAccent else chromeColors.panelBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            tile.icon()
            Text(
                tile.label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
