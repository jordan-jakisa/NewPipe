package dev.jordanempire.youflow.ui.subscriptions

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.components.Thumbnail
import dev.jordanempire.youflow.ui.components.VideoCard
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.ChannelItem
import dev.jordanempire.youflow.ui.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)

    val channels: StateFlow<List<ChannelItem>?> = repo.subscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _feed = MutableStateFlow<List<VideoItem>>(emptyList())
    val feed = _feed.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            _feed.value = runCatching { repo.feed() }.getOrDefault(emptyList())
            if (_feed.value.isEmpty()) refresh()
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            _error.value = null
            try {
                repo.refreshFeed()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _error.value = e.message ?: e.javaClass.simpleName
            }
            _feed.value = runCatching { repo.feed() }.getOrDefault(emptyList())
            _refreshing.value = false
        }
    }
}

@Composable
fun SubscriptionsScreen(actions: AppActions, contentPadding: PaddingValues, vm: SubscriptionsViewModel = viewModel()) {
    val channels by vm.channels.collectAsState()
    val feed by vm.feed.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val error by vm.error.collectAsState()

    val top = Modifier.padding(top = contentPadding.calculateTopPadding())
    when {
        channels == null -> Unit
        channels!!.isEmpty() -> MessageBox(
            title = "No subscriptions yet",
            body = "Search for a channel and tap Subscribe, or import a NewPipe backup from the classic app.",
            actionLabel = "Open classic app",
            onAction = actions.openClassicUi,
            modifier = top
        )
        else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh, modifier = top) {
            LazyColumn(contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
                item(key = "strip") { ChannelStrip(channels!!, actions) }
                if (refreshing) item(key = "progress") {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                }
                if (error != null) item(key = "error") {
                    Text("Some channels failed to update: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                if (feed.isEmpty() && !refreshing) item(key = "empty") {
                    Text("No new videos yet. Pull down to refresh.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(feed, key = { it.url }) { video ->
                    VideoCard(video, onClick = { actions.openVideo(video.url, video.title) }, onChannelClick = { video.channelUrl?.let(actions.openChannel) })
                }
            }
        }
    }
}

@Composable
private fun ChannelStrip(channels: List<ChannelItem>, actions: AppActions) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(channels, key = { it.url }) { channel ->
            Column(Modifier.width(72.dp).clickable { actions.openChannel(channel.url) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Thumbnail(channel.avatar, Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer))
                Text(channel.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
