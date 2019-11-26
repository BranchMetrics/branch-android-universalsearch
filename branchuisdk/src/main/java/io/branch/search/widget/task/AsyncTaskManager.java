package io.branch.search.widget.task;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

/**
 * Executes a single {@link AsyncTask} at a time, canceling the older in case
 * a new one is requested. This is the policy we want for Branch queries.
 *
 * It returns Google's {@link Task} so we can add success and failure listeners.
 */
public class AsyncTaskManager {

    /**
     * Specifies the async implementation. Currently only {@link #ANDROID}
     * which will use {@link AndroidAsyncTask} under the hood.
     */
    enum Type {
        ANDROID
    }

    @NonNull private final Type mType;
    private AsyncTask<?, ?> mTask = null;

    @SuppressWarnings("WeakerAccess")
    public AsyncTaskManager(@NonNull Type type) {
        mType = type;
    }

    /**
     * Executes the given action by passing in the given input, canceling any older task
     * if some was present. Wraps the action into a Google's {@link Task} so we can
     * add success and failure listeners.
     *
     * A null action will abort any old action and immediately return.
     *
     * @param input the input object
     * @param action the action
     * @param <Input> input type
     * @param <Output> output type
     * @return a Task
     */
    public <Input, Output> Task<Output> execute(@NonNull Input input,
                                                @Nullable AsyncTask.Action<Input, Output> action) {
        AsyncTask<Input, Output> newTask = createAsyncTask(action);
        if (mTask != null) mTask.abort();
        mTask = newTask;

        final TaskCompletionSource<Output> token = new TaskCompletionSource<>();
        newTask.execute(input, new AsyncTask.Callback<Input, Output>() {
            @Override
            public void onSuccess(@NonNull Output result) {
                token.trySetResult(result);
            }

            @Override
            public void onFailure(@NonNull Exception error) {
                token.trySetException(error);
            }
        });
        return token.getTask();
    }

    /**
     * Cretes a {@link AsyncTask} implementation based on the given {@link Type}.
     * A null action will return a task that immediately completes.
     */
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private <Input, Output> AsyncTask<Input, Output> createAsyncTask(
            @Nullable AsyncTask.Action<Input, Output> action) {
        if (action == null) {
            return new NoOpAsyncTask<>();
        }
        switch (mType) {
            case ANDROID: return new AndroidAsyncTask<>(action);
            default: throw new IllegalStateException("Type not implemented.");
        }
    }

    private static class NoOpAsyncTask<Input, Output> implements AsyncTask<Input, Output> {

        @Override
        public void execute(@NonNull Input input, @NonNull Callback<Input, Output> callback) {
            //noinspection ConstantConditions
            callback.onSuccess(null);
        }

        @Override
        public void abort() { }
    }
}
