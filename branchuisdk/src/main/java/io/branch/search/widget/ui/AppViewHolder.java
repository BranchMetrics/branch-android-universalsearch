package io.branch.search.widget.ui;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.branch.search.widget.R;
import io.branch.search.widget.model.App;

/**
 * Shows app results (icon + label) from {@link App}s.
 */
public class AppViewHolder extends DiscoveryViewHolder<App> {

    @NonNull
    private final ImageView mIcon;
    @NonNull
    private final TextView mLabel;

    public AppViewHolder(@NonNull LayoutInflater inflater,
                         @NonNull ViewGroup parent,
                         @Nullable Callback<App> callback) {
        super(inflater, parent, R.layout.branch_app, callback);
        mIcon = itemView.findViewById(R.id.branch_app_icon);
        mLabel = itemView.findViewById(R.id.branch_app_label);
    }

    @Override
    protected void onBind(@NonNull App model, @NonNull String query, @Nullable Object payload) {
        Drawable drawable;
        if (payload instanceof Drawable) {
            drawable = (Drawable) payload;
        } else {
            drawable = model.getIcon(getContext());
        }
        mIcon.setImageDrawable(drawable);
        mLabel.setText(model.getLabel());
    }
}
