package io.branch.search.widget.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import java.util.Objects;

import io.branch.search.widget.R;

/**
 * Represents a shortcut to a settings screen.
 */
@Keep
public class Shortcut implements Parcelable {

    @NonNull private final String mIntentAction;
    @StringRes private final int mLabelResId;
    @DrawableRes private final int mIconResId;

    public Shortcut(@NonNull String intentAction, int labelResId) {
        this(intentAction, labelResId, R.drawable.branch_ic_settings_24dp);
    }

    public Shortcut(@NonNull String intentAction, int labelResId, int iconResId) {
        mIntentAction = intentAction;
        mLabelResId = labelResId;
        mIconResId = iconResId;
    }

    @NonNull
    public Drawable getIcon(@NonNull Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, mIconResId);
        if (drawable == null) {
            throw new RuntimeException("Shortcut icon is null. id=" + mIconResId);
        }
        return drawable;
    }

    @NonNull
    public String getIntentAction() {
        return mIntentAction;
    }

    @NonNull
    public String getLabel(@NonNull Context context) {
        return context.getString(mLabelResId);
    }


    // Equals implementation

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Shortcut
                && mIntentAction.equals(((Shortcut) obj).mIntentAction);
    }

    @Override
    public int hashCode() {
        return 31 + mIntentAction.hashCode();
    }

    // Parcelable implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mIntentAction);
        dest.writeInt(mLabelResId);
        dest.writeInt(mIconResId);
    }

    public static final Creator<Shortcut> CREATOR = new Creator<Shortcut>() {
        @Override
        public Shortcut createFromParcel(Parcel source) {
            return new Shortcut(
                    Objects.requireNonNull(source.readString()),
                    source.readInt(),
                    source.readInt());
        }

        @Override
        public Shortcut[] newArray(int size) {
            return new Shortcut[size];
        }
    };
}
