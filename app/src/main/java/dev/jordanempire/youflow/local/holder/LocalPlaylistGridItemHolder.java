package dev.jordanempire.youflow.local.holder;

import android.view.ViewGroup;

import dev.jordanempire.youflow.R;
import dev.jordanempire.youflow.local.LocalItemBuilder;

public class LocalPlaylistGridItemHolder extends LocalPlaylistItemHolder {
    public LocalPlaylistGridItemHolder(final LocalItemBuilder infoItemBuilder,
                                       final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_playlist_grid_item, parent);
    }
}
