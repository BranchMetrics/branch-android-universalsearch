package io.branch.search.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class BaseTest {
    @NonNull
    protected Context getTestContext() {
        return InstrumentationRegistry.getContext();
    }

    @NonNull
    protected Context getTargetContext() { return InstrumentationRegistry.getTargetContext(); }
}
