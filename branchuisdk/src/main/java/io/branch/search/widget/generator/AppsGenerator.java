package io.branch.search.widget.generator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import io.branch.search.widget.database.AppsDao;
import io.branch.search.widget.model.App;
import io.branch.search.widget.util.DeviceAppsManager;
import io.branch.search.widget.util.Utility;

/**
 * Generate and provides a list of top apps by reading the asset files
 * and caching them in static memory and in database.
 */
public class AppsGenerator extends DatabaseGenerator<App> {

    private final static String LOCALE_KEY = "LOCALE";

    private final static List<App> sApps
            = Collections.synchronizedList(new ArrayList<App>());

    @NonNull
    private AppsDao getDao(@NonNull Context context) {
        return getDatabase(context).appItemDao();
    }

    /**
     * Tracks language / locale and checks if it changed with respect
     * to the last call. This is important because when locale changes, the
     * applications label might also change, so our list is dirty.
     * @param context context
     * @return true if dirty
     */
    private boolean isLocaleDirty(@NonNull Context context) {
        SharedPreferences preferences = Utility.getSharedPreferences(context);
        String lastLocale = preferences.getString(LOCALE_KEY, null);
        String locale = Locale.getDefault().toString();
        if (lastLocale == null) {
            // First call - we're not dirty, just need to update preferences.
            preferences.edit().putString(LOCALE_KEY, locale).apply();
            return false;
        } else if (locale.equals(lastLocale)) {
            return false;
        } else {
            preferences.edit().putString(LOCALE_KEY, locale).apply();
            return true;
        }
    }

    @Override
    protected boolean needsGeneration(@NonNull Context context) {
        if (DeviceAppsManager.isDirty(context)) return true;
        if (isLocaleDirty(context)) return true;
        if (sApps.isEmpty()) {
            sApps.addAll(getDao(context).getAll());
        }
        return sApps.isEmpty();
    }

    @NonNull
    @Override
    public List<App> get(@NonNull Context context) {
        // Synchronize on our class. Other instances of the same class might be doing the
        // same generation path, but we don't want to generate twice.
        // We could wrap this into a needsGeneration(), but DeviceAppsManager will not tell us
        // that apps are dirty twice, so this is much safer though a bit slower.
        synchronized (AppsGenerator.class) {
            if (needsGeneration(context)) {
                sApps.clear();
                sApps.addAll(generate(context));
            }
        }
        // Return a copy just to be safer.
        return new ArrayList<>(sApps);
    }

    @Override
    public void add(final @NonNull Context context, final @NonNull App item) {
        if (!DeviceAppsManager.isPackageInstalled(context,
                item.getPackageName())) return;
        // Increase interactions and save. Also we must clear sApps so it will be
        // updated at the next get() call. This is not very expensive.
        item.setInteractions(item.getInteractions() + 1);
        Tasks.call(AsyncTask.THREAD_POOL_EXECUTOR, new Callable<Void>() {
            @Override
            public Void call() {
                getDao(context).update(item);
                sApps.clear();
                sApps.addAll(getDao(context).getAll());
                return null;
            }
        });
    }

    @NonNull
    @Override
    protected List<App> onGenerate(@NonNull Context context) throws Exception {
        // We must sync the device apps and the dao apps.
        List<App> deviceApps = getDeviceApps(context);
        List<App> daoApps = getDao(context).getAll();

        // Remove from DAO any app that's not in the device anymore.
        for (App daoApp : daoApps) {
            if (!deviceApps.contains(daoApp)) {
                getDao(context).delete(daoApp);
            }
        }
        daoApps = getDao(context).getAll();

        // Add to DAO any app that's not in dao but is in device.
        for (App deviceApp : deviceApps) {
            if (!daoApps.contains(deviceApp)) {
                getDao(context).update(deviceApp);
            }
        }
        daoApps = getDao(context).getAll();

        // Return. Apps will be sorted based on DAO query.
        return daoApps;
    }

    @NonNull
    private List<App> getDeviceApps(@NonNull Context context) throws IOException,
            JSONException {
        // Read the top apps JSON
        InputStream is = context.getAssets().open("branch_topapps.json");
        int size = is.available();
        byte[] buffer = new byte[size];
        //noinspection ResultOfMethodCallIgnored
        is.read(buffer);
        is.close();
        String topAppsString = new String(buffer, StandardCharsets.UTF_8);
        JSONArray topAppsArray = new JSONArray(topAppsString);

        // Create a list of app items from the device
        ArrayList<App> deviceAppItems = new ArrayList<>();
        List<ResolveInfo> apps = DeviceAppsManager.getInstalledApps(context);
        for (ResolveInfo app : apps) {
            String packageName = app.activityInfo.applicationInfo.packageName;
            String label = app.loadLabel(context.getPackageManager()).toString();
            deviceAppItems.add(new App(packageName, label));
        }

        // Merge them with the JSON results popularity.
        for (int i = 0; i < topAppsArray.length(); i++) {
            JSONObject jsonAppItem = topAppsArray.getJSONObject(i);
            String packageName = jsonAppItem.getString("package_name");
            App item = new App(packageName, "");
            if (deviceAppItems.contains(item)) {
                App appItem = deviceAppItems.get(deviceAppItems.indexOf(item));
                appItem.setPopularity(jsonAppItem.getInt("popularity"));
            }
        }
        return deviceAppItems;
    }

    @Override
    public void release() {
        // Do nothing
    }
}
