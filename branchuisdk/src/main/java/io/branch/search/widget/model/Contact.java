package io.branch.search.widget.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.branch.search.widget.database.DatabaseItem;

/**
 * Represents a contact.
 */
@Entity(tableName = "Contacts")
@Keep
public class Contact extends DatabaseItem implements Parcelable {

    @ColumnInfo(name = "displayName")
    @NonNull
    private final String mFullName;

    @ColumnInfo(name = "phoneNumber")
    @Nullable
    private String mPhoneNumber = null;

    @ColumnInfo(name = "firstName")
    @Nullable
    private String mFirstName = null;

    public Contact(int id, @NonNull String fullName) {
        super(id);
        mFullName = fullName;
    }

    @NonNull
    public String getFullName() {
        return mFullName;
    }

    @Ignore
    @NonNull
    public List<String> getNames() {
        String[] names = mFullName.split(" ");
        return Arrays.asList(names);
    }

    @Nullable
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(@Nullable String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    @Nullable
    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(@Nullable String firstName) {
        mFirstName = firstName;
    }

    // Other getters

    @Ignore
    @NonNull
    public String getInitials() {
        String result = "";
        if (mFullName.length() > 0) {
            result = mFullName.substring(0, 1);
        }
        return result.trim().toUpperCase();
    }

    @Ignore
    public boolean hasPhoneNumber() {
        return mPhoneNumber != null && mPhoneNumber.trim().length() > 0;
    }

    @Ignore
    public boolean hasFirstName() {
        return mFirstName != null && mFirstName.trim().length() > 0;
    }

    // Parcelable implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getId());
        dest.writeString(mFullName);
        dest.writeInt(getInteractions());
        dest.writeByte(mPhoneNumber != null ? (byte) 1 : 0);
        if (mPhoneNumber != null) {
            dest.writeString(mPhoneNumber);
        }
        dest.writeByte(mFirstName != null ? (byte) 1 : 0);
        if (mFirstName != null) {
            dest.writeString(mFirstName);
        }
    }

    public final static Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel source) {
            Contact item = new Contact(
                    source.readInt(),
                    Objects.requireNonNull(source.readString()));
            item.setInteractions(source.readInt());
            if (source.readByte() == 1) {
                item.setPhoneNumber(source.readString());
            }
            if (source.readByte() == 1) {
                item.setFirstName(source.readString());
            }
            return item;
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
