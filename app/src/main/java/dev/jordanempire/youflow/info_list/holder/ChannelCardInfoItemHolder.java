package dev.jordanempire.youflow.info_list.holder;

import android.view.ViewGroup;

import androidx.annotation.Nullable;

import dev.jordanempire.youflow.R;
import dev.jordanempire.youflow.info_list.InfoItemBuilder;

public class ChannelCardInfoItemHolder extends ChannelMiniInfoItemHolder {
    public ChannelCardInfoItemHolder(final InfoItemBuilder infoItemBuilder,
                                     final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_channel_card_item, parent);
    }

    @Override
    protected int getDescriptionMaxLineCount(@Nullable final String content) {
        // Based on `list_channel_card_item` left side content (thumbnail 100dp
        // + additional details), Right side description can grow up to 8 lines.
        return 8;
    }
}
