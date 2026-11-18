package com.omnidroid.app.mobile.feature.settings.general

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnidroid.R
import com.omnidroid.app.mobile.shared.compose.ui.accentColor
import com.omnidroid.app.mobile.shared.compose.ui.selectionColors
import com.omnidroid.app.mobile.shared.compose.ui.ChromeDivider
import com.omnidroid.app.mobile.shared.compose.ui.RailScaffold
import androidx.compose.foundation.layout.heightIn
import com.omnidroid.app.mobile.shared.compose.ui.chromeContentColor
import com.omnidroid.app.mobile.shared.compose.ui.chromeMutedContentColor
import com.omnidroid.app.mobile.shared.compose.ui.homeChromeBackground
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow

@Composable
internal fun SettingsBackHeader(
    sectionTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onBack,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .controllerFocusGlow(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = accentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.settings_back_to, sectionTitle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = chromeContentColor,
            )
        }
    }
}

/** Section icon for the rail and the portrait chips. */
private fun SettingsSection.icon(): ImageVector =
    when (this) {
        SettingsSection.LIBRARY -> Icons.Outlined.VideoLibrary
        SettingsSection.DISPLAY -> Icons.Outlined.Palette
        SettingsSection.CONTROLS -> Icons.Outlined.SportsEsports
        SettingsSection.SAVES -> Icons.Outlined.Save
        SettingsSection.EMULATION -> Icons.Outlined.Memory
        SettingsSection.ADVANCED -> Icons.Outlined.Tune
        SettingsSection.ABOUT -> Icons.Outlined.Info
    }

/**
 * One section entry, shared by the landscape rail and the portrait chips so both read the same:
 * icon and label, the current section filled with the accent.
 */
@Composable
private fun SettingsSectionItem(
    item: SettingsSection,
    selected: Boolean,
    focusRequester: FocusRequester,
    onFocused: (SettingsSection) -> Unit,
    onActivate: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    val colors = selectionColors(selected)
    Surface(
        onClick = { onActivate(item) },
        modifier =
            modifier
                .heightIn(min = 40.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocused(item) }
                .controllerFocusGlow(CircleShape, onFill = selected),
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content,
        border = if (outlined && !selected) BorderStroke(1.dp, chromeColors.panelBorder) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = item.icon(),
                contentDescription = null,
                tint = if (selected) colors.content else chromeMutedContentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(item.titleId),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Portrait: the sections as a scrolling row of chips. */
@Composable
internal fun SettingsTopMenu(
    modifier: Modifier = Modifier,
    selected: SettingsSection,
    focusRequesters: Map<SettingsSection, FocusRequester>,
    onFocused: (SettingsSection) -> Unit,
    onActivate: (SettingsSection) -> Unit,
    edgePadding: Dp = 16.dp,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        // Bring the chip into view only when it isn't fully visible already, so the row doesn't
        // jump every time a section is picked.
        val index = SettingsSection.values().indexOf(selected)
        if (index < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
        val fullyVisible =
            item != null && item.offset >= info.viewportStartOffset && item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem(index)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().enterOnSelected(selected, focusRequesters).padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = edgePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(SettingsSection.values()) { item ->
                SettingsSectionItem(
                    item = item,
                    selected = item == selected,
                    focusRequester = focusRequesters.getValue(item),
                    onFocused = onFocused,
                    onActivate = onActivate,
                    outlined = true,
                )
            }
        }
        ChromeDivider()
    }
}

/** Landscape: the sections as a rail (the divider beside it comes from [RailScaffold]). */
@Composable
internal fun SettingsSidebar(
    modifier: Modifier = Modifier,
    selected: SettingsSection,
    focusRequesters: Map<SettingsSection, FocusRequester>,
    onFocused: (SettingsSection) -> Unit,
    onActivate: (SettingsSection) -> Unit,
) {
    Column(
        modifier =
            modifier
                .enterOnSelected(selected, focusRequesters)
                .verticalScroll(rememberScrollState())
                .padding(end = 12.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SettingsSection.values().forEach { item ->
            SettingsSectionItem(
                item = item,
                selected = item == selected,
                focusRequester = focusRequesters.getValue(item),
                onFocused = onFocused,
                onActivate = onActivate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Focusing a section opens it, so a controller coming in from the page must land on the open
 * section, not whichever one is nearest (which would switch pages under the user).
 */
private fun Modifier.enterOnSelected(
    selected: SettingsSection,
    focusRequesters: Map<SettingsSection, FocusRequester>,
): Modifier =
    focusProperties { onEnter = { focusRequesters[selected]?.requestFocus() } }.focusGroup()
