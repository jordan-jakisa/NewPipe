package dev.jordanempire.youflow.ui

import androidx.compose.runtime.Immutable
import dev.jordanempire.youflow.ui.model.VideoItem

/** Navigation side effects the screens trigger. */
@Immutable
data class AppActions(
    val openVideo: (video: VideoItem) -> Unit,
    val openChannel: (url: String) -> Unit,
    val openPlaylist: (url: String) -> Unit,
    /** Plays [videos] as a queue starting at [index]. */
    val playVideos: (videos: List<VideoItem>, index: Int) -> Unit,
    val openClassicUi: () -> Unit,
    val openSettings: () -> Unit
)
