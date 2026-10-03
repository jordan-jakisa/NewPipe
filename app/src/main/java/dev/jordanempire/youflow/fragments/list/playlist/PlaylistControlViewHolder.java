package dev.jordanempire.youflow.fragments.list.playlist;

import dev.jordanempire.youflow.player.playqueue.PlayQueue;

/**
 * Interface for {@code R.layout.playlist_control} view holders
 * to give access to the play queue.
 */
public interface PlaylistControlViewHolder {
    PlayQueue getPlayQueue();
}
