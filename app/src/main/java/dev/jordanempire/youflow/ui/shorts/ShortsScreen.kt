package dev.jordanempire.youflow.ui.shorts

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.media.engine.ShortsPlayer
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.ErrorBox
import dev.jordanempire.youflow.ui.components.LoadingBox
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.components.Thumbnail
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.UiState
import dev.jordanempire.youflow.ui.model.VideoItem
import dev.jordanempire.youflow.ui.util.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShortsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    private val _state = MutableStateFlow<UiState<List<VideoItem>>>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(repo.shorts())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.toUiError()
            }
        }
    }
}

/** Full screen vertical pager. One shared player moves to whichever short is on screen. */
@Composable
fun ShortsScreen(actions: AppActions, vm: ShortsViewModel = viewModel()) {
    val context = LocalContext.current
    val player = remember { ShortsPlayer.get(context.applicationContext as Application) }
    val engine = remember { PlaybackEngine.get(context.applicationContext as Application) }
    val state by vm.state.collectAsState()

    // Only one thing plays at a time: pause the main player here, and the short when leaving.
    DisposableEffect(Unit) {
        engine.exo.playWhenReady = false
        onDispose { player.stop() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s, vm::load)
            is UiState.Content -> if (s.data.isEmpty()) {
                MessageBox("No Shorts yet", "Subscribe to channels and their Shorts will show up here.")
            } else {
                ShortsPager(s.data, player, actions)
            }
        }
    }
}

@Composable
private fun ShortsPager(items: List<VideoItem>, player: ShortsPlayer, actions: AppActions) {
    val pager = rememberPagerState { items.size }
    val loading by player.loading.collectAsState()
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(pager, items) {
        snapshotFlow { pager.settledPage }.collect { page ->
            paused = false
            player.play(items[page].url)
        }
    }

    VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
        val item = items[page]
        Box(
            Modifier.fillMaxSize().pointerInput(page) {
                detectTapGestures(onTap = { player.togglePlayPause(); paused = !paused })
            }
        ) {
            if (page == pager.currentPage) {
                ContentFrame(player.exo, surfaceType = SURFACE_TYPE_SURFACE_VIEW, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Thumbnail(item.thumbnail, Modifier.fillMaxSize().then(if (page == pager.currentPage && !loading) Modifier.size(0.dp) else Modifier), contentDescription = null)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 80.dp, bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(item.avatar, Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer))
                    Text(
                        item.channel,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Text(
                    item.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (page == pager.currentPage) {
                if (loading) LoadingIndicator(Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
                if (paused) Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.Center).size(72.dp))
            }
        }
    }
}
