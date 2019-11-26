package io.branch.search.widget;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.util.List;

import io.branch.search.widget.provider.IDiscoveryProvider;

/**
 * Public callback for {@link BranchSearchController} events.
 * Some of these methods are mandatory while others can be implemented only if needed.
 */
@Keep
public abstract class BranchSearchCallback {

    /**
     * Notifies that one of the provider requested the current query to be updated to the
     * new value in {@code newQuery}. This value should be applied to the search box text
     * so that it is passed back to the controller.
     *
     * @param newQuery new query
     */
    public abstract void onQueryUpdateRequested(@NonNull CharSequence newQuery);

    /**
     * Notifies that one of the provider has found results for the given query.
     *
     * @param provider provider
     * @param query query
     * @param results results
     */
    public void onProviderResults(@NonNull IDiscoveryProvider provider,
                                  @NonNull String query,
                                  @NonNull List<Object> results) {
        // Default to no-op.
    }

    /**
     * Notifies that one of the provider has encountered an error while looking
     * for results for the given query.
     *
     * @param provider provider
     * @param query query
     * @param error error
     */
    public void onProviderError(@NonNull IDiscoveryProvider provider,
                                @NonNull String query,
                                @NonNull Exception error) {
        // Default to no-op.
    }

    /**
     * Notifies that one of the provider results has been clicked and action is about to
     * be taken to open that item.
     *
     * @param provider provider
     * @param query query
     * @param result result
     */
    public void onProviderResultClicked(@NonNull IDiscoveryProvider provider,
                                        @NonNull String query,
                                        @NonNull Object result) {
        // Default to no-op.
    }

    /**
     * Notifies that the loading state of the whole search controller has changed.
     * The loading state will be true if any of the providers is loading - false if all of
     * them have completed their query.
     *
     * @param loading loading state
     */
    public void onLoadingStateChanged(boolean loading) {
        // Default to no-op.
    }
}
