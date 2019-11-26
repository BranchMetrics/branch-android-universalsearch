package io.branch.search.widget.generator;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;

import io.branch.search.widget.database.DatabaseItem;

/**
 * A Generator will:
 *
 * - generate a list of items, used by providers
 * - cache the list in memory
 * - save the list somewhere else so it can be retrieved (e.g. SharedPreferences)
 * - keep it in sync when new items are used
 *
 * With the {@link #add(Context, T)} method, the generator will be notified that a certain item
 * has been recently used. This item will be returned at the top of the list next time.
 *
 * @param <T> item type
 */
interface Generator<T extends DatabaseItem> {

    /**
     * Gets the current list of items, generating it if necessary.
     * Should be called from background threads.
     *
     * @param context a context
     * @return a list of items, possibly empty
     */
    @NonNull
    List<T> get(@NonNull Context context);

    /**
     * Adds this item to the list, as the most recently used.
     *
     * @param context a context
     * @param item the recently used item
     */
    void add(@NonNull Context context, @NonNull T item);

    /**
     * Should be called to release resources.
     */
    void release();
}
