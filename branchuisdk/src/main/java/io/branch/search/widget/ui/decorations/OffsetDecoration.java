package io.branch.search.widget.ui.decorations;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Base abstract class for {@link android.support.v7.widget.RecyclerView.ItemDecoration}s
 * that accept an offset in their constructor, which means a certain number of items that
 * should be ignored when applying the effect.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OffsetDecoration extends RecyclerView.ItemDecoration {

    /**
     * Data of a specific item along the two axes.
     */
    protected static class Data {
        boolean isVertical;
        int primaryPosition;
        int primaryCount;
        int secondaryPosition;
        int secondaryCount;
    }

    private int mOffset; // Number of items to be ignored

    protected OffsetDecoration(int offset) {
        mOffset = offset;
    }

    protected int getOffset() {
        return mOffset;
    }

    protected void computeData(@NonNull RecyclerView parent,
                               @NonNull View view,
                               @NonNull Data outData) {
        RecyclerView.Adapter adapter = parent.getAdapter();
        RecyclerView.LayoutManager manager = parent.getLayoutManager();
        if (adapter == null || manager == null) return;
        int position = parent.getChildLayoutPosition(view);
        if (manager instanceof GridLayoutManager) {
            GridLayoutManager grid = (GridLayoutManager) manager;
            GridLayoutManager.LayoutParams params
                    = (GridLayoutManager.LayoutParams) view.getLayoutParams();
            GridLayoutManager.SpanSizeLookup lookup = grid.getSpanSizeLookup();
            outData.isVertical = grid.getOrientation() == RecyclerView.VERTICAL;
            outData.primaryPosition = lookup.getSpanGroupIndex(position, grid.getSpanCount());
            outData.primaryCount = lookup.getSpanGroupIndex(
                    adapter.getItemCount() - 1, grid.getSpanCount()) + 1;
            outData.secondaryPosition = params.getSpanIndex();
            outData.secondaryCount = grid.getSpanCount();
            // For primary position, we must exclude all the rows that have an offset.
            // For secondary position, we also want to start acting from the next primary row.
            int lastPrimaryOffsetLine = mOffset == 0 ? -1 :
                    lookup.getSpanGroupIndex(mOffset - 1, grid.getSpanCount());
            int primaryOffsetLines = lastPrimaryOffsetLine + 1;
            outData.primaryPosition -= primaryOffsetLines;
            outData.primaryCount -= primaryOffsetLines;
            if (outData.primaryPosition < 0) outData.secondaryPosition = -1;
        } else if (manager instanceof LinearLayoutManager) {
            LinearLayoutManager list = (LinearLayoutManager) manager;
            outData.isVertical = list.getOrientation() == RecyclerView.VERTICAL;
            outData.primaryPosition = position - mOffset;
            outData.primaryCount = adapter.getItemCount() - mOffset;
            outData.secondaryPosition = -1;
            outData.secondaryCount = -1;
        } else {
            throw new RuntimeException("Unsupported LayoutManager: "
                    + manager.getClass().getSimpleName());
        }
    }

    protected void applyStartMargin(@NonNull Rect outRect,
                                    @NonNull View view,
                                    boolean vertical,
                                    int margin) {
        if (vertical) {
            outRect.top = margin;
        } else {
            boolean isLtr = view.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL;
            if (isLtr) {
                outRect.left = margin;
            } else {
                outRect.right = margin;
            }
        }
    }

    protected void applyEndMargin(@NonNull Rect outRect,
                                  @NonNull View view,
                                  boolean vertical,
                                  int margin) {
        if (vertical) {
            outRect.bottom = margin;
        } else {
            boolean isLtr = view.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL;
            if (isLtr) {
                outRect.right = margin;
            } else {
                outRect.left = margin;
            }
        }
    }
}
