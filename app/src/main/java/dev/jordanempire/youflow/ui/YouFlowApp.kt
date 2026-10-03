package dev.jordanempire.youflow.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.media.engine.QueueEntry
import dev.jordanempire.youflow.ui.channel.ChannelScreen
import dev.jordanempire.youflow.ui.home.HomeScreen
import dev.jordanempire.youflow.ui.model.VideoItem
import dev.jordanempire.youflow.ui.playlist.PlaylistScreen
import dev.jordanempire.youflow.ui.search.SearchScreen
import dev.jordanempire.youflow.ui.shorts.ShortsScreen
import dev.jordanempire.youflow.ui.subscriptions.SubscriptionsScreen
import dev.jordanempire.youflow.ui.watch.MiniPlayer
import dev.jordanempire.youflow.ui.watch.WatchScreen
import dev.jordanempire.youflow.ui.you.YouScreen

private enum class Tab(val label: String, val selected: ImageVector, val unselected: ImageVector) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Shorts("Shorts", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    Subscriptions("Subscriptions", Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
    You("You", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
}

/** Window level state the activity needs to react to (PiP, fullscreen). */
class WatchWindowState {
    var expanded by mutableStateOf(false)
    var fullscreen by mutableStateOf(false)
    var inPip by mutableStateOf(false)
}

private fun VideoItem.toEntry() = QueueEntry(url, title, channel, thumbnail)

@OptIn(UnstableApi::class)
@Composable
fun YouFlowApp(window: WatchWindowState, onOpenClassicUi: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { PlaybackEngine.get(context.applicationContext as Application) }
    val playerState by engine.state.collectAsState()

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var searching by rememberSaveable { mutableStateOf(false) }
    // Pushed screens on top of the tabs: "c|<channel url>" or "p|<playlist url>".
    var stack by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    fun push(route: String) { stack = ArrayList(stack + route) }
    fun pop() { stack = ArrayList(stack.dropLast(1)) }

    val actions = AppActions(
        openVideo = { video -> engine.play(listOf(video.toEntry())); window.expanded = true },
        openChannel = { push("c|$it"); searching = false; window.expanded = false },
        openPlaylist = { push("p|$it"); searching = false; window.expanded = false },
        playVideos = { videos, index ->
            engine.play(videos.map { it.toEntry() }, index)
            window.expanded = true
        },
        openClassicUi = onOpenClassicUi,
        openSettings = onOpenSettings
    )

    // Picture in picture shows only the video.
    if (window.inPip) {
        ContentFrame(
            player = engine.exo,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.fillMaxSize().background(Color.Black)
        )
        return
    }

    val hasVideo = playerState.entry != null
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (tab != Tab.Shorts) TopAppBar(
                title = { Text("YouFlow", style = MaterialTheme.typography.headlineSmallEmphasized) },
                actions = {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Column {
                if (hasVideo && !window.expanded) {
                    MiniPlayer(engine, onExpand = { window.expanded = true }, onClose = { engine.stop() })
                }
                ShortNavigationBar {
                    Tab.entries.forEach { entry ->
                        ShortNavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry; stack = ArrayList() },
                            icon = { Icon(if (tab == entry) entry.selected else entry.unselected, contentDescription = null) },
                            label = { Text(entry.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.Home -> HomeScreen(actions, padding)
            Tab.Shorts -> ShortsScreen(actions)
            Tab.Subscriptions -> SubscriptionsScreen(actions, padding)
            Tab.You -> YouScreen(actions, padding)
        }
    }

    val top = stack.lastOrNull()
    AnimatedVisibility(
        visible = top != null,
        enter = fadeIn() + slideInVertically { it / 12 },
        exit = fadeOut() + slideOutVertically { it / 12 }
    ) {
        val route = remember(top) { top } ?: return@AnimatedVisibility
        BackHandler(onBack = ::pop)
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Surface(Modifier.weight(1f)) {
                val kind = route.substringBefore('|')
                val target = route.substringAfter('|')
                if (kind == "c") ChannelScreen(target, actions, ::pop) else PlaylistScreen(target, actions, ::pop)
            }
            if (hasVideo) MiniPlayer(engine, onExpand = { window.expanded = true }, onClose = { engine.stop() })
        }
    }

    AnimatedVisibility(
        visible = searching,
        enter = fadeIn() + slideInVertically { it / 12 },
        exit = fadeOut() + slideOutVertically { it / 12 }
    ) {
        SearchScreen(actions = actions, onClose = { searching = false })
    }

    AnimatedVisibility(
        visible = window.expanded && hasVideo,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        BackHandler {
            if (window.fullscreen) window.fullscreen = false else window.expanded = false
        }
        WatchScreen(
            actions = actions,
            fullscreen = window.fullscreen,
            onToggleFullscreen = { window.fullscreen = !window.fullscreen },
            onCollapse = { window.fullscreen = false; window.expanded = false }
        )
    }
}
