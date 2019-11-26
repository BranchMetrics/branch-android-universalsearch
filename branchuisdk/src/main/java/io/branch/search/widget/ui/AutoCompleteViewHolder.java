package io.branch.search.widget.ui;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import io.branch.search.widget.R;
import io.branch.search.widget.model.AutoComplete;

/**
 * Displays results from {@link AutoComplete}s.
 */
public class AutoCompleteViewHolder extends DiscoveryViewHolder<AutoComplete> {

    @NonNull
    private final TextView mText;

    public AutoCompleteViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @Nullable Callback<AutoComplete> callback) {
        super(inflater, parent, R.layout.branch_autocomplete, callback);
        mText = itemView.findViewById(R.id.branch_autocomplete_text);
    }

    @Override
    protected void onBind(@NonNull AutoComplete model,
                          @NonNull String query,
                          @Nullable Object payload) {
        int index = model.getName().toLowerCase().indexOf(query.toLowerCase());
        if (index >= 0) {
            Object boldSpan = new StyleSpan(Typeface.BOLD);
            Object colorSpan = new ForegroundColorSpan(ContextCompat.getColor(getContext(),
                    R.color.branch_autocomplete_text_highlight));

            int end = index + query.length();
            SpannableStringBuilder spannable = new SpannableStringBuilder(model.getName());
            spannable.setSpan(boldSpan, index, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spannable.setSpan(colorSpan, index, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            mText.setText(maybeEllipsize(spannable, query));
        } else {
            // This can happen. E.g. seraching for "do not" and getting "don't ..."
            mText.setText(maybeEllipsize(model.getName(), query));
        }
    }

    @NonNull
    private CharSequence maybeEllipsize(@NonNull CharSequence text, @NonNull String query) {
        if (text.length() <= 20) {
            return text;
        } else {
            // Text is too large. What we want is to remove the words that are common to both
            // the query and the text, then replace them with "…".
            SpannableStringBuilder builder = text instanceof SpannableStringBuilder
                    ? (SpannableStringBuilder) text
                    : new SpannableStringBuilder(text);
            int index = builder.toString().indexOf(query);
            int length = query.length();
            if (index < 0) {
                // Fallback: just drop the last characters.
                builder = (SpannableStringBuilder) builder.subSequence(0, 19);
                builder.append("…");
            } else if (index == 0) {
                // query is in the first part
                // TODO check words
                builder.replace(0, length, "…");
            } else if (index + length == builder.length()) {
                // query is in the last part
                // TODO check words
                builder.replace(index, index + length, "…");
            } else {
                // query is in the middle
                // TODO check words
                builder.replace(index, index + length, "…");
            }
            return builder;
        }
    }
}
