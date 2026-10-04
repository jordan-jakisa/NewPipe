package dev.jordanempire.youflow.ui.shorts

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.media.engine.ShortsPlayer
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.ErrorBox
import dev.jordanempire.youflow.ui.components.LoadingBox
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.components.Thumbnail
import dev.jordanempire.youflow.ui.data.ListCache
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.UiState
import dev.jordanempire.youflow.ui.model.VideoItem
import dev.jordanempire.youflow.ui.util.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShortsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    private val _state = MutableStateFlow<UiState<List<VideoItem>>>(UiState.Loading)
    val state = _state.asStateFlow()

    val likedUrls: StateFlow<Set<String>> = repo.likedVideos()
        .map { list -> list.map { it.url }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleLike(video: VideoItem) {
        viewModelScope.launch { repo.setLiked(video, video.url !in likedUrls.value) }
    }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val cached = ListCache.read(getApplication(), "shorts")
            _state.value = if (cached != null) UiState.Content(cached.shuffled()) else UiState.Loading
            try {
                val fresh = repo.shorts()
                if (fresh.isNotEmpty()) ListCache.write(getApplication(), "shorts", fresh)
                // Do not swap the list under the user's finger when cached shorts are already playing.
                if (cached == null) _state.value = UiState.Content(fresh)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (cached == null) _state.value = e.toUiError()
            }
        }
    }
}

/** Full screen vertical pager. One shared player moves to whichever short is on screen. */
@Composable
fun ShortsScreen(actions: AppActions, contentPadding: PaddingValues, vm: ShortsViewModel = viewModel()) {
    val context = LocalContext.current
    val player = remember { ShortsPlayer.get(context.applicationContext as Application) }
    val engine = remember { PlaybackEngine.get(context.applicationContext as Application) }
    val state by vm.state.collectAsState()

    // Only one thing plays at a time: pause the main player here, and the short when leaving.
    DisposableEffect(Unit) {
        engine.exo.playWhenReady = false
        onDispose { player.stop() }
    }

    // Keep the whole short above the bottom navigation bar (and the mini player when it shows).
    Box(Modifier.fillMaxSize().background(Color.Black).padding(bottom = contentPadding.calculateBottomPadding())) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()

            is UiState.Error -> ErrorBox(s, vm::load)

            is UiState.Content -> if (s.data.isEmpty()) {
                MessageBox("No Shorts yet", "Subscribe to channels and their Shorts will show up here.")
            } else {
                ShortsPager(s.data, player, actions, vm)
            }
        }
    }
}

@Composable
private fun ShortsPager(items: List<VideoItem>, player: ShortsPlayer, actions: AppActions, vm: ShortsViewModel) {
    val context = LocalContext.current
    val liked by vm.likedUrls.collectAsState()
    val pager = rememberPagerState { items.size }
    val loadingSlots by player.loading.collectAsState()
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(pager, items) {
        snapshotFlow { pager.settledPage }.collect { page ->
            paused = false
            player.focus(page, items.map { it.url })
        }
    }

    // One page either side is composed too, so the next short's first frame is already drawn.
    VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
        val item = items[page]
        val loading = (page % 3) in loadingSlots
        Box(
            Modifier.fillMaxSize().pointerInput(page) {
                detectTapGestures(onTap = {
                    player.togglePlayPause(page)
                    paused = !paused
                })
            }
        ) {
            if (kotlin.math.abs(page - pager.currentPage) <= 1) {
                ContentFrame(
                    player.exoFor(page),
                    surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // The poster stays until the first frame is ready.
            if (loading || kotlin.math.abs(page - pager.currentPage) > 1) {
                Thumbnail(item.thumbnail, Modifier.fillMaxSize(), contentDescription = null)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 72.dp, bottom = 20.dp)) {
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
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isLiked = item.url in liked
                RailButton(if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, if (isLiked) "Unlike" else "Like", isLiked) { vm.toggleLike(item) }
                RailButton(Icons.AutoMirrored.Filled.PlaylistAdd, "Save") { actions.saveVideo(item) }
                RailButton(Icons.Filled.Share, "Share") {
                    context.startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, item.url), null)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                RailButton(Icons.Filled.OpenInFull, "Full video") {
                    player.stop()
                    actions.openVideo(item)
                }
            }
            if (page == pager.currentPage) ShortsProgress(player, page, Modifier.align(Alignment.BottomCenter))
            if (page == pager.currentPage) {
                if (loading) LoadingIndicator(Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
                if (paused) Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.Center).size(72.dp))
            }
        }
    }
}

@Composable
private fun RailButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/** A hairline progress bar along the bottom edge, polled while the short plays. */
@Composable
private fun ShortsProgress(player: ShortsPlayer, page: Int, modifier: Modifier = Modifier) {
    var fraction by remember(page) { mutableFloatStateOf(0f) }
    LaunchedEffect(page) {
        while (true) {
            val exo = player.exoFor(page)
            val duration = exo.duration
            fraction = if (duration > 0) (exo.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
            delay(100)
        }
    }
    Box(modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.25f))) {
        Box(Modifier.fillMaxWidth(fraction).height(2.dp).background(Color.White))
    }
}
