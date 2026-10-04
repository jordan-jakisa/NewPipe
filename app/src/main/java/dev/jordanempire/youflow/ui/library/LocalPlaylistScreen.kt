package dev.jordanempire.youflow.ui.library

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.components.VideoRow
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.VideoItem
import dev.jordanempire.youflow.ui.util.keyedViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocalPlaylistViewModel(app: Application, val playlistId: Long) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    val videos: StateFlow<List<Pair<Long, VideoItem>>?> =
        repo.playlistVideos(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val name: StateFlow<String> = repo.playlistName(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun remove(streamId: Long) = viewModelScope.launch { repo.removeFromPlaylist(playlistId, streamId) }
    fun rename(name: String) = viewModelScope.launch { repo.renamePlaylist(playlistId, name) }
    fun delete(done: () -> Unit) = viewModelScope.launch {
        repo.deletePlaylist(playlistId)
        done()
    }
}

@Composable
fun LocalPlaylistScreen(playlistId: Long, actions: AppActions, onBack: () -> Unit) {
    val vm = keyedViewModel("local-playlist:$playlistId") { app -> LocalPlaylistViewModel(app, playlistId) }
    val videos by vm.videos.collectAsState()
    val name by vm.name.collectAsState()
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    if (name != YouTubeRepository.WATCH_LATER) {
                        IconButton(onClick = { renaming = true }) { Icon(Icons.Outlined.Edit, "Rename") }
                    }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete playlist") }
                }
            )
        }
    ) { padding ->
        val list = videos
        when {
            list == null -> Unit

            list.isEmpty() -> MessageBox("Nothing here yet", "Use Save on a video to add it to this playlist.", modifier = Modifier.padding(padding))

            else -> LazyColumn(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
                item("actions") {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { actions.playVideos(list.map { it.second }, 0) }) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Text("Play all", Modifier.padding(start = 6.dp))
                        }
                        FilledTonalButton(onClick = { actions.playVideos(list.map { it.second }.shuffled(), 0) }) {
                            Icon(Icons.Filled.Shuffle, null, Modifier.size(18.dp))
                            Text("Shuffle", Modifier.padding(start = 6.dp))
                        }
                    }
                }
                itemsIndexed(list, key = { index, item -> "$index:${item.first}" }) { index, (streamId, video) ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        VideoRow(video, index = index, modifier = Modifier.weight(1f), onClick = { actions.playVideos(list.map { it.second }, index) })
                        IconButton(onClick = { vm.remove(streamId) }) { Icon(Icons.Outlined.RemoveCircleOutline, "Remove from playlist") }
                    }
                }
            }
        }
    }

    if (renaming) NamePlaylistDialog("Rename playlist", name, { renaming = false }) { vm.rename(it) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"$name\"?") },
            text = { Text("The videos stay in your history, only the playlist is removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
