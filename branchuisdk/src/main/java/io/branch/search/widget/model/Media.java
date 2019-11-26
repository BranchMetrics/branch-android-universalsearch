package io.branch.search.widget.model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.branch.search.widget.R;
import io.branch.search.widget.util.BranchFileProvider;
import java.io.File;
import java.util.Objects;

/**
 * Represents a media item from MediaStore.
 */
@Keep
public class Media implements Parcelable {

    public final static int TYPE_FOLDER = 0;
    public final static int TYPE_PICTURE = 1;
    public final static int TYPE_VIDEO = 2;
    public final static int TYPE_AUDIO = 3;
    public final static int TYPE_PLAYLIST = 4;
    public final static int TYPE_PDF = 5;
    public final static int TYPE_OTHER = -1;

    @NonNull private final Uri mUri;
    @Nullable private String mMediaTitle;
    private int mMediaType;

    public Media(@NonNull Uri uri, @Nullable String mediaTitle, int mediaType) {
        mUri = uri;
        mMediaTitle = mediaTitle;
        mMediaType = mediaType;
    }

    @NonNull
    public Uri getUri() {
        return mUri;
    }

    /**
     * Returns a content:// type of Uri thanks to {@link BranchFileProvider}.
     * Unlike {@link #getUri()}, this uri can be exported for example through Intents.
     */
    @NonNull
    public Uri getContentUri(@NonNull Context context) {
        File file = new File(mUri.getPath());
        return BranchFileProvider.getUriForFile(context, file);
    }

    /**
     * Returns a readable name.
     */
    @NonNull
    public String getName() {
        if (mMediaTitle != null) {
            return mMediaTitle;
        } else if ("file".equals(mUri.getScheme()) && mUri.getLastPathSegment() != null) {
            return Objects.requireNonNull(mUri.getLastPathSegment());
        } else {
            // Fallback to full URI but this should not happen as far as I know.
            return mUri.toString();
        }
    }

    /**
     * Returns a color based on the mediaType value.
     */
    @ColorRes
    public int getColor() {
        switch (mMediaType) {
            case TYPE_FOLDER: return R.color.branch_media_folder;
            case TYPE_PICTURE: return R.color.branch_media_picture;
            case TYPE_VIDEO: return R.color.branch_media_video;
            case TYPE_AUDIO: return R.color.branch_media_audio;
            case TYPE_PLAYLIST: return R.color.branch_media_playlist;
            case TYPE_PDF: return R.color.branch_media_pdf;
            default: return R.color.branch_media_other;
        }
    }

    /**
     * Returns an icon based on the mediaType value.
     */
    @DrawableRes
    public int getIcon() {
        switch (mMediaType) {
            case TYPE_FOLDER: return R.drawable.branch_ic_media_folder_24dp;
            case TYPE_PICTURE: return R.drawable.branch_ic_media_picture_24dp;
            case TYPE_VIDEO: return R.drawable.branch_ic_media_video_24dp;
            case TYPE_AUDIO: return R.drawable.branch_ic_media_audio_24dp;
            case TYPE_PLAYLIST: return R.drawable.branch_ic_media_playlist_24dp;
            case TYPE_PDF: return R.drawable.branch_ic_media_pdf_24dp;
            default: return R.drawable.branch_ic_media_other_24dp;
        }
    }

    // Parcelable implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mUri, flags);
        dest.writeByte(mMediaTitle != null ? (byte) 1 : 0);
        if (mMediaTitle != null) {
            dest.writeString(mMediaTitle);
        }
        dest.writeInt(mMediaType);
    }

    public final static Creator<Media> CREATOR = new ClassLoaderCreator<Media>() {

        @Override
        public Media createFromParcel(Parcel source) {
            return createFromParcel(source, getClass().getClassLoader());
        }

        @Override
        public Media createFromParcel(Parcel source, ClassLoader loader) {
            Uri uri = (Uri) Objects.requireNonNull(source.readParcelable(loader));
            String mediaTitle = null;
            if (source.readByte() == 1) {
                mediaTitle = source.readString();
            }
            int mediaType = source.readInt();
            return new Media(uri, mediaTitle, mediaType);
        }

        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }
    };
}
