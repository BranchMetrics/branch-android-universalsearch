package io.branch.search.widget.provider;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import io.branch.search.BranchSearch;
import io.branch.search.BranchSearchRequest;
import io.branch.search.widget.util.BranchLocationFinder;


/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 */
public abstract class BaseBranchProvider<T, VM extends DiscoveryViewModel<T>>
        extends DiscoveryProvider<T, VM> {

    private static final String[] REQUIRED_PERMISSIONS
            = {android.Manifest.permission.ACCESS_FINE_LOCATION};

    private String mBranchKey;

    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    // Super checks for hasPermissions() but for Branch providers it is not a strictly required
    // permission, we will work without it anyway.
    @Override
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        return !TextUtils.isEmpty(query);
    }

    /**
     * Returns the current branch key. Might be used by some provider that does
     * direct network requests instead of using the search SDK.
     * @return the key
     */
    @SuppressWarnings("unused")
    @Nullable
    protected String getBranchKey() {
        if (mBranchKey == null) {
            try {
                Context context = requireContext();
                final ApplicationInfo info = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                if (info.metaData != null) {
                    mBranchKey = info.metaData.getString("io.branch.sdk.BranchKey");
                }
            } catch (Exception ignore) { }
        }
        return mBranchKey;
    }

    @Override
    public boolean initialize(@NonNull Context context,
                              @NonNull IDiscoveryProviderCallback callback,
                              @Nullable Object payload) {
        if (super.initialize(context, callback, payload)) {
            // Initialize Branch SDK if needed. Make sure we don't do it twice.
            if (BranchSearch.getInstance() == null) {
                BranchSearch.init(context);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        BranchLocationFinder.initialize(requireActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        BranchLocationFinder.release();
    }

    @Override
    public void onPermissionResults(boolean[] granted) {
        BranchLocationFinder.getInstance().requestLocationUpdate(requireActivity());
        super.onPermissionResults(granted);
    }

    /**
     * Returns a {@link BranchSearchRequest} configured for the given query
     * and the last known location.
     * @param query query
     * @return request
     */
    @NonNull
    protected BranchSearchRequest createSearchRequest(@NonNull String query) {
        BranchSearchRequest request = BranchSearchRequest.Create(query);
        Location lastKnownLocation = BranchLocationFinder.getInstance().getLastKnownLocation();
        if (lastKnownLocation != null) {
            request.setLocation(lastKnownLocation);
        }
        // request.setLatitude(40.730610);
        // request.setLongitude(-73.935242);
        return request;
    }
}