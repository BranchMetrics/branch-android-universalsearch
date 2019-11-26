package io.branch.search.widget.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import io.branch.search.BranchLinkResult;
import io.branch.search.widget.R;

/**
 * Displays {@link io.branch.search.BranchLinkResult} results.
 */
public class InAppLinkViewHolder extends DiscoveryViewHolder<BranchLinkResult> {

    @NonNull private final SimpleDraweeView mIcon;
    @NonNull private final TextView mTitle;
    @NonNull private final TextView mDescription;

    public InAppLinkViewHolder(@NonNull LayoutInflater inflater,
                               @NonNull ViewGroup parent,
                               @Nullable Callback<BranchLinkResult> callback) {
        super(inflater, parent, R.layout.branch_inapp_link, callback);
        mIcon = itemView.findViewById(R.id.discovery_place_img);
        mTitle = itemView.findViewById(R.id.discovery_place_title);
        mDescription = itemView.findViewById(R.id.discovery_place_description);
    }

    @Override
    protected void onBind(@NonNull BranchLinkResult model,
                          @NonNull String query,
                          @Nullable Object payload) {

        mTitle.setText(model.getName());
        mIcon.setImageURI(model.getImageUrl());
        boolean hasDescription = !TextUtils.isEmpty(model.getDescription());
        mDescription.setText(model.getDescription());
        mDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        mTitle.setMaxLines(hasDescription ? 1 : 2);
    }
}
