package dev.jordanempire.youflow.ui.watch

import android.app.Application
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.media.engine.PlayerState
import dev.jordanempire.youflow.media.engine.QueueEntry
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.Thumbnail
import dev.jordanempire.youflow.ui.components.VideoCard
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.ChannelItem
import dev.jordanempire.youflow.ui.model.VideoItem
import dev.jordanempire.youflow.ui.util.formatCount
import dev.jordanempire.youflow.util.image.ImageStrategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

@OptIn(ExperimentalCoroutinesApi::class)
class WatchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    val engine = PlaybackEngine.get(app)

    val subscribed: StateFlow<Boolean> = engine.state
        .map { it.info?.uploaderUrl }
        .distinctUntilChanged()
        .flatMapLatest { url -> if (url == null) flowOf(false) else repo.isSubscribed(url) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleSubscribe(info: StreamInfo) {
        val url = info.uploaderUrl ?: return
        viewModelScope.launch {
            if (subscribed.value) {
                repo.unsubscribe(url)
            } else {
                repo.subscribe(
                    ChannelItem(
                        url, info.uploaderName.orEmpty(), ImageStrategy.choosePreferredImage(info.uploaderAvatars),
                        info.uploaderSubscriberCount.takeIf { it >= 0 }, null
                    )
                )
            }
        }
    }
}

/** The expanded watch page: player on top, details and related videos below. */
@Composable
fun WatchScreen(
    actions: AppActions,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onCollapse: () -> Unit,
    vm: WatchViewModel = viewModel()
) {
    val engine = vm.engine
    val state by engine.state.collectAsState()
    val subscribed by vm.subscribed.collectAsState()
    val info = state.info
    var showSettings by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    if (showSettings) PlayerSettingsSheet(engine, state) { showSettings = false }
    if (showComments) CommentsSheet(onDismiss = { showComments = false })

    if (fullscreen) {
        PlayerBox(engine, state, fullscreen = true, onToggleFullscreen, onCollapse, { showSettings = true }, Modifier.fillMaxSize().background(Color.Black))
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        PlayerBox(
            engine, state, fullscreen = false, onToggleFullscreen, onCollapse, { showSettings = true },
            Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding().aspectRatio(state.videoAspect.coerceIn(1f, 16f / 9f))
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
            val entry = state.entry
            item(key = "title") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(info?.name ?: entry?.title.orEmpty(), style = MaterialTheme.typography.titleLargeEmphasized)
                    val meta = listOfNotNull(
                        info?.viewCount?.takeIf { it >= 0 }?.let { formatCount(it, "views") },
                        info?.let(::formatUploaded)
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            if (info != null) {
                item(key = "channel") { ChannelRow(info, subscribed, actions, onSubscribe = { vm.toggleSubscribe(info) }) }
                item(key = "actions") { ActionRow(info, engine, onSave = { actions.saveVideo(info.toVideoItemForSave()) }) }
                item(key = "description") { DescriptionCard(info) }
                item(key = "comments") { CommentsTeaser(info.url, onClick = { showComments = true }) }
                val related = info.relatedItems.filterIsInstance<StreamInfoItem>()
                items(related, key = { it.url }) { item ->
                    VideoCard(
                        item.toVideoItem(),
                        onClick = { engine.play(listOf(QueueEntry(item.url, item.name, item.uploaderName.orEmpty(), null))) },
                        onChannelClick = { item.uploaderUrl?.let(actions.openChannel) },
                        onSave = { actions.saveVideo(item.toVideoItem()) },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerBox(
    engine: PlaybackEngine,
    state: PlayerState,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onCollapse: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier
) {
    Box(modifier) {
        ContentFrame(
            player = engine.exo,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        PlayerControls(engine, state, fullscreen, onToggleFullscreen, onCollapse, onSettings)
    }
}

@Composable
private fun ChannelRow(info: StreamInfo, subscribed: Boolean, actions: AppActions, onSubscribe: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { info.uploaderUrl?.let(actions.openChannel) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Thumbnail(
                ImageStrategy.choosePreferredImage(info.uploaderAvatars),
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
            )
            Column {
                Text(info.uploaderName.orEmpty(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                info.uploaderSubscriberCount.takeIf { it >= 0 }?.let {
                    Text(formatCount(it, "subscribers"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (subscribed) OutlinedButton(onClick = onSubscribe) { Text("Subscribed") } else Button(onClick = onSubscribe) { Text("Subscribe") }
    }
}

@Composable
private fun ActionRow(info: StreamInfo, engine: PlaybackEngine, onSave: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (info.likeCount >= 0) {
            AssistChip(onClick = {}, label = { Text(formatCount(info.likeCount, "").trim()) }, leadingIcon = { Icon(Icons.Outlined.ThumbUp, null, Modifier.size(18.dp)) })
        }
        AssistChip(
            onClick = {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, info.url),
                        null
                    )
                )
            },
            label = { Text("Share") },
            leadingIcon = { Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)) }
        )
        AssistChip(
            onClick = onSave,
            label = { Text("Save") },
            leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, null, Modifier.size(18.dp)) }
        )
        AssistChip(
            onClick = { engine.setVideoEnabled(false) },
            label = { Text("Audio only") },
            leadingIcon = { Icon(Icons.Outlined.Headphones, null, Modifier.size(18.dp)) }
        )
        AssistChip(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(info.url))) },
            label = { Text("Browser") },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null, Modifier.size(18.dp)) }
        )
    }
}

@Composable
private fun DescriptionCard(info: StreamInfo) {
    val text = info.description?.content.orEmpty()
    if (text.isBlank()) return
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = { expanded = !expanded }
    ) {
        Text(
            androidx.core.text.HtmlCompat.fromHtml(text, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp)
        )
    }
}

internal fun StreamInfoItem.toVideoItem() = VideoItem(
    url = url,
    title = name,
    channel = uploaderName.orEmpty(),
    channelUrl = uploaderUrl,
    thumbnail = ImageStrategy.choosePreferredImage(thumbnails),
    avatar = ImageStrategy.choosePreferredImage(uploaderAvatars),
    durationSeconds = duration,
    views = viewCount.takeIf { it >= 0 },
    uploaded = textualUploadDate,
    isLive = streamType == org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM,
    isShort = isShortFormContent
)

/** "3 days ago" from the exact upload date when there is one, else YouTube's own text. */
private fun formatUploaded(info: StreamInfo): String? {
    val date = info.uploadDate
    if (date != null) {
        val millis = date.offsetDateTime().toInstant().toEpochMilli()
        return android.text.format.DateUtils.getRelativeTimeSpanString(
            millis, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    return info.textualUploadDate?.takeIf { it.isNotBlank() }
}

internal fun StreamInfo.toVideoItemForSave() = VideoItem(
    url = url,
    title = name,
    channel = uploaderName.orEmpty(),
    channelUrl = uploaderUrl,
    thumbnail = ImageStrategy.choosePreferredImage(thumbnails),
    avatar = ImageStrategy.choosePreferredImage(uploaderAvatars),
    durationSeconds = duration,
    views = viewCount.takeIf { it >= 0 },
    uploaded = textualUploadDate,
    isLive = streamType == org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM,
    isShort = false
)
