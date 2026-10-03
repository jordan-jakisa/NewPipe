package dev.jordanempire.youflow.ui

import androidx.compose.runtime.Immutable

/** Navigation side effects the screens trigger. The classic (view based) UI still hosts the player. */
@Immutable
class AppActions(
    val openVideo: (url: String, title: String) -> Unit,
    val openChannel: (url: String) -> Unit,
    val openPlaylist: (url: String) -> Unit,
    val openClassicUi: () -> Unit,
    val openSettings: () -> Unit
)
