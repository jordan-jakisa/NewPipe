package dev.jordanempire.youflow.ui.data

import android.content.Context
import dev.jordanempire.youflow.ui.model.VideoItem
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Remembers the last list shown for a screen so the next launch can paint it immediately while the
 * fresh one loads. Stored in the cache directory, so the system may clear it.
 */
object ListCache {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(VideoItem.serializer())

    private fun file(context: Context, key: String) = File(File(context.cacheDir, "lists").apply { mkdirs() }, key.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    fun read(context: Context, key: String): List<VideoItem>? = runCatching {
        json.decodeFromString(serializer, file(context, key).readText())
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    fun write(context: Context, key: String, items: List<VideoItem>) {
        runCatching { file(context, key).writeText(json.encodeToString(serializer, items)) }
    }
}
