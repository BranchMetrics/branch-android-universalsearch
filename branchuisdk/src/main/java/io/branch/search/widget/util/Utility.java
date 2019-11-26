package io.branch.search.widget.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Collects utilities of different kinds.
 */
public class Utility {

    private static final String BRANCH_SHARED_PREF_KEY = "BRANCH_SHARED_PREF_KEY";

    /**
     * Returns the branch private {@link SharedPreferences}.
     * @param context a valid context
     * @return the preferences
     */
    public static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(BRANCH_SHARED_PREF_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Calculates the number of columns for a full-width grid layout.
     * The child view will be measured as wrap_content .
     *
     * @param recycler the parent view
     * @param childResId the child view resource id
     * @param childMinSpacing the minimum spacing between children
     * @return the number of columns
     */
    public static int computeRecyclerColumns(
            @NonNull RecyclerView recycler,
            @LayoutRes int childResId,
            @Dimension int childMinSpacing,
            @Dimension int extraPadding) {
        Context context = recycler.getContext();
        LinearLayoutManager manager = (LinearLayoutManager) recycler.getLayoutManager();
        boolean checkWidth = manager.getOrientation() == RecyclerView.VERTICAL;
        int parentDim = checkWidth
                ? computeRecyclerAvailableWidth(recycler)
                : computeRecyclerAvailableHeight(recycler);
        parentDim -= extraPadding * 2;
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup fakeParent = new FrameLayout(context);
        View child = inflater.inflate(childResId, fakeParent, false);
        int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        child.measure(measureSpec, measureSpec);
        int childDim = checkWidth ? child.getMeasuredWidth() : child.getMeasuredHeight();
        int columns = (int) Math.floor((double) parentDim / childDim);
        // Need to account for minimum spacing.
        while (columns * (childDim + childMinSpacing) > parentDim) {
            columns--;
        }
        return columns;
    }

    private static int computeRecyclerAvailableWidth(@NonNull RecyclerView recycler) {
        int parentWidth;
        int parentPadding = recycler.getPaddingLeft() + recycler.getPaddingRight();
        if (recycler.getWidth() > 0) {
            parentWidth = recycler.getWidth() - parentPadding;
        } else if (recycler.getMeasuredWidth() > 0) {
            parentWidth = recycler.getMeasuredWidth() - parentPadding;
        } else {
            parentWidth = recycler.getResources().getDisplayMetrics().widthPixels - parentPadding;
        }
        return parentWidth;
    }

    private static int computeRecyclerAvailableHeight(@NonNull RecyclerView recycler) {
        int parentHeight;
        int parentPadding = recycler.getPaddingTop() + recycler.getPaddingBottom();
        if (recycler.getHeight() > 0) {
            parentHeight = recycler.getHeight() - parentPadding;
        } else if (recycler.getMeasuredHeight() > 0) {
            parentHeight = recycler.getMeasuredHeight() - parentPadding;
        } else {
            parentHeight = recycler.getResources().getDisplayMetrics().heightPixels - parentPadding;
        }
        return parentHeight;
    }
}
