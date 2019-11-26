package io.branch.search.widget.database;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Base class for model items that will be retrieved by a
 * {@link io.branch.search.widget.generator.DatabaseGenerator}.
 */
@Entity
@Keep
public class DatabaseItem {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private final int mId;

    @ColumnInfo(name = "interactions")
    private int mInteractions = 0;

    protected DatabaseItem(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public int getInteractions() {
        return mInteractions;
    }

    public void setInteractions(int interactions) {
        mInteractions = interactions;
    }

    // Equals implementation

    @Override
    public final boolean equals(@Nullable Object obj) {
        return obj != null
                && obj.getClass() == getClass()
                && ((DatabaseItem) obj).getId() == getId();
    }

    @Override
    public final int hashCode() {
        return getId();
    }
}
