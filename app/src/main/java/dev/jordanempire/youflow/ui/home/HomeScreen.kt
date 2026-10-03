package dev.jordanempire.youflow.ui.home

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.StateHost
import dev.jordanempire.youflow.ui.components.VideoCard
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.UiState
import dev.jordanempire.youflow.ui.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    val kiosks = repo.kiosks

    private val _selected = MutableStateFlow(0)
    val selected = _selected.asStateFlow()
    private val _state = MutableStateFlow<UiState<List<VideoItem>>>(UiState.Loading)
    val state = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    private var job: Job? = null

    init {
        load(pullToRefresh = false)
    }

    fun select(index: Int) {
        if (index == _selected.value) return
        _selected.value = index
        load(pullToRefresh = false)
    }

    fun refresh() = load(pullToRefresh = true)

    private fun load(pullToRefresh: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            val kiosk = kiosks.getOrNull(_selected.value)
            if (kiosk == null) {
                _state.value = UiState.Error("No categories are available right now.")
                return@launch
            }
            if (pullToRefresh) _refreshing.value = true else _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(repo.kioskVideosWithRetry(kiosk, forceLoad = pullToRefresh))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                UiState.Error(e.message ?: e.javaClass.simpleName)
            }
            _refreshing.value = false
        }
    }
}

@Composable
fun HomeScreen(actions: AppActions, contentPadding: PaddingValues, vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val selected by vm.selected.collectAsState()
    val refreshing by vm.refreshing.collectAsState()

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh, modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
        StateHost(state, onRetry = vm::refresh, isEmpty = { it.isEmpty() }) { videos ->
            LazyColumn(contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
                item(key = "chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(vm.kiosks) { index, kiosk ->
                            FilterChip(selected = index == selected, onClick = { vm.select(index) }, label = { Text(kiosk.title) })
                        }
                    }
                }
                items(videos, key = { it.url }) { video ->
                    VideoCard(
                        video,
                        onClick = { actions.openVideo(video.url, video.title) },
                        onChannelClick = { video.channelUrl?.let(actions.openChannel) }
                    )
                }
            }
        }
    }
}
