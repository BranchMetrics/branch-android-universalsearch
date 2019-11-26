package io.branch.search.widget.provider;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.branch.search.widget.ui.DiscoveryViewHolder;
import io.branch.search.widget.ui.HeaderViewHolder;

/**
 * A simple {@link RecyclerView.Adapter} implementation to be used by provider sections.
 * It is backed by a List and supports a category header with a collapse / expand feature.
 */
@SuppressWarnings("WeakerAccess")
public abstract class DiscoveryAdapter<T>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements DiscoveryViewHolder.Callback<T> {

    public interface Callback<T> {
        void onItemClick(@NonNull T item, @Nullable Object payload, int position);
    }

    // Just a random number unlikely to be used by subclasses
    @SuppressWarnings("WeakerAccess")
    public static final int VIEW_TYPE_HEADER = -99;

    @NonNull
    private final List<T> mItems = new ArrayList<>();
    private final CharSequence mHeader;
    private boolean mCollapsed;
    @NonNull
    private final Context mContext;
    @NonNull
    private final Callback<T> mCallback;

    // Set by DiscoverySection. Should not be changed or accessed by
    // any other class.
    int mColumns;

    /**
     * Creates a new adapter. If headerTitle is not null, we will add a category header
     * on top of the list and use the provided text to fill the info. All of this can be
     * overriden below.
     *
     * @param context a context
     * @param header a nullable header title
     */
    protected DiscoveryAdapter(@NonNull Context context,
                               @NonNull Callback<T> callback,
                               @Nullable CharSequence header) {
        mContext = context;
        mCallback = callback;
        mHeader = header;
    }

    @NonNull
    public final Context getContext() {
        return mContext;
    }

    /**
     * Returns true if this adapter has a header set.
     * @return true if header
     */
    public final boolean hasHeader() {
        return mHeader != null;
    }

    /**
     * Returns true if this adapter is in the collapsed state.
     * @return true if collapsed
     */
    public final boolean isCollapsed() {
        return mCollapsed;
    }

    /**
     * Returns the maximum number of items that this adapter should show.
     * @return the max number
     */
    protected int getCapacity(int columns) {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the maximum number of items that this adapter should show
     * in the collapsed state. Can be negative or 0 to disable collapsing.
     * Defaults to 0.
     *
     * @return the max number
     */
    protected int getCollapsedCapacity(int columns) {
        return 0;
    }

    @Override
    public final int getItemCount() {
        return getItemCount(mCollapsed);
    }

    private int getItemCount(boolean collapsed) {
        int collapsedLimit = getCollapsedCapacity(mColumns);
        int maxLimit = getCapacity(mColumns);
        int limit = (collapsed && collapsedLimit > 0)
                ? collapsedLimit : maxLimit;
        int count = Math.min(limit, onGetItemCount());
        if (count == 0 || !hasHeader()) {
            return count;
        } else {
            return count + 1;
        }
    }

    /**
     * Can be subclassed to return an item count different than what the adapter
     * is aware of, for example to insert items.
     * @return the item count
     */
    protected int onGetItemCount() {
        return mItems.size();
    }

    /**
     * Applies a new set of items and dispatches an UI update.
     * Returns a list of items that were not consumed.
     * By default, we consume all items, but subclasses can override.
     *
     * @param items new items
     */
    @NonNull
    public List<T> setItems(@NonNull List<T> items) {
        if (getCollapsedCapacity(mColumns) > 0) {
            // Always start in the collapsed state.
            mCollapsed = true;
        }
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
        return Collections.emptyList();
    }

    /**
     * Returns the item for a given adapter position, or null
     * if no item was found (e.g. if the position belongs to the header or is invalid).
     * @param adapterPosition the adapter position
     * @return the item or null
     */
    @Nullable
    public final T getItemForPosition(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition > getItemCount() - 1) {
            return null;
        }
        if (hasHeader() && adapterPosition == 0) {
            return null;
        }
        int itemsPosition = adapterPosition;
        if (hasHeader()) itemsPosition--;
        return onGetItemForPosition(itemsPosition);
    }

    /**
     * Can be implemented to change the logic by which we fetch the item
     * for a given position. By default, we just query the list.
     * @param itemsPosition position in the item array
     * @return actual item
     */
    @NonNull
    protected T onGetItemForPosition(int itemsPosition) {
        return mItems.get(itemsPosition);
    }

    @Override
    public final int getItemViewType(int adapterPosition) {
        if (hasHeader() && adapterPosition == 0) {
            return VIEW_TYPE_HEADER;
        }
        return onGetItemViewType(adapterPosition, getItemForPosition(adapterPosition));
    }

    /**
     * Returns the item view type for this adapter position and item.
     * @param adapterPosition the adapter position
     * @param item the item
     * @return the view type
     */
    protected int onGetItemViewType(int adapterPosition, T item) {
        return 0;
    }

    @NonNull
    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                            int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        if (viewType == VIEW_TYPE_HEADER) {
            return onCreateHeaderViewHolder(inflater, parent);
        } else {
            return onCreateItemViewHolder(inflater, parent, viewType);
        }
    }

    /**
     * Creates a view holder for the header, if needed.
     * @param inflater an inflater
     * @param parent the parent
     * @return a view holder
     */
    @NonNull
    protected DiscoveryViewHolder<CharSequence> onCreateHeaderViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent) {
        return new HeaderViewHolder(inflater, parent,
                new DiscoveryViewHolder.Callback<CharSequence>() {
            @Override
            public void onClick(@NonNull CharSequence item,
                                @Nullable Object payload,
                                int position) {
                toggleCollapseState();
            }
        });
    }

    /**
     * Can be called to toggle the collapse state. By default this is called
     * whenever the header is clicked, if a header is present.
     */
    protected final void toggleCollapseState() {
        if (mCollapsed) {
            mCollapsed = false;
            onCollapseStateChanged(false,
                    getItemCount(true),
                    getItemCount(false));
        } else {
            // Before collapsing, check that it is enabled.
            int count = getCollapsedCapacity(mColumns);
            if (count > 0) {
                mCollapsed = true;
                onCollapseStateChanged(true,
                        getItemCount(false),
                        getItemCount(true));
            }
        }
    }

    /**
     * Called when the collapse state changed.
     * @param collapsed new state
     * @param oldCount old item count
     * @param newCount new item count
     */
    protected void onCollapseStateChanged(boolean collapsed, int oldCount, int newCount) {
        // If we have a header, rebind.
        if (hasHeader()) notifyItemChanged(0);
        if (newCount > oldCount) {
            notifyItemRangeInserted(oldCount, newCount - oldCount);
        } else if (newCount < oldCount) {
            notifyItemRangeRemoved(newCount, oldCount - newCount);
        }
    }

    /**
     * Creates a view holder for items.
     * @param inflater an inflater
     * @param parent the parent
     * @param viewType the type
     * @return a view holder
     */
    @NonNull
    protected abstract DiscoveryViewHolder<T> onCreateItemViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            int viewType);

    @Override
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @SuppressWarnings("unchecked")
    @CallSuper
    @Override
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder,
                                     int position) {
        if (viewHolder.getItemViewType() == VIEW_TYPE_HEADER) {
            onBindHeader((DiscoveryViewHolder<CharSequence>) viewHolder);
        } else {
            T item = getItemForPosition(position);
            if (item != null) {
                onBindItem((DiscoveryViewHolder<T>) viewHolder, item);
            }
        }
    }

    /**
     * Binds the header view.
     * @param holder the header view holder
     */
    protected void onBindHeader(@NonNull DiscoveryViewHolder<CharSequence> holder) {
        boolean canCollapse = getCollapsedCapacity(mColumns) > 0;
        boolean shouldCollapse = getItemCount(true) != getItemCount(false);
        if (canCollapse && shouldCollapse) {
            holder.bind(mHeader, "", mCollapsed);
        } else {
            holder.bind(mHeader, "", null);
        }
    }

    /**
     * Binds the item view.
     * @param viewHolder the item view holder
     * @param item the item
     */
    protected abstract void onBindItem(@NonNull DiscoveryViewHolder<T> viewHolder,
                                       @NonNull T item);

    /**
     * Computes the item position (position in the item array) for the given
     * adapter position. At this level, we basically only have to account for the
     * presence of the header. Subclasses might do something more complex.
     *
     * @param adapterPosition adapter position
     * @return item position
     */
    protected int computeItemPositionForAdapterPosition(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition > getItemCount() - 1) {
            throw new IllegalArgumentException("Invalid adapter position! " + adapterPosition);
        }
        if (hasHeader()) adapterPosition--;
        return adapterPosition;
    }

    @Override
    public void onClick(@NonNull T item, @Nullable Object payload, int position) {
        position = computeItemPositionForAdapterPosition(position);
        mCallback.onItemClick(item, payload, position);
    }
}
