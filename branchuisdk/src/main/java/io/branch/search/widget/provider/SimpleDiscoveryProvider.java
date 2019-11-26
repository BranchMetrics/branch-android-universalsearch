package io.branch.search.widget.provider;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.branch.search.widget.R;
import io.branch.search.widget.ui.DiscoveryViewHolder;

/**
 * A simple implementation of {@link DiscoveryProvider}. In most cases, this is the class that
 * you should be subclassing for custom providers, unless your provider needs multiple UIs
 * (which we call sections, see {@link DiscoverySection}) or other special needs.
 *
 * This provider implementation uses a single section and offers control through
 * protected methods (avoids having to create a separate adapter class).
 *
 * For setting up the UI:
 * - {@link #getAdapter()}
 * - {@link #getAdapterLayoutRes()}
 * - {@link #getAdapterColumns()}
 * - {@link #getAdapterOrientation()}
 * - {@link #getAdapterHeader()}
 *
 * For managing the Recycler elements:
 * - {@link #createAdapterViewHolder(LayoutInflater, ViewGroup, DiscoveryViewHolder.Callback)}
 * - {@link #bindAdapterViewHolder(DiscoveryViewHolder, Object)}
 * - {@link #getAdapterItemPayload(Object)}
 *
 * @param <T> item type
 * @param <VM> view model
 */
public abstract class SimpleDiscoveryProvider<T, VM extends DiscoveryViewModel<T>>
    extends DiscoveryProvider<T, VM> {

    @Override
    protected void onCreateSection(@NonNull DiscoverySection.Builder<T> builder) {
        builder.adapter = getAdapter();
        builder.layoutRes = getAdapterLayoutRes();
        builder.columns = getAdapterColumns();
        builder.orientation = getAdapterOrientation();
    }

    @Override
    protected void onSectionCreated(@NonNull DiscoverySection<T> section, @NonNull View view) {
        super.onSectionCreated(section, view);
        section.setSpacing(getAdapterSpacing());
    }

    /**
     * Returns the layout resource for our UI. Should be something that
     * contains a RecyclerView with the R.id.recycler id.
     * @return a layout res
     */
    @LayoutRes
    protected int getAdapterLayoutRes() {
        return R.layout.branch_section;
    }

    /**
     * Returns the number of "columns" for the UI. If the orientation is
     * horizontal, this actually means "rows".
     * @return the columns
     */
    protected int getAdapterColumns() {
        return 1;
    }

    /**
     * Returns the internal spacing between the adapter elements.
     * @return the internal spacing
     */
    protected float getAdapterSpacing() {
        return 0F;
    }

    /**
     * Returns the UI orientation, either {@link RecyclerView#VERTICAL}
     * or {@link RecyclerView#HORIZONTAL}.
     * @return orientation
     */
    protected int getAdapterOrientation() {
        return RecyclerView.VERTICAL;
    }

    /**
     * Returns the adapter that will be passed to our UI section and will
     * be responsible of showing the elements there.
     * @return the adapter
     */
    @NonNull
    protected Adapter getAdapter() {
        return new Adapter();
    }

    /**
     * Returns the header for the adapter returned by {@link #getAdapter()}.
     * @return header or null
     */
    @Nullable
    protected abstract CharSequence getAdapterHeader();

    /**
     * Should be implemented to create a {@link DiscoveryViewHolder} for the items
     * to be displayed.
     * @param inflater inflater
     * @param parent parent
     * @param callback callback
     * @return holder
     */
    protected abstract DiscoveryViewHolder<T> createAdapterViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @NonNull DiscoveryViewHolder.Callback<T> callback);

    /**
     * Called to bind the holder with the model data that was loaded during
     * {@link DiscoveryProvider#loadResults(String, int, int)}. Thanks to our base view holder this already
     * does what's needed.
     * If the view holder expects some payload, it can be passed by simply
     * overriding {@link #getAdapterItemPayload(Object)}.
     *
     * @param holder holder
     * @param item model
     */
    protected void bindAdapterViewHolder(
            @NonNull DiscoveryViewHolder<T> holder,
            @NonNull T item) {
        holder.bind(item, viewModel.getCurrentQuery(), getAdapterItemPayload(item));
    }

    /**
     * Can be implemented to pass some payload to the view holder that
     * was instantiated during
     * {@link #createAdapterViewHolder(LayoutInflater, ViewGroup, DiscoveryViewHolder.Callback)}.
     * @param item model
     * @return some object
     */
    @Nullable
    protected Object getAdapterItemPayload(@NonNull T item) {
        return null;
    }

    /**
     * Returns the max capacity for this adapter. If the input items are more than the adapter
     * capacity, the remaining items will not be shown.
     * @return capacity
     */
    protected int getAdapterCapacity(int columns) {
        return Integer.MAX_VALUE;
    }

    /**
     * A simple {@link DiscoveryAdapter} implementation that simply delegates
     * everything to the containing provider.
     */
    protected class Adapter extends DiscoveryAdapter<T> {
        @SuppressWarnings("WeakerAccess")
        protected Adapter() {
            super(requireContext(),
                    SimpleDiscoveryProvider.this,
                    getAdapterHeader());
        }

        @NonNull
        @Override
        protected DiscoveryViewHolder<T> onCreateItemViewHolder(
                @NonNull LayoutInflater inflater,
                @NonNull ViewGroup parent,
                int viewType) {
            return createAdapterViewHolder(inflater, parent, this);
        }

        @Override
        protected void onBindItem(@NonNull DiscoveryViewHolder<T> viewHolder,
                                  @NonNull T item) {
            bindAdapterViewHolder(viewHolder, item);
        }

        @Override
        protected int getCapacity(int columns) {
            return getAdapterCapacity(columns);
        }
    }
}
