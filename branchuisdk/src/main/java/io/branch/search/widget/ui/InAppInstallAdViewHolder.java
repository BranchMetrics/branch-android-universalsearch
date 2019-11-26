package io.branch.search.widget.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import io.branch.search.widget.R;
import io.branch.search.widget.model.InAppInstallAd;
import io.branch.search.widget.provider.InAppSearchProvider;

/**
 * Displays {@link InAppSearchProvider}'s InstallAds.
 */
public class InAppInstallAdViewHolder extends DiscoveryViewHolder<InAppInstallAd> {

    @NonNull private final SimpleDraweeView mImage;
    @NonNull private final TextView mAppName;
    @NonNull private final TextView mLinkName;
    @NonNull private final TextView mLinkDescription;

    public InAppInstallAdViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @Nullable Callback<InAppInstallAd> callback) {
        super(inflater, parent, R.layout.branch_inapp_ad_install, callback);
        mImage = itemView.findViewById(R.id.image);
        mAppName = itemView.findViewById(R.id.app_name);
        mLinkName = itemView.findViewById(R.id.link_name);
        mLinkDescription = itemView.findViewById(R.id.link_description);
    }

    @Override
    protected void onBind(@NonNull InAppInstallAd model,
                          @NonNull String query,
                          @Nullable Object payload) {
        mAppName.setText(model.appResult.getAppName());
        mLinkName.setText(model.linkResult.getName());
        mLinkDescription.setText(model.linkResult.getDescription());
        mImage.setImageURI(model.linkResult.getImageUrl());
    }
}
