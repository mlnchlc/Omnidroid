package com.omnidroid.app.mobile.feature.settings.general

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.omnidroid.app.mobile.feature.library.LibraryStartupStore
import com.omnidroid.app.mobile.feature.library.StartupDestinationPickerDialog
import com.omnidroid.app.mobile.feature.library.startupDestinationLabel
import com.omnidroid.app.mobile.feature.library.startupDestinations
import com.omnidroid.lib.library.MetaSystemID
import com.omnidroid.R
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsGroup
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsMenuLink

/**
 * Consoles: add or manage them, and which one Home opens on. These used to sit on the Home dock
 * (an add button and long-press to pin), which a controller couldn't reach.
 */
@Composable
internal fun ConsoleSettings(
    availableConsoles: List<MetaSystemID>,
    onOpenConsoles: () -> Unit,
) {
    val context = LocalContext.current
    val startupStore = remember { LibraryStartupStore(context) }
    val selected by startupStore.observe().collectAsState(initial = startupStore.get())
    var showPicker by remember { mutableStateOf(false) }
    val destinations = remember(availableConsoles) { startupDestinations(availableConsoles) }

    OmnidroidSettingsGroup(titleRes = R.string.title_add_console) {
        OmnidroidSettingsMenuLink(
            titleRes = R.string.settings_title_manage_consoles,
            subtitleRes = R.string.settings_description_manage_consoles,
            onClick = onOpenConsoles,
        )
        OmnidroidSettingsMenuLink(
            titleRes = R.string.settings_title_startup_destination,
            subtitle = { Text(text = startupDestinationLabel(selected)) },
            onClick = { showPicker = true },
        )
    }

    if (!showPicker) return
    StartupDestinationPickerDialog(
        destinations = destinations,
        selected = selected,
        onSelected = { destination ->
            startupStore.set(destination)
            showPicker = false
        },
        onDismiss = { showPicker = false },
    )
}

@Composable
internal fun EmulationSettings(
    indexingInProgress: Boolean,
    onNavigateSubscreen: (SettingsSubscreen) -> Unit,
) {
    OmnidroidSettingsGroup {
        OmnidroidSettingsMenuLink(
            titleRes = R.string.settings_title_open_cores_selection,
            subtitleRes = R.string.settings_description_open_cores_selection,
            onClick = { onNavigateSubscreen(SettingsSubscreen.CORES_SELECTION) },
        )
        OmnidroidSettingsMenuLink(
            titleRes = R.string.settings_title_graphics_api_selection,
            subtitleRes = R.string.settings_description_graphics_api_selection,
            onClick = { onNavigateSubscreen(SettingsSubscreen.GRAPHICS_API) },
        )
        OmnidroidSettingsMenuLink(
            titleRes = R.string.settings_title_display_bios_info,
            // Say why it's greyed out instead of leaving the row silently disabled.
            subtitleRes = if (indexingInProgress) R.string.settings_unavailable_while_indexing else R.string.settings_description_display_bios_info,
            enabled = !indexingInProgress,
            onClick = { onNavigateSubscreen(SettingsSubscreen.BIOS) },
        )
        OmnidroidSettingsMenuLink(
            titleRes = R.string.settings_title_device_profile,
            subtitleRes = R.string.settings_description_device_profile,
            onClick = { onNavigateSubscreen(SettingsSubscreen.DEVICE_PROFILE) },
        )
    }
}
