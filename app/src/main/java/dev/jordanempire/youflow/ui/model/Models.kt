package dev.jordanempire.youflow.ui.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ContentItem {
    val url: String
}

@Immutable
data class VideoItem(
    override val url: String,
    val title: String,
    val channel: String,
    val channelUrl: String?,
    val thumbnail: String?,
    val avatar: String?,
    val durationSeconds: Long,
    val views: Long?,
    val uploaded: String?,
    val isLive: Boolean,
    val isShort: Boolean,
    /** 0f..1f watched fraction, 0f when unwatched. */
    val progress: Float = 0f
) : ContentItem

@Immutable
data class ChannelItem(
    override val url: String,
    val name: String,
    val avatar: String?,
    val subscribers: Long?,
    val description: String?
) : ContentItem

@Immutable
data class PlaylistItem(
    override val url: String,
    val name: String,
    val thumbnail: String?,
    val uploader: String?,
    val streamCount: Long
) : ContentItem

@Immutable
data class KioskRef(val id: String, val url: String, val title: String)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Immutable
data class ChannelTab(val key: String, val label: String, val index: Int)

@Immutable
data class ChannelDetails(
    val url: String,
    val name: String,
    val avatar: String?,
    val banner: String?,
    val subscribers: Long?,
    val description: String?,
    val tabs: List<ChannelTab>
)

@Immutable
data class PlaylistDetails(
    val url: String,
    val name: String,
    val thumbnail: String?,
    val uploader: String?,
    val streamCount: Long,
    val description: String?
)
