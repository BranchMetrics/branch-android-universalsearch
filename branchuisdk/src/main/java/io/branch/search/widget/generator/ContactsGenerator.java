package io.branch.search.widget.generator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import io.branch.search.widget.database.ContactsDao;
import io.branch.search.widget.model.Contact;

import static android.provider.ContactsContract.*;

/**
 * Generate, caches and provides a list of top contacts by:
 * - Keeping track of last used contacts by the app
 * - Seeing if any contact is called 'Mom', 'Mommy', 'Ma', or 'Amma'
 * - Seeing if any contact is called 'Dad', 'Daddy', 'Pa', or 'Papa'
 * - Seeing if any contact is called 'Home'
 * - Falling back to alphabetical order
 *
 * Unlike {@link AppsGenerator}, there seems to be no easy way to check whether the contact list
 * has changed recently. To address this, we do two things:
 * - when app is first opened, assume something has changed
 * - while app is alive, use a {@link ContentObserver} to check for changes
 */
public class ContactsGenerator extends DatabaseGenerator<Contact> {

    private static final String PERMISSION = Manifest.permission.READ_CONTACTS;

    // For a future release, should consider adding these to an asset file and making this generator
    // dynamic so it can be changed and/or localized easily.
    private static final String[] CANDIDATE_MOM = {"mom", "mommy", "ma", "amma"};
    private static final String[] CANDIDATE_DAD = {"dad", "daddy", "pa", "papa"};
    private static final String[] CANDIDATE_HOME = {"home"};
    private static final String[][] CANDIDATES = {CANDIDATE_MOM, CANDIDATE_DAD, CANDIDATE_HOME};

    private static final List<Contact> sContacts
            = Collections.synchronizedList(new ArrayList<Contact>());
    private static boolean sContactsShouldBeEmpty = false;
    private static boolean sFirstSyncDone = false;
    private static boolean sIsSynced = true; // default to true!

    private Context mObserverContext = null;
    private final ContentObserver mObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            sIsSynced = false;
        }
    };

    private void maybeRegisterObserver(@NonNull Context context) {
        if (mObserverContext == null) {
            mObserverContext = context.getApplicationContext();
            mObserverContext.getContentResolver()
                    // Observe the Phone URI. This should be fine - it will surely change if the
                    // phone changes or if the display name changes.
                    .registerContentObserver(CommonDataKinds.Phone.CONTENT_URI,
                            false, mObserver);
        }
    }

    private void maybeUnregisterObserver() {
        if (mObserverContext != null) {
            mObserverContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserverContext = null;
        }
    }

    @NonNull
    private ContactsDao getDao(@NonNull Context context) {
        return getDatabase(context).contactItemDao();
    }

    @Override
    protected boolean needsGeneration(@NonNull Context context) {
        if (!sFirstSyncDone) {
            sFirstSyncDone = true;
            return true;
        }
        if (!sIsSynced) {
            sIsSynced = true;
            return true;
        }
        if (sContacts.isEmpty() && !sContactsShouldBeEmpty) {
            sContacts.addAll(getDao(context).getAll());
        }
        return sContacts.isEmpty() && !sContactsShouldBeEmpty;
    }

    @NonNull
    @Override
    public List<Contact> get(@NonNull Context context) {
        maybeRegisterObserver(context);
        // Synchronize on our class. Other instances of the same class might be doing the
        // same generation path, but we don't want to generate twice.
        synchronized (ContactsGenerator.class) {
            if (needsGeneration(context)) {
                sContacts.clear();
                sContacts.addAll(generate(context));
                sContactsShouldBeEmpty = sContacts.isEmpty();
            }
        }
        // Return a copy just to be safer.
        return new ArrayList<>(sContacts);
    }

    @Override
    public void add(final @NonNull Context context, final @NonNull Contact item) {
        maybeRegisterObserver(context);
        // Increase interactions and save. Also we must clear sContacts so it will be
        // updated at the next get() call. This is not very expensive.
        item.setInteractions(item.getInteractions() + 1);
        Tasks.call(AsyncTask.THREAD_POOL_EXECUTOR, new Callable<Void>() {
            @Override
            public Void call() {
                getDao(context).update(item);
                sContacts.clear();
                sContacts.addAll(getDao(context).getAll());
                return null;
            }
        });
    }

    @NonNull
    @Override
    protected List<Contact> onGenerate(final @NonNull Context context) {
        if (ContextCompat.checkSelfPermission(context, PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            // We can't read contacts. Return an empty list, which means we failed.
            return new ArrayList<>();
        }
        // We must sync the device apps and the dao apps.
        List<Contact> deviceContacts = getDeviceContacts(context);
        List<Contact> daoContacts = getDao(context).getAll();

        // Remove from DAO any app that's not in the device anymore.
        for (Contact daoContact : daoContacts) {
            if (!deviceContacts.contains(daoContact)) {
                getDao(context).delete(daoContact);
            }
        }
        daoContacts = getDao(context).getAll();

        // Add to DAO any app that's not in dao but is in device, plus check
        // for changes.
        for (Contact deviceContact : deviceContacts) {
            int daoIndex = daoContacts.indexOf(deviceContact);
            if (daoIndex < 0) {
                getDao(context).update(deviceContact);
            } else {
                Contact daoContact = daoContacts.get(daoIndex);
                if (daoContact.getFullName().equals(deviceContact.getFullName())
                        && daoContact.getInteractions() >= deviceContact.getInteractions()
                        && Objects.equals(daoContact.getPhoneNumber(),
                        deviceContact.getPhoneNumber())
                        && Objects.equals(daoContact.getFirstName(),
                        deviceContact.getFirstName())) {
                    // These contacts are identical.
                    //noinspection UnnecessaryContinue
                    continue;
                } else {
                    // Merge the two contacts.
                    Contact newContact = new Contact(
                            deviceContact.getId(),
                            deviceContact.getFullName()
                    );
                    newContact.setPhoneNumber(deviceContact.getPhoneNumber());
                    newContact.setFirstName(deviceContact.getFirstName());
                    newContact.setInteractions(Math.max(
                            daoContact.getInteractions(),
                            deviceContact.getInteractions()
                    ));
                    getDao(context).update(newContact);
                }
            }
        }
        daoContacts = getDao(context).getAll();

        // Return. Apps will be sorted based on DAO query.
        return daoContacts;
    }

    @NonNull
    private List<Contact> getDeviceContacts(@NonNull Context context) {
        List<Contact> results = new ArrayList<>();
        // We need first/last name information so we must query the top-level Data URI
        // and ask for rows of two kinds, either Phone or StructuredName type.
        String selection = Data.MIMETYPE + " = ? OR "
                + Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[] {
                CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
        };
        String[] fields = new String[] {
                Data.CONTACT_ID,
                Data.DISPLAY_NAME_PRIMARY,
                Data.MIMETYPE,
                CommonDataKinds.Phone.NUMBER, // For Phone rows
                CommonDataKinds.StructuredName.GIVEN_NAME, // For StructuredName rows
                CommonDataKinds.StructuredName.FAMILY_NAME, // For StructuredName rows
                CommonDataKinds.StructuredName.PREFIX // For StructuredName rows
        };
        Cursor cursor = context.getContentResolver()
                .query(ContactsContract.Data.CONTENT_URI,
                        fields,
                        selection,
                        selectionArgs,
                        null);
        if (cursor == null) {
            throw new RuntimeException("Cursor is null.");
        }
        while (cursor.moveToNext()) {
            int contactId = cursor.getInt(cursor.getColumnIndex(Data.CONTACT_ID));
            String contactName = cursor.getString(cursor.getColumnIndex(Data.DISPLAY_NAME_PRIMARY));
            if (contactName == null) continue; // Not sure what this is, but we don't want it.
            Contact contactItem = new Contact(contactId, contactName);
            int index = results.indexOf(contactItem);
            if (index >= 0) {
                // Retrieve information from the previous kind.
                contactItem = results.get(index);
            }

            // Apply information based on the mime type.
            String mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
            if (mimeType.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    && !contactItem.hasPhoneNumber()) {
                String phoneNumber = cursor.getString(cursor
                        .getColumnIndex(CommonDataKinds.Phone.NUMBER));
                contactItem.setPhoneNumber(phoneNumber);
            } else if (mimeType.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    && !contactItem.hasFirstName()) {
                String prefix = cursor.getString(cursor.getColumnIndex(
                        CommonDataKinds.StructuredName.PREFIX));
                String given = cursor.getString(cursor.getColumnIndex(
                        CommonDataKinds.StructuredName.GIVEN_NAME));
                String family = cursor.getString(cursor.getColumnIndex(
                        CommonDataKinds.StructuredName.FAMILY_NAME));
                prefix = TextUtils.isEmpty(prefix) ? null : prefix;
                given = TextUtils.isEmpty(given) ? null : given;
                family = TextUtils.isEmpty(family) ? null : family;
                // First, check that we have the information. If we don't, probably the user
                // has written the contact information in a single line.
                if (given != null && family != null) {
                    // Which is the "first" name? The given name or the family name? It depends
                    // on culture and alphabet. But our goal is to get the very "first" name,
                    // since it is shown BEFORE the rest - just in a different format.
                    String full = contactItem.getFullName();
                    if (full.startsWith(given)) {
                        contactItem.setFirstName(given);
                    } else if (full.startsWith(family)) {
                        contactItem.setFirstName(family);
                    } else if (prefix != null && full.startsWith(prefix + " " + given)) {
                        contactItem.setFirstName(prefix + " " + given);
                    } else if (prefix != null && full.startsWith(prefix + " " + family)) {
                        contactItem.setFirstName(prefix + " " + family);
                    } else if (prefix != null && full.startsWith(prefix + given)) {
                        contactItem.setFirstName(prefix + given);
                    } else if (prefix != null && full.startsWith(prefix + family)) {
                        contactItem.setFirstName(prefix + family);
                    }
                }
            }

            // Avoid duplication! Only add if needed.
            if (index < 0) {
                results.add(contactItem);
            }
        }
        cursor.close();

        // Extra loop to make sure we have assigned a first name. It is possible that we weren't
        // able to detect one, but we can guess it like iOS does - first name is the first word.
        for (Contact item : results) {
            if (!item.hasFirstName()) {
                List<String> names = item.getNames();
                if (names.size() > 1) item.setFirstName(names.get(0));
            }
        }

        // If a contact matches our preference pattern, add an 'interaction' to them.
        // This will make them appear as first in the list.
        for (String[] tests : CANDIDATES) {
            // See if we find 1 contact that matches any of these.
            contactsLoop: for (Contact item : results) {
                for (String test : tests) {
                    if (item.getFullName().equalsIgnoreCase(test)) {
                        item.setInteractions(1);
                        break contactsLoop; // Check with next array
                    }
                }
            }
        }
        return results;
    }

    @Override
    public void release() {
        maybeUnregisterObserver();
    }
}
