package io.branch.search.widget.provider.loader;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.Map;

import io.branch.search.widget.provider.IDiscoveryProvider;

/**
 * This class loads interfaces from an android string-array resource.
 * The resource should be named like the full class name of
 * {@link IDiscoveryProvider}.
 */
public class ResourceProviderLoader extends ProviderLoader {
    private static final String TAG = "Branch::ResourceLoader";

    public ResourceProviderLoader(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void load(@NonNull Context context,
                        @NonNull Map<String, IDiscoveryProvider> output) {
        String className = IDiscoveryProvider.class.getSimpleName();
        Log.d(TAG, "Loading: " + className);

        String[] array = getCandidateNames(context, className);
        for (String str : array) {
            Log.d(TAG, "Constructing: " + str);
            try {
                Class clazz = Class.forName(str);
                //noinspection unchecked
                Constructor<IDiscoveryProvider> constructor = clazz.getConstructor();
                IDiscoveryProvider obj = constructor.newInstance();
                output.put(str, obj);
            } catch (Exception e) {
                Log.d(TAG, "Constructing: " + str + " Failed.", e);
            }
        }
    }

    private String[] getCandidateNames(@NonNull Context context, @NonNull String className) {
        return getStringArrayByName(context, className);
    }

    private String[] getStringArrayByName(@NonNull Context context, @NonNull String aString) {
        String packageName = context.getPackageName();
        int resId = context.getResources().getIdentifier(aString, "array", packageName);

        String[] result = new String[0];
        try {
            result = context.getResources().getStringArray(resId);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource Not Found", e);
        }

        return result;
    }
}
