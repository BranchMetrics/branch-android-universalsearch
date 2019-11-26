package io.branch.search.widget.provider;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.branch.search.widget.R;
import io.branch.search.widget.model.App;
import io.branch.search.widget.util.AppIconProvider;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 * <p>
 * This provider shows all apps, regardless of the query.
 */
@Keep
public class AllAppsProvider extends BaseAppsProvider<DiscoveryViewModel<App>> {

    @SuppressWarnings("unused")
    private static final String TAG = "Branch::AllApps";

    // Retrieving the drawable is very expensive. Not the AppIconProvider part, but the
    // PackageManager one. So it's better to cache the icons here since we have a lot to show,
    // potentially.
    private final Map<String, Drawable> mIconCache = new HashMap<>();
    private final AppIconProvider mIconProvider = new AppIconProvider();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.branch_provider_all_apps, container, false);
    }

    @Override
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        return true;
    }

    @NonNull
    @Override
    protected List<App> loadResults(@NonNull String query, int token, int capacity) {
        // Could query DeviceAppsManager.getInstalledApps(context) and then loadLabel() for
        // each app, but we already have a SQL list of all apps in our database. Just use that
        // one so we avoid the expensive loadLabel() call.
        List<App> list = getTopAppsList();
        if (list == null) throw new RuntimeException();
        // Cache the icons. Better here than in onBind(), as that would create
        // a delay for the first scroll.
        Context context = requireContext();
        for (App item : list) {
            if (!mIconCache.containsKey(item.getPackageName())) {
                Drawable icon = mIconProvider.provideAppIcon(context, item.getIcon(context));
                mIconCache.put(item.getPackageName(), icon);
            }
        }
        // Just reorder alphabetically.
        Collections.sort(list, new Comparator<App>() {
            @Override
            public int compare(App o1, App o2) {
                return o1.getLabel().toLowerCase()
                        .compareTo(o2.getLabel().toLowerCase());
            }
        });
        return list;
    }

    @Nullable
    @Override
    protected CharSequence getAdapterHeader() {
        return null;
    }

    @Override
    protected float getAdapterSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.branch_all_apps_vertical_margin);
    }

    @Nullable
    @Override
    protected Object getAdapterItemPayload(@NonNull App item) {
        return mIconCache.get(item.getPackageName());
    }
}