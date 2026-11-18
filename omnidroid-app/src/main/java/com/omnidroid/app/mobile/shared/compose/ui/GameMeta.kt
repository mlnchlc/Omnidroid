package com.omnidroid.app.mobile.shared.compose.ui

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.omnidroid.R
import com.omnidroid.app.mobile.feature.gamedetails.PlayTimeStore
import com.omnidroid.app.shared.library.GameStatsCache
import com.omnidroid.app.utils.games.GameUtils
import com.omnidroid.lib.library.db.entity.Game
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/** "3h 40m", "45m", "< 1m". */
fun formatPlayTime(playTimeMs: Long): String {
    if (playTimeMs <= 0L) return "0m"
    val hours = TimeUnit.MILLISECONDS.toHours(playTimeMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(playTimeMs) % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

/**
 * The current time, ticking once a minute, for "played 5 min. ago" text. Provide one per screen
 * with [rememberMinuteClock]; without it, text uses the time it was composed at.
 */
val LocalMinuteClock: ProvidableCompositionLocal<State<Long>?> = compositionLocalOf { null }

@Composable
fun rememberMinuteClock(): State<Long> =
    produceState(System.currentTimeMillis()) {
        while (true) {
            delay(DateUtils.MINUTE_IN_MILLIS - System.currentTimeMillis() % DateUtils.MINUTE_IN_MILLIS)
            value = System.currentTimeMillis()
        }
    }

/**
 * A game's total play time: the larger of the session log (one shared query for every game,
 * [GameStatsCache]) and the older per-game counter.
 */
@Composable
fun rememberPlayTimeMs(game: Game): Long {
    val context = LocalContext.current
    val stored = remember(context, game.id) { PlayTimeStore(context).get(game.id) }
    val allStats by remember(context) { GameStatsCache.observe(context) }.collectAsState()
    return maxOf(allStats[game.id]?.totalDurationMs ?: 0L, stored)
}

/** "NES · played 12 min. ago · 3h 40m": console, when it was last played and the total play time. */
@Composable
fun gameMetaLine(game: Game): String {
    val context = LocalContext.current
    val system = remember(game.id, game.systemId) { GameUtils.getSystemShortName(context, game) }
    val playTime = rememberPlayTimeMs(game)
    val now = LocalMinuteClock.current?.value ?: System.currentTimeMillis()
    val played =
        game.lastPlayedAt?.let {
            if (now - it < DateUtils.MINUTE_IN_MILLIS) {
                return@let stringResource(R.string.home_played_just_now)
            }
            val ago = DateUtils.getRelativeTimeSpanString(it, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
            stringResource(R.string.home_played_ago, ago)
        }
    return listOfNotNull(
        system.takeIf { it.isNotBlank() },
        played,
        playTime.takeIf { it > 0L }?.let(::formatPlayTime),
    ).joinToString(" · ")
}
