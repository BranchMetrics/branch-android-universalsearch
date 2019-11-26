package io.branch.search.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.After;

import java.util.HashMap;
import java.util.Map;

import io.branch.search.widget.provider.IDiscoveryProvider;
import io.branch.search.widget.provider.loader.ProviderLoader;

public abstract class BaseControllerTest extends BaseActivityTest {

    private BranchSearchController mController;

    @After
    public void tearDown() {
        mController.tearDown();
    }

    @NonNull
    protected BranchSearchController createController(
            @Nullable Map<Class<? extends IDiscoveryProvider>, Object> payloads,
            @Nullable ProviderLoader loader) {
        if (mController != null) tearDown();
        mController = BranchSearchController.init(
                getActivity().getSupportFragmentManager(),
                getResultsView(),
                payloads,
                loader);
        return mController;
    }

    @NonNull
    protected ProviderLoader createLoader(@NonNull final IDiscoveryProvider... providers) {
        return new ProviderLoader(getActivity()) {
            @Override
            protected void load(@NonNull Context context,
                                @NonNull Map<String, IDiscoveryProvider> output) {
                for (IDiscoveryProvider provider : providers) {
                    output.put(provider.getClass().getName(), provider);
                }
            }
        };
    }

    @NonNull
    protected Map<Class<? extends IDiscoveryProvider>, Object> createPayload(
            @NonNull IDiscoveryProvider provider,
            @NonNull Object payload
    ) {
        Map<Class<? extends IDiscoveryProvider>, Object> map = new HashMap<>();
        map.put(provider.getClass(), payload);
        return map;
    }
}

