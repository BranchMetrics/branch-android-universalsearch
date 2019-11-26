package io.branch.search.widget;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import io.branch.search.widget.provider.IDiscoveryProvider;

/**
 * This vertical scrolling class should be passed to
 * {@link BranchSearchController#init(FragmentManager, BranchSearchResultsView)}
 * in order to display Branch search results.
 */
public class BranchSearchResultsView extends NestedScrollView {

    private final static float TOUCH_DELTA = 80;

    private LinearLayout mLayout;
    private int mTouchPointerId = -1;
    private float mTouchDownY;
    private boolean mTouchCompleted;

    public BranchSearchResultsView(@NonNull Context context) {
        super(context);
        init();
    }

    public BranchSearchResultsView(@NonNull Context context,
                                   @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BranchSearchResultsView(@NonNull Context context,
                                   @Nullable AttributeSet attrs,
                                   int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mLayout = new LinearLayout(getContext());
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setId(View.generateViewId());
        addView(mLayout);

        setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView nestedScrollView, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {
                //noinspection StatementWithEmptyBody
                if (scrollY == 0) {
                    showKeyboard(null);
                } else {
                    // No need to hide here, we do this through the touch listener.
                }
            }
        });
    }

    /**
     * We analyze touch events instead of using a scroll listener so we can
     * trigger the keyboard action even when the view is not scrollable due
     * to small contents.
     *
     * Using dispatchTouchEvent instead of a touch listener lets us
     * catch the events before they are stolen by child views.
     *
     * @param event event
     * @return true if consumed
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchCompleted = false;
                mTouchPointerId = event.getPointerId(0);
                mTouchDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mTouchCompleted
                        && event.getPointerId(event.getActionIndex()) == mTouchPointerId) {
                    float newY = event.getY();
                    if (newY < mTouchDownY - TOUCH_DELTA) {
                        mTouchCompleted = true;
                        hideKeyboard();
                    } else if (newY > mTouchDownY + TOUCH_DELTA) {
                        if (getScrollY() == 0) {
                            // Check that we are already in the scrollY=0 state.
                            // If we're not, the scroll listener will do this instead.
                            mTouchCompleted = true;
                            showKeyboard(null);
                        }
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Tries to find an activity from the given context.
     * @return an activity if one was found
     */
    @Nullable
    private Activity findActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Shows the keyboard, focusing the given view if not null.
     * @param view view that should get focus or null
     */
    @SuppressWarnings("SameParameterValue")
    void showKeyboard(@Nullable View view) {
        InputMethodManager input
                = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (view != null) {
            view.requestFocus();
            input.showSoftInput(view, 0);
        } else {
            Activity activity = findActivity();
            if (activity == null) return;
            View focus = activity.getCurrentFocus();
            input.showSoftInput(focus, 0);
        }
    }

    /**
     * Hides the keyboard and clears focus from the currently
     * focused view, if one is found.
     */
    void hideKeyboard() {
        Activity activity = findActivity();
        if (activity == null) return;
        IBinder windowToken = activity.getWindow().getDecorView().getWindowToken();
        InputMethodManager input
                = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focus = activity.getCurrentFocus();
        if (focus == null && input.isActive()) {
            input.hideSoftInputFromWindow(windowToken, 0);
        } else if (focus != null && input.isActive(focus)) {
            input.hideSoftInputFromWindow(windowToken, 0);
            // This must be after input.isActive(), or that one returns false.
            focus.clearFocus();
        }
    }

    /**
     * Adds providers to this view, using the given manager.
     * @param manager a fragment manager
     * @param providers providers
     */
    void addProviders(@NonNull FragmentManager manager,
                      @NonNull Iterable<IDiscoveryProvider> providers) {
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        for (IDiscoveryProvider provider : providers) {
            // See if there's another fragment with the same tag. Can happen if
            // init() is called twice or on some state restoration occasions, I guess.
            String tag = provider.getClass().getSimpleName();
            Fragment old = manager.findFragmentByTag(tag);
            if (old != null) {
                fragmentTransaction.remove(old);
            }
            fragmentTransaction.add(mLayout.getId(), provider.getFragment(), tag);
        }
        fragmentTransaction.commit();
    }

    /**
     * Removes providers from this view.
     */
    void clearProviders(@NonNull Iterable<IDiscoveryProvider> providers) {
        // Find the fragment manager.
        FragmentManager manager = null;
        for (IDiscoveryProvider provider : providers) {
            manager = provider.getFragment().getFragmentManager();
            if (manager != null) break;
        }
        if (manager == null) return;

        // Remove all fragments.
        // When removing, we allow state loss because we're tearing down.
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        for (IDiscoveryProvider provider : providers) {
            fragmentTransaction.remove(provider.getFragment());
        }
        fragmentTransaction.commitAllowingStateLoss();
    }
}
