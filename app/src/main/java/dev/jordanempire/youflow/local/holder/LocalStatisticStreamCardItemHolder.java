package dev.jordanempire.youflow.local.holder;

import android.view.ViewGroup;

import dev.jordanempire.youflow.R;
import dev.jordanempire.youflow.local.LocalItemBuilder;

public class LocalStatisticStreamCardItemHolder extends LocalStatisticStreamItemHolder {
    public LocalStatisticStreamCardItemHolder(final LocalItemBuilder infoItemBuilder,
                                              final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_stream_card_item, parent);
    }
}
