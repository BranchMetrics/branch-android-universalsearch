package io.branch.search.widget.provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Callback interface between {@link IDiscoveryProvider}s and their controller, typically
 * a {@link io.branch.search.widget.BranchSearchController} instance.
 */
public interface IDiscoveryProviderCallback {

    /**
     * Requests a new discovery trigger as soon as possible. Implementors
     * should call {@link IDiscoveryProvider#startDiscovery(String, int, boolean)}.
     * Optionally, a query update can be passed, in which case the provider is asking
     * to start discovery for that query.
     * @param provider provider
     * @param queryUpdate update
     */
    void requestDiscovery(@NonNull IDiscoveryProvider provider,
                          @Nullable String queryUpdate);

    /**
     * Notifies that the provider discovery process has started.
     * @param provider provider
     * @param query query
     * @param token token
     */
    void onDiscoveryStarted(@NonNull IDiscoveryProvider provider,
                            @NonNull String query,
                            int token);

    /**
     * Notifies that the provider discovery process has ended, with
     * either a list of results or an exception.
     * @param provider provider
     * @param query query
     * @param token token
     * @param results results
     * @param exception error
     */
    void onDiscoveryCompleted(@NonNull IDiscoveryProvider provider,
                              @NonNull String query,
                              int token,
                              @Nullable List<Object> results,
                              @Nullable Exception exception);

    /**
     * Notifies that one of this provider results is an 'exact match'.
     * The meaning of this can be provider specific but it is a signal
     * that this provider owns a very relevant result.
     * @param provider provider
     * @param query query
     * @param token token
     */
    void onExactMatch(@NonNull IDiscoveryProvider provider,
                      @NonNull String query,
                      int token);

    /**
     * Notifies that one of this provider results has been clicked.
     * @param provider provider
     * @param query query
     * @param token token
     * @param result result
     */
    void onResultClick(@NonNull IDiscoveryProvider provider,
                       @NonNull String query,
                       int token,
                       @NonNull Object result);
}
