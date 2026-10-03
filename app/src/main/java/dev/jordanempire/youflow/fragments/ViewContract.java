package dev.jordanempire.youflow.fragments;

public interface ViewContract<I> {
    void showLoading();

    void hideLoading();

    void showEmptyState();

    void handleResult(I result);

    void handleError();
}
