package dev.jordanempire.youflow.info_list.holder;

import android.view.ViewGroup;

import dev.jordanempire.youflow.R;
import dev.jordanempire.youflow.info_list.InfoItemBuilder;

public class ChannelGridInfoItemHolder extends ChannelMiniInfoItemHolder {
    public ChannelGridInfoItemHolder(final InfoItemBuilder infoItemBuilder,
                                     final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_channel_grid_item, parent);
    }
}
