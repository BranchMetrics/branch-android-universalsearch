package io.branch.search.widget.provider;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.branch.search.widget.R;
import io.branch.search.widget.model.Contact;
import io.branch.search.widget.util.WordMatcher;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 *
 * NOTE: Do not rename without changing the array.
 */
@Keep
@SuppressWarnings("unused")
public class ContactsProvider
        extends BaseContactsProvider<DiscoveryViewModel<Contact>> {
    private static final String TAG = "Branch::Contacts";

    private WordMatcher mWordMatcher = new WordMatcher();

    @NonNull
    @Override
    protected List<Contact> loadResults(@NonNull String query, int token, int capacity) {
        // Construct a list of candidates based on the search
        List<Contact> contacts = getTopContactsList();
        List<Contact> list = new ArrayList<>();
        if (contacts != null && contacts.size() > 0) {
            for (Contact contact : contacts) {
                if (mWordMatcher.matches(contact.getFullName(), query)) {
                    list.add(contact);
                }
                // Check exact match.
                for (String displayName : contact.getNames()) {
                    if (displayName.equalsIgnoreCase(query)) {
                        notifyExactMatch(query, token);
                    }
                }

                // Don't bother if we already have max count.
                if (list.size() == capacity) {
                    break;
                }
            }
        }
        return list;
    }

    @Nullable
    @Override
    protected CharSequence getAdapterHeader() {
        return getResources().getText(R.string.branch_contacts_provider_title);
    }

    @Override
    protected int getAdapterCapacity(int columns) {
        return columns * 2;
    }

    @Nullable
    @Override
    protected Object getAdapterItemPayload(@NonNull Contact item) {
        return mWordMatcher;
    }
}
