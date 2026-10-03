package dev.jordanempire.youflow.local.holder;

import android.view.View;
import android.view.ViewGroup;

import dev.jordanempire.youflow.database.LocalItem;
import dev.jordanempire.youflow.database.playlist.PlaylistDuplicatesEntry;
import dev.jordanempire.youflow.database.playlist.PlaylistMetadataEntry;
import dev.jordanempire.youflow.local.LocalItemBuilder;
import dev.jordanempire.youflow.local.history.HistoryRecordManager;
import dev.jordanempire.youflow.util.Localization;
import dev.jordanempire.youflow.util.image.CoilHelper;

import java.time.format.DateTimeFormatter;

public class LocalPlaylistItemHolder extends PlaylistItemHolder {

    private static final float GRAYED_OUT_ALPHA = 0.6f;

    public LocalPlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final ViewGroup parent) {
        super(infoItemBuilder, parent);
    }

    LocalPlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final int layoutId,
                            final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem,
                               final HistoryRecordManager historyRecordManager,
                               final DateTimeFormatter dateTimeFormatter) {
        if (!(localItem instanceof PlaylistMetadataEntry item)) {
            return;
        }

        itemTitleView.setText(item.getOrderingName());
        itemStreamCountView.setText(Localization.localizeStreamCountMini(
                itemStreamCountView.getContext(), item.getStreamCount()));
        itemUploaderView.setVisibility(View.INVISIBLE);

        CoilHelper.INSTANCE.loadPlaylistThumbnail(itemThumbnailView, item.getThumbnailUrl());

        if (item instanceof PlaylistDuplicatesEntry
                && ((PlaylistDuplicatesEntry) item).getTimesStreamIsContained() > 0) {
            itemView.setAlpha(GRAYED_OUT_ALPHA);
        } else {
            itemView.setAlpha(1.0f);
        }

        super.updateFromItem(localItem, historyRecordManager, dateTimeFormatter);
    }
}
