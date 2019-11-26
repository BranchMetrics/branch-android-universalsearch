package io.branch.search.widget.app;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.R;
import io.branch.search.widget.util.BranchEvents;

/**
 * A secondary fragment showing a list of settings.
 */
public class BranchSettingsFragment extends Fragment {

    @NonNull
    private BranchSearchFragment getSearchFragment() {
        //noinspection ConstantConditions
        return (BranchSearchFragment) getParentFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.branchapp_settings_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = view.findViewById(R.id.branchapp_settings_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSearchFragment().navigateBack();
            }
        });
        // Assume the toolbar elements color is the same of the tabs.
        int color = ContextCompat.getColor(requireContext(), R.color.branchapp_tab_icon_selected);
        Drawable icon = toolbar.getNavigationIcon();
        if (icon != null) icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        toolbar.setTitleTextColor(color);
        // Add the internal preference fragment, only if SIS is null.
        if (savedInstanceState == null) {
            Fragment preferenceFragment = new InnerSettingsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.branchapp_settings_layout, preferenceFragment)
                    .commit();
        }
    }

    public static class InnerSettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.branchapp_settings);
            Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    // Add this to all the preferences in the tree.
                    // Returning true means allowing the preference change.
                    new BranchEvent(BranchEvents.TYPE_SETTINGS)
                            .addCustomDataProperty(BranchEvents.Settings.PREFERENCE_NAME,
                                    preference.getKey())
                            .addCustomDataProperty(BranchEvents.Settings.PREFERENCE_NEW_VALUE,
                                    String.valueOf(o))
                            .logEvent(requireContext());
                    return true;
                }
            };
            addListenerRecursive(getPreferenceScreen(), listener);
        }

        private void addListenerRecursive(@NonNull PreferenceGroup group,
                                          @NonNull Preference.OnPreferenceChangeListener listener) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference preference = group.getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    addListenerRecursive((PreferenceGroup) preference, listener);
                } else {
                    preference.setOnPreferenceChangeListener(listener);
                }
            }
        }
    }
}
