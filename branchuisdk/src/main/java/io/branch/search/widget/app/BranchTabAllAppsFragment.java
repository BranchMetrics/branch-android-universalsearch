package io.branch.search.widget.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.R;
import io.branch.search.widget.provider.AllAppsProvider;
import io.branch.search.widget.provider.IDiscoveryProvider;
import io.branch.search.widget.provider.IDiscoveryProviderCallback;
import io.branch.search.widget.util.BranchEvents;

/**
 * One of the pages inside a {@link BranchTabsFragment}. This shows a drawer with a list
 * of all apps currently installed.
 */
public class BranchTabAllAppsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.branchapp_tab_my_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // If and when we support state restoration inside the SDK, we can remove the onDestroyView
        // implementation here, and add this provider ONLY if savedInstanceState = null.
        // For now we add it and remove it each time.
        AllAppsProvider provider = new AllAppsProvider();
        provider.initialize(requireActivity(), new IDiscoveryProviderCallback() {
            @Override
            public void requestDiscovery(@NonNull IDiscoveryProvider provider,
                                         @Nullable String queryUpdate) {
                provider.startDiscovery("", 0, true);
            }
            public void onDiscoveryStarted(@NonNull IDiscoveryProvider provider,
                                           @NonNull String query, int token) { }
            public void onDiscoveryCompleted(@NonNull IDiscoveryProvider provider,
                                             @NonNull String query,
                                             int token, @Nullable List<Object> results,
                                             @Nullable Exception exception) { }
            public void onExactMatch(@NonNull IDiscoveryProvider provider, @NonNull String query,
                                     int token) {}
            public void onResultClick(@NonNull IDiscoveryProvider provider, @NonNull String query,
                                      int token, @NonNull Object result) { }
        }, null);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(view.getId(), provider, AllAppsProvider.class.getSimpleName());
        transaction.commitNow();
    }

    /**
     * Launch a open event when this becomes visible.
     * Note: this callback is used because we are stored inside a {@link ViewPager}.
     *
     * If we weren't, we could probably use {@link #onViewCreated(View, Bundle)}.
     *
     * @param isVisibleToUser true if visible
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getContext() != null) {
            new BranchEvent(BranchEvents.TYPE_OPEN)
                    .addCustomDataProperty(BranchEvents.Open.TARGET,
                            BranchEvents.Open.TARGET_APP_LIST)
                    .logEvent(requireContext());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FragmentManager manager = getChildFragmentManager();
        String tag = AllAppsProvider.class.getSimpleName();
        AllAppsProvider provider = (AllAppsProvider) manager.findFragmentByTag(tag);
        if (provider == null) {
            throw new IllegalStateException("AllAppsProvider fragment should not be null here.");
        }
        manager.beginTransaction()
                .remove(provider)
                .commitNowAllowingStateLoss();
    }
}
