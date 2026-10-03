package dev.jordanempire.youflow.ui.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.media.engine.PlayerState

private val Speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@Composable
fun PlayerSettingsSheet(engine: PlaybackEngine, state: PlayerState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.qualities.isNotEmpty()) {
                Section("Quality") {
                    state.qualities.forEachIndexed { index, label ->
                        FilterChip(
                            selected = index == state.selectedQuality,
                            onClick = { engine.selectQuality(index) },
                            label = { Text(label) }
                        )
                    }
                }
            }
            Section("Playback speed") {
                Speeds.forEach { speed ->
                    FilterChip(
                        selected = speed == state.speed,
                        onClick = { engine.setSpeed(speed) },
                        label = { Text(if (speed == 1f) "Normal" else "${speed}x") }
                    )
                }
            }
            Section("Captions") {
                FilterChip(selected = state.selectedCaption < 0, onClick = { engine.selectCaption(-1) }, label = { Text("Off") })
                state.captions.forEachIndexed { index, label ->
                    FilterChip(selected = index == state.selectedCaption, onClick = { engine.selectCaption(index) }, label = { Text(label) })
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
