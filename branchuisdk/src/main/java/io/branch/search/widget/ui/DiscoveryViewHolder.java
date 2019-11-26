package io.branch.search.widget.ui;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Base class for ViewHolders. We want them to specify their own layout resource
 * and they might also have a click listener.
 */
@Keep
public abstract class DiscoveryViewHolder<T> extends RecyclerView.ViewHolder {

    public interface Callback<T> {
        void onClick(@NonNull T item, @Nullable Object payload, int position);
    }

    protected final Callback<T> callback;

    protected DiscoveryViewHolder(@NonNull LayoutInflater inflater,
                                  @NonNull ViewGroup parent,
                                  @LayoutRes int layout,
                                  @Nullable Callback<T> callback) {
        super(inflater.inflate(layout, parent, false));
        this.callback = callback;
    }

    public void bind(final @NonNull T model,
                     @NonNull String query,
                     final @Nullable Object payload) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onClick(model, payload, getAdapterPosition());
                }
            }
        });
        onBind(model, query, payload);
    }

    protected abstract void onBind(@NonNull T model,
                                   @NonNull String query,
                                   @Nullable Object payload);

    @NonNull
    protected Context getContext() {
        return itemView.getContext();
    }
}
