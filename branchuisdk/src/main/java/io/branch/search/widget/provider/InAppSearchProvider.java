package io.branch.search.widget.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.branch.referral.util.BranchEvent;
import io.branch.search.BranchAppResult;
import io.branch.search.BranchLinkResult;
import io.branch.search.BranchSearch;
import io.branch.search.BranchSearchError;
import io.branch.search.BranchSearchRequest;
import io.branch.search.BranchSearchResult;
import io.branch.search.IBranchSearchEvents;
import io.branch.search.widget.model.App;
import io.branch.search.widget.model.InAppFilter;
import io.branch.search.widget.model.InAppInstallAd;
import io.branch.search.widget.ui.DiscoveryViewHolder;
import io.branch.search.widget.ui.InAppAppViewHolder;
import io.branch.search.widget.ui.InAppInstallAdViewHolder;
import io.branch.search.widget.ui.InAppLinkViewHolder;
import io.branch.search.widget.ui.InAppFilterViewHolder;
import io.branch.search.widget.util.BranchEvents;
import io.branch.search.widget.generator.AppsGenerator;
import io.branch.search.widget.util.DeviceAppsManager;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 *
 * NOTE: Do not rename without changing the array.
 */
@Keep
@SuppressWarnings("unused")
public class InAppSearchProvider extends BaseBranchProvider<Object,
        DiscoveryViewModel<Object>> {
    private static final String TAG = "Branch::Provider";
    private static final int MIN_CHARS = 3;

    private static final int SECTION_TYPE_APP = 0;
    private static final int SECTION_TYPE_FILTERS = 1;
    private static final int SECTION_TYPE_INSTALL_AD = 2;

    @SuppressWarnings("WeakerAccess")
    @Keep
    public static class Options {
        public int maxAppResults = 10;
        public int maxContentPerAppResults = 5;
        public boolean filtersEnabled = true;

        @VisibleForTesting
        Location location = null;
    }

    private boolean mShowNonInstalledAppResults = true;
    private Set<String> mInstalledApps;
    private Options mOptions = new Options();
    private BranchAppResult mFilter = null;

    @Override
    public boolean initialize(@NonNull Context context,
                              @NonNull IDiscoveryProviderCallback callback,
                              @Nullable Object payload) {
        if (payload != null) {
            mOptions = (Options) payload;
        }
        return super.initialize(context, callback, payload);
    }

    @Override
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        return super.isQueryValid(query, token, confirmed) && query.length() >= MIN_CHARS;
    }

    /**
     * Sets whether Branch should show results from apps that are not currently
     * installed on this device. Defaults to true.
     *
     * @param show true to show them
     */
    public void setShowNonInstalledAppResults(boolean show) {
        mShowNonInstalledAppResults = show;
    }

    private boolean shouldShowAppResult(@NonNull BranchAppResult result) {
        if (mShowNonInstalledAppResults) {
            return true;
        } else {
            return isAppInstalled(result);
        }
    }

    private boolean isAppInstalled(@NonNull BranchAppResult result) {
        if (mInstalledApps == null || DeviceAppsManager.isDirty(requireContext())) {
            List<ResolveInfo> apps = DeviceAppsManager.getInstalledApps(requireContext());
            mInstalledApps = new HashSet<>();
            for (ResolveInfo app : apps) {
                mInstalledApps.add(app.activityInfo.applicationInfo.packageName);
            }
        }
        return mInstalledApps.contains(result.getPackageName());
    }

    @Override
    protected boolean showsLoadingIndicator() {
        return false;
    }

    /**
     * We want to perform this synchronously, so we use Tasks.await()
     * since we are already running in a background thread.
     */
    @NonNull
    @Override
    protected List<Object> loadResults(@NonNull final String query, int token, int capacity) {
        final TaskCompletionSource<List<Object>> source = new TaskCompletionSource<>();
        BranchSearchRequest request = createSearchRequest(query);
        request.setMaxAppResults(mOptions.maxAppResults);
        request.setMaxContentPerAppResults(mOptions.maxContentPerAppResults);
        if (mOptions.location != null) {
            request.setLocation(mOptions.location);
        }
        BranchSearch.getInstance().query(request, new IBranchSearchEvents() {
            @Override
            @SuppressWarnings("unchecked")
            public void onBranchSearchResult(BranchSearchResult branchSearchResult) {
                List<BranchAppResult> results = branchSearchResult.getResults();
                List<BranchAppResult> filteredResults = new ArrayList<>();
                for (BranchAppResult result : results) {
                    if (shouldShowAppResult(result)) {
                        filteredResults.add(result);
                    }
                }
                source.trySetResult((List) filteredResults);
            }

            @Override
            public void onBranchSearchError(BranchSearchError error) {
                Log.d(TAG, "Error with Branch Search. " + error.getErrorMsg());
                source.trySetException(new RuntimeException(error.toString()));
            }
        });

        try {
            return Tasks.await(source.getTask());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void launchResult(@NonNull Object item, @Nullable Object payload, int position) {
        Context context = requireContext();
        if (item instanceof BranchAppResult) {
            BranchAppResult appResult = (BranchAppResult) item;
            String appPackage = appResult.getPackageName();
            // If installed, add the app to top-apps
            if (DeviceAppsManager.isPackageInstalled(context, appPackage)) {
                try {
                    ApplicationInfo appInfo = context
                            .getPackageManager()
                            .getApplicationInfo(appPackage, 0);
                    String appLabel = appInfo.loadLabel(context.getPackageManager()).toString();
                    App appItem = new App(appResult.getPackageName(), appLabel);
                    AppsGenerator generator = new AppsGenerator();
                    generator.add(context, appItem);
                    generator.release();
                } catch (PackageManager.NameNotFoundException ignore) {}
            }
            // Launch
            appResult.openSearchDeepLink(context, true);
        } else if (item instanceof BranchLinkResult) {
            BranchLinkResult linkResult = (BranchLinkResult) item;
            linkResult.openContent(context, true);

            position++; // 1 based
            new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                    .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                            getClass().getSimpleName())
                    .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                            String.valueOf(position))
                    .addCustomDataProperty(BranchEvents.ResultClick.EXTRA,
                            linkResult.getWebLink())
                    .logEvent(requireContext());
        } else if (item instanceof InAppInstallAd) {
            // This method method will open the app if present (it shouldn't be),
            // or direct the user to the play store to install the app.
            InAppInstallAd installAd = (InAppInstallAd) item;
            installAd.linkResult.openContent(context, true);
        }
    }

    /**
     * Invalidate our sections anytime there are new results, since our sections depend
     * on the result that we have been loading.
     */
    @Override
    protected void applyResults(@NonNull String query, @NonNull List<Object> results) {
        invalidateSections(false);
        super.applyResults(query, results);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    protected List<Integer> computeSections(
            @Nullable List<Object> latestResults) {
        if (latestResults == null) return Collections.emptyList();
        if (latestResults.isEmpty()) return Collections.emptyList();
        // All results should be BranchAppResults.
        // Need one section for each app + 1 for filters.
        List<BranchAppResult> apps = (List<BranchAppResult>) (List) latestResults;
        List<BranchAppResult> nonAds = new ArrayList<>();
        List<Integer> sections = new ArrayList<>();
        for (BranchAppResult app : apps) {
            if (InAppInstallAd.isInstallAd(app)) {
                sections.add(SECTION_TYPE_INSTALL_AD);
            } else {
                nonAds.add(app);
            }
        }
        // Show the app filters if we have at least 2 apps.
        // Remove the current filter if the app is not present
        // in the new results.
        boolean showsFilters = mOptions.filtersEnabled && nonAds.size() > 1;
        if (mFilter != null && !nonAds.contains(mFilter)) {
            mFilter = null;
        }
        if (showsFilters) {
            sections.add(0, SECTION_TYPE_FILTERS);
        }
        // Now add the apps based on the filter presence.
        for (BranchAppResult app : nonAds) {
            if (mFilter == null || mFilter.equals(app)) {
                sections.add(SECTION_TYPE_APP);
            }
        }
        return sections;
    }

    @Override
    protected void onCreateSection(@NonNull DiscoverySection.Builder<Object> builder) {
        switch (builder.type) {
            case SECTION_TYPE_APP:
                builder.adapter = new AppAdapter();
                break;
            case SECTION_TYPE_FILTERS:
                builder.adapter = new FiltersAdapter();
                builder.orientation = RecyclerView.HORIZONTAL;
                break;
            case SECTION_TYPE_INSTALL_AD:
                builder.adapter = new AdAdapter();
                break;
            default:
                throw new RuntimeException("Unexpected type: " + builder.type);
        }
    }

    private class AdAdapter extends DiscoveryAdapter<Object> {
        private AdAdapter() {
            super(requireContext(),
                    InAppSearchProvider.this,
                    null);
        }

        @NonNull
        @Override
        public List<Object> setItems(@NonNull List<Object> items) {
            Object app = items.remove(0);
            if (!InAppInstallAd.isInstallAd((BranchAppResult) app)) {
                throw new RuntimeException("Unexpected app - not an ad!");
            }
            super.setItems(Collections.singletonList(app));
            return items;
        }

        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        @NonNull
        @Override
        protected DiscoveryViewHolder<Object> onCreateItemViewHolder(
                @NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int viewType) {
            DiscoveryViewHolder.Callback callback = this;
            DiscoveryViewHolder holder = new InAppInstallAdViewHolder(inflater, parent, callback);
            return holder;
        }

        @Override
        protected void onBindItem(@NonNull DiscoveryViewHolder<Object> viewHolder,
                                  @NonNull Object item) {
            viewHolder.bind(item, viewModel.getCurrentQuery(), null);
        }
    }

    private class FiltersAdapter extends DiscoveryAdapter<Object> {
        private FiltersAdapter() {
            super(requireContext(),
                    InAppSearchProvider.this,
                    null);
        }

        @NonNull
        @Override
        public List<Object> setItems(@NonNull List<Object> items) {
            // Do not steal any result. Just compute the filters,
            // starting with the 'no filter'/'see all' option.
            List<Object> results = new ArrayList<>();
            results.add(new InAppFilter(null));
            for (Object app : items) {
                if (!InAppInstallAd.isInstallAd((BranchAppResult) app)) {
                    results.add(new InAppFilter((BranchAppResult) app));
                }
            }
            super.setItems(results);
            return items;
        }

        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        @NonNull
        @Override
        protected DiscoveryViewHolder<Object> onCreateItemViewHolder(
                @NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int viewType) {
            DiscoveryViewHolder holder = new InAppFilterViewHolder(inflater, parent,
                    new DiscoveryViewHolder.Callback<InAppFilter>() {
                @Override
                public void onClick(@NonNull InAppFilter item,
                                    @Nullable Object payload,
                                    int position) {
                    mFilter = item.filter;
                    invalidateSections(true);
                }
            });
            return holder;
        }

        @Override
        protected void onBindItem(@NonNull DiscoveryViewHolder<Object> viewHolder,
                                  @NonNull Object item) {
            viewHolder.bind(item, viewModel.getCurrentQuery(), mFilter);
        }
    }

    private class AppAdapter extends DiscoveryAdapter<Object> {
        private static final int VIEW_TYPE_APP = 1;
        private static final int VIEW_TYPE_LINK = 2;

        private AppAdapter() {
            super(requireContext(),
                    InAppSearchProvider.this,
                    null);
        }

        @NonNull
        @Override
        public List<Object> setItems(@NonNull List<Object> items) {
            BranchAppResult app;
            if (mFilter == null) {
                app = (BranchAppResult) items.remove(0);
            } else {
                int index = items.indexOf(mFilter);
                if (index < 0) throw new IllegalStateException("Filter not found: " + mFilter);
                app = (BranchAppResult) items.remove(index);
            }
            List<Object> results = new ArrayList<>();
            results.add(app);
            results.addAll(app.getDeepLinks());
            super.setItems(results);
            return items;
        }

        @Override
        protected int onGetItemViewType(int adapterPosition, Object item) {
            if (item instanceof BranchLinkResult) {
                return VIEW_TYPE_LINK;
            } else if (item instanceof BranchAppResult) {
                return VIEW_TYPE_APP;
            } else {
                throw new RuntimeException("Unknown item. " + item);
            }
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        protected DiscoveryViewHolder<Object> onCreateItemViewHolder(
                @NonNull LayoutInflater inflater,
                @NonNull ViewGroup parent,
                int viewType) {
            DiscoveryViewHolder.Callback callback = this;
            DiscoveryViewHolder holder;
            switch (viewType) {
                case VIEW_TYPE_APP:
                    holder = new InAppAppViewHolder(inflater, parent, callback); break;
                case VIEW_TYPE_LINK:
                    holder = new InAppLinkViewHolder(inflater, parent, callback); break;
                default:
                    throw new RuntimeException("Unknown viewType. " + viewType);
            }
            return (DiscoveryViewHolder<Object>) holder;
        }

        @Override
        protected void onBindItem(@NonNull DiscoveryViewHolder<Object> viewHolder,
                                  @NonNull Object item) {
            int type = viewHolder.getItemViewType();
            String query = viewModel.getCurrentQuery();
            Object payload = null;
            if (type == VIEW_TYPE_APP) {
                payload = isAppInstalled((BranchAppResult) item);
            }
            viewHolder.bind(item, viewModel.getCurrentQuery(), payload);
        }
    }
}