package io.branch.search.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.branch.search.widget.R;
import io.branch.search.widget.generator.AppsGenerator;
import io.branch.search.widget.model.App;
import io.branch.search.widget.ui.AppViewHolder;
import io.branch.search.widget.ui.DiscoveryViewHolder;

/**
 * Base class for providers of apps. Offers:
 * <p>
 * - a list of top apps, which is used by various providers. {@link #getTopAppsList()},
 * {@link #addTopApp(App)}
 * - implementation of {@link #launchResult(App, Object, int)} to launch an app
 */
public abstract class BaseAppsProvider<VM extends DiscoveryViewModel<App>>
        extends SimpleDiscoveryProvider<App, VM> {

    private final AppsGenerator mAppsGenerator = new AppsGenerator();

    /**
     * Gets a list of top apps from {@link AppsGenerator}.
     *
     * @return a list of top apps, or null if not available
     */
    @Nullable
    protected final List<App> getTopAppsList() {
        Context context = getContext();
        if (context == null) return null;
        return mAppsGenerator.get(context);
    }

    /**
     * Registers a new top app. Only the package name matters.
     *
     * @param appItem the app name
     */
    protected final void addTopApp(@NonNull App appItem) {
        Context context = getContext();
        if (context == null) return;
        mAppsGenerator.add(context, appItem);
    }

    @Override
    protected void launchResult(@NonNull App item, @Nullable Object payload, int position) {
        PackageManager packageManager = requireContext().getPackageManager();
        String packageName = item.getPackageName();
        if (!TextUtils.isEmpty(packageName)) {
            addTopApp(item);
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    @Override
    protected float getAdapterSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.branch_apps_spacing);
    }

    @Override
    protected DiscoveryViewHolder<App> createAdapterViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @NonNull DiscoveryViewHolder.Callback<App> callback) {
        return new AppViewHolder(inflater, parent, callback);
    }

    @Override
    protected void onSectionCreated(@NonNull DiscoverySection<App> section, @NonNull View view) {
        super.onSectionCreated(section, view);
        section.setPadding(
                getResources().getDimension(R.dimen.branch_apps_list_padding_horizontal),
                getResources().getDimension(R.dimen.branch_apps_list_padding_vertical));
        section.setAutoColumns(R.layout.branch_app);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAppsGenerator.release();
    }

    /**
     * Our content can change while the app was not in the resumed state.
     * Request a new discovery trigger to ensure we are updated.
     */
    @Override
    public void onResume() {
        super.onResume();
        callback.requestDiscovery(this, null);
    }
}