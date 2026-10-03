package dev.jordanempire.youflow.media.engine

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * The queue lives in [PlaybackEngine] (streams are resolved lazily), so ExoPlayer itself only ever
 * holds one item. This wrapper lets the media session notification show working next/previous.
 */
internal class QueueAwarePlayer(player: Player, private val engine: PlaybackEngine) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands {
        val builder = super.getAvailableCommands().buildUpon()
        val s = engine.state.value
        if (s.hasNext) builder.addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        else builder.removeAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        builder.addAll(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        return builder.build()
    }

    override fun isCommandAvailable(command: Int) = availableCommands.contains(command)
    override fun hasNextMediaItem() = engine.state.value.hasNext
    override fun hasPreviousMediaItem() = true
    override fun seekToNext() { engine.next() }
    override fun seekToNextMediaItem() { engine.next() }
    override fun seekToPrevious() { engine.previous() }
    override fun seekToPreviousMediaItem() { engine.previous() }
}
