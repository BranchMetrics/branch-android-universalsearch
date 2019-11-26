package io.branch.search.widget.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.branch.search.widget.R;

/**
 * A SearchEngine implementation to show an extra UI element that, when clicked,
 * will perform the search query on that specific engine. Engines should provide
 * an icon, a label and a function to launch.
 */
public abstract class SearchEngine {
    @NonNull
    public String getLabel(@NonNull Context context, @NonNull String query) {
        return context.getString(R.string.branch_search_engine, query);
    }

    @Nullable
    public abstract Drawable getIcon(@NonNull Context context);

    public void launch(@NonNull Context context, @NonNull String query) {
        String search;
        try {
            search = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            search = query;
        }
        Uri uri = getUri(search);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    @NonNull
    public abstract Uri getUri(@NonNull String encodedQuery);
}
