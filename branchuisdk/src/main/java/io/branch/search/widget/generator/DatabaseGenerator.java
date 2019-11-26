package io.branch.search.widget.generator;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.room.Room;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

import io.branch.search.widget.database.BranchDatabase;
import io.branch.search.widget.database.DatabaseItem;

/**
 * A generator that uses a {@link io.branch.search.widget.database.BranchDatabase}
 * to cache items. Items should extend the {@link DatabaseItem} class.
 *
 * @param <T> item
 */
public abstract class DatabaseGenerator<T extends DatabaseItem> implements Generator<T> {

    private final static Object sLock = new Object();
    private static BranchDatabase sDatabase = null;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected BranchDatabase getDatabase(@NonNull Context context) {
        if (sDatabase == null) {
            synchronized (sLock) {
                if (sDatabase == null) {
                    sDatabase = Room.databaseBuilder(context.getApplicationContext(),
                            BranchDatabase.class, "branchDatabase")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return sDatabase;
    }

    /**
     * Whether this generator needs a new generation, either because it has no in-memory cache,
     * no database data, or simply because we think that data is dirty.
     * If this returns true, {@link #generate(Context)} can be called.
     *
     * @param context a context
     * @return whether new generation is needed
     */
    abstract protected boolean needsGeneration(@NonNull Context context);

    /**
     * Generates the base list of items, synchronously.
     * Important: this should NOT check for needsGeneration()!
     *
     * @param context a context
     * @return a task
     */
    @NonNull
    abstract protected List<T> onGenerate(@NonNull final Context context) throws Exception;

    /**
     * Waits for {@link #onGenerate(Context)}, returning true if
     * the generation succeeded, false otherwise.
     *
     * @param context a context
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected List<T> generate(@NonNull Context context) {
        try {
            return onGenerate(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
