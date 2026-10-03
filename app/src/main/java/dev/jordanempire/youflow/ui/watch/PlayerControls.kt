package dev.jordanempire.youflow.ui.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.jordanempire.youflow.media.engine.Phase
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.media.engine.PlayerState
import dev.jordanempire.youflow.ui.util.formatDuration
import kotlinx.coroutines.delay

private val Scrim = Color.Black.copy(alpha = 0.45f)

/**
 * Tap to show/hide, double tap left/right third to seek 10s. Everything sits on a dark scrim so it
 * reads on any video.
 */
@Composable
fun PlayerControls(
    engine: PlaybackEngine,
    state: PlayerState,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }
    var interaction by remember { mutableLongStateOf(0L) }
    var seekFlash by remember { mutableStateOf<String?>(null) }

    // Hide 3s after the last interaction while playing.
    LaunchedEffect(visible, state.isPlaying, interaction) {
        if (visible && state.isPlaying) {
            delay(3_000)
            visible = false
        }
    }
    LaunchedEffect(seekFlash) {
        if (seekFlash != null) {
            delay(600)
            seekFlash = null
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { visible = !visible; interaction++ },
                    onDoubleTap = { offset ->
                        when {
                            offset.x < size.width / 3f -> { engine.seekBy(-10_000); seekFlash = "-10s" }
                            offset.x > size.width * 2f / 3f -> { engine.seekBy(10_000); seekFlash = "+10s" }
                            else -> engine.togglePlayPause()
                        }
                        interaction++
                    }
                )
            }
    ) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Scrim, Color.Transparent, Color.Transparent, Scrim)))) {
                // Top bar
                Row(Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                // Transport
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleButton(Icons.Filled.SkipPrevious, "Previous", enabled = true) { engine.previous(); interaction++ }
                    PlayPauseButton(state) { engine.togglePlayPause(); interaction++ }
                    CircleButton(Icons.Filled.SkipNext, "Next", enabled = state.hasNext) { engine.next(); interaction++ }
                }
                // Bottom: seek bar, time, fullscreen
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp)) {
                    SeekBar(engine, state, onInteraction = { interaction++ })
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TimeLabel(engine, state, Modifier.weight(1f))
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = if (fullscreen) "Exit fullscreen" else "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (state.phase == Phase.Loading || state.phase == Phase.Buffering) {
            LoadingIndicator(Modifier.align(Alignment.Center).size(56.dp), color = Color.White)
        }
        seekFlash?.let {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
            ) { Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) }
        }
        state.error?.takeIf { state.phase == Phase.Error }?.let { message ->
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Can't play this video", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(message, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { engine.skipTo(state.index) }) { Icon(Icons.Filled.Replay, "Retry", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun CircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, label = "alpha")
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.35f * alpha),
        contentColor = Color.White.copy(alpha = alpha),
        modifier = Modifier.size(52.dp)
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, modifier = Modifier.size(28.dp)) } }
}

/** The play button squares off while playing and rounds again when paused. */
@Composable
private fun PlayPauseButton(state: PlayerState, onClick: () -> Unit) {
    val corner by animateFloatAsState(if (state.playWhenReady) 28f else 50f, label = "corner")
    val container by animateColorAsState(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f), label = "container")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = corner.toInt()),
        color = container,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                when {
                    state.phase == Phase.Ended -> Icons.Filled.Replay
                    state.playWhenReady -> Icons.Filled.Pause
                    else -> Icons.Filled.PlayArrow
                },
                contentDescription = if (state.playWhenReady) "Pause" else "Play",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun SeekBar(engine: PlaybackEngine, state: PlayerState, onInteraction: () -> Unit) {
    val position by engine.position.collectAsState()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1)
    if (state.isLive) return
    Slider(
        value = dragging ?: (position.toFloat() / duration).coerceIn(0f, 1f),
        onValueChange = { dragging = it; onInteraction() },
        onValueChangeFinished = {
            dragging?.let { engine.seekTo((it * duration).toLong()) }
            dragging = null
        },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        )
    )
}

@Composable
private fun TimeLabel(engine: PlaybackEngine, state: PlayerState, modifier: Modifier) {
    val position by engine.position.collectAsState()
    val text = if (state.isLive) "LIVE" else "${formatDuration(position / 1000).ifEmpty { "0:00" }} / ${formatDuration(state.durationMs / 1000).ifEmpty { "0:00" }}"
    Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = modifier.padding(start = 12.dp))
}
