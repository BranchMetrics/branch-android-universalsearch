package io.branch.search.widget.ui;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import io.branch.search.BranchAppResult;
import io.branch.search.widget.R;
import io.branch.search.widget.model.InAppFilter;

/**
 * Displays filters for Branch SDK results.
 */
public class InAppFilterViewHolder extends DiscoveryViewHolder<InAppFilter> {

    private final float mElevationSelected;
    private final float mElevationUnselected;

    private final SimpleDraweeView mImage;
    private final TextView mText;
    // Need to store default typeface here since we're changing it dynamically,
    // otherwise it breaks (calling NORMAL keeps the bold).
    private final Typeface mTextTypeface;

    public InAppFilterViewHolder(@NonNull LayoutInflater inflater,
                                 @NonNull ViewGroup parent,
                                 @NonNull Callback<InAppFilter> callback) {
        super(inflater, parent, R.layout.branch_inapp_filter, callback);
        mElevationSelected = parent.getContext().getResources()
                .getDimension(R.dimen.branch_inapp_filter_elevation_selected);
        mElevationUnselected = parent.getContext().getResources()
                .getDimension(R.dimen.branch_inapp_filter_elevation_unselected);

        mImage = itemView.findViewById(R.id.branch_filter_image);
        mText = itemView.findViewById(R.id.branch_filter_text);
        mTextTypeface = mText.getTypeface();
    }

    @Override
    protected void onBind(@NonNull InAppFilter model,
                          @NonNull String query,
                          @Nullable Object payload) {
        if (model.filter == null) {
            mText.setText(R.string.branch_discover_all_results);
            mImage.setVisibility(View.GONE);
        } else {
            mText.setText(model.filter.getAppName());
            mImage.setVisibility(View.VISIBLE);
            mImage.setImageURI(model.filter.getAppIconUrl());
        }

        BranchAppResult currentFilter = (BranchAppResult) payload;
        boolean isCurrent = (currentFilter == null && model.filter == null)
                || (currentFilter != null && currentFilter.equals(model.filter));
        mText.setSelected(isCurrent);
        mText.setTypeface(mTextTypeface, isCurrent ? Typeface.BOLD
                : Typeface.NORMAL);
        itemView.setSelected(isCurrent);
        itemView.setElevation(isCurrent ? mElevationSelected
                : mElevationUnselected);
    }
}
