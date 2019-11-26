package io.branch.search.widget.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ChangedPackages;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.branch.search.widget.generator.AppsGenerator;

/**
 * Manages device packages which are used, for example, by {@link AppsGenerator}
 * to generate a list.
 *
 * Keeping track of newly installed - removed apps requires extra work after API 26 since
 * we can't listen to the Intent.ACTION_PACKAGE_ADDED broadcast anymore.
 */
public class DeviceAppsManager extends BroadcastReceiver {

    private final static String HAS_CHANGES_KEY = "HAS_CHANGES";
    private final static String SEQUENCE_NUMBER_KEY = "SEQUENCE_NUMBER";
    private final static String BOOT_COUNT_KEY = "BOOT_COUNT";

    /**
     * @param context a context
     * @param packageName name to be checked
     * @return true if package is installed
     */
    public static boolean isPackageInstalled(@NonNull Context context,
                                             @NonNull String packageName) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        return intent != null;
    }

    /**
     * Returns a list of currently installed apps.
     * @param context a context
     * @return a list of apps
     */
    @NonNull
    public static List<ResolveInfo> getInstalledApps(@NonNull Context context) {
        SharedPreferences preferences = Utility.getSharedPreferences(context);
        preferences.edit().putBoolean(HAS_CHANGES_KEY, false).apply();

        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = manager.queryIntentActivities(intent,
                PackageManager.GET_META_DATA);
        // Remove this package just in case.
        List<ResolveInfo> filteredApps = new ArrayList<>();
        for (ResolveInfo app : apps) {
            if (!context.getPackageName().equals(app.activityInfo.packageName)) {
                filteredApps.add(app);
            }
        }
        return filteredApps;
    }

    /**
     * Returns true if the device apps are dirty, meaning that something changed
     * since the last time we checked.
     * If true is returned, {@link #getInstalledApps(Context)} should be called again.
     *
     * @param context a context
     * @return true if dirty
     */
    public static boolean isDirty(@NonNull Context context) {
        SharedPreferences preferences = Utility.getSharedPreferences(context);
        if (Build.VERSION.SDK_INT >= 26) {
            // Android requires us to use getChangedPackages which has a messy API where we must
            // keep track of a sequence number and the boot count. Doing so through preferences.
            int sequence = preferences.getInt(SEQUENCE_NUMBER_KEY, 0);
            int realBootCount = 0;
            try {
                realBootCount = Settings.Global.getInt(
                        context.getContentResolver(),
                        Settings.Global.BOOT_COUNT);
            } catch (Settings.SettingNotFoundException ignore) { /* ignore */ }
            int bootCount = preferences.getInt(BOOT_COUNT_KEY, realBootCount);
            if (bootCount != realBootCount) {
                sequence = 0;
                preferences.edit()
                        .putInt(SEQUENCE_NUMBER_KEY, sequence)
                        .putInt(BOOT_COUNT_KEY, realBootCount)
                        .apply();
            }
            PackageManager manager = context.getPackageManager();
            ChangedPackages packages = manager.getChangedPackages(sequence);
            if (packages != null) {
                preferences.edit()
                        .putInt(SEQUENCE_NUMBER_KEY, packages.getSequenceNumber())
                        .apply();
                return true;
            } else {
                // For some reason that I couldn't figure out, sometimes our sequence number can
                // become bigger than it should. Due to this, the ChangedPackages will always be
                // null for a long time. For example, our sequence number can be 100 but
                // PackageManager is left on 20: in this case, we need 80 changes before we can
                // inspect the correct sequence number. I think this can happen maybe when
                // preferences are saved into a new device.
                // Anyway, this can be easily solved by inspecting sequence 0 to get on par with
                // the PackageManager counter.
                if (sequence != 0) {
                    packages = manager.getChangedPackages(0);
                    if (packages != null && sequence > packages.getSequenceNumber()) {
                        // Update the sequence and return true. We don't know if some package
                        // changed, but there's no way to know since our reference number was wrong.
                        preferences.edit()
                                .putInt(SEQUENCE_NUMBER_KEY, packages.getSequenceNumber())
                                .apply();
                        return true;
                    }
                }
                return false;
            }
        } else {
            return preferences.getBoolean(HAS_CHANGES_KEY, false);
        }
    }

    /**
     * For API < 26, we listen to PACKAGE_ADDED event and set HAS_CHANGES to true.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            Utility.getSharedPreferences(context)
                    .edit()
                    .putBoolean(HAS_CHANGES_KEY, true)
                    .apply();
        }
    }
}
