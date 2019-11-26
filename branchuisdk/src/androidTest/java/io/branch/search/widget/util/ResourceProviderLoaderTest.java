package io.branch.search.widget.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;

import junit.framework.Assert;

import java.util.List;

import io.branch.search.widget.BaseTest;
import io.branch.search.widget.provider.IDiscoveryProvider;
import io.branch.search.widget.provider.IDiscoveryProviderCallback;
import io.branch.search.widget.provider.loader.ResourceProviderLoader;

public class ResourceProviderLoaderTest extends BaseTest {

    @Test
    public void testInterfaceLoadSuccess() {
        // Note:  Demonstrating that we can initialize the loader using the test context, but use
        // the target context to initialize each provider.

        ResourceProviderLoader loader = new ResourceProviderLoader(getTestContext());
        Assert.assertTrue(loader.size() > 0);

        IDiscoveryProviderCallback callback = new IDiscoveryProviderCallback() {
            public void onDiscoveryCompleted(@NonNull IDiscoveryProvider provider,
                                             @NonNull String query, int token,
                                             @Nullable List<Object> results,
                                             @Nullable Exception exception) { }
            public void requestDiscovery(@NonNull IDiscoveryProvider provider,
                                         @Nullable String queryUpdate) { }
            public void onDiscoveryStarted(@NonNull IDiscoveryProvider provider,
                                           @NonNull String query, int token) { }
            public void onExactMatch(@NonNull IDiscoveryProvider provider, @NonNull String query,
                                     int token) {
            }
            public void onResultClick(@NonNull IDiscoveryProvider provider,
                                      @NonNull String query, int token, @NonNull Object result) { }



        };
        for (IDiscoveryProvider provider : loader) {
            provider.initialize(getTargetContext(), callback, null);
        }
    }
}

