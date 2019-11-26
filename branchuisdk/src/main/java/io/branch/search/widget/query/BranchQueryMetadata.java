package io.branch.search.widget.query;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;

import io.branch.search.widget.BranchSearchController;

/**
 * Utilities to add metadata to query strings.
 *
 * This is a way of passing meta information to {@link BranchSearchController} without changing
 * the API - we can simply add spans to the text, assuming it is spannable, and the text coming
 * from EditText will always be.
 *
 * Currently used to pass {@link #SOURCE} information, that is, info about what triggered the query.
 */
public class BranchQueryMetadata {

    /**
     * Key for metadata values. The type T can identify a specific set of values
     * or be a generic type like String.
     * @param <T> value type
     */
    @SuppressWarnings("WeakerAccess")
    public static class Key<T> {
        private final Class<T> c;
        private Key(@NonNull Class<T> c) {
            this.c = c;
        }
    }

    public static Key<BranchQuerySource> SOURCE = new Key<>(BranchQuerySource.class);

    @SuppressWarnings("WeakerAccess")
    public static <T> void clear(@NonNull CharSequence input, @NonNull Key<T> key) {
        if (input instanceof Spannable) {
            T[] values = ((Spannable) input).getSpans(0, input.length(), key.c);
            for (T value : values) {
                ((Spannable) input).removeSpan(value);
            }
        }
    }

    @NonNull
    public static <T> CharSequence set(@NonNull CharSequence input,
                                       @NonNull Key<T> key,
                                       @NonNull T value) {
        Spannable spannable;
        if (input instanceof Spannable) {
            spannable = (Spannable) input;
            clear(spannable, key);
        } else {
            spannable = new SpannableString(input);
        }
        spannable.setSpan(value, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannable;
    }

    @Nullable
    public static <T> T get(@NonNull CharSequence input, @NonNull Key<T> key) {
        if (input instanceof Spanned) {
            T[] values = ((Spanned) input).getSpans(0, input.length(), key.c);
            if (values != null && values.length > 0) {
                return values[0];
            }
        }
        return null;
    }
}
