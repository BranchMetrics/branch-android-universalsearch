package io.branch.search.widget.provider.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import io.branch.search.widget.provider.IDiscoveryProvider;

/**
 * This class is responsible for loading providers.
 */
public abstract class ProviderLoader implements Iterable<IDiscoveryProvider> {

    // Cached providers, in instantiation order
    private final Map<String, IDiscoveryProvider> mProviders = new LinkedHashMap<>();

    protected ProviderLoader(@NonNull Context context) {
        load(context, mProviders);
    }

    /**
     * Loads the providers and puts them into the given map.
     * @param context a context
     * @param output the output map
     */
    protected abstract void load(@NonNull Context context,
                                 @NonNull Map<String, IDiscoveryProvider> output);

    public int size() {
        return mProviders.size();
    }

    @NonNull
    @Override
    public Iterator<IDiscoveryProvider> iterator() {
        return new Iterator<IDiscoveryProvider>() {

            Iterator<Map.Entry<String, IDiscoveryProvider>> mKnownProviders
                    = mProviders.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return mKnownProviders.hasNext();
            }

            @Override
            public IDiscoveryProvider next() {
                if (mKnownProviders.hasNext())
                    return mKnownProviders.next().getValue();

                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
