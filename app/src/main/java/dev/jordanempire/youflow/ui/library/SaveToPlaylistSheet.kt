package dev.jordanempire.youflow.ui.library

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.PlaylistItem
import dev.jordanempire.youflow.ui.model.VideoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    val playlists: StateFlow<List<PlaylistItem>> =
        repo.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(video: VideoItem, playlist: PlaylistItem, done: () -> Unit) {
        viewModelScope.launch {
            repo.addToPlaylist(playlist.url.removePrefix("local:").toLong(), video)
            done()
        }
    }

    fun create(name: String, video: VideoItem?, done: () -> Unit) {
        viewModelScope.launch {
            repo.createPlaylist(name.trim(), video)
            done()
        }
    }

    fun watchLater(video: VideoItem, done: () -> Unit) {
        viewModelScope.launch {
            repo.addToWatchLater(video)
            done()
        }
    }
}

/** Pick a local playlist (or make a new one) for [video]. */
@Composable
fun SaveToPlaylistSheet(video: VideoItem, onDismiss: () -> Unit, vm: LibraryViewModel = viewModel()) {
    val context = LocalContext.current
    val playlists by vm.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }
    fun saved(name: String) {
        Toast.makeText(context, "Saved to $name", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
        Text("Save to", style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        LazyColumn(Modifier.navigationBarsPadding()) {
            item("watch-later") {
                ListItem(
                    headlineContent = { Text("Watch later") },
                    leadingContent = { Icon(Icons.Filled.WatchLater, null) },
                    modifier = Modifier.clickable { vm.watchLater(video) { saved("Watch later") } }
                )
            }
            items(playlists.filter { it.name != YouTubeRepository.WATCH_LATER }, key = { it.url }) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name) },
                    supportingContent = { Text("${playlist.streamCount} videos") },
                    leadingContent = { Icon(Icons.Outlined.PlaylistPlay, null) },
                    modifier = Modifier.clickable { vm.save(video, playlist) { saved(playlist.name) } }
                )
            }
            item("new") {
                ListItem(
                    headlineContent = { Text("New playlist") },
                    leadingContent = { Icon(Icons.Filled.Add, null) },
                    modifier = Modifier.clickable { creating = true }
                )
            }
        }
    }
    if (creating) {
        NamePlaylistDialog(
            title = "New playlist",
            initial = "",
            onDismiss = { creating = false },
            onConfirm = { name -> vm.create(name, video) { saved(name) } }
        )
    }
}

@Composable
fun NamePlaylistDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onConfirm(name)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
