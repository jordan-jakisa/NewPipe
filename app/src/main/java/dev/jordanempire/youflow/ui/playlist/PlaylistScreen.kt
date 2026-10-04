package dev.jordanempire.youflow.ui.playlist

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.ErrorBox
import dev.jordanempire.youflow.ui.components.LoadingBox
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.components.Thumbnail
import dev.jordanempire.youflow.ui.components.VideoRow
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.PlaylistDetails
import dev.jordanempire.youflow.ui.model.UiState
import dev.jordanempire.youflow.ui.model.VideoItem
import dev.jordanempire.youflow.ui.util.keyedViewModel
import dev.jordanempire.youflow.ui.util.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page

class PlaylistViewModel(app: Application, private val url: String) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    private var loaded: YouTubeRepository.LoadedPlaylist? = null
    private var next: Page? = null
    private var loadingMore = false

    private val _state = MutableStateFlow<UiState<Pair<PlaylistDetails, List<VideoItem>>>>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val l = repo.playlist(url)
                loaded = l
                next = l.next
                UiState.Content(l.details to l.videos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.toUiError()
            }
        }
    }

    fun loadMore() {
        val page = next ?: return
        val current = (_state.value as? UiState.Content)?.data ?: return
        if (loadingMore) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val more = repo.morePlaylistVideos(url, page)
                next = more.next
                _state.value = UiState.Content(current.first to (current.second + more.items))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                next = null
            }
            loadingMore = false
        }
    }
}

@Composable
fun PlaylistScreen(url: String, actions: AppActions, onBack: () -> Unit) {
    val vm = keyedViewModel("playlist:$url") { app -> PlaylistViewModel(app, url) }
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 4) vm.loadMore() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> LoadingBox(Modifier.padding(padding))

            is UiState.Error -> ErrorBox(s, vm::load, Modifier.padding(padding), "Couldn't load this playlist")

            is UiState.Content -> {
                val (details, videos) = s.data
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                    contentPadding = WindowInsets.navigationBars.asPaddingValues()
                ) {
                    item(key = "header") {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Thumbnail(
                                details.thumbnail,
                                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            )
                            Text(details.name, style = MaterialTheme.typography.headlineSmallEmphasized)
                            Text(
                                listOfNotNull(details.uploader, "${details.streamCount} videos").joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                Button(onClick = { actions.playVideos(videos, 0) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Play all", modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                    }
                    itemsIndexed(videos, key = { index, v -> "$index:${v.url}" }) { index, video ->
                        VideoRow(video, index = index, onClick = { actions.playVideos(videos, index) })
                    }
                }
            }
        }
    }
}
