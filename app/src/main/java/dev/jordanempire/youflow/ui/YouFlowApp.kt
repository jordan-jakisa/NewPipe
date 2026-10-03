package dev.jordanempire.youflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Surface
import dev.jordanempire.youflow.ui.channel.ChannelScreen
import dev.jordanempire.youflow.ui.playlist.PlaylistScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import dev.jordanempire.youflow.ui.home.HomeScreen
import dev.jordanempire.youflow.ui.search.SearchScreen
import dev.jordanempire.youflow.ui.subscriptions.SubscriptionsScreen
import dev.jordanempire.youflow.ui.you.YouScreen

private enum class Tab(val label: String, val selected: ImageVector, val unselected: ImageVector) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Subscriptions("Subscriptions", Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
    You("You", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
}

@Composable
fun YouFlowApp(baseActions: AppActions) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var searching by rememberSaveable { mutableStateOf(false) }
    // Pushed screens on top of the tabs: "c|<channel url>" or "p|<playlist url>".
    var stack by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    fun push(route: String) { stack = ArrayList(stack + route) }
    fun pop() { stack = ArrayList(stack.dropLast(1)) }
    val actions = baseActions.copy(
        openChannel = { push("c|$it"); searching = false },
        openPlaylist = { push("p|$it"); searching = false }
    )
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("YouFlow", style = androidx.compose.material3.MaterialTheme.typography.headlineSmallEmphasized) },
                actions = {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            ShortNavigationBar {
                Tab.entries.forEach { entry ->
                    ShortNavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(if (tab == entry) entry.selected else entry.unselected, contentDescription = null) },
                        label = { Text(entry.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.Home -> HomeScreen(actions, padding)
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
        // Keep composing the last route while it animates out.
        val route = remember(top) { top } ?: return@AnimatedVisibility
        androidx.activity.compose.BackHandler(onBack = ::pop)
        Surface(Modifier.fillMaxSize()) {
            val kind = route.substringBefore('|')
            val target = route.substringAfter('|')
            if (kind == "c") ChannelScreen(target, actions, ::pop) else PlaylistScreen(target, actions, ::pop)
        }
    }

    AnimatedVisibility(
        visible = searching,
        enter = fadeIn() + slideInVertically { it / 12 },
        exit = fadeOut() + slideOutVertically { it / 12 }
    ) {
        SearchScreen(actions = actions, onClose = { searching = false })
    }
}
