package com.omnidroid.app.mobile.feature.gamemenu

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.omnidroid.R
import com.omnidroid.app.mobile.shared.compose.dialog.DialogActionRole
import com.omnidroid.app.mobile.shared.compose.dialog.OmnidroidDialog
import com.omnidroid.app.mobile.shared.compose.dialog.OmnidroidDialogButton
import com.omnidroid.app.utils.android.settings.booleanPreferenceState

/** Settings → Saves → "Confirm before losing progress" (on by default). */
@Composable
fun confirmGameMenuActionsEnabled(): Boolean =
    booleanPreferenceState(R.string.pref_key_confirm_game_menu_actions, true).value

/**
 * Confirmation for in-game menu actions that throw away progress (restart, load, overwrite a
 * save). Destructive, so controller focus starts on Cancel.
 */
@Composable
fun ConfirmProgressLossDialog(
    title: String,
    confirmLabel: String,
    message: String = stringResource(R.string.game_menu_confirm_progress_loss),
    preview: Bitmap? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OmnidroidDialog(
        destructive = true,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // The save being loaded or overwritten, so it is clear which one this is.
                preview?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
                Text(message)
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            OmnidroidDialogButton(onClick = onConfirm, role = DialogActionRole.Destructive) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            OmnidroidDialogButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
