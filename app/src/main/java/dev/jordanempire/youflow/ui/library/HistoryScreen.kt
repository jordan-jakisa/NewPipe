package dev.jordanempire.youflow.ui.library

import android.app.Application
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.components.VideoRow
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.VideoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    val history: StateFlow<List<VideoItem>?> =
        repo.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clear() = viewModelScope.launch { repo.clearHistory() }
}

@Composable
fun HistoryScreen(actions: AppActions, onBack: () -> Unit, vm: HistoryViewModel = viewModel()) {
    val history by vm.history.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { confirm = true }) { Icon(Icons.Outlined.DeleteSweep, "Clear history") } }
            )
        }
    ) { padding ->
        val list = history
        when {
            list == null -> Unit
            list.isEmpty() -> MessageBox("No history yet", "Videos you watch will show up here.", modifier = Modifier.padding(padding))
            else -> LazyColumn(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
                itemsIndexed(list, key = { index, v -> "$index:${v.url}" }) { _, video ->
                    VideoRow(video, onClick = { actions.openVideo(video) })
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Clear watch history?") },
            text = { Text("This also forgets where you stopped in each video.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.clear() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}
