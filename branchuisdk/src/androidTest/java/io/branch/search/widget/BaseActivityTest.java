package io.branch.search.widget;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import org.junit.Rule;
import org.junit.Test;

public abstract class BaseActivityTest extends BaseTest {

    public static class TestActivity extends FragmentActivity {
        private BranchSearchResultsView mResultsView;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mResultsView = new BranchSearchResultsView(this);
            setContentView(mResultsView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class, true, true);

    @NonNull
    protected FragmentActivity getActivity() {
        return mActivityRule.getActivity();
    }

    @NonNull
    protected BranchSearchResultsView getResultsView() {
        return mActivityRule.getActivity().mResultsView;
    }
}

