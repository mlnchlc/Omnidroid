package com.omnidroid.app.mobile.feature.gamemenu

import com.omnidroid.app.mobile.shared.compose.ui.rememberPlayTimeMs
import com.omnidroid.app.mobile.shared.compose.ui.formatPlayTime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnidroid.R
import com.omnidroid.app.mobile.shared.compose.sidemenu.SideMenuRoundButton
import com.omnidroid.app.mobile.shared.compose.ui.OmnidroidGameImage
import com.omnidroid.app.mobile.shared.compose.ui.chromeColors
import com.omnidroid.app.mobile.shared.compose.ui.chromeContentColor
import com.omnidroid.app.utils.games.GameUtils
import com.omnidroid.lib.library.db.entity.Game

/** First page header: the game's cover, name, console and play time, and a round Close button. */
@Composable
fun GameMenuHeader(
    game: Game,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    // Same play time as Home's cards: the session log and the older counter, whichever is larger.
    val system = remember(game.id, game.systemId) { GameUtils.getSystemShortName(context, game) }
    val played = rememberPlayTimeMs(game)
    val meta = if (played > 0L) stringResource(R.string.game_menu_header_meta, system, formatPlayTime(played)) else system
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OmnidroidGameImage(
            modifier = Modifier.size(width = 38.dp, height = 56.dp).clip(RoundedCornerShape(4.dp)),
            game = game,
            aspectRatio = null,
            decodeWidth = 38.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(
                game.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = chromeContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, style = MaterialTheme.typography.bodySmall, color = chromeColors.muted, maxLines = 1)
        }
        SideMenuRoundButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
        }
    }
}
