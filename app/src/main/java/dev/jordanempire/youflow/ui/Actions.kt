package dev.jordanempire.youflow.ui

import androidx.compose.runtime.Immutable
import dev.jordanempire.youflow.player.playqueue.PlayQueue

/** Navigation side effects the screens trigger. The classic (view based) UI still hosts the player. */
@Immutable
data class AppActions(
    val openVideo: (url: String, title: String) -> Unit,
    val openChannel: (url: String) -> Unit,
    val openPlaylist: (url: String) -> Unit,
    val playQueue: (PlayQueue) -> Unit,
    val openClassicUi: () -> Unit,
    val openSettings: () -> Unit
)
