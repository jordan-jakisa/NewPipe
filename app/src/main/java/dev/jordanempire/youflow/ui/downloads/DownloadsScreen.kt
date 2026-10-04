package dev.jordanempire.youflow.ui.downloads

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordanempire.youflow.ui.components.MessageBox
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.shandian.giga.get.DownloadMission
import us.shandian.giga.get.Mission
import us.shandian.giga.service.DownloadManager
import us.shandian.giga.service.DownloadManagerService

enum class DownloadState { Running, Paused, Queued, Failed, Finished }

/** One row of the list. [mission] is kept so the actions can reach the real download. */
data class DownloadRow(
    val id: Long,
    val name: String,
    val state: DownloadState,
    val done: Long,
    val length: Long,
    val bytesPerSecond: Long,
    val mission: Mission
)

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {
    private var manager: DownloadManager? = null
    private val lastSample = HashMap<Long, Pair<Long, Long>>() // id -> (done, time)
    private var poll: Job? = null

    private val _rows = MutableStateFlow<List<DownloadRow>?>(null)
    val rows = _rows.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            manager = (service as DownloadManagerService.DownloadManagerBinder).downloadManager
            startPolling()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            manager = null
        }
    }

    init {
        val intent = Intent(app, DownloadManagerService::class.java)
        app.startService(intent)
        app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun startPolling() {
        poll?.cancel()
        poll = viewModelScope.launch {
            while (true) {
                refresh()
                delay(600)
            }
        }
    }

    private fun refresh() {
        val m = manager ?: return
        val iterator = m.iterator
        iterator.start()
        iterator.end()
        val now = System.currentTimeMillis()
        val list = ArrayList<DownloadRow>()
        for (i in 0 until iterator.oldListSize) {
            val mission = iterator.getItem(i).mission ?: continue
            val id = mission.timestamp
            val name = mission.storage?.name ?: mission.source.orEmpty()
            if (mission is DownloadMission) {
                val state = when {
                    mission.errCode != DownloadMission.ERROR_NOTHING -> DownloadState.Failed
                    mission.running -> DownloadState.Running
                    mission.enqueued -> DownloadState.Queued
                    else -> DownloadState.Paused
                }
                val previous = lastSample[id]
                val speed = if (state == DownloadState.Running && previous != null && now > previous.second) {
                    ((mission.done - previous.first) * 1000 / (now - previous.second)).coerceAtLeast(0)
                } else {
                    0L
                }
                lastSample[id] = mission.done to now
                list += DownloadRow(id, name, state, mission.done, mission.length, speed, mission)
            } else {
                list += DownloadRow(id, name, DownloadState.Finished, mission.length, mission.length, 0, mission)
            }
        }
        _rows.value = list
    }

    fun toggle(row: DownloadRow) {
        val mission = row.mission as? DownloadMission ?: return
        when (row.state) {
            DownloadState.Running -> manager?.pauseMission(mission)
            else -> manager?.resumeMission(mission)
        }
    }

    fun delete(row: DownloadRow, alsoFile: Boolean) {
        manager?.deleteMission(row.mission, alsoFile)
        refresh()
    }

    fun pauseAll() = manager?.pauseAllMissions(false)
    fun resumeAll() = manager?.startAllMissions()

    fun clearFinished() {
        manager?.forgetFinishedDownloads()
        refresh()
    }

    override fun onCleared() {
        poll?.cancel()
        runCatching { getApplication<Application>().unbindService(connection) }
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(Locale.US, if (value >= 100) "%.0f %s" else "%.1f %s", value, units[unit])
}

@Composable
fun DownloadsScreen(onBack: () -> Unit, vm: DownloadsViewModel = viewModel()) {
    val rows by vm.rows.collectAsState()
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DownloadRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Pause all") }, onClick = {
                            menu = false
                            vm.pauseAll()
                        })
                        DropdownMenuItem(text = { Text("Resume all") }, onClick = {
                            menu = false
                            vm.resumeAll()
                        })
                        DropdownMenuItem(text = { Text("Clear finished") }, onClick = {
                            menu = false
                            vm.clearFinished()
                        })
                    }
                }
            )
        }
    ) { padding ->
        val list = rows
        when {
            list == null -> Unit

            list.isEmpty() -> MessageBox(
                "No downloads yet",
                "Use Download on a video's watch page. Files are saved to the folder chosen in Download settings.",
                modifier = Modifier.padding(padding)
            )

            else -> {
                val pending = list.filter { it.state != DownloadState.Finished }
                val finished = list.filter { it.state == DownloadState.Finished }
                LazyColumn(
                    Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                    contentPadding = WindowInsets.navigationBars.asPaddingValues()
                ) {
                    if (pending.isNotEmpty()) {
                        item("pending-header") { Header("In progress") }
                        items(pending, key = { "p${it.id}" }) { row ->
                            PendingRow(row, onToggle = { vm.toggle(row) }, onDelete = { deleting = row })
                        }
                    }
                    if (finished.isNotEmpty()) {
                        item("finished-header") { Header("Finished") }
                        items(finished, key = { "f${it.id}" }) { row ->
                            ListItem(
                                headlineContent = { Text(row.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(formatBytes(row.length)) },
                                trailingContent = {
                                    IconButton(onClick = { deleting = row }) { Icon(Icons.Rounded.Delete, "Delete") }
                                },
                                modifier = Modifier.clickable { open(context, row) }
                            )
                        }
                    }
                }
            }
        }
    }

    deleting?.let { row ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${row.name}\"?") },
            text = { Text("Remove the file from your device, or only forget it in this list.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    vm.delete(row, true)
                }) { Text("Delete file") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        deleting = null
                        vm.delete(row, false)
                    }) { Text("Forget only") }
                    TextButton(onClick = { deleting = null }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun Header(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun PendingRow(row: DownloadRow, onToggle: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val status = when (row.state) {
                    DownloadState.Running -> buildString {
                        append(formatBytes(row.done))
                        if (row.length > 0) append(" of ${formatBytes(row.length)}")
                        if (row.bytesPerSecond > 0) append(" · ${formatBytes(row.bytesPerSecond)}/s")
                    }

                    DownloadState.Paused -> "Paused · ${formatBytes(row.done)}"

                    DownloadState.Queued -> "Waiting in the queue"

                    DownloadState.Failed -> "Failed · tap play to retry"

                    DownloadState.Finished -> ""
                }
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.state == DownloadState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (row.state == DownloadState.Running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (row.state == DownloadState.Running) "Pause" else "Resume"
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Cancel download") }
        }
        if (row.length > 0) {
            LinearWavyProgressIndicator(
                progress = { (row.done.toFloat() / row.length).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun open(context: Context, row: DownloadRow) {
    val storage = row.mission.storage ?: return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(storage.uri, storage.type)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show() }
}
