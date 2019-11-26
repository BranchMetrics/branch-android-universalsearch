package io.branch.search.widget.ui;

import android.content.ContentUris;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;

import io.branch.search.widget.R;
import io.branch.search.widget.model.Contact;
import io.branch.search.widget.ui.color.ColorPicker;
import io.branch.search.widget.ui.color.PaletteColorPicker;
import io.branch.search.widget.util.WordMatcher;

/**
 * Shows contact results (icon, name, call and message buttons) from
 * {@link Contact}s.
 */
public class ContactViewHolder extends DiscoveryViewHolder<Contact> {

    // Payloads for the click listener.
    public final static Object PAYLOAD_CALL = new Object();
    public final static Object PAYLOAD_CHAT = new Object();

    @NonNull private final SimpleDraweeView mIcon;
    @NonNull private final TextView mName;
    @NonNull private final TextView mNumber;
    @NonNull private final ImageButton mCallButton;
    @NonNull private final ImageButton mChatButton;
    @NonNull private final TextView mFistLetter;

    private final boolean mShouldShowNumber;

    public ContactViewHolder(@NonNull LayoutInflater inflater,
                             @NonNull ViewGroup parent,
                             @Nullable Callback<Contact> callback) {
        super(inflater, parent, R.layout.branch_contact, callback);
        mIcon = itemView.findViewById(R.id.branch_contact_image);
        mName = itemView.findViewById(R.id.branch_contact_name);
        mName.setSelected(true); // If ellipsize=marquee, this enables it.
        mNumber = itemView.findViewById(R.id.branch_contact_number);
        mShouldShowNumber = mNumber.getVisibility() == View.VISIBLE;
        mCallButton = itemView.findViewById(R.id.branch_contact_call);
        mChatButton = itemView.findViewById(R.id.branch_contact_message);
        mFistLetter = itemView.findViewById(R.id.branch_contact_letter);
    }

    @Override
    protected void onBind(final @NonNull Contact model,
                          @NonNull String query,
                          @Nullable Object payload) {
        if (payload instanceof WordMatcher) {
            WordMatcher matcher = (WordMatcher) payload;
            matcher.decorate(model.getFullName(), query, mName,
                    new StyleSpan(Typeface.BOLD));
        } else {
            mName.setText(model.getFullName());
        }

        if (mShouldShowNumber && model.hasPhoneNumber()) {
            mNumber.setVisibility(View.VISIBLE);
            mNumber.setText(model.getPhoneNumber());
        } else {
            mNumber.setVisibility(View.GONE);
        }
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.onClick(model, PAYLOAD_CALL, getAdapterPosition());
            }
        });
        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.onClick(model, PAYLOAD_CHAT, getAdapterPosition());
            }
        });

        // get contact photo uri
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                model.getId());
        Uri contactPhotoUri = Uri.withAppendedPath(contactUri,
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

        //random color for contacts without photo according to their name
        final ColorPicker colorPicker = new PaletteColorPicker(getContext().getResources());
        ControllerListener<ImageInfo> listener = new BaseControllerListener<ImageInfo>() {
            @Override
            public void onFinalImageSet(String id,
                                        @Nullable ImageInfo imageInfo,
                                        @Nullable Animatable animatable) {
                //Action on final image load
                mFistLetter.setVisibility(View.GONE);
                mIcon.setColorFilter(null);
            }

            @Override
            public void onFailure(String id, Throwable throwable) {
                //Action on failure
                mIcon.setImageResource(R.drawable.branch_shape_circle);
                mIcon.setColorFilter(colorPicker.pickColor(model.getFullName()));
                mFistLetter.setVisibility(View.VISIBLE);
                mFistLetter.setText(model.getInitials());
            }

        };

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setUri(contactPhotoUri)
                .setControllerListener(listener)
                .build();
        mIcon.setController(controller);
    }
}
