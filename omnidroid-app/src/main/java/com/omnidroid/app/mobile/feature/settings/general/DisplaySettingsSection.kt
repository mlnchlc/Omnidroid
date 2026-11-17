package com.omnidroid.app.mobile.feature.settings.general

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.omnidroid.R
import com.omnidroid.app.mobile.feature.main.MainRoute
import com.omnidroid.app.mobile.feature.main.navigateToRoute
import com.omnidroid.app.shared.settings.rememberHdModeAllowed
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsGroup
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsList
import com.omnidroid.app.mobile.feature.library.HomeView
import com.omnidroid.app.mobile.feature.library.LibraryLayout
import com.omnidroid.app.mobile.feature.library.LibraryViewModel
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsMenuLink
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsSlider
import com.omnidroid.app.utils.android.settings.OmnidroidSettingsSwitch
import com.omnidroid.app.utils.android.settings.booleanPreferenceState
import com.omnidroid.app.utils.android.settings.indexPreferenceState
import com.omnidroid.app.utils.android.settings.intPreferenceState
import com.omnidroid.app.utils.android.stringListResource

@Composable
internal fun DisplaySettings(navController: NavController) {
    val hdMode = booleanPreferenceState(R.string.pref_key_hd_mode, false)
    val immersiveMode = booleanPreferenceState(R.string.pref_key_enable_immersive_mode, false)
    val hdModeAllowed = rememberHdModeAllowed()
    val uiScaleStep =
        intPreferenceState(
            key = stringResource(id = R.string.pref_key_button_ui_scale_step),
            default = 0,
        )
    val scalePercentage =
        com.omnidroid.app.mobile.feature.settings.uiscale.UiScaleHelper.percentageForStep(uiScaleStep.value)

    LaunchedEffect(hdModeAllowed) {
        if (!hdModeAllowed && hdMode.value) {
            hdMode.value = false
        }
    }

    val libraryLayout =
        indexPreferenceState(
            LibraryViewModel.LAYOUT_KEY,
            LibraryLayout.CONSOLE.name,
            LibraryLayout.entries.map { it.name },
        )
    OmnidroidSettingsGroup {
        OmnidroidSettingsList(
            state = libraryLayout,
            titleRes = R.string.settings_title_library_layout,
            subtitleRes = R.string.settings_description_library_layout,
            items =
                LibraryLayout.entries.map {
                    stringResource(
                        when (it) {
                            LibraryLayout.CONSOLE -> R.string.library_view_console
                            LibraryLayout.HOME -> R.string.library_view_home
                            LibraryLayout.GRID -> R.string.library_view_grid
                            LibraryLayout.LIST -> R.string.library_view_list
                        },
                    )
                },
        )
        OmnidroidSettingsList(
            enabled = LibraryLayout.entries.getOrNull(libraryLayout.value) == LibraryLayout.HOME,
            state =
                indexPreferenceState(
                    LibraryViewModel.HOME_VIEW_KEY,
                    HomeView.ROW.name,
                    HomeView.entries.map { it.name },
                ),
            titleRes = R.string.settings_title_home_view,
            subtitleRes = R.string.settings_description_home_view,
            items =
                listOf(
                    stringResource(R.string.library_view_row),
                    stringResource(R.string.library_view_grid),
                    stringResource(R.string.library_view_list),
                ),
        )
        OmnidroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_app_theme,
                    "dark",
                    stringListResource(R.array.pref_key_app_theme_values).toList(),
                ),
            titleRes = R.string.settings_title_app_theme,
            items = stringListResource(R.array.pref_key_app_theme_display_names),
        )
        OmnidroidSettingsMenuLink(
            titleRes = R.string.settings_title_button_ui_scale,
            subtitle = {
                Text(
                    text =
                        stringResource(
                            id = R.string.settings_description_button_ui_scale,
                            scalePercentage,
                        ),
                )
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_UI_SCALE) },
        )
        OmnidroidSettingsSwitch(
            state = immersiveMode,
            titleRes = R.string.settings_title_immersive_mode,
            subtitleRes = R.string.settings_description_immersive_mode,
        )
        OmnidroidSettingsSwitch(
            enabled = hdModeAllowed,
            state = hdMode,
            titleRes = R.string.settings_title_hd_mode,
            subtitle = {
                Text(
                    text =
                        stringResource(
                            if (hdModeAllowed) {
                                R.string.settings_description_hd_mode
                            } else {
                                R.string.settings_description_hd_mode_battery
                            },
                        ),
                )
            },
            onCheckedChange = { enabled ->
                if (!hdModeAllowed && enabled) {
                    hdMode.value = false
                }
            },
        )
        OmnidroidSettingsSlider(
            enabled = hdMode.value && hdModeAllowed,
            state =
                intPreferenceState(
                    key = stringResource(id = R.string.pref_key_hd_mode_quality),
                    default = 2,
                ),
            steps = 1,
            valueRange = 0f..2f,
            titleRes = R.string.settings_title_hd_quality,
            subtitleRes = R.string.settings_description_hd_quality,
        )
        OmnidroidSettingsList(
            enabled = !hdMode.value,
            state =
                indexPreferenceState(
                    R.string.pref_key_shader_filter,
                    "auto",
                    stringListResource(R.array.pref_key_shader_filter_values).toList(),
                ),
            titleRes = R.string.display_filter,
            items = stringListResource(R.array.pref_key_shader_filter_display_names),
        )
    }
}
