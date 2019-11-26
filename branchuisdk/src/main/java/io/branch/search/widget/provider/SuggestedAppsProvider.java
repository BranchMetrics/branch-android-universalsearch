package io.branch.search.widget.provider;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.List;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.model.App;
import io.branch.search.widget.util.AppIconProvider;
import io.branch.search.widget.util.BranchEvents;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 *
 * NOTE: Do not rename without changing the array.
 *
 * This provider shows suggested apps when the text query is empty.
 * When we have a query, {@link AppsProvider} will do the job.
 */
@Keep
@SuppressWarnings("unused")
public class SuggestedAppsProvider extends BaseAppsProvider<DiscoveryViewModel<App>> {
    private static final String TAG = "Branch::Apps";
    private final AppIconProvider mIconProvider = new AppIconProvider();

    /**
     * We only accept empty queries, so this is inverted.
     */
    @Override
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        return TextUtils.isEmpty(query);
    }

    @NonNull
    @Override
    protected List<App> loadResults(@NonNull String query, int token, int capacity) {
        List<App> results = getTopAppsList();
        if (results == null) throw new RuntimeException("No results.");
        return results;
    }

    @Override
    protected void launchResult(@NonNull App item, @Nullable Object payload, int position) {
        super.launchResult(item, payload, position);
        position++; // 1 based
        new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                        getClass().getSimpleName())
                .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                        String.valueOf(position))
                .logEvent(requireContext());
    }

    @Nullable
    @Override
    protected CharSequence getAdapterHeader() {
        return null;
    }

    @Override
    protected int getAdapterCapacity(int columns) {
        return columns; // = 1 row
    }

    @Nullable
    @Override
    protected Object getAdapterItemPayload(@NonNull App item) {
        Context context = requireContext();
        return mIconProvider.provideAppIcon(context, item.getIcon(context));
    }
}