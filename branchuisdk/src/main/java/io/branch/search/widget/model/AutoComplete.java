package io.branch.search.widget.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import io.branch.search.BranchQueryResult;
import io.branch.search.widget.provider.AutoCompleteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Represents the result of a branch autocomplete request
 * made by {@link AutoCompleteProvider}.
 */
@Keep
public class AutoComplete implements Parcelable {

    @NonNull private final String mName;

    @SuppressWarnings("WeakerAccess")
    public AutoComplete(@NonNull String name) {
        mName = name;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
    }

    public final static Creator<AutoComplete> CREATOR
            = new Creator<AutoComplete>() {
        @Override
        public AutoComplete createFromParcel(Parcel source) {
            return new AutoComplete(Objects.requireNonNull(source.readString()));
        }

        @Override
        public AutoComplete[] newArray(int size) {
            return new AutoComplete[size];
        }
    };

    /**
     * Parses the network response and returns a list of
     * {@link AutoComplete}s.
     */
    @NonNull
    public static List<AutoComplete> parseResults(@NonNull JSONObject result) {
        List<AutoComplete> list = new ArrayList<>();
        JSONArray array = result.optJSONArray("results");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    list.add(new AutoComplete(array.getString(i)));
                } catch (JSONException ignore) { }
            }
        }
        return list;
    }

    /**
     * Parses the Search SDK response and returns a list of
     * {@link AutoComplete}s.
     */
    @NonNull
    public static List<AutoComplete> parseResults(@NonNull BranchQueryResult result) {
        List<AutoComplete> list = new ArrayList<>();
        for (String value : result.getQueryResults()) {
            list.add(new AutoComplete(value));
        }
        return list;
    }
}
