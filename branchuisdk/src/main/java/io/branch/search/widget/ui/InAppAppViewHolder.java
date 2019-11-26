package io.branch.search.widget.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import io.branch.search.BranchAppResult;
import io.branch.search.widget.R;

/**
 * Displays {@link io.branch.search.BranchAppResult} results.
 */
public class InAppAppViewHolder extends DiscoveryViewHolder<BranchAppResult> {

    @NonNull private final SimpleDraweeView mIcon;
    @NonNull private final TextView mName;
    @NonNull private final TextView mType;

    public InAppAppViewHolder(@NonNull LayoutInflater inflater,
                              @NonNull ViewGroup parent,
                              @Nullable Callback<BranchAppResult> callback) {
        super(inflater, parent, R.layout.branch_inapp_app, callback);
        mIcon = itemView.findViewById(R.id.discovery_app_result_img);
        mName = itemView.findViewById(R.id.discovery_app_result_name);
        mType = itemView.findViewById(R.id.discovery_app_result_type);
    }

    @Override
    protected void onBind(@NonNull BranchAppResult model,
                          @NonNull String query,
                          @Nullable Object payload) {
        boolean installed = payload instanceof Boolean ? (Boolean) payload : false;
        mName.setText(model.getAppName());
        mIcon.setImageURI(model.getAppIconUrl());
        mType.setText(installed ? R.string.branch_discover_local : R.string.branch_discover_web);
    }
}
