package io.branch.search.widget.provider;

import android.Manifest;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.rule.GrantPermissionRule;
import android.util.Log;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.search.widget.BaseControllerTest;
import io.branch.search.widget.BranchSearchCallback;
import io.branch.search.widget.BranchSearchController;
import io.branch.search.widget.R;

// TODO mock location instead of mocking through emulator?
public class LatencyTest extends BaseControllerTest {
    private static final String TAG = "Branch::LatencyTest";

    @Rule
    public GrantPermissionRule mRuntimePermissionsRule =
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CONTACTS);
    @Test
    public void testCallLatency() throws Throwable {
        Assert.assertTrue(testCallLatency("pizza", 8000));
    }

    @SuppressWarnings("SameParameterValue")
    private boolean testCallLatency(@NonNull final String text,
                                    long timeoutMillis) throws Throwable {
        String[] allProviders = getTargetContext()
                .getResources()
                .getStringArray(R.array.IDiscoveryProvider);
        final CountDownLatch latch = new CountDownLatch(allProviders.length);
        final Set<IDiscoveryProvider> providers
                = Collections.synchronizedSet(new HashSet<IDiscoveryProvider>());
        BranchSearchController controller = createController(null, null);
        controller.addCallback(new BranchSearchCallback() {
            @Override
            public void onQueryUpdateRequested(@NonNull CharSequence newQuery) { }

            @Override
            public void onProviderResults(@NonNull IDiscoveryProvider provider,
                                          @NonNull String query,
                                          @NonNull List<Object> results) {
                if (query.equalsIgnoreCase(text) && providers.add(provider)) {
                    Log.d(TAG, "Provider " + provider.getClass().getSimpleName()
                            + " returned with results: " + results.size());
                    latch.countDown();
                }
            }

            @Override
            public void onProviderError(@NonNull IDiscoveryProvider provider,
                                        @NonNull String query,
                                        @NonNull Exception error) {
                if (query.equalsIgnoreCase(text) && providers.add(provider)) {
                    Log.d(TAG, "Provider " + provider.getClass().getSimpleName()
                            + " returned with error: " + error);
                    latch.countDown();
                }
            }
        });
        long before = System.currentTimeMillis();
        controller.onTextChanged(text);
        boolean result;
        if (timeoutMillis > 0) {
            result = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } else {
            latch.await();
            result = true;
        }
        long after = System.currentTimeMillis();
        Log.w(TAG, "-----------------------------------------------------------------");
        Log.w(TAG, "Latency test for '" + text + "' took " + (after - before) + " ms.");
        return result;
    }
}

