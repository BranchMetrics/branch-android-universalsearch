package io.branch.search.widget.task;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.concurrent.CancellationException;

import io.branch.search.widget.provider.IDiscoveryProvider;
import io.branch.search.widget.provider.IDiscoveryProviderCallback;

/**
 * An {@link AsyncTaskManager} customized for {@link IDiscoveryProvider}.
 * They will execute async tasks relative to a certain query and send result to a certain
 * {@link IDiscoveryProviderCallback} which, in the current implementation, can change at
 * every call.
 *
 * The input is always the query string.
 * The output is always a list of something.
 */
public class DiscoveryTaskManager<Output> extends AsyncTaskManager {

    @NonNull
    private final IDiscoveryProvider mProvider;
    private final IDiscoveryProviderCallback mCallback;

    public DiscoveryTaskManager(@NonNull IDiscoveryProvider provider,
                                @Nullable IDiscoveryProviderCallback callback) {
        super(Type.ANDROID);
        mProvider = provider;
        mCallback = callback;
    }

    /**
     * Executes the given action using the query as input, and dealing with the callbacks
     * that the {@link IDiscoveryProviderCallback} is expecting, based on the action results.
     *
     * @param query the query
     * @param token the query token
     * @param action the action
     * @return a Task
     */
    @NonNull
    public Task<List<Output>> execute(
            @NonNull final String query,
            final int token,
            @NonNull AsyncTask.Action<String, List<Output>> action) {
        if (mCallback != null) mCallback.onDiscoveryStarted(mProvider, query, token);
        return super.execute(query, action)
                .addOnSuccessListener(new OnSuccessListener<List<Output>>() {
            @Override
            public void onSuccess(List<Output> outputs) {
                if (mCallback != null) {
                    //noinspection unchecked
                    List<Object> objects = (List) outputs;
                    mCallback.onDiscoveryCompleted(mProvider, query, token, objects, null);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (mCallback != null) {
                    mCallback.onDiscoveryCompleted(mProvider, query, token, null, e);
                }
            }
        });
    }

    /**
     * Aborts any previous operation, dispatching an error result to the given
     * callback if one is present.
     */
    public void abort(@Nullable String abortedQuery, int abortedToken) {
        super.execute("", null);
        if (mCallback != null && abortedQuery != null) {
            mCallback.onDiscoveryCompleted(mProvider, abortedQuery, abortedToken,
                    null, new CancellationException());
        }
    }
}
