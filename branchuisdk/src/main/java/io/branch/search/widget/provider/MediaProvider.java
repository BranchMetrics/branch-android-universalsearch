package io.branch.search.widget.provider;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.branch.referral.util.BranchEvent;
import io.branch.search.widget.R;
import io.branch.search.widget.model.Media;
import io.branch.search.widget.ui.DiscoveryViewHolder;
import io.branch.search.widget.ui.MediaViewHolder;
import io.branch.search.widget.util.BranchEvents;
import io.branch.search.widget.util.WordMatcher;

import static android.provider.MediaStore.Files.*;

/**
 * Providers are instantiated by reflection.
 * This class is used if present in the IDiscoveryProvider string array.
 *
 * NOTE: Do not rename without changing the array.
 */
@Keep
@SuppressWarnings("unused")
public class MediaProvider
        extends SimpleDiscoveryProvider<Media, DiscoveryViewModel<Media>> {
    private static final String TAG = "Branch::MediaStore";
    private static final int MIN_QUERY_CHARS = 2;

    /**
     * We will filter results that belong to one of these folders.
     */
    private final List<File> mFilters = Arrays.asList(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    );

    private final WordMatcher mWordMatcher = new WordMatcher(
            new WordMatcher.NonLowerCaseSplitter()
    );

    @NonNull
    @Override
    public String[] getRequiredPermissions() {
        return new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE };
    }

    @Override
    protected boolean isQueryValid(@NonNull String query, int token, boolean confirmed) {
        return super.isQueryValid(query, token, confirmed)
                && query.length() >= MIN_QUERY_CHARS;
    }

    @NonNull
    @Override
    protected List<Media> loadResults(@NonNull String query, int token, int capacity) {
        Context context = getContext();
        if (context == null) throw new RuntimeException("No Context");
        List<Media> results = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri target = MediaStore.Files.getContentUri("external");
        String[] projection = {
                FileColumns._ID,
                FileColumns.DATA,
                FileColumns.MEDIA_TYPE,
                FileColumns.TITLE,
        };

        // Match the query wherever in the full path field (DATA). We'll apply refinements
        // later below. Could use the TITLE field but it does not contain the file extension.
        String selection = FileColumns.DATA + " LIKE ?";
        String[] selectionArgs = new String[]{ "%" + query + "%" };

        // In 26+ we can specify the query limit through a Bundle, but we do post-query filtering
        // that can filter out most results. There might be a way to integrate the post-query
        // filters into the SQL query, but that would create a huge number of combinations (ORs).
        // It might be simpler and better to filter by directory after the query as we're doing
        // now (no LIMIT).
        Cursor cursor = resolver.query(target, projection, selection, selectionArgs, null);

        // Read results
        try {
            //noinspection ConstantConditions
            while (cursor.moveToNext() && results.size() < capacity) {
                String fullPath = cursor.getString(cursor.getColumnIndex(FileColumns.DATA));
                if (fullPath.endsWith("/")) {
                    fullPath = fullPath.substring(0, fullPath.length() - 1);
                }
                String lastPath = fullPath.substring(fullPath.lastIndexOf("/") + 1);
                String title = cursor.getString(cursor.getColumnIndex(FileColumns.TITLE));
                int type = cursor.getInt(cursor.getColumnIndex(FileColumns.MEDIA_TYPE));
                File file = new File(fullPath);
                Uri uri = Uri.fromFile(file);

                // Apply filters.
                if (!applyFilters(fullPath)) continue;
                if (file.isHidden()) continue;
                if (!mWordMatcher.matches(lastPath, query)) continue;
                if (file.isDirectory()) continue; // Not a lot of apps can open folders on click

                // Check type and add.
                int mediaType;
                if (type == FileColumns.MEDIA_TYPE_AUDIO) {
                    mediaType = Media.TYPE_AUDIO;
                } else if (type == FileColumns.MEDIA_TYPE_IMAGE) {
                    mediaType = Media.TYPE_PICTURE;
                } else if (type == FileColumns.MEDIA_TYPE_VIDEO) {
                    mediaType = Media.TYPE_VIDEO;
                } else if (type == FileColumns.MEDIA_TYPE_PLAYLIST) {
                    mediaType = Media.TYPE_PLAYLIST;
                } else {
                    // Use uri.toString() so we have encoded String that is accepted by MimeTypeMap.
                    String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
                    if (extension.equals("pdf")) {
                        mediaType = Media.TYPE_PDF;
                    } else {
                        mediaType = Media.TYPE_OTHER;
                    }
                }
                Media result = new Media(uri, title, mediaType);
                results.add(result);

                // Check for exact match
                if (query.equalsIgnoreCase(result.getName())) {
                    notifyExactMatch(query, token);
                }
            }
            cursor.close();
            return results;
        } catch (Exception e) {
            Log.d(TAG, "Error while retrieving media objects. " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean applyFilters(@NonNull String fullPath) {
        for (File filter : mFilters) {
            if (fullPath.startsWith(filter.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void launchResult(@NonNull Media item, @Nullable Object payload, int position) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(item.getContentUri(requireContext()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(intent,
                getString(R.string.branch_media_provider_open));
        startActivity(chooser);


        position++; // 1 based
        new BranchEvent(BranchEvents.TYPE_RESULT_CLICK)
                .addCustomDataProperty(BranchEvents.ResultClick.PROVIDER,
                        getClass().getSimpleName())
                .addCustomDataProperty(BranchEvents.ResultClick.POSITION,
                        String.valueOf(position))
                .logEvent(requireContext());
    }

    @Override
    protected float getAdapterSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.branch_media_spacing);
    }

    @Nullable
    @Override
    protected CharSequence getAdapterHeader() {
        return getResources().getString(R.string.branch_media_provider_title);
    }

    @Override
    protected int getAdapterCapacity(int columns) {
        return columns * 2;
    }

    @Override
    protected DiscoveryViewHolder<Media> createAdapterViewHolder(
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent,
            @NonNull DiscoveryViewHolder.Callback<Media> callback) {
        return new MediaViewHolder(inflater, parent, callback);
    }

    @Override
    protected void onSectionCreated(@NonNull DiscoverySection<Media> section, @NonNull View view) {
        super.onSectionCreated(section, view);
        section.setPadding(
                getResources().getDimension(R.dimen.branch_media_list_padding_horizontal),
                getResources().getDimension(R.dimen.branch_media_list_padding_vertical));
        section.setAutoColumns(R.layout.branch_media);
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