package dev.jordanempire.youflow.ui.subscriptions

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.Avatar
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.ChannelItem
import dev.jordanempire.youflow.ui.util.formatCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageSubscriptionsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)
    val channels: StateFlow<List<ChannelItem>?> = repo.subscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun unsubscribe(channel: ChannelItem) = viewModelScope.launch { repo.unsubscribe(channel.url) }
    fun setNotify(channel: ChannelItem, enabled: Boolean) = viewModelScope.launch { repo.setNotifications(channel.url, enabled) }
}

@Composable
fun ManageSubscriptionsScreen(actions: AppActions, onBack: () -> Unit, vm: ManageSubscriptionsViewModel = viewModel()) {
    val channels by vm.channels.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage channels") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        val list = channels
        when {
            list == null -> Unit

            list.isEmpty() -> MessageBox("No subscriptions", "Subscribe to a channel and it shows up here.", modifier = Modifier.padding(padding))

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
                items(list, key = { it.url }) { channel ->
                    Row(
                        Modifier.fillMaxWidth().clickable { actions.openChannel(channel.url) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(channel.avatar, channel.name, 48.dp)
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(channel.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            channel.subscribers?.let {
                                Text(formatCount(it, "subscribers"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { vm.setNotify(channel, !channel.notify) }) {
                            Icon(
                                if (channel.notify) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                                contentDescription = if (channel.notify) "Turn off notifications" else "Notify me about new videos"
                            )
                        }
                        OutlinedButton(onClick = { vm.unsubscribe(channel) }) { Text("Unsubscribe") }
                    }
                }
            }
        }
    }
}
