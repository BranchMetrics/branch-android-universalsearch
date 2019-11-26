package io.branch.search.widget.app;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Keep;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import io.branch.search.widget.R;

/**
 * Need to @Keep this since it can be declared in the hosting app manifest.
 * This simply contains a {@link BranchSearchFragment}.
 */
@Keep
public class BranchSearchActivity extends AppCompatActivity {

    private BranchSearchFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentLayoutRes());
        if (savedInstanceState == null) {
            mFragment = onInstantiateContent();
            getSupportFragmentManager().beginTransaction()
                    .replace(getContentContainerId(),
                            mFragment,
                            BranchSearchFragment.class.getSimpleName())
                    .setPrimaryNavigationFragment(mFragment)
                    .commit();
        } else {
            mFragment = (BranchSearchFragment) getSupportFragmentManager()
                    .findFragmentByTag(BranchSearchFragment.class.getSimpleName());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFragment = null;
    }

    @LayoutRes
    protected int getContentLayoutRes() {
        return R.layout.branchapp_search_activity;
    }

    @IdRes
    protected int getContentContainerId() {
        return R.id.branchapp_fragment_container;
    }

    @NonNull
    protected BranchSearchFragment onInstantiateContent() {
        return new BranchSearchFragment();
    }
}
