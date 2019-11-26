package io.branch.search.widget.provider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.R;
import io.branch.search.widget.generator.ContactsGenerator;
import io.branch.search.widget.model.Contact;
import io.branch.search.widget.ui.ContactViewHolder;
import io.branch.search.widget.ui.DiscoveryViewHolder;
import io.branch.search.widget.util.BranchEvents;

/**
 * Base class for providers of contacts. Offers:
 * <p>
 * - a list of top contacts. {@link #getTopContactsList()}, {@link #addTopContact(Contact)}
 * - implementation of {@link #launchResult(Contact, Object, int)} to launch a contact
 * - methods to send a message or call a contact
 */
public abstract class BaseContactsProvider<VM extends DiscoveryViewModel<Contact>>
        extends SimpleDiscoveryProvider<Contact, VM> {

    private final ContactsGenerator mContactsGenerator = new ContactsGenerator();
    private final static String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS};
    protected static final int NUM_COLUMNS = 2;

    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return PERMISSIONS;
    }

    /**
     * Gets a list of top contacts from {@link ContactsGenerator}.
     *
     * @return a list of top contacts, or null if not available
     */
    @Nullable
    protected final List<Contact> getTopContactsList() {
        Context context = getContext();
        if (context == null) return null;
        return mContactsGenerator.get(context);
    }

    /**
     * Registers a new top contact.
     *
     * @param contact the contact
     */
    protected final void addTopContact(@NonNull Contact contact) {
        Context context = getContext();
        if (context == null) return;
        mContactsGenerator.add(context, contact);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContactsGenerator.release();
    }

    @Override
    protected void launchResult(@NonNull Contact item, @Nullable Object payload, int position) {
        if (ContactViewHolder.PAYLOAD_CALL == payload) {
            callItem(position, item);
        } else if (ContactViewHolder.PAYLOAD_CHAT == payload) {
            messageItem(position, item);
        } else {
            addTopContact(item);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_URI,
                            "" + item.getId()));
            startActivity(intent);

            position++; // 1 based
            new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                    .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                            getClass().getSimpleName())
                    .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                            String.valueOf(position))
                    .addCustomDataProperty(BranchEvents.ResultClick.EXTRA,
                            BranchEvents.ResultClick.CONTACT_OPEN)
                    .logEvent(requireContext());
        }
    }

    protected void messageItem(int position, @NonNull Contact item) {
        if (!item.hasPhoneNumber()) return;
        addTopContact(item);
        Uri uri = Uri.parse("smsto:" + item.getPhoneNumber());
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, uri);
        startActivity(Intent.createChooser(smsIntent, ""));

        position++; // 1 based
        new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                        getClass().getSimpleName())
                .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                        String.valueOf(position))
                .addCustomDataProperty(BranchEvents.ResultClick.EXTRA,
                        BranchEvents.ResultClick.CONTACT_MESSAGE)
                .logEvent(requireContext());
    }

    protected void callItem(int position, @NonNull Contact item) {
        if (!item.hasPhoneNumber()) return;
        addTopContact(item);
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + item.getPhoneNumber()));
        startActivity(intent);

        position++; // 1 based
        new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                        getClass().getSimpleName())
                .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                        String.valueOf(position))
                .addCustomDataProperty(BranchEvents.ResultClick.EXTRA,
                        BranchEvents.ResultClick.CONTACT_CALL)
                .logEvent(requireContext());
    }

    @Override
    protected int getAdapterColumns() {
        return NUM_COLUMNS;
    }

    @Override
    protected float getAdapterSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.branch_contact_spacing);
    }

    @Override
    protected DiscoveryViewHolder<Contact> createAdapterViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @NonNull DiscoveryViewHolder.Callback<Contact> callback) {
        return new ContactViewHolder(inflater, parent, callback);
    }

    @Override
    protected void onSectionCreated(@NonNull DiscoverySection<Contact> section,
                                    @NonNull View view) {
        super.onSectionCreated(section, view);
        section.setPadding(
                getResources().getDimension(R.dimen.branch_contact_list_padding_horizontal),
                getResources().getDimension(R.dimen.branch_contact_list_padding_vertical));
    }

    /**
     * Our content can change while the app was not in the resumed state.
     * Request a new discovery trigger to ensure we are updated.
     */
    @Override
    public void onResume() {
        super.onResume();
        callback.requestDiscovery(this, null);
    }
}