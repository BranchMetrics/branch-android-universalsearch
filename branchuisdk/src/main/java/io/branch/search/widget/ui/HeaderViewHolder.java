package io.branch.search.widget.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.branch.search.widget.R;

/**
 * Shows the header for a provider list.
 */
public class HeaderViewHolder extends DiscoveryViewHolder<CharSequence> {

    @NonNull
    private final TextView mName;
    @NonNull
    private final TextView mToggle;
    @NonNull
    private final ImageView mToggleIcon;

    public HeaderViewHolder(@NonNull LayoutInflater inflater,
                            @NonNull ViewGroup parent,
                            @NonNull Callback<CharSequence> callback) {
        super(inflater, parent, R.layout.branch_section_header, callback);
        mName = itemView.findViewById(R.id.branch_header_title);
        mToggle = itemView.findViewById(R.id.branch_header_toggle);
        mToggleIcon = itemView.findViewById(R.id.branch_header_toggle_icon);
        View line = itemView.findViewById(R.id.branch_header_line);
        ColorDrawable drawable = (ColorDrawable) line.getBackground();
        if (Color.alpha(drawable.getColor()) == 0) {
            line.setVisibility(View.GONE);
        } else {
            line.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onBind(@NonNull CharSequence model,
                          @NonNull String query,
                          @Nullable Object payload) {
        mName.setText(model);
        if (payload instanceof Boolean && (Boolean) payload) {
            mToggle.setText(R.string.branch_provider_toggle_more);
            mToggle.setVisibility(View.VISIBLE);
            mToggleIcon.setImageResource(R.drawable.branch_ic_more_24dp);
            mToggleIcon.setVisibility(View.VISIBLE);
        } else if (payload instanceof Boolean) {
            mToggle.setText(R.string.branch_provider_toggle_less);
            mToggle.setVisibility(View.VISIBLE);
            mToggleIcon.setImageResource(R.drawable.branch_ic_less_24dp);
            mToggleIcon.setVisibility(View.VISIBLE);
        } else {
            mToggle.setVisibility(View.GONE);
            mToggleIcon.setVisibility(View.GONE);
        }
    }
}
