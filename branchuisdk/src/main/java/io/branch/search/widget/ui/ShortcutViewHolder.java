package io.branch.search.widget.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.branch.search.widget.R;
import io.branch.search.widget.model.Shortcut;

/**
 * Displays shortcut items from {@link io.branch.search.widget.provider.ShortcutProvider}.
 */
public class ShortcutViewHolder extends DiscoveryViewHolder<Shortcut> {

    @NonNull
    private final ImageView mIcon;
    @NonNull
    private final TextView mTitle;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    @NonNull
    private final TextView mSubtitle;

    public ShortcutViewHolder(@NonNull LayoutInflater inflater,
                              @NonNull ViewGroup parent,
                              @Nullable Callback<Shortcut> clickListener) {
        super(inflater, parent, R.layout.branch_shortcut, clickListener);
        mIcon = itemView.findViewById(R.id.shortcut_img);
        mTitle = itemView.findViewById(R.id.shortcut_title);
        mSubtitle = itemView.findViewById(R.id.shortcut_subtitle);
    }

    @Override
    protected void onBind(@NonNull Shortcut model,
                          @NonNull String query,
                          @Nullable Object payload) {
        mIcon.setImageDrawable(model.getIcon(getContext()));
        mTitle.setText(model.getLabel(getContext()));
    }
}
