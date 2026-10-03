package dev.jordanempire.youflow.ui.you

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import dev.jordanempire.youflow.ui.components.PlaylistRow
import dev.jordanempire.youflow.ui.components.VideoTile
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.PlaylistItem
import dev.jordanempire.youflow.ui.model.VideoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class YouViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    val history: StateFlow<List<VideoItem>> = repo.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists: StateFlow<List<PlaylistItem>> = repo.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun YouScreen(actions: AppActions, contentPadding: PaddingValues, vm: YouViewModel = viewModel()) {
    val history by vm.history.collectAsState()
    val playlists by vm.playlists.collectAsState()

    LazyColumn(contentPadding = contentPadding) {
        item(key = "history-header") { SectionHeader("History") }
        item(key = "history") {
            if (history.isEmpty()) {
                Text("Videos you watch will show up here.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(history.take(20), key = { it.url }) { video -> VideoTile(video, onClick = { actions.openVideo(video) }) }
                }
            }
        }
        item(key = "playlists-header") { SectionHeader("Playlists") }
        if (playlists.isEmpty()) {
            item(key = "playlists-empty") {
                Text("Playlists you create will show up here.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(playlists, key = { it.url }) { playlist -> PlaylistRow(playlist, onClick = {}) }
        }
        item(key = "more-header") { SectionHeader("More") }
        item(key = "settings") {
            ListItem(
                headlineContent = { Text("Settings") },
                leadingContent = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                modifier = Modifier.clickable(onClick = actions.openSettings)
            )
        }
        item(key = "classic") {
            ListItem(
                headlineContent = { Text("Classic app") },
                supportingContent = { Text("Backups, downloads and the original screens") },
                leadingContent = { Icon(Icons.Outlined.Subscriptions, contentDescription = null) },
                modifier = Modifier.clickable(onClick = actions.openClassicUi)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp))
}
