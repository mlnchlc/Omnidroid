package com.omnidroid.app.mobile.feature.gamemenu.states

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnidroid.R
import com.omnidroid.app.mobile.feature.gamemenu.ConfirmProgressLossDialog
import com.omnidroid.app.mobile.feature.gamemenu.confirmGameMenuActionsEnabled
import com.omnidroid.app.mobile.shared.compose.gamemenu.MenuDrawableIcon
import com.omnidroid.app.mobile.shared.compose.sidemenu.MenuAction
import com.omnidroid.app.mobile.shared.compose.sidemenu.MenuPreviewGrid
import com.omnidroid.app.mobile.shared.compose.sidemenu.MenuPreviewItem
import com.omnidroid.app.mobile.shared.compose.sidemenu.MenuSection
import com.omnidroid.app.mobile.shared.compose.sidemenu.SideMenuPage

/** What a click on a slot does, which decides when to ask before acting. */
enum class StatesMenuMode {
    /** Saving: confirm only when the slot already holds a save. */
    SAVE,

    /** Loading: always confirm, since it replaces the current progress. */
    LOAD,
}

/** A slot click waiting for confirmation; [slot] is null for the autosave. */
private data class PendingSlot(val slot: Int?, val title: String, val description: String, val preview: Bitmap? = null)

@Composable
fun GameMenuStatesScreen(
    viewModel: GameMenuStatesViewModel,
    mode: StatesMenuMode,
    onStateClicked: (Int) -> Unit,
    onAutoSaveClicked: () -> Unit = {},
    onNewStateClicked: () -> Unit = {},
) {
    val state = viewModel.uiStates.collectAsState()
    var pending by remember { mutableStateOf<PendingSlot?>(null) }
    val autoSaveTitle = stringResource(R.string.game_menu_autosave)
    val askToConfirm = confirmGameMenuActionsEnabled()
    val slotPreview = LocalSlotPreview.current
    DisposableEffect(slotPreview) {
        onDispose { slotPreview?.bitmap = null }
    }

    pending?.let { target ->
        val isLoad = mode == StatesMenuMode.LOAD
        ConfirmProgressLossDialog(
            title =
                stringResource(
                    if (isLoad) R.string.game_menu_load_confirm_title else R.string.game_menu_save_overwrite_title,
                    target.title,
                ),
            confirmLabel = stringResource(if (isLoad) R.string.game_menu_load else R.string.game_menu_overwrite),
            message =
                if (isLoad) {
                    stringResource(R.string.game_menu_confirm_progress_loss)
                } else {
                    stringResource(R.string.game_menu_save_overwrite_message, target.description)
                },
            preview = target.preview,
            onDismiss = { pending = null },
        ) {
            pending = null
            target.slot?.let(onStateClicked) ?: onAutoSaveClicked()
        }
    }

    SideMenuPage("game-slots") {
        MenuSection("error", visible = state.value.loadFailed) {
            MenuAction(
                "retry",
                titleRes = R.string.game_menu_states_retry,
                subtitle = { Text(stringResource(R.string.game_menu_states_load_failed)) },
                onClick = viewModel::retry,
            )
        }
        val noSave = stringResource(R.string.game_menu_no_save)
        val gridItems =
            buildList {
                state.value.autoSave?.let { autoSave ->
                    add(
                        MenuPreviewItem(
                            id = "autosave",
                            title = autoSaveTitle,
                            subtitle = autoSave.description,
                            preview = null,
                            placeholder = autoSaveTitle,
                            enabled = autoSave.enabled,
                            onFocused = { slotPreview?.bitmap = null },
                        ) {
                            if (askToConfirm) pending = PendingSlot(null, autoSaveTitle, autoSave.description) else onAutoSaveClicked()
                        },
                    )
                }
                state.value.entries.forEach { entry ->
                    add(
                        MenuPreviewItem(
                            id = "slot-${entry.slot}",
                            title = entry.title,
                            subtitle = entry.description,
                            preview = entry.preview?.asImageBitmap(),
                            placeholder = noSave,
                            enabled = entry.enabled,
                            onFocused = { slotPreview?.bitmap = entry.preview },
                        ) {
                            val needsConfirm = askToConfirm && (mode == StatesMenuMode.LOAD || entry.exists)
                            if (needsConfirm) {
                                pending = PendingSlot(entry.slot, entry.title, entry.description, entry.preview)
                            } else {
                                onStateClicked(entry.slot)
                            }
                        },
                    )
                }
            }
        if (gridItems.isNotEmpty()) MenuPreviewGrid("slots", gridItems)
        MenuSection("new-slot", visible = state.value.canAddSlot) {
            PreviewOnFocus(slotPreview, null) {
                MenuAction(
                    "new-slot",
                    titleRes = R.string.game_menu_save_new,
                    subtitle = { Text(stringResource(R.string.game_menu_save_new_description)) },
                    onClick = onNewStateClicked,
                )
            }
        }
    }
}

/**
 * Shows [preview] beside the menu while the wrapped row has focus. The listener sits on a wrapper
 * because the settings row applies its modifier inside its own clickable, out of focus's reach.
 */
@Composable
private fun PreviewOnFocus(
    state: SlotPreviewState?,
    preview: Bitmap?,
    content: @Composable () -> Unit,
) {
    Box(Modifier.onFocusChanged { if (it.hasFocus) state?.bitmap = preview }) { content() }
}
