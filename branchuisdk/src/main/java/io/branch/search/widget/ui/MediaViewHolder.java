package io.branch.search.widget.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.branch.search.widget.R;
import io.branch.search.widget.model.Media;

/**
 * Shows media results (icon + label) from {@link Media}s.
 */
public class MediaViewHolder extends DiscoveryViewHolder<Media> {

    @NonNull
    private final ImageView mIcon;
    @NonNull
    private final TextView mLabel;

    public MediaViewHolder(@NonNull LayoutInflater inflater,
                           @NonNull ViewGroup parent,
                           @Nullable Callback<Media> callback) {
        super(inflater, parent, R.layout.branch_media, callback);
        mIcon = itemView.findViewById(R.id.branch_media_icon);
        mLabel = itemView.findViewById(R.id.branch_media_label);
    }

    @Override
    protected void onBind(@NonNull Media model,
                          @NonNull String query,
                          @Nullable Object payload) {
        Context context = getContext();
        mLabel.setText(model.getName());
        mIcon.getBackground().setColorFilter(ContextCompat.getColor(context, model.getColor()),
                PorterDuff.Mode.SRC_IN);
        mIcon.setImageDrawable(ContextCompat.getDrawable(context, model.getIcon()));
        mIcon.setColorFilter(ContextCompat.getColor(context, R.color.branch_media_icon));
    }
}
