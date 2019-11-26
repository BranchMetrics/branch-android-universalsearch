package io.branch.search.widget.ui;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.branch.search.widget.R;
import io.branch.search.widget.model.SearchEngine;

/**
 * Displays a search engine prompt.
 */
public class SearchEngineViewHolder extends DiscoveryViewHolder<SearchEngine> {

    @NonNull private final TextView mText;
    @NonNull private final ImageView mIcon;

    public SearchEngineViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @Nullable Callback<SearchEngine> callback) {
        super(inflater, parent, R.layout.branch_search_engine, callback);
        mText = itemView.findViewById(R.id.branch_autocomplete_footer_text);
        mIcon = itemView.findViewById(R.id.branch_autocomplete_footer_image);
    }

    @Override
    protected void onBind(@NonNull SearchEngine model,
                          @NonNull String query,
                          @Nullable Object payload) {
        String string = model.getLabel(getContext(), query);
        int index = string.indexOf("\"" + query + "\"");
        Object boldSpan = new StyleSpan(Typeface.BOLD);
        Object colorSpan = new ForegroundColorSpan(ContextCompat.getColor(getContext(),
                R.color.branch_autocomplete_text_highlight));
        int start = index + 1;
        int end = start + query.length();
        SpannableString spannable = new SpannableString(string);
        spannable.setSpan(boldSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannable.setSpan(colorSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        mText.setText(spannable);
        Drawable icon = model.getIcon(getContext());
        if (icon != null) {
            mIcon.setImageDrawable(icon);
            mIcon.setVisibility(View.VISIBLE);
        } else {
            mIcon.setVisibility(View.GONE);
        }
    }
}
