package dev.jordanempire.youflow.local.holder;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import dev.jordanempire.youflow.R;
import dev.jordanempire.youflow.database.LocalItem;
import dev.jordanempire.youflow.database.playlist.PlaylistMetadataEntry;
import dev.jordanempire.youflow.local.LocalItemBuilder;
import dev.jordanempire.youflow.local.history.HistoryRecordManager;

import java.time.format.DateTimeFormatter;

public class LocalBookmarkPlaylistItemHolder extends LocalPlaylistItemHolder {
    private final View itemHandleView;

    public LocalBookmarkPlaylistItemHolder(final LocalItemBuilder infoItemBuilder,
                                           final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_playlist_bookmark_item, parent);
    }

    LocalBookmarkPlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final int layoutId,
                                    final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
        itemHandleView = itemView.findViewById(R.id.itemHandle);
    }

    @Override
    public void updateFromItem(final LocalItem localItem,
                               final HistoryRecordManager historyRecordManager,
                               final DateTimeFormatter dateTimeFormatter) {
        if (!(localItem instanceof PlaylistMetadataEntry)) {
            return;
        }
        final PlaylistMetadataEntry item = (PlaylistMetadataEntry) localItem;

        itemHandleView.setOnTouchListener(getOnTouchListener(item));

        super.updateFromItem(localItem, historyRecordManager, dateTimeFormatter);
    }

    private View.OnTouchListener getOnTouchListener(final PlaylistMetadataEntry item) {
        return (view, motionEvent) -> {
            view.performClick();
            if (itemBuilder != null && itemBuilder.getOnItemSelectedListener() != null
                    && motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                itemBuilder.getOnItemSelectedListener().drag(item,
                        LocalBookmarkPlaylistItemHolder.this);
            }
            return false;
        };
    }
}
