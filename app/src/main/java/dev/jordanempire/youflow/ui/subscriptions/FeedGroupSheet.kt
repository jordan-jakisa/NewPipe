package dev.jordanempire.youflow.ui.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jordanempire.youflow.ui.components.Avatar
import dev.jordanempire.youflow.ui.model.ChannelItem

/** Create or edit a feed group: a name and which subscribed channels belong to it. */
@Composable
fun FeedGroupSheet(
    title: String,
    initialName: String,
    channels: List<ChannelItem>,
    loadMembers: suspend () -> List<Long>,
    onDismiss: () -> Unit,
    onSave: (name: String, subscriptionIds: List<Long>) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initialName) }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    LaunchedEffect(Unit) { selected = loadMembers().toSet() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(title, style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(channels, key = { it.uid }) { channel ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { selected = if (channel.uid in selected) selected - channel.uid else selected + channel.uid }
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(channel.avatar, channel.name, 36.dp)
                    Text(channel.name, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 1)
                    Checkbox(checked = channel.uid in selected, onCheckedChange = null)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onDelete != null) {
                OutlinedButton(onClick = {
                    onDelete()
                    onDismiss()
                }) { Text("Delete") }
            }
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(name.trim(), selected.toList())
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
        }
    }
}
