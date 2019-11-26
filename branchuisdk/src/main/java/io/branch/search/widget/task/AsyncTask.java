package io.branch.search.widget.task;

import android.support.annotation.NonNull;

/**
 * Abstract interface for async operations. We currently have only one
 * implementation based on Android's AsyncTasks, but this can be changed in the
 * future (should we want to use different threading models).
 *
 * These tasks should only be used once.
 */
public interface AsyncTask<Input, Output> {

    /**
     * Executes the task.
     */
    void execute(@NonNull Input input, @NonNull Callback<Input, Output> callback);


    /**
     * Can be called to abort the execution.
     */
    void abort();

    /**
     * Recieves callbacks.
     */
    interface Callback<Input, Output> {
        void onSuccess(@NonNull Output result);
        void onFailure(@NonNull Exception error);
    }

    /**
     * Specifies an action.
     */
    interface Action<Input, Output> {
        @NonNull Output execute(@NonNull Input input);
    }
}
