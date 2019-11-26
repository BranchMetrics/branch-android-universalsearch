package io.branch.search.widget.provider;

import android.Manifest;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.rule.GrantPermissionRule;
import android.util.Log;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.search.widget.BaseControllerTest;
import io.branch.search.widget.BranchSearchCallback;
import io.branch.search.widget.BranchSearchController;
import io.branch.search.widget.provider.loader.ProviderLoader;

public class InAppSearchProviderTest extends BaseControllerTest {
    private static final String TAG = "Branch::InAppSearchProviderTest";

    @Rule
    public GrantPermissionRule mRuntimePermissionsRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void testLatency() throws Throwable {
        InAppSearchProvider.Options options = new InAppSearchProvider.Options();
        options.location = new Location("mock");
        options.location.setLatitude(40.730610);
        options.location.setLongitude(-73.935242);
        final long timeout = 8000;

        // First, warm up with 3 calls.
        testLatency("warmup", options, timeout, null);
        testLatency("warmup", options, timeout, null);
        testLatency("warmup", options, timeout, null);

        // 3*3 (9)
        options.maxAppResults = 3;
        options.maxContentPerAppResults = 3;
        Assert.assertTrue(testLatency(options, timeout, "3x3, 9 results"));

        // 5*10 (50)
        options.maxAppResults = 5;
        options.maxContentPerAppResults = 10;
        Assert.assertTrue(testLatency(options, timeout, "5x10, 50 results"));

        // 10*20 (200)
        options.maxAppResults = 10;
        options.maxContentPerAppResults = 20;
        Assert.assertTrue(testLatency(options, timeout, "10x20, 200 results"));

        // 15*50 (750)
        options.maxAppResults = 15;
        options.maxContentPerAppResults = 50;
        Assert.assertTrue(testLatency(options, timeout, "15x50, 750 results"));
    }

    @SuppressWarnings("SameParameterValue")
    private boolean testLatency(
            @NonNull InAppSearchProvider.Options options,
            long timeoutMillis,
            @Nullable String log) throws Throwable {
        return testLatency("pizza", options, timeoutMillis, log);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean testLatency(
            @NonNull final String text,
            @NonNull InAppSearchProvider.Options options,
            long timeoutMillis,
            @Nullable String log) throws Throwable {
        // Set up
        InAppSearchProvider provider = new InAppSearchProvider();
        ProviderLoader loader = createLoader(provider);
        Map<Class<? extends IDiscoveryProvider>, Object> payload
                = createPayload(provider, options);
        final CountDownLatch latch = new CountDownLatch(1);

        // Create controller and run
        BranchSearchController controller = createController(payload, loader);
        controller.addCallback(new BranchSearchCallback() {
            public void onQueryUpdateRequested(@NonNull CharSequence newQuery) { }

            @Override
            public void onProviderResults(@NonNull IDiscoveryProvider provider,
                                          @NonNull String query,
                                          @NonNull List<Object> results) {
                if (query.equalsIgnoreCase(text)) {
                    latch.countDown();
                }
            }

            @Override
            public void onProviderError(@NonNull IDiscoveryProvider provider,
                                        @NonNull String query,
                                        @NonNull Exception error) {
                if (query.equalsIgnoreCase(text)) {
                    latch.countDown();
                }
            }
        });
        long before = System.currentTimeMillis();
        controller.onTextChanged(text);
        boolean result = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        long after = System.currentTimeMillis();
        String prefix = "";
        if (log != null) {
            prefix = "[" + log.toUpperCase() + "] ";
        }
        Log.w(TAG, prefix + "Latency test for '" + text + "' took " + (after - before) + " ms.");
        return result;

    }
}

