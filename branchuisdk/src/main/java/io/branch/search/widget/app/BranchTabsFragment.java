package io.branch.search.widget.app;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.branch.search.widget.R;

/**
 * The main fragment inside a {@link BranchSearchFragment}, having two tabs.
 */
public class BranchTabsFragment extends Fragment {

    private static final int POSITION_SEARCH = 0;
    private static final int POSITION_ALL_APPS = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.branchapp_tabs_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Adapter adapter = new Adapter(getChildFragmentManager());
        TabLayout tabs = view.findViewById(R.id.branchapp_tab_layout);
        ViewPager viewPager = view.findViewById(R.id.branchapp_view_pager);
        viewPager.setAdapter(adapter);
        tabs.setupWithViewPager(viewPager);
        // TabLayout was now populated. Make sure we add the icons.
        TabLayout.Tab tabSearch = tabs.getTabAt(POSITION_SEARCH);
        TabLayout.Tab tabApps = tabs.getTabAt(POSITION_ALL_APPS);
        if (tabSearch != null) tabSearch.setIcon(R.drawable.branch_ic_search_24dp);
        if (tabApps != null) tabApps.setIcon(R.drawable.branch_ic_apps_24dp);
    }

    /**
     * Adapter for the search fragment ViewPager.
     */
    private class Adapter extends FragmentStatePagerAdapter {
        private Adapter(@NonNull FragmentManager manager) {
            super(manager);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == POSITION_SEARCH) {
                return new BranchTabDiscoveryFragment();
            } else if (position == POSITION_ALL_APPS) {
                return new BranchTabAllAppsFragment();
            } else {
                throw new RuntimeException("Unexpected position: " + position);
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == POSITION_SEARCH) {
                return getString(R.string.branchapp_tab_title_search);
            } else if (position == POSITION_ALL_APPS) {
                return getString(R.string.branchapp_tab_title_myapps);
            } else {
                throw new RuntimeException("Unexpected position " + position);
            }
        }
    }
}
