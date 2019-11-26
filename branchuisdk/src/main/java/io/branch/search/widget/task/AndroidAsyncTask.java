package io.branch.search.widget.task;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link io.branch.search.widget.task.AsyncTask} implementation based on Android's
 * {@link android.os.AsyncTask}.
 *
 * The default AsyncTask executors executes serially which is not what we want.
 * As an alternative, one can use {@link android.os.AsyncTask#THREAD_POOL_EXECUTOR}.
 *
 * However, it might make sense to customize the thread pool based on the number of
 * providers loading data and our specific needs, which is what we do here.
 */
public class AndroidAsyncTask<Input, Output> implements AsyncTask<Input, Output> {

    // Executor setup

    private static final int CORE_POOL_SIZE = 5;
    private static final long KEEP_ALIVE_SECONDS = 10L;
    private static final LinkedBlockingQueue<Runnable> QUEUE
            = new LinkedBlockingQueue<>(50);

    private static final ThreadFactory FACTORY = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "BranchThread #" + mCount.getAndIncrement());
        }
    };

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            CORE_POOL_SIZE * 2,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            QUEUE,
            FACTORY
    );

    static {
        EXECUTOR.allowCoreThreadTimeOut(true);
    }

    // Implementation

    @NonNull private final Action<Input, Output> mAction;
    private android.os.AsyncTask<Input, Void, Output> mAndroidAsyncTask;

    /**
     * Creates a task that will perform the given action.
     * @param action action
     */
    public AndroidAsyncTask(@NonNull Action<Input, Output> action) {
        mAction = action;
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("StaticFieldLeak")
    @Override
    public void execute(@NonNull Input input, @NonNull final Callback<Input, Output> callback) {
        mAndroidAsyncTask = new android.os.AsyncTask<Input, Void, Output>() {
            @SafeVarargs
            @Override
            protected final Output doInBackground(Input... inputs) {
                try {
                    Output output = mAction.execute(inputs[0]);
                    callback.onSuccess(output);
                    return output;
                } catch (Exception e) {
                    callback.onFailure(e);
                    return null;
                }
            }
        };
        mAndroidAsyncTask.executeOnExecutor(EXECUTOR, input);
    }

    @Override
    public void abort() {
        mAndroidAsyncTask.cancel(true);
    }
}
