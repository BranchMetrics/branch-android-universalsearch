package io.branch.search.widget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.Selection;
import android.text.SpannableString;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.os.Handler;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.provider.IDiscoveryProvider;
import io.branch.search.widget.provider.IDiscoveryProviderCallback;
import io.branch.search.widget.util.BranchEvents;
import io.branch.search.widget.provider.loader.ProviderLoader;
import io.branch.search.widget.provider.loader.ResourceProviderLoader;
import io.branch.search.widget.query.BranchQueryMetadata;
import io.branch.search.widget.query.BranchQuerySource;

/**
 * The base controller for Branch Discovery search.
 */
@Keep
public class BranchSearchController {
    private static final String TAG = "Branch::Controller";
    private static final int ID_REQUEST_PERMISSIONS = 532;
    private static final int DEBOUNCE_TIME_MILLIS = 150;

    @NonNull private final BranchSearchResultsView mResultsView;
    @NonNull private final Context mContext;
    private boolean mLoading = false;

    // Fields related to the current query - could create a CurrentQuery object but
    // probably we don't want to create a new object for each letter.
    @NonNull private String mCurrentQuery = "";
    private int mCurrentQueryToken = 0;
    private boolean mCurrentQueryConfirmed = false;
    private BranchQuerySource mCurrentQuerySource;
    private final Set<IDiscoveryProvider> mCurrentQueryExactMatches
            = Collections.synchronizedSet(new HashSet<IDiscoveryProvider>());
    private final Map<IDiscoveryProvider, Integer> mCurrentQueryResults
            = Collections.synchronizedMap(new HashMap<IDiscoveryProvider, Integer>());

    private final ProviderLoader mProviders;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final IDiscoveryProviderCallback mProviderCallback = new ProviderCallback();
    private final List<BranchSearchCallback> mCallbacks = new ArrayList<>();
    private final DispatchSearchRunnable mDispatchSearchRunnable = new DispatchSearchRunnable();

    /**
     * Returns an initialized instance of {@link BranchSearchController} which you can use
     * to control the discovery search and push results to the given
     * {@link BranchSearchResultsView}.
     *
     * @param fragmentManager a fragmentManager
     * @param resultsView the results view
     * @return the controller
     */
    public static BranchSearchController init(@NonNull FragmentManager fragmentManager,
                                              @NonNull BranchSearchResultsView resultsView) {
        return init(fragmentManager, resultsView, null);
    }

    /**
     * Returns an initialized instance of {@link BranchSearchController} which you can use
     * to control the discovery search and push results to the given
     * {@link BranchSearchResultsView}.
     *
     * @param fragmentManager a fragmentManager
     * @param resultsView the results view
     * @param payloads the provider payloads
     * @return the controller
     */
    @SuppressWarnings("WeakerAccess")
    public static BranchSearchController init(
            @NonNull FragmentManager fragmentManager,
            @NonNull BranchSearchResultsView resultsView,
            @Nullable Map<Class<? extends IDiscoveryProvider>, Object> payloads) {
        return init(fragmentManager, resultsView, payloads, null);
    }

    /**
     * Returns an initialized instance of {@link BranchSearchController} which you can use
     * to control the discovery search and push results to the given
     * {@link BranchSearchResultsView}.
     *
     * @param fragmentManager a fragmentManager
     * @param resultsView the results view
     * @param payloads the provider payloads
     * @return the controller
     */
    @SuppressWarnings("WeakerAccess")
    public static BranchSearchController init(
            @NonNull FragmentManager fragmentManager,
            @NonNull BranchSearchResultsView resultsView,
            @Nullable Map<Class<? extends IDiscoveryProvider>, Object> payloads,
            @Nullable ProviderLoader loader) {
        Context context = resultsView.getContext();
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(context);
        }

        BranchSearchController controller
                = new BranchSearchController(resultsView, loader);

        // Initialize each of the providers.
        // If present, pass in the provider specific payload.
        for (IDiscoveryProvider provider : controller.mProviders) {
            Object payload = null;
            if (payloads != null && payloads.containsKey(provider.getClass())) {
                payload = payloads.get(provider.getClass());
            }
            provider.initialize(context, controller.mProviderCallback, payload);
        }

        // Add them to fragment manager
        resultsView.addProviders(fragmentManager, controller.mProviders);
        return controller;
    }

    /**
     * Creates a new controller. For an initialized instance,
     * please use {@link #init(FragmentManager, BranchSearchResultsView)}.
     */
    @SuppressLint("ClickableViewAccessibility")
    private BranchSearchController(@NonNull BranchSearchResultsView resultsView,
                                   @Nullable ProviderLoader loader) {
        mContext = resultsView.getContext();
        mResultsView = resultsView;
        mProviders = loader != null ? loader : new ResourceProviderLoader(mContext);

        setCurrentQuery("", null);
    }

    /**
     * Reverts initialization made in
     * {@link #init(FragmentManager, BranchSearchResultsView)}, clearing up both the
     * view hierarchy and the fragment manager.
     *
     */
    public void tearDown() {
        mHandler.removeCallbacks(mDispatchSearchRunnable);
        mResultsView.clearProviders(mProviders);
    }

    /**
     * Adds a {@link BranchSearchCallback} to listen for branch search events.
     */
    public void addCallback(@Nullable BranchSearchCallback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    /**
     * Removes a previously added {@link BranchSearchCallback}.
     */
    @SuppressWarnings("unused")
    public void removeCallback(@Nullable BranchSearchCallback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * Returns the provider for the given provider class.
     * If none is found, this throws an exception.
     *
     * @param providerClass class of desired provider
     * @param <T> type of desired provider
     * @return provider
     */
    @NonNull
    public <T extends IDiscoveryProvider> T getProvider(
            @NonNull Class<T> providerClass) {
        for (IDiscoveryProvider candidate : mProviders) {
            if (providerClass.isAssignableFrom(candidate.getClass())) {
                //noinspection unchecked
                return (T) candidate;
            }
        }
        throw new RuntimeException("Provider not found: " + providerClass.getSimpleName());
    }

    /**
     * Checks for runtime permissions to be requested. These will be requested
     * by the given {@code activity} which should pass the results to
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     *
     * @param activity an activity
     */
    public void checkPermissions(@NonNull Activity activity) {
        List<String> permissionList = collectPermissions();
        if (permissionList.size() > 0) {
            String[] permissionArray = new String[permissionList.size()];
            permissionArray = permissionList.toArray(permissionArray);
            ActivityCompat.requestPermissions(activity, permissionArray, ID_REQUEST_PERMISSIONS);
        }
    }

    /**
     * Checks for runtime permissions to be requested. These will be requested
     * by the given {@code fragment} which should pass the results to
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     *
     * @param fragment a fragment
     */
    @SuppressWarnings("unused")
    public void checkPermissions(@NonNull Fragment fragment) {
        List<String> permissionList = collectPermissions();
        if (permissionList.size() > 0) {
            String[] permissionArray = new String[permissionList.size()];
            permissionArray = permissionList.toArray(permissionArray);
            fragment.requestPermissions(permissionArray, ID_REQUEST_PERMISSIONS);
        }
    }

    @NonNull
    private List<String> collectPermissions() {
        Set<String> permissionSet = new HashSet<>();
        for (IDiscoveryProvider provider : mProviders) {
            String[] permissions = provider.getRequiredPermissions();
            Collections.addAll(permissionSet, permissions);
        }
        return new ArrayList<>(permissionSet);
    }

    /**
     * Should be called from host activity
     * {@link Activity#onRequestPermissionsResult(int, String[], int[])}
     * to notify this controller of the permission request results.
     *
     * @param requestCode code
     * @param permissions permissions
     * @param grantResults results
     */
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @SuppressWarnings("unused") @NonNull int[] grantResults) {
        if (requestCode == ID_REQUEST_PERMISSIONS && permissions.length > 0) {
            for (IDiscoveryProvider provider : mProviders) {
                String[] providerPermissions = provider.getRequiredPermissions();
                int length = providerPermissions.length;
                if (length == 0) continue;
                boolean[] granted = new boolean[length];
                for (int i = 0; i < length; i++) {
                    granted[i] = ContextCompat.checkSelfPermission(mContext,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
                }
                provider.onPermissionResults(granted);
            }
        }
    }

    /**
     * Updates the current query.
     * @param query the new query
     * @param source the query source
     */
    private void setCurrentQuery(@NonNull String query, @Nullable BranchQuerySource source) {
        mCurrentQuery = query;
        mCurrentQueryToken++;
        mCurrentQueryExactMatches.clear();
        mCurrentQueryResults.clear();
        mCurrentQuerySource = source;
    }

    /**
     * Executes the search logic.
     */
    private void dispatchSearch(long delay, boolean confirmed) {
        mHandler.removeCallbacks(mDispatchSearchRunnable);
        if (delay == 0) {
            String sourceValue = BranchEvents.SearchExecuted.SOURCE_KEYBOARD;
            if (mCurrentQuerySource == BranchQuerySource.VOICE) {
                sourceValue = BranchEvents.SearchExecuted.SOURCE_VOICE;
            } else if (mCurrentQuerySource == BranchQuerySource.AUTO_COMPLETE) {
                sourceValue = BranchEvents.SearchExecuted.SOURCE_AUTOSUGGEST;
            }
            new BranchEvent(BranchEvents.TYPE_SEARCH_EXECUTED)
                    .addCustomDataProperty(BranchEvents.SearchExecuted.QUERY, mCurrentQuery)
                    .addCustomDataProperty(BranchEvents.SearchExecuted.SOURCE, sourceValue)
                    .logEvent(mContext);
            mCurrentQueryConfirmed = confirmed;
            for (IDiscoveryProvider provider : mProviders) {
                if (provider.isActive()) {
                    provider.startDiscovery(mCurrentQuery,
                            mCurrentQueryToken,
                            mCurrentQueryConfirmed);
                }
            }
        } else {
            mDispatchSearchRunnable.isConfirmed = confirmed;
            mHandler.postDelayed(mDispatchSearchRunnable, delay);
        }
    }

    /**
     * Triggers {@link #dispatchSearch(long, boolean)} without delay.
     */
    private class DispatchSearchRunnable implements Runnable {
        private boolean isConfirmed;
        @Override
        public void run() {
            dispatchSearch(0, isConfirmed);
        }
    }

    /**
     * This should be called from the
     * {@link android.text.TextWatcher#onTextChanged(CharSequence, int, int, int)}
     * callback of your search EditText.
     * @param query the new query
     */
    public void onTextChanged(@NonNull CharSequence query) {
        BranchQuerySource source = BranchQueryMetadata.get(query, BranchQueryMetadata.SOURCE);
        setCurrentQuery(query.toString()/*.trim()*/.toLowerCase(), source);
        dispatchSearch(DEBOUNCE_TIME_MILLIS, false);
    }

    /**
     * This should be called from the
     * {@link TextView.OnEditorActionListener#onEditorAction(TextView, int, KeyEvent)}
     * callback of your search EditText.
     */
    @SuppressWarnings("unused")
    public boolean onEditorAction(@NonNull TextView view,
                                  int actionId,
                                  @NonNull KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
            mResultsView.hideKeyboard();
            // Even if query is the same, we want to call setCurrentQuery to cleanup other data.
            setCurrentQuery(mCurrentQuery, BranchQuerySource.KEYBOARD);
            dispatchSearch(0, true);
            return true;
        }
        return false;
    }

    /**
     * Offers fine grained control to activate or deactivate a single provider,
     * identified by the class name.
     * In deactivated state, provider is not showing up nor doing any work.
     *
     * @param provider the provider
     * @param active whether to activate or deactivate
     */
    public void setActive(@NonNull Class<? extends IDiscoveryProvider> provider,
                          boolean active) {
        for (IDiscoveryProvider candidate : mProviders) {
            if (provider.isAssignableFrom(candidate.getClass())) {
                if (candidate.isActive() != active) {
                    candidate.setActive(active);
                    // If this provider was deactivated and now activated again, pass the last
                    // available query so it can restore its state flawlessly.
                    if (active) {
                        candidate.startDiscovery(mCurrentQuery,
                                mCurrentQueryToken,
                                mCurrentQueryConfirmed);
                    }
                }
            }
        }
    }

    /**
     * Returns the number of currently active providers.
     * @return count
     */
    private int getActiveProvidersCount() {
        int count = 0;
        for (IDiscoveryProvider provider : mProviders) {
            if (provider.isActive()) count++;
        }
        return count;
    }

    private void setLoading(boolean loading) {
        if (loading != mLoading) {
            mLoading = loading;
            for (BranchSearchCallback callback : mCallbacks) {
                callback.onLoadingStateChanged(loading);
            }
        }
    }

    /**
     * Listener for all providers.
     */
    private class ProviderCallback implements IDiscoveryProviderCallback {

        @Override
        public void requestDiscovery(@NonNull IDiscoveryProvider provider,
                                     @Nullable String queryUpdate) {
            logEvent(provider, null, "requestDiscovery. newQuery:'"
                    + queryUpdate + "'.");
            if (queryUpdate == null || queryUpdate.equals(mCurrentQuery)) {
                // Give this provider a new discovery call, as long as it is active.
                if (provider.isActive()) {
                    provider.startDiscovery(mCurrentQuery,
                            mCurrentQueryToken,
                            mCurrentQueryConfirmed);
                }
            } else {
                // A new query was requested. We want to pass this to all providers,
                // but we also want the search box to be updated. For this we must ask
                // our callbacks. We also set the selection to the end of the string.
                mResultsView.hideKeyboard();
                SpannableString newQuery = new SpannableString(queryUpdate);
                Selection.setSelection(newQuery, newQuery.length());
                for (BranchSearchCallback callback : mCallbacks) {
                    callback.onQueryUpdateRequested(queryUpdate);
                }
            }
        }

        @Override
        public void onDiscoveryStarted(@NonNull IDiscoveryProvider provider,
                                       @NonNull String query, int token) {
            logEvent(provider, query, "onDiscoveryStarted.");
            setLoading(true);
        }

        @Override
        public void onDiscoveryCompleted(@NonNull IDiscoveryProvider provider,
                                         @NonNull String query,
                                         int token,
                                         @Nullable List<Object> results,
                                         @Nullable Exception error) {
            int count = results == null ? -1 : results.size();
            logEvent(provider, query, "onDiscoveryResult. results:" +
                    (results != null) + ", count:" + count + ".");
            if (token == mCurrentQueryToken) {
                mCurrentQueryResults.put(provider, count);
                if (mCurrentQueryResults.entrySet().size() == getActiveProvidersCount()) {
                    setLoading(false);
                    StringBuilder providersList = new StringBuilder();
                    StringBuilder resultCountList = new StringBuilder();
                    String suffix = "";
                    synchronized (mCurrentQueryResults) {
                        for (IDiscoveryProvider p : mCurrentQueryResults.keySet()) {
                            providersList.append(suffix).append(p.getClass().getSimpleName());
                            resultCountList.append(suffix).append(mCurrentQueryResults.get(p));
                            suffix = ","; // from 2nd onwards.
                        }
                    }
                    new BranchEvent(BranchEvents.TYPE_SEARCH_RESULTS)
                            .addCustomDataProperty(BranchEvents.SearchResults.QUERY, query)
                            .addCustomDataProperty(BranchEvents.SearchResults.PROVIDERS,
                                    providersList.toString())
                            .addCustomDataProperty(BranchEvents.SearchResults.RESULTS,
                                    resultCountList.toString())
                            .logEvent(mContext);
                }
            }

            if (results != null) {
                for (BranchSearchCallback callback : mCallbacks) {
                    callback.onProviderResults(provider, query, results);
                }
            } else if (error != null) {
                for (BranchSearchCallback callback : mCallbacks) {
                    callback.onProviderError(provider, query, error);
                }
            } else {
                throw new IllegalArgumentException("Either results or error must be non-null");
            }
        }

        @Override
        public void onExactMatch(@NonNull IDiscoveryProvider provider,
                                 final @NonNull String query,
                                 final int token) {
            logEvent(provider, query, "onExactMatch.");
            if (token == mCurrentQueryToken) {
                mCurrentQueryExactMatches.add(provider);
                final Set<IDiscoveryProvider> copy
                        = new HashSet<>(mCurrentQueryExactMatches);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (IDiscoveryProvider other : mProviders) {
                            other.onExactMatch(query, token, copy);
                        }
                    }
                });
            }
        }

        @Override
        public void onResultClick(@NonNull IDiscoveryProvider provider,
                                  @NonNull String query,
                                  int token, @NonNull Object result) {
            logEvent(provider, query, "onResultClick.");
            for (BranchSearchCallback callback : mCallbacks) {
                callback.onProviderResultClicked(provider, query, result);
            }
        }
    }

    private static void logEvent(@NonNull IDiscoveryProvider provider,
                                 @Nullable String query,
                                 @NonNull String message) {
        if (query == null) {
            Log.d(TAG, provider.getClass().getSimpleName() + ": " + message);
        } else {
            Log.d(TAG, provider.getClass().getSimpleName()
                    + " (query:'" + query + "'): " + message);
        }
    }
}
