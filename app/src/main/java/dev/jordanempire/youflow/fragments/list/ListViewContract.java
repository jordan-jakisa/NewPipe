package dev.jordanempire.youflow.fragments.list;

import dev.jordanempire.youflow.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
