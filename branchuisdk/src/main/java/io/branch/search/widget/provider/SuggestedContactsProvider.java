package io.branch.search.widget.provider;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.List;

import io.branch.search.widget.model.Contact;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 *
 * NOTE: Do not rename without changing the array.
 *
 * This provider shows suggested contacts when the text query is empty.
 * When we have a query, {@link ContactsProvider} will do the job.
 */
@Keep
@SuppressWarnings("unused")
public class SuggestedContactsProvider
        extends BaseContactsProvider<DiscoveryViewModel<Contact>> {
    private static final String TAG = "Branch::Contacts";

    /**
     * We only accept empty queries, so this is inverted.
     */
    @Override
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        return TextUtils.isEmpty(query);
    }

    @NonNull
    @Override
    protected List<Contact> loadResults(@NonNull String query, int token, int capacity) {
        List<Contact> result = getTopContactsList();
        if (result == null) throw new RuntimeException("Could not retrieve top contacts.");
        return result;
    }

    @Nullable
    @Override
    protected CharSequence getAdapterHeader() {
        return null;
    }

    @Override
    protected int getAdapterCapacity(int columns) {
        return columns;
    }
}
