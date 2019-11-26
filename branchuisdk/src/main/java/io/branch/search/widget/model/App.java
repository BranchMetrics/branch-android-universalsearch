package io.branch.search.widget.model;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import java.util.Objects;

import io.branch.search.widget.database.DatabaseItem;

/**
 * Represents an app (or an action/intent/activity) installed on the device.
 */
@Entity(tableName = "Apps")
@Keep
public class App extends DatabaseItem implements Parcelable {

    @ColumnInfo(name = "packageName")
    @NonNull
    private final String mPackageName;

    @ColumnInfo(name = "label")
    @NonNull
    private final String mLabel;

    @ColumnInfo(name = "iconResId")
    @DrawableRes
    private final int mIconResId;

    @ColumnInfo(name = "popularity")
    private int mPopularity = -1;

    @Ignore
    public App(@NonNull String packageName, @NonNull String label) {
        this(packageName, label, 0);
    }

    @Ignore
    public App(@NonNull String packageName, @NonNull String label, int iconResId) {
        this(packageName.hashCode(), packageName, label, iconResId);
    }

    // Needed by room so we have an id setter
    public App(int id, @NonNull String packageName, @NonNull String label, int iconResId) {
        super(id);
        mPackageName = packageName;
        mLabel = label;
        mIconResId = iconResId;
    }

    @Ignore
    @NonNull
    public Drawable getIcon(@NonNull Context context) {
        if (mIconResId != 0) {
            // Use our own value. Currently only used for shortcuts.
            //noinspection ConstantConditions
            return ContextCompat.getDrawable(context, mIconResId);
        } else {
            try {
                // This is an expensive operation.
                return context.getPackageManager().getApplicationIcon(mPackageName);
            } catch (PackageManager.NameNotFoundException e) {
                // Should never happen
                return new ColorDrawable(Color.WHITE);
            }

        }
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    @NonNull
    public String getLabel() {
        return mLabel;
    }

    @SuppressWarnings("unused")
    public int getIconResId() {
        return mIconResId;
    }

    public void setPopularity(int popularity) {
        mPopularity = popularity;
    }

    public int getPopularity() {
        return mPopularity;
    }

    // Parcelable implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackageName);
        dest.writeString(mLabel);
        dest.writeInt(mIconResId);
        dest.writeInt(mPopularity);
        dest.writeInt(getInteractions());
    }

    public static final Creator<App> CREATOR = new Creator<App>() {
        @Override
        public App createFromParcel(Parcel source) {
            App item = new App(
                    Objects.requireNonNull(source.readString()),
                    Objects.requireNonNull(source.readString()),
                    source.readInt());
            item.setPopularity(source.readInt());
            item.setInteractions(source.readInt());
            return item;
        }

        @Override
        public App[] newArray(int size) {
            return new App[size];
        }
    };
}
