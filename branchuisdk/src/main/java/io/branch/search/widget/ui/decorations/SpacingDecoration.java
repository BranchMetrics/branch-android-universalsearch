package io.branch.search.widget.ui.decorations;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * A {@link RecyclerView.ItemDecoration} that adds spacing between items.
 * Spacing is only added between items, not on the edges of the recycler.
 *
 * This will work for {@link LinearLayoutManager} and {@link GridLayoutManager}.
 * Other layout managers are not supported.
 */
public class SpacingDecoration extends OffsetDecoration {

    private final int mSpacing;
    private final Drawable mDrawable;
    private final Data mData = new Data();

    public SpacingDecoration(int spacing) {
        this(spacing, 0);
    }

    public SpacingDecoration(int spacing, int offset) {
        this(spacing, offset, null);
    }

    public SpacingDecoration(int spacing, int offset, @Nullable Drawable drawable) {
        super(offset);
        mSpacing = spacing;
        mDrawable = drawable;
    }

    public int getSpacing() {
        return mSpacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
        computeData(parent, view, mData);
        if (mData.primaryPosition >= 0) {
            maybeApplyPrimarySpacing(outRect, view,
                    mData.primaryPosition,
                    mData.isVertical);
        }
        if (mData.secondaryPosition >= 0) {
            maybeApplySecondarySpacing(outRect, view,
                    mData.secondaryPosition,
                    mData.secondaryCount,
                    mData.isVertical);
        }
    }

    private void maybeApplyPrimarySpacing(@NonNull Rect outRect,
                                          @NonNull View view,
                                          int positionInLine,
                                          boolean vertical) {
        // In the primary axis we have no big issues. Just apply if not first.
        if (positionInLine > 0) {
            applyStartMargin(outRect, view, vertical, mSpacing);
        }
    }

    private void maybeApplySecondarySpacing(@NonNull Rect outRect,
                                            @NonNull View view,
                                            int positionInLine,
                                            int lineSize,
                                            boolean vertical) {
        // We could check that positionInLine is > 0 just like the primary case, however this
        // has issues. Assuming a vertical grid with 3 columns, if we apply start margin only
        // on columns 2-3, the adapter will think that they are bigger than 1, but it will try
        // to evenly space everything, so it will make 1 bigger.
        // -> We need to respect the internal spacing between items, but to keep the item
        //    margins constant with respect to the item position.
        // EXAMPLE: 3 elements, the spacing between them is 2.
        // The first element must have:     marginStart = 0,    marginEnd = 1.33
        // The second element must have:    marginStart = 0.66  marginEnd = 0.66
        // The third element must have:     marginStart = 1.33  marginEnd = 0
        // The sum of internal margins is always 2, and the sum of margins for the same element
        // is constant between elements (1.33). This is exactly what we need.
        float startMargin = computeSecondaryMarginStart(positionInLine, lineSize,
                mSpacing);
        float endMargin = computeSecondaryMarginEnd(positionInLine, lineSize, mSpacing);
        applyStartMargin(outRect, view, !vertical, Math.round(startMargin));
        applyEndMargin(outRect, view, !vertical, Math.round(endMargin));
    }

    private static float computeSecondaryMarginStart(int position, int count, float spacing) {
        if (position == 0) {
            return 0F;
        } else {
            float previousEndMargin = computeSecondaryMarginEnd(position - 1,
                    count, spacing);
            return spacing - previousEndMargin;
        }
    }

    private static float computeSecondaryMarginEnd(int position, int count, float spacing) {
        float fullMargin = spacing * (count - 1) / count;
        float startMargin = computeSecondaryMarginStart(position, count, spacing);
        return fullMargin - startMargin;
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {
        // Drawing only on the primary direction. This can be improved but not needed now.
        maybeDrawPrimaryDecoration(c, parent);
    }

    private void maybeDrawPrimaryDecoration(@NonNull Canvas canvas, @NonNull RecyclerView parent) {
        if (mDrawable == null) return;
        // We think in terms of primary lines, so e.g. count == number of rows for a vertical grid.
        // For simplicity we also assume that layoutPosition == adapterPosition .
        int count, offset, firstVisible, lastVisible;
        boolean isVertical;
        RecyclerView.Adapter adapter = parent.getAdapter();
        RecyclerView.LayoutManager manager = parent.getLayoutManager();
        if (adapter == null || manager == null) return;
        if (manager instanceof GridLayoutManager) {
            GridLayoutManager grid = (GridLayoutManager) manager;
            isVertical = grid.getOrientation() == RecyclerView.VERTICAL;
            GridLayoutManager.SpanSizeLookup lookup = grid.getSpanSizeLookup();
            // Primary count is the span group index of the last item.
            // Primary offset should be computed like maybeApplySpacing does.
            count = lookup.getSpanGroupIndex(adapter.getItemCount() - 1,
                    grid.getSpanCount()) + 1;
            int lastOffsetLine = getOffset() == 0 ? -1 :
                    lookup.getSpanGroupIndex(getOffset() - 1, grid.getSpanCount());
            offset = lastOffsetLine + 1;
            firstVisible = lookup.getSpanGroupIndex(grid.findFirstVisibleItemPosition(),
                    grid.getSpanCount());
            lastVisible = lookup.getSpanGroupIndex(grid.findLastVisibleItemPosition(),
                    grid.getSpanCount());
        } else if (manager instanceof LinearLayoutManager) {
            LinearLayoutManager list = (LinearLayoutManager) manager;
            isVertical = list.getOrientation() == RecyclerView.VERTICAL;
            count = adapter.getItemCount();
            offset = getOffset();
            firstVisible = list.findFirstVisibleItemPosition();
            lastVisible = list.findLastVisibleItemPosition();
        } else {
            throw new RuntimeException("Unsupported LayoutManager: "
                    + manager.getClass().getSimpleName());
        }

        // Add a +1 because we only draw inside, so exclude the first and draw before the others.
        // Take the max between the real offset and what's visible. Opposite for the end.
        int start = Math.max(offset + 1, firstVisible);
        int end = Math.min(count, lastVisible + 1);
        int lastViewPosition = 0; // Optimization
        for (int position = start; position < end; position++) {
            // Find the adapter/layout position for this line position.
            int viewPosition;
            if (manager instanceof GridLayoutManager) {
                GridLayoutManager list = (GridLayoutManager) manager;
                GridLayoutManager.SpanSizeLookup lookup = list.getSpanSizeLookup();
                viewPosition = lastViewPosition;
                while (lookup.getSpanGroupIndex(viewPosition, list.getSpanCount()) != position) {
                    viewPosition++;
                }
            } else {
                viewPosition = position;
            }
            lastViewPosition = viewPosition;
            // Find a suitable view and draw.
            RecyclerView.ViewHolder holder = parent.findViewHolderForLayoutPosition(viewPosition);
            View view = holder.itemView;
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            if (isVertical) {
                int bottom = view.getTop() - params.topMargin;
                mDrawable.setBounds(parent.getLeft() + parent.getPaddingLeft(),
                        bottom - mSpacing,
                        parent.getRight() - parent.getPaddingRight(),
                        bottom);
            } else {
                boolean isLtr = parent.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL;
                int left, right;
                if (isLtr) {
                    // Drawing before means drawing on the left side of the item.
                    right = view.getLeft() - params.leftMargin;
                    left = right - mSpacing;
                } else {
                    // Drawing before means drawing on the right side of the item.
                    left = view.getRight() + params.rightMargin;
                    right = left + mSpacing;
                }
                mDrawable.setBounds(left,
                        parent.getTop() + parent.getPaddingTop(),
                        right,
                        parent.getBottom() - parent.getPaddingBottom());
            }
            mDrawable.draw(canvas);
        }
    }
}
