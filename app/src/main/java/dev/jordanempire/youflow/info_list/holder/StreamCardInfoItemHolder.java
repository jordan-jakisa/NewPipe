package dev.jordanempire.youflow.info_list.holder;

import android.view.ViewGroup;

import dev.jordanempire.youflow.R;
import dev.jordanempire.youflow.info_list.InfoItemBuilder;

/**
 * Card layout for stream.
 */
public class StreamCardInfoItemHolder extends StreamInfoItemHolder {

    public StreamCardInfoItemHolder(final InfoItemBuilder infoItemBuilder, final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_stream_card_item, parent);
    }
}
