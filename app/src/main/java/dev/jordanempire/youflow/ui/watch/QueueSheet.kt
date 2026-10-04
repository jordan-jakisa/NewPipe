package dev.jordanempire.youflow.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.media.engine.PlayerState

@Composable
fun QueueSheet(engine: PlaybackEngine, state: PlayerState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text("Queue  ${state.queue.size}", style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        LazyColumn(Modifier.navigationBarsPadding()) {
            itemsIndexed(state.queue, key = { index, e -> "$index:${e.url}" }) { index, entry ->
                val current = index == state.index
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { engine.skipTo(index) }
                        .padding(start = 20.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(
                            if (current) state.info?.name ?: entry.title else entry.title.ifBlank { entry.url },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.uploader.isNotBlank()) {
                            Text(entry.uploader, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    IconButton(onClick = { engine.removeFromQueue(index) }) { Icon(Icons.Rounded.Close, "Remove from queue") }
                }
            }
        }
    }
}
