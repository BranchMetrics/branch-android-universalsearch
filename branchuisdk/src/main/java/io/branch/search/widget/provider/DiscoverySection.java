package io.branch.search.widget.provider;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.List;

import io.branch.search.widget.R;
import io.branch.search.widget.util.Utility;
import io.branch.search.widget.ui.decorations.PaddingDecoration;
import io.branch.search.widget.ui.decorations.SpacingDecoration;

/**
 * Represents a piece of UI for a {@link DiscoveryProvider}.
 *
 * The vast majority of providers will have a single section (as in the
 * {@link SimpleDiscoveryProvider} implementation). However, it is possible for providers
 * to define more than one section for their UI, and to update them dynamically.
 *
 * The purpose of sections is mostly visual - sections can have a consistent background
 * and consistent dividers between them. This can also make a "provider with multiple sections"
 * look like "multiple providers".
 *
 * Providers can:
 * - List the section they will be showing in {@link DiscoveryProvider#computeSections(List)}.
 *   Sections have a "type" that has the same meaning of the view type in a RecyclerView adapter.
 *   Sections with the same type might be reused to improve performance.
 * - Set-up the section during {@link DiscoveryProvider#onCreateSection(Builder)}.
 *   At this point the section type can be inspected so providers can create different sections for
 *   different types with simplified logic.
 *
 * The section view is then created and can be configured further during
 * {@link DiscoveryProvider#onSectionCreated(DiscoverySection, View)}.
 *
 * When the provider loads results, they are passed to all the sections in sequence.
 * Each section's adapter has the ability to consume some items, so that the next section
 * will only see the remaining items.
 * See {@link DiscoveryAdapter#setItems(List)} and
 * {@link DiscoveryProvider#applyResults(String, List)}.
 *
 * @param <T> the model type
 */
public final class DiscoverySection<T> {

    /**
     * An object used to build a section. This class should only be instantiated
     * by the base provider.
     * @param <T> model
     */
    @SuppressWarnings("WeakerAccess")
    public static class Builder<T> {
        public final int type;
        public DiscoveryAdapter<T> adapter;
        public int orientation = RecyclerView.VERTICAL;
        public int columns = 1;
        @LayoutRes
        public int layoutRes = R.layout.branch_section;

        /**
         * Called by {@link DiscoveryProvider}.
         * @param type the section type
         */
        Builder(int type) {
            this.type = type;
        }

        /**
         * Called by {@link DiscoveryProvider}.
         * @return a section
         */
        @NonNull
        DiscoverySection<T> build() {
            if (adapter == null) {
                throw new RuntimeException("You should pass an adapter to this builder " +
                        "during DiscoveryProvider.onCreateSection.");
            }
            return new DiscoverySection<>(type, adapter, layoutRes, orientation, columns);
        }
    }

    private final int mType;
    private final View mView;
    private final RecyclerView mRecycler;
    private final GridLayoutManager mManager;
    private final DiscoveryAdapter<T> mAdapter;
    private SpacingDecoration mSpacingDecoration;
    private PaddingDecoration mPaddingDecoration;

    @LayoutRes
    private int mAutoColumnsLayout = -1;

    private DiscoverySection(int type,
                            @NonNull DiscoveryAdapter<T> adapter,
                            @LayoutRes int layoutRes,
                            int orientation,
                            int columns) {
        mType = type;
        Context context = adapter.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        FrameLayout container = new FrameLayout(context);
        mView = inflater.inflate(layoutRes, container, false);
        mRecycler = mView.findViewById(R.id.recycler);
        mRecycler.setNestedScrollingEnabled(false);
        mManager = new GridLayoutManager(context,
                columns,
                orientation,
                false);
        mManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                //noinspection SwitchStatementWithTooFewBranches
                switch (mAdapter.getItemViewType(position)) {
                    case DiscoveryAdapter.VIEW_TYPE_HEADER:
                        return mManager.getSpanCount();
                    default:
                        return 1;
                }
            }
        });
        mRecycler.setLayoutManager(mManager);
        mAdapter = adapter;
        mAdapter.mColumns = columns;
        mRecycler.setAdapter(mAdapter);
        mRecycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (mAutoColumnsLayout != -1) {
                    setAutoColumns(mAutoColumnsLayout);
                }
            }
        });
    }

    /**
     * Sets the columns of this UI piece. If the orientation
     * is horizontal, this actually means "rows".
     * @param columns columns
     */
    @SuppressWarnings("unused")
    public void setColumns(int columns) {
        if (columns != mManager.getSpanCount()) {
            mManager.setSpanCount(columns);
            mAdapter.mColumns = columns;
        }
    }

    /**
     * Like {@link #setColumns(int)}, but the number of columns will be automatically inferred
     * by measuring the given item layout and comparing with the available space.
     * Spacing and padding are also accounted for.
     *
     * @param itemLayout layout
     */
    @SuppressWarnings("WeakerAccess")
    public void setAutoColumns(@LayoutRes final int itemLayout) {
        mAutoColumnsLayout = itemLayout;
        int extraHorizontal = mPaddingDecoration != null
                ? mPaddingDecoration.getPaddingHorizontal() : 0;
        int extraVertical = mPaddingDecoration != null
                ? mPaddingDecoration.getPaddingVertical() : 0;
        int extra = mManager.getOrientation() == RecyclerView.VERTICAL
                ? extraHorizontal : extraVertical;
        int spacing = mSpacingDecoration != null ? mSpacingDecoration.getSpacing() : 0;
        int columns = Utility.computeRecyclerColumns(mRecycler, itemLayout, spacing, extra);
        setColumns(columns);
    }

    /**
     * Sets the orientation of this UI piece.
     * This is either {@link RecyclerView#HORIZONTAL} or {@link RecyclerView#VERTICAL}.
     * @param orientation orientation
     */
    public void setOrientation(int orientation) {
        mManager.setOrientation(orientation);
    }

    /**
     * Sets the internal spacing that will be added in between the
     * adapter items (not outside).
     * @param spacing spacing
     */
    @SuppressWarnings("WeakerAccess")
    public void setSpacing(float spacing) {
        if (mSpacingDecoration != null) {
            mRecycler.removeItemDecoration(mSpacingDecoration);
        }
        mSpacingDecoration = new SpacingDecoration(Math.round(spacing),
                mAdapter.hasHeader() ? 1 : 0);
        mRecycler.addItemDecoration(mSpacingDecoration);
    }

    /**
     * Sets the outside padding that will be added outside of the items.
     * This gives more control than applying padding to the RecyclerView, because
     * this function will not include the section header (if present).
     *
     * @param padding padding
     */
    public void setPadding(float padding) {
        setPadding(padding, padding);
    }

    /**
     * Sets the outside padding that will be added outside of the items.
     * This gives more control than applying padding to the RecyclerView, because
     * this function will not include the section header (if present).
     *
     * @param paddingHorizontal horizontal
     * @param paddingVertical vertical
     */
    @SuppressWarnings("WeakerAccess")
    public void setPadding(float paddingHorizontal, float paddingVertical) {
        if (mPaddingDecoration != null) {
            mRecycler.removeItemDecoration(mPaddingDecoration);
        }
        mPaddingDecoration = new PaddingDecoration(mAdapter.hasHeader() ? 1 : 0,
                Math.round(paddingHorizontal), Math.round(paddingVertical));
        mRecycler.addItemDecoration(mPaddingDecoration);
    }

    /**
     * Called by {@link DiscoveryProvider}.
     * Returns a list of items that were not consumed.
     *
     * @param results results
     * @return unconsumed
     */
    @NonNull
    List<T> apply(@NonNull List<T> results) {
        return mAdapter.setItems(results);
    }

    /**
     * Returns this UI root view.
     * @return root view
     */
    @NonNull
    public View getView() {
        return mView;
    }

    /**
     * Returns this adapter capacity.
     * @return capacity
     */
    public int getCapacity() {
        return mAdapter.getCapacity(mAdapter.mColumns);
    }

    /**
     * Returns the section type.
     * @return section type
     */
    public int getType() {
        return mType;
    }

    /**
     * Returns the section adapter.
     * @return adapter
     */
    @SuppressWarnings("unused")
    @NonNull
    public DiscoveryAdapter<T> getAdapter() {
        return mAdapter;
    }
}
