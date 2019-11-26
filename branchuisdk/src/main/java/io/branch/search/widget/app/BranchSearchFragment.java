package io.branch.search.widget.app;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Keep;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.BranchSearchController;
import io.branch.search.widget.BranchSearchResultsView;
import io.branch.search.widget.R;
import io.branch.search.widget.util.BranchEvents;

/**
 * Need to @Keep this since it can be used directly without {@link BranchSearchActivity}.
 */
@Keep
public class BranchSearchFragment extends Fragment {

    private BranchSearchController mController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getContentLayoutRes(), container, false);
    }

    @LayoutRes
    protected int getContentLayoutRes() {
        return R.layout.branchapp_search_fragment;
    }

    @IdRes
    protected int getContentContainerId() {
        return R.id.branchapp_fragment_container;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            navigateToTabs();
        }
    }

    protected void navigateToTabs() {
        getChildFragmentManager().beginTransaction()
                .replace(getContentContainerId(), new BranchTabsFragment())
                .commit();
    }

    protected void navigateToSettings() {
        // I'd say this is the best place for event logging - if put in the Settings fragment,
        // we'd probably be logging twice in some occasions, e.g. for activity recreation
        new BranchEvent(BranchEvents.TYPE_OPEN)
                .addCustomDataProperty(BranchEvents.Open.TARGET,
                        BranchEvents.Open.TARGET_SEARCH_SETTINGS)
                .logEvent(requireContext());
        getChildFragmentManager().beginTransaction()
                .replace(getContentContainerId(), new BranchSettingsFragment())
                .addToBackStack("settings")
                .commit();
    }

    protected void navigateBack() {
        getChildFragmentManager().popBackStack();
    }

    @NonNull
    final BranchSearchController initializeSearchController(
            @NonNull FragmentManager manager,
            @NonNull BranchSearchResultsView view) {
        mController = onInitializeSearchController(manager, view);
        return mController;
    }

    final void tearDownSearchController(@NonNull BranchSearchController controller) {
        onTearDownSearchController(controller);
        mController = null;
    }

    @NonNull
    protected BranchSearchController onInitializeSearchController(
            @NonNull FragmentManager manager,
            @NonNull BranchSearchResultsView view) {
        return BranchSearchController.init(manager, view);
    }

    protected void onTearDownSearchController(@NonNull BranchSearchController controller) {
        controller.tearDown();
    }
}
