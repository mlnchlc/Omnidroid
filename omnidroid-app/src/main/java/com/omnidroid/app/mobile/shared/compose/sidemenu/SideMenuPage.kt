package com.omnidroid.app.mobile.shared.compose.sidemenu

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import com.omnidroid.app.mobile.shared.compose.ui.chromeContentColor
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow
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
    /** Short note at the end of the row, such as "asks first". */
    trailing: String? = null,
    /** Shows › for rows that open another page. */
    chevron: Boolean = false,
    onClick: () -> Unit,
) = OmnidroidSettingsMenuLink(
    modifier = itemModifier(id, enabled, modifier), title = title, subtitle = subtitle, icon = icon,
    enabled = enabled, onClick = onClick,
    action =
        if (trailing != null || chevron) {
            { MenuRowEnd(trailing, chevron) }
        } else {
            null
        },
)

@Composable
internal fun MenuRowEnd(text: String?, chevron: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
        if (text != null) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = chromeColors.muted, maxLines = 1)
        }
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = chromeColors.muted,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
    }
}

/** One tile in a [MenuPreviewGrid]: a save slot with its picture, or "No save". */
data class MenuPreviewItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val preview: ImageBitmap?,
    /** Shown in place of the picture, e.g. "No save". */
    val placeholder: String,
    val enabled: Boolean,
    val onFocused: () -> Unit,
    val onClick: () -> Unit,
)

/**
 * Save slots as a two-column grid of pictures, so saves can be told apart at a glance. Each
 * tile is a controller focus target like any other row.
 */
@Composable
fun SideMenuPageScope.MenuPreviewGrid(id: String, items: List<MenuPreviewItem>) {
    key(id) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { item ->
                        val shape = RoundedCornerShape(10.dp)
                        Surface(
                            onClick = item.onClick,
                            enabled = item.enabled,
                            modifier =
                                itemModifier(
                                    item.id,
                                    item.enabled,
                                    Modifier.weight(1f).controllerFocusGlow(shape).onFocusChanged {
                                        if (it.isFocused) item.onFocused()
                                    },
                                ),
                            shape = shape,
                            color = chromeColors.panelSelected,
                            contentColor = chromeContentColor,
                            border = BorderStroke(1.dp, chromeColors.panelBorder),
                        ) {
                            Column {
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(chromeColors.panel),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (item.preview != null) {
                                        Image(
                                            bitmap = item.preview,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Text(item.placeholder, style = MaterialTheme.typography.bodySmall, color = chromeColors.muted)
                                    }
                                }
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                    )
                                    Text(
                                        item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = chromeColors.muted,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

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
 * The most-used actions as a row of big tiles, for the portrait sheet where they are easier to
 * hit than list rows. Each tile is a controller focus target like any other row.
 */
@Composable
fun SideMenuPageScope.MenuTiles(id: String, tiles: List<MenuTile>) {
    key(id) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tiles.forEach { tile ->
                val shape = RoundedCornerShape(12.dp)
                Surface(
                    onClick = tile.onClick,
                    // The focus requester must come before the clickable for focus restore to find it.
                    modifier = itemModifier(tile.id, true, Modifier.weight(1f).height(76.dp).controllerFocusGlow(shape)),
                    shape = shape,
                    color = if (tile.highlighted) chromeColors.gameMenuAccent.copy(alpha = 0.18f) else chromeColors.panelSelected,
                    contentColor = chromeContentColor,
                    border = BorderStroke(1.dp, if (tile.highlighted) chromeColors.gameMenuAccent else chromeColors.panelBorder),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        tile.icon()
                        Text(
                            tile.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
