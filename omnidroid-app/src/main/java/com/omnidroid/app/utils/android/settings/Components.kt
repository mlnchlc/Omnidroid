package com.omnidroid.app.utils.android.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.base.SettingValueState
import com.omnidroid.app.mobile.shared.compose.ui.LocalAppDarkTheme
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import com.omnidroid.app.mobile.shared.controller.controllerFocusGlow
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlin.math.roundToInt

/** Optional host surface for rows; null uses the app's default settings colors. */
val LocalSettingsRowContainerColor = staticCompositionLocalOf<Color?> { null }

/**
 * When true (the Settings screen), each group is a rounded card under a small-caps title, and
 * dividers between groups become spacing.
 */
val LocalSettingsGroupsAsCards = staticCompositionLocalOf { false }

@Composable
fun OmnidroidSettingsPage(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}

@Composable
fun OmnidroidSettingsSwitch(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: SettingValueState<Boolean>? = null,
    value: Boolean = state?.value ?: false,
    icon: @Composable (() -> Unit)? = null,
    titleRes: Int? = null,
    title: @Composable () -> Unit = { Text(stringResource(requireNotNull(titleRes))) },
    subtitleRes: Int? = null,
    subtitle: (@Composable () -> Unit)? = subtitleRes?.let { { Text(stringResource(it)) } },
    onCheckedChange: (Boolean) -> Unit = {},
) {
    SettingsSwitch(
        modifier = modifier.controllerFocusGlow(RoundedCornerShape(8.dp)),
        enabled = enabled,
        state = value,
        icon = icon,
        title = title,
        subtitle = subtitle,
        onCheckedChange = {
            state?.value = it
            onCheckedChange(it)
        },
        colors = omnidroidSettingsColor(enabled),
    )
}

@Composable
fun OmnidroidSettingsMenuLink(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    titleRes: Int? = null,
    title: @Composable () -> Unit = { Text(stringResource(requireNotNull(titleRes))) },
    subtitleRes: Int? = null,
    subtitle: (@Composable () -> Unit)? = subtitleRes?.let { { Text(stringResource(it)) } },
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    SettingsMenuLink(
        modifier = modifier.controllerFocusGlow(RoundedCornerShape(8.dp)),
        enabled = enabled,
        icon = icon,
        title = title,
        subtitle = subtitle,
        action = action,
        onClick = onClick,
        colors = omnidroidSettingsColor(enabled),
    )
}

@Composable
fun OmnidroidSettingsGroup(
    modifier: Modifier = Modifier,
    titleRes: Int? = null,
    title: @Composable (() -> Unit)? = titleRes?.let { { SettingsGroupTitleText(stringResource(it)) } },
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalSettingsGroupsAsCards.current) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            if (title != null) {
                Box(Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp)) {
                    ProvideTextStyle(
                        MaterialTheme.typography.labelMedium.copy(
                            color = chromeColors.muted,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                        ),
                    ) {
                        CompositionLocalProvider(LocalSettingsTitleUppercase provides true) { title() }
                    }
                }
            }
            val shape = RoundedCornerShape(12.dp)
            Surface(
                shape = shape,
                color = chromeColors.panel,
                border = BorderStroke(1.dp, chromeColors.panelBorder),
                tonalElevation = 0.dp,
            ) {
                CompositionLocalProvider(LocalSettingsRowContainerColor provides chromeColors.panel) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { content() }
                }
            }
        }
        return
    }
    Surface(color = Color.Transparent, tonalElevation = 0.dp) {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            if (title != null) {
                SettingsGroupTitleSmall(title)
            }
            content()
        }
    }
}

@Composable
fun SettingsHorizontalDivider(
    modifier: Modifier = Modifier,
) {
    if (LocalSettingsGroupsAsCards.current) {
        Spacer(modifier.height(4.dp))
        return
    }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.75f),
            thickness = 1.dp,
            color = chromeColors.divider,
        )
    }
}

@Composable
fun OmnidroidSettingsSlider(
    modifier: Modifier = Modifier,
    state: SettingValueState<Int>,
    steps: Int,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    titleRes: Int? = null,
    title: @Composable () -> Unit = { Text(stringResource(requireNotNull(titleRes))) },
    subtitleRes: Int? = null,
    subtitle: (@Composable () -> Unit)? = subtitleRes?.let { { Text(stringResource(it)) } },
    valueText: (@Composable (Int) -> Unit)? = null,
) {
    val defaultColors = ListItemDefaults.colors(
        containerColor = settingsRowContainerColor() ?: Color.Transparent,
    )
    val disabledColors =
        ListItemDefaults.colors(
            containerColor = defaultColors.containerColor,
            headlineColor = defaultColors.disabledHeadlineColor,
            leadingIconColor = defaultColors.disabledLeadingIconColor,
            trailingIconColor = defaultColors.disabledTrailingIconColor,
            supportingColor = defaultColors.supportingTextColor.copy(alpha = 0.3f),
        )
    val colors = if (enabled) defaultColors else disabledColors
    val min = valueRange.start.roundToInt()
    val max = valueRange.endInclusive.roundToInt()
    val stepSize =
        if (steps > 0) {
            ((max - min).toFloat() / (steps + 1)).coerceAtLeast(1f).roundToInt().coerceAtLeast(1)
        } else {
            1
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .focusable(enabled)
                .controllerFocusGlow(RoundedCornerShape(8.dp))
                .onPreviewKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            state.value = (state.value - stepSize).coerceIn(min, max)
                            true
                        }
                        Key.DirectionRight -> {
                            state.value = (state.value + stepSize).coerceIn(min, max)
                            true
                        }
                        else -> false
                    }
                },
    ) {
        ListItem(
            headlineContent = title,
            supportingContent = subtitle,
            trailingContent = valueText?.let { { it(state.value) } },
            colors = colors,
        )
        Slider(
            value = state.value.toFloat(),
            onValueChange = { state.value = it.roundToInt() },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun SettingsGroupTitleSmall(title: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val titleStyle = MaterialTheme.typography.labelLarge.copy(color = primary)
        ProvideTextStyle(value = titleStyle) { title() }
    }
}

@Composable
private fun settingsRowContainerColor(): Color? =
    LocalSettingsRowContainerColor.current
        ?: if (LocalAppDarkTheme.current) null else MaterialTheme.colorScheme.background

@Composable
private fun omnidroidSettingsColor(enabled: Boolean): ListItemColors {
    val defaultColors = settingsRowContainerColor()?.let {
        ListItemDefaults.colors(containerColor = it)
    } ?: ListItemDefaults.colors()

    if (enabled) {
        return defaultColors
    }

    return ListItemDefaults.colors(
        containerColor = defaultColors.containerColor,
        headlineColor = defaultColors.disabledHeadlineColor,
        leadingIconColor = defaultColors.disabledLeadingIconColor,
        trailingIconColor = defaultColors.disabledTrailingIconColor,
        supportingColor = defaultColors.supportingTextColor.copy(alpha = 0.3f),
    )
}

/** Lets a group title render in capitals without every caller changing its string. */
val LocalSettingsTitleUppercase = staticCompositionLocalOf { false }

/** Group title text that follows [LocalSettingsTitleUppercase]. */
@Composable
fun SettingsGroupTitleText(text: String) {
    Text(if (LocalSettingsTitleUppercase.current) text.uppercase() else text)
}

/**
 * A row with a segmented control on the right, for short choices (two to four options) where a
 * dialog would be overkill. Each segment is focusable, so the D-pad moves between them.
 */
@Composable
fun OmnidroidSettingsSegmented(
    state: SettingValueState<Int>,
    items: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    titleRes: Int? = null,
    title: @Composable () -> Unit = { Text(stringResource(requireNotNull(titleRes))) },
    subtitleRes: Int? = null,
    subtitle: (@Composable () -> Unit)? = subtitleRes?.let { { Text(stringResource(it)) } },
) {
    val container = settingsRowContainerColor() ?: Color.Transparent
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(container)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(color = chromeColors.content)) { title() }
            if (subtitle != null) {
                ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(color = chromeColors.muted)) { subtitle() }
            }
        }
        Row(
            modifier =
                Modifier
                    .border(BorderStroke(1.dp, chromeColors.panelBorder), CircleShape)
                    .padding(3.dp)
                    .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEachIndexed { index, label ->
                val selected = index == state.value
                Box(
                    modifier =
                        Modifier
                            .height(32.dp)
                            .controllerFocusGlow(CircleShape)
                            .background(if (selected) chromeColors.content else Color.Transparent, CircleShape)
                            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton) { state.value = index }
                            .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selected) chromeColors.panel else chromeColors.content,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
