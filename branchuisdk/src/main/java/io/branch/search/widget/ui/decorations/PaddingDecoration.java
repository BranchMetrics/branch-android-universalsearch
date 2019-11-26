package io.branch.search.widget.ui.decorations;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * A {@link RecyclerView.ItemDecoration} that adds padding on the four sides of a group of items.
 * The difference with just adding padding to the RecyclerView is that this accepts an offset, so
 * the first(s) items in the list can be ignored.
 *
 * This is useful to apply padding to items but ignore the list header (item 0).
 */
public class PaddingDecoration extends OffsetDecoration {

    private final int mPaddingHorizontal;
    private final int mPaddingVertical;

    private final Data mData = new Data();

    public PaddingDecoration(int offset, int padding) {
        this(offset, padding, padding);
    }

    public PaddingDecoration(int offset, int paddingHorizontal, int paddingVertical) {
        super(offset);
        mPaddingHorizontal = paddingHorizontal;
        mPaddingVertical = paddingVertical;
    }

    public int getPaddingHorizontal() {
        return mPaddingHorizontal;
    }

    public int getPaddingVertical() {
        return mPaddingVertical;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
        computeData(parent, view, mData);
        if (mData.primaryPosition >= 0) {
            maybeApplyPrimaryPadding(outRect, view,
                    mData.primaryPosition,
                    mData.primaryCount,
                    mData.isVertical);
        }
        if (mData.secondaryPosition >= 0) {
            maybeApplySecondaryPadding(outRect, view,
                    mData.secondaryPosition,
                    mData.secondaryCount,
                    mData.isVertical);
        }
    }

    private void maybeApplyPrimaryPadding(@NonNull Rect outRect,
                                          @NonNull View view,
                                          int positionInLine,
                                          int lineSize,
                                          boolean vertical) {
        if (positionInLine == 0) {
            int paddingStart = vertical ? mPaddingVertical : mPaddingHorizontal;
            applyStartMargin(outRect, view, vertical, paddingStart);
        } else if (positionInLine == lineSize - 1) {
            int paddingEnd = vertical ? mPaddingVertical : mPaddingHorizontal;
            applyEndMargin(outRect, view, vertical, paddingEnd);
        }
    }

    private void maybeApplySecondaryPadding(@NonNull Rect outRect,
                                            @NonNull View view,
                                            int positionInLine,
                                            int lineSize,
                                            boolean vertical) {
        maybeApplyPrimaryPadding(outRect, view, positionInLine, lineSize, !vertical);
    }
}
