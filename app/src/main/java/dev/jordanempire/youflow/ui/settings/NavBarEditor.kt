package dev.jordanempire.youflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.jordanempire.youflow.ui.Tab
import kotlin.math.roundToInt

private val RowHeight = 64.dp

/**
 * Edits the bottom bar: a live preview of the bar as it will look, and below it the tabs to drag
 * into order. Shorts can be switched off; the other tabs always stay.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavBarEditor(order: List<String>, showShorts: Boolean, onMove: (from: Int, to: Int) -> Unit, onShowShorts: (Boolean) -> Unit) {
    val tabs = order.mapNotNull { key -> Tab.entries.firstOrNull { it.name == key } }
    val visible = tabs.filter { showShorts || it != Tab.Shorts }
    val haptics = LocalHapticFeedback.current
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                ShortNavigationBar {
                    visible.forEachIndexed { index, tab ->
                        ShortNavigationBarItem(
                            selected = index == 0,
                            onClick = {},
                            icon = { Icon(if (index == 0) tab.selected else tab.unselected, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                tabs.forEachIndexed { index, tab ->
                    key(tab.name) {
                        val currentIndex by rememberUpdatedState(index)
                        val dragging = draggingKey == tab.name
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(RowHeight)
                                .zIndex(if (dragging) 1f else 0f)
                                .offset { IntOffset(0, if (dragging) dragOffset.roundToInt() else 0) }
                                .shadow(if (dragging) 8.dp else 0.dp, RoundedCornerShape(16.dp))
                                .background(if (dragging) MaterialTheme.colorScheme.surfaceContainerHighest else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder ${tab.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(12.dp)
                                    .pointerInput(tab.name) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggingKey = tab.name
                                                dragOffset = 0f
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragEnd = {
                                                draggingKey = null
                                                dragOffset = 0f
                                            },
                                            onDragCancel = {
                                                draggingKey = null
                                                dragOffset = 0f
                                            }
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            // Swap places each time the row has moved past half of its neighbour.
                                            if (dragOffset > rowPx / 2 && currentIndex < tabs.lastIndex) {
                                                onMove(currentIndex, currentIndex + 1)
                                                dragOffset -= rowPx
                                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                            } else if (dragOffset < -rowPx / 2 && currentIndex > 0) {
                                                onMove(currentIndex, currentIndex - 1)
                                                dragOffset += rowPx
                                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                            }
                                        }
                                    }
                            )
                            Icon(tab.unselected, null, Modifier.padding(start = 8.dp).size(24.dp))
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(tab.label, style = MaterialTheme.typography.bodyLarge)
                                val note = when {
                                    tab == Tab.Shorts && !showShorts -> "Hidden"
                                    tab == visible.firstOrNull() -> "Opens first"
                                    else -> null
                                }
                                if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (tab == Tab.Shorts) {
                                Switch(checked = showShorts, onCheckedChange = onShowShorts, modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
