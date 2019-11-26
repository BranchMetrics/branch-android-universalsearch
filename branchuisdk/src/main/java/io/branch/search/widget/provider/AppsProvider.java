package io.branch.search.widget.provider;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.R;
import io.branch.search.widget.model.App;
import io.branch.search.widget.util.AppIconProvider;
import io.branch.search.widget.util.BranchEvents;
import io.branch.search.widget.util.WordMatcher;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 * <p>
 * This provider shows apps that match the text query, unlike
 * {@link SuggestedAppsProvider} which suggests apps when query is empty.
 */
@Keep
public class AppsProvider extends BaseAppsProvider<DiscoveryViewModel<App>> {

    @SuppressWarnings("unused")
    private static final String TAG = "Branch::Apps";

    private final WordMatcher mWordMatcher = new WordMatcher(
            new WordMatcher.NonLowerCaseSplitter()
    );

    private final AppIconProvider mIconProvider = new AppIconProvider();

    @NonNull
    @Override
    protected List<App> loadResults(@NonNull String query, int token, int capacity) {
        // Construct a list of candidates based on the search
        List<App> apps = getTopAppsList();
        List<App> list = new ArrayList<>();
        if (apps != null && apps.size() > 0) {
            for (App app : apps) {
                if (mWordMatcher.matches(app.getLabel(), query)) {
                    list.add(app);
                }
                if (app.getLabel().equalsIgnoreCase(query)) {
                    notifyExactMatch(query, token);
                }

                // Don't bother if we already have max count.
                if (list.size() == capacity) {
                    break;
                }
            }
        }
        return list;
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
        return getResources().getText(R.string.branch_suggested_apps_provider_title);
    }

    @Override
    protected int getAdapterCapacity(int columns) {
        return 2 * columns;
    }

    @Nullable
    @Override
    protected Object getAdapterItemPayload(@NonNull App item) {
        Context context = requireContext();
        return mIconProvider.provideAppIcon(context, item.getIcon(context));
    }
}