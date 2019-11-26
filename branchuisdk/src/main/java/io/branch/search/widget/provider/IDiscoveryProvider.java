package io.branch.search.widget.provider;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;

import java.util.Set;

import io.branch.search.widget.BranchSearchController;

/**
 * This interface must be @Keep kept because it's used by ResourceProviderLoader to load all
 * implementors in the IDiscoveryProvider string array.
 */
@Keep
public interface IDiscoveryProvider {

    /**
     * Initialize the Discovery Provider
     *
     * @param context Context
     * @param callback Callbacks
     * @param payload Configuration payload
     * @return {@code true} if the provider was successfully initialized.
     */
    @UiThread
    boolean initialize(@NonNull Context context,
                       @NonNull IDiscoveryProviderCallback callback,
                       @Nullable Object payload);

    /**
     * @return an array of permissions that are needed.
     */
    @NonNull
    String[] getRequiredPermissions();

    /**
     * Called after permissions request results have been found.
     * Each permission will be either granted or not.
     * @param granted array
     *
     */
    @UiThread
    void onPermissionResults(boolean[] granted);

    /**
     * This method triggers the search on the provider with the query string.
     * @param query The query to search the provider for
     * @param token The query token
     * @param confirmed Whether this query was explicitly started by the user, for example by
     *                  clicking the keyboard search button, as opposed to queries that are launched
     */
    @UiThread
    void startDiscovery(@NonNull final String query, int token, boolean confirmed);

    /**
     * Called to activate or deactivate this provider. If deactivated, UI should be hidden
     * and provider should not get further query request. When active, UI should be restored.
     * @param active true to activate
     */
    void setActive(boolean active);

    /**
     * Depends on {@link #setActive(boolean)}.
     * @return true if active
     */
    boolean isActive();

    /**
     * Notifies this provider that one or more other providers have found an exact match for
     * the given query. This introduces a dependency between providers controlled by the same
     * {@link BranchSearchController}.
     *
     * This can be called:
     * - at any moment: before or after results are shown
     * - multiple times for the same query (e.g. if some providers is added)
     * - for the same provider that reported the exact match
     *  @param query the query
     * @param token
     * @param providers non-null, non-empty set of providers
     */
    @UiThread
    void onExactMatch(@NonNull final String query,
                      int token, @NonNull Set<IDiscoveryProvider> providers);

    @NonNull
    Fragment getFragment();
}
