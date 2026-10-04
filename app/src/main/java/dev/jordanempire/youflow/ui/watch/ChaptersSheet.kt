package dev.jordanempire.youflow.ui.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jordanempire.youflow.ui.components.Thumbnail
import dev.jordanempire.youflow.ui.util.formatDuration
import dev.jordanempire.youflow.util.image.ImageStrategy
import org.schabi.newpipe.extractor.stream.StreamSegment

/** Index of the chapter playing at [positionMs], or -1 when there are none. */
fun currentChapter(segments: List<StreamSegment>, positionMs: Long): Int = segments.indexOfLast { it.startTimeSeconds * 1000L <= positionMs }

@Composable
fun ChaptersSheet(segments: List<StreamSegment>, currentIndex: Int, onSeek: (Long) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text("Chapters", style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        LazyColumn(Modifier.navigationBarsPadding()) {
            itemsIndexed(segments) { index, segment ->
                val selected = index == currentIndex
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable {
                            onSeek(segment.startTimeSeconds * 1000L)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Thumbnail(
                        segment.previewUrl,
                        Modifier.size(width = 96.dp, height = 54.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Text(
                        segment.title.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Text(formatDuration(segment.startTimeSeconds.toLong()).ifEmpty { "0:00" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
