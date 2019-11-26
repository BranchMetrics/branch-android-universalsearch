package io.branch.search.widget.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import java.io.File;

/**
 * A {@link FileProvider} implementation. Currently empty, but since it's declared in the
 * manifest, we don't want to use base provider to avoid conflicts with app manifests.
 */
public class BranchFileProvider extends FileProvider {

    private final static String AUTHORITY_SUFFIX = ".branchfileprovider";

    /**
     * Calls {@link FileProvider#getUriForFile(Context, String, File)} by using the correct
     * authority that we have used in the manifest file.
     */
    @NonNull
    public static Uri getUriForFile(@NonNull Context context, @NonNull File file) {
        // context.getPackageName() returns the application id, which is what we use in manifest.
        String authority = context.getPackageName() + AUTHORITY_SUFFIX;
        return FileProvider.getUriForFile(context, authority, file);
    }
}
